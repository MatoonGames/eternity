package com.matoon.eternity.world;

import com.matoon.eternity.EternityConfig;
import net.minecraft.block.BlockColored;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStone;
import net.minecraft.block.BlockStoneBrick;
import net.minecraft.block.BlockStoneSlab;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.DimensionType;
import net.minecraftforge.common.DimensionManager;

import javax.annotation.Nullable;
import java.util.ArrayDeque;

/**
 * Odin's Treasure Room, deep under the palace of Valaskjalf: a dimension of its own, and nothing
 * in it but the inside of the vault. You arrive at the foot of a flight of steps where two of the
 * Einherjar stand guard. At the top is the hall: tall and narrow, its walls stepping in as they
 * rise toward a lattice of light running the length of the ceiling, scored with diagonal grooves,
 * with deep bays down either side where the treasures stand in pools of light. A pedestal holds
 * the Casket of Ancient Winters in the middle of the floor, and at the far end, in a niche of its
 * own, against a wall of light, stands the Destroyer.
 *
 * The treasures and their stands are not blocks; the client draws them where {@link Relic} says.
 * Mjolnir and the Tesseract are HeroesExpansion's own, set out on their stands by the director.
 *
 * Block x is mirrored with |x|, so everything is symmetric about x = 0.5, the hall's centre line.
 */
public final class OdinsVault {

    /** The hall's floor blocks; everyone stands at {@link #STAND}. */
    public static final int FLOOR_Y = 64, STAND = 65;
    /** The ante-room at the foot of the steps, where you arrive, and the doors behind it that lead out. */
    public static final double ARRIVE_X = 0.5D, ARRIVE_Y = 59D, ARRIVE_Z = 54.5D;
    public static final int DOOR_Z = 59;
    /** Where the Einherjar stand guard: at the head of the steps, one either side. */
    public static final double POST_X = 3.5D, POST_Y = STAND, POST_Z = 33.5D;
    /** The Destroyer's niche at the far end, and the pedestal in the middle of the floor. */
    public static final double NICHE_X = 0.5D, NICHE_Y = STAND, NICHE_Z = -62.5D;
    public static final double CASKET_X = 0.5D, CASKET_Z = -38.5D;

    /** The hall from the head of the steps back to the far wall, and where the bays are. */
    private static final int HALL_FRONT = 35, HALL_BACK = -60, HALF = 9, SLOT = 2, ROOF_H = 27;
    private static final int[] BAYS = {25, 11, -3, -17};
    private static final int BAY_HALF = 5, BAY_IN = 10, BAY_OUT = 14, BAY_TOP = 8;
    /** Bays in the order of {@link Relic#bay}: right side first (+x), then left. */
    private static final int ANTE_FRONT = 58, ANTE_BACK = 48, ANTE_HALF = 4, ANTE_FLOOR = 58, ANTE_ROOF = 66;
    private static final int STEPS_TOP = 36, STEPS_BOTTOM = 47, STEPS_HALF = 4, STEPS_ROOF = 73;
    private static final int NICHE_HALF = 4, NICHE_BACK = -66, NICHE_TOP = 11;

    private static final IBlockState WALL = Blocks.STONE.getDefaultState().withProperty(BlockStone.VARIANT, BlockStone.EnumType.ANDESITE_SMOOTH);
    private static final IBlockState GROOVE = concrete(EnumDyeColor.GRAY);
    private static final IBlockState DARK = concrete(EnumDyeColor.BLACK);
    private static final IBlockState FLOOR = concrete(EnumDyeColor.GRAY);
    private static final IBlockState TRIM = Blocks.STONEBRICK.getDefaultState();
    private static final IBlockState CARVED = Blocks.STONEBRICK.getDefaultState().withProperty(BlockStoneBrick.VARIANT, BlockStoneBrick.EnumType.CHISELED);
    private static final IBlockState GOLD = Blocks.GOLD_BLOCK.getDefaultState();
    private static final IBlockState OCHRE = Blocks.STAINED_HARDENED_CLAY.getDefaultState().withProperty(BlockColored.COLOR, EnumDyeColor.YELLOW);
    private static final IBlockState LIGHT = Blocks.SEA_LANTERN.getDefaultState();
    private static final IBlockState GLOW = Blocks.GLOWSTONE.getDefaultState();
    private static final IBlockState BARS = Blocks.IRON_BARS.getDefaultState();
    private static final IBlockState BARRIER = Blocks.BARRIER.getDefaultState();
    private static final IBlockState STEP = Blocks.STONEBRICK.getDefaultState();
    private static final IBlockState HALF_STEP = Blocks.STONE_SLAB.getDefaultState()
            .withProperty(BlockStoneSlab.VARIANT, BlockStoneSlab.EnumType.SMOOTHBRICK).withProperty(BlockSlab.HALF, BlockSlab.EnumBlockHalf.BOTTOM);
    /** Marks a block of the open inside of the vault. */
    private static final IBlockState AIR = Blocks.AIR.getDefaultState();

