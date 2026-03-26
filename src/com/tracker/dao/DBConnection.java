package com.tracker.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Manages a single MySQL database connection (Singleton pattern).
 * All DAO classes call DBConnection.getConnection() to get the connection.
 *
 * Setup:
 *   1. Make sure MySQL is running
 *   2. Run database/schema.sql to create the database and tables
 *   3. Change DB_USER and DB_PASS below to match your MySQL credentials
 *
 * Author: Team Lead
 */
public class DBConnection {

    // ── Database config — change these to match your MySQL setup ──────────
    private static final String DB_URL  = "jdbc:mysql://localhost:3306/sports_tracker";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "root";         // change if needed

    // Singleton connection instance
    private static Connection connection = null;

    /**
     * Returns the existing connection, or creates a new one if it doesn't exist.
     * @return live MySQL Connection object
     * @throws SQLException if connection fails
     */
    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                // Load MySQL JDBC driver
                Class.forName("com.mysql.cj.jdbc.Driver");

                connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                System.out.println("[DBConnection] Connected to MySQL: " + DB_URL);

            } catch (ClassNotFoundException e) {
                throw new SQLException("MySQL JDBC driver not found. Add mysql-connector-java to your classpath.", e);
            }
        }
        return connection;
    }

    /**
     * Closes the connection. Call this when the application shuts down.
     */
    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                connection = null;
                System.out.println("[DBConnection] Connection closed.");
            } catch (SQLException e) {
                System.err.println("[DBConnection] Error closing connection: " + e.getMessage());
            }
        }
    }

    /**
     * Quick test — run this to verify DB connection before running the server.
     */
    public static void main(String[] args) {
        try {
            Connection conn = DBConnection.getConnection();
            if (conn != null) {
                System.out.println("SUCCESS: Database connected!");
            }
        } catch (SQLException e) {
            System.err.println("FAILED: " + e.getMessage());
            System.err.println("Make sure MySQL is running and schema.sql has been executed.");
        } finally {
            DBConnection.closeConnection();
        }
    }
}
