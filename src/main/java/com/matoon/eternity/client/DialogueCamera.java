package com.matoon.eternity.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

/**
 * While someone is talking to you, you look at them. Whenever a conversation screen is
 * open, your view turns smoothly toward whoever is speaking: the In-Betweener, the Living
 * Tribunal on his throne, or whichever of Master Order and Lord Chaos has the line, turning
 * from one to the other as they take turns. Once they have finished, you keep looking at
 * whoever spoke last while you answer.
 */
@SideOnly(Side.CLIENT)
public final class DialogueCamera {

    public static final DialogueCamera INSTANCE = new DialogueCamera();

    /** How quickly your view catches up with the speaker; higher is snappier. */
    private static final float TURN_RATE = 4.5F;

    private final Minecraft mc = Minecraft.getMinecraft();
    private long lastFrame;

    private DialogueCamera() {
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        long now = System.nanoTime();
        float seconds = lastFrame == 0L ? 0F : Math.min(0.1F, (now - lastFrame) / 1.0E9F);
        lastFrame = now;
        EntityPlayerSP player = mc.player;
        if (player == null || mc.world == null) {
            return;
        }
        Vec3d target = speaker(event.renderTickTime);
        if (target == null) {
            return;
        }
        Vec3d to = target.subtract(player.getPositionEyes(event.renderTickTime));
        double flat = Math.sqrt(to.x * to.x + to.z * to.z);
        if (to.lengthSquared() < 0.25D) {
            return;
        }
        float yaw = (float) Math.toDegrees(Math.atan2(-to.x, to.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(to.y, flat));
        float k = 1F - (float) Math.exp(-seconds * TURN_RATE);
        float turnYaw = MathHelper.wrapDegrees(yaw - player.rotationYaw) * k;
        float turnPitch = (MathHelper.clamp(pitch, -90F, 90F) - player.rotationPitch) * k;
        // Both ends of this frame's interpolation move together, so the turn is smooth.
        player.rotationYaw += turnYaw;
        player.prevRotationYaw += turnYaw;
        player.rotationPitch += turnPitch;
        player.prevRotationPitch += turnPitch;
        player.rotationYawHead += turnYaw;
        player.prevRotationYawHead += turnYaw;
    }

    @Nullable
    private Vec3d speaker(float partialTicks) {
        if (mc.currentScreen instanceof GuiInBetweener) {
            return ((GuiInBetweener) mc.currentScreen).speakerAt(partialTicks);
        }
        if (mc.currentScreen instanceof GuiTribunal) {
            return TribunalClient.INSTANCE.speakerAt();
        }
        return null;
    }
}
