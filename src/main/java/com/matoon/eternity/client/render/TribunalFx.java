package com.matoon.eternity.client.render;

import com.matoon.eternity.entity.EntityLivingTribunal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.Random;

/** The visible shape of the Living Tribunal's judgements. Drawn by {@link FxRenderer#renderAll}. */
@SideOnly(Side.CLIENT)
public final class TribunalFx {

    private static final Vec3d UP = new Vec3d(0, 1, 0);

    private TribunalFx() {
    }

    /**
     * A circle of judgement burned onto the floor: two rings turning against each other,
     * a ring of runes between them and a six-pointed star inside, filling with light as
     * the blow it marks draws near, then flaring as it lands.
     */
    public static class Sigil extends FxRenderer.Fx {
        final Vec3d centre;
        final double radius;

        public Sigil(Vec3d centre, double radius, int ticks) {
            super(ticks + 8);
            this.centre = centre.add(0D, 0.06D, 0D);
            this.radius = radius;
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            float time = age + partialTicks;
            float charge = MathHelper.clamp(time / (lifetime - 8), 0F, 1F);
            float flare = time > lifetime - 8 ? 1F - (time - (lifetime - 8)) / 8F : 0F;
            float alpha = Math.min(1F, time / 4F) * (flare > 0F ? flare : 1F);
            Vec3d c = centre.subtract(camera);
            float spin = time * 0.04F;
            float r = 1F, g = 0.78F + 0.2F * charge, b = 0.3F + 0.6F * flare;

            ring(c, radius, radius * 0.06D, spin, r, g, b, 0.9F * alpha);
            ring(c, radius * 0.72D, radius * 0.04D, -spin * 1.6F, r, g, b, 0.75F * alpha);
            // Runes: short bars around the band between the rings.
            for (int i = 0; i < 24; i++) {
                double a = Math.PI * 2D * i / 24D + spin;
                double inner = radius * (0.78D + (i % 3) * 0.04D);
                double outer = radius * 0.92D;
                line(c.add(polar(a, inner)), c.add(polar(a + 0.06D, outer)), radius * 0.025D, r, g, b, 0.7F * alpha);
            }
            // The star of the court.
            for (int k = 0; k < 2; k++) {
                for (int i = 0; i < 3; i++) {
                    double a0 = -spin * 0.5D + k * Math.PI / 3D + i * Math.PI * 2D / 3D;
                    double a1 = a0 + Math.PI * 2D / 3D;
                    line(c.add(polar(a0, radius * 0.7D)), c.add(polar(a1, radius * 0.7D)), radius * 0.03D, r, g, b, 0.8F * alpha);
                }
            }
            // It fills with light as the moment comes.
            disc(c, radius * (0.2D + 0.5D * charge), 1F, 0.85F, 0.45F, (0.15F + 0.5F * charge + flare) * alpha);
        }
    }

    /** The light of Equity's eyes: twin shafts of white fire, scorching where they meet the floor. */
    public static class Gaze extends FxRenderer.Fx {
        final Vec3d from, to;
        /** Half the distance between the two eyes; he is vast, a mortal is not. */
        final double spread;

        public Gaze(Vec3d from, Vec3d to, double spread, int ticks) {
            super(ticks);
            this.from = from;
            this.to = to;
            this.spread = spread;
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            float fade = 1F - progress(partialTicks) * 0.5F;
            double size = Math.max(0.25D, spread / 3.6D);
            Vec3d a = from.subtract(camera);
            Vec3d b = to.subtract(camera);
            Vec3d side = a.subtract(b).crossProduct(UP);
            side = side.lengthSquared() < 1.0E-6 ? new Vec3d(1, 0, 0) : side.normalize();
            for (int eye = -1; eye <= 1; eye += 2) {
                Vec3d start = a.add(side.scale(eye * spread));
                ribbon(start, b, 2.6D * size, 0.45F, 0.75F, 1F, 0.35F * fade);
                ribbon(start, b, 1.0D * size, 0.85F, 0.95F, 1F, 0.7F * fade);
                ribbon(start, b, 0.3D * size, 1F, 1F, 1F, fade);
            }
            disc(b.add(0D, 0.08D, 0D), 4.5D * size, 1F, 0.95F, 0.8F, 0.6F * fade);
            disc(b.add(0D, 0.1D, 0D), 1.8D * size, 1F, 1F, 1F, 0.9F * fade);
            halo(b.add(0D, 1D, 0D), 5D * size, 0.8F, 0.9F, 1F, 0.5F * fade);
        }
    }

