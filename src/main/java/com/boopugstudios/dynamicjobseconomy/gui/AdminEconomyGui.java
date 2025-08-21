package com.boopugstudios.dynamicjobseconomy.gui;

import com.boopugstudios.dynamicjobseconomy.DynamicJobsEconomy;
import com.boopugstudios.dynamicjobseconomy.economy.EconomyManager;
import com.boopugstudios.dynamicjobseconomy.admin.AdminConfirmationManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

/**
 * Admin Economy GUI providing simple views for moderation tasks.
 * Minimal implementation: Home, Player Selector, Player Account, Confirmations (placeholder).
 * Follows BusinessGUI event/session patterns.
 */
public class AdminEconomyGui implements Listener {

    private static final Map<DynamicJobsEconomy, AdminEconomyGui> INSTANCES = new WeakHashMap<>();

    public static synchronized AdminEconomyGui get(DynamicJobsEconomy plugin) {
        return INSTANCES.computeIfAbsent(plugin, p -> new AdminEconomyGui(p));
    }

    private final DynamicJobsEconomy plugin;
    private final EconomyManager economy;
    private final Map<UUID, Session> sessions = new HashMap<>();
    private final boolean debugClicks;
    private final boolean useFillerPanes;

    private AdminEconomyGui(DynamicJobsEconomy plugin) {
        this.plugin = plugin;
        this.economy = plugin.getEconomyManager();
        this.debugClicks = plugin.getConfig().getBoolean("debug.guiClicks", false);
        this.useFillerPanes = plugin.getConfig().getBoolean("gui.useFillerPanes", true);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public enum View {
        HOME("Admin Economy", 54),
        PLAYER_SELECTOR("Select Player", 54),
        PLAYER_ACCOUNT("Player Account", 54),
        CONFIRMATIONS("Pending Confirmations", 54);
        private final String title;
        private final int size;
        View(String title, int size) { this.title = title; this.size = size; }
        public String title() { return title; }
        public int size() { return size; }
    }

    public static class Session {
        private View view;
        private UUID targetPlayerId;
        private long last;
        public Session(View view) { this.view = view; this.last = System.currentTimeMillis(); }
        public View getView() { return view; }
        public void setView(View v) { this.view = v; this.last = System.currentTimeMillis(); }
        public UUID getTargetPlayerId() { return targetPlayerId; }
        public void setTargetPlayerId(UUID id) { this.targetPlayerId = id; this.last = System.currentTimeMillis(); }
        public boolean isExpired() { return System.currentTimeMillis() - last > 300000L; }
    }

    private static class Holder implements InventoryHolder {
        private final View view;
        public Holder(View v) { this.view = v; }
        @SuppressWarnings("unused")
        public View getView() { return view; }
        @Override public Inventory getInventory() { return null; }
    }

    // ---------- Public entry points ----------

    public void openHome(Player player) {
        if (!player.hasPermission("djeconomy.gui.admin.economy")) {
            Map<String, String> ph = new HashMap<>();
            ph.put("node", "djeconomy.gui.admin.economy");
            player.sendMessage(getPrefix() + msg("gui.admin.economy.error.no_permission", ph, "You lack permission: %node%"));
            return;
        }
        String title = msg("gui.admin.economy.title.home", null, "Admin Economy");
        Inventory inv = Bukkit.createInventory(new Holder(View.HOME), View.HOME.size(), color("&6" + title));

        // Primary actions
        inv.setItem(20, menuItem(Material.PLAYER_HEAD, color("&bPlayer Selector"), Arrays.asList(color("&7Browse online players"), color("&eClick to open"))));
        inv.setItem(24, menuItem(Material.PAPER, color("&dConfirmations"), Arrays.asList(color("&7Review pending confirmations"), color("&eClick to open"))));

        // Close
        inv.setItem(49, menuItem(Material.BARRIER, color("&cClose"), Collections.singletonList(color("&7Close this menu"))));

        fill(inv, Material.GRAY_STAINED_GLASS_PANE);
        player.openInventory(inv);
        sessions.put(player.getUniqueId(), new Session(View.HOME));
    }

    // ---------- View builders ----------

    private void openPlayerSelector(Player player) {
        String title = msg("gui.admin.economy.title.player_select", null, "Select Player");
        Inventory inv = Bukkit.createInventory(new Holder(View.PLAYER_SELECTOR), View.PLAYER_SELECTOR.size(), color("&9" + title));

        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        online.sort(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER));

        for (int i = 0; i < Math.min(45, online.size()); i++) {
            Player p = online.get(i);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta meta = head.getItemMeta();
            if (meta instanceof SkullMeta) {
                SkullMeta sm = (SkullMeta) meta;
                sm.setOwningPlayer(p);
                sm.setDisplayName(color("&a" + p.getName()));
                sm.setLore(Arrays.asList(color("&7Click to view account")));
                head.setItemMeta(sm);
            } else if (meta != null) {
                meta.setDisplayName(color("&a" + p.getName()));
                meta.setLore(Arrays.asList(color("&7Click to view account")));
                head.setItemMeta(meta);
            }
            inv.setItem(i, head);
        }

        inv.setItem(49, menuItem(Material.ARROW, color("&eBack"), Collections.singletonList(color("&7Return to home"))));
        fill(inv, Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        player.openInventory(inv);
        Session s = sessions.computeIfAbsent(player.getUniqueId(), id -> new Session(View.PLAYER_SELECTOR));
        s.setView(View.PLAYER_SELECTOR);
        s.setTargetPlayerId(null);
    }

    private void openPlayerAccount(Player admin, OfflinePlayer target) {
        String tname = target.getName() != null ? target.getName() : target.getUniqueId().toString();
        Map<String, String> ph = new HashMap<>();
        ph.put("player", tname);
        String title = msg("gui.admin.economy.title.player", ph, "Player • %player%");
        Inventory inv = Bukkit.createInventory(new Holder(View.PLAYER_ACCOUNT), View.PLAYER_ACCOUNT.size(), color("&2" + title));

        // Head + balance display
        inv.setItem(4, playerHead(target, color("&a" + (target.getName() != null ? target.getName() : target.getUniqueId().toString())), Arrays.asList(
            color("&7Balance:"),
            color("&6" + economy.formatMoney(economy.getBalance(target)))
        )));

        boolean canModify = admin.hasPermission("djeconomy.gui.admin.economy.balance.modify");

        // Placeholders for actions (minimal implementation routes admin to commands)
        inv.setItem(20, menuItem(Material.LIME_DYE, color("&aGive Money"), Arrays.asList(
            color("&7Use command:"),
            color("&e/djeconomy economy give " + safeName(target) + " <amount>")
        )));
        inv.setItem(22, menuItem(Material.RED_DYE, color("&cTake Money"), Arrays.asList(
            color("&7Use command:"),
            color("&e/djeconomy economy take " + safeName(target) + " <amount>")
        )));
        inv.setItem(24, menuItem(Material.GOLD_INGOT, color("&6Set Balance"), Arrays.asList(
            color("&7Use command:"),
            color("&e/djeconomy economy set " + safeName(target) + " <amount>")
        )));

        if (!canModify) {
            // Overlay a warning if lacks permission
            inv.setItem(31, menuItem(Material.BARRIER, color("&cNo permission to modify"), Collections.singletonList(color("&7Requires djeconomy.gui.admin.economy.balance.modify"))));
        }

        inv.setItem(45, menuItem(Material.ARROW, color("&eBack to Players"), Collections.singletonList(color("&7Return to selector"))));
        inv.setItem(49, menuItem(Material.BARRIER, color("&cClose"), Collections.singletonList(color("&7Close this menu"))));

        fill(inv, Material.GREEN_STAINED_GLASS_PANE);
        admin.openInventory(inv);
        Session s = sessions.computeIfAbsent(admin.getUniqueId(), id -> new Session(View.PLAYER_ACCOUNT));
        s.setView(View.PLAYER_ACCOUNT);
        s.setTargetPlayerId(target.getUniqueId());
    }

    private void openConfirmations(Player player) {
        String title = msg("gui.admin.economy.title.confirm_queue", null, "Pending Confirmations");
        Inventory inv = Bukkit.createInventory(new Holder(View.CONFIRMATIONS), View.CONFIRMATIONS.size(), color("&d" + title));

        // Load the player's pending confirmation from the shared manager
        AdminConfirmationManager mgr = plugin.getAdminConfirmationManager();
        AdminConfirmationManager.PendingAdminAction pending = null;
        if (mgr != null) {
            pending = mgr.getPending(player.getUniqueId());
            if (pending != null) {
                long now = System.currentTimeMillis();
                long expiryMs = mgr.getExpiryMillis();
                if (pending.isExpired(now, expiryMs)) {
                    // Auto-clean expired and refresh view
                    mgr.remove(player.getUniqueId());
                    // Audit log auto-expiry for visibility
                    try {
                        plugin.getLogger().info(String.format("[ADMIN-GUI] Auto-expired pending for %s: %s %s $%.2f",
                            player.getName(), String.valueOf(pending.action), String.valueOf(pending.playerName), pending.amount));
                    } catch (Throwable ignored) {}
                    player.sendMessage(getPrefix() + msg("admin.confirm_expired", null, "§cConfirmation expired! Please retry the command."));
                    // Rebuild as empty state
                    pending = null;
                }
            }
        }

        if (pending == null) {
            inv.setItem(22, menuItem(Material.PAPER, color("&7No pending confirmations"), Arrays.asList(
                color("&8You currently have no pending actions."),
                color("&7Use /djeconomy confirm after large actions."))));
        } else {
            // Render the pending action details and approve/cancel buttons
            String act = pending.action != null ? pending.action.toUpperCase(Locale.ROOT) : "?";
            String target = pending.playerName != null ? pending.playerName : "?";
            String amountStr = economy != null ? economy.formatMoney(pending.amount) : String.format(Locale.ROOT, "%.2f", pending.amount);
            long remainingSec = 0L;
            if (mgr != null) {
                remainingSec = Math.max(0L, mgr.getExpirySeconds() - ((System.currentTimeMillis() - pending.timestamp) / 1000L));
            }

            inv.setItem(20, menuItem(Material.PAPER, color("&dPending Action"), Arrays.asList(
                color("&7Action: &f" + act),
                color("&7Target: &f" + target),
                color("&7Amount: &6" + amountStr),
                color("&7Time left: &e" + remainingSec + "s")
            )));

            inv.setItem(30, menuItem(Material.LIME_DYE, color("&aApprove"), Arrays.asList(
                color("&7Click to confirm and execute this action"))));
            inv.setItem(32, menuItem(Material.RED_DYE, color("&cCancel"), Arrays.asList(
                color("&7Click to cancel and discard this action"))));
        }

        inv.setItem(49, menuItem(Material.ARROW, color("&eBack"), Collections.singletonList(color("&7Return to home"))));
        fill(inv, Material.PURPLE_STAINED_GLASS_PANE);
        player.openInventory(inv);
        Session s = sessions.computeIfAbsent(player.getUniqueId(), id -> new Session(View.CONFIRMATIONS));
        s.setView(View.CONFIRMATIONS);
    }

    // ---------- Event handling ----------

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        if (e.getView().getTopInventory() == null || !(e.getView().getTopInventory().getHolder() instanceof Holder)) return;
        e.setCancelled(true);
        ItemStack item = e.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        Session s = sessions.get(p.getUniqueId());
        if (s == null) return;
        if (debugClicks) {
            String name = (item.getItemMeta() != null ? ChatColor.stripColor(item.getItemMeta().getDisplayName()) : "");
            plugin.getLogger().info(String.format("[AdminGUI-DEBUG] %s view=%s slot=%d item=%s name='%s' action=%s", p.getName(), s.getView(), e.getSlot(), item.getType(), name, e.getAction()));
        }

        String name = item.getItemMeta() != null ? ChatColor.stripColor(item.getItemMeta().getDisplayName()) : "";
        switch (s.getView()) {
            case HOME:
                if ("Player Selector".equalsIgnoreCase(name)) openPlayerSelector(p);
                else if ("Confirmations".equalsIgnoreCase(name)) openConfirmations(p);
                else if ("Close".equalsIgnoreCase(name)) p.closeInventory();
                break;
            case PLAYER_SELECTOR:
                if ("Back".equalsIgnoreCase(name)) { openHome(p); break; }
                if (e.getSlot() < 45) {
                    ItemMeta meta = item.getItemMeta();
                    String targetName = meta != null ? ChatColor.stripColor(meta.getDisplayName()) : null;
                    if (targetName != null && !targetName.isEmpty()) {
                        Player target = Bukkit.getPlayerExact(ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', targetName.replace("§", ""))).replace("§", ""));
                        if (target != null) openPlayerAccount(p, target);
                    }
                }
                break;
            case PLAYER_ACCOUNT:
                if ("Back to Players".equalsIgnoreCase(name)) { openPlayerSelector(p); break; }
                if ("Close".equalsIgnoreCase(name)) { p.closeInventory(); break; }
                // Route to commands if clicked on action items
                if (name.equalsIgnoreCase("Give Money") || name.equalsIgnoreCase("Take Money") || name.equalsIgnoreCase("Set Balance")) {
                    if (s.getTargetPlayerId() != null) {
                        OfflinePlayer tgt = Bukkit.getOfflinePlayer(s.getTargetPlayerId());
                        String tname = safeName(tgt);
                        String sub = name.toLowerCase().contains("give") ? "give" : name.toLowerCase().contains("take") ? "take" : "set";
                        p.closeInventory();
                        p.sendMessage(getPrefix() + color("&eUse &f/djeconomy economy " + sub + " " + tname + " <amount> &eto perform this action."));
                    }
                }
                break;
            case CONFIRMATIONS:
                if ("Back".equalsIgnoreCase(name)) { openHome(p); break; }
                if ("Approve".equalsIgnoreCase(name)) {
                    // Switch to chat-based reason capture instead of immediate confirmation
                    AdminConfirmationManager mgr = plugin.getAdminConfirmationManager();
                    AdminConfirmationManager.PendingAdminAction pending = (mgr != null) ? mgr.getPending(p.getUniqueId()) : null;
                    try {
                        if (pending != null) {
                            plugin.getLogger().info(String.format("[ADMIN-GUI] %s approved (awaiting reason) %s %s $%.2f",
                                p.getName(), String.valueOf(pending.action), String.valueOf(pending.playerName), pending.amount));
                            if (mgr != null) {
                                mgr.setAwaitingReason(p.getUniqueId(), true);
                            }
                        } else {
                            plugin.getLogger().info(String.format("[ADMIN-GUI] %s clicked Approve with no pending action", p.getName()));
                        }
                    } catch (Throwable ignored) {}
                    p.closeInventory();
                    // Prompt for reason via chat
                    Map<String, String> ph = new HashMap<>();
                    ph.put("seconds", String.valueOf(plugin.getAdminConfirmationManager().getExpirySeconds()));
                    p.sendMessage(getPrefix() + msg("admin.reason.prompt", ph, "§ePlease type a reason in chat for this action (expires in %seconds%s)."));
                    p.sendMessage(getPrefix() + msg("admin.reason.hint", null, "§7Example: 'Refund for bug', 'Anti-cheat action', or 'Manual adjustment'."));
                    break;
                }
                if ("Cancel".equalsIgnoreCase(name)) {
                    AdminConfirmationManager mgr = plugin.getAdminConfirmationManager();
                    AdminConfirmationManager.PendingAdminAction pending = (mgr != null) ? mgr.getPending(p.getUniqueId()) : null;
                    try {
                        if (pending != null) {
                            plugin.getLogger().info(String.format("[ADMIN-GUI] %s cancelled %s %s $%.2f",
                                p.getName(), String.valueOf(pending.action), String.valueOf(pending.playerName), pending.amount));
                        } else {
                            plugin.getLogger().info(String.format("[ADMIN-GUI] %s clicked Cancel with no pending action", p.getName()));
                        }
                    } catch (Throwable ignored) {}
                    if (mgr != null) {
                        mgr.remove(p.getUniqueId());
                    }
                    p.sendMessage(getPrefix() + msg("admin.confirm_cancelled", null, "§7Pending action cancelled."));
                    openConfirmations(p);
                    break;
                }
                break;
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory() != null && e.getView().getTopInventory().getHolder() instanceof Holder) {
            if (debugClicks) {
                Player p = (Player) e.getWhoClicked();
                plugin.getLogger().info(String.format("[AdminGUI-DEBUG] Drag by %s slots=%s type=%s", p.getName(), e.getRawSlots(), e.getType()));
            }
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getPlayer() instanceof Player) {
            sessions.remove(e.getPlayer().getUniqueId());
        }
    }

    // ---------- Helpers ----------

    private ItemStack menuItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack playerHead(OfflinePlayer p, String name, List<String> lore) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = head.getItemMeta();
        if (meta instanceof SkullMeta) {
            SkullMeta sm = (SkullMeta) meta;
            sm.setOwningPlayer(p);
            sm.setDisplayName(name);
            sm.setLore(lore);
            head.setItemMeta(sm);
        } else if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            head.setItemMeta(meta);
        }
        return head;
    }

    private void fill(Inventory inv, Material mat) {
        if (!useFillerPanes) return;
        ItemStack filler = new ItemStack(mat);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) { meta.setDisplayName(" "); filler.setItemMeta(meta); }
        for (int i = 0; i < inv.getSize(); i++) if (inv.getItem(i) == null) inv.setItem(i, filler);
    }

    private String color(String s) { return ChatColor.translateAlternateColorCodes('&', s); }
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
    /**
     * Localized message helper using messages.yml with safe fallback to defaults.
     */
    private String msg(String path, Map<String, String> placeholders, String def) {
        try {
            if (plugin.getMessages() != null) {
                return plugin.getMessages().get(path, placeholders, def);
            }
        } catch (Throwable ignored) {
            // Fallback below
        }
        String out = def;
        if (placeholders != null && out != null) {
            for (Map.Entry<String, String> e : placeholders.entrySet()) {
                out = out.replace("%" + e.getKey() + "%", e.getValue());
            }
        }
        return out;
    }
    private String safeName(OfflinePlayer p) { return p.getName() != null ? p.getName() : p.getUniqueId().toString(); }
}
