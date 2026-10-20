package com.matoon.eternity.client.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A sound whose volume glides toward a target, so ambience swells and dies away
 * instead of switching. Non-positional sounds ride along with the listener.
 */
@SideOnly(Side.CLIENT)
public class LoopSound extends MovingSound {

    private final boolean positional;
    private final float fadeRate;
    private float target;
    private float current;
    private boolean stopping;

    public LoopSound(SoundEvent event, SoundCategory category, boolean repeat, float fadeRate) {
        super(event, category);
        this.positional = false;
        this.fadeRate = fadeRate;
        this.repeat = repeat;
        this.repeatDelay = 0;
        this.attenuationType = ISound.AttenuationType.NONE;
        this.volume = 0.001F;
    }

    public LoopSound(SoundEvent event, SoundCategory category, double x, double y, double z, float fadeRate) {
        super(event, category);
        this.positional = true;
        this.fadeRate = fadeRate;
        this.repeat = true;
        this.repeatDelay = 0;
        this.attenuationType = ISound.AttenuationType.LINEAR;
        this.xPosF = (float) x;
        this.yPosF = (float) y;
        this.zPosF = (float) z;
        this.volume = 0.001F;
    }

    public LoopSound pitch(float pitch) {
        this.pitch = pitch;
        return this;
    }

    public void setTarget(float target) {
        this.target = target;
        if (target > 0F) {
            stopping = false;
        }
    }

    /** Snaps straight to a volume, for sounds that should hit at once. */
    public void setNow(float value) {
        this.target = value;
        this.current = value;
        this.volume = Math.max(0.001F, value);
    }

    public void fadeOut() {
        target = 0F;
        stopping = true;
    }

    public boolean isStopping() {
        return stopping;
    }

    @Override
    public void update() {
        current += (target - current) * fadeRate;
        volume = Math.max(0.001F, current);
        if (!positional) {
            EntityPlayer player = Minecraft.getMinecraft().player;
            if (player != null) {
                xPosF = (float) player.posX;
                yPosF = (float) (player.posY + player.getEyeHeight());
                zPosF = (float) player.posZ;
            }
        }
        if (stopping && current < 0.003F) {
            donePlaying = true;
        }
    }
}