    /** Lightning out of the void in his chest: a forked, flickering bolt of violet and white. */
    public static class Bolt extends FxRenderer.Fx {
        final Vec3d[] path;
        final Vec3d[] branch;

        public Bolt(Vec3d from, Vec3d to, int seed, int ticks) {
            super(ticks);
            Random random = new Random(seed);
            path = jagged(from, to, 14, 3.5D, random);
            Vec3d split = path[6];
            branch = jagged(split, split.add((random.nextDouble() - 0.5D) * 14D, -8D - random.nextDouble() * 8D, (random.nextDouble() - 0.5D) * 14D), 7, 2D, random);
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            float p = progress(partialTicks);
            float flicker = (age % 2 == 0 ? 1F : 0.6F) * (1F - p);
            draw(path, camera, 1.4D, flicker);
            draw(branch, camera, 0.7D, flicker * 0.7F);
            Vec3d end = path[path.length - 1].subtract(camera);
            halo(end.add(0D, 1D, 0D), 6D, 0.7F, 0.55F, 1F, 0.7F * flicker);
            disc(end.add(0D, 0.08D, 0D), 3.5D, 0.85F, 0.75F, 1F, 0.6F * flicker);
        }

        private static void draw(Vec3d[] points, Vec3d camera, double width, float alpha) {
            for (int i = 0; i + 1 < points.length; i++) {
                Vec3d a = points[i].subtract(camera);
                Vec3d b = points[i + 1].subtract(camera);
                ribbon(a, b, width * 2.2D, 0.55F, 0.35F, 1F, 0.4F * alpha);
                ribbon(a, b, width * 0.5D, 0.95F, 0.92F, 1F, alpha);
            }
        }

        private static Vec3d[] jagged(Vec3d from, Vec3d to, int segments, double wander, Random random) {
            Vec3d[] points = new Vec3d[segments + 1];
            for (int i = 0; i <= segments; i++) {
                double t = i / (double) segments;
                Vec3d p = from.add(to.subtract(from).scale(t));
                if (i > 0 && i < segments) {
                    double w = wander * Math.sin(t * Math.PI);
                    p = p.add((random.nextDouble() - 0.5D) * 2D * w, (random.nextDouble() - 0.5D) * w, (random.nextDouble() - 0.5D) * 2D * w);
                }
                points[i] = p;
            }
            return points;
        }
    }

    /** He is being drawn in: streams of gold and white spiralling from his heart into the hand that holds the Regulator. */
    public static class Absorb extends FxRenderer.Fx {
        final Vec3d from;
        final int holder;

        public Absorb(Vec3d from, int holder, int ticks) {
            super(ticks);
            this.from = from;
            this.holder = holder;
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            Minecraft mc = Minecraft.getMinecraft();
            Entity entity = mc.world == null ? null : mc.world.getEntityByID(holder);
            if (!(entity instanceof EntityPlayer)) {
                return;
            }
            EntityPlayer player = (EntityPlayer) entity;
            Vec3d look = player.getLook(partialTicks);
            Vec3d right = look.crossProduct(UP);
            right = right.lengthSquared() < 1.0E-6 ? new Vec3d(1, 0, 0) : right.normalize();
            Vec3d hand = new Vec3d(
                    player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks,
                    player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks + player.getEyeHeight() - 0.45D,
                    player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks)
                    .add(look.scale(0.7D)).add(right.scale(0.35D));
            float fade = 1F - progress(partialTicks);
            float time = player.ticksExisted + partialTicks;
            Vec3d a = from.subtract(camera);
            Vec3d b = hand.subtract(camera);
            Vec3d axis = b.subtract(a);
            Vec3d u = axis.crossProduct(UP);
            u = u.lengthSquared() < 1.0E-6 ? new Vec3d(1, 0, 0) : u.normalize();
            Vec3d w = axis.crossProduct(u).normalize();
            for (int s = 0; s < 6; s++) {
                Vec3d prev = null;
                for (int i = 0; i <= 24; i++) {
                    double t = i / 24D;
                    double angle = s * Math.PI / 3D + t * 9D - time * 0.3D;
                    double swirl = 6D * Math.sin(t * Math.PI) * (1D - t * 0.6D);
                    Vec3d p = a.add(axis.scale(t)).add(u.scale(Math.cos(angle) * swirl)).add(w.scale(Math.sin(angle) * swirl));
                    if (prev != null) {
                        boolean gold = s % 2 == 0;
                        ribbon(prev, p, 0.5D, 1F, gold ? 0.82F : 0.95F, gold ? 0.4F : 1F, 0.6F * fade);
                    }
                    prev = p;
                }
            }
            ribbon(a, b, 0.6D, 1F, 1F, 1F, 0.7F * fade);
            halo(b, 1.6D, 1F, 0.95F, 0.8F, 0.9F * fade);
        }
    }

