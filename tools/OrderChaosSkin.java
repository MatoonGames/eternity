import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;

/**
 * Paints the 64x64 skins of Master Order and Lord Chaos, the two vast floating heads that
 * come for the In-Betweener. Order is a bald man's head, brown, scowling, teeth bared, deep
 * lines in his brow. Chaos is a lumpen violet mass with dark hollows where eyes and a mouth
 * should be; the renderer fills those hollows with deep space. The box layout here must
 * match client/render/ModelOrderChaos.java:
 *
 * <pre>
 *   skull     0,0   12x14x12      jaw       0,26  10x3x10      brow  0,39  12x2x1
 *   nose     26,39   2x3x1        ears     32,39   1x3x2  (Order only)
 *   forehead  0,44  10x2x8        cheeks   36,44   1x4x6  (Chaos only)
 * </pre>
 *
 * Usage: java OrderChaosSkin <output dir>   (src/main/resources/assets/eternity/textures/entity)
 */
public class OrderChaosSkin {

    private static final int[] BROWN = {0x2A160C, 0x442414, 0x62361E, 0x804A2A, 0x9C5E36, 0xB57446, 0xCC8E5C};
    private static final int[] VIOLET = {0x1A1236, 0x2A1C54, 0x3C2A74, 0x523C96, 0x6850B4, 0x8068CC, 0x9C86E0};

    private static final Random RANDOM = new Random(1977L);
    private static BufferedImage skin;
    private static int[] ramp;

    public static void main(String[] args) throws Exception {
        File out = new File(args[0]);
        out.mkdirs();
        order();
        ImageIO.write(skin, "png", new File(out, "master_order.png"));
        chaos();
        ImageIO.write(skin, "png", new File(out, "lord_chaos.png"));
    }

    /** Master Order: bald, scowling, his teeth bared in fury, the lines of his brow cut deep. */
    private static void order() {
        skin = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        ramp = BROWN;
        box(0, 0, 12, 14, 12);
        art(12, 12, new String[]{
                "445555555544",
                "455566665554",
                "455555555554",
                "443322223344",
                "445544445544",
                "4k3344443k34",
                "4k3k4444k3k4",
                "4EEP3443PEE4",
                "43kk3443kk34",
                "443343343344",
                "43LLLLLLLL34",
                "4LWwWwWwWwL4",
                "43LwWwWwWL34",
                "443LLLLLL344"});
        box(0, 26, 10, 3, 10);
        art(10, 36, new String[]{
                "3444444443",
                "2344444432",
                "2233333322"});
        box(0, 39, 12, 2, 1);
        art(1, 40, new String[]{
                "4kkk3443kkk4",
                "34kkkkkkkk43"});
        box(26, 39, 2, 3, 1);
        art(27, 40, new String[]{"54", "43", "kk"});
        box(32, 39, 1, 3, 2);
        lines(0, 12, 12, 14, 0.04F);
    }

    /** Lord Chaos: lumps and folds of violet, and hollows for the stars to show through. */
    private static void chaos() {
        skin = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        ramp = VIOLET;
        box(0, 0, 12, 14, 12);
        art(12, 12, new String[]{
                "345565565543",
                "456655566554",
                "455544455554",
                "345433345443",
                "445543345544",
                "43kk4444kk34",
                "4kkkk34kkkk3",
                "4kkkk43kkkk4",
                "3kkk3443kkk3",
                "443344443344",
                "43kkkkkkkk34",
                "4kkkkkkkkkk3",
                "43kkkkkkkk34",
                "4433kkkk3344"});
        box(0, 26, 10, 3, 10);
        art(10, 36, new String[]{
                "3455445543",
                "2344334432",
                "2233223322"});
        box(0, 39, 12, 2, 1);
        art(1, 40, new String[]{
                "455666665554",
                "3445544554k3"});
        box(26, 39, 2, 3, 1);
        art(27, 40, new String[]{"54", "43", "kk"});
        box(0, 44, 10, 2, 8);
        box(36, 44, 1, 4, 6);
        lumps(0, 0, 48, 26);
        lumps(0, 44, 50, 10);
    }

    /** Fills all six faces of a box laid out the vanilla way at (u, v), lit from above and in front. */
    private static void box(int u, int v, int w, int h, int d) {
        fill(u + d, v, w, d, 5);
        fill(u + d + w, v, w, d, 1);
        fill(u, v + d, d, h, 3);
        fill(u + d, v + d, w, h, 4);
        fill(u + d + w, v + d, d, h, 3);
        fill(u + d + d + w, v + d, w, h, 2);
    }

    /** A face of shaded, grainy pixels: lighter at the top, darker at the bottom and edges. */
    private static void fill(int x, int y, int w, int h, int level) {
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
                if (RANDOM.nextFloat() < 0.15F) {
                    l += RANDOM.nextBoolean() ? 1 : -1;
                }
                set(x + i, y + j, ramp[Math.max(0, Math.min(ramp.length - 1, l))]);
            }
        }
    }

    /** Fine dark lines, like the ink of a comic panel. */
    private static void lines(int x, int y, int w, int h, float chance) {
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                if (RANDOM.nextFloat() < chance && (skin.getRGB(x + i, y + j) >>> 24) != 0) {
                    set(x + i, y + j, ramp[1]);
                }
            }
        }
    }

    /** Swellings and pits scattered over his lumpen surface. */
    private static void lumps(int x, int y, int w, int h) {
        int count = w * h / 30;
        for (int c = 0; c < count; c++) {
            int px = x + RANDOM.nextInt(w), py = y + RANDOM.nextInt(h);
            if ((skin.getRGB(px, py) >>> 24) == 0) {
                continue;
            }
            boolean pit = RANDOM.nextBoolean();
            set(px, py, ramp[pit ? 1 : 6]);
            if (py + 1 < y + h && (skin.getRGB(px, py + 1) >>> 24) != 0) {
                set(px, py + 1, ramp[pit ? 2 : 5]);
            }
        }
    }

    /**
     * Hand-placed pixels. Digits are shades of the skin, k its deepest shadow, E the white
     * of an eye, P a pupil, L a lip, W and w bared teeth.
     */
    private static void art(int x, int y, String[] rows) {
        for (int j = 0; j < rows.length; j++) {
            String row = rows[j];
            for (int i = 0; i < row.length(); i++) {
                char ch = row.charAt(i);
                int px = x + i, py = y + j;
                if (ch >= '0' && ch <= '6') {
                    set(px, py, ramp[ch - '0']);
                } else if (ch == 'k') {
                    set(px, py, ramp == VIOLET ? 0x0C0818 : 0x160A04);
                } else if (ch == 'E') {
                    set(px, py, 0xEEE6D6);
                } else if (ch == 'P') {
                    set(px, py, 0x120804);
                } else if (ch == 'L') {
                    set(px, py, 0x4E2416);
                } else if (ch == 'W') {
                    set(px, py, 0xF2ECDA);
                } else if (ch == 'w') {
                    set(px, py, 0xB8AE96);
                }
            }
        }
    }

    private static void set(int x, int y, int rgb) {
        skin.setRGB(x, y, 0xFF000000 | rgb);
    }
}
