package com.boopugstudios.dynamicjobseconomy.admin;

import com.boopugstudios.dynamicjobseconomy.DynamicJobsEconomy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Listens for admin chat input when a confirmation flow is awaiting a reason.
 * Captures the message as the reason, cancels chat broadcast, and triggers /djeconomy confirm.
 */
public class AdminReasonChatListener implements Listener {

    private final DynamicJobsEconomy plugin;

    public AdminReasonChatListener(DynamicJobsEconomy plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onAsyncChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        AdminConfirmationManager mgr = plugin.getAdminConfirmationManager();
        if (mgr == null) return;

        if (!mgr.isAwaitingReason(uuid)) return;

        AdminConfirmationManager.PendingAdminAction pending = mgr.getPending(uuid);
        if (pending == null) {
            // No longer pending; stop awaiting and ignore
            mgr.setAwaitingReason(uuid, false);
            return;
        }

        // If expired, clear and inform the player
        if (pending.isExpired(System.currentTimeMillis(), mgr.getExpiryMillis())) {
            mgr.remove(uuid);
            mgr.setAwaitingReason(uuid, false);
            Map<String, String> ph = new HashMap<>();
            ph.put("seconds", String.valueOf(mgr.getExpirySeconds()));
            player.sendMessage(getPrefix() + msg("admin.reason.expired", ph, "§cConfirmation expired. Please retry the command."));
            event.setCancelled(true);
            return;
        }

        String reason = event.getMessage();
        if (reason != null) {
            reason = reason.trim();
        }
        // Store reason and stop awaiting
        mgr.setReason(uuid, reason);
        mgr.setAwaitingReason(uuid, false);

        // Cancel chat broadcast
        event.setCancelled(true);

        // Acknowledge capture and execute confirm on the main thread
        player.sendMessage(getPrefix() + msg("admin.reason.captured", null, "§aReason captured. Executing confirmation..."));
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                player.performCommand("djeconomy confirm");
            } catch (Throwable ignored) {
                // As a fallback, dispatch via console
                Bukkit.dispatchCommand(player, "djeconomy confirm");
            }
        });
    }

    private String getPrefix() {
        try {
            if (plugin.getMessages() != null) {
                String p = plugin.getMessages().getPrefix();
                if (p != null && !p.isEmpty()) return p;
            }
            if (plugin.getConfig() != null) {
                String fromCfg = plugin.getConfig().getString("messages.prefix", "");
                if (fromCfg != null && !fromCfg.isEmpty()) return fromCfg;
            }
        } catch (Throwable ignored) {}
        return "§8[§6DynamicJobs§8] ";
    }

    private String msg(String path, Map<String, String> placeholders, String def) {
        try {
            if (plugin.getMessages() != null) {
                return plugin.getMessages().get(path, placeholders, def);
            }
        } catch (Throwable ignored) {}
        String out = def;
        if (placeholders != null && out != null) {
            for (Map.Entry<String, String> e : placeholders.entrySet()) {
                out = out.replace("%" + e.getKey() + "%", e.getValue());
            }
        }
        return out;
    }
}
