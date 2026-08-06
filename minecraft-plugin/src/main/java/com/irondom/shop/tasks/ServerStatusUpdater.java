package com.irondom.shop.tasks;

import com.irondom.shop.IronDominionShop;
import org.bukkit.Bukkit;

public class ServerStatusUpdater implements Runnable {
    private IronDominionShop plugin;

    public ServerStatusUpdater(IronDominionShop plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        try {
            // Get server stats
            int playerCount = Bukkit.getOnlinePlayers().size();
            int maxPlayers = Bukkit.getMaxPlayers();
            double tps = getTPS();
            boolean online = true;

            // Send to API
            plugin.getApiClient().updateServerStatus(playerCount, maxPlayers, tps, online);
        } catch (Exception e) {
            plugin.getLogger().severe("Error updating server status: " + e.getMessage());
        }
    }

    /**
     * Get server TPS (Ticks Per Second)
     * Paper only - returns 20.0 for other servers
     */
    private double getTPS() {
        try {
            // Try to get TPS from Paper API
            return Bukkit.getServer().getClass().getMethod("getAverageTPS").invoke(Bukkit.getServer()) != null ?
                    ((double[]) Bukkit.getServer().getClass().getMethod("getAverageTPS").invoke(Bukkit.getServer()))[0]
                    : 20.0;
        } catch (Exception e) {
            // Default to 20.0 if method doesn't exist
            return 20.0;
        }
    }
}
