package com.matoon.eternity.network;

import com.matoon.eternity.world.InBetweenerDirector;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Something a visitor does in front of the In-Betweener: answers him, asks him to go on
 * once he has finished speaking, or strikes his glass. The server only accepts each from
 * the one visitor he is dealing with, at the moment it makes sense, and checks the rest.
 */
public class MsgInBetweenerAct implements IMessage {

    public static final byte ANSWER = 0;
    public static final byte CONTINUE = 1;
    public static final byte STRIKE = 2;

    public byte action;
    public int node;
    public int answer;

    public MsgInBetweenerAct() {
    }

    public MsgInBetweenerAct(byte action, int node, int answer) {
        this.action = action;
        this.node = node;
        this.answer = answer;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        action = buf.readByte();
        node = buf.readInt();
        answer = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(action);
        buf.writeInt(node);
        buf.writeInt(answer);
    }

    public static class Handler implements IMessageHandler<MsgInBetweenerAct, IMessage> {
        @Override
        public IMessage onMessage(MsgInBetweenerAct message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> InBetweenerDirector.INSTANCE.act(player, message.action, message.node, message.answer));
            return null;
        }
    }
}
