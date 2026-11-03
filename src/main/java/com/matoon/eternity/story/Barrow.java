package com.matoon.eternity.story;

import com.matoon.eternity.block.BlockBarrowSeal;
import com.matoon.eternity.block.BlockRuneStone;
import com.matoon.eternity.block.EternityBlocks;
import com.matoon.eternity.block.TileRuneStone;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFlower;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockOldLog;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.BlockStoneBrick;
import net.minecraft.block.BlockTorch;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Random;

/**
 * The mound outside the village, and the barrow under it, in a frame of its own: x across the
 * door (positive to the right as you face in), y up from the ground at the door's threshold, z
 * in, away from the village.
 * <p>
 * A grassed hill with a door of carved stone in its side; a passage stepping down from it; a long
 * hall with four graves cut into each wall, the dead lying in them and the keepers' stones at the
 * heads of three; a second door, carved with the oath; and past it a round chamber where the
 * cube hangs over an altar in a ring of ward-light. Outside the door, at the side of the path,
 * the village's newer stones.
 */
public final class Barrow {

    /** The door: three blocks wide and high at z = 0; the inner door the same at z = INNER. */
    public static final int INNER = 27;
    /** The floor of the hall, below the threshold. */
    public static final int FLOOR = -9;
    /** The middle of the ward chamber, and the altar in it. */
    public static final int WARD = 32;
    /** The graves along the hall: z of each pair, the deepest first. */
    public static final int[] GRAVE_Z = {24, 20, 16, 12};

    /** Whose grave each is (left of the hall, then right, deepest first); -1 for the keepers' kin. */
    private static final int[] KEEPER_OF = {Stones.ULF, Stones.ASGERD, Stones.BJORN, -1, -1, -1, -1, -1};

    public final BlockPos origin;
    public final EnumFacing in, right;

    public Barrow(BlockPos origin, int facing) {
        this.origin = origin;
        this.in = EnumFacing.byHorizontalIndex(facing);
        this.right = in.rotateY();
    }

    // ------------------------------------------------------------------ the frame

    public BlockPos at(int x, int y, int z) {
        return origin.add(right.getXOffset() * x + in.getXOffset() * z, y, right.getZOffset() * x + in.getZOffset() * z);
    }

    /** The middle of a block of the frame, or any point in it. */
    public Vec3d vec(double x, double y, double z) {
        return new Vec3d(origin.getX() + 0.5D + right.getXOffset() * x + in.getXOffset() * z, origin.getY() + y,
                origin.getZ() + 0.5D + right.getZOffset() * x + in.getZOffset() * z);
    }

    /** A point in the world, back in the barrow's frame (x, y, z). */
    public double[] local(double wx, double wy, double wz) {
        double dx = wx - (origin.getX() + 0.5D), dz = wz - (origin.getZ() + 0.5D);
        return new double[]{dx * right.getXOffset() + dz * right.getZOffset(), wy - origin.getY(), dx * in.getXOffset() + dz * in.getZOffset()};
    }

    /** The yaw of something facing into the barrow. */
    public float yawIn() {
        return in.getHorizontalAngle();
    }

    /** Somewhere in the barrow, under the mound, past the outer door. */
    public boolean inside(double wx, double wy, double wz) {
        double[] l = local(wx, wy, wz);
        return l[2] > 0.6D && l[2] < WARD + 6D && Math.abs(l[0]) < 7D && l[1] < 4D && l[1] > FLOOR - 2D;
    }

    /** In the ward chamber, past the inner door. */
    public boolean inWard(double wx, double wy, double wz) {
        double[] l = local(wx, wy, wz);
        return l[2] > INNER + 0.6D && l[2] < WARD + 6D && Math.abs(l[0]) < 6D && l[1] < 0D && l[1] > FLOOR - 2D;
    }

    public Vec3d doorMiddle() {
        return vec(0D, 1.5D, 0D);
    }

    public Vec3d innerMiddle() {
        return vec(0D, FLOOR + 2.5D, INNER);
    }

    /** Where the cube hangs, over the altar. */
    public Vec3d cube() {
        return vec(0D, FLOOR + 2.4D, WARD);
    }

    public BlockPos altar() {
        return at(0, FLOOR + 1, WARD);
    }

