package com.boopugstudios.dynamicjobseconomy.doctor;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Validates key configuration settings and returns structured results for doctor diagnostics.
 */
public class ConfigValidator {
    public static class Result {
        private final boolean ok;
        private final List<String> missingKeys;
        private final List<String> invalidKeys;

        public Result(boolean ok, List<String> missingKeys, List<String> invalidKeys) {
            this.ok = ok;
            this.missingKeys = missingKeys == null ? Collections.emptyList() : missingKeys;
            this.invalidKeys = invalidKeys == null ? Collections.emptyList() : invalidKeys;
        }

        public boolean isOk() { return ok; }
        public List<String> getMissingKeys() { return missingKeys; }
        public List<String> getInvalidKeys() { return invalidKeys; }
    }

    private final FileConfiguration cfg;

    public ConfigValidator(FileConfiguration cfg) {
        this.cfg = cfg;
    }

    public Result validate() {
        List<String> missing = new ArrayList<>();
        List<String> invalid = new ArrayList<>();

        // database.type
        String dbType = null;
        if (!isSet("database.type")) {
            missing.add("database.type");
        } else {
            dbType = cfg.getString("database.type", "");
            if (!("sqlite".equalsIgnoreCase(dbType) || "mysql".equalsIgnoreCase(dbType))) {
                invalid.add("database.type");
            }
        }

        // mysql specific
        if ("mysql".equalsIgnoreCase(dbType)) {
            if (!isSet("database.mysql.host")) missing.add("database.mysql.host");
            if (!isSet("database.mysql.database")) missing.add("database.mysql.database");
            if (!isSet("database.mysql.username")) missing.add("database.mysql.username");
            if (!isSet("database.mysql.port")) {
                missing.add("database.mysql.port");
            } else {
                int port = 0;
                try { port = cfg.getInt("database.mysql.port"); } catch (Throwable ignored) {}
                if (port <= 0) invalid.add("database.mysql.port");
            }
        }

        // Admin confirmation settings
        if (!isSet("economy.admin_confirmation.threshold")) {
            missing.add("economy.admin_confirmation.threshold");
        } else {
            double val = -1;
            try { val = cfg.getDouble("economy.admin_confirmation.threshold"); } catch (Throwable ignored) {}
            if (val <= 0) invalid.add("economy.admin_confirmation.threshold");
        }

        if (!isSet("economy.admin_confirmation.expiry_seconds")) {
            missing.add("economy.admin_confirmation.expiry_seconds");
        } else {
            int seconds = -1;
            try { seconds = cfg.getInt("economy.admin_confirmation.expiry_seconds"); } catch (Throwable ignored) {}
            if (seconds <= 0) invalid.add("economy.admin_confirmation.expiry_seconds");
        }

        boolean ok = missing.isEmpty() && invalid.isEmpty();
        return new Result(ok, missing, invalid);
    }

    private boolean isSet(String path) {
        try { return cfg != null && cfg.isSet(path); } catch (Throwable t) { return false; }
    }
}
