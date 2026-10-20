package com.matoon.eternity.ability;

import com.matoon.eternity.Eternity;
import lucraft.mods.lucraftcore.superpowers.abilities.Ability;
import lucraft.mods.lucraftcore.superpowers.abilities.AbilityEntry;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Eternity's powers as Lucraft abilities, and which absorbed entity grants which. */
@Mod.EventBusSubscriber(modid = Eternity.MODID)
public final class EternityAbilities {

    /** Ability names granted by an absorbed Eternity, in the order the tooltip lists them. */
    public static final String[] ETERNITY_POWERS = {"unmake", "void_lances", "starfall", "shatter", "singularity", "rift_step", "rift_gate"};

    private EternityAbilities() {
    }

    @SubscribeEvent
    public static void register(RegistryEvent.Register<AbilityEntry> event) {
        event.getRegistry().registerAll(
                new AbilityEntry(AbilityUnmake.class, new ResourceLocation(Eternity.MODID, "unmake")),
                new AbilityEntry(AbilityVoidLances.class, new ResourceLocation(Eternity.MODID, "void_lances")),
                new AbilityEntry(AbilityStarfall.class, new ResourceLocation(Eternity.MODID, "starfall")),
                new AbilityEntry(AbilityShatter.class, new ResourceLocation(Eternity.MODID, "shatter")),
                new AbilityEntry(AbilitySingularity.class, new ResourceLocation(Eternity.MODID, "singularity")),
                new AbilityEntry(AbilityRiftStep.class, new ResourceLocation(Eternity.MODID, "rift_step")),
                new AbilityEntry(AbilityRiftGate.class, new ResourceLocation(Eternity.MODID, "rift_gate")));
    }

    /** Everything an absorbed Eternity lets you do, with cooldowns in ticks. */
    public static void grantEternity(EntityLivingBase entity, Ability.AbilityMap map) {
        map.put("unmake", new AbilityUnmake(entity).setMaxCooldown(40));
        map.put("void_lances", new AbilityVoidLances(entity).setMaxCooldown(100));
        map.put("starfall", new AbilityStarfall(entity).setMaxCooldown(300));
        map.put("shatter", new AbilityShatter(entity).setMaxCooldown(240));
        map.put("singularity", new AbilitySingularity(entity).setMaxCooldown(1200));
        map.put("rift_step", new AbilityRiftStep(entity).setMaxCooldown(60));
        map.put("rift_gate", new AbilityRiftGate(entity).setMaxCooldown(1200));
    }
}
