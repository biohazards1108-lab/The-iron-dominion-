package com.irondom.shop;

import org.bukkit.plugin.java.JavaPlugin;
import com.irondom.shop.commands.ShopCommand;
import com.irondom.shop.commands.AdminCommand;
import com.irondom.shop.tasks.DeliveryProcessor;
import com.irondom.shop.tasks.ServerStatusUpdater;
import com.irondom.shop.api.ApiClient;

public class IronDominionShop extends JavaPlugin {
    private static IronDominionShop instance;
    private ApiClient apiClient;

    @Override
    public void onEnable() {
        instance = this;
        
        // Create config directory
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        // Load default config
        saveDefaultConfig();
        
        // Initialize API client
        String apiUrl = getConfig().getString("api.url", "http://localhost:3000");
        apiClient = new ApiClient(apiUrl, this);

        // Register commands
        getCommand("domshop").setExecutor(new ShopCommand(this));
        getCommand("domadmin").setExecutor(new AdminCommand(this));

        // Schedule delivery processor (every 30 seconds)
        getServer().getScheduler().scheduleSyncRepeatingTask(this,
                new DeliveryProcessor(this),
                0L,
                600L  // 30 seconds = 600 ticks
        );

        // Schedule server status updater (every 60 seconds)
        getServer().getScheduler().scheduleSyncRepeatingTask(this,
                new ServerStatusUpdater(this),
                0L,
                1200L  // 60 seconds = 1200 ticks
        );

        getLogger().info("\n");
        getLogger().info("╔════════════════════════════════════════╗");
        getLogger().info("║   🏰 Iron Dominion Shop Plugin 🏰    ║");
        getLogger().info("║   Connected to: " + apiUrl);
        getLogger().info("║   Status: ENABLED ✓");
        getLogger().info("╚════════════════════════════════════════╝");
        getLogger().info("");
    }

    @Override
    public void onDisable() {
        getLogger().info("🏰 Iron Dominion Shop Plugin disabled.");
    }

    public static IronDominionShop getInstance() {
        return instance;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }
}
