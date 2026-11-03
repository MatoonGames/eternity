package com.matoon.eternity.world;

import com.matoon.eternity.EternityConfig;
import com.matoon.eternity.block.BlockSovereignHull.Kind;
import com.matoon.eternity.block.EternityBlocks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Warlock's home, the Sovereign station, as a pure function of position: one spine of halls
 * running east from a landing ring open to space, each sealed from the next by an iris door.
 * <ol>
 * <li><b>The landing ring</b>, open to the stars, and the arm into the station.</li>
 * <li><b>The atrium</b>: a dome of amber glass over the Earth, ribbed in gold; a statue of their
 * High Priestess at its heart, her face cut away.</li>
 * <li><b>The Hall of Cocoons</b>: split-open birthing cocoons in the walls, some that never
 * opened, and at the far end a great one, his.</li>
 * <li><b>The vats</b>: a ring of growing-vats, some smashed open from inside.</li>
 * <li><b>The archive</b>: her records, on consoles.</li>
 * <li><b>The Gallery of the Ascended</b>: frosted capsules with sleepers in them who look like
 * him, older and wrong.</li>
 * <li><b>The Sanctum</b>: her dais and throne under the biggest dome, a ring of dark mirrors.</li>
 * </ol>
 * Built once into a grid and laid into chunks as they generate, already lit by the station's own
 * seams of light. Everything the story needs to find again (doors, consoles, capsules, the dais,
 * where the window will open) is listed here by name.
 */
public final class SovereignStation {

    public static final int FLOOR = 100;
    /** Where arrivals come down, on the landing ring. */
    public static final BlockPos LANDING = new BlockPos(0, FLOOR + 1, 0);

    // The halls, west to east: where each starts and ends on the spine.
    static final int ATRIUM_X = 30, ATRIUM_R = 13;
    static final int COCOONS_X0 = 48, COCOONS_X1 = 84, COCOONS_HALF = 6, COCOONS_H = 10;
    public static final int VATS_X = 100, VATS_R = 11;
    static final int ARCHIVE_X0 = 116, ARCHIVE_X1 = 136, ARCHIVE_HALF = 7, ARCHIVE_H = 8;
    static final int GALLERY_X0 = 140, GALLERY_X1 = 176, GALLERY_HALF = 5, GALLERY_H = 9;
    public static final int SANCTUM_X = 196, SANCTUM_R = 16;
    /** Her dais, the throne on it, and where the window opens: the heart of the Sanctum. */
    public static final BlockPos DAIS = new BlockPos(SANCTUM_X + 9, FLOOR + 2, 0);
    public static final BlockPos THRONE = new BlockPos(SANCTUM_X + 12, FLOOR + 3, 0);
    public static final BlockPos WINDOW = new BlockPos(SANCTUM_X - 2, FLOOR + 6, 0);

    private static final int X0 = -14, X1 = SANCTUM_X + SANCTUM_R + 10, Y0 = FLOOR - 12, Y1 = FLOOR + 30, Z0 = -24, Z1 = 24;
    private static final int SX = X1 - X0 + 1, SY = Y1 - Y0 + 1, SZ = Z1 - Z0 + 1;

    /** A door between halls: its name, and the blocks that fill it. */
    public static final class Door {
        public final String name;
        public final int x;
        public final List<BlockPos> blocks = new ArrayList<>();

        Door(String name, int x) {
            this.name = name;
            this.x = x;
        }

        public BlockPos centre() {
            return new BlockPos(x, FLOOR + 2, 0);
        }
    }

    private static IBlockState[] blocks;
    private static byte[] light;
    private static final Map<String, Door> DOORS = new LinkedHashMap<>();
    private static final Map<BlockPos, String> CONSOLES = new LinkedHashMap<>();
    private static final Map<BlockPos, String> PODS = new LinkedHashMap<>();
    private static final Map<BlockPos, EnumFacing> POD_FACING = new LinkedHashMap<>();
    /** Where the cocoons and vats stand, for the things that come out of them. */
    private static final List<BlockPos> BROKEN_VATS = new ArrayList<>();
    private static final List<BlockPos> LIGHTS = new ArrayList<>();

    private SovereignStation() {
    }

    public static int dimension() {
        return EternityConfig.sovereign.dimension;
    }

    // ------------------------------------------------------------------ what the story needs

    public static Map<String, Door> doors() {
        build();
        return Collections.unmodifiableMap(DOORS);
    }

