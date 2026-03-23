package com.tracker.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.OutputStream;

public class AuthController implements HttpHandler {

    public void handle(HttpExchange exchange) {

        try {

            String response = "Login Successful";

            exchange.sendResponseHeaders(200, response.length());

            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
