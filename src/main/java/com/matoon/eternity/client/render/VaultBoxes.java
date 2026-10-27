package com.matoon.eternity.client.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Every box of everything in Odin's Vault that is not a block: the Einherjar, the Destroyer, and
 * the treasures on their stands. Each has a sheet of its own, with where every box sits on its
 * texture. Both the models and tools/VaultSkin.java read this, so the textures are always painted
 * for exactly the boxes the models have; texture offsets are packed here, never kept by hand.
 *
 * Boxes are given in their part's own space, the usual model space (y down, the front at -z, the
 * figure's right at -x). Parts with a side have a right and a left version, mirrored.
 *
 * Plain Java with nothing of Minecraft in it, so the skin tool can compile against it.
 */
public final class VaultBoxes {

    // What a box is made of, which decides how the skin tool paints it.
    /** Einherjar: blackened steel plate, gold trim, mail, leather, the ochre cloak, the spear's shaft and blade, the shield, the helm. */
    public static final int STEEL = 0, GOLD = 1, MAIL = 2, LEATHER = 3, CLOAK = 4, SHAFT = 5, BLADE = 6, SHIELD = 7, HELM = 8, VISOR = 9;
    /** The Destroyer: ribbed plate, the head, the furnace inside it, its face plates, spikes, fists and feet; the hollow in
     * its back and the two plates that close over it; the raised bands of its plate, the dark grooves between them, and
     * bands studded with rivets. */
    public static final int RIB = 20, HEAD = 21, CORE = 22, FACE = 23, SPIKE = 24, FIST = 25, FOOT = 26, CROWN = 27, HOLLOW = 28, HATCH = 29,
            BAND = 30, GROOVE = 31, STUD = 32;
    /** The treasures. */
    public static final int STONE_DARK = 40, STONE_CARVED = 41, STONE_FLUTED = 42, STONE_FROST = 43, BRONZE = 44, EMBER = 45,
            GILT = 46, TABLET = 47, ORB = 48, IRON = 49, CASKET = 50, CASKET_CORE = 51, ICE = 52, BRASS = 53, LIGHT = 54,
            SKULL = 55, CROWN_STONE = 56,
            GEM_SPACE = 60, GEM_MIND = 61, GEM_REALITY = 62, GEM_POWER = 63, GEM_TIME = 64, GEM_SOUL = 65;

    public static final class Box {
        public final String part;
        public final float x, y, z, delta;
        public final int w, h, d, material;
        /** Which side it is on: -1 right, 1 left, 0 the middle. */
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

    /** One texture and the boxes painted on it. */
    public static final class Sheet {
        public final String name;
        public final int width, height;
        public final List<Box> all = new ArrayList<>();
        private final Map<String, List<Box>> parts = new LinkedHashMap<>();

        Sheet(String name, int width, int height) {
            this.name = name;
            this.width = width;
            this.height = height;
        }

        /** The boxes of one part. A part with sides is named with "R" or "L" on the end. */
        public List<Box> of(String part) {
            List<Box> boxes = parts.get(part);
            return boxes == null ? Collections.<Box>emptyList() : boxes;
        }

        Sheet add(String part, float x, float y, float z, int w, int h, int d, float delta, int material) {
            return put(new Box(part, x, y, z, w, h, d, delta, material, 0));
        }

        /** A box on the right side as given, and its mirror on the left. */
        Sheet sym(String part, float x, float y, float z, int w, int h, int d, float delta, int material) {
            put(new Box(part + "R", x, y, z, w, h, d, delta, material, -1));
            return put(new Box(part + "L", -(x + w), y, z, w, h, d, delta, material, 1));
        }

        private Sheet put(Box box) {
            parts.computeIfAbsent(box.part, k -> new ArrayList<>()).add(box);
            all.add(box);
            return this;
        }

        /** Shelf-packs every box's unwrapped faces onto the texture, tallest first, a pixel apart. */
        Sheet pack() {
            List<Box> order = new ArrayList<>(all);
            order.sort((a, b) -> b.footprintH() != a.footprintH() ? b.footprintH() - a.footprintH() : b.footprintW() - a.footprintW());
            int x = 0, y = 0, row = 0;
            for (Box box : order) {
                if (x + box.footprintW() > width) {
                    x = 0;
                    y += row + 1;
                    row = 0;
                }
                box.u = x;
                box.v = y;
                x += box.footprintW() + 1;
                row = Math.max(row, box.footprintH());
            }
            if (y + row > height) {
                throw new IllegalStateException(name + "'s boxes do not fit on its texture");
            }
            return this;
        }
    }