    /** The console at a position, by record name; null if there is none. */
    @Nullable
    public static String consoleAt(BlockPos pos) {
        build();
        return CONSOLES.get(pos);
    }

    @Nullable
    public static String podAt(BlockPos pos) {
        build();
        return PODS.get(pos);
    }

    /** Every capsule's foot, and which way its sleeper faces. */
    public static Map<BlockPos, EnumFacing> pods() {
        build();
        return Collections.unmodifiableMap(POD_FACING);
    }

    public static List<BlockPos> brokenVats() {
        build();
        return Collections.unmodifiableList(BROKEN_VATS);
    }

    /** Every seam of light, for the director to let a few of them fail. */
    public static List<BlockPos> lights() {
        build();
        return Collections.unmodifiableList(LIGHTS);
    }

    /** Which hall a position is in: 0 the landing, 1 the atrium ... 6 the Sanctum; -1 outside. */
    public static int hallAt(double x, double y, double z) {
        if (y < FLOOR - 2 || y > FLOOR + 26 || Math.abs(z) > 22) {
            return -1;
        }
        if (x < 16) {
            return 0;
        }
        if (x < COCOONS_X0) {
            return 1;
        }
        if (x < 88) {
            return 2;
        }
        if (x < ARCHIVE_X0) {
            return 3;
        }
        if (x < GALLERY_X0) {
            return 4;
        }
        if (x < 180) {
            return 5;
        }
        return 6;
    }

    @Nullable
    public static IBlockState blockAt(int x, int y, int z) {
        build();
        int i = index(x, y, z);
        return i < 0 ? null : blocks[i];
    }

    public static int lightAt(int x, int y, int z) {
        build();
        int i = index(x, y, z);
        return i < 0 ? 0 : light[i];
    }

    /** The dark mirrors round the Sanctum's floor: every pane of every one of them. */
    public static List<BlockPos> mirrors() {
        List<BlockPos> out = new ArrayList<>();
        for (int k = 0; k < 12; k++) {
            double a = k * Math.PI / 6D;
            int x = SANCTUM_X + (int) Math.round(Math.cos(a) * (SANCTUM_R - 3)), z = (int) Math.round(Math.sin(a) * (SANCTUM_R - 3));
            if (x > SANCTUM_X + 4 || k == 6) {
                continue;
            }
            for (int y = FLOOR + 2; y <= FLOOR + 4; y++) {
                out.add(new BlockPos(x, y, z));
            }
        }
        return out;
    }

    /**
     * The top of the Sanctum's dome, as built: every block of it above a ragged line round
     * the dome, {@code from} blocks over the floor give or take a few, highest first.
     */
    public static List<BlockPos> domeCap(int from, long seed) {
        java.util.Random rand = new java.util.Random(seed);
        List<BlockPos> out = new ArrayList<>();
        int top = FLOOR + 10 + SANCTUM_R + 1;
        for (int y = top; y >= FLOOR + from - 3; y--) {
            for (int x = SANCTUM_X - SANCTUM_R - 1; x <= SANCTUM_X + SANCTUM_R + 1; x++) {
                for (int z = -SANCTUM_R - 1; z <= SANCTUM_R + 1; z++) {
                    // The line it tears along wanders: the same for a given seed.
                    double a = Math.atan2(z, x - SANCTUM_X);
                    double edge = from + 1.6D * Math.sin(a * 5D + seed % 7) + 1.2D * Math.sin(a * 11D + seed % 5);
                    if (y - FLOOR < edge || blockAt(x, y, z) == null) {
                        continue;
                    }
                    out.add(new BlockPos(x, y, z));
                }
            }
        }
        Collections.shuffle(out.subList(0, out.size()), rand);
        out.sort((p, q) -> q.getY() - p.getY());
        return out;
    }

    /** The Sanctum and the halls round it put back exactly as they were built. Returns how many blocks it took. */
    public static int restoreSanctum(net.minecraft.world.World world) {
        int n = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        // As far out as Warlock's going reaches.
        for (int x = SANCTUM_X - 32; x <= X1; x++) {
            for (int z = Z0; z <= Z1; z++) {
                for (int y = FLOOR - 2; y <= FLOOR + 10 + SANCTUM_R + 4; y++) {
                    pos.setPos(x, y, z);
                    IBlockState want = blockAt(x, y, z);
                    if (want == null) {
                        want = net.minecraft.init.Blocks.AIR.getDefaultState();
                    }
                    if (world.getBlockState(pos) != want) {
                        world.setBlockState(pos, want, 2);
                        n++;
                    }
                }
            }
        }
        return n;
    }

