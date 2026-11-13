import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;

/**
 * Paints Odin's Vault (32x32 blocks into textures/blocks, and the banners and floating runes into
 * textures/entity). Asgard builds in dark basalt and gold: great blocks of blue-black stone laid
 * without mortar, gold set into every edge, knotwork and runes cut deep and filled with light. The
 * light itself is warm, the colour of old gold; the runes burn cold, the blue of seidr.
 * <ul>
 * <li><b>vault_wall</b>, <b>vault_wall_b</b>, <b>vault_wall_c</b>: ashlar courses, the joints offset in each.</li>
 * <li><b>vault_slate</b>: polished slate for recesses and grooves, a sheen running across it.</li>
 * <li><b>vault_obsidian</b>: near-black stone veined with gold.</li>
 * <li><b>vault_floor</b>: polished floor slabs, gold-grouted, faintly veined.</li>
 * <li><b>vault_trim</b>: a gold-framed tile with a lozenge at its heart, for the aisle's edge.</li>
 * <li><b>vault_carved</b>: the Valknut in a ring, inlaid in gold.</li>
 * <li><b>vault_gold</b>: hammered gold with an engraved braid across it.</li>
 * <li><b>vault_pillar</b>: fluted stone with gold fillets.</li>
 * <li><b>vault_step</b>: worn stone for the stair treads, gold-nosed.</li>
 * <li><b>vault_runes</b> (animated): a band of runes, their cuts glowing blue, a wave of light running through them.</li>
 * <li><b>vault_lattice</b>, <b>vault_panel</b>, <b>vault_uplight</b>, <b>vault_lamp</b>: the vault's lights:
 * the roof's diamond lattice, the far wall's glowing panes, the grilles under the treasures, and gold rosette lamps.</li>
 * <li><b>vault_gate</b>: the doors, a gold grille (cutout) over light.</li>
 * <li><b>vault_brazier</b>, <b>vault_brazier_inner</b>, <b>vault_coals</b> (animated): a gold brazier and the coals in it.</li>
 * <li>entity/<b>vault_banner</b>: Odin's banner, crimson and gold, its hem cut to a point.</li>
 * <li>entity/<b>vault_glyphs</b>: sixteen runes, white on clear, for the runes that drift in the air.</li>
 * </ul>
 * Usage: javac -d out tools/VaultArt.java then java -cp out VaultArt src/main/resources/assets/eternity/textures
 */
public class VaultArt {

    private static final int S = 32;
    /** Basalt, black to its palest worn face. */
    private static final int[] STONE = {0x07080B, 0x0D0F14, 0x14171E, 0x1B1F28, 0x232833, 0x2C3240, 0x373E4E, 0x454D5F, 0x586173};
    /** Gold, dark to light. */
    private static final int[] GOLD = {0x2E1F08, 0x4E3610, 0x735016, 0x98701E, 0xBE9129, 0xDBB141, 0xF0D06E, 0xFFEDB0};
    /** The vault's light, from the edge of a pane to its heart. */
    private static final int[] LIGHT = {0xC8862A, 0xE6AE4A, 0xF6CF7C, 0xFFE6AE, 0xFFF4D8, 0xFFFCF2};
    /** Seidr: the cold glow in the runes. */
    private static final int[] SEIDR = {0x061424, 0x0B2A4A, 0x12487A, 0x1E70B0, 0x3EA0E0, 0x86D4FF, 0xD8F4FF};
    private static final int[] CRIMSON = {0x1E0405, 0x330709, 0x4D0B0E, 0x6A1014, 0x86171A, 0xA32220, 0xBF3328};

    private static File blocks, entity;

    public static void main(String[] args) throws Exception {
        File root = new File(args[0]);
        blocks = new File(root, "blocks");
        entity = new File(root, "entity");
        blocks.mkdirs();
        entity.mkdirs();
        write("vault_wall", wall(0, 1));
        write("vault_wall_b", wall(16, 2));
        write("vault_wall_c", wall(8, 3));
        write("vault_slate", slate());
        write("vault_obsidian", obsidian());
        write("vault_floor", floor());
        write("vault_trim", trim());
        write("vault_carved", carved());
        write("vault_gold", gold());
        write("vault_pillar", pillar());
        write("vault_step", step());
        write("vault_runes", runes());
        animate("vault_runes", 5);
        write("vault_lattice", lattice());
        write("vault_panel", panel());
        write("vault_uplight", uplight());
        write("vault_lamp", lamp());
        write("vault_gate", gate());
        write("vault_brazier", brazier());
        write("vault_brazier_inner", brazierInner());
        write("vault_coals", coals());
        animate("vault_coals", 6);
        ImageIO.write(banner(), "png", new File(entity, "vault_banner.png"));
        ImageIO.write(glyphs(), "png", new File(entity, "vault_glyphs.png"));
    }

