import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;

/**
 * Paints the Champion of the Universe's 128x128 skin and its two companions, and Tamarata's
 * icon for Galacticraft's celestial map:
 * <ul>
 * <li><b>champion.png</b>: as he stands. Pale blue-grey skin, a great swept-back red mane, red
 * brows over hard eyes, the Power Stone set in his brow. A navy sleeveless muscle top cut in a
 * deep V, a high gold collar, a gold championship belt with a black-stone plate, gold engraved
 * cuffs at his wrists, red tights and red boots.</li>
 * <li><b>champion_rage.png</b>: what his rage lights: his eyes, and the veins standing out on
 * his arms, neck and chest. Drawn additively, stronger the angrier he is.</li>
 * <li><b>champion_stone.png</b>: the stone in his brow, and its light on his forehead. Drawn
 * additively while he wears it.</li>
 * <li><b>tamarata.png</b>: the planet, 16x16, for the celestial map.</li>
 * </ul>
 * The box layout here must match client/render/ModelChampion.java. In every front face the left
 * of the picture is his right side, as when you face him.
 *
 * Usage: java ChampionSkin &lt;entity texture dir&gt; &lt;celestial body texture dir&gt;
 * (src/main/resources/assets/eternity/textures/entity and .../textures/gui/celestialbodies)
 */
public class ChampionSkin {

    private static final int[] SKIN = {0x3A4252, 0x515B72, 0x68748C, 0x808CA2, 0x99A4B8, 0xB3BCCC, 0xCDD4E0};
    private static final int[] HAIR = {0x420908, 0x66110D, 0x8C1A13, 0xB2261B, 0xCE3824, 0xE45A38, 0xF4885C};
    private static final int[] NAVY = {0x080B1C, 0x0F152E, 0x161F44, 0x1F2C58, 0x2A3A6E, 0x384A86, 0x4C60A0};
    private static final int[] GOLD = {0x4A3006, 0x74500E, 0xA07418, 0xC89828, 0xE6BC44, 0xF4D677, 0xFFF0B8};
    private static final int[] RED = {0x360506, 0x56090B, 0x781014, 0x9A171B, 0xBA2327, 0xD43E3C, 0xEA685E};
    private static final int[] BLACK = {0x050507, 0x0C0C10, 0x15151B, 0x1F1F27, 0x2C2C36, 0x40404C, 0x585866};
    private static final int[] STONE = {0x3A0A40, 0x5E1468, 0x8A1E96, 0xB028BE, 0xC32AD1, 0xE070F0, 0xF8C8FF};
    private static final int EYE_WHITE = 0xDADFEA, PUPIL = 0x1A1424;
    private static final int VEIN = 0xE040FF, EYE_GLOW = 0xF6B8FF;

    private static final int TOP = 0, BOTTOM = 1, RIGHT = 2, FRONT = 3, LEFT = 4, BACK = 5;

    private static final Random RANDOM = new Random(1933L);
    private static BufferedImage skin, rage, stone;

    /** Paints one face of a box: given the face and a pixel in it, a colour, or -1 for none. */
    private interface Painter {
        int paint(int face, int x, int y, int w, int h);
    }

    public static void main(String[] args) throws Exception {
        File out = new File(args[0]);
        out.mkdirs();
        skin = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        rage = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        stone = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);

        head();
        mane();
        torso();
        arms();
        legs();

