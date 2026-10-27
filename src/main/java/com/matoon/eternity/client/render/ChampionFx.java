package com.matoon.eternity.client.render;

import com.matoon.eternity.entity.EntityChampion;
import net.minecraft.client.Minecraft;
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

import java.util.Random;

/**
 * The Champion's effects: what his fists, his feet and his temper do to the world around him.
 * Raw force is drawn in white and the colour of the ground it throws up; anything of the stone
 * in his brow is purple. Positions are world coordinates; geometry is drawn relative to the
 * camera, with Adam Warlock's shapes, which fade as they come near your eye.
 */
@SideOnly(Side.CLIENT)
public final class ChampionFx {

    // The stone's colours, as InfinityCraft draws the Power Stone, and the white heat of a blow.
    static final float[] VIOLET = {0.765F, 0.165F, 0.82F};
    static final float[] MAGENTA = {0.945F, 0.165F, 1F};
    static final float[] PALE = {1F, 0.82F, 1F};
    static final float[] WHITE = {1F, 0.98F, 0.95F};
    static final float[] HEAT = {1F, 0.88F, 0.7F};
    static final float[] DUST = {0.82F, 0.52F, 0.36F};
    static final float[] DUST_DARK = {0.5F, 0.3F, 0.2F};
    static final float[] FLAME = {1F, 0.6F, 0.18F};

    private static final Vec3d UP = new Vec3d(0D, 1D, 0D);

    private ChampionFx() {
    }

    // ------------------------------------------------------------------ blows

    /** A blow landing: a flash, a burst of spikes, and a ring of force driven on through what it hit. Kind 0 a jab, 1 big, 2 a haymaker. */
    public static class Impact extends FxRenderer.Fx {
        final Vec3d at, dir;
        final int kind;
        final long seed;

        public Impact(Vec3d at, Vec3d dir, int kind, long seed) {
            super(kind == 0 ? 8 : kind == 1 ? 14 : 18);
            this.at = at;
            this.dir = dir.lengthSquared() < 1.0E-6 ? UP : dir.normalize();
            this.kind = kind;
            this.seed = seed;
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            float p = progress(partialTicks);
            float fade = 1F - p;
            double size = kind == 0 ? 0.6D : kind == 1 ? 1.05D : 1.5D;
            Vec3d c = at.subtract(camera);
            float flash = Math.max(0F, 1F - p * 3F);
            WarlockFx.glow(c, size * (1D + 1.4D * flash), WHITE, 0.9F * flash);
            WarlockFx.glow(c, size * 2.4D, HEAT, 0.3F * fade);
            WarlockFx.star(c, size * 0.25D, size * (1.3D + p * 1.4D), 14, seed, (age + partialTicks) * 0.05F, WHITE, 0.85F * fade);
            WarlockFx.star(c, size * 0.2D, size * (1.9D + p * 1.8D), 9, seed + 7L, -(age + partialTicks) * 0.03F, PALE, 0.35F * fade);
            double grow = 1D - Math.pow(1D - p, 2.5D);
            WarlockFx.ring(c.add(dir.scale(grow * size * 1.5D)), dir, size * (0.5D + 2.8D * grow), size * 0.55D, WHITE, 0.7F * fade);
            if (kind > 0) {
                WarlockFx.ring(c.add(dir.scale(grow * size * 3.2D)), dir, size * (0.35D + 2D * grow), size * 0.45D, HEAT, 0.55F * fade);
            }
            if (kind == 2) {
                WarlockFx.ring(c.add(dir.scale(grow * size * 5D)), dir, size * (0.3D + 1.4D * grow), size * 0.4D, MAGENTA, 0.4F * fade);
            }
        }
    }

    /**
     * Your blow meets his guard (0): a hard white flash against his forearms; or he slips it
     * (1): only the whistle of air where he was; or catches it and turns it (2), a purple flare.
     */
    public static class Guard extends FxRenderer.Fx {
        final Vec3d at, dir;
        final int how;
        final long seed;

