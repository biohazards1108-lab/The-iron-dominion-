package com.irondom.shop.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.irondom.shop.IronDominionShop;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class ApiClient {
    private String apiUrl;
    private HttpClient httpClient;
    private Gson gson;
    private IronDominionShop plugin;

    public ApiClient(String apiUrl, IronDominionShop plugin) {
        this.apiUrl = apiUrl;
        this.plugin = plugin;
        this.httpClient = HttpClients.createDefault();
        this.gson = new Gson();
    }

    /**
     * Get player balance from API
     */
    public int getPlayerBalance(String playerName) {
        try {
            String url = apiUrl + "/api/player/" + playerName + "/balance";
            HttpGet request = new HttpGet(url);
            
            String response = httpClient.execute(request, response1 -> {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response1.getEntity().getContent()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                reader.close();
                return result.toString();
            });

            JsonObject json = gson.fromJson(response, JsonObject.class);
            return json.get("balance").getAsInt();
        } catch (Exception e) {
            plugin.getLogger().severe("Error fetching balance for " + playerName + ": " + e.getMessage());
            return -1;
        }
    }

    /**
     * Get pending deliveries from API
     */
    public String getPendingDeliveries() {
        try {
            String url = apiUrl + "/api/shop/pending-deliveries";
            HttpGet request = new HttpGet(url);
            
            String response = httpClient.execute(request, response1 -> {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response1.getEntity().getContent()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                reader.close();
                return result.toString();
            });

            return response;
        } catch (Exception e) {
            plugin.getLogger().severe("Error fetching pending deliveries: " + e.getMessage());
            return "[]";
        }
    }

    /**
     * Mark a purchase as delivered
     */
    public boolean markDelivered(String transactionId) {
        try {
            String url = apiUrl + "/api/shop/deliver/" + transactionId;
            HttpPost request = new HttpPost(url);
            
            boolean success = httpClient.execute(request, response -> {
                return response.getCode() >= 200 && response.getCode() < 300;
            });

            if (success) {
                plugin.getLogger().info("✓ Marked " + transactionId + " as delivered");
            }
            return success;
        } catch (Exception e) {
            plugin.getLogger().severe("Error marking delivery: " + e.getMessage());
            return false;
        }
    }

    /**
     * Update server status
     */
    public boolean updateServerStatus(int players, int maxPlayers, double tps, boolean online) {
        try {
            String url = apiUrl + "/api/server/update";
            HttpPost request = new HttpPost(url);
            
            JsonObject body = new JsonObject();
            body.addProperty("players", players);
            body.addProperty("maxPlayers", maxPlayers);
            body.addProperty("tps", tps);
            body.addProperty("online", online);
            
            request.setEntity(new StringEntity(gson.toJson(body), ContentType.APPLICATION_JSON));
            
            boolean success = httpClient.execute(request, response -> {
                return response.getCode() >= 200 && response.getCode() < 300;
            });

            return success;
        } catch (Exception e) {
            plugin.getLogger().severe("Error updating server status: " + e.getMessage());
            return false;
        }
    }

    /**
     * Add tokens to player (admin)
     */
    public boolean addPlayerTokens(String playerName, int amount, String reason) {
        try {
            String url = apiUrl + "/api/admin/add-tokens";
            HttpPost request = new HttpPost(url);
            
            JsonObject body = new JsonObject();
            body.addProperty("playerName", playerName);
            body.addProperty("amount", amount);
            body.addProperty("reason", reason);
            
            request.setEntity(new StringEntity(gson.toJson(body), ContentType.APPLICATION_JSON));
            
            boolean success = httpClient.execute(request, response -> {
                return response.getCode() >= 200 && response.getCode() < 300;
            });

            if (success) {
                plugin.getLogger().info("✓ Added " + amount + " tokens to " + playerName);
            }
            return success;
        } catch (Exception e) {
            plugin.getLogger().severe("Error adding tokens: " + e.getMessage());
            return false;
        }
    }

    public boolean isConnected() {
        try {
            String url = apiUrl + "/health";
            HttpGet request = new HttpGet(url);
            return httpClient.execute(request, response -> response.getCode() == 200);
        } catch (Exception e) {
            return false;
        }
    }
}
