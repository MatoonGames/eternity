package com.matoon.eternity.world;

import com.matoon.eternity.client.render.StarChamberSky;
import net.minecraft.entity.Entity;
import net.minecraft.init.Biomes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.biome.BiomeProviderSingle;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.client.IRenderHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

/**
 * Odin's Vault: deep underground, so no sky, no day, no weather; nothing but the light of its
 * own lamps, and away from them, the dark.
 */
public class WorldProviderVault extends WorldProvider {

    @Override
    protected void init() {
        biomeProvider = new BiomeProviderSingle(Biomes.VOID);
        hasSkyLight = false;
    }

    @Override
    public DimensionType getDimensionType() {
        return OdinsVault.type();
    }

    @Override
    public IChunkGenerator createChunkGenerator() {
        return new ChunkGeneratorVault(world);
    }

    /** Darker than the Nether away from the lamps, but never quite black, so the shapes of the hall still read. */
    @Override
    protected void generateLightBrightnessTable() {
        for (int i = 0; i <= 15; i++) {
            float f = 1F - i / 15F;
            lightBrightnessTable[i] = (1F - f) / (f * 3F + 1F) * 0.94F + 0.06F;
        }
    }

    /**
     * The vault's light is the colour of old gold where its lamps and braziers burn, and its
     * shadows go the cold blue of deep stone: blocks well lit are warmed, blocks in the dark cooled.
     */
    @SideOnly(Side.CLIENT)
    @Override
    public void getLightmapColors(float partialTicks, float sunBrightness, float skyLight, float blockLight, float[] colors) {
        float warm = MathHelper.clamp((blockLight - 0.25F) / 0.6F, 0F, 1F);
        colors[0] = MathHelper.clamp(colors[0] * (0.8F + 0.3F * warm), 0F, 1F);
        colors[1] = MathHelper.clamp(colors[1] * (0.9F + 0.04F * warm), 0F, 1F);
        colors[2] = MathHelper.clamp(colors[2] * (1.18F - 0.42F * warm), 0F, 1F);
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
        return new BlockPos(OdinsVault.ARRIVE_X, OdinsVault.ARRIVE_Y, OdinsVault.ARRIVE_Z);
    }

    @Override
    public float calculateCelestialAngle(long worldTime, float partialTicks) {
        return 0.5F;
    }

    @Override
    public boolean isSkyColored() {
        return false;
    }

    @Override
    public double getVoidFogYFactor() {
        return 1D;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public Vec3d getFogColor(float celestialAngle, float partialTicks) {
        return new Vec3d(0.035D, 0.04D, 0.05D);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public Vec3d getSkyColor(Entity camera, float partialTicks) {
        return Vec3d.ZERO;
    }

    @SideOnly(Side.CLIENT)
    @Nullable
    @Override
    public IRenderHandler getSkyRenderer() {
        return StarChamberSky.NOTHING;
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
