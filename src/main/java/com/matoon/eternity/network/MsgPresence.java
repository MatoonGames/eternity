package com.matoon.eternity.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/** Where Eternity is, sent once a second to everyone. {@code present} is false outside his dimension. */
public class MsgPresence implements IMessage {

    public boolean present;
    public double x, y, z;
    public boolean hunted;
    public boolean space;

    public MsgPresence() {
    }

    public MsgPresence(boolean present, double x, double y, double z, boolean hunted, boolean space) {
        this.present = present;
        this.x = x;
        this.y = y;
        this.z = z;
        this.hunted = hunted;
        this.space = space;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        present = buf.readBoolean();
        x = buf.readDouble();
        y = buf.readDouble();
        z = buf.readDouble();
        hunted = buf.readBoolean();
        space = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(present);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeBoolean(hunted);
        buf.writeBoolean(space);
    }
}
