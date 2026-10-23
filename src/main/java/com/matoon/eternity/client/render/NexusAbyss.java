package com.matoon.eternity.client.render;

import com.matoon.eternity.world.Nexus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * The abyss beneath the Nexus of All Realities, spanning its whole floor: a dark that runs
 * out to the horizon, a crimson haze drifting over it, eyes opening in it here and there,
 * and hands beyond counting reaching up out of it, gaunt and long-fingered, slowly
 * clutching at nothing. Come down toward them and the nearest strain up at you, stretching
 * and grasping faster, as if they could reach. Touch the surface and they can: it is
 * quicksand, and five of them rise around you, close on you and drag you under.
 *
 * Each hand stands in a fixed place, from a hash of its little patch of the floor, so they
 * are the same for everyone. Near hands are drawn in full, far ones more simply. Nothing
 * here flashes: they move like things underwater.
 */
@SideOnly(Side.CLIENT)
final class NexusAbyss {

    /** The surface of the abyss: quicksand, which whatever touches it sinks through. */
    private static final double FLOOR = Nexus.ABYSS_Y;
    private static final int PATCH = 9;
    private static final double RANGE = 112D;
    private static final double DETAIL = 56D;
    /** How far off a hand can sense you: across the floor, and up from it. */
    private static final double SENSE = 44D, SENSE_UP = 72D;
    private static final double[] UP = {0D, 1D, 0D};
    private static final double[] ACROSS = {1D, 0D, 0D};
    private static final double[] LIGHT = normalise(new double[]{0.3D, 0.8D, 0.5D});
    /** How a finger's length is shared between its three joints. */
    private static final double[] JOINTS = {0.45D, 0.33D, 0.22D};
    /** Where the grabbing hands take hold, as fractions of a body's height; lower when it is your own. */
    private static final double[] HOLDS = {0.9D, 0.6D, 0.8D, 0.4D, 0.95D};
    private static final double[] SELF_HOLDS = {0.7D, 0.5D, 0.65D, 0.35D, 0.75D};

    private static final float[] DEEP = {0.04F, 0.015F, 0.06F};
    private static final float[] SKIN = {0.3F, 0.21F, 0.31F};

    private NexusAbyss() {
    }

    static void render(Vec3d camera, float time) {
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        floor(camera);
        GlStateManager.shadeModel(GL11.GL_FLAT);

        double[] target = {camera.x, camera.y + 1.2D, camera.z};
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        int x0 = Math.floorDiv(MathHelper.floor(camera.x - RANGE), PATCH), x1 = Math.floorDiv(MathHelper.floor(camera.x + RANGE), PATCH);
        int z0 = Math.floorDiv(MathHelper.floor(camera.z - RANGE), PATCH), z1 = Math.floorDiv(MathHelper.floor(camera.z + RANGE), PATCH);
        for (int px = x0; px <= x1; px++) {
            for (int pz = z0; pz <= z1; pz++) {
                int h = hash(px, pz);
                double bx = px * PATCH + 1D + unit(h, 0) * (PATCH - 2);
                double bz = pz * PATCH + 1D + unit(h, 1) * (PATCH - 2);
                double dx = bx - camera.x, dz = bz - camera.z;
                double dist = dx * dx + dz * dz;
                if (dist > RANGE * RANGE) {
                    continue;
                }
                if ((h & 3) != 0) {
                    arm(buffer, bx, bz, h, target, camera, time, dist < DETAIL * DETAIL);
                } else if (((h >>> 2) & 3) == 0) {
                    eyes(buffer, bx, bz, h, camera, time);
                }
            }
        }
        float pt = Minecraft.getMinecraft().getRenderPartialTicks();
        for (Entity entity : Minecraft.getMinecraft().world.loadedEntityList) {
            if (entity instanceof EntityLivingBase && sinking(entity) && entity.getDistanceSq(camera.x, camera.y, camera.z) < 96D * 96D) {
                grabbers(buffer, (EntityLivingBase) entity, pt, camera, time, entity == Minecraft.getMinecraft().getRenderViewEntity()
                        && Minecraft.getMinecraft().gameSettings.thirdPersonView == 0);
            }
        }
        tessellator.draw();

        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        GlStateManager.depthMask(false);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        mist(camera, time);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.depthMask(true);
        GlStateManager.shadeModel(GL11.GL_FLAT);
    }

