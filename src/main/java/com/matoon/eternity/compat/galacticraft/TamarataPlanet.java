package com.matoon.eternity.compat.galacticraft;

import com.matoon.eternity.Eternity;
import com.matoon.eternity.EternityConfig;
import com.matoon.eternity.world.Tamarata;
import micdoodle8.mods.galacticraft.api.GalacticraftRegistry;
import micdoodle8.mods.galacticraft.api.galaxies.CelestialBody;
import micdoodle8.mods.galacticraft.api.galaxies.GalaxyRegistry;
import micdoodle8.mods.galacticraft.api.galaxies.Planet;
import micdoodle8.mods.galacticraft.api.world.AtmosphereInfo;
import micdoodle8.mods.galacticraft.api.world.EnumAtmosphericGas;
import micdoodle8.mods.galacticraft.core.GalacticraftCore;
import micdoodle8.mods.galacticraft.core.util.WorldUtil;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.DimensionType;
import net.minecraftforge.common.MinecraftForge;

/**
 * Tamarata, out past Neptune: hot, windy, and with nothing in its air to breathe. Galacticraft
 * registers its dimension from what is set here, as it does its own planets; this only says
 * what the planet is, where it orbits, and how arrivals come down.
 */
public final class TamarataPlanet {

    private static Planet planet;
    private static DimensionType type;

    private TamarataPlanet() {
    }

    /** In preInit, after Galacticraft has made the solar system. */
    public static void register() {
        planet = new Planet("tamarata").setParentSolarSystem(GalacticraftCore.solarSystemSol);
        planet.setRingColorRGB(0.95F, 0.45F, 0.15F);
        planet.setPhaseShift(2.4F);
        planet.setRelativeSize(1.15F);
        planet.setRelativeDistanceFromCenter(new CelestialBody.ScalableDistance(2.6F, 2.6F));
        planet.setRelativeOrbitTime(40F);
        planet.setBodyIcon(new ResourceLocation(Eternity.MODID, "textures/gui/celestialbodies/tamarata.png"));
        planet.setDimensionInfo(Tamarata.dimension(), WorldProviderTamarata.class);
        planet.setTierRequired(EternityConfig.champion.tierRequired);
        planet.setAtmosphere(new AtmosphereInfo(true, false, false, 0F, 0.6F, 1F));
        planet.atmosphereComponent(EnumAtmosphericGas.NITROGEN).atmosphereComponent(EnumAtmosphericGas.OXYGEN)
                .atmosphereComponent(EnumAtmosphericGas.ARGON);
        GalaxyRegistry.register(planet);
        GalacticraftRegistry.registerTeleportType(WorldProviderTamarata.class, new TeleportTypeTamarata());
        GalacticraftRegistry.registerRocketGui(WorldProviderTamarata.class,
                new ResourceLocation("galacticraftplanets", "textures/gui/mars_rocket_gui.png"));
        MinecraftForge.EVENT_BUS.register(TamarataGravity.INSTANCE);
    }

    /** In postInit, once Galacticraft has registered the dimension. */
    public static void postInit() {
        type = WorldUtil.getDimensionTypeById(Tamarata.dimension());
    }

    public static DimensionType type() {
        return type;
    }

    public static Planet planet() {
        return planet;
    }
}
