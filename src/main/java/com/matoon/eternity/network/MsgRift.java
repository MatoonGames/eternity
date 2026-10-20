package com.matoon.eternity.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/** Opens or closes a tear in reality. Broadcast to the whole dimension, regardless of distance. */
public class MsgRift implements IMessage {

    public static final byte OPEN = 0;
    public static final byte CLOSE = 1;

    /** Part of the finale: enormous, and silent up close. */
    public static final int FLAG_FINALE = 1;
    /** Not a rift but the sky itself breaking. x,y,z is a unit direction; height is the span in degrees. */
    public static final int FLAG_SKY_FRACTURE = 2;

    public int id;
    public byte action;
    public double x, y, z;
    public float height;
    public long seed;
    public int flags;

    public MsgRift() {
    }

    public static MsgRift open(int id, double x, double y, double z, float height, long seed, int flags) {
        MsgRift msg = new MsgRift();
        msg.id = id;
        msg.action = OPEN;
        msg.x = x;
        msg.y = y;
        msg.z = z;
        msg.height = height;
        msg.seed = seed;
        msg.flags = flags;
        return msg;
    }

    public static MsgRift close(int id) {
        MsgRift msg = new MsgRift();
        msg.id = id;
        msg.action = CLOSE;
        return msg;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        id = buf.readInt();
        action = buf.readByte();
        if (action == OPEN) {
            x = buf.readDouble();
            y = buf.readDouble();
            z = buf.readDouble();
            height = buf.readFloat();
            seed = buf.readLong();
            flags = buf.readByte();
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(id);
        buf.writeByte(action);
        if (action == OPEN) {
            buf.writeDouble(x);
            buf.writeDouble(y);
            buf.writeDouble(z);
            buf.writeFloat(height);
            buf.writeLong(seed);
            buf.writeByte(flags);
        }
    }
}
