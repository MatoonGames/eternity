package com.matoon.eternity.stones.network;

import io.netty.buffer.ByteBuf;

/** To a player whose mind has been taken, or given back: they can still look around, but do nothing else. */
public class MsgMindControlLock implements StoneMessage {

    public boolean locked;
    public int remainingTicks;

    public MsgMindControlLock() {
    }

    public MsgMindControlLock(boolean locked, int remainingTicks) {
        this.locked = locked;
        this.remainingTicks = remainingTicks;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(locked);
        buf.writeInt(remainingTicks);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        locked = buf.readBoolean();
        remainingTicks = buf.readInt();
    }
}
