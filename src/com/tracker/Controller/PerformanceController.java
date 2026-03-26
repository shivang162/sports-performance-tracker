package com.tracker.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.tracker.dao.PerformanceDAO;
import com.tracker.service.PerformanceLevel;
import com.tracker.service.PerformanceService;
import com.tracker.service.RecordFormatter;
import com.tracker.service.ValidationService;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * HTTP handler for POST /save
 *
 * Flow:
 *   1. Read JSON body  (distance, time, accuracy, stamina, athlete)
 *   2. Calculate speed = distance / time
 *   3. Validate with ValidationService
 *   4. Calculate score with PerformanceService
 *   5. Get level from PerformanceLevel
 *   6. Save to MySQL via PerformanceDAO
 *   7. Return JSON via RecordFormatter
 *
 * Author: Team Lead (integration)
 */
public class PerformanceController implements HttpHandler {

    private final PerformanceService performanceService = new PerformanceService();
    private final ValidationService  validationService  = new ValidationService();
    private final PerformanceLevel   performanceLevel   = new PerformanceLevel();
    private final PerformanceDAO     performanceDAO     = new PerformanceDAO();
    private final RecordFormatter    formatter          = new RecordFormatter();

    @Override
    public void handle(HttpExchange exchange) {

        // CORS
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin",  "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

        if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            sendResponse(exchange, 204, ""); return;
        }
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            sendResponse(exchange, 405, formatter.formatError("Method not allowed")); return;
        }

        try {
            // ── 1. Read body ───────────────────────────────────────────────
            InputStream is = exchange.getRequestBody();
            String body    = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            System.out.println("[PerformanceController] Received: " + body);

            // ── 2. Parse fields ────────────────────────────────────────────
            String athlete  = parseString(body, "athlete");
            double distance = parseDouble(body, "distance");
            double timeSec  = parseDouble(body, "time");
            double accuracy = parseDouble(body, "accuracy");
            double stamina  = parseDouble(body, "stamina");

            if (athlete == null || athlete.isBlank()) athlete = "Unknown";
            if (timeSec <= 0) {
                sendResponse(exchange, 400, formatter.formatError("Time must be greater than 0.")); return;
            }

            // ── 3. Calculate speed ─────────────────────────────────────────
            double speed = distance / timeSec;

            // ── 4. Validate ────────────────────────────────────────────────
            if (!validationService.validateSpeed(speed)) {
                sendResponse(exchange, 400, formatter.formatError("Speed out of valid range (0–200).")); return;
            }
            if (!validationService.validateAccuracy(accuracy)) {
                sendResponse(exchange, 400, formatter.formatError("Accuracy must be 0–100.")); return;
            }
            if (!validationService.validateStamina(stamina)) {
                sendResponse(exchange, 400, formatter.formatError("Stamina must be 0–100.")); return;
            }

            // ── 5. Calculate score + level ─────────────────────────────────
            double score = performanceService.calculateScore(speed, accuracy, stamina);
            String level = performanceLevel.getLevel(score);

            // ── 6. Save to DB ──────────────────────────────────────────────
            performanceDAO.insertRecord(athlete, distance, timeSec, speed, accuracy, stamina, score, level);

            // ── 7. Return JSON ─────────────────────────────────────────────
            String response = formatter.formatSaveResponse(athlete, speed, accuracy, stamina, score, level);
            System.out.println("[PerformanceController] Score=" + score + " Level=" + level);
            sendResponse(exchange, 200, response);

        } catch (NumberFormatException e) {
            sendResponse(exchange, 400, formatter.formatError("Invalid number in request."));
        } catch (Exception e) {
            System.err.println("[PerformanceController] Error: " + e.getMessage());
            sendResponse(exchange, 500, formatter.formatError("Server error: " + e.getMessage()));
        }
    }

    // ── JSON parsers ──────────────────────────────────────────────────────

    private double parseDouble(String json, String key) {
        String search = "\"" + key + "\"";
        int idx   = json.indexOf(search);
        if (idx == -1) throw new NumberFormatException("Missing: " + key);
        int colon = json.indexOf(":", idx);
        int end   = json.indexOf(",", colon);
        if (end == -1) end = json.indexOf("}", colon);
        return Double.parseDouble(json.substring(colon + 1, end).trim());
    }

    private String parseString(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx == -1) return null;
        int colon = json.indexOf(":", idx);
        int open  = json.indexOf("\"", colon + 1);
        int close = json.indexOf("\"", open + 1);
        if (open == -1 || close == -1) return null;
        return json.substring(open + 1, close);
    }

    // ── Send response ─────────────────────────────────────────────────────

    private void sendResponse(HttpExchange exchange, int code, String body) {
        try {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(code, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        } catch (Exception e) {
            System.err.println("[PerformanceController] sendResponse error: " + e.getMessage());
        }
    }
}
