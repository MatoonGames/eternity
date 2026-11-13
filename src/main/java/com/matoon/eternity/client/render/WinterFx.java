package com.matoon.eternity.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.Random;

import static com.matoon.eternity.client.render.WarlockFx.basis;
import static com.matoon.eternity.client.render.WarlockFx.glow;
import static com.matoon.eternity.client.render.WarlockFx.nearBeam;
import static com.matoon.eternity.client.render.WarlockFx.ring;
import static com.matoon.eternity.client.render.WarlockFx.strip;

/**
 * The Casket's winter let out: the torrent pouring from it while it is held open, the
 * Fimbulwinter racing out across the land when it is let go, the blizzard it leaves behind,
 * and frost closing in over your sight. Positions are world coordinates; geometry is drawn
 * relative to the camera, additively.
 */
@SideOnly(Side.CLIENT)
public final class WinterFx {

    static final float[] FROST = VaultFx.FROST;
    static final float[] ICE = VaultFx.ICE;
    static final float[] DEEP = VaultFx.DEEP;
    static final float[] WHITE = {1F, 1F, 1F};

    private static final Vec3d UP = new Vec3d(0D, 1D, 0D);
    private static final int STREAKS = 90, FLAKES = 700;
    private static final double SNOW_BOX = 26D;

    private static DynamicTexture rime;

    private WinterFx() {
    }

    /** The additive, depth-writeless, untextured state everything here draws in. */
    public static void begin() {
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        GlStateManager.depthMask(false);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
    }

    // ------------------------------------------------------------------ the torrent

    /**
     * The winter pouring out of the open casket at {@code from} along {@code dir}, as far as
     * {@code reach} or the first thing it strikes ({@code end}): a blast of white down its middle,
     * streaks of frost racing out in a widening cone, two ribbons of ice twisting round it, and a
     * mist rolling out behind; and where it lands, frost bursting outward. The fuller the casket,
     * the wider and fiercer all of it.
     */
    public static void torrent(Vec3d camera, Vec3d from, Vec3d dir, double reach, Vec3d end, float full, float time, int seed) {
        Vec3d a = from.subtract(camera);
        Vec3d d = dir.normalize();
        double length = end.subtract(from).length();
        double spread = 0.16D + 0.16D * full;
        float power = 0.55F + 0.45F * full;
        Vec3d[] b = basis(d);

        int pieces = Math.max(1, (int) Math.ceil(length / 1.5D));
        // Its body: a haze of frost that tints what is behind it, not only lights it, so it shows against snow and a bright sky.
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        for (int i = 0; i < pieces; i++) {
            double s0 = length * i / pieces, s1 = length * (i + 1) / pieces;
            Vec3d p0 = a.add(d.scale(s0)), p1 = a.add(d.scale(s1));
            float f0 = nearBeam(p0) * power * ramp(s0), f1 = nearBeam(p1) * power * ramp(s1);
            double w0 = girth(s0, spread), w1 = girth(s1, spread);
            float churn0 = 0.75F + 0.25F * MathHelper.sin((float) (s0 * 0.6D) - time * 0.9F);
            float churn1 = 0.75F + 0.25F * MathHelper.sin((float) (s1 * 0.6D) - time * 0.9F);
            strip(p0, p1, w0 * 1.8D, w1 * 1.8D, ICE, ICE, 0.3F * f0 * churn0, 0.3F * f1 * churn1);
            strip(p0, p1, w0 * 0.8D, w1 * 0.8D, FROST, FROST, 0.4F * f0 * churn0, 0.35F * f1 * churn1);
        }
        for (int k = 0; k < 2; k++) {
            Vec3d prev = null;
            for (int i = 0; i <= 40; i++) {
                double s = length * i / 40D;
                double r = 0.25D + s * spread * 0.7D;
                double ang = s * 0.75D - time * 0.35D + k * Math.PI;
                Vec3d p = a.add(d.scale(s)).add(b[0].scale(Math.cos(ang) * r)).add(b[1].scale(Math.sin(ang) * r));
                if (prev != null) {
                    strip(prev, p, 0.09D + s * 0.01D, DEEP, 0.4F * nearBeam(p) * power * ramp(s) * (float) (1D - s / (length + 2D)));
                }
                prev = p;
            }
        }
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);

