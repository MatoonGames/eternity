package com.matoon.eternity.network;

import com.matoon.eternity.item.ItemEinherjarSpear;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * A player struck with an Einherjar's spear at something beyond a sword's reach, which vanilla
 * would not let them hit. The server measures the reach itself, checks they can see it, and strikes
 * with their own spear and cooldown.
 */
public class MsgSpearStrike implements IMessage {

    public int entityId;

    public MsgSpearStrike() {
    }

    public MsgSpearStrike(int entityId) {
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

    public static class Handler implements IMessageHandler<MsgSpearStrike, IMessage> {
        @Override
        public IMessage onMessage(MsgSpearStrike message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                Entity target = player.world.getEntityByID(message.entityId);
                if (target == null || target == player || !ItemEinherjarSpear.wields(player) || !(target instanceof EntityLivingBase)
                        || !target.isEntityAlive() || player.isRiding()) {
                    return;
                }
                double reach = ItemEinherjarSpear.REACH + target.width * 0.5D + 0.5D;
                if (player.getDistanceSq(target.posX, target.posY + target.height * 0.5D, target.posZ) <= reach * reach
                        && player.canEntityBeSeen(target)) {
                    player.attackTargetEntityWithCurrentItem(target);
                }
            });
            return null;
        }
    }
}
