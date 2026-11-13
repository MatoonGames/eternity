package com.matoon.eternity.client.render;

import com.matoon.eternity.entity.EntityOdin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

import static com.matoon.eternity.client.render.WarlockFx.glow;
import static com.matoon.eternity.client.render.WarlockFx.ring;
import static com.matoon.eternity.client.render.WarlockFx.star;
import static com.matoon.eternity.client.render.WarlockFx.strip;

/**
 * Odin's effects, and the Bifrost's. The Bifrost is all the colours at once: a white core, sheathed in bands of every
 * hue that slide over one another, streaks pouring down it, motes of light whirling about its foot, and where it meets
 * the ground its seal burned in rings and runes. Everything of his own is gold and white: Gungnir, the Odinforce, the
 * runes it leaves. His ravens are the only black thing about him.
 * <p>
 * Positions are world coordinates; geometry is drawn relative to the camera.
 */
@SideOnly(Side.CLIENT)
public final class OdinFx {

    private static final Vec3d UP = new Vec3d(0D, 1D, 0D);
    static final float[] WHITE = {1F, 1F, 1F};
    static final float[] GOLD = {1F, 0.8F, 0.35F};
    static final float[] PALE = {1F, 0.95F, 0.78F};
    static final float[] CYAN = {0.45F, 0.9F, 1F};
    static final float[] ICE = {0.75F, 0.95F, 1F};
    static final float[] SOOT = {0.03F, 0.03F, 0.04F};

    /** How high above the ground the Bifrost is drawn from: out of the sky, as far as anyone can see. */
    private static final double SKY = 320D;

    private OdinFx() {
    }

    // ------------------------------------------------------------------ colour

    /** A hue of the Bifrost, h from 0 to 1 round the wheel, a little paled so that it reads as light. */
    static float[] hue(double h) {
        h = h - Math.floor(h);
        float r = (float) MathHelper.clamp(Math.abs(h * 6D - 3D) - 1D, 0D, 1D);
        float g = (float) MathHelper.clamp(2D - Math.abs(h * 6D - 2D), 0D, 1D);
        float b = (float) MathHelper.clamp(2D - Math.abs(h * 6D - 4D), 0D, 1D);
        return new float[]{0.25F + 0.75F * r, 0.25F + 0.75F * g, 0.25F + 0.75F * b};
    }

    private static float[] mix(float[] a, float[] b, float k) {
        return new float[]{a[0] + (b[0] - a[0]) * k, a[1] + (b[1] - a[1]) * k, a[2] + (b[2] - a[2]) * k};
    }

    // ------------------------------------------------------------------ geometry

    /** A line lying flat (in the horizontal plane), between two camera-relative points. */
    static void flatLine(Vec3d a, Vec3d b, double width, float[] col, float alpha) {
        if (alpha <= 0.003F) {
            return;
        }
        Vec3d d = b.subtract(a);
        Vec3d side = new Vec3d(-d.z, 0D, d.x);
        if (side.lengthSquared() < 1.0E-9) {
            return;
        }
        side = side.normalize().scale(width * 0.5D);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(a.x - side.x, a.y, a.z - side.z).color(col[0], col[1], col[2], 0F).endVertex();
        buffer.pos(a.x, a.y, a.z).color(col[0], col[1], col[2], alpha).endVertex();
        buffer.pos(b.x - side.x, b.y, b.z - side.z).color(col[0], col[1], col[2], 0F).endVertex();
        buffer.pos(b.x, b.y, b.z).color(col[0], col[1], col[2], alpha).endVertex();
        tessellator.draw();
        buffer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(a.x + side.x, a.y, a.z + side.z).color(col[0], col[1], col[2], 0F).endVertex();
        buffer.pos(a.x, a.y, a.z).color(col[0], col[1], col[2], alpha).endVertex();
        buffer.pos(b.x + side.x, b.y, b.z + side.z).color(col[0], col[1], col[2], 0F).endVertex();
        buffer.pos(b.x, b.y, b.z).color(col[0], col[1], col[2], alpha).endVertex();
        tessellator.draw();
    }

