package com.boopugstudios.dynamicjobseconomy.business;

import java.util.UUID;

/**
 * Represents a business contract - large projects that require multiple employees
 * and generate significant revenue over time
 */
public class BusinessContract {
    
    private final int contractId;
    private final int businessId;
    private final String title;
    private final String description;
    private final ContractType type;
    private final double totalValue;
    private final double completionBonus;
    private final int requiredEmployees;
    private final long startTime;
    private final long deadline;
    private final ContractStatus status;
    private final UUID createdBy;
    private final String requirements; // JSON string of requirements
    private final String clientName;
    private final double progressPercentage;
    
    public enum ContractType {
        CONSTRUCTION("Construction Project", "Building and infrastructure work", 1.5),
        CONSULTING("Consulting Project", "Advisory and strategic services", 2.0),
        MANUFACTURING("Manufacturing Order", "Large-scale production contract", 1.3),
        SERVICE_DELIVERY("Service Delivery", "Ongoing service provision", 1.2),
        RESEARCH_DEVELOPMENT("R&D Project", "Research and development work", 2.2),
        EVENT_MANAGEMENT("Event Management", "Organize and manage events", 1.4),
        TRAINING_PROGRAM("Training Program", "Educational service delivery", 1.6),
        MAINTENANCE("Maintenance Contract", "Ongoing maintenance services", 1.1),
        CUSTOM("Custom Project", "Specialized custom work", 1.8);
        
        private final String displayName;
        private final String description;
        private final double difficultyMultiplier;
        
        ContractType(String displayName, String description, double difficultyMultiplier) {
            this.displayName = displayName;
            this.description = description;
            this.difficultyMultiplier = difficultyMultiplier;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
        public double getDifficultyMultiplier() { return difficultyMultiplier; }
    }
    
    public enum ContractStatus {
        AVAILABLE("Available", "Contract is available for bidding"),
        AWARDED("Awarded", "Contract has been awarded to business"),
        IN_PROGRESS("In Progress", "Contract work is ongoing"),
        COMPLETED("Completed", "Contract successfully completed"),
        FAILED("Failed", "Contract failed or cancelled"),
        EXPIRED("Expired", "Contract deadline passed");
        
        private final String displayName;
        private final String description;
        
        ContractStatus(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }
    
    public BusinessContract(int contractId, int businessId, String title, String description,
                           ContractType type, double totalValue, double completionBonus,
                           int requiredEmployees, long startTime, long deadline,
                           ContractStatus status, UUID createdBy, String requirements,
                           String clientName, double progressPercentage) {
        this.contractId = contractId;
        this.businessId = businessId;
        this.title = title;
        this.description = description;
        this.type = type;
        this.totalValue = totalValue;
        this.completionBonus = completionBonus;
        this.requiredEmployees = requiredEmployees;
        this.startTime = startTime;
        this.deadline = deadline;
        this.status = status;
        this.createdBy = createdBy;
        this.requirements = requirements;
        this.clientName = clientName;
        this.progressPercentage = progressPercentage;
    }
    
    // Constructor for new contracts
    public BusinessContract(String title, String description, ContractType type,
                           double totalValue, double completionBonus, int requiredEmployees,
                           long deadline, String requirements, String clientName) {
        this(0, 0, title, description, type, totalValue, completionBonus,
             requiredEmployees, System.currentTimeMillis(), deadline,
             ContractStatus.AVAILABLE, null, requirements, clientName, 0.0);
    }
    
    // Getters
    public int getContractId() { return contractId; }
    public int getBusinessId() { return businessId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public ContractType getType() { return type; }
    public double getTotalValue() { return totalValue; }
    public double getCompletionBonus() { return completionBonus; }
    public int getRequiredEmployees() { return requiredEmployees; }
    public long getStartTime() { return startTime; }
    public long getDeadline() { return deadline; }
    public ContractStatus getStatus() { return status; }
    public UUID getCreatedBy() { return createdBy; }
    public String getRequirements() { return requirements; }
    public String getClientName() { return clientName; }
    public double getProgressPercentage() { return progressPercentage; }
    
    // Utility methods
    public boolean isExpired() {
        return System.currentTimeMillis() > deadline;
    }
    
    public boolean isActive() {
        return status == ContractStatus.IN_PROGRESS && !isExpired();
    }
    
    public double getEstimatedPayment() {
        double basePayment = totalValue * (progressPercentage / 100.0);
        if (progressPercentage >= 100.0) {
            basePayment += completionBonus;
        }
        return basePayment;
    }
    
    public long getTimeRemaining() {
        return Math.max(0, deadline - System.currentTimeMillis());
    }
    
    public int getDaysRemaining() {
        return (int) (getTimeRemaining() / (24 * 60 * 60 * 1000));
    }
    
    @Override
    public String toString() {
        return String.format("BusinessContract{id=%d, title='%s', type=%s, value=%.2f, status=%s, progress=%.1f%%}",
                contractId, title, type, totalValue, status, progressPercentage);
    }
}
