package com.matoon.eternity.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

import static com.matoon.eternity.client.render.RenderMagus.DEEP;
import static com.matoon.eternity.client.render.RenderMagus.VIOLET;
import static com.matoon.eternity.client.render.RenderMagus.WHITE;
import static com.matoon.eternity.client.render.WarlockFx.basis;
import static com.matoon.eternity.client.render.WarlockFx.glow;
import static com.matoon.eternity.client.render.WarlockFx.ring;
import static com.matoon.eternity.client.render.WarlockFx.star;
import static com.matoon.eternity.client.render.WarlockFx.strip;

/**
 * The Magus's effects, and what is done in the Sovereign when he comes: his blows landing, the
 * hold he takes on someone from across a room, a soul going up out of a body, and the roof of
 * the Sanctum coming off. Drawn additively in his cold white and violet, but for the soul,
 * which is hers, and gold. Positions are world coordinates; geometry is drawn relative to the
 * camera.
 */
@SideOnly(Side.CLIENT)
public final class MagusFx {

    private static final Vec3d UP = new Vec3d(0D, 1D, 0D);
    private static final float[] SOUL_GOLD = {1F, 0.86F, 0.5F};
    private static final float[] SOUL_WHITE = {1F, 0.97F, 0.88F};
    private static final float[] EMBER = {1F, 0.45F, 0.1F};

    private MagusFx() {
    }

    /** A blow of his landing: a white flash, a starburst, rings driven on through what it hit. Kind 0 light, 1 heavy, 2 ruinous. */
    public static class Impact extends FxRenderer.Fx {
        final Vec3d at, dir;
        final int kind;
        final long seed;

        public Impact(Vec3d at, Vec3d dir, int kind, long seed) {
            super(kind == 0 ? 9 : kind == 1 ? 16 : 24);
            this.at = at;
            this.dir = dir.lengthSquared() < 1.0E-6 ? UP : dir.normalize();
            this.kind = kind;
            this.seed = seed;
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            float p = progress(partialTicks);
            float fade = 1F - p;
            double size = kind == 0 ? 0.5D : kind == 1 ? 0.9D : 1.5D;
            Vec3d c = at.subtract(camera);
            float flash = Math.max(0F, 1F - p * 3F);
            glow(c, size * (1.2D + 1.4D * flash), WHITE, 0.9F * flash);
            glow(c, size * 2.6D, VIOLET, 0.4F * fade);
            star(c, size * 0.25D, size * (1.3D + p * 1.4D), 14, seed, (age + partialTicks) * 0.04F, WHITE, 0.85F * fade);
            star(c, size * 0.3D, size * (2D + p * 2D), 10, seed + 7L, -(age + partialTicks) * 0.03F, VIOLET, 0.5F * fade);
            double grow = 1D - Math.pow(1D - p, 2.5D);
            ring(c.add(dir.scale(grow * size * 1.4D)), dir, size * (0.4D + 2.8D * grow), size * 0.45D, WHITE, 0.75F * fade);
            ring(c.add(dir.scale(grow * size * 3D)), dir, size * (0.3D + 2D * grow), size * 0.4D, VIOLET, 0.6F * fade);
            if (kind == 2) {
                ring(c.add(dir.scale(grow * size * 5D)), dir, size * (0.2D + 1.4D * grow), size * 0.35D, DEEP, 0.55F * fade);
                // Lightning thrown off it in every direction.
                Random rand = new Random(seed + age / 2);
                for (int i = 0; i < 6; i++) {
                    Vec3d d = new Vec3d(rand.nextGaussian(), rand.nextGaussian(), rand.nextGaussian()).normalize();
                    arc(c, c.add(d.scale(size * (2D + rand.nextDouble() * 2D))), rand, 0.05D, 0.9F * fade);
                }
            }
        }
    }

    /**
     * His hold on someone, from wherever he stands: arcs of violet lightning, never the same
     * twice, from his raised hand into them, following both of them wherever they go.
     */
    public static class Grasp extends FxRenderer.Fx {
        final int from, to;

        public Grasp(int from, int to, int ticks) {
            super(ticks);
            this.from = from;
            this.to = to;
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            Minecraft mc = Minecraft.getMinecraft();
            Entity a = mc.world.getEntityByID(from), b = mc.world.getEntityByID(to);
            if (a == null || b == null) {
                return;
            }
            float p = progress(partialTicks);
            float alpha = Math.min(1F, p * 8F) * Math.min(1F, (1F - p) * 6F);
            float yaw = (float) Math.toRadians(a.rotationYaw);
            Vec3d forward = new Vec3d(-MathHelper.sin(yaw), 0D, MathHelper.cos(yaw));
            Vec3d right = new Vec3d(-forward.z, 0D, forward.x);
            Vec3d hand = lerp(a, partialTicks).add(0D, 1.6D, 0D).add(forward.scale(0.85D)).add(right.scale(0.37D)).subtract(camera);
            Vec3d into = lerp(b, partialTicks).add(0D, b.height * 0.62D, 0D).subtract(camera);
            Random rand = new Random(age * 977L + from);
            for (int i = 0; i < 5; i++) {
                arc(hand, into, rand, i == 0 ? 0.07D : 0.04D, alpha * (i == 0 ? 1F : 0.6F));
            }
            glow(hand, 0.7D, WHITE, 0.8F * alpha);
            glow(hand, 1.4D, VIOLET, 0.4F * alpha);
            glow(into, 1.1D, VIOLET, 0.45F * alpha);
            // Around them, the hold itself: a cage of light closing in.
            for (int k = 0; k < 3; k++) {
                double r = 0.75D + 0.1D * MathHelper.sin((age + partialTicks) * 0.5F + k * 2F);
                Vec3d axis = new Vec3d(Math.cos(k * 1.05D + age * 0.05D), 0.6D, Math.sin(k * 1.05D + age * 0.05D));
                ring(into, axis, r, 0.08D, VIOLET, 0.55F * alpha);
            }
        }
    }