    /**
     * One of the Scales of Balance: a great golden dish hung on three chains of light,
     * dropping out of the sky onto the floor, flaring as it lands.
     */
    public static class Pan extends FxRenderer.Fx {
        final Vec3d centre;
        final double radius;
        final int fall;

        public Pan(Vec3d centre, double radius, int ticks) {
            super(ticks + 10);
            this.centre = centre;
            this.radius = radius;
            this.fall = Math.max(1, ticks);
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            float time = age + partialTicks;
            float t = MathHelper.clamp(time / fall, 0F, 1F);
            float landed = time > fall ? (time - fall) / 10F : 0F;
            float alpha = Math.min(1F, time / 5F) * (1F - landed);
            double height = radius * 4D * (1D - t * t);
            Vec3d c = centre.subtract(camera).add(0D, 0.1D + height, 0D);
            Vec3d apex = c.add(0D, radius * 2.6D, 0D);
            disc(c, radius, 1F, 0.8F, 0.35F, 0.55F * alpha);
            ring(c, radius, radius * 0.05D, time * 0.02F, 1F, 0.88F, 0.5F, 0.9F * alpha);
            ring(c.add(0D, radius * 0.25D, 0D), radius * 0.8D, radius * 0.03D, -time * 0.02F, 1F, 0.85F, 0.45F, 0.6F * alpha);
            for (int i = 0; i < 3; i++) {
                double a = Math.PI * 2D * i / 3D;
                ribbon(c.add(polar(a, radius)), apex, radius * 0.04D, 1F, 0.9F, 0.6F, 0.8F * alpha);
            }
            ribbon(apex, apex.add(0D, 200D, 0D), radius * 0.06D, 1F, 0.95F, 0.7F, 0.5F * alpha);
            if (landed > 0F) {
                halo(centre.subtract(camera).add(0D, 1D, 0D), radius * (1D + landed * 1.5D), 1F, 0.9F, 0.6F, 0.8F * (1F - landed));
            }
        }
    }

    /** A cage of golden bars springing up around a point and squeezing in, then bursting. */
    public static class Cage extends FxRenderer.Fx {
        final Vec3d centre;
        final double radius;
        final int hold;

        public Cage(Vec3d centre, double radius, int ticks) {
            super(ticks + 8);
            this.centre = centre;
            this.radius = radius;
            this.hold = Math.max(1, ticks);
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            float time = age + partialTicks;
            float t = MathHelper.clamp(time / hold, 0F, 1F);
            float burst = time > hold ? (time - hold) / 8F : 0F;
            float alpha = Math.min(1F, time / 6F) * (1F - burst);
            double r = radius * (1D - 0.55D * t * t);
            double height = radius * 2.4D * Math.min(1F, time / 8F);
            Vec3d c = centre.subtract(camera);
            for (int i = 0; i < 16; i++) {
                double a = Math.PI * 2D * i / 16D + time * 0.01D;
                Vec3d base = c.add(polar(a, r));
                ribbon(base, base.add(0D, height, 0D), radius * 0.05D, 1F, 0.85F, 0.4F, 0.85F * alpha);
            }
            ring(c.add(0D, 0.1D, 0D), r, radius * 0.06D, 0F, 1F, 0.85F, 0.45F, alpha);
            ring(c.add(0D, height, 0D), r, radius * 0.06D, 0F, 1F, 0.85F, 0.45F, alpha);
            ring(c.add(0D, height * 0.5D, 0D), r, radius * 0.03D, time * 0.05F, 1F, 0.95F, 0.7F, 0.5F * alpha);
            if (burst > 0F) {
                halo(c.add(0D, height * 0.5D, 0D), radius * (1D + burst * 2D), 1F, 0.95F, 0.75F, 0.9F * (1F - burst));
            }
        }
    }

