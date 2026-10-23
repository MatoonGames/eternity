package com.matoon.eternity.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/** The In-Betweener's side of things: what he says, his glass breaking, his wrath, and his masters'. */
public class MsgInBetweener implements IMessage {

    /** He speaks. value = the dialogue node, mask = which of its answers you may give, cue = what he does as he says it. */
    public static final byte SPEAK = 0;
    /** The conversation is over. */
    public static final byte CLOSE = 1;
    /** He presses on the glass from within: strike it now. value = ticks you have. */
    public static final byte SHATTER = 2;
    /** The glass breaks, for everyone in the Nexus. */
    public static final byte SHATTERED = 3;
    /** Inside his bubble with him. value = which of his warpings has begun, from 0. */
    public static final byte BATTLE = 4;
    /** It is over, one way or the other. */
    public static final byte BATTLE_END = 5;
    /** Order and Chaos have felt the glass break. value = ticks until they arrive. */
    public static final byte WRATH_SOON = 6;
    /** They are here. */
    public static final byte WRATH = 7;
    /** Inside his glass, he puts you back out of it. */
    public static final byte CAST_OUT = 8;

    public byte type;
    public int value;
    public int mask;
    /** With SPEAK: what he does as he speaks; see the B_ beats in InBetweenerDialogue. */
    public byte cue;

    public MsgInBetweener() {
    }

    public MsgInBetweener(byte type, int value) {
        this(type, value, 0);
    }

    public MsgInBetweener(byte type, int value, int mask) {
        this.type = type;
        this.value = value;
        this.mask = mask;
    }

    public MsgInBetweener(byte type, int value, int mask, byte cue) {
        this(type, value, mask);
        this.cue = cue;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        type = buf.readByte();
        value = buf.readInt();
        mask = buf.readInt();
        cue = buf.readByte();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(type);
        buf.writeInt(value);
        buf.writeInt(mask);
        buf.writeByte(cue);
    }
}
