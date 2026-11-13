package com.matoon.eternity.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/**
 * The Magus's last fight, to the one fighting it: a struggle for the gauntlet beginning, how
 * it stands, how it ended; and the white of his snap.
 */
public class MsgGauntletFight implements IMessage {

    /** A struggle begins: id = the Magus, value = how it stands (0 to 1000, 1000 theirs). */
    public static final byte START = 0;
    /** How it stands now: value, 0 to 1000. */
    public static final byte BAR = 1;
    /** It is over: value 1 if they won it. */
    public static final byte END = 2;
    /** He has snapped: everything goes white, and comes back over value ticks. */
    public static final byte WHITE = 3;

    public byte type;
    public int id, value;

    public MsgGauntletFight() {
    }

    public MsgGauntletFight(byte type, int id, int value) {
        this.type = type;
        this.id = id;
        this.value = value;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        type = buf.readByte();
        id = buf.readInt();
        value = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(type);
        buf.writeInt(id);
        buf.writeInt(value);
    }
}
