package com.matoon.eternity.world;

import com.matoon.eternity.EternityConfig;
import net.minecraft.block.BlockColored;
import net.minecraft.block.BlockStainedGlass;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.DimensionType;
import net.minecraftforge.common.DimensionManager;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The Nexus of All Realities: the place where every universe touches every other.
 * An endless neon void, strewn with whatever has drifted in from everywhere at once:
 * swamp islands floating on nothing, eyeballs the size of houses, orbs bored through,
 * geodesic spheres, looping tubes, crystals, little banded planets, mushrooms on stalks
 * taller than towers, and twisting spires threaded with rings. The world is cut into a
 * three-dimensional grid of cells, five layers deep, so things hang above and below you
 * as well as all around; each cell's contents come from the world seed, so the same
 * Nexus is there every visit. At its heart, a hub island and the vortex that leads out.
 *
 * Everything that moves (eyes that watch you, tentacles, windows onto other realities)
 * is drawn by the client; see {@link com.matoon.eternity.client.NexusClient}.
 */
public final class Nexus {

    /** Where travellers arrive, on the hub island. */
    public static final double HUB_X = 0.5D, HUB_Y = 122D, HUB_Z = 14.5D;
    /** The vortex standing upright on the hub, its foot just above the ground. Walk into it to leave. */
    public static final double VORTEX_X = 0.5D, VORTEX_Y = 127.5D, VORTEX_Z = 0.5D;
    public static final double VORTEX_RADIUS = 7D;

    /**
     * The floor of the Nexus: an abyss under everything, full of hands reaching up. Anything
     * that sinks below it is dragged down and never comes back.
     */
    public static final double ABYSS_Y = 6D;

    /**
     * The point of absolute neutrality, where the powers of Master Order and Lord Chaos meet
     * and cancel. The In-Betweener hangs here in his crystal bubble, over a disc of grey.
     * Travellers stand on the disc at JUNCTION_Y.
     */
    public static final double JUNCTION_X = 512.5D, JUNCTION_Y = 120D, JUNCTION_Z = -511.5D;
    /** The middle of his bubble, and how big it is. */
    public static final double BUBBLE_Y = 127D;
    public static final double BUBBLE_RADIUS = 6D;
    /** Within this far of the junction, everything is grey; beyond NEUTRAL_REACH, nothing is. */
    public static final double NEUTRAL_CORE = 40D, NEUTRAL_REACH = 448D;
    private static final double DISC_RADIUS = 22D;
    /** The epicentre: within this far of the junction, nothing changes, nothing falls, and almost nothing is heard. */
    public static final double EPICENTRE = 48D;

    /**
     * How strongly the junction's neutrality holds at a point, from 0 (not at all) to 1 (its
     * heart). Colour drains out of the world as it rises, and things grow sparse.
     */
    public static float neutrality(double x, double z) {
        double d = Math.sqrt((x - JUNCTION_X) * (x - JUNCTION_X) + (z - JUNCTION_Z) * (z - JUNCTION_Z));
        double t = MathHelper.clamp((NEUTRAL_REACH - d) / (NEUTRAL_REACH - NEUTRAL_CORE), 0D, 1D);
        return (float) (t * t * (3D - 2D * t));
    }

    /** Whether a point lies within the epicentre of absolute neutrality. */
    public static boolean inEpicentre(double x, double y, double z) {
        double dx = x - JUNCTION_X, dz = z - JUNCTION_Z;
        return dx * dx + dz * dz < EPICENTRE * EPICENTRE && Math.abs(y - JUNCTION_Y) < 64D;
    }

    /** How far the abyss has taken something, from 0 at its surface to 1 once it has gone under. */
    public static float sunk(Entity entity) {
        return (float) MathHelper.clamp((ABYSS_Y - entity.posY) / (entity.height + 0.6D), 0D, 1D);
    }

    /** How fast it drags things down, in blocks a tick: slowly at first, faster toward the end, about seven seconds in all. */
    public static double sinkSpeed(float sunk) {
        return 0.01D + 0.014D * sunk;
    }

    private static final int CELL = 56;
    private static final int LAYER = 44;
    private static final int LAYERS = 5;
    private static final int BASE_Y = 22;
    /** How far past its own cell anything may reach; islands with their lobes, rocks and mushrooms stay inside this. */
    private static final int REACH = 40;

    private static DimensionType type;

    private Nexus() {
    }

    public static void register() {
        int id = EternityConfig.nexus.dimension;
        type = DimensionType.register("nexus_of_all_realities", "_nexus", id, WorldProviderNexus.class, false);
        DimensionManager.registerDimension(id, type);
    }

    public static DimensionType type() {
        return type;
    }

    public static int dimension() {
        return EternityConfig.nexus.dimension;
    }

    // ------------------------------------------------------------------ blocks

    private static IBlockState concrete(EnumDyeColor colour) {
        return Blocks.CONCRETE.getDefaultState().withProperty(BlockColored.COLOR, colour);
    }

    private static IBlockState clay(EnumDyeColor colour) {
        return Blocks.STAINED_HARDENED_CLAY.getDefaultState().withProperty(BlockColored.COLOR, colour);
    }

    private static IBlockState glass(EnumDyeColor colour) {
        return Blocks.STAINED_GLASS.getDefaultState().withProperty(BlockStainedGlass.COLOR, colour);
    }

