package com.boopugstudios.dynamicjobseconomy.commands;

import com.boopugstudios.dynamicjobseconomy.DynamicJobsEconomy;
import com.boopugstudios.dynamicjobseconomy.jobs.Job;
import com.boopugstudios.dynamicjobseconomy.jobs.JobLevel;
import com.boopugstudios.dynamicjobseconomy.jobs.PlayerJobData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class JobsCommand implements CommandExecutor, TabCompleter {
    
    private final DynamicJobsEconomy plugin;
    
    public JobsCommand(DynamicJobsEconomy plugin) {
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
            showJobsHelp(player, prefix);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "join":
                if (args.length < 2) {
                    player.sendMessage(prefix + "§cUsage: /jobs join <job>");
                    return true;
                }
                handleJoinJob(player, args[1], prefix);
                break;
                
            case "leave":
                if (args.length < 2) {
                    player.sendMessage(prefix + "§cUsage: /jobs leave <job>");
                    return true;
                }
                handleLeaveJob(player, args[1], prefix);
                break;
                
            case "info":
                if (args.length < 2) {
                    showAllJobs(player, prefix);
                } else {
                    showJobInfo(player, args[1], prefix);
                }
                break;
                
            case "stats":
                showPlayerStats(player, prefix);
                break;
                
            default:
                showJobsHelp(player, prefix);
                break;
        }
        
        return true;
    }
    
    private void handleJoinJob(Player player, String jobName, String prefix) {
        if (!plugin.getJobManager().getJobs().containsKey(jobName)) {
            player.sendMessage(prefix + "§cJob '" + jobName + "' does not exist!");
            return;
        }
        
        if (plugin.getJobManager().joinJob(player, jobName)) {
            String message = plugin.getConfig().getString("messages.job_joined", "§aYou have joined the %job% job!")
                .replace("%job%", plugin.getJobManager().getJob(jobName).getDisplayName())
                .replace("&", "§");
            player.sendMessage(prefix + message);
        } else {
            player.sendMessage(prefix + "§cCould not join job. You may already have this job or reached the maximum number of jobs.");
        }
    }
    
    private void handleLeaveJob(Player player, String jobName, String prefix) {
        if (plugin.getJobManager().leaveJob(player, jobName)) {
            String message = plugin.getConfig().getString("messages.job_left", "§cYou have left the %job% job!")
                .replace("%job%", plugin.getJobManager().getJob(jobName).getDisplayName())
                .replace("&", "§");
            player.sendMessage(prefix + message);
        } else {
            player.sendMessage(prefix + "§cYou are not in that job!");
        }
    }
    
    private void showJobInfo(Player player, String jobName, String prefix) {
        Job job = plugin.getJobManager().getJob(jobName);
        if (job == null) {
            player.sendMessage(prefix + "§cJob '" + jobName + "' does not exist!");
            return;
        }
        
        player.sendMessage("§8§m----------§r " + job.getDisplayName() + " §8§m----------");
        player.sendMessage("§7Description: §f" + job.getDescription());
        player.sendMessage("§7Base Income: §a$" + job.getBaseIncome());
        player.sendMessage("§7XP per Action: §b" + job.getXpPerAction());
        player.sendMessage("§7Max Level: §e" + job.getMaxLevel());
        
        if (!job.getAllPerks().isEmpty()) {
            player.sendMessage("§7Perks:");
            job.getAllPerks().entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e1.getKey(), e2.getKey()))
                .forEach(entry -> player.sendMessage("  §7Level " + entry.getKey() + ": §f" + entry.getValue()));
        }
    }
    
    private void showAllJobs(Player player, String prefix) {
        player.sendMessage("§8§m----------§r §6Available Jobs §8§m----------");
        for (Job job : plugin.getJobManager().getJobs().values()) {
            player.sendMessage("§e" + job.getName() + " §7- " + job.getDescription());
        }
        player.sendMessage("§7Use §f/jobs info <job> §7for more details");
    }
    
    private void showPlayerStats(Player player, String prefix) {
        PlayerJobData data = plugin.getJobManager().getPlayerData(player);
        
        if (data.getJobs().isEmpty()) {
            player.sendMessage(prefix + "§7You don't have any jobs yet! Use §f/jobs join <job> §7to get started.");
            return;
        }
        
        player.sendMessage("§8§m----------§r §6Your Jobs §8§m----------");
        for (String jobName : data.getJobs()) {
            Job job = plugin.getJobManager().getJob(jobName);
            JobLevel level = data.getJobLevel(jobName);
            
            if (job != null) {
                player.sendMessage("§e" + job.getDisplayName() + " §7- Level §b" + level.getLevel() + 
                    " §7(§a" + level.getExperience() + " XP§7)");
            }
        }
    }
    
    private void showJobsHelp(Player player, String prefix) {
        player.sendMessage("§8§m----------§r §6Jobs Help §8§m----------");
        player.sendMessage("§f/jobs info §7- Show all available jobs");
        player.sendMessage("§f/jobs info <job> §7- Show detailed job information");
        player.sendMessage("§f/jobs join <job> §7- Join a job");
        player.sendMessage("§f/jobs leave <job> §7- Leave a job");
        player.sendMessage("§f/jobs stats §7- Show your job statistics");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("join", "leave", "info", "stats").stream()
                .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        if (args.length == 2 && (args[0].equalsIgnoreCase("join") || args[0].equalsIgnoreCase("leave") || args[0].equalsIgnoreCase("info"))) {
            return plugin.getJobManager().getJobs().keySet().stream()
                .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        return new ArrayList<>();
    }
}
