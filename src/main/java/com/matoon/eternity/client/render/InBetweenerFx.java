package com.matoon.eternity.client.render;

import com.matoon.eternity.world.Nexus;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * What the In-Betweener's corner of the Nexus looks like, beyond the man himself:
 * <ul>
 * <li>his bubble, a smooth, nearly clear lens split between Order's gold and Chaos's blue, cracking from the
 * points where it is struck, with black and white motes of his power wheeling inside;</li>
 * <li>the seam, a pale grey band standing up out of the abyss at the junction, the one
 * colourless thing in a neon sky, visible from anywhere in the Nexus;</li>
 * <li>the glass bursting into shards;</li>
 * <li>Master Order and Lord Chaos arriving: a beam of white and a beam of black lancing in
 * and meeting at the junction (their heads themselves are {@link RenderOrderChaos});</li>
 * <li>and Lord Chaos's doings: patches of the world flickering onto deep space and
 * bursting, space streaming into his maw, flashes where he flings someone.</li>
 * </ul>
 * Everything is drawn relative to the camera, with fog, lighting and textures off.
 */
@SideOnly(Side.CLIENT)
public final class InBetweenerFx {

    private static final Vec3d LIGHT = new Vec3d(0.3D, 0.8D, 0.5D).normalize();
    /** Where his masters' heads rise while they come, either side of the junction, and how near they draw. */
    public static final Vec3d ORDER_FROM = new Vec3d(560D, 170D, -120D), CHAOS_FROM = new Vec3d(-560D, 170D, 120D);
    public static final Vec3d ORDER_AT = new Vec3d(240D, 130D, -50D), CHAOS_AT = new Vec3d(-240D, 130D, 50D);

    /** A unit icosphere, twice subdivided: 320 facets. */
    private static final List<Vec3d[]> FACETS = new ArrayList<>();
    /** A smooth unit sphere, ring by ring from the top down, for his glass. */
    private static final int RINGS = 30, SEGMENTS = 56;
    private static final Vec3d[][] GRID = new Vec3d[RINGS + 1][SEGMENTS + 1];
    /** Cracks wandering over the unit sphere, in the order they appear. */
    private static final Vec3d[][] CRACKS = new Vec3d[18][];
    /** Frost creeping over the inside of the unit sphere, branch by branch. */
    private static final Vec3d[][] FROST = new Vec3d[40][];

    static {
        double p = (1D + Math.sqrt(5D)) / 2D;
        Vec3d[] v = {
                new Vec3d(-1, p, 0), new Vec3d(1, p, 0), new Vec3d(-1, -p, 0), new Vec3d(1, -p, 0),
                new Vec3d(0, -1, p), new Vec3d(0, 1, p), new Vec3d(0, -1, -p), new Vec3d(0, 1, -p),
                new Vec3d(p, 0, -1), new Vec3d(p, 0, 1), new Vec3d(-p, 0, -1), new Vec3d(-p, 0, 1)};
        int[][] f = {{0, 11, 5}, {0, 5, 1}, {0, 1, 7}, {0, 7, 10}, {0, 10, 11}, {1, 5, 9}, {5, 11, 4}, {11, 10, 2}, {10, 7, 6}, {7, 1, 8},
                {3, 9, 4}, {3, 4, 2}, {3, 2, 6}, {3, 6, 8}, {3, 8, 9}, {4, 9, 5}, {2, 4, 11}, {6, 2, 10}, {8, 6, 7}, {9, 8, 1}};
        List<Vec3d[]> faces = new ArrayList<>();
        for (int[] face : f) {
            faces.add(new Vec3d[]{v[face[0]].normalize(), v[face[1]].normalize(), v[face[2]].normalize()});
        }
        for (int level = 0; level < 2; level++) {
            List<Vec3d[]> next = new ArrayList<>();
            for (Vec3d[] t : faces) {
                Vec3d ab = t[0].add(t[1]).normalize(), bc = t[1].add(t[2]).normalize(), ca = t[2].add(t[0]).normalize();
                next.add(new Vec3d[]{t[0], ab, ca});
                next.add(new Vec3d[]{t[1], bc, ab});
                next.add(new Vec3d[]{t[2], ca, bc});
                next.add(new Vec3d[]{ab, bc, ca});
            }
            faces = next;
        }
        FACETS.addAll(faces);
        for (int i = 0; i <= RINGS; i++) {
            for (int j = 0; j <= SEGMENTS; j++) {
                GRID[i][j] = unit(Math.PI * i / RINGS, Math.PI * 2D * j / SEGMENTS);
            }
        }

        Random random = new Random(1977L);
        for (int i = 0; i < CRACKS.length; i++) {
            Vec3d at = randomUnit(random);
            Vec3d heading = at.crossProduct(randomUnit(random)).normalize();
            Vec3d[] path = new Vec3d[9];
            path[0] = at;
            for (int s = 1; s < path.length; s++) {
                heading = heading.add(randomUnit(random).scale(0.6D)).normalize();
                // Along the surface: drop whatever part of the heading points out of it.
                heading = heading.subtract(at.scale(heading.dotProduct(at))).normalize();
                at = at.add(heading.scale(0.09D + random.nextDouble() * 0.07D)).normalize();
                path[s] = at;
            }
            CRACKS[i] = path;
        }
        for (int i = 0; i < FROST.length; i++) {
            Vec3d at = randomUnit(random);
            Vec3d[] path = new Vec3d[14];
            path[0] = at;
            Vec3d heading = at.crossProduct(randomUnit(random)).normalize();
            for (int s = 1; s < path.length; s++) {
                // Frost grows in short, sharp turns.
                heading = heading.add(randomUnit(random).scale(s % 3 == 0 ? 1.4D : 0.25D)).normalize();
                heading = heading.subtract(at.scale(heading.dotProduct(at))).normalize();
                at = at.add(heading.scale(0.04D + random.nextDouble() * 0.03D)).normalize();
                path[s] = at;
            }
            FROST[i] = path;
        }
    }

    private InBetweenerFx() {
    }

    private static Vec3d randomUnit(Random random) {
        double x, y, z, len;
        do {
            x = random.nextDouble() * 2D - 1D;
            y = random.nextDouble() * 2D - 1D;
            z = random.nextDouble() * 2D - 1D;
            len = x * x + y * y + z * z;
        } while (len > 1D || len < 1.0E-3);
        return new Vec3d(x, y, z).normalize();
    }

    private static float hash(int i) {
        int h = i * 0x2C1B3C6D ^ 0x297A2D39;
        h ^= h >>> 15;
        h *= 0x5bd1e995;
        return ((h >>> 8) & 0xFFFF) / 65535F;
    }

    // ------------------------------------------------------------------ the bubble

    /**
     * What he is doing to his glass at the moment, each from 0 (nothing) to 1, for
     * {@link #bubble}: the things he does while he speaks, which you see happen.
     */
    public static final class Look {
        /** Frost of two colours, creeping over the inside. */
        public float frost;
        /** The glass going dark. */
        public float dim;
        /** The glass full of stars. */
        public float stars;
        /** The glass rippling, as with laughter. */
        public float ripple;
        /** The glass thinning and bulging toward you, as he beckons or offers. */
        public float thin;
        /** A crack running across it in front of you that was not there a moment ago. */
        public float fakeCrack;
        /** A ring running round the glass from where he rapped on it, 0 to 1 as it travels; below 0, none. */
        public float tap = -1F;
        /** The glass brightening, as he rises to his full height. */
        public float bright;
        /** Which way you are from the middle of the glass. */
        public Vec3d toward = new Vec3d(0D, 0D, 1D);
    }

    private static final Look STILL = new Look();

    public static void bubble(Vec3d c, double r, float crack, float power, float time) {
        bubble(c, r, crack, power, time, STILL);
    }

