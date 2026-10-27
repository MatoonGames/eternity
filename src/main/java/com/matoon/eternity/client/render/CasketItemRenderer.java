package com.matoon.eternity.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Draws the Casket of Ancient Winters wherever an item is drawn: in hand, in the inventory, on the ground. */
@SideOnly(Side.CLIENT)
public class CasketItemRenderer extends TileEntityItemStackRenderer {

    @Override
    public void renderByItem(ItemStack stack, float partialTicks) {
        float time = (Minecraft.getSystemTime() % 3600000L) / 50F;
        GlStateManager.pushMatrix();
        GlStateManager.translate(0.5F, 0.16F, 0.5F);
        GlStateManager.scale(-0.7F, -0.7F, 0.7F);
        VaultTreasures.casket(time);
        GlStateManager.popMatrix();
    }
}