    /**
     * Runes of the elder futhark, each a few strokes in a unit square (x across, y up): ansuz (Odin's own), fehu,
     * thurisaz, raidho, kenaz, gebo, hagalaz, naudiz, tiwaz, algiz, sowilo, dagaz, othala.
     */
    private static final double[][][] RUNES = {
            {{0.3, 0, 0.3, 1}, {0.3, 1, 0.75, 0.72}, {0.3, 0.7, 0.75, 0.42}},
            {{0.3, 0, 0.3, 1}, {0.3, 0.75, 0.75, 1}, {0.3, 0.45, 0.75, 0.75}},
            {{0.3, 0, 0.3, 1}, {0.3, 0.75, 0.72, 0.5}, {0.72, 0.5, 0.3, 0.25}},
            {{0.3, 0, 0.3, 1}, {0.3, 1, 0.72, 0.78}, {0.72, 0.78, 0.3, 0.55}, {0.3, 0.55, 0.72, 0}},
            {{0.72, 1, 0.3, 0.5}, {0.3, 0.5, 0.72, 0}},
            {{0.2, 0, 0.8, 1}, {0.8, 0, 0.2, 1}},
            {{0.25, 0, 0.25, 1}, {0.75, 0, 0.75, 1}, {0.25, 0.65, 0.75, 0.35}},
            {{0.5, 0, 0.5, 1}, {0.28, 0.66, 0.72, 0.36}},
            {{0.5, 0, 0.5, 1}, {0.2, 0.7, 0.5, 1}, {0.5, 1, 0.8, 0.7}},
            {{0.5, 0, 0.5, 1}, {0.5, 0.55, 0.2, 1}, {0.5, 0.55, 0.8, 1}},
            {{0.72, 1, 0.3, 0.62}, {0.3, 0.62, 0.72, 0.38}, {0.72, 0.38, 0.3, 0}},
            {{0.2, 0, 0.2, 1}, {0.2, 1, 0.8, 0}, {0.8, 0, 0.8, 1}, {0.8, 1, 0.2, 0}},
            {{0.5, 1, 0.8, 0.62}, {0.8, 0.62, 0.5, 0.25}, {0.5, 0.25, 0.2, 0.62}, {0.2, 0.62, 0.5, 1}, {0.5, 0.25, 0.2, 0}, {0.5, 0.25, 0.8, 0}}
    };

    /** A rune lying on the ground at c (camera-relative), its foot toward the centre, size blocks tall. */
    static void rune(int which, Vec3d c, Vec3d along, Vec3d out, double size, double width, float[] col, float alpha) {
        double[][] strokes = RUNES[Math.floorMod(which, RUNES.length)];
        for (double[] s : strokes) {
            Vec3d a = c.add(along.scale((s[0] - 0.5D) * size * 0.7D)).add(out.scale((s[1] - 0.5D) * size));
            Vec3d b = c.add(along.scale((s[2] - 0.5D) * size * 0.7D)).add(out.scale((s[3] - 0.5D) * size));
            flatLine(a, b, width, col, alpha);
        }
    }

    /** A rune standing upright, facing the camera, at c: for what hangs in the air. */
    static void runeUp(int which, Vec3d c, double size, double width, float[] col, float alpha) {
        double[][] strokes = RUNES[Math.floorMod(which, RUNES.length)];
        float rx = ActiveRenderInfo.getRotationX(), rz = ActiveRenderInfo.getRotationZ();
        Vec3d right = new Vec3d(rx, 0D, rz);
        for (double[] s : strokes) {
            Vec3d a = c.add(right.scale((s[0] - 0.5D) * size * 0.7D)).add(0D, (s[1] - 0.5D) * size, 0D);
            Vec3d b = c.add(right.scale((s[2] - 0.5D) * size * 0.7D)).add(0D, (s[3] - 0.5D) * size, 0D);
            strip(a, b, width, col, alpha);
        }
    }

    /** A ring of runes on the ground round c, turning, between two radii. */
    static void runeRing(Vec3d c, double radius, int count, double size, double spin, long seed, float[] col, float alpha) {
        Random rand = new Random(seed);
        for (int i = 0; i < count; i++) {
            double a = spin + Math.PI * 2D * i / count;
            Vec3d out = new Vec3d(Math.cos(a), 0D, Math.sin(a));
            Vec3d along = new Vec3d(-out.z, 0D, out.x);
            rune(rand.nextInt(RUNES.length), c.add(out.scale(radius)), along, out, size, size * 0.12D, col, alpha);
        }
    }

    /** Light thrown off something in a jagged line, in his colours. */
    static void bolt(Vec3d a, Vec3d b, Random rand, double width, float[] col, float alpha) {
        Vec3d d = b.subtract(a);
        double len = d.length();
        if (len < 1.0E-3 || alpha <= 0.01F) {
            return;
        }
        Vec3d[] side = WarlockFx.basis(d.scale(1D / len));
        int steps = Math.max(3, (int) (len * 2D));
        Vec3d prev = a;
        for (int s = 1; s <= steps; s++) {
            double f = s / (double) steps;
            double jag = s == steps ? 0D : Math.sin(f * Math.PI) * len * 0.12D;
            Vec3d p = a.add(d.scale(f)).add(side[0].scale(rand.nextGaussian() * jag)).add(side[1].scale(rand.nextGaussian() * jag));
            strip(prev, p, width * 3D, col, 0.35F * alpha);
            strip(prev, p, width, WHITE, 0.9F * alpha);
            prev = p;
        }
    }

    /** Draws with ordinary blending: what darkens rather than lights (his ravens, the storm, a scorch). */
    static void dark(Runnable draw) {
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        draw.run();
        WarlockFx.additive();
    }

    // ------------------------------------------------------------------ the Bifrost

    /**
     * The Bifrost: a column of every colour from the top of the sky down to the ground. Coming down (dir 1) it falls
     * out of the sky in a few ticks and slams into the ground; going back up (dir -1) it is there at once. Either way it
     * draws back up into the sky at the end, its foot rising, and is gone.
     */
    public static class Bifrost extends FxRenderer.Fx {
        final Vec3d ground;
        final double radius;
        final boolean falls;
        final long seed;

        public Bifrost(Vec3d ground, double radius, int dir, int lifetime, long seed) {
            super(lifetime);
            this.ground = ground;
            this.radius = radius;
            this.falls = dir > 0;
            this.seed = seed;
        }

