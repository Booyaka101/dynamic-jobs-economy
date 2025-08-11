package com.boopugstudios.dynamicjobseconomy.commands;

import com.boopugstudios.dynamicjobseconomy.DynamicJobsEconomy;
import com.boopugstudios.dynamicjobseconomy.gigs.Gig;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class GigsCommand implements CommandExecutor, TabCompleter {
    
    private final DynamicJobsEconomy plugin;
    
    public GigsCommand(DynamicJobsEconomy plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }
        
        Player player = (Player) sender;
        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6DynamicJobs§8] ");
        
        if (args.length == 0) {
            showGigsHelp(player, prefix);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "list":
                handleListGigs(player);
                break;
            case "create":
                handleCreateGig(player, args);
                break;
            case "accept":
                handleAcceptGig(player, args);
                break;
            case "complete":
                handleCompleteGig(player, args);
                break;
            case "review":
            case "approve":
                handleApproveGig(player, args);
                break;
            case "reject":
                handleRejectGig(player, args);
                break;
            case "cancel":
                handleCancelGig(player, args);
                break;
            case "mine":
                handleMyGigs(player);
                break;
                
            default:
                showGigsHelp(player, prefix);
                break;
        }
        
        return true;
    }
    
    private void showGigsHelp(Player player, String prefix) {
        player.sendMessage("§8§m----------§r §6Gigs Help §8§m----------");
        player.sendMessage(prefix + "§7/gigs list - View available gigs");
        player.sendMessage(prefix + "§7/gigs create <title> <payment> <description> §7- Create a new gig");
        player.sendMessage(prefix + "§7/gigs accept <id> - Accept a gig");
        player.sendMessage(prefix + "§7/gigs complete <id> - Submit gig completion");
        player.sendMessage(prefix + "§7/gigs approve <id> - Approve completed gig (poster only)");
        player.sendMessage(prefix + "§7/gigs reject <id> [reason] - Reject gig submission (poster only)");
        player.sendMessage(prefix + "§7/gigs cancel <id> - Cancel your posted gig");
        player.sendMessage(prefix + "§7/gigs mine - View your gigs");
    }
    
    private void handleListGigs(Player player) {
        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6DJE§8] ");
        List<Gig> openGigs = plugin.getGigManager().getOpenGigs();
        
        if (openGigs.isEmpty()) {
            player.sendMessage(prefix + "§7No gigs available at the moment.");
            return;
        }
        
        player.sendMessage(prefix + "§6Available Gigs:");
        for (Gig gig : openGigs) {
            player.sendMessage(String.format("§7[§e%d§7] §f%s §7- §a$%.2f", 
                gig.getId(), gig.getTitle(), gig.getPayment()));
            player.sendMessage("  §7" + gig.getDescription());
        }
    }
    
    private void handleCreateGig(Player player, String[] args) {
        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6DJE§8] ");
        
        if (args.length < 4) {
            player.sendMessage(prefix + "§cUsage: /gigs create <title> <payment> <description>");
            return;
        }
        
        String title = args[1];
        double payment;
        
        try {
            payment = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage(prefix + "§cInvalid payment amount!");
            return;
        }
        
        if (payment <= 0) {
            player.sendMessage(prefix + "§cPayment must be greater than 0!");
            return;
        }
        
        StringBuilder description = new StringBuilder();
        for (int i = 3; i < args.length; i++) {
            description.append(args[i]).append(" ");
        }
        
        if (plugin.getGigManager().createGig(player, title, description.toString().trim(), payment)) {
            player.sendMessage(prefix + "§aGig created successfully!");
        } else {
            player.sendMessage(prefix + "§cFailed to create gig. Check your balance!");
        }
    }
    
    private void handleAcceptGig(Player player, String[] args) {
        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6DJE§8] ");
        
        if (args.length < 2) {
            player.sendMessage(prefix + "§cUsage: /gigs accept <gig_id>");
            return;
        }
        
        int gigId;
        try {
            gigId = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(prefix + "§cInvalid gig ID!");
            return;
        }
        
        if (plugin.getGigManager().acceptGig(player, gigId)) {
            player.sendMessage(prefix + "§aGig accepted! You can now work on it.");
        } else {
            player.sendMessage(prefix + "§cFailed to accept gig. It may no longer be available.");
        }
    }
    
    private void handleCompleteGig(Player player, String[] args) {
        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6DJE§8] ");
        
        if (args.length < 2) {
            player.sendMessage(prefix + "§cUsage: /gigs complete <gig_id>");
            return;
        }
        
        int gigId;
        try {
            gigId = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(prefix + "§cInvalid gig ID!");
            return;
        }
        
        if (plugin.getGigManager().submitCompletion(player, gigId)) {
            player.sendMessage(prefix + "§aGig completion submitted! Waiting for poster approval.");
        } else {
            player.sendMessage(prefix + "§cFailed to submit completion. Make sure you're assigned to it.");
        }
    }
    
    private void handleApproveGig(Player player, String[] args) {
        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6DJE§8] ");
        
        if (args.length < 2) {
            player.sendMessage(prefix + "§cUsage: /gigs approve <gig_id>");
            return;
        }
        
        int gigId;
        try {
            gigId = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(prefix + "§cInvalid gig ID!");
            return;
        }
        
        if (plugin.getGigManager().approveGig(player, gigId)) {
            player.sendMessage(prefix + "§aGig approved! Payment has been released to the worker.");
        } else {
            player.sendMessage(prefix + "§cFailed to approve gig. Make sure it's pending approval and you're the poster.");
        }
    }
    
    private void handleRejectGig(Player player, String[] args) {
        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6DJE§8] ");
        
        if (args.length < 2) {
            player.sendMessage(prefix + "§cUsage: /gigs reject <gig_id> [reason]");
            return;
        }
        
        int gigId;
        try {
            gigId = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(prefix + "§cInvalid gig ID!");
            return;
        }
        
        String reason = "";
        if (args.length > 2) {
            StringBuilder sb = new StringBuilder();
            for (int i = 2; i < args.length; i++) {
                sb.append(args[i]).append(" ");
            }
            reason = sb.toString().trim();
        }
        
        if (plugin.getGigManager().rejectGig(player, gigId, reason)) {
            player.sendMessage(prefix + "§cGig submission rejected. Worker has been notified.");
        } else {
            player.sendMessage(prefix + "§cFailed to reject gig. Make sure it's pending approval and you're the poster.");
        }
    }
    
    private void handleCancelGig(Player player, String[] args) {
        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6DJE§8] ");
        
        if (args.length < 2) {
            player.sendMessage(prefix + "§cUsage: /gigs cancel <gig_id>");
            return;
        }
        
        int gigId;
        try {
            gigId = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(prefix + "§cInvalid gig ID!");
            return;
        }
        
        if (plugin.getGigManager().cancelGig(player, gigId)) {
            player.sendMessage(prefix + "§aGig cancelled. Refund has been processed.");
        } else {
            player.sendMessage(prefix + "§cFailed to cancel gig. Make sure you're the poster and it's not completed.");
        }
    }
    
    private void handleMyGigs(Player player) {
        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6DJE§8] ");
        List<Gig> playerGigs = plugin.getGigManager().getPlayerGigs(player);
        
        if (playerGigs.isEmpty()) {
            player.sendMessage(prefix + "§7You have no active gigs.");
            return;
        }
        
        player.sendMessage(prefix + "§6Your Gigs:");
        for (Gig gig : playerGigs) {
            String role = player.getUniqueId().equals(gig.getPosterUUID()) ? "Posted" : "Working";
            player.sendMessage(String.format("§7[§e%d§7] §f%s §7- §a$%.2f §7(%s - %s)", 
                gig.getId(), gig.getTitle(), gig.getPayment(), role, gig.getStatus()));
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("create", "list", "accept", "complete", "approve", "reject", "cancel", "mine").stream()
                .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        return new ArrayList<>();
    }
}