    /**
     * His glass. Not a crystal: a smooth, nearly clear lens, made from the spot where Order's
     * power and Chaos's meet, and it shows. A wandering seam of colourless light runs round
     * it where the two cancel. On one side of the seam the glass is Order's, warm gold, with
     * a fine, perfectly regular lattice etched into it; on the other it is Chaos's, cold blue
     * and violet, churning slowly like oil on water. Grey bands of colour slide down it like
     * the film on a soap bubble. Face on, you can barely see it at all; only its edges, its
     * seam and its glints give it away.
     *
     * @param c     its middle, relative to the camera
     * @param crack how near it is to breaking, 0 to 1
     * @param power how much of him is wheeling inside it, 0 to 1
     */
    public static void bubble(Vec3d c, double r, float crack, float power, float time, Look look) {
        if (r < 0.05D) {
            return;
        }
        float flicker = crack > 0.5F ? 1F + 0.35F * crack * MathHelper.sin(time * 1.9F) : 1F;
        flicker *= 1F + 0.6F * look.bright;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        Seam seam = new Seam(time);

        if (look.stars > 0.01F) {
            // The inside of everything, which he can show you whenever he likes.
            final double inner = r * 0.97D;
            Cosmic.draw(() -> shell(c, inner, time, look), 6, 0.85F * look.stars, 1.4F * look.stars, 0.85F, 0.8F, 1F, false);
            FxRenderer.additiveState();
        }

        // The body of the glass, coloured vertex by vertex.
        float[][][] colour = new float[RINGS + 1][SEGMENTS + 1][];
        Vec3d[][] at = new Vec3d[RINGS + 1][SEGMENTS + 1];
        for (int i = 0; i <= RINGS; i++) {
            for (int j = 0; j <= SEGMENTS; j++) {
                Vec3d n = GRID[i][j];
                Vec3d p = surface(c, n, r, time, look);
                at[i][j] = p;
                colour[i][j] = glass(n, p, seam, time, look, flicker);
            }
        }
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < RINGS; i++) {
            for (int j = 0; j < SEGMENTS; j++) {
                vertex(buffer, at[i][j], colour[i][j]);
                vertex(buffer, at[i + 1][j], colour[i + 1][j]);
                vertex(buffer, at[i + 1][j + 1], colour[i + 1][j + 1]);
                vertex(buffer, at[i][j], colour[i][j]);
                vertex(buffer, at[i + 1][j + 1], colour[i + 1][j + 1]);
                vertex(buffer, at[i][j + 1], colour[i][j + 1]);
            }
        }
        tessellator.draw();

        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        lattice(c, r * 1.002D, seam, time, look, flicker);
        seamLine(c, r * 1.003D, seam, time, look, flicker);

        if (crack > 0.01F) {
            GlStateManager.glLineWidth(2.5F);
            buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
            double cr = r * 1.004D;
            for (int i = 0; i < CRACKS.length; i++) {
                float threshold = i / (float) CRACKS.length * 0.85F;
                float grown = MathHelper.clamp((crack - threshold) / 0.15F, 0F, 1F);
                int segments = (int) (grown * (CRACKS[i].length - 1));
                for (int s = 0; s < segments; s++) {
                    Vec3d a = CRACKS[i][s], b = CRACKS[i][s + 1];
                    buffer.pos(c.x + a.x * cr, c.y + a.y * cr, c.z + a.z * cr).color(1F, 1F, 1F, 0.95F).endVertex();
                    buffer.pos(c.x + b.x * cr, c.y + b.y * cr, c.z + b.z * cr).color(1F, 1F, 1F, 0.95F).endVertex();
                }
            }
            tessellator.draw();
        }
        if (look.fakeCrack > 0.01F) {
            fakeCrack(c, r * 1.004D, look);
        }
        if (look.frost > 0.01F) {
            frost(c, r * 0.992D, look.frost);
        }
        if (look.tap >= 0F && look.tap <= 1F) {
            tapRing(c, r * 1.006D, look);
        }
        GlStateManager.glLineWidth(1F);

