import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;

/**
 * Paints the blocks of Warlock's home, the Sovereign station (16x16 each, into textures/blocks).
 * The Sovereign build in gold and nothing else: brushed, engraved, polished, and lit from
 * inside by seams of white light, with the cold teal of their instruments the only other colour.
 * Everything in it was made to be admired, and has not been, for a long time.
 * <ul>
 * <li><b>sovereign_plate</b>: brushed hull plate, bevelled edge, a rivet at each corner.</li>
 * <li><b>sovereign_panel</b>: a wall panel with their sunburst sunk into it.</li>
 * <li><b>sovereign_rib</b>: vertical ribbing for columns and arches.</li>
 * <li><b>sovereign_trim</b>: a band of gold beading with teal set between.</li>
 * <li><b>sovereign_dark</b>: tarnished bronze, for shadowed walls and the dead parts of the station.</li>
 * <li><b>sovereign_floor</b>: polished gold tiles, diamond-set, dark grout.</li>
 * <li><b>sovereign_light</b>, <b>sovereign_light_dim</b>: a seam of white light in a gold frame, and the same gone weak.</li>
 * <li><b>sovereign_glass</b>: amber-tinted glass for the domes, gold-framed.</li>
 * <li><b>sovereign_mirror</b>: a dark polished face with reflections streaking down it.</li>
 * <li><b>sovereign_door</b>, <b>sovereign_door_lit</b>: an iris door, the sunburst at its heart; lit when it will open.</li>
 * <li><b>cocoon_shell</b>, <b>cocoon_inner</b>, <b>cocoon_fused</b>: a birthing cocoon's veined gold shell, its
 * wet membrane, and the blackened, cracked shell of one that never opened.</li>
 * <li><b>vat_glass</b>, <b>vat_broken</b>, <b>vat_metal</b>, <b>vat_liquid</b> (animated): a growing-vat's
 * green glass, the same smashed, its gold fittings, and the gold-green fluid moving inside.</li>
 * <li><b>console_body</b>, <b>console_screen</b> (animated): a records console and the teal script crawling over it.</li>
 * <li><b>pod_glass</b>, <b>pod_glass_clear</b>, <b>pod_base</b>: the frosted capsules of the gallery, the same with
 * the frost cleared, and their plinths.</li>
 * </ul>
 * Usage: javac -d out tools/SovereignArt.java then java -cp out SovereignArt src/main/resources/assets/eternity/textures/blocks
 */
public class SovereignArt {

    /** Gold, dark to light. */
    private static final int[] GOLD = {0x3A2A0C, 0x5A4112, 0x7C5A18, 0x9E7620, 0xC2952C, 0xDDB443, 0xF0D272, 0xFFF0B8};
    /** Tarnished bronze. */
    private static final int[] BRONZE = {0x17110A, 0x221A0F, 0x2F2414, 0x3E3019, 0x4F3E1F, 0x624D26, 0x775E2F, 0x8C7038};
    private static final int[] TEAL = {0x06302E, 0x0B4B47, 0x13706A, 0x22998F, 0x45C2B5, 0x8EE8DC};
    private static final int[] GREEN = {0x0C2A12, 0x15401A, 0x225C22, 0x38792B, 0x5A9636, 0x8DB646, 0xC6D86A};

    private static File out;

    public static void main(String[] args) throws Exception {
        out = new File(args[0]);
        out.mkdirs();
        write("sovereign_plate", plate());
        write("sovereign_panel", panel());
        write("sovereign_rib", rib());
        write("sovereign_trim", trim());
        write("sovereign_dark", dark());
        write("sovereign_floor", floor());
        write("sovereign_light", light(true));
        write("sovereign_light_dim", light(false));
        write("sovereign_glass", glass());
        write("sovereign_mirror", mirror());
        write("sovereign_door", door(false));
        write("sovereign_door_lit", door(true));
        write("cocoon_shell", shell(false));
        write("cocoon_fused", shell(true));
        write("cocoon_inner", inner());
        write("vat_glass", vatGlass(false));
        write("vat_broken", vatGlass(true));
        write("vat_metal", vatMetal());
        write("vat_liquid", liquid());
        animate("vat_liquid", 3);
        write("console_body", consoleBody());
        write("console_screen", screen());
        animate("console_screen", 4);
        write("pod_glass", podGlass());
        write("pod_glass_clear", podGlassClear());
        write("pod_base", podBase());
    }