    /** The floor opening onto the void: a pool of stars ringed in violet, everything spiralling into it. */
    public static class VoidPool extends FxRenderer.Fx {
        final Vec3d centre;
        final double radius;

        public VoidPool(Vec3d centre, double radius, int ticks) {
            super(ticks);
            this.centre = centre;
            this.radius = radius;
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            float time = age + partialTicks;
            float open = Math.min(1F, time / 10F) * Math.min(1F, (lifetime - time) / 10F);
            double r = radius * open;
            Vec3d c = centre.subtract(camera).add(0D, 0.07D, 0D);
            Cosmic.draw(() -> fan(c, r), 10, open, 1.6F * open, 0.8F, 0.5F, 1F, false);
            FxRenderer.additiveState();
            ring(c.add(0D, 0.02D, 0D), r, radius * 0.06D, time * 0.05F, 0.7F, 0.35F, 1F, 0.9F * open);
            ring(c.add(0D, 0.03D, 0D), r * 1.25D, radius * 0.03D, -time * 0.08F, 0.55F, 0.25F, 1F, 0.5F * open);
            for (int i = 0; i < 10; i++) {
                double a0 = Math.PI * 2D * i / 10D - time * 0.06D;
                Vec3d prev = null;
                for (int k = 0; k <= 8; k++) {
                    double t = k / 8D;
                    Vec3d p = c.add(polar(a0 + t * 2.2D, r * 1.6D * (1D - t)));
                    if (prev != null) {
                        line(prev, p, radius * 0.02D, 0.75F, 0.5F, 1F, 0.6F * open * (float) t);
                    }
                    prev = p;
                }
            }
        }
    }

    /** Golden blades circling a point, faster and closer, until they meet. */
    public static class Blades extends FxRenderer.Fx {
        final Vec3d centre;
        final double radius;
        final int close;

        public Blades(Vec3d centre, double radius, int ticks) {
            super(ticks + 8);
            this.centre = centre;
            this.radius = radius;
            this.close = Math.max(1, ticks);
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            float time = age + partialTicks;
            float t = MathHelper.clamp(time / close, 0F, 1F);
            float burst = time > close ? (time - close) / 8F : 0F;
            float alpha = Math.min(1F, time / 6F) * (1F - burst);
            double r = radius * (1D - t * t * t);
            double size = radius * 0.3D;
            double spin = time * (0.03D + 0.12D * t * t);
            Vec3d c = centre.subtract(camera).add(0D, size * 0.8D, 0D);
            for (int i = 0; i < 12; i++) {
                double a = Math.PI * 2D * i / 12D + spin;
                Vec3d at = c.add(polar(a, r));
                Vec3d tangent = polar(a + Math.PI / 2D, size * 0.6D);
                ribbon(at.subtract(0D, size, 0D), at.add(0D, size, 0D), size * 0.18D, 1F, 0.85F, 0.4F, 0.9F * alpha);
                ribbon(at.subtract(tangent), at.add(tangent), size * 0.05D, 1F, 0.95F, 0.7F, 0.5F * alpha);
            }
            ring(centre.subtract(camera).add(0D, 0.08D, 0D), r, radius * 0.02D, 0F, 1F, 0.85F, 0.45F, 0.4F * alpha);
            if (burst > 0F) {
                halo(c, radius * 0.4D * (1D + burst * 3D), 1F, 0.95F, 0.7F, (1F - burst));
            }
        }
    }

    /** A column of white-gold light bursting out of the floor and fading. */
    public static class Pillar extends FxRenderer.Fx {
        final Vec3d base;
        final double height;