    /** The dark itself, out past the edge of sight, lightening only enough to meet the sky. */
    private static void floor(Vec3d camera) {
        double far = Minecraft.getMinecraft().gameSettings.renderDistanceChunks * 16D * 1.5D;
        double y = FLOOR - camera.y;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(0D, y, 0D).color(0.015F, 0F, 0.03F, 1F).endVertex();
        for (int i = 0; i <= 32; i++) {
            double a = Math.PI * 2D * i / 32D;
            buffer.pos(Math.cos(a) * far, y, Math.sin(a) * far).color(0.05F, 0.01F, 0.04F, 1F).endVertex();
        }
        tessellator.draw();
    }

    /** Crimson haze in three thin layers, drifting slowly, thinning out with distance. */
    private static void mist(Vec3d camera, float time) {
        int step = 16, cells = 9;
        int gx = Math.floorDiv(MathHelper.floor(camera.x), step), gz = Math.floorDiv(MathHelper.floor(camera.z), step);
        double reach = step * cells;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        for (int layer = 0; layer < 3; layer++) {
            double y = FLOOR + 0.6D + layer * layer * 2.5D + layer * 0.5D - camera.y;
            float strength = 0.4F / (1 + layer * 1.5F);
            for (int i = -cells; i < cells; i++) {
                for (int j = -cells; j < cells; j++) {
                    double ax = (gx + i) * step, az = (gz + j) * step;
                    haze(buffer, ax, az, y, layer, strength, reach, camera, time);
                    haze(buffer, ax + step, az, y, layer, strength, reach, camera, time);
                    haze(buffer, ax + step, az + step, y, layer, strength, reach, camera, time);
                    haze(buffer, ax, az + step, y, layer, strength, reach, camera, time);
                }
            }
        }
        tessellator.draw();
    }

    private static void haze(BufferBuilder buffer, double x, double z, double y, int layer, float strength, double reach, Vec3d camera, float time) {
        double dx = x - camera.x, dz = z - camera.z;
        double fade = Math.max(0D, 1D - Math.sqrt(dx * dx + dz * dz) / reach);
        double drift = Math.sin(x * 0.045D + time * 0.004D + layer * 2D) * Math.cos(z * 0.05D - time * 0.003D + layer) * 0.5D + 0.5D;
        float alpha = (float) (strength * (0.35D + 0.65D * drift) * fade);
        buffer.pos(dx, y, dz).color(0.5F, 0.04F, 0.22F, alpha).endVertex();
    }

    /** A pair of eyes opening in the dark, red, unblinking for long stretches. */
    private static void eyes(BufferBuilder buffer, double x, double z, int h, Vec3d camera, float time) {
        double phase = unit(h, 2) * 50D;
        double open = MathHelper.clamp((Math.sin(time * 0.006D + phase) + 0.2D) * 2D, 0D, 1D);
        if (open <= 0D) {
            return;
        }
        double yaw = unit(h, 3) * Math.PI;
        double ux = Math.cos(yaw), uz = Math.sin(yaw);
        double y = FLOOR + 0.3D - camera.y;
        for (int side = -1; side <= 1; side += 2) {
            double cx = x + ux * side * 0.8D - camera.x, cz = z + uz * side * 0.8D - camera.z;
            double w = 0.55D, t = 0.22D * open;
            // A narrow diamond, long across, thin when nearly shut.
            buffer.pos(cx - ux * w, y, cz - uz * w).color(0.9F, 0.08F, 0.05F, 1F).endVertex();
            buffer.pos(cx - uz * t, y, cz + ux * t).color(1F, 0.3F, 0.15F, 1F).endVertex();
            buffer.pos(cx + ux * w, y, cz + uz * w).color(0.9F, 0.08F, 0.05F, 1F).endVertex();
            buffer.pos(cx + uz * t, y, cz - ux * t).color(1F, 0.3F, 0.15F, 1F).endVertex();
        }
    }

    // ------------------------------------------------------------------ the hands

