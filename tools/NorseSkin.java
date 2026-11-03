import com.matoon.eternity.client.render.NorseBoxes;
import com.matoon.eternity.client.render.VaultBoxes.Box;
import com.matoon.eternity.client.render.VaultBoxes.Sheet;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Paints the people of the first chapter onto the sheets {@link NorseBoxes} lays out, every box by
 * what it is made of and which way each face of it looks:
 * <ul>
 * <li><b>norse_stranger.png</b>: a woman of about thirty in mourning. Black wool to the ground,
 * a fine twill catching the light along its folds, a narrow band of silver-grey braid at the hem;
 * a charcoal apron-dress over it, two oval silver brooches with a boss and a raised rim, a string
 * of jet; a heavy black mantle with a fringe; dark hair smooth from a centre parting, a silver
 * pin through the knot. Her face pale, composed, grey eyes under straight dark brows, the mouth
 * level.</li>
 * <li><b>norse_death.png</b>: a robe of black so deep it barely takes the light, folds of a faint
 * violet in it; a hood dark as a well inside; the face a skull, ivory, fine-boned, the sockets
 * deep; the hands bone, every joint showing.</li>
 * <li><b>norse_ragna.png</b>: a woad-blue dress with a tablet-woven band, red and gold, at the hem;
 * a linen apron-dress over it with bronze brooches and glass beads in four colours; the shift's
 * sleeves pushed up; a white kerchief; a fair braid tied with red; freckles, blue eyes.</li>
 * <li><b>norse_hrafn.png</b>: a brown wool tunic with a green and red band at its hem and cuffs, a
 * belt with a bronze buckle and strap-end, a knife in a sheath; dark trousers bound in striped
 * wraps; stitched turnshoes; a grey cloak with a darker border on an iron ring-pin; dark auburn
 * hair, a beard going grey at the chin, tired eyes with the skin heavy under them, a farmer's
 * weathered hands.</li>
 * <li><b>norse_draugr.png</b> and <b>norse_draugr_glow.png</b>: the dead. Grey-blue skin drawn tight,
 * ribs showing through rotted mail, the mail rusted through in patches, the wool under it rotten;
 * a jaw with its teeth bared; a rusted helm; grey hair and beard grown long in the grave; a
 * shroud worn to holes. Lit from within, the pale points of their eyes.</li>
 * <li><b>norse_gardener.png</b> and <b>norse_gardener_glow.png</b>: the Gardener. Skin the green of
 * moss in shade, deep lines in it; kind gold-green eyes under heavy white brows; white hair and a
 * long white beard with a leaf caught here and there; a circlet of small leaves and white flowers;
 * a robe of undyed bark-cloth with a vine stitched round its hem in green; a mantle of living
 * leaves; a belt of plaited grass; bare feet with long toes like roots; a trowel worn bright. Lit
 * from within once he has bound it into himself: the Time Stone in his chest, and the roots that
 * run out from it under the skin.</li>
 * </ul>
 * Usage: javac -d out tools/NorseSkin.java src/main/java/com/matoon/eternity/client/render/VaultBoxes.java
 * src/main/java/com/matoon/eternity/client/render/NorseBoxes.java
 * then java -cp out NorseSkin src/main/resources/assets/eternity/textures/entity
 */
public class NorseSkin {

    private static final int TOP = 0, BOTTOM = 1, RIGHT = 2, FRONT = 3, LEFT = 4, BACK = 5;

    // ------------------------------------------------------------------ palettes, dark to light
    private static final int[] PALE = {0x4E4044, 0x6E5C5E, 0x927E7E, 0xB4A09E, 0xCDBBB6, 0xE0D2CC, 0xF0E6E0};
    private static final int[] FAIR = {0x5C3A2C, 0x86584A, 0xAC7C64, 0xCC9C80, 0xE0B89A, 0xEED0B6, 0xF8E4D0};
    private static final int[] RUDDY = {0x3C2214, 0x5C3622, 0x7E4E34, 0x9C6644, 0xB47E58, 0xC89670, 0xDCB08C};
    private static final int[] DEAD = {0x161E20, 0x253032, 0x384644, 0x4C5C58, 0x62726C, 0x7C8A82, 0x9AA49A};
    private static final int[] IVORY = {0x3E3A30, 0x625C4C, 0x8A826C, 0xAEA68E, 0xC8C0A8, 0xDDD6C0, 0xF0EBDA};
    private static final int[] BLACK_WOOL = {0x050507, 0x09090D, 0x0F0F15, 0x16161E, 0x1F1F29, 0x2A2A36, 0x3A3A48};
    private static final int[] DEEP_BLACK = {0x010102, 0x030305, 0x060609, 0x0A0A0F, 0x101017, 0x181722, 0x262434};
    private static final int[] CHARCOAL = {0x0C0C0E, 0x141418, 0x1C1C21, 0x26262C, 0x313138, 0x3E3E46, 0x52525C};
    private static final int[] WOAD = {0x0E1A2A, 0x16283E, 0x203654, 0x2C486A, 0x3C5C82, 0x527496, 0x6E8EAE};
    private static final int[] LINEN = {0x5A5242, 0x7A715E, 0x988E78, 0xB4AA92, 0xCAC0A8, 0xDCD4BE, 0xEEE8D6};
    private static final int[] WHITE_LINEN = {0x8A8676, 0xA6A290, 0xBEBAA8, 0xD2CEBC, 0xE2DECC, 0xEEEADA, 0xF8F5EA};
    private static final int[] SILVER = {0x24262C, 0x3E424A, 0x5E646E, 0x828892, 0xA6ACB6, 0xCCD0D8, 0xF0F2F6};
    private static final int[] BRONZE = {0x281808, 0x402810, 0x5A3C18, 0x7A5424, 0x986C34, 0xB88A4A, 0xD8AE70};
    private static final int[] DARK_HAIR = {0x040304, 0x090708, 0x100D0E, 0x181415, 0x221D1E, 0x2E2829, 0x423A3B};
    private static final int[] FAIR_HAIR = {0x584020, 0x7A5A2A, 0x9C783A, 0xBA964E, 0xD2B068, 0xE4C886, 0xF2DEAA};
    private static final int[] AUBURN = {0x1C0B05, 0x2C1309, 0x401D0D, 0x562913, 0x6C371B, 0x844827, 0x9E603A};
    private static final int[] GREY_HAIR = {0x302E2A, 0x484540, 0x625E58, 0x7E7A72, 0x9A968E, 0xB6B2AA, 0xD0CCC4};
    private static final int[] BROWN_WOOL = {0x1C1009, 0x2A180D, 0x3A2213, 0x4C2E1A, 0x603C24, 0x764C30, 0x905E40};
    private static final int[] DARK_WOOL = {0x121010, 0x1C1918, 0x272320, 0x332E2A, 0x413B35, 0x524A42, 0x685E52};
    private static final int[] GREY_WOOL = {0x222428, 0x313438, 0x42464B, 0x55595F, 0x696E74, 0x80858B, 0x9A9FA5};
    private static final int[] LEATHER = {0x120A05, 0x1C1009, 0x29180E, 0x382214, 0x4A2E1C, 0x5E3C26, 0x784E32};
    private static final int[] IRON = {0x141518, 0x212328, 0x313439, 0x44484E, 0x5A5F66, 0x757A82, 0x989EA6};
    private static final int[] RUST = {0x1A0D07, 0x2A150B, 0x3E2111, 0x542E18, 0x6A3C20, 0x7E4E2E, 0x946642};
    private static final int[] ROT = {0x14120D, 0x201D16, 0x2D2920, 0x3B362A, 0x494336, 0x595242, 0x6C6452};
    private static final int[] WOOD = {0x120A06, 0x1C110A, 0x28180E, 0x342013, 0x432A19, 0x553621, 0x6C472C};
    private static final int[] HOOD_IN = {0x000000, 0x010102, 0x030305, 0x05050A, 0x08080F, 0x0C0C14, 0x12121C};
    private static final int[] MOSS = {0x1A2414, 0x28361E, 0x3A4C2A, 0x4E6438, 0x647E48, 0x7C9A5C, 0x98B674};
    private static final int[] WHITE_HAIR = {0x55534C, 0x726F66, 0x8E8B80, 0xA8A598, 0xC0BDB0, 0xD6D3C8, 0xEAE8E0};
    private static final int[] BARK = {0x15110A, 0x221B10, 0x312717, 0x41341F, 0x534329, 0x675435, 0x7E6844};
    private static final int[] LEAF = {0x0C220C, 0x143412, 0x1E4A1A, 0x2A6022, 0x38782C, 0x4A9038, 0x62A84A};
    private static final int[] STRAW = {0x3A3016, 0x54461E, 0x6E5C28, 0x887234, 0xA08A42, 0xB8A256, 0xCEBA70};
    private static final int RED = 0x8C2418, RED_L = 0xB23A26, GOLD = 0xC0962E, GOLD_L = 0xDAB64E, GREEN = 0x2C4A22, GREEN_L = 0x406A30;
    // The Sovereign: gold skin, gold hair, gold silk, worked gold; his violet; and the sick gold of what the vats rejected.
    private static final int[] GOLD_SKIN = {0x5A3C10, 0x7E5818, 0xA27822, 0xC49A34, 0xDCB850, 0xEED27A, 0xFAEAB0};
    private static final int[] VIOLET = {0x24102F, 0x381A4A, 0x4E2A66, 0x683E84, 0x8456A2, 0xA276BE, 0xC29CD8};
    private static final int[] SICK_GOLD = {0x231E10, 0x37301B, 0x4E4529, 0x665B3A, 0x7F734E, 0x998C66, 0xB4A882};
    private static final int[] GOLD_SILK = {0x4A3008, 0x6E4A10, 0x94681A, 0xB88A2A, 0xD4AA44, 0xE8C868, 0xF6E09A};
    private static final int[] WHITE_SILK = {0x6E6A7C, 0x8E8A9C, 0xACA8BA, 0xC6C2D2, 0xDAD6E6, 0xEAE8F2, 0xFAF8FF};
    private static final int[] WORKED_GOLD = {0x4A3410, 0x6E4E16, 0x94701E, 0xBA902A, 0xD8B040, 0xEED068, 0xFFEEAA};
    private static final int[] GOLD_HAIR = {0x6A4810, 0x8E6418, 0xB08222, 0xCCA032, 0xE2BC4E, 0xF0D474, 0xFAE8A6};
    private static final int[] TEAL_SILK = {0x041E1C, 0x08302C, 0x0E4842, 0x16625A, 0x227E74, 0x3A9C90, 0x5EBCB0};
    private static final int[] VIOLET_SILK = {0x14081E, 0x200E2E, 0x2E1642, 0x3E2058, 0x522C70, 0x683A8A, 0x8250A6};
    private static final int[] SNOW_HAIR = {0x7C7A92, 0x9A98AE, 0xB8B6CA, 0xD2D0E0, 0xE6E4F0, 0xF4F3FA, 0xFFFFFF};

