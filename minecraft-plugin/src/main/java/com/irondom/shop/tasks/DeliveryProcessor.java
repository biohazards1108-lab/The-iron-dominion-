package com.irondom.shop.tasks;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.irondom.shop.IronDominionShop;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Delivers Tebex Game Server API commands on the legacy server.
 *
 * Security: the bridge does not blindly execute arbitrary remote commands.
 * Only the seven Iron Dominion supporter-rank commands are accepted. This
 * protects the legacy server if a package is accidentally misconfigured.
 */
public class DeliveryProcessor implements Runnable {
    private static final Pattern RANK_COMMAND = Pattern.compile(
            "^manuadd\\s+([^\\s]+)\\s+(Iron|Steel|Titanium|Diamond|Obsidian|Dominion|Overlord)$",
            Pattern.CASE_INSENSITIVE);

    private final IronDominionShop plugin;
    private final Gson gson = new Gson();

    public DeliveryProcessor(IronDominionShop plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (!plugin.getConfig().getBoolean("store.enabled", false)) return;
        if (!plugin.getApiClient().tebexReady()) return;

        try {
            processOfflineCommands();

            JsonObject due = gson.fromJson(plugin.getApiClient().getTebexDuePlayers(), JsonObject.class);
            JsonArray players = due != null && due.has("players") && due.get("players").isJsonArray()
                    ? due.getAsJsonArray("players") : new JsonArray();

            for (JsonElement element : players) {
                if (!element.isJsonObject()) continue;
                JsonObject player = element.getAsJsonObject();
                String playerId = string(player, "id");
                String playerName = string(player, "name");
                if (playerId.isEmpty() || playerName.isEmpty()) continue;

                if (Bukkit.getPlayerExact(playerName) == null) continue;
                processOnlineCommands(playerId, playerName);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Tebex delivery check failed: " + e.getMessage());
        }
    }

    private void processOfflineCommands() throws Exception {
        JsonObject payload = gson.fromJson(plugin.getApiClient().getTebexOfflineCommands(), JsonObject.class);
        JsonArray commands = payload != null && payload.has("commands") && payload.get("commands").isJsonArray()
                ? payload.getAsJsonArray("commands") : new JsonArray();
        processCommands(commands, false, null);
    }

    private void processOnlineCommands(String playerId, String playerName) throws Exception {
        JsonObject payload = gson.fromJson(plugin.getApiClient().getTebexOnlineCommands(playerId), JsonObject.class);
        JsonArray commands = payload != null && payload.has("commands") && payload.get("commands").isJsonArray()
                ? payload.getAsJsonArray("commands") : new JsonArray();
        processCommands(commands, true, playerName);
    }

    private void processCommands(JsonArray commands, boolean onlineOnly, String fallbackName) {
        if (commands == null || commands.size() == 0) return;

        JsonArray completedIds = new JsonArray();
        Set<String> seen = new HashSet<String>();
        ConsoleCommandSender console = Bukkit.getConsoleSender();

        for (JsonElement element : commands) {
            if (!element.isJsonObject()) continue;
            JsonObject item = element.getAsJsonObject();
            String id = string(item, "id");
            String command = string(item, "command");
            JsonObject player = item.has("player") && item.get("player").isJsonObject()
                    ? item.getAsJsonObject("player") : null;
            String playerName = player == null ? fallbackName : string(player, "name");
            if (playerName.isEmpty()) playerName = fallbackName;

            if (id.isEmpty() || command.isEmpty() || playerName == null || playerName.isEmpty()) continue;
            if (!seen.add(id)) continue;

            String normalized = command.trim();
            if (normalized.startsWith("/")) normalized = normalized.substring(1).trim();
            normalized = normalized.replace("{name}", playerName).replace("{username}", playerName);

            Matcher matcher = RANK_COMMAND.matcher(normalized);
            if (!matcher.matches()) {
                plugin.getLogger().severe("Refusing unsupported Tebex command " + id + ": " + command);
                continue;
            }

            if (!matcher.group(1).equalsIgnoreCase(playerName)) {
                plugin.getLogger().severe("Refusing Tebex command " + id + ": player name mismatch.");
                continue;
            }

            try {
                boolean accepted = Bukkit.dispatchCommand(console, normalized);
                if (!accepted) {
                    plugin.getLogger().warning("GroupManager rejected Tebex rank command " + id + " for " + playerName);
                    continue;
                }
                completedIds.add(id);
                plugin.getLogger().info("Delivered Tebex rank " + matcher.group(2) + " to " + playerName + " (command " + id + ").");
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to execute Tebex command " + id + ": " + e.getMessage());
            }
        }

        if (completedIds.size() > 0 && plugin.getApiClient().deleteTebexCommands(completedIds)) {
            plugin.getLogger().info("Acknowledged " + completedIds.size() + " Tebex delivery command(s).");
        }
    }

    private String string(JsonObject object, String key) {
        return object != null && object.has(key) && !object.get(key).isJsonNull()
                ? object.get(key).getAsString() : "";
    }
}
