package com.nexus.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/**
 * Embedded lightweight HTTP Authentication server running inside NEXUS.
 * Provides live /api/auth/signup, /api/auth/login and protected /api/auth/profile endpoints
 * to test Bearer Token authentication directly in API Studio and unit tests.
 */
public class LocalAuthServer {

    private static final LocalAuthServer INSTANCE = new LocalAuthServer();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private HttpServer server;
    private int port = 8089;
    private boolean started = false;

    // In-memory user database: username -> password
    private final Map<String, String> userCredentials = new ConcurrentHashMap<>();
    // In-memory active tokens: token -> username
    private final Map<String, String> activeTokens = new ConcurrentHashMap<>();

    private LocalAuthServer() {
        // Pre-seed default test user
        userCredentials.put("admin", "admin123");
        userCredentials.put("nexus_dev", "nexus2026");
    }

    public static LocalAuthServer getInstance() {
        return INSTANCE;
    }

    public synchronized void start() {
        if (started && server != null) return;

        int[] candidatePorts = {8089, 8090, 8091, 8092, 0};
        for (int p : candidatePorts) {
            try {
                server = HttpServer.create(new InetSocketAddress("127.0.0.1", p), 0);
                this.port = server.getAddress().getPort();
                break;
            } catch (IOException ignored) {}
        }

        if (server == null) {
            System.err.println("[NEXUS] LocalAuthServer: Failed to bind to any port");
            return;
        }

        server.createContext("/api/auth/signup", new SignupHandler());
        server.createContext("/api/auth/login", new LoginHandler());
        server.createContext("/api/auth/profile", new ProfileHandler());
        server.createContext("/api/auth/health", exchange -> {
            sendJsonResponse(exchange, 200, "{\"status\":\"healthy\",\"server\":\"NEXUS-Auth-Server\"}");
        });

        server.setExecutor(Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "Nexus-AuthServer-Worker");
            t.setDaemon(true);
            return t;
        }));

        server.start();
        started = true;
        System.out.println("[NEXUS] LocalAuthServer started on " + getBaseUrl());

        // Register default environment variable
        EnvironmentService.getInstance().setVariable("authUrl", getBaseUrl());
    }

    public synchronized void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
            started = false;
        }
    }

    public boolean isStarted() {
        return started;
    }

    public int getPort() {
        return port;
    }

    public String getBaseUrl() {
        return "http://127.0.0.1:" + port;
    }

    public void registerUser(String username, String password) {
        userCredentials.put(username, password);
    }

    public String createTokenForUser(String username) {
        String token = "nexus_bearer_" + UUID.randomUUID().toString().replace("-", "");
        activeTokens.put(token, username);
        return token;
    }

    public boolean validateToken(String token) {
        return token != null && activeTokens.containsKey(token);
    }

    public String getUsernameForToken(String token) {
        return activeTokens.get(token);
    }

    // Handlers
    private class SignupHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }

            try (InputStream is = exchange.getRequestBody()) {
                String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                JsonNode json = MAPPER.readTree(body);

                String username = json.has("username") ? json.get("username").asText().trim() : "";
                String password = json.has("password") ? json.get("password").asText() : "";
                String email = json.has("email") ? json.get("email").asText() : username + "@nexus.io";

                if (username.isEmpty() || password.isEmpty()) {
                    sendJsonResponse(exchange, 400, "{\"status\":\"error\",\"message\":\"Username and password are required\"}");
                    return;
                }

                if (userCredentials.containsKey(username)) {
                    sendJsonResponse(exchange, 409, "{\"status\":\"error\",\"message\":\"User '" + username + "' already exists\"}");
                    return;
                }

                userCredentials.put(username, password);
                String token = createTokenForUser(username);

                String resp = String.format(
                        "{\"status\":\"created\",\"message\":\"User registered successfully\",\"username\":\"%s\",\"email\":\"%s\",\"token\":\"%s\"}",
                        username, email, token
                );
                sendJsonResponse(exchange, 201, resp);
            } catch (Exception e) {
                sendJsonResponse(exchange, 400, "{\"status\":\"error\",\"message\":\"Invalid JSON payload: " + e.getMessage() + "\"}");
            }
        }
    }

    private class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }

            try (InputStream is = exchange.getRequestBody()) {
                String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                JsonNode json = MAPPER.readTree(body);

                String username = json.has("username") ? json.get("username").asText().trim() : "";
                String password = json.has("password") ? json.get("password").asText() : "";

                if (!userCredentials.containsKey(username) || !userCredentials.get(username).equals(password)) {
                    sendJsonResponse(exchange, 401, "{\"status\":\"unauthorized\",\"message\":\"Invalid credentials. Check username or password.\"}");
                    return;
                }

                String token = createTokenForUser(username);
                String resp = String.format(
                        "{\"status\":\"success\",\"message\":\"Authentication successful\",\"token\":\"%s\",\"user\":{\"username\":\"%s\",\"role\":\"developer\"}}",
                        token, username
                );
                sendJsonResponse(exchange, 200, resp);
            } catch (Exception e) {
                sendJsonResponse(exchange, 400, "{\"status\":\"error\",\"message\":\"Invalid JSON payload: " + e.getMessage() + "\"}");
            }
        }
    }

    private class ProfileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }

            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                sendJsonResponse(exchange, 401, "{\"status\":\"unauthorized\",\"error\":\"Missing or invalid Bearer token. Include 'Authorization: Bearer <token>' header.\"}");
                return;
            }

            String token = authHeader.substring(7).trim();
            if (!validateToken(token)) {
                sendJsonResponse(exchange, 401, "{\"status\":\"unauthorized\",\"error\":\"Bearer token expired or invalid: " + token + "\"}");
                return;
            }

            String user = getUsernameForToken(token);
            String resp = String.format(
                    "{\"status\":\"authenticated\",\"message\":\"Access granted to protected endpoint\",\"user\":\"%s\",\"role\":\"developer\",\"session\":\"active\",\"auth\":\"Bearer verified\"}",
                    user
            );
            sendJsonResponse(exchange, 200, resp);
        }
    }

    private static void sendJsonResponse(HttpExchange exchange, int statusCode, String responseBody) throws IOException {
        byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}

