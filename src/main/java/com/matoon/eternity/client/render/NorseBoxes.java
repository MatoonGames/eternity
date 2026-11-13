package com.matoon.eternity.client.render;

import com.matoon.eternity.client.render.VaultBoxes.Sheet;

/**
 * Every box of the people of the first chapter: the stranger in her mourning, what is under it,
 * Ragna, Hrafn, and the dead under the mound. They share one skeleton and differ in everything
 * that hangs off it.
 * <p>
 * The skeleton: hips, a waist and a chest that bend and twist, a neck, a head with a nose, lids
 * that close and a mouth that moves; shoulders, elbows, wrists, a hand with its fingers and a
 * thumb that close; hips, knees, ankles and feet. What hangs off it swings on springs: a long
 * skirt in six panels of two pieces each, each pushed out by the leg behind it; a cloak or a
 * mantle in three; a braid, the hair down a back, a beard; the long sleeves of a robe. Death's
 * hood stands off her skull all round, deep enough to be dark inside. The dead have a jaw that
 * hangs open.
 * <p>
 * Each look has a sheet of its own, laid out exactly as {@link VaultBoxes} lays out the vault's,
 * so the model and tools/NorseSkin.java always agree on where everything is painted. Boxes are
 * in their part's own space: y down, the front at -z, the figure's right at -x.
 * <p>
 * Plain Java with nothing of Minecraft in it, so the skin tool can compile against it.
 */
public final class NorseBoxes {

    // What a box is made of, which decides how the skin tool paints it.
    public static final int SKIN = 0, FACE = 1, HAIR = 2, DRESS = 3, APRON = 4, BROOCH = 5, BEADS = 6, TUNIC = 7, TROUSERS = 8,
            WRAPS = 9, SHOE = 10, BELT = 11, CLOAK = 12, PIN = 13, BEARD = 14, ROBE = 15, HOOD = 16, BONE = 17, NOSE = 18,
            MAIL = 19, HELM = 20, SHROUD = 21, HAFT = 22, AXE_HEAD = 23, SLEEVE = 24, KERCHIEF = 25, LID = 26, MOUTH = 27,
            HAND = 28, MANTLE = 29, HAIRPIN = 30, TEETH = 31, RIBS = 32, KNIFE = 33, LEAVES = 34, BLADE = 35, GEM = 36, SILK = 37,
            REGALIA = 38, FLESH = 39, GOWN = 40, GLOVE = 41, JEWEL = 42, TOP = 43, SHORTS = 44, NET = 45, BOOT = 46, CUFF = 47, CLIP = 48,
            EYEBALL = 49, GUT = 50, FLAP = 51, JAWBONE = 52, SUIT = 53, STOCKING = 54, RIBBON = 55, CAPE = 56,
            NECKLACE = 57, BAND = 58, SASH = 59, SKULL = 60, SOCKET = 61;

    /** The looks, in the order the entity syncs them. */
    public static final int STRANGER = 0, DEATH = 1, RAGNA = 2, HRAFN = 3, DRAUGR = 4, GARDENER = 5, AYESHA = 6, MAGUS = 7, SPAWN = 8, TEMPTRESS = 9, UNVEILED = 10;

    /** Segments in a cloak, a braid or the hair down a back, a beard. */
    public static final int CLOAK_SEGMENTS = 3, BRAID = 3, BEARD_SEGMENTS = 2;
    /**
     * Her cape: pleated, in strips hinged edge to edge out from the middle of her back to each side,
     * each strip in lengths to the ground; and from each wrist, a fall of it in lengths, which the
     * model stretches to the ground. Every piece of it is a sheet with no thickness.
     */
    public static final int CAPE_PLEATS = 4, CAPE_SEGMENTS = 4, CAPE_LENGTH = 7, DRAPES = 3, DRAPE_LENGTH = 6;
    /** How wide each pleat is, from the middle of her back outward. */
    public static final int[] PLEAT_WIDTHS = {2, 2, 2, 3};
    /**
     * Which way each pleat is turned, outward from the middle, on her right (her left is the mirror):
     * back, forward, back, then the last wrapped forward round her side. Positive turns its outer edge
     * back, away from her.
     */
    public static final float[] PLEAT_TURNS = {0.35F, -0.35F, 0.1F, -1.3F};
    /** The skirt's panels: front right, front left, back right, back left, right side, left side. */
    public static final String[] PANELS = {"FR", "FL", "BR", "BL", "SR", "SL"};

    public static final Sheet[] SHEETS = {stranger(), death(), ragna(), hrafn(), draugr(), gardener(), ayesha(), magus(), spawn(), temptress(), unveiled()};

    private NorseBoxes() {
    }

    // ------------------------------------------------------------------ the skeleton

