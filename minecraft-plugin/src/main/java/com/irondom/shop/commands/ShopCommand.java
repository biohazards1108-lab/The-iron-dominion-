package com.irondom.shop.commands;

import com.irondom.shop.IronDominionShop;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Public command intentionally exposes status only until the production store is connected. */
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
        player.sendMessage("§6§l        Iron Dominion Support");
        player.sendMessage("§6§l════════════════════════════════════════");
        player.sendMessage("§eVoting is free and helps the server grow.");
        player.sendMessage("§eDominion Tokens are awarded only for verified server-side rewards.");
        player.sendMessage("§7The online store is not connected to this legacy server yet.");
        player.sendMessage("§7No purchase command is active, so nothing can accidentally charge or deliver an unverified product.");
        player.sendMessage("§6§l════════════════════════════════════════");
        return true;
    }
}
