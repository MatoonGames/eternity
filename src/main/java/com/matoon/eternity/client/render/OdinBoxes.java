package com.matoon.eternity.client.render;

/**
 * Every box of Odin and Sleipnir, and where each sits on their texture. The model
 * (client/render/ModelOdin.java) and the skin tool (tools/OdinSkin.java) both read this, so the
 * texture is always painted for exactly the boxes the model has. Laid out on a {@link VaultBoxes.Sheet},
 * which packs the texture offsets.
 * <p>
 * Boxes are in their part's own space: y down, the front at -z, the figure's right at -x. Parts with
 * a side are given for the right and mirrored to the left ("R" and "L" on the end of the name).
 * <p>
 * Plain Java with nothing of Minecraft in it, so the skin tool can compile against it.
 */
public final class OdinBoxes {

    // What a box is made of, which decides how the skin tool paints it.
    /** Sleipnir: his dark coat, his pale mane and tail, his muzzle, his gold-shod hooves, the crimson caparison, gold barding, the saddle. */
    public static final int HIDE = 70, MANE = 71, MUZZLE = 72, HOOF = 73, CAPARISON = 74, BARD = 75, SADDLE = 76;
    /** Bright polished gold, for trim and rings; and the gold of a helm's horns. */
    public static final int BRIGHT = 77, HORN = 78;
    /** Odin: his face (the patch over his right eye), beard, hair, helm, the dark scale under his armour, the cuirass, plate,
     * his cape, his breeches, boots and gloves; Gungnir's shaft and blade. */
    public static final int FACE = 80, BEARD = 81, HAIR = 82, HELM = 83, MAIL = 84, CUIRASS = 85, PLATE = 86, CAPE = 87,
            CLOTH = 88, BOOT = 89, GLOVE = 90, SHAFT = 91, BLADE = 92;

    /** Segments in his cape, top to bottom; in Sleipnir's tail; and in each of the helm's horns. */
    public static final int CAPE_SEGMENTS = 4, TAIL_SEGMENTS = 4, HORN_SEGMENTS = 4;
    /** Sleipnir's eight legs: front and hind, each side, outer and inner. */
    public static final String[] LEGS = {"Fo", "Fi", "Ho", "Hi"};

    public static final VaultBoxes.Sheet SHEET = odin();

    private OdinBoxes() {
    }

