package com.matoon.eternity.compat;

import me.guichaguri.tickratechanger.api.TickrateAPI;

/** Bends the flow of time for the whole server through Tickrate Changer. */
public final class Tickrate {

    public static final float NORMAL = 20F;

    private static float current = NORMAL;

    private Tickrate() {
    }

    /** Sets server and client tickrate, silently. Small changes are skipped to avoid packet spam. */
    public static void set(float rate) {
        float clamped = Math.max(0.5F, Math.min(100F, rate));
        if (Math.abs(clamped - current) < 0.05F) {
            return;
        }
        current = clamped;
        TickrateAPI.changeTickrate(clamped, false);
    }

    public static void restore() {
        current = -1F;
        set(NORMAL);
    }

    public static float current() {
        return current;
    }

    /** Players joining mid-effect get the current rate too. */
    public static void resend() {
        float rate = current;
        current = -1F;
        set(rate < 0F ? NORMAL : rate);
    }
}
