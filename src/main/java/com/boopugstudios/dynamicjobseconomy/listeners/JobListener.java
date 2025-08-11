package com.boopugstudios.dynamicjobseconomy.listeners;

import com.boopugstudios.dynamicjobseconomy.DynamicJobsEconomy;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.player.PlayerHarvestBlockEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class JobListener implements Listener {
    
    private final DynamicJobsEconomy plugin;
    private final Map<UUID, Long> lastJobPayment = new HashMap<>();
    private final Map<String, Long> blockPlacementTimes = new HashMap<>();
    private static final long JOB_COOLDOWN = 3000; // 3 second cooldown between payments
    private static final long BLOCK_PLACEMENT_PROTECTION = 10000; // 10 seconds before placed blocks give rewards
    
    public JobListener(DynamicJobsEconomy plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Material material = event.getBlock().getType();
        
        // Anti-exploit: Check cooldown
        if (!checkJobCooldown(player)) {
            return;
        }
        
        // Anti-exploit: Check if block was recently placed
        String blockKey = getBlockKey(event.getBlock().getLocation());
        Long placementTime = blockPlacementTimes.get(blockKey);
        if (placementTime != null) {
            long timeSincePlacement = System.currentTimeMillis() - placementTime;
            if (timeSincePlacement < BLOCK_PLACEMENT_PROTECTION) {
                return; // Block was placed too recently
            }
            blockPlacementTimes.remove(blockKey); // Clean up
        }
        
        // Check if player has miner job and is mining ores
        if (isMiningMaterial(material)) {
            if (plugin.getJobManager().getPlayerData(player).hasJob("miner")) {
                // Check WorldGuard protection if enabled
                if (plugin.getIntegrationManager().isWorldGuardEnabled() && 
                    plugin.getIntegrationManager().isInProtectedRegion(player)) {
                    return;
                }
                
                // Apply McMMO bonus if available
                double bonus = plugin.getIntegrationManager().getMcMMOBonus(player, "mining");
                int baseXP = 10;
                double basePay = 50.0;
                
                plugin.getJobManager().addExperience(player, "miner", (int)(baseXP * (1 + bonus)));
                plugin.getEconomyManager().deposit(player, basePay * (1 + bonus));
                
                updateJobCooldown(player);
            }
        }
        
        // Builder job for breaking blocks (not placing to avoid double-dipping)
        if (plugin.getJobManager().getPlayerData(player).hasJob("builder")) {
            // Apply McMMO bonus if available
            double bonus = plugin.getIntegrationManager().getMcMMOBonus(player, "excavation");
            int baseXP = 5;
            double basePay = 25.0; // Reduced from 45 to prevent exploitation
            
            plugin.getJobManager().addExperience(player, "builder", (int)(baseXP * (1 + bonus)));
            plugin.getEconomyManager().deposit(player, basePay * (1 + bonus));
            
            updateJobCooldown(player);
        }
    }
    
    @EventHandler
    public void onFurnaceExtract(FurnaceExtractEvent event) {
        Player player = event.getPlayer();
        
        // Chef job for cooking
        if (plugin.getJobManager().getPlayerData(player).hasJob("chef")) {
            plugin.getEconomyManager().deposit(player, 40);
        }
    }
    
    @EventHandler
    public void onPlayerHarvest(PlayerHarvestBlockEvent event) {
        Player player = event.getPlayer();
        
        // Farmer job for harvesting
        if (plugin.getJobManager().getPlayerData(player).hasJob("farmer")) {
            plugin.getJobManager().addExperience(player, "farmer", 12);
            plugin.getEconomyManager().deposit(player, 35);
        }
    }
    
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        // Track when blocks are placed to prevent immediate farming
        String blockKey = getBlockKey(event.getBlock().getLocation());
        blockPlacementTimes.put(blockKey, System.currentTimeMillis());
        
        // Clean up old entries (older than protection time)
        cleanupOldPlacements();
    }
    
    private boolean checkJobCooldown(Player player) {
        long now = System.currentTimeMillis();
        Long lastPay = lastJobPayment.get(player.getUniqueId());
        
        if (lastPay != null && (now - lastPay) < JOB_COOLDOWN) {
            return false; // Still on cooldown
        }
        
        return true;
    }
    
    private void updateJobCooldown(Player player) {
        lastJobPayment.put(player.getUniqueId(), System.currentTimeMillis());
    }
    
    private String getBlockKey(org.bukkit.Location location) {
        return location.getWorld().getName() + "_" + 
               location.getBlockX() + "_" + 
               location.getBlockY() + "_" + 
               location.getBlockZ();
    }
    
    private void cleanupOldPlacements() {
        long now = System.currentTimeMillis();
        blockPlacementTimes.entrySet().removeIf(entry -> 
            (now - entry.getValue()) > BLOCK_PLACEMENT_PROTECTION);
    }
    
    private boolean isMiningMaterial(Material material) {
        return material == Material.COAL_ORE || 
               material == Material.IRON_ORE || 
               material == Material.GOLD_ORE || 
               material == Material.DIAMOND_ORE || 
               material == Material.EMERALD_ORE ||
               material == Material.DEEPSLATE_COAL_ORE ||
               material == Material.DEEPSLATE_IRON_ORE ||
               material == Material.DEEPSLATE_GOLD_ORE ||
               material == Material.DEEPSLATE_DIAMOND_ORE ||
               material == Material.DEEPSLATE_EMERALD_ORE;
    }
}
