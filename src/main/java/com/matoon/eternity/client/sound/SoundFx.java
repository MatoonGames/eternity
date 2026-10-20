package com.matoon.eternity.client.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** One-shot sound helpers. */
@SideOnly(Side.CLIENT)
public final class SoundFx {

    private SoundFx() {
    }

    /**
     * Plays a sound "on the wind": no distance falloff, but placed a few blocks
     * away in the given direction so it pans and the listener can turn toward it.
     * The far sounds are pre-filtered to sound like they crossed miles of air.
     */
    public static void carried(SoundEvent event, float volume, float pitch, double angle) {
        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null) {
            return;
        }
        float x = (float) (player.posX + Math.cos(angle) * 8D);
        float y = (float) (player.posY + player.getEyeHeight() + 2D);
        float z = (float) (player.posZ + Math.sin(angle) * 8D);
        play(new PositionedSoundRecord(event.getSoundName(), SoundCategory.AMBIENT, volume, pitch, false, 0,
                ISound.AttenuationType.NONE, x, y, z));
    }

    /** Fills the head: no position, no falloff. */
    public static void everywhere(SoundEvent event, SoundCategory category, float volume, float pitch) {
        play(new PositionedSoundRecord(event.getSoundName(), category, volume, pitch, false, 0,
                ISound.AttenuationType.NONE, 0F, 0F, 0F));
    }

    public static void at(SoundEvent event, SoundCategory category, double x, double y, double z, float volume, float pitch) {
        play(new PositionedSoundRecord(event, category, volume, pitch, (float) x, (float) y, (float) z));
    }

    /** Angle, in the convention {@link #carried} uses, from the player toward a point. */
    public static double angleTo(double x, double z) {
        EntityPlayer player = Minecraft.getMinecraft().player;
        return player == null ? 0D : Math.atan2(z - player.posZ, x - player.posX);
    }

    /** Angle directly behind where the player is looking. */
    public static double angleBehind() {
        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null) {
            return 0D;
        }
        double yaw = Math.toRadians(player.rotationYaw);
        return Math.atan2(-Math.cos(yaw), Math.sin(yaw));
    }

    public static void play(ISound sound) {
        Minecraft.getMinecraft().getSoundHandler().playSound(sound);
    }
}
