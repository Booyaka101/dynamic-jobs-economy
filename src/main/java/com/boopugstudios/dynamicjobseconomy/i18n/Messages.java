package com.boopugstudios.dynamicjobseconomy.i18n;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Map;

public class Messages {
    private final JavaPlugin plugin;
    private File file;
    private FileConfiguration config;

    public Messages(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            this.file = new File(plugin.getDataFolder(), "messages.yml");
            if (!file.exists()) {
                // Save default from resources
                plugin.saveResource("messages.yml", false);
            }
            this.config = YamlConfiguration.loadConfiguration(file);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load messages.yml: " + e.getMessage());
            this.config = new YamlConfiguration();
        }
    }

    public String getPrefix() {
        // Prefer config.yml override for backward compatibility; fallback to messages.yml and default
        try {
            FileConfiguration cfg = plugin.getConfig();
            if (cfg != null) {
                String fromConfig = cfg.getString("messages.prefix", null);
                if (fromConfig != null && !fromConfig.isEmpty()) {
                    return colorize(fromConfig);
                }
            }
        } catch (Throwable ignored) {
            // If plugin.getConfig() is unavailable in tests/mocks, fall back below
        }
        String raw = (config != null)
            ? config.getString("messages.prefix", "§8[§6DynamicJobs§8] ")
            : "§8[§6DynamicJobs§8] ";
        return colorize(raw);
    }

    public String get(String path, Map<String, String> placeholders, String def) {
        String raw = config.getString(path, def);
        raw = colorize(raw);
        if (placeholders != null) {
            for (Map.Entry<String, String> e : placeholders.entrySet()) {
                raw = raw.replace("%" + e.getKey() + "%", e.getValue());
            }
        }
        return raw;
    }

    private String colorize(String s) {
        if (s == null) return null;
        // Translate common '&' codes to '§'
        return s.replace('&', '§');
    }
}
