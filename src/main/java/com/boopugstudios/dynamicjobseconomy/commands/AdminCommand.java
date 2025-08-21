package com.boopugstudios.dynamicjobseconomy.commands;

import com.boopugstudios.dynamicjobseconomy.DynamicJobsEconomy;
import com.boopugstudios.dynamicjobseconomy.admin.AdminConfirmationManager;
import com.boopugstudios.dynamicjobseconomy.economy.EconomyManager;
import com.boopugstudios.dynamicjobseconomy.gui.AdminEconomyGui;
import com.boopugstudios.dynamicjobseconomy.util.JobNameUtil;
import com.boopugstudios.dynamicjobseconomy.util.EconomyFormat;
import com.boopugstudios.dynamicjobseconomy.business.Business;
import com.boopugstudios.dynamicjobseconomy.business.ConsolidatedBusinessManager;
import com.boopugstudios.dynamicjobseconomy.doctor.DoctorCommandExecutor;
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
    // Confirmation settings are configurable via config.yml
    private AdminConfirmationManager localAdminConfirm;
    
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
    
    // Pending confirmation state is managed centrally by AdminConfirmationManager
    
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
            case "gui":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(prefix + msg("admin.gui.players_only", null, "§cOnly players can open the GUI!"));
                    return true;
                }
                Player guiPlayer = (Player) sender;
                // Permission gating is already handled above via getRequiredPermissionForSub, but double-check here for clarity
                if (!guiPlayer.hasPermission("djeconomy.gui.admin.economy") &&
                        !guiPlayer.hasPermission("djeconomy.admin") &&
                        !guiPlayer.hasPermission("dynamicjobs.admin.*")) {
                    sender.sendMessage(prefix + msg("admin.no_permission", null, "§cYou don't have permission to use this command!"));
                    return true;
                }
                AdminEconomyGui.get(plugin).openHome(guiPlayer);
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
                    sender.sendMessage(prefix + msg("admin.usage.history", null, "§cUsage: /djeconomy history <player> [page] [size]"));
                    return true;
                }
                String page = args.length >= 3 ? args[2] : null;
                String size = args.length >= 4 ? args[3] : null;
                handleHistory(sender, args[1], page, size, prefix);
                break;
            case "businessinfo":
                handleBusinessInfo(sender, args, prefix);
                break;
            case "doctor":
                handleDoctor(sender, prefix);
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
            
            // 1 billion limit (configurable display only)
            double MAX = 1_000_000_000d;
            if (amount > MAX) {
                Map<String, String> ph = new HashMap<>();
                ph.put("max", com.boopugstudios.dynamicjobseconomy.util.EconomyFormat.money(MAX));
                sender.sendMessage(prefix + msg("admin.amount_too_large", ph, "§cAmount too large! Maximum: %max%"));
                return;
            }
            
            // Check for large amounts requiring confirmation
            if (amount >= getConfirmThreshold()) {
                UUID senderUUID = sender instanceof Player ? ((Player) sender).getUniqueId() : null;
                if (senderUUID != null) {
                    AdminConfirmationManager mgr = getOrCreateConfirmationManager();
                    AdminConfirmationManager.PendingAdminAction pending = mgr.getPending(senderUUID);
                    long expiryMs = mgr.getExpiryMillis();
                    if (pending == null || pending.isExpired(nowMillis(), expiryMs) ||
                        !pending.action.equals(action) || !pending.playerName.equals(playerName) ||
                        pending.amount != amount) {

                        // Store pending action using the time seam for testability
                        mgr.putPending(senderUUID, action, playerName, amount);
                        Map<String, String> ph1 = new HashMap<>();
                        ph1.put("money", com.boopugstudios.dynamicjobseconomy.util.EconomyFormat.money(amount));
                        sender.sendMessage(prefix + msg("admin.large_detected", ph1, "§e⚠ Large amount detected: %money%"));
                        Map<String, String> ph2 = new HashMap<>();
                        ph2.put("seconds", String.valueOf(getConfirmExpirySeconds()));
                        sender.sendMessage(prefix + msg("admin.confirm_prompt", ph2, "§eUse §f/djeconomy confirm §eto proceed (expires in %seconds% seconds)"));
                        return;
                    } else {
                        // Matching pending exists; require explicit /djeconomy confirm
                        Map<String, String> ph2 = new HashMap<>();
                        ph2.put("seconds", String.valueOf(getConfirmExpirySeconds()));
                        sender.sendMessage(prefix + msg("admin.confirm_prompt", ph2, "§eUse §f/djeconomy confirm §eto proceed (expires in %seconds% seconds)"));
                        return;
                    }
                }
            }
            
            // Execute the action (no explicit reason provided in direct execution)
            boolean success = performEconomy(sender, action, resolution, amount, prefix, null);
            if (!success) {
                sender.sendMessage(prefix + msg("admin.failed_execute", null, "§cFailed to execute economy command!"));
            }
            
        } catch (NumberFormatException e) {
            sender.sendMessage(prefix + msg("admin.invalid_amount", null, "§cInvalid amount!"));
        }
    }

    private boolean performEconomy(CommandSender sender, String action, PlayerResolution resolution, double amount, String prefix, String reason) {
        EconomyManager econ = plugin.getEconomyManager();
        String admin = (sender instanceof Player) ? ((Player) sender).getName() : "Console";
        switch (action.toLowerCase()) {
            case "give": {
                boolean ok = resolution.isOnline
                    ? econ.deposit(resolution.onlinePlayer, amount)
                    : econ.depositPlayer(resolution.offlinePlayer, amount);
                if (ok) {
                    String money = EconomyFormat.money(amount);
                    Map<String, String> ph = new HashMap<>();
                    ph.put("money", money);
                    ph.put("player", resolution.getName());
                    sender.sendMessage(prefix + msg("admin.give_success", ph, "§aGave %money% to %player%"));
                    appendHistory(admin, "GIVE", resolution.getName(), amount, reason);
                    return true;
                }
                return false;
            }
            case "take": {
                double balance = resolution.isOnline
                    ? econ.getBalance(resolution.onlinePlayer)
                    : econ.getBalance(resolution.offlinePlayer);
                if (balance < amount) {
                    String balStr = EconomyFormat.money(balance);
                    Map<String, String> ph = new HashMap<>();
                    ph.put("balance", balStr);
                    sender.sendMessage(prefix + msg("admin.take_insufficient", ph,
                        "§cPlayer only has %balance%!"));
                    return true; // handled with specific message
                }
                boolean ok = resolution.isOnline
                    ? econ.withdraw(resolution.onlinePlayer, amount)
                    : econ.withdraw(resolution.offlinePlayer, amount);
                if (ok) {
                    String money = EconomyFormat.money(amount);
                    Map<String, String> ph = new HashMap<>();
                    ph.put("money", money);
                    ph.put("player", resolution.getName());
                    sender.sendMessage(prefix + msg("admin.take_success", ph, "§aTook %money% from %player%"));
                    appendHistory(admin, "TAKE", resolution.getName(), amount, reason);
                    return true;
                }
                return false;
            }
            case "set": {
                double current = resolution.isOnline
                    ? econ.getBalance(resolution.onlinePlayer)
                    : econ.getBalance(resolution.offlinePlayer);
                boolean withdrew = true;
                if (current > 0) {
                    withdrew = resolution.isOnline
                        ? econ.withdraw(resolution.onlinePlayer, current)
                        : econ.withdraw(resolution.offlinePlayer, current);
                }
                if (!withdrew) return false;
                boolean deposited = resolution.isOnline
                    ? econ.deposit(resolution.onlinePlayer, amount)
                    : econ.depositPlayer(resolution.offlinePlayer, amount);
                if (deposited) {
                    String money = EconomyFormat.money(amount);
                    Map<String, String> ph = new HashMap<>();
                    ph.put("player", resolution.getName());
                    ph.put("money", money);
                    sender.sendMessage(prefix + msg("admin.set_success", ph,
                        "§aSet %player%'s balance to %money%"));
                    appendHistory(admin, "SET", resolution.getName(), amount, reason);
                    return true;
                }
                return false;
            }
            default:
                // Invalid action -> report error and return true to avoid generic failure message from caller
                sender.sendMessage(prefix + msg("admin.invalid_action", null, "§cInvalid action! Use give, take, or set"));
                return true;
        }
    }

    private void handleConfirmation(CommandSender sender, String prefix) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(prefix + msg("admin.confirm_players_only", null, "§cOnly players can use confirmations!"));
            return;
        }
        
        Player player = (Player) sender;
        AdminConfirmationManager mgr = getOrCreateConfirmationManager();
        AdminConfirmationManager.PendingAdminAction pending = mgr.getPending(player.getUniqueId());
        
        if (pending == null) {
            sender.sendMessage(prefix + msg("admin.no_pending_confirm", null, "§cNo pending action to confirm!"));
            return;
        }
        
        if (pending.isExpired(nowMillis(), mgr.getExpiryMillis())) {
            mgr.remove(player.getUniqueId());
            sender.sendMessage(prefix + msg("admin.confirm_expired", null, "§cConfirmation expired! Please retry the command."));
            return;
        }
        
        // Execute the original command with confirmation bypass
        // If still awaiting reason, remind and exit
        if (mgr.isAwaitingReason(player.getUniqueId())) {
            Map<String, String> ph = new HashMap<>();
            ph.put("seconds", String.valueOf(mgr.getExpirySeconds()));
            sender.sendMessage(prefix + msg("admin.reason.still_awaiting", ph, "§ePlease type a reason in chat to proceed."));
            return;
        }
        String storedReason = mgr.getReason(player.getUniqueId());
        PlayerResolution resolution = resolvePlayer(pending.playerName);
        if (!resolution.isValid()) {
            Map<String, String> ph = new HashMap<>();
            ph.put("player", pending.playerName);
            sender.sendMessage(prefix + msg("admin.player_not_found", ph, "§cPlayer '%player%' not found or has never joined the server!"));
            return;
        }
        if (!resolution.isOnline) {
            Map<String, String> ph = new HashMap<>();
            ph.put("player", pending.playerName);
            sender.sendMessage(prefix + msg("admin.offline_note", ph, "§7Note: Player '%player%' is offline. Processing transaction..."));
        }
        boolean success = performEconomy(sender, pending.action, resolution, pending.amount, prefix, storedReason);
        if (!success) {
            sender.sendMessage(prefix + msg("admin.failed_execute", null, "§cFailed to execute economy command!"));
        }
        // Remove pending after execution attempt
        mgr.remove(player.getUniqueId());
    }

    private void showAdminHelp(CommandSender sender, String prefix) {
        sender.sendMessage(msg("admin.help.header", null, "§8§m----------§r §6Admin Help §8§m----------"));
        sender.sendMessage(msg("admin.help.reload", null, "§f/djeconomy reload §7- Reload configuration"));
        sender.sendMessage(msg("admin.help.doctor", null, "§f/djeconomy doctor §7- Run system diagnostics"));
        sender.sendMessage(msg("admin.help.gui", null, "§f/djeconomy gui §7- Open the Admin Economy GUI"));
        sender.sendMessage(msg("admin.help.setlevel", null, "§f/djeconomy setlevel <player> <job> <level> §7- Set player's job level (supports offline)"));
        sender.sendMessage(msg("admin.help.getlevel", null, "§f/djeconomy getlevel <player> <job> §7- Show player's job level (supports offline)"));
        sender.sendMessage(msg("admin.help.resetlevel", null, "§f/djeconomy resetlevel <player> <job> §7- Reset player's job level to 1"));
        sender.sendMessage(msg("admin.help.addxp", null, "§f/djeconomy addxp <player> <job> <amount> §7- Add XP to player's job (online only)"));
        sender.sendMessage(msg("admin.help.economy", null, "§f/djeconomy economy <give|take|set> <player> <amount> §7- Manage player money"));
        sender.sendMessage(msg("admin.help.history", null, "§f/djeconomy history <player> [page] [size] §7- View admin economy history (with reasons)"));
        sender.sendMessage(msg("admin.help.refreshjobs", null, "§f/djeconomy refreshjobs <player> §7- Reload a player's job data from DB (online only)"));
        sender.sendMessage(msg("admin.help.invalidatejobs", null, "§f/djeconomy invalidatejobs <player> §7- Invalidate cached job data (online only)"));
        sender.sendMessage(msg("admin.help.businessinfo", null, "§f/djeconomy businessinfo [businessName] §7- View global or per-business stats"));
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

    /**
     * Obtain the AdminConfirmationManager, falling back to a local instance when the plugin
     * (often a Mockito mock in tests) does not provide one. This guarantees confirmation
     * gating works consistently across runtime and tests.
     */
    private AdminConfirmationManager getOrCreateConfirmationManager() {
        AdminConfirmationManager mgr = null;
        try {
            mgr = plugin.getAdminConfirmationManager();
        } catch (Throwable ignored) {}
        if (mgr == null) {
            if (localAdminConfirm == null) {
                localAdminConfirm = new AdminConfirmationManager(plugin);
            }
            return localAdminConfirm;
        }
        return mgr;
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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> base = Arrays.asList("reload", "doctor", "gui", "setlevel", "getlevel", "resetlevel", "addxp", "economy", "history", "refreshjobs", "invalidatejobs", "businessinfo");
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
            if (args[0].equalsIgnoreCase("businessinfo")) {
                ConsolidatedBusinessManager mgr = plugin.getConsolidatedBusinessManager();
                if (mgr == null) return new ArrayList<>();
                return mgr.getAllBusinesses().stream()
                    .map(Business::getName)
                    .filter(n -> n != null && n.toLowerCase().startsWith(args[1].toLowerCase()))
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
                // 3rd arg is treated as size when 4th is absent
                return Arrays.asList("1", "5", "10", "20", "50", "100").stream()
                    .filter(s -> s.startsWith(args[2]))
                    .collect(Collectors.toList());
            }
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("history")) {
            return Arrays.asList("5", "10", "20", "50", "100").stream()
                .filter(s -> s.startsWith(args[3]))
                .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }

    private String getPrefix() {
        // Centralized prefix retrieval via Messages; it already prefers config.yml override then messages.yml
        try {
            if (plugin.getMessages() != null) {
                String p = plugin.getMessages().getPrefix();
                if (p != null && !p.isEmpty()) return p;
            }
            if (plugin.getConfig() != null) {
                // Provide non-null default so Mockito anyString() stubs match in tests
                String fromCfg = plugin.getConfig().getString("messages.prefix", "");
                if (fromCfg != null && !fromCfg.isEmpty()) return fromCfg;
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
            case "doctor":
                return "djeconomy.system.doctor";
            case "gui":
                return "djeconomy.gui.admin.economy";
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
            case "businessinfo":
                return "djeconomy.admin.businessinfo";
            default:
                return null;
        }
    }

    private void handleDoctor(CommandSender sender, String prefix) {
        // Delegate to the new DoctorCommandExecutor for maintainability and testing
        new DoctorCommandExecutor(plugin).execute(sender);
    }

    private void handleBusinessInfo(CommandSender sender, String[] args, String prefix) {
        ConsolidatedBusinessManager mgr = plugin.getConsolidatedBusinessManager();
        if (mgr == null) {
            sender.sendMessage(prefix + msg("admin.businessinfo.unavailable", null, "§cBusiness system is unavailable."));
            return;
        }

        if (args.length == 1) {
            int totalBusinesses = mgr.getTotalBusinessesCount();
            int totalPositions = mgr.getTotalActivePositionsCount();
            int totalEmployees = mgr.getTotalActiveEmployeesCount();
            int totalPending = mgr.getTotalPendingHiringRequestsCount();

            sender.sendMessage(msg("admin.businessinfo.header.global", null, "§6Business Statistics (Global)"));
            Map<String, String> ph = new HashMap<>();
            ph.put("businesses", String.valueOf(totalBusinesses));
            ph.put("positions", String.valueOf(totalPositions));
            ph.put("employees", String.valueOf(totalEmployees));
            ph.put("pending", String.valueOf(totalPending));
            sender.sendMessage(msg("admin.businessinfo.global", ph,
                "§7Total Businesses: §f%businesses%§7, Active Positions: §f%positions%§7, Employees: §f%employees%§7, Pending Requests: §f%pending%"));
            return;
        }

        String businessName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        Business b = mgr.getBusinessByName(businessName);
        if (b == null) {
            Map<String, String> ph = new HashMap<>();
            ph.put("name", businessName);
            sender.sendMessage(prefix + msg("admin.businessinfo.not_found", ph, "§cBusiness '%name%' not found."));
            return;
        }

        int activePositions = mgr.getActivePositionsCount(b.getId());
        int activeEmployees = mgr.getEmployeesCount(b.getId());
        int pending = mgr.getPendingHiringRequestsCountForBusiness(b.getId());

        Map<String, String> ph = new HashMap<>();
        ph.put("name", b.getName());
        ph.put("id", String.valueOf(b.getId()));
        ph.put("positions", String.valueOf(activePositions));
        ph.put("employees", String.valueOf(activeEmployees));
        ph.put("pending", String.valueOf(pending));
        sender.sendMessage(msg("admin.businessinfo.header.business", ph, "§6Business: §e%name% §7(ID: %id%)"));
        sender.sendMessage(msg("admin.businessinfo.business", ph,
            "§7Active Positions: §f%positions%§7, Employees: §f%employees%§7, Pending Requests: §f%pending%"));
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

    private void appendHistory(String admin, String action, String target, double amount, String reason) {
        File file = getHistoryFile();
        try (PrintWriter out = new PrintWriter(new FileWriter(file, true))) {
            String safeReason = reason == null ? "" : reason.replace('\n', ' ').replace('\r', ' ');
            out.printf("%d|%s|%s|%s|%.2f|%s%n", System.currentTimeMillis(), admin, action, target, amount, safeReason);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to write history: " + e.getMessage());
        }
    }

    private void handleHistory(CommandSender sender, String playerName, String pageStr, String sizeStr, String prefix) {
        int page = 1;
        int size = 10;
        try {
            if (sizeStr != null) {
                if (pageStr != null) page = Integer.parseInt(pageStr);
                size = Integer.parseInt(sizeStr);
            } else if (pageStr != null) {
                size = Integer.parseInt(pageStr);
            }
        } catch (NumberFormatException ignored) {
        }
        if (page < 1) page = 1;
        if (size < 1) size = 1;
        if (size > 100) size = 100;

        Path path = getHistoryFile().toPath();
        if (!Files.exists(path)) {
            Map<String, String> ph = new HashMap<>();
            ph.put("player", playerName);
            sender.sendMessage(prefix + msg("admin.history_none", ph, "§7No history found for '%player%'."));
            return;
        }
        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            List<String> entries = new ArrayList<>();
            for (int i = lines.size() - 1; i >= 0; i--) {
                String line = lines.get(i);
                String[] parts = line.split("\\|", -1);
                if (parts.length < 5) continue;
                String target = parts[3];
                if (!target.equalsIgnoreCase(playerName)) continue;
                long ts;
                try {
                    ts = Long.parseLong(parts[0]);
                } catch (NumberFormatException ex) {
                    ts = System.currentTimeMillis();
                }
                String admin = parts[1];
                String action = parts[2];
                String amount = parts[4];
                String reason = parts.length >= 6 ? parts[5] : null;
                String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(ts));
                String formatted = reason == null || reason.isEmpty()
                    ? String.format("§7[%s] §f%s §7-> §6%s §7$%s", time, admin, action + " " + target, amount)
                    : String.format("§7[%s] §f%s §7-> §6%s §7$%s §8| §7Reason: §f%s", time, admin, action + " " + target, amount, reason);
                entries.add(formatted);
            }
            if (entries.isEmpty()) {
                Map<String, String> ph = new HashMap<>();
                ph.put("player", playerName);
                sender.sendMessage(prefix + msg("admin.history_none", ph, "§7No history found for '%player%'."));
                return;
            }

            int total = entries.size();
            int totalPages = (int) Math.ceil(total / (double) size);
            if (page > totalPages) page = totalPages;
            int from = Math.max(0, (page - 1) * size);
            int to = Math.min(total, from + size);
            Map<String, String> ph2 = new HashMap<>();
            ph2.put("player", playerName);
            ph2.put("page", String.valueOf(page));
            ph2.put("pages", String.valueOf(Math.max(totalPages, 1)));
            ph2.put("count", String.valueOf(to - from));
            sender.sendMessage(prefix + msg("admin.history_header", ph2, "§eShowing last %count% entries for '%player%' §7(Page %page%/%pages%)"));
            for (int i = from; i < to; i++) {
                sender.sendMessage(entries.get(i));
            }
        } catch (IOException e) {
            Map<String, String> ph = new HashMap<>();
            ph.put("error", e.getMessage());
            sender.sendMessage(prefix + msg("admin.history_read_failed", ph, "§cFailed to read history: %error%"));
        }
    }
}
