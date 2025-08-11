package com.boopugstudios.dynamicjobseconomy.business;

import org.bukkit.Location;
import org.bukkit.Material;
import java.util.*;

/**
 * Represents a construction contract where players hire businesses to build structures
 * Integrates with Minecraft building mechanics and WorldGuard protection
 */
public class ConstructionContract {
    
    private final int contractId;
    private final int businessId;
    private final UUID clientUUID;
    private final String projectName;
    private final ConstructionType type;
    private final Location startLocation;
    private final Location endLocation; // For area-based projects
    private final Map<Material, Integer> requiredMaterials;
    private final double contractValue;
    private final double materialsCost;
    private final long estimatedTime; // In milliseconds
    private final int requiredWorkers;
    private final ContractStatus status;
    private final long createdAt;
    private final long deadline;
    private final String blueprintData; // JSON or schematic data
    private final List<String> specialRequirements;
    private final double completionPercentage;
    private final UUID assignedForeman; // Lead worker
    
    public enum ConstructionType {
        HOUSE_BUILDING("House Building", "Residential construction projects", 1.0),
        COMMERCIAL_BUILDING("Commercial Building", "Business and office construction", 1.3),
        INFRASTRUCTURE("Infrastructure", "Roads, bridges, and public works", 1.5),
        LANDSCAPING("Landscaping", "Garden and terrain modification", 0.8),
        MINING_OPERATION("Mining Operation", "Excavation and tunnel construction", 1.2),
        FARM_SETUP("Farm Setup", "Agricultural facility construction", 0.9),
        DEFENSIVE_STRUCTURES("Defensive Structures", "Walls, towers, and fortifications", 1.4),
        REDSTONE_ENGINEERING("Redstone Engineering", "Complex redstone contraptions", 1.8),
        RENOVATION("Renovation", "Modification of existing structures", 0.7),
        DEMOLITION("Demolition", "Controlled structure removal", 0.6),
        CUSTOM_BUILD("Custom Build", "Unique architectural projects", 2.0);
        
        private final String displayName;
        private final String description;
        private final double complexityMultiplier;
        
        ConstructionType(String displayName, String description, double complexityMultiplier) {
            this.displayName = displayName;
            this.description = description;
            this.complexityMultiplier = complexityMultiplier;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
        public double getComplexityMultiplier() { return complexityMultiplier; }
    }
    
    public enum ContractStatus {
        PENDING_APPROVAL("Pending Approval", "Waiting for business to accept contract"),
        MATERIALS_GATHERING("Materials Gathering", "Collecting required materials"),
        IN_PROGRESS("In Progress", "Active construction work"),
        QUALITY_REVIEW("Quality Review", "Client reviewing completed work"),
        COMPLETED("Completed", "Project successfully finished"),
        CANCELLED("Cancelled", "Contract was cancelled"),
        FAILED("Failed", "Project could not be completed"),
        DISPUTED("Disputed", "Contract under dispute resolution");
        
        private final String displayName;
        private final String description;
        
        ContractStatus(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }
    
    public ConstructionContract(int contractId, int businessId, UUID clientUUID, String projectName,
                               ConstructionType type, Location startLocation, Location endLocation,
                               Map<Material, Integer> requiredMaterials, double contractValue, double materialsCost,
                               long estimatedTime, int requiredWorkers, ContractStatus status, long createdAt,
                               long deadline, String blueprintData, List<String> specialRequirements,
                               double completionPercentage, UUID assignedForeman) {
        this.contractId = contractId;
        this.businessId = businessId;
        this.clientUUID = clientUUID;
        this.projectName = projectName;
        this.type = type;
        this.startLocation = startLocation;
        this.endLocation = endLocation;
        this.requiredMaterials = new HashMap<>(requiredMaterials);
        this.contractValue = contractValue;
        this.materialsCost = materialsCost;
        this.estimatedTime = estimatedTime;
        this.requiredWorkers = requiredWorkers;
        this.status = status;
        this.createdAt = createdAt;
        this.deadline = deadline;
        this.blueprintData = blueprintData;
        this.specialRequirements = new ArrayList<>(specialRequirements);
        this.completionPercentage = completionPercentage;
        this.assignedForeman = assignedForeman;
    }
    
    // Constructor for new contracts
    public ConstructionContract(UUID clientUUID, String projectName, ConstructionType type,
                               Location startLocation, Map<Material, Integer> requiredMaterials,
                               List<String> specialRequirements) {
        this(0, 0, clientUUID, projectName, type, startLocation, null, requiredMaterials,
             0.0, 0.0, 0L, 0, ContractStatus.PENDING_APPROVAL, System.currentTimeMillis(),
             System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000), // 7 days deadline
             "", specialRequirements, 0.0, null);
    }
    
    // Getters
    public int getContractId() { return contractId; }
    public int getBusinessId() { return businessId; }
    public UUID getClientUUID() { return clientUUID; }
    public String getProjectName() { return projectName; }
    public ConstructionType getType() { return type; }
    public Location getStartLocation() { return startLocation; }
    public Location getEndLocation() { return endLocation; }
    public Map<Material, Integer> getRequiredMaterials() { return new HashMap<>(requiredMaterials); }
    public double getContractValue() { return contractValue; }
    public double getMaterialsCost() { return materialsCost; }
    public long getEstimatedTime() { return estimatedTime; }
    public int getRequiredWorkers() { return requiredWorkers; }
    public ContractStatus getStatus() { return status; }
    public long getCreatedAt() { return createdAt; }
    public long getDeadline() { return deadline; }
    public String getBlueprintData() { return blueprintData; }
    public List<String> getSpecialRequirements() { return new ArrayList<>(specialRequirements); }
    public double getCompletionPercentage() { return completionPercentage; }
    public UUID getAssignedForeman() { return assignedForeman; }
    
