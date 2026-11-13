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
    // Death, beguiling: moonlit skin with a little rose in it, blue-black hair, black silk with crimson moving in it.
    private static final int[] RAVEN = {0x020205, 0x06060C, 0x0C0C16, 0x141422, 0x1E1E30, 0x2A2A42, 0x3C3C5A};
    private static final int[] BLACK_SILK = {0x020203, 0x050508, 0x09090E, 0x0F0F16, 0x181822, 0x242432, 0x363648};
    // Death, as you'd want her: warm skin, black leather.
    private static final int[] PEACH = {0x6A4A40, 0x9A6E60, 0xC49482, 0xE0B6A0, 0xF0CEBA, 0xF8E0D0, 0xFFF0E6};
    private static final int[] LEATHER_BLACK = {0x050404, 0x0A0808, 0x110E0D, 0x191514, 0x231E1C, 0x2F2826, 0x3E3532};

    /** Texels to a unit of the model: the sheets are laid out in units and painted at twice that; hers at four times. */
    private static int S = 2;

    private static BufferedImage skin, glow, splits;
    private static int look;
    /** How far the pretty one has rotted on her, while she is being painted: 0 fresh to 4 coming apart. */
    private static int rot;
    /** Whether her skull is being painted bare, for when her hood is off: bone all the way round, not falling away into the dark of it. */
    private static boolean bare;

    public static void main(String[] args) throws Exception {
        File out = new File(args[0]);
        out.mkdirs();
        String[] names = {"stranger", "death", "ragna", "hrafn", "draugr", "gardener", "ayesha", "magus", "spawn", "temptress", "unveiled"};
        for (look = 0; look < names.length; look++) {
            S = look == NorseBoxes.UNVEILED ? 4 : 2;
            Sheet s = NorseBoxes.SHEETS[look];
            if (look == NorseBoxes.TEMPTRESS) {
                // Hers four more times, each worse; and with the worst, what is under it where it splits.
                for (rot = 1; rot <= 7; rot++) {
                    skin = new BufferedImage(s.width * S, s.height * S, BufferedImage.TYPE_INT_ARGB);
                    glow = new BufferedImage(s.width * S, s.height * S, BufferedImage.TYPE_INT_ARGB);
                    splits = rot == 7 ? new BufferedImage(s.width * S, s.height * S, BufferedImage.TYPE_INT_ARGB) : null;
                    for (Box b : s.all) {
                        box(b);
                    }
                    ImageIO.write(skin, "png", new File(out, "norse_temptress_rot" + rot + ".png"));
                }
                ImageIO.write(splits, "png", new File(out, "norse_temptress_splits.png"));
                splits = null;
                rot = 0;
            }
            skin = new BufferedImage(s.width * S, s.height * S, BufferedImage.TYPE_INT_ARGB);
            glow = new BufferedImage(s.width * S, s.height * S, BufferedImage.TYPE_INT_ARGB);
            for (Box b : s.all) {
                box(b);
            }
            ImageIO.write(skin, "png", new File(out, "norse_" + names[look] + ".png"));
            if (look == NorseBoxes.UNVEILED) {
                // Her skull again, bare, for before her hood has come up over it.
                bare = true;
                skin = new BufferedImage(s.width * S, s.height * S, BufferedImage.TYPE_INT_ARGB);
                for (Box b : s.all) {
                    box(b);
                }
                ImageIO.write(skin, "png", new File(out, "norse_unveiled_bare.png"));
                bare = false;
            }
            if (look == NorseBoxes.DRAUGR || look == NorseBoxes.GARDENER || look == NorseBoxes.MAGUS || look == NorseBoxes.SPAWN || look == NorseBoxes.UNVEILED) {
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
                if (rot > 0) {
                    c = decay(b, face, x, y, w, h, c);
                }
                if (splits != null) {
                    int a = split(b, face, x, y, w, h);
                    if (a != 0) {
                        splits.setRGB(x0 + x, y0 + y, a);
                    }
                }
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

    // ------------------------------------------------------------------ the pretty one, rotting

    /** Whether a colour is her skin: warm, red over blue, not too dark (lips and blush with it). */
    private static boolean fleshy(int c) {
        int r = (c >> 16) & 255, g = (c >> 8) & 255, bl = c & 255;
        return r >= 0x60 && r - bl >= 0x20 && r >= g;
    }

    /** A colour as bright as a given one, in another tint. */
    private static int tint(int tint, int like) {
        float l = (((like >> 16) & 255) * 0.3F + ((like >> 8) & 255) * 0.59F + (like & 255) * 0.11F) / 255F;
        float k = Math.min(1F, l * 1.18F);
        return mix(0, tint, k);
    }

    /** Raw meat, where the skin has come away: red, striated, wet in places, and the bone under it here and there. */
    private static int raw(int x, int y, float fine) {
        if (fine > 0.96F) {
            return 0xD8CCB0;
        }
        int c = (x % 2 == 0) ^ (y % 5 == 0) ? 0x7E1616 : 0x5A0C0C;
        return fine > 0.84F ? mix(c, 0xC04040, 0.6F) : c;
    }

    /**
     * Her skin as it goes, over the closer someone comes to her. First pale, the warmth gone out
     * of it, a little shadow under the eyes and the lips gone mauve. Then grey-blue, veins showing
     * through like cracks in ice, the eyes sunk. Then the grey-green of old meat, bruised, split
     * along the jaw, the fingertips black, the hair coming out, one eye clouding over. And at the
     * worst: olive and black with rot, maggots in it, sloughed away in places to the red meat and
     * the bone, a cheek torn through to the teeth, the clothes rotted through, and the seams of
     * the thing showing at every edge, where it was stitched together to be worn.
     */
    private static int decay(Box b, int face, int x, int y, int w, int h, int c) {
        if (c == -1 || c == 0) {
            return c;
        }
        int m = b.material;
        if (m == NorseBoxes.EYEBALL || m == NorseBoxes.GUT || m == NorseBoxes.FLAP || m == NorseBoxes.JAWBONE) {
            // Already as bad as it gets.
            return c;
        }
        int seed = b.u * 131 + b.v * 31 + face * 7;
        char ch = m == NorseBoxes.FACE && face == FRONT && b.w == 8 ? TEMPTRESS_FACE[y].charAt(x) : 0;
        float blot = noise(x / 3, y / 3, seed + 5), fine = noise(x, y, seed + 9), blot2 = noise(x / 2, y / 2, seed + 13);
        if (ch == 'w') {
            // The whites yellow, and at the worst run with blood.
            return rot >= 4 ? (fine > (rot >= 6 ? 0.3F : 0.55F) ? 0xA03838 : 0xC4A868) : rot == 3 ? 0xC8B47A : rot == 2 ? mix(c, 0xD8C4B4, 0.45F) : c;
        }
        if (ch == 'Y' || ch == 'y' || ch == 'P') {
            // Her left eye clouds over, milk-white; the other only dulls.
            if (rot >= 3 && x >= 8) {
                return mix(c, 0xC8CCC0, rot == 4 ? 0.9F : 0.6F);
            }
            return rot >= 4 ? mix(c, 0x5A6A3A, 0.4F) : c;
        }
        if (m == NorseBoxes.HAIR || ch == 'h' || ch == 'H') {
            // Dull, then coming out in patches, the scalp grey under it.
            if (m == NorseBoxes.HAIR && rot >= 3 && blot > (rot >= 7 ? 0.25F : rot == 6 ? 0.4F : rot >= 4 ? 0.6F : 0.8F) && face != BOTTOM) {
                return mix(0x6A6450, 0x3A3428, fine);
            }
            return mix(c, 0x3A3836, 0.12F * rot);
        }
        if (m == NorseBoxes.CLIP || m == NorseBoxes.CUFF || m == NorseBoxes.BOOT) {
            return rot >= 3 ? mix(c, 0x2A2418, 0.2F) : c;
        }
        if (m == NorseBoxes.TOP || m == NorseBoxes.SHORTS) {
            // Faded and stained, and at the worst rotted through to what is under it.
            if (rot >= 4 && blot > 0.76F - (rot - 4) * 0.1F && fine > 0.2F && !fleshy(c)) {
                return blot2 > 0.6F ? raw(x, y, fine) : mix(0x1E1812, 0x3A3020, fine);
            }
            if (rot >= 3 && fine > 0.97F && !fleshy(c)) {
                return 0x4A3A2A;
            }
            if (!fleshy(c)) {
                return mix(c, 0x3A3530, 0.08F * rot);
            }
        }
        if (!fleshy(c)) {
            return c;
        }
        boolean underEye = ch != 0 && (y == 10 || y == 9 && (x == 2 || x == 13)) && (x >= 2 && x <= 5 || x >= 10 && x <= 13) && ch != 'X';
        boolean lips = ch == 'Q';
        boolean tips = b.part.startsWith("fingers") || b.part.startsWith("thumb");
        boolean edge = face != TOP && face != BOTTOM && (x == 0 || x == w - 1 || y == 0 || y == h - 1) && m != NorseBoxes.FACE;
        int s;
        switch (rot) {
            case 1:
                s = mix(c, tint(0xF2E8EA, c), 0.55F);
                if (underEye) {
                    s = mix(s, 0xA08CA2, 0.25F);
                }
                if (lips) {
                    s = mix(s, 0xA88090, 0.5F);
                }
                return s;
            case 2:
                s = mix(c, tint(0xD2D6DE, c), 0.8F);
                if (vein(x, y, seed)) {
                    s = mix(s, 0x52628E, 0.55F);
                }
                if (underEye) {
                    s = mix(s, 0x6E5E80, 0.45F);
                }
                if (lips) {
                    s = 0x7A7A92;
                }
                return s;
            case 3:
                s = tint(0xA6AC92, c);
                if (blot > 0.72F) {
                    s = mix(s, 0x5E4866, 0.5F);
                } else if (blot < 0.2F) {
                    s = mix(s, 0x748450, 0.4F);
                }
                if (vein(x, y, seed)) {
                    s = mix(s, 0x34344E, 0.7F);
                }
                if (crack(x, y, seed) && blot2 > 0.55F) {
                    s = 0x4A0808;
                }
                if (tips) {
                    s = mix(s, 0x2A2420, 0.65F);
                }
                if (underEye) {
                    s = mix(s, 0x3A2A3A, 0.6F);
                }
                if (lips) {
                    s = 0x4A3A4A;
                }
                return s;
            default:
                if (ch != 0 && x >= 10 && x <= 13 && y >= 11 && y <= 13) {
                    // Her right cheek torn through: the teeth behind it.
                    return y >= 12 && x <= 12 ? (x % 2 == 0 ? 0xD8D0B0 : 0x2A1010) : raw(x, y, fine);
                }
                if (rot >= 5 && ch != 0 && (y == 12 || y == 13) && x >= 4 && x <= 11) {
                    // The lips gone: just teeth, top and bottom, and the gums black.
                    return x % 2 == 0 ? (y == 12 ? 0xD8D0B0 : 0xC8BC98) : 0x1A0606;
                }
                if (rot >= 6 && ch != 0 && x >= 8 && y >= 5 && y <= 14) {
                    // Her left side, from the brow to the jaw, worn through to the skull; the socket the eye fell out of, empty.
                    if (x >= 10 && x <= 13 && y >= 7 && y <= 10) {
                        return x == 10 || x == 13 || y == 7 || y == 10 ? 0x5A0C0C : 0x080202;
                    }
                    return fine > 0.85F ? 0xA89C80 : x == 8 || noise(x, y, seed + 31) > 0.8F ? raw(x, y, fine) : 0xD8D0B6;
                }
                if (rot >= 5 && tips) {
                    // The fingertips worn down to the bone.
                    return fine > 0.5F ? 0xD0C8B0 : 0x7A6E58;
                }
                if (rot >= 6 && face == FRONT && b.part.equals("abdomen") && b.w == 7) {
                    // Her ribs, the last of them, showing through the bare stomach.
                    return y % 3 == 0 ? (x == 0 || x == w - 1 ? 0x8A7E66 : 0xD8CEB4) : raw(x, y, fine);
                }
                if (edge) {
                    // The seams it was stitched together along, pulling apart.
                    return (x + y) % 3 == 0 ? 0xB8A88C : 0x2A0A0A;
                }
                s = tint(0x8C8A66, c);
                if (rot >= 6 && blot < 0.22F && (b.part.startsWith("thigh") || b.part.equals("pelvis"))) {
                    // Bloated, purple-black, tight as a drum.
                    return mix(0x3A1A3A, 0x5A2A52, fine);
                }
                if (blot2 > (rot == 4 ? 0.8F : rot == 5 ? 0.68F : rot == 6 ? 0.56F : 0.42F)) {
                    return raw(x, y, fine);
                }
                if (blot > 0.68F - (rot - 4) * 0.05F) {
                    s = fine > 0.92F ? 0xE6E2C6 : mix(0x1E1812, 0x3A3022, fine);
                }
                if (vein(x, y, seed)) {
                    s = mix(s, 0x22222E, 0.7F);
                }
                if (crack(x, y, seed) && blot2 > 0.4F) {
                    s = 0x3A0606;
                }
                if (tips) {
                    s = 0x1A1612;
                }
                if (underEye) {
                    s = 0x2A1A22;
                }
                if (lips) {
                    s = 0x3A0A10;
                }
                return s;
        }
    }

    /** The eye out of its socket: wet white, veined red, the iris clouded green and the pupil blown; raw at the back where the stalk goes in. */
    private static int eyeball(int face, int x, int y, int w, int h, int seed) {
        if (face == BACK) {
            return raw(x, y, noise(x, y, seed));
        }
        if (face == FRONT && x >= w / 2 - 1 && x <= w / 2 && y >= h / 2 - 1 && y <= h / 2) {
            return x == w / 2 && y == h / 2 - 1 ? 0x0A0A0A : 0x7A9A6A;
        }
        float n = noise(x, y, seed + 3);
        return n > 0.78F ? 0xB03030 : n > 0.6F ? 0xD8B0A8 : 0xEEE6D8;
    }

    /** Gut: grey-pink and glistening, in folds, wet highlights catching the light. */
    private static int gut(int face, int x, int y, float light, int seed) {
        float n = noise(x, y, seed + 5);
        if (n > 0.88F) {
            return 0xF0C0C0;
        }
        int c = y % 3 == 0 ? 0x7A3A44 : n > 0.5F ? 0xB87880 : 0xA0646C;
        return mix(c, 0x000000, light < 0F ? 0.25F : 0F);
    }

    /** A flap of her skin, peeled half off: the outside grey and rotten, stitched at the edge; the inside raw. */
    private static int flap(Box b, int face, int x, int y, float light, int seed) {
        boolean outside = b.side < 0 ? face == RIGHT : face == LEFT;
        float n = noise(x, y, seed + 7);
        if (outside) {
            return y == 0 || x == 0 ? ((x + y) % 2 == 0 ? 0xB8A88C : 0x2A0A0A) : n > 0.8F ? 0x1E1812 : mix(0x8C8A66, 0x6A6A4A, n);
        }
        return raw(x, y, n);
    }

    /** Her jaw, hanging: a row of teeth along the top, bone and raw meat round it, the gums black. */
    private static int jawbone(int face, int x, int y, int w, int h, int seed) {
        float n = noise(x, y, seed + 9);
        if (face == TOP) {
            return y < 2 ? (x % 2 == 0 ? 0xD8D0B0 : 0x1A0606) : raw(x, y, n);
        }
        if (face == FRONT) {
            return y == 0 ? (x % 2 == 0 ? 0xD0C8A8 : 0x1A0606) : n > 0.55F ? 0xD8D0B6 : raw(x, y, n);
        }
        return n > 0.5F ? 0xC8BCA0 : raw(x, y, n);
    }

    /** Veins: thin lines that wander down the skin. */
    private static boolean vein(int x, int y, int seed) {
        float wander = (float) Math.sin(y * 0.7F + seed) * 1.6F + (float) Math.sin(y * 0.23F + seed * 0.3F) * 2F;
        int at = Math.round(x + wander);
        return Math.floorMod(at + seed, 7) == 0 && noise(x, y / 3, seed + 3) > 0.3F;
    }

    /** Splits: short dark breaks in the skin, along the diagonals. */
    private static boolean crack(int x, int y, int seed) {
        return Math.floorMod(x + y + seed, 9) == 0 && noise(x / 2, y / 2, seed + 7) > 0.55F;
    }

    /**
     * What is under the skin where it splits, for the tearing: the alpha says how soon each texel
     * opens, the edges of every piece first (the seams it was sewn along), and down the middle of
     * the face and the body; the colour is the raw meat, black in the deepest of the splits.
     * Hair and boots and the little clip don't split.
     */
    private static int split(Box b, int face, int x, int y, int w, int h) {
        int m = b.material;
        if (m == NorseBoxes.HAIR || m == NorseBoxes.BOOT || m == NorseBoxes.CLIP || m == NorseBoxes.CUFF || m == NorseBoxes.LID || m == NorseBoxes.MOUTH
                || m >= NorseBoxes.EYEBALL) {
            return 0;
        }
        if (m == NorseBoxes.FACE && face == FRONT && b.w == 8) {
            char ch = TEMPTRESS_FACE[y].charAt(x);
            if (ch == 'h' || ch == 'H') {
                return 0;
            }
        }
        if (m == NorseBoxes.FACE && face != FRONT) {
            return 0;
        }
        int seed = b.u * 131 + b.v * 31 + face * 7;
        float fine = noise(x, y, seed + 17), blot = noise(x / 3, y / 3, seed + 19);
        float e = Math.min(Math.min(x, w - 1 - x), Math.min(y, h - 1 - y)) / Math.max(1F, Math.min(w, h) / 2F);
        if (face == FRONT && (b.part.equals("chest") || b.part.equals("abdomen") || b.part.equals("pelvis") || b.part.equals("head"))) {
            e = Math.min(e, Math.abs(x - (w - 1) / 2F) / (w / 2F) * 1.3F);
        }
        float a = 1F - e * 0.85F - fine * 0.3F + (blot - 0.5F) * 0.25F;
        a = Math.max(0.03F, Math.min(1F, a));
        int c = a > 0.9F ? 0x200404 : raw(x, y, fine);
        return Math.round(a * 255F) << 24 | c;
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
            case NorseBoxes.TEMPTRESS:
                return PEACH;
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
            case NorseBoxes.TEMPTRESS:
                return RAVEN;
            default:
                return GREY_HAIR;
        }
    }

    private static int paint(Box b, int face, int x, int y, int w, int h) {
        if (look == NorseBoxes.UNVEILED) {
            return lady(b, face, x, y, w, h);
        }
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
            case NorseBoxes.TOP:
                return top(b, face, x, y, w, h, light, seed);
            case NorseBoxes.SHORTS:
                return shorts(b, face, x, y, w, h, light, seed);
            case NorseBoxes.NET:
                return net(b, face, x, y, w, h, light, seed);
            case NorseBoxes.BOOT:
                return boot(b, face, x, y, w, h, light, seed);
            case NorseBoxes.CUFF:
                return cuff(b, face, x, y, w, h, light, seed);
            case NorseBoxes.CLIP:
                return clip(b, face, x, y, w, h, light, seed);
            case NorseBoxes.EYEBALL:
                return eyeball(face, x, y, w, h, seed);
            case NorseBoxes.GUT:
                return gut(face, x, y, light, seed);
            case NorseBoxes.FLAP:
                return flap(b, face, x, y, light, seed);
            case NorseBoxes.JAWBONE:
                return jawbone(face, x, y, w, h, seed);
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

    // ------------------------------------------------------------------ Death, as you'd want her

    /**
     * Her top: black knit, loose, long in the sleeve and cropped short over the stomach, the hem
     * ribbed; a square neckline that shows the skin from the collarbones down to the top of the bust.
     */
    private static int top(Box b, int face, int x, int y, int w, int h, float light, int seed) {
        float mid = Math.abs(x - (w - 1) / 2F);
        int[] tone = tone();
        if (b.part.equals("chest") && b.w == 9 && face == FRONT && mid < (y < 2 ? 2.2F : 3.6F)) {
            return at(tone, 4F + light * 0.4F - (y >= h - 1 && mid < 0.6F ? 0.8F : 0F));
        }
        if (b.part.equals("chest") && b.w == 7 && b.h == 1 && (face == FRONT || face == TOP) && mid < 3.6F) {
            // Just above the neckline, and the line between down the middle.
            return at(tone, 3.8F + light * 0.4F - (mid < 0.6F ? 0.9F : 0F));
        }
        if (b.part.equals("chest") && b.w == 7 && b.h == 2 && (face == TOP || face == FRONT && y == 0) && mid < 3F) {
            return at(tone, 3.6F + light * 0.4F - (mid < 0.6F ? 0.9F : 0F));
        }
        float l = 2.6F + light * 0.6F + ((x + y / 2) % 3 == 0 ? 0.4F : 0F);
        if (b.part.equals("abdomen") && y >= h - 1 && face != TOP && face != BOTTOM) {
            // The hem, ribbed.
            l += x % 2 == 0 ? 0.8F : 0F;
        }
        return at(BLACK_WOOL, l);
    }

    /** Little black shorts: a waistband with a silver button, the legs edged in grey. */
    private static int shorts(Box b, int face, int x, int y, int w, int h, float light, int seed) {
        if (b.part.equals("pelvis") && b.w == 9 && face != BOTTOM) {
            if (face == FRONT && Math.abs(x - (w - 1) / 2F) < 1F) {
                return at(SILVER, 4.5F + light * 0.4F);
            }
            return at(GREY_WOOL, 2.2F + light * 0.4F);
        }
        if (b.part.startsWith("thigh") && face != TOP && face != BOTTOM && y >= h - 1) {
            return at(GREY_WOOL, 3F + light * 0.4F);
        }
        return at(LEATHER_BLACK, 3F + light * 0.6F + (noise(x, y, seed) > 0.75F ? 0.4F : 0F));
    }

    /** Fishnet over bare legs: skin, under a fine diamond of black. */
    private static int net(Box b, int face, int x, int y, int w, int h, float light, int seed) {
        if (b.part.startsWith("thigh") && face != TOP && face != BOTTOM && y < 1) {
            // The stockings' tops, just over the knee.
            return at(BLACK_SILK, 2.6F + light * 0.4F + (x % 2 == 0 ? 0.4F : 0F));
        }
        if ((x + y) % 4 == 0 || (x - y + 60) % 4 == 0) {
            return at(BLACK_SILK, 2.2F + light * 0.3F);
        }
        return at(tone(), 3.4F + light * 0.6F);
    }

    /** Heavy black platform boots: leather, a thick grey-black sole, a silver buckle up the side. */
    private static int boot(Box b, int face, int x, int y, int w, int h, float light, int seed) {
        if (b.part.startsWith("foot") && face != TOP) {
            return y >= h - 1 || face == BOTTOM ? at(IRON, 1.5F) : at(LEATHER_BLACK, 3F + light * 0.5F);
        }
        if ((face == RIGHT || face == LEFT) && x == w / 2 && y % 2 == 1) {
            return at(SILVER, 4.4F);
        }
        return at(LEATHER_BLACK, 2.8F + light * 0.6F + (x == 0 || x == w - 1 ? -0.4F : 0F));
    }

    /** Checked bands at her shoulders, studded cuffs at her wrists, a choker with a ring at its front. */
    private static int cuff(Box b, int face, int x, int y, int w, int h, float light, int seed) {
        if (face == TOP || face == BOTTOM) {
            return at(BLACK_SILK, 2.4F);
        }
        if (b.part.startsWith("upper")) {
            return (x + y) % 2 == 0 ? at(BLACK_SILK, 2.6F) : at(SILVER, 5F + light * 0.3F);
        }
        if (b.part.equals("head")) {
            return face == FRONT && Math.abs(x - (w - 1) / 2F) < 1F ? at(SILVER, 5F) : at(BLACK_SILK, 2.4F + light * 0.4F);
        }
        return x % 2 == 0 && y == h / 2 ? at(SILVER, 5.2F) : at(BLACK_SILK, 2.6F + light * 0.4F);
    }

    /** A white skull, clipped in the black of her hair. */
    private static int clip(Box b, int face, int x, int y, int w, int h, float light, int seed) {
        if (face != FRONT) {
            return at(IVORY, 4F + light * 0.4F);
        }
        boolean eye = y == 1 && (x == 0 || x == w - 1), teeth = y == h - 1 && x % 2 == 1;
        return eye || teeth ? 0x101012 : at(IVORY, 5.5F);
    }

    // Death, as you'd want her: a cute face that knows it. A heavy black fringe; big green eyes,
    // low and soft under thick lashes; freckles across the nose; a small black heart drawn under
    // her right eye; pink in the cheeks; a little smile, turned up at one corner.
    private static final String[] TEMPTRESS_FACE = {
            "hhhhhhhhhhhhhhhh",
            "hhhhhhhhhhhhhhhh",
            "hhhhhhhhhhhhhhhh",
            "hhhhhhhhhhhhhhhh",
            "hhhhHhhhhhhHhhhh",
            "hhshhhshhshhhshh",
            "hdssbbSSSSbbssdh",
            "hLLLLLSSSSLLLLLh",
            "hdwYPYSSSSYPYwdh",
            "hdsyyySSSSyyysdh",
            "hdXrrsfssfsrrsdh",
            "hdsrrsssnssrrsdh",
            "jdsssssssssnssdj",
            "jjdsssnQQQnssdjj",
            "jjjdssssssssdjjj",
            "jjjjjddddddjjjjj"};

    // ------------------------------------------------------------------ Death, unveiled

    /*
     * Painted at twice the density of everyone else (see S), and in the manner of the old pictures
     * of her: flat light and shade, hard highlights on everything that shines, and a line of ink
     * round her edges.
     */
    private static final int[] FROST = {0x3A3448, 0x5E5670, 0x857C98, 0xA9A1BA, 0xC8C1D4, 0xE0DBE8, 0xF4F1F8};
    private static final int[] SUIT_BLACK = {0x040308, 0x0A0811, 0x12101D, 0x1C182E, 0x2A2444, 0x3E365E, 0x5C5284};
    private static final int[] CAPE_VIOLET = {0x0E0A1A, 0x18122C, 0x241B40, 0x312558, 0x413372, 0x56478E, 0x7262AE};
    private static final int[] LINING = {0x160E24, 0x22163A, 0x301F50, 0x402A68, 0x523882, 0x684A9C, 0x8462B8};
    private static final int[] TRIM = {0x34364A, 0x585C72, 0x83879E, 0xACB0C4, 0xCDD1E0, 0xE6E8F2, 0xFCFCFF};
    private static final int[] SKULL_BONE = {0x23212E, 0x45435A, 0x6C6A80, 0x9694A8, 0xBEBCCC, 0xDDDBE6, 0xF7F6FB};
    private static final int INK = 0x07060C, VOID = 0x020104, GLINT = 0xEEDCFF;

    /** Everything on her, by what it is made of. */
    private static int lady(Box b, int face, int x, int y, int w, int h) {
        float light = lit(face, y, h);
        switch (b.material) {
            case NorseBoxes.SKULL:
                return skull(b, face, x, y, w, h);
            case NorseBoxes.JAWBONE:
                return jaw(face, x, y, w, h);
            case NorseBoxes.SOCKET:
                return socket(b, face, x, y, w, h, light);
            case NorseBoxes.SUIT:
                return suit(b, face, x, y, w, h, light);
            case NorseBoxes.STOCKING:
                return stocking(b, face, x, y, w, h, light);
            case NorseBoxes.GLOVE:
                return glove(b, face, x, y, w, h, light);
            case NorseBoxes.BAND:
                return band(b, face, x, y, w, h, light);
            case NorseBoxes.SASH:
                return sash(b, face, x, y, w, h, light);
            case NorseBoxes.RIBBON:
                return ribbon(b, face, x, y, w, h);
            case NorseBoxes.CAPE:
                return cape(b, face, x, y, w, h, light);
            case NorseBoxes.HOOD:
                return cowl(b, face, x, y, w, h, light);
            case NorseBoxes.NECKLACE:
                return bones(b, face, x, y, w, h);
            default:
                return 0xFF00FF;
        }
    }

    /** Where a texel is down a box's side, in the units of its part. */
    private static float down(Box b, int face, int y) {
        return face == TOP ? b.y : face == BOTTOM ? b.y + b.h : b.y + (y + 0.5F) / S;
    }

    /** How far a texel is from the middle of a box's face, across it, -1 at one edge to 1 at the other. */
    private static float across(int x, int w) {
        return (x + 0.5F) / w * 2F - 1F;
    }

    /** How far toward her front a texel on a box's side is, in texels from its front edge. */
    private static int toFront(int face, int x, int w) {
        return face == RIGHT ? x : w - 1 - x;
    }

    /** Her skin: frost, a little blue in its shadows, rounded off toward the edges of every box. */
    private static int frost(int face, int x, int w, float light) {
        float l = 3.9F + light * 0.6F;
        if (face != TOP && face != BOTTOM) {
            float e = across(x, w);
            l += 0.35F - e * e * 0.9F;
        }
        return at(FROST, l);
    }

    /**
     * Black that shines: flat dark, a hard highlight a third of the way across each front and a
     * softer one down the outer sides, and ink at every edge.
     */
    private static int gloss(int[] pal, int face, int x, int y, int w, int h, float light) {
        float l = 2.1F + light * 0.45F;
        float e = across(x, w);
        if (face == FRONT || face == BACK) {
            if (e > -0.62F && e < -0.3F) {
                l += 2.6F;
            } else if (e > -0.75F && e < -0.15F) {
                l += 1.1F;
            }
        } else if (face == RIGHT || face == LEFT) {
            if (Math.abs(e) < 0.2F) {
                l += 1.2F;
            }
        } else if (face == TOP) {
            l += 0.6F;
        }
        if (face != TOP && face != BOTTOM && (x == 0 || x == w - 1)) {
            l -= 1.6F;
        }
        return at(pal, l);
    }

    // Her skull, its face 32 texels square: the cranium lit from above, a hairline crack down from the
    // crown; the sockets tilted like a frown and cut right through, ringed in shadow; the cheekbones
    // high, the cheeks gone hollow under them; the nose a hole, cut through too; the top teeth, long,
    // in a grin it can't stop. Below them, behind where the jaw sits, the dark of the mouth.

    private static boolean cranium(int x, int y) {
        if (x < 0 || x > 31 || y < 0 || y > 31) {
            return false;
        }
        float ax = Math.abs(x - 15.5F);
        if (y < 5) {
            return ax <= 15.5F - (5 - y) * (5 - y) * 0.3F;
        }
        if (y <= 18) {
            return true;
        }
        return y < 24 && ax <= 15.5F - (y - 18) * 1.5F;
    }

    /** How far into a socket a texel is: under 1 inside, the rim just past it. */
    private static float orbit(int x, int y) {
        float ax = Math.abs(x - 15.5F);
        float r = (float) Math.sqrt(Math.pow((ax - 6.7F) / 5.2F, 2) + Math.pow((y + 0.5F - 13.2F) / 4.4F, 2));
        // The brow comes down over the inner corner of each, so it scowls.
        float brow = 9.4F + Math.max(0F, 8.5F - ax) * 0.5F;
        if (y + 0.5F < brow) {
            r = Math.max(r, 1.05F + (brow - y - 0.5F) * 0.2F);
        }
        return r;
    }

    private static boolean noseHole(int x, int y) {
        float ax = Math.abs(x - 15.5F);
        if (y < 16 || y > 20) {
            return false;
        }
        return ax < (y - 15.4F) * 0.62F && !(ax < 0.6F && y >= 19);
    }

    /**
     * One piece of her skull. Its front is its part of the face, wherever on the face it lies: the
     * one picture painted across all of them. The rest of it is bone, lit from above and falling
     * away into the dark the further back it goes, so that in the hood there is no telling how far
     * back it goes.
     */
    private static int skull(Box b, int face, int x, int y, int w, int h) {
        boolean face3d = b.z < 0F;
        if (face == FRONT && face3d) {
            float hx = b.x + (x + 0.5F) / S, hy = b.y + (y + 0.5F) / S;
            return skullFace((int) Math.floor((hx + 4F) * 4F), Math.max(0, (int) Math.floor((hy + 8F) * 4F)));
        }
        if (face3d && face == BOTTOM) {
            // Undersides: in shadow, and the dark of the sockets and the nose seen from inside them.
            return VOID;
        }
        if (face3d && face == TOP && b.y >= -3.5F) {
            // The tops of the pieces under the brow are inside the one above them: through the nose they would show.
            return 0;
        }
        if (face3d && (face == RIGHT || face == LEFT) && b.y + (y + 0.5F) / S > -2F) {
            // Under the teeth is the jaw's.
            return 0;
        }
        if (face3d && (face == RIGHT || face == LEFT)) {
            // A side follows the outline of the face: where the face has narrowed in from this edge, nothing.
            float edge = face == RIGHT ? b.x + 0.1F : b.x + b.w - 0.1F;
            int px = (int) Math.floor((edge + 4F) * 4F), py = (int) Math.floor((b.y + (y + 0.5F) / S + 8F) * 4F);
            if (py >= 0 && py < 24 && !cranium(px, py)) {
                return 0;
            }
        }
        float hz = face == BACK ? b.z + b.d : face == FRONT ? b.z : face == TOP || face == BOTTOM ? b.z + (y + 0.5F) / S
                : b.z + (toFront(face, x, w) + 0.5F) / S;
        float l = face == TOP ? 5F : face == BOTTOM ? 2F : 4.4F - (y + 0.5F) / h * 0.8F;
        float dark = Math.max(0F, Math.min(1F, (hz + 3.5F) / 6.5F));
        return mix(smooth(SKULL_BONE, l), VOID, dark * (bare ? 0.25F : 0.85F));
    }

    /** The face, 32 texels square over the front of her head: the colour at one, or 0 where there is nothing (through to the dark). */
    private static int skullFace(int x, int y) {
        if (x < 0 || x > 31) {
            return 0;
        }
        float ax = Math.abs(x - 15.5F);
        if (y >= 24) {
            return ax < 11F ? VOID : 0;
        }
        if (!cranium(x, y)) {
            return 0;
        }
        float r = orbit(x, y);
        if (r < 1F || noseHole(x, y)) {
            return 0;
        }
        if (!cranium(x - 1, y) || !cranium(x + 1, y) || !cranium(x, y - 1) || !cranium(x, y + 1) && y < 23) {
            return at(SKULL_BONE, 1.4F);
        }
        if (r < 1.12F || noseHole(x - 1, y) || noseHole(x + 1, y) || noseHole(x, y + 1)) {
            return at(SKULL_BONE, 0.8F);
        }
        if (y >= 20 && ax < 8F) {
            // The teeth, a fine line between each, the bite under them.
            int col = (x - 8) % 3;
            if (y == 20) {
                return at(SKULL_BONE, 3F);
            }
            if (y == 23) {
                return at(SKULL_BONE, 1.2F);
            }
            if (col == 0) {
                return at(SKULL_BONE, 3.4F);
            }
            return at(SKULL_BONE, (col == 1 ? 6F : 5.4F) - (y == 21 ? 0.5F : 0F));
        }
        float l = 4.3F;
        // Lit from above: the forehead brightest, the temples falling away.
        l += Math.max(0F, 1F - (ax / 9F) * (ax / 9F) - ((y - 4F) / 6F) * ((y - 4F) / 6F)) * 1.6F;
        l -= Math.max(0F, ax - 11F) * 0.35F;
        if (r < 1.55F) {
            // Round each socket: the brow over it lit, shadow under the bone below it.
            l += y < 13 ? 1F : -0.8F;
        }
        if (y >= 15 && y <= 17 && ax >= 9F && ax <= 14F) {
            l += 1.2F;
        }
        if (y >= 18 && ax >= 8F) {
            l -= y == 18 ? 1.8F : 1.1F;
        }
        int crack = 10 + (y < 3 ? 0 : y < 5 ? 1 : 2) + (y % 3 == 0 ? 1 : 0);
        if (y < 8 && x == crack) {
            l -= 1.6F;
        }
        return at(SKULL_BONE, l);
    }

    /** The jaw: the bottom teeth along its top, the bone of it narrowing to the chin. */
    private static int jaw(int face, int x, int y, int w, int h) {
        float ax = Math.abs(x - (w - 1) / 2F);
        if (face == TOP) {
            // Seen when it drops: the teeth along the front, the dark of the mouth behind them.
            return y < 3 && ax < 8F ? at(SKULL_BONE, x % 3 == 0 ? 3.4F : 5.4F) : VOID;
        }
        if (face == BACK) {
            return bare ? at(SKULL_BONE, 2.2F) : HOOD_IN[1];
        }
        if (face == BOTTOM) {
            // Only as wide as the chin.
            return ax < 4.5F ? at(SKULL_BONE, 2.2F) : 0;
        }
        if (face != FRONT) {
            // The sides only where the front reaches its edges, along the teeth; below that the chin has narrowed away from them.
            if (y >= 3) {
                return 0;
            }
            int f = toFront(face, x, w);
            return f < 8 || bare ? at(SKULL_BONE, 3.6F - (bare && f >= 8 ? 0.8F : 0F)) : HOOD_IN[1];
        }
        // The chin: the corners cut away below the teeth, so it narrows.
        float edge = y < 3 ? 12F : 11.5F - (y - 2) * 1.35F;
        if (ax > edge) {
            return 0;
        }
        if (ax > edge - 1F || y == h - 1) {
            return at(SKULL_BONE, 1.4F);
        }
        if (y < 3 && ax < 8F) {
            if (y == 0) {
                return at(SKULL_BONE, 1.2F);
            }
            return at(SKULL_BONE, (x + 1) % 3 == 0 ? 3.4F : (x % 3 == 1 ? 5.8F : 5.2F) - (y == 2 ? 0.5F : 0F));
        }
        return at(SKULL_BONE, 4.5F - ax * 0.16F + (y >= 4 && ax < 4F ? 0.8F : 0F) - (y == 3 ? 0.9F : 0F));
    }

    /** The dark inside her head, seen through her sockets and her nose; and in each socket, far back, the one point of light. */
    private static int socket(Box b, int face, int x, int y, int w, int h, float light) {
        if (face != FRONT) {
            return VOID;
        }
        float r = glint(b, x, y);
        return r < 0.2F ? GLINT : r < 0.6F ? mix(0x2C1E48, VOID, (r - 0.2F) / 0.4F) : VOID;
    }

    /**
     * The suit: high on the bust with a scalloped neckline, close at the waist with a long pointed
     * panel down the front, cut high at the hip; trimmed white all along its edges. Above and
     * beside it, her.
     */
    private static int suit(Box b, int face, int x, int y, int w, int h, float light) {
        float py = down(b, face, y);
        float e = across(x, w);
        String part = b.part;
        int trim = at(TRIM, 4.6F + light * 0.4F);
        if (part.equals("chest") && b.h == 7) {
            // The chest under the bust: bare above the line of the suit, the shoulders and the back.
            float line = face == FRONT ? -5.2F : face == BACK ? -4.4F : -5.4F;
            if (face == TOP || py < line) {
                return frost(face, x, w, light);
            }
            if (py < line + 0.3F) {
                return trim;
            }
            if (face == FRONT) {
                return panel(e, -8.4F + 3.4F * (py + 3.4F) / 3.4F, x, w, light);
            }
            return gloss(SUIT_BLACK, face, x, y, w, h, light);
        }
        if (part.equals("chest") && b.y == -6.1F) {
            // The top of the bust, above the neckline.
            return face == BOTTOM ? trim : frost(face, x, w, light + 0.2F);
        }
        if (part.equals("chest") && b.y == -5.3F) {
            // The neckline, in two scallops over the bust, dipping between and at each side.
            if (face == BOTTOM) {
                return at(SUIT_BLACK, 1F);
            }
            float v = face == TOP ? 0F : y / (h - 1F);
            float u = (e + 1F) / 2F;
            float neck = face == FRONT ? 0.08F + 0.44F * (1F - Math.abs((float) Math.sin(Math.PI * 2 * u))) : 0.4F;
            if (face == TOP || v < neck) {
                return frost(face, x, w, light + 0.3F);
            }
            if (v < neck + 0.16F) {
                return trim;
            }
            if (face == FRONT && v > 0.42F && v < 0.7F && (Math.abs(u - 0.26F) < 0.06F || Math.abs(u - 0.74F) < 0.06F)) {
                // A hard highlight on each, where the light catches.
                return at(SUIT_BLACK, 6F);
            }
            return gloss(SUIT_BLACK, face, x, y, w, h, light);
        }
        if (part.equals("chest")) {
            // Tucked in under the bust, a line of white beneath it.
            if (face == FRONT && y >= h - S / 2) {
                return trim;
            }
            return face == TOP ? at(SUIT_BLACK, 1.4F) : gloss(SUIT_BLACK, face, x, y, w, h, light - 0.5F);
        }
        if (part.equals("abdomen")) {
            if (face == FRONT) {
                return panel(e, py, x, w, light);
            }
            return gloss(SUIT_BLACK, face, x, y, w, h, light);
        }
        // The hips: across the top it covers her; down the front it narrows to a point, and the
        // sides are bare from the hip bone down.
        float v = (py + 2F) / 3F;
        if (face == RIGHT || face == LEFT) {
            if (py < -1.45F) {
                return gloss(SUIT_BLACK, face, x, y, w, h, light);
            }
            return py < -1.2F ? trim : frost(face, x, w, light);
        }
        if (face == TOP) {
            return at(SUIT_BLACK, 2.2F);
        }
        float half = face == BACK ? 1F - 0.4F * v : 1F - 0.78F * (float) Math.pow(Math.max(0F, v), 0.85F);
        float ax = Math.abs(e) * b.w / 2F;
        float edge = half * b.w / 2F;
        if (face == BOTTOM) {
            return ax < 1F ? at(SUIT_BLACK, 1F) : frost(face, x, w, light);
        }
        if (ax > edge + 0.3F) {
            return frost(face, x, w, light);
        }
        if (ax > edge) {
            return trim;
        }
        int c = gloss(SUIT_BLACK, face, x, y, w, h, light);
        return face == FRONT && Math.abs(e) < 0.08F && v > 0.25F ? at(SUIT_BLACK, 1F) : c;
    }

    /**
     * The long panel down the front of the suit, from under the bust to below the navel, by how far
     * up her body a texel is (-8.4 under the bust to 0 at the hips): a point of it trimmed white,
     * the black either side of it shaped to her, a little light down the middle of it.
     */
    private static int panel(float e, float body, int x, int w, float light) {
        float k = (body + 8.4F) / 8.4F;
        float half = 0.82F - 0.7F * k;
        float ae = Math.abs(e);
        if (Math.abs(ae - half) < 0.07F) {
            return at(TRIM, 4.6F + light * 0.4F);
        }
        float l = 2.2F + light * 0.4F;
        if (ae < half) {
            l += 0.7F - ae * 0.8F;
            if (ae < 0.05F && k > 0.25F && k < 0.85F) {
                l -= 0.8F;
            }
            if (k > 0.35F && k < 0.75F && Math.abs(k * 8.4F % 1.6F - 0.8F) < 0.12F && ae < half - 0.15F) {
                l -= 0.6F;
            }
        } else {
            l -= 0.3F;
            if (ae > 0.55F && ae < 0.7F) {
                l += 1.2F;
            }
        }
        if (x == 0 || x == w - 1) {
            l -= 1.4F;
        }
        return at(SUIT_BLACK, l);
    }

    /** The stockings, to mid-thigh and open at the toe and the heel; her above them. */
    private static int stocking(Box b, int face, int x, int y, int w, int h, float light) {
        if (b.part.startsWith("thigh") && down(b, face, y) < 3.7F) {
            return frost(face, x, w, light);
        }
        if (b.part.startsWith("foot")) {
            // Bare toes, a line between each; bare heel; the stocking over the arch only.
            int f = face == FRONT ? 0 : face == BACK ? 5 * S : face == TOP || face == BOTTOM ? y : toFront(face, x, w);
            if (f < S * 1.4F || f >= S * 4) {
                if (f < S * 1.4F && (face == TOP || face == FRONT) && x % 3 == 2) {
                    return at(FROST, 2.4F);
                }
                return frost(face, x, w, light - (face == BOTTOM ? 0.6F : 0F));
            }
            if (f < S * 1.4F + 1 || f >= S * 4 - 1) {
                return at(SUIT_BLACK, 0.4F);
            }
        }
        return gloss(SUIT_BLACK, face, x, y, w, h, light);
    }

    /** The gloves, past the elbow; her shoulders above them. */
    private static int glove(Box b, int face, int x, int y, int w, int h, float light) {
        if (b.part.startsWith("upper") && down(b, face, y) < 1.3F) {
            return frost(face, x, w, light);
        }
        if (b.part.startsWith("fingers") && (face == FRONT || face == BACK) && x % S == S - 1) {
            return at(SUIT_BLACK, 0.5F);
        }
        return gloss(SUIT_BLACK, face, x, y, w, h, light);
    }

    /**
     * Bands: at the tops of the gloves and the stockings, black with a stripe of white through it;
     * and at the wrists, the cuffs, silver, bevelled, a groove round the middle of each.
     */
    private static int band(Box b, int face, int x, int y, int w, int h, float light) {
        if (b.part.startsWith("fore")) {
            if (face == TOP || face == BOTTOM) {
                return at(TRIM, 2.4F);
            }
            if (face == FRONT && Math.abs(across(x, w)) < 0.2F && y > 0 && y < h - 1) {
                // The clasp, where the cape is tied.
                return at(TRIM, 6);
            }
            float l = 3.8F + light * 0.5F;
            if (y == 0) {
                l += 2F;
            } else if (y == h - 1) {
                l -= 1.6F;
            } else if (y == h / 2) {
                l -= 1.8F;
            }
            return at(TRIM, l);
        }
        if (face == TOP || face == BOTTOM) {
            return at(SUIT_BLACK, 1.6F);
        }
        if (y == h / 2 || y == h / 2 - 1) {
            return at(TRIM, 5F + light * 0.4F);
        }
        return gloss(SUIT_BLACK, face, x, y, w, h, light + (y == 0 ? 0.6F : 0F));
    }

    /** The sash at her hips: white satin, gathered, round her sides and back only, so the front of the suit comes down to its point. */
    private static int sash(Box b, int face, int x, int y, int w, int h, float light) {
        if (face == FRONT || (face == TOP || face == BOTTOM) && Math.abs(across(x, w)) * b.w / 2F < 3.4F) {
            return 0;
        }
        if (face == TOP || face == BOTTOM) {
            return at(TRIM, 2.6F);
        }
        float fold = (float) Math.sin(x * 0.9F + y * 0.4F);
        return at(TRIM, 4.2F + light * 0.4F + fold * 0.7F - (y == 0 || y == h - 1 ? 1.4F : 0F));
    }

    /**
     * The ribbons: long and narrow, white and some black, each a little wavy and each its own
     * length, with a fork cut in its end, and nothing between them but air. Fewer in front.
     */
    private static int ribbon(Box b, int face, int x, int y, int w, int h) {
        boolean side = b.part.startsWith("skirtS");
        boolean broad = side ? face == RIGHT || face == LEFT : face == FRONT || face == BACK;
        if (!broad) {
            return 0;
        }
        boolean lower = b.part.endsWith("2");
        float py = (lower ? 5.8F : 0F) + (y + 0.5F) / S;
        int panel = b.part.charAt(5) * 7 + b.part.charAt(6) * 3 + face;
        int sway = Math.round((float) Math.sin(py * 0.7F + panel) * 1.2F);
        int xc = x - w / 2 - sway;
        int slot = Math.floorDiv(xc, 3), in = Math.floorMod(xc, 3);
        if (in == 2) {
            return 0;
        }
        if (b.part.startsWith("skirtF") && noise(slot, panel, 41) < 0.4F) {
            return 0;
        }
        float end = 6.5F + noise(slot, panel, 43) * 4.5F;
        if (py > end || py > end - 0.6F && in == (slot & 1)) {
            return 0;
        }
        boolean dark = noise(slot, panel, 47) < 0.3F;
        float l = (dark ? 2.4F : 4.4F) + (in == 0 ? 1.2F : 0F) - py * 0.06F;
        return at(dark ? SUIT_BLACK : TRIM, l);
    }

    /**
     * The cape: deep violet satin, painted as the one sheet it is, whatever pieces it hangs in. Its
     * folds start close together across her shoulders and open out as they fall, the way cloth
     * gathered at a yoke does; a few large ones, a few smaller between; each catching a soft sheen
     * along its crest and settling into shadow between, shaded smoothly, never in steps. It darkens
     * toward the hem and toward its edges, where it turns away round her; the lining is a shade
     * lighter and warmer; the hem falls in a slow wave.
     * <p>
     * The falls from the wrists the same, gathered narrow at the cuff and sweeping back and wide
     * toward the ground, their folds fanning out from the wrist.
     */
    private static int cape(Box b, int face, int x, int y, int w, int h, float light) {
        if (b.part.startsWith("drape")) {
            return fall(b, face, x, y, w, h);
        }
        boolean right = b.part.charAt(4) == 'R';
        int j = b.part.charAt(5) - '0', i = b.part.charAt(7) - '0';
        boolean last = i == NorseBoxes.CAPE_SEGMENTS - 1;
        float down = (i + (y + 0.5F) / h) / NorseBoxes.CAPE_SEGMENTS;
        // Where this texel lies across the whole cape, in units of cloth out from the middle of her back: the front
        // faces run with the model's x, the back faces against it. The same at every length and across every
        // pleat, so nothing jumps where one piece meets the next.
        float modelX = b.x + (face == BACK ? b.w - (x + 0.5F) / S : (x + 0.5F) / S);
        float inner = 0F, half = 0F;
        for (int k = 0; k < NorseBoxes.CAPE_PLEATS; k++) {
            inner += k < j ? NorseBoxes.PLEAT_WIDTHS[k] : 0;
            half += NorseBoxes.PLEAT_WIDTHS[k];
        }
        float t = (inner + Math.abs(modelX)) / half * (right ? -1F : 1F);
        if (face != FRONT && face != BACK) {
            return 0;
        }
        if (last && y >= h - (0.7F + 0.5F * (float) Math.sin(t * 6F + 0.5F) + 0.4F * t * t) * S) {
            return 0;
        }
        return satin(t, down, face == FRONT);
    }

    /** A fall of the cape from a wrist (see cape): straight-edged, its folds running down it, its hem in a slow wave. */
    private static int fall(Box b, int face, int x, int y, int w, int h) {
        int i = b.part.charAt(5) - '0';
        boolean last = i == NorseBoxes.DRAPES - 1;
        float v = face == TOP ? 0F : face == BOTTOM ? 1F : (y + 0.5F) / h;
        float down = (i + v) / NorseBoxes.DRAPES;
        float reach = b.d;
        if (face == BOTTOM || face == TOP && i > 0) {
            return 0;
        }
        if (face == FRONT || face == BACK) {
            return satin(face == FRONT ? -1F : 1F, down, false);
        }
        float f = face == TOP ? (y + 0.5F) / S : (toFront(face, x, w) + 0.5F) / S;
        if (f > reach || last && v > 0.96F - 0.04F * (1F + (float) Math.sin(f * 0.9F))) {
            return 0;
        }
        boolean outer = b.side < 0 ? face == RIGHT : face == LEFT;
        return satin(f / reach * 1.6F - 0.8F, down, !outer);
    }

    /**
     * The satin itself, at a place across it (-1 to 1, the folds fanning out down it) and a way down
     * it (0 to 1): the outside, or the lining.
     */
    private static int satin(float t, float down, boolean lining) {
        // Fewer, closer folds at the yoke, opening out and deepening as it falls.
        float spread = 0.55F + 0.45F * down;
        float a = t / spread;
        float fold = 0.55F * (float) Math.sin(a * 5.3F + 0.4F) + 0.3F * (float) Math.sin(a * 9.7F + 1.3F + down * 1.4F)
                + 0.15F * (float) Math.sin(a * 16.1F - down * 2F);
        float depth = 0.6F + 0.4F * down;
        float sheen = Math.max(0F, fold - 0.35F);
        float l = (lining ? 3.1F : 2.6F) + fold * depth * 1.1F + sheen * sheen * 3F - down * 0.6F - t * t * t * t * 0.7F;
        return smooth(lining ? LINING : CAPE_VIOLET, l);
    }

    /** A colour from a palette at any level between its steps, blended, not stepped. */
    private static int smooth(int[] pal, float level) {
        float l = Math.max(0F, Math.min(pal.length - 1.001F, level));
        int lo = (int) l;
        return mix(pal[lo], pal[lo + 1], l - lo);
    }

    /**
     * The hood: violet-black satin outside, folds down it and a sheen near the front; inside, dark as
     * a well; the edge of the opening catching the light, so the dark in it has a frame.
     */
    private static int cowl(Box b, int face, int x, int y, int w, int h, float light) {
        boolean inside = b.part.equals("brim") ? face == BOTTOM || face == BACK
                : b.w == 1 ? (b.x < 0 ? face == LEFT : face == RIGHT) : b.d == 1 && b.z > 0 ? face == FRONT : face == BOTTOM;
        if (inside) {
            return at(HOOD_IN, 1.1F + (face == BOTTOM ? 0F : 0.5F));
        }
        if (face == FRONT && b.z <= -5.1F) {
            return smooth(CAPE_VIOLET, 4F + light * 0.4F);
        }
        // The same satin as the cape, shaded smoothly: brightest over the crown, a soft sheen down the sides
        // toward the front, falling into shadow behind.
        float l = 2.4F + light * 0.5F;
        if (face == RIGHT || face == LEFT) {
            float f = (toFront(face, x, w) + 0.5F) / w;
            l += 0.9F * (float) Math.exp(-(f - 0.25F) * (f - 0.25F) * 18F) - f * 0.5F;
        }
        return smooth(CAPE_VIOLET, l);
    }

    /** The collar: little bones strung close, each lit on top, a gap of dark string between. */
    private static int bones(Box b, int face, int x, int y, int w, int h) {
        boolean along = b.w > 1;
        int t = along ? (face == RIGHT || face == LEFT ? -1 : x) : (face == FRONT || face == BACK ? -1 : face == TOP || face == BOTTOM ? y : x);
        if (t < 0) {
            return at(SKULL_BONE, 4F);
        }
        int bead = t % 3;
        int row = face == TOP || face == BOTTOM ? 1 : y;
        if (bead == 2) {
            return row == 1 || row == 2 ? at(SKULL_BONE, 1F) : 0;
        }
        if ((row == 0 || row == h - 1) && face != TOP && face != BOTTOM) {
            return bead == 0 ? at(SKULL_BONE, 3F) : 0;
        }
        return at(SKULL_BONE, face == TOP ? 6F : face == BOTTOM ? 2.6F : bead == 0 ? 5.6F : 4.4F);
    }

    /** How near a texel on the back wall of her sockets is to the light in one of them, in units. */
    private static float glint(Box b, int x, int y) {
        float xc = (x + 0.5F) / S + b.x, yc = (y + 0.5F) / S + b.y;
        float gx = Math.abs(xc) - 1.68F, gy = yc + 4.6F;
        return (float) Math.sqrt(gx * gx + gy * gy);
    }

    /** What of her gives off light: the point at the back of each socket. */
    private static int ladyGlow(Box b, int face, int x, int y) {
        if (b.material != NorseBoxes.SOCKET || face != FRONT) {
            return 0;
        }
        float r = glint(b, x, y);
        return r < 0.2F ? 0xFFF4E8FF : r < 0.55F ? 0x70A070FF : 0;
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
            case NorseBoxes.TEMPTRESS:
                return art(TEMPTRESS_FACE, x, y);
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
                return look == NorseBoxes.TEMPTRESS ? mix(tone[4], 0xF07A92, 0.32F) : mix(tone[4], 0xC06A62, look == NorseBoxes.STRANGER ? 0.08F : 0.28F);
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
                return look == NorseBoxes.TEMPTRESS ? mix(tone[4], FAIR[2], 0.45F) : FAIR[2];
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
            case 'e':
                // Smoke on the lids.
                return mix(tone[3], 0x6A4A6A, 0.35F);
            case 'L':
                return 0x0A0608;
            case 'Y':
                return 0x48A858;
            case 'y':
                return 0x2C7038;
            case 'X':
                // The little heart under her eye.
                return 0x1A0A12;
            case 'Q':
                return 0xB0505E;
            case 'V':
                return 0x9A5CE0;
            case 'R':
                return 0xA0283A;
            case 'q':
                return 0xC4485A;
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
        if (look == NorseBoxes.TEMPTRESS && face != TOP && face != BOTTOM) {
            float e = Math.abs(x - (w - 1) / 2F) / (w / 2F);
            l += 0.3F - e * e * 0.8F;
        }
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
        if (look == NorseBoxes.UNVEILED) {
            return ladyGlow(b, face, x, y);
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
