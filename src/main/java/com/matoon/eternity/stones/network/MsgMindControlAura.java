package com.matoon.eternity.stones.network;

import io.netty.buffer.ByteBuf;

import java.util.UUID;

/** The halo around something under the Mind Stone's control, lit or put out, to everyone. */
public class MsgMindControlAura implements StoneMessage {

    public UUID entity;
    public boolean enabled;

    public MsgMindControlAura() {
    }

    public MsgMindControlAura(UUID entity, boolean enabled) {
        this.entity = entity;
        this.enabled = enabled;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        StoneMessage.writeUuid(buf, entity);
        buf.writeBoolean(enabled);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        entity = StoneMessage.readUuid(buf);
        enabled = buf.readBoolean();
    }
}
