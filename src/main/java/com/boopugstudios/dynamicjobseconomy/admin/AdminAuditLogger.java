package com.boopugstudios.dynamicjobseconomy.admin;

import com.boopugstudios.dynamicjobseconomy.DynamicJobsEconomy;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;

public class AdminAuditLogger {
    
    private final DynamicJobsEconomy plugin;
    
    public AdminAuditLogger(DynamicJobsEconomy plugin) {
        this.plugin = plugin;
        createAuditTable();
    }
    
    /**
     * Creates the admin audit table if it doesn't exist
     */
    private void createAuditTable() {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = """
                CREATE TABLE IF NOT EXISTS admin_audit_log (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    admin_uuid VARCHAR(36),
                    admin_name VARCHAR(50) NOT NULL,
                    action_type VARCHAR(50) NOT NULL,
                    target_player VARCHAR(50),
                    amount DECIMAL(15,2),
                    details TEXT,
                    server_name VARCHAR(100),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_admin_uuid (admin_uuid),
                    INDEX idx_action_type (action_type),
                    INDEX idx_created_at (created_at)
                )
            """;
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error creating admin audit table", e);
        }
    }
    
    /**
     * Logs an admin economy action
     */
    public void logEconomyAction(CommandSender admin, String action, String targetPlayer, double amount) {
        logAction(admin, "ECONOMY_" + action.toUpperCase(), targetPlayer, amount, 
            "Economy action: " + action + " $" + String.format("%.2f", amount) + " for " + targetPlayer);
    }
    
    /**
     * Logs an admin job action
     */
    public void logJobAction(CommandSender admin, String action, String targetPlayer, String jobName, int level) {
        logAction(admin, "JOB_" + action.toUpperCase(), targetPlayer, level, 
            "Job action: " + action + " " + jobName + " level " + level + " for " + targetPlayer);
    }
    
    /**
     * Logs a general admin action
     */
    public void logAction(CommandSender admin, String actionType, String targetPlayer, double amount, String details) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = """
                INSERT INTO admin_audit_log (admin_uuid, admin_name, action_type, target_player, amount, details, server_name) 
                VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                if (admin instanceof Player) {
                    Player adminPlayer = (Player) admin;
                    stmt.setString(1, adminPlayer.getUniqueId().toString());
                    stmt.setString(2, adminPlayer.getName());
                } else {
                    stmt.setString(1, null); // Console
                    stmt.setString(2, "CONSOLE");
                }
                
                stmt.setString(3, actionType);
                stmt.setString(4, targetPlayer);
                stmt.setDouble(5, amount);
                stmt.setString(6, details);
                stmt.setString(7, plugin.getServer().getName());
                
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error logging admin action", e);
        }
    }
    
    /**
     * Cleans up old audit logs (older than 90 days)
     */
    public void cleanupOldAuditLogs() {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = "DELETE FROM admin_audit_log WHERE created_at < DATE_SUB(NOW(), INTERVAL 90 DAY)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                int deleted = stmt.executeUpdate();
                if (deleted > 0) {
                    plugin.getLogger().info("Cleaned up " + deleted + " old audit log entries");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error cleaning up old audit logs", e);
        }
    }
}