        @Override
        void render(Vec3d camera, float pt) {
            float t = age + pt;
            double top = ground.y + SKY;
            double fallen = falls ? Math.min(1D, Math.pow(t / 7D, 1.6D)) : 1D;
            double rise = smooth((t - (lifetime - 16)) / 16F);
            double bottom = ground.y + (top - ground.y) * rise;
            double front = top - (top - ground.y) * fallen;
            double foot = Math.max(bottom, front);
            if (foot >= top - 1D) {
                return;
            }
            float in = falls ? 1F : MathHelper.clamp(t / 3F, 0F, 1F);
            float out = (float) (1D - rise * rise);
            float a = in * Math.max(0.2F, out);
            Vec3d base = new Vec3d(ground.x, foot, ground.z).subtract(camera);
            Vec3d high = new Vec3d(ground.x, top, ground.z).subtract(camera);
            double r = radius * (1D + 0.06D * MathHelper.sin(t * 0.9F));
            // Outer haze, the bands of every colour sliding over each other, the inner light, the core.
            strip(base, high, r * 2.6D, ICE, 0.14F * a);
            for (int i = 0; i < 7; i++) {
                double wob = MathHelper.sin(t * 0.35F + i * 1.3F) * r * 0.12D;
                Vec3d off = new Vec3d(wob, 0D, -wob);
                strip(base.add(off), high.add(off), r * (1.55D - i * 0.14D), hue(t * 0.015D + i / 7D), 0.2F * a);
            }
            strip(base, high, r * 0.62D, CYAN, 0.55F * a);
            strip(base, high, r * 0.28D, WHITE, 0.95F * a);
            // Streaks of light pouring down it.
            double span = Math.min(top - foot, 90D);
            for (int k = 0; k < 14; k++) {
                double ang = k * 2.39996D + seed % 7;
                double y0 = foot + ((t * 4.5D + k * 37.3D) % span);
                Vec3d side = new Vec3d(Math.cos(ang) * r * 0.55D, 0D, Math.sin(ang) * r * 0.55D);
                Vec3d s1 = new Vec3d(ground.x, y0 + 6D, ground.z).add(side).subtract(camera);
                Vec3d s0 = new Vec3d(ground.x, y0, ground.z).add(side).subtract(camera);
                strip(s0, s1, r * 0.08D, k % 3 == 0 ? hue(k * 0.13D + t * 0.01D) : WHITE, 0.55F * a);
            }
            // Motes whirling about its foot.
            for (int k = 0; k < 46; k++) {
                double ang = k * 2.39996D + t * (0.08D + (k % 5) * 0.02D);
                double h = (k * 13.7D + t * (0.8D + (k % 3) * 0.5D)) % 22D;
                double rr = r * (1.1D + (k % 4) * 0.35D) + h * 0.05D;
                Vec3d m = new Vec3d(ground.x + Math.cos(ang) * rr, foot + h, ground.z + Math.sin(ang) * rr).subtract(camera);
                glow(m, 0.18D + (k % 3) * 0.06D, hue(k * 0.071D + t * 0.004D), 0.75F * a * (1F - (float) (h / 22D)));
            }
            if (front <= ground.y + 0.5D && rise < 0.98D) {
                // Where it meets the ground: blinding, pulsing outward.
                Vec3d g = new Vec3d(ground.x, ground.y + 0.1D, ground.z).subtract(camera);
                float hit = falls ? (float) Math.exp(-Math.max(0D, t - 7D) * 0.12D) : 0F;
                glow(g.add(0D, 0.8D, 0D), r * (2.4D + 4D * hit), WHITE, (0.6F + 0.4F * hit) * a);
                glow(g.add(0D, 0.5D, 0D), r * 3.5D, CYAN, 0.35F * a);
                for (int p = 0; p < 3; p++) {
                    double ph = ((t * 0.06D) + p / 3D) % 1D;
                    ring(g, UP, r * (1D + ph * 5D), r * 0.35D, hue(ph + t * 0.01D), 0.5F * (1F - (float) ph) * a);
                }
            }
        }
    }

    /** The Bifrost's seal, burned into the ground where it came down: rings and runes, white-hot, cooling to gold, to scorch. */
    public static class Seal extends FxRenderer.Fx {
        final Vec3d at;
        final double radius;
        final long seed;

        public Seal(Vec3d at, double radius, int lifetime, long seed) {
            super(lifetime);
            this.at = at;
            this.radius = radius;
            this.seed = seed;
        }

