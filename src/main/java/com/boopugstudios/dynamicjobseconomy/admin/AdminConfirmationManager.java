package com.boopugstudios.dynamicjobseconomy.admin;

import com.boopugstudios.dynamicjobseconomy.DynamicJobsEconomy;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Central manager for admin confirmation actions so both commands and GUIs can access and mutate state.
 */
public class AdminConfirmationManager {

    private final DynamicJobsEconomy plugin;
    private final Map<UUID, PendingAdminAction> pending = new HashMap<>();

    public AdminConfirmationManager(DynamicJobsEconomy plugin) {
        this.plugin = plugin;
    }

    public synchronized void putPending(UUID adminId, String action, String playerName, double amount) {
        pending.put(adminId, new PendingAdminAction(action, playerName, amount, now()));
    }

    public synchronized PendingAdminAction getPending(UUID adminId) {
        return pending.get(adminId);
    }

    public synchronized void remove(UUID adminId) {
        pending.remove(adminId);
    }

    public synchronized Map<UUID, PendingAdminAction> viewAll() {
        return Collections.unmodifiableMap(new HashMap<>(pending));
    }

    public synchronized void purgeExpired() {
        long now = now();
        long expiryMillis = getExpiryMillis();
        pending.entrySet().removeIf(e -> e.getValue().isExpired(now, expiryMillis));
    }

    /**
     * Set or update the reason for the admin's current pending action.
     */
    public synchronized void setReason(UUID adminId, String reason) {
        PendingAdminAction p = pending.get(adminId);
        if (p != null) {
            p.reason = reason;
        }
    }

    /**
     * Get the stored reason for the admin's current pending action (may be null).
     */
    public synchronized String getReason(UUID adminId) {
        PendingAdminAction p = pending.get(adminId);
        return p != null ? p.reason : null;
    }

    /**
     * Mark whether this admin is currently being prompted in chat for a reason.
     */
    public synchronized void setAwaitingReason(UUID adminId, boolean awaiting) {
        PendingAdminAction p = pending.get(adminId);
        if (p != null) {
            p.awaitingReason = awaiting;
        }
    }

    /**
     * Whether the admin is currently expected to provide a reason in chat.
     */
    public synchronized boolean isAwaitingReason(UUID adminId) {
        PendingAdminAction p = pending.get(adminId);
        return p != null && p.awaitingReason;
    }

    public long getExpiryMillis() {
        return getExpirySeconds() * 1000L;
    }

    public int getExpirySeconds() {
        try {
            int v = plugin.getConfig().getInt("economy.admin_confirmation.expiry_seconds", 30);
            return v <= 0 ? 30 : v;
        } catch (Throwable t) {
            return 30;
        }
    }

    protected long now() { return System.currentTimeMillis(); }

    public static class PendingAdminAction {
        public final String action;
        public final String playerName;
        public final double amount;
        public final long timestamp;
        // Optional reason captured via chat after GUI approval or manual flow
        public String reason;
        // Flag to indicate the admin should type a reason in chat
        public boolean awaitingReason;

        public PendingAdminAction(String action, String playerName, double amount, long timestamp) {
            this.action = action;
            this.playerName = playerName;
            this.amount = amount;
            this.timestamp = timestamp;
            this.reason = null;
            this.awaitingReason = false;
        }

        public boolean isExpired(long now, long expiryMillis) {
            return now - timestamp > expiryMillis;
        }
    }
}
