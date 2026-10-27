package com.matoon.eternity.client.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Every box of Eternity's model, with where it sits on his 128x128 skin. Both the model and
 * tools/EternitySkin.java read this, so the skin is always painted for exactly the boxes the
 * model has; texture offsets are packed here automatically rather than kept by hand.
 *
 * Boxes are given in their part's own space, the usual model space (y down, his front at -z,
 * his right at -x). Parts with a side have a right and a left version, mirrored.
 *
 * Plain Java with nothing of Minecraft in it, so the skin tool can compile against it.
 */
public final class EternityBoxes {

    public static final int SIZE = 128;

    /** What a box is made of, which decides how the skin tool paints it. */
    public static final int COSMOS = 0, HOOD = 1, SKULL = 2, FACE = 3, BROW = 4, BONE = 5, CLOAK = 6, HEM = 7, CREST = 8, MANTLE = 9;

    public static final class Box {
        public final String part;
        public final float x, y, z, delta;
        public final int w, h, d, material;
        /** Which side of him it is on: -1 right, 1 left, 0 the middle. */
        public final int side;
        public int u, v;

        Box(String part, float x, float y, float z, int w, int h, int d, float delta, int material, int side) {
            this.part = part;
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
            this.h = h;
            this.d = d;
            this.delta = delta;
            this.material = material;
            this.side = side;
        }

        public int footprintW() {
            return 2 * (w + d);
        }

        public int footprintH() {
            return d + h;
        }
    }

    private static final Map<String, List<Box>> PARTS = new LinkedHashMap<>();
    public static final List<Box> ALL = new ArrayList<>();

    /** The crests: how many segments in the upper hook and the lower one. */
    public static final int CREST_UP = 6, CREST_DOWN = 4;
    /** Segments in the cape down his back, and in each panel hung from a shoulder. */
    public static final int CAPE = 5, PANEL = 3;

