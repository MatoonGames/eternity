package com.matoon.eternity.client.render;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The Astral Regulator's model: a faceted crystal sphere of pale light with burning
 * edges, a white-hot core and a ring of gold beads set inside it. Charged, its heart
 * becomes a universe in miniature. Once it holds the Living Tribunal it evolves: the
 * crystal warms to gold, the universe inside burns gold, and his power circles it
 * outside in turning rings, rays and drifting motes.
 */
@SideOnly(Side.CLIENT)
public final class OrbRenderer {

    private static final float[][] VERTICES;
    private static final int[][] FACES;
    private static final int[][] EDGES;
    private static final float[] LIGHT = normalize(new float[]{0.4F, 0.8F, 0.45F});

    static {
        // An icosahedron, subdivided once: eighty flat facets, like the crystal in the art.
        float t = (1F + (float) Math.sqrt(5D)) / 2F;
        List<float[]> verts = new ArrayList<>();
        float[][] base = {{-1, t, 0}, {1, t, 0}, {-1, -t, 0}, {1, -t, 0}, {0, -1, t}, {0, 1, t},
                {0, -1, -t}, {0, 1, -t}, {t, 0, -1}, {t, 0, 1}, {-t, 0, -1}, {-t, 0, 1}};
        for (float[] v : base) {
            verts.add(normalize(v));
        }
        int[][] ico = {{0, 11, 5}, {0, 5, 1}, {0, 1, 7}, {0, 7, 10}, {0, 10, 11}, {1, 5, 9}, {5, 11, 4}, {11, 10, 2},
                {10, 7, 6}, {7, 1, 8}, {3, 9, 4}, {3, 4, 2}, {3, 2, 6}, {3, 6, 8}, {3, 8, 9}, {4, 9, 5}, {2, 4, 11},
                {6, 2, 10}, {8, 6, 7}, {9, 8, 1}};
        Map<Long, Integer> midpoints = new HashMap<>();
        List<int[]> faces = new ArrayList<>();
        for (int[] f : ico) {
            int a = midpoint(f[0], f[1], verts, midpoints);
            int b = midpoint(f[1], f[2], verts, midpoints);
            int c = midpoint(f[2], f[0], verts, midpoints);
            faces.add(new int[]{f[0], a, c});
            faces.add(new int[]{f[1], b, a});
            faces.add(new int[]{f[2], c, b});
            faces.add(new int[]{a, b, c});
        }
        Set<Long> edges = new LinkedHashSet<>();
        for (int[] f : faces) {
            for (int i = 0; i < 3; i++) {
                int p = f[i], q = f[(i + 1) % 3];
                edges.add(((long) Math.min(p, q) << 32) | Math.max(p, q));
            }
        }
        VERTICES = verts.toArray(new float[0][]);
        FACES = faces.toArray(new int[0][]);
        EDGES = new int[edges.size()][];
        int i = 0;
        for (long e : edges) {
            EDGES[i++] = new int[]{(int) (e >> 32), (int) e};
        }
    }

    private OrbRenderer() {
    }

    /**
     * Draws the orb centred on the current origin.
     *
     * @param charge 0 for an empty Regulator, 1 for a charged one; in between while it absorbs
     */
    public static void draw(float radius, float time, float charge) {
        draw(radius, time, charge, true);
    }

    /**
     * @param heart whether to draw the glowing core and, when charged, the universe inside it.
     *              Off when something else is meant to be seen through the crystal.
     */
    public static void draw(float radius, float time, float charge, boolean heart) {
        draw(radius, time, charge, heart, false);
    }

    /** @param tribunal whether it holds the Living Tribunal, and so burns gold */
    public static void draw(float radius, float time, float charge, boolean heart, boolean tribunal) {
        GlStateManager.pushMatrix();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);

        float pulse = 0.85F + 0.15F * MathHelper.sin(time * 0.15F);

        // A universe in its heart, once charged.
        if (heart && charge > 0.01F) {
            GlStateManager.disableCull();
            if (tribunal) {
                Cosmic.draw(() -> sphere(radius * 0.62F), 10, charge, charge * 1.9F, 1F, 0.8F, 0.35F, false);
            } else {
                Cosmic.draw(() -> sphere(radius * 0.62F), 10, charge, charge * 1.6F, 0.9F, 0.6F, 1F, false);
            }
        }

        GlStateManager.disableTexture2D();
        GlStateManager.depthMask(false);

        // White-hot core.
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        GlStateManager.enableCull();
        if (heart && tribunal) {
            // A golden sun where the universe was.
            GlStateManager.color(1F, 0.88F, 0.5F, 0.9F * pulse);
            sphere(radius * 0.36F);
            GlStateManager.color(1F, 0.7F, 0.25F, 0.4F * pulse);
            sphere(radius * 0.52F);
            GlStateManager.color(1F, 0.95F, 0.8F, 0.15F * pulse);
            sphere(radius * 0.7F);
        } else if (heart) {
            GlStateManager.color(0.8F + 0.2F * charge, 0.95F - 0.3F * charge, 1F, (0.85F - 0.35F * charge) * pulse);
            sphere(radius * 0.38F);
            GlStateManager.color(0.6F, 0.85F, 1F, 0.25F * pulse);
            sphere(radius * 0.55F);
        }

