package com.matoon.eternity.client;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The sky breaking like glass. A long ragged band of the dome shatters into shards
 * that peel away and drift off, the void showing through every hole, while cracks
 * race out across the unbroken sky around it.
 *
 * Everything lives on the sky dome, in angular coordinates around the fracture's
 * centre: u runs along the band, v across it, both in radians.
 */
@SideOnly(Side.CLIENT)
public class SkyFracture {

    /** Ticks for the band to break open fully. */
    public static final int GROW_TICKS = 1400;

    public final int id;
    /** Unit vectors: centre of the band, along it, and across it. */
    public final Vec3d centre, along, across;
    /** Angular half-length of the band, and its half-width at the middle. */
    public final float halfLength, halfWidth;

    public final List<Shard> shards = new ArrayList<>();
    public final List<float[]> cracks = new ArrayList<>();
    public final List<Float> crackDelay = new ArrayList<>();
    public final List<float[]> sparks = new ArrayList<>();

    public int age;

    /** One pane of sky: a jagged quad that breaks at {@code breakAt} (0-1 of growth). */
    public static final class Shard {
        public final float[] u = new float[4];
        public final float[] v = new float[4];
        public final float cu, cv;
        public final float breakAt;
        /** Drift once broken: direction across the band, spin, and a little along it. */
        public final float push, spin, slide;

        Shard(float[] us, float[] vs, float breakAt, float push, float spin, float slide) {
            System.arraycopy(us, 0, u, 0, 4);
            System.arraycopy(vs, 0, v, 0, 4);
            this.cu = (us[0] + us[1] + us[2] + us[3]) / 4F;
            this.cv = (vs[0] + vs[1] + vs[2] + vs[3]) / 4F;
            this.breakAt = breakAt;
            this.push = push;
            this.spin = spin;
            this.slide = slide;
        }
    }

    /**
     * @param direction unit vector toward the middle of the band
     * @param lengthDeg how much of the sky the band spans, in degrees
     */
    public SkyFracture(int id, Vec3d direction, float lengthDeg, long seed) {
        this.id = id;
        Random rand = new Random(seed);
        this.centre = direction.normalize();
        // Pick a band direction tilted off the horizon, like a great diagonal wound.
        Vec3d up = new Vec3d(0, 1, 0);
        Vec3d side = centre.crossProduct(up);
        if (side.lengthSquared() < 1.0E-4) {
            side = new Vec3d(1, 0, 0);
        }
        side = side.normalize();
        Vec3d tangentUp = side.crossProduct(centre).normalize();
        double tilt = (rand.nextDouble() - 0.5D) * 1.4D;
        this.along = side.scale(Math.cos(tilt)).add(tangentUp.scale(Math.sin(tilt))).normalize();
        this.across = along.crossProduct(centre).normalize();

        this.halfLength = (float) Math.toRadians(lengthDeg) / 2F;
        this.halfWidth = halfLength * (0.16F + rand.nextFloat() * 0.1F);
        buildShards(rand, halfLength, halfWidth);
        buildCracks(rand, halfLength, halfWidth);
        for (int i = 0; i < 70; i++) {
            float u = (rand.nextFloat() * 2F - 1F) * halfLength * 0.9F;
            float v = (rand.nextFloat() * 2F - 1F) * envelope(u, halfLength, halfWidth) * 0.8F;
            sparks.add(new float[]{u, v, rand.nextFloat(), 0.5F + rand.nextFloat()});
        }
    }

    /** The band's half-width at a point along it: ragged, fat in the middle, thin at the ends. */
    private static float envelope(float u, float halfLength, float halfWidth) {
        float t = Math.abs(u) / halfLength;
        return halfWidth * (float) Math.pow(Math.max(0F, 1F - t * t), 0.6D);
    }

