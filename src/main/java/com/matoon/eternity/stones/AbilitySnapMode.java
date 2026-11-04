package com.matoon.eternity.stones;

import anvil.infinity.abilities.AbilityChangeSnap;
import net.minecraft.entity.EntityLivingBase;

/**
 * The Mind Stone choosing what the snap does, set in place of InfinityCraft's, which still calls its two unfinished
 * ways "(WIP)". They are finished here (see {@link SnapPowers}).
 */
public class AbilitySnapMode extends AbilityChangeSnap {

    public AbilitySnapMode(EntityLivingBase entity) {
        super(entity);
    }

    @Override
    public boolean action() {
        return SnapPowers.cycle(entity);
    }
}
