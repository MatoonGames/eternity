package com.matoon.eternity.stones.network;

import io.netty.buffer.ByteBuf;

import java.util.UUID;

/** To a player the Mind Stone reaches for (or holds): their chance to resist, or break free, by timing a key press. */
public class MsgMindControlResistance implements StoneMessage {

    public UUID token;
    public int duration, windowStart, windowEnd;
    public boolean escape;

    public MsgMindControlResistance() {
    }

    public MsgMindControlResistance(UUID token, int duration, int windowStart, int windowEnd, boolean escape) {
        this.token = token;
        this.duration = duration;
        this.windowStart = windowStart;
        this.windowEnd = windowEnd;
        this.escape = escape;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        StoneMessage.writeUuid(buf, token);
        buf.writeInt(duration);
        buf.writeInt(windowStart);
        buf.writeInt(windowEnd);
        buf.writeBoolean(escape);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        token = StoneMessage.readUuid(buf);
        duration = buf.readInt();
        windowStart = buf.readInt();
        windowEnd = buf.readInt();
        escape = buf.readBoolean();
    }
}