    private static final IBlockState WHITE = concrete(EnumDyeColor.WHITE);
    private static final IBlockState BLACK = concrete(EnumDyeColor.BLACK);
    private static final IBlockState IRIS = concrete(EnumDyeColor.LIGHT_BLUE);
    private static final IBlockState IRIS_DEEP = concrete(EnumDyeColor.BLUE);
    private static final IBlockState VEIN = concrete(EnumDyeColor.RED);
    private static final IBlockState MAGENTA = concrete(EnumDyeColor.MAGENTA);
    private static final IBlockState PURPLE = concrete(EnumDyeColor.PURPLE);
    private static final IBlockState PINK = concrete(EnumDyeColor.PINK);
    private static final IBlockState ORANGE = concrete(EnumDyeColor.ORANGE);
    private static final IBlockState YELLOW = concrete(EnumDyeColor.YELLOW);
    private static final IBlockState LIME = concrete(EnumDyeColor.LIME);
    private static final IBlockState GREEN = concrete(EnumDyeColor.GREEN);
    private static final IBlockState MOSS = clay(EnumDyeColor.GREEN);
    private static final IBlockState CLAY_MAGENTA = clay(EnumDyeColor.MAGENTA);
    private static final IBlockState CLAY_PURPLE = clay(EnumDyeColor.PURPLE);
    private static final IBlockState CLAY_PINK = clay(EnumDyeColor.PINK);
    private static final IBlockState CLAY_BLUE = clay(EnumDyeColor.BLUE);
    private static final IBlockState CLAY_SKY = clay(EnumDyeColor.LIGHT_BLUE);
    private static final IBlockState CLAY_ORANGE = clay(EnumDyeColor.ORANGE);
    private static final IBlockState CYAN = concrete(EnumDyeColor.CYAN);
    private static final IBlockState CRYSTAL = glass(EnumDyeColor.PINK);
    private static final IBlockState CRYSTAL_CORE = glass(EnumDyeColor.MAGENTA);
    private static final IBlockState PANE = glass(EnumDyeColor.WHITE);
    private static final IBlockState SLIME = Blocks.SLIME_BLOCK.getDefaultState();
    private static final IBlockState LIGHT = Blocks.SEA_LANTERN.getDefaultState();
    private static final IBlockState GREY = concrete(EnumDyeColor.SILVER);

    /**
     * Near the junction colour flakes away, block by block, the more the closer; at its heart
     * every block is the one same grey. Bright colours fade pale and dark ones dim, until
     * even that difference is gone.
     */
    public static IBlockState drain(IBlockState state, int x, int y, int z) {
        float n = neutrality(x + 0.5D, z + 0.5D);
        if (n <= 0F || ((hash(x, y, z) >>> 6) & 1023) / 1024F >= n / 0.8F) {
            return state;
        }
        boolean pale = true;
        if (state.getPropertyKeys().contains(BlockColored.COLOR)) {
            EnumDyeColor colour = state.getValue(BlockColored.COLOR);
            pale = colour != EnumDyeColor.BLACK && colour != EnumDyeColor.GRAY && colour != EnumDyeColor.BLUE
                    && colour != EnumDyeColor.PURPLE && colour != EnumDyeColor.GREEN && colour != EnumDyeColor.RED
                    && colour != EnumDyeColor.BROWN;
        }
        EnumDyeColor grey = n > 0.95F || pale ? EnumDyeColor.SILVER : EnumDyeColor.GRAY;
        if (state.getBlock() == Blocks.STAINED_GLASS) {
            return glass(grey);
        }
        if (state.getBlock() == Blocks.STAINED_HARDENED_CLAY) {
            return clay(grey);
        }
        return concrete(grey);
    }

    // ------------------------------------------------------------------ features

    /** Everything whose bounds reach into this chunk. */
    public static List<Feature> featuresNear(int chunkX, int chunkZ, long seed) {
        List<Feature> near = new ArrayList<>();
        int x0 = chunkX * 16 - REACH, x1 = chunkX * 16 + 15 + REACH;
        int z0 = chunkZ * 16 - REACH, z1 = chunkZ * 16 + 15 + REACH;
        for (int cx = Math.floorDiv(x0, CELL); cx <= Math.floorDiv(x1, CELL); cx++) {
            for (int cz = Math.floorDiv(z0, CELL); cz <= Math.floorDiv(z1, CELL); cz++) {
                for (int cy = 0; cy < LAYERS; cy++) {
                    Feature feature = featureIn(cx, cy, cz, seed);
                    if (feature != null && feature.maxX >= chunkX * 16 && feature.minX <= chunkX * 16 + 15
                            && feature.maxZ >= chunkZ * 16 && feature.minZ <= chunkZ * 16 + 15) {
                        near.add(feature);
                    }
                }
                if (cx == 0 && cz == 0) {
                    near.add(new Tube(VORTEX_X, VORTEX_Y, VORTEX_Z, VORTEX_RADIUS + 1.5D, 1.4D, 0D, 0D, 1D, PURPLE));
                }
            }
        }
        if (JUNCTION.maxX >= chunkX * 16 && JUNCTION.minX <= chunkX * 16 + 15
                && JUNCTION.maxZ >= chunkZ * 16 && JUNCTION.minZ <= chunkZ * 16 + 15) {
            near.add(JUNCTION);
        }
        return near;
    }

    private static final Junction JUNCTION = new Junction();