    /**
     * The treasures, each on its stand: which bay it is in (0 to 3 down the right side from the
     * steps, 4 to 7 down the left), and how tall its stand is in whole blocks (an invisible
     * barrier stands there so nobody walks through it).
     */
    public enum Relic {
        WARLOCKS_EYE(0, 1), TESSERACT(1, 1), CROWN(2, 1), ETERNAL_FLAME(3, 3),
        GAUNTLET(4, 1), TUNING_FORK(5, 2), TABLET(6, 2), MJOLNIR(7, 1),
        CASKET(-1, 1);

        public final int bay, height;

        Relic(int bay, int height) {
            this.bay = bay;
            this.height = height;
        }

        /** The middle of its stand's footprint, on the floor. */
        public Vec3d base() {
            if (bay < 0) {
                return new Vec3d(CASKET_X, STAND, CASKET_Z);
            }
            int side = bay < 4 ? 1 : -1;
            return new Vec3d(0.5D + side * (BAY_IN + BAY_OUT) / 2D, STAND, BAYS[bay % 4] + 0.5D);
        }

        /** Which way it faces: out of its bay toward the aisle, in degrees of yaw. */
        public float yaw() {
            return bay < 0 ? 0F : bay < 4 ? 90F : -90F;
        }

        /** The block its stand's barrier starts at. */
        int blockX() {
            return (int) Math.floor(base().x);
        }

        int blockZ() {
            return (int) Math.floor(base().z);
        }
    }

    private static DimensionType type;

    private OdinsVault() {
    }

    private static IBlockState concrete(EnumDyeColor color) {
        return Blocks.CONCRETE.getDefaultState().withProperty(BlockColored.COLOR, color);
    }

    public static void register() {
        int id = EternityConfig.vault.dimension;
        type = DimensionType.register("odins_vault", "_odins_vault", id, WorldProviderVault.class, false);
        DimensionManager.registerDimension(id, type);
    }

    public static DimensionType type() {
        return type;
    }

    public static int dimension() {
        return EternityConfig.vault.dimension;
    }

    /** Whether someone standing at this point has climbed the steps into the hall itself. */
    public static boolean inHall(double x, double y, double z) {
        return z < HALL_FRONT + 1 && y > FLOOR_Y;
    }

    /** Whether someone has walked back into the doors they came in by. */
    public static boolean atDoors(double x, double y, double z) {
        return z > DOOR_Z - 0.75D && Math.abs(x - 0.5D) < 3D && y < ANTE_ROOF;
    }

    // ------------------------------------------------------------------ what is where

    // Everything that is anywhere, worked out once: the box it all fits in, every block in it, and
    // the light each lamp throws, which is laid straight into chunks as they generate.
    private static final int X0 = -BAY_OUT - 3, X1 = BAY_OUT + 3, Y0 = ANTE_FLOOR - 3, Y1 = STAND + ROOF_H + 3, Z0 = NICHE_BACK - 3, Z1 = DOOR_Z + 3;
    private static final int SX = X1 - X0 + 1, SY = Y1 - Y0 + 1, SZ = Z1 - Z0 + 1;
    private static IBlockState[] blocks;
    private static byte[] light;

    private static int index(int x, int y, int z) {
        if (x < X0 || x > X1 || y < Y0 || y > Y1 || z < Z0 || z > Z1) {
            return -1;
        }
        return ((x - X0) * SY + (y - Y0)) * SZ + (z - Z0);
    }

