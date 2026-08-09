package com.irondominion.npcs;

import java.lang.reflect.Method;
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
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class IronDominionNPCs extends JavaPlugin implements Listener {
    private final Map<String, Entity> live = new HashMap<String, Entity>();
    private final Map<String, Object> citizensNpcs = new HashMap<String, Object>();
    private final Map<String, Long> cooldowns = new HashMap<String, Long>();

    @Override public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("IronDominionNPCs v1.0.5 enabled. Citizens-backed NPC engine preferred.");
        getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() { public void run() { respawnAll(); } });
    }

    @Override public void onDisable() {
        for (String id : citizensNpcs.keySet()) destroyCitizensNpc(citizensNpcs.get(id));
        citizensNpcs.clear(); live.clear(); cooldowns.clear();
    }

    private String color(String s) { return ChatColor.translateAlternateColorCodes('&', s); }
    private boolean citizensAvailable() {
        Plugin p = getServer().getPluginManager().getPlugin("Citizens");
        return p != null && p.isEnabled();
    }
    private void respawnAll() {
        if (!getConfig().isConfigurationSection("npcs")) return;
        for (String id : getConfig().getConfigurationSection("npcs").getKeys(false)) spawn(id);
    }
    private Location safeLocation(Location base) {
        Location l = base.clone(); World w = l.getWorld(); int x = l.getBlockX(), z = l.getBlockZ();
        int start = Math.max(1, l.getBlockY());
        for (int y = start; y <= start + 8; y++) {
            if (w.getBlockAt(x, y, z).isEmpty() && w.getBlockAt(x, y + 1, z).isEmpty() && !w.getBlockAt(x, y - 1, z).isEmpty()) { l.setY(y); return l; }
        }
        l.setY(base.getY() + 1.0D); return l;
    }
    private void destroyCitizensNpc(Object npc) {
        if (npc == null) return;
        try { npc.getClass().getMethod("despawn").invoke(npc); } catch (Throwable ignored) { }
        try { npc.getClass().getMethod("destroy").invoke(npc); } catch (Throwable ignored) { }
    }
    private Entity citizensEntity(Object npc) {
        if (npc == null) return null;
        try {
            Object result = npc.getClass().getMethod("getEntity").invoke(npc);
            if (result instanceof Entity) return (Entity) result;
        } catch (Throwable t) { getLogger().warning("Citizens getEntity failed: " + t.getClass().getName() + ": " + t.getMessage()); }
        return null;
    }
    private boolean spawnWithCitizens(String id, String path, Location loc) {
        try {
            Class<?> api = Class.forName("net.citizensnpcs.api.CitizensAPI");
            Object registry = api.getMethod("getNPCRegistry").invoke(null);
            if (registry == null) throw new IllegalStateException("Citizens NPCRegistry is null");
            String npcType = getConfig().getString(path + ".type", "PLAYER").toUpperCase();
            EntityType type = EntityType.valueOf(npcType);
            String name = color(getConfig().getString(path + ".name", id));
            Object npc = registry.getClass().getMethod("createNPC", EntityType.class, String.class).invoke(registry, type, name);
            if (npc == null) throw new IllegalStateException("Citizens returned a null NPC");
            Object spawned = npc.getClass().getMethod("spawn", Location.class).invoke(npc, loc);
            if (spawned instanceof Boolean && !((Boolean) spawned).booleanValue()) throw new IllegalStateException("Citizens NPC.spawn returned false");
            Entity entity = citizensEntity(npc);
            if (entity == null || entity.isDead()) throw new IllegalStateException("Citizens NPC has no live entity after spawn");
            citizensNpcs.put(id, npc); live.put(id, entity);
            getLogger().info("CITIZENS NPC SPAWN SUCCESS: id=" + id + ", entityId=" + entity.getEntityId() + ", type=" + type.name());
            final Entity tracked = entity; final String trackedId = id;
            getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() { public void run() {
                if (tracked.isDead()) getLogger().warning("CITIZENS NPC POST-TICK DEAD: id=" + trackedId + ", entityId=" + tracked.getEntityId());
                else getLogger().info("CITIZENS NPC POST-TICK ALIVE: id=" + trackedId + ", entityId=" + tracked.getEntityId());
            }});
            return true;
        } catch (Throwable t) {
            getLogger().warning("CITIZENS NPC SPAWN FAILED: id=" + id + ", " + t.getClass().getName() + ": " + t.getMessage());
            return false;
        }
    }
    private boolean spawnNative(String id, String path, Location loc) {
        try {
            Entity entity = loc.getWorld().spawnCreature(loc, EntityType.VILLAGER);
            if (entity == null || entity.isDead()) return false;
            Villager v = (Villager) entity; v.setCustomName(color(getConfig().getString(path + ".name", id))); v.setCustomNameVisible(true); v.setRemoveWhenFarAway(false);
            live.put(id, entity); getLogger().info("NATIVE NPC SPAWN SUCCESS: id=" + id + ", entityId=" + entity.getEntityId()); return true;
        } catch (Throwable t) { getLogger().warning("Native NPC spawn failed: " + t.getClass().getName() + ": " + t.getMessage()); return false; }
    }
    private boolean spawn(String id) {
        String path = "npcs." + id;
        World world = getServer().getWorld(getConfig().getString(path + ".world"));
        if (world == null) { getLogger().warning("NPC " + id + " references a missing world."); return false; }
        Location saved = new Location(world, getConfig().getDouble(path + ".x"), getConfig().getDouble(path + ".y"), getConfig().getDouble(path + ".z"), (float)getConfig().getDouble(path + ".yaw"), (float)getConfig().getDouble(path + ".pitch"));
        Location loc = safeLocation(saved); world.loadChunk(loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
        Entity old = live.remove(id); if (old != null && !old.isDead()) old.remove(); Object oldCitizens = citizensNpcs.remove(id); if (oldCitizens != null) destroyCitizensNpc(oldCitizens);
        getLogger().info("NPC SPAWN ATTEMPT: id=" + id + ", saved=" + saved.getBlockX() + "," + saved.getBlockY() + "," + saved.getBlockZ() + ", safe=" + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
        if (citizensAvailable()) {
            getLogger().info("NPC ENGINE: Citizens detected; using Citizens API for physical rendering.");
            if (spawnWithCitizens(id, path, loc)) return true;
            getLogger().warning("Citizens failed for NPC " + id + "; native fallback will be attempted.");
        } else getLogger().warning("Citizens is not enabled; using native legacy villager fallback.");
        return spawnNative(id, path, loc);
    }
    private String npcId(Entity entity) { for (Map.Entry<String, Entity> e : live.entrySet()) { Entity tracked = e.getValue(); if (tracked != null && tracked.getEntityId() == entity.getEntityId()) return e.getKey(); } return null; }
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
        if (args[0].equalsIgnoreCase("version")) { sender.sendMessage(color("&6IronDominionNPCs &ev1.0.5 &7| Citizens-backed legacy NPC engine")); return true; }
        if (args[0].equalsIgnoreCase("debug")) { sender.sendMessage(color("&6NPC plugin: &e" + getDescription().getVersion())); sender.sendMessage(color("&6Citizens available: &e" + citizensAvailable())); sender.sendMessage(color("&6Configured NPCs: &e" + (getConfig().isConfigurationSection("npcs") ? getConfig().getConfigurationSection("npcs").getKeys(false).size() : 0))); sender.sendMessage(color("&6Live NPCs: &e" + live.size())); if (sender instanceof Player) { Player p = (Player)sender; sender.sendMessage(color("&6Your location: &e" + p.getWorld().getName() + " " + p.getLocation().getBlockX() + "," + p.getLocation().getBlockY() + "," + p.getLocation().getBlockZ())); } return true; }
        if (args[0].equalsIgnoreCase("reload")) { reloadConfig(); respawnAll(); sender.sendMessage(color("&aNPC configuration reloaded.")); return true; }
        if (args[0].equalsIgnoreCase("respawn")) { respawnAll(); sender.sendMessage(color("&aNPCs respawned.")); return true; }
        if (!(sender instanceof Player)) { sender.sendMessage("This command requires a player for create/remove."); return true; }
        Player p = (Player)sender;
        if (args[0].equalsIgnoreCase("create") && args.length >= 2) {
            String id = args[1].toLowerCase(); String path = "npcs." + id; Location l = p.getLocation();
            getConfig().set(path + ".world", l.getWorld().getName()); getConfig().set(path + ".x", l.getX()); getConfig().set(path + ".y", l.getY()); getConfig().set(path + ".z", l.getZ()); getConfig().set(path + ".yaw", l.getYaw()); getConfig().set(path + ".pitch", l.getPitch()); getConfig().set(path + ".name", "&6" + id); getConfig().set(path + ".type", "PLAYER"); getConfig().set(path + ".lines", java.util.Arrays.asList("&eWelcome to Iron Dominion!")); getConfig().set(path + ".commands", java.util.Collections.emptyList()); getConfig().set(path + ".permission", ""); saveConfig();
            getLogger().info("NPC CREATE REQUEST: id=" + id + " from=" + p.getName()); if (spawn(id)) p.sendMessage(color("&aCreated NPC &e" + id + " &7and spawned it. Plugin v1.0.5")); else p.sendMessage(color("&cNPC &e" + id + " &ccould not be spawned. Check the server console.")); return true;
        }
        if (args[0].equalsIgnoreCase("remove") && args.length >= 2) { String id = args[1].toLowerCase(); Entity e = live.remove(id); if (e != null && !e.isDead()) e.remove(); Object npc = citizensNpcs.remove(id); if (npc != null) destroyCitizensNpc(npc); getConfig().set("npcs." + id, null); saveConfig(); p.sendMessage(color("&aRemoved NPC &e" + id)); return true; }
        p.sendMessage(color("&e/idnpc version &7| &e/idnpc debug &7| &e/idnpc list &7| &e/idnpc create <id> &7| &e/idnpc remove <id> &7| &e/idnpc reload &7| &e/idnpc respawn")); return true;
    }
}
