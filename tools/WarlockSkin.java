import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;

/**
 * Paints Adam Warlock's 128x128 skin and its two companions:
 * <ul>
 * <li><b>adam_warlock.png</b>: as he stands at rest. Slicked-back golden hair with a quiff,
 * sun-gold skin and blue eyes, an amber gem on his brow. Crimson armour in plates over a
 * dark mesh suit, a chevron of gold trim converging down his chest, a high collar, layered
 * pauldrons, vambraces, knee guards and boots, a gold belt with a jewelled buckle. A
 * crimson cape lined with gold.</li>
 * <li><b>adam_warlock_gold.png</b>: the same man with his power up, burnished gold from head
 * to foot, the way he burns when he flies; his cape only warmed by it.</li>
 * <li><b>adam_warlock_glow.png</b>: what lights up from within: his eyes, the gem, the gold
 * seams of his armour. Drawn additively and at full brightness.</li>
 * </ul>
 * The box layout here must match client/render/ModelAdamWarlock.java. In every front face
 * the left of the picture is his right side, as when you face him.
 *
 * Usage: java WarlockSkin <output dir>   (src/main/resources/assets/eternity/textures/entity)
 */
public class WarlockSkin {

    private static final int[] SKIN = {0x5A3420, 0x7A4A2C, 0x9A6038, 0xB87A48, 0xD0965C, 0xE2B078, 0xF0CC9C};
    private static final int[] HAIR = {0x5C3C10, 0x86601A, 0xAE8628, 0xCDA43A, 0xE2BE55, 0xF0D57E, 0xFAEAB4};
    private static final int[] ARMOR = {0x24080A, 0x3E0E14, 0x5E1820, 0x82222A, 0xA63438, 0xC8564E, 0xE88A78};
    private static final int[] SUIT = {0x120607, 0x1C090B, 0x281012, 0x351518, 0x441B1E, 0x552427, 0x682E2E};
    private static final int[] TRIM = {0x4E3206, 0x7A520C, 0xA67414, 0xCC9826, 0xE8BC44, 0xF6D97C, 0xFFF2C4};
    private static final int[] CAPE = {0x320407, 0x52080D, 0x761015, 0x98161C, 0xB82027, 0xD43336, 0xEC5A52};
    private static final int[] LINING = {0x6A4410, 0x8E5E16, 0xB07C20, 0xCC9A2E, 0xE0B444, 0xEECB6A, 0xF8E29C};
    private static final int[] GEM = {0x8A4800, 0xC06A00, 0xF09A18, 0xFFC040, 0xFFDC7A, 0xFFF0B8, 0xFFFFFF};
    private static final int[] BURNISHED = {0x4A2606, 0x7A4610, 0xA86C18, 0xD49628, 0xEEBC46, 0xFFDA80, 0xFFF2CC};
    private static final int EYE_WHITE = 0xEDE6DC, IRIS = 0x3A6EA8, PUPIL = 0x101826;

    private static final int TOP = 0, BOTTOM = 1, RIGHT = 2, FRONT = 3, LEFT = 4, BACK = 5;

    private static final Random RANDOM = new Random(1972L);
    private static BufferedImage skin, glow;

    /** Paints one face of a box: given the face and a pixel in it, a colour, or -1 for none. */
    private interface Painter {
        int paint(int face, int x, int y, int w, int h);
    }

    public static void main(String[] args) throws Exception {
        File out = new File(args[0]);
        out.mkdirs();
        skin = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        glow = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);

        head();
        hair();
        torso();
        arms();
        legs();
        cape();

