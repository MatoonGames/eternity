package com.matoon.eternity.story;

import com.matoon.eternity.block.BlockRuneStone;
import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.network.MsgStory;
import net.minecraft.block.BlockDirt;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.BlockSnow;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.passive.AbstractHorse;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;
import java.util.Random;

/**
 * Pieces of the north coming loose from round the cube and landing across the world: a disc of
 * snowed-over turf where there was none, and whatever was standing on it when it was taken. Each
 * lands hard, and skids: a tail of snow and torn turf runs out from it on the far side from where
 * it was thrown, so a few of them read together point back at the village.
 * <p>
 * Later they reach home, and take from it instead: blocks round someone's bed turned to snow and
 * turf, a villager gone, an animal gone from its pen. None of it comes back.
 */
final class Leaks {

    /** What was standing on the ground when it was taken. */
    static final int TURF = 0, BOAT = 1, DOOR = 2, BOWL = 3, SHIELD = 4, STONE = 5;

    private Leaks() {
    }

    /** What the next leak carries: a stone of the keepers' the second time, a boat once, a door once, and so on. */
    static int kind(int index, Random rand) {
        switch (index) {
            case 0:
                return TURF;
            case 1:
                return STONE;
            case 2:
                return BOAT;
            case 4:
                return DOOR;
            default:
                int[] pool = {TURF, TURF, BOWL, SHIELD, DOOR, TURF};
                return pool[rand.nextInt(pool.length)];
        }
    }

    /**
     * Lands a piece of the north at a spot, thrown from the direction of {@code from}. Returns
     * where its middle came down.
     */
    static BlockPos land(World world, BlockPos at, Vec3d from, int kind, int radius, Random rand, boolean keepersStone) {
        BlockPos middle = Barrow.surface(world, at.getX(), at.getZ());
        Vec3d along = new Vec3d(middle.getX() - from.x, 0D, middle.getZ() - from.z);
        along = along.lengthSquared() < 1.0E-4 ? new Vec3d(1D, 0D, 0D) : along.normalize();
        // The disc, ragged at its edge.
        for (int dx = -radius - 1; dx <= radius + 1; dx++) {
            for (int dz = -radius - 1; dz <= radius + 1; dz++) {
                double r = Math.sqrt(dx * dx + dz * dz) + (rand.nextFloat() - 0.5F) * 1.2F;
                if (r > radius + 0.3D) {
                    continue;
                }
                turf(world, middle.getX() + dx, middle.getZ() + dz, rand, r > radius - 1D ? 0.5F : 0.85F);
            }
        }
        // The skid: snow and torn turf thrown on past where it came down.
        int tail = 5 + radius * 2;
        for (int i = 1; i <= tail; i++) {
            double t = radius + i;
            double spread = 1.2D + i * 0.18D;
            for (int k = 0; k < 2 + rand.nextInt(2); k++) {
                double side = (rand.nextDouble() - 0.5D) * 2D * spread;
                int x = MathHelper.floor(middle.getX() + 0.5D + along.x * t - along.z * side);
                int z = MathHelper.floor(middle.getZ() + 0.5D + along.z * t + along.x * side);
                if (rand.nextFloat() < 1F - i / (float) (tail + 2)) {
                    turf(world, x, z, rand, 0.3F);
                }
            }
        }
        BlockPos top = Barrow.surface(world, middle.getX(), middle.getZ());
        EnumFacing facing = EnumFacing.getFacingFromVector((float) along.x, 0F, (float) along.z);
        switch (kind) {
            case BOAT: {
                EntityBoat boat = new EntityBoat(world, top.getX() + 0.5D, top.getY() + 0.1D, top.getZ() + 0.5D);
                boat.setBoatType(EntityBoat.Type.SPRUCE);
                boat.rotationYaw = facing.getHorizontalAngle() + 70F;
                world.spawnEntity(boat);
                world.setBlockState(top.offset(facing.rotateY(), 2), Blocks.WEB.getDefaultState(), 2);
                drop(world, top.offset(facing.rotateYCCW(), 2), named(new ItemStack(Items.FISH), "A cod, still wet",
                        "It came a long way to be here."));
                break;
            }
            case DOOR: {
                // A door with no house: its posts, its lintel, and it, standing open.
                EnumFacing across = facing.rotateY();
                BlockPos d = top;
                world.setBlockState(d.offset(across), Blocks.LOG.getStateFromMeta(1), 2);
                world.setBlockState(d.offset(across).up(), Blocks.LOG.getStateFromMeta(1), 2);
                world.setBlockState(d.offset(across.getOpposite()), Blocks.LOG.getStateFromMeta(1), 2);
                world.setBlockState(d.offset(across.getOpposite()).up(), Blocks.LOG.getStateFromMeta(1), 2);
                world.setBlockState(d.up(2), Blocks.LOG.getStateFromMeta(1 | 4 * (across.getAxis() == EnumFacing.Axis.X ? 1 : 2)), 2);
                world.setBlockState(d.up(2).offset(across), Blocks.LOG.getStateFromMeta(1 | 4 * (across.getAxis() == EnumFacing.Axis.X ? 1 : 2)), 2);
                world.setBlockState(d.up(2).offset(across.getOpposite()), Blocks.LOG.getStateFromMeta(1 | 4 * (across.getAxis() == EnumFacing.Axis.X ? 1 : 2)), 2);
                IBlockState lower = Blocks.SPRUCE_DOOR.getDefaultState().withProperty(BlockDoor.FACING, facing)
                        .withProperty(BlockDoor.OPEN, true).withProperty(BlockDoor.HALF, BlockDoor.EnumDoorHalf.LOWER);
                world.setBlockState(d, lower, 2);
                world.setBlockState(d.up(), lower.withProperty(BlockDoor.HALF, BlockDoor.EnumDoorHalf.UPPER), 2);
                break;
            }
            case BOWL:
                drop(world, top, named(new ItemStack(Items.BOWL), "A carved bowl", "Ravens are cut round its rim."));
                break;
            case SHIELD:
                drop(world, top, shield());
                break;
            case STONE: {
                int stone = keepersStone ? Stones.EIRIK : Stones.WORN;
                Barrow.stone(world, top, facing.getOpposite(), stone, false);
                break;
            }
            default:
                if (rand.nextBoolean()) {
                    world.setBlockState(top, Blocks.SAPLING.getStateFromMeta(1), 2);
                } else {
                    world.setBlockState(top.offset(facing), Blocks.MOSSY_COBBLESTONE.getDefaultState(), 2);
                }
                break;
        }
        world.playSound(null, middle, SoundEvents.ENTITY_LIGHTNING_THUNDER, SoundCategory.AMBIENT, 3F, 0.45F);
        world.playSound(null, middle, SoundEvents.BLOCK_SNOW_BREAK, SoundCategory.BLOCKS, 2F, 0.6F);
        EternityNetwork.sendAround(MsgStory.fx(MsgStory.FX_LEAK, new Vec3d(middle).add(0.5D, 0D, 0.5D), from, radius),
                world.provider.getDimension(), middle.getX(), middle.getY(), middle.getZ(), 320D);
        return middle;
    }

