package com.matoon.eternity.entity;

/**
 * Death taking off the pretty one: the timeline everything about it keeps to. The pose, the
 * renderer, the gore and the sounds on each client, and the server that ends it, all count the
 * same ticks from when she starts.
 * <p>
 * She shudders, and the skin splits along its seams; she claws at her face; she peels the left
 * hand off like a glove, then the right; she digs her fingers in at her hairline and drags her
 * face off over her head; she tears the body open down the front and throws it off her
 * shoulders; she shoves the rest down off her hips and steps out of it. Then she straightens,
 * taller than the thing she was wearing, and breathes out.
 */
public final class SkinTear {

    /** How long it takes, start to finish. */
    public static final int TICKS = 170;
    /** When each piece of the skin comes away. */
    public static final int[] AT = {56, 70, 96, 116, 134, 144};
    /** The model's parts that go with each piece. */
    public static final String[][] PIECES = {
            {"foreL", "handL", "fingersL", "thumbL", "flapL"},
            {"foreR", "handR", "fingersR", "thumbR", "flapR"},
            {"head", "lid", "mouth", "braid0", "braid1", "braid2", "eye", "jaw"},
            {"chest", "abdomen", "upperR", "upperL", "gut"},
            {"pelvis", "thighR", "thighL"},
            {"shinR", "shinL", "footR", "footL"}};
    /**
     * Those of each piece that are seen to fly or fall, and where from: across (+ to her left),
     * up and forward of her feet, in blocks; and how they go: across, up and forward, per tick.
     */
    public static final String[][] FLUNG = {
            {"foreL", "handL"},
            {"foreR", "handR"},
            {"head", "braid0"},
            {"chest", "upperR", "upperL", "abdomen"},
            {"pelvis", "thighR", "thighL"},
            {"shinR", "shinL", "footR", "footL"}};
    public static final double[][][] FROM = {
            {{0.12, 1.02, 0.32, 0.22, 0.2, 0.06}, {0.12, 0.85, 0.36, 0.24, 0.18, 0.08}},
            {{-0.12, 1.02, 0.32, -0.24, 0.22, 0.04}, {-0.12, 0.85, 0.36, -0.26, 0.2, 0.06}},
            {{0, 1.54, 0.18, 0.02, 0.18, 0.24}, {0, 1.5, -0.18, -0.03, 0.12, 0.1}},
            {{0, 1.16, 0.08, 0.0, 0.1, 0.14}, {-0.3, 1.45, 0, -0.2, 0.16, 0.02}, {0.3, 1.45, 0, 0.2, 0.16, 0.02}, {0, 0.88, 0.06, 0.03, 0.04, 0.1}},
            {{0, 0.77, 0, 0.0, 0.0, 0.02}, {-0.11, 0.77, 0, -0.03, 0.0, 0.03}, {0.11, 0.77, 0, 0.03, 0.0, 0.03}},
            {{-0.11, 0.38, 0, -0.06, 0.02, 0.04}, {0.11, 0.38, 0, 0.06, 0.02, 0.04}, {-0.11, 0.05, 0.05, -0.08, 0.04, 0.05}, {0.11, 0.05, 0.05, 0.08, 0.04, 0.05}}};

    private SkinTear() {
    }

    /** Which piece comes away on this tick, or -1. */
    public static int pieceAt(int t) {
        for (int i = 0; i < AT.length; i++) {
            if (AT[i] == t) {
                return i;
            }
        }
        return -1;
    }

    /** Whether a part of the skin is gone by t. */
    public static boolean gone(String part, float t) {
        for (int i = 0; i < AT.length; i++) {
            if (t >= AT[i]) {
                for (String p : PIECES[i]) {
                    if (p.equals(part)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Which piece of the skin covers a part of what is under it. */
    public static String under(String part) {
        if (part.startsWith("braid") || part.startsWith("lock") || part.equals("brow") || part.equals("nose")) {
            return "head";
        }
        if (part.equals("beads") || part.startsWith("wing") || part.equals("collar")) {
            return "chest";
        }
        if (part.startsWith("skirt")) {
            return "thighR";
        }
        return part;
    }

    /** When the dark comes down over her: once she is all but out of it, her hood, her cape and her ribbons, unrolling. */
    public static final int CLOTHED = 136, CLOTHING = 30;

    /** Whether a part of what is under it is cloth she puts on afterwards, not something the skin was covering. */
    public static boolean cloth(String part) {
        return part.startsWith("hood") || part.equals("brim") || part.startsWith("cape") || part.startsWith("drape") || part.startsWith("skirt")
                || part.equals("sash");
    }

    /** How far it has come down over her by t, 0 to 1. */
    public static float dressed(float t) {
        float k = clamp((t - CLOTHED) / CLOTHING);
        return k * k * (3F - 2F * k);
    }

    /** How far the skin has split along its seams, 0 to 1: it opens from the first shudder until she starts pulling it off. */
    public static float split(float t) {
        float k = clamp((t - 8F) / 84F);
        return k * k * (3F - 2F * k) * 0.5F;
    }

    /**
     * How big she is, inside it: small enough to fit until she is out, then she straightens to
     * her own height.
     */
    public static float inner(float t) {
        float k = clamp((t - 134F) / 30F);
        return 0.85F + (0.93F - 0.85F) * k * k * (3F - 2F * k);
    }

    private static float clamp(float x) {
        return x < 0F ? 0F : x > 1F ? 1F : x;
    }
}
