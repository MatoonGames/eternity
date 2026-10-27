package com.matoon.eternity.item;

import com.matoon.eternity.Eternity;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = Eternity.MODID)
public final class EternityItems {

    public static final ItemRelicTracker RELIC_TRACKER = new ItemRelicTracker();
    public static final ItemAstralRegulator ASTRAL_REGULATOR = new ItemAstralRegulator();
    public static final ItemCasket CASKET = new ItemCasket();
    public static final ItemEinherjarArmor EINHERJAR_HELM = new ItemEinherjarArmor("einherjar_helm", EntityEquipmentSlot.HEAD);
    public static final ItemEinherjarArmor EINHERJAR_CUIRASS = new ItemEinherjarArmor("einherjar_cuirass", EntityEquipmentSlot.CHEST);
    public static final ItemEinherjarArmor EINHERJAR_GREAVES = new ItemEinherjarArmor("einherjar_greaves", EntityEquipmentSlot.LEGS);
    public static final ItemEinherjarArmor EINHERJAR_BOOTS = new ItemEinherjarArmor("einherjar_boots", EntityEquipmentSlot.FEET);
    public static final ItemEinherjarSpear EINHERJAR_SPEAR = new ItemEinherjarSpear();
    public static final ItemEinherjarShield EINHERJAR_SHIELD = new ItemEinherjarShield();

    /** Everything an Einherjar carries, drawn by one renderer. */
    public static final Item[] EINHERJAR_GEAR = {EINHERJAR_HELM, EINHERJAR_CUIRASS, EINHERJAR_GREAVES, EINHERJAR_BOOTS, EINHERJAR_SPEAR, EINHERJAR_SHIELD};

    /** The Relic Tracker's name before it learned to find Regulators too. */
    private static final ResourceLocation STONE_FINDER = new ResourceLocation(Eternity.MODID, "stone_finder");

    private EternityItems() {
    }

    @SubscribeEvent
    public static void register(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(RELIC_TRACKER);
        event.getRegistry().register(ASTRAL_REGULATOR);
        event.getRegistry().register(CASKET);
        event.getRegistry().registerAll(EINHERJAR_GEAR);
    }

    /** Stone Finders in older worlds become Relic Trackers. */
    @SubscribeEvent
    public static void remap(RegistryEvent.MissingMappings<Item> event) {
        for (RegistryEvent.MissingMappings.Mapping<Item> mapping : event.getAllMappings()) {
            if (mapping.key.equals(STONE_FINDER)) {
                mapping.remap(RELIC_TRACKER);
            }
        }
    }
}
