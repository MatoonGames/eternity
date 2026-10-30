package com.matoon.eternity.block;

import com.matoon.eternity.Eternity;
import net.minecraft.block.Block;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = Eternity.MODID)
public final class EternityBlocks {

    /** The Grandmaster's board. It has no item: nobody carries it but him. */
    public static final BlockBoard BOARD = new BlockBoard();

    private EternityBlocks() {
    }

    @SubscribeEvent
    public static void register(RegistryEvent.Register<Block> event) {
        event.getRegistry().register(BOARD);
    }
}
