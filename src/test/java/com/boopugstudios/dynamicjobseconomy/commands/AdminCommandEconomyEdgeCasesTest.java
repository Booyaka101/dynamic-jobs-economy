package com.boopugstudios.dynamicjobseconomy.commands;

import com.boopugstudios.dynamicjobseconomy.DynamicJobsEconomy;
import com.boopugstudios.dynamicjobseconomy.economy.EconomyManager;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdminCommandEconomyEdgeCasesTest {

    private static class TestableAdminCommand extends AdminCommand {
        private final Map<String, Player> byName;
        private final OfflinePlayer[] offline;
        private long now = System.currentTimeMillis();
        TestableAdminCommand(DynamicJobsEconomy plugin, Map<String, Player> byName, OfflinePlayer[] offline) {
            super(plugin);
            this.byName = byName;
            this.offline = offline;
        }
        protected Player getPlayerByName(String name) { return byName.get(name); }
        protected OfflinePlayer[] getOfflinePlayersArray() { return offline; }
        protected Collection<? extends Player> getOnlinePlayers() { return byName.values(); }
        protected long nowMillis() { return now; }
        void setNow(long n) { this.now = n; }
    }

    private static class MsgCollector {
        final List<String> list = new ArrayList<>();
        Player asPlayerWithPerms() {
            Player p = mock(Player.class);
            when(p.getUniqueId()).thenReturn(UUID.randomUUID());
            when(p.getName()).thenReturn("Admin");
            when(p.hasPermission(anyString())).thenReturn(true);
            doAnswer(inv -> { list.add(inv.getArgument(0)); return null; }).when(p).sendMessage(anyString());
            return p;
        }
        Player asPlayerNoPerms() {
            Player p = mock(Player.class);
            when(p.getUniqueId()).thenReturn(UUID.randomUUID());
            when(p.getName()).thenReturn("NoPerm");
            when(p.hasPermission(anyString())).thenReturn(false);
            doAnswer(inv -> { list.add(inv.getArgument(0)); return null; }).when(p).sendMessage(anyString());
            return p;
        }
        CommandSender asConsole() {
            CommandSender s = mock(CommandSender.class);
            when(s.hasPermission(anyString())).thenReturn(true);
            doAnswer(inv -> { list.add(inv.getArgument(0)); return null; }).when(s).sendMessage(anyString());
            return s;
        }
    }

    private static class Players {
        final Player online;
        final OfflinePlayer offline;
        final Map<String, Player> byName;
        final OfflinePlayer[] offlineArr;
        Players() {
            online = mock(Player.class);
            when(online.getUniqueId()).thenReturn(UUID.randomUUID());
            when(online.getName()).thenReturn("Alice");

            offline = mock(OfflinePlayer.class);
            when(offline.getUniqueId()).thenReturn(UUID.randomUUID());
            when(offline.getName()).thenReturn("Bob");
            when(offline.hasPlayedBefore()).thenReturn(true);

            byName = new HashMap<>();
            byName.put("Alice", online);
            offlineArr = new OfflinePlayer[]{ offline };
        }
    }

    private DynamicJobsEconomy setupPlugin(EconomyManager econ, File dataDir) {
        DynamicJobsEconomy plugin = mock(DynamicJobsEconomy.class);
        FileConfiguration cfg = mock(FileConfiguration.class);
        when(plugin.getConfig()).thenReturn(cfg);
        // Return the provided default prefix
        when(cfg.getString(eq("messages.prefix"), anyString())).thenAnswer(inv -> inv.getArgument(1));
        // New configurable confirmation settings: make getters return their provided defaults unless overridden per test
        when(cfg.getDouble(eq("economy.admin_confirmation.threshold"), anyDouble())).thenAnswer(inv -> inv.getArgument(1));
        when(cfg.getInt(eq("economy.admin_confirmation.expiry_seconds"), anyInt())).thenAnswer(inv -> inv.getArgument(1));
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        when(plugin.getEconomyManager()).thenReturn(econ);
        when(plugin.getDataFolder()).thenReturn(dataDir);
        return plugin;
    }

    private File newTempDir() {
        File dir = new File(System.getProperty("java.io.tmpdir"), "dje-econ-tests-" + System.nanoTime());
        assertTrue(dir.mkdirs() || dir.exists());
        return dir;
    }

    @Test
    void invalid_action_reports_error() {
        Players ppl = new Players();
        EconomyManager econ = mock(EconomyManager.class);
        DynamicJobsEconomy plugin = setupPlugin(econ, newTempDir());
        AdminCommand admin = new TestableAdminCommand(plugin, ppl.byName, ppl.offlineArr);

        MsgCollector out = new MsgCollector();
        Player sender = out.asPlayerWithPerms();

        boolean result = admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"economy", "foo", "Alice", "100"});
        assertTrue(result);
        assertTrue(out.list.stream().anyMatch(m -> m.toLowerCase().contains("invalid") && m.toLowerCase().contains("action")));
        // should not send generic failure after invalid action (early return)
        assertFalse(out.list.stream().anyMatch(m -> m.contains("Failed to execute economy command")));
    }

    @Test
    void non_numeric_amount_reports_error() {
        Players ppl = new Players();
        EconomyManager econ = mock(EconomyManager.class);
        DynamicJobsEconomy plugin = setupPlugin(econ, newTempDir());
        AdminCommand admin = new TestableAdminCommand(plugin, ppl.byName, ppl.offlineArr);

        MsgCollector out = new MsgCollector();
        Player sender = out.asPlayerWithPerms();

        assertTrue(admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"economy", "give", "Alice", "abc"}));
        assertTrue(out.list.stream().anyMatch(m -> m.toLowerCase().contains("invalid") && m.toLowerCase().contains("amount")));
    }

    @Test
    void negative_amount_reports_error() {
        Players ppl = new Players();
        EconomyManager econ = mock(EconomyManager.class);
        DynamicJobsEconomy plugin = setupPlugin(econ, newTempDir());
        AdminCommand admin = new TestableAdminCommand(plugin, ppl.byName, ppl.offlineArr);

        MsgCollector out = new MsgCollector();
        Player sender = out.asPlayerWithPerms();

        assertTrue(admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"economy", "give", "Alice", "-5"}));
        assertTrue(out.list.stream().anyMatch(m -> m.contains("Amount cannot be negative")));
    }

    @Test
    void amount_too_large_reports_error() {
        Players ppl = new Players();
        EconomyManager econ = mock(EconomyManager.class);
        DynamicJobsEconomy plugin = setupPlugin(econ, newTempDir());
        AdminCommand admin = new TestableAdminCommand(plugin, ppl.byName, ppl.offlineArr);

        MsgCollector out = new MsgCollector();
        Player sender = out.asPlayerWithPerms();

        assertTrue(admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"economy", "give", "Alice", "1000000001"}));
        assertTrue(out.list.stream().anyMatch(m -> m.toLowerCase().contains("too large") || m.toLowerCase().contains("maximum")));
    }

    @Test
    void take_insufficient_funds_reports_message_and_no_withdraw() {
        Players ppl = new Players();
        EconomyManager econ = mock(EconomyManager.class);
        when(econ.getBalance(ppl.online)).thenReturn(50.0);
        when(econ.has(ppl.online, 100.0)).thenReturn(false);
        DynamicJobsEconomy plugin = setupPlugin(econ, newTempDir());
        AdminCommand admin = new TestableAdminCommand(plugin, ppl.byName, ppl.offlineArr);

        MsgCollector out = new MsgCollector();
        Player sender = out.asPlayerWithPerms();

        assertTrue(admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"economy", "take", "Alice", "100"}));
        assertTrue(out.list.stream().anyMatch(m -> m.contains("only has $50")));
        verify(econ, never()).withdraw(eq(ppl.online), anyDouble());
    }

    @Test
    void offline_deposit_uses_depositPlayer_and_logs_history_and_offline_note() throws Exception {
        Players ppl = new Players();
        EconomyManager econ = mock(EconomyManager.class);
        when(econ.depositPlayer(eq(ppl.offline), eq(50.0))).thenReturn(true);
        DynamicJobsEconomy plugin = setupPlugin(econ, newTempDir());
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.emptyMap(), new OfflinePlayer[]{ppl.offline});

        MsgCollector out = new MsgCollector();
        Player sender = out.asPlayerWithPerms();

        assertTrue(admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"economy", "give", "Bob", "50"}));

        // Offline note
        assertTrue(out.list.stream().anyMatch(m -> m.contains("offline")));
        assertTrue(out.list.stream().anyMatch(m -> m.contains("Processing")));
        // Success message
        assertTrue(out.list.stream().anyMatch(m -> m.contains("Gave $50")));
        assertTrue(out.list.stream().anyMatch(m -> m.contains("Bob")));

        // History written
        File log = new File(plugin.getDataFolder(), "admin-economy-history.log");
        assertTrue(log.exists());
        String content = Files.readString(log.toPath(), StandardCharsets.UTF_8);
        assertTrue(content.contains("|GIVE|Bob|50.00"));

        verify(econ, times(1)).depositPlayer(eq(ppl.offline), eq(50.0));
    }

    @Test
    void large_amount_requires_confirm_then_confirm_applies() {
        Players ppl = new Players();
        EconomyManager econ = mock(EconomyManager.class);
        when(econ.deposit(eq(ppl.online), eq(200000.0))).thenReturn(true);
        DynamicJobsEconomy plugin = setupPlugin(econ, newTempDir());
        AdminCommand admin = new TestableAdminCommand(plugin, ppl.byName, ppl.offlineArr);

        MsgCollector out = new MsgCollector();
        Player sender = out.asPlayerWithPerms();

        // First attempt -> prompts for confirm
        assertTrue(admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"economy", "give", "Alice", "200000"}));
        assertTrue(out.list.stream().anyMatch(m -> m.toLowerCase().contains("confirm")));
        verify(econ, never()).deposit(any(Player.class), anyDouble());

        out.list.clear();
        // Confirm -> should apply
        assertTrue(admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"confirm"}));
        assertTrue(out.list.stream().anyMatch(m -> m.contains("Gave $200,000.00") || m.contains("Gave $200000.00")));
        verify(econ, times(1)).deposit(eq(ppl.online), eq(200000.0));
    }

    @Test
    void confirm_without_pending_reports_message() {
        Players ppl = new Players();
        EconomyManager econ = mock(EconomyManager.class);
        DynamicJobsEconomy plugin = setupPlugin(econ, newTempDir());
        AdminCommand admin = new TestableAdminCommand(plugin, ppl.byName, ppl.offlineArr);

        MsgCollector out = new MsgCollector();
        Player sender = out.asPlayerWithPerms();

        assertTrue(admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"confirm"}));
        assertTrue(out.list.stream().anyMatch(m -> m.contains("No pending") && m.contains("confirm")));
    }

    @Test
    void confirm_requires_permission() {
        Players ppl = new Players();
        EconomyManager econ = mock(EconomyManager.class);
        DynamicJobsEconomy plugin = setupPlugin(econ, newTempDir());
        AdminCommand admin = new TestableAdminCommand(plugin, ppl.byName, ppl.offlineArr);

        MsgCollector out = new MsgCollector();
        Player sender = out.asPlayerNoPerms();

        assertTrue(admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"confirm"}));
        assertTrue(out.list.stream().anyMatch(m -> m.toLowerCase().contains("permission")));
    }

    @Test
    void confirm_only_players_can_use() {
        Players ppl = new Players();
        EconomyManager econ = mock(EconomyManager.class);
        DynamicJobsEconomy plugin = setupPlugin(econ, newTempDir());
        AdminCommand admin = new TestableAdminCommand(plugin, ppl.byName, ppl.offlineArr);

        MsgCollector out = new MsgCollector();
        CommandSender console = out.asConsole();

        assertTrue(admin.onCommand(console, mock(Command.class), "djeconomy", new String[]{"confirm"}));
        assertTrue(out.list.stream().anyMatch(m -> m.contains("Only players") && m.toLowerCase().contains("confirm")));
    }

    @Test
    void small_give_executes_immediately_online() {
        Players ppl = new Players();
        EconomyManager econ = mock(EconomyManager.class);
        when(econ.deposit(eq(ppl.online), eq(500.0))).thenReturn(true);
        DynamicJobsEconomy plugin = setupPlugin(econ, newTempDir());
        AdminCommand admin = new TestableAdminCommand(plugin, ppl.byName, ppl.offlineArr);

        MsgCollector out = new MsgCollector();
        Player sender = out.asPlayerWithPerms();

        assertTrue(admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"economy", "give", "Alice", "500"}));
        assertTrue(out.list.stream().anyMatch(m -> m.contains("Gave $500.00") || m.contains("Gave $500")));
        // Should not prompt for confirm on small amount
        assertFalse(out.list.stream().anyMatch(m -> m.toLowerCase().contains("confirm")));
        verify(econ, times(1)).deposit(eq(ppl.online), eq(500.0));
    }

    @Test
    void take_executes_when_sufficient_balance_online() {
        Players ppl = new Players();
        EconomyManager econ = mock(EconomyManager.class);
        when(econ.getBalance(ppl.online)).thenReturn(200.0);
        when(econ.withdraw(eq(ppl.online), eq(100.0))).thenReturn(true);
        DynamicJobsEconomy plugin = setupPlugin(econ, newTempDir());
        AdminCommand admin = new TestableAdminCommand(plugin, ppl.byName, ppl.offlineArr);

        MsgCollector out = new MsgCollector();
        Player sender = out.asPlayerWithPerms();

        assertTrue(admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"economy", "take", "Alice", "100"}));
        assertTrue(out.list.stream().anyMatch(m -> m.contains("Took $100.00") || m.contains("Took $100")));
        verify(econ, times(1)).withdraw(eq(ppl.online), eq(100.0));
    }

    @Test
    void take_executes_when_sufficient_balance_offline() {
        Players ppl = new Players();
        EconomyManager econ = mock(EconomyManager.class);
        when(econ.getBalance(ppl.offline)).thenReturn(300.0);
        when(econ.withdraw(eq(ppl.offline), eq(150.0))).thenReturn(true);
        DynamicJobsEconomy plugin = setupPlugin(econ, newTempDir());
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.emptyMap(), new OfflinePlayer[]{ppl.offline});

        MsgCollector out = new MsgCollector();
        Player sender = out.asPlayerWithPerms();

        assertTrue(admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"economy", "take", "Bob", "150"}));
        assertTrue(out.list.stream().anyMatch(m -> m.contains("Took $150.00") || m.contains("Took $150")));
        verify(econ, times(1)).withdraw(eq(ppl.offline), eq(150.0));
    }

    @Test
    void set_executes_online() {
        Players ppl = new Players();
        EconomyManager econ = mock(EconomyManager.class);
        when(econ.getBalance(ppl.online)).thenReturn(250.0);
        when(econ.withdraw(eq(ppl.online), eq(250.0))).thenReturn(true);
        when(econ.deposit(eq(ppl.online), eq(400.0))).thenReturn(true);
        DynamicJobsEconomy plugin = setupPlugin(econ, newTempDir());
        AdminCommand admin = new TestableAdminCommand(plugin, ppl.byName, ppl.offlineArr);

        MsgCollector out = new MsgCollector();
        Player sender = out.asPlayerWithPerms();

        assertTrue(admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"economy", "set", "Alice", "400"}));
        assertTrue(out.list.stream().anyMatch(m -> m.contains("Set Alice's balance to $400.00") || m.contains("Set Alice's balance to $400")));
        verify(econ, times(1)).withdraw(eq(ppl.online), eq(250.0));
        verify(econ, times(1)).deposit(eq(ppl.online), eq(400.0));
    }

    @Test
    void set_executes_offline() {
        Players ppl = new Players();
        EconomyManager econ = mock(EconomyManager.class);
        when(econ.getBalance(ppl.offline)).thenReturn(600.0);
        when(econ.withdraw(eq(ppl.offline), eq(600.0))).thenReturn(true);
        when(econ.depositPlayer(eq(ppl.offline), eq(50.0))).thenReturn(true);
        DynamicJobsEconomy plugin = setupPlugin(econ, newTempDir());
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.emptyMap(), new OfflinePlayer[]{ppl.offline});

        MsgCollector out = new MsgCollector();
        Player sender = out.asPlayerWithPerms();

        assertTrue(admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"economy", "set", "Bob", "50"}));
        assertTrue(out.list.stream().anyMatch(m -> m.contains("Set Bob's balance to $50.00") || m.contains("Set Bob's balance to $50")));
        verify(econ, times(1)).withdraw(eq(ppl.offline), eq(600.0));
        verify(econ, times(1)).depositPlayer(eq(ppl.offline), eq(50.0));
    }

    @Test
    void confirmation_expires_after_30s_and_does_not_apply() {
        Players ppl = new Players();
        EconomyManager econ = mock(EconomyManager.class);
        DynamicJobsEconomy plugin = setupPlugin(econ, newTempDir());
        TestableAdminCommand admin = new TestableAdminCommand(plugin, ppl.byName, ppl.offlineArr);

        MsgCollector out = new MsgCollector();
        Player sender = out.asPlayerWithPerms();

        long t0 = System.currentTimeMillis();
        admin.setNow(t0);
        // Initiate large give -> requires confirm
        assertTrue(admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"economy", "give", "Alice", "100000"}));
        assertTrue(out.list.stream().anyMatch(m -> m.toLowerCase().contains("confirm")));
        verify(econ, never()).deposit(any(Player.class), anyDouble());

        out.list.clear();
        // Advance beyond 30s
        admin.setNow(t0 + 31000);
        assertTrue(admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"confirm"}));
        assertTrue(out.list.stream().anyMatch(m -> m.toLowerCase().contains("expired")));
        // Still no economy action applied
        verify(econ, never()).deposit(any(Player.class), anyDouble());
    }

    @Test
    void custom_threshold_and_expiry_are_applied() {
        Players ppl = new Players();
        EconomyManager econ = mock(EconomyManager.class);
        when(econ.deposit(eq(ppl.online), eq(6000.0))).thenReturn(true);
        DynamicJobsEconomy plugin = setupPlugin(econ, newTempDir());

        // Override config values for this scenario
        FileConfiguration cfg = plugin.getConfig();
        when(cfg.getDouble(eq("economy.admin_confirmation.threshold"), anyDouble())).thenReturn(5000.0);
        when(cfg.getInt(eq("economy.admin_confirmation.expiry_seconds"), anyInt())).thenReturn(5);

        TestableAdminCommand admin = new TestableAdminCommand(plugin, ppl.byName, ppl.offlineArr);

        MsgCollector out = new MsgCollector();
        Player sender = out.asPlayerWithPerms();

        // First attempt over threshold -> prompts with dynamic seconds
        assertTrue(admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"economy", "give", "Alice", "6000"}));
        assertTrue(out.list.stream().anyMatch(m -> m.toLowerCase().contains("expires in 5 seconds")));
        verify(econ, never()).deposit(any(Player.class), anyDouble());

        out.list.clear();
        // Confirm within expiry -> should apply
        assertTrue(admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"confirm"}));
        assertTrue(out.list.stream().anyMatch(m -> m.contains("Gave $6,000.00") || m.contains("Gave $6000.00")));
        verify(econ, times(1)).deposit(eq(ppl.online), eq(6000.0));
    }

    @Test
    void reload_applies_new_threshold_and_expiry_settings() {
        // Arrange two configs with different confirmation settings
        FileConfiguration cfgOld = mock(FileConfiguration.class);
        when(cfgOld.getString(eq("messages.prefix"), anyString())).thenReturn("[OLD] ");
        when(cfgOld.getDouble(eq("economy.admin_confirmation.threshold"), anyDouble())).thenReturn(100000.0);
        when(cfgOld.getInt(eq("economy.admin_confirmation.expiry_seconds"), anyInt())).thenReturn(30);

        FileConfiguration cfgNew = mock(FileConfiguration.class);
        when(cfgNew.getString(eq("messages.prefix"), anyString())).thenReturn("[NEW] ");
        when(cfgNew.getDouble(eq("economy.admin_confirmation.threshold"), anyDouble())).thenReturn(3000.0);
        when(cfgNew.getInt(eq("economy.admin_confirmation.expiry_seconds"), anyInt())).thenReturn(5);

        DynamicJobsEconomy plugin = mock(DynamicJobsEconomy.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        AtomicBoolean reloaded = new AtomicBoolean(false);
        when(plugin.getConfig()).thenAnswer(inv -> reloaded.get() ? cfgNew : cfgOld);
        doAnswer(inv -> { reloaded.set(true); return null; }).when(plugin).reloadConfig();

        EconomyManager econ = mock(EconomyManager.class);
        when(plugin.getEconomyManager()).thenReturn(econ);

        Players ppl = new Players();
        when(econ.deposit(eq(ppl.online), anyDouble())).thenReturn(true);

        TestableAdminCommand admin = new TestableAdminCommand(plugin, ppl.byName, ppl.offlineArr);
        MsgCollector out = new MsgCollector();
        Player sender = out.asPlayerWithPerms();

        // Before reload: threshold high (100000) -> amount 5000 applies immediately, no confirmation
        assertTrue(admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"economy", "give", "Alice", "5000"}));
        assertTrue(out.list.stream().anyMatch(m -> m.contains("Gave $5,000.00") || m.contains("Gave $5000.00")));
        verify(econ, times(1)).deposit(eq(ppl.online), eq(5000.0));

        out.list.clear();

        // Reload config to switch to new settings (threshold 3000, expiry 5s)
        assertTrue(admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"reload"}));

        out.list.clear();
        // After reload: same amount now requires confirmation and mentions 5 seconds
        assertTrue(admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"economy", "give", "Alice", "5000"}));
        assertTrue(out.list.stream().anyMatch(m -> m.toLowerCase().contains("expires in 5 seconds")));
        verify(econ, times(1)).deposit(eq(ppl.online), anyDouble()); // still only the first deposit

        out.list.clear();
        // Confirm -> should apply the pending action
        assertTrue(admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"confirm"}));
        verify(econ, times(2)).deposit(eq(ppl.online), anyDouble());
    }
}
