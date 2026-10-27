package com.matoon.eternity.client.render;

import com.matoon.eternity.entity.EntityDestroyer;
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

import static com.matoon.eternity.client.render.WarlockFx.basis;
import static com.matoon.eternity.client.render.WarlockFx.flat;
import static com.matoon.eternity.client.render.WarlockFx.glow;
import static com.matoon.eternity.client.render.WarlockFx.nearBeam;
import static com.matoon.eternity.client.render.WarlockFx.ring;
import static com.matoon.eternity.client.render.WarlockFx.star;
import static com.matoon.eternity.client.render.WarlockFx.strip;

/**
 * Odin's Vault's effects: the Destroyer's fire, and the Casket's winter. Drawn additively, like
 * everything else, except the ice that closes over whatever the Casket freezes.
 * Positions are world coordinates; geometry is drawn relative to the camera.
 */
@SideOnly(Side.CLIENT)
public final class VaultFx {

    // The Destroyer's furnace: white-hot, yellow, orange, and the red at the edges of it.
    static final float[] WHITE = {1F, 0.96F, 0.85F};
    static final float[] YELLOW = {1F, 0.82F, 0.35F};
    static final float[] ORANGE = {1F, 0.5F, 0.1F};
    static final float[] RED = {0.9F, 0.18F, 0.04F};
    // The Casket's winter.
    static final float[] FROST = {0.85F, 0.95F, 1F};
    static final float[] ICE = {0.45F, 0.75F, 1F};
    static final float[] DEEP = {0.2F, 0.45F, 1F};
    // The Einherjar's gold.
    static final float[] GOLD = {1F, 0.84F, 0.45F};

    private static final Vec3d UP = new Vec3d(0D, 1D, 0D);

    private VaultFx() {
    }

    // ------------------------------------------------------------------ the Destroyer

    /** Its ray, for one instant: a white-hot core in a sheath of yellow and orange fire, rippling, and a splash where it lands. */
    public static class Ray extends FxRenderer.Fx {
        final Vec3d from, to;
        final int phase;
        public boolean replaced;

        public Ray(Vec3d from, Vec3d to, int phase, int ticks) {
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
            float fade = 1F - Math.max(0F, p - 0.5F) * 2F;
            Vec3d a = from.subtract(camera), b = to.subtract(camera);
            float time = phase + age + partialTicks;
            float pulse = 0.88F + 0.12F * MathHelper.sin(time * 2.3F);
            Vec3d axis = b.subtract(a);
            double length = axis.length();
            Vec3d d = axis.normalize();
            int pieces = Math.max(1, (int) Math.ceil(length / 1.5D));
            for (int i = 0; i < pieces; i++) {
                double s0 = length * i / pieces, s1 = length * (i + 1) / pieces;
                Vec3d p0 = a.add(d.scale(s0)), p1 = a.add(d.scale(s1));
                float f0 = fade * nearBeam(p0), f1 = fade * nearBeam(p1);
                if (f0 < 0.01F && f1 < 0.01F) {
                    continue;
                }
                // Swelling a little as it goes, a ripple running down it.
                double w0 = (0.9D + 0.2D * Math.sin(s0 * 0.7D - time * 1.3D)) * pulse, w1 = (0.9D + 0.2D * Math.sin(s1 * 0.7D - time * 1.3D)) * pulse;
                strip(p0, p1, w0 * 1.6D, w1 * 1.6D, RED, RED, 0.22F * f0, 0.22F * f1);
                strip(p0, p1, w0, w1, ORANGE, ORANGE, 0.45F * f0, 0.45F * f1);
                strip(p0, p1, w0 * 0.55D, w1 * 0.55D, YELLOW, YELLOW, 0.65F * f0, 0.65F * f1);
                strip(p0, p1, 0.2D, 0.2D, WHITE, WHITE, 0.95F * f0, 0.95F * f1);
            }
            // Rings of heat running down it.
            for (int k = 0; k < 6; k++) {
                double along = ((time * 1.6D + k * length / 6D) % length);
                Vec3d c = a.add(d.scale(along));
                float f = fade * nearBeam(c) * 0.4F;
                ring(c, d, 1.1D + 0.2D * Math.sin(k + time), 0.25D, YELLOW, f);
            }
            glow(a, 1.3D * pulse, WHITE, 0.7F * fade);
            glow(a, 2.6D, ORANGE, 0.35F * fade);
            glow(b, 1.8D * pulse, WHITE, 0.6F * fade);
            glow(b, 3.6D, ORANGE, 0.35F * fade);
            star(b, 0.4D, 2.6D * pulse, 14, phase * 31L, time * 0.12F, YELLOW, 0.5F * fade);
            star(a, 0.3D, 1.8D * pulse, 10, phase * 17L, -time * 0.1F, YELLOW, 0.35F * fade);
        }
    }

