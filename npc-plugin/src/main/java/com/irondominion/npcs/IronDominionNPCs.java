package com.irondominion.npcs;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class IronDominionNPCs extends JavaPlugin implements Listener {
    private final Map<String, Entity> live = new HashMap<String, Entity>();
    private final Map<String, Long> cooldowns = new HashMap<String, Long>();

    @Override public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        respawnAll();
        getLogger().info("IronDominionNPCs enabled.");
    }

    @Override public void onDisable() { live.clear(); cooldowns.clear(); }

    private String color(String s) { return ChatColor.translateAlternateColorCodes('&', s); }

    private void respawnAll() {
        if (!getConfig().isConfigurationSection("npcs")) return;
        for (String id : getConfig().getConfigurationSection("npcs").getKeys(false)) spawn(id);
    }

    private boolean spawn(String id) {
        String path = "npcs." + id;
        String worldName = getConfig().getString(path + ".world");
        World world = getServer().getWorld(worldName);
        if (world == null) {
            getLogger().warning("NPC " + id + " references missing world " + worldName);
            return false;
        }

        double x = getConfig().getDouble(path + ".x");
        double y = getConfig().getDouble(path + ".y") + 0.05D;
        double z = getConfig().getDouble(path + ".z");
        float yaw = (float)getConfig().getDouble(path + ".yaw");
        float pitch = (float)getConfig().getDouble(path + ".pitch");
        Location loc = new Location(world, x, y, z, yaw, pitch);

        world.loadChunk(loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
        Entity old = live.remove(id);
        if (old != null && !old.isDead()) old.remove();

        Entity entity;
        try {
            // spawnCreature is the legacy Bukkit API path intended for 1.6.x.
            entity = world.spawnCreature(loc, EntityType.VILLAGER);
        } catch (Throwable t) {
            getLogger().warning("Could not spawn NPC " + id + ": " + t.getClass().getName() + ": " + t.getMessage());
            return false;
        }
        if (entity == null || entity.isDead()) {
            getLogger().warning("NPC " + id + " was not created by the server.");
            return false;
        }

        Villager v = (Villager) entity;
        v.setCustomName(color(getConfig().getString(path + ".name", id)));
        v.setCustomNameVisible(true);
        v.setRemoveWhenFarAway(false);
        String profession = getConfig().getString(path + ".profession", "LIBRARIAN");
        try { v.setProfession(Villager.Profession.valueOf(profession.toUpperCase())); } catch (IllegalArgumentException ignored) { }
        live.put(id, entity);
        getLogger().info("Spawned NPC '" + id + "' as entity #" + entity.getEntityId() + " at " + world.getName() + " " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
        return true;
    }

    private String npcId(Entity entity) {
        for (Map.Entry<String, Entity> e : live.entrySet()) if (e.getValue().getEntityId() == entity.getEntityId()) return e.getKey();
        return null;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDamage(EntityDamageEvent event) {
        if (getConfig().getBoolean("settings.protect-npcs", true) && npcId(event.getEntity()) != null) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEntityEvent event) {
        String id = npcId(event.getRightClicked());
        if (id == null) return;
        event.setCancelled(true);
        Player p = event.getPlayer();
        String path = "npcs." + id;
        String permission = getConfig().getString(path + ".permission", "");
        if (permission.length() > 0 && !p.hasPermission(permission)) { p.sendMessage(color("&cYou do not have permission to use this NPC.")); return; }
        String key = p.getName().toLowerCase() + ":" + id;
        long now = System.currentTimeMillis();
        long cd = getConfig().getLong("settings.interaction-cooldown-seconds", 2L) * 1000L;
        Long last = cooldowns.get(key);
        if (last != null && now - last.longValue() < cd) return;
        cooldowns.put(key, Long.valueOf(now));
        List<String> lines = getConfig().getStringList(path + ".lines");
        for (String line : lines) p.sendMessage(color(line.replace("%player%", p.getName()).replace("%npc%", id)));
        List<String> commands = getConfig().getStringList(path + ".commands");
        for (String command : commands) {
            String c = command.replace("%player%", p.getName()).replace("%npc%", id).trim();
            if (c.startsWith("/")) c = c.substring(1);
            if (c.length() > 0) getServer().dispatchCommand(getServer().getConsoleSender(), c);
        }
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("irondominion.npcs.admin")) { sender.sendMessage(color("&cNo permission.")); return true; }
        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            if (!getConfig().isConfigurationSection("npcs")) { sender.sendMessage("No NPCs configured."); return true; }
            sender.sendMessage(color("&6Iron Dominion NPCs:"));
            for (String id : getConfig().getConfigurationSection("npcs").getKeys(false)) sender.sendMessage(color("&e- " + id));
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) { reloadConfig(); respawnAll(); sender.sendMessage(color("&aNPC configuration reloaded.")); return true; }
        if (args[0].equalsIgnoreCase("respawn")) { respawnAll(); sender.sendMessage(color("&aNPCs respawned.")); return true; }
        if (!(sender instanceof Player)) { sender.sendMessage("This command requires a player for create/remove."); return true; }
        Player p = (Player)sender;
        if (args[0].equalsIgnoreCase("create") && args.length >= 2) {
            String id = args[1].toLowerCase();
            String path = "npcs." + id;
            Location l = p.getLocation();
            getConfig().set(path + ".world", l.getWorld().getName());
            getConfig().set(path + ".x", l.getX()); getConfig().set(path + ".y", l.getY()); getConfig().set(path + ".z", l.getZ());
            getConfig().set(path + ".yaw", l.getYaw()); getConfig().set(path + ".pitch", l.getPitch());
            getConfig().set(path + ".name", "&6" + id); getConfig().set(path + ".profession", "LIBRARIAN");
            getConfig().set(path + ".lines", java.util.Arrays.asList("&eWelcome to Iron Dominion!"));
            getConfig().set(path + ".commands", java.util.Collections.emptyList()); getConfig().set(path + ".permission", "");
            saveConfig();
            if (spawn(id)) p.sendMessage(color("&aCreated NPC &e" + id + " &7and spawned it."));
            else p.sendMessage(color("&cNPC &e" + id + " &ccould not be spawned. Check the server console."));
            return true;
        }
        if (args[0].equalsIgnoreCase("remove") && args.length >= 2) {
            String id = args[1].toLowerCase(); Entity e = live.remove(id); if (e != null && !e.isDead()) e.remove();
            getConfig().set("npcs." + id, null); saveConfig(); p.sendMessage(color("&aRemoved NPC &e" + id)); return true;
        }
        p.sendMessage(color("&e/idnpc list &7| &e/idnpc create <id> &7| &e/idnpc remove <id> &7| &e/idnpc reload &7| &e/idnpc respawn"));
        return true;
    }
}
