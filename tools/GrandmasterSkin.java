import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;

/**
 * Paints the Grandmaster and everything of his:
 * <ul>
 * <li><b>entity/grandmaster.png</b> (128x128): blue skin, silver hair swept back off a high
 * forehead with a lock down each side of the face and a mane down his back, white eyes and a
 * thin grin. A dark suit under a gold breastplate and a high flared gold collar, an engraved
 * pauldron on the left shoulder, a gold cord across the chest. Bare arms, coils of gold up the
 * right forearm and a bangle on the left wrist. A gold belt with a red jewel and a ring of
 * brass keys, long blue coat tails, dark boots with gold cuffs.</li>
 * <li><b>entity/grandmaster_glow.png</b>: what lights from within: his eyes, the jewel, the
 * edges of the gold. Drawn additively.</li>
 * <li><b>entity/grandmaster_cracks.png</b>: light breaking through him along fissures, as his
 * board breaks and his life with it.</li>
 * <li><b>entity/grandmaster_cards.png</b> (128x32): eight cards, sixteen by twenty-four: a
 * red back, then faces.</li>
 * <li><b>entity/tafl_raider.png, tafl_guard.png, tafl_king.png</b> (64x64): his pieces, one
 * layout for all three.</li>
 * <li><b>blocks/board_*.png</b> (16x16): the squares of his board, its rim and its sides.</li>
 * </ul>
 * The box layouts here must match client/render/ModelGrandmaster.java and ModelTaflPiece.java.
 * In every front face the left of the picture is his right side, as when you face him.
 *
 * Usage: java GrandmasterSkin <textures dir>   (src/main/resources/assets/eternity/textures)
 */
public class GrandmasterSkin {

    private static final int[] SKIN = {0x24306A, 0x34468E, 0x4A62B2, 0x6480CC, 0x7F9CDC, 0xA2BBEC, 0xC8D7F6};
    private static final int[] HAIR = {0x55586C, 0x7E8298, 0xA4A8BC, 0xC2C5D4, 0xD8DAE6, 0xEAECF3, 0xFAFBFF};
    private static final int[] GOLD = {0x4A2C06, 0x7A500C, 0xA67614, 0xD09C26, 0xECBF46, 0xF8DB80, 0xFFF3C6};
    private static final int[] SUIT = {0x07080F, 0x0E1120, 0x161B30, 0x1F2640, 0x293252, 0x343F64, 0x445078};
    private static final int[] CAPE = {0x0E1438, 0x172258, 0x213276, 0x2C4494, 0x3C58AE, 0x5274C4, 0x7092D6};
    private static final int[] JEWEL = {0x4A0606, 0x7A0C0C, 0xA81414, 0xD42222, 0xF04838, 0xFF8A70, 0xFFD0C0};
    private static final int[] BRASS = {0x3E2A0A, 0x664416, 0x8E6420, 0xB4862E, 0xD0A64A, 0xE6C678, 0xF6E2B0};
    private static final int EYE = 0xF2F5FF, TEETH = 0xE6E2D8, LIP = 0x23306A;

    private static final int TOP = 0, BOTTOM = 1, RIGHT = 2, FRONT = 3, LEFT = 4, BACK = 5;

    private static final Random RANDOM = new Random(1966L);
    private static BufferedImage skin, glow, cracks;

    private interface Painter {
        int paint(int face, int x, int y, int w, int h);
    }

    public static void main(String[] args) throws Exception {
        File root = new File(args[0]);
        File entity = new File(root, "entity"), blocks = new File(root, "blocks");
        entity.mkdirs();
        blocks.mkdirs();

        skin = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        glow = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        head();
        hair();
        torso();
        arms();
        legs();
        tails();
        ImageIO.write(skin, "png", new File(entity, "grandmaster.png"));
        ImageIO.write(glow, "png", new File(entity, "grandmaster_glow.png"));
        cracks = fractures();
        ImageIO.write(cracks, "png", new File(entity, "grandmaster_cracks.png"));
        ImageIO.write(cards(), "png", new File(entity, "grandmaster_cards.png"));
        for (int kind = 0; kind < 3; kind++) {
            ImageIO.write(piece(kind), "png", new File(entity, new String[]{"tafl_raider.png", "tafl_guard.png", "tafl_king.png"}[kind]));
        }
        board(blocks);
    }

    // ------------------------------------------------------------------ the head

    private static void head() {
        box(0, 0, 8, 8, 8, (face, x, y, w, h) -> {
            switch (face) {
                case TOP:
                    return streak(HAIR, 4, x, y);
                case BACK:
                    return streak(HAIR, y < 2 ? 4 : 3, x, y);
                case RIGHT:
                case LEFT: {
                    int toFront = face == RIGHT ? x : w - 1 - x;
                    // Hair swept back over the ear; the hollow of the cheek; a long jaw.
                    if (y < 2 || toFront < 3 && y < 5) {
                        return streak(HAIR, y < 2 ? 4 : 3, x, y);
                    }
                    if (toFront == 3 && y >= 3 && y <= 4) {
                        return SKIN[2];
                    }
                    if (y >= 5 && toFront > 4) {
                        return SKIN[2];
                    }
                    return shade(SKIN, toFront > 5 ? 4 : 3, x, y, w, h);
                }
                case BOTTOM:
                    return SKIN[1];
                default:
                    return -1;
            }
        });
        // His face. h hair, s/S/d skin, c hollow of the cheek, b brow, W eye, n nose, m lips, T teeth.
        art(8, 8, new String[]{
                "hhhhhhhh",
                "hdSHHSdh",
                "dSSSSSSd",
                "bbsSSsbb",
                "dWWnnWWd",
                "csSnnSsc",
                "dmTTTTmd",
                "ddcSScdd"});
        // White eyes, lit from within.
        for (int x : new int[]{9, 10, 13, 14}) {
            glowAt(x, 12, 0xF4F7FF, 0.95F);
        }
        glowAt(8, 12, 0xC8D4FF, 0.3F);
        glowAt(15, 12, 0xC8D4FF, 0.3F);
    }

    private static void hair() {
        // The crown: combed straight back, the light catching it in long streaks.
        box(32, 0, 9, 2, 9, (face, x, y, w, h) -> {
            switch (face) {
                case TOP:
                    return streak(HAIR, 4 + (y < 2 ? 1 : 0) - (y > 6 ? 1 : 0), x, y);
                case FRONT:
                    return streak(HAIR, 5 - y, x, y);
                case BOTTOM:
                    return HAIR[1];
                default:
                    return streak(HAIR, 4 - y, x, y);
            }
        });
        box(68, 0, 9, 5, 2, (face, x, y, w, h) -> streak(HAIR, face == BOTTOM ? 1 : 3 - (y > 3 ? 1 : 0), x, y));
        // The locks down each side of his face, thick at the temple, thinning to the shoulder.
        for (int u : new int[]{80, 88}) {
            box(u, 16, 1, 8, 3, (face, x, y, w, h) -> {
                if (face == TOP || face == BOTTOM) {
                    return HAIR[2];
                }
                int level = 4 - y / 3 + ((x + y) % 3 == 0 ? 1 : 0);
                return streak(HAIR, level, x, y);
            });
        }
        // The mane down his back, darker underneath, the ends wisping.
        int[][] uv = {{90, 0}, {90, 7}, {68, 7}};
        int[] w = {10, 9, 7};
        int[] h = {6, 6, 5};
        for (int i = 0; i < 3; i++) {
            final int seg = i;
            box(uv[i][0], uv[i][1], w[i], h[i], 1, (face, x, y, ww, hh) -> {
                if (face == FRONT) {
                    return streak(HAIR, 2, x, y);
                }
                if (face == BOTTOM && seg == 2) {
                    return HAIR[2];
                }
                int level = 4 - seg + (x % 3 == 1 ? 1 : 0) - (seg == 2 && y >= hh - 2 && x % 2 == 0 ? 1 : 0);
                return streak(HAIR, level, x, y);
            });
        }
    }

    // ------------------------------------------------------------------ the torso