        public Pillar(Vec3d base, double height, int ticks) {
            super(ticks);
            this.base = base;
            this.height = height;
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            float time = age + partialTicks;
            float rise = Math.min(1F, time / 4F);
            float fade = 1F - progress(partialTicks);
            double width = Math.max(0.6D, height / 12D);
            Vec3d b = base.subtract(camera);
            Vec3d top = b.add(0D, height * rise, 0D);
            ribbon(b, top, width * 1.8D, 0.95F, 0.8F, 0.45F, 0.45F * fade);
            ribbon(b, top, width * 0.6D, 1F, 0.97F, 0.85F, fade);
            disc(b.add(0D, 0.1D, 0D), width * 3D, 1F, 0.9F, 0.6F, 0.7F * fade);
            halo(top, width * 2.5D, 1F, 0.95F, 0.8F, 0.6F * fade);
        }
    }

    /** A wall of light racing outward across the floor in a ring. Jump it. */
    public static class Nova extends FxRenderer.Fx {
        final Vec3d centre;
        final double reach;

        public Nova(Vec3d centre, double reach, int ticks) {
            super(ticks);
            this.centre = centre;
            this.reach = reach;
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            float t = progress(partialTicks);
            double r = reach * t;
            float alpha = 1F - t * 0.6F;
            Vec3d c = centre.subtract(camera).add(0D, 0.05D, 0D);
            wall(c, r, 2.4D, 1F, 0.88F, 0.55F, 0.8F * alpha);
            ring(c, r, 0.35D, 0F, 1F, 1F, 0.9F, alpha);
            ring(c, r * 0.97D, 1.4D, 0F, 1F, 0.8F, 0.4F, 0.35F * alpha);
        }
    }

    /** A wall of light sweeping the length of the hall, with one gap in it. */
    public static class Sweep extends FxRenderer.Fx {
        final double gapX, floorY, fromZ, toZ;

        public Sweep(double gapX, double floorY, double fromZ, double toZ, int ticks) {
            super(ticks);
            this.gapX = gapX;
            this.floorY = floorY;
            this.fromZ = fromZ;
            this.toZ = toZ;
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            float t = progress(partialTicks);
            double z = fromZ + (toZ - fromZ) * t - camera.z;
            double y0 = floorY - camera.y;
            double gap = 4.5D;
            float alpha = Math.min(1F, (age + partialTicks) / 6F) * (t > 0.95F ? (1F - t) * 20F : 1F);
            panel(-30D - camera.x, gapX - gap - camera.x, y0, z, 1F, 0.85F, 0.45F, alpha);
            panel(gapX + gap - camera.x, 30D - camera.x, y0, z, 1F, 0.85F, 0.45F, alpha);
            // The gap is marked on the floor ahead of the wall.
            line(new Vec3d(gapX - gap - camera.x, y0 + 0.08D, z), new Vec3d(gapX - gap - camera.x, y0 + 0.08D, toZ - camera.z), 0.15D, 1F, 0.95F, 0.7F, 0.5F * alpha);
            line(new Vec3d(gapX + gap - camera.x, y0 + 0.08D, z), new Vec3d(gapX + gap - camera.x, y0 + 0.08D, toZ - camera.z), 0.15D, 1F, 0.95F, 0.7F, 0.5F * alpha);
        }

        private static void panel(double x0, double x1, double y0, double z, float r, float g, float b, float alpha) {
            if (x1 <= x0) {
                return;
            }
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            buffer.pos(x0, y0, z).color(r, g, b, 0.85F * alpha).endVertex();
            buffer.pos(x1, y0, z).color(r, g, b, 0.85F * alpha).endVertex();
            buffer.pos(x1, y0 + 22D, z).color(r, g, b, 0F).endVertex();
            buffer.pos(x0, y0 + 22D, z).color(r, g, b, 0F).endVertex();
            buffer.pos(x0, y0, z).color(1F, 1F, 1F, alpha).endVertex();
            buffer.pos(x1, y0, z).color(1F, 1F, 1F, alpha).endVertex();
            buffer.pos(x1, y0 + 1.2D, z).color(1F, 1F, 1F, 0F).endVertex();
            buffer.pos(x0, y0 + 1.2D, z).color(1F, 1F, 1F, 0F).endVertex();
            tessellator.draw();
        }
    }