        @Override
        void render(Vec3d camera, float pt) {
            float t = age + pt;
            float p = progress(pt);
            float in = MathHelper.clamp(t / 6F, 0F, 1F);
            float heat = (float) Math.exp(-t / 90D);
            float fade = 1F - smooth((p - 0.7F) / 0.3F);
            float a = in * fade;
            Vec3d c = at.subtract(camera).add(0D, 0.06D, 0D);
            float[] col = mix(GOLD, mix(ICE, WHITE, 0.4F), heat);
            // The scorch under it all.
            dark(() -> WarlockFx.flat(c.add(0D, -0.01D, 0D), radius * 1.15D, SOOT, 0.55F * a));
            double spin = t * 0.004D;
            ring(c, UP, radius, 0.18D, col, 0.9F * a);
            ring(c, UP, radius * 0.93D, 0.08D, col, 0.7F * a);
            ring(c, UP, radius * 0.62D, 0.12D, col, 0.8F * a);
            ring(c, UP, radius * 0.3D, 0.1D, col, 0.7F * a);
            runeRing(c, radius * 0.78D, 18, radius * 0.17D, spin, seed, col, 0.85F * a);
            runeRing(c, radius * 0.46D, 10, radius * 0.12D, -spin * 1.5D, seed + 3L, col, 0.75F * a);
            // Knotwork between the rings: arcs that cross and cross back.
            for (int i = 0; i < 12; i++) {
                double a0 = Math.PI * 2D * i / 12D + spin;
                for (int s = 0; s < 6; s++) {
                    double f0 = s / 6D, f1 = (s + 1) / 6D;
                    double r0 = radius * (0.32D + 0.28D * (0.5D + 0.5D * Math.sin(f0 * Math.PI * 2D)));
                    double r1 = radius * (0.32D + 0.28D * (0.5D + 0.5D * Math.sin(f1 * Math.PI * 2D)));
                    Vec3d q0 = c.add(Math.cos(a0 + f0 * 0.6D) * r0, 0D, Math.sin(a0 + f0 * 0.6D) * r0);
                    Vec3d q1 = c.add(Math.cos(a0 + f1 * 0.6D) * r1, 0D, Math.sin(a0 + f1 * 0.6D) * r1);
                    flatLine(q0, q1, radius * 0.03D, col, 0.6F * a);
                }
            }
            if (heat > 0.2F) {
                glow(c.add(0D, 0.3D, 0D), radius * 1.2D, col, 0.25F * heat * a);
            }
        }
    }

    /** The sky opening over where the Bifrost will come down: storm cloud wheeling round an eye of every colour. */
    public static class Vortex extends FxRenderer.Fx {
        final Vec3d at;
        final double height, radius;
        final long seed;

        public Vortex(Vec3d at, double height, double radius, int lifetime, long seed) {
            super(lifetime);
            this.at = at;
            this.height = height;
            this.radius = radius;
            this.seed = seed;
        }

        @Override
        void render(Vec3d camera, float pt) {
            float t = age + pt;
            float p = progress(pt);
            float grow = smooth(p / 0.55F);
            float fade = 1F - smooth((p - 0.85F) / 0.15F);
            float a = grow * fade;
            double r = radius * (0.25D + 0.75D * grow);
            Vec3d c = at.add(0D, height, 0D).subtract(camera);
            double spin = t * 0.025D;
            // The cloud: dark bands turning, thickest at the rim.
            dark(() -> {
                for (int k = 0; k < 6; k++) {
                    ring(c, UP, r * (0.35D + k * 0.13D), r * 0.12D, SOOT, (0.25F + k * 0.06F) * a);
                }
            });
            // Its arms, lit from inside.
            for (int arm = 0; arm < 4; arm++) {
                double a0 = arm * Math.PI / 2D + spin;
                Vec3d prev = c;
                for (int s = 1; s <= 18; s++) {
                    double f = s / 18D;
                    double ang = a0 + f * 3.2D;
                    Vec3d q = c.add(Math.cos(ang) * r * f, -f * 4D, Math.sin(ang) * r * f);
                    strip(prev, q, r * 0.06D * (1.2D - f) + 0.3D, hue(f * 0.8D + t * 0.01D + arm * 0.25D), 0.4F * a * (float) (1D - f * 0.6D));
                    prev = q;
                }
            }
            glow(c, r * 0.45D, WHITE, 0.85F * a);
            glow(c, r * 0.9D, CYAN, 0.35F * a);
            star(c, r * 0.1D, r * (0.6D + 0.2D * MathHelper.sin(t * 0.3F)), 16, seed, (float) spin, ICE, 0.6F * a);
            // Lightning in it.
            Random rand = new Random(seed + age / 3);
            if (rand.nextInt(3) == 0) {
                double ang = rand.nextDouble() * Math.PI * 2D;
                bolt(c, c.add(Math.cos(ang) * r * 0.9D, -rand.nextDouble() * 4D, Math.sin(ang) * r * 0.9D), rand, 0.12D, CYAN, a);
            }
        }
    }

    /** Blinding white at a point, and a streak of light right across it, the way the eye sees too much light. */
    public static class Flare extends FxRenderer.Fx {
        final Vec3d at;
        final double size;
        final long seed;

        public Flare(Vec3d at, double size, long seed) {
            super(28);
            this.at = at;
            this.size = size;
            this.seed = seed;
        }

        @Override
        void render(Vec3d camera, float pt) {
            float p = progress(pt);
            float f = (float) Math.exp(-p * 4D);
            Vec3d c = at.subtract(camera);
            Vec3d right = new Vec3d(ActiveRenderInfo.getRotationX(), 0D, ActiveRenderInfo.getRotationZ());
            double len = size * (10D + 14D * (1D - f));
            glow(c, size * (1.5D + 2.5D * f), WHITE, 0.95F * f);
            glow(c, size * 4D, CYAN, 0.45F * f);
            strip(c.subtract(right.scale(len)), c.add(right.scale(len)), size * 0.35D, size * 0.35D, CYAN, CYAN, 0.7F * f, 0.7F * f);
            strip(c.subtract(right.scale(len * 0.6D)), c.add(right.scale(len * 0.6D)), size * 0.12D, WHITE, 0.95F * f);
            star(c, size * 0.3D, size * (2.5D + 2D * p), 12, seed, p, WHITE, 0.8F * f);
        }
    }

