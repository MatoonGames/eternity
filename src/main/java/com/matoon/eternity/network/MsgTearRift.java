package com.matoon.eternity.network;

import com.matoon.eternity.world.CosmicPowers;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * The one packet a client sends: where the player wants their Rift Gate to lead.
 * Everything in it is checked again on the server before anything happens.
 */
public class MsgTearRift implements IMessage {

    public int dimension;
    public double x, y, z;

    public MsgTearRift() {
    }

    public MsgTearRift(int dimension, double x, double y, double z) {
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        dimension = buf.readInt();
        x = buf.readDouble();
        y = buf.readDouble();
        z = buf.readDouble();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(dimension);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
    }

    public static class Handler implements IMessageHandler<MsgTearRift, IMessage> {
        @Override
        public IMessage onMessage(MsgTearRift message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() ->
                    CosmicPowers.INSTANCE.riftGate(player, message.dimension, message.x, message.y, message.z));
            return null;
        }
    }
}
