package com.matoon.eternity.block;

import com.matoon.eternity.Eternity;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * What is left of the Gardener's hedge once something stronger than him has been at it.
 * <ul>
 * <li><b>Frozen</b>: the Casket's winter has gone into the stems. They aren't alive while they're
 * frozen through, so he can't keep them so: they break like glass, and nothing grows back.</li>
 * <li><b>Dead</b>: the Soul Stone has drunk the life out of them. Grey, split, nothing to stop
 * anyone walking through; it falls away to dust a little at a time.</li>
 * </ul>
 */
public class BlockThornHusk extends Block {

    public static final PropertyBool DEAD = PropertyBool.create("dead");
    private static final AxisAlignedBB SHAPE = new AxisAlignedBB(0.0625D, 0D, 0.0625D, 0.9375D, 0.9375D, 0.9375D);

    public BlockThornHusk() {
        super(Material.ICE, MapColor.ICE);
        setRegistryName(Eternity.MODID, "garden_thorn_husk");
        setTranslationKey(Eternity.MODID + ".garden_thorn_husk");
        setHardness(0.4F);
        setResistance(1F);
        setSoundType(SoundType.GLASS);
        setLightOpacity(2);
        setTickRandomly(true);
        setDefaultState(blockState.getBaseState().withProperty(DEAD, false));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, DEAD);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(DEAD, (meta & 1) != 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(DEAD) ? 1 : 0;
    }

    @Override
    public SoundType getSoundType(IBlockState state, World world, BlockPos pos, @Nullable net.minecraft.entity.Entity entity) {
        return state.getValue(DEAD) ? SoundType.PLANT : SoundType.GLASS;
    }

    @Override
    public MapColor getMapColor(IBlockState state, IBlockAccess world, BlockPos pos) {
        return state.getValue(DEAD) ? MapColor.BROWN : MapColor.ICE;
    }

    @Override
    public float getBlockHardness(IBlockState state, World world, BlockPos pos) {
        return state.getValue(DEAD) ? 0F : super.getBlockHardness(state, world, pos);
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT_MIPPED;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean shouldSideBeRendered(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
        return world.getBlockState(pos.offset(side)) != state && super.shouldSideBeRendered(state, world, pos, side);
    }

    @Nullable
    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return state.getValue(DEAD) ? NULL_AABB : SHAPE;
    }

    @Override
    public boolean isPassable(IBlockAccess world, BlockPos pos) {
        return world.getBlockState(pos).getValue(DEAD);
    }

    /** Dead stems fall away to dust, a few at a time, until only stumps are left. */
    @Override
    public void randomTick(World world, BlockPos pos, IBlockState state, Random random) {
        if (state.getValue(DEAD) && pos.getY() > 0 && world.getBlockState(pos.down()).getBlock() == this && random.nextInt(3) == 0) {
            world.destroyBlock(pos, false);
        }
    }

    /** A frozen stem broken by hand: the hedge has a hole in it he can't grow shut. */
    @Override
    public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player, boolean willHarvest) {
        boolean removed = super.removedByPlayer(state, world, pos, player, willHarvest);
        if (removed && !world.isRemote && !state.getValue(DEAD) && player instanceof EntityPlayerMP) {
            com.matoon.eternity.story.StoryDirector.INSTANCE.hedgeShattered((EntityPlayerMP) player, pos);
        }
        return removed;
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Items.AIR;
    }

    @Override
    public boolean canSilkHarvest(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
        return false;
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, net.minecraft.util.math.RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
        return ItemStack.EMPTY;
    }
}
