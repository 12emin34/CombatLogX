package com.SirBlobman.expansion.cheatprevention.listener;

import com.SirBlobman.combatlogx.config.ConfigLang;
import com.SirBlobman.combatlogx.event.PlayerCombatTimerChangeEvent;
import com.SirBlobman.combatlogx.event.PlayerTagEvent;
import com.SirBlobman.combatlogx.event.PlayerTagEvent.TagReason;
import com.SirBlobman.combatlogx.event.PlayerTagEvent.TagType;
import com.SirBlobman.combatlogx.event.PlayerUntagEvent;
import com.SirBlobman.combatlogx.event.PlayerUntagEvent.UntagReason;
import com.SirBlobman.combatlogx.utility.CombatUtil;
import com.SirBlobman.combatlogx.utility.SchedulerUtil;
import com.SirBlobman.combatlogx.utility.Util;
import com.SirBlobman.expansion.cheatprevention.config.ConfigCheatPrevention;

import java.util.List;

import org.bukkit.GameMode;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;

public class ListenCheatPrevention implements Listener {
    private boolean isBlocked(String command) {
        List<String> commandList = ConfigCheatPrevention.BLOCKED_COMMANDS_LIST;
        if(ConfigCheatPrevention.BLOCKED_COMMANDS_IS_WHITELIST) {
            for(String inList : commandList) {
                if(command.startsWith(inList)) return false;
            }
            
            return true;
        }
        
        for(String inList : commandList) {
            if(command.startsWith(inList)) return true;
        }
        
        return false;
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        Player player = e.getPlayer();
        String message = e.getMessage();
        String[] split = message.split(" ");
        String cmd = split[0].toLowerCase();
        if (CombatUtil.isInCombat(player)) {
            if (cmd.startsWith("/cmi") && split.length > 1) {
                cmd = "/" + split[1].toLowerCase();
                if (cmd.contains(":")) {
                    String[] split1 = cmd.split(":");
                    cmd = split1[0].toLowerCase();
                }
            }
            
            boolean deny = isBlocked(cmd);
            if (deny) {
                e.setCancelled(true);
                List<String> keys = Util.newList("{command}");
                List<?> vals = Util.newList(cmd);
                String format = ConfigLang.getWithPrefix("messages.expansions.cheat prevention.command.not allowed");
                String error = Util.formatMessage(format, keys, vals);
                Util.sendMessage(player, error);
            }
        }
    }
    
    @EventHandler
    public void onUntag(PlayerUntagEvent e) {
        Player player = e.getPlayer();
        UntagReason reason = e.getUntagReason();
        SchedulerUtil.runLater(5L, () -> {
            if (reason == UntagReason.EXPIRE) {
                String perm = ConfigCheatPrevention.FLIGHT_ENABLE_PERMISSION;
                if (perm != null && !perm.isEmpty()) {
                    if (player.hasPermission(perm)) {
                        player.setAllowFlight(true);
                        player.setFlying(true);
                    }
                }
            }
        });
    }
    
    @EventHandler
    public void onChangeTimer(PlayerCombatTimerChangeEvent e) {
        Player player = e.getPlayer();
        
        if (ConfigCheatPrevention.GAMEMODE_CHANGE_WHEN_TAGGED) {
            GameMode pgm = player.getGameMode();
            String smode = ConfigCheatPrevention.GAMEMODE_GAMEMODE;
            GameMode gm = GameMode.valueOf(smode);
            if (pgm != gm) {
                player.setGameMode(gm);
                List<String> keys = Util.newList("{gamemode}");
                List<?> vals = Util.newList(gm.name());
                String format = ConfigLang.getWithPrefix("messages.expansions.cheat prevention.gamemode.change");
                String msg = Util.formatMessage(format, keys, vals);
                Util.sendMessage(player, msg);
            }
        }
        
        if (!ConfigCheatPrevention.FLIGHT_ALLOW_DURING_COMBAT) {
            if (player.isFlying() || player.getAllowFlight()) {
                player.setFlying(false);
                player.setAllowFlight(false);
                String msg = ConfigLang.getWithPrefix("messages.expansions.cheat prevention.flight.disabled");
                Util.sendMessage(player, msg);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onToggleFlight(PlayerToggleFlightEvent e) {
        Player player = e.getPlayer();
        if (!ConfigCheatPrevention.FLIGHT_ALLOW_DURING_COMBAT && CombatUtil.isInCombat(player)) {
            if (e.isFlying()) {
                e.setCancelled(true);
                player.setAllowFlight(false);
                player.setFlying(false);
                
                String error = ConfigLang.getWithPrefix("messages.expansions.cheat prevention.flight.not allowed");
                Util.sendMessage(player, error);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChangeGameMode(PlayerGameModeChangeEvent e) {
        Player player = e.getPlayer();
        if (ConfigCheatPrevention.GAMEMODE_CHANGE_WHEN_TAGGED && CombatUtil.isInCombat(player)) {
            GameMode pgm = e.getNewGameMode();
            String smode = ConfigCheatPrevention.GAMEMODE_GAMEMODE;
            GameMode gm = GameMode.valueOf(smode);
            if (pgm != gm) {
                e.setCancelled(true);
                player.setGameMode(gm);
                
                String error = ConfigLang.getWithPrefix("messages.expansions.cheat prevention.gamemode.not allowed");
                Util.sendMessage(player, error);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        Player player = e.getPlayer();
        if (CombatUtil.isInCombat(player)) {
            TeleportCause cause = e.getCause();
            String causeName = cause.name();
            if (!ConfigCheatPrevention.TELEPORTATION_ALLOW_DURING_COMBAT) {
                if(!ConfigCheatPrevention.TELEPORTATION_ALLOWED_CAUSES.contains(causeName)) {
                    e.setCancelled(true);
                    String error = ConfigLang.getWithPrefix("messages.expansions.cheat prevention.teleport.other.not allowed");
                    Util.sendMessage(player, error);
                }
            }
            
            if(cause == TeleportCause.ENDER_PEARL && ConfigCheatPrevention.TELEPORTATION_ENDER_PEARLS_RESTART_TIMER) {
                LivingEntity enemy = CombatUtil.getEnemy(player);
                CombatUtil.tag(player, enemy, TagType.UNKNOWN, TagReason.UNKNOWN);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTag(PlayerTagEvent e) {
        Player player = e.getPlayer();
        if (ConfigCheatPrevention.INVENTORY_CLOSE_ON_COMBAT) {
            InventoryView playerView = player.getOpenInventory();
            if(playerView != null) {
                Inventory top = playerView.getTopInventory();
                if(top != null) {
                    player.closeInventory();
                    String error = ConfigLang.getWithPrefix("messages.expansions.cheat prevention.inventory.closed");
                    Util.sendMessage(player, error);
                }
            }
        }
    }
    
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onOpenInventory(InventoryOpenEvent e) {
        HumanEntity he = e.getPlayer();
        if (he instanceof Player) {
            Player player = (Player) he;
            if (CombatUtil.isInCombat(player) && ConfigCheatPrevention.INVENTORY_PREVENT_OPENING) {
                e.setCancelled(true);
                String error = ConfigLang.getWithPrefix("messages.expansions.cheat prevention.inventory.not allowed");
                Util.sendMessage(player, error);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();
        if (CombatUtil.isInCombat(player) && !ConfigCheatPrevention.CHAT_ALLOW_DURING_COMBAT) {
            e.setCancelled(true);
            
            String error = ConfigLang.getWithPrefix("messages.expansions.cheat prevention.chat.not allowed");
            Util.sendMessage(player, error);
        }
    }
}