    /** An arm rising out of the dark, swaying, and if you are near, straining up toward you. */
    private static void arm(BufferBuilder buffer, double bx, double bz, int h, double[] target, Vec3d camera, float time, boolean near) {
        double height = 7D + unit(h, 2) * 13D;
        double phase = unit(h, 3) * 10D;
        double thick = 0.45D + unit(h, 4) * 0.45D;
        double leanAngle = unit(h, 5) * Math.PI * 2D, lean = 0.15D + unit(h, 6) * 0.35D;
        double by = FLOOR - 2D;

        double tx = target[0] - bx, ty = target[1] - by, tz = target[2] - bz;
        double want = MathHelper.clamp(1D - Math.sqrt(tx * tx + tz * tz) / SENSE, 0D, 1D)
                * MathHelper.clamp(1D - (target[1] - FLOOR) / SENSE_UP, 0D, 1D);
        want = want * want * (3D - 2D * want);
        double tl = Math.max(1.0E-3D, Math.sqrt(tx * tx + ty * ty + tz * tz));
        tx /= tl;
        ty /= tl;
        tz /= tl;

        int segments = near ? 7 : 3;
        // Stretching toward you, but stopping short: the ones that have you are drawn by {@link #grabbers}.
        double length = Math.min(height * (1D + 0.8D * want) + 2D, Math.max(height * 0.6D + 2D, tl - 1.5D));
        double[][] spine = new double[segments + 1][];
        double[] radius = new double[segments + 1];
        spine[0] = new double[]{bx, by, bz};
        double lx = Math.cos(leanAngle) * lean, lz = Math.sin(leanAngle) * lean;
        double sway = time * (0.018D + 0.03D * want) + phase;
        for (int i = 0; i < segments; i++) {
            double s = (i + 0.5D) / segments;
            // Drifting as if underwater; trembling with the effort when it reaches for you.
            double wx = lx * s + Math.sin(sway + s * 2.2D) * 0.22D * s + Math.sin(time * 0.3D + i * 1.7D + phase) * 0.08D * want;
            double wz = lz * s + Math.cos(sway * 1.2D + s * 1.7D) * 0.22D * s + Math.cos(time * 0.27D + i * 1.3D + phase) * 0.08D * want;
            double reach = want * Math.min(1D, s * 1.6D);
            double[] d = normalise(new double[]{wx * (1D - reach) + tx * reach, (1D - reach) + ty * reach, wz * (1D - reach) + tz * reach});
            double[] p = spine[i];
            spine[i + 1] = new double[]{p[0] + d[0] * length / segments, p[1] + d[1] * length / segments, p[2] + d[2] * length / segments};
        }
        for (int i = 0; i <= segments; i++) {
            radius[i] = thick * (1.25D - 0.5D * i / segments);
        }
        double[] overall = normalise(sub(spine[segments], spine[0]));
        tube(buffer, spine, radius, radius, near ? 6 : 4, Math.abs(overall[1]) > 0.7D ? ACROSS : UP, camera, 0D, 0.8D);

        double[] dir = normalise(sub(spine[segments], spine[segments - 1]));
        double[] ref = Math.abs(dir[1]) > 0.95D ? ACROSS : UP;

        // Clutching slowly at nothing, or opening and closing on you.
        double grip = 0.5D + (0.3D + 0.2D * want) * Math.sin(time * (0.025D + 0.07D * want) + phase * 3D);
        hand(buffer, spine[segments], dir, ref, radius[segments], grip, camera, near);
    }

    /** Whether the abyss has hold of something: it has touched the surface and not yet gone under. */
    private static boolean sinking(Entity entity) {
        return entity.posY < FLOOR + 0.05D && entity.posY > FLOOR - entity.height - 1.5D
                && !(entity instanceof EntityPlayer && ((EntityPlayer) entity).isSpectator());
    }