    /** Texels to a unit of the model: the sheets are laid out in units and painted at twice that. */
    private static final int S = 2;

    private static BufferedImage skin, glow;
    private static int look;

    public static void main(String[] args) throws Exception {
        File out = new File(args[0]);
        out.mkdirs();
        String[] names = {"stranger", "death", "ragna", "hrafn", "draugr", "gardener", "ayesha", "magus", "spawn"};
        for (look = 0; look < names.length; look++) {
            Sheet s = NorseBoxes.SHEETS[look];
            skin = new BufferedImage(s.width * S, s.height * S, BufferedImage.TYPE_INT_ARGB);
            glow = new BufferedImage(s.width * S, s.height * S, BufferedImage.TYPE_INT_ARGB);
            for (Box b : s.all) {
                box(b);
            }
            ImageIO.write(skin, "png", new File(out, "norse_" + names[look] + ".png"));
            if (look == NorseBoxes.DRAUGR || look == NorseBoxes.GARDENER || look == NorseBoxes.MAGUS || look == NorseBoxes.SPAWN) {
                ImageIO.write(glow, "png", new File(out, "norse_" + names[look] + "_glow.png"));
            }
        }
    }

    // ------------------------------------------------------------------ laying out a box

    private static void box(Box b) {
        int u = b.u * S, v = b.v * S, w = b.w * S, h = b.h * S, d = b.d * S;
        face(b, TOP, u + d, v, w, d);
        face(b, BOTTOM, u + d + w, v, w, d);
        face(b, RIGHT, u, v + d, d, h);
        face(b, FRONT, u + d, v + d, w, h);
        face(b, LEFT, u + d + w, v + d, d, h);
        face(b, BACK, u + d + w + d, v + d, w, h);
    }

