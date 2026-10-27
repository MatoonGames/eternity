import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Paints the Destroyer Armor's upgrade icons, one 16x16 picture for each upgrade on its Forge page,
 * in the order of {@code ArmorUpgrade}, left to right along the top of
 * {@code textures/gui/destroyer_icons.png}. Each is drawn by hand below, a character a pixel, in the
 * Destroyer's own dark banded steel, Uru, gold and the orange of its fire.
 * <p>
 * Run: {@code javac -d /tmp/ai tools/ArmorIcons.java && java -cp /tmp/ai ArmorIcons src/main/resources/assets/eternity/textures/gui}
 */
public final class ArmorIcons {

    private static final Map<Character, Integer> PALETTE = new HashMap<>();

    static {
        // Steel, darkest to brightest.
        PALETTE.put('k', 0x0b0b0e);
        PALETTE.put('d', 0x1d1f24);
        PALETTE.put('s', 0x2b2e35);
        PALETTE.put('m', 0x41454e);
        PALETTE.put('l', 0x666c76);
        PALETTE.put('w', 0xa8aeb7);
        PALETTE.put('W', 0xeef1f4);
        // Uru.
        PALETTE.put('u', 0x7890b2);
        PALETTE.put('U', 0xc0d4ec);
        // Fire.
        PALETTE.put('O', 0xa8380c);
        PALETTE.put('o', 0xff7a20);
        PALETTE.put('y', 0xffcc4a);
        PALETTE.put('Y', 0xfff4c4);
        // Gold.
        PALETTE.put('G', 0x8a6a3a);
        PALETTE.put('g', 0xd8b070);
        // Wind.
        PALETTE.put('c', 0x9fe0ff);
        // Forge Energy.
        PALETTE.put('R', 0xc8202a);
        // The stones.
        PALETTE.put('b', 0x3f7bff);
        PALETTE.put('n', 0xff8a1e);
        PALETTE.put('r', 0xff2a2a);
        PALETTE.put('e', 0x2ee86a);
        PALETTE.put('p', 0xb040ff);
        PALETTE.put('P', 0xe2b8ff);
        PALETTE.put('i', 0xffe03a);
    }