    private static void torso() {
        // Chest: the dark suit; the breastplate is a box of its own, standing proud of it.
        box(0, 16, 9, 7, 5, (face, x, y, w, h) -> {
            switch (face) {
                case TOP:
                    return x >= 3 && x <= 5 && y < 3 ? SKIN[3] : shade(SUIT, 3, x, y, w, h);
                case FRONT:
                    // Showing around the plate: gold at the shoulders, the dark suit under the arms.
                    return y == 0 ? GOLD[4] : x == 0 || x == w - 1 ? SUIT[3] : GOLD[2];
                case BACK:
                    return x == 4 ? GOLD[2] : shade(SUIT, 3, x, y, w, h);
                default:
                    return y == 0 ? GOLD[3] : shade(SUIT, 3, x, y, w, h);
            }
        });
        // The breastplate: a gold V converging on a ridge down the sternum, engraved at the left
        // shoulder, a gold cord running from it across to the right hip.
        box(102, 30, 8, 6, 1, (face, x, y, w, h) -> {
            if (face != FRONT) {
                return GOLD[face == TOP ? 5 : 2];
            }
            return -1;
        });
        art(103, 31, new String[]{
                "YGgdSgGY",
                "GYGgdgYG",
                "gGYGdGeE",
                "dgGYcYeE",
                "SdgGcGgE",
                "SSdgGgdg"});
        // Edges of the plate catch the light.
        for (int x = 103; x < 111; x++) {
            glowAt(x, 31, 0xFFE8A0, 0.25F);
        }
        glowAt(107, 33, 0xFFE8A0, 0.4F);

        // Abdomen: the plate continues down in two columns to the belt.
        box(28, 16, 8, 5, 4, (face, x, y, w, h) -> {
            if (face == FRONT) {
                if (x == 3 || x == 4) {
                    return SUIT[y % 2 == 0 ? 3 : 2];
                }
                if (x == 0 || x == 7) {
                    return SUIT[2];
                }
                return GOLD[y % 2 == 1 ? 2 : x == 2 || x == 5 ? 4 : 3];
            }
            return shade(SUIT, 3, x, y, w, h);
        });
        // Belt: gold, with the jewel set in its buckle.
        box(52, 16, 9, 2, 5, (face, x, y, w, h) -> {
            if (face == FRONT || face == BACK) {
                return y == 0 ? GOLD[x % 3 == 0 ? 6 : 5] : GOLD[x % 2 == 0 ? 3 : 2];
            }
            return face == TOP || face == BOTTOM ? GOLD[2] : GOLD[y == 0 ? 5 : 3];
        });
        box(112, 0, 2, 2, 1, (face, x, y, w, h) -> face == FRONT ? JEWEL[x + y == 1 ? 5 : 3] : GOLD[4]);
        for (int x = 113; x < 115; x++) {
            for (int y = 1; y < 3; y++) {
                glowAt(x, y, 0xFF5040, 0.75F);
            }
        }
        // Keys: a brass ring and two keys.
        box(122, 0, 2, 2, 1, (face, x, y, w, h) -> BRASS[x == 0 ? 3 : 5]);
        box(118, 0, 1, 3, 1, (face, x, y, w, h) -> y == 2 ? BRASS[2] : BRASS[y == 0 ? 5 : 4]);

        // The collar: gold outside, the dark lining inside.
        box(28, 25, 10, 4, 1, (face, x, y, w, h) -> {
            if (face == FRONT) {
                return SUIT[3];
            }
            if (face == BACK) {
                return y == 0 ? GOLD[6] : x == 0 || x == w - 1 ? GOLD[3] : GOLD[(x + y) % 4 == 0 ? 5 : 4];
            }
            return GOLD[4];
        });
        for (int u : new int[]{96, 110}) {
            final boolean right = u == 110;
            box(u, 16, 1, 5, 6, (face, x, y, w, h) -> {
                int outer = right ? RIGHT : LEFT, inner = right ? LEFT : RIGHT;
                if (face == inner) {
                    return SUIT[3];
                }
                if (face == outer) {
                    return y == 0 ? GOLD[6] : x == 0 ? GOLD[5] : GOLD[(x + y) % 3 == 0 ? 5 : 4];
                }
                return GOLD[y == 0 ? 6 : 3];
            });
        }
    }

    // ------------------------------------------------------------------ the arms

