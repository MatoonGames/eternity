import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * The Gardener's plants and his hedge, drawn by hand a texel at a time:
 * <ul>
 * <li><b>strange_bloom_glass_bell.png</b>: a pale arching stem hung with three bells of blue glass,
 * a clapper of gold in each.</li>
 * <li><b>strange_bloom_ember_fern.png</b>: fronds fanning from a dark red heart, orange going to
 * yellow at the tips, as if lit from inside.</li>
 * <li><b>strange_bloom_lantern_head.png</b>: two stalks off a rosette, each with a pale globe on it,
 * yellow at the core.</li>
 * <li><b>strange_bloom_blackleaf.png</b>: broad leaves of violet so dark it is nearly black, every
 * vein silver.</li>
 * <li><b>garden_thorn.png</b>: black-green stems as thick as an arm, woven, pale thorns off them,
 * light showing through the gaps.</li>
 * <li><b>garden_thorn_frozen.png</b>: the same hedge under the Casket's winter: the stems gone pale
 * blue-white, frost grown across the gaps, the thorns rimed.</li>
 * <li><b>garden_thorn_dead.png</b>: the same hedge with the life drunk out of it by the Soul Stone:
 * grey-brown and split, half of it already fallen away to dust.</li>
 * </ul>
 * Usage: javac -d out tools/GardenArt.java, then java -cp out GardenArt src/main/resources/assets/eternity/textures/blocks
 */
public class GardenArt {

    private static final String[] GLASS_BELL = {
            "................",
            ".......ssss.....",
            ".....ss....ss...",
            "....s........s..",
            "...s..........s.",
            "..bBb........s..",
            ".bBwbb......bBb.",
            ".bbbbb.....bBwbb",
            ".rrrrr.....bbbbb",
            "...k..s....rrrrr",
            "......s......k..",
            "......s.bBb.....",
            "......sbBwbb....",
            "......sbbbbb....",
            ".....SsrrrrrS...",
            "....ss.s.k..ss..",
    };
    private static final String[] EMBER_FERN = {
            "................",
            "..y.........y...",
            "...o...y...o....",
            ".y..o..o..o..y..",
            "..o..o.o.o..o...",
            "...oo.oOo.oo....",
            ".y...ooOoo...y..",
            "..oo..oOo..oo...",
            "....oo.O.oo.....",
            "..y...rOr...y...",
            "...oo.rOr.oo....",
            ".....orRro......",
            "......rRr.......",
            ".......r........",
            "......rRr.......",
            ".....r.r.r......",
    };
    private static final String[] LANTERN_HEAD = {
            "................",
            "...wWw..........",
            "..wWYWw.........",
            "..wYYYw...wWw...",
            "..wWYWw..wWYWw..",
            "...wWw...wYYYw..",
            "....g....wWYWw..",
            "....g.....wWw...",
            "....g......g....",
            "....gg.....g....",
            ".....g....g.....",
            "..l...g..gg.l...",
            ".lLl..g.g..lLl..",
            "..lLl.ggg.lLl...",
            "...llLgggLll....",
            "....lllgglll....",
    };
    private static final String[] BLACKLEAF = {
            "................",
            ".......vv.......",
            "......vkkv......",
            ".....vkSKkv.....",
            "..vv.vkSKkv.vv..",
            ".vkKvvkSkv.vKkv.",
            ".vkSKkvSvvkKSkv.",
            "..vkSkkSkkkSkv..",
            "...vkkSSSkkkv...",
            "....vvkSkkvv....",
            "..vvkKkSkKkvv...",
            ".vkSSkkSkkSSkv..",
            "..vvkkkSkkkvv...",
            "....vvvSvvv.....",
            ".......S........",
            "......sSs.......",
    };
    private static final String[] THORN = {
            "d..sd...t.ds..s.",
            "ss.sS..ss.sS.ss.",
            ".sSs.ssd..Sss.d.",
            "..sssSs.tsss....",
            "t..sSsd.sss..sst",
            ".ss.sSsssS..sSs.",
            "ssS..sSs.dssss..",
            "sSs..t.sSs.sS..t",
            ".sdst..sSss.sss.",
            "...sSsss.sSs.sSs",
            ".t.ssSs..ssS..ds",
            "sss.dssS.t.sss..",
            "sSss..sSs..sSss.",
            ".ssSs.tsds.ssSs.",
            "..sss..sSs..sss.",
            "t.sSs.ss.sss.sSs",
    };

