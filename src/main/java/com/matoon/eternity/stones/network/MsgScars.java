package com.matoon.eternity.stones.network;

import io.netty.buffer.ByteBuf;

import java.util.UUID;

/**
 * How badly snapping has burnt someone's arms, to everyone: each arm's burn (see {@code SnapScars}), and which arm, if
 * either, has just snapped, so it flares up and smokes.
 */
public class MsgScars implements StoneMessage {

    public static final int NO_FLARE = 0, FLARE_RIGHT = 1, FLARE_LEFT = 2;

    public UUID player;
    public float right, left;
    public int flare;

    public MsgScars() {
    }

    public MsgScars(UUID player, float right, float left, int flare) {
        this.player = player;
        this.right = right;
        this.left = left;
        this.flare = flare;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        StoneMessage.writeUuid(buf, player);
        buf.writeFloat(right);
        buf.writeFloat(left);
        buf.writeByte(flare);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        player = StoneMessage.readUuid(buf);
        right = buf.readFloat();
        left = buf.readFloat();
        flare = buf.readByte();
    }
}