    // ------------------------------------------------------------------ stone

    /** Ashlar: two courses to a block, each block of stone a block long, the joints offset by shift. */
    private static BufferedImage wall(int shift, int seed) {
        BufferedImage img = img(S, S);
        for (int y = 0; y < S; y++) {
            int course = y / 16, cy = y % 16;
            for (int x = 0; x < S; x++) {
                int cx = Math.floorMod(x + shift + course * 16, S);
                int stoneId = Math.floorMod(x + shift + course * 16, 64) / 32 + course * 2;
                float l = 3.6F + fbm(x, y, 3, seed) * 1.6F - 0.8F;
                // Each stone a slightly different shade from its neighbours.
                l += (hash(stoneId, seed, 7) - 0.5F) * 0.9F;
                // Face worn smooth toward the middle, a little rougher at its edges.
                l += fbm(x, y, 1, seed + 5) * 0.5F - 0.25F;
                // Joints: a hairline of shadow, lit along the lower and right lips.
                if (cy == 0 || cx == 0) {
                    l = 0.6F;
                } else if (cy == 1 || cx == 1) {
                    l += 1.1F;
                } else if (cy == 15 || cx == 31) {
                    l -= 0.9F;
                }
                int c = shade(STONE, l);
                // A fleck of gold in the stone, here and there.
                if (cy > 1 && cx > 1 && hash(x, y, seed + 11) > 0.997F) {
                    c = GOLD[5];
                }
                set(img, x, y, c);
            }
        }
        return img;
    }

    private static BufferedImage slate() {
        BufferedImage img = img(S, S);
        for (int y = 0; y < S; y++) {
            for (int x = 0; x < S; x++) {
                float l = 2.1F + fbm(x, y, 2, 21) * 1.0F;
                // A sheen running diagonally across it, as if from a lamp somewhere above.
                float d = Math.floorMod(x + y, S) / (float) S;
                l += Math.max(0F, 1F - Math.abs(d - 0.5F) * 6F) * 0.9F;
                // Faint diagonal striations in the stone.
                l += (hash((x - y) & 31, 3, 22) - 0.5F) * 0.4F;
                set(img, x, y, shade(STONE, l));
            }
        }
        return img;
    }

    private static BufferedImage obsidian() {
        BufferedImage img = img(S, S);
        for (int y = 0; y < S; y++) {
            for (int x = 0; x < S; x++) {
                float l = 0.9F + fbm(x, y, 2, 31) * 1.2F;
                set(img, x, y, shade(STONE, l));
                // Veins of gold where the noise crosses a ridge.
                float r = Math.abs(fbm(x, y, 3, 33) - 0.5F);
                if (r < 0.014F) {
                    set(img, x, y, r < 0.008F ? GOLD[5] : GOLD[2]);
                }
            }
        }
        return img;
    }

    /** One slab to a block, gold in its seams, polished so the light pools in it. */
    private static BufferedImage floor() {
        BufferedImage img = img(S, S);
        for (int y = 0; y < S; y++) {
            for (int x = 0; x < S; x++) {
                float l = 2.6F + fbm(x, y, 3, 41) * 1.2F;
                // Pale veins drifting through it.
                float v = Math.abs(fbm(x + 7, y * 2, 3, 43) - 0.5F);
                if (v < 0.025F) {
                    l += 1.6F;
                }
                // The polish: brighter toward one corner.
                l += (1F - (x + y) / 62F) * 0.8F;
                int c = shade(STONE, l);
                if (x == 0 || y == 0) {
                    c = (x + y) % 7 == 0 ? GOLD[4] : GOLD[2];
                } else if (x == 1 || y == 1) {
                    c = shade(STONE, l + 1.4F);
                } else if (x == 31 || y == 31) {
                    c = shade(STONE, l - 1.2F);
                }
                set(img, x, y, c);
            }
        }
        return img;
    }

    private static BufferedImage trim() {
        BufferedImage img = img(S, S);
        for (int y = 0; y < S; y++) {
            for (int x = 0; x < S; x++) {
                float l = 1.6F + fbm(x, y, 2, 51) * 0.8F;
                int c = shade(STONE, l);
                int edge = Math.min(Math.min(x, y), Math.min(31 - x, 31 - y));
                if (edge == 0) {
                    c = GOLD[1];
                } else if (edge == 2 || edge == 3) {
                    c = edge == 2 ? GOLD[6] : GOLD[3];
                }
                // A lozenge at its heart, gold-rimmed, dark inside.
                int dd = Math.abs(x - 15) + Math.abs(y - 15);
                if (dd == 9 || dd == 10) {
                    c = dd == 9 ? GOLD[6] : GOLD[3];
                } else if (dd < 9) {
                    c = shade(STONE, 0.8F + (dd < 3 ? 2.5F : 0F));
                    if (dd < 2) {
                        c = GOLD[5];
                    }
                }
                set(img, x, y, c);
            }
        }
        return img;
    }

