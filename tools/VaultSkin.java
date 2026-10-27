import com.matoon.eternity.client.render.VaultBoxes;
import com.matoon.eternity.client.render.VaultBoxes.Box;
import com.matoon.eternity.client.render.VaultBoxes.Sheet;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Paints everything in Odin's Vault that is not a block, onto the sheets {@link VaultBoxes} lays out:
 * <ul>
 * <li><b>einherjar.png</b>: blackened steel plate edged and chevroned in gold over mail, dark leather,
 * a mustard wool cloak with a woven border, a spear of dark wood bound in gold with a gilt blade, and
 * a bronze shield embossed in rings with a band of knotwork. His helm shows nothing of his face: a
 * T of black between steel cheek plates.</li>
 * <li><b>destroyer.png</b> and <b>destroyer_glow.png</b>: banded gunmetal, every band catching the light
 * along its top edge and falling into shadow at its seam; spikes bright at the tips; a polished,
 * engraved crown; a face of slats over a furnace. Lit from within: the slits of its face and the
 * furnace behind them.</li>
 * <li><b>vault_relics.png</b> and <b>vault_relics_glow.png</b>: the treasures and their stands. Black
 * and carved stone, fluted columns with a band of runes, the frosted pedestal, aged bronze, engraved
 * gilt, the Tablet's carved rings, the milky orb and its iris, Surtur's black iron split with fire, the
 * Casket's pale iron round its blue heart, and the six stones. Lit from within: the stones, the
 * embers, the runes, the iris, the cracks, the Casket's heart.</li>
 * </ul>
 * Usage: javac -d out tools/VaultSkin.java src/main/java/com/matoon/eternity/client/render/VaultBoxes.java
 * then java -cp out VaultSkin src/main/resources/assets/eternity/textures/entity
 */
public class VaultSkin {

    private static final int TOP = 0, BOTTOM = 1, RIGHT = 2, FRONT = 3, LEFT = 4, BACK = 5;

    // ------------------------------------------------------------------ palettes, dark to light
    private static final int[] STEEL = {0x111318, 0x191c22, 0x23272f, 0x2f343e, 0x404754, 0x59626f, 0x848d9b};
    private static final int[] GOLD = {0x3a2507, 0x5a3c0d, 0x82591a, 0xa97d28, 0xcda243, 0xe8c670, 0xfbe8b4};
    private static final int[] MAIL = {0x0d0e10, 0x16181b, 0x22252a, 0x30343a, 0x454a51, 0x5f656d, 0x7d838b};
    private static final int[] LEATHER = {0x140b06, 0x1e120a, 0x2b1a0f, 0x3a2416, 0x4c311e, 0x603f28, 0x7a5234};
    private static final int[] WOOL = {0x372606, 0x54390b, 0x734f12, 0x8f661a, 0xa77c24, 0xbd9033, 0xd3a94e};
    private static final int[] WOOD = {0x120a06, 0x1c110a, 0x28180e, 0x342013, 0x432a19, 0x553621, 0x6c472c};
    private static final int[] BRONZE = {0x2a1a0a, 0x412a12, 0x5c3d1a, 0x7a5426, 0x986c34, 0xb58a4a, 0xd2ac6e};
    private static final int[] GUNMETAL = {0x0a0b0d, 0x121418, 0x1a1d22, 0x252a31, 0x353b44, 0x4c535e, 0x737c8a};
    private static final int[] POLISH = {0x1a1c20, 0x2a2d33, 0x3d4149, 0x565b64, 0x767c86, 0x9da3ac, 0xcfd4da};
    private static final int[] BLACK_STONE = {0x0a0a0c, 0x121215, 0x1a1a1e, 0x232328, 0x2e2e34, 0x3b3b42, 0x4d4d55};
    private static final int[] GREY_STONE = {0x1d1e21, 0x2a2b2f, 0x37393d, 0x46484d, 0x56595e, 0x696c72, 0x80848a};
    private static final int[] FROST_STONE = {0x2a3440, 0x3a4654, 0x4c5a6a, 0x5f6f80, 0x748597, 0x8c9daf, 0xb2c3d4};
    private static final int[] TABLET_STONE = {0x2f2a24, 0x403a32, 0x524b41, 0x655d52, 0x7a7165, 0x91877a, 0xaea395};
    private static final int[] PALE_IRON = {0x2c3138, 0x3d434c, 0x515862, 0x67707b, 0x808a96, 0x9ea8b3, 0xc4ccd5};
    private static final int[] BLACK_IRON = {0x08070a, 0x100e12, 0x19161b, 0x231f25, 0x2f2a31, 0x3d373f, 0x524a54};
    private static final int[] PEARL = {0x6f6a75, 0x8a8591, 0xa49fab, 0xbcb7c3, 0xd2cdd8, 0xe4e0e9, 0xf5f3f8};

