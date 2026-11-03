package com.matoon.eternity.block;

import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The station's furniture, each shaped by its own model rather than a cube: the cocoons that
 * Warlock and his failed brothers came out of (or didn't), the vats the worse things grew in,
 * the gallery's capsules, and the records consoles. Most stand two blocks tall, in two halves;
 * some face a way. Each kind is its own block, sharing all of this.
 */
public abstract class BlockSovereignFixture extends SovereignBlock {

    public static final PropertyDirection FACING = BlockHorizontal.FACING;
    public static final PropertyBool UPPER = PropertyBool.create("upper");
    public static final PropertyBool FLAG = PropertyBool.create("flag");
    /** Three blocks tall (the vats and the capsules, a whole grown body high): which third this is, from the foot. */
    public static final net.minecraft.block.properties.PropertyInteger PART = net.minecraft.block.properties.PropertyInteger.create("part", 0, 2);

    private static final AxisAlignedBB BOX = new AxisAlignedBB(1 / 16D, 0D, 1 / 16D, 15 / 16D, 1D, 15 / 16D);

    private final int light, flagLight;
    private final boolean translucent;

    BlockSovereignFixture(String name, Material material, SoundType sound, int light,
                          int flagLight, boolean translucent) {
        super(name, material, sound);
        this.light = light;
        this.flagLight = flagLight;
        this.translucent = translucent;
        setLightOpacity(0);
        IBlockState state = blockState.getBaseState();
        for (IProperty<?> p : blockState.getProperties()) {
            if (p == FACING) {
                state = state.withProperty(FACING, EnumFacing.NORTH);
            } else if (p == UPPER || p == FLAG) {
                state = state.withProperty((PropertyBool) p, false);
            } else if (p == PART) {
                state = state.withProperty(PART, 0);
            }
        }
        setDefaultState(state);
    }

    /** One third of a three-block fixture: 0 the foot, 2 the head. */
    public IBlockState part(int part, boolean flag) {
        IBlockState s = getDefaultState().withProperty(PART, Math.max(0, Math.min(2, part)));
        return has(FLAG) ? s.withProperty(FLAG, flag) : s;
    }

    /** The foot of whatever stands here, if it is three blocks tall; the position itself otherwise. */
    public static BlockPos foot(IBlockState state, BlockPos pos) {
        return state.getPropertyKeys().contains(PART) ? pos.down(state.getValue(PART)) : state.getPropertyKeys().contains(UPPER)
                && state.getValue(UPPER) ? pos.down() : pos;
    }

    /** The properties a kind has: some of FACING, UPPER, PART and FLAG, decided before the constructor runs. */
    protected abstract IProperty<?>[] props();

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, props());
    }

    private boolean has(IProperty<?> p) {
        return getBlockState().getProperties().contains(p);
    }

    public IBlockState of(EnumFacing facing, boolean upper, boolean flag) {
        IBlockState s = getDefaultState();
        if (has(FACING)) {
            s = s.withProperty(FACING, facing.getAxis().isHorizontal() ? facing : EnumFacing.NORTH);
        }
        if (has(UPPER)) {
            s = s.withProperty(UPPER, upper);
        }
        if (has(FLAG)) {
            s = s.withProperty(FLAG, flag);
        }
        return s;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        IBlockState s = getDefaultState();
        if (has(FACING)) {
            s = s.withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3));
        } else if (has(PART)) {
            s = s.withProperty(PART, Math.min(2, meta & 3));
        }
        if (has(UPPER)) {
            s = s.withProperty(UPPER, (meta & 4) != 0);
        }
        if (has(FLAG)) {
            s = s.withProperty(FLAG, (meta & 8) != 0);
        }
        return s;
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        int meta = 0;
        if (has(FACING)) {
            meta |= state.getValue(FACING).getHorizontalIndex();
        } else if (has(PART)) {
            meta |= state.getValue(PART);
        }
        if (has(UPPER) && state.getValue(UPPER)) {
            meta |= 4;
        }
        if (has(FLAG) && state.getValue(FLAG)) {
            meta |= 8;
        }
        return meta;
    }

    @Override
    public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
        return getLightValue(state);
    }

    @Override
    public int getLightValue(IBlockState state) {
        return has(FLAG) && state.getValue(FLAG) ? flagLight : light;
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
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return BOX;
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public BlockRenderLayer getRenderLayer() {
        return translucent ? BlockRenderLayer.TRANSLUCENT : BlockRenderLayer.CUTOUT;
    }

    // ------------------------------------------------------------------ the kinds

    /** A birthing cocoon, split open down its front: what was in it got out. Faces the way it opened. */
    public static class Open extends BlockSovereignFixture {
        public Open() {
            super("cocoon", Material.ROCK, SoundType.STONE, 0, 0, false);
        }

        @Override
        protected IProperty<?>[] props() {
            return new IProperty<?>[]{FACING, UPPER};
        }
    }

    /** A cocoon that never opened. FLAG: fused, blackened and split, and long dead inside. */
    public static class Sealed extends BlockSovereignFixture {
        public Sealed() {
            super("cocoon_sealed", Material.ROCK, SoundType.STONE, 2, 0, false);
        }

        @Override
        protected IProperty<?>[] props() {
            return new IProperty<?>[]{UPPER, FLAG};
        }
    }

    /** A growing-vat, three blocks of glass over gold, the fluid still moving. FLAG: smashed open from inside. */
    public static class Vat extends BlockSovereignFixture {
        public Vat() {
            super("vat", Material.GLASS, SoundType.GLASS, 7, 2, true);
        }

        @Override
        protected IProperty<?>[] props() {
            return new IProperty<?>[]{PART, FLAG};
        }
    }

    /**
     * A capsule in the gallery, three blocks tall, someone asleep on his feet inside it. FLAG: the
     * frost has cleared from the glass, for whoever has come close to look.
     */
    public static class Pod extends BlockSovereignFixture {
        public Pod() {
            super("ascended_pod", Material.GLASS, SoundType.GLASS, 4, 7, true);
        }

        @Override
        protected IProperty<?>[] props() {
            return new IProperty<?>[]{PART, FLAG};
        }

        @Override
        public boolean onBlockActivated(net.minecraft.world.World world, BlockPos pos, IBlockState state, net.minecraft.entity.player.EntityPlayer player,
                                        net.minecraft.util.EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
            if (!world.isRemote && hand == net.minecraft.util.EnumHand.MAIN_HAND) {
                com.matoon.eternity.story.StoryDirector.INSTANCE.sovereignPod(player, foot(state, pos));
            }
            return true;
        }
    }

    /** A records console, its screen tilted up to whoever stands at it. Right-click plays what it keeps. */
    public static class Console extends BlockSovereignFixture {
        public Console() {
            super("sovereign_console", Material.IRON, SoundType.METAL, 6, 6, false);
        }

        @Override
        protected IProperty<?>[] props() {
            return new IProperty<?>[]{FACING};
        }

        @Override
        public boolean onBlockActivated(net.minecraft.world.World world, BlockPos pos, IBlockState state, net.minecraft.entity.player.EntityPlayer player,
                                        net.minecraft.util.EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
            if (!world.isRemote && hand == net.minecraft.util.EnumHand.MAIN_HAND) {
                com.matoon.eternity.story.StoryDirector.INSTANCE.sovereignConsole(player, pos);
            }
            return true;
        }
    }
}
