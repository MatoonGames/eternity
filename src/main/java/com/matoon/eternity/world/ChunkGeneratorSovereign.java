package com.matoon.eternity.world;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Biomes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.IChunkGenerator;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Generates the Sovereign station from {@link SovereignStation#blockAt}; everything else is space. The
 * game never lights the lamps of blocks laid straight into a chunk, and lighting them afterwards
 * would reach nobody's screen in time; so each chunk is generated already lit by the station's own seams.
 */
public class ChunkGeneratorSovereign implements IChunkGenerator {

    private static final int MIN_Y = SovereignStation.minY(), MAX_Y = SovereignStation.maxY();

    private final World world;

    public ChunkGeneratorSovereign(World world) {
        this.world = world;
    }

    @Override
    public Chunk generateChunk(int chunkX, int chunkZ) {
        ChunkPrimer primer = new ChunkPrimer();
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int x = chunkX * 16 + lx;
                int z = chunkZ * 16 + lz;
                for (int y = MIN_Y; y <= MAX_Y; y++) {
                    IBlockState state = SovereignStation.blockAt(x, y, z);
                    if (state != null) {
                        primer.setBlockState(lx, y, lz, state);
                    }
                }
            }
        }
        Chunk chunk = new Chunk(world, primer, chunkX, chunkZ);
        Arrays.fill(chunk.getBiomeArray(), (byte) Biome.getIdForBiome(Biomes.VOID));
        chunk.generateSkylightMap();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                for (int y = MIN_Y; y <= MAX_Y; y++) {
                    int light = SovereignStation.lightAt(chunkX * 16 + lx, y, chunkZ * 16 + lz);
                    if (light > 0) {
                        chunk.setLightFor(EnumSkyBlock.BLOCK, pos.setPos(chunkX * 16 + lx, y, chunkZ * 16 + lz), light);
                    }
                }
            }
        }
        chunk.setLightPopulated(true);
        return chunk;
    }

    @Override
    public void populate(int x, int z) {
    }

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