    /** A ghost of the Tribunal, translucent gold, bringing his fist down beside the real one. */
    public static class Echo extends FxRenderer.Fx {
        private static final ModelTribunal MODEL = new ModelTribunal();
        final Vec3d at;
        final float yaw;

        public Echo(Vec3d at, float yaw, int ticks) {
            super(ticks);
            this.at = at;
            this.yaw = yaw;
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            float t = progress(partialTicks);
            float alpha = Math.min(1F, t * 6F) * Math.min(1F, (1F - t) * 5F);
            Vec3d c = at.subtract(camera);
            GlStateManager.pushMatrix();
            GlStateManager.translate(c.x, c.y, c.z);
            GlStateManager.rotate(-yaw, 0F, 1F, 0F);
            float s = EntityLivingTribunal.SCALE;
            GlStateManager.scale(s, -s, -s);
            MODEL.drawGhost(ModelTribunal.Pose.GAVEL, Math.min(1F, t * 1.25F), alpha, age + partialTicks);
            GlStateManager.popMatrix();
            FxRenderer.additiveState();
        }
    }

    /** Weight bearing down: violet streaks falling all around, and a ring pressing on the floor. */
    public static class Gravity extends FxRenderer.Fx {
        final Vec3d centre;
        final double radius;

        public Gravity(Vec3d centre, double radius, int ticks) {
            super(ticks);
            this.centre = centre;
            this.radius = radius;
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            float time = age + partialTicks;
            float alpha = Math.min(1F, time / 6F) * Math.min(1F, (lifetime - time) / 6F);
            Vec3d c = centre.subtract(camera);
            Random random = new Random(77L);
            for (int i = 0; i < 40; i++) {
                double a = random.nextDouble() * Math.PI * 2D;
                double d = Math.sqrt(random.nextDouble()) * radius;
                double speed = 1.5D + random.nextDouble() * 1.5D;
                double fall = (random.nextDouble() * 30D + time * speed) % 30D;
                Vec3d top = c.add(polar(a, d)).add(0D, 30D - fall, 0D);
                ribbon(top, top.add(0D, -4D, 0D), 0.12D, 0.7F, 0.45F, 1F, 0.7F * alpha);
            }
            ring(c.add(0D, 0.08D, 0D), radius, 0.4D, time * 0.03F, 0.65F, 0.35F, 1F, 0.8F * alpha);
            disc(c.add(0D, 0.06D, 0D), radius, 0.45F, 0.2F, 0.85F, 0.3F * alpha);
        }
    }

    // ------------------------------------------------------------------ geometry

