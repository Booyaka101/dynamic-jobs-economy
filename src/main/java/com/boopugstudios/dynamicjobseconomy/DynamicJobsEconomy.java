package com.boopugstudios.dynamicjobseconomy;

import com.boopugstudios.dynamicjobseconomy.commands.*;
import com.boopugstudios.dynamicjobseconomy.commands.ConsolidatedBusinessCommand;
import com.boopugstudios.dynamicjobseconomy.database.DatabaseManager;
import com.boopugstudios.dynamicjobseconomy.economy.EconomyManager;
import com.boopugstudios.dynamicjobseconomy.integrations.IntegrationManager;
import com.boopugstudios.dynamicjobseconomy.jobs.JobManager;
import com.boopugstudios.dynamicjobseconomy.admin.AdminAuditLogger;
import com.boopugstudios.dynamicjobseconomy.business.ConsolidatedBusinessManager;
import com.boopugstudios.dynamicjobseconomy.gigs.GigManager;
import com.boopugstudios.dynamicjobseconomy.notifications.NotificationManager;
import com.boopugstudios.dynamicjobseconomy.listeners.PlayerListener;
import com.boopugstudios.dynamicjobseconomy.listeners.JobListener;
import com.boopugstudios.dynamicjobseconomy.listeners.BusinessListener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Level;

public final class DynamicJobsEconomy extends JavaPlugin {

    private static DynamicJobsEconomy instance;
    
    // Core managers
    private DatabaseManager databaseManager;
    private EconomyManager economyManager;
    private JobManager jobManager;
    private ConsolidatedBusinessManager consolidatedBusinessManager;
    private GigManager gigManager;
    private IntegrationManager integrationManager;
    
    // New v1.0.2 managers
    private NotificationManager notificationManager;
    private AdminAuditLogger adminAuditLogger;

    @Override
    public void onEnable() {
        instance = this;
        
        // Detect first run before writing files
        boolean isFirstRun = !new File(getDataFolder(), "config.yml").exists();

        // Save default config
        saveDefaultConfig();
        
        getLogger().info("\n" +
            "  ____              ____             ____  _             _ _           \n" +
            " |  _ \\            |  _ \\           / ___|| |_ _   _  __| (_) ___  ___ \n" +
            " | |_) | ___   ___ | |_) |_   _  __ \\___ \\| __| | | |/ _` | |/ _ \\ / __|\n" +
            " |  _ < / _ \\ / _ \\|  __/| | | |/ _` |___) | |_| |_| | (_| | | (_) \\__ \\\n" +
            " |_| \\_\\___/ \\___/|_|   |_| |_|\\__, |____/ \\__|\\__,_|\\__,_|_|\\___/|___/\n" +
            "                                |___/                                   \n");
        getLogger().info("Starting Dynamic Jobs & Economy Pro v" + getDescription().getVersion());
        getLogger().info("Made with ❤\uFE0F by BooPug Studios");
        
        
        // Validate configuration before initializing
        if (!validateConfiguration()) {
            getLogger().severe("Configuration validation failed! Please check your config.yml. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Initialize managers
        if (!initializeManagers()) {
            getLogger().severe("Failed to initialize managers! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // First-time setup messages
        if (isFirstRun) {
            displayFirstTimeSetup();
        }
        
        // Register commands
        registerCommands();
        
        // Register listeners
        registerListeners();
        
        // Setup integrations
        integrationManager.setupIntegrations();
        
        getLogger().info("\n" +
            "🎉 SUCCESS! Dynamic Jobs & Economy Pro is now LIVE!\n" +
            "📊 Economy System: " + (economyManager.isVaultEnabled() ? "Vault Integration" : "Internal Economy") + "\n" +
            "💾 Database: " + getDatabaseManager().getDatabaseType() + "\n" +
            "🏆 Jobs Available: 5 (Miner, Chef, Farmer, Builder, Merchant)\n" +
            "💼 Features: Jobs, Gigs, Businesses, Admin Tools\n" +
            "📚 Help: Use /jobs help, /gigs help, /business help\n" +
            "🐶 BooPug Studios - Thank you for choosing our plugin!\n");
        
        // Schedule a delayed message for admins
        getServer().getScheduler().runTaskLater(this, () -> sendAdminWelcomeMessage(), 60L);
        
        // Schedule periodic cleanup tasks
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            // Clean up old anti-exploit data every 5 minutes
            if (jobManager != null) {
                jobManager.cleanupAntiExploitData();
            }
            // Save all player data every 5 minutes
            if (jobManager != null) {
                jobManager.saveAllPlayerData();
            }
        }, 6000L, 6000L); // 5 minutes initial delay, then every 5 minutes
        
        // Schedule business payroll system - runs every hour
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            if (consolidatedBusinessManager != null) {
                getLogger().info("Running scheduled business payroll...");
                consolidatedBusinessManager.processPayroll();
            }
        }, 72000L, 72000L); // 1 hour initial delay, then every hour
        
        getLogger().info("Dynamic Jobs & Economy Pro has been enabled successfully!");
        getLogger().info("Version: " + getDescription().getVersion());
        getLogger().info("Developed by BooPug Studios");
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabling Dynamic Jobs & Economy Pro...");
        
        // Save all data
        if (jobManager != null) {
            jobManager.saveAllPlayerData();
        }
        
        if (consolidatedBusinessManager != null) {
            // Business data is automatically saved when modified
        }
        
        // Close database connections
        if (databaseManager != null) {
            databaseManager.closeConnections();
        }
        
        getLogger().info("Dynamic Jobs & Economy Pro has been disabled. Thanks for using BooPug Studios plugins!");
    }
    