    /**
     * Head, body, limbs, hands and feet. Slender is the women's and hers: a narrow waist, slim
     * arms; broad is a man's. Gaunt is the dead's: bones under skin.
     */
    private static void body(Sheet s, boolean slender, boolean gaunt, int torso, int sleeve, int legs, int feet, int hands) {
        int w = slender || gaunt ? 3 : 4;
        float half = w / 2F;
        // Hips, waist and chest; the women narrower at the waist.
        s.add("pelvis", -4, -2, -2, 8, 3, 4, 0, torso);
        s.add("abdomen", slender ? -3.5F : -4, -5, -2, slender ? 7 : 8, 5, 4, 0, torso);
        s.add("chest", -4, -7, -2, 8, 7, 4, 0, torso);
        // The head, its neck, and the nose that stands off its face.
        s.add("head", -4, -8, -4, 8, 8, 8, 0, FACE);
        s.add("head", -1.5F, -0.5F, -1.5F, 3, 2, 3, 0, SKIN);
        s.add("nose", -0.5F, 0, -1, 1, 2, 1, gaunt ? -0.2F : -0.12F, NOSE);
        // Lids over the eyes and a mouth over the lips, flat on the face, shown when they blink or speak.
        s.add("lid", -3, 0, 0, 2, 1, 0, 0, LID);
        s.add("lid", 1, 0, 0, 2, 1, 0, 0, LID);
        s.add("mouth", -1, 0, 0, 2, 1, 0, 0, MOUTH);
        // Arms: the upper arm in the sleeve, the forearm, a hand, its fingers and a thumb.
        s.sym("upper", -half, -1.5F, -half, w, 6, w, 0, sleeve);
        s.sym("fore", -half, 0, -half, w, 4, w, -0.05F, sleeve == SKIN ? SKIN : sleeve);
        s.sym("hand", -half + 0.25F, 0, -1, w, 1, 2, -0.1F, hands);
        s.sym("fingers", -half + 0.25F, 0, -0.5F, w, 2, 1, -0.08F, hands);
        s.sym("thumb", -0.5F, 0, -0.5F, 1, 1, 1, -0.1F, hands);
        // Legs: thighs, shins, feet.
        s.sym("thigh", -2, 0, -2, 4, 6, 4, 0, legs);
        s.sym("shin", -2, 0, -2, 4, 5, 4, -0.02F, legs);
        s.sym("foot", -2, 0, -3, 4, 1, 5, 0, feet);
    }

    /** A long skirt to the ankle: six panels, each two pieces, the lower a little wider. */
    private static void skirt(Sheet s, int upper, int lower, float flare, int material) {
        for (String p : PANELS) {
            boolean side = p.charAt(0) == 'S';
            int w = side ? 4 : 5;
            if (side) {
                s.add("skirt" + p, -0.5F, 0, -w / 2F, 1, upper, w, flare, material);
                s.add("skirt" + p + "2", -0.5F, 0, -(w + 1) / 2F, 1, lower, w + 1, flare, material);
            } else {
                s.add("skirt" + p, -w / 2F, 0, -0.5F, w, upper, 1, flare, material);
                s.add("skirt" + p + "2", -(w + 1) / 2F, 0, -0.5F, w + 1, lower, 1, flare, material);
            }
        }
    }

    /** A short hem to the knee: four panels, one piece each. */
    private static void hem(Sheet s, int length, float flare, int material) {
        for (int i = 0; i < 4; i++) {
            String p = PANELS[i];
            s.add("skirt" + p, -2.5F, 0, -0.5F, 5, length, 1, flare, material);
        }
        s.add("skirtSR", -0.5F, 0, -2, 1, length - 1, 4, flare, material);
        s.add("skirtSL", -0.5F, 0, -2, 1, length - 1, 4, flare, material);
    }

    private static void cloak(Sheet s, int width, int length, int material) {
        for (int i = 0; i < CLOAK_SEGMENTS; i++) {
            int w = width + i;
            s.add("cloak" + i, -w / 2F, 0, -0.5F, w, i == CLOAK_SEGMENTS - 1 ? length - 1 : length, 1, 0, material);
        }
    }

    // ------------------------------------------------------------------ the looks

    /**
     * The stranger, as the village sees her: black wool to the ground; a charcoal apron-dress over
     * it, front and back, held at the collarbones by two oval silver brooches with a string of jet
     * between them; a black wool mantle over her shoulders; her dark hair smooth over the crown and
     * bound in a knot at the nape with a silver pin through it, a strand loose at each temple.
     * Sleeves to the wrist.
     */
    private static Sheet stranger() {
        Sheet s = new Sheet("The stranger", 128, 128);
        body(s, true, false, DRESS, DRESS, DRESS, SHOE, HAND);
        skirt(s, 6, 6, 0.05F, DRESS);
        // The apron-dress, over the gown in front.
        s.add("chest", -3.6F, -6.2F, -2.45F, 7, 7, 1, 0, APRON);
        s.add("abdomen", -3.2F, -5, -2.45F, 6, 5, 1, 0, APRON);
        s.add("apron", -3, 0, -0.5F, 6, 6, 1, 0, APRON);
        s.add("apron2", -3, 0, -0.5F, 6, 4, 1, 0, APRON);
        s.sym("brooch", -1, -1, -0.6F, 2, 2, 1, -0.05F, BROOCH);
        s.add("beads", -2.5F, 0, -0.5F, 5, 1, 1, -0.3F, BEADS);
        // Her hair: smooth over the crown, the knot at the nape, the pin through it, two loose strands.
        s.add("head", -4, -8, -4, 8, 2, 8, 0.22F, HAIR);
        s.add("head", -4.25F, -6.2F, -4.2F, 1, 4, 4, 0, HAIR);
        s.add("head", 3.25F, -6.2F, -4.2F, 1, 4, 4, 0, HAIR);
        s.add("head", -4, -6.2F, 2.6F, 8, 5, 2, 0.24F, HAIR);
        s.add("bun", -1.5F, -1.5F, 0, 3, 3, 2, 0.2F, HAIR);
        s.add("bun", -2.5F, -0.5F, 0.6F, 5, 1, 1, -0.1F, HAIRPIN);
        s.sym("lock", -0.5F, 0, -0.5F, 1, 4, 1, -0.05F, HAIR);
        // The mantle, over the shoulders and down her back to the knee.
        s.add("chest", -4.5F, -7.5F, -2.3F, 9, 2, 5, 0.15F, MANTLE);
        cloak(s, 9, 5, MANTLE);
        return s.pack();
    }