    // ------------------------------------------------------------------ Gungnir

    /** Gungnir in flight, told where it is every tick: a streak of gold with a white-hot point and runes in its wake. */
    public static class Spear extends FxRenderer.Fx {
        public final int id;
        Vec3d pos, prev, vel = Vec3d.ZERO;
        boolean returning;
        int sinceUpdate;
        final Deque<Vec3d> trail = new ArrayDeque<>();

        public Spear(int id, Vec3d pos) {
            super(100000);
            this.id = id;
            this.pos = this.prev = pos;
        }

        public void update(Vec3d at, Vec3d vel, boolean returning) {
            trail.addFirst(pos);
            while (trail.size() > 10) {
                trail.removeLast();
            }
            prev = pos;
            pos = at;
            this.vel = vel;
            this.returning = returning;
            sinceUpdate = 0;
        }

        public void end() {
            age = lifetime;
        }

        @Override
        void tick() {
            if (++sinceUpdate > 4) {
                end();
            }
        }

        @Override
        void render(Vec3d camera, float pt) {
            Vec3d p = prev.add(pos.subtract(prev).scale(pt)).subtract(camera);
            Vec3d dir = vel.lengthSquared() < 1.0E-6 ? UP : vel.normalize();
            Vec3d tail = p.subtract(dir.scale(3.4D)), head = p.add(dir.scale(0.6D));
            strip(tail, head, 0.06D, 0.32D, GOLD, GOLD, 0.15F, 0.65F);
            strip(tail, head, 0.03D, 0.1D, PALE, WHITE, 0.4F, 0.95F);
            glow(head, 0.55D, WHITE, 0.9F);
            glow(head, 1.4D, GOLD, 0.4F);
            Vec3d last = tail;
            int i = 0;
            for (Vec3d q : trail) {
                Vec3d qc = q.subtract(camera).subtract(dir.scale(3.4D));
                float k = 1F - i / 10F;
                strip(last, qc, 0.25D * k, GOLD, 0.35F * k);
                if (i % 2 == 0) {
                    runeUp(id * 7 + i, qc, 0.45D * k, 0.04D, PALE, 0.6F * k);
                }
                last = qc;
                i++;
            }
        }
    }

    /** Gungnir striking: a burst of white-gold, a ring of runes thrown out from it, lightning. */
    public static class SpearHit extends FxRenderer.Fx {
        final Vec3d at;
        final double size;
        final long seed;

        public SpearHit(Vec3d at, double size, long seed) {
            super(22);
            this.at = at;
            this.size = size;
            this.seed = seed;
        }

        @Override
        void render(Vec3d camera, float pt) {
            float p = progress(pt);
            float fade = 1F - p;
            float flash = Math.max(0F, 1F - p * 3F);
            Vec3d c = at.subtract(camera);
            glow(c, size * (1.2D + 2D * flash), WHITE, 0.95F * flash);
            glow(c, size * 2.8D, GOLD, 0.5F * fade);
            star(c, size * 0.2D, size * (1.5D + 2D * p), 14, seed, p * 0.5F, PALE, 0.85F * fade);
            double grow = 1D - Math.pow(1D - p, 2.5D);
            ring(c, UP, size * (0.4D + 2.6D * grow), size * 0.3D, GOLD, 0.7F * fade);
            for (int i = 0; i < 8; i++) {
                double ang = Math.PI * 2D * i / 8D + seed % 5;
                Vec3d q = c.add(Math.cos(ang) * size * (0.6D + 2.4D * grow), 0.3D + grow, Math.sin(ang) * size * (0.6D + 2.4D * grow));
                runeUp((int) (seed + i), q, size * 0.5D, 0.05D, PALE, 0.8F * fade);
            }
            Random rand = new Random(seed + age / 2);
            for (int i = 0; i < 4; i++) {
                Vec3d d = new Vec3d(rand.nextGaussian(), rand.nextGaussian() * 0.6D + 0.3D, rand.nextGaussian()).normalize();
                bolt(c, c.add(d.scale(size * (1.5D + rand.nextDouble() * 1.5D))), rand, 0.05D, GOLD, fade);
            }
        }
    }

    // ------------------------------------------------------------------ the Odinforce

    /** The Odinforce let go into the ground: a ring of it running outward, a wall of light at its front, runes riding it. */
    public static class Ring extends FxRenderer.Fx {
        final Vec3d at;
        final double max;
        final long seed;

        public Ring(Vec3d at, double max, int ticks, long seed) {
            super(ticks + 10);
            this.at = at;
            this.max = max;
            this.seed = seed;
        }

