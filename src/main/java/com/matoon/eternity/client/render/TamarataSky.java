package com.matoon.eternity.client.render;

import com.matoon.eternity.client.ChampionClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.IRenderHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.Random;

/**
 * The sky over Tamarata. Whole, it is dust: rust overhead and burnt orange at the horizon, a
 * small white sun a long way off, and low in the east a ringed giant, Tamarata's neighbour.
 * Broken, there is no sky at all, only space: stars all the way round, a glow of dust where the
 * planet was, and a band of rubble across the heavens that used to be the ground under your feet.
 */
@SideOnly(Side.CLIENT)
public class TamarataSky extends IRenderHandler {

    public static final TamarataSky INSTANCE = new TamarataSky();

    /** For clouds and weather, of which there are none. */
    public static final IRenderHandler NOTHING = new IRenderHandler() {
        @Override
        public void render(float partialTicks, WorldClient world, Minecraft mc) {
        }
    };

    private static final float[] HORIZON = {0.9F, 0.55F, 0.34F};
    private static final float[] ZENITH = {0.42F, 0.2F, 0.14F};
    private static final float[] NIGHT_HORIZON = {0.12F, 0.06F, 0.08F};
    private static final float[] NIGHT_ZENITH = {0.02F, 0.01F, 0.03F};

    private final float[][] stars;
    private final float[][] rubble;
    /** Each point's four corners, worked out once: the sky is drawn every frame. */
    private final float[] starQuads;
    private final float[] rubbleQuads;

    private TamarataSky() {
        Random random = new Random(1933L);
        stars = new float[1400][];
        for (int i = 0; i < stars.length; i++) {
            Vec3d d = new Vec3d(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).normalize();
            stars[i] = new float[]{(float) d.x, (float) d.y, (float) d.z, 0.08F + random.nextFloat() * 0.22F, 0.4F + random.nextFloat() * 0.6F,
                    random.nextFloat()};
        }
        // The rubble belt: along a great circle tipped across the sky, thicker in the middle of the band.
        rubble = new float[900][];
        for (int i = 0; i < rubble.length; i++) {
            double a = random.nextDouble() * Math.PI * 2D;
            double off = random.nextGaussian() * 0.07D;
            Vec3d d = new Vec3d(Math.cos(a), off, Math.sin(a)).normalize();
            // Tip the band 28 degrees.
            double tilt = Math.toRadians(28D);
            Vec3d t = new Vec3d(d.x, d.y * Math.cos(tilt) - d.z * Math.sin(tilt), d.y * Math.sin(tilt) + d.z * Math.cos(tilt));
            rubble[i] = new float[]{(float) t.x, (float) t.y, (float) t.z, 0.15F + random.nextFloat() * random.nextFloat() * 0.9F,
                    0.35F + random.nextFloat() * 0.35F, random.nextFloat()};
        }
        starQuads = quads(stars, 100D);
        rubbleQuads = quads(rubble, 98D);
    }

    @Override
    public void render(float partialTicks, WorldClient world, Minecraft mc) {
        boolean broken = ChampionClient.INSTANCE.broken();
        float angle = world.getCelestialAngle(partialTicks);
        float day = MathHelper.clamp(MathHelper.cos(angle * (float) Math.PI * 2F) * 2F + 0.5F, 0F, 1F);
        float time = world.getTotalWorldTime() + partialTicks;
        GlSnapshot before = GlSnapshot.take();
        GlStateManager.pushMatrix();
        GlStateManager.disableFog();
        GlStateManager.disableAlpha();
        GlStateManager.disableCull();
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        GlStateManager.enableBlend();
        if (broken) {
            space(time);
        } else {
            dome(day);
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            points(stars, starQuads, 1F - day, time, new float[]{1F, 0.95F, 0.9F});
            neighbour(day);
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            haze(day);
        }
        // The sun, small and white and a long way off: square, like every sun in this sky.
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        GlStateManager.pushMatrix();
        GlStateManager.rotate(-90F, 0F, 1F, 0F);
        GlStateManager.rotate(angle * 360F, 1F, 0F, 0F);
        Vec3d sun = new Vec3d(0D, 100D, 0D);
        square(sun, 3.2D, new float[]{1F, 1F, 0.95F}, 1F, 1F);
        square(sun, 12D, new float[]{1F, 0.85F, 0.6F}, broken ? 0.25F : 0.45F, 0F);
        square(sun, 30D, new float[]{1F, 0.6F, 0.35F}, broken ? 0.06F : 0.18F, 0F);
        GlStateManager.popMatrix();
        GlStateManager.popMatrix();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        before.restore();
        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();
    }