    /**
     * Death as she is. A robe of black so deep it barely takes light, to the ground and over it;
     * a hood standing off the skull all round, its brim drawn forward, so the face is a long way
     * back in the dark; the face a skull, ivory, fine-boned; long sleeves hanging open below the
     * wrist, and the hands in them bone.
     */
    private static Sheet death() {
        Sheet s = new Sheet("Death", 128, 128);
        body(s, true, true, ROBE, ROBE, ROBE, ROBE, BONE);
        skirt(s, 6, 7, 0.15F, ROBE);
        // The hood: crown, two cheeks, a back, and a brim, all a hand's breadth off the skull.
        s.add("hood", -5, -9.4F, -4.5F, 10, 1, 10, 0, HOOD);
        s.sym("hood", -5.4F, -9, -5.2F, 1, 9, 10, 0, HOOD);
        s.add("hood", -5, -9, 4.5F, 10, 10, 1, 0, HOOD);
        s.add("brim", -5, -1, -1, 10, 2, 2, 0, HOOD);
        // The sleeves, open and hanging from the elbow.
        s.sym("sleeve", -2.5F, 0, -2.5F, 5, 6, 5, 0, ROBE);
        // A shawl of the robe over the shoulders, and the length of it down her back.
        s.add("chest", -4.6F, -7.6F, -2.4F, 9, 3, 5, 0.2F, ROBE);
        cloak(s, 10, 7, ROBE);
        return s.pack();
    }

    /**
     * Ragna: a woad-blue dress; a linen apron-dress over it with bronze brooches and a string of
     * glass beads in four colours; the sleeves of her shift pushed above the elbow for the
     * washing; a white kerchief knotted at the nape, and a fair braid from under it to her waist.
     */
    private static Sheet ragna() {
        Sheet s = new Sheet("Ragna", 128, 128);
        body(s, true, false, DRESS, SLEEVE, DRESS, SHOE, HAND);
        skirt(s, 6, 6, 0.05F, DRESS);
        s.add("chest", -3.6F, -6.2F, -2.45F, 7, 7, 1, 0, APRON);
        s.add("abdomen", -3.2F, -5, -2.45F, 6, 5, 1, 0, APRON);
        s.add("apron", -3, 0, -0.5F, 6, 6, 1, 0, APRON);
        s.add("apron2", -3, 0, -0.5F, 6, 4, 1, 0, APRON);
        s.sym("brooch", -1, -1, -0.6F, 2, 2, 1, -0.05F, BROOCH);
        s.add("beads", -2.5F, 0, -0.5F, 5, 1, 1, -0.3F, BEADS);
        // The kerchief over her hair, its knot, and the braid.
        s.add("head", -4, -8, -4, 8, 3, 8, 0.28F, KERCHIEF);
        s.add("head", -4, -5, 1.5F, 8, 3, 3, 0.35F, KERCHIEF);
        s.add("knot", -1.5F, -1, 0, 3, 2, 1, 0.1F, KERCHIEF);
        s.sym("lock", -0.5F, 0, -0.5F, 1, 3, 1, -0.05F, HAIR);
        for (int i = 0; i < BRAID; i++) {
            s.add("braid" + i, -1, 0, -1, 2, i == BRAID - 1 ? 3 : 4, 2, i == 0 ? 0.1F : -0.05F * i, HAIR);
        }
        return s.pack();
    }

    /**
     * Hrafn: a brown wool tunic to the knee with a woven band at its hem and cuffs, belted, a
     * knife at the belt as well as the axe; dark trousers bound with striped wraps; turnshoes; a
     * grey cloak pinned at the right shoulder with an iron ring-pin. Dark auburn hair to the
     * collar, a short beard going grey, a farmer's hands.
     */
    private static Sheet hrafn() {
        Sheet s = new Sheet("Hrafn", 128, 128);
        body(s, false, false, TUNIC, TUNIC, TROUSERS, SHOE, HAND);
        hem(s, 6, 0.05F, TUNIC);
        s.add("pelvis", -4.5F, -2.4F, -2.5F, 9, 2, 5, 0, BELT);
        s.add("pelvis", 2.4F, -1, -2.8F, 1, 4, 1, 0, KNIFE);
        s.sym("shin", -2, 0.3F, -2, 4, 4, 4, 0.18F, WRAPS);
        // Hair to the collar, a fringe, a beard and moustache.
        s.add("head", -4, -8, -4, 8, 2, 8, 0.22F, HAIR);
        s.add("head", -4, -6.2F, 2.6F, 8, 7, 2, 0.24F, HAIR);
        s.add("head", -4.25F, -6.2F, -2.5F, 1, 4, 4, 0, HAIR);
        s.add("head", 3.25F, -6.2F, -2.5F, 1, 4, 4, 0, HAIR);
        s.sym("lock", -0.5F, 0, -0.5F, 1, 3, 1, 0, HAIR);
        s.add("head", -2.5F, -2.6F, -4.5F, 5, 1, 1, 0, BEARD);
        s.add("beard0", -3, 0, -1, 6, 3, 2, 0, BEARD);
        s.add("beard1", -2, 0, -1, 4, 2, 2, -0.05F, BEARD);
        // The cloak, its fold over the shoulders, and the ring-pin at the right shoulder.
        cloak(s, 9, 6, CLOAK);
        s.add("chest", -4.6F, -7.6F, -2.4F, 9, 2, 5, 0.12F, CLOAK);
        s.add("chest", -4.4F, -7.8F, -3.2F, 2, 2, 1, 0, PIN);
        axe(s);
        return s.pack();
    }

