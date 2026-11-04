package com.matoon.eternity.stones.network;

import io.netty.buffer.ByteBuf;

/** A snap, to everyone near it: one of three sounds, the same for all of them, and a white flash. */
public class MsgSnapEffect implements StoneMessage {

    public int sound;

    public MsgSnapEffect() {
    }

    public MsgSnapEffect(int sound) {
        this.sound = sound;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(sound);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        sound = buf.readByte();
    }
}
