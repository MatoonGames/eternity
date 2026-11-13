package com.matoon.eternity.client.render;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;
import java.util.Random;

/**
 * The Aether, as Bor's people sealed it: a lantern-shaped reliquary standing on the Tesseract's
 * old stand. A stepped plinth and a stepped cap of old bronze, studded with rivets; a pillar of
 * bronze up each corner, engraved from foot to cap with knots and roundels; between them panels
 * of black stone, faceted like obsidian; and a handle over the top, bent square. Round its middle
 * runs a single slit, and through it the Aether shows: red, moving too slowly to be a liquid and
 * too much to be anything else, its light bleeding out over the stone, wisps of it coming loose
 * and curling away into the dark.
 * <p>
 * Drawn camera-relative at its foot, its four faces square to the world.
 */
@SideOnly(Side.CLIENT)
public final class AetherRenderer {

    // Half the body's width, and the heights of its parts: plinth, body, the slit's middle, cap, handle.
    private static final double W = 0.245D, BASE = 0.12D, TOP = 1.02D, SLIT = 0.57D, SLIT_H = 0.032D, CAP = 1.17D, HANDLE = 1.46D;
    private static final float[] BRONZE = {0.5F, 0.4F, 0.24F};
    private static final float[] RED = {0.95F, 0.08F, 0.06F};
    private static final float[] DEEP = {0.4F, 0.0F, 0.03F};
    private static final int WISPS = 9;

    private static final FloatBuffer MODELVIEW = GLAllocation.createDirectFloatBuffer(16);

    private static DynamicTexture bronze;
    private static DynamicTexture engraved;
    private static DynamicTexture stone;
    private static DynamicTexture flow;

    private static float rx, ry, rz, ux, uy, uz;

    private AetherRenderer() {
    }

    /** Draws it standing at {@code foot}, relative to the camera, lit as the world is at {@code world}'s {@code light}. */
    public static void draw(Vec3d foot, World world, BlockPos light, float age) {
        GlSnapshot before = GlSnapshot.take();
        ensureTextures();
        float breathe = 0.5F + 0.5F * MathHelper.sin(age * 0.045F);
        float flicker = 0.85F + 0.1F * MathHelper.sin(age * 0.31F) + 0.05F * MathHelper.sin(age * 0.77F);

        GlStateManager.pushMatrix();
        GlStateManager.translate(foot.x, foot.y, foot.z);
        readView();
        GlStateManager.disableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.disableCull();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        GlStateManager.color(1F, 1F, 1F, 1F);
        // The vault's lamps light it, and it is never quite lost in the dark.
        int sky = world.getLightFor(EnumSkyBlock.SKY, light), block = Math.max(6, world.getLightFor(EnumSkyBlock.BLOCK, light));
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, block * 16F, sky * 16F);

        // The black stone.
        GlStateManager.bindTexture(stone.getGlTextureId());
        Solid.begin(2.2D);
        Solid.color(1F, 1F, 1F);
        Solid.box(-W + 0.01D, BASE, -W + 0.01D, W - 0.01D, TOP, W - 0.01D);
        Solid.end();

        // The bronze: plinth, cap, rails, rivets, handle.
        GlStateManager.bindTexture(bronze.getGlTextureId());
        Solid.begin(3D);
        Solid.color(BRONZE[0], BRONZE[1], BRONZE[2]);
        bronzework();
        Solid.end();

        // The corner pillars, engraved.
        GlStateManager.bindTexture(engraved.getGlTextureId());
        Solid.begin(1D);
        Solid.color(BRONZE[0] * 1.05F, BRONZE[1] * 1.02F, BRONZE[2] * 0.95F);
        pillars();
        Solid.end();

        // The slit, and the Aether showing through it: its own light from here on.
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.depthMask(false);
        GlStateManager.disableTexture2D();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        slits(0D, SLIT_H, DEEP, 1F, 0.002D);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        GlStateManager.enableTexture2D();
        GlStateManager.bindTexture(flow.getGlTextureId());
        // The red moving in it: two layers drifting their own ways.
        flowing(age * 0.0021F, age * 0.0007F, 1F, RED, 0.95F * flicker);
        flowing(-age * 0.0013F, age * 0.0011F, 1.7F, new float[]{1F, 0.35F, 0.2F}, 0.5F * flicker);
        GlStateManager.disableTexture2D();
        // A hot thread down its middle, and its light bleeding out over the stone above and below.
        slits(0D, SLIT_H * 0.3D, new float[]{1F, 0.55F, 0.45F}, 0.55F * flicker, 0.004D);
        bleed(0.15D + 0.04D * breathe, 0.34F + 0.14F * breathe);
        GlStateManager.popMatrix();

