package com.matoon.eternity.block;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/** A band of runes cut into the vault's walls, the cuts full of a cold seidr-light that runs along them. */
public class BlockVaultRunes extends SovereignBlock {

    public BlockVaultRunes() {
        super("vault_runes", Material.ROCK, SoundType.STONE);
    }

    @Override
    public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
        return 7;
    }

    @Override
    public int getLightValue(IBlockState state) {
        return 7;
    }
}
