package com.irondom.shop.tasks;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
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
 * Only the seven Iron Dominion supporter-rank commands are accepted.
 *
 * Tebex controls the polling cadence through meta.next_check. We honor that
 * value rather than repeatedly polling the queue at a fixed interval.
 */
public class DeliveryProcessor implements Runnable {
    private static final Pattern RANK_COMMAND = Pattern.compile(
            "^manuadd\\s+([^\\s]+)\\s+(Iron|Steel|Titanium|Diamond|Obsidian|Dominion|Overlord)$",
            Pattern.CASE_INSENSITIVE);
    private static final long MIN_RETRY_SECONDS = 30L;
    private static final long MAX_RETRY_SECONDS = 300L;

    private final IronDominionShop plugin;
    private final Gson gson = new Gson();

    public DeliveryProcessor(IronDominionShop plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (!plugin.getConfig().getBoolean("store.enabled", false)) return;
        if (!plugin.getApiClient().tebexReady()) return;

        long nextCheckSeconds = getFallbackPollSeconds();
        try {
            processOfflineCommands();

            JsonObject due = gson.fromJson(plugin.getApiClient().getTebexDuePlayers(), JsonObject.class);
            nextCheckSeconds = nextCheckSeconds(due);
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
            nextCheckSeconds = getFallbackPollSeconds();
        } finally {
            scheduleNext(nextCheckSeconds);
        }
    }

    private void scheduleNext(long seconds) {
        long boundedSeconds = Math.max(MIN_RETRY_SECONDS, Math.min(MAX_RETRY_SECONDS, seconds));
        long ticks = Math.max(20L, boundedSeconds * 20L);
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new DeliveryProcessor(plugin), ticks);
    }

    private long getFallbackPollSeconds() {
        return Math.max(MIN_RETRY_SECONDS, plugin.getConfig().getLong("delivery.poll-seconds", 90L));
    }

    private long nextCheckSeconds(JsonObject due) {
        if (due == null || !due.has("meta") || !due.get("meta").isJsonObject()) {
            return getFallbackPollSeconds();
        }
        JsonObject meta = due.getAsJsonObject("meta");
        if (!meta.has("next_check") || !meta.get("next_check").isJsonPrimitive()) {
            return getFallbackPollSeconds();
        }
        try {
            long value = meta.get("next_check").getAsLong();
            return value > 0 ? value : getFallbackPollSeconds();
        } catch (Exception ignored) {
            return getFallbackPollSeconds();
        }
    }

    private void processOfflineCommands() throws Exception {
        JsonObject payload = gson.fromJson(plugin.getApiClient().getTebexOfflineCommands(), JsonObject.class);
        JsonArray commands = payload != null && payload.has("commands") && payload.get("commands").isJsonArray()
                ? payload.getAsJsonArray("commands") : new JsonArray();
        processCommands(commands, null);
    }

    private void processOnlineCommands(String playerId, String playerName) throws Exception {
        JsonObject payload = gson.fromJson(plugin.getApiClient().getTebexOnlineCommands(playerId), JsonObject.class);
        JsonArray commands = payload != null && payload.has("commands") && payload.get("commands").isJsonArray()
                ? payload.getAsJsonArray("commands") : new JsonArray();
        processCommands(commands, playerName);
    }

    private void processCommands(JsonArray commands, String fallbackName) {
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
            if (playerName == null || playerName.isEmpty()) playerName = fallbackName;

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
                completedIds.add(new JsonPrimitive(id));
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
