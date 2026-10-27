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
 * engraved crown; a face of slats over a furnace; a ribbed hollow in its back behind two hinged
 * plates. Lit from within: the slits of its face, the furnace behind them, and the seams of the hollow.</li>
 * <li><b>destroyer_damage_1..3.png</b> and <b>destroyer_damage_glow_1..3.png</b>: its wounds, laid over whatever finish it
 * wears, each stage keeping the last's: dents and bright gouges, then soot and cracks, then plate burst open over
 * the fire. Lit from within: the deepest cracks and the holes.</li>
 * <li><b>vault_relics.png</b> and <b>vault_relics_glow.png</b>: the treasures and their stands. Black
 * and carved stone, fluted columns with a band of runes, the frosted pedestal, aged bronze, engraved
 * gilt, the Tablet's carved rings, the Warlock's Eye and its bronze frame, Surtur's skull and its
 * whorled blades and horns, the Casket's veined blue glass in its dark stone frame, and the six stones.
 * Lit from within: the stones, the embers, the runes, the eye, the plinth's light, the Casket's glass.</li>
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
    private static final int[] GUNMETAL = {0x09090b, 0x121317, 0x1d1f24, 0x2b2e35, 0x41454e, 0x666c76, 0xa8aeb7};
    private static final int[] POLISH = {0x1a1c20, 0x2a2d33, 0x3d4149, 0x565b64, 0x767c86, 0x9da3ac, 0xcfd4da};
    private static final int[] BLACK_STONE = {0x0a0a0c, 0x121215, 0x1a1a1e, 0x232328, 0x2e2e34, 0x3b3b42, 0x4d4d55};
    private static final int[] GREY_STONE = {0x1d1e21, 0x2a2b2f, 0x37393d, 0x46484d, 0x56595e, 0x696c72, 0x80848a};
    private static final int[] FROST_STONE = {0x2a3440, 0x3a4654, 0x4c5a6a, 0x5f6f80, 0x748597, 0x8c9daf, 0xb2c3d4};
    private static final int[] TABLET_STONE = {0x2f2a24, 0x403a32, 0x524b41, 0x655d52, 0x7a7165, 0x91877a, 0xaea395};
    private static final int[] BRASS = {0x1e1408, 0x2f200e, 0x473016, 0x634420, 0x825b2c, 0xa6793e, 0xcc9e5a};
    private static final int[] CROWN_GREY = {0x0e0e10, 0x17171a, 0x222225, 0x2e2e33, 0x3e3e44, 0x56565e, 0x7c7c86};
    private static final int[] FRAME = {0x121316, 0x1b1d21, 0x26292e, 0x33373d, 0x454a51, 0x5e646c, 0x858b94};
    private static final int[] PEARL = {0x6f6a75, 0x8a8591, 0xa49fab, 0xbcb7c3, 0xd2cdd8, 0xe4e0e9, 0xf5f3f8};

    private static BufferedImage skin, glow;
    private static Sheet sheet;

    public static void main(String[] args) throws Exception {
        File out = new File(args[0]);
        out.mkdirs();
        paint(VaultBoxes.EINHERJAR, new File(out, "einherjar.png"), null);
        paint(VaultBoxes.DESTROYER, new File(out, "destroyer.png"), new File(out, "destroyer_glow.png"));
        finishes(out);
        wounds(out);
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
                int c = paint(p);
                skin.setRGB(x0 + x, y0 + y, 0xFF000000 | (finish == null ? c : finish(p, c)));
                int g = glowOf(p);
                if (finish != null && finishGlow(p) != 0) {
                    g = finishGlow(p);
                }
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
                return slats(p);
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
            case VaultBoxes.HOLLOW:
                return hollow(p);
            case VaultBoxes.HATCH:
                return hatch(p);
            case VaultBoxes.BAND:
                return band(p, false);
            case VaultBoxes.STUD:
                return band(p, true);
            case VaultBoxes.GROOVE:
                return grain(GUNMETAL, p.cap() ? 1 : 0, p, 0.15D);
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
            case VaultBoxes.BRASS:
                return brass(p);
            case VaultBoxes.LIGHT:
                return light(p);
            case VaultBoxes.SKULL:
                return skull(p);
            case VaultBoxes.CROWN_STONE:
                return crownStone(p);
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
            case VaultBoxes.HOLLOW:
                return seam(p) ? argb(0.8F, 0xFF8A28) : 0;
            case VaultBoxes.HATCH:
                return p.face == FRONT && seam(p) ? argb(0.6F, 0xFF8A28) : 0;
            case VaultBoxes.FACE: {
                if (p.face != FRONT || !slit(p)) {
                    return 0;
                }
                // Its eyes: the fire behind its face, showing through the slits.
                boolean eyes = p.y == 2;
                return argb(eyes ? 1F : 0.6F, eyes ? 0xFFC860 : 0xFF7A20);
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
            case VaultBoxes.LIGHT:
                return argb(0.9F, 0xfff0d8);
            case VaultBoxes.CASKET_CORE:
                return casketGlow(p);
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

    /**
     * One raised band of its plate, as the film has them: polished, a hard bright lip along the top where
     * the light catches it, the face falling away into shadow toward the groove below, the ledge on top
     * shining. Studded bands carry a row of rivets along their lower half, as its shoulders and arms do.
     */
    private static int band(Px p, boolean studded) {
        if (p.face == TOP) {
            return grain(GUNMETAL, (p.x + p.y) % 5 == 0 ? 3 : 4, p, 0.03D);
        }
        if (p.face == BOTTOM) {
            return GUNMETAL[0];
        }
        if (studded && p.h >= 2 && p.y == p.h - 1 && p.x % 3 == 1) {
            return POLISH[5];
        }
        if (studded && p.h >= 2 && p.y == p.h - 1 && p.x % 3 == 2) {
            return GUNMETAL[1];
        }
        int level = p.y == 0 ? 4 : p.y == p.h - 1 ? 2 : 3;
        double across = p.w > 1 ? (p.x + 0.5D) / p.w : 0.5D;
        double sheen = Math.exp(-Math.pow((across - 0.35D) * 3.2D, 2D));
        level += (int) Math.round(sheen * (p.y == 0 ? 1.2D : 0.9D)) - (p.face == BACK ? 1 : 0);
        // The seam down the middle of its chest and waist.
        if ((p.face == FRONT || p.face == BACK) && p.b.side == 0 && p.w >= 8 && Math.abs(p.mx) < 0.5D) {
            level = 1;
        }
        return mix(grain(GUNMETAL, level, p, 0.02D), 0x4a3c28, 0.06D);
    }

    /** Its helm: fine polished slats, each lit along its top, a thin dark line between them, brightest down its front. */
    private static int slats(Px p) {
        if (p.cap()) {
            return grain(GUNMETAL, p.face == TOP ? 4 : 1, p, 0.03D);
        }
        int level = (int) Math.floor(p.my + 100D) % 2 == 0 ? 4 : 2;
        if (p.face == FRONT) {
            level++;
        } else if (p.face == BACK) {
            level--;
        }
        return mix(grain(GUNMETAL, level, p, 0.02D), 0x4a3c28, 0.05D);
    }

    /**
     * The face plates: polished slats, a fine dark seam between each, a bright rim along their outer edge;
     * and near the top, under the crown, the slit of an eye cut right through, where the fire shows.
     */
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
        if (p.y % 2 == 1) {
            return GUNMETAL[2];
        }
        boolean outer = p.b.part.equals("faceR") ? p.x == 0 : p.x == p.w - 1;
        return mix(grain(GUNMETAL, outer ? 6 : 5, p, 0.03D), 0x4a3c28, 0.05D);
    }

    /** The eye slit cut through a face plate, two rows under the crown. */
    private static boolean slit(Px p) {
        return p.y == 2 || p.y == 3;
    }

    /** The hollow in its back: dark ribbing, the furnace's heat showing along the seams between the ribs. */
    private static int hollow(Px p) {
        if (seam(p)) {
            return 0x2a0e04;
        }
        return grain(GUNMETAL, p.x % 3 == 0 ? 0 : 1, p, 0.1D);
    }

    /** The plates of its back: banded plate outside; inside, the same ribbing as the hollow they close over. */
    private static int hatch(Px p) {
        if (p.face == FRONT) {
            return hollow(p);
        }
        if (p.face == BACK) {
            boolean edge = p.b.side < 0 ? p.x == 0 : p.x == p.w - 1;
            return edge ? GUNMETAL[1] : rib(p, 2);
        }
        return rib(p, 2);
    }

    /** The seams of the hollow: a lattice the heat inside shows through. */
    private static boolean seam(Px p) {
        return p.y % 3 == 1 && p.x % 3 != 0;
    }

    /** The furnace behind its face, cold and dark until it wakes. */
    private static int core(Px p) {
        int c = mix(0x1a0602, 0x4a1206, noise(p, 7));
        return p.face == FRONT ? mix(c, 0x8a2a08, noise(p, 3) * 0.4D) : c;
    }

    private static int spike(Px p) {
        // The point bright, the broad base the dark steel of the plate it stands on.
        boolean tip = p.b.delta < 0F;
        return tip ? POLISH[p.face == TOP ? 6 : 5] : GUNMETAL[p.face == TOP ? 4 : 3];
    }

    /** Its fists: banded, a ridge of knuckles across the front. */
    private static int fist(Px p) {
        if (p.face == FRONT && p.b.h == 2) {
            return p.x % 2 == 0 ? GUNMETAL[5] : GUNMETAL[1];
        }
        if (p.face == BOTTOM) {
            return GUNMETAL[2];
        }
        return rib(p, 2);
    }

    /** The folded crown: the same dark polished steel as the rest of it, engraved front to back, its edges catching the light. */
    private static int crown(Px p) {
        if (p.face == TOP) {
            boolean line = p.x % 2 == 1;
            boolean edge = p.y == p.h - 1 || p.x == 0 || p.x == p.w - 1;
            return edge ? GUNMETAL[5] : grain(GUNMETAL, line ? 3 : 4, p, 0.03D);
        }
        if (p.face == BOTTOM) {
            return GUNMETAL[2];
        }
        return p.y == 0 ? GUNMETAL[5] : mix(grain(GUNMETAL, p.face == FRONT ? 3 : 2, p, 0.03D), 0x4a3c28, 0.05D);
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

    /** The eye itself: white and wet, faint veins in it, a pale green iris streaked toward its rim round a black pupil. */
    private static int orb(Px p) {
        double r = Math.hypot(p.mx, p.my);
        if (p.face == FRONT && r < 2.4D) {
            if (r < 0.9D) {
                return 0x050605;
            }
            if (r > 1.95D) {
                return 0x3a5530;
            }
            double streak = Math.sin(Math.atan2(p.my, p.mx) * 9D) * 0.15D;
            return mix(0xeef3c0, 0x9cc078, (r - 0.9D) / 1.05D + streak);
        }
        int c = grain(PEARL, 5 + (p.face == TOP ? 1 : 0), p, 0.06D);
        double vein = Math.sin(p.mx * 2.3D + Math.sin(p.my * 1.7D) * 1.5D + p.mz * 1.1D);
        return Math.abs(vein) < 0.05D && r > 2.6D ? mix(c, 0xb05050, 0.35D) : mix(c, 0xfff8f0, 0.3D);
    }

    private static int orbGlow(Px p) {
        double r = Math.hypot(p.mx, p.my);
        if (p.face == FRONT && r < 2D && r >= 0.9D) {
            return argb(0.55F, 0xd8f0a0);
        }
        return p.face == FRONT || p.side() ? argb(0.12F, 0xfffaf0) : 0;
    }

    /**
     * The Warlock's Eye's frame: dark bronze, gold where it catches the light. The mask is cut through on
     * either side with an eye-shaped hole, ridged along its top like a lower lid, glyphs along its foot;
     * the tabs are cut with runes.
     */
    private static int brass(Px p) {
        int level = 3 + (p.face == TOP ? 2 : p.face == BOTTOM ? -2 : 0);
        if (p.part("eMask") && p.face == FRONT) {
            double ax = Math.abs(p.mx);
            double hole = Math.pow((ax - 8D) / 2.8D, 2D) + Math.pow((p.my - 5.8D) / 1.2D, 2D);
            if (hole < 1D) {
                return 0x0b0805;
            }
            if (hole < 1.6D) {
                return BRASS[5];
            }
            // The rim along its top, like a lower lid, and the groove under it.
            double top = 3.4D - 4.2D * Math.pow(ax / 14D, 2D);
            if (p.my < top + 0.9D) {
                level += 2;
            } else if (p.my < top + 1.6D) {
                level -= 1;
            }
            if (p.my > 8.6D && rune(p, 2)) {
                level -= 2;
            }
            if (ax < 0.8D && p.my > 4.5D) {
                return BRASS[6];
            }
        }
        if (p.part("eTab") && p.face == FRONT) {
            if (p.y == 0 || p.y == p.h - 1 || p.x == 0 || p.x == p.w - 1) {
                level += 2;
            } else if (rune(p, 2)) {
                return BRASS[0];
            }
        }
        if (p.part("eArch") && (p.face == FRONT || p.face == TOP)) {
            level += 1;
        }
        return grain(BRASS, level, p, 0.1D);
    }

    /** The lit top of the Warlock's Eye's plinth. */
    private static int light(Px p) {
        return mix(0xfff2dc, 0xffffff, noise(p, 2) * 0.3D);
    }

    /** Surtur's skull: dark weathered stone, deep sockets, a gaping nose, and a row of teeth. */
    private static int skull(Px p) {
        if (p.face == FRONT) {
            if (p.b.h == 7 && p.b.w == 13) {
                double socket = Math.pow((Math.abs(p.mx) - 3.2D) / 2.3D, 2D) + Math.pow((p.my + 9.2D) / 1.6D, 2D);
                if (socket < 1D) {
                    return 0x060607;
                }
                if (socket < 1.6D) {
                    return CROWN_GREY[5];
                }
                if (p.my > -7.2D && p.my < -5.3D && Math.abs(p.mx) < 1.4D * (p.my + 7.2D) / 1.9D + 0.3D) {
                    return 0x0a0a0c;
                }
            }
            if (p.b.h == 5 && p.b.w == 9) {
                if (p.my < -4.2D) {
                    return CROWN_GREY[2];
                }
                boolean tooth = Math.floorMod((int) Math.floor(p.mx + 10D), 2) == 0;
                return p.my < -1.2D ? (tooth ? CROWN_GREY[5] : 0x0a0a0c) : CROWN_GREY[2];
            }
        }
        int level = 3 + (p.face == TOP ? 2 : p.face == BOTTOM ? -2 : 0);
        if (Math.abs(Math.sin(p.mx * 1.4D + p.my * 0.9D + p.mz * 1.2D)) < 0.07D) {
            level -= 2;
        }
        return grain(CROWN_GREY, level, p, 0.14D);
    }

    /** The blades and horns: the blades paler, carved deep with whorls; the horns dark, ridged along their length, pale at the tips. */
    private static int crownStone(Px p) {
        int level = 2 + (p.face == TOP ? 2 : p.face == BOTTOM ? -1 : 0);
        if (p.part("cWing") && (p.face == FRONT || p.face == TOP || p.face == BACK)) {
            double w = Math.sin(p.mx * 0.75D + Math.sin(p.my * 1.6D + p.mx * 0.3D) * 1.6D);
            double whorl = Math.abs(Math.hypot(Math.floorMod((int) Math.floor(p.mx), 7) - 3D, p.my) - 1.6D);
            if (Math.abs(w) < 0.14D || whorl < 0.35D) {
                return CROWN_GREY[2];
            }
            level = 5;
        }
        if (p.part("cHorn")) {
            level += p.x % 2 == 0 ? 0 : -1;
            if (p.b.part.startsWith("cHorn" + (VaultBoxes.CROWN_HORN - 1))) {
                level += 2;
            }
        }
        return grain(CROWN_GREY, level, p, 0.12D);
    }

    /** The Casket's frame: dark stone, its edges worn pale, carved lines following its shape. */
    private static int casket(Px p) {
        int level = 3 + (p.face == TOP ? 1 : p.face == BOTTOM ? -2 : 0);
        if (!p.cap() && p.h >= 3 && (p.y == 0 || p.x == 0 || p.x == p.w - 1)) {
            level += 1;
        }
        if (!p.cap() && p.w >= 3 && p.h >= 6 && p.x == p.w / 2) {
            level -= 2;
        }
        return grain(FRAME, level, p, 0.1D);
    }

    /** The Casket's glass: deep blue, clouded, veined through with dark knotwork. */
    private static int casketCore(Px p) {
        if (vein(p)) {
            return 0x05102a;
        }
        return mix(0x0a2458, 0x2f6cc0, noise(p, 4) * 0.5D + cloud(p) * 0.5D);
    }

    private static int casketGlow(Px p) {
        if (vein(p)) {
            return 0;
        }
        return argb(0.3F + 0.4F * (float) cloud(p), mix(0x2f86ff, 0xa8dcff, cloud(p)));
    }

    /** Where the knotwork runs across the glass: great interlaced loops, cut dark, unbroken. */
    private static boolean vein(Px p) {
        double u = p.side() ? p.mz : p.mx, v = p.cap() ? p.mz : p.my + 6.5D;
        double[][] loops = {{-4D, -1.5D, 5.2D}, {4D, -1.5D, 5.2D}, {0D, 4.5D, 5.8D}, {0D, -6.5D, 3.6D}};
        for (double[] l : loops) {
            if (Math.abs(Math.hypot(u - l[0], v - l[1]) - l[2]) < 0.48D) {
                return true;
            }
        }
        return false;
    }

    /** The cloud of light inside the glass, brightest toward the middle of each panel. */
    private static double cloud(Px p) {
        double u = p.side() ? p.mz : p.mx, v = p.cap() ? p.mz : p.my + 6.5D;
        double fall = Math.exp(-(u * u) / 40D - (v * v) / 30D);
        return Math.max(0D, Math.min(1D, fall * 0.8D + noise(p, 6) * 0.3D));
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


    // ------------------------------------------------------------------ the Destroyer Armor's paints

    /** The finish being painted over the Destroyer's own gunmetal, by its key in ArmorSkin; null for none. */
    // ------------------------------------------------------------------ the Destroyer Armor's wounds

    /**
     * Its wounds at each of three stages, every stage keeping all of the last: each mark is a field over its part's
     * own space with a threshold that only ever widens, so a dent at the first stage is still there at the third.
     */
    private static void wounds(File out) throws Exception {
        Sheet s = VaultBoxes.DESTROYER;
        for (int stage = 1; stage <= 3; stage++) {
            BufferedImage marks = new BufferedImage(s.width, s.height, BufferedImage.TYPE_INT_ARGB);
            BufferedImage light = new BufferedImage(s.width, s.height, BufferedImage.TYPE_INT_ARGB);
            for (Box b : s.all) {
                int u = b.u, v = b.v, w = b.w, h = b.h, d = b.d;
                int[][] faces = {{TOP, u + d, v, w, d}, {BOTTOM, u + d + w, v, w, d}, {RIGHT, u, v + d, d, h}, {FRONT, u + d, v + d, w, h},
                        {LEFT, u + d + w, v + d, d, h}, {BACK, u + d + w + d, v + d, w, h}};
                for (int[] f : faces) {
                    for (int y = 0; y < f[4]; y++) {
                        for (int x = 0; x < f[3]; x++) {
                            Px p = new Px(b, f[0], x, y, f[3], f[4]);
                            int c = wound(p, stage);
                            if (c != 0) {
                                marks.setRGB(f[1] + x, f[2] + y, c);
                            }
                            int g = woundGlow(p, stage);
                            if (g != 0) {
                                light.setRGB(f[1] + x, f[2] + y, g);
                            }
                        }
                    }
                }
            }
            ImageIO.write(marks, "png", new File(out, "destroyer_damage_" + stage + ".png"));
            ImageIO.write(light, "png", new File(out, "destroyer_damage_glow_" + stage + ".png"));
        }
    }

    /** Whether a box takes wounds: its plate, not the furnace in its face. */
    private static boolean scarred(Px p) {
        int m = p.b.material;
        return m != VaultBoxes.CORE && !(m == VaultBoxes.FACE && p.face == FRONT && slit(p));
    }

    /** One pixel of its wounds at a stage, as ARGB to lay over it, or 0 where it is whole. */
    private static int wound(Px p, int stage) {
        if (!scarred(p)) {
            return 0;
        }
        double salt = (p.b.part.hashCode() & 1023) * 1.37D;
        // Burst open, over the fire.
        double hole = field(p, 0.42D, salt + 11D);
        double holeAt = new double[]{2D, 2D, 0.84D}[stage - 1];
        if (hole > holeAt) {
            return hole > holeAt + 0.025D ? argb(1F, mix(0x0b0503, 0x2a0c04, noise(p, 21))) : argb(1F, mix(0x1c1610, 0x5a4a3a, noise(p, 22)));
        }
        // Cracks, wandering through it in patches that spread at every stage.
        double reach = new double[]{0.4D, 0.6D, 0.85D}[stage - 1];
        double width = new double[]{0.016D, 0.024D, 0.034D}[stage - 1];
        double crack = Math.abs(field(p, 0.33D, salt + 3D) - 0.5D);
        if (crack < width && field(p, 0.09D, salt + 5D) > 1D - reach) {
            return argb(crack < width * 0.5D ? 1F : 0.85F, crack < width * 0.5D ? 0x050202 : 0x14100d);
        }
        // Dents: a dark pit, bright at its upper rim where the light catches it.
        double dent = field(p, 0.5D, salt + 7D);
        double dentAt = new double[]{0.76D, 0.73D, 0.7D}[stage - 1];
        if (dent > dentAt) {
            boolean rim = dent < dentAt + 0.035D;
            double above = fieldAt(p.mx, p.my - 1D, p.mz, 0.5D, salt + 7D);
            if (rim) {
                return above < dent ? argb(0.5F, 0xb8bec6) : argb(0.55F, 0x050506);
            }
            return argb(0.5F, 0x060607);
        }
        // Gouges: bright scrapes across it, edged dark.
        double scrape = (p.mx + p.mz) * 0.7D - p.my * 1.1D + field(p, 0.3D, salt + 9D) * 3D;
        double frac = scrape * 0.22D - Math.floor(scrape * 0.22D);
        double gouges = new double[]{0.5D, 0.6D, 0.7D}[stage - 1];
        if (frac < 0.07D && field(p, 0.11D, salt + 13D) > 1D - gouges) {
            return frac < 0.035D ? argb(0.75F, mix(0x9aa0a8, 0xd8dde2, noise(p, 23))) : argb(0.6F, 0x08080a);
        }
        // Soot, scorched on wherever it has burned.
        double soot = field(p, 0.16D, salt + 17D);
        double sootAt = new double[]{0.6D, 0.52D, 0.42D}[stage - 1];
        if (soot > sootAt) {
            return argb((float) Math.min(0.78D, (soot - sootAt) * 3.2D), mix(0x0a0705, 0x1a0f08, noise(p, 24)));
        }
        return 0;
    }

    /** The light in its wounds: the fire showing through its deepest cracks, and through its holes. */
    private static int woundGlow(Px p, int stage) {
        if (stage < 2 || !scarred(p)) {
            return 0;
        }
        double salt = (p.b.part.hashCode() & 1023) * 1.37D;
        double hole = field(p, 0.42D, salt + 11D);
        if (stage == 3 && hole > 0.84D) {
            double depth = Math.min(1D, (hole - 0.84D) * 12D);
            return argb((float) (0.55D + 0.45D * depth), mix(0xff4a10, 0xffd070, depth * noise(p, 25)));
        }
        double width = stage == 2 ? 0.024D : 0.034D;
        double crack = Math.abs(field(p, 0.33D, salt + 3D) - 0.5D);
        double reach = stage == 2 ? 0.6D : 0.85D;
        if (crack < width * 0.5D && field(p, 0.09D, salt + 5D) > 1D - reach) {
            return argb(stage == 2 ? 0.7F : 0.95F, mix(0xff5a14, 0xffb050, noise(p, 26)));
        }
        return 0;
    }

    /** Smooth value noise in [0, 1) over a part's own space, at a frequency: the same however its boxes are packed. */
    private static double field(Px p, double freq, double salt) {
        return fieldAt(p.mx, p.my, p.mz, freq, salt);
    }

    private static double fieldAt(double x, double y, double z, double freq, double salt) {
        // Two octaves, the second half as strong.
        return (lattice(x * freq + salt, y * freq, z * freq - salt) * 2D + lattice(x * freq * 2.1D - salt, y * freq * 2.1D + salt, z * freq * 2.1D)) / 3D;
    }

    private static double lattice(double x, double y, double z) {
        int x0 = (int) Math.floor(x), y0 = (int) Math.floor(y), z0 = (int) Math.floor(z);
        double fx = smoothstep(x - x0), fy = smoothstep(y - y0), fz = smoothstep(z - z0);
        double c00 = lerp(corner(x0, y0, z0), corner(x0 + 1, y0, z0), fx), c10 = lerp(corner(x0, y0 + 1, z0), corner(x0 + 1, y0 + 1, z0), fx);
        double c01 = lerp(corner(x0, y0, z0 + 1), corner(x0 + 1, y0, z0 + 1), fx), c11 = lerp(corner(x0, y0 + 1, z0 + 1), corner(x0 + 1, y0 + 1, z0 + 1), fx);
        return lerp(lerp(c00, c10, fy), lerp(c01, c11, fy), fz);
    }

    private static double corner(int x, int y, int z) {
        return (hash(x * 73856093 ^ y * 19349663, z * 83492791) & 0xFFFF) / 65536D;
    }

    private static double smoothstep(double t) {
        return t * t * (3D - 2D * t);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static String finish;

    private static final String[] FINISHES = {"gold", "burnished", "royal", "frost", "muspel", "bifrost", "celestial", "warpaint"};
    private static final int[] GILT = {0x2a1a06, 0x4a3010, 0x6e4a18, 0x9a6c24, 0xc49434, 0xe8bc58, 0xfff0b0};
    private static final int[] BRIGHT_STEEL = {0x2a2d33, 0x3d4149, 0x565b64, 0x767c86, 0x9da3ac, 0xc4c9d0, 0xf0f3f6};
    private static final int[] ROYAL_BLUE = {0x05081a, 0x0a1230, 0x111d48, 0x1a2a62, 0x26397e, 0x3a52a0, 0x6f8ad0};
    private static final int[] RIME = {0x1c2a3c, 0x2c4058, 0x426078, 0x5f8298, 0x86a8bc, 0xb4d0de, 0xeaf6fc};
    private static final int[] CINDER = {0x050404, 0x0b0807, 0x120d0b, 0x1a1310, 0x241a15, 0x33251d, 0x4a3529};
    private static final int[] VOID_VIOLET = {0x0a0612, 0x140b22, 0x1f1134, 0x2c1848, 0x3c2160, 0x55307e, 0x8058b0};
    private static final int[] CRIMSON = {0x200404, 0x3a0808, 0x560c0c, 0x761212, 0x961c18, 0xb82c22, 0xe05040};

    /** Paints every finish of the Destroyer Armor, each its own texture, with its own glow if it has light of its own. */
    private static void finishes(File out) throws Exception {
        for (String key : FINISHES) {
            finish = key;
            boolean glows = "muspel".equals(key) || "celestial".equals(key);
            paint(VaultBoxes.DESTROYER, new File(out, "destroyer_" + key + ".png"), glows ? new File(out, "destroyer_" + key + "_glow.png") : null);
        }
        finish = null;
    }

    /** One pixel of the Destroyer, repainted in the finish: by how bright its own gunmetal is there, and what it is part of. */
    private static int finish(Px p, int c) {
        int m = p.b.material;
        // The furnace inside it and the hollow of its back are never painted.
        if (m == VaultBoxes.CORE || m == VaultBoxes.HOLLOW) {
            return c;
        }
        double t = Math.max(0D, Math.min(1D, (luminance(c) - 0.03D) / 0.62D));
        boolean trim = m == VaultBoxes.BAND || m == VaultBoxes.STUD || m == VaultBoxes.CROWN || m == VaultBoxes.SPIKE;
        switch (finish) {
            case "gold":
                return ramp(GILT, 0.08D + 0.92D * t);
            case "burnished":
                return ramp(BRIGHT_STEEL, 0.15D + 0.85D * t);
            case "royal":
                return gilded(p, t) ? ramp(GILT, 0.15D + 0.85D * t) : ramp(ROYAL_BLUE, Math.min(1D, t * 1.15D));
            case "frost":
                return noise(p, 9) > 0.975D ? 0xf4fcff : ramp(RIME, 0.1D + 0.9D * t);
            case "muspel":
                return crack(p) ? mix(0x8a2408, 0xffa040, noise(p, 4)) : ramp(CINDER, t);
            case "bifrost":
                return trim ? hsv(rainbow(p), 0.72D, 0.22D + 0.78D * t) : c;
            case "celestial":
                if (star(p)) {
                    return mix(0xffe39a, 0xfff8e0, noise(p, 5));
                }
                return gilded(p, t) ? ramp(GILT, 0.1D + 0.8D * t) : ramp(VOID_VIOLET, Math.min(1D, t * 1.15D));
            case "warpaint":
                return warpaint(p, trim) && noise(p, 11) > 0.12D ? ramp(CRIMSON, 0.1D + 0.9D * t) : c;
            default:
                return c;
        }
    }

    /** Light of a finish's own, burning as the furnace does: Muspelheim's cracks, the Celestial's stars. */
    private static int finishGlow(Px p) {
        if ("muspel".equals(finish) && crack(p)) {
            return argb(0.9F, 0xff7a20);
        }
        if ("celestial".equals(finish) && star(p)) {
            return argb(0.75F, 0xffe6a0);
        }
        return 0;
    }

    /** Picked out in gold: its studs, crown and spikes whole, and the bright lip along the top of every band. */
    private static boolean gilded(Px p, double t) {
        int m = p.b.material;
        return m == VaultBoxes.STUD || m == VaultBoxes.CROWN || m == VaultBoxes.SPIKE || m == VaultBoxes.BAND && t > 0.62D;
    }

    /** Wandering molten cracks through its plates. */
    private static boolean crack(Px p) {
        int m = p.b.material;
        if (m == VaultBoxes.CORE || m == VaultBoxes.HOLLOW || m == VaultBoxes.FACE || m == VaultBoxes.SPIKE || m == VaultBoxes.CROWN
                || m == VaultBoxes.BAND || m == VaultBoxes.STUD) {
            return false;
        }
        double salt = (p.b.part.hashCode() & 7) * 0.9D;
        double v = Math.sin(p.mx * 0.9D + p.my * 0.45D + salt) + Math.sin(p.mz * 0.8D - p.my * 0.6D + salt * 2D) + (noise(p, 6) - 0.5D) * 0.25D;
        return Math.abs(v) < 0.075D;
    }

    /** A scatter of gold stars over its plates, never on its trim. */
    private static boolean star(Px p) {
        int m = p.b.material;
        return m != VaultBoxes.BAND && m != VaultBoxes.STUD && m != VaultBoxes.CROWN && m != VaultBoxes.SPIKE && m != VaultBoxes.FACE
                && m != VaultBoxes.CORE && m != VaultBoxes.HOLLOW && noise(p, 13) > 0.972D;
    }

    /** Where its war-stripes are daubed: every third band, and slashes across its chest, shoulders and thighs. */
    private static boolean warpaint(Px p, boolean trim) {
        if (trim) {
            return p.b.material == VaultBoxes.BAND && hash(p.b.u, p.b.v) % 3 == 0;
        }
        boolean marked = p.part("chest") || p.part("shoulder") || p.part("thigh") || p.part("upper");
        return marked && p.b.material == VaultBoxes.GROOVE && Math.floorMod((int) Math.floor(p.mx + p.my + p.mz), 7) == 0;
    }

    /** Where in the rainbow a band of the Bifrost finish falls: red at its feet, through to violet at its crown. */
    private static double rainbow(Px p) {
        double y = partHeight(p.b.part) + p.my;
        return Math.max(0D, Math.min(0.8D, (24D - y) / 44D));
    }

    /** Roughly how high a part's own origin sits on the standing Destroyer, in model units (down is +y, its feet at 24). */
    private static double partHeight(String part) {
        String[][] heights = {{"shin", "18"}, {"foot", "25"}, {"thigh", "11"}, {"pelvis", "11"}, {"abdomen", "10"}, {"chest", "3.5"},
                {"back", "-2"}, {"trap", "-8.9"}, {"neck", "-5.3"}, {"head", "-7.3"}, {"face", "-7.3"}, {"crown", "-16.5"},
                {"core", "-7.3"}, {"shoulder", "-4"}, {"upper", "-1.5"}, {"fore", "6.5"}, {"fist", "14.5"}};
        for (String[] h : heights) {
            if (part.startsWith(h[0])) {
                return Double.parseDouble(h[1]);
            }
        }
        return 4D;
    }

    private static double luminance(int c) {
        return (0.299D * ((c >> 16) & 0xFF) + 0.587D * ((c >> 8) & 0xFF) + 0.114D * (c & 0xFF)) / 255D;
    }

    /** A colour along a seven-step ramp, between its steps as well as on them. */
    private static int ramp(int[] ramp, double t) {
        double at = Math.max(0D, Math.min(1D, t)) * (ramp.length - 1);
        int i = Math.min(ramp.length - 2, (int) Math.floor(at));
        return mix(ramp[i], ramp[i + 1], at - i);
    }

    private static int hsv(double h, double s, double v) {
        int rgb = java.awt.Color.HSBtoRGB((float) h, (float) s, (float) Math.max(0D, Math.min(1D, v)));
        return rgb & 0xFFFFFF;
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
