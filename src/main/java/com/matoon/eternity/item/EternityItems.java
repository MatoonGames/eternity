package com.matoon.eternity.item;

import com.matoon.eternity.Eternity;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = Eternity.MODID)
public final class EternityItems {

    public static final ItemStoneFinder STONE_FINDER = new ItemStoneFinder();
    public static final ItemAstralRegulator ASTRAL_REGULATOR = new ItemAstralRegulator();

    private EternityItems() {
    }

    @SubscribeEvent
    public static void register(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(STONE_FINDER);
        event.getRegistry().register(ASTRAL_REGULATOR);
    }
}
