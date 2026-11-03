package com.matoon.eternity.block;

import com.matoon.eternity.Eternity;
import com.matoon.eternity.story.StoryDirector;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The stone of the barrow's doors, cut with the oath's runes. It answers to nothing but what the
 * oath says it answers to; nothing breaks it, and when it opens it is the story that moves it.
 * Lit, its runes give a cold light. A door of it is one carving across all nine of its blocks:
 * each block works out from its neighbours which ninth of it to show.
 */
public class BlockBarrowSeal extends Block {

    public static final PropertyBool LIT = PropertyBool.create("lit");
    public static final PropertyInteger COL = PropertyInteger.create("col", 0, 2), ROW = PropertyInteger.create("row", 0, 2);

    public BlockBarrowSeal() {
        super(Material.ROCK, MapColor.STONE);
        setRegistryName(Eternity.MODID, "barrow_seal");
        setTranslationKey(Eternity.MODID + ".barrow_seal");
        setBlockUnbreakable();
        setResistance(6000000F);
        setSoundType(SoundType.STONE);
        setDefaultState(blockState.getBaseState().withProperty(LIT, false).withProperty(COL, 1).withProperty(ROW, 1));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, LIT, COL, ROW);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(LIT, meta != 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(LIT) ? 1 : 0;
    }

    /** Which ninth of the carving this block is: by how many more of the door lie to its left and below it. */
    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
        boolean alongX = world.getBlockState(pos.east()).getBlock() == this || world.getBlockState(pos.west()).getBlock() == this;
        boolean alongZ = !alongX && (world.getBlockState(pos.north()).getBlock() == this || world.getBlockState(pos.south()).getBlock() == this);
        int col = 1;
        if (alongX || alongZ) {
            EnumFacing left = alongX ? EnumFacing.WEST : EnumFacing.NORTH;
            boolean l1 = world.getBlockState(pos.offset(left)).getBlock() == this;
            boolean r1 = world.getBlockState(pos.offset(left.getOpposite())).getBlock() == this;
            col = !l1 ? 0 : !r1 ? 2 : 1;
        }
        boolean below = world.getBlockState(pos.down()).getBlock() == this;
        boolean above = world.getBlockState(pos.up()).getBlock() == this;
        int row = !below ? 2 : !above ? 0 : 1;
        return state.withProperty(COL, col).withProperty(ROW, row);
    }

    @Override
    public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
        return state.getValue(LIT) ? 6 : 0;
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing,
                                    float hitX, float hitY, float hitZ) {
        if (hand == EnumHand.MAIN_HAND && !world.isRemote && player instanceof EntityPlayerMP) {
            StoryDirector.INSTANCE.touchSeal((EntityPlayerMP) player, pos);
        }
        return true;
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
    }

    @Override
    public boolean canEntityDestroy(IBlockState state, IBlockAccess world, BlockPos pos, Entity entity) {
        return false;
    }

    @Override
    public void onBlockExploded(World world, BlockPos pos, Explosion explosion) {
    }

    @Override
    public boolean canDropFromExplosion(Explosion explosion) {
        return false;
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
        return ItemStack.EMPTY;
    }
}
