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
        assertTrue(all.contains("reload") && all.contains("setlevel") && all.contains("addxp")
                && all.contains("economy") && all.contains("refreshjobs") && all.contains("invalidatejobs"));

        List<String> re = admin.onTabComplete(sender, mock(Command.class), "djeconomy",
                new String[]{"re"});
        assertTrue(re.contains("reload") && re.contains("refreshjobs") && re.size() == 2);

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
}