    private static BufferedImage skin, glow;
    private static Sheet sheet;

    public static void main(String[] args) throws Exception {
        File out = new File(args[0]);
        out.mkdirs();
        paint(VaultBoxes.EINHERJAR, new File(out, "einherjar.png"), null);
        paint(VaultBoxes.DESTROYER, new File(out, "destroyer.png"), new File(out, "destroyer_glow.png"));
        paint(VaultBoxes.RELICS, new File(out, "vault_relics.png"), new File(out, "vault_relics_glow.png"));
    }

    private static void paint(Sheet s, File skinFile, File glowFile) throws Exception {
        sheet = s;
        skin = new BufferedImage(s.width, s.height, BufferedImage.TYPE_INT_ARGB);
        glow = new BufferedImage(s.width, s.height, BufferedImage.TYPE_INT_ARGB);
        for (Box b : s.all) {
            box(b);
        }
        ImageIO.write(skin, "png", skinFile);
        if (glowFile != null) {
            ImageIO.write(glow, "png", glowFile);
        }
    }

    // ------------------------------------------------------------------ laying out a box

    /** Paints every face of a box, laid out the vanilla way at its (u, v). */
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

    /** One pixel of one face of one box, and where it lies in the part's own space. */
    private static final class Px {
        final Box b;
        final int face, x, y, w, h;
        /** The pixel's centre in model space. */
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
    }

    // ------------------------------------------------------------------ what each thing is made of

    private static int paint(Px p) {
        switch (p.b.material) {
            // The Einherjar.
            case VaultBoxes.STEEL:
                return steel(p);
            case VaultBoxes.GOLD:
                return brushed(GOLD, 4, p);
            case VaultBoxes.MAIL:
                return mail(p);
            case VaultBoxes.LEATHER:
                return leather(p);
            case VaultBoxes.CLOAK:
                return cloak(p);
            case VaultBoxes.SHAFT:
                return shaft(p);
            case VaultBoxes.BLADE:
                return blade(p);
            case VaultBoxes.SHIELD:
                return shield(p);
            case VaultBoxes.HELM:
                return helm(p);
            case VaultBoxes.VISOR:
                return visor(p);
            // The Destroyer.
            case VaultBoxes.RIB:
                return rib(p, 2);
            case VaultBoxes.HEAD:
                return rib(p, 2);
            case VaultBoxes.CORE:
                return core(p);
            case VaultBoxes.FACE:
                return faceplate(p);
            case VaultBoxes.SPIKE:
                return spike(p);
            case VaultBoxes.FIST:
                return fist(p);
            case VaultBoxes.FOOT:
                return rib(p, 3);
            case VaultBoxes.CROWN:
                return crown(p);
            // The treasures.
            case VaultBoxes.STONE_DARK:
                return stone(BLACK_STONE, 3, p);
            case VaultBoxes.STONE_CARVED:
                return carved(p);
            case VaultBoxes.STONE_FLUTED:
                return fluted(p);
            case VaultBoxes.STONE_FROST:
                return frostStone(p);
            case VaultBoxes.BRONZE:
                return bronze(p);
            case VaultBoxes.EMBER:
                return embers(p);
            case VaultBoxes.GILT:
                return gilt(p);
            case VaultBoxes.TABLET:
                return tablet(p);
            case VaultBoxes.ORB:
                return orb(p);
            case VaultBoxes.IRON:
                return blackIron(p);
            case VaultBoxes.CASKET:
                return casket(p);
            case VaultBoxes.CASKET_CORE:
                return casketCore(p);
            case VaultBoxes.ICE:
                return ice(p);
            default:
                return gem(p);
        }
    }

