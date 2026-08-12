package com.irondom.shop.commands;

import com.irondom.shop.ranks.RankManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Public rank lookup plus protected rank administration. */
public final class DominionCommand implements CommandExecutor {
    private final RankManager ranks;

    public DominionCommand(RankManager ranks) {
        this.ranks = ranks;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            help(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("ranks")) {
            sender.sendMessage("§6Iron Dominion: §fIRON §7→ §fSTEEL §7→ §fTITANIUM §7→ §fDIAMOND §7→ §fOBSIDIAN §7→ §fDOMINION §7→ §fOVERLORD");
            return true;
        }

        if (!args[0].equalsIgnoreCase("rank")) {
            help(sender);
            return true;
        }

        if (args.length == 1) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cConsole usage: /dominion rank <player>");
                return true;
            }
            show(sender, sender.getName());
            return true;
        }

        if (args.length == 2 && !args[1].equalsIgnoreCase("set") && !args[1].equalsIgnoreCase("remove")) {
            show(sender, args[1]);
            return true;
        }

        if (!sender.hasPermission("irondominion.rank.admin")) {
            sender.sendMessage("§c✗ You don't have permission to manage ranks.");
            return true;
        }

        if (args.length == 4 && args[1].equalsIgnoreCase("set")) {
            String player = args[2];
            String rank = args[3];
            if (!ranks.isValidRank(rank)) {
                sender.sendMessage("§cInvalid rank. Use IRON, STEEL, TITANIUM, DIAMOND, OBSIDIAN, DOMINION, or OVERLORD.");
                return true;
            }
            if (ranks.setRank(player, rank)) {
                sender.sendMessage("§a✓ " + player + " is now " + rank.toUpperCase() + ".");
            } else {
                sender.sendMessage("§c✗ GroupManager rejected the rank change or rank data could not be saved.");
            }
            return true;
        }

        if (args.length == 3 && args[1].equalsIgnoreCase("remove")) {
            if (ranks.removeRank(args[2])) {
                sender.sendMessage("§a✓ Removed the Dominion rank assignment from " + args[2] + ".");
            } else {
                sender.sendMessage("§c✗ GroupManager rejected the rank removal or rank data could not be saved.");
            }
            return true;
        }

        help(sender);
        return true;
    }

    private void show(CommandSender sender, String player) {
        String rank = ranks.getStoredRank(player);
        sender.sendMessage("§6Dominion rank for §f" + player + "§6: §f" + (rank.isEmpty() ? "Unranked" : rank));
    }

    private void help(CommandSender sender) {
        sender.sendMessage("§6§lIron Dominion");
        sender.sendMessage("§f/dominion rank §7- View your stored rank");
        sender.sendMessage("§f/dominion rank <player> §7- View a stored rank");
        sender.sendMessage("§f/dominion ranks §7- List available ranks");
        if (sender.hasPermission("irondominion.rank.admin")) {
            sender.sendMessage("§f/dominion rank set <player> <rank> §7- Assign a rank");
            sender.sendMessage("§f/dominion rank remove <player> §7- Remove a rank");
        }
    }
}
