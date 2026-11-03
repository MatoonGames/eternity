package com.matoon.eternity.compat.galacticraft;

import com.matoon.eternity.Eternity;
import com.matoon.eternity.EternityConfig;
import com.matoon.eternity.world.SovereignStation;
import micdoodle8.mods.galacticraft.api.GalacticraftRegistry;
import micdoodle8.mods.galacticraft.api.galaxies.CelestialBody;
import micdoodle8.mods.galacticraft.api.galaxies.GalaxyRegistry;
import micdoodle8.mods.galacticraft.api.galaxies.Moon;
import micdoodle8.mods.galacticraft.api.world.AtmosphereInfo;
import micdoodle8.mods.galacticraft.api.world.EnumAtmosphericGas;
import micdoodle8.mods.galacticraft.core.GalacticraftCore;
import micdoodle8.mods.galacticraft.core.util.WorldUtil;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.DimensionType;

/**
 * The Sovereign station, in orbit beside the Earth: on Galacticraft's map it is one more thing
 * circling home, close in, gold. Galacticraft registers its dimension from what is set here.
 */
public final class SovereignMoon {

    private static Moon moon;
    private static DimensionType type;

    private SovereignMoon() {
    }

    /** In preInit, after Galacticraft has made the solar system. */
    public static void register() {
        moon = new Moon("sovereign").setParentPlanet(GalacticraftCore.planetOverworld);
        moon.setRingColorRGB(1F, 0.8F, 0.3F);
        moon.setPhaseShift(1.1F);
        moon.setRelativeSize(0.12F);
        moon.setRelativeDistanceFromCenter(new CelestialBody.ScalableDistance(6F, 6F));
        moon.setRelativeOrbitTime(0.4F);
        moon.setBodyIcon(new ResourceLocation(Eternity.MODID, "textures/gui/celestialbodies/sovereign.png"));
        moon.setDimensionInfo(SovereignStation.dimension(), WorldProviderSovereign.class);
        moon.setTierRequired(EternityConfig.sovereign.tierRequired);
        moon.setAtmosphere(new AtmosphereInfo(true, false, false, 0F, 0.2F, 0F));
        moon.atmosphereComponent(EnumAtmosphericGas.NITROGEN).atmosphereComponent(EnumAtmosphericGas.OXYGEN);
        GalaxyRegistry.registerMoon(moon);
        GalacticraftRegistry.registerTeleportType(WorldProviderSovereign.class, new TeleportTypeSovereign());
        GalacticraftRegistry.registerRocketGui(WorldProviderSovereign.class,
                new ResourceLocation("galacticraftcore", "textures/gui/overworld_rocket_gui.png"));
    }

    /** In postInit, once Galacticraft has registered the dimension. */
    public static void postInit() {
        type = WorldUtil.getDimensionTypeById(SovereignStation.dimension());
    }

    public static DimensionType type() {
        return type;
    }

    public static Moon moon() {
        return moon;
    }
}