    /** Standing just outside the door, on the path, facing it. */
    public Vec3d step() {
        return vec(0D, 0D, -2.5D);
    }

    /** Where each grave's body lies, and which keeper it is (or -1). */
    public Vec3d grave(int i) {
        int z = GRAVE_Z[i / 2 % GRAVE_Z.length];
        int side = i % 2 == 0 ? -1 : 1;
        return vec(side * 4.5D, FLOOR + 1.05D, z);
    }

    public static int keeperOf(int grave) {
        return grave >= 0 && grave < KEEPER_OF.length ? KEEPER_OF[grave] : -1;
    }

    public static int graves() {
        return KEEPER_OF.length;
    }

    /** Where a keeper's stone stands at the head of their grave, facing into the hall. */
    public BlockPos keeperStone(int grave) {
        int z = GRAVE_Z[grave / 2];
        int side = grave % 2 == 0 ? -1 : 1;
        return at(side * 3, FLOOR + 1, z - 1);
    }

    /** The village's newer stones, at the right of the path outside the door: Ragna's first. */
    public BlockPos village(int i) {
        int[][] spots = {{5, -6}, {7, -6}, {6, -9}, {8, -9}, {4, -9}, {9, -6}};
        return at(spots[i][0], 0, spots[i][1]);
    }

    /** What stands at each of those: Ragna's, Leif's, Gunnhild's, Sigrid's, and two too old to read. */
    public static final int[] VILLAGE_STONES = {Stones.RAGNA, Stones.LEIF, Stones.GUNNHILD, Stones.SIGRID, Stones.WORN, Stones.WORN};

    // ------------------------------------------------------------------ choosing a site

    /**
     * Somewhere for the mound near the village: on open, fairly level ground clear of anything
     * built, with the door facing the village. Tried at several distances, all the way round.
     */
    public static Barrow site(World world, BlockPos village) {
        Barrow best = null;
        double bestScore = Double.MAX_VALUE;
        for (int r = 56; r <= 104; r += 8) {
            for (int a = 0; a < 16; a++) {
                double angle = a * Math.PI / 8D;
                int x = village.getX() + (int) Math.round(Math.cos(angle) * r);
                int z = village.getZ() + (int) Math.round(Math.sin(angle) * r);
                // The door faces the village: in is away from it.
                EnumFacing in = EnumFacing.getFacingFromVector((float) Math.cos(angle), 0F, (float) Math.sin(angle));
                if (in.getAxis() == EnumFacing.Axis.Y) {
                    continue;
                }
                BlockPos ground = surface(world, x, z);
                Barrow b = new Barrow(ground, in.getHorizontalIndex());
                double score = b.unevenness(world);
                if (score < 0D) {
                    continue;
                }
                score += r * 0.02D;
                if (score < bestScore) {
                    bestScore = score;
                    best = b;
                }
            }
            if (best != null && bestScore < 2D) {
                break;
            }
        }
        if (best == null) {
            // Nowhere good: straight out from the village, whatever is there.
            BlockPos ground = surface(world, village.getX() + 48, village.getZ());
            best = new Barrow(ground, EnumFacing.EAST.getHorizontalIndex());
        }
        return best;
    }

    /**
     * How poor a site the mound's footprint is: how uneven, and how wooded (a mound in a forest
     * can't be seen for the trees); or -1 if it would land on anything built, or in water.
     */
    private double unevenness(World world) {
        double sum = 0D;
        int n = 0;
        for (int z = -14; z <= 26; z += 2) {
            for (int x = -12; x <= 12; x += 2) {
                BlockPos p = at(x, 0, z);
                BlockPos top = surface(world, p.getX(), p.getZ());
                IBlockState s = world.getBlockState(top.down());
                if (built(s) || s.getMaterial().isLiquid() || world.getBlockState(top).getMaterial().isLiquid()) {
                    return -1D;
                }
                sum += Math.abs(top.getY() - origin.getY());
                if (world.getHeight(p).getY() > top.getY() + 1) {
                    sum += 1.5D;
                }
                n++;
            }
        }
        return sum / n;
    }

