package com.matoon.eternity.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/** Where the Stone Finder should point, in the player's own dimension. Empty means nowhere. */
public class MsgStoneTarget implements IMessage {

    public boolean found;
    public double x, z;

    public MsgStoneTarget() {
    }

    public MsgStoneTarget(double x, double z) {
        this.found = true;
        this.x = x;
        this.z = z;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        found = buf.readBoolean();
        x = buf.readDouble();
        z = buf.readDouble();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(found);
        buf.writeDouble(x);
        buf.writeDouble(z);
    }
}