        public Guard(Vec3d at, Vec3d dir, int how, long seed) {
            super(how == 1 ? 9 : 11);
            this.at = at;
            this.dir = dir.lengthSquared() < 1.0E-6 ? UP : dir.normalize();
            this.how = how;
            this.seed = seed;
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            float p = progress(partialTicks);
            float fade = 1F - p;
            Vec3d c = at.subtract(camera);
            if (how == 1) {
                // A sweep of displaced air, curving past him.
                Vec3d[] b = WarlockFx.basis(dir);
                Vec3d last = null;
                for (int i = 0; i <= 10; i++) {
                    double a = -1.2D + 2.4D * i / 10D;
                    Vec3d pt = c.add(b[0].scale(Math.sin(a) * 1.4D)).add(dir.scale(Math.cos(a) * 0.9D - 0.4D)).add(0D, -0.3D + 0.6D * p, 0D);
                    if (last != null) {
                        WarlockFx.strip(last, pt, 0.06D, WHITE, 0.55F * fade * MathHelper.sin(i / 10F * (float) Math.PI));
                    }
                    last = pt;
                }
                return;
            }
            float flash = Math.max(0F, 1F - p * 3.5F);
            float[] col = how == 2 ? MAGENTA : WHITE;
            WarlockFx.glow(c, 0.5D + 0.9D * flash, col, 0.85F * flash);
            WarlockFx.ring(c, dir, 0.3D + 1.1D * (1D - Math.pow(1D - p, 2D)), 0.25D, col, 0.65F * fade);
            WarlockFx.star(c, 0.08D, 0.55D + 0.5D * p, 8, seed, 0F, how == 2 ? PALE : HEAT, 0.8F * fade);
        }
    }

    // ------------------------------------------------------------------ the ground

    /**
     * The ground he struck, thrown outward: rings of dust racing across it and lifting, a hard
     * white edge on the first, and the air shimmering above it.
     */
    public static class Quake extends FxRenderer.Fx {
        final Vec3d at;
        final double reach;
        final int rings;

        public Quake(Vec3d at, double reach, int rings, int ticks) {
            super(ticks);
            this.at = at;
            this.reach = reach;
            this.rings = Math.max(1, rings);
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            float p = progress(partialTicks);
            Vec3d c = at.subtract(camera).add(0D, 0.15D, 0D);
            for (int i = 0; i < rings; i++) {
                float q = MathHelper.clamp(p * 1.5F - i * 0.16F, 0F, 1F);
                if (q <= 0F || q >= 1F) {
                    continue;
                }
                double grow = 1D - Math.pow(1D - q, 2.6D);
                double r = reach * (1D + i * 0.35D) * grow;
                float fade = (1F - q) * (1F - q);
                // The dust itself, opaque-ish, lifting as it goes.
                GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                wall(c, r, 0.4D + 1.3D * q * (1D - i * 0.2D), 0.9D + i * 0.3D, i == 0 ? DUST : DUST_DARK, 0.55F * fade);
                WarlockFx.additive();
                if (i == 0) {
                    WarlockFx.ring(c, UP, r, 0.35D, WHITE, 0.55F * fade);
                }
            }
            float flash = Math.max(0F, 1F - p * 4F);
            WarlockFx.glow(c.add(0D, 0.5D, 0D), reach * 0.5D * flash + 0.5D, HEAT, 0.6F * flash);
        }
    }

    /**
     * Fissures torn across the ground, running outward, glowing the purple of the stone deep down
     * and cooling to black at the edges as they fade.
     */
    public static class Crack extends FxRenderer.Fx {
        final Vec3d at;
        final double reach;
        final long seed;
        final double[][][] lines;

        public Crack(Vec3d at, double reach, long seed, int ticks) {
            super(ticks);
            this.at = at;
            this.reach = reach;
            this.seed = seed;
            Random rand = new Random(seed);
            int count = 7 + (int) (reach / 2.5D);
            lines = new double[count][][];
            for (int i = 0; i < count; i++) {
                double a = i * Math.PI * 2D / count + rand.nextDouble() * 0.4D;
                double length = reach * (0.55D + rand.nextDouble() * 0.6D);
                int steps = 4 + (int) (length / 1.2D);
                lines[i] = new double[steps + 1][];
                double x = 0D, z = 0D;
                lines[i][0] = new double[]{0D, 0D, 0D};
                for (int s = 1; s <= steps; s++) {
                    a += (rand.nextDouble() - 0.5D) * 0.7D;
                    double step = length / steps;
                    x += Math.cos(a) * step;
                    z += Math.sin(a) * step;
                    lines[i][s] = new double[]{x, z, s / (double) steps};
                }
            }
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            float p = progress(partialTicks);
            float open = Math.min(1F, (age + partialTicks) / 6F);
            float fade = 1F - smooth((p - 0.5F) / 0.5F);
            Vec3d c = at.subtract(camera).add(0D, 0.06D, 0D);
            for (double[][] line : lines) {
                for (int s = 1; s < line.length; s++) {
                    double along = line[s][2];
                    if (along > open) {
                        break;
                    }
                    Vec3d a = c.add(line[s - 1][0], 0D, line[s - 1][1]);
                    Vec3d b = c.add(line[s][0], 0D, line[s][1]);
                    double width = 0.22D * (1D - along * 0.7D);
                    // Scorched black, then the glow inside.
                    GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                    flatStrip(a, b, width * 2.2D, DUST_DARK, 0.7F * fade);
                    WarlockFx.additive();
                    float hot = (float) (1D - along) * (1F - p * 0.8F);
                    flatStrip(a, b, width, p < 0.25F ? PALE : MAGENTA, 0.9F * fade * (0.4F + 0.6F * hot));
                }
            }
            WarlockFx.flat(c, reach * 0.35D, VIOLET, 0.35F * fade * (1F - p));
        }
    }

