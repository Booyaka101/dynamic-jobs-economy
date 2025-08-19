package com.boopugstudios.dynamicjobseconomy.commands;

import com.boopugstudios.dynamicjobseconomy.DynamicJobsEconomy;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdminCommandHistoryTest {

    private static class TestableAdminCommand extends AdminCommand {
        TestableAdminCommand(DynamicJobsEconomy plugin) { super(plugin); }
    }

    private static class MsgCollector {
        final List<String> list = new ArrayList<>();
        CommandSender asPlayerWithPerms() {
            Player p = mock(Player.class);
            when(p.getUniqueId()).thenReturn(UUID.randomUUID());
            when(p.getName()).thenReturn("Admin");
            when(p.hasPermission(anyString())).thenReturn(true);
            doAnswer(inv -> { list.add(inv.getArgument(0)); return null; }).when(p).sendMessage(anyString());
            return p;
        }
    }

    private DynamicJobsEconomy setupPluginWithTempDataFolder(Path dataFolder) {
        DynamicJobsEconomy plugin = mock(DynamicJobsEconomy.class);
        FileConfiguration cfg = mock(FileConfiguration.class);
        when(plugin.getConfig()).thenReturn(cfg);
        // Return provided default as prefix
        when(cfg.getString(eq("messages.prefix"), anyString())).thenAnswer(inv -> inv.getArgument(1));
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        when(plugin.getDataFolder()).thenReturn(dataFolder.toFile());
        return plugin;
    }

    private Path historyPathOf(DynamicJobsEconomy plugin) {
        File dir = plugin.getDataFolder();
        return new File(dir, "admin-economy-history.log").toPath();
    }

    @Test
    void history_noFile_showsNoHistoryMessage() throws Exception {
        Path tmp = Files.createTempDirectory("dje-test");
        try {
            DynamicJobsEconomy plugin = setupPluginWithTempDataFolder(tmp);
            AdminCommand admin = new TestableAdminCommand(plugin);

            // Ensure no file exists
            Files.deleteIfExists(historyPathOf(plugin));

            MsgCollector out = new MsgCollector();
            boolean handled = admin.onCommand(out.asPlayerWithPerms(), mock(Command.class), "djeconomy",
                    new String[]{"history", "Alice"});
            assertTrue(handled);
            assertTrue(out.list.stream().anyMatch(m -> m.contains("No history found for 'Alice'")));
        } finally {
            // Cleanup
            try { Files.walk(tmp).sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete()); } catch (IOException ignored) {}
        }
    }

    @Test
    void history_fileExists_butNoEntriesForPlayer_showsNoHistory() throws Exception {
        Path tmp = Files.createTempDirectory("dje-test");
        try {
            DynamicJobsEconomy plugin = setupPluginWithTempDataFolder(tmp);
            Path log = historyPathOf(plugin);
            // Write entries for Bob, not Alice
            try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(log, StandardCharsets.UTF_8))) {
                out.printf("%d|%s|%s|%s|%.2f%n", System.currentTimeMillis(), "Admin", "gave", "Bob", 10.0);
            }
            AdminCommand admin = new TestableAdminCommand(plugin);

            MsgCollector outMsg = new MsgCollector();
            boolean handled = admin.onCommand(outMsg.asPlayerWithPerms(), mock(Command.class), "djeconomy",
                    new String[]{"history", "Alice"});
            assertTrue(handled);
            assertTrue(outMsg.list.stream().anyMatch(m -> m.contains("No history found for 'Alice'")));
        } finally {
            try { Files.walk(tmp).sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete()); } catch (IOException ignored) {}
        }
    }

    @Test
    void history_ioError_whenLogIsDirectory_showsError() throws Exception {
        Path tmp = Files.createTempDirectory("dje-test");
        try {
            DynamicJobsEconomy plugin = setupPluginWithTempDataFolder(tmp);
            // Create a directory with the same name to force IOException on read
            Files.createDirectory(historyPathOf(plugin));

            AdminCommand admin = new TestableAdminCommand(plugin);
            MsgCollector out = new MsgCollector();
            boolean handled = admin.onCommand(out.asPlayerWithPerms(), mock(Command.class), "djeconomy",
                    new String[]{"history", "Alice"});
            assertTrue(handled);
            assertTrue(out.list.stream().anyMatch(m -> m.contains("Failed to read history:")));
        } finally {
            try { Files.walk(tmp).sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete()); } catch (IOException ignored) {}
        }
    }

    @Test
    void history_limitParsing_default10_onNonNumeric_and_clampingBounds() throws Exception {
        Path tmp = Files.createTempDirectory("dje-test");
        try {
            DynamicJobsEconomy plugin = setupPluginWithTempDataFolder(tmp);
            Path log = historyPathOf(plugin);

            // Create 120 entries for Alice
            try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(log, StandardCharsets.UTF_8))) {
                for (int i = 0; i < 120; i++) {
                    out.printf("%d|%s|%s|%s|%.2f%n", System.currentTimeMillis(), "Admin", "gave", "Alice", (double) i);
                }
            }

            AdminCommand admin = new TestableAdminCommand(plugin);

            // Non-numeric => default 10
            MsgCollector nonNum = new MsgCollector();
            assertTrue(admin.onCommand(nonNum.asPlayerWithPerms(), mock(Command.class), "djeconomy",
                    new String[]{"history", "Alice", "xx"}));
            Optional<String> header10 = nonNum.list.stream().filter(m -> m.contains("Showing last ")).findFirst();
            assertTrue(header10.isPresent() && header10.get().contains("last 10 "));
            long entryLines10 = nonNum.list.stream().filter(m -> m.startsWith("§7[")).count();
            assertEquals(10, entryLines10);

            // Clamp low (0 -> 1)
            MsgCollector low = new MsgCollector();
            assertTrue(admin.onCommand(low.asPlayerWithPerms(), mock(Command.class), "djeconomy",
                    new String[]{"history", "Alice", "0"}));
            Optional<String> header1 = low.list.stream().filter(m -> m.contains("Showing last ")).findFirst();
            assertTrue(header1.isPresent() && header1.get().contains("last 1 "));
            long entryLines1 = low.list.stream().filter(m -> m.startsWith("§7[")).count();
            assertEquals(1, entryLines1);

            // Clamp high (150 -> 100)
            MsgCollector high = new MsgCollector();
            assertTrue(admin.onCommand(high.asPlayerWithPerms(), mock(Command.class), "djeconomy",
                    new String[]{"history", "Alice", "150"}));
            Optional<String> header100 = high.list.stream().filter(m -> m.contains("Showing last ")).findFirst();
            assertTrue(header100.isPresent() && header100.get().contains("last 100 "));
            long entryLines100 = high.list.stream().filter(m -> m.startsWith("§7[")).count();
            assertEquals(100, entryLines100);
        } finally {
            try { Files.walk(tmp).sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete()); } catch (IOException ignored) {}
        }
    }
}
