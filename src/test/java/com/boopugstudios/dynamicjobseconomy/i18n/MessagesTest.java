package com.boopugstudios.dynamicjobseconomy.i18n;

import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MessagesTest {

    private File newTempDir() {
        File dir = new File(System.getProperty("java.io.tmpdir"), "dje-msg-tests-" + System.nanoTime());
        assertTrue(dir.mkdirs() || dir.exists());
        return dir;
    }

    @Test
    void loads_prefix_and_translates_colors_and_placeholders() throws Exception {
        // Arrange: create a temp messages.yml with & color codes and a placeholder
        File dataDir = newTempDir();
        File msgFile = new File(dataDir, "messages.yml");
        String yaml = "messages:\n" +
                "  prefix: \"&8[&6Test&8] \"\n" +
                "admin:\n" +
                "  hello: \"&aHello %name%\"\n";
        try (FileWriter w = new FileWriter(msgFile, StandardCharsets.UTF_8)) {
            w.write(yaml);
        }

        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(dataDir);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));

        Messages messages = new Messages(plugin);
        messages.load();

        // Act + Assert: prefix is read and colorized
        assertEquals("§8[§6Test§8] ", messages.getPrefix());

        // Placeholder replacement and color code translation
        Map<String, String> ph = new HashMap<>();
        ph.put("name", "Alex");
        assertEquals("§aHello Alex", messages.get("admin.hello", ph, "&cDEF"));
    }

    @Test
    void returns_default_when_key_missing_and_colorizes_default() throws Exception {
        File dataDir = newTempDir();
        // Create empty messages.yml to bypass saveResource call
        Files.writeString(new File(dataDir, "messages.yml").toPath(), "messages:\n  prefix: \"&7[P]\"\n", StandardCharsets.UTF_8);

        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(dataDir);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));

        Messages messages = new Messages(plugin);
        messages.load();

        // Missing key -> default is used and colorized
        assertEquals("§cOops", messages.get("admin.missing", null, "&cOops"));
    }
}
