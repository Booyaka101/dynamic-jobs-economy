package com.boopugstudios.dynamicjobseconomy.business;

import java.util.UUID;

public class BusinessEmployee {
    
    private int employeeId;
    private int businessId;
    private int positionId;
    private UUID playerUUID;
    private String playerName;
    private double currentSalary;
    private long hiredAt;
    private boolean isActive;
    private String notes;
    
    public BusinessEmployee(int businessId, int positionId, UUID playerUUID, String playerName, double currentSalary) {
        this.businessId = businessId;
        this.positionId = positionId;
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.currentSalary = currentSalary;
        this.hiredAt = System.currentTimeMillis();
        this.isActive = true;
        this.notes = "";
    }
    
    // Full constructor for database loading
    public BusinessEmployee(int employeeId, int businessId, int positionId, UUID playerUUID, String playerName, 
                          double currentSalary, long hiredAt, boolean isActive, String notes) {
        this.employeeId = employeeId;
        this.businessId = businessId;
        this.positionId = positionId;
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.currentSalary = currentSalary;
        this.hiredAt = hiredAt;
        this.isActive = isActive;
        this.notes = notes != null ? notes : "";
    }
    
    // Getters and setters
    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }
    
    public int getBusinessId() { return businessId; }
    public void setBusinessId(int businessId) { this.businessId = businessId; }
    
    public int getPositionId() { return positionId; }
    public void setPositionId(int positionId) { this.positionId = positionId; }
    
    public UUID getPlayerUUID() { return playerUUID; }
    public void setPlayerUUID(UUID playerUUID) { this.playerUUID = playerUUID; }
    
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    
    public double getCurrentSalary() { return currentSalary; }
    public void setCurrentSalary(double currentSalary) { this.currentSalary = currentSalary; }
    
    public long getHiredAt() { return hiredAt; }
    public void setHiredAt(long hiredAt) { this.hiredAt = hiredAt; }
    
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes != null ? notes : ""; }
    
    /**
     * Gets the duration of employment in days
     */
    public long getEmploymentDurationDays() {
        return (System.currentTimeMillis() - hiredAt) / (1000 * 60 * 60 * 24);
    }
    
    /**
     * Get position name (for GUI compatibility)
     */
    public String getPosition() {
        // This would normally look up the position name from the position ID
        // For now, return a default position name
        return "Employee";
    }
    
    /**
     * Get salary (alias for getCurrentSalary for GUI compatibility)
     */
    public double getSalary() {
        return currentSalary;
    }
    
    /**
     * Get position name (for GUI compatibility)
     */
    public String getPositionName() {
        // This would normally look up the position name from the position ID
        // For now, return a default position name based on position ID
        return "Position " + positionId;
    }
    
    @Override
    public String toString() {
        return "BusinessEmployee{" +
                "employeeId=" + employeeId +
                ", businessId=" + businessId +
                ", positionId=" + positionId +
                ", playerName='" + playerName + '\'' +
                ", currentSalary=" + currentSalary +
                ", isActive=" + isActive +
                '}';
    }
}
