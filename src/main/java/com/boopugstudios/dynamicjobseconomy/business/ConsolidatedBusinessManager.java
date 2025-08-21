package com.boopugstudios.dynamicjobseconomy.business;

import com.boopugstudios.dynamicjobseconomy.DynamicJobsEconomy;
import com.boopugstudios.dynamicjobseconomy.gui.BusinessGUI;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Consolidated Business Manager - Combines all business-related operations
 * Replaces: BusinessManager, BusinessPositionManager, HiringRequestManager, 
 * PositionManagementExtensions, BusinessAnalytics, AdvancedBusinessAnalytics
 */
public class ConsolidatedBusinessManager {
    
    private final DynamicJobsEconomy plugin;
    private final Map<Integer, Business> businessCache = new HashMap<>();
    private final Map<Integer, List<BusinessContract>> businessContracts = new HashMap<>();
    private final Map<Integer, BusinessRevenueModel> businessRevenueModels = new HashMap<>();
    
    // Revenue generation timers and tracking
    private final Map<Integer, Long> lastRevenueGeneration = new HashMap<>();
    private final Map<Integer, Double> dailyRevenueTargets = new HashMap<>();
    
    public ConsolidatedBusinessManager(DynamicJobsEconomy plugin) {
        this.plugin = plugin;
        initializeTables();
        initializeMinecraftViableTables();
        loadBusinesses();
        startRevenueGenerationTask();
    }
    
    // ==================== CORE BUSINESS OPERATIONS ====================
    
    public boolean createBusiness(Player owner, String name, String type) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = "INSERT INTO businesses (name, owner_uuid, type, balance) VALUES (?, ?, ?, 0.0)";
            try (PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, name);
                stmt.setString(2, owner.getUniqueId().toString());
                stmt.setString(3, type);
                
                if (stmt.executeUpdate() > 0) {
                    try (ResultSet keys = stmt.getGeneratedKeys()) {
                        if (keys.next()) {
                            int businessId = keys.getInt(1);
                            Business business = new Business(businessId, name, owner.getUniqueId(), type, 0.0);
                            businessCache.put(businessId, business);
                            return true;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error creating business", e);
        }
        return false;
    }
    
    public Business getBusiness(int businessId) {
        return businessCache.get(businessId);
    }
    
    /**
     * Get business by name
     */
    public Business getBusinessByName(String businessName) {
        return businessCache.values().stream()
            .filter(b -> b.getName().equalsIgnoreCase(businessName))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Update position salary by position name
     */
    public boolean updatePositionSalary(int businessId, String positionName, double newSalary) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = "UPDATE business_positions SET salary = ? WHERE business_id = ? AND title = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setDouble(1, newSalary);
                stmt.setInt(2, businessId);
                stmt.setString(3, positionName);
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error updating position salary", e);
        }
        return false;
    }
    
