package com.matoon.eternity.network;

import com.matoon.eternity.world.TribunalDirector;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * A defendant's answer to the Living Tribunal. The server only accepts it from the
 * player on trial, for the question currently being asked, and keeps the score itself.
 */
public class MsgTribunalAnswer implements IMessage {

    public int node;
    public int answer;

    public MsgTribunalAnswer() {
    }

    public MsgTribunalAnswer(int node, int answer) {
        this.node = node;
        this.answer = answer;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        node = buf.readInt();
        answer = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(node);
        buf.writeInt(answer);
    }

    public static class Handler implements IMessageHandler<MsgTribunalAnswer, IMessage> {
        @Override
        public IMessage onMessage(MsgTribunalAnswer message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> TribunalDirector.INSTANCE.answer(player, message.node, message.answer));
            return null;
        }
    }
}
