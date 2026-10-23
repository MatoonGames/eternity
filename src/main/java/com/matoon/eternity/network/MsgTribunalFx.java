package com.matoon.eternity.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/** The Living Tribunal's attacks, and Master Order's and Lord Chaos's, for everyone who can see them. */
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
    /** Lord Chaos: a patch of the world at (x,y,z) flickers onto deep space, then bursts. a = radius, time = ticks until it bursts. */
    public static final byte CHAOS_BURST = 16;
    /** Lord Chaos inhales: space streams from around (x,y,z) into his maw at (a,b,c). time = how long. */
    public static final byte CHAOS_MAW = 17;
    /** Lord Chaos flings something from (x,y,z) to (a,b,c). */
    public static final byte CHAOS_SCATTER = 18;
    /** Master Order lays down an edict for everyone at the junction. id = which (0 stand still, 1 do not strike, 2 look upon me), time = how long it holds. */
    public static final byte ORDER_EDICT = 19;
    /** A straight golden bolt of punishment falls on (x,y,z), for breaking his edict. */
    public static final byte ORDER_BOLT = 20;
    /** A five-by-five grid of golden tiles around (x,y,z), a blocks across each; id = which tiles are lit, one bit each. time = ticks until they erupt. */
    public static final byte ORDER_TILES = 21;
    /** The lit tiles of a grid erupt. Same fields as ORDER_TILES. */
    public static final byte ORDER_ERUPT = 22;
    /** A golden line is drawn across the floor from (x,y,z) to (a,b,c); everything is pulled toward it. time = how long. */
    public static final byte ORDER_LINE = 23;
    /** A beam of judgement runs the length of the line from (x,y,z) to (a,b,c). */
    public static final byte ORDER_BEAM = 24;
    /** Lord Chaos rolls two vast dice, landing at (x,y,z). id = the two faces, as tens and units. time = how long they tumble. */
    public static final byte CHAOS_DICE = 25;
    /** A star falls from (x,y,z) onto (a,b,c). time = how long it falls. */
    public static final byte CHAOS_STAR = 26;
    /** A little rift of deep space wanders at (x,y,z), a across. Sent every couple of ticks while it lives. */
    public static final byte CHAOS_WANDER = 27;
    /** Chaos's beam of deep space, from his maw at (x,y,z) to (a,b,c), for this instant. */
    public static final byte CHAOS_BEAM = 28;
    /** One of the In-Betweener's paradoxes takes hold at (x,y,z): two rings, one white and one black, turning through each other. a = radius. */
    public static final byte PARADOX = 29;

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
