package com.boopugstudios.dynamicjobseconomy.doctor;

import com.boopugstudios.dynamicjobseconomy.DynamicJobsEconomy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DatabaseHealthCheck {
    private final DynamicJobsEconomy plugin;

    public DatabaseHealthCheck(DynamicJobsEconomy plugin) {
        this.plugin = plugin;
    }

    public Result run() {
        String type = "unknown";
        long start = System.currentTimeMillis();
        boolean ok = false;
        String error = null;

        try {
            if (plugin.getDatabaseManager() == null) {
                error = "DatabaseManager unavailable";
            } else {
                type = plugin.getDatabaseManager().getDatabaseType();
                try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                    if (conn != null) {
                        try (PreparedStatement ps = conn.prepareStatement("SELECT 1")) {
                            ps.execute();
                            ok = true;
                        }
                    } else {
                        error = "Connection is null";
                    }
                }
            }
        } catch (SQLException e) {
            ok = false;
            error = e.getMessage();
        } catch (Throwable t) {
            ok = false;
            error = t.getMessage();
        }

        long latency = Math.max(0, System.currentTimeMillis() - start);
        return new Result(ok, latency, type, error);
    }

    public static class Result {
        private final boolean ok;
        private final long latencyMs;
        private final String dbType;
        private final String errorMessage;

        public Result(boolean ok, long latencyMs, String dbType, String errorMessage) {
            this.ok = ok;
            this.latencyMs = latencyMs;
            this.dbType = dbType;
            this.errorMessage = errorMessage;
        }

        public boolean isOk() { return ok; }
        public long getLatencyMs() { return latencyMs; }
        public String getDbType() { return dbType; }
        public String getErrorMessage() { return errorMessage; }
    }
}
