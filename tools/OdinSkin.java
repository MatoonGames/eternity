import com.matoon.eternity.client.render.OdinBoxes;
import com.matoon.eternity.client.render.VaultBoxes.Box;
import com.matoon.eternity.client.render.VaultBoxes.Sheet;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Paints Odin and Sleipnir onto the sheet {@link OdinBoxes} lays out, and what of them lights up from within:
 * <ul>
 * <li><b>odin.png</b>. Sleipnir: a dark dappled coat, a pale silver mane and tail, a gold-shod hoof on each of his eight
 * legs, gold barding engraved with knotwork, a crimson caparison hemmed and knotted in gold, a gold bridle. Odin: an
 * old king's face, the patch over his right eye, white brows and a long white beard, a gold helm with a crest and great
 * horns, a gold cuirass with the circles of Asgard on it, dark scale under layered gold plate, a crimson cape trimmed
 * in gold, dark breeches and boots. Gungnir: a gold shaft cut with runes, a pale gold blade.</li>
 * <li><b>odin_glow.png</b>: his one eye, Sleipnir's, the runes on Gungnir and the edge of its blade. Drawn additively,
 * at full brightness.</li>
 * </ul>
 * In every front face the left of the picture is his right side, as when you face him.
 * <p>
 * Usage: javac -d out tools/OdinSkin.java src/main/java/com/matoon/eternity/client/render/VaultBoxes.java
 * src/main/java/com/matoon/eternity/client/render/OdinBoxes.java
 * then java -cp out OdinSkin src/main/resources/assets/eternity/textures/entity
 */
public class OdinSkin {

    private static final int TOP = 0, BOTTOM = 1, RIGHT = 2, FRONT = 3, LEFT = 4, BACK = 5;

    // ------------------------------------------------------------------ palettes, dark to light
    private static final int[] COAT = {0x0b0c0f, 0x121418, 0x1a1d22, 0x23272d, 0x2e333a, 0x3c424a, 0x525a64};
    private static final int[] SILVER = {0x4a4d56, 0x6a6e78, 0x8c909a, 0xadb1ba, 0xc9ccd4, 0xe0e2e8, 0xf4f5f8};
    private static final int[] GOLD = {0x3a2507, 0x5a3c0d, 0x82591a, 0xa97d28, 0xcda243, 0xe8c670, 0xfbe8b4};
    private static final int[] POLISH = {0x4a300a, 0x74500f, 0x9c7019, 0xc39428, 0xe2b844, 0xf6d878, 0xfff3c8};
    private static final int[] CRIMSON = {0x200305, 0x34060a, 0x4c0a0f, 0x661016, 0x82191e, 0x9e2628, 0xbc3a36};
    private static final int[] SKIN = {0x5a3624, 0x7a4a32, 0x986044, 0xb27a58, 0xc8946e, 0xdaac88, 0xe8c4a4};
    private static final int[] WHITE = {0x6e6c6a, 0x8e8c88, 0xaeaca6, 0xc8c6c0, 0xdcdad4, 0xecebe6, 0xfbfaf6};
    private static final int[] SCALE = {0x0c0b0a, 0x161412, 0x221e19, 0x2f2921, 0x3e352a, 0x524634, 0x6c5c42};
    private static final int[] LEATHER = {0x120a06, 0x1c110a, 0x28180e, 0x342013, 0x432a19, 0x553621, 0x6c472c};
    private static final int[] HORNHOOF = {0x0d0c0b, 0x171513, 0x221f1c, 0x2e2a26, 0x3c3732, 0x4d4740, 0x645c53};

    private static BufferedImage skin, glow;

    public static void main(String[] args) throws Exception {
        File out = new File(args[0]);
        out.mkdirs();
        Sheet s = OdinBoxes.SHEET;
        skin = new BufferedImage(s.width, s.height, BufferedImage.TYPE_INT_ARGB);
        glow = new BufferedImage(s.width, s.height, BufferedImage.TYPE_INT_ARGB);
        for (Box b : s.all) {
            box(b);
        }
        ImageIO.write(skin, "png", new File(out, "odin.png"));
        ImageIO.write(glow, "png", new File(out, "odin_glow.png"));
    }

