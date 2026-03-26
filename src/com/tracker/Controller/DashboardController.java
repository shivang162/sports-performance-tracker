package com.tracker.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.tracker.service.PerformanceService;
import com.tracker.service.PerformanceService.DashboardStats;
import com.tracker.service.RecordFormatter;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * HTTP handler for GET /dashboard
 *
 * Flow:
 *   1. Call PerformanceService.getDashboardStats() → pulls scores from MySQL
 *   2. Return JSON with average, trend, improvement, level, totalSessions, scores[]
 *   3. Frontend dashboard.html reads this JSON on page load
 *
 * Author: Team Lead
 */
public class DashboardController implements HttpHandler {

    private final PerformanceService performanceService = new PerformanceService();
    private final RecordFormatter    formatter          = new RecordFormatter();

    @Override
    public void handle(HttpExchange exchange) {

        exchange.getResponseHeaders().add("Access-Control-Allow-Origin",  "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

        if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            sendResponse(exchange, 204, ""); return;
        }
        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            sendResponse(exchange, 405, formatter.formatError("Method not allowed")); return;
        }

        try {
            // Pull all stats from DB via PerformanceService
            DashboardStats stats = performanceService.getDashboardStats();

            String response = formatter.formatDashboard(
                stats.average,
                stats.trend,
                stats.improvement,
                stats.level,
                stats.totalSessions,
                stats.scores
            );

            System.out.println("[DashboardController] avg=" +
                String.format("%.2f", stats.average) + " trend=" + stats.trend);

            sendResponse(exchange, 200, response);

        } catch (Exception e) {
            System.err.println("[DashboardController] Error: " + e.getMessage());
            sendResponse(exchange, 500, formatter.formatError("Could not load dashboard."));
        }
    }

    private void sendResponse(HttpExchange exchange, int code, String body) {
        try {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(code, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        } catch (Exception e) {
            System.err.println("[DashboardController] sendResponse error: " + e.getMessage());
        }
    }
}
