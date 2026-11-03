package com.matoon.eternity.block;

import com.matoon.eternity.Eternity;
import net.minecraft.block.BlockBush;
import net.minecraft.block.SoundType;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/**
 * Plants that don't exist, growing where nothing should: in bare stone, in sand, in the cracks of
 * a cliff. The Gardener is seeding the inner worlds, and these are what comes up. Four kinds, none
 * of them from here: glass bells that ring when the wind moves them; ember-ferns, warm to the
 * touch; lantern-heads, pale globes that give off a little light; and blackleaf, its veins silver.
 * Each gives a little light of its own.
 */
public class BlockStrangeBloom extends BlockBush {

    public static final PropertyInteger KIND = PropertyInteger.create("kind", 0, 3);
    public static final String[] KINDS = {"glass_bell", "ember_fern", "lantern_head", "blackleaf"};

    private static final AxisAlignedBB SHAPE = new AxisAlignedBB(0.2D, 0D, 0.2D, 0.8D, 0.8D, 0.8D);

    public BlockStrangeBloom() {
        super();
        setRegistryName(Eternity.MODID, "strange_bloom");
        setTranslationKey(Eternity.MODID + ".strange_bloom");
        setSoundType(SoundType.PLANT);
        setLightLevel(0.4F);
        setDefaultState(blockState.getBaseState().withProperty(KIND, 0));
        setCreativeTab(CreativeTabs.DECORATIONS);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, KIND);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(KIND, meta & 3);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(KIND);
    }

    @Override
    public int damageDropped(IBlockState state) {
        return state.getValue(KIND);
    }

    @Override
    public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> items) {
        for (int i = 0; i < KINDS.length; i++) {
            items.add(new ItemStack(this, 1, i));
        }
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return SHAPE;
    }

    /** They take root in anything solid: that's the wrongness of them. */
    @Override
    protected boolean canSustainBush(IBlockState state) {
        return state.isFullCube() && state.getMaterial().isSolid();
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        IBlockState below = world.getBlockState(pos.down());
        return world.getBlockState(pos).getBlock().isReplaceable(world, pos) && below.isSideSolid(world, pos.down(), EnumFacing.UP);
    }

    @Override
    public boolean canBlockStay(World world, BlockPos pos, IBlockState state) {
        IBlockState below = world.getBlockState(pos.down());
        return below.isSideSolid(world, pos.down(), EnumFacing.UP);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rand) {
        if (rand.nextInt(4) != 0) {
            return;
        }
        double x = pos.getX() + 0.3D + rand.nextDouble() * 0.4D, y = pos.getY() + 0.4D + rand.nextDouble() * 0.4D,
                z = pos.getZ() + 0.3D + rand.nextDouble() * 0.4D;
        switch (state.getValue(KIND)) {
            case 1:
                world.spawnParticle(EnumParticleTypes.FLAME, x, y + 0.2D, z, 0D, 0.005D, 0D);
                break;
            case 2:
                world.spawnParticle(EnumParticleTypes.END_ROD, x, y + 0.2D, z, 0D, 0.01D, 0D);
                break;
            case 3:
                world.spawnParticle(EnumParticleTypes.SPELL_WITCH, x, y, z, 0D, 0D, 0D);
                break;
            default:
                world.spawnParticle(EnumParticleTypes.WATER_DROP, x, y, z, 0D, 0D, 0D);
                break;
        }
    }
}
