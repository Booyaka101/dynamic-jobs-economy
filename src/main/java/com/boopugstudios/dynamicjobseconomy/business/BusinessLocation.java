package com.boopugstudios.dynamicjobseconomy.business;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import java.util.UUID;

/**
 * Represents a physical business location in the Minecraft world
 * Integrates with WorldGuard regions for protected business areas
 */
public class BusinessLocation {
    
    private final int locationId;
    private final int businessId;
    private final String locationName;
    private final String worldName;
    private final double x, y, z;
    private final String regionName; // WorldGuard region name
    private final BusinessLocationType type;
    private final boolean isActive;
    private final UUID createdBy;
    private final long createdAt;
    private final double rentCost; // Monthly rent if rented
    private final String description;
    
    public enum BusinessLocationType {
        HEADQUARTERS("Headquarters", "Main business office and operations center"),
        BRANCH_OFFICE("Branch Office", "Secondary business location"),
        WAREHOUSE("Warehouse", "Storage and inventory management facility"),
        SHOP_FRONT("Shop Front", "Customer-facing retail location"),
        FACTORY("Factory", "Production and manufacturing facility"),
        FARM("Farm", "Agricultural operations center"),
        MINE("Mine", "Resource extraction facility"),
        CONSTRUCTION_SITE("Construction Site", "Temporary project location"),
        SERVICE_CENTER("Service Center", "Customer service and support location"),
        RESEARCH_LAB("Research Lab", "Innovation and development facility");
        
        private final String displayName;
        private final String description;
        
        BusinessLocationType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }
    
    public BusinessLocation(int locationId, int businessId, String locationName, String worldName,
                           double x, double y, double z, String regionName, BusinessLocationType type,
                           boolean isActive, UUID createdBy, long createdAt, double rentCost, String description) {
        this.locationId = locationId;
        this.businessId = businessId;
        this.locationName = locationName;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.regionName = regionName;
        this.type = type;
        this.isActive = isActive;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.rentCost = rentCost;
        this.description = description;
    }
    
    // Constructor for new locations
    public BusinessLocation(int businessId, String locationName, Location location, String regionName,
                           BusinessLocationType type, UUID createdBy, double rentCost, String description) {
        this(0, businessId, locationName, location.getWorld().getName(),
             location.getX(), location.getY(), location.getZ(), regionName, type,
             true, createdBy, System.currentTimeMillis(), rentCost, description);
    }
    
    // Getters
    public int getLocationId() { return locationId; }
    public int getBusinessId() { return businessId; }
    public String getLocationName() { return locationName; }
    public String getWorldName() { return worldName; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public String getRegionName() { return regionName; }
    public BusinessLocationType getType() { return type; }
    public boolean isActive() { return isActive; }
    public UUID getCreatedBy() { return createdBy; }
    public long getCreatedAt() { return createdAt; }
    public double getRentCost() { return rentCost; }
    public String getDescription() { return description; }
    
    /**
     * Get the Bukkit Location object
     */
    public Location getBukkitLocation(World world) {
        return new Location(world, x, y, z);
    }
    
    /**
     * Check if a block is within this business location's region
     */
    public boolean containsBlock(Block block) {
        // This would integrate with WorldGuard to check if block is in region
        // For now, simple distance check
        Location blockLoc = block.getLocation();
        if (!blockLoc.getWorld().getName().equals(worldName)) return false;
        
        double distance = blockLoc.distance(new Location(blockLoc.getWorld(), x, y, z));
        return distance <= 50; // 50 block radius for now
    }
    
    /**
     * Calculate monthly operational costs for this location
     */
    public double getMonthlyOperationalCost() {
        double baseCost = rentCost;
        
        // Different location types have different operational costs
        switch (type) {
            case HEADQUARTERS:
                baseCost *= 1.5; // Higher costs for main office
                break;
            case FACTORY:
            case WAREHOUSE:
                baseCost *= 1.3; // Industrial locations cost more
                break;
            case SHOP_FRONT:
                baseCost *= 1.2; // Retail locations have moderate costs
                break;
            case FARM:
            case MINE:
                baseCost *= 1.1; // Resource locations have slight premium
                break;
            default:
                baseCost *= 1.0; // Standard cost
        }
        
        return baseCost;
    }
    
    /**
     * Get location efficiency bonus based on type and setup
     */
    public double getEfficiencyBonus() {
        switch (type) {
            case HEADQUARTERS:
                return 1.2; // 20% efficiency bonus
            case FACTORY:
                return 1.15; // 15% production bonus
            case WAREHOUSE:
                return 1.1; // 10% storage efficiency
            case SHOP_FRONT:
                return 1.25; // 25% sales bonus
            case RESEARCH_LAB:
                return 1.3; // 30% innovation bonus
            default:
                return 1.0; // No bonus
        }
    }
    
    @Override
    public String toString() {
        return String.format("BusinessLocation{id=%d, name='%s', type=%s, world='%s', coords=(%.1f,%.1f,%.1f)}",
                locationId, locationName, type, worldName, x, y, z);
    }
}