    /**
     * The hands that have hold of something sinking: five of them rising out of the dark
     * around it, arching over and closing on it, clenching, going down with it. Seen from
     * inside, they grip lower, so they close on your body rather than across your eyes.
     */
    private static void grabbers(BufferBuilder buffer, EntityLivingBase entity, float pt, Vec3d camera, float time, boolean self) {
        double ex = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * pt;
        double ey = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * pt;
        double ez = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * pt;
        double w = Math.max(0.3D, entity.width * 0.5D);
        double[] heights = self ? SELF_HOLDS : HOLDS;
        int n = 6;
        for (int i = 0; i < heights.length; i++) {
            double a = Math.PI * 2D * i / heights.length + entity.getEntityId() * 0.7D + Math.sin(time * 0.01D + i) * 0.1D;
            double ca = Math.cos(a), sa = Math.sin(a);
            double out = w + 1.4D + 0.4D * (i % 3);
            double[] base = {ex + ca * out, FLOOR - 1.5D, ez + sa * out};
            // The wrist just outside the body, so the fingers close over it.
            double[] hold = {ex + ca * (w + 0.55D), ey + entity.height * heights[i], ez + sa * (w + 0.55D)};
            double[] lift = {base[0] + (hold[0] - base[0]) * 0.3D, Math.max(base[1], hold[1]) + 1D + 0.3D * Math.sin(time * 0.05D + i),
                    base[2] + (hold[2] - base[2]) * 0.3D};
            double[][] spine = new double[n + 1][];
            double[] radius = new double[n + 1];
            for (int k = 0; k <= n; k++) {
                double t = k / (double) n, u = 1D - t;
                spine[k] = new double[]{u * u * base[0] + 2D * u * t * lift[0] + t * t * hold[0],
                        u * u * base[1] + 2D * u * t * lift[1] + t * t * hold[1], u * u * base[2] + 2D * u * t * lift[2] + t * t * hold[2]};
                radius[k] = 0.34D * (1.25D - 0.5D * t);
            }
            double[] overall = normalise(sub(hold, base));
            tube(buffer, spine, radius, radius, 6, Math.abs(overall[1]) > 0.7D ? ACROSS : UP, camera, 0D, 0.8D);
            double[] dir = normalise(sub(spine[n], spine[n - 1]));
            double grip = 0.8D + 0.1D * Math.sin(time * 0.12D + i * 1.3D);
            hand(buffer, spine[n], dir, Math.abs(dir[1]) > 0.95D ? ACROSS : UP, radius[n], grip, camera, true);
        }
    }

    /** A palm and long gaunt fingers, curled by {@code grip} from splayed open (0) to a fist (1). */
    private static void hand(BufferBuilder buffer, double[] wrist, double[] dir, double[] ref, double r, double grip, Vec3d camera, boolean near) {
        double[] u = normalise(cross(dir, ref));
        double[] v = cross(u, dir);
        double palm = r * 2.2D;
        double[] knuckles = add(wrist, dir, palm);
        tube(buffer, new double[][]{wrist, knuckles}, new double[]{r * 1.2D, r * 1.6D}, new double[]{r * 0.8D, r * 0.5D}, near ? 6 : 4, ref, camera, 0.8D, 0.9D);

        double[] lengths = {0.9D, 1D, 0.95D, 0.75D};
        int joints = near ? 3 : 1;
        for (int k = 0; k < 4; k++) {
            double[] base = add(knuckles, u, (k - 1.5D) * r * 0.95D);
            double splay = (k - 1.5D) * 0.2D * (1D - grip);
            double total = r * 3.2D * lengths[k];
            double[][] finger = new double[joints + 1][];
            double[] radii = new double[joints + 1];
            finger[0] = base;
            radii[0] = r * 0.3D;
            double bend = 0D;
            for (int j = 0; j < joints; j++) {
                bend += grip * (j == 0 ? 0.6D : 1D) * (near ? 1D : 1.6D);
                double[] d = normalise(new double[]{
                        dir[0] * Math.cos(bend) + v[0] * Math.sin(bend) + u[0] * splay,
                        dir[1] * Math.cos(bend) + v[1] * Math.sin(bend) + u[1] * splay,
                        dir[2] * Math.cos(bend) + v[2] * Math.sin(bend) + u[2] * splay});
                finger[j + 1] = add(finger[j], d, total * (near ? JOINTS[j] : 1D));
                // Tapering to a point, like a claw.
                radii[j + 1] = j == joints - 1 ? 0.02D : r * (0.26D - 0.06D * j);
            }
            tube(buffer, finger, radii, radii, near ? 4 : 3, ref, camera, 0.9D, 1D);
        }
        if (near) {
            double[] base = add(add(wrist, dir, palm * 0.35D), u, -r * 1.3D);
            double[] reach = normalise(new double[]{dir[0] * 0.55D - u[0] * 0.6D + v[0] * 0.35D,
                    dir[1] * 0.55D - u[1] * 0.6D + v[1] * 0.35D, dir[2] * 0.55D - u[2] * 0.6D + v[2] * 0.35D});
            double[] mid = add(base, reach, r * 1.3D);
            double[] across = normalise(new double[]{reach[0] + (v[0] + u[0] * 0.7D) * grip,
                    reach[1] + (v[1] + u[1] * 0.7D) * grip, reach[2] + (v[2] + u[2] * 0.7D) * grip});
            double[] tip = add(mid, across, r * 1.1D);
            tube(buffer, new double[][]{base, mid, tip}, new double[]{r * 0.36D, r * 0.28D, 0.02D}, new double[]{r * 0.36D, r * 0.28D, 0.02D},
                    4, ref, camera, 0.9D, 1D);
        }
    }

