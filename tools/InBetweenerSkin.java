import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;

/**
 * Paints the In-Betweener's 64x64 skin, in the vanilla player layout, and its drained twin:
 * the same man with the black and the white run out of him, all one flat grey, as he looks
 * after he steps out of his glass. Like the comics draw him, he is split straight down the
 * middle: his right side white, his left side black, the head bald, the tunic quartered
 * under a broad black belt, each leg the opposite colour of its boot. A small orange gem
 * sits apart, for the box on his brow; it keeps its colour even when he does not.
 * The box layout here must match client/render/ModelInBetweener.java (vanilla ModelPlayer
 * plus the gem at 56,16).
 *
 * In the faces below the left of the picture is his right side, as when you face him; on
 * every back face it is his left.
 *
 * Usage: java InBetweenerSkin <output dir>   (src/main/resources/assets/eternity/textures/entity)
 */
public class InBetweenerSkin {

    private static final int[] WHITE = {0x7C7C84, 0x9C9CA4, 0xBBBBC2, 0xD6D6DC, 0xE8E8EC, 0xF5F5F7, 0xFFFFFF};
    private static final int[] BLACK = {0x040405, 0x0A0A0C, 0x121215, 0x1C1C21, 0x29292F, 0x3A3A43, 0x55555F};
    private static final int[] GEM = {0x6A1E00, 0x9A3A04, 0xC85A0A, 0xEC7C14, 0xFF9E2C, 0xFFC064, 0xFFE6B0};

    private static final Random RANDOM = new Random(1977L);
    private static BufferedImage skin;

    public static void main(String[] args) throws Exception {
        File out = new File(args[0]);
        out.mkdirs();
        skin = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);

        // ------------------------------------------------------------ the head, bald, split down the middle
        int hu = 0, hv = 0, hd = 8;
        split(hu + hd, hv, 8, 8, true, 5);          // top
        split(hu + hd + 8, hv, 8, 8, true, 2);      // bottom
        fill(hu, hv + hd, 8, 8, WHITE, 3);          // his right side
        fill(hu + hd + 8, hv + hd, 8, 8, BLACK, 3); // his left side
        split(hu + 2 * hd + 8, hv + hd, 8, 8, false, 3); // back
        // Ears, shaded in.
        art(hu + 3, hv + hd + 3, new String[]{"2", "1", "2"});
        art(hu + hd + 8 + 4, hv + hd + 3, new String[]{"d", "e", "d"});
        // His face. Digits are his white side, a-g his black side, E a white eye, P a pupil.
        art(hu + hd, hv + hd, new String[]{
                "5544bbcc",
                "5444bccb",
                "500533b3",
                "52P4cEPb",
                "5442cccb",
                "5432ebbb",
                "4000eeec",
                "4433bbcc"});

        // ------------------------------------------------------------ the body: jacket, belt, tunic
        int bu = 16, bv = 16;
        art(bu + 4, bv, new String[]{   // top: shoulders, the neck split
                "5555bbbb",
                "5554ebbb",
                "5554ebbb",
                "4444bbbb"});
        art(bu + 12, bv, new String[]{  // bottom: the tunic's hem
                "bbbb5555",
                "bbbb4444",
                "aaaa4444",
                "aaaa3333"});
        // Front. His white jacket; on his right a black lapel over a black shirt, on his left a white shirt
        // under a black lapel; at the neck, his own two colours.
        art(bu + 4, bv + 4, new String[]{
                "45b5bb54",
                "45b5bb54",
                "45bA5b54",
                "45bA5b54",
                "45bA5b54",
                "34bA4b43",
                "34bA4b43",
                "abfggfba",
                "bccddccb",
                "abbbbbba",
                "bccb5544",
                "abbb4443"});
        // His right side: jacket, belt, the black quarter of the tunic.
        art(bu, bv + 4, new String[]{
                "4554", "4554", "4454", "3454", "3444", "3443", "2443",
                "bffb", "bccb", "abba", "bccb", "abba"});
        // His left side: jacket, belt, the white quarter.
        art(bu + 12, bv + 4, new String[]{
                "4554", "4554", "4544", "4543", "4443", "3443", "3442",
                "bffb", "bccb", "abba", "5544", "4443"});
        // Back: split like the head, belt across, the tunic quartered the same way round.
        art(bu + 16, bv + 4, new String[]{
                "cccc5555",
                "cbcc5455",
                "cbbc5445",
                "bbbc4445",
                "bbbb4444",
                "abbb4443",
                "abba3443",
                "bfggggfb",
                "bccddccb",
                "abbbbbba",
                "5544bccb",
                "4443abbb"});

