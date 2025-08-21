package com.boopugstudios.dynamicjobseconomy.doctor;

import com.boopugstudios.dynamicjobseconomy.DynamicJobsEconomy;

public class VaultHealthCheck {
    private final DynamicJobsEconomy plugin;

    public VaultHealthCheck(DynamicJobsEconomy plugin) {
        this.plugin = plugin;
    }

    public Result run() {
        boolean preferVault = true;
        boolean enabled = false;
        String provider = null;
        try {
            preferVault = plugin.getConfig().getBoolean("integrations.vault.use_vault_economy", true);
        } catch (Throwable ignored) {}
        try {
            if (plugin.getEconomyManager() != null) {
                enabled = plugin.getEconomyManager().isVaultEnabled();
                provider = plugin.getEconomyManager().getVaultProviderName();
            }
        } catch (Throwable ignored) {}
        return new Result(preferVault, enabled, provider);
    }

    public static class Result {
        private final boolean preferVault;
        private final boolean enabled;
        private final String providerName;

        public Result(boolean preferVault, boolean enabled, String providerName) {
            this.preferVault = preferVault;
            this.enabled = enabled;
            this.providerName = providerName;
        }

        public boolean isPreferVault() { return preferVault; }
        public boolean isEnabled() { return enabled; }
        public String getProviderName() { return providerName; }
    }
}