    /** The Valknut, three triangles locked together, in a ring, inlaid in gold in dark stone. */
    private static BufferedImage carved() {
        BufferedImage img = img(S, S);
        float[][] cover = new float[S][S];
        double cx = 15.5D, cy = 16.2D;
        // Three triangles, each turned and pushed off the centre.
        double[][] segs = new double[9][];
        int n = 0;
        for (int t = 0; t < 3; t++) {
            double off = Math.PI * 2D * t / 3D - Math.PI / 2D - Math.PI / 6D;
            double ox = cx + Math.cos(off) * 3.4D, oy = cy + Math.sin(off) * 3.4D;
            double[][] p = new double[3][];
            for (int k = 0; k < 3; k++) {
                double a = -Math.PI / 2D + Math.PI * 2D * k / 3D;
                p[k] = new double[]{ox + Math.cos(a) * 7D, oy + Math.sin(a) * 7D};
            }
            for (int k = 0; k < 3; k++) {
                segs[n++] = new double[]{p[k][0], p[k][1], p[(k + 1) % 3][0], p[(k + 1) % 3][1]};
            }
        }
        for (int y = 0; y < S; y++) {
            for (int x = 0; x < S; x++) {
                double d = 99D;
                for (double[] s : segs) {
                    d = Math.min(d, segment(x + 0.5D, y + 0.5D, s));
                }
                cover[x][y] = (float) d;
            }
        }
        for (int y = 0; y < S; y++) {
            for (int x = 0; x < S; x++) {
                float l = 2.4F + fbm(x, y, 2, 61) * 1.0F;
                int c = shade(STONE, l);
                int edge = Math.min(Math.min(x, y), Math.min(31 - x, 31 - y));
                if (edge == 0) {
                    c = shade(STONE, 0.7F);
                } else if (edge == 1) {
                    c = shade(STONE, l + 1F);
                }
                double r = Math.hypot(x + 0.5D - 16D, y + 0.5D - 16D);
                // The ring: a channel cut round, gold laid in it.
                if (Math.abs(r - 13.2D) < 1.6D) {
                    c = Math.abs(r - 13.2D) < 0.75D ? GOLD[r < 13.2D ? 6 : 4] : shade(STONE, 0.6F);
                } else if (r < 11.6D) {
                    c = shade(STONE, l - 0.9F);
                }
                float d = cover[x][y];
                if (d < 1.25F) {
                    c = d < 0.6F ? GOLD[6] : GOLD[3];
                }
                set(img, x, y, c);
            }
        }
        return img;
    }

    /** Hammered gold, an engraved braid running across it. */
    private static BufferedImage gold() {
        BufferedImage img = img(S, S);
        for (int y = 0; y < S; y++) {
            for (int x = 0; x < S; x++) {
                // Hammer dimples: the distance to the nearest of a scatter of points.
                float dimple = cells(x, y, 6, 71);
                float l = 4.2F + dimple * 1.6F - 0.6F + fbm(x, y, 1, 72) * 0.5F;
                if (y == 0) {
                    l = 6.5F;
                } else if (y == 31) {
                    l = 1F;
                } else if (y == 1) {
                    l = 5.5F;
                }
                // The braid: two strands crossing in the middle band.
                if (y >= 10 && y <= 21) {
                    double s1 = 15.5D + Math.sin((x + 0.5D) * Math.PI / 8D) * 4D;
                    double s2 = 15.5D - Math.sin((x + 0.5D) * Math.PI / 8D) * 4D;
                    double d1 = Math.abs(y + 0.5D - s1), d2 = Math.abs(y + 0.5D - s2);
                    boolean over1 = Math.floorMod(x, 16) < 8;
                    l = 2.4F;
                    if (d1 < 1.6D && (over1 || d2 >= 1.6D)) {
                        l = d1 < 0.7D ? 6.8F : 5F;
                    } else if (d2 < 1.6D) {
                        l = d2 < 0.7D ? 6.4F : 4.6F;
                    }
                    if (y == 10 || y == 21) {
                        l = 1.2F;
                    }
                }
                set(img, x, y, shade(GOLD, l));
            }
        }
        return img;
    }

