package com.irondom.shop.commands;

import com.irondom.shop.IronDominionShop;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ShopCommand implements CommandExecutor {
    private IronDominionShop plugin;

    public ShopCommand(IronDominionShop plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by players!");
            return false;
        }

        Player player = (Player) sender;
        String playerName = player.getName();

        // Get balance from API
        int balance = plugin.getApiClient().getPlayerBalance(playerName);
        
        if (balance < 0) {
            player.sendMessage("§c✗ Error connecting to shop server. Try again later.");
            return false;
        }

        // Display shop info
        player.sendMessage("\n§6§l════════════════════════════════════════");
        player.sendMessage("§6§l🏪 Iron Dominion Shop §6§l🏪");
        player.sendMessage("§6§l════════════════════════════════════════");
        player.sendMessage("");
        player.sendMessage("§eYour Balance: §a⚒ " + balance + " Dominion Tokens");
        player.sendMessage("");
        player.sendMessage("§6Available Crates:");
        player.sendMessage("§7• §fStarter Crate §7- §a100 tokens§7 - Basic tools & resources");
        player.sendMessage("§7• §fIndustrial Crate §7- §a500 tokens§7 - Machines & power");
        player.sendMessage("§7• §fLegendary Crate §7- §a2000 tokens§7 - Rare & exclusive");
        player.sendMessage("");
        player.sendMessage("§eTo purchase, visit: §b/domshop buy <cratename>");
        player.sendMessage("§6§l════════════════════════════════════════\n");

        return true;
    }
}
