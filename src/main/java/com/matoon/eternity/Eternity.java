package com.matoon.eternity;

import com.matoon.eternity.command.CommandEternity;
import com.matoon.eternity.command.CommandTpDimension;
import com.matoon.eternity.compat.InfinityTemples;
import com.matoon.eternity.compat.Tickrate;
import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.world.CometDirector;
import com.matoon.eternity.world.Director;
import com.matoon.eternity.world.InBetweenerDirector;
import com.matoon.eternity.world.MastersPowers;
import com.matoon.eternity.world.Nexus;
import com.matoon.eternity.world.OrderAndChaos;
import com.matoon.eternity.world.NexusDirector;
import com.matoon.eternity.world.RegulatorPowers;
import com.matoon.eternity.world.StarChamber;
import com.matoon.eternity.world.RelicTracker;
import com.matoon.eternity.world.TribunalDirector;
import com.matoon.eternity.world.TribunalPowers;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = Eternity.MODID, name = Eternity.NAME, version = Eternity.VERSION,
        acceptedMinecraftVersions = "[1.12.2]",
        dependencies = "required-after:lucraftcore;required-after:infinity;"
                + "required-after:heroesexpansion;required-after:galacticraftcore;required-after:tickratechanger")
public class Eternity {

    public static final String MODID = "eternity";
    public static final String NAME = "Eternity";
    public static final String VERSION = "2.0.0";
    public static final Logger LOG = LogManager.getLogger(NAME);

    @SidedProxy(clientSide = "com.matoon.eternity.client.ClientProxy", serverSide = "com.matoon.eternity.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        EternityNetwork.init();
        MinecraftForge.EVENT_BUS.register(Director.INSTANCE);
        MinecraftForge.EVENT_BUS.register(RelicTracker.INSTANCE);
        MinecraftForge.EVENT_BUS.register(CometDirector.INSTANCE);
        MinecraftForge.EVENT_BUS.register(TribunalDirector.INSTANCE);
        MinecraftForge.EVENT_BUS.register(RegulatorPowers.INSTANCE);
        MinecraftForge.EVENT_BUS.register(TribunalPowers.INSTANCE);
        MinecraftForge.EVENT_BUS.register(NexusDirector.INSTANCE);
        MinecraftForge.EVENT_BUS.register(InBetweenerDirector.INSTANCE);
        MinecraftForge.EVENT_BUS.register(OrderAndChaos.INSTANCE);
        MinecraftForge.EVENT_BUS.register(MastersPowers.INSTANCE);
        StarChamber.register();
        Nexus.register();
        proxy.preInit();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        InfinityTemples.disable();
    }

    @Mod.EventHandler
    public void serverAboutToStart(FMLServerAboutToStartEvent event) {
        // Before any world loads and generates its first chunks.
        InfinityTemples.disable();
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandEternity());
        event.registerServerCommand(new CommandTpDimension());
    }

    @Mod.EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
        // Never leave a player's game running slow after the world closes.
        Tickrate.restore();
    }

    @Mod.EventHandler
    public void serverStopped(FMLServerStoppedEvent event) {
        Director.INSTANCE.reset();
        RelicTracker.INSTANCE.reset();
        CometDirector.INSTANCE.reset();
        TribunalDirector.INSTANCE.reset();
        TribunalPowers.INSTANCE.reset();
        InBetweenerDirector.INSTANCE.reset();
        MastersPowers.INSTANCE.reset();
    }
}