    private static BufferedImage pillar() {
        BufferedImage img = img(S, S);
        for (int y = 0; y < S; y++) {
            for (int x = 0; x < S; x++) {
                int f = x % 8;
                // Each flute hollowed: dark at its heart, lit along one lip.
                float profile = (float) Math.cos((f - 3.5D) / 4D * Math.PI);
                float l = 3.8F - profile * 1.8F + fbm(x, y, 2, 81) * 0.7F;
                if (f == 1) {
                    l += 1F;
                }
                int c = shade(STONE, l);
                if (f == 0) {
                    c = (y + x / 8) % 8 == 0 ? GOLD[7] : GOLD[4];
                }
                set(img, x, y, c);
            }
        }
        return img;
    }

    private static BufferedImage step() {
        BufferedImage img = img(S, S);
        for (int y = 0; y < S; y++) {
            for (int x = 0; x < S; x++) {
                float l = 3.4F + fbm(x, y, 3, 91) * 1.2F;
                // Worn smoother and paler down the middle where everyone walks.
                l += Math.max(0F, 1F - Math.abs(x - 15.5F) / 10F) * 0.8F;
                int c = shade(STONE, l);
                if (y < 3) {
                    c = y == 1 ? GOLD[6] : GOLD[y == 0 ? 3 : 2];
                } else if (y == 3) {
                    c = shade(STONE, 0.8F);
                } else if (x == 0 || x == 31) {
                    c = shade(STONE, 1.4F);
                }
                set(img, x, y, c);
            }
        }
        return img;
    }

    /** A band of runes cut in slate between two gold lines; their cuts glow, and a wave of light runs along them. */
    private static BufferedImage runes() {
        int frames = 12;
        BufferedImage img = img(S, S * frames);
        int[] which = {3, 10, 6};
        float[][] glyph = new float[S][S];
        for (int i = 0; i < 3; i++) {
            float[][] g = rune(which[i], 9, 13, 1.25F);
            for (int y = 0; y < 13; y++) {
                for (int x = 0; x < 9; x++) {
                    int px = 1 + i * 10 + x, py = 9 + y;
                    if (px < S) {
                        glyph[px][py] = Math.max(glyph[px][py], g[x][y]);
                    }
                }
            }
        }
        for (int f = 0; f < frames; f++) {
            for (int y = 0; y < S; y++) {
                for (int x = 0; x < S; x++) {
                    float l = 1.8F + fbm(x, y, 2, 101) * 0.8F;
                    int c = shade(STONE, l);
                    if (y == 5 || y == 26) {
                        c = GOLD[5];
                    } else if (y == 4 || y == 27) {
                        c = GOLD[2];
                    } else if (y > 5 && y < 26) {
                        c = shade(STONE, l - 0.6F);
                    }
                    float g = glyph[x][y];
                    if (g > 0.05F) {
                        float wave = 0.5F + 0.5F * (float) Math.sin(Math.PI * 2D * (x / (double) S - f / (double) frames));
                        float glow = 0.35F + 0.65F * wave * wave;
                        c = mix(c, shade(SEIDR, 2F + glow * 4.2F), Math.min(1F, g * 1.4F));
                    } else {
                        // The stone about each rune catches a little of its light.
                        float near = 0F;
                        for (int dy = -2; dy <= 2; dy++) {
                            for (int dx = -2; dx <= 2; dx++) {
                                int nx = x + dx, ny = y + dy;
                                if (nx >= 0 && ny >= 0 && nx < S && ny < S) {
                                    near = Math.max(near, glyph[nx][ny] / (1F + Math.abs(dx) + Math.abs(dy)));
                                }
                            }
                        }
                        if (near > 0F && y > 5 && y < 26) {
                            c = mix(c, SEIDR[2], near * 0.5F);
                        }
                    }
                    set(img, x, y + f * S, c);
                }
            }
        }
        return img;
    }

    // ------------------------------------------------------------------ light

    /** The roof's lattice: a gold diamond grille over light that tiles into one long lattice. */
    private static BufferedImage lattice() {
        BufferedImage img = img(S, S);
        for (int y = 0; y < S; y++) {
            for (int x = 0; x < S; x++) {
                double r = Math.hypot(x - 15.5D, y - 15.5D) / 22D;
                int c = shade(LIGHT, 5F - (float) r * 3.5F + fbm(x, y, 2, 111) * 0.4F);
                int d1 = Math.floorMod(x - y, 16), d2 = Math.floorMod(x + y + 1, 16);
                if (d1 <= 1 || d2 <= 1) {
                    c = d1 == 0 || d2 == 0 ? GOLD[6] : GOLD[3];
                }
                // Where the bars cross, a stud.
                if (Math.floorMod(x - y, 16) <= 1 && Math.floorMod(x + y + 1, 16) <= 1) {
                    c = GOLD[7];
                }
                int edge = Math.min(Math.min(x, y), Math.min(31 - x, 31 - y));
                if (edge == 0) {
                    c = GOLD[2];
                }
                set(img, x, y, c);
            }
        }
        return img;
    }