    /** What lights up from within, drawn additively at full brightness; 0 for nothing. */
    private static int glowOf(Px p) {
        switch (p.b.material) {
            case VaultBoxes.CORE:
                return argb(0.95F, mix(0xFFB040, 0xFFF4C0, noise(p, 5) * 0.6D));
            case VaultBoxes.FACE: {
                if (p.face != FRONT || !slit(p)) {
                    return 0;
                }
                // The eyes burn brightest; the slats below and above only smoulder.
                boolean eyes = p.y >= 2 && p.y <= 3;
                return argb(eyes ? 1F : 0.35F, eyes ? 0xFFC860 : 0xFF7A20);
            }
            case VaultBoxes.EMBER:
                return emberGlow(p);
            case VaultBoxes.STONE_FROST:
                return rune(p, 5) ? argb(0.85F, 0x7FD8FF) : 0;
            case VaultBoxes.STONE_FLUTED:
                return runeBand(p) && rune(p, 3) ? argb(0.5F, 0xFFC860) : 0;
            case VaultBoxes.TABLET:
                return tabletGlow(p);
            case VaultBoxes.ORB:
                return orbGlow(p);
            case VaultBoxes.IRON:
                return p.part("cStand") ? 0 : crack(p) ? argb(0.95F, mix(0xFF4010, 0xFFB040, noise(p, 9))) : 0;
            case VaultBoxes.CASKET_CORE:
                return lattice(p) ? 0 : argb(0.95F, mix(0x60C8FF, 0xE0F8FF, noise(p, 4) * 0.7D));
            case VaultBoxes.ICE:
                return argb(0.35F, 0xA0E0FF);
            case VaultBoxes.GEM_SPACE:
            case VaultBoxes.GEM_MIND:
            case VaultBoxes.GEM_REALITY:
            case VaultBoxes.GEM_POWER:
            case VaultBoxes.GEM_TIME:
            case VaultBoxes.GEM_SOUL:
                return argb(0.9F, gemColour(p.b.material, 5));
            default:
                return 0;
        }
    }

    // ------------------------------------------------------------------ the Einherjar

    /** Blackened plate: lit from above, darker at its edges, gold along its rims; on the breastplate, the gold chevrons. */
    private static int steel(Px p) {
        int level = 3;
        if (p.h >= 4 && !p.cap()) {
            level += p.y < p.h / 3 ? 1 : p.y >= p.h - p.h / 3 ? -1 : 0;
        }
        if (p.face == TOP) {
            level += 1;
        }
        if (!p.cap() && (p.x == 0 || p.x == p.w - 1)) {
            level -= 1;
        }
        // A rim of gold round the plates you see most: breastplate, pauldrons, tassets, greaves.
        boolean rimmed = p.part("chest") || p.part("pauldron") || p.part("tasset") || p.part("thigh") || p.part("shin") || p.part("abdomen");
        if (rimmed && !p.cap() && (p.y == p.h - 1 || p.y == 0 && p.part("pauldron"))) {
            return GOLD[p.y == 0 ? 5 : 4];
        }
        // The chevrons, converging down the breastplate.
        if (p.part("chest") && p.face == FRONT && p.b.w == 8 && p.b.h == 5) {
            double c = (p.w - 1) / 2D;
            double dx = Math.abs(p.x - c);
            double k = dx - (c - p.y * 1.1D);
            if (Math.abs(k) < 0.6D || Math.abs(k + 2.2D) < 0.6D) {
                return GOLD[Math.abs(k) < 0.6D ? 5 : 4];
            }
        }
        // Rivets at the corners of the pauldrons and tassets.
        if ((p.part("pauldron") || p.part("tasset")) && !p.cap() && p.h >= 3 && (p.x == 1 || p.x == p.w - 2) && p.y == p.h - 2) {
            return GOLD[5];
        }
        return grain(STEEL, level, p, 0.1D);
    }

    /** Brushed metal: fine horizontal strokes, brighter on top. */
    private static int brushed(int[] ramp, int level, Px p) {
        int l = level + (p.face == TOP ? 1 : p.face == BOTTOM ? -2 : 0);
        if (!p.cap() && p.h > 2) {
            l += p.y == 0 ? 1 : p.y == p.h - 1 ? -1 : 0;
        }
        if (hash(p.y * 7 + p.face, p.b.u) % 3 == 0) {
            l--;
        }
        return grain(ramp, l, p, 0.06D);
    }

    /** Mail: rows of rings, each catching the light at its top. */
    private static int mail(Px p) {
        if (p.face == BOTTOM) {
            return MAIL[1];
        }
        boolean ring = (p.x + (p.y % 2)) % 2 == 0;
        int level = ring ? 4 : 1;
        if (p.y % 2 == 0 && ring) {
            level++;
        }
        if (p.part("upper") || p.part("fore")) {
            level -= p.y > p.h * 2 / 3 ? 1 : 0;
        }
        return mix(MAIL[clamp(level)], 0x6b5a3c, ring ? 0.08D : 0D);
    }

