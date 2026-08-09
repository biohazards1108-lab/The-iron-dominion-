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
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class IronDominionNPCs extends JavaPlugin implements Listener {
    private final Map<String, Entity> live = new HashMap<String, Entity>();
    private final Map<String, Long> cooldowns = new HashMap<String, Long>();
    private String pendingId;
    private Location pendingLocation;
    private long pendingStarted;

    @Override public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("IronDominionNPCs v1.0.3 enabled. Spawn-event diagnostics active.");
        respawnAll();
    }
    @Override public void onDisable() { live.clear(); cooldowns.clear(); pendingId = null; pendingLocation = null; }
    private String color(String s) { return ChatColor.translateAlternateColorCodes('&', s); }
    private void respawnAll() {
        if (!getConfig().isConfigurationSection("npcs")) return;
        for (String id : getConfig().getConfigurationSection("npcs").getKeys(false)) spawn(id);
    }
    private boolean sameTarget(Location a, Location b) {
        if (a == null || b == null || !a.getWorld().getName().equals(b.getWorld().getName())) return false;
        return a.distanceSquared(b) <= 4.0D;
    }
    private boolean spawn(String id) {
        String path = "npcs." + id;
        World world = getServer().getWorld(getConfig().getString(path + ".world"));
        if (world == null) { getLogger().warning("NPC " + id + " references a missing world."); return false; }
        Location loc = new Location(world, getConfig().getDouble(path + ".x"), getConfig().getDouble(path + ".y") + 1.0D, getConfig().getDouble(path + ".z"), (float)getConfig().getDouble(path + ".yaw"), (float)getConfig().getDouble(path + ".pitch"));
        world.loadChunk(loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
        Entity old = live.remove(id); if (old != null && !old.isDead()) old.remove();
        getLogger().info("NPC SPAWN ATTEMPT: id=" + id + ", world=" + world.getName() + ", location=" + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
        pendingId = id; pendingLocation = loc.clone(); pendingStarted = System.currentTimeMillis();
        Entity entity = null;
        try { entity = world.spawnEntity(loc, EntityType.VILLAGER); }
        catch (Throwable t) { getLogger().warning("spawnEntity exception: " + t.getClass().getName() + ": " + t.getMessage()); }
        if (entity == null || entity.isDead()) {
            try { entity = world.spawnCreature(loc, EntityType.VILLAGER); }
            catch (Throwable t) { getLogger().warning("spawnCreature exception: " + t.getClass().getName() + ": " + t.getMessage()); pendingId = null; pendingLocation = null; return false; }
        }
        if (entity == null) { getLogger().warning("Both Bukkit spawn APIs returned null."); pendingId = null; pendingLocation = null; return false; }
        if (entity.isDead()) {
            getLogger().warning("NPC " + id + " returned DEAD immediately. Spawn event was either cancelled or the server rejected/removed the entity.");
            pendingId = null; pendingLocation = null; return false;
        }
        if (!(entity instanceof Villager)) { getLogger().warning("NPC " + id + " spawned " + entity.getType().name() + " instead of VILLAGER."); pendingId = null; pendingLocation = null; entity.remove(); return false; }
        Villager v = (Villager)entity;
        v.setCustomName(color(getConfig().getString(path + ".name", id)));
        v.setCustomNameVisible(true);
        v.setRemoveWhenFarAway(false);
        try { v.setProfession(Villager.Profession.valueOf(getConfig().getString(path + ".profession", "LIBRARIAN").toUpperCase())); } catch (IllegalArgumentException ignored) { }
        live.put(id, entity);
        getLogger().info("NPC SPAWN SUCCESS: id=" + id + ", entityId=" + entity.getEntityId());
        final Entity spawned = entity;
        final String spawnedId = id;
        getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() { public void run() {
            if (spawned.isDead()) getLogger().warning("NPC SPAWN POST-TICK DEAD: id=" + spawnedId + ", entityId=" + spawned.getEntityId());
            else getLogger().info("NPC SPAWN POST-TICK ALIVE: id=" + spawnedId + ", entityId=" + spawned.getEntityId());
        }});
        pendingId = null; pendingLocation = null;
        return true;
    }
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (pendingId == null || pendingLocation == null) return;
        if (System.currentTimeMillis() - pendingStarted > 2000L) return;
        if (event.getEntityType() != EntityType.VILLAGER || !sameTarget(event.getLocation(), pendingLocation)) return;
        if (event.isCancelled()) getLogger().warning("NPC SPAWN CANCELLED by another plugin/mod: id=" + pendingId + ", reason=" + event.getSpawnReason());
        else getLogger().info("NPC CreatureSpawnEvent accepted: id=" + pendingId + ", reason=" + event.getSpawnReason());
    }
    private String npcId(Entity entity) { for (Map.Entry<String, Entity> e : live.entrySet()) if (e.getValue().getEntityId() == entity.getEntityId()) return e.getKey(); return null; }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDamage(EntityDamageEvent event) { if (getConfig().getBoolean("settings.protect-npcs", true) && npcId(event.getEntity()) != null) event.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEntityEvent event) {
        String id = npcId(event.getRightClicked()); if (id == null) return; event.setCancelled(true); Player p = event.getPlayer(); String path = "npcs." + id;
        String permission = getConfig().getString(path + ".permission", ""); if (permission.length() > 0 && !p.hasPermission(permission)) { p.sendMessage(color("&cYou do not have permission to use this NPC.")); return; }
        String key = p.getName().toLowerCase() + ":" + id; long now = System.currentTimeMillis(); long cd = getConfig().getLong("settings.interaction-cooldown-seconds", 2L) * 1000L; Long last = cooldowns.get(key); if (last != null && now - last.longValue() < cd) return; cooldowns.put(key, Long.valueOf(now));
        for (String line : getConfig().getStringList(path + ".lines")) p.sendMessage(color(line.replace("%player%", p.getName()).replace("%npc%", id)));
        for (String command : getConfig().getStringList(path + ".commands")) { String c = command.replace("%player%", p.getName()).replace("%npc%", id).trim(); if (c.startsWith("/")) c = c.substring(1); if (c.length() > 0) getServer().dispatchCommand(getServer().getConsoleSender(), c); }
    }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("irondominion.npcs.admin")) { sender.sendMessage(color("&cNo permission.")); return true; }
        if (args.length == 0 || args[0].equalsIgnoreCase("list")) { if (!getConfig().isConfigurationSection("npcs")) { sender.sendMessage("No NPCs configured."); return true; } sender.sendMessage(color("&6Iron Dominion NPCs:")); for (String id : getConfig().getConfigurationSection("npcs").getKeys(false)) sender.sendMessage(color("&e- " + id)); return true; }
        if (args[0].equalsIgnoreCase("version")) { sender.sendMessage(color("&6IronDominionNPCs &ev1.0.3 &7| legacy 1.6.4 spawn engine")); return true; }
        if (args[0].equalsIgnoreCase("debug")) { sender.sendMessage(color("&6NPC plugin: &e" + getDescription().getVersion())); sender.sendMessage(color("&6Configured NPCs: &e" + (getConfig().isConfigurationSection("npcs") ? getConfig().getConfigurationSection("npcs").getKeys(false).size() : 0))); if (sender instanceof Player) { Player p = (Player)sender; sender.sendMessage(color("&6Your location: &e" + p.getWorld().getName() + " " + p.getLocation().getBlockX() + "," + p.getLocation().getBlockY() + "," + p.getLocation().getBlockZ())); sender.sendMessage(color("&6Live NPCs: &e" + live.size())); } return true; }
        if (args[0].equalsIgnoreCase("reload")) { reloadConfig(); respawnAll(); sender.sendMessage(color("&aNPC configuration reloaded.")); return true; }
        if (args[0].equalsIgnoreCase("respawn")) { respawnAll(); sender.sendMessage(color("&aNPCs respawned.")); return true; }
        if (!(sender instanceof Player)) { sender.sendMessage("This command requires a player for create/remove."); return true; }
        Player p = (Player)sender;
        if (args[0].equalsIgnoreCase("create") && args.length >= 2) { String id = args[1].toLowerCase(); String path = "npcs." + id; Location l = p.getLocation(); getConfig().set(path + ".world", l.getWorld().getName()); getConfig().set(path + ".x", l.getX()); getConfig().set(path + ".y", l.getY()); getConfig().set(path + ".z", l.getZ()); getConfig().set(path + ".yaw", l.getYaw()); getConfig().set(path + ".pitch", l.getPitch()); getConfig().set(path + ".name", "&6" + id); getConfig().set(path + ".profession", "LIBRARIAN"); getConfig().set(path + ".lines", java.util.Arrays.asList("&eWelcome to Iron Dominion!")); getConfig().set(path + ".commands", java.util.Collections.emptyList()); getConfig().set(path + ".permission", ""); saveConfig(); getLogger().info("NPC CREATE REQUEST: id=" + id + " from=" + p.getName()); if (spawn(id)) p.sendMessage(color("&aCreated NPC &e" + id + " &7and spawned it. Plugin v1.0.3")); else p.sendMessage(color("&cNPC &e" + id + " &ccould not be spawned. Check the server console.")); return true; }
        if (args[0].equalsIgnoreCase("remove") && args.length >= 2) { String id = args[1].toLowerCase(); Entity e = live.remove(id); if (e != null && !e.isDead()) e.remove(); getConfig().set("npcs." + id, null); saveConfig(); p.sendMessage(color("&aRemoved NPC &e" + id)); return true; }
        p.sendMessage(color("&e/idnpc version &7| &e/idnpc debug &7| &e/idnpc list &7| &e/idnpc create <id> &7| &e/idnpc remove <id> &7| &e/idnpc reload &7| &e/idnpc respawn")); return true;
    }
}
