package com.matoon.eternity.api.event;

import anvil.infinity.snap.SnapResult;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

/** Someone with all six stones snaps, on the server. Posted on {@code MinecraftForge.EVENT_BUS}. */
public abstract class SnapEvent extends Event {

    private final EntityLivingBase snapper;
    private final SnapResult mode;

    protected SnapEvent(EntityLivingBase snapper, SnapResult mode) {
        this.snapper = snapper;
        this.mode = mode;
    }

    public EntityLivingBase getSnapper() {
        return snapper;
    }

    /** What the Mind Stone has set the snap to do. */
    public SnapResult getMode() {
        return mode;
    }

    /** Before anything happens. Cancel it and the snap fails, with no wear and no burn. */
    @Cancelable
    public static class Pre extends SnapEvent {
        public Pre(EntityLivingBase snapper, SnapResult mode) {
            super(snapper, mode);
        }
    }

    /** After a snap that worked: the gauntlet has worn, the arm burned, the story heard. */
    public static class Post extends SnapEvent {
        public Post(EntityLivingBase snapper, SnapResult mode) {
            super(snapper, mode);
        }
    }
}
