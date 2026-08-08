package com.irondom.tokens;

import java.io.File;
import java.io.IOException;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Dominion Tokens for legacy Bukkit/Cauldron 1.6.4.
 *
 * Minecraft 1.6.4's Bukkit API does not expose UUIDs on OfflinePlayer, so the
 * legacy token store uses a normalized player-name key. This keeps the plugin
 * compatible with the actual server API instead of compiling against a newer
 * Bukkit contract.
 */
public final class DominionTokens extends JavaPlugin {
    private File dataDir;

    @Override public void onEnable() {
        dataDir = new File(getDataFolder(), "players");
        if (!dataDir.exists() && !dataDir.mkdirs()) {
            getLogger().severe("Could not create token data directory: " + dataDir.getAbsolutePath());
        }
        getLogger().info("Dominion Tokens enabled. Legacy balances are stored by player name.");
    }

    private String key(OfflinePlayer player) {
        String playerName = player == null ? null : player.getName();
        if (playerName == null || playerName.trim().isEmpty()) return "unknown";
        return playerName.trim().toLowerCase(java.util.Locale.ENGLISH);
    }

    private File file(OfflinePlayer player) {
        return new File(dataDir, key(player) + ".yml");
    }

    private YamlConfiguration load(OfflinePlayer player) {
        return YamlConfiguration.loadConfiguration(file(player));
    }

    private long balance(OfflinePlayer player) {
        return Math.max(0L, load(player).getLong("tokens", 0L));
    }

    private void save(OfflinePlayer player, long value) throws IOException {
        YamlConfiguration config = load(player);
        config.set("tokens", Math.max(0L, value));
        config.set("player", player.getName());
        config.save(file(player));
    }

    private String displayName(OfflinePlayer player) {
        return player.getName() == null ? "Unknown" : player.getName();
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("dt")) return false;

        if (args.length == 0 || args[0].equalsIgnoreCase("balance")) {
            OfflinePlayer player;
            if (args.length >= 2 && sender.hasPermission("dominiontokens.admin")) {
                player = getServer().getOfflinePlayer(args[1]);
            } else if (sender instanceof Player) {
                player = (Player) sender;
            } else {
                sender.sendMessage(ChatColor.RED + "Usage: /dt balance <player>");
                return true;
            }

            sender.sendMessage(ChatColor.GOLD + "Dominion Tokens: " + ChatColor.WHITE + balance(player));
            return true;
        }

        if (!sender.hasPermission("dominiontokens.admin")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }

        if (args.length != 3 || !(args[0].equalsIgnoreCase("add")
                || args[0].equalsIgnoreCase("take")
                || args[0].equalsIgnoreCase("set"))) {
            sender.sendMessage(ChatColor.YELLOW + "/dt balance [player]");
            sender.sendMessage(ChatColor.YELLOW + "/dt add|take|set <player> <amount>");
            return true;
        }

        OfflinePlayer player = getServer().getOfflinePlayer(args[1]);
        long amount;
        try {
            amount = Long.parseLong(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Amount must be a whole number.");
            return true;
        }

        if (amount < 0 || amount > 1000000000L) {
            sender.sendMessage(ChatColor.RED + "Amount must be between 0 and 1,000,000,000.");
            return true;
        }

        long old = balance(player);
        long next;
        if (args[0].equalsIgnoreCase("add")) {
            next = Math.min(1000000000L, old + amount);
        } else if (args[0].equalsIgnoreCase("take")) {
            next = Math.max(0L, old - amount);
        } else {
            next = amount;
        }

        try {
            save(player, next);
        } catch (IOException e) {
            getLogger().severe("Could not save " + displayName(player) + ": " + e.getMessage());
            sender.sendMessage(ChatColor.RED + "Could not save token balance.");
            return true;
        }

        sender.sendMessage(ChatColor.GREEN + displayName(player) + " now has " + next + " Dominion Tokens.");
        return true;
    }
}
