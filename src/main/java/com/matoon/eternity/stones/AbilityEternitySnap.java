package com.matoon.eternity.stones;

import anvil.infinity.abilities.AbilitySnap;
import net.minecraft.entity.EntityLivingBase;

/** InfinityCraft's snap, set into the Soul Stone in its place, doing all six of its ways (see {@link SnapPowers}). */
public class AbilityEternitySnap extends AbilitySnap {

    public AbilityEternitySnap(EntityLivingBase entity) {
        super(entity);
    }

    @Override
    public boolean action() {
        return SnapPowers.snap(entity);
    }
}
