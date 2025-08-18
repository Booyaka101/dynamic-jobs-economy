package com.boopugstudios.dynamicjobseconomy.database;

import com.boopugstudios.dynamicjobseconomy.DynamicJobsEconomy;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
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
        // Ensure plugin data directory exists
        if (!plugin.getDataFolder().exists()) {
            //noinspection ResultOfMethodCallIgnored
            plugin.getDataFolder().mkdirs();
        }
        String url = "jdbc:sqlite:" + plugin.getDataFolder().getAbsolutePath() + "/database.db";
        connection = DriverManager.getConnection(url);
        // Ensure foreign keys are enforced on SQLite
        try (Statement s = connection.createStatement()) {
            s.execute("PRAGMA foreign_keys = ON");
        }
        
        createTables();
        initializeConnectionPool();
        plugin.getLogger().info("SQLite database initialized successfully!");
        return true;
    }
    
    private boolean initializeMySQL() throws SQLException {
        connection = createMySQLConnection();
        createTables();
        initializeConnectionPool();
        plugin.getLogger().info("MySQL database initialized successfully!");
        return true;
    }
    
    private boolean initializeMongoDB() {
        // MongoDB implementation would go here
        plugin.getLogger().info("MongoDB support is not yet implemented. Using SQL database instead.");
        return false;
    }
    
    private void createTables() throws SQLException {
        boolean isSQLite = "sqlite".equalsIgnoreCase(databaseType);

        // Players table
        String playersTable = isSQLite ? """
            CREATE TABLE IF NOT EXISTS players (
                uuid TEXT PRIMARY KEY,
                username TEXT NOT NULL,
                money REAL DEFAULT 1000.00,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                last_seen DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        """ : """
            CREATE TABLE IF NOT EXISTS players (
                uuid VARCHAR(36) PRIMARY KEY,
                username VARCHAR(16) NOT NULL,
                money DECIMAL(15,2) DEFAULT 1000.00,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;
        
        // Job levels table
        String jobLevelsTable = isSQLite ? """
            CREATE TABLE IF NOT EXISTS job_levels (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player_uuid TEXT NOT NULL,
                job_name TEXT NOT NULL,
                level INTEGER DEFAULT 1,
                experience INTEGER DEFAULT 0,
                joined_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                UNIQUE (player_uuid, job_name),
                FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE CASCADE
            )
        """ : """
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
        String businessesTable = isSQLite ? """
            CREATE TABLE IF NOT EXISTS businesses (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                owner_uuid TEXT NOT NULL,
                type TEXT NOT NULL,
                revenue_model TEXT DEFAULT 'STARTUP',
                balance REAL DEFAULT 0.00,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (owner_uuid) REFERENCES players(uuid) ON DELETE CASCADE
            )
        """ : """
            CREATE TABLE IF NOT EXISTS businesses (
                id INTEGER PRIMARY KEY AUTO_INCREMENT,
                name VARCHAR(100) NOT NULL,
                owner_uuid VARCHAR(36) NOT NULL,
                type VARCHAR(50) NOT NULL,
                revenue_model VARCHAR(50) DEFAULT 'STARTUP',
                balance DECIMAL(15,2) DEFAULT 0.00,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (owner_uuid) REFERENCES players(uuid) ON DELETE CASCADE
            )
        """;
        
        // Business employees table
        // Business positions table
        String positionsTable = isSQLite ? """
            CREATE TABLE IF NOT EXISTS business_positions (
                position_id INTEGER PRIMARY KEY AUTOINCREMENT,
                business_id INTEGER NOT NULL,
                title TEXT NOT NULL,
                salary REAL DEFAULT 0.00,
                description TEXT,
                max_employees INTEGER DEFAULT 1,
                is_active INTEGER DEFAULT 1,
                created_by TEXT,
                created_at INTEGER NOT NULL,
                FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE
            )
        """ : """
            CREATE TABLE IF NOT EXISTS business_positions (
                position_id INTEGER PRIMARY KEY AUTO_INCREMENT,
                business_id INTEGER NOT NULL,
                title VARCHAR(100) NOT NULL,
                salary DECIMAL(10,2) DEFAULT 0.00,
                description TEXT,
                max_employees INTEGER DEFAULT 1,
                is_active BOOLEAN DEFAULT TRUE,
                created_by VARCHAR(36),
                created_at BIGINT NOT NULL,
                FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE
            )
        """;

        // Business employees table (aligned with code usage)
        String employeesTable = isSQLite ? """
            CREATE TABLE IF NOT EXISTS business_employees (
                employee_id INTEGER PRIMARY KEY AUTOINCREMENT,
                business_id INTEGER NOT NULL,
                position_id INTEGER NOT NULL,
                player_uuid TEXT NOT NULL,
                player_name TEXT,
                current_salary REAL DEFAULT 0.00,
                hired_at INTEGER NOT NULL,
                is_active INTEGER DEFAULT 1,
                notes TEXT,
                FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE,
                FOREIGN KEY (position_id) REFERENCES business_positions(position_id) ON DELETE CASCADE,
                FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE CASCADE
            )
        """ : """
            CREATE TABLE IF NOT EXISTS business_employees (
                employee_id INTEGER PRIMARY KEY AUTO_INCREMENT,
                business_id INTEGER NOT NULL,
                position_id INTEGER NOT NULL,
                player_uuid VARCHAR(36) NOT NULL,
                player_name VARCHAR(50),
                current_salary DECIMAL(10,2) DEFAULT 0.00,
                hired_at BIGINT NOT NULL,
                is_active BOOLEAN DEFAULT TRUE,
                notes TEXT,
                FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE,
                FOREIGN KEY (position_id) REFERENCES business_positions(position_id) ON DELETE CASCADE,
                FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE CASCADE
            )
        """;

        // Employee notes table
        String employeeNotesTable = isSQLite ? """
            CREATE TABLE IF NOT EXISTS employee_notes (
                note_id INTEGER PRIMARY KEY AUTOINCREMENT,
                business_id INTEGER NOT NULL,
                player_uuid TEXT NOT NULL,
                note TEXT NOT NULL,
                author TEXT,
                created_at INTEGER NOT NULL,
                FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE,
                FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE CASCADE
            )
        """ : """
            CREATE TABLE IF NOT EXISTS employee_notes (
                note_id INTEGER PRIMARY KEY AUTO_INCREMENT,
                business_id INTEGER NOT NULL,
                player_uuid VARCHAR(36) NOT NULL,
                note TEXT NOT NULL,
                author VARCHAR(50),
                created_at BIGINT NOT NULL,
                FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE,
                FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE CASCADE
            )
        """;

        // Hiring requests table
        String hiringRequestsTable = isSQLite ? """
            CREATE TABLE IF NOT EXISTS hiring_requests (
                request_id INTEGER PRIMARY KEY AUTOINCREMENT,
                business_id INTEGER NOT NULL,
                position_id INTEGER NOT NULL,
                player_uuid TEXT NOT NULL,
                requested_by TEXT NOT NULL,
                offered_salary REAL NOT NULL,
                message TEXT,
                request_time INTEGER NOT NULL,
                expiration_time INTEGER NOT NULL,
                status TEXT NOT NULL,
                rejection_reason TEXT,
                FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE,
                FOREIGN KEY (position_id) REFERENCES business_positions(position_id) ON DELETE CASCADE,
                FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE CASCADE
            )
        """ : """
            CREATE TABLE IF NOT EXISTS hiring_requests (
                request_id INTEGER PRIMARY KEY AUTO_INCREMENT,
                business_id INTEGER NOT NULL,
                position_id INTEGER NOT NULL,
                player_uuid VARCHAR(36) NOT NULL,
                requested_by VARCHAR(36) NOT NULL,
                offered_salary DECIMAL(10,2) NOT NULL,
                message TEXT,
                request_time BIGINT NOT NULL,
                expiration_time BIGINT NOT NULL,
                status VARCHAR(20) NOT NULL,
                rejection_reason TEXT,
                FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE,
                FOREIGN KEY (position_id) REFERENCES business_positions(position_id) ON DELETE CASCADE,
                FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE CASCADE
            )
        """;

        // Business transactions table (used by BusinessAnalytics)
        String businessTransactionsTable = isSQLite ? """
            CREATE TABLE IF NOT EXISTS business_transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                business_id INTEGER NOT NULL,
                transaction_type TEXT NOT NULL,
                amount REAL NOT NULL,
                description TEXT,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE
            )
        """ : """
            CREATE TABLE IF NOT EXISTS business_transactions (
                id INTEGER PRIMARY KEY AUTO_INCREMENT,
                business_id INTEGER NOT NULL,
                transaction_type VARCHAR(20) NOT NULL,
                amount DECIMAL(10,2) NOT NULL,
                description TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE
            )
        """;

        // Employee performance table (for productivity metrics)
        String employeePerformanceTable = isSQLite ? """
            CREATE TABLE IF NOT EXISTS employee_performance (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                business_id INTEGER NOT NULL,
                employee_uuid TEXT NOT NULL,
                performance_rating REAL NOT NULL,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE
            )
        """ : """
            CREATE TABLE IF NOT EXISTS employee_performance (
                id INTEGER PRIMARY KEY AUTO_INCREMENT,
                business_id INTEGER NOT NULL,
                employee_uuid VARCHAR(36) NOT NULL,
                performance_rating DECIMAL(5,2) NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE
            )
        """;
        
        // Market prices table
        String marketTable = isSQLite ? """
            CREATE TABLE IF NOT EXISTS market_prices (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                item_name TEXT NOT NULL,
                price REAL NOT NULL,
                supply INTEGER DEFAULT 0,
                demand INTEGER DEFAULT 0,
                updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        """ : """
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
        String gigsTable = isSQLite ? """
            CREATE TABLE IF NOT EXISTS gigs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                description TEXT,
                poster_uuid TEXT NOT NULL,
                worker_uuid TEXT,
                payment REAL NOT NULL,
                status TEXT DEFAULT 'OPEN',
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                completed_at DATETIME NULL,
                FOREIGN KEY (poster_uuid) REFERENCES players(uuid) ON DELETE CASCADE,
                FOREIGN KEY (worker_uuid) REFERENCES players(uuid) ON DELETE SET NULL
            )
        """ : """
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
            // Attempt to add revenue_model column for existing installations (ignore if already exists)
            try {
                String alterBusinessesRevenueModel = isSQLite
                    ? "ALTER TABLE businesses ADD COLUMN revenue_model TEXT DEFAULT 'STARTUP'"
                    : "ALTER TABLE businesses ADD COLUMN revenue_model VARCHAR(50) DEFAULT 'STARTUP'";
                stmt.execute(alterBusinessesRevenueModel);
            } catch (SQLException ignore) {
                // Column may already exist; safe to ignore
            }
            stmt.execute(positionsTable);
            stmt.execute(employeesTable);
            stmt.execute(marketTable);
            stmt.execute(gigsTable);
            stmt.execute(employeeNotesTable);
            stmt.execute(hiringRequestsTable);
            stmt.execute(businessTransactionsTable);
            stmt.execute(employeePerformanceTable);

            // Safe migrations for existing databases
            try {
                String alterEmployeesHiredAt = isSQLite
                    ? "ALTER TABLE business_employees ADD COLUMN hired_at INTEGER NOT NULL DEFAULT 0"
                    : "ALTER TABLE business_employees ADD COLUMN hired_at BIGINT NOT NULL DEFAULT 0";
                stmt.execute(alterEmployeesHiredAt);
            } catch (SQLException ignore) { }

            try {
                String alterHiringRequestTime = isSQLite
                    ? "ALTER TABLE hiring_requests ADD COLUMN request_time INTEGER NOT NULL DEFAULT 0"
                    : "ALTER TABLE hiring_requests ADD COLUMN request_time BIGINT NOT NULL DEFAULT 0";
                stmt.execute(alterHiringRequestTime);
            } catch (SQLException ignore) { }

            try {
                String alterHiringExpirationTime = isSQLite
                    ? "ALTER TABLE hiring_requests ADD COLUMN expiration_time INTEGER NOT NULL DEFAULT 0"
                    : "ALTER TABLE hiring_requests ADD COLUMN expiration_time BIGINT NOT NULL DEFAULT 0";
                stmt.execute(alterHiringExpirationTime);
            } catch (SQLException ignore) { }

            // Helpful indexes (ignore errors if they already exist)
            try {
                String idxEmployeesPlayer = isSQLite
                    ? "CREATE INDEX IF NOT EXISTS idx_emp_player ON business_employees(player_uuid)"
                    : "CREATE INDEX idx_emp_player ON business_employees(player_uuid)";
                stmt.execute(idxEmployeesPlayer);
            } catch (SQLException ignore) { }

            try {
                String idxEmployeesBusiness = isSQLite
                    ? "CREATE INDEX IF NOT EXISTS idx_emp_business ON business_employees(business_id)"
                    : "CREATE INDEX idx_emp_business ON business_employees(business_id)";
                stmt.execute(idxEmployeesBusiness);
            } catch (SQLException ignore) { }

            try {
                String idxEmployeesPosition = isSQLite
                    ? "CREATE INDEX IF NOT EXISTS idx_emp_position ON business_employees(position_id)"
                    : "CREATE INDEX idx_emp_position ON business_employees(position_id)";
                stmt.execute(idxEmployeesPosition);
            } catch (SQLException ignore) { }

            try {
                String idxHiringPlayer = isSQLite
                    ? "CREATE INDEX IF NOT EXISTS idx_hiring_player ON hiring_requests(player_uuid)"
                    : "CREATE INDEX idx_hiring_player ON hiring_requests(player_uuid)";
                stmt.execute(idxHiringPlayer);
            } catch (SQLException ignore) { }

            try {
                String idxNotesPlayer = isSQLite
                    ? "CREATE INDEX IF NOT EXISTS idx_notes_player ON employee_notes(player_uuid)"
                    : "CREATE INDEX idx_notes_player ON employee_notes(player_uuid)";
                stmt.execute(idxNotesPlayer);
            } catch (SQLException ignore) { }

            // Additional helpful indexes
            try {
                String idxPositionsBusiness = isSQLite
                    ? "CREATE INDEX IF NOT EXISTS idx_positions_business ON business_positions(business_id)"
                    : "CREATE INDEX idx_positions_business ON business_positions(business_id)";
                stmt.execute(idxPositionsBusiness);
            } catch (SQLException ignore) { }

            try {
                String idxPositionsBusinessActive = isSQLite
                    ? "CREATE INDEX IF NOT EXISTS idx_positions_business_active ON business_positions(business_id, is_active)"
                    : "CREATE INDEX idx_positions_business_active ON business_positions(business_id, is_active)";
                stmt.execute(idxPositionsBusinessActive);
            } catch (SQLException ignore) { }

            try {
                String idxHiringBusiness = isSQLite
                    ? "CREATE INDEX IF NOT EXISTS idx_hiring_business ON hiring_requests(business_id)"
                    : "CREATE INDEX idx_hiring_business ON hiring_requests(business_id)";
                stmt.execute(idxHiringBusiness);
            } catch (SQLException ignore) { }

            try {
                String idxHiringPosition = isSQLite
                    ? "CREATE INDEX IF NOT EXISTS idx_hiring_position ON hiring_requests(position_id)"
                    : "CREATE INDEX idx_hiring_position ON hiring_requests(position_id)";
                stmt.execute(idxHiringPosition);
            } catch (SQLException ignore) { }

            try {
                String idxHiringStatus = isSQLite
                    ? "CREATE INDEX IF NOT EXISTS idx_hiring_status ON hiring_requests(status)"
                    : "CREATE INDEX idx_hiring_status ON hiring_requests(status)";
                stmt.execute(idxHiringStatus);
            } catch (SQLException ignore) { }

            try {
                String idxTransBusinessCreated = isSQLite
                    ? "CREATE INDEX IF NOT EXISTS idx_trans_business_created ON business_transactions(business_id, created_at)"
                    : "CREATE INDEX idx_trans_business_created ON business_transactions(business_id, created_at)";
                stmt.execute(idxTransBusinessCreated);
            } catch (SQLException ignore) { }

            try {
                String idxNotesBusiness = isSQLite
                    ? "CREATE INDEX IF NOT EXISTS idx_notes_business ON employee_notes(business_id)"
                    : "CREATE INDEX idx_notes_business ON employee_notes(business_id)";
                stmt.execute(idxNotesBusiness);
            } catch (SQLException ignore) { }

            try {
                String idxGigsStatus = isSQLite
                    ? "CREATE INDEX IF NOT EXISTS idx_gigs_status ON gigs(status)"
                    : "CREATE INDEX idx_gigs_status ON gigs(status)";
                stmt.execute(idxGigsStatus);
            } catch (SQLException ignore) { }

            try {
                String idxMarketItem = isSQLite
                    ? "CREATE INDEX IF NOT EXISTS idx_market_item ON market_prices(item_name)"
                    : "CREATE INDEX idx_market_item ON market_prices(item_name)";
                stmt.execute(idxMarketItem);
            } catch (SQLException ignore) { }
        }
        
        plugin.getLogger().info("Database tables created successfully!");
    }
    
    public Connection getConnection() {
        // Try to get connection from pool first
        Connection conn = connectionPool.poll();
        
        if (conn != null) {
            try {
                if (!conn.isClosed() && conn.isValid(5)) {
                    return wrapConnection(conn);
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
                return wrapConnection(conn);
            }
        }
        
        // Fallback to original connection if pool is exhausted
        try {
            if (connection == null || connection.isClosed()) {
                connection = createNewConnection();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error checking database connection", e);
        }
        return wrapConnection(connection);
    }

    /**
     * Wrap a JDBC Connection so that close() returns it to our pool instead of actually closing it.
     */
    private Connection wrapConnection(final Connection underlying) {
        if (underlying == null) return null;
        final boolean[] closed = {false};
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                String name = method.getName();
                if ("close".equals(name)) {
                    if (!closed[0]) {
                        closed[0] = true;
                        returnConnection(underlying);
                    }
                    return null;
                }
                if ("isClosed".equals(name)) {
                    return closed[0] || underlying.isClosed();
                }
                return method.invoke(underlying, args);
            }
        };
        return (Connection) Proxy.newProxyInstance(
            Connection.class.getClassLoader(),
            new Class[]{Connection.class},
            handler
        );
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
        String url = "jdbc:sqlite:" + plugin.getDataFolder().getAbsolutePath() + "/database.db";
        Connection conn = DriverManager.getConnection(url);
        try (Statement s = conn.createStatement()) {
            s.execute("PRAGMA foreign_keys = ON");
            // Improve concurrency and durability for SQLite
            s.execute("PRAGMA journal_mode = WAL");
            s.execute("PRAGMA synchronous = NORMAL");
            s.execute("PRAGMA busy_timeout = 5000");
        }
        return conn;
    }
    
    private Connection createMySQLConnection() throws SQLException {
        FileConfiguration config = plugin.getConfig();
        String host = config.getString("database.mysql.host", "localhost");
        int port = config.getInt("database.mysql.port", 3306);
        String database = config.getString("database.mysql.database", "dynamicjobs");
        String username = config.getString("database.mysql.username", "root");
        String password = config.getString("database.mysql.password", "");
        boolean useSSL = config.getBoolean("database.mysql.useSSL", false);

        String url = String.format(
            "jdbc:mysql://%s:%d/%s?useSSL=%s&serverTimezone=UTC&useUnicode=true&characterEncoding=utf8mb4&allowPublicKeyRetrieval=true&rewriteBatchedStatements=true&cachePrepStmts=true&prepStmtCacheSize=250&prepStmtCacheSqlLimit=2048&useServerPrepStmts=true&connectTimeout=10000&socketTimeout=60000&tcpKeepAlive=true",
            host, port, database, Boolean.toString(useSSL)
        );
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
