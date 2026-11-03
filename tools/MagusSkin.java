import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;

/**
 * Paints the Magus's 128x128 skin and its two companions, cut to the same pattern as Adam
 * Warlock's (he is what Warlock grows into, and the likeness has to read):
 * <ul>
 * <li><b>magus.png</b>: lavender skin, white hair swept up and back off a hard widow's peak,
 * blank white eyes under a heavy brow, a scowl. A violet tunic with a white lightning bolt
 * on the chest and great pointed shoulders, violet trunks, bare arms and legs. Silver
 * everywhere else: an ornate belt with a round buckle, wristbands, and boots to the knee,
 * flared and scrolled at the top.</li>
 * <li><b>magus_power.png</b>: the same man with what he holds thrown open, white-violet from
 * head to foot.</li>
 * <li><b>magus_glow.png</b>: what lights up from within: his eyes, the gem at his brow, the
 * bolt on his chest. Drawn additively and at full brightness.</li>
 * </ul>
 * The box layout here must match client/render/ModelMagus.java. In every front face the
 * left of the picture is his right side, as when you face him.
 *
 * Usage: java MagusSkin <output dir>   (src/main/resources/assets/eternity/textures/entity)
 */
public class MagusSkin {

    private static final int[] SKIN = {0x4E3A78, 0x6A5498, 0x8670B6, 0xA08CCC, 0xB8A6DC, 0xCEBEEA, 0xE2D6F6};
    private static final int[] HAIR = {0x5E5A6C, 0x827E92, 0xA4A0B4, 0xC2C0D0, 0xDAD8E6, 0xEDECF4, 0xFFFFFF};
    private static final int[] TUNIC = {0x1A0634, 0x2A0A50, 0x3E106E, 0x54188E, 0x6C22AC, 0x8634C8, 0xA250E0};
    private static final int[] SILVER = {0x34363F, 0x52545F, 0x737582, 0x9698A6, 0xBABCC8, 0xD8DAE3, 0xF4F5FA};
    private static final int[] GEM = {0x3A0A5A, 0x5A1488, 0x7E22B4, 0xA040DA, 0xC274F0, 0xE0B0FF, 0xFFFFFF};
    private static final int[] POWER = {0x2A0E48, 0x4A2478, 0x7048AA, 0x9C78D4, 0xC4AAF0, 0xE6DAFF, 0xFFFFFF};
    private static final int BOLT = 0xF2F0FA, BOLT_EDGE = 0x9A94B4, EYE = 0xF4F0FF, EYE_SHADOW = 0xB8A8D8;

    private static final int TOP = 0, BOTTOM = 1, RIGHT = 2, FRONT = 3, LEFT = 4, BACK = 5;

    private static final Random RANDOM = new Random(2099L);
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
        shoulders();
        arms();
        legs();

