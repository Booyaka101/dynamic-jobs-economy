package com.boopugstudios.dynamicjobseconomy;

import com.boopugstudios.dynamicjobseconomy.commands.*;
import com.boopugstudios.dynamicjobseconomy.commands.ConsolidatedBusinessCommand;
import com.boopugstudios.dynamicjobseconomy.database.DatabaseManager;
import com.boopugstudios.dynamicjobseconomy.economy.EconomyManager;
import com.boopugstudios.dynamicjobseconomy.integrations.IntegrationManager;
import com.boopugstudios.dynamicjobseconomy.jobs.JobManager;
import com.boopugstudios.dynamicjobseconomy.admin.AdminAuditLogger;
import com.boopugstudios.dynamicjobseconomy.admin.AdminConfirmationManager;
import com.boopugstudios.dynamicjobseconomy.admin.AdminReasonChatListener;
import com.boopugstudios.dynamicjobseconomy.business.ConsolidatedBusinessManager;
import com.boopugstudios.dynamicjobseconomy.gigs.GigManager;
import com.boopugstudios.dynamicjobseconomy.notifications.NotificationManager;
import com.boopugstudios.dynamicjobseconomy.listeners.PlayerListener;
import com.boopugstudios.dynamicjobseconomy.listeners.JobListener;
import com.boopugstudios.dynamicjobseconomy.listeners.BusinessListener;
import com.boopugstudios.dynamicjobseconomy.i18n.Messages;
import org.bukkit.plugin.java.JavaPlugin;
import com.boopugstudios.dynamicjobseconomy.util.EconomyFormat;

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
    private Messages messages;
    private AdminConfirmationManager adminConfirmationManager;

    @Override
    public void onEnable() {
        instance = this;
        
        // Detect first run before writing files
        boolean isFirstRun = !new File(getDataFolder(), "config.yml").exists();

        // Save default config
        saveDefaultConfig();
        // Load currency formatting settings
        EconomyFormat.reloadFromConfig(getConfig());
        
        // Load messages.yml (i18n)
        messages = new Messages(this);
        messages.load();
        
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

        // Periodically purge expired admin confirmations (every 60 seconds)
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            if (adminConfirmationManager != null) {
                adminConfirmationManager.purgeExpired();
            }
        }, 1200L, 1200L);

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
            
            // Initialize v1.0.2 new managers
            notificationManager = new NotificationManager(this);
            adminAuditLogger = new AdminAuditLogger(this);
            adminConfirmationManager = new AdminConfirmationManager(this);
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
        // Create single instances so executor and tab completer share state
        JobsCommand jobsCmd = new JobsCommand(this);
        ConsolidatedBusinessCommand bizCmd = new ConsolidatedBusinessCommand(this);
        GigsCommand gigsCmd = new GigsCommand(this);
        AdminCommand adminCmd = new AdminCommand(this);

        // Register main commands
        getCommand("jobs").setExecutor(jobsCmd);
        getCommand("business").setExecutor(bizCmd);
        getCommand("gigs").setExecutor(gigsCmd);
        getCommand("djeconomy").setExecutor(adminCmd);

        // Set tab completers
        getCommand("jobs").setTabCompleter(jobsCmd);
        getCommand("business").setTabCompleter(bizCmd);
        getCommand("gigs").setTabCompleter(gigsCmd);
        getCommand("djeconomy").setTabCompleter(adminCmd);
    }
    
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new JobListener(this), this);
        getServer().getPluginManager().registerEvents(new BusinessListener(this), this);
        getServer().getPluginManager().registerEvents(notificationManager, this);
        getServer().getPluginManager().registerEvents(new AdminReasonChatListener(this), this);
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
    
    public AdminConfirmationManager getAdminConfirmationManager() {
        // Lazy init to support unit tests that do not run onEnable()
        if (adminConfirmationManager == null) {
            adminConfirmationManager = new AdminConfirmationManager(this);
        }
        return adminConfirmationManager;
    }
    
    public Messages getMessages() {
        return messages;
    }
    
    // Utility methods
    public void reloadConfiguration() {
        reloadConfig();
        EconomyFormat.reloadFromConfig(getConfig());
        if (messages != null) {
            messages.load();
        }
        
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
        // Reload configs
        reloadConfig();
        EconomyFormat.reloadFromConfig(getConfig());
        if (messages != null) {
            messages.load();
        }
        // Reload managers
        if (jobManager != null) jobManager.reload();
        if (consolidatedBusinessManager != null) consolidatedBusinessManager.reload();
        if (gigManager != null) gigManager.reload();
        getLogger().info("Reload complete.");
    }

    private boolean validateConfiguration() {
        try {
            getLogger().info("Validating configuration...");
            // Economy
            double startingMoney = getConfig().getDouble("economy.starting_money", 1000.0);
            double maxMoney = getConfig().getDouble("economy.max_money", 10000000.0);
            if (startingMoney < 0 || maxMoney <= startingMoney) {
                getLogger().severe("Invalid economy settings: starting_money must be >= 0 and max_money > starting_money");
                return false;
            }
            // Business
            double businessCreationCost = getConfig().getDouble("business.creation_cost", 1000.0);
            double defaultSalary = getConfig().getDouble("business.default_salary", 100.0);
            if (businessCreationCost < 0 || defaultSalary < 0) {
                getLogger().severe("Invalid business settings: costs and salaries must be >= 0");
                return false;
            }
            // Gigs
            double gigPostingCost = getConfig().getDouble("gigs.posting_cost", 50.0);
            double commissionRate = getConfig().getDouble("gigs.commission_rate", 0.05);
            double cancellationPenalty = getConfig().getDouble("gigs.cancellation_penalty", 0.1);
            if (gigPostingCost < 0 || commissionRate < 0 || commissionRate > 1 || cancellationPenalty < 0 || cancellationPenalty > 1) {
                getLogger().severe("Invalid gig settings: costs must be >= 0, rates 0-1");
                return false;
            }
            // Admin confirmation (economy)
            double adminThreshold = getConfig().getDouble("economy.admin_confirmation.threshold", 100000.0);
            int expirySeconds = getConfig().getInt("economy.admin_confirmation.expiry_seconds", 30);
            if (adminThreshold <= 0 || expirySeconds <= 0) {
                getLogger().severe("Invalid admin confirmation settings: threshold and expiry_seconds must be > 0");
                return false;
            }
            // Database
            String dbType = getConfig().getString("database.type", "sqlite").toLowerCase();
            if (!dbType.equals("sqlite") && !dbType.equals("mysql")) {
                getLogger().severe("Invalid database.type. Must be sqlite or mysql.");
                return false;
            }
            getLogger().info("\u2713 Configuration validation passed!");
            return true;
        } catch (Exception e) {
            getLogger().severe("Configuration validation failed: " + e.getMessage());
            return false;
        }
    }

    private void displayFirstTimeSetup() {
        getLogger().info("First-time setup detected. Default configuration created. See INSTALLATION.md for guidance.");
    }
    
    private void sendAdminWelcomeMessage() {
        // Send welcome message to online ops/admins
        getServer().getOnlinePlayers().stream()
            .filter(player -> player.isOp() || player.hasPermission("djeconomy.admin"))
            .forEach(admin -> {
                String prefix = messages.getPrefix();
                admin.sendMessage("\n" + prefix + messages.get("admin.welcome.header", null, "§8§m----------§r §6BooPug Studios§8 §m----------"));
                admin.sendMessage(prefix + messages.get("admin.welcome.active", null, "§6Dynamic Jobs & Economy Pro §7is now §aactive§7!"));
                admin.sendMessage("");
                admin.sendMessage(prefix + messages.get("admin.welcome.quick_header", null, "§7Quick Admin Commands:"));
                admin.sendMessage(prefix + messages.get("admin.welcome.status", null, "§f/djeconomy status §7- Check plugin status"));
                admin.sendMessage(prefix + messages.get("admin.welcome.reload", null, "§f/djeconomy reload §7- Reload configuration"));
                admin.sendMessage("");
                admin.sendMessage(prefix + messages.get("admin.welcome.player_commands", null, "§7Player Commands:"));
                admin.sendMessage(prefix + messages.get("admin.welcome.jobs", null, "§f/jobs §7- Job system"));
                admin.sendMessage(prefix + messages.get("admin.welcome.gigs", null, "§f/gigs §7- Gig marketplace"));
                admin.sendMessage(prefix + messages.get("admin.welcome.business", null, "§f/business §7- Business management"));
                admin.sendMessage("");
                admin.sendMessage(prefix + messages.get("admin.welcome.installation", null, "§7 Check §fINSTALLATION.md §7for full setup guide"));
                admin.sendMessage(prefix + messages.get("admin.welcome.header", null, "§8§m----------§r §6BooPug Studios§8 §m----------") + "\n");
            });
    }
}
