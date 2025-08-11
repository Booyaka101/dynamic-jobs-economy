package com.boopugstudios.dynamicjobseconomy.jobs;

public class JobLevel {
    
    private int level;
    private int experience;
    
    public JobLevel() {
        this.level = 1;
        this.experience = 0;
    }
    
    public JobLevel(int level, int experience) {
        this.level = level;
        this.experience = experience;
    }
    
    public void addExperience(int xp) {
        this.experience += xp;
    }
    
    public int getLevel() {
        return level;
    }
    
    public void setLevel(int level) {
        this.level = level;
    }
    
    public int getExperience() {
        return experience;
    }
    
    public void setExperience(int experience) {
        this.experience = experience;
    }
}
