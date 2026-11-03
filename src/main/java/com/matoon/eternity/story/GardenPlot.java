package com.matoon.eternity.story;

import com.matoon.eternity.block.BlockStrangeBloom;
import com.matoon.eternity.block.EternityBlocks;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.BlockFarmland;
import net.minecraft.block.BlockFlower;
import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenBigTree;
import net.minecraft.world.gen.feature.WorldGenBirchTree;
import net.minecraft.world.gen.feature.WorldGenTrees;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The Gardener's garden, laid out on the red plain. A lawn round as a pond with its edge gone
 * ragged where the red comes back; a real pond off the middle, lilies on it, reeds at its lip;
 * beds of grain and roots in tidy rows, because he is a gardener and not a wizard; flowers in
 * rows between them; old trees round the rim that nothing on this planet could have grown; a
 * bench to sit on, facing the water; and among it all the plants that don't exist anywhere.
 * <p>
 * Everything is placed relative to the middle, in a frame turned to {@link #facing}, so the
 * garden can be laid out, hedged, ruined and healed again by the same numbers.
 */
final class GardenPlot {

    /** The lawn's radius; the hedge stands just outside it. */
    static final int RADIUS = 18, HEDGE = RADIUS + 1, HEDGE_HEIGHT = 5;

    final BlockPos middle;
    final EnumFacing facing;

    GardenPlot(BlockPos middle, EnumFacing facing) {
        this.middle = middle;
        this.facing = facing;
    }

    /** A point in the garden's own frame: x across, z forward from the middle. */
    BlockPos at(int x, int y, int z) {
        EnumFacing right = facing.rotateY();
        return middle.add(facing.getXOffset() * z + right.getXOffset() * x, y, facing.getZOffset() * z + right.getZOffset() * x);
    }

    /** Where the lawn's ragged edge is, round the compass: the radius plus or minus a little. */
    static double edge(double angle, long seed) {
        Random r = new Random(seed);
        double a = r.nextDouble() * 6.28D, b = r.nextDouble() * 6.28D;
        return RADIUS - 0.5D + Math.sin(angle * 3D + a) * 1.2D + Math.sin(angle * 7D + b) * 0.7D;
    }

    boolean inside(double x, double z) {
        double dx = x - middle.getX() - 0.5D, dz = z - middle.getZ() - 0.5D;
        return dx * dx + dz * dz < (RADIUS + 0.5D) * (RADIUS + 0.5D);
    }

    double distance(double x, double z) {
        double dx = x - middle.getX() - 0.5D, dz = z - middle.getZ() - 0.5D;
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** True if every chunk the garden and its hedge touch is loaded. */
    boolean loaded(World world) {
        for (int x = -HEDGE - 2; x <= HEDGE + 2; x += 8) {
            for (int z = -HEDGE - 2; z <= HEDGE + 2; z += 8) {
                if (!world.isBlockLoaded(middle.add(x, 0, z))) {
                    return false;
                }
            }
        }
        return true;
    }

    // ------------------------------------------------------------------ laying it out

    /** Where the pond is, in the garden's frame, and how big. */
    private static final int POND_X = 5, POND_Z = -4;
    private static final double POND_A = 4.2D, POND_B = 3.2D;

    private boolean pond(int x, int z) {
        double u = (x - POND_X) / POND_A, v = (z - POND_Z) / POND_B;
        return u * u + v * v < 1D;
    }

    private boolean pondLip(int x, int z) {
        double u = (x - POND_X) / (POND_A + 1.2D), v = (z - POND_Z) / (POND_B + 1.2D);
        return u * u + v * v < 1D && !pond(x, z);
    }

    /** The beds, in the garden's frame: x0, z0, x1, z1, and what grows in each. */
    private static final int[][] BEDS = {{-12, -3, -6, 3, 0}, {-12, 6, -6, 11, 1}, {-3, 7, 3, 12, 2}, {6, 6, 11, 10, 3}};

    /** Where the old trees stand round the rim, in the garden's frame, and what each is. */
    private static final int[][] TREES = {{-13, -11, 0}, {0, -15, 1}, {13, -10, 2}, {15, 3, 0}, {-12, 12, 2}, {9, 14, 1}};

    /** Where he works: kneeling at the near edge of the first bed, facing into it. */
    BlockPos workPlace() {
        return at(-4, 1, 0);
    }

    BlockPos bench() {
        return at(POND_X - 1, 1, POND_Z + 5);
    }

    void build(World world, long seed) {
        Random rand = new Random(seed);
        int y0 = middle.getY();
        // The ground: cut down or built up to one level, grass over dirt, the edge ragged.
        for (int x = -HEDGE - 2; x <= HEDGE + 2; x++) {
            for (int z = -HEDGE - 2; z <= HEDGE + 2; z++) {
                double d = Math.sqrt(x * x + z * z);
                double angle = Math.atan2(z, x);
                double edge = edge(angle, seed);
                BlockPos col = middle.add(x, 0, z);
                if (d > edge + 3D) {
                    continue;
                }
                boolean lawn = d <= edge;
                // Clear above the garden's level, a little higher near the middle.
                for (int y = 1; y <= 8; y++) {
                    BlockPos p = col.up(y);
                    if (!world.isAirBlock(p)) {
                        world.setBlockState(p, Blocks.AIR.getDefaultState(), 2);
                    }
                }
                if (lawn) {
                    for (int y = -4; y < 0; y++) {
                        world.setBlockState(col.up(y), Blocks.DIRT.getDefaultState(), 2);
                    }
                    world.setBlockState(col, Blocks.GRASS.getDefaultState(), 2);
                } else if (rand.nextDouble() < 1D - (d - edge) / 3D) {
                    // The fringe, where the red comes back through: coarse earth, podzol, a little grass.
                    world.setBlockState(col, rand.nextInt(3) == 0 ? Blocks.DIRT.getStateFromMeta(2) : Blocks.DIRT.getStateFromMeta(1), 2);
                    world.setBlockState(col.down(), Blocks.DIRT.getDefaultState(), 2);
                }
            }
        }
        // The pond, its clay bed, lilies, reeds at the lip.
        for (int x = -10; x <= 14; x++) {
            for (int z = -12; z <= 4; z++) {
                BlockPos p = at(x, 0, z);
                if (pond(x, z)) {
                    double u = (x - POND_X) / POND_A, v = (z - POND_Z) / POND_B;
                    int depth = u * u + v * v < 0.4D ? 3 : 2;
                    for (int y = 0; y > -depth; y--) {
                        world.setBlockState(p.up(y), Blocks.WATER.getDefaultState(), 2);
                    }
                    world.setBlockState(p.up(-depth), Blocks.CLAY.getDefaultState(), 2);
                    if (rand.nextInt(6) == 0) {
                        world.setBlockState(p.up(), Blocks.WATERLILY.getDefaultState(), 2);
                    }
                } else if (pondLip(x, z) && rand.nextInt(3) == 0) {
                    int h = 1 + rand.nextInt(3);
                    world.setBlockState(p, Blocks.SAND.getDefaultState(), 2);
                    for (int y = 1; y <= h; y++) {
                        world.setBlockState(p.up(y), Blocks.REEDS.getDefaultState(), 2);
                    }
                }
            }
        }
        // The beds: farmland, watered by a channel down the middle, edged in logs.
        IBlockState[] crops = {Blocks.WHEAT.getDefaultState().withProperty(BlockCrops.AGE, 7),
                Blocks.CARROTS.getDefaultState().withProperty(BlockCrops.AGE, 7),
                Blocks.POTATOES.getDefaultState().withProperty(BlockCrops.AGE, 7),
                Blocks.BEETROOTS.getStateFromMeta(3)};
        for (int[] bed : BEDS) {
            int midX = (bed[0] + bed[2]) / 2;
            for (int x = bed[0] - 1; x <= bed[2] + 1; x++) {
                for (int z = bed[1] - 1; z <= bed[3] + 1; z++) {
                    BlockPos p = at(x, 0, z);
                    boolean rim = x == bed[0] - 1 || x == bed[2] + 1 || z == bed[1] - 1 || z == bed[3] + 1;
                    if (rim) {
                        boolean along = z == bed[1] - 1 || z == bed[3] + 1;
                        EnumFacing.Axis axis = along ? facing.rotateY().getAxis() : facing.getAxis();
                        world.setBlockState(p, Blocks.LOG.getDefaultState().withProperty(BlockLog.LOG_AXIS,
                                axis == EnumFacing.Axis.X ? BlockLog.EnumAxis.X : BlockLog.EnumAxis.Z), 2);
                    } else if (x == midX) {
                        world.setBlockState(p, Blocks.WATER.getDefaultState(), 2);
                        world.setBlockState(p.down(), Blocks.CLAY.getDefaultState(), 2);
                    } else {
                        world.setBlockState(p, Blocks.FARMLAND.getDefaultState().withProperty(BlockFarmland.MOISTURE, 7), 2);
                        world.setBlockState(p.up(), crops[bed[4]], 2);
                    }
                }
            }
        }
        // Flowers in rows between the beds and along the paths: colour by colour, the way he likes them.
        IBlockState[] rows = {
                Blocks.RED_FLOWER.getDefaultState().withProperty(Blocks.RED_FLOWER.getTypeProperty(), BlockFlower.EnumFlowerType.ORANGE_TULIP),
                Blocks.RED_FLOWER.getDefaultState().withProperty(Blocks.RED_FLOWER.getTypeProperty(), BlockFlower.EnumFlowerType.BLUE_ORCHID),
                Blocks.RED_FLOWER.getDefaultState().withProperty(Blocks.RED_FLOWER.getTypeProperty(), BlockFlower.EnumFlowerType.OXEYE_DAISY),
                Blocks.RED_FLOWER.getDefaultState().withProperty(Blocks.RED_FLOWER.getTypeProperty(), BlockFlower.EnumFlowerType.ALLIUM),
                Blocks.YELLOW_FLOWER.getDefaultState(),
                Blocks.RED_FLOWER.getDefaultState().withProperty(Blocks.RED_FLOWER.getTypeProperty(), BlockFlower.EnumFlowerType.PINK_TULIP)};
        for (int i = 0; i < 6; i++) {
            int z = -12 + i * 2;
            for (int x = -4; x <= 1; x++) {
                place(world, at(x, 1, z), rows[i]);
            }
        }
        for (int x = -12; x <= -6; x++) {
            place(world, at(x, 1, 4), rows[(x + 12) % rows.length]);
        }
        // Tall flowers by the bench.
        BlockDoublePlant.EnumPlantType[] tall = {BlockDoublePlant.EnumPlantType.ROSE, BlockDoublePlant.EnumPlantType.SYRINGA,
                BlockDoublePlant.EnumPlantType.PAEONIA, BlockDoublePlant.EnumPlantType.SUNFLOWER};
        for (int i = 0; i < tall.length; i++) {
            BlockPos p = at(POND_X - 4 + i * 2, 1, POND_Z + 7);
            if (world.isAirBlock(p) && world.isAirBlock(p.up())) {
                Blocks.DOUBLE_PLANT.placeAt(world, p, tall[i], 2);
            }
        }
        // The bench: two stairs facing the water, and a lantern on a post at its end.
        BlockPos b = bench();
        for (int i = 0; i < 2; i++) {
            BlockPos s = b.offset(facing.rotateY(), i);
            world.setBlockState(s, Blocks.OAK_STAIRS.getDefaultState().withProperty(BlockStairs.FACING, facing), 2);
        }
        BlockPos post = b.offset(facing.rotateY(), 2);
        world.setBlockState(post, Blocks.OAK_FENCE.getDefaultState(), 2);
        world.setBlockState(post.up(), Blocks.GLOWSTONE.getDefaultState(), 2);
        // Paths of trodden earth: one in from each side to the middle, one round the pond.
        for (int z = -RADIUS + 1; z <= RADIUS - 1; z++) {
            path(world, at(0, 0, z));
        }
        for (int x = -RADIUS + 1; x <= RADIUS - 1; x++) {
            if (Math.abs(x - POND_X) > 6) {
                path(world, at(x, 0, 5));
            }
        }
        // The old trees round the rim.
        for (int[] t : TREES) {
            BlockPos p = at(t[0], 1, t[1]);
            world.setBlockState(p.down(), Blocks.GRASS.getDefaultState(), 2);
            switch (t[2]) {
                case 0:
                    new WorldGenBigTree(false).generate(world, rand, p);
                    break;
                case 1:
                    new WorldGenBirchTree(false, true).generate(world, rand, p);
                    break;
                default:
                    new WorldGenTrees(false, 6, Blocks.LOG.getDefaultState().withProperty(net.minecraft.block.BlockOldLog.VARIANT,
                            BlockPlanks.EnumType.JUNGLE), Blocks.LEAVES.getDefaultState().withProperty(net.minecraft.block.BlockOldLeaf.VARIANT,
                            BlockPlanks.EnumType.JUNGLE).withProperty(net.minecraft.block.BlockLeaves.CHECK_DECAY, false), true).generate(world, rand, p);
                    break;
            }
        }
        // Grass and ferns in the gaps; and the plants that don't exist, here and there among the rest.
        for (int i = 0; i < 260; i++) {
            int x = rand.nextInt(RADIUS * 2) - RADIUS, z = rand.nextInt(RADIUS * 2) - RADIUS;
            if (x * x + z * z > (RADIUS - 2) * (RADIUS - 2)) {
                continue;
            }
            BlockPos p = at(x, 1, z);
            if (world.getBlockState(p.down()).getBlock() != Blocks.GRASS || !world.isAirBlock(p)) {
                continue;
            }
            int k = rand.nextInt(10);
            if (k < 2) {
                world.setBlockState(p, EternityBlocks.STRANGE_BLOOM.getDefaultState().withProperty(BlockStrangeBloom.KIND, rand.nextInt(4)), 2);
            } else if (k < 7) {
                world.setBlockState(p, Blocks.TALLGRASS.getStateFromMeta(rand.nextInt(4) == 0 ? 2 : 1), 2);
            }
        }
    }

    private static void place(World world, BlockPos p, IBlockState flower) {
        if (world.isAirBlock(p) && world.getBlockState(p.down()).getBlock() == Blocks.GRASS) {
            world.setBlockState(p, flower, 2);
        }
    }

    private static void path(World world, BlockPos p) {
        if (world.getBlockState(p).getBlock() == Blocks.GRASS && world.isAirBlock(p.up())) {
            world.setBlockState(p, Blocks.GRASS_PATH.getDefaultState(), 2);
        }
    }

    // ------------------------------------------------------------------ the hedge

    /** Every block of the hedge: a ring two thick and five high just outside the lawn. */
    List<BlockPos> hedge() {
        List<BlockPos> out = new ArrayList<>();
        for (int x = -HEDGE - 2; x <= HEDGE + 2; x++) {
            for (int z = -HEDGE - 2; z <= HEDGE + 2; z++) {
                double d = Math.sqrt(x * x + z * z);
                if (d >= HEDGE - 0.5D && d < HEDGE + 1.5D) {
                    for (int y = 1; y <= HEDGE_HEIGHT; y++) {
                        out.add(middle.add(x, y, z));
                    }
                }
            }
        }
        return out;
    }

    void raiseHedge(World world) {
        for (BlockPos p : hedge()) {
            IBlockState s = world.getBlockState(p);
            if (s.getBlock().isReplaceable(world, p) || s.getMaterial() == Material.LEAVES || s.getMaterial() == Material.PLANTS) {
                world.setBlockState(p, EternityBlocks.GARDEN_THORN.getDefaultState(), 2);
            }
        }
    }

    void lowerHedge(World world) {
        for (BlockPos p : hedge()) {
            if (world.getBlockState(p).getBlock() == EternityBlocks.GARDEN_THORN || world.getBlockState(p).getBlock() == EternityBlocks.THORN_HUSK) {
                world.setBlockState(p, Blocks.AIR.getDefaultState(), 2);
            }
        }
    }

    /** Some of what grows within a radius of a point dies: grain and flowers to dead stalks, grass to bare earth. */
    static int ruin(World world, BlockPos around, int radius, int count, Random rand) {
        int done = 0;
        for (int i = 0; i < count * 4 && done < count; i++) {
            BlockPos p = around.add(rand.nextInt(radius * 2 + 1) - radius, rand.nextInt(5) - 2, rand.nextInt(radius * 2 + 1) - radius);
            IBlockState s = world.getBlockState(p);
            if (s.getBlock() instanceof BlockCrops || s.getBlock() instanceof BlockFlower || s.getBlock() == Blocks.TALLGRASS
                    || s.getBlock() == EternityBlocks.STRANGE_BLOOM) {
                world.setBlockState(p, Blocks.DEADBUSH.getDefaultState(), 2);
                done++;
            } else if (s.getBlock() == Blocks.DOUBLE_PLANT) {
                world.setBlockState(p, Blocks.AIR.getDefaultState(), 2);
                done++;
            } else if (s.getBlock() == Blocks.GRASS && rand.nextInt(3) == 0 && world.isAirBlock(p.up())) {
                world.setBlockState(p, Blocks.DIRT.getStateFromMeta(1), 2);
                done++;
            } else if (s.getBlock() == Blocks.LEAVES && rand.nextInt(4) == 0) {
                world.setBlockState(p, Blocks.AIR.getDefaultState(), 2);
                done++;
            }
        }
        return done;
    }

    /** A point a little way off in a direction, at the garden's level: for placing things round it. */
    static BlockPos around(BlockPos c, double angle, double r) {
        return c.add(MathHelper.floor(Math.cos(angle) * r), 0, MathHelper.floor(Math.sin(angle) * r));
    }
}
