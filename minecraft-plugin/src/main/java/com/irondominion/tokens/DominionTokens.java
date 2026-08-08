package com.irondominion.tokens;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class DominionTokens extends JavaPlugin {
    private final Map<String, Long> balances = new HashMap<String, Long>();
    private File dataFile;

    @Override public void onEnable() {
        dataFile = new File(getDataFolder(), "balances.properties");
        if (!getDataFolder().exists()) getDataFolder().mkdirs();
        loadBalances();
        getLogger().info("DominionTokens enabled.");
    }

    @Override public void onDisable() { saveBalances(); }

    // Bukkit 1.6.4 has no OfflinePlayer#getUniqueId(). Use the player name as
    // the legacy account key. This is deliberately kept compatible with the
    // exact API exposed by Cauldron/Bukkit v1_6_R3.
    private String keyFor(OfflinePlayer player) {
        String name = player.getName();
        return name == null ? "unknown" : name.toLowerCase();
    }

    private long getBalance(OfflinePlayer player) {
        Long value = balances.get(keyFor(player));
        return value == null ? 0L : value.longValue();
    }

    private boolean changeBalance(OfflinePlayer player, long delta) {
        String key = keyFor(player);
        long current = getBalance(player);
        if (delta > 0L && current > 1000000000L - delta) return false;
        if (delta < 0L && current < -delta) return false;
        long next = current + delta;
        if (next < 0L || next > 1000000000L) return false;
        balances.put(key, Long.valueOf(next));
        saveBalances();
        return true;
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("dt")) return false;
        if (args.length == 0 || args[0].equalsIgnoreCase("balance")) {
            OfflinePlayer target = sender instanceof Player ? (Player) sender : null;
            if (args.length >= 2 && sender.hasPermission("dominiontokens.admin")) target = Bukkit.getOfflinePlayer(args[1]);
            if (target == null) { sender.sendMessage("Usage: /dt balance <player>"); return true; }
            sender.sendMessage("§6Dominion Tokens: §e" + getBalance(target));
            return true;
        }
        if (!sender.hasPermission("dominiontokens.admin")) { sender.sendMessage("§cYou do not have permission."); return true; }
        if (args.length != 3) { sender.sendMessage("Usage: /dt <add|take|set> <player> <amount>"); return true; }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        long amount;
        try { amount = Long.parseLong(args[2]); } catch (NumberFormatException e) { sender.sendMessage("§cAmount must be a whole number."); return true; }
        if (amount < 0L || amount > 1000000000L) { sender.sendMessage("§cInvalid amount."); return true; }
        long current = getBalance(target);
        boolean ok;
        if (args[0].equalsIgnoreCase("add")) ok = changeBalance(target, amount);
        else if (args[0].equalsIgnoreCase("take")) ok = changeBalance(target, -amount);
        else if (args[0].equalsIgnoreCase("set")) { balances.put(keyFor(target), Long.valueOf(amount)); saveBalances(); ok = true; }
        else { sender.sendMessage("Usage: /dt <add|take|set> <player> <amount>"); return true; }
        if (!ok) { sender.sendMessage("§cToken operation failed or exceeded the balance limit."); return true; }
        sender.sendMessage("§aBalance updated: §e" + current + " §a→ §e" + getBalance(target));
        return true;
    }

    private void loadBalances() {
        if (!dataFile.exists()) return;
        java.util.Properties p = new java.util.Properties();
        try (java.io.FileInputStream in = new java.io.FileInputStream(dataFile)) {
            p.load(in);
            for (String k : p.stringPropertyNames()) try { balances.put(k, Long.valueOf(p.getProperty(k))); } catch (NumberFormatException ignored) { }
        } catch (IOException e) { getLogger().warning("Could not load balances: " + e.getMessage()); }
    }

    private void saveBalances() {
        if (dataFile == null) return;
        java.util.Properties p = new java.util.Properties();
        for (Map.Entry<String, Long> e : balances.entrySet()) p.setProperty(e.getKey(), String.valueOf(e.getValue()));
        try (java.io.FileOutputStream out = new java.io.FileOutputStream(dataFile)) { p.store(out, "Iron Dominion Dominion Token balances"); }
        catch (IOException e) { getLogger().warning("Could not save balances: " + e.getMessage()); }
    }
}