    private static int leather(Px p) {
        int level = 3 + (p.face == TOP ? 1 : 0);
        // Stitching along the edges, a row of pale dots.
        if (!p.cap() && (p.y == 0 || p.y == p.h - 1) && p.x % 2 == 0 && p.h >= 2) {
            return LEATHER[6];
        }
        if (p.part("pelvis") && p.face == FRONT && p.x >= p.w / 2 - 1 && p.x <= p.w / 2) {
            return GOLD[5];
        }
        return grain(LEATHER, level, p, 0.14D);
    }

    /** The cloak: heavy mustard wool in long folds, a woven border along its hem and a darker lining. */
    private static int cloak(Px p) {
        boolean inside = p.face == FRONT && p.part("cape");
        int segment = p.part("cape") ? p.b.part.charAt(4) - '0' : -1;
        double fold = Math.sin(p.mx * 1.15D + (segment < 0 ? 0 : segment * 0.4D)) * 1.3D + Math.sin(p.mx * 0.5D + 1.3D) * 0.6D;
        int level = 4 + (int) Math.round(fold);
        if (inside) {
            level -= 2;
        }
        if (segment == VaultBoxes.EINHERJAR_CAPE - 1 && !p.cap()) {
            if (p.y == p.h - 1) {
                return GOLD[3];
            }
            if (p.y == p.h - 2) {
                return (p.x % 3 == 0) ? GOLD[5] : WOOL[1];
            }
            if (p.y == p.h - 3) {
                return GOLD[4];
            }
        }
        if (p.face == TOP && segment > 0) {
            level -= 1;
        }
        // Worn and dusty: a few lighter flecks.
        if (hash((int) (p.mx * 13), (int) (p.my * 7) + p.face * 31) % 11 == 0) {
            level++;
        }
        return WOOL[clamp(level)];
    }

    /** The spear's shaft: dark wood, one edge catching the light. */
    private static int shaft(Px p) {
        int level = p.face == FRONT || p.face == LEFT ? 4 : 2;
        if (!p.cap() && hash(p.y, p.face) % 5 == 0) {
            level++;
        }
        return WOOD[clamp(level)];
    }

    /** A gilt leaf blade, a bright fuller down its middle, its edges honed pale. */
    private static int blade(Px p) {
        if (p.face == FRONT || p.face == BACK) {
            boolean edge = p.w > 1 && (p.x == 0 || p.x == p.w - 1);
            return edge ? GOLD[6] : GOLD[p.y % 3 == 0 ? 5 : 4];
        }
        return GOLD[p.cap() ? 5 : 3];
    }

    /** The shield: bronze embossed in rings, a band of knotwork, rivets at the rim, and its back of leather on wood. */
    private static int shield(Px p) {
        if (p.face == RIGHT) {
            return grain(WOOD, 3, p, 0.1D);
        }
        if (p.face != LEFT) {
            return BRONZE[2];
        }
        double r = Math.sqrt(p.my * p.my + p.mz * p.mz);
        double a = Math.atan2(p.my, p.mz);
        if (r > 5.4D) {
            return BRONZE[(int) Math.round(a * 4D / Math.PI) % 2 == 0 && r > 5.8D ? 5 : 2];
        }
        if (r > 4.4D) {
            return BRONZE[4];
        }
        if (r > 3.2D) {
            // The knotwork: a braid running round the shield.
            double braid = Math.sin(a * 9D) * 0.5D;
            return Math.abs(r - 3.8D - braid * 0.5D) < 0.35D ? BRONZE[6] : BRONZE[2];
        }
        if (r > 2.6D) {
            return BRONZE[1];
        }
        return grain(BRONZE, r < 1.6D ? 5 : 4, p, 0.1D);
    }

    /** The helm: blackened steel, a gold band at the brow and swirls of gold up its sides. */
    private static int helm(Px p) {
        if (p.face == TOP) {
            return grain(STEEL, 4, p, 0.08D);
        }
        if (p.side() && p.b.h == 8) {
            double cx = p.w / 2D, cy = p.h / 2D;
            double r = Math.hypot(p.x + 0.5D - cx, p.y + 0.5D - cy);
            if (Math.abs(r - 2.3D) < 0.45D && p.y < p.h - 2) {
                return GOLD[4];
            }
        }
        if (!p.cap() && p.y == 1 && p.b.h >= 6) {
            return GOLD[4];
        }
        return steel(p);
    }

