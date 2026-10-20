package com.matoon.eternity.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/** Where the Cosmic Comet is and what is happening to it, for everyone in its dimension. */
public class MsgComet implements IMessage {

    /** Where it is: (x,y,z), velocity (vx,vy,vz) in blocks per tick, size and remaining integrity. */
    public static final byte SYNC = 0;
    /** It is not in your dimension, or not anywhere. */
    public static final byte HIDE = 1;
    /** It has just entered your sky. */
    public static final byte APPEAR = 2;
    /** A blow landed at (x,y,z). */
    public static final byte HIT = 3;
    /** Something thrown from too far burned up at (x,y,z). */
    public static final byte REPELLED = 4;
    /** It answers a wound with a flare of heat from (x,y,z). radius = reach of the flare. */
    public static final byte FLARE = 5;
    /** Its heart starts to break open. */
    public static final byte CRACK = 6;
    /** It bursts apart at (x,y,z). radius = its own radius. */
    public static final byte SHATTER = 7;
    /** It slipped away and is streaking off. */
    public static final byte DEPART = 8;
    /** The Astral Regulator it left behind still hangs at (x,y,z). */
    public static final byte RELIC = 9;
    /** Someone has taken the Regulator. */
    public static final byte RELIC_GONE = 10;

    /** Stages carried by {@link #SYNC}. */
    public static final byte PASSING = 0;
    public static final byte CRACKING = 1;
    public static final byte DEPARTING = 2;

    public byte type;
    public byte stage;
    public boolean space;
    public double x, y, z;
    public float vx, vy, vz;
    public float radius;
    public float integrity;

    public MsgComet() {
    }

    private MsgComet(byte type, double x, double y, double z, float radius) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.z = z;
        this.radius = radius;
    }

    public static MsgComet sync(byte stage, boolean space, double x, double y, double z,
                                double vx, double vy, double vz, float radius, float integrity) {
        MsgComet m = new MsgComet(SYNC, x, y, z, radius);
        m.stage = stage;
        m.space = space;
        m.vx = (float) vx;
        m.vy = (float) vy;
        m.vz = (float) vz;
        m.integrity = integrity;
        return m;
    }

    public static MsgComet hide() {
        return new MsgComet(HIDE, 0, 0, 0, 0F);
    }

    public static MsgComet event(byte type, boolean space, double x, double y, double z, float radius) {
        MsgComet m = new MsgComet(type, x, y, z, radius);
        m.space = space;
        return m;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        type = buf.readByte();
        stage = buf.readByte();
        space = buf.readBoolean();
        x = buf.readDouble();
        y = buf.readDouble();
        z = buf.readDouble();
        vx = buf.readFloat();
        vy = buf.readFloat();
        vz = buf.readFloat();
        radius = buf.readFloat();
        integrity = buf.readFloat();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(type);
        buf.writeByte(stage);
        buf.writeBoolean(space);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeFloat(vx);
        buf.writeFloat(vy);
        buf.writeFloat(vz);
        buf.writeFloat(radius);
        buf.writeFloat(integrity);
    }
}
