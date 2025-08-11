package com.boopugstudios.dynamicjobseconomy.business;

import java.util.UUID;

public class BusinessPosition {
    
    private int positionId;
    private int businessId;
    private String title;
    private double salary;
    private String description;
    private int maxEmployees;
    private boolean isActive;
    private UUID createdBy;
    private long createdAt;
    
    public BusinessPosition(int positionId, int businessId, String title, double salary, String description, int maxEmployees) {
        this.positionId = positionId;
        this.businessId = businessId;
        this.title = title;
        this.salary = salary;
        this.description = description;
        this.maxEmployees = maxEmployees;
        this.isActive = true;
        this.createdAt = System.currentTimeMillis();
    }
    
    // Full constructor for database loading
    public BusinessPosition(int positionId, int businessId, String title, double salary, String description, 
                          int maxEmployees, boolean isActive, UUID createdBy, long createdAt) {
        this.positionId = positionId;
        this.businessId = businessId;
        this.title = title;
        this.salary = salary;
        this.description = description;
        this.maxEmployees = maxEmployees;
        this.isActive = isActive;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }
    
    // Getters and setters
    public int getPositionId() { return positionId; }
    public void setPositionId(int positionId) { this.positionId = positionId; }
    
    public int getBusinessId() { return businessId; }
    public void setBusinessId(int businessId) { this.businessId = businessId; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public double getSalary() { return salary; }
    public void setSalary(double salary) { this.salary = salary; }
    
    /**
     * Get base salary (alias for getSalary for GUI compatibility)
     */
    public double getBaseSalary() { return salary; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public int getMaxEmployees() { return maxEmployees; }
    public void setMaxEmployees(int maxEmployees) { this.maxEmployees = maxEmployees; }
    
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    
    @Override
    public String toString() {
        return "BusinessPosition{" +
                "positionId=" + positionId +
                ", businessId=" + businessId +
                ", title='" + title + '\'' +
                ", salary=" + salary +
                ", description='" + description + '\'' +
                ", maxEmployees=" + maxEmployees +
                ", isActive=" + isActive +
                '}';
    }
}