    /**
     * Calculate estimated contract value based on materials and complexity
     */
    public double calculateEstimatedValue() {
        double baseMaterialsCost = calculateMaterialsCost();
        double laborCost = baseMaterialsCost * 0.5; // 50% of materials cost for labor
        double complexityBonus = baseMaterialsCost * type.getComplexityMultiplier() * 0.2;
        double businessProfit = (baseMaterialsCost + laborCost + complexityBonus) * 0.25; // 25% profit margin
        
        return baseMaterialsCost + laborCost + complexityBonus + businessProfit;
    }
    
    /**
     * Calculate total cost of required materials
     */
    private double calculateMaterialsCost() {
        double totalCost = 0.0;
        for (Map.Entry<Material, Integer> entry : requiredMaterials.entrySet()) {
            totalCost += getBaseMaterialValue(entry.getKey()) * entry.getValue();
        }
        return totalCost;
    }
    
    /**
     * Get base material value (should integrate with server economy)
     */
    private double getBaseMaterialValue(Material material) {
        switch (material) {
            // Building blocks
            case STONE: return 0.2;
            case COBBLESTONE: return 0.1;
            case STONE_BRICKS: return 0.3;
            case BRICKS: return 0.5;
            case OAK_PLANKS: return 0.3;
            case OAK_LOG: return 1.0;
            case GLASS: return 0.4;
            case IRON_BARS: return 2.0;
            case IRON_DOOR: return 6.0;
            case OAK_DOOR: return 1.5;
            
            // Roofing materials
            case OAK_STAIRS: return 0.5;
            case STONE_STAIRS: return 0.4;
            case STONE_SLAB: return 0.2;
            case OAK_SLAB: return 0.2;
            
            // Decorative
            case WHITE_WOOL: return 1.0;
            case WHITE_CARPET: return 0.7;
            case PAINTING: return 5.0;
            case ITEM_FRAME: return 3.0;
            
            // Redstone components
            case REDSTONE: return 2.0;
            case REDSTONE_TORCH: return 3.0;
            case LEVER: return 2.5;
            case STONE_BUTTON: return 1.5;
            case REDSTONE_LAMP: return 8.0;
            case PISTON: return 15.0;
            case STICKY_PISTON: return 20.0;
            
            default: return 1.0;
        }
    }
    
    /**
     * Calculate estimated completion time based on project size and complexity
     */
    public long calculateEstimatedTime(int availableWorkers) {
        int materialCount = requiredMaterials.values().stream().mapToInt(Integer::intValue).sum();
        
        // Base time: 1 minute per block/item
        long baseTime = materialCount * 60 * 1000L; // milliseconds
        
        // Apply complexity multiplier
        baseTime = (long) (baseTime * type.getComplexityMultiplier());
        
        // Worker efficiency
        if (availableWorkers > 0) {
            double workerEfficiency = Math.min(2.0, 1.0 + (availableWorkers - 1) * 0.2); // Max 2x speed
            baseTime = (long) (baseTime / workerEfficiency);
        }
        
        return baseTime;
    }
    
    /**
     * Check if contract is overdue
     */
    public boolean isOverdue() {
        return System.currentTimeMillis() > deadline && status != ContractStatus.COMPLETED;
    }
    
    /**
     * Get time remaining until deadline
     */
    public long getTimeRemaining() {
        return Math.max(0, deadline - System.currentTimeMillis());
    }
    
    /**
     * Get days remaining until deadline
     */
    public int getDaysRemaining() {
        return (int) (getTimeRemaining() / (24 * 60 * 60 * 1000));
    }
    
    /**
     * Calculate current payment based on completion percentage
     */
    public double calculateCurrentPayment() {
        if (status == ContractStatus.COMPLETED) {
            return contractValue;
        } else if (completionPercentage >= 50.0) {
            // Pay 50% at halfway point, remaining at completion
            return contractValue * 0.5;
        }
        return 0.0; // No payment until 50% complete
    }
    
    /**
     * Get project area size (if applicable)
     */
    public int getProjectArea() {
        if (endLocation == null || startLocation == null) return 1;
        
        int deltaX = Math.abs((int) endLocation.getX() - (int) startLocation.getX()) + 1;
        int deltaZ = Math.abs((int) endLocation.getZ() - (int) startLocation.getZ()) + 1;
        int deltaY = Math.abs((int) endLocation.getY() - (int) startLocation.getY()) + 1;
        
        return deltaX * deltaZ * deltaY;
    }
    
    /**
     * Get total cost (alias for getContractValue for GUI compatibility)
     */
    public double getTotalCost() {
        return contractValue;
    }
    
    /**
     * Create common construction contract templates
     */
    public static List<ConstructionContract> getCommonContractTemplates() {
        List<ConstructionContract> templates = new ArrayList<>();
        
        // Basic House Template
        Map<Material, Integer> houseMaterials = new HashMap<>();
        houseMaterials.put(Material.STONE, 200);
        houseMaterials.put(Material.OAK_PLANKS, 150);
        houseMaterials.put(Material.GLASS, 50);
        houseMaterials.put(Material.OAK_DOOR, 2);
        houseMaterials.put(Material.OAK_STAIRS, 30);
        
        templates.add(new ConstructionContract(null, "Basic House Template", ConstructionType.HOUSE_BUILDING,
                null, houseMaterials, Arrays.asList("Include windows", "Add basic furniture", "Weatherproof roof")));
        
        return templates;
    }
    
    @Override
    public String toString() {
        return String.format("ConstructionContract{id=%d, project='%s', type=%s, value=%.2f, status=%s, completion=%.1f%%}",
                contractId, projectName, type, contractValue, status, completionPercentage);
    }
}
