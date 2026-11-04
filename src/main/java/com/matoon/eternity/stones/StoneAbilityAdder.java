package com.matoon.eternity.stones;

import anvil.infinity.api.IAbilityAdder;
import anvil.infinity.api.StoneAbility;
import com.matoon.eternity.stones.mind.AbilityMindControl;
import com.matoon.eternity.stones.time.AbilityResetTime;
import com.matoon.eternity.stones.time.AbilitySlowTime;
import com.matoon.eternity.stones.time.AbilitySpeedTime;
import com.matoon.eternity.stones.time.AbilityTimeUnlocker;
import lucraft.mods.lucraftcore.infinity.EnumInfinityStone;
import net.minecraft.entity.EntityLivingBase;

import java.util.ArrayList;
import java.util.List;

/**
 * Adds to what the stones lend, through InfinityCraft's own hook, beside their usual powers: the Time Stone slows,
 * speeds and restores time, and steps outside it; the Mind Stone takes minds. And the snap, and the Mind Stone's choice of
 * what it does, are replaced with Eternity's, which finish the two ways InfinityCraft left undone.
 */
public class StoneAbilityAdder implements IAbilityAdder {

    @Override
    public List<StoneAbility> addStoneAbilities(EntityLivingBase entity, EnumInfinityStone stone) {
        List<StoneAbility> abilities = new ArrayList<>();
        if (stone == EnumInfinityStone.TIME) {
            abilities.add(new StoneAbility("eternity:slow_time", new AbilitySlowTime(entity)));
            abilities.add(new StoneAbility("eternity:speed_time", new AbilitySpeedTime(entity)));
            abilities.add(new StoneAbility("eternity:reset_time", new AbilityResetTime(entity)));
            abilities.add(new StoneAbility("eternity:time_unlocker", new AbilityTimeUnlocker(entity)));
        } else if (stone == EnumInfinityStone.MIND) {
            abilities.add(new StoneAbility("eternity:mind_control", new AbilityMindControl(entity)));
            // In place of InfinityCraft's own, under the same names.
            abilities.add(new StoneAbility("changesnap", new AbilitySnapMode(entity)));
        } else if (stone == EnumInfinityStone.SOUL) {
            abilities.add(new StoneAbility("snap", new AbilityEternitySnap(entity)));
        }
        return abilities;
    }
}
