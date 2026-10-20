package com.matoon.eternity.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.Random;

/** Full-screen overlays: the gloom, the closing vignette, fractures in the glass of reality, the flash. */
@SideOnly(Side.CLIENT)
public final class ScreenFx {

    private static final ResourceLocation VIGNETTE = new ResourceLocation("textures/misc/vignette.png");
    private static final Random CRACKS = new Random();

    private ScreenFx() {
    }

    public static void render(double width, double height, float darkness, float vignette, float cracks, float flash,
                              float flashR, float flashG, float flashB, float time) {
        GlStateManager.disableDepth();
        GlStateManager.disableCull();
        GlStateManager.depthMask(false);
        GlStateManager.disableAlpha();
        GlStateManager.enableBlend();

        if (vignette > 0.01F) {
            Minecraft.getMinecraft().getTextureManager().bindTexture(VIGNETTE);
            GlStateManager.enableTexture2D();
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR,
                    GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            float v = MathHelper.clamp(vignette, 0F, 1F);
            GlStateManager.color(v, v, v, 1F);
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
            buffer.pos(0, height, -90).tex(0, 1).endVertex();
            buffer.pos(width, height, -90).tex(1, 1).endVertex();
            buffer.pos(width, 0, -90).tex(1, 0).endVertex();
            buffer.pos(0, 0, -90).tex(0, 0).endVertex();
            tessellator.draw();
        }

        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        if (darkness > 0.005F) {
            rect(width, height, 0.01F, 0F, 0.025F, MathHelper.clamp(darkness, 0F, 0.92F));
        }

        if (cracks > 0.01F) {
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            drawCracks(width, height, cracks, time);
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        }

        if (flash > 0.005F) {
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            rect(width, height, flashR, flashG, flashB, MathHelper.clamp(flash, 0F, 1F));
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        }

        GlStateManager.enableTexture2D();
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.enableAlpha();
        GlStateManager.enableCull();
        GlStateManager.color(1F, 1F, 1F, 1F);
    }

    /** Nothing. Absolute black, and a handful of stars going out one by one. */
    public static void renderVoid(double width, double height, float time, float fade) {
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.disableAlpha();
        GlStateManager.disableCull();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        rect(width, height, 0F, 0F, 0F, MathHelper.clamp(fade, 0F, 1F));
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        CRACKS.setSeed(4242L);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < 40; i++) {
            double x = (CRACKS.nextDouble() * width + time * (0.02D + CRACKS.nextDouble() * 0.05D)) % width;
            double y = CRACKS.nextDouble() * height;
            float life = 0.5F + 0.5F * MathHelper.sin(time * 0.01F + i * 1.7F);
            float a = 0.35F * life * life * life * fade;
            double s = 0.5D + CRACKS.nextDouble();
            buffer.pos(x - s, y + s, -90).color(0.7F, 0.65F, 1F, a).endVertex();
            buffer.pos(x + s, y + s, -90).color(0.7F, 0.65F, 1F, a).endVertex();
            buffer.pos(x + s, y - s, -90).color(0.7F, 0.65F, 1F, a).endVertex();
            buffer.pos(x - s, y - s, -90).color(0.7F, 0.65F, 1F, a).endVertex();
        }
        tessellator.draw();
        GlStateManager.enableTexture2D();
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.enableAlpha();
        GlStateManager.enableCull();
        GlStateManager.color(1F, 1F, 1F, 1F);
    }

    private static void rect(double width, double height, float r, float g, float b, float a) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(0, height, -90).color(r, g, b, a).endVertex();
        buffer.pos(width, height, -90).color(r, g, b, a).endVertex();
        buffer.pos(width, 0, -90).color(r, g, b, a).endVertex();
        buffer.pos(0, 0, -90).color(r, g, b, a).endVertex();
        tessellator.draw();
    }

    /** Fractures that start at the edges of the view and creep inward as {@code amount} grows. */
    private static void drawCracks(double width, double height, float amount, float time) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        CRACKS.setSeed(9001L);
        float flicker = 0.7F + 0.3F * MathHelper.sin(time * 1.7F);
        double diag = Math.sqrt(width * width + height * height);
        int count = 14;
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        for (int c = 0; c < count; c++) {
            // Start on a random edge and head roughly toward the centre.
            double x, y;
            switch (CRACKS.nextInt(4)) {
                case 0: x = CRACKS.nextDouble() * width; y = 0; break;
                case 1: x = CRACKS.nextDouble() * width; y = height; break;
                case 2: x = 0; y = CRACKS.nextDouble() * height; break;
                default: x = width; y = CRACKS.nextDouble() * height; break;
            }
            double angle = Math.atan2(height / 2 - y, width / 2 - x);
            int steps = 9;
            float reach = amount * steps * (0.6F + CRACKS.nextFloat() * 0.4F);
            for (int s = 0; s < steps; s++) {
                angle += (CRACKS.nextDouble() - 0.5D) * 1.2D;
                double length = diag * (0.025D + CRACKS.nextDouble() * 0.03D);
                double nx = x + Math.cos(angle) * length;
                double ny = y + Math.sin(angle) * length;
                if (s < reach) {
                    float t = Math.min(1F, reach - s);
                    double ex = x + (nx - x) * t, ey = y + (ny - y) * t;
                    float a = 0.85F * flicker * (1F - s / (float) steps);
                    segment(buffer, x, y, ex, ey, 3.5D, 0.55F, 0.25F, 1F, a * 0.35F);
                    segment(buffer, x, y, ex, ey, 0.9D, 1F, 0.92F, 1F, a);
                }
                x = nx;
                y = ny;
            }
        }
        tessellator.draw();
    }

    private static void segment(BufferBuilder buffer, double x0, double y0, double x1, double y1, double w,
                                float r, float g, float b, float a) {
        double dx = x1 - x0, dy = y1 - y0;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 1.0E-4) {
            return;
        }
        double nx = -dy / len * w, ny = dx / len * w;
        buffer.pos(x0 - nx, y0 - ny, -90).color(r, g, b, a).endVertex();
        buffer.pos(x1 - nx, y1 - ny, -90).color(r, g, b, a).endVertex();
        buffer.pos(x1 + nx, y1 + ny, -90).color(r, g, b, a).endVertex();
        buffer.pos(x0 + nx, y0 + ny, -90).color(r, g, b, a).endVertex();
    }
}