    /**
     * The dead under the mound: grey skin drawn tight over bone, the ribs showing where the mail
     * has rotted away; a jaw that hangs; rusted mail over rotted wool; a rusted helm with a nasal;
     * hair and beard grown long and grey in the grave; a shroud of a cloak with holes worn through
     * it; and eyes that give a little light.
     */
    private static Sheet draugr() {
        Sheet s = new Sheet("The draugr", 128, 128);
        body(s, false, true, MAIL, MAIL, SHROUD, WRAPS, SKIN);
        hem(s, 7, 0.05F, SHROUD);
        s.add("pelvis", -4.5F, -2.4F, -2.5F, 9, 2, 5, 0, BELT);
        s.add("chest", -3, -6, -2.2F, 6, 4, 1, 0, RIBS);
        s.sym("shin", -2, 2, -2, 4, 3, 4, 0.15F, WRAPS);
        // Hair long and matted down the back; the jaw that hangs; the beard on it.
        s.add("head", -4, -8, -4, 8, 2, 8, 0.25F, HAIR);
        for (int i = 0; i < BRAID; i++) {
            s.add("braid" + i, -3.5F, 0, -0.5F, 7, i == 0 ? 4 : 3, 1, 0, HAIR);
        }
        s.add("jaw", -3, 0, -4.2F, 6, 2, 4, 0, FACE);
        s.add("jaw", -2.5F, -0.4F, -4.15F, 5, 1, 1, -0.1F, TEETH);
        s.add("beard0", -3, 0, -1, 6, 3, 2, 0, BEARD);
        s.add("beard1", -2, 0, -1, 4, 3, 2, -0.05F, BEARD);
        // The helm: a cap, its rim, the nasal down over the nose, one cheek-plate left.
        s.add("helm", -4.5F, -9, -4.5F, 9, 4, 9, 0, HELM);
        s.add("helm", -4.6F, -5.6F, -4.6F, 9, 1, 9, 0.1F, HELM);
        s.add("helm", -0.5F, -5.4F, -5, 1, 3, 1, 0, HELM);
        s.add("helm", -4.7F, -5, -3.8F, 1, 3, 3, 0, HELM);
        cloak(s, 9, 6, SHROUD);
        axe(s);
        return s.pack();
    }

    /**
     * The Gardener, Ord Zyonz: an Elder older than most stars, and he looks it, kindly. Green
     * skin, the colour of moss in shade; long white hair down his back and a long white beard,
     * leaves caught in both; a circlet of small leaves and white flowers. A robe of undyed
     * bark-cloth to the ground, its open sleeves hanging from the elbow; a mantle of living
     * leaves over his shoulders and down his back; a belt of plaited grass, a trowel through it.
     * Barefoot, his toes long, like roots. When he has bound the Time Stone into himself it
     * shows in his chest, green, with roots running out from it under the skin.
     */
    private static Sheet gardener() {
        Sheet s = new Sheet("The Gardener", 128, 128);
        body(s, false, false, ROBE, ROBE, ROBE, SKIN, HAND);
        skirt(s, 6, 7, 0.1F, ROBE);
        s.add("pelvis", -4.5F, -2.4F, -2.5F, 9, 2, 5, 0, BELT);
        // The robe's sleeves, open and hanging from the elbow.
        s.sym("sleeve", -2.5F, 0, -2.5F, 5, 5, 5, 0, ROBE);
        // Hair: over the crown, down past his ears, and the whole length of it down his back.
        s.add("head", -4, -8, -4, 8, 2, 8, 0.22F, HAIR);
        s.add("head", -4.25F, -6.2F, -3, 1, 6, 5, 0, HAIR);
        s.add("head", 3.25F, -6.2F, -3, 1, 6, 5, 0, HAIR);
        s.add("head", -4, -6.2F, 2.6F, 8, 7, 2, 0.24F, HAIR);
        for (int i = 0; i < BRAID; i++) {
            s.add("braid" + i, -3.5F, 0, -0.5F, 7, i == 0 ? 5 : 4, 1, -0.02F * i, HAIR);
        }
        // The circlet: a ring of leaves round the crown, open in the middle.
        s.add("head", -4.6F, -8.4F, -4.6F, 9, 1, 9, 0.05F, LEAVES);
        // A moustache over the lip, the beard to his chest in two lengths.
        s.add("head", -2.5F, -2.6F, -4.5F, 5, 1, 1, 0, BEARD);
        s.add("beard0", -3, 0, -1, 6, 4, 2, 0, BEARD);
        s.add("beard1", -2, 0, -1, 4, 4, 2, -0.05F, BEARD);
        // The mantle of leaves over the shoulders, and down his back.
        s.add("chest", -4.6F, -7.6F, -2.5F, 9, 3, 5, 0.25F, LEAVES);
        cloak(s, 9, 5, LEAVES);
        // The stone, once it is in him.
        s.add("gem", -1, -1, -0.5F, 2, 2, 1, -0.05F, GEM);
        // The trowel: a turned handle and a blade worn bright.
        s.add("trowel", -0.5F, -4, -0.5F, 1, 5, 1, -0.1F, HAFT);
        s.add("trowel", -1.5F, -9, -0.5F, 3, 5, 1, -0.32F, BLADE);
        return s.pack();
    }

