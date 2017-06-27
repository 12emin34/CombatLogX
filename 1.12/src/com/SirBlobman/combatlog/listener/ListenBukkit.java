package com.SirBlobman.combatlog.listener;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.PluginManager;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;

import com.SirBlobman.combatlog.Combat;
import com.SirBlobman.combatlog.compat.CompatFactions;
import com.SirBlobman.combatlog.compat.CompatLegacyFactions;
import com.SirBlobman.combatlog.config.Config;
import com.SirBlobman.combatlog.listener.event.CombatEvent;
import com.SirBlobman.combatlog.listener.event.PlayerCombatEvent;
import com.SirBlobman.combatlog.listener.event.PlayerCombatLogEvent;
import com.SirBlobman.combatlog.utility.LegacyUtil;
import com.SirBlobman.combatlog.utility.Util;
import com.SirBlobman.combatlog.utility.WorldGuardUtil;

public class ListenBukkit implements Listener {
	private static final Server SERVER = Bukkit.getServer();
	private static final PluginManager PM = SERVER.getPluginManager();
	
	@EventHandler(priority=EventPriority.HIGHEST) 
	public void eve(EntityDamageByEntityEvent e){
		if(e.isCancelled()) return;
		Entity ed = e.getEntity();
		Entity er = e.getDamager();
		double dam = e.getDamage();
		List<String> worlds = Config.DISABLED_WORLDS;
		String world = ed.getWorld().getName();
		if(worlds.contains(world)) return;
		if(dam > 0.0D) {
			LivingEntity led = null;
			LivingEntity ler = null;
			if(ed instanceof LivingEntity) led = (LivingEntity) ed;
			if(er instanceof LivingEntity) ler = (LivingEntity) er;
			if(er instanceof Projectile) {
				if(e.getCause() == DamageCause.FALL) {ler = null;}
				else {
					Projectile p = (Projectile) er;
					ProjectileSource ps = p.getShooter();
					if(ps instanceof LivingEntity) ler = (LivingEntity) ps;
				}
			}
			if(ler != null && led != null) {
				boolean both = ((led instanceof Player) && (ler instanceof Player));
				if(!both && !Config.MOBS_COMBAT) return;
				if(ler instanceof Player) {
					Player p = (Player) ler;
					if(!canPVP(p)) return;
					if(p.equals(led) && !Config.SELF_COMBAT) return;
					PlayerCombatEvent pce = new PlayerCombatEvent(p, led, dam);
					PM.callEvent(pce);
				} else {
					CombatEvent ce = new CombatEvent(ler, led, dam);
					PM.callEvent(ce);
				}
				
				if(led instanceof Player) {
					Player p = (Player) led;
					if(!canPVP(p)) return;
					if(p.equals(ler) && !Config.SELF_COMBAT) return;
					PlayerCombatEvent pce = new PlayerCombatEvent(ler, p, dam);
					PM.callEvent(pce);
				}
			}
		}
	}
	
	@EventHandler(priority=EventPriority.HIGHEST)
	public void pvp(PlayerCombatEvent e) {
		if(e.isCancelled()) return;
		Player p = e.getPlayer();
		LivingEntity enemy = e.getEnemy();
		String ename = LegacyUtil.name(enemy);
		boolean attack = e.isPlayerAttacker();
		boolean enepla = (enemy instanceof Player);
		if(!checkPerm(p)) {
			if(!Combat.in(p)) {
				if(enepla) {
					if(attack) {
						String msg = Util.color(String.format(Config.MSG_PREFIX + Config.MSG_ATTACK, ename));
						p.sendMessage(msg);
					} else {
						String msg = Util.color(String.format(Config.MSG_PREFIX + Config.MSG_TARGET, ename));
						p.sendMessage(msg);
					}
				} else {
					if(attack) {
						String msg = Util.color(String.format(Config.MSG_PREFIX + Config.MSG_ATTACK_MOB, ename));
						p.sendMessage(msg);
					} else {
						String msg = Util.color(String.format(Config.MSG_PREFIX + Config.MSG_TARGET_MOB, ename));
						p.sendMessage(msg);
					}
				}
			}
			Combat.add(p, enemy);

			if(Config.REMOVE_POTIONS) {
				for(String s : Config.BANNED_POTIONS) {
					PotionEffectType pet = PotionEffectType.getByName(s);
					if(pet == null) {continue;}
					else {if(p.hasPotionEffect(pet)) p.removePotionEffect(pet);}
				}
			}
			
			if(Config.SUDO_ON_COMBAT) {
				for(String s : Config.COMBAT_COMMANDS) {
					String cmd = s.replace("{player}", p.getName());
					p.performCommand(cmd);
				}
			}

			if(Config.PREVENT_FLIGHT) {
				p.setFlying(false);
				p.setAllowFlight(false);
			}

			if(Config.CHANGE_GAMEMODE) {
				p.setGameMode(GameMode.SURVIVAL);
			}
		}
	}
	
