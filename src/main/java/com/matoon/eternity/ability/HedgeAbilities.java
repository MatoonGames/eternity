package com.matoon.eternity.ability;

import com.matoon.eternity.Eternity;
import com.matoon.eternity.compat.InfinityStones;
import com.matoon.eternity.story.StoryDirector;
import lucraft.mods.lucraftcore.infinity.EnumInfinityStone;
import lucraft.mods.lucraftcore.superpowers.abilities.Ability;
import lucraft.mods.lucraftcore.superpowers.abilities.AbilityEntry;
import lucraft.mods.lucraftcore.superpowers.abilities.supplier.AbilityContainer;
import lucraft.mods.lucraftcore.superpowers.abilities.supplier.IAbilityProvider;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;

/**
 * What the Reality and Soul Stones can do at the Gardener's hedge, and only there: two powers that come
 * onto the ability bar while one of them is in your hand within reach of it, and go again when you
 * step away or the hedge is gone.
 * <ul>
 * <li><b>Rewrite</b> (Reality): the world round you is made so that there was never a hedge there.</li>
 * <li><b>Reap</b> (Soul): everything alive in his garden gives up its life to the stone, and what is
 * left is dead and rotting.</li>
 * </ul>
 * They come through a context of their own, decided the same way on both sides: the server by the
 * story, the client by what the server last told it of where the hedge stands.
 */
@Mod.EventBusSubscriber(modid = Eternity.MODID)
public final class HedgeAbilities {

    public static Ability.EnumAbilityContext CONTEXT;

    private static final Gifts REALITY = new Gifts(true, false), SOUL = new Gifts(false, true), BOTH = new Gifts(true, true);

    private HedgeAbilities() {
    }

    @SubscribeEvent
    public static void onRegisterAbilities(RegistryEvent.Register<AbilityEntry> event) {
        event.getRegistry().registerAll(new AbilityEntry(Rewrite.class, new ResourceLocation(Eternity.MODID, "rewrite")),
                new AbilityEntry(Reap.class, new ResourceLocation(Eternity.MODID, "reap")));
    }

    /** In pre-init, before any entity is made. */
    public static void register() {
        Ability.addAbilityContext("ETERNITY_HEDGE");
        CONTEXT = Ability.EnumAbilityContext.valueOf("ETERNITY_HEDGE");
        Ability.registerSupplier(CONTEXT, HedgeAbilities::provider, AbilityContainer::new);
    }

    @Nullable
    private static IAbilityProvider provider(EntityLivingBase entity) {
        if (!(entity instanceof EntityPlayer) || entity.ticksExisted < 20) {
            return null;
        }
        EntityPlayer player = (EntityPlayer) entity;
        boolean near = player.world.isRemote ? Eternity.proxy.nearHedge(player) : StoryDirector.INSTANCE.nearHedge(player);
        if (!near) {
            return null;
        }
        boolean reality = InfinityStones.wields(player, EnumInfinityStone.REALITY), soul = InfinityStones.wields(player, EnumInfinityStone.SOUL);
        return reality && soul ? BOTH : reality ? REALITY : soul ? SOUL : null;
    }

    /** What the stones in hand lend at the hedge: one provider for each, so the bar only changes when the stones do. */
    private static final class Gifts implements IAbilityProvider {
        private final boolean reality, soul;

        Gifts(boolean reality, boolean soul) {
            this.reality = reality;
            this.soul = soul;
        }

        @Override
        public Ability.AbilityMap addDefaultAbilities(EntityLivingBase entity, Ability.AbilityMap map, Ability.EnumAbilityContext context) {
            if (reality) {
                map.put("rewrite", ready(new Rewrite(entity)));
            }
            if (soul) {
                map.put("reap", ready(new Reap(entity)));
            }
            return map;
        }
    }

    /** Lucraft starts an action on a full cooldown, as if just used; these are there to be used the moment they appear. */
    private static Ability ready(Ability ability) {
        ability.setMaxCooldown(200);
        ability.setCooldown(0);
        return ability;
    }

    public static class Rewrite extends AbilityCosmic {
        public Rewrite(EntityLivingBase entity) {
            super(entity, "rewrite");
        }

        @Override
        protected boolean use() {
            return StoryDirector.INSTANCE.rewriteHedge((EntityPlayer) entity);
        }
    }

    public static class Reap extends AbilityCosmic {
        public Reap(EntityLivingBase entity) {
            super(entity, "reap");
        }

        @Override
        protected boolean use() {
            return StoryDirector.INSTANCE.reapGarden((EntityPlayer) entity);
        }
    }
}
