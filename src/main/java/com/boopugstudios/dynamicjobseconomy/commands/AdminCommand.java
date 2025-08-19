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


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class AdminCommand implements CommandExecutor, TabCompleter {
    
    private final DynamicJobsEconomy plugin;
    private final Map<UUID, PendingAdminAction> pendingConfirmations = new HashMap<>();
    private static final double LARGE_AMOUNT_THRESHOLD = 100000.0; // $100k threshold
    
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
        Player onlinePlayer = Bukkit.getPlayer(playerName);
        if (onlinePlayer != null) {
            return new PlayerResolution(onlinePlayer, onlinePlayer, true);
        }
        
        // Try to find in server's cached offline players
        for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
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
        
        PendingAdminAction(String action, String playerName, double amount) {
            this.action = action;
            this.playerName = playerName;
            this.amount = amount;
            this.timestamp = System.currentTimeMillis();
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > 30000; // 30 second timeout
        }
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6DynamicJobs§8] ");
        
        // Accept either the new permission or the legacy wildcard for backward compatibility
        if (!(sender.hasPermission("djeconomy.admin") || sender.hasPermission("dynamicjobs.admin.*"))) {
            sender.sendMessage(prefix + "§cYou don't have permission to use this command!");
            return true;
        }
        
        if (args.length == 0) {
            showAdminHelp(sender, prefix);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "reload":
                plugin.reloadConfig();
                sender.sendMessage(prefix + "§aConfiguration reloaded!");
                break;
            case "confirm":
                handleConfirmation(sender, prefix);
                break;
            case "economy":
                if (args.length < 4) {
                    sender.sendMessage(prefix + "§cUsage: /djeconomy economy <give|take|set> <player> <amount>");
                    return true;
                }
                handleEconomy(sender, args[1], args[2], args[3], prefix);
                break;
            case "setlevel":
                if (args.length < 4) {
                    sender.sendMessage(prefix + "§cUsage: /djeconomy setlevel <player> <job> <level>");
                    return true;
                }
                handleSetLevel(sender, args[1], args[2], args[3], prefix);
                break;
            case "addxp":
                if (args.length < 4) {
                    sender.sendMessage(prefix + "§cUsage: /djeconomy addxp <player> <job> <amount>");
                    return true;
                }
                handleAddXP(sender, args[1], args[2], args[3], prefix);
                break;
            case "refreshjobs":
                if (args.length < 2) {
                    sender.sendMessage(prefix + "§cUsage: /djeconomy refreshjobs <player>");
                    return true;
                }
                handleRefreshJobs(sender, args[1], prefix);
                break;
            case "invalidatejobs":
                if (args.length < 2) {
                    sender.sendMessage(prefix + "§cUsage: /djeconomy invalidatejobs <player>");
                    return true;
                }
                handleInvalidateJobs(sender, args[1], prefix);
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
            sender.sendMessage(prefix + "§cPlayer '" + playerName + "' not found or has never joined the server!");
            return;
        }
        int level;
        try {
            level = Integer.parseInt(levelStr);
        } catch (NumberFormatException e) {
            sender.sendMessage(prefix + "§cInvalid level number!");
            return;
        }

        boolean ok;
        if (resolution.isOnline) {
            ok = plugin.getJobManager().setJobLevel(resolution.onlinePlayer, jobName, level);
        } else {
            ok = plugin.getJobManager().setOfflineJobLevel(resolution.offlinePlayer, jobName, level);
        }

        if (!ok) {
            sender.sendMessage(prefix + "§cUnknown job '" + jobName + "'.");
            return;
        }

        sender.sendMessage(prefix + "§aSet " + resolution.getName() + "'s " + jobName + " level to " + level + (resolution.isOnline ? " (online)" : " (offline)") );
    }
    
    private void handleAddXP(CommandSender sender, String playerName, String jobName, String xpStr, String prefix) {
        // Use modern player resolution
        PlayerResolution resolution = resolvePlayer(playerName);
        
        if (!resolution.isValid()) {
            sender.sendMessage(prefix + "§cPlayer '" + playerName + "' not found or has never joined the server!");
            return;
        }
        
        if (!resolution.isOnline) {
            sender.sendMessage(prefix + "§cCannot add XP to offline player!");
            return;
        }
        
        try {
            int xp = Integer.parseInt(xpStr);
            // Resolve job (case-insensitive)
            com.boopugstudios.dynamicjobseconomy.jobs.Job job = plugin.getJobManager().getJob(jobName);
            if (job == null) {
                sender.sendMessage(prefix + "§cUnknown job '" + jobName + "'.");
                return;
            }
            String canonical = job.getName();
            // Validate player has joined the job
            com.boopugstudios.dynamicjobseconomy.jobs.PlayerJobData pdata = plugin.getJobManager().getPlayerData(resolution.onlinePlayer);
            if (!pdata.hasJob(canonical)) {
                sender.sendMessage(prefix + "§c" + resolution.getName() + " has not joined the job '" + canonical + "'.");
                return;
            }
            plugin.getJobManager().addExperience(resolution.onlinePlayer, canonical, xp);
            sender.sendMessage(prefix + "§aAdded " + xp + " XP to " + resolution.getName() + "'s '" + canonical + "' job");
        } catch (NumberFormatException e) {
            sender.sendMessage(prefix + "§cInvalid XP amount!");
        }
    }
    
    private void handleEconomy(CommandSender sender, String action, String playerName, String amountStr, String prefix) {
        // Use modern player resolution
        PlayerResolution resolution = resolvePlayer(playerName);
        
        if (!resolution.isValid()) {
            sender.sendMessage(prefix + "§cPlayer '" + playerName + "' not found or has never joined the server!");
            return;
        }
        
        if (!resolution.isOnline) {
            sender.sendMessage(prefix + "§7Note: Player '" + playerName + "' is offline. Processing transaction...");
        }
        
        try {
            double amount = Double.parseDouble(amountStr);
            
            // Validate amount
            if (amount < 0) {
                sender.sendMessage(prefix + "§cAmount cannot be negative!");
                return;
            }
            
            if (amount > 1000000000) { // 1 billion limit
                sender.sendMessage(prefix + "§cAmount too large! Maximum: $1,000,000,000");
                return;
            }
            
            // Check for large amounts requiring confirmation
            if (amount >= LARGE_AMOUNT_THRESHOLD) {
                UUID senderUUID = sender instanceof Player ? ((Player) sender).getUniqueId() : null;
                if (senderUUID != null) {
                    PendingAdminAction pending = pendingConfirmations.get(senderUUID);
                    if (pending == null || pending.isExpired() || 
                        !pending.action.equals(action) || !pending.playerName.equals(playerName) || 
                        pending.amount != amount) {
                        
                        // Store pending action
                        pendingConfirmations.put(senderUUID, new PendingAdminAction(action, playerName, amount));
                        sender.sendMessage(prefix + "§e⚠ Large amount detected: $" + String.format("%.2f", amount));
                        sender.sendMessage(prefix + "§eUse §f/djeconomy confirm §eto proceed (expires in 30 seconds)");
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
                    if (resolution.isOnline) {
                        success = plugin.getEconomyManager().deposit(resolution.onlinePlayer, amount);
                    } else {
                        success = plugin.getEconomyManager().depositPlayer(resolution.offlinePlayer, amount);
                    }
                    if (success) {
                        sender.sendMessage(prefix + "§aGave $" + String.format("%.2f", amount) + " to " + resolution.getName());
                        logAdminAction(sender, "GIVE", resolution.getName(), amount);
                    }
                    break;
                case "take":
                    double currentBalance = resolution.isOnline ? 
                        plugin.getEconomyManager().getBalance(resolution.onlinePlayer) :
                        plugin.getEconomyManager().getBalance(resolution.offlinePlayer);
                    if (currentBalance < amount) {
                        sender.sendMessage(prefix + "§cPlayer only has $" + String.format("%.2f", currentBalance) + "!");
                        return;
                    }
                    if (resolution.isOnline) {
                        success = plugin.getEconomyManager().withdraw(resolution.onlinePlayer, amount);
                    } else {
                        success = plugin.getEconomyManager().withdraw(resolution.offlinePlayer, amount);
                    }
                    if (success) {
                        sender.sendMessage(prefix + "§aTook $" + String.format("%.2f", amount) + " from " + resolution.getName());
                        logAdminAction(sender, "TAKE", resolution.getName(), amount);
                    }
                    break;
                case "set":
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
                        sender.sendMessage(prefix + "§aSet " + resolution.getName() + "'s balance to $" + String.format("%.2f", amount));
                        logAdminAction(sender, "SET", resolution.getName(), amount);
                        success = true;
                    }
                    break;
                default:
                    sender.sendMessage(prefix + "§cInvalid action! Use give, take, or set");
                    return;
            }
            
            if (!success) {
                sender.sendMessage(prefix + "§cFailed to execute economy command!");
            }
            
        } catch (NumberFormatException e) {
            sender.sendMessage(prefix + "§cInvalid amount!");
        }
    }
    
    private void handleConfirmation(CommandSender sender, String prefix) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(prefix + "§cOnly players can use confirmations!");
            return;
        }
        
        Player player = (Player) sender;
        PendingAdminAction pending = pendingConfirmations.get(player.getUniqueId());
        
        if (pending == null) {
            sender.sendMessage(prefix + "§cNo pending action to confirm!");
            return;
        }
        
        if (pending.isExpired()) {
            pendingConfirmations.remove(player.getUniqueId());
            sender.sendMessage(prefix + "§cConfirmation expired! Please retry the command.");
            return;
        }
        
        // Re-execute the original command with confirmation bypass
        handleEconomy(sender, pending.action, pending.playerName, String.valueOf(pending.amount), prefix);
    }
    
    private void logAdminAction(CommandSender sender, String action, String targetPlayer, double amount) {
        String adminName = sender instanceof Player ? ((Player) sender).getName() : "CONSOLE";
        plugin.getLogger().info(String.format("[ADMIN] %s used %s on %s for $%.2f", 
            adminName, action, targetPlayer, amount));
    }
    
    private void showAdminHelp(CommandSender sender, String prefix) {
        sender.sendMessage("§8§m----------§r §6Admin Help §8§m----------");
        sender.sendMessage("§f/djeconomy reload §7- Reload configuration");
        sender.sendMessage("§f/djeconomy setlevel <player> <job> <level> §7- Set player's job level (supports offline)");
        sender.sendMessage("§f/djeconomy addxp <player> <job> <amount> §7- Add XP to player's job (online only)");
        sender.sendMessage("§f/djeconomy economy <give|take|set> <player> <amount> §7- Manage player money");
        sender.sendMessage("§f/djeconomy refreshjobs <player> §7- Reload a player's job data from DB (online only)");
        sender.sendMessage("§f/djeconomy invalidatejobs <player> §7- Invalidate cached job data (online only)");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("reload", "setlevel", "addxp", "economy", "refreshjobs", "invalidatejobs").stream()
                .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("economy")) {
                return Arrays.asList("give", "take", "set").stream()
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            }
            if (args[0].equalsIgnoreCase("refreshjobs") || args[0].equalsIgnoreCase("invalidatejobs")
                    || args[0].equalsIgnoreCase("setlevel") || args[0].equalsIgnoreCase("addxp")) {
                return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            }
        }

        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("setlevel") || args[0].equalsIgnoreCase("addxp")) {
                return JobNameUtil.suggestJobs(plugin.getJobManager().getJobs().keySet(), args[2]);
            }
            if (args[0].equalsIgnoreCase("economy")) {
                return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
            }
        }

        return new ArrayList<>();
    }

    private void handleRefreshJobs(CommandSender sender, String playerName, String prefix) {
        PlayerResolution resolution = resolvePlayer(playerName);
        if (!resolution.isValid()) {
            sender.sendMessage(prefix + "§cPlayer '" + playerName + "' not found or has never joined the server!");
            return;
        }
        if (!resolution.isOnline) {
            sender.sendMessage(prefix + "§cPlayer must be online to refresh job data!");
            return;
        }
        plugin.getJobManager().refreshPlayerData(resolution.onlinePlayer);
        sender.sendMessage(prefix + "§aRefreshed job data for " + resolution.getName());
    }

    private void handleInvalidateJobs(CommandSender sender, String playerName, String prefix) {
        PlayerResolution resolution = resolvePlayer(playerName);
        if (!resolution.isValid()) {
            sender.sendMessage(prefix + "§cPlayer '" + playerName + "' not found or has never joined the server!");
            return;
        }
        if (!resolution.isOnline) {
            sender.sendMessage(prefix + "§cPlayer must be online to invalidate cached job data!");
            return;
        }
        plugin.getJobManager().invalidatePlayerData(resolution.onlinePlayer);
        sender.sendMessage(prefix + "§aInvalidated cached job data for " + resolution.getName());
    }
}
