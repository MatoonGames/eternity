import com.matoon.eternity.client.render.EternityBoxes;
import com.matoon.eternity.client.render.EternityBoxes.Box;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Paints Eternity's 128x128 skin and its two glow layers, box by box, from the layout in
 * client/render/EternityBoxes.java (so it always matches the model):
 * <ul>
 * <li><b>eternity.png</b>: his body as open space, near-black indigo, drifting patches of
 * orange, green, rose and teal gas, stars of every brightness and a few great cross-shaped
 * ones; a blue mask of a face, gaunt and severe, with a heavy brow, deep sockets and pale
 * gold eyes, a long nose, a mouth turned down at the corners; his hood and crests dark and
 * starry; his cloak and the mantle on his shoulders pale ice-blue, with flowing darker lines,
 * lit edges, and the hem torn into rags.</li>
 * <li><b>eternity_glow_a.png</b> and <b>eternity_glow_b.png</b>: what shines in him, drawn
 * additively at full brightness: his eyes in both, and his brighter stars split between the
 * two, which the renderer fades in and out against each other so his stars twinkle.</li>
 * </ul>
 * In every front face the left of the picture is his right side.
 *
 * Usage: javac -d out tools/EternitySkin.java src/main/java/com/matoon/eternity/client/render/EternityBoxes.java
 *        java -cp out EternitySkin &lt;output dir&gt;   (src/main/resources/assets/eternity/textures/entity)
 */
public class EternitySkin {

    private static final int TOP = 0, BOTTOM = 1, RIGHT = 2, FRONT = 3, LEFT = 4, BACK = 5;

    private static final int[] SPACE = {0x05040C, 0x09081A, 0x0E0C26, 0x141233, 0x1C1942, 0x262252, 0x312C63};
    private static final int[] SKIN = {0x16224E, 0x22346E, 0x2F4B8E, 0x3F62A8, 0x5580C2, 0x6E9CD6, 0x8DB8E8, 0xB2D2F4};
    private static final int[] CLOTH = {0x2A4A86, 0x3D64A6, 0x5482C2, 0x6E9ED8, 0x8DBAE8, 0xAED2F4, 0xD4EAFF};
    private static final int[][] GAS = {{0xFF7A1E, 0xFFB050}, {0x34D47A, 0x8CF0B0}, {0xF0589C, 0xFF9AC8}, {0x2CB8D0, 0x86E4F0}, {0x8A4CE0, 0xBC94FF}};
    private static final int EYE = 0xFFE08A, EYE_HOT = 0xFFF6D8;

    private static BufferedImage skin, glowA, glowB;
    private static Box current;

    private interface Painter {
        /** A colour for a pixel of one face of the current box, or -1 to leave it clear. */
        int paint(int face, int x, int y, int w, int h);
    }

    public static void main(String[] args) throws Exception {
        File out = new File(args[0]);
        out.mkdirs();
        int size = EternityBoxes.SIZE;
        skin = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        glowA = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        glowB = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        for (Box box : EternityBoxes.ALL) {
            current = box;
            switch (box.material) {
                case EternityBoxes.FACE:
                    paint(box, EternitySkin::face);
                    break;
                case EternityBoxes.SKULL:
                    paint(box, EternitySkin::skull);
                    break;
                case EternityBoxes.BROW:
                    paint(box, EternitySkin::brow);
                    break;
                case EternityBoxes.BONE:
                    paint(box, EternitySkin::bone);
                    break;
                case EternityBoxes.CLOAK:
                case EternityBoxes.MANTLE:
                    paint(box, (f, x, y, w, h) -> cloth(f, x, y, w, h, false));
                    break;
                case EternityBoxes.HEM:
                    paint(box, (f, x, y, w, h) -> cloth(f, x, y, w, h, true));
                    break;
                case EternityBoxes.HOOD:
                    paint(box, (f, x, y, w, h) -> space(f, x, y, w, h, 0, -1));
                    break;
                case EternityBoxes.CREST:
                    paint(box, (f, x, y, w, h) -> space(f, x, y, w, h, 0.25, -1));
                    break;
                default:
                    paint(box, (f, x, y, w, h) -> space(f, x, y, w, h, 0.6, 0));
                    break;
            }
        }
        ImageIO.write(skin, "png", new File(out, "eternity.png"));
        ImageIO.write(glowA, "png", new File(out, "eternity_glow_a.png"));
        ImageIO.write(glowB, "png", new File(out, "eternity_glow_b.png"));
    }

    // ------------------------------------------------------------------ open space

