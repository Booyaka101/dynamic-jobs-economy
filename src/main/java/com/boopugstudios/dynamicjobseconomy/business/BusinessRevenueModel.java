package com.boopugstudios.dynamicjobseconomy.business;

/**
 * Enum defining different business revenue models
 * Each business can have multiple revenue streams active simultaneously
 */
public enum BusinessRevenueModel {
    
    // Service-Based Revenue
    SERVICE_PROVIDER("Service Provider", "Earn from employee job completions and services", 0.15, 1.0),
    CONSULTING("Consulting", "High-value advisory services with premium rates", 0.25, 0.7),
    FREELANCE_AGENCY("Freelance Agency", "Coordinate freelance work with commission", 0.20, 0.9),
    
    // Product Sales Revenue
    RETAIL_SHOP("Retail Shop", "Sell items through integrated shop systems", 0.10, 1.2),
    MANUFACTURING("Manufacturing", "Produce and sell crafted items", 0.12, 1.1),
    TRADING_COMPANY("Trading Company", "Buy low, sell high with market analysis", 0.18, 0.8),
    
    // Contract & Gig Revenue
    PROJECT_CONTRACTOR("Project Contractor", "Complete large multi-employee projects", 0.30, 0.6),
    GIG_COORDINATOR("Gig Coordinator", "Organize and manage gig completions", 0.22, 0.8),
    CONSTRUCTION_FIRM("Construction Firm", "Building and infrastructure projects", 0.28, 0.7),
    
    // Passive Revenue Streams
    PROPERTY_MANAGEMENT("Property Management", "Rent collection and real estate", 0.08, 1.5),
    INVESTMENT_FIRM("Investment Firm", "Market-based returns and dividends", 0.12, 1.3),
    FRANCHISE_OWNER("Franchise Owner", "Automated business operations", 0.10, 1.4),
    
    // Hybrid Models
    FULL_SERVICE("Full Service", "All revenue streams available", 0.15, 1.0),
    STARTUP("Startup", "Flexible model, can pivot between revenue types", 0.18, 0.9);
    
    private final String displayName;
    private final String description;
    private final double baseCommissionRate;
    private final double revenueMultiplier;
    
    BusinessRevenueModel(String displayName, String description, double baseCommissionRate, double revenueMultiplier) {
        this.displayName = displayName;
        this.description = description;
        this.baseCommissionRate = baseCommissionRate;
        this.revenueMultiplier = revenueMultiplier;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public double getBaseCommissionRate() {
        return baseCommissionRate;
    }
    
    public double getRevenueMultiplier() {
        return revenueMultiplier;
    }
    
    /**
     * Get revenue models by category for easier UI organization
     */
    public static BusinessRevenueModel[] getServiceModels() {
        return new BusinessRevenueModel[]{SERVICE_PROVIDER, CONSULTING, FREELANCE_AGENCY};
    }
    
    public static BusinessRevenueModel[] getProductModels() {
        return new BusinessRevenueModel[]{RETAIL_SHOP, MANUFACTURING, TRADING_COMPANY};
    }
    
    public static BusinessRevenueModel[] getContractModels() {
        return new BusinessRevenueModel[]{PROJECT_CONTRACTOR, GIG_COORDINATOR, CONSTRUCTION_FIRM};
    }
    
    public static BusinessRevenueModel[] getPassiveModels() {
        return new BusinessRevenueModel[]{PROPERTY_MANAGEMENT, INVESTMENT_FIRM, FRANCHISE_OWNER};
    }
    
    public static BusinessRevenueModel[] getHybridModels() {
        return new BusinessRevenueModel[]{FULL_SERVICE, STARTUP};
    }
}