	@EventHandler
	public void die(PlayerDeathEvent e) {
		Player p = e.getEntity();
		if(Combat.in(p)) Combat.remove(p);
	}
	
	@EventHandler
	public void inv(InventoryOpenEvent e) {
		if(Config.OPEN_INVENTORY) {
			HumanEntity he = e.getPlayer();
			if(he instanceof Player) {
				Player p = (Player) he;
				if(Combat.in(p)) {
					Inventory i = e.getInventory();
					InventoryType it = i.getType();
					if(it != InventoryType.PLAYER) {
						e.setCancelled(true);
						Util.msg(p, Config.MSG_INVENTORY);
					}
				}
			}
		}
	}
	
	@EventHandler
	public void cmd(PlayerCommandPreprocessEvent e) {
		Player p = e.getPlayer();
		if(Combat.in(p)) {
			String msg = e.getMessage();
			String[] command = msg.split(" ");
			String cm = command[0].toLowerCase();
			for(String cmd : Config.BLOCKED_COMMANDS) {
				if(cm.equals("/" + cmd)) {
					e.setCancelled(true);
					p.sendMessage(Util.color(Config.MSG_PREFIX + Util.format(Config.MSG_BLOCKED, cm)));
					break;
				}
			}
		}
	}
	
	@EventHandler(priority=EventPriority.HIGHEST)
	public void quit(PlayerQuitEvent e) {
		Player p = e.getPlayer();
		if(Combat.in(p)) {
			PlayerCombatLogEvent PCLE = new PlayerCombatLogEvent(p);
			PM.callEvent(PCLE);
			Combat.remove(p);
		}
	}
		
	@EventHandler(priority=EventPriority.LOWEST)
	public void kick(PlayerKickEvent e) {
		if(!Config.PUNISH_KICKED) {
			Player p = e.getPlayer();
			Combat.remove(p);
		}
	}
	
	@EventHandler
	public void quit(PlayerCombatLogEvent e) {
		Player p = e.getPlayer();
		if(Config.KILL_PLAYER) p.setHealth(0.0D);		
		if(Config.PUNISH_LOGGERS) {
			for(String s : Config.PUNISH_COMMANDS) {
				String cmd = s.replace("{player}", p.getName());
				ConsoleCommandSender ccs = Bukkit.getConsoleSender();
				Bukkit.dispatchCommand(ccs, cmd);
			}
		}
		if(Config.SUDO_LOGGERS) {
			for(String s : Config.SUDO_COMMANDS) {
				String cmd = s.replace("{player}", p.getName());
				p.performCommand(cmd);
			}
		}
		if(Config.QUIT_MESSAGE) {
			String msg = Util.format(Config.MSG_PREFIX + Config.MSG_QUIT, p.getName());
			Bukkit.broadcastMessage(msg);
		}
	}
	
	private boolean checkPerm(Player p) {
		String perm = "combatlog.bypass";
		if(Config.ENABLE_BYPASS) {
			boolean has = p.hasPermission(perm);
			return has;
		} else return false;
	}
	
	private boolean canPVP(Player p) {
		try {
			Location l = p.getLocation();
			boolean wg = WorldGuardUtil.canPvp(p);
			boolean to = ListenTowny.pvp(l);
			boolean fa = CompatFactions.canPVP(p);
			boolean lf = CompatLegacyFactions.canPVP(p);
			boolean pvp = (wg && to && fa && lf);
			return pvp;
		} catch(Throwable ex) {
			World w = p.getWorld();
			boolean pvp = w.getPVP();
			return pvp;
		}
	}
}