    /**
     * A tube along {@code points}, its cross-section an ellipse of radii {@code ru} and {@code rv}.
     * Its colour runs from deep at the bottom of the arm to pale skin at the fingertips; {@code f0}
     * and {@code f1} are how far along the whole arm this piece begins and ends.
     */
    private static void tube(BufferBuilder buffer, double[][] points, double[] ru, double[] rv, int sides, double[] ref, Vec3d camera, double f0, double f1) {
        int n = points.length;
        double[][] previous = null;
        float[] previousColour = null;
        for (int i = 0; i < n; i++) {
            double[] tangent = normalise(sub(points[Math.min(n - 1, i + 1)], points[Math.max(0, i - 1)]));
            double[] side = cross(tangent, ref);
            if (side[0] * side[0] + side[1] * side[1] + side[2] * side[2] < 0.04D) {
                // Running nearly along the reference: any other axis will do here.
                side = cross(tangent, ref == UP ? ACROSS : UP);
            }
            double[] u = normalise(side);
            double[] v = cross(tangent, u);
            double[][] ring = new double[sides + 1][];
            for (int j = 0; j <= sides; j++) {
                double a = Math.PI * 2D * j / sides;
                double cu = Math.cos(a) * ru[i], cv = Math.sin(a) * rv[i];
                ring[j] = new double[]{points[i][0] + u[0] * cu + v[0] * cv - camera.x,
                        points[i][1] + u[1] * cu + v[1] * cv - camera.y, points[i][2] + u[2] * cu + v[2] * cv - camera.z};
            }
            float f = (float) (f0 + (f1 - f0) * i / Math.max(1, n - 1));
            float[] colour = {DEEP[0] + (SKIN[0] - DEEP[0]) * f, DEEP[1] + (SKIN[1] - DEEP[1]) * f, DEEP[2] + (SKIN[2] - DEEP[2]) * f};
            if (previous != null) {
                for (int j = 0; j < sides; j++) {
                    double a = Math.PI * 2D * (j + 0.5D) / sides;
                    double light = (u[0] * Math.cos(a) + v[0] * Math.sin(a)) * LIGHT[0] + (u[1] * Math.cos(a) + v[1] * Math.sin(a)) * LIGHT[1]
                            + (u[2] * Math.cos(a) + v[2] * Math.sin(a)) * LIGHT[2];
                    float shade = 0.55F + 0.45F * (float) Math.max(0D, light);
                    put(buffer, previous[j], previousColour, shade);
                    put(buffer, previous[j + 1], previousColour, shade);
                    put(buffer, ring[j + 1], colour, shade);
                    put(buffer, ring[j], colour, shade);
                }
            }
            previous = ring;
            previousColour = colour;
        }
    }

    private static void put(BufferBuilder buffer, double[] p, float[] c, float shade) {
        buffer.pos(p[0], p[1], p[2]).color(c[0] * shade, c[1] * shade, c[2] * shade, 1F).endVertex();
    }

    // ------------------------------------------------------------------ helpers

    private static int hash(int x, int z) {
        int h = x * 73856093 ^ z * 83492791 ^ 0x2545F491;
        h ^= h >>> 13;
        h *= 0x5bd1e995;
        h ^= h >>> 15;
        h *= 0x27d4eb2d;
        return h ^ (h >>> 16);
    }

    /** A number from 0 to 1, a different one for each {@code salt}, from a patch's hash. */
    private static double unit(int h, int salt) {
        int k = h * (0x9E3779B1 + salt * 0x6A09E667) + salt * 0x3C6EF372;
        k ^= k >>> 15;
        k *= 0x85EBCA6B;
        k ^= k >>> 13;
        return (k >>> 8) / (double) (1 << 24);
    }

    private static double[] sub(double[] a, double[] b) {
        return new double[]{a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    private static double[] add(double[] a, double[] d, double scale) {
        return new double[]{a[0] + d[0] * scale, a[1] + d[1] * scale, a[2] + d[2] * scale};
    }

    private static double[] cross(double[] a, double[] b) {
        return new double[]{a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    private static double[] normalise(double[] a) {
        double len = Math.sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2]);
        return len < 1.0E-9D ? new double[]{0D, 1D, 0D} : new double[]{a[0] / len, a[1] / len, a[2] / len};
    }
}
