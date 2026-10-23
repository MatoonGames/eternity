import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Draws the 16x16 icons for Eternity's abilities: a void-dark disc rimmed in gold,
 * with a glowing glyph for each power, violet for Eternity's and gold for the Living
 * Tribunal's. Drawn at 64x64 and scaled down.
 *
 * Usage: java AbilityIcons <output dir>
 */
public class AbilityIcons {

    private static final Color VIOLET = new Color(170, 80, 255, 150);
    private static final Color GOLD = new Color(255, 190, 60, 160);
    private static final String[] TRIBUNAL = {"gavel", "equitys_gaze", "necessitys_storm", "scales", "sentence", "convergence", "pillars", "cosmic_verdict",
            "decree", "edict", "tessellation", "alignment"};
    private static final String[] CHAOS = {"dice", "maw", "wanderers", "disarray"};
    private static final String[] BETWEEN = {"inversion", "equilibrium", "paradox"};
    private static final Color BLUE = new Color(120, 200, 255, 160);
    private static final Color GREY = new Color(200, 200, 205, 150);
    private static Color GLOW = VIOLET;
    private static final Color CORE = new Color(245, 235, 255);

    public static void main(String[] args) throws Exception {
        File out = new File(args[0]);
        out.mkdirs();
        String[] names = {"unmake", "void_lances", "starfall", "shatter", "singularity", "rift_step", "rift_gate",
                "gavel", "equitys_gaze", "necessitys_storm", "scales", "sentence", "convergence", "pillars", "cosmic_verdict",
                "decree", "edict", "tessellation", "alignment", "dice", "maw", "wanderers", "disarray", "inversion", "equilibrium", "paradox"};
        for (String name : names) {
            GLOW = java.util.Arrays.asList(TRIBUNAL).contains(name) ? GOLD : java.util.Arrays.asList(CHAOS).contains(name) ? BLUE
                    : java.util.Arrays.asList(BETWEEN).contains(name) ? GREY : VIOLET;
            BufferedImage big = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = big.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            backdrop(g);
            glyph(g, name);
            g.dispose();
            BufferedImage small = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            Graphics2D s = small.createGraphics();
            s.drawImage(big.getScaledInstance(16, 16, Image.SCALE_AREA_AVERAGING), 0, 0, null);
            s.dispose();
            ImageIO.write(small, "png", new File(out, name + ".png"));
        }
    }

    private static void backdrop(Graphics2D g) {
        g.setPaint(new RadialGradientPaint(32, 32, 30, new float[]{0F, 1F},
                new Color[]{new Color(40, 14, 70), new Color(8, 2, 18)}));
        g.fill(new Ellipse2D.Double(2, 2, 60, 60));
        g.setStroke(new BasicStroke(4F));
        g.setColor(new Color(212, 154, 28));
        g.draw(new Ellipse2D.Double(3, 3, 58, 58));
    }

