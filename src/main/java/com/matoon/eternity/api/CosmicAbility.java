package com.matoon.eternity.api;

import com.matoon.eternity.ability.AbilityCosmic;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;

/**
 * The base Eternity's own powers are built on, for an addon's: a Lucraft action that runs {@link #use()} on the server
 * when its key is pressed, draws a 16x16 icon on the bar, and only starts its cooldown if it worked.
 * <p>
 * Like every Lucraft ability, each subclass needs its own {@code AbilityEntry} in
 * {@code RegistryEvent.Register<AbilityEntry>} (Lucraft looks the entry up by exact class as the ability is made, and
 * fails without it), and a constructor taking only the entity. Give it a bar colour with {@link Force#tint} or
 * {@link EternityAPI#tint}; abilities a {@link RegulatorBeing} grants are tinted for you.
 */
public abstract class CosmicAbility extends AbilityCosmic {

    protected CosmicAbility(EntityLivingBase entity, ResourceLocation icon) {
        super(entity, icon);
    }
}
