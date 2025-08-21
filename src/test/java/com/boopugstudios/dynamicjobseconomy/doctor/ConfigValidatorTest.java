package com.boopugstudios.dynamicjobseconomy.doctor;

import org.bukkit.configuration.file.FileConfiguration;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

class ConfigValidatorTest {

    @Test
    void sqlite_config_is_valid_when_required_keys_present() {
        FileConfiguration cfg = Mockito.mock(FileConfiguration.class);
        Mockito.when(cfg.isSet("database.type")).thenReturn(true);
        Mockito.when(cfg.getString("database.type", "")).thenReturn("sqlite");

        Mockito.when(cfg.isSet("economy.admin_confirmation.threshold")).thenReturn(true);
        Mockito.when(cfg.getDouble("economy.admin_confirmation.threshold")).thenReturn(10.0);
        Mockito.when(cfg.isSet("economy.admin_confirmation.expiry_seconds")).thenReturn(true);
        Mockito.when(cfg.getInt("economy.admin_confirmation.expiry_seconds")).thenReturn(30);

        ConfigValidator.Result res = new ConfigValidator(cfg).validate();
        assertTrue(res.isOk());
        assertTrue(res.getMissingKeys().isEmpty());
        assertTrue(res.getInvalidKeys().isEmpty());
    }

    @Test
    void mysql_config_reports_missing_keys() {
        FileConfiguration cfg = Mockito.mock(FileConfiguration.class);
        Mockito.when(cfg.isSet("database.type")).thenReturn(true);
        Mockito.when(cfg.getString("database.type", "")).thenReturn("mysql");

        // Intentionally missing mysql keys
        Mockito.when(cfg.isSet("database.mysql.host")).thenReturn(false);
        Mockito.when(cfg.isSet("database.mysql.database")).thenReturn(false);
        Mockito.when(cfg.isSet("database.mysql.username")).thenReturn(false);
        Mockito.when(cfg.isSet("database.mysql.port")).thenReturn(false);

        Mockito.when(cfg.isSet("economy.admin_confirmation.threshold")).thenReturn(true);
        Mockito.when(cfg.getDouble("economy.admin_confirmation.threshold")).thenReturn(10.0);
        Mockito.when(cfg.isSet("economy.admin_confirmation.expiry_seconds")).thenReturn(true);
        Mockito.when(cfg.getInt("economy.admin_confirmation.expiry_seconds")).thenReturn(30);

        ConfigValidator.Result res = new ConfigValidator(cfg).validate();
        assertFalse(res.isOk());
        assertTrue(res.getMissingKeys().contains("database.mysql.host"));
        assertTrue(res.getMissingKeys().contains("database.mysql.database"));
        assertTrue(res.getMissingKeys().contains("database.mysql.username"));
        assertTrue(res.getMissingKeys().contains("database.mysql.port"));
        assertTrue(res.getInvalidKeys().isEmpty());
    }

    @Test
    void mysql_config_reports_invalid_port() {
        FileConfiguration cfg = Mockito.mock(FileConfiguration.class);
        Mockito.when(cfg.isSet("database.type")).thenReturn(true);
        Mockito.when(cfg.getString("database.type", "")).thenReturn("mysql");

        Mockito.when(cfg.isSet("database.mysql.host")).thenReturn(true);
        Mockito.when(cfg.isSet("database.mysql.database")).thenReturn(true);
        Mockito.when(cfg.isSet("database.mysql.username")).thenReturn(true);
        Mockito.when(cfg.isSet("database.mysql.port")).thenReturn(true);
        Mockito.when(cfg.getInt("database.mysql.port")).thenReturn(0); // invalid

        Mockito.when(cfg.isSet("economy.admin_confirmation.threshold")).thenReturn(true);
        Mockito.when(cfg.getDouble("economy.admin_confirmation.threshold")).thenReturn(10.0);
        Mockito.when(cfg.isSet("economy.admin_confirmation.expiry_seconds")).thenReturn(true);
        Mockito.when(cfg.getInt("economy.admin_confirmation.expiry_seconds")).thenReturn(30);

        ConfigValidator.Result res = new ConfigValidator(cfg).validate();
        assertFalse(res.isOk());
        assertTrue(res.getInvalidKeys().contains("database.mysql.port"));
    }

    @Test
    void admin_confirmation_values_must_be_positive() {
        FileConfiguration cfg = Mockito.mock(FileConfiguration.class);
        Mockito.when(cfg.isSet("database.type")).thenReturn(true);
        Mockito.when(cfg.getString("database.type", "")).thenReturn("sqlite");

        Mockito.when(cfg.isSet("economy.admin_confirmation.threshold")).thenReturn(true);
        Mockito.when(cfg.getDouble("economy.admin_confirmation.threshold")).thenReturn(0.0); // invalid
        Mockito.when(cfg.isSet("economy.admin_confirmation.expiry_seconds")).thenReturn(true);
        Mockito.when(cfg.getInt("economy.admin_confirmation.expiry_seconds")).thenReturn(0); // invalid

        ConfigValidator.Result res = new ConfigValidator(cfg).validate();
        assertFalse(res.isOk());
        assertTrue(res.getInvalidKeys().contains("economy.admin_confirmation.threshold"));
        assertTrue(res.getInvalidKeys().contains("economy.admin_confirmation.expiry_seconds"));
    }
}