    static {
        // The body.
        add("pelvis", -4, -1.5F, -2.5F, 8, 3, 5, 0, COSMOS);
        add("abdomen", -3.5F, -5.5F, -2, 7, 6, 4, 0, COSMOS);
        add("abdomen", -2.5F, -5, -2.4F, 5, 4, 1, -0.05F, COSMOS);
        add("chest", -4.5F, -5, -2.5F, 9, 5, 5, 0, COSMOS);
        add("chest", -5.5F, -9, -3, 11, 4, 6, 0, COSMOS);
        add("chest", -4, -8.6F, -3.45F, 8, 3, 1, 0, COSMOS);
        add("chest", -3.5F, -10, -2, 7, 1, 4, 0, COSMOS);
        add("chest", -1, -9, 2.4F, 2, 8, 1, -0.1F, COSMOS);
        add("neck", -1.5F, -3, -1.5F, 3, 3, 3, 0, COSMOS);

        // The head: a long skull, and the mask of his face on the front of it.
        add("head", -3, -8, -3.5F, 6, 8, 7, 0, SKULL);
        add("head", -3, -7.5F, -4.5F, 6, 7, 1, 0, FACE);
        add("head", -3.5F, -6.3F, -5, 7, 1, 1, 0, BROW);
        add("head", -0.5F, -5.2F, -5.4F, 1, 3, 1, -0.05F, BONE);
        sym("head", -3.25F, -4.1F, -4.95F, 1, 1, 1, 0, BONE);
        add("head", -1, -1.3F, -4.95F, 2, 1, 1, 0, BONE);

        // The hood: tall and squared off, open at the front, with a mantle over his shoulders.
        add("hood", -5, -15.5F, 3, 10, 15, 1, 0, HOOD);
        sym("hood", -5.5F, -15, -4, 1, 14, 7, 0, HOOD);
        add("hood", -5.5F, -16, -5, 11, 1, 9, 0, HOOD);
        add("hood", -5.5F, -16.3F, -5.6F, 11, 2, 1, 0, HOOD);
        sym("hood", -6.5F, -4, -3.5F, 1, 4, 7, 0, HOOD);
        add("hood", -7.5F, -1, -3.5F, 15, 2, 8, 0, MANTLE);

        // The crests: a bar out from the hood, a stalk up from its end curling into a hook, and a second hook swept down.
        sym("crest", -5, -1, -0.5F, 5, 2, 1, 0, CREST);
        int[] upLength = {4, 4, 3, 3, 3, 2};
        int[] upWidth = {2, 2, 2, 2, 1, 1};
        float[] upDelta = {0.05F, 0, -0.05F, -0.12F, 0, -0.18F};
        for (int i = 0; i < CREST_UP; i++) {
            sym("crestUp" + i, -upWidth[i] / 2F, -upLength[i], -0.5F, upWidth[i], upLength[i], 1, upDelta[i], CREST);
        }
        int[] downLength = {4, 3, 3, 2};
        int[] downWidth = {2, 2, 1, 1};
        float[] downDelta = {0, -0.1F, 0, -0.2F};
        for (int i = 0; i < CREST_DOWN; i++) {
            sym("crestDown" + i, -downWidth[i] / 2F, 0, -0.5F, downWidth[i], downLength[i], 1, downDelta[i], CREST);
        }

        // Arms: the shoulder, a long upper arm and forearm, a narrow hand and splinter-long fingers.
        sym("shoulder", -2.4F, -2.2F, -2, 4, 4, 4, 0.1F, COSMOS);
        sym("upper", -1.5F, -1, -1.5F, 3, 11, 3, 0, COSMOS);
        sym("fore", -1.5F, 0, -1.5F, 3, 9, 3, -0.15F, COSMOS);
        sym("fore", -1.5F, 0.5F, -1.5F, 3, 3, 3, 0.05F, COSMOS);
        sym("hand", -1, 0, -1.5F, 2, 3, 3, -0.2F, COSMOS);
        for (int f = 0; f < 4; f++) {
            sym("finger" + f, -0.5F, 0, -0.5F, 1, 3, 1, -0.22F, COSMOS);
            sym("fingerTip" + f, -0.5F, 0, -0.5F, 1, 3, 1, -0.28F, COSMOS);
        }
        sym("thumb", -0.5F, 0, -0.5F, 1, 3, 1, -0.22F, COSMOS);
        sym("thumbTip", -0.5F, 0, -0.5F, 1, 2, 1, -0.28F, COSMOS);

        // Legs, long and lean, the feet pointed.
        sym("thigh", -2, -0.5F, -2, 4, 11, 4, 0, COSMOS);
        sym("thigh", -1.5F, 9.4F, -2.3F, 3, 2, 1, 0, COSMOS);
        sym("shin", -1.5F, 0, -1.5F, 3, 11, 3, -0.1F, COSMOS);
        sym("shin", -1.5F, 1.5F, -0.6F, 3, 4, 3, 0.05F, COSMOS);
        sym("foot", -1, -0.5F, -3.5F, 2, 2, 5, -0.1F, COSMOS);
        sym("foot", -0.5F, 0.2F, -5.4F, 1, 1, 2, -0.1F, COSMOS);

        // The cloak: five widening segments down his back, three at each shoulder that lift with his arms.
        for (int i = 0; i < CAPE; i++) {
            int width = 13 + i;
            add("cape" + i, -width / 2F, 0, 0, width, 6, 1, 0, i == CAPE - 1 ? HEM : CLOAK);
        }
        int[] panelWidth = {6, 7, 7};
        int[] panelLength = {7, 7, 6};
        for (int i = 0; i < PANEL; i++) {
            sym("panel" + i, -panelWidth[i] / 2F, 0, 0, panelWidth[i], panelLength[i], 1, 0, i == PANEL - 1 ? HEM : CLOAK);
        }
        pack();
    }

    private EternityBoxes() {
    }

    /** The boxes of one part. A part with sides is named with "R" or "L" on the end. */
    public static List<Box> of(String part) {
        List<Box> boxes = PARTS.get(part);
        return boxes == null ? Collections.<Box>emptyList() : boxes;
    }

    private static void add(String part, float x, float y, float z, int w, int h, int d, float delta, int material) {
        put(new Box(part, x, y, z, w, h, d, delta, material, 0));
    }

    /** A box on his right side as given, and its mirror on his left. */
    private static void sym(String part, float x, float y, float z, int w, int h, int d, float delta, int material) {
        put(new Box(part + "R", x, y, z, w, h, d, delta, material, -1));
        put(new Box(part + "L", -(x + w), y, z, w, h, d, delta, material, 1));
    }

    private static void put(Box box) {
        PARTS.computeIfAbsent(box.part, k -> new ArrayList<>()).add(box);
        ALL.add(box);
    }

    /** Shelf-packs every box's unwrapped faces onto the skin, tallest first, a pixel apart. */
    private static void pack() {
        List<Box> order = new ArrayList<>(ALL);
        order.sort((a, b) -> b.footprintH() != a.footprintH() ? b.footprintH() - a.footprintH() : b.footprintW() - a.footprintW());
        int x = 0, y = 0, row = 0;
        for (Box box : order) {
            if (x + box.footprintW() > SIZE) {
                x = 0;
                y += row + 1;
                row = 0;
            }
            box.u = x;
            box.v = y;
            x += box.footprintW() + 1;
            row = Math.max(row, box.footprintH());
        }
        if (y + row > SIZE) {
            throw new IllegalStateException("Eternity's boxes do not fit on his skin");
        }
    }
}