        // Gold beads circling inside.
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.depthMask(true);
        beads(radius * 0.8F, radius * 0.06F, time);
        GlStateManager.depthMask(false);

        // The crystal: inner facets first, dim, then the outer ones.
        float r = tribunal ? 1F : 0.7F - 0.1F * charge;
        float g = tribunal ? 0.88F : 0.93F - 0.2F * charge;
        float b = tribunal ? 0.6F : 1F;
        GlStateManager.enableCull();
        GlStateManager.cullFace(GlStateManager.CullFace.FRONT);
        facets(radius, r, g, b, 0.16F);
        GlStateManager.cullFace(GlStateManager.CullFace.BACK);
        facets(radius, r, g, b, 0.3F);

        // Sheen and edges, additive.
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        sheen(radius * 1.001F);
        GlStateManager.disableCull();
        GlStateManager.glLineWidth(1.6F);
        if (tribunal) {
            edges(radius * 1.002F, 1F, 0.85F, 0.4F, pulse);
            judgement(radius, time, pulse);
        } else {
            edges(radius * 1.002F, 0.85F, 1F - 0.2F * charge, 1F, 0.9F * pulse);
        }

        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();
        GlStateManager.enableLighting();
        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.popMatrix();
    }

    /**
     * The Tribunal's power, circling the crystal: three tilted rings of gold turning
     * against each other, beads of light running along them, rays of light thrown out
     * from the heart, and motes drifting in a slow orbit.
     */
    private static void judgement(float radius, float time, float pulse) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        GlStateManager.disableCull();
        float[][] tilts = {{70F, 0F, 0.3F}, {-55F, 1F, 0.2F}, {15F, 0.4F, 1F}};
        for (int k = 0; k < tilts.length; k++) {
            GlStateManager.pushMatrix();
            GlStateManager.rotate(tilts[k][0], tilts[k][1], 0F, tilts[k][2]);
            GlStateManager.rotate(time * (k % 2 == 0 ? 1.4F : -1.1F) * (1F + k * 0.3F), 0F, 1F, 0F);
            float ring = radius * (1.25F + 0.08F * k);
            float width = radius * 0.025F;
            buffer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
            for (int i = 0; i <= 48; i++) {
                double a = Math.PI * 2D * i / 48D;
                // Brighter in a band that runs around the ring.
                float run = 0.4F + 0.6F * Math.max(0F, MathHelper.sin((float) a * 2F - time * 0.15F));
                float c = (float) Math.cos(a), s = (float) Math.sin(a);
                buffer.pos(c * (ring - width), 0, s * (ring - width)).color(1F, 0.82F, 0.35F, 0.7F * run * pulse).endVertex();
                buffer.pos(c * (ring + width), 0, s * (ring + width)).color(1F, 0.95F, 0.7F, 0.7F * run * pulse).endVertex();
            }
            tessellator.draw();
            buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2D * i / 6D;
                octahedron(buffer, (float) Math.cos(a) * ring, 0F, (float) Math.sin(a) * ring, radius * 0.045F, 1F, 0.95F * pulse, 0.6F * pulse);
            }
            tessellator.draw();
            GlStateManager.popMatrix();
        }

        // Rays thrown out from the heart, turning slowly and breathing.
        GlStateManager.pushMatrix();
        GlStateManager.rotate(time * 0.6F, 0.2F, 1F, 0.1F);
        buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < 12; i++) {
            double a = Math.PI * 2D * i / 12D;
            double tilt = (i % 3 - 1) * 0.5D;
            float length = radius * (1.5F + 0.25F * MathHelper.sin(time * 0.12F + i * 1.7F));
            float spread = radius * 0.07F;
            float dx = (float) (Math.cos(a) * Math.cos(tilt)), dy = (float) Math.sin(tilt), dz = (float) (Math.sin(a) * Math.cos(tilt));
            float sx = (float) -Math.sin(a) * spread, sz = (float) Math.cos(a) * spread;
            float base = radius * 0.9F;
            buffer.pos(dx * base + sx, dy * base, dz * base + sz).color(1F, 0.9F, 0.55F, 0.6F * pulse).endVertex();
            buffer.pos(dx * base - sx, dy * base, dz * base - sz).color(1F, 0.9F, 0.55F, 0.6F * pulse).endVertex();
            buffer.pos(dx * length, dy * length, dz * length).color(1F, 0.8F, 0.4F, 0F).endVertex();
        }
        tessellator.draw();
        GlStateManager.popMatrix();

        // Motes of gold drifting around it.
        buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < 10; i++) {
            float a = time * (0.025F + i * 0.004F) + i * 2.4F;
            float orbit = radius * (1.35F + 0.2F * MathHelper.sin(i * 1.3F));
            float y = radius * 0.6F * MathHelper.sin(a * 1.3F + i);
            float glow = 0.6F + 0.4F * MathHelper.sin(time * 0.3F + i);
            octahedron(buffer, MathHelper.cos(a) * orbit, y, MathHelper.sin(a) * orbit, radius * 0.035F, glow, 0.85F * glow, 0.4F * glow);
        }
        tessellator.draw();
        GlStateManager.enableCull();
    }

    private static void facets(float radius, float r, float g, float b, float alpha) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
        for (int[] f : FACES) {
            float[] n = faceNormal(f);
            float shade = 0.55F + 0.45F * Math.max(0F, dot(n, LIGHT));
            for (int i = 0; i < 3; i++) {
                float[] v = VERTICES[f[i]];
                buffer.pos(v[0] * radius, v[1] * radius, v[2] * radius).color(r * shade, g * shade, b * shade, alpha).endVertex();
            }
        }
        tessellator.draw();
    }

    /** A glint on the facets turned toward the light. */
    private static void sheen(float radius) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
        for (int[] f : FACES) {
            float d = Math.max(0F, dot(faceNormal(f), LIGHT));
            float glint = d * d * d * d * 0.35F;
            for (int i = 0; i < 3; i++) {
                float[] v = VERTICES[f[i]];
                buffer.pos(v[0] * radius, v[1] * radius, v[2] * radius).color(1F, 1F, 1F, glint).endVertex();
            }
        }
        tessellator.draw();
    }

    private static void edges(float radius, float r, float g, float b, float alpha) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        for (int[] e : EDGES) {
            float[] p = VERTICES[e[0]], q = VERTICES[e[1]];
            buffer.pos(p[0] * radius, p[1] * radius, p[2] * radius).color(r, g, b, alpha).endVertex();
            buffer.pos(q[0] * radius, q[1] * radius, q[2] * radius).color(r, g, b, alpha).endVertex();
        }
        tessellator.draw();
    }

    private static void beads(float ring, float size, float time) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        GlStateManager.pushMatrix();
        GlStateManager.rotate(20F, 1F, 0F, 0.4F);
        GlStateManager.rotate(time * 0.8F, 0F, 1F, 0F);
        buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
        int count = 22;
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2D * i / count;
            float cx = (float) Math.cos(angle) * ring, cz = (float) Math.sin(angle) * ring;
            float glow = 0.75F + 0.25F * MathHelper.sin(time * 0.2F + i);
            octahedron(buffer, cx, 0F, cz, size, 1F * glow, 0.78F * glow, 0.25F * glow);
        }
        tessellator.draw();
        GlStateManager.popMatrix();
    }

    private static void octahedron(BufferBuilder buffer, float x, float y, float z, float s, float r, float g, float b) {
        float[][] p = {{x + s, y, z}, {x - s, y, z}, {x, y + s, z}, {x, y - s, z}, {x, y, z + s}, {x, y, z - s}};
        int[][] tris = {{0, 2, 4}, {4, 2, 1}, {1, 2, 5}, {5, 2, 0}, {4, 3, 0}, {1, 3, 4}, {5, 3, 1}, {0, 3, 5}};
        for (int i = 0; i < tris.length; i++) {
            float shade = i < 4 ? 1F : 0.65F;
            for (int k : tris[i]) {
                buffer.pos(p[k][0], p[k][1], p[k][2]).color(r * shade, g * shade, b * shade, 1F).endVertex();
            }
        }
    }

    /** The icosphere as plain positions, for use inside {@link Cosmic#draw}. */
    public static void sphere(float radius) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION);
        for (int[] f : FACES) {
            for (int i = 0; i < 3; i++) {
                float[] v = VERTICES[f[i]];
                buffer.pos(v[0] * radius, v[1] * radius, v[2] * radius).endVertex();
            }
        }
        tessellator.draw();
    }

    private static int midpoint(int a, int b, List<float[]> verts, Map<Long, Integer> cache) {
        long key = ((long) Math.min(a, b) << 32) | Math.max(a, b);
        Integer cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        float[] p = verts.get(a), q = verts.get(b);
        verts.add(normalize(new float[]{(p[0] + q[0]) / 2F, (p[1] + q[1]) / 2F, (p[2] + q[2]) / 2F}));
        cache.put(key, verts.size() - 1);
        return verts.size() - 1;
    }

    private static float[] faceNormal(int[] f) {
        float[] a = VERTICES[f[0]], b = VERTICES[f[1]], c = VERTICES[f[2]];
        return normalize(new float[]{(a[0] + b[0] + c[0]), (a[1] + b[1] + c[1]), (a[2] + b[2] + c[2])});
    }

    private static float dot(float[] a, float[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private static float[] normalize(float[] v) {
        float len = (float) Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        return new float[]{v[0] / len, v[1] / len, v[2] / len};
    }
}
