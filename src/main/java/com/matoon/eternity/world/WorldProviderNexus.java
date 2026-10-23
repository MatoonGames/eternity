package com.matoon.eternity.world;

import com.matoon.eternity.client.NexusClient;
import com.matoon.eternity.client.render.NexusSky;
import com.matoon.eternity.client.render.StarChamberSky;
import net.minecraft.client.audio.MusicTicker;
import net.minecraft.entity.Entity;
import net.minecraft.init.Biomes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.biome.BiomeProviderSingle;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.client.IRenderHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

/** The Nexus of All Realities: no day, no weather, a neon sky, and light everywhere. */
public class WorldProviderNexus extends WorldProvider {

    @Override
    protected void init() {
        biomeProvider = new BiomeProviderSingle(Biomes.VOID);
    }

    @Override
    public DimensionType getDimensionType() {
        return Nexus.type();
    }

    @Override
    public IChunkGenerator createChunkGenerator() {
        return new ChunkGeneratorNexus(world);
    }

    /** Lit from everywhere and nowhere: even the shade is bright. */
    @Override
    protected void generateLightBrightnessTable() {
        for (int i = 0; i <= 15; i++) {
            lightBrightnessTable[i] = 0.55F + 0.45F * i / 15F;
        }
    }

    @Override
    public boolean canRespawnHere() {
        return false;
    }

    @Override
    public boolean isSurfaceWorld() {
        return false;
    }

    @Override
    public boolean canCoordinateBeSpawn(int x, int z) {
        return false;
    }

    @Override
    public BlockPos getSpawnPoint() {
        return new BlockPos(Nexus.HUB_X, Nexus.HUB_Y, Nexus.HUB_Z);
    }

    @Override
    public float calculateCelestialAngle(long worldTime, float partialTicks) {
        return 0.5F;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public Vec3d getFogColor(float celestialAngle, float partialTicks) {
        return new Vec3d(0.55D, 0.12D, 0.5D);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public Vec3d getSkyColor(Entity camera, float partialTicks) {
        return new Vec3d(0.5D, 0.1D, 0.45D);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public boolean doesXZShowFog(int x, int z) {
        return false;
    }

    @SideOnly(Side.CLIENT)
    @Nullable
    @Override
    public IRenderHandler getSkyRenderer() {
        return NexusSky.INSTANCE;
    }

    /** Its own music, in place of the overworld's. */
    @SideOnly(Side.CLIENT)
    @Nullable
    @Override
    public MusicTicker.MusicType getMusicType() {
        return NexusClient.music();
    }

    @SideOnly(Side.CLIENT)
    @Nullable
    @Override
    public IRenderHandler getCloudRenderer() {
        return StarChamberSky.NOTHING;
    }

    @SideOnly(Side.CLIENT)
    @Nullable
    @Override
    public IRenderHandler getWeatherRenderer() {
        return StarChamberSky.NOTHING;
    }
}
