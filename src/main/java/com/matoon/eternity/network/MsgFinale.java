package com.matoon.eternity.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/** Advances the death sequence, or the end of the world. Sent to every player in every dimension. */
public class MsgFinale implements IMessage {

    public static final int FRACTURE = 0;
    public static final int UNRAVEL = 1;
    public static final int COLLAPSE = 2;
    public static final int SUPERNOVA = 3;
    public static final int SILENCE = 4;
    public static final int AFTERMATH = 5;
    public static final int END = 6;
    /** The Astral Regulator is drawing his energy back in. level = progress. */
    public static final int CONTAIN = 7;
    /** Contained. The universe is saved. */
    public static final int SAVED = 8;
    /** He died uncontained and reality is coming apart. level = progress. */
    public static final int UNMAKING = 9;
    /** The final collapse. level = progress. */
    public static final int OBLIVION = 10;
    /** Nothing remains. */
    public static final int VOID = 11;

    public int stage;
    public int dimension;
    public double x, y, z;
    public float level;

    public MsgFinale() {
    }

    public MsgFinale(int stage, int dimension, double x, double y, double z) {
        this(stage, dimension, x, y, z, 0F);
    }

    public MsgFinale(int stage, int dimension, double x, double y, double z, float level) {
        this.stage = stage;
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
        this.level = level;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        stage = buf.readByte();
        dimension = buf.readInt();
        x = buf.readDouble();
        y = buf.readDouble();
        z = buf.readDouble();
        level = buf.readFloat();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(stage);
        buf.writeInt(dimension);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeFloat(level);
    }
}
