package com.tracker.service;

import java.util.List;

/**
 * Formats all data as JSON strings for HTTP API responses.
 * Used by every controller — keeps JSON building out of controllers.
 *
 * Author: Team Lead
 */
public class RecordFormatter {

    // ── POST /save response ───────────────────────────────────────────────

    public String formatSaveResponse(String athlete, double speed,
                                      double accuracy, double stamina,
                                      double score, String level) {
        return String.format(
            "{\"success\":true,\"athlete\":\"%s\"," +
            "\"speed\":%.2f,\"accuracy\":%.2f,\"stamina\":%.2f," +
            "\"score\":%.2f,\"level\":\"%s\"}",
            athlete, speed, accuracy, stamina, score, level
        );
    }

    // ── GET /dashboard response ───────────────────────────────────────────

    public String formatDashboard(double average, String trend,
                                   double improvement, String level,
                                   int totalSessions, List<Double> scores) {
        // Build scores array string
        StringBuilder arr = new StringBuilder("[");
        for (int i = 0; i < scores.size(); i++) {
            arr.append(String.format("%.2f", scores.get(i)));
            if (i < scores.size() - 1) arr.append(",");
        }
        arr.append("]");

        return String.format(
            "{\"summary\":{\"average\":%.2f,\"trend\":\"%s\"," +
            "\"improvement\":%.1f,\"level\":\"%s\",\"totalSessions\":%d}," +
            "\"scores\":%s}",
            average, trend, improvement, level, totalSessions, arr.toString()
        );
    }

    // ── Error / Success ───────────────────────────────────────────────────

    public String formatError(String message) {
        return String.format("{\"success\":false,\"error\":\"%s\"}", message);
    }

    public String formatSuccess(String message) {
        return String.format("{\"success\":true,\"message\":\"%s\"}", message);
    }
}