        if (power > 0.01F && look.dim < 0.5F) {
            motes(c, r, power * (1F - look.dim * 2F), time);
        }
        if (look.dim > 0.01F) {
            // The glass goes dark from inside: nothing of him shows now.
            final double inner = r * 0.985D;
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            GlStateManager.color(0.01F, 0.01F, 0.015F, 0.93F * look.dim);
            shell(c, inner, time, look);
            GlStateManager.color(1F, 1F, 1F, 1F);
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        }
    }

    private static void vertex(BufferBuilder buffer, Vec3d p, float[] rgba) {
        buffer.pos(p.x, p.y, p.z).color(rgba[0], rgba[1], rgba[2], rgba[3]).endVertex();
    }

    /**
     * Where Order's half of the glass meets Chaos's: a great circle round it, tilted and slowly
     * turning, that wavers as it goes. Positive on Order's side, negative on Chaos's.
     */
    private static final class Seam {
        final Vec3d axis, u, w;
        final float time;

        Seam(float time) {
            this.time = time;
            double tilt = 0.45D + 0.15D * Math.sin(time * 0.0021D);
            double turn = time * 0.0035D;
            axis = new Vec3d(Math.sin(tilt) * Math.cos(turn), Math.cos(tilt), Math.sin(tilt) * Math.sin(turn));
            Vec3d side = axis.crossProduct(new Vec3d(0D, 0D, 1D));
            u = side.normalize();
            w = axis.crossProduct(u).normalize();
        }

        /** How far the seam wavers off its circle at this angle round it. */
        double waver(double angle) {
            return 0.13D * Math.sin(angle * 3D + time * 0.017D) + 0.05D * Math.sin(angle * 7D - time * 0.031D);
        }

        double side(Vec3d unit) {
            double angle = Math.atan2(unit.dotProduct(w), unit.dotProduct(u));
            return unit.dotProduct(axis) - waver(angle);
        }

        /** A point on the seam itself. */
        Vec3d at(double angle) {
            return u.scale(Math.cos(angle)).add(w.scale(Math.sin(angle))).add(axis.scale(waver(angle))).normalize();
        }
    }

    /** The colour of the glass at one point: nearly clear face on, bright at its edges and along its seam. */
    private static float[] glass(Vec3d n, Vec3d p, Seam seam, float time, Look look, float flicker) {
        Vec3d view = p.normalize();
        double dot = n.dotProduct(view);
        double rim = 1D - Math.abs(dot);
        rim *= rim;
        Vec3d reflect = view.subtract(n.scale(2D * dot));
        double glint = Math.pow(Math.max(0D, reflect.dotProduct(LIGHT)), 40D);

        double side = seam.side(n);
        double edge = Math.exp(-(side * side) / 0.0035D);
        // How far into its own half a point lies: 0 at the seam, 1 well inside it.
        float order = (float) MathHelper.clamp(side / 0.25D, 0D, 1D), chaos = (float) MathHelper.clamp(-side / 0.25D, 0D, 1D);

        // Chaos's half never sits still.
        double churn = Math.sin(n.x * 4.1D + time * 0.021D + 2D * Math.sin(n.y * 3.3D - time * 0.013D))
                * Math.sin(n.z * 3.7D - time * 0.017D + 2D * Math.sin(n.x * 2.9D + time * 0.011D));
        float violet = (float) (0.5D + 0.5D * churn);
        // A soap film's bands, without the colour: they slide slowly downward.
        double film = 0.5D + 0.5D * Math.sin(n.y * 16D + 3D * Math.sin(n.x * 2D + n.z * 2D) + time * 0.03D);

        float red = 0.82F, green = 0.84F, blue = 0.88F;
        red += order * (1F - red) * 0.9F - chaos * (0.3F + 0.15F * (1F - violet));
        green += -order * 0.06F - chaos * (0.25F + 0.1F * violet);
        blue += -order * 0.4F + chaos * (0.12F + 0.05F * violet);
        float filmTint = (float) (film * 0.25D * rim);
        red += filmTint;
        green += filmTint;
        blue += filmTint;

        float facing = (float) Math.max(0D, (n.dotProduct(look.toward) - 0.8D) / 0.2D) * look.thin;
        float light = (float) Math.max(Math.max(glint, edge * 0.85D), facing);
        red += (1F - red) * light;
        green += (1F - green) * light;
        blue += (1F - blue) * light;

        double alpha = 0.02D + 0.3D * rim + 0.05D * film * rim + 0.1D * chaos * Math.abs(churn) * (0.4D + rim)
                + 0.45D * edge * (0.35D + rim) + 0.6D * glint;
        alpha *= flicker * (1F - 0.6F * facing);
        return new float[]{MathHelper.clamp(red, 0F, 1F), MathHelper.clamp(green, 0F, 1F), MathHelper.clamp(blue, 0F, 1F),
                (float) MathHelper.clamp(alpha, 0D, 0.9D)};
    }

    /** Order's lattice: fine golden lines of latitude and longitude, exact, on his half of the glass only. */
    private static void lattice(Vec3d c, double r, Seam seam, float time, Look look, float flicker) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        double spin = time * 0.0012D;
        GlStateManager.glLineWidth(1F);
        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        int steps = 72;
        for (int k = 1; k < 12; k++) {
            double lat = Math.PI * k / 12D;
            for (int s = 0; s < steps; s++) {
                latticeSegment(buffer, c, r, seam, time, look, flicker, unit(lat, spin + Math.PI * 2D * s / steps),
                        unit(lat, spin + Math.PI * 2D * (s + 1) / steps));
            }
        }
        for (int k = 0; k < 24; k++) {
            double lon = spin + Math.PI * 2D * k / 24D;
            for (int s = 0; s < steps / 2; s++) {
                latticeSegment(buffer, c, r, seam, time, look, flicker, unit(Math.PI * s / (steps / 2D), lon),
                        unit(Math.PI * (s + 1) / (steps / 2D), lon));
            }
        }
        tessellator.draw();
    }

    private static void latticeSegment(BufferBuilder buffer, Vec3d c, double r, Seam seam, float time, Look look, float flicker,
                                       Vec3d a, Vec3d b) {
        Vec3d mid = a.add(b).normalize();
        double side = seam.side(mid);
        if (side <= 0.02D) {
            return;
        }
        Vec3d pa = surface(c, a, r, time, look), pb = surface(c, b, r, time, look);
        Vec3d view = pa.normalize();
        double rim = 1D - Math.abs(mid.dotProduct(view));
        float alpha = (float) ((0.05D + 0.3D * rim * rim) * MathHelper.clamp((side - 0.02D) / 0.15D, 0D, 1D)) * flicker;
        buffer.pos(pa.x, pa.y, pa.z).color(1F, 0.8F, 0.38F, alpha).endVertex();
        buffer.pos(pb.x, pb.y, pb.z).color(1F, 0.8F, 0.38F, alpha).endVertex();
    }

    /** The seam itself: a thin, colourless line of light where the two halves cancel. */
    private static void seamLine(Vec3d c, double r, Seam seam, float time, Look look, float flicker) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        for (int pass = 0; pass < 2; pass++) {
            GlStateManager.glLineWidth(pass == 0 ? 5F : 1.6F);
            buffer.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
            for (int s = 0; s < 120; s++) {
                double angle = Math.PI * 2D * s / 120D;
                Vec3d p = surface(c, seam.at(angle), r, time, look);
                float pulse = 0.75F + 0.25F * MathHelper.sin((float) (angle * 5D) - time * 0.05F);
                float alpha = (pass == 0 ? 0.12F : 0.7F) * pulse * flicker;
                buffer.pos(p.x, p.y, p.z).color(0.94F, 0.94F, 0.96F, Math.min(1F, alpha)).endVertex();
            }
            tessellator.draw();
        }
        GlStateManager.glLineWidth(1F);
    }

    private static Vec3d unit(double polar, double azimuth) {
        double s = Math.sin(polar);
        return new Vec3d(s * Math.cos(azimuth), Math.cos(polar), s * Math.sin(azimuth));
    }

    /** A point on the glass, rippling and bulging as he makes it. */
    private static Vec3d surface(Vec3d c, Vec3d unit, double r, float time, Look look) {
        double scale = 1D;
        if (look.ripple > 0.01F) {
            scale += 0.045D * look.ripple * Math.sin(time * 0.9D + unit.y * 7D + unit.x * 3D);
        }
        if (look.thin > 0.01F) {
            double toward = Math.max(0D, (unit.dotProduct(look.toward) - 0.8D) / 0.2D);
            scale += 0.08D * look.thin * toward * toward;
        }
        return c.add(unit.scale(r * scale));
    }

    /** The whole sphere of facets, position only, for drawing darkness or deep space onto. */
    private static void shell(Vec3d c, double r, float time, Look look) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION);
        for (int i = 0; i < RINGS; i++) {
            for (int j = 0; j < SEGMENTS; j++) {
                Vec3d a = surface(c, GRID[i][j], r, time, look), b = surface(c, GRID[i + 1][j], r, time, look);
                Vec3d d = surface(c, GRID[i + 1][j + 1], r, time, look), e = surface(c, GRID[i][j + 1], r, time, look);
                buffer.pos(a.x, a.y, a.z).endVertex();
                buffer.pos(b.x, b.y, b.z).endVertex();
                buffer.pos(d.x, d.y, d.z).endVertex();
                buffer.pos(a.x, a.y, a.z).endVertex();
                buffer.pos(d.x, d.y, d.z).endVertex();
                buffer.pos(e.x, e.y, e.z).endVertex();
            }
        }
        tessellator.draw();
    }

    /** Frost of two colours, white and black, branching over the inside of the glass as it grows. */
    private static void frost(Vec3d c, double r, float amount) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        for (int pass = 0; pass < 2; pass++) {
            boolean white = pass == 0;
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                    white ? GlStateManager.DestFactor.ONE : GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            GlStateManager.glLineWidth(white ? 1.6F : 2F);
            buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
            float v = white ? 1F : 0.02F;
            for (int i = pass; i < FROST.length; i += 2) {
                int segments = (int) (amount * (FROST[i].length - 1));
                for (int s = 0; s < segments; s++) {
                    Vec3d a = FROST[i][s], b = FROST[i][s + 1];
                    float alpha = 0.85F * (1F - s / (float) FROST[i].length * 0.5F);
                    buffer.pos(c.x + a.x * r, c.y + a.y * r, c.z + a.z * r).color(v, v, v, alpha).endVertex();
                    buffer.pos(c.x + b.x * r, c.y + b.y * r, c.z + b.z * r).color(v, v, v, alpha).endVertex();
                }
            }
            tessellator.draw();
        }
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
    }

    /** A crack that was not there a moment ago, running across the glass right in front of you. */
    private static void fakeCrack(Vec3d c, double r, Look look) {
        Vec3d at = look.toward.normalize();
        Vec3d side = at.crossProduct(new Vec3d(0D, 1D, 0D));
        side = side.lengthSquared() < 1.0E-4 ? new Vec3d(1D, 0D, 0D) : side.normalize();
        Vec3d up = side.crossProduct(at).normalize();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        GlStateManager.glLineWidth(2.5F);
        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        Random random = new Random(42L);
        int steps = (int) (7 * look.fakeCrack);
        for (int branch = 0; branch < 6; branch++) {
            Vec3d p = at;
            double a = random.nextDouble() * Math.PI * 2D;
            Vec3d heading = side.scale(Math.cos(a)).add(up.scale(Math.sin(a)));
            for (int s = 0; s < 7; s++) {
                heading = heading.add(new Vec3d(random.nextDouble() - 0.5D, random.nextDouble() - 0.5D, random.nextDouble() - 0.5D).scale(0.7D));
                heading = heading.subtract(p.scale(heading.dotProduct(p))).normalize();
                Vec3d q = p.add(heading.scale(0.05D)).normalize();
                if (s < steps) {
                    buffer.pos(c.x + p.x * r, c.y + p.y * r, c.z + p.z * r).color(1F, 1F, 1F, 0.95F).endVertex();
                    buffer.pos(c.x + q.x * r, c.y + q.y * r, c.z + q.z * r).color(1F, 1F, 1F, 0.95F).endVertex();
                }
                p = q;
            }
        }
        tessellator.draw();
    }

    /** A ring running round the glass from the point where he rapped on it. */
    private static void tapRing(Vec3d c, double r, Look look) {
        Vec3d at = look.toward.normalize();
        Vec3d side = at.crossProduct(new Vec3d(0D, 1D, 0D));
        side = side.lengthSquared() < 1.0E-4 ? new Vec3d(1D, 0D, 0D) : side.normalize();
        Vec3d up = side.crossProduct(at).normalize();
        double angle = look.tap * Math.PI;
        float alpha = 0.9F * (1F - look.tap);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        GlStateManager.glLineWidth(3F);
        buffer.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < 48; i++) {
            double a = Math.PI * 2D * i / 48D;
            Vec3d around = side.scale(Math.cos(a)).add(up.scale(Math.sin(a)));
            Vec3d p = at.scale(Math.cos(angle)).add(around.scale(Math.sin(angle)));
            buffer.pos(c.x + p.x * r, c.y + p.y * r, c.z + p.z * r).color(0.95F, 0.97F, 1F, alpha).endVertex();
        }
        tessellator.draw();
    }

    /** Black and white, wheeling round him inside the glass. */
    private static void motes(Vec3d c, double r, float power, float time) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        int count = (int) (64 * power);
        GL11.glPointSize(4F);
        for (int pass = 0; pass < 2; pass++) {
            boolean white = pass == 0;
            if (white) {
                GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            } else {
                GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            }
            buffer.begin(GL11.GL_POINTS, DefaultVertexFormats.POSITION_COLOR);
            for (int i = pass; i < count; i += 2) {
                float h = hash(i + 101);
                double theta = time * 0.025D * (1D + (i % 3)) * (i % 2 == 0 ? 1D : -1D) + i * 2.4D;
                double phi = Math.sin(time * 0.017D + i) * 1.1D;
                double radius = r * (0.3D + 0.55D * h);
                double x = Math.cos(phi) * Math.cos(theta) * radius, y = Math.sin(phi) * radius * 0.8D, z = Math.cos(phi) * Math.sin(theta) * radius;
                float v = white ? 1F : 0.02F;
                buffer.pos(c.x + x, c.y + y, c.z + z).color(v, v, v, white ? 0.85F : 0.9F).endVertex();
            }
            tessellator.draw();
        }
        GL11.glPointSize(1F);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
    }

    // ------------------------------------------------------------------ the seam

    /**
     * A tall band of colourless grey standing at the junction, from the abyss to far above
     * the highest island. From far off it is the only grey thing in the sky; close up it
     * fades, because by then everything around you is grey too.
     */
    public static void seam(Vec3d camera, float time) {
        Vec3d base = new Vec3d(Nexus.JUNCTION_X, 0D, Nexus.JUNCTION_Z).subtract(camera);
        double dist = Math.sqrt(base.x * base.x + base.z * base.z);
        float alpha = (float) MathHelper.clamp((dist - 40D) / 120D, 0D, 1D) * (0.3F + 0.04F * MathHelper.sin(time * 0.02F));
        if (alpha <= 0.01F) {
            return;
        }
        Vec3d side = new Vec3d(-base.z, 0D, base.x).normalize();
        double width = 4D + dist * 0.004D;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        for (int half = -1; half <= 1; half += 2) {
            Vec3d edge = side.scale(width * half);
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            buffer.pos(base.x, base.y, base.z).color(0.6F, 0.6F, 0.62F, alpha).endVertex();
            buffer.pos(base.x + edge.x, base.y, base.z + edge.z).color(0.6F, 0.6F, 0.62F, 0F).endVertex();
            buffer.pos(base.x + edge.x, base.y + 420D, base.z + edge.z).color(0.6F, 0.6F, 0.62F, 0F).endVertex();
            buffer.pos(base.x, base.y + 420D, base.z).color(0.6F, 0.6F, 0.62F, alpha).endVertex();
            tessellator.draw();
        }
    }



    /**
     * Where their powers meet: Order's light, gold, streaming in from one edge of the sky and
     * Chaos's, pale blue, from the opposite edge, both greying as they come, until above his
     * glass there is no colour left in either and they are the same light, and then none.
     */
    public static void convergence(Vec3d camera, float time) {
        Vec3d meet = new Vec3d(Nexus.JUNCTION_X, Nexus.BUBBLE_Y + 40D, Nexus.JUNCTION_Z);
        double dist = Math.sqrt(Math.pow(camera.x - meet.x, 2) + Math.pow(camera.z - meet.z, 2));
        if (dist > 1400D) {
            return;
        }
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        stream(meet.add(720D, 220D, -160D), meet, 1F, 0.8F, 0.3F, camera, time);
        stream(meet.add(-720D, 220D, 160D), meet, 0.45F, 0.75F, 1F, camera, time);
    }

    /** A broad, wavering band of light from far off to the meeting point, losing its colour and then itself as it comes. */
    private static void stream(Vec3d from, Vec3d to, float r, float g, float b, Vec3d camera, float time) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        int segments = 32;
        buffer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i <= segments; i++) {
            double s = i / (double) segments;
            Vec3d at = from.add(to.subtract(from).scale(s)).add(0D, Math.sin(s * 9D + time * 0.01D) * 6D * (1D - s), 0D);
            Vec3d rel = at.subtract(camera);
            Vec3d dir = to.subtract(from).normalize();
            Vec3d side = dir.crossProduct(rel).normalize();
            double width = 18D * (1D - s) + 1.5D;
            // Colour drains to grey along the way; the band itself fades out at the very end.
            float grey = (float) Math.min(1D, s * 1.25D);
            float cr = r + (0.62F - r) * grey, cg = g + (0.62F - g) * grey, cb = b + (0.64F - b) * grey;
            float alpha = (float) (0.32D * Math.min(1D, (1D - s) * 6D) * Math.min(1D, s * 8D + 0.3D));
            buffer.pos(rel.x - side.x * width, rel.y - side.y * width, rel.z - side.z * width).color(cr, cg, cb, 0F).endVertex();
            buffer.pos(rel.x, rel.y, rel.z).color(cr, cg, cb, alpha).endVertex();
        }
        tessellator.draw();
        buffer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i <= segments; i++) {
            double s = i / (double) segments;
            Vec3d at = from.add(to.subtract(from).scale(s)).add(0D, Math.sin(s * 9D + time * 0.01D) * 6D * (1D - s), 0D);
            Vec3d rel = at.subtract(camera);
            Vec3d dir = to.subtract(from).normalize();
            Vec3d side = dir.crossProduct(rel).normalize();
            double width = 18D * (1D - s) + 1.5D;
            float grey = (float) Math.min(1D, s * 1.25D);
            float cr = r + (0.62F - r) * grey, cg = g + (0.62F - g) * grey, cb = b + (0.64F - b) * grey;
            float alpha = (float) (0.32D * Math.min(1D, (1D - s) * 6D) * Math.min(1D, s * 8D + 0.3D));
            buffer.pos(rel.x, rel.y, rel.z).color(cr, cg, cb, alpha).endVertex();
            buffer.pos(rel.x + side.x * width, rel.y + side.y * width, rel.z + side.z * width).color(cr, cg, cb, 0F).endVertex();
        }
        tessellator.draw();
    }
    // ------------------------------------------------------------------ Lord Chaos

    /** A patch of the world flickers onto deep space, a ring of pale blue marking it, then bursts. */
    public static class ChaosBurst extends FxRenderer.Fx {
        private final Vec3d centre;
        private final double radius;
        private final int warning;

        public ChaosBurst(Vec3d centre, double radius, int warning) {
            super(warning + 14);
            this.centre = centre;
            this.radius = radius;
            this.warning = warning;
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            float t = age + partialTicks;
            Vec3d c = centre.subtract(camera).add(0D, 0.08D, 0D);
            if (t < warning) {
                // A flickering ring, quicker as it nears.
                float p = t / warning;
                float flicker = 0.5F + 0.5F * MathHelper.sin(t * (0.6F + 2F * p));
                ring(c, radius, 0.55F, 0.85F, 1F, (0.3F + 0.5F * p) * flicker);
                final Vec3d at = c;
                final double r = radius * p;
                Cosmic.draw(() -> disc(at, r), 4, 0.8F * p, 1.2F, 0.6F, 0.8F, 1F, false);
                FxRenderer.additiveState();
                return;
            }
            float burst = (t - warning) / 14F;
            final Vec3d at = c.add(0D, radius * 0.5D * burst, 0D);
            final double r = radius * (1D + burst);
            Cosmic.draw(() -> sphere(at, r * 0.6D), 5, 0.7F * (1F - burst), 1.5F * (1F - burst), 0.6F, 0.8F, 1F, false);
            FxRenderer.additiveState();
            ring(c, r, 0.7F, 0.9F, 1F, 0.8F * (1F - burst));
        }
    }

    /** Space streams from someone into his open maw. */
    public static class ChaosMaw extends FxRenderer.Fx {
        private final Vec3d from, to;
        private final long seed;
        private float red = 0.5F, green = 0.75F, blue = 1F;

        public ChaosMaw(Vec3d from, Vec3d to, long seed, int ticks) {
            super(ticks);
            this.from = from;
            this.to = to;
            this.seed = seed;
        }

        /** The same stream in another colour: the Soul Gem's orange, flying between hand and brow. */
        public ChaosMaw tint(float r, float g, float b) {
            red = r;
            green = g;
            blue = b;
            return this;
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            float fade = 1F - progress(partialTicks);
            Random random = new Random(seed);
            Vec3d a = from.subtract(camera), b = to.subtract(camera);
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            GlStateManager.glLineWidth(2F);
            buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
            for (int i = 0; i < 10; i++) {
                Vec3d off = new Vec3d(random.nextDouble() - 0.5D, random.nextDouble() - 0.5D, random.nextDouble() - 0.5D).scale(2.5D);
                double s0 = (progress(partialTicks) + i / 10D) % 1D;
                Vec3d p0 = a.add(off).add(b.subtract(a.add(off)).scale(s0));
                Vec3d p1 = a.add(off).add(b.subtract(a.add(off)).scale(Math.min(1D, s0 + 0.15D)));
                buffer.pos(p0.x, p0.y, p0.z).color(red, green, blue, 0F).endVertex();
                buffer.pos(p1.x, p1.y, p1.z).color(Math.min(1F, red + 0.25F), Math.min(1F, green + 0.15F), blue, 0.8F * fade).endVertex();
            }
            tessellator.draw();
            GlStateManager.glLineWidth(1F);
        }
    }

    /** A flash of pale blue where something was, and where it is now. */
    public static class ChaosFlash extends FxRenderer.Fx {
        private final Vec3d from, to;

        public ChaosFlash(Vec3d from, Vec3d to, int ticks) {
            super(ticks);
            this.from = from;
            this.to = to;
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            float fade = 1F - progress(partialTicks);
            for (Vec3d at : new Vec3d[]{from, to}) {
                Vec3d c = at.subtract(camera);
                Tessellator tessellator = Tessellator.getInstance();
                BufferBuilder buffer = tessellator.getBuffer();
                buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
                for (int k = 0; k < 2; k++) {
                    double dx = k == 0 ? 0.9D : 0D, dz = k == 0 ? 0D : 0.9D;
                    buffer.pos(c.x - dx, c.y, c.z - dz).color(0.55F, 0.85F, 1F, 0.8F * fade).endVertex();
                    buffer.pos(c.x + dx, c.y, c.z + dz).color(0.55F, 0.85F, 1F, 0.8F * fade).endVertex();
                    buffer.pos(c.x + dx, c.y + 3D, c.z + dz).color(0.55F, 0.85F, 1F, 0F).endVertex();
                    buffer.pos(c.x - dx, c.y + 3D, c.z - dz).color(0.55F, 0.85F, 1F, 0F).endVertex();
                }
                tessellator.draw();
                ring(c.add(0D, 0.1D, 0D), 1.5D + 2D * progress(partialTicks), 0.55F, 0.85F, 1F, 0.7F * fade);
            }
        }
    }

    /** A flat ring on the floor. */
    private static void ring(Vec3d c, double radius, float r, float g, float b, float alpha) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i <= 40; i++) {
            double a = Math.PI * 2D * i / 40D;
            double cos = Math.cos(a), sin = Math.sin(a);
            buffer.pos(c.x + cos * radius * 0.88D, c.y, c.z + sin * radius * 0.88D).color(r, g, b, 0F).endVertex();
            buffer.pos(c.x + cos * radius, c.y, c.z + sin * radius).color(r, g, b, alpha).endVertex();
        }
        tessellator.draw();
    }

    /** A flat disc, position only, for drawing deep space onto. */
    private static void disc(Vec3d c, double radius) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION);
        buffer.pos(c.x, c.y, c.z).endVertex();
        for (int i = 0; i <= 24; i++) {
            double a = -Math.PI * 2D * i / 24D;
            buffer.pos(c.x + Math.cos(a) * radius, c.y, c.z + Math.sin(a) * radius).endVertex();
        }
        tessellator.draw();
    }

    /** A sphere, position only, for drawing deep space onto. */
    private static void sphere(Vec3d c, double radius) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION);
        for (Vec3d[] t : FACETS) {
            for (Vec3d corner : t) {
                buffer.pos(c.x + corner.x * radius, c.y + corner.y * radius, c.z + corner.z * radius).endVertex();
            }
        }
        tessellator.draw();
    }

    // ------------------------------------------------------------------ Master Order

    /** A straight golden bolt of punishment, perfectly vertical, ringing the ground where it lands. */
    public static class OrderBolt extends FxRenderer.Fx {
        private final Vec3d at;

        public OrderBolt(Vec3d at, int ticks) {
            super(ticks);
            this.at = at;
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            float fade = 1F - progress(partialTicks);
            Vec3d c = at.subtract(camera);
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            for (int k = 0; k < 2; k++) {
                double w = k == 0 ? 0.9D : 0.25D;
                float a = (k == 0 ? 0.4F : 1F) * fade;
                buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
                for (int s = 0; s < 2; s++) {
                    double dx = s == 0 ? w : 0D, dz = s == 0 ? 0D : w;
                    buffer.pos(c.x - dx, c.y, c.z - dz).color(1F, 0.85F, 0.35F, a).endVertex();
                    buffer.pos(c.x + dx, c.y, c.z + dz).color(1F, 0.85F, 0.35F, a).endVertex();
                    buffer.pos(c.x + dx, c.y + 60D, c.z + dz).color(1F, 0.95F, 0.7F, 0F).endVertex();
                    buffer.pos(c.x - dx, c.y + 60D, c.z - dz).color(1F, 0.95F, 0.7F, 0F).endVertex();
                }
                tessellator.draw();
            }
            ring(c.add(0D, 0.06D, 0D), 1.2D + progress(partialTicks) * 2.5D, 1F, 0.85F, 0.35F, 0.9F * fade);
        }
    }

    /**
     * Five by five golden tiles laid on the floor, the lit ones glowing brighter as the
     * moment nears; or, erupting, the lit ones throwing up walls of light.
     */
    public static class Tiles extends FxRenderer.Fx {
        private final Vec3d centre;
        private final double size;
        private final int lit;
        private final boolean erupting;

        public Tiles(Vec3d centre, double size, int lit, int ticks, boolean erupting) {
            super(ticks);
            this.centre = centre;
            this.size = size;
            this.lit = lit;
            this.erupting = erupting;
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            float p = progress(partialTicks);
            Vec3d c = centre.subtract(camera).add(0D, 0.05D, 0D);
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            for (int i = 0; i < 5; i++) {
                for (int j = 0; j < 5; j++) {
                    boolean on = (lit >> (i * 5 + j) & 1) != 0;
                    double x0 = c.x + (i - 2.5D) * size + 0.1D, z0 = c.z + (j - 2.5D) * size + 0.1D;
                    double x1 = x0 + size - 0.2D, z1 = z0 + size - 0.2D;
                    float a;
                    if (erupting) {
                        if (!on) {
                            continue;
                        }
                        a = 0.9F * (1F - p);
                    } else {
                        // The lit tiles quicken as they near; the dark ones only mark out the grid.
                        a = on ? (0.25F + 0.55F * p) * (0.75F + 0.25F * MathHelper.sin((age + partialTicks) * (0.4F + p))) : 0.08F;
                    }
                    buffer.pos(x0, c.y, z0).color(1F, 0.82F, 0.3F, a).endVertex();
                    buffer.pos(x0, c.y, z1).color(1F, 0.82F, 0.3F, a).endVertex();
                    buffer.pos(x1, c.y, z1).color(1F, 0.82F, 0.3F, a).endVertex();
                    buffer.pos(x1, c.y, z0).color(1F, 0.82F, 0.3F, a).endVertex();
                    if (erupting) {
                        double h = 6D * (1D - Math.pow(1D - p, 3D));
                        wall(buffer, x0, z0, x1, z0, c.y, h, a);
                        wall(buffer, x1, z0, x1, z1, c.y, h, a);
                        wall(buffer, x1, z1, x0, z1, c.y, h, a);
                        wall(buffer, x0, z1, x0, z0, c.y, h, a);
                    }
                }
            }
            tessellator.draw();
            // The grid's lines, exact and thin.
            GlStateManager.glLineWidth(1.5F);
            buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
            float la = erupting ? 0.5F * (1F - p) : 0.5F;
            for (int k = 0; k <= 5; k++) {
                double o = (k - 2.5D) * size;
                buffer.pos(c.x + o, c.y, c.z - 2.5D * size).color(1F, 0.9F, 0.55F, la).endVertex();
                buffer.pos(c.x + o, c.y, c.z + 2.5D * size).color(1F, 0.9F, 0.55F, la).endVertex();
                buffer.pos(c.x - 2.5D * size, c.y, c.z + o).color(1F, 0.9F, 0.55F, la).endVertex();
                buffer.pos(c.x + 2.5D * size, c.y, c.z + o).color(1F, 0.9F, 0.55F, la).endVertex();
            }
            tessellator.draw();
            GlStateManager.glLineWidth(1F);
        }

        private static void wall(BufferBuilder buffer, double x0, double z0, double x1, double z1, double y, double h, float a) {
            buffer.pos(x0, y, z0).color(1F, 0.85F, 0.35F, a).endVertex();
            buffer.pos(x1, y, z1).color(1F, 0.85F, 0.35F, a).endVertex();
            buffer.pos(x1, y + h, z1).color(1F, 0.95F, 0.7F, 0F).endVertex();
            buffer.pos(x0, y + h, z0).color(1F, 0.95F, 0.7F, 0F).endVertex();
        }
    }

    /** A golden line drawn across the floor, with chevrons running along it toward its middle; or, as a beam, judgement running its length. */
    public static class AlignLine extends FxRenderer.Fx {
        private final Vec3d from, to;
        private final boolean beam;

        public AlignLine(Vec3d from, Vec3d to, int ticks, boolean beam) {
            super(ticks);
            this.from = from;
            this.to = to;
            this.beam = beam;
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            float p = progress(partialTicks);
            Vec3d a = from.subtract(camera).add(0D, 0.06D, 0D), b = to.subtract(camera).add(0D, 0.06D, 0D);
            Vec3d dir = b.subtract(a).normalize();
            Vec3d side = new Vec3d(-dir.z, 0D, dir.x);
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            double width = beam ? 1.8D * (1D - p * 0.5D) : 0.35D;
            float alpha = beam ? 0.9F * (1F - p) : 0.6F + 0.3F * MathHelper.sin((age + partialTicks) * 0.5F);
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            buffer.pos(a.x - side.x * width, a.y, a.z - side.z * width).color(1F, 0.85F, 0.35F, alpha).endVertex();
            buffer.pos(a.x + side.x * width, a.y, a.z + side.z * width).color(1F, 0.85F, 0.35F, alpha).endVertex();
            buffer.pos(b.x + side.x * width, b.y, b.z + side.z * width).color(1F, 0.85F, 0.35F, alpha).endVertex();
            buffer.pos(b.x - side.x * width, b.y, b.z - side.z * width).color(1F, 0.85F, 0.35F, alpha).endVertex();
            if (beam) {
                // Upright too: a wall of light along the whole line.
                buffer.pos(a.x, a.y, a.z).color(1F, 0.9F, 0.5F, alpha).endVertex();
                buffer.pos(b.x, b.y, b.z).color(1F, 0.9F, 0.5F, alpha).endVertex();
                buffer.pos(b.x, b.y + 4D, b.z).color(1F, 0.95F, 0.7F, 0F).endVertex();
                buffer.pos(a.x, a.y + 4D, a.z).color(1F, 0.95F, 0.7F, 0F).endVertex();
            }
            tessellator.draw();
            if (!beam) {
                // Chevrons on either side, pointing in at the line: everything is drawn to it.
                GlStateManager.glLineWidth(2F);
                buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
                double length = b.subtract(a).length();
                double shift = (age + partialTicks) * 0.15D % 1D;
                for (double s = 2D; s < length; s += 4D) {
                    Vec3d on = a.add(dir.scale(s));
                    for (int k = -1; k <= 1; k += 2) {
                        double off = (4D - shift * 3D) * k;
                        Vec3d tip = on.add(side.scale(off - 0.8D * k));
                        Vec3d l = on.add(side.scale(off)).add(dir.scale(-0.6D)), r = on.add(side.scale(off)).add(dir.scale(0.6D));
                        buffer.pos(l.x, l.y, l.z).color(1F, 0.85F, 0.4F, 0.5F).endVertex();
                        buffer.pos(tip.x, tip.y, tip.z).color(1F, 0.85F, 0.4F, 0.5F).endVertex();
                        buffer.pos(tip.x, tip.y, tip.z).color(1F, 0.85F, 0.4F, 0.5F).endVertex();
                        buffer.pos(r.x, r.y, r.z).color(1F, 0.85F, 0.4F, 0.5F).endVertex();
                    }
                }
                tessellator.draw();
                GlStateManager.glLineWidth(1F);
            }
        }
    }

    /** Under your feet while his edict holds: a ring of golden runes, turning slowly, the law written round you. */
    public static class EdictRing extends FxRenderer.Fx {
        private final net.minecraft.entity.Entity on;

        public EdictRing(net.minecraft.entity.Entity on, int ticks) {
            super(ticks);
            this.on = on;
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            float fade = Math.min(1F, (lifetime - age - partialTicks) / 15F) * Math.min(1F, (age + partialTicks) / 10F);
            Vec3d c = new Vec3d(on.lastTickPosX + (on.posX - on.lastTickPosX) * partialTicks, on.lastTickPosY + (on.posY - on.lastTickPosY) * partialTicks + 0.05D,
                    on.lastTickPosZ + (on.posZ - on.lastTickPosZ) * partialTicks).subtract(camera);
            ring(c, 1.6D, 1F, 0.85F, 0.35F, 0.7F * fade);
            ring(c, 1.15D, 1F, 0.85F, 0.35F, 0.4F * fade);
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            GlStateManager.glLineWidth(1.5F);
            buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
            double spin = (age + partialTicks) * 0.02D;
            for (int i = 0; i < 12; i++) {
                double a = spin + i * Math.PI / 6D;
                double r0 = 1.22D, r1 = 1.52D;
                // Each rune: a stroke and a cross-stroke, all the same, all exactly spaced.
                Vec3d p0 = c.add(Math.cos(a) * r0, 0D, Math.sin(a) * r0), p1 = c.add(Math.cos(a) * r1, 0D, Math.sin(a) * r1);
                Vec3d q0 = c.add(Math.cos(a + 0.12D) * 1.37D, 0D, Math.sin(a + 0.12D) * 1.37D);
                Vec3d q1 = c.add(Math.cos(a - 0.12D) * 1.37D, 0D, Math.sin(a - 0.12D) * 1.37D);
                buffer.pos(p0.x, p0.y, p0.z).color(1F, 0.9F, 0.5F, 0.9F * fade).endVertex();
                buffer.pos(p1.x, p1.y, p1.z).color(1F, 0.9F, 0.5F, 0.9F * fade).endVertex();
                buffer.pos(q0.x, q0.y, q0.z).color(1F, 0.9F, 0.5F, 0.9F * fade).endVertex();
                buffer.pos(q1.x, q1.y, q1.z).color(1F, 0.9F, 0.5F, 0.9F * fade).endVertex();
            }
            tessellator.draw();
            GlStateManager.glLineWidth(1F);
        }
    }

    // ------------------------------------------------------------------ Lord Chaos, more

    /**
     * Two vast dice of deep space, tumbling down out of the air and coming to rest with
     * their faces up, the pips burning white.
     */
    public static class Dice extends FxRenderer.Fx {
        private final Vec3d at;
        private final int faces;
        private final int fall;

        public Dice(Vec3d at, int faces, int fall) {
            super(fall + 50);
            this.at = at;
            this.faces = faces;
            this.fall = fall;
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            float t = age + partialTicks;
            float landed = MathHelper.clamp(t / fall, 0F, 1F);
            float fade = Math.min(1F, (lifetime - t) / 15F);
            for (int k = 0; k < 2; k++) {
                int face = k == 0 ? faces / 10 : faces % 10;
                double size = 1.6D;
                double drop = 18D * (1D - landed * landed);
                Vec3d c = at.add(k == 0 ? -1.4D : 1.4D, size + drop, k == 0 ? 0.6D : -0.6D).subtract(camera);
                double spin = (1D - landed) * 12D + k;
                final Vec3d centre = c;
                Cosmic.draw(() -> cube(centre, size, spin), 5, 0.9F * fade, 1.3F * fade, 0.6F, 0.8F, 1F, false);
                FxRenderer.additiveState();
                cubeEdges(centre, size, spin, 0.55F, 0.85F, 1F, 0.8F * fade);
                if (landed >= 1F) {
                    pips(centre.add(0D, size + 0.02D, 0D), size, face, fade);
                }
            }
        }

        private static Vec3d corner(Vec3d c, double s, double spin, int x, int y, int z) {
            double cx = x * s, cy = y * s, cz = z * s;
            double cos = Math.cos(spin), sin = Math.sin(spin);
            double rx = cx * cos - cz * sin, rz = cx * sin + cz * cos;
            double ry = cy * Math.cos(spin * 0.7D) - rz * Math.sin(spin * 0.7D);
            rz = cy * Math.sin(spin * 0.7D) + rz * Math.cos(spin * 0.7D);
            return c.add(rx, ry, rz);
        }

        private static final int[][] FACES = {
                {-1, -1, -1, 1, -1, -1, 1, 1, -1, -1, 1, -1}, {-1, -1, 1, -1, 1, 1, 1, 1, 1, 1, -1, 1},
                {-1, -1, -1, -1, 1, -1, -1, 1, 1, -1, -1, 1}, {1, -1, -1, 1, -1, 1, 1, 1, 1, 1, 1, -1},
                {-1, -1, -1, -1, -1, 1, 1, -1, 1, 1, -1, -1}, {-1, 1, -1, 1, 1, -1, 1, 1, 1, -1, 1, 1}};

        private static void cube(Vec3d c, double s, double spin) {
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
            for (int[] f : FACES) {
                for (int v = 0; v < 4; v++) {
                    Vec3d p = corner(c, s, spin, f[v * 3], f[v * 3 + 1], f[v * 3 + 2]);
                    buffer.pos(p.x, p.y, p.z).endVertex();
                }
            }
            tessellator.draw();
        }

        private static void cubeEdges(Vec3d c, double s, double spin, float r, float g, float b, float a) {
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            GlStateManager.glLineWidth(2F);
            for (int[] f : FACES) {
                buffer.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
                for (int v = 0; v < 4; v++) {
                    Vec3d p = corner(c, s, spin, f[v * 3], f[v * 3 + 1], f[v * 3 + 2]);
                    buffer.pos(p.x, p.y, p.z).color(r, g, b, a).endVertex();
                }
                tessellator.draw();
            }
            GlStateManager.glLineWidth(1F);
        }

        /** The pips of a face, laid on top of a landed die. */
        private static void pips(Vec3d top, double s, int face, float fade) {
            int[][] layout = {{}, {4}, {0, 8}, {0, 4, 8}, {0, 2, 6, 8}, {0, 2, 4, 6, 8}, {0, 2, 3, 5, 6, 8}};
            for (int cell : layout[Math.max(0, Math.min(6, face))]) {
                double x = (cell % 3 - 1) * s * 0.55D, z = (cell / 3 - 1) * s * 0.55D;
                disc(top.add(x, 0D, z), s * 0.16D, 1F, 1F, 1F, 0.95F * fade);
            }
        }

        private static void disc(Vec3d c, double r, float red, float green, float blue, float a) {
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
            buffer.pos(c.x, c.y, c.z).color(red, green, blue, a).endVertex();
            for (int i = 0; i <= 12; i++) {
                double ang = Math.PI * 2D * i / 12D;
                buffer.pos(c.x + Math.cos(ang) * r, c.y, c.z + Math.sin(ang) * r).color(red, green, blue, a * 0.6F).endVertex();
            }
            tessellator.draw();
        }
    }

    /** A star of deep space falling, a pale blue streak behind it. */
    public static class ChaosStar extends FxRenderer.Fx {
        private final Vec3d from, to;

        public ChaosStar(Vec3d from, Vec3d to, int ticks) {
            super(ticks);
            this.from = from;
            this.to = to;
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            float p = progress(partialTicks);
            Vec3d head = from.add(to.subtract(from).scale(p)).subtract(camera);
            Vec3d tail = from.add(to.subtract(from).scale(Math.max(0F, p - 0.35F))).subtract(camera);
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            GlStateManager.glLineWidth(4F);
            buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
            buffer.pos(tail.x, tail.y, tail.z).color(0.5F, 0.75F, 1F, 0F).endVertex();
            buffer.pos(head.x, head.y, head.z).color(0.85F, 0.95F, 1F, 1F).endVertex();
            tessellator.draw();
            GlStateManager.glLineWidth(1F);
            final Vec3d at = head;
            Cosmic.draw(() -> sphere(at, 0.7D), 4, 1F, 1.5F, 0.6F, 0.8F, 1F, false);
            FxRenderer.additiveState();
            if (p > 0.6F) {
                ring(to.subtract(camera).add(0D, 0.06D, 0D), 2.6D, 0.55F, 0.85F, 1F, (p - 0.6F) * 2F);
            }
        }
    }

    /** A little rift of deep space, wandering. Each lives only until the next word of where it has got to. */
    public static class Wanderer extends FxRenderer.Fx {
        private final Vec3d at;
        private final double radius;

        public Wanderer(Vec3d at, double radius, int ticks) {
            super(ticks);
            this.at = at;
            this.radius = radius;
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            Vec3d c = at.subtract(camera);
            final double r = radius * (0.9D + 0.1D * Math.sin((age + partialTicks) * 1.3D));
            Cosmic.draw(() -> sphere(c, r), 5, 1F, 1.5F, 0.6F, 0.8F, 1F, false);
            FxRenderer.additiveState();
            ring(c.add(0D, -0.75D, 0D), r * 1.2D, 0.55F, 0.85F, 1F, 0.5F);
        }
    }

    /** Chaos's beam of deep space, crossing his brother's gold. */
    public static class ChaosBeam extends FxRenderer.Fx {
        private final Vec3d from, to;

        public ChaosBeam(Vec3d from, Vec3d to, int ticks) {
            super(ticks);
            this.from = from;
            this.to = to;
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            Vec3d a = from.subtract(camera), b = to.subtract(camera);
            Vec3d dir = b.subtract(a);
            if (dir.lengthSquared() < 1.0E-4) {
                return;
            }
            Vec3d side = dir.crossProduct(a.add(b).scale(0.5D)).normalize().scale(0.9D);
            final Vec3d s = side;
            Cosmic.draw(() -> {
                Tessellator tessellator = Tessellator.getInstance();
                BufferBuilder buffer = tessellator.getBuffer();
                buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
                buffer.pos(a.x - s.x, a.y - s.y, a.z - s.z).endVertex();
                buffer.pos(a.x + s.x, a.y + s.y, a.z + s.z).endVertex();
                buffer.pos(b.x + s.x, b.y + s.y, b.z + s.z).endVertex();
                buffer.pos(b.x - s.x, b.y - s.y, b.z - s.z).endVertex();
                tessellator.draw();
            }, 5, 0.9F, 1.5F, 0.6F, 0.8F, 1F, false);
            FxRenderer.additiveState();
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            GlStateManager.glLineWidth(2F);
            buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
            buffer.pos(a.x + side.x, a.y + side.y, a.z + side.z).color(0.55F, 0.85F, 1F, 0.8F).endVertex();
            buffer.pos(b.x + side.x, b.y + side.y, b.z + side.z).color(0.55F, 0.85F, 1F, 0.8F).endVertex();
            buffer.pos(a.x - side.x, a.y - side.y, a.z - side.z).color(0.55F, 0.85F, 1F, 0.8F).endVertex();
            buffer.pos(b.x - side.x, b.y - side.y, b.z - side.z).color(0.55F, 0.85F, 1F, 0.8F).endVertex();
            tessellator.draw();
            GlStateManager.glLineWidth(1F);
        }
    }

    // ------------------------------------------------------------------ the In-Betweener's paradoxes

    /** Two rings, one white and one black, turning through each other where a paradox takes hold. */
    public static class ParadoxRings extends FxRenderer.Fx {
        private final Vec3d at;
        private final double radius;

        public ParadoxRings(Vec3d at, double radius, int ticks) {
            super(ticks);
            this.at = at;
            this.radius = radius;
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            float p = progress(partialTicks);
            float fade = 1F - p;
            Vec3d c = at.subtract(camera).add(0D, 1D, 0D);
            double spin = (age + partialTicks) * 0.15D;
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            for (int k = 0; k < 2; k++) {
                boolean white = k == 0;
                GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                        white ? GlStateManager.DestFactor.ONE : GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                GlStateManager.glLineWidth(3F);
                float v = white ? 1F : 0.02F;
                double tilt = spin * (white ? 1D : -1D) + k * Math.PI / 2D;
                double r = radius * (0.6D + 0.4D * p);
                buffer.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
                for (int i = 0; i < 40; i++) {
                    double a = Math.PI * 2D * i / 40D;
                    double x = Math.cos(a) * r, y = Math.sin(a) * r * Math.cos(tilt), z = Math.sin(a) * r * Math.sin(tilt);
                    buffer.pos(c.x + x, c.y + y, c.z + z).color(v, v, v, 0.9F * fade).endVertex();
                }
                tessellator.draw();
            }
            GlStateManager.glLineWidth(1F);
            FxRenderer.additiveState();
        }
    }
    // ------------------------------------------------------------------ effects

    /** The glass bursting outward in a cloud of glittering shards. */
    public static class Shards extends FxRenderer.Fx {
        private final double[][] pos, vel, e1, e2;
        private final double[] angle, spin, size;

        public Shards(Vec3d centre, double radius, long seed) {
            super(100);
            Random random = new Random(seed);
            int n = 180;
            pos = new double[n][];
            vel = new double[n][];
            e1 = new double[n][];
            e2 = new double[n][];
            angle = new double[n];
            spin = new double[n];
            size = new double[n];
            for (int i = 0; i < n; i++) {
                Vec3d dir = randomUnit(random);
                Vec3d at = centre.add(dir.scale(radius));
                double speed = 0.2D + random.nextDouble() * 0.5D;
                Vec3d a = randomUnit(random);
                Vec3d b = a.crossProduct(randomUnit(random)).normalize();
                pos[i] = new double[]{at.x, at.y, at.z};
                vel[i] = new double[]{dir.x * speed, dir.y * speed + 0.1D, dir.z * speed};
                e1[i] = new double[]{a.x, a.y, a.z};
                e2[i] = new double[]{b.x, b.y, b.z};
                angle[i] = random.nextDouble() * Math.PI * 2D;
                spin[i] = (random.nextDouble() - 0.5D) * 0.6D;
                size[i] = 0.25D + random.nextDouble() * 0.7D;
            }
        }

        @Override
        void tick() {
            for (int i = 0; i < pos.length; i++) {
                for (int k = 0; k < 3; k++) {
                    pos[i][k] += vel[i][k];
                    vel[i][k] *= 0.975D;
                }
                vel[i][1] -= 0.01D;
                angle[i] += spin[i];
            }
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            float fade = 1F - progress(partialTicks);
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
            for (int i = 0; i < pos.length; i++) {
                double px = pos[i][0] + vel[i][0] * partialTicks - camera.x;
                double py = pos[i][1] + vel[i][1] * partialTicks - camera.y;
                double pz = pos[i][2] + vel[i][2] * partialTicks - camera.z;
                double a = angle[i] + spin[i] * partialTicks;
                float glint = 0.5F + 0.5F * MathHelper.sin((float) a * 3F);
                for (int k = 0; k < 3; k++) {
                    double t = a + k * 2.1D;
                    double s = size[i] * (k == 0 ? 1D : k == 1 ? 0.75D : 0.55D);
                    double cx = Math.cos(t) * s, sx = Math.sin(t) * s;
                    buffer.pos(px + e1[i][0] * cx + e2[i][0] * sx, py + e1[i][1] * cx + e2[i][1] * sx, pz + e1[i][2] * cx + e2[i][2] * sx)
                            .color(0.85F + 0.15F * glint, 0.92F, 1F, (0.35F + 0.5F * glint) * fade).endVertex();
                }
            }
            tessellator.draw();
        }
    }

    /**
     * Order and Chaos arrive: a beam of white and a beam of black lance in from where they
     * stood and meet at the junction; a sphere of light, half white and half black, bursts
     * out from the meeting and swallows everything near it.
     */
    public static class Arrival extends FxRenderer.Fx {
        private final Vec3d centre;
        private final double radius;

        public Arrival(Vec3d centre, double radius) {
            super(120);
            this.centre = centre;
            this.radius = radius;
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            float p = progress(partialTicks);
            Vec3d c = centre.subtract(camera);
            Vec3d order = centre.add(ORDER_AT).subtract(camera), chaos = centre.add(CHAOS_AT).subtract(camera);
            float lance = MathHelper.clamp(p / 0.25F, 0F, 1F);
            float fade = 1F - MathHelper.clamp((p - 0.6F) / 0.4F, 0F, 1F);
            beam(order, order.add(c.subtract(order).scale(lance)), 6D, true, fade);
            beam(chaos, chaos.add(c.subtract(chaos).scale(lance)), 6D, false, fade);
            float burst = MathHelper.clamp((p - 0.25F) / 0.35F, 0F, 1F);
            if (burst > 0F) {
                double r = radius * (1D - Math.pow(1D - burst, 3D));
                hemisphere(c, r, true, 0.55F * fade);
                hemisphere(c, r, false, 0.6F * fade);
            }
            FxRenderer.additiveState();
        }

        private static void beam(Vec3d from, Vec3d to, double width, boolean white, float alpha) {
            Vec3d dir = to.subtract(from);
            if (dir.lengthSquared() < 1.0E-4) {
                return;
            }
            Vec3d mid = from.add(to).scale(0.5D);
            Vec3d side = dir.crossProduct(mid).normalize().scale(width);
            float v = white ? 1F : 0F;
            if (white) {
                GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            } else {
                GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            }
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
            buffer.pos(from.x - side.x, from.y - side.y, from.z - side.z).color(v, v, v, 0F).endVertex();
            buffer.pos(from.x, from.y, from.z).color(v, v, v, alpha).endVertex();
            buffer.pos(to.x - side.x, to.y - side.y, to.z - side.z).color(v, v, v, 0F).endVertex();
            buffer.pos(to.x, to.y, to.z).color(v, v, v, alpha).endVertex();
            tessellator.draw();
            buffer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
            buffer.pos(from.x, from.y, from.z).color(v, v, v, alpha).endVertex();
            buffer.pos(from.x + side.x, from.y + side.y, from.z + side.z).color(v, v, v, 0F).endVertex();
            buffer.pos(to.x, to.y, to.z).color(v, v, v, alpha).endVertex();
            buffer.pos(to.x + side.x, to.y + side.y, to.z + side.z).color(v, v, v, 0F).endVertex();
            tessellator.draw();
        }

        /** Half a sphere: the white half toward Order, the black half toward Chaos. */
        private static void hemisphere(Vec3d c, double r, boolean white, float alpha) {
            if (white) {
                GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            } else {
                GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            }
            float v = white ? 1F : 0F;
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            int stacks = 12, slices = 12;
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            for (int i = 0; i < stacks; i++) {
                double t0 = Math.PI * i / stacks - Math.PI / 2D, t1 = Math.PI * (i + 1) / stacks - Math.PI / 2D;
                for (int j = 0; j < slices; j++) {
                    // Order's half faces +X, toward where he rose; Chaos's faces -X.
                    double p0 = Math.PI * j / slices - Math.PI / 2D + (white ? 0D : Math.PI);
                    double p1 = Math.PI * (j + 1) / slices - Math.PI / 2D + (white ? 0D : Math.PI);
                    vertex(buffer, c, r, t0, p0, v, alpha);
                    vertex(buffer, c, r, t0, p1, v, alpha);
                    vertex(buffer, c, r, t1, p1, v, alpha);
                    vertex(buffer, c, r, t1, p0, v, alpha);
                }
            }
            tessellator.draw();
        }

        private static void vertex(BufferBuilder buffer, Vec3d c, double r, double theta, double phi, float v, float alpha) {
            double cos = Math.cos(theta);
            buffer.pos(c.x + r * cos * Math.cos(phi), c.y + r * Math.sin(theta), c.z + r * cos * Math.sin(phi)).color(v, v, v, alpha).endVertex();
        }
    }
}