    /** Segments in an Einherjar's cloak, top to bottom, and in the ring of his helm's crest. */
    public static final int EINHERJAR_CAPE = 4, CREST_RING = 9;
    /** Segments in the Warlock's Eye's two arches, round each end of the Casket, and in each horn of the Crown. */
    public static final int ARCH_OUTER = 11, ARCH_INNER = 9, CASKET_RING = 12, CROWN_HORN = 5;
    /** How many spikes run down each of the Destroyer's forearms, over each shoulder, and along each trap. */
    public static final int ARM_SPIKES = 5, SHOULDER_SPIKES = 6, TRAP_SPIKES = 6;

    public static final Sheet EINHERJAR = einherjar();
    public static final Sheet DESTROYER = destroyer();
    public static final Sheet RELICS = relics();

    private VaultBoxes() {
    }

    // ------------------------------------------------------------------ the Einherjar

    private static Sheet einherjar() {
        Sheet s = new Sheet("The Einherjar", 128, 128);
        // The hips: mail under a broad belt, and armoured tassets hung front and back that swing as he strides.
        s.add("pelvis", -4, -2, -2, 8, 3, 4, 0, MAIL);
        s.add("pelvis", -4.5F, -2.6F, -2.5F, 9, 2, 5, 0, LEATHER);
        s.add("pelvis", -1, -2.8F, -2.9F, 2, 2, 1, 0, GOLD);
        s.add("tassetF", -3.5F, 0, -0.5F, 7, 6, 1, 0, STEEL);
        s.add("tassetB", -3.5F, 0, -0.5F, 7, 6, 1, 0, STEEL);
        s.sym("tasset", -4.6F, -0.5F, -2, 1, 5, 4, 0, MAIL);

        // The body: a mail shirt, the cuirass over it with the gold chevron, a gorget at the throat.
        s.add("abdomen", -4, -5, -2, 8, 5, 4, 0, MAIL);
        s.add("abdomen", -3.5F, -4.5F, -2.4F, 7, 4, 1, 0, STEEL);
        s.add("chest", -4.5F, -7, -2.5F, 9, 7, 5, 0, STEEL);
        s.add("chest", -4, -6.6F, -3.1F, 8, 5, 1, 0, STEEL);
        s.add("chest", -3, -8.2F, -2.6F, 6, 2, 5, 0, STEEL);
        s.add("chest", -1, -6.2F, -3.5F, 2, 4, 1, -0.1F, GOLD);
        // The cloak's mantle, pinned at the collarbones, and its clasps.
        s.add("chest", -5.5F, -7.8F, 0.6F, 11, 2, 3, 0, CLOAK);
        s.sym("chest", -5, -8, -1.5F, 3, 2, 2, 0, CLOAK);
        s.sym("clasp", -4.1F, -7.6F, -3.2F, 2, 2, 1, -0.1F, GOLD);

        // The helm, with nothing of the face showing: a T of black between the cheek plates, a crest
        // of gold along the crown, and the ring of horns standing above it with a blade through it.
        s.add("head", -4, -8, -4, 8, 8, 8, 0, HELM);
        s.add("head", -3, -6.5F, -4.6F, 6, 6, 1, 0, VISOR);
        s.sym("head", -4.5F, -6, -4.5F, 2, 6, 3, 0, HELM);
        s.add("head", -4.4F, -8.4F, -4.5F, 9, 2, 1, 0, HELM);
        s.add("head", -0.5F, -9.6F, -4.4F, 1, 2, 9, 0, GOLD);
        s.add("head", -4.5F, -1.2F, -2, 9, 2, 6, 0, HELM);
        s.add("crestStem", -1, -2.5F, -1, 2, 3, 2, 0, GOLD);
        s.add("crestBlade", -0.5F, -9, -0.5F, 1, 9, 1, -0.05F, BLADE);
        s.add("crestBlade", -0.5F, -11, -0.5F, 1, 2, 1, -0.25F, BLADE);
        for (int i = 0; i < CREST_RING; i++) {
            s.add("ring" + i, -0.5F, -1.8F, -0.5F, 1, 3, 1, i == 0 || i == CREST_RING - 1 ? 0.05F : 0.12F, GOLD);
        }
        s.sym("horn", -0.5F, -2.5F, -0.5F, 1, 3, 1, -0.05F, GOLD);

        // Arms: mail sleeves, layered pauldrons, armoured vambraces and gauntleted fists.
        s.sym("upper", -2.5F, -1.5F, -2, 4, 6, 4, 0, MAIL);
        s.sym("pauldron", -3.6F, -2.8F, -3, 5, 3, 6, 0, STEEL);
        s.sym("pauldron", -3.9F, -1.2F, -3.2F, 4, 2, 6, 0.2F, STEEL);
        s.sym("pauldron", -4.1F, -3.2F, -3.1F, 1, 4, 6, 0, GOLD);
        s.sym("fore", -2, 0, -2, 4, 6, 4, 0, MAIL);
        s.sym("fore", -2.3F, 0.8F, -2.3F, 4, 4, 4, 0.3F, STEEL);
        s.sym("fore", -1.5F, 5.4F, -2, 3, 2, 4, 0.1F, LEATHER);

        // Legs: mail, armoured shins, gold at the knee, heavy boots.
        s.sym("thigh", -2, 0, -2, 4, 6, 4, 0, MAIL);
        s.sym("thigh", -2.2F, 0.5F, -2.6F, 4, 4, 1, 0, STEEL);
        s.sym("shin", -2, 0, -2, 4, 6, 4, 0, LEATHER);
        s.sym("shin", -2.3F, 0.4F, -2.7F, 4, 4, 1, 0.1F, STEEL);
        s.sym("shin", -2, -1, -2.9F, 4, 2, 1, 0, GOLD);
        s.sym("shin", -2.3F, 4, -3, 4, 2, 5, 0.1F, LEATHER);

        // The spear: a long dark shaft bound in gold, a leaf blade, a spike at its butt.
        s.add("spear", -0.5F, -24, -0.5F, 1, 40, 1, -0.12F, SHAFT);
        s.add("spear", -0.5F, -18, -0.5F, 1, 2, 1, 0.05F, GOLD);
        s.add("spear", -0.5F, 4, -0.5F, 1, 2, 1, 0.05F, GOLD);
        s.add("spear", -1.5F, -25, -0.5F, 3, 1, 1, 0, GOLD);
        s.add("spear", -1, -31, -0.5F, 2, 6, 1, -0.05F, BLADE);
        s.add("spear", -0.5F, -34, -0.5F, 1, 3, 1, -0.15F, BLADE);
        s.add("spear", -0.5F, 16, -0.5F, 1, 3, 1, -0.2F, GOLD);

        // The round shield, embossed bronze with a boss: three plates crossed into a disc.
        s.add("shield", 0, -6, -4, 1, 12, 8, 0, SHIELD);
        s.add("shield", 0, -4, -6, 1, 8, 12, 0, SHIELD);
        s.add("shield", 0, -5, -5, 1, 10, 10, 0, SHIELD);
        s.add("shield", 0.6F, -1.5F, -1.5F, 1, 3, 3, 0, GOLD);
        s.add("shield", -1, -2, -0.5F, 1, 4, 1, 0, LEATHER);

        // The cloak, hanging from the mantle, each segment a little wider than the last.
        for (int i = 0; i < EINHERJAR_CAPE; i++) {
            int width = 11 + i;
            s.add("cape" + i, -width / 2F, 0, 0, width, 6, 1, 0, CLOAK);
        }
        return s.pack();
    }

