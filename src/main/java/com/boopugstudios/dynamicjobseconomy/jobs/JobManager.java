package com.boopugstudios.dynamicjobseconomy.jobs;

import com.boopugstudios.dynamicjobseconomy.DynamicJobsEconomy;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

public class JobManager {
    
    private final DynamicJobsEconomy plugin;
    private final Map<String, Job> jobs = new HashMap<>();
    private final Map<UUID, PlayerJobData> playerData = new HashMap<>();
    
    public JobManager(DynamicJobsEconomy plugin) {
        this.plugin = plugin;
        loadJobs();
    }
    
    private void loadJobs() {
        ConfigurationSection jobsSection = plugin.getConfig().getConfigurationSection("jobs");
        if (jobsSection == null) return;
        
        for (String jobName : jobsSection.getKeys(false)) {
            if (jobName.equals("enabled") || jobName.equals("max_jobs_per_player")) continue;
            
            ConfigurationSection jobConfig = jobsSection.getConfigurationSection(jobName);
            if (jobConfig == null || !jobConfig.getBoolean("enabled", true)) continue;
            
            Job job = new Job(
                jobName,
                jobConfig.getString("display_name", jobName),
                jobConfig.getString("description", ""),
                jobConfig.getInt("base_income", 50),
                jobConfig.getInt("xp_per_action", 10),
                jobConfig.getInt("max_level", 100)
            );
            
            // Load perks
            ConfigurationSection perksSection = jobConfig.getConfigurationSection("perks");
            if (perksSection != null) {
                for (String levelStr : perksSection.getKeys(false)) {
                    try {
                        int level = Integer.parseInt(levelStr.replace("level_", ""));
                        String perk = perksSection.getString(levelStr);
                        job.addPerk(level, perk);
                    } catch (NumberFormatException e) {
                        plugin.getLogger().warning("Invalid perk level: " + levelStr + " for job " + jobName);
                    }
                }
            }
            
            jobs.put(jobName, job);
        }
        
        plugin.getLogger().info("Loaded " + jobs.size() + " jobs: " + String.join(", ", jobs.keySet()));
    }
    
    public boolean joinJob(Player player, String jobName) {
        if (!jobs.containsKey(jobName)) {
            return false;
        }
        
        PlayerJobData data = getPlayerData(player);
        int maxJobs = plugin.getConfig().getInt("jobs.max_jobs_per_player", 3);
        
        if (data.getJobs().size() >= maxJobs) {
            return false;
        }
        
        if (data.hasJob(jobName)) {
            return false;
        }
        
        data.addJob(jobName);
        savePlayerJobData(player, jobName);
        return true;
    }
    
    public boolean leaveJob(Player player, String jobName) {
        PlayerJobData data = getPlayerData(player);
        
        if (!data.hasJob(jobName)) {
            return false;
        }
        
        data.removeJob(jobName);
        removePlayerJobData(player, jobName);
        return true;
    }
    
    public void addExperience(Player player, String jobName, int xp) {
        if (!jobs.containsKey(jobName)) return;
        
        PlayerJobData data = getPlayerData(player);
        if (!data.hasJob(jobName)) return;
        
        JobLevel jobLevel = data.getJobLevel(jobName);
        int oldLevel = jobLevel.getLevel();
        
        jobLevel.addExperience(xp);
        
        // Check for level up
        Job job = jobs.get(jobName);
        int xpNeeded = calculateXPNeeded(jobLevel.getLevel());
        
        while (jobLevel.getExperience() >= xpNeeded && jobLevel.getLevel() < job.getMaxLevel()) {
            jobLevel.setExperience(jobLevel.getExperience() - xpNeeded);
            jobLevel.setLevel(jobLevel.getLevel() + 1);
            
            // Send level up message
            String message = plugin.getConfig().getString("messages.job_levelup", "&6Congratulations! You reached level %level% in %job%!")
                .replace("%level%", String.valueOf(jobLevel.getLevel()))
                .replace("%job%", job.getDisplayName())
                .replace("&", "§");
            player.sendMessage(message);
            
            // Apply perks
            applyJobPerks(player, jobName, jobLevel.getLevel());
            
            xpNeeded = calculateXPNeeded(jobLevel.getLevel());
        }
        
        // Save if level changed
        if (oldLevel != jobLevel.getLevel()) {
            savePlayerJobData(player, jobName);
        }
    }
    
