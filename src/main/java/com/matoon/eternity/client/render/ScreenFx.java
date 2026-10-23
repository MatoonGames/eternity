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
    /** The colours a view goes as it breaks up: magenta, cyan, black, blood. */
    private static final float[][] CORRUPT = {{0.9F, 0.1F, 0.7F}, {0.1F, 0.9F, 0.95F}, {0F, 0F, 0F}, {0.6F, 0F, 0.1F}};

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

    /**
     * The view coming apart as the abyss takes you: gloom closing in from the edges, torn
     * bands of colour sliding across it, static, and squares of it going out one by one,
     * edges first, until nothing is left. With {@code gentle} (reduceFlashing) the pattern
     * changes slowly instead of seething.
     */
    public static void renderCorruption(double width, double height, float amount, float time, boolean gentle) {
        float a = MathHelper.clamp(amount, 0F, 1F);
        render(width, height, a * 0.35F, a * 1.2F, 0F, 0F, 0F, 0F, 0F, time);

        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.disableAlpha();
        GlStateManager.disableCull();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        int frame = (int) (time / (gentle ? 24F : 3F));
        CRACKS.setSeed(frame * 341873128712L + 77L);
        float calm = gentle ? 0.5F : 1F;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        int bands = 2 + (int) (14F * a);
        for (int i = 0; i < bands; i++) {
            double y = CRACKS.nextDouble() * height;
            double h = 1D + CRACKS.nextDouble() * (3D + 10D * a);
            double x0 = CRACKS.nextBoolean() ? 0D : CRACKS.nextDouble() * width;
            double x1 = x0 + width * (0.2D + CRACKS.nextDouble() * 0.8D);
            float[] c = CORRUPT[CRACKS.nextInt(CORRUPT.length)];
            quad(buffer, x0, y, x1, y + h, c[0], c[1], c[2], (0.15F + 0.45F * a) * calm);
        }

        int grains = (int) (400F * a * a);
        for (int i = 0; i < grains; i++) {
            double x = CRACKS.nextDouble() * width, y = CRACKS.nextDouble() * height;
            double s = 0.5D + CRACKS.nextDouble() * 1.5D;
            float g = CRACKS.nextFloat();
            quad(buffer, x, y, x + s, y + s, g, g * 0.8F, g, (0.1F + 0.4F * a) * calm);
        }

        // Going out: every square has its moment, the edges' first, and it comes as the abyss closes over you.
        int cols = 32, rows = Math.max(1, (int) Math.round(cols * height / width));
        double cw = width / cols, ch = height / rows;
        float gone = a * a * 1.15F - 0.1F;
        for (int i = 0; i < cols; i++) {
            for (int j = 0; j < rows; j++) {
                int h = i * 73856093 ^ j * 19349663;
                h ^= h >>> 13;
                h *= 0x5bd1e995;
                h ^= h >>> 15;
                double dx = (i + 0.5D) / cols - 0.5D, dy = (j + 0.5D) / rows - 0.5D;
                double centre = 1D - Math.min(1D, Math.sqrt(dx * dx + dy * dy) * 1.5D);
                double k = 0.55D * ((h >>> 8) & 0xFFFF) / 65536D + 0.45D * centre;
                if (k < gone) {
                    quad(buffer, i * cw, j * ch, (i + 1) * cw, (j + 1) * ch, 0F, 0F, 0F, 1F);
                } else if (k < gone + 0.06D) {
                    float[] c = CORRUPT[Math.floorMod(h + frame, CORRUPT.length)];
                    quad(buffer, i * cw, j * ch, (i + 1) * cw, (j + 1) * ch, c[0] * 0.6F, c[1] * 0.6F, c[2] * 0.6F, 0.75F);
                }
            }
        }
        tessellator.draw();

        GlStateManager.enableTexture2D();
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.enableAlpha();
        GlStateManager.enableCull();
        GlStateManager.color(1F, 1F, 1F, 1F);
        if (a > 0.85F) {
            // Oblivion.
            renderVoid(width, height, time, (a - 0.85F) / 0.15F);
        }
    }

    private static void quad(BufferBuilder buffer, double x0, double y0, double x1, double y1, float r, float g, float b, float a) {
        buffer.pos(x0, y1, -90).color(r, g, b, a).endVertex();
        buffer.pos(x1, y1, -90).color(r, g, b, a).endVertex();
        buffer.pos(x1, y0, -90).color(r, g, b, a).endVertex();
        buffer.pos(x0, y0, -90).color(r, g, b, a).endVertex();
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
