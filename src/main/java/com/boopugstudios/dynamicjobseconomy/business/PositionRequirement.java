package com.boopugstudios.dynamicjobseconomy.business;

/**
 * Represents requirements for a business position
 */
public class PositionRequirement {
    
    public enum RequirementType {
        JOB_LEVEL,      // Requires specific job level
        PERMISSION,     // Requires specific permission
        EXPERIENCE,     // Requires minimum experience points
        PLAYTIME,       // Requires minimum playtime hours
        CUSTOM          // Custom requirement
    }
    
    private int requirementId;
    private int positionId;
    private RequirementType type;
    private String value;       // The requirement value (job name, permission, etc.)
    private int minValue;       // Minimum value (level, hours, etc.)
    private String description; // Human-readable description
    private boolean isRequired; // Whether this is mandatory or preferred
    
    public PositionRequirement(int requirementId, int positionId, RequirementType type, 
                             String value, int minValue, String description, boolean isRequired) {
        this.requirementId = requirementId;
        this.positionId = positionId;
        this.type = type;
        this.value = value;
        this.minValue = minValue;
        this.description = description;
        this.isRequired = isRequired;
    }
    
    // Getters and setters
    public int getRequirementId() { return requirementId; }
    public void setRequirementId(int requirementId) { this.requirementId = requirementId; }
    
    public int getPositionId() { return positionId; }
    public void setPositionId(int positionId) { this.positionId = positionId; }
    
    public RequirementType getType() { return type; }
    public void setType(RequirementType type) { this.type = type; }
    
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    
    public int getMinValue() { return minValue; }
    public void setMinValue(int minValue) { this.minValue = minValue; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public boolean isRequired() { return isRequired; }
    public void setRequired(boolean required) { isRequired = required; }
    
    /**
     * Gets a human-readable string for this requirement
     */
    public String getDisplayString() {
        switch (type) {
            case JOB_LEVEL:
                return (isRequired ? "Requires" : "Prefers") + " " + value + " level " + minValue + "+";
            case PERMISSION:
                return (isRequired ? "Requires" : "Prefers") + " permission: " + value;
            case EXPERIENCE:
                return (isRequired ? "Requires" : "Prefers") + " " + minValue + "+ experience points";
            case PLAYTIME:
                return (isRequired ? "Requires" : "Prefers") + " " + minValue + "+ hours playtime";
            case CUSTOM:
                return description;
            default:
                return "Unknown requirement";
        }
    }
    
    @Override
    public String toString() {
        return "PositionRequirement{" +
                "requirementId=" + requirementId +
                ", positionId=" + positionId +
                ", type=" + type +
                ", value='" + value + '\'' +
                ", minValue=" + minValue +
                ", description='" + description + '\'' +
                ", isRequired=" + isRequired +
                '}';
    }
}