        ImageIO.write(skin, "png", new File(out, "magus.png"));
        ImageIO.write(powered(), "png", new File(out, "magus_power.png"));
        ImageIO.write(glow, "png", new File(out, "magus_glow.png"));
        ImageIO.write(sleeper(), "png", new File(out, "magus_sleeper.png"));
    }

    /**
     * One of the gallery's sleepers: the same face, grown in a vat and never woken, the eyes
     * shut, the skin gone grey and sickly under the frost.
     */
    private static BufferedImage sleeper() {
        BufferedImage out = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 128; y++) {
            for (int x = 0; x < 128; x++) {
                int argb = skin.getRGB(x, y);
                if ((argb >>> 24) == 0) {
                    continue;
                }
                int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
                int grey = (int) (0.3 * r + 0.59 * g + 0.11 * b);
                out.setRGB(x, y, 0xFF000000 | mix(argb & 0xFFFFFF, grey << 16 | (grey + 8) << 8 | grey, 0.45));
            }
        }
        // The eyes shut: lids where the light was.
        for (int x = 9; x <= 14; x++) {
            if (x != 11 && x != 12) {
                out.setRGB(x, 12, 0xFF000000 | mix(SKIN[2], 0x707070, 0.45));
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ the head

    private static void head() {
        box(0, 0, 8, 8, 8, (face, x, y, w, h) -> {
            switch (face) {
                case TOP:
                    return streak(HAIR, 4, x, y);
                case BACK:
                    return y < 6 ? streak(HAIR, 3, x, y) : shade(SKIN, y == 6 ? 2 : 3, x, y, w, h);
                case RIGHT:
                case LEFT: {
                    // Swept straight back over the ears; the ear; a hard jaw.
                    int toFront = face == RIGHT ? x : w - 1 - x;
                    if (y < 3 || y < 4 && toFront < 5) {
                        return streak(HAIR, y < 2 ? 4 : 3, x, y);
                    }
                    if (toFront >= 2 && toFront <= 3 && y >= 3 && y <= 5) {
                        return SKIN[y == 4 ? 2 : 3];
                    }
                    if (y == 7 && toFront < 4) {
                        return SKIN[2];
                    }
                    return shade(SKIN, toFront > 5 ? 4 : 3, x, y, w, h);
                }
                case BOTTOM:
                    return SKIN[2];
                default:
                    return -1;
            }
        });
        // His face. h/H hair, s/S/d skin, b brow, W the blank white of an eye, w its shadowed edge, n nose, m/M mouth.
        art(8, 8, new String[]{
                "hhhhhhhh",
                "shhHHhhs",
                "dsShhSsd",
                "sbbsSbbs",
                "swWsSWws",
                "dssnnssd",
                "dssMMssd",
                "ddsSSsdd"});
        // His eyes are nothing but light.
        for (int[] e : new int[][]{{10, 12}, {13, 12}}) {
            glowAt(e[0], e[1], 0xF0E4FF, 1F);
        }
        glowAt(9, 12, 0xC8A0FF, 0.55F);
        glowAt(14, 12, 0xC8A0FF, 0.55F);
        // The gem at his brow, dark until it is fed.
        box(122, 0, 1, 1, 1, (face, x, y, w, h) -> face == FRONT ? GEM[4] : face == TOP ? GEM[3] : GEM[2]);
        for (int x = 122; x < 126; x++) {
            for (int y = 0; y < 2; y++) {
                glowAt(x, y, 0xD090FF, 1F);
            }
        }
    }

    // ------------------------------------------------------------------ the hair

    private static void hair() {
        // The crown, combed hard back.
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
        // Swept up off the forehead, high: the brightest thing about him but his eyes.
        box(68, 0, 8, 4, 5, (face, x, y, w, h) -> {
            switch (face) {
                case TOP:
                    return streak(HAIR, 6, x, y);
                case FRONT:
                    return streak(HAIR, y == 0 ? 6 : y == 1 ? 5 : y == 2 ? 4 : 3, x, y);
                case BOTTOM:
                    return HAIR[1];
                default:
                    return streak(HAIR, 5 - y / 2, x, y);
            }
        });
        // Down the back to the nape.
        box(94, 0, 9, 6, 2, (face, x, y, w, h) -> streak(HAIR, face == BOTTOM ? 1 : y >= 4 ? 2 : 3, x, y));
        // Close at the sides.
        for (int u : new int[]{94, 108}) {
            box(u, 8, 1, 4, 6, (face, x, y, w, h) -> streak(HAIR, y < 2 ? 4 : 3, x, y));
        }
    }

    // ------------------------------------------------------------------ the torso

    private static void torso() {
        // The tunic: violet, the chest drawn in muscle under it.
        box(0, 16, 9, 7, 5, (face, x, y, w, h) -> {
            switch (face) {
                case TOP:
                    return shade(TUNIC, 4, x, y, w, h);
                case BACK:
                    return x == 4 ? TUNIC[2] : shade(TUNIC, y < 3 ? 4 : 3, x, y, w, h);
                case RIGHT:
                case LEFT:
                    return shade(TUNIC, 3, x, y, w, h);
                default:
                    return TUNIC[3];
            }
        });
        // The front: pectorals in two tones, and the bolt across them. P tunic, p shadow, L light, B bolt, e its edge.
        art(5, 21, new String[]{
                "PLLPPPLLP",
                "LLLPPBBeL",
                "PLLPBBePP",
                "pPPBBBBBp",
                "pPPPeBBPp",
                "PpPPBePpP",
                "PPpPBPpPP"});
        int[][] bolt = {{10, 22}, {11, 22}, {9, 23}, {10, 23}, {8, 24}, {9, 24}, {10, 24}, {11, 24}, {12, 24}, {10, 25}, {11, 25}, {9, 26}, {9, 27}};
        for (int[] b : bolt) {
            glowAt(b[0], b[1], 0xE8DCFF, 0.4F);
        }

        // Abdomen: the tunic's skirt over the stomach, the muscle showing through in rows.
        box(28, 16, 8, 5, 4, (face, x, y, w, h) -> {
            if (face == FRONT) {
                if (x == 0 || x == 7) {
                    return TUNIC[2];
                }
                boolean seam = x == 3 || x == 4 ? y % 2 == 1 : y % 2 == 1;
                return seam ? TUNIC[2] : TUNIC[x == 1 || x == 6 ? 3 : 4];
            }
            return shade(TUNIC, 3, x, y, w, h);
        });
        // The belt: silver links, and between them scrollwork.
        box(52, 16, 9, 2, 5, (face, x, y, w, h) -> {
            if (face == FRONT || face == BACK) {
                return y == 0 ? SILVER[x % 3 == 1 ? 6 : 5] : SILVER[x % 2 == 0 ? 3 : 2];
            }
            return face == TOP || face == BOTTOM ? SILVER[2] : SILVER[y == 0 ? 5 : 3];
        });
        // Its buckle: a round plate, an eye of dark metal in it.
        box(80, 16, 3, 3, 1, (face, x, y, w, h) -> {
            if (face == FRONT) {
                if (x == 1 && y == 1) {
                    return TUNIC[1];
                }
                return (x + y) % 2 == 0 ? SILVER[4] : SILVER[6];
            }
            return SILVER[3];
        });
        glowAt(85, 18, 0xB070FF, 0.5F);
    }

    // ------------------------------------------------------------------ the shoulders

    /** The tunic's great shoulders, flaring up to points. */
    private static void shoulders() {
        for (int side = 0; side < 2; side++) {
            box(side == 0 ? 0 : 20, 44, 4, 2, 6, (face, x, y, w, h) -> {
                if (face == TOP) {
                    boolean rim = x == 0 || y == 0 || x == w - 1 || y == h - 1;
                    return rim ? TUNIC[6] : TUNIC[(x + y) % 3 == 0 ? 5 : 4];
                }
                if (face == BOTTOM) {
                    return TUNIC[1];
                }
                return y == 0 ? TUNIC[5] : TUNIC[3];
            });
            box(112, side == 0 ? 44 : 50, 2, 2, 4, (face, x, y, w, h) ->
                    face == TOP ? TUNIC[6] : face == BOTTOM ? TUNIC[1] : TUNIC[y == 0 ? 5 : 3]);
        }
    }

    // ------------------------------------------------------------------ the arms

    private static void arms() {
        for (int side = 0; side < 2; side++) {
            final int inner = side == 0 ? LEFT : RIGHT, outer = side == 0 ? RIGHT : LEFT;
            // Upper arm: bare, heavy with muscle.
            box(side == 0 ? 0 : 16, 32, 4, 6, 4, (face, x, y, w, h) -> {
                if (face == TOP) {
                    return SKIN[4];
                }
                if (face == BOTTOM) {
                    return SKIN[2];
                }
                if (face == FRONT && (x == 1 || x == 2) && y >= 1 && y <= 3) {
                    return SKIN[5];
                }
                if (face == outer && y >= 1 && y <= 2) {
                    return SKIN[5];
                }
                return shade(SKIN, face == inner ? 3 : 4, x, y, w, h);
            });
            // Forearm and fist.
            box(side == 0 ? 32 : 48, 32, 4, 7, 4, (face, x, y, w, h) -> {
                if (face == TOP) {
                    return SKIN[3];
                }
                if (face == BOTTOM) {
                    return SKIN[2];
                }
                if (y >= 5 && face == FRONT) {
                    return y == 5 ? SKIN[x % 2 == 0 ? 5 : 4] : SKIN[x % 2 == 0 ? 3 : 2];
                }
                return shade(SKIN, y < 2 ? 4 : 3, x, y, w, h);
            });
            // Wristband: silver, ridged.
            box(side == 0 ? 64 : 84, 32, 5, 2, 5, (face, x, y, w, h) -> {
                if (face == TOP || face == BOTTOM) {
                    return SILVER[3];
                }
                return y == 0 ? SILVER[x % 2 == 0 ? 6 : 5] : SILVER[x % 2 == 0 ? 3 : 4];
            });
        }
    }

    // ------------------------------------------------------------------ the legs

    private static void legs() {
        for (int side = 0; side < 2; side++) {
            final int outer = side == 0 ? RIGHT : LEFT;
            // Thigh: the trunks at the top, then bare.
            box(side == 0 ? 48 : 64, 44, 4, 6, 4, (face, x, y, w, h) -> {
                if (face == TOP) {
                    return TUNIC[3];
                }
                if (face == BOTTOM) {
                    return SKIN[2];
                }
                if (y < 2) {
                    return TUNIC[y == 0 ? 3 : 2];
                }
                if (face == FRONT && (x == 1 || x == 2) && y < 5) {
                    return SKIN[5];
                }
                return shade(SKIN, face == outer ? 4 : 3, x, y, w, h);
            });
            // Shin: under the boot.
            box(side == 0 ? 80 : 96, 44, 4, 6, 4, (face, x, y, w, h) -> shade(SKIN, 3, x, y, w, h));
            // Boot: silver, scrollwork running down its face, a dark sole.
            box(side == 0 ? 0 : 20, 56, 5, 6, 5, (face, x, y, w, h) -> {
                if (face == BOTTOM) {
                    return SILVER[0];
                }
                if (face == TOP) {
                    return SILVER[3];
                }
                if (y == 5) {
                    return SILVER[1];
                }
                if (face == FRONT || face == outer) {
                    // A curl: a line that turns back on itself down the shin.
                    boolean scroll = y == 1 && x >= 1 && x <= 3 || y == 2 && x == 3 || y == 3 && x >= 2 && x <= 3 || y == 4 && x == 1;
                    if (scroll) {
                        return SILVER[1];
                    }
                    return SILVER[x == 0 || x == w - 1 ? 3 : y < 2 ? 5 : 4];
                }
                return shade(SILVER, 3, x, y, w, h);
            });
            // The flared top of the boot, at the knee.
            box(side == 0 ? 40 : 64, 56, 6, 2, 6, (face, x, y, w, h) -> {
                if (face == TOP) {
                    return SILVER[x == 0 || y == 0 || x == w - 1 || y == h - 1 ? 6 : 2];
                }
                if (face == BOTTOM) {
                    return SILVER[1];
                }
                return y == 0 ? SILVER[6] : SILVER[x % 2 == 0 ? 4 : 3];
            });
            // Knee plate.
            box(side == 0 ? 88 : 98, 56, 4, 3, 1, (face, x, y, w, h) -> {
                if (face == FRONT) {
                    return y == 1 && (x == 1 || x == 2) ? SILVER[6] : SILVER[y == 2 ? 3 : 4];
                }
                return SILVER[3];
            });
        }
    }

    // ------------------------------------------------------------------ powered

    /** Every pixel as white-violet light, by how light it was: what he looks like with what he holds thrown open. */
    private static BufferedImage powered() {
        BufferedImage out = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 128; y++) {
            for (int x = 0; x < 128; x++) {
                int argb = skin.getRGB(x, y);
                if ((argb >>> 24) == 0) {
                    continue;
                }
                int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
                double lum = (0.3 * r + 0.59 * g + 0.11 * b) / 255.0;
                double t = Math.pow(lum, 0.55) * 6.0;
                int lo = (int) Math.floor(t), hi = Math.min(6, lo + 1);
                out.setRGB(x, y, 0xFF000000 | mix(POWER[Math.min(6, lo)], POWER[hi], t - lo));
            }
        }
        out.setRGB(10, 12, 0xFFFFFFFF);
        out.setRGB(13, 12, 0xFFFFFFFF);
        return out;
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
     * Hand-placed pixels. Face: h/H hair, s/S/d skin, b brow, W/w eye, n nose, m/M mouth.
     * Tunic: P/p/L violet, B/e the bolt.
     */
    private static void art(int x, int y, String[] rows) {
        boolean tunic = false;
        for (String row : rows) {
            tunic |= row.indexOf('P') >= 0;
        }
        for (int j = 0; j < rows.length; j++) {
            for (int i = 0; i < rows[j].length(); i++) {
                int c = tunic ? tunicColour(rows[j].charAt(i)) : faceColour(rows[j].charAt(i));
                if (c >= 0) {
                    set(x + i, y + j, c);
                }
            }
        }
    }

    private static int faceColour(char ch) {
        switch (ch) {
            case 'h': return HAIR[4];
            case 'H': return HAIR[6];
            case 's': return SKIN[4];
            case 'S': return SKIN[5];
            case 'd': return SKIN[3];
            case 'b': return SKIN[1];
            case 'W': return EYE;
            case 'w': return EYE_SHADOW;
            case 'n': return SKIN[3];
            case 'm': return SKIN[2];
            case 'M': return 0x3C2458;
            default: return -1;
        }
    }

    private static int tunicColour(char ch) {
        switch (ch) {
            case 'P': return TUNIC[3];
            case 'p': return TUNIC[2];
            case 'L': return TUNIC[5];
            case 'B': return BOLT;
            case 'e': return BOLT_EDGE;
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
