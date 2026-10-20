package com.matoon.eternity.compat;

import anvil.infinity.config.ModConfig;
import com.matoon.eternity.Eternity;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Turns off InfinityCraft's stone temples, which it scatters through the world more
 * than a thousand chunks from spawn. Its config is a Forge {@code @Config}, reloaded
 * from its file whenever any mod's config screen is saved, so the switch is thrown
 * again after every reload as well as at startup.
 */
@Mod.EventBusSubscriber(modid = Eternity.MODID)
public final class InfinityTemples {

    private InfinityTemples() {
    }

    public static void disable() {
        ModConfig.Worldgen.enable = false;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onConfigChanged(ConfigChangedEvent.PostConfigChangedEvent event) {
        disable();
    }
}
