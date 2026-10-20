package com.matoon.eternity.client.render;

import com.matoon.eternity.client.Rift;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.Collection;

/**
 * Draws rifts at any distance and through anything. Beyond the far plane a rift is
 * pulled in and shrunk in proportion, keeping its direction and apparent size; a
 * minimum angular size and line width keep even the most distant crack visible.
 */
@SideOnly(Side.CLIENT)
public final class RiftRenderer {

    private RiftRenderer() {
    }

    /** Pulls a far point in to the render limit. Returns {dx, dy, dz, scale}. */
    public static double[] project(double dx, double dy, double dz, double size) {
        double limit = farPlane() * 1.15D;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist <= limit) {
            return new double[]{dx, dy, dz, 1D};
        }
        double k = limit / dist;
        double scale = Math.max(k, limit * 0.05D / size);
        return new double[]{dx * k, dy * k, dz * k, scale};
    }

    public static double farPlane() {
        return Minecraft.getMinecraft().gameSettings.renderDistanceChunks * 16D;
    }

    public static void renderAll(Collection<Rift> rifts, Vec3d camera, float partialTicks) {
        if (rifts.isEmpty()) {
            return;
        }
        GlStateManager.pushMatrix();
        GlStateManager.disableFog();
        GlStateManager.disableCull();
        GlStateManager.disableLighting();
        for (Rift rift : rifts) {
            float open = rift.openness(partialTicks);
            if (open > 0.001F) {
                if (rift.seenThroughWorld()) {
                    GlStateManager.disableDepth();
                }
                render(rift, camera, open, partialTicks);
                GlStateManager.enableDepth();
            }
        }
        GlStateManager.enableCull();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }

    private static void render(Rift rift, Vec3d camera, float open, float partialTicks) {
        double[] p = project(rift.x - camera.x, rift.y - camera.y, rift.z - camera.z, rift.height);
        double unit = rift.height * 0.5D * p[3];
        double hx = p[0], hz = p[2];
        double len = Math.sqrt(hx * hx + hz * hz);
        double rx = len < 1.0E-4 ? 1D : -hz / len;
        double rz = len < 1.0E-4 ? 0D : hx / len;
        Plane plane = new Plane(p[0], p[1], p[2], rx * unit, rz * unit, unit);
        // Lines never get thinner than about a pixel, however far away.
        double viewDist = Math.sqrt(p[0] * p[0] + p[1] * p[1] + p[2] * p[2]);
        double minLine = viewDist * 0.0012D / unit;

        float time = (Minecraft.getMinecraft().player == null ? 0 : Minecraft.getMinecraft().player.ticksExisted) + partialTicks;
        float flicker = 0.78F + 0.22F * MathHelper.sin(time * 0.9F + rift.id) * MathHelper.sin(time * 0.37F + rift.id * 3);
        boolean far = p[3] < 1D;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        additive();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);

        // A faint bloom of violet light around the wound.
        buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        plane.vertex(buffer, rift.centre[Rift.SEGMENTS / 2], 0).color(0.5F, 0.2F, 1F, 0.28F * open * flicker).endVertex();
        for (int i = 0; i <= 32; i++) {
            double a = Math.PI * 2D * i / 32D;
            plane.vertex(buffer, Math.cos(a) * 0.45D, Math.sin(a) * 1.1D).color(0.2F, 0F, 0.5F, 0F).endVertex();
        }
        tessellator.draw();

        // Fractures racing across the sky.
        drawFractures(rift, plane, open, flicker, Math.max(0.012D, minLine * 4D), 0.55F, 0.25F, 1F, 0.3F);
        drawFractures(rift, plane, open, flicker, Math.max(0.0028D, minLine), 1F, 0.93F, 1F, 0.95F);

        // The split itself, opening onto the void.
        GlStateManager.shadeModel(GL11.GL_FLAT);
        double widthFloor = minLine * 1.5D;
        Cosmic.draw(() -> {
            buffer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION);
            for (int i = 0; i <= Rift.SEGMENTS; i++) {
                double v = -1D + 2D * i / Rift.SEGMENTS;
                double w = Math.max(widthFloor, rift.half[i] * open);
                plane.vertex(buffer, rift.centre[i] - w, v * open).endVertex();
                plane.vertex(buffer, rift.centre[i] + w, v * open).endVertex();
            }
            tessellator.draw();
        }, far ? 7 : 14, 1F, 1.7F, 1F, 0.75F, 1F, false);

        // Burning edges.
        additive();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        for (int side = -1; side <= 1; side += 2) {
            drawEdge(rift, plane, open, side, Math.max(0.035D, minLine * 6D), widthFloor, 0.5F, 0.15F, 1F, 0.5F * flicker);
            drawEdge(rift, plane, open, side, Math.max(0.008D, minLine * 1.5D), widthFloor, 1F, 0.95F, 1F, 1F);
        }

        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.depthMask(true);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
    }

    private static void additive() {
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        GlStateManager.depthMask(false);
    }

    private static void drawEdge(Rift rift, Plane plane, float open, int side, double width, double floor,
                                 float r, float g, float b, float alpha) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i <= Rift.SEGMENTS; i++) {
            double v = (-1D + 2D * i / Rift.SEGMENTS) * open;
            double edge = rift.centre[i] + side * Math.max(floor, rift.half[i] * open);
            float a = alpha * (0.5F + 0.5F * (float) Math.sqrt(Math.max(0D, 1D - v * v)));
            plane.vertex(buffer, edge, v).color(r, g, b, a * open).endVertex();
            plane.vertex(buffer, edge + side * width, v).color(r * 0.4F, g * 0.1F, b, 0F).endVertex();
        }
        tessellator.draw();
    }

    private static void drawFractures(Rift rift, Plane plane, float open, float flicker, double width,
                                      float r, float g, float b, float alpha) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        for (int f = 0; f < rift.fractures.size(); f++) {
            float[] line = rift.fractures.get(f);
            float delay = rift.fractureDelay.get(f);
            float grow = MathHelper.clamp((open - delay) / (1F - delay), 0F, 1F);
            int points = line.length / 2;
            float reach = grow * (points - 1);
            for (int s = 0; s < points - 1 && s < reach; s++) {
                float t = Math.min(1F, reach - s);
                double u0 = line[s * 2], v0 = line[s * 2 + 1];
                double u1 = u0 + (line[s * 2 + 2] - u0) * t;
                double v1 = v0 + (line[s * 2 + 3] - v0) * t;
                double du = u1 - u0, dv = v1 - v0;
                double l = Math.sqrt(du * du + dv * dv);
                if (l < 1.0E-6) {
                    continue;
                }
                double taper0 = 1D - (double) s / points, taper1 = 1D - (double) (s + 1) / points;
                double nu0 = -dv / l * width * taper0, nv0 = du / l * width * taper0;
                double nu1 = -dv / l * width * taper1, nv1 = du / l * width * taper1;
                float a0 = alpha * flicker * (float) taper0;
                float a1 = alpha * flicker * (float) taper1;
                plane.vertex(buffer, u0 - nu0, v0 - nv0).color(r, g, b, a0).endVertex();
                plane.vertex(buffer, u0 + nu0, v0 + nv0).color(r, g, b, a0).endVertex();
                plane.vertex(buffer, u1 + nu1, v1 + nv1).color(r, g, b, a1).endVertex();
                plane.vertex(buffer, u1 - nu1, v1 - nv1).color(r, g, b, a1).endVertex();
            }
        }
        tessellator.draw();
    }

    /** Maps rift-local (u, v) onto a vertical plane turned toward the viewer. */
    private static final class Plane {
        final double cx, cy, cz, rx, rz, up;

        Plane(double cx, double cy, double cz, double rx, double rz, double up) {
            this.cx = cx;
            this.cy = cy;
            this.cz = cz;
            this.rx = rx;
            this.rz = rz;
            this.up = up;
        }

        BufferBuilder vertex(BufferBuilder buffer, double u, double v) {
            return buffer.pos(cx + rx * u, cy + up * v, cz + rz * u);
        }
    }
}
