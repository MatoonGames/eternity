package com.matoon.eternity.network;

import com.matoon.eternity.world.GrandmasterDirector;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * A player at the Grandmaster's board lays a hand on a square: on the piece standing there to
 * take it up, or on an empty square to move there. The client works out which square from
 * the pieces it draws, which the server has no shape for; the server checks the player is on
 * the board, near enough to it, and that it is their move.
 */
public class MsgGrandmasterAct implements IMessage {

    public int square;

    public MsgGrandmasterAct() {
    }

    public MsgGrandmasterAct(int square) {
        this.square = square;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        square = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(square);
    }

    public static class Handler implements IMessageHandler<MsgGrandmasterAct, IMessage> {
        @Override
        public IMessage onMessage(MsgGrandmasterAct message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> GrandmasterDirector.INSTANCE.pick(player, message.square));
            return null;
        }
    }
}
