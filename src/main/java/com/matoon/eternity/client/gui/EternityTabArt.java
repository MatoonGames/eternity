package com.matoon.eternity.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/**
 * The Eternity creative tab's panel, painted fresh every frame: a window onto the space he is made of.
 * Nebulae drift through it in violet and teal, a galaxy turns slowly behind the slots, stars twinkle
 * and now and then flare into four-pointed sparks. Round it, a frame of gold with a violet inner
 * line, four gem studs at its corners, and two motes of light that chase each other round it for
 * ever. The slots are wells of dark glass whose lower rims breathe in a slow wave across the grid.
 * <p>
 * The panel keeps vanilla's layout exactly (195 x 136: a 9 x 5 grid, the hotbar under it, the
 * scrollbar track at the right), and the same texture holds the scroll knob, lit and unlit, where
 * vanilla's tabs sheet keeps its own, at (232, 0) and (244, 0). Also here: a soft round glow, for
 * the auras {@link EternityTabFx} draws round the tab.
 */
@SideOnly(Side.CLIENT)
public final class EternityTabArt {

    public static final int W = 195, H = 136;
    private static final int SIZE = 256;
    /** Where the knob sprites sit, lit and unlit, each 12 x 15. */
    public static final int KNOB_U = 232, KNOB_DIM_U = 244;
    /** Where the tab button sprites sit, each 28 x 32: on top open, on top shut, below open, below shut. */
    public static final int TAB_V = 140;
    /** Repaints at most this often, in milliseconds: about thirty times a second. */
    private static final long FRAME_MS = 33L;

    /** What a pixel of the frame, or of a slot, is, so the animation knows what to do with it. */
    private static final byte NONE = 0, RIM = 1, FRAME = 2, GEM = 3;

    private static ResourceLocation panel, glow;
    private static DynamicTexture texture;
    private static long painted = -1L;

    private static float[] nebulaA, nebulaB;
    private static float[] radius, angle;
    /** The still parts, laid over the sky: colour, how opaque, and what kind of pixel each is. */
    private static int[] overlay;
    private static float[] overlayAlpha;
    private static byte[] kind;
    /** Where round the frame each frame pixel sits, 0 to 1 clockwise from the top left. */
    private static float[] perimeter;
    private static Star[] stars;

    private EternityTabArt() {
    }

    /** The panel, repainted if a frame's time has gone by since it last was. */
    public static ResourceLocation panel() {
        if (panel == null) {
            build();
        }
        long now = System.currentTimeMillis();
        if (now - painted >= FRAME_MS) {
            painted = now;
            paint((now % 3_600_000L) / 1000D);
            texture.updateDynamicTexture();
        }
        return panel;
    }

    /** A soft white disc, fading out to nothing at its edge, for additive glows. */
    public static ResourceLocation glow() {
        if (glow == null) {
            int n = 32;
            DynamicTexture t = new DynamicTexture(n, n);
            int[] px = t.getTextureData();
            for (int y = 0; y < n; y++) {
                for (int x = 0; x < n; x++) {
                    double dx = (x + 0.5D) / n * 2D - 1D, dy = (y + 0.5D) / n * 2D - 1D;
                    double d = Math.sqrt(dx * dx + dy * dy);
                    double a = d >= 1D ? 0D : Math.pow(1D - d, 2.2D);
                    px[y * n + x] = ((int) (a * 255D) << 24) | 0xFFFFFF;
                }
            }
            t.updateDynamicTexture();
            glow = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation("eternity_tab_glow", t);
        }
        return glow;
    }

    // ------------------------------------------------------------------ painting

