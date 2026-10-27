package com.matoon.eternity.network;

import com.matoon.eternity.inventory.ContainerDestroyerArmor;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * A button pressed in the Destroyer Armor's systems menu: forging, painting, turning its overcharge. Vanilla's
 * enchanting-table packet carries only a byte, too little for all of them, so they come this way, to the same
 * {@link ContainerDestroyerArmor#enchantItem}, and only for the menu the sender has open.
 */
public class MsgArmorButton implements IMessage {

    public int window;
    public int id;

    public MsgArmorButton() {
    }

    public MsgArmorButton(int window, int id) {
        this.window = window;
        this.id = id;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        window = buf.readInt();
        id = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(window);
        buf.writeInt(id);
    }

    public static class Handler implements IMessageHandler<MsgArmorButton, IMessage> {
        @Override
        public IMessage onMessage(MsgArmorButton message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                if (player.openContainer instanceof ContainerDestroyerArmor && player.openContainer.windowId == message.window
                        && player.openContainer.canInteractWith(player)) {
                    player.openContainer.enchantItem(player, message.id);
                }
            });
            return null;
        }
    }
}
