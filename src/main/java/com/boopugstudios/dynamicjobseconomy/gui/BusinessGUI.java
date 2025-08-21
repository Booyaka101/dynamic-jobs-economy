package com.boopugstudios.dynamicjobseconomy.gui;

import com.boopugstudios.dynamicjobseconomy.business.*;
import com.boopugstudios.dynamicjobseconomy.DynamicJobsEconomy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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

import java.util.*;

/**
 * Simple GUI interface for business management in Minecraft
 * Provides intuitive inventory-based menus for business operations
 */
public class BusinessGUI implements Listener {
    
    private final ConsolidatedBusinessManager businessManager;
    private final Map<UUID, GUISession> activeSessions;
    private final DynamicJobsEconomy plugin;
    private final boolean debugClicks;
    private final boolean useFillerPanes;
    
    public BusinessGUI(DynamicJobsEconomy plugin, ConsolidatedBusinessManager businessManager) {
        this.plugin = plugin;
        this.businessManager = businessManager;
        this.activeSessions = new HashMap<>();
        this.debugClicks = plugin.getConfig().getBoolean("debug.guiClicks", false);
        this.useFillerPanes = plugin.getConfig().getBoolean("gui.useFillerPanes", true);
        
        // Register event listener
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * Represents an active GUI session for a player
     */
    public static class GUISession {
        private final UUID playerUUID;
        private final GUIType guiType;
        private Business currentBusiness;
        private long lastInteraction;
        private final Map<String, Object> sessionData;
        
        public GUISession(UUID playerUUID, GUIType guiType) {
            this.playerUUID = playerUUID;
            this.guiType = guiType;
            this.lastInteraction = System.currentTimeMillis();
            this.sessionData = new HashMap<>();
        }
        
        public UUID getPlayerUUID() { return playerUUID; }
        public GUIType getGuiType() { return guiType; }
        public GUIType getCurrentGUI() { return guiType; }
        public Business getCurrentBusiness() { return currentBusiness; }
        public void setCurrentBusiness(Business business) { this.currentBusiness = business; }
        public long getLastInteraction() { return lastInteraction; }
        public void updateLastInteraction() { this.lastInteraction = System.currentTimeMillis(); }
        public Map<String, Object> getSessionData() { return sessionData; }
        public long getOpenTime() { return lastInteraction; }
        
        public boolean isExpired() {
            return System.currentTimeMillis() - lastInteraction > 300000; // 5 minutes
        }
    }
    
    /**
     * Types of GUI interfaces available
     */
    public enum GUIType {
        MAIN_MENU("Business Management", 27),
        BUSINESS_LIST("My Businesses", 54),
        BUSINESS_DETAILS("Business Details", 45),
        EMPLOYEE_MANAGEMENT("Employee Management", 54),
        REVENUE_OVERVIEW("Revenue Overview", 36),
        LOCATIONS_MENU("Business Locations", 45),
        PROCESSING_CHAINS("Processing Chains", 54),
        CONSTRUCTION_CONTRACTS("Construction Contracts", 54),
        CREATE_BUSINESS("Create New Business", 27);
        
        private final String title;
        private final int size;
        
        GUIType(String title, int size) {
            this.title = title;
            this.size = size;
        }
        
        public String getTitle() { return title; }
        public int getSize() { return size; }
    }
    
    /**
     * Open the main business management GUI for a player
     */
    public void openMainMenu(Player player) {
        Inventory gui = Bukkit.createInventory(new BusinessGUIHolder(GUIType.MAIN_MENU), 
                GUIType.MAIN_MENU.getSize(), ChatColor.DARK_GREEN + GUIType.MAIN_MENU.getTitle());
        
        // Main menu items
        gui.setItem(10, createMenuItem(Material.EMERALD, ChatColor.GREEN + "My Businesses", 
                Arrays.asList(ChatColor.GRAY + "View and manage your businesses", 
                             ChatColor.YELLOW + "Click to open business list")));
        
        gui.setItem(12, createMenuItem(Material.GOLD_INGOT, ChatColor.GOLD + "Revenue Overview", 
                Arrays.asList(ChatColor.GRAY + "View revenue and financial reports", 
                             ChatColor.YELLOW + "Click to view revenue dashboard")));
        
        gui.setItem(14, createMenuItem(Material.COMPASS, ChatColor.BLUE + "Business Locations", 
                Arrays.asList(ChatColor.GRAY + "Manage physical business locations", 
                             ChatColor.YELLOW + "Click to view location manager")));
        
        gui.setItem(16, createMenuItem(Material.CRAFTING_TABLE, ChatColor.AQUA + "Processing Chains", 
                Arrays.asList(ChatColor.GRAY + "Set up resource processing operations", 
                             ChatColor.YELLOW + "Click to manage processing chains")));
        
        gui.setItem(19, createMenuItem(Material.BRICK, ChatColor.RED + "Construction Contracts", 
                Arrays.asList(ChatColor.GRAY + "Manage building and construction projects", 
                             ChatColor.YELLOW + "Click to view active contracts")));
        
        gui.setItem(21, createMenuItem(Material.PLAYER_HEAD, ChatColor.LIGHT_PURPLE + "Employee Management", 
                Arrays.asList(ChatColor.GRAY + "Hire, fire, and manage employees", 
                             ChatColor.YELLOW + "Click to open employee panel")));
        
        gui.setItem(23, createMenuItem(Material.NETHER_STAR, ChatColor.YELLOW + "Create New Business", 
                Arrays.asList(ChatColor.GRAY + "Start a new business venture", 
                             ChatColor.YELLOW + "Click to create business")));
        
        gui.setItem(25, createMenuItem(Material.BARRIER, ChatColor.RED + "Close Menu", 
                Arrays.asList(ChatColor.GRAY + "Close this interface")));
        
        // Fill empty slots with glass panes
        fillEmptySlots(gui, Material.GRAY_STAINED_GLASS_PANE);
        
        player.openInventory(gui);
        activeSessions.put(player.getUniqueId(), new GUISession(player.getUniqueId(), GUIType.MAIN_MENU));
    }
    
    /**
     * Open business list GUI showing player's businesses
     */
    public void openBusinessList(Player player) {
        List<Business> businesses = businessManager.getBusinessesByOwner(player.getUniqueId());
        
        Inventory gui = Bukkit.createInventory(new BusinessGUIHolder(GUIType.BUSINESS_LIST), 
                GUIType.BUSINESS_LIST.getSize(), ChatColor.DARK_GREEN + GUIType.BUSINESS_LIST.getTitle());
        
        // Add businesses to GUI
        for (int i = 0; i < Math.min(businesses.size(), 45); i++) {
            Business business = businesses.get(i);
            ItemStack businessItem = createBusinessItem(business);
            gui.setItem(i, businessItem);
        }
        
        // Navigation items
        gui.setItem(49, createMenuItem(Material.ARROW, ChatColor.YELLOW + "Back to Main Menu", 
                Arrays.asList(ChatColor.GRAY + "Return to main business menu")));
        
        gui.setItem(53, createMenuItem(Material.EMERALD, ChatColor.GREEN + "Create New Business", 
                Arrays.asList(ChatColor.GRAY + "Start a new business venture")));
        
        fillEmptySlots(gui, Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        
        player.openInventory(gui);
        activeSessions.put(player.getUniqueId(), new GUISession(player.getUniqueId(), GUIType.BUSINESS_LIST));
    }
    
    /**
     * Open detailed view of a specific business
     */
    public void openBusinessDetails(Player player, Business business) {
        Inventory gui = Bukkit.createInventory(new BusinessGUIHolder(GUIType.BUSINESS_DETAILS), 
                GUIType.BUSINESS_DETAILS.getSize(), ChatColor.DARK_GREEN + business.getName());
        
        // Business info
        gui.setItem(4, createBusinessItem(business));
        
        // Management options
        gui.setItem(19, createMenuItem(Material.PLAYER_HEAD, ChatColor.AQUA + "Employees (" + business.getEmployees().size() + ")", 
                Arrays.asList(ChatColor.GRAY + "Manage business employees", 
                             ChatColor.YELLOW + "Click to open employee panel")));
        
        gui.setItem(21, createMenuItem(Material.GOLD_INGOT, ChatColor.GOLD + "Revenue: $" + String.format("%.2f", business.getBalance()), 
                Arrays.asList(ChatColor.GRAY + "Business financial overview", 
                             ChatColor.YELLOW + "Click for detailed revenue report")));
        
        gui.setItem(23, createMenuItem(Material.COMPASS, ChatColor.BLUE + "Locations", 
                Arrays.asList(ChatColor.GRAY + "Manage business locations", 
                             ChatColor.YELLOW + "Click to view location manager")));
        
        gui.setItem(25, createMenuItem(Material.CRAFTING_TABLE, ChatColor.GREEN + "Processing", 
                Arrays.asList(ChatColor.GRAY + "Resource processing operations", 
                             ChatColor.YELLOW + "Click to manage processing chains")));
        
        // Navigation
        gui.setItem(36, createMenuItem(Material.ARROW, ChatColor.YELLOW + "Back to Business List", 
                Arrays.asList(ChatColor.GRAY + "Return to business list")));
        
        gui.setItem(44, createMenuItem(Material.BARRIER, ChatColor.RED + "Close", 
                Arrays.asList(ChatColor.GRAY + "Close this interface")));
        
        fillEmptySlots(gui, Material.BLUE_STAINED_GLASS_PANE);
        
        player.openInventory(gui);
        GUISession session = new GUISession(player.getUniqueId(), GUIType.BUSINESS_DETAILS);
        session.getSessionData().put("business", business);
        activeSessions.put(player.getUniqueId(), session);
    }
    
    /**
     * Create a menu item with custom name and lore
     */
    private ItemStack createMenuItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    /**
     * Create an item representing a business
     */
    private ItemStack createBusinessItem(Business business) {
        Material material = getBusinessMaterial(business.getType());
        List<String> lore = Arrays.asList(
                ChatColor.GRAY + "Type: " + ChatColor.WHITE + business.getType(),
                ChatColor.GRAY + "Employees: " + ChatColor.WHITE + business.getEmployees().size(),
                ChatColor.GRAY + "Balance: " + ChatColor.GOLD + "$" + String.format("%.2f", business.getBalance()),
                ChatColor.GRAY + "Revenue Model: " + ChatColor.YELLOW + business.getRevenueModel().getDisplayName(),
                "",
                ChatColor.YELLOW + "Click to manage this business"
        );
        
        return createMenuItem(material, ChatColor.GREEN + business.getName(), lore);
    }
    
    /**
     * Get appropriate material for business type
     */
    private Material getBusinessMaterial(String businessType) {
        switch (businessType.toLowerCase()) {
            case "restaurant": return Material.COOKED_BEEF;
            case "construction": return Material.BRICK;
            case "mining": return Material.DIAMOND_PICKAXE;
            case "farming": return Material.WHEAT;
            case "retail": return Material.CHEST;
            case "technology": return Material.REDSTONE;
            case "manufacturing": return Material.CRAFTING_TABLE;
            case "transportation": return Material.MINECART;
            default: return Material.EMERALD;
        }
    }
    
    /**
     * Fill empty inventory slots with glass panes
     */
    private void fillEmptySlots(Inventory inventory, Material material) {
        if (!useFillerPanes) {
            return;
        }
        ItemStack filler = new ItemStack(material);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            filler.setItemMeta(meta);
        }
        
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }
    }
    
