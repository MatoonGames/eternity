package com.matoon.eternity.stones.client;

import java.util.Random;

/**
 * Paints the burns snapping leaves onto the 64 by 64 skin layout, as three layers: the burn itself, its glowing cracks,
 * and the white heat of the flare. Each arm is painted to its own burn (see {@code SnapScars}), deepest at the hand:
 * <ul>
 * <li>Slight: raw red scarring round the hand, which heals back to nothing.</li>
 * <li>Deeper: black char climbing the arm to a ragged scorched front, flecked with ash, split by fissures grown up from
 * the fingers, white-hot at the hand and cooling to orange as they climb.</li>
 * <li>Deeper still: the char past the shoulder onto the chest and back, then the neck and that side of the face.</li>
 * <li>Fused: the gauntlet's gold run into the hand and forearm, in plates sunk into the char, seamed with molten
 * light.</li>
 * </ul>
 * The arm is painted as the cylinder it is, unrolled into a band round it, so everything runs round it without seams.
 */
final class BurnPainter {

    private static final int SIZE = 64;
    private static final int CHAR = 0, GLOW = 1, HEAT = 2;
    /** A burn this deep or more has the gauntlet's metal in it (SnapScars.FUSED). */
    private static final float FUSED = 7F;
    private static final int[] GOLD = {0x3a2507, 0x5a3c0d, 0x82591a, 0xa97d28, 0xcda243, 0xe8c670, 0xfbe8b4};

    private final long seed;
    private final Random random;
    private final int[][] layers = new int[3][SIZE * SIZE];

    /** The arm being painted: how far up it the burn reaches, and how much of it is char rather than raw scar. */
    private float reach, charred;

    private BurnPainter(long seed) {
        this.seed = seed;
        random = new Random(seed);
    }

    /** The burn, glow and heat layers, in ARGB, for these burns on the right and left arms, wide or slim. */
    static int[][] paint(long seed, float right, float left, boolean slim) {
        BurnPainter painter = new BurnPainter(seed);
        int width = slim ? 3 : 4;
        // Each arm's box on the skin: the right arm's at 40,16, the left's at 32,48.
        if (right > 0.05F) {
            painter.arm(40, 16, width, right);
            painter.body(true, right);
        }
        if (left > 0.05F) {
            painter.arm(32, 48, width, left);
            painter.body(false, left);
        }
        return painter.layers;
    }

    /** How grave a burn is, 0 to 1: one snap a little over a tenth, the metal fused in near the top. */
    static float severity(float burn) {
        return Math.max(0F, Math.min(1F, burn / 8F));
    }

    // ------------------------------------------------------------------ the arm

