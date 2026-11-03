package com.matoon.eternity.block;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A seam of white light set in gold. Most of the station's still burn; the ones that don't are
 * where the power has started to fail, and the director lets a few stutter between the two.
 */
public class BlockSovereignLight extends SovereignBlock {

    public static final PropertyBool LIT = PropertyBool.create("lit");

    public BlockSovereignLight() {
        super("sovereign_light", Material.GLASS, SoundType.GLASS);
        setDefaultState(blockState.getBaseState().withProperty(LIT, true));
    }

    public IBlockState of(boolean lit) {
        return getDefaultState().withProperty(LIT, lit);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, LIT);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(LIT, (meta & 1) == 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(LIT) ? 0 : 1;
    }

    @Override
    public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
        return state.getValue(LIT) ? 15 : 3;
    }

    @Override
    public int getLightValue(IBlockState state) {
        return state.getValue(LIT) ? 15 : 3;
    }
}