        // ------------------------------------------------------------ the arms
        // His right arm: white sleeve, black cuff, white hand.
        limb(40, 16, WHITE, WHITE, new int[][]{{9, 9, 1}}, BLACK, new int[]{10, 12});
        // His left arm: white sleeve, black cuff, black hand.
        limb(32, 48, WHITE, BLACK, new int[][]{{9, 9, 1}}, BLACK, new int[]{10, 12});
        // Shoulder seams where the black lapel meets the sleeve on his left.
        for (int i = 0; i < 4; i++) {
            set(32 + 4 + i, 48 + 4, BLACK[3]);
        }

        // ------------------------------------------------------------ the legs
        // His right leg: the white tunic's lower quarter, a white leg, a black boot.
        legs(0, 16, WHITE, BLACK);
        // His left leg: the black quarter, a black leg, a white boot.
        legs(16, 48, BLACK, WHITE);

        // ------------------------------------------------------------ inked: hatching on the white, highlights on the black
        ink(0, 0, 64, 64);

        // ------------------------------------------------------------ the gem on his brow
        for (int y = 16; y < 18; y++) {
            for (int x = 56; x < 60; x++) {
                set(x, y, GEM[3 + RANDOM.nextInt(3)]);
            }
        }
        set(57, 16, GEM[6]);
        set(57, 17, GEM[5]);

