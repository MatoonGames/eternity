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

    /** The Destroyer Armor's systems menu, on the client. */
    public Object armorMenu(net.minecraft.entity.player.EntityPlayer player, com.matoon.eternity.entity.EntityDestroyerArmor armor) {
        return null;
    }
}