    private static final String[][] ICONS = {
            // Uru Plating: a banded plate of Uru, riveted.
            {"................",
             "..kkkkkkkkkkkk..",
             ".kUUUUUUUUUUUUk.",
             ".kuWuuuuuuuuWuk.",
             ".kuuuuuuuuuuuuk.",
             ".kkkkkkkkkkkkkk.",
             ".kUUUUUUUUUUUUk.",
             ".kuuuuuuuuuuuuk.",
             ".kkkkkkkkkkkkkk.",
             "..kUUUUUUUUUUk..",
             "..kuWuuuuuuWuk..",
             "..kkkkkkkkkkkk..",
             "...kUUUUUUUUk...",
             "...kuuuuuuuuk...",
             "....kkkkkkkk....",
             "................"},
            // Banded Hull: its torso, the pecs split down the middle, banded down to the waist.
            {"................",
             ".kkkkk....kkkkk.",
             "kwlllmkkkkmlllwk",
             "klmmmmskksmmmmlk",
             "kkkkkkkkkkkkkkkk",
             ".kwllllkkllllwk.",
             ".klmmmskksmmmlk.",
             ".kkkkkkkkkkkkkk.",
             "..kwlllkklllwk..",
             "..klmmskksmmlk..",
             "..kkkkkkkkkkkk..",
             "...kwllllllwk...",
             "...klmmmmmmlk...",
             "...kkkkkkkkkk...",
             "....kwllllwk....",
             "....kkkkkkkk...."},
            // Odin's Wards: a rune of protection burning gold on a dark round plate.
            {"................",
             ".....kkkkkk.....",
             "...kkddddddkk...",
             "..kdgddggddgdk..",
             ".kdddgdggdgdddk.",
             ".kddddggggddddk.",
             "kddddddggddddddk",
             "kddddddggddddddk",
             "kddddddggddddddk",
             "kddddddggddddddk",
             ".kdddddggdddddk.",
             ".kdddddggdddddk.",
             "..kddddggddddk..",
             "...kkddddddkk...",
             ".....kkkkkk.....",
             "................"},
            // Deep Furnace: a furnace with a deep well of lava in it.
            {"................",
             ".kkkkkkkkkkkkkk.",
             ".kwlllllllllllk.",
             ".klmmmmmmmmmmsk.",
             ".klkkkkkkkkkksk.",
             ".klkOOOOOOOOksk.",
             ".klkoooooooOksk.",
             ".klkoyoooyooksk.",
             ".klkooyoyoooksk.",
             ".klkoooyooooksk.",
             ".klkoooooooOksk.",
             ".klkkkkkkkkkksk.",
             ".kwlmlmlmlmlmsk.",
             ".ksssssssssssdk.",
             ".kkkkkkkkkkkkkk.",
             "................"},
            // Bellows: air driven into a flame.
            {"................",
             "................",
             "................",
             "..........k.....",
             ".........kok....",
             ".........kok....",
             "........koyok...",
             "..ccc...koyok...",
             ".......koyyyok..",
             ".cccccWkoyYyok..",
             ".......koyYyok..",
             "..ccc..kooyook..",
             "........kOOOk...",
             ".........kkk....",
             "................",
             "................"},
            // Disintegrator: its face open, the ray pouring out of it.
            {"................",
             "kk....kk........",
             "kmk..kmk........",
             "kmmkkmmk........",
             "kwlllllk........",
             ".kssssk.........",
             ".kslslk.....oooo",
             ".kooyokoooyyyyyy",
             ".koyYykyyyYYYWWW",
             ".kooyokoooyyyyyy",
             ".kslslk.....oooo",
             ".kslslk.........",
             ".kslslk.........",
             "..kssk..........",
             "...kk...........",
             "................"},
            // Molten Heart: its core, white hot, bursting.
            {"................",
             ".......yy.......",
             "..y....oo....y..",
             "...o..kkkk..o...",
             ".....kOooOk.....",
             "....kOoyyoOk....",
             "....koyYYyok....",
             "yoo.kyYWWYyk.ooy",
             "yoo.kyYWWYyk.ooy",
             "....koyYYyok....",
             "....kOoyyoOk....",
             ".....kOooOk.....",
             "...o..kkkk..o...",
             "..y....oo....y..",
             ".......yy.......",
             "................"},
            // Hydraulic Pistons: the piston in its leg, driving.
            {"................",
             "....kkkkkkkk....",
             "....kwllllmk....",
             "....klmmmmsk....",
             "....klmmmmsk....",
             "....kUuuuuuk....",
             "....klmmmmsk....",
             "....kkkkkkkk....",
             "......kwlk..o...",
             "......kwlk.ooo..",
             "......kwlk..o...",
             "...kkkkkkkkkk...",
             "...kwllllllmk...",
             "...ksmmmmmmsk...",
             "...kkkkkkkkkk...",
             "................"},
            // Leap Jets: its foot, fire pouring from its heel and toe.
            {"................",
             "....kkkkkk......",
             "....kwllmk......",
             "....klmmsk......",
             "....klmmsk......",
             "....kwllmkkkk...",
             "....klmmmmmlk...",
             "...kwlllllllmk..",
             "...kkkkkkkkkkk..",
             "....koyok.koyok.",
             "....koYok.koYok.",
             ".....kyk...kyk..",
             ".....kok...kok..",
             "......o.....o...",
             "......O.....O...",
             "................"},
            // Thunderstep: its foot coming down and the ground splitting molten under it.
            {"................",
             "......kkkkk.....",
             "......kwlmk.....",
             "......klmsk.....",
             "...y..klmsk..y..",
             "....y.kwlmkkk.y.",
             "....y.klmmmlk.y.",
             "...kwllllllllk..",
             "...kkkkkkkkkkk..",
             "msmmsmkOOkmsmmsm",
             "dddddkOooOkddddd",
             "ddddkOoyyoOkdddd",
             "dddkOoyYYyoOkddd",
             "dddddddddddddddd",
             "kkkkkkkkkkkkkkkk",
             "................"},
            // Uru Knuckles: its fist, Uru across the knuckles and the wrist.
            {"................",
             "................",
             "...kkkkkkkkk....",
             "..kUUkUUkUUkk...",
             "..kuukuukuukUk..",
             "..kkkkkkkkkkuk..",
             "..kwllllllllsk..",
             "..klmmmmmmmmsk..",
             "..klmmmmmmmmsk..",
             "..klmkkkkkkmsk..",
             "..klmmmmmmmmsk..",
             "...ksssssssk....",
             "...kuUUUUUuk....",
             "...ksssssssk....",
             "...kkkkkkkkk....",
             "................"},
            // Shockwave Slam: both fists down, rings racing out across the ground.
            {"................",
             "..kkkkk..kkkkk..",
             "..kwlmk..kwlmk..",
             "..klmsk..klmsk..",
             "..kkkkk..kkkkk..",
             "..kwlmk..kwlmk..",
             "..klmsk..klmsk..",
             "..kkkkk..kkkkk..",
             "................",
             "...oooooooooo...",
             ".oo..........oo.",
             "o..yyyyyyyyyy..o",
             "..y..........y..",
             ".y....YYYY....y.",
             "mmmmmmmmmmmmmmmm",
             "................"},
            // Hammerfall: its fist wreathed in fire.
            {"..o....o....o...",
             ".oyo..oyo..oyo..",
             ".oyo..oyo..oyo..",
             "..kkkkkkkkkkk...",
             "..kwlkwlkwlkk...",
             "..klmklmklmkk...",
             "..kkkkkkkkkkok..",
             "..kwllllllllok..",
             "..klmmmmmmmmok..",
             "..klmkkkkkkmok..",
             "..klmmmmmmmmok..",
             "..kssssssssssk..",
             "...kOoooooooOk..",
             "....kOyyyyyOk...",
             ".....kkkkkkk....",
             "................"},
            // Infinity Cradle: six empty sockets ringed in gold, set in a plate of its chest.
            {"....kkkkkkkk....",
             "..kklllllllmkk..",
             ".kllmmmGGGmmmsk.",
             ".klmmmmGdGmmmmk.",
             "klmmmmmGGGmmmmsk",
             "kmGGGmmmmmmGGGsk",
             "kmGdGmmmmmmGdGsk",
             "kmGGGmmmmmmGGGsk",
             "kmmmmmmmmmmmmmsk",
             "kmGGGmmmmmmGGGsk",
             "kmGdGmmmmmmGdGsk",
             "kmGGGmmmmmmGGGsk",
             "ksmmmmmGGGmmmmsk",
             ".ksmmmmGdGmmmsk.",
             "..kkssmGGGsskk..",
             "....kkkkkkkk...."},
            // Stone Conduits: the six stones' power running in to one burning core.
            {"................",
             ".b.....ii.....r.",
             "..l....ll....l..",
             "...l...ll...l...",
             "....l..ll..l....",
             ".....kkkkkk.....",
             ".....kPWWPk.....",
             "lllllkWWWWklllll",
             "lllllkWWWWklllll",
             ".....kPWWPk.....",
             ".....kkkkkk.....",
             "....l..ll..l....",
             "...l...ll...l...",
             "..l....ll....l..",
             ".e.....nn.....p.",
             "................"},
            // Ascension: the Destroyer rising on fire.
            {"................",
             "..o..........o..",
             ".o.o..kkkk..o.o.",
             "o...o.kook.o...o",
             "....kkkkkkkk....",
             "...kmllllllmk...",
             "...km.kllk.mk...",
             "...km.kmmk.mk...",
             "...kk.kmmk.kk...",
             "......kkkk......",
             ".....kmk.kmk....",
             ".....kok.kok....",
             ".....yoy.yoy....",
             "......y...y.....",
             "......o...o.....",
             "................"},
            // The Infinite Destroyer: all six stones around a burning star.
            {"................",
             ".......bb.......",
             ".......bb.......",
             "....G......G....",
             "..pp........nn..",
             "..pp...gg...nn..",
             "......gYYg......",
             ".G...gYWWYg...G.",
             ".G...gYWWYg...G.",
             "......gYYg......",
             "..ee...gg...rr..",
             "..ee........rr..",
             "....G......G....",
             ".......ii.......",
             ".......ii.......",
             "................"},
            // The Dynamo: a banded steel cell, a bolt of energy through it where its furnace was.
            {"................",
             "......kkkk......",
             "......mwwm......",
             "...kkkkkkkkkk...",
             "...kmmmmmmmmk...",
             "...kRRRRRyRRk...",
             "...ksssssyysk...",
             "...ksssssyyok...",
             "...ksssyyyosk...",
             "...kssyyyyssk...",
             "...ksoyyssssk...",
             "...kRRyRRRRRk...",
             "...ksyssssssk...",
             "...kmmmmmmmmk...",
             "...kkkkkkkkkk...",
             "................"},
            // Overcharge: a bolt of energy, white-hot, between two rising chevrons.
            {"................",
             "........Yy......",
             ".......YyO......",
             "..R...Yyo....R..",
             ".RRR.Yyo....RRR.",
             "RR.RYyyyyy.RR.RR",
             ".....yyyyYy.....",
             "..R....oyY....R.",
             ".RRR..oyY....RRR",
             "RR.RR.yY....RR.R",
             "......y.........",
             ".....o..........",
             "....o...........",
             "................",
             "................",
             "................"},
    };