    /** A jittered grid of shared corners, so shards tile the band with no gaps. */
    private void buildShards(Random rand, float halfLength, float halfWidth) {
        int cols = 26;
        int rows = 8;
        float du = halfLength * 2F / cols;
        float dv = halfWidth * 2F / rows;
        float[][] cu = new float[cols + 1][rows + 1];
        float[][] cv = new float[cols + 1][rows + 1];
        for (int i = 0; i <= cols; i++) {
            for (int j = 0; j <= rows; j++) {
                float edge = (i == 0 || i == cols || j == 0 || j == rows) ? 0.15F : 0.45F;
                cu[i][j] = -halfLength + i * du + (rand.nextFloat() - 0.5F) * du * edge * 2F;
                cv[i][j] = -halfWidth + j * dv + (rand.nextFloat() - 0.5F) * dv * edge * 2F;
            }
        }
        for (int i = 0; i < cols; i++) {
            for (int j = 0; j < rows; j++) {
                float[] us = {cu[i][j], cu[i + 1][j], cu[i + 1][j + 1], cu[i][j + 1]};
                float[] vs = {cv[i][j], cv[i + 1][j], cv[i + 1][j + 1], cv[i][j + 1]};
                float mu = (us[0] + us[2]) / 2F, mv = (vs[0] + vs[2]) / 2F;
                float width = envelope(mu, halfLength, halfWidth) * (0.75F + rand.nextFloat() * 0.5F);
                if (Math.abs(mv) > width) {
                    continue;
                }
                // The middle of the band goes first; the edges and the ends hold out longer.
                float breakAt = 0.55F * Math.abs(mv) / Math.max(1.0E-4F, width)
                        + 0.25F * Math.abs(mu) / halfLength + 0.2F * rand.nextFloat();
                float push = Math.signum(mv == 0 ? rand.nextFloat() - 0.5F : mv) * (0.4F + rand.nextFloat());
                shards.add(new Shard(us, vs, Math.min(0.95F, breakAt), push,
                        (rand.nextFloat() - 0.5F) * 3F, (rand.nextFloat() - 0.5F) * 0.6F));
            }
        }
    }

    /** Lightning-like cracks spreading from the band's edges across the unbroken sky. */
    private void buildCracks(Random rand, float halfLength, float halfWidth) {
        int count = 14 + rand.nextInt(8);
        for (int c = 0; c < count; c++) {
            float u = (rand.nextFloat() * 2F - 1F) * halfLength * 0.85F;
            float side = rand.nextBoolean() ? 1F : -1F;
            float v = side * envelope(u, halfLength, halfWidth) * 0.9F;
            float angle = (float) Math.atan2(side * (0.6F + rand.nextFloat()), (rand.nextFloat() - 0.5F) * 1.6F);
            float delay = 0.15F + 0.5F * rand.nextFloat();
            grow(rand, u, v, angle, 8 + rand.nextInt(8), halfWidth * (0.12F + rand.nextFloat() * 0.12F), delay, 0);
        }
    }

    private void grow(Random rand, float u, float v, float angle, int steps, float step, float delay, int depth) {
        float[] line = new float[(steps + 1) * 2];
        line[0] = u;
        line[1] = v;
        for (int s = 1; s <= steps; s++) {
            angle += (rand.nextFloat() - 0.5F) * 1.1F;
            u += MathHelper.cos(angle) * step;
            v += MathHelper.sin(angle) * step;
            line[s * 2] = u;
            line[s * 2 + 1] = v;
            if (depth < 2 && s > 1 && rand.nextFloat() < 0.25F) {
                grow(rand, u, v, angle + (rand.nextBoolean() ? 0.7F : -0.7F), 3 + rand.nextInt(5), step * 0.75F,
                        delay + s * 0.03F, depth + 1);
            }
        }
        cracks.add(line);
        crackDelay.add(Math.min(0.9F, delay));
    }

    /** The band's half-width at a point along it. */
    public float envelope(float u) {
        return envelope(u, halfLength, halfWidth);
    }

    public void tick() {
        age++;
    }

    /** 0 to 1 as the band breaks open. */
    public float growth(float partialTicks) {
        float t = MathHelper.clamp((age + partialTicks) / GROW_TICKS, 0F, 1F);
        return 1F - (1F - t) * (1F - t);
    }

    /** Unit direction on the dome for angular coordinates (u, v), widened by {@code spread}. */
    public Vec3d direction(double u, double v, double spread) {
        double su = u * spread, sv = v * spread;
        double cosV = Math.cos(sv);
        return centre.scale(Math.cos(su) * cosV).add(along.scale(Math.sin(su) * cosV)).add(across.scale(Math.sin(sv)));
    }
}