    /** One column of northern ground: turf or podzol under snow, now and then a stone. */
    static void turf(World world, int x, int z, Random rand, float snow) {
        BlockPos top = Barrow.surface(world, x, z);
        BlockPos ground = top.down();
        IBlockState was = world.getBlockState(ground);
        if (!natural(was) || world.getTileEntity(ground) != null) {
            return;
        }
        float r = rand.nextFloat();
        IBlockState s = r < 0.55F ? Blocks.GRASS.getDefaultState() : r < 0.8F
                ? Blocks.DIRT.getDefaultState().withProperty(BlockDirt.VARIANT, BlockDirt.DirtType.PODZOL)
                : r < 0.93F ? Blocks.DIRT.getDefaultState().withProperty(BlockDirt.VARIANT, BlockDirt.DirtType.COARSE_DIRT)
                : Blocks.MOSSY_COBBLESTONE.getDefaultState();
        world.setBlockState(ground, s, 2);
        IBlockState above = world.getBlockState(top);
        if (above.getMaterial().isReplaceable() && !above.getMaterial().isLiquid() && rand.nextFloat() < snow) {
            world.setBlockState(top, Blocks.SNOW_LAYER.getDefaultState().withProperty(BlockSnow.LAYERS, 1 + rand.nextInt(2)), 2);
        }
    }

    /** Ground the north can land on: nothing anyone built, nothing wet. */
    static boolean natural(IBlockState s) {
        Material m = s.getMaterial();
        return m == Material.GRASS || m == Material.GROUND || m == Material.SAND || m == Material.ROCK && s.getBlock() == Blocks.STONE
                || m == Material.CLAY || m == Material.SNOW || m == Material.CRAFTED_SNOW;
    }

    private static void drop(World world, BlockPos at, ItemStack stack) {
        EntityItem item = new EntityItem(world, at.getX() + 0.5D, at.getY() + 0.2D, at.getZ() + 0.5D, stack);
        item.motionX = item.motionY = item.motionZ = 0D;
        item.lifespan = Integer.MAX_VALUE;
        item.setPickupDelay(10);
        world.spawnEntity(item);
    }

