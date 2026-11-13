package com.matoon.eternity.entity;

/**
 * How the pretty one moves on its own once it has rotted far enough: every so often a joint in it
 * snaps somewhere a joint can't go, holds there, and creaks back. Worked out from the same clock
 * on every client, so the model and the crack of it keep time.
 */
public final class Rot {

    /** The worst it gets before she tears it off. */
    public static final int MAX = 7;
    /** How far gone before she starts to twitch. */
    public static final int TWITCHES = 5;

    private Rot() {
    }

    /** Ticks between twitches: more often the worse it is. */
    public static int period(int rot) {
        return rot >= 7 ? 47 : rot == 6 ? 71 : 113;
    }

    /** How far into a twitch she is: 0 when not in one; snapping to 1 in a tick, held, creaking back. */
    public static float twitch(float age, int seed, int rot) {
        if (rot < TWITCHES) {
            return 0F;
        }
        float k = (age + seed * 13) % period(rot);
        if (k > 7F) {
            return 0F;
        }
        return k < 1F ? k : k < 3F ? 1F : 1F - (k - 3F) / 4F;
    }

    /** Which joint goes, this time: 0 the neck, sideways, too far; 1 the right elbow, backwards; 2 the head, round, too far. */
    public static int which(float age, int seed, int rot) {
        int n = (int) ((age + seed * 13) / period(rot));
        return Math.floorMod(n * 7 + seed, rot >= 7 ? 3 : 2);
    }

    /** Whether a twitch starts on this tick. */
    public static boolean snaps(int age, int seed, int rot) {
        return rot >= TWITCHES && Math.floorMod(age + seed * 13, period(rot)) == 0;
    }
}
