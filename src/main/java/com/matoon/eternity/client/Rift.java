package com.matoon.eternity.client;

import com.matoon.eternity.client.sound.LoopSound;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A crack in the universe, as the client sees it. A long jagged split, hairline at
 * its ends and gaping at its heart where he passes through, with fractures racing
 * out across the sky. All coordinates are in units of half its height.
 */
@SideOnly(Side.CLIENT)
public class Rift {

    public static final int OPEN_TICKS = 50;
    public static final int CLOSE_TICKS = 30;
    public static final int SEGMENTS = 48;

    public final int id;
    public final double x, y, z;
    public final float height;
    public final boolean finale;

    /** Per segment along the split: half-width and sideways offset. */
    public final float[] half = new float[SEGMENTS + 1];
    public final float[] centre = new float[SEGMENTS + 1];
    /** Fractures: polylines of (u, v) pairs, plus how late in the opening each starts to grow. */
    public final List<float[]> fractures = new ArrayList<>();
    public final List<Float> fractureDelay = new ArrayList<>();

    public int age;
    public int closeAge = -1;
    /** Ticks after which it closes by itself; -1 waits for the server. */
    public int lifetime = -1;
    public LoopSound loop;

    public Rift(int id, double x, double y, double z, float height, long seed, boolean finale) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.z = z;
        this.height = height;
        this.finale = finale;
        Random rand = new Random(seed);
        float tilt = (rand.nextFloat() - 0.5F) * 0.5F;
        float offset = 0F;
        for (int i = 0; i <= SEGMENTS; i++) {
            float v = -1F + 2F * i / SEGMENTS;
            float envelope = (float) Math.sqrt(Math.max(0F, 1F - v * v));
            offset = offset * 0.55F + (rand.nextFloat() - 0.5F) * 0.07F;
            centre[i] = offset * envelope + tilt * v;
            float mouth = 0.055F * (float) Math.exp(-(v * v) / (0.22F * 0.22F));
            half[i] = envelope * (0.006F + rand.nextFloat() * 0.012F) + mouth;
        }
        int count = 18 + rand.nextInt(12);
        for (int c = 0; c < count; c++) {
            int i = 3 + rand.nextInt(SEGMENTS - 5);
            float side = rand.nextBoolean() ? 1F : -1F;
            float v = -1F + 2F * i / SEGMENTS;
            float u = centre[i] + side * half[i];
            float angle = (float) Math.atan2((rand.nextFloat() - 0.5F) * 1.6F, side);
            float delay = Math.abs(v) * 0.5F + rand.nextFloat() * 0.3F;
            grow(rand, u, v, angle, 6 + rand.nextInt(8), 0.03F + rand.nextFloat() * 0.05F, delay, 0);
        }
    }

    private void grow(Random rand, float u, float v, float angle, int steps, float step, float delay, int depth) {
        float[] line = new float[(steps + 1) * 2];
        line[0] = u;
        line[1] = v;
        for (int s = 1; s <= steps; s++) {
            angle += (rand.nextFloat() - 0.5F) * 1F;
            u += MathHelper.cos(angle) * step;
            v += MathHelper.sin(angle) * step;
            line[s * 2] = u;
            line[s * 2 + 1] = v;
            if (depth == 0 && s > 1 && rand.nextFloat() < 0.22F) {
                grow(rand, u, v, angle + (rand.nextBoolean() ? 0.8F : -0.8F), 3 + rand.nextInt(4), step * 0.7F,
                        delay + s * 0.05F, depth + 1);
            }
        }
        fractures.add(line);
        fractureDelay.add(Math.min(0.8F, delay));
    }

    public void close() {
        if (closeAge < 0) {
            closeAge = 0;
        }
    }

    public void tick() {
        age++;
        if (closeAge >= 0) {
            closeAge++;
        } else if (lifetime > 0 && age >= lifetime) {
            close();
        }
    }

    public boolean isDead() {
        return closeAge > CLOSE_TICKS;
    }

    /** Big rifts are seen through everything; small ones sit in the world. */
    public boolean seenThroughWorld() {
        return height > 40F;
    }

    public float openness(float partialTicks) {
        float open = smooth(MathHelper.clamp((age + partialTicks) / OPEN_TICKS, 0F, 1F));
        if (closeAge >= 0) {
            open *= 1F - smooth(MathHelper.clamp((closeAge + partialTicks) / CLOSE_TICKS, 0F, 1F));
        }
        return open;
    }

    private static float smooth(float t) {
        return t * t * (3F - 2F * t);
    }
}