    /** A pane of the wall of light: brightest at its heart, threads of brighter light running up it. */
    private static BufferedImage panel() {
        BufferedImage img = img(S, S);
        for (int y = 0; y < S; y++) {
            for (int x = 0; x < S; x++) {
                double r = Math.max(Math.abs(x - 15.5D), Math.abs(y - 15.5D)) / 16D;
                float l = 4.6F - (float) (r * r) * 2.6F;
                l += (fbm(x * 3, y / 4, 2, 121) - 0.5F) * 0.8F;
                int c = shade(LIGHT, l);
                int edge = Math.min(Math.min(x, y), Math.min(31 - x, 31 - y));
                if (edge == 0) {
                    c = GOLD[3];
                } else if (edge == 1) {
                    c = GOLD[6];
                }
                set(img, x, y, c);
            }
        }
        return img;
    }

    /** The grille set in the floor under a treasure: a gold ring, spokes, and light welling up through them. */
    private static BufferedImage uplight() {
        BufferedImage img = img(S, S);
        for (int y = 0; y < S; y++) {
            for (int x = 0; x < S; x++) {
                double dx = x + 0.5D - 16D, dy = y + 0.5D - 16D;
                double r = Math.hypot(dx, dy);
                int c = shade(STONE, 2F + fbm(x, y, 2, 131));
                if (r < 14D) {
                    c = shade(LIGHT, 5.2F - (float) r / 4F);
                    double a = Math.atan2(dy, dx);
                    boolean spoke = Math.abs(Math.sin(a * 6D)) < 0.16D && r > 3D;
                    if (spoke || Math.abs(r - 8D) < 0.8D) {
                        c = GOLD[5];
                    }
                }
                if (Math.abs(r - 14.2D) < 1.2D) {
                    c = r < 14.2D ? GOLD[6] : GOLD[3];
                }
                int edge = Math.min(Math.min(x, y), Math.min(31 - x, 31 - y));
                if (edge == 0) {
                    c = GOLD[4];
                }
                set(img, x, y, c);
            }
        }
        return img;
    }

    /** A rosette of gold petals round a heart of light. */
    private static BufferedImage lamp() {
        BufferedImage img = img(S, S);
        for (int y = 0; y < S; y++) {
            for (int x = 0; x < S; x++) {
                double dx = x + 0.5D - 16D, dy = y + 0.5D - 16D;
                double r = Math.hypot(dx, dy), a = Math.atan2(dy, dx);
                double petal = 9.5D + Math.cos(a * 8D) * 3.2D;
                int c = shade(GOLD, 2.4F + cells(x, y, 6, 141) * 1.2F);
                if (r < petal) {
                    c = shade(GOLD, 5.6F - (float) (r / petal) * 2.4F);
                }
                if (r < 6D) {
                    c = shade(LIGHT, 5.4F - (float) r * 0.5F);
                }
                int edge = Math.min(Math.min(x, y), Math.min(31 - x, 31 - y));
                if (edge == 0) {
                    c = GOLD[1];
                } else if (edge == 1) {
                    c = GOLD[6];
                }
                set(img, x, y, c);
            }
        }
        return img;
    }

    /** The doors: a gold grille of uprights and a diamond lattice; between the bars, nothing (light shows through). */
    private static BufferedImage gate() {
        BufferedImage img = img(S, S);
        for (int y = 0; y < S; y++) {
            for (int x = 0; x < S; x++) {
                int upright = x % 16;
                int d1 = Math.floorMod(x - y, 16), d2 = Math.floorMod(x + y + 1, 16);
                int c = -1;
                if (upright <= 2) {
                    c = upright == 1 ? GOLD[6] : GOLD[3];
                    if (y % 8 == 0) {
                        c = GOLD[7];
                    }
                } else if (d1 == 0 || d2 == 0) {
                    c = GOLD[5];
                } else if (y == 0 || y == 31) {
                    c = GOLD[4];
                }
                if (c >= 0) {
                    set(img, x, y, c);
                }
            }
        }
        return img;
    }

    // ------------------------------------------------------------------ the braziers

