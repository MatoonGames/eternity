package com.matoon.eternity.client.render;

import com.matoon.eternity.entity.EntityAdamWarlock;
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
 * Adam Warlock's effects: everything his power does to the air around him. All of it is
 * drawn additively in gold, amber and white, the colours of the cocoon he was born from.
 * Positions are world coordinates; geometry is drawn relative to the camera.
 */
@SideOnly(Side.CLIENT)
public final class WarlockFx {

    // His palette: white-hot, gold, amber, ember.
    static final float[] WHITE = {1F, 0.97F, 0.86F};
    static final float[] GOLD = {1F, 0.8F, 0.3F};
    static final float[] AMBER = {1F, 0.52F, 0.12F};
    static final float[] EMBER = {0.95F, 0.25F, 0.05F};

    private static final Vec3d UP = new Vec3d(0D, 1D, 0D);

    private WarlockFx() {
    }

    // ------------------------------------------------------------------ the fly-by

    /** The sound barrier breaking: rings of shocked air racing out around where he left, and a cone of streaks behind. */
    public static class Boom extends FxRenderer.Fx {
        final Vec3d at, dir;
        final long seed;

        public Boom(Vec3d at, Vec3d dir, int ticks, long seed) {
            super(ticks);
            this.at = at;
            this.dir = dir.lengthSquared() < 1.0E-6 ? UP : dir.normalize();
            this.seed = seed;
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            float p = progress(partialTicks);
            float fade = 1F - p;
            Vec3d c = at.subtract(camera);
            double grow = 1D - Math.pow(1D - p, 3D);
            ring(c, dir, 0.6D + 5.5D * grow, 0.35D + 0.6D * grow, WHITE, 0.85F * fade * fade);
            ring(c.subtract(dir.scale(1.2D * grow)), dir, 0.4D + 3.8D * grow, 0.6D, GOLD, 0.6F * fade);
            ring(c.subtract(dir.scale(2.6D * grow)), dir, 0.3D + 2.4D * grow, 0.5D, AMBER, 0.45F * fade);
            if (p < 0.35F) {
                float f = 1F - p / 0.35F;
                glow(c, 2.4D * f + 0.4D, WHITE, 0.9F * f);
                glow(c, 4.5D * f, GOLD, 0.35F * f);
            }
            // Streaks of displaced air, pulled out behind him.
            Random rand = new Random(seed);
            Vec3d[] basis = basis(dir);
            for (int i = 0; i < 14; i++) {
                double a = rand.nextDouble() * Math.PI * 2D;
                double r = 0.4D + rand.nextDouble() * 1.4D;
                Vec3d off = basis[0].scale(Math.cos(a) * r).add(basis[1].scale(Math.sin(a) * r));
                double len = (2D + rand.nextDouble() * 5D) * grow;
                Vec3d s = c.add(off.scale(1D + grow * 1.5D));
                strip(s, s.subtract(dir.scale(len)).add(off.scale(grow)), 0.07D, GOLD, 0.7F * fade);
            }
        }
    }

    /**
     * A blow landing: a white flash, a starburst of light spikes, and a ring of force driven
     * on through whatever it hit. Kind 0 is a jab, 1 a big hit, 2 the hammer.
     */
    public static class Impact extends FxRenderer.Fx {
        final Vec3d at, dir;
        final int kind;
        final long seed;

        public Impact(Vec3d at, Vec3d dir, int kind, long seed) {
            super(kind == 0 ? 8 : 14);
            this.at = at;
            this.dir = dir.lengthSquared() < 1.0E-6 ? UP : dir.normalize();
            this.kind = kind;
            this.seed = seed;
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            float p = progress(partialTicks);
            float fade = 1F - p;
            double size = kind == 0 ? 0.45D : kind == 1 ? 0.8D : 1.0D;
            Vec3d c = at.subtract(camera);
            float flash = Math.max(0F, 1F - p * 3F);
            glow(c, size * (1D + 1.2D * flash), WHITE, 0.85F * flash);
            glow(c, size * 2.2D, GOLD, 0.35F * fade);
            star(c, size * 0.25D, size * (1.2D + p * 1.2D), 12, seed, (age + partialTicks) * 0.05F, WHITE, 0.8F * fade);
            star(c, size * 0.25D, size * (1.8D + p * 1.6D), 9, seed + 7L, -(age + partialTicks) * 0.03F, GOLD, 0.45F * fade);
            double grow = 1D - Math.pow(1D - p, 2.5D);
            ring(c.add(dir.scale(grow * size * 1.5D)), dir, size * (0.4D + 2.6D * grow), size * 0.5D, GOLD, 0.7F * fade);
            if (kind > 0) {
                ring(c.add(dir.scale(grow * size * 3D)), dir, size * (0.3D + 1.8D * grow), size * 0.4D, AMBER, 0.6F * fade);
            }
        }
    }

