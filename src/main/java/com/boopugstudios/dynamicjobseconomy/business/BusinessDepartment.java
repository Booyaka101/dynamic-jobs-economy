package com.boopugstudios.dynamicjobseconomy.business;

import java.util.UUID;

/**
 * Represents a business department for organizing positions
 */
public class BusinessDepartment {
    
    private int departmentId;
    private int businessId;
    private String name;
    private String description;
    private UUID managerId; // Player who manages this department
    private double budget; // Department budget for salaries
    private boolean isActive;
    private long createdAt;
    
    public BusinessDepartment(int departmentId, int businessId, String name, String description, 
                            UUID managerId, double budget) {
        this.departmentId = departmentId;
        this.businessId = businessId;
        this.name = name;
        this.description = description;
        this.managerId = managerId;
        this.budget = budget;
        this.isActive = true;
        this.createdAt = System.currentTimeMillis();
    }
    
    // Full constructor for database loading
    public BusinessDepartment(int departmentId, int businessId, String name, String description, 
                            UUID managerId, double budget, boolean isActive, long createdAt) {
        this.departmentId = departmentId;
        this.businessId = businessId;
        this.name = name;
        this.description = description;
        this.managerId = managerId;
        this.budget = budget;
        this.isActive = isActive;
        this.createdAt = createdAt;
    }
    
    // Getters and setters
    public int getDepartmentId() { return departmentId; }
    public void setDepartmentId(int departmentId) { this.departmentId = departmentId; }
    
    public int getBusinessId() { return businessId; }
    public void setBusinessId(int businessId) { this.businessId = businessId; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public UUID getManagerId() { return managerId; }
    public void setManagerId(UUID managerId) { this.managerId = managerId; }
    
    public double getBudget() { return budget; }
    public void setBudget(double budget) { this.budget = budget; }
    
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    
    @Override
    public String toString() {
        return "BusinessDepartment{" +
                "departmentId=" + departmentId +
                ", businessId=" + businessId +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", managerId=" + managerId +
                ", budget=" + budget +
                ", isActive=" + isActive +
                '}';
    }
}