    private static BufferedImage brazier() {
        BufferedImage img = img(S, S);
        for (int y = 0; y < S; y++) {
            for (int x = 0; x < S; x++) {
                float l = 4F + cells(x, y, 5, 151) * 1.4F - 0.5F;
                // Engraved bands round it every few pixels.
                if (y % 8 == 3) {
                    l = 1.5F;
                } else if (y % 8 == 4) {
                    l = 6.3F;
                }
                // A row of rivets.
                if (y % 8 == 6 && x % 4 == 1) {
                    l = 7F;
                }
                set(img, x, y, shade(GOLD, l));
            }
        }
        return img;
    }

    private static BufferedImage brazierInner() {
        BufferedImage img = img(S, S);
        for (int y = 0; y < S; y++) {
            for (int x = 0; x < S; x++) {
                set(img, x, y, mix(shade(STONE, 0.6F + fbm(x, y, 2, 161)), CRIMSON[3], 0.2F + 0.2F * fbm(x, y, 3, 162)));
            }
        }
        return img;
    }

    /** Coals: black crusts with fire showing through the cracks, the glow moving under them. */
    private static BufferedImage coals() {
        int frames = 6;
        BufferedImage img = img(S, S * frames);
        int[] fire = {0x2A0602, 0x5A1004, 0x9A2A06, 0xD8540C, 0xFF8A1E, 0xFFC04A, 0xFFEFA0};
        for (int f = 0; f < frames; f++) {
            double phase = Math.PI * 2D * f / frames;
            for (int y = 0; y < S; y++) {
                for (int x = 0; x < S; x++) {
                    float crack = cells(x, y, 6, 171);
                    float heat = 0.5F + 0.5F * (float) Math.sin(phase + fbm(x, y, 2, 172) * 9D);
                    int c;
                    if (crack < 0.22F) {
                        c = shade(fire, 3F + heat * 3F - crack * 6F);
                    } else {
                        c = mix(shade(STONE, 0.6F + crack), fire[2], heat * 0.35F * (1F - crack));
                    }
                    set(img, x, y + f * S, c);
                }
            }
        }
        return img;
    }

    // ------------------------------------------------------------------ the banners and the floating runes

