package com.matoon.eternity.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.network.play.server.SPacketUpdateHealth;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

/**
 * The death that is not one, when health past all measure falls back to an ordinary amount.
 * <p>
 * The client takes its health from the server as a blow: the difference from what it had, subtracted again. The Soul
 * Stone's immortality is near the largest number a float holds, and against it twenty is lost to rounding; the blow is
 * the whole of it, and health lands on nothing. The player lies dying and the death screen comes up, while the server
 * knows them alive. So the server's health is watched as it arrives, and where the blow would come out wrong the health
 * is set to it outright first, leaving vanilla no difference to take. A real death (nothing left) is left to vanilla.
 */
public final class HealthSync {

    public static final HealthSync INSTANCE = new HealthSync();

    private static final String HANDLER = "eternity:health_sync";

    private HealthSync() {
    }

    @SubscribeEvent
    public void onConnect(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        ChannelPipeline pipeline = event.getManager().channel().pipeline();
        if (pipeline.get(HANDLER) == null && pipeline.get("packet_handler") != null) {
            pipeline.addBefore("packet_handler", HANDLER, new Watch());
        }
    }

    /** Ahead of vanilla's own handling of the same packet, on the main thread. */
    private static void arrive(float health) {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player == null || !(health > 0F)) {
            return;
        }
        float had = player.getHealth();
        if (had > health || Float.isNaN(had)) {
            float landed = had - (had - health);
            if (!(Math.abs(landed - health) <= Math.max(0.5F, health * 0.001F))) {
                player.setHealth(health);
            }
        }
    }

    private static final class Watch extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (msg instanceof SPacketUpdateHealth) {
                float health = ((SPacketUpdateHealth) msg).getHealth();
                // Queued before vanilla queues its own handling, so it runs first.
                Minecraft.getMinecraft().addScheduledTask(() -> arrive(health));
            }
            super.channelRead(ctx, msg);
        }
    }
}