    /** Stone glowing where the ray has passed, cooling from yellow to a dull red. */
    public static class Molten extends FxRenderer.Fx {
        final Vec3d at;
        final double radius;

        public Molten(Vec3d at, double radius, int ticks) {
            super(ticks);
            this.at = at;
            this.radius = radius;
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            float p = progress(partialTicks);
            Vec3d c = at.subtract(camera).add(0D, 0.02D, 0D);
            float heat = 1F - p;
            float[] col = {1F, 0.25F + 0.6F * heat * heat, 0.05F + 0.3F * heat * heat * heat};
            flat(c, radius * (0.8D + 0.3D * p), col, 0.6F * heat);
            flat(c, radius * 0.45D, YELLOW, 0.5F * heat * heat);
        }
    }

    /** Heat gathering in its face before the ray: sparks streaming in, the light swelling. */
    public static class Charge extends FxRenderer.Fx {
        final int id;
        final long seed;

        public Charge(int id, long seed, int ticks) {
            super(ticks);
            this.id = id;
            this.seed = seed;
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            Entity entity = Minecraft.getMinecraft().world.getEntityByID(id);
            if (!(entity instanceof EntityDestroyer)) {
                return;
            }
            float p = progress(partialTicks);
            EntityDestroyer d = (EntityDestroyer) entity;
            Vec3d c = d.face().add(entity.posX - entity.lastTickPosX, 0D, entity.posZ - entity.lastTickPosZ).subtract(camera);
            float time = age + partialTicks;
            Random rand = new Random(seed);
            for (int i = 0; i < 26; i++) {
                Vec3d dir = new Vec3d(rand.nextGaussian(), rand.nextGaussian() * 0.6D, rand.nextGaussian()).normalize();
                float local = (time * (0.05F + rand.nextFloat() * 0.04F) + rand.nextFloat()) % 1F;
                double r = 3.5D * (1D - local);
                Vec3d head = c.add(dir.scale(r));
                Vec3d tail = c.add(dir.scale(Math.min(3.8D, r + 0.6D)));
                strip(tail, head, 0.04D, 0.07D, ORANGE, YELLOW, 0F, 0.8F * local * p);
            }
            glow(c, 0.5D + 1.6D * p, YELLOW, 0.3F + 0.5F * p);
            glow(c, 0.3D + 0.6D * p, WHITE, 0.4F + 0.6F * p);
            star(c, 0.2D, 0.8D + 1.6D * p, 12, seed + age / 2, time * 0.15F, ORANGE, 0.5F * p);
        }
    }

