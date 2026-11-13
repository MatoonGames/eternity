package com.matoon.eternity.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
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
import static com.matoon.eternity.client.render.WarlockFx.flat;
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
            dyed(() -> glow(c, size * 2.2D, DYE, 0.4F * fade));
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

    // ------------------------------------------------------------------ his fight

    /**
     * A shard of thought cut out of the air: a long crystal, white at its heart in a violet
     * sheath. Hung, it trembles where it was made, aimed, with a thread of light laid along the
     * way it will go; loosed, it is a streak.
     */
    public static class Shard extends FxRenderer.Fx {
        public final int id;
        Vec3d from, vel, dir;
        boolean hung;
        /** Broken on something, or out of the air: drawn no more. */
        public boolean gone;

        public Shard(Vec3d at, Vec3d vel, int id, int ticks) {
            super(ticks);
            this.id = id;
            set(at, vel);
        }

        /** Sent again: loosed. */
        public void set(Vec3d at, Vec3d vel) {
            from = at;
            hung = vel.lengthSquared() < 1.0E-4D;
            dir = vel.lengthSquared() < 1.0E-12D ? UP : vel.normalize();
            this.vel = hung ? Vec3d.ZERO : vel;
            age = hung ? age : 0;
        }

        @Override
        public boolean isDead() {
            return gone || super.isDead();
        }

        public Vec3d at(float partialTicks) {
            return from.add(vel.scale(age + partialTicks));
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            float t = age + partialTicks;
            Vec3d c = at(partialTicks).subtract(camera);
            if (hung) {
                // Drawn together out of nothing, then still but for a tremor, and aimed.
                float in = Math.min(1F, t / 6F);
                Vec3d shake = new Vec3d(MathHelper.sin(t * 2.7F + id), MathHelper.sin(t * 3.3F + id * 2), MathHelper.sin(t * 2.1F + id * 3)).scale(0.02D);
                Vec3d mid = c.add(shake);
                dyed(() -> glow(mid, 0.8D, DYE, 0.45F * in));
                crystal(mid.add(dir.scale(0.95D * in)), mid.subtract(dir.scale(0.7D * in)), 0.2D * in, 1F);
                glow(mid, 0.7D, VIOLET, 0.35F * in);
                glow(mid, 0.25D, WHITE, 0.7F * in);
                // The way it will go.
                float thread = in * (0.25F + 0.15F * MathHelper.sin(t * 1.7F));
                strip(mid.add(dir.scale(0.8D)), mid.add(dir.scale(9D)), 0.035D, 0.005D, WHITE, VIOLET, thread, 0F);
                if (t < 6F) {
                    // Gathered in from a spray of light round it.
                    Random rand = new Random(id * 131L);
                    for (int i = 0; i < 8; i++) {
                        Vec3d d = new Vec3d(rand.nextGaussian(), rand.nextGaussian(), rand.nextGaussian()).normalize();
                        double r = 1.4D * (1D - in);
                        strip(mid.add(d.scale(r + 0.3D)), mid.add(d.scale(r)), 0.03D, VIOLET, 0.6F * (1F - in));
                    }
                }
                return;
            }
            double tail = Math.min(t, 3F);
            Vec3d back = c.subtract(vel.scale(tail));
            dyed(() -> strip(c, back, 0.3D, 0.03D, DYE, DYE, 0.6F, 0F));
            strip(c, back, 0.22D, 0.02D, VIOLET, DEEP, 0.5F, 0F);
            strip(c, back, 0.07D, 0.01D, WHITE, VIOLET, 0.85F, 0F);
            crystal(c.add(dir.scale(0.7D)), c.subtract(dir.scale(0.5D)), 0.15D, 1F);
            glow(c, 0.6D, VIOLET, 0.5F);
        }
    }

    /**
     * A sphere of the Power Stone's light, thrown slow and heavy: a white heart in violet, rings
     * of it turning round it every way at once, lightning crawling off it.
     */
    public static class Orb extends FxRenderer.Fx {
        public final int id;
        final Vec3d from, vel;
        final long seed;
        public boolean gone;

        public Orb(Vec3d from, Vec3d vel, int id, int ticks, long seed) {
            super(ticks);
            this.from = from;
            this.vel = vel;
            this.id = id;
            this.seed = seed;
        }

        @Override
        public boolean isDead() {
            return gone || super.isDead();
        }

        public Vec3d at(float partialTicks) {
            return from.add(vel.scale(age + partialTicks));
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            float t = age + partialTicks;
            Vec3d c = at(partialTicks).subtract(camera);
            float in = Math.min(1F, t / 3F);
            float pulse = 0.88F + 0.12F * MathHelper.sin(t * 1.3F);
            dyed(() -> {
                glow(c, 2.2D * pulse * in, DYE, 0.6F);
                ring(c, new Vec3d(Math.sin(t * 0.05D), 1D, Math.cos(t * 0.05D)), 1.25D * in, 0.18D, DYE, 0.7F);
            });
            glow(c, 2.4D * pulse * in, DEEP, 0.45F);
            glow(c, 1.3D * pulse * in, VIOLET, 0.75F);
            glow(c, 0.6D * in, WHITE, 1F);
            star(c, 0.25D, 1.9D * pulse * in, 12, seed, t * 0.08F, WHITE, 0.55F);
            for (int k = 0; k < 3; k++) {
                double a = t * (0.09D + k * 0.04D) + k * 2.1D;
                Vec3d axis = new Vec3d(Math.cos(a), Math.sin(a * 0.7D + k), Math.sin(a));
                ring(c, axis, 1.05D * in, 0.09D, k == 1 ? WHITE : VIOLET, 0.7F);
            }
            Random rand = new Random(seed + age / 2);
            for (int i = 0; i < 4; i++) {
                Vec3d d = new Vec3d(rand.nextGaussian(), rand.nextGaussian(), rand.nextGaussian()).normalize();
                dyedArc(c.add(d.scale(0.5D)), c.add(d.scale(1.4D + rand.nextDouble() * 1.4D)), rand, 0.035D, 0.8F * in);
            }
            Vec3d back = c.subtract(vel.scale(Math.min(t, 6F)));
            dyed(() -> strip(c, back, 1D, 0.1D, DYE, DYE, 0.5F, 0F));
            strip(c, back, 0.9D, 0.1D, VIOLET, DEEP, 0.45F, 0F);
        }
    }

    /** What he holds, gathering in his hands: light drawn in from all round, and crackling where it meets. */
    public static class Gather extends FxRenderer.Fx {
        final int entityId;
        final Vec3d fallback;
        final double radius, up, forward;
        final long seed;

        public Gather(Vec3d at, int entityId, double radius, double up, double forward, int ticks, long seed) {
            super(ticks);
            this.fallback = at;
            this.entityId = entityId;
            this.radius = radius;
            this.up = up;
            this.forward = forward;
            this.seed = seed;
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            float p = progress(partialTicks);
            Vec3d centre = fallback;
            Entity e = Minecraft.getMinecraft().world.getEntityByID(entityId);
            if (e != null) {
                float yaw = (float) Math.toRadians(e.rotationYaw);
                centre = lerp(e, partialTicks).add(-MathHelper.sin(yaw) * forward, up, MathHelper.cos(yaw) * forward);
            }
            Vec3d c = centre.subtract(camera);
            float t = age + partialTicks;
            Random rand = new Random(seed);
            for (int i = 0; i < 22; i++) {
                Vec3d d = new Vec3d(rand.nextGaussian(), rand.nextGaussian() * 0.8D, rand.nextGaussian()).normalize();
                float phase = (t * 0.1F + rand.nextFloat()) % 1F;
                double r0 = radius * (1D - phase), r1 = Math.max(0D, r0 - radius * 0.3D);
                float a = MathHelper.sin(phase * (float) Math.PI) * (0.35F + 0.65F * p);
                strip(c.add(d.scale(r0)), c.add(d.scale(r1)), 0.045D, i % 3 == 0 ? WHITE : VIOLET, a);
            }
            Random crackle = new Random(seed + age / 2);
            for (int i = 0; i < 2 + (int) (p * 3F); i++) {
                Vec3d d = new Vec3d(crackle.nextGaussian(), crackle.nextGaussian(), crackle.nextGaussian()).normalize();
                dyedArc(c, c.add(d.scale(radius * (0.3D + crackle.nextDouble() * 0.5D))), crackle, 0.03D, 0.6F * p);
            }
            dyed(() -> glow(c, 0.5D + 1.1D * p, DYE, 0.5F * p));
            glow(c, 0.25D + 0.6D * p, WHITE, 0.45F + 0.5F * p);
            glow(c, 0.8D + 1.4D * p, VIOLET, 0.3F * p);
        }
    }

    /** A sphere of his bursting: a shell of light thrown out every way, a white flash, lightning flung off it. */
    public static class Nova extends FxRenderer.Fx {
        final Vec3d at;
        final double radius;
        final long seed;

        public Nova(Vec3d at, double radius, long seed) {
            super(22);
            this.at = at;
            this.radius = radius;
            this.seed = seed;
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            float p = progress(partialTicks);
            float fade = 1F - p;
            float flash = Math.max(0F, 1F - p * 3.5F);
            double grow = 1D - Math.pow(1D - p, 3D);
            Vec3d c = at.subtract(camera);
            glow(c, radius * (0.5D + 0.6D * flash), WHITE, 0.95F * flash);
            dyed(() -> {
                glow(c, radius * 1.15D * grow, DYE, 0.4F * fade);
                Random dye = new Random(seed + 3L);
                for (int k = 0; k < 4; k++) {
                    ring(c, new Vec3d(dye.nextGaussian(), dye.nextGaussian(), dye.nextGaussian()), radius * grow * 0.97D, 0.5D + 0.6D * fade, DYE, 0.6F * fade);
                }
            });
            glow(c, radius * 1.3D * grow, VIOLET, 0.35F * fade);
            Random rand = new Random(seed);
            for (int k = 0; k < 7; k++) {
                Vec3d n = new Vec3d(rand.nextGaussian(), rand.nextGaussian(), rand.nextGaussian());
                ring(c, n, radius * grow, 0.25D + 0.5D * fade, k % 2 == 0 ? WHITE : VIOLET, 0.6F * fade * fade);
            }
            ring(c, UP, radius * 1.25D * grow, 0.6D, DEEP, 0.55F * fade);
            star(c, radius * 0.1D, radius * (0.7D + grow), 16, seed, p * 0.6F, WHITE, 0.7F * fade);
            Random crackle = new Random(seed + age / 2);
            for (int i = 0; i < 7; i++) {
                Vec3d d = new Vec3d(crackle.nextGaussian(), crackle.nextGaussian(), crackle.nextGaussian()).normalize();
                dyedArc(c.add(d.scale(radius * 0.2D)), c.add(d.scale(radius * (0.6D + 0.6D * grow))), crackle, 0.05D, 0.9F * fade);
            }
        }
    }

    /**
     * Where he means to come down, marked on the ground: a ring of violet with a white one
     * closing in on its centre, timed to the blow, and a column of light standing up out of it.
     */
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
            float t = age + partialTicks;
            float in = Math.min(1F, t / 4F);
            float pulse = 0.6F + 0.4F * MathHelper.sin(t * (0.5F + p * 1.6F));
            Vec3d c = at.subtract(camera).add(0D, 0.08D, 0D);
            dyed(() -> {
                flat(c, radius * 1.05D, DYE, 0.3F * in);
                ring(c, UP, radius, 0.45D, DYE, 0.8F * in);
                ring(c, UP, radius * 0.55D, 0.2D, DYE, 0.6F * in);
            });
            ring(c, UP, radius, 0.3D, VIOLET, 0.85F * in);
            ring(c, UP, radius * (1D - p) + 0.4D, 0.35D, WHITE, 0.9F * pulse * in);
            ring(c, UP, radius * 0.55D, 0.12D, DEEP, 0.7F * in);
            // Six spokes turning, slowly, then faster.
            for (int i = 0; i < 6; i++) {
                double a = i * Math.PI / 3D + t * (0.02D + 0.08D * p);
                Vec3d o = new Vec3d(Math.cos(a), 0D, Math.sin(a));
                strip(c.add(o.scale(radius * 0.6D)), c.add(o.scale(radius * 1.1D)), 0.1D, WHITE, 0.6F * in);
                strip(c.add(o.scale(radius * 0.15D)), c.add(o.scale(radius * 0.5D)), 0.06D, VIOLET, 0.5F * in);
            }
            flat(c, radius, VIOLET, 0.16F * pulse * in);
            strip(c, c.add(0D, 30D, 0D), 0.15D + 0.4D * p, 0.05D, WHITE, VIOLET, 0.5F * pulse * in, 0F);
        }
    }

    /**
     * He has come down: a white flash, the ground broken in rings racing out across it, spears of
     * light thrown up out of it all round, and lightning crawling over it.
     */
    public static class Slam extends FxRenderer.Fx {
        final Vec3d at;
        final double radius;
        final long seed;

        public Slam(Vec3d at, double radius, long seed) {
            super(32);
            this.at = at;
            this.radius = radius;
            this.seed = seed;
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            float p = progress(partialTicks);
            float fade = 1F - p;
            float flash = Math.max(0F, 1F - p * 3F);
            double grow = 1D - Math.pow(1D - p, 3D);
            Vec3d c = at.subtract(camera).add(0D, 0.1D, 0D);
            glow(c.add(0D, 1D, 0D), radius * (0.7D + 0.8D * flash), WHITE, 0.95F * flash);
            dyed(() -> {
                flat(c, radius * (0.6D + 0.9D * grow), DYE, 0.55F * fade);
                ring(c, UP, radius * (0.25D + 1.5D * grow), 1.4D, DYE, 0.85F * fade);
                ring(c, UP, radius * (0.15D + 1.1D * grow), 1.2D, DYE, 0.75F * fade);
            });
            ring(c, UP, radius * (0.3D + 1.7D * grow), 0.6D, WHITE, 0.6F * fade * fade);
            ring(c, UP, radius * (0.2D + 1.2D * grow), 1.1D, VIOLET, 0.65F * fade);
            ring(c.add(0D, 0.6D * grow, 0D), UP, radius * (0.1D + 0.8D * grow), 0.7D, DEEP, 0.5F * fade);
            strip(c, c.add(0D, 16D * grow, 0D), 1.4D * fade + 0.1D, 0.1D, WHITE, VIOLET, 0.8F * fade, 0F);
            Random rand = new Random(seed);
            for (int i = 0; i < 18; i++) {
                double a = rand.nextDouble() * Math.PI * 2D;
                double r = radius * (0.3D + rand.nextDouble() * 0.8D);
                Vec3d base = c.add(Math.cos(a) * r, 0D, Math.sin(a) * r);
                Vec3d d = new Vec3d(Math.cos(a) * 0.35D, 1D, Math.sin(a) * 0.35D).normalize();
                double len = (1.5D + rand.nextDouble() * 3D) * Math.min(1D, p * 6D) * (1D - p * 0.6D);
                strip(base, base.add(d.scale(len)), 0.28D * fade, 0.02D, WHITE, VIOLET, 0.85F * fade, 0F);
            }
            Random crackle = new Random(seed + age / 2);
            for (int i = 0; i < 6; i++) {
                double a = crackle.nextDouble() * Math.PI * 2D;
                Vec3d end = c.add(Math.cos(a) * radius * 1.3D * grow, 0.2D, Math.sin(a) * radius * 1.3D * grow);
                dyedArc(c.add(0D, 0.3D, 0D), end, crackle, 0.05D, 0.85F * fade);
            }
        }
    }

    /** A long crystal from tip to tail, widest a third of the way back: violet sheath, white heart. */
    static void crystal(Vec3d tip, Vec3d tail, double width, float alpha) {
        Vec3d mid = tip.add(tail.subtract(tip).scale(0.35D));
        dyed(() -> {
            strip(tip, mid, 0D, width * 2.8D, DYE, DYE, 0.7F * alpha, 0.7F * alpha);
            strip(mid, tail, width * 2.8D, 0D, DYE, DYE, 0.7F * alpha, 0.3F * alpha);
        });
        strip(tip, mid, 0D, width * 2.2D, VIOLET, VIOLET, 0.55F * alpha, 0.55F * alpha);
        strip(mid, tail, width * 2.2D, 0D, VIOLET, DEEP, 0.55F * alpha, 0.2F * alpha);
        strip(tip, mid, 0D, width, WHITE, WHITE, alpha, alpha);
        strip(mid, tail, width, 0D, WHITE, VIOLET, alpha, 0.5F * alpha);
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

    /** A deep violet that a bright sky cannot wash out, for what is drawn over the world rather than added to it. */
    static final float[] DYE = {0.46F, 0.06F, 0.9F};

    /**
     * Draws with ordinary blending rather than additive: it colours what is behind it, so his violet reads as
     * violet even against a noon sky, where added light only ever goes white. The light is drawn over it after.
     */
    static void dyed(Runnable draw) {
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        draw.run();
        WarlockFx.additive();
    }

    /** As {@link #arc}, the lightning sheathed in dye under its light. */
    static void dyedArc(Vec3d a, Vec3d b, Random rand, double width, float alpha) {
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
        Vec3d[] points = new Vec3d[steps + 1];
        points[0] = a;
        for (int s = 1; s <= steps; s++) {
            double f = s / (double) steps;
            double jag = s == steps ? 0D : Math.sin(f * Math.PI) * len * 0.12D;
            points[s] = a.add(d.scale(f)).add(side[0].scale(rand.nextGaussian() * jag)).add(side[1].scale(rand.nextGaussian() * jag));
        }
        dyed(() -> {
            for (int s = 1; s <= steps; s++) {
                strip(points[s - 1], points[s], width * 3D, DYE, 0.55F * alpha);
            }
        });
        for (int s = 1; s <= steps; s++) {
            strip(points[s - 1], points[s], width * 2D, VIOLET, 0.4F * alpha);
            strip(points[s - 1], points[s], width, WHITE, alpha);
        }
    }
}