    /** What fills one cell of the grid, if anything does. */
    @Nullable
    private static Feature featureIn(int cellX, int cellY, int cellZ, long seed) {
        if (cellX == 0 && cellZ == 0) {
            // The hub fills the middle of its column; the layers either side are kept clear around it.
            if (cellY == 2) {
                return Island.hub(seed);
            }
            if (cellY == 1 || cellY == 3) {
                return null;
            }
        }
        Random random = new Random(seed ^ (cellX * 341873128712L + cellZ * 132897987541L + cellY * 4398042316799L));
        double x = cellX * CELL + 10 + random.nextInt(CELL - 20);
        double z = cellZ * CELL + 10 + random.nextInt(CELL - 20);
        double y = BASE_Y + cellY * LAYER + 8 + random.nextInt(LAYER - 16);
        // Toward the junction less and less has drifted in, and around it nothing at all.
        double neutral = neutrality(x, z);
        if (neutral > 0D && (Math.hypot(x - JUNCTION_X, z - JUNCTION_Z) < 110D || random.nextDouble() < (neutral - 0.3D) * 1.4D)) {
            return null;
        }
        // Spires climb out of the abyss through several layers above it.
        if (cellY == 0 && random.nextInt(100) < 12) {
            double base = ABYSS_Y - 4D;
            return new Spire(x, base, z, BASE_Y - base + 90 + random.nextInt(90), random.nextDouble() * 10D);
        }
        if (random.nextInt(100) >= 72) {
            return null;
        }
        int roll = random.nextInt(100);
        if (roll < 28) {
            // Mostly small, now and then a big one.
            double r = 9D + Math.pow(random.nextDouble(), 1.5D) * 17D;
            return new Island(x, y, z, r, new Random(random.nextLong()), random.nextInt(3) == 0 ? 1 : 0);
        } else if (roll < 37) {
            return new Eye(x, y, z, 5 + random.nextInt(6), unit(random));
        } else if (roll < 45) {
            return new Orb(x, y, z, 7 + random.nextInt(5), unit(random));
        } else if (roll < 51) {
            return new Geodesic(x, y, z, 12 + random.nextInt(8));
        } else if (roll < 62) {
            double[] n = unit(random);
            // Loops stand upright more often than they lie flat, so the Nexus climbs.
            n[1] *= 0.4D;
            double len = Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]);
            return new Tube(x, y, z, 12 + random.nextInt(10), 2.2D, n[0] / len, n[1] / len, n[2] / len, ORANGE);
        } else if (roll < 71) {
            return new Crystals(x, y, z, random);
        } else if (roll < 78) {
            return new Planets(x, y, z, random);
        }
        // A grove: an island with mushrooms taller than towers growing out of it.
        return new Island(x, y - 8, z, 13 + random.nextInt(10), new Random(random.nextLong()), 2 + random.nextInt(3));
    }

    private static double[] unit(Random random) {
        double x, y, z, len;
        do {
            x = random.nextDouble() * 2D - 1D;
            y = random.nextDouble() * 2D - 1D;
            z = random.nextDouble() * 2D - 1D;
            len = x * x + y * y + z * z;
        } while (len > 1D || len < 1.0E-3);
        len = Math.sqrt(len);
        return new double[]{x / len, y / len, z / len};
    }

    private static int hash(int x, int y, int z) {
        int h = x * 73856093 ^ y * 19349663 ^ z * 83492791;
        h ^= h >>> 13;
        h *= 0x5bd1e995;
        return h ^ (h >>> 15);
    }

    /** Smooth value noise in two dimensions, between -1 and 1: the same for the same seed, everywhere, every time. */
    private static double noise(long seed, double x, double z) {
        int x0 = MathHelper.floor(x), z0 = MathHelper.floor(z);
        double fx = x - x0, fz = z - z0;
        fx = fx * fx * (3D - 2D * fx);
        fz = fz * fz * (3D - 2D * fz);
        double a = lattice(seed, x0, z0), b = lattice(seed, x0 + 1, z0);
        double c = lattice(seed, x0, z0 + 1), d = lattice(seed, x0 + 1, z0 + 1);
        return a + (b - a) * fx + (c - a) * fz + (a - b - c + d) * fx * fz;
    }

    /** Two octaves of {@link #noise}, for land that rolls rather than ripples. */
    private static double rolling(long seed, double x, double z) {
        return noise(seed, x, z) * 0.7D + noise(seed + 7L, x * 2.3D, z * 2.3D) * 0.3D;
    }

    private static double lattice(long seed, int x, int z) {
        long h = seed + x * 0x9E3779B97F4A7C15L + z * 0xC2B2AE3D27D4EB4FL;
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        h ^= h >>> 31;
        return (h >>> 11) * 0x1.0p-52 - 1D;
    }

    /** One of three, the first more often than the other two. */
    private static int mostlyFirst(Random random) {
        int roll = random.nextInt(100);
        return roll < 60 ? 0 : roll < 80 ? 1 : 2;
    }

    /** Something drifted in from another reality, occupying a box of the world. */
    public abstract static class Feature {
        public int minX, maxX, minY, maxY, minZ, maxZ;

        void bounds(double cx, double cy, double cz, double rx, double ry, double rz) {
            minX = MathHelper.floor(cx - rx);
            maxX = MathHelper.ceil(cx + rx);
            minY = MathHelper.floor(cy - ry);
            maxY = MathHelper.ceil(cy + ry);
            minZ = MathHelper.floor(cz - rz);
            maxZ = MathHelper.ceil(cz + rz);
        }

        boolean contains(int x, int y, int z) {
            return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
        }

        @Nullable
        public IBlockState at(int x, int y, int z) {
            return contains(x, y, z) ? block(x, y, z) : null;
        }

        /** The block at a point inside this feature's box, or null for empty space. */
        @Nullable
        public abstract IBlockState block(int x, int y, int z);
    }

    /**
     * A piece of some swampy reality floating on nothing, and no two alike. Each grows from
     * one to four overlapping lobes with ragged, wandering edges; its top rolls in low or
     * high hills; its underside hangs as a shallow bowl or a long jagged point, banded in
     * one of several palettes, with roots dangling from it. Slime pools in its hollows,
     * little rocks drift beside it, and on some, mushrooms taller than towers grow out of it.
     */
    static final class Island extends Feature {
        /** Swamp, bloom and mire: the ground on top, and the soil just beneath it. */
        private static final IBlockState[][] TOPS = {
                {LIME, LIME, LIME, LIME, LIME, LIME, GREEN, GREEN, GREEN, MOSS},
                {PINK, PINK, PINK, PINK, CLAY_PINK, CLAY_PINK, CLAY_PINK, MAGENTA, MAGENTA, LIME},
                {GREEN, GREEN, GREEN, GREEN, MOSS, MOSS, MOSS, MOSS, LIME, CYAN}};
        private static final IBlockState[] SOILS = {MOSS, CLAY_MAGENTA, MOSS};
        /** Violet, dusk and ember: the bands of its underside. */
        private static final IBlockState[][] BANDS = {
                {CLAY_MAGENTA, CLAY_MAGENTA, PINK, CLAY_PURPLE, CLAY_PURPLE, PURPLE, PURPLE},
                {CLAY_PURPLE, CLAY_PURPLE, CLAY_BLUE, CLAY_BLUE, PURPLE, CLAY_SKY, IRIS_DEEP},
                {CLAY_ORANGE, CLAY_ORANGE, ORANGE, CLAY_MAGENTA, CLAY_MAGENTA, PINK, CLAY_PURPLE}};

        final double cx, cy, cz;
        final long seed;
        /** Each lobe: centre x and z, radius, and how far its top is lifted. */
        final double[][] lobes;
        final double ragged, hills, hillScale, depth, taper, jag, bandScale;
        final boolean pools;
        final IBlockState[] top, bands;
        final IBlockState soil;
        /** Little rocks drifting beside it: centre and radius. */
        final double[][] rocks;
        final List<Mushroom> mushrooms = new ArrayList<>();

        // The column last asked about. The generator fills a whole column at a time.
        private int colX = Integer.MIN_VALUE, colZ = Integer.MIN_VALUE;
        private boolean colInside, colPool;
        private double colTop, colBottom, colEdge, colWarp;

        Island(double cx, double cy, double cz, double r, Random random, int mushrooms) {
            this(cx, cy, cz, r, random, mushrooms, false);
        }

        /** The hub, where travellers arrive: broad, gently rolling, a mushroom either side of the vortex. */
        static Island hub(long seed) {
            return new Island(0D, 120D, 0D, 26D, new Random(seed ^ 0x6E657875L), 2, true);
        }

        private Island(double cx, double cy, double cz, double r, Random random, int mushroomCount, boolean hub) {
            this.cx = cx;
            this.cy = cy;
            this.cz = cz;
            this.seed = random.nextLong();
            lobes = new double[1 + (hub ? 2 : random.nextInt(4))][];
            lobes[0] = new double[]{cx, cz, r, 0D};
            for (int i = 1; i < lobes.length; i++) {
                // The hub's extra lobes both lie behind the vortex, away from where travellers arrive.
                double a = hub ? Math.PI * (0.6D + 0.6D * i) : random.nextDouble() * Math.PI * 2D;
                double off = r * (0.3D + random.nextDouble() * 0.3D);
                lobes[i] = new double[]{cx + Math.cos(a) * off, cz + Math.sin(a) * off,
                        r * (0.35D + random.nextDouble() * 0.25D), hub ? 0D : random.nextDouble() * 7D - 3D};
            }
            ragged = hub ? 0.12D : 0.15D + random.nextDouble() * 0.25D;
            hills = hub ? 0.8D : 1D + random.nextDouble() * random.nextDouble() * 7D;
            hillScale = 0.03D + random.nextDouble() * 0.05D;
            depth = 0.45D + random.nextDouble() * 0.7D;
            taper = 0.5D + random.nextDouble() * 1.1D;
            jag = random.nextBoolean() ? random.nextDouble() * 0.9D : 0D;
            bandScale = 0.7D + random.nextDouble() * 1.1D;
            int ground = hub ? 0 : mostlyFirst(random);
            top = TOPS[ground];
            soil = SOILS[ground];
            bands = BANDS[hub ? 0 : mostlyFirst(random)];
            pools = hub || random.nextInt(3) > 0;

            rocks = new double[hub ? 0 : random.nextInt(6)][];
            for (int i = 0; i < rocks.length; i++) {
                double a = random.nextDouble() * Math.PI * 2D;
                double dist = r * (0.95D + random.nextDouble() * 0.35D);
                rocks[i] = new double[]{cx + Math.cos(a) * dist, cy - r * 0.35D + random.nextDouble() * r * 0.45D,
                        cz + Math.sin(a) * dist, 1.8D + random.nextDouble() * 2.7D};
            }

            for (int i = 0; i < mushroomCount; i++) {
                double mx, mz, height;
                if (hub) {
                    mx = cx + (i == 0 ? -11D : 12D);
                    mz = cz + (i == 0 ? -11D : -9D);
                    height = i == 0 ? 30D : 38D;
                } else {
                    double a = (i + random.nextDouble() * 0.6D) * Math.PI * 2D / mushroomCount;
                    double off = r * random.nextDouble() * 0.45D;
                    mx = cx + Math.cos(a) * off;
                    mz = cz + Math.sin(a) * off;
                    height = mushroomCount > 1 ? 24D + random.nextInt(28) : 10D + random.nextInt(10);
                }
                column(MathHelper.floor(mx), MathHelper.floor(mz));
                if (!colInside) {
                    continue;
                }
                double base = colTop - 3D;
                double cap = 3.5D + height * 0.2D + random.nextDouble() * 3D;
                // Nothing grows out of the top of the world.
                height = Math.min(height, 250D - base - cap);
                if (height >= 8D) {
                    mushrooms.add(new Mushroom(mx, base, mz, height, 1.2D + height * 0.04D + random.nextDouble(), cap, random));
                }
            }
            colX = Integer.MIN_VALUE;

            double reach = 0D, lift = 0D;
            for (double[] lobe : lobes) {
                reach = Math.max(reach, Math.hypot(lobe[0] - cx, lobe[1] - cz) + lobe[2] * (1D + ragged));
                lift = Math.max(lift, lobe[3]);
            }
            minX = MathHelper.floor(cx - reach) - 1;
            maxX = MathHelper.ceil(cx + reach) + 1;
            minZ = MathHelper.floor(cz - reach) - 1;
            maxZ = MathHelper.ceil(cz + reach) + 1;
            maxY = MathHelper.ceil(cy + lift + hills) + 1;
            // The deepest the underside can hang, and the roots below that.
            minY = Math.max(MathHelper.floor(ABYSS_Y) + 2, MathHelper.floor(cy - 3D - hills - r * (depth + jag)) - 30);
            for (double[] rock : rocks) {
                minX = Math.min(minX, MathHelper.floor(rock[0] - rock[3]));
                maxX = Math.max(maxX, MathHelper.ceil(rock[0] + rock[3]));
                minZ = Math.min(minZ, MathHelper.floor(rock[2] - rock[3]));
                maxZ = Math.max(maxZ, MathHelper.ceil(rock[2] + rock[3]));
                minY = Math.min(minY, MathHelper.floor(rock[1] - rock[3] * 1.6D));
            }
            for (Mushroom m : mushrooms) {
                minX = Math.min(minX, m.minX);
                maxX = Math.max(maxX, m.maxX);
                minZ = Math.min(minZ, m.minZ);
                maxZ = Math.max(maxZ, m.maxZ);
                maxY = Math.max(maxY, m.maxY);
            }
        }

        /** Works out the ground in one column: its top, its underside, and how near the rim it lies. */
        private void column(int x, int z) {
            if (x == colX && z == colZ) {
                return;
            }
            colX = x;
            colZ = z;
            colInside = false;
            colTop = -Double.MAX_VALUE;
            colBottom = Double.MAX_VALUE;
            colEdge = 1D;
            double px = x + 0.5D, pz = z + 0.5D;
            colWarp = rolling(seed, px * 0.05D, pz * 0.05D);
            double hill = rolling(seed + 1L, px * hillScale, pz * hillScale);
            double drip = Math.max(0D, noise(seed + 2L, px * 0.16D, pz * 0.16D));
            for (double[] lobe : lobes) {
                double dx = px - lobe[0], dz = pz - lobe[1];
                double f = Math.sqrt(dx * dx + dz * dz) / (lobe[2] * (1D + ragged * colWarp));
                if (f >= 1D) {
                    continue;
                }
                // The ground rolls, and falls away a little toward the rim.
                double ground = cy + lobe[3] + hills * hill - f * f * f * f * 2.5D;
                double under = lobe[2] * (depth * Math.pow(1D - f, taper) + jag * drip * drip * (1D - f));
                colInside = true;
                colTop = Math.max(colTop, ground);
                colBottom = Math.min(colBottom, ground - 1D - under);
                colEdge = Math.min(colEdge, f);
            }
            // Nothing hangs down into the abyss.
            colBottom = Math.max(colBottom, ABYSS_Y + 8D);
            colPool = colInside && pools && colEdge < 0.6D && noise(seed + 3L, px * 0.11D, pz * 0.11D) > 0.45D;
            if (colPool) {
                colTop -= 1D;
            }
        }

        @Override
        public IBlockState block(int x, int y, int z) {
            column(x, z);
            if (colInside && y <= colTop && y >= colBottom) {
                double below = colTop - y;
                if (below < 1D) {
                    return colPool ? SLIME : top[(hash(x, y, z) >>> 4) % top.length];
                }
                if (below < 2.5D + colWarp) {
                    return soil;
                }
                return bands[Math.floorMod(MathHelper.floor((y + colWarp * 3D) / bandScale), bands.length)];
            }
            if (colInside && y < colBottom && colEdge < 0.75D) {
                // Roots and drips hanging from its underside.
                int hc = hash(x, 0, z);
                if ((hc & 15) == 0 && y > colBottom - (6 + ((hc >>> 4) & 31) % 22)) {
                    return ((hc >>> 9) & 1) == 0 ? MOSS : PURPLE;
                }
            }
            for (double[] rock : rocks) {
                double vx = x + 0.5D - rock[0], vy = y + 0.5D - rock[1], vz = z + 0.5D - rock[2];
                double rr = rock[3];
                // Flat on top, hanging to a point beneath, like the islands themselves.
                double sink = vy < 0D ? vy / (rr * 1.6D) : 0D;
                if (vy <= rr * 0.35D && (vx * vx + vz * vz) / (rr * rr) + sink * sink < 1D) {
                    return vy > rr * 0.35D - 1D ? top[(hash(x, y, z) >>> 4) % top.length]
                            : bands[Math.floorMod(MathHelper.floor(y / bandScale), bands.length)];
                }
            }
            for (Mushroom m : mushrooms) {
                IBlockState state = m.at(x, y, z);
                if (state != null) {
                    return state;
                }
            }
            return null;
        }
    }

    /** An eyeball the size of a house, its iris turned toward something only it can see. */
    static final class Eye extends Feature {
        final double cx, cy, cz, r;
        final double[] look;

        Eye(double cx, double cy, double cz, double r, double[] look) {
            this.cx = cx;
            this.cy = cy;
            this.cz = cz;
            this.r = r;
            this.look = look;
            bounds(cx, cy, cz, r, r, r);
        }

        @Override
        public IBlockState block(int x, int y, int z) {
            double vx = x + 0.5D - cx, vy = y + 0.5D - cy, vz = z + 0.5D - cz;
            double d = Math.sqrt(vx * vx + vy * vy + vz * vz);
            if (d > r || d < r - 2.2D) {
                return null;
            }
            double facing = (vx * look[0] + vy * look[1] + vz * look[2]) / d;
            if (facing > 0.975D) {
                return BLACK;
            }
            if (facing > 0.91D) {
                return IRIS_DEEP;
            }
            if (facing > 0.78D) {
                return IRIS;
            }
            return facing < 0.4D && (hash(x, y, z) & 63) == 0 ? VEIN : WHITE;
        }
    }

    /** A magenta orb with a dark hole bored straight through it. */
    static final class Orb extends Feature {
        final double cx, cy, cz, r;
        final double[] axis;

        Orb(double cx, double cy, double cz, double r, double[] axis) {
            this.cx = cx;
            this.cy = cy;
            this.cz = cz;
            this.r = r;
            this.axis = axis;
            bounds(cx, cy, cz, r, r, r);
        }

        @Override
        public IBlockState block(int x, int y, int z) {
            double vx = x + 0.5D - cx, vy = y + 0.5D - cy, vz = z + 0.5D - cz;
            double d = Math.sqrt(vx * vx + vy * vy + vz * vz);
            if (d > r || d < r - 2.5D && d > 4.6D) {
                return null;
            }
            double along = vx * axis[0] + vy * axis[1] + vz * axis[2];
            double off = Math.sqrt(Math.max(0D, d * d - along * along));
            if (off < 3.2D) {
                return null;
            }
            if (off < 4.4D) {
                return BLACK;
            }
            return vy < -r * 0.45D ? PURPLE : MAGENTA;
        }
    }

    /** A geodesic sphere: pale panes on a dark frame, with dark nodes where the struts meet. */
    static final class Geodesic extends Feature {
        final double cx, cy, cz, r;

        Geodesic(double cx, double cy, double cz, double r) {
            this.cx = cx;
            this.cy = cy;
            this.cz = cz;
            this.r = r;
            bounds(cx, cy, cz, r + 1, r + 1, r + 1);
        }

        @Override
        public IBlockState block(int x, int y, int z) {
            double vx = x + 0.5D - cx, vy = y + 0.5D - cy, vz = z + 0.5D - cz;
            double d = Math.sqrt(vx * vx + vy * vy + vz * vz);
            if (Math.abs(d - r) > 0.7D) {
                return null;
            }
            double theta = Math.asin(MathHelper.clamp(vy / d, -1D, 1D));
            double phi = Math.atan2(vz, vx);
            double latStep = Math.PI / 8D;
            double lonStep = Math.PI * 2D / 14D;
            double lat = Math.abs(theta / latStep - Math.rint(theta / latStep)) * latStep * r;
            double lon = Math.abs(phi / lonStep - Math.rint(phi / lonStep)) * lonStep * r * Math.cos(theta);
            boolean onLat = lat < 0.7D;
            boolean onLon = lon < 0.7D;
            if (onLat && onLon) {
                return BLACK;
            }
            return onLat || onLon ? PURPLE : PANE;
        }
    }

    /** A thick tube bent into a loop, hanging at whatever angle it fell into this reality at. */
    static final class Tube extends Feature {
        final double cx, cy, cz, major, minor;
        final double nx, ny, nz;
        final IBlockState block;

        Tube(double cx, double cy, double cz, double major, double minor, double nx, double ny, double nz, IBlockState block) {
            this.cx = cx;
            this.cy = cy;
            this.cz = cz;
            this.major = major;
            this.minor = minor;
            this.nx = nx;
            this.ny = ny;
            this.nz = nz;
            this.block = block;
            double reach = major + minor + 1;
            bounds(cx, cy, cz, reach, reach, reach);
        }

        @Override
        public IBlockState block(int x, int y, int z) {
            double vx = x + 0.5D - cx, vy = y + 0.5D - cy, vz = z + 0.5D - cz;
            double h = vx * nx + vy * ny + vz * nz;
            double qx = vx - nx * h, qy = vy - ny * h, qz = vz - nz * h;
            double radial = Math.sqrt(qx * qx + qy * qy + qz * qz) - major;
            double d = Math.sqrt(radial * radial + h * h);
            if (d > minor) {
                return null;
            }
            if (block == PURPLE && (hash(x, y, z) & 15) == 0) {
                return LIGHT;
            }
            // A bright stripe along the top of the tube, as if lit from above.
            return block == ORANGE && h > minor * 0.55D ? YELLOW : block;
        }
    }

    /** A cluster of pink crystals, pointed at both ends, bursting out of a common core. */
    static final class Crystals extends Feature {
        final double cx, cy, cz;
        final double[][] axes;
        final double[] lengths;
        final double[] radii;

        Crystals(double cx, double cy, double cz, Random random) {
            this.cx = cx;
            this.cy = cy;
            this.cz = cz;
            int n = 5 + random.nextInt(4);
            axes = new double[n][];
            lengths = new double[n];
            radii = new double[n];
            for (int i = 0; i < n; i++) {
                axes[i] = unit(random);
                lengths[i] = 8D + random.nextDouble() * 10D;
                radii[i] = 1.8D + random.nextDouble() * 1.6D;
            }
            bounds(cx, cy, cz, 20, 20, 20);
        }

        @Override
        public IBlockState block(int x, int y, int z) {
            double vx = x + 0.5D - cx, vy = y + 0.5D - cy, vz = z + 0.5D - cz;
            for (int i = 0; i < axes.length; i++) {
                double t = vx * axes[i][0] + vy * axes[i][1] + vz * axes[i][2];
                if (t < 0D || t > lengths[i]) {
                    continue;
                }
                double ox = vx - axes[i][0] * t, oy = vy - axes[i][1] * t, oz = vz - axes[i][2] * t;
                double off = Math.sqrt(ox * ox + oy * oy + oz * oz);
                // Swelling out from the core, then narrowing to a point.
                double shape = Math.sin(Math.PI * t / lengths[i]);
                double allowed = radii[i] * Math.pow(shape, 0.6D);
                if (off < allowed) {
                    return off < allowed * 0.4D ? CRYSTAL_CORE : CRYSTAL;
                }
            }
            return null;
        }
    }

    /**
     * A mushroom on a wavering striped stalk, rooted in an island and sometimes taller than
     * a tower: a dome, a broad flat parasol or a tall bell, mostly magenta spotted pink, its
     * gills glowing faintly underneath.
     */
    static final class Mushroom extends Feature {
        private static final IBlockState[][] CAPS = {{MAGENTA, PINK}, {MAGENTA, PINK}, {MAGENTA, PINK}, {PURPLE, MAGENTA}, {PINK, WHITE}};
        private static final IBlockState[][] STALKS = {{LIME, YELLOW}, {LIME, YELLOW}, {WHITE, PINK}};

        final double cx, baseY, cz, height, stalk, cap, thick, sway, phase;
        final IBlockState[] colours, stripes;

        Mushroom(double cx, double baseY, double cz, double height, double stalk, double cap, Random random) {
            this.cx = cx;
            this.baseY = baseY;
            this.cz = cz;
            this.height = height;
            this.stalk = stalk;
            int shape = random.nextInt(3);
            this.cap = shape == 1 ? cap * 1.15D : shape == 2 ? cap * 0.8D : cap;
            this.thick = this.cap * (shape == 1 ? 0.25D : shape == 2 ? 0.7D : 0.45D);
            this.sway = Math.min(3D, height * 0.07D);
            this.phase = random.nextDouble() * 10D;
            this.colours = CAPS[random.nextInt(CAPS.length)];
            this.stripes = STALKS[random.nextInt(STALKS.length)];
            double reach = this.cap + sway * 2D + 2D;
            bounds(cx, baseY + (height + thick) / 2D, cz, reach, (height + thick) / 2D + 2D, reach);
        }

        // The stalk wanders as it climbs, but its foot stays where it is rooted.
        private double driftX(double t) {
            return (Math.sin(t * 0.08D + phase) - Math.sin(phase)) * sway;
        }

        private double driftZ(double t) {
            return (Math.cos(t * 0.06D + phase) - Math.cos(phase)) * sway * 0.7D;
        }

        @Override
        public IBlockState block(int x, int y, int z) {
            double capY = baseY + height;
            double dx = x + 0.5D - cx - driftX(height), dz = z + 0.5D - cz - driftZ(height);
            double dy = y + 0.5D - capY;
            if (dy >= -1D && dy <= thick) {
                double e = (dx * dx + dz * dz) / (cap * cap) + (dy / thick) * (dy / thick);
                if (e <= 1D && (e >= 0.45D || dy < 0.5D)) {
                    if (dy < 0.5D) {
                        return (hash(x, y, z) & 31) == 0 ? LIGHT : CLAY_PURPLE;
                    }
                    return hash(x, y, z) % 9 == 0 ? colours[1] : colours[0];
                }
            }
            if (y >= baseY && y < capY) {
                double t = y - baseY;
                double ox = x + 0.5D - cx - driftX(t), oz = z + 0.5D - cz - driftZ(t);
                // Flaring a little at the foot, where it grips the ground.
                double s = stalk * (1D + Math.max(0D, 1D - t / 4D) * 0.6D);
                if (ox * ox + oz * oz < s * s) {
                    return Math.floorMod(y, 8) < 4 ? stripes[0] : stripes[1];
                }
            }
            return null;
        }
    }

    /** Two strands twisting up around a dark core, high into the Nexus, threaded with glowing rings. */
    static final class Spire extends Feature {
        final double cx, baseY, cz, height, phase;

        Spire(double cx, double baseY, double cz, double height, double phase) {
            this.cx = cx;
            this.baseY = baseY;
            this.cz = cz;
            this.height = height;
            this.phase = phase;
            bounds(cx, baseY + height / 2D, cz, 10D, height / 2D + 1D, 10D);
        }

        @Override
        public IBlockState block(int x, int y, int z) {
            double t = y - baseY;
            if (t < 0D || t > height) {
                return null;
            }
            double dx = x + 0.5D - cx, dz = z + 0.5D - cz;
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d < 1.6D) {
                return PURPLE;
            }
            for (int k = 0; k < 2; k++) {
                double a = t * 0.14D + phase + k * Math.PI;
                double px = Math.cos(a) * 5.5D, pz = Math.sin(a) * 5.5D;
                double ox = dx - px, oz = dz - pz;
                if (ox * ox + oz * oz < 3.2D) {
                    boolean band = Math.floorMod(y, 10) < 5;
                    return k == 0 ? (band ? ORANGE : YELLOW) : (band ? MAGENTA : PINK);
                }
            }
            double ring = Math.floorMod((int) t, 22);
            if (Math.abs(ring - 11D) < 0.9D && Math.abs(d - 8D) < 0.9D) {
                return (hash(x, y, z) & 3) == 0 ? LIGHT : IRIS;
            }
            return null;
        }
    }

    /**
     * The point of absolute neutrality: a perfect disc of flat grey with no edge, no shading,
     * nothing on it but the In-Betweener's bubble hanging above. Four lines of grey stepping
     * stones lead in to it across the empty air, one of them from the direction of the hub.
     */
    static final class Junction extends Feature {
        private static final int SPOKES = 4;
        private static final double FIRST = 31D, LAST = 140D, STEP = 9D, STONE = 2.5D;
        private static final int TOP = MathHelper.floor(JUNCTION_Y) - 1;
        private static final double LATTICE_SPAN = 30D;
        private final List<double[]> stones = new ArrayList<>();

        Junction() {
            for (int s = 0; s < SPOKES; s++) {
                double a = Math.PI / 4D + s * Math.PI * 2D / SPOKES;
                for (double r = FIRST; r <= LAST; r += STEP) {
                    // A little wander, the same in every world.
                    double wander = ((hash(s, (int) r, 7) >>> 8) & 255) / 255D - 0.5D;
                    double b = a + wander * 0.06D;
                    stones.add(new double[]{JUNCTION_X + Math.cos(b) * r, JUNCTION_Z + Math.sin(b) * r});
                }
            }
            bounds(JUNCTION_X, TOP + 1, JUNCTION_Z, LAST + STONE + 1D, LATTICE_SPAN, LAST + STONE + 1D);
        }

        @Override
        public IBlockState block(int x, int y, int z) {
            double px = x + 0.5D, pz = z + 0.5D;
            double dx = px - JUNCTION_X, dz = pz - JUNCTION_Z;
            if (y >= TOP - 3 && y <= TOP && dx * dx + dz * dz <= DISC_RADIUS * DISC_RADIUS) {
                return GREY;
            }
            double d2 = dx * dx + dz * dz;
            if (y != TOP) {
                // Around the disc, a perfect lattice of identical grey cubes hangs motionless: nothing
                // drifts here, and nothing is any different from anything else.
                boolean layer = y == TOP - 12 || y == TOP + 14 || y == TOP + 28;
                if (layer && d2 > 30D * 30D && d2 < 110D * 110D
                        && Math.floorMod(x - MathHelper.floor(JUNCTION_X), 12) == 0 && Math.floorMod(z - MathHelper.floor(JUNCTION_Z), 12) == 0) {
                    return GREY;
                }
                return null;
            }
            for (double[] stone : stones) {
                double sx = px - stone[0], sz = pz - stone[1];
                if (sx * sx + sz * sz <= STONE * STONE) {
                    return GREY;
                }
            }
            return null;
        }
    }

    /** A handful of little planets, banded like the comics draw them. */
    static final class Planets extends Feature {
        final double[][] spheres;

        Planets(double cx, double cy, double cz, Random random) {
            int n = 3 + random.nextInt(3);
            spheres = new double[n][];
            for (int i = 0; i < n; i++) {
                spheres[i] = new double[]{cx + (random.nextDouble() - 0.5D) * 26D, cy + (random.nextDouble() - 0.5D) * 18D,
                        cz + (random.nextDouble() - 0.5D) * 26D, 2.5D + random.nextDouble() * 3.5D};
            }
            bounds(cx, cy, cz, 20, 16, 20);
        }

        @Override
        public IBlockState block(int x, int y, int z) {
            for (double[] s : spheres) {
                double vx = x + 0.5D - s[0], vy = y + 0.5D - s[1], vz = z + 0.5D - s[2];
                if (vx * vx + vy * vy + vz * vz < s[3] * s[3]) {
                    switch (Math.floorMod((int) Math.floor((vy + s[3]) / 1.5D), 3)) {
                        case 0:
                            return PINK;
                        case 1:
                            return CLAY_MAGENTA;
                        default:
                            return CLAY_PINK;
                    }
                }
            }
            return null;
        }
    }
}
