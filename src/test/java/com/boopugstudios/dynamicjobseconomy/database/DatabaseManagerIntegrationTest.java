package com.boopugstudios.dynamicjobseconomy.database;

import com.boopugstudios.dynamicjobseconomy.DynamicJobsEconomy;
import org.bukkit.configuration.file.FileConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
 
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseManagerIntegrationTest {

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

    private void assertCoreTablesExist(Connection conn, boolean mysql, String dbNameIfMySQL) throws Exception {
        if (mysql) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT table_name FROM information_schema.tables WHERE table_schema = ? AND table_name IN ('players','business_positions','business_employees')")) {
                ps.setString(1, dbNameIfMySQL);
                try (ResultSet rs = ps.executeQuery()) {
                    int count = 0;
                    while (rs.next()) count++;
                    assertTrue(count >= 3, "Expected at least 3 core tables in MySQL schema");
                }
            }
        } else {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name IN ('players','business_positions','business_employees')")) {
                try (ResultSet rs = ps.executeQuery()) {
                    int count = 0;
                    while (rs.next()) count++;
                    assertTrue(count >= 3, "Expected at least 3 core tables in SQLite DB");
                }
            }
        }
    }

    private void doSimpleCrudOnPlayers(Connection conn) throws Exception {
        String uuid = UUID.randomUUID().toString();
        String name = "TestUser";
        double money = 1234.56;
        try (PreparedStatement insert = conn.prepareStatement("INSERT INTO players (uuid, username, money) VALUES (?, ?, ?)");) {
            insert.setString(1, uuid);
            insert.setString(2, name);
            insert.setDouble(3, money);
            int rows = insert.executeUpdate();
            assertEquals(1, rows, "Insert should affect 1 row");
        }
        try (PreparedStatement ps = conn.prepareStatement("SELECT username, money FROM players WHERE uuid = ?")) {
            ps.setString(1, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "Inserted player should exist");
                assertEquals(name, rs.getString(1));
                assertEquals(money, rs.getDouble(2), 0.001);
            }
        }
    }

    @Nested
    class SQLiteIntegration {
        @TempDir
        Path tempDir;

        @Test
        @Tag("integration")
        void sqlite_initialize_and_crud() throws Exception {
            FileConfiguration cfg = sqliteConfig();
            DynamicJobsEconomy plugin = mockPlugin(tempDir, cfg);

            db = new DatabaseManager(plugin);
            assertTrue(db.initialize(), "SQLite initialize() should return true");
            assertEquals("sqlite", db.getDatabaseType());

            try (Connection conn = db.getConnection()) {
                assertNotNull(conn);
                assertFalse(conn.isClosed());
                assertCoreTablesExist(conn, false, null);
                doSimpleCrudOnPlayers(conn);
            }

            // Acquire another connection after closing previous (pool should handle it)
            try (Connection conn2 = db.getConnection()) {
                try (Statement s = conn2.createStatement()) {
                    try (ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM players")) {
                        assertTrue(rs.next());
                        assertTrue(rs.getInt(1) >= 1);
                    }
                }
            }
        }
    }

    @Nested
    @Testcontainers
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
        void mysql_initialize_and_crud() throws Exception {
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

            try (Connection conn = db.getConnection()) {
                assertNotNull(conn);
                assertFalse(conn.isClosed());
                assertCoreTablesExist(conn, true, MYSQL.getDatabaseName());
                doSimpleCrudOnPlayers(conn);
            }

            // Ensure pool returns usable connection again
            try (Connection conn2 = db.getConnection()) {
                try (Statement s = conn2.createStatement()) {
                    try (ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM players")) {
                        assertTrue(rs.next());
                        assertTrue(rs.getInt(1) >= 1);
                    }
                }
            }
        }
    }
}
