package com.matoon.eternity.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/** The Living Tribunal's attacks, for everyone who can see them. */
public class MsgTribunalFx implements IMessage {

    /** A golden sigil of judgement burns onto the floor at (x,y,z). a = radius, time = ticks until it strikes. */
    public static final byte SIGIL = 0;
    /** His fist comes down at (x,y,z). a = reach of the blow. */
    public static final byte GAVEL = 1;
    /** The light of his eyes, from (x,y,z) to (a,b,c), for this instant. id = tenths of a block between the two beams. */
    public static final byte GAZE = 2;
    /** Lightning from the void in his chest, from (x,y,z) to (a,b,c). */
    public static final byte BOLT = 3;
    /** He is being drawn into the Regulator: light streams from (x,y,z) to the hand of entity {@link #id}. */
    public static final byte ABSORB = 4;
    /** He falls to his knees at (x,y,z). */
    public static final byte FALL = 5;
    /** He rises again, healed, at (x,y,z). id = the ascension he rises to. */
    public static final byte RISE = 6;
    /** A great scale-pan of light comes down on (x,y,z). a = radius, time = ticks until it lands. */
    public static final byte PAN = 7;
    /** A cage of light closes around (x,y,z). a = radius, time = ticks until it bursts. */
    public static final byte CAGE = 8;
    /** The floor at (x,y,z) opens onto the void. a = radius, time = how long it stays open. */
    public static final byte VOID = 9;
    /** Golden blades circle (x,y,z) and close in. a = radius, time = ticks until they meet. */
    public static final byte BLADES = 10;
    /** A pillar of light erupts at (x,y,z). a = height. */
    public static final byte PILLAR = 11;
    /** A ring of light races out across the floor from (x,y,z). a = how far it goes, time = ticks it takes. */
    public static final byte NOVA = 12;
    /** A wall of light sweeps from z to c, along the floor at y, leaving a gap at x. time = ticks it takes. */
    public static final byte SWEEP = 13;
    /** A ghost of him appears at (x,y,z) facing a degrees, and brings his fist down. time = how long it lasts. */
    public static final byte ECHO = 14;
    /** Crushing weight bears down on everything around (x,y,z). a = radius, time = how long. */
    public static final byte GRAVITY = 15;

    public byte type;
    public double x, y, z;
    public double a, b, c;
    public int id;
    public int time;

    public MsgTribunalFx() {
    }

    public MsgTribunalFx(byte type, double x, double y, double z, double a, double b, double c, int id, int time) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.z = z;
        this.a = a;
        this.b = b;
        this.c = c;
        this.id = id;
        this.time = time;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        type = buf.readByte();
        x = buf.readDouble();
        y = buf.readDouble();
        z = buf.readDouble();
        a = buf.readDouble();
        b = buf.readDouble();
        c = buf.readDouble();
        id = buf.readInt();
        time = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(type);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeDouble(a);
        buf.writeDouble(b);
        buf.writeDouble(c);
        buf.writeInt(id);
        buf.writeInt(time);
    }
}
