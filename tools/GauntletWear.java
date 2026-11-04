import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Paints the Infinity Gauntlet worn by snapping, from Lucraft's own textures, in four stages, each keeping the last's:
 * <ol>
 * <li><b>Scorched</b>: soot round the knuckles and fingertips, the gold tempered bronze at the edges of it.</li>
 * <li><b>Charred</b>: half the metal blackened, the rest dulled and tempered, the first cracks.</li>
 * <li><b>Cracked</b>: almost all of it black, split by cracks still molten at their hearts, a fingertip broken off.</li>
 * <li><b>Fused</b>: the metal melted: the fingers run together and sag, molten seams through it, and burnt flesh
 * showing where it has run into the hand inside it.</li>
 * </ol>
 * Writes items/gauntlet_worn_N.png, the icon, and models/gauntlet_worn_N.png, the 3D model's texture.
 * <p>
 * Usage: javac -d out tools/GauntletWear.java, then
 * java -cp out GauntletWear libs/LucraftCore-deobf.jar src/main/resources/assets/eternity/textures
 */
public class GauntletWear {

    private static final int SOOT = 0x221a14, CRACK = 0x140c08, MOLTEN = 0xff7a1c, MOLTEN_CORE = 0xffd27a, FLESH = 0x4a1e16, TEMPER = 0xa8582a;
    /** How much of the metal each stage has blackened. */
    private static final float[] COVER = {0F, 0.18F, 0.4F, 0.62F, 0.8F};

    public static void main(String[] args) throws Exception {
        File out = new File(args[1]);
        new File(out, "items").mkdirs();
        new File(out, "models").mkdirs();
        try (ZipFile jar = new ZipFile(args[0])) {
            BufferedImage icon = read(jar, "assets/lucraftcore/textures/items/infinity_gauntlet.png");
            BufferedImage model = read(jar, "assets/lucraftcore/textures/models/infinity_gauntlet.png");
            for (int stage = 1; stage <= 4; stage++) {
                ImageIO.write(wear(icon, stage, true), "png", new File(out, "items/gauntlet_worn_" + stage + ".png"));
                ImageIO.write(wear(model, stage, false), "png", new File(out, "models/gauntlet_worn_" + stage + ".png"));
            }
        }
    }

    private static BufferedImage read(ZipFile jar, String path) throws Exception {
        ZipEntry entry = jar.getEntry(path);
        try (InputStream in = jar.getInputStream(entry)) {
            BufferedImage image = ImageIO.read(in);
            BufferedImage argb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
            argb.getGraphics().drawImage(image, 0, 0, null);
            return argb;
        }
    }