    /** Rings of force racing out across the floor from where its fists came down, and a flash of heat where they struck. */
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
            Vec3d c = at.subtract(camera).add(0D, 0.08D, 0D);
            for (int k = 0; k < rings; k++) {
                float local = MathHelper.clamp((p - k * 0.12F) / (1F - k * 0.12F), 0F, 1F);
                if (local <= 0F || local >= 1F) {
                    continue;
                }
                double grow = 1D - Math.pow(1D - local, 2.2D);
                float fade = (1F - local) * (1F - local);
                ring(c, UP, reach * grow, 0.5D + 0.6D * grow, k == 0 ? WHITE : YELLOW, 0.75F * fade);
                ring(c, UP, reach * grow * 0.96D, 1.1D, ORANGE, 0.35F * fade);
            }
            float flash = 1F - MathHelper.clamp(p * 4F, 0F, 1F);
            flat(c, 2.5D + reach * 0.2D * p, ORANGE, 0.6F * flash);
            glow(c.add(0D, 0.6D, 0D), 2.5D * flash + 0.5D, WHITE, 0.6F * flash);
        }
    }

    /** One of its blows landing: a flash, a burst of hot sparks, a shock ring driven along the blow. */
    public static class Impact extends FxRenderer.Fx {
        final Vec3d at, dir;
        final long seed;
        final float weight;

        public Impact(Vec3d at, Vec3d dir, float weight, long seed) {
            super(10);
            this.at = at;
            this.dir = dir.lengthSquared() < 1.0E-6 ? UP : dir.normalize();
            this.weight = weight;
            this.seed = seed;
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            float p = progress(partialTicks);
            float fade = 1F - p;
            Vec3d c = at.subtract(camera);
            glow(c, (1.2D + 1.4D * p) * weight, WHITE, 0.8F * fade * fade);
            star(c, 0.2D, (1.4D + 2.2D * p) * weight, 14, seed, 0F, YELLOW, 0.75F * fade);
            ring(c.add(dir.scale(0.6D * p)), dir, (0.4D + 2.8D * p) * weight, 0.4D, ORANGE, 0.5F * fade);
        }
    }

    // ------------------------------------------------------------------ the Casket of Ancient Winters

    /** The winter pouring out of the Casket in a gale: streaks of frost racing out in a widening cone, a cold mist behind them. */
    public static class Gale extends FxRenderer.Fx {
        final Vec3d from, dir;
        final double[][] streaks;

        public Gale(Vec3d from, Vec3d dir, int ticks, long seed) {
            super(ticks);
            this.from = from;
            this.dir = dir.normalize();
            Random rand = new Random(seed);
            Vec3d[] b = basis(this.dir);
            streaks = new double[70][5];
            for (double[] s : streaks) {
                double a = rand.nextDouble() * Math.PI * 2D, r = rand.nextDouble() * 0.42D;
                Vec3d d = this.dir.add(b[0].scale(Math.cos(a) * r)).add(b[1].scale(Math.sin(a) * r)).normalize();
                s[0] = d.x;
                s[1] = d.y;
                s[2] = d.z;
                s[3] = 0.9D + rand.nextDouble() * 0.7D;
                s[4] = rand.nextDouble() * 0.35D;
            }
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            float p = progress(partialTicks);
            Vec3d c = from.subtract(camera);
            float time = age + partialTicks;
            for (double[] s : streaks) {
                double t = time - s[4] * lifetime;
                if (t <= 0D) {
                    continue;
                }
                Vec3d d = new Vec3d(s[0], s[1], s[2]);
                double head = Math.min(19D, t * s[3]);
                double tail = Math.max(0D, head - 2.5D);
                float fade = (float) (1D - head / 19D) * (1F - p);
                strip(c.add(d.scale(tail)), c.add(d.scale(head)), 0.05D, 0.12D + head * 0.02D, ICE, FROST, 0F, 0.7F * fade);
            }
            float mist = (1F - p) * (1F - p);
            for (int k = 1; k <= 5; k++) {
                double along = k * 3D * Math.min(1D, time / 6D);
                glow(c.add(dir.scale(along)), 0.5D + along * 0.28D, ICE, 0.07F * mist);
            }
            glow(c, 1.2D, FROST, 0.5F * mist);
        }
    }

    /** Winter bursting out all round: a ring of frost racing out over the ground, shards of ice flung up in it. */
    public static class Burst extends FxRenderer.Fx {
        final Vec3d at;
        final double reach;
        final long seed;

        public Burst(Vec3d at, double reach, int ticks, long seed) {
            super(ticks);
            this.at = at;
            this.reach = reach;
            this.seed = seed;
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            float p = progress(partialTicks);
            Vec3d c = at.subtract(camera).add(0D, 0.1D, 0D);
            double grow = 1D - Math.pow(1D - p, 3D);
            float fade = (1F - p);
            ring(c, UP, reach * grow, 0.6D + grow, FROST, 0.8F * fade);
            ring(c, UP, reach * grow * 0.9D, 1.5D, ICE, 0.45F * fade);
            flat(c, reach * grow, DEEP, 0.1F * fade);
            glow(c.add(0D, 1D, 0D), 3D * (1F - p) + 0.5D, FROST, 0.6F * fade * fade);
            Random rand = new Random(seed);
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
            for (int i = 0; i < 40; i++) {
                double a = rand.nextDouble() * Math.PI * 2D;
                double r = reach * grow * (0.6D + rand.nextDouble() * 0.4D);
                double h = (0.4D + rand.nextDouble() * 1.6D) * Math.sin(Math.min(1D, p * 2.5D) * Math.PI);
                Vec3d base = c.add(Math.cos(a) * r, 0D, Math.sin(a) * r);
                Vec3d tip = base.add(Math.cos(a) * 0.3D, h, Math.sin(a) * 0.3D);
                Vec3d side = new Vec3d(-Math.sin(a), 0D, Math.cos(a)).scale(0.18D);
                buffer.pos(base.x - side.x, base.y, base.z - side.z).color(ICE[0], ICE[1], ICE[2], 0.7F * fade).endVertex();
                buffer.pos(base.x + side.x, base.y, base.z + side.z).color(ICE[0], ICE[1], ICE[2], 0.7F * fade).endVertex();
                buffer.pos(tip.x, tip.y, tip.z).color(FROST[0], FROST[1], FROST[2], 0.9F * fade).endVertex();
            }
            tessellator.draw();
        }
    }

    /** Ice closed over something the Casket froze: a pale shell round it, bright along its edges, that cracks away at the end. */
    public static class Shell extends FxRenderer.Fx {
        final int id;
        final double width, height;

        public Shell(int id, double width, double height, int ticks) {
            super(ticks);
            this.id = id;
            this.width = width;
            this.height = height;
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            Entity entity = Minecraft.getMinecraft().world.getEntityByID(id);
            if (entity == null || entity.isDead) {
                age = lifetime;
                return;
            }
            float p = progress(partialTicks);
            float alpha = Math.min(1F, (age + partialTicks) / 4F) * (1F - MathHelper.clamp((p - 0.85F) / 0.15F, 0F, 1F));
            double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks - camera.x;
            double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks - camera.y;
            double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks - camera.z;
            double hw = width * 0.5D + 0.25D, h = height + 0.25D;
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            box(x - hw, y - 0.05D, z - hw, x + hw, y + h, z + hw, 0.55F, 0.8F, 1F, 0.3F * alpha);
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            edges(x - hw, y - 0.05D, z - hw, x + hw, y + h, z + hw, FROST, 0.6F * alpha);
        }

        private static void box(double x0, double y0, double z0, double x1, double y1, double z1, float r, float g, float b, float a) {
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            double[][] faces = {
                    {x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0}, {x0, y0, z1, x0, y1, z1, x1, y1, z1, x1, y0, z1},
                    {x0, y0, z0, x0, y1, z0, x0, y1, z1, x0, y0, z1}, {x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0},
                    {x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1}, {x0, y0, z0, x0, y0, z1, x1, y0, z1, x1, y0, z0}};
            for (double[] f : faces) {
                for (int i = 0; i < 4; i++) {
                    float shade = i >= 2 ? 1F : 0.7F;
                    buffer.pos(f[i * 3], f[i * 3 + 1], f[i * 3 + 2]).color(r * shade, g * shade, b, a * (i >= 2 ? 0.7F : 1F)).endVertex();
                }
            }
            tessellator.draw();
        }

        private static void edges(double x0, double y0, double z0, double x1, double y1, double z1, float[] col, float a) {
            double[][] e = {{x0, y0, z0, x1, y0, z0}, {x0, y1, z0, x1, y1, z0}, {x0, y0, z1, x1, y0, z1}, {x0, y1, z1, x1, y1, z1},
                    {x0, y0, z0, x0, y1, z0}, {x1, y0, z0, x1, y1, z0}, {x0, y0, z1, x0, y1, z1}, {x1, y0, z1, x1, y1, z1},
                    {x0, y0, z0, x0, y0, z1}, {x1, y0, z0, x1, y0, z1}, {x0, y1, z0, x0, y1, z1}, {x1, y1, z0, x1, y1, z1}};
            for (double[] s : e) {
                strip(new Vec3d(s[0], s[1], s[2]), new Vec3d(s[3], s[4], s[5]), 0.06D, col, a);
            }
        }
    }

    // ------------------------------------------------------------------ the Einherjar

    /** A blow turned by a shield: a ring of struck bronze and a spray of golden sparks. */
    public static class Clang extends FxRenderer.Fx {
        final Vec3d at, dir;
        final long seed;

        public Clang(Vec3d at, Vec3d dir, long seed) {
            super(7);
            this.at = at;
            this.dir = dir.lengthSquared() < 1.0E-6 ? UP : dir.normalize();
            this.seed = seed;
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            float p = progress(partialTicks);
            float fade = 1F - p;
            Vec3d c = at.subtract(camera);
            glow(c, 0.5D + 0.6D * p, WHITE, 0.7F * fade);
            star(c, 0.08D, 0.6D + 0.8D * p, 10, seed, 0F, GOLD, 0.8F * fade);
            ring(c, dir, 0.2D + 0.9D * p, 0.12D, GOLD, 0.5F * fade);
        }
    }
}
