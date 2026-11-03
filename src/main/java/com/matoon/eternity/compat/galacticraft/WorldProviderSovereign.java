package com.matoon.eternity.compat.galacticraft;

import com.matoon.eternity.EternityConfig;
import com.matoon.eternity.world.ChunkGeneratorSovereign;
import com.matoon.eternity.world.SovereignStation;
import micdoodle8.mods.galacticraft.api.galaxies.CelestialBody;
import micdoodle8.mods.galacticraft.api.prefab.world.gen.WorldProviderSpace;
import micdoodle8.mods.galacticraft.api.vector.Vector3;
import micdoodle8.mods.galacticraft.core.client.SkyProviderOrbit;
import net.minecraft.block.Block;
import net.minecraft.init.Biomes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.biome.BiomeProviderSingle;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.client.IRenderHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * The Sovereign as a world: the station, the Earth turning under it, the sun and the stars, and
 * nothing else. The Sovereign kept their own air and their own weight in it, and both still
 * hold: you breathe, and you stand, as long as you are aboard.
 */
public class WorldProviderSovereign extends WorldProviderSpace {

    @SideOnly(Side.CLIENT)
    private IRenderHandler sky;

    @Override
    public DimensionType getDimensionType() {
        return SovereignMoon.type();
    }

    @Override
    protected void init() {
        hasSkyLight = true;
        biomeProvider = new BiomeProviderSingle(Biomes.VOID);
    }

    @Override
    public BiomeProvider getBiomeProvider() {
        return biomeProvider;
    }

    @Override
    public IChunkGenerator createChunkGenerator() {
        return new ChunkGeneratorSovereign(world);
    }

    @Override
    public Class<? extends IChunkGenerator> getChunkProviderClass() {
        return ChunkGeneratorSovereign.class;
    }

    @Override
    public float getGravity() {
        return 0F;
    }

    @Override
    public float getArrowGravity() {
        return 0.05F;
    }

    @Override
    public boolean hasBreathableAtmosphere() {
        return true;
    }

    @Override
    public boolean shouldClientCheckLighting() {
        return false;
    }

    @Override
    public double getMeteorFrequency() {
        return 0D;
    }

    @Override
    public double getFuelUsageMultiplier() {
        return 0.5D;
    }

    @Override
    public boolean canSpaceshipTierPass(int tier) {
        return tier >= EternityConfig.sovereign.tierRequired;
    }

    @Override
    public float getFallDamageModifier() {
        return 1F;
    }

    @Override
    public CelestialBody getCelestialBody() {
        return SovereignMoon.moon();
    }

    @Override
    public float getSolarSize() {
        return 1.1F;
    }

    @Override
    public int getDungeonSpacing() {
        return 0;
    }

    @Nullable
    @Override
    public ResourceLocation getDungeonChestType() {
        return null;
    }

    @Override
    public List<Block> getSurfaceBlocks() {
        return Collections.emptyList();
    }

    @Override
    public boolean hasSunset() {
        return false;
    }

    @Override
    public long getDayLength() {
        return 24000L;
    }

    @Override
    public boolean canRespawnHere() {
        return false;
    }

    @Override
    public boolean canCoordinateBeSpawn(int x, int z) {
        return true;
    }

    @Override
    public BlockPos getSpawnPoint() {
        return SovereignStation.LANDING;
    }

    @Override
    public int getAverageGroundLevel() {
        return SovereignStation.FLOOR;
    }

    @Override
    public Vector3 getFogColor() {
        return new Vector3(0D, 0D, 0D);
    }

    @Override
    public Vector3 getSkyColor() {
        return new Vector3(0D, 0D, 0D);
    }

    @Override
    public float getStarBrightness(float partialTicks) {
        return 1F;
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
        if (sky == null) {
            // The Earth below, as from any station in orbit round it.
            sky = new SkyProviderOrbit(new ResourceLocation("galacticraftcore", "textures/gui/celestialbodies/earth.png"), true, true);
        }
        return sky;
    }

    @SideOnly(Side.CLIENT)
    @Nullable
    @Override
    public IRenderHandler getCloudRenderer() {
        return com.matoon.eternity.client.render.TamarataSky.NOTHING;
    }

    @SideOnly(Side.CLIENT)
    @Nullable
    @Override
    public IRenderHandler getWeatherRenderer() {
        return com.matoon.eternity.client.render.TamarataSky.NOTHING;
    }
}
