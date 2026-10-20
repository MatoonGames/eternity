package com.matoon.eternity.world;

import com.matoon.eternity.EternityConfig;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Arrays;

/** Block destruction for Eternity's attacks. */
public final class Terrain {

    /** Largest radius the precomputed sphere covers. */
    public static final int MAX_RADIUS = 64;

    private static int[] sphere;

    private Terrain() {
    }

    public static boolean enabled() {
        return EternityConfig.combat.destroyTerrain;
    }

    /** Removes one block if it can be broken at all. Returns the removed state, or null. */
    public static IBlockState erase(World world, BlockPos pos) {
        if (!world.isBlockLoaded(pos)) {
            return null;
        }
        IBlockState state = world.getBlockState(pos);
        if (state.getMaterial() == Material.AIR || state.getBlockHardness(world, pos) < 0F) {
            return null;
        }
        world.setBlockState(pos, Blocks.AIR.getDefaultState(), 2);
        return state;
    }

    /** Removes anything at all, bedrock included. Only the end of the world does this. */
    public static boolean eraseAbsolutely(World world, BlockPos pos) {
        if (!world.isBlockLoaded(pos) || world.isAirBlock(pos)) {
            return false;
        }
        world.setBlockState(pos, Blocks.AIR.getDefaultState(), 2);
        return true;
    }

    /** Carves a tube along a segment, like a lance burning through the world. */
    public static void carveLine(World world, Vec3d from, Vec3d to, double radius, int budget) {
        if (!enabled()) {
            return;
        }
        Vec3d dir = to.subtract(from);
        double length = dir.length();
        if (length < 1.0E-3) {
            return;
        }
        dir = dir.scale(1D / length);
        int r = (int) Math.ceil(radius);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (double t = 0; t <= length && budget > 0; t += 0.8D) {
            Vec3d p = from.add(dir.scale(t));
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (dx * dx + dy * dy + dz * dz > radius * radius) {
                            continue;
                        }
                        pos.setPos(p.x + dx, p.y + dy, p.z + dz);
                        if (erase(world, pos) != null) {
                            budget--;
                        }
                    }
                }
            }
        }
    }

    /**
     * Offsets of every block within {@link #MAX_RADIUS}, nearest first, packed as
     * (x+64)<<16 | (y+64)<<8 | (z+64). Built once, on the first black hole.
     */
    public static int[] sphere() {
        if (sphere == null) {
            int r = MAX_RADIUS;
            long[] keyed = new long[(int) (4.2D * r * r * r) + 1];
            int n = 0;
            for (int x = -r; x <= r; x++) {
                for (int y = -r; y <= r; y++) {
                    for (int z = -r; z <= r; z++) {
                        int d2 = x * x + y * y + z * z;
                        if (d2 <= r * r) {
                            keyed[n++] = ((long) d2 << 24) | pack(x, y, z);
                        }
                    }
                }
            }
            Arrays.sort(keyed, 0, n);
            int[] packed = new int[n];
            for (int i = 0; i < n; i++) {
                packed[i] = (int) (keyed[i] & 0xFFFFFF);
            }
            sphere = packed;
        }
        return sphere;
    }

    public static int pack(int x, int y, int z) {
        return ((x + 64) << 16) | ((y + 64) << 8) | (z + 64);
    }

    public static int unpackX(int p) {
        return ((p >> 16) & 0xFF) - 64;
    }

    public static int unpackY(int p) {
        return ((p >> 8) & 0xFF) - 64;
    }

    public static int unpackZ(int p) {
        return (p & 0xFF) - 64;
    }

    /** Finds standing room near a point: solid ground below, two blocks of air. */
    public static double safeY(World world, double x, double y, double z) {
        int top = world.provider.isNether() ? 120 : 250;
        int start = (int) Math.min(Math.max(y, 2), top);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dy = 0; dy < 96; dy++) {
            int[] candidates = {start - dy, start + dy};
            for (int cy : candidates) {
                if (cy < 1 || cy > top) {
                    continue;
                }
                pos.setPos(x, cy, z);
                if (standable(world, pos)) {
                    return cy;
                }
            }
        }
        return y;
    }

    private static boolean standable(World world, BlockPos.MutableBlockPos pos) {
        BlockPos below = pos.down();
        return world.getBlockState(below).getMaterial().blocksMovement()
                && !world.getBlockState(pos).getMaterial().blocksMovement()
                && !world.getBlockState(pos.up()).getMaterial().blocksMovement()
                && !world.getBlockState(pos).getMaterial().isLiquid();
    }
}