    // ------------------------------------------------------------------ the Destroyer

    private static Sheet destroyer() {
        Sheet s = new Sheet("The Destroyer", 256, 128);
        // Built as the film has it: every surface banded, and every band a raised ring of plate over a dark
        // groove, so the bands stand out in relief. A narrow helm under a broad folded crown, almost no neck,
        // great sloping traps running down into rounded shoulders, a ridge of spikes along them that carries
        // on down the outside of the arms, riveted bands, massive pecs split down the middle, a narrower waist.

        // The hips: banded, a plate over the groin.
        s.add("pelvis", -5, -1, -3, 10, 4, 6, 0, GROOVE);
        s.add("pelvis", -5.5F, -1, -3.5F, 11, 2, 7, 0, BAND);
        s.add("pelvis", -6, 1.3F, -3.6F, 12, 2, 7, 0, BAND);
        s.add("pelvis", -1.5F, -0.2F, -4.3F, 3, 3, 1, 0, BAND);

        // The waist: three bands to each side of a seam down the middle, narrowing toward the hips.
        s.add("abdomen", -4, -6.5F, -2.8F, 8, 7, 7, 0, GROOVE);
        for (int i = 0; i < 3; i++) {
            s.sym("abdomen", -4.7F + 0.25F * i, -6.4F + 2.15F * i, -3.4F + 0.1F * i, 5, 2, 6, 0, BAND);
        }

        // The chest: a dark core; on it the pecs, each four bands bulging forward, split down the middle; the
        // yoke under the neck; the lats under the arms. Its back is a hollow big enough to stand in, framed in
        // plate, with a spine down the middle and the two plates that close over it hinged at its shoulder blades.
        // The middle of the core and the inner ends of the pecs are its plug: whole on the vault's Destroyer, taken out
        // of the Destroyer Armor once its cradle is forged, opening its cavity (below).
        s.sym("chest", -6.5F, -10, -3.5F, 4, 10, 6, 0, GROOVE);
        s.add("chest", -2.5F, -10, -3.5F, 5, 2, 6, 0, GROOVE);
        s.add("chest", -2.5F, -2, -3.5F, 5, 2, 6, 0, GROOVE);
        s.add("plug", -2.5F, -8, -3.5F, 5, 6, 6, 0, GROOVE);
        float[] bulge = {0.2F, 0F, 0.15F, 0.7F};
        for (int i = 0; i < 4; i++) {
            int inner = 2, outer = (i == 3 ? 5 : 6) - inner;
            float x = -6.7F + 0.2F * i;
            s.sym("chest", x, -9.6F + 2.2F * i, -6F + bulge[i], outer, 2, 5, 0, BAND);
            s.sym("plug", x + outer, -9.6F + 2.2F * i, -6F + bulge[i], inner, 2, 5, 0, BAND);
        }
        // The cavity in the Destroyer Armor's chest: a deep frame of riveted plate through to a ribbed wall the furnace
        // shows through, for the Astral Regulator to hang in, the stones' sockets arched over its top.
        s.add("cavity", -3.5F, -9, -6.4F, 1, 8, 3, 0, STUD);
        s.add("cavity", 2.5F, -9, -6.4F, 1, 8, 3, 0, STUD);
        s.add("cavity", -2.5F, -9, -6.4F, 5, 1, 3, 0, STUD);
        s.add("cavity", -2.5F, -2, -6.4F, 5, 1, 3, 0, STUD);
        s.add("cavity", -2.5F, -8, -1, 5, 6, 1, 0, HOLLOW);
        s.add("cavity", -2.5F, -8, 0, 5, 6, 2, 0, GROOVE);
        s.add("chest", -5, -11.4F, -3.4F, 10, 2, 5, 0, BAND);
        s.sym("chest", -8.2F, -9.4F, -3, 2, 7, 6, 0, BAND);
        s.add("chest", -5, -9, 2, 10, 7, 1, 0, HOLLOW);
        s.sym("chest", -8, -10, 2, 3, 10, 3, 0, BAND);
        s.sym("chest", -8.6F, -6.5F, 2.4F, 3, 3, 3, 0, BAND);
        s.add("chest", -5, -2, 2, 10, 2, 3, 0, BAND);
        s.add("chest", -0.5F, -9.5F, 2.5F, 1, 8, 2, 0.1F, RIB);
        s.sym("back", 0, -4, -2, 5, 8, 2, 0, HATCH);

        // The traps: great sloping banks of plate from the head down to the shoulders, a row of spikes along the top.
        s.sym("trap", -9, -1, -4, 9, 4, 8, 0, BAND);
        s.sym("trap", -7.5F, -2.2F, -3.4F, 7, 2, 7, 0, BAND);
        for (int i = 0; i < TRAP_SPIKES; i++) {
            spike(s, "trap", -6.5F + i * 1.2F, -2.2F, i % 2 == 0 ? -1.2F : 1.2F, UP);
        }

        // The neck, banded, all but hidden between the traps.
        s.add("neck", -2, -3, -2, 4, 3, 4, 0, GROOVE);
        s.add("neck", -2.5F, -3, -2.5F, 5, 1, 5, 0, BAND);
        s.add("neck", -2.5F, -1.5F, -2.5F, 5, 1, 5, 0, BAND);

        // The helm: narrow, its sides and back banded; the furnace inside its face hidden behind two slatted plates
        // that meet in a prow behind a raised ridge, and swing open; a broad folded crown over it all, two
        // squared plates folded down at the front and sides, tilted up from a valley down its middle so the deep front
        // face reads as a notched V; barely wider than the helm.
        s.add("head", -3, -9, -2, 6, 9, 5, 0, HEAD);
        s.sym("head", -3.6F, -9, -3.6F, 1, 7, 6, 0, HEAD);
        s.sym("head", -3.3F, -2, -3.2F, 1, 2, 5, -0.1F, HEAD);
        s.add("head", -2, -0.8F, -3.8F, 4, 1, 2, 0, HEAD);
        s.add("head", -0.5F, -9.6F, -4.5F, 1, 10, 1, 0, HEAD);
        s.add("head", -0.5F, -10.2F, -4.9F, 1, 2, 8, 0, CROWN);
        s.add("core", -2.5F, -9.5F, -2.6F, 5, 8, 1, 0, CORE);
        s.add("core", -2, -8.5F, -3.1F, 4, 6, 1, -0.2F, CORE);
        s.add("faceR", 0, -9, -1, 3, 9, 1, 0, FACE);
        s.add("faceL", -3, -9, -1, 3, 9, 1, 0, FACE);
        s.sym("crown", -5, -2, -3.8F, 5, 2, 7, 0, CROWN);
        s.sym("crown", -5, -2, -4.8F, 5, 4, 1, 0, CROWN);
        s.sym("crown", -5.2F, -2, -3.8F, 1, 3, 7, 0, CROWN);

        // Shoulders: rounded, banded and riveted, spikes along their tops and out over their sides.
        s.sym("shoulder", -4, -3, -4, 6, 7, 8, 0, GROOVE);
        s.sym("shoulder", -3.5F, -3.8F, -3, 5, 1, 6, 0, BAND);
        s.sym("shoulder", -4.5F, -2.6F, -4.5F, 7, 2, 9, 0, STUD);
        s.sym("shoulder", -4.8F, -0.1F, -4.7F, 7, 2, 9, 0, STUD);
        s.sym("shoulder", -4.4F, 2.4F, -3.8F, 6, 2, 8, 0, STUD);
        for (int i = 0; i < SHOULDER_SPIKES; i++) {
            if (i < 3) {
                spike(s, "shoulder", -2.9F + i * 1.5F, -3.8F, 0, UP);
            } else {
                spike(s, "shoulder", -4.8F, -2.1F + (i - 3) * 1.9F, 0, OUT);
            }
        }

        // The arms: banded rings of riveted plate over dark grooves, the forearms massive, spikes down the outside.
        s.sym("upper", -2.5F, 0, -2.5F, 5, 8, 5, 0, GROOVE);
        s.sym("fore", -3, 0, -3, 6, 8, 6, 0, GROOVE);
        for (int i = 0; i < 3; i++) {
            s.sym("upper", -3, 0.4F + i * 2.5F, -3, 6, 2, 6, 0, STUD);
            s.sym("fore", -3.5F, 0.4F + i * 2.5F, -3.5F, 7, 2, 7, 0, STUD);
        }
        for (int i = 0; i < ARM_SPIKES; i++) {
            if (i < 4) {
                spike(s, "upper", -3, 1.4F + i * 1.9F, 0, OUT);
            }
            spike(s, "fore", -3.5F, 1.1F + i * 1.6F, 0, OUT);
            if (i < 3) {
                spike(s, "fore", 0, 1.5F + i * 2.4F, 3.5F, BACK);
            }
        }
        s.sym("fist", -3, 0, -3, 6, 5, 6, 0.1F, FIST);
        s.sym("fist", -3, 0.4F, -3.7F, 6, 2, 1, 0, FIST);
        s.sym("fist", -3.4F, 0.6F, -3.2F, 2, 3, 3, 0, FIST);
        s.sym("fist", -2.6F, 3.4F, -3.4F, 5, 1, 1, 0, FIST);

        // Legs: thick banded thighs and shins, a cap over the knee, heavy plated feet.
        s.sym("thigh", -3, 0, -3.2F, 6, 7, 7, 0, GROOVE);
        for (int i = 0; i < 3; i++) {
            s.sym("thigh", -3.5F, 0.3F + i * 2.3F, -3.7F, 7, 2, 8, 0, BAND);
        }
        s.sym("shin", -2.5F, 0, -2.5F, 5, 4, 5, 0, GROOVE);
        s.sym("shin", -3, 0.3F, -3, 6, 2, 6, 0, BAND);
        s.sym("shin", -2, -1.3F, -3.9F, 4, 2, 1, 0, BAND);
        s.sym("shin", -3.2F, 2.4F, -3.2F, 6, 2, 6, 0, BAND);
        s.sym("shin", -3.5F, 4, -5, 7, 2, 8, 0, FOOT);
        s.sym("shin", -3, 3.2F, -5.6F, 6, 2, 2, 0, FOOT);
        return s.pack();
    }

