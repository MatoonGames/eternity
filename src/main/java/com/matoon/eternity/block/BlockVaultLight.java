package com.matoon.eternity.block;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * The vault's lights, all the warm colour of old gold: the diamond lattice down the roof, the
 * panes of the wall of light, the grilles in the floor that light each treasure from below, and
 * the gold rosette lamps in the bays and down the steps.
 */
public class BlockVaultLight extends SovereignBlock {

    public enum Kind implements IStringSerializable {
        LATTICE, PANEL, UPLIGHT, LAMP;

        @Override
        public String getName() {
            return name().toLowerCase();
        }
    }

    public static final PropertyEnum<Kind> KIND = PropertyEnum.create("kind", Kind.class);

    public BlockVaultLight() {
        super("vault_light", Material.GLASS, SoundType.GLASS);
        setDefaultState(blockState.getBaseState().withProperty(KIND, Kind.PANEL));
    }

    public IBlockState of(Kind kind) {
        return getDefaultState().withProperty(KIND, kind);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, KIND);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        Kind[] kinds = Kind.values();
        return getDefaultState().withProperty(KIND, kinds[Math.max(0, Math.min(kinds.length - 1, meta))]);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(KIND).ordinal();
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