    /**
     * The block at a world position: something inside the vault, a piece of its walls, floor or
     * ceiling, or null for open air and the nothing outside.
     */
    @Nullable
    public static IBlockState blockAt(int x, int y, int z) {
        build();
        int i = index(x, y, z);
        return i < 0 ? null : blocks[i];
    }

    /** The light the vault's own lamps throw on a block. */
    public static int lightAt(int x, int y, int z) {
        build();
        int i = index(x, y, z);
        return i < 0 ? 0 : light[i];
    }

    private static synchronized void build() {
        if (light != null) {
            return;
        }
        IBlockState[] grid = new IBlockState[SX * SY * SZ];
        boolean[] open = new boolean[grid.length];
        for (int x = X0; x <= X1; x++) {
            for (int y = Y0; y <= Y1; y++) {
                for (int z = Z0; z <= Z1; z++) {
                    IBlockState in = inside(x, y, z);
                    if (in != null) {
                        open[index(x, y, z)] = true;
                        grid[index(x, y, z)] = in == AIR ? null : in;
                    }
                }
            }
        }
        // The walls: everything within two blocks of the inside.
        for (int x = X0; x <= X1; x++) {
            for (int y = Y0; y <= Y1; y++) {
                for (int z = Z0; z <= Z1; z++) {
                    int i = index(x, y, z);
                    if (open[i]) {
                        continue;
                    }
                    search:
                    for (int dx = -2; dx <= 2; dx++) {
                        for (int dy = -2; dy <= 2; dy++) {
                            for (int dz = -2; dz <= 2; dz++) {
                                int j = index(x + dx, y + dy, z + dz);
                                if (j >= 0 && open[j]) {
                                    grid[i] = shell(x, y, z);
                                    break search;
                                }
                            }
                        }
                    }
                }
            }
        }
        // Light, spread from every lamp the way the game spreads it.
        byte[] lit = new byte[grid.length];
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        for (int i = 0; i < grid.length; i++) {
            int emits = grid[i] == null ? 0 : grid[i].getLightValue();
            if (emits > 0) {
                lit[i] = (byte) emits;
                queue.add(i);
            }
        }
        int[][] steps = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
        while (!queue.isEmpty()) {
            int i = queue.poll();
            int z = i % SZ + Z0, y = (i / SZ) % SY + Y0, x = i / (SZ * SY) + X0;
            for (int[] s : steps) {
                int j = index(x + s[0], y + s[1], z + s[2]);
                if (j < 0) {
                    continue;
                }
                int opacity = grid[j] == null ? 0 : grid[j].getLightOpacity();
                int value = lit[i] - Math.max(1, opacity);
                if (value > lit[j] && opacity < 15) {
                    lit[j] = (byte) value;
                    queue.add(j);
                }
            }
        }
        blocks = grid;
        light = lit;
    }

    /** The open inside of the vault and everything standing in it, or null for walls and beyond. */
    @Nullable
    private static IBlockState inside(int x, int y, int z) {
        int ax = Math.abs(x);
        // The ante-room.
        if (z >= ANTE_BACK && z <= ANTE_FRONT && ax <= ANTE_HALF && y > ANTE_FLOOR && y < ANTE_ROOF) {
            return AIR;
        }
        // The steps, half a block at a time from the ante-room's floor to the hall's.
        if (z >= STEPS_TOP && z <= STEPS_BOTTOM && ax <= STEPS_HALF && y > ANTE_FLOOR && y < STEPS_ROOF) {
            int k = STEPS_BOTTOM - z;
            int halves = k + 1;
            int full = halves / 2;
            if (y <= ANTE_FLOOR + full) {
                return STEP;
            }
            if (halves % 2 == 1 && y == ANTE_FLOOR + full + 1) {
                return HALF_STEP;
            }
            return AIR;
        }
        if (y <= FLOOR_Y) {
            return null;
        }
        int h = y - STAND;
        // The Destroyer's niche, and the stands' barriers.
        if (z < HALL_BACK && z >= NICHE_BACK && ax <= NICHE_HALF && h <= NICHE_TOP) {
            return AIR;
        }
        if (z > HALL_FRONT || z < HALL_BACK) {
            return null;
        }
        for (Relic relic : Relic.values()) {
            if (x == relic.blockX() && z == relic.blockZ() && h < relic.height) {
                return BARRIER;
            }
        }
        // The hall, stepping in toward the roof.
        if (ax <= halfWidth(h) && h < ROOF_H) {
            return AIR;
        }
        // The bays.
        if (bayAt(z) != Integer.MIN_VALUE && ax >= BAY_IN && ax <= BAY_OUT && h <= BAY_TOP) {
            return AIR;
        }
        return null;
    }

