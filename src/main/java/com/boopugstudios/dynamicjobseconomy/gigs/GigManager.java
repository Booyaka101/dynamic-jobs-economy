package com.boopugstudios.dynamicjobseconomy.gigs;

import com.boopugstudios.dynamicjobseconomy.DynamicJobsEconomy;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.logging.Level;

public class GigManager {
    
    private final DynamicJobsEconomy plugin;
    private final Map<Integer, Gig> activeGigs = new HashMap<>();
    
    public GigManager(DynamicJobsEconomy plugin) {
        this.plugin = plugin;
        loadActiveGigs();
        
        // Schedule gig timeout checker to run every hour
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                checkGigTimeouts();
            }
        }, 3600L, 3600L);
    }
    
    public boolean createGig(Player poster, String title, String description, double payment) {
        double postingCost = plugin.getConfig().getDouble("gigs.posting_cost", 50.0);
        double totalCost = postingCost + payment; // Posting cost + escrow payment
        
        // Check if poster has enough money for posting cost + payment (escrow)
        if (!plugin.getEconomyManager().has(poster, totalCost)) {
            return false;
        }
        
        // Withdraw total amount (posting cost + payment goes into escrow)
        if (!plugin.getEconomyManager().withdraw(poster, totalCost)) {
            return false;
        }
        
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = "INSERT INTO gigs (title, description, poster_uuid, payment, status) VALUES (?, ?, ?, ?, 'OPEN')";
            try (PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, title);
                stmt.setString(2, description);
                stmt.setString(3, poster.getUniqueId().toString());
                stmt.setDouble(4, payment);
                
                int result = stmt.executeUpdate();
                if (result > 0) {
                    try (ResultSet keys = stmt.getGeneratedKeys()) {
                        if (keys.next()) {
                            int gigId = keys.getInt(1);
                            Gig gig = new Gig(gigId, title, description, poster.getUniqueId(), payment);
                            activeGigs.put(gigId, gig);
                            return true;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error creating gig", e);
        }
        
        return false;
    }
    
    public boolean acceptGig(Player worker, int gigId) {
        Gig gig = activeGigs.get(gigId);
        if (gig == null || !gig.getStatus().equals("OPEN")) {
            return false;
        }
        
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = "UPDATE gigs SET worker_uuid = ?, status = 'IN_PROGRESS' WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, worker.getUniqueId().toString());
                stmt.setInt(2, gigId);
                
                int result = stmt.executeUpdate();
                if (result > 0) {
                    gig.setWorkerUUID(worker.getUniqueId());
                    gig.setStatus("IN_PROGRESS");
                    return true;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error accepting gig", e);
        }
        
        return false;
    }
    
    public boolean submitCompletion(Player worker, int gigId) {
        Gig gig = activeGigs.get(gigId);
        if (gig == null || !gig.getStatus().equals("IN_PROGRESS") || 
            !worker.getUniqueId().equals(gig.getWorkerUUID())) {
            return false;
        }
        
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = "UPDATE gigs SET status = 'PENDING_APPROVAL', submitted_at = CURRENT_TIMESTAMP WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, gigId);
                
                int result = stmt.executeUpdate();
                if (result > 0) {
                    gig.setStatus("PENDING_APPROVAL");
                    
                    // Notify poster that gig is ready for review
                    Player poster = plugin.getServer().getPlayer(gig.getPosterUUID());
                    if (poster != null && poster.isOnline()) {
                        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6DJE§8] ");
                        poster.sendMessage(prefix + "§e" + worker.getName() + " has submitted completion for gig: §f" + gig.getTitle());
                        poster.sendMessage(prefix + "§7Use §f/gigs review " + gigId + " §7to approve or reject it.");
                    }
                    
                    return true;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error submitting gig completion", e);
        }
        
        return false;
    }
    
    public boolean approveGig(Player poster, int gigId) {
        Gig gig = activeGigs.get(gigId);
        if (gig == null || !gig.getStatus().equals("PENDING_APPROVAL") || 
            !poster.getUniqueId().equals(gig.getPosterUUID())) {
            return false;
        }
        
        double commission = plugin.getConfig().getDouble("gigs.commission_rate", 0.05);
        double workerPayment = gig.getPayment() * (1.0 - commission);
        
        // Use atomic transaction for gig approval
        Connection conn = null;
        try {
            conn = plugin.getDatabaseManager().getConnection();
            conn.setAutoCommit(false); // Start transaction
            
            // Try to pay the worker from escrow
            OfflinePlayer worker = plugin.getServer().getOfflinePlayer(gig.getWorkerUUID());
            boolean paymentSuccessful = false;
            
            // Try online player first, then offline
            Player onlineWorker = plugin.getServer().getPlayer(gig.getWorkerUUID());
            if (onlineWorker != null) {
                paymentSuccessful = plugin.getEconomyManager().deposit(onlineWorker, workerPayment);
            } else {
                paymentSuccessful = plugin.getEconomyManager().depositPlayer(worker, workerPayment);
            }
            
            if (paymentSuccessful) {
                // Payment successful - mark gig as completed
                String sql = "UPDATE gigs SET status = 'COMPLETED', completed_at = CURRENT_TIMESTAMP WHERE id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, gigId);
                    
                    int result = stmt.executeUpdate();
                    if (result > 0) {
                        conn.commit(); // Commit transaction
                        
                        gig.setStatus("COMPLETED");
                        activeGigs.remove(gigId);
                        
                        // Notify worker if online
                        if (onlineWorker != null) {
                            String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6DJE§8] ");
                            onlineWorker.sendMessage(prefix + "§aGig approved! Payment of §f$" + String.format("%.2f", workerPayment) + " §areceived.");
                        }
                        
                        plugin.getLogger().info("Gig #" + gigId + " approved successfully. Worker paid $" + String.format("%.2f", workerPayment));
                        return true;
                    } else {
                        // Database update failed - rollback and refund
                        conn.rollback();
                        refundEscrowToGigPoster(gig, "Database update failed during approval");
                    }
                }
            } else {
                // Worker payment failed - refund escrow to poster
                conn.rollback();
                refundEscrowToGigPoster(gig, "Worker payment failed");
            }
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Database error during gig approval", e);
            
            // Attempt rollback on database error
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to rollback gig approval transaction", rollbackEx);
                }
            }
            
            // Refund escrow to poster due to system error
            refundEscrowToGigPoster(gig, "System error during approval");
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true); // Restore auto-commit
                    conn.close();
                } catch (SQLException e) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to close gig approval database connection", e);
                }
            }
        }
        
        return false;
    }
    
    /**
     * Refunds escrow money to the gig poster when approval fails
     */
    private void refundEscrowToGigPoster(Gig gig, String reason) {
        try {
            OfflinePlayer poster = plugin.getServer().getOfflinePlayer(gig.getPosterUUID());
            double refundAmount = gig.getPayment();
            
            // Try to refund to online player first, then offline
            Player onlinePoster = plugin.getServer().getPlayer(gig.getPosterUUID());
            boolean refundSuccessful = false;
            
            if (onlinePoster != null) {
                refundSuccessful = plugin.getEconomyManager().deposit(onlinePoster, refundAmount);
            } else {
                refundSuccessful = plugin.getEconomyManager().depositPlayer(poster, refundAmount);
            }
            
            if (refundSuccessful) {
                // Update gig status to cancelled
                try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                    String sql = "UPDATE gigs SET status = 'CANCELLED' WHERE id = ?";
                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setInt(1, gig.getId());
                        stmt.executeUpdate();
                    }
                }
                
                gig.setStatus("CANCELLED");
                activeGigs.remove(gig.getId());
                
                // Notify poster if online
                if (onlinePoster != null) {
                    String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6DJE§8] ");
                    onlinePoster.sendMessage(prefix + "§cGig #" + gig.getId() + " approval failed. Escrow refunded: $" + 
                        String.format("%.2f", refundAmount));
                    onlinePoster.sendMessage(prefix + "§7Reason: " + reason);
                }
                
                plugin.getLogger().info("Refunded $" + String.format("%.2f", refundAmount) + 
                    " to gig poster " + poster.getName() + " for gig #" + gig.getId() + ". Reason: " + reason);
            } else {
                plugin.getLogger().severe("CRITICAL: Failed to refund escrow $" + String.format("%.2f", refundAmount) + 
                    " to poster " + poster.getName() + " for gig #" + gig.getId() + ". Manual intervention required!");
                
                // Notify online poster of the issue
                if (onlinePoster != null) {
                    String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6DJE§8] ");
                    onlinePoster.sendMessage(prefix + "§4CRITICAL ERROR: Escrow refund failed! Contact an administrator immediately.");
                    onlinePoster.sendMessage(prefix + "§7Gig ID: #" + gig.getId() + ", Amount: $" + String.format("%.2f", refundAmount));
                }
            }
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Database error during escrow refund for gig #" + gig.getId(), e);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Unexpected error during escrow refund for gig #" + gig.getId(), e);
        }
    }
    
    public boolean rejectGig(Player poster, int gigId, String reason) {
        Gig gig = activeGigs.get(gigId);
        if (gig == null || !gig.getStatus().equals("PENDING_APPROVAL") || 
            !poster.getUniqueId().equals(gig.getPosterUUID())) {
            return false;
        }
        
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = "UPDATE gigs SET status = 'IN_PROGRESS' WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, gigId);
                
                int result = stmt.executeUpdate();
                if (result > 0) {
                    gig.setStatus("IN_PROGRESS");
                    
                    // Notify worker if online
                    Player worker = plugin.getServer().getPlayer(gig.getWorkerUUID());
                    if (worker != null && worker.isOnline()) {
                        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6DJE§8] ");
                        worker.sendMessage(prefix + "§cGig submission rejected by " + poster.getName());
                        if (reason != null && !reason.trim().isEmpty()) {
                            worker.sendMessage(prefix + "§7Reason: " + reason);
                        }
                        worker.sendMessage(prefix + "§7Please continue working and resubmit when ready.");
                    }
                    
                    return true;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error rejecting gig", e);
        }
        
        return false;
    }
    
    public boolean cancelGig(Player poster, int gigId) {
        Gig gig = activeGigs.get(gigId);
        if (gig == null || !poster.getUniqueId().equals(gig.getPosterUUID())) {
            return false;
        }
        
        // Can only cancel if OPEN or if poster wants to cancel IN_PROGRESS (with penalty)
        if (!gig.getStatus().equals("OPEN") && !gig.getStatus().equals("IN_PROGRESS")) {
            return false;
        }
        
        double refundAmount = gig.getPayment();
        boolean isInProgress = gig.getStatus().equals("IN_PROGRESS");
        
        // If gig is in progress, apply cancellation penalty
        if (isInProgress) {
            double penalty = plugin.getConfig().getDouble("gigs.cancellation_penalty", 0.25); // 25% penalty
            refundAmount *= (1.0 - penalty);
        }
        
        // Refund money from escrow
        if (plugin.getEconomyManager().deposit(poster, refundAmount)) {
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                String sql = "UPDATE gigs SET status = 'CANCELLED', cancelled_at = CURRENT_TIMESTAMP WHERE id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, gigId);
                    
                    int result = stmt.executeUpdate();
                    if (result > 0) {
                        gig.setStatus("CANCELLED");
                        activeGigs.remove(gigId);
                        
                        // Notify worker if gig was in progress
                        if (isInProgress && gig.getWorkerUUID() != null) {
                            Player worker = plugin.getServer().getPlayer(gig.getWorkerUUID());
                            if (worker != null && worker.isOnline()) {
                                String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6DJE§8] ");
                                worker.sendMessage(prefix + "§cGig '" + gig.getTitle() + "' has been cancelled by the poster.");
                            }
                        }
                        
                        return true;
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error cancelling gig", e);
            }
        }
        
        return false;
    }
    
    public List<Gig> getOpenGigs() {
        return activeGigs.values().stream()
            .filter(gig -> "OPEN".equals(gig.getStatus()))
            .toList();
    }
    
    public List<Gig> getPlayerGigs(Player player) {
        return activeGigs.values().stream()
            .filter(gig -> player.getUniqueId().equals(gig.getPosterUUID()) || 
                          player.getUniqueId().equals(gig.getWorkerUUID()))
            .toList();
    }
    
    private void loadActiveGigs() {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = "SELECT * FROM gigs WHERE status IN ('OPEN', 'IN_PROGRESS')";
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String title = rs.getString("title");
                    String description = rs.getString("description");
                    UUID posterUUID = UUID.fromString(rs.getString("poster_uuid"));
                    String workerUUIDStr = rs.getString("worker_uuid");
                    UUID workerUUID = workerUUIDStr != null ? UUID.fromString(workerUUIDStr) : null;
                    double payment = rs.getDouble("payment");
                    String status = rs.getString("status");
                    
                    Gig gig = new Gig(id, title, description, posterUUID, payment);
                    gig.setWorkerUUID(workerUUID);
                    gig.setStatus(status);
                    
                    activeGigs.put(id, gig);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading active gigs", e);
        }
    }
    
    public void reload() {
        activeGigs.clear();
        loadActiveGigs();
    }
    
    /**
     * Checks for gigs that have exceeded the timeout period and auto-resolves them
     */
    private void checkGigTimeouts() {
        try {
            int timeoutDays = plugin.getConfig().getInt("gigs.timeout_days", 7);
            long timeoutMillis = timeoutDays * 24L * 60L * 60L * 1000L; // Convert days to milliseconds
            long currentTime = System.currentTimeMillis();
            
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                // Find gigs that are stuck in PENDING_APPROVAL status for too long
                String sql = "SELECT * FROM gigs WHERE status = 'PENDING_APPROVAL' AND created_at < ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setTimestamp(1, new Timestamp(currentTime - timeoutMillis));
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        int timeoutCount = 0;
                        
                        while (rs.next()) {
                            int gigId = rs.getInt("id");
                            UUID posterUUID = UUID.fromString(rs.getString("poster_uuid"));
                            UUID workerUUID = UUID.fromString(rs.getString("worker_uuid"));
                            double payment = rs.getDouble("payment");
                            String title = rs.getString("title");
                            
                            // Auto-approve the gig (worker gets paid)
                            OfflinePlayer worker = plugin.getServer().getOfflinePlayer(workerUUID);
                            
                            double commission = plugin.getConfig().getDouble("gigs.commission_rate", 0.05);
                            double workerPayment = payment * (1.0 - commission);
                            
                            boolean paymentSuccessful = false;
                            Player onlineWorker = plugin.getServer().getPlayer(workerUUID);
                            if (onlineWorker != null) {
                                paymentSuccessful = plugin.getEconomyManager().deposit(onlineWorker, workerPayment);
                            } else {
                                paymentSuccessful = plugin.getEconomyManager().depositPlayer(worker, workerPayment);
                            }
                            
                            if (paymentSuccessful) {
                                // Mark gig as completed
                                String updateSql = "UPDATE gigs SET status = 'COMPLETED', completed_at = CURRENT_TIMESTAMP WHERE id = ?";
                                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                                    updateStmt.setInt(1, gigId);
                                    updateStmt.executeUpdate();
                                }
                                
                                // Remove from active gigs
                                activeGigs.remove(gigId);
                                timeoutCount++;
                                
                                // Notify players if online
                                if (onlineWorker != null) {
                                    String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6DJE§8] ");
                                    onlineWorker.sendMessage(prefix + "§aGig '" + title + "' auto-approved due to timeout. Payment received: $" + 
                                        String.format("%.2f", workerPayment));
                                }
                                
                                Player onlinePoster = plugin.getServer().getPlayer(posterUUID);
                                if (onlinePoster != null) {
                                    String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6DJE§8] ");
                                    onlinePoster.sendMessage(prefix + "§7Gig '" + title + "' was auto-approved due to " + timeoutDays + 
                                        " day timeout. Worker has been paid.");
                                }
                                
                                plugin.getLogger().info("Auto-approved gig #" + gigId + " (" + title + ") due to " + timeoutDays + " day timeout");
                            } else {
                                plugin.getLogger().warning("Failed to auto-approve gig #" + gigId + " - worker payment failed");
                            }
                        }
                        
                        if (timeoutCount > 0) {
                            plugin.getLogger().info("Auto-resolved " + timeoutCount + " timed-out gigs");
                        }
                    }
                }
            }
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error checking gig timeouts", e);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Unexpected error during gig timeout check", e);
        }
    }
}
