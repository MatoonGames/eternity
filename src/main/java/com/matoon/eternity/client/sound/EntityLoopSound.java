package com.matoon.eternity.client.sound;

import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.entity.Entity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A loop that rides along with an entity, gliding toward a target volume so it swells in
 * and dies away instead of switching.
 */
@SideOnly(Side.CLIENT)
public class EntityLoopSound extends MovingSound {

    private final Entity entity;
    private final float fadeRate;
    private float target;
    private float current;
    private boolean stopping;

    public EntityLoopSound(SoundEvent event, SoundCategory category, Entity entity, float target, float fadeRate) {
        super(event, category);
        this.entity = entity;
        this.target = target;
        this.fadeRate = fadeRate;
        this.repeat = true;
        this.repeatDelay = 0;
        this.attenuationType = ISound.AttenuationType.LINEAR;
        this.volume = 0.001F;
        follow();
    }

    public EntityLoopSound pitch(float pitch) {
        this.pitch = pitch;
        return this;
    }

    public void setTarget(float target) {
        if (!stopping) {
            this.target = target;
        }
    }

    public void fadeOut() {
        target = 0F;
        stopping = true;
    }

    @Override
    public void update() {
        if (entity.isDead) {
            fadeOut();
        }
        current += (target - current) * fadeRate;
        volume = Math.max(0.001F, current);
        follow();
        if (stopping && current < 0.003F) {
            donePlaying = true;
        }
    }

    private void follow() {
        xPosF = (float) entity.posX;
        yPosF = (float) (entity.posY + entity.height * 0.6D);
        zPosF = (float) entity.posZ;
    }
}
