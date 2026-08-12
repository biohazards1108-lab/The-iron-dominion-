package com.irondom.shop.ranks;

import com.irondom.shop.IronDominionShop;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Safe server-side rank operations for the legacy GroupManager environment. */
public final class RankManager {
    private static final Set<String> RANKS = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
            "IRON", "STEEL", "TITANIUM", "DIAMOND", "OBSIDIAN", "DOMINION", "OVERLORD"
    )));

    private final IronDominionShop plugin;
    private final File file;
    private final YamlConfiguration data;

    public RankManager(IronDominionShop plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "ranks.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    public boolean isValidRank(String rank) {
        return rank != null && RANKS.contains(rank.toUpperCase(Locale.ENGLISH));
    }

    public Set<String> getRanks() {
        return RANKS;
    }

    public String getStoredRank(String playerName) {
        if (playerName == null || playerName.trim().isEmpty()) return "";
        return data.getString("players." + playerName.trim().toLowerCase(Locale.ENGLISH), "");
    }

    public boolean setRank(String playerName, String rank) {
        if (playerName == null || playerName.trim().isEmpty() || !isValidRank(rank)) return false;
        String normalizedPlayer = playerName.trim();
        String normalizedRank = rank.toUpperCase(Locale.ENGLISH);
        ConsoleCommandSender console = Bukkit.getConsoleSender();
        if (!Bukkit.dispatchCommand(console, "manuadd " + normalizedPlayer + " " + normalizedRank)) return false;
        data.set("players." + normalizedPlayer.toLowerCase(Locale.ENGLISH), normalizedRank);
        return save();
    }

    public boolean removeRank(String playerName) {
        if (playerName == null || playerName.trim().isEmpty()) return false;
        String normalizedPlayer = playerName.trim();
        ConsoleCommandSender console = Bukkit.getConsoleSender();
        if (!Bukkit.dispatchCommand(console, "manudel " + normalizedPlayer)) return false;
        data.set("players." + normalizedPlayer.toLowerCase(Locale.ENGLISH), null);
        return save();
    }

    private boolean save() {
        try {
            data.save(file);
            return true;
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save rank data: " + e.getMessage());
            return false;
        }
    }
}
