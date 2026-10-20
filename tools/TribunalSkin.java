import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;

/**
 * Paints the Living Tribunal's 128x128 skin and its glow layer, in the style of a
 * vanilla mob: every box face filled with shaded, grainy pixels, then hand-placed
 * pixel art for his faces, the void in his chest, and the carving of his muscles.
 * The box layout here must match client/render/ModelTribunal.java exactly.
 *
 * Usage: java TribunalSkin <output dir>   (src/main/resources/assets/eternity/textures/entity)
 */
public class TribunalSkin {

    private static final int[] GOLD = {0x4A2C06, 0x6E4508, 0x986510, 0xC48A22, 0xE2AE3E, 0xF4D06E, 0xFFEDAA};
    private static final int[] HOOD = {0x24143E, 0x3A2462, 0x54398C, 0x7052B0, 0x8C6ECE, 0xAD93E6, 0xCFC0F5};
    private static final int[] STONE = {0x141418, 0x22222A, 0x30303A, 0x40404C, 0x52525E, 0x666672, 0x7C7C88};
    private static final int[] HAIR = {0x24160A, 0x3A2410, 0x523418, 0x6A4622, 0x7E5830, 0x8E683A, 0x9E7844};

    private static final Random RANDOM = new Random(1931L);
    private static BufferedImage skin;
    private static BufferedImage glow;

    public static void main(String[] args) throws Exception {
        File out = new File(args[0]);
        out.mkdirs();
        skin = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        glow = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);

        // ------------------------------------------------------------ the head and hood
        box(0, 0, 10, 10, 10, GOLD);
        fill(10, 0, 10, 10, HAIR, 3);
        fill(30, 10, 10, 10, HAIR, 2);
        art(10, 10, new String[]{
                "kkhkhhkhkk",
                "hk4hk4khk4",
                "4545555454",
                "3222442223",
                "4EE3443EE4",
                "4334554334",
                "4443553444",
                "4432332344",
                "3421111243",
                "2343333432"});

        box(40, 0, 12, 3, 12, HOOD);
        fill(52, 0, 12, 12, HOOD, 5);
        for (int i = 0; i < 12; i++) {
            set(57, i, HOOD[6]);
            set(58, i, HOOD[5]);
        }
        art(52, 12, new String[]{
                "eeddeeeeddee",
                "ddccddddccdd",
                "ccbbccccbbcc"});

        box(88, 0, 12, 13, 2, HOOD);
        stripes(104, 2, 12, 13, new int[]{4, 3, 4, 5, 4, 3, 3, 4, 5, 4, 3, 4});
        box(88, 15, 12, 2, 2, HOOD);
        art(90, 17, new String[]{
                "ffeeffffeeff",
                "ddddcdddcddd"});

        box(0, 20, 2, 13, 9, HOOD);
        stripes(0, 29, 9, 13, new int[]{4, 3, 4, 5, 4, 3, 2, 3, 4});
        stripes(11, 29, 9, 13, new int[]{4, 3, 2, 3, 4, 5, 4, 3, 4});
        box(22, 20, 2, 13, 9, HOOD);
        stripes(22, 29, 9, 13, new int[]{4, 3, 4, 5, 4, 3, 2, 3, 4});
        stripes(33, 29, 9, 13, new int[]{4, 3, 2, 3, 4, 5, 4, 3, 4});

        // Vengeance, in profile, on his left.
        box(44, 20, 1, 6, 6, GOLD);
        art(51, 26, new String[]{
                "hhkhhh",
                "332244",
                "3E3444",
                "444554",
                "431234",
                "234432"});
        box(58, 20, 1, 2, 1, GOLD);

        // Necessity, veiled, on his right: cloth taking the shape of a face.
        box(62, 20, 1, 7, 6, HOOD);
        art(62, 26, new String[]{
                "effffe",
                "edddde",
                "edccde",
                "eeffee",
                "eeddee",
                "eeeeee",
                "ddccdd"});
        box(76, 20, 1, 2, 1, HOOD);

        // ------------------------------------------------------------ the body
        box(0, 42, 12, 12, 6, GOLD);
        art(6, 48, new String[]{
                "433455554334",
                "3456CWWC6543",
                "455CWWWWC554",
                "45CWWWWWWC54",
                "45CWWWWWWC54",
                "455CWWWWC554",
                "3456CWWC6543",
                "234433334432",
                "345433334543",
                "334542245433",
                "234554455432",
                "223443344322"});
        art(24, 48, new String[]{
                "322333333223",
                "334444444433",
                "343444444343",
                "343344443343",
                "334333333433",
                "334434434433",
                "343344443343",
                "334333333433",
                "334434434433",
                "233333333332",
                "223333333322",
                "222333333222"});

        box(36, 42, 10, 6, 5, GOLD);
        art(41, 47, new String[]{
                "3452332543",
                "3451221543",
                "3354334533",
                "3451221543",
                "3354334533",
                "2342112432"});

        box(80, 24, 4, 9, 4, GOLD);
        art(84, 28, new String[]{
                "4554",
                "4565",
                "4565",
                "3554",
                "3454",
                "3443",
                "2343",
                "2332",
                "2332"});
        box(96, 24, 5, 5, 6, GOLD);
        art(102, 30, new String[]{
                "45554",
                "45654",
                "45654",
                "34543",
                "23432"});
        box(80, 37, 4, 8, 4, GOLD);
        art(84, 41, new String[]{
                "4554",
                "4554",
                "3454",
                "3443",
                "3443",
                "3433",
                "2332",
                "2332"});
        box(96, 37, 4, 3, 4, GOLD);
        art(100, 41, new String[]{
                "3434",
                "4444",
                "3333"});
        box(112, 37, 1, 3, 1, GOLD);

