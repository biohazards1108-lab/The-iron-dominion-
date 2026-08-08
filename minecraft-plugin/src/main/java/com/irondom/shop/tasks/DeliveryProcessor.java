package com.irondom.shop.tasks;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.irondom.shop.IronDominionShop;

/**
 * Placeholder delivery processor.
 *
 * Store item delivery is intentionally disabled until the final Tebex catalog
 * and the actual Tekkit 1.6.4 item IDs are defined. Never mark an unsupported
 * delivery as completed: doing so would permanently lose a customer's reward.
 */
public class DeliveryProcessor implements Runnable {
    private final IronDominionShop plugin;
    private final Gson gson = new Gson();

    public DeliveryProcessor(IronDominionShop plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (!plugin.getConfig().getBoolean("store.enabled", false)) return;

        try {
            String response = plugin.getApiClient().getPendingDeliveries();
            JsonArray deliveries = gson.fromJson(response, JsonArray.class);
            if (deliveries == null || deliveries.size() == 0) return;

            plugin.getLogger().warning("Store delivery integration is not enabled for the legacy Tekkit catalog yet. "
                    + deliveries.size() + " delivery item(s) remain pending and were NOT marked delivered.");

            for (JsonElement element : deliveries) {
                if (element.isJsonObject()) {
                    JsonObject delivery = element.getAsJsonObject();
                    if (delivery.has("transaction_id")) {
                        plugin.getLogger().warning("Pending transaction: " + delivery.get("transaction_id").getAsString());
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Unable to inspect pending deliveries: " + e.getMessage());
        }
    }
}
