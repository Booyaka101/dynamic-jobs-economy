package com.boopugstudios.dynamicjobseconomy.permissions;

import org.bukkit.entity.Player;

/**
 * Comprehensive permissions system for Dynamic Jobs & Economy Pro
 * Provides role-based access control for all business operations
 */
public class BusinessPermissions {
    
    // Base permission node
    private static final String BASE = "djeconomy";
    
    // ==================== CORE BUSINESS PERMISSIONS ====================
    
    // Basic business operations
    public static final String BUSINESS_CREATE = BASE + ".business.create";
    public static final String BUSINESS_DELETE = BASE + ".business.delete";
    public static final String BUSINESS_VIEW = BASE + ".business.view";
    public static final String BUSINESS_EDIT = BASE + ".business.edit";
    public static final String BUSINESS_LIST = BASE + ".business.list";
    public static final String BUSINESS_INFO = BASE + ".business.info";
    
    // Financial operations
    public static final String BUSINESS_DEPOSIT = BASE + ".business.deposit";
    public static final String BUSINESS_WITHDRAW = BASE + ".business.withdraw";
    public static final String BUSINESS_VIEW_BALANCE = BASE + ".business.balance";
    public static final String BUSINESS_TRANSFER = BASE + ".business.transfer";
    
    // ==================== EMPLOYEE MANAGEMENT PERMISSIONS ====================
    
    // Position management
    public static final String POSITION_CREATE = BASE + ".position.create";
    public static final String POSITION_DELETE = BASE + ".position.delete";
    public static final String POSITION_EDIT = BASE + ".position.edit";
    public static final String POSITION_LIST = BASE + ".position.list";
    public static final String POSITION_ACTIVATE = BASE + ".position.activate";
    public static final String POSITION_DEACTIVATE = BASE + ".position.deactivate";
    
    // Employee operations
    public static final String EMPLOYEE_HIRE = BASE + ".employee.hire";
    public static final String EMPLOYEE_FIRE = BASE + ".employee.fire";
    public static final String EMPLOYEE_PROMOTE = BASE + ".employee.promote";
    public static final String EMPLOYEE_LIST = BASE + ".employee.list";
    public static final String EMPLOYEE_VIEW = BASE + ".employee.view";
    public static final String EMPLOYEE_NOTES = BASE + ".employee.notes";
    public static final String EMPLOYEE_HISTORY = BASE + ".employee.history";
    
    // Job offers and applications
    public static final String JOB_APPLY = BASE + ".job.apply";
    public static final String JOB_ACCEPT = BASE + ".job.accept";
    public static final String JOB_REJECT = BASE + ".job.reject";
    public static final String JOB_VIEW_OFFERS = BASE + ".job.view";
    
    // ==================== MINECRAFT-VIABLE FEATURES PERMISSIONS ====================
    
    // Physical business locations
    public static final String LOCATION_CREATE = BASE + ".location.create";
    public static final String LOCATION_DELETE = BASE + ".location.delete";
    public static final String LOCATION_EDIT = BASE + ".location.edit";
    public static final String LOCATION_LIST = BASE + ".location.list";
    public static final String LOCATION_TELEPORT = BASE + ".location.teleport";
    public static final String LOCATION_WORLDGUARD = BASE + ".location.worldguard";
    
    // Resource processing chains
    public static final String PROCESSING_CREATE = BASE + ".processing.create";
    public static final String PROCESSING_DELETE = BASE + ".processing.delete";
    public static final String PROCESSING_EDIT = BASE + ".processing.edit";
    public static final String PROCESSING_LIST = BASE + ".processing.list";
    public static final String PROCESSING_START = BASE + ".processing.start";
    public static final String PROCESSING_STOP = BASE + ".processing.stop";
    
    // Construction contracts
    public static final String CONTRACT_CREATE = BASE + ".contract.create";
    public static final String CONTRACT_DELETE = BASE + ".contract.delete";
    public static final String CONTRACT_EDIT = BASE + ".contract.edit";
    public static final String CONTRACT_LIST = BASE + ".contract.list";
    public static final String CONTRACT_ACCEPT = BASE + ".contract.accept";
    public static final String CONTRACT_COMPLETE = BASE + ".contract.complete";
    public static final String CONTRACT_CANCEL = BASE + ".contract.cancel";
    
    // GUI interface
    public static final String GUI_ACCESS = BASE + ".gui.access";
    public static final String GUI_ADMIN = BASE + ".gui.admin";
    
    // ==================== REVENUE AND ANALYTICS PERMISSIONS ====================
    
    // Revenue management
    public static final String REVENUE_VIEW = BASE + ".revenue.view";
    public static final String REVENUE_GENERATE = BASE + ".revenue.generate";
    public static final String REVENUE_HISTORY = BASE + ".revenue.history";
    public static final String REVENUE_MODEL_SET = BASE + ".revenue.model.set";
    public static final String REVENUE_MODEL_LIST = BASE + ".revenue.model.list";
    
    // Analytics and reports
    public static final String ANALYTICS_PERFORMANCE = BASE + ".analytics.performance";
    public static final String ANALYTICS_PRODUCTIVITY = BASE + ".analytics.productivity";
    public static final String ANALYTICS_EFFECTIVENESS = BASE + ".analytics.effectiveness";
    public static final String ANALYTICS_PROFITABILITY = BASE + ".analytics.profitability";
    public static final String ANALYTICS_POSITION = BASE + ".analytics.position";
    
    // ==================== ADMINISTRATIVE PERMISSIONS ====================
    
