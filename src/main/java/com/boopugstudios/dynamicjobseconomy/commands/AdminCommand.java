package com.boopugstudios.dynamicjobseconomy.commands;

import com.boopugstudios.dynamicjobseconomy.DynamicJobsEconomy;
import com.boopugstudios.dynamicjobseconomy.util.JobNameUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AdminCommand implements CommandExecutor, TabCompleter {
    
    private final DynamicJobsEconomy plugin;
    private final Map<UUID, PendingAdminAction> pendingConfirmations = new HashMap<>();
    // Confirmation settings are configurable via config.yml
    
    public AdminCommand(DynamicJobsEconomy plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Modern player resolution utility that avoids deprecated methods
     * @param playerName The player name to resolve
     * @return PlayerResolution containing both online and offline player references
     */
    private PlayerResolution resolvePlayer(String playerName) {
        // Try online player first
        Player onlinePlayer = getPlayerByName(playerName);
        if (onlinePlayer != null) {
            return new PlayerResolution(onlinePlayer, onlinePlayer, true);
        }
        
        // Try to find in server's cached offline players
        for (OfflinePlayer offlinePlayer : getOfflinePlayersArray()) {
            if (offlinePlayer.getName() != null && offlinePlayer.getName().equalsIgnoreCase(playerName)) {
                if (offlinePlayer.hasPlayedBefore()) {
                    return new PlayerResolution(null, offlinePlayer, false);
                }
            }
        }
        
        return new PlayerResolution(null, null, false);
    }
    
    /**
     * Helper class to hold player resolution results
     */
    private static class PlayerResolution {
        final Player onlinePlayer;
        final OfflinePlayer offlinePlayer;
        final boolean isOnline;
        
        PlayerResolution(Player onlinePlayer, OfflinePlayer offlinePlayer, boolean isOnline) {
            this.onlinePlayer = onlinePlayer;
            this.offlinePlayer = offlinePlayer;
            this.isOnline = isOnline;
        }
        
        boolean isValid() {
            return offlinePlayer != null;
        }
        
        String getName() {
            return isOnline ? onlinePlayer.getName() : (offlinePlayer != null ? offlinePlayer.getName() : "Unknown");
        }
    }
    
    private static class PendingAdminAction {
        final String action;
        final String playerName;
        final double amount;
        final long timestamp;
        
        PendingAdminAction(String action, String playerName, double amount, long timestamp) {
            this.action = action;
            this.playerName = playerName;
            this.amount = amount;
            this.timestamp = timestamp;
        }
        
        boolean isExpired(long now, long expiryMillis) {
            return now - timestamp > expiryMillis; // configurable timeout
        }
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String prefix = getPrefix();
        
        // Backward compatibility: full admin or legacy wildcard grants everything.
        boolean hasAdmin = sender.hasPermission("djeconomy.admin") || sender.hasPermission("dynamicjobs.admin.*");
        if (!hasAdmin && args.length > 0) {
            String sub = args[0].toLowerCase();
            String requiredNode = getRequiredPermissionForSub(sub);
            if (requiredNode != null && !sender.hasPermission(requiredNode)) {
                sender.sendMessage(prefix + msg("admin.no_permission", null, "§cYou don't have permission to use this command!"));
                return true;
            }
            if (requiredNode == null && !hasAdmin) {
                // If subcommand is unknown and user isn't admin, deny by default
                sender.sendMessage(prefix + msg("admin.no_permission", null, "§cYou don't have permission to use this command!"));
                return true;
            }
        } else if (!hasAdmin) {
            // No args and not admin: still show help but it's harmless; alternatively could deny.
        }
        
        if (args.length == 0) {
            showAdminHelp(sender, prefix);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "reload":
                // Perform reload; use the existing prefix for this message.
                // Subsequent commands will observe the new prefix via getPrefix().
                plugin.reloadConfig();
                plugin.onReload();
                sender.sendMessage(prefix + msg("admin.reload_success", null, "§aConfiguration reloaded!"));
                break;
            case "confirm":
                handleConfirmation(sender, prefix);
                break;
            case "economy":
                if (args.length < 4) {
                    sender.sendMessage(prefix + msg("admin.usage.economy", null, "§cUsage: /djeconomy economy <give|take|set> <player> <amount>"));
                    return true;
                }
                handleEconomy(sender, args[1], args[2], args[3], prefix);
                break;
            case "setlevel":
                if (args.length < 4) {
                    sender.sendMessage(prefix + msg("admin.usage.setlevel", null, "§cUsage: /djeconomy setlevel <player> <job> <level>"));
                    return true;
                }
                handleSetLevel(sender, args[1], args[2], args[3], prefix);
                break;
            case "addxp":
                if (args.length < 4) {
                    sender.sendMessage(prefix + msg("admin.usage.addxp", null, "§cUsage: /djeconomy addxp <player> <job> <amount>"));
                    return true;
                }
                handleAddXP(sender, args[1], args[2], args[3], prefix);
                break;
            case "refreshjobs":
                if (args.length < 2) {
                    sender.sendMessage(prefix + msg("admin.usage.refreshjobs", null, "§cUsage: /djeconomy refreshjobs <player>"));
                    return true;
                }
                handleRefreshJobs(sender, args[1], prefix);
                break;
            case "invalidatejobs":
                if (args.length < 2) {
                    sender.sendMessage(prefix + msg("admin.usage.invalidatejobs", null, "§cUsage: /djeconomy invalidatejobs <player>"));
                    return true;
                }
                handleInvalidateJobs(sender, args[1], prefix);
                break;
            case "getlevel":
                if (args.length < 3) {
                    sender.sendMessage(prefix + msg("admin.usage.getlevel", null, "§cUsage: /djeconomy getlevel <player> <job>"));
                    return true;
                }
                handleGetLevel(sender, args[1], args[2], prefix);
                break;
            case "resetlevel":
                if (args.length < 3) {
                    sender.sendMessage(prefix + msg("admin.usage.resetlevel", null, "§cUsage: /djeconomy resetlevel <player> <job>"));
                    return true;
                }
                handleResetLevel(sender, args[1], args[2], prefix);
                break;
            case "history":
                if (args.length < 2) {
                    sender.sendMessage(prefix + msg("admin.usage.history", null, "§cUsage: /djeconomy history <player> [limit]"));
                    return true;
                }
                String limit = args.length >= 3 ? args[2] : null;
                handleHistory(sender, args[1], limit, prefix);
                break;
                
            default:
                showAdminHelp(sender, prefix);
                break;
        }
        
        return true;
    }
    
    private void handleSetLevel(CommandSender sender, String playerName, String jobName, String levelStr, String prefix) {
        PlayerResolution resolution = resolvePlayer(playerName);
        if (!resolution.isValid()) {
            Map<String, String> ph = new HashMap<>();
            ph.put("player", playerName);
            sender.sendMessage(prefix + msg("admin.player_not_found", ph, "§cPlayer '%player%' not found or has never joined the server!"));
            return;
        }
        int level;
        try {
            level = Integer.parseInt(levelStr);
        } catch (NumberFormatException e) {
            sender.sendMessage(prefix + msg("admin.invalid_level", null, "§cInvalid level number!"));
            return;
        }

        boolean ok;
        if (resolution.isOnline) {
            ok = plugin.getJobManager().setJobLevel(resolution.onlinePlayer, jobName, level);
        } else {
            ok = plugin.getJobManager().setOfflineJobLevel(resolution.offlinePlayer, jobName, level);
        }

        if (!ok) {
            Map<String, String> ph = new HashMap<>();
            ph.put("job", jobName);
            sender.sendMessage(prefix + msg("admin.unknown_job", ph, "§cUnknown job '%job%'."));
            return;
        }

        String suffix = (resolution.isOnline ? " (online)" : " (offline)");
        Map<String, String> ph = new HashMap<>();
        ph.put("player", resolution.getName());
        ph.put("job", jobName);
        ph.put("level", String.valueOf(level));
        ph.put("suffix", suffix);
        sender.sendMessage(prefix + msg("admin.setlevel_success", ph, "§aSet %player%'s %job% level to %level%%suffix%"));
    }
    
    private void handleAddXP(CommandSender sender, String playerName, String jobName, String xpStr, String prefix) {
        // Use modern player resolution
        PlayerResolution resolution = resolvePlayer(playerName);
        
        if (!resolution.isValid()) {
            Map<String, String> ph = new HashMap<>();
            ph.put("player", playerName);
            sender.sendMessage(prefix + msg("admin.player_not_found", ph, "§cPlayer '%player%' not found or has never joined the server!"));
            return;
        }
        
        if (!resolution.isOnline) {
            sender.sendMessage(prefix + msg("admin.cannot_addxp_offline", null, "§cCannot add XP to offline player!"));
            return;
        }
        
        try {
            int xp = Integer.parseInt(xpStr);
            // Resolve job (case-insensitive)
            com.boopugstudios.dynamicjobseconomy.jobs.Job job = plugin.getJobManager().getJob(jobName);
            if (job == null) {
                Map<String, String> ph = new HashMap<>();
                ph.put("job", jobName);
                sender.sendMessage(prefix + msg("admin.unknown_job", ph, "§cUnknown job '%job%'."));
                return;
            }
            String canonical = job.getName();
            // Validate player has joined the job
            com.boopugstudios.dynamicjobseconomy.jobs.PlayerJobData pdata = plugin.getJobManager().getPlayerData(resolution.onlinePlayer);
            if (!pdata.hasJob(canonical)) {
                Map<String, String> ph = new HashMap<>();
                ph.put("player", resolution.getName());
                ph.put("job", canonical);
                sender.sendMessage(prefix + msg("admin.not_joined_job", ph, "§c%player% has not joined the job '%job%'."));
                return;
            }
            plugin.getJobManager().addExperience(resolution.onlinePlayer, canonical, xp);
            Map<String, String> ph2 = new HashMap<>();
            ph2.put("amount", String.valueOf(xp));
            ph2.put("player", resolution.getName());
            ph2.put("job", canonical);
            sender.sendMessage(prefix + msg("admin.added_xp", ph2, "§aAdded %amount% XP to %player%'s '%job%' job"));
        } catch (NumberFormatException e) {
            sender.sendMessage(prefix + msg("admin.invalid_xp", null, "§cInvalid XP amount!"));
        }
    }
    
    private void handleEconomy(CommandSender sender, String action, String playerName, String amountStr, String prefix) {
        // Use modern player resolution
        PlayerResolution resolution = resolvePlayer(playerName);
        
        if (!resolution.isValid()) {
            Map<String, String> ph = new HashMap<>();
            ph.put("player", playerName);
            sender.sendMessage(prefix + msg("admin.player_not_found", ph, "§cPlayer '%player%' not found or has never joined the server!"));
            return;
        }
        
        if (!resolution.isOnline) {
            Map<String, String> ph = new HashMap<>();
            ph.put("player", playerName);
            sender.sendMessage(prefix + msg("admin.offline_note", ph, "§7Note: Player '%player%' is offline. Processing transaction..."));
        }
        
        try {
            double amount = Double.parseDouble(amountStr);
            
            // Validate amount
            if (amount < 0) {
                sender.sendMessage(prefix + msg("admin.negative_amount", null, "§cAmount cannot be negative!"));
                return;
            }
            
            if (amount > 1000000000) { // 1 billion limit
                sender.sendMessage(prefix + msg("admin.amount_too_large", null, "§cAmount too large! Maximum: $1,000,000,000"));
                return;
            }
            
            // Check for large amounts requiring confirmation
            if (amount >= getConfirmThreshold()) {
                UUID senderUUID = sender instanceof Player ? ((Player) sender).getUniqueId() : null;
                if (senderUUID != null) {
                    PendingAdminAction pending = pendingConfirmations.get(senderUUID);
                    if (pending == null || pending.isExpired(nowMillis(), getConfirmExpiryMillis()) || 
                        !pending.action.equals(action) || !pending.playerName.equals(playerName) || 
                        pending.amount != amount) {
                        
                        // Store pending action using the time seam for testability
                        pendingConfirmations.put(senderUUID, new PendingAdminAction(action, playerName, amount, nowMillis()));
                        Map<String, String> ph1 = new HashMap<>();
                        ph1.put("amount", String.format("%.2f", amount));
                        sender.sendMessage(prefix + msg("admin.large_detected", ph1, "§e⚠ Large amount detected: $%amount%"));
                        Map<String, String> ph2 = new HashMap<>();
                        ph2.put("seconds", String.valueOf(getConfirmExpirySeconds()));
                        sender.sendMessage(prefix + msg("admin.confirm_prompt", ph2, "§eUse §f/djeconomy confirm §eto proceed (expires in %seconds% seconds)"));
                        return;
                    }
                    // Clear confirmation after use
                    pendingConfirmations.remove(senderUUID);
                }
            }
            
            // Execute the action
            boolean success = false;
            switch (action.toLowerCase()) {
                case "give":
                    // Ensure the EconomyManager is available (tests may not mock it)
                    if (plugin.getEconomyManager() == null) {
                        sender.sendMessage(prefix + msg("admin.economy_unavailable", null, "§cEconomy system is not available!"));
                        return;
                    }
                    if (resolution.isOnline) {
                        success = plugin.getEconomyManager().deposit(resolution.onlinePlayer, amount);
                    } else {
                        success = plugin.getEconomyManager().depositPlayer(resolution.offlinePlayer, amount);
                    }
                    if (success) {
                        Map<String, String> ph = new HashMap<>();
                        ph.put("amount", String.format("%.2f", amount));
                        ph.put("player", resolution.getName());
                        sender.sendMessage(prefix + msg("admin.give_success", ph, "§aGave $%amount% to %player%"));
                        logAdminAction(sender, "GIVE", resolution.getName(), amount);
                    }
                    break;
                case "take":
                    // Ensure the EconomyManager is available
                    if (plugin.getEconomyManager() == null) {
                        sender.sendMessage(prefix + msg("admin.economy_unavailable", null, "§cEconomy system is not available!"));
                        return;
                    }
                    double currentBalance = resolution.isOnline ? 
                        plugin.getEconomyManager().getBalance(resolution.onlinePlayer) :
                        plugin.getEconomyManager().getBalance(resolution.offlinePlayer);
                    if (currentBalance < amount) {
                        Map<String, String> ph = new HashMap<>();
                        ph.put("balance", String.format("%.2f", currentBalance));
                        sender.sendMessage(prefix + msg("admin.take_insufficient", ph, "§cPlayer only has $%balance%!"));
                        return;
                    }
                    if (resolution.isOnline) {
                        success = plugin.getEconomyManager().withdraw(resolution.onlinePlayer, amount);
                    } else {
                        success = plugin.getEconomyManager().withdraw(resolution.offlinePlayer, amount);
                    }
                    if (success) {
                        Map<String, String> ph = new HashMap<>();
                        ph.put("amount", String.format("%.2f", amount));
                        ph.put("player", resolution.getName());
                        sender.sendMessage(prefix + msg("admin.take_success", ph, "§aTook $%amount% from %player%"));
                        logAdminAction(sender, "TAKE", resolution.getName(), amount);
                    }
                    break;
                case "set":
                    // Ensure the EconomyManager is available
                    if (plugin.getEconomyManager() == null) {
                        sender.sendMessage(prefix + msg("admin.economy_unavailable", null, "§cEconomy system is not available!"));
                        return;
                    }
                    double current = resolution.isOnline ? 
                        plugin.getEconomyManager().getBalance(resolution.onlinePlayer) :
                        plugin.getEconomyManager().getBalance(resolution.offlinePlayer);
                    boolean withdrawSuccess = resolution.isOnline ?
                        plugin.getEconomyManager().withdraw(resolution.onlinePlayer, current) :
                        plugin.getEconomyManager().withdraw(resolution.offlinePlayer, current);
                    boolean depositSuccess = resolution.isOnline ?
                        plugin.getEconomyManager().deposit(resolution.onlinePlayer, amount) :
                        plugin.getEconomyManager().depositPlayer(resolution.offlinePlayer, amount);
                    if (withdrawSuccess && depositSuccess) {
                        Map<String, String> ph = new HashMap<>();
                        ph.put("amount", String.format("%.2f", amount));
                        ph.put("player", resolution.getName());
                        sender.sendMessage(prefix + msg("admin.set_success", ph, "§aSet %player%'s balance to $%amount%"));
                        logAdminAction(sender, "SET", resolution.getName(), amount);
                        success = true;
                    }
                    break;
                default:
                    sender.sendMessage(prefix + msg("admin.invalid_action", null, "§cInvalid action! Use give, take, or set"));
                    return;
            }
            
            if (!success) {
                sender.sendMessage(prefix + msg("admin.failed_execute", null, "§cFailed to execute economy command!"));
            }
            
        } catch (NumberFormatException e) {
            sender.sendMessage(prefix + msg("admin.invalid_amount", null, "§cInvalid amount!"));
        }
    }
    
    private void handleConfirmation(CommandSender sender, String prefix) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(prefix + msg("admin.confirm_players_only", null, "§cOnly players can use confirmations!"));
            return;
        }
        
        Player player = (Player) sender;
        PendingAdminAction pending = pendingConfirmations.get(player.getUniqueId());
        
        if (pending == null) {
            sender.sendMessage(prefix + msg("admin.no_pending_confirm", null, "§cNo pending action to confirm!"));
            return;
        }
        
        if (pending.isExpired(nowMillis(), getConfirmExpiryMillis())) {
            pendingConfirmations.remove(player.getUniqueId());
            sender.sendMessage(prefix + msg("admin.confirm_expired", null, "§cConfirmation expired! Please retry the command."));
            return;
        }
        
        // Re-execute the original command with confirmation bypass
        handleEconomy(sender, pending.action, pending.playerName, String.valueOf(pending.amount), prefix);
    }
    
    private void logAdminAction(CommandSender sender, String action, String targetPlayer, double amount) {
        String adminName = sender instanceof Player ? ((Player) sender).getName() : "CONSOLE";
        plugin.getLogger().info(String.format("[ADMIN] %s used %s on %s for $%.2f", 
            adminName, action, targetPlayer, amount));
        appendHistory(adminName, action, targetPlayer, amount);
    }
    
    private void showAdminHelp(CommandSender sender, String prefix) {
        sender.sendMessage(msg("admin.help.header", null, "§8§m----------§r §6Admin Help §8§m----------"));
        sender.sendMessage(msg("admin.help.reload", null, "§f/djeconomy reload §7- Reload configuration"));
        sender.sendMessage(msg("admin.help.setlevel", null, "§f/djeconomy setlevel <player> <job> <level> §7- Set player's job level (supports offline)"));
        sender.sendMessage(msg("admin.help.getlevel", null, "§f/djeconomy getlevel <player> <job> §7- Show player's job level (supports offline)"));
        sender.sendMessage(msg("admin.help.resetlevel", null, "§f/djeconomy resetlevel <player> <job> §7- Reset player's job level to 1"));
        sender.sendMessage(msg("admin.help.addxp", null, "§f/djeconomy addxp <player> <job> <amount> §7- Add XP to player's job (online only)"));
        sender.sendMessage(msg("admin.help.economy", null, "§f/djeconomy economy <give|take|set> <player> <amount> §7- Manage player money"));
        sender.sendMessage(msg("admin.help.history", null, "§f/djeconomy history <player> [limit] §7- View recent admin economy actions"));
        sender.sendMessage(msg("admin.help.refreshjobs", null, "§f/djeconomy refreshjobs <player> §7- Reload a player's job data from DB (online only)"));
        sender.sendMessage(msg("admin.help.invalidatejobs", null, "§f/djeconomy invalidatejobs <player> §7- Invalidate cached job data (online only)"));
    }

    /**
     * Seam for retrieving online players to aid testing without static mocking.
     */
    protected Collection<? extends Player> getOnlinePlayers() {
        return Bukkit.getOnlinePlayers();
    }

    /**
     * Seam for resolving a player by name (online lookup) for testing without static mocking.
     */
    protected Player getPlayerByName(String name) {
        return Bukkit.getPlayer(name);
    }

    /**
     * Seam for retrieving offline players cache for testing without static mocking.
     */
    protected OfflinePlayer[] getOfflinePlayersArray() {
        return Bukkit.getOfflinePlayers();
    }

    /**
     * Time seam for tests to control confirmation expiry without sleeping.
     */
    protected long nowMillis() {
        return System.currentTimeMillis();
    }

    // --- Config accessors for confirmation settings ---
    private double getConfirmThreshold() {
        // Use the overload with default so tests can stub (path, default) and reload reflects new values.
        // When using a Mockito mock, unstubbed calls may return 0.0; guard against that by applying our default.
        double v = plugin.getConfig().getDouble("economy.admin_confirmation.threshold", 100000.0);
        return v <= 0 ? 100000.0 : v;
    }

    private int getConfirmExpirySeconds() {
        // Use the overload with default so tests can stub (path, default) and reload reflects new values.
        // Guard for mocks returning 0 by enforcing a sane default of 30 seconds.
        int v = plugin.getConfig().getInt("economy.admin_confirmation.expiry_seconds", 30);
        return v <= 0 ? 30 : v;
    }

    private long getConfirmExpiryMillis() {
        return getConfirmExpirySeconds() * 1000L;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> base = Arrays.asList("reload", "setlevel", "getlevel", "resetlevel", "addxp", "economy", "history", "refreshjobs", "invalidatejobs");
            String pref = args[0].toLowerCase();
            return base.stream()
                .filter(s -> s.toLowerCase().startsWith(pref))
                .filter(s -> isSubAllowed(sender, s))
                .collect(Collectors.toList());
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("economy")) {
                return Arrays.asList("give", "take", "set").stream()
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            }
            if (args[0].equalsIgnoreCase("refreshjobs") || args[0].equalsIgnoreCase("invalidatejobs")
                    || args[0].equalsIgnoreCase("setlevel") || args[0].equalsIgnoreCase("getlevel")
                    || args[0].equalsIgnoreCase("resetlevel") || args[0].equalsIgnoreCase("addxp")
                    || args[0].equalsIgnoreCase("history")) {
                return getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            }
        }

        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("setlevel") || args[0].equalsIgnoreCase("addxp")
                || args[0].equalsIgnoreCase("getlevel") || args[0].equalsIgnoreCase("resetlevel")) {
                return JobNameUtil.suggestJobs(plugin.getJobManager().getJobs().keySet(), args[2]);
            }
            if (args[0].equalsIgnoreCase("economy")) {
                return getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
            }
            if (args[0].equalsIgnoreCase("history")) {
                return Arrays.asList("5", "10", "20", "50").stream()
                    .filter(s -> s.startsWith(args[2]))
                    .collect(Collectors.toList());
            }
        }

        return new ArrayList<>();
    }

    private String getPrefix() {
        // Prefer config.yml for backward compatibility; fallback to messages.yml and default.
        // First try with null default (so tests stubbing isNull() match)
        String fromConfig = plugin.getConfig().getString("messages.prefix", null);
        if (fromConfig == null || fromConfig.isEmpty()) {
            // Then try with non-null default (so tests stubbing anyString() match)
            fromConfig = plugin.getConfig().getString("messages.prefix", "");
        }
        if (fromConfig != null && !fromConfig.isEmpty()) return fromConfig;
        try {
            if (plugin.getMessages() != null) {
                String fromMessages = plugin.getMessages().getPrefix();
                if (fromMessages != null && !fromMessages.isEmpty()) return fromMessages;
            }
        } catch (Throwable ignored) {}
        return "§8[§6DynamicJobs§8] ";
    }

    /**
     * Localized message helper using messages.yml with safe fallback to defaults.
     */
    private String msg(String path, Map<String, String> placeholders, String def) {
        try {
            if (plugin.getMessages() != null) {
                return plugin.getMessages().get(path, placeholders, def);
            }
        } catch (Throwable ignored) {
            // Fallback below
        }
        String out = def;
        if (placeholders != null && out != null) {
            for (Map.Entry<String, String> e : placeholders.entrySet()) {
                out = out.replace("%" + e.getKey() + "%", e.getValue());
            }
        }
        return out;
    }

    private boolean isSubAllowed(CommandSender sender, String sub) {
        // In tests, sender may be null; allow all suggestions in that case
        if (sender == null) return true;
        // Admin or legacy wildcard sees all
        if (sender.hasPermission("djeconomy.admin") || sender.hasPermission("dynamicjobs.admin.*")) return true;
        String node = getRequiredPermissionForSub(sub.toLowerCase());
        return node == null || sender.hasPermission(node);
    }

    private String getRequiredPermissionForSub(String sub) {
        switch (sub) {
            case "reload":
                return "djeconomy.system.reload";
            case "economy":
            case "confirm":
                return "djeconomy.admin.economy";
            case "setlevel":
                return "djeconomy.admin.level.set";
            case "getlevel":
                return "djeconomy.admin.level.get";
            case "resetlevel":
                return "djeconomy.admin.level.reset";
            case "addxp":
                return "djeconomy.admin.level.addxp";
            case "refreshjobs":
                return "djeconomy.admin.jobs.refresh";
            case "invalidatejobs":
                return "djeconomy.admin.jobs.invalidate";
            case "history":
                return "djeconomy.admin.history.view";
            default:
                return null;
        }
    }

    private void handleRefreshJobs(CommandSender sender, String playerName, String prefix) {
        PlayerResolution resolution = resolvePlayer(playerName);
        if (!resolution.isValid()) {
            Map<String, String> ph = new HashMap<>();
            ph.put("player", playerName);
            sender.sendMessage(prefix + msg("admin.player_not_found", ph, "§cPlayer '%player%' not found or has never joined the server!"));
            return;
        }
        if (!resolution.isOnline) {
            sender.sendMessage(prefix + msg("admin.refreshjobs_requires_online", null, "§cPlayer must be online to refresh job data!"));
            return;
        }
        plugin.getJobManager().refreshPlayerData(resolution.onlinePlayer);
        Map<String, String> ph = new HashMap<>();
        ph.put("player", resolution.getName());
        sender.sendMessage(prefix + msg("admin.refreshjobs_success", ph, "§aRefreshed job data for %player%"));
    }

    private void handleInvalidateJobs(CommandSender sender, String playerName, String prefix) {
        PlayerResolution resolution = resolvePlayer(playerName);
        if (!resolution.isValid()) {
            Map<String, String> ph = new HashMap<>();
            ph.put("player", playerName);
            sender.sendMessage(prefix + msg("admin.player_not_found", ph, "§cPlayer '%player%' not found or has never joined the server!"));
            return;
        }
        if (!resolution.isOnline) {
            sender.sendMessage(prefix + msg("admin.invalidate_requires_online", null, "§cPlayer must be online to invalidate cached job data!"));
            return;
        }
        plugin.getJobManager().invalidatePlayerData(resolution.onlinePlayer);
        Map<String, String> ph = new HashMap<>();
        ph.put("player", resolution.getName());
        sender.sendMessage(prefix + msg("admin.invalidate_success", ph, "§aInvalidated cached job data for %player%"));
    }

    private void handleGetLevel(CommandSender sender, String playerName, String jobName, String prefix) {
        PlayerResolution resolution = resolvePlayer(playerName);
        if (!resolution.isValid()) {
            Map<String, String> ph = new HashMap<>();
            ph.put("player", playerName);
            sender.sendMessage(prefix + msg("admin.player_not_found", ph, "§cPlayer '%player%' not found or has never joined the server!"));
            return;
        }
        com.boopugstudios.dynamicjobseconomy.jobs.Job job = plugin.getJobManager().getJob(jobName);
        if (job == null) {
            Map<String, String> ph = new HashMap<>();
            ph.put("job", jobName);
            sender.sendMessage(prefix + msg("admin.unknown_job", ph, "§cUnknown job '%job%'."));
            return;
        }
        String canonical = job.getName();
        Integer level = resolution.isOnline
            ? plugin.getJobManager().getJobLevel(resolution.onlinePlayer, canonical)
            : plugin.getJobManager().getOfflineJobLevel(resolution.offlinePlayer, canonical);
        if (level == null) {
            Map<String, String> ph = new HashMap<>();
            ph.put("player", resolution.getName());
            ph.put("job", canonical);
            sender.sendMessage(prefix + msg("admin.not_joined_job", ph, "§c%player% has not joined the job '%job%'."));
            return;
        }
        String suffix = (resolution.isOnline ? " (online)" : " (offline)");
        Map<String, String> ph2 = new HashMap<>();
        ph2.put("player", resolution.getName());
        ph2.put("job", canonical);
        ph2.put("level", String.valueOf(level));
        ph2.put("suffix", suffix);
        sender.sendMessage(prefix + msg("admin.getlevel_value", ph2, "§a%player%'s '%job%' level is %level%%suffix%"));
    }

    private void handleResetLevel(CommandSender sender, String playerName, String jobName, String prefix) {
        PlayerResolution resolution = resolvePlayer(playerName);
        if (!resolution.isValid()) {
            Map<String, String> ph = new HashMap<>();
            ph.put("player", playerName);
            sender.sendMessage(prefix + msg("admin.player_not_found", ph, "§cPlayer '%player%' not found or has never joined the server!"));
            return;
        }
        boolean ok = resolution.isOnline
            ? plugin.getJobManager().setJobLevel(resolution.onlinePlayer, jobName, 1)
            : plugin.getJobManager().setOfflineJobLevel(resolution.offlinePlayer, jobName, 1);
        if (!ok) {
            Map<String, String> ph2 = new HashMap<>();
            ph2.put("job", jobName);
            sender.sendMessage(prefix + msg("admin.unknown_job", ph2, "§cUnknown job '%job%'."));
            return;
        }
        String canonical = plugin.getJobManager().getJob(jobName) != null ? plugin.getJobManager().getJob(jobName).getName() : jobName;
        String suffix = (resolution.isOnline ? " (online)" : " (offline)");
        Map<String, String> ph3 = new HashMap<>();
        ph3.put("player", resolution.getName());
        ph3.put("job", canonical);
        ph3.put("suffix", suffix);
        sender.sendMessage(prefix + msg("admin.resetlevel_success", ph3, "§aReset %player%'s '%job%' level to 1%suffix%"));
    }

    private File getHistoryFile() {
        File dir = plugin.getDataFolder();
        if (dir == null) {
            dir = new File(System.getProperty("java.io.tmpdir"), "dje-data");
        }
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, "admin-economy-history.log");
    }

    private void appendHistory(String admin, String action, String target, double amount) {
        File file = getHistoryFile();
        try (PrintWriter out = new PrintWriter(new FileWriter(file, true))) {
            out.printf("%d|%s|%s|%s|%.2f%n", System.currentTimeMillis(), admin, action, target, amount);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to write history: " + e.getMessage());
        }
    }

    private void handleHistory(CommandSender sender, String playerName, String limitStr, String prefix) {
        int limit = 10;
        if (limitStr != null) {
            try { limit = Integer.parseInt(limitStr); } catch (NumberFormatException ignored) {}
        }
        if (limit < 1) limit = 1;
        if (limit > 100) limit = 100;

        Path path = getHistoryFile().toPath();
        if (!Files.exists(path)) {
            Map<String, String> ph = new HashMap<>();
            ph.put("player", playerName);
            sender.sendMessage(prefix + msg("admin.history_none", ph, "§7No history found for '%player%'."));
            return;
        }
        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            List<String> filtered = new ArrayList<>();
            for (int i = lines.size() - 1; i >= 0 && filtered.size() < limit; i--) {
                String line = lines.get(i);
                String[] parts = line.split("\\|", -1);
                if (parts.length != 5) continue;
                String target = parts[3];
                if (target.equalsIgnoreCase(playerName)) {
                    long ts;
                    try { ts = Long.parseLong(parts[0]); } catch (NumberFormatException ex) { ts = System.currentTimeMillis(); }
                    String admin = parts[1];
                    String action = parts[2];
                    String amount = parts[4];
                    String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(ts));
                    filtered.add(String.format("§7[%s] §f%s §7-> §6%s §7$%s", time, admin, action + " " + target, amount));
                }
            }
            if (filtered.isEmpty()) {
                Map<String, String> ph = new HashMap<>();
                ph.put("player", playerName);
                sender.sendMessage(prefix + msg("admin.history_none", ph, "§7No history found for '%player%'."));
                return;
            }
            Map<String, String> ph2 = new HashMap<>();
            ph2.put("count", String.valueOf(filtered.size()));
            ph2.put("player", playerName);
            sender.sendMessage(prefix + msg("admin.history_header", ph2, "§eShowing last %count% entries for '%player%':"));
            for (String msg : filtered) {
                sender.sendMessage(msg);
            }
        } catch (IOException e) {
            Map<String, String> ph = new HashMap<>();
            ph.put("error", e.getMessage());
            sender.sendMessage(prefix + msg("admin.history_read_failed", ph, "§cFailed to read history: %error%"));
        }
    }
}
