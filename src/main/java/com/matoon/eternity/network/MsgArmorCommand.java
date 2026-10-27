package com.matoon.eternity.network;

import com.matoon.eternity.entity.EntityDestroyerArmor;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Whoever is inside the Destroyer Armor pressed or let go of something: its fists, its ray, its
 * slam, or the way out. The armour itself checks that the sender is the one inside it.
 */
public class MsgArmorCommand implements IMessage {

    public byte command;
    public boolean down;
    /** Anything more the command needs: for its overcharge, the part and the step, packed. */
    public int value;

    public MsgArmorCommand() {
    }

    public MsgArmorCommand(EntityDestroyerArmor.Command command, boolean down) {
        this.command = (byte) command.ordinal();
        this.down = down;
    }

    public MsgArmorCommand(EntityDestroyerArmor.Command command, boolean down, int value) {
        this(command, down);
        this.value = value;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        command = buf.readByte();
        down = buf.readBoolean();
        value = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(command);
        buf.writeBoolean(down);
        buf.writeInt(value);
    }

    public static class Handler implements IMessageHandler<MsgArmorCommand, IMessage> {
        @Override
        public IMessage onMessage(MsgArmorCommand message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                EntityDestroyerArmor.Command[] commands = EntityDestroyerArmor.Command.values();
                if (message.command >= 0 && message.command < commands.length && player.getRidingEntity() instanceof EntityDestroyerArmor) {
                    ((EntityDestroyerArmor) player.getRidingEntity()).command(player, commands[message.command], message.down, message.value);
                }
            });
            return null;
        }
    }
}