    /** He stops dead in the air, and the air keeps going: a flat ring racing outward under him and a flare where he hangs. */
    public static class Land extends FxRenderer.Fx {
        final Vec3d at;
        final double reach;

        public Land(Vec3d at, double reach, int ticks) {
            super(ticks);
            this.at = at;
            this.reach = reach;
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            float p = progress(partialTicks);
            float fade = 1F - p;
            Vec3d c = at.subtract(camera);
            double grow = 1D - Math.pow(1D - p, 3D);
            ring(c, UP, reach * 1.4D * grow + 0.5D, 0.3D + 0.5D * grow, WHITE, 0.6F * fade * fade);
            ring(c.add(0D, -0.6D, 0D), UP, reach * grow + 0.3D, 0.5D, GOLD, 0.4F * fade);
            float flash = Math.max(0F, 1F - p * 3F);
            glow(c, 2.2D * flash + 0.4D, WHITE, 0.7F * flash);
            glow(c, 3.5D * fade, GOLD, 0.2F * fade);
        }
    }

    /** Power drawn into him: streaks of gold converging on his chest, and the light there growing. */
    public static class Charge extends FxRenderer.Fx {
        final Vec3d fallback;
        final double radius;
        final int entityId;
        final long seed;

        public Charge(Vec3d at, double radius, int entityId, int ticks, long seed) {
            super(ticks);
            this.fallback = at;
            this.radius = radius;
            this.entityId = entityId;
            this.seed = seed;
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            float p = progress(partialTicks);
            Vec3d centre = fallback;
            Entity entity = Minecraft.getMinecraft().world.getEntityByID(entityId);
            if (entity instanceof EntityAdamWarlock) {
                centre = new Vec3d(entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks,
                        entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks + 1.3D,
                        entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks);
            }
            Vec3d c = centre.subtract(camera);
            float time = age + partialTicks;
            Random rand = new Random(seed);
            for (int i = 0; i < 18; i++) {
                Vec3d d = new Vec3d(rand.nextGaussian(), rand.nextGaussian() * 0.7D, rand.nextGaussian()).normalize();
                float phase = (time * 0.09F + rand.nextFloat()) % 1F;
                double r0 = radius * (1D - phase);
                double r1 = Math.max(0D, r0 - radius * 0.35D);
                float a = MathHelper.sin(phase * (float) Math.PI) * (0.4F + 0.6F * p);
                strip(c.add(d.scale(r0)), c.add(d.scale(r1)), 0.05D, GOLD, a);
            }
            glow(c, 0.3D + 0.7D * p, WHITE, 0.4F + 0.5F * p);
            glow(c, 1.2D + 1.2D * p, GOLD, 0.25F * p);
        }
    }

    // ------------------------------------------------------------------ quantum energy

    /** A bolt of quantum energy: a white-hot core in a gold flare, dragging a tail of light. Bursting kills it early. */
    public static class Bolt extends FxRenderer.Fx {
        public final int id;
        final Vec3d from, vel;
        public boolean burst;

        public Bolt(Vec3d from, Vec3d vel, int id, int ticks) {
            super(ticks);
            this.from = from;
            this.vel = vel;
            this.id = id;
        }

        @Override
        public boolean isDead() {
            return burst || super.isDead();
        }

