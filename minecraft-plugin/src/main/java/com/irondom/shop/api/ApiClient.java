package com.irondom.shop.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.irondom.shop.IronDominionShop;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Small authenticated HTTP client for the Iron Dominion website backend.
 *
 * Security properties:
 * - HTTPS is required by default.
 * - The API key is read from server-local configuration, never from the website.
 * - Player names and transaction IDs are URL encoded before entering paths.
 * - Requests have bounded connect/read timeouts.
 * - Only 2xx responses are treated as successful.
 */
public class ApiClient {
    private final String apiUrl;
    private final String apiKey;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;
    private final boolean requireHttps;
    private final Gson gson = new Gson();
    private final IronDominionShop plugin;

    public ApiClient(String apiUrl, IronDominionShop plugin) {
        this.apiUrl = normalizeBaseUrl(apiUrl);
        this.apiKey = plugin.getConfig().getString("api.api-key", "").trim();
        this.connectTimeoutMs = Math.max(1000, plugin.getConfig().getInt("api.connect-timeout-ms", 5000));
        this.readTimeoutMs = Math.max(1000, plugin.getConfig().getInt("api.read-timeout-ms", 5000));
        this.requireHttps = plugin.getConfig().getBoolean("api.require-https", true);
        this.plugin = plugin;

        if (this.requireHttps && !this.apiUrl.startsWith("https://")) {
            plugin.getLogger().severe("API HTTPS is required, but the configured API URL is not HTTPS. Backend calls are disabled.");
        }
        if (this.apiKey.isEmpty() || this.apiKey.startsWith("REPLACE_")) {
            plugin.getLogger().warning("No production API key is configured. Backend calls are disabled until one is supplied on the server.");
        }
    }

    private String normalizeBaseUrl(String value) {
        if (value == null) return "";
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) trimmed = trimmed.substring(0, trimmed.length() - 1);
        return trimmed;
    }

    private boolean ready() {
        return !apiUrl.isEmpty()
                && !apiKey.isEmpty()
                && !apiKey.startsWith("REPLACE_")
                && (!requireHttps || apiUrl.startsWith("https://"));
    }

    private String encode(String value) throws Exception {
        return URLEncoder.encode(value == null ? "" : value, "UTF-8");
    }

    private String request(String method, String path, String jsonBody) throws Exception {
        if (!ready()) return null;

        URL url = new URL(apiUrl + (path.startsWith("/") ? path : "/" + path));
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(connectTimeoutMs);
        connection.setReadTimeout(readTimeoutMs);
        connection.setUseCaches(false);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", "IronDominionBridge/1.0");
        connection.setRequestProperty("X-Iron-Dominion-Key", apiKey);

        if (jsonBody != null) {
            byte[] payload = jsonBody.getBytes(StandardCharsets.UTF_8);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setFixedLengthStreamingMode(payload.length);
            OutputStream output = connection.getOutputStream();
            output.write(payload);
            output.close();
        }

        int status = connection.getResponseCode();
        InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
        String response = "";
        if (stream != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) result.append(line);
            reader.close();
            response = result.toString();
        }
        connection.disconnect();

        if (status < 200 || status >= 300) {
            throw new IllegalStateException("HTTP " + status);
        }
        return response;
    }

    public int getPlayerBalance(String playerName) {
        try {
            String response = request("GET", "/api/player/" + encode(playerName) + "/balance", null);
            if (response == null) return -1;
            JsonObject json = gson.fromJson(response, JsonObject.class);
            return json.has("balance") ? json.get("balance").getAsInt() : -1;
        } catch (Exception e) {
            plugin.getLogger().warning("Unable to fetch balance: " + e.getMessage());
            return -1;
        }
    }

    public String getPendingDeliveries() {
        try {
            String response = request("GET", "/api/shop/pending-deliveries", null);
            return response == null ? "[]" : response;
        } catch (Exception e) {
            plugin.getLogger().warning("Unable to fetch pending deliveries: " + e.getMessage());
            return "[]";
        }
    }

    public boolean markDelivered(String transactionId) {
        try {
            request("POST", "/api/shop/deliver/" + encode(transactionId), "{}");
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Unable to mark delivery complete: " + e.getMessage());
            return false;
        }
    }

    public boolean updateServerStatus(int players, int maxPlayers, double tps, boolean online) {
        if (players < 0 || maxPlayers < 0 || players > maxPlayers || Double.isNaN(tps) || Double.isInfinite(tps)) {
            return false;
        }
        try {
            JsonObject body = new JsonObject();
            body.addProperty("players", players);
            body.addProperty("maxPlayers", maxPlayers);
            body.addProperty("tps", Math.max(0.0D, Math.min(100.0D, tps)));
            body.addProperty("online", online);
            request("POST", "/api/server/update", gson.toJson(body));
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Unable to update server status: " + e.getMessage());
            return false;
        }
    }

    public boolean addPlayerTokens(String playerName, int amount, String reason) {
        if (amount <= 0 || amount > 1000000) return false;
        try {
            JsonObject body = new JsonObject();
            body.addProperty("playerName", playerName);
            body.addProperty("amount", amount);
            body.addProperty("reason", reason == null ? "" : reason);
            request("POST", "/api/admin/add-tokens", gson.toJson(body));
            plugin.getLogger().info("Token grant accepted by backend for " + playerName);
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Unable to grant tokens: " + e.getMessage());
            return false;
        }
    }

    public boolean isConnected() {
        try {
            return request("GET", "/health", null) != null;
        } catch (Exception e) {
            return false;
        }
    }
}
