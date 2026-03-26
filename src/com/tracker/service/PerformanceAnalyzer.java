package com.tracker.service;

import java.util.List;

/**
 * Benchmark comparison module.
 * Compares athlete's average score against standard levels
 * and calculates gap to next level.
 *
 * Thresholds match PerformanceLevel.java:
 *   Excellent >= 85 | Good >= 70 | Average >= 50 | Needs Improvement < 50
 *
 * Author: Team Lead
 */
public class PerformanceAnalyzer {

    private static final double EXCELLENT_MIN = 85.0;
    private static final double GOOD_MIN      = 70.0;
    private static final double AVERAGE_MIN   = 50.0;

    private final PerformanceService performanceService = new PerformanceService();
    private final PerformanceLevel   performanceLevel   = new PerformanceLevel();

    // ── 1. Get level from scores ──────────────────────────────────────────

    public String analyzeLevel(List<Double> scores) {
        double avg = performanceService.calculateAverage(scores);
        return performanceLevel.getLevel(avg);
    }

    // ── 2. Gap to next level ──────────────────────────────────────────────

    public String gapToNextLevel(List<Double> scores) {
        double avg = performanceService.calculateAverage(scores);

        if (avg >= EXCELLENT_MIN)
            return "You are at the top level: Excellent!";
        else if (avg >= GOOD_MIN)
            return String.format("You need %.1f more points to reach Excellent.", EXCELLENT_MIN - avg);
        else if (avg >= AVERAGE_MIN)
            return String.format("You need %.1f more points to reach Good.", GOOD_MIN - avg);
        else
            return String.format("You need %.1f more points to reach Average.", AVERAGE_MIN - avg);
    }

    // ── 3. Full benchmark report as JSON string ───────────────────────────

    public String getBenchmarkJson(List<Double> scores, String athleteName) {
        double avg   = performanceService.calculateAverage(scores);
        String level = analyzeLevel(scores);
        String gap   = gapToNextLevel(scores).replace("\"", "'");

        return String.format(
            "{\"athlete\":\"%s\",\"average\":%.2f,\"level\":\"%s\",\"nextTarget\":\"%s\"}",
            athleteName, avg, level, gap
        );
    }

    // ── 4. Compare two athletes ───────────────────────────────────────────

    public String compareAthletesJson(List<Double> scoresA, String nameA,
                                       List<Double> scoresB, String nameB) {
        double avgA   = performanceService.calculateAverage(scoresA);
        double avgB   = performanceService.calculateAverage(scoresB);
        String levelA = performanceLevel.getLevel(avgA);
        String levelB = performanceLevel.getLevel(avgB);

        String winner;
        if      (avgA > avgB) winner = nameA + " is performing better.";
        else if (avgB > avgA) winner = nameB + " is performing better.";
        else                  winner = "Both athletes are equal.";

        return String.format(
            "{\"athleteA\":{\"name\":\"%s\",\"average\":%.2f,\"level\":\"%s\"}," +
             "\"athleteB\":{\"name\":\"%s\",\"average\":%.2f,\"level\":\"%s\"}," +
             "\"result\":\"%s\"}",
            nameA, avgA, levelA, nameB, avgB, levelB, winner
        );
    }
}
