package com.matoon.eternity.stones.time;

import net.minecraft.entity.EntityLivingBase;

/** The Time Stone slowing time in its holder's dimension, five ticks a second each press, to as slow as one. */
public class AbilitySlowTime extends AbilityBendTime {

    public AbilitySlowTime(EntityLivingBase entity) {
        super(entity, "slow_time", -1F, 1F);
    }
}
