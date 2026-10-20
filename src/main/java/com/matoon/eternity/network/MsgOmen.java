package com.matoon.eternity.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/**
 * A wordless shift in the world, sent to one player when they take a stone type for
 * the first time. {@code level} is how many types they now have; at six,
 * {@code delay} is the number of ticks until Eternity arrives.
 */
public class MsgOmen implements IMessage {

    public int level;
    public int delay;

    public MsgOmen() {
    }

    public MsgOmen(int level, int delay) {
        this.level = level;
        this.delay = delay;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        level = buf.readByte();
        delay = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(level);
        buf.writeInt(delay);
    }
}