    /** The tabs of its systems menu, from index {@value #TAB_AT}: Status, Supply, Forge, Cradle, Overcharge, Paint. */
    private static final int TAB_AT = 32;
    private static final String[][] TABS = {
            // Status: its head, the fire behind the slit of its face.
            {"................",
             ".....kkkkkk.....",
             "....kmllllmk....",
             "...kmlwwwwlmk...",
             "...klmmmmmmlk...",
             "..kkmsssssskkk..",
             "..kmkkkkkkkkmk..",
             "..kmkOooyooOkm..",
             "..kmkkkkkkkkmk..",
             "..kmssmssmssmk..",
             "..kkmsmssmsmkk..",
             "...kmsmssmsmk...",
             "...kkmmmmmmkk...",
             "....kkssssk.k...",
             ".....kkkkkk.....",
             "................"},
            // Supply: lava poured into a banded tank, and an Uru plate.
            {"......kk........",
             "......ok........",
             "......yo........",
             "......oO........",
             "..kkkkkkkkkk....",
             "..kmmmmmmmmk....",
             "..kssssssssk....",
             "..kOoooooyOk....",
             "..kooyooooOk.kkk",
             "..kkkkkkkkkkkUUk",
             "..kOooyooooOkuuk",
             "..koooooyooOkkkk",
             "..kOoyooooooUUUk",
             "..kmmmmmmmmkuuuk",
             "..kkkkkkkkkkkkkk",
             "................"},
            // Forge: a hammer over an anvil, sparks flying.
            {"............y...",
             "..kkkkk...y...Y.",
             "..klwwk.....y...",
             "..kllmkk.Y......",
             "..kkkkmGk.......",
             "......kGGk......",
             ".......kGGk.....",
             "........kGk.....",
             "..kkkkkkkkkkkk..",
             ".kwwwwwwwwwwlmk.",
             ".kkllllllllmmkk.",
             "....kkmmmmkk....",
             ".....kmssmk.....",
             "...kkmmssmmkk...",
             "..kmmmmmmmmmmk..",
             "..kkkkkkkkkkkk.."},
            // Cradle: the six stones arched over the Regulator.
            {"................",
             ".....rr..ee.....",
             "...nn.GGGG.pp...",
             "...nnG....Gpp...",
             "..GG........GG..",
             ".bb..........ii.",
             ".bbG...kk...Gii.",
             "..G...kPPk...G..",
             "......kPpk......",
             ".....kPpppk.....",
             ".....kpppPk.....",
             "......kppk......",
             ".......kk.......",
             "................",
             "................",
             "................"},
            // Overcharge: a dial, its needle thrown into the red.
            {"................",
             ".....kkkkkk.....",
             "...kkmmmmmmkk...",
             "..kmssssssRRmk..",
             ".kmsgssssRRRRmk.",
             ".kssssssssRRYRk.",
             "kmsgsssssssYRRmk",
             "kmssssssssYsRRmk",
             "kmsssssssYssssmk",
             "kmsgssssYsssgsmk",
             "kmssssskWksssssk",
             ".kssssskkksssmk.",
             ".kmsssssssssmmk.",
             "..kmmmmmmmmmmk..",
             "...kkkkkkkkkk...",
             "................"},
            // Paint: a brush laying down a stroke of old gold.
            {"...........kk...",
             "..........kGGk..",
             ".........kGGk...",
             "........kGGk....",
             ".......kGGk.....",
             "......kmmk......",
             ".....kmlmk......",
             "....kwwlk.......",
             "...kgggk........",
             "..kgggk.........",
             "..kggk..........",
             "...kk...........",
             "................",
             ".ggggggggggggg..",
             "..GGGGGGGGGGGGG.",
             "................"},
    };

