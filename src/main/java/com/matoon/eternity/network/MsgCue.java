package com.matoon.eternity.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/** A short-lived combat visual or audio cue around Eternity. */
public class MsgCue implements IMessage {

    /** Eternity locks eyes with the target. a = duration in ticks. */
    public static final byte STARE = 0;
    /** He vanishes at (x,y,z) and reappears at (a,b,c). */
    public static final byte BLINK = 1;
    /** A blow passes through him harmlessly. */
    public static final byte PHASE = 2;
    /** A lance rift opens at (x,y,z), aimed at (a,b,c). */
    public static final byte LANCE_WARN = 3;
    /** The lance fires from (x,y,z) to (a,b,c). */
    public static final byte LANCE_FIRE = 4;
    /** A black hole forms at (x,y,z). a = max radius. */
    public static final byte SINGULARITY = 5;
    /** The black hole detonates. a = radius. */
    public static final byte SINGULARITY_BURST = 6;
    /** His touch lands at (x,y,z). */
    public static final byte UNMAKE = 7;
    /** A star is about to fall on (x,y,z). */
    public static final byte STAR_WARN = 8;
    /** The ground erupts at (x,y,z). */
    public static final byte ERUPT = 9;
    /** The local player stepped through a rift. */
    public static final byte TRAVERSE = 10;

    public byte type;
    public int entityId;
    public double x, y, z;
    public double a, b, c;

    public MsgCue() {
    }

    public MsgCue(byte type, int entityId, double x, double y, double z, double a, double b, double c) {
        this.type = type;
        this.entityId = entityId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.a = a;
        this.b = b;
        this.c = c;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        type = buf.readByte();
        entityId = buf.readInt();
        x = buf.readDouble();
        y = buf.readDouble();
        z = buf.readDouble();
        a = buf.readDouble();
        b = buf.readDouble();
        c = buf.readDouble();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(type);
        buf.writeInt(entityId);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeDouble(a);
        buf.writeDouble(b);
        buf.writeDouble(c);
    }
}
