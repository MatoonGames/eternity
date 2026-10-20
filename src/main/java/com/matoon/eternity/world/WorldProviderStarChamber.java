package com.matoon.eternity.world;

import com.matoon.eternity.client.render.StarChamberSky;
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

/** The Star Chamber: no day, no weather, no way out, and a sky of open space. */
public class WorldProviderStarChamber extends WorldProvider {

    @Override
    protected void init() {
        biomeProvider = new BiomeProviderSingle(Biomes.VOID);
    }

    @Override
    public DimensionType getDimensionType() {
        return StarChamber.type();
    }

    @Override
    public IChunkGenerator createChunkGenerator() {
        return new ChunkGeneratorStarChamber(world);
    }

    /** Dimly lit everywhere, so the hall reads even far from its lights. */
    @Override
    protected void generateLightBrightnessTable() {
        for (int i = 0; i <= 15; i++) {
            lightBrightnessTable[i] = 0.35F + 0.65F * i / 15F;
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
        return new BlockPos(StarChamber.DAIS_X, StarChamber.DAIS_Y, StarChamber.DAIS_Z);
    }

    @Override
    public float calculateCelestialAngle(long worldTime, float partialTicks) {
        return 0.5F;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public Vec3d getFogColor(float celestialAngle, float partialTicks) {
        return new Vec3d(0.05D, 0.02D, 0.09D);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public Vec3d getSkyColor(Entity camera, float partialTicks) {
        return new Vec3d(0.02D, 0.01D, 0.05D);
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
        return StarChamberSky.INSTANCE;
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
