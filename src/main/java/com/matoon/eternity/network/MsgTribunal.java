package com.matoon.eternity.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/** The Living Tribunal's sequence, for the one player he has taken. */
public class MsgTribunal implements IMessage {

    /** He appears before you, reaches out, and takes you. */
    public static final byte ABDUCT = 0;
    /** You have arrived in the Star Chamber. */
    public static final byte CHAMBER = 1;
    /** He speaks. value = the dialogue node, a question or a verdict. */
    public static final byte SPEAK = 2;
    /** You are being sent home through the wormhole. */
    public static final byte RETURN = 3;
    /** Judgement is over. */
    public static final byte RELEASE = 4;
    /** He has risen from his throne to fight. */
    public static final byte FIGHT = 5;

    public byte type;
    public int value;

    public MsgTribunal() {
    }

    public MsgTribunal(byte type, int value) {
        this.type = type;
        this.value = value;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        type = buf.readByte();
        value = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(type);
        buf.writeInt(value);
    }
}
