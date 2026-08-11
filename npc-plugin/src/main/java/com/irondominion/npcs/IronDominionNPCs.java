package com.irondominion.npcs;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

/** Native Forge/Cauldron NMS-backed NPC engine for Tekkit 1.6.4. */
public final class IronDominionNPCs extends JavaPlugin implements Listener {
    private final Map<String, Entity> live = new HashMap<String, Entity>();
    private final Map<String, Long> cooldowns = new HashMap<String, Long>();

    @Override public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("IronDominionNPCs v1.0.7 enabled. Native Forge/Cauldron entity engine active.");
        getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() { public void run() { respawnAll(); } });
    }
    @Override public void onDisable() { for (Entity e : live.values()) if (e != null && !e.isDead()) e.remove(); live.clear(); cooldowns.clear(); }
    private String color(String s) { return ChatColor.translateAlternateColorCodes('&', s); }
    private void respawnAll() { if (!getConfig().isConfigurationSection("npcs")) return; for (String id : getConfig().getConfigurationSection("npcs").getKeys(false)) spawn(id); }

    private Location safeLocation(Location base) {
        Location l=base.clone(); World w=l.getWorld(); int x=l.getBlockX(), z=l.getBlockZ();
        int center=Math.max(1,Math.min(w.getMaxHeight()-2,base.getBlockY()));
        for(int y=center+4;y>=1;y--) {
            if(w.getBlockAt(x,y,z).isEmpty() && w.getBlockAt(x,y+1,z).isEmpty() && !w.getBlockAt(x,y-1,z).isEmpty()) {
                l.setY(y); return l;
            }
        }
        for(int y=1;y<=w.getMaxHeight()-2;y++) {
            if(w.getBlockAt(x,y,z).isEmpty() && w.getBlockAt(x,y+1,z).isEmpty() && !w.getBlockAt(x,y-1,z).isEmpty()) {
                l.setY(y); return l;
            }
        }
        l.setY(base.getY()+1.0D); return l;
    }

    private boolean spawnNms(String id, Location loc) {
        try {
            Class<?> entityVillager = Class.forName("net.minecraft.entity.passive.EntityVillager");
            Class<?> nmsWorld = Class.forName("net.minecraft.world.World");
            Class<?> nmsEntity = Class.forName("net.minecraft.entity.Entity");

            // Do not guess CraftBukkit's package. Cauldron's CraftWorld class is
            // exposed by the actual Bukkit World implementation at runtime.
            Object craftWorld = loc.getWorld();
            Method getHandle = craftWorld.getClass().getMethod("getHandle");
            Object worldHandle = getHandle.invoke(craftWorld);
            getLogger().info("NMS NPC WORLD HANDLE: id="+id+", class="+worldHandle.getClass().getName());

            Constructor<?> ctor = entityVillager.getConstructor(nmsWorld);
            Object nms = ctor.newInstance(worldHandle);

            Method setLocation = null;
            try { setLocation = entityVillager.getMethod("setLocationAndAngles", double.class,double.class,double.class,float.class,float.class); }
            catch (NoSuchMethodException ignored) { }
            if (setLocation != null) {
                setLocation.invoke(nms,loc.getX(),loc.getY(),loc.getZ(),loc.getYaw(),loc.getPitch());
            } else {
                Method setPosition = entityVillager.getMethod("setPosition",double.class,double.class,double.class);
                setPosition.invoke(nms,loc.getX(),loc.getY(),loc.getZ());
            }

            Method spawn = worldHandle.getClass().getMethod("spawnEntityInWorld", nmsEntity);
            Object result = spawn.invoke(worldHandle, nms);
            if (result instanceof Boolean && !((Boolean)result).booleanValue()) throw new IllegalStateException("World.spawnEntityInWorld returned false");

            Entity bukkit = (Entity) entityVillager.getMethod("getBukkitEntity").invoke(nms);
            if (bukkit == null || bukkit.isDead()) throw new IllegalStateException("Forge EntityVillager was dead immediately after spawnEntityInWorld");
            live.put(id,bukkit);
            getLogger().info("NMS NPC SPAWN SUCCESS: id="+id+", entityId="+bukkit.getEntityId()+", class="+entityVillager.getName()+", location="+bukkit.getLocation().getBlockX()+","+bukkit.getLocation().getBlockY()+","+bukkit.getLocation().getBlockZ());
            final Entity tracked=bukkit; final String trackedId=id;
            getServer().getScheduler().scheduleSyncDelayedTask(this,new Runnable(){public void run(){if(tracked.isDead())getLogger().warning("NMS NPC POST-TICK DEAD: id="+trackedId+", entityId="+tracked.getEntityId());else getLogger().info("NMS NPC POST-TICK ALIVE: id="+trackedId+", entityId="+tracked.getEntityId());}});
            return true;
        } catch(Throwable t) {
            getLogger().warning("NMS NPC SPAWN FAILED: id="+id+", "+t.getClass().getName()+": "+t.getMessage());
            return false;
        }
    }

    private boolean spawn(String id) {
        String path="npcs."+id; World w=getServer().getWorld(getConfig().getString(path+".world"));
        if(w==null){getLogger().warning("NPC "+id+" references a missing world.");return false;}
        Location saved=new Location(w,getConfig().getDouble(path+".x"),getConfig().getDouble(path+".y"),getConfig().getDouble(path+".z"),(float)getConfig().getDouble(path+".yaw"),(float)getConfig().getDouble(path+".pitch"));
        Location loc=safeLocation(saved); w.loadChunk(loc.getBlockX()>>4,loc.getBlockZ()>>4);
        Entity old=live.remove(id); if(old!=null&&!old.isDead())old.remove();
        getLogger().info("NMS NPC SPAWN ATTEMPT: id="+id+", saved="+saved.getBlockX()+","+saved.getBlockY()+","+saved.getBlockZ()+", safe="+loc.getBlockX()+","+loc.getBlockY()+","+loc.getBlockZ()+", below="+w.getBlockAt(loc.getBlockX(),loc.getBlockY()-1,loc.getBlockZ()).getType().name());
        return spawnNms(id,loc);
    }

    private String npcId(Entity e){for(Map.Entry<String,Entity>x:live.entrySet())if(x.getValue()!=null&&x.getValue().getEntityId()==e.getEntityId())return x.getKey();return null;}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=false) public void onDamage(EntityDamageEvent e){if(getConfig().getBoolean("settings.protect-npcs",true)&&npcId(e.getEntity())!=null)e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST) public void onInteract(PlayerInteractEntityEvent e){String id=npcId(e.getRightClicked());if(id==null)return;e.setCancelled(true);Player p=e.getPlayer();String path="npcs."+id;String perm=getConfig().getString(path+".permission","");if(perm.length()>0&&!p.hasPermission(perm)){p.sendMessage(color("&cYou do not have permission to use this NPC."));return;}long now=System.currentTimeMillis(),cd=getConfig().getLong("settings.interaction-cooldown-seconds",2L)*1000L;String key=p.getName().toLowerCase()+":"+id;Long last=cooldowns.get(key);if(last!=null&&now-last.longValue()<cd)return;cooldowns.put(key,Long.valueOf(now));for(String line:getConfig().getStringList(path+".lines"))p.sendMessage(color(line.replace("%player%",p.getName()).replace("%npc%",id)));for(String cmd:getConfig().getStringList(path+".commands")){String c=cmd.replace("%player%",p.getName()).replace("%npc%",id).trim();if(c.startsWith("/"))c=c.substring(1);if(c.length()>0)getServer().dispatchCommand(getServer().getConsoleSender(),c);}}

    @Override public boolean onCommand(CommandSender s,Command c,String label,String[] a){
        if(!s.hasPermission("irondominion.npcs.admin")){s.sendMessage(color("&cNo permission."));return true;}
        if(a.length==0||a[0].equalsIgnoreCase("list")){if(!getConfig().isConfigurationSection("npcs")){s.sendMessage("No NPCs configured.");return true;}s.sendMessage(color("&6Iron Dominion NPCs:"));for(String id:getConfig().getConfigurationSection("npcs").getKeys(false))s.sendMessage(color("&e- "+id));return true;}
        if(a[0].equalsIgnoreCase("version")){s.sendMessage(color("&6IronDominionNPCs &ev1.0.7 &7| native Forge/Cauldron 1.6.4 entity engine"));return true;}
        if(a[0].equalsIgnoreCase("debug")){s.sendMessage(color("&6NPC plugin: &e"+getDescription().getVersion()));s.sendMessage(color("&6Configured NPCs: &e"+(getConfig().isConfigurationSection("npcs")?getConfig().getConfigurationSection("npcs").getKeys(false).size():0)));s.sendMessage(color("&6Live NPCs: &e"+live.size()));if(s instanceof Player){Player p=(Player)s;s.sendMessage(color("&6Your location: &e"+p.getWorld().getName()+" "+p.getLocation().getBlockX()+","+p.getLocation().getBlockY()+","+p.getLocation().getBlockZ()));}return true;}
        if(a[0].equalsIgnoreCase("reload")){reloadConfig();respawnAll();s.sendMessage(color("&aNPC configuration reloaded."));return true;}if(a[0].equalsIgnoreCase("respawn")){respawnAll();s.sendMessage(color("&aNPCs respawned."));return true;}
        if(!(s instanceof Player)){s.sendMessage("This command requires a player for create/remove.");return true;}Player p=(Player)s;
        if(a[0].equalsIgnoreCase("create")&&a.length>=2){String id=a[1].toLowerCase(),path="npcs."+id;Location l=p.getLocation();getConfig().set(path+".world",l.getWorld().getName());getConfig().set(path+".x",l.getX());getConfig().set(path+".y",l.getY());getConfig().set(path+".z",l.getZ());getConfig().set(path+".yaw",l.getYaw());getConfig().set(path+".pitch",l.getPitch());getConfig().set(path+".name","&6"+id);getConfig().set(path+".lines",java.util.Arrays.asList("&eWelcome to Iron Dominion!"));getConfig().set(path+".commands",java.util.Collections.emptyList());getConfig().set(path+".permission","");saveConfig();getLogger().info("NPC CREATE REQUEST: id="+id+" from="+p.getName());if(spawn(id))p.sendMessage(color("&aCreated NPC &e"+id+" &7and spawned it. Plugin v1.0.7"));else p.sendMessage(color("&cNPC &e"+id+" &ccould not be spawned. Check the server console."));return true;}
        if(a[0].equalsIgnoreCase("remove")&&a.length>=2){String id=a[1].toLowerCase();Entity e=live.remove(id);if(e!=null&&!e.isDead())e.remove();getConfig().set("npcs."+id,null);saveConfig();p.sendMessage(color("&aRemoved NPC &e"+id));return true;}
        s.sendMessage(color("&e/idnpc version &7| &e/idnpc debug &7| &e/idnpc list &7| &e/idnpc create <id> &7| &e/idnpc remove <id> &7| &e/idnpc reload &7| &e/idnpc respawn"));return true;
    }
}