    /** The dusty sky, banded from the horizon up, and the dark ground beneath it. */
    private static void dome(float day) {
        float[] h = mix(NIGHT_HORIZON, HORIZON, day), z = mix(NIGHT_ZENITH, ZENITH, day);
        float[] below = {h[0] * 0.45F, h[1] * 0.4F, h[2] * 0.4F};
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        int slices = 32;
        double[] elevation = {-90D, -12D, 0D, 8D, 22D, 45D, 90D};
        for (int band = 0; band < elevation.length - 1; band++) {
            buffer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
            for (int i = 0; i <= slices; i++) {
                double a = Math.PI * 2D * i / slices;
                for (int k = 0; k <= 1; k++) {
                    double e = Math.toRadians(elevation[band + 1 - k]);
                    float[] col = colourAt(elevation[band + 1 - k], h, z, below);
                    buffer.pos(Math.cos(a) * Math.cos(e) * 100D, Math.sin(e) * 100D, Math.sin(a) * Math.cos(e) * 100D)
                            .color(col[0], col[1], col[2], 1F).endVertex();
                }
            }
            tessellator.draw();
        }
    }

    private static float[] colourAt(double elevation, float[] horizon, float[] zenith, float[] below) {
        if (elevation <= 0D) {
            return mix(below, horizon, (float) MathHelper.clamp(1D + elevation / 12D, 0D, 1D));
        }
        float f = (float) Math.pow(elevation / 90D, 0.6D);
        return mix(horizon, zenith, f);
    }