    /** Its overcharged parts, from index {@value #PART_AT}: Beam, Jets, Fists, Plating. */
    private static final int PART_AT = 48;
    private static final String[][] PARTS = {
            // Beam: the ray pouring out of its face.
            {"................",
             "................",
             "kkkk............",
             "kmmk.......oO...",
             "kmsk....ooyyyoO.",
             "kmskooyyyYYYYyyo",
             "kmkyyYYWWWWWWYYy",
             "kmkyYWWWWWWWWWWY",
             "kmkyyYYWWWWWWYYy",
             "kmskooyyyYYYYyyo",
             "kmsk....ooyyyoO.",
             "kmmk.......oO...",
             "kkkk............",
             "................",
             "................",
             "................"},
            // Jets: twin flames driving down from its heels.
            {"................",
             "...kkkk..kkkk...",
             "...kmmk..kmmk...",
             "...kssk..kssk...",
             "..kkkkkkkkkkkk..",
             "...kmmk..kmmk...",
             "...cWWc..cWWc...",
             "...cWYc..cYWc...",
             "...yYYy..yYYy...",
             "...yYyy..yyYy...",
             "...oyyo..oyyo...",
             "...oyo....oyo...",
             "...Oo.o..o.oO...",
             "....o......o....",
             "...O........O...",
             "................"},
            // Fists: a gauntleted fist, sparks off its knuckles.
            {"................",
             "..Y..........y..",
             "....kkkkkkkk....",
             "y..kwwlwwlwwlk..",
             "...klmmlmmlmmk.Y",
             "...kkkkkkkkkkk..",
             "...kwlmmmmmmmk..",
             "..kwlkkkkkkkmk..",
             "..klmmmmmmmmmk..",
             "..kmmssssssmmk..",
             "...kmssssssmk...",
             "...kkmmmmmmkk...",
             "....kmssssmk....",
             "....kmssssmk....",
             "....kkkkkkkk....",
             "................"},
            // Plating: a banded plate with charge running through it.
            {"................",
             "..kkkkkkkkkkkk..",
             "..kUUUUUUUUUUk..",
             "..kuuuuucuuuuk..",
             "..kkkkkkckkkkk..",
             "..kUUUUUcWUUUk..",
             "..kuuucWcuuuuk..",
             "..kkkkcWkkkkkk..",
             "...kUUcWcUUUk...",
             "...kuuuWcuuuk...",
             "...kkkkcWkkkk...",
             "....kUUUcUUk....",
             "....kuuucuuk....",
             ".....kkkkkk.....",
             "................",
             "................"},
    };

