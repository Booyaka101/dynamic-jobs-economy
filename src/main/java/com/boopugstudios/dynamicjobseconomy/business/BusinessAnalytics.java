package com.boopugstudios.dynamicjobseconomy.business;

import com.boopugstudios.dynamicjobseconomy.DynamicJobsEconomy;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class BusinessAnalytics {
    
    private final DynamicJobsEconomy plugin;
    
    public BusinessAnalytics(DynamicJobsEconomy plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Gets comprehensive analytics for a business
     */
    public BusinessStats getBusinessStats(int businessId) {
        BusinessStats stats = new BusinessStats();
        
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            // Get basic business info
            String businessSql = "SELECT * FROM businesses WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(businessSql)) {
                stmt.setInt(1, businessId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        stats.businessName = rs.getString("name");
                        stats.businessType = rs.getString("type");
                        stats.currentBalance = rs.getDouble("balance");
                        stats.ownerUUID = UUID.fromString(rs.getString("owner_uuid"));
                    }
                }
            }
            
            // Get employee count and salary expenses
            String employeeSql = "SELECT COUNT(*) as employee_count FROM business_employees WHERE business_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(employeeSql)) {
                stmt.setInt(1, businessId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        stats.employeeCount = rs.getInt("employee_count");
                    }
                }
            }
            
            // Calculate monthly salary expenses
            double defaultSalary = plugin.getConfig().getDouble("business.default_salary", 100.0);
            int payrollsPerMonth = 24 * 30; // Hourly payroll * 30 days
            stats.monthlySalaryExpense = stats.employeeCount * defaultSalary * payrollsPerMonth;
            
            // Get transaction history (last 30 days)
            boolean isSQLite = "sqlite".equalsIgnoreCase(plugin.getDatabaseManager().getDatabaseType());
            String transactionSql = isSQLite
                ? "SELECT SUM(CASE WHEN transaction_type = 'DEPOSIT' THEN amount ELSE 0 END) as total_deposits, " +
                  "SUM(CASE WHEN transaction_type = 'WITHDRAW' THEN amount ELSE 0 END) as total_withdrawals, " +
                  "COUNT(*) as transaction_count FROM business_transactions WHERE business_id = ? " +
                  "AND created_at >= datetime('now','-30 days')"
                : "SELECT SUM(CASE WHEN transaction_type = 'DEPOSIT' THEN amount ELSE 0 END) as total_deposits, " +
                  "SUM(CASE WHEN transaction_type = 'WITHDRAW' THEN amount ELSE 0 END) as total_withdrawals, " +
                  "COUNT(*) as transaction_count FROM business_transactions WHERE business_id = ? " +
                  "AND created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)";
            
            try (PreparedStatement stmt = conn.prepareStatement(transactionSql)) {
                stmt.setInt(1, businessId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        stats.monthlyDeposits = rs.getDouble("total_deposits");
                        stats.monthlyWithdrawals = rs.getDouble("total_withdrawals");
                        stats.transactionCount = rs.getInt("transaction_count");
                    }
                }
            }
            
            // Calculate profit/loss
            stats.monthlyProfit = stats.monthlyDeposits - stats.monthlyWithdrawals - stats.monthlySalaryExpense;
            
            // Get employee productivity (if we track it)
            stats.employeeProductivity = getEmployeeProductivity(businessId);
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting business analytics for business " + businessId, e);
        }
        
        return stats;
    }
    
    /**
     * Gets employee productivity metrics
     */
    private Map<UUID, Double> getEmployeeProductivity(int businessId) {
        Map<UUID, Double> productivity = new HashMap<>();
        
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            // This would require tracking employee contributions
            // For now, we'll return empty map as it would need additional tracking
            boolean isSQLite = "sqlite".equalsIgnoreCase(plugin.getDatabaseManager().getDatabaseType());
            String sql = isSQLite
                ? "SELECT employee_uuid, COUNT(*) as tasks_completed, AVG(performance_rating) as avg_rating " +
                  "FROM employee_performance WHERE business_id = ? AND created_at >= datetime('now','-30 days') " +
                  "GROUP BY employee_uuid"
                : "SELECT employee_uuid, COUNT(*) as tasks_completed, AVG(performance_rating) as avg_rating " +
                  "FROM employee_performance WHERE business_id = ? AND created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY) " +
                  "GROUP BY employee_uuid";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, businessId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        UUID employeeUUID = UUID.fromString(rs.getString("employee_uuid"));
                        double rating = rs.getDouble("avg_rating");
                        productivity.put(employeeUUID, rating);
                    }
                }
            }
        } catch (SQLException e) {
            // Table might not exist yet, that's okay
            plugin.getLogger().log(Level.INFO, "Employee performance tracking not yet implemented");
        }
        
        return productivity;
    }
    
    /**
     * Displays business analytics to the owner
     */
    public void showBusinessAnalytics(Player owner, int businessId) {
        BusinessStats stats = getBusinessStats(businessId);
        
        if (stats.businessName == null) {
            owner.sendMessage("§cBusiness not found or you don't own it!");
            return;
        }
        
        owner.sendMessage("§8§m----------§r §6Business Analytics §8§m----------");
        owner.sendMessage("§fBusiness: §a" + stats.businessName + " §7(" + stats.businessType + ")");
        owner.sendMessage("§fCurrent Balance: §a$" + String.format("%.2f", stats.currentBalance));
        owner.sendMessage("§fEmployees: §a" + stats.employeeCount);
        owner.sendMessage("");
        
        owner.sendMessage("§e§lMonthly Overview:");
        owner.sendMessage("§fDeposits: §a+$" + String.format("%.2f", stats.monthlyDeposits));
        owner.sendMessage("§fWithdrawals: §c-$" + String.format("%.2f", stats.monthlyWithdrawals));
        owner.sendMessage("§fSalary Expenses: §c-$" + String.format("%.2f", stats.monthlySalaryExpense));
        owner.sendMessage("§fNet Profit/Loss: " + 
            (stats.monthlyProfit >= 0 ? "§a+$" : "§c-$") + 
            String.format("%.2f", Math.abs(stats.monthlyProfit)));
        owner.sendMessage("");
        
        owner.sendMessage("§e§lPerformance:");
        owner.sendMessage("§fTransactions: §a" + stats.transactionCount + " §7this month");
        
        if (stats.monthlyProfit > 0) {
            owner.sendMessage("§a✓ Your business is profitable!");
        } else if (stats.monthlyProfit < 0) {
            owner.sendMessage("§c⚠ Your business is operating at a loss");
            owner.sendMessage("§7Consider reducing expenses or increasing revenue");
        } else {
            owner.sendMessage("§7Your business is breaking even");
        }
        
        owner.sendMessage("§8§m----------------------------------------");
    }
    
    /**
     * Data class to hold business statistics
     */
    public static class BusinessStats {
        public String businessName;
        public String businessType;
        public double currentBalance;
        public UUID ownerUUID;
        public int employeeCount;
        public double monthlyDeposits;
        public double monthlyWithdrawals;
        public double monthlySalaryExpense;
        public double monthlyProfit;
        public int transactionCount;
        public Map<UUID, Double> employeeProductivity = new HashMap<>();
    }
}