    /**
     * Handle inventory click events
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        UUID playerUUID = player.getUniqueId();
        
        if (!activeSessions.containsKey(playerUUID)) return;
        org.bukkit.inventory.Inventory top = event.getView().getTopInventory();
        if (top == null || !(top.getHolder() instanceof BusinessGUIHolder)) return;
        
        event.setCancelled(true); // Prevent item movement
        if (event.getClickedInventory() == null) return; // Clicked outside
        
        GUISession session = activeSessions.get(playerUUID);
        ItemStack clickedItem = event.getCurrentItem();
        
        if (debugClicks) {
            String name = (clickedItem != null && clickedItem.getItemMeta() != null) ? ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName()) : "";
            String whichInv = event.getClickedInventory() == event.getView().getTopInventory() ? "TOP" : "BOTTOM";
            plugin.getLogger().info(String.format("[GUI-DEBUG] Click by %s gui=%s slot=%d rawSlot=%d inv=%s type=%s name='%s' action=%s", 
                    player.getName(),
                    session.getCurrentGUI(),
                    event.getSlot(),
                    event.getRawSlot(),
                    whichInv,
                    (clickedItem == null ? "null" : clickedItem.getType().name()),
                    name,
                    event.getAction()));
        }

        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
        
        handleGUIClick(player, session, clickedItem, event.getSlot());
    }

    /**
     * Prevent dragging items in GUI inventories
     */
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        org.bukkit.inventory.Inventory top = event.getView().getTopInventory();
        if (top != null && top.getHolder() instanceof BusinessGUIHolder) {
            if (debugClicks) {
                Player p = (Player) event.getWhoClicked();
                plugin.getLogger().info(String.format("[GUI-DEBUG] Drag by %s slots=%s type=%s", 
                        p.getName(),
                        event.getRawSlots().toString(),
                        event.getType()));
            }
            event.setCancelled(true);
        }
    }
    
    /**
     * Handle specific GUI click actions
     */
    private void handleGUIClick(Player player, GUISession session, ItemStack clickedItem, int slot) {
        String itemName = clickedItem.getItemMeta() != null ? 
                ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName()) : "";
        
        switch (session.getCurrentGUI()) {
            case MAIN_MENU:
                handleMainMenuClick(player, itemName);
                break;
            case BUSINESS_LIST:
                handleBusinessListClick(player, itemName, slot);
                break;
            case BUSINESS_DETAILS:
                handleBusinessDetailsClick(player, session, itemName);
                break;
            case EMPLOYEE_MANAGEMENT:
                handleEmployeeManagementClick(player, session, itemName, slot);
                break;
            case REVENUE_OVERVIEW:
                handleRevenueOverviewClick(player, session, itemName, slot);
                break;
            case LOCATIONS_MENU:
                handleLocationsMenuClick(player, session, itemName, slot);
                break;
            case PROCESSING_CHAINS:
                handleProcessingChainsClick(player, session, itemName, slot);
                break;
            case CONSTRUCTION_CONTRACTS:
                handleConstructionContractsClick(player, session, itemName, slot);
                break;
            case CREATE_BUSINESS:
                handleCreateBusinessClick(player, session, itemName);
                break;
        }
    }
    
    /**
     * Handle main menu clicks
     */
    private void handleMainMenuClick(Player player, String itemName) {
        switch (itemName) {
            case "My Businesses":
                openBusinessList(player);
                break;
            case "Revenue Overview":
                openRevenueOverview(player);
                break;
            case "Business Locations":
                openLocationsMenu(player);
                break;
            case "Processing Chains":
                openProcessingChains(player);
                break;
            case "Construction Contracts":
                openConstructionContracts(player);
                break;
            case "Employee Management":
                openEmployeeManagement(player);
                break;
            case "Create New Business":
                openCreateBusiness(player);
                break;
            case "Close Menu":
                player.closeInventory();
                break;
        }
    }
    
    /**
     * Handle business list clicks
     */
    private void handleBusinessListClick(Player player, String itemName, int slot) {
        if (itemName.equals("Back to Main Menu")) {
            openMainMenu(player);
        } else if (itemName.equals("Create New Business")) {
            player.closeInventory();
            player.sendMessage(ChatColor.YELLOW + "Use /business create <name> <type> to create a new business!");
        } else if (slot < 45) {
            // Clicked on a business item
            List<Business> businesses = businessManager.getBusinessesByOwner(player.getUniqueId());
            if (slot < businesses.size()) {
                Business business = businesses.get(slot);
                openBusinessDetails(player, business);
            }
        }
    }
    
    /**
     * Handle business details clicks
     */
    private void handleBusinessDetailsClick(Player player, GUISession session, String itemName) {
        Business business = (Business) session.getSessionData().get("business");
        
        switch (itemName) {
            case "Back to Business List":
                openBusinessList(player);
                break;
            case "Close":
                player.closeInventory();
                break;
            default:
                if (itemName.startsWith("Employees")) {
                    openEmployeeManagementGUI(player, business);
                } else if (itemName.startsWith("Revenue")) {
                    openRevenueOverviewGUI(player, business);
                } else if (itemName.equals("Locations")) {
                    openLocationManagementGUI(player, business);
                } else if (itemName.equals("Processing")) {
                    openProcessingChainGUI(player, business);
                }
                break;
        }
    }
    
    /**
     * Handle inventory close events
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            activeSessions.remove(player.getUniqueId());
        }
    }
    
    /**
     * Custom inventory holder for business GUIs
     */
    private static class BusinessGUIHolder implements InventoryHolder {
        private final GUIType type;

        public BusinessGUIHolder(GUIType guiType) {
            this.type = guiType;
        }

        @SuppressWarnings("unused")
        public GUIType getType() {
            return type;
        }

        @Override
        public Inventory getInventory() {
            return null; // Not used
        }
    }
    
    /**
     * Clean up inactive sessions (called periodically)
     */
    public void cleanupInactiveSessions() {
        long currentTime = System.currentTimeMillis();
        activeSessions.entrySet().removeIf(entry ->
                currentTime - entry.getValue().getOpenTime() > 300000); // 5 minutes
    }

    /**
     * Handle employee management menu clicks (top-level and per-business)
     */
    private void handleEmployeeManagementClick(Player player, GUISession session, String itemName, int slot) {
        // Global navigation
        if ("Back to Main Menu".equals(itemName)) {
            openMainMenu(player);
            return;
        }

        Business current = session.getCurrentBusiness();

        // Top-level Employee Management (list of businesses)
        if (current == null) {
            if ("Hire Employee".equals(itemName)) {
                player.closeInventory();
                player.sendMessage("§6Use: §f/business hire <player> <position> <salary>");
                return;
            }
            if (slot < 21) {
                List<Business> businesses = businessManager.getPlayerBusinesses(player);
                if (slot < businesses.size()) {
                    openEmployeeManagementGUI(player, businesses.get(slot));
                }
            }
            return;
        }

        // Per-business Employee Management
        if ("Back to Business Menu".equals(itemName)) {
            openBusinessDetails(player, current);
            return;
        }
        if ("Back to Employees".equals(itemName)) {
            openEmployeeManagementGUI(player, current);
            return;
        }
        if (itemName.startsWith("Employee: ")) {
            String employeeName = itemName.substring("Employee: ".length());
            openEmployeeDetailsGUI(player, current, employeeName);
            return;
        }
        if ("Add Employee".equals(itemName)) {
            player.closeInventory();
            player.sendMessage("§6Use: §f/business hire <player> <position> <salary>");
            return;
        }
        if ("Manage Positions".equals(itemName)) {
            openPositionManagementGUI(player, current);
        }
    }

    /**
     * Handle revenue overview clicks (top-level and per-business)
     */
    private void handleRevenueOverviewClick(Player player, GUISession session, String itemName, int slot) {
        if ("Back to Main Menu".equals(itemName)) {
            openMainMenu(player);
            return;
        }
        Business current = session.getCurrentBusiness();
        if (current == null) {
            // Top-level Revenue list
            if (slot < 21) {
                List<Business> businesses = businessManager.getPlayerBusinesses(player);
                if (slot < businesses.size()) {
                    openRevenueOverviewGUI(player, businesses.get(slot));
                }
            }
        } else {
            if ("Back to Business Menu".equals(itemName)) {
                openBusinessDetails(player, current);
            }
        }
    }

    /**
     * Handle locations menu clicks (top-level and per-business)
     */
    private void handleLocationsMenuClick(Player player, GUISession session, String itemName, int slot) {
        if ("Back to Main Menu".equals(itemName)) {
            openMainMenu(player);
            return;
        }
        Business current = session.getCurrentBusiness();
        if (current == null) {
            if ("Add New Location".equals(itemName)) {
                player.closeInventory();
                player.sendMessage("§6Use: §f/business add-location <type> <region>");
                return;
            }
            if (slot < 21) {
                List<Business> businesses = businessManager.getPlayerBusinesses(player);
                if (slot < businesses.size()) {
                    openLocationManagementGUI(player, businesses.get(slot));
                }
            }
        } else {
            if ("Back to Business Menu".equals(itemName)) {
                openBusinessDetails(player, current);
                return;
            }
            if ("Add Location".equals(itemName)) {
                player.closeInventory();
                player.sendMessage("§6Use: §f/business add-location <type> <region>");
            }
        }
    }

    /**
     * Handle processing chains clicks (top-level and per-business)
     */
    private void handleProcessingChainsClick(Player player, GUISession session, String itemName, int slot) {
        if ("Back to Main Menu".equals(itemName)) {
            openMainMenu(player);
            return;
        }
        Business current = session.getCurrentBusiness();
        if (current == null) {
            if ("Add Processing Chain".equals(itemName)) {
                player.closeInventory();
                player.sendMessage("§6Use: §f/business add-processing <type>");
                return;
            }
            if (slot < 21) {
                List<Business> businesses = businessManager.getPlayerBusinesses(player);
                if (slot < businesses.size()) {
                    openProcessingChainGUI(player, businesses.get(slot));
                }
            }
        } else {
            if ("Back to Business Menu".equals(itemName)) {
                openBusinessDetails(player, current);
                return;
            }
            if ("Add Processing Chain".equals(itemName)) {
                player.closeInventory();
                player.sendMessage("§6Use: §f/business add-processing <type>");
            }
        }
    }

    /**
     * Handle construction contracts clicks
     */
    private void handleConstructionContractsClick(Player player, GUISession session, String itemName, int slot) {
        if ("Back to Main Menu".equals(itemName)) {
            openMainMenu(player);
            return;
        }
        if ("New Contract".equals(itemName)) {
            player.closeInventory();
            player.performCommand("gigs list");
            return;
        }
        if (slot < 21) {
            List<Business> businesses = businessManager.getPlayerBusinesses(player);
            if (slot < businesses.size()) {
                player.closeInventory();
                player.performCommand("gigs mine");
            }
        }
    }

    /**
     * Handle create business menu clicks
     */
    private void handleCreateBusinessClick(Player player, GUISession session, String itemName) {
        if ("Back to Main Menu".equals(itemName)) {
            openMainMenu(player);
            return;
        }
        if ("Instructions".equals(itemName)) {
            // No-op
            return;
        }
        if (itemName.endsWith(" Business")) {
            // Derive type from first word
            String type = itemName.split(" ")[0].toLowerCase();
            player.closeInventory();
            player.sendMessage("§6Use: §f/business create <name> " + type);
        }
    }
    
    /**
     * Open revenue overview menu
     */
    private void openRevenueOverview(Player player) {
        Inventory inventory = Bukkit.createInventory(new BusinessGUIHolder(GUIType.REVENUE_OVERVIEW), 45, "§6Revenue Overview");
        
        List<Business> playerBusinesses = businessManager.getPlayerBusinesses(player);
        
        if (playerBusinesses.isEmpty()) {
            inventory.setItem(22, createMenuItem(Material.BARRIER, 
                "§cNo Businesses", 
                Arrays.asList("§7You don't own any businesses yet!", "§7Create one to see revenue data.")));
        } else {
            double totalRevenue = 0;
            double totalExpenses = 0;
            
            for (int i = 0; i < Math.min(playerBusinesses.size(), 21); i++) {
                Business business = playerBusinesses.get(i);
                double revenue = business.getTotalRevenue();
                double expenses = business.getTotalExpenses();
                totalRevenue += revenue;
                totalExpenses += expenses;
                
                List<String> lore = Arrays.asList(
                    "§7Revenue: §a$" + String.format("%.2f", revenue),
                    "§7Expenses: §c$" + String.format("%.2f", expenses),
                    "§7Profit: " + (revenue - expenses >= 0 ? "§a" : "§c") + "$" + String.format("%.2f", revenue - expenses),
                    "§7Revenue Model: §e" + business.getRevenueModel().getDisplayName(),
                    "",
                    "§eClick for detailed breakdown"
                );
                
                inventory.setItem(i, createMenuItem(getBusinessMaterial(business.getType()), 
                    "§6" + business.getName(), lore));
            }
            
            // Summary item
            inventory.setItem(40, createMenuItem(Material.GOLD_INGOT, 
                "§6Total Summary", 
                Arrays.asList(
                    "§7Total Revenue: §a$" + String.format("%.2f", totalRevenue),
                    "§7Total Expenses: §c$" + String.format("%.2f", totalExpenses),
                    "§7Net Profit: " + (totalRevenue - totalExpenses >= 0 ? "§a" : "§c") + "$" + String.format("%.2f", totalRevenue - totalExpenses)
                )));
        }
        
        // Back button
        inventory.setItem(44, createMenuItem(Material.ARROW, "§cBack to Main Menu", Arrays.asList("§7Return to main menu")));
        
        fillEmptySlots(inventory, Material.GRAY_STAINED_GLASS_PANE);
        player.openInventory(inventory);
        
        GUISession session = new GUISession(player.getUniqueId(), GUIType.REVENUE_OVERVIEW);
        activeSessions.put(player.getUniqueId(), session);
    }
    
    /**
     * Open business locations menu
     */
    private void openLocationsMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(new BusinessGUIHolder(GUIType.LOCATIONS_MENU), 45, "§9Business Locations");
        
        List<Business> playerBusinesses = businessManager.getPlayerBusinesses(player);
        
        if (playerBusinesses.isEmpty()) {
            inventory.setItem(22, createMenuItem(Material.BARRIER, 
                "§cNo Businesses", 
                Arrays.asList("§7You don't own any businesses yet!", "§7Create one to manage locations.")));
        } else {
            for (int i = 0; i < Math.min(playerBusinesses.size(), 21); i++) {
                Business business = playerBusinesses.get(i);
                List<BusinessLocation> locations = business.getLocations();
                
                List<String> lore = new ArrayList<>();
                lore.add("§7Locations: §b" + locations.size());
                
                if (locations.isEmpty()) {
                    lore.add("§7No locations set");
                } else {
                    for (BusinessLocation location : locations.subList(0, Math.min(3, locations.size()))) {
                        lore.add("§8• §f" + location.getType().getDisplayName());
                    }
                    if (locations.size() > 3) {
                        lore.add("§8• §7... and " + (locations.size() - 3) + " more");
                    }
                }
                
                lore.add("");
                lore.add("§eClick to manage locations");
                
                inventory.setItem(i, createMenuItem(Material.COMPASS, 
                    "§9" + business.getName(), lore));
            }
        }
        
        // Add location button
        inventory.setItem(40, createMenuItem(Material.EMERALD, 
            "§aAdd New Location", 
            Arrays.asList("§7Create a new business location", "§7Requires WorldGuard integration", "", "§eClick to add location")));
        
        // Back button
        inventory.setItem(44, createMenuItem(Material.ARROW, "§cBack to Main Menu", Arrays.asList("§7Return to main menu")));
        
        fillEmptySlots(inventory, Material.BLUE_STAINED_GLASS_PANE);
        player.openInventory(inventory);
        
        GUISession session = new GUISession(player.getUniqueId(), GUIType.LOCATIONS_MENU);
        activeSessions.put(player.getUniqueId(), session);
    }
    
    /**
     * Open processing chains menu
     */
    private void openProcessingChains(Player player) {
        Inventory inventory = Bukkit.createInventory(new BusinessGUIHolder(GUIType.PROCESSING_CHAINS), 45, "§aResource Processing");
        
        List<Business> playerBusinesses = businessManager.getPlayerBusinesses(player);
        
        if (playerBusinesses.isEmpty()) {
            inventory.setItem(22, createMenuItem(Material.BARRIER, 
                "§cNo Businesses", 
                Arrays.asList("§7You don't own any businesses yet!", "§7Create one to set up processing.")));
        } else {
            for (int i = 0; i < Math.min(playerBusinesses.size(), 21); i++) {
                Business business = playerBusinesses.get(i);
                List<ResourceProcessingChain> chains = business.getProcessingChains();
                
                List<String> lore = new ArrayList<>();
                lore.add("§7Processing Chains: §a" + chains.size());
                
                if (chains.isEmpty()) {
                    lore.add("§7No processing chains active");
                } else {
                    for (ResourceProcessingChain chain : chains.subList(0, Math.min(3, chains.size()))) {
                        lore.add("§8• §f" + chain.getType().getDisplayName());
                        lore.add("§8  Status: " + (chain.isActive() ? "§aActive" : "§cInactive"));
                    }
                    if (chains.size() > 3) {
                        lore.add("§8• §7... and " + (chains.size() - 3) + " more");
                    }
                }
                
                lore.add("");
                lore.add("§eClick to manage processing");
                
                inventory.setItem(i, createMenuItem(Material.CRAFTING_TABLE, 
                    "§a" + business.getName(), lore));
            }
        }
        
        // Add processing chain button
        inventory.setItem(40, createMenuItem(Material.EMERALD, 
            "§aAdd Processing Chain", 
            Arrays.asList("§7Set up automated resource processing", "§7Generate passive income", "", "§eClick to add chain")));
        
        // Back button
        inventory.setItem(44, createMenuItem(Material.ARROW, "§cBack to Main Menu", Arrays.asList("§7Return to main menu")));
        
        fillEmptySlots(inventory, Material.GREEN_STAINED_GLASS_PANE);
        player.openInventory(inventory);
        
        GUISession session = new GUISession(player.getUniqueId(), GUIType.PROCESSING_CHAINS);
        activeSessions.put(player.getUniqueId(), session);
    }
    
    /**
     * Open construction contracts menu
     */
    private void openConstructionContracts(Player player) {
        Inventory inventory = Bukkit.createInventory(new BusinessGUIHolder(GUIType.CONSTRUCTION_CONTRACTS), 45, "§cConstruction Contracts");
        
        List<Business> playerBusinesses = businessManager.getPlayerBusinesses(player);
        
        if (playerBusinesses.isEmpty()) {
            inventory.setItem(22, createMenuItem(Material.BARRIER, 
                "§cNo Businesses", 
                Arrays.asList("§7You don't own any businesses yet!", "§7Create one to manage contracts.")));
        } else {
            for (int i = 0; i < Math.min(playerBusinesses.size(), 21); i++) {
                Business business = playerBusinesses.get(i);
                List<ConstructionContract> contracts = business.getConstructionContracts();
                
                List<String> lore = new ArrayList<>();
                lore.add("§7Active Contracts: §c" + contracts.size());
                
                if (contracts.isEmpty()) {
                    lore.add("§7No active contracts");
                } else {
                    for (ConstructionContract contract : contracts.subList(0, Math.min(3, contracts.size()))) {
                        lore.add("§8• §f" + contract.getType().getDisplayName());
                        lore.add("§8  Status: §e" + contract.getStatus().getDisplayName());
                        lore.add("§8  Value: §6$" + String.format("%.2f", contract.getTotalCost()));
                    }
                    if (contracts.size() > 3) {
                        lore.add("§8• §7... and " + (contracts.size() - 3) + " more");
                    }
                }
                
                lore.add("");
                lore.add("§eClick to manage contracts");
                
                inventory.setItem(i, createMenuItem(Material.BRICKS, 
                    "§c" + business.getName(), lore));
            }
        }
        
        // Add contract button
        inventory.setItem(40, createMenuItem(Material.EMERALD, 
            "§aNew Contract", 
            Arrays.asList("§7Accept a new construction contract", "§7Earn money by building", "", "§eClick to browse contracts")));
        
        // Back button
        inventory.setItem(44, createMenuItem(Material.ARROW, "§cBack to Main Menu", Arrays.asList("§7Return to main menu")));
        
        fillEmptySlots(inventory, Material.RED_STAINED_GLASS_PANE);
        player.openInventory(inventory);
        
        GUISession session = new GUISession(player.getUniqueId(), GUIType.CONSTRUCTION_CONTRACTS);
        activeSessions.put(player.getUniqueId(), session);
    }
    
    /**
     * Open employee management menu
     */
    private void openEmployeeManagement(Player player) {
        Inventory inventory = Bukkit.createInventory(new BusinessGUIHolder(GUIType.EMPLOYEE_MANAGEMENT), 45, "§dEmployee Management");
        
        List<Business> playerBusinesses = businessManager.getPlayerBusinesses(player);
        
        if (playerBusinesses.isEmpty()) {
            inventory.setItem(22, createMenuItem(Material.BARRIER, 
                "§cNo Businesses", 
                Arrays.asList("§7You don't own any businesses yet!", "§7Create one to hire employees.")));
        } else {
            for (int i = 0; i < Math.min(playerBusinesses.size(), 21); i++) {
                Business business = playerBusinesses.get(i);
                List<BusinessEmployee> employees = business.getBusinessEmployees();
                
                List<String> lore = new ArrayList<>();
                lore.add("§7Employees: §d" + employees.size() + "/" + business.getMaxEmployees());
                
                if (employees.isEmpty()) {
                    lore.add("§7No employees hired");
                } else {
                    for (BusinessEmployee employee : employees.subList(0, Math.min(3, employees.size()))) {
                        lore.add("§8• §f" + employee.getPlayerName());
                        lore.add("§8  Position: §e" + employee.getPosition());
                        lore.add("§8  Salary: §6$" + String.format("%.2f", employee.getSalary()));
                    }
                    if (employees.size() > 3) {
                        lore.add("§8• §7... and " + (employees.size() - 3) + " more");
                    }
                }
                
                lore.add("");
                lore.add("§eClick to manage employees");
                
                inventory.setItem(i, createMenuItem(Material.PLAYER_HEAD, 
                    "§d" + business.getName(), lore));
            }
        }
        
        // Hire employee button
        inventory.setItem(40, createMenuItem(Material.EMERALD, 
            "§aHire Employee", 
            Arrays.asList("§7Hire a new employee", "§7Use /business hire command", "", "§eClick for instructions")));
        
        // Back button
        inventory.setItem(44, createMenuItem(Material.ARROW, "§cBack to Main Menu", Arrays.asList("§7Return to main menu")));
        
        fillEmptySlots(inventory, Material.PURPLE_STAINED_GLASS_PANE);
        player.openInventory(inventory);
        
        GUISession session = new GUISession(player.getUniqueId(), GUIType.EMPLOYEE_MANAGEMENT);
        activeSessions.put(player.getUniqueId(), session);
    }
    
    /**
     * Open create business menu
     */
    private void openCreateBusiness(Player player) {
        Inventory inventory = Bukkit.createInventory(new BusinessGUIHolder(GUIType.CREATE_BUSINESS), 45, "§eCreate New Business");
        
        // Business type options
        String[] businessTypes = {"restaurant", "shop", "factory", "farm", "construction", "mining", "technology", "transportation"};
        Material[] materials = {Material.COOKED_BEEF, Material.CHEST, Material.CRAFTING_TABLE, Material.WHEAT, 
                               Material.BRICKS, Material.DIAMOND_PICKAXE, Material.REDSTONE, Material.MINECART};
        
        for (int i = 0; i < businessTypes.length; i++) {
            String type = businessTypes[i];
            Material material = materials[i];
            
            List<String> lore = Arrays.asList(
                "§7Type: §e" + type.substring(0, 1).toUpperCase() + type.substring(1),
                "§7Creation Cost: §6$" + plugin.getConfig().getDouble("business.creation_cost", 5000),
                "§7Max Employees: §b" + plugin.getConfig().getInt("business.types." + type + ".max_employees", 5),
                "§7Daily Upkeep: §c$" + plugin.getConfig().getDouble("business.types." + type + ".daily_upkeep", 100),
                "",
                "§eClick to create this business type",
                "§7Use: /business create <name> " + type
            );
            
            inventory.setItem(i + 10, createMenuItem(material, "§e" + type.substring(0, 1).toUpperCase() + type.substring(1) + " Business", lore));
        }
        
        // Instructions
        inventory.setItem(40, createMenuItem(Material.BOOK, 
            "§6Instructions", 
            Arrays.asList(
                "§7To create a business:",
                "§f/business create <name> <type>",
                "",
                "§7Example:",
                "§f/business create 'My Shop' shop",
                "",
                "§7Available types: restaurant, shop,",
                "§7factory, farm, construction, mining,",
                "§7technology, transportation"
            )));
        
        // Back button
        inventory.setItem(44, createMenuItem(Material.ARROW, "§cBack to Main Menu", Arrays.asList("§7Return to main menu")));
        
        fillEmptySlots(inventory, Material.YELLOW_STAINED_GLASS_PANE);
        player.openInventory(inventory);
        
        GUISession session = new GUISession(player.getUniqueId(), GUIType.CREATE_BUSINESS);
        activeSessions.put(player.getUniqueId(), session);
    }
    
    /**
     * Open employee management GUI for a specific business
     */
    private void openEmployeeManagementGUI(Player player, Business business) {
        Inventory inventory = Bukkit.createInventory(new BusinessGUIHolder(GUIType.EMPLOYEE_MANAGEMENT), 45, "§9" + business.getName() + " - Employees");
        
        List<BusinessEmployee> employees = business.getBusinessEmployees();
        
        if (employees.isEmpty()) {
            inventory.setItem(22, createMenuItem(Material.BARRIER, 
                "§cNo Employees", 
                Arrays.asList("§7This business has no employees yet!", "§7Use /business hire to add employees.")));
        } else {
            for (int i = 0; i < Math.min(employees.size(), 21); i++) {
                BusinessEmployee employee = employees.get(i);
                
                List<String> lore = Arrays.asList(
                    "§7Position: §e" + employee.getPosition(),
                    "§7Salary: §a$" + String.format("%.2f", employee.getSalary()) + "/day",
                    "§7Status: " + (employee.isActive() ? "§aActive" : "§cInactive"),
                    "§7Hired: §b" + new java.util.Date(employee.getHiredAt()).toString(),
                    "",
                    "§eClick for employee details"
                );
                
                inventory.setItem(i, createMenuItem(Material.PLAYER_HEAD, 
                    "§6Employee: " + employee.getPlayerName(), lore));
            }
        }
        
        // Management options
        inventory.setItem(36, createMenuItem(Material.EMERALD, "§aAdd Employee", 
            Arrays.asList("§7Use command to hire:", "§f/business hire <player> <position> <salary>")));
        inventory.setItem(37, createMenuItem(Material.WRITABLE_BOOK, "§eManage Positions", 
            Arrays.asList("§7View and edit business positions", "§eClick to manage")));
        inventory.setItem(38, createMenuItem(Material.GOLD_INGOT, "§6Payroll Summary", 
            Arrays.asList("§7Total Daily Payroll: §a$" + 
                String.format("%.2f", employees.stream().mapToDouble(BusinessEmployee::getSalary).sum()))));
        
        // Back button
        inventory.setItem(44, createMenuItem(Material.ARROW, "§cBack to Business Menu", 
            Arrays.asList("§7Return to business menu")));
        
        fillEmptySlots(inventory, Material.BLUE_STAINED_GLASS_PANE);
        player.openInventory(inventory);
        
        GUISession session = new GUISession(player.getUniqueId(), GUIType.EMPLOYEE_MANAGEMENT);
        session.setCurrentBusiness(business);
        activeSessions.put(player.getUniqueId(), session);
    }
    
    /**
     * Open revenue overview GUI for a specific business
     */
    private void openRevenueOverviewGUI(Player player, Business business) {
        Inventory inventory = Bukkit.createInventory(new BusinessGUIHolder(GUIType.REVENUE_OVERVIEW), 45, "§6" + business.getName() + " - Revenue");
        
        // Get revenue data
        double dailyRevenue = business.getDailyRevenue();
        double weeklyRevenue = business.getWeeklyRevenue();
        double monthlyRevenue = business.getMonthlyRevenue();
        double totalExpenses = business.getTotalExpenses();
        double netProfit = dailyRevenue - totalExpenses;
        
        // Revenue summary
        inventory.setItem(10, createMenuItem(Material.GOLD_INGOT, "§6Daily Revenue", 
            Arrays.asList("§7Amount: §a$" + String.format("%.2f", dailyRevenue),
                         "§7Trend: " + (dailyRevenue > 0 ? "§a↑ Positive" : "§c↓ Negative"))));
        
        inventory.setItem(11, createMenuItem(Material.GOLD_BLOCK, "§6Weekly Revenue", 
            Arrays.asList("§7Amount: §a$" + String.format("%.2f", weeklyRevenue),
                         "§7Average/Day: §e$" + String.format("%.2f", weeklyRevenue / 7))));
        
        inventory.setItem(12, createMenuItem(Material.DIAMOND, "§6Monthly Revenue", 
            Arrays.asList("§7Amount: §a$" + String.format("%.2f", monthlyRevenue),
                         "§7Average/Day: §e$" + String.format("%.2f", monthlyRevenue / 30))));
        
        // Expense breakdown
        inventory.setItem(14, createMenuItem(Material.REDSTONE, "§cDaily Expenses", 
            Arrays.asList("§7Payroll: §c$" + String.format("%.2f", business.getDailyPayroll()),
                         "§7Operations: §c$" + String.format("%.2f", business.getOperationalCosts()),
                         "§7Total: §c$" + String.format("%.2f", totalExpenses))));
        
        // Net profit
        inventory.setItem(16, createMenuItem(Material.EMERALD, "§aNet Profit", 
            Arrays.asList("§7Daily: " + (netProfit >= 0 ? "§a" : "§c") + "$" + String.format("%.2f", netProfit),
                         "§7Status: " + (netProfit >= 0 ? "§aProfitable" : "§cLoss"))));
        
        // Revenue sources
        inventory.setItem(19, createMenuItem(Material.CHEST, "§eRevenue Sources", 
            Arrays.asList("§7Service Revenue: §a$" + String.format("%.2f", business.getServiceRevenue()),
                         "§7Product Sales: §a$" + String.format("%.2f", business.getProductRevenue()),
                         "§7Contract Revenue: §a$" + String.format("%.2f", business.getContractRevenue()))));
        
        // Back button
        inventory.setItem(44, createMenuItem(Material.ARROW, "§cBack to Business Menu", 
            Arrays.asList("§7Return to business menu")));
        
        fillEmptySlots(inventory, Material.YELLOW_STAINED_GLASS_PANE);
        player.openInventory(inventory);
        
        GUISession session = new GUISession(player.getUniqueId(), GUIType.REVENUE_OVERVIEW);
        session.setCurrentBusiness(business);
        activeSessions.put(player.getUniqueId(), session);
    }
    
    /**
     * Open location management GUI for a specific business
     */
    private void openLocationManagementGUI(Player player, Business business) {
        Inventory inventory = Bukkit.createInventory(new BusinessGUIHolder(GUIType.LOCATIONS_MENU), 45, "§9" + business.getName() + " - Locations");
        
        List<BusinessLocation> locations = business.getLocations();
        
        if (locations.isEmpty()) {
            inventory.setItem(22, createMenuItem(Material.BARRIER, 
                "§cNo Locations", 
                Arrays.asList("§7This business has no locations yet!", "§7Use /business add-location to add locations.")));
        } else {
            for (int i = 0; i < Math.min(locations.size(), 21); i++) {
                BusinessLocation location = locations.get(i);
                
                List<String> lore = Arrays.asList(
                    "§7Type: §e" + location.getType().toString().replace("_", " "),
                    "§7World: §b" + location.getWorldName(),
                    "§7Region: §b" + location.getRegionName(),
                    "§7Efficiency Bonus: §a+" + String.format("%.1f", location.getEfficiencyBonus() * 100) + "%",
                    "§7Monthly Cost: §c$" + String.format("%.2f", location.getMonthlyOperationalCost()),
                    "",
                    "§eClick for location details"
                );
                
                inventory.setItem(i, createMenuItem(getLocationMaterial(location.getType().toString()), 
                    "§6" + location.getType().toString().replace("_", " "), lore));
            }
        }
        
        // Management options
        inventory.setItem(36, createMenuItem(Material.EMERALD, "§aAdd Location", 
            Arrays.asList("§7Use command to add location:", "§f/business add-location <type> <region>")));
        inventory.setItem(37, createMenuItem(Material.MAP, "§eLocation Summary", 
            Arrays.asList("§7Total Locations: §b" + locations.size(),
                         "§7Total Monthly Cost: §c$" + String.format("%.2f", 
                             locations.stream().mapToDouble(BusinessLocation::getMonthlyOperationalCost).sum()))));
        
        // Back button
        inventory.setItem(44, createMenuItem(Material.ARROW, "§cBack to Business Menu", 
            Arrays.asList("§7Return to business menu")));
        
        fillEmptySlots(inventory, Material.GREEN_STAINED_GLASS_PANE);
        player.openInventory(inventory);
        
        GUISession session = new GUISession(player.getUniqueId(), GUIType.LOCATIONS_MENU);
        session.setCurrentBusiness(business);
        activeSessions.put(player.getUniqueId(), session);
    }
    
    /**
     * Open processing chain GUI for a specific business
     */
    private void openProcessingChainGUI(Player player, Business business) {
        Inventory inventory = Bukkit.createInventory(new BusinessGUIHolder(GUIType.PROCESSING_CHAINS), 45, "§a" + business.getName() + " - Processing");
        
        List<ResourceProcessingChain> chains = business.getProcessingChains();
        
        if (chains.isEmpty()) {
            inventory.setItem(22, createMenuItem(Material.BARRIER, 
                "§cNo Processing Chains", 
                Arrays.asList("§7This business has no processing chains yet!", "§7Use /business add-processing to add chains.")));
        } else {
            for (int i = 0; i < Math.min(chains.size(), 21); i++) {
                ResourceProcessingChain chain = chains.get(i);
                
                List<String> lore = Arrays.asList(
                    "§7Type: §e" + chain.getProcessingType().toString().replace("_", " "),
                    "§7Input: §b" + chain.getInputMaterial(),
                    "§7Output: §a" + chain.getOutputMaterial(),
                    "§7Processing Time: §e" + chain.getProcessingTimeMinutes() + " minutes",
                    "§7Profit per Cycle: §a$" + String.format("%.2f", chain.getProfitPerCycle()),
                    "§7Status: " + (chain.isActive() ? "§aActive" : "§cInactive"),
                    "",
                    "§eClick for chain details"
                );
                
                inventory.setItem(i, createMenuItem(getProcessingMaterial(chain.getProcessingType().toString()), 
                    "§6" + chain.getChainName(), lore));
            }
        }
        
        // Management options
        inventory.setItem(36, createMenuItem(Material.EMERALD, "§aAdd Processing Chain", 
            Arrays.asList("§7Use command to add chain:", "§f/business add-processing <type>")));
        inventory.setItem(37, createMenuItem(Material.CLOCK, "§eProcessing Summary", 
            Arrays.asList("§7Total Chains: §b" + chains.size(),
                         "§7Active Chains: §a" + chains.stream().mapToInt(c -> c.isActive() ? 1 : 0).sum(),
                         "§7Total Hourly Profit: §a$" + String.format("%.2f", 
                              chains.stream().mapToDouble(ResourceProcessingChain::getHourlyProfit).sum()))));
        
        // Back button
        inventory.setItem(44, createMenuItem(Material.ARROW, "§cBack to Business Menu", 
            Arrays.asList("§7Return to business menu")));
        
        fillEmptySlots(inventory, Material.LIME_STAINED_GLASS_PANE);
        player.openInventory(inventory);
        
        GUISession session = new GUISession(player.getUniqueId(), GUIType.PROCESSING_CHAINS);
        session.setCurrentBusiness(business);
        activeSessions.put(player.getUniqueId(), session);
    }
    
    /**
     * Open employee details GUI for a specific employee
     */
    private void openEmployeeDetailsGUI(Player player, Business business, String employeeName) {
        Inventory inventory = Bukkit.createInventory(new BusinessGUIHolder(GUIType.EMPLOYEE_MANAGEMENT), 27, "§6Employee: " + employeeName);
        
        BusinessEmployee employee = business.getBusinessEmployees().stream()
            .filter(e -> e.getPlayerName().equals(employeeName))
            .findFirst().orElse(null);
        
        if (employee == null) {
            inventory.setItem(13, createMenuItem(Material.BARRIER, "§cEmployee Not Found", 
                Arrays.asList("§7This employee could not be found.")));
        } else {
            // Employee info
            inventory.setItem(10, createMenuItem(Material.PLAYER_HEAD, "§6" + employee.getPlayerName(), 
                Arrays.asList(
                    "§7Position: §e" + employee.getPosition(),
                    "§7Salary: §a$" + String.format("%.2f", employee.getSalary()) + "/day",
                    "§7Status: " + (employee.isActive() ? "§aActive" : "§cInactive"),
                    "§7Hired: §b" + new java.util.Date(employee.getHiredAt()).toString()
                )));
            
            // Actions
            inventory.setItem(12, createMenuItem(Material.WRITABLE_BOOK, "§eEdit Salary", 
                Arrays.asList("§7Use command:", "§f/business edit-position " + employee.getPosition() + " salary <amount>")));
            inventory.setItem(14, createMenuItem(Material.DIAMOND, "§aPromote Employee", 
                Arrays.asList("§7Use command:", "§f/business promote-employee " + employeeName + " <position>")));
            inventory.setItem(16, createMenuItem(Material.BOOK, "§6Add Note", 
                Arrays.asList("§7Use command:", "§f/business add-note " + employeeName + " <note>")));
        }
        
        // Back button
        inventory.setItem(26, createMenuItem(Material.ARROW, "§cBack to Employees", 
            Arrays.asList("§7Return to employee list")));
        
        fillEmptySlots(inventory, Material.GRAY_STAINED_GLASS_PANE);
        player.openInventory(inventory);
        
        GUISession session = new GUISession(player.getUniqueId(), GUIType.EMPLOYEE_MANAGEMENT);
        session.setCurrentBusiness(business);
        activeSessions.put(player.getUniqueId(), session);
    }
    
    /**
     * Open position management GUI for a specific business
     */
    private void openPositionManagementGUI(Player player, Business business) {
        Inventory inventory = Bukkit.createInventory(new BusinessGUIHolder(GUIType.EMPLOYEE_MANAGEMENT), 45, "§e" + business.getName() + " - Positions");
        
        List<BusinessPosition> positions = business.getPositions();
        
        if (positions.isEmpty()) {
            inventory.setItem(22, createMenuItem(Material.BARRIER, 
                "§cNo Positions", 
                Arrays.asList("§7This business has no positions yet!", "§7Use /business create-position to add positions.")));
        } else {
            for (int i = 0; i < Math.min(positions.size(), 21); i++) {
                BusinessPosition position = positions.get(i);
                
                List<String> lore = Arrays.asList(
                    "§7Title: §e" + position.getTitle(),
                    "§7Base Salary: §a$" + String.format("%.2f", position.getBaseSalary()),
                    "§7Max Employees: §b" + position.getMaxEmployees(),
                    "§7Current Employees: §b" + business.getEmployeesInPosition(position.getTitle()).size(),
                    "§7Status: " + (position.isActive() ? "§aActive" : "§cInactive"),
                    "",
                    "§eClick for position details"
                );
                
                inventory.setItem(i, createMenuItem(Material.WRITABLE_BOOK, 
                    "§6" + position.getTitle(), lore));
            }
        }
        
        // Management options
        inventory.setItem(36, createMenuItem(Material.EMERALD, "§aCreate Position", 
            Arrays.asList("§7Use command to create position:", "§f/business create-position <title> <salary> <max>")));
        inventory.setItem(37, createMenuItem(Material.BOOK, "§ePosition Summary", 
            Arrays.asList("§7Total Positions: §b" + positions.size(),
                         "§7Active Positions: §a" + positions.stream().mapToInt(p -> p.isActive() ? 1 : 0).sum())));
        
        // Back button
        inventory.setItem(44, createMenuItem(Material.ARROW, "§cBack to Employees", 
            Arrays.asList("§7Return to employee management")));
        
        fillEmptySlots(inventory, Material.ORANGE_STAINED_GLASS_PANE);
        player.openInventory(inventory);
        
        GUISession session = new GUISession(player.getUniqueId(), GUIType.EMPLOYEE_MANAGEMENT);
        session.setCurrentBusiness(business);
        activeSessions.put(player.getUniqueId(), session);
    }
    
    /**
     * Get material for location type
     */
    private Material getLocationMaterial(String locationType) {
        switch (locationType.toUpperCase()) {
            case "HEADQUARTERS": return Material.BEACON;
            case "BRANCH_OFFICE": return Material.LECTERN;
            case "WAREHOUSE": return Material.CHEST;
            case "SHOP_FRONT": return Material.EMERALD;
            case "FACTORY": return Material.FURNACE;
            case "FARM": return Material.WHEAT;
            case "MINE": return Material.DIAMOND_PICKAXE;
            case "CONSTRUCTION_SITE": return Material.BRICKS;
            case "SERVICE_CENTER": return Material.ANVIL;
            case "RESEARCH_LAB": return Material.BREWING_STAND;
            default: return Material.STONE;
        }
    }
    
    /**
     * Get material for processing type
     */
    private Material getProcessingMaterial(String processingType) {
        switch (processingType.toUpperCase()) {
            case "MINING_REFINERY": return Material.DIAMOND_ORE;
            case "FOOD_PROCESSING": return Material.COOKED_BEEF;
            case "LUMBER_MILL": return Material.OAK_LOG;
            case "TEXTILE_FACTORY": return Material.WHITE_WOOL;
            case "SMELTING": return Material.FURNACE;
            case "BREWING": return Material.BREWING_STAND;
            case "ENCHANTING": return Material.ENCHANTING_TABLE;
            case "TOOL_MANUFACTURING": return Material.DIAMOND_SWORD;
            case "ARMOR_SMITHING": return Material.DIAMOND_CHESTPLATE;
            case "CONSTRUCTION_MATERIALS": return Material.BRICKS;
            case "AGRICULTURAL_PROCESSING": return Material.WHEAT;
            case "REDSTONE_ENGINEERING": return Material.REDSTONE;
            default: return Material.CRAFTING_TABLE;
        }
    }
    
}