    public static void main(String[] args) throws Exception {
        File out = new File(args[0]);
        out.mkdirs();
        BufferedImage sheet = new BufferedImage(256, 64, BufferedImage.TYPE_INT_ARGB);
        paint(sheet, ICONS, 0);
        paint(sheet, TABS, TAB_AT);
        paint(sheet, PARTS, PART_AT);
        ImageIO.write(sheet, "png", new File(out, "destroyer_icons.png"));
    }

    /** Each of a set of icons onto the sheet, from a place along it. */
    private static void paint(BufferedImage sheet, String[][] icons, int from) {
        for (int n = 0; n < icons.length; n++) {
            String[] rows = icons[n];
            int i = from + n;
            if (rows.length != 16) {
                throw new IllegalStateException("icon " + i + " has " + rows.length + " rows");
            }
            for (int y = 0; y < 16; y++) {
                if (rows[y].length() != 16) {
                    throw new IllegalStateException("icon " + i + " row " + y + " is " + rows[y].length() + " wide: " + rows[y]);
                }
                for (int x = 0; x < 16; x++) {
                    char c = rows[y].charAt(x);
                    if (c == '.') {
                        continue;
                    }
                    Integer rgb = PALETTE.get(c);
                    if (rgb == null) {
                        throw new IllegalStateException("icon " + i + " has no colour for '" + c + "'");
                    }
                    sheet.setRGB((i % 16) * 16 + x, (i / 16) * 16 + y, 0xFF000000 | rgb);
                }
            }
        }
    }
}
