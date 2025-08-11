package com.boopugstudios.dynamicjobseconomy.database;

import com.boopugstudios.dynamicjobseconomy.DynamicJobsEconomy;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public class DatabaseManager {
    
    private final DynamicJobsEconomy plugin;
    private Connection connection;
    private String databaseType;
    
    // Connection pooling
    private final ConcurrentLinkedQueue<Connection> connectionPool = new ConcurrentLinkedQueue<>();
    private final AtomicInteger activeConnections = new AtomicInteger(0);
    private static final int MAX_POOL_SIZE = 10;
    private static final int MIN_POOL_SIZE = 2;
    
    public DatabaseManager(DynamicJobsEconomy plugin) {
        this.plugin = plugin;
    }
    
    public boolean initialize() {
        FileConfiguration config = plugin.getConfig();
        databaseType = config.getString("database.type", "sqlite").toLowerCase();
        
        try {
            switch (databaseType) {
                case "sqlite":
                    return initializeSQLite();
                case "mysql":
                    return initializeMySQL();
                case "mongodb":
                    return initializeMongoDB();
                default:
                    plugin.getLogger().severe("Unknown database type: " + databaseType);
                    return false;
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize database", e);
            return false;
        }
    }
    
    private boolean initializeSQLite() throws SQLException {
        String url = "jdbc:sqlite:" + plugin.getDataFolder().getAbsolutePath() + "/database.db";
        connection = DriverManager.getConnection(url);
        
        createTables();
        plugin.getLogger().info("SQLite database initialized successfully!");
        return true;
    }
    
    private boolean initializeMySQL() throws SQLException {
        FileConfiguration config = plugin.getConfig();
        String host = config.getString("database.mysql.host");
        int port = config.getInt("database.mysql.port");
        String database = config.getString("database.mysql.database");
        String username = config.getString("database.mysql.username");
        String password = config.getString("database.mysql.password");
        boolean useSSL = config.getBoolean("database.mysql.useSSL");
        
        String url = String.format("jdbc:mysql://%s:%d/%s?useSSL=%s", host, port, database, useSSL);
        connection = DriverManager.getConnection(url, username, password);
        
        createTables();
        plugin.getLogger().info("MySQL database initialized successfully!");
        return true;
    }
    
    private boolean initializeMongoDB() {
        // MongoDB implementation would go here
        plugin.getLogger().info("MongoDB support is not yet implemented. Using SQL database instead.");
        return false;
    }
    
    private void createTables() throws SQLException {
        // Players table
        String playersTable = """
            CREATE TABLE IF NOT EXISTS players (
                uuid VARCHAR(36) PRIMARY KEY,
                username VARCHAR(16) NOT NULL,
                money DECIMAL(15,2) DEFAULT 1000.00,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;
        
        // Job levels table
        String jobLevelsTable = """
            CREATE TABLE IF NOT EXISTS job_levels (
                id INTEGER PRIMARY KEY AUTO_INCREMENT,
                player_uuid VARCHAR(36) NOT NULL,
                job_name VARCHAR(50) NOT NULL,
                level INTEGER DEFAULT 1,
                experience INTEGER DEFAULT 0,
                joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                UNIQUE KEY unique_player_job (player_uuid, job_name),
                FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE CASCADE
            )
        """;
        
        // Businesses table
        String businessesTable = """
            CREATE TABLE IF NOT EXISTS businesses (
                id INTEGER PRIMARY KEY AUTO_INCREMENT,
                name VARCHAR(100) NOT NULL,
                owner_uuid VARCHAR(36) NOT NULL,
                type VARCHAR(50) NOT NULL,
                balance DECIMAL(15,2) DEFAULT 0.00,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (owner_uuid) REFERENCES players(uuid) ON DELETE CASCADE
            )
        """;
        
        // Business employees table
        String employeesTable = """
            CREATE TABLE IF NOT EXISTS business_employees (
                id INTEGER PRIMARY KEY AUTO_INCREMENT,
                business_id INTEGER NOT NULL,
                employee_uuid VARCHAR(36) NOT NULL,
                position VARCHAR(50) DEFAULT 'Employee',
                salary DECIMAL(10,2) DEFAULT 0.00,
                hired_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE,
                FOREIGN KEY (employee_uuid) REFERENCES players(uuid) ON DELETE CASCADE
            )
        """;
        
        // Market prices table
        String marketTable = """
            CREATE TABLE IF NOT EXISTS market_prices (
                id INTEGER PRIMARY KEY AUTO_INCREMENT,
                item_name VARCHAR(100) NOT NULL,
                price DECIMAL(10,2) NOT NULL,
                supply INTEGER DEFAULT 0,
                demand INTEGER DEFAULT 0,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;
        
        // Gigs table
        String gigsTable = """
            CREATE TABLE IF NOT EXISTS gigs (
                id INTEGER PRIMARY KEY AUTO_INCREMENT,
                title VARCHAR(200) NOT NULL,
                description TEXT,
                poster_uuid VARCHAR(36) NOT NULL,
                worker_uuid VARCHAR(36),
                payment DECIMAL(10,2) NOT NULL,
                status ENUM('OPEN', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED') DEFAULT 'OPEN',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                completed_at TIMESTAMP NULL,
                FOREIGN KEY (poster_uuid) REFERENCES players(uuid) ON DELETE CASCADE,
                FOREIGN KEY (worker_uuid) REFERENCES players(uuid) ON DELETE SET NULL
            )
        """;
        
        // Execute table creation
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(playersTable);
            stmt.execute(jobLevelsTable);
            stmt.execute(businessesTable);
            stmt.execute(employeesTable);
            stmt.execute(marketTable);
            stmt.execute(gigsTable);
        }
        
        plugin.getLogger().info("Database tables created successfully!");
    }
    
    public Connection getConnection() {
        // Try to get connection from pool first
        Connection conn = connectionPool.poll();
        
        if (conn != null) {
            try {
                if (!conn.isClosed() && conn.isValid(5)) {
                    return conn;
                } else {
                    // Connection is invalid, close it and create new one
                    try {
                        conn.close();
                    } catch (SQLException ignored) {}
                    activeConnections.decrementAndGet();
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Error validating pooled connection", e);
            }
        }
        
        // Create new connection if pool is empty or connection was invalid
        if (activeConnections.get() < MAX_POOL_SIZE) {
            conn = createNewConnection();
            if (conn != null) {
                activeConnections.incrementAndGet();
                return conn;
            }
        }
        
        // Fallback to original connection if pool is exhausted
        try {
            if (connection == null || connection.isClosed()) {
                initialize();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error checking database connection", e);
        }
        return connection;
    }
    
    /**
     * Returns a connection to the pool for reuse
     */
    public void returnConnection(Connection conn) {
        if (conn != null && connectionPool.size() < MAX_POOL_SIZE) {
            try {
                if (!conn.isClosed() && conn.isValid(5)) {
                    connectionPool.offer(conn);
                    return;
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Error validating connection for return to pool", e);
            }
        }
        
        // Close connection if it can't be returned to pool
        if (conn != null) {
            try {
                conn.close();
                activeConnections.decrementAndGet();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Error closing database connection", e);
            }
        }
    }
    
    /**
     * Creates a new database connection based on the configured type
     */
    private Connection createNewConnection() {
        try {
            switch (databaseType) {
                case "sqlite":
                    return createSQLiteConnection();
                case "mysql":
                    return createMySQLConnection();
                case "mongodb":
                    // MongoDB doesn't use SQL connections, return null
                    return null;
                default:
                    plugin.getLogger().severe("Unknown database type for connection creation: " + databaseType);
                    return null;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error creating new database connection", e);
            return null;
        }
    }
    
    private Connection createSQLiteConnection() throws SQLException {
        String url = "jdbc:sqlite:" + plugin.getDataFolder() + "/database.db";
        return DriverManager.getConnection(url);
    }
    
    private Connection createMySQLConnection() throws SQLException {
        FileConfiguration config = plugin.getConfig();
        String host = config.getString("database.mysql.host", "localhost");
        int port = config.getInt("database.mysql.port", 3306);
        String database = config.getString("database.mysql.database", "dynamicjobs");
        String username = config.getString("database.mysql.username", "root");
        String password = config.getString("database.mysql.password", "");
        
        String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&serverTimezone=UTC";
        return DriverManager.getConnection(url, username, password);
    }
    
    public void closeConnections() {
        // Close all pooled connections
        Connection conn;
        int closedCount = 0;
        while ((conn = connectionPool.poll()) != null) {
            try {
                if (!conn.isClosed()) {
                    conn.close();
                    closedCount++;
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Error closing pooled connection", e);
            }
        }
        
        // Close main connection
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                closedCount++;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error closing main database connection", e);
        }
        
        // Reset counters
        activeConnections.set(0);
        plugin.getLogger().info("Closed " + closedCount + " database connections successfully");
    }
    
    /**
     * Initialize minimum connections in the pool for better performance
     */
    public void initializeConnectionPool() {
        if (!databaseType.equals("mongodb")) { // Only for SQL databases
            plugin.getLogger().info("Initializing database connection pool...");
            
            for (int i = 0; i < MIN_POOL_SIZE; i++) {
                Connection conn = createNewConnection();
                if (conn != null) {
                    connectionPool.offer(conn);
                    activeConnections.incrementAndGet();
                }
            }
            
            plugin.getLogger().info("Connection pool initialized with " + connectionPool.size() + " connections");
        }
    }
    
    public String getDatabaseType() {
        return databaseType;
    }
}
