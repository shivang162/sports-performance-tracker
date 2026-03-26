package com.tracker.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Data Access Object for the users table.
 * Upgraded from hardcoded credentials to real MySQL query.
 *
 * Author: Member 3 (DB) — integrated by Team Lead
 */
public class UserDAO {

    /**
     * Validates a user's email and password against the users table in MySQL.
     * Returns true if a matching record is found.
     *
     * Falls back to hardcoded demo credentials if DB is unavailable,
     * so the app still works without a DB connection during development.
     */
    public boolean validateUser(String email, String password) {
        if (email == null || password == null) return false;

        String sql = "SELECT id FROM users WHERE email = ? AND password = ? LIMIT 1";

        try {
            Connection conn = DBConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, email.trim());
            ps.setString(2, password.trim());

            ResultSet rs   = ps.executeQuery();
            boolean found  = rs.next();   // true if a row exists

            rs.close();
            ps.close();

            System.out.println("[UserDAO] Login for " + email + ": " + (found ? "SUCCESS" : "FAILED"));
            return found;

        } catch (SQLException e) {
            System.err.println("[UserDAO] DB unavailable, using fallback: " + e.getMessage());

            // Fallback: hardcoded demo user so login works without DB
            return email.equals("coach@example.com") && password.equals("safePassword123");
        }
    }
}