    static ItemStack named(ItemStack stack, String name, String lore) {
        NBTTagCompound display = new NBTTagCompound();
        display.setString("Name", "§r" + name);
        NBTTagList list = new NBTTagList();
        list.appendTag(new NBTTagString("§7§o" + lore));
        display.setTag("Lore", list);
        NBTTagCompound tag = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
        tag.setTag("display", display);
        stack.setTagCompound(tag);
        return stack;
    }

    /** A round shield, black, with the village's raven on it in white. */
    private static ItemStack shield() {
        ItemStack stack = new ItemStack(Items.SHIELD);
        NBTTagCompound bet = new NBTTagCompound();
        bet.setInteger("Base", 0);
        NBTTagList patterns = new NBTTagList();
        NBTTagCompound p = new NBTTagCompound();
        p.setString("Pattern", "flo");
        p.setInteger("Color", 15);
        patterns.appendTag(p);
        NBTTagCompound b = new NBTTagCompound();
        b.setString("Pattern", "bo");
        b.setInteger("Color", 8);
        patterns.appendTag(b);
        bet.setTag("Patterns", patterns);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("BlockEntityTag", bet);
        stack.setTagCompound(tag);
        return named(stack, "A shield with a raven on it", "Split along the grain. Somebody's.");
    }

    // ------------------------------------------------------------------ reaching home

    /**
     * The north takes from someone's home instead: a patch round their bed turned to snow and
     * turf, and maybe a villager or a penned animal near it, gone. Returns how many things it took.
     */
    static int takeFromHome(World world, EntityPlayerMP player, BlockPos home, Random rand) {
        int taken = 0;
        Vec3d c = new Vec3d(home).add(0.5D, 0D, 0.5D);
        double a = rand.nextDouble() * Math.PI * 2D;
        BlockPos middle = new BlockPos(c.x + Math.cos(a) * (3 + rand.nextInt(7)), home.getY(), c.z + Math.sin(a) * (3 + rand.nextInt(7)));
        for (int i = 0; i < 10 + rand.nextInt(8); i++) {
            int x = middle.getX() + rand.nextInt(7) - 3, z = middle.getZ() + rand.nextInt(7) - 3;
            BlockPos top = Barrow.surface(world, x, z);
            BlockPos ground = top.down();
            if (Math.abs(ground.getY() - home.getY()) > 6) {
                continue;
            }
            IBlockState s = world.getBlockState(ground);
            if (s.getBlockHardness(world, ground) < 0F || world.getTileEntity(ground) != null || s.getMaterial().isLiquid()
                    || s.getMaterial() == Material.AIR || ground.equals(home) || ground.equals(home.down())) {
                continue;
            }
            world.setBlockState(ground, rand.nextBoolean() ? Blocks.SNOW.getDefaultState()
                    : Blocks.DIRT.getDefaultState().withProperty(BlockDirt.VARIANT, BlockDirt.DirtType.PODZOL), 2);
            taken++;
        }
        // Someone who lived near.
        List<EntityVillager> villagers = world.getEntitiesWithinAABB(EntityVillager.class, new AxisAlignedBB(home).grow(64D));
        if (!villagers.isEmpty() && rand.nextInt(3) == 0) {
            vanish(world, villagers.get(rand.nextInt(villagers.size())));
            taken++;
        }
        // An animal from its pen: never anyone's pet.
        List<EntityAnimal> animals = world.getEntitiesWithinAABB(EntityAnimal.class, new AxisAlignedBB(home).grow(24D),
                e -> !(e instanceof EntityTameable) && !(e instanceof AbstractHorse) && !e.hasCustomName());
        if (!animals.isEmpty() && rand.nextBoolean()) {
            vanish(world, animals.get(rand.nextInt(animals.size())));
            taken++;
        }
        world.playSound(null, middle, SoundEvents.ENTITY_LIGHTNING_THUNDER, SoundCategory.AMBIENT, 2F, 0.4F);
        EternityNetwork.sendAround(MsgStory.fx(MsgStory.FX_TAKEN, new Vec3d(middle).add(0.5D, 0D, 0.5D), c, taken),
                world.provider.getDimension(), middle.getX(), middle.getY(), middle.getZ(), 160D);
        return taken;
    }

    /** Gone: a gust of snow where they stood. */
    private static void vanish(World world, Entity e) {
        EternityNetwork.sendAround(MsgStory.fx(MsgStory.FX_TAKEN, e.getPositionVector(), e.getPositionVector(), 0),
                world.provider.getDimension(), e.posX, e.posY, e.posZ, 96D);
        world.playSound(null, e.posX, e.posY, e.posZ, SoundEvents.BLOCK_SNOW_BREAK, SoundCategory.NEUTRAL, 1.5F, 0.5F);
        e.setDead();
    }
}
