package com.tracker.Controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.tracker.service.AuthService;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class AuthController implements HttpHandler {

    private final AuthService authService = new AuthService();

    @Override
    public void handle(HttpExchange exchange) {

        // CORS headers so the browser frontend can reach this
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

        // Handle preflight OPTIONS request
        if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            sendResponse(exchange, 204, "");
            return;
        }

        // Only allow POST
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }

        try {
            // Read request body — expects: { "email": "...", "password": "..." }
            InputStream is   = exchange.getRequestBody();
            String      body = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            System.out.println("Login request received: " + body);

            // Extract email and password from JSON (no external library needed)
            String email    = extractJsonValue(body, "email");
            String password = extractJsonValue(body, "password");

            // Delegate validation to AuthService → UserDAO → MySQL
            boolean success = authService.login(email, password);

            if (success) {
                sendResponse(exchange, 200, "Login Successful");
            } else {
                sendResponse(exchange, 401, "Invalid email or password");
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500, "Internal Server Error");
        }
    }

    /**
     * Extracts a string value from a flat JSON object without any external library.
     * e.g. extractJsonValue("{\"email\":\"a@b.com\"}", "email") → "a@b.com"
     */
    private String extractJsonValue(String json, String key) {
        String search = "\"" + key + "\"";
        int keyIndex  = json.indexOf(search);
        if (keyIndex == -1) return null;

        int colon      = json.indexOf(":", keyIndex + search.length());
        int quoteOpen  = json.indexOf("\"", colon + 1);
        int quoteClose = json.indexOf("\"", quoteOpen + 1);

        if (quoteOpen == -1 || quoteClose == -1) return null;
        return json.substring(quoteOpen + 1, quoteClose);
    }

    private void sendResponse(HttpExchange exchange, int code, String message) {
        try {
            byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(code, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