    // ------------------------------------------------------------------ his voice

    /** The roar: the air itself thrown back from him in shells, and the stone flaring. */
    public static class Roar extends FxRenderer.Fx {
        final Vec3d fallback;
        final double reach;
        final int entityId;

        public Roar(Vec3d at, double reach, int entityId, int ticks) {
            super(ticks);
            this.fallback = at;
            this.reach = reach;
            this.entityId = entityId;
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            float p = progress(partialTicks);
            Vec3d centre = brow(entityId, partialTicks, fallback);
            Vec3d c = centre.subtract(camera);
            for (int i = 0; i < 4; i++) {
                float q = MathHelper.clamp(p * 1.6F - i * 0.18F, 0F, 1F);
                if (q <= 0F || q >= 1F) {
                    continue;
                }
                double r = reach * (1D - Math.pow(1D - q, 2D));
                float fade = (1F - q) * (1F - q) * 0.4F;
                // Shells of shocked air in three planes, so they read as a sphere from anywhere.
                WarlockFx.ring(c, UP, r, 0.2D + r * 0.05D, PALE, fade);
                WarlockFx.ring(c, c.lengthSquared() < 1.0E-4 ? UP : c.normalize(), r, 0.3D + r * 0.06D, WHITE, fade * 0.8F);
                WarlockFx.ring(c, new Vec3d(1D, 0D, 0D), r * 0.92D, 0.2D, MAGENTA, fade * 0.5F);
            }
            float flash = Math.max(0F, 1F - p * 2.5F);
            WarlockFx.glow(c, 1.5D + 2D * flash, MAGENTA, 0.5F * flash);
        }
    }

    /** He leaves the ground: a ring of dust where he jumped from; for Planet Breaker, a column of purple light too. */
    public static class Leap extends FxRenderer.Fx {
        final Vec3d from, to;
        final int kind;

        public Leap(Vec3d from, Vec3d to, int kind, int ticks) {
            super(Math.max(ticks, 20));
            this.from = from;
            this.to = to;
            this.kind = kind;
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            float p = progress(partialTicks);
            float fade = 1F - p;
            Vec3d c = from.subtract(camera).add(0D, 0.2D, 0D);
            double grow = 1D - Math.pow(1D - Math.min(1F, p * 2F), 3D);
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            wall(c, 1.5D + 3.5D * grow, 0.3D + 1.2D * grow, 1.2D, DUST, 0.5F * fade * fade);
            WarlockFx.additive();
            WarlockFx.ring(c, UP, 1D + 4D * grow, 0.3D, WHITE, 0.5F * fade * fade);
            if (kind == 1) {
                Vec3d top = to.subtract(camera);
                WarlockFx.strip(c, top, 0.9D * fade, VIOLET, 0.35F * fade);
                WarlockFx.strip(c, top, 0.25D, PALE, 0.6F * fade);
            }
        }
    }

    // ------------------------------------------------------------------ the stone

    /** Lightning of the stone's purple, crawling out of his brow and over his shoulders. Never the same twice. */
    public static class Arcs extends FxRenderer.Fx {
        final int entityId;
        final float wild;
        final Vec3d fallback;