    private static void paint(double t) {
        int[] px = texture.getTextureData();
        float ax = (float) (t * 2.2D), ay = (float) (t * 0.9D);
        float bx = (float) (-t * 1.6D), by = (float) (t * 1.3D);
        float spin = (float) (t * 0.08D);
        float[] rgb = new float[W * H * 3];

        for (int y = 0; y < H; y++) {
            float v = y / (float) H;
            for (int x = 0; x < W; x++) {
                int k = y * W + x;
                // The dark itself: indigo at the top, a deeper violet at the bottom.
                float r = 0.030F + 0.025F * v, g = 0.018F + 0.006F * v, b = 0.095F + 0.050F * v;

                float a = sample(nebulaA, x + ax, y + ay);
                float c = sample(nebulaB, x + bx, y + by);
                float violet = smooth(0.46F, 0.86F, a), teal = smooth(0.56F, 0.92F, c);
                r += violet * 0.40F + teal * 0.03F;
                g += violet * 0.07F + teal * 0.27F;
                b += violet * 0.58F + teal * 0.42F;
                // Bright filaments along one contour of the violet cloud.
                float fil = 1F - Math.abs(a - 0.63F) * 16F;
                if (fil > 0F) {
                    fil *= fil * smooth(0.3F, 0.8F, c + 0.25F);
                    r += fil * 0.30F;
                    g += fil * 0.13F;
                    b += fil * 0.32F;
                }

                // A galaxy, turning slowly behind everything: two arms and a golden core.
                float rr = radius[k];
                float arm = (float) Math.cos(2D * (angle[k] - spin) - Math.log(rr + 1F) * 3.4D);
                float arms = arm > 0F ? arm * arm * arm * (float) Math.exp(-rr / 34F) * 0.55F : 0F;
                float core = (float) Math.exp(-rr * rr / 55F);
                r += arms * 0.50F + core * 0.85F;
                g += arms * 0.40F + core * 0.62F;
                b += arms * 0.70F + core * 0.38F;

                rgb[k * 3] = r;
                rgb[k * 3 + 1] = g;
                rgb[k * 3 + 2] = b;
            }
        }

        for (Star s : stars) {
            double tw = 0.5D + 0.5D * Math.sin(t * s.speed + s.phase);
            float bright = (float) (s.base * (0.30D + 0.70D * tw * tw));
            add(rgb, s.x, s.y, s.r * bright, s.g * bright, s.b * bright);
            if (s.big && tw > 0.72D) {
                // At its brightest a big star flares into a four-pointed spark.
                float f = (float) ((tw - 0.72D) / 0.28D) * bright;
                for (int d = 1; d <= 2; d++) {
                    float arm = f * (d == 1 ? 0.75F : 0.30F);
                    add(rgb, s.x + d, s.y, s.r * arm, s.g * arm, s.b * arm);
                    add(rgb, s.x - d, s.y, s.r * arm, s.g * arm, s.b * arm);
                    add(rgb, s.x, s.y + d, s.r * arm, s.g * arm, s.b * arm);
                    add(rgb, s.x, s.y - d, s.r * arm, s.g * arm, s.b * arm);
                }
            }
        }

        // Two motes of light chase each other round the frame, each with a tail.
        float lead = (float) ((t / 7D) % 1D);
        // The slot rims breathe in a wave that rolls down and across the grid.
        double breath = t * 2.1D;
        float gemPulse = (float) (0.5D + 0.5D * Math.sin(t * 1.7D));

        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                int k = y * W + x;
                float alpha = overlayAlpha[k];
                if (alpha < 0F) {
                    px[y * SIZE + x] = 0;
                    continue;
                }
                float r = rgb[k * 3], g = rgb[k * 3 + 1], b = rgb[k * 3 + 2];
                int o = overlay[k];
                r = r * (1F - alpha) + ((o >> 16) & 0xFF) / 255F * alpha;
                g = g * (1F - alpha) + ((o >> 8) & 0xFF) / 255F * alpha;
                b = b * (1F - alpha) + (o & 0xFF) / 255F * alpha;

                switch (kind[k]) {
                    case RIM: {
                        float p = (float) (0.5D + 0.5D * Math.sin(breath - (x + y) * 0.07D));
                        r += p * 0.13F;
                        g += p * 0.06F;
                        b += p * 0.22F;
                        break;
                    }
                    case FRAME: {
                        float light = mote(perimeter[k], lead) + mote(perimeter[k], (lead + 0.5F) % 1F);
                        r += light * 1.00F;
                        g += light * 0.90F;
                        b += light * 0.65F;
                        break;
                    }
                    case GEM:
                        r += gemPulse * 0.25F;
                        g += gemPulse * 0.10F;
                        b += gemPulse * 0.35F;
                        break;
                    default:
                        break;
                }
                px[y * SIZE + x] = 0xFF000000 | (channel(r) << 16) | (channel(g) << 8) | channel(b);
            }
        }
    }

    /** How lit a frame pixel is by a mote at {@code at}: a bright head, and a tail behind it. */
    private static float mote(float s, float at) {
        float d = s - at;
        if (d > 0.5F) {
            d -= 1F;
        } else if (d < -0.5F) {
            d += 1F;
        }
        if (d > 0F) {
            float h = d / 0.006F;
            return (float) Math.exp(-h * h);
        }
        float tail = 1F + d / 0.09F;
        float head = (float) Math.exp(-(d / 0.006F) * (d / 0.006F));
        return Math.max(head, tail > 0F ? tail * tail * 0.55F : 0F);
    }

    private static void add(float[] rgb, int x, int y, float r, float g, float b) {
        if (x < 0 || y < 0 || x >= W || y >= H) {
            return;
        }
        int k = (y * W + x) * 3;
        rgb[k] += r;
        rgb[k + 1] += g;
        rgb[k + 2] += b;
    }

    private static int channel(float v) {
        return v <= 0F ? 0 : v >= 1F ? 255 : (int) (v * 255F);
    }

    private static float smooth(float lo, float hi, float v) {
        float u = Math.max(0F, Math.min(1F, (v - lo) / (hi - lo)));
        return u * u * (3F - 2F * u);
    }

    /** Reads a tiling field between its texels. */
    private static float sample(float[] field, float x, float y) {
        int x0 = (int) Math.floor(x), y0 = (int) Math.floor(y);
        float fx = x - x0, fy = y - y0;
        int ax = x0 & 255, ay = y0 & 255, bx = (x0 + 1) & 255, by = (y0 + 1) & 255;
        float top = field[ay * 256 + ax] * (1F - fx) + field[ay * 256 + bx] * fx;
        float bottom = field[by * 256 + ax] * (1F - fx) + field[by * 256 + bx] * fx;
        return top * (1F - fy) + bottom * fy;
    }

    // ------------------------------------------------------------------ building, once

    private static void build() {
        Random rand = new Random(0xE7E2_17L);
        nebulaA = fbm(rand, 64, 5);
        nebulaB = fbm(rand, 64, 4);

        radius = new float[W * H];
        angle = new float[W * H];
        // The galaxy's heart sits a little up and left of the grid's middle, so its core lands between slots.
        float cx = 97F, cy = 62F;
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                float dx = x - cx, dy = (y - cy) * 1.6F;
                radius[y * W + x] = (float) Math.sqrt(dx * dx + dy * dy);
                angle[y * W + x] = (float) Math.atan2(dy, dx);
            }
        }

        stars = new Star[170];
        for (int i = 0; i < stars.length; i++) {
            stars[i] = new Star(rand);
        }

        overlay = new int[W * H];
        overlayAlpha = new float[W * H];
        kind = new byte[W * H];
        perimeter = new float[W * H];
        frame();
        slots();
        divider();
        track();
        title();

        texture = new DynamicTexture(SIZE, SIZE);
        knob(texture.getTextureData(), KNOB_U, false);
        knob(texture.getTextureData(), KNOB_DIM_U, true);
        Random sky = new Random(0x7AB5L);
        for (int i = 0; i < 4; i++) {
            tab(texture.getTextureData(), i * 28, i < 2, i % 2 == 0, sky);
        }
        panel = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation("eternity_creative_tab", texture);
    }

    private static void put(int x, int y, int rgb, float alpha, byte what) {
        if (x < 0 || y < 0 || x >= W || y >= H) {
            return;
        }
        int k = y * W + x;
        overlay[k] = rgb;
        overlayAlpha[k] = alpha;
        kind[k] = what;
    }

    /** The gold frame: a dark outline, a bevel lit from the top left, a violet inner line, and a soft shadow inside it. */
    private static void frame() {
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                int e = Math.min(Math.min(x, y), Math.min(W - 1 - x, H - 1 - y));
                boolean lit = Math.min(x, y) == e;
                switch (e) {
                    case 0:
                        put(x, y, 0x14081F, 1F, NONE);
                        break;
                    case 1:
                        put(x, y, lit ? 0xF8E19A : 0x9A6420, 1F, FRAME);
                        break;
                    case 2:
                        put(x, y, lit ? 0xD6A548 : 0x6E4216, 1F, FRAME);
                        break;
                    case 3:
                        put(x, y, 0x7C3FD0, 0.9F, NONE);
                        break;
                    case 4:
                        put(x, y, 0x000000, 0.45F, NONE);
                        break;
                    default:
                        break;
                }
                if (e == 1 || e == 2) {
                    perimeter[y * W + x] = around(x, y);
                }
            }
        }
        // Rounded corners, as vanilla's are.
        int[][] cut = {{0, 0}, {1, 0}, {0, 1}, {W - 1, 0}, {W - 2, 0}, {W - 1, 1}, {0, H - 1}, {1, H - 1}, {0, H - 2},
                {W - 1, H - 1}, {W - 2, H - 1}, {W - 1, H - 2}};
        for (int[] c : cut) {
            overlayAlpha[c[1] * W + c[0]] = -1F;
        }
        put(1, 1, 0x14081F, 1F, NONE);
        put(W - 2, 1, 0x14081F, 1F, NONE);
        put(1, H - 2, 0x14081F, 1F, NONE);
        put(W - 2, H - 2, 0x14081F, 1F, NONE);

        gem(4, 4, 3);
        gem(W - 5, 4, 3);
        gem(4, H - 5, 3);
        gem(W - 5, H - 5, 3);
    }

    /** Where a frame pixel sits round the panel, 0 to 1, clockwise from the top left. */
    private static float around(int x, int y) {
        float w = W - 1, h = H - 1, total = 2F * (w + h);
        int e = Math.min(Math.min(x, y), Math.min(W - 1 - x, H - 1 - y));
        float s;
        if (y == e) {
            s = x;
        } else if (W - 1 - x == e) {
            s = w + y;
        } else if (H - 1 - y == e) {
            s = w + h + (w - x);
        } else {
            s = 2F * w + h + (h - y);
        }
        return s / total;
    }

    /** A cut violet gem in a gold setting: a diamond, lit at its top left, with a white point at its heart. */
    private static void gem(int cx, int cy, int r) {
        for (int dy = -r; dy <= r; dy++) {
            for (int dx = -r; dx <= r; dx++) {
                int d = Math.abs(dx) + Math.abs(dy);
                if (d > r) {
                    continue;
                }
                if (d == r) {
                    put(cx + dx, cy + dy, dx + dy < 0 ? 0xF8E19A : 0x8A5A1E, 1F, NONE);
                } else if (dx == 0 && dy == 0) {
                    put(cx, cy, 0xFFF4FF, 1F, GEM);
                } else {
                    put(cx + dx, cy + dy, dx + dy < 0 ? 0xD9A8FF : dx + dy == 0 ? 0x9B5BE0 : 0x5A1E9A, 1F, GEM);
                }
            }
        }
    }

    /** Wells of dark glass: shadowed at the top left, lit at the bottom right like vanilla's, the sky dimly through them. */
    private static void slots() {
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 9; col++) {
                well(8 + col * 18, 17 + row * 18, 18, 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            well(8 + col * 18, 111, 18, 18);
        }
    }

    private static void well(int x0, int y0, int w, int h) {
        for (int y = y0; y < y0 + h; y++) {
            for (int x = x0; x < x0 + w; x++) {
                boolean top = y == y0, left = x == x0, bottom = y == y0 + h - 1, right = x == x0 + w - 1;
                if ((top || left) && !(bottom && left) && !(top && right)) {
                    put(x, y, 0x05020C, 0.92F, NONE);
                } else if (bottom || right) {
                    put(x, y, 0x7A5CC0, 0.42F, (bottom && left) || (top && right) ? NONE : RIM);
                } else {
                    float inner = (x - x0 - 1 + y - y0 - 1) / (float) (w + h - 4);
                    put(x, y, 0x06030F, 0.62F - inner * 0.14F, NONE);
                }
            }
        }
    }

    /** A gold line between the grid and the hotbar, fading out at both ends, with a small gem in the middle. */
    private static void divider() {
        int y = 109, from = 10, to = 168;
        for (int x = from; x <= to; x++) {
            float u = (x - from) / (float) (to - from);
            float a = (float) Math.sin(u * Math.PI);
            put(x, y, 0xE0B458, 0.25F + 0.65F * a, NONE);
        }
        gem(88, 109, 2);
    }

    /** The scrollbar's channel: a deep well with a faint violet thread down it. */
    private static void track() {
        int x0 = 174, y0 = 17, w = 14, h = 114;
        well(x0, y0, w, h);
        for (int y = y0 + 2; y < y0 + h - 2; y++) {
            put(x0 + 6, y, 0x5A2C9C, 0.35F, NONE);
            put(x0 + 7, y, 0x5A2C9C, 0.35F, NONE);
        }
    }

    /** A gold thread under where the title is written, running out into nothing. */
    private static void title() {
        int y = 15, from = 7, to = 120;
        for (int x = from; x <= to; x++) {
            float u = (x - from) / (float) (to - from);
            put(x, y, 0xE0B458, 0.85F * (1F - u * u), NONE);
        }
        // A darker band behind the letters, so the sky never drowns them.
        for (int yy = 5; yy <= 14; yy++) {
            for (int x = 8; x <= 60; x++) {
                float u = Math.max(0F, (x - 40) / 20F);
                put(x, yy, 0x0A0418, 0.45F * (1F - u), NONE);
            }
        }
    }

    /** A crystal knob: a violet prism in gold, three grooves to grip it by. Unlit, it is grey and dull. */
    private static void knob(int[] px, int u, boolean dim) {
        for (int y = 0; y < 15; y++) {
            for (int x = 0; x < 12; x++) {
                int c;
                boolean edge = x == 0 || y == 0 || x == 11 || y == 14;
                boolean corner = (x == 0 || x == 11) && (y == 0 || y == 14);
                if (corner) {
                    c = 0;
                } else if (edge) {
                    c = 0xFF14081F;
                } else if (x == 1 || y == 1) {
                    c = 0xFFF8E19A;
                } else if (x == 10 || y == 13) {
                    c = 0xFF8A5A1E;
                } else if ((y == 5 || y == 7 || y == 9) && x >= 3 && x <= 8) {
                    c = 0xFF2A0E52;
                } else if ((y == 6 || y == 8 || y == 10) && x >= 3 && x <= 8) {
                    c = 0xFFE6C8FF;
                } else {
                    float v = (y - 2) / 10F, h = (x - 2) / 7F;
                    int r = (int) (190 - 110 * v - 20 * h), g = (int) (130 - 95 * v - 15 * h), b = (int) (255 - 95 * v - 10 * h);
                    c = 0xFF000000 | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
                }
                if (dim && c != 0) {
                    int r = (c >> 16) & 0xFF, g = (c >> 8) & 0xFF, b = c & 0xFF;
                    int grey = (r * 3 + g * 6 + b) / 10;
                    c = 0xFF000000 | (((grey + r) / 4) << 16) | (((grey + g) / 4) << 8) | ((grey + b) / 4);
                }
                px[y * SIZE + u + x] = c;
            }
        }
    }

    /**
     * The tab's own button, in place of vanilla's grey: a little slab of the same night, in the same gold.
     * Open, its near end is left open into the panel; closed, it is darker, and shut with gold.
     */
    private static void tab(int[] px, int u, boolean top, boolean open, Random rand) {
        int[] sprite = new int[28 * 32];
        for (int y = 0; y < 32; y++) {
            float v = y / 31F;
            for (int x = 0; x < 28; x++) {
                int c;
                boolean corner = y == 0 && (x == 0 || x == 27);
                if (corner) {
                    c = 0;
                } else if (x == 0 || x == 27 || y == 0 || y == 1 && (x == 1 || x == 26)) {
                    c = 0xFF14081F;
                } else if (!open && y == 27) {
                    c = 0xFF6E4216;
                } else if (!open && y >= 28) {
                    c = 0xFF14081F;
                } else if (x == 1 || y == 1) {
                    c = 0xFFF8E19A;
                } else if (x == 26) {
                    c = 0xFF9A6420;
                } else if (x == 2 || y == 2) {
                    c = 0xFFD6A548;
                } else if (x == 25) {
                    c = 0xFF6E4216;
                } else if (x == 3 || x == 24 || y == 3) {
                    c = 0xFF7C3FD0;
                } else {
                    float dim = open ? 1F : 0.62F;
                    float r = (0.17F - 0.12F * v) * dim, g = (0.06F - 0.04F * v) * dim, b = (0.34F - 0.22F * v) * dim;
                    c = 0xFF000000 | (channel(r) << 16) | (channel(g) << 8) | channel(b);
                }
                sprite[y * 28 + x] = c;
            }
        }
        for (int i = 0; i < 9; i++) {
            int x = 4 + rand.nextInt(20), y = 4 + rand.nextInt(open ? 26 : 22);
            int b = 120 + rand.nextInt(136);
            sprite[y * 28 + x] = 0xFF000000 | (b << 16) | (b << 8) | Math.min(255, b + 40);
        }
        for (int y = 0; y < 32; y++) {
            int sy = top ? y : 31 - y;
            System.arraycopy(sprite, sy * 28, px, (TAB_V + y) * SIZE + u, 28);
        }
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    /** Fractal value noise that tiles every 256 texels: octaves of smoothed random lattices, each finer and fainter. */
    private static float[] fbm(Random rand, int cell, int octaves) {
        float[] out = new float[256 * 256];
        float amp = 1F, total = 0F;
        for (int o = 0; o < octaves && cell >= 2; o++, cell /= 2, amp *= 0.5F) {
            int n = 256 / cell;
            float[] lattice = new float[n * n];
            for (int i = 0; i < lattice.length; i++) {
                lattice[i] = rand.nextFloat();
            }
            for (int y = 0; y < 256; y++) {
                for (int x = 0; x < 256; x++) {
                    int gx = x / cell, gy = y / cell;
                    float fx = (x % cell) / (float) cell, fy = (y % cell) / (float) cell;
                    fx = fx * fx * (3F - 2F * fx);
                    fy = fy * fy * (3F - 2F * fy);
                    float a = lattice[gy * n + gx], b = lattice[gy * n + (gx + 1) % n];
                    float c = lattice[((gy + 1) % n) * n + gx], d = lattice[((gy + 1) % n) * n + (gx + 1) % n];
                    out[y * 256 + x] += amp * ((a * (1F - fx) + b * fx) * (1F - fy) + (c * (1F - fx) + d * fx) * fy);
                }
            }
            total += amp;
        }
        for (int i = 0; i < out.length; i++) {
            out[i] /= total;
        }
        return out;
    }

    private static final class Star {
        final int x, y;
        final float r, g, b, base;
        final double speed, phase;
        final boolean big;

        Star(Random rand) {
            x = rand.nextInt(W);
            y = rand.nextInt(H);
            big = rand.nextInt(9) == 0;
            base = big ? 0.9F + rand.nextFloat() * 0.3F : 0.25F + rand.nextFloat() * 0.55F;
            speed = 0.6D + rand.nextDouble() * 2.4D;
            phase = rand.nextDouble() * Math.PI * 2D;
            int tint = rand.nextInt(6);
            // Mostly white and blue-white; now and then gold, or a rose.
            r = tint == 4 ? 1F : tint == 5 ? 1F : tint < 2 ? 0.75F : 0.95F;
            g = tint == 4 ? 0.85F : tint == 5 ? 0.65F : tint < 2 ? 0.85F : 0.95F;
            b = tint == 4 ? 0.50F : tint == 5 ? 0.85F : 1F;
        }
    }
}
