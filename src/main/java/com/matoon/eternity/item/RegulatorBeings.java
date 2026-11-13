package com.matoon.eternity.item;

import com.matoon.eternity.ability.EternityAbilities;
import com.matoon.eternity.ability.MastersAbilities;
import com.matoon.eternity.ability.TribunalAbilities;
import com.matoon.eternity.api.Force;
import com.matoon.eternity.api.RegulatorBeing;
import com.matoon.eternity.entity.EntityEternity;
import com.matoon.eternity.entity.EntityInBetweener;
import com.matoon.eternity.entity.EntityLivingTribunal;
import com.matoon.eternity.entity.EntityOrderChaos;
import com.matoon.eternity.world.Director;
import com.matoon.eternity.world.InBetweenerDirector;
import com.matoon.eternity.world.OrderAndChaos;
import com.matoon.eternity.world.TribunalDirector;

import java.util.function.Consumer;

import static com.matoon.eternity.item.ItemAstralRegulator.CHAOS;
import static com.matoon.eternity.item.ItemAstralRegulator.DEATH;
import static com.matoon.eternity.item.ItemAstralRegulator.ETERNITY;
import static com.matoon.eternity.item.ItemAstralRegulator.INBETWEENER;
import static com.matoon.eternity.item.ItemAstralRegulator.ORDER;
import static com.matoon.eternity.item.ItemAstralRegulator.TRIBUNAL;

/**
 * Eternity's own beings, as the Regulator knows them: registered before any addon's, in the order the tooltip shows
 * them and the Regulator, raised, tries them.
 */
public final class RegulatorBeings {

    private RegulatorBeings() {
    }

    public static void registerBuiltIns(Consumer<RegulatorBeing> register) {
        // Eternity it takes by flying into his heart; it comes back holding him and everything it held before.
        register.accept(RegulatorBeing.builder(ETERNITY)
                .name("entity.eternity.eternity.name", 0xC48AFF).header("tooltip.eternity.powers", 0xD49A1C, 0xE6D9FF)
                .bar(Force.ETERNITY).energy(1_000_000L).powers(EternityAbilities.ETERNITY_POWERS).grants(EternityAbilities::grantEternity)
                .entity(EntityEternity.class)
                .sensedBy((player, held) -> !held.contains(ETERNITY))
                .absorbedBy((player, stack, hand, held) -> {
                    if (held.contains(ETERNITY) || !Director.INSTANCE.tryContain(player, held)) {
                        return false;
                    }
                    if (!player.capabilities.isCreativeMode) {
                        stack.shrink(1);
                    }
                    return true;
                }).build());
        register.accept(RegulatorBeing.builder(TRIBUNAL)
                .name("entity.eternity.living_tribunal.name", 0xF0C24A).header("tooltip.eternity.judgements", 0xF0C24A, 0xFFF0C8)
                .bar(Force.LIVING_TRIBUNAL).energy(100_000_000L).powers(TribunalAbilities.POWERS).grants(TribunalAbilities::grant)
                .entity(EntityLivingTribunal.class)
                .sensedBy((player, held) -> !held.contains(TRIBUNAL) && EntityLivingTribunal.downedNear(player))
                .absorbedBy((player, stack, hand, held) -> !held.contains(TRIBUNAL) && TribunalDirector.INSTANCE.tryAbsorb(player, hand))
                .build());
        // Order and Chaos are taken by the same hand: whichever of them lies beaten.
        register.accept(RegulatorBeing.builder(ORDER)
                .name("entity.eternity.master_order.name", 0xFFC640).header("tooltip.eternity.laws", 0xFFC640, 0xFFEEB8)
                .bar(Force.MASTER_ORDER).energy(250_000_000L).powers(MastersAbilities.ORDER_POWERS).grants(MastersAbilities::grantOrder)
                .entity(EntityOrderChaos.class)
                .sensedBy((player, held) -> EntityOrderChaos.downedNear(player) || EntityOrderChaos.standingBefore(player))
                .absorbedBy((player, stack, hand, held) -> OrderAndChaos.INSTANCE.tryAbsorb(player, hand))
                .build());
        register.accept(RegulatorBeing.builder(CHAOS)
                .name("entity.eternity.lord_chaos.name", 0x7CC8F0).header("tooltip.eternity.whims", 0x7CC8F0, 0xD6F0FF)
                .bar(Force.LORD_CHAOS).energy(250_000_000L).powers(MastersAbilities.CHAOS_POWERS).grants(MastersAbilities::grantChaos)
                .entity(EntityOrderChaos.class).build());
        register.accept(RegulatorBeing.builder(INBETWEENER)
                .name("entity.eternity.inbetweener.name", 0xE8E8EC).header("tooltip.eternity.paradoxes", 0xB8B8C0, 0xF4F4F6)
                .bar(Force.IN_BETWEENER).energy(500_000_000L).powers(MastersAbilities.INBETWEENER_POWERS).grants(MastersAbilities::grantInBetweener)
                .entity(EntityInBetweener.class)
                .sensedBy((player, held) -> !held.contains(INBETWEENER) && EntityInBetweener.fallenNear(player))
                .absorbedBy((player, stack, hand, held) -> !held.contains(INBETWEENER) && InBetweenerDirector.INSTANCE.tryAbsorb(player, hand))
                .build());
        // Death lends nothing but what pours out of her; she is taken in the story's own time (see story.Betrayal).
        register.accept(RegulatorBeing.builder(DEATH).bar(Force.DEATH).energy(500_000_000L).unlisted().build());
    }
}
