package com.matoon.eternity.stones.network;

import io.netty.buffer.ByteBuf;

import java.util.UUID;

/**
 * The full gauntlet's power-up starting or stopping on someone, to them and everyone near: they get the sound, the
 * screen overlay and the snap's progress; everyone else the sound and the glow on them.
 */
public class MsgPowerUp implements StoneMessage {

    /** How long the power-up lasts, in ticks: the snap's lock on the server, the overlay on the client. */
    public static final int DURATION = 400;

    public boolean start;
    public UUID holder;

    public MsgPowerUp() {
    }

    public MsgPowerUp(boolean start, UUID holder) {
        this.start = start;
        this.holder = holder;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(start);
        StoneMessage.writeUuid(buf, holder);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        start = buf.readBoolean();
        holder = StoneMessage.readUuid(buf);
    }
}