    /**
     * Space: dark, with patches of glowing gas and stars. The gas follows one field across the
     * whole sheet, so neighbouring faces share their clouds.
     *
     * @param gas    how much gas there is, 0 to 1
     * @param darken how much darker than the body (negative for the hood)
     */
    private static int space(int face, int x, int y, int w, int h, double gas, int darken) {
        int gx = px(face, x), gy = py(face, y);
        double n = fbm(gx / 14.0, gy / 14.0, 4, 11);
        int level = 2 + darken + (n > 0.62 ? 1 : 0) + (rand(gx, gy, 3) < 0.15 ? 1 : 0) - (rand(gx, gy, 4) < 0.1 ? 1 : 0);
        // Darker toward the edges of a face, so every block reads as a block.
        if (w > 2 && (x == 0 || x == w - 1) || h > 2 && (y == 0 || y == h - 1)) {
            level--;
        }
        int c = SPACE[clamp(level, SPACE.length)];
        // Gas: one hue per region, its brighter core where the field peaks.
        double cloud = fbm(gx / 9.0 + 40, gy / 9.0, 3, 23);
        if (cloud > 0.66 - 0.06 * gas && gas > 0) {
            int hue = (int) (fbm(gx / 60.0, gy / 60.0, 2, 37) * GAS.length * 1.6) % GAS.length;
            double k = Math.min(1, (cloud - (0.66 - 0.06 * gas)) / 0.14);
            boolean core = k > 0.75 && rand(gx, gy, 5) < 0.6;
            c = mix(c, GAS[hue][core ? 1 : 0], (0.12 + 0.3 * k) * gas);
            if (core) {
                glow(gx, gy, GAS[hue][1], 0.25, rand(gx, gy, 8) < 0.5);
            }
        }
        // Stars.
        double r = rand(gx, gy, 7);
        if (r < 0.012) {
            int star = rand(gx, gy, 9) < 0.5 ? 0xFFFFFF : rand(gx, gy, 10) < 0.5 ? 0xFFE8A0 : 0xA8D0FF;
            glow(gx, gy, star, 1, rand(gx, gy, 11) < 0.5);
            return star;
        }
        if (r < 0.04) {
            return mix(c, 0x8CA0D8, 0.25 + 0.2 * rand(gx, gy, 12));
        }
        return c;
    }

    // ------------------------------------------------------------------ the face

    private static int face(int face, int x, int y, int w, int h) {
        if (face != FRONT) {
            return skull(face, x, y, w, h);
        }
        // L light, l mid, d dark, k deep, E eye, n the bridge of the nose, m the set mouth.
        String[] art = {
                "lLLLLl",
                "dlLLld",
                "kEkkEk",
                "dkllkd",
                "ldnnld",
                "dmmmmd",
                "mdLLdm"};
        char ch = art[y].charAt(x);
        if (ch == 'E') {
            glow(px(face, x), py(face, y), EYE_HOT, 1, true);
            glow(px(face, x), py(face, y), EYE_HOT, 1, false);
            return EYE;
        }
        return skinColour(ch);
    }

    private static int skull(int face, int x, int y, int w, int h) {
        switch (face) {
            case TOP:
            case BACK:
                return space(face, x, y, w, h, 0.3, -1);
            case BOTTOM:
                return SKIN[1];
            default: {
                // Temples and the line of the jaw, darkening toward the back where the hood shadows him.
                int toFront = face == RIGHT ? x : w - 1 - x;
                int level = 5 - toFront / 2 - (y > h - 3 ? 1 : 0) - (y < 2 ? 1 : 0);
                if (toFront == 1 && y >= 3 && y <= 4) {
                    level = 2;
                }
                return SKIN[clamp(level, SKIN.length)];
            }
        }
    }

    /** The heavy brow: deep shadow across the front, catching the light along its top. */
    private static int brow(int face, int x, int y, int w, int h) {
        if (face == FRONT) {
            return skinColour("lkkdkkl".charAt(x));
        }
        return face == TOP ? SKIN[6] : SKIN[3];
    }

    /** The nose, the cheekbones, the chin: lit on top, shadowed beneath. */
    private static int bone(int face, int x, int y, int w, int h) {
        switch (face) {
            case TOP:
                return SKIN[7];
            case BOTTOM:
                return SKIN[1];
            case FRONT:
                return SKIN[h >= 3 ? (y == 0 ? 7 : y == 1 ? 6 : 4) : 6];
            default:
                return SKIN[3];
        }
    }

    private static int skinColour(char ch) {
        switch (ch) {
            case 'L': return SKIN[6];
            case 'l': return SKIN[5];
            case 'd': return SKIN[3];
            case 'k': return SKIN[1];
            case 'n': return SKIN[7];
            case 'm': return SKIN[0];
            default: return SKIN[4];
        }
    }

    // ------------------------------------------------------------------ the cloak

