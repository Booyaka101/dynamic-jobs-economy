package com.boopugstudios.dynamicjobseconomy.jobs;

import java.util.HashMap;
import java.util.Map;

public class Job {
    
    private final String name;
    private final String displayName;
    private final String description;
    private final int baseIncome;
    private final int xpPerAction;
    private final int maxLevel;
    private final Map<Integer, String> perks;
    
    public Job(String name, String displayName, String description, int baseIncome, int xpPerAction, int maxLevel) {
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.baseIncome = baseIncome;
        this.xpPerAction = xpPerAction;
        this.maxLevel = maxLevel;
        this.perks = new HashMap<>();
    }
    
    public void addPerk(int level, String perk) {
        perks.put(level, perk);
    }
    
    public String getPerkAtLevel(int level) {
        return perks.get(level);
    }
    
    public Map<Integer, String> getAllPerks() {
        return new HashMap<>(perks);
    }
    
    // Getters
    public String getName() {
        return name;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public int getBaseIncome() {
        return baseIncome;
    }
    
    public int getXpPerAction() {
        return xpPerAction;
    }
    
    public int getMaxLevel() {
        return maxLevel;
    }
}