    /** No face: a T of black between the cheek plates, its edges in gold. */
    private static int visor(Px p) {
        if (p.face != FRONT) {
            return STEEL[2];
        }
        int cx = p.w / 2;
        boolean brow = p.y == 1 || p.y == 2;
        boolean nose = (p.x == cx - 1 || p.x == cx) && p.y >= 1;
        if (brow || nose) {
            return 0x050506;
        }
        if (p.y == 0 || (p.y == 3 && !nose)) {
            return GOLD[3];
        }
        return grain(STEEL, 3, p, 0.08D);
    }

    // ------------------------------------------------------------------ the Destroyer

    /**
     * Banded plate, dark and glossy: every band catches the light along its top edge and falls into
     * shadow toward the seam under it, a soft sheen runs across the bands where the light falls, and
     * down the middle of its body a seam where the plates meet.
     */
    private static int rib(Px p, int pitch) {
        if (p.cap()) {
            int level = p.face == TOP ? 3 : 1;
            if ((p.x + p.y) % 4 == 0) {
                level--;
            }
            return grain(GUNMETAL, level, p, 0.08D);
        }
        int within = (int) Math.floor(p.my + 100D) % pitch;
        if (within == pitch - 1 && pitch > 1) {
            return GUNMETAL[0];
        }
        if ((p.face == FRONT || p.face == BACK) && p.b.side == 0 && p.w >= 8 && Math.abs(p.mx) < 0.5D) {
            return GUNMETAL[1];
        }
        int level = within == 0 ? 3 : 2;
        // The sheen: brightest a third of the way across the face, toward the light.
        double across = p.w > 1 ? (p.x + 0.5D) / p.w : 0.5D;
        double sheen = Math.exp(-Math.pow((across - 0.35D) * 3.2D, 2D));
        level += (int) Math.round(sheen * (within == 0 ? 1.4D : 0.8D)) - (p.face == BACK ? 1 : 0);
        int c = grain(GUNMETAL, level, p, 0.05D);
        return mix(within == pitch - 2 && pitch > 2 ? mix(c, GUNMETAL[1], 0.4D) : c, 0x4a3c28, 0.07D);
    }

    /** The face plates: slats with black slits between, a bright rim along their outer edge. */
    private static int faceplate(Px p) {
        if (p.face == BACK) {
            return GUNMETAL[1];
        }
        if (p.face != FRONT) {
            return GUNMETAL[3];
        }
        if (slit(p)) {
            return 0x0a0603;
        }
        boolean outer = p.b.part.equals("faceR") ? p.x == 0 : p.x == p.w - 1;
        int level = outer ? 6 : 4 + (p.y % 2 == 0 ? 1 : 0);
        return grain(POLISH, level - 1, p, 0.05D);
    }

    private static boolean slit(Px p) {
        return p.y % 2 == 1 && p.y < p.h - 1 && p.y > 0;
    }

    /** The furnace behind its face, cold and dark until it wakes. */
    private static int core(Px p) {
        int c = mix(0x1a0602, 0x4a1206, noise(p, 7));
        return p.face == FRONT ? mix(c, 0x8a2a08, noise(p, 3) * 0.4D) : c;
    }

    private static int spike(Px p) {
        boolean tip = p.cap() || p.side() && p.x == (p.b.part.endsWith("R") ? 0 : p.w - 1);
        return tip ? POLISH[6] : POLISH[3];
    }

    /** Its fists: banded, a ridge of knuckles across the front. */
    private static int fist(Px p) {
        if (p.face == FRONT && p.b.h == 2) {
            return p.x % 2 == 0 ? POLISH[5] : GUNMETAL[2];
        }
        if (p.face == BOTTOM) {
            return GUNMETAL[2];
        }
        return rib(p, 2);
    }

    /** The folded crown: polished, engraved in long lines from front to back, its edges bright. */
    private static int crown(Px p) {
        if (p.face == TOP) {
            boolean line = p.x % 2 == 1;
            boolean edge = p.y == p.h - 1 || p.x == 0 || p.x == p.w - 1;
            return edge ? POLISH[6] : grain(POLISH, line ? 3 : 5, p, 0.05D);
        }
        if (p.face == BOTTOM) {
            return GUNMETAL[2];
        }
        return p.y == 0 ? POLISH[6] : grain(POLISH, 4, p, 0.05D);
    }

