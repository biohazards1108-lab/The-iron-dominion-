package com.irondom.shop;

import com.irondom.shop.api.ApiClient;
import com.irondom.shop.commands.AdminCommand;
import com.irondom.shop.commands.ShopCommand;
import com.irondom.shop.tasks.DeliveryProcessor;
import com.irondom.shop.tasks.ServerStatusUpdater;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Iron Dominion's legacy-server bridge.
 *
 * This plugin is intentionally small: the browser never receives the API key,
 * and sensitive rewards are granted only by authenticated server-side calls.
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
        // Capture server state on the server thread; the updater moves network I/O off-thread.
        getServer().getScheduler().scheduleSyncRepeatingTask(this,
                new ServerStatusUpdater(this), 20L, statusPeriod);

        if (getConfig().getBoolean("store.enabled", false)) {
            long deliveryPeriod = Math.max(20L, getConfig().getLong("delivery.poll-seconds", 30L) * 20L);
            getServer().getScheduler().scheduleSyncRepeatingTask(this,
                    new DeliveryProcessor(this), 20L, deliveryPeriod);
            getLogger().warning("Store delivery polling is enabled, but item delivery remains intentionally disabled until the legacy catalog is verified.");
        }

        getLogger().info("Iron Dominion Bridge enabled. API endpoint: " + (apiUrl.isEmpty() ? "NOT CONFIGURED" : apiUrl));
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
