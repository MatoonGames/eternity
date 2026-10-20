package com.matoon.eternity.client.render;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/** Places the Living Tribunal in the world, and draws the wormhole he drags you through. */
@SideOnly(Side.CLIENT)
public final class TribunalRenderer {

    /** Blocks per model pixel on his throne: he sits some sixty blocks tall. */
    public static final float THRONE_SCALE = 1.5F;
    /** Blocks per model pixel as he looms out of nowhere. */
    public static final float LOOM_SCALE = 1.1F;
    /** Height of his eyes above his feet when standing, in model pixels. */
    public static final float LOOM_EYE = 41.5F;

    private static final ModelTribunal MODEL = new ModelTribunal();

    private static final Vec3d UP = new Vec3d(0, 1, 0);

    private TribunalRenderer() {
    }

    /** On his throne in the Star Chamber, looking down the aisle. */
    public static void seated(Vec3d rel, float eyes, float time) {
        GlStateManager.pushMatrix();
        GlStateManager.disableFog();
        GlStateManager.translate(rel.x, rel.y, rel.z);
        // Model space is Y down and faces -Z; flipping both stands him up facing +Z.
        GlStateManager.scale(THRONE_SCALE, -THRONE_SCALE, -THRONE_SCALE);
        MODEL.draw(ModelTribunal.Pose.SEATED, 0F, eyes, time);
        GlStateManager.popMatrix();
        GlStateManager.enableLighting();
    }

    /** Standing over you, filling the sky, reaching. Nothing in the world hides him. */
    public static void looming(Vec3d rel, float yaw, float reach, float time) {
        GlStateManager.pushMatrix();
        GlStateManager.disableFog();
        GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT);
        GlStateManager.translate(rel.x, rel.y, rel.z);
        GlStateManager.rotate(yaw, 0F, 1F, 0F);
        GlStateManager.scale(LOOM_SCALE, -LOOM_SCALE, -LOOM_SCALE);
        MODEL.draw(ModelTribunal.Pose.REACH, reach, 1F, time);
        GlStateManager.popMatrix();
        GlStateManager.enableLighting();
    }

    /**
     * A tunnel through the space between worlds, around the camera: walls of stars,
     * rings of light rushing past, and a blinding light at the far end.
     */
    public static void wormhole(Vec3d look, float strength, float time) {
        Vec3d forward = look.normalize();
        Vec3d right = forward.crossProduct(UP);
        right = right.lengthSquared() < 1.0E-6 ? new Vec3d(1, 0, 0) : right.normalize();
        Vec3d up = right.crossProduct(forward).normalize();

        GlStateManager.pushMatrix();
        GlStateManager.disableDepth();
        GlStateManager.disableFog();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.disableAlpha();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);

        Vec3d f = forward;
        Vec3d r = right;
        Vec3d u = up;
        Cosmic.draw(() -> tube(f, r, u, -40D, 150D, 7D), 12, strength, 2.2F * strength, 0.9F, 0.6F, 1F, false);

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        GlStateManager.depthMask(false);
        for (int i = 0; i < 36; i++) {
            float d = ((i * 4F - time * 3.5F) % 144F + 144F) % 144F - 6F;
            float hue = (i % 3) / 2F;
            float near = 1F - Math.max(0F, d) / 144F;
            ring(f, r, u, d, 6.3D + 0.5D * MathHelper.sin(i + time * 0.1F), 0.7D,
                    0.6F + 0.4F * hue, 0.45F + 0.3F * (1F - hue), 1F, 0.55F * strength * near);
        }
        streaks(f, r, u, strength, time);
        cap(f.scale(150D), r, u, 22D, 1F, 0.95F, 1F, strength);
        cap(f.scale(-40D), r, u, 14D, 0.35F, 0.15F, 0.6F, 0.6F * strength);

        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.depthMask(true);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.enableCull();
        GlStateManager.enableDepth();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }

    private static void tube(Vec3d f, Vec3d r, Vec3d u, double from, double to, double radius) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        int around = 24;
        int along = 10;
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        for (int i = 0; i < along; i++) {
            double d0 = from + (to - from) * i / along;
            double d1 = from + (to - from) * (i + 1) / along;
            for (int j = 0; j < around; j++) {
                double a0 = Math.PI * 2D * j / around;
                double a1 = Math.PI * 2D * (j + 1) / around;
                put(buffer, point(f, r, u, d0, a0, radius));
                put(buffer, point(f, r, u, d0, a1, radius));
                put(buffer, point(f, r, u, d1, a1, radius));
                put(buffer, point(f, r, u, d1, a0, radius));
            }
        }
        tessellator.draw();
    }

    private static void ring(Vec3d f, Vec3d r, Vec3d u, double d, double radius, double thickness,
                             float red, float green, float blue, float alpha) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (int j = 0; j <= 32; j++) {
            double a = Math.PI * 2D * j / 32D;
            Vec3d inner = point(f, r, u, d, a, radius - thickness);
            Vec3d outer = point(f, r, u, d, a, radius);
            buffer.pos(inner.x, inner.y, inner.z).color(red, green, blue, 0F).endVertex();
            buffer.pos(outer.x, outer.y, outer.z).color(red, green, blue, alpha).endVertex();
        }
        tessellator.draw();
    }

    /** Streaks of light racing along the walls toward you. */
    private static void streaks(Vec3d f, Vec3d r, Vec3d u, float strength, float time) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        GlStateManager.glLineWidth(2F);
        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        for (int k = 0; k < 60; k++) {
            double a = Math.PI * 2D * k / 60D + time * 0.015D;
            float d = ((k * 37F - time * 6F) % 150F + 150F) % 150F - 10F;
            double radius = 5.2D + (k % 4) * 0.4D;
            Vec3d head = point(f, r, u, d, a, radius);
            Vec3d tail = point(f, r, u, d + 14D, a, radius);
            buffer.pos(head.x, head.y, head.z).color(1F, 1F, 1F, 0.8F * strength).endVertex();
            buffer.pos(tail.x, tail.y, tail.z).color(0.6F, 0.5F, 1F, 0F).endVertex();
        }
        tessellator.draw();
        GlStateManager.glLineWidth(1F);
    }

    private static void cap(Vec3d c, Vec3d r, Vec3d u, double radius, float red, float green, float blue, float alpha) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(c.x, c.y, c.z).color(red, green, blue, alpha).endVertex();
        for (int j = 0; j <= 32; j++) {
            double a = Math.PI * 2D * j / 32D;
            Vec3d edge = c.add(r.scale(Math.cos(a) * radius)).add(u.scale(Math.sin(a) * radius));
            buffer.pos(edge.x, edge.y, edge.z).color(red, green, blue, 0F).endVertex();
        }
        tessellator.draw();
    }

    private static Vec3d point(Vec3d f, Vec3d r, Vec3d u, double d, double angle, double radius) {
        return f.scale(d).add(r.scale(Math.cos(angle) * radius)).add(u.scale(Math.sin(angle) * radius));
    }

    private static void put(BufferBuilder buffer, Vec3d v) {
        buffer.pos(v.x, v.y, v.z).endVertex();
    }
}