    private static boolean built(IBlockState s) {
        Block b = s.getBlock();
        return b == Blocks.PLANKS || b == Blocks.COBBLESTONE || b == Blocks.STONEBRICK || b == Blocks.GRAVEL || b == Blocks.FARMLAND
                || b == Blocks.OAK_FENCE || b == Blocks.SPRUCE_FENCE || b == Blocks.DARK_OAK_FENCE || b == Blocks.GLASS_PANE
                || b == Blocks.WOOL || b == Blocks.TORCH || b == Blocks.WOODEN_SLAB || b == Blocks.STONE_SLAB || b == Blocks.GRASS_PATH
                || b == Blocks.OAK_STAIRS || b == Blocks.SPRUCE_STAIRS || b == Blocks.STONE_STAIRS;
    }

    /** The first open block above solid ground in a column. */
    public static BlockPos surface(World world, int x, int z) {
        BlockPos p = world.getHeight(new BlockPos(x, 0, z));
        while (p.getY() > 1) {
            IBlockState below = world.getBlockState(p.down());
            Material m = below.getMaterial();
            if (m.blocksMovement() && m != Material.LEAVES && m != Material.WOOD && m != Material.PLANTS && m != Material.VINE) {
                break;
            }
            p = p.down();
        }
        return p;
    }

    /** True if every chunk the barrow and its mound touch is loaded. */
    public boolean loaded(World world) {
        for (int z = -12; z <= WARD + 8; z += 8) {
            for (int x = -12; x <= 12; x += 8) {
                if (!world.isBlockLoaded(at(x, 0, z))) {
                    return false;
                }
            }
        }
        return true;
    }

    // ------------------------------------------------------------------ building it

    public void build(World world, Random rand) {
        mound(world, rand);
        envelope(world, rand);
        passage(world, rand);
        hall(world, rand);
        innerDoor(world, rand);
        ward(world, rand);
        outerDoor(world, rand);
        graveyard(world, rand);
    }

    /** A grassed hill over the door and the first of the hall, rising out of whatever ground is there. */
    private void mound(World world, Random rand) {
        for (int z = -3; z <= 22; z++) {
            for (int x = -12; x <= 12; x++) {
                double dz = (z - 9.5D) / 12D, dx = x / 11.5D;
                double r = dz * dz + dx * dx;
                if (r >= 1D || z <= 0 && Math.abs(x) <= 2) {
                    continue;
                }
                int height = (int) Math.round(7.5D * (1D - r) + MathHelper.sin(x * 1.3F + z * 0.7F) * 0.4D);
                BlockPos col = at(x, 0, z);
                BlockPos top = surface(world, col.getX(), col.getZ());
                int peak = origin.getY() + height;
                for (int y = Math.min(top.getY(), origin.getY() - 1); y <= Math.max(peak, top.getY() + 2); y++) {
                    BlockPos p = new BlockPos(col.getX(), y, col.getZ());
                    if (y < peak) {
                        world.setBlockState(p, y < peak - 3 ? Blocks.STONE.getDefaultState() : Blocks.DIRT.getDefaultState(), 2);
                    } else if (y == peak) {
                        world.setBlockState(p, Blocks.GRASS.getDefaultState(), 2);
                    } else if (y == peak + 1 && rand.nextInt(5) == 0) {
                        world.setBlockState(p, Blocks.TALLGRASS.getStateFromMeta(1), 2);
                    } else {
                        world.setBlockState(p, Blocks.AIR.getDefaultState(), 2);
                    }
                }
                // Trees standing where the mound rises go with the ground they grew from.
                for (int y = Math.max(peak, top.getY() + 2) + 1; y <= peak + 16; y++) {
                    BlockPos p = new BlockPos(col.getX(), y, col.getZ());
                    Material m = world.getBlockState(p).getMaterial();
                    if (m == Material.WOOD || m == Material.LEAVES || m == Material.PLANTS || m == Material.VINE) {
                        world.setBlockState(p, Blocks.AIR.getDefaultState(), 2);
                    }
                }
            }
        }
    }

    /** A shell of stone round everything below, so no cave or sand runs into it. */
    private void envelope(World world, Random rand) {
        for (int z = 0; z <= WARD + 6; z++) {
            for (int x = -7; x <= 7; x++) {
                for (int y = FLOOR - 1; y <= -1; y++) {
                    BlockPos p = at(x, y, z);
                    IBlockState s = world.getBlockState(p);
                    if (s.getMaterial() == Material.AIR || s.getMaterial().isLiquid() || s.getBlock() == Blocks.SAND
                            || s.getBlock() == Blocks.GRAVEL || s.getMaterial() == Material.GRASS || s.getMaterial() == Material.GROUND) {
                        world.setBlockState(p, rough(rand), 2);
                    }
                }
            }
        }
    }

