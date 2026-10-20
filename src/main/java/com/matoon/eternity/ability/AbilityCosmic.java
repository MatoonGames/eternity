package com.matoon.eternity.ability;

import com.matoon.eternity.Eternity;
import lucraft.mods.lucraftcore.superpowers.abilities.AbilityAction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * One of Eternity's powers, granted by a charged Astral Regulator held in either hand.
 * Lucraft handles the key, the cooldown and the ability bar; subclasses only act.
 */
public abstract class AbilityCosmic extends AbilityAction {

    private final ResourceLocation icon;

    protected AbilityCosmic(EntityLivingBase entity, String name) {
        super(entity);
        this.icon = new ResourceLocation(Eternity.MODID, "textures/abilities/" + name + ".png");
    }

    @Override
    public boolean action() {
        if (entity.world.isRemote || !(entity instanceof EntityPlayer)) {
            return false;
        }
        return use();
    }

    /** Performs the power on the server. Returns false if it could not be used, so no cooldown starts. */
    protected abstract boolean use();

    @SideOnly(Side.CLIENT)
    @Override
    public void drawIcon(Minecraft mc, Gui gui, int x, int y) {
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.color(1F, 1F, 1F, 1F);
        mc.getTextureManager().bindTexture(icon);
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, 16, 16, 16, 16);
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }
}
