package com.matoon.eternity.compat.galacticraft;

import com.matoon.eternity.world.ChampionDirector;
import com.matoon.eternity.world.Tamarata;
import com.matoon.eternity.world.TamarataBreaker;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBanner;
import net.minecraft.block.BlockSkull;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBanner;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.IChunkGenerator;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Builds Tamarata from {@link Tamarata}: the ground, the landing site and the Colosseum, and
 * the ruins scattered across the desert. Populating a chunk sets out what needs a living world:
 * the launch pad, armour stands, banners and skulls. Once the planet is broken, new chunks are
 * born as the field of fragments it became, the island around the landing pad aside.
 * <p>
 * Galacticraft builds this through its public (World, long, boolean) constructor.
 */
public class ChunkGeneratorTamarata implements IChunkGenerator {

    private static final int SITE_LOW = Tamarata.ARENA_Y - 14, SITE_HIGH = Tamarata.ARENA_Y + 40;

    private final World world;
    private final long seed;

    public ChunkGeneratorTamarata(World world, long seed, boolean features) {
        this.world = world;
        this.seed = seed;
    }

    @Override
    public Chunk generateChunk(int chunkX, int chunkZ) {
        ChunkPrimer primer = new ChunkPrimer();
        boolean broken = ChampionDirector.INSTANCE.planetBroken();
        IBlockState[] column = new IBlockState[256];
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int x = chunkX * 16 + lx, z = chunkZ * 16 + lz;
                if (broken) {
                    island(primer, x, z, lx, lz, column);
                    continue;
                }
                ground(x, z, column);
                for (int y = 0; y < 256; y++) {
                    if (column[y] != null) {
                        primer.setBlockState(lx, y, lz, column[y]);
                    }
                }
            }
        }
        if (!broken) {
            for (Tamarata.Remnant remnant : Tamarata.remnantsNear(seed, chunkX, chunkZ)) {
                build(primer, remnant, chunkX, chunkZ);
            }
        } else {
            Tamarata.fragmentsInChunk(seed, chunkX, chunkZ, (x, y, z, state) -> {
                if (!Tamarata.onIsland(seed, x, y, z)) {
                    primer.setBlockState(x - chunkX * 16, y, z - chunkZ * 16, state);
                }
            });
            TamarataBreaker.INSTANCE.markShattered(chunkX, chunkZ);
        }
        Chunk chunk = new Chunk(world, primer, chunkX, chunkZ);
        Arrays.fill(chunk.getBiomeArray(), (byte) Biome.getIdForBiome(BiomeTamarata.INSTANCE));
        chunk.generateSkylightMap();
        return chunk;
    }

    /** One column of the planet as it was: the natural ground, then the fixed sites over it. */
    private void ground(int x, int z, IBlockState[] column) {
        Tamarata.column(seed, x, z, column);
        if (Math.hypot(x - Tamarata.LANDING_X, z - Tamarata.LANDING_Z) < 17D
                || Math.hypot(x - Tamarata.ARENA_X, z - Tamarata.ARENA_Z) < Tamarata.WALL_RADIUS + 2D) {
            for (int y = SITE_LOW; y <= SITE_HIGH; y++) {
                IBlockState site = Tamarata.site(seed, x, y, z);
                if (site != null) {
                    column[y] = site.getBlock() == Blocks.AIR ? null : site;
                }
            }
        }
    }

    /**
     * One column of the island the landing pad stands on, all that is left whole of the planet.
     * The drifting fragments around it are laid in afterwards, a chunk at a time.
     */
    private void island(ChunkPrimer primer, int x, int z, int lx, int lz, IBlockState[] column) {
        if (Math.hypot(x - Tamarata.LANDING_X, z - Tamarata.LANDING_Z) > Tamarata.ISLAND_RADIUS) {
            return;
        }
        ground(x, z, column);
        for (int y = 0; y < 256; y++) {
            if (column[y] != null && Tamarata.onIsland(seed, x, y, z)) {
                primer.setBlockState(lx, y, lz, column[y]);
            }
        }
    }

    private void build(ChunkPrimer primer, Tamarata.Remnant remnant, int chunkX, int chunkZ) {
        int minY = Math.max(1, remnant.minY()), maxY = Math.min(255, remnant.maxY());
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int x = chunkX * 16 + lx, z = chunkZ * 16 + lz;
                int surface = MathHelper.floor(Tamarata.height(seed, x, z));
                for (int y = minY; y <= maxY; y++) {
                    IBlockState state = remnant.at(x, y, z, surface);
                    if (state != null) {
                        primer.setBlockState(lx, y, lz, state);
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ populating

    /**
     * Sets out everything whose spot lies in this chunk's populating window, which vanilla
     * guarantees is loaded, so nothing here ever reaches into an ungenerated chunk.
     */
    @Override
    public void populate(int chunkX, int chunkZ) {
        boolean broken = ChampionDirector.INSTANCE.planetBroken();
        List<Tamarata.Decoration> all = new ArrayList<>();
        Tamarata.siteDecorations(seed, all);
        if (!broken) {
            // The window spans the corners of four chunks; anything set out in it belongs to a remnant touching one of them.
            for (int i = 0; i <= 1; i++) {
                for (int j = 0; j <= 1; j++) {
                    for (Tamarata.Remnant remnant : Tamarata.remnantsNear(seed, chunkX + i, chunkZ + j)) {
                        remnant.decorations(all);
                    }
                }
            }
        }
        int minX = chunkX * 16 + 8, minZ = chunkZ * 16 + 8;
        Set<String> done = new HashSet<>();
        for (Tamarata.Decoration d : all) {
            if (d.x < minX || d.x > minX + 15 || d.z < minZ || d.z > minZ + 15) {
                if (d.kind != Tamarata.Decoration.Kind.PAD) {
                    continue;
                }
            }
            String key = d.kind + ":" + d.x + ":" + d.z;
            if (!done.add(key)) {
                continue;
            }
            if (broken && d.kind != Tamarata.Decoration.Kind.PAD) {
                continue;
            }
            switch (d.kind) {
                case PAD:
                    pad(minX, minZ);
                    break;
                case BANNER:
                    banner(d);
                    break;
                case SKULL:
                    skull(d);
                    break;
                case ARMOUR_STAND:
                    armourStand(d);
                    break;
            }
        }
    }

    /** The launch pad: nine pad blocks, which Galacticraft joins into a working pad by itself. */
    private void pad(int minX, int minZ) {
        Block block = Block.REGISTRY.getObject(new ResourceLocation("galacticraftcore", "landing_pad"));
        if (block == Blocks.AIR) {
            return;
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int x = Tamarata.LANDING_X + dx, z = Tamarata.LANDING_Z + dz;
                if (x < minX || x > minX + 15 || z < minZ || z > minZ + 15) {
                    continue;
                }
                BlockPos pos = new BlockPos(x, Tamarata.LANDING_Y, z);
                if (world.isAirBlock(pos)) {
                    world.setBlockState(pos, block.getDefaultState(), 3);
                }
            }
        }
    }

    private int surface(int x, int z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, 140, z);
        while (pos.getY() > 1) {
            IBlockState state = world.getBlockState(pos);
            if (state.getMaterial().blocksMovement() && state.getBlock() != Blocks.IRON_BARS && state.getBlock() != Blocks.SPRUCE_FENCE) {
                return pos.getY();
            }
            pos.move(EnumFacing.DOWN);
        }
        return -1;
    }

    /** A torn banner on a post: the colours of one of the armies that came for him and fell. */
    private void banner(Tamarata.Decoration d) {
        int y = d.y >= 0 ? d.y : surface(d.x, d.z) + 1;
        if (y <= 1) {
            return;
        }
        BlockPos post = new BlockPos(d.x, y, d.z);
        if (!world.isAirBlock(post) || !world.isAirBlock(post.up())) {
            return;
        }
        world.setBlockState(post, Blocks.SPRUCE_FENCE.getDefaultState(), 2);
        BlockPos at = post.up();
        world.setBlockState(at, Blocks.STANDING_BANNER.getDefaultState().withProperty(BlockBanner.ROTATION, ((int) d.facing) & 15), 2);
        TileEntity tile = world.getTileEntity(at);
        if (tile instanceof TileEntityBanner) {
            int[] bases = {0, 1, 3, 8};
            int[] trims = {1, 15, 14, 0};
            String[][] torn = {{"gru", "bo"}, {"cbo", "gra"}, {"bri", "gru"}, {"gra", "cr"}};
            int v = Math.floorMod(d.variant, 4);
            NBTTagCompound tag = new NBTTagCompound();
            tile.writeToNBT(tag);
            tag.setInteger("Base", bases[v]);
            NBTTagList patterns = new NBTTagList();
            for (String pattern : torn[v]) {
                NBTTagCompound layer = new NBTTagCompound();
                layer.setString("Pattern", pattern);
                layer.setInteger("Color", pattern.equals("gru") || pattern.equals("gra") ? 3 : trims[v]);
                patterns.appendTag(layer);
            }
            tag.setTag("Patterns", patterns);
            tile.readFromNBT(tag);
            tile.markDirty();
        }
    }

    private void skull(Tamarata.Decoration d) {
        int y = surface(d.x, d.z) + 1;
        if (y <= 1) {
            return;
        }
        BlockPos at = new BlockPos(d.x, y, d.z);
        if (!world.isAirBlock(at)) {
            return;
        }
        world.setBlockState(at, Blocks.SKULL.getDefaultState().withProperty(BlockSkull.FACING, EnumFacing.UP), 2);
        TileEntity tile = world.getTileEntity(at);
        if (tile instanceof TileEntitySkull) {
            ((TileEntitySkull) tile).setType(d.variant);
            ((TileEntitySkull) tile).setSkullRotation(((int) d.facing) & 15);
            tile.markDirty();
        }
    }

    /** A soldier's armour, still standing where its wearer fell: dented, rusted, and a piece or two missing. */
    private void armourStand(Tamarata.Decoration d) {
        int y = surface(d.x, d.z) + 1;
        if (y <= 1 || !world.isAirBlock(new BlockPos(d.x, y, d.z)) || !world.isAirBlock(new BlockPos(d.x, y + 1, d.z))) {
            return;
        }
        Random random = new Random(d.seed);
        EntityArmorStand stand = new EntityArmorStand(world, d.x + 0.5D, y, d.z + 0.5D);
        stand.rotationYaw = d.facing;
        NBTTagCompound tag = new NBTTagCompound();
        stand.writeEntityToNBT(tag);
        tag.setBoolean("ShowArms", true);
        tag.setBoolean("NoBasePlate", random.nextBoolean());
        NBTTagCompound pose = new NBTTagCompound();
        pose.setTag("Head", angles(random.nextFloat() * 40F - 10F, random.nextFloat() * 50F - 25F, random.nextFloat() * 30F - 15F));
        pose.setTag("Body", angles(random.nextFloat() * 10F, 0F, random.nextFloat() * 8F - 4F));
        pose.setTag("RightArm", angles(-random.nextFloat() * 80F, 0F, random.nextFloat() * 20F));
        pose.setTag("LeftArm", angles(-random.nextFloat() * 30F, 0F, -random.nextFloat() * 25F));
        tag.setTag("Pose", pose);
        stand.readEntityFromNBT(tag);
        Item[][] sets = {
                {Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS},
                {Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS},
                {Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS}};
        Item[] set = sets[Math.floorMod(d.variant, sets.length)];
        EntityEquipmentSlot[] slots = {EntityEquipmentSlot.HEAD, EntityEquipmentSlot.CHEST, EntityEquipmentSlot.LEGS, EntityEquipmentSlot.FEET};
        for (int i = 0; i < 4; i++) {
            if (random.nextInt(5) == 0) {
                continue;
            }
            stand.setItemStackToSlot(slots[i], dented(set[i], random));
        }
        if (random.nextInt(3) == 0) {
            stand.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, dented(random.nextBoolean() ? Items.IRON_SWORD : Items.IRON_AXE, random));
        }
        world.spawnEntity(stand);
    }

    private static ItemStack dented(Item item, Random random) {
        ItemStack stack = new ItemStack(item);
        if (stack.isItemStackDamageable()) {
            stack.setItemDamage((int) (stack.getMaxDamage() * (0.55F + random.nextFloat() * 0.4F)));
        }
        return stack;
    }

    private static NBTTagList angles(float x, float y, float z) {
        NBTTagList list = new NBTTagList();
        list.appendTag(new NBTTagFloat(x));
        list.appendTag(new NBTTagFloat(y));
        list.appendTag(new NBTTagFloat(z));
        return list;
    }

    // ------------------------------------------------------------------ the rest

    @Override
    public boolean generateStructures(Chunk chunk, int x, int z) {
        return false;
    }

    @Override
    public List<Biome.SpawnListEntry> getPossibleCreatures(EnumCreatureType creatureType, BlockPos pos) {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public BlockPos getNearestStructurePos(World world, String structureName, BlockPos position, boolean findUnexplored) {
        return null;
    }

    @Override
    public void recreateStructures(Chunk chunk, int x, int z) {
    }

    @Override
    public boolean isInsideStructure(World world, String structureName, BlockPos pos) {
        return false;
    }
}
