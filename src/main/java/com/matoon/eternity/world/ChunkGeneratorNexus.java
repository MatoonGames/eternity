package com.matoon.eternity.world;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Biomes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.IChunkGenerator;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Fills the Nexus from {@link Nexus#featuresNear}; everything else is void. */
public class ChunkGeneratorNexus implements IChunkGenerator {

    private final World world;

    public ChunkGeneratorNexus(World world) {
        this.world = world;
    }

    @Override
    public Chunk generateChunk(int chunkX, int chunkZ) {
        ChunkPrimer primer = new ChunkPrimer();
        // Each feature only fills the part of its own box that falls in this chunk, so empty
        // void costs nothing. Where two overlap, whichever came first keeps the block.
        int baseX = chunkX * 16;
        int baseZ = chunkZ * 16;
        for (Nexus.Feature feature : Nexus.featuresNear(chunkX, chunkZ, world.getSeed())) {
            int x0 = Math.max(feature.minX, baseX), x1 = Math.min(feature.maxX, baseX + 15);
            int z0 = Math.max(feature.minZ, baseZ), z1 = Math.min(feature.maxZ, baseZ + 15);
            // Nothing is generated under the surface of the abyss.
            int y0 = Math.max(feature.minY, MathHelper.ceil(Nexus.ABYSS_Y)), y1 = Math.min(feature.maxY, 254);
            for (int x = x0; x <= x1; x++) {
                for (int z = z0; z <= z1; z++) {
                    for (int y = y0; y <= y1; y++) {
                        if (primer.getBlockState(x - baseX, y, z - baseZ).getMaterial() != Material.AIR) {
                            continue;
                        }
                        IBlockState state = feature.block(x, y, z);
                        if (state != null) {
                            primer.setBlockState(x - baseX, y, z - baseZ, Nexus.drain(state, x, y, z));
                        }
                    }
                }
            }
        }
        Chunk chunk = new Chunk(world, primer, chunkX, chunkZ);
        Arrays.fill(chunk.getBiomeArray(), (byte) Biome.getIdForBiome(Biomes.VOID));
        chunk.generateSkylightMap();
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
