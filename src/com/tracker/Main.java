package com.tracker;

import com.sun.net.httpserver.HttpServer;
import com.tracker.controller.AuthController;
import com.tracker.controller.DashboardController;
import com.tracker.controller.PerformanceController;
import com.tracker.dao.DBConnection;

import java.net.InetSocketAddress;

/**
 * Application entry point.
 * Starts HTTP server, tests DB, registers all 3 routes.
 *
 * Routes:
 *   POST /login      → AuthController       (email + password → MySQL users table)
 *   POST /save       → PerformanceController (validate → score → MySQL → JSON)
 *   GET  /dashboard  → DashboardController   (MySQL scores → analysis → JSON)
 *
 * How to run:
 *   1. Start MySQL
 *   2. Run database/schema.sql
 *   3. Add mysql-connector-java JAR to lib/ folder
 *   4. Run this file
 *   5. Open frontend/login.html in browser
 *
 * Author: Team Lead
 */
public class Main {

    public static void main(String[] args) throws Exception {

        // Test DB on startup
        System.out.println("Connecting to database...");
        try {
            DBConnection.getConnection();
            System.out.println("[Main] Database connected.");
        } catch (Exception e) {
            System.err.println("[Main] WARNING: DB not available — " + e.getMessage());
            System.err.println("[Main] Server starting anyway. DB ops will fail until fixed.");
        }

        // Start HTTP server
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/login",     new AuthController());
        server.createContext("/save",      new PerformanceController());
        server.createContext("/dashboard", new DashboardController());

        server.setExecutor(null);
        server.start();

        System.out.println("\nServer running on http://localhost:8080");
        System.out.println("  POST /login      — authenticate user");
        System.out.println("  POST /save       — save performance record");
        System.out.println("  GET  /dashboard  — get analysis summary");
        System.out.println("\nOpen src/com/tracker/frontend/login.html in browser.");
    }
}
