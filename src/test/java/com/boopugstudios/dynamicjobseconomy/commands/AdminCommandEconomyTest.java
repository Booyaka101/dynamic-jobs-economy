package com.boopugstudios.dynamicjobseconomy.commands;

import com.boopugstudios.dynamicjobseconomy.DynamicJobsEconomy;
import com.boopugstudios.dynamicjobseconomy.economy.EconomyManager;
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
}
