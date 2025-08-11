package com.boopugstudios.dynamicjobseconomy.listeners;

import com.boopugstudios.dynamicjobseconomy.DynamicJobsEconomy;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {
    
    private final DynamicJobsEconomy plugin;
    
    public PlayerListener(DynamicJobsEconomy plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Load player data
        plugin.getJobManager().getPlayerData(player);
        
        // Initialize player in economy system
        plugin.getEconomyManager().getBalance(player);
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Save player job data to database
        plugin.getJobManager().savePlayerData(player);
        
        // Clean up memory
        plugin.getJobManager().unloadPlayerData(player);
        
        plugin.getLogger().info("Saved and unloaded data for player: " + player.getName());
    }
}