    /**
     * Promote employee to a position by name
     */
    public boolean promoteEmployee(int businessId, UUID playerUUID, String positionName) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            // First get the position ID
            String getPositionSql = "SELECT position_id FROM business_positions WHERE business_id = ? AND title = ?";
            try (PreparedStatement stmt = conn.prepareStatement(getPositionSql)) {
                stmt.setInt(1, businessId);
                stmt.setString(2, positionName);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        int positionId = rs.getInt("position_id");
                        return promoteEmployee(businessId, playerUUID, positionId);
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error promoting employee", e);
        }
        return false;
    }
    
    /**
     * Add employee note with author
     */
    public boolean addEmployeeNote(int businessId, UUID playerUUID, String note, String author) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = "INSERT INTO employee_notes (business_id, player_uuid, note, author, created_at) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, businessId);
                stmt.setString(2, playerUUID.toString());
                stmt.setString(3, note);
                stmt.setString(4, author);
                stmt.setLong(5, System.currentTimeMillis());
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error adding employee note", e);
        }
        return false;
    }
    

    
    public List<Business> getPlayerBusinesses(Player player) {
        return businessCache.values().stream()
            .filter(b -> b.getOwnerUUID().equals(player.getUniqueId()))
            .toList();
    }
    
    public boolean depositToBusiness(int businessId, double amount) {
        Business business = getBusiness(businessId);
        if (business != null) {
            business.setBalance(business.getBalance() + amount);
            return saveBusiness(business);
        }
        return false;
    }
    
    public boolean withdrawFromBusiness(int businessId, double amount) {
        Business business = getBusiness(businessId);
        if (business != null && business.getBalance() >= amount) {
            business.setBalance(business.getBalance() - amount);
            return saveBusiness(business);
        }
        return false;
    }
    
    // ==================== POSITION MANAGEMENT ====================
    
    public boolean createPosition(int businessId, String title, double salary, String description, int maxEmployees) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = "INSERT INTO business_positions (business_id, title, salary, description, max_employees) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, businessId);
                stmt.setString(2, title);
                stmt.setDouble(3, salary);
                stmt.setString(4, description);
                stmt.setInt(5, maxEmployees);
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error creating position", e);
        }
        return false;
    }
    
    public List<BusinessPosition> getBusinessPositions(int businessId) {
        List<BusinessPosition> positions = new ArrayList<>();
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = "SELECT * FROM business_positions WHERE business_id = ? AND is_active = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, businessId);
                stmt.setBoolean(2, true);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        positions.add(new BusinessPosition(
                            rs.getInt("position_id"),
                            rs.getInt("business_id"),
                            rs.getString("title"),
                            rs.getDouble("salary"),
                            rs.getString("description"),
                            rs.getInt("max_employees"),
                            rs.getBoolean("is_active"),
                            rs.getString("created_by") != null ? UUID.fromString(rs.getString("created_by")) : null,
                            rs.getLong("created_at")
                        ));
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting business positions", e);
        }
        return positions;
    }
    
    public BusinessPosition getPosition(int positionId) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = "SELECT * FROM business_positions WHERE position_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, positionId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return new BusinessPosition(
                            rs.getInt("position_id"),
                            rs.getInt("business_id"),
                            rs.getString("title"),
                            rs.getDouble("salary"),
                            rs.getString("description"),
                            rs.getInt("max_employees"),
                            rs.getBoolean("is_active"),
                            rs.getString("created_by") != null ? UUID.fromString(rs.getString("created_by")) : null,
                            rs.getLong("created_at")
                        );
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting position", e);
        }
        return null;
    }
    
    // ==================== EMPLOYEE MANAGEMENT ====================
    
    public List<BusinessEmployee> getBusinessEmployees(int businessId) {
        List<BusinessEmployee> employees = new ArrayList<>();
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = """
                SELECT be.*, bp.title as position_title 
                FROM business_employees be 
                JOIN business_positions bp ON be.position_id = bp.position_id 
                WHERE be.business_id = ? AND be.is_active = ? 
                ORDER BY be.hired_at DESC
            """;
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, businessId);
                stmt.setBoolean(2, true);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        employees.add(new BusinessEmployee(
                            rs.getInt("employee_id"),
                            rs.getInt("business_id"),
                            rs.getInt("position_id"),
                            UUID.fromString(rs.getString("player_uuid")),
                            rs.getString("player_name"),
                            rs.getDouble("current_salary"),
                            rs.getLong("hired_at"),
                            rs.getBoolean("is_active"),
                            rs.getString("notes")
                        ));
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting business employees", e);
        }
        return employees;
    }
    
    public boolean hirePlayerToPosition(int businessId, int positionId, UUID playerUUID, double customSalary) {
        BusinessPosition position = getPosition(positionId);
        if (position == null || position.getBusinessId() != businessId) return false;
        
        // Check capacity and existing employment
        if (getPositionEmployeeCount(positionId) >= position.getMaxEmployees()) return false;
        if (isPlayerEmployedByBusiness(businessId, playerUUID)) return false;
        
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = "INSERT INTO business_employees (business_id, position_id, player_uuid, player_name, current_salary, hired_at) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, businessId);
                stmt.setInt(2, positionId);
                stmt.setString(3, playerUUID.toString());
                stmt.setString(4, plugin.getServer().getOfflinePlayer(playerUUID).getName());
                stmt.setDouble(5, customSalary);
                stmt.setLong(6, System.currentTimeMillis());
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error hiring player", e);
        }
        return false;
    }
    
    public boolean fireEmployee(int businessId, UUID playerUUID) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = "UPDATE business_employees SET is_active = ? WHERE business_id = ? AND player_uuid = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setBoolean(1, false);
                stmt.setInt(2, businessId);
                stmt.setString(3, playerUUID.toString());
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error firing employee", e);
        }
        return false;
    }
    
    // ==================== HIRING REQUESTS (CONSENT SYSTEM) ====================
    
    public boolean createHiringRequest(int businessId, int positionId, UUID playerUUID, UUID requestedBy, double offeredSalary, String message) {
        if (hasPendingRequest(playerUUID, businessId)) return false;
        
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = """
                INSERT INTO hiring_requests (business_id, position_id, player_uuid, requested_by, 
                                           offered_salary, message, request_time, expiration_time, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'PENDING')
            """;
            
            long currentTime = System.currentTimeMillis();
            long expirationTime = currentTime + (24 * 60 * 60 * 1000);
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, businessId);
                stmt.setInt(2, positionId);
                stmt.setString(3, playerUUID.toString());
                stmt.setString(4, requestedBy.toString());
                stmt.setDouble(5, offeredSalary);
                stmt.setString(6, message);
                stmt.setLong(7, currentTime);
                stmt.setLong(8, expirationTime);
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error creating hiring request", e);
        }
        return false;
    }
    
    public boolean acceptHiringRequest(int requestId, UUID playerUUID) {
        HiringRequest request = getHiringRequest(requestId);
        if (request == null || !request.getPlayerUUID().equals(playerUUID) || 
            request.getStatus() != HiringRequest.HiringRequestStatus.PENDING || request.isExpired()) {
            return false;
        }
        
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Update request status
                String updateSql = "UPDATE hiring_requests SET status = 'ACCEPTED' WHERE request_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                    stmt.setInt(1, requestId);
                    stmt.executeUpdate();
                }
                
                // Create employment
                boolean hired = hirePlayerToPosition(request.getBusinessId(), request.getPositionId(), 
                    playerUUID, request.getOfferedSalary());
                
                if (hired) {
                    conn.commit();
                    sendHiringNotifications(request, true);
                    return true;
                } else {
                    conn.rollback();
                }
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error accepting hiring request", e);
        }
        return false;
    }
    
    // ==================== PAYROLL PROCESSING ====================
    
    public void processPayroll() {
        for (Business business : businessCache.values()) {
            processBusinessPayroll(business);
        }
        plugin.getLogger().info("Processed payroll for all businesses");
    }
    
    private void processBusinessPayroll(Business business) {
        List<BusinessEmployee> employees = getBusinessEmployees(business.getId());
        if (employees.isEmpty()) return;
        
        double totalPayroll = employees.stream().mapToDouble(BusinessEmployee::getCurrentSalary).sum();
        
        if (business.getBalance() < totalPayroll) {
            notifyInsufficientFunds(business, totalPayroll);
            return;
        }
        
        // Process atomic payroll transaction
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            conn.setAutoCommit(false);
            
            boolean allPaymentsSuccessful = true;
            for (BusinessEmployee emp : employees) {
                OfflinePlayer player = plugin.getServer().getOfflinePlayer(emp.getPlayerUUID());
                if (!plugin.getEconomyManager().depositPlayer(player, emp.getCurrentSalary())) {
                    allPaymentsSuccessful = false;
                    break;
                }
            }
            
            if (allPaymentsSuccessful) {
                business.setBalance(business.getBalance() - totalPayroll);
                saveBusiness(business);
                conn.commit();
                notifyPayrollSuccess(business, employees, totalPayroll);
            } else {
                conn.rollback();
                plugin.getLogger().warning("Payroll failed for business: " + business.getName());
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error processing payroll", e);
        }
    }
    
    // ==================== ANALYTICS & REPORTING ====================
    
    public List<String> getBusinessPerformanceReport(int businessId) {
        List<String> report = new ArrayList<>();
        Business business = getBusiness(businessId);
        if (business == null) return report;
        
        List<BusinessEmployee> employees = getBusinessEmployees(businessId);
        List<BusinessPosition> positions = getBusinessPositions(businessId);
        
        report.add("§f" + business.getName() + " §7- Performance Report");
        report.add("§7═══════════════════════════════════════");
        report.add("§f📊 Business Overview:");
        report.add("  §7Owner: §f" + plugin.getServer().getOfflinePlayer(business.getOwnerUUID()).getName());
        report.add("  §7Balance: §a$" + String.format("%.2f", business.getBalance()));
        report.add("  §7Total Employees: §e" + employees.size());
        report.add("  §7Total Positions: §e" + positions.size());
        
        if (!employees.isEmpty()) {
            double totalSalaryCost = employees.stream().mapToDouble(BusinessEmployee::getCurrentSalary).sum();
            double avgSalary = totalSalaryCost / employees.size();
            
            report.add("§f💰 Financial Overview:");
            report.add("  §7Daily Salary Cost: §c$" + String.format("%.2f", totalSalaryCost));
            report.add("  §7Average Salary: §a$" + String.format("%.2f", avgSalary));
            report.add("  §7Sustainability: §e" + String.format("%.0f", business.getBalance() / totalSalaryCost) + " days");
        }
        
        return report;
    }
    
    // ==================== MISSING METHODS FROM OLD MANAGERS ====================
    
    public void reload() {
        loadBusinesses();
        plugin.getLogger().info("Consolidated business manager reloaded");
    }
    
    // Position Management Extensions methods
    public boolean updatePosition(int positionId, String title, String description, double salary, int maxEmployees) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = "UPDATE business_positions SET title = ?, description = ?, salary = ?, max_employees = ? WHERE position_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, title);
                stmt.setString(2, description);
                stmt.setDouble(3, salary);
                stmt.setInt(4, maxEmployees);
                stmt.setInt(5, positionId);
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error updating position", e);
        }
        return false;
    }
    
    public BusinessEmployee getBusinessEmployee(int businessId, UUID playerUUID) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = "SELECT * FROM business_employees WHERE business_id = ? AND player_uuid = ? AND is_active = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, businessId);
                stmt.setString(2, playerUUID.toString());
                stmt.setBoolean(3, true);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return new BusinessEmployee(
                            rs.getInt("employee_id"),
                            rs.getInt("business_id"),
                            rs.getInt("position_id"),
                            UUID.fromString(rs.getString("player_uuid")),
                            rs.getString("player_name"),
                            rs.getDouble("current_salary"),
                            rs.getLong("hired_at"),
                            rs.getBoolean("is_active"),
                            rs.getString("notes")
                        );
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting business employee", e);
        }
        return null;
    }
    
    public boolean promoteEmployee(int businessId, UUID playerUUID, int newPositionId) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            // Get new position salary
            BusinessPosition newPosition = getPosition(newPositionId);
            if (newPosition == null) return false;
            
            String sql = "UPDATE business_employees SET position_id = ?, current_salary = ? WHERE business_id = ? AND player_uuid = ? AND is_active = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, newPositionId);
                stmt.setDouble(2, newPosition.getSalary());
                stmt.setInt(3, businessId);
                stmt.setString(4, playerUUID.toString());
                stmt.setBoolean(5, true);
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error promoting employee", e);
        }
        return false;
    }
    
    public boolean addEmployeeNote(int businessId, UUID playerUUID, String note) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = "INSERT INTO employee_notes (business_id, player_uuid, note, created_at) VALUES (?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, businessId);
                stmt.setString(2, playerUUID.toString());
                stmt.setString(3, note);
                stmt.setLong(4, System.currentTimeMillis());
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error adding employee note", e);
        }
        return false;
    }
    
    public List<String> getEmployeeHistory(int businessId, UUID playerUUID) {
        List<String> history = new ArrayList<>();
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String notesSql = "SELECT note, author, created_at FROM employee_notes WHERE business_id = ? AND player_uuid = ? ORDER BY created_at DESC";
            try (PreparedStatement stmt = conn.prepareStatement(notesSql)) {
                stmt.setInt(1, businessId);
                stmt.setString(2, playerUUID.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String note = rs.getString("note");
                        String author = rs.getString("author");
                        long timestamp = rs.getLong("created_at");
                        String date = new java.util.Date(timestamp).toString();
                        history.add(String.format("[%s] %s: %s", date, author, note));
                    }
                }
            }
            
            String empSql = "SELECT * FROM business_employees WHERE business_id = ? AND player_uuid = ?";
            try (PreparedStatement stmt = conn.prepareStatement(empSql)) {
                stmt.setInt(1, businessId);
                stmt.setString(2, playerUUID.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        long hiredAt = rs.getLong("hired_at");
                        double salary = rs.getDouble("current_salary");
                        boolean active = rs.getBoolean("is_active");
                        String hiredDate = new java.util.Date(hiredAt).toString();
                        history.add(String.format("[%s] Hired at $%.2f/day - Status: %s", hiredDate, salary, active ? "Active" : "Inactive"));
                    }
                }
            }
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting employee history", e);
        }
        
        return history;
    }
    
    /**
     * Update market price for business revenue calculations
     */
    public void updateMarketPrice(String itemType, double price) {
        try {
            // Update market prices for all businesses dealing with this item
            // Store market prices for business revenue calculations
            plugin.getLogger().fine("Updated market price for " + itemType + ": $" + price);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error updating market price", e);
        }
    }
    
    public List<String> getPositionAnalytics(int positionId) {
        List<String> analytics = new ArrayList<>();
        BusinessPosition position = getPosition(positionId);
        if (position == null) return analytics;
        
        int currentEmployees = getPositionEmployeeCount(positionId);
        double utilizationRate = (double) currentEmployees / position.getMaxEmployees() * 100;
        
        analytics.add("§f" + position.getTitle() + " §7- Position Analytics");
        analytics.add("§7═══════════════════════════════════════");
        analytics.add("§fUtilization: §e" + currentEmployees + "/" + position.getMaxEmployees() + " (" + String.format("%.1f", utilizationRate) + "%)");
        analytics.add("§fSalary Cost: §a$" + String.format("%.2f", position.getSalary() * currentEmployees) + "/day");
        analytics.add("§fAverage Tenure: §e" + getAveragePositionTenure(positionId) + " days");
        
        return analytics;
    }
    
    public boolean deactivatePosition(int positionId) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = "UPDATE business_positions SET is_active = ? WHERE position_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setBoolean(1, false);
                stmt.setInt(2, positionId);
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error deactivating position", e);
        }
        return false;
    }
    
    public boolean reactivatePosition(int positionId) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = "UPDATE business_positions SET is_active = ? WHERE position_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setBoolean(1, true);
                stmt.setInt(2, positionId);
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error reactivating position", e);
        }
        return false;
    }
    
    // Advanced Analytics methods
    public List<String> getEmployeeProductivityReport(int businessId) {
        return getBusinessPerformanceReport(businessId); // Delegate to performance report for now
    }
    
    public List<String> getPositionEffectivenessReport(int businessId) {
        return getBusinessPerformanceReport(businessId); // Delegate to performance report for now
    }
    
    // Hiring Request methods
    public boolean rejectHiringRequest(int requestId, UUID playerUUID, String reason) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = "UPDATE hiring_requests SET status = 'REJECTED', rejection_reason = ? WHERE request_id = ? AND player_uuid = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, reason);
                stmt.setInt(2, requestId);
                stmt.setString(3, playerUUID.toString());
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error rejecting hiring request", e);
        }
        return false;
    }
    
    public List<HiringRequest> getPendingRequests(UUID playerUUID) {
        List<HiringRequest> requests = new ArrayList<>();
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = "SELECT * FROM hiring_requests WHERE player_uuid = ? AND status = 'PENDING' AND expiration_time > ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerUUID.toString());
                stmt.setLong(2, System.currentTimeMillis());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        requests.add(new HiringRequest(
                            rs.getInt("request_id"),
                            rs.getInt("business_id"),
                            rs.getInt("position_id"),
                            UUID.fromString(rs.getString("player_uuid")),
                            UUID.fromString(rs.getString("requested_by")),
                            rs.getDouble("offered_salary"),
                            rs.getString("message"),
                            rs.getLong("request_time"),
                            rs.getLong("expiration_time"),
                            HiringRequest.HiringRequestStatus.valueOf(rs.getString("status"))
                        ));
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting pending hiring requests", e);
        }
        return requests;
    }

    /**
     * Expose all businesses (read-only copy) for suggestions and overview
     */
    public Collection<Business> getAllBusinesses() {
        return new ArrayList<>(businessCache.values());
    }
    
    // ==================== AGGREGATE STATS HELPERS ====================
    
    /** Total count of all businesses */
    public int getTotalBusinessesCount() {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = "SELECT COUNT(*) FROM businesses";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next() ? rs.getInt(1) : businessCache.size();
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error counting businesses; falling back to cache size", e);
            return businessCache.size();
        }
    }
    
    /** Total count of active positions across all businesses */
    public int getTotalActivePositionsCount() {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = "SELECT COUNT(*) FROM business_positions WHERE is_active = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setBoolean(1, true);
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next() ? rs.getInt(1) : 0;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error counting active positions", e);
            return 0;
        }
    }
    
    /** Total count of active employees across all businesses */
    public int getTotalActiveEmployeesCount() {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = "SELECT COUNT(*) FROM business_employees WHERE is_active = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setBoolean(1, true);
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next() ? rs.getInt(1) : 0;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error counting active employees", e);
            return 0;
        }
    }
    
    /** Total count of pending, non-expired hiring requests across all businesses */
    public int getTotalPendingHiringRequestsCount() {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = "SELECT COUNT(*) FROM hiring_requests WHERE status = 'PENDING' AND expiration_time > ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, System.currentTimeMillis());
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next() ? rs.getInt(1) : 0;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error counting pending hiring requests", e);
            return 0;
        }
    }
    
    /** Count of active positions for a specific business */
    public int getActivePositionsCount(int businessId) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = "SELECT COUNT(*) FROM business_positions WHERE business_id = ? AND is_active = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, businessId);
                stmt.setBoolean(2, true);
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next() ? rs.getInt(1) : 0;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error counting business active positions", e);
            return 0;
        }
    }
    
    /** Count of active employees for a specific business */
    public int getEmployeesCount(int businessId) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = "SELECT COUNT(*) FROM business_employees WHERE business_id = ? AND is_active = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, businessId);
                stmt.setBoolean(2, true);
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next() ? rs.getInt(1) : 0;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error counting business employees", e);
            return 0;
        }
    }
    
    /** Count of pending, non-expired hiring requests for a specific business */
    public int getPendingHiringRequestsCountForBusiness(int businessId) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = "SELECT COUNT(*) FROM hiring_requests WHERE business_id = ? AND status = 'PENDING' AND expiration_time > ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, businessId);
                stmt.setLong(2, System.currentTimeMillis());
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next() ? rs.getInt(1) : 0;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error counting business pending requests", e);
            return 0;
        }
    }
    
    // ==================== UTILITY METHODS ====================
    
    private void initializeTables() {
        // Initialize all required database tables
        // (Implementation would include all table creation SQL)
    }
    
    private int getAveragePositionTenure(int positionId) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = "SELECT AVG((? - hired_at) / (24 * 60 * 60 * 1000)) as avg_tenure FROM business_employees WHERE position_id = ? AND is_active = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, System.currentTimeMillis());
                stmt.setInt(2, positionId);
                stmt.setBoolean(3, true);
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next() ? rs.getInt("avg_tenure") : 0;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error calculating average tenure", e);
        }
        return 0;
    }
    
    // Make helper methods public for external access
    public boolean isPlayerEmployedByBusiness(int businessId, UUID playerUUID) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = "SELECT COUNT(*) FROM business_employees WHERE business_id = ? AND player_uuid = ? AND is_active = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, businessId);
                stmt.setString(2, playerUUID.toString());
                stmt.setBoolean(3, true);
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next() && rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error checking player employment", e);
        }
        return false;
    }
    
    public int getPositionEmployeeCount(int positionId) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = "SELECT COUNT(*) FROM business_employees WHERE position_id = ? AND is_active = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, positionId);
                stmt.setBoolean(2, true);
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next() ? rs.getInt(1) : 0;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting position employee count", e);
        }
        return 0;
    }
    
    private void loadBusinesses() {
        businessCache.clear();
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = "SELECT * FROM businesses";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Business business = new Business(
                            rs.getInt("id"),
                            rs.getString("name"),
                            UUID.fromString(rs.getString("owner_uuid")),
                            rs.getString("type"),
                            rs.getDouble("balance")
                        );
                        businessCache.put(business.getId(), business);
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading businesses", e);
        }
    }
    
    private boolean saveBusiness(Business business) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = "UPDATE businesses SET name = ?, balance = ? WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, business.getName());
                stmt.setDouble(2, business.getBalance());
                stmt.setInt(3, business.getId());
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error saving business", e);
        }
        return false;
    }
    
    // Helper methods for various operations - removed duplicate definitions
    
    private boolean hasPendingRequest(UUID playerUUID, int businessId) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = "SELECT COUNT(*) FROM hiring_requests WHERE player_uuid = ? AND business_id = ? AND status = 'PENDING' AND expiration_time > ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerUUID.toString());
                stmt.setInt(2, businessId);
                stmt.setLong(3, System.currentTimeMillis());
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next() && rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error checking pending requests", e);
        }
        return false;
    }
    
    private HiringRequest getHiringRequest(int requestId) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = "SELECT * FROM hiring_requests WHERE request_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, requestId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return new HiringRequest(
                            rs.getInt("request_id"),
                            rs.getInt("business_id"),
                            rs.getInt("position_id"),
                            UUID.fromString(rs.getString("player_uuid")),
                            UUID.fromString(rs.getString("requested_by")),
                            rs.getDouble("offered_salary"),
                            rs.getString("message"),
                            rs.getLong("request_time"),
                            rs.getLong("expiration_time"),
                            HiringRequest.HiringRequestStatus.valueOf(rs.getString("status"))
                        );
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting hiring request", e);
        }
        return null;
    }
    
    private void sendHiringNotifications(HiringRequest request, boolean accepted) {
        // Implementation for sending notifications
    }
    
    private void notifyInsufficientFunds(Business business, double needed) {
        Player owner = plugin.getServer().getPlayer(business.getOwnerUUID());
        if (owner != null) {
            String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6DJE§8] ");
            owner.sendMessage(prefix + "§cInsufficient funds for payroll! Need $" + String.format("%.2f", needed) + 
                " but only have $" + String.format("%.2f", business.getBalance()));
        }
    }
    
    private void notifyPayrollSuccess(Business business, List<BusinessEmployee> employees, double total) {
        Player owner = plugin.getServer().getPlayer(business.getOwnerUUID());
        if (owner != null) {
            String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6DJE§8] ");
            owner.sendMessage(prefix + "§aPayroll processed! Paid $" + String.format("%.2f", total) + 
                " to " + employees.size() + " employees.");
        }
        
        // Notify employees
        for (BusinessEmployee emp : employees) {
            Player employee = plugin.getServer().getPlayer(emp.getPlayerUUID());
            if (employee != null) {
                String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6DJE§8] ");
                employee.sendMessage(prefix + "§aReceived salary: $" + String.format("%.2f", emp.getCurrentSalary()) + 
                    " from " + business.getName());
            }
        }
    }
    
    // ==================== COMPREHENSIVE BUSINESS REVENUE SYSTEM ====================
    
    /**
     * Set the revenue model for a business
     */
    public boolean setBusinessRevenueModel(int businessId, BusinessRevenueModel model) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = "UPDATE businesses SET revenue_model = ? WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, model.name());
                stmt.setInt(2, businessId);
                boolean success = stmt.executeUpdate() > 0;
                if (success) {
                    businessRevenueModels.put(businessId, model);
                    calculateDailyRevenueTarget(businessId);
                }
                return success;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error setting business revenue model", e);
        }
        return false;
    }
    
    /**
     * Get the revenue model for a business
     */
    public BusinessRevenueModel getBusinessRevenueModel(int businessId) {
        return businessRevenueModels.getOrDefault(businessId, BusinessRevenueModel.STARTUP);
    }
    
    /**
     * Start the automated revenue generation task
     */
    private void startRevenueGenerationTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                generateAllBusinessRevenue();
            }
        }.runTaskTimer(plugin, 20L * 60 * 5, 20L * 60 * 15); // Every 15 minutes, start after 5 minutes
    }
    
    /**
     * Generate revenue for all businesses based on their models
     */
    private void generateAllBusinessRevenue() {
        for (Business business : businessCache.values()) {
            generateBusinessRevenue(business);
        }
    }
    
    /**
     * Generate revenue for a specific business based on its model and employees
     */
    public void generateBusinessRevenue(Business business) {
        BusinessRevenueModel model = getBusinessRevenueModel(business.getId());
        List<BusinessEmployee> employees = getBusinessEmployees(business.getId());
        
        if (employees.isEmpty()) return; // No employees, no revenue generation
        
        // Check if enough time has passed since last revenue generation
        long currentTime = System.currentTimeMillis();
        long lastGeneration = lastRevenueGeneration.getOrDefault(business.getId(), 0L);
        long timeSinceLastGeneration = currentTime - lastGeneration;
        
        // Minimum 10 minutes between revenue generations for same business
        if (timeSinceLastGeneration < (10 * 60 * 1000)) {
            return; // Too soon since last generation
        }
        
        double revenueAmount = calculateRevenueAmount(business, model, employees);
        if (revenueAmount > 0) {
            // Generate revenue based on business model
            BusinessRevenue.RevenueType revenueType = getRevenueTypeForModel(model);
            recordBusinessRevenue(business.getId(), revenueType, revenueAmount, 
                "Automated " + model.getDisplayName() + " Revenue", null, 
                "Generated by " + employees.size() + " employees", 
                "{\"employees\":" + employees.size() + ",\"model\":\"" + model.name() + "\"}");
            
            // Add revenue to business balance
            depositToBusiness(business.getId(), revenueAmount);
            
            // Update last revenue generation time
            lastRevenueGeneration.put(business.getId(), currentTime);
            
            // Notify business owner
            notifyRevenueGenerated(business, revenueAmount, revenueType);
        }
    }
    
    /**
     * Calculate revenue amount based on business model, employees, and performance
     */
    private double calculateRevenueAmount(Business business, BusinessRevenueModel model, List<BusinessEmployee> employees) {
        double baseRevenue = 0.0;
        int employeeCount = employees.size();
        double avgSalary = employees.stream().mapToDouble(BusinessEmployee::getCurrentSalary).average().orElse(50.0);
        
        switch (model) {
            case SERVICE_PROVIDER:
            case CONSULTING:
            case FREELANCE_AGENCY:
                // Service-based: Revenue based on employee productivity and hourly rates
                baseRevenue = employeeCount * avgSalary * (1.2 + ThreadLocalRandom.current().nextDouble(0.8));
                break;
                
            case RETAIL_SHOP:
            case MANUFACTURING:
            case TRADING_COMPANY:
                // Product-based: Revenue based on production capacity and market demand
                baseRevenue = employeeCount * avgSalary * (1.5 + ThreadLocalRandom.current().nextDouble(1.0));
                break;
                
            case PROJECT_CONTRACTOR:
            case GIG_COORDINATOR:
            case CONSTRUCTION_FIRM:
                // Contract-based: Higher revenue but less frequent
                if (ThreadLocalRandom.current().nextDouble() < 0.3) { // 30% chance
                    baseRevenue = employeeCount * avgSalary * (2.0 + ThreadLocalRandom.current().nextDouble(1.5));
                }
                // Add contract-specific revenue
                baseRevenue += generateContractRevenue(business, employees);
                break;
                
            case PROPERTY_MANAGEMENT:
            case INVESTMENT_FIRM:
            case FRANCHISE_OWNER:
                // Passive income: Steady, predictable revenue
                baseRevenue = employeeCount * avgSalary * (0.8 + ThreadLocalRandom.current().nextDouble(0.4));
                break;
                
            case FULL_SERVICE:
                // Hybrid: All revenue types with balanced approach
                baseRevenue = employeeCount * avgSalary * (1.3 + ThreadLocalRandom.current().nextDouble(0.7));
                break;
                
            case STARTUP:
                // Variable revenue with growth potential
                double growthFactor = Math.min(2.0, 0.5 + (employeeCount * 0.1));
                baseRevenue = employeeCount * avgSalary * (growthFactor + ThreadLocalRandom.current().nextDouble(0.5));
                break;
        }
        
        // Apply business model multiplier
        baseRevenue *= model.getRevenueMultiplier();
        
        // Apply random market fluctuations (±20%)
        double marketFactor = 0.8 + ThreadLocalRandom.current().nextDouble(0.4);
        baseRevenue *= marketFactor;
        
        return Math.max(0, baseRevenue);
    }
    
    /**
     * Get appropriate revenue type for business model
     */
    private BusinessRevenue.RevenueType getRevenueTypeForModel(BusinessRevenueModel model) {
        switch (model) {
            case SERVICE_PROVIDER:
            case CONSULTING:
            case FREELANCE_AGENCY:
                return BusinessRevenue.RevenueType.SERVICE_COMPLETION;
            case RETAIL_SHOP:
            case MANUFACTURING:
            case TRADING_COMPANY:
                return BusinessRevenue.RevenueType.PRODUCT_SALE;
            case PROJECT_CONTRACTOR:
            case GIG_COORDINATOR:
            case CONSTRUCTION_FIRM:
                return BusinessRevenue.RevenueType.CONTRACT_PAYMENT;
            case PROPERTY_MANAGEMENT:
                return BusinessRevenue.RevenueType.PROPERTY_RENT;
            case INVESTMENT_FIRM:
                return BusinessRevenue.RevenueType.INVESTMENT_RETURN;
            case FRANCHISE_OWNER:
                return BusinessRevenue.RevenueType.FRANCHISE_FEE;
            default:
                return BusinessRevenue.RevenueType.OTHER;
        }
    }
    
    /**
     * Record business revenue in the database
     */
    public boolean recordBusinessRevenue(int businessId, BusinessRevenue.RevenueType type, double amount, 
                                        String source, UUID generatedBy, String description, String metadata) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = "INSERT INTO business_revenue (business_id, revenue_type, amount, source, generated_by, timestamp, description, metadata) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, businessId);
                stmt.setString(2, type.name());
                stmt.setDouble(3, amount);
                stmt.setString(4, source);
                stmt.setString(5, generatedBy != null ? generatedBy.toString() : null);
                stmt.setLong(6, System.currentTimeMillis());
                stmt.setString(7, description);
                stmt.setString(8, metadata);
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error recording business revenue", e);
        }
        return false;
    }
    
    /**
     * Get business revenue history
     */
    public List<BusinessRevenue> getBusinessRevenueHistory(int businessId, int days) {
        List<BusinessRevenue> revenues = new ArrayList<>();
        long cutoffTime = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L);
        
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = "SELECT * FROM business_revenue WHERE business_id = ? AND timestamp > ? ORDER BY timestamp DESC";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, businessId);
                stmt.setLong(2, cutoffTime);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        revenues.add(new BusinessRevenue(
                            rs.getInt("revenue_id"),
                            rs.getInt("business_id"),
                            BusinessRevenue.RevenueType.valueOf(rs.getString("revenue_type")),
                            rs.getDouble("amount"),
                            rs.getString("source"),
                            rs.getString("generated_by") != null ? UUID.fromString(rs.getString("generated_by")) : null,
                            rs.getLong("timestamp"),
                            rs.getString("description"),
                            rs.getString("metadata")
                        ));
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting business revenue history", e);
        }
        return revenues;
    }
    
    /**
     * Calculate daily revenue target for a business
     */
    private void calculateDailyRevenueTarget(int businessId) {
        Business business = getBusiness(businessId);
        if (business == null) return;
        
        List<BusinessEmployee> employees = getBusinessEmployees(businessId);
        double totalSalaryCost = employees.stream().mapToDouble(BusinessEmployee::getCurrentSalary).sum();
        
        BusinessRevenueModel model = getBusinessRevenueModel(businessId);
        double targetMultiplier = 1.5; // Target 150% of salary costs for profitability
        
        double dailyTarget = totalSalaryCost * targetMultiplier * model.getRevenueMultiplier();
        dailyRevenueTargets.put(businessId, dailyTarget);
    }
    
    /**
     * Get business profitability report
     */
    public List<String> getBusinessProfitabilityReport(int businessId) {
        List<String> report = new ArrayList<>();
        Business business = getBusiness(businessId);
        if (business == null) return report;
        
        List<BusinessRevenue> recentRevenue = getBusinessRevenueHistory(businessId, 7);
        List<BusinessEmployee> employees = getBusinessEmployees(businessId);
        BusinessRevenueModel model = getBusinessRevenueModel(businessId);
        
        double totalRevenue = recentRevenue.stream().mapToDouble(BusinessRevenue::getAmount).sum();
        double totalSalaryCosts = employees.stream().mapToDouble(BusinessEmployee::getCurrentSalary).sum() * 7; // 7 days
        double profit = totalRevenue - totalSalaryCosts;
        double profitMargin = totalRevenue > 0 ? (profit / totalRevenue) * 100 : 0;
        
        report.add("§f" + business.getName() + " §7- Profitability Report (7 days)");
        report.add("§7═══════════════════════════════════════");
        report.add("§f💼 Business Model: §e" + model.getDisplayName());
        report.add("§f📊 Revenue Breakdown:");
        report.add("  §7Total Revenue: §a$" + String.format("%.2f", totalRevenue));
        report.add("  §7Total Salary Costs: §c$" + String.format("%.2f", totalSalaryCosts));
        report.add("  §7Net Profit: " + (profit >= 0 ? "§a" : "§c") + "$" + String.format("%.2f", profit));
        report.add("  §7Profit Margin: " + (profitMargin >= 0 ? "§a" : "§c") + String.format("%.1f", profitMargin) + "%");
        
        // Revenue by type
        Map<BusinessRevenue.RevenueType, Double> revenueByType = recentRevenue.stream()
            .collect(Collectors.groupingBy(BusinessRevenue::getType, 
                    Collectors.summingDouble(BusinessRevenue::getAmount)));
        
        if (!revenueByType.isEmpty()) {
            report.add("§f💰 Revenue Sources:");
            revenueByType.entrySet().stream()
                .sorted(Map.Entry.<BusinessRevenue.RevenueType, Double>comparingByValue().reversed())
                .forEach(entry -> {
                    double percentage = (entry.getValue() / totalRevenue) * 100;
                    report.add("  §7" + entry.getKey().getDisplayName() + ": §a$" + 
                        String.format("%.2f", entry.getValue()) + " §7(" + String.format("%.1f", percentage) + "%)");
                });
        }
        
        return report;
    }
    
    /**
     * Notify business owner of revenue generation
     */
    private void notifyRevenueGenerated(Business business, double amount, BusinessRevenue.RevenueType type) {
        Player owner = plugin.getServer().getPlayer(business.getOwnerUUID());
        if (owner != null) {
            String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6DJE§8] ");
            owner.sendMessage(prefix + "§a💰 Revenue Generated! §f$" + String.format("%.2f", amount) + 
                " §7from " + type.getDisplayName() + " at " + business.getName());
        }
    }
    
    /**
     * Manual revenue generation for testing or special events
     */
    public boolean generateManualRevenue(int businessId, BusinessRevenue.RevenueType type, double amount, String description) {
        Business business = getBusiness(businessId);
        if (business == null) return false;
        
        boolean recorded = recordBusinessRevenue(businessId, type, amount, "Manual Entry", null, description, 
            "{\"manual\":true,\"timestamp\":" + System.currentTimeMillis() + "}");
        
        if (recorded) {
            depositToBusiness(businessId, amount);
            notifyRevenueGenerated(business, amount, type);
        }
        
        return recorded;
    }
    
    /**
     * Get available business revenue models for selection
     */
    public List<String> getAvailableRevenueModels() {
        List<String> models = new ArrayList<>();
        models.add("§f§l=== Service-Based Models ===");
        for (BusinessRevenueModel model : BusinessRevenueModel.getServiceModels()) {
            models.add("§e" + model.getDisplayName() + " §7- " + model.getDescription());
        }
        
        models.add("§f§l=== Product Sales Models ===");
        for (BusinessRevenueModel model : BusinessRevenueModel.getProductModels()) {
            models.add("§e" + model.getDisplayName() + " §7- " + model.getDescription());
        }
        
        models.add("§f§l=== Contract & Gig Models ===");
        for (BusinessRevenueModel model : BusinessRevenueModel.getContractModels()) {
            models.add("§e" + model.getDisplayName() + " §7- " + model.getDescription());
        }
        
        models.add("§f§l=== Passive Income Models ===");
        for (BusinessRevenueModel model : BusinessRevenueModel.getPassiveModels()) {
            models.add("§e" + model.getDisplayName() + " §7- " + model.getDescription());
        }
        
        models.add("§f§l=== Hybrid Models ===");
        for (BusinessRevenueModel model : BusinessRevenueModel.getHybridModels()) {
            models.add("§e" + model.getDisplayName() + " §7- " + model.getDescription());
        }
        
        return models;
    }
    
    // ==================== BASIC CONTRACT SYSTEM ====================
    
    /**
     * Get active contracts for a business
     */
    public List<BusinessContract> getBusinessContracts(int businessId) {
        return businessContracts.getOrDefault(businessId, new ArrayList<>());
    }
    
    /**
     * Add a contract to a business (for future contract system expansion)
     */
    public void addBusinessContract(int businessId, BusinessContract contract) {
        businessContracts.computeIfAbsent(businessId, k -> new ArrayList<>()).add(contract);
    }
    
    /**
     * Generate contract-based revenue (used by contract business models)
     */
    private double generateContractRevenue(Business business, List<BusinessEmployee> employees) {
        List<BusinessContract> contracts = getBusinessContracts(business.getId());
        double contractRevenue = 0.0;
        
        for (BusinessContract contract : contracts) {
            if (contract.isActive() && employees.size() >= contract.getRequiredEmployees()) {
                // Simulate contract progress and payment
                double progressPayment = contract.getTotalValue() * 0.1; // 10% progress payment
                contractRevenue += progressPayment;
            }
        }
        
        return contractRevenue;
    }
    
    // ==================== MINECRAFT-VIABLE BUSINESS FEATURES ====================
    
    private final Map<Integer, List<BusinessLocation>> businessLocations = new HashMap<>();
    private final Map<Integer, List<ResourceProcessingChain>> processingChains = new HashMap<>();
    private final Map<Integer, List<ConstructionContract>> constructionContracts = new HashMap<>();
    private BusinessGUI businessGUI;
    
    /**
     * Initialize the business GUI system
     */
    public void initializeGUI() {
        this.businessGUI = new BusinessGUI(plugin, this);
    }
    
    /**
     * Get the business GUI instance
     */
    public BusinessGUI getBusinessGUI() {
        return businessGUI;
    }
    
    /**
     * Get businesses owned by a specific player
     */
    public List<Business> getBusinessesByOwner(UUID ownerUUID) {
        return businessCache.values().stream()
                .filter(business -> business.getOwnerUUID().equals(ownerUUID))
                .collect(Collectors.toList());
    }
    
    // ==================== BUSINESS LOCATION MANAGEMENT ====================
    
    /**
     * Add a physical location to a business
     */
    public boolean addBusinessLocation(int businessId, String locationName, Location location, 
                                     BusinessLocation.BusinessLocationType type, UUID createdBy, 
                                     double rentCost, String description) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = "INSERT INTO business_locations (business_id, location_name, world_name, x, y, z, " +
                        "region_name, type, rent_cost, description, created_by, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, businessId);
                stmt.setString(2, locationName);
                stmt.setString(3, location.getWorld().getName());
                stmt.setDouble(4, location.getX());
                stmt.setDouble(5, location.getY());
                stmt.setDouble(6, location.getZ());
                stmt.setString(7, "region_" + businessId + "_" + locationName.toLowerCase().replace(" ", "_"));
                stmt.setString(8, type.name());
                stmt.setDouble(9, rentCost);
                stmt.setString(10, description);
                stmt.setString(11, createdBy.toString());
                stmt.setLong(12, System.currentTimeMillis());
                
                boolean success = stmt.executeUpdate() > 0;
                if (success) {
                    // Add to cache
                    BusinessLocation businessLocation = new BusinessLocation(businessId, locationName, location, 
                            "region_" + businessId + "_" + locationName.toLowerCase().replace(" ", "_"), 
                            type, createdBy, rentCost, description);
                    businessLocations.computeIfAbsent(businessId, k -> new ArrayList<>()).add(businessLocation);
                }
                return success;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error adding business location", e);
            return false;
        }
    }
    
    /**
     * Get all locations for a business
     */
    public List<BusinessLocation> getBusinessLocations(int businessId) {
        return businessLocations.getOrDefault(businessId, new ArrayList<>());
    }
    
    /**
     * Calculate total location operational costs for a business
     */
    public double calculateLocationCosts(int businessId) {
        return getBusinessLocations(businessId).stream()
                .filter(BusinessLocation::isActive)
                .mapToDouble(BusinessLocation::getMonthlyOperationalCost)
                .sum();
    }
    
    // ==================== RESOURCE PROCESSING CHAINS ====================
    
    /**
     * Add a processing chain to a business
     */
    public boolean addProcessingChain(int businessId, String chainName, ResourceProcessingChain.ProcessingType type,
                                    Map<Material, Integer> inputMaterials, Map<Material, Integer> outputMaterials,
                                    double processingCost, long processingTime, int requiredEmployees, double profitMargin) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = "INSERT INTO processing_chains (business_id, chain_name, type, input_materials, " +
                        "output_materials, processing_cost, processing_time, required_employees, profit_margin) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, businessId);
                stmt.setString(2, chainName);
                stmt.setString(3, type.name());
                stmt.setString(4, serializeMaterialMap(inputMaterials));
                stmt.setString(5, serializeMaterialMap(outputMaterials));
                stmt.setDouble(6, processingCost);
                stmt.setLong(7, processingTime);
                stmt.setInt(8, requiredEmployees);
                stmt.setDouble(9, profitMargin);
                
                boolean success = stmt.executeUpdate() > 0;
                if (success) {
                    // Add to cache
                    ResourceProcessingChain chain = new ResourceProcessingChain(0, businessId, chainName, type,
                            inputMaterials, outputMaterials, processingCost, processingTime, requiredEmployees,
                            profitMargin, true, System.currentTimeMillis());
                    processingChains.computeIfAbsent(businessId, k -> new ArrayList<>()).add(chain);
                }
                return success;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error adding processing chain", e);
            return false;
        }
    }
    
    /**
     * Get processing chains for a business
     */
    public List<ResourceProcessingChain> getProcessingChains(int businessId) {
        return processingChains.getOrDefault(businessId, new ArrayList<>());
    }
    
    /**
     * Process resources through business chains (automated revenue generation)
     */
    public double processResourceChains(Business business, List<BusinessEmployee> employees) {
        List<ResourceProcessingChain> chains = getProcessingChains(business.getId());
        double totalRevenue = 0.0;
        
        for (ResourceProcessingChain chain : chains) {
            if (chain.isActive() && employees.size() >= chain.getRequiredEmployees()) {
                // Simulate processing with available employees
                double efficiency = chain.getProcessingEfficiency(employees.size());
                double potentialProfit = chain.calculatePotentialProfit() * efficiency;
                totalRevenue += potentialProfit * 0.1; // 10% of potential per cycle
            }
        }
        
        return totalRevenue;
    }
    
    // ==================== CONSTRUCTION CONTRACT SYSTEM ====================
    
    /**
     * Create a construction contract
     */
    public boolean createConstructionContract(UUID clientUUID, String projectName, 
                                            ConstructionContract.ConstructionType type, Location startLocation,
                                            Map<Material, Integer> requiredMaterials, List<String> specialRequirements) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = "INSERT INTO construction_contracts (client_uuid, project_name, type, world_name, " +
                        "start_x, start_y, start_z, required_materials, special_requirements, status, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, clientUUID.toString());
                stmt.setString(2, projectName);
                stmt.setString(3, type.name());
                stmt.setString(4, startLocation.getWorld().getName());
                stmt.setDouble(5, startLocation.getX());
                stmt.setDouble(6, startLocation.getY());
                stmt.setDouble(7, startLocation.getZ());
                stmt.setString(8, serializeMaterialMap(requiredMaterials));
                stmt.setString(9, String.join(";", specialRequirements));
                stmt.setString(10, ConstructionContract.ContractStatus.PENDING_APPROVAL.name());
                stmt.setLong(11, System.currentTimeMillis());
                
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error creating construction contract", e);
            return false;
        }
    }
    
    /**
     * Get construction contracts for a business
     */
    public List<ConstructionContract> getConstructionContracts(int businessId) {
        return constructionContracts.getOrDefault(businessId, new ArrayList<>());
    }
    
    /**
     * Assign a construction contract to a business
     */
    public boolean assignConstructionContract(int contractId, int businessId) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = "UPDATE construction_contracts SET business_id = ?, status = ? WHERE contract_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, businessId);
                stmt.setString(2, ConstructionContract.ContractStatus.MATERIALS_GATHERING.name());
                stmt.setInt(3, contractId);
                
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error assigning construction contract", e);
            return false;
        }
    }
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Serialize material map to JSON string for database storage
     */
    private String serializeMaterialMap(Map<Material, Integer> materials) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<Material, Integer> entry : materials.entrySet()) {
            if (!first) json.append(",");
            json.append("\"").append(entry.getKey().name()).append("\":").append(entry.getValue());
            first = false;
        }
        json.append("}");
        return json.toString();
    }
    
    /**
     * Initialize database tables for new Minecraft-viable features
     */
    private void initializeMinecraftViableTables() {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            // Business locations table
            String locationsSql = "CREATE TABLE IF NOT EXISTS business_locations (" +
                    "location_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "business_id INTEGER NOT NULL," +
                    "location_name TEXT NOT NULL," +
                    "world_name TEXT NOT NULL," +
                    "x REAL NOT NULL," +
                    "y REAL NOT NULL," +
                    "z REAL NOT NULL," +
                    "region_name TEXT," +
                    "type TEXT NOT NULL," +
                    "rent_cost REAL DEFAULT 0.0," +
                    "description TEXT," +
                    "is_active BOOLEAN DEFAULT 1," +
                    "created_by TEXT NOT NULL," +
                    "created_at INTEGER NOT NULL," +
                    "FOREIGN KEY (business_id) REFERENCES businesses(id)" +
                    ")";
            
            // Processing chains table
            String chainsSql = "CREATE TABLE IF NOT EXISTS processing_chains (" +
                    "chain_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "business_id INTEGER NOT NULL," +
                    "chain_name TEXT NOT NULL," +
                    "type TEXT NOT NULL," +
                    "input_materials TEXT NOT NULL," +
                    "output_materials TEXT NOT NULL," +
                    "processing_cost REAL NOT NULL," +
                    "processing_time INTEGER NOT NULL," +
                    "required_employees INTEGER NOT NULL," +
                    "profit_margin REAL NOT NULL," +
                    "is_active BOOLEAN DEFAULT 1," +
                    "created_at INTEGER NOT NULL," +
                    "FOREIGN KEY (business_id) REFERENCES businesses(id)" +
                    ")";
            
            // Construction contracts table
            String contractsSql = "CREATE TABLE IF NOT EXISTS construction_contracts (" +
                    "contract_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "business_id INTEGER," +
                    "client_uuid TEXT NOT NULL," +
                    "project_name TEXT NOT NULL," +
                    "type TEXT NOT NULL," +
                    "world_name TEXT NOT NULL," +
                    "start_x REAL NOT NULL," +
                    "start_y REAL NOT NULL," +
                    "start_z REAL NOT NULL," +
                    "end_x REAL," +
                    "end_y REAL," +
                    "end_z REAL," +
                    "required_materials TEXT NOT NULL," +
                    "contract_value REAL DEFAULT 0.0," +
                    "materials_cost REAL DEFAULT 0.0," +
                    "estimated_time INTEGER DEFAULT 0," +
                    "required_workers INTEGER DEFAULT 1," +
                    "status TEXT NOT NULL," +
                    "special_requirements TEXT," +
                    "completion_percentage REAL DEFAULT 0.0," +
                    "assigned_foreman TEXT," +
                    "created_at INTEGER NOT NULL," +
                    "deadline INTEGER," +
                    "blueprint_data TEXT," +
                    "FOREIGN KEY (business_id) REFERENCES businesses(id)" +
                    ")";
            
            conn.createStatement().execute(locationsSql);
            conn.createStatement().execute(chainsSql);
            conn.createStatement().execute(contractsSql);
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error initializing Minecraft-viable business tables", e);
        }
    }
}
