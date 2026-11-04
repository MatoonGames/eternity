package com.matoon.eternity.stones.network;

import com.matoon.eternity.stones.Stones;
import com.matoon.eternity.stones.mind.MindControlManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.UUID;

/**
 * Whatever the Mind Stone's holder asks of someone they control, from the ability bar or the window into them: open
 * that window, drain them or let them go, turn their autopilot on or off, or have them use one of their powers. Or, from
 * a player resisting, when they pressed. The server checks that the asker really controls them (or is resisting) first.
 */
public class MsgMindControlAct implements IMessage {

    public static final int OPEN = 0, DRAIN = 1, RELEASE = 2, AUTOCAST_ON = 3, AUTOCAST_OFF = 4, TRIGGER = 5, RESIST = 6;

    private int act;
    private UUID target;
    private String key;
    private int elapsed;

    public MsgMindControlAct() {
    }

    private MsgMindControlAct(int act, UUID target, String key, int elapsed) {
        this.act = act;
        this.target = target;
        this.key = key;
        this.elapsed = elapsed;
    }

    public static MsgMindControlAct of(int act, UUID target) {
        return new MsgMindControlAct(act, target, "", 0);
    }

    public static MsgMindControlAct trigger(UUID target, String key) {
        return new MsgMindControlAct(TRIGGER, target, key, 0);
    }

    /** {@code token} is the attempt's; {@code elapsed}, the ticks since it began. */
    public static MsgMindControlAct resist(UUID token, int elapsed) {
        return new MsgMindControlAct(RESIST, token, "", elapsed);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(act);
        StoneMessage.writeUuid(buf, target);
        ByteBufUtils.writeUTF8String(buf, key);
        buf.writeInt(elapsed);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        act = buf.readByte();
        target = StoneMessage.readUuid(buf);
        key = ByteBufUtils.readUTF8String(buf);
        elapsed = buf.readInt();
    }

    public static class Handler implements IMessageHandler<MsgMindControlAct, IMessage> {
        @Override
        public IMessage onMessage(MsgMindControlAct message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            if (Stones.active()) {
                player.getServerWorld().addScheduledTask(() -> handle(player, message));
            }
            return null;
        }

        private static void handle(EntityPlayerMP player, MsgMindControlAct message) {
            switch (message.act) {
                case OPEN:
                    MindControlManager.openMenu(player, message.target);
                    break;
                case DRAIN:
                case RELEASE:
                    MindControlManager.release(player, message.target, message.act == DRAIN);
                    break;
                case AUTOCAST_ON:
                case AUTOCAST_OFF:
                    MindControlManager.setAbilityAutocast(player, message.target, message.act == AUTOCAST_ON);
                    break;
                case TRIGGER:
                    MindControlManager.triggerAbility(player, message.target, message.key);
                    break;
                case RESIST:
                    MindControlManager.resistanceResponse(player, message.target, message.elapsed);
                    break;
                default:
                    break;
            }
        }
    }
}