    private static void arms() {
        for (int side = 0; side < 2; side++) {
            final boolean right = side == 0;
            final int outer = right ? RIGHT : LEFT, inner = right ? LEFT : RIGHT;
            // Upper arm: bare, long, the shoulder catching the light.
            box(right ? 0 : 12, 30, 3, 6, 3, (face, x, y, w, h) -> {
                if (face == TOP) {
                    return SKIN[5];
                }
                if (face == BOTTOM) {
                    return SKIN[2];
                }
                int l = face == outer ? 4 : face == inner ? 2 : 3;
                return shade(SKIN, l + (y < 2 ? 1 : 0), x, y, w, h);
            });
            // Forearm.
            box(right ? 24 : 36, 30, 3, 6, 3, (face, x, y, w, h) -> {
                if (face == TOP || face == BOTTOM) {
                    return SKIN[3];
                }
                return shade(SKIN, face == outer ? 4 : face == inner ? 2 : 3, x, y, w, h);
            });
            // Hand: long fingers folded, knuckles lighter.
            box(right ? 52 : 62, 23, 3, 3, 2, (face, x, y, w, h) -> {
                if (face == FRONT) {
                    return y == 0 ? SKIN[5] : SKIN[x % 2 == 0 ? 4 : 3];
                }
                if (face == BOTTOM) {
                    return SKIN[3];
                }
                return SKIN[face == TOP ? 2 : 3];
            });
            // Forefinger, with a gold ring on the right.
            box(right ? 110 : 114, 7, 1, 3, 1, (face, x, y, w, h) -> right && y == 1 ? GOLD[5] : SKIN[y == 2 ? 5 : 4]);
        }
        // Coils of gold wound up his right forearm.
        box(48, 30, 4, 4, 4, (face, x, y, w, h) -> {
            if (face == TOP || face == BOTTOM) {
                return GOLD[2];
            }
            return GOLD[y % 2 == 0 ? 5 : 2] - (x == 0 && y % 2 == 0 ? 0x101000 : 0);
        });
        for (int x = 48; x < 64; x++) {
            glowAt(x, 34, 0xFFDC80, 0.3F);
            glowAt(x, 36, 0xFFDC80, 0.3F);
        }
        // A bangle on his left wrist.
        box(64, 30, 4, 2, 4, (face, x, y, w, h) -> GOLD[face == TOP || face == BOTTOM ? 3 : y == 0 ? 5 : 3]);
        // The pauldron on his left shoulder: gold, engraved in curling lines, a bright rim.
        box(80, 30, 5, 3, 6, (face, x, y, w, h) -> {
            if (face == BOTTOM) {
                return SUIT[2];
            }
            if (face == TOP) {
                boolean rim = x == 0 || y == 0 || x == w - 1 || y == h - 1;
                if (rim) {
                    return GOLD[6];
                }
                double swirl = Math.sin(x * 1.7 + y * 0.9) + Math.cos(y * 1.3 - x * 0.6);
                return GOLD[swirl > 0.6 ? 2 : swirl < -0.8 ? 5 : 4];
            }
            return y == 2 ? GOLD[2] : GOLD[y == 0 ? 5 : 4];
        });
    }

    // ------------------------------------------------------------------ the legs

    private static void legs() {
        for (int side = 0; side < 2; side++) {
            final boolean right = side == 0;
            final int outer = right ? RIGHT : LEFT;
            box(right ? 0 : 16, 40, 4, 6, 4, (face, x, y, w, h) -> {
                if (face == outer && x == 1) {
                    return SUIT[5];
                }
                return shade(SUIT, face == FRONT ? 4 : 3, x, y, w, h);
            });
            box(right ? 32 : 48, 40, 4, 6, 4, (face, x, y, w, h) -> shade(SUIT, face == FRONT ? 4 : 3, x, y, w, h));
            // Boots: dark, a gold cuff at the top, a pointed toe.
            box(right ? 64 : 84, 40, 5, 3, 5, (face, x, y, w, h) -> {
                if (face == BOTTOM) {
                    return SUIT[0];
                }
                if (face == TOP) {
                    return GOLD[4];
                }
                if (y == 0) {
                    return GOLD[5];
                }
                return face == FRONT && x == 2 ? SUIT[5] : SUIT[y == 1 ? 3 : 2];
            });
        }
    }

    // ------------------------------------------------------------------ the coat tails

