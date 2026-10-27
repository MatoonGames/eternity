package com.matoon.eternity.world;

import com.matoon.eternity.EternityConfig;
import net.minecraft.block.BlockAnvil;
import net.minecraft.block.BlockBone;
import net.minecraft.block.BlockColored;
import net.minecraft.block.BlockPrismarine;
import net.minecraft.block.BlockQuartz;
import net.minecraft.block.BlockRedSandstone;
import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.block.BlockSand;
import net.minecraft.block.BlockStoneBrick;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.MathHelper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Tamarata, where the Champion of the Universe lives: a desert world his wars ruined long ago.
 * Red dunes over banded terracotta, craters, plains of sand fused to glass, and everywhere the
 * remains of the people who lived here and the armies that came to fight him: broken towers of
 * an advanced civilisation, toppled colossi, the ribcages of things too big to have been
 * animals, skull piles, trench lines, and fields of dented armour.
 * <p>
 * Two places are fixed: the landing site, with an intact launch pad, and the Colosseum, the
 * ruined arena where he waits, {@link #ARENA_X} blocks east of it. Everything here is a pure
 * function of position and the world seed, so any chunk can be built alone, in any order.
 * <p>
 * Once he breaks it, the planet is a field of drifting rock and ruin; only the island the
 * landing pad stands on holds together.
 */
public final class Tamarata {

    /** Where arrivals come down, and the launch pad they leave from: the middle of its 3x3. */
    public static final int LANDING_X = 16, LANDING_Z = 16, LANDING_Y = 72;
    /** The middle of the Colosseum's floor, and the height you stand at on it. */
    public static final int ARENA_X = 168, ARENA_Z = 16, ARENA_Y = 70;
    public static final int ARENA_RADIUS = 30, SEATS_RADIUS = 44, WALL_RADIUS = 47;
    /** How much of the landing site survives the end of the planet. */
    public static final int ISLAND_RADIUS = 20;

    private static final int CELL = 64;

    private static final IBlockState AIR = Blocks.AIR.getDefaultState();
    private static final IBlockState BEDROCK = Blocks.BEDROCK.getDefaultState();
    private static final IBlockState STONE = Blocks.STONE.getDefaultState();
    private static final IBlockState GRAVEL = Blocks.GRAVEL.getDefaultState();
    private static final IBlockState COBBLE = Blocks.COBBLESTONE.getDefaultState();
    static final IBlockState RED_SAND = Blocks.SAND.getDefaultState().withProperty(BlockSand.VARIANT, BlockSand.EnumType.RED_SAND);
    private static final IBlockState SANDSTONE = Blocks.RED_SANDSTONE.getDefaultState();
    private static final IBlockState SMOOTH = Blocks.RED_SANDSTONE.getDefaultState().withProperty(BlockRedSandstone.TYPE, BlockRedSandstone.EnumType.SMOOTH);
    private static final IBlockState CHISELED = Blocks.RED_SANDSTONE.getDefaultState().withProperty(BlockRedSandstone.TYPE, BlockRedSandstone.EnumType.CHISELED);
    private static final IBlockState CLAY = Blocks.HARDENED_CLAY.getDefaultState();
    private static final IBlockState GLASS = Blocks.GLASS.getDefaultState();
    private static final IBlockState QUARTZ = Blocks.QUARTZ_BLOCK.getDefaultState();
    private static final IBlockState QUARTZ_PILLAR = Blocks.QUARTZ_BLOCK.getDefaultState().withProperty(BlockQuartz.VARIANT, BlockQuartz.EnumType.LINES_Y);
    private static final IBlockState QUARTZ_CHISELED = Blocks.QUARTZ_BLOCK.getDefaultState().withProperty(BlockQuartz.VARIANT, BlockQuartz.EnumType.CHISELED);
    private static final IBlockState PRISMARINE = Blocks.PRISMARINE.getDefaultState().withProperty(BlockPrismarine.VARIANT, BlockPrismarine.EnumType.BRICKS);
    private static final IBlockState DARK_PRISMARINE = Blocks.PRISMARINE.getDefaultState().withProperty(BlockPrismarine.VARIANT, BlockPrismarine.EnumType.DARK);
    private static final IBlockState SEA_LANTERN = Blocks.SEA_LANTERN.getDefaultState();
    private static final IBlockState IRON_BARS = Blocks.IRON_BARS.getDefaultState();
    private static final IBlockState IRON = Blocks.IRON_BLOCK.getDefaultState();
    private static final IBlockState BRICK = Blocks.STONEBRICK.getDefaultState();
    private static final IBlockState BRICK_CRACKED = Blocks.STONEBRICK.getDefaultState().withProperty(BlockStoneBrick.VARIANT, BlockStoneBrick.EnumType.CRACKED);
    private static final IBlockState BRICK_CHISELED = Blocks.STONEBRICK.getDefaultState().withProperty(BlockStoneBrick.VARIANT, BlockStoneBrick.EnumType.CHISELED);
    private static final IBlockState GOLD = Blocks.GOLD_BLOCK.getDefaultState();
    private static final IBlockState FENCE = Blocks.SPRUCE_FENCE.getDefaultState();
    private static final IBlockState SANDBAG = Blocks.WOOL.getDefaultState().withProperty(BlockColored.COLOR, EnumDyeColor.BROWN);

    private Tamarata() {
    }

    public static int dimension() {
        return EternityConfig.champion.dimension;
    }

    public static IBlockState redSand() {
        return RED_SAND;
    }

    private static IBlockState clay(EnumDyeColor colour) {
        return Blocks.STAINED_HARDENED_CLAY.getDefaultState().withProperty(BlockColored.COLOR, colour);
    }

    private static IBlockState bone(EnumFacing.Axis axis) {
        return Blocks.BONE_BLOCK.getDefaultState().withProperty(BlockRotatedPillar.AXIS, axis);
    }

    // ------------------------------------------------------------------ noise

    static double hash(long seed, int x, int z) {
        long h = seed ^ x * 0x9E3779B97F4A7C15L ^ z * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 31;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 29;
        h *= 0x94D049BB133111EBL;
        h ^= h >>> 32;
        return (h >>> 11) * 0x1.0p-53;
    }

    private static double hash3(long seed, int x, int y, int z) {
        return hash(seed ^ y * 0x632BE59BD9B4E019L, x, z);
    }

    /** Smooth value noise, 0 to 1. */
    static double noise(long seed, double x, double z) {
        int x0 = MathHelper.floor(x), z0 = MathHelper.floor(z);
        double fx = x - x0, fz = z - z0;
        fx = fx * fx * (3D - 2D * fx);
        fz = fz * fz * (3D - 2D * fz);
        double a = hash(seed, x0, z0), b = hash(seed, x0 + 1, z0), c = hash(seed, x0, z0 + 1), d = hash(seed, x0 + 1, z0 + 1);
        return a + (b - a) * fx + (c - a) * fz + (a - b - c + d) * fx * fz;
    }

    static double fbm(long seed, double x, double z) {
        return (noise(seed, x, z) * 4D + noise(seed + 1, x * 2.03D, z * 2.03D) * 2D + noise(seed + 2, x * 4.1D, z * 4.1D)) / 7D;
    }

    // ------------------------------------------------------------------ the land

    /** The height of the ground's surface at a column: the top solid block is at floor(height). */
    public static double height(long seed, int x, int z) {
        double base = 66D + 9D * fbm(seed, x / 110D, z / 110D);
        // Dunes, long and asymmetric, their crests wandering.
        double warp = fbm(seed + 11, x / 60D, z / 60D) * 5D;
        double along = (x * 0.82D + z * 0.57D) / 11D + warp;
        double crest = along - Math.floor(along);
        double dune = (crest < 0.7D ? crest / 0.7D : (1D - crest) / 0.3D);
        base += dune * dune * (2D + 4D * fbm(seed + 13, x / 70D, z / 70D));
        base += crater(seed, x, z);
        // The two places that matter are levelled.
        double land = Math.hypot(x - LANDING_X - 0.5D, z - LANDING_Z - 0.5D);
        base = blend(base, LANDING_Y - 1D, land, 16D, 34D);
        double arena = Math.hypot(x - ARENA_X - 0.5D, z - ARENA_Z - 0.5D);
        base = blend(base, ARENA_Y - 1D, arena, WALL_RADIUS + 2D, WALL_RADIUS + 26D);
        return base;
    }

    private static double blend(double natural, double level, double distance, double inner, double outer) {
        if (distance >= outer) {
            return natural;
        }
        double f = distance <= inner ? 1D : 1D - (distance - inner) / (outer - inner);
        f = f * f * (3D - 2D * f);
        return natural + (level - natural) * f;
    }

    /** Old impacts: a bowl and a raised rim, one at most in each cell of the grid. */
    private static double crater(long seed, int x, int z) {
        double total = 0D;
        int cx = Math.floorDiv(x, 96), cz = Math.floorDiv(z, 96);
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                Crater c = craterIn(seed, cx + i, cz + j);
                if (c == null) {
                    continue;
                }
                double d = Math.hypot(x - c.x, z - c.z) / c.radius;
                if (d < 1D) {
                    total -= c.depth * (1D - d * d);
                } else if (d < 1.6D) {
                    double rim = (d - 1D) / 0.6D;
                    total += c.depth * 0.35D * Math.sin(rim * Math.PI) * (1D - rim * 0.3D);
                }
            }
        }
        return total;
    }

    private static final class Crater {
        final double x, z, radius, depth;

        Crater(double x, double z, double radius, double depth) {
            this.x = x;
            this.z = z;
            this.radius = radius;
            this.depth = depth;
        }
    }

    @Nullable
    private static Crater craterIn(long seed, int cx, int cz) {
        if (hash(seed + 21, cx, cz) > 0.45D) {
            return null;
        }
        double x = cx * 96 + 16 + hash(seed + 22, cx, cz) * 64, z = cz * 96 + 16 + hash(seed + 23, cx, cz) * 64;
        if (nearSites(x, z, 70D)) {
            return null;
        }
        double radius = 6D + hash(seed + 24, cx, cz) * 16D;
        return new Crater(x, z, radius, radius * (0.35D + hash(seed + 25, cx, cz) * 0.25D));
    }

    private static boolean nearSites(double x, double z, double range) {
        return Math.hypot(x - LANDING_X, z - LANDING_Z) < range || Math.hypot(x - ARENA_X, z - ARENA_Z) < range + WALL_RADIUS;
    }

    /** Whether a column lies in a crater's bowl, where the heat of the impact fused the sand. */
    private static boolean inCraterBowl(long seed, int x, int z) {
        int cx = Math.floorDiv(x, 96), cz = Math.floorDiv(z, 96);
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                Crater c = craterIn(seed, cx + i, cz + j);
                if (c != null && Math.hypot(x - c.x, z - c.z) < c.radius * 0.7D) {
                    return true;
                }
            }
        }
        return false;
    }

    /** One column of natural ground, bottom to top, into the column array (index = y). */
    public static void column(long seed, int x, int z, IBlockState[] out) {
        double h = height(seed, x, z);
        int top = MathHelper.floor(h);
        boolean scorched = fbm(seed + 31, x / 45D, z / 45D) > 0.7D || inCraterBowl(seed, x, z);
        double bandShift = fbm(seed + 33, x / 80D, z / 80D) * 6D;
        for (int y = 0; y < out.length; y++) {
            if (y > top) {
                out[y] = null;
                continue;
            }
            if (y == 0 || y <= 2 && hash3(seed, x, y, z) < 0.5D) {
                out[y] = BEDROCK;
                continue;
            }
            int depth = top - y;
            if (depth == 0 && scorched) {
                double r = hash3(seed + 1, x, y, z);
                out[y] = r < 0.16D ? GLASS : r < 0.62D ? clay(EnumDyeColor.BLACK) : r < 0.85D ? clay(EnumDyeColor.BROWN) : clay(EnumDyeColor.GRAY);
            } else if (depth < 3 + (int) (fbm(seed + 35, x / 20D, z / 20D) * 3D)) {
                out[y] = scorched && depth < 2 ? clay(EnumDyeColor.BLACK) : RED_SAND;
            } else if (depth < 16) {
                out[y] = band(y + bandShift);
            } else if (depth < 26) {
                out[y] = SANDSTONE;
            } else {
                out[y] = STONE;
            }
        }
    }

    /** Terracotta laid down in bands, like a mesa. */
    private static IBlockState band(double y) {
        switch (Math.floorMod((int) Math.floor(y), 11)) {
            case 0:
            case 1:
                return clay(EnumDyeColor.ORANGE);
            case 2:
                return clay(EnumDyeColor.RED);
            case 3:
                return CLAY;
            case 4:
                return clay(EnumDyeColor.YELLOW);
            case 5:
            case 6:
                return clay(EnumDyeColor.ORANGE);
            case 7:
                return clay(EnumDyeColor.BROWN);
            case 8:
                return clay(EnumDyeColor.WHITE);
            case 9:
                return clay(EnumDyeColor.RED);
            default:
                return CLAY;
        }
    }

    // ------------------------------------------------------------------ the two fixed sites

    /**
     * The landing site and the Colosseum, over the natural ground. Returns the block to put at
     * a position, AIR to clear it, or null to leave the ground as it is.
     */
    @Nullable
    public static IBlockState site(long seed, int x, int y, int z) {
        double land = Math.hypot(x - LANDING_X, z - LANDING_Z);
        if (land < 16D) {
            return landing(seed, x, y, z, land);
        }
        double arena = Math.hypot(x - ARENA_X, z - ARENA_Z);
        if (arena <= WALL_RADIUS + 1D) {
            return colosseum(seed, x, y, z, arena);
        }
        return null;
    }

    /** A paved plateau, and around it a ring of broken obelisks. The pad itself is set down when the chunk is populated. */
    @Nullable
    private static IBlockState landing(long seed, int x, int y, int z, double r) {
        int floor = LANDING_Y - 1;
        if (y == floor && r < 12.5D) {
            boolean line = Math.abs(x - LANDING_X) == 0 || Math.abs(z - LANDING_Z) == 0 || Math.abs(r - 8D) < 0.5D;
            return line ? CHISELED : hash3(seed, x, y, z) < 0.12D ? SANDSTONE : SMOOTH;
        }
        if (y > floor) {
            // Eight obelisks at the plateau's edge, each broken off at its own height.
            double angle = Math.atan2(z - LANDING_Z, x - LANDING_X);
            int k = (int) Math.round(angle / (Math.PI / 4D));
            double ox = LANDING_X + Math.cos(k * Math.PI / 4D) * 11D, oz = LANDING_Z + Math.sin(k * Math.PI / 4D) * 11D;
            if (Math.abs(x - ox) < 0.9D && Math.abs(z - oz) < 0.9D) {
                int h = 3 + (int) (hash(seed + 41, k, 7) * 7D);
                int over = y - floor;
                if (over <= h) {
                    return over == h ? QUARTZ_CHISELED : QUARTZ_PILLAR;
                }
            }
            // Drifted sand at the edge of the paving.
            if (y == floor + 1 && r > 9.5D && fbm(seed + 43, x / 4D, z / 4D) > 0.6D) {
                return RED_SAND;
            }
            return y < floor + 12 ? AIR : null;
        }
        return null;
    }

    /**
     * The Colosseum: a sand-drifted floor of red sandstone, tiers of seats around it, and an
     * outer wall of arches, all of it broken. Whole sections of seating have fallen in and
     * the wall is down to its footings in places. Four gates.
     */
    @Nullable
    private static IBlockState colosseum(long seed, int x, int y, int z, double r) {
        int floor = ARENA_Y - 1;
        double angle = Math.atan2(z - ARENA_Z, x - ARENA_X);
        double degrees = Math.toDegrees(angle);
        boolean gate = Math.abs(MathHelper.wrapDegrees(degrees - Math.round(degrees / 90D) * 90D)) < 4.2D;
        // How broken this stretch of the ring is, 0 whole to 1 gone.
        double ruin = MathHelper.clamp((noise(seed + 51, angle * 5D, 0.5D) - 0.35D) * 2.2D, 0D, 1D);
        if (r <= ARENA_RADIUS) {
            if (y == floor) {
                if (Math.abs(r - 10D) < 0.6D || Math.abs(r - 22D) < 0.6D || r < 2.2D) {
                    return CHISELED;
                }
                return hash3(seed, x, y, z) < 0.08D ? SANDSTONE : SMOOTH;
            }
            if (y == floor + 1 && fbm(seed + 53, x / 6D, z / 6D) > 0.62D) {
                return RED_SAND;
            }
            return y > floor && y < floor + 40 ? AIR : null;
        }
        if (r <= SEATS_RADIUS) {
            int tier = (int) ((r - ARENA_RADIUS) / 2D);
            int seat = floor + 1 + tier * 2;
            if (ruin > 0.55D) {
                // Fallen in: a slope of rubble.
                seat = floor + (int) ((seat - floor) * (1.4D - ruin) * 0.6D) + (hash3(seed + 2, x, 0, z) < 0.3D ? 1 : 0);
            }
            if (gate && y > floor && y < floor + 9) {
                return AIR;
            }
            if (y > floor && y <= seat) {
                if (ruin > 0.55D) {
                    double roll = hash3(seed + 3, x, y, z);
                    return roll < 0.3D ? RED_SAND : roll < 0.45D ? GRAVEL : SANDSTONE;
                }
                return y == seat ? (tier % 2 == 0 ? SMOOTH : CHISELED) : SANDSTONE;
            }
            return y > floor && y < floor + 40 ? AIR : null;
        }
        // The outer wall: arches all the way round, three storeys, broken.
        double step = 360D / 40D;
        double into = Math.abs(MathHelper.wrapDegrees(degrees - Math.round(degrees / step) * step)) * r * Math.PI / 180D;
        int full = 20;
        int height = (int) (full * (1D - ruin * 0.85D)) - (int) (hash(seed + 55, (int) Math.round(degrees / step), 3) * 3D);
        if (y > floor && y < floor + 40) {
            if (gate && y < floor + 10) {
                return AIR;
            }
            int over = y - floor;
            if (over > height) {
                return over < 26 ? AIR : null;
            }
            // Three storeys, a band of carved stone between each; in every storey an arch between
            // each pair of piers, right through the wall, its top rounded.
            int storey = (over - 1) / 7, inStorey = (over - 1) % 7;
            boolean band = inStorey == 6 || over == 1;
            double half = (step * r * Math.PI / 180D) / 2D;
            double fromPier = into, width = half - 1.6D;
            if (!band && inStorey >= 1 && storey < 3 && fromPier > 1.6D) {
                double across = (fromPier - 1.6D - width / 2D) / (width / 2D);
                double top = 3.6D + Math.sqrt(Math.max(0D, 1D - across * across)) * 1.4D;
                if (inStorey <= top) {
                    return AIR;
                }
            }
            if (over == height && hash3(seed + 4, x, y, z) < 0.5D) {
                return AIR;
            }
            return band ? CHISELED : fromPier < 1.6D ? SMOOTH : SANDSTONE;
        }
        return null;
    }

    // ------------------------------------------------------------------ ruins

    /** The remains of the people of Tamarata and of the armies that came for the Champion. */
    public abstract static class Remnant {
        final int x, z, radius;
        final int ground;
        final long seed;

        Remnant(long seed, int x, int z, int radius, int ground) {
            this.seed = seed;
            this.x = x;
            this.z = z;
            this.radius = radius;
            this.ground = ground;
        }

        /** The block this remnant puts at a position, AIR to clear it, or null to leave it alone. */
        @Nullable
        public abstract IBlockState at(int px, int py, int pz, int surface);

        public int minY() {
            return ground - 20;
        }

        public int maxY() {
            return ground + 64;
        }

        /** Things that need a living world to be set out: armour stands, banners, skulls. */
        public void decorations(List<Decoration> out) {
        }

        public boolean touches(int chunkX, int chunkZ) {
            return x + radius >= chunkX * 16 && x - radius <= chunkX * 16 + 15 && z + radius >= chunkZ * 16 && z - radius <= chunkZ * 16 + 15;
        }
    }

    /** What populating a chunk sets out: an armour stand, a banner, a skull, an anvil, or the launch pad. */
    public static final class Decoration {
        public enum Kind { ARMOUR_STAND, BANNER, SKULL, PAD }

        public final Kind kind;
        public final int x, y, z;
        /** Facing, in degrees for stands and sixteenths of a turn for banners and skulls. */
        public final float facing;
        /** Which armour, which banner colour, which skull. */
        public final int variant;
        public final long seed;

        Decoration(Kind kind, int x, int y, int z, float facing, int variant, long seed) {
            this.kind = kind;
            this.x = x;
            this.y = y;
            this.z = z;
            this.facing = facing;
            this.variant = variant;
            this.seed = seed;
        }
    }

    /** Every remnant that could reach into a chunk. */
    public static List<Remnant> remnantsNear(long seed, int chunkX, int chunkZ) {
        List<Remnant> found = new ArrayList<>();
        int minCell = Math.floorDiv(chunkX * 16 - 40, CELL), maxCell = Math.floorDiv(chunkX * 16 + 56, CELL);
        int minCellZ = Math.floorDiv(chunkZ * 16 - 40, CELL), maxCellZ = Math.floorDiv(chunkZ * 16 + 56, CELL);
        for (int cx = minCell; cx <= maxCell; cx++) {
            for (int cz = minCellZ; cz <= maxCellZ; cz++) {
                Remnant remnant = remnantIn(seed, cx, cz);
                if (remnant != null && remnant.touches(chunkX, chunkZ)) {
                    found.add(remnant);
                }
            }
        }
        return found;
    }

    @Nullable
    private static Remnant remnantIn(long seed, int cx, int cz) {
        Random random = new Random(seed ^ cx * 341873128712L ^ cz * 132897987541L ^ 0x7A3A7A7AL);
        if (random.nextInt(100) >= 70) {
            return null;
        }
        int x = cx * CELL + 12 + random.nextInt(CELL - 24);
        int z = cz * CELL + 12 + random.nextInt(CELL - 24);
        if (nearSites(x, z, 46D)) {
            return null;
        }
        int ground = MathHelper.floor(height(seed, x, z));
        long s = random.nextLong();
        int roll = random.nextInt(100);
        if (roll < 22) {
            return new Tower(s, x, z, ground, random);
        }
        if (roll < 36) {
            return new Colossus(s, x, z, ground, random);
        }
        if (roll < 52) {
            return new Ribcage(s, x, z, ground, random);
        }
        if (roll < 62) {
            return new SkullPile(s, x, z, ground, random);
        }
        if (roll < 78) {
            return new Trench(s, x, z, ground, random);
        }
        return new Battlefield(s, x, z, ground, random);
    }

    /** A tower of the old civilisation, quartz banded with prismarine, its top sheared off at a slant. */
    static final class Tower extends Remnant {
        final int half, tall;
        final double shearX, shearZ;
        final boolean round;

        Tower(long seed, int x, int z, int ground, Random random) {
            super(seed, x, z, 12, ground);
            half = 2 + random.nextInt(3);
            tall = 18 + random.nextInt(36);
            double a = random.nextDouble() * Math.PI * 2D;
            shearX = Math.cos(a);
            shearZ = Math.sin(a);
            round = random.nextBoolean();
        }

        @Override
        public IBlockState at(int px, int py, int pz, int surface) {
            int dx = px - x, dz = pz - z;
            double d = round ? Math.hypot(dx, dz) : Math.max(Math.abs(dx), Math.abs(dz));
            if (py < Math.min(ground - 3, surface - 1)) {
                return null;
            }
            if (d <= half + 0.5D) {
                double lean = (dx * shearX + dz * shearZ) / (half + 1D);
                int top = ground + (int) (tall * (0.62D + 0.38D * lean)) - (int) (hash3(seed, px, 0, pz) * 3D);
                if (py > top) {
                    return py < ground + tall + 4 ? AIR : null;
                }
                boolean wall = d > half - 0.6D;
                if (!wall) {
                    return py <= ground ? QUARTZ : (py - ground) % 9 == 0 ? QUARTZ : AIR;
                }
                int level = py - ground;
                if (level % 9 == 0) {
                    return DARK_PRISMARINE;
                }
                if (level % 9 == 4 && hash3(seed + 1, px, py, pz) < 0.1D) {
                    return SEA_LANTERN;
                }
                boolean window = level % 9 >= 3 && level % 9 <= 6 && (Math.abs(dx) == 0 || Math.abs(dz) == 0);
                if (window) {
                    // Glass mostly gone; the bars left standing, rusted.
                    return hash3(seed + 2, px, py, pz) < 0.35D ? IRON_BARS : AIR;
                }
                if (hash3(seed + 3, px, py, pz) < 0.06D * (1D + level / 12D)) {
                    return AIR;
                }
                return level % 9 == 8 ? PRISMARINE : QUARTZ;
            }
            // What fell off it, piled around its foot.
            if (d < half + 7D && py == surface + 1) {
                double roll = hash3(seed + 4, px, py, pz);
                double near = 1D - (d - half) / 7D;
                if (roll < 0.25D * near) {
                    return roll < 0.08D ? PRISMARINE : QUARTZ;
                }
                if (roll < 0.3D * near) {
                    return IRON_BARS;
                }
            }
            if (d < half + 4D && py == surface + 2 && hash3(seed + 5, px, py, pz) < 0.08D) {
                return QUARTZ;
            }
            return null;
        }
    }

    /** A colossal statue of some forgotten hero of Tamarata, toppled on its side and half sunk in the sand. */
    static final class Colossus extends Remnant {
        final double dirX, dirZ;
        final int length;

        Colossus(long seed, int x, int z, int ground, Random random) {
            super(seed, x, z, 20, ground);
            double a = random.nextDouble() * Math.PI * 2D;
            dirX = Math.cos(a);
            dirZ = Math.sin(a);
            length = 12 + random.nextInt(5);
        }

        @Override
        public IBlockState at(int px, int py, int pz, int surface) {
            double dx = px - x, dz = pz - z;
            double along = dx * dirX + dz * dirZ;
            double side = -dx * dirZ + dz * dirX;
            double up = py - (ground - 1.5D);
            // The head, at the far end, face up to the sky.
            double hx = along - length * 0.5D - 4D;
            double head = hx * hx + side * side + up * up;
            if (head < 4.2D * 4.2D) {
                // Hollow eye sockets, looking straight up.
                if (up > 2.6D && Math.abs(hx + 1.2D) < 1.1D && Math.abs(Math.abs(side) - 1.6D) < 0.8D) {
                    return AIR;
                }
                if (up > 3.4D && Math.abs(hx - 1.2D) < 2D && hash3(seed, px, py, pz) < 0.5D) {
                    return GOLD;
                }
                return hash3(seed + 1, px, py, pz) < 0.2D ? BRICK_CRACKED : BRICK;
            }
            // The body: chest and belly, an ellipsoid along the axis.
            double bx = along / (length * 0.62D);
            double body = bx * bx + (side / 5D) * (side / 5D) + (up / 3.6D) * (up / 3.6D);
            if (body < 1D) {
                if (Math.abs(side) < 0.6D && up > 2.2D) {
                    return BRICK_CHISELED;
                }
                return hash3(seed + 2, px, py, pz) < 0.25D ? BRICK_CRACKED : BRICK;
            }
            // One arm flung out, broken off at the elbow.
            double ax = along - length * 0.2D, as = side - 5.5D;
            if (ax > -1.5D && ax < 2D && as > 0D && as < 9D && up > -1D && up < 1.8D) {
                return BRICK;
            }
            // The rest of the arm, lying a little apart.
            if (ax > -1D && ax < 1.6D && as > 11D && as < 15D && up > -1.5D && up < 1D) {
                return BRICK_CRACKED;
            }
            if (body < 1.3D && up < 0D && up > -2D) {
                return RED_SAND;
            }
            return null;
        }
    }

    /** The ribcage of something vast that died here, rising out of the dunes. */
    static final class Ribcage extends Remnant {
        final double dirX, dirZ;
        final int length;
        final int span;

        Ribcage(long seed, int x, int z, int ground, Random random) {
            super(seed, x, z, 18, ground);
            double a = random.nextDouble() * Math.PI * 2D;
            dirX = Math.cos(a);
            dirZ = Math.sin(a);
            length = 18 + random.nextInt(8);
            span = 6 + random.nextInt(3);
        }

        @Override
        public IBlockState at(int px, int py, int pz, int surface) {
            double dx = px - x, dz = pz - z;
            double along = dx * dirX + dz * dirZ;
            double side = -dx * dirZ + dz * dirX;
            double up = py - (ground - 2D);
            if (Math.abs(along) > length / 2D + 6D) {
                return null;
            }
            EnumFacing.Axis spineAxis = Math.abs(dirX) > Math.abs(dirZ) ? EnumFacing.Axis.X : EnumFacing.Axis.Z;
            // The spine, along the top, sagging toward the tail.
            double spineY = span * (1D - Math.pow(Math.abs(along) / (length / 2D + 6D), 3D)) + 0.5D;
            if (Math.abs(side) < 0.8D && Math.abs(up - spineY) < 0.8D && Math.abs(along) < length / 2D + 5D) {
                return bone(spineAxis);
            }
            // The ribs, arching down to the sand on both sides, every few blocks.
            if (Math.abs(along) < length / 2D) {
                double step = along - Math.floor(along / 3D) * 3D;
                if (step < 1D && hash(seed, (int) Math.floor(along / 3D), 1) > 0.15D) {
                    double rib = Math.hypot(side, up * (double) span / Math.max(1D, spineY));
                    if (Math.abs(rib - span) < 0.75D && up >= 0D) {
                        return bone(EnumFacing.Axis.Y);
                    }
                }
            }
            // The skull at the head end.
            double sx = along - length / 2D - 4D;
            double skull = sx * sx + side * side * 1.4D + (up - 2D) * (up - 2D) * 1.6D;
            if (skull < 16D) {
                if (skull < 7D) {
                    return AIR;
                }
                if (up > 2D && sx > 0.5D && Math.abs(Math.abs(side) - 1.5D) < 0.8D) {
                    return AIR;
                }
                return bone(EnumFacing.Axis.Y);
            }
            return null;
        }
    }

    /** Skulls heaped on a mound of bone: the armies who came to fight him, and lost. */
    static final class SkullPile extends Remnant {
        final int mounds;

        SkullPile(long seed, int x, int z, int ground, Random random) {
            super(seed, x, z, 8, ground);
            mounds = 3 + random.nextInt(4);
        }

        @Override
        public IBlockState at(int px, int py, int pz, int surface) {
            for (int i = 0; i < mounds; i++) {
                double mx = x + (hash(seed, i, 1) - 0.5D) * 10D, mz = z + (hash(seed, i, 2) - 0.5D) * 10D;
                double r = 1.5D + hash(seed, i, 3) * 1.8D;
                double d = Math.hypot(px - mx, pz - mz);
                int top = surface + (int) Math.round((r - d) * 0.9D);
                if (d < r && py > surface - 1 && py <= top) {
                    return hash3(seed, px, py, pz) < 0.6D ? bone(EnumFacing.Axis.Y) : clay(EnumDyeColor.WHITE);
                }
            }
            return null;
        }

        @Override
        public void decorations(List<Decoration> out) {
            for (int i = 0; i < mounds; i++) {
                double mx = x + (hash(seed, i, 1) - 0.5D) * 10D, mz = z + (hash(seed, i, 2) - 0.5D) * 10D;
                double r = 1.5D + hash(seed, i, 3) * 1.8D;
                int count = 3 + (int) (r * 2D);
                for (int k = 0; k < count; k++) {
                    double a = hash(seed, i * 31 + k, 4) * Math.PI * 2D, rr = hash(seed, i * 31 + k, 5) * r;
                    int sx = MathHelper.floor(mx + Math.cos(a) * rr), sz = MathHelper.floor(mz + Math.sin(a) * rr);
                    int variant = hash(seed, i * 31 + k, 6) < 0.25D ? 1 : 0;
                    out.add(new Decoration(Decoration.Kind.SKULL, sx, -1, sz,
                            (float) Math.floor(hash(seed, i * 31 + k, 7) * 16D), variant, seed + k));
                }
            }
        }
    }

    /** A trench line, dug and abandoned: sandbags, posts, and the bars they strung for wire. */
    static final class Trench extends Remnant {
        final double dirX, dirZ;
        final int length;

        Trench(long seed, int x, int z, int ground, Random random) {
            super(seed, x, z, 22, ground);
            double a = random.nextDouble() * Math.PI * 2D;
            dirX = Math.cos(a);
            dirZ = Math.sin(a);
            length = 26 + random.nextInt(14);
        }

        @Override
        public IBlockState at(int px, int py, int pz, int surface) {
            double dx = px - x, dz = pz - z;
            double along = dx * dirX + dz * dirZ;
            double side = -dx * dirZ + dz * dirX;
            if (Math.abs(along) > length / 2D) {
                return null;
            }
            // It zigzags, the way trenches do.
            double zig = Math.abs(((along / 6D) % 2D + 2D) % 2D - 1D) * 2.5D - 1.25D;
            double s = side - zig;
            if (Math.abs(s) < 1.6D) {
                if (py == surface - 3) {
                    return COBBLE;
                }
                if (py > surface - 3 && py <= surface + 2) {
                    return AIR;
                }
                return null;
            }
            if (Math.abs(s) < 2.6D) {
                if (py > surface - 3 && py <= surface) {
                    return hash3(seed, px, py, pz) < 0.2D ? GRAVEL : COBBLE;
                }
                if (py == surface + 1) {
                    return hash3(seed + 1, px, py, pz) < 0.75D ? SANDBAG : AIR;
                }
                return null;
            }
            // Posts in front of the parapet, barred where the wire was.
            if (Math.abs(s) > 4D && Math.abs(s) < 5D && Math.floorMod((int) Math.floor(along), 4) == 0) {
                if (py == surface + 1) {
                    return FENCE;
                }
                if (py == surface + 2 && hash3(seed + 2, px, py, pz) < 0.6D) {
                    return FENCE;
                }
            }
            if (Math.abs(s) > 4D && Math.abs(s) < 5D && py == surface + 2 && hash3(seed + 3, px, py, pz) < 0.45D) {
                return IRON_BARS;
            }
            return null;
        }

        @Override
        public int minY() {
            return ground - 16;
        }
    }

    /**
     * Where one of the armies made its last stand: armour stands in dented armour, swords and
     * spears rusted into the ground, broken anvils, and torn banners.
     */
    static final class Battlefield extends Remnant {
        final int stands;

        Battlefield(long seed, int x, int z, int ground, Random random) {
            super(seed, x, z, 10, ground);
            stands = 4 + random.nextInt(5);
        }

        @Override
        public IBlockState at(int px, int py, int pz, int surface) {
            double d = Math.hypot(px - x, pz - z);
            if (d > radius) {
                return null;
            }
            double roll = hash3(seed, px, 0, pz);
            // Weapons driven into the ground: a rusted blade of bars, sometimes with a hilt.
            if (roll < 0.035D && py > surface && py <= surface + 2 + (int) (roll * 40D)) {
                return py == surface + 2 && roll < 0.015D ? IRON : IRON_BARS;
            }
            if (roll > 0.985D && py == surface + 1) {
                int damage = (int) ((roll - 0.985D) / 0.005D) % 3;
                return Blocks.ANVIL.getDefaultState().withProperty(BlockAnvil.DAMAGE, damage)
                        .withProperty(BlockAnvil.FACING, EnumFacing.byHorizontalIndex((int) (roll * 1000D) % 4));
            }
            if (roll > 0.94D && roll < 0.96D && py == surface + 1) {
                return FENCE;
            }
            return null;
        }

        @Override
        public void decorations(List<Decoration> out) {
            for (int i = 0; i < stands; i++) {
                double a = hash(seed, i, 11) * Math.PI * 2D, r = 1D + hash(seed, i, 12) * (radius - 2D);
                int sx = MathHelper.floor(x + Math.cos(a) * r), sz = MathHelper.floor(z + Math.sin(a) * r);
                out.add(new Decoration(Decoration.Kind.ARMOUR_STAND, sx, -1, sz, (float) (hash(seed, i, 13) * 360D),
                        (int) (hash(seed, i, 14) * 1000D), seed + i));
            }
            for (int i = 0; i < 2; i++) {
                double a = hash(seed, i, 21) * Math.PI * 2D, r = 2D + hash(seed, i, 22) * (radius - 3D);
                int bx = MathHelper.floor(x + Math.cos(a) * r), bz = MathHelper.floor(z + Math.sin(a) * r);
                out.add(new Decoration(Decoration.Kind.BANNER, bx, -1, bz, (float) Math.floor(hash(seed, i, 23) * 16D),
                        (int) (hash(seed, i, 24) * 4D), seed + 100 + i));
            }
        }
    }

    /** What populating a chunk around the fixed sites sets out: the launch pad, and banners and skulls in the Colosseum. */
    public static void siteDecorations(long seed, List<Decoration> out) {
        out.add(new Decoration(Decoration.Kind.PAD, LANDING_X, LANDING_Y, LANDING_Z, 0F, 0, seed));
        // Torn banners at the gates and on the arena's edge.
        for (int k = 0; k < 4; k++) {
            double a = k * Math.PI / 2D;
            for (int s = -1; s <= 1; s += 2) {
                double b = a + s * Math.toRadians(6.5D);
                int bx = MathHelper.floor(ARENA_X + 0.5D + Math.cos(b) * (ARENA_RADIUS - 0.5D));
                int bz = MathHelper.floor(ARENA_Z + 0.5D + Math.sin(b) * (ARENA_RADIUS - 0.5D));
                float facing = (float) Math.floorMod(Math.round((Math.toDegrees(b) + 90D) / 22.5D), 16);
                out.add(new Decoration(Decoration.Kind.BANNER, bx, ARENA_Y, bz, facing, k, seed + k * 2 + (s > 0 ? 1 : 0)));
            }
        }
        // Skulls left lying in the seats.
        for (int i = 0; i < 18; i++) {
            double a = hash(seed, i, 61) * Math.PI * 2D;
            double r = ARENA_RADIUS + 1D + hash(seed, i, 62) * 12D;
            int sx = MathHelper.floor(ARENA_X + Math.cos(a) * r), sz = MathHelper.floor(ARENA_Z + Math.sin(a) * r);
            out.add(new Decoration(Decoration.Kind.SKULL, sx, -1, sz, (float) Math.floor(hash(seed, i, 63) * 16D),
                    hash(seed, i, 64) < 0.3D ? 1 : 0, seed + i));
        }
    }

    // ------------------------------------------------------------------ broken

    /**
     * Whether a position in the broken planet is part of the island that survived around the
     * landing pad: the ground there, cut away beneath into a jagged point.
     */
    public static boolean onIsland(long seed, int x, int y, int z) {
        double r = Math.hypot(x - LANDING_X, z - LANDING_Z);
        if (r > ISLAND_RADIUS) {
            return false;
        }
        double edge = r / ISLAND_RADIUS;
        double ragged = noise(seed + 71, x / 3D, z / 3D) * 4D;
        int bottom = LANDING_Y - 2 - (int) ((1D - edge * edge) * 22D + ragged);
        return y >= bottom && edge < 1D - noise(seed + 72, Math.atan2(z - LANDING_Z, x - LANDING_X) * 3D, 0.3D) * 0.25D;
    }

    /** The drifting fragments lie in cells of a coarse grid, between these heights. */
    public static final int FRAGMENT_CELL = 20, FRAGMENT_LOW = 24, FRAGMENT_HIGH = 150;
    /** How far a fragment's ragged edge can reach past its radius: the most its roughness ever adds. */
    private static final double FRAGMENT_REACH = 1.35D;

    /** Something told about each block of fragment in a chunk. */
    public interface FragmentSink {
        void put(int x, int y, int z, IBlockState state);
    }

    /**
     * The fragment in one cell of the coarse grid, as {x, y, z, radius} of its middle; null if the
     * cell is empty, as most are.
     */
    @Nullable
    private static double[] fragmentIn(long seed, int cx, int cy, int cz) {
        double roll = hash3(seed + 81, cx, cy, cz);
        if (roll > 0.16D) {
            return null;
        }
        return new double[]{cx * FRAGMENT_CELL + 4 + hash3(seed + 82, cx, cy, cz) * 12D,
                cy * FRAGMENT_CELL + 4 + hash3(seed + 83, cx, cy, cz) * 12D,
                cz * FRAGMENT_CELL + 4 + hash3(seed + 84, cx, cy, cz) * 12D,
                1.2D + hash3(seed + 85, cx, cy, cz) * 4.2D, roll};
    }

    /**
     * Every block of drifting fragment within one chunk, and nothing else: only the few cells that
     * hold a fragment are looked at, and only inside each one's reach, so empty space costs nothing.
     * Exactly the blocks {@link #fragment} gives, so the generator, the breaker and anything broken
     * later always agree.
     */
    public static void fragmentsInChunk(long seed, int chunkX, int chunkZ, FragmentSink sink) {
        int x0 = chunkX * 16, z0 = chunkZ * 16;
        // fragment() only ever asks the cell a block lies in, so each lump is clipped to its own cell.
        for (int cx = Math.floorDiv(x0, FRAGMENT_CELL); cx <= Math.floorDiv(x0 + 15, FRAGMENT_CELL); cx++) {
            for (int cz = Math.floorDiv(z0, FRAGMENT_CELL); cz <= Math.floorDiv(z0 + 15, FRAGMENT_CELL); cz++) {
                for (int cy = Math.floorDiv(FRAGMENT_LOW, FRAGMENT_CELL); cy <= Math.floorDiv(FRAGMENT_HIGH, FRAGMENT_CELL); cy++) {
                    double[] f = fragmentIn(seed, cx, cy, cz);
                    if (f == null) {
                        continue;
                    }
                    double reach = f[3] * FRAGMENT_REACH + 1D;
                    int minX = Math.max(Math.max(x0, cx * FRAGMENT_CELL), (int) Math.floor(f[0] - reach)), maxX = Math.min(Math.min(x0 + 15, cx * FRAGMENT_CELL + FRAGMENT_CELL - 1), (int) Math.ceil(f[0] + reach));
                    int minZ = Math.max(Math.max(z0, cz * FRAGMENT_CELL), (int) Math.floor(f[2] - reach)), maxZ = Math.min(Math.min(z0 + 15, cz * FRAGMENT_CELL + FRAGMENT_CELL - 1), (int) Math.ceil(f[2] + reach));
                    int minY = Math.max(Math.max(FRAGMENT_LOW, cy * FRAGMENT_CELL), (int) Math.floor(f[1] - reach / 1.3D)), maxY = Math.min(Math.min(FRAGMENT_HIGH, cy * FRAGMENT_CELL + FRAGMENT_CELL - 1), (int) Math.ceil(f[1] + reach / 1.3D));
                    for (int x = minX; x <= maxX; x++) {
                        for (int z = minZ; z <= maxZ; z++) {
                            for (int y = minY; y <= maxY; y++) {
                                IBlockState state = fragment(seed, x, y, z);
                                if (state != null) {
                                    sink.put(x, y, z, state);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * The drifting rock and ruin the planet broke into: a lump in some cells of a coarse grid,
     * each a ragged ball of whatever the ground there was made of. Returns null for empty space.
     */
    @Nullable
    public static IBlockState fragment(long seed, int x, int y, int z) {
        if (y < FRAGMENT_LOW || y > FRAGMENT_HIGH) {
            return null;
        }
        double[] f = fragmentIn(seed, Math.floorDiv(x, FRAGMENT_CELL), Math.floorDiv(y, FRAGMENT_CELL), Math.floorDiv(z, FRAGMENT_CELL));
        if (f == null) {
            return null;
        }
        double fx = f[0], fy = f[1], fz = f[2], radius = f[3], roll = f[4];
        double dx = x + 0.5D - fx, dy = (y + 0.5D - fy) * 1.3D, dz = z + 0.5D - fz;
        double d = Math.sqrt(dx * dx + dy * dy + dz * dz) / radius;
        double ragged = 0.75D + hash3(seed + 86, x, y, z) * 0.3D + noise(seed + 87, (x + y) / 2D, (z - y) / 2D) * 0.3D;
        if (d > ragged) {
            return null;
        }
        if (roll < 0.025D) {
            // A piece of a ruin, torn loose.
            return hash3(seed + 88, x, y, z) < 0.7D ? QUARTZ : PRISMARINE;
        }
        // The top of a fragment is the surface it was torn from; inside, rock.
        double top = fy + radius * 0.5D;
        if (y + 0.5D > top) {
            return RED_SAND;
        }
        if (y + 0.5D > top - 2D) {
            return band(y);
        }
        return roll < 0.08D ? SANDSTONE : STONE;
    }
}
