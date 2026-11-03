import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Paints the blocks of the oath's village and its barrow (16x16 each, into textures/blocks):
 * <ul>
 * <li><b>rune_stone.png</b>: the face of an old keeper's stone: grey granite, lichened, a carved
 * serpent-band running up one side, over the top and down the other, and a column of runes inside
 * it, the cuts dark with their edges catching the light.</li>
 * <li><b>rune_stone_fresh.png</b>: a stone cut this year: paler, clean, the band and the runes
 * painted red and black the way they were meant to be.</li>
 * <li><b>rune_stone_side.png</b>: the stone's sides and top, rough-dressed.</li>
 * <li><b>barrow_seal.png</b> and <b>barrow_seal_lit.png</b>: the barrow's doors: dark dressed stone
 * with a rune cut deep in it, between bands of knotwork; lit, the cuts hold a cold light.</li>
 * </ul>
 * Usage: javac -d out tools/BarrowArt.java then java -cp out BarrowArt src/main/resources/assets/eternity/textures/blocks
 */
public class BarrowArt {

    private static final int[] GRANITE = {0x2E2F31, 0x3B3D40, 0x4A4C4F, 0x5A5C5F, 0x6B6D70, 0x7E8083, 0x94969A};
    private static final int[] PALE = {0x5E5C58, 0x74726D, 0x8A8883, 0x9F9D98, 0xB3B1AC, 0xC6C4BF, 0xD8D6D1};
    private static final int[] DARK = {0x121315, 0x191A1D, 0x212327, 0x2A2C31, 0x34363C, 0x40434A, 0x4E525A};
    private static final int LICHEN = 0x6E7A4A, LICHEN_LIGHT = 0x8C9A5C, RED = 0x9C2A1C, RED_LIGHT = 0xB8402C, BLACK = 0x18140F;

    /** The runes in the column, top to bottom: each a 5x3 cut. */
    private static final String[][] RUNES = {
            {".#.", "##.", "#.#", ".#.", ".#."},
            {"#..", "##.", "#.#", "#..", "#.."},
            {"#.#", ".#.", ".#.", ".#.", ".#."},
            {".#.", "#.#", ".#.", "#.#", ".#."}};

    public static void main(String[] args) throws Exception {
        File out = new File(args[0]);
        out.mkdirs();
        ImageIO.write(stone(false), "png", new File(out, "rune_stone.png"));
        ImageIO.write(stone(true), "png", new File(out, "rune_stone_fresh.png"));
        ImageIO.write(side(), "png", new File(out, "rune_stone_side.png"));
        ImageIO.write(seal(false), "png", new File(out, "barrow_seal.png"));
        ImageIO.write(seal(true), "png", new File(out, "barrow_seal_lit.png"));
        doors(out);
    }