        // The blast down the middle, fattening as it goes.
        for (int i = 0; i < pieces; i++) {
            double s0 = length * i / pieces, s1 = length * (i + 1) / pieces;
            Vec3d p0 = a.add(d.scale(s0)), p1 = a.add(d.scale(s1));
            // Faded in from the mouth, so it never fills the eyes of whoever holds it.
            float f0 = nearBeam(p0) * power * ramp(s0), f1 = nearBeam(p1) * power * ramp(s1);
            double w0 = girth(s0, spread) * 0.8D, w1 = girth(s1, spread) * 0.8D;
            float ripple0 = 0.85F + 0.15F * MathHelper.sin((float) (s0 * 0.9D) - time * 1.6F);
            float ripple1 = 0.85F + 0.15F * MathHelper.sin((float) (s1 * 0.9D) - time * 1.6F);
            strip(p0, p1, w0 * 1.8D, w1 * 1.8D, DEEP, DEEP, 0.12F * f0, 0.12F * f1);
            strip(p0, p1, w0 * 1.3D, w1 * 1.3D, ICE, ICE, 0.22F * f0 * ripple0, 0.22F * f1 * ripple1);
            strip(p0, p1, w0 * 0.45D, w1 * 0.45D, FROST, FROST, 0.45F * f0 * ripple0, 0.4F * f1 * ripple1);
            strip(p0, p1, 0.06D + 0.05D * full, 0.06D + 0.05D * full, WHITE, WHITE, 0.8F * f0, 0.5F * f1);
        }

        // Streaks of frost racing out.
        Random rand = new Random(seed);
        for (int i = 0; i < STREAKS; i++) {
            double ang = rand.nextDouble() * Math.PI * 2D, off = Math.sqrt(rand.nextDouble()) * spread * 1.1D;
            double speed = 1.4D + rand.nextDouble() * 1.2D, phase = rand.nextDouble();
            Vec3d sd = d.add(b[0].scale(Math.cos(ang) * off)).add(b[1].scale(Math.sin(ang) * off)).normalize();
            double cycle = reach / speed;
            double t = ((time / cycle + phase) % 1D) * reach;
            if (t > length + 1D) {
                continue;
            }
            double head = t, tail = Math.max(0D, head - 2.2D - full * 1.5D);
            Vec3d h = a.add(sd.scale(head)), tl = a.add(sd.scale(tail));
            float fade = (float) (1D - head / (reach + 1D)) * nearBeam(h) * power * ramp(head);
            strip(tl, h, 0.03D, 0.08D + head * 0.015D, ICE, FROST, 0F, 0.7F * fade);
        }

        // Two ribbons of ice twisting round it.
        for (int k = 0; k < 2; k++) {
            Vec3d prev = null;
            for (int i = 0; i <= 40; i++) {
                double s = length * i / 40D;
                double r = 0.25D + s * spread * 0.7D;
                double ang = s * 0.75D - time * 0.35D + k * Math.PI;
                Vec3d p = a.add(d.scale(s)).add(b[0].scale(Math.cos(ang) * r)).add(b[1].scale(Math.sin(ang) * r));
                if (prev != null) {
                    float f = nearBeam(p) * power * ramp(s) * (float) (1D - s / (length + 2D));
                    strip(prev, p, 0.06D + s * 0.008D, ICE, 0.45F * f);
                    strip(prev, p, 0.025D, FROST, 0.6F * f);
                }
                prev = p;
            }
        }

        // Mist rolling out behind, in billows.
        for (int k = 0; k < 9; k++) {
            double s = ((time * 0.11D + k / 9D) % 1D) * length;
            double r = 0.6D + s * spread * 1.6D;
            float f = (float) Math.sin(Math.PI * s / Math.max(1D, length));
            glow(a.add(d.scale(s)).add(b[0].scale(Math.sin(k * 2.3D + time * 0.05D) * r * 0.3D)), r, FROST, 0.07F * f * power);
        }