    // ------------------------------------------------------------------ the treasures

    private static int stone(int[] ramp, int level, Px p) {
        int l = level + (p.face == TOP ? 1 : p.face == BOTTOM ? -1 : 0);
        double vein = Math.sin(p.mx * 0.9D + p.my * 0.6D + p.mz * 0.7D + noise(p, 2) * 2D);
        if (Math.abs(vein) < 0.08D) {
            l += 2;
        }
        return grain(ramp, l, p, 0.12D);
    }

    /** Carved stone: plain bands at top and bottom, and between them an interlace cut into it. */
    private static int carved(Px p) {
        if (p.cap() || p.h < 2) {
            return stone(GREY_STONE, 3, p);
        }
        if (p.y == 0) {
            return GREY_STONE[5];
        }
        if (p.y == p.h - 1) {
            return GREY_STONE[1];
        }
        int k = Math.floorMod(p.x + p.y, 4);
        int j = Math.floorMod(p.x - p.y, 4);
        if (p.h >= 3 && (k == 0 || j == 0)) {
            return GREY_STONE[1];
        }
        return grain(GREY_STONE, 4, p, 0.1D);
    }

    /** A fluted column: grooves running up it, and a band of runes round its middle. */
    private static int fluted(Px p) {
        if (p.cap()) {
            return stone(GREY_STONE, 3, p);
        }
        if (runeBand(p)) {
            return rune(p, 3) ? GOLD[4] : GREY_STONE[1];
        }
        if (Math.abs(p.my - (p.b.y + p.b.h * 0.42D)) < 0.6D || Math.abs(p.my - (p.b.y + p.b.h * 0.58D)) < 0.6D) {
            return GOLD[3];
        }
        int level = p.x % 3 == 0 ? 1 : p.x % 3 == 1 ? 4 : 3;
        level += p.y < p.h / 5 ? 1 : 0;
        return grain(GREY_STONE, level, p, 0.08D);
    }

    private static boolean runeBand(Px p) {
        double mid = p.b.y + p.b.h * 0.5D;
        return !p.cap() && Math.abs(p.my - mid) < 2D && p.b.h > 10;
    }

    /** A rune: short strokes, deterministic from where the pixel is. */
    private static boolean rune(Px p, int period) {
        if (p.cap()) {
            return false;
        }
        int cell = (int) Math.floor((p.side() ? p.mz : p.mx) / period + 50D);
        int row = (int) Math.floor(p.my / period + 50D);
        int lx = Math.floorMod((int) Math.floor(p.side() ? p.mz : p.mx), period);
        int ly = Math.floorMod((int) Math.floor(p.my), period);
        int h = hash(cell * 31 + row, p.face);
        boolean vertical = lx == h % Math.max(1, period - 1);
        boolean stroke = (h >> 3) % 2 == 0 ? ly == lx : ly == period - 1 - lx;
        return (vertical || stroke && (h >> 5) % 3 != 0) && lx < period - 1 && ly < period - 1;
    }

    /** The Pedestal of Fimbulvetr: pale frosted stone, crystals of rime on it, runes cut deep and glowing blue. */
    private static int frostStone(Px p) {
        if (rune(p, 5) && !p.cap()) {
            return 0x3a6a8a;
        }
        int c = grain(FROST_STONE, 4 + (p.face == TOP ? 1 : 0), p, 0.12D);
        if (hash((int) (p.mx * 11) + p.face, (int) (p.my * 13)) % 7 == 0) {
            c = mix(c, 0xEAF6FF, 0.6D);
        }
        return c;
    }

    /** Old bronze, darkened, green in its hollows, bright where it has been handled. */
    private static int bronze(Px p) {
        int level = 3 + (p.face == TOP ? 2 : p.face == BOTTOM ? -1 : 0);
        if (!p.cap() && p.y == 0) {
            level += 2;
        }
        int c = grain(BRONZE, level, p, 0.12D);
        if (noise(p, 6) > 0.82D) {
            c = mix(c, 0x3f7a62, 0.5D);
        }
        if (!p.cap() && p.h >= 4 && (p.y == p.h / 2) && p.x % 2 == 0) {
            c = BRONZE[5];
        }
        return c;
    }

