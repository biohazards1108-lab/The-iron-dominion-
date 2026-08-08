package com.irondom.tokens;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

public final class DominionTokens extends JavaPlugin {
    private File dataDir;

    @Override public void onEnable() {
        dataDir = new File(getDataFolder(), "players");
        if (!dataDir.exists()) dataDir.mkdirs();
        getLogger().info("Dominion Tokens enabled. Balances are stored by UUID.");
    }

    private File file(UUID uuid) { return new File(dataDir, uuid.toString() + ".yml"); }
    private YamlConfiguration load(UUID uuid) { return YamlConfiguration.loadConfiguration(file(uuid)); }
    private long balance(UUID uuid) { return Math.max(0L, load(uuid).getLong("tokens", 0L)); }
    private void save(UUID uuid, long value) throws IOException {
        YamlConfiguration c = load(uuid);
        c.set("tokens", Math.max(0L, value));
        c.save(file(uuid));
    }
    private String name(OfflinePlayer p) { return p.getName() == null ? p.getUniqueId().toString() : p.getName(); }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("dt")) return false;
        if (args.length == 0 || args[0].equalsIgnoreCase("balance")) {
            OfflinePlayer p;
            if (args.length >= 2 && sender.hasPermission("dominiontokens.admin")) p = getServer().getOfflinePlayer(args[1]);
            else if (sender instanceof Player) p = (Player) sender; else { sender.sendMessage(ChatColor.RED + "Usage: /dt balance <player>"); return true; }
            sender.sendMessage(ChatColor.GOLD + "Dominion Tokens: " + ChatColor.WHITE + balance(p.getUniqueId()));
            return true;
        }
        if (!sender.hasPermission("dominiontokens.admin")) { sender.sendMessage(ChatColor.RED + "No permission."); return true; }
        if (args.length != 3 || !(args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("take") || args[0].equalsIgnoreCase("set"))) {
            sender.sendMessage(ChatColor.YELLOW + "/dt balance [player]");
            sender.sendMessage(ChatColor.YELLOW + "/dt add|take|set <player> <amount>");
            return true;
        }
        OfflinePlayer p = getServer().getOfflinePlayer(args[1]);
        long amount;
        try { amount = Long.parseLong(args[2]); } catch (NumberFormatException e) { sender.sendMessage(ChatColor.RED + "Amount must be a whole number."); return true; }
        if (amount < 0 || amount > 1000000000L) { sender.sendMessage(ChatColor.RED + "Amount must be between 0 and 1,000,000,000."); return true; }
        long old = balance(p.getUniqueId());
        long next;
        if (args[0].equalsIgnoreCase("add")) next = Math.min(1000000000L, old + amount);
        else if (args[0].equalsIgnoreCase("take")) next = Math.max(0L, old - amount);
        else next = amount;
        try { save(p.getUniqueId(), next); } catch (IOException e) { getLogger().severe("Could not save " + p.getUniqueId() + ": " + e.getMessage()); sender.sendMessage(ChatColor.RED + "Could not save token balance."); return true; }
        sender.sendMessage(ChatColor.GREEN + name(p) + " now has " + next + " Dominion Tokens.");
        return true;
    }
}
