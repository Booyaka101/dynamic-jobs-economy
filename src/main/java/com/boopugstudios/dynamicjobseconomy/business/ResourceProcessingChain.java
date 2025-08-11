package com.boopugstudios.dynamicjobseconomy.business;

import org.bukkit.Material;
import java.util.*;

/**
 * Represents a resource processing chain for businesses
 * Transforms raw materials into processed products for profit
 */
public class ResourceProcessingChain {
    
    private final int chainId;
    private final int businessId;
    private final String chainName;
    private final ProcessingType type;
    private final Map<Material, Integer> inputMaterials; // Material -> Quantity needed
    private final Map<Material, Integer> outputMaterials; // Material -> Quantity produced
    private final double processingCost; // Cost per processing cycle
    private final long processingTime; // Time in milliseconds
    private final int requiredEmployees; // Minimum employees needed
    private final double profitMargin; // Expected profit percentage
    private final boolean isActive;
    private final long createdAt;
    
    public enum ProcessingType {
        MINING_REFINERY("Mining Refinery", "Process raw ores into refined materials"),
        FOOD_PROCESSING("Food Processing", "Transform raw food into cooked/processed items"),
        LUMBER_MILL("Lumber Mill", "Convert logs into planks and wooden products"),
        TEXTILE_FACTORY("Textile Factory", "Process wool and materials into dyed items"),
        SMELTING_OPERATION("Smelting Operation", "Smelt ores and raw materials"),
        BREWING_FACILITY("Brewing Facility", "Create potions and consumables"),
        ENCHANTING_SERVICE("Enchanting Service", "Add enchantments to items"),
        TOOL_MANUFACTURING("Tool Manufacturing", "Craft tools and equipment"),
        ARMOR_SMITHING("Armor Smithing", "Create and repair armor sets"),
        CONSTRUCTION_MATERIALS("Construction Materials", "Produce building supplies"),
        AGRICULTURAL_PROCESSING("Agricultural Processing", "Process farm products"),
        REDSTONE_ENGINEERING("Redstone Engineering", "Create complex redstone devices");
        
        private final String displayName;
        private final String description;
        
        ProcessingType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }
    
    public ResourceProcessingChain(int chainId, int businessId, String chainName, ProcessingType type,
                                  Map<Material, Integer> inputMaterials, Map<Material, Integer> outputMaterials,
                                  double processingCost, long processingTime, int requiredEmployees,
                                  double profitMargin, boolean isActive, long createdAt) {
        this.chainId = chainId;
        this.businessId = businessId;
        this.chainName = chainName;
        this.type = type;
        this.inputMaterials = new HashMap<>(inputMaterials);
        this.outputMaterials = new HashMap<>(outputMaterials);
        this.processingCost = processingCost;
        this.processingTime = processingTime;
        this.requiredEmployees = requiredEmployees;
        this.profitMargin = profitMargin;
        this.isActive = isActive;
        this.createdAt = createdAt;
    }
    
    // Getters
    public int getChainId() { return chainId; }
    public int getBusinessId() { return businessId; }
    public String getChainName() { return chainName; }
    public ProcessingType getType() { return type; }
    public Map<Material, Integer> getInputMaterials() { return new HashMap<>(inputMaterials); }
    public Map<Material, Integer> getOutputMaterials() { return new HashMap<>(outputMaterials); }
    public double getProcessingCost() { return processingCost; }
    public long getProcessingTime() { return processingTime; }
    public int getRequiredEmployees() { return requiredEmployees; }
    public double getProfitMargin() { return profitMargin; }
    public boolean isActive() { return isActive; }
    public long getCreatedAt() { return createdAt; }
    
    /**
     * Calculate potential profit from processing
     */
    public double calculatePotentialProfit() {
        double inputCost = calculateInputCost();
        double outputValue = calculateOutputValue();
        return (outputValue - inputCost - processingCost) * (profitMargin / 100.0);
    }
    
    /**
     * Calculate cost of input materials
     */
    private double calculateInputCost() {
        double totalCost = 0.0;
        for (Map.Entry<Material, Integer> entry : inputMaterials.entrySet()) {
            // Base material values - could be integrated with server economy
            double materialValue = getBaseMaterialValue(entry.getKey());
            totalCost += materialValue * entry.getValue();
        }
        return totalCost;
    }
    
    /**
     * Calculate value of output materials
     */
    private double calculateOutputValue() {
        double totalValue = 0.0;
        for (Map.Entry<Material, Integer> entry : outputMaterials.entrySet()) {
            double materialValue = getBaseMaterialValue(entry.getKey());
            totalValue += materialValue * entry.getValue();
        }
        return totalValue;
    }
    
