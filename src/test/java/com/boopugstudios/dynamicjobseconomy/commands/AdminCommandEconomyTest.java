package com.boopugstudios.dynamicjobseconomy.commands;

import com.boopugstudios.dynamicjobseconomy.DynamicJobsEconomy;
import com.boopugstudios.dynamicjobseconomy.economy.EconomyManager;
import com.boopugstudios.dynamicjobseconomy.jobs.JobManager;
import com.boopugstudios.dynamicjobseconomy.jobs.Job;
import com.boopugstudios.dynamicjobseconomy.jobs.PlayerJobData;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
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
}