    private static void tails() {
        int[][] uv = {{0, 50}, {22, 50}, {46, 50}};
        for (int i = 0; i < 3; i++) {
            final int seg = i;
            final int width = 10 + i;
            box(uv[i][0], uv[i][1], width, 5, 1, (face, x, y, w, h) -> {
                double fold = Math.sin((x - width / 2.0) * 1.3 + 0.4) + 0.4 * Math.sin((x - width / 2.0) * 0.55);
                int level = (int) Math.round(3.4 + fold * 1.3 - seg * 0.3);
                boolean split = x == width / 2;
                boolean hem = seg == 2 && y == 4;
                switch (face) {
                    case BACK:
                        return hem ? GOLD[3] : split ? CAPE[1] : CAPE[clamp(level)];
                    case FRONT:
                        return hem ? GOLD[2] : SUIT[clamp(level - 1)];
                    case TOP:
                        return CAPE[3];
                    default:
                        return hem ? GOLD[3] : CAPE[2];
                }
            });
        }
    }

    // ------------------------------------------------------------------ light breaking through him

    /** Branching fissures over his skin, face and plate: white at the heart, gold at the edges. */
    private static BufferedImage fractures() {
        BufferedImage out = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        Random r = new Random(77L);
        int[][] seeds = {{12, 13}, {2, 36}, {14, 36}, {26, 36}, {38, 36}, {106, 33}, {4, 22}, {32, 19}, {56, 2}, {20, 12}};
        for (int[] s : seeds) {
            for (int b = 0; b < (s[0] == 12 ? 2 : 3); b++) {
                double x = s[0], y = s[1];
                double a = r.nextDouble() * Math.PI * 2D;
                for (int step = 0; step < 9; step++) {
                    int ix = (int) Math.round(x), iy = (int) Math.round(y);
                    if (ix >= 0 && iy >= 0 && ix < 128 && iy < 128 && (skin.getRGB(ix, iy) >>> 24) != 0) {
                        float alpha = 1F - step / 11F;
                        out.setRGB(ix, iy, ((int) (alpha * 255) << 24) | (step < 3 ? 0xFFFFF4 : 0xFFD878));
                    }
                    a += (r.nextDouble() - 0.5D) * 1.2D;
                    x += Math.cos(a);
                    y += Math.sin(a);
                }
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ cards

    private static BufferedImage cards() {
        BufferedImage img = new BufferedImage(128, 32, BufferedImage.TYPE_INT_ARGB);
        for (int i = 0; i < 8; i++) {
            int x0 = i * 16;
            for (int y = 0; y < 24; y++) {
                for (int x = 0; x < 16; x++) {
                    boolean corner = (x == 0 || x == 15) && (y == 0 || y == 23);
                    if (corner) {
                        continue;
                    }
                    int c;
                    boolean edge = x == 0 || x == 15 || y == 0 || y == 23;
                    if (i == 0) {
                        // The back: a white border, red inside, a lattice of diamonds.
                        boolean border = x <= 1 || x >= 14 || y <= 1 || y >= 22;
                        boolean lattice = ((x + y) % 4 == 0) || ((x - y + 64) % 4 == 0);
                        c = edge ? 0xB8B0A4 : border ? 0xF4EEE2 : lattice ? 0xE04A42 : 0xA81C1C;
                    } else {
                        c = edge ? 0xB8B0A4 : 0xF6F2E8;
                    }
                    img.setRGB(x0 + x, y, 0xFF000000 | c);
                }
            }
            if (i > 0) {
                face(img, x0, i);
            }
        }
        return img;
    }

    private static void face(BufferedImage img, int x0, int which) {
        int red = 0xC81E1E, black = 0x161616, blue = 0x4A7AD8, gold = 0xE0B040;
        String[] spade = {"..X..", ".XXX.", "XXXXX", "XXXXX", "..X.."};
        String[] heart = {".X.X.", "XXXXX", "XXXXX", ".XXX.", "..X.."};
        String[] club = {"..X..", ".XXX.", "XX.XX", "XXXXX", "..X.."};
        String[] diamond = {"..X..", ".XXX.", "XXXXX", ".XXX.", "..X.."};
        String[][] suits = {spade, spade, club, heart, club, spade, diamond, diamond};
        int[] colours = {black, blue, black, red, black, black, red, red};
        String[] suit = suits[which];
        int col = colours[which];
        // The corner index.
        img.setRGB(x0 + 2, 2, 0xFF000000 | col);
        img.setRGB(x0 + 2, 3, 0xFF000000 | col);
        img.setRGB(x0 + 13, 20, 0xFF000000 | col);
        img.setRGB(x0 + 13, 21, 0xFF000000 | col);
        boolean court = which == 2 || which == 3 || which == 4 || which == 7;
        if (court) {
            // A court card: a figure in a frame, robed in the suit's colour with a gold crown.
            for (int y = 5; y < 19; y++) {
                for (int x = 4; x < 12; x++) {
                    boolean frame = x == 4 || x == 11 || y == 5 || y == 18;
                    int c = frame ? gold : y < 7 ? gold : y < 10 ? 0xE8C8A0 : (x + y) % 3 == 0 ? 0x2A4A9A : col;
                    img.setRGB(x0 + x, y, 0xFF000000 | c);
                }
            }
            img.setRGB(x0 + 6, 8, 0xFF161616);
            img.setRGB(x0 + 9, 8, 0xFF161616);
            return;
        }
        // The ace's great pip, drawn in blue like the sigil on his; the others a column of pips.
        int count = which == 1 ? 1 : which == 5 ? 4 : 3;
        for (int k = 0; k < count; k++) {
            int cx = count == 1 ? 6 : 3 + (k % 2) * 5, cy = count == 1 ? 9 : 4 + k * 4;
            int scale = count == 1 ? 1 : 0;
            for (int y = 0; y < 5; y++) {
                for (int x = 0; x < 5; x++) {
                    if (suit[y].charAt(x) == 'X') {
                        if (scale == 1) {
                            img.setRGB(x0 + cx - 2 + x, cy - 2 + y + 2, 0xFF000000 | col);
                        } else {
                            img.setRGB(x0 + cx + x - 1, cy + y, 0xFF000000 | col);
                        }
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ his pieces

    /** 0 a raider, 1 a guard, 2 the king. One layout for all three; see ModelTaflPiece. */
    private static BufferedImage piece(int kind) {
        BufferedImage saved = skin;
        skin = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        int[] body = kind == 0 ? GOLD : CAPE;
        int[] trim = kind == 0 ? CAPE : GOLD;
        int[] stone = {0x1A1A24, 0x262636, 0x34344A, 0x44445E, 0x585874, 0x6E6E8C, 0x8A8AA6};
        // The plinth and its step: dark stone with a band of the side's colour.
        box(0, 0, 12, 2, 12, (face, x, y, w, h) -> face == TOP ? (x == 0 || y == 0 || x == w - 1 || y == h - 1 ? trim[4] : stone[3])
                : face == BOTTOM ? stone[1] : y == 0 ? trim[4] : stone[2]);
        box(0, 14, 10, 2, 10, (face, x, y, w, h) -> face == TOP ? stone[4] : y == 0 ? trim[5] : body[2]);
        // The robe, wide at the hem, folds down it.
        box(0, 26, 8, 8, 8, (face, x, y, w, h) -> {
            if (face == TOP) {
                return body[3];
            }
            if (face == BOTTOM) {
                return body[1];
            }
            if (y == h - 1) {
                return trim[4];
            }
            int fold = (x % 3 == 1) ? 1 : 0;
            if (kind == 2 && face == FRONT && (x == 3 || x == 4)) {
                return GOLD[y % 2 == 0 ? 5 : 4];
            }
            return body[clamp(3 + fold - (y > 5 ? 1 : 0))];
        });
        // The chest: a belt of trim, a breastplate for the raider.
        box(32, 26, 7, 6, 7, (face, x, y, w, h) -> {
            if (face == TOP) {
                return body[4];
            }
            if (y == h - 1) {
                return trim[3];
            }
            if (face == FRONT && kind == 0 && (x + y) % 4 == 0) {
                return body[5];
            }
            return body[clamp(4 - (y > 3 ? 1 : 0) + (x == 3 && face == FRONT ? 1 : 0))];
        });
        // A head without a face: a smooth, featureless ovoid, like all his pieces.
        box(40, 14, 5, 5, 5, (face, x, y, w, h) -> {
            int[] ramp = kind == 0 ? GOLD : HAIR;
            return ramp[clamp(face == TOP ? 5 : 4 - (y > 3 ? 1 : 0) + (face == FRONT && y == 2 ? 1 : 0))];
        });
        // Horns for the raider, a spear for the guard.
        box(48, 0, 1, 4, 1, (face, x, y, w, h) -> HAIR[y == 0 ? 6 : 4 - y / 2]);
        box(60, 0, 1, 24, 1, (face, x, y, w, h) -> y < 4 ? HAIR[y == 0 ? 6 : 5] : y == 4 ? GOLD[5] : BRASS[2 + (y % 5 == 0 ? 1 : 0)]);
        // The shield: round and studded for the raider, a tall tower shield with a cross for the guard; the king's has a crown.
        box(0, 42, 8, 11, 1, (face, x, y, w, h) -> {
            if (face != FRONT) {
                return stone[2];
            }
            if (kind == 0) {
                double dx = x - 3.5, dy = y - 5;
                double r = Math.sqrt(dx * dx + dy * dy);
                if (r > 4.3) {
                    return stone[2];
                }
                return r > 3.4 ? GOLD[5] : r < 1.1 ? GOLD[6] : CAPE[(x + y) % 2 == 0 ? 3 : 2];
            }
            boolean rim = x == 0 || x == w - 1 || y == 0 || y == h - 1;
            boolean cross = x == 3 || x == 4 || y == 4;
            return rim ? GOLD[5] : cross ? GOLD[kind == 2 ? 6 : 4] : CAPE[3];
        });
        // The helm, or the king's crown.
        box(20, 42, 6, 3, 6, (face, x, y, w, h) -> {
            if (kind == 2) {
                if (face == TOP) {
                    return x == 0 || y == 0 || x == w - 1 || y == h - 1 ? GOLD[5] : CAPE[2];
                }
                if (face == BOTTOM) {
                    return GOLD[2];
                }
                if (y == 0) {
                    return x % 2 == 0 ? GOLD[6] : -1;
                }
                return x == 2 && y == 1 ? JEWEL[4] : GOLD[y == 1 ? 5 : 4];
            }
            int[] ramp = kind == 0 ? CAPE : GOLD;
            return ramp[face == TOP ? 5 : y == 2 ? 2 : 4];
        });
        // The king's cape down his back.
        box(44, 39, 8, 12, 1, (face, x, y, w, h) -> {
            if (face == BACK) {
                return y == h - 1 ? GOLD[4] : CAPE[clamp(3 + (x % 3 == 1 ? 1 : 0) - (y > 8 ? 1 : 0))];
            }
            return face == FRONT ? GOLD[2] : CAPE[2];
        });
        // The raider's crest.
        box(0, 54, 1, 3, 5, (face, x, y, w, h) -> JEWEL[face == TOP ? 4 : 3]);
        BufferedImage out = skin;
        skin = saved;
        return out;
    }

    // ------------------------------------------------------------------ the board

    private static void board(File dir) throws Exception {
        tile(dir, "board_gold", (x, y) -> {
            // Polished gold: a soft sheen across it, and a fine engraved lattice.
            double sheen = Math.sin((x + y) * 0.25) * 0.5 + 0.5;
            boolean line = (x + y) % 8 == 0 || (x - y + 16) % 8 == 0;
            int l = line ? 2 : (int) Math.round(3 + sheen * 1.6);
            return GOLD[clamp(l + (RANDOM.nextFloat() < 0.06F ? 1 : 0))];
        });
        tile(dir, "board_blue", (x, y) -> {
            // Deep blue lacquer, faintly veined, a few flecks of gold sunk in it.
            double vein = Math.sin(x * 0.7 + Math.sin(y * 0.4) * 2.0) + Math.cos(y * 0.5 + x * 0.2);
            if (RANDOM.nextFloat() < 0.025F) {
                return GOLD[4];
            }
            return CAPE[clamp((int) Math.round(2 + vein * 0.6))];
        });
        tile(dir, "board_throne", (x, y) -> {
            // Pale gold, a sunburst in every block of it.
            double dx = x - 7.5, dy = y - 7.5;
            double r = Math.sqrt(dx * dx + dy * dy);
            double a = Math.atan2(dy, dx);
            boolean ray = Math.cos(a * 8) > 0.6 && r > 3 && r < 7.5;
            if (r < 2.5) {
                return GOLD[6];
            }
            return ray ? GOLD[6] : GOLD[clamp(4 + (r < 4 ? 1 : 0))];
        });
        tile(dir, "board_refuge", (x, y) -> {
            // The night sky, and a gold star in every block of it.
            double dx = x - 7.5, dy = y - 7.5;
            double ax = Math.abs(dx), ay = Math.abs(dy);
            boolean star = ax + ay < 3.2 || (ax < 0.8 && ay < 6) || (ay < 0.8 && ax < 6);
            if (star) {
                return GOLD[ax + ay < 1.6 ? 6 : 5];
            }
            return RANDOM.nextFloat() < 0.04F ? HAIR[5] : SUIT[clamp(2 + (int) Math.round(Math.sin(x * 0.4 + y * 0.3)))];
        });
        tile(dir, "board_rim", (x, y) -> {
            // Dark bronze with a gold rule and a row of rivets.
            if (y == 1 || y == 14) {
                return GOLD[5];
            }
            if ((y == 4 || y == 11) && x % 4 == 1) {
                return GOLD[4];
            }
            return BRASS[clamp(1 + (int) Math.round(Math.sin(x * 0.5) * 0.6) + (RANDOM.nextFloat() < 0.08F ? 1 : 0))];
        });
        tile(dir, "board_side", (x, y) -> y == 0 ? GOLD[5] : y == 1 ? GOLD[2] : SUIT[clamp(2 + (x % 8 == 0 ? 1 : 0))]);
    }

    private interface Tile {
        int paint(int x, int y);
    }

    private static void tile(File dir, String name, Tile tile) throws Exception {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                img.setRGB(x, y, 0xFF000000 | tile.paint(x, y));
            }
        }
        ImageIO.write(img, "png", new File(dir, name + ".png"));
    }

    // ------------------------------------------------------------------ painting

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
        if (RANDOM.nextFloat() < 0.1F) {
            l += RANDOM.nextBoolean() ? 1 : -1;
        }
        return ramp[clamp(l)];
    }

    private static int streak(int[] ramp, int level, int x, int y) {
        int l = level + ((x * 5 + 2) % 4 == 0 ? 1 : x % 2 == 0 ? 0 : -1);
        if (RANDOM.nextFloat() < 0.12F) {
            l += 1;
        }
        return ramp[clamp(l)];
    }

    /**
     * Hand-placed pixels. Face: h/H hair, s/S/d skin, c hollow, b brow, W eye, n nose, m lip,
     * T teeth. Plate: Y/G/g/d gold light to dark, S suit, c the cord, e/E engraving.
     * A string is plate art if it contains Y; otherwise it is the face.
     */
    private static void art(int x, int y, String[] rows) {
        boolean plate = false;
        for (String row : rows) {
            plate |= row.indexOf('Y') >= 0;
        }
        for (int j = 0; j < rows.length; j++) {
            for (int i = 0; i < rows[j].length(); i++) {
                int c = plate ? plateColour(rows[j].charAt(i)) : faceColour(rows[j].charAt(i));
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
            case 'c': return SKIN[2];
            case 'b': return SKIN[1];
            case 'W': return EYE;
            case 'n': return SKIN[3];
            case 'm': return LIP;
            case 'T': return TEETH;
            default: return -1;
        }
    }

    private static int plateColour(char ch) {
        switch (ch) {
            case 'Y': return GOLD[6];
            case 'G': return GOLD[5];
            case 'g': return GOLD[3];
            case 'd': return GOLD[2];
            case 'S': return SUIT[3];
            case 'c': return BRASS[5];
            case 'e': return GOLD[2];
            case 'E': return GOLD[4];
            default: return -1;
        }
    }

    private static void glowAt(int x, int y, int rgb, float alpha) {
        glow.setRGB(x, y, ((int) (alpha * 255F) << 24) | rgb);
    }

    private static int clamp(int level) {
        return Math.max(0, Math.min(6, level));
    }

    private static void set(int x, int y, int rgb) {
        skin.setRGB(x, y, 0xFF000000 | rgb);
    }
}