    private static final int UP = 0, OUT = 1, BACK = 2;

    /**
     * One of its spikes, on the right side and mirrored on the left: a short cone standing off the surface
     * at (x, y, z), its broad base sunk into the plate and its point a step further out, up, outward or back.
     */
    private static void spike(Sheet s, String part, float x, float y, float z, int way) {
        float dx = way == OUT ? -1 : 0, dy = way == UP ? -1 : 0, dz = way == BACK ? 1 : 0;
        s.sym(part, x - 0.5F + dx * 0.45F, y - 0.5F + dy * 0.45F, z - 0.5F + dz * 0.45F, 1, 1, 1, 0.05F, SPIKE);
        s.sym(part, x - 0.5F + dx * 1.2F, y - 0.5F + dy * 1.2F, z - 0.5F + dz * 1.2F, 1, 1, 1, -0.28F, SPIKE);
    }

    // ------------------------------------------------------------------ the treasures

    private static Sheet relics() {
        Sheet s = new Sheet("The vault's treasures", 256, 256);

        // A stand of black stone with a gold cradle: the fake gauntlet, upright, back of the hand out.
        s.add("gStand", -5, -2, -5, 10, 2, 10, 0, STONE_DARK);
        s.add("gStand", -2, -8, -2, 4, 6, 4, 0, STONE_DARK);
        s.add("gStand", -3.5F, -9, -3, 7, 1, 6, 0, GILT);
        s.add("gCuff", -4, -6, -3, 8, 6, 6, 0, GILT);
        s.add("gCuff", -4.5F, -2, -3.5F, 9, 2, 7, 0, GILT);
        s.add("gCuff", -3.5F, -7, -2.5F, 7, 1, 5, 0, GILT);
        s.add("gHand", -4, -6, -2, 8, 6, 4, 0, GILT);
        s.add("gHand", -3.5F, -4.6F, -2.4F, 7, 3, 1, 0, GILT);
        int[] fingerLength = {5, 6, 6, 4};
        for (int i = 0; i < 4; i++) {
            s.add("gFinger" + i, -1, -3, -1, 2, 3, 2, -0.02F, GILT);
            s.add("gTip" + i, -1, -fingerLength[i] + 3, -1, 2, fingerLength[i] - 3, 2, -0.08F, GILT);
        }
        s.add("gThumb", -1, -3, -1, 2, 3, 2, 0, GILT);
        s.add("gThumbTip", -1, -2, -1, 2, 2, 2, -0.1F, GILT);
        s.add("gGemSoul", -0.5F, -0.5F, -0.6F, 1, 1, 1, 0.05F, GEM_SOUL);
        s.add("gGemCentre", -1.5F, -4.5F, -2.9F, 3, 3, 1, 0, GEM_MIND);
        s.add("gGemSpace", -0.5F, -0.5F, -0.5F, 1, 1, 1, 0.1F, GEM_SPACE);
        s.add("gGemReality", -0.5F, -0.5F, -0.5F, 1, 1, 1, 0.1F, GEM_REALITY);
        s.add("gGemPower", -0.5F, -0.5F, -0.5F, 1, 1, 1, 0.1F, GEM_POWER);
        s.add("gGemTime", -0.5F, -0.5F, -0.5F, 1, 1, 1, 0.1F, GEM_TIME);

        // The Pillar of Privaldi: fluted stone on a carved base, a carved capital; on it the Chalice
        // of Muspelheim, a wide bronze bowl on a short stem, prongs at its rim, embers in it.
        s.add("fPillar", -6, -3, -6, 12, 3, 12, 0, STONE_CARVED);
        s.add("fPillar", -4.5F, -21, -4.5F, 9, 18, 9, 0, STONE_FLUTED);
        s.add("fPillar", -5.5F, -24, -5.5F, 11, 3, 11, 0, STONE_CARVED);
        s.add("fPillar", -5, -4.5F, -5, 10, 2, 10, 0, STONE_CARVED);
        s.add("fChalice", -2, -4, -2, 4, 4, 4, 0, BRONZE);
        s.add("fChalice", -3.5F, -5.5F, -3.5F, 7, 2, 7, 0, BRONZE);
        s.add("fChalice", -5, -7, -5, 10, 2, 10, 0, BRONZE);
        s.add("fChalice", -6.5F, -11, -6.5F, 13, 4, 1, 0, BRONZE);
        s.add("fChalice", -6.5F, -11, 5.5F, 13, 4, 1, 0, BRONZE);
        s.add("fChalice", -6.5F, -11, -5.5F, 1, 4, 11, 0, BRONZE);
        s.add("fChalice", 5.5F, -11, -5.5F, 1, 4, 11, 0, BRONZE);
        s.add("fChalice", -7.5F, -12, -7.5F, 15, 1, 2, 0, BRONZE);
        s.add("fChalice", -7.5F, -12, 5.5F, 15, 1, 2, 0, BRONZE);
        s.add("fChalice", -7.5F, -12, -5.5F, 2, 1, 11, 0, BRONZE);
        s.add("fChalice", 5.5F, -12, -5.5F, 2, 1, 11, 0, BRONZE);
        s.add("fEmbers", -5.5F, -9, -5.5F, 11, 1, 11, 0, EMBER);
        for (int i = 0; i < 4; i++) {
            s.add("fProng" + i, -0.5F, -5, -0.5F, 1, 5, 1, -0.1F, BRONZE);
        }

        // The Tuning Fork: a tall gilt fork with runes down its tines, on a stand of black stone.
        s.add("tStand", -4, -2, -4, 8, 2, 8, 0, STONE_DARK);
        s.add("tStand", -2.5F, -4, -2.5F, 5, 2, 5, 0, STONE_CARVED);
        s.add("tFork", -1.5F, -7.5F, -1.5F, 3, 3, 3, 0, GILT);
        s.add("tFork", -1, -16, -1, 2, 9, 2, 0, GILT);
        s.add("tFork", -2, -17, -1.5F, 4, 1, 3, 0, GILT);
        s.add("tFork", -4.5F, -19, -1, 9, 2, 2, 0, GILT);
        s.sym("tTine", -1, -16, -1, 2, 16, 2, -0.05F, GILT);
        s.sym("tTine", -1, -17.5F, -1, 2, 2, 2, -0.3F, GILT);

        // The Tablet of Life and Time: an upright slab, arched, gold-clamped, carved with rings and runes.
        s.add("lStand", -6.5F, -3, -3, 13, 3, 6, 0, STONE_DARK);
        s.add("lStand", -5.5F, -4, -2, 11, 1, 4, 0, GILT);
        s.add("lSlab", -5, -16, -1, 10, 16, 2, 0, TABLET);
        s.add("lSlab", -4, -17, -1, 8, 1, 2, 0, TABLET);
        s.add("lSlab", -2.5F, -18, -1, 5, 1, 2, 0, TABLET);
        s.sym("lSlab", -5.6F, -12, -1.5F, 1, 7, 3, 0, GILT);
        s.add("lSlab", -2, -10, -1.4F, 4, 4, 1, -0.1F, TABLET);

        // The Warlock's Eye: an eye, living and wet, floating in a frame of dark bronze shaped like an eye
        // itself: a carved mask beneath it, the arch of a lid over it in two bands, rune-cut tabs at the
        // ends of the outer one. The frame stands on a block of dark stone lit along its top.
        s.add("ePlinth", -14, -10, -6, 28, 10, 12, 0, STONE_DARK);
        s.add("ePlinth", -15, -11, -6.5F, 30, 1, 13, 0, STONE_CARVED);
        s.add("ePlinth", -13, -11.2F, -5, 26, 1, 10, 0, LIGHT);
        for (int c = -14; c < 14; c += 2) {
            float x = Math.abs(c + 1F);
            float top = 3.4F - 4.2F * (x / 14F) * (x / 14F);
            float bottom = x <= 2F ? 7.5F : x <= 10F ? 10.5F : 10.5F - (x - 10F) * 1.4F;
            s.add("eMask", c, top, -1.5F, 2, Math.max(1, Math.round(bottom - top)), 3, 0, BRASS);
        }
        s.add("eMask", -10, 1.8F, -1.9F, 20, 1, 1, 0, BRASS);
        s.add("eArchOuter", -2, -0.6F, -1, 4, 1, 2, 0.15F, BRASS);
        s.add("eArchInner", -1.5F, -0.5F, -0.5F, 3, 1, 1, 0.1F, BRASS);
        s.sym("eTab", -7, -2, -1, 7, 4, 2, 0, BRASS);
        s.add("eOrb", -3, -3, -3, 6, 6, 6, 0, ORB);
        s.add("eOrb", -2, -4, -2, 4, 8, 4, 0, ORB);
        s.add("eOrb", -4, -2, -2, 8, 4, 4, 0, ORB);
        s.add("eOrb", -2, -2, -4, 4, 4, 8, 0, ORB);

        // The Crown of Black Fire: Surtur's crown, a horned skull of black stone. Two broad blades carved
        // with whorls spread up and out from its brow in a great V, ragged along their undersides, and
        // from their ends two long horns sweep up. It rests on a low slab.
        s.add("cStand", -14, -2, -7, 28, 2, 14, 0, STONE_DARK);
        s.add("cSkull", -4.5F, -5, -4.5F, 9, 5, 8, 0, SKULL);
        s.add("cSkull", -6.5F, -12, -5, 13, 7, 9, 0, SKULL);
        s.add("cSkull", -6, -17, -4, 12, 5, 9, 0, SKULL);
        s.add("cSkull", -4.5F, -19, -3, 9, 2, 7, 0, SKULL);
        s.add("cSkull", -0.5F, -20, -4, 1, 3, 8, 0, SKULL);
        s.add("cSkull", -0.5F, -13.5F, -5.6F, 1, 4, 1, 0, SKULL);
        s.sym("cSkull", -9, -10, -3, 3, 2, 4, 0, SKULL);
        s.sym("cWing", -22, -2, -2, 22, 4, 4, 0, CROWN_STONE);
        for (int i = 0; i < 4; i++) {
            s.sym("cWing", -20 + i * 5, 2, -1.5F, 3, 2, 3, -0.1F, CROWN_STONE);
        }
        int[] hornSize = {4, 3, 3, 2, 1};
        int[] hornLength = {7, 7, 6, 6, 5};
        for (int i = 0; i < CROWN_HORN; i++) {
            float half = hornSize[i] / 2F;
            s.sym("cHorn" + i, -half, -hornLength[i], -half, hornSize[i], hornLength[i], hornSize[i], i == CROWN_HORN - 1 ? -0.1F : 0.05F, CROWN_STONE);
        }

        // The Pedestal of Fimbulvetr: frosted stone carved with runes, and the casket's cradle on top.
        s.add("pPedestal", -6, -2, -6, 12, 2, 12, 0, STONE_CARVED);
        s.add("pPedestal", -4, -14, -4, 8, 12, 8, 0, STONE_FROST);
        s.sym("pPedestal", -5, -13, -5, 1, 10, 1, 0, STONE_CARVED);
        s.sym("pPedestal", -5, -13, 4, 1, 10, 1, 0, STONE_CARVED);
        s.add("pPedestal", -9.5F, -16, -6.5F, 19, 2, 13, 0, STONE_CARVED);
        s.add("pPedestal", -8.5F, -17, -5.5F, 17, 1, 11, 0, ICE);

        // The Casket of Ancient Winters: panels of frosted blue glass veined with dark knotwork, lit from
        // within, in a frame of dark stone. Its corner posts flare to points top and bottom, the front and
        // back panels pinch in at their sides, and each end is ringed by a pointed oval. A crest at the top
        // of the front and back.
        s.add("casket", -8, -12, -5, 16, 11, 10, 0, CASKET_CORE);
        s.add("casket", -8.5F, -1.2F, -5.5F, 17, 1, 11, 0, CASKET);
        s.add("casket", -8.5F, -12.8F, -5.5F, 17, 1, 1, 0, CASKET);
        s.add("casket", -8.5F, -12.8F, 4.5F, 17, 1, 1, 0, CASKET);
        s.sym("casket", -8.6F, -12.8F, -5, 1, 1, 10, 0, CASKET);
        s.add("kPost", -1.5F, -12, -1.5F, 3, 12, 3, 0, CASKET);
        s.add("kPost", -2, -13.5F, -2, 4, 1, 4, 0, CASKET);
        s.add("kPost", -1, -14.5F, -1, 2, 1, 2, -0.15F, CASKET);
        s.add("kPost", -2, -0.6F, -2, 4, 1, 4, 0, CASKET);
        s.add("kBulge", -1, -9, -0.5F, 2, 6, 1, 0, CASKET);
        s.add("kBulge", -0.5F, -10.5F, -0.5F, 1, 9, 1, 0, CASKET);
        s.add("kCrest", -2, -1, -0.5F, 4, 1, 1, 0, CASKET);
        s.add("kCrest", -1, -2, -0.5F, 2, 1, 1, -0.05F, CASKET);
        s.add("kRing", -0.5F, -1.5F, -0.5F, 1, 3, 1, 0.05F, CASKET);

        // The stands Mjolnir and the Tesseract rest on: black stone with carved bands, a gold cradle for the cube.
        s.add("plinth", -7, -2, -7, 14, 2, 14, 0, STONE_CARVED);
        s.add("plinth", -5.5F, -14, -5.5F, 11, 12, 11, 0, STONE_DARK);
        s.add("plinth", -6.5F, -16, -6.5F, 13, 2, 13, 0, STONE_CARVED);
        s.add("cradle", -4, -17, -4, 8, 1, 8, 0, GILT);
        for (int i = 0; i < 4; i++) {
            s.add("cradleProng" + i, -0.5F, -4, -0.5F, 1, 4, 1, -0.1F, GILT);
        }
        return s.pack();
    }
}
