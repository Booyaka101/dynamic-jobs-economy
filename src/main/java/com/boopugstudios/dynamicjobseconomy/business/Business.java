package com.boopugstudios.dynamicjobseconomy.business;

import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import org.bukkit.Bukkit;

public class Business {
    
    private final int id;
    private final String name;
    private final UUID ownerUUID;
    private final String type;
    private double balance;
    private final List<UUID> employees;
    private BusinessRevenueModel revenueModel; // Will be set via setter
    
    public Business(int id, String name, UUID ownerUUID, String type, double balance) {
        this.id = id;
        this.name = name;
        this.ownerUUID = ownerUUID;
        this.type = type;
        this.balance = balance;
        this.employees = new ArrayList<>();
    }
    
    public int getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public UUID getOwnerUUID() {
        return ownerUUID;
    }
    
    public String getType() {
        return type;
    }
    
    public double getBalance() {
        return balance;
    }
    
    public void setBalance(double balance) {
        this.balance = balance;
    }
    
    public List<UUID> getEmployees() {
        return employees;
    }
    
    public BusinessRevenueModel getRevenueModel() {
        return revenueModel != null ? revenueModel : BusinessRevenueModel.SERVICE_PROVIDER;
    }
    
    public void setRevenueModel(BusinessRevenueModel revenueModel) {
        this.revenueModel = revenueModel;
    }
    
    // Additional fields for Minecraft-viable business features
    private List<BusinessLocation> locations = new ArrayList<>();
    private List<ResourceProcessingChain> processingChains = new ArrayList<>();
    private List<ConstructionContract> constructionContracts = new ArrayList<>();
    private double totalRevenue = 0.0;
    private double totalExpenses = 0.0;
    private int maxEmployees = 10;
    
    /**
     * Get business locations
     */
    public List<BusinessLocation> getLocations() {
        return locations;
    }
    
    /**
     * Get resource processing chains
     */
    public List<ResourceProcessingChain> getProcessingChains() {
        return processingChains;
    }
    
    /**
     * Get construction contracts
     */
    public List<ConstructionContract> getConstructionContracts() {
        return constructionContracts;
    }
    
    /**
     * Get total revenue
     */
    public double getTotalRevenue() {
        return totalRevenue;
    }
    
    /**
     * Set total revenue
     */
    public void setTotalRevenue(double totalRevenue) {
        this.totalRevenue = totalRevenue;
    }
    
    /**
     * Get total expenses
     */
    public double getTotalExpenses() {
        return totalExpenses;
    }
    
    /**
     * Set total expenses
     */
    public void setTotalExpenses(double totalExpenses) {
        this.totalExpenses = totalExpenses;
    }
    
    /**
     * Get maximum employees
     */
    public int getMaxEmployees() {
        return maxEmployees;
    }
    
    /**
     * Set maximum employees
     */
    public void setMaxEmployees(int maxEmployees) {
        this.maxEmployees = maxEmployees;
    }
    
    /**
     * Get employees as BusinessEmployee objects (converts UUIDs to BusinessEmployee objects)
     */
    public List<BusinessEmployee> getBusinessEmployees() {
        List<BusinessEmployee> businessEmployees = new ArrayList<>();
        for (UUID employeeUUID : employees) {
            // Create a basic BusinessEmployee object
            String playerName = Bukkit.getOfflinePlayer(employeeUUID).getName();
            if (playerName == null) playerName = "Unknown Player";
            
            BusinessEmployee employee = new BusinessEmployee(
                this.id, // businessId
                1, // positionId (default position)
                employeeUUID,
                playerName,
                100.0 // Default salary
            );
            businessEmployees.add(employee);
        }
        return businessEmployees;
    }
    
    /**
     * Add a business location
     */
    public void addLocation(BusinessLocation location) {
        locations.add(location);
    }
    
    /**
     * Add a processing chain
     */
    public void addProcessingChain(ResourceProcessingChain chain) {
        processingChains.add(chain);
    }
    
    /**
     * Add a construction contract
     */
    public void addConstructionContract(ConstructionContract contract) {
        constructionContracts.add(contract);
    }
    
    // Revenue tracking methods needed by BusinessGUI
    private double dailyRevenue = 0.0;
    private double weeklyRevenue = 0.0;
    private double monthlyRevenue = 0.0;
    private double serviceRevenue = 0.0;
    private double productRevenue = 0.0;
    private double contractRevenue = 0.0;
    private double dailyPayroll = 0.0;
    private double operationalCosts = 0.0;
    private List<BusinessPosition> positions = new ArrayList<>();
    
    /**
     * Get daily revenue
     */
    public double getDailyRevenue() {
        return dailyRevenue;
    }
    
    /**
     * Get weekly revenue
     */
    public double getWeeklyRevenue() {
        return weeklyRevenue;
    }
    
    /**
     * Get monthly revenue
     */
    public double getMonthlyRevenue() {
        return monthlyRevenue;
    }
    
    /**
     * Get service revenue
     */
    public double getServiceRevenue() {
        return serviceRevenue;
    }
    
    /**
     * Get product revenue
     */
    public double getProductRevenue() {
        return productRevenue;
    }
    
    /**
     * Get contract revenue
     */
    public double getContractRevenue() {
        return contractRevenue;
    }
    
    /**
     * Get daily payroll
     */
    public double getDailyPayroll() {
        return dailyPayroll;
    }
    
    /**
     * Get operational costs
     */
    public double getOperationalCosts() {
        return operationalCosts;
    }
    
    /**
     * Get business positions
     */
    public List<BusinessPosition> getPositions() {
        return positions;
    }
    
    /**
     * Get employees in a specific position
     */
    public List<BusinessEmployee> getEmployeesInPosition(String positionName) {
        return getBusinessEmployees().stream()
            .filter(emp -> emp.getPositionName() != null && emp.getPositionName().equals(positionName))
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Set revenue values (for updating from revenue calculations)
     */
    public void setDailyRevenue(double dailyRevenue) { this.dailyRevenue = dailyRevenue; }
    public void setWeeklyRevenue(double weeklyRevenue) { this.weeklyRevenue = weeklyRevenue; }
    public void setMonthlyRevenue(double monthlyRevenue) { this.monthlyRevenue = monthlyRevenue; }
    public void setServiceRevenue(double serviceRevenue) { this.serviceRevenue = serviceRevenue; }
    public void setProductRevenue(double productRevenue) { this.productRevenue = productRevenue; }
    public void setContractRevenue(double contractRevenue) { this.contractRevenue = contractRevenue; }
    public void setDailyPayroll(double dailyPayroll) { this.dailyPayroll = dailyPayroll; }
    public void setOperationalCosts(double operationalCosts) { this.operationalCosts = operationalCosts; }
}
