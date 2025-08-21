package com.boopugstudios.dynamicjobseconomy.doctor;

import com.boopugstudios.dynamicjobseconomy.DynamicJobsEconomy;
import com.boopugstudios.dynamicjobseconomy.economy.EconomyManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VaultHealthCheckTest {

    @Mock
    DynamicJobsEconomy plugin;

    @Mock
    FileConfiguration config;

    @Mock
    EconomyManager economyManager;

    @Test
    void run_reportsEnabledWhenVaultActive() {
        when(plugin.getConfig()).thenReturn(config);
        when(config.getBoolean("integrations.vault.use_vault_economy", true)).thenReturn(true);
        when(plugin.getEconomyManager()).thenReturn(economyManager);
        when(economyManager.isVaultEnabled()).thenReturn(true);
        when(economyManager.getVaultProviderName()).thenReturn("TestProvider");

        VaultHealthCheck.Result result = new VaultHealthCheck(plugin).run();

        assertTrue(result.isPreferVault());
        assertTrue(result.isEnabled());
        assertEquals("TestProvider", result.getProviderName());
    }

    @Test
    void run_reportsDisabledWhenVaultPreferredButNotPresent() {
        when(plugin.getConfig()).thenReturn(config);
        when(config.getBoolean("integrations.vault.use_vault_economy", true)).thenReturn(true);
        when(plugin.getEconomyManager()).thenReturn(economyManager);
        when(economyManager.isVaultEnabled()).thenReturn(false);
        when(economyManager.getVaultProviderName()).thenReturn(null);

        VaultHealthCheck.Result result = new VaultHealthCheck(plugin).run();

        assertTrue(result.isPreferVault());
        assertFalse(result.isEnabled());
        assertNull(result.getProviderName());
    }

    @Test
    void run_handlesNoEconomyManager() {
        when(plugin.getConfig()).thenReturn(config);
        when(config.getBoolean("integrations.vault.use_vault_economy", true)).thenReturn(false);
        when(plugin.getEconomyManager()).thenReturn(null);

        VaultHealthCheck.Result result = new VaultHealthCheck(plugin).run();

        assertFalse(result.isPreferVault());
        assertFalse(result.isEnabled());
        assertNull(result.getProviderName());
    }
}