    /** Strokes a shape twice: a wide violet glow, then a bright core. */
    private static void burn(Graphics2D g, java.awt.Shape shape, float width) {
        g.setColor(GLOW);
        g.setStroke(new BasicStroke(width * 2.6F, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(shape);
        g.setColor(CORE);
        g.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(shape);
    }

    private static Path2D line(double... points) {
        Path2D path = new Path2D.Double();
        path.moveTo(points[0], points[1]);
        for (int i = 2; i < points.length; i += 2) {
            path.lineTo(points[i], points[i + 1]);
        }
        return path;
    }

    private static void glyph(Graphics2D g, String name) {
        switch (name) {
            case "unmake":
                // Three claw marks.
                burn(g, line(18, 16, 30, 46), 4F);
                burn(g, line(28, 13, 38, 48), 4F);
                burn(g, line(38, 15, 46, 44), 4F);
                break;
            case "void_lances":
                burn(g, line(14, 20, 18, 14, 16, 10), 3F);
                burn(g, line(18, 18, 48, 48), 5F);
                burn(g, line(30, 14, 50, 32), 3F);
                break;
            case "starfall": {
                burn(g, line(48, 14, 30, 34), 3F);
                Path2D star = new Path2D.Double();
                for (int i = 0; i < 10; i++) {
                    double r = i % 2 == 0 ? 11 : 4.5;
                    double a = Math.PI * 2 * i / 10 - Math.PI / 2;
                    double x = 24 + Math.cos(a) * r, y = 40 + Math.sin(a) * r;
                    if (i == 0) star.moveTo(x, y); else star.lineTo(x, y);
                }
                star.closePath();
                g.setColor(GLOW);
                g.fill(star);
                burn(g, star, 2F);
                break;
            }
            case "shatter":
                burn(g, line(32, 46, 32, 30, 28, 22), 3F);
                burn(g, line(32, 46, 18, 38, 12, 40), 3F);
                burn(g, line(32, 46, 46, 36, 52, 38), 3F);
                burn(g, line(32, 46, 22, 52), 3F);
                burn(g, line(32, 46, 42, 53), 3F);
                break;
            case "singularity":
                g.setColor(GLOW);
                g.setStroke(new BasicStroke(5F));
                g.draw(new Ellipse2D.Double(10, 27, 44, 10));
                g.setColor(Color.BLACK);
                g.fill(new Ellipse2D.Double(21, 21, 22, 22));
                burn(g, new Ellipse2D.Double(21, 21, 22, 22), 2.5F);
                g.setColor(CORE);
                g.setStroke(new BasicStroke(2F));
                g.draw(new Ellipse2D.Double(10, 27, 44, 10).getBounds2D().createIntersection(new java.awt.geom.Rectangle2D.Double(0, 32, 64, 32)));
                break;
            case "gavel":
                // A fist coming down on a ring.
                g.setColor(GLOW);
                g.fill(new java.awt.geom.RoundRectangle2D.Double(22, 12, 20, 18, 8, 8));
                burn(g, new java.awt.geom.RoundRectangle2D.Double(22, 12, 20, 18, 8, 8), 2.5F);
                burn(g, line(32, 30, 32, 38), 3F);
                burn(g, new Ellipse2D.Double(12, 40, 40, 12), 2.5F);
                break;
            case "equitys_gaze": {
                // An eye, its light pouring out.
                Path2D eye = new Path2D.Double();
                eye.moveTo(10, 26);
                eye.quadTo(32, 8, 54, 26);
                eye.quadTo(32, 44, 10, 26);
                burn(g, eye, 2.5F);
                g.setColor(CORE);
                g.fill(new Ellipse2D.Double(27, 21, 10, 10));
                burn(g, line(26, 34, 18, 54), 3F);
                burn(g, line(38, 34, 46, 54), 3F);
                break;
            }
            case "necessitys_storm":
                burn(g, line(36, 10, 26, 30, 36, 30, 24, 54), 3.5F);
                burn(g, line(30, 30, 18, 40), 2F);
                break;
            case "scales":
                // A balance.
                burn(g, line(32, 12, 32, 46), 3F);
                burn(g, line(14, 20, 50, 20), 3F);
                burn(g, line(14, 20, 8, 34, 20, 34, 14, 20), 2F);
                burn(g, line(50, 20, 44, 34, 56, 34, 50, 20), 2F);
                burn(g, line(22, 50, 42, 50), 3F);
                break;
            case "sentence":
                // Bars of a cage.
                for (int i = 0; i < 4; i++) {
                    burn(g, line(18 + i * 9.3, 14, 18 + i * 9.3, 50), 2.5F);
                }
                burn(g, line(14, 14, 50, 14), 2.5F);
                burn(g, line(14, 50, 50, 50), 2.5F);
                break;
            case "convergence":
                // Blades closing on a point.
                for (int i = 0; i < 6; i++) {
                    double a = Math.PI * 2 * i / 6;
                    burn(g, line(32 + Math.cos(a) * 22, 32 + Math.sin(a) * 22, 32 + Math.cos(a) * 12, 32 + Math.sin(a) * 12), 3F);
                }
                g.setColor(CORE);
                g.fill(new Ellipse2D.Double(28, 28, 8, 8));
                break;
            case "pillars":
                burn(g, line(16, 52, 16, 34), 3.5F);
                burn(g, line(32, 52, 32, 22), 3.5F);
                burn(g, line(48, 52, 48, 12), 3.5F);
                burn(g, line(10, 52, 54, 52), 2F);
                break;
            case "cosmic_verdict":
                // A burst ringed by a wave.
                burn(g, new Ellipse2D.Double(10, 10, 44, 44), 2.5F);
                for (int i = 0; i < 8; i++) {
                    double a = Math.PI * 2 * i / 8;
                    burn(g, line(32 + Math.cos(a) * 5, 32 + Math.sin(a) * 5, 32 + Math.cos(a) * 15, 32 + Math.sin(a) * 15), 2F);
                }
                break;
            case "decree": {
                // One eye, wide open, its single line of light running straight down.
                Path2D eye = new Path2D.Double();
                eye.moveTo(12, 24);
                eye.quadTo(32, 8, 52, 24);
                eye.quadTo(32, 40, 12, 24);
                burn(g, eye, 2.5F);
                g.setColor(CORE);
                g.fill(new Ellipse2D.Double(27, 19, 10, 10));
                burn(g, line(32, 32, 32, 54), 4F);
                break;
            }
            case "edict":
                // A tablet of law, its lines ruled exactly.
                burn(g, new java.awt.geom.RoundRectangle2D.Double(16, 10, 32, 44, 10, 10), 2.5F);
                for (int i = 0; i < 4; i++) {
                    burn(g, line(22, 20 + i * 8, 42, 20 + i * 8), 1.8F);
                }
                break;
            case "tessellation":
                // A three-by-three grid, its corners and centre lit.
                for (int i = 0; i <= 3; i++) {
                    burn(g, line(14 + i * 12, 14, 14 + i * 12, 50), 1.6F);
                    burn(g, line(14, 14 + i * 12, 50, 14 + i * 12), 1.6F);
                }
                g.setColor(CORE);
                for (int[] c : new int[][]{{0, 0}, {2, 0}, {1, 1}, {0, 2}, {2, 2}}) {
                    g.fill(new java.awt.geom.Rectangle2D.Double(16 + c[0] * 12, 16 + c[1] * 12, 8, 8));
                }
                break;
            case "alignment":
                // A line, and chevrons drawn in to it from either side.
                burn(g, line(32, 10, 32, 54), 3.5F);
                for (int i = 0; i < 3; i++) {
                    double y = 18 + i * 14;
                    burn(g, line(12, y - 5, 20, y, 12, y + 5), 2F);
                    burn(g, line(52, y - 5, 44, y, 52, y + 5), 2F);
                }
                break;
            case "dice":
                // Two dice, tumbling.
                burn(g, new java.awt.geom.RoundRectangle2D.Double(10, 24, 22, 22, 6, 6), 2.5F);
                burn(g, new java.awt.geom.RoundRectangle2D.Double(32, 12, 22, 22, 6, 6), 2.5F);
                g.setColor(CORE);
                g.fill(new Ellipse2D.Double(18, 32, 6, 6));
                g.fill(new Ellipse2D.Double(36, 16, 5, 5));
                g.fill(new Ellipse2D.Double(45, 25, 5, 5));
                break;
            case "maw": {
                // A mouth, open, breathing in.
                Path2D mouth = new Path2D.Double();
                mouth.moveTo(12, 26);
                mouth.quadTo(32, 12, 52, 26);
                mouth.quadTo(32, 56, 12, 26);
                g.setColor(Color.BLACK);
                g.fill(mouth);
                burn(g, mouth, 2.5F);
                burn(g, line(18, 27, 22, 33, 26, 28, 30, 34, 34, 28, 38, 34, 42, 28, 46, 33), 1.5F);
                break;
            }
            case "wanderers":
                // Little rifts, scattered, each trailing where it has wandered.
                for (int[] w : new int[][]{{18, 20}, {44, 18}, {30, 40}, {48, 46}}) {
                    g.setColor(Color.BLACK);
                    g.fill(new Ellipse2D.Double(w[0] - 5, w[1] - 5, 10, 10));
                    burn(g, new Ellipse2D.Double(w[0] - 5, w[1] - 5, 10, 10), 2F);
                    burn(g, line(w[0] - 6, w[1] + 6, w[0] - 10, w[1] + 4, w[0] - 12, w[1] + 9), 1.4F);
                }
                break;
            case "disarray":
                // Arrows turned against one another.
                burn(g, line(12, 16, 48, 48), 2.5F);
                burn(g, line(48, 48, 40, 48, 48, 40, 48, 48), 2F);
                burn(g, line(52, 16, 16, 48), 2.5F);
                burn(g, line(16, 48, 24, 48, 16, 40, 16, 48), 2F);
                break;
            case "inversion":
                // Two halves, white and black, swapping places.
                g.setColor(Color.WHITE);
                g.fill(new java.awt.geom.Arc2D.Double(14, 14, 36, 36, 90, 180, java.awt.geom.Arc2D.PIE));
                g.setColor(Color.BLACK);
                g.fill(new java.awt.geom.Arc2D.Double(14, 14, 36, 36, 270, 180, java.awt.geom.Arc2D.PIE));
                burn(g, new Ellipse2D.Double(14, 14, 36, 36), 2F);
                burn(g, line(10, 10, 18, 10, 10, 18), 2F);
                burn(g, line(54, 54, 46, 54, 54, 46), 2F);
                break;
            case "equilibrium":
                // A scale level on its pivot, one pan white and one black.
                burn(g, line(32, 14, 32, 48), 2.5F);
                burn(g, line(12, 24, 52, 24), 2.5F);
                g.setColor(Color.WHITE);
                g.fill(new java.awt.geom.Arc2D.Double(6, 26, 14, 10, 180, 180, java.awt.geom.Arc2D.CHORD));
                g.setColor(Color.BLACK);
                g.fill(new java.awt.geom.Arc2D.Double(44, 26, 14, 10, 180, 180, java.awt.geom.Arc2D.CHORD));
                burn(g, line(24, 50, 40, 50), 2.5F);
                break;
            case "paradox":
                // An arrow pointing up and down at once.
                burn(g, line(32, 10, 32, 54), 3F);
                burn(g, line(24, 18, 32, 10, 40, 18), 2.5F);
                burn(g, line(24, 46, 32, 54, 40, 46), 2.5F);
                g.setColor(Color.WHITE);
                g.fill(new Ellipse2D.Double(18, 28, 8, 8));
                g.setColor(Color.BLACK);
                g.fill(new Ellipse2D.Double(38, 28, 8, 8));
                break;
            case "rift_gate":
                // A doorway torn in the dark, a crack running through it.
                g.setColor(Color.BLACK);
                g.fill(new Ellipse2D.Double(20, 10, 24, 44));
                burn(g, new Ellipse2D.Double(20, 10, 24, 44), 3F);
                burn(g, line(32, 14, 29, 24, 35, 32, 30, 42, 33, 50), 2F);
                break;
            default:
                // A tear to step through.
                burn(g, line(32, 10, 28, 20, 35, 28, 29, 38, 34, 46, 31, 54), 3.5F);
                burn(g, line(44, 32, 52, 32, 48, 28), 2.5F);
                break;
        }
    }
}