    /** Something going up out of a body that has stopped: a small gold light, rising, slow, and going out. */
    public static class Soul extends FxRenderer.Fx {
        final Vec3d at;
        final long seed;

        public Soul(Vec3d at, int ticks, long seed) {
            super(ticks);
            this.at = at;
            this.seed = seed;
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            float p = progress(partialTicks);
            float alpha = Math.min(1F, p * 5F) * (1F - p * p);
            double rise = 4.5D * (1D - Math.pow(1D - p, 1.6D));
            float t = age + partialTicks;
            Vec3d c = at.add(Math.sin(t * 0.07D) * 0.25D, rise, Math.cos(t * 0.05D) * 0.25D).subtract(camera);
            float beat = 0.85F + 0.15F * MathHelper.sin(t * 0.3F);
            glow(c, 0.35D * beat, SOUL_WHITE, 0.95F * alpha);
            glow(c, 0.9D * beat, SOUL_GOLD, 0.45F * alpha);
            glow(c, 2.2D, SOUL_GOLD, 0.12F * alpha);
            // A thread of it still down to where she lies, thinning as it goes.
            Vec3d below = at.add(0D, 0.3D, 0D).subtract(camera);
            strip(below, c, 0.02D, 0.08D, SOUL_GOLD, SOUL_WHITE, 0.2F * alpha * (1F - p), 0.5F * alpha);
            Random rand = new Random(seed);
            for (int i = 0; i < 8; i++) {
                double a = rand.nextDouble() * Math.PI * 2D + t * 0.04D;
                double r = 0.3D + rand.nextDouble() * 0.5D;
                double y = (rand.nextDouble() - 0.5D) * 0.8D;
                glow(c.add(Math.cos(a) * r, y, Math.sin(a) * r), 0.08D, SOUL_WHITE, 0.7F * alpha);
            }
        }
    }

    /**
     * The roof of the Sanctum coming off: a white flash filling the dome, rings of shocked air
     * going out level across it, and shafts of light thrown up and out through the hole.
     */
    public static class Breach extends FxRenderer.Fx {
        final Vec3d at;
        final double radius;
        final long seed;
        /** Warlock's going, not the Magus's coming: gold, not violet. */
        final boolean gold;

        public Breach(Vec3d at, double radius, int ticks, long seed, boolean gold) {
            super(ticks);
            this.at = at;
            this.radius = radius;
            this.seed = seed;
            this.gold = gold;
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            float p = progress(partialTicks);
            float fade = 1F - p;
            Vec3d c = at.subtract(camera);
            float flash = Math.max(0F, 1F - p * 2.5F);
            float[] white = gold ? SOUL_WHITE : WHITE, mid = gold ? SOUL_GOLD : VIOLET, deep = gold ? EMBER : DEEP;
            glow(c, radius * (0.6D + 0.8D * flash), white, 0.95F * flash);
            glow(c, radius * 1.6D, mid, 0.35F * fade);
            double grow = 1D - Math.pow(1D - p, 3D);
            ring(c, UP, radius * 0.3D + radius * 2.2D * grow, 1.2D + 2D * grow, white, 0.7F * fade * fade);
            ring(c.add(0D, 2D * grow, 0D), UP, radius * 0.2D + radius * 1.6D * grow, 1.5D, mid, 0.55F * fade);
            ring(c.add(0D, 5D * grow, 0D), UP, radius * 0.1D + radius * 1.1D * grow, 1.2D, deep, 0.45F * fade);
            Random rand = new Random(seed);
            for (int i = 0; i < 26; i++) {
                double a = rand.nextDouble() * Math.PI * 2D;
                double up = 0.5D + rand.nextDouble() * 1.2D;
                Vec3d d = new Vec3d(Math.cos(a), up, Math.sin(a)).normalize();
                double len = radius * (0.8D + rand.nextDouble() * 1.4D) * grow;
                Vec3d s = c.add(d.scale(radius * 0.25D));
                strip(s, s.add(d.scale(len)), 0.35D * fade + 0.05D, 0.05D, white, mid, 0.75F * fade, 0F);
            }
        }
    }

    // ------------------------------------------------------------------ geometry

    private static Vec3d lerp(Entity e, float pt) {
        return new Vec3d(e.lastTickPosX + (e.posX - e.lastTickPosX) * pt, e.lastTickPosY + (e.posY - e.lastTickPosY) * pt,
                e.lastTickPosZ + (e.posZ - e.lastTickPosZ) * pt);
    }

    /** A jagged arc of lightning between two camera-relative points: a white core in a violet sheath. */
    static void arc(Vec3d a, Vec3d b, Random rand, double width, float alpha) {
        if (alpha <= 0.01F) {
            return;
        }
        Vec3d d = b.subtract(a);
        double len = d.length();
        if (len < 1.0E-3) {
            return;
        }
        Vec3d[] side = basis(d.scale(1D / len));
        int steps = Math.max(3, (int) (len * 2.5D));
        Vec3d last = a;
        for (int s = 1; s <= steps; s++) {
            double f = s / (double) steps;
            double jag = s == steps ? 0D : Math.sin(f * Math.PI) * len * 0.12D;
            Vec3d next = a.add(d.scale(f)).add(side[0].scale(rand.nextGaussian() * jag)).add(side[1].scale(rand.nextGaussian() * jag));
            strip(last, next, width * 2.6D, VIOLET, 0.45F * alpha);
            strip(last, next, width, WHITE, alpha);
            last = next;
        }
    }
}
