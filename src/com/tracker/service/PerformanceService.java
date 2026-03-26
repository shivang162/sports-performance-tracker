package com.tracker.service;

import com.tracker.dao.PerformanceDAO;
import java.util.List;

/**
 * Core logic service — Team Lead's main file.
 *
 * Methods:
 *   calculateScore          — weighted score (speed 40%, accuracy 30%, stamina 30%)
 *   calculateAverage        — average of a list of scores
 *   detectTrend             — Improving / Stable / Declining via linear regression slope
 *   calculatePeriodImprovement — % change from first-half avg to second-half avg
 *   getDashboardStats       — pulls scores from DB and returns full analysis
 *
 * Author: Team Lead
 */
public class PerformanceService {

    private final PerformanceDAO performanceDAO = new PerformanceDAO();

    // ── 1. Weighted Score ─────────────────────────────────────────────────

    public double calculateScore(double speed, double accuracy, double stamina) {
        return (speed * 0.4) + (accuracy * 0.3) + (stamina * 0.3);
    }

    // ── 2. Average ────────────────────────────────────────────────────────

    public double calculateAverage(List<Double> scores) {
        if (scores == null || scores.isEmpty()) return 0.0;
        double sum = 0.0;
        for (double s : scores) sum += s;
        return sum / scores.size();
    }

    // ── 3. Trend Detection (linear regression slope) ──────────────────────

    public String detectTrend(List<Double> scores) {
        if (scores == null || scores.size() < 2) return "Stable";

        int n = scores.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;

        for (int i = 0; i < n; i++) {
            double x = i, y = scores.get(i);
            sumX  += x;  sumY  += y;
            sumXY += x * y;  sumX2 += x * x;
        }

        double denom = (n * sumX2) - (sumX * sumX);
        if (denom == 0) return "Stable";

        double slope = ((n * sumXY) - (sumX * sumY)) / denom;

        if (slope > 0.5)       return "Improving";
        else if (slope < -0.5) return "Declining";
        else                   return "Stable";
    }

    // ── 4. Percentage Improvement (first half vs second half) ─────────────

    public double calculatePeriodImprovement(List<Double> scores) {
        if (scores == null || scores.size() < 2) return 0.0;

        int mid = scores.size() / 2;
        double avgFirst  = calculateAverage(scores.subList(0, mid));
        double avgSecond = calculateAverage(scores.subList(mid, scores.size()));

        if (avgFirst == 0) return 0.0;
        return ((avgSecond - avgFirst) / Math.abs(avgFirst)) * 100.0;
    }

    // ── 5. Full Dashboard Stats from DB ───────────────────────────────────

    /**
     * Fetches all scores from DB and computes full analysis.
     * Called by DashboardController to build the JSON response.
     */
    public DashboardStats getDashboardStats() {
        List<Double> scores = performanceDAO.getAllScores();

        double avg         = calculateAverage(scores);
        String trend       = detectTrend(scores);
        double improvement = calculatePeriodImprovement(scores);
        int    count       = performanceDAO.getTotalCount();

        PerformanceLevel levelChecker = new PerformanceLevel();
        String level = levelChecker.getLevel(avg);

        return new DashboardStats(avg, trend, improvement, level, count, scores);
    }

    // ── Inner class: holds all dashboard data ─────────────────────────────

    public static class DashboardStats {
        public final double       average;
        public final String       trend;
        public final double       improvement;
        public final String       level;
        public final int          totalSessions;
        public final List<Double> scores;

        public DashboardStats(double average, String trend, double improvement,
                               String level, int totalSessions, List<Double> scores) {
            this.average       = average;
            this.trend         = trend;
            this.improvement   = improvement;
            this.level         = level;
            this.totalSessions = totalSessions;
            this.scores        = scores;
        }
    }
}