        WarlockFx.glow(foot.add(0D, SLIT, 0D), 1.1D + 0.2D * breathe, RED, (0.1F + 0.06F * breathe) * flicker);
        for (int i = 0; i < 4; i++) {
            double a = i * Math.PI / 2D;
            WarlockFx.glow(foot.add(Math.cos(a) * (W + 0.04D), SLIT, Math.sin(a) * (W + 0.04D)), 0.3D, RED, 0.16F * flicker);
        }
        GlStateManager.pushMatrix();
        GlStateManager.translate(foot.x, foot.y, foot.z);
        GlStateManager.enableTexture2D();
        GlStateManager.bindTexture(ProcTex.wisp().getGlTextureId());
        wisps(age);
        GlStateManager.popMatrix();
        before.restore();
    }

    // ------------------------------------------------------------------ the bronze

    private static void bronzework() {
        // The plinth, two steps, studded.
        Solid.box(-0.33D, 0D, -0.33D, 0.33D, 0.06D, 0.33D);
        Solid.box(-0.305D, 0.06D, -0.305D, 0.305D, BASE, 0.305D);
        rivets(0.33D, 0.03D, 6);
        rivets(0.305D, 0.09D, 5);
        // Rails top and bottom of every panel, and either side of the slit.
        frame(BASE, BASE + 0.035D, 0.012D);
        frame(TOP - 0.035D, TOP, 0.012D);
        frame(SLIT - SLIT_H - 0.02D, SLIT - SLIT_H, 0.008D);
        frame(SLIT + SLIT_H, SLIT + SLIT_H + 0.02D, 0.008D);
        // The cap: two steps out, one in, studded.
        Solid.box(-0.305D, TOP, -0.305D, 0.305D, TOP + 0.05D, 0.305D);
        Solid.box(-0.33D, TOP + 0.05D, -0.33D, 0.33D, TOP + 0.11D, 0.33D);
        Solid.box(-0.26D, TOP + 0.11D, -0.26D, 0.26D, CAP, 0.26D);
        rivets(0.33D, TOP + 0.08D, 6);
        // The handle, bent square, its right post stepped.
        Solid.box(-0.23D, CAP, -0.03D, -0.17D, HANDLE - 0.03D, 0.03D);
        Solid.box(0.17D, CAP, -0.03D, 0.23D, HANDLE - 0.03D, 0.03D);
        Solid.box(-0.25D, HANDLE - 0.07D, -0.035D, 0.25D, HANDLE, 0.035D);
        Solid.box(0.15D, HANDLE - 0.17D, -0.045D, 0.25D, HANDLE - 0.11D, 0.045D);
        Solid.box(-0.25D, CAP, -0.05D, -0.15D, CAP + 0.035D, 0.05D);
        Solid.box(0.15D, CAP, -0.05D, 0.25D, CAP + 0.035D, 0.05D);
    }

    /** A band of bronze round all four faces, standing out from the stone by {@code out}. */
    private static void frame(double y0, double y1, double out) {
        double e = W + out;
        Solid.box(-e, y0, W - 0.02D, e, y1, e);
        Solid.box(-e, y0, -e, e, y1, -W + 0.02D);
        Solid.box(W - 0.02D, y0, -W, e, y1, W);
        Solid.box(-e, y0, -W, -W + 0.02D, y1, W);
    }

    /** A row of studs round a step of half-width {@code half}, at height {@code y}. */
    private static void rivets(double half, double y, int each) {
        double s = 0.012D;
        for (int i = 0; i < each; i++) {
            double t = -half + 2D * half * (i + 0.5D) / each;
            Solid.boxAt(t, y, half + s * 0.5D, s * 2D, s * 2D, s * 1.2D);
            Solid.boxAt(t, y, -half - s * 0.5D, s * 2D, s * 2D, s * 1.2D);
            Solid.boxAt(half + s * 0.5D, y, t, s * 1.2D, s * 2D, s * 2D);
            Solid.boxAt(-half - s * 0.5D, y, t, s * 1.2D, s * 2D, s * 2D);
        }
    }

    /** A pillar up each corner, engraved on its two outer faces, plain on the rest. */
    private static void pillars() {
        double p = 0.055D;
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                double cx = sx * W, cz = sz * W;
                double x0 = cx - p, x1 = cx + p, z0 = cz - p, z1 = cz + p;
                double h = TOP - BASE;
                // The outer face along x, and the outer face along z.
                if (sz > 0) {
                    Solid.face(new double[]{x0, BASE, z1}, new double[]{2 * p, 0, 0}, new double[]{0, h, 0}, 0, 0, 1, 0F, 0F, 1F, 1F);
                    Solid.face(new double[]{x1, BASE, z0}, new double[]{-2 * p, 0, 0}, new double[]{0, h, 0}, 0, 0, -1, 0F, 0F, 0.08F, 0.08F);
                } else {
                    Solid.face(new double[]{x1, BASE, z0}, new double[]{-2 * p, 0, 0}, new double[]{0, h, 0}, 0, 0, -1, 0F, 0F, 1F, 1F);
                    Solid.face(new double[]{x0, BASE, z1}, new double[]{2 * p, 0, 0}, new double[]{0, h, 0}, 0, 0, 1, 0F, 0F, 0.08F, 0.08F);
                }
                if (sx > 0) {
                    Solid.face(new double[]{x1, BASE, z1}, new double[]{0, 0, -2 * p}, new double[]{0, h, 0}, 1, 0, 0, 0F, 0F, 1F, 1F);
                    Solid.face(new double[]{x0, BASE, z0}, new double[]{0, 0, 2 * p}, new double[]{0, h, 0}, -1, 0, 0, 0F, 0F, 0.08F, 0.08F);
                } else {
                    Solid.face(new double[]{x0, BASE, z0}, new double[]{0, 0, 2 * p}, new double[]{0, h, 0}, -1, 0, 0, 0F, 0F, 1F, 1F);
                    Solid.face(new double[]{x1, BASE, z1}, new double[]{0, 0, -2 * p}, new double[]{0, h, 0}, 1, 0, 0, 0F, 0F, 0.08F, 0.08F);
                }
            }
        }
    }

    // ------------------------------------------------------------------ the Aether

    /** The slit on all four faces, {@code half} high either side of its middle, standing {@code out} off the stone. */
    private static void slits(double offset, double half, float[] col, float alpha, double out) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        double e = W - 0.01D + out, l = W - 0.045D;
        double y0 = SLIT + offset - half, y1 = SLIT + offset + half;
        for (int f = 0; f < 4; f++) {
            double[][] q = side(f, e, l, y0, y1);
            for (double[] v : q) {
                buffer.pos(v[0], v[1], v[2]).color(col[0], col[1], col[2], alpha).endVertex();
            }
        }
        tessellator.draw();
    }

    /** The red moving in the slit: the flow texture drifting along it, {@code scale} times over. */
    private static void flowing(float du, float dv, float scale, float[] col, float alpha) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        double e = W - 0.01D + 0.003D, l = W - 0.045D;
        for (int f = 0; f < 4; f++) {
            double[][] q = side(f, e, l, SLIT - SLIT_H, SLIT + SLIT_H);
            float u0 = f * 0.5F * scale + du, u1 = u0 + 0.5F * scale;
            float v0 = dv, v1 = dv + 0.12F * scale;
            float[][] uv = {{u0, v1}, {u1, v1}, {u1, v0}, {u0, v0}};
            for (int i = 0; i < 4; i++) {
                buffer.pos(q[i][0], q[i][1], q[i][2]).tex(uv[i][0], uv[i][1]).color(col[0], col[1], col[2], alpha).endVertex();
            }
        }
        tessellator.draw();
    }

    /** Its light spilling out of the slit over the stone, fading away above and below it. */
    private static void bleed(double reach, float alpha) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        double e = W - 0.01D + 0.001D, l = W - 0.05D;
        for (int dir = -1; dir <= 1; dir += 2) {
            for (int f = 0; f < 4; f++) {
                double[][] q = side(f, e, l, SLIT, SLIT + dir * reach);
                for (int i = 0; i < 4; i++) {
                    boolean far = i >= 2;
                    buffer.pos(q[i][0], q[i][1], q[i][2]).color(RED[0], RED[1], RED[2], far ? 0F : alpha).endVertex();
                }
            }
        }
        tessellator.draw();
    }

    /** One face's quad, out at {@code e} from the middle and {@code l} either side, from y0 to y1. */
    private static double[][] side(int face, double e, double l, double y0, double y1) {
        switch (face) {
            case 0:
                return new double[][]{{-l, y0, e}, {l, y0, e}, {l, y1, e}, {-l, y1, e}};
            case 1:
                return new double[][]{{l, y0, -e}, {-l, y0, -e}, {-l, y1, -e}, {l, y1, -e}};
            case 2:
                return new double[][]{{e, y0, l}, {e, y0, -l}, {e, y1, -l}, {e, y1, l}};
            default:
                return new double[][]{{-e, y0, -l}, {-e, y0, l}, {-e, y1, l}, {-e, y1, -l}};
        }
    }

    /** Wisps of it coming loose from the slit and curling away, darkening as they go. */
    private static void wisps(float age) {
        for (int i = 0; i < WISPS; i++) {
            Random seed = new Random(i * 5003L + 29L);
            float life = 90F + seed.nextFloat() * 90F;
            float clock = age + seed.nextFloat() * life;
            int cycle = (int) (clock / life);
            float t = (clock - cycle * life) / life;
            Random rand = new Random(i * 86243L + cycle * 1299709L);
            int face = rand.nextInt(4);
            double along = (rand.nextDouble() * 2D - 1D) * (W - 0.06D);
            double out = W + 0.02D + 0.35D * t;
            double curl = Math.sin(t * 5D + i) * 0.12D * t;
            double y = SLIT + 0.45D * t * t + (rand.nextDouble() - 0.5D) * 0.02D;
            double x, z;
            switch (face) {
                case 0:
                    x = along + curl;
                    z = out;
                    break;
                case 1:
                    x = along + curl;
                    z = -out;
                    break;
                case 2:
                    x = out;
                    z = along + curl;
                    break;
                default:
                    x = -out;
                    z = along + curl;
                    break;
            }
            float fade = MathHelper.sin((float) Math.PI * t) * (1F - t * 0.5F);
            float r = RED[0] - 0.45F * t, g = RED[1] * (1F - t);
            sprite((float) x, (float) y, (float) z, 0.12F + 0.32F * t, rand.nextFloat() * 6.28F + t * 2F, r, g, 0.05F, 0.3F * fade);
        }
    }

    private static void sprite(float x, float y, float z, float size, float angle, float r, float g, float b, float alpha) {
        float cos = MathHelper.cos(angle) * size / 2F, sin = MathHelper.sin(angle) * size / 2F;
        float ax = rx * cos + ux * sin, ay = ry * cos + uy * sin, az = rz * cos + uz * sin;
        float bx = ux * cos - rx * sin, by = uy * cos - ry * sin, bz = uz * cos - rz * sin;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        buffer.pos(x - ax - bx, y - ay - by, z - az - bz).tex(0, 0).color(r, g, b, alpha).endVertex();
        buffer.pos(x + ax - bx, y + ay - by, z + az - bz).tex(1, 0).color(r, g, b, alpha).endVertex();
        buffer.pos(x + ax + bx, y + ay + by, z + az + bz).tex(1, 1).color(r, g, b, alpha).endVertex();
        buffer.pos(x - ax + bx, y - ay + by, z - az + bz).tex(0, 1).color(r, g, b, alpha).endVertex();
        tessellator.draw();
    }

    private static void readView() {
        GlStateManager.getFloat(GL11.GL_MODELVIEW_MATRIX, MODELVIEW);
        float[] right = {MODELVIEW.get(0), MODELVIEW.get(4), MODELVIEW.get(8)};
        float[] up = {MODELVIEW.get(1), MODELVIEW.get(5), MODELVIEW.get(9)};
        float lr = MathHelper.sqrt(right[0] * right[0] + right[1] * right[1] + right[2] * right[2]);
        float lu = MathHelper.sqrt(up[0] * up[0] + up[1] * up[1] + up[2] * up[2]);
        rx = right[0] / lr;
        ry = right[1] / lr;
        rz = right[2] / lr;
        ux = up[0] / lu;
        uy = up[1] / lu;
        uz = up[2] / lu;
    }

    // ------------------------------------------------------------------ textures

    private static void ensureTextures() {
        if (bronze != null) {
            return;
        }
        bronze = ProcTex.texture(64, (u, v) -> {
            // Old bronze: dark in its hollows, rubbed bright on its edges, a green bloom of age here and there.
            float f = ProcTex.fbm(u * 4F, v * 4F, 4, 4, 81) * 0.5F + 0.5F;
            float patina = ProcTex.smooth((ProcTex.fbm(u * 3F + 7F, v * 3F, 3, 3, 83) - 0.25F) / 0.4F) * 0.35F;
            float g = 0.6F + 0.4F * f;
            return ProcTex.argb(1F, g * (1F - patina * 0.4F), g * (1F - patina * 0.1F), g * (1F - patina * 0.3F));
        });
        engraved = ProcTex.texture(64, 256, AetherRenderer::engravedPixel);
        stone = ProcTex.texture(128, (u, v) -> {
            // Black stone gone glassy: facets each catching the light their own way, their edges sharp.
            float[] c = ProcTex.cells(u * 7F, v * 7F, 7, 91);
            float[] fine = ProcTex.cells(u * 19F, v * 19F, 19, 93);
            float facet = 0.07F + 0.11F * c[2] + 0.04F * fine[2];
            float edge = MathHelper.clamp(1F - (c[1] - c[0]) / 0.05F, 0F, 1F) * 0.16F + MathHelper.clamp(1F - (fine[1] - fine[0]) / 0.06F, 0F, 1F) * 0.05F;
            float n = ProcTex.fbm(u * 8F, v * 8F, 8, 3, 95) * 0.03F;
            float g = facet + edge + n;
            return ProcTex.argb(1F, g * 0.95F, g * 0.97F, g * 1.08F);
        });
        flow = ProcTex.texture(128, (u, v) -> {
            // Folding filaments: warped noise, bright only along its ridges.
            float x = u * 4F, y = v * 4F;
            float wx = x + 1.4F * ProcTex.fbm(x, y, 4, 3, 101), wy = y + 1.4F * ProcTex.fbm(x + 3.1F, y + 1.7F, 4, 3, 103);
            float f = ProcTex.fbm(wx, wy, 4, 4, 107);
            float ridge = 1F - Math.abs(f);
            float c = ProcTex.smooth((ridge - 0.45F) / 0.5F);
            return ProcTex.argb(1F, c, c, c);
        });
    }

    /**
     * The pillars' engraving, one pillar's height of it: a border down either side, and between
     * them roundels, knots and lozenges in turn, cut deep and dark into the bronze.
     */
    private static int engravedPixel(float u, float v) {
        float x = (u - 0.5F) * 2F;
        float cut = 0F;
        // The border.
        cut = Math.max(cut, groove(Math.abs(Math.abs(x) - 0.78F), 0.06F));
        // Motifs, five of them up the pillar, each in its own panel.
        float cell = v * 5F;
        int k = (int) cell;
        // In the same units as x: a fifth of the pillar's height, over half its width.
        float y = (cell - k - 0.5F) * 3.27F;
        cut = Math.max(cut, groove(Math.abs(Math.abs(y) - 1.55F), 0.06F));
        float r = MathHelper.sqrt(x * x + y * y);
        switch (k % 3) {
            case 0:
                // A roundel with a cross in it.
                cut = Math.max(cut, groove(Math.abs(r - 0.55F), 0.07F));
                cut = Math.max(cut, groove(Math.abs(r - 0.28F), 0.06F));
                if (r < 0.55F) {
                    cut = Math.max(cut, groove(Math.min(Math.abs(x), Math.abs(y)), 0.05F));
                }
                break;
            case 1:
                // A knot: two loops crossing.
                cut = Math.max(cut, groove(Math.abs(MathHelper.sqrt(x * x + (y - 0.35F) * (y - 0.35F)) - 0.38F), 0.06F));
                cut = Math.max(cut, groove(Math.abs(MathHelper.sqrt(x * x + (y + 0.35F) * (y + 0.35F)) - 0.38F), 0.06F));
                break;
            default:
                // A lozenge with a stud.
                cut = Math.max(cut, groove(Math.abs(Math.abs(x) + Math.abs(y) * 0.6F - 0.55F), 0.06F));
                cut = Math.max(cut, r < 0.14F ? 0.6F : 0F);
                break;
        }
        float f = ProcTex.fbm(u * 2F, v * 8F, 2, 3, 111) * 0.5F + 0.5F;
        float g = (0.72F + 0.28F * f) * (1F - 0.75F * cut);
        return ProcTex.argb(1F, g, g * 0.98F, g * 0.92F);
    }

    private static float groove(float d, float width) {
        return MathHelper.clamp(1F - (d - width) / 0.04F, 0F, 1F);
    }
}