    /** Down from the door: three wide, stepping down a block at a time to the hall. */
    private void passage(World world, Random rand) {
        for (int z = 1; z <= 10; z++) {
            int floor = z <= 1 ? -1 : Math.max(FLOOR, -z);
            for (int x = -2; x <= 2; x++) {
                for (int y = floor; y <= floor + 5; y++) {
                    BlockPos p = at(x, y, z);
                    boolean wall = Math.abs(x) == 2 || y == floor || y == floor + 5;
                    if (wall) {
                        world.setBlockState(p, y == floor ? floor(rand) : y == floor + 5 ? ceiling(rand, z) : wall(rand), 2);
                    } else {
                        world.setBlockState(p, Blocks.AIR.getDefaultState(), 2);
                    }
                }
            }
        }
        // Two torches the village keeps at the head of the stair; below them it is dark.
        torch(world, at(-1, -1, 2), right);
        torch(world, at(1, -1, 2), right.getOpposite());
    }

    private void torch(World world, BlockPos at, EnumFacing facing) {
        world.setBlockState(at, Blocks.TORCH.getDefaultState().withProperty(BlockTorch.FACING, facing), 2);
    }

    /** The hall of the dead: long and low, beams across its roof, four graves cut into each wall. */
    private void hall(World world, Random rand) {
        for (int z = 10; z <= INNER; z++) {
            for (int x = -4; x <= 4; x++) {
                for (int y = FLOOR; y <= FLOOR + 6; y++) {
                    BlockPos p = at(x, y, z);
                    boolean wall = Math.abs(x) == 4 || y == FLOOR || y == FLOOR + 6 || z == INNER;
                    if (wall) {
                        IBlockState s = y == FLOOR ? floor(rand) : y == FLOOR + 6 ? ceiling(rand, z) : wall(rand);
                        world.setBlockState(p, s, 2);
                    } else {
                        world.setBlockState(p, Blocks.AIR.getDefaultState(), 2);
                    }
                }
            }
            if (z % 4 == 2) {
                for (int x = -3; x <= 3; x++) {
                    world.setBlockState(at(x, FLOOR + 5, z), beam(), 2);
                }
            }
        }
        for (int i = 0; i < graves(); i++) {
            int z = GRAVE_Z[i / 2];
            int side = i % 2 == 0 ? -1 : 1;
            for (int dz = -1; dz <= 1; dz++) {
                for (int dx = 4; dx <= 5; dx++) {
                    for (int y = FLOOR + 1; y <= FLOOR + 2; y++) {
                        world.setBlockState(at(side * dx, y, z + dz), Blocks.AIR.getDefaultState(), 2);
                    }
                    world.setBlockState(at(side * dx, FLOOR, z + dz), Blocks.COBBLESTONE.getDefaultState(), 2);
                    world.setBlockState(at(side * dx, FLOOR + 3, z + dz), Blocks.STONE_SLAB.getStateFromMeta(5 | 8), 2);
                }
                world.setBlockState(at(side * 6, FLOOR + 1, z + dz), wall(rand), 2);
                world.setBlockState(at(side * 6, FLOOR + 2, z + dz), wall(rand), 2);
            }
            int keeper = keeperOf(i);
            if (keeper >= 0) {
                stone(world, keeperStone(i), side < 0 ? right : right.getOpposite(), keeper, false);
            }
        }
    }

    /** The oath door: three by three, set in a carved frame, the hall's end wall all round it. */
    private void innerDoor(World world, Random rand) {
        for (int x = -2; x <= 2; x++) {
            for (int y = FLOOR + 1; y <= FLOOR + 4; y++) {
                boolean frame = Math.abs(x) == 2 || y == FLOOR + 4;
                world.setBlockState(at(x, y, INNER), frame ? Blocks.STONEBRICK.getDefaultState()
                        .withProperty(BlockStoneBrick.VARIANT, BlockStoneBrick.EnumType.CHISELED) : seal(false), 2);
            }
        }
    }

