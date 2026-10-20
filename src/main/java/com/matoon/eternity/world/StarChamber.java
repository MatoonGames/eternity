package com.matoon.eternity.world;

import com.matoon.eternity.EternityConfig;
import net.minecraft.block.BlockStainedGlass;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.world.DimensionType;
import net.minecraftforge.common.DimensionManager;

import javax.annotation.Nullable;

/**
 * The Star Chamber: an abstract courtroom outside every universe, where the Living
 * Tribunal sits in judgement. A floor of obsidian veined with light, an aisle of
 * glowing glass leading to the defendant's golden dais, pillars of white stone, and
 * walls nobody can pass. The Tribunal and his throne are not blocks; the client
 * draws them, seated at {@link #THRONE_Z}, looking down the aisle at the dais.
 */
public final class StarChamber {

    public static final int FLOOR_Y = 64;
    /** Where the defendant stands. */
    public static final double DAIS_X = 0.5D, DAIS_Y = 66D, DAIS_Z = 30.5D;
    /** Where the Tribunal sits, his feet on the floor. */
    public static final double THRONE_X = 0.5D, THRONE_Y = 65D, THRONE_Z = -45.5D;

    private static final int MIN_X = -30, MAX_X = 30, MIN_Z = -80, MAX_Z = 44, TOP_Y = 140;

    private static final IBlockState OBSIDIAN = Blocks.OBSIDIAN.getDefaultState();
    private static final IBlockState BLACK_GLASS = Blocks.STAINED_GLASS.getDefaultState().withProperty(BlockStainedGlass.COLOR, EnumDyeColor.BLACK);
    private static final IBlockState PURPLE_GLASS = Blocks.STAINED_GLASS.getDefaultState().withProperty(BlockStainedGlass.COLOR, EnumDyeColor.PURPLE);
    private static final IBlockState LANTERN = Blocks.SEA_LANTERN.getDefaultState();
    private static final IBlockState GLOW = Blocks.GLOWSTONE.getDefaultState();
    private static final IBlockState GOLD = Blocks.GOLD_BLOCK.getDefaultState();
    private static final IBlockState QUARTZ = Blocks.QUARTZ_BLOCK.getDefaultState();
    private static final IBlockState BARRIER = Blocks.BARRIER.getDefaultState();

    private static DimensionType type;

    private StarChamber() {
    }

    public static void register() {
        int id = EternityConfig.tribunal.chamberDimension;
        type = DimensionType.register("star_chamber", "_star_chamber", id, WorldProviderStarChamber.class, false);
        DimensionManager.registerDimension(id, type);
    }

    public static DimensionType type() {
        return type;
    }

    public static int dimension() {
        return EternityConfig.tribunal.chamberDimension;
    }

    /** Whether a point is inside the walls. */
    public static boolean inside(double x, double y, double z) {
        return x > MIN_X + 1 && x < MAX_X && z > MIN_Z + 1 && z < MAX_Z && y > FLOOR_Y && y < TOP_Y;
    }

    /** The block at a world position, or null for the void. */
    @Nullable
    public static IBlockState blockAt(int x, int y, int z) {
        boolean withinX = x >= MIN_X && x <= MAX_X;
        boolean withinZ = z >= MIN_Z && z <= MAX_Z;
        if (withinX && withinZ && y >= FLOOR_Y + 1 && y <= TOP_Y) {
            if (x == MIN_X || x == MAX_X || z == MIN_Z || z == MAX_Z || y == TOP_Y) {
                return BARRIER;
            }
        }
        if (!withinX || !withinZ) {
            return debris(x, y, z);
        }
        boolean aisle = Math.abs(x) <= 2 && z > -38;
        boolean vein = Math.floorMod(x, 8) == 0 || Math.floorMod(z, 8) == 0;
        if (y == FLOOR_Y) {
            return aisle ? BLACK_GLASS : vein ? PURPLE_GLASS : OBSIDIAN;
        }
        if (y == FLOOR_Y - 1) {
            return aisle ? LANTERN : vein ? GLOW : OBSIDIAN;
        }
        if (y == FLOOR_Y + 1) {
            double dx = x - (DAIS_X - 0.5D);
            double dz = z - (DAIS_Z - 0.5D);
            if (dx * dx + dz * dz <= 10D) {
                return GOLD;
            }
        }
        // Two rows of pillars down the hall, each a different height.
        if (y > FLOOR_Y && (x == -22 || x == -21 || x == 21 || x == 22)) {
            int row = Math.floorMod(z + 64, 16);
            if (z >= -64 && z <= 40 && (row == 0 || row == 1)) {
                int height = 18 + Math.floorMod((z + 64) / 16 * 7, 13);
                if (y < FLOOR_Y + height) {
                    return QUARTZ;
                }
                if (y == FLOOR_Y + height) {
                    return GLOW;
                }
            }
        }
        return null;
    }

    /** Shards of black drifting in the void beyond the walls. */
    @Nullable
    private static IBlockState debris(int x, int y, int z) {
        if (y < 40 || y > 200 || Math.abs(x) > 160 || z < -220 || z > 160) {
            return null;
        }
        long h = x * 73856093L ^ y * 19349663L ^ z * 83492791L;
        h ^= h >>> 13;
        h *= 0x5bd1e995L;
        h ^= h >>> 15;
        return (h & 1023) == 0 ? OBSIDIAN : null;
    }
}