    /**
     * Get base material value (could be integrated with server economy)
     */
    private double getBaseMaterialValue(Material material) {
        switch (material) {
            // Raw materials
            case COBBLESTONE: return 0.1;
            case STONE: return 0.2;
            case COAL: return 1.0;
            case IRON_ORE: return 2.0;
            case GOLD_ORE: return 5.0;
            case DIAMOND: return 20.0;
            case EMERALD: return 25.0;
            
            // Processed materials
            case IRON_INGOT: return 3.0;
            case GOLD_INGOT: return 7.0;
            case COAL_BLOCK: return 9.0;
            case IRON_BLOCK: return 27.0;
            case GOLD_BLOCK: return 63.0;
            case DIAMOND_BLOCK: return 180.0;
            
            // Food items
            case WHEAT: return 0.5;
            case BREAD: return 2.0;
            case BEEF: return 1.5;
            case COOKED_BEEF: return 3.0;
            case PORKCHOP: return 1.5;
            case COOKED_PORKCHOP: return 3.0;
            
            // Wood products
            case OAK_LOG: return 1.0;
            case OAK_PLANKS: return 0.3;
            case STICK: return 0.1;
            
            default: return 1.0; // Default value
        }
    }
    
    /**
     * Check if business has required materials for processing
     */
    public boolean canProcess(Map<Material, Integer> availableInventory) {
        for (Map.Entry<Material, Integer> required : inputMaterials.entrySet()) {
            int available = availableInventory.getOrDefault(required.getKey(), 0);
            if (available < required.getValue()) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Get processing efficiency based on employee count
     */
    public double getProcessingEfficiency(int employeeCount) {
        if (employeeCount < requiredEmployees) {
            return 0.5; // 50% efficiency if understaffed
        } else if (employeeCount == requiredEmployees) {
            return 1.0; // 100% efficiency with exact staff
        } else {
            // Bonus efficiency for overstaffing, but diminishing returns
            double bonus = Math.min(0.5, (employeeCount - requiredEmployees) * 0.1);
            return 1.0 + bonus;
        }
    }
    
    /**
     * Create common processing chains for different business types
     */
    public static List<ResourceProcessingChain> getCommonProcessingChains() {
        List<ResourceProcessingChain> chains = new ArrayList<>();
        
        // Iron Processing Chain
        Map<Material, Integer> ironInput = Map.of(Material.IRON_ORE, 9);
        Map<Material, Integer> ironOutput = Map.of(Material.IRON_BLOCK, 1);
        chains.add(new ResourceProcessingChain(0, 0, "Iron Block Production", ProcessingType.SMELTING_OPERATION,
                ironInput, ironOutput, 5.0, 60000, 2, 15.0, true, System.currentTimeMillis()));
        
        // Food Processing Chain
        Map<Material, Integer> foodInput = Map.of(Material.WHEAT, 3);
        Map<Material, Integer> foodOutput = Map.of(Material.BREAD, 1);
        chains.add(new ResourceProcessingChain(0, 0, "Bread Production", ProcessingType.FOOD_PROCESSING,
                foodInput, foodOutput, 1.0, 30000, 1, 25.0, true, System.currentTimeMillis()));
        
        // Lumber Processing Chain
        Map<Material, Integer> lumberInput = Map.of(Material.OAK_LOG, 1);
        Map<Material, Integer> lumberOutput = Map.of(Material.OAK_PLANKS, 4);
        chains.add(new ResourceProcessingChain(0, 0, "Lumber Processing", ProcessingType.LUMBER_MILL,
                lumberInput, lumberOutput, 0.5, 15000, 1, 20.0, true, System.currentTimeMillis()));
        
        return chains;
    }
    
    /**
     * Additional methods needed by BusinessGUI
     */
    public ProcessingType getProcessingType() {
        return type;
    }
    
    public String getInputMaterial() {
        return inputMaterials.keySet().stream()
            .findFirst()
            .map(Material::name)
            .orElse("None");
    }
    
    public String getOutputMaterial() {
        return outputMaterials.keySet().stream()
            .findFirst()
            .map(Material::name)
            .orElse("None");
    }
    
    public long getProcessingTimeMinutes() {
        return processingTime / (1000 * 60); // Convert milliseconds to minutes
    }
    
    public double getProfitPerCycle() {
        return calculatePotentialProfit();
    }
    
    public double getHourlyProfit() {
        if (processingTime <= 0) return 0.0;
        double cyclesPerHour = 3600000.0 / processingTime; // 1 hour in milliseconds / processing time
        return getProfitPerCycle() * cyclesPerHour;
    }
    
    @Override
    public String toString() {
        return String.format("ProcessingChain{id=%d, name='%s', type=%s, profit=%.2f}",
                chainId, chainName, type, calculatePotentialProfit());
    }
}