        public Arcs(Vec3d at, int entityId, float wild, int ticks) {
            super(ticks);
            this.fallback = at;
            this.entityId = entityId;
            this.wild = wild;
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            float p = progress(partialTicks);
            float fade = Math.min(1F, (age + partialTicks) / 4F) * (1F - smooth((p - 0.7F) / 0.3F));
            Vec3d brow = brow(entityId, partialTicks, fallback);
            Vec3d c = brow.subtract(camera);
            Random rand = new Random(entityId * 31L + age / 2 * 7919L);
            int count = 2 + (int) (wild * 6F);
            for (int i = 0; i < count; i++) {
                // Down over his body, or out into the air.
                Vec3d d = new Vec3d(rand.nextGaussian(), rand.nextGaussian() * 0.8D - (i % 2 == 0 ? 0.9D : 0D), rand.nextGaussian()).normalize();
                double length = 0.8D + rand.nextDouble() * (1.2D + 2.4D * wild);
                Vec3d last = c;
                int steps = 5;
                for (int s = 1; s <= steps; s++) {
                    Vec3d next = c.add(d.scale(length * s / steps)).add(rand.nextGaussian() * 0.15D, rand.nextGaussian() * 0.15D, rand.nextGaussian() * 0.15D);
                    float a = fade * (1F - s / (float) (steps + 1));
                    WarlockFx.strip(last, next, 0.09D, MAGENTA, 0.5F * a);
                    WarlockFx.strip(last, next, 0.03D, PALE, 0.9F * a);
                    last = next;
                }
            }
            WarlockFx.glow(c, 0.45D + 0.35D * wild, MAGENTA, 0.5F * fade);
        }
    }

    /** The stone's light, torn from his brow and flying in an arc to the hand that bought it. */
    public static class StoneFlight extends FxRenderer.Fx {
        final Vec3d from;
        final int targetId;

        public StoneFlight(Vec3d from, int targetId, int ticks) {
            super(ticks + 14);
            this.from = from;
            this.targetId = targetId;
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            Entity target = Minecraft.getMinecraft().world.getEntityByID(targetId);
            Vec3d to = target == null ? from : new Vec3d(target.lastTickPosX + (target.posX - target.lastTickPosX) * partialTicks,
                    target.lastTickPosY + (target.posY - target.lastTickPosY) * partialTicks + target.height * 0.55D,
                    target.lastTickPosZ + (target.posZ - target.lastTickPosZ) * partialTicks);
            int flight = lifetime - 14;
            float t = age + partialTicks;
            if (t < flight) {
                float p = smooth(t / flight);
                Vec3d prev = null;
                for (int i = 0; i <= 8; i++) {
                    float q = Math.max(0F, p - i * 0.025F);
                    Vec3d pos = arc(to, q).subtract(camera);
                    if (prev != null) {
                        WarlockFx.strip(prev, pos, 0.18D * (1D - i / 9D), MAGENTA, 0.7F * (1F - i / 9F));
                    }
                    prev = pos;
                }
                Vec3d c = arc(to, p).subtract(camera);
                float flicker = 0.85F + 0.15F * MathHelper.sin(t * 2.3F);
                WarlockFx.glow(c, 1.1D * flicker, MAGENTA, 0.75F);
                WarlockFx.glow(c, 0.4D, WHITE, 1F);
                WarlockFx.star(c, 0.1D, 0.9D * flicker, 8, targetId * 13L, t * 0.15F, PALE, 0.7F);
            } else {
                float b = (t - flight) / 14F;
                Vec3d c = to.subtract(camera);
                WarlockFx.glow(c, 0.6D + 2.2D * b, MAGENTA, 0.8F * (1F - b));
                WarlockFx.ring(c, UP, 0.5D + 2.5D * b, 0.3D, PALE, 0.7F * (1F - b));
            }
        }

        private Vec3d arc(Vec3d to, float p) {
            Vec3d mid = from.add(to).scale(0.5D).add(0D, 2.5D + from.distanceTo(to) * 0.2D, 0D);
            Vec3d a = from.add(mid.subtract(from).scale(p)), b = mid.add(to.subtract(mid).scale(p));
            return a.add(b.subtract(a).scale(p));
        }
    }

    // ------------------------------------------------------------------ Planet Breaker

    /**
     * Planet Breaker: a flash that whites out the sky, a pillar of the stone's light, rings of
     * force racing out for hundreds of blocks, and the ground splitting open under all of it.
     */
    public static class Break extends FxRenderer.Fx {
        final Vec3d at;
        final double reach;
        final Crack cracks;

        public Break(Vec3d at, double reach, long seed, int ticks) {
            super(ticks);
            this.at = at;
            this.reach = reach;
            this.cracks = new Crack(at, Math.min(60D, reach * 0.4D), seed, ticks);
        }

