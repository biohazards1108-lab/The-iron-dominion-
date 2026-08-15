package com.irondominion.npcs;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
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

/** Native Forge/Cauldron 1.6.4 NPC engine. */
public final class IronDominionNPCs extends JavaPlugin implements Listener {
 private final Map<String,Entity> live=new HashMap<String,Entity>();
 private final Map<String,Long> cooldowns=new HashMap<String,Long>();
 private String color(String s){return ChatColor.translateAlternateColorCodes('&',s);}
 @Override public void onEnable(){saveDefaultConfig();getServer().getPluginManager().registerEvents(this,this);getLogger().info("IronDominionNPCs v1.0.9 enabled. Native Forge/Cauldron entity engine active.");getServer().getScheduler().scheduleSyncDelayedTask(this,new Runnable(){public void run(){respawnAll();}});}
 @Override public void onDisable(){for(Entity e:live.values())if(e!=null&&!e.isDead())e.remove();live.clear();cooldowns.clear();}
 private void respawnAll(){if(!getConfig().isConfigurationSection("npcs"))return;for(String id:getConfig().getConfigurationSection("npcs").getKeys(false))spawn(id);}
 private Method method(Class<?> c,String n,Class<?>...p){while(c!=null){try{Method m=c.getDeclaredMethod(n,p);m.setAccessible(true);return m;}catch(Throwable x){c=c.getSuperclass();}}return null;}
 private Field field(Class<?> c,String n){while(c!=null){try{Field f=c.getDeclaredField(n);f.setAccessible(true);return f;}catch(Throwable x){c=c.getSuperclass();}}return null;}
 private Location safe(Location b){World w=b.getWorld();int x=b.getBlockX(),z=b.getBlockZ();int y=Math.max(1,Math.min(w.getMaxHeight()-2,b.getBlockY()));for(int i=y+6;i>=1;i--)if(w.getBlockAt(x,i,z).isEmpty()&&w.getBlockAt(x,i+1,z).isEmpty()&&!w.getBlockAt(x,i-1,z).isEmpty()){Location n=b.clone();n.setY(i);return n;}return b.clone().add(0,1,0);}
 private void setPosition(Object n,Location l)throws Exception{
  Method m=method(n.getClass(),"setLocationAndAngles",double.class,double.class,double.class,float.class,float.class);
  if(m!=null){m.invoke(n,l.getX(),l.getY(),l.getZ(),l.getYaw(),l.getPitch());getLogger().info("NMS NPC POSITION API: setLocationAndAngles");return;}
  m=method(n.getClass(),"setPositionAndRotation",double.class,double.class,double.class,float.class,float.class);
  if(m!=null){m.invoke(n,l.getX(),l.getY(),l.getZ(),l.getYaw(),l.getPitch());getLogger().info("NMS NPC POSITION API: setPositionAndRotation");return;}
  Field px=field(n.getClass(),"posX"),py=field(n.getClass(),"posY"),pz=field(n.getClass(),"posZ"),ry=field(n.getClass(),"rotationYaw"),rp=field(n.getClass(),"rotationPitch");
  if(px==null||py==null||pz==null)throw new NoSuchMethodException("No position API or posX/posY/posZ fields found on "+n.getClass().getName());
  px.setDouble(n,l.getX());py.setDouble(n,l.getY());pz.setDouble(n,l.getZ());if(ry!=null)ry.setFloat(n,l.getYaw());if(rp!=null)rp.setFloat(n,l.getPitch());getLogger().info("NMS NPC POSITION API: direct entity position fields");
 }
 private boolean spawnNms(String id,Location l){try{
  Class<?> villager=Class.forName("net.minecraft.entity.passive.EntityVillager");Class<?> nw=Class.forName("net.minecraft.world.World");Class<?> ne=Class.forName("net.minecraft.entity.Entity");
  Object cw=l.getWorld();Method gh=method(cw.getClass(),"getHandle");if(gh==null)throw new NoSuchMethodException("getHandle on "+cw.getClass().getName());Object wh=gh.invoke(cw);getLogger().info("NMS NPC WORLD HANDLE: id="+id+", class="+wh.getClass().getName());
  Constructor<?> ctor=villager.getConstructor(nw);Object n=ctor.newInstance(wh);setPosition(n,l);
  Method spawn=method(wh.getClass(),"spawnEntityInWorld",ne);if(spawn==null)throw new NoSuchMethodException("spawnEntityInWorld on "+wh.getClass().getName());Object r=spawn.invoke(wh,n);if(r instanceof Boolean&&!((Boolean)r).booleanValue())throw new IllegalStateException("spawnEntityInWorld returned false");
  Method gb=method(n.getClass(),"getBukkitEntity");if(gb==null)throw new NoSuchMethodException("getBukkitEntity on "+n.getClass().getName());Entity e=(Entity)gb.invoke(n);if(e==null||e.isDead())throw new IllegalStateException("EntityVillager dead immediately after insertion");live.put(id,e);getLogger().info("NMS NPC SPAWN SUCCESS: id="+id+", entityId="+e.getEntityId());final Entity t=e;final String tid=id;getServer().getScheduler().scheduleSyncDelayedTask(this,new Runnable(){public void run(){if(t.isDead())getLogger().warning("NMS NPC POST-TICK DEAD: id="+tid+", entityId="+t.getEntityId());else getLogger().info("NMS NPC POST-TICK ALIVE: id="+tid+", entityId="+t.getEntityId());}});return true;
 }catch(Throwable t){getLogger().warning("NMS NPC SPAWN FAILED: id="+id+", "+t.getClass().getName()+": "+t.getMessage());return false;}}
 private boolean spawn(String id){String p="npcs."+id;World w=getServer().getWorld(getConfig().getString(p+".world"));if(w==null){getLogger().warning("NPC "+id+" references missing world.");return false;}Location s=new Location(w,getConfig().getDouble(p+".x"),getConfig().getDouble(p+".y"),getConfig().getDouble(p+".z"),(float)getConfig().getDouble(p+".yaw"),(float)getConfig().getDouble(p+".pitch"));Location l=safe(s);w.loadChunk(l.getBlockX()>>4,l.getBlockZ()>>4);Entity old=live.remove(id);if(old!=null&&!old.isDead())old.remove();getLogger().info("NMS NPC SPAWN ATTEMPT: id="+id+", saved="+s.getBlockX()+","+s.getBlockY()+","+s.getBlockZ()+", safe="+l.getBlockX()+","+l.getBlockY()+","+l.getBlockZ()+", below="+w.getBlockAt(l.getBlockX(),l.getBlockY()-1,l.getBlockZ()).getType().name());return spawnNms(id,l);}
 private String id(Entity e){for(Map.Entry<String,Entity>x:live.entrySet())if(x.getValue()!=null&&x.getValue().getEntityId()==e.getEntityId())return x.getKey();return null;}
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=false)public void damage(EntityDamageEvent e){if(getConfig().getBoolean("settings.protect-npcs",true)&&id(e.getEntity())!=null)e.setCancelled(true);}
 @EventHandler(priority=EventPriority.HIGHEST)public void interact(PlayerInteractEntityEvent e){String id=id(e.getRightClicked());if(id==null)return;e.setCancelled(true);Player p=e.getPlayer();String path="npcs."+id;long now=System.currentTimeMillis(),cd=getConfig().getLong("settings.interaction-cooldown-seconds",2)*1000L;String key=p.getName().toLowerCase()+":"+id;Long last=cooldowns.get(key);if(last!=null&&now-last<cd)return;cooldowns.put(key,Long.valueOf(now));for(String line:getConfig().getStringList(path+".lines"))p.sendMessage(color(line.replace("%player%",p.getName()).replace("%npc%",id)));for(String c:getConfig().getStringList(path+".commands")){c=c.replace("%player%",p.getName()).replace("%npc%",id);if(c.startsWith("/"))c=c.substring(1);if(c.length()>0)getServer().dispatchCommand(getServer().getConsoleSender(),c);}}
 @Override public boolean onCommand(CommandSender s,Command c,String label,String[] a){if(!s.hasPermission("irondominion.npcs.admin")){s.sendMessage(color("&cNo permission."));return true;}if(a.length==0||a[0].equalsIgnoreCase("list")){if(!getConfig().isConfigurationSection("npcs")){s.sendMessage("No NPCs configured.");return true;}for(String id:getConfig().getConfigurationSection("npcs").getKeys(false))s.sendMessage(color("&e- "+id));return true;}if(a[0].equalsIgnoreCase("version")){s.sendMessage(color("&6IronDominionNPCs &ev1.0.9 &7| native Forge/Cauldron 1.6.4"));return true;}if(a[0].equalsIgnoreCase("debug")){s.sendMessage(color("&6Plugin &e"+getDescription().getVersion()+" &6Configured &e"+(getConfig().isConfigurationSection("npcs")?getConfig().getConfigurationSection("npcs").getKeys(false).size():0)+" &6Live &e"+live.size()));return true;}if(a[0].equalsIgnoreCase("respawn")){respawnAll();s.sendMessage(color("&aNPCs respawned."));return true;}if(a[0].equalsIgnoreCase("reload")){reloadConfig();respawnAll();s.sendMessage(color("&aNPC configuration reloaded."));return true;}if(!(s instanceof Player)){s.sendMessage("Player required.");return true;}Player p=(Player)s;if(a[0].equalsIgnoreCase("create")&&a.length>1){String id=a[1].toLowerCase(),path="npcs."+id;Location l=p.getLocation();getConfig().set(path+".world",l.getWorld().getName());getConfig().set(path+".x",l.getX());getConfig().set(path+".y",l.getY());getConfig().set(path+".z",l.getZ());getConfig().set(path+".yaw",l.getYaw());getConfig().set(path+".pitch",l.getPitch());getConfig().set(path+".lines",java.util.Arrays.asList("&eWelcome to Iron Dominion!"));getConfig().set(path+".commands",java.util.Collections.emptyList());saveConfig();getLogger().info("NPC CREATE REQUEST: id="+id+" from="+p.getName());p.sendMessage(spawn(id)?color("&aCreated NPC &e"+id):color("&cNPC spawn failed; check console."));return true;}if(a[0].equalsIgnoreCase("remove")&&a.length>1){String id=a[1].toLowerCase();Entity e=live.remove(id);if(e!=null&&!e.isDead())e.remove();getConfig().set("npcs."+id,null);saveConfig();p.sendMessage(color("&aRemoved NPC &e"+id));return true;}return true;}
}