package com.matoon.eternity.block;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/** A gold brazier of coals down the vault's hall. Its fire is drawn by the client, over the coals. */
public class BlockVaultBrazier extends SovereignBlock {

    private static final AxisAlignedBB BOX = new AxisAlignedBB(1D / 16D, 0D, 1D / 16D, 15D / 16D, 15D / 16D, 15D / 16D);

    public BlockVaultBrazier() {
        super("vault_brazier", Material.IRON, SoundType.METAL);
        setLightOpacity(0);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return BOX;
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
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    @Override
    public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
        return 15;
    }

    @Override
    public int getLightValue(IBlockState state) {
        return 15;
    }
}
