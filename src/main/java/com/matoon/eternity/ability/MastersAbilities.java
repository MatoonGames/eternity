package com.matoon.eternity.ability;

import com.matoon.eternity.Eternity;
import com.matoon.eternity.world.MastersPowers;
import lucraft.mods.lucraftcore.superpowers.abilities.Ability;
import lucraft.mods.lucraftcore.superpowers.abilities.AbilityEntry;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.function.Predicate;

/**
 * The powers of Master Order, Lord Chaos and the In-Betweener as Lucraft abilities,
 * granted once each is inside the Regulator. Order's are laws: a decree, an edict, a
 * tessellation, an alignment. Chaos's are whims: dice, a maw, wanderers, disarray. The
 * In-Betweener's are paradoxes, three of them, and nobody is told what they are until
 * they hold him.
 */
@Mod.EventBusSubscriber(modid = Eternity.MODID)
public final class MastersAbilities {

    /** Ability names each grants, in the order the tooltip lists them. */
    public static final String[] ORDER_POWERS = {"decree", "edict", "tessellation", "alignment"};
    public static final String[] CHAOS_POWERS = {"dice", "maw", "wanderers", "disarray"};
    public static final String[] INBETWEENER_POWERS = {"inversion", "equilibrium", "paradox"};

    private MastersAbilities() {
    }

    @SubscribeEvent
    public static void register(RegistryEvent.Register<AbilityEntry> event) {
        event.getRegistry().registerAll(
                entry(Decree.class, "decree"), entry(Edict.class, "edict"), entry(Tessellation.class, "tessellation"),
                entry(Alignment.class, "alignment"), entry(Dice.class, "dice"), entry(Maw.class, "maw"),
                entry(Wanderers.class, "wanderers"), entry(Disarray.class, "disarray"), entry(Inversion.class, "inversion"),
                entry(Equilibrium.class, "equilibrium"), entry(Paradox.class, "paradox"));
    }

    private static AbilityEntry entry(Class<? extends Ability> type, String name) {
        return new AbilityEntry(type, new ResourceLocation(Eternity.MODID, name));
    }

    /** Everything an absorbed Master Order lets you do, with cooldowns in ticks. */
    public static void grantOrder(EntityLivingBase entity, Ability.AbilityMap map) {
        map.put("decree", new Decree(entity).setMaxCooldown(120));
        map.put("edict", new Edict(entity).setMaxCooldown(400));
        map.put("tessellation", new Tessellation(entity).setMaxCooldown(300));
        map.put("alignment", new Alignment(entity).setMaxCooldown(360));
    }

    /** Everything an absorbed Lord Chaos lets you do, with cooldowns in ticks. */
    public static void grantChaos(EntityLivingBase entity, Ability.AbilityMap map) {
        map.put("dice", new Dice(entity).setMaxCooldown(200));
        map.put("maw", new Maw(entity).setMaxCooldown(300));
        map.put("wanderers", new Wanderers(entity).setMaxCooldown(360));
        map.put("disarray", new Disarray(entity).setMaxCooldown(600));
    }

    /** The In-Betweener's paradoxes, with cooldowns in ticks. */
    public static void grantInBetweener(EntityLivingBase entity, Ability.AbilityMap map) {
        map.put("inversion", new Inversion(entity).setMaxCooldown(100));
        map.put("equilibrium", new Equilibrium(entity).setMaxCooldown(900));
        map.put("paradox", new Paradox(entity).setMaxCooldown(600));
    }

    /** One power, its name and what it does. */
    private abstract static class Power extends AbilityCosmic {
        private final Predicate<EntityLivingBase> use;

        Power(EntityLivingBase entity, String name, Predicate<EntityLivingBase> use) {
            super(entity, name);
            this.use = use;
        }

        @Override
        protected boolean use() {
            return use.test(entity);
        }
    }

    public static class Decree extends Power {
        public Decree(EntityLivingBase entity) {
            super(entity, "decree", MastersPowers.INSTANCE::decree);
        }
    }

    public static class Edict extends Power {
        public Edict(EntityLivingBase entity) {
            super(entity, "edict", MastersPowers.INSTANCE::edict);
        }
    }

    public static class Tessellation extends Power {
        public Tessellation(EntityLivingBase entity) {
            super(entity, "tessellation", MastersPowers.INSTANCE::tessellation);
        }
    }

    public static class Alignment extends Power {
        public Alignment(EntityLivingBase entity) {
            super(entity, "alignment", MastersPowers.INSTANCE::alignment);
        }
    }

    public static class Dice extends Power {
        public Dice(EntityLivingBase entity) {
            super(entity, "dice", MastersPowers.INSTANCE::dice);
        }
    }

    public static class Maw extends Power {
        public Maw(EntityLivingBase entity) {
            super(entity, "maw", MastersPowers.INSTANCE::maw);
        }
    }

    public static class Wanderers extends Power {
        public Wanderers(EntityLivingBase entity) {
            super(entity, "wanderers", MastersPowers.INSTANCE::wanderers);
        }
    }

    public static class Disarray extends Power {
        public Disarray(EntityLivingBase entity) {
            super(entity, "disarray", MastersPowers.INSTANCE::disarray);
        }
    }

    public static class Inversion extends Power {
        public Inversion(EntityLivingBase entity) {
            super(entity, "inversion", MastersPowers.INSTANCE::inversion);
        }
    }

    public static class Equilibrium extends Power {
        public Equilibrium(EntityLivingBase entity) {
            super(entity, "equilibrium", MastersPowers.INSTANCE::equilibrium);
        }
    }

    public static class Paradox extends Power {
        public Paradox(EntityLivingBase entity) {
            super(entity, "paradox", MastersPowers.INSTANCE::paradox);
        }
    }
}
