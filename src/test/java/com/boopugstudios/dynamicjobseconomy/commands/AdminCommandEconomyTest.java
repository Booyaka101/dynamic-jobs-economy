package com.boopugstudios.dynamicjobseconomy.commands;

import com.boopugstudios.dynamicjobseconomy.DynamicJobsEconomy;
import com.boopugstudios.dynamicjobseconomy.economy.EconomyManager;
import com.boopugstudios.dynamicjobseconomy.jobs.JobManager;
import com.boopugstudios.dynamicjobseconomy.jobs.Job;
import com.boopugstudios.dynamicjobseconomy.jobs.PlayerJobData;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdminCommandEconomyTest {

    private static class TestableAdminCommand extends AdminCommand {
        private final Map<String, Player> byName;
        private final OfflinePlayer[] offline;
        TestableAdminCommand(DynamicJobsEconomy plugin, Map<String, Player> byName, OfflinePlayer[] offline) {
            super(plugin);
            this.byName = byName;
            this.offline = offline;
        }
        protected Player getPlayerByName(String name) {
            return byName.get(name);
        }
        protected OfflinePlayer[] getOfflinePlayersArray() {
            return offline;
        }
        protected Collection<? extends Player> getOnlinePlayers() {
            return byName.values();
        }
    }

    private DynamicJobsEconomy setupPluginWithConfigAndEconomy(EconomyManager eco) {
        DynamicJobsEconomy plugin = mock(DynamicJobsEconomy.class);
        FileConfiguration cfg = mock(FileConfiguration.class);
        when(plugin.getConfig()).thenReturn(cfg);
        // Return the provided default value for prefix
        when(cfg.getString(eq("messages.prefix"), anyString())).thenAnswer(inv -> inv.getArgument(1));
        if (eco != null) {
            when(plugin.getEconomyManager()).thenReturn(eco);
        }
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        return plugin;
    }

    private Player mockSenderCollectingMessages(List<String> messages) {
        Player sender = mock(Player.class);
        when(sender.getUniqueId()).thenReturn(UUID.randomUUID());
        when(sender.getName()).thenReturn("Admin");
        when(sender.hasPermission(anyString())).thenReturn(true);
        doAnswer(inv -> { messages.add(inv.getArgument(0)); return null; }).when(sender).sendMessage(anyString());
        return sender;
    }

    private CommandSender mockNonPlayerSender(List<String> messages, boolean hasPermission) {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission(anyString())).thenReturn(hasPermission);
        doAnswer(inv -> { messages.add(inv.getArgument(0)); return null; }).when(sender).sendMessage(anyString());
        return sender;
    }

    @Test
    void permissionGate_deniesNonAdminWithoutSpecificNode() {
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(null);
        AdminCommand admin = new AdminCommand(plugin);

        List<String> messages = new ArrayList<>();
        CommandSender sender = mock(CommandSender.class);
        // Default deny all permissions
        when(sender.hasPermission(anyString())).thenReturn(false);
        doAnswer(inv -> { messages.add(inv.getArgument(0)); return null; }).when(sender).sendMessage(anyString());

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"economy", "give", "Alice", "10"});
        assertTrue(handled);
        assertTrue(messages.stream().anyMatch(m -> m.contains("don't have permission")));
    }

    @Test
    void reloadAllowed_withSystemReloadPermission_withoutAdmin() {
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(null);
        AdminCommand admin = new AdminCommand(plugin);

        List<String> messages = new ArrayList<>();
        CommandSender sender = mock(CommandSender.class);
        // Default deny all permissions
        when(sender.hasPermission(anyString())).thenReturn(false);
        // Allow only reload node
        when(sender.hasPermission("djeconomy.system.reload")).thenReturn(true);
        doAnswer(inv -> { messages.add(inv.getArgument(0)); return null; }).when(sender).sendMessage(anyString());

        boolean handledReload = admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"reload"});
        assertTrue(handledReload);
        assertTrue(messages.stream().anyMatch(m -> m.contains("Configuration reloaded")));

        messages.clear();
        // Another subcommand should still be denied without admin
        boolean handledOther = admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"setlevel", "Alice", "Miner", "3"});
        assertTrue(handledOther);
        assertTrue(messages.stream().anyMatch(m -> m.contains("don't have permission")));
    }

    @Test
    void granularPermissions_allow_whenSpecificNodePresent() {
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(null);
        AdminCommand admin = new AdminCommand(plugin);

        String[] subs = {"economy","setlevel","getlevel","resetlevel","addxp","refreshjobs","invalidatejobs","history"};
        String[] nodes = {"djeconomy.admin.economy","djeconomy.admin.level.set","djeconomy.admin.level.get","djeconomy.admin.level.reset","djeconomy.admin.level.addxp","djeconomy.admin.jobs.refresh","djeconomy.admin.jobs.invalidate","djeconomy.admin.history.view"};

        for (int i = 0; i < subs.length; i++) {
            List<String> messages = new ArrayList<>();
            CommandSender sender = mock(CommandSender.class);
            when(sender.hasPermission(anyString())).thenReturn(false);
            when(sender.hasPermission(nodes[i])).thenReturn(true);
            doAnswer(inv -> { messages.add(inv.getArgument(0)); return null; }).when(sender).sendMessage(anyString());

            boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{subs[i]});
            assertTrue(handled, "Expected handler to process subcommand: " + subs[i]);
            assertFalse(messages.stream().anyMatch(m -> m.contains("don't have permission")),
                    "Should not show permission denial for subcommand: " + subs[i]);
        }
    }

    @Test
    void granularPermissions_deny_withoutSpecificNode() {
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(null);
        AdminCommand admin = new AdminCommand(plugin);

        String[] subs = {"economy","setlevel","getlevel","resetlevel","addxp","refreshjobs","invalidatejobs","history"};

        for (String sub : subs) {
            List<String> messages = new ArrayList<>();
            CommandSender sender = mock(CommandSender.class);
            when(sender.hasPermission(anyString())).thenReturn(false);
            doAnswer(inv -> { messages.add(inv.getArgument(0)); return null; }).when(sender).sendMessage(anyString());

            boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{sub});
            assertTrue(handled, "Expected handler to process subcommand: " + sub);
            assertTrue(messages.stream().anyMatch(m -> m.contains("don't have permission")),
                    "Should show permission denial for subcommand: " + sub);
        }
    }

    @Test
    void economy_invalidAmount_nonNumeric() {
        EconomyManager eco = mock(EconomyManager.class);
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(eco);
        Player target = mock(Player.class);
        when(target.getName()).thenReturn("Alice");
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.singletonMap("Alice", target), new OfflinePlayer[0]);

        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy",
                new String[]{"economy", "give", "Alice", "abc"});
        assertTrue(handled);
        assertTrue(messages.stream().anyMatch(m -> m.contains("Invalid amount")));
        verify(eco, never()).deposit(any(Player.class), anyDouble());
    }

    @Test
    void economy_negativeAmount_rejected() {
        EconomyManager eco = mock(EconomyManager.class);
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(eco);
        Player target = mock(Player.class);
        when(target.getName()).thenReturn("Alice");
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.singletonMap("Alice", target), new OfflinePlayer[0]);

        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy",
                new String[]{"economy", "give", "Alice", "-5"});
        assertTrue(handled);
        assertTrue(messages.stream().anyMatch(m -> m.contains("Amount cannot be negative")));
        verify(eco, never()).deposit(any(Player.class), anyDouble());
    }

    @Test
    void economy_amountTooLarge_rejected() {
        EconomyManager eco = mock(EconomyManager.class);
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(eco);
        Player target = mock(Player.class);
        when(target.getName()).thenReturn("Alice");
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.singletonMap("Alice", target), new OfflinePlayer[0]);

        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy",
                new String[]{"economy", "give", "Alice", "1000000001"});
        assertTrue(handled);
        assertTrue(messages.stream().anyMatch(m -> m.contains("Amount too large")));
        verify(eco, never()).deposit(any(Player.class), anyDouble());
    }

    @Test
    void economy_largeAmount_requiresConfirmation_thenExecutesOnConfirm() {
        EconomyManager eco = mock(EconomyManager.class);
        when(eco.deposit(any(Player.class), anyDouble())).thenReturn(true);
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(eco);
        Player target = mock(Player.class);
        when(target.getName()).thenReturn("Alice");
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.singletonMap("Alice", target), new OfflinePlayer[0]);

        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        // First call triggers confirmation (>= 100000)
        boolean handled1 = admin.onCommand(sender, mock(Command.class), "djeconomy",
                new String[]{"economy", "give", "Alice", "100000"});
        assertTrue(handled1);
        assertTrue(messages.stream().anyMatch(m -> m.contains("Large amount") || m.contains("Use /djeconomy confirm")));
        verify(eco, never()).deposit(any(Player.class), anyDouble());

        messages.clear();

        // Confirm executes the pending action
        boolean handled2 = admin.onCommand(sender, mock(Command.class), "djeconomy",
                new String[]{"confirm"});
        assertTrue(handled2);
        verify(eco, times(1)).deposit(eq(target), eq(100000.0));
        assertTrue(messages.stream().anyMatch(m -> m.contains("Gave $") || m.contains("balance")));
    }

    @Test
    void economy_take_online_sufficient_withdrawsAndNotifies() {
        EconomyManager eco = mock(EconomyManager.class);
        when(eco.getBalance(any(Player.class))).thenReturn(50.0);
        when(eco.withdraw(any(Player.class), anyDouble())).thenReturn(true);
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(eco);
        Player target = mock(Player.class);
        when(target.getName()).thenReturn("Alice");
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.singletonMap("Alice", target), new OfflinePlayer[0]);

        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy",
                new String[]{"economy", "take", "Alice", "10"});
        assertTrue(handled);
        verify(eco, times(1)).withdraw(eq(target), eq(10.0));
        assertTrue(messages.stream().anyMatch(m -> m.contains("Took $") && m.contains("Alice")));
    }

    @Test
    void economy_take_online_insufficient_showsError() {
        EconomyManager eco = mock(EconomyManager.class);
        when(eco.getBalance(any(Player.class))).thenReturn(5.0);
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(eco);
        Player target = mock(Player.class);
        when(target.getName()).thenReturn("Alice");
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.singletonMap("Alice", target), new OfflinePlayer[0]);

        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy",
                new String[]{"economy", "take", "Alice", "10"});
        assertTrue(handled);
        assertTrue(messages.stream().anyMatch(m -> m.contains("Player only has $") ));
        verify(eco, never()).withdraw(any(Player.class), anyDouble());
    }

    @Test
    void economy_set_online_success() {
        EconomyManager eco = mock(EconomyManager.class);
        when(eco.getBalance(any(Player.class))).thenReturn(25.0);
        when(eco.withdraw(any(Player.class), anyDouble())).thenReturn(true);
        when(eco.deposit(any(Player.class), anyDouble())).thenReturn(true);
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(eco);
        Player target = mock(Player.class);
        when(target.getName()).thenReturn("Alice");
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.singletonMap("Alice", target), new OfflinePlayer[0]);

        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy",
                new String[]{"economy", "set", "Alice", "100"});
        assertTrue(handled);
        verify(eco, times(1)).withdraw(eq(target), eq(25.0));
        verify(eco, times(1)).deposit(eq(target), eq(100.0));
        assertTrue(messages.stream().anyMatch(m -> m.contains("Set ") && m.contains("Alice") && m.contains("$100")));
    }

    @Test
    void economy_give_offline_usesDepositPlayer() {
        EconomyManager eco = mock(EconomyManager.class);
        when(eco.depositPlayer(any(OfflinePlayer.class), anyDouble())).thenReturn(true);
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(eco);

        OfflinePlayer off = mock(OfflinePlayer.class);
        when(off.getName()).thenReturn("Bob");
        when(off.hasPlayedBefore()).thenReturn(true);

        // Ensure online lookup misses and offline array contains Bob
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.emptyMap(), new OfflinePlayer[]{off});

        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy",
                new String[]{"economy", "give", "Bob", "15"});
        assertTrue(handled);
        verify(eco, times(1)).depositPlayer(eq(off), eq(15.0));
        assertTrue(messages.stream().anyMatch(m -> m.contains("Gave $") && m.contains("Bob")));
    }

    @Test
    void economy_take_offline_sufficient() {
        EconomyManager eco = mock(EconomyManager.class);
        when(eco.getBalance(any(OfflinePlayer.class))).thenReturn(40.0);
        when(eco.withdraw(any(OfflinePlayer.class), anyDouble())).thenReturn(true);
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(eco);

        OfflinePlayer off = mock(OfflinePlayer.class);
        when(off.getName()).thenReturn("Bob");
        when(off.hasPlayedBefore()).thenReturn(true);

        AdminCommand admin = new TestableAdminCommand(plugin, Collections.emptyMap(), new OfflinePlayer[]{off});

        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy",
                new String[]{"economy", "take", "Bob", "10"});
        assertTrue(handled);
        verify(eco, times(1)).withdraw(eq(off), eq(10.0));
        assertTrue(messages.stream().anyMatch(m -> m.contains("Took $") && m.contains("Bob")));
    }

    @Test
    void economy_set_offline_success() {
        EconomyManager eco = mock(EconomyManager.class);
        when(eco.getBalance(any(OfflinePlayer.class))).thenReturn(70.0);
        when(eco.withdraw(any(OfflinePlayer.class), anyDouble())).thenReturn(true);
        when(eco.depositPlayer(any(OfflinePlayer.class), anyDouble())).thenReturn(true);
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(eco);

        OfflinePlayer off = mock(OfflinePlayer.class);
        when(off.getName()).thenReturn("Bob");
        when(off.hasPlayedBefore()).thenReturn(true);

        AdminCommand admin = new TestableAdminCommand(plugin, Collections.emptyMap(), new OfflinePlayer[]{off});

        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy",
                new String[]{"economy", "set", "Bob", "120"});
        assertTrue(handled);
        verify(eco, times(1)).withdraw(eq(off), eq(70.0));
        verify(eco, times(1)).depositPlayer(eq(off), eq(120.0));
        assertTrue(messages.stream().anyMatch(m -> m.contains("Set ") && m.contains("Bob") && m.contains("$120")));
    }

    @Test
    void setlevel_offline_callsSetOfflineJobLevel_success() {
        EconomyManager eco = mock(EconomyManager.class);
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(eco);
        JobManager jobMgr = mock(JobManager.class);
        when(plugin.getJobManager()).thenReturn(jobMgr);
        when(jobMgr.setOfflineJobLevel(any(OfflinePlayer.class), anyString(), anyInt())).thenReturn(true);

        OfflinePlayer off = mock(OfflinePlayer.class);
        when(off.getName()).thenReturn("Bob");
        when(off.hasPlayedBefore()).thenReturn(true);

        AdminCommand admin = new TestableAdminCommand(plugin, Collections.emptyMap(), new OfflinePlayer[]{off});

        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy",
                new String[]{"setlevel", "Bob", "miner", "5"});
        assertTrue(handled);
        verify(jobMgr, times(1)).setOfflineJobLevel(eq(off), eq("miner"), eq(5));
        assertTrue(messages.stream().anyMatch(m -> m.contains("Set Bob's miner level to 5") || m.contains("Set Bob's") ));
    }

    @Test
    void addxp_offline_rejected() {
        EconomyManager eco = mock(EconomyManager.class);
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(eco);

        OfflinePlayer off = mock(OfflinePlayer.class);
        when(off.getName()).thenReturn("Bob");
        when(off.hasPlayedBefore()).thenReturn(true);

        AdminCommand admin = new TestableAdminCommand(plugin, Collections.emptyMap(), new OfflinePlayer[]{off});

        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy",
                new String[]{"addxp", "Bob", "miner", "10"});
        assertTrue(handled);
        assertTrue(messages.stream().anyMatch(m -> m.contains("Cannot add XP to offline player")));
    }

    @Test
    void setlevel_online_success() {
        EconomyManager eco = mock(EconomyManager.class);
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(eco);
        JobManager jobMgr = mock(JobManager.class);
        when(plugin.getJobManager()).thenReturn(jobMgr);
        when(jobMgr.setJobLevel(any(Player.class), eq("miner"), eq(7))).thenReturn(true);

        Player target = mock(Player.class);
        when(target.getName()).thenReturn("Alice");
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.singletonMap("Alice", target), new OfflinePlayer[0]);

        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy",
                new String[]{"setlevel", "Alice", "miner", "7"});
        assertTrue(handled);
        verify(jobMgr, times(1)).setJobLevel(eq(target), eq("miner"), eq(7));
        assertTrue(messages.stream().anyMatch(m -> m.contains("Set Alice's miner level to 7") && m.contains("(online)")));
    }

    @Test
    void setlevel_online_unknownJob_showsError() {
        EconomyManager eco = mock(EconomyManager.class);
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(eco);
        JobManager jobMgr = mock(JobManager.class);
        when(plugin.getJobManager()).thenReturn(jobMgr);
        when(jobMgr.setJobLevel(any(Player.class), anyString(), anyInt())).thenReturn(false);

        Player target = mock(Player.class);
        when(target.getName()).thenReturn("Alice");
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.singletonMap("Alice", target), new OfflinePlayer[0]);

        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy",
                new String[]{"setlevel", "Alice", "miner", "7"});
        assertTrue(handled);
        assertTrue(messages.stream().anyMatch(m -> m.contains("Unknown job 'miner'")));
        verify(jobMgr, times(1)).setJobLevel(eq(target), eq("miner"), eq(7));
    }

    @Test
    void addxp_online_success() {
        EconomyManager eco = mock(EconomyManager.class);
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(eco);
        JobManager jobMgr = mock(JobManager.class);
        when(plugin.getJobManager()).thenReturn(jobMgr);

        Job job = mock(Job.class);
        when(job.getName()).thenReturn("miner");
        when(jobMgr.getJob(eq("miner"))).thenReturn(job);

        PlayerJobData pdata = mock(PlayerJobData.class);
        when(pdata.hasJob(eq("miner"))).thenReturn(true);
        when(jobMgr.getPlayerData(any(Player.class))).thenReturn(pdata);

        Player target = mock(Player.class);
        when(target.getName()).thenReturn("Alice");
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.singletonMap("Alice", target), new OfflinePlayer[0]);

        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy",
                new String[]{"addxp", "Alice", "miner", "10"});
        assertTrue(handled);
        verify(jobMgr, times(1)).addExperience(eq(target), eq("miner"), eq(10));
        assertTrue(messages.stream().anyMatch(m -> m.contains("Added 10 XP") && m.contains("Alice's 'miner' job")));
    }

    @Test
    void addxp_online_notJoined_showsError() {
        EconomyManager eco = mock(EconomyManager.class);
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(eco);
        JobManager jobMgr = mock(JobManager.class);
        when(plugin.getJobManager()).thenReturn(jobMgr);

        Job job = mock(Job.class);
        when(job.getName()).thenReturn("miner");
        when(jobMgr.getJob(eq("miner"))).thenReturn(job);

        PlayerJobData pdata = mock(PlayerJobData.class);
        when(pdata.hasJob(eq("miner"))).thenReturn(false);
        when(jobMgr.getPlayerData(any(Player.class))).thenReturn(pdata);

        Player target = mock(Player.class);
        when(target.getName()).thenReturn("Alice");
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.singletonMap("Alice", target), new OfflinePlayer[0]);

        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy",
                new String[]{"addxp", "Alice", "miner", "10"});
        assertTrue(handled);
        assertTrue(messages.stream().anyMatch(m -> m.contains("has not joined the job 'miner'")));
        verify(jobMgr, never()).addExperience(any(Player.class), anyString(), anyInt());
    }
    
    @Test
    void economy_take_offline_insufficient_showsError() {
        EconomyManager eco = mock(EconomyManager.class);
        when(eco.getBalance(any(OfflinePlayer.class))).thenReturn(8.0);
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(eco);

        OfflinePlayer off = mock(OfflinePlayer.class);
        when(off.getName()).thenReturn("Bob");
        when(off.hasPlayedBefore()).thenReturn(true);

        AdminCommand admin = new TestableAdminCommand(plugin, Collections.emptyMap(), new OfflinePlayer[]{off});

        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy",
                new String[]{"economy", "take", "Bob", "10"});
        assertTrue(handled);
        assertTrue(messages.stream().anyMatch(m -> m.contains("Player only has $") ));
        verify(eco, never()).withdraw(any(OfflinePlayer.class), anyDouble());
    }

    @Test
    void economy_invalid_action_showsError() {
        EconomyManager eco = mock(EconomyManager.class);
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(eco);
        Player target = mock(Player.class);
        when(target.getName()).thenReturn("Alice");
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.singletonMap("Alice", target), new OfflinePlayer[0]);

        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy",
                new String[]{"economy", "foo", "Alice", "10"});
        assertTrue(handled);
        assertTrue(messages.stream().anyMatch(m -> m.contains("Invalid action!")));
    }

    @Test
    void setlevel_invalid_level_showsError() {
        EconomyManager eco = mock(EconomyManager.class);
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(eco);
        Player target = mock(Player.class);
        when(target.getName()).thenReturn("Alice");
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.singletonMap("Alice", target), new OfflinePlayer[0]);

        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy",
                new String[]{"setlevel", "Alice", "miner", "not-a-number"});
        assertTrue(handled);
        assertTrue(messages.stream().anyMatch(m -> m.contains("Invalid level number")));
    }

    @Test
    void addxp_invalid_amount_showsError() {
        EconomyManager eco = mock(EconomyManager.class);
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(eco);
        Player target = mock(Player.class);
        when(target.getName()).thenReturn("Alice");
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.singletonMap("Alice", target), new OfflinePlayer[0]);

        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy",
                new String[]{"addxp", "Alice", "miner", "ten"});
        assertTrue(handled);
        assertTrue(messages.stream().anyMatch(m -> m.contains("Invalid XP amount")));
    }

    @Test
    void refreshjobs_online_success_and_offline_error() {
        EconomyManager eco = mock(EconomyManager.class);
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(eco);
        JobManager jobMgr = mock(JobManager.class);
        when(plugin.getJobManager()).thenReturn(jobMgr);

        Player online = mock(Player.class);
        when(online.getName()).thenReturn("Alice");
        OfflinePlayer off = mock(OfflinePlayer.class);
        when(off.getName()).thenReturn("Bob");
        when(off.hasPlayedBefore()).thenReturn(true);

        AdminCommand adminOnline = new TestableAdminCommand(plugin, Collections.singletonMap("Alice", online), new OfflinePlayer[0]);
        AdminCommand adminOffline = new TestableAdminCommand(plugin, Collections.emptyMap(), new OfflinePlayer[]{off});

        // Online success
        List<String> msg1 = new ArrayList<>();
        Player sender1 = mockSenderCollectingMessages(msg1);
        boolean handled1 = adminOnline.onCommand(sender1, mock(Command.class), "djeconomy",
                new String[]{"refreshjobs", "Alice"});
        assertTrue(handled1);
        verify(jobMgr, times(1)).refreshPlayerData(eq(online));
        assertTrue(msg1.stream().anyMatch(m -> m.contains("Refreshed job data for") && m.contains("Alice")));

        // Offline error
        reset(jobMgr);
        List<String> msg2 = new ArrayList<>();
        Player sender2 = mockSenderCollectingMessages(msg2);
        boolean handled2 = adminOffline.onCommand(sender2, mock(Command.class), "djeconomy",
                new String[]{"refreshjobs", "Bob"});
        assertTrue(handled2);
        assertTrue(msg2.stream().anyMatch(m -> m.contains("must be online") && m.contains("refresh job data")));
        verify(jobMgr, never()).refreshPlayerData(any(Player.class));
    }

    @Test
    void invalidatejobs_online_success_and_offline_error() {
        EconomyManager eco = mock(EconomyManager.class);
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(eco);
        JobManager jobMgr = mock(JobManager.class);
        when(plugin.getJobManager()).thenReturn(jobMgr);

        Player online = mock(Player.class);
        when(online.getName()).thenReturn("Alice");
        OfflinePlayer off = mock(OfflinePlayer.class);
        when(off.getName()).thenReturn("Bob");
        when(off.hasPlayedBefore()).thenReturn(true);

        AdminCommand adminOnline = new TestableAdminCommand(plugin, Collections.singletonMap("Alice", online), new OfflinePlayer[0]);
        AdminCommand adminOffline = new TestableAdminCommand(plugin, Collections.emptyMap(), new OfflinePlayer[]{off});

        // Online success
        List<String> msg1 = new ArrayList<>();
        Player sender1 = mockSenderCollectingMessages(msg1);
        boolean handled1 = adminOnline.onCommand(sender1, mock(Command.class), "djeconomy",
                new String[]{"invalidatejobs", "Alice"});
        assertTrue(handled1);
        verify(jobMgr, times(1)).invalidatePlayerData(eq(online));
        assertTrue(msg1.stream().anyMatch(m -> m.contains("Invalidated cached job data for") && m.contains("Alice")));

        // Offline error
        reset(jobMgr);
        List<String> msg2 = new ArrayList<>();
        Player sender2 = mockSenderCollectingMessages(msg2);
        boolean handled2 = adminOffline.onCommand(sender2, mock(Command.class), "djeconomy",
                new String[]{"invalidatejobs", "Bob"});
        assertTrue(handled2);
        assertTrue(msg2.stream().anyMatch(m -> m.contains("must be online") && m.contains("invalidate cached job data")));
        verify(jobMgr, never()).invalidatePlayerData(any(Player.class));
    }

    @Test
    void tabcomplete_economy_subcommands_prefix_and_empty() {
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(null);
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.emptyMap(), new OfflinePlayer[0]);
        Player sender = mockSenderCollectingMessages(new ArrayList<>());

        List<String> all = admin.onTabComplete(sender, mock(Command.class), "djeconomy",
                new String[]{"economy", ""});
        assertTrue(all.contains("give") && all.contains("take") && all.contains("set"));
        
        List<String> onlyT = admin.onTabComplete(sender, mock(Command.class), "djeconomy",
                new String[]{"economy", "t"});
        assertTrue(onlyT.contains("take") && onlyT.size() == 1);
    }

    @Test
    void tabcomplete_player_suggestions_for_economy_and_jobs() {
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(null);
        Map<String, Player> online = new HashMap<>();
        Player a = mock(Player.class); when(a.getName()).thenReturn("Alice"); online.put("Alice", a);
        Player b = mock(Player.class); when(b.getName()).thenReturn("Bob"); online.put("Bob", b);
        Player c = mock(Player.class); when(c.getName()).thenReturn("Charlie"); online.put("Charlie", c);
        AdminCommand admin = new TestableAdminCommand(plugin, online, new OfflinePlayer[0]);
        Player sender = mockSenderCollectingMessages(new ArrayList<>());

        // economy player arg (3rd arg)
        List<String> econAll = admin.onTabComplete(sender, mock(Command.class), "djeconomy",
                new String[]{"economy", "give", ""});
        assertTrue(econAll.contains("Alice") && econAll.contains("Bob") && econAll.contains("Charlie"));

        List<String> econB = admin.onTabComplete(sender, mock(Command.class), "djeconomy",
                new String[]{"economy", "give", "B"});
        assertTrue(econB.contains("Bob") && econB.size() == 1);

        // setlevel first player arg (2nd arg)
        List<String> jobsA = admin.onTabComplete(sender, mock(Command.class), "djeconomy",
                new String[]{"setlevel", "A"});
        assertTrue(jobsA.contains("Alice"));
    }

    @Test
    void tabcomplete_job_suggestions() {
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(null);
        JobManager jobMgr = mock(JobManager.class);
        when(plugin.getJobManager()).thenReturn(jobMgr);
        Map<String, Job> jobs = new HashMap<>();
        Job miner = mock(Job.class); when(miner.getName()).thenReturn("Miner"); jobs.put("Miner", miner);
        Job farmer = mock(Job.class); when(farmer.getName()).thenReturn("Farmer"); jobs.put("Farmer", farmer);
        when(jobMgr.getJobs()).thenReturn(jobs);

        AdminCommand admin = new TestableAdminCommand(plugin, Collections.emptyMap(), new OfflinePlayer[0]);
        Player sender = mockSenderCollectingMessages(new ArrayList<>());

        List<String> sugg1 = admin.onTabComplete(sender, mock(Command.class), "djeconomy",
                new String[]{"setlevel", "Alice", "m"});
        assertTrue(sugg1.stream().anyMatch(s -> s.equalsIgnoreCase("Miner")));

        List<String> suggAll = admin.onTabComplete(sender, mock(Command.class), "djeconomy",
                new String[]{"addxp", "Alice", ""});
        // Should include both jobs
        assertTrue(suggAll.contains("Miner") && suggAll.contains("Farmer"));
    }

    @Test
    void confirm_no_pending_showsError() {
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(null);
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.emptyMap(), new OfflinePlayer[0]);
        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);
        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy",
                new String[]{"confirm"});
        assertTrue(handled);
        assertTrue(messages.stream().anyMatch(m -> m.contains("No pending action to confirm")));
    }

    @Test
    void confirm_non_player_sender_showsError() {
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(null);
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.emptyMap(), new OfflinePlayer[0]);
        List<String> messages = new ArrayList<>();
        CommandSender console = mockNonPlayerSender(messages, true);
        boolean handled = admin.onCommand(console, mock(Command.class), "djeconomy",
                new String[]{"confirm"});
        assertTrue(handled);
        assertTrue(messages.stream().anyMatch(m -> m.contains("Only players can use confirmations!")));
    }

    @Test
    void permission_denied_showsMessage_andNoAction() {
        EconomyManager eco = mock(EconomyManager.class);
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(eco);
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.emptyMap(), new OfflinePlayer[0]);
        List<String> messages = new ArrayList<>();
        CommandSender noPerm = mockNonPlayerSender(messages, false);

        boolean handled = admin.onCommand(noPerm, mock(Command.class), "djeconomy",
                new String[]{"economy", "give", "Alice", "10"});
        assertTrue(handled);
        assertTrue(messages.stream().anyMatch(m -> m.contains("don't have permission")));
        verifyNoInteractions(eco);
    }

    @Test
    void economy_unknown_player_showsError_andNoEconomyCalls() {
        EconomyManager eco = mock(EconomyManager.class);
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(eco);
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.emptyMap(), new OfflinePlayer[0]);
        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy",
                new String[]{"economy", "give", "Ghost", "10"});
        assertTrue(handled);
        assertTrue(messages.stream().anyMatch(m -> m.contains("not found or has never joined")));
        verify(eco, never()).deposit(any(Player.class), anyDouble());
        verify(eco, never()).depositPlayer(any(OfflinePlayer.class), anyDouble());
        verify(eco, never()).withdraw(any(Player.class), anyDouble());
        verify(eco, never()).withdraw(any(OfflinePlayer.class), anyDouble());
    }

    @Test
    void setlevel_unknown_player_showsError() {
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(null);
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.emptyMap(), new OfflinePlayer[0]);
        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy",
                new String[]{"setlevel", "Ghost", "miner", "5"});
        assertTrue(handled);
        assertTrue(messages.stream().anyMatch(m -> m.contains("not found or has never joined")));
    }

    @Test
    void addxp_unknown_player_showsError() {
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(null);
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.emptyMap(), new OfflinePlayer[0]);
        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy",
                new String[]{"addxp", "Ghost", "miner", "5"});
        assertTrue(handled);
        assertTrue(messages.stream().anyMatch(m -> m.contains("not found or has never joined")));
    }

    @Test
    void refreshjobs_unknown_player_showsError_andNoJobCalls() {
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(null);
        JobManager jobMgr = mock(JobManager.class);
        when(plugin.getJobManager()).thenReturn(jobMgr);
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.emptyMap(), new OfflinePlayer[0]);
        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy",
                new String[]{"refreshjobs", "Ghost"});
        assertTrue(handled);
        assertTrue(messages.stream().anyMatch(m -> m.contains("not found or has never joined")));
        verify(jobMgr, never()).refreshPlayerData(any(Player.class));
    }

    @Test
    void invalidatejobs_unknown_player_showsError_andNoJobCalls() {
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(null);
        JobManager jobMgr = mock(JobManager.class);
        when(plugin.getJobManager()).thenReturn(jobMgr);
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.emptyMap(), new OfflinePlayer[0]);
        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy",
                new String[]{"invalidatejobs", "Ghost"});
        assertTrue(handled);
        assertTrue(messages.stream().anyMatch(m -> m.contains("not found or has never joined")));
        verify(jobMgr, never()).invalidatePlayerData(any(Player.class));
    }

    @Test
    void tabcomplete_root_commands_empty_and_prefixes() {
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(null);
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.emptyMap(), new OfflinePlayer[0]);
        Player sender = mockSenderCollectingMessages(new ArrayList<>());

        List<String> all = admin.onTabComplete(sender, mock(Command.class), "djeconomy",
                new String[]{""});
        assertTrue(all.contains("reload") && all.contains("setlevel") && all.contains("getlevel")
                && all.contains("resetlevel") && all.contains("addxp") && all.contains("economy")
                && all.contains("history") && all.contains("refreshjobs") && all.contains("invalidatejobs"));

        List<String> re = admin.onTabComplete(sender, mock(Command.class), "djeconomy",
                new String[]{"re"});
        assertTrue(re.contains("reload") && re.contains("refreshjobs"));

        List<String> inv = admin.onTabComplete(sender, mock(Command.class), "djeconomy",
                new String[]{"in"});
        assertTrue(inv.contains("invalidatejobs") && inv.size() == 1);
    }

    @Test
    void usage_economy_when_insufficient_args() {
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(null);
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.emptyMap(), new OfflinePlayer[0]);
        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy",
                new String[]{"economy", "give", "Alice"});
        assertTrue(handled);
        assertTrue(messages.stream().anyMatch(m -> m.contains("Usage: /djeconomy economy")));
    }

    @Test
    void usage_setlevel_when_insufficient_args() {
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(null);
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.emptyMap(), new OfflinePlayer[0]);
        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy",
                new String[]{"setlevel", "Alice", "miner"});
        assertTrue(handled);
        assertTrue(messages.stream().anyMatch(m -> m.contains("Usage: /djeconomy setlevel")));
    }

    @Test
    void usage_addxp_when_insufficient_args() {
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(null);
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.emptyMap(), new OfflinePlayer[0]);
        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy",
                new String[]{"addxp", "Alice", "miner"});
        assertTrue(handled);
        assertTrue(messages.stream().anyMatch(m -> m.contains("Usage: /djeconomy addxp")));
    }

    @Test
    void usage_refreshjobs_when_insufficient_args() {
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(null);
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.emptyMap(), new OfflinePlayer[0]);
        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy",
                new String[]{"refreshjobs"});
        assertTrue(handled);
        assertTrue(messages.stream().anyMatch(m -> m.contains("Usage: /djeconomy refreshjobs")));
    }

    @Test
    void usage_invalidatejobs_when_insufficient_args() {
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(null);
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.emptyMap(), new OfflinePlayer[0]);
        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy",
                new String[]{"invalidatejobs"});
        assertTrue(handled);
        assertTrue(messages.stream().anyMatch(m -> m.contains("Usage: /djeconomy invalidatejobs")));
    }

    @Test
    void reload_command_reloadsConfig_and_sendsMessage() {
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(null);
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.emptyMap(), new OfflinePlayer[0]);
        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy",
                new String[]{"reload"});
        assertTrue(handled);
        verify(plugin, times(1)).reloadConfig();
        assertTrue(messages.stream().anyMatch(m -> m.contains("Configuration reloaded!")));
    }

    @Test
    void help_shown_with_no_args_lists_core_commands() {
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(null);
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.emptyMap(), new OfflinePlayer[0]);
        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{});
        assertTrue(handled);
        // Check for presence of key help lines (ignore color codes/prefix)
        assertTrue(messages.stream().anyMatch(m -> m.contains("/djeconomy reload")));
        assertTrue(messages.stream().anyMatch(m -> m.contains("/djeconomy setlevel")));
        assertTrue(messages.stream().anyMatch(m -> m.contains("/djeconomy addxp")));
        assertTrue(messages.stream().anyMatch(m -> m.contains("/djeconomy economy")));
        assertTrue(messages.stream().anyMatch(m -> m.contains("/djeconomy refreshjobs")));
        assertTrue(messages.stream().anyMatch(m -> m.contains("/djeconomy invalidatejobs")));
    }

    @Test
    void help_shown_on_unknown_subcommand_lists_core_commands() {
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(null);
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.emptyMap(), new OfflinePlayer[0]);
        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"doesnotexist"});
        assertTrue(handled);
        assertTrue(messages.stream().anyMatch(m -> m.contains("/djeconomy reload")));
        assertTrue(messages.stream().anyMatch(m -> m.contains("/djeconomy setlevel")));
        assertTrue(messages.stream().anyMatch(m -> m.contains("/djeconomy addxp")));
        assertTrue(messages.stream().anyMatch(m -> m.contains("/djeconomy economy")));
        assertTrue(messages.stream().anyMatch(m -> m.contains("/djeconomy refreshjobs")));
        assertTrue(messages.stream().anyMatch(m -> m.contains("/djeconomy invalidatejobs")));
    }

    // === Additional tests ===

    @Test
    void confirm_with_no_pending_shows_message() {
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(null);
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.emptyMap(), new OfflinePlayer[0]);
        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"confirm"});
        assertTrue(handled);
        assertTrue(messages.stream().anyMatch(m -> m.contains("No pending action to confirm!")));
    }

    @Test
    void confirm_uses_latest_pending_action_and_clears_after_use() {
        EconomyManager eco = mock(EconomyManager.class);
        when(eco.getBalance(any(Player.class))).thenReturn(500000.0);
        when(eco.withdraw(any(Player.class), anyDouble())).thenReturn(true);
        when(eco.deposit(any(Player.class), anyDouble())).thenReturn(true);
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(eco);

        Player target = mock(Player.class);
        when(target.getName()).thenReturn("Alice");
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.singletonMap("Alice", target), new OfflinePlayer[0]);

        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        // First large operation stores pending (GIVE)
        assertTrue(admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"economy", "give", "Alice", "100000"}));
        // Second large operation overwrites pending (TAKE)
        assertTrue(admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"economy", "take", "Alice", "100000"}));

        messages.clear();
        // Confirm should execute the latest (TAKE) and clear the pending
        assertTrue(admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"confirm"}));
        verify(eco, times(1)).withdraw(eq(target), eq(100000.0));
        verify(eco, never()).deposit(eq(target), anyDouble());
        assertTrue(messages.stream().anyMatch(m -> m.contains("Took $") && m.contains("Alice")));

        messages.clear();
        // A second confirm immediately after should report no pending
        assertTrue(admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"confirm"}));
        assertTrue(messages.stream().anyMatch(m -> m.contains("No pending action to confirm!")));
    }

    @Test
    void refreshjobs_online_success_calls_manager_and_notifies() {
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(null);
        JobManager jobMgr = mock(JobManager.class);
        when(plugin.getJobManager()).thenReturn(jobMgr);

        Player target = mock(Player.class);
        when(target.getName()).thenReturn("Alice");
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.singletonMap("Alice", target), new OfflinePlayer[0]);

        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"refreshjobs", "Alice"});
        assertTrue(handled);
        verify(jobMgr, times(1)).refreshPlayerData(eq(target));
        assertTrue(messages.stream().anyMatch(m -> m.contains("Refreshed job data for Alice")));
    }

    @Test
    void refreshjobs_offline_player_shows_online_only_message() {
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(null);
        JobManager jobMgr = mock(JobManager.class);
        when(plugin.getJobManager()).thenReturn(jobMgr);

        OfflinePlayer off = mock(OfflinePlayer.class);
        when(off.getName()).thenReturn("Bob");
        when(off.hasPlayedBefore()).thenReturn(true);

        AdminCommand admin = new TestableAdminCommand(plugin, Collections.emptyMap(), new OfflinePlayer[]{off});

        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"refreshjobs", "Bob"});
        assertTrue(handled);
        assertTrue(messages.stream().anyMatch(m -> m.contains("Player must be online to refresh job data")));
        verify(jobMgr, never()).refreshPlayerData(any());
    }

    @Test
    void refreshjobs_unknown_player_shows_not_found() {
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(null);
        JobManager jobMgr = mock(JobManager.class);
        when(plugin.getJobManager()).thenReturn(jobMgr);

        AdminCommand admin = new TestableAdminCommand(plugin, Collections.emptyMap(), new OfflinePlayer[0]);
        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"refreshjobs", "Ghost"});
        assertTrue(handled);
        assertTrue(messages.stream().anyMatch(m -> m.contains("not found or has never joined")));
        verify(jobMgr, never()).refreshPlayerData(any());
    }

    @Test
    void invalidatejobs_online_success_calls_manager_and_notifies() {
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(null);
        JobManager jobMgr = mock(JobManager.class);
        when(plugin.getJobManager()).thenReturn(jobMgr);

        Player target = mock(Player.class);
        when(target.getName()).thenReturn("Alice");
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.singletonMap("Alice", target), new OfflinePlayer[0]);

        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"invalidatejobs", "Alice"});
        assertTrue(handled);
        verify(jobMgr, times(1)).invalidatePlayerData(eq(target));
        assertTrue(messages.stream().anyMatch(m -> m.contains("Invalidated cached job data for Alice")));
    }

    @Test
    void invalidatejobs_offline_player_shows_online_only_message() {
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(null);
        JobManager jobMgr = mock(JobManager.class);
        when(plugin.getJobManager()).thenReturn(jobMgr);

        OfflinePlayer off = mock(OfflinePlayer.class);
        when(off.getName()).thenReturn("Bob");
        when(off.hasPlayedBefore()).thenReturn(true);

        AdminCommand admin = new TestableAdminCommand(plugin, Collections.emptyMap(), new OfflinePlayer[]{off});

        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"invalidatejobs", "Bob"});
        assertTrue(handled);
        assertTrue(messages.stream().anyMatch(m -> m.contains("Player must be online to invalidate cached job data")));
        verify(jobMgr, never()).invalidatePlayerData(any());
    }

    @Test
    void invalidatejobs_unknown_player_shows_not_found() {
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(null);
        JobManager jobMgr = mock(JobManager.class);
        when(plugin.getJobManager()).thenReturn(jobMgr);

        AdminCommand admin = new TestableAdminCommand(plugin, Collections.emptyMap(), new OfflinePlayer[0]);
        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"invalidatejobs", "Ghost"});
        assertTrue(handled);
        assertTrue(messages.stream().anyMatch(m -> m.contains("not found or has never joined")));
        verify(jobMgr, never()).invalidatePlayerData(any());
    }

    @Test
    void addxp_invalid_xp_amount_rejected() {
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(null);
        JobManager jobMgr = mock(JobManager.class);
        when(plugin.getJobManager()).thenReturn(jobMgr);

        Player target = mock(Player.class);
        when(target.getName()).thenReturn("Alice");
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.singletonMap("Alice", target), new OfflinePlayer[0]);

        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"addxp", "Alice", "miner", "abc"});
        assertTrue(handled);
        assertTrue(messages.stream().anyMatch(m -> m.contains("Invalid XP amount")));
        verify(jobMgr, never()).addExperience(any(Player.class), anyString(), anyInt());
    }

    @Test
    void addxp_unknown_job_shows_error() {
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(null);
        JobManager jobMgr = mock(JobManager.class);
        when(plugin.getJobManager()).thenReturn(jobMgr);

        Player target = mock(Player.class);
        when(target.getName()).thenReturn("Alice");
        when(jobMgr.getJob(eq("miner"))).thenReturn(null); // Unknown job
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.singletonMap("Alice", target), new OfflinePlayer[0]);

        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"addxp", "Alice", "miner", "5"});
        assertTrue(handled);
        assertTrue(messages.stream().anyMatch(m -> m.contains("Unknown job 'miner'")));
        verify(jobMgr, never()).addExperience(any(Player.class), anyString(), anyInt());
    }

    @Test
    void setlevel_invalid_level_number_rejected() {
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(null);
        JobManager jobMgr = mock(JobManager.class);
        when(plugin.getJobManager()).thenReturn(jobMgr);

        Player target = mock(Player.class);
        when(target.getName()).thenReturn("Alice");
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.singletonMap("Alice", target), new OfflinePlayer[0]);

        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"setlevel", "Alice", "miner", "NaN"});
        assertTrue(handled);
        assertTrue(messages.stream().anyMatch(m -> m.contains("Invalid level number")));
        verify(jobMgr, never()).setJobLevel(any(Player.class), anyString(), anyInt());
        verify(jobMgr, never()).setOfflineJobLevel(any(OfflinePlayer.class), anyString(), anyInt());
    }

    @Test
    void economy_offline_note_is_sent_for_offline_player() {
        EconomyManager eco = mock(EconomyManager.class);
        when(eco.depositPlayer(any(OfflinePlayer.class), anyDouble())).thenReturn(true);
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(eco);

        OfflinePlayer off = mock(OfflinePlayer.class);
        when(off.getName()).thenReturn("Bob");
        when(off.hasPlayedBefore()).thenReturn(true);

        AdminCommand admin = new TestableAdminCommand(plugin, Collections.emptyMap(), new OfflinePlayer[]{off});

        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"economy", "give", "Bob", "15"});
        assertTrue(handled);
        assertTrue(messages.stream().anyMatch(m -> m.contains("is offline. Processing transaction")));
    }

    @Test
    void tabcomplete_root_prefix_no_match_returns_empty() {
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(null);
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.emptyMap(), new OfflinePlayer[0]);
        List<String> suggestions = admin.onTabComplete(mock(CommandSender.class), mock(Command.class), "djeconomy", new String[]{"zzz"});
        assertNotNull(suggestions);
        assertTrue(suggestions.isEmpty());
    }

    @Test
    void tabcomplete_refresh_and_invalidate_suggest_online_players_by_prefix() {
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(null);
        Player alice = mock(Player.class);
        when(alice.getName()).thenReturn("Alice");
        OfflinePlayer off = mock(OfflinePlayer.class);
        when(off.getName()).thenReturn("Bob");
        when(off.hasPlayedBefore()).thenReturn(true);

        AdminCommand admin = new TestableAdminCommand(plugin, Collections.singletonMap("Alice", alice), new OfflinePlayer[]{off});

        List<String> s1 = admin.onTabComplete(mock(CommandSender.class), mock(Command.class), "djeconomy", new String[]{"refreshjobs", "Al"});
        assertTrue(s1.contains("Alice"));
        assertFalse(s1.contains("Bob"));

        List<String> s2 = admin.onTabComplete(mock(CommandSender.class), mock(Command.class), "djeconomy", new String[]{"invalidatejobs", "a"});
        assertTrue(s2.contains("Alice"));
        assertFalse(s2.contains("Bob"));
    }

    @Test
    void tabcomplete_economy_arg3_player_prefix_case_insensitive() {
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(null);
        Player alice = mock(Player.class);
        when(alice.getName()).thenReturn("Alice");
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.singletonMap("Alice", alice), new OfflinePlayer[0]);

        List<String> suggestions = admin.onTabComplete(mock(CommandSender.class), mock(Command.class), "djeconomy", new String[]{"economy", "give", "AL"});
        assertTrue(suggestions.contains("Alice"));
    }

    @Test
    void tabcomplete_job_suggestions_case_insensitive_and_prefix() {
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(null);
        JobManager jobMgr = mock(JobManager.class);
        when(plugin.getJobManager()).thenReturn(jobMgr);
        Map<String, Job> jobs = new HashMap<>();
        jobs.put("Miner", mock(Job.class));
        jobs.put("Farmer", mock(Job.class));
        when(jobMgr.getJobs()).thenReturn(jobs);

        AdminCommand admin = new TestableAdminCommand(plugin, Collections.emptyMap(), new OfflinePlayer[0]);

        List<String> suggestions = admin.onTabComplete(mock(CommandSender.class), mock(Command.class), "djeconomy", new String[]{"setlevel", "Alice", "mi"});
        assertTrue(suggestions.contains("Miner"));
        assertFalse(suggestions.contains("Farmer"));
    }
    
    // === addxp behaviors ===
    @Test
    void addxp_offline_player_rejected_with_message() {
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(null);
        JobManager jobMgr = mock(JobManager.class);
        when(plugin.getJobManager()).thenReturn(jobMgr);

        OfflinePlayer off = mock(OfflinePlayer.class);
        when(off.getName()).thenReturn("Alice");
        when(off.hasPlayedBefore()).thenReturn(true);
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.emptyMap(), new OfflinePlayer[]{off});

        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"addxp", "Alice", "miner", "10"});
        assertTrue(handled);
        assertTrue(messages.stream().anyMatch(m -> m.contains("Cannot add XP to offline player")));
    }

    @Test
    void addxp_unknown_player_shows_not_found_message() {
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(null);
        JobManager jobMgr = mock(JobManager.class);
        when(plugin.getJobManager()).thenReturn(jobMgr);

        AdminCommand admin = new TestableAdminCommand(plugin, Collections.emptyMap(), new OfflinePlayer[0]);
        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"addxp", "Ghost", "miner", "10"});
        assertTrue(handled);
        assertTrue(messages.stream().anyMatch(m -> m.contains("not found or has never joined")));
    }

    @Test
    void addxp_player_not_joined_job_shows_message() {
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(null);
        JobManager jobMgr = mock(JobManager.class);
        when(plugin.getJobManager()).thenReturn(jobMgr);

        Job job = mock(Job.class);
        when(job.getName()).thenReturn("Miner");
        when(jobMgr.getJob(eq("miner"))).thenReturn(job);

        Player player = mock(Player.class);
        when(player.getName()).thenReturn("Alice");

        PlayerJobData pdata = mock(PlayerJobData.class);
        when(jobMgr.getPlayerData(eq(player))).thenReturn(pdata);
        when(pdata.hasJob(eq("Miner"))).thenReturn(false);

        AdminCommand admin = new TestableAdminCommand(plugin, Collections.singletonMap("Alice", player), new OfflinePlayer[0]);
        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"addxp", "Alice", "miner", "10"});
        assertTrue(handled);
        assertTrue(messages.stream().anyMatch(m -> m.contains("has not joined the job 'Miner'")));
        verify(jobMgr, never()).addExperience(any(Player.class), anyString(), anyInt());
    }

    @Test
    void addxp_online_success_calls_addExperience_and_notifies() {
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(null);
        JobManager jobMgr = mock(JobManager.class);
        when(plugin.getJobManager()).thenReturn(jobMgr);

        Job job = mock(Job.class);
        when(job.getName()).thenReturn("Miner");
        when(jobMgr.getJob(eq("miner"))).thenReturn(job);

        Player player = mock(Player.class);
        when(player.getName()).thenReturn("Alice");

        PlayerJobData pdata = mock(PlayerJobData.class);
        when(jobMgr.getPlayerData(eq(player))).thenReturn(pdata);
        when(pdata.hasJob(eq("Miner"))).thenReturn(true);

        AdminCommand admin = new TestableAdminCommand(plugin, Collections.singletonMap("Alice", player), new OfflinePlayer[0]);
        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"addxp", "Alice", "miner", "10"});
        assertTrue(handled);
        verify(jobMgr, times(1)).addExperience(eq(player), eq("Miner"), eq(10));
        assertTrue(messages.stream().anyMatch(m -> m.contains("Added 10 XP") && m.contains("'Miner' job")));
    }

    // === setlevel behaviors ===
    @Test
    void setlevel_online_unknown_job_shows_error() {
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(null);
        JobManager jobMgr = mock(JobManager.class);
        when(plugin.getJobManager()).thenReturn(jobMgr);

        Player player = mock(Player.class);
        when(player.getName()).thenReturn("Alice");

        when(jobMgr.setJobLevel(eq(player), eq("miner"), eq(3))).thenReturn(false);

        AdminCommand admin = new TestableAdminCommand(plugin, Collections.singletonMap("Alice", player), new OfflinePlayer[0]);
        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"setlevel", "Alice", "miner", "3"});
        assertTrue(handled);
        assertTrue(messages.stream().anyMatch(m -> m.contains("Unknown job 'miner'")));
    }

    @Test
    void setlevel_offline_unknown_job_shows_error() {
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(null);
        JobManager jobMgr = mock(JobManager.class);
        when(plugin.getJobManager()).thenReturn(jobMgr);

        OfflinePlayer off = mock(OfflinePlayer.class);
        when(off.getName()).thenReturn("Bob");
        when(off.hasPlayedBefore()).thenReturn(true);

        when(jobMgr.setOfflineJobLevel(eq(off), eq("miner"), eq(3))).thenReturn(false);

        AdminCommand admin = new TestableAdminCommand(plugin, Collections.emptyMap(), new OfflinePlayer[]{off});
        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"setlevel", "Bob", "miner", "3"});
        assertTrue(handled);
        assertTrue(messages.stream().anyMatch(m -> m.contains("Unknown job 'miner'")));
    }

    @Test
    void setlevel_online_success_message_and_call() {
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(null);
        JobManager jobMgr = mock(JobManager.class);
        when(plugin.getJobManager()).thenReturn(jobMgr);

        Player player = mock(Player.class);
        when(player.getName()).thenReturn("Alice");

        when(jobMgr.setJobLevel(eq(player), eq("miner"), eq(3))).thenReturn(true);

        AdminCommand admin = new TestableAdminCommand(plugin, Collections.singletonMap("Alice", player), new OfflinePlayer[0]);
        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"setlevel", "Alice", "miner", "3"});
        assertTrue(handled);
        verify(jobMgr, times(1)).setJobLevel(eq(player), eq("miner"), eq(3));
        assertTrue(messages.stream().anyMatch(m -> m.contains("Set Alice's miner level to 3 (online)")));
    }

    @Test
    void setlevel_offline_success_message_and_call() {
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(null);
        JobManager jobMgr = mock(JobManager.class);
        when(plugin.getJobManager()).thenReturn(jobMgr);

        OfflinePlayer off = mock(OfflinePlayer.class);
        when(off.getName()).thenReturn("Bob");
        when(off.hasPlayedBefore()).thenReturn(true);

        when(jobMgr.setOfflineJobLevel(eq(off), eq("miner"), eq(2))).thenReturn(true);

        AdminCommand admin = new TestableAdminCommand(plugin, Collections.emptyMap(), new OfflinePlayer[]{off});
        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"setlevel", "Bob", "miner", "2"});
        assertTrue(handled);
        verify(jobMgr, times(1)).setOfflineJobLevel(eq(off), eq("miner"), eq(2));
        assertTrue(messages.stream().anyMatch(m -> m.contains("Set Bob's miner level to 2 (offline)")));
    }

    // === economy additional behaviors ===
    @Test
    void economy_invalid_action_sends_error_message() {
        EconomyManager eco = mock(EconomyManager.class);
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(eco);
        Player player = mock(Player.class);
        when(player.getName()).thenReturn("Alice");
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.singletonMap("Alice", player), new OfflinePlayer[0]);

        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"economy", "oops", "Alice", "10"});
        assertTrue(handled);
        assertTrue(messages.stream().anyMatch(m -> m.contains("Invalid action! Use give, take, or set")));
    }

    @Test
    void economy_take_insufficient_balance_shows_message_and_no_withdraw() {
        EconomyManager eco = mock(EconomyManager.class);
        when(eco.getBalance(any(Player.class))).thenReturn(50.0);
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(eco);
        Player player = mock(Player.class);
        when(player.getName()).thenReturn("Alice");
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.singletonMap("Alice", player), new OfflinePlayer[0]);

        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"economy", "take", "Alice", "100"});
        assertTrue(handled);
        assertTrue(messages.stream().anyMatch(m -> m.contains("Player only has $") && m.contains("50.00")));
        verify(eco, never()).withdraw(any(Player.class), anyDouble());
    }

    @Test
    void economy_give_failure_sends_failed_message() {
        EconomyManager eco = mock(EconomyManager.class);
        when(eco.deposit(any(Player.class), anyDouble())).thenReturn(false);
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(eco);
        Player player = mock(Player.class);
        when(player.getName()).thenReturn("Alice");
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.singletonMap("Alice", player), new OfflinePlayer[0]);

        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"economy", "give", "Alice", "10"});
        assertTrue(handled);
        assertTrue(messages.stream().anyMatch(m -> m.contains("Failed to execute economy command!")));
    }

    @Test
    void economy_set_online_success_sends_message() {
        EconomyManager eco = mock(EconomyManager.class);
        when(eco.getBalance(any(Player.class))).thenReturn(25.0);
        when(eco.withdraw(any(Player.class), anyDouble())).thenReturn(true);
        when(eco.deposit(any(Player.class), anyDouble())).thenReturn(true);
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(eco);
        Player player = mock(Player.class);
        when(player.getName()).thenReturn("Alice");
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.singletonMap("Alice", player), new OfflinePlayer[0]);

        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"economy", "set", "Alice", "100"});
        assertTrue(handled);
        assertTrue(messages.stream().anyMatch(m -> m.contains("Set Alice's balance to $100.00")));
    }

    @Test
    void economy_large_amount_triggers_confirmation_prompt_and_no_execution() {
        EconomyManager eco = mock(EconomyManager.class);
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(eco);
        Player target = mock(Player.class);
        when(target.getName()).thenReturn("Alice");
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.singletonMap("Alice", target), new OfflinePlayer[0]);

        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"economy", "give", "Alice", "100000"});
        assertTrue(handled);
        assertTrue(messages.stream().anyMatch(m -> m.contains("Large amount detected: $")));
        assertTrue(messages.stream().anyMatch(m -> m.toLowerCase().contains("use") && m.contains("/djeconomy confirm")));
        verify(eco, never()).deposit(any(Player.class), anyDouble());
        verify(eco, never()).withdraw(any(Player.class), anyDouble());
    }

    @Test
    void economy_set_offline_success_sends_message() {
        EconomyManager eco = mock(EconomyManager.class);
        when(eco.getBalance(any(OfflinePlayer.class))).thenReturn(200.0);
        when(eco.withdraw(any(OfflinePlayer.class), anyDouble())).thenReturn(true);
        when(eco.depositPlayer(any(OfflinePlayer.class), anyDouble())).thenReturn(true);
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(eco);

        OfflinePlayer off = mock(OfflinePlayer.class);
        when(off.getName()).thenReturn("Bob");
        when(off.hasPlayedBefore()).thenReturn(true);

        AdminCommand admin = new TestableAdminCommand(plugin, Collections.emptyMap(), new OfflinePlayer[]{off});

        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"economy", "set", "Bob", "100"});
        assertTrue(handled);
        assertTrue(messages.stream().anyMatch(m -> m.contains("Set Bob's balance to $100.00")));
    }

    // === getlevel tests ===
    @Test
    void getlevel_unknown_player_shows_error() {
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(null);
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.emptyMap(), new OfflinePlayer[0]);
        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);
        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy",
                new String[]{"getlevel", "Ghost", "miner"});
        assertTrue(handled);
        assertTrue(messages.stream().anyMatch(m -> m.contains("not found or has never joined")));
    }

    @Test
    void getlevel_unknown_job_shows_error() {
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(null);
        JobManager jobMgr = mock(JobManager.class);
        when(plugin.getJobManager()).thenReturn(jobMgr);
        Player alice = mock(Player.class); when(alice.getName()).thenReturn("Alice");
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.singletonMap("Alice", alice), new OfflinePlayer[0]);
        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);
        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy",
                new String[]{"getlevel", "Alice", "miner"});
        assertTrue(handled);
        assertTrue(messages.stream().anyMatch(m -> m.contains("Unknown job 'miner'")));
    }

    @Test
    void getlevel_online_shows_level_and_online_suffix() {
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(null);
        JobManager jobMgr = mock(JobManager.class);
        when(plugin.getJobManager()).thenReturn(jobMgr);
        Job job = mock(Job.class); when(job.getName()).thenReturn("Miner");
        when(jobMgr.getJob(eq("miner"))).thenReturn(job);
        when(jobMgr.getJobLevel(any(Player.class), eq("Miner"))).thenReturn(5);
        Player alice = mock(Player.class); when(alice.getName()).thenReturn("Alice");
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.singletonMap("Alice", alice), new OfflinePlayer[0]);
        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);
        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy",
                new String[]{"getlevel", "Alice", "miner"});
        assertTrue(handled);
        assertTrue(messages.stream().anyMatch(m -> m.contains("Alice's 'Miner' level is 5") && m.contains("(online)")));
    }

    @Test
    void getlevel_offline_not_joined_job_shows_message() {
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(null);
        JobManager jobMgr = mock(JobManager.class);
        when(plugin.getJobManager()).thenReturn(jobMgr);
        Job job = mock(Job.class); when(job.getName()).thenReturn("Miner");
        when(jobMgr.getJob(eq("miner"))).thenReturn(job);
        when(jobMgr.getOfflineJobLevel(any(OfflinePlayer.class), eq("Miner"))).thenReturn(null);
        OfflinePlayer off = mock(OfflinePlayer.class); when(off.getName()).thenReturn("Bob"); when(off.hasPlayedBefore()).thenReturn(true);
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.emptyMap(), new OfflinePlayer[]{off});
        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);
        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy",
                new String[]{"getlevel", "Bob", "miner"});
        assertTrue(handled);
        assertTrue(messages.stream().anyMatch(m -> m.contains("has not joined the job 'Miner'")));
    }

    @Test
    void getlevel_offline_shows_level_and_offline_suffix() {
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(null);
        JobManager jobMgr = mock(JobManager.class);
        when(plugin.getJobManager()).thenReturn(jobMgr);
        Job job = mock(Job.class); when(job.getName()).thenReturn("Miner");
        when(jobMgr.getJob(eq("miner"))).thenReturn(job);
        when(jobMgr.getOfflineJobLevel(any(OfflinePlayer.class), eq("Miner"))).thenReturn(3);
        OfflinePlayer off = mock(OfflinePlayer.class); when(off.getName()).thenReturn("Bob"); when(off.hasPlayedBefore()).thenReturn(true);
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.emptyMap(), new OfflinePlayer[]{off});
        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);
        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy",
                new String[]{"getlevel", "Bob", "miner"});
        assertTrue(handled);
        assertTrue(messages.stream().anyMatch(m -> m.contains("Bob's 'Miner' level is 3") && m.contains("(offline)")));
    }

    // === resetlevel tests ===
    @Test
    void resetlevel_online_success_and_message() {
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(null);
        JobManager jobMgr = mock(JobManager.class);
        when(plugin.getJobManager()).thenReturn(jobMgr);
        when(jobMgr.setJobLevel(any(Player.class), eq("miner"), eq(1))).thenReturn(true);
        Job job = mock(Job.class); when(job.getName()).thenReturn("Miner");
        when(jobMgr.getJob(eq("miner"))).thenReturn(job);
        Player alice = mock(Player.class); when(alice.getName()).thenReturn("Alice");
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.singletonMap("Alice", alice), new OfflinePlayer[0]);
        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);
        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy",
                new String[]{"resetlevel", "Alice", "miner"});
        assertTrue(handled);
        verify(jobMgr, times(1)).setJobLevel(eq(alice), eq("miner"), eq(1));
        assertTrue(messages.stream().anyMatch(m -> m.contains("Reset Alice's 'Miner' level to 1 (online)")));
    }

    @Test
    void resetlevel_offline_unknown_job_shows_error() {
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(null);
        JobManager jobMgr = mock(JobManager.class);
        when(plugin.getJobManager()).thenReturn(jobMgr);
        when(jobMgr.setOfflineJobLevel(any(OfflinePlayer.class), eq("miner"), eq(1))).thenReturn(false);
        OfflinePlayer off = mock(OfflinePlayer.class); when(off.getName()).thenReturn("Bob"); when(off.hasPlayedBefore()).thenReturn(true);
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.emptyMap(), new OfflinePlayer[]{off});
        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);
        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy",
                new String[]{"resetlevel", "Bob", "miner"});
        assertTrue(handled);
        assertTrue(messages.stream().anyMatch(m -> m.contains("Unknown job 'miner'")));
    }

    // === history tests ===
    @Test
    void history_no_file_shows_no_history_message() throws Exception {
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(null);
        // temp data folder
        Path tmp = Files.createTempDirectory("dje-test");
        File dataFolder = tmp.toFile();
        when(plugin.getDataFolder()).thenReturn(dataFolder);
        Player alice = mock(Player.class); when(alice.getName()).thenReturn("Alice");
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.singletonMap("Alice", alice), new OfflinePlayer[0]);
        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);
        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy",
                new String[]{"history", "Alice"});
        assertTrue(handled);
        assertTrue(messages.stream().anyMatch(m -> m.contains("No history found for 'Alice'")));
    }

    @Test
    void history_with_entries_filters_by_player_and_honors_limit() throws Exception {
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(null);
        Path tmp = Files.createTempDirectory("dje-test");
        File dataFolder = tmp.toFile();
        when(plugin.getDataFolder()).thenReturn(dataFolder);
        // Create history file with mixed entries
        File hist = new File(dataFolder, "admin-economy-history.log");
        List<String> lines = Arrays.asList(
                String.format("%d|Admin|GIVE|Alice|10.00", System.currentTimeMillis()-1000),
                String.format("%d|Admin|TAKE|Bob|5.00", System.currentTimeMillis()-900),
                String.format("%d|Admin|SET|Alice|20.00", System.currentTimeMillis()-800),
                String.format("%d|Other|GIVE|Alice|30.00", System.currentTimeMillis()-700)
        );
        Files.write(hist.toPath(), lines, StandardCharsets.UTF_8);

        Player alice = mock(Player.class); when(alice.getName()).thenReturn("Alice");
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.singletonMap("Alice", alice), new OfflinePlayer[0]);
        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);
        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy",
                new String[]{"history", "Alice", "2"});
        assertTrue(handled);
        assertTrue(messages.stream().anyMatch(m -> m.contains("Showing last 2 entr")));
        // Should contain lines mentioning Alice and amounts
        assertTrue(messages.stream().anyMatch(m -> m.contains("GIVE Alice") || m.contains("SET Alice")));
    }

    // === tab-complete additions for new commands ===
    @Test
    void tabcomplete_getlevel_and_resetlevel_job_names_and_history_limits() {
        DynamicJobsEconomy plugin = setupPluginWithConfigAndEconomy(null);
        JobManager jobMgr = mock(JobManager.class);
        when(plugin.getJobManager()).thenReturn(jobMgr);
        Map<String, Job> jobs = new HashMap<>();
        Job miner = mock(Job.class); when(miner.getName()).thenReturn("Miner"); jobs.put("Miner", miner);
        Job farmer = mock(Job.class); when(farmer.getName()).thenReturn("Farmer"); jobs.put("Farmer", farmer);
        when(jobMgr.getJobs()).thenReturn(jobs);

        Player alice = mock(Player.class); when(alice.getName()).thenReturn("Alice");
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.singletonMap("Alice", alice), new OfflinePlayer[0]);

        List<String> sugg1 = admin.onTabComplete(alice, mock(Command.class), "djeconomy",
                new String[]{"getlevel", "Alice", "m"});
        assertTrue(sugg1.contains("Miner") && !sugg1.contains("Farmer"));

        List<String> sugg2 = admin.onTabComplete(alice, mock(Command.class), "djeconomy",
                new String[]{"resetlevel", "Alice", ""});
        assertTrue(sugg2.contains("Miner") && sugg2.contains("Farmer"));

        List<String> h = admin.onTabComplete(alice, mock(Command.class), "djeconomy",
                new String[]{"history", "Alice", "1"});
        assertTrue(h.contains("10"));
    }
}
