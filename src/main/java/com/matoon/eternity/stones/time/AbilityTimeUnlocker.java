package com.matoon.eternity.stones.time;

import com.matoon.eternity.Eternity;
import lucraft.mods.lucraftcore.superpowers.abilities.AbilityToggle;
import me.guichaguri.tickratechanger.api.TickrateAPI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The Time Stone's holder stepping outside the time it bends: while on, their own game runs at the natural rate
 * whatever their dimension's is.
 * <p>
 * Tickrate Changer's client rate is one per game, so this only changes how their own game runs; others still see them
 * at the server's pace. It only re-sends when their dimension's rate changes, not every tick.
 */
public class AbilityTimeUnlocker extends AbilityToggle {

    private static final ResourceLocation LOCKED = new ResourceLocation(Eternity.MODID, "textures/abilities/time_locked.png");
    private static final ResourceLocation UNLOCKED = new ResourceLocation(Eternity.MODID, "textures/abilities/time_unlocked.png");

    private float lastSeen = -1F;

    public AbilityTimeUnlocker(EntityLivingBase entity) {
        super(entity);
    }

    @Override
    public void firstTick() {
        if (entity instanceof EntityPlayer && !entity.world.isRemote) {
            TickrateAPI.changeClientTickrate((EntityPlayer) entity, TimeStoneRates.normal());
            lastSeen = TimeStoneRates.getRate(entity.dimension);
        }
    }

    @Override
    public void updateTick() {
        if (!isEnabled() || !(entity instanceof EntityPlayer) || entity.world.isRemote) {
            return;
        }
        float rate = TimeStoneRates.getRate(entity.dimension);
        if (rate != lastSeen) {
            TickrateAPI.changeClientTickrate((EntityPlayer) entity, TimeStoneRates.normal());
            lastSeen = rate;
        }
    }

    @Override
    public void lastTick() {
        if (entity instanceof EntityPlayer && !entity.world.isRemote) {
            TickrateAPI.changeClientTickrate((EntityPlayer) entity, TimeStoneRates.getRate(entity.dimension));
            lastSeen = -1F;
        }
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void drawIcon(Minecraft mc, Gui gui, int x, int y) {
        AbilityBendTime.drawTexture(mc, isEnabled() ? UNLOCKED : LOCKED, x, y);
    }
}
