package com.nexus.service;

import com.nexus.model.AuditLogEntry;
import com.nexus.model.CommandHistoryItem;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.Properties;

public class DatabaseService {

    private static final DatabaseService INSTANCE = new DatabaseService();

    private String host = "localhost";
    private int port = 3306;
    private String database = "nexus_db";
    private String username = "root";
    private String password = "root";
    private boolean connected = false;

    private DatabaseService() {
        loadConfiguration();
        initDatabase();
    }

    public static DatabaseService getInstance() {
        return INSTANCE;
    }

    public void loadConfiguration() {
        // 1. Environment variables
        String envHost = System.getenv("NEXUS_DB_HOST");
        String envPort = System.getenv("NEXUS_DB_PORT");
        String envDb = System.getenv("NEXUS_DB_NAME");
        String envUser = System.getenv("NEXUS_DB_USER");
        String envPass = System.getenv("NEXUS_DB_PASSWORD");

        // 2. ~/.nexus/config.properties
        File configFile = new File(System.getProperty("user.home"), ".nexus/config.properties");
        if (configFile.exists()) {
            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream(configFile)) {
                props.load(fis);
                if (envHost == null) envHost = props.getProperty("db.host");
                if (envPort == null) envPort = props.getProperty("db.port");
                if (envDb == null) envDb = props.getProperty("db.database");
                if (envUser == null) envUser = props.getProperty("db.user");
                if (envPass == null) envPass = props.getProperty("db.password");
            } catch (IOException ignored) {}
        }

        if (envHost != null) this.host = envHost.trim();
        if (envPort != null) {
            try { this.port = Integer.parseInt(envPort.trim()); } catch (NumberFormatException ignored) {}
        }
        if (envDb != null) this.database = envDb.trim();
        if (envUser != null) this.username = envUser.trim();
        if (envPass != null) this.password = envPass.trim();
    }

    public Connection getConnection() throws SQLException {
        String url = String.format("jdbc:mysql://%s:%d/%s?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC",
                host, port, database);
        return DriverManager.getConnection(url, username, password);
    }

    public synchronized boolean initDatabase() {
        try {
            // Ensure database exists
            String serverUrl = String.format("jdbc:mysql://%s:%d/?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC",
                    host, port);
            try (Connection conn = DriverManager.getConnection(serverUrl, username, password);
                 Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + database);
            }

            // Create tables
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement()) {

                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS command_history (
                        id VARCHAR(64) PRIMARY KEY,
                        command TEXT NOT NULL,
                        working_directory TEXT,
                        status VARCHAR(32),
                        exit_code INT,
                        duration_ms BIGINT,
                        tool_category VARCHAR(64),
                        executed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);

                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS ai_audit_log (
                        id VARCHAR(64) PRIMARY KEY,
                        timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        tool_name VARCHAR(128) NOT NULL,
                        permission_level VARCHAR(64),
                        arguments_summary TEXT,
                        decision VARCHAR(64),
                        success BOOLEAN,
                        result_summary TEXT
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);

                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS ai_conversations (
                        id VARCHAR(64) PRIMARY KEY,
                        conversation_id VARCHAR(64),
                        role VARCHAR(32),
                        content TEXT,
                        code_snippet TEXT,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
            }

            this.connected = true;
            System.out.println("[NEXUS] Successfully connected to MySQL database: " + database + " on " + host + ":" + port);
            return true;
        } catch (SQLException e) {
            this.connected = false;
            System.err.println("[NEXUS] MySQL connection failed: " + e.getMessage());
            return false;
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public void saveAuditEntry(AuditLogEntry entry) {
        if (!connected) return;
        String sql = "INSERT INTO ai_audit_log (id, timestamp, tool_name, permission_level, arguments_summary, decision, success, result_summary) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, entry.getId());
            ps.setTimestamp(2, Timestamp.valueOf(entry.getTimestamp()));
            ps.setString(3, entry.getToolName());
            ps.setString(4, entry.getPermissionLevel().name());
            ps.setString(5, entry.getArgumentsSummary());
            ps.setString(6, entry.getDecision());
            ps.setBoolean(7, entry.isSuccess());
            ps.setString(8, entry.getResultSummary());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[NEXUS] Error persisting audit entry to MySQL: " + e.getMessage());
        }
    }

    public String getDatabaseStatus() {
        return connected
                ? String.format("CONNECTED (MySQL 8.0 // %s@%s:%d/%s)", username, host, port, database)
                : String.format("DISCONNECTED (MySQL // %s:%d/%s)", host, port, database);
    }
}

