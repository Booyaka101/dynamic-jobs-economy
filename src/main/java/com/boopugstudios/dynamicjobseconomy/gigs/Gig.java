package com.boopugstudios.dynamicjobseconomy.gigs;

import java.util.UUID;

public class Gig {
    
    private final int id;
    private final String title;
    private final String description;
    private final UUID posterUUID;
    private UUID workerUUID;
    private final double payment;
    private String status;
    
    public Gig(int id, String title, String description, UUID posterUUID, double payment) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.posterUUID = posterUUID;
        this.payment = payment;
        this.status = "OPEN";
    }
    
    // Getters
    public int getId() {
        return id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public UUID getPosterUUID() {
        return posterUUID;
    }
    
    public UUID getWorkerUUID() {
        return workerUUID;
    }
    
    public double getPayment() {
        return payment;
    }
    
    public String getStatus() {
        return status;
    }
    
    // Setters
    public void setWorkerUUID(UUID workerUUID) {
        this.workerUUID = workerUUID;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
}
