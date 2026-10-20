package com.matoon.eternity.world;

import com.matoon.eternity.EternityConfig;
import com.matoon.eternity.entity.EntityEternity;
import com.matoon.eternity.item.ItemAstralRegulator;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.UUID;

/**
 * Holding a charged Astral Regulator, you carry Eternity's endurance as well as his
 * powers: his health is added to yours, and, just as with him, no single blow can
 * take more than a sliver of it (combat.maxHitFraction).
 */
public final class RegulatorPowers {

    public static final RegulatorPowers INSTANCE = new RegulatorPowers();

    private static final UUID HEALTH = UUID.fromString("6f1b0d8e-3c2a-4b8e-9a51-2e7d4c9f1a30");

    private RegulatorPowers() {
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        EntityPlayer player = event.player;
        if (event.phase != TickEvent.Phase.END || player.world.isRemote || player.ticksExisted % 10 != 0) {
            return;
        }
        boolean holding = ItemAstralRegulator.holdsCharged(player);
        IAttributeInstance maxHealth = player.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
        AttributeModifier modifier = maxHealth.getModifier(HEALTH);
        if (holding && modifier == null) {
            maxHealth.applyModifier(new AttributeModifier(HEALTH, "Astral Regulator", EntityEternity.MAX_HEALTH, 0).setSaved(false));
            // His health is added to theirs at once, not just the room for it.
            player.setHealth(player.getHealth() + (float) EntityEternity.MAX_HEALTH);
        } else if (!holding && modifier != null) {
            maxHealth.removeModifier(modifier);
            if (player.getHealth() > player.getMaxHealth()) {
                player.setHealth(player.getMaxHealth());
            }
        }
    }

    /** No blow takes more than Eternity himself would let one take. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onHurt(LivingHurtEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer) || !ItemAstralRegulator.holdsCharged(event.getEntityLiving())) {
            return;
        }
        float cap = event.getEntityLiving().getMaxHealth() * (float) EternityConfig.combat.maxHitFraction;
        if (event.getAmount() > cap) {
            event.setAmount(cap);
        }
    }
}