    /**
     * Death, as she makes herself for someone she wants something from: not as she is, but as they
     * would most like her to be. A girl their own age, it seems, cute and knowing it: black hair,
     * full and soft round her face, a heavy fringe, a white skull clip; big green eyes, a little
     * heart drawn under one, freckles, pink in the cheeks; a soft smile. An oversized black top,
     * cropped, with long sleeves, checked bands at the shoulders and studded cuffs; a black choker;
     * little black shorts; fishnet below the knee; heavy black platform boots.
     * <p>
     * Built curvy, as nobody else in the north is: everything rounded off in steps, a full bust,
     * a waist pulled in tight under the top's hem, and hips and thighs that swell out wider than
     * her shoulders before they taper to the knee.
     */
    private static Sheet temptress() {
        Sheet s = new Sheet("Death, as you'd want her", 128, 128);
        // The skeleton, as body() makes it but hers: a narrow waist, long legs with slim shins, no nose to speak of.
        s.add("pelvis", -4, -2, -2, 8, 3, 4, 0, SKIN);
        s.add("abdomen", -3, -5, -2, 6, 5, 4, 0, SKIN);
        s.add("chest", -4, -7, -2, 8, 7, 4, 0, TOP);
        s.add("head", -4, -8, -4, 8, 8, 8, 0, FACE);
        s.add("head", -1.5F, -0.5F, -1.5F, 3, 2, 3, 0, SKIN);
        s.add("lid", -3, 0, 0, 2, 1, 0, 0, LID);
        s.add("lid", 1, 0, 0, 2, 1, 0, 0, LID);
        s.add("mouth", -1, 0, 0, 2, 1, 0, 0, MOUTH);
        s.sym("upper", -1.5F, -1.5F, -1.5F, 3, 6, 3, 0, TOP);
        s.sym("fore", -1.5F, 0, -1.5F, 3, 4, 3, -0.05F, TOP);
        s.sym("hand", -1.25F, 0, -1, 3, 1, 2, -0.1F, HAND);
        s.sym("fingers", -1.25F, 0, -0.5F, 3, 2, 1, -0.08F, HAND);
        s.sym("thumb", -0.5F, 0, -0.5F, 1, 1, 1, -0.1F, HAND);
        s.sym("thigh", -2, 0, -2, 4, 7, 4, 0, SKIN);
        s.sym("shin", -1.6F, 0, -1.6F, 3, 6, 3, 0.1F, NET);
        s.sym("foot", -2, 0, -3, 4, 1, 5, 0, BOOT);

        // The top: loose over the shoulders and the sleeves, a full bust under it in three steps
        // (a little out at the collarbone, most at the middle, tucked in beneath), and a wide hem
        // that stands off the waist.
        s.add("chest", -4.5F, -7.3F, -2.4F, 9, 3, 5, 0.05F, TOP);
        s.add("chest", -3.5F, -6.3F, -3.0F, 7, 1, 1, 0.05F, TOP);
        s.add("chest", -3.5F, -5.4F, -3.7F, 7, 2, 2, 0.06F, TOP);
        s.add("chest", -3, -3.5F, -3.2F, 6, 1, 2, 0.05F, TOP);
        s.add("abdomen", -4, -5.2F, -2.5F, 8, 2, 5, 0.1F, TOP);
        s.add("abdomen", -3.5F, -2, -2.2F, 7, 2, 4, 0.05F, SKIN);
        s.sym("upper", -2, -1.8F, -2, 4, 6, 4, 0.12F, TOP);
        s.sym("fore", -2, 0, -2, 4, 3, 4, 0.14F, TOP);

        // The shorts, low across the hips, and round at the back: the waistband a step in from them, so the waist swells into the hips.
        s.add("pelvis", -4.5F, -2.5F, -2.5F, 9, 1, 5, 0.05F, SHORTS);
        s.add("pelvis", -5.5F, -1.6F, -2.8F, 11, 3, 6, 0.05F, SHORTS);
        s.add("pelvis", -5, -1.2F, 2.7F, 10, 3, 1, 0.12F, SHORTS);

        // The thighs: widest at the top, out past the hips, and tapering to the knee in steps, so
        // they meet in the middle all the way down. The shorts' leg over the top of each.
        s.sym("thigh", -4, -0.4F, -2.8F, 6, 3, 6, 0, SKIN);
        s.sym("thigh", -4, -0.4F, -2.8F, 6, 1, 6, 0.14F, SHORTS);
        s.sym("thigh", -3, 2.6F, -2.5F, 5, 2, 5, 0.05F, SKIN);
        s.sym("thigh", -2.5F, 4.6F, -2.2F, 4, 2, 4, 0.12F, SKIN);
        // The stockings' tops, at the knee.
        s.sym("thigh", -2, 6.2F, -2, 4, 1, 4, 0.14F, NET);
        // The boots: to mid-shin, on a thick sole.
        s.sym("shin", -2, 3.2F, -2, 4, 3, 4, 0.12F, BOOT);
        s.sym("foot", -2, 0.6F, -3, 4, 1, 5, 0.18F, BOOT);
        // Studded cuffs at the wrists, checked bands at the shoulders, and the choker.
        s.sym("fore", -2, 2.4F, -2, 4, 1, 4, 0.22F, CUFF);
        s.sym("upper", -2, -1.8F, -2, 4, 1, 4, 0.2F, CUFF);
        s.add("head", -1.5F, -0.2F, -1.5F, 3, 1, 3, 0.16F, CUFF);
        // Her hair: full and rounded over the crown, soft and wide at the sides and round her face,
        // down the back to the shoulders; and the clip.
        s.add("head", -4, -8, -4, 8, 2, 8, 0.32F, HAIR);
        s.add("head", -3.5F, -8.9F, -3.5F, 7, 1, 7, 0, HAIR);
        s.add("head", -5.1F, -7.2F, -4.3F, 2, 7, 8, 0, HAIR);
        s.add("head", 3.1F, -7.2F, -4.3F, 2, 7, 8, 0, HAIR);
        s.add("head", -5.5F, -5.6F, -3.6F, 1, 4, 6, 0, HAIR);
        s.add("head", 4.5F, -5.6F, -3.6F, 1, 4, 6, 0, HAIR);
        s.add("head", -4.5F, -7.4F, 2.4F, 9, 8, 2, 0.1F, HAIR);
        for (int i = 0; i < BRAID; i++) {
            s.add("braid" + i, -4.5F, 0, -0.5F, 9, i == 0 ? 4 : 3 - i, 1, -0.03F * i, HAIR);
        }
        s.add("head", 1.6F, -7.8F, -4.7F, 2, 2, 1, 0, CLIP);
        // What comes loose as it rots, shown only once it has: her left eye out of its socket on its
        // stalk; a flap of skin peeled half off each forearm; her jaw, come away at one hinge; and a
        // loop of something out of the split in her belly.
        s.add("eye", -0.5F, 0, -0.5F, 1, 2, 1, -0.25F, GUT);
        s.add("eye", -1, 1.6F, -1, 2, 2, 2, -0.15F, EYEBALL);
        s.sym("flap", -0.5F, 0, -1.5F, 1, 4, 3, -0.3F, FLAP);
        s.add("jaw", -3, 0, -4.4F, 6, 2, 4, 0.05F, JAWBONE);
        s.add("gut", -1, 0, -1, 2, 3, 2, -0.1F, GUT);
        s.add("gut", -0.5F, 2.6F, -1.4F, 2, 3, 2, -0.15F, GUT);
        return s.pack();
    }

