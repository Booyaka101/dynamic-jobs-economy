package com.boopugstudios.dynamicjobseconomy.business;

import java.util.UUID;

/**
 * Represents a pending hiring request that requires player consent
 */
public class HiringRequest {
    
    private int requestId;
    private int businessId;
    private int positionId;
    private UUID playerUUID;
    private UUID requestedBy; // Business owner who made the request
    private double offeredSalary;
    private String message; // Optional message from employer
    private long requestTime;
    private long expirationTime;
    private HiringRequestStatus status;
    
    public enum HiringRequestStatus {
        PENDING,
        ACCEPTED,
        REJECTED,
        EXPIRED,
        CANCELLED
    }
    
    public HiringRequest(int requestId, int businessId, int positionId, UUID playerUUID, 
                        UUID requestedBy, double offeredSalary, String message) {
        this.requestId = requestId;
        this.businessId = businessId;
        this.positionId = positionId;
        this.playerUUID = playerUUID;
        this.requestedBy = requestedBy;
        this.offeredSalary = offeredSalary;
        this.message = message;
        this.requestTime = System.currentTimeMillis();
        this.expirationTime = requestTime + (24 * 60 * 60 * 1000); // 24 hours
        this.status = HiringRequestStatus.PENDING;
    }
    
    // Full constructor for database loading
    public HiringRequest(int requestId, int businessId, int positionId, UUID playerUUID, 
                        UUID requestedBy, double offeredSalary, String message, 
                        long requestTime, long expirationTime, HiringRequestStatus status) {
        this.requestId = requestId;
        this.businessId = businessId;
        this.positionId = positionId;
        this.playerUUID = playerUUID;
        this.requestedBy = requestedBy;
        this.offeredSalary = offeredSalary;
        this.message = message;
        this.requestTime = requestTime;
        this.expirationTime = expirationTime;
        this.status = status;
    }
    
    // Getters and setters
    public int getRequestId() { return requestId; }
    public void setRequestId(int requestId) { this.requestId = requestId; }
    
    public int getBusinessId() { return businessId; }
    public void setBusinessId(int businessId) { this.businessId = businessId; }
    
    public int getPositionId() { return positionId; }
    public void setPositionId(int positionId) { this.positionId = positionId; }
    
    public UUID getPlayerUUID() { return playerUUID; }
    public void setPlayerUUID(UUID playerUUID) { this.playerUUID = playerUUID; }
    
    public UUID getRequestedBy() { return requestedBy; }
    public void setRequestedBy(UUID requestedBy) { this.requestedBy = requestedBy; }
    
    public double getOfferedSalary() { return offeredSalary; }
    public void setOfferedSalary(double offeredSalary) { this.offeredSalary = offeredSalary; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public long getRequestTime() { return requestTime; }
    public void setRequestTime(long requestTime) { this.requestTime = requestTime; }
    
    public long getExpirationTime() { return expirationTime; }
    public void setExpirationTime(long expirationTime) { this.expirationTime = expirationTime; }
    
    public HiringRequestStatus getStatus() { return status; }
    public void setStatus(HiringRequestStatus status) { this.status = status; }
    
    /**
     * Checks if this hiring request has expired
     */
    public boolean isExpired() {
        return System.currentTimeMillis() > expirationTime && status == HiringRequestStatus.PENDING;
    }
    
    /**
     * Gets time remaining until expiration in hours
     */
    public long getHoursUntilExpiration() {
        if (isExpired()) return 0;
        return Math.max(0, (expirationTime - System.currentTimeMillis()) / (60 * 60 * 1000));
    }
    
    /**
     * Gets a formatted time string for when this request was made
     */
    public String getFormattedRequestTime() {
        long diff = System.currentTimeMillis() - requestTime;
        long hours = diff / (60 * 60 * 1000);
        long minutes = (diff % (60 * 60 * 1000)) / (60 * 1000);
        
        if (hours > 0) {
            return hours + "h " + minutes + "m ago";
        } else {
            return minutes + "m ago";
        }
    }
    
    @Override
    public String toString() {
        return "HiringRequest{" +
                "requestId=" + requestId +
                ", businessId=" + businessId +
                ", positionId=" + positionId +
                ", playerUUID=" + playerUUID +
                ", requestedBy=" + requestedBy +
                ", offeredSalary=" + offeredSalary +
                ", message='" + message + '\'' +
                ", status=" + status +
                '}';
    }
}