    public static int minY() {
        return Y0;
    }

    public static int maxY() {
        return Y1;
    }

    private static int index(int x, int y, int z) {
        if (x < X0 || x > X1 || y < Y0 || y > Y1 || z < Z0 || z > Z1) {
            return -1;
        }
        return ((x - X0) * SY + (y - Y0)) * SZ + (z - Z0);
    }

    // ------------------------------------------------------------------ building it

    private static IBlockState[] grid;
    /** Open air the halls have claimed, which no wall may fill. */
    private static boolean[] open;
    /** Open space outside that the skin must leave open: the mouth of the arm onto the landing ring. */
    private static boolean[] outside;

    private static IBlockState hull(Kind kind) {
        return EternityBlocks.SOVEREIGN_HULL.of(kind);
    }

    private static void put(int x, int y, int z, @Nullable IBlockState state) {
        int i = index(x, y, z);
        if (i >= 0) {
            grid[i] = state;
        }
    }

    private static void air(int x, int y, int z) {
        int i = index(x, y, z);
        if (i >= 0) {
            grid[i] = null;
            open[i] = true;
        }
    }

    /** A wall block, unless a hall has already claimed the space. */
    private static void wall(int x, int y, int z, IBlockState state) {
        int i = index(x, y, z);
        if (i >= 0 && !open[i]) {
            grid[i] = state;
        }
    }


    private static synchronized void build() {
        if (light != null) {
            return;
        }
        grid = new IBlockState[SX * SY * SZ];
        open = new boolean[grid.length];
        outside = new boolean[grid.length];
        landing();
        atrium();
        hallOfCocoons();
        vats();
        archive();
        gallery();
        sanctum();
        corridor(15, ATRIUM_X - ATRIUM_R + 1, 2, 4, "atrium");
        corridor(ATRIUM_X + ATRIUM_R - 1, COCOONS_X0, 2, 4, "cocoons");
        corridor(COCOONS_X1, VATS_X - VATS_R + 1, 2, 4, "vats");
        corridor(VATS_X + VATS_R - 1, ARCHIVE_X0, 2, 4, "archive");
        corridor(ARCHIVE_X1, GALLERY_X0, 2, 4, "gallery");
        corridor(GALLERY_X1, SANCTUM_X - SANCTUM_R + 1, 2, 4, "sanctum");
        skin();
        keel();
        blocks = grid;
        light = spread(grid);
        grid = null;
        open = null;
        outside = null;
    }