    /** The hall's half-width at a height above the floor: upright to the tops of the bays, then stepping in. */
    private static int halfWidth(int h) {
        if (h < 0) {
            return -1;
        }
        return h <= 10 ? HALF : Math.max(SLOT, HALF - (h - 9) / 2);
    }

    /** The middle of the bay at z, or MIN_VALUE between bays. */
    private static int bayAt(int z) {
        for (int bay : BAYS) {
            if (Math.abs(z - bay) <= BAY_HALF) {
                return bay;
            }
        }
        return Integer.MIN_VALUE;
    }

    /** The walls, floors and ceilings: what they are made of depends on where they face. */
    private static IBlockState shell(int x, int y, int z) {
        int ax = Math.abs(x);
        int h = y - STAND;
        // The doors you came in by: a grille over light.
        if (z == DOOR_Z && ax <= 2 && y > ANTE_FLOOR && y <= ANTE_FLOOR + 5) {
            return BARS;
        }
        if (z == DOOR_Z + 1 && ax <= 2 && y > ANTE_FLOOR && y <= ANTE_FLOOR + 5) {
            return LIGHT;
        }
        // Lamps set into the walls of the steps and the ante-room, low down, every few blocks.
        if (ax == STEPS_HALF + 1 && z >= STEPS_TOP && z <= ANTE_FRONT && Math.floorMod(z, 4) == 1) {
            int k = STEPS_BOTTOM - z;
            int floor = z > STEPS_BOTTOM ? ANTE_FLOOR + 1 : ANTE_FLOOR + 1 + (k + 1) / 2;
            if (y == floor) {
                return LIGHT;
            }
        }
        // A lamp in the ceiling of each bay, and one in the floor under every treasure, lighting it from below.
        int bay = bayAt(z);
        if (bay != Integer.MIN_VALUE && z == bay && ax == (BAY_IN + BAY_OUT) / 2 && h == BAY_TOP + 1) {
            return GLOW;
        }
        if (y == FLOOR_Y) {
            for (Relic relic : Relic.values()) {
                if (x == relic.blockX() && z == relic.blockZ()) {
                    return GLOW;
                }
            }
        }
        // The lattice of light along the roof, and the wall of light at the far end and behind the niche.
        if (h >= ROOF_H && ax <= SLOT && z >= HALL_BACK && z <= HALL_FRONT) {
            return x == 0 || Math.floorMod(z, 3) == 0 ? DARK : LIGHT;
        }
        if ((z == HALL_BACK - 1 && !(ax <= NICHE_HALF && h <= NICHE_TOP) || z == NICHE_BACK - 1) && h >= 0 && h < ROOF_H) {
            return ax % 3 == 0 || Math.floorMod(h, 3) == 2 ? DARK : LIGHT;
        }
        // Floors: dark stone, a black runner up the aisle edged in stone, bands of carved stone between the bays.
        if (y == FLOOR_Y || y == ANTE_FLOOR) {
            if (ax <= 1) {
                return DARK;
            }
            if (ax == 2) {
                return Math.floorMod(z, 6) == 0 ? OCHRE : TRIM;
            }
            return bayAt(z) == Integer.MIN_VALUE && z <= HALL_FRONT && z >= HALL_BACK && ax <= HALF ? CARVED : FLOOR;
        }
        // The bays are dark stone inside, with a band of gold round them at the top.
        if (bay != Integer.MIN_VALUE && ax > HALF && h >= 0 && h <= BAY_TOP + 1) {
            return h == BAY_TOP ? GOLD : GROOVE;
        }
        // Diagonal grooves scored across the walls as they rise.
        if (h > 0 && z <= HALL_FRONT && z >= HALL_BACK && Math.floorMod(z + h * (x < 0 ? 1 : -1), 9) == 0) {
            return GROOVE;
        }
        // A dark band at the foot of every wall.
        if (h == 0 || y == ANTE_FLOOR + 1) {
            return GROOVE;
        }
        return WALL;
    }
}
