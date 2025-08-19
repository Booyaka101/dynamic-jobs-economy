package com.boopugstudios.dynamicjobseconomy.commands;

import com.boopugstudios.dynamicjobseconomy.DynamicJobsEconomy;
import com.boopugstudios.dynamicjobseconomy.i18n.Messages;
import com.boopugstudios.dynamicjobseconomy.jobs.JobManager;
import com.boopugstudios.dynamicjobseconomy.economy.EconomyManager;
import com.boopugstudios.dynamicjobseconomy.jobs.Job;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.OfflinePlayer;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdminCommandMessagesIntegrationTest {

    private File newTempDir() {
        File dir = new File(System.getProperty("java.io.tmpdir"), "dje-admin-i18n-" + System.nanoTime());
        assertTrue(dir.mkdirs() || dir.exists());
        return dir;
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
        CommandSender asConsole() {
            CommandSender s = mock(CommandSender.class);
            when(s.hasPermission(anyString())).thenReturn(true);
            doAnswer(inv -> { list.add(inv.getArgument(0)); return null; }).when(s).sendMessage(anyString());
            return s;
        }
    }

    @Test
    void history_none_uses_messages_yml_prefix_and_message_when_no_config_prefix() throws Exception {
        // Arrange: messages.yml with a prefix and history_none
        File dataDir = newTempDir();
        File msgFile = new File(dataDir, "messages.yml");
        try (FileWriter w = new FileWriter(msgFile, StandardCharsets.UTF_8)) {
            w.write("messages:\n");
            w.write("  prefix: \"&8[&6I18N&8] \"\n");
            w.write("admin:\n");
            w.write("  history_none: \"&7No history for %player%.\"\n");
        }

        DynamicJobsEconomy plugin = mock(DynamicJobsEconomy.class);
        FileConfiguration cfg = mock(FileConfiguration.class);
        // Explicitly ensure config does not provide a prefix
        when(cfg.getString(eq("messages.prefix"), isNull())).thenReturn(null);
        when(plugin.getConfig()).thenReturn(cfg);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        when(plugin.getDataFolder()).thenReturn(dataDir);

        // Wire Messages into plugin mock
        Messages messages = new Messages(plugin);
        messages.load();
        when(plugin.getMessages()).thenReturn(messages);

        AdminCommand admin = new AdminCommand(plugin);
        MsgCollector out = new MsgCollector();
        Player sender = out.asPlayerWithPerms();

        // Act: run history for a player with no history file
        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"history", "Bob"});

        // Assert:
        assertTrue(handled);
        assertTrue(out.list.stream().anyMatch(m -> m.startsWith("§8[§6I18N§8] ")));
        assertTrue(out.list.stream().anyMatch(m -> m.contains("No history for Bob.")));
    }

    @Test
    void config_prefix_overrides_messages_yml_prefix() throws Exception {
        // Arrange: messages.yml with prefix, but config supplies its own prefix
        File dataDir = newTempDir();
        File msgFile = new File(dataDir, "messages.yml");
        try (FileWriter w = new FileWriter(msgFile, StandardCharsets.UTF_8)) {
            w.write("messages:\n");
            w.write("  prefix: \"&8[&6I18N&8] \"\n");
            w.write("admin:\n");
            w.write("  history_none: \"&7No history for %player%.\"\n");
        }

        DynamicJobsEconomy plugin = mock(DynamicJobsEconomy.class);
        FileConfiguration cfg = mock(FileConfiguration.class);
        // Config provides a raw (already colored) prefix override for backward compatibility
        when(cfg.getString(eq("messages.prefix"), isNull())).thenReturn("[CFG] ");
        when(plugin.getConfig()).thenReturn(cfg);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        when(plugin.getDataFolder()).thenReturn(dataDir);

        Messages messages = new Messages(plugin);
        messages.load();
        when(plugin.getMessages()).thenReturn(messages);

        AdminCommand admin = new AdminCommand(plugin);
        MsgCollector out = new MsgCollector();
        Player sender = out.asPlayerWithPerms();

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"history", "Bob"});

        assertTrue(handled);
        // Should use config prefix instead of messages.yml
        assertTrue(out.list.stream().anyMatch(m -> m.startsWith("[CFG] ")));
        assertFalse(out.list.stream().anyMatch(m -> m.startsWith("§8[§6I18N§8] "))); // ensure override happened
    }

    @Test
    void help_uses_messages_yml_entries() throws Exception {
        // Arrange: messages.yml with distinct help lines
        File dataDir = newTempDir();
        File msgFile = new File(dataDir, "messages.yml");
        try (FileWriter w = new FileWriter(msgFile, StandardCharsets.UTF_8)) {
            w.write("messages:\n");
            w.write("  prefix: \"&8[&6I18N&8] \"\n");
            w.write("admin:\n");
            w.write("  help:\n");
            w.write("    header: \"&6HELP-HEADER\"\n");
            w.write("    reload: \"&f/cmd reload - from i18n\"\n");
        }

        DynamicJobsEconomy plugin = mock(DynamicJobsEconomy.class);
        FileConfiguration cfg = mock(FileConfiguration.class);
        when(cfg.getString(eq("messages.prefix"), isNull())).thenReturn(null);
        when(plugin.getConfig()).thenReturn(cfg);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        when(plugin.getDataFolder()).thenReturn(dataDir);

        Messages messages = new Messages(plugin);
        messages.load();
        when(plugin.getMessages()).thenReturn(messages);

        AdminCommand admin = new AdminCommand(plugin);
        MsgCollector out = new MsgCollector();
        Player sender = out.asPlayerWithPerms();

        // Act: run base command with no args to show help
        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{});

        // Assert
        assertTrue(handled);
        assertTrue(out.list.stream().anyMatch(m -> m.contains("§6HELP-HEADER")));
        assertTrue(out.list.stream().anyMatch(m -> m.contains("/cmd reload - from i18n")));
    }

    @Test
    void history_read_failure_uses_messages_yml_key() throws Exception {
        // Arrange: create messages with custom read-failed text
        File dataDir = newTempDir();
        File msgFile = new File(dataDir, "messages.yml");
        try (FileWriter w = new FileWriter(msgFile, StandardCharsets.UTF_8)) {
            w.write("messages:\n");
            w.write("  prefix: \"&8[&6I18N&8] \"\n");
            w.write("admin:\n");
            w.write("  history_read_failed: \"&cERR %error%\"\n");
        }

        // Create a DIRECTORY where the history file should be, to force an IOException on read
        File historyDirAsFile = new File(dataDir, "admin-economy-history.log");
        assertTrue(historyDirAsFile.mkdirs() || historyDirAsFile.exists());

        DynamicJobsEconomy plugin = mock(DynamicJobsEconomy.class);
        FileConfiguration cfg = mock(FileConfiguration.class);
        when(cfg.getString(eq("messages.prefix"), isNull())).thenReturn(null);
        when(plugin.getConfig()).thenReturn(cfg);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        when(plugin.getDataFolder()).thenReturn(dataDir);

        Messages messages = new Messages(plugin);
        messages.load();
        when(plugin.getMessages()).thenReturn(messages);

        AdminCommand admin = new AdminCommand(plugin);
        MsgCollector out = new MsgCollector();
        Player sender = out.asPlayerWithPerms();

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"history", "Bob"});

        assertTrue(handled);
        assertTrue(out.list.stream().anyMatch(m -> m.startsWith("§8[§6I18N§8] ")));
        assertTrue(out.list.stream().anyMatch(m -> m.contains("ERR ")));
    }

    @Test
    void confirmation_prompt_and_expiry_messages() throws Exception {
        // Arrange messages for confirmation
        File dataDir = newTempDir();
        File msgFile = new File(dataDir, "messages.yml");
        try (FileWriter w = new FileWriter(msgFile, StandardCharsets.UTF_8)) {
            w.write("messages:\n");
            w.write("  prefix: \"&8[&6I18N&8] \"\n");
            w.write("admin:\n");
            w.write("  large_detected: \"&eWARN %amount%\"\n");
            w.write("  confirm_prompt: \"&eCONFIRM in %seconds%\"\n");
            w.write("  confirm_expired: \"&cEXPIRED\"\n");
            w.write("  player_not_found: \"&cNope %player%\"\n");
        }

        DynamicJobsEconomy plugin = mock(DynamicJobsEconomy.class);
        FileConfiguration cfg = mock(FileConfiguration.class);
        when(cfg.getString(eq("messages.prefix"), isNull())).thenReturn(null);
        when(cfg.getDouble(eq("economy.admin_confirmation.threshold"), anyDouble())).thenReturn(1.0);
        when(cfg.getInt(eq("economy.admin_confirmation.expiry_seconds"), anyInt())).thenReturn(1);
        when(plugin.getConfig()).thenReturn(cfg);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        when(plugin.getDataFolder()).thenReturn(dataDir);

        Messages messages = new Messages(plugin);
        messages.load();
        when(plugin.getMessages()).thenReturn(messages);

        // AdminCommand with seams overridden to supply an offline player "Bob"
        class TestAdmin extends AdminCommand {
            long now = System.currentTimeMillis();
            final OfflinePlayer[] offs;
            TestAdmin(DynamicJobsEconomy p, OfflinePlayer[] o) { super(p); this.offs = o; }
            @Override protected long nowMillis() { return now; }
            @Override protected Player getPlayerByName(String name) { return null; }
            @Override protected OfflinePlayer[] getOfflinePlayersArray() { return offs; }
        }
        OfflinePlayer off = mock(OfflinePlayer.class);
        when(off.getName()).thenReturn("Bob");
        when(off.hasPlayedBefore()).thenReturn(true);
        TestAdmin admin = new TestAdmin(plugin, new OfflinePlayer[]{off});

        MsgCollector out = new MsgCollector();
        Player sender = out.asPlayerWithPerms();

        // Act 1: issue large amount economy command to trigger confirmation
        boolean handled1 = admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"economy", "give", "Bob", "5"});
        assertTrue(handled1);
        assertTrue(out.list.stream().anyMatch(m -> m.contains("WARN ")));
        assertTrue(out.list.stream().anyMatch(m -> m.contains("CONFIRM in ")));

        // Act 2: advance time beyond expiry and run confirm
        admin.now += 1500; // > 1 second
        boolean handled2 = admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"confirm"});
        assertTrue(handled2);
        assertTrue(out.list.stream().anyMatch(m -> m.contains("EXPIRED")));
    }

    @Test
    void setlevel_online_success_uses_i18n_message() throws Exception {
        // Arrange: messages with setlevel_success
        File dataDir = newTempDir();
        try (FileWriter w = new FileWriter(new File(dataDir, "messages.yml"), StandardCharsets.UTF_8)) {
            w.write("messages:\n");
            w.write("  prefix: \"&8[&6I18N&8] \"\n");
            w.write("admin:\n");
            w.write("  setlevel_success: \"&aSet %player%'s %job% level to %level%%suffix%\"\n");
        }

        DynamicJobsEconomy plugin = mock(DynamicJobsEconomy.class);
        FileConfiguration cfg = mock(FileConfiguration.class);
        when(cfg.getString(eq("messages.prefix"), isNull())).thenReturn(null);
        when(plugin.getConfig()).thenReturn(cfg);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        when(plugin.getDataFolder()).thenReturn(dataDir);

        Messages messages = new Messages(plugin);
        messages.load();
        when(plugin.getMessages()).thenReturn(messages);

        // Mock JobManager interactions
        JobManager jm = mock(JobManager.class);
        when(plugin.getJobManager()).thenReturn(jm);
        when(jm.setJobLevel(any(Player.class), eq("miner"), eq(5))).thenReturn(true);
        Job job = mock(Job.class);
        when(job.getName()).thenReturn("Miner");
        when(jm.getJob(eq("miner"))).thenReturn(job);

        class TestAdmin extends AdminCommand {
            final Player p;
            TestAdmin(DynamicJobsEconomy plg, Player p) { super(plg); this.p = p; }
            @Override protected Player getPlayerByName(String name) { return p; }
            @Override protected OfflinePlayer[] getOfflinePlayersArray() { return new OfflinePlayer[0]; }
        }
        Player target = mock(Player.class);
        when(target.getUniqueId()).thenReturn(UUID.randomUUID());
        when(target.getName()).thenReturn("Alice");
        TestAdmin admin = new TestAdmin(plugin, target);

        MsgCollector out = new MsgCollector();
        Player sender = out.asPlayerWithPerms();

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"setlevel", "Alice", "miner", "5"});

        assertTrue(handled);
        assertTrue(out.list.stream().anyMatch(m -> m.startsWith("§8[§6I18N§8] ")));
        assertTrue(out.list.stream().anyMatch(m -> m.contains("Alice") && m.contains("level to 5")));
    }

    @Test
    void getlevel_offline_not_joined_uses_i18n_message() throws Exception {
        // Arrange messages
        File dataDir = newTempDir();
        try (FileWriter w = new FileWriter(new File(dataDir, "messages.yml"), StandardCharsets.UTF_8)) {
            w.write("messages:\n");
            w.write("  prefix: \"&8[&6I18N&8] \"\n");
            w.write("admin:\n");
            w.write("  not_joined_job: \"&c%player% has not joined the job '%job%'.\"\n");
        }

        DynamicJobsEconomy plugin = mock(DynamicJobsEconomy.class);
        FileConfiguration cfg = mock(FileConfiguration.class);
        when(cfg.getString(eq("messages.prefix"), isNull())).thenReturn(null);
        when(plugin.getConfig()).thenReturn(cfg);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        when(plugin.getDataFolder()).thenReturn(dataDir);

        Messages messages = new Messages(plugin);
        messages.load();
        when(plugin.getMessages()).thenReturn(messages);

        JobManager jm = mock(JobManager.class);
        when(plugin.getJobManager()).thenReturn(jm);
        Job job = mock(Job.class);
        when(job.getName()).thenReturn("Miner");
        when(jm.getJob(eq("miner"))).thenReturn(job);
        when(jm.getOfflineJobLevel(any(OfflinePlayer.class), anyString())).thenReturn(null);

        class TestAdmin extends AdminCommand {
            final OfflinePlayer off;
            TestAdmin(DynamicJobsEconomy p, OfflinePlayer o) { super(p); this.off = o; }
            @Override protected Player getPlayerByName(String name) { return null; }
            @Override protected OfflinePlayer[] getOfflinePlayersArray() { return new OfflinePlayer[]{off}; }
        }
        OfflinePlayer off = mock(OfflinePlayer.class);
        when(off.getName()).thenReturn("Bob");
        when(off.hasPlayedBefore()).thenReturn(true);
        TestAdmin admin = new TestAdmin(plugin, off);

        MsgCollector out = new MsgCollector();
        Player sender = out.asPlayerWithPerms();

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"getlevel", "Bob", "miner"});

        assertTrue(handled);
        assertTrue(out.list.stream().anyMatch(m -> m.contains("Bob") && m.contains("Miner") && m.contains("has not joined")));
    }

    @Test
    void resetlevel_unknown_job_uses_i18n_message() throws Exception {
        File dataDir = newTempDir();
        try (FileWriter w = new FileWriter(new File(dataDir, "messages.yml"), StandardCharsets.UTF_8)) {
            w.write("messages:\n");
            w.write("  prefix: \"&8[&6I18N&8] \"\n");
            w.write("admin:\n");
            w.write("  unknown_job: \"&cUnknown job '%job%'.\"\n");
        }

        DynamicJobsEconomy plugin = mock(DynamicJobsEconomy.class);
        FileConfiguration cfg = mock(FileConfiguration.class);
        when(cfg.getString(eq("messages.prefix"), isNull())).thenReturn(null);
        when(plugin.getConfig()).thenReturn(cfg);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        when(plugin.getDataFolder()).thenReturn(dataDir);

        Messages messages = new Messages(plugin);
        messages.load();
        when(plugin.getMessages()).thenReturn(messages);

        JobManager jm = mock(JobManager.class);
        when(plugin.getJobManager()).thenReturn(jm);
        // Unknown job -> getJob returns null
        when(jm.getJob(anyString())).thenReturn(null);

        class TestAdmin extends AdminCommand {
            final Player p;
            TestAdmin(DynamicJobsEconomy plg, Player p) { super(plg); this.p = p; }
            @Override protected Player getPlayerByName(String name) { return p; }
            @Override protected OfflinePlayer[] getOfflinePlayersArray() { return new OfflinePlayer[0]; }
        }
        Player target = mock(Player.class);
        when(target.getUniqueId()).thenReturn(UUID.randomUUID());
        when(target.getName()).thenReturn("Carl");
        TestAdmin admin = new TestAdmin(plugin, target);

        MsgCollector out = new MsgCollector();
        Player sender = out.asPlayerWithPerms();

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"resetlevel", "Carl", "miner"});

        assertTrue(handled);
        assertTrue(out.list.stream().anyMatch(m -> m.contains("Unknown job") && m.contains("miner")));
    }

    @Test
    void addxp_offline_rejected_uses_i18n_message() throws Exception {
        File dataDir = newTempDir();
        try (FileWriter w = new FileWriter(new File(dataDir, "messages.yml"), StandardCharsets.UTF_8)) {
            w.write("messages:\n");
            w.write("  prefix: \"&8[&6I18N&8] \"\n");
            w.write("admin:\n");
            w.write("  cannot_addxp_offline: \"&cCannot add XP to offline player!\"\n");
        }

        DynamicJobsEconomy plugin = mock(DynamicJobsEconomy.class);
        FileConfiguration cfg = mock(FileConfiguration.class);
        when(cfg.getString(eq("messages.prefix"), isNull())).thenReturn(null);
        when(plugin.getConfig()).thenReturn(cfg);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        when(plugin.getDataFolder()).thenReturn(dataDir);

        Messages messages = new Messages(plugin);
        messages.load();
        when(plugin.getMessages()).thenReturn(messages);

        class TestAdmin extends AdminCommand {
            final OfflinePlayer off;
            TestAdmin(DynamicJobsEconomy p, OfflinePlayer o) { super(p); this.off = o; }
            @Override protected Player getPlayerByName(String name) { return null; }
            @Override protected OfflinePlayer[] getOfflinePlayersArray() { return new OfflinePlayer[]{off}; }
        }
        OfflinePlayer off = mock(OfflinePlayer.class);
        when(off.getName()).thenReturn("Deb");
        when(off.hasPlayedBefore()).thenReturn(true);
        TestAdmin admin = new TestAdmin(plugin, off);

        MsgCollector out = new MsgCollector();
        Player sender = out.asPlayerWithPerms();

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"addxp", "Deb", "miner", "10"});

        assertTrue(handled);
        assertTrue(out.list.stream().anyMatch(m -> m.contains("Cannot add XP to offline player")));
    }

    @Test
    void refreshjobs_requires_online_and_success_messages() throws Exception {
        File dataDir = newTempDir();
        try (FileWriter w = new FileWriter(new File(dataDir, "messages.yml"), StandardCharsets.UTF_8)) {
            w.write("messages:\n");
            w.write("  prefix: \"&8[&6I18N&8] \"\n");
            w.write("admin:\n");
            w.write("  refreshjobs_requires_online: \"&cPlayer must be online to refresh job data!\"\n");
            w.write("  refreshjobs_success: \"&aRefreshed job data for %player%\"\n");
        }

        DynamicJobsEconomy plugin = mock(DynamicJobsEconomy.class);
        FileConfiguration cfg = mock(FileConfiguration.class);
        when(cfg.getString(eq("messages.prefix"), isNull())).thenReturn(null);
        when(plugin.getConfig()).thenReturn(cfg);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        when(plugin.getDataFolder()).thenReturn(dataDir);

        Messages messages = new Messages(plugin);
        messages.load();
        when(plugin.getMessages()).thenReturn(messages);

        JobManager jm = mock(JobManager.class);
        when(plugin.getJobManager()).thenReturn(jm);

        // Case 1: offline -> requires_online message
        class TestAdmin1 extends AdminCommand {
            final OfflinePlayer off;
            TestAdmin1(DynamicJobsEconomy p, OfflinePlayer o) { super(p); this.off = o; }
            @Override protected Player getPlayerByName(String name) { return null; }
            @Override protected OfflinePlayer[] getOfflinePlayersArray() { return new OfflinePlayer[]{off}; }
        }
        OfflinePlayer off = mock(OfflinePlayer.class);
        when(off.getName()).thenReturn("Frank");
        when(off.hasPlayedBefore()).thenReturn(true);
        TestAdmin1 admin1 = new TestAdmin1(plugin, off);
        MsgCollector out1 = new MsgCollector();
        Player sender1 = out1.asPlayerWithPerms();
        boolean handled1 = admin1.onCommand(sender1, mock(Command.class), "djeconomy", new String[]{"refreshjobs", "Frank"});
        assertTrue(handled1);
        assertTrue(out1.list.stream().anyMatch(m -> m.contains("must be online to refresh job data")));

        // Case 2: online -> success and method called
        class TestAdmin2 extends AdminCommand {
            final Player p;
            TestAdmin2(DynamicJobsEconomy p0, Player p) { super(p0); this.p = p; }
            @Override protected Player getPlayerByName(String name) { return p; }
            @Override protected OfflinePlayer[] getOfflinePlayersArray() { return new OfflinePlayer[0]; }
        }
        Player online = mock(Player.class);
        when(online.getUniqueId()).thenReturn(UUID.randomUUID());
        when(online.getName()).thenReturn("Eve");
        TestAdmin2 admin2 = new TestAdmin2(plugin, online);
        MsgCollector out2 = new MsgCollector();
        Player sender2 = out2.asPlayerWithPerms();
        boolean handled2 = admin2.onCommand(sender2, mock(Command.class), "djeconomy", new String[]{"refreshjobs", "Eve"});
        assertTrue(handled2);
        verify(jm, atLeastOnce()).refreshPlayerData(eq(online));
        assertTrue(out2.list.stream().anyMatch(m -> m.contains("Refreshed job data for Eve")));
    }

    @Test
    void invalidatejobs_requires_online_and_success_messages() throws Exception {
        File dataDir = newTempDir();
        try (FileWriter w = new FileWriter(new File(dataDir, "messages.yml"), StandardCharsets.UTF_8)) {
            w.write("messages:\n");
            w.write("  prefix: \"&8[&6I18N&8] \"\n");
            w.write("admin:\n");
            w.write("  invalidate_requires_online: \"&cPlayer must be online to invalidate cached job data!\"\n");
            w.write("  invalidate_success: \"&aInvalidated cached job data for %player%\"\n");
        }

        DynamicJobsEconomy plugin = mock(DynamicJobsEconomy.class);
        FileConfiguration cfg = mock(FileConfiguration.class);
        when(cfg.getString(eq("messages.prefix"), isNull())).thenReturn(null);
        when(plugin.getConfig()).thenReturn(cfg);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        when(plugin.getDataFolder()).thenReturn(dataDir);

        Messages messages = new Messages(plugin);
        messages.load();
        when(plugin.getMessages()).thenReturn(messages);

        JobManager jm = mock(JobManager.class);
        when(plugin.getJobManager()).thenReturn(jm);

        // Case 1: offline -> requires_online
        class TestAdmin1 extends AdminCommand {
            final OfflinePlayer off;
            TestAdmin1(DynamicJobsEconomy p, OfflinePlayer o) { super(p); this.off = o; }
            @Override protected Player getPlayerByName(String name) { return null; }
            @Override protected OfflinePlayer[] getOfflinePlayersArray() { return new OfflinePlayer[]{off}; }
        }
        OfflinePlayer off = mock(OfflinePlayer.class);
        when(off.getName()).thenReturn("Nina");
        when(off.hasPlayedBefore()).thenReturn(true);
        TestAdmin1 admin1 = new TestAdmin1(plugin, off);
        MsgCollector out1 = new MsgCollector();
        Player sender1 = out1.asPlayerWithPerms();
        boolean handled1 = admin1.onCommand(sender1, mock(Command.class), "djeconomy", new String[]{"invalidatejobs", "Nina"});
        assertTrue(handled1);
        assertTrue(out1.list.stream().anyMatch(m -> m.contains("must be online to invalidate cached job data")));

        // Case 2: online -> success and manager called
        class TestAdmin2 extends AdminCommand {
            final Player p;
            TestAdmin2(DynamicJobsEconomy p0, Player p) { super(p0); this.p = p; }
            @Override protected Player getPlayerByName(String name) { return p; }
            @Override protected OfflinePlayer[] getOfflinePlayersArray() { return new OfflinePlayer[0]; }
        }
        Player online = mock(Player.class);
        when(online.getUniqueId()).thenReturn(UUID.randomUUID());
        when(online.getName()).thenReturn("Omar");
        TestAdmin2 admin2 = new TestAdmin2(plugin, online);
        MsgCollector out2 = new MsgCollector();
        Player sender2 = out2.asPlayerWithPerms();
        boolean handled2 = admin2.onCommand(sender2, mock(Command.class), "djeconomy", new String[]{"invalidatejobs", "Omar"});
        assertTrue(handled2);
        verify(jm, atLeastOnce()).invalidatePlayerData(eq(online));
        assertTrue(out2.list.stream().anyMatch(m -> m.contains("Invalidated cached job data for Omar")));
    }

    @Test
    void history_usage_message_when_missing_args_uses_i18n() throws Exception {
        File dataDir = newTempDir();
        try (FileWriter w = new FileWriter(new File(dataDir, "messages.yml"), StandardCharsets.UTF_8)) {
            w.write("messages:\n");
            w.write("  prefix: \"&8[&6I18N&8] \"\n");
            w.write("admin:\n");
            w.write("  usage:\n");
            w.write("    history: \"&cUSE\"\n");
        }

        DynamicJobsEconomy plugin = mock(DynamicJobsEconomy.class);
        FileConfiguration cfg = mock(FileConfiguration.class);
        when(cfg.getString(eq("messages.prefix"), isNull())).thenReturn(null);
        when(plugin.getConfig()).thenReturn(cfg);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        when(plugin.getDataFolder()).thenReturn(dataDir);

        Messages messages = new Messages(plugin);
        messages.load();
        when(plugin.getMessages()).thenReturn(messages);

        AdminCommand admin = new AdminCommand(plugin);
        MsgCollector out = new MsgCollector();
        Player sender = out.asPlayerWithPerms();

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"history"});
        assertTrue(handled);
        assertTrue(out.list.stream().anyMatch(m -> m.startsWith("§8[§6I18N§8] ") && m.contains("USE")));
    }

    @Test
    void history_header_and_entries_respect_limit_and_placeholders() throws Exception {
        File dataDir = newTempDir();
        // Prepare messages.yml with header format
        try (FileWriter w = new FileWriter(new File(dataDir, "messages.yml"), StandardCharsets.UTF_8)) {
            w.write("messages:\n");
            w.write("  prefix: \"&8[&6I18N&8] \"\n");
            w.write("admin:\n");
            w.write("  history_header: \"&eHEAD %count% %player%\"\n");
        }

        // Create a history file with multiple entries (some for Bob, some for Alice)
        File hist = new File(dataDir, "admin-economy-history.log");
        try (FileWriter w = new FileWriter(hist, StandardCharsets.UTF_8)) {
            long now = System.currentTimeMillis();
            w.write(now + "|Admin|give|Bob|10.00\n");
            w.write((now + 1) + "|Admin|take|Bob|5.00\n");
            w.write((now + 2) + "|Root|give|Alice|7.00\n");
        }

        DynamicJobsEconomy plugin = mock(DynamicJobsEconomy.class);
        FileConfiguration cfg = mock(FileConfiguration.class);
        when(cfg.getString(eq("messages.prefix"), isNull())).thenReturn(null);
        when(plugin.getConfig()).thenReturn(cfg);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        when(plugin.getDataFolder()).thenReturn(dataDir);

        Messages messages = new Messages(plugin);
        messages.load();
        when(plugin.getMessages()).thenReturn(messages);

        AdminCommand admin = new AdminCommand(plugin);
        MsgCollector out = new MsgCollector();
        Player sender = out.asPlayerWithPerms();

        // Request last 2 entries for Bob
        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"history", "Bob", "2"});
        assertTrue(handled);
        // Header contains replaced placeholders
        assertTrue(out.list.stream().anyMatch(m -> m.startsWith("§8[§6I18N§8] ") && m.contains("HEAD 2 Bob")));
        // There should be two entry lines for Bob (contain amount marker $ and name)
        long bobLines = out.list.stream().filter(m -> m.contains("Bob") && m.contains("$")).count();
        assertTrue(bobLines >= 2);
    }

    @Test
    void history_existing_file_but_no_entries_for_player_shows_none() throws Exception {
        File dataDir = newTempDir();
        try (FileWriter w = new FileWriter(new File(dataDir, "messages.yml"), StandardCharsets.UTF_8)) {
            w.write("messages:\n");
            w.write("  prefix: \"&8[&6I18N&8] \"\n");
            w.write("admin:\n");
            w.write("  history_none: \"&7No history for %player%.\"\n");
        }

        // Create a history file that has entries only for Alice
        File hist = new File(dataDir, "admin-economy-history.log");
        try (FileWriter w = new FileWriter(hist, StandardCharsets.UTF_8)) {
            long now = System.currentTimeMillis();
            w.write(now + "|Admin|give|Alice|10.00\n");
            w.write((now + 1) + "|Admin|take|Alice|5.00\n");
        }

        DynamicJobsEconomy plugin = mock(DynamicJobsEconomy.class);
        FileConfiguration cfg = mock(FileConfiguration.class);
        when(cfg.getString(eq("messages.prefix"), isNull())).thenReturn(null);
        when(plugin.getConfig()).thenReturn(cfg);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        when(plugin.getDataFolder()).thenReturn(dataDir);

        Messages messages = new Messages(plugin);
        messages.load();
        when(plugin.getMessages()).thenReturn(messages);

        AdminCommand admin = new AdminCommand(plugin);
        MsgCollector out = new MsgCollector();
        Player sender = out.asPlayerWithPerms();

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"history", "Bob"});
        assertTrue(handled);
        assertTrue(out.list.stream().anyMatch(m -> m.startsWith("§8[§6I18N§8] ") && m.contains("No history for Bob.")));
    }

    @Test
    void reload_recomputes_prefix_and_uses_i18n_message() throws Exception {
        File dataDir = newTempDir();
        try (FileWriter w = new FileWriter(new File(dataDir, "messages.yml"), StandardCharsets.UTF_8)) {
            w.write("messages:\n");
            w.write("  prefix: \"&8[&6I18N&8] \"\n");
            w.write("admin:\n");
            w.write("  reload_success: \"&aRELOAD_OK\"\n");
        }

        DynamicJobsEconomy plugin = mock(DynamicJobsEconomy.class);
        FileConfiguration cfg = mock(FileConfiguration.class);
        AtomicReference<String> pref = new AtomicReference<>("[P1] ");
        when(cfg.getString(eq("messages.prefix"), isNull())).thenAnswer(inv -> pref.get());
        when(plugin.getConfig()).thenReturn(cfg);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        when(plugin.getDataFolder()).thenReturn(dataDir);

        Messages messages = new Messages(plugin);
        messages.load();
        when(plugin.getMessages()).thenReturn(messages);

        AdminCommand admin = new AdminCommand(plugin);
        MsgCollector out = new MsgCollector();
        Player sender = out.asPlayerWithPerms();

        // Simulate that config prefix changed before reload; getPrefix() will read the new value after plugin.onReload()
        pref.set("[P2] ");
        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"reload"});

        assertTrue(handled);
        assertTrue(out.list.stream().anyMatch(m -> m.startsWith("[P2] ") && m.contains("RELOAD_OK")));
    }

    @Test
    void economy_usage_message_when_missing_args_uses_i18n() throws Exception {
        File dataDir = newTempDir();
        try (FileWriter w = new FileWriter(new File(dataDir, "messages.yml"), StandardCharsets.UTF_8)) {
            w.write("messages:\n");
            w.write("  prefix: \"&8[&6I18N&8] \"\n");
            w.write("admin:\n");
            w.write("  usage:\n");
            w.write("    economy: \"&cUSE_ECON\"\n");
        }

        DynamicJobsEconomy plugin = mock(DynamicJobsEconomy.class);
        FileConfiguration cfg = mock(FileConfiguration.class);
        when(cfg.getString(eq("messages.prefix"), isNull())).thenReturn(null);
        when(plugin.getConfig()).thenReturn(cfg);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        when(plugin.getDataFolder()).thenReturn(dataDir);

        Messages messages = new Messages(plugin);
        messages.load();
        when(plugin.getMessages()).thenReturn(messages);

        AdminCommand admin = new AdminCommand(plugin);
        MsgCollector out = new MsgCollector();
        Player sender = out.asPlayerWithPerms();

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"economy"});
        assertTrue(handled);
        assertTrue(out.list.stream().anyMatch(m -> m.startsWith("§8[§6I18N§8] ") && m.contains("USE_ECON")));
    }

    @Test
    void economy_invalid_action_uses_i18n_message() throws Exception {
        File dataDir = newTempDir();
        try (FileWriter w = new FileWriter(new File(dataDir, "messages.yml"), StandardCharsets.UTF_8)) {
            w.write("messages:\n");
            w.write("  prefix: \"&8[&6I18N&8] \"\n");
            w.write("admin:\n");
            w.write("  invalid_action: \"&cBAD_ACTION\"\n");
        }

        DynamicJobsEconomy plugin = mock(DynamicJobsEconomy.class);
        FileConfiguration cfg = mock(FileConfiguration.class);
        when(cfg.getString(eq("messages.prefix"), isNull())).thenReturn(null);
        when(plugin.getConfig()).thenReturn(cfg);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        when(plugin.getDataFolder()).thenReturn(dataDir);

        Messages messages = new Messages(plugin);
        messages.load();
        when(plugin.getMessages()).thenReturn(messages);

        // Provide an online player resolution
        class TestAdmin extends AdminCommand {
            final Player p;
            TestAdmin(DynamicJobsEconomy plg, Player p) { super(plg); this.p = p; }
            @Override protected Player getPlayerByName(String name) { return p; }
            @Override protected OfflinePlayer[] getOfflinePlayersArray() { return new OfflinePlayer[0]; }
        }
        Player target = mock(Player.class);
        when(target.getUniqueId()).thenReturn(UUID.randomUUID());
        when(target.getName()).thenReturn("Alice");
        TestAdmin admin = new TestAdmin(plugin, target);

        MsgCollector out = new MsgCollector();
        Player sender = out.asPlayerWithPerms();

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"economy", "foo", "Alice", "10"});
        assertTrue(handled);
        assertTrue(out.list.stream().anyMatch(m -> m.contains("BAD_ACTION")));
    }

    @Test
    void economy_invalid_amount_and_negative_and_too_large_use_i18n() throws Exception {
        File dataDir = newTempDir();
        try (FileWriter w = new FileWriter(new File(dataDir, "messages.yml"), StandardCharsets.UTF_8)) {
            w.write("messages:\n");
            w.write("  prefix: \"&8[&6I18N&8] \"\n");
            w.write("admin:\n");
            w.write("  invalid_amount: \"&cBAD_AMT\"\n");
            w.write("  negative_amount: \"&cNEG_AMT\"\n");
            w.write("  amount_too_large: \"&cTOO_LARGE\"\n");
        }

        DynamicJobsEconomy plugin = mock(DynamicJobsEconomy.class);
        FileConfiguration cfg = mock(FileConfiguration.class);
        when(cfg.getString(eq("messages.prefix"), isNull())).thenReturn(null);
        when(plugin.getConfig()).thenReturn(cfg);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        when(plugin.getDataFolder()).thenReturn(dataDir);

        Messages messages = new Messages(plugin);
        messages.load();
        when(plugin.getMessages()).thenReturn(messages);

        class TestAdmin extends AdminCommand {
            final Player p;
            TestAdmin(DynamicJobsEconomy plg, Player p) { super(plg); this.p = p; }
            @Override protected Player getPlayerByName(String name) { return p; }
            @Override protected OfflinePlayer[] getOfflinePlayersArray() { return new OfflinePlayer[0]; }
        }
        Player target = mock(Player.class);
        when(target.getUniqueId()).thenReturn(UUID.randomUUID());
        when(target.getName()).thenReturn("Bob");
        TestAdmin admin = new TestAdmin(plugin, target);

        MsgCollector out = new MsgCollector();
        Player sender = out.asPlayerWithPerms();

        // invalid amount (non-numeric)
        boolean h1 = admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"economy", "give", "Bob", "abc"});
        assertTrue(h1);
        assertTrue(out.list.stream().anyMatch(m -> m.contains("BAD_AMT")));

        // negative amount
        boolean h2 = admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"economy", "give", "Bob", "-5"});
        assertTrue(h2);
        assertTrue(out.list.stream().anyMatch(m -> m.contains("NEG_AMT")));

        // too large amount (> 1_000_000_000)
        boolean h3 = admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"economy", "give", "Bob", "1000000001"});
        assertTrue(h3);
        assertTrue(out.list.stream().anyMatch(m -> m.contains("TOO_LARGE")));
    }

    @Test
    void economy_give_online_success_uses_i18n_message() throws Exception {
        File dataDir = newTempDir();
        try (FileWriter w = new FileWriter(new File(dataDir, "messages.yml"), StandardCharsets.UTF_8)) {
            w.write("messages:\n");
            w.write("  prefix: \"&8[&6I18N&8] \"\n");
            w.write("admin:\n");
            w.write("  give_success: \"&aGAVE $%amount% to %player%\"\n");
        }

        DynamicJobsEconomy plugin = mock(DynamicJobsEconomy.class);
        FileConfiguration cfg = mock(FileConfiguration.class);
        when(cfg.getString(eq("messages.prefix"), isNull())).thenReturn(null);
        when(plugin.getConfig()).thenReturn(cfg);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        when(plugin.getDataFolder()).thenReturn(dataDir);

        Messages messages = new Messages(plugin);
        messages.load();
        when(plugin.getMessages()).thenReturn(messages);

        EconomyManager econ = mock(EconomyManager.class);
        when(plugin.getEconomyManager()).thenReturn(econ);
        when(econ.deposit(any(Player.class), eq(25.0))).thenReturn(true);

        class TestAdmin extends AdminCommand {
            final Player p;
            TestAdmin(DynamicJobsEconomy plg, Player p) { super(plg); this.p = p; }
            @Override protected Player getPlayerByName(String name) { return p; }
            @Override protected OfflinePlayer[] getOfflinePlayersArray() { return new OfflinePlayer[0]; }
        }
        Player target = mock(Player.class);
        when(target.getUniqueId()).thenReturn(UUID.randomUUID());
        when(target.getName()).thenReturn("Eve");
        TestAdmin admin = new TestAdmin(plugin, target);

        MsgCollector out = new MsgCollector();
        Player sender = out.asPlayerWithPerms();

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"economy", "give", "Eve", "25"});
        assertTrue(handled);
        assertTrue(out.list.stream().anyMatch(m -> m.startsWith("§8[§6I18N§8] ") && m.contains("GAVE $25.00 to Eve")));
    }

    @Test
    void economy_give_offline_sends_offline_note_and_success_and_calls_depositPlayer() throws Exception {
        File dataDir = newTempDir();
        try (FileWriter w = new FileWriter(new File(dataDir, "messages.yml"), StandardCharsets.UTF_8)) {
            w.write("messages:\n");
            w.write("  prefix: \"&8[&6I18N&8] \"\n");
            w.write("admin:\n");
            w.write("  offline_note: \"&7OFFLINE %player%\"\n");
            w.write("  give_success: \"&aGAVE $%amount% to %player%\"\n");
        }

        DynamicJobsEconomy plugin = mock(DynamicJobsEconomy.class);
        FileConfiguration cfg = mock(FileConfiguration.class);
        when(cfg.getString(eq("messages.prefix"), isNull())).thenReturn(null);
        when(plugin.getConfig()).thenReturn(cfg);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        when(plugin.getDataFolder()).thenReturn(dataDir);

        Messages messages = new Messages(plugin);
        messages.load();
        when(plugin.getMessages()).thenReturn(messages);

        EconomyManager econ = mock(EconomyManager.class);
        when(plugin.getEconomyManager()).thenReturn(econ);
        when(econ.depositPlayer(any(OfflinePlayer.class), eq(50.0))).thenReturn(true);

        class TestAdmin extends AdminCommand {
            final OfflinePlayer off;
            TestAdmin(DynamicJobsEconomy p, OfflinePlayer o) { super(p); this.off = o; }
            @Override protected Player getPlayerByName(String name) { return null; }
            @Override protected OfflinePlayer[] getOfflinePlayersArray() { return new OfflinePlayer[]{off}; }
        }
        OfflinePlayer off = mock(OfflinePlayer.class);
        when(off.getName()).thenReturn("Zoe");
        when(off.hasPlayedBefore()).thenReturn(true);
        TestAdmin admin = new TestAdmin(plugin, off);

        MsgCollector out = new MsgCollector();
        Player sender = out.asPlayerWithPerms();

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"economy", "give", "Zoe", "50"});
        assertTrue(handled);
        verify(econ, atLeastOnce()).depositPlayer(eq(off), eq(50.0));
        assertTrue(out.list.stream().anyMatch(m -> m.contains("OFFLINE Zoe")));
        assertTrue(out.list.stream().anyMatch(m -> m.contains("GAVE $50.00 to Zoe")));
    }

    @Test
    void economy_take_insufficient_and_success_messages_use_i18n() throws Exception {
        File dataDir = newTempDir();
        try (FileWriter w = new FileWriter(new File(dataDir, "messages.yml"), StandardCharsets.UTF_8)) {
            w.write("messages:\n");
            w.write("  prefix: \"&8[&6I18N&8] \"\n");
            w.write("admin:\n");
            w.write("  take_insufficient: \"&cONLY $%balance%\"\n");
            w.write("  take_success: \"&aTOOK $%amount% from %player%\"\n");
        }

        DynamicJobsEconomy plugin = mock(DynamicJobsEconomy.class);
        FileConfiguration cfg = mock(FileConfiguration.class);
        when(cfg.getString(eq("messages.prefix"), isNull())).thenReturn(null);
        when(plugin.getConfig()).thenReturn(cfg);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        when(plugin.getDataFolder()).thenReturn(dataDir);

        Messages messages = new Messages(plugin);
        messages.load();
        when(plugin.getMessages()).thenReturn(messages);

        EconomyManager econ = mock(EconomyManager.class);
        when(plugin.getEconomyManager()).thenReturn(econ);
        when(econ.getBalance(any(Player.class))).thenReturn(3.0, 10.0); // first insufficient, then enough
        when(econ.withdraw(any(Player.class), eq(10.0))).thenReturn(true);

        class TestAdmin extends AdminCommand {
            final Player p;
            TestAdmin(DynamicJobsEconomy plg, Player p) { super(plg); this.p = p; }
            @Override protected Player getPlayerByName(String name) { return p; }
            @Override protected OfflinePlayer[] getOfflinePlayersArray() { return new OfflinePlayer[0]; }
        }
        Player target = mock(Player.class);
        when(target.getUniqueId()).thenReturn(UUID.randomUUID());
        when(target.getName()).thenReturn("Max");
        TestAdmin admin = new TestAdmin(plugin, target);

        MsgCollector out = new MsgCollector();
        Player sender = out.asPlayerWithPerms();

        // insufficient
        boolean h1 = admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"economy", "take", "Max", "5"});
        assertTrue(h1);
        assertTrue(out.list.stream().anyMatch(m -> m.contains("ONLY $3.00")));

        // success
        boolean h2 = admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"economy", "take", "Max", "10"});
        assertTrue(h2);
        assertTrue(out.list.stream().anyMatch(m -> m.contains("TOOK $10.00 from Max")));
    }

    @Test
    void economy_set_success_and_failed_execute_use_i18n() throws Exception {
        File dataDir = newTempDir();
        try (FileWriter w = new FileWriter(new File(dataDir, "messages.yml"), StandardCharsets.UTF_8)) {
            w.write("messages:\n");
            w.write("  prefix: \"&8[&6I18N&8] \"\n");
            w.write("admin:\n");
            w.write("  set_success: \"&aSET %player% $%amount%\"\n");
            w.write("  failed_execute: \"&cFAIL_EXEC\"\n");
        }

        DynamicJobsEconomy plugin = mock(DynamicJobsEconomy.class);
        FileConfiguration cfg = mock(FileConfiguration.class);
        when(cfg.getString(eq("messages.prefix"), isNull())).thenReturn(null);
        when(plugin.getConfig()).thenReturn(cfg);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        when(plugin.getDataFolder()).thenReturn(dataDir);

        Messages messages = new Messages(plugin);
        messages.load();
        when(plugin.getMessages()).thenReturn(messages);

        EconomyManager econ = mock(EconomyManager.class);
        when(plugin.getEconomyManager()).thenReturn(econ);
        when(econ.getBalance(any(Player.class))).thenReturn(12.0, 12.0); // for both runs
        when(econ.withdraw(any(Player.class), eq(12.0))).thenReturn(true, true);
        when(econ.deposit(any(Player.class), eq(20.0))).thenReturn(true, false); // success then failure

        class TestAdmin extends AdminCommand {
            final Player p;
            TestAdmin(DynamicJobsEconomy plg, Player p) { super(plg); this.p = p; }
            @Override protected Player getPlayerByName(String name) { return p; }
            @Override protected OfflinePlayer[] getOfflinePlayersArray() { return new OfflinePlayer[0]; }
        }
        Player target = mock(Player.class);
        when(target.getUniqueId()).thenReturn(UUID.randomUUID());
        when(target.getName()).thenReturn("Neo");
        TestAdmin admin = new TestAdmin(plugin, target);

        MsgCollector out = new MsgCollector();
        Player sender = out.asPlayerWithPerms();

        // success
        boolean h1 = admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"economy", "set", "Neo", "20"});
        assertTrue(h1);
        assertTrue(out.list.stream().anyMatch(m -> m.contains("SET Neo $20.00")));

        // failed execute (deposit false)
        boolean h2 = admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"economy", "set", "Neo", "20"});
        assertTrue(h2);
        assertTrue(out.list.stream().anyMatch(m -> m.contains("FAIL_EXEC")));
    }

    @Test
    void confirm_players_only_and_no_pending_use_i18n() throws Exception {
        File dataDir = newTempDir();
        try (FileWriter w = new FileWriter(new File(dataDir, "messages.yml"), StandardCharsets.UTF_8)) {
            w.write("messages:\n");
            w.write("  prefix: \"&8[&6I18N&8] \"\n");
            w.write("admin:\n");
            w.write("  confirm_players_only: \"&cPLAYERS_ONLY\"\n");
            w.write("  no_pending_confirm: \"&cNO_PENDING\"\n");
        }

        DynamicJobsEconomy plugin = mock(DynamicJobsEconomy.class);
        FileConfiguration cfg = mock(FileConfiguration.class);
        when(cfg.getString(eq("messages.prefix"), isNull())).thenReturn(null);
        when(plugin.getConfig()).thenReturn(cfg);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        when(plugin.getDataFolder()).thenReturn(dataDir);

        Messages messages = new Messages(plugin);
        messages.load();
        when(plugin.getMessages()).thenReturn(messages);

        AdminCommand admin = new AdminCommand(plugin);

        // players-only: use console sender
        MsgCollector out1 = new MsgCollector();
        CommandSender console = out1.asConsole();
        boolean h1 = admin.onCommand(console, mock(Command.class), "djeconomy", new String[]{"confirm"});
        assertTrue(h1);
        assertTrue(out1.list.stream().anyMatch(m -> m.contains("PLAYERS_ONLY")));

        // no-pending: use player sender with no prior large action
        MsgCollector out2 = new MsgCollector();
        Player player = out2.asPlayerWithPerms();
        boolean h2 = admin.onCommand(player, mock(Command.class), "djeconomy", new String[]{"confirm"});
        assertTrue(h2);
        assertTrue(out2.list.stream().anyMatch(m -> m.contains("NO_PENDING")));
    }
    
    @Test
    void economy_give_failed_execute_uses_i18n() throws Exception {
        File dataDir = newTempDir();
        try (FileWriter w = new FileWriter(new File(dataDir, "messages.yml"), StandardCharsets.UTF_8)) {
            w.write("messages:\n");
            w.write("  prefix: \"&8[&6I18N&8] \"\n");
            w.write("admin:\n");
            w.write("  failed_execute: \"&cFAIL_EXEC_GIVE\"\n");
        }

        DynamicJobsEconomy plugin = mock(DynamicJobsEconomy.class);
        FileConfiguration cfg = mock(FileConfiguration.class);
        when(cfg.getString(eq("messages.prefix"), isNull())).thenReturn(null);
        when(plugin.getConfig()).thenReturn(cfg);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        when(plugin.getDataFolder()).thenReturn(dataDir);

        Messages messages = new Messages(plugin);
        messages.load();
        when(plugin.getMessages()).thenReturn(messages);

        EconomyManager econ = mock(EconomyManager.class);
        when(plugin.getEconomyManager()).thenReturn(econ);
        when(econ.deposit(any(Player.class), eq(15.0))).thenReturn(false);

        class TestAdmin extends AdminCommand {
            final Player p;
            TestAdmin(DynamicJobsEconomy plg, Player p) { super(plg); this.p = p; }
            @Override protected Player getPlayerByName(String name) { return p; }
            @Override protected OfflinePlayer[] getOfflinePlayersArray() { return new OfflinePlayer[0]; }
        }
        Player target = mock(Player.class);
        when(target.getUniqueId()).thenReturn(UUID.randomUUID());
        when(target.getName()).thenReturn("Gary");
        TestAdmin admin = new TestAdmin(plugin, target);

        MsgCollector out = new MsgCollector();
        Player sender = out.asPlayerWithPerms();

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"economy", "give", "Gary", "15"});
        assertTrue(handled);
        assertTrue(out.list.stream().anyMatch(m -> m.contains("FAIL_EXEC_GIVE")));
    }

    @Test
    void economy_take_failed_execute_uses_i18n() throws Exception {
        File dataDir = newTempDir();
        try (FileWriter w = new FileWriter(new File(dataDir, "messages.yml"), StandardCharsets.UTF_8)) {
            w.write("messages:\n");
            w.write("  prefix: \"&8[&6I18N&8] \"\n");
            w.write("admin:\n");
            w.write("  failed_execute: \"&cFAIL_EXEC_TAKE\"\n");
        }

        DynamicJobsEconomy plugin = mock(DynamicJobsEconomy.class);
        FileConfiguration cfg = mock(FileConfiguration.class);
        when(cfg.getString(eq("messages.prefix"), isNull())).thenReturn(null);
        when(plugin.getConfig()).thenReturn(cfg);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        when(plugin.getDataFolder()).thenReturn(dataDir);

        Messages messages = new Messages(plugin);
        messages.load();
        when(plugin.getMessages()).thenReturn(messages);

        EconomyManager econ = mock(EconomyManager.class);
        when(plugin.getEconomyManager()).thenReturn(econ);
        when(econ.getBalance(any(Player.class))).thenReturn(20.0);
        when(econ.withdraw(any(Player.class), eq(10.0))).thenReturn(false);

        class TestAdmin extends AdminCommand {
            final Player p;
            TestAdmin(DynamicJobsEconomy plg, Player p) { super(plg); this.p = p; }
            @Override protected Player getPlayerByName(String name) { return p; }
            @Override protected OfflinePlayer[] getOfflinePlayersArray() { return new OfflinePlayer[0]; }
        }
        Player target = mock(Player.class);
        when(target.getUniqueId()).thenReturn(UUID.randomUUID());
        when(target.getName()).thenReturn("Tina");
        TestAdmin admin = new TestAdmin(plugin, target);

        MsgCollector out = new MsgCollector();
        Player sender = out.asPlayerWithPerms();

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"economy", "take", "Tina", "10"});
        assertTrue(handled);
        assertTrue(out.list.stream().anyMatch(m -> m.contains("FAIL_EXEC_TAKE")));
    }

    @Test
    void economy_player_not_found_uses_i18n() throws Exception {
        File dataDir = newTempDir();
        try (FileWriter w = new FileWriter(new File(dataDir, "messages.yml"), StandardCharsets.UTF_8)) {
            w.write("messages:\n");
            w.write("  prefix: \"&8[&6I18N&8] \"\n");
            w.write("admin:\n");
            w.write("  player_not_found: \"&cNO_PLAYER %player%\"\n");
        }

        DynamicJobsEconomy plugin = mock(DynamicJobsEconomy.class);
        FileConfiguration cfg = mock(FileConfiguration.class);
        when(cfg.getString(eq("messages.prefix"), isNull())).thenReturn(null);
        when(plugin.getConfig()).thenReturn(cfg);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        when(plugin.getDataFolder()).thenReturn(dataDir);

        Messages messages = new Messages(plugin);
        messages.load();
        when(plugin.getMessages()).thenReturn(messages);

        class TestAdmin extends AdminCommand {
            TestAdmin(DynamicJobsEconomy plg) { super(plg); }
            @Override protected Player getPlayerByName(String name) { return null; }
            @Override protected OfflinePlayer[] getOfflinePlayersArray() { return new OfflinePlayer[0]; }
        }
        AdminCommand admin = new TestAdmin(plugin);

        MsgCollector out = new MsgCollector();
        Player sender = out.asPlayerWithPerms();

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"economy", "give", "Ghost", "5"});
        assertTrue(handled);
        assertTrue(out.list.stream().anyMatch(m -> m.contains("NO_PLAYER Ghost")));
    }

    @Test
    void economy_no_permission_sends_no_permission() throws Exception {
        File dataDir = newTempDir();
        try (FileWriter w = new FileWriter(new File(dataDir, "messages.yml"), StandardCharsets.UTF_8)) {
            w.write("messages:\n");
            w.write("  prefix: \"&8[&6I18N&8] \"\n");
            w.write("admin:\n");
            w.write("  no_permission: \"&cNO_PERM\"\n");
        }

        DynamicJobsEconomy plugin = mock(DynamicJobsEconomy.class);
        FileConfiguration cfg = mock(FileConfiguration.class);
        when(cfg.getString(eq("messages.prefix"), isNull())).thenReturn(null);
        when(plugin.getConfig()).thenReturn(cfg);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        when(plugin.getDataFolder()).thenReturn(dataDir);

        Messages messages = new Messages(plugin);
        messages.load();
        when(plugin.getMessages()).thenReturn(messages);

        AdminCommand admin = new AdminCommand(plugin);

        // Sender without any permissions
        MsgCollector out = new MsgCollector();
        Player sender = mock(Player.class);
        when(sender.getUniqueId()).thenReturn(UUID.randomUUID());
        when(sender.getName()).thenReturn("NoPerm");
        when(sender.hasPermission(anyString())).thenReturn(false);
        doAnswer(inv -> { out.list.add(inv.getArgument(0)); return null; }).when(sender).sendMessage(anyString());

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"economy", "give", "Someone", "1"});
        assertTrue(handled);
        assertTrue(out.list.stream().anyMatch(m -> m.contains("NO_PERM")));
    }

    @Test
    void confirm_no_permission_sends_no_permission() throws Exception {
        File dataDir = newTempDir();
        try (FileWriter w = new FileWriter(new File(dataDir, "messages.yml"), StandardCharsets.UTF_8)) {
            w.write("messages:\n");
            w.write("  prefix: \"&8[&6I18N&8] \"\n");
            w.write("admin:\n");
            w.write("  no_permission: \"&cNO_PERM\"\n");
        }

        DynamicJobsEconomy plugin = mock(DynamicJobsEconomy.class);
        FileConfiguration cfg = mock(FileConfiguration.class);
        when(cfg.getString(eq("messages.prefix"), isNull())).thenReturn(null);
        when(plugin.getConfig()).thenReturn(cfg);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        when(plugin.getDataFolder()).thenReturn(dataDir);

        Messages messages = new Messages(plugin);
        messages.load();
        when(plugin.getMessages()).thenReturn(messages);

        AdminCommand admin = new AdminCommand(plugin);

        // Player sender with no perms
        MsgCollector out = new MsgCollector();
        Player sender = mock(Player.class);
        when(sender.getUniqueId()).thenReturn(UUID.randomUUID());
        when(sender.getName()).thenReturn("NoPermPlayer");
        when(sender.hasPermission(anyString())).thenReturn(false);
        doAnswer(inv -> { out.list.add(inv.getArgument(0)); return null; }).when(sender).sendMessage(anyString());

        boolean handled = admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"confirm"});
        assertTrue(handled);
        assertTrue(out.list.stream().anyMatch(m -> m.contains("NO_PERM")));
    }
}
