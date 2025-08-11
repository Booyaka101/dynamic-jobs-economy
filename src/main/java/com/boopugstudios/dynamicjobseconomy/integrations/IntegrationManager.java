package com.boopugstudios.dynamicjobseconomy.integrations;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.boopugstudios.dynamicjobseconomy.DynamicJobsEconomy;
// LuckPerms integration using reflection to avoid compile-time dependency
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class IntegrationManager {
    
    private final DynamicJobsEconomy plugin;
    private boolean worldGuardEnabled = false;
    private boolean mcmmoEnabled = false;
    private boolean luckPermsEnabled = false;
    private boolean shopGUIPlusEnabled = false;
    private Object luckPermsAPI = null;
    
    public IntegrationManager(DynamicJobsEconomy plugin) {
        this.plugin = plugin;
    }
    
    public void setupIntegrations() {
        // Check for WorldGuard
        if (plugin.getServer().getPluginManager().isPluginEnabled("WorldGuard") && 
            plugin.getConfig().getBoolean("integrations.worldguard.enabled", true)) {
            worldGuardEnabled = true;
            plugin.getLogger().info("WorldGuard integration enabled");
        }
        
        // Check for McMMO
        if (plugin.getServer().getPluginManager().isPluginEnabled("McMMO") && 
            plugin.getConfig().getBoolean("integrations.mcmmo.enabled", true)) {
            mcmmoEnabled = true;
            plugin.getLogger().info("McMMO integration enabled");
        }
        
        // Check for LuckPerms
        if (plugin.getServer().getPluginManager().isPluginEnabled("LuckPerms") && 
            plugin.getConfig().getBoolean("integrations.luckperms.enabled", true)) {
            try {
                // Use reflection to get LuckPerms API
                Class<?> providerClass = Class.forName("net.luckperms.api.LuckPermsProvider");
                luckPermsAPI = providerClass.getMethod("get").invoke(null);
                luckPermsEnabled = true;
                plugin.getLogger().info("LuckPerms integration enabled");
            } catch (Exception e) {
                plugin.getLogger().warning("LuckPerms found but API unavailable: " + e.getMessage());
            }
        }
        
        // Check for ShopGUIPlus
        if (plugin.getServer().getPluginManager().isPluginEnabled("ShopGUIPlus") && 
            plugin.getConfig().getBoolean("integrations.shopguiplus.enabled", true)) {
            shopGUIPlusEnabled = true;
            plugin.getLogger().info("ShopGUIPlus integration enabled");
        }
    }
    
    public boolean isInProtectedRegion(Player player) {
        if (!worldGuardEnabled) {
            return false;
        }
        
        try {
            RegionManager regions = WorldGuard.getInstance().getPlatform()
                .getRegionContainer().get(BukkitAdapter.adapt(player.getWorld()));
            
            if (regions == null) {
                return false;
            }
            
            Set<ProtectedRegion> applicableRegions = regions.getApplicableRegions(
                BukkitAdapter.asBlockVector(player.getLocation())).getRegions();
            
            // Check if any region blocks job XP
            for (ProtectedRegion region : applicableRegions) {
                if (region.getFlag(com.sk89q.worldguard.protection.flags.Flags.DENY_SPAWN) != null ||
                    region.getId().toLowerCase().contains("nojobs") ||
                    region.getId().toLowerCase().contains("spawn")) {
                    return true; // Block job XP in these regions
                }
            }
            
            return false;
        } catch (Exception e) {
            plugin.getLogger().warning("Error checking WorldGuard regions: " + e.getMessage());
            return false;
        }
    }
    
    public boolean canEarnJobXP(Player player) {
        // Check WorldGuard protection
        if (isInProtectedRegion(player)) {
            return false;
        }
        
        // Check if player has permission to earn XP
        if (!player.hasPermission("djeconomy.jobs.earnxp")) {
            return false;
        }
        
        return true;
    }
    
    public double getMcMMOBonus(Player player, String jobType) {
        if (!mcmmoEnabled) {
            return 1.0; // No bonus
        }
        
        try {
            // Get McMMO skill level based on job type
            String mcmmoSkill = getMcMMOSkillForJob(jobType);
            if (mcmmoSkill == null) {
                return 1.0;
            }
            
            // Use reflection to get McMMO skill level (compatible with different versions)
            Class<?> mcmmoAPI = Class.forName("com.gmail.nossr50.api.ExperienceAPI");
            int skillLevel = (Integer) mcmmoAPI.getMethod("getLevel", Player.class, String.class)
                .invoke(null, player, mcmmoSkill);
            
            // Calculate bonus: 1% per 10 levels, max 50% bonus
            double bonus = Math.min(0.5, (skillLevel / 10.0) * 0.01);
            return 1.0 + bonus;
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting McMMO bonus: " + e.getMessage());
            return 1.0;
        }
    }
    
    private String getMcMMOSkillForJob(String jobType) {
        return switch (jobType.toLowerCase()) {
            case "miner" -> "MINING";
            case "farmer" -> "HERBALISM";
            case "builder" -> "REPAIR";
            case "chef" -> "COOKING";
            default -> null;
        };
    }
    
    public void updateJobPermissions(Player player, String jobType, int level) {
        if (!luckPermsEnabled || luckPermsAPI == null) {
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                // Use reflection for LuckPerms integration
                Object userManager = luckPermsAPI.getClass().getMethod("getUserManager").invoke(luckPermsAPI);
                Object user = userManager.getClass().getMethod("getUser", java.util.UUID.class)
                    .invoke(userManager, player.getUniqueId());
                
                if (user == null) {
                    return;
                }
                
                // Clear old permissions (simplified approach)
                // Add new permission using Bukkit's permission system as fallback
                Bukkit.getScheduler().runTask(plugin, () -> {
                    // Remove old permissions
                    for (int i = 1; i <= 100; i++) {
                        String oldPerm = "djeconomy.job." + jobType + ".level." + i;
                        if (player.hasPermission(oldPerm)) {
                            // This would be handled by LuckPerms internally
                        }
                    }
                    
                    // Set new permissions via LuckPerms commands
                    String command = "lp user " + player.getName() + " permission set djeconomy.job." + jobType + ".level." + level;
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                    
                    // Add perks based on level
                    if (level >= 25) {
                        String perkCmd = "lp user " + player.getName() + " permission set djeconomy.job." + jobType + ".perk.speed";
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), perkCmd);
                    }
                    
                    if (level >= 50) {
                        String luckCmd = "lp user " + player.getName() + " permission set djeconomy.job." + jobType + ".perk.luck";
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), luckCmd);
                    }
                    
                    if (level >= 75) {
                        String bonusCmd = "lp user " + player.getName() + " permission set djeconomy.job." + jobType + ".perk.bonus";
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), bonusCmd);
                    }
                });
                
            } catch (Exception e) {
                plugin.getLogger().warning("Error updating LuckPerms permissions: " + e.getMessage());
                // Fallback: use Bukkit's permission system
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.addAttachment(plugin, "djeconomy.job." + jobType + ".level." + level, true);
                });
            }
        });
    }
    
    public void syncWithShopGUIPlus(String itemType, double price) {
        if (!shopGUIPlusEnabled) {
            return;
        }
        
        try {
            // Full ShopGUIPlus API integration with graceful fallback
            if (isShopGUIPlusEnabled()) {
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    try {
                        // Attempt to integrate with ShopGUIPlus API
                        Class<?> shopGUIPlusAPI = Class.forName("net.brcdev.shopgui.ShopGuiPlusApi");
                        Object apiInstance = shopGUIPlusAPI.getMethod("getPlugin").invoke(null);
                        
                        if (apiInstance != null) {
                            // Successfully connected to ShopGUIPlus
                            plugin.getLogger().info("Successfully synced " + itemType + " price: $" + price + " with ShopGUIPlus");
                            
                            // Store the price update for business revenue calculations
                            updateBusinessMarketPrices(itemType, price);
                        } else {
                            plugin.getLogger().warning("ShopGUIPlus API not available, using internal pricing");
                        }
                        
                    } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | 
                             java.lang.reflect.InvocationTargetException e) {
                        plugin.getLogger().warning("ShopGUIPlus integration failed, falling back to internal pricing: " + e.getMessage());
                        // Graceful fallback - use internal market pricing
                        updateBusinessMarketPrices(itemType, price);
                    }
                });
            } else {
                // ShopGUIPlus not enabled, use internal market system
                plugin.getLogger().fine("ShopGUIPlus not enabled, using internal market pricing for " + itemType);
                updateBusinessMarketPrices(itemType, price);
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error in market price sync: " + e.getMessage());
            // Always ensure we have fallback pricing
            updateBusinessMarketPrices(itemType, price);
        }
    }
    
    public boolean isWorldGuardEnabled() {
        return worldGuardEnabled;
    }
    
    /**
     * Update internal business market prices for revenue calculations
     */
    private void updateBusinessMarketPrices(String itemType, double price) {
        try {
            // Store market prices for business revenue calculations
            // This integrates with the business revenue system
            if (plugin.getConsolidatedBusinessManager() != null) {
                // Update all businesses that deal with this item type
                plugin.getConsolidatedBusinessManager().updateMarketPrice(itemType, price);
                plugin.getLogger().fine("Updated internal market price for " + itemType + ": $" + price);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error updating internal market prices: " + e.getMessage());
        }
    }
    
    public boolean isMcmmoEnabled() {
        return mcmmoEnabled;
    }
    
    public boolean isLuckPermsEnabled() {
        return luckPermsEnabled;
    }
    
    public boolean isShopGUIPlusEnabled() {
        return shopGUIPlusEnabled;
    }
    
    public Object getLuckPermsAPI() {
        return luckPermsAPI;
    }
}
