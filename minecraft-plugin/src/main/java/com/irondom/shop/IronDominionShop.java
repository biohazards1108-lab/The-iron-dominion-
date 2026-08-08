package com.irondom.shop;

import com.irondom.shop.api.ApiClient;
import com.irondom.shop.commands.AdminCommand;
import com.irondom.shop.commands.ShopCommand;
import com.irondom.shop.tasks.DeliveryProcessor;
import com.irondom.shop.tasks.ServerStatusUpdater;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Iron Dominion's legacy-server bridge for Tekkit 1.6.4/Cauldron.
 *
 * Tebex delivery is performed through Tebex's Game Server Plugin API rather
 * than the incompatible modern Tebex plugin. The browser never receives the
 * Tebex server secret.
 */
public class IronDominionShop extends JavaPlugin {
    private static IronDominionShop instance;
    private ApiClient apiClient;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        String apiUrl = getConfig().getString("api.url", "").trim();
        apiClient = new ApiClient(apiUrl, this);

        if (getCommand("domshop") != null) {
            getCommand("domshop").setExecutor(new ShopCommand(this));
        }
        if (getCommand("domadmin") != null) {
            getCommand("domadmin").setExecutor(new AdminCommand(this));
        }

        long statusPeriod = Math.max(20L, getConfig().getLong("status.update-seconds", 60L) * 20L);
        getServer().getScheduler().scheduleSyncRepeatingTask(this,
                new ServerStatusUpdater(this), 20L, statusPeriod);

        if (getConfig().getBoolean("store.enabled", false)) {
            long initialDelay = Math.max(20L, getConfig().getLong("delivery.poll-seconds", 90L) * 20L);
            getServer().getScheduler().scheduleSyncDelayedTask(this,
                    new DeliveryProcessor(this), initialDelay);
            getLogger().info("Tebex rank delivery polling is enabled. The bridge honors Tebex's server-provided queue interval and only executes the seven configured Iron Dominion rank commands.");
        } else {
            getLogger().info("Tebex rank delivery is disabled. Set store.enabled=true after configuring the Tebex Game Server secret locally.");
        }

        getLogger().info("Iron Dominion Bridge enabled. Website API: "
                + (apiUrl.isEmpty() ? "NOT CONFIGURED" : apiUrl)
                + "; Tebex API: " + (apiClient.tebexReady() ? "CONFIGURED" : "NOT CONFIGURED"));
    }

    @Override
    public void onDisable() {
        getLogger().info("Iron Dominion Bridge disabled.");
    }

    public static IronDominionShop getInstance() {
        return instance;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }
}