        public Vec3d at(float partialTicks) {
            return from.add(vel.scale(age + partialTicks));
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            Vec3d c = at(partialTicks).subtract(camera);
            float time = age + partialTicks;
            float flicker = 0.85F + 0.15F * MathHelper.sin(time * 2.3F);
            double tail = Math.min(age + partialTicks, 2.2F);
            Vec3d end = c.subtract(vel.scale(tail));
            strip(c, end, 0.3D, 0.05D, AMBER, EMBER, 0.55F, 0F);
            strip(c, end, 0.1D, 0.02D, WHITE, GOLD, 0.85F, 0F);
            glow(c, 0.85D * flicker, GOLD, 0.75F);
            glow(c, 0.32D, WHITE, 1F);
            star(c, 0.1D, 0.7D * flicker, 6, id * 31L, time * 0.2F, WHITE, 0.6F);
        }
    }

    /** A bolt bursting: a flash, a sphere of shocked air, spikes of light. */
    public static class Burst extends FxRenderer.Fx {
        final Vec3d at;
        final double radius;
        final long seed;

        public Burst(Vec3d at, double radius, int ticks, long seed) {
            super(ticks);
            this.at = at;
            this.radius = radius;
            this.seed = seed;
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            float p = progress(partialTicks);
            float fade = 1F - p;
            Vec3d c = at.subtract(camera);
            float flash = Math.max(0F, 1F - p * 2.5F);
            glow(c, radius * (0.5D + flash), WHITE, flash);
            glow(c, radius * 1.6D, GOLD, 0.5F * fade);
            double grow = 1D - Math.pow(1D - p, 3D);
            ring(c, UP, radius * grow, 0.3D, GOLD, 0.7F * fade);
            ring(c, c.normalize(), radius * 1.1D * grow, 0.35D, WHITE, 0.5F * fade);
            star(c, radius * 0.1D, radius * (0.8D + p), 10, seed, 0F, AMBER, 0.7F * fade);
        }
    }

    /**
     * His beam: a white core in a broad golden shaft, two strands of quantum light coiling
     * round it, a sun at his palms and a splash of light where it strikes.
     */
    public static class Beam extends FxRenderer.Fx {
        final Vec3d from, to;
        final int phase;
        /** A newer instant of the same beam has arrived; this one makes way for it. */
        public boolean replaced;

        public Beam(Vec3d from, Vec3d to, int phase, int ticks) {
            super(ticks);
            this.from = from;
            this.to = to;
            this.phase = phase;
        }

        @Override
        public boolean isDead() {
            return replaced || super.isDead();
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            float p = progress(partialTicks);
            // It holds at full strength until the next instant replaces it, and only fades if none comes.
            float fade = 1F - Math.max(0F, p - 0.5F) * 2F;
            Vec3d a = from.subtract(camera), b = to.subtract(camera);
            float time = phase + age + partialTicks;
            float pulse = 0.85F + 0.15F * MathHelper.sin(time * 1.9F);
            Vec3d axis = b.subtract(a);
            double length = axis.length();
            Vec3d d = axis.normalize();
            // In short lengths, each fading as it comes near the camera: a beam that strikes you is
            // felt, not stared down the length of.
            int pieces = Math.max(1, (int) Math.ceil(length / 1.5D));
            for (int i = 0; i < pieces; i++) {
                Vec3d p0 = a.add(d.scale(length * i / pieces)), p1 = a.add(d.scale(length * (i + 1) / pieces));
                float f0 = fade * nearBeam(p0), f1 = fade * nearBeam(p1);
                if (f0 < 0.01F && f1 < 0.01F) {
                    continue;
                }
                strip(p0, p1, 0.9D * pulse, 0.9D * pulse, AMBER, AMBER, 0.35F * f0, 0.35F * f1);
                strip(p0, p1, 0.45D * pulse, 0.45D * pulse, GOLD, GOLD, 0.5F * f0, 0.5F * f1);
                strip(p0, p1, 0.14D, 0.14D, WHITE, WHITE, 0.8F * f0, 0.8F * f1);
            }
            // The coils.
            Vec3d[] basis = basis(d);
            for (int strand = 0; strand < 2; strand++) {
                Vec3d last = null;
                float lastFade = 0F;
                int steps = Math.max(8, (int) (length * 3D));
                for (int s = 0; s <= steps; s++) {
                    double along = length * s / steps;
                    double angle = along * 1.6D - time * 0.7D + strand * Math.PI;
                    double r = 0.55D + 0.12D * Math.sin(along * 0.9D + time);
                    Vec3d pt = a.add(d.scale(along)).add(basis[0].scale(Math.cos(angle) * r)).add(basis[1].scale(Math.sin(angle) * r));
                    float f = fade * nearBeam(pt);
                    if (last != null && (f > 0.01F || lastFade > 0.01F)) {
                        strip(last, pt, 0.07D, 0.07D, GOLD, GOLD, 0.5F * lastFade, 0.5F * f);
                    }
                    last = pt;
                    lastFade = f;
                }
            }
            glow(a, 0.9D * pulse, WHITE, 0.6F * fade);
            glow(a, 1.6D, GOLD, 0.3F * fade);
            glow(b, 1.4D * pulse, WHITE, 0.55F * fade);
            glow(b, 2.4D, AMBER, 0.3F * fade);
            star(b, 0.3D, 1.8D * pulse, 10, phase * 13L, time * 0.1F, GOLD, 0.5F * fade);
        }
    }

