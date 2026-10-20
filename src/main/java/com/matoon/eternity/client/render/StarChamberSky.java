package com.matoon.eternity.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraftforge.client.IRenderHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.Random;

/** The sky over the Star Chamber: open space, torn with violet light, littered with shards of black. */
@SideOnly(Side.CLIENT)
public class StarChamberSky extends IRenderHandler {

    public static final StarChamberSky INSTANCE = new StarChamberSky();

    /** For clouds and weather, of which there are none. */
    public static final IRenderHandler NOTHING = new IRenderHandler() {
        @Override
        public void render(float partialTicks, WorldClient world, Minecraft mc) {
        }
    };

    @Override
    public void render(float partialTicks, WorldClient world, Minecraft mc) {
        float time = world.getTotalWorldTime() + partialTicks;
        GlStateManager.pushMatrix();
        GlStateManager.disableFog();
        GlStateManager.disableAlpha();
        GlStateManager.disableCull();
        GlStateManager.depthMask(false);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);

        Cosmic.draw(() -> FxRenderer.skySphere(100D), 14, 1F, 1.7F, 0.95F, 0.6F, 1F, false);

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        GlStateManager.depthMask(false);
        // Great rays of violet light fanning out behind the throne.
        Random random = new Random(4242L);
        for (int i = 0; i < 9; i++) {
            double spread = (i - 4) * 0.22D + (random.nextDouble() - 0.5D) * 0.08D;
            float pulse = 0.8F + 0.2F * (float) Math.sin(time * 0.01F + i);
            ray(spread, 0.15D + random.nextDouble() * 0.5D, 0.6F, 0.4F, 1F, (0.12F + random.nextFloat() * 0.1F) * pulse);
        }
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.enableAlpha();
        GlStateManager.popMatrix();
    }

    /** A long wedge of light from the horizon behind the throne, sweeping up across the sky. */
    private static void ray(double yaw, double pitch, float r, float g, float b, float alpha) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        double width = 0.05D;
        buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
        // The throne lies toward -Z; the rays rise from just above the horizon there.
        buffer.pos(0, 6, -90).color(r, g, b, alpha).endVertex();
        buffer.pos(Math.sin(yaw - width) * 90D, 90D * Math.sin(pitch + 0.9D), -Math.cos(yaw - width) * 40D).color(r, g, b, 0F).endVertex();
        buffer.pos(Math.sin(yaw + width) * 90D, 90D * Math.sin(pitch + 0.9D), -Math.cos(yaw + width) * 40D).color(r, g, b, 0F).endVertex();
        tessellator.draw();
    }
}
