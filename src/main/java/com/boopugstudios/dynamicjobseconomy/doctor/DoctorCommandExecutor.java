package com.boopugstudios.dynamicjobseconomy.doctor;

import com.boopugstudios.dynamicjobseconomy.DynamicJobsEconomy;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.util.*;

/**
 * Executes the /djeconomy doctor diagnostics.
 *
 * This class extracts the doctor logic out of AdminCommand for clarity and future unit testing.
 */
public class DoctorCommandExecutor {
    private final DynamicJobsEconomy plugin;

    public DoctorCommandExecutor(DynamicJobsEconomy plugin) {
        this.plugin = plugin;
    }

    public void execute(CommandSender sender) {
        // Header
        sender.sendMessage(msg("admin.doctor.header", null, "§6Dynamic Jobs & Economy - System Doctor"));

        // Basic environment info
        String version = "unknown";
        try {
            if (plugin.getDescription() != null) {
                version = plugin.getDescription().getVersion();
            }
        } catch (Throwable ignored) {}
        Map<String, String> ph = new HashMap<>();
        ph.put("version", version);
        sender.sendMessage(msg("admin.doctor.env.version", ph, "§7Plugin version: §f%version%"));
        try {
            ph = new HashMap<>();
            ph.put("server", Bukkit.getVersion());
            sender.sendMessage(msg("admin.doctor.env.server", ph, "§7Server: §f%server%"));
        } catch (Throwable ignored) {}
        try {
            ph = new HashMap<>();
            ph.put("players", String.valueOf(Bukkit.getOnlinePlayers().size()));
            sender.sendMessage(msg("admin.doctor.env.players", ph, "§7Online players: §f%players%"));
        } catch (Throwable ignored) {}

        // Server performance (Paper only; via reflection when available)
        try {
            Object server = Bukkit.getServer();
            java.lang.reflect.Method getTPS = server.getClass().getMethod("getTPS");
            double[] tps = (double[]) getTPS.invoke(server);
            ph = new HashMap<>();
            ph.put("tps1", String.format("%.2f", tps.length > 0 ? tps[0] : 20.0));
            ph.put("tps5", String.format("%.2f", tps.length > 1 ? tps[1] : 20.0));
            ph.put("tps15", String.format("%.2f", tps.length > 2 ? tps[2] : 20.0));
            sender.sendMessage(msg("admin.doctor.perf.tps", ph, "§7TPS: §f%tps1%§7, §f%tps5%§7, §f%tps15%"));
        } catch (Throwable ignored) {}
        try {
            Object server = Bukkit.getServer();
            java.lang.reflect.Method getMspt = server.getClass().getMethod("getAverageTickTime");
            double mspt = (double) getMspt.invoke(server);
            ph = new HashMap<>();
            ph.put("mspt", String.format("%.2f", mspt));
            sender.sendMessage(msg("admin.doctor.perf.mspt", ph, "§7MSPT: §f%mspt%"));
        } catch (Throwable ignored) {}

        // Database health check (modular)
        DatabaseHealthCheck dbCheck = new DatabaseHealthCheck(plugin);
        DatabaseHealthCheck.Result dbResult = dbCheck.run();
        String dbType = dbResult.getDbType();
        boolean dbOk = dbResult.isOk();
        long dur = dbResult.getLatencyMs();
        ph = new HashMap<>();
        ph.put("type", dbType);
        ph.put("status", dbOk ? "§aOK" : "§cERROR");
        ph.put("ms", String.valueOf(dur));
        sender.sendMessage(msg("admin.doctor.db.summary", ph, "§7Database: §f%type% §7- %status% §8(%ms%ms)"));

        // Database pool stats (if available)
        if (plugin.getDatabaseManager() != null) {
            int active = 0, pooled = 0, max = 0, min = 0;
            try {
                active = plugin.getDatabaseManager().getActiveConnectionsCount();
                pooled = plugin.getDatabaseManager().getPoolSize();
                max = plugin.getDatabaseManager().getMaxPoolSize();
                min = plugin.getDatabaseManager().getMinPoolSize();
            } catch (Throwable ignored) {}
            ph = new HashMap<>();
            ph.put("active", String.valueOf(active));
            ph.put("pool", String.valueOf(pooled));
            ph.put("max", String.valueOf(max));
            ph.put("min", String.valueOf(min));
            sender.sendMessage(msg("admin.doctor.db.pool", ph, "§7DB Pool: §factive=%active%§7, pooled=%pool%§7, min=%min%§7, max=%max%"));
        }

        // SQLite file info (if applicable)
        if ("sqlite".equalsIgnoreCase(dbType)) {
            try {
                File dbFile = new File(plugin.getDataFolder(), "database.db");
                ph = new HashMap<>();
                ph.put("path", dbFile.getAbsolutePath());
                sender.sendMessage(msg("admin.doctor.sqlite.path", ph, "§7SQLite file: §f%path%"));
                if (dbFile.exists()) {
                    long bytes = dbFile.length();
                    double mb = bytes / 1024.0 / 1024.0;
                    ph = new HashMap<>();
                    ph.put("size", String.format("%.2fMB", mb));
                    sender.sendMessage(msg("admin.doctor.sqlite.size", ph, "§7SQLite size: §f%size%"));
                }
            } catch (Throwable ignored) {}
        }

        // Economy integration check (modular)
        VaultHealthCheck vaultCheck = new VaultHealthCheck(plugin);
        VaultHealthCheck.Result vaultResult = vaultCheck.run();
        boolean vaultPref = vaultResult.isPreferVault();
        boolean vaultEnabled = vaultResult.isEnabled();
        if (vaultEnabled) {
            String provider = vaultResult.getProviderName();
            ph = new HashMap<>();
            ph.put("provider", provider != null ? provider : "Unknown");
            sender.sendMessage(msg("admin.doctor.economy.vault", ph, "§7Economy: §fVault §7(Provider: §f%provider%§7) - §aENABLED"));
        } else {
            String def = vaultPref
                ? "§7Economy: §fInternal §7- §eUsing internal (Vault preferred but not available)"
                : "§7Economy: §fInternal §7- §aENABLED";
            sender.sendMessage(msg(vaultPref ? "admin.doctor.economy.internal_preferred" : "admin.doctor.economy.internal", null, def));
        }

        // Managers presence
        boolean ok = true;
        if (plugin.getEconomyManager() == null) { ph = new HashMap<>(); ph.put("name", "EconomyManager"); sender.sendMessage(msg("admin.doctor.manager_missing", ph, "§7%name%: §cMISSING")); ok = false; }
        if (plugin.getDatabaseManager() == null) { ph = new HashMap<>(); ph.put("name", "DatabaseManager"); sender.sendMessage(msg("admin.doctor.manager_missing", ph, "§7%name%: §cMISSING")); ok = false; }

        // Runtime info
        try {
            ph = new HashMap<>();
            ph.put("java", System.getProperty("java.version", "unknown"));
            sender.sendMessage(msg("admin.doctor.runtime.java", ph, "§7Java: §f%java%"));
        } catch (Throwable ignored) {}
        try {
            Runtime rt = Runtime.getRuntime();
            long total = rt.totalMemory();
            long free = rt.freeMemory();
            long used = total - free;
            long max = rt.maxMemory();
            ph = new HashMap<>();
            ph.put("used", String.valueOf(used / (1024 * 1024)));
            ph.put("total", String.valueOf(total / (1024 * 1024)));
            ph.put("max", String.valueOf(max / (1024 * 1024)));
            sender.sendMessage(msg("admin.doctor.runtime.memory", ph, "§7Memory: §f%used%MB§7/%total%MB (max %max%MB)"));
        } catch (Throwable ignored) {}

        // Config highlights
        try {
            ph = new HashMap<>();
            ph.put("vault_prefer", String.valueOf(vaultPref));
            sender.sendMessage(msg("admin.doctor.config.vault_prefer", ph, "§7Config: §fintegrations.vault.use_vault_economy=%vault_prefer%"));
        } catch (Throwable ignored) {}

        // --- Detailed validation checks (PASS/WARN/FAIL) ---
        try {
            // 0) Configuration Keys summary (missing/invalid)
            try {
                ConfigValidator.Result cfgRes = new ConfigValidator(plugin.getConfig()).validate();
                if (cfgRes.isOk()) {
                    sendDiagnosticCheck(sender,
                        msg("admin.doctor.checks.names.config_keys", null, "Configuration Keys"),
                        "pass",
                        null);
                } else {
                    Map<String, String> tph = new HashMap<>();
                    tph.put("missing", String.join(", ", cfgRes.getMissingKeys()));
                    tph.put("invalid", String.join(", ", cfgRes.getInvalidKeys()));
                    sendDiagnosticCheck(sender,
                        msg("admin.doctor.checks.names.config_keys", null, "Configuration Keys"),
                        "warn",
                        msg("admin.doctor.checks.tips.config_keys_review", tph, "Missing: %missing%. Invalid: %invalid%"));
                }
            } catch (Throwable ignored) {}

            // 1) Database Connectivity
            if (plugin.getDatabaseManager() == null) {
                sendDiagnosticCheck(sender,
                    msg("admin.doctor.checks.names.db_connectivity", null, "Database Connectivity"),
                    "fail",
                    msg("admin.doctor.checks.tips.db.manager_unavailable", null, "DatabaseManager unavailable. Check database.type in config.yml and restart."));
            } else if (dbOk) {
                sendDiagnosticCheck(sender,
                    msg("admin.doctor.checks.names.db_connectivity", null, "Database Connectivity"),
                    "pass",
                    null);
            } else {
                sendDiagnosticCheck(sender,
                    msg("admin.doctor.checks.names.db_connectivity", null, "Database Connectivity"),
                    "fail",
                    msg("admin.doctor.checks.tips.db.connect_fail", null, "Cannot query database. Verify credentials/URL and that the server is reachable."));
            }

            // 2) Database Latency (SELECT 1 duration)
            if (dbOk) {
                if (dur < 200) {
                    sendDiagnosticCheck(sender,
                        msg("admin.doctor.checks.names.db_latency", null, "Database Latency"),
                        "pass",
                        null);
                } else if (dur < 1000) {
                    Map<String, String> tph = new HashMap<>();
                    tph.put("ms", String.valueOf(dur));
                    sendDiagnosticCheck(sender,
                        msg("admin.doctor.checks.names.db_latency", null, "Database Latency"),
                        "warn",
                        msg("admin.doctor.checks.tips.db.latency_high", tph, "High latency (%ms% ms). Consider tuning DB or network."));
                } else {
                    Map<String, String> tph = new HashMap<>();
                    tph.put("ms", String.valueOf(dur));
                    sendDiagnosticCheck(sender,
                        msg("admin.doctor.checks.names.db_latency", null, "Database Latency"),
                        "warn",
                        msg("admin.doctor.checks.tips.db.latency_very_high", tph, "Very high latency (%ms% ms). Consider switching to MySQL or optimizing storage/network."));
                }
            }

            // 3) MySQL Config Keys (if mysql)
            if ("mysql".equalsIgnoreCase(dbType)) {
                String host = plugin.getConfig().getString("database.mysql.host", "");
                String dbName = plugin.getConfig().getString("database.mysql.database", "");
                String username = plugin.getConfig().getString("database.mysql.username", "");
                int port = 0;
                try { port = plugin.getConfig().getInt("database.mysql.port", 0); } catch (Throwable ignored2) {}
                List<String> missing = new ArrayList<>();
                if (host == null || host.trim().isEmpty()) missing.add("host");
                if (dbName == null || dbName.trim().isEmpty()) missing.add("database");
                if (username == null || username.trim().isEmpty()) missing.add("username");
                if (port <= 0) missing.add("port");
                if (missing.isEmpty()) {
                    sendDiagnosticCheck(sender,
                        msg("admin.doctor.checks.names.mysql_config", null, "MySQL Config"),
                        "pass",
                        null);
                } else {
                    Map<String, String> tph = new HashMap<>();
                    tph.put("keys", String.join(", ", missing));
                    sendDiagnosticCheck(sender,
                        msg("admin.doctor.checks.names.mysql_config", null, "MySQL Config"),
                        "fail",
                        msg("admin.doctor.checks.tips.mysql.missing_keys", tph, "Missing/invalid keys: %keys%. Set database.mysql.* in config.yml."));
                }
            }

            // 4) SQLite File Presence (if sqlite)
            if ("sqlite".equalsIgnoreCase(dbType)) {
                try {
                    File dbFile = new File(plugin.getDataFolder(), "database.db");
                    if (dbFile.exists() && dbFile.length() >= 0) {
                        sendDiagnosticCheck(sender,
                            msg("admin.doctor.checks.names.sqlite_file", null, "SQLite File"),
                            "pass",
                            null);
                    } else {
                        sendDiagnosticCheck(sender,
                            msg("admin.doctor.checks.names.sqlite_file", null, "SQLite File"),
                            "fail",
                            msg("admin.doctor.checks.tips.sqlite.file_missing", null, "database.db not found. Ensure plugin can write to its data folder."));
                    }
                } catch (Throwable t) {
                    Map<String, String> tph = new HashMap<>();
                    tph.put("error", String.valueOf(t.getMessage()));
                    sendDiagnosticCheck(sender,
                        msg("admin.doctor.checks.names.sqlite_file", null, "SQLite File"),
                        "fail",
                        msg("admin.doctor.checks.tips.sqlite.inspect_fail", tph, "Could not inspect SQLite file: %error%"));
                }
            }

            // 5) Economy Integration vs Preference
            if (plugin.getEconomyManager() == null) {
                sendDiagnosticCheck(sender,
                    msg("admin.doctor.checks.names.economy_manager", null, "Economy Manager"),
                    "fail",
                    msg("admin.doctor.checks.tips.economy.manager_missing", null, "Economy system unavailable. If using Vault, install Vault and an economy provider; otherwise use internal economy."));
            } else if (vaultPref) {
                if (vaultEnabled) {
                    sendDiagnosticCheck(sender,
                        msg("admin.doctor.checks.names.economy_integration_vault", null, "Economy Integration (Vault)"),
                        "pass",
                        null);
                } else {
                    sendDiagnosticCheck(sender,
                        msg("admin.doctor.checks.names.economy_integration_vault", null, "Economy Integration (Vault)"),
                        "warn",
                        msg("admin.doctor.checks.tips.vault.preferred_not_enabled", null, "Vault preferred but not enabled. Install/enable Vault and a provider."));
                }
            } else {
                sendDiagnosticCheck(sender,
                    msg("admin.doctor.checks.names.economy_integration_internal", null, "Economy Integration (Internal)"),
                    "pass",
                    null);
            }

            // 6) Admin Confirmation Settings
            double threshold = -1.0;
            int expiry = -1;
            try { threshold = plugin.getConfig().getDouble("economy.admin_confirmation.threshold", -1.0); } catch (Throwable ignored2) {}
            try { expiry = plugin.getConfig().getInt("economy.admin_confirmation.expiry_seconds", -1); } catch (Throwable ignored2) {}
            if (threshold > 0) {
                sendDiagnosticCheck(sender,
                    msg("admin.doctor.checks.names.admin_confirm_threshold", null, "Admin Confirmation Threshold"),
                    "pass",
                    null);
            } else {
                sendDiagnosticCheck(sender,
                    msg("admin.doctor.checks.names.admin_confirm_threshold", null, "Admin Confirmation Threshold"),
                    "warn",
                    msg("admin.doctor.checks.tips.admin.threshold_not_set", null, "Set economy.admin_confirmation.threshold to a positive number to avoid accidental large payouts."));
            }
            if (expiry > 0) {
                sendDiagnosticCheck(sender,
                    msg("admin.doctor.checks.names.admin_confirm_expiry", null, "Admin Confirmation Expiry"),
                    "pass",
                    null);
            } else {
                sendDiagnosticCheck(sender,
                    msg("admin.doctor.checks.names.admin_confirm_expiry", null, "Admin Confirmation Expiry"),
                    "warn",
                    msg("admin.doctor.checks.tips.admin.expiry_not_set", null, "Set economy.admin_confirmation.expiry_seconds to a positive number (e.g., 30)."));
            }
            
            // 7) Permissions Registration
            try {
                PermissionsHealthCheck permCheck = new PermissionsHealthCheck();
                PermissionsHealthCheck.Result permResult = permCheck.run();
                if (!permResult.getMissingCritical().isEmpty()) {
                    Map<String, String> tph = new HashMap<>();
                    tph.put("nodes", String.join(", ", permResult.getMissingCritical()));
                    sendDiagnosticCheck(sender,
                        msg("admin.doctor.checks.names.permissions", null, "Permissions Registration"),
                        "fail",
                        msg("admin.doctor.checks.tips.permissions.missing_critical", tph, "Missing critical permission nodes: %nodes%. Verify plugin.yml and load order."));
                } else if (!permResult.getMissingOptional().isEmpty()) {
                    Map<String, String> tph = new HashMap<>();
                    tph.put("nodes", String.join(", ", permResult.getMissingOptional()));
                    sendDiagnosticCheck(sender,
                        msg("admin.doctor.checks.names.permissions", null, "Permissions Registration"),
                        "warn",
                        msg("admin.doctor.checks.tips.permissions.missing_optional", tph, "Missing optional permission nodes: %nodes%."));
                } else {
                    sendDiagnosticCheck(sender,
                        msg("admin.doctor.checks.names.permissions", null, "Permissions Registration"),
                        "pass",
                        null);
                }
            } catch (Throwable ignored2) {}
        } catch (Throwable ignored) {}

        // Summary
        sender.sendMessage(msg(ok && dbOk ? "admin.doctor.summary.ok" : "admin.doctor.summary.issues", null,
            ok && dbOk ? "§aAll critical systems are operational." : "§eDiagnostics complete. See above for issues."));
    }

