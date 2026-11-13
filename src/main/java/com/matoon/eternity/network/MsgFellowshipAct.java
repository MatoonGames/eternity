package com.matoon.eternity.network;

import com.matoon.eternity.story.Fellowship;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Something done in the fellowship menu: looked at it, asked someone along, answered them, or left. */
public class MsgFellowshipAct implements IMessage {

    /** The menu is open (or still open): send who is who. */
    public static final byte LOOK = 0;
    public static final byte INVITE = 1;
    public static final byte ACCEPT = 2;
    public static final byte DECLINE = 3;
    public static final byte LEAVE = 4;

    public byte type;
    /** Whom it is about, by name; empty for LOOK and LEAVE. */
    public String name = "";

    public MsgFellowshipAct() {
    }

    public MsgFellowshipAct(byte type, String name) {
        this.type = type;
        this.name = name;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        type = buf.readByte();
        name = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(type);
        ByteBufUtils.writeUTF8String(buf, name);
    }

    public static class Handler implements IMessageHandler<MsgFellowshipAct, IMessage> {
        @Override
        public IMessage onMessage(MsgFellowshipAct message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> Fellowship.act(player, message.type, message.name));
            return null;
        }
    }
}
