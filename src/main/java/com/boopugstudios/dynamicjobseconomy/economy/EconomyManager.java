package com.boopugstudios.dynamicjobseconomy.economy;

import com.boopugstudios.dynamicjobseconomy.DynamicJobsEconomy;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

public class EconomyManager {
    
    private final DynamicJobsEconomy plugin;
    private Economy vaultEconomy;
    private boolean useVault;
    
    public EconomyManager(DynamicJobsEconomy plugin) {
        this.plugin = plugin;
        this.useVault = plugin.getConfig().getBoolean("integrations.vault.use_vault_economy", true);
        setupVaultEconomy();
    }
    
    private void setupVaultEconomy() {
        if (!useVault) return;
        
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().info("Vault not found, using internal economy system");
            useVault = false;
            return;
        }
        
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().info("No economy provider found, using internal economy system");
            useVault = false;
            return;
        }
        
        vaultEconomy = rsp.getProvider();
        plugin.getLogger().info("Vault economy integration enabled");
    }
    
    public double getBalance(Player player) {
        if (useVault && vaultEconomy != null) {
            return vaultEconomy.getBalance(player);
        }
        
        return getInternalBalance(player);
    }
    
    public boolean deposit(Player player, double amount) {
        if (useVault && vaultEconomy != null) {
            return vaultEconomy.depositPlayer(player, amount).transactionSuccess();
        }
        
        return setInternalBalance(player, getInternalBalance(player) + amount);
    }
    
    public boolean depositPlayer(OfflinePlayer player, double amount) {
        if (useVault && vaultEconomy != null) {
            return vaultEconomy.depositPlayer(player, amount).transactionSuccess();
        }
        
        // For offline players, we need to work directly with the database
        return setInternalBalanceOffline(player, getInternalBalanceOffline(player) + amount);
    }
    
    public double getBalance(OfflinePlayer player) {
        if (useVault && vaultEconomy != null) {
            return vaultEconomy.getBalance(player);
        }
        
        return getInternalBalanceOffline(player);
    }
    
    public boolean withdraw(OfflinePlayer player, double amount) {
        if (useVault && vaultEconomy != null) {
            return vaultEconomy.withdrawPlayer(player, amount).transactionSuccess();
        }
        
        double currentBalance = getInternalBalanceOffline(player);
        if (currentBalance >= amount) {
            return setInternalBalanceOffline(player, currentBalance - amount);
        }
        return false;
    }
    
    public boolean withdraw(Player player, double amount) {
        if (useVault && vaultEconomy != null) {
            return vaultEconomy.withdrawPlayer(player, amount).transactionSuccess();
        }
        
        double currentBalance = getInternalBalance(player);
        if (currentBalance >= amount) {
            return setInternalBalance(player, currentBalance - amount);
        }
        return false;
    }
    
    public boolean has(Player player, double amount) {
        return getBalance(player) >= amount;
    }
    
    private double getInternalBalance(Player player) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = "SELECT money FROM players WHERE uuid = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, player.getUniqueId().toString());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getDouble("money");
                    } else {
                        // Create new player record
                        createPlayerRecord(player);
                        return plugin.getConfig().getDouble("economy.starting_money", 1000.0);
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting balance for " + player.getName(), e);
            return 0.0;
        }
    }
    
    private boolean setInternalBalance(Player player, double amount) {
        double maxMoney = plugin.getConfig().getDouble("economy.max_money", 10000000.0);
        if (amount > maxMoney) {
            amount = maxMoney;
        }
        
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = """
                INSERT INTO players (uuid, username, money) VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE money = ?, last_seen = CURRENT_TIMESTAMP
            """;
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, player.getUniqueId().toString());
                stmt.setString(2, player.getName());
                stmt.setDouble(3, amount);
                stmt.setDouble(4, amount);
                
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error setting balance for " + player.getName(), e);
            return false;
        }
    }
    
    private void createPlayerRecord(Player player) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = "INSERT IGNORE INTO players (uuid, username, money) VALUES (?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, player.getUniqueId().toString());
                stmt.setString(2, player.getName());
                stmt.setDouble(3, plugin.getConfig().getDouble("economy.starting_money", 1000.0));
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error creating player record for " + player.getName(), e);
        }
    }
    
    public String formatMoney(double amount) {
        return String.format("$%.2f", amount);
    }
    
    public boolean isVaultEnabled() {
        return useVault && vaultEconomy != null;
    }
    
    private double getInternalBalanceOffline(OfflinePlayer player) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = "SELECT money FROM players WHERE uuid = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, player.getUniqueId().toString());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getDouble("money");
                    } else {
                        // Create new player record with default balance
                        double startingMoney = plugin.getConfig().getDouble("economy.starting_money", 1000.0);
                        createPlayerRecordOffline(player, startingMoney);
                        return startingMoney;
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting balance for offline player " + player.getName(), e);
            return 0.0;
        }
    }
    
    private boolean setInternalBalanceOffline(OfflinePlayer player, double amount) {
        double maxMoney = plugin.getConfig().getDouble("economy.max_money", 10000000.0);
        if (amount > maxMoney) {
            amount = maxMoney;
        }
        
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = """
                INSERT INTO players (uuid, username, money) VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE money = ?, last_seen = CURRENT_TIMESTAMP
            """;
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, player.getUniqueId().toString());
                stmt.setString(2, player.getName() != null ? player.getName() : "Unknown");
                stmt.setDouble(3, amount);
                stmt.setDouble(4, amount);
                
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error setting balance for offline player " + player.getName(), e);
            return false;
        }
    }
    
    private void createPlayerRecordOffline(OfflinePlayer player, double startingMoney) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = "INSERT IGNORE INTO players (uuid, username, money) VALUES (?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, player.getUniqueId().toString());
                stmt.setString(2, player.getName() != null ? player.getName() : "Unknown");
                stmt.setDouble(3, startingMoney);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error creating player record for offline player " + player.getName(), e);
        }
    }
}
