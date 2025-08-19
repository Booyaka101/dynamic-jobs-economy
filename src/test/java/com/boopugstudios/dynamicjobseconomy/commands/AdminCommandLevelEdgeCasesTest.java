package com.boopugstudios.dynamicjobseconomy.commands;

import com.boopugstudios.dynamicjobseconomy.DynamicJobsEconomy;
import com.boopugstudios.dynamicjobseconomy.jobs.Job;
import com.boopugstudios.dynamicjobseconomy.jobs.JobManager;
import com.boopugstudios.dynamicjobseconomy.jobs.PlayerJobData;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdminCommandLevelEdgeCasesTest {

    private static class TestableAdminCommand extends AdminCommand {
        private final Map<String, Player> byName;
        private final OfflinePlayer[] offline;
        TestableAdminCommand(DynamicJobsEconomy plugin, Map<String, Player> byName, OfflinePlayer[] offline) {
            super(plugin);
            this.byName = byName;
            this.offline = offline;
        }
        protected Player getPlayerByName(String name) { return byName.get(name); }
        protected OfflinePlayer[] getOfflinePlayersArray() { return offline; }
        protected Collection<? extends Player> getOnlinePlayers() { return byName.values(); }
    }

    private DynamicJobsEconomy setupPlugin(JobManager jm) {
        DynamicJobsEconomy plugin = mock(DynamicJobsEconomy.class);
        FileConfiguration cfg = mock(FileConfiguration.class);
        when(plugin.getConfig()).thenReturn(cfg);
        when(cfg.getString(eq("messages.prefix"), anyString())).thenAnswer(inv -> inv.getArgument(1));
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        if (jm != null) when(plugin.getJobManager()).thenReturn(jm);
        // Data folder for any file operations (not used here)
        when(plugin.getDataFolder()).thenReturn(new File(System.getProperty("java.io.tmpdir"), "dje-test"));
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
    void setlevel_invalidLevel_nonNumeric() {
        JobManager jm = mock(JobManager.class);
        DynamicJobsEconomy plugin = setupPlugin(jm);
        Player alice = mock(Player.class); when(alice.getName()).thenReturn("Alice");
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.singletonMap("Alice", alice), new OfflinePlayer[0]);

        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy",
                new String[]{"setlevel", "Alice", "Miner", "abc"});
        assertTrue(handled);
        assertTrue(messages.stream().anyMatch(m -> m.contains("Invalid level number")));
        verify(jm, never()).setJobLevel(any(Player.class), anyString(), anyInt());
    }

    @Test
    void setlevel_largeLevel_succeeds() {
        JobManager jm = mock(JobManager.class);
        when(jm.setJobLevel(any(Player.class), anyString(), anyInt())).thenReturn(true);
        DynamicJobsEconomy plugin = setupPlugin(jm);
        Player alice = mock(Player.class); when(alice.getName()).thenReturn("Alice");
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.singletonMap("Alice", alice), new OfflinePlayer[0]);

        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy",
                new String[]{"setlevel", "Alice", "Miner", "9999"});
        assertTrue(handled);
        verify(jm, times(1)).setJobLevel(eq(alice), eq("Miner"), eq(9999));
        assertTrue(messages.stream().anyMatch(m -> m.contains("level to 9999")));
    }

    @Test
    void addxp_invalidAmount_nonNumeric() {
        JobManager jm = mock(JobManager.class);
        DynamicJobsEconomy plugin = setupPlugin(jm);
        Player alice = mock(Player.class); when(alice.getName()).thenReturn("Alice");
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.singletonMap("Alice", alice), new OfflinePlayer[0]);

        // Job resolution mocks
        Job miner = new Job("Miner", "Miner", "desc", 1, 1, 100);
        when(jm.getJob(eq("Miner"))).thenReturn(miner);
        PlayerJobData pdata = mock(PlayerJobData.class);
        when(jm.getPlayerData(eq(alice))).thenReturn(pdata);
        when(pdata.hasJob(eq("Miner"))).thenReturn(true);

        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy",
                new String[]{"addxp", "Alice", "Miner", "abc"});
        assertTrue(handled);
        assertTrue(messages.stream().anyMatch(m -> m.contains("Invalid XP amount")));
        verify(jm, never()).addExperience(any(Player.class), anyString(), anyInt());
    }

    @Test
    void addxp_offlinePlayer_rejected() {
        JobManager jm = mock(JobManager.class);
        DynamicJobsEconomy plugin = setupPlugin(jm);
        OfflinePlayer off = mock(OfflinePlayer.class); when(off.getName()).thenReturn("Alice"); when(off.hasPlayedBefore()).thenReturn(true);
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.emptyMap(), new OfflinePlayer[]{off});

        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy",
                new String[]{"addxp", "Alice", "Miner", "10"});
        assertTrue(handled);
        assertTrue(messages.stream().anyMatch(m -> m.contains("Cannot add XP to offline player")));
        verify(jm, never()).addExperience(any(Player.class), anyString(), anyInt());
    }

    @Test
    void addxp_validLargeValue_succeeds() {
        JobManager jm = mock(JobManager.class);
        DynamicJobsEconomy plugin = setupPlugin(jm);
        Player alice = mock(Player.class); when(alice.getName()).thenReturn("Alice");
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.singletonMap("Alice", alice), new OfflinePlayer[0]);

        Job miner = new Job("Miner", "Miner", "desc", 1, 1, 100);
        when(jm.getJob(eq("Miner"))).thenReturn(miner);
        PlayerJobData pdata = mock(PlayerJobData.class);
        when(jm.getPlayerData(eq(alice))).thenReturn(pdata);
        when(pdata.hasJob(eq("Miner"))).thenReturn(true);

        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy",
                new String[]{"addxp", "Alice", "Miner", "1000000"});
        assertTrue(handled);
        verify(jm, times(1)).addExperience(eq(alice), eq("Miner"), eq(1_000_000));
        assertTrue(messages.stream().anyMatch(m -> m.contains("Added 1000000 XP")));
    }

    @Test
    void addxp_playerNotInJob_rejected() {
        JobManager jm = mock(JobManager.class);
        DynamicJobsEconomy plugin = setupPlugin(jm);
        Player alice = mock(Player.class); when(alice.getName()).thenReturn("Alice");
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.singletonMap("Alice", alice), new OfflinePlayer[0]);

        Job miner = new Job("Miner", "Miner", "desc", 1, 1, 100);
        when(jm.getJob(eq("Miner"))).thenReturn(miner);
        PlayerJobData pdata = mock(PlayerJobData.class);
        when(jm.getPlayerData(eq(alice))).thenReturn(pdata);
        when(pdata.hasJob(eq("Miner"))).thenReturn(false);

        List<String> messages = new ArrayList<>();
        Player sender = mockSenderCollectingMessages(messages);

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy",
                new String[]{"addxp", "Alice", "Miner", "10"});
        assertTrue(handled);
        assertTrue(messages.stream().anyMatch(m -> m.contains("has not joined the job 'Miner'")));
        verify(jm, never()).addExperience(any(Player.class), anyString(), anyInt());
    }
}
