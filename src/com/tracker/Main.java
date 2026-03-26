package com.tracker;

import com.sun.net.httpserver.HttpServer;
import com.tracker.controller.AuthController;
import com.tracker.controller.PerformanceController;
import java.net.InetSocketAddress;

public class Main {

    public static void main(String[] args) throws Exception {

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/login", new AuthController());
        server.createContext("/save", new PerformanceController());

        server.setExecutor(null);
        server.start();

        System.out.println("Server running on http://localhost:8080");
    }
}