    /** Everything open with nothing in it, wrapped in a shell of plate where it meets the void. */
    private static void skin() {
        IBlockState plate = hull(Kind.PLATE), dark = hull(Kind.DARK);
        for (int x = X0 + 1; x < X1; x++) {
            for (int y = Y0 + 1; y < Y1; y++) {
                for (int z = Z0 + 1; z < Z1; z++) {
                    int i = index(x, y, z);
                    if (open[i] || outside[i] || grid[i] != null) {
                        continue;
                    }
                    // Next to a hall, but not part of it: the outer skin.
                    if (open[index(x + 1, y, z)] || open[index(x - 1, y, z)] || open[index(x, y + 1, z)] || open[index(x, y - 1, z)]
                            || open[index(x, y, z + 1)] || open[index(x, y, z - 1)]) {
                        grid[i] = noise(x, y, z) > 0.8F ? dark : plate;
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ the landing ring

    private static void landing() {
        IBlockState floor = hull(Kind.FLOOR), trim = hull(Kind.TRIM), rib = hull(Kind.RIB), plate = hull(Kind.PLATE);
        for (int x = -10; x <= 10; x++) {
            for (int z = -10; z <= 10; z++) {
                double r = Math.sqrt(x * x + z * z);
                if (r <= 9.5D) {
                    put(x, FLOOR, z, r > 8.5D ? trim : r < 2.5D ? hull(Kind.PANEL) : floor);
                    put(x, FLOOR - 1, z, plate);
                    if (r > 6.5D) {
                        put(x, FLOOR - 2, z, hull(Kind.DARK));
                    }
                }
                // A ring of light set into the deck, round the pad.
                if (r > 4.5D && r < 5.5D) {
                    put(x, FLOOR, z, EternityBlocks.SOVEREIGN_LIGHT.of(true));
                }
            }
        }
        // Pylons round the rim, each crowned with light: the ring was meant to be seen from far off.
        for (int k = 0; k < 8; k++) {
            if (k == 0) {
                continue; // the arm into the station goes this way
            }
            double a = k * Math.PI / 4D;
            int px = (int) Math.round(Math.cos(a) * 9D), pz = (int) Math.round(Math.sin(a) * 9D);
            for (int y = FLOOR + 1; y <= FLOOR + 4; y++) {
                put(px, y, pz, rib);
            }
            put(px, FLOOR + 5, pz, EternityBlocks.SOVEREIGN_LIGHT.of(true));
            put(px, FLOOR + 6, pz, trim);
        }
        // The arm: a covered walk from the ring to the atrium's door, open at the ring end.
        for (int y = FLOOR + 1; y <= FLOOR + 5; y++) {
            for (int z = -2; z <= 2; z++) {
                for (int x = 5; x <= 8; x++) {
                    int i = index(x, y, z);
                    if (i >= 0) {
                        outside[i] = true;
                    }
                }
            }
        }
        for (int x = 9; x <= 16; x++) {
            for (int z = -3; z <= 3; z++) {
                for (int y = FLOOR; y <= FLOOR + 6; y++) {
                    boolean shell = Math.abs(z) == 3 || y == FLOOR || y == FLOOR + 6;
                    if (shell) {
                        put(x, y, z, y == FLOOR ? floor : Math.abs(z) == 3 && y == FLOOR + 3 ? EternityBlocks.SOVEREIGN_GLASS.getDefaultState()
                                : x % 3 == 0 ? rib : plate);
                    } else {
                        air(x, y, z);
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ a round hall under a dome

    /**
     * A round hall: walls {@code wallH} high, then a dome of glass ribbed with gold, every seam
     * of the ribbing lit at its foot.
     */
    private static void domed(int cx, int r, int wallH, boolean glassDome) {
        IBlockState floor = hull(Kind.FLOOR), panel = hull(Kind.PANEL), rib = hull(Kind.RIB), trim = hull(Kind.TRIM);
        IBlockState glass = EternityBlocks.SOVEREIGN_GLASS.getDefaultState();
        for (int x = cx - r - 1; x <= cx + r + 1; x++) {
            for (int z = -r - 1; z <= r + 1; z++) {
                double d = Math.sqrt((x - cx) * (x - cx) + z * z);
                if (d > r + 0.5D) {
                    continue;
                }
                double a = Math.atan2(z, x - cx);
                boolean ribLine = Math.abs(Math.sin(a * 6D)) < 0.13D;
                for (int y = FLOOR; y <= FLOOR + wallH + r; y++) {
                    int h = y - FLOOR;
                    double domeR = h <= wallH ? r : Math.sqrt(Math.max(0D, r * r - (h - wallH) * (h - wallH) * 1.0D));
                    boolean inside = d < domeR - 0.5D;
                    boolean surface = d <= domeR + 0.5D && !inside;
                    if (h == 0) {
                        put(x, y, z, d > r - 1.5D ? trim : floor);
                        put(x, y - 1, z, hull(Kind.DARK));
                    } else if (inside) {
                        air(x, y, z);
                    } else if (surface) {
                        IBlockState s;
                        if (h <= wallH) {
                            boolean between = Math.abs(Math.sin(a * 6D)) > 0.5D;
                            s = ribLine ? rib : h == wallH || h == 4 ? trim : h == 1 ? hull(Kind.DARK)
                                    : h == wallH - 1 && between ? EternityBlocks.SOVEREIGN_LIGHT.of(true) : h <= 3 ? panel : hull(Kind.PLATE);
                        } else {
                            s = ribLine || !glassDome ? rib : glass;
                        }
                        wall(x, y, z, s);
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ 1: the atrium

    private static void atrium() {
        domed(ATRIUM_X, ATRIUM_R, 7, true);
        // Her statue: a robed figure three times a man's height, on a plinth, her face cut away.
        IBlockState plate = hull(Kind.PLATE), panel = hull(Kind.PANEL), trim = hull(Kind.TRIM), dark = hull(Kind.DARK);
        int cx = ATRIUM_X;
        for (int x = cx - 2; x <= cx + 2; x++) {
            for (int z = -2; z <= 2; z++) {
                put(x, FLOOR + 1, z, Math.abs(x - cx) == 2 || Math.abs(z) == 2 ? trim : panel);
            }
        }
        for (int y = FLOOR + 2; y <= FLOOR + 9; y++) {
            int h = y - FLOOR - 2;
            // The robe flares at the hem and narrows to the shoulders.
            int half = h < 2 ? 1 : h < 6 ? 1 : 0;
            for (int x = cx - half; x <= cx + half; x++) {
                for (int z = -half; z <= half; z++) {
                    put(x, y, z, h % 3 == 2 ? trim : plate);
                }
            }
        }
        // Shoulders and arms, held out, palms up: she offered something.
        for (int z = -2; z <= 2; z++) {
            put(cx, FLOOR + 8, z, plate);
        }
        put(cx - 1, FLOOR + 7, -3, plate);
        put(cx - 1, FLOOR + 7, 3, plate);
        put(cx - 1, FLOOR + 8, -3, trim);
        put(cx - 1, FLOOR + 8, 3, trim);
        // Her head, and the place on it where a face was, chiselled flat and black.
        put(cx, FLOOR + 10, 0, plate);
        put(cx - 1, FLOOR + 10, 0, dark);
        put(cx, FLOOR + 11, 0, trim);
        // A circlet of light round the plinth.
        for (int[] l : new int[][]{{-3, 0}, {3, 0}, {0, -3}, {0, 3}}) {
            put(cx + l[0], FLOOR + 1, l[1], EternityBlocks.SOVEREIGN_LIGHT.of(true));
        }
        consoleAt(ATRIUM_X + 6, 7, EnumFacing.SOUTH, "welcome");
    }

    // ------------------------------------------------------------------ 2: the Hall of Cocoons

    /** A long hall: plate and panel walls, fluted ribs every four blocks, light along the cornice, a vaulted ceiling. */
    private static void hall(int x0, int x1, int half, int h) {
        IBlockState floor = hull(Kind.FLOOR), panel = hull(Kind.PANEL), plate = hull(Kind.PLATE), rib = hull(Kind.RIB), trim = hull(Kind.TRIM);
        for (int x = x0; x <= x1; x++) {
            boolean ribbed = (x - x0) % 4 == 0;
            for (int z = -half - 1; z <= half + 1; z++) {
                // The ceiling vaults: higher in the middle.
                int roof = h - (int) Math.round(Math.pow(Math.abs(z) / (double) (half + 1), 2) * 3D);
                for (int y = FLOOR; y <= FLOOR + h + 1; y++) {
                    int dy = y - FLOOR;
                    if (dy == 0) {
                        put(x, y, z, Math.abs(z) == half ? trim : Math.abs(z) <= 1 && x % 2 == 0 ? hull(Kind.PANEL) : floor);
                        put(x, y - 1, z, hull(Kind.DARK));
                    } else if (Math.abs(z) <= half && dy < roof) {
                        air(x, y, z);
                    } else if (Math.abs(z) == half + 1 && dy <= roof) {
                        // The wall, in bands: a bronze foot, a row of sunburst panels, a beaded rail,
                        // brushed plate above, and a seam of light under the cornice.
                        IBlockState s;
                        if (ribbed) {
                            s = rib;
                        } else if (dy == 1) {
                            s = hull(Kind.DARK);
                        } else if (dy == 2 || dy == 3) {
                            s = panel;
                        } else if (dy == 4) {
                            s = trim;
                        } else if (dy == roof - 1 && x % 2 == 0) {
                            s = EternityBlocks.SOVEREIGN_LIGHT.of(true);
                        } else {
                            s = plate;
                        }
                        wall(x, y, z, s);
                    } else if (Math.abs(z) <= half && dy == roof) {
                        // The vault: bronze coffers between the ribs, a seam of light down its spine.
                        IBlockState s = ribbed ? rib : z == 0 && x % 4 == 2 ? EternityBlocks.SOVEREIGN_LIGHT.of(true)
                                : Math.abs(z) % 3 == 1 ? trim : hull(Kind.DARK);
                        wall(x, y, z, s);
                    }
                }
            }
        }
    }

    private static void hallOfCocoons() {
        hall(COCOONS_X0, COCOONS_X1, COCOONS_HALF, COCOONS_H);
        // Alcoves down both walls, a cocoon in each: opened, most of them; some never; a few fused black.
        int n = 0;
        for (int x = COCOONS_X0 + 2; x <= COCOONS_X1 - 10; x += 4) {
            for (int side = -1; side <= 1; side += 2) {
                int z = side * COCOONS_HALF;
                // Cut the alcove back into the wall.
                for (int y = FLOOR + 1; y <= FLOOR + 3; y++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        air(x + dx, y, z + side);
                    }
                }
                EnumFacing facing = side < 0 ? EnumFacing.SOUTH : EnumFacing.NORTH;
                int kind = (n * 7 + (side > 0 ? 3 : 0)) % 5;
                if (kind < 3) {
                    put(x, FLOOR + 1, z + side, EternityBlocks.COCOON.of(facing, false, false));
                    put(x, FLOOR + 2, z + side, EternityBlocks.COCOON.of(facing, true, false));
                } else {
                    boolean fused = kind == 4;
                    put(x, FLOOR + 1, z + side, EternityBlocks.COCOON_SEALED.of(EnumFacing.NORTH, false, fused));
                    put(x, FLOOR + 2, z + side, EternityBlocks.COCOON_SEALED.of(EnumFacing.NORTH, true, fused));
                }
                put(x, FLOOR + 4, z + side, EternityBlocks.SOVEREIGN_LIGHT.of(kind != 4));
            }
            n++;
        }
        // His: the great cocoon at the end of the hall, split open down its front, taller than three men.
        int cx = COCOONS_X1 - 4;
        IBlockState shell = hull(Kind.SHELL), membrane = hull(Kind.MEMBRANE);
        for (int y = FLOOR + 1; y <= FLOOR + 9; y++) {
            double t = (y - FLOOR - 1) / 8D;
            double r = 3.4D * Math.sin(Math.PI * (0.12D + t * 0.8D));
            for (int x = cx - 4; x <= cx + 4; x++) {
                for (int z = -4; z <= 4; z++) {
                    double d = Math.sqrt((x - cx) * (x - cx) * 1.2D + z * z);
                    if (d > r + 0.5D) {
                        continue;
                    }
                    boolean skin = d > r - 0.7D;
                    // Split down the west face, the way he came out.
                    boolean split = x < cx && Math.abs(z) <= 1 && y > FLOOR + 1 && y < FLOOR + 9;
                    if (split) {
                        air(x, y, z);
                    } else if (skin) {
                        put(x, y, z, shell);
                    } else {
                        put(x, y, z, x == cx + (int) r - 1 ? membrane : null);
                        if (x != cx + (int) r - 1) {
                            air(x, y, z);
                        }
                    }
                }
            }
        }
        put(cx, FLOOR + 1, 0, membrane);
        consoleAt(cx - 6, 3, EnumFacing.WEST, "his_cocoon");
        consoleAt(COCOONS_X0 + 8, -4, EnumFacing.NORTH, "cocoons");
    }

    // ------------------------------------------------------------------ 3: the vats

    private static void vats() {
        domed(VATS_X, VATS_R, 9, false);
        for (int k = 0; k < 12; k++) {
            double a = k * Math.PI / 6D + Math.PI / 12D;
            int x = VATS_X + (int) Math.round(Math.cos(a) * 8D), z = (int) Math.round(Math.sin(a) * 8D);
            // The ones toward the way in are the ones that broke: whatever grew there went out that way.
            boolean broken = k == 5 || k == 6 || k == 8 || k == 2;
            for (int part = 0; part < 3; part++) {
                put(x, FLOOR + 1 + part, z, EternityBlocks.VAT.part(part, broken));
            }
            put(x, FLOOR + 4, z, hull(Kind.TRIM));
            if (broken) {
                BROKEN_VATS.add(new BlockPos(x, FLOOR + 1, z));
            }
        }
        // The keeper's station in the middle, where someone watched them grow.
        for (int x = VATS_X - 1; x <= VATS_X + 1; x++) {
            for (int z = -1; z <= 1; z++) {
                put(x, FLOOR + 1, z, hull(Kind.TRIM));
            }
        }
        put(VATS_X, FLOOR + 2, 0, EternityBlocks.CONSOLE.of(EnumFacing.WEST, false, false));
        CONSOLES.put(new BlockPos(VATS_X, FLOOR + 2, 0), "vats");
    }

    // ------------------------------------------------------------------ 4: the archive

    private static void archive() {
        hall(ARCHIVE_X0, ARCHIVE_X1, ARCHIVE_HALF, ARCHIVE_H);
        // Two rows of consoles, facing the walls, between record stacks of trim and panel.
        String[] records = {"subjects", "rejected", "recycled", "the_one_who_left", "him", "church"};
        int i = 0;
        for (int x = ARCHIVE_X0 + 4; x <= ARCHIVE_X1 - 4 && i < records.length; x += 6) {
            for (int side = -1; side <= 1 && i < records.length; side += 2) {
                consoleAt(x, side * (ARCHIVE_HALF - 2), side < 0 ? EnumFacing.SOUTH : EnumFacing.NORTH, records[i++]);
                for (int y = FLOOR + 1; y <= FLOOR + 4; y++) {
                    put(x - 1, y, side * ARCHIVE_HALF, y % 2 == 0 ? hull(Kind.TRIM) : hull(Kind.PANEL));
                    put(x + 1, y, side * ARCHIVE_HALF, y % 2 == 0 ? hull(Kind.TRIM) : hull(Kind.PANEL));
                }
            }
        }
    }

    // ------------------------------------------------------------------ 5: the Gallery of the Ascended

    private static void gallery() {
        hall(GALLERY_X0, GALLERY_X1, GALLERY_HALF, GALLERY_H);
        int i = 0;
        for (int x = GALLERY_X0 + 3; x <= GALLERY_X1 - 3; x += 5) {
            for (int side = -1; side <= 1; side += 2) {
                int z = side * (GALLERY_HALF - 1);
                EnumFacing facing = side < 0 ? EnumFacing.SOUTH : EnumFacing.NORTH;
                for (int part = 0; part < 3; part++) {
                    put(x, FLOOR + 1 + part, z, EternityBlocks.POD.part(part, false));
                }
                put(x, FLOOR + 4, z, hull(Kind.TRIM));
                PODS.put(new BlockPos(x, FLOOR + 1, z), "ascended_" + i++);
                POD_FACING.put(new BlockPos(x, FLOOR + 1, z), facing);
                // A mirror behind each, so you see him twice.
                for (int y = FLOOR + 1; y <= FLOOR + 4; y++) {
                    put(x, y, side * (GALLERY_HALF + 1), hull(Kind.MIRROR));
                }
            }
        }
    }

    // ------------------------------------------------------------------ 6: the Sanctum

    private static void sanctum() {
        domed(SANCTUM_X, SANCTUM_R, 10, true);
        IBlockState trim = hull(Kind.TRIM), panel = hull(Kind.PANEL), floor = hull(Kind.FLOOR), plate = hull(Kind.PLATE);
        // The dais: three steps up, half a hall wide, at the east end.
        for (int x = SANCTUM_X + 5; x <= SANCTUM_X + 14; x++) {
            for (int z = -6; z <= 6; z++) {
                int step = Math.min(3, Math.min(x - SANCTUM_X - 4, 7 - Math.abs(z)));
                for (int y = FLOOR + 1; y <= FLOOR + step; y++) {
                    if (Math.sqrt((x - SANCTUM_X) * (x - SANCTUM_X) + z * z) < SANCTUM_R - 1.5D) {
                        put(x, y, z, y == FLOOR + step ? (step == 3 ? floor : trim) : panel);
                    }
                }
            }
        }
        // The throne: a high back of ribbed gold with a sunburst, and arms.
        int tx = THRONE.getX();
        for (int y = FLOOR + 4; y <= FLOOR + 9; y++) {
            for (int z = -1; z <= 1; z++) {
                put(tx + 1, y, z, y == FLOOR + 9 ? trim : z == 0 && y == FLOOR + 7 ? EternityBlocks.SOVEREIGN_LIGHT.of(true) : hull(Kind.RIB));
            }
        }
        put(tx + 1, FLOOR + 10, 0, trim);
        put(tx, FLOOR + 4, -1, plate);
        put(tx, FLOOR + 4, 1, plate);
        put(tx, FLOOR + 4, 0, panel);
        // The ring of mirrors, round the floor, each three blocks of dark glass in a gold frame.
        for (int k = 0; k < 12; k++) {
            double a = k * Math.PI / 6D;
            int x = SANCTUM_X + (int) Math.round(Math.cos(a) * (SANCTUM_R - 3)), z = (int) Math.round(Math.sin(a) * (SANCTUM_R - 3));
            if (x > SANCTUM_X + 4 || k == 6) {
                continue; // not in front of the dais, nor across the way in
            }
            put(x, FLOOR + 1, z, trim);
            for (int y = FLOOR + 2; y <= FLOOR + 4; y++) {
                put(x, y, z, hull(Kind.MIRROR));
            }
            put(x, FLOOR + 5, z, trim);
        }
        consoleAt(SANCTUM_X + 4, -5, EnumFacing.NORTH, "sanctum");
    }

    // ------------------------------------------------------------------ the corridors and their doors

    /** A short corridor along the spine, {@code half} out each side and {@code h} high, with an iris door at its east end. */
    private static void corridor(int x0, int x1, int half, int h, String door) {
        IBlockState floor = hull(Kind.FLOOR), rib = hull(Kind.RIB), plate = hull(Kind.PLATE);
        for (int x = x0; x <= x1; x++) {
            for (int z = -half - 1; z <= half + 1; z++) {
                for (int y = FLOOR; y <= FLOOR + h + 1; y++) {
                    int dy = y - FLOOR;
                    if (dy == 0) {
                        put(x, y, z, floor);
                    } else if (Math.abs(z) <= half && dy <= h) {
                        air(x, y, z);
                    } else {
                        wall(x, y, z, x % 3 == 0 ? rib : dy == h + 1 && z == 0 ? EternityBlocks.SOVEREIGN_LIGHT.of(true) : plate);
                    }
                }
            }
        }
        // The door, in the middle of the corridor: a disc of iris, filling it.
        int dx = (x0 + x1) / 2;
        Door d = new Door(door, dx);
        for (int z = -half; z <= half; z++) {
            for (int y = FLOOR + 1; y <= FLOOR + h; y++) {
                put(dx, y, z, EternityBlocks.SOVEREIGN_DOOR.of(false));
                d.blocks.add(new BlockPos(dx, y, z));
            }
        }
        DOORS.put(door, d);
    }

    /** Under the halls, the length of the station: a keel of ribbed gold, and the vanes that hold the halls apart. */
    private static void keel() {
        IBlockState rib = hull(Kind.RIB), dark = hull(Kind.DARK), trim = hull(Kind.TRIM);
        for (int x = 12; x <= SANCTUM_X + SANCTUM_R; x++) {
            for (int y = FLOOR - 6; y <= FLOOR - 2; y++) {
                int w = y == FLOOR - 6 ? 0 : 1;
                for (int z = -w; z <= w; z++) {
                    if (index(x, y, z) >= 0 && !open[index(x, y, z)] && grid[index(x, y, z)] == null) {
                        put(x, y, z, y == FLOOR - 6 ? trim : x % 5 == 0 ? rib : dark);
                    }
                }
            }
            // Running lights along its belly.
            if (x % 10 == 0) {
                put(x, FLOOR - 7, 0, EternityBlocks.SOVEREIGN_LIGHT.of(true));
            }
        }
        // Vanes: thin fins out from the Sanctum, catching the sun.
        for (int k = 0; k < 4; k++) {
            int sz = k < 2 ? 1 : -1;
            int fx = SANCTUM_X + (k % 2 == 0 ? -6 : 6);
            for (int z = SANCTUM_R; z <= SANCTUM_R + 7; z++) {
                for (int y = FLOOR + 2; y <= FLOOR + 2 + (SANCTUM_R + 7 - z); y++) {
                    put(fx, y, sz * z, z == SANCTUM_R + 7 || y == FLOOR + 2 ? trim : rib);
                }
            }
        }
    }

    private static void consoleAt(int x, int z, EnumFacing facing, String record) {
        BlockPos pos = new BlockPos(x, FLOOR + 1, z);
        put(x, FLOOR + 1, z, EternityBlocks.CONSOLE.of(facing, false, false));
        CONSOLES.put(pos, record);
    }

    // ------------------------------------------------------------------ light

    private static byte[] spread(IBlockState[] grid) {
        byte[] lit = new byte[grid.length];
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        for (int i = 0; i < grid.length; i++) {
            int emits = grid[i] == null ? 0 : grid[i].getLightValue();
            if (emits > 0) {
                lit[i] = (byte) emits;
                queue.add(i);
                if (grid[i].getBlock() == EternityBlocks.SOVEREIGN_LIGHT) {
                    int z = i % SZ + Z0, y = (i / SZ) % SY + Y0, x = i / (SZ * SY) + X0;
                    LIGHTS.add(new BlockPos(x, y, z));
                }
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
        return lit;
    }

    private static float noise(int x, int y, int z) {
        long h = x * 374761393L + y * 668265263L + z * 2147483647L;
        h = (h ^ (h >>> 13)) * 1274126177L;
        h ^= h >>> 16;
        return (h & 0xFFFF) / 65535F;
    }
}