    private void applyJobPerks(Player player, String jobName, int level) {
        Job job = jobs.get(jobName);
        if (job == null) return;
        
        String perk = job.getPerkAtLevel(level);
        if (perk == null) return;
        
        // Apply perk logic here
        switch (perk) {
            case "double_ores":
                // Give player temporary mining luck effect
                player.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, 6000, 1)); // 5 minutes
                player.sendMessage(plugin.getConfig().getString("messages.prefix", "§8[§6DJE§8] ") + 
                    "§aDouble Ore perk activated! Mining luck increased for 5 minutes.");
                break;
            case "fortune_boost":
                // Store fortune boost in player metadata for use in mining events
                player.setMetadata("dje_fortune_boost", new FixedMetadataValue(plugin, level));
                player.sendMessage(plugin.getConfig().getString("messages.prefix", "§8[§6DJE§8] ") + 
                    "§aFortune boost perk activated! Level " + level + " fortune effect.");
                break;
            case "speed_boost":
                // Give player speed boost
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 12000, Math.min(level / 20, 2))); // 10 minutes
                player.sendMessage(plugin.getConfig().getString("messages.prefix", "§8[§6DJE§8] ") + 
                    "§aSpeed boost perk activated! Movement speed increased.");
                break;
            case "extra_crops":
                // Store crop multiplier in player metadata
                player.setMetadata("dje_crop_multiplier", new FixedMetadataValue(plugin, 1.0 + (level * 0.01))); // 1% per level
                player.sendMessage(plugin.getConfig().getString("messages.prefix", "§8[§6DJE§8] ") + 
                    "§aExtra Crops perk activated! Crop yield increased by " + (level) + "%.");
                break;
            case "night_vision":
                // Give night vision for miners
                player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 18000, 0)); // 15 minutes
                player.sendMessage(plugin.getConfig().getString("messages.prefix", "§8[§6DJE§8] ") + 
                    "§aNight Vision perk activated! See clearly in dark mines.");
                break;
            case "water_breathing":
                // Give water breathing for underwater work
                player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 12000, 0)); // 10 minutes
                player.sendMessage(plugin.getConfig().getString("messages.prefix", "§8[§6DJE§8] ") + 
                    "§aWater Breathing perk activated! Work underwater without worry.");
                break;
            case "haste":
                // Give haste effect for faster work
                player.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, 9600, Math.min(level / 25, 2))); // 8 minutes
                player.sendMessage(plugin.getConfig().getString("messages.prefix", "§8[§6DJE§8] ") + 
                    "§aHaste perk activated! Work faster with increased mining/digging speed.");
                break;
            default:
                plugin.getLogger().warning("Unknown job perk: " + perk);
                break;
        }
    }
    
    private int calculateXPNeeded(int level) {
        return level * 100; // Simple formula: 100 XP per level
    }
    
    public PlayerJobData getPlayerData(Player player) {
        return playerData.computeIfAbsent(player.getUniqueId(), uuid -> {
            PlayerJobData data = new PlayerJobData(uuid);
            loadPlayerJobData(player);
            return data;
        });
    }
    
    private void loadPlayerJobData(Player player) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = "SELECT job_name, level, experience FROM job_levels WHERE player_uuid = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, player.getUniqueId().toString());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    PlayerJobData data = playerData.get(player.getUniqueId());
                    while (rs.next()) {
                        String jobName = rs.getString("job_name");
                        int level = rs.getInt("level");
                        int experience = rs.getInt("experience");
                        
                        data.addJob(jobName);
                        data.getJobLevel(jobName).setLevel(level);
                        data.getJobLevel(jobName).setExperience(experience);
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading player job data for " + player.getName(), e);
        }
    }
    
    private void savePlayerJobData(Player player, String jobName) {
        PlayerJobData data = playerData.get(player.getUniqueId());
        if (data == null) return;
        
        JobLevel jobLevel = data.getJobLevel(jobName);
        
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = """
                INSERT INTO job_levels (player_uuid, job_name, level, experience) 
                VALUES (?, ?, ?, ?) 
                ON DUPLICATE KEY UPDATE level = ?, experience = ?
            """;
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, player.getUniqueId().toString());
                stmt.setString(2, jobName);
                stmt.setInt(3, jobLevel.getLevel());
                stmt.setInt(4, jobLevel.getExperience());
                stmt.setInt(5, jobLevel.getLevel());
                stmt.setInt(6, jobLevel.getExperience());
                
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error saving job data for " + player.getName(), e);
        }
    }
    
    public void savePlayerData(Player player) {
        PlayerJobData data = playerData.get(player.getUniqueId());
        if (data == null) return;
        
        // Save all job data for this player
        for (String jobName : data.getJobs()) {
            savePlayerJobData(player, jobName);
        }
    }
    
    public void unloadPlayerData(Player player) {
        playerData.remove(player.getUniqueId());
    }
    
    private void removePlayerJobData(Player player, String jobName) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = "DELETE FROM job_levels WHERE player_uuid = ? AND job_name = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, player.getUniqueId().toString());
                stmt.setString(2, jobName);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error removing job data for " + player.getName(), e);
        }
    }
    
    public void saveAllPlayerData() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            savePlayerData(player);
        }
        plugin.getLogger().info("Saved job data for all online players");
    }
    
    public void cleanupAntiExploitData() {
        // This method would clean up old anti-exploit data from JobListener
        // For now, we'll add a simple log message since the actual cleanup
        // logic is in JobListener
        plugin.getLogger().info("Performing anti-exploit data cleanup...");
        
        // In a real implementation, we'd access JobListener's cleanup method
        // or have shared data structures to clean up
    }
    
    public void reload() {
        jobs.clear();
        loadJobs();
    }
    
    public Map<String, Job> getJobs() {
        return new HashMap<>(jobs);
    }
    
    public Job getJob(String name) {
        return jobs.get(name);
    }
}