        @Override
        void render(Vec3d camera, float pt) {
            float t = age + pt;
            float run = MathHelper.clamp(t / (lifetime - 10), 0F, 1F);
            float fade = 1F - smooth((t - (lifetime - 12)) / 12F);
            double r = 1D + run * (max - 1D);
            Vec3d c = at.subtract(camera).add(0D, 0.08D, 0D);
            ring(c, UP, r, 0.9D, PALE, 0.9F * fade);
            ring(c, UP, r * 0.97D, 0.3D, WHITE, 0.95F * fade);
            ring(c, UP, r * 0.8D, 0.5D, GOLD, 0.35F * fade);
            ring(c, UP, r * 0.55D, 0.4D, GOLD, 0.18F * fade);
            int n = (int) Math.max(12, r * 4D);
            for (int i = 0; i < n; i++) {
                double ang = Math.PI * 2D * i / n;
                Vec3d foot = c.add(Math.cos(ang) * r, 0D, Math.sin(ang) * r);
                strip(foot, foot.add(0D, 1.2D + 0.4D * MathHelper.sin(t * 0.8F + i), 0D), 0.35D, 0.05D, GOLD, PALE, 0.55F * fade, 0F);
            }
            runeRing(c, r - 0.6D, (int) Math.max(6, r * 1.2D), 0.9D, t * 0.01D, seed, PALE, 0.8F * fade);
        }
    }

    /** The Odinforce gathering on Gungnir's blade: motes drawn in to it, a ring of runes turning round it, lightning. */
    public static class Gather extends FxRenderer.Fx {
        final int entity;
        final long seed;

        public Gather(int entity, int ticks, long seed) {
            super(ticks);
            this.entity = entity;
            this.seed = seed;
        }

        @Override
        void render(Vec3d camera, float pt) {
            Entity e = Minecraft.getMinecraft().world.getEntityByID(entity);
            if (!(e instanceof EntityOdin)) {
                return;
            }
            float t = age + pt;
            float p = progress(pt);
            float fade = 1F - smooth((p - 0.85F) / 0.15F);
            Vec3d blade = ((EntityOdin) e).bladeAt().subtract(camera).add(lerpOffset(e, pt));
            glow(blade, 0.6D + 1.4D * p, WHITE, (0.5F + 0.4F * p) * fade);
            glow(blade, 1.6D + 2.5D * p, GOLD, 0.35F * fade);
            star(blade, 0.15D, 1.2D + 2.2D * p, 10, seed + age / 2, t * 0.05F, PALE, 0.6F * fade);
            Random rand = new Random(seed);
            for (int i = 0; i < 24; i++) {
                double phase = (rand.nextDouble() + t * (0.03D + rand.nextDouble() * 0.03D)) % 1D;
                Vec3d d = new Vec3d(rand.nextGaussian(), rand.nextGaussian(), rand.nextGaussian()).normalize();
                Vec3d q = blade.add(d.scale((1D - phase) * 5D));
                glow(q, 0.12D + 0.1D * phase, i % 2 == 0 ? PALE : GOLD, 0.8F * (float) phase * fade);
            }
            for (int i = 0; i < 8; i++) {
                double ang = Math.PI * 2D * i / 8D + t * 0.08D;
                runeUp((int) seed + i, blade.add(Math.cos(ang) * 1.6D, Math.sin(t * 0.1D + i) * 0.4D, Math.sin(ang) * 1.6D), 0.45D, 0.04D, PALE, 0.7F * fade);
            }
            Random bolts = new Random(seed + age / 2);
            for (int i = 0; i < 2; i++) {
                Vec3d d = new Vec3d(bolts.nextGaussian(), bolts.nextGaussian(), bolts.nextGaussian()).normalize();
                bolt(blade, blade.add(d.scale(1.5D + bolts.nextDouble() * 2D * p)), bolts, 0.04D, GOLD, fade * p);
            }
        }
    }

    /** Where Odin is between ticks, against where he was last tick: what is drawn on him follows him smoothly. */
    private static Vec3d lerpOffset(Entity e, float pt) {
        return new Vec3d((e.lastTickPosX - e.posX) * (1F - pt), (e.lastTickPosY - e.posY) * (1F - pt), (e.lastTickPosZ - e.posZ) * (1F - pt));
    }

    // ------------------------------------------------------------------ the Bifrost, struck down

    /** Where it will come down: a ring of every colour on the ground, a ring inside it closing on its middle, runes. */
    public static class Mark extends FxRenderer.Fx {
        final Vec3d at;
        final double radius;
        final long seed;

        public Mark(Vec3d at, double radius, int ticks, long seed) {
            super(ticks);
            this.at = at;
            this.radius = radius;
            this.seed = seed;
        }

        @Override
        void render(Vec3d camera, float pt) {
            float t = age + pt;
            float p = progress(pt);
            Vec3d c = at.subtract(camera).add(0D, 0.07D, 0D);
            float pulse = 0.7F + 0.3F * MathHelper.sin(t * (0.4F + p));
            float[] col = hue(t * 0.02D + seed % 10 * 0.1D);
            ring(c, UP, radius, 0.15D, col, 0.85F * pulse);
            ring(c, UP, radius * (1D - p * 0.92D), 0.12D, WHITE, 0.9F);
            runeRing(c, radius * 0.7D, 8, radius * 0.25D, t * 0.03D, seed, col, 0.7F * pulse);
            glow(c.add(0D, 0.2D, 0D), radius * 0.8D, col, 0.2F + 0.3F * p);
            // A thread of it already standing up into the sky, brightening.
            strip(c, c.add(0D, 60D, 0D), radius * 0.12D * p, col, 0.25F * p);
        }
    }