        ImageIO.write(skin, "png", new File(out, "adam_warlock.png"));
        ImageIO.write(golden(), "png", new File(out, "adam_warlock_gold.png"));
        ImageIO.write(glow, "png", new File(out, "adam_warlock_glow.png"));
    }

    // ------------------------------------------------------------------ the head

    private static void head() {
        box(0, 0, 8, 8, 8, (face, x, y, w, h) -> {
            switch (face) {
                case TOP:
                    return shade(HAIR, 4, x, y, w, h);
                case BACK:
                    // Slicked down to the nape, skin below.
                    return y < 6 ? streak(HAIR, 3, x, y) : shade(SKIN, y == 6 ? 2 : 3, x, y, w, h);
                case RIGHT:
                case LEFT: {
                    // Hair swept back over the ears, faded short above them; the ear; the jaw.
                    int toFront = face == RIGHT ? x : w - 1 - x;
                    if (y < 2 || y < 4 && toFront < 4) {
                        return streak(HAIR, y < 2 ? 4 : 2, x, y);
                    }
                    if (toFront >= 2 && toFront <= 3 && y >= 3 && y <= 5) {
                        return SKIN[y == 4 ? 2 : 3];
                    }
                    return shade(SKIN, toFront > 5 ? 4 : 3, x, y, w, h);
                }
                case BOTTOM:
                    return SKIN[2];
                default:
                    return -1;
            }
        });
        // His face. h hair, s skin (S light, d shadow), b brow, W white of an eye, I iris, P pupil, n nose, m mouth.
        art(8, 8, new String[]{
                "hhhhhhhh",
                "hhHhhHhh",
                "dsSSSSsd",
                "sbbSSbbs",
                "sWIsSIWs",
                "dssnnssd",
                "dssmmssd",
                "ddsSSsdd"});
        // His eyes light up when his power rises.
        glowAt(10, 12, 0xFFE9A0, 0.95F);
        glowAt(13, 12, 0xFFE9A0, 0.95F);
        glowAt(9, 12, 0xFFC860, 0.35F);
        glowAt(14, 12, 0xFFC860, 0.35F);
        // The gem on his brow.
        box(118, 0, 1, 1, 1, (face, x, y, w, h) -> face == FRONT ? GEM[6] : face == TOP ? GEM[5] : GEM[3]);
        for (int x = 118; x < 122; x++) {
            for (int y = 0; y < 2; y++) {
                glowAt(x, y, 0xFFD070, 1F);
            }
        }
    }

    // ------------------------------------------------------------------ the hair

    private static void hair() {
        // The crown, combed straight back.
        box(32, 0, 9, 2, 9, (face, x, y, w, h) -> {
            switch (face) {
                case TOP:
                    return streak(HAIR, 4 + (y < 3 ? 1 : 0), x, y);
                case FRONT:
                    return streak(HAIR, 5 - y, x, y);
                case BACK:
                    return streak(HAIR, 3, x, y);
                default:
                    return streak(HAIR, 4 - y, x, y);
            }
        });
        // The quiff standing up off his forehead, brightest where it catches the light.
        box(68, 0, 8, 3, 4, (face, x, y, w, h) -> {
            switch (face) {
                case TOP:
                    return streak(HAIR, 6, x, y);
                case FRONT:
                    return streak(HAIR, y == 0 ? 6 : y == 1 ? 5 : 3, x, y);
                default:
                    return streak(HAIR, 4, x, y);
            }
        });
        // Down the back to the nape.
        box(68, 7, 9, 5, 2, (face, x, y, w, h) -> streak(HAIR, face == BOTTOM ? 1 : y >= 4 ? 2 : 3, x, y));
        // Short at the sides.
        for (int u : new int[]{90, 104}) {
            box(u, 0, 1, 4, 6, (face, x, y, w, h) -> streak(HAIR, y < 2 ? 3 : 2, x, y));
        }
    }

    // ------------------------------------------------------------------ the torso

    private static void torso() {
        // Chest: crimson plates, a gold chevron converging on his sternum.
        box(0, 16, 9, 7, 5, (face, x, y, w, h) -> {
            switch (face) {
                case TOP:
                    return y < 2 ? shade(ARMOR, 4, x, y, w, h) : (x == 4 ? SUIT[2] : shade(ARMOR, 5, x, y, w, h));
                case BACK: {
                    if (x == 4) {
                        return TRIM[y < 2 ? 3 : 2];
                    }
                    return y == 3 ? SUIT[3] : shade(ARMOR, 3, x, y, w, h);
                }
                case RIGHT:
                case LEFT: {
                    int toFront = face == RIGHT ? x : w - 1 - x;
                    if (toFront == 4) {
                        return TRIM[3];
                    }
                    return y >= 4 && toFront < 3 ? SUIT[3] : shade(ARMOR, 3, x, y, w, h);
                }
                default:
                    return SUIT[2];
            }
        });
        art(5, 21, new String[]{
                "LRrSgSrRL",
                "RLRgSgRLR",
                "rRLRgRLRr",
                "SrRLGLRrS",
                "gSrRGRrSg",
                "SgSrYrSgS",
                "SSgSESgSS"});
        // The seams of his armour glow with his power.
        int[][] seams = {{9, 21}, {11, 22}, {13, 22}, {9, 23}, {9, 24}, {9, 25}, {5, 25}, {13, 25}, {9, 26}, {6, 26}, {12, 26}, {7, 27}, {9, 27}, {11, 27}};
        for (int[] s : seams) {
            glowAt(s[0], s[1], 0xFFC060, 0.6F);
        }
        glowAt(9, 27, 0xFFE0A0, 1F);

        // Abdomen: two columns of plates over the suit.
        box(28, 16, 8, 5, 4, (face, x, y, w, h) -> {
            if (face == FRONT) {
                if (x == 3 || x == 4) {
                    return SUIT[y % 2 == 0 ? 3 : 1];
                }
                if (x == 0 || x == 7) {
                    return SUIT[2];
                }
                return ARMOR[y % 2 == 1 ? 2 : x == 2 || x == 5 ? 5 : 4];
            }
            if (face == BACK) {
                return x == 3 || x == 4 ? SUIT[2] : shade(ARMOR, 3, x, y, w, h);
            }
            return shade(SUIT, 3, x, y, w, h);
        });
        // The belt, gold, crimson set into it.
        box(52, 16, 9, 2, 5, (face, x, y, w, h) -> {
            if (face == FRONT || face == BACK) {
                return y == 0 ? TRIM[x % 3 == 0 ? 6 : 5] : (x % 2 == 0 ? TRIM[3] : ARMOR[3]);
            }
            return face == TOP || face == BOTTOM ? TRIM[2] : (y == 0 ? TRIM[5] : TRIM[3]);
        });
        // Its buckle: a jewel in gold.
        box(80, 16, 2, 2, 1, (face, x, y, w, h) -> face == FRONT ? (x + y == 1 ? GEM[4] : GEM[2]) : TRIM[4]);
        for (int x = 81; x < 83; x++) {
            for (int y = 17; y < 19; y++) {
                glowAt(x, y, 0xFFB040, 0.7F);
            }
        }
        // The high collar behind his head.
        box(90, 10, 10, 5, 1, (face, x, y, w, h) -> {
            if (face == FRONT) {
                return y == 0 || x == 0 || x == w - 1 ? TRIM[4] : shade(ARMOR, 2, x, y, w, h);
            }
            if (face == BACK) {
                return y == 0 ? TRIM[5] : x == 0 || x == w - 1 ? TRIM[3] : shade(ARMOR, 4, x, y, w, h);
            }
            return TRIM[4];
        });
    }

    // ------------------------------------------------------------------ the arms

    private static void arms() {
        for (int side = 0; side < 2; side++) {
            final int inner = side == 0 ? LEFT : RIGHT, outer = side == 0 ? RIGHT : LEFT;
            // Upper arm: a crimson plate on the outside, mesh at the elbow and on the inside.
            box(side == 0 ? 0 : 16, 32, 4, 6, 4, (face, x, y, w, h) -> {
                if (face == TOP) {
                    return ARMOR[4];
                }
                if (face == BOTTOM || face == inner || y >= 5) {
                    return shade(SUIT, 3, x, y, w, h);
                }
                if (y == 4) {
                    return TRIM[face == outer ? 4 : 3];
                }
                return ARMOR[(face == outer && (x == 1 || x == 2)) || face == FRONT && x == 1 ? 5 : 3 + (y < 2 ? 1 : 0)];
            });
            // Forearm: mesh under the vambrace, then a bare fist.
            box(side == 0 ? 32 : 48, 32, 4, 7, 4, (face, x, y, w, h) -> {
                if (face == TOP) {
                    return SUIT[3];
                }
                if (face == BOTTOM) {
                    return SKIN[3];
                }
                if (y < 5) {
                    return shade(SUIT, 3, x, y, w, h);
                }
                if (face == FRONT) {
                    // Knuckles, then the folded fingers.
                    return y == 5 ? SKIN[x % 2 == 0 ? 5 : 4] : SKIN[x % 2 == 0 ? 3 : 2];
                }
                return SKIN[y == 5 ? 4 : 3];
            });
            // Vambrace: crimson, cuffed in gold at both ends, a bright fin down the outside.
            box(side == 0 ? 64 : 84, 32, 5, 4, 5, (face, x, y, w, h) -> {
                if (face == TOP || face == BOTTOM) {
                    return TRIM[3];
                }
                if (y == 0 || y == 3) {
                    return TRIM[y == 0 ? 5 : 3];
                }
                if (face == outer && x == 2) {
                    return ARMOR[6];
                }
                return ARMOR[y == 1 ? 4 : 3];
            });
            // Pauldron: overlapping crimson plates with a gold rim.
            box(side == 0 ? 0 : 24, 44, 6, 3, 6, (face, x, y, w, h) -> {
                if (face == TOP) {
                    boolean rim = x == 0 || y == 0 || x == w - 1 || y == h - 1;
                    return rim ? TRIM[5] : ARMOR[(x + y) % 3 == 0 ? 6 : 4];
                }
                if (face == BOTTOM) {
                    return SUIT[2];
                }
                return y == 2 ? TRIM[3] : ARMOR[y == 0 ? 5 : 3];
            });
        }
        // The gold edges of the vambraces glow faintly.
        for (int x = 64; x < 104; x++) {
            if (skin.getRGB(x, 37) != 0) {
                glowAt(x, 37, 0xFFC060, 0.35F);
            }
        }
    }

    // ------------------------------------------------------------------ the legs

    private static void legs() {
        for (int side = 0; side < 2; side++) {
            final int inner = side == 0 ? LEFT : RIGHT, outer = side == 0 ? RIGHT : LEFT;
            // Thigh: crimson plates in front, a gold stripe down the outside, mesh inside and behind.
            box(side == 0 ? 48 : 64, 44, 4, 6, 4, (face, x, y, w, h) -> {
                if (face == TOP || face == BOTTOM) {
                    return SUIT[2];
                }
                if (face == outer) {
                    return x == 1 || x == 2 ? TRIM[y == 0 ? 5 : 3] : ARMOR[3];
                }
                if (face == FRONT) {
                    return y == 3 ? SUIT[2] : ARMOR[x == 1 ? 5 : 4 - (y > 3 ? 1 : 0)];
                }
                return shade(SUIT, face == inner ? 3 : 2, x, y, w, h);
            });
            // Shin: mostly under the boot and the knee guard.
            box(side == 0 ? 80 : 96, 44, 4, 6, 4, (face, x, y, w, h) ->
                    face == FRONT && y < 2 ? ARMOR[4] : shade(SUIT, 3, x, y, w, h));
            // Boot: crimson, a gold cuff, a dark sole.
            box(side == 0 ? 0 : 20, 56, 5, 4, 5, (face, x, y, w, h) -> {
                if (face == BOTTOM) {
                    return SUIT[1];
                }
                if (face == TOP) {
                    return TRIM[4];
                }
                if (y == 0) {
                    return TRIM[5];
                }
                if (y == 3) {
                    return SUIT[2];
                }
                return ARMOR[face == FRONT && x == 2 ? 5 : y == 1 ? 4 : 3];
            });
            // Knee guard.
            art(side == 0 ? 29 : 39, 67, new String[]{"gRRg", "RLLR", "gRRg"});
            box(side == 0 ? 28 : 38, 66, 4, 3, 1, (face, x, y, w, h) -> face == FRONT ? -1 : TRIM[3]);
        }
    }

    // ------------------------------------------------------------------ the cape

    private static void cape() {
        int[][] uv = {{40, 56}, {62, 56}, {86, 56}, {0, 66}};
        for (int i = 0; i < uv.length; i++) {
            final int segment = i;
            final int width = 10 + i;
            box(uv[i][0], uv[i][1], width, 6, 1, (face, x, y, w, h) -> {
                // Folds run down its whole length, the same across every segment.
                double fold = Math.sin((x - width / 2.0) * 1.25 + 0.5) + 0.4 * Math.sin((x - width / 2.0) * 0.6);
                int level = (int) Math.round(3.5 + fold * 1.4 - segment * 0.25 - y * 0.05);
                boolean hem = segment == 3 && y == 5;
                switch (face) {
                    case BACK:
                        return hem ? CAPE[1] : CAPE[clamp(level)];
                    case FRONT:
                        // The gold lining, darker where it is shadowed by him.
                        return hem ? LINING[5] : LINING[clamp(level - 1)];
                    case TOP:
                        return CAPE[4];
                    default:
                        return CAPE[2];
                }
            });
        }
    }

    // ------------------------------------------------------------------ the golden form

    /**
     * Every pixel as burnished gold, by how light it was: skin, hair and armour alike. His
     * eyes go white-gold and the gem stays as it is. The cape only takes on some of the
     * gold: it is cloth, not him.
     */
    private static BufferedImage golden() {
        BufferedImage gold = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 128; y++) {
            for (int x = 0; x < 128; x++) {
                int argb = skin.getRGB(x, y);
                if ((argb >>> 24) == 0) {
                    continue;
                }
                int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
                double lum = (0.3 * r + 0.59 * g + 0.11 * b) / 255.0;
                // Lift the dark armour so it reads as gold metal, not shadow.
                double t = Math.pow(lum, 0.6) * 6.0;
                int lo = (int) Math.floor(t), hi = Math.min(6, lo + 1);
                int c = mix(BURNISHED[Math.min(6, lo)], BURNISHED[hi], t - lo);
                if (isCape(x, y)) {
                    c = mix(argb & 0xFFFFFF, c, 0.4);
                }
                if (x >= 118 && x < 122 && y < 2) {
                    c = argb & 0xFFFFFF;
                }
                gold.setRGB(x, y, 0xFF000000 | c);
            }
        }
        // His eyes burn white-gold.
        gold.setRGB(10, 12, 0xFFFFF4D0);
        gold.setRGB(13, 12, 0xFFFFF4D0);
        return gold;
    }

    private static boolean isCape(int x, int y) {
        return y >= 56 && y < 63 && x >= 40 && x < 112 || y >= 66 && y < 73 && x < 28;
    }

    // ------------------------------------------------------------------ painting

    /** Paints every face of a box laid out the vanilla way at (u, v). */
    private static void box(int u, int v, int w, int h, int d, Painter painter) {
        face(painter, TOP, u + d, v, w, d);
        face(painter, BOTTOM, u + d + w, v, w, d);
        face(painter, RIGHT, u, v + d, d, h);
        face(painter, FRONT, u + d, v + d, w, h);
        face(painter, LEFT, u + d + w, v + d, d, h);
        face(painter, BACK, u + d + w + d, v + d, w, h);
    }

    private static void face(Painter painter, int face, int x0, int y0, int w, int h) {
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int c = painter.paint(face, x, y, w, h);
                if (c >= 0) {
                    set(x0 + x, y0 + y, c);
                }
            }
        }
    }

    /** A shaded, grainy pixel: lighter toward the top, darker toward the bottom and the edges. */
    private static int shade(int[] ramp, int level, int x, int y, int w, int h) {
        int l = level;
        if (h >= 4) {
            if (y < h / 4) {
                l++;
            } else if (y >= h - h / 4) {
                l--;
            }
        }
        if (w >= 4 && (x == 0 || x == w - 1)) {
            l--;
        }
        if (RANDOM.nextFloat() < 0.12F) {
            l += RANDOM.nextBoolean() ? 1 : -1;
        }
        return ramp[clamp(l)];
    }

    /** Hair: strands combed one way, alternate columns a shade apart, a few lighter strands catching the light. */
    private static int streak(int[] ramp, int level, int x, int y) {
        int l = level + ((x * 7 + 3) % 5 == 0 ? 1 : x % 2 == 0 ? 0 : -1);
        if (RANDOM.nextFloat() < 0.1F) {
            l += 1;
        }
        return ramp[clamp(l)];
    }

    /**
     * Hand-placed pixels. Face: h/H hair, s/S/d skin, b brow, W white, I iris, P pupil, n nose,
     * m/M mouth. Armour: R/r/L crimson, S suit (in armour art), g/G/Y gold, E the jewel.
     * A string is armour art if it contains R; otherwise it is the face.
     */
    private static void art(int x, int y, String[] rows) {
        boolean armour = false;
        for (String row : rows) {
            armour |= row.indexOf('R') >= 0;
        }
        for (int j = 0; j < rows.length; j++) {
            for (int i = 0; i < rows[j].length(); i++) {
                int c = armour ? armourColour(rows[j].charAt(i)) : faceColour(rows[j].charAt(i));
                if (c >= 0) {
                    set(x + i, y + j, c);
                }
            }
        }
    }

    private static int faceColour(char ch) {
        switch (ch) {
            case 'h': return HAIR[3];
            case 'H': return HAIR[5];
            case 's': return SKIN[4];
            case 'S': return SKIN[5];
            case 'd': return SKIN[3];
            case 'b': return HAIR[1];
            case 'W': return EYE_WHITE;
            case 'I': return IRIS;
            case 'P': return PUPIL;
            case 'n': return SKIN[3];
            case 'm': return 0xB06C58;
            case 'M': return 0x7E3E34;
            default: return -1;
        }
    }

    private static int armourColour(char ch) {
        switch (ch) {
            case 'R': return ARMOR[4];
            case 'r': return ARMOR[2];
            case 'L': return ARMOR[5];
            case 'S': return SUIT[2];
            case 'g': return TRIM[3];
            case 'G': return TRIM[4];
            case 'Y': return TRIM[6];
            case 'E': return GEM[4];
            default: return -1;
        }
    }

    private static void glowAt(int x, int y, int rgb, float alpha) {
        glow.setRGB(x, y, ((int) (alpha * 255F) << 24) | rgb);
    }

    private static int mix(int a, int b, double t) {
        int r = (int) Math.round(((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
        int g = (int) Math.round(((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
        int bl = (int) Math.round((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return r << 16 | g << 8 | bl;
    }

    private static int clamp(int level) {
        return Math.max(0, Math.min(6, level));
    }

    private static void set(int x, int y, int rgb) {
        skin.setRGB(x, y, 0xFF000000 | rgb);
    }
}
