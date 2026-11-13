package com.matoon.eternity.api.event;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * An Astral Regulator has just taken a being in (Eternity as he dies, the Tribunal, Order or Chaos beaten, the
 * In-Betweener fallen, Death turned out of her skin, or any addon's), on the server. Posted on
 * {@code MinecraftForge.EVENT_BUS} once the stack already holds it.
 */
public class RegulatorAbsorbEvent extends Event {

    private final ItemStack regulator;
    private final String being;

    public RegulatorAbsorbEvent(ItemStack regulator, String being) {
        this.regulator = regulator;
        this.being = being;
    }

    public ItemStack getRegulator() {
        return regulator;
    }

    /** The being's id, as in {@code ItemAstralRegulator} or an addon's {@code RegulatorBeing}. */
    public String getBeing() {
        return being;
    }
}
