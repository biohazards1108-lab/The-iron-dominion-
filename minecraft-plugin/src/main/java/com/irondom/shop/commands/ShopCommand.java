package com.irondom.shop.commands;

import com.irondom.shop.IronDominionShop;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Public in-game store status command. */
public class ShopCommand implements CommandExecutor {
    private final IronDominionShop plugin;

    public ShopCommand(IronDominionShop plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by players.");
            return true;
        }

        Player player = (Player) sender;
        player.sendMessage("§6§l════════════════════════════════════════");
        player.sendMessage("§6§l          IRON DOMINION STORE");
        player.sendMessage("§6§l════════════════════════════════════════");
        player.sendMessage("§eStore: §fhttps://iron-dominion-wasteland-studios-projects.vercel.app/shop.html");
        player.sendMessage("§eSeven supporter ranks are delivered through the secure Tebex bridge.");
        player.sendMessage("§7Purchases are verified by Tebex before a rank command is executed.");
        player.sendMessage("§7Current bridge: §a" + (plugin.getApiClient().tebexReady() ? "CONNECTED" : "NOT CONFIGURED"));
        player.sendMessage("§6§l════════════════════════════════════════");
        return true;
    }
}