    public static void main(String[] args) throws Exception {
        File out = new File(args[0]);
        out.mkdirs();
        Map<Character, Integer> bell = palette('s', 0x4E7E46, 'S', 0x6E9A56, 'b', 0x8ED4E2, 'B', 0xC8F0F6, 'w', 0xFFFFFF, 'r', 0x3E8AA2, 'k', 0xE8D07A);
        Map<Character, Integer> fern = palette('y', 0xFFDC64, 'o', 0xEE8628, 'O', 0xFFB040, 'r', 0x7E2412, 'R', 0xB0401C);
        Map<Character, Integer> lantern = palette('w', 0xE2DCC4, 'W', 0xFFFDF0, 'Y', 0xFFF0A0, 'g', 0x5A7A3A, 'l', 0x2C5624, 'L', 0x4A7A3A);
        Map<Character, Integer> black = palette('v', 0x120A1C, 'k', 0x2A1A3A, 'K', 0x3E2A56, 'S', 0xBCC4D4, 's', 0x1E1428);
        Map<Character, Integer> thorn = palette('s', 0x22301A, 'S', 0x3E5828, 'd', 0x121A0E, 't', 0xD6CEA6);
        write(out, "strange_bloom_glass_bell", GLASS_BELL, bell);
        write(out, "strange_bloom_ember_fern", EMBER_FERN, fern);
        write(out, "strange_bloom_lantern_head", LANTERN_HEAD, lantern);
        write(out, "strange_bloom_blackleaf", BLACKLEAF, black);
        write(out, "garden_thorn", THORN, thorn);
        write(out, "garden_thorn_frozen", frozen(THORN), palette('s', 0x6C98B8, 'S', 0xB8DAF0, 'd', 0x2E5272, 't', 0xFFFFFF, 'i', 0xE4F4FF));
        write(out, "garden_thorn_dead", dead(THORN), palette('s', 0x6A5C4C, 'S', 0x8E806C, 'd', 0x2A231C, 't', 0x9A9284));
    }

    /** Frost grown across the gaps between the stems: every third gap, in a slant, so it reads as rime and not as a wall. */
    private static String[] frozen(String[] rows) {
        String[] out = new String[rows.length];
        for (int y = 0; y < rows.length; y++) {
            StringBuilder b = new StringBuilder(rows[y]);
            for (int x = 0; x < b.length(); x++) {
                if (b.charAt(x) == '.' && (x + y * 2) % 3 == 0) {
                    b.setCharAt(x, 'i');
                }
            }
            out[y] = b.toString();
        }
        return out;
    }

    /** Dead and falling apart: the thorns gone, and the stems broken off in runs where they rotted through. */
    private static String[] dead(String[] rows) {
        String[] out = new String[rows.length];
        for (int y = 0; y < rows.length; y++) {
            StringBuilder b = new StringBuilder(rows[y]);
            for (int x = 0; x < b.length(); x++) {
                char c = b.charAt(x);
                if (c == 't' || (x * 7 + y * 5) % 13 == 0) {
                    b.setCharAt(x, '.');
                }
            }
            out[y] = b.toString();
        }
        return out;
    }

    private static Map<Character, Integer> palette(Object... pairs) {
        Map<Character, Integer> m = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            m.put((Character) pairs[i], (Integer) pairs[i + 1]);
        }
        return m;
    }

    private static void write(File out, String name, String[] rows, Map<Character, Integer> pal) throws Exception {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) {
            if (rows[y].length() != 16) {
                throw new IllegalStateException(name + " row " + y + " is " + rows[y].length() + " wide");
            }
            for (int x = 0; x < 16; x++) {
                char c = rows[y].charAt(x);
                if (c == '.') {
                    continue;
                }
                Integer rgb = pal.get(c);
                if (rgb == null) {
                    throw new IllegalStateException(name + ": no colour for '" + c + "'");
                }
                img.setRGB(x, y, 0xFF000000 | rgb);
            }
        }
        ImageIO.write(img, "png", new File(out, name + ".png"));
    }
}
