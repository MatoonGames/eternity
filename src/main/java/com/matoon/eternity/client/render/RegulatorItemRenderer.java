package com.matoon.eternity.client.render;

import com.matoon.eternity.item.ItemAstralRegulator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Draws the Astral Regulator wherever an item is drawn: in hand, in the inventory, on
 * the ground, and as Lucraft's indestructible item entity when dropped.
 */
@SideOnly(Side.CLIENT)
public class RegulatorItemRenderer extends TileEntityItemStackRenderer {

    @Override
    public void renderByItem(ItemStack stack) {
        float time = (Minecraft.getSystemTime() % 3600000L) / 50F;
        GlStateManager.pushMatrix();
        GlStateManager.translate(0.5F, 0.5F, 0.5F);
        GlStateManager.rotate(time * 1.2F, 0F, 1F, 0F);
        GlStateManager.rotate(12F, 1F, 0F, 0F);
        // Holding the Living Tribunal, it has evolved: gold within, and his power circling it.
        boolean tribunal = ItemAstralRegulator.absorbed(stack).contains(ItemAstralRegulator.TRIBUNAL);
        OrbRenderer.draw(0.42F, time, ItemAstralRegulator.isCharged(stack) ? 1F : 0F, true, tribunal);
        GlStateManager.popMatrix();
    }
}
