package com.irondom.shop.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
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
 * Authenticated HTTP client for the Iron Dominion website backend and Tebex
 * Game Server Plugin API. Secrets are read only from server-local config.
 */
public class ApiClient {
    private static final String TEBEX_QUEUE_URL = "https://plugin.tebex.io/queue";

    private final String apiUrl;
    private final String apiKey;
    private final String tebexSecret;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;
    private final boolean requireHttps;
    private final IronDominionShop plugin;
    private final Gson gson = new Gson();

    public ApiClient(String apiUrl, IronDominionShop plugin) {
        this.apiUrl = normalizeBaseUrl(apiUrl);
        this.apiKey = plugin.getConfig().getString("api.api-key", "").trim();
        this.tebexSecret = plugin.getConfig().getString("tebex.secret", "").trim();
        this.connectTimeoutMs = Math.max(1000, plugin.getConfig().getInt("api.connect-timeout-ms", 5000));
        this.readTimeoutMs = Math.max(1000, plugin.getConfig().getInt("api.read-timeout-ms", 5000));
        this.requireHttps = plugin.getConfig().getBoolean("api.require-https", true);
        this.plugin = plugin;

        if (this.requireHttps && !this.apiUrl.isEmpty() && !this.apiUrl.startsWith("https://")) {
            plugin.getLogger().severe("API HTTPS is required, but the configured API URL is not HTTPS.");
        }
        if (this.apiKey.isEmpty() || this.apiKey.startsWith("REPLACE_")) {
            plugin.getLogger().info("Website API key is not configured; website-only bridge features remain disabled.");
        }
        if (this.tebexSecret.isEmpty() || this.tebexSecret.startsWith("REPLACE_")) {
            plugin.getLogger().warning("Tebex Game Server secret is not configured. Store delivery is disabled until it is supplied in config.yml.");
        }
    }

    private String normalizeBaseUrl(String value) {
        if (value == null) return "";
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) trimmed = trimmed.substring(0, trimmed.length() - 1);
        return trimmed;
    }

    private boolean websiteReady() {
        return !apiUrl.isEmpty()
                && !apiKey.isEmpty()
                && !apiKey.startsWith("REPLACE_")
                && (!requireHttps || apiUrl.startsWith("https://"));
    }

    public boolean tebexReady() {
        return !tebexSecret.isEmpty() && !tebexSecret.startsWith("REPLACE_");
    }

    private String encode(String value) throws Exception {
        return URLEncoder.encode(value == null ? "" : value, "UTF-8");
    }

    private String request(String method, String urlString, String jsonBody, String authHeaderName, String authHeaderValue) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(connectTimeoutMs);
        connection.setReadTimeout(readTimeoutMs);
        connection.setUseCaches(false);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", "IronDominionBridge/1.1");
        if (authHeaderName != null && authHeaderValue != null && !authHeaderValue.isEmpty()) {
            connection.setRequestProperty(authHeaderName, authHeaderValue);
        }

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
            throw new IllegalStateException("HTTP " + status + ": " + response);
        }
        return response;
    }

    private String websiteRequest(String method, String path, String jsonBody) throws Exception {
        if (!websiteReady()) return null;
        return request(method, apiUrl + (path.startsWith("/") ? path : "/" + path), jsonBody,
                "X-Iron-Dominion-Key", apiKey);
    }

    private String tebexRequest(String method, String path, String jsonBody) throws Exception {
        if (!tebexReady()) return null;
        return request(method, TEBEX_QUEUE_URL + (path.startsWith("/") ? path : "/" + path), jsonBody,
                "X-Tebex-Secret", tebexSecret);
    }

    public int getPlayerBalance(String playerName) {
        try {
            String response = websiteRequest("GET", "/api/player/" + encode(playerName) + "/balance", null);
            if (response == null) return -1;
            JsonObject json = gson.fromJson(response, JsonObject.class);
            return json.has("balance") ? json.get("balance").getAsInt() : -1;
        } catch (Exception e) {
            plugin.getLogger().warning("Unable to fetch balance: " + e.getMessage());
            return -1;
        }
    }

    public String getTebexDuePlayers() {
        try {
            String response = tebexRequest("GET", "", null);
            return response == null ? "{\"players\":[],\"meta\":{\"next_check\":90}}" : response;
        } catch (Exception e) {
            plugin.getLogger().warning("Unable to fetch Tebex command queue: " + e.getMessage());
            return "{\"players\":[],\"meta\":{\"next_check\":90}}";
        }
    }

    public String getTebexOfflineCommands() throws Exception {
        String response = tebexRequest("GET", "/offline-commands", null);
        return response == null ? "{\"commands\":[]}" : response;
    }

    public String getTebexOnlineCommands(String playerId) throws Exception {
        String response = tebexRequest("GET", "/online-commands/" + encode(playerId), null);
        return response == null ? "{\"commands\":[]}" : response;
    }

    public boolean deleteTebexCommands(JsonArray ids) {
        if (!tebexReady() || ids == null || ids.size() == 0) return false;
        try {
            JsonObject body = new JsonObject();
            body.add("ids", ids);
            tebexRequest("DELETE", "", gson.toJson(body));
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Unable to acknowledge Tebex commands: " + e.getMessage());
            return false;
        }
    }

    public String getPendingDeliveries() {
        try {
            String response = websiteRequest("GET", "/api/shop/pending-deliveries", null);
            return response == null ? "[]" : response;
        } catch (Exception e) {
            plugin.getLogger().warning("Unable to fetch website pending deliveries: " + e.getMessage());
            return "[]";
        }
    }

    public boolean markDelivered(String transactionId) {
        try {
            websiteRequest("POST", "/api/shop/deliver/" + encode(transactionId), "{}");
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Unable to mark delivery complete: " + e.getMessage());
            return false;
        }
    }

    public boolean updateServerStatus(int players, int maxPlayers, double tps, boolean online) {
        if (!websiteReady() || players < 0 || maxPlayers < 0 || players > maxPlayers || Double.isNaN(tps) || Double.isInfinite(tps)) {
            return false;
        }
        try {
            JsonObject body = new JsonObject();
            body.addProperty("players", players);
            body.addProperty("maxPlayers", maxPlayers);
            body.addProperty("tps", Math.max(0.0D, Math.min(100.0D, tps)));
            body.addProperty("online", online);
            websiteRequest("POST", "/api/server/update", gson.toJson(body));
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Unable to update server status: " + e.getMessage());
            return false;
        }
    }

    public boolean addPlayerTokens(String playerName, int amount, String reason) {
        if (!websiteReady() || amount <= 0 || amount > 1000000) return false;
        try {
            JsonObject body = new JsonObject();
            body.addProperty("playerName", playerName);
            body.addProperty("amount", amount);
            body.addProperty("reason", reason == null ? "" : reason);
            websiteRequest("POST", "/api/admin/add-tokens", gson.toJson(body));
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Unable to grant tokens: " + e.getMessage());
            return false;
        }
    }

    public boolean isConnected() {
        try {
            return websiteRequest("GET", "/health", null) != null;
        } catch (Exception e) {
            return false;
        }
    }
}