    private static int embers(Px p) {
        double n = noise(p, 8);
        return n > 0.55D ? mix(0x5a1a04, 0xc24a10, (n - 0.55D) * 2D) : mix(0x0e0806, 0x2a120a, n);
    }

    private static int emberGlow(Px p) {
        double n = noise(p, 8);
        return n > 0.55D ? argb((float) Math.min(1D, (n - 0.5D) * 2.4D), mix(0xFF5010, 0xFFD060, (n - 0.55D) * 2D)) : 0;
    }

    /** Gilt: polished gold, engraved with lines that follow the shape, a bright edge where the light falls. */
    private static int gilt(Px p) {
        int level = 4 + (p.face == TOP ? 1 : p.face == BOTTOM ? -2 : 0);
        if (!p.cap() && p.h >= 3) {
            if (p.y == 0) {
                level += 2;
            }
            if (p.y % 3 == 2 && p.w > 1) {
                level -= 2;
            }
        }
        if (!p.cap() && p.w >= 3 && (p.x == 0 || p.x == p.w - 1)) {
            level--;
        }
        return grain(GOLD, level, p, 0.08D);
    }

    /** The Tablet: carved stone, and on its face a great ring of life and time with spokes, runes above and below. */
    private static int tablet(Px p) {
        if (tabletMark(p)) {
            return 0x1e1a15;
        }
        return grain(TABLET_STONE, 4 + (p.face == TOP ? 1 : 0), p, 0.12D);
    }

    private static boolean tabletMark(Px p) {
        if (p.face != FRONT) {
            return p.face == BACK && rune(p, 4);
        }
        if (p.b.h == 16) {
            double cx = 0D, cy = -9D;
            double r = Math.hypot(p.mx - cx, p.my - cy);
            if (Math.abs(r - 3.6D) < 0.5D || Math.abs(r - 2.2D) < 0.4D) {
                return true;
            }
            if (r < 3.6D && r > 2.2D) {
                double a = Math.atan2(p.my - cy, p.mx - cx);
                return Math.abs(Math.sin(a * 6D)) < 0.25D;
            }
            if (p.my < -14D || p.my > -4.5D) {
                return rune(p, 3);
            }
            return p.x == 0 || p.x == p.w - 1;
        }
        return p.b.h == 4 && (p.x == 0 || p.x == p.w - 1 || p.y == 0 || p.y == p.h - 1);
    }

    private static int tabletGlow(Px p) {
        if (p.face != FRONT || !tabletMark(p)) {
            return 0;
        }
        boolean ring = p.b.h == 16 && Math.abs(Math.hypot(p.mx, p.my + 9D) - 3.6D) < 0.5D;
        return argb(ring ? 0.9F : 0.6F, ring ? 0x8FFFE0 : 0xFFD070);
    }

    /** The orb: milky and veined, and on its front an iris round a black pupil. */
    private static int orb(Px p) {
        if (p.face == FRONT) {
            double r = Math.hypot(p.mx, p.my);
            if (r < 1.1D) {
                return 0x08060a;
            }
            if (r < 2.4D) {
                return mix(0x5a2a7a, 0xd09a30, (r - 1.1D) / 1.3D);
            }
            if (r < 2.9D) {
                return 0x2a1a30;
            }
        }
        int c = grain(PEARL, 4 + (p.face == TOP ? 1 : 0), p, 0.08D);
        double vein = Math.sin(p.mx * 1.7D + p.my * 1.3D + p.mz * 0.9D);
        return Math.abs(vein) < 0.06D ? mix(c, 0x9a6a8a, 0.45D) : c;
    }

    private static int orbGlow(Px p) {
        if (p.face != FRONT) {
            return 0;
        }
        double r = Math.hypot(p.mx, p.my);
        return r >= 1.1D && r < 2.4D ? argb(0.75F, mix(0xB070FF, 0xFFC060, (r - 1.1D) / 1.3D)) : 0;
    }

    /** Surtur's black iron, split by cracks that still burn. */
    private static int blackIron(Px p) {
        if (!p.part("cStand") && crack(p)) {
            return 0x7a1a04;
        }
        return grain(BLACK_IRON, 3 + (p.face == TOP ? 2 : 0), p, 0.1D);
    }

    private static boolean crack(Px p) {
        double n = Math.sin(p.mx * 1.3D + Math.sin(p.my * 1.1D) * 1.8D + p.mz * 0.8D) + Math.sin(p.my * 2.1D + p.mz * 1.4D) * 0.4D;
        return Math.abs(n) < 0.12D;
    }