    private void arm(int u, int v, int width, float burn) {
        float s = severity(burn);
        reach = lerp(-0.62F, 0.4F, s);
        charred = clamp((s - 0.1F) / 0.45F);
        int around = 2 * (4 + width);
        double radius = around / (2D * Math.PI);
        float[][] depth = new float[around][12];
        // The four sides, unrolled in a band round the arm: shoulder at the top, hand at the bottom.
        for (int c = 0; c < around; c++) {
            double angle = (c + 0.5D) / around * 2D * Math.PI;
            double px = Math.cos(angle) * radius, pz = Math.sin(angle) * radius;
            for (int h = 0; h < 12; h++) {
                depth[c][h] = burn(u + c, v + 4 + h, h / 11F, px, h + 0.5D, pz);
            }
        }
        // The top, the shoulder, barely touched; the bottom, the hand, burnt through and, if it is deep, still glowing.
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < 4; j++) {
                double px = (i - width / 2D + 0.5D) * 0.9D, pz = (j - 1.5D) * 0.9D;
                burn(u + 4 + i, v + j, -0.25F, px, -0.5D, pz);
                int x = u + 4 + width + i, y = v + j;
                burn(x, y, 1.15F, px, 12.5D, pz);
                if (charred > 0.5F && hash(x, y, 21) < 0.4F * charred) {
                    glow(x, y, 0.55F + hash(x, y, 22) * 0.45F);
                }
            }
        }
        int veins = s < 0.2F ? 0 : s < 0.45F ? 1 : s < 0.7F ? 2 : 3;
        veins(u, v + 4, around, depth, veins);
        if (burn >= FUSED) {
            metal(u, v, width, around, radius, burn);
        }
    }

    /**
     * One texel, {@code toHand} from 0 at the shoulder to 1 at the hand, at a point on its surface. Past the burn's
     * front it is char, or raw scar if the burn is slight; at the front, scorched red; above it, a few specks of soot.
     * How deep the burn is there.
     */
    private float burn(int x, int y, float toHand, double px, double py, double pz) {
        double ragged = fbm(px * 0.55D, py * 0.42D, pz * 0.55D, 0) - 0.5D;
        float depth = (float) (toHand + reach + ragged * 0.65D);
        int index = y * SIZE + x;
        if (depth < 0.18F) {
            return depth;
        }
        if (depth < 0.3F) {
            if (hash(x, y, 7) < (depth - 0.18F) * 3.5F) {
                layers[CHAR][index] = argb(110 + (int) (hash(x, y, 8) * 50), (int) lerp(120, 34, charred), (int) lerp(60, 26, charred), (int) lerp(52, 24, charred));
            }
            return depth;
        }
        if (depth < 0.4F) {
            // The front: raw, blistered red-brown, giving way to what lies past it.
            float t = (depth - 0.3F) / 0.1F;
            layers[CHAR][index] = argb((int) lerp(170, 245, t), (int) lerp(122, 58 + 60 * (1F - charred), t), (int) lerp(46, 26 + 30 * (1F - charred), t), (int) lerp(30, 20 + 26 * (1F - charred), t));
            layers[HEAT][index] = argb(110, 255, 150, 80);
            return depth;
        }
        double mottle = fbm(px * 1.3D, py * 1.1D, pz * 1.3D, 1);
        float m = (float) (mottle * mottle);
        // Raw scar: red, mottled pink, glossy in places.
        int sr = (int) lerp(118, 176, m), sg = (int) lerp(48, 98, m), sb = (int) lerp(42, 88, m);
        // Char: black and dark ash, mottled, flecked with pale ash.
        int cr = (int) lerp(16, 58, m), cg = (int) lerp(12, 48, m), cb = (int) lerp(11, 44, m);
        float fleck = hash(x, y, 3);
        if (fleck < 0.07F) {
            cr = 108 + (int) (fleck * 300);
            cg = cr - 6;
            cb = cr - 10;
        }
        // The deeper the burn there, the more of it is char.
        float black = clamp(charred * (0.6F + (depth - 0.4F) * 1.2F));
        layers[CHAR][index] = argb(255, (int) lerp(sr, cr, black), (int) lerp(sg, cg, black), (int) lerp(sb, cb, black));
        layers[HEAT][index] = argb(255, 255, 196, 120);
        return depth;
    }

    /**
     * The fissures: a few grown up from the fingers, spread round the arm, each wandering up through the char a texel
     * at a time and now and then putting out a short branch, until the char is too shallow to split.
     */
    private void veins(int u, int top, int around, float[][] depth, int count) {
        float[][] heat = new float[around][12];
        for (int i = 0; i < count; i++) {
            int c = (int) ((i + random.nextFloat() * 0.6F) * around / Math.max(1, count)) % around;
            grow(heat, depth, around, c, 11, random.nextBoolean() ? 1 : -1, 1F, 0);
        }
        for (int c = 0; c < around; c++) {
            for (int h = 0; h < 12; h++) {
                int x = u + c, y = top + h;
                if (heat[c][h] > 0F) {
                    layers[CHAR][y * SIZE + x] = argb(255, 78, 18, 8);
                    glow(x, y, heat[c][h]);
                    continue;
                }
                // The haze round a fissure: its own heat, spread a texel, fading.
                float near = 0F;
                for (int dc = -1; dc <= 1; dc++) {
                    for (int dh = -1; dh <= 1; dh++) {
                        int nh = h + dh;
                        if (nh >= 0 && nh < 12 && (dc == 0 || dh == 0)) {
                            near = Math.max(near, heat[(c + dc + around) % around][nh]);
                        }
                    }
                }
                if (near > 0.45F && depth[c][h] > 0.3F) {
                    layers[GLOW][y * SIZE + x] = argb((int) (15 + 40 * near), 255, 72, 10);
                }
            }
        }
    }

    /** A fissure from {@code c}, {@code h}, climbing, its heat {@code k} cooling as it goes. */
    private void grow(float[][] heat, float[][] depth, int around, int c, int h, int lean, float k, int generation) {
        int length = 0;
        while (h >= 0 && depth[c][h] > 0.48F && k > 0.2F) {
            heat[c][h] = Math.max(heat[c][h], k);
            // Mostly up, now and then a diagonal step round the arm, holding to the way it leans.
            if (random.nextFloat() < 0.3F) {
                if (random.nextFloat() < 0.25F) {
                    lean = -lean;
                }
                c = (c + lean + around) % around;
            }
            if (generation < 1 && length > 1 && random.nextFloat() < 0.18F) {
                grow(heat, depth, around, (c - lean + around) % around, h - 1, -lean, k * 0.7F, generation + 1);
            }
            h--;
            length++;
            k -= generation == 0 ? 0.07F : 0.16F;
        }
    }

    /**
     * The gauntlet's gold, run into the hand and forearm: plates of it sunk into the char, more of it the deeper the
     * burn, each lit along its edge where it meets the flesh, the hand underneath all metal.
     */
    private void metal(int u, int v, int width, int around, double radius, float burn) {
        float spread = clamp((burn - FUSED) / (12F - FUSED));
        boolean[][] gold = new boolean[around][12];
        for (int c = 0; c < around; c++) {
            double angle = (c + 0.5D) / around * 2D * Math.PI;
            double px = Math.cos(angle) * radius, pz = Math.sin(angle) * radius;
            for (int h = 0; h < 12; h++) {
                float toHand = h / 11F;
                double plate = fbm(px * 0.7D, h * 0.5D, pz * 0.7D, 5);
                // The hand is plated through; up the forearm the plates thin out, reaching further the deeper it is.
                gold[c][h] = toHand > 0.92F || plate + toHand * 0.9F - 0.62F + spread * 0.35F > 0.55F && toHand > 0.45F - spread * 0.25F;
            }
        }
        for (int c = 0; c < around; c++) {
            for (int h = 0; h < 12; h++) {
                int x = u + c, y = v + 4 + h, index = y * SIZE + x;
                if (!gold[c][h]) {
                    continue;
                }
                boolean edge = !gold[(c + 1) % around][h] || !gold[(c - 1 + around) % around][h] || h > 0 && !gold[c][h - 1];
                // Gold, dented and tarnished: lit from above, darker where it sank and at its edges.
                float shade = (float) fbm(c * 0.8D, h * 0.8D, 9.1D, 6);
                int tone = Math.max(0, Math.min(GOLD.length - 1, (int) (shade * 5F) + (edge ? 0 : 1) - (h > 9 ? 1 : 0)));
                layers[CHAR][index] = 0xFF000000 | GOLD[tone];
                layers[HEAT][index] = argb(255, 255, 230, 160);
                if (edge) {
                    // Where the metal meets the flesh, it is still molten.
                    glow(x, y, 0.65F + hash(x, y, 31) * 0.3F);
                } else {
                    layers[GLOW][index] = 0;
                }
            }
        }
        // The hand underneath, all metal.
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < 4; j++) {
                int x = u + 4 + width + i, y = v + j;
                int tone = 1 + (int) (hash(x, y, 33) * 3F);
                layers[CHAR][y * SIZE + x] = 0xFF000000 | GOLD[tone];
                if (hash(x, y, 34) < 0.3F) {
                    glow(x, y, 0.7F);
                }
            }
        }
    }

    /** A hot texel: white-yellow at its hottest, cooling through orange to a dull red. */
    private void glow(int x, int y, float k) {
        int g = (int) lerp(60, 225, k * k), b = (int) lerp(8, 150, k * k * k);
        layers[GLOW][y * SIZE + x] = argb((int) (110 + 145 * k), 255, g, b);
    }

    // ------------------------------------------------------------------ the chest, back, neck and face

    /**
     * The burn past the shoulder, the further the graver it is: onto the chest, the side and the back nearest the arm,
     * and at its gravest up the neck and that side of the face.
     */
    private void body(boolean right, float burn) {
        float s = severity(burn);
        float past = lerp(-0.9F, 0.55F, s);
        // Body box at 16,16, 8 wide, 12 tall, 4 deep: top 20..28 x 16..20; sides at y 20..32: right 16..20, front 20..28,
        // left 28..32, back 32..40. The player's right is the texture's left on the front, and its right on the back.
        for (int y = 20; y < 32; y++) {
            int down = y - 20;
            for (int i = 0; i < 4; i++) {
                int x = right ? 16 + i : 28 + i;
                creep(x, y, down * 0.75F + 1.1F + (right ? 3 - i : i) * 0.15F, past);
            }
            for (int i = 0; i < 8; i++) {
                creep(right ? 20 + i : 27 - i, y, down + i * 1.3F, past);
                creep(right ? 39 - i : 32 + i, y, down + i * 1.3F, past);
            }
        }
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 4; j++) {
                creep(right ? 20 + i : 27 - i, 16 + j, i * 1.3F + 0.3F, past);
            }
        }
        if (s < 0.55F) {
            return;
        }
        // Head box at 0,0, 8 by 8 by 8: its sides at y 8..16: right 0..8, front 8..16, left 16..24. Up from the neck.
        float face = lerp(-1.2F, 0.6F, (s - 0.55F) / 0.45F);
        for (int y = 8; y < 16; y++) {
            int up = 15 - y;
            for (int i = 0; i < 8; i++) {
                // The side of the head, from the jaw back.
                creep(right ? i : 16 + i, y, up * 0.9F + (right ? i : 7 - i) * 0.35F + 1F, face);
            }
            for (int i = 0; i < 3; i++) {
                // The cheek and jaw on that side of the face.
                creep(right ? 8 + i : 15 - i, y, up * 1.1F + i * 0.9F + 1.6F, face);
            }
        }
    }

    private void creep(int x, int y, float distance, float past) {
        double ragged = fbm(x * 0.35D, y * 0.35D, 3.7D, 2) - 0.5D;
        float depth = 0.95F - distance * 0.24F + past + (float) ragged * 0.7F;
        int index = y * SIZE + x;
        if (depth > 0.42F) {
            float m = hash(x, y, 5);
            float black = clamp(charred * (0.5F + depth));
            layers[CHAR][index] = argb(240, (int) lerp(lerp(120, 170, m), lerp(18, 50, m), black), (int) lerp(lerp(48, 92, m), lerp(14, 40, m), black),
                    (int) lerp(lerp(42, 84, m), lerp(12, 36, m), black));
            layers[HEAT][index] = argb(170, 255, 170, 100);
            if (charred > 0.6F && hash(x, y, 13) < 0.06F) {
                layers[GLOW][index] = argb(170, 255, 110, 30);
            }
        } else if (depth > 0.3F) {
            layers[CHAR][index] = argb(150, 100, 38, 26);
        } else if (depth > 0.18F && hash(x, y, 9) < 0.35F) {
            layers[CHAR][index] = argb(100, (int) lerp(110, 34, charred), (int) lerp(56, 26, charred), (int) lerp(48, 24, charred));
        }
    }

    // ------------------------------------------------------------------ noise

    private float hash(int x, int y, int salt) {
        long h = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (y * 0xC2B2AE3D27D4EB4FL) ^ (salt * 0x165667B19E3779F9L);
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        return (h >>> 40) / (float) (1L << 24);
    }

    private float lattice(int x, int y, int z, int salt) {
        return hash(x * 73856093 ^ z * 19349663, y, salt);
    }

    /** Smooth value noise in three dimensions, 0 to 1. */
    private double noise(double x, double y, double z, int salt) {
        int x0 = (int) Math.floor(x), y0 = (int) Math.floor(y), z0 = (int) Math.floor(z);
        double fx = smooth(x - x0), fy = smooth(y - y0), fz = smooth(z - z0);
        double c00 = lerp(lattice(x0, y0, z0, salt), lattice(x0 + 1, y0, z0, salt), fx);
        double c10 = lerp(lattice(x0, y0 + 1, z0, salt), lattice(x0 + 1, y0 + 1, z0, salt), fx);
        double c01 = lerp(lattice(x0, y0, z0 + 1, salt), lattice(x0 + 1, y0, z0 + 1, salt), fx);
        double c11 = lerp(lattice(x0, y0 + 1, z0 + 1, salt), lattice(x0 + 1, y0 + 1, z0 + 1, salt), fx);
        return lerp(lerp(c00, c10, fy), lerp(c01, c11, fy), fz);
    }

    private double fbm(double x, double y, double z, int salt) {
        return noise(x, y, z, salt) * 0.57D + noise(x * 2.1D, y * 2.1D, z * 2.1D, salt + 17) * 0.29D
                + noise(x * 4.3D, y * 4.3D, z * 4.3D, salt + 31) * 0.14D;
    }

    private static double smooth(double t) {
        return t * t * (3D - 2D * t);
    }

    private static float clamp(float t) {
        return Math.max(0F, Math.min(1F, t));
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static int argb(int a, int r, int g, int b) {
        return Math.max(0, Math.min(255, a)) << 24 | Math.max(0, Math.min(255, r)) << 16 | Math.max(0, Math.min(255, g)) << 8 | Math.max(0, Math.min(255, b));
    }
}
