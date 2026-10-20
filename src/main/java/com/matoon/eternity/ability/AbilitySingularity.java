package com.matoon.eternity.ability;

import com.matoon.eternity.world.CosmicPowers;
import net.minecraft.entity.EntityLivingBase;

/** Singularity: a black hole opens where you look. */
public class AbilitySingularity extends AbilityCosmic {

    public AbilitySingularity(EntityLivingBase entity) {
        super(entity, "singularity");
    }

    @Override
    protected boolean use() {
        return CosmicPowers.INSTANCE.singularity(entity);
    }
}
