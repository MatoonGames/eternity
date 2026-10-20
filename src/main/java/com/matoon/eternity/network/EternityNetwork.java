package com.matoon.eternity.network;

import com.matoon.eternity.Eternity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Packets flow server to client, with three exceptions: {@link MsgTearRift}, where a
 * player asks for a Rift Gate, {@link MsgStrikeComet}, a swing at the comet, and
 * {@link MsgTribunalAnswer}, an answer to the Living Tribunal. The server re-checks
 * every part of those requests, so a modified client cannot forge its way to
 * anything it could not do legitimately.
 */
public final class EternityNetwork {

    public static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(Eternity.MODID);

    private EternityNetwork() {
    }

    public static void init() {
        ClientBound handler = new ClientBound();
        int id = 0;
        CHANNEL.registerMessage(handler, MsgPresence.class, id++, Side.CLIENT);
        CHANNEL.registerMessage(handler, MsgRift.class, id++, Side.CLIENT);
        CHANNEL.registerMessage(handler, MsgOmen.class, id++, Side.CLIENT);
        CHANNEL.registerMessage(handler, MsgCue.class, id++, Side.CLIENT);
        CHANNEL.registerMessage(handler, MsgFinale.class, id++, Side.CLIENT);
        CHANNEL.registerMessage(handler, MsgStoneTarget.class, id++, Side.CLIENT);
        CHANNEL.registerMessage(handler, MsgOpenRiftGate.class, id++, Side.CLIENT);
        CHANNEL.registerMessage(new MsgTearRift.Handler(), MsgTearRift.class, id++, Side.SERVER);
        CHANNEL.registerMessage(handler, MsgComet.class, id++, Side.CLIENT);
        CHANNEL.registerMessage(new MsgStrikeComet.Handler(), MsgStrikeComet.class, id++, Side.SERVER);
        CHANNEL.registerMessage(handler, MsgTribunal.class, id++, Side.CLIENT);
        CHANNEL.registerMessage(new MsgTribunalAnswer.Handler(), MsgTribunalAnswer.class, id++, Side.SERVER);
        CHANNEL.registerMessage(handler, MsgTribunalFx.class, id++, Side.CLIENT);
        CHANNEL.registerMessage(new MsgStrikeTribunal.Handler(), MsgStrikeTribunal.class, id++, Side.SERVER);
    }

    public static void sendToServer(IMessage message) {
        CHANNEL.sendToServer(message);
    }

    public static void send(IMessage message, EntityPlayerMP player) {
        CHANNEL.sendTo(message, player);
    }

    public static void sendToDimension(IMessage message, int dimension) {
        CHANNEL.sendToDimension(message, dimension);
    }

    public static void sendToAll(IMessage message) {
        CHANNEL.sendToAll(message);
    }

    public static void sendAround(IMessage message, int dimension, double x, double y, double z, double range) {
        CHANNEL.sendToAllAround(message, new NetworkRegistry.TargetPoint(dimension, x, y, z, range));
    }

    /** Hands every message to the proxy, which only acts on the physical client. */
    public static class ClientBound implements IMessageHandler<IMessage, IMessage> {
        @Override
        public IMessage onMessage(IMessage message, MessageContext ctx) {
            Eternity.proxy.receive(message);
            return null;
        }
    }
}
