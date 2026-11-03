package com.matoon.eternity.block;

import com.matoon.eternity.Eternity;
import com.matoon.eternity.story.Stones;
import com.matoon.eternity.story.StoryDirector;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A grave-stone of the oath's village: a standing slab cut with runes, raised for someone. Old
 * ones are grey and mossed; the new ones are pale, their runes painted red the way they were
 * meant to be. Each knows whose it is (see {@link Stones}), and reading one tells you what it
 * says. Taken up, it remembers.
 */
public class BlockRuneStone extends Block implements ITileEntityProvider {

    public static final PropertyBool FRESH = PropertyBool.create("fresh");

    private static final AxisAlignedBB NS = new AxisAlignedBB(1 / 16D, 0D, 5 / 16D, 15 / 16D, 1D, 11 / 16D);
    private static final AxisAlignedBB EW = new AxisAlignedBB(5 / 16D, 0D, 1 / 16D, 11 / 16D, 1D, 15 / 16D);

    public BlockRuneStone() {
        super(Material.ROCK, MapColor.STONE);
        setRegistryName(Eternity.MODID, "rune_stone");
        setTranslationKey(Eternity.MODID + ".rune_stone");
        setHardness(3F);
        setResistance(20F);
        setSoundType(SoundType.STONE);
        setDefaultState(blockState.getBaseState().withProperty(BlockHorizontal.FACING, EnumFacing.NORTH).withProperty(FRESH, false));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, BlockHorizontal.FACING, FRESH);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(BlockHorizontal.FACING, EnumFacing.byHorizontalIndex(meta & 3)).withProperty(FRESH, (meta & 4) != 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(BlockHorizontal.FACING).getHorizontalIndex() | (state.getValue(FRESH) ? 4 : 0);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return state.getValue(BlockHorizontal.FACING).getAxis() == EnumFacing.Axis.Z ? NS : EW;
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
        return face == EnumFacing.DOWN ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileRuneStone();
    }

    public static int stoneAt(IBlockAccess world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        return te instanceof TileRuneStone ? ((TileRuneStone) te).stone : Stones.WORN;
    }

    // ------------------------------------------------------------------ reading it

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing,
                                    float hitX, float hitY, float hitZ) {
        if (hand != EnumHand.MAIN_HAND) {
            return true;
        }
        if (!world.isRemote && player instanceof EntityPlayerMP) {
            StoryDirector.INSTANCE.readStone((EntityPlayerMP) player, pos, stoneAt(world, pos));
        }
        return true;
    }

    // ------------------------------------------------------------------ taking it up, setting it down

    public static ItemStack stack(int stone) {
        ItemStack stack = new ItemStack(EternityBlocks.RUNE_STONE);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("Stone", stone);
        stack.setTagCompound(tag);
        return stack;
    }

    public static int stoneOf(ItemStack stack) {
        return stack.hasTagCompound() && stack.getTagCompound().hasKey("Stone") ? stack.getTagCompound().getInteger("Stone") : Stones.WORN;
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta,
                                            EntityLivingBase placer, EnumHand hand) {
        ItemStack held = placer.getHeldItem(hand);
        return getDefaultState().withProperty(BlockHorizontal.FACING, placer.getHorizontalFacing().getOpposite())
                .withProperty(FRESH, Stones.fresh(stoneOf(held)));
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileRuneStone) {
            ((TileRuneStone) te).stone = stoneOf(stack);
            te.markDirty();
        }
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        drops.add(stack(stoneAt(world, pos)));
    }

    @Override
    public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player, boolean willHarvest) {
        // Keep the tile until the drop has been made, so the stone remembers whose it is.
        return willHarvest || super.removedByPlayer(state, world, pos, player, false);
    }

    @Override
    public void harvestBlock(World world, EntityPlayer player, BlockPos pos, IBlockState state, @Nullable TileEntity te, ItemStack tool) {
        super.harvestBlock(world, player, pos, state, te, tool);
        world.setBlockToAir(pos);
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
        return stack(stoneAt(world, pos));
    }

    @net.minecraftforge.fml.relauncher.SideOnly(net.minecraftforge.fml.relauncher.Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add("§7§o" + Stones.whose(stoneOf(stack)));
    }
}
