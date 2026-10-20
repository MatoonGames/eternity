package com.matoon.eternity.ability;

import com.matoon.eternity.world.CosmicPowers;
import net.minecraft.entity.EntityLivingBase;

/** Void Lances: rifts open around your target and fire lances of nothing. */
public class AbilityVoidLances extends AbilityCosmic {

    public AbilityVoidLances(EntityLivingBase entity) {
        super(entity, "void_lances");
    }

    @Override
    protected boolean use() {
        return CosmicPowers.INSTANCE.lances(entity);
    }
}