    /**
     * Death as she is, once she no longer needs to be anything else: the queen of the dead out of
     * the old stories. A skull, white and grinning, close in a hood of violet-black; under it a
     * woman's body, pale as frost, in a suit of black that takes the light violet, shaped at the
     * bust and cut high at the hip, every edge of it trimmed white; a collar of little bones at her
     * throat; gloves of the same black past the elbow, banded white, a silver cuff at each wrist; a
     * white sash low on her hips and from it a fall of long ribbons, white and black, to her shins;
     * stockings to mid-thigh, banded at the top, open at the toe and the heel. A cape from her
     * shoulders to the ground, and tied at each wrist, so that when she lifts her arms she has wings.
     */
    private static Sheet unveiled() {
        Sheet s = new Sheet("Death, unveiled", 128, 192);
        // The skeleton, as long in the leg as the pretty one: a narrow waist, slim shins and feet.
        s.add("pelvis", -4, -2, -2, 8, 3, 4, 0, SUIT);
        s.add("abdomen", -3, -5, -2, 6, 5, 4, 0, SUIT);
        s.add("chest", -4, -7, -2, 8, 7, 4, 0, SUIT);
        skull(s);
        s.sym("upper", -1.5F, -1.5F, -1.5F, 3, 6, 3, 0, GLOVE);
        s.sym("fore", -1.5F, 0, -1.5F, 3, 4, 3, -0.05F, GLOVE);
        s.sym("hand", -1.25F, 0, -1, 3, 1, 2, -0.1F, GLOVE);
        s.sym("fingers", -1.25F, 0, -0.5F, 3, 2, 1, -0.08F, GLOVE);
        s.sym("thumb", -0.5F, 0, -0.5F, 1, 1, 1, -0.1F, GLOVE);
        s.sym("thigh", -2, 0, -2, 4, 7, 4, 0, STOCKING);
        s.sym("shin", -1.5F, 0, -1.5F, 3, 6, 3, 0.1F, STOCKING);
        s.sym("foot", -1.5F, 0, -3, 3, 1, 5, 0, STOCKING);

        // The bust, in three steps as the suit shapes it: bare above the neckline, the fullest step
        // with the neckline scalloped along its top, and tucked in under it; the shoulders bare.
        s.add("chest", -3.5F, -6.1F, -2.8F, 7, 1, 1, 0.05F, SUIT);
        s.add("chest", -3.5F, -5.3F, -3.5F, 7, 2, 2, 0.06F, SUIT);
        s.add("chest", -3, -3.4F, -3.0F, 6, 1, 2, 0.05F, SUIT);
        // Her hips, wider than her waist, and the thighs widest at the top.
        s.add("pelvis", -4.5F, -1.2F, -2.4F, 9, 2, 5, 0, SUIT);
        // And round behind: the seat of the suit full, and the thighs under it.
        s.add("pelvis", -4, -1.5F, 2.3F, 8, 3, 1, 0.15F, SUIT);
        s.add("pelvis", -3, -1, 3.1F, 6, 2, 1, 0.05F, SUIT);
        s.sym("thigh", -2.2F, -0.2F, 1.6F, 4, 2, 1, 0.1F, STOCKING);
        s.sym("thigh", -2.4F, -0.3F, -2.4F, 5, 3, 5, 0, STOCKING);
        s.sym("thigh", -2.2F, 2.6F, -2.2F, 4, 2, 4, 0.15F, STOCKING);
        // The stockings' tops, banded; the swell of the calf.
        s.sym("thigh", -2, 3.6F, -2, 4, 1, 4, 0.2F, BAND);
        s.sym("shin", -1.5F, 0.7F, -1.5F, 3, 3, 3, 0.3F, STOCKING);

        // The collar of little bones: a string close round the neck, and a longer one lying on her collarbones.
        s.add("chest", -2.5F, -7.45F, -2.45F, 5, 1, 1, -0.15F, NECKLACE);
        s.add("chest", -3.5F, -7.45F, -2.2F, 1, 1, 3, -0.15F, NECKLACE);
        s.add("chest", 2.5F, -7.45F, -2.2F, 1, 1, 3, -0.15F, NECKLACE);
        s.add("chest", -3, -6.95F, -2.55F, 6, 1, 1, -0.22F, NECKLACE);
        // The gloves' white bands above the elbow; the silver cuffs at the wrists, where the cape is tied.
        s.sym("upper", -1.5F, 1.2F, -1.5F, 3, 1, 3, 0.14F, BAND);
        s.sym("fore", -1.5F, 2.8F, -1.5F, 3, 1, 3, 0.24F, BAND);
        // The sash, low on her hips, and the ribbons that fall from it.
        s.add("sash", -4.5F, -0.5F, -2.5F, 9, 1, 5, 0.12F, SASH);
        skirt(s, 6, 5, 0.04F, RIBBON);

        // The hood: close round the skull, the skull set back in it. Rounded over the crown, its edge drawn
        // forward over the brow, the opening arched; falling to her shoulders at the sides and down her
        // back; a second layer over the sides and the back, narrower, so it is rounded outside.
        s.add("hood", -5, -9.4F, -4.9F, 10, 1, 10, 0, HOOD);
        s.add("hood", -4, -10.2F, -3.9F, 8, 1, 8, 0, HOOD);
        s.add("hood", -5, -9.2F, -5.9F, 10, 1, 2, 0, HOOD);
        s.sym("hood", -5.4F, -9, -5.2F, 1, 10, 10, 0, HOOD);
        s.sym("hood", -6.1F, -8, -3.6F, 1, 7, 8, 0, HOOD);
        s.sym("hood", -6.3F, -1.4F, -3.6F, 1, 2, 9, 0, HOOD);
        s.add("hood", -5, -9, 4.6F, 10, 11, 1, 0, HOOD);
        s.add("hood", -4, -8.2F, 5.4F, 8, 8, 1, 0, HOOD);
        s.sym("hood", -4.6F, -8.6F, -5.2F, 2, 1, 1, 0, HOOD);
        s.sym("hood", -4.6F, -7.6F, -5.2F, 1, 1, 1, 0, HOOD);
        // All of it cloth with no thickness: one sheet, the outside on one face and the lining on the other, so
        // there is no edge to see and nothing between them.
        // The cape, from her shoulders to the ground, in pleats: each strip from its inner edge outward, her right
        // toward -x and her left toward +x, in lengths.
        for (int j = 0; j < CAPE_PLEATS; j++) {
            int w = PLEAT_WIDTHS[j];
            for (int i = 0; i < CAPE_SEGMENTS; i++) {
                s.add("capeR" + j + "_" + i, -w, 0, 0, w, CAPE_LENGTH, 0, 0, CAPE);
                s.add("capeL" + j + "_" + i, 0, 0, 0, w, CAPE_LENGTH, 0, 0, CAPE);
            }
        }
        // And from each wrist, a fall of it in three lengths, straight-edged, back from the wrist toward the rest
        // of it behind her.
        for (int i = 0; i < DRAPES; i++) {
            s.sym("drape" + i, 0, 0, -1, 0, DRAPE_LENGTH, 8, 0, CAPE);
        }
        return s.pack();
    }

