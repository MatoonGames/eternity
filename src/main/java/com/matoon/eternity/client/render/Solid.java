package com.matoon.eternity.client.render;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * Solid shapes for relics built in code: boxes and bent bars of metal, textured by where they
 * are in space so the grain runs on across every piece, and shaded by a light of their own
 * from above and in front, so they read the same in a slot, in a hand and on a pedestal. The
 * light of the place they are in comes on top of that through the lightmap.
 */
@SideOnly(Side.CLIENT)
final class Solid {

    private static final double[] LIGHT = normalize(-0.45D, 0.8D, 0.55D);

    private static BufferBuilder buffer;
    private static double scale = 1D;
    private static float r = 1F, g = 1F, b = 1F, a = 1F;

    private Solid() {
    }

    /** Starts a batch: texture coordinates are position times {@code texScale}. */
    static void begin(double texScale) {
        scale = texScale;
        buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
    }

    static void end() {
        Tessellator.getInstance().draw();
    }

    static void color(float red, float green, float blue) {
        color(red, green, blue, 1F);
    }

    static void color(float red, float green, float blue, float alpha) {
        r = red;
        g = green;
        b = blue;
        a = alpha;
    }

    /** How brightly a face turned this way is lit. */
    static float shade(double nx, double ny, double nz) {
        double d = nx * LIGHT[0] + ny * LIGHT[1] + nz * LIGHT[2];
        return (float) (0.42D + 0.58D * Math.max(0D, d) + 0.08D * Math.max(0D, -nz));
    }

    /** An axis-aligned box. */
    static void box(double x0, double y0, double z0, double x1, double y1, double z1) {
        quad(x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, 0, 1, 0);
        quad(x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, 0, -1, 0);
        quad(x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, 0, 0, 1);
        quad(x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, 0, 0, -1);
        quad(x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, 1, 0, 0);
        quad(x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, -1, 0, 0);
    }

    /** A box centred on (x, y, z), w by h by d. */
    static void boxAt(double x, double y, double z, double w, double h, double d) {
        box(x - w / 2D, y - h / 2D, z - d / 2D, x + w / 2D, y + h / 2D, z + d / 2D);
    }

    /** A quad facing along (nx, ny, nz), wound counter-clockwise seen from outside. */
    static void quad(double x0, double y0, double z0, double x1, double y1, double z1, double x2, double y2, double z2,
                     double x3, double y3, double z3, double nx, double ny, double nz) {
        float s = shade(nx, ny, nz);
        vertex(x0, y0, z0, nx, ny, nz, s);
        vertex(x1, y1, z1, nx, ny, nz, s);
        vertex(x2, y2, z2, nx, ny, nz, s);
        vertex(x3, y3, z3, nx, ny, nz, s);
    }

    /**
     * A flat face from {@code o} along {@code u} and {@code v}, facing along (nx, ny, nz), with
     * the texture laid across it from (u0, v0) to (u1, v1) rather than by where it is.
     */
    static void face(double[] o, double[] u, double[] v, double nx, double ny, double nz, float u0, float v0, float u1, float v1) {
        float s = shade(nx, ny, nz);
        buffer.pos(o[0], o[1], o[2]).tex(u0, v1).color(r * s, g * s, b * s, a).endVertex();
        buffer.pos(o[0] + u[0], o[1] + u[1], o[2] + u[2]).tex(u1, v1).color(r * s, g * s, b * s, a).endVertex();
        buffer.pos(o[0] + u[0] + v[0], o[1] + u[1] + v[1], o[2] + u[2] + v[2]).tex(u1, v0).color(r * s, g * s, b * s, a).endVertex();
        buffer.pos(o[0] + v[0], o[1] + v[1], o[2] + v[2]).tex(u0, v0).color(r * s, g * s, b * s, a).endVertex();
    }

    private static void vertex(double x, double y, double z, double nx, double ny, double nz, float s) {
        // Texture by the two axes the face lies across.
        double u, v;
        if (Math.abs(nx) > Math.abs(ny) && Math.abs(nx) > Math.abs(nz)) {
            u = z;
            v = y;
        } else if (Math.abs(ny) > Math.abs(nz)) {
            u = x;
            v = z;
        } else {
            u = x;
            v = y;
        }
        buffer.pos(x, y, z).tex(u * scale, v * scale).color(r * s, g * s, b * s, a).endVertex();
    }

    /**
     * A bar of square-ish section bent through the given points, {@code radius} across, with
     * {@code sides} faces round it.
     */
    static void tube(double[][] points, double radius, int sides) {
        double[][] prev = null;
        for (int i = 0; i < points.length; i++) {
            double[] p = points[i];
            double[] t = i == 0 ? sub(points[1], points[0]) : i == points.length - 1 ? sub(points[i], points[i - 1]) : sub(points[i + 1], points[i - 1]);
            t = normalize(t[0], t[1], t[2]);
            double[] helper = Math.abs(t[1]) > 0.9D ? new double[]{1D, 0D, 0D} : new double[]{0D, 1D, 0D};
            double[] u = normalize(cross(t, helper));
            double[] w = cross(t, u);
            double[][] ring = new double[sides][];
            for (int k = 0; k < sides; k++) {
                double ang = Math.PI * 2D * (k + 0.5D) / sides;
                double c = Math.cos(ang), s = Math.sin(ang);
                ring[k] = new double[]{p[0] + (u[0] * c + w[0] * s) * radius, p[1] + (u[1] * c + w[1] * s) * radius, p[2] + (u[2] * c + w[2] * s) * radius,
                        u[0] * c + w[0] * s, u[1] * c + w[1] * s, u[2] * c + w[2] * s};
            }
            if (prev != null) {
                for (int k = 0; k < sides; k++) {
                    int n = (k + 1) % sides;
                    double[] a0 = prev[k], a1 = prev[n], b0 = ring[k], b1 = ring[n];
                    double nx = a0[3] + a1[3], ny = a0[4] + a1[4], nz = a0[5] + a1[5];
                    double[] nn = normalize(nx, ny, nz);
                    quad(a0[0], a0[1], a0[2], a1[0], a1[1], a1[2], b1[0], b1[1], b1[2], b0[0], b0[1], b0[2], nn[0], nn[1], nn[2]);
                }
            }
            prev = ring;
        }
    }

    /** Points along a quadratic curve from a through the pull of c to b. */
    static double[][] curve(double[] a, double[] c, double[] b, int steps) {
        double[][] out = new double[steps + 1][];
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps, m = 1D - t;
            out[i] = new double[]{m * m * a[0] + 2D * m * t * c[0] + t * t * b[0], m * m * a[1] + 2D * m * t * c[1] + t * t * b[1],
                    m * m * a[2] + 2D * m * t * c[2] + t * t * b[2]};
        }
        return out;
    }

    private static double[] sub(double[] a, double[] b) {
        return new double[]{a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    private static double[] cross(double[] a, double[] b) {
        return new double[]{a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    private static double[] normalize(double[] v) {
        return normalize(v[0], v[1], v[2]);
    }

    private static double[] normalize(double x, double y, double z) {
        double len = Math.sqrt(x * x + y * y + z * z);
        return len < 1.0E-9 ? new double[]{0D, 1D, 0D} : new double[]{x / len, y / len, z / len};
    }
}