    /**
     * Pale ice-blue cloth: lines flowing down it, lighter along its folds, darker on the inside
     * that faces his body; torn into rags along the bottom when it is the hem.
     */
    private static int cloth(int face, int x, int y, int w, int h, boolean hem) {
        int gx = px(face, x), gy = py(face, y);
        if (hem && face != TOP) {
            // Each column torn off at its own length, some into long strands, a few holes above.
            int column = face == BOTTOM ? gx : gx;
            int torn = 1 + (int) (rand(column, 0, 21) * 4);
            if (rand(column, 0, 22) < 0.2) {
                torn = 0;
            }
            if (face != BOTTOM && y >= h - torn) {
                return -1;
            }
            if (face != BOTTOM && y >= h / 2 && rand(gx, gy, 23) < 0.07) {
                return -1;
            }
        }
        if (face == BOTTOM && hem) {
            return -1;
        }
        double flow = Math.sin(gx * 0.9 + fbm(gx / 6.0, gy / 9.0, 3, 31) * 5.0);
        int level = 4 + (flow > 0.7 ? 1 : 0) - (flow < -0.75 ? 2 : 0);
        if (rand(gx, gy, 24) < 0.08) {
            level++;
        }
        if (y == 0 && face != BOTTOM) {
            level++;
        }
        if (face == FRONT) {
            // The inside, in its own shadow.
            level -= 2;
        }
        if (face == LEFT || face == RIGHT) {
            level--;
        }
        int c = CLOTH[clamp(level, CLOTH.length)];
        if (hem && face != BOTTOM && face != TOP) {
            // Darker toward the torn edge.
            int torn = 1 + (int) (rand(gx, 0, 21) * 4);
            if (y >= h - torn - 2) {
                c = mix(c, CLOTH[0], 0.35);
            }
        }
        return c;
    }

    // ------------------------------------------------------------------ painting

    private static void paint(Box b, Painter painter) {
        int u = b.u, v = b.v, w = b.w, h = b.h, d = b.d;
        face(painter, TOP, u + d, v, w, d);
        face(painter, BOTTOM, u + d + w, v, w, d);
        face(painter, RIGHT, u, v + d, d, h);
        face(painter, FRONT, u + d, v + d, w, h);
        face(painter, LEFT, u + d + w, v + d, d, h);
        face(painter, BACK, u + d + w + d, v + d, w, h);
    }

    private static int faceX0, faceY0;

    private static void face(Painter painter, int face, int x0, int y0, int w, int h) {
        faceX0 = x0;
        faceY0 = y0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int c = painter.paint(face, x, y, w, h);
                if (c >= 0) {
                    skin.setRGB(x0 + x, y0 + y, 0xFF000000 | c);
                }
            }
        }
    }

    /** Where a pixel of the current face is on the sheet. */
    private static int px(int face, int x) {
        return faceX0 + x;
    }

    private static int py(int face, int y) {
        return faceY0 + y;
    }

    /** Lights a pixel in one of the glow layers. */
    private static void glow(int x, int y, int rgb, double alpha, boolean first) {
        (first ? glowA : glowB).setRGB(x, y, ((int) (alpha * 255) << 24) | rgb);
    }

    private static double rand(int x, int y, int seed) {
        long h = x * 374761393L + y * 668265263L + seed * 1442695040888963407L;
        h = (h ^ (h >>> 13)) * 1274126177L;
        h ^= h >>> 16;
        return (h & 0xFFFF) / 65536.0;
    }

    private static double noise(double x, double y, int seed) {
        int x0 = (int) Math.floor(x), y0 = (int) Math.floor(y);
        double fx = x - x0, fy = y - y0;
        fx = fx * fx * (3 - 2 * fx);
        fy = fy * fy * (3 - 2 * fy);
        double a = rand(x0, y0, seed), b = rand(x0 + 1, y0, seed), c = rand(x0, y0 + 1, seed), d = rand(x0 + 1, y0 + 1, seed);
        return (a + (b - a) * fx) * (1 - fy) + (c + (d - c) * fx) * fy;
    }

    private static double fbm(double x, double y, int octaves, int seed) {
        double sum = 0, amp = 0.5, norm = 0;
        for (int o = 0; o < octaves; o++) {
            sum += amp * noise(x, y, seed + o);
            norm += amp;
            amp *= 0.5;
            x *= 2;
            y *= 2;
        }
        return sum / norm;
    }

    private static int mix(int a, int b, double t) {
        t = Math.max(0, Math.min(1, t));
        int r = (int) Math.round(((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
        int g = (int) Math.round(((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
        int bl = (int) Math.round((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return r << 16 | g << 8 | bl;
    }

    private static int clamp(int level, int length) {
        return Math.max(0, Math.min(length - 1, level));
    }
}