    /**
     * Her skull, shaped, not a cube: the cranium rounded over the crown in steps and widest at the
     * brow and the cheekbones; the face narrowing under them to the teeth; the bones of the neck under it. The face is painted across the fronts of all of it as
     * one picture, the sockets and the nose cut through to the dark set back inside, with one point
     * of light in each socket. The jaw on its own, so it drops when she speaks; it hangs from its
     * hinge at (0, -2, 0.2) in the head (see ModelNorse).
     */
    private static void skull(Sheet s) {
        s.add("head", -2.5F, -8.25F, -2.5F, 5, 1, 6, 0, SKULL);
        s.add("head", -3.5F, -7.5F, -3.5F, 7, 1, 8, 0, SKULL);
        s.add("head", -4, -6.5F, -3.8F, 8, 3, 8, 0, SKULL);
        s.add("head", -3, -6.8F, 4.2F, 6, 3, 1, -0.1F, SKULL);
        s.add("head", -3, -3.5F, -3.8F, 6, 1, 5, 0, SKULL);
        s.add("head", -2.5F, -2.5F, -3.7F, 5, 1, 4, 0, SKULL);
        s.add("head", -1, -1.5F, 0.3F, 2, 1, 2, -0.1F, SKULL);
        s.add("head", -1, -0.6F, 0.4F, 2, 1, 2, -0.1F, SKULL);
        s.add("head", -3.6F, -6.2F, -3, 7, 4, 1, 0, SOCKET);
        s.add("jaw", -3, 0, -4, 6, 2, 4, 0, JAWBONE);
    }