    private static void face(Box b, int face, int x0, int y0, int w, int h) {
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int c = paint(b, face, x, y, w, h);
                if (c != -1) {
                    skin.setRGB(x0 + x, y0 + y, c == 0 ? 0 : 0xFF000000 | c);
                }
                int g = glowAt(b, face, x, y);
                if (g != 0) {
                    glow.setRGB(x0 + x, y0 + y, g);
                }
            }
        }
    }

    // ------------------------------------------------------------------ light

    /**
     * How much light a pixel gets: from above and a little in front. Tops brightest, fronts
     * full, sides less, backs and undersides least; and every box darker toward its foot, where
     * the one below crowds it.
     */
    private static float lit(int face, int y, int h) {
        float f;
        switch (face) {
            case TOP:
                return 1F;
            case BOTTOM:
                return -2F;
            case FRONT:
                f = 0.3F;
                break;
            case BACK:
                f = -0.7F;
                break;
            default:
                f = -0.3F;
                break;
        }
        return f + (h > 2 ? 0.6F - 1.3F * y / (h - 1F) : 0F);
    }

    private static int at(int[] pal, float level) {
        int l = Math.round(level);
        return pal[l < 0 ? 0 : l > 6 ? 6 : l];
    }

    // ------------------------------------------------------------------ what everything is

    private static int[] tone() {
        switch (look) {
            case NorseBoxes.STRANGER:
                return PALE;
            case NorseBoxes.DEATH:
                return IVORY;
            case NorseBoxes.RAGNA:
                return FAIR;
            case NorseBoxes.HRAFN:
                return RUDDY;
            case NorseBoxes.GARDENER:
                return MOSS;
            case NorseBoxes.AYESHA:
                return GOLD_SKIN;
            case NorseBoxes.MAGUS:
                return VIOLET;
            case NorseBoxes.SPAWN:
                return SICK_GOLD;
            default:
                return DEAD;
        }
    }

    private static int[] hair() {
        switch (look) {
            case NorseBoxes.STRANGER:
                return DARK_HAIR;
            case NorseBoxes.RAGNA:
                return FAIR_HAIR;
            case NorseBoxes.HRAFN:
                return AUBURN;
            case NorseBoxes.GARDENER:
                return WHITE_HAIR;
            case NorseBoxes.MAGUS:
                return SNOW_HAIR;
            case NorseBoxes.AYESHA:
                return GOLD_HAIR;
            default:
                return GREY_HAIR;
        }
    }

    private static int paint(Box b, int face, int x, int y, int w, int h) {
        int seed = b.u * 131 + b.v * 31 + face * 7;
        float light = lit(face, y, h);
        switch (b.material) {
            case NorseBoxes.FACE:
                return head(b, face, x, y, w, h);
            case NorseBoxes.SKIN:
                return limb(b, face, x, y, w, h, light);
            case NorseBoxes.HAND:
                return hand(b, face, x, y, w, h, light);
            case NorseBoxes.BONE:
                return bone(b, face, x, y, w, h, light);
            case NorseBoxes.NOSE:
                return nose(face, x, y, light);
            case NorseBoxes.LID:
                return y == 0 ? at(tone(), 3.5F) : mix(at(tone(), 1), 0x0C0C10, 0.6F);
            case NorseBoxes.MOUTH:
                return x == 0 || x == w - 1 ? 0x2A0E0C : 0x140606;
            case NorseBoxes.HAIR:
                return strands(hair(), b, face, x, y, w, h, light, seed);
            case NorseBoxes.HAIRPIN:
                return at(SILVER, 4 + light + (x == 1 ? 1 : 0));
            case NorseBoxes.KERCHIEF:
                return cloth(WHITE_LINEN, b, face, x, y, w, h, light + 0.3F, seed, 3, 0);
            case NorseBoxes.DRESS:
                return dress(b, face, x, y, w, h, light, seed);
            case NorseBoxes.APRON:
                return apron(b, face, x, y, w, h, light, seed);
            case NorseBoxes.SLEEVE:
                // Ragna's shift, pushed up above the elbow: linen, rucked.
                return cloth(WHITE_LINEN, b, face, x, y, w, h, light + ((y % 3 == 1) ? -0.8F : 0F), seed, 2, 0);
            case NorseBoxes.BROOCH:
                return brooch(look == NorseBoxes.STRANGER ? SILVER : BRONZE, face, x, y, w, h, light);
            case NorseBoxes.BEADS:
                return beads(x, face);
            case NorseBoxes.MANTLE:
                return mantle(b, face, x, y, w, h, light, seed);
            case NorseBoxes.TUNIC:
                return tunic(b, face, x, y, w, h, light, seed);
            case NorseBoxes.TROUSERS:
                return cloth(DARK_WOOL, b, face, x, y, w, h, light, seed, 3, 0);
            case NorseBoxes.WRAPS:
                return wraps(x, y, light, seed);
            case NorseBoxes.SHOE:
                return shoe(face, x, y, w, h, light, seed);
            case NorseBoxes.BELT:
                return belt(face, x, y, w, h, light);
            case NorseBoxes.KNIFE:
                return y == h - 1 ? at(BRONZE, 4 + light) : y == 0 ? at(WOOD, 4 + light) : at(LEATHER, 3 + light + (y == 1 ? 1 : 0));
            case NorseBoxes.CLOAK:
                return cloak(b, face, x, y, w, h, light, seed);
            case NorseBoxes.PIN:
                return (x + y) % 2 == 0 ? at(IRON, 5 + light) : at(IRON, 2 + light);
            case NorseBoxes.BEARD:
                return beard(b, face, x, y, w, h, light, seed);
            case NorseBoxes.ROBE:
                return robe(b, face, x, y, w, h, light, seed);
            case NorseBoxes.HOOD:
                return hood(b, face, x, y, w, h, light, seed);
            case NorseBoxes.MAIL:
                return mail(b, face, x, y, w, h, light, seed);
            case NorseBoxes.HELM:
                return helm(face, x, y, w, h, light, seed);
            case NorseBoxes.SHROUD:
                return shroud(b, face, x, y, w, h, light, seed);
            case NorseBoxes.TEETH:
                return face == FRONT ? (x % 2 == 0 ? 0xB0A888 : 0x8C846A) : 0x5A5446;
            case NorseBoxes.RIBS:
                return face == FRONT ? (y % 2 == 1 ? at(DEAD, 1 + light) : at(DEAD, 4 + light + (x == w / 2 ? -1 : 0))) : at(DEAD, 3 + light);
            case NorseBoxes.HAFT:
                return at(WOOD, 3 + light + ((x + y) % 3 == 0 ? 1 : 0));
            case NorseBoxes.AXE_HEAD:
                return look == NorseBoxes.DRAUGR ? rusty(x, y, seed, 3 + light) : at(IRON, 3 + light + (face == FRONT && y == h - 1 ? 2 : 0));
            case NorseBoxes.LEAVES:
                return leaves(b, face, x, y, w, h, light, seed);
            case NorseBoxes.BLADE:
                // Steel worn bright with use, brightest at its edge, dull where the soil has stained it.
                return at(SILVER, 3.4F + light * 0.6F + (y == 0 || x == 0 || x == w - 1 ? 1.2F : 0F) - (noise(x, y, seed) > 0.8F ? 1F : 0F));
            case NorseBoxes.GEM:
                return look == NorseBoxes.AYESHA ? (face == FRONT ? 0x6FE8DA : 0x13706A) : look == NorseBoxes.MAGUS
                        ? (face == FRONT ? 0xFFE58A : 0xA87A1E) : gem(x, y, w, h, face);
            case NorseBoxes.SILK:
                return silk(b, face, x, y, w, h, light, seed);
            case NorseBoxes.REGALIA:
                return regalia(b, face, x, y, w, h, light, seed);
            case NorseBoxes.FLESH:
                return flesh(b, face, x, y, w, h, light, seed);
            default:
                return 0xFF00FF;
        }
    }

    // ------------------------------------------------------------------ the Sovereign

    /**
     * Silk: her gold, his white. It falls in long smooth folds and takes the light in streaks;
     * the hems are worked gold. Their capes are lined: teal in hers, his own violet in his.
     */
    private static int silk(Box b, int face, int x, int y, int w, int h, float light, int seed) {
        boolean magus = look == NorseBoxes.MAGUS;
        // His white; her teal, so that the gold of her shows against it.
        int[] pal = magus ? WHITE_SILK : TEAL_SILK;
        if (b.part.startsWith("cloak") && face == FRONT) {
            pal = magus ? VIOLET_SILK : GOLD_SILK;
        }
        if (b.part.startsWith("sleeve") && face == BOTTOM) {
            return at(magus ? VIOLET_SILK : GOLD_SILK, 2);
        }
        boolean hem = (b.part.endsWith("2") && b.part.startsWith("skirt") || b.part.equals("cloak2") || b.part.startsWith("sleeve"))
                && face != TOP && face != BOTTOM && y >= h - 2;
        if (hem) {
            return at(WORKED_GOLD, 4 + light * 0.6F + (y == h - 2 ? 1 : 0) - ((x + y) % 3 == 0 ? 1 : 0));
        }
        float fold = (float) Math.sin((x + seed % 5) * 0.9F + y * 0.05F);
        float l = 3.2F + light * 0.75F + fold * 0.7F;
        // A streak of sheen down each fold's crest.
        if (fold > 0.85F && (face == FRONT || face == BACK || face == LEFT || face == RIGHT)) {
            l += 0.8F;
        }
        // The Magus's sun, on his breast.
        return at(pal, l);
    }

    /**
     * Worked gold: plates, collars, circlets, gauntlets. Bright at every edge, engraved across
     * in fine lines; their collars carry the sunburst rayed out from behind the head.
     */
    private static int regalia(Box b, int face, int x, int y, int w, int h, float light, int seed) {
        // Polished: smooth, a broad highlight across the upper third, a clean bevel at every edge.
        float l = 3.8F + light * 0.6F + (y > 0 && y < h / 3 + 1 ? 0.6F : 0F);
        if (x == 0 || y == 0) {
            l += 1.3F;
        } else if (x == w - 1 || y == h - 1) {
            l -= 1.3F;
        }
        if (b.part.equals("collar") && (face == FRONT || face == BACK)) {
            // Rays out from the bottom middle, where the head stands in front of it.
            float cx = w / 2F - 0.5F, dy = h - y, dx = x - cx;
            double a = Math.atan2(dy, dx);
            boolean ray = Math.abs(Math.sin(a * 9D)) > 0.82D;
            l += ray ? 1F : -0.4F;
            if (Math.sqrt(dx * dx + dy * dy) < 3.2F) {
                l += 1.5F;
            }
            if (y == 0 || x == 0 || x == w - 1) {
                return look == NorseBoxes.AYESHA ? TEAL_SILK[5] : VIOLET[5];
            }
        } else if (b.part.equals("chest") && look == NorseBoxes.MAGUS && face == FRONT && w <= 8) {
            // His sun: a bright disc, and rays.
            float cx = w / 2F - 0.5F, cy = h / 2F - 0.5F, dx = x - cx, dy = y - cy;
            double r = Math.sqrt(dx * dx + dy * dy), a = Math.atan2(dy, dx);
            l = r < 1.8F ? 6F : Math.abs(Math.sin(a * 6D)) > 0.75D ? 5F : 2.6F;
        } else if (w > 4 && h > 4 && (x == 2 || x == w - 3) && y > 1 && y < h - 2) {
            // An engraved line inset from each side.
            l -= 1.1F;
        }
        return at(WORKED_GOLD, l);
    }

    /**
     * What the vats made and threw away: sick gold skin, mottled, stretched over growths; dark
     * veins under it; and wet where it should be dry.
     */
    private static int flesh(Box b, int face, int x, int y, int w, int h, float light, int seed) {
        float l = 3F + light * 0.7F;
        float blot = noise(x / 2, y / 2, seed);
        if (blot > 0.7F) {
            l -= 1.2F;
        } else if (blot < 0.2F) {
            l += 0.6F;
        }
        int c = at(SICK_GOLD, l);
        // Veins, wandering.
        if (Math.abs(Math.sin(x * 0.8F + Math.sin(y * 0.6F + seed) * 2F)) < 0.12F) {
            c = mix(c, 0x3A1A2A, 0.5F);
        }
        if (noise(x, y, seed + 3) > 0.93F) {
            c = mix(c, 0xF4F0C8, 0.6F);
        }
        return c;
    }

    // ------------------------------------------------------------------ heads

    /** The head: hair or a kerchief or bone round it, ears at the sides, and a face on the front. */
    private static int head(Box b, int face, int x, int y, int w, int h) {
        int seed = b.u * 131 + b.v * 31;
        int[] tone = tone();
        float light = lit(face, y, h);
        if (b.part.equals("jaw")) {
            // The dead's jaw: grey lips drawn back, the chin, and its underside.
            if (face == FRONT) {
                return y == 0 ? 0x2A1E1A : at(DEAD, 3 + (x == 0 || x == w - 1 ? -1 : 0));
            }
            return at(DEAD, 2 + light);
        }
        if (look == NorseBoxes.DEATH) {
            if (face == FRONT) {
                return art(SKULL, x, y);
            }
            return at(IVORY, 3 + light + (noise(x, y, seed) > 0.8F ? 1 : 0));
        }
        switch (face) {
            case TOP:
                return strands(hair(), b, face, x, y, w, h, 1F, seed);
            case BACK:
                return strands(hair(), b, face, x, y, w, h, -0.5F, seed);
            case BOTTOM:
                return at(tone, 1);
            case RIGHT:
            case LEFT: {
                int toFront = face == RIGHT ? x : w - 1 - x;
                // An ear, a little behind the middle; hair over the top and back of the side.
                if (y >= 7 && y <= 11 && toFront >= 6 && toFront <= 8 && look != NorseBoxes.DRAUGR) {
                    return (y == 9 && toFront == 7) || toFront == 6 ? at(tone, 2.4F) : at(tone, 3.4F);
                }
                if (y < 4 || toFront < 6 && y < 13 || toFront >= 14 && y < 11) {
                    return strands(hair(), b, face, x, y, w, h, -0.3F, seed);
                }
                // The jaw's line and the hollow of the cheek.
                if (y >= 12 && y <= 13 && toFront > 9) {
                    return at(tone, 2);
                }
                return at(tone, 3.4F - (toFront < 10 ? 0.4F : 0F) + (y < 8 ? 0.3F : 0F) - (y > 13 ? 0.5F : 0F));
            }
            default:
                break;
        }
        switch (look) {
            case NorseBoxes.STRANGER:
                return art(STRANGER_FACE, x, y);
            case NorseBoxes.RAGNA:
                return art(RAGNA_FACE, x, y);
            case NorseBoxes.HRAFN:
                return art(HRAFN_FACE, x, y);
            case NorseBoxes.GARDENER:
                return art(GARDENER_FACE, x, y);
            case NorseBoxes.AYESHA:
                return art(AYESHA_FACE, x, y);
            case NorseBoxes.MAGUS:
                return art(MAGUS_FACE, x, y);
            case NorseBoxes.SPAWN:
                return art(SPAWN_FACE, x, y);
            default:
                return art(DRAUGR_FACE, x, y);
        }
    }

    // Ayesha: gold, flawless, and cold. High arched brows, eyes long and heavy-lidded with teal in
    // them, the cheekbones high and the cheeks hollow under them; a small mouth, darker gold, that
    // has never needed to smile.
    private static final String[] AYESHA_FACE = {
            "hhhhhhhHHhhhhhhh",
            "hhhhhHHHHHHhhhhh",
            "hhhsSSSSSSSSshhh",
            "hhsSbbSSSSbbSshh",
            "hdsbSSbSSbSSbsdh",
            "hdsSSSSSSSSSSsdh",
            "hdsllllSSllllsdh",
            "hdswTPwSSwPTwsdh",
            "hdsuuusSSsuuusdh",
            "hdcsSSssssSSscdh",
            "hdcsSSsnnsSSscdh",
            "hdcsssssssssscdh",
            "jdssssmmmmssssdj",
            "jjdsssMMMMsssdjj",
            "jjjdssssssssdjjj",
            "jjjjdddssdddjjjj"};
    // The Magus: Warlock's face, older, violet, and certain. Heavy level brows, the eyes white and
    // gold with no warmth in them, deep lines from the nose to the mouth, a mouth set thin. His
    // hair thrown back off a high forehead.
    private static final String[] MAGUS_FACE = {
            "hhhhhhhHHhhhhhhh",
            "hhhsSSSSSSSSshhh",
            "hhsSSSSSSSSSSshh",
            "hdsSSSSSSSSSSsdh",
            "hdsSSccSSccSSsdh",
            "hdbbbbbSSbbbbbdh",
            "hdsllllSSllllsdh",
            "hdswGGwSSwGGwsdh",
            "hdsddddSSddddsdh",
            "hdcsSSssssSSscdh",
            "hdcsSSsnnsSSscdh",
            "hdcsssssssssscdh",
            "jdcsssmmmmssscdj",
            "jjdcssMMMMsscdjj",
            "jjjdssssssssdjjj",
            "jjjjdddssdddjjjj"};
    // What came out of a vat: the face of something meant to be a man. One eye too low, a
    // second socket where none should be; no nose to speak of; a mouth that never closed.
    private static final String[] SPAWN_FACE = {
            "ssssssssssssssss",
            "sssdssssssssdsss",
            "sdssccssssccssds",
            "sdsssssssssssssd",
            "sdssxxssssssssds",
            "sdsxPxsssxxxsssd",
            "sdssxxsssxPxxssd",
            "sdsssssssxxxsssd",
            "sdccsssnnsssccsd",
            "sdcssssssssssscd",
            "sdcssssssssssscd",
            "sdccsssssssssccd",
            "vvvvvvvvvvvvvvvv",
            "vtvvtvvvvvtvvtvv",
            "vvvvvvvvvvvvvvvv",
            "jjvvvvvvvvvvvvjj"};

    // Faces, sixteen by sixteen (two texels to a unit), the left of the picture being their right.
    // h hair, H its light, p the parting, k kerchief, K its shade, s/S skin and its light, d shadow, c hollow or line,
    // b brow, l the lash line, w white of the eye, I iris, E a blue iris, P pupil, u the heaviness under an eye,
    // n the nose's shadow, m upper lip, M lower lip, f freckle, r colour in the cheek, a beard, g grey in it,
    // x socket, v dark gap, t tooth, j jaw's shadow, o skull shadow, i ivory, the dot '.' nothing at all.
    private static final String[] STRANGER_FACE = {
            "hhhhhhhHHhhhhhhh",
            "hhhhhhHppHhhhhhh",
            "hhhhhHpSSpHhhhhh",
            "hhhhsSSSSSSshhhh",
            "hhhsSSSSSSSSshhh",
            "hhdsSSSSSSSSsdhh",
            "hdsbbbbSSbbbbsdh",
            "hdsllllsSllllsdh",
            "hdswIPwsSwPIwsdh",
            "hdsddddsSddddsdh",
            "hdssSSssssSSssdh",
            "hcdsSSsnnsSSsdch",
            "jcdssrsmmsrssdcj",
            "jjdsssMMMMsssdjj",
            "jjjdssssssssdjjj",
            "jjjjdddssdddjjjj"};
    private static final String[] RAGNA_FACE = {
            "kkkkkkkkkkkkkkkk",
            "kkkkkkkkkkkkkkkk",
            "kKkkkkkkkkkkkkKk",
            "kkHHSSSSSSSSHHkk",
            "kHSSSSSSSSSSSSHk",
            "kdSSSSSSSSSSSSdk",
            "kdsbbbbSSbbbbsdk",
            "kdsllllsSllllsdk",
            "kdswEPwsSwPEwsdk",
            "kdsdddssSsdddsdk",
            "kdsfSfssssfSfsdk",
            "kdsrSfsnnsfSrsdk",
            "jdssrssmmssrssdj",
            "jjdsssMMMMsssdjj",
            "jjjdssssssssdjjj",
            "jjjjdddssdddjjjj"};
    private static final String[] HRAFN_FACE = {
            "hhhhhhhhhhhhhhhh",
            "hhhhHhhhhhhHhhhh",
            "hhhsShhhhhhSshhh",
            "hhsSSSSSSSSSSshh",
            "hdSSccSSSSccSSdh",
            "hdSSSSSSSSSSSSdh",
            "hdbbbbbSSbbbbbdh",
            "hdsllllsSllllsdh",
            "hdswIPwsSwPIwsdh",
            "hduuuuusSuuuuudh",
            "hdssSSssssSSssdh",
            "haasSSsnnsSSsaah",
            "aaaaammmmmmaaaaa",
            "aaaaaMMMMMMaaaaa",
            "aaagaaaaaaaagaaa",
            "aaaaagaaaagaaaaa"};
    private static final String[] DRAUGR_FACE = {
            "hhhhhhhhhhhhhhhh",
            "hhhhhhhhhhhhhhhh",
            "hhdssssssssssdhh",
            "hdssccssssccssdh",
            "hdssssssssssssdh",
            "hdcssssssssssscd",
            "hdxxxxxsSxxxxxdh",
            "hxxxxxxsSxxxxxxh",
            "hxxxPxxsSxxPxxxh",
            "hdxxxxxssxxxxxdh",
            "hdccsssssssccsdh",
            "hdcsssssssssscdh",
            "vvvvvvvvvvvvvvvv",
            "vvvvvvvvvvvvvvvv",
            "vtvtvvvvvvvvtvtv",
            "vvvvvvvvvvvvvvvv"};
    // The Gardener: lined, kind, the brows heavy and white, the eyes gold-green and creased at the
    // corners from smiling; a white moustache over the lip, the beard from the cheekbones down.
    private static final String[] GARDENER_FACE = {
            "hhhhhhhhhhhhhhhh",
            "hhhhhHhhhhhHhhhh",
            "hhhsSSSSSSSSshhh",
            "hhsSScSSSScSSshh",
            "hdSSSSccccSSSSdh",
            "hdbbbbbSSbbbbbdh",
            "hcbllllsSllllbch",
            "hdswIPwsSwPIwsdh",
            "hcuuuussSsuuuuch",
            "hdssSSssssSSssdh",
            "hcasSSsnnsSSsach",
            "aaaaammmmmmaaaaa",
            "aaaammMMMMmmaaaa",
            "aagaaaaaaaaaagaa",
            "aaaaagaaaagaaaaa",
            "aaaaaaaaaaaaaaaa"};
    private static final String[] SKULL = {
            "ooiiiiIIIIiiiioo",
            "oiiiIIIIIIIIiiio",
            "oiiIIIIIIIIIIiio",
            "oiiIIIIIIIIIIiio",
            "oiiiiIIIIIIiiiio",
            "ojxxxxiIIixxxxjo",
            "jxxxxxxiixxxxxxj",
            "jxxvvxxiixxvvxxj",
            "jxxxxxxIIxxxxxxj",
            "joxxxxoiioxxxxoj",
            "jIiooiiiiiiooiIj",
            "jjIiiiivviiiiIjj",
            "jjiiiiivviiiiijj",
            "jjjtltltltltljjj",
            "jjjltltltltltjjj",
            "jjjjiiiiiiiijjjj"};

    private static int art(String[] rows, int x, int y) {
        if (rows[y].length() != 16) {
            throw new IllegalStateException("A face row is not sixteen wide: " + rows[y]);
        }
        int[] tone = tone();
        int[] hair = hair();
        char c = rows[y].charAt(x);
        if (look == NorseBoxes.DEATH) {
            // A skull: ivory, its light and shadow, the sockets dark as the hood, teeth and the lines between them.
            switch (c) {
                case 'I':
                    return IVORY[4];
                case 'i':
                    return IVORY[3];
                case 'o':
                    return IVORY[1];
                case 'j':
                    return IVORY[1];
                case 'x':
                    return HOOD_IN[2];
                case 'v':
                    return HOOD_IN[0];
                case 't':
                    return IVORY[5];
                case 'l':
                    return IVORY[1];
                default:
                    return 0xFF00FF;
            }
        }
        switch (c) {
            case 'h':
                return hair[2 + (x * 3 + y) % 2];
            case 'H':
                return hair[4];
            case 'p':
                return tone[3];
            case 'k':
                return WHITE_LINEN[4 + (x / 2) % 2];
            case 'K':
                return WHITE_LINEN[3];
            case 's':
                return tone[4];
            case 'S':
                return tone[5];
            case 'd':
                return tone[3];
            case 'c':
            case 'j':
                return tone[2];
            case 'r':
                return mix(tone[4], 0xC06A62, look == NorseBoxes.STRANGER ? 0.08F : 0.28F);
            case 'b':
                return look == NorseBoxes.RAGNA ? FAIR_HAIR[2] : look == NorseBoxes.GARDENER ? WHITE_HAIR[4 + (x % 2)] : hair[1];
            case 'l':
                return mix(tone[1], 0x0C0C10, 0.7F);
            case 'w':
                return look == NorseBoxes.HRAFN ? 0xD4CCC0 : look == NorseBoxes.GARDENER ? 0xDCD8C4 : 0xE6E4E0;
            case 'I':
                return look == NorseBoxes.HRAFN ? 0x5C6C42 : look == NorseBoxes.GARDENER ? 0xA8A43A : 0x6E7884;
            case 'E':
                return 0x4A86B4;
            case 'T':
                return 0x2FB8A8;
            case 'G':
                return 0xF6D46A;
            case 'P':
                return look == NorseBoxes.DRAUGR ? 0xC8DCE6 : look == NorseBoxes.SPAWN ? 0xF0C850 : 0x121418;
            case 'u':
                return look == NorseBoxes.GARDENER ? mix(tone[3], 0x20301A, 0.35F) : mix(tone[3], 0x6A4A5A, 0.28F);
            case 'n':
                return tone[2];
            case 'm':
                if (look == NorseBoxes.AYESHA || look == NorseBoxes.MAGUS) {
                    return tone[1];
                }
                return look == NorseBoxes.STRANGER ? 0x80605E : look == NorseBoxes.RAGNA ? 0xAC6A62 : look == NorseBoxes.GARDENER
                        ? WHITE_HAIR[5 - (x + y) % 2] : AUBURN[2];
            case 'M':
                if (look == NorseBoxes.AYESHA || look == NorseBoxes.MAGUS) {
                    return tone[2];
                }
                return look == NorseBoxes.STRANGER ? 0x987270 : look == NorseBoxes.RAGNA ? 0xC48276 : look == NorseBoxes.GARDENER ? 0x4A3A2C : 0x8E5A44;
            case 'f':
                return FAIR[2];
            case 'a':
                return look == NorseBoxes.GARDENER ? WHITE_HAIR[4 + ((x / 2 + y) % 3 == 0 ? 1 : 0)] : AUBURN[3 + ((x / 2 + y) % 3 == 0 ? 1 : 0)];
            case 'g':
                return look == NorseBoxes.GARDENER ? WHITE_HAIR[2] : GREY_HAIR[4];
            case 'x':
                return look == NorseBoxes.SPAWN ? 0x1A1408 : 0x07080A;
            case 'v':
                return y % 2 == 0 ? 0x2A1210 : 0x1C0A08;
            case 't':
                return 0x9A947C;
            default:
                return 0xFF00FF;
        }
    }

    /** The nose: lit along its ridge, shadowed under it. */
    private static int nose(int face, int x, int y, float light) {
        int[] tone = look == NorseBoxes.DEATH ? HOOD_IN : tone();
        if (face == BOTTOM) {
            return at(tone, 1.5F);
        }
        return at(tone, 3.3F + light * 0.4F + (face == FRONT && x == 0 ? 0.4F : 0F));
    }

    // ------------------------------------------------------------------ hands and limbs

    /** Bare skin on an arm or leg: Hrafn's weathered forearms; the dead's skin gone to grey, rotted wool clinging. */
    private static int limb(Box b, int face, int x, int y, int w, int h, float light) {
        int[] tone = tone();
        if (look == NorseBoxes.DRAUGR && y < h - 2 && noise(x, y, b.u) > 0.35F) {
            return at(ROT, 2 + light);
        }
        float l = 3.6F + light * 0.7F;
        if (look == NorseBoxes.HRAFN && noise(x, y, b.v) > 0.82F) {
            l -= 0.6F;
        }
        if (look == NorseBoxes.GARDENER && b.part.startsWith("foot")) {
            // Bare feet, long-toed like roots: the toes parted along the top and the front, the soles dark with earth.
            if (face == BOTTOM) {
                return at(BARK, 2);
            }
            if ((face == TOP && y < 4 || face == FRONT) && x % 2 == 1) {
                return at(tone, 1.8F);
            }
            if (face == FRONT && y == h - 1) {
                return mix(at(tone, 2), BARK[2], 0.5F);
            }
            l -= 0.3F;
        }
        return at(tone, l);
    }

    /** Hands: a little lighter on the backs, knuckles across them, nails at the fingertips. */
    private static int hand(Box b, int face, int x, int y, int w, int h, float light) {
        int[] tone = tone();
        boolean fingers = b.part.startsWith("fingers");
        if (look == NorseBoxes.STRANGER && b.part.startsWith("hand") && y == 0) {
            // Her cuff, black, at the wrist.
            return at(BLACK_WOOL, 3 + light);
        }
        if (fingers && face != TOP && face != BOTTOM && (x % 2 == 1 && (face == FRONT || face == BACK))) {
            return at(tone, 2.6F + light * 0.5F);
        }
        if (fingers && y == h - 1 && (face == FRONT || face == BACK)) {
            return look == NorseBoxes.HRAFN ? at(tone, 2.4F) : mix(at(tone, 4), 0xE8D8D0, 0.3F);
        }
        if (!fingers && y == h - 1 && face == BACK) {
            return at(tone, 2.8F);
        }
        return at(tone, 3.7F + light * 0.6F);
    }

    /** Bone: ivory, pale on the joints, a hairline of shadow between each. */
    private static int bone(Box b, int face, int x, int y, int w, int h, float light) {
        if (b.part.startsWith("hand") && y == 0) {
            return HOOD_IN[3];
        }
        boolean joint = b.part.startsWith("fingers") ? y == 0 : y % 2 == 1;
        boolean gap = (face == FRONT || face == BACK) && x % 2 == 1;
        if (gap) {
            return at(IVORY, 1 + light * 0.5F);
        }
        return at(IVORY, (joint ? 4.6F : 3.6F) + light * 0.6F);
    }

    // ------------------------------------------------------------------ hair

    /** Hair in strands down the box, a sheen across it a third of the way down, dark at the roots and the ends. */
    private static int strands(int[] pal, Box b, int face, int x, int y, int w, int h, float light, int seed) {
        float l = 3F + light * 0.5F;
        float strand = noise(x, 0, seed);
        l += strand > 0.7F ? 0.8F : strand < 0.25F ? -0.8F : 0F;
        if (h > 3 && face != TOP) {
            float k = y / (h - 1F);
            l += k < 0.35F ? (k > 0.15F ? 0.9F : 0.2F) : -k * 0.9F;
        }
        if (b.part.startsWith("braid")) {
            // A plait: the strands cross, left over right.
            l += ((x + y) % 2 == 0 ? 0.7F : -0.5F);
            if (b.part.equals("braid" + (NorseBoxes.BRAID - 1)) && y >= h - 1 && look == NorseBoxes.RAGNA) {
                return RED;
            }
        }
        if (look == NorseBoxes.DRAUGR) {
            // Matted, dull, with grave-dirt in it.
            if (noise(x, y, seed + 3) > 0.75F) {
                return at(ROT, 2 + light);
            }
            l -= 0.4F;
        }
        if (look == NorseBoxes.GARDENER && face != TOP && noise(x / 2, y / 2, seed + 21) > 0.975F) {
            // A leaf caught in it.
            return at(LEAF, 3 + light * 0.5F + (x % 2));
        }
        if (look == NorseBoxes.STRANGER && face == TOP && x == w / 2 && b.part.equals("head")) {
            // Her parting, a hairline of skin toward the front.
            if (y < h / 2) {
                return at(DARK_HAIR, 5);
            }
        }
        return at(pal, l);
    }

    private static int beard(Box b, int face, int x, int y, int w, int h, float light, int seed) {
        if (look == NorseBoxes.GARDENER) {
            if (noise(x / 2, y / 2, seed + 5) > 0.95F) {
                return at(LEAF, 3 + light * 0.4F);
            }
            // Long and soft: wavy strands parting and rejoining down it, the ends thinning to a point.
            boolean end = b.part.equals("beard1");
            if (end && face != TOP && y >= h - 2 && (x == 0 || x == w - 1 || y == h - 1 && (x == 1 || x == w - 2))) {
                return 0;
            }
            float wave = (float) Math.sin(x * 1.7F + y * 0.6F + (end ? 1.3F : 0F));
            float l = 3.7F + light * 0.4F + wave * 0.6F - (end ? y * 0.12F : 0F) - ((x + y / 3) % 3 == 0 ? 0.9F : 0F);
            return at(WHITE_HAIR, l);
        }
        int[] pal = look == NorseBoxes.HRAFN ? AUBURN : GREY_HAIR;
        float l = 3.2F + light * 0.4F + (noise(x, 0, seed) > 0.7F ? 0.6F : 0F) - y * 0.08F;
        if (look == NorseBoxes.HRAFN && noise(x / 2, y / 2, seed + 9) > (b.part.equals("beard1") ? 0.82F : 0.94F)) {
            // Going grey, at the chin first.
            return at(GREY_HAIR, l + 0.5F);
        }
        return at(pal, l);
    }

    // ------------------------------------------------------------------ cloth

    /**
     * Wool or linen: folds running down it (lighter on the crest, darker in the trough), a fine
     * weave, and the hem a little darker where it drags.
     */
    private static int cloth(int[] pal, Box b, int face, int x, int y, int w, int h, float light, int seed, int base, int band) {
        float l = base + light * 0.8F;
        if (face == FRONT || face == BACK || face == LEFT || face == RIGHT) {
            float fold = (float) Math.sin((x + seed % 7) * 1.7F + y * 0.15F);
            l += fold * 0.55F;
        }
        if (((x + y * 3) & 3) == 0) {
            l -= 0.35F;
        }
        if (noise(x, y, seed) > 0.85F) {
            l += 0.5F;
        }
        return at(pal, l);
    }

    private static boolean hemRow(Box b, int face, int y, int h) {
        return face != TOP && face != BOTTOM && y >= h - 2;
    }

    private static boolean lowerPanel(Box b) {
        return b.part.startsWith("skirt") && b.part.endsWith("2");
    }

    /** The gown: black wool for her, woad for Ragna, with a woven band round the very bottom. */
    private static int dress(Box b, int face, int x, int y, int w, int h, float light, int seed) {
        boolean hem = lowerPanel(b) && hemRow(b, face, y, h);
        if (look == NorseBoxes.STRANGER) {
            if (hem && y == h - 2) {
                return at(SILVER, 2 + ((x % 3 == 0) ? 1 : 0));
            }
            return cloth(BLACK_WOOL, b, face, x, y, w, h, light, seed, 3, 0);
        }
        if (hem) {
            return tablet(x, y == h - 1);
        }
        return cloth(WOAD, b, face, x, y, w, h, light, seed, 3, 0);
    }

    /** A tablet-woven band: red, gold and dark in small diamonds. */
    private static int tablet(int x, boolean lower) {
        int k = (x + (lower ? 1 : 0)) % 4;
        return k == 0 ? GOLD : k == 2 ? GOLD_L : lower ? RED : RED_L;
    }

    /** The apron-dress: its edges bound, the top edge in a band; for her charcoal, for Ragna linen. */
    private static int apron(Box b, int face, int x, int y, int w, int h, float light, int seed) {
        boolean stranger = look == NorseBoxes.STRANGER;
        int[] pal = stranger ? CHARCOAL : LINEN;
        boolean top = face == FRONT && y == 0 && (b.part.equals("chest") || b.part.equals("abdomen"));
        boolean edge = face == FRONT && (x == 0 || x == w - 1) && b.part.startsWith("apron");
        if (top) {
            return stranger ? at(SILVER, 2) : tablet(x, false);
        }
        if (edge) {
            return stranger ? at(CHARCOAL, 1) : at(WOAD, 3);
        }
        if (b.part.equals("apron2") && y >= h - 1 && face != TOP) {
            return stranger ? at(SILVER, 1 + x % 2) : tablet(x, true);
        }
        return cloth(pal, b, face, x, y, w, h, light, seed, stranger ? 3 : 4, 0);
    }

    /** Her mantle: heavier wool, a fringe at its foot. */
    private static int mantle(Box b, int face, int x, int y, int w, int h, float light, int seed) {
        if (b.part.equals("cloak" + (NorseBoxes.CLOAK_SEGMENTS - 1)) && y >= h - 1 && face != TOP) {
            return x % 2 == 0 ? 0 : at(BLACK_WOOL, 2);
        }
        return cloth(BLACK_WOOL, b, face, x, y, w, h, light - 0.2F, seed, 3, 0);
    }

    /** Hrafn's tunic: brown wool, a green and red band at the hem and the cuffs. */
    private static int tunic(Box b, int face, int x, int y, int w, int h, float light, int seed) {
        boolean band = b.part.startsWith("skirt") && hemRow(b, face, y, h) || b.part.startsWith("fore") && y >= h - 1 && face != BOTTOM;
        if (band) {
            int k = x % 3;
            return y == h - 1 ? (k == 1 ? RED : GREEN) : (k == 0 ? RED_L : GREEN_L);
        }
        // The neck opening, at the top of the chest.
        if (b.part.equals("chest") && face == FRONT && y < 2 && x >= w / 2 - 1 && x <= w / 2) {
            return at(RUDDY, 3);
        }
        return cloth(BROWN_WOOL, b, face, x, y, w, h, light, seed, 3, 0);
    }

    private static int cloak(Box b, int face, int x, int y, int w, int h, float light, int seed) {
        boolean border = face != TOP && face != BOTTOM && (b.part.equals("cloak2") && y >= h - 2 || (x == 0 || x == w - 1) && b.part.startsWith("cloak"));
        if (border) {
            return at(DARK_WOOL, 2 + light * 0.5F);
        }
        return cloth(GREY_WOOL, b, face, x, y, w, h, light, seed, 3, 0);
    }

    /** Her robe: so black it barely takes the light; where it does, a faint violet along the folds. */
    private static int robe(Box b, int face, int x, int y, int w, int h, float light, int seed) {
        if (look == NorseBoxes.GARDENER) {
            return barkCloth(b, face, x, y, w, h, light, seed);
        }
        if (b.part.startsWith("sleeve") && face == BOTTOM) {
            return HOOD_IN[0];
        }
        float fold = (float) Math.sin((x + seed % 5) * 1.3F + y * 0.1F);
        float l = 2.6F + light * 0.7F + fold * 0.8F;
        if (b.part.startsWith("sleeve") && (face == FRONT || face == BACK || face == LEFT || face == RIGHT)) {
            l += 0.3F;
        }
        return at(DEEP_BLACK, l);
    }

    /** The hood: outside like the robe; inside, the faces that look in, dark as a well. */
    private static int hood(Box b, int face, int x, int y, int w, int h, float light, int seed) {
        boolean inside = b.part.equals("brim") ? face == BOTTOM || face == BACK
                : b.w == 1 ? (b.x < 0 ? face == LEFT : face == RIGHT) : b.d == 1 && b.z > 0 ? face == FRONT : face == BOTTOM;
        if (inside) {
            return at(HOOD_IN, 1 + (y < 2 ? 1 : 0));
        }
        return robe(b, face, x, y, w, h, light, seed);
    }

    /**
     * The Gardener's robe: undyed bark-cloth, beaten, so its grain runs down it in long fibres;
     * at the hem and the cuffs a vine stitched in green, its leaves alternating.
     */
    private static int barkCloth(Box b, int face, int x, int y, int w, int h, float light, int seed) {
        boolean side = face != TOP && face != BOTTOM;
        boolean hem = side && (lowerPanel(b) && y >= h - 3 || b.part.startsWith("sleeve") && y >= h - 2);
        if (b.part.startsWith("sleeve") && face == BOTTOM) {
            return at(BARK, 0);
        }
        if (hem) {
            int row = y - (h - 3);
            boolean stem = row == 1 && b.part.startsWith("skirt") || row == 0 && b.part.startsWith("sleeve");
            boolean leaf = (x % 4 == 1 && row == 0) || (x % 4 == 3 && row == 2);
            if (stem) {
                return LEAF[3];
            }
            if (leaf) {
                return LEAF[5];
            }
        }
        float l = 3F + light * 0.7F;
        // Fibres running down: long streaks, each a little lighter or darker than the next.
        l += (noise(x, y / 5, seed) - 0.5F) * 1.1F;
        if (side) {
            l += (float) Math.sin((x + seed % 5) * 1.4F + y * 0.12F) * 0.45F;
        }
        if (noise(x, y, seed + 3) > 0.9F) {
            l -= 0.7F;
        }
        return at(BARK, l);
    }

    /**
     * Leaves, overlapping, each lit along one edge; small white flowers among them on the
     * circlet and the shoulders. The circlet is a ring, open on top and underneath; the mantle's
     * last edge is ragged, leaves hanging off it.
     */
    private static int leaves(Box b, int face, int x, int y, int w, int h, float light, int seed) {
        if (b.part.equals("head") && (face == TOP || face == BOTTOM) && x > 2 && x < w - 3 && y > 2 && y < h - 3) {
            return 0;
        }
        if (b.part.equals("cloak" + (NorseBoxes.CLOAK_SEGMENTS - 1)) && face != TOP && y >= h - 2
                && noise(x, y, seed) > (y == h - 1 ? 0.45F : 0.8F)) {
            return 0;
        }
        // Each leaf a lozenge three texels across; its upper edge catches the light.
        int lx = (x + (y / 3) % 2 * 2) / 3, ly = y / 3;
        float shade = noise(lx, ly, seed);
        int inLeafY = y % 3;
        float l = 2.6F + light * 0.6F + shade * 1.6F + (inLeafY == 0 ? 0.8F : inLeafY == 2 ? -0.6F : 0F);
        boolean flowered = b.part.equals("head") || b.part.equals("chest");
        if (flowered && noise(x, y, seed + 17) > 0.93F) {
            return noise(x, y, seed + 19) > 0.5F ? 0xF2F0E2 : 0xE6D26A;
        }
        return at(LEAF, l);
    }

    /** The Time Stone, as it shows in his chest: deep green at the edge, bright as new leaves at the heart. */
    private static int gem(int x, int y, int w, int h, int face) {
        if (face != FRONT) {
            return 0x1E6A26;
        }
        float cx = (w - 1) / 2F, cy = (h - 1) / 2F;
        float d = Math.abs(x - cx) / Math.max(1F, cx) + Math.abs(y - cy) / Math.max(1F, cy);
        return d < 0.6F ? 0xC8FFB8 : d < 1.1F ? 0x5ADA5A : 0x1E8A2A;
    }

    private static int brooch(int[] pal, int face, int x, int y, int w, int h, float light) {
        if (face != FRONT) {
            return at(pal, 2 + light);
        }
        // An oval: the corners cut away to the cloth, a rim, a boss in the middle catching the light.
        boolean corner = (x == 0 || x == w - 1) && (y == 0 || y == h - 1);
        if (corner) {
            return look == NorseBoxes.STRANGER ? at(CHARCOAL, 2) : at(LINEN, 3);
        }
        boolean rim = y == 0 || y == h - 1 || x == 0 || x == w - 1;
        if (rim) {
            return at(pal, y == 0 ? 4 : 3);
        }
        return (x + y) % 2 == 0 ? at(pal, 5) : at(pal, 2);
    }

    private static int beads(int x, int face) {
        if (look == NorseBoxes.STRANGER) {
            return x % 2 == 0 ? JET(5) : JET(1);
        }
        int[] glass = {0x2E6A8E, 0xC09030, 0x8A2A22, 0x2E6A8E, 0xD8C8A0, 0x3A7A3A};
        return glass[x % glass.length];
    }

    private static int JET(int l) {
        int[] jet = {0x020203, 0x060608, 0x0C0C10, 0x15151B, 0x22222A, 0x40404E, 0x707082};
        return jet[l];
    }

    /** Leg wraps: bands of wool wound on the bias, light and dark. */
    private static int wraps(int x, int y, float light, int seed) {
        boolean stripe = ((x + y * 2) % 4) < 2;
        if (look == NorseBoxes.DRAUGR) {
            return at(ROT, (stripe ? 3 : 2) + light);
        }
        return stripe ? at(GREY_WOOL, 3 + light) : at(DARK_WOOL, 3 + light);
    }

    /** Turnshoes: leather, the seam down the instep, a toggle at the ankle. */
    private static int shoe(int face, int x, int y, int w, int h, float light, int seed) {
        int[] pal = look == NorseBoxes.DEATH ? DEEP_BLACK : LEATHER;
        if (face == TOP && x == w / 2) {
            return at(pal, 1);
        }
        return at(pal, 3 + light * 0.8F + (noise(x, y, seed) > 0.85F ? 0.6F : 0F));
    }

    /** A belt, its buckle and the strap's end hanging from it. */
    private static int belt(int face, int x, int y, int w, int h, float light) {
        if (face == FRONT && x >= w / 2 - 1 && x <= w / 2) {
            return at(BRONZE, y == 0 ? 6 : 4);
        }
        if (face == FRONT && x == w / 2 + 2 && look == NorseBoxes.HRAFN) {
            return at(BRONZE, 3);
        }
        if (look == NorseBoxes.GARDENER) {
            // Plaited grass: green and straw, crossing.
            return ((x + y) % 2 == 0 ? at(LEAF, 3 + light * 0.5F) : at(STRAW, 3 + light * 0.5F + (x % 3 == 0 ? 1 : 0)));
        }
        return at(look == NorseBoxes.DRAUGR ? ROT : LEATHER, 3 + light * 0.6F + (y == 0 ? 0.6F : 0F));
    }

    // ------------------------------------------------------------------ the dead

    /** Mail: rings catching the light, rust spreading, and where it has rotted through, the wool or the ribs. */
    private static int mail(Box b, int face, int x, int y, int w, int h, float light, int seed) {
        if (noise(x / 2, y / 2, seed + 7) > 0.78F) {
            return b.part.equals("chest") ? at(DEAD, 2 + light) : at(ROT, 2 + light);
        }
        boolean ring = (x + (y % 2)) % 2 == 0;
        float r = noise(x / 3, y / 3, seed) * 0.75F + noise(x, y, seed) * 0.25F;
        if (r > 0.55F) {
            return at(RUST, (ring ? 4 : 2) + light);
        }
        return at(IRON, (ring ? 4 : 1) + light);
    }

    private static int helm(int face, int x, int y, int w, int h, float light, int seed) {
        float l = 3 + light;
        if (face == FRONT && y == h - 1) {
            l -= 1;
        }
        return rusty(x, y, seed, l);
    }

    private static int rusty(int x, int y, int seed, float level) {
        // Rust in patches a few texels across, not speckle: iron showing through where it has worn.
        float r = noise(x / 3, y / 3, seed) * 0.7F + noise(x, y, seed) * 0.3F;
        return r > 0.62F ? at(IRON, level - 0.6F) : at(RUST, level - 0.3F + (r < 0.2F ? -0.6F : 0F));
    }

    /** Grave-cloth: rotted wool with holes worn through the hems. */
    private static int shroud(Box b, int face, int x, int y, int w, int h, float light, int seed) {
        boolean edge = b.part.startsWith("cloak") || b.part.startsWith("skirt");
        if (edge && face != TOP && y >= h - 3 && noise(x, y, seed) > (y == h - 1 ? 0.35F : 0.72F)) {
            return 0;
        }
        if (edge && face != TOP && noise(x, y, seed + 11) > 0.9F) {
            return 0;
        }
        return cloth(ROT, b, face, x, y, w, h, light, seed, 3, 0);
    }

    /** What lights from within: the pale points of the dead's eyes, and a little round them. */
    private static int glowAt(Box b, int face, int x, int y) {
        if (look == NorseBoxes.GARDENER) {
            return gardenerGlow(b, face, x, y);
        }
        if (look == NorseBoxes.MAGUS) {
            // The gem in his brow, and his eyes, which give off the same cold gold.
            if (b.material == NorseBoxes.GEM) {
                return face == FRONT ? 0xFFFFF0B0 : 0x80C89A30;
            }
            if (b.material == NorseBoxes.FACE && face == FRONT && b.part.equals("head") && MAGUS_FACE[y].charAt(x) == 'G') {
                return 0xFFFFE89A;
            }
            return 0;
        }
        if (look == NorseBoxes.SPAWN) {
            if (b.material == NorseBoxes.FACE && face == FRONT && b.part.equals("head") && SPAWN_FACE[y].charAt(x) == 'P') {
                return 0xFFFFD860;
            }
            return 0;
        }
        if (look != NorseBoxes.DRAUGR || b.material != NorseBoxes.FACE || face != FRONT || !b.part.equals("head")) {
            return 0;
        }
        char c = DRAUGR_FACE[y].charAt(x);
        if (c == 'P') {
            return 0xFFD8F0FF;
        }
        if (c == 'x') {
            return 0x5068A8C8;
        }
        return 0;
    }

    /**
     * The stone itself, white-green at its heart; and on the robe over his chest, the roots that
     * run out from it under the skin: seven of them, wandering, thinning, fading as they go.
     */
    private static int gardenerGlow(Box b, int face, int x, int y) {
        if (b.material == NorseBoxes.GEM) {
            return face == FRONT ? 0xFFB8FFA8 : 0x8048C048;
        }
        if (!b.part.equals("chest") || b.material != NorseBoxes.ROBE || face != FRONT) {
            return 0;
        }
        // Where the stone sits on the chest's front, in texels.
        float cx = b.w * S / 2F - 0.5F, cy = 2.6F * S;
        float dx = x - cx, dy = y - cy;
        float r = (float) Math.sqrt(dx * dx + dy * dy);
        if (r < 1.6F || r > 9.5F) {
            return 0;
        }
        double theta = Math.atan2(dy, dx);
        double[] veins = {0.35, 1.2, 1.95, 2.75, 3.55, 4.6, 5.55};
        for (int i = 0; i < veins.length; i++) {
            double a = veins[i] + 0.22 * Math.sin(r * 0.8 + i * 1.7);
            double off = Math.abs(Math.IEEEremainder(theta - a, Math.PI * 2)) * r;
            if (off < 0.55) {
                int alpha = (int) (220 * (1F - r / 10F));
                return alpha << 24 | 0x6CF06A;
            }
        }
        return 0;
    }

    // ------------------------------------------------------------------ helpers

    private static int mix(int a, int b, float k) {
        int r = (int) (((a >> 16) & 255) * (1 - k) + ((b >> 16) & 255) * k);
        int g = (int) (((a >> 8) & 255) * (1 - k) + ((b >> 8) & 255) * k);
        int bl = (int) ((a & 255) * (1 - k) + (b & 255) * k);
        return r << 16 | g << 8 | bl;
    }

    private static float noise(int x, int y, int seed) {
        int n = x * 374761393 + y * 668265263 + seed * 1274126177;
        n = (n ^ (n >>> 13)) * 1274126177;
        n ^= n >>> 16;
        return (n & 0xFFFF) / 65535F;
    }
}