    /** The Bifrost struck down on one mark: a narrow column of it, a flash, a ring burned out from it. */
    public static class Strike extends FxRenderer.Fx {
        final Vec3d at;
        final double radius;
        final long seed;

        public Strike(Vec3d at, double radius, long seed) {
            super(20);
            this.at = at;
            this.radius = radius;
            this.seed = seed;
        }

        @Override
        void render(Vec3d camera, float pt) {
            float t = age + pt;
            float p = progress(pt);
            float a = (float) Math.exp(-t * 0.18D);
            Vec3d g = at.subtract(camera).add(0D, 0.1D, 0D);
            Vec3d top = g.add(0D, SKY, 0D);
            double r = radius * (0.7D + 0.6D * a);
            strip(g, top, r * 2.2D, ICE, 0.2F * a);
            for (int i = 0; i < 5; i++) {
                strip(g, top, r * (1.3D - i * 0.18D), hue(t * 0.03D + i / 5D), 0.25F * a);
            }
            strip(g, top, r * 0.35D, WHITE, 0.95F * a);
            glow(g.add(0D, 0.6D, 0D), r * 3D, WHITE, 0.8F * a);
            double grow = 1D - Math.pow(1D - p, 2.5D);
            ring(g, UP, r * (0.5D + 3D * grow), r * 0.4D, hue(p + seed % 10 * 0.1D), 0.7F * (1F - p));
            if (p > 0.15F) {
                dark(() -> WarlockFx.flat(g.add(0D, -0.04D, 0D), radius * 1.1D, SOOT, 0.5F * (1F - p)));
            }
        }
    }

    // ------------------------------------------------------------------ Huginn and Muninn

    /** One of his ravens, told where it is every tick: black, wings beating, an eye like a coal. */
    public static class Raven extends FxRenderer.Fx {
        public final int id;
        Vec3d pos, prev, vel = new Vec3d(0D, 0D, 1D);
        int sinceUpdate, peck = -100;
        final Deque<Vec3d> trail = new ArrayDeque<>();

        public Raven(int id, Vec3d pos) {
            super(100000);
            this.id = id;
            this.pos = this.prev = pos;
        }

        public void update(Vec3d at, Vec3d vel, boolean pecked) {
            trail.addFirst(pos);
            while (trail.size() > 6) {
                trail.removeLast();
            }
            prev = pos;
            pos = at;
            if (vel.lengthSquared() > 1.0E-6) {
                this.vel = vel;
            }
            if (pecked) {
                peck = age;
            }
            sinceUpdate = 0;
        }

        public void end() {
            age = lifetime;
        }

        @Override
        void tick() {
            if (++sinceUpdate > 4) {
                end();
            }
        }

        @Override
        void render(Vec3d camera, float pt) {
            float t = age + pt;
            Vec3d c = prev.add(pos.subtract(prev).scale(pt)).subtract(camera);
            Vec3d fwd = vel.normalize();
            Vec3d across = fwd.crossProduct(UP);
            Vec3d side = across.lengthSquared() < 1.0E-6 ? new Vec3d(1D, 0D, 0D) : across.normalize();
            Vec3d up = side.crossProduct(fwd).normalize();
            double flap = Math.sin(t * 1.3D + id) * 0.9D;
            double span = 0.75D;
            Vec3d wingL = c.add(side.scale(span * Math.cos(flap))).add(up.scale(span * Math.sin(flap))).subtract(fwd.scale(0.15D));
            Vec3d wingR = c.subtract(side.scale(span * Math.cos(flap))).add(up.scale(span * Math.sin(flap))).subtract(fwd.scale(0.15D));
            Vec3d head = c.add(fwd.scale(0.32D)).add(up.scale(0.04D));
            Vec3d tail = c.subtract(fwd.scale(0.42D));
            dark(() -> {
                // A haze of shadow behind it.
                Vec3d last = tail;
                int i = 0;
                for (Vec3d q : trail) {
                    Vec3d qc = q.subtract(camera);
                    strip(last, qc, 0.18D * (1D - i / 6D), SOOT, 0.25F * (1F - i / 6F));
                    last = qc;
                    i++;
                }
                triangle(c, wingL, c.subtract(fwd.scale(0.3D)), SOOT, 0.95F);
                triangle(c, wingR, c.subtract(fwd.scale(0.3D)), SOOT, 0.95F);
                triangle(c.add(fwd.scale(0.1D)), wingL, wingL.subtract(fwd.scale(0.25D)).add(side.scale(0.05D)), SOOT, 0.9F);
                triangle(c.add(fwd.scale(0.1D)), wingR, wingR.subtract(fwd.scale(0.25D)).subtract(side.scale(0.05D)), SOOT, 0.9F);
                strip(head, tail, 0.13D, SOOT, 0.98F);
                strip(tail, tail.subtract(fwd.scale(0.25D)), 0.12D, 0.2D, SOOT, SOOT, 0.95F, 0.7F);
                strip(head, head.add(fwd.scale(0.18D)), 0.05D, 0.01D, SOOT, SOOT, 1F, 1F);
            });
            glow(head.add(side.scale(0.06D)), 0.06D, GOLD, 0.9F);
            float since = age - peck + pt;
            if (since >= 0F && since < 8F) {
                float k = 1F - since / 8F;
                glow(head.add(fwd.scale(0.3D)), 0.6D * k, PALE, 0.7F * k);
            }
        }
    }

