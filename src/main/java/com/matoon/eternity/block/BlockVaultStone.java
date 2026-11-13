package com.matoon.eternity.block;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.IStringSerializable;

/**
 * The stone of Odin's Vault, deep under Valaskjalf: blue-black basalt laid in great courses,
 * polished slate, gold-veined black, the floor's slabs and the aisle's framed tiles, the Valknut
 * inlaid between the bays, hammered gold, fluted pilasters and the treads of the steps. Built to
 * the same rule as the Sovereign's station: nobody quarries a king's treasure room.
 */
public class BlockVaultStone extends SovereignBlock {

    public enum Kind implements IStringSerializable {
        WALL, SLATE, OBSIDIAN, FLOOR, TRIM, CARVED, GOLD, PILLAR, STEP;

        @Override
        public String getName() {
            return name().toLowerCase();
        }
    }

    public static final PropertyEnum<Kind> KIND = PropertyEnum.create("kind", Kind.class);

    public BlockVaultStone() {
        super("vault_stone", Material.ROCK, SoundType.STONE);
        setDefaultState(blockState.getBaseState().withProperty(KIND, Kind.WALL));
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