    private boolean initializeManagers() {
        try {
            // Initialize database first
            databaseManager = new DatabaseManager(this);
            if (!databaseManager.initialize()) {
                getLogger().severe("Failed to initialize database! Plugin will be disabled.");
                getServer().getPluginManager().disablePlugin(this);
                return false;
            }
            
            // Initialize connection pool for better performance
            databaseManager.initializeConnectionPool();
            
            // Initialize v1.0.2 new managers
            notificationManager = new NotificationManager(this);
            adminAuditLogger = new AdminAuditLogger(this);
            consolidatedBusinessManager = new ConsolidatedBusinessManager(this);
            
            // Initialize Minecraft-viable business GUI
            consolidatedBusinessManager.initializeGUI();
            
            gigManager = new GigManager(this);
            integrationManager = new IntegrationManager(this);
            
            // Initialize other managers
            economyManager = new EconomyManager(this);
            jobManager = new JobManager(this);
            
            return true;
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error initializing managers", e);
            return false;
        }
    }
    
    private void registerCommands() {
        // Register main commands
        getCommand("jobs").setExecutor(new JobsCommand(this));
        getCommand("business").setExecutor(new ConsolidatedBusinessCommand(this));
        getCommand("gigs").setExecutor(new GigsCommand(this));
        getCommand("djeconomy").setExecutor(new AdminCommand(this));
        
        // Set tab completers
        getCommand("jobs").setTabCompleter(new JobsCommand(this));
        getCommand("business").setTabCompleter(new ConsolidatedBusinessCommand(this));
        getCommand("gigs").setTabCompleter(new GigsCommand(this));
        getCommand("djeconomy").setTabCompleter(new AdminCommand(this));
    }
    
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new JobListener(this), this);
        getServer().getPluginManager().registerEvents(new BusinessListener(this), this);
        getServer().getPluginManager().registerEvents(notificationManager, this);
    }
    
    // Getters for managers
    public static DynamicJobsEconomy getInstance() {
        return instance;
    }
    
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    public EconomyManager getEconomyManager() {
        return economyManager;
    }
    
    public JobManager getJobManager() {
        return jobManager;
    }
    
    public ConsolidatedBusinessManager getConsolidatedBusinessManager() {
        return consolidatedBusinessManager;
    }
    
    // Legacy compatibility methods - delegate to consolidated manager
    @Deprecated
    public ConsolidatedBusinessManager getBusinessManager() {
        return consolidatedBusinessManager;
    }
    
    @Deprecated
    public ConsolidatedBusinessManager getBusinessPositionManager() {
        return consolidatedBusinessManager;
    }
    
    @Deprecated
    public ConsolidatedBusinessManager getPositionExtensions() {
        return consolidatedBusinessManager;
    }
    
    @Deprecated
    public ConsolidatedBusinessManager getAdvancedAnalytics() {
        return consolidatedBusinessManager;
    }
    
    @Deprecated
    public ConsolidatedBusinessManager getHiringRequestManager() {
        return consolidatedBusinessManager;
    }
    
    public GigManager getGigManager() {
        return gigManager;
    }
    
    public IntegrationManager getIntegrationManager() {
        return integrationManager;
    }
    
    public NotificationManager getNotificationManager() {
        return notificationManager;
    }
    
    public AdminAuditLogger getAdminAuditLogger() {
        return adminAuditLogger;
    }
    
    // Utility methods
    public void reloadConfiguration() {
        reloadConfig();
        
        // Reload managers
        if (jobManager != null) {
            jobManager.reload();
        }
        
        if (consolidatedBusinessManager != null) {
            consolidatedBusinessManager.reload();
        }
        
        if (gigManager != null) {
            gigManager.reload();
        }
        
        getLogger().info("Configuration reloaded successfully!");
    }
    
    public void onReload() {
        reloadConfig();
        
        if (jobManager != null) {
            jobManager.reload();
        }
        
        if (consolidatedBusinessManager != null) {
            consolidatedBusinessManager.reload();
        }
        
        if (gigManager != null) {
            gigManager.reload();
        }
        
        getLogger().info("Dynamic Jobs & Economy Pro reloaded successfully!");
    }
    
    private boolean validateConfiguration() {
        try {
            getLogger().info("Validating configuration...");
            
            // Validate economy settings
            double startingMoney = getConfig().getDouble("economy.starting_money", 1000.0);
            double maxMoney = getConfig().getDouble("economy.max_money", 10000000.0);
            if (startingMoney < 0 || maxMoney <= startingMoney) {
                getLogger().severe("Invalid economy settings: starting_money must be >= 0 and max_money must be > starting_money");
                return false;
            }
            
            // Validate job settings
            double jobCooldown = getConfig().getDouble("jobs.cooldown_seconds", 3.0);
            if (jobCooldown < 0) {
                getLogger().severe("Invalid job cooldown: must be >= 0");
                return false;
            }
            
            // Validate business settings
            double businessCreationCost = getConfig().getDouble("business.creation_cost", 1000.0);
            double defaultSalary = getConfig().getDouble("business.default_salary", 100.0);
            if (businessCreationCost < 0 || defaultSalary < 0) {
                getLogger().severe("Invalid business settings: costs and salaries must be >= 0");
                return false;
            }
            
            // Validate gig settings
            double gigCreationCost = getConfig().getDouble("gigs.creation_cost", 50.0);
            double commissionRate = getConfig().getDouble("gigs.commission_rate", 0.05);
            double cancellationPenalty = getConfig().getDouble("gigs.cancellation_penalty", 0.1);
            if (gigCreationCost < 0 || commissionRate < 0 || commissionRate > 1 || cancellationPenalty < 0 || cancellationPenalty > 1) {
                getLogger().severe("Invalid gig settings: costs must be >= 0, rates must be between 0-1");
                return false;
            }
            
            // Validate admin settings
            double adminThreshold = getConfig().getDouble("admin.large_transaction_threshold", 100000.0);
            int confirmationTimeout = getConfig().getInt("admin.confirmation_timeout_seconds", 30);
            if (adminThreshold <= 0 || confirmationTimeout <= 0) {
                getLogger().severe("Invalid admin settings: threshold and timeout must be > 0");
                return false;
            }
            
            // Validate database settings
            String dbType = getConfig().getString("database.type", "sqlite").toLowerCase();
            if (!dbType.equals("sqlite") && !dbType.equals("mysql") && !dbType.equals("mongodb")) {
                getLogger().severe("Invalid database type: must be 'sqlite', 'mysql', or 'mongodb'");
                return false;
            }
            
            getLogger().info("✓ Configuration validation passed!");
            return true;
            
        } catch (Exception e) {
            getLogger().severe("Configuration validation failed with exception: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private void displayFirstTimeSetup() {
        getLogger().info("\n" +
            "🎉 WELCOME TO BOOPUG STUDIOS! 🐶\n" +
            "\n" +
            "This appears to be your first time running Dynamic Jobs & Economy Pro!\n" +
            "\n" +
            "✅ AUTOMATIC SETUP COMPLETE:\n" +
            "   • Database initialized (SQLite)\n" +
            "   • Default configuration created\n" +
            "   • All job types loaded\n" +
            "   • Economy system ready\n" +
            "\n" +
            "🚀 YOUR PLUGIN IS READY TO USE!\n" +
            "\n" +
            "📚 QUICK START:\n" +
            "   • Players can use /jobs to get started\n" +
            "   • Use /djeconomy status to check everything\n" +
            "   • Check INSTALLATION.md for detailed setup\n" +
            "\n" +
            "🔧 OPTIONAL: Edit config.yml to customize settings\n" +
            "\n" +
            "Thank you for choosing BooPug Studios! 🎉\n");
    }
    
    private void sendAdminWelcomeMessage() {
        // Send welcome message to online ops/admins
        getServer().getOnlinePlayers().stream()
            .filter(player -> player.isOp() || player.hasPermission("djeconomy.admin"))
            .forEach(admin -> {
                admin.sendMessage("\n§8§m----------§r §6BooPug Studios§8 §m----------");
                admin.sendMessage("§6Dynamic Jobs & Economy Pro §7is now §aactive§7!");
                admin.sendMessage("");
                admin.sendMessage("§7Quick Admin Commands:");
                admin.sendMessage("§f/djeconomy status §7- Check plugin status");
                admin.sendMessage("§f/djeconomy reload §7- Reload configuration");
                admin.sendMessage("");
                admin.sendMessage("§7Player Commands:");
                admin.sendMessage("§f/jobs §7- Job system");
                admin.sendMessage("§f/gigs §7- Gig marketplace");
                admin.sendMessage("§f/business §7- Business management");
                admin.sendMessage("");
                admin.sendMessage("§7📚 Check §fINSTALLATION.md §7for full setup guide");
                admin.sendMessage("§8§m----------§r §6BooPug Studios§8 §m----------\n");
            });
    }
}
