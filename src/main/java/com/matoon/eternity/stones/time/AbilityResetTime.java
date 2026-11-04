package com.matoon.eternity.stones.time;

import com.matoon.eternity.Eternity;
import com.matoon.eternity.EternitySounds;
import lucraft.mods.lucraftcore.superpowers.abilities.AbilityAction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** The Time Stone letting time in its holder's dimension flow normally again. */
public class AbilityResetTime extends AbilityAction {

    private static final ResourceLocation ICON = new ResourceLocation(Eternity.MODID, "textures/abilities/reset_time.png");

    public AbilityResetTime(EntityLivingBase entity) {
        super(entity);
    }

    @Override
    public boolean action() {
        if (!(entity instanceof EntityPlayerMP)) {
            return false;
        }
        EntityPlayerMP player = (EntityPlayerMP) entity;
        TimeStoneRates.resetRate(player, player.dimension);
        player.world.playSound(null, player.posX, player.posY, player.posZ, EternitySounds.STONES_TIME, SoundCategory.PLAYERS, 1F, 1F);
        return true;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void drawIcon(Minecraft mc, Gui gui, int x, int y) {
        AbilityBendTime.drawTexture(mc, ICON, x, y);
    }
}