    /** The Casket's frame: pale iron, engraved in knots, riveted at its corners. */
    private static int casket(Px p) {
        int level = 4 + (p.face == TOP ? 1 : p.face == BOTTOM ? -2 : 0);
        if (!p.cap() && p.w >= 4 && p.h >= 4) {
            if ((p.x == 0 || p.x == p.w - 1) && (p.y == 0 || p.y == p.h - 1)) {
                return PALE_IRON[6];
            }
            int k = Math.floorMod(p.x + p.y * 2, 5);
            if (k == 0 && p.x > 0 && p.x < p.w - 1 && p.y > 0 && p.y < p.h - 1) {
                level -= 2;
            }
        }
        return grain(PALE_IRON, level, p, 0.08D);
    }

    /** A lattice of pale iron over the Casket's heart: the heart shows through the gaps. */
    private static int casketCore(Px p) {
        if (lattice(p)) {
            return grain(PALE_IRON, 5, p, 0.05D);
        }
        return mix(0x0a2a6a, 0x3a8ad8, noise(p, 4));
    }

    private static boolean lattice(Px p) {
        if (p.cap() || p.w < 3) {
            return p.b.w <= 2;
        }
        int a = Math.floorMod(p.x + p.y, 3), b = Math.floorMod(p.x - p.y, 3);
        return a == 0 || b == 0;
    }

    private static int ice(Px p) {
        return mix(grain(FROST_STONE, 5, p, 0.1D), 0xdff4ff, 0.45D);
    }

    /** A stone: its colour, a highlight in one corner, a shadow in the opposite one. */
    private static int gem(Px p) {
        int base = gemColour(p.b.material, 3);
        if (p.w > 1 && p.x == 0 && p.y == 0) {
            return gemColour(p.b.material, 6);
        }
        if (p.w > 1 && p.x == p.w - 1 && p.y == p.h - 1) {
            return gemColour(p.b.material, 1);
        }
        return p.face == TOP ? gemColour(p.b.material, 5) : base;
    }

    private static int gemColour(int material, int level) {
        int c;
        switch (material) {
            case VaultBoxes.GEM_SPACE:
                c = 0x2a6aff;
                break;
            case VaultBoxes.GEM_MIND:
                c = 0xffd21a;
                break;
            case VaultBoxes.GEM_REALITY:
                c = 0xe8102a;
                break;
            case VaultBoxes.GEM_POWER:
                c = 0x9a2aff;
                break;
            case VaultBoxes.GEM_TIME:
                c = 0x1ae05a;
                break;
            default:
                c = 0xff8a1a;
                break;
        }
        return level >= 3 ? mix(c, 0xFFFFFF, (level - 3) * 0.22D) : mix(c, 0x000000, (3 - level) * 0.25D);
    }

    // ------------------------------------------------------------------ helpers

    /** A level on a ramp with a little grain: neighbouring pixels a shade apart here and there. */
    private static int grain(int[] ramp, int level, Px p, double amount) {
        double n = noise(p, 1);
        int l = level;
        if (n < amount) {
            l--;
        } else if (n > 1D - amount) {
            l++;
        }
        return ramp[clamp(l)];
    }

    /** Noise in [0, 1), fixed for a pixel of a face of a box, so painting twice paints the same. */
    private static double noise(Px p, int salt) {
        int h = hash(p.b.u * 977 + p.b.v * 131 + p.face * 7919 + salt * 104729, p.x * 73 + p.y * 151);
        return (h & 0xFFFF) / 65536D;
    }

    private static int hash(int a, int b) {
        int h = a * 0x45d9f3b ^ b * 0x119de1f3;
        h ^= h >>> 16;
        h *= 0x45d9f3b;
        h ^= h >>> 16;
        return h & 0x7FFFFFFF;
    }

    private static int argb(float alpha, int rgb) {
        return ((int) (Math.max(0F, Math.min(1F, alpha)) * 255F) << 24) | (rgb & 0xFFFFFF);
    }

    private static int mix(int a, int b, double t) {
        t = Math.max(0D, Math.min(1D, t));
        int r = (int) Math.round(((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
        int g = (int) Math.round(((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
        int bl = (int) Math.round((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return r << 16 | g << 8 | bl;
    }

    private static int clamp(int level) {
        return Math.max(0, Math.min(6, level));
    }
}