    /** A flat fan of a circle, for Cosmic to texture. */
    private static void fan(Vec3d c, double radius) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION);
        buffer.pos(c.x, c.y, c.z).endVertex();
        for (int i = 0; i <= 32; i++) {
            double a = Math.PI * 2D * i / 32D;
            buffer.pos(c.x + Math.cos(a) * radius, c.y, c.z + Math.sin(a) * radius).endVertex();
        }
        tessellator.draw();
    }

    /** A low circular wall of light, bright at its foot. */
    private static void wall(Vec3d c, double radius, double height, float r, float g, float bl, float alpha) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUAD_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i <= 64; i++) {
            double a = Math.PI * 2D * i / 64D;
            double x = c.x + Math.cos(a) * radius;
            double z = c.z + Math.sin(a) * radius;
            buffer.pos(x, c.y, z).color(r, g, bl, alpha).endVertex();
            buffer.pos(x, c.y + height, z).color(r, g, bl, 0F).endVertex();
        }
        tessellator.draw();
    }

    private static Vec3d polar(double angle, double radius) {
        return new Vec3d(Math.cos(angle) * radius, 0D, Math.sin(angle) * radius);
    }

    /** A flat bar of light lying on the floor. */
    private static void line(Vec3d a, Vec3d b, double width, float r, float g, float bl, float alpha) {
        Vec3d side = b.subtract(a).crossProduct(UP);
        side = side.lengthSquared() < 1.0E-8 ? new Vec3d(width, 0, 0) : side.normalize().scale(width);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(a.x - side.x, a.y, a.z - side.z).color(r, g, bl, alpha).endVertex();
        buffer.pos(a.x + side.x, a.y, a.z + side.z).color(r, g, bl, alpha).endVertex();
        buffer.pos(b.x + side.x, b.y, b.z + side.z).color(r, g, bl, alpha).endVertex();
        buffer.pos(b.x - side.x, b.y, b.z - side.z).color(r, g, bl, alpha).endVertex();
        tessellator.draw();
    }

    /** A flat ring lying on the floor, broken into turning dashes. */
    private static void ring(Vec3d c, double radius, double thickness, float spin, float r, float g, float bl, float alpha) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        int segments = 72;
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < segments; i++) {
            if (i % 9 == 8) {
                continue;
            }
            double a0 = Math.PI * 2D * i / segments + spin;
            double a1 = Math.PI * 2D * (i + 1) / segments + spin;
            Vec3d i0 = c.add(polar(a0, radius - thickness));
            Vec3d o0 = c.add(polar(a0, radius + thickness));
            Vec3d o1 = c.add(polar(a1, radius + thickness));
            Vec3d i1 = c.add(polar(a1, radius - thickness));
            buffer.pos(i0.x, i0.y, i0.z).color(r, g, bl, alpha).endVertex();
            buffer.pos(o0.x, o0.y, o0.z).color(r, g, bl, alpha).endVertex();
            buffer.pos(o1.x, o1.y, o1.z).color(r, g, bl, alpha).endVertex();
            buffer.pos(i1.x, i1.y, i1.z).color(r, g, bl, alpha).endVertex();
        }
        tessellator.draw();
    }

    /** A soft pool of light lying on the floor. */
    private static void disc(Vec3d c, double radius, float r, float g, float bl, float alpha) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(c.x, c.y, c.z).color(r, g, bl, alpha).endVertex();
        for (int i = 0; i <= 32; i++) {
            double a = Math.PI * 2D * i / 32D;
            buffer.pos(c.x + Math.cos(a) * radius, c.y, c.z + Math.sin(a) * radius).color(r, g, bl, 0F).endVertex();
        }
        tessellator.draw();
    }

    /** A soft glow facing the camera. */
    private static void halo(Vec3d c, double radius, float r, float g, float bl, float alpha) {
        Vec3d forward = c.lengthSquared() < 1.0E-8 ? new Vec3d(0, 0, 1) : c.normalize();
        Vec3d right = forward.crossProduct(UP);
        right = right.lengthSquared() < 1.0E-6 ? new Vec3d(1, 0, 0) : right.normalize();
        Vec3d up = right.crossProduct(forward).normalize();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(c.x, c.y, c.z).color(r, g, bl, alpha).endVertex();
        for (int i = 0; i <= 24; i++) {
            double a = Math.PI * 2D * i / 24D;
            Vec3d edge = c.add(right.scale(Math.cos(a) * radius)).add(up.scale(Math.sin(a) * radius));
            buffer.pos(edge.x, edge.y, edge.z).color(r, g, bl, 0F).endVertex();
        }
        tessellator.draw();
    }

    /** Camera-facing strip between two camera-relative points, bright down its middle. */
    private static void ribbon(Vec3d a, Vec3d b, double width, float r, float g, float bl, float alpha) {
        Vec3d side = b.subtract(a).crossProduct(a.add(b).scale(0.5D));
        side = side.lengthSquared() < 1.0E-10 ? UP.scale(width) : side.normalize().scale(width);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        for (int half = -1; half <= 1; half += 2) {
            buffer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
            buffer.pos(a.x + side.x * half, a.y + side.y * half, a.z + side.z * half).color(r, g, bl, 0F).endVertex();
            buffer.pos(a.x, a.y, a.z).color(r, g, bl, alpha).endVertex();
            buffer.pos(b.x + side.x * half, b.y + side.y * half, b.z + side.z * half).color(r, g, bl, 0F).endVertex();
            buffer.pos(b.x, b.y, b.z).color(r, g, bl, alpha).endVertex();
            tessellator.draw();
        }
    }
}