    /** A warm haze of dust lying along the horizon. */
    private static void haze(float day) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        int slices = 32;
        buffer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i <= slices; i++) {
            double a = Math.PI * 2D * i / slices;
            float alpha = 0.18F * day * (0.7F + 0.3F * MathHelper.sin((float) a * 3F));
            buffer.pos(Math.cos(a) * 100D, -2D, Math.sin(a) * 100D).color(1F, 0.7F, 0.45F, alpha).endVertex();
            buffer.pos(Math.cos(a) * 100D, 14D, Math.sin(a) * 100D).color(1F, 0.7F, 0.45F, 0F).endVertex();
        }
        tessellator.draw();
    }

    /**
     * The ringed giant low in the eastern sky, square like the sun: a face of shaded cells, lit
     * from one side and banded across, with a flat ring passing behind it and in front.
     */
    private static void neighbour(float day) {
        double az = Math.toRadians(-35D), el = Math.toRadians(24D);
        Vec3d c = new Vec3d(Math.cos(az) * Math.cos(el), Math.sin(el), Math.sin(az) * Math.cos(el)).scale(100D);
        Vec3d[] b = WarlockFx.basis(c.normalize());
        double r = 8D;
        float vis = 0.35F + 0.65F * day;
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        ring(c, b, r, vis, true);
        // The face: an eight-by-eight grid of cells, like a texture, lit from the upper left.
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        int cells = 8;
        double cell = r * 2D / cells;
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < cells; i++) {
            for (int j = 0; j < cells; j++) {
                // Lit toward one corner, falling into shadow toward the other.
                float lit = (float) MathHelper.clamp(1.05D - (i + (cells - 1 - j)) / (double) (cells * 2 - 2) * 0.95D, 0.14D, 1D);
                // Bands of cloud across it, two shades, with a darker storm in one band.
                boolean band = j % 3 == 1;
                boolean storm = j == 4 && (i == 2 || i == 3);
                float[] col = storm ? new float[]{0.55F, 0.28F, 0.36F} : band ? new float[]{0.62F, 0.4F, 0.52F} : new float[]{0.8F, 0.56F, 0.64F};
                float shade = lit * vis * (((i * 7 + j * 3) % 5 == 0) ? 0.92F : 1F);
                double u0 = -r + i * cell, v0 = -r + j * cell;
                for (int[] k : new int[][]{{0, 0}, {1, 0}, {1, 1}, {0, 1}}) {
                    Vec3d p = c.add(b[0].scale(u0 + k[0] * cell)).add(b[1].scale(v0 + k[1] * cell));
                    buffer.pos(p.x, p.y, p.z).color(col[0] * shade, col[1] * shade, col[2] * shade, 1F).endVertex();
                }
            }
        }
        tessellator.draw();
        ring(c, b, r, vis, false);
    }

    /**
     * The giant's ring: a flat rectangular frame tipped toward you, in two halves, so the far side
     * passes behind the planet and the near side in front of it.
     */
    private static void ring(Vec3d c, Vec3d[] b, double r, float vis, boolean back) {
        double outerU = r * 1.9D, innerU = r * 1.45D, tilt = 0.3D;
        double outerV = outerU * tilt, innerV = innerU * tilt;
        float alpha = 0.6F * vis;
        float[] col = {0.86F, 0.72F, 0.56F};
        // Tipped toward you, the far half of the ring shows above the middle line and the near half below.
        double side = back ? 1D : -1D;
        // The long edge on this side of the frame...
        quad(c, b, -outerU, side * innerV, outerU, side * outerV, col, alpha);
        // ...and this half of each short edge.
        quad(c, b, -outerU, 0D, -innerU, side * innerV, col, alpha);
        quad(c, b, innerU, 0D, outerU, side * innerV, col, alpha);
    }

    /** A flat quad in the plane of b, from (u0, v0) to (u1, v1) about c. */
    private static void quad(Vec3d c, Vec3d[] b, double u0, double v0, double u1, double v1, float[] col, float alpha) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        for (double[] k : new double[][]{{u0, v0}, {u1, v0}, {u1, v1}, {u0, v1}}) {
            Vec3d p = c.add(b[0].scale(k[0])).add(b[1].scale(k[1]));
            buffer.pos(p.x, p.y, p.z).color(col[0], col[1], col[2], alpha).endVertex();
        }
        tessellator.draw();
    }

    /** Space, where the planet was: stars everywhere, a glow of dust, and the rubble belt across it all. */
    private void space(float time) {
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        points(stars, starQuads, 1F, time, new float[]{1F, 0.96F, 0.92F});
        // The dust of Tamarata, glowing faintly in the light of the stone that broke it.
        Random random = new Random(77L);
        for (int i = 0; i < 14; i++) {
            float[] r = rubble[random.nextInt(rubble.length)];
            Vec3d at = new Vec3d(r[0], r[1], r[2]).scale(100D);
            disc(at, 18D + random.nextDouble() * 24D, i % 3 == 0 ? ChampionFx.VIOLET : new float[]{0.85F, 0.45F, 0.25F}, 0.05F, 0F);
        }
        points(rubble, rubbleQuads, 1F, time, new float[]{0.62F, 0.42F, 0.32F});
    }

    /** The four corners of each point's quad, facing the middle, at a distance. */
    private static float[] quads(float[][] list, double distance) {
        float[] out = new float[list.length * 12];
        int[][] corners = {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}};
        for (int i = 0; i < list.length; i++) {
            float[] s = list[i];
            Vec3d d = new Vec3d(s[0], s[1], s[2]);
            Vec3d[] b = WarlockFx.basis(d);
            Vec3d c = d.scale(distance);
            for (int k = 0; k < 4; k++) {
                Vec3d p = c.add(b[0].scale(corners[k][0] * s[3])).add(b[1].scale(corners[k][1] * s[3]));
                out[i * 12 + k * 3] = (float) p.x;
                out[i * 12 + k * 3 + 1] = (float) p.y;
                out[i * 12 + k * 3 + 2] = (float) p.z;
            }
        }
        return out;
    }

    /** Points of light (or rubble), each a soft quad facing the middle, twinkling. */
    private static void points(float[][] list, float[] quads, float visible, float time, float[] col) {
        if (visible <= 0.01F) {
            return;
        }
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < list.length; i++) {
            float[] s = list[i];
            float a = s[4] * visible * (0.75F + 0.25F * MathHelper.sin(time * 0.05F + s[5] * 40F));
            for (int k = i * 12; k < i * 12 + 12; k += 3) {
                buffer.pos(quads[k], quads[k + 1], quads[k + 2]).color(col[0], col[1], col[2], a).endVertex();
            }
        }
        tessellator.draw();
    }

    /**
     * A square of light facing the middle of the sky, as the vanilla sun is square. With edge equal to
     * alpha it is a hard tile; with edge 0 it fades from its centre out to its square rim.
     */
    private static void square(Vec3d c, double half, float[] col, float alpha, float edge) {
        Vec3d[] b = WarlockFx.basis(c.normalize());
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(c.x, c.y, c.z).color(col[0], col[1], col[2], alpha).endVertex();
        for (int[] k : new int[][]{{-1, -1}, {1, -1}, {1, 1}, {-1, 1}, {-1, -1}}) {
            Vec3d p = c.add(b[0].scale(k[0] * half)).add(b[1].scale(k[1] * half));
            buffer.pos(p.x, p.y, p.z).color(col[0], col[1], col[2], edge).endVertex();
        }
        tessellator.draw();
    }

    private static void disc(Vec3d c, double radius, float[] col, float alpha, float edge) {
        Vec3d[] b = WarlockFx.basis(c.normalize());
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(c.x, c.y, c.z).color(col[0], col[1], col[2], alpha).endVertex();
        for (int i = 0; i <= 24; i++) {
            double a = Math.PI * 2D * i / 24D;
            Vec3d p = c.add(b[0].scale(Math.cos(a) * radius)).add(b[1].scale(Math.sin(a) * radius));
            buffer.pos(p.x, p.y, p.z).color(col[0], col[1], col[2], edge).endVertex();
        }
        tessellator.draw();
    }

    private static float[] mix(float[] a, float[] b, float f) {
        return new float[]{a[0] + (b[0] - a[0]) * f, a[1] + (b[1] - a[1]) * f, a[2] + (b[2] - a[2]) * f};
    }
}
