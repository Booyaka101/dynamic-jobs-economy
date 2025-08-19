package com.boopugstudios.dynamicjobseconomy.commands;

import com.boopugstudios.dynamicjobseconomy.DynamicJobsEconomy;
import com.boopugstudios.dynamicjobseconomy.jobs.JobManager;
import com.boopugstudios.dynamicjobseconomy.jobs.Job;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminCommandTabCompleteTest {

    private static class TestableAdminCommand extends AdminCommand {
        private final List<Player> online;
        TestableAdminCommand(DynamicJobsEconomy plugin, List<Player> online) {
            super(plugin);
            this.online = online;
        }

        @Override
        protected Collection<? extends Player> getOnlinePlayers() {
            return online;
        }
    }

    @Test
    void subcommandSuggestions_filterByPrefix() {
        DynamicJobsEconomy plugin = mock(DynamicJobsEconomy.class);
        AdminCommand admin = new TestableAdminCommand(plugin, Collections.emptyList());

        // No prefix
        List<String> all = admin.onTabComplete(null, mock(Command.class), "djeconomy", new String[]{""});
        assertTrue(all.containsAll(Arrays.asList("reload", "setlevel", "addxp", "economy", "refreshjobs", "invalidatejobs")));

        // With prefix
        List<String> re = admin.onTabComplete(null, mock(Command.class), "djeconomy", new String[]{"re"});
        assertEquals(Arrays.asList("reload", "refreshjobs"), re);
    }

    @Test
    void economyActionSuggestions_filterByPrefix() {
        DynamicJobsEconomy plugin = mock(DynamicJobsEconomy.class);
        AdminCommand admin = new AdminCommand(plugin);

        List<String> all = admin.onTabComplete(null, mock(Command.class), "djeconomy", new String[]{"economy", ""});
        assertTrue(all.containsAll(Arrays.asList("give", "take", "set")));

        List<String> onlySet = admin.onTabComplete(null, mock(Command.class), "djeconomy", new String[]{"economy", "s"});
        assertEquals(Collections.singletonList("set"), onlySet);
    }

    @Test
    void jobNameSuggestions_forSetlevelAndAddxp() {
        DynamicJobsEconomy plugin = mock(DynamicJobsEconomy.class);
        JobManager jm = mock(JobManager.class);
        when(plugin.getJobManager()).thenReturn(jm);

        Map<String, Job> jobs = new HashMap<>();
        // Only keys are used by AdminCommand via JobNameUtil.suggestJobs
        jobs.put("Miner", new Job("Miner", "Miner", "desc", 1, 1, 100));
        jobs.put("Farmer", new Job("Farmer", "Farmer", "desc", 1, 1, 100));
        jobs.put("Builder", new Job("Builder", "Builder", "desc", 1, 1, 100));
        when(jm.getJobs()).thenReturn(jobs);

        AdminCommand admin = new AdminCommand(plugin);

        // setlevel ... <job>
        List<String> suggestions = admin.onTabComplete(null, mock(Command.class), "djeconomy", new String[]{"setlevel", "Alice", "m"});
        assertEquals(Collections.singletonList("Miner"), suggestions);

        // addxp ... <job>
        List<String> suggestions2 = admin.onTabComplete(null, mock(Command.class), "djeconomy", new String[]{"addxp", "Alice", "F"});
        assertEquals(Collections.singletonList("Farmer"), suggestions2);
    }

    @Test
    void economyPlayerSuggestions_thirdArg() {
        DynamicJobsEconomy plugin = mock(DynamicJobsEconomy.class);
        // AdminCommand instance will be created with test seam below

        Player alice = mock(Player.class);
        when(alice.getName()).thenReturn("Alice");
        Player bob = mock(Player.class);
        when(bob.getName()).thenReturn("Bob");

        AdminCommand admin = new TestableAdminCommand(plugin, Arrays.asList(alice, bob));

        List<String> suggestions = admin.onTabComplete(null, mock(Command.class), "djeconomy",
                new String[]{"economy", "give", "a"});
        assertEquals(Collections.singletonList("Alice"), suggestions);
    }

    @Test
    void playerSuggestions_secondArg_forVariousCommands() {
        DynamicJobsEconomy plugin = mock(DynamicJobsEconomy.class);
        // AdminCommand instance will be created with test seam below

        Player alice = mock(Player.class);
        when(alice.getName()).thenReturn("Alice");
        Player bob = mock(Player.class);
        when(bob.getName()).thenReturn("Bob");

        AdminCommand admin = new TestableAdminCommand(plugin, Arrays.asList(alice, bob));

        // setlevel <player>
        List<String> s1 = admin.onTabComplete(null, mock(Command.class), "djeconomy",
                new String[]{"setlevel", "a"});
        assertEquals(Collections.singletonList("Alice"), s1);

        // addxp <player>
        List<String> s2 = admin.onTabComplete(null, mock(Command.class), "djeconomy",
                new String[]{"addxp", "A"});
        assertEquals(Collections.singletonList("Alice"), s2);

        // refreshjobs <player>
        List<String> s3 = admin.onTabComplete(null, mock(Command.class), "djeconomy",
                new String[]{"refreshjobs", "b"});
        assertEquals(Collections.singletonList("Bob"), s3);

        // invalidatejobs <player>
        List<String> s4 = admin.onTabComplete(null, mock(Command.class), "djeconomy",
                new String[]{"invalidatejobs", "B"});
        assertEquals(Collections.singletonList("Bob"), s4);
    }
}
