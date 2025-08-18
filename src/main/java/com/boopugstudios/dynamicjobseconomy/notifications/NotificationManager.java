package com.boopugstudios.dynamicjobseconomy.notifications;

import com.boopugstudios.dynamicjobseconomy.DynamicJobsEconomy;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class NotificationManager implements Listener {
    
    private final DynamicJobsEconomy plugin;
    
    public NotificationManager(DynamicJobsEconomy plugin) {
        this.plugin = plugin;
        createNotificationTable();
    }
    
    /**
     * Creates the notification table if it doesn't exist
     */
    private void createNotificationTable() {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            boolean isSQLite = "sqlite".equalsIgnoreCase(plugin.getDatabaseManager().getDatabaseType());
            if (isSQLite) {
                String sql = """
                    CREATE TABLE IF NOT EXISTS player_notifications (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        player_uuid TEXT NOT NULL,
                        message TEXT NOT NULL,
                        notification_type TEXT NOT NULL,
                        is_read INTEGER DEFAULT 0,
                        created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                    )
                """;
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.executeUpdate();
                }
                // Create indexes separately in SQLite
                try (Statement st = conn.createStatement()) {
                    st.execute("CREATE INDEX IF NOT EXISTS idx_player_uuid ON player_notifications(player_uuid)");
                    st.execute("CREATE INDEX IF NOT EXISTS idx_is_read ON player_notifications(is_read)");
                }
            } else {
                String sql = """
                    CREATE TABLE IF NOT EXISTS player_notifications (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        player_uuid VARCHAR(36) NOT NULL,
                        message TEXT NOT NULL,
                        notification_type VARCHAR(50) NOT NULL,
                        is_read BOOLEAN DEFAULT FALSE,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        INDEX idx_player_uuid (player_uuid),
                        INDEX idx_is_read (is_read)
                    )
                """;
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error creating notification table", e);
        }
    }
    
    /**
     * Queues a notification for a player (online or offline)
     */
    public void queueNotification(UUID playerUUID, String message, NotificationType type) {
        // Try to send immediately if player is online
        Player onlinePlayer = plugin.getServer().getPlayer(playerUUID);
        if (onlinePlayer != null && onlinePlayer.isOnline()) {
            sendNotificationToPlayer(onlinePlayer, message, type);
            return;
        }
        
        // Store notification for offline player
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = "INSERT INTO player_notifications (player_uuid, message, notification_type) VALUES (?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerUUID.toString());
                stmt.setString(2, message);
                stmt.setString(3, type.name());
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error queuing notification for player " + playerUUID, e);
        }
    }
    
    /**
     * Sends a notification directly to an online player
     */
    private void sendNotificationToPlayer(Player player, String message, NotificationType type) {
        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6DJE§8] ");
        String formattedMessage = prefix + type.getColor() + type.getIcon() + " " + message;
        
        player.sendMessage(formattedMessage);
        
        // Play notification sound if enabled
        if (plugin.getConfig().getBoolean("notifications.play_sound", true)) {
            player.playSound(player.getLocation(), type.getSound(), 0.5f, 1.0f);
        }
    }
    
    /**
     * Gets all unread notifications for a player
     */
    public List<String> getUnreadNotifications(UUID playerUUID) {
        List<String> notifications = new ArrayList<>();
        
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            boolean isSQLite = "sqlite".equalsIgnoreCase(plugin.getDatabaseManager().getDatabaseType());
            String sql = isSQLite
                ? "SELECT message, notification_type FROM player_notifications WHERE player_uuid = ? AND is_read = 0 ORDER BY created_at ASC"
                : "SELECT message, notification_type FROM player_notifications WHERE player_uuid = ? AND is_read = FALSE ORDER BY created_at ASC";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerUUID.toString());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String message = rs.getString("message");
                        NotificationType type = NotificationType.valueOf(rs.getString("notification_type"));
                        notifications.add(type.getColor() + type.getIcon() + " " + message);
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting notifications for player " + playerUUID, e);
        }
        
        return notifications;
    }
    
    /**
     * Marks all notifications as read for a player
     */
    public void markAllAsRead(UUID playerUUID) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            boolean isSQLite = "sqlite".equalsIgnoreCase(plugin.getDatabaseManager().getDatabaseType());
            String sql = isSQLite
                ? "UPDATE player_notifications SET is_read = 1 WHERE player_uuid = ?"
                : "UPDATE player_notifications SET is_read = TRUE WHERE player_uuid = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerUUID.toString());
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error marking notifications as read for player " + playerUUID, e);
        }
    }
    
    /**
     * Cleans up old read notifications (older than 30 days)
     */
    public void cleanupOldNotifications() {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            boolean isSQLite = "sqlite".equalsIgnoreCase(plugin.getDatabaseManager().getDatabaseType());
            String sql = isSQLite
                ? "DELETE FROM player_notifications WHERE is_read = 1 AND created_at < datetime('now','-30 days')"
                : "DELETE FROM player_notifications WHERE is_read = TRUE AND created_at < DATE_SUB(NOW(), INTERVAL 30 DAY)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                int deleted = stmt.executeUpdate();
                if (deleted > 0) {
                    plugin.getLogger().info("Cleaned up " + deleted + " old notifications");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error cleaning up old notifications", e);
        }
    }
    
    /**
     * Handles player join event to show pending notifications
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        
        // Delay notification delivery to avoid login spam
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            List<String> notifications = getUnreadNotifications(playerUUID);
            
            if (!notifications.isEmpty()) {
                String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6DJE§8] ");
                player.sendMessage(prefix + "§e§lYou have " + notifications.size() + " pending notification(s):");
                
                // Show up to 5 most recent notifications
                int maxShow = Math.min(5, notifications.size());
                for (int i = 0; i < maxShow; i++) {
                    player.sendMessage("§7• " + notifications.get(i));
                }
                
                if (notifications.size() > 5) {
                    player.sendMessage("§7... and " + (notifications.size() - 5) + " more. Use §f/djeconomy notifications §7to see all.");
                }
                
                // Mark all as read after showing
                markAllAsRead(playerUUID);
            }
        }, 60L); // 3 second delay
    }
    
    /**
     * Notification types with colors and icons
     */
    public enum NotificationType {
        BUSINESS_PAYROLL("§a", "💰", "ENTITY_EXPERIENCE_ORB_PICKUP"),
        GIG_APPROVED("§a", "✅", "ENTITY_PLAYER_LEVELUP"),
        GIG_REJECTED("§c", "❌", "ENTITY_VILLAGER_NO"),
        GIG_TIMEOUT("§7", "⏰", "BLOCK_NOTE_BLOCK_PLING"),
        BUSINESS_HIRED("§b", "🤝", "ENTITY_PLAYER_LEVELUP"),
        BUSINESS_FIRED("§c", "👋", "ENTITY_VILLAGER_NO"),
        ADMIN_TRANSACTION("§e", "⚡", "ENTITY_EXPERIENCE_ORB_PICKUP"),
        SYSTEM_MESSAGE("§f", "ℹ", "BLOCK_NOTE_BLOCK_PLING");
        
        private final String color;
        private final String icon;
        private final String sound;
        
        NotificationType(String color, String icon, String sound) {
            this.color = color;
            this.icon = icon;
            this.sound = sound;
        }
        
        public String getColor() { return color; }
        public String getIcon() { return icon; }
        public String getSound() { return sound; }
    }
}
