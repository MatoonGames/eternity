package com.matoon.eternity.client.render;

import com.matoon.eternity.client.SkyFracture;
import com.matoon.eternity.client.SkyFracture.Shard;
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
 * Draws the sky shattering. Fractures sit on a dome just inside the far plane, so
 * terrain still stands in front of them, and are built in layers: a violet haze, the
 * void showing through every broken pane, stars sparkling inside it, shards peeling
 * away and drifting off, burning edges, and cracks racing out across the dome.
 */
@SideOnly(Side.CLIENT)
public final class FractureRenderer {

    private FractureRenderer() {
    }

    /**
     * @param spread how far the wounds have widened; 1 when fresh, more as the world ends
     * @param time   client ticks plus partial ticks, for flicker
     */
    public static void renderAll(Collection<SkyFracture> fractures, float partialTicks, float spread, float time) {
        if (fractures.isEmpty()) {
            return;
        }
        double radius = RiftRenderer.farPlane() * 1.2D;
        GlStateManager.pushMatrix();
        GlStateManager.disableFog();
        GlStateManager.disableCull();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.depthMask(false);
        for (SkyFracture fracture : fractures) {
            render(fracture, radius, fracture.growth(partialTicks), spread, time);
        }
        GlStateManager.depthMask(true);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.enableCull();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }

