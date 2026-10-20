package com.matoon.eternity.network;

import com.matoon.eternity.world.TribunalDirector;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * A swing at the Living Tribunal. Like the comet he is far too big for vanilla's
 * reach check, so the server measures from the player to his body and swings for them
 * with their own weapon and cooldown.
 */
public class MsgStrikeTribunal implements IMessage {

    public int entityId;

    public MsgStrikeTribunal() {
    }

    public MsgStrikeTribunal(int entityId) {
        this.entityId = entityId;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        entityId = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(entityId);
    }

    public static class Handler implements IMessageHandler<MsgStrikeTribunal, IMessage> {
        @Override
        public IMessage onMessage(MsgStrikeTribunal message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> TribunalDirector.INSTANCE.melee(player, message.entityId));
            return null;
        }
    }
}