    private static BufferedImage stone(boolean fresh) {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        int[] pal = fresh ? PALE : GRANITE;
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int l = 3 + (noise(x, y, 3) > 0.7F ? 1 : 0) - (noise(x, y, 5) < 0.18F ? 1 : 0);
                // The light from above and the left.
                if (y < 4) {
                    l++;
                }
                if (x > 12 || y > 13) {
                    l--;
                }
                int c = pal[clamp(l)];
                if (!fresh && noise(x, y, 11) > 0.83F && (y > 9 || x < 3)) {
                    c = noise(x, y, 12) > 0.5F ? LICHEN : LICHEN_LIGHT;
                }
                img.setRGB(x, y, 0xFF000000 | c);
            }
        }
        // The serpent-band: up the left, over the top, down the right, a hair in from the edge.
        for (int y = 2; y < 15; y++) {
            cut(img, 2, y, pal, fresh, fresh && (y % 3 != 0));
            cut(img, 13, y, pal, fresh, fresh && (y % 3 != 1));
        }
        for (int x = 2; x <= 13; x++) {
            cut(img, x, 2, pal, fresh, fresh && (x % 3 != 2));
        }
        // Its head, biting its own tail at the foot of the right side.
        cut(img, 12, 14, pal, fresh, fresh);
        cut(img, 11, 13, pal, fresh, fresh);
        // The runes, in a column down the middle.
        int y0 = 4;
        for (String[] rune : RUNES) {
            for (int ry = 0; ry < rune.length; ry++) {
                for (int rx = 0; rx < 3; rx++) {
                    if (rune[ry].charAt(rx) == '#' && y0 + ry < 15) {
                        cut(img, 6 + rx, y0 + ry, pal, fresh, false);
                    }
                }
            }
            y0 += 3;
        }
        return img;
    }

    /** A cut in the stone: dark in the bottom of it, a bright edge above; painted, red or black. */
    private static void cut(BufferedImage img, int x, int y, int[] pal, boolean fresh, boolean redPaint) {
        if (fresh) {
            img.setRGB(x, y, 0xFF000000 | (redPaint ? (noise(x, y, 7) > 0.5F ? RED : RED_LIGHT) : BLACK));
            return;
        }
        img.setRGB(x, y, 0xFF000000 | pal[0]);
        if (y > 0 && (img.getRGB(x, y - 1) & 0xFFFFFF) != pal[0]) {
            img.setRGB(x, y - 1, 0xFF000000 | pal[6]);
        }
    }

    private static BufferedImage side() {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int l = 3 + (noise(x, y, 21) > 0.72F ? 1 : 0) - (noise(x, y, 23) < 0.2F ? 1 : 0) - ((x * 7 + y * 3) % 11 == 0 ? 1 : 0);
                img.setRGB(x, y, 0xFF000000 | GRANITE[clamp(l)]);
            }
        }
        return img;
    }

    private static BufferedImage seal(boolean lit) {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int l = 3 + (noise(x, y, 31) > 0.75F ? 1 : 0) - (noise(x, y, 33) < 0.15F ? 1 : 0);
                if (y == 0 || x == 0) {
                    l += 1;
                }
                if (y == 15 || x == 15) {
                    l -= 2;
                }
                img.setRGB(x, y, 0xFF000000 | DARK[clamp(l)]);
            }
        }
        // Knotwork bands across the top and foot.
        for (int x = 1; x < 15; x++) {
            for (int y : new int[]{2, 13}) {
                boolean over = (x + y) % 4 < 2;
                img.setRGB(x, y, 0xFF000000 | (over ? DARK[6] : DARK[0]));
                img.setRGB(x, y + (over ? 1 : -1), 0xFF000000 | DARK[0]);
            }
        }
        // One rune, cut deep in the middle.
        String[] rune = {"..#..", ".###.", "#.#.#", "..#..", "..#..", ".#.#.", "#...#"};
        int glow = lit ? 0xBFE4FF : DARK[0];
        for (int ry = 0; ry < rune.length; ry++) {
            for (int rx = 0; rx < 5; rx++) {
                if (rune[ry].charAt(rx) == '#') {
                    img.setRGB(5 + rx, 4 + ry, 0xFF000000 | (lit ? (ry % 2 == 0 ? glow : 0x8EC8F4) : glow));
                    if (!lit && 3 + ry >= 0) {
                        img.setRGB(5 + rx, 3 + ry, img.getRGB(5 + rx, 3 + ry) == (0xFF000000 | DARK[0]) ? 0xFF000000 | DARK[0] : 0xFF000000 | DARK[5]);
                    }
                }
            }
        }
        return img;
    }

    // ------------------------------------------------------------------ the doors: one carving over nine blocks

    /**
     * The barrow's doors, carved as one: a knotwork border round the whole of it, a column of
     * runes down either side, and in the middle the valknut, three triangles locked together,
     * the One-Eyed's sign. Written out as nine tiles, row by row from the top. Symmetric, so it
     * reads the same from either face.
     */
    private static void doors(File out) throws Exception {
        for (int lit = 0; lit < 2; lit++) {
            BufferedImage big = new BufferedImage(48, 48, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < 48; y++) {
                for (int x = 0; x < 48; x++) {
                    int l = 3 + (noise(x, y, 41) > 0.76F ? 1 : 0) - (noise(x, y, 43) < 0.14F ? 1 : 0);
                    // Dressed in courses, faintly.
                    if (y % 16 == 0 && noise(x, y, 45) > 0.4F) {
                        l--;
                    }
                    big.setRGB(x, y, 0xFF000000 | DARK[clamp(l)]);
                }
            }
            boolean on = lit == 1;
            // The border: a band of interlace a hair in from the edge.
            for (int i = 2; i < 46; i++) {
                for (int[] p : new int[][]{{i, 2}, {i, 45}, {2, i}, {45, i}}) {
                    boolean over = (i / 2) % 2 == 0;
                    carve(big, p[0], p[1], on && over, false);
                }
                for (int[] p : new int[][]{{i, 4}, {i, 43}, {4, i}, {43, i}}) {
                    boolean over = (i / 2) % 2 == 1;
                    carve(big, p[0], p[1], on && over, false);
                }
            }
            // The runes, down either side.
            int y0 = 8;
            for (int k = 0; y0 + 5 < 41; k++) {
                String[] rune = RUNES[k % RUNES.length];
                for (int ry = 0; ry < 5; ry++) {
                    for (int rx = 0; rx < 3; rx++) {
                        if (rune[ry].charAt(rx) == '#') {
                            carve(big, 7 + rx, y0 + ry, on, true);
                            carve(big, 40 - rx, y0 + ry, on, true);
                        }
                    }
                }
                y0 += 7;
            }
            // The valknut.
            double cx = 23.5D, cy = 25D;
            for (int t = 0; t < 3; t++) {
                double a = Math.PI / 2D + t * Math.PI * 2D / 3D;
                double tx = cx + Math.cos(a) * 3.6D, ty = cy - Math.sin(a) * 3.6D;
                double[][] v = new double[3][];
                for (int k = 0; k < 3; k++) {
                    double b = -Math.PI / 2D + k * Math.PI * 2D / 3D;
                    v[k] = new double[]{tx + Math.cos(b) * 9.5D, ty + Math.sin(b) * 9.5D};
                }
                for (int y = 0; y < 48; y++) {
                    for (int x = 0; x < 48; x++) {
                        double d = Math.min(seg(x + 0.5D, y + 0.5D, v[0], v[1]), Math.min(seg(x + 0.5D, y + 0.5D, v[1], v[2]), seg(x + 0.5D, y + 0.5D, v[2], v[0])));
                        if (d < 1.05D) {
                            carve(big, x, y, on, true);
                        }
                    }
                }
            }
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    ImageIO.write(big.getSubimage(col * 16, row * 16, 16, 16), "png",
                            new File(out, "barrow_seal_" + row + col + (on ? "_lit" : "") + ".png"));
                }
            }
        }
    }

    /** A cut: dark in its bottom, its top edge catching the light; lit, a cold glow in it instead. */
    private static void carve(BufferedImage img, int x, int y, boolean lit, boolean deep) {
        if (lit) {
            img.setRGB(x, y, 0xFF000000 | ((x + y) % 3 == 0 ? 0xDDF0FF : 0x9ED2FA));
            return;
        }
        img.setRGB(x, y, 0xFF000000 | (deep ? DARK[0] : DARK[1]));
        if (y > 0 && (img.getRGB(x, y - 1) & 0xFFFFFF) != DARK[0] && (img.getRGB(x, y - 1) & 0xFFFFFF) != DARK[1]) {
            img.setRGB(x, y - 1, 0xFF000000 | DARK[6]);
        }
    }

    private static double seg(double px, double py, double[] a, double[] b) {
        double dx = b[0] - a[0], dy = b[1] - a[1];
        double t = Math.max(0D, Math.min(1D, ((px - a[0]) * dx + (py - a[1]) * dy) / (dx * dx + dy * dy)));
        double qx = a[0] + t * dx - px, qy = a[1] + t * dy - py;
        return Math.sqrt(qx * qx + qy * qy);
    }

    private static float noise(int x, int y, int seed) {
        int n = x * 374761393 + y * 668265263 + seed * 1274126177;
        n = (n ^ (n >>> 13)) * 1274126177;
        n ^= n >>> 16;
        return (n & 0xFFFF) / 65535F;
    }

    private static int clamp(int l) {
        return l < 0 ? 0 : l > 6 ? 6 : l;
    }
}
