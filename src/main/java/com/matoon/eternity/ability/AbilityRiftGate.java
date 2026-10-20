package com.matoon.eternity.ability;

import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.network.MsgOpenRiftGate;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;

/**
 * Rift Gate: tear open one of his great rifts to any dimension and place you name.
 *
 * The key only opens the destination screen; the gate (and the cooldown) come later,
 * when the server accepts the request in {@link com.matoon.eternity.world.CosmicPowers#riftGate}.
 */
public class AbilityRiftGate extends AbilityCosmic {

    public AbilityRiftGate(EntityLivingBase entity) {
        super(entity, "rift_gate");
    }

    @Override
    protected boolean use() {
        if (entity instanceof EntityPlayerMP) {
            EternityNetwork.send(new MsgOpenRiftGate(), (EntityPlayerMP) entity);
        }
        // No cooldown yet: closing the screen without choosing should cost nothing.
        return false;
    }

    /** Starts the cooldown once a gate has actually opened. */
    public void spend() {
        setCooldown(getMaxCooldown());
    }
}
