package com.matoon.eternity;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/** Server-side proxy. Every client-bound message is a no-op here. */
public class CommonProxy {

    public void preInit() {
    }

    public void receive(IMessage message) {
    }

    /** Whether Tamarata is broken, as the client has been told. Only the client ever asks. */
    public boolean tamarataBroken() {
        return false;
    }

    /** Whether the client has been told someone stands near the Gardener's standing hedge. Only the client ever asks. */
    public boolean nearHedge(net.minecraft.entity.Entity entity) {
        return false;
    }

    /** The Destroyer Armor's systems menu, on the client. */
    public Object armorMenu(net.minecraft.entity.player.EntityPlayer player, com.matoon.eternity.entity.EntityDestroyerArmor armor) {
        return null;
    }
}
