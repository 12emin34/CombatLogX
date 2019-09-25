package com.SirBlobman.expansion.citizens;

import java.io.File;

import com.SirBlobman.api.nms.NMS_Handler;
import com.SirBlobman.combatlogx.expansion.CLXExpansion;
import com.SirBlobman.combatlogx.expansion.Expansions;
import com.SirBlobman.combatlogx.utility.PluginUtil;
import com.SirBlobman.expansion.citizens.config.ConfigCitizens;
import com.SirBlobman.expansion.citizens.listener.*;
import com.SirBlobman.expansion.citizens.trait.TraitCombatLogX;

public class CompatCitizens implements CLXExpansion {
    public static File FOLDER;

    @Override
    public String getUnlocalizedName() {
        return "CompatCitizens";
    }

    @Override
    public String getName() {
        return "Citizens Compatibility";
    }

    @Override
    public String getVersion() {
        return "14.26";
    }
    
    @Override
    public void enable() {
        if(!PluginUtil.isEnabled("Citizens")) {
            print("Citizens is not installed, automatically disabling...");
            Expansions.unloadExpansion(this);
            return;
        }

        FOLDER = getDataFolder();
        ConfigCitizens.load();

        TraitCombatLogX.onEnable();
        PluginUtil.regEvents(new ListenCombat(), new ListenCreateNPCs(this), new ListenPlayerJoin(), new ListenHandleNPCs());
        
        int minorVersion = NMS_Handler.getMinorVersion();
        if(minorVersion >= 11) PluginUtil.regEvents(new ListenTotemNPC());
    }
    
    @Override
    public void disable() {
        if(!PluginUtil.isEnabled("Citizens")) return;
        TraitCombatLogX.onDisable();
    }
    
    @Override
    public void onConfigReload() {
        if(!PluginUtil.isEnabled("Citizens")) return;
        
        ConfigCitizens.load();
    }
}