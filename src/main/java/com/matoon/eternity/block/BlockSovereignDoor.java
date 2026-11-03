package com.matoon.eternity.block;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * An iris door between the station's halls, sealed since whoever kept them stopped. It lights
 * when it is ready to open, and the station's director opens it (the story decides when; a
 * hand on it only asks).
 */
public class BlockSovereignDoor extends SovereignBlock {

    public static final PropertyBool LIT = PropertyBool.create("lit");

    public BlockSovereignDoor() {
        super("sovereign_door", Material.IRON, SoundType.METAL);
        setDefaultState(blockState.getBaseState().withProperty(LIT, false));
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
        return getDefaultState().withProperty(LIT, (meta & 1) == 1);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(LIT) ? 1 : 0;
    }

    @Override
    public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
        return state.getValue(LIT) ? 9 : 0;
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing,
                                    float hitX, float hitY, float hitZ) {
        if (!world.isRemote && hand == EnumHand.MAIN_HAND) {
            com.matoon.eternity.story.StoryDirector.INSTANCE.sovereignDoor(player, pos);
        }
        return true;
    }
}