        // Its mouth, blazing.
        glow(a, 0.5D + 0.6D * full, WHITE, 0.6F * power);
        glow(a, 1.2D + 1.4D * full, ICE, 0.25F * power);

        // Where it lands.
        if (length < reach - 0.5D) {
            Vec3d hit = end.subtract(camera);
            float beat = 0.7F + 0.3F * MathHelper.sin(time * 0.9F);
            glow(hit, 1.5D + 2.5D * full, FROST, 0.35F * beat * power);
            for (int k = 0; k < 3; k++) {
                float p = ((time * 0.06F) + k / 3F) % 1F;
                ring(hit, d.scale(-1D), (0.5D + 3D * full + 1D) * p, 0.35D, FROST, 0.5F * (1F - p) * power);
            }
        }
    }

    /** How thick the torrent is this far out: widening as it goes, but never more than a couple of blocks across. */
    private static double girth(double along, double spread) {
        return Math.min(0.9D, 0.22D + along * spread * 0.22D);
    }

    /** 0 at the casket's mouth, rising to 1 three blocks out. */
    private static float ramp(double along) {
        return (float) MathHelper.clamp((along - 0.8D) / 3D, 0D, 1D);
    }

    // ------------------------------------------------------------------ the Fimbulwinter

    /**
     * The Fimbulwinter racing out: first a pillar of the casket's light up into the sky and a
     * flash that whites out everything near; then a wall of winter, a storm front of snow and ice
     * light taller than the trees, running out across the land, a ring of frost and flung ice at
     * its foot, and behind it everything gone white.
     */
    public static class Fimbul extends FxRenderer.Fx {
        final Vec3d at;
        final double radius, speed;
        final long seed;

        public Fimbul(Vec3d at, double radius, double speed, long seed) {
            super((int) Math.ceil(radius / speed) + 40);
            this.at = at;
            this.radius = radius;
            this.speed = speed;
            this.seed = seed;
        }

        /** How far out the wall has run. */
        public double front(float partialTicks) {
            return Math.min(radius, speed * (age + partialTicks));
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            float time = age + partialTicks;
            double r = front(partialTicks);
            boolean running = r < radius;
            float fade = running ? 1F : 1F - MathHelper.clamp((float) ((time - radius / speed) / 40D), 0F, 1F);
            Vec3d c = at.subtract(camera);

            // The pillar, and the flash.
            float burst = 1F - MathHelper.clamp(time / 45F, 0F, 1F);
            if (burst > 0F) {
                for (int i = 0; i < 12; i++) {
                    Vec3d p0 = c.add(0D, i * 12D, 0D), p1 = c.add(0D, (i + 1) * 12D, 0D);
                    float f = burst * (1F - i / 12F);
                    strip(p0, p1, 3.5D * burst + 0.6D, ICE, 0.3F * f);
                    strip(p0, p1, 1.2D * burst + 0.2D, FROST, 0.6F * f);
                    strip(p0, p1, 0.35D, WHITE, 0.9F * f);
                }
                glow(c.add(0D, 1D, 0D), 4D + 18D * (1F - burst), WHITE, 0.9F * burst * burst);
                glow(c.add(0D, 1D, 0D), 10D + 30D * (1F - burst), ICE, 0.35F * burst);
            }

            if (fade <= 0F) {
                return;
            }
            // The wall: a storm front, bright and thick at its foot, thinning up into the sky, ragged along its top.
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            int segments = 160;
            double height = 26D + radius * 0.12D;
            float strength = fade * (running ? 1F : 0.6F);
            for (int layer = 0; layer < 3; layer++) {
                double lr = r - layer * 2.2D;
                if (lr <= 0.5D) {
                    continue;
                }
                float[] col = layer == 0 ? FROST : layer == 1 ? ICE : DEEP;
                float la = (layer == 0 ? 0.32F : layer == 1 ? 0.22F : 0.14F) * strength;
                double lh = height * (1D - layer * 0.18D);
                buffer.begin(GL11.GL_QUAD_STRIP, DefaultVertexFormats.POSITION_COLOR);
                for (int i = 0; i <= segments; i++) {
                    double ang = Math.PI * 2D * i / segments;
                    double cos = Math.cos(ang), sin = Math.sin(ang);
                    float n = 0.55F + 0.45F * MathHelper.sin((float) (ang * 7D + time * 0.05D + layer)) * MathHelper.sin((float) (ang * 13D - time * 0.08D));
                    double top = lh * (0.6D + 0.4D * n);
                    double x = c.x + cos * lr, z = c.z + sin * lr;
                    float near = (float) MathHelper.clamp((Math.sqrt(x * x + z * z) - 2D) / 10D, 0D, 1D);
                    float a = la * near * (0.6F + 0.4F * n);
                    buffer.pos(x, c.y - 6D, z).color(col[0], col[1], col[2], a).endVertex();
                    buffer.pos(x, c.y + top, z).color(col[0], col[1], col[2], 0F).endVertex();
                }
                tessellator.draw();
            }
            // Its foot: a ring of frost on the ground, and ice flung up along it.
            ring(c.add(0D, 0.3D, 0D), UP, r, 3D, FROST, 0.6F * strength);
            ring(c.add(0D, 0.3D, 0D), UP, Math.max(0D, r - 4D), 6D, ICE, 0.25F * strength);
            if (running) {
                Random rand = new Random(seed + age);
                buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
                for (int i = 0; i < 120; i++) {
                    double ang = rand.nextDouble() * Math.PI * 2D;
                    double rr = r - rand.nextDouble() * 3D;
                    double h = 0.8D + rand.nextDouble() * 4D;
                    Vec3d base = c.add(Math.cos(ang) * rr, rand.nextDouble() * 2D - 1D, Math.sin(ang) * rr);
                    Vec3d tip = base.add(Math.cos(ang) * 0.8D, h, Math.sin(ang) * 0.8D);
                    Vec3d side = new Vec3d(-Math.sin(ang), 0D, Math.cos(ang)).scale(0.3D);
                    float near = (float) MathHelper.clamp((base.length() - 3D) / 8D, 0D, 1D);
                    buffer.pos(base.x - side.x, base.y, base.z - side.z).color(ICE[0], ICE[1], ICE[2], 0.6F * near).endVertex();
                    buffer.pos(base.x + side.x, base.y, base.z + side.z).color(ICE[0], ICE[1], ICE[2], 0.6F * near).endVertex();
                    buffer.pos(tip.x, tip.y, tip.z).color(FROST[0], FROST[1], FROST[2], 0.85F * near).endVertex();
                }
                tessellator.draw();
                // Snow thrown up behind the front, in clouds.
                for (int i = 0; i < 24; i++) {
                    double ang = Math.PI * 2D * i / 24D + time * 0.004D;
                    glow(c.add(Math.cos(ang) * (r - 5D), 3D, Math.sin(ang) * (r - 5D)), 7D, FROST, 0.1F * strength);
                }
            }
        }
    }

    // ------------------------------------------------------------------ the blizzard

    /**
     * Snow driven sideways on a hard wind, all round the camera, {@code strength} 0 to 1. The
     * flakes are fixed in the world and wrap round a box that follows the camera, so moving
     * through them reads true.
     */
    public static void blizzard(Vec3d camera, float time, float strength) {
        if (strength <= 0.01F) {
            return;
        }
        Vec3d wind = new Vec3d(0.55D, -0.28D, 0.22D);
        Vec3d streak = wind.normalize().scale(0.35D + 0.25D * strength);
        Random rand = new Random(4242L);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        int count = (int) (FLAKES * strength);
        for (int i = 0; i < count; i++) {
            double speed = 0.6D + rand.nextDouble() * 0.6D;
            double sx = rand.nextDouble() * SNOW_BOX + wind.x * time * speed;
            double sy = rand.nextDouble() * SNOW_BOX + wind.y * time * speed;
            double sz = rand.nextDouble() * SNOW_BOX + wind.z * time * speed;
            double x = wrap(sx - camera.x), y = wrap(sy - camera.y), z = wrap(sz - camera.z);
            double dist = Math.sqrt(x * x + y * y + z * z);
            if (dist < 0.8D) {
                continue;
            }
            float a = (float) (0.55D * strength * Math.min(1D, (dist - 0.8D) / 2D) * (1D - dist / (SNOW_BOX * 0.6D)));
            if (a <= 0.01F) {
                continue;
            }
            Vec3d p = new Vec3d(x, y, z);
            Vec3d side = streak.crossProduct(p).normalize().scale(0.025D + 0.01D * (i % 3));
            Vec3d q = p.subtract(streak);
            buffer.pos(p.x - side.x, p.y - side.y, p.z - side.z).color(1F, 1F, 1F, a).endVertex();
            buffer.pos(p.x + side.x, p.y + side.y, p.z + side.z).color(1F, 1F, 1F, a).endVertex();
            buffer.pos(q.x + side.x, q.y + side.y, q.z + side.z).color(0.8F, 0.9F, 1F, 0F).endVertex();
            buffer.pos(q.x - side.x, q.y - side.y, q.z - side.z).color(0.8F, 0.9F, 1F, 0F).endVertex();
        }
        tessellator.draw();
    }

    private static double wrap(double v) {
        double m = v % SNOW_BOX;
        if (m < 0D) {
            m += SNOW_BOX;
        }
        return m - SNOW_BOX / 2D;
    }

    // ------------------------------------------------------------------ frost over your sight

    /** Rime creeping in from the edges of the screen, {@code strength} 0 to 1, and white over everything at {@code flash}. */
    public static void frost(float strength, float flash) {
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution res = new ScaledResolution(mc);
        double w = res.getScaledWidth_double(), h = res.getScaledHeight_double();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        if (strength > 0.01F) {
            GlStateManager.enableTexture2D();
            if (rime == null) {
                rime = ProcTex.texture(256, WinterFx::rimePixel);
            }
            GlStateManager.bindTexture(rime.getGlTextureId());
            GlStateManager.color(0.85F, 0.93F, 1F, Math.min(1F, strength));
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
            buffer.pos(0D, h, 0D).tex(0D, 1D).endVertex();
            buffer.pos(w, h, 0D).tex(1D, 1D).endVertex();
            buffer.pos(w, 0D, 0D).tex(1D, 0D).endVertex();
            buffer.pos(0D, 0D, 0D).tex(0D, 0D).endVertex();
            tessellator.draw();
        }
        if (flash > 0.01F) {
            GlStateManager.disableTexture2D();
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            buffer.pos(0D, h, 0D).color(0.92F, 0.97F, 1F, flash).endVertex();
            buffer.pos(w, h, 0D).color(0.92F, 0.97F, 1F, flash).endVertex();
            buffer.pos(w, 0D, 0D).color(0.92F, 0.97F, 1F, flash).endVertex();
            buffer.pos(0D, 0D, 0D).color(0.92F, 0.97F, 1F, flash).endVertex();
            tessellator.draw();
            GlStateManager.enableTexture2D();
        }
        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.enableAlpha();
    }

    /** Frost on glass: feathers of rime growing in from the edges, thick at the corners, clear in the middle. */
    private static int rimePixel(float u, float v) {
        float dx = (u - 0.5F) * 2F, dy = (v - 0.5F) * 2F;
        float edge = (float) Math.pow(Math.pow(Math.abs(dx), 4D) + Math.pow(Math.abs(dy), 4D), 0.25D);
        float n = ProcTex.fbm(u * 5F, v * 5F, 5, 5, 61) * 0.5F + 0.5F;
        float reach = ProcTex.smooth((edge - 0.68F - 0.25F * n) / 0.3F);
        // Feathers: the borders of fine cells, drawn out by the noise.
        float[] c = ProcTex.cells(u * 22F + n * 3F, v * 22F - n * 3F, 22, 63);
        float vein = MathHelper.clamp(1F - (c[1] - c[0]) / 0.07F, 0F, 1F) * (0.4F + 0.6F * n);
        float sparkle = ProcTex.hash((int) (u * 256F), (int) (v * 256F), 5) > 0.97F ? 0.5F : 0F;
        float a = MathHelper.clamp(reach * (0.45F + 0.3F * vein + 0.25F * n) + reach * sparkle, 0F, 0.9F);
        float white = 0.75F + 0.25F * vein;
        return ProcTex.argb(a, white, white, 1F);
    }
}