    // ------------------------------------------------------------------ hull

    /** Brushed gold: fine horizontal grain, never quite even. */
    private static int brushed(int x, int y, int seed) {
        float n = noise(x / 6, y, seed) * 0.6F + noise(x, y, seed + 1) * 0.4F;
        return n > 0.72F ? 1 : n < 0.22F ? -1 : 0;
    }

    private static BufferedImage plate() {
        BufferedImage img = img();
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int l = 4 + brushed(x, y, 1);
                // The bevel: light along the top and left, shadow along the bottom and right.
                if (x == 0 || y == 0) {
                    l = 6;
                } else if (x == 15 || y == 15) {
                    l = 1;
                } else if (x == 1 || y == 1) {
                    l = 5;
                } else if (x == 14 || y == 14) {
                    l = 2;
                }
                // A soft sheen across the middle.
                if (x + y >= 12 && x + y <= 15 && x > 1 && y > 1 && x < 14 && y < 14) {
                    l++;
                }
                set(img, x, y, GOLD[clamp(l, GOLD)]);
            }
        }
        for (int[] r : new int[][]{{3, 3}, {12, 3}, {3, 12}, {12, 12}}) {
            rivet(img, r[0], r[1]);
        }
        return img;
    }

    private static void rivet(BufferedImage img, int x, int y) {
        set(img, x, y, GOLD[6]);
        set(img, x + 1, y, GOLD[4]);
        set(img, x, y + 1, GOLD[3]);
        set(img, x + 1, y + 1, GOLD[1]);
    }

    private static BufferedImage panel() {
        BufferedImage img = img();
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int l = 4 + brushed(x, y, 3);
                if (x == 0 || y == 0) {
                    l = 6;
                } else if (x == 15 || y == 15) {
                    l = 1;
                }
                // The sunken field.
                if (x >= 2 && x <= 13 && y >= 2 && y <= 13) {
                    l = 3 + brushed(x, y, 4);
                    if (x == 2 || y == 2) {
                        l = 1;
                    } else if (x == 13 || y == 13) {
                        l = 5;
                    }
                }
                set(img, x, y, GOLD[clamp(l, GOLD)]);
            }
        }
        // The sunburst: a disc, and rays raised out of the field.
        double cx = 7.5D, cy = 7.5D;
        for (int y = 3; y <= 12; y++) {
            for (int x = 3; x <= 12; x++) {
                double dx = x - cx, dy = y - cy, r = Math.sqrt(dx * dx + dy * dy);
                double a = Math.atan2(dy, dx);
                boolean ray = Math.abs(Math.sin(a * 6D)) > 0.86D && r > 2.4D && r < 5.4D;
                if (r < 2.0D) {
                    set(img, x, y, GOLD[r < 1D ? 7 : dx + dy < 0 ? 6 : 5]);
                } else if (r < 2.6D) {
                    set(img, x, y, GOLD[2]);
                } else if (ray) {
                    set(img, x, y, GOLD[dx + dy < 0 ? 6 : 5]);
                }
            }
        }
        return img;
    }

    private static BufferedImage rib() {
        BufferedImage img = img();
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                // Four flutes, each lit on its left and in shadow on its right.
                int f = x % 4;
                int l = f == 0 ? 6 : f == 1 ? 5 : f == 2 ? 3 : 1;
                if (noise(x, y, 7) > 0.85F) {
                    l--;
                }
                if (y == 0 || y == 15) {
                    l = y == 0 ? 7 : 1;
                }
                set(img, x, y, GOLD[clamp(l, GOLD)]);
            }
        }
        return img;
    }

    private static BufferedImage trim() {
        BufferedImage img = img();
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int l = 4 + brushed(x, y, 9);
                if (y <= 1 || y >= 14) {
                    l = y == 0 ? 6 : y == 15 ? 1 : y == 1 ? 5 : 2;
                } else if (y == 3 || y == 12) {
                    l = y == 3 ? 2 : 5;
                }
                set(img, x, y, GOLD[clamp(l, GOLD)]);
            }
        }
        // Beading, top and bottom; teal stones set between, down the middle.
        for (int x = 0; x < 16; x += 2) {
            set(img, x, 2, GOLD[7]);
            set(img, x + 1, 2, GOLD[3]);
            set(img, x, 13, GOLD[6]);
            set(img, x + 1, 13, GOLD[2]);
        }
        for (int x = 1; x < 16; x += 5) {
            for (int dy = 0; dy < 4; dy++) {
                for (int dx = 0; dx < 3; dx++) {
                    boolean corner = (dy == 0 || dy == 3) && dx != 1;
                    if (!corner) {
                        set(img, x + dx, 6 + dy, TEAL[dy == 0 ? 5 : dx == 0 ? 4 : dy == 3 ? 1 : 2]);
                    }
                }
            }
            set(img, x - 1 < 0 ? 15 : x - 1, 7, GOLD[1]);
            set(img, (x + 3) % 16, 8, GOLD[6]);
        }
        return img;
    }

    private static BufferedImage dark() {
        BufferedImage img = img();
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int l = 3 + brushed(x, y, 11);
                if (noise(x / 3, y / 3, 12) > 0.8F) {
                    l -= 1;
                }
                if (x == 0 || y == 0) {
                    l++;
                } else if (x == 15 || y == 15) {
                    l -= 2;
                }
                int c = BRONZE[clamp(l, BRONZE)];
                // Verdigris where the damp got into it.
                if (noise(x, y, 13) > 0.9F && y > 8) {
                    c = mix(c, 0x3E6A55, 0.5F);
                }
                set(img, x, y, c);
            }
        }
        return img;
    }

    private static BufferedImage floor() {
        BufferedImage img = img();
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                // A polished slab: smooth, a slow sheen across it, a fine seam where it meets the next.
                int l = 4 + (noise(x / 4, y / 4, 14) > 0.75F ? 1 : 0);
                if (x + y > 9 && x + y < 15) {
                    l++;
                }
                int c = GOLD[clamp(l, GOLD)];
                if (x == 0 || y == 0) {
                    c = GOLD[2];
                } else if (x == 1 || y == 1) {
                    c = GOLD[6];
                }
                set(img, x, y, c);
            }
        }
        // A small inlay of teal at each corner, where four slabs meet.
        int[][] inlay = {{0, 0}, {15, 0}, {0, 15}, {15, 15}, {1, 0}, {0, 1}, {14, 0}, {15, 1}, {0, 14}, {1, 15}, {15, 14}, {14, 15}};
        for (int[] p : inlay) {
            set(img, p[0], p[1], TEAL[p[0] % 15 == 0 && p[1] % 15 == 0 ? 3 : 1]);
        }
        return img;
    }

    private static BufferedImage light(boolean lit) {
        BufferedImage img = img();
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int c;
                if (x <= 2 || x >= 13) {
                    int l = x == 0 ? 6 : x == 1 ? 4 : x == 2 ? 2 : x == 13 ? 2 : x == 14 ? 4 : 1;
                    c = GOLD[l];
                } else {
                    // The seam: white at its heart, warming to gold at its edges.
                    double e = Math.abs(x - 7.5D) / 5.5D;
                    int core = mix(0xFFFDF2, 0xF6D47A, (float) (e * e));
                    c = lit ? core : mix(mix(core, 0x6B5320, 0.72F), 0x000000, noise(x, y, 15) > 0.7F ? 0.25F : 0F);
                }
                set(img, x, y, c);
            }
        }
        // The frame's bolts.
        for (int y = 2; y < 16; y += 6) {
            set(img, 1, y, GOLD[7]);
            set(img, 14, y, GOLD[6]);
        }
        return img;
    }

    private static BufferedImage glass() {
        BufferedImage img = img();
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                if (x == 0 || y == 0 || x == 15 || y == 15) {
                    set(img, x, y, GOLD[x == 0 || y == 0 ? 5 : 2]);
                    continue;
                }
                int a = 0x48;
                int c = 0xE8B04A;
                // A streak of reflection across one corner.
                int s = x - y;
                if (s >= 4 && s <= 6 || s == -8) {
                    c = 0xFFF2C8;
                    a = 0x90;
                }
                img.setRGB(x, y, (a << 24) | c);
            }
        }
        return img;
    }

    private static BufferedImage mirror() {
        BufferedImage img = img();
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                if (x == 0 || x == 15) {
                    set(img, x, y, GOLD[x == 0 ? 5 : 2]);
                    continue;
                }
                float n = noise(x, y / 4, 16);
                int c = mix(0x0B0C14, 0x2A2D44, (16 - y) / 22F + n * 0.2F);
                // Long streaks of whatever it is reflecting.
                if ((x == 4 || x == 5) && y > 1 && y < 13 || x == 10 && y > 3) {
                    c = mix(c, 0x9AA3C8, x == 5 ? 0.25F : 0.4F);
                }
                set(img, x, y, c);
            }
        }
        return img;
    }

    private static BufferedImage door(boolean lit) {
        BufferedImage img = img();
        double cx = 7.5D, cy = 7.5D;
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                double dx = x - cx, dy = y - cy, r = Math.sqrt(dx * dx + dy * dy), a = Math.atan2(dy, dx);
                int c;
                if (r > 7.2D) {
                    c = BRONZE[2 + brushed(x, y, 17)];
                } else if (r > 6.2D) {
                    c = GOLD[dx + dy < 0 ? 6 : 2];
                } else {
                    // The iris: six blades, overlapping, each lit along its leading edge.
                    double blade = ((a + Math.PI) / (Math.PI * 2D) * 6D + r * 0.12D) % 1D;
                    int l = blade < 0.12D ? 6 : blade < 0.5D ? 4 : 3;
                    c = GOLD[l];
                    if (r < 2.2D) {
                        c = lit ? (r < 1.2D ? 0xFFFFFF : 0x9FF5E9) : (r < 1.2D ? GOLD[7] : GOLD[2]);
                    } else if (lit && blade < 0.12D) {
                        c = mix(c, 0x8EE8DC, 0.6F);
                    }
                }
                set(img, x, y, c);
            }
        }
        return img;
    }

    // ------------------------------------------------------------------ the cocoons

    private static BufferedImage shell(boolean fused) {
        BufferedImage img = img();
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                // Veins: ridges where a slow wobble in x crosses itself.
                double v = Math.sin(x * 0.9D + Math.sin(y * 0.55D) * 2.2D) + Math.sin(y * 0.4D + x * 0.2D);
                int l = 4 + (v > 1.25D ? 2 : v > 0.8D ? 1 : v < -1.2D ? -2 : 0);
                if (noise(x, y, 18) > 0.85F) {
                    l--;
                }
                int c = GOLD[clamp(l, GOLD)];
                if (fused) {
                    // Blackened, dull, split: whatever was inside never came out.
                    c = mix(BRONZE[clamp(l - 1, BRONZE)], 0x0A0806, 0.25F);
                    boolean crack = Math.abs(x - 8 - (int) (Math.sin(y * 0.8D) * 2.5D)) < 1 && y > 2 && y < 14
                            || y == 9 && x > 9 && x < 14 || y == 5 && x > 2 && x < 6;
                    if (crack) {
                        c = 0x050302;
                    } else if (noise(x, y, 19) > 0.88F) {
                        c = mix(c, 0x6A5022, 0.5F);
                    }
                }
                set(img, x, y, c);
            }
        }
        return img;
    }

    private static BufferedImage inner() {
        BufferedImage img = img();
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                double v = Math.sin(x * 0.7D + y * 0.3D) * 0.5D + Math.sin(y * 0.9D - x * 0.2D) * 0.5D;
                int c = mix(0x8A4A2E, 0xE9B07A, (float) (v * 0.5D + 0.5D));
                // Wet: highlights that catch.
                if (noise(x, y, 20) > 0.9F) {
                    c = 0xFFE6C4;
                }
                set(img, x, y, c);
            }
        }
        return img;
    }

    // ------------------------------------------------------------------ the vats

    private static BufferedImage vatGlass(boolean broken) {
        BufferedImage img = img();
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int a = 0x55, c = 0x7FD0A0;
                if (x == 0 || x == 15) {
                    a = 0xC0;
                    c = 0x2E6A48;
                } else if (x == 3 || x == 4) {
                    a = 0x90;
                    c = 0xD8FFE4;
                }
                if (broken) {
                    // Smashed out from inside: a ragged hole, sharp edges standing.
                    double dx = x - 8.5D, dy = y - 9D;
                    double edge = 5.2D + Math.sin(Math.atan2(dy, dx) * 7D) * 1.6D;
                    double r = Math.sqrt(dx * dx + dy * dy);
                    if (r < edge) {
                        a = 0;
                    } else if (r < edge + 1D) {
                        a = 0xD0;
                        c = 0xE8FFF0;
                    }
                }
                img.setRGB(x, y, (a << 24) | c);
            }
        }
        return img;
    }

    private static BufferedImage vatMetal() {
        BufferedImage img = img();
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int l = 3 + brushed(x, y, 21);
                if (y == 0 || y == 4 || y == 11) {
                    l = 6;
                } else if (y == 3 || y == 10 || y == 15) {
                    l = 1;
                }
                set(img, x, y, GOLD[clamp(l, GOLD)]);
            }
        }
        // Pipes in, teal-lit.
        for (int y = 5; y < 10; y++) {
            set(img, 7, y, TEAL[2]);
            set(img, 8, y, TEAL[y == 7 ? 5 : 3]);
        }
        return img;
    }

    /** Eight frames of gold-green fluid, moving slowly, with something's shadow drifting through. */
    private static BufferedImage liquid() {
        int frames = 8;
        BufferedImage img = new BufferedImage(16, 16 * frames, BufferedImage.TYPE_INT_ARGB);
        for (int f = 0; f < frames; f++) {
            double t = f / (double) frames * Math.PI * 2D;
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    double v = Math.sin(x * 0.6D + t) * 0.4D + Math.sin(y * 0.45D - t * 2D) * 0.4D + Math.sin((x + y) * 0.3D + t) * 0.2D;
                    int l = 3 + (int) Math.round(v * 2D);
                    int c = GREEN[clamp(l, GREEN)];
                    c = mix(c, GOLD[5], 0.18F);
                    // Bubbles rising.
                    int by = Math.floorMod(14 - f * 2 - x * 3, 16);
                    if ((x == 4 || x == 11) && y == by) {
                        c = 0xE4F4B0;
                    }
                    img.setRGB(x, y + f * 16, 0xD8000000 | c);
                }
            }
        }
        return img;
    }

    // ------------------------------------------------------------------ the consoles

    private static BufferedImage consoleBody() {
        BufferedImage img = img();
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int l = 3 + brushed(x, y, 22);
                if (y % 5 == 0) {
                    l = 5;
                } else if (y % 5 == 4) {
                    l = 1;
                }
                set(img, x, y, GOLD[clamp(l, GOLD)]);
            }
        }
        for (int x = 3; x < 13; x += 3) {
            set(img, x, 7, TEAL[4]);
            set(img, x, 12, TEAL[2]);
        }
        return img;
    }

    /** Four frames of their script crawling up a black glass face, teal, with a cursor. */
    private static BufferedImage screen() {
        int frames = 4;
        BufferedImage img = new BufferedImage(16, 16 * frames, BufferedImage.TYPE_INT_ARGB);
        for (int f = 0; f < frames; f++) {
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    int c;
                    if (x == 0 || y == 0 || x == 15 || y == 15) {
                        c = GOLD[x == 0 || y == 0 ? 5 : 2];
                    } else {
                        c = 0x041210;
                        int row = y + f * 2;
                        boolean line = row % 3 != 0 && x > 1 && x < 14;
                        boolean glyph = line && noise(x, row / 3, 23 + row % 3) > 0.42F && x < 3 + (int) (noise(0, row / 3, 30) * 12F);
                        if (glyph) {
                            c = TEAL[row % 3 == 1 ? 4 : 3];
                        }
                        // The heading, steady.
                        if (y >= 2 && y <= 3 && x >= 3 && x <= 12) {
                            c = (x + y) % 2 == 0 ? TEAL[5] : TEAL[3];
                        }
                    }
                    img.setRGB(x, y + f * 16, 0xFF000000 | c);
                }
            }
            if (f % 2 == 0) {
                img.setRGB(12, 12 + f * 16, 0xFF000000 | TEAL[5]);
                img.setRGB(12, 13 + f * 16, 0xFF000000 | TEAL[5]);
            }
        }
        return img;
    }

    // ------------------------------------------------------------------ the gallery

    private static BufferedImage podGlass() {
        BufferedImage img = img();
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int a = 0x38, c = 0xDDE8F0;
                // Frost creeping up from the bottom, and down from the seals.
                float frost = noise(x, y, 24) * 0.5F + (y > 11 ? (y - 11) * 0.12F : 0F) + (y < 2 ? 0.3F : 0F);
                if (frost > 0.55F) {
                    a = 0x88 + (int) Math.min(0x50, (frost - 0.55F) * 200F);
                    c = 0xF2F8FF;
                }
                if (x == 2 || x == 3) {
                    a = Math.max(a, 0x70);
                    c = 0xFFFFFF;
                }
                img.setRGB(x, y, (a << 24) | c);
            }
        }
        return img;
    }

    /** The same glass with the frost gone: nearly nothing, a glint down one side, a ring of rime left at the seals. */
    private static BufferedImage podGlassClear() {
        BufferedImage img = img();
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int a = 0x14, c = 0xEAF2FA;
                if (x == 2) {
                    a = 0x60;
                    c = 0xFFFFFF;
                } else if ((y == 0 || y == 15) && noise(x, y, 27) > 0.4F) {
                    a = 0x70;
                    c = 0xF2F8FF;
                }
                img.setRGB(x, y, (a << 24) | c);
            }
        }
        return img;
    }

    private static BufferedImage podBase() {
        BufferedImage img = img();
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int l = 4 + brushed(x, y, 25);
                if (y < 2) {
                    l = y == 0 ? 7 : 5;
                } else if (y > 13) {
                    l = 1;
                }
                set(img, x, y, GOLD[clamp(l, GOLD)]);
            }
        }
        // The plaque: a name nobody wrote down, in their script.
        for (int x = 4; x < 12; x++) {
            set(img, x, 6, BRONZE[2]);
            set(img, x, 10, GOLD[6]);
            for (int y = 7; y < 10; y++) {
                set(img, x, y, BRONZE[1]);
                if (noise(x, y, 26) > 0.5F && x > 4 && x < 11) {
                    set(img, x, y, GOLD[5]);
                }
            }
        }
        return img;
    }

    // ------------------------------------------------------------------ helpers

    private static BufferedImage img() {
        return new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
    }

    private static void set(BufferedImage img, int x, int y, int c) {
        if (x >= 0 && y >= 0 && x < img.getWidth() && y < img.getHeight()) {
            img.setRGB(x, y, 0xFF000000 | c);
        }
    }

    private static int clamp(int l, int[] pal) {
        return Math.max(0, Math.min(pal.length - 1, l));
    }

    private static int mix(int a, int b, float t) {
        t = Math.max(0F, Math.min(1F, t));
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }

    private static float noise(int x, int y, int seed) {
        long h = x * 374761393L + y * 668265263L + seed * 2147483647L;
        h = (h ^ (h >>> 13)) * 1274126177L;
        h ^= h >>> 16;
        return (h & 0xFFFF) / 65535F;
    }

    private static void write(String name, BufferedImage img) throws Exception {
        ImageIO.write(img, "png", new File(out, name + ".png"));
    }

    private static void animate(String name, int frameTime) throws Exception {
        try (FileWriter w = new FileWriter(new File(out, name + ".png.mcmeta"))) {
            w.write("{\"animation\":{\"frametime\":" + frameTime + ",\"interpolate\":true}}\n");
        }
    }
}
