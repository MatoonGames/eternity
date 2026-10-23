package com.matoon.eternity.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/**
 * Where the Relic Tracker should point, in the player's own dimension, and the registry
 * name of the relic it found there. Empty means nowhere.
 */
public class MsgRelicTarget implements IMessage {

    public boolean found;
    public double x, z;
    public String item = "";

    public MsgRelicTarget() {
    }

    public MsgRelicTarget(double x, double z, String item) {
        this.found = true;
        this.x = x;
        this.z = z;
        this.item = item;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        found = buf.readBoolean();
        x = buf.readDouble();
        z = buf.readDouble();
        item = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(found);
        buf.writeDouble(x);
        buf.writeDouble(z);
        ByteBufUtils.writeUTF8String(buf, item);
    }
}
