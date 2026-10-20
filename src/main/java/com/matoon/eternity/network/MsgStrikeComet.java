package com.matoon.eternity.network;

import com.matoon.eternity.world.CometDirector;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * A player swung at the Cosmic Comet. Vanilla refuses melee more than a few blocks
 * from an entity's centre, and the comet's surface is dozens of blocks from its
 * centre, so blows travel this way instead. The server measures the reach to the
 * surface itself and swings with the player's own weapon and cooldown.
 */
public class MsgStrikeComet implements IMessage {

    public int entityId;

    public MsgStrikeComet() {
    }

    public MsgStrikeComet(int entityId) {
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

    public static class Handler implements IMessageHandler<MsgStrikeComet, IMessage> {
        @Override
        public IMessage onMessage(MsgStrikeComet message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> CometDirector.INSTANCE.melee(player, message.entityId));
            return null;
        }
    }
}
