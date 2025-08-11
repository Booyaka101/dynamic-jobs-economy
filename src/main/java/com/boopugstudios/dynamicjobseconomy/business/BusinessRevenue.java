package com.boopugstudios.dynamicjobseconomy.business;

import java.util.UUID;

/**
 * Represents a revenue transaction for a business
 * Tracks all income sources and provides detailed analytics
 */
public class BusinessRevenue {
    
    private final int revenueId;
    private final int businessId;
    private final RevenueType type;
    private final double amount;
    private final String source;
    private final UUID generatedBy; // Employee or system UUID
    private final long timestamp;
    private final String description;
    private final String metadata; // JSON for additional data
    
    public enum RevenueType {
        SERVICE_COMPLETION("Service Completion", "Revenue from completed services"),
        PRODUCT_SALE("Product Sale", "Revenue from item/product sales"),
        CONTRACT_PAYMENT("Contract Payment", "Revenue from contract completions"),
        GIG_COMMISSION("Gig Commission", "Commission from gig completions"),
        PASSIVE_INCOME("Passive Income", "Automated revenue generation"),
        PROPERTY_RENT("Property Rent", "Rent collection from properties"),
        INVESTMENT_RETURN("Investment Return", "Returns from investments"),
        SHOP_SALE("Shop Sale", "Revenue from shop transactions"),
        FRANCHISE_FEE("Franchise Fee", "Franchise operation income"),
        BONUS_PAYMENT("Bonus Payment", "Performance-based bonuses"),
        OTHER("Other", "Miscellaneous revenue");
        
        private final String displayName;
        private final String description;
        
        RevenueType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    public BusinessRevenue(int revenueId, int businessId, RevenueType type, double amount, 
                          String source, UUID generatedBy, long timestamp, String description, String metadata) {
        this.revenueId = revenueId;
        this.businessId = businessId;
        this.type = type;
        this.amount = amount;
        this.source = source;
        this.generatedBy = generatedBy;
        this.timestamp = timestamp;
        this.description = description;
        this.metadata = metadata;
    }
    
    // Constructor for new revenue (auto-generated ID)
    public BusinessRevenue(int businessId, RevenueType type, double amount, String source, 
                          UUID generatedBy, String description, String metadata) {
        this(0, businessId, type, amount, source, generatedBy, System.currentTimeMillis(), description, metadata);
    }
    
    // Getters
    public int getRevenueId() { return revenueId; }
    public int getBusinessId() { return businessId; }
    public RevenueType getType() { return type; }
    public double getAmount() { return amount; }
    public String getSource() { return source; }
    public UUID getGeneratedBy() { return generatedBy; }
    public long getTimestamp() { return timestamp; }
    public String getDescription() { return description; }
    public String getMetadata() { return metadata; }
    
    @Override
    public String toString() {
        return String.format("BusinessRevenue{id=%d, business=%d, type=%s, amount=%.2f, source='%s', timestamp=%d}",
                revenueId, businessId, type, amount, source, timestamp);
    }
}
