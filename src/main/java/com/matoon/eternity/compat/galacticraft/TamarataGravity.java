package com.matoon.eternity.compat.galacticraft;

import micdoodle8.mods.galacticraft.api.event.ZeroGravityEvent;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Galacticraft gives every zero-g dimension space-station physics: you hang where you are and
 * push off from walls, whatever the dimension says about freefall. Tamarata is only weightless
 * once it is broken; while it is whole, all of that is turned off and you walk, jump and fall
 * as you would at home.
 */
public final class TamarataGravity {

    public static final TamarataGravity INSTANCE = new TamarataGravity();

    private TamarataGravity() {
    }

    @SubscribeEvent
    public void inFreefall(ZeroGravityEvent.InFreefall event) {
        cancelIfWhole(event);
    }

    @SubscribeEvent
    public void motion(ZeroGravityEvent.Motion event) {
        cancelIfWhole(event);
    }

    @SubscribeEvent
    public void sneak(ZeroGravityEvent.SneakOverride event) {
        cancelIfWhole(event);
    }

    private static void cancelIfWhole(ZeroGravityEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        if (entity != null && entity.world != null && entity.world.provider instanceof WorldProviderTamarata
                && !((WorldProviderTamarata) entity.world.provider).broken()) {
            event.setCanceled(true);
        }
    }
}