        @Override
        void tick() {
            cracks.age = age;
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            float p = progress(partialTicks);
            float time = age + partialTicks;
            Vec3d c = at.subtract(camera);
            cracks.render(camera, partialTicks);
            WarlockFx.additive();
            for (int i = 0; i < 5; i++) {
                float q = MathHelper.clamp(time / (30F + i * 18F), 0F, 1F);
                if (q >= 1F) {
                    continue;
                }
                double r = reach * (0.15D + 0.85D * (1D - Math.pow(1D - q, 2D))) * (1D + i * 0.25D);
                float fade = (1F - q) * (1F - q);
                WarlockFx.ring(c.add(0D, 0.5D + i * 1.5D, 0D), UP, r, 2D + i * 1.2D, i == 0 ? WHITE : i % 2 == 0 ? MAGENTA : PALE, 0.7F * fade);
            }
            float pillar = smooth(time / 6F) * (1F - smooth((p - 0.3F) / 0.5F));
            WarlockFx.strip(c, c.add(0D, 220D, 0D), 6D * pillar, VIOLET, 0.35F * pillar);
            WarlockFx.strip(c, c.add(0D, 220D, 0D), 2D * pillar, PALE, 0.6F * pillar);
            // Light, not a thing: the flash shows through the ground rather than being cut off by it.
            float flash = Math.max(0F, 1F - time / 20F);
            GlStateManager.disableDepth();
            WarlockFx.glow(c.add(0D, 3D, 0D), 30D * flash + 4D, WHITE, flash);
            WarlockFx.glow(c.add(0D, 3D, 0D), 60D * (1F - p), MAGENTA, 0.18F * (1F - p) * (1F - p));
            GlStateManager.enableDepth();
        }
    }

    /** Where he will come down: a purple ring closing in on the spot, and a column of light up to him. */
    public static class Mark extends FxRenderer.Fx {
        final Vec3d at;
        final double radius;

        public Mark(Vec3d at, double radius, int ticks) {
            super(ticks);
            this.at = at;
            this.radius = radius;
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            float p = progress(partialTicks);
            float time = age + partialTicks;
            float pulse = 0.6F + 0.4F * MathHelper.sin(time * (0.5F + p * 1.6F));
            Vec3d c = at.subtract(camera).add(0D, 0.1D, 0D);
            float in = Math.min(1F, time / 5F);
            WarlockFx.ring(c, UP, radius, 0.4D, VIOLET, 0.8F * in);
            WarlockFx.ring(c, UP, radius * (1D - p) + 0.8D, 0.5D, PALE, 0.9F * pulse * in);
            WarlockFx.ring(c, UP, radius * 0.5D, 0.25D, MAGENTA, 0.6F * in);
            WarlockFx.flat(c, radius, VIOLET, 0.15F * pulse * in);
            WarlockFx.strip(c, c.add(0D, 70D, 0D), 0.3D + 0.5D * p, MAGENTA, 0.35F * pulse * in);
        }
    }

    // ------------------------------------------------------------------ the rocket

    /** A Galacticraft rocket's engine: a flame under it, flickering, roaring once it lifts. */
    public static class Exhaust extends FxRenderer.Fx {
        final int rocketId;
        final boolean lifting;
        final Vec3d fallback;

        public Exhaust(Vec3d at, int rocketId, boolean lifting, int ticks) {
            super(ticks);
            this.fallback = at;
            this.rocketId = rocketId;
            this.lifting = lifting;
        }

        public Vec3d nozzle(float partialTicks) {
            Entity rocket = Minecraft.getMinecraft().world.getEntityByID(rocketId);
            if (rocket == null) {
                return fallback;
            }
            return new Vec3d(rocket.lastTickPosX + (rocket.posX - rocket.lastTickPosX) * partialTicks,
                    rocket.lastTickPosY + (rocket.posY - rocket.lastTickPosY) * partialTicks - 0.2D,
                    rocket.lastTickPosZ + (rocket.posZ - rocket.lastTickPosZ) * partialTicks);
        }

        public boolean lifting() {
            return lifting;
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            float time = age + partialTicks;
            float in = Math.min(1F, time / 10F) * (1F - smooth((time - lifetime + 10F) / 10F));
            Vec3d c = nozzle(partialTicks).subtract(camera);
            float flicker = 0.8F + 0.2F * MathHelper.sin(time * 3.1F) + 0.1F * MathHelper.sin(time * 7.3F);
            double length = (lifting ? 7D : 1.2D) * flicker;
            Vec3d end = c.add(0D, -length, 0D);
            WarlockFx.strip(c, end, 1.1D * (lifting ? 1D : 0.5D), 0.2D, FLAME, FLAME, 0.6F * in, 0F);
            WarlockFx.strip(c, end, 0.45D * (lifting ? 1D : 0.5D), 0.05D, WHITE, HEAT, 0.9F * in, 0F);
            WarlockFx.glow(c, (lifting ? 3.5D : 1.2D) * flicker, FLAME, 0.5F * in);
            WarlockFx.glow(c, lifting ? 1.2D : 0.5D, WHITE, 0.8F * in);
        }
    }