    /**
     * Odin's banner, 64x256 (two blocks wide, eight tall): crimson cloth, a gold border, the
     * Valknut, three runes down it, and a pointed hem fringed with gold.
     */
    private static BufferedImage banner() {
        int w = 64, h = 256;
        BufferedImage img = img(w, h);
        double[][] valknut = new double[9][];
        int n = 0;
        double cx = 31.5D, cy = 64D;
        for (int t = 0; t < 3; t++) {
            double off = Math.PI * 2D * t / 3D - Math.PI / 2D - Math.PI / 6D;
            double ox = cx + Math.cos(off) * 6.5D, oy = cy + Math.sin(off) * 6.5D;
            double[][] p = new double[3][];
            for (int k = 0; k < 3; k++) {
                double a = -Math.PI / 2D + Math.PI * 2D * k / 3D;
                p[k] = new double[]{ox + Math.cos(a) * 14.5D, oy + Math.sin(a) * 14.5D};
            }
            for (int k = 0; k < 3; k++) {
                valknut[n++] = new double[]{p[k][0], p[k][1], p[(k + 1) % 3][0], p[(k + 1) % 3][1]};
            }
        }
        int[] word = {3, 14, 11};
        float[][][] letters = new float[3][][];
        for (int i = 0; i < 3; i++) {
            letters[i] = rune(word[i], 18, 26, 2.6F);
        }
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                // The hem, cut to a point.
                double tail = 256D - 26D * (1D - Math.abs(x - 31.5D) / 32D);
                if (y > tail) {
                    continue;
                }
                // The weave, and folds hanging down it.
                float fold = (float) Math.sin(x / 64D * Math.PI * 3D) * 0.8F;
                float l = 3.6F + fold + fbm(x, y, 2, 181) * 0.6F + (((x + y) & 1) == 0 ? 0.15F : -0.15F);
                int c = shade(CRIMSON, l);
                int edge = Math.min(x, w - 1 - x);
                if (y < 6) {
                    // The hem the pole runs through.
                    c = shade(GOLD, y == 0 ? 2F : 4.6F + fold * 0.5F - (y == 5 ? 2F : 0F));
                } else if (edge < 4 || (y - tail) > -5D) {
                    c = shade(GOLD, edge == 0 || (y - tail) > -1D ? 2.2F : 4.8F + fold * 0.6F);
                } else if (edge == 6 || y == 12) {
                    c = shade(GOLD, 5F + fold * 0.6F);
                }
                // The Valknut in a ring.
                double r = Math.hypot(x + 0.5D - cx, y + 0.5D - cy);
                if (Math.abs(r - 23D) < 1.2D) {
                    c = shade(GOLD, 5.5F + fold * 0.5F);
                }
                double d = 99D;
                for (double[] s : valknut) {
                    d = Math.min(d, segment(x + 0.5D, y + 0.5D, s));
                }
                if (d < 2.2D) {
                    c = d < 1.2D ? shade(GOLD, 6F + fold * 0.5F) : shade(GOLD, 2.5F);
                }
                // Three runes down the middle: Ansuz, Odin's own, and the others of his name.
                for (int i = 0; i < 3; i++) {
                    int lx = x - 23, ly = y - (106 + i * 38);
                    if (lx >= 0 && ly >= 0 && lx < 18 && ly < 26 && letters[i][lx][ly] > 0.4F) {
                        c = shade(GOLD, 5.8F + fold * 0.5F);
                    }
                }
                set(img, x, y, c);
            }
        }
        // A fringe of gold threads hanging from the tail's edge.
        for (int x = 1; x < w - 1; x += 2) {
            double tail = 256D - 26D * (1D - Math.abs(x - 31.5D) / 32D);
            for (int k = 1; k < 4 + (x * 7 % 3); k++) {
                set(img, x, (int) tail + k, GOLD[k % 2 == 0 ? 4 : 6]);
            }
        }
        return img;
    }

    /** Sixteen runes on a 128x32 sheet (eight to a row, 16x16 each), white with a soft halo. */
    private static BufferedImage glyphs() {
        BufferedImage img = img(128, 32);
        for (int i = 0; i < 16; i++) {
            float[][] g = rune(i, 16, 16, 1.6F);
            int ox = (i % 8) * 16, oy = (i / 8) * 16;
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    float halo = 0F;
                    for (int dy = -2; dy <= 2; dy++) {
                        for (int dx = -2; dx <= 2; dx++) {
                            int nx = x + dx, ny = y + dy;
                            if (nx >= 0 && ny >= 0 && nx < 16 && ny < 16) {
                                halo = Math.max(halo, g[nx][ny] / (1.5F + dx * dx + dy * dy));
                            }
                        }
                    }
                    float a = Math.max(g[x][y], halo * 0.9F);
                    if (a > 0.01F) {
                        img.setRGB(ox + x, oy + y, ((int) (Math.min(1F, a) * 255) << 24) | 0xFFFFFF);
                    }
                }
            }
        }
        return img;
    }

    // ------------------------------------------------------------------ the runes themselves

    /**
     * The Elder Futhark (sixteen of it), each as strokes on an 8 wide by 12 tall grid: Fehu, Uruz,
     * Thurisaz, Ansuz, Raido, Kenaz, Gebo, Wunjo, Hagalaz, Naudiz, Isa, Tiwaz, Algiz, Sowilo, Othala, Mannaz.
     */
    private static final double[][][] FUTHARK = {
            {{2, 0, 2, 12}, {2, 4, 7, 0}, {2, 8, 7, 4}},
            {{1, 12, 1, 0}, {1, 0, 7, 4}, {7, 4, 7, 12}},
            {{2, 0, 2, 12}, {2, 3, 6, 6}, {6, 6, 2, 9}},
            {{2, 0, 2, 12}, {2, 0, 7, 4}, {2, 4, 7, 8}},
            {{2, 0, 2, 12}, {2, 0, 6, 3}, {6, 3, 2, 6}, {2, 6, 7, 12}},
            {{6, 1, 2, 6}, {2, 6, 6, 11}},
            {{1, 1, 7, 11}, {7, 1, 1, 11}},
            {{2, 0, 2, 12}, {2, 0, 6, 3}, {6, 3, 2, 6}},
            {{1, 0, 1, 12}, {7, 0, 7, 12}, {1, 4, 7, 8}},
            {{4, 0, 4, 12}, {1, 4, 7, 8}},
            {{4, 0, 4, 12}},
            {{4, 0, 4, 12}, {4, 0, 0.5, 4}, {4, 0, 7.5, 4}},
            {{4, 0, 4, 12}, {4, 5, 0.5, 0.5}, {4, 5, 7.5, 0.5}},
            {{6, 0, 2, 4}, {2, 4, 6, 8}, {6, 8, 2, 12}},
            {{4, 0, 7, 4}, {7, 4, 4, 8}, {4, 8, 1, 4}, {1, 4, 4, 0}, {2.5, 6.5, 0, 12}, {5.5, 6.5, 8, 12}},
            {{1, 0, 1, 12}, {7, 0, 7, 12}, {1, 0, 7, 6}, {7, 0, 1, 6}},
    };

    /** A rune's coverage on a w by h grid (1 on its strokes, fading off their edges), strokes thick wide. */
    private static float[][] rune(int index, int w, int h, float thick) {
        float[][] out = new float[w][h];
        double sx = (w - 2D) / 8D, sy = (h - 2D) / 12D;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                double d = 99D;
                for (double[] s : FUTHARK[index]) {
                    d = Math.min(d, segment(x + 0.5D, y + 0.5D, new double[]{1 + s[0] * sx, 1 + s[1] * sy, 1 + s[2] * sx, 1 + s[3] * sy}));
                }
                out[x][y] = (float) Math.max(0D, Math.min(1D, thick / 2D + 0.5D - d));
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ helpers

    private static double segment(double px, double py, double[] s) {
        double dx = s[2] - s[0], dy = s[3] - s[1];
        double t = ((px - s[0]) * dx + (py - s[1]) * dy) / (dx * dx + dy * dy);
        t = Math.max(0D, Math.min(1D, t));
        return Math.hypot(px - (s[0] + dx * t), py - (s[1] + dy * t));
    }

    /** A colour from a palette at a fractional level, blended between its steps. */
    private static int shade(int[] pal, float l) {
        l = Math.max(0F, Math.min(pal.length - 1, l));
        int i = (int) l;
        return i >= pal.length - 1 ? pal[pal.length - 1] : mix(pal[i], pal[i + 1], l - i);
    }

    /** Value noise that tiles every 32 pixels, at a cell size of 32 / 2^octave, summed over octaves. */
    private static float fbm(int x, int y, int octaves, int seed) {
        float sum = 0F, amp = 0.5F, norm = 0F;
        for (int o = 0; o < octaves; o++) {
            int cells = 2 << o;
            sum += value(x, y, cells, seed + o * 13) * amp;
            norm += amp;
            amp *= 0.5F;
        }
        return sum / norm;
    }

    private static float value(int x, int y, int cells, int seed) {
        double cs = (double) S / cells;
        double fx = x / cs, fy = y / cs;
        int x0 = (int) Math.floor(fx), y0 = (int) Math.floor(fy);
        double tx = smooth(fx - x0), ty = smooth(fy - y0);
        float a = hash(Math.floorMod(x0, cells), Math.floorMod(y0, cells), seed);
        float b = hash(Math.floorMod(x0 + 1, cells), Math.floorMod(y0, cells), seed);
        float c = hash(Math.floorMod(x0, cells), Math.floorMod(y0 + 1, cells), seed);
        float d = hash(Math.floorMod(x0 + 1, cells), Math.floorMod(y0 + 1, cells), seed);
        return (float) ((a * (1 - tx) + b * tx) * (1 - ty) + (c * (1 - tx) + d * tx) * ty);
    }

    /** Distance to the nearest of a scatter of points (one per cell, cells across 32 pixels), 0 to about 1; tiles. */
    private static float cells(int x, int y, int cells, int seed) {
        double cs = (double) S / cells;
        double best = 9D;
        int cx = (int) (x / cs), cy = (int) (y / cs);
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int gx = cx + dx, gy = cy + dy;
                double px = (gx + hash(Math.floorMod(gx, cells), Math.floorMod(gy, cells), seed)) * cs;
                double py = (gy + hash(Math.floorMod(gx, cells), Math.floorMod(gy, cells), seed + 1)) * cs;
                best = Math.min(best, Math.hypot(x + 0.5D - px, y + 0.5D - py) / cs);
            }
        }
        return (float) Math.min(1D, best);
    }

    private static double smooth(double t) {
        return t * t * (3D - 2D * t);
    }

    private static float hash(int x, int y, int seed) {
        long h = x * 374761393L + y * 668265263L + seed * 2147483647L;
        h = (h ^ (h >>> 13)) * 1274126177L;
        h ^= h >>> 16;
        return (h & 0xFFFF) / 65535F;
    }

    private static int mix(int a, int b, float t) {
        t = Math.max(0F, Math.min(1F, t));
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }

    private static BufferedImage img(int w, int h) {
        return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    }

    private static void set(BufferedImage img, int x, int y, int c) {
        if (x >= 0 && y >= 0 && x < img.getWidth() && y < img.getHeight()) {
            img.setRGB(x, y, 0xFF000000 | c);
        }
    }

    private static void write(String name, BufferedImage img) throws Exception {
        ImageIO.write(img, "png", new File(blocks, name + ".png"));
    }

    private static void animate(String name, int frameTime) throws Exception {
        try (FileWriter w = new FileWriter(new File(blocks, name + ".png.mcmeta"))) {
            w.write("{\"animation\":{\"frametime\":" + frameTime + ",\"interpolate\":true}}\n");
        }
    }
}
