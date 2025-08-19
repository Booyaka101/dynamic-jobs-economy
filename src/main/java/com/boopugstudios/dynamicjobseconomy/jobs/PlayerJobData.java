package com.boopugstudios.dynamicjobseconomy.jobs;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PlayerJobData {
    
    private final UUID playerUUID;
    private final Map<String, JobLevel> jobLevels;
    private boolean loaded;
    
    public PlayerJobData(UUID playerUUID) {
        this.playerUUID = playerUUID;
        this.jobLevels = new HashMap<>();
        this.loaded = false;
    }
    
    public void addJob(String jobName) {
        if (!jobLevels.containsKey(jobName)) {
            jobLevels.put(jobName, new JobLevel());
        }
    }
    
    public void removeJob(String jobName) {
        jobLevels.remove(jobName);
    }
    
    public boolean hasJob(String jobName) {
        return jobLevels.containsKey(jobName);
    }
    
    public JobLevel getJobLevel(String jobName) {
        return jobLevels.computeIfAbsent(jobName, k -> new JobLevel());
    }
    
    public Set<String> getJobs() {
        return jobLevels.keySet();
    }
    
    public UUID getPlayerUUID() {
        return playerUUID;
    }
    
    public Map<String, JobLevel> getAllJobLevels() {
        return new HashMap<>(jobLevels);
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }
}
