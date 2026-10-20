package com.matoon.eternity.ability;

import com.matoon.eternity.world.CosmicPowers;
import net.minecraft.entity.EntityLivingBase;

/** Unmake: a touch that tears apart whatever you reach for. */
public class AbilityUnmake extends AbilityCosmic {

    public AbilityUnmake(EntityLivingBase entity) {
        super(entity, "unmake");
    }

    @Override
    protected boolean use() {
        return CosmicPowers.INSTANCE.unmake(entity);
    }
}