    /** The ward chamber: round, domed, the altar in the middle and a ring of stone posts round it. */
    private void ward(World world, Random rand) {
        for (int z = INNER + 1; z <= WARD + 6; z++) {
            for (int x = -6; x <= 6; x++) {
                double r = Math.sqrt(x * x + (z - WARD) * (z - WARD));
                for (int y = FLOOR; y <= FLOOR + 7; y++) {
                    BlockPos p = at(x, y, z);
                    double dome = FLOOR + 1 + 5.5D * Math.sqrt(Math.max(0D, 1D - (r / 5.3D) * (r / 5.3D)));
                    boolean open = r < 5.0D && y > FLOOR && y < dome;
                    if (open) {
                        world.setBlockState(p, Blocks.AIR.getDefaultState(), 2);
                    } else if (r < 6.2D || z == INNER + 1 && Math.abs(x) <= 1) {
                        world.setBlockState(p, y == FLOOR ? Blocks.STONEBRICK.getDefaultState() : wall(rand), 2);
                    }
                }
            }
        }
        // The way through from the inner door.
        for (int x = -1; x <= 1; x++) {
            for (int y = FLOOR + 1; y <= FLOOR + 3; y++) {
                world.setBlockState(at(x, y, INNER + 1), Blocks.AIR.getDefaultState(), 2);
            }
        }
        world.setBlockState(altar(), Blocks.STONEBRICK.getDefaultState().withProperty(BlockStoneBrick.VARIANT, BlockStoneBrick.EnumType.CHISELED), 2);
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4D + Math.PI / 8D;
            int x = (int) Math.round(Math.cos(a) * 3.2D), z = WARD + (int) Math.round(Math.sin(a) * 3.2D);
            world.setBlockState(at(x, FLOOR + 1, z), Blocks.COBBLESTONE_WALL.getStateFromMeta(1), 2);
        }
    }

    /** The outer door: the frame of great stones, and the seal in it. */
    private void outerDoor(World world, Random rand) {
        for (int x = -2; x <= 2; x++) {
            for (int y = -1; y <= 4; y++) {
                boolean frame = Math.abs(x) == 2 || y >= 3 || y == -1;
                if (frame) {
                    world.setBlockState(at(x, y, 0), y == 3 && Math.abs(x) < 2 ? Blocks.STONEBRICK.getDefaultState()
                            .withProperty(BlockStoneBrick.VARIANT, BlockStoneBrick.EnumType.CHISELED) : Blocks.STONEBRICK.getDefaultState(), 2);
                } else {
                    world.setBlockState(at(x, y, 0), seal(false), 2);
                }
            }
            world.setBlockState(at(x, 4, 1), Blocks.STONEBRICK.getDefaultState(), 2);
        }
        // A path worn up to it, and nothing growing across it.
        for (int z = -9; z <= -1; z++) {
            for (int x = -1; x <= 1; x++) {
                BlockPos col = at(x, 0, z);
                BlockPos top = surface(world, col.getX(), col.getZ());
                world.setBlockState(top.down(), (x + z) % 3 == 0 ? Blocks.GRAVEL.getDefaultState() : Blocks.DIRT.getStateFromMeta(1), 2);
                for (int y = 0; y < 3; y++) {
                    if (!world.isAirBlock(top.up(y)) && world.getBlockState(top.up(y)).getMaterial().isReplaceable()) {
                        world.setBlockToAir(top.up(y));
                    }
                }
            }
        }
    }

    /** The village's newer stones beside the path, and flowers left at Ragna's. */
    private void graveyard(World world, Random rand) {
        for (int i = 0; i < VILLAGE_STONES.length; i++) {
            BlockPos spot = village(i);
            BlockPos top = surface(world, spot.getX(), spot.getZ());
            int stone = VILLAGE_STONES[i];
            stone(world, top, right.getOpposite(), stone, Stones.fresh(stone));
            if (stone == Stones.GUNNHILD) {
                // Hers is a real grave: she died at home.
                BlockPos before = top.offset(right.getOpposite());
                world.setBlockState(before.down(), Blocks.DIRT.getStateFromMeta(1), 2);
            }
            if (stone == Stones.RAGNA) {
                BlockPos before = top.offset(right.getOpposite());
                world.setBlockState(before, Blocks.RED_FLOWER.getDefaultState().withProperty(Blocks.RED_FLOWER.getTypeProperty(),
                        BlockFlower.EnumFlowerType.BLUE_ORCHID), 2);
            }
        }
    }

    public static void stone(World world, BlockPos at, EnumFacing facing, int stone, boolean fresh) {
        world.setBlockState(at, EternityBlocks.RUNE_STONE.getDefaultState().withProperty(BlockHorizontal.FACING, facing)
                .withProperty(BlockRuneStone.FRESH, fresh), 2);
        TileEntity te = world.getTileEntity(at);
        if (te instanceof TileRuneStone) {
            ((TileRuneStone) te).stone = stone;
            te.markDirty();
        }
    }

    // ------------------------------------------------------------------ the doors, later

    public static IBlockState seal(boolean lit) {
        return EternityBlocks.BARROW_SEAL.getDefaultState().withProperty(BlockBarrowSeal.LIT, lit);
    }

    /** Every block of the outer door's seal. */
    public BlockPos[] outerSeal() {
        BlockPos[] s = new BlockPos[9];
        int i = 0;
        for (int y = 2; y >= 0; y--) {
            for (int x = -1; x <= 1; x++) {
                s[i++] = at(x, y, 0);
            }
        }
        return s;
    }

    /** Every block of the inner door's seal, bottom row first. */
    public BlockPos[] innerSeal() {
        BlockPos[] s = new BlockPos[9];
        int i = 0;
        for (int y = FLOOR + 1; y <= FLOOR + 3; y++) {
            for (int x = -1; x <= 1; x++) {
                s[i++] = at(x, y, INNER);
            }
        }
        return s;
    }

    public boolean isOuterSeal(BlockPos p) {
        double[] l = local(p.getX() + 0.5D, p.getY(), p.getZ() + 0.5D);
        return Math.abs(l[2]) < 0.6D && Math.abs(l[0]) < 1.6D && l[1] >= 0 && l[1] < 3;
    }

    public boolean isInnerSeal(BlockPos p) {
        double[] l = local(p.getX() + 0.5D, p.getY(), p.getZ() + 0.5D);
        return Math.abs(l[2] - INNER) < 0.6D && Math.abs(l[0]) < 1.6D && l[1] > FLOOR && l[1] < FLOOR + 4;
    }

    // ------------------------------------------------------------------ materials

    private static IBlockState wall(Random rand) {
        int r = rand.nextInt(10);
        if (r < 4) {
            return Blocks.STONEBRICK.getDefaultState();
        }
        if (r < 6) {
            return Blocks.STONEBRICK.getDefaultState().withProperty(BlockStoneBrick.VARIANT, BlockStoneBrick.EnumType.MOSSY);
        }
        if (r < 8) {
            return Blocks.STONEBRICK.getDefaultState().withProperty(BlockStoneBrick.VARIANT, BlockStoneBrick.EnumType.CRACKED);
        }
        return r < 9 ? Blocks.MOSSY_COBBLESTONE.getDefaultState() : Blocks.COBBLESTONE.getDefaultState();
    }

    private static IBlockState rough(Random rand) {
        return rand.nextInt(4) == 0 ? Blocks.COBBLESTONE.getDefaultState() : Blocks.STONE.getDefaultState();
    }

    private static IBlockState floor(Random rand) {
        int r = rand.nextInt(6);
        return r < 3 ? Blocks.STONEBRICK.getDefaultState() : r < 5 ? Blocks.DIRT.getStateFromMeta(1)
                : Blocks.STONEBRICK.getDefaultState().withProperty(BlockStoneBrick.VARIANT, BlockStoneBrick.EnumType.CRACKED);
    }

    private static IBlockState ceiling(Random rand, int z) {
        return rand.nextInt(3) == 0 ? Blocks.COBBLESTONE.getDefaultState() : Blocks.STONEBRICK.getDefaultState();
    }

    private IBlockState beam() {
        BlockLog.EnumAxis axis = right.getAxis() == EnumFacing.Axis.X ? BlockLog.EnumAxis.X : BlockLog.EnumAxis.Z;
        return Blocks.LOG.getDefaultState().withProperty(BlockOldLog.VARIANT, BlockPlanks.EnumType.SPRUCE).withProperty(BlockLog.LOG_AXIS, axis);
    }
}
