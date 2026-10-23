import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Builds the Relic Tracker textures from the vanilla compass frames: the iron case is
 * recoloured gold, the face goes void-dark and the red needle turns cosmic violet.
 *
 * Usage: java GoldCompass <vanilla textures/items dir> <output dir>
 */
public class GoldCompass {

    /** Hand-picked gold, dark to bright. */
    private static final int[] GOLD = {0x5A3A08, 0x7E540C, 0xA87412, 0xD49A1C, 0xEDBF35, 0xF8DC6A, 0xFFF4B0};

    public static void main(String[] args) throws Exception {
        File in = new File(args[0]);
        File out = new File(args[1]);
        out.mkdirs();
        int[] ramp = GOLD;
        for (int i = 0; i < 32; i++) {
            String name = String.format("compass_%02d.png", i);
            BufferedImage src = ImageIO.read(new File(in, name));
            BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < src.getHeight(); y++) {
                for (int x = 0; x < src.getWidth(); x++) {
                    dst.setRGB(x, y, recolour(src.getRGB(x, y), ramp));
                }
            }
            ImageIO.write(dst, "png", new File(out, String.format("relic_tracker_%02d.png", i)));
        }
    }

    private static int recolour(int argb, int[] ramp) {
        int a = argb >>> 24;
        if (a == 0) {
            return argb;
        }
        int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
        int max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        float lum = (0.299F * r + 0.587F * g + 0.114F * b) / 255F;
        if (r > 120 && r - Math.max(g, b) > 60) {
            // The needle's red tip becomes the violet of open space.
            float k = r / 255F;
            return (a << 24) | (clamp(150 * k + 40) << 16) | (clamp(40 * k) << 8) | clamp(255 * k);
        }
        if (max - min < 40) {
            if (lum < 0.24F) {
                // The dark face becomes a window onto the void.
                float k = lum / 0.24F;
                return (a << 24) | (clamp(14 + 30 * k) << 16) | (clamp(6 + 14 * k) << 8) | clamp(30 + 46 * k);
            }
            float t = (lum - 0.24F) / 0.76F;
            int index = Math.min(ramp.length - 1, Math.max(0, Math.round(t * (ramp.length - 1))));
            return (a << 24) | (ramp[index] & 0xFFFFFF);
        }
        return argb;
    }

    private static int clamp(float v) {
        return Math.max(0, Math.min(255, Math.round(v)));
    }
}
