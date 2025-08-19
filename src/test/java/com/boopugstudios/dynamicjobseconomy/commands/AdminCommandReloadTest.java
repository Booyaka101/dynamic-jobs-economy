package com.boopugstudios.dynamicjobseconomy.commands;

import com.boopugstudios.dynamicjobseconomy.DynamicJobsEconomy;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdminCommandReloadTest {

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

    @Test
    void reload_updates_config_so_next_command_uses_new_prefix() {
        // Arrange two configs with different prefixes
        FileConfiguration cfgOld = mock(FileConfiguration.class);
        when(cfgOld.getString(eq("messages.prefix"), anyString())).thenReturn("[OLD] ");
        FileConfiguration cfgNew = mock(FileConfiguration.class);
        when(cfgNew.getString(eq("messages.prefix"), anyString())).thenReturn("[NEW] ");

        DynamicJobsEconomy plugin = mock(DynamicJobsEconomy.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        AtomicBoolean reloaded = new AtomicBoolean(false);
        when(plugin.getConfig()).thenAnswer(inv -> reloaded.get() ? cfgNew : cfgOld);
        doAnswer(inv -> { reloaded.set(true); return null; }).when(plugin).reloadConfig();

        AdminCommand admin = new AdminCommand(plugin);
        MsgCollector out = new MsgCollector();
        CommandSender sender = out.asPlayerWithPerms();

        // Act 1: first reload -> should use OLD prefix for its message
        assertTrue(admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"reload"}));
        assertTrue(out.list.stream().anyMatch(m -> m.startsWith("[OLD] ") && m.contains("Configuration reloaded")));

        out.list.clear();

        // Act 2: subsequent command obtains NEW prefix
        assertTrue(admin.onCommand(sender, mock(Command.class), "djeconomy", new String[]{"reload"}));
        assertTrue(out.list.stream().anyMatch(m -> m.startsWith("[NEW] ") && m.contains("Configuration reloaded")));
    }
}
