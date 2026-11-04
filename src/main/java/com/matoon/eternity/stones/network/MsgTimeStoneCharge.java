package com.matoon.eternity.stones.network;

import io.netty.buffer.ByteBuf;

/** The Time Stone's charge, to its holder, for their ability bar. */
public class MsgTimeStoneCharge implements StoneMessage {

    public float charge;
    public boolean depleted;

    public MsgTimeStoneCharge() {
    }

    public MsgTimeStoneCharge(float charge, boolean depleted) {
        this.charge = charge;
        this.depleted = depleted;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeFloat(charge);
        buf.writeBoolean(depleted);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        charge = buf.readFloat();
        depleted = buf.readBoolean();
    }
}
