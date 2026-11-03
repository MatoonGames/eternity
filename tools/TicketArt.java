import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * The Champion's ticket, drawn by hand a texel at a time: champion_ticket.png, a stub of old ivory card
 * torn down its left edge, banded in his violet top and bottom, two lines of Colosseum print, and his
 * mark stamped through it in gold: a fist in a ring.
 * <p>
 * Usage: javac -d out tools/TicketArt.java, then java -cp out TicketArt src/main/resources/assets/eternity/textures/items
 */
public class TicketArt {

    private static final String[] TICKET = {
            "................",
            "................",
            "................",
            ".oooooooooooooo.",
            "oVvvvvvvvvvvvvo.",
            ".oppppppppgggpo.",
            "oppkkkkppgGGGgo.",
            ".oppppppPgGkGgo.",
            "oppkkkppPgGGGgo.",
            ".opppppPPpgggpo.",
            "oPppppPPPpppppo.",
            ".ovvvvvvvvvvvVo.",
            ".oooooooooooooo.",
            "................",
            "................",
            "................",
    };

    public static void main(String[] args) throws Exception {
        File out = new File(args[0]);
        out.mkdirs();
        Map<Character, Integer> pal = new HashMap<>();
        pal.put('o', 0x3A2A1E);
        pal.put('p', 0xE8DCC0);
        pal.put('P', 0xCDBE9C);
        pal.put('v', 0x5A2A82);
        pal.put('V', 0x8048B0);
        pal.put('g', 0xE0B040);
        pal.put('G', 0x9A7020);
        pal.put('k', 0x2A1A30);
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) {
            if (TICKET[y].length() != 16) {
                throw new IllegalStateException("row " + y + " is " + TICKET[y].length() + " wide");
            }
            for (int x = 0; x < 16; x++) {
                char c = TICKET[y].charAt(x);
                if (c != '.') {
                    img.setRGB(x, y, 0xFF000000 | pal.get(c));
                }
            }
        }
        ImageIO.write(img, "png", new File(out, "champion_ticket.png"));
    }
}