    // ------------------------------------------------------------------ laying out a box

    private static void box(Box b) {
        int u = b.u, v = b.v, w = b.w, h = b.h, d = b.d;
        face(b, TOP, u + d, v, w, d);
        face(b, BOTTOM, u + d + w, v, w, d);
        face(b, RIGHT, u, v + d, d, h);
        face(b, FRONT, u + d, v + d, w, h);
        face(b, LEFT, u + d + w, v + d, d, h);
        face(b, BACK, u + d + w + d, v + d, w, h);
    }

    private static void face(Box b, int face, int x0, int y0, int w, int h) {
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                Px p = new Px(b, face, x, y, w, h);
                skin.setRGB(x0 + x, y0 + y, 0xFF000000 | paint(p));
                int g = glowOf(p);
                if (g != 0) {
                    glow.setRGB(x0 + x, y0 + y, g);
                }
            }
        }
    }

    /** One pixel of one face of one box, and where its centre lies in the part's own space. */
    private static final class Px {
        final Box b;
        final int face, x, y, w, h;
        final double mx, my, mz;

        Px(Box b, int face, int x, int y, int w, int h) {
            this.b = b;
            this.face = face;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            switch (face) {
                case FRONT:
                    mx = b.x + x + 0.5D;
                    my = b.y + y + 0.5D;
                    mz = b.z;
                    break;
                case BACK:
                    mx = b.x + (w - 1 - x) + 0.5D;
                    my = b.y + y + 0.5D;
                    mz = b.z + b.d;
                    break;
                case LEFT:
                    mx = b.x + b.w;
                    my = b.y + y + 0.5D;
                    mz = b.z + x + 0.5D;
                    break;
                case RIGHT:
                    mx = b.x;
                    my = b.y + y + 0.5D;
                    mz = b.z + (w - 1 - x) + 0.5D;
                    break;
                case TOP:
                    mx = b.x + x + 0.5D;
                    my = b.y;
                    mz = b.z + (h - 1 - y) + 0.5D;
                    break;
                default:
                    mx = b.x + x + 0.5D;
                    my = b.y + b.h;
                    mz = b.z + y + 0.5D;
                    break;
            }
        }

        boolean side() {
            return face == LEFT || face == RIGHT;
        }

        boolean cap() {
            return face == TOP || face == BOTTOM;
        }

        boolean part(String name) {
            return b.part.startsWith(name);
        }

        boolean edge() {
            return x == 0 || y == 0 || x == w - 1 || y == h - 1;
        }

        /** Light from above: tops brightest, bottoms darkest, the backs a little shaded. */
        int light() {
            switch (face) {
                case TOP:
                    return 1;
                case BOTTOM:
                    return -2;
                case BACK:
                    return -1;
                default:
                    return 0;
            }
        }
    }

    // ------------------------------------------------------------------ what each thing is made of

    private static int paint(Px p) {
        switch (p.b.material) {
            case OdinBoxes.HIDE:
                return hide(p);
            case OdinBoxes.MANE:
                return strands(SILVER, 4, p);
            case OdinBoxes.MUZZLE:
                return muzzle(p);
            case OdinBoxes.HOOF:
                return hoof(p);
            case OdinBoxes.CAPARISON:
                return caparison(p);
            case OdinBoxes.BARD:
                return engraved(GOLD, 4, p);
            case OdinBoxes.SADDLE:
                return saddle(p);
            case OdinBoxes.BRIGHT:
                return polished(p);
            case OdinBoxes.HORN:
                return horn(p);
            case OdinBoxes.FACE:
                return face(p);
            case OdinBoxes.BEARD:
                return beard(p);
            case OdinBoxes.HAIR:
                return strands(WHITE, 4, p);
            case OdinBoxes.HELM:
                return helm(p);
            case OdinBoxes.MAIL:
                return scale(p);
            case OdinBoxes.CUIRASS:
                return cuirass(p);
            case OdinBoxes.PLATE:
                return engraved(GOLD, 4, p);
            case OdinBoxes.CAPE:
                return cape(p);
            case OdinBoxes.CLOTH:
                return grain(LEATHER, 2 + p.light(), p, 0.25D);
            case OdinBoxes.BOOT:
                return boot(p);
            case OdinBoxes.GLOVE:
                return glove(p);
            case OdinBoxes.SHAFT:
                return shaft(p);
            default:
                return blade(p);
        }
    }

    private static int glowOf(Px p) {
        int m = p.b.material;
        if (m == OdinBoxes.FACE && p.face == FRONT && p.y == 4 && p.x == 5) {
            // His one eye, pale and cold.
            return 0xFFCFEAFF;
        }
        if (m == OdinBoxes.FACE && p.face == FRONT && p.y == 4 && p.x == 6) {
            return 0x707A9CB8;
        }
        if (m == OdinBoxes.HIDE && p.part("horseHead") && p.side() && eye(p)) {
            return 0xB0A8C0FF;
        }
        if (m == OdinBoxes.SHAFT && !p.cap() && rune(p)) {
            return 0xD0FFD890;
        }
        if (m == OdinBoxes.BLADE && !p.cap()) {
            boolean edge = p.face == FRONT || p.face == BACK ? p.x == 0 || p.x == p.w - 1 : true;
            if (edge) {
                return 0xA0FFF4D0;
            }
            if (rune(p)) {
                return 0xE0FFE8A0;
            }
        }
        return 0;
    }

    // ------------------------------------------------------------------ Sleipnir

    /** His coat: near black, dappled paler over the barrel and the quarters, shaded under, muscle catching the light. */
    private static int hide(Px p) {
        int level = 3 + p.light();
        double n = noise(p.mx * 0.55D + p.mz * 0.13D, p.my * 0.55D + p.mz * 0.41D, 7);
        if (p.part("barrel") || p.part("up") || p.part("neck")) {
            // Dapples: rings of paler hair.
            double ring = noise(p.mx * 0.9D + 31D, p.mz * 0.9D + p.my * 0.7D, 11);
            if (ring > 0.62D && ring < 0.72D && p.face != BOTTOM) {
                level++;
            }
        }
        if (p.part("low") || p.part("hoof")) {
            level--;
        }
        if (p.part("horseHead")) {
            if (p.side() && eye(p)) {
                return p.mz > -6.6D ? 0x05060a : 0x9aa4c4;
            }
            // The bridle: a gold strap behind the eye, and the brow band.
            if (p.side() && p.mz > -3.7D && p.mz < -2.8D || p.face == TOP && p.mz > -3.7D && p.mz < -2.8D) {
                return POLISH[p.my < -2D ? 4 : 3];
            }
        }
        if (p.part("ear") && (p.face == FRONT)) {
            level -= 2;
        }
        return dither(COAT, level + (n > 0.68D ? 1 : n < 0.25D ? -1 : 0), p);
    }

    private static boolean eye(Px p) {
        return Math.abs(p.mz + 6.5D) < 0.7D && Math.abs(p.my + 2.5D) < 0.6D;
    }

    private static int muzzle(Px p) {
        if (p.face == FRONT) {
            // Nostrils, wide; the mouth below them.
            if (p.y == 1 && (p.x == 0 || p.x == 3)) {
                return 0x040404;
            }
            if (p.y == 3) {
                return COAT[0];
            }
        }
        // The noseband of the bridle round the muzzle.
        if (!p.cap() && p.face != FRONT && p.mz > -11.4D && p.mz < -10.4D) {
            return POLISH[3 + p.light()];
        }
        return dither(COAT, 2 + p.light(), p);
    }

    /** A dark hoof shod in gold, a rune cut in the shoe. */
    private static int hoof(Px p) {
        if (p.face == BOTTOM) {
            return HORNHOOF[1];
        }
        if (p.face == TOP) {
            return SILVER[2];
        }
        return p.y == 1 ? POLISH[(p.x % 2 == 0) ? 4 : 3] : HORNHOOF[3 + (p.x == 1 ? 1 : 0)];
    }

    /** Crimson cloth over his back: falling in folds, hemmed in gold, a gold knot worked on each flank. */
    private static int caparison(Px p) {
        if (p.face == TOP) {
            // A gold band along his spine, and the edges.
            if (Math.abs(p.mx) < 0.6D || p.x == 0 || p.x == p.w - 1) {
                return POLISH[3];
            }
            return dither(CRIMSON, 4 + (fold(p.mz) > 0.5D ? 1 : 0), p);
        }
        if (p.side()) {
            if (p.y == p.h - 1 || p.y == 0) {
                return POLISH[p.y == 0 ? 4 : 3];
            }
            // Odin's knot: three interlaced triangles, worked in gold on each flank.
            if (valknut(p.mz + 11.5D, p.my - 0.8D)) {
                return POLISH[4];
            }
            int level = 3 + (fold(p.mz) > 0.6D ? 1 : fold(p.mz) < 0.25D ? -1 : 0) - (p.y > p.h - 3 ? 1 : 0);
            return dither(CRIMSON, level, p);
        }
        return dither(CRIMSON, 2, p);
    }

    /** A knot of three triangles, pointing up and down by turns, about 5 pixels across, centred on (0, 0). */
    private static boolean valknut(double x, double y) {
        for (int i = 0; i < 3; i++) {
            double cx = (i - 1) * 1.6D;
            double cy = i == 1 ? -0.6D : 0.5D;
            boolean up = i != 1;
            double dx = x - cx, dy = (y - cy) * (up ? 1D : -1D);
            // The outline of a triangle of height 3: |dx| = (dy + 1.5) / 2, between dy -1.5 and 1.5; and its base.
            if (dy > -1.6D && dy < 1.6D) {
                double half = (dy + 1.5D) * 0.62D;
                if (Math.abs(Math.abs(dx) - half) < 0.45D) {
                    return true;
                }
                if (dy > 1.0D && Math.abs(dx) < half) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int saddle(Px p) {
        if (!p.cap() && (p.x + p.y) % 3 == 0 && p.y == 1) {
            return POLISH[5];
        }
        return grain(CRIMSON, 1 + p.light(), p, 0.3D);
    }

    // ------------------------------------------------------------------ gold

    /** Gold plate, engraved: an interlace of fine dark lines over it, the edges catching the light. */
    private static int engraved(int[] ramp, int level, Px p) {
        level += p.light();
        if (p.edge() && !p.cap()) {
            return ramp[clamp(level + 2)];
        }
        double a = p.mx + p.my * 0.9D + p.mz * 0.7D, b = p.mx - p.my * 0.9D + p.mz * 0.6D;
        boolean line = Math.abs(((a % 3D) + 3D) % 3D - 1.5D) < 0.35D || Math.abs(((b % 3D) + 3D) % 3D - 1.5D) < 0.35D;
        if (line && !p.cap()) {
            level -= 2;
        }
        return dither(ramp, level, p);
    }

    /** Bright, polished gold: rings and trim, a highlight running along them. */
    private static int polished(Px p) {
        int level = 4 + p.light();
        if (!p.cap() && (p.x + p.y) % 4 == 0) {
            level += 2;
        }
        return dither(POLISH, level, p);
    }

    /** The helm's horns: gold, ringed, brightening to the points. */
    private static int horn(Px p) {
        int seg = p.b.part.charAt(4) - '0';
        int level = 3 + seg / 2 + p.light();
        if (!p.cap() && p.y % 2 == 1) {
            level -= 2;
        }
        return dither(POLISH, level, p);
    }

    private static int helm(Px p) {
        if (p.part("cheek")) {
            return engraved(GOLD, 4, p);
        }
        // The crown: gold, a band of knotwork round it just above the brow.
        if (!p.cap() && p.y >= p.h - 2) {
            return engraved(POLISH, 3, p);
        }
        if (p.face == TOP) {
            double r = Math.sqrt(p.mx * p.mx + p.mz * p.mz);
            return dither(POLISH, 4 + (r < 2D ? 1 : 0) - (Math.abs(r - 3D) < 0.4D ? 2 : 0), p);
        }
        return dither(GOLD, 4 + p.light() + (p.y == 0 ? 1 : 0), p);
    }

    /** The cuirass: gold, and on the breastplate the circles of Asgard, ring inside ring. */
    private static int cuirass(Px p) {
        if (p.face == FRONT) {
            double dx = p.mx, dy = p.my + 4D;
            double r = Math.sqrt(dx * dx + dy * dy);
            if (Math.abs(r - 2.2D) < 0.5D || r < 0.7D) {
                return POLISH[r < 0.7D ? 6 : 5];
            }
            if (Math.abs(r - 3.6D) < 0.45D) {
                return GOLD[2];
            }
        }
        return engraved(GOLD, 4, p);
    }

    // ------------------------------------------------------------------ Odin

    /**
     * His face: weathered, white-browed; the patch over his right eye, its strap up over his brow; his left eye pale.
     * s/S/d skin, b brow, P the patch, p its rim, w the white of his eye, i its pale iris, n his nose, N its shadow.
     */
    private static final String[] FACE_ART = {
            "dsSSSSsd",
            "dsSssSsd",
            "PSssssSd",
            "pbPdsbbd",
            "dPPssiwd",
            "dpssnNsd",
            "dssnNssd",
            "ddssssdd"};

    private static int face(Px p) {
        if (p.face == FRONT) {
            char c = FACE_ART[p.y].charAt(p.x);
            switch (c) {
                case 'S':
                    return SKIN[5];
                case 'd':
                    return SKIN[2];
                case 'b':
                    return WHITE[6];
                case 'P':
                    return 0x0e0c0a;
                case 'p':
                    return POLISH[3];
                case 'w':
                    return 0xe8ecf2;
                case 'i':
                    return 0xa8d4ff;
                case 'n':
                    return SKIN[5];
                case 'N':
                    return SKIN[2];
                default:
                    return SKIN[4];
            }
        }
        if (p.side()) {
            // Skin at the front of the head, white hair behind the ear.
            boolean front = p.mz < -1.5D;
            if (!front) {
                return strands(WHITE, 4, p);
            }
            if (p.face == RIGHT && p.y == 3) {
                return 0x0e0c0a;
            }
            return dither(SKIN, 3 + (p.y > 5 ? -1 : 0), p);
        }
        if (p.face == BACK) {
            return strands(WHITE, 4, p);
        }
        return p.face == TOP ? WHITE[5] : SKIN[2];
    }

    /** His beard, long and white; the moustache across the top of it, the mouth in it. */
    private static int beard(Px p) {
        if (p.part("head") && p.face == FRONT) {
            if (p.y == 0) {
                return WHITE[(p.x == 3 || p.x == 4) ? 4 : 6];
            }
            if (p.y == 1 && (p.x == 3 || p.x == 4)) {
                return 0x2a1c16;
            }
        }
        return strands(WHITE, 5, p);
    }

    /** Dark scale, each scale's edge catching a little light. */
    private static int scale(Px p) {
        if (p.cap()) {
            return SCALE[2];
        }
        int row = (int) Math.floor(p.my + 8D);
        double off = row % 2 == 0 ? 0D : 1D;
        double across = ((p.mx + p.mz + off) % 2D + 2D) % 2D;
        boolean rim = across < 0.5D || ((p.my % 1D) + 1D) % 1D > 0.8D;
        return SCALE[clamp(3 + p.light() + (rim ? 2 : 0))];
    }

    /** The cape: crimson, falling in folds, the inside darker, a gold border down its edges and along its hem. */
    private static int cape(Px p) {
        boolean last = p.b.part.equals("cape" + (OdinBoxes.CAPE_SEGMENTS - 1));
        if (p.side() || (p.face == FRONT || p.face == BACK) && (p.x == 0 || p.x == p.w - 1)) {
            return POLISH[3];
        }
        if (last && !p.cap() && p.y >= p.h - 2) {
            return POLISH[p.y == p.h - 1 ? 3 : 4];
        }
        if (p.b.part.equals("chest")) {
            // The mantle across the shoulders.
            return dither(CRIMSON, 4 + p.light(), p);
        }
        int level = (p.face == FRONT ? 2 : 4) + (fold(p.mx * 1.3D) > 0.6D ? 1 : fold(p.mx * 1.3D) < 0.3D ? -1 : 0);
        return dither(CRIMSON, level, p);
    }

    private static int boot(Px p) {
        if (p.face == FRONT && p.y >= p.h - 2 && p.b.h == 2) {
            return POLISH[3];
        }
        return grain(LEATHER, 2 + p.light(), p, 0.3D);
    }

    private static int glove(Px p) {
        if (p.face == FRONT && p.y == 0) {
            return POLISH[4];
        }
        return grain(LEATHER, 2 + p.light(), p, 0.3D);
    }

    /** Gungnir's shaft: gold, cut along its length with runes. */
    private static int shaft(Px p) {
        if (p.cap()) {
            return POLISH[3];
        }
        if (rune(p)) {
            return GOLD[1];
        }
        return POLISH[4 + (p.face == FRONT || p.face == LEFT ? 1 : 0)];
    }

    /** Its blade: pale gold, the ridge brightest, runes down the middle. */
    private static int blade(Px p) {
        if (p.cap()) {
            return POLISH[5];
        }
        if (rune(p)) {
            return GOLD[3];
        }
        boolean edge = (p.face == FRONT || p.face == BACK) && (p.x == 0 || p.x == p.w - 1);
        return POLISH[edge ? 6 : 5];
    }

    /** Whether a pixel down a shaft or blade is cut with a rune: short strokes in a column, a few to a hand's length. */
    private static boolean rune(Px p) {
        int row = (int) Math.floor(p.my);
        int r = Math.floorMod(row, 7);
        if (r == 6) {
            return false;
        }
        long h = (long) row * 2654435761L;
        return ((h >> (r + p.face)) & 1L) == 1L && r != 0;
    }

    // ------------------------------------------------------------------ textures

    /** Hair: strands running down its length. */
    private static int strands(int[] ramp, int level, Px p) {
        double n = noise((p.side() ? p.mz : p.mx) * 1.7D, p.my * 0.25D, 3);
        level += p.light() + (n > 0.65D ? 1 : n < 0.3D ? -1 : 0);
        return ramp[clamp(level)];
    }

    private static int grain(int[] ramp, int level, Px p, double amount) {
        double n = noise(p.mx * 0.8D + p.mz * 0.3D, p.my * 0.8D + p.mz * 0.5D, 5);
        return ramp[clamp(level + (n > 1D - amount ? 1 : n < amount ? -1 : 0))];
    }

    private static double fold(double at) {
        return 0.5D + 0.5D * Math.sin(at * 1.1D + Math.sin(at * 0.37D) * 2D);
    }

    private static int dither(int[] ramp, int level, Px p) {
        double n = noise(p.mx * 2.3D + p.mz * 1.7D, p.my * 2.3D + p.mz * 0.9D, 13);
        return ramp[clamp(level + (n > 0.82D ? 1 : n < 0.12D ? -1 : 0))];
    }

    private static int clamp(int level) {
        return Math.max(0, Math.min(6, level));
    }

    /** Smooth value noise, 0 to 1. */
    private static double noise(double x, double y, int seed) {
        int x0 = (int) Math.floor(x), y0 = (int) Math.floor(y);
        double fx = x - x0, fy = y - y0;
        fx = fx * fx * (3D - 2D * fx);
        fy = fy * fy * (3D - 2D * fy);
        double a = hash(x0, y0, seed), b = hash(x0 + 1, y0, seed), c = hash(x0, y0 + 1, seed), d = hash(x0 + 1, y0 + 1, seed);
        double top = a + (b - a) * fx, bottom = c + (d - c) * fx;
        return top + (bottom - top) * fy;
    }

    private static double hash(int x, int y, int seed) {
        int h = x * 374761393 + y * 668265263 + seed * 1442695041;
        h = (h ^ h >>> 13) * 1274126177;
        return ((h ^ h >>> 16) & 0xFFFF) / 65535D;
    }
}
