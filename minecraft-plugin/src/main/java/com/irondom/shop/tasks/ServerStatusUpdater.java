package com.irondom.shop.tasks;

import com.irondom.shop.IronDominionShop;
import org.bukkit.Bukkit;

/** Captures server state synchronously, then performs network I/O asynchronously. */
public class ServerStatusUpdater implements Runnable {
    private final IronDominionShop plugin;

    public ServerStatusUpdater(IronDominionShop plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        final int playerCount = Bukkit.getOnlinePlayers().length;
        final int maxPlayers = Bukkit.getMaxPlayers();
        final double tps = getTPS();

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                plugin.getApiClient().updateServerStatus(playerCount, maxPlayers, tps, true);
            }
        });
    }

    private double getTPS() {
        // Cauldron/Bukkit 1.6.4 has no standardized TPS API. Use reflection so
        // compatible forks can expose a TPS method without a hard dependency.
        try {
            Object value = Bukkit.getServer().getClass().getMethod("getAverageTPS").invoke(Bukkit.getServer());
            if (value instanceof double[]) {
                double[] values = (double[]) value;
                if (values.length > 0 && !Double.isNaN(values[0]) && !Double.isInfinite(values[0])) {
                    return Math.max(0.0D, Math.min(20.0D, values[0]));
                }
            }
            if (value instanceof Number) {
                return Math.max(0.0D, Math.min(20.0D, ((Number) value).doubleValue()));
            }
        } catch (Exception ignored) {
            // No standardized TPS method on this server fork.
        }
        return 20.0D;
    }
}
