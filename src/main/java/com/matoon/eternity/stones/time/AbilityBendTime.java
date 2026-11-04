package com.matoon.eternity.stones.time;

import com.matoon.eternity.Eternity;
import com.matoon.eternity.EternitySounds;
import lucraft.mods.lucraftcore.superpowers.abilities.AbilityAction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The Time Stone slowing or speeding time in its holder's dimension by a step each press, as far as the charge allows
 * (see {@link TimeStoneCharge}), whose bar it draws along the bottom of its icon.
 */
public abstract class AbilityBendTime extends AbilityAction {

    private static final float STEP = 5F;

    private final ResourceLocation icon;
    private final float direction, limit;

    protected AbilityBendTime(EntityLivingBase entity, String icon, float direction, float limit) {
        super(entity);
        this.icon = new ResourceLocation(Eternity.MODID, "textures/abilities/" + icon + ".png");
        this.direction = direction;
        this.limit = limit;
    }

    @Override
    public boolean action() {
        if (!(entity instanceof EntityPlayerMP)) {
            return false;
        }
        EntityPlayerMP player = (EntityPlayerMP) entity;
        if (TimeStoneCharge.refuse(player)) {
            return false;
        }
        float next = TimeStoneRates.getRate(player.dimension) + STEP * direction;
        TimeStoneRates.applyRate(player, player.dimension, direction < 0F ? Math.max(limit, next) : Math.min(limit, next));
        player.world.playSound(null, player.posX, player.posY, player.posZ, EternitySounds.STONES_TIME, SoundCategory.PLAYERS, 1F, 1F);
        return true;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void drawIcon(Minecraft mc, Gui gui, int x, int y) {
        drawTexture(mc, icon, x, y);
        float charge = TimeStoneCharge.clientCharge();
        Gui.drawRect(x, y + 14, x + 16, y + 16, 0xFF000000);
        int width = (int) (charge * 16);
        if (width > 0) {
            Gui.drawRect(x, y + 14, x + width, y + 16, charge > 0.5F ? 0xFF00FF00 : charge > 0.25F ? 0xFFFFFF00 : 0xFFFF4400);
        }
    }

    @SideOnly(Side.CLIENT)
    static void drawTexture(Minecraft mc, ResourceLocation texture, int x, int y) {
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.color(1F, 1F, 1F, 1F);
        mc.getTextureManager().bindTexture(texture);
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, 16, 16, 16, 16);
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }
}
