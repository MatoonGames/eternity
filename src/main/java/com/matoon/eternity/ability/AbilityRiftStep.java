package com.matoon.eternity.ability;

import com.matoon.eternity.world.CosmicPowers;
import net.minecraft.entity.EntityLivingBase;

/** Rift Step: step through a rift to wherever you are looking. */
public class AbilityRiftStep extends AbilityCosmic {

    public AbilityRiftStep(EntityLivingBase entity) {
        super(entity, "rift_step");
    }

    @Override
    protected boolean use() {
        return CosmicPowers.INSTANCE.riftStep(entity);
    }
}