    // ------------------------------------------------------------------ shared

    /** Where his brow is, as he is drawn now; failing that, where it was. */
    static Vec3d brow(int entityId, float partialTicks, Vec3d fallback) {
        Entity entity = Minecraft.getMinecraft().world.getEntityByID(entityId);
        if (!(entity instanceof EntityChampion)) {
            return fallback;
        }
        EntityChampion c = (EntityChampion) entity;
        float yaw = (float) Math.toRadians(c.prevRotationYaw + MathHelper.wrapDegrees(c.rotationYaw - c.prevRotationYaw) * partialTicks);
        double x = c.lastTickPosX + (c.posX - c.lastTickPosX) * partialTicks;
        double y = c.lastTickPosY + (c.posY - c.lastTickPosY) * partialTicks;
        double z = c.lastTickPosZ + (c.posZ - c.lastTickPosZ) * partialTicks;
        return new Vec3d(x - MathHelper.sin(yaw) * 0.35D, y + 2.95D, z + MathHelper.cos(yaw) * 0.35D);
    }

    /** A low wall of colour standing up off the ground in a ring, fading to nothing at its top. */
    static void wall(Vec3d c, double radius, double height, double thickness, float[] col, float alpha) {
        if (radius <= 0D || alpha <= 0.003F) {
            return;
        }
        double toCamera = Math.sqrt(c.x * c.x + c.z * c.z);
        alpha *= (float) MathHelper.clamp((Math.abs(toCamera - radius) - 0.8D) / 2.5D + (Math.abs(c.y) > height + 1D ? 1D : 0D), 0D, 1D);
        if (alpha <= 0.003F) {
            return;
        }
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        int segments = Math.max(24, Math.min(96, (int) (radius * 3D)));
        buffer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i <= segments; i++) {
            double a = Math.PI * 2D * i / segments;
            double cos = Math.cos(a), sin = Math.sin(a);
            buffer.pos(c.x + cos * radius, c.y, c.z + sin * radius).color(col[0], col[1], col[2], alpha).endVertex();
            buffer.pos(c.x + cos * (radius + thickness * 0.5D), c.y + height, c.z + sin * (radius + thickness * 0.5D)).color(col[0], col[1], col[2], 0F).endVertex();
        }
        tessellator.draw();
        buffer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i <= segments; i++) {
            double a = Math.PI * 2D * i / segments;
            double cos = Math.cos(a), sin = Math.sin(a);
            buffer.pos(c.x + cos * Math.max(0D, radius - thickness), c.y, c.z + sin * Math.max(0D, radius - thickness)).color(col[0], col[1], col[2], 0F).endVertex();
            buffer.pos(c.x + cos * radius, c.y, c.z + sin * radius).color(col[0], col[1], col[2], alpha).endVertex();
        }
        tessellator.draw();
    }

    /** A strip lying flat on the ground, brightest down its middle. */
    static void flatStrip(Vec3d a, Vec3d b, double width, float[] col, float alpha) {
        if (alpha <= 0.003F) {
            return;
        }
        Vec3d dir = b.subtract(a);
        Vec3d side = new Vec3d(-dir.z, 0D, dir.x);
        side = side.lengthSquared() < 1.0E-10 ? new Vec3d(1D, 0D, 0D) : side.normalize().scale(width);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        for (int half = -1; half <= 1; half += 2) {
            buffer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
            buffer.pos(a.x + side.x * half, a.y, a.z + side.z * half).color(col[0], col[1], col[2], 0F).endVertex();
            buffer.pos(a.x, a.y, a.z).color(col[0], col[1], col[2], alpha).endVertex();
            buffer.pos(b.x + side.x * half, b.y, b.z + side.z * half).color(col[0], col[1], col[2], 0F).endVertex();
            buffer.pos(b.x, b.y, b.z).color(col[0], col[1], col[2], alpha).endVertex();
            tessellator.draw();
        }
    }

    static float smooth(float x) {
        x = MathHelper.clamp(x, 0F, 1F);
        return x * x * (3F - 2F * x);
    }
}
