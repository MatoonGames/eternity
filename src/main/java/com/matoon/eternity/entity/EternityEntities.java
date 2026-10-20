package com.matoon.eternity.entity;

import com.matoon.eternity.Eternity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;

@Mod.EventBusSubscriber(modid = Eternity.MODID)
public final class EternityEntities {

    private EternityEntities() {
    }

    @SubscribeEvent
    public static void register(RegistryEvent.Register<EntityEntry> event) {
        event.getRegistry().register(EntityEntryBuilder.create()
                .entity(EntityEternity.class)
                .id(new ResourceLocation(Eternity.MODID, "eternity"), 0)
                .name(Eternity.MODID + ".eternity")
                .tracker(256, 1, true)
                .build());
        event.getRegistry().register(EntityEntryBuilder.create()
                .entity(EntityAstralRegulator.class)
                .id(new ResourceLocation(Eternity.MODID, "astral_regulator"), 1)
                .name(Eternity.MODID + ".astral_regulator")
                .tracker(256, 1, true)
                .build());
        event.getRegistry().register(EntityEntryBuilder.create()
                .entity(EntityCosmicComet.class)
                .id(new ResourceLocation(Eternity.MODID, "cosmic_comet"), 2)
                .name(Eternity.MODID + ".cosmic_comet")
                .tracker(256, 1, true)
                .build());
        event.getRegistry().register(EntityEntryBuilder.create()
                .entity(EntityLivingTribunal.class)
                .id(new ResourceLocation(Eternity.MODID, "living_tribunal"), 3)
                .name(Eternity.MODID + ".living_tribunal")
                .tracker(256, 1, true)
                .build());
    }
}
