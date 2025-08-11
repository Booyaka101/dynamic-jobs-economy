package com.boopugstudios.dynamicjobseconomy.listeners;

import com.boopugstudios.dynamicjobseconomy.DynamicJobsEconomy;
import org.bukkit.event.Listener;

public class BusinessListener implements Listener {
    
    @SuppressWarnings("unused") // Will be used when business events are implemented
    private final DynamicJobsEconomy plugin;
    
    public BusinessListener(DynamicJobsEconomy plugin) {
        this.plugin = plugin;
    }
    
    // Business-related event handlers will be implemented here
    // Future events: business transactions, employee actions, etc.
}
