package com.matoon.eternity.block;

import com.matoon.eternity.Eternity;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

/**
 * The Gardener's hedge, grown up round his garden once he knows what is coming for him: black
 * stems as thick as an arm, woven, every one of them thorned. No blade cuts it and no fire takes
 * it; it is alive, and he is keeping it so. Only the Power Stone breaks it (see
 * com.matoon.eternity.story.GardenStory). It tears whoever pushes into it.
 */
public class BlockGardenThorn extends Block {

    private static final AxisAlignedBB SHAPE = new AxisAlignedBB(0.0625D, 0D, 0.0625D, 0.9375D, 0.9375D, 0.9375D);
    public static final DamageSource THORNS = new DamageSource("eternity.thorns");

    public BlockGardenThorn() {
        super(Material.WOOD, MapColor.GREEN_STAINED_HARDENED_CLAY);
        setRegistryName(Eternity.MODID, "garden_thorn");
        setTranslationKey(Eternity.MODID + ".garden_thorn");
        setBlockUnbreakable();
        setResistance(6000000F);
        setSoundType(SoundType.PLANT);
        setLightOpacity(3);
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
        return world.getBlockState(pos.offset(side)).getBlock() != this && super.shouldSideBeRendered(state, world, pos, side);
    }

    @Nullable
    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return SHAPE;
    }

    @Override
    public void onEntityCollision(World world, BlockPos pos, IBlockState state, Entity entity) {
        if (!world.isRemote && entity instanceof net.minecraft.entity.EntityLivingBase) {
            entity.attackEntityFrom(THORNS, 2F);
        }
    }

    @Override
    public boolean canEntityDestroy(IBlockState state, IBlockAccess world, BlockPos pos, Entity entity) {
        return false;
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, net.minecraft.util.math.RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
        return ItemStack.EMPTY;
    }
}
