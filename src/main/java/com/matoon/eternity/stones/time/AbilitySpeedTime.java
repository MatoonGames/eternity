package com.matoon.eternity.stones.time;

import net.minecraft.entity.EntityLivingBase;

/** The Time Stone speeding time in its holder's dimension, five ticks a second each press, to as fast as a hundred. */
public class AbilitySpeedTime extends AbilityBendTime {

    public AbilitySpeedTime(EntityLivingBase entity) {
        super(entity, "speed_time", 1F, 100F);
    }
}
