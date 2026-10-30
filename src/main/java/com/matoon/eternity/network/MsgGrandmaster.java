package com.matoon.eternity.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/**
 * The Grandmaster's board, for everyone who can see it: how it stands, and everything that
 * happens on it. {@link #SYNC} carries the whole board in {@link #tag}; everything else is an
 * effect at {@link #a}, sometimes toward {@link #b}, with one number of its own.
 */
public class MsgGrandmaster implements IMessage {

    /** The board as it stands. tag = see GrandmasterDirector.boardTag. */
    public static final byte SYNC = 0;
    /** The board is gone. */
    public static final byte CLEAR = 1;
    /** Cards burst out of a, value of them. */
    public static final byte FX_CARDS = 2;
    /** The stone leaps from a to b. value = 1 if it stuttered rather than leapt. */
    public static final byte FX_LEAP = 3;
    /** A square of the board is struck at a. value = the square, b.x = its cracks now (3 = shattered). */
    public static final byte FX_STRIKE = 4;
    /** A piece is taken on the square at a. value = what it was. */
    public static final byte FX_TAKEN = 5;
    /** A ring of the board comes into being, a = the board's middle, value = how far out. */
    public static final byte FX_BUILD = 6;
    /** The whole board breaks apart, outward from a. value = ticks it takes. */
    public static final byte FX_COLLAPSE = 7;
    /** Something is laid on the table at a. */
    public static final byte FX_STAKE = 8;
    /** A square refuses the move asked of it, at a. */
    public static final byte FX_REFUSE = 9;
    /** He moves a piece himself, from a to b: gold threads and a hand in the air. value = the Grandmaster's entity id. */
    public static final byte FX_CHEAT = 10;
    /** The king falls, at a. */
    public static final byte FX_KING_FALLS = 11;
    /** He comes apart into cards, at a. */
    public static final byte FX_FADE = 12;
    /** A line spoken, shown in the speaker panel. value = the speaker's entity id (-1 for none), tag s = who (GrandmasterScenes), t = the line. */
    public static final byte LINE = 13;
    /** A cutscene begins for whoever receives this. value = which (0 the rules). */
    public static final byte CUTSCENE = 14;

    public byte type;
    public Vec3d a = Vec3d.ZERO, b = Vec3d.ZERO;
    public int value;
    public NBTTagCompound tag;

    public MsgGrandmaster() {
    }

    public static MsgGrandmaster sync(NBTTagCompound tag) {
        MsgGrandmaster m = new MsgGrandmaster();
        m.type = SYNC;
        m.tag = tag;
        return m;
    }

    public static MsgGrandmaster clear() {
        MsgGrandmaster m = new MsgGrandmaster();
        m.type = CLEAR;
        return m;
    }

    public static MsgGrandmaster line(byte speaker, int entity, String text) {
        MsgGrandmaster m = new MsgGrandmaster();
        m.type = LINE;
        m.value = entity;
        m.tag = new NBTTagCompound();
        m.tag.setByte("s", speaker);
        m.tag.setString("t", text);
        return m;
    }

    public static MsgGrandmaster fx(byte type, Vec3d a, Vec3d b, int value) {
        MsgGrandmaster m = new MsgGrandmaster();
        m.type = type;
        m.a = a;
        m.b = b;
        m.value = value;
        return m;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        type = buf.readByte();
        a = new Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble());
        b = new Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble());
        value = buf.readInt();
        if (buf.readBoolean()) {
            tag = ByteBufUtils.readTag(buf);
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(type);
        buf.writeDouble(a.x);
        buf.writeDouble(a.y);
        buf.writeDouble(a.z);
        buf.writeDouble(b.x);
        buf.writeDouble(b.y);
        buf.writeDouble(b.z);
        buf.writeInt(value);
        buf.writeBoolean(tag != null);
        if (tag != null) {
            ByteBufUtils.writeTag(buf, tag);
        }
    }
}