    private static VaultBoxes.Sheet odin() {
        VaultBoxes.Sheet s = new VaultBoxes.Sheet("Odin", 256, 256);

        // ------------------------------------------------------------------ Sleipnir
        // His barrel, the hip at its back: deep and long, under a crimson caparison hemmed in gold, a gold peytral over
        // his chest, a crupper plate over his rump, and the saddle.
        s.add("barrel", -5, -4, -25, 10, 10, 26, 0, HIDE);
        s.add("barrel", -5.5F, -4.6F, -21, 11, 7, 19, 0, CAPARISON);
        s.add("barrel", -5.5F, 2.4F, -21, 11, 2, 19, 0.05F, BRIGHT);
        s.add("barrel", -5, -3, -26.6F, 10, 8, 2, 0.15F, BARD);
        s.add("barrel", -5, -4.9F, -3.5F, 10, 4, 6, 0.1F, BARD);
        s.add("barrel", -3.5F, -6.2F, -16, 7, 2, 8, 0, SADDLE);
        s.add("barrel", -1.5F, -8.4F, -16.6F, 3, 3, 2, 0, BRIGHT);
        s.add("barrel", -3.5F, -8.6F, -9.4F, 7, 3, 2, 0, BRIGHT);

        // The neck, arched, armoured down its front, the mane along its crest.
        s.add("neck", -2.5F, -13, -4, 5, 14, 7, 0, HIDE);
        s.add("neck", -2.5F, -13, -4.4F, 5, 12, 2, 0.2F, BARD);
        s.add("neck", -1, -14.5F, 1.8F, 2, 15, 3, 0, MANE);

        // The head: a long skull, the muzzle, a gold chanfron down his face with a ridge along it, the forelock, the ears.
        s.add("horseHead", -2.5F, -4, -9, 5, 5, 9, 0, HIDE);
        s.add("horseHead", -2, -2.6F, -13, 4, 4, 4, 0, MUZZLE);
        s.add("horseHead", -2.5F, -4.4F, -11.2F, 5, 2, 9, 0.12F, BARD);
        s.add("horseHead", -0.5F, -5.6F, -10, 1, 2, 6, 0, BRIGHT);
        s.add("horseHead", -1, -5.2F, -1.5F, 2, 2, 4, 0, MANE);
        s.sym("ear", -0.5F, -3, -0.5F, 1, 3, 1, 0, HIDE);

        // The tail, in segments that stream behind him.
        for (int i = 0; i < TAIL_SEGMENTS; i++) {
            s.add("tail" + i, -1.5F, 0, -1.5F, 3, i == 0 ? 5 : 6, 3, i * 0.12F, MANE);
        }

        // Eight legs. The fore: a forearm, gold at the knee; the cannon, feathered at the fetlock; the hoof, gold-shod.
        // The hind: a heavier gaskin, then the same.
        for (String leg : LEGS) {
            boolean fore = leg.charAt(0) == 'F';
            if (fore) {
                s.sym("up" + leg, -1.5F, 0, -1.5F, 3, 7, 3, 0, HIDE);
            } else {
                s.sym("up" + leg, -2, 0, -2, 4, 8, 4, 0, HIDE);
            }
            s.sym("up" + leg, -1.5F, fore ? 4.6F : 5.6F, -2, 3, 2, 3, 0.15F, BARD);
            s.sym("low" + leg, -1, 0, -1, 2, fore ? 7 : 6, 2, 0, HIDE);
            s.sym("low" + leg, -1.5F, fore ? 4.8F : 3.8F, -1.5F, 3, 2, 3, 0, MANE);
            s.sym("hoof" + leg, -1.5F, 0, -1.5F, 3, 2, 3, 0, HOOF);
        }

        // ------------------------------------------------------------------ Odin
        // Hips in the saddle: scale under a broad gold belt and its buckle, faulds hung over his thighs.
        s.add("hips", -4, -2, -2.5F, 8, 3, 5, 0, MAIL);
        s.add("hips", -4.5F, -2.6F, -3, 9, 2, 6, 0, PLATE);
        s.add("hips", -1.5F, -3, -3.4F, 3, 3, 1, 0, BRIGHT);
        s.sym("fauld", -1, 0, -3, 1, 4, 6, 0, PLATE);

        // Legs astride: dark breeches with plate over the thigh; boots, greaves, the foot in the stirrup.
        s.sym("thigh", -2, 0, -2, 4, 7, 4, 0, CLOTH);
        s.sym("thigh", -2, 0.6F, -2.6F, 4, 5, 1, 0.1F, PLATE);
        s.sym("shin", -2, 0, -2, 4, 7, 4, 0, BOOT);
        s.sym("shin", -2, 0, -2.7F, 4, 5, 1, 0.15F, PLATE);
        s.sym("shin", -2, 5.2F, -3.4F, 4, 2, 5, 0.1F, BOOT);

        // The body: scale at the waist under plates, the gold cuirass with its raised breastplate, a gorget at the throat,
        // the red of the cape's mantle across the back of his shoulders, and the clasps that hold it.
        s.add("abdomen", -4, -5, -2.5F, 8, 5, 5, 0, MAIL);
        s.add("abdomen", -3.5F, -4.6F, -2.9F, 7, 4, 1, 0, PLATE);
        s.add("chest", -4.5F, -7, -3, 9, 7, 6, 0, CUIRASS);
        s.add("chest", -4, -6.4F, -3.6F, 8, 5, 1, 0, CUIRASS);
        s.add("chest", -3.5F, -8.6F, -3.2F, 7, 2, 6, 0, PLATE);
        s.add("chest", -5.5F, -8.2F, 0.8F, 11, 2, 3, 0, CAPE);
        s.sym("clasp", -1, -1, -0.5F, 2, 2, 1, 0, BRIGHT);

        // The head: an old king's face, the beard at his jaw, white hair down his back under the helm.
        s.add("head", -4, -8, -4, 8, 8, 8, 0, FACE);
        s.add("head", -4, -2.6F, -4.6F, 8, 4, 1, 0.15F, BEARD);
        s.add("head", -4, -5, 3.4F, 8, 7, 1, 0.15F, HAIR);
        // The beard falls to his chest, in two lengths that sway.
        s.add("beard", -3.5F, -0.5F, -1, 7, 6, 2, 0, BEARD);
        s.add("beardTip", -2.5F, 0, -1, 5, 4, 2, 0, BEARD);
        s.add("beardTip", -1.5F, 3.5F, -0.8F, 3, 2, 1, 0, BEARD);

        // The helm: a gold crown, its brow band, a crest from front to back, cheek guards, and the horns.
        s.add("helm", -4.5F, -9, -4.5F, 9, 4, 9, 0.05F, HELM);
        s.add("helm", -4.5F, -5.4F, -4.5F, 9, 1, 9, 0.15F, BRIGHT);
        s.add("helm", -0.5F, -10.6F, -4.9F, 1, 2, 10, 0, BRIGHT);
        s.sym("cheek", -0.5F, 0, -2.5F, 1, 5, 5, 0, HELM);
        for (int i = 0; i < HORN_SEGMENTS; i++) {
            switch (i) {
                case 0:
                    s.sym("horn" + i, -1.5F, -1.5F, -1.5F, 3, 3, 3, 0, HORN);
                    break;
                case 1:
                    s.sym("horn" + i, -1, -5, -1, 2, 5, 2, 0.1F, HORN);
                    break;
                case 2:
                    s.sym("horn" + i, -1, -5, -1, 2, 5, 2, -0.1F, HORN);
                    break;
                default:
                    s.sym("horn" + i, -0.5F, -4, -0.5F, 1, 4, 1, 0, HORN);
                    break;
            }
        }

        // Arms: scale sleeves under layered gold pauldrons, vambraces, gloves.
        s.sym("upper", -3, -1.5F, -2, 4, 6, 4, 0, MAIL);
        s.sym("pauldron", -4.6F, -3.2F, -3.5F, 6, 3, 7, 0, PLATE);
        s.sym("pauldron", -5, -1.4F, -3.5F, 5, 2, 7, 0.1F, PLATE);
        s.sym("pauldron", -5.3F, 0.2F, -3.5F, 4, 2, 7, 0.1F, PLATE);
        s.sym("pauldron", -4.6F, -3.6F, -3.5F, 6, 1, 7, 0.05F, BRIGHT);
        s.sym("fore", -2, 0, -2, 4, 7, 4, 0, MAIL);
        s.sym("fore", -2, 0.6F, -2, 4, 4, 4, 0.35F, PLATE);
        s.sym("fore", -2, 5, -2, 4, 2, 4, 0.25F, GLOVE);

        // The cape, in segments, widening as it falls.
        for (int i = 0; i < CAPE_SEGMENTS; i++) {
            int w = 11 + i;
            s.add("cape" + i, -w / 2F, 0, 0, w, i == CAPE_SEGMENTS - 1 ? 8 : 7, 1, 0, CAPE);
        }

        // Gungnir: a long gold shaft bound with rings, a collar with two prongs swept back from it, and the blade.
        s.add("gungnir", -0.5F, -30, -0.5F, 1, 52, 1, -0.1F, SHAFT);
        s.add("gungnir", -0.5F, -10, -0.5F, 1, 2, 1, 0.12F, BRIGHT);
        s.add("gungnir", -0.5F, 4, -0.5F, 1, 2, 1, 0.12F, BRIGHT);
        s.add("gungnir", -1.5F, -32, -1.5F, 3, 2, 3, 0, BRIGHT);
        s.add("gungnir", -3.5F, -33, -0.5F, 2, 1, 1, 0, BRIGHT);
        s.add("gungnir", 1.5F, -33, -0.5F, 2, 1, 1, 0, BRIGHT);
        s.add("gungnir", -3.5F, -35, -0.5F, 1, 2, 1, 0, BRIGHT);
        s.add("gungnir", 2.5F, -35, -0.5F, 1, 2, 1, 0, BRIGHT);
        s.add("gungnir", -1.5F, -40, -0.5F, 3, 8, 1, 0, BLADE);
        s.add("gungnir", -0.5F, -41, -0.5F, 1, 9, 1, 0.15F, BLADE);
        s.add("gungnir", -0.5F, -43, -0.5F, 1, 2, 1, -0.15F, BLADE);
        s.add("gungnir", -0.5F, 22, -0.5F, 1, 3, 1, -0.1F, BRIGHT);

        return s.pack();
    }
}
