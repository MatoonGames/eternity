package com.matoon.eternity.block;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.IStringSerializable;

/**
 * The station's skin, in every finish the Sovereign gave it: brushed plate, sunburst panels,
 * fluted ribs, teal-set trim, tarnished bronze where nobody looks, polished floor, the dark
 * mirrors of the Sanctum, and the shell and lining of the one great cocoon.
 */
public class BlockSovereignHull extends SovereignBlock {

    public enum Kind implements IStringSerializable {
        PLATE, PANEL, RIB, TRIM, DARK, FLOOR, MIRROR, SHELL, MEMBRANE;

        @Override
        public String getName() {
            return name().toLowerCase();
        }
    }

    public static final PropertyEnum<Kind> KIND = PropertyEnum.create("kind", Kind.class);

    public BlockSovereignHull() {
        super("sovereign_hull", Material.IRON, SoundType.METAL);
        setDefaultState(blockState.getBaseState().withProperty(KIND, Kind.PLATE));
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
}