        ImageIO.write(skin, "png", new File(out, "champion.png"));
        ImageIO.write(rage, "png", new File(out, "champion_rage.png"));
        ImageIO.write(stone, "png", new File(out, "champion_stone.png"));
        if (args.length > 1) {
            File icons = new File(args[1]);
            icons.mkdirs();
            ImageIO.write(planet(), "png", new File(icons, "tamarata.png"));
        }
    }

    // ------------------------------------------------------------------ the head

    private static void head() {
        box(38, 0, 7, 7, 7, (face, x, y, w, h) -> {
            switch (face) {
                case TOP:
                    return streak(HAIR, 3, x, y);
                case BACK:
                    return streak(HAIR, y < 5 ? 3 : 2, x, y);
                case RIGHT:
                case LEFT: {
                    // The mane swept back over the top of the ear; the ear; the heavy jaw.
                    int toFront = face == RIGHT ? x : w - 1 - x;
                    if (y < 2 || toFront > 4) {
                        return streak(HAIR, y < 2 ? 3 : 2, x, y);
                    }
                    if (toFront >= 2 && toFront <= 3 && y >= 3 && y <= 4) {
                        return SKIN[y == 3 ? 3 : 2];
                    }
                    return shade(SKIN, y >= 5 ? 3 : 4, x, y, w, h);
                }
                case BOTTOM:
                    return SKIN[2];
                default:
                    return -1;
            }
        });
        // His face. s/S/d skin, b brow, W white of an eye, P pupil, n nose, m mouth, j jaw shadow.
        art(45, 7, new String[]{
                "dsSSSsd",
                "sSSsSSs",
                "bbbsbbb",
                "sWPsPWs",
                "dssnssd",
                "jsmmmsj",
                "jjsssjj"});
        // His eyes burn with his rage.
        rageAt(46, 10, EYE_GLOW, 0.95F);
        rageAt(47, 10, EYE_GLOW, 0.95F);
        rageAt(49, 10, EYE_GLOW, 0.95F);
        rageAt(50, 10, EYE_GLOW, 0.95F);
        // Veins at his temples.
        rageAt(45, 8, VEIN, 0.6F);
        rageAt(51, 8, VEIN, 0.6F);
        rageAt(45, 7, VEIN, 0.35F);
        rageAt(51, 7, VEIN, 0.35F);
        // The heavy brow ridge, red brows knotted across it.
        box(98, 6, 7, 1, 1, (face, x, y, w, h) -> face == FRONT ? (x == 3 ? SKIN[3] : HAIR[x == 0 || x == 6 ? 1 : 2])
                : face == TOP ? SKIN[4] : SKIN[3]);
        // The stone in his brow.
        box(120, 0, 1, 1, 1, (face, x, y, w, h) -> face == FRONT ? STONE[5] : face == TOP ? STONE[4] : STONE[2]);
        for (int x = 120; x < 124; x++) {
            for (int y = 0; y < 2; y++) {
                stoneAt(x, y, 0xF060FF, 1F);
            }
        }
        // Its light on his forehead, under the stone.
        stoneAt(48, 7, 0xC32AD1, 0.45F);
        stoneAt(47, 7, 0xC32AD1, 0.2F);
        stoneAt(49, 7, 0xC32AD1, 0.2F);
        stoneAt(48, 8, 0xC32AD1, 0.2F);
    }

    // ------------------------------------------------------------------ the mane

    private static void mane() {
        // The crown, swept straight back off his forehead, its front edge standing up.
        box(66, 0, 8, 3, 8, (face, x, y, w, h) -> {
            switch (face) {
                case TOP:
                    return streak(HAIR, 4 + (y < 2 ? 1 : 0), x, y);
                case FRONT:
                    return streak(HAIR, y == 0 ? 6 : y == 1 ? 5 : 3, x, y);
                case BACK:
                    return streak(HAIR, 3, x, y);
                case BOTTOM:
                    return HAIR[1];
                default:
                    return streak(HAIR, 4 - y, x, y);
            }
        });
        // Full at the sides.
        box(100, 16, 1, 5, 6, (face, x, y, w, h) -> streak(HAIR, face == TOP ? 4 : y < 2 ? 4 : 3, x, y));
        // The long locks behind: lighter where they catch the light, darker underneath.
        for (int[] seg : new int[][]{{0, 42, 10}, {24, 42, 8}}) {
            box(seg[0], seg[1], seg[2], 3, 2, (face, x, y, w, h) -> {
                switch (face) {
                    case BACK:
                        return streak(HAIR, y < 1 ? 4 : 3, x, y);
                    case FRONT:
                        return streak(HAIR, 2, x, y);
                    case TOP:
                        return streak(HAIR, 4, x, y);
                    case BOTTOM:
                        return streak(HAIR, seg[2] == 8 ? 5 : 2, x, y);
                    default:
                        return streak(HAIR, 3, x, y);
                }
            });
        }
    }

    // ------------------------------------------------------------------ the torso

    private static void torso() {
        // Chest: the navy top, cut low; bare skin at his shoulders where it is sleeveless.
        box(0, 0, 12, 8, 7, (face, x, y, w, h) -> {
            switch (face) {
                case TOP:
                    return y < 3 ? shade(SKIN, 4, x, y, w, h) : (x < 2 || x > 9 ? shade(SKIN, 4, x, y, w, h) : shade(NAVY, 3, x, y, w, h));
                case BACK: {
                    // Lats spreading out from his spine.
                    int fromSpine = Math.abs(x * 2 - 11);
                    return y < 1 ? SKIN[3] : shade(NAVY, fromSpine > 7 && y < 5 ? 4 : 3, x, y, w, h);
                }
                case RIGHT:
                case LEFT: {
                    int toFront = face == RIGHT ? x : w - 1 - x;
                    // The armhole: skin showing high on his side.
                    if (y < 3 && toFront > 1 && toFront < 6) {
                        return shade(SKIN, 3, x, y, w, h);
                    }
                    return shade(NAVY, 3, x, y, w, h);
                }
                case BOTTOM:
                    return NAVY[1];
                default: {
                    // The V of the neckline, deep, down between the pecs.
                    double half = (5 - y) * 1.1D;
                    if (y < 5 && Math.abs(x - 5.5D) < half) {
                        return shade(SKIN, Math.abs(x - 5.5D) < 1D ? 3 : 4, x, y, w, h);
                    }
                    return shade(NAVY, y > 5 ? 2 : 3, x, y, w, h);
                }
            }
        });
        // The pecs over it, carrying the same V; the line where they meet.
        box(60, 16, 12, 4, 1, (face, x, y, w, h) -> {
            if (face == FRONT) {
                double half = (4.4D - y) * 1.25D;
                if (Math.abs(x - 5.5D) < half) {
                    return x == 5 || x == 6 ? SKIN[y < 2 ? 3 : 2] : shade(SKIN, 4, x, y, w, h);
                }
                // Light across the top of each pec, shadow underneath.
                return NAVY[y == 0 ? 5 : y == 3 ? 2 : (x == 0 || x == 11 ? 3 : 4)];
            }
            if (face == TOP) {
                return x > 3 && x < 8 ? SKIN[4] : NAVY[4];
            }
            return face == BOTTOM ? NAVY[1] : NAVY[3];
        });
        // The neck and traps, bare, veined.
        box(60, 21, 8, 2, 4, (face, x, y, w, h) -> shade(SKIN, face == TOP ? 5 : 4, x, y, w, h));
        rageAt(66, 26, VEIN, 0.7F);
        rageAt(67, 25, VEIN, 0.5F);
        rageAt(69, 26, VEIN, 0.7F);
        rageAt(70, 25, VEIN, 0.5F);
        // The lats, flaring out under his arms.
        box(86, 16, 2, 5, 5, (face, x, y, w, h) -> shade(NAVY, face == TOP ? 3 : 3, x, y, w, h));
        // The veins on his chest where the V shows it.
        rageAt(66, 18, VEIN, 0.5F);
        rageAt(67, 19, VEIN, 0.5F);
        rageAt(70, 18, VEIN, 0.5F);
        rageAt(70, 19, VEIN, 0.4F);

        // Abdomen: the top runs down to his belt.
        box(0, 16, 9, 5, 5, (face, x, y, w, h) -> {
            if (face == FRONT) {
                // The shapes of his abdominals pressing through it.
                boolean groove = x == 4 || y == 1 || y == 3;
                return NAVY[groove ? 2 : (x == 0 || x == 8 ? 2 : 3)];
            }
            return shade(NAVY, 3, x, y, w, h);
        });

        // The championship belt: gold, engraved, studded.
        box(28, 16, 10, 3, 6, (face, x, y, w, h) -> {
            if (face == FRONT || face == BACK || face == RIGHT || face == LEFT) {
                if (y == 0 || y == h - 1) {
                    return GOLD[y == 0 ? 6 : 2];
                }
                return (x + y) % 3 == 0 ? GOLD[5] : x % 2 == 0 ? GOLD[3] : GOLD[4];
            }
            return face == TOP ? GOLD[5] : GOLD[2];
        });
        // Its plate: black stone in a gold rim.
        box(114, 16, 4, 3, 1, (face, x, y, w, h) -> {
            if (face == FRONT) {
                if (x == 0 || x == w - 1 || y == 0 || y == h - 1) {
                    return GOLD[(x + y) % 2 == 0 ? 6 : 4];
                }
                return x == 1 ? BLACK[5] : BLACK[2];
            }
            return GOLD[4];
        });
        // The high collar, gold, engraved with a line inside its rim.
        box(98, 0, 10, 5, 1, (face, x, y, w, h) -> {
            if (face == FRONT || face == BACK) {
                boolean rim = y == 0 || x == 0 || x == w - 1;
                boolean line = y == 1 && x > 0 && x < w - 1 || (x == 1 || x == w - 2) && y > 0;
                return rim ? GOLD[6] : line ? GOLD[2] : GOLD[face == FRONT ? 3 : 4];
            }
            return GOLD[5];
        });
        box(114, 6, 1, 4, 5, (face, x, y, w, h) -> {
            boolean rim = y == 0 || (face == RIGHT || face == LEFT) && (x == 0 || x == w - 1);
            return rim ? GOLD[6] : (x + y) % 2 == 0 ? GOLD[4] : GOLD[3];
        });
    }

    // ------------------------------------------------------------------ the arms

    private static void arms() {
        // Upper arm: bare, enormous; the line between biceps and triceps.
        box(0, 30, 5, 7, 5, (face, x, y, w, h) -> {
            switch (face) {
                case FRONT:
                    return shade(SKIN, y < 4 ? 5 : 4, x, y, w, h);
                case BACK:
                    return shade(SKIN, 3, x, y, w, h);
                case RIGHT:
                case LEFT: {
                    int toFront = face == RIGHT ? x : w - 1 - x;
                    return toFront == 2 && y > 1 ? SKIN[2] : shade(SKIN, 4, x, y, w, h);
                }
                default:
                    return SKIN[3];
            }
        });
        // Veins down the biceps.
        int[][] upper = {{6, 37}, {6, 38}, {7, 39}, {7, 40}, {8, 41}, {1, 37}, {1, 38}, {2, 39}, {11, 37}, {12, 38}, {12, 39}};
        for (int[] v : upper) {
            rageAt(v[0], v[1], VEIN, 0.8F);
        }
        // The shoulder, round as a boulder.
        box(84, 30, 6, 3, 6, (face, x, y, w, h) -> shade(SKIN, face == TOP ? 5 : y == 0 ? 5 : 4, x, y, w, h));
        rageAt(92, 37, VEIN, 0.5F);
        rageAt(93, 38, VEIN, 0.5F);
        // Forearm: bare, veined, thick as a tree.
        box(20, 30, 5, 6, 5, (face, x, y, w, h) -> {
            switch (face) {
                case FRONT:
                    return shade(SKIN, 4, x, y, w, h);
                case BACK:
                    return shade(SKIN, 3, x, y, w, h);
                case TOP:
                case BOTTOM:
                    return SKIN[3];
                default:
                    return shade(SKIN, 4, x, y, w, h);
            }
        });
        int[][] fore = {{26, 35}, {27, 36}, {27, 37}, {26, 38}, {28, 39}, {21, 35}, {22, 36}, {22, 37}, {31, 36}, {32, 37}, {32, 38}, {36, 35}, {37, 37}};
        for (int[] v : fore) {
            rageAt(v[0], v[1], VEIN, 0.85F);
        }
        // The cuffs: gold, engraved with a band of chevrons.
        box(40, 30, 6, 3, 6, (face, x, y, w, h) -> {
            if (face == TOP || face == BOTTOM) {
                return GOLD[face == TOP ? 5 : 2];
            }
            if (y == 0 || y == h - 1) {
                return GOLD[y == 0 ? 6 : 3];
            }
            return (x % 3 == 1) ? GOLD[2] : GOLD[x % 2 == 0 ? 4 : 5];
        });
        // Fists, knuckles forward.
        box(64, 30, 5, 3, 5, (face, x, y, w, h) -> {
            if (face == BOTTOM) {
                // The knuckles: the end of his fist, where it hits.
                return x % 2 == 0 ? SKIN[5] : SKIN[3];
            }
            if (face == FRONT) {
                return y == 2 ? SKIN[3] : shade(SKIN, 4, x, y, w, h);
            }
            return shade(SKIN, 3, x, y, w, h);
        });
    }

    // ------------------------------------------------------------------ the legs

    private static void legs() {
        // Thighs and shins: red tights, a seam down the outside.
        for (int u : new int[]{44, 64}) {
            box(u, 42, 5, 6, 5, (face, x, y, w, h) -> {
                if (face == RIGHT || face == LEFT) {
                    int toFront = face == RIGHT ? x : w - 1 - x;
                    return toFront == 2 ? RED[2] : shade(RED, 3, x, y, w, h);
                }
                if (face == FRONT) {
                    return shade(RED, u == 44 && y < 3 ? 5 : 4, x, y, w, h);
                }
                return shade(RED, 3, x, y, w, h);
            });
        }
        // Boots: red, glossier and darker, a turned-down top.
        box(84, 42, 6, 4, 6, (face, x, y, w, h) -> {
            if (face == TOP) {
                return RED[2];
            }
            if (face == BOTTOM) {
                return RED[0];
            }
            if (y == 0) {
                return RED[5];
            }
            if (face == FRONT && x > 1 && x < 4) {
                return RED[y == 1 ? 6 : 5];
            }
            return shade(RED, 4, x, y, w, h);
        });
        box(28, 25, 6, 2, 2, (face, x, y, w, h) -> face == TOP ? RED[5] : face == BOTTOM ? RED[0] : shade(RED, 4, x, y, w, h));
    }

    // ------------------------------------------------------------------ the planet icon

    /** Tamarata as the celestial map shows it: a red-orange world, banded with dust, scarred. A square tile, not a disc. */
    private static BufferedImage planet() {
        BufferedImage icon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        int[] ramp = {0x3A1408, 0x5E200E, 0x8A3416, 0xB04E22, 0xD06A30, 0xE88C48, 0xF6B070};
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                double dx = x - 7.5D, dy = y - 7.5D;
                // Square, like every body in Tamarata's sky: the whole tile, darkening toward its edges.
                double d = Math.max(Math.abs(dx), Math.abs(dy));
                // Lit from the upper left; bands of dust across it; a dark crater.
                double light = 0.55D - (dx + dy) * 0.045D - Math.max(0D, d - 5.5D) * 0.12D;
                int band = (int) Math.floor((y + Math.sin(x * 0.7D) * 0.8D) / 2.5D) % 2;
                int level = (int) Math.round(light * 6D) + (band == 0 ? 0 : -1);
                if (Math.hypot(x - 10D, y - 5D) < 1.6D) {
                    level -= 2;
                }
                int c = ramp[clamp(level)];
                icon.setRGB(x, y, 0xFF000000 | c);
            }
        }
        return icon;
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

    /** Hair: thick locks combed one way, alternate columns a shade apart, a few lighter strands catching the light. */
    private static int streak(int[] ramp, int level, int x, int y) {
        int l = level + ((x * 7 + 3) % 5 == 0 ? 1 : x % 2 == 0 ? 0 : -1);
        if (RANDOM.nextFloat() < 0.12F) {
            l += 1;
        }
        return ramp[clamp(l)];
    }

    /** Hand-placed pixels of the face: s/S/d skin, j jaw shadow, b brow, W white, P pupil, n nose, m mouth. */
    private static void art(int x, int y, String[] rows) {
        for (int j = 0; j < rows.length; j++) {
            for (int i = 0; i < rows[j].length(); i++) {
                int c = faceColour(rows[j].charAt(i));
                if (c >= 0) {
                    set(x + i, y + j, c);
                }
            }
        }
    }

    private static int faceColour(char ch) {
        switch (ch) {
            case 's': return SKIN[4];
            case 'S': return SKIN[5];
            case 'd': return SKIN[3];
            case 'j': return SKIN[2];
            case 'b': return HAIR[2];
            case 'W': return EYE_WHITE;
            case 'P': return PUPIL;
            case 'n': return SKIN[3];
            case 'm': return 0x4A3446;
            default: return -1;
        }
    }

    private static void rageAt(int x, int y, int rgb, float alpha) {
        rage.setRGB(x, y, ((int) (alpha * 255F) << 24) | rgb);
    }

    private static void stoneAt(int x, int y, int rgb, float alpha) {
        stone.setRGB(x, y, ((int) (alpha * 255F) << 24) | rgb);
    }

    private static int clamp(int level) {
        return Math.max(0, Math.min(6, level));
    }

    private static void set(int x, int y, int rgb) {
        skin.setRGB(x, y, 0xFF000000 | rgb);
    }
}
