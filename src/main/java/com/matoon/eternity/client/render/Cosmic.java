package com.matoon.eternity.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;
import java.util.Random;

/**
 * Renders arbitrary geometry as a window into space, using the same screen-space
 * texture generation as the vanilla end portal. The stars do not move with the
 * surface; they stay fixed to the view, so whatever is drawn seems cut out of the
 * world, opening onto the void behind it.
 */
@SideOnly(Side.CLIENT)
public final class Cosmic {

    private static final ResourceLocation END_SKY = new ResourceLocation("textures/environment/end_sky.png");
    private static final ResourceLocation END_PORTAL = new ResourceLocation("textures/entity/end_portal.png");

    private static final FloatBuffer MODELVIEW = GLAllocation.createDirectFloatBuffer(16);
    private static final FloatBuffer PROJECTION = GLAllocation.createDirectFloatBuffer(16);
    private static final FloatBuffer PLANE = GLAllocation.createDirectFloatBuffer(16);
    private static final Random RANDOM = new Random();

    private Cosmic() {
    }

    /**
     * @param geometry   draws the surface; called once per layer and must not touch texture or colour state
     * @param layers     number of star layers (1-16); fewer for distant things
     * @param base       opacity of the dark first layer (0 for stars only)
     * @param stars      brightness of the additive star layers
     * @param writeDepth whether the dark base layer writes depth
     */
    public static void draw(Runnable geometry, int layers, float base, float stars, float r, float g, float b, boolean writeDepth) {
        Minecraft mc = Minecraft.getMinecraft();
        RANDOM.setSeed(31100L);
        GlStateManager.getFloat(GL11.GL_MODELVIEW_MATRIX, MODELVIEW);
        GlStateManager.getFloat(GL11.GL_PROJECTION_MATRIX, PROJECTION);
        GlStateManager.disableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);

        float time = (float) (Minecraft.getSystemTime() % 800000L) / 800000F;
        for (int i = 0; i < layers; i++) {
            float shade = 2F / (18 - i);
            if (i == 0) {
                mc.getTextureManager().bindTexture(END_SKY);
                shade = 0.15F;
                GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                GlStateManager.depthMask(writeDepth);
            } else {
                mc.getTextureManager().bindTexture(END_PORTAL);
                GlStateManager.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
                GlStateManager.depthMask(false);
            }

            GlStateManager.texGen(GlStateManager.TexGen.S, GL11.GL_EYE_LINEAR);
            GlStateManager.texGen(GlStateManager.TexGen.T, GL11.GL_EYE_LINEAR);
            GlStateManager.texGen(GlStateManager.TexGen.R, GL11.GL_EYE_LINEAR);
            GlStateManager.texGen(GlStateManager.TexGen.S, GL11.GL_EYE_PLANE, plane(1F, 0F, 0F, 0F));
            GlStateManager.texGen(GlStateManager.TexGen.T, GL11.GL_EYE_PLANE, plane(0F, 1F, 0F, 0F));
            GlStateManager.texGen(GlStateManager.TexGen.R, GL11.GL_EYE_PLANE, plane(0F, 0F, 1F, 0F));
            GlStateManager.enableTexGenCoord(GlStateManager.TexGen.S);
            GlStateManager.enableTexGenCoord(GlStateManager.TexGen.T);
            GlStateManager.enableTexGenCoord(GlStateManager.TexGen.R);

            GlStateManager.matrixMode(GL11.GL_TEXTURE);
            GlStateManager.pushMatrix();
            GlStateManager.loadIdentity();
            GlStateManager.translate(0.5F, 0.5F, 0F);
            GlStateManager.scale(0.5F, 0.5F, 1F);
            float n = i + 1;
            GlStateManager.translate(17F / n, (2F + n / 1.5F) * time, 0F);
            GlStateManager.rotate((n * n * 4321F + n * 9F) * 2F, 0F, 0F, 1F);
            GlStateManager.scale(4.5F - n / 4F, 4.5F - n / 4F, 1F);
            GlStateManager.multMatrix(PROJECTION);
            GlStateManager.multMatrix(MODELVIEW);
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);

            float cr = (RANDOM.nextFloat() * 0.5F + 0.1F) * shade;
            float cg = (RANDOM.nextFloat() * 0.5F + 0.4F) * shade;
            float cb = (RANDOM.nextFloat() * 0.5F + 0.5F) * shade;
            if (i == 0) {
                GlStateManager.color(cr, cg, cb, base);
            } else {
                GlStateManager.color(cr * r * stars, cg * g * stars, cb * b * stars, 1F);
            }
            if (i > 0 || base > 0F) {
                geometry.run();
            }

            GlStateManager.matrixMode(GL11.GL_TEXTURE);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        }

        GlStateManager.disableTexGenCoord(GlStateManager.TexGen.S);
        GlStateManager.disableTexGenCoord(GlStateManager.TexGen.T);
        GlStateManager.disableTexGenCoord(GlStateManager.TexGen.R);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.depthMask(true);
        GlStateManager.enableAlpha();
        GlStateManager.color(1F, 1F, 1F, 1F);
    }

    private static FloatBuffer plane(float a, float b, float c, float d) {
        PLANE.clear();
        PLANE.put(a).put(b).put(c).put(d);
        PLANE.flip();
        return PLANE;
    }
}