        box(66, 49, 5, 8, 5, GOLD);
        art(71, 54, new String[]{
                "34543",
                "45654",
                "45654",
                "45554",
                "34543",
                "34443",
                "33433",
                "23432"});
        box(86, 49, 4, 8, 4, GOLD);
        art(90, 53, new String[]{
                "4554",
                "4664",
                "3553",
                "3443",
                "3443",
                "3433",
                "2332",
                "2332"});
        box(102, 49, 4, 2, 6, GOLD);

        // ------------------------------------------------------------ the throne
        box(0, 64, 16, 6, 12, STONE);
        box(56, 64, 5, 14, 14, STONE);
        box(0, 82, 22, 34, 3, STONE);
        box(50, 100, 24, 3, 5, STONE);
        cracks(0, 64, 56, 18);
        cracks(56, 64, 38, 28);
        cracks(0, 82, 50, 37);
        cracks(50, 100, 58, 8);

        ImageIO.write(skin, "png", new File(out, "tribunal.png"));
        ImageIO.write(glow, "png", new File(out, "tribunal_glow.png"));
    }

    /** Fills all six faces of a box laid out the vanilla way at (u, v), lit from above and in front. */
    private static void box(int u, int v, int w, int h, int d, int[] ramp) {
        fill(u + d, v, w, d, ramp, 5);
        fill(u + d + w, v, w, d, ramp, 1);
        fill(u, v + d, d, h, ramp, 3);
        fill(u + d, v + d, w, h, ramp, 4);
        fill(u + d + w, v + d, d, h, ramp, 3);
        fill(u + d + d + w, v + d, w, h, ramp, 2);
    }

    /** A face of shaded, grainy pixels: lighter at the top, darker at the bottom and edges. */
    private static void fill(int x, int y, int w, int h, int[] ramp, int level) {
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                int l = level;
                if (h >= 4) {
                    if (j < h / 4) {
                        l++;
                    } else if (j >= h - h / 4) {
                        l--;
                    }
                }
                if (w >= 4 && (i == 0 || i == w - 1)) {
                    l--;
                }
                if (RANDOM.nextFloat() < 0.18F) {
                    l += RANDOM.nextBoolean() ? 1 : -1;
                }
                set(x + i, y + j, ramp[Math.max(0, Math.min(ramp.length - 1, l))]);
            }
        }
    }

    /** Folds of cloth: one shade per column, deepening toward the hem. */
    private static void stripes(int x, int y, int w, int h, int[] columns) {
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                int l = columns[i % columns.length] - (j >= h - 2 ? 1 : 0) - (j == h - 1 ? 1 : 0);
                if (RANDOM.nextFloat() < 0.1F) {
                    l += RANDOM.nextBoolean() ? 1 : -1;
                }
                set(x + i, y + j, HOOD[Math.max(0, Math.min(HOOD.length - 1, l))]);
            }
        }
    }

    /** Dark cracks wandering over rough stone. */
    private static void cracks(int x, int y, int w, int h) {
        int count = w * h / 60;
        for (int c = 0; c < count; c++) {
            int px = x + RANDOM.nextInt(w);
            int py = y + RANDOM.nextInt(h);
            int length = 2 + RANDOM.nextInt(4);
            for (int s = 0; s < length; s++) {
                if (px >= x && px < x + w && py >= y && py < y + h && (skin.getRGB(px, py) >>> 24) != 0) {
                    set(px, py, STONE[RANDOM.nextInt(2)]);
                }
                px += RANDOM.nextInt(3) - 1;
                py += 1;
            }
        }
    }

    /**
     * Hand-placed pixels. Digits are gold shades 0-6, a-g hood shades, h and k hair, and
     * E, W and C the light of his eyes and heart, which also go into the glow layer.
     */
    private static void art(int x, int y, String[] rows) {
        for (int j = 0; j < rows.length; j++) {
            String row = rows[j];
            for (int i = 0; i < row.length(); i++) {
                char ch = row.charAt(i);
                int px = x + i;
                int py = y + j;
                if (ch >= '0' && ch <= '6') {
                    set(px, py, GOLD[ch - '0']);
                } else if (ch >= 'a' && ch <= 'g') {
                    set(px, py, HOOD[ch - 'a']);
                } else if (ch == 'h') {
                    set(px, py, HAIR[3]);
                } else if (ch == 'k') {
                    set(px, py, HAIR[1]);
                } else if (ch == 'E') {
                    set(px, py, 0xFFFFFF);
                    glow.setRGB(px, py, 0xFFFFFFFF);
                } else if (ch == 'W') {
                    set(px, py, 0xF4FBFF);
                    glow.setRGB(px, py, 0xFFF4FBFF);
                } else if (ch == 'C') {
                    set(px, py, 0x9FDDFF);
                    glow.setRGB(px, py, 0xFF8FD8FF);
                }
            }
        }
    }

    private static void set(int x, int y, int rgb) {
        skin.setRGB(x, y, 0xFF000000 | rgb);
    }
}
