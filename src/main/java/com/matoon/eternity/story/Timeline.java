package com.matoon.eternity.story;

import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Something staged to the tick: things that happen at a moment, and movements that run from
 * one moment to another, each told how far through it is. Ticked once a server tick until it
 * has played out. The scenes' conversations wait on a Talk; what bodies do between the words
 * (a fight, a fall) is a Timeline, so it runs at its own pace whatever is being said over it.
 */
final class Timeline {

    /** A movement: told, each tick it runs, how far through it is, from 0 to 1. */
    interface Motion {
        void at(float k);
    }

    private static final class Event {
        final int tick;
        final Runnable run;

        Event(int tick, Runnable run) {
            this.tick = tick;
            this.run = run;
        }
    }

    private static final class Span {
        final int from, to;
        final Motion motion;

        Span(int from, int to, Motion motion) {
            this.from = from;
            this.to = to;
            this.motion = motion;
        }
    }

    private final List<Event> events = new ArrayList<>();
    private final List<Span> spans = new ArrayList<>();
    private int now = -1;
    private int length;
    private boolean stopped;

    /** Something that happens at a tick. */
    Timeline at(int tick, Runnable run) {
        events.add(new Event(tick, run));
        length = Math.max(length, tick);
        return this;
    }

    /** A movement from one tick to another, inclusive; told k = 0 at the first and 1 at the last. */
    Timeline span(int from, int to, Motion motion) {
        spans.add(new Span(from, to, motion));
        length = Math.max(length, to);
        return this;
    }

    /** One tick on. False once it has played out (or been stopped). */
    boolean tick() {
        if (stopped) {
            return false;
        }
        now++;
        // In the order they were written, so a later line can rely on what an earlier one set up.
        for (Event e : new ArrayList<>(events)) {
            if (e.tick == now) {
                e.run.run();
            }
        }
        for (Span s : new ArrayList<>(spans)) {
            if (now >= s.from && now <= s.to) {
                s.motion.at(s.to == s.from ? 1F : (now - s.from) / (float) (s.to - s.from));
            }
        }
        return !stopped && now < length;
    }

    void stop() {
        stopped = true;
    }

    int now() {
        return now;
    }

    // ------------------------------------------------------------------ easing, for the movements

    static Vec3d lerp(Vec3d a, Vec3d b, float k) {
        return a.add(b.subtract(a).scale(k));
    }

    static float smooth(float x) {
        x = Math.max(0F, Math.min(1F, x));
        return x * x * (3F - 2F * x);
    }

    static float out(float x) {
        x = 1F - Math.max(0F, Math.min(1F, x));
        return 1F - x * x * x;
    }

    static float in(float x) {
        x = Math.max(0F, Math.min(1F, x));
        return x * x;
    }
}
