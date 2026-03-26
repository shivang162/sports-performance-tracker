package com.tracker.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for the performance_records table.
 * Handles all database operations: insert, fetch all, fetch by athlete.
 *
 * Falls back gracefully if DB is unavailable (returns empty list / false).
 *
 * Author: Member 3 (DB) + Team Lead (integration)
 */
public class PerformanceDAO {

    // ─── Insert a new record ───────────────────────────────────────────────

    /**
     * Saves a performance record to the database.
     *
     * @return true if saved successfully, false if DB unavailable
     */
    public boolean insertRecord(String athlete, double distance, double timeSec,
                                double speed, double accuracy, double stamina,
                                double score, String level) {
        String sql = "INSERT INTO performance_records " +
                    "(athlete, distance, time_sec, speed, accuracy, stamina, score, level) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            Connection conn = DBConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, athlete);
            ps.setDouble(2, distance);
            ps.setDouble(3, timeSec);
            ps.setDouble(4, speed);
            ps.setDouble(5, accuracy);
            ps.setDouble(6, stamina);
            ps.setDouble(7, score);
            ps.setString(8, level);

            int rows = ps.executeUpdate();
            ps.close();
            System.out.println("[PerformanceDAO] Record inserted for: " + athlete);
            return rows > 0;

        } catch (SQLException e) {
            System.err.println("[PerformanceDAO] insertRecord failed: " + e.getMessage());
            return false;
        }
    }

    // ─── Fetch all records ────────────────────────────────────────────────

    /**
     * Returns all performance records, newest first.
     * Used by DashboardController to compute analysis stats.
     *
     * @return List of record rows as double[] {distance, timeSec, speed, accuracy, stamina, score}
     *         Returns empty list if DB unavailable.
     */
    public List<double[]> getAllRecords() {
        List<double[]> records = new ArrayList<>();
        String sql = "SELECT distance, time_sec, speed, accuracy, stamina, score " +
                     "FROM performance_records ORDER BY created_at ASC";
        try {
            Connection conn = DBConnection.getConnection();
            Statement  stmt = conn.createStatement();
            ResultSet  rs   = stmt.executeQuery(sql);

            while (rs.next()) {
                double[] row = {
                    rs.getDouble("distance"),
                    rs.getDouble("time_sec"),
                    rs.getDouble("speed"),
                    rs.getDouble("accuracy"),
                    rs.getDouble("stamina"),
                    rs.getDouble("score")
                };
                records.add(row);
            }
            rs.close();
            stmt.close();

        } catch (SQLException e) {
            System.err.println("[PerformanceDAO] getAllRecords failed: " + e.getMessage());
        }
        return records;
    }

    // ─── Fetch all scores (for analysis) ─────────────────────────────────

    /**
     * Returns just the score column, in chronological order.
     * Used directly by PerformanceService for trend and average calculations.
     */
    public List<Double> getAllScores() {
        List<Double> scores = new ArrayList<>();
        String sql = "SELECT score FROM performance_records ORDER BY created_at ASC";
        try {
            Connection conn = DBConnection.getConnection();
            Statement  stmt = conn.createStatement();
            ResultSet  rs   = stmt.executeQuery(sql);

            while (rs.next()) {
                scores.add(rs.getDouble("score"));
            }
            rs.close();
            stmt.close();

        } catch (SQLException e) {
            System.err.println("[PerformanceDAO] getAllScores failed: " + e.getMessage());
        }
        return scores;
    }

    // ─── Fetch records by athlete name ────────────────────────────────────

    /**
     * Returns all records for a specific athlete, chronological order.
     */
    public List<double[]> getRecordsByAthlete(String athleteName) {
        List<double[]> records = new ArrayList<>();
        String sql = "SELECT distance, time_sec, speed, accuracy, stamina, score " +
                     "FROM performance_records WHERE athlete = ? ORDER BY created_at ASC";
        try {
            Connection       conn = DBConnection.getConnection();
            PreparedStatement ps  = conn.prepareStatement(sql);
            ps.setString(1, athleteName);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                double[] row = {
                    rs.getDouble("distance"),
                    rs.getDouble("time_sec"),
                    rs.getDouble("speed"),
                    rs.getDouble("accuracy"),
                    rs.getDouble("stamina"),
                    rs.getDouble("score")
                };
                records.add(row);
            }
            rs.close();
            ps.close();

        } catch (SQLException e) {
            System.err.println("[PerformanceDAO] getRecordsByAthlete failed: " + e.getMessage());
        }
        return records;
    }

    // ─── Count total records ─────────────────────────────────────────────

    /**
     * Returns total number of sessions recorded.
     */
    public int getTotalCount() {
        String sql = "SELECT COUNT(*) AS cnt FROM performance_records";
        try {
            Connection conn = DBConnection.getConnection();
            Statement  stmt = conn.createStatement();
            ResultSet  rs   = stmt.executeQuery(sql);
            if (rs.next()) {
                int count = rs.getInt("cnt");
                rs.close(); stmt.close();
                return count;
            }
        } catch (SQLException e) {
            System.err.println("[PerformanceDAO] getTotalCount failed: " + e.getMessage());
        }
        return 0;
    }
}