        ImageIO.write(skin, "png", new File(out, "inbetweener.png"));
        ImageIO.write(drained(), "png", new File(out, "inbetweener_drained.png"));
    }

    /** A face split down the middle: his white side on one half, his black side on the other. */
    private static void split(int x, int y, int w, int h, boolean rightOnLeft, int level) {
        int half = w / 2;
        fill(x, y, half, h, rightOnLeft ? WHITE : BLACK, level);
        fill(x + half, y, w - half, h, rightOnLeft ? BLACK : WHITE, level);
    }

    /**
     * An arm: four faces of sleeve, a band of cuff, and the hand below it, laid out the
     * vanilla way for a 4x12x4 box at (u, v).
     */
    private static void limb(int u, int v, int[] sleeve, int[] hand, int[][] cuffs, int[] cuff, int[] handRows) {
        fill(u + 4, v, 4, 4, sleeve, 5);
        fill(u + 8, v, 4, 4, hand, 2);
        for (int face = 0; face < 4; face++) {
            int x = u + face * 4;
            int level = face == 1 ? 4 : face == 3 ? 2 : 3;
            fill(x, v + 4, 4, 12, sleeve, level);
            for (int[] band : cuffs) {
                fill(x, v + 4 + band[0], 4, band[2], cuff, level);
            }
            fill(x, v + 4 + handRows[0], 4, handRows[1] - handRows[0], hand, level + (hand == BLACK ? 1 : 0));
            // A fold in the sleeve at the elbow.
            set(x + 1, v + 4 + 5, sleeve[1]);
            set(x + 2, v + 4 + 5, sleeve[2]);
        }
    }

    /** A leg: the tunic's lower quarter, the leg, and a boot of the other colour, on a 4x12x4 box at (u, v). */
    private static void legs(int u, int v, int[] leg, int[] boot) {
        fill(u + 4, v, 4, 4, leg, 4);
        fill(u + 8, v, 4, 4, boot, 2);
        for (int face = 0; face < 4; face++) {
            int x = u + face * 4;
            int level = face == 1 ? 4 : face == 3 ? 2 : 3;
            fill(x, v + 4, 4, 4, leg, level + 1);
            // The tunic's hem.
            for (int i = 0; i < 4; i++) {
                set(x + i, v + 4 + 3, leg[Math.max(0, level - 1)]);
            }
            fill(x, v + 8, 4, 4, leg, level);
            fill(x, v + 12, 4, 4, boot, level);
            // The boot's cuff catches the light.
            for (int i = 0; i < 4; i++) {
                set(x + i, v + 12, boot[Math.min(6, level + 2)]);
            }
        }
    }

    /** Comic-book inking: fine dark strokes across his white, and pale glints across his black. */
    private static void ink(int x0, int y0, int w, int h) {
        for (int y = y0; y < y0 + h; y++) {
            for (int x = x0; x < x0 + w; x++) {
                int argb = skin.getRGB(x, y);
                if ((argb >>> 24) == 0 || RANDOM.nextFloat() > 0.07F) {
                    continue;
                }
                int lum = argb & 0xFF;
                if (lum > 0xB0) {
                    set(x, y, WHITE[1 + RANDOM.nextInt(2)]);
                } else if (lum < 0x40) {
                    set(x, y, BLACK[4 + RANDOM.nextInt(2)]);
                }
            }
        }
    }

    /**
     * The same skin with the neutral nothing poured into it: every pixel one flat grey,
     * only the faintest ghost of his shape left. The gem keeps its colour.
     */
    private static BufferedImage drained() {
        BufferedImage grey = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                int argb = skin.getRGB(x, y);
                if ((argb >>> 24) == 0) {
                    continue;
                }
                if (x >= 56 && x < 60 && y >= 16 && y < 18) {
                    grey.setRGB(x, y, argb);
                    continue;
                }
                int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
                double lum = 0.3D * r + 0.59D * g + 0.11D * b;
                int v = (int) Math.round(112D + (lum - 128D) * 0.08D + (RANDOM.nextDouble() - 0.5D) * 4D);
                v = Math.max(0, Math.min(255, v));
                grey.setRGB(x, y, 0xFF000000 | v << 16 | v << 8 | (v + 3));
            }
        }
        return grey;
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
                if (RANDOM.nextFloat() < 0.12F) {
                    l += RANDOM.nextBoolean() ? 1 : -1;
                }
                set(x + i, y + j, ramp[Math.max(0, Math.min(ramp.length - 1, l))]);
            }
        }
    }

    /**
     * Hand-placed pixels. Digits are shades 0-6 of his white side, a-g shades of his black
     * side, A his black side drawn darker still (the shirt under the lapel), E the white of
     * an eye, P a pupil.
     */
    private static void art(int x, int y, String[] rows) {
        for (int j = 0; j < rows.length; j++) {
            String row = rows[j];
            for (int i = 0; i < row.length(); i++) {
                char ch = row.charAt(i);
                int px = x + i;
                int py = y + j;
                if (ch >= '0' && ch <= '6') {
                    set(px, py, WHITE[ch - '0']);
                } else if (ch == '7') {
                    set(px, py, WHITE[6]);
                } else if (ch >= 'a' && ch <= 'g') {
                    set(px, py, BLACK[ch - 'a']);
                } else if (ch == 'A') {
                    set(px, py, BLACK[0]);
                } else if (ch == 'E') {
                    set(px, py, 0xF4F4F6);
                } else if (ch == 'P') {
                    set(px, py, 0x050506);
                }
            }
        }
    }

    private static void set(int x, int y, int rgb) {
        skin.setRGB(x, y, 0xFF000000 | rgb);
    }
}
