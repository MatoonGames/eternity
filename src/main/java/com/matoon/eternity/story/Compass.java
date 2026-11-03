package com.matoon.eternity.story;

import net.minecraft.util.math.Vec3d;

/** Which way something lies, in words: north, north-east, and so on. North is toward -z. */
final class Compass {

    private static final String[] POINTS = {"north", "north-east", "east", "south-east", "south", "south-west", "west", "north-west"};

    private Compass() {
    }

    /** The way from one place to another. */
    static String of(Vec3d from, Vec3d to) {
        double dx = to.x - from.x, dz = to.z - from.z;
        double deg = Math.toDegrees(Math.atan2(dx, -dz));
        int i = (int) Math.round(((deg % 360D) + 360D) % 360D / 45D) % 8;
        return POINTS[i];
    }

    static String capital(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