    private static void triangle(Vec3d a, Vec3d b, Vec3d c, float[] col, float alpha) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(a.x, a.y, a.z).color(col[0], col[1], col[2], alpha).endVertex();
        buffer.pos(b.x, b.y, b.z).color(col[0], col[1], col[2], alpha * 0.85F).endVertex();
        buffer.pos(c.x, c.y, c.z).color(col[0], col[1], col[2], alpha).endVertex();
        tessellator.draw();
    }

    // ------------------------------------------------------------------ Sleipnir

    /** Sleipnir's hooves coming down: the ground broken in a ring and gold dust thrown up; or, running, runes where he trod. */
    public static class Stomp extends FxRenderer.Fx {
        final Vec3d at;
        final double radius;
        final boolean step;
        final long seed;

        public Stomp(Vec3d at, double radius, boolean step, long seed) {
            super(step ? 30 : 24);
            this.at = at;
            this.radius = radius;
            this.step = step;
            this.seed = seed;
        }

        @Override
        void render(Vec3d camera, float pt) {
            float p = progress(pt);
            float fade = 1F - p;
            Vec3d c = at.subtract(camera).add(0D, 0.05D, 0D);
            if (step) {
                // A rune of light where a hoof came down on the air or the ground, fading.
                Random rand = new Random(seed);
                Vec3d out = new Vec3d(rand.nextGaussian(), 0D, rand.nextGaussian());
                out = out.lengthSquared() < 1.0E-6 ? new Vec3d(1D, 0D, 0D) : out.normalize();
                rune(rand.nextInt(RUNES.length), c, new Vec3d(-out.z, 0D, out.x), out, radius, 0.08D, GOLD, 0.8F * fade);
                glow(c, radius * 0.8D, GOLD, 0.25F * fade);
                return;
            }
            double grow = 1D - Math.pow(1D - p, 2.2D);
            ring(c, UP, radius * (0.3D + grow), radius * 0.18D, GOLD, 0.75F * fade);
            ring(c, UP, radius * (0.2D + grow * 0.7D), radius * 0.1D, PALE, 0.6F * fade);
            dark(() -> {
                Random rand = new Random(seed);
                for (int i = 0; i < 14; i++) {
                    double ang = rand.nextDouble() * Math.PI * 2D;
                    double len = radius * (0.4D + rand.nextDouble() * 0.7D);
                    Vec3d a = c.add(Math.cos(ang) * 0.6D, 0D, Math.sin(ang) * 0.6D);
                    Vec3d b = c.add(Math.cos(ang) * len, 0D, Math.sin(ang) * len);
                    flatLine(a, b, 0.18D, SOOT, 0.8F * (1F - p * 0.7F));
                }
            });
            glow(c.add(0D, 0.5D, 0D), radius * 0.9D, PALE, 0.6F * (float) Math.exp(-p * 6D));
        }
    }

    // ------------------------------------------------------------------ drawn to his hand, and gone

    /** A thread of gold from something to him: what he means to have, drawn to his hand. */
    public static class Pull extends FxRenderer.Fx {
        final int from, to;
        final Vec3d fromAt;
        final long seed;

        public Pull(int from, Vec3d fromAt, int to, int ticks, long seed) {
            super(ticks);
            this.from = from;
            this.fromAt = fromAt;
            this.to = to;
            this.seed = seed;
        }

        @Override
        void render(Vec3d camera, float pt) {
            Entity target = Minecraft.getMinecraft().world.getEntityByID(to);
            if (target == null) {
                return;
            }
            Entity source = from >= 0 ? Minecraft.getMinecraft().world.getEntityByID(from) : null;
            Vec3d a = (source != null ? source.getPositionVector().add(0D, source.height * 0.6D, 0D) : fromAt).subtract(camera);
            Vec3d b = (target instanceof EntityOdin ? ((EntityOdin) target).chest() : target.getPositionVector().add(0D, target.height * 0.6D, 0D))
                    .subtract(camera).add(lerpOffset(target, pt));
            float t = age + pt;
            float fade = 1F - smooth((progress(pt) - 0.8F) / 0.2F);
            strip(a, b, 0.25D, GOLD, 0.35F * fade);
            strip(a, b, 0.06D, PALE, 0.8F * fade);
            for (int i = 0; i < 10; i++) {
                double f = ((i / 10D) + t * 0.05D) % 1D;
                glow(a.add(b.subtract(a).scale(f)), 0.2D, PALE, 0.8F * fade);
            }
            glow(a, 0.8D, GOLD, 0.5F * fade);
        }
    }

    // ------------------------------------------------------------------ easing

    private static float smooth(float x) {
        x = MathHelper.clamp(x, 0F, 1F);
        return x * x * (3F - 2F * x);
    }

    private static float smooth(double x) {
        return smooth((float) x);
    }
}
