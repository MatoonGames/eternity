package com.matoon.eternity.compat.galacticraft;

import com.matoon.eternity.Eternity;
import com.matoon.eternity.EternityConfig;
import com.matoon.eternity.client.render.TamarataSky;
import com.matoon.eternity.world.ChampionDirector;
import com.matoon.eternity.world.Tamarata;
import micdoodle8.mods.galacticraft.api.galaxies.CelestialBody;
import micdoodle8.mods.galacticraft.api.prefab.world.gen.WorldProviderSpace;
import micdoodle8.mods.galacticraft.api.vector.Vector3;
import micdoodle8.mods.galacticraft.api.world.EnumAtmosphericGas;
import micdoodle8.mods.galacticraft.api.world.IZeroGDimension;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
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
 * Tamarata as a world: a long, hot day under a dusty sky, with air you can breathe and gravity
 * like home, and no meteors (nothing falls here that he did not throw). Once he has broken it, the
 * air is gone and nothing falls at all: what is left of the planet drifts, and so do you.
 */
public class WorldProviderTamarata extends WorldProviderSpace implements IZeroGDimension {

    @Override
    public DimensionType getDimensionType() {
        return TamarataPlanet.type();
    }

    @Override
    protected void init() {
        hasSkyLight = true;
        biomeProvider = new BiomeProviderSingle(BiomeTamarata.INSTANCE);
    }

    @Override
    public BiomeProvider getBiomeProvider() {
        return biomeProvider;
    }

    @Override
    public IChunkGenerator createChunkGenerator() {
        return new ChunkGeneratorTamarata(world, world.getSeed(), false);
    }

    @Override
    public Class<? extends IChunkGenerator> getChunkProviderClass() {
        return ChunkGeneratorTamarata.class;
    }

    /** Whether the planet has been broken, as this side knows it. */
    public boolean broken() {
        return world != null && world.isRemote ? Eternity.proxy.tamarataBroken() : ChampionDirector.INSTANCE.planetBroken();
    }

    // ------------------------------------------------------------------ the planet

    @Override
    public float getGravity() {
        return broken() ? 0.08F : 0F;
    }

    @Override
    public float getArrowGravity() {
        return broken() ? 0F : 0.05F;
    }

    @Override
    public boolean hasBreathableAtmosphere() {
        return !broken();
    }

    @Override
    public boolean isGasPresent(EnumAtmosphericGas gas) {
        return !broken() && super.isGasPresent(gas);
    }

    @Override
    public boolean hasNoAtmosphere() {
        return broken();
    }

    /** The server sends every chunk already lit, and the client relighting each broken one again is slow. */
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
        return 1D;
    }

    @Override
    public boolean canSpaceshipTierPass(int tier) {
        return tier >= EternityConfig.champion.tierRequired;
    }

    @Override
    public float getFallDamageModifier() {
        return 1F;
    }

    @Override
    public CelestialBody getCelestialBody() {
        return TamarataPlanet.planet();
    }

    @Override
    public float getSolarSize() {
        return 0.45F;
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
        return 36000L;
    }

    @Override
    public boolean canCoordinateBeSpawn(int x, int z) {
        return true;
    }

    @Override
    public BlockPos getSpawnPoint() {
        return new BlockPos(Tamarata.LANDING_X, Tamarata.LANDING_Y, Tamarata.LANDING_Z + 4);
    }

    @Override
    public int getAverageGroundLevel() {
        return Tamarata.LANDING_Y;
    }

    // ------------------------------------------------------------------ broken: no up, no down

    @Override
    public boolean inFreefall(Entity entity) {
        return broken();
    }

    @Override
    public void setInFreefall(Entity entity) {
        entity.fallDistance = 0F;
    }

    // ------------------------------------------------------------------ how it looks

    @Override
    public Vector3 getFogColor() {
        return broken() ? new Vector3(0.02D, 0.01D, 0.03D) : new Vector3(0.78D, 0.47D, 0.3D);
    }

    @Override
    public Vector3 getSkyColor() {
        return broken() ? new Vector3(0D, 0D, 0D) : new Vector3(0.79D, 0.47D, 0.29D);
    }

    @Override
    public float getStarBrightness(float partialTicks) {
        return broken() ? 1F : 0.15F;
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
        return TamarataSky.INSTANCE;
    }

    @SideOnly(Side.CLIENT)
    @Nullable
    @Override
    public IRenderHandler getCloudRenderer() {
        return TamarataSky.NOTHING;
    }

    @SideOnly(Side.CLIENT)
    @Nullable
    @Override
    public IRenderHandler getWeatherRenderer() {
        return TamarataSky.NOTHING;
    }
}
