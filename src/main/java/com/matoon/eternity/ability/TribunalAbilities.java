package com.matoon.eternity.ability;

import com.matoon.eternity.Eternity;
import com.matoon.eternity.world.TribunalPowers;
import lucraft.mods.lucraftcore.superpowers.abilities.Ability;
import lucraft.mods.lucraftcore.superpowers.abilities.AbilityEntry;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** The Living Tribunal's powers as Lucraft abilities, granted once he is inside the Regulator. */
@Mod.EventBusSubscriber(modid = Eternity.MODID)
public final class TribunalAbilities {

    /** Ability names granted by an absorbed Tribunal, in the order the tooltip lists them. */
    public static final String[] POWERS = {"gavel", "equitys_gaze", "necessitys_storm", "scales", "sentence", "convergence", "pillars", "cosmic_verdict"};

    private TribunalAbilities() {
    }

    @SubscribeEvent
    public static void register(RegistryEvent.Register<AbilityEntry> event) {
        event.getRegistry().registerAll(
                entry(Gavel.class, "gavel"),
                entry(Gaze.class, "equitys_gaze"),
                entry(Storm.class, "necessitys_storm"),
                entry(Scales.class, "scales"),
                entry(Sentence.class, "sentence"),
                entry(Convergence.class, "convergence"),
                entry(Pillars.class, "pillars"),
                entry(Verdict.class, "cosmic_verdict"));
    }

    private static AbilityEntry entry(Class<? extends Ability> type, String name) {
        return new AbilityEntry(type, new ResourceLocation(Eternity.MODID, name));
    }

    /** Everything an absorbed Tribunal lets you do, with cooldowns in ticks. */
    public static void grant(EntityLivingBase entity, Ability.AbilityMap map) {
        map.put("gavel", new Gavel(entity).setMaxCooldown(80));
        map.put("equitys_gaze", new Gaze(entity).setMaxCooldown(160));
        map.put("necessitys_storm", new Storm(entity).setMaxCooldown(200));
        map.put("scales", new Scales(entity).setMaxCooldown(300));
        map.put("sentence", new Sentence(entity).setMaxCooldown(400));
        map.put("convergence", new Convergence(entity).setMaxCooldown(300));
        map.put("pillars", new Pillars(entity).setMaxCooldown(240));
        map.put("cosmic_verdict", new Verdict(entity).setMaxCooldown(1200));
    }

    /** The Gavel: a sigil where you look, then a fist out of nowhere. */
    public static class Gavel extends AbilityCosmic {
        public Gavel(EntityLivingBase entity) {
            super(entity, "gavel");
        }

        @Override
        protected boolean use() {
            return TribunalPowers.INSTANCE.gavel(entity);
        }
    }

    /** Equity's Gaze: twin beams of white fire from your eyes. */
    public static class Gaze extends AbilityCosmic {
        public Gaze(EntityLivingBase entity) {
            super(entity, "equitys_gaze");
        }

        @Override
        protected boolean use() {
            return TribunalPowers.INSTANCE.gaze(entity);
        }
    }

    /** Necessity's Storm: lightning wherever a sigil burns. */
    public static class Storm extends AbilityCosmic {
        public Storm(EntityLivingBase entity) {
            super(entity, "necessitys_storm");
        }

        @Override
        protected boolean use() {
            return TribunalPowers.INSTANCE.storm(entity);
        }
    }

    /** The Scales of Balance: two pans of light crash down in turn. */
    public static class Scales extends AbilityCosmic {
        public Scales(EntityLivingBase entity) {
            super(entity, "scales");
        }

        @Override
        protected boolean use() {
            return TribunalPowers.INSTANCE.scales(entity);
        }
    }

    /** Sentence: a cage of light holds what you look at, then bursts. */
    public static class Sentence extends AbilityCosmic {
        public Sentence(EntityLivingBase entity) {
            super(entity, "sentence");
        }

        @Override
        protected boolean use() {
            return TribunalPowers.INSTANCE.sentence(entity);
        }
    }

    /** Convergence: golden blades circle and meet. */
    public static class Convergence extends AbilityCosmic {
        public Convergence(EntityLivingBase entity) {
            super(entity, "convergence");
        }

        @Override
        protected boolean use() {
            return TribunalPowers.INSTANCE.convergence(entity);
        }
    }

    /** Pillars of Creation: columns of light marching away from you. */
    public static class Pillars extends AbilityCosmic {
        public Pillars(EntityLivingBase entity) {
            super(entity, "pillars");
        }

        @Override
        protected boolean use() {
            return TribunalPowers.INSTANCE.pillars(entity);
        }
    }

    /** Cosmic Verdict: a ring of light bursting out from you across the ground. */
    public static class Verdict extends AbilityCosmic {
        public Verdict(EntityLivingBase entity) {
            super(entity, "cosmic_verdict");
        }

        @Override
        protected boolean use() {
            return TribunalPowers.INSTANCE.verdict(entity);
        }
    }
}