    // Admin operations
    public static final String ADMIN_ALL = BASE + ".admin";
    public static final String ADMIN_MANUAL_REVENUE = BASE + ".admin.revenue.manual";
    public static final String ADMIN_DELETE_ANY = BASE + ".admin.delete.any";
    public static final String ADMIN_VIEW_ANY = BASE + ".admin.view.any";
    public static final String ADMIN_EDIT_ANY = BASE + ".admin.edit.any";
    public static final String ADMIN_FORCE_HIRE = BASE + ".admin.hire.force";
    public static final String ADMIN_FORCE_FIRE = BASE + ".admin.fire.force";
    
    // System operations
    public static final String SYSTEM_RELOAD = BASE + ".system.reload";
    public static final String SYSTEM_DEBUG = BASE + ".system.debug";
    public static final String SYSTEM_BACKUP = BASE + ".system.backup";
    
    // ==================== LIMIT PERMISSIONS ====================
    
    // Business limits
    public static final String LIMIT_BUSINESS_UNLIMITED = BASE + ".limit.business.unlimited";
    public static final String LIMIT_EMPLOYEE_UNLIMITED = BASE + ".limit.employee.unlimited";
    public static final String LIMIT_POSITION_UNLIMITED = BASE + ".limit.position.unlimited";
    public static final String LIMIT_LOCATION_UNLIMITED = BASE + ".limit.location.unlimited";
    public static final String LIMIT_CONTRACT_UNLIMITED = BASE + ".limit.contract.unlimited";
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Check if player has permission for basic business operations
     */
    public static boolean canManageBusiness(Player player) {
        return player.hasPermission(BUSINESS_CREATE) || 
               player.hasPermission(BUSINESS_EDIT) || 
               player.hasPermission(ADMIN_ALL);
    }
    
    /**
     * Check if player can manage employees
     */
    public static boolean canManageEmployees(Player player) {
        return player.hasPermission(EMPLOYEE_HIRE) || 
               player.hasPermission(EMPLOYEE_FIRE) || 
               player.hasPermission(ADMIN_ALL);
    }
    
    /**
     * Check if player can access Minecraft-viable features
     */
    public static boolean canAccessMinecraftFeatures(Player player) {
        return player.hasPermission(LOCATION_CREATE) || 
               player.hasPermission(PROCESSING_CREATE) || 
               player.hasPermission(CONTRACT_CREATE) || 
               player.hasPermission(ADMIN_ALL);
    }
    
    /**
     * Check if player can view analytics and reports
     */
    public static boolean canViewAnalytics(Player player) {
        return player.hasPermission(ANALYTICS_PERFORMANCE) || 
               player.hasPermission(ANALYTICS_PRODUCTIVITY) || 
               player.hasPermission(ADMIN_ALL);
    }
    
    /**
     * Check if player is admin
     */
    public static boolean isAdmin(Player player) {
        return player.hasPermission(ADMIN_ALL) || player.isOp();
    }
    
    /**
     * Check if player can perform financial operations
     */
    public static boolean canManageFinances(Player player) {
        return player.hasPermission(BUSINESS_DEPOSIT) || 
               player.hasPermission(BUSINESS_WITHDRAW) || 
               player.hasPermission(ADMIN_ALL);
    }
    
    /**
     * Get maximum number of businesses a player can create
     */
    public static int getMaxBusinesses(Player player) {
        if (player.hasPermission(LIMIT_BUSINESS_UNLIMITED) || isAdmin(player)) {
            return -1; // Unlimited
        }
        
        // Check for specific limits (can be configured via permissions plugins)
        for (int i = 100; i >= 1; i--) {
            if (player.hasPermission(BASE + ".limit.business." + i)) {
                return i;
            }
        }
        
        return 1; // Default limit
    }
    
    /**
     * Get maximum number of employees per business
     */
    public static int getMaxEmployees(Player player) {
        if (player.hasPermission(LIMIT_EMPLOYEE_UNLIMITED) || isAdmin(player)) {
            return -1; // Unlimited
        }
        
        // Check for specific limits
        for (int i = 100; i >= 1; i--) {
            if (player.hasPermission(BASE + ".limit.employee." + i)) {
                return i;
            }
        }
        
        return 10; // Default limit
    }
    
    /**
     * Get maximum number of positions per business
     */
    public static int getMaxPositions(Player player) {
        if (player.hasPermission(LIMIT_POSITION_UNLIMITED) || isAdmin(player)) {
            return -1; // Unlimited
        }
        
        // Check for specific limits
        for (int i = 50; i >= 1; i--) {
            if (player.hasPermission(BASE + ".limit.position." + i)) {
                return i;
            }
        }
        
        return 5; // Default limit
    }
    
    /**
     * Check if player owns a specific business or is admin
     */
    public static boolean canAccessBusiness(Player player, String businessOwnerUUID) {
        return player.getUniqueId().toString().equals(businessOwnerUUID) || isAdmin(player);
    }
    
    /**
     * Send permission denied message
     */
    public static void sendPermissionDenied(Player player, String action) {
        player.sendMessage("§c✗ You don't have permission to " + action + ".");
        player.sendMessage("§7Contact an administrator if you believe this is an error.");
    }
    
    /**
     * Send permission denied message with required permission
     */
    public static void sendPermissionDenied(Player player, String action, String requiredPermission) {
        player.sendMessage("§c✗ You don't have permission to " + action + ".");
        player.sendMessage("§7Required permission: §e" + requiredPermission);
        player.sendMessage("§7Contact an administrator if you believe this is an error.");
    }
}
