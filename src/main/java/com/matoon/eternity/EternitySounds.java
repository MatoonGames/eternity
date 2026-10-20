package com.matoon.eternity;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = Eternity.MODID)
public final class EternitySounds {

    private static final List<SoundEvent> ALL = new ArrayList<>();

    // Ambient beds: stereo, played non-positionally.
    public static final SoundEvent AMBIENT_DRONE = create("ambient.drone");
    public static final SoundEvent AMBIENT_WIND = create("ambient.wind");
    public static final SoundEvent AMBIENT_ABYSS = create("ambient.abyss");
    public static final SoundEvent AMBIENT_ENCOUNTER = create("ambient.encounter");
    public static final SoundEvent AMBIENT_RUMBLE = create("ambient.rumble");

    // Carried on the wind: mono, pre-filtered to sound far away.
    public static final SoundEvent FAR_CHAINS = create("far.chains");
    public static final SoundEvent FAR_GROAN = create("far.groan");
    public static final SoundEvent FAR_HORN = create("far.horn");
    public static final SoundEvent FAR_REVERSE = create("far.reverse");
    public static final SoundEvent FAR_NOISE = create("far.noise");
    public static final SoundEvent FAR_RIFT = create("far.rift");

    public static final SoundEvent OMEN_SHIMMER = create("omen.shimmer");
    public static final SoundEvent OMEN_STARE = create("omen.stare");

    public static final SoundEvent RIFT_OPEN = create("rift.open");
    public static final SoundEvent RIFT_LOOP = create("rift.loop");
    public static final SoundEvent RIFT_CLOSE = create("rift.close");
    public static final SoundEvent RIFT_IMPACT = create("rift.impact");

    public static final SoundEvent ENTITY_VOICE = create("entity.voice");
    public static final SoundEvent ENTITY_GLITCH = create("entity.glitch");

    public static final SoundEvent FINALE_DRUMS = create("finale.drums");
    public static final SoundEvent FINALE_AFTERMATH = create("finale.aftermath");

    // The Cosmic Comet. The fly-by is mono; impact and blast are stereo and played non-positionally.
    public static final SoundEvent COMET_FLYBY = create("comet.flyby");
    public static final SoundEvent COMET_IMPACT = create("comet.impact");
    public static final SoundEvent COMET_BLAST = create("comet.blast");

    private EternitySounds() {
    }

    private static SoundEvent create(String name) {
        ResourceLocation id = new ResourceLocation(Eternity.MODID, name);
        SoundEvent event = new SoundEvent(id).setRegistryName(id);
        ALL.add(event);
        return event;
    }

    @SubscribeEvent
    public static void register(RegistryEvent.Register<SoundEvent> event) {
        for (SoundEvent sound : ALL) {
            event.getRegistry().register(sound);
        }
    }
}
