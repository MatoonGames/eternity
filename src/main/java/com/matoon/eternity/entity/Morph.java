package com.matoon.eternity.entity;

/**
 * Death changing shape, in front of whoever is there to see it: the timeline the renderer, the
 * contortions, and the gore and sounds on each client all keep to, counted from the tick her
 * look changes.
 * <p>
 * It isn't smoke. It goes through her a piece at a time, the right arm first, then the left,
 * then the trunk, the legs, and the head last: each piece swells and knots as whatever is inside
 * it moves, darkens like a bruise, and bursts; and out of it, wet and red and too small, the new
 * one comes, and grows, and the skin closes over it. What is left of the old one sloughs off and
 * drops. While it happens she doubles up and writhes, the joints going where they can't, and her
 * head wrenches all the way round once and back.
 */
public final class Morph {

    /** How long it takes, start to finish. */
    public static final int TICKS = 90;
    /** When each region of her starts to go: right arm, left arm, trunk, legs, head. */
    public static final int[] AT = {4, 14, 28, 42, 58};
    /** How long each region takes, from the first swelling to the skin closed over the new one. */
    public static final int SPAN = 28;
    /** How far through a region the old flesh gives and the new comes out of it. */
    public static final float BURST = 0.5F;
    public static final int ARM_R = 0, ARM_L = 1, TRUNK = 2, LEGS = 3, HEAD = 4;
    /** Of each region, the parts seen to slough off and drop when it bursts. */
    public static final String[][] SLOUGH = {
            {"foreR", "handR"}, {"foreL", "handL"}, {"chest"}, {"thighR", "shinL"}, {"head"}};
    /** And where from: across (+ to her left), up, and forward of her feet, in blocks. */
    public static final double[][] FROM = {{-0.3, 1.0, 0.1}, {0.3, 1.0, 0.1}, {0, 1.2, 0.1}, {0, 0.5, 0.05}, {0, 1.6, 0.1}};

    private Morph() {
    }

    /** Which region a part of any of her shapes belongs to. */
    public static int region(String part) {
        if (part.startsWith("head") || part.equals("lid") || part.equals("mouth") || part.equals("jaw") || part.equals("eye")
                || part.startsWith("braid") || part.startsWith("lock") || part.equals("brow") || part.equals("nose") || part.equals("beard")
                || part.equals("bun") || part.equals("helm") || part.equals("brim") || part.startsWith("hood") || part.equals("knot")) {
            return HEAD;
        }
        if (part.startsWith("thigh") || part.startsWith("shin") || part.startsWith("foot") || part.equals("pelvis")
                || part.startsWith("skirt") || part.startsWith("apron")) {
            return LEGS;
        }
        boolean arm = part.startsWith("upper") || part.startsWith("fore") || part.startsWith("hand") || part.startsWith("fingers")
                || part.startsWith("thumb") || part.startsWith("flap") || part.startsWith("sleeve") || part.startsWith("drape");
        if (arm || part.equals("axe") || part.equals("trowel")) {
            return part.endsWith("L") ? ARM_L : ARM_R;
        }
        return TRUNK;
    }

    /** How far through a region is at t, 0 to 1. */
    public static float u(int region, float t) {
        return clamp((t - AT[region]) / SPAN);
    }

    /** The tick a region bursts. */
    public static int burstAt(int region) {
        return AT[region] + Math.round(SPAN * BURST);
    }

    /** Which region bursts on this tick, or -1. */
    public static int burstingAt(int t) {
        for (int i = 0; i < AT.length; i++) {
            if (burstAt(i) == t) {
                return i;
            }
        }
        return -1;
    }

    /** Around the moment a region bursts, 0 to 1 and back: when its joint goes the wrong way. */
    public static float snap(int region, float t) {
        float k = clamp((u(region, t) - 0.36F) / 0.3F);
        return (float) Math.sin(k * Math.PI);
    }

    /** How hard she is writhing, 0 to 1: in quickly, and out once her head has come through. */
    public static float writhe(float t) {
        return Math.min(clamp(t / 8F), clamp((TICKS - t) / 14F));
    }

    private static float clamp(float x) {
        return x < 0F ? 0F : x > 1F ? 1F : x;
    }
}
