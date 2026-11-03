package com.matoon.eternity.network;

import com.matoon.eternity.story.StoryDirector;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * A player answers something the story asked them, or tells it a scene they were watching has
 * finished. The server checks the answer belongs to the conversation it is waiting on.
 */
public class MsgStoryAct implements IMessage {

    /** An answer: id = the conversation, value = which answer. */
    public static final byte ANSWER = 0;
    /** A cutscene finished or was skipped: id = which. */
    public static final byte SCENE_DONE = 1;
    /** Right-clicked toward the cube where it hangs in its ward, with nothing solid in reach. */
    public static final byte LIFT = 2;
    /** Right-clicked with an empty hand while the vault was showing. */
    public static final byte REACH = 3;
    /** Done reading the line on screen: move on. Everyone in the conversation has to be. */
    public static final byte SKIP = 4;

    public byte type;
    public int id, value;

    public MsgStoryAct() {
    }

    public MsgStoryAct(byte type, int id, int value) {
        this.type = type;
        this.id = id;
        this.value = value;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        type = buf.readByte();
        id = buf.readInt();
        value = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(type);
        buf.writeInt(id);
        buf.writeInt(value);
    }

    public static class Handler implements IMessageHandler<MsgStoryAct, IMessage> {
        @Override
        public IMessage onMessage(MsgStoryAct message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> StoryDirector.INSTANCE.act(player, message.type, message.id, message.value));
            return null;
        }
    }
}
