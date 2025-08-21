package com.boopugstudios.dynamicjobseconomy.doctor;

import com.boopugstudios.dynamicjobseconomy.DynamicJobsEconomy;
import com.boopugstudios.dynamicjobseconomy.database.DatabaseManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseHealthCheckIntegrationTest {

    private DatabaseManager db;

    @AfterEach
    void tearDown() {
        if (db != null) {
            db.closeConnections();
        }
    }

    private DynamicJobsEconomy mockPlugin(Path dataFolder, FileConfiguration config) {
        DynamicJobsEconomy plugin = Mockito.mock(DynamicJobsEconomy.class);
        Mockito.when(plugin.getConfig()).thenReturn(config);
        Mockito.when(plugin.getDataFolder()).thenReturn(dataFolder.toFile());
        Mockito.when(plugin.getLogger()).thenReturn(Logger.getLogger("DJE-Test"));
        return plugin;
    }

    private FileConfiguration sqliteConfig() {
        FileConfiguration cfg = Mockito.mock(FileConfiguration.class);
        Mockito.when(cfg.getString(Mockito.eq("database.type"), ArgumentMatchers.anyString()))
                .thenReturn("sqlite");
        return cfg;
    }

    private FileConfiguration mysqlConfig(String host, int port, String dbName, String user, String pass) {
        FileConfiguration cfg = Mockito.mock(FileConfiguration.class);
        Mockito.when(cfg.getString(Mockito.eq("database.type"), ArgumentMatchers.anyString()))
                .thenReturn("mysql");
        Mockito.when(cfg.getString(Mockito.eq("database.mysql.host"), ArgumentMatchers.anyString()))
                .thenReturn(host);
        Mockito.when(cfg.getInt(Mockito.eq("database.mysql.port"), ArgumentMatchers.anyInt()))
                .thenReturn(port);
        Mockito.when(cfg.getString(Mockito.eq("database.mysql.database"), ArgumentMatchers.anyString()))
                .thenReturn(dbName);
        Mockito.when(cfg.getString(Mockito.eq("database.mysql.username"), ArgumentMatchers.anyString()))
                .thenReturn(user);
        Mockito.when(cfg.getString(Mockito.eq("database.mysql.password"), ArgumentMatchers.anyString()))
                .thenReturn(pass);
        Mockito.when(cfg.getBoolean(Mockito.eq("database.mysql.useSSL"), ArgumentMatchers.anyBoolean()))
                .thenReturn(false);
        return cfg;
    }

    @Nested
    class SQLiteIntegration {
        @TempDir
        Path tempDir;

        @Test
        @Tag("integration")
        void sqlite_healthcheck_ok() {
            FileConfiguration cfg = sqliteConfig();
            DynamicJobsEconomy plugin = mockPlugin(tempDir, cfg);

            db = new DatabaseManager(plugin);
            assertTrue(db.initialize(), "SQLite initialize() should return true");
            assertEquals("sqlite", db.getDatabaseType());

            Mockito.when(plugin.getDatabaseManager()).thenReturn(db);

            DatabaseHealthCheck hc = new DatabaseHealthCheck(plugin);
            DatabaseHealthCheck.Result res = hc.run();

            assertTrue(res.isOk(), "Health check should pass on SQLite");
            assertEquals("sqlite", res.getDbType());
            assertNull(res.getErrorMessage());
            assertTrue(res.getLatencyMs() >= 0);
        }
    }

    @Nested
    @Testcontainers
    @Tag("docker")
    @EnabledIfEnvironmentVariable(named = "DJE_DOCKER", matches = "true")
    class MySQLIntegration {
        @Container
        @SuppressWarnings("resource")
        private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
                .withDatabaseName("dynamicjobs")
                .withUsername("test")
                .withPassword("test");

        @TempDir
        Path tempDir;

        @Test
        @Tag("integration")
        void mysql_healthcheck_ok() {
            FileConfiguration cfg = mysqlConfig(
                    MYSQL.getHost(),
                    MYSQL.getFirstMappedPort(),
                    MYSQL.getDatabaseName(),
                    MYSQL.getUsername(),
                    MYSQL.getPassword()
            );
            DynamicJobsEconomy plugin = mockPlugin(tempDir, cfg);

            db = new DatabaseManager(plugin);
            assertTrue(db.initialize(), "MySQL initialize() should return true");
            assertEquals("mysql", db.getDatabaseType());

            Mockito.when(plugin.getDatabaseManager()).thenReturn(db);

            DatabaseHealthCheck hc = new DatabaseHealthCheck(plugin);
            DatabaseHealthCheck.Result res = hc.run();

            assertTrue(res.isOk(), "Health check should pass on MySQL");
            assertEquals("mysql", res.getDbType());
            assertNull(res.getErrorMessage());
            assertTrue(res.getLatencyMs() >= 0);
        }
    }
}