    private static BufferedImage wear(BufferedImage source, int stage, boolean icon) {
        int w = source.getWidth(), h = source.getHeight();
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                image.setRGB(x, y, source.getRGB(x, y));
            }
        }
        // The same wear at every stage, only more of it: each stage keeps the last's.
        long seed = icon ? 0x5EEDL : 0xF00DL;
        if (stage >= 4 && icon) {
            melt(image, seed);
        }
        float[][] field = field(w, h, seed, icon ? 3F : 5F);
        float cover = COVER[stage];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = image.getRGB(x, y);
                if ((argb >>> 24) < 16) {
                    continue;
                }
                int rgb = argb & 0xFFFFFF;
                // Burnt in its own shading, so its edges, knuckles and highlights still show through the black.
                float light = luminance(rgb);
                float n = field[x][y];
                // The gold dulls with every stage.
                rgb = mix(rgb, darken(rgb, 0.7F), stage * 0.1F);
                // Where the heat reached, the gold tempered to bronze; where it stayed, it blackened.
                float temper = 1F - smooth(clamp((n - cover) / 0.16F));
                float burnt = 1F - smooth(clamp((n - cover + 0.1F) / 0.12F));
                rgb = mix(rgb, mix(TEMPER, rgb, light * 0.5F), temper * 0.6F);
                int charred = mix(SOOT, 0x6a523c, (float) Math.pow(light, 1.6D));
                rgb = mix(rgb, charred, burnt);
                image.setRGB(x, y, argb & 0xFF000000 | rgb);
            }
        }
        if (stage >= 2) {
            cracks(image, seed + 1, stage == 2 ? 2 : stage == 3 ? 4 : 6, stage >= 3);
        }
        if (stage >= 3 && icon) {
            // A fingertip broken off: the topmost texel of a finger, gone.
            chip(image);
        }
        return image;
    }

    /** Smooth noise, 0 to 1, at cells of {@code scale} texels. */
    private static float[][] field(int w, int h, long seed, float scale) {
        Random random = new Random(seed);
        int gw = (int) (w / scale) + 2, gh = (int) (h / scale) + 2;
        float[][] grid = new float[gw][gh];
        for (int i = 0; i < gw; i++) {
            for (int j = 0; j < gh; j++) {
                grid[i][j] = random.nextFloat();
            }
        }
        float[][] field = new float[w][h];
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                float gx = x / scale, gy = y / scale;
                int ix = (int) gx, iy = (int) gy;
                float fx = smooth(gx - ix), fy = smooth(gy - iy);
                float top = grid[ix][iy] + (grid[ix + 1][iy] - grid[ix][iy]) * fx;
                float bottom = grid[ix][iy + 1] + (grid[ix + 1][iy + 1] - grid[ix][iy + 1]) * fx;
                // A little grain on top, so the soot is not smooth.
                field[x][y] = Math.max(0F, Math.min(1F, top + (bottom - top) * fy + (random.nextFloat() - 0.5F) * 0.18F));
            }
        }
        return field;
    }

    /** Cracks walked across the metal; past the third stage, molten at their hearts. */
    private static void cracks(BufferedImage image, long seed, int count, boolean molten) {
        Random random = new Random(seed);
        int w = image.getWidth(), h = image.getHeight();
        for (int i = 0; i < count; i++) {
            int x = random.nextInt(w), y = random.nextInt(h);
            int dx = random.nextBoolean() ? 1 : -1, dy = random.nextBoolean() ? 1 : -1;
            int length = 3 + random.nextInt(Math.max(2, Math.min(w, h) / 2));
            for (int step = 0; step < length; step++) {
                if (x < 0 || y < 0 || x >= w || y >= h || (image.getRGB(x, y) >>> 24) < 16) {
                    break;
                }
                int alpha = image.getRGB(x, y) & 0xFF000000;
                boolean core = molten && step > 0 && step < length - 1 && random.nextFloat() < 0.6F;
                image.setRGB(x, y, alpha | (core ? (random.nextFloat() < 0.35F ? MOLTEN_CORE : MOLTEN) : CRACK));
                if (random.nextBoolean()) {
                    x += dx;
                } else {
                    y += dy;
                }
                if (random.nextFloat() < 0.2F) {
                    dx = -dx;
                }
            }
        }
    }

    /** The topmost texel of one finger broken away. */
    private static void chip(BufferedImage image) {
        int w = image.getWidth();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = w - 1; x >= 0; x--) {
                if ((image.getRGB(x, y) >>> 24) >= 16) {
                    image.setRGB(x, y, 0);
                    return;
                }
            }
        }
    }

    /**
     * The icon melting: the gaps between the fingers fill with burnt flesh where the metal has run into the hand, and
     * the fingers sag, each run down a texel or two.
     */
    private static void melt(BufferedImage image, long seed) {
        Random random = new Random(seed + 7);
        int w = image.getWidth(), h = image.getHeight();
        // The fingers run together: a gap with metal on both sides of it is filled.
        for (int y = 0; y < h; y++) {
            for (int x = 1; x < w - 1; x++) {
                if ((image.getRGB(x, y) >>> 24) < 16 && (image.getRGB(x - 1, y) >>> 24) >= 16 && (image.getRGB(x + 1, y) >>> 24) >= 16) {
                    image.setRGB(x, y, 0xFF000000 | (random.nextFloat() < 0.3F ? MOLTEN : FLESH));
                }
            }
        }
        // And sag: every column above the palm slides down by a texel or two, the drips trailing below.
        int[] copy = image.getRGB(0, 0, w, h, null, 0, w);
        for (int x = 0; x < w; x++) {
            int sag = random.nextInt(3);
            if (sag == 0) {
                continue;
            }
            for (int y = h - 1; y >= 0; y--) {
                int from = y - sag;
                int pixel = from >= 0 ? copy[from * w + x] : 0;
                if (y < h / 2 || (pixel >>> 24) >= 16) {
                    image.setRGB(x, y, pixel);
                }
            }
        }
    }

    private static float luminance(int rgb) {
        return (0.3F * ((rgb >> 16) & 255) + 0.59F * ((rgb >> 8) & 255) + 0.11F * (rgb & 255)) / 255F;
    }

    private static float clamp(float t) {
        return Math.max(0F, Math.min(1F, t));
    }

    private static float smooth(float t) {
        return t * t * (3F - 2F * t);
    }

    private static int mix(int a, int b, float t) {
        t = Math.max(0F, Math.min(1F, t));
        int r = (int) (((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
        int g = (int) (((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
        int bl = (int) ((a & 255) + ((b & 255) - (a & 255)) * t);
        return r << 16 | g << 8 | bl;
    }

    private static int darken(int rgb, float f) {
        return mix(rgb, 0, 1F - f);
    }
}
