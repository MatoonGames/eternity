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
        event.getRegistry().register(EntityEntryBuilder.create()
                .entity(EntityInBetweener.class)
                .id(new ResourceLocation(Eternity.MODID, "inbetweener"), 4)
                .name(Eternity.MODID + ".inbetweener")
                .tracker(160, 1, true)
                .build());
        event.getRegistry().register(EntityEntryBuilder.create()
                .entity(EntityOrderChaos.class)
                .id(new ResourceLocation(Eternity.MODID, "order_and_chaos"), 5)
                .name(Eternity.MODID + ".order_and_chaos")
                .tracker(256, 1, true)
                .build());
        event.getRegistry().register(EntityEntryBuilder.create()
                .entity(EntityAdamWarlock.class)
                .id(new ResourceLocation(Eternity.MODID, "adam_warlock"), 6)
                .name(Eternity.MODID + ".adam_warlock")
                .tracker(256, 1, true)
                .build());
        event.getRegistry().register(EntityEntryBuilder.create()
                .entity(EntityChampion.class)
                .id(new ResourceLocation(Eternity.MODID, "champion"), 7)
                .name(Eternity.MODID + ".champion")
                .tracker(256, 1, true)
                .build());
        event.getRegistry().register(EntityEntryBuilder.create()
                .entity(EntityEinherjar.class)
                .id(new ResourceLocation(Eternity.MODID, "einherjar"), 8)
                .name(Eternity.MODID + ".einherjar")
                .tracker(128, 1, true)
                .build());
        event.getRegistry().register(EntityEntryBuilder.create()
                .entity(EntityDestroyer.class)
                .id(new ResourceLocation(Eternity.MODID, "destroyer"), 9)
                .name(Eternity.MODID + ".destroyer")
                .tracker(200, 1, true)
                .build());
        event.getRegistry().register(EntityEntryBuilder.create()
                .entity(EntityDestroyerArmor.class)
                .id(new ResourceLocation(Eternity.MODID, "destroyer_armor"), 10)
                .name(Eternity.MODID + ".destroyer_armor")
                .tracker(200, 1, true)
                .build());
        event.getRegistry().register(EntityEntryBuilder.create()
                .entity(EntityGrandmaster.class)
                .id(new ResourceLocation(Eternity.MODID, "grandmaster"), 11)
                .name(Eternity.MODID + ".grandmaster")
                .tracker(200, 1, true)
                .build());
    }
}
