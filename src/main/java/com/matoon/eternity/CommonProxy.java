package com.matoon.eternity;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/** Server-side proxy. Every client-bound message is a no-op here. */
public class CommonProxy {

    public void preInit() {
    }

    public void receive(IMessage message) {
    }
}
