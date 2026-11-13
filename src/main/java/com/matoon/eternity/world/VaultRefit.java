package com.matoon.eternity.world;

import com.matoon.eternity.Eternity;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Keeps Odin's Vault up to date in worlds that saw an older one: every chunk of it is saved with
 * the {@link OdinsVault#LAYOUT} it was laid under, and one saved under an older layout (or none)
 * is laid again from the current one as it loads, before anyone sees it.
 */
@Mod.EventBusSubscriber(modid = Eternity.MODID)
public final class VaultRefit {

    private static final String KEY = "EternityVaultLayout";

    private VaultRefit() {
    }

    @SubscribeEvent
    public static void onLoad(ChunkDataEvent.Load event) {
        Chunk chunk = event.getChunk();
        if (inVault(chunk) && event.getData().getInteger(KEY) < OdinsVault.LAYOUT) {
            OdinsVault.refit(chunk);
        }
    }

    @SubscribeEvent
    public static void onSave(ChunkDataEvent.Save event) {
        if (inVault(event.getChunk())) {
            event.getData().setInteger(KEY, OdinsVault.LAYOUT);
        }
    }

    private static boolean inVault(Chunk chunk) {
        return chunk.getWorld() != null && chunk.getWorld().provider.getDimension() == OdinsVault.dimension();
    }
}
