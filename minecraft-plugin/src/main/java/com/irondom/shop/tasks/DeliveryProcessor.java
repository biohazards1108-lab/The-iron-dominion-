package com.irondom.shop.tasks;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.irondom.shop.IronDominionShop;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class DeliveryProcessor implements Runnable {
    private IronDominionShop plugin;
    private Gson gson = new Gson();

    public DeliveryProcessor(IronDominionShop plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        try {
            // Get pending deliveries from API
            String response = plugin.getApiClient().getPendingDeliveries();
            JsonArray deliveries = gson.fromJson(response, JsonArray.class);

            if (deliveries == null || deliveries.size() == 0) {
                return; // No deliveries to process
            }

            plugin.getLogger().info("Processing " + deliveries.size() + " pending deliveries...");

            for (JsonElement element : deliveries) {
                JsonObject delivery = element.getAsJsonObject();
                processDelivery(delivery);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error processing deliveries: " + e.getMessage());
        }
    }

    private void processDelivery(JsonObject delivery) {
        try {
            String username = delivery.get("username").getAsString();
            String transactionId = delivery.get("transaction_id").getAsString();
            String crateName = delivery.get("crate_name").getAsString();
            JsonArray itemsArray = delivery.get("items").getAsJsonArray();

            // Check if player is online
            Player player = Bukkit.getPlayer(username);
            if (player != null && player.isOnline()) {
                // Give items to player
                for (JsonElement itemElement : itemsArray) {
                    String itemName = itemElement.getAsString();
                    giveItemToPlayer(player, itemName);
                }

                // Notify player
                player.sendMessage("§a✓ Your §6" + crateName + "§a has been delivered!");
                player.sendMessage("§eCheck your inventory for your items!");

                // Mark as delivered
                if (plugin.getApiClient().markDelivered(transactionId)) {
                    plugin.getLogger().info("Delivered " + crateName + " to " + username);
                }
            } else {
                plugin.getLogger().warning("Player " + username + " is not online. Delivery will retry.");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error processing delivery: " + e.getMessage());
        }
    }

    /**
     * Give item to player (map shop items to Minecraft items)
     */
    private void giveItemToPlayer(Player player, String itemName) {
        try {
            // Parse item (format: "Item x64" or "Item")
            String[] parts = itemName.split(" x");
            String baseItem = parts[0].toLowerCase().replace(" ", "_");
            int amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;

            // Map shop items to Minecraft items
            switch (baseItem) {
                case "iron_pickaxe":
                    player.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND_PICKAXE, amount));
                    break;
                case "coal":
                    player.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.COAL, amount));
                    break;
                case "wood":
                    player.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.OAK_LOG, amount));
                    break;
                case "electric_furnace":
                    player.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.BLAST_FURNACE, amount));
                    break;
                case "engine":
                    player.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.FURNACE, amount));
                    break;
                case "cable":
                    player.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.REDSTONE, amount));
                    break;
                case "quantum_armor_helmet":
                    player.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND_HELMET, amount));
                    break;
                case "mfsu":
                    player.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND_BLOCK, amount));
                    break;
                case "singularity":
                    player.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.NETHER_STAR, amount));
                    break;
                default:
                    plugin.getLogger().warning("Unknown item: " + itemName);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error giving item " + itemName + " to " + player.getName() + ": " + e.getMessage());
        }
    }
}
