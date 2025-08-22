package com.boopugstudios.dynamicjobseconomy.gui;

import com.boopugstudios.dynamicjobseconomy.DynamicJobsEconomy;
import com.boopugstudios.dynamicjobseconomy.admin.AdminConfirmationManager;
import com.boopugstudios.dynamicjobseconomy.economy.EconomyManager;
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
    // Removed chat-based amount prompts; replaced by GUI keypad input
    // private final Map<UUID, AmountPrompt> amountPrompts = new ConcurrentHashMap<>();
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
        // New views for GUI-based input
        AMOUNT_INPUT("Enter Amount", 54),
        REASON_SELECT("Select Reason", 54),
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
        // GUI input state
        private String pendingAction; // give | take | set
        private String amountInput;   // numeric text buffer
        public Session(View view) { this.view = view; this.last = System.currentTimeMillis(); }
        public View getView() { return view; }
        public void setView(View v) { this.view = v; this.last = System.currentTimeMillis(); }
        public UUID getTargetPlayerId() { return targetPlayerId; }
        public void setTargetPlayerId(UUID id) { this.targetPlayerId = id; this.last = System.currentTimeMillis(); }
        public boolean isExpired() { return System.currentTimeMillis() - last > 300000L; }
        public String getPendingAction() { return pendingAction; }
        public void setPendingAction(String action) { this.pendingAction = action; this.last = System.currentTimeMillis(); }
        public String getAmountInput() { return amountInput; }
        public void setAmountInput(String input) { this.amountInput = input; this.last = System.currentTimeMillis(); }
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
        if (player.hasPermission("djeconomy.gui.admin.economy.confirm.manage")) {
            inv.setItem(24, menuItem(Material.PAPER, color("&dConfirmations"), Arrays.asList(color("&7Review pending confirmations"), color("&eClick to open"))));
        } else {
            inv.setItem(24, menuItem(Material.BARRIER, color("&8Confirmations"), Arrays.asList(color("&7Missing permission: djeconomy.gui.admin.economy.confirm.manage"))));
        }

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
        boolean canView = player.hasPermission("djeconomy.gui.admin.economy.balance.view");

        for (int i = 0; i < Math.min(45, online.size()); i++) {
            Player p = online.get(i);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta meta = head.getItemMeta();
            if (meta instanceof SkullMeta) {
                SkullMeta sm = (SkullMeta) meta;
                sm.setOwningPlayer(p);
                sm.setDisplayName(color("&a" + p.getName()));
                List<String> lore = new ArrayList<>();
                if (canView) {
                    lore.add(color("&7Balance: &6" + economy.formatMoney(economy.getBalance(p))));
                }
                lore.add(color("&7Click to view account"));
                sm.setLore(lore);
                head.setItemMeta(sm);
            } else if (meta != null) {
                meta.setDisplayName(color("&a" + p.getName()));
                List<String> lore = new ArrayList<>();
                if (canView) {
                    lore.add(color("&7Balance: &6" + economy.formatMoney(economy.getBalance(p))));
                }
                lore.add(color("&7Click to view account"));
                meta.setLore(lore);
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
        boolean canView = admin.hasPermission("djeconomy.gui.admin.economy.balance.view");
        List<String> headLore = new ArrayList<>();
        if (canView) {
            headLore.add(color("&7Balance:"));
            headLore.add(color("&6" + economy.formatMoney(economy.getBalance(target))));
        } else {
            headLore.add(color("&7Balance: &8<hidden>"));
            headLore.add(color("&8Requires djeconomy.gui.admin.economy.balance.view"));
        }
        inv.setItem(4, playerHead(target, color("&a" + (target.getName() != null ? target.getName() : target.getUniqueId().toString())), headLore));

        boolean canModify = admin.hasPermission("djeconomy.gui.admin.economy.balance.modify");

        // Action buttons now open GUI-based amount input
        String thresholdStr = economy.formatMoney(getConfirmThreshold());
        inv.setItem(20, menuItem(Material.LIME_DYE, color("&aGive Money"), Arrays.asList(
            color("&7Click to enter an amount"),
            color("&7Large amounts may require confirmation"),
            color("&8Threshold: &e" + thresholdStr)
        )));
        inv.setItem(22, menuItem(Material.RED_DYE, color("&cTake Money"), Arrays.asList(
            color("&7Click to enter an amount"),
            color("&7Large amounts may require confirmation"),
            color("&8Threshold: &e" + thresholdStr)
        )));
        inv.setItem(24, menuItem(Material.GOLD_INGOT, color("&6Set Balance"), Arrays.asList(
            color("&7Click to enter an amount"),
            color("&7Large amounts may require confirmation"),
            color("&8Threshold: &e" + thresholdStr)
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
        if (!player.hasPermission("djeconomy.gui.admin.economy.confirm.manage")) {
            Map<String, String> ph = new HashMap<>();
            ph.put("node", "djeconomy.gui.admin.economy.confirm.manage");
            player.sendMessage(getPrefix() + msg("gui.admin.economy.error.no_permission", ph, "You lack permission: %node%"));
            openHome(player);
            return;
        }
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
                // Start GUI-based amount input flow
                if (name.equalsIgnoreCase("Give Money") || name.equalsIgnoreCase("Take Money") || name.equalsIgnoreCase("Set Balance")) {
                    if (!p.hasPermission("djeconomy.gui.admin.economy.balance.modify")) {
                        Map<String, String> ph = new HashMap<>();
                        ph.put("node", "djeconomy.gui.admin.economy.balance.modify");
                        p.sendMessage(getPrefix() + msg("gui.admin.economy.error.no_permission", ph, "You lack permission: %node%"));
                        break;
                    }
                    if (s.getTargetPlayerId() != null) {
                        OfflinePlayer tgt = Bukkit.getOfflinePlayer(s.getTargetPlayerId());
                        String action = name.toLowerCase().contains("give") ? "give" : name.toLowerCase().contains("take") ? "take" : "set";
                        s.setPendingAction(action);
                        s.setAmountInput("");
                        openAmountInput(p, tgt, action);
                    }
                }
                break;
            case CONFIRMATIONS:
                if ("Back".equalsIgnoreCase(name)) { openHome(p); break; }
                if ("Approve".equalsIgnoreCase(name)) {
                    if (!p.hasPermission("djeconomy.gui.admin.economy.confirm.manage")) {
                        Map<String, String> ph = new HashMap<>();
                        ph.put("node", "djeconomy.gui.admin.economy.confirm.manage");
                        p.sendMessage(getPrefix() + msg("gui.admin.economy.error.no_permission", ph, "You lack permission: %node%"));
                        break;
                    }
                    // Open GUI-based reason selection instead of chat capture
                    AdminConfirmationManager mgr = plugin.getAdminConfirmationManager();
                    AdminConfirmationManager.PendingAdminAction pending = (mgr != null) ? mgr.getPending(p.getUniqueId()) : null;
                    try {
                        if (pending != null) {
                            plugin.getLogger().info(String.format("[ADMIN-GUI] %s approved (selecting reason) %s %s $%.2f",
                                p.getName(), String.valueOf(pending.action), String.valueOf(pending.playerName), pending.amount));
                        } else {
                            plugin.getLogger().info(String.format("[ADMIN-GUI] %s clicked Approve with no pending action", p.getName()));
                        }
                    } catch (Throwable ignored) {}
                    openReasonSelect(p);
                    break;
                }
                if ("Cancel".equalsIgnoreCase(name)) {
                    if (!p.hasPermission("djeconomy.gui.admin.economy.confirm.manage")) {
                        Map<String, String> ph = new HashMap<>();
                        ph.put("node", "djeconomy.gui.admin.economy.confirm.manage");
                        p.sendMessage(getPrefix() + msg("gui.admin.economy.error.no_permission", ph, "You lack permission: %node%"));
                        break;
                    }
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
            case AMOUNT_INPUT: {
                // Keypad handling and confirmation
                String prefix = getPrefix();
                if ("Cancel".equalsIgnoreCase(name)) {
                    // Return to player account if possible
                    if (s.getTargetPlayerId() != null) {
                        OfflinePlayer tgt = Bukkit.getOfflinePlayer(s.getTargetPlayerId());
                        openPlayerAccount(p, tgt);
                    } else {
                        openHome(p);
                    }
                    break;
                }
                if ("Backspace".equalsIgnoreCase(name)) {
                    String cur = s.getAmountInput() == null ? "" : s.getAmountInput();
                    if (!cur.isEmpty()) {
                        s.setAmountInput(cur.substring(0, cur.length() - 1));
                    }
                    // Refresh view
                    OfflinePlayer tgt = Bukkit.getOfflinePlayer(s.getTargetPlayerId());
                    openAmountInput(p, tgt, s.getPendingAction());
                    break;
                }
                if ("Clear".equalsIgnoreCase(name)) {
                    s.setAmountInput("");
                    OfflinePlayer tgt = Bukkit.getOfflinePlayer(s.getTargetPlayerId());
                    openAmountInput(p, tgt, s.getPendingAction());
                    break;
                }
                if ("Confirm".equalsIgnoreCase(name)) {
                    String inp = s.getAmountInput() == null ? "" : s.getAmountInput();
                    if (inp.isEmpty() || ".".equals(inp)) {
                        p.sendMessage(prefix + msg("admin.invalid_amount", null, "§cInvalid amount!"));
                        break;
                    }
                    try {
                        double amount = Double.parseDouble(inp);
                        if (amount < 0) {
                            p.sendMessage(prefix + msg("admin.negative_amount", null, "§cAmount cannot be negative!"));
                            break;
                        }
                        double MAX = 1_000_000_000d;
                        if (amount > MAX) {
                            Map<String, String> ph = new HashMap<>();
                            ph.put("max", com.boopugstudios.dynamicjobseconomy.util.EconomyFormat.money(MAX));
                            p.sendMessage(prefix + msg("admin.amount_too_large", ph, "§cAmount too large! Maximum: %max%"));
                            break;
                        }
                        UUID targetId = s.getTargetPlayerId();
                        if (targetId == null || s.getPendingAction() == null) {
                            p.sendMessage(prefix + msg("admin.failed_execute", null, "§cFailed to execute economy command!"));
                            break;
                        }
                        // Threshold check -> create pending confirmation or execute immediately
                        if (amount >= getConfirmThreshold()) {
                            AdminConfirmationManager mgr = plugin.getAdminConfirmationManager();
                            OfflinePlayer off = Bukkit.getOfflinePlayer(targetId);
                            String playerName = off != null && off.getName() != null ? off.getName() : (Bukkit.getPlayer(targetId) != null ? Bukkit.getPlayer(targetId).getName() : safeName(off));
                            if (mgr != null) {
                                mgr.putPending(p.getUniqueId(), s.getPendingAction(), playerName, amount);
                            }
                            Map<String, String> ph1 = new HashMap<>();
                            ph1.put("money", com.boopugstudios.dynamicjobseconomy.util.EconomyFormat.money(amount));
                            p.sendMessage(prefix + msg("admin.large_detected", ph1, "§e⚠ Large amount detected: %money%"));
                            Map<String, String> ph2 = new HashMap<>();
                            ph2.put("seconds", String.valueOf(plugin.getAdminConfirmationManager().getExpirySeconds()));
                            p.sendMessage(prefix + msg("admin.confirm_prompt", ph2, "§eUse §f/djeconomy confirm §eto proceed (expires in %seconds% seconds)"));
                            openConfirmations(p);
                        } else {
                            boolean ok = executeEconomyAction(p, s.getPendingAction(), targetId, amount, prefix);
                            // Return to player account after execution
                            OfflinePlayer tgt = Bukkit.getOfflinePlayer(targetId);
                            openPlayerAccount(p, tgt);
                            if (!ok) {
                                p.sendMessage(prefix + msg("admin.failed_execute", null, "§cFailed to execute economy command!"));
                            }
                        }
                    } catch (NumberFormatException ex) {
                        p.sendMessage(prefix + msg("admin.invalid_amount", null, "§cInvalid amount!"));
                    }
                    break;
                }
                // Digits and decimal point
                if ("0".equals(name) || "1".equals(name) || "2".equals(name) || "3".equals(name) ||
                    "4".equals(name) || "5".equals(name) || "6".equals(name) || "7".equals(name) ||
                    "8".equals(name) || "9".equals(name) || ".".equals(name)) {
                    String cur = s.getAmountInput() == null ? "" : s.getAmountInput();
                    if (".".equals(name)) {
                        if (cur.contains(".")) { break; }
                        if (cur.isEmpty()) cur = "0"; // prepend 0 for leading dot
                        cur = cur + ".";
                    } else {
                        // Limit to 12 characters to avoid spam
                        if (cur.length() >= 12) { break; }
                        cur = cur + name;
                    }
                    s.setAmountInput(cur);
                    OfflinePlayer tgt = Bukkit.getOfflinePlayer(s.getTargetPlayerId());
                    openAmountInput(p, tgt, s.getPendingAction());
                }
                break;
            }
            case REASON_SELECT: {
                String prefix = getPrefix();
                if ("Back".equalsIgnoreCase(name)) { openConfirmations(p); break; }
                AdminConfirmationManager mgr = plugin.getAdminConfirmationManager();
                if (mgr == null) { p.sendMessage(prefix + msg("admin.no_pending_confirm", null, "§cNo pending action to confirm!")); openHome(p); break; }
                AdminConfirmationManager.PendingAdminAction pending = mgr.getPending(p.getUniqueId());
                if (pending == null) { p.sendMessage(prefix + msg("admin.no_pending_confirm", null, "§cNo pending action to confirm!")); openHome(p); break; }
                // Expiry check
                if (pending.isExpired(System.currentTimeMillis(), mgr.getExpiryMillis())) {
                    mgr.remove(p.getUniqueId());
                    p.sendMessage(prefix + msg("admin.confirm_expired", null, "§cConfirmation expired! Please retry the command."));
                    openHome(p);
                    break;
                }
                String chosenReason = null;
                if ("Skip Reason".equalsIgnoreCase(name)) {
                    chosenReason = null;
                } else if (!"".equals(name) && !"Back".equalsIgnoreCase(name)) {
                    chosenReason = name;
                }
                mgr.setReason(p.getUniqueId(), chosenReason);
                // Acknowledge and execute confirm on main thread
                p.sendMessage(prefix + msg("admin.reason.captured", null, "§aReason captured. Executing confirmation..."));
                Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        p.performCommand("djeconomy confirm");
                    } catch (Throwable ignored) {
                        Bukkit.dispatchCommand(p, "djeconomy confirm");
                    }
                });
                break;
            }
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
    // Chat-based amount input removed; replaced by GUI keypad in AMOUNT_INPUT view

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
    // Config accessors mirror those in AdminCommand
    private double getConfirmThreshold() {
        double v = plugin.getConfig().getDouble("economy.admin_confirmation.threshold", 100000.0);
        return v <= 0 ? 100000.0 : v;
    }
    // removed unused getConfirmExpirySeconds()
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

    // ---------- New GUI Input Flows ----------

    private void openAmountInput(Player admin, OfflinePlayer target, String action) {
        String title = msg("gui.admin.economy.title.amount_input", null, "Enter Amount");
        Inventory inv = Bukkit.createInventory(new Holder(View.AMOUNT_INPUT), View.AMOUNT_INPUT.size(), color("&e" + title));

        Session s = sessions.computeIfAbsent(admin.getUniqueId(), id -> new Session(View.AMOUNT_INPUT));
        s.setView(View.AMOUNT_INPUT);
        s.setTargetPlayerId(target.getUniqueId());
        s.setPendingAction(action);
        if (s.getAmountInput() == null) s.setAmountInput("");

        // Display panel
        String display = (s.getAmountInput().isEmpty() ? "_" : s.getAmountInput());
        List<String> lore = new ArrayList<>();
        lore.add(color("&7Target: &f" + safeName(target)));
        lore.add(color("&7Action: &f" + action.toUpperCase(Locale.ROOT)));
        lore.add(color("&7Input: &6" + display));
        lore.add(color("&8Use keypad below. '.' allowed."));
        inv.setItem(4, menuItem(Material.PAPER, color("&bAmount Entry"), lore));

        // Keypad digits
        Map<Integer, String> keys = new HashMap<>();
        keys.put(19, "7"); keys.put(20, "8"); keys.put(21, "9");
        keys.put(28, "4"); keys.put(29, "5"); keys.put(30, "6");
        keys.put(37, "1"); keys.put(38, "2"); keys.put(39, "3");
        keys.put(40, "0"); keys.put(41, ".");
        for (Map.Entry<Integer, String> e : keys.entrySet()) {
            String k = e.getValue();
            Material mat = k.equals(".") ? Material.LIGHT_GRAY_DYE : Material.YELLOW_DYE;
            inv.setItem(e.getKey(), menuItem(mat, color("&f" + k), Collections.singletonList(color("&7Click to add"))));
        }
        // Backspace, Clear, Confirm, Back
        inv.setItem(42, menuItem(Material.FLINT, color("&eBackspace"), Collections.singletonList(color("&7Remove last character"))));
        inv.setItem(25, menuItem(Material.BARRIER, color("&cClear"), Collections.singletonList(color("&7Reset input"))));
        inv.setItem(34, menuItem(Material.LIME_DYE, color("&aConfirm"), Arrays.asList(color("&7Proceed with amount"), color("&8Large amounts will require confirmation"))));
        inv.setItem(49, menuItem(Material.ARROW, color("&eCancel"), Collections.singletonList(color("&7Go back"))));

        fill(inv, Material.YELLOW_STAINED_GLASS_PANE);
        admin.openInventory(inv);
    }

    public void openReasonSelect(Player player) {
        String title = msg("gui.admin.economy.title.reason_select", null, "Select Reason");
        Inventory inv = Bukkit.createInventory(new Holder(View.REASON_SELECT), View.REASON_SELECT.size(), color("&d" + title));

        List<String> options = Arrays.asList(
            "Refund (bug)",
            "Anti-cheat action",
            "Manual adjustment",
            "Business payout correction"
        );
        int[] slots = {20, 21, 22, 23};
        for (int i = 0; i < options.size(); i++) {
            inv.setItem(slots[i], menuItem(Material.PAPER, color("&f" + options.get(i)), Collections.singletonList(color("&7Use this reason"))));
        }
        inv.setItem(31, menuItem(Material.BARRIER, color("&7Skip Reason"), Collections.singletonList(color("&7No reason provided"))));
        inv.setItem(49, menuItem(Material.ARROW, color("&eBack"), Collections.singletonList(color("&7Return"))));

        fill(inv, Material.PURPLE_STAINED_GLASS_PANE);
        player.openInventory(inv);
        Session s = sessions.computeIfAbsent(player.getUniqueId(), id -> new Session(View.REASON_SELECT));
        s.setView(View.REASON_SELECT);
    }

    // --- Immediate execution helpers (replicate AdminCommand behavior) ---
    private boolean executeEconomyAction(Player sender, String action, UUID targetId, double amount, String prefix) {
        OfflinePlayer off = Bukkit.getOfflinePlayer(targetId);
        Player online = Bukkit.getPlayer(targetId);
        boolean isOnline = online != null && online.isOnline();
        EconomyManager econ = plugin.getEconomyManager();

        // Inform if offline
        if (!isOnline) {
            Map<String, String> ph = new HashMap<>();
            ph.put("player", safeName(off));
            sender.sendMessage(prefix + msg("admin.offline_note", ph, "§7Note: Player '%player%' is offline. Processing transaction..."));
        }

        switch (action.toLowerCase(Locale.ROOT)) {
            case "give": {
                boolean ok = isOnline ? econ.deposit(online, amount) : econ.depositPlayer(off, amount);
                if (ok) {
                    String money = com.boopugstudios.dynamicjobseconomy.util.EconomyFormat.money(amount);
                    Map<String, String> ph = new HashMap<>();
                    ph.put("money", money);
                    ph.put("player", safeName(off));
                    sender.sendMessage(prefix + msg("admin.give_success", ph, "§aGave %money% to %player%"));
                    appendHistory(sender.getName(), "GIVE", safeName(off), amount, null);
                    try { plugin.getAdminAuditLogger().logEconomyAction(sender, "give", safeName(off), amount); } catch (Throwable ignored) {}
                    return true;
                }
                return false;
            }
            case "take": {
                double balance = isOnline ? econ.getBalance(online) : econ.getBalance(off);
                if (balance < amount) {
                    String balStr = com.boopugstudios.dynamicjobseconomy.util.EconomyFormat.money(balance);
                    Map<String, String> ph = new HashMap<>();
                    ph.put("balance", balStr);
                    sender.sendMessage(prefix + msg("admin.take_insufficient", ph, "§cPlayer only has %balance%!"));
                    return true;
                }
                boolean ok = isOnline ? econ.withdraw(online, amount) : econ.withdraw(off, amount);
                if (ok) {
                    String money = com.boopugstudios.dynamicjobseconomy.util.EconomyFormat.money(amount);
                    Map<String, String> ph = new HashMap<>();
                    ph.put("money", money);
                    ph.put("player", safeName(off));
                    sender.sendMessage(prefix + msg("admin.take_success", ph, "§aTook %money% from %player%"));
                    appendHistory(sender.getName(), "TAKE", safeName(off), amount, null);
                    try { plugin.getAdminAuditLogger().logEconomyAction(sender, "take", safeName(off), amount); } catch (Throwable ignored) {}
                    return true;
                }
                return false;
            }
            case "set": {
                double current = isOnline ? econ.getBalance(online) : econ.getBalance(off);
                boolean withdrew = true;
                if (current > 0) {
                    withdrew = isOnline ? econ.withdraw(online, current) : econ.withdraw(off, current);
                }
                if (!withdrew) return false;
                boolean deposited = isOnline ? econ.deposit(online, amount) : econ.depositPlayer(off, amount);
                if (deposited) {
                    String money = com.boopugstudios.dynamicjobseconomy.util.EconomyFormat.money(amount);
                    Map<String, String> ph = new HashMap<>();
                    ph.put("player", safeName(off));
                    ph.put("money", money);
                    sender.sendMessage(prefix + msg("admin.set_success", ph, "§aSet %player%'s balance to %money%"));
                    appendHistory(sender.getName(), "SET", safeName(off), amount, null);
                    try { plugin.getAdminAuditLogger().logEconomyAction(sender, "set", safeName(off), amount); } catch (Throwable ignored) {}
                    return true;
                }
                return false;
            }
            default:
                sender.sendMessage(prefix + msg("admin.invalid_action", null, "§cInvalid action! Use give, take, or set"));
                return true;
        }
    }

    private java.io.File getHistoryFile() {
        java.io.File dir = plugin.getDataFolder();
        if (dir == null) {
            dir = new java.io.File(System.getProperty("java.io.tmpdir"), "dje-data");
        }
        if (!dir.exists()) dir.mkdirs();
        return new java.io.File(dir, "admin-economy-history.log");
    }

    private void appendHistory(String admin, String action, String target, double amount, String reason) {
        java.io.File file = getHistoryFile();
        try (java.io.PrintWriter out = new java.io.PrintWriter(new java.io.FileWriter(file, true))) {
            String safeReason = reason == null ? "" : reason.replace('\n', ' ').replace('\r', ' ');
            out.printf("%d|%s|%s|%s|%.2f|%s%n", System.currentTimeMillis(), admin, action, target, amount, safeReason);
        } catch (java.io.IOException e) {
            plugin.getLogger().warning("Failed to write history: " + e.getMessage());
        }
    }
}