    // ------------------------------------------------------------------ the meteor dive

    /** Where he will land: a gold ring closing in on the spot, cross-hairs, and a thin column of light up to him. */
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
            float pulse = 0.6F + 0.4F * MathHelper.sin(time * (0.6F + p * 1.4F));
            Vec3d c = at.subtract(camera).add(0D, 0.08D, 0D);
            float in = Math.min(1F, time / 4F);
            ring(c, UP, radius, 0.25D, GOLD, 0.8F * in);
            ring(c, UP, radius * (1D - p) + 0.6D, 0.4D, WHITE, 0.9F * pulse * in);
            ring(c, UP, radius * 0.45D, 0.15D, AMBER, 0.6F * in);
            for (int i = 0; i < 4; i++) {
                double a = i * Math.PI / 2D + time * 0.02D;
                Vec3d o = new Vec3d(Math.cos(a), 0D, Math.sin(a));
                strip(c.add(o.scale(radius * 0.55D)), c.add(o.scale(radius * 1.15D)), 0.12D, GOLD, 0.8F * in);
            }
            flat(c, radius, GOLD, 0.12F * pulse * in);
            strip(c, c.add(0D, 40D, 0D), 0.25D + 0.3D * p, GOLD, 0.35F * pulse * in);
        }
    }

    /** He lands: the ground flashes, rings race out across it, and gold cracks run outward from the impact. */
    public static class Slam extends FxRenderer.Fx {
        final Vec3d at;
        final double reach;
        final long seed;

        public Slam(Vec3d at, double reach, int ticks, long seed) {
            super(ticks);
            this.at = at;
            this.reach = reach;
            this.seed = seed;
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            float p = progress(partialTicks);
            float fade = 1F - p;
            Vec3d c = at.subtract(camera).add(0D, 0.12D, 0D);
            for (int i = 0; i < 3; i++) {
                float q = MathHelper.clamp(p * 1.6F - i * 0.18F, 0F, 1F);
                double grow = 1D - Math.pow(1D - q, 3D);
                ring(c.add(0D, 0.3D * i, 0D), UP, reach * (1.3D + i * 0.5D) * grow, 0.6D + i * 0.3D,
                        i == 0 ? WHITE : i == 1 ? GOLD : AMBER, 0.8F * (1F - q));
            }
            float flash = Math.max(0F, 1F - p * 3F);
            glow(c.add(0D, 1D, 0D), 5D * flash + 1D, WHITE, flash);
            flat(c, reach * 0.6D, GOLD, 0.5F * fade * fade);
            // Gold fissures across the ground, cooling as they fade.
            Random rand = new Random(seed);
            for (int i = 0; i < 9; i++) {
                double a = i * Math.PI * 2D / 9D + rand.nextDouble() * 0.5D;
                Vec3d last = c;
                double r = 0D;
                double reachThis = reach * (0.6D + rand.nextDouble() * 0.5D) * Math.min(1D, p * 6D);
                while (r < reachThis) {
                    r += 0.8D + rand.nextDouble() * 0.8D;
                    a += (rand.nextDouble() - 0.5D) * 0.6D;
                    Vec3d next = c.add(Math.cos(a) * r, 0D, Math.sin(a) * r);
                    strip(last, next, 0.12D, p < 0.4F ? WHITE : GOLD, 0.9F * fade);
                    last = next;
                }
            }
        }
    }

    /** Gone from here and there in one instant: a streak of gold between the two, flaring at both ends. */
    public static class Blink extends FxRenderer.Fx {
        final Vec3d from, to;

        public Blink(Vec3d from, Vec3d to, int ticks) {
            super(ticks);
            this.from = from;
            this.to = to;
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            float p = progress(partialTicks);
            float fade = (1F - p) * (1F - p);
            Vec3d a = from.subtract(camera), b = to.subtract(camera);
            strip(a, b, 0.6D * (1F - p), GOLD, 0.6F * fade);
            strip(a, b, 0.15D, WHITE, 0.9F * fade);
            glow(a, 1.5D * (1F - p) + 0.3D, GOLD, 0.7F * fade);
            glow(b, 2D * (1F - p) + 0.3D, WHITE, 0.8F * fade);
        }
    }

    // ------------------------------------------------------------------ the cocoon

    /** His cocoon coming apart: shards of gold shell flung outward and tumbling down, and a nova if it hatched. */
    public static class Shards extends FxRenderer.Fx {
        final Vec3d at;
        final boolean hatched;
        final double reach;
        final double[][] shards;

        public Shards(Vec3d at, boolean hatched, double reach, int ticks, long seed) {
            super(ticks);
            this.at = at;
            this.hatched = hatched;
            this.reach = reach;
            Random rand = new Random(seed);
            shards = new double[hatched ? 34 : 22][];
            for (int i = 0; i < shards.length; i++) {
                Vec3d d = new Vec3d(rand.nextGaussian(), rand.nextGaussian() * 0.8D + 0.3D, rand.nextGaussian()).normalize()
                        .scale((hatched ? 0.5D : 0.25D) + rand.nextDouble() * 0.3D);
                shards[i] = new double[]{d.x, d.y, d.z, rand.nextDouble() * 6D, rand.nextDouble() * 0.4D + 0.15D, rand.nextDouble() - 0.5D};
            }
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            float p = progress(partialTicks);
            float time = age + partialTicks;
            float fade = 1F - p;
            Vec3d c = at.subtract(camera);
            if (hatched) {
                double grow = 1D - Math.pow(1D - p, 3D);
                ring(c, UP, reach * 0.8D * grow, 0.5D + grow, WHITE, 0.75F * fade * fade);
                ring(c, new Vec3d(0D, 0D, 1D), reach * 0.5D * grow, 0.4D, GOLD, 0.55F * fade);
                ring(c, new Vec3d(1D, 0D, 0D), reach * 0.5D * grow, 0.4D, GOLD, 0.55F * fade);
                float flash = Math.max(0F, 1F - p * 2.5F);
                glow(c, 4.5D * flash + 0.8D, WHITE, 0.9F * flash);
                glow(c, 7D * fade, GOLD, 0.22F * fade);
            } else {
                float flash = Math.max(0F, 1F - p * 4F);
                glow(c, 3D * flash + 0.5D, GOLD, 0.7F * flash);
            }
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
            for (double[] s : shards) {
                double drag = 1D - Math.exp(-time * 0.12D);
                Vec3d pos = c.add(new Vec3d(s[0], s[1], s[2]).scale(drag / 0.12D)).add(0D, -0.012D * time * time, 0D);
                double spin = s[3] + time * s[5] * 0.6D;
                double size = s[4];
                Vec3d u = new Vec3d(Math.cos(spin), Math.sin(spin * 0.7D), Math.sin(spin)).normalize().scale(size);
                Vec3d v = new Vec3d(-Math.sin(spin), Math.cos(spin * 1.3D), Math.cos(spin)).normalize().scale(size * 0.7D);
                float[] col = hatched ? GOLD : AMBER;
                float a = 0.85F * fade;
                buffer.pos(pos.x + u.x, pos.y + u.y, pos.z + u.z).color(col[0], col[1], col[2], a).endVertex();
                buffer.pos(pos.x + v.x, pos.y + v.y, pos.z + v.z).color(WHITE[0], WHITE[1], WHITE[2], a).endVertex();
                buffer.pos(pos.x - u.x - v.x, pos.y - u.y - v.y, pos.z - u.z - v.z).color(col[0], col[1], col[2], a * 0.5F).endVertex();
            }
            tessellator.draw();
        }
    }

    // ------------------------------------------------------------------ geometry, shared with his renderer

    /** Two directions perpendicular to d and to each other. */
    static Vec3d[] basis(Vec3d d) {
        Vec3d helper = Math.abs(d.y) > 0.9D ? new Vec3d(1D, 0D, 0D) : UP;
        Vec3d u = d.crossProduct(helper).normalize();
        Vec3d v = d.crossProduct(u).normalize();
        return new Vec3d[]{u, v};
    }

    /** A beam fades over the last five blocks before your eye. */
    static float nearBeam(Vec3d relative) {
        return (float) MathHelper.clamp((relative.length() - 1D) / 4D, 0D, 1D);
    }

    /** 0 at the camera, 1 from a couple of blocks out: light right against your eye would only blind you. */
    static float nearFade(double distance) {
        return (float) MathHelper.clamp((distance - 0.6D) / 2D, 0D, 1D);
    }

    /** A soft disc of light at c, always facing the camera (which sits at the origin). */
    static void glow(Vec3d c, double radius, float[] col, float alpha) {
        alpha *= nearFade(c.length());
        if (radius <= 0D || alpha <= 0.003F) {
            return;
        }
        Vec3d[] b = basis(c.lengthSquared() < 1.0E-6 ? new Vec3d(0D, 0D, 1D) : c.normalize());
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(c.x, c.y, c.z).color(col[0], col[1], col[2], Math.min(1F, alpha)).endVertex();
        for (int i = 0; i <= 20; i++) {
            double a = Math.PI * 2D * i / 20D;
            Vec3d o = b[0].scale(Math.cos(a) * radius).add(b[1].scale(Math.sin(a) * radius));
            buffer.pos(c.x + o.x, c.y + o.y, c.z + o.z).color(col[0], col[1], col[2], 0F).endVertex();
        }
        tessellator.draw();
    }

    /** Spikes of light radiating from c toward the camera's view, of uneven length. */
    static void star(Vec3d c, double inner, double outer, int spikes, long seed, float spin, float[] col, float alpha) {
        alpha *= nearFade(c.length() - outer * 0.5D);
        if (alpha <= 0.003F) {
            return;
        }
        Vec3d[] b = basis(c.lengthSquared() < 1.0E-6 ? new Vec3d(0D, 0D, 1D) : c.normalize());
        Random rand = new Random(seed);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < spikes; i++) {
            double a = Math.PI * 2D * (i + rand.nextDouble() * 0.6D) / spikes + spin;
            double len = outer * (0.45D + rand.nextDouble() * 0.55D);
            double w = 0.08D + rand.nextDouble() * 0.06D;
            Vec3d tip = b[0].scale(Math.cos(a) * len).add(b[1].scale(Math.sin(a) * len));
            Vec3d l = b[0].scale(Math.cos(a - w) * inner).add(b[1].scale(Math.sin(a - w) * inner));
            Vec3d r = b[0].scale(Math.cos(a + w) * inner).add(b[1].scale(Math.sin(a + w) * inner));
            buffer.pos(c.x + l.x, c.y + l.y, c.z + l.z).color(col[0], col[1], col[2], alpha).endVertex();
            buffer.pos(c.x + r.x, c.y + r.y, c.z + r.z).color(col[0], col[1], col[2], alpha).endVertex();
            buffer.pos(c.x + tip.x, c.y + tip.y, c.z + tip.z).color(col[0], col[1], col[2], 0F).endVertex();
        }
        tessellator.draw();
    }

    /**
     * A soft-edged ring at c in the plane facing along normal. The nearer any part of it
     * passes to the camera, the fainter it is drawn, so a ring never sweeps across your whole view.
     */
    static void ring(Vec3d c, Vec3d normal, double radius, double thickness, float[] col, float alpha) {
        if (radius <= 0D || alpha <= 0.003F) {
            return;
        }
        Vec3d n = normal.normalize();
        Vec3d toCamera = c.scale(-1D);
        double height = toCamera.dotProduct(n);
        double across = toCamera.subtract(n.scale(height)).length();
        alpha *= (float) MathHelper.clamp((Math.sqrt(height * height + (across - radius) * (across - radius)) - thickness - 0.8D) / 2.5D, 0D, 1D);
        if (alpha <= 0.003F) {
            return;
        }
        Vec3d[] b = basis(n);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        int segments = 48;
        for (int pass = 0; pass < 2; pass++) {
            double r0 = pass == 0 ? Math.max(0D, radius - thickness) : radius;
            double r1 = pass == 0 ? radius : radius + thickness;
            float a0 = pass == 0 ? 0F : alpha, a1 = pass == 0 ? alpha : 0F;
            buffer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
            for (int i = 0; i <= segments; i++) {
                double a = Math.PI * 2D * i / segments;
                Vec3d o = b[0].scale(Math.cos(a)).add(b[1].scale(Math.sin(a)));
                buffer.pos(c.x + o.x * r0, c.y + o.y * r0, c.z + o.z * r0).color(col[0], col[1], col[2], a0).endVertex();
                buffer.pos(c.x + o.x * r1, c.y + o.y * r1, c.z + o.z * r1).color(col[0], col[1], col[2], a1).endVertex();
            }
            tessellator.draw();
        }
    }

    /** A flat, soft disc lying on the ground at c. */
    static void flat(Vec3d c, double radius, float[] col, float alpha) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(c.x, c.y, c.z).color(col[0], col[1], col[2], alpha).endVertex();
        for (int i = 0; i <= 32; i++) {
            double a = Math.PI * 2D * i / 32D;
            buffer.pos(c.x + Math.cos(a) * radius, c.y, c.z + Math.sin(a) * radius).color(col[0], col[1], col[2], 0F).endVertex();
        }
        tessellator.draw();
    }

    /** A camera-facing strip between two camera-relative points, brightest down its middle. */
    static void strip(Vec3d a, Vec3d b, double width, float[] col, float alpha) {
        strip(a, b, width, width, col, col, alpha, alpha);
    }

    /** A camera-facing strip that can change width, colour and brightness from one end to the other. */
    static void strip(Vec3d a, Vec3d b, double wa, double wb, float[] ca, float[] cb, float alphaA, float alphaB) {
        if (alphaA <= 0.003F && alphaB <= 0.003F) {
            return;
        }
        Vec3d dir = b.subtract(a);
        Vec3d side = dir.crossProduct(a.add(b).scale(0.5D));
        side = side.lengthSquared() < 1.0E-10 ? UP : side.normalize();
        Vec3d sa = side.scale(wa), sb = side.scale(wb);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        for (int half = -1; half <= 1; half += 2) {
            buffer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
            buffer.pos(a.x + sa.x * half, a.y + sa.y * half, a.z + sa.z * half).color(ca[0], ca[1], ca[2], 0F).endVertex();
            buffer.pos(a.x, a.y, a.z).color(ca[0], ca[1], ca[2], alphaA).endVertex();
            buffer.pos(b.x + sb.x * half, b.y + sb.y * half, b.z + sb.z * half).color(cb[0], cb[1], cb[2], 0F).endVertex();
            buffer.pos(b.x, b.y, b.z).color(cb[0], cb[1], cb[2], alphaB).endVertex();
            tessellator.draw();
        }
    }

    /** Puts back the additive, depth-writeless state these effects are drawn in. */
    static void additive() {
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        GlStateManager.depthMask(false);
    }
}
