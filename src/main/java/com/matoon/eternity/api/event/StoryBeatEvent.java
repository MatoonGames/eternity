package com.matoon.eternity.api.event;

import net.minecraftforge.fml.common.eventhandler.Event;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * The story has moved on: a beat (a named moment, like "warlock_took" or "arena_called") has just been recorded for
 * the first time, on the server. Posted on {@code MinecraftForge.EVENT_BUS}. Beats shared by a fellowship are recorded
 * once for all of it; beats of the world as a whole have no player.
 */
public class StoryBeatEvent extends Event {

    @Nullable
    private final UUID player;
    private final String beat;

    public StoryBeatEvent(@Nullable UUID player, String beat) {
        this.player = player;
        this.beat = beat;
    }

    /** Whose story it is, or null for the world's. */
    @Nullable
    public UUID getPlayer() {
        return player;
    }

    public String getBeat() {
        return beat;
    }
}
