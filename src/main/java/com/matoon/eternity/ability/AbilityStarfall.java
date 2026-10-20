package com.matoon.eternity.ability;

import com.matoon.eternity.world.CosmicPowers;
import net.minecraft.entity.EntityLivingBase;

/** Starfall: stars rain down wherever you look. */
public class AbilityStarfall extends AbilityCosmic {

    public AbilityStarfall(EntityLivingBase entity) {
        super(entity, "starfall");
    }

    @Override
    protected boolean use() {
        return CosmicPowers.INSTANCE.starfall(entity);
    }
}
