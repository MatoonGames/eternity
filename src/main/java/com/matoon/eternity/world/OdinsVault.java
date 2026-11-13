package com.matoon.eternity.world;

import com.matoon.eternity.EternityConfig;
import com.matoon.eternity.block.BlockVaultLight;
import com.matoon.eternity.block.BlockVaultStone.Kind;
import com.matoon.eternity.block.EternityBlocks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.DimensionType;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.common.DimensionManager;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Odin's Treasure Room, deep under the palace of Valaskjalf: a dimension of its own, and nothing
 * in it but the inside of the vault. You arrive at the foot of a flight of steps where two of the
 * Einherjar stand guard. At the top is the hall: tall and narrow, its walls stepping in as they
 * rise toward a lattice of light running the length of the ceiling, scored with diagonal grooves,
 * with deep bays down either side where the treasures stand in pools of light. A pedestal holds
 * the Casket of Ancient Winters in the middle of the floor, and at the far end, in a niche of its
 * own, against a wall of light, stands the Destroyer.
 *
 * It is built of its own stone ({@link EternityBlocks#VAULT_STONE} and the rest): blue-black basalt and
 * gold, runes cut into the walls and burning cold, braziers down the hall under Odin's banners. The
 * banners, the fire in the braziers and the runes in the air are the client's ({@code VaultHall}).
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
    /** Just off the face of the far wall, where Odin's sigil is traced over the niche. */
    public static final double SIGIL_Z = HALL_BACK + 0.05D;
    private static final int[] BAYS = {25, 11, -3, -17};
    private static final int BAY_HALF = 5, BAY_IN = 10, BAY_OUT = 14, BAY_TOP = 8;
    /** Bays in the order of {@link Relic#bay}: right side first (+x), then left. */
    private static final int ANTE_FRONT = 58, ANTE_BACK = 48, ANTE_HALF = 4, ANTE_FLOOR = 58, ANTE_ROOF = 66;
    private static final int STEPS_TOP = 36, STEPS_BOTTOM = 47, STEPS_HALF = 4, STEPS_ROOF = 73;
    private static final int NICHE_HALF = 4, NICHE_BACK = -66, NICHE_TOP = 11;

    private static final IBlockState WALL = stone(Kind.WALL);
    private static final IBlockState SLATE = stone(Kind.SLATE);
    private static final IBlockState DARK = stone(Kind.OBSIDIAN);
    private static final IBlockState FLOOR = stone(Kind.FLOOR);
    private static final IBlockState TRIM = stone(Kind.TRIM);
    private static final IBlockState CARVED = stone(Kind.CARVED);
    private static final IBlockState GOLD = stone(Kind.GOLD);
    private static final IBlockState PILLAR = stone(Kind.PILLAR);
    private static final IBlockState STEP = stone(Kind.STEP);
    private static final IBlockState HALF_STEP = EternityBlocks.VAULT_TREAD.getDefaultState();
    private static final IBlockState RUNES = EternityBlocks.VAULT_RUNES.getDefaultState();
    private static final IBlockState LATTICE = EternityBlocks.VAULT_LIGHT.of(BlockVaultLight.Kind.LATTICE);
    private static final IBlockState PANE = EternityBlocks.VAULT_LIGHT.of(BlockVaultLight.Kind.PANEL);
    private static final IBlockState UPLIGHT = EternityBlocks.VAULT_LIGHT.of(BlockVaultLight.Kind.UPLIGHT);
    private static final IBlockState LAMP = EternityBlocks.VAULT_LIGHT.of(BlockVaultLight.Kind.LAMP);
    private static final IBlockState GATE = EternityBlocks.VAULT_GATE.getDefaultState();
    private static final IBlockState BRAZIER = EternityBlocks.VAULT_BRAZIER.getDefaultState();
    private static final IBlockState BARRIER = Blocks.BARRIER.getDefaultState();
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
        /** Worked out on first use (the renderers ask for it several times a frame); immutable, so a race only repeats the sum. */
        private Vec3d base;

        Relic(int bay, int height) {
            this.bay = bay;
            this.height = height;
        }

        /** The middle of its stand's footprint, on the floor. */
        public Vec3d base() {
            Vec3d at = base;
            if (at == null) {
                if (bay < 0) {
                    at = new Vec3d(CASKET_X, STAND, CASKET_Z);
                } else {
                    int side = bay < 4 ? 1 : -1;
                    at = new Vec3d(0.5D + side * (BAY_IN + BAY_OUT) / 2D, STAND, BAYS[bay % 4] + 0.5D);
                }
                base = at;
            }
            return at;
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

    /** {@code Relic.values()} copies the array on every call; the grid's build asks hundreds of thousands of times. */
    private static final Relic[] RELICS = Relic.values();

    private static DimensionType type;

    private OdinsVault() {
    }

    private static IBlockState stone(Kind kind) {
        return EternityBlocks.VAULT_STONE.of(kind);
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
        int i = index(x, y, z);
        return i < 0 ? null : Grid.BLOCKS[i];
    }

    /** The light the vault's own lamps throw on a block. */
    public static int lightAt(int x, int y, int z) {
        int i = index(x, y, z);
        return i < 0 ? 0 : Grid.LIGHT[i];
    }

    /** Whether any of the vault reaches into this chunk; everything outside it is empty. */
    public static boolean touches(int chunkX, int chunkZ) {
        return chunkX << 4 <= X1 && (chunkX << 4) + 15 >= X0 && chunkZ << 4 <= Z1 && (chunkZ << 4) + 15 >= Z0;
    }

    /**
     * The grid, built the first time anyone asks for a block. The JVM's class initialisation is the
     * lock, taken once, so the tens of thousands of lookups each chunk makes go straight to the arrays.
     */
    private static final class Grid {
        static final IBlockState[] BLOCKS = new IBlockState[SX * SY * SZ];
        /** The open inside of the vault: air, or something standing in it. */
        static final boolean[] OPEN = new boolean[BLOCKS.length];
        static final byte[] LIGHT = new byte[BLOCKS.length];

        static {
            for (int x = X0; x <= X1; x++) {
                for (int y = Y0; y <= Y1; y++) {
                    for (int z = Z0; z <= Z1; z++) {
                        IBlockState in = inside(x, y, z);
                        if (in != null) {
                            int i = index(x, y, z);
                            OPEN[i] = true;
                            BLOCKS[i] = in == AIR ? null : in;
                        }
                    }
                }
            }
            // The walls: everything within two blocks of the inside. A 5x5x5 box is three 5-long runs, one
            // along each axis, so the inside is grown two blocks along x, then y, then z.
            boolean[] near = grow(grow(grow(OPEN, SY * SZ, SX), SZ, SY), 1, SZ);
            for (int x = X0; x <= X1; x++) {
                for (int y = Y0; y <= Y1; y++) {
                    for (int z = Z0; z <= Z1; z++) {
                        int i = index(x, y, z);
                        if (near[i] && !OPEN[i]) {
                            BLOCKS[i] = shell(x, y, z);
                        }
                    }
                }
            }
            // Light, spread from every lamp the way the game spreads it. A block already waiting in the
            // queue only has its light raised, and passes on the brighter value when its turn comes, so
            // nothing is queued twice and the queue never holds more than the grid.
            int[] queue = new int[BLOCKS.length];
            boolean[] queued = new boolean[BLOCKS.length];
            int head = 0, size = 0;
            for (int i = 0; i < BLOCKS.length; i++) {
                int emits = BLOCKS[i] == null ? 0 : BLOCKS[i].getLightValue();
                if (emits > 0) {
                    LIGHT[i] = (byte) emits;
                    queue[size++] = i;
                    queued[i] = true;
                }
            }
            int[][] steps = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
            while (size > 0) {
                int i = queue[head];
                head = (head + 1) % queue.length;
                size--;
                queued[i] = false;
                int z = i % SZ + Z0, y = (i / SZ) % SY + Y0, x = i / (SZ * SY) + X0;
                for (int[] s : steps) {
                    int j = index(x + s[0], y + s[1], z + s[2]);
                    if (j < 0) {
                        continue;
                    }
                    int opacity = BLOCKS[j] == null ? 0 : BLOCKS[j].getLightOpacity();
                    int value = LIGHT[i] - Math.max(1, opacity);
                    if (value > LIGHT[j] && opacity < 15) {
                        LIGHT[j] = (byte) value;
                        if (!queued[j]) {
                            queue[(head + size++) % queue.length] = j;
                            queued[j] = true;
                        }
                    }
                }
            }
        }

        /** Every cell within two of a set one along a single axis, given that axis's stride and length. */
        private static boolean[] grow(boolean[] in, int stride, int length) {
            boolean[] out = new boolean[in.length];
            for (int i = 0; i < in.length; i++) {
                if (!in[i]) {
                    continue;
                }
                int at = i / stride % length;
                for (int d = Math.max(-2, -at); d <= Math.min(2, length - 1 - at); d++) {
                    out[i + d * stride] = true;
                }
            }
            return out;
        }
    }

    /** The open inside of the vault and everything standing in it, or null for walls and beyond. */
    @Nullable
    private static IBlockState inside(int x, int y, int z) {
        int ax = Math.abs(x);
        // The ante-room.
        if (z >= ANTE_BACK && z <= ANTE_FRONT && ax <= ANTE_HALF && y > ANTE_FLOOR && y < ANTE_ROOF) {
            return ax == ANTE_BRAZIER_X && z == ANTE_BRAZIER_Z && y == ANTE_FLOOR + 1 ? BRAZIER : AIR;
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
        for (Relic relic : RELICS) {
            if (x == relic.blockX() && z == relic.blockZ() && h < relic.height) {
                return BARRIER;
            }
        }
        if (h == 0 && brazierAt(ax, z)) {
            return BRAZIER;
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

    /**
     * Whether someone could stand with their feet in this block: open floor inside the vault, room
     * for their head, solid floor under them (not a treasure's stand or a brazier). Nowhere outside it.
     */
    public static boolean standable(int x, int y, int z) {
        IBlockState under = blockAt(x, y - 1, z);
        return air(x, y, z) && air(x, y + 1, z) && under != null && under != BARRIER && under != BRAZIER;
    }

    /** Open air inside the vault, read off the grid rather than worked out again. */
    private static boolean air(int x, int y, int z) {
        int i = index(x, y, z);
        return i >= 0 && Grid.OPEN[i] && Grid.BLOCKS[i] == null;
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
        // The doors you came in by: a gold grille over light, in a frame of gold.
        if (z == DOOR_Z && ax <= 2 && y > ANTE_FLOOR && y <= ANTE_FLOOR + 5) {
            return GATE;
        }
        if (z == DOOR_Z + 1 && ax <= 2 && y > ANTE_FLOOR && y <= ANTE_FLOOR + 5) {
            return PANE;
        }
        if (z == DOOR_Z && (ax == 3 && y > ANTE_FLOOR && y <= ANTE_FLOOR + 6 || ax <= 3 && y == ANTE_FLOOR + 6)) {
            return GOLD;
        }
        // Down the steps and round the ante-room: lamps low in the walls every few blocks, a band of runes above.
        if (ax == STEPS_HALF + 1 && z >= STEPS_TOP && z <= ANTE_FRONT) {
            int k = STEPS_BOTTOM - z;
            int floor = z > STEPS_BOTTOM ? ANTE_FLOOR + 1 : ANTE_FLOOR + 1 + (k + 1) / 2;
            if (y == floor && Math.floorMod(z, 4) == 1) {
                return LAMP;
            }
            if (y == floor + 4) {
                return RUNES;
            }
        }
        // A strip of the lattice down the ceilings of the steps and the ante-room.
        if (ax <= 1 && (y == ANTE_ROOF && z >= ANTE_BACK && z <= ANTE_FRONT || y == STEPS_ROOF && z >= STEPS_TOP && z <= STEPS_BOTTOM)) {
            return Math.floorMod(z, 4) == 0 ? GOLD : LATTICE;
        }
        // A lamp in the ceiling of each bay, and a grille of light in the floor under every treasure.
        int bay = bayAt(z);
        if (bay != Integer.MIN_VALUE && z == bay && ax == (BAY_IN + BAY_OUT) / 2 && h == BAY_TOP + 1) {
            return LAMP;
        }
        if (y == FLOOR_Y) {
            for (Relic relic : RELICS) {
                if (x == relic.blockX() && z == relic.blockZ()) {
                    return UPLIGHT;
                }
            }
        }
        // The lattice of light along the roof, crossed by beams of gold.
        if (h >= ROOF_H && ax <= SLOT && z >= HALL_BACK && z <= HALL_FRONT) {
            return Math.floorMod(z, 3) == 0 ? GOLD : LATTICE;
        }
        // Behind the Destroyer, a wall of light; round the mouth of its niche, a frame of gold; the rest of
        // the far wall dark stone, the runes carried across it (the sigil over the niche is the client's).
        if (z == NICHE_BACK - 1 && h >= 0) {
            return Math.floorMod(h, 4) == 3 ? GOLD : PANE;
        }
        if (z == HALL_BACK - 1 && h >= 0 && h < ROOF_H) {
            if (ax <= NICHE_HALF + 1 && h <= NICHE_TOP + 1) {
                return GOLD;
            }
            return h == 0 ? DARK : h == 10 ? RUNES : WALL;
        }
        // Floors: polished slabs, a runner of gold-veined black up the aisle edged in framed tiles and lit
        // every few blocks, and the Valknut inlaid in a row across the hall under each pair of banners.
        if (y == FLOOR_Y || y == ANTE_FLOOR) {
            if (ax <= 1) {
                return DARK;
            }
            if (ax == 2) {
                return Math.floorMod(z, 6) == 0 ? UPLIGHT : TRIM;
            }
            return y == FLOOR_Y && ax >= 3 && ax <= 6 && bannerRow(z) ? CARVED : FLOOR;
        }
        // The bays: stone inside, a band of gold round them at the top, pilasters in their back corners
        // and the Valknut over the treasure.
        if (bay != Integer.MIN_VALUE && ax > HALF && h >= 0 && h <= BAY_TOP + 1) {
            if (h == BAY_TOP + 1) {
                return SLATE;
            }
            if (h == BAY_TOP) {
                return GOLD;
            }
            if (h == 0) {
                return DARK;
            }
            if (ax == BAY_OUT + 1) {
                return z == bay && h == 5 ? CARVED : Math.abs(z - bay) >= BAY_HALF ? PILLAR : WALL;
            }
            return WALL;
        }
        boolean hall = z <= HALL_FRONT && z >= HALL_BACK;
        // The Destroyer's niche, flanked by pilasters.
        if (z < HALL_BACK && z >= NICHE_BACK && ax == NICHE_HALF + 1 && h > 0 && h <= NICHE_TOP) {
            return PILLAR;
        }
        // The hall's walls: a band of runes along the top, where they start to step in; pilasters between
        // the bays and between the banners further down; above, more bands of runes climbing the vault as it
        // steps in, and diagonal grooves scored across it.
        if (hall && ax == HALF + 1 && h > 0 && h <= 10) {
            if (h == 10) {
                return RUNES;
            }
            int beat = Math.floorMod(z - BANNERS[3], 7);
            if (bay == Integer.MIN_VALUE && (z > BAYS[3] - BAY_HALF && z < BAYS[0] + BAY_HALF || z < BAYS[3] - BAY_HALF && beat >= 3 && beat <= 4)) {
                return PILLAR;
            }
        }
        if (hall && h > 10 && h < ROOF_H && ax == halfWidth(h) + 1 && (h - 10) % 4 == 0) {
            return RUNES;
        }
        if (h > 10 && hall && Math.floorMod(z + h * (x < 0 ? 1 : -1), 9) == 0) {
            return SLATE;
        }
        // A band of gold-veined black at the foot of every wall.
        if (h == 0 || y == ANTE_FLOOR + 1) {
            return DARK;
        }
        return WALL;
    }

    /** Where the braziers stand down the hall, as (|x|, z): at the head of the steps, under each banner, and by the niche. */
    private static final int[][] BRAZIERS = {{7, 33}, {7, 18}, {7, 4}, {7, -10}, {7, -24}, {7, -31}, {7, -38}, {7, -45}, {7, -52}, {6, -58}};
    /** And the two inside the doors. */
    private static final int ANTE_BRAZIER_X = 3, ANTE_BRAZIER_Z = ANTE_FRONT - 1;
    /** Where Odin's banners hang down the hall's walls, by z: between the bays, then down the long walls past them. */
    private static final int[] BANNERS = {18, 4, -10, -24, -31, -38, -45, -52};

    private static boolean bannerRow(int z) {
        for (int b : BANNERS) {
            if (b == z) {
                return true;
            }
        }
        return false;
    }

    private static boolean brazierAt(int ax, int z) {
        for (int[] b : BRAZIERS) {
            if (b[0] == ax && b[1] == z) {
                return true;
            }
        }
        return false;
    }

    /** Every brazier in the vault, the block it stands in. */
    public static List<BlockPos> braziers() {
        List<BlockPos> out = new ArrayList<>();
        for (int[] b : BRAZIERS) {
            out.add(new BlockPos(b[0], STAND, b[1]));
            out.add(new BlockPos(-b[0], STAND, b[1]));
        }
        out.add(new BlockPos(ANTE_BRAZIER_X, ANTE_FLOOR + 1, ANTE_BRAZIER_Z));
        out.add(new BlockPos(-ANTE_BRAZIER_X, ANTE_FLOOR + 1, ANTE_BRAZIER_Z));
        return out;
    }

    /**
     * Every banner: the middle of the rod it hangs from, against the wall; two blocks wide and eight
     * long, hanging facing the hall's centre line.
     */
    public static List<Vec3d> banners() {
        List<Vec3d> out = new ArrayList<>();
        for (int z : BANNERS) {
            out.add(new Vec3d(HALF + 1 - 0.07D, STAND + 10D, z + 0.5D));
            out.add(new Vec3d(-HALF + 0.07D, STAND + 10D, z + 0.5D));
        }
        return out;
    }

    // ------------------------------------------------------------------ vaults built before this one

    /**
     * Bumped whenever the vault is rebuilt. Chunks saved under an older layout are laid again from
     * {@link #blockAt} as they load ({@link VaultRefit}), so a world that visited the old vault sees the new one.
     */
    public static final int LAYOUT = 3;
    /** The heights the vault spans, with room to spare. */
    public static final int MIN_Y = 50, MAX_Y = 100;

    /** Lays the vault into a chunk saved before the last rebuild: every block, and the light on it. */
    public static void refit(Chunk chunk) {
        ExtendedBlockStorage[] storage = chunk.getBlockStorageArray();
        if (!touches(chunk.x, chunk.z) && empty(storage)) {
            // Nothing of the vault here and nothing left over from an older one: no work, and no
            // reason to mark the chunk dirty and write it back out.
            return;
        }
        boolean sky = chunk.getWorld().provider.hasSkyLight();
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int x = chunk.x * 16 + lx, z = chunk.z * 16 + lz;
                for (int y = MIN_Y; y <= MAX_Y; y++) {
                    IBlockState state = blockAt(x, y, z);
                    ExtendedBlockStorage section = storage[y >> 4];
                    if (section == Chunk.NULL_BLOCK_STORAGE) {
                        if (state == null) {
                            continue;
                        }
                        section = new ExtendedBlockStorage(y >> 4 << 4, sky);
                        storage[y >> 4] = section;
                    }
                    section.set(lx, y & 15, lz, state == null ? AIR : state);
                    section.setBlockLight(lx, y & 15, lz, lightAt(x, y, z));
                }
            }
        }
        chunk.generateSkylightMap();
        chunk.markDirty();
    }

    /** Whether the sections across the vault's heights hold nothing at all. */
    private static boolean empty(ExtendedBlockStorage[] storage) {
        for (int s = MIN_Y >> 4; s <= MAX_Y >> 4; s++) {
            if (storage[s] != Chunk.NULL_BLOCK_STORAGE && !storage[s].isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