    private static void render(SkyFracture f, double radius, float growth, float spread, float time) {
        if (growth <= 0.001F) {
            return;
        }
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        // Violet haze bleeding out of the wound.
        additive();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        int steps = 24;
        buffer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i <= steps; i++) {
            float u = -f.halfLength + 2F * f.halfLength * i / steps;
            float w = f.envelope(u) * 2.4F;
            put(buffer, f, u, -w, spread, radius).color(0.35F, 0.12F, 0.7F, 0F).endVertex();
            put(buffer, f, u, 0, spread, radius).color(0.45F, 0.2F, 0.9F, 0.28F * growth).endVertex();
        }
        tessellator.draw();
        buffer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i <= steps; i++) {
            float u = -f.halfLength + 2F * f.halfLength * i / steps;
            float w = f.envelope(u) * 2.4F;
            put(buffer, f, u, 0, spread, radius).color(0.45F, 0.2F, 0.9F, 0.28F * growth).endVertex();
            put(buffer, f, u, w, spread, radius).color(0.35F, 0.12F, 0.7F, 0F).endVertex();
        }
        tessellator.draw();
        GlStateManager.shadeModel(GL11.GL_FLAT);

        // The void, showing through every pane that has broken away.
        Cosmic.draw(() -> {
            buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION);
            for (Shard s : f.shards) {
                if (s.breakAt < growth) {
                    quad(buffer, f, s.u, s.v, 1.04F, s.cu, s.cv, spread, radius);
                }
            }
            tessellator.draw();
        }, 12, 1F, 1.9F, 0.55F, 0.6F, 1F, false);

        // Stars burning inside the opening.
        additive();
        double sparkSize = radius * 0.0035D;
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        for (float[] spark : f.sparks) {
            float reveal = 0.3F + 0.6F * Math.abs(spark[1]) / Math.max(1.0E-4F, f.envelope(spark[0]));
            if (growth < reveal) {
                continue;
            }
            float twinkle = 0.5F + 0.5F * MathHelper.sin(time * 0.15F + spark[2] * 40F);
            Vec3d at = f.direction(spark[0], spark[1], spread).scale(radius);
            billboard(buffer, at, sparkSize * spark[3], 0.85F, 0.8F, 1F, 0.9F * twinkle);
        }
        tessellator.draw();

        // Panes peeling off and drifting away; panes about to go, lifted and lit at the edges.
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
        for (Shard s : f.shards) {
            float t = (growth - s.breakAt) / 0.35F;
            if (t > 0F && t < 1F) {
                debris(buffer, f, s, t, spread, radius, 0.11F, 0.04F, 0.2F, 0.9F * (1F - t));
            }
        }
        tessellator.draw();

        additive();
        double edge = radius * 0.0011D;
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        for (Shard s : f.shards) {
            float since = growth - s.breakAt;
            float flicker = 0.75F + 0.25F * MathHelper.sin(time * 0.6F + s.cu * 90F);
            if (since > 0F) {
                // The rim of the hole: blazing when fresh, settling to a glow.
                float fresh = 1F - MathHelper.clamp(since / 0.2F, 0F, 1F);
                outline(buffer, f, s.u, s.v, 0F, 0F, 0F, 1F, spread, radius, edge * (1F + 2F * fresh),
                        0.7F, 0.55F, 1F, (0.35F + 0.65F * fresh) * flicker);
                float t = since / 0.35F;
                if (t < 1F) {
                    float[] off = drift(f, s, t);
                    outline(buffer, f, s.u, s.v, off[0], off[1], off[2], off[3], spread, radius, edge,
                            0.6F, 0.35F, 1F, 0.7F * (1F - t));
                }
            } else if (since > -0.06F) {
                // About to break: its edges catch light first.
                float soon = 1F + since / 0.06F;
                outline(buffer, f, s.u, s.v, 0F, 0F, 0F, 1F, spread, radius, edge * 0.7F,
                        0.8F, 0.65F, 1F, 0.6F * soon * flicker);
            }
        }
        tessellator.draw();

        // Cracks running out across the unbroken sky.
        drawCracks(f, growth, spread, radius, radius * 0.0055D, 0.55F, 0.3F, 1F, 0.3F, time);
        drawCracks(f, growth, spread, radius, radius * 0.0012D, 0.95F, 0.9F, 1F, 0.95F, time);
    }

    /** Offset of a shard drifting away: across, along, spin and how far toward the viewer. */
    private static float[] drift(SkyFracture f, Shard s, float t) {
        float ease = t * t;
        return new float[]{s.push * ease * f.halfWidth * 1.6F, s.slide * ease * f.halfWidth, s.spin * ease, 1F - 0.08F * ease};
    }

    private static void debris(BufferBuilder buffer, SkyFracture f, Shard s, float t, float spread, double radius,
                               float r, float g, float b, float alpha) {
        float[] off = drift(f, s, t);
        Vec3d[] corners = new Vec3d[4];
        for (int i = 0; i < 4; i++) {
            corners[i] = moved(f, s, i, off, spread, radius);
        }
        int[] tris = {0, 1, 2, 0, 2, 3};
        for (int i : tris) {
            buffer.pos(corners[i].x, corners[i].y, corners[i].z).color(r, g, b, alpha).endVertex();
        }
    }

    private static Vec3d moved(SkyFracture f, Shard s, int i, float[] off, float spread, double radius) {
        float lu = s.u[i] - s.cu, lv = s.v[i] - s.cv;
        float cos = MathHelper.cos(off[2]), sin = MathHelper.sin(off[2]);
        float u = s.cu + off[1] + lu * cos - lv * sin;
        float v = s.cv + off[0] + lu * sin + lv * cos;
        return f.direction(u, v, spread).scale(radius * off[3]);
    }

    private static void outline(BufferBuilder buffer, SkyFracture f, float[] us, float[] vs, float across, float along,
                                float spin, float depth, float spread, double radius, double width,
                                float r, float g, float b, float alpha) {
        float cu = (us[0] + us[1] + us[2] + us[3]) / 4F, cv = (vs[0] + vs[1] + vs[2] + vs[3]) / 4F;
        float cos = MathHelper.cos(spin), sin = MathHelper.sin(spin);
        Vec3d[] p = new Vec3d[4];
        for (int i = 0; i < 4; i++) {
            float lu = us[i] - cu, lv = vs[i] - cv;
            p[i] = f.direction(cu + along + lu * cos - lv * sin, cv + across + lu * sin + lv * cos, spread).scale(radius * depth);
        }
        for (int i = 0; i < 4; i++) {
            segment(buffer, p[i], p[(i + 1) % 4], width, r, g, b, alpha, alpha);
        }
    }

    private static void drawCracks(SkyFracture f, float growth, float spread, double radius, double width,
                                   float r, float g, float b, float alpha, float time) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        for (int c = 0; c < f.cracks.size(); c++) {
            float[] line = f.cracks.get(c);
            float delay = f.crackDelay.get(c);
            float grow = MathHelper.clamp((growth - delay) / (1F - delay), 0F, 1F);
            int points = line.length / 2;
            float reach = grow * (points - 1);
            float flicker = 0.7F + 0.3F * MathHelper.sin(time * 0.5F + c * 1.9F);
            for (int s = 0; s < points - 1 && s < reach; s++) {
                float t = Math.min(1F, reach - s);
                float u0 = line[s * 2], v0 = line[s * 2 + 1];
                float u1 = u0 + (line[s * 2 + 2] - u0) * t, v1 = v0 + (line[s * 2 + 3] - v0) * t;
                Vec3d a = f.direction(u0, v0, spread).scale(radius);
                Vec3d bb = f.direction(u1, v1, spread).scale(radius);
                float taper0 = 1F - (float) s / points, taper1 = 1F - (float) (s + 1) / points;
                segment(buffer, a, bb, width * taper0, r, g, b, alpha * flicker * taper0, alpha * flicker * taper1);
            }
        }
        tessellator.draw();
    }

    // ------------------------------------------------------------------ primitives

    private static void additive() {
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        GlStateManager.depthMask(false);
    }

    private static BufferBuilder put(BufferBuilder buffer, SkyFracture f, double u, double v, float spread, double radius) {
        Vec3d p = f.direction(u, v, spread).scale(radius);
        return buffer.pos(p.x, p.y, p.z);
    }

    /** A shard as two triangles, swollen slightly about its centre so neighbours overlap. */
    private static void quad(BufferBuilder buffer, SkyFracture f, float[] us, float[] vs, float swell, float cu, float cv,
                             float spread, double radius) {
        int[] tris = {0, 1, 2, 0, 2, 3};
        for (int i : tris) {
            double u = cu + (us[i] - cu) * swell, v = cv + (vs[i] - cv) * swell;
            Vec3d p = f.direction(u, v, spread).scale(radius);
            buffer.pos(p.x, p.y, p.z).endVertex();
        }
    }

    /** A strip of light between two points on the dome, lying flat against it. */
    private static void segment(BufferBuilder buffer, Vec3d a, Vec3d b, double width,
                                float r, float g, float bl, float alphaA, float alphaB) {
        Vec3d along = b.subtract(a);
        Vec3d side = along.crossProduct(a);
        if (side.lengthSquared() < 1.0E-10) {
            return;
        }
        side = side.normalize().scale(width / 2D);
        buffer.pos(a.x - side.x, a.y - side.y, a.z - side.z).color(r, g, bl, alphaA).endVertex();
        buffer.pos(a.x + side.x, a.y + side.y, a.z + side.z).color(r, g, bl, alphaA).endVertex();
        buffer.pos(b.x + side.x, b.y + side.y, b.z + side.z).color(r, g, bl, alphaB).endVertex();
        buffer.pos(b.x - side.x, b.y - side.y, b.z - side.z).color(r, g, bl, alphaB).endVertex();
    }

    /** A small square facing the viewer, who sits at the origin. */
    private static void billboard(BufferBuilder buffer, Vec3d at, double size, float r, float g, float b, float alpha) {
        Vec3d dir = at.normalize();
        Vec3d right = dir.crossProduct(new Vec3d(0, 1, 0));
        right = right.lengthSquared() < 1.0E-6 ? new Vec3d(1, 0, 0) : right.normalize();
        Vec3d up = right.crossProduct(dir).normalize();
        right = right.scale(size);
        up = up.scale(size);
        buffer.pos(at.x - right.x - up.x, at.y - right.y - up.y, at.z - right.z - up.z).color(r, g, b, alpha).endVertex();
        buffer.pos(at.x + right.x - up.x, at.y + right.y - up.y, at.z + right.z - up.z).color(r, g, b, alpha).endVertex();
        buffer.pos(at.x + right.x + up.x, at.y + right.y + up.y, at.z + right.z + up.z).color(r, g, b, alpha).endVertex();
        buffer.pos(at.x - right.x + up.x, at.y - right.y + up.y, at.z - right.z + up.z).color(r, g, b, alpha).endVertex();
    }
}
