package com.matoon.eternity.ability;

import com.matoon.eternity.world.CosmicPowers;
import net.minecraft.entity.EntityLivingBase;

/** Shatter: the ground breaks apart in rings of eruptions. */
public class AbilityShatter extends AbilityCosmic {

    public AbilityShatter(EntityLivingBase entity) {
        super(entity, "shatter");
    }

    @Override
    protected boolean use() {
        return CosmicPowers.INSTANCE.shatter(entity);
    }
}
