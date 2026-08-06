package com.irondom.shop.commands;

import com.irondom.shop.IronDominionShop;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class AdminCommand implements CommandExecutor {
    private IronDominionShop plugin;

    public AdminCommand(IronDominionShop plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("irondom.admin")) {
            sender.sendMessage("§c✗ You don't have permission to use this command.");
            return false;
        }

        if (args.length == 0) {
            sender.sendMessage("§6Usage: /domadmin <addtokens|sync|status>");
            sender.sendMessage("§6  /domadmin addtokens <player> <amount> <reason>");
            sender.sendMessage("§6  /domadmin sync - Sync pending deliveries");
            sender.sendMessage("§6  /domadmin status - Check API status");
            return true;
        }

        String subcommand = args[0].toLowerCase();

        if (subcommand.equals("addtokens")) {
            if (args.length < 3) {
                sender.sendMessage("§cUsage: /domadmin addtokens <player> <amount> [reason]");
                return false;
            }
            
            String playerName = args[1];
            int amount;
            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§c✗ Amount must be a number.");
                return false;
            }

            String reason = args.length > 3 ? String.join(" ", java.util.Arrays.copyOfRange(args, 3, args.length)) : "Admin reward";

            boolean success = plugin.getApiClient().addPlayerTokens(playerName, amount, reason);
            if (success) {
                sender.sendMessage("§a✓ Added " + amount + " tokens to " + playerName);
                Bukkit.getServer().broadcastMessage("§6" + playerName + " received " + amount + " Dominion Tokens!");
            } else {
                sender.sendMessage("§c✗ Failed to add tokens.");
            }
            return true;
        }

        if (subcommand.equals("sync")) {
            sender.sendMessage("§6Syncing pending deliveries...");
            // This is handled by the DeliveryProcessor task
            sender.sendMessage("§aDeliveries will be processed shortly.");
            return true;
        }

        if (subcommand.equals("status")) {
            boolean connected = plugin.getApiClient().isConnected();
            if (connected) {
                sender.sendMessage("§a✓ API Server Status: CONNECTED");
            } else {
                sender.sendMessage("§c✗ API Server Status: DISCONNECTED");
            }
            return true;
        }

        sender.sendMessage("§cUnknown subcommand: " + subcommand);
        return false;
    }
}