    private String msg(String path, Map<String, String> placeholders, String def) {
        try {
            if (plugin.getMessages() != null) {
                return plugin.getMessages().get(path, placeholders, def);
            }
        } catch (Throwable ignored) {}
        String out = def;
        if (placeholders != null && out != null) {
            for (Map.Entry<String, String> e : placeholders.entrySet()) {
                out = out.replace("%" + e.getKey() + "%", e.getValue());
            }
        }
        return out;
    }

    private void sendDiagnosticCheck(CommandSender sender, String name, String statusKey, String tipSuggestion) {
        String statusLabel;
        if ("pass".equalsIgnoreCase(statusKey)) {
            statusLabel = msg("admin.doctor.checks.status.pass", null, "§aPASS");
        } else if ("warn".equalsIgnoreCase(statusKey)) {
            statusLabel = msg("admin.doctor.checks.status.warn", null, "§eWARN");
        } else {
            statusLabel = msg("admin.doctor.checks.status.fail", null, "§cFAIL");
            statusKey = "fail";
        }
        Map<String, String> ph = new HashMap<>();
        ph.put("name", name);
        ph.put("status", statusLabel);
        sender.sendMessage(msg("admin.doctor.checks.item", ph, "§7%name%: %status%"));
        if (tipSuggestion != null && !tipSuggestion.isEmpty()) {
            Map<String, String> tph = new HashMap<>();
            tph.put("suggestion", tipSuggestion);
            sender.sendMessage(msg("admin.doctor.checks.tip", tph, "§8Tip: §7%suggestion%"));
        }
        try {
            String plain = name + ": " + statusLabel.replace('§', '&');
            if ("fail".equalsIgnoreCase(statusKey)) {
                plugin.getLogger().severe(plain + (tipSuggestion != null ? " | Tip: " + tipSuggestion : ""));
            } else if ("warn".equalsIgnoreCase(statusKey)) {
                plugin.getLogger().warning(plain + (tipSuggestion != null ? " | Tip: " + tipSuggestion : ""));
            } else {
                plugin.getLogger().info(plain + (tipSuggestion != null ? " | Tip: " + tipSuggestion : ""));
            }
        } catch (Throwable ignored) {}
    }
}
