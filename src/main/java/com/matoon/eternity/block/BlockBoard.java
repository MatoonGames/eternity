package com.matoon.eternity.block;

import com.matoon.eternity.Eternity;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A square of the Grandmaster's board: gold or blue lacquer, the throne at its heart, the
 * refuges at its corners, and the rim around it all. He lays it out and takes it away again,
 * so it can never be mined, blown up, picked or dropped; striking it is another matter, and
 * the board's own director listens for that.
 */
public class BlockBoard extends Block {

    public enum Face implements IStringSerializable {
        GOLD, BLUE, THRONE, REFUGE, RIM;

        @Override
        public String getName() {
            return name().toLowerCase();
        }
    }

    public static final PropertyEnum<Face> COLOR = PropertyEnum.create("color", Face.class);

    public BlockBoard() {
        super(Material.ROCK, MapColor.GOLD);
        setRegistryName(Eternity.MODID, "board");
        setTranslationKey(Eternity.MODID + ".board");
        setBlockUnbreakable();
        setResistance(6000000F);
        setSoundType(SoundType.GLASS);
        setDefaultState(blockState.getBaseState().withProperty(COLOR, Face.GOLD));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, COLOR);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        Face[] faces = Face.values();
        return getDefaultState().withProperty(COLOR, faces[Math.max(0, Math.min(faces.length - 1, meta))]);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(COLOR).ordinal();
    }

    @Override
    public MapColor getMapColor(IBlockState state, IBlockAccess world, BlockPos pos) {
        Face face = state.getValue(COLOR);
        return face == Face.BLUE || face == Face.REFUGE ? MapColor.LAPIS : MapColor.GOLD;
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canEntityDestroy(IBlockState state, IBlockAccess world, BlockPos pos, Entity entity) {
        return false;
    }

    @Override
    public boolean canDropFromExplosion(Explosion explosion) {
        return false;
    }

    @Override
    public void onBlockExploded(World world, BlockPos pos, Explosion explosion) {
    }

    @Override
    public net.minecraft.block.material.EnumPushReaction getPushReaction(IBlockState state) {
        return net.minecraft.block.material.EnumPushReaction.BLOCK;
    }
}