    /**
     * Ayesha, High Priestess of the Sovereign: Warlock's Mother. Gold all over, as they all are,
     * and taller than anyone near her, and she knows it. A gown of gold silk to the floor, close
     * at the waist; open sleeves that hang from the elbow; a breastplate of worked gold over it,
     * a girdle of gold plates; a standing collar fanned out behind her head like a halo, so she
     * is always framed; her hair swept up and back into a crest, and down her back below it; a
     * circlet with one teal stone at the brow. A train of a cape, lined in teal.
     */
    private static Sheet ayesha() {
        Sheet s = new Sheet("Ayesha", 128, 128);
        body(s, true, false, SILK, SILK, SILK, REGALIA, HAND);
        skirt(s, 7, 8, 0.14F, SILK);
        s.add("pelvis", -4.5F, -2.6F, -2.5F, 9, 2, 5, 0, REGALIA);
        // The breastplate: over the chest, a narrower plate down to the girdle.
        s.add("chest", -4.2F, -7.2F, -2.4F, 8, 4, 1, 0.1F, REGALIA);
        s.add("abdomen", -2.5F, -5, -2.3F, 5, 4, 1, 0, REGALIA);
        // The standing collar, fanned behind her head, and its wings round her neck.
        s.add("collar", -6, -13, -0.5F, 12, 13, 1, 0, REGALIA);
        s.sym("wing", -1, -6, -2.5F, 1, 6, 4, 0, REGALIA);
        // The open sleeves.
        s.sym("sleeve", -2.5F, 0, -2.5F, 5, 6, 5, 0, SILK);
        // Her hair: over the crown, swept up into a crest, and down her back.
        s.add("head", -4, -8, -4, 8, 2, 8, 0.22F, HAIR);
        s.add("head", -4.25F, -6.4F, -3, 1, 5, 6, 0, HAIR);
        s.add("head", 3.25F, -6.4F, -3, 1, 5, 6, 0, HAIR);
        s.add("head", -4, -6.4F, 2.6F, 8, 6, 2, 0.24F, HAIR);
        s.add("bun", -2, -4, -3, 4, 4, 6, 0, HAIR);
        for (int i = 0; i < BRAID; i++) {
            s.add("braid" + i, -3, 0, -0.5F, 6, i == 0 ? 5 : 4, 1, -0.02F * i, HAIR);
        }
        // The circlet, and its stone.
        s.add("head", -4.5F, -7.4F, -4.5F, 9, 1, 9, 0.05F, REGALIA);
        s.add("brow", -0.5F, -0.5F, -0.5F, 1, 1, 1, 0, GEM);
        cloak(s, 10, 8, SILK);
        return s.pack();
    }

    /**
     * The Magus: Warlock, grown, from a future where his Mother had her way. The same face, but
     * violet, and older, and certain. Hair white and long, thrown back off his brow like a mane;
     * a gold gem in the brow. A robe of white to the floor with a gold sun on the breast; gold at
     * the shoulders, gold gauntlets, a sash of gold; a collar higher than hers, and a cape of
     * white lined with the violet of his skin. He stands like a man who has never once been
     * contradicted.
     */
    private static Sheet magus() {
        Sheet s = new Sheet("The Magus", 128, 128);
        body(s, false, false, SILK, SILK, SILK, REGALIA, HAND);
        skirt(s, 6, 7, 0.08F, SILK);
        s.add("pelvis", -4.5F, -2.6F, -2.5F, 9, 2, 5, 0, REGALIA);
        // The sun on his breast.
        s.add("chest", -2, -6, -2.35F, 4, 4, 1, 0, REGALIA);
        // Gold over the shoulders, and the gauntlets.
        s.sym("upper", -2.5F, -2, -2.5F, 5, 3, 5, 0.1F, REGALIA);
        s.sym("fore", -2, 1, -2, 4, 3, 4, 0.12F, REGALIA);
        // The collar: higher than hers, and squarer.
        s.add("collar", -7, -15, -0.5F, 14, 15, 1, 0, REGALIA);
        s.sym("wing", -1, -7, -2.5F, 1, 7, 4, 0, REGALIA);
        // His hair: white, the whole mass of it thrown back off a high forehead, and down his back.
        s.add("head", -4, -8.6F, -3.2F, 8, 2, 8, 0.3F, HAIR);
        s.add("head", -4.4F, -7.8F, -1.5F, 1, 5, 6, 0, HAIR);
        s.add("head", 3.4F, -7.8F, -1.5F, 1, 5, 6, 0, HAIR);
        s.add("head", -4.6F, -8, 2.4F, 9, 9, 3, 0.2F, HAIR);
        s.add("bun", -3.5F, -4.5F, -1, 7, 3, 7, 0, HAIR);
        for (int i = 0; i < BRAID; i++) {
            s.add("braid" + i, -4, 0, -0.5F, 8, i == 0 ? 5 : 4, 1, -0.03F * i, HAIR);
        }
        s.add("brow", -0.5F, -0.5F, -0.5F, 1, 1, 1, 0, GEM);
        cloak(s, 11, 8, SILK);
        return s.pack();
    }

    /**
     * What came out of the vats: grown toward a man and abandoned partway. Gold skin gone grey
     * and sick, stretched over too much and too little; one shoulder a mass of growth, a second
     * swelling on the skull; a jaw that never set right and hangs; the rags of the vat's membrane
     * still on it; and eyes that give off a little of the gold they were meant to have.
     */
    private static Sheet spawn() {
        Sheet s = new Sheet("Sovereign spawn", 128, 128);
        body(s, false, true, FLESH, FLESH, FLESH, FLESH, HAND);
        hem(s, 5, 0.05F, SHROUD);
        s.add("chest", -3, -6, -2.2F, 6, 4, 1, 0, RIBS);
        // The growths: a shoulder, the skull, a knot at the hip.
        s.add("upperR", -3.5F, -3, -3, 5, 4, 5, 0, FLESH);
        s.add("head", -5, -9.5F, -2, 4, 4, 5, 0, FLESH);
        s.add("pelvis", 2.5F, -1.5F, -2.5F, 3, 3, 3, 0, FLESH);
        s.add("jaw", -3, 0, -4.2F, 6, 2, 4, 0, FACE);
        s.add("jaw", -2.5F, -0.4F, -4.15F, 5, 1, 1, -0.1F, TEETH);
        return s.pack();
    }

    /** An axe, at the belt and in the hand: a haft, and a bearded head. */
    private static void axe(Sheet s) {
        s.add("axe", -0.5F, -10, -0.5F, 1, 13, 1, -0.1F, HAFT);
        s.add("axe", -0.5F, -10.5F, -4, 1, 3, 4, 0, AXE_HEAD);
        s.add("axe", -0.5F, -8, -3.5F, 1, 2, 2, -0.1F, AXE_HEAD);
    }
}
