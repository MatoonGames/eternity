package com.matoon.eternity.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
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
 * The Cosmic Comet: a cratered blue rock, lit from within through glowing veins,
 * wrapped in a small cyan coma. Behind it streams a narrow blue ion tail and a broad,
 * wispy dust tail that fans away to one side, pale rose near the head and fading
 * to grey-blue. Seen from beyond the far plane it is pulled in along its true
 * direction and scaled down by the same amount, so it keeps its true size in view.
 */
@SideOnly(Side.CLIENT)
public final class CometRenderer {

    private static final int STACKS = 24;
    private static final int SLICES = 36;
    /** Points of the rock on a unit scale, pushed into ridges and dished into craters. */
    private static final Vec3d[][] SURFACE = new Vec3d[STACKS + 1][SLICES + 1];
    private static final Vec3d[][] NORMALS = new Vec3d[STACKS + 1][SLICES + 1];
    /** Mottling, so the rock is never one flat colour. */
    private static final float[][] SHADE = new float[STACKS + 1][SLICES + 1];
    /** Veins of light running through the rock, as polylines just above the surface. */
    private static final List<Vec3d[]> VEINS = new ArrayList<>();
    private static final Vec3d SPIN_AXIS = new Vec3d(0.3D, 1D, 0.2D).normalize();
    private static final Vec3d LIGHT = new Vec3d(0.45D, 0.75D, 0.35D).normalize();
    private static final Vec3d UP = new Vec3d(0, 1, 0);
    /** How much bigger than the rock the tails are drawn. The rock is small; the tails are not. */
    private static final double TRAIL = 3.5D;
    /** Length of the ion tail, the longest, in tail units. */
    private static final double ION_LENGTH = 120D;

    static {
        Random random = new Random(7331L);
        Vec3d[] lumps = new Vec3d[22];
        double[] heights = new double[lumps.length];
        for (int k = 0; k < lumps.length; k++) {
            lumps[k] = randomUnit(random);
            heights[k] = (random.nextDouble() - 0.35D) * 0.45D;
        }
        Vec3d[] craters = new Vec3d[10];
        double[] widths = new double[craters.length];
        for (int k = 0; k < craters.length; k++) {
            craters[k] = randomUnit(random);
            widths[k] = 0.12D + random.nextDouble() * 0.22D;
        }
        for (int i = 0; i <= STACKS; i++) {
            double theta = Math.PI * i / STACKS - Math.PI / 2D;
            for (int j = 0; j <= SLICES; j++) {
                double phi = Math.PI * 2D * (j % SLICES) / SLICES;
                Vec3d dir = new Vec3d(Math.cos(theta) * Math.cos(phi), Math.sin(theta), Math.cos(theta) * Math.sin(phi));
                SURFACE[i][j] = dir.scale(surfaceRadius(dir, lumps, heights, craters, widths));
                SHADE[i][j] = 0.75F + random.nextFloat() * 0.3F;
            }
            SHADE[i][SLICES] = SHADE[i][0];
        }
        for (int i = 0; i <= STACKS; i++) {
            for (int j = 0; j <= SLICES; j++) {
                Vec3d radial = SURFACE[i][j].normalize();
                if (i == 0 || i == STACKS) {
                    NORMALS[i][j] = radial;
                    continue;
                }
                Vec3d along = SURFACE[i][(j + 1) % SLICES].subtract(SURFACE[i][(j + SLICES - 1) % SLICES]);
                Vec3d up = SURFACE[i + 1][j].subtract(SURFACE[i - 1][j]);
                Vec3d n = up.crossProduct(along);
                n = n.lengthSquared() < 1.0E-10 ? radial : n.normalize();
                NORMALS[i][j] = n.dotProduct(radial) < 0D ? n.scale(-1D) : n;
            }
        }
        for (int k = 0; k < 22; k++) {
            Vec3d p = randomUnit(random);
            Vec3d heading = tangent(p, randomUnit(random));
            Vec3d[] vein = new Vec3d[7 + random.nextInt(6)];
            for (int n = 0; n < vein.length; n++) {
                vein[n] = p.scale(surfaceRadius(p, lumps, heights, craters, widths) * 1.015D);
                heading = tangent(p, heading.add(randomUnit(random).scale(0.6D)));
                p = p.add(heading.scale(0.13D)).normalize();
            }
            VEINS.add(vein);
        }
    }

    private CometRenderer() {
    }

    /**
     * @param rel      centre of the rock, relative to the camera
     * @param heading  direction it is travelling; the tails stream the other way
     * @param cracking 0 until it begins to break open, then rising to 1 as it does
     * @param fade     overall opacity, for arriving and leaving
     */
    public static void render(Vec3d rel, Vec3d heading, float radius, float integrity, float cracking, float fade, float time) {
        double[] p = project(rel);
        Vec3d c = new Vec3d(p[0], p[1], p[2]);
        double size = radius * p[3];
        Vec3d back = heading.lengthSquared() < 1.0E-8 ? new Vec3d(0.3D, -0.15D, 1D) : heading.scale(-1D);
        // A little lift, so a comet crossing the sky never shows its tails edge on.
        back = back.normalize().add(0D, 0.12D, 0D).normalize();
        float wound = 1F - integrity;
        float angle = time * 0.004F;

        GlStateManager.pushMatrix();
        GlStateManager.disableFog();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.disableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
        additive();
        GlStateManager.disableTexture2D();

        tails(c, back, radius * TRAIL * tailScale(rel, radius), fade, time);

        Vec3d[] basis = basis(c);
        float pulse = 0.92F + 0.08F * MathHelper.sin(time * 0.07F);
        halo(c, basis, size * 6D, 0.3F, 0.6F, 1F, 0.18F * fade);
        halo(c, basis, size * 3D, 0.5F, 0.85F, 1F, (0.35F + cracking * 0.4F) * fade * pulse);
        halo(c, basis, size * 1.6D, 0.8F, 1F, 1F, 0.5F * fade * pulse);

        rock(c, size, angle, fade, wound, cracking);

        additive();
        GlStateManager.disableTexture2D();
        rim(transform(c, size * 1.04D, angle), c, fade, wound, cracking);
        veins(c, size, angle, wound, cracking, fade, time);

        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.depthMask(true);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.enableCull();
        GlStateManager.enableLighting();
        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.popMatrix();
    }

    /**
     * Pulls anything past half the far plane in along its true direction, scaling it
     * by the same amount, so it looks exactly as big as it really is from there.
     * Returns {x, y, z, scale}.
     */
    private static double[] project(Vec3d rel) {
        double limit = RiftRenderer.farPlane() * 0.55D;
        double dist = rel.length();
        if (dist <= limit) {
            return new double[]{rel.x, rel.y, rel.z, 1D};
        }
        double k = limit / dist;
        return new double[]{rel.x * k, rel.y * k, rel.z * k, k};
    }

    /**
     * Scale for the tails: the same as the rock, but shortened further if needed so
     * the longest one ends inside the far clip.
     */
    private static double tailScale(Vec3d rel, double radius) {
        double far = RiftRenderer.farPlane();
        double limit = far * 0.55D;
        double dist = rel.length();
        if (dist <= limit) {
            return 1D;
        }
        return Math.min(limit / dist, (far * 1.3D - limit) / (radius * TRAIL * ION_LENGTH));
    }

    // ------------------------------------------------------------------ tails

    /**
     * A narrow, straight blue ion tail, a broad dust tail fanning away to one side in
     * wisps, a white-hot sheath right behind the rock, and glints of ice streaming
     * down the whole length.
     */
    private static void tails(Vec3d c, Vec3d back, double size, float fade, float time) {
        // size here is the tail unit, TRAIL times the radius of the rock.
        Vec3d side = back.crossProduct(UP);
        side = side.lengthSquared() < 1.0E-6 ? new Vec3d(1, 0, 0) : side.normalize();
        Vec3d lift = side.crossProduct(back).normalize();

        // The dust tail: a broad pale sheath, streaked through with fainter strands that drift as it flies.
        double dust = size * 85D;
        Vec3d fanDir = back.add(side.scale(0.28D)).normalize();
        tail(c, fanDir, side.scale(0.18D), dust, size * 1.2D, dust * 0.24D,
                1F, 0.8F, 0.86F, 0.5F, 0.55F, 0.88F, 0.32F * fade, time, 0.9F);
        tail(c, fanDir, side.scale(0.12D), dust * 0.55D, size, dust * 0.1D,
                1F, 0.88F, 0.92F, 0.75F, 0.7F, 0.95F, 0.3F * fade, time, 0.9F);
        Random strands = new Random(97L);
        for (int k = 0; k < 22; k++) {
            double spread = strands.nextDouble() * 0.6D;
            float sway = MathHelper.sin(time * 0.006F + k * 1.3F);
            Vec3d dir = back.add(side.scale(spread)).normalize();
            double length = dust * (0.55D + strands.nextDouble() * 0.6D);
            Vec3d bend = side.scale(0.1D + spread * 0.3D + sway * 0.03D).add(lift.scale((strands.nextDouble() - 0.5D) * 0.08D));
            tail(c, dir, bend, length, size * 0.3D, length * 0.03D, 1F, 0.86F, 0.92F, 0.45F, 0.5F, 0.88F,
                    (0.07F + strands.nextFloat() * 0.09F) * fade, time + k * 31F, 0.7F);
        }

        // The ion tail: straight as a ruler, deep blue, and longer than everything else.
        double ion = size * ION_LENGTH;
        Vec3d ionDir = back.subtract(side.scale(0.04D)).normalize();
        tail(c, ionDir, Vec3d.ZERO, ion, size * 0.5D, size * 4D,
                0.5F, 0.72F, 1F, 0.15F, 0.3F, 0.95F, 0.6F * fade, time, 0.8F);
        for (int k = 0; k < 6; k++) {
            float t = time * 0.01F + k * 1.7F;
            Vec3d dir = ionDir.add(side.scale((k - 2.5D) * 0.01D)).normalize();
            Vec3d bend = side.scale(MathHelper.sin(t) * 0.015D).add(lift.scale(MathHelper.cos(t * 0.7F) * 0.015D));
            tail(c, dir, bend, ion * (0.7D + k * 0.06D), size * 0.1D, size * 0.9D,
                    0.65F, 0.85F, 1F, 0.2F, 0.4F, 1F, 0.3F * fade, time + k * 50F, 0.55F);
        }

        // Right behind the rock, the gas boiling off it burns white.
        tail(c, back, Vec3d.ZERO, size * 10D, size * 0.35D, size * 1.6D,
                0.9F, 0.97F, 1F, 0.4F, 0.7F, 1F, 0.65F * fade, time, 0.85F);

        // Glints of ice streaming away down the tails.
        Random glints = new Random(1234L);
        for (int k = 0; k < 70; k++) {
            float speed = 0.0012F + glints.nextFloat() * 0.002F;
            float u = (glints.nextFloat() + time * speed) % 1F;
            double spread = glints.nextDouble() * 0.55D;
            double glintSize = size * (0.12D + glints.nextDouble() * 0.22D);
            Vec3d dir = back.add(side.scale(spread)).add(lift.scale((glints.nextDouble() - 0.5D) * 0.1D)).normalize();
            Vec3d at = c.add(dir.scale(dust * u)).add(side.scale(dust * 0.15D * u * u));
            float a = 0.7F * (float) Math.pow(1F - u, 1.5D) * Math.min(1F, u * 20F) * fade;
            halo(at, basis(at), glintSize * (1D - u * 0.5D), 0.85F, 0.93F, 1F, a);
        }
    }

    /**
     * One tail: a camera-facing strip that widens, cools and fades along a gently
     * bent path. {@code calm} is how steady its light is; lower ripples more.
     */
    private static void tail(Vec3d c, Vec3d dir, Vec3d bend, double length, double w0, double w1,
                             float r0, float g0, float b0, float r1, float g1, float b1, float alpha, float time, float calm) {
        int n = 48;
        Vec3d[] points = new Vec3d[n + 1];
        for (int i = 0; i <= n; i++) {
            double u = i / (double) n;
            points[i] = c.add(dir.scale(length * u)).add(bend.scale(length * u * u));
        }
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        for (int half = -1; half <= 1; half += 2) {
            buffer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
            for (int i = 0; i <= n; i++) {
                float u = i / (float) n;
                Vec3d along = i < n ? points[i + 1].subtract(points[i]) : points[i].subtract(points[i - 1]);
                Vec3d across = along.crossProduct(points[i]);
                across = across.lengthSquared() < 1.0E-10 ? UP : across.normalize();
                double width = w0 + (w1 - w0) * Math.sqrt(u);
                float flow = calm + (1F - calm) * MathHelper.sin(u * 26F - time * 0.12F);
                float a = alpha * (float) Math.pow(1F - u, 1.2D) * flow * Math.min(1F, 0.2F + u * 14F);
                float r = r0 + (r1 - r0) * u;
                float g = g0 + (g1 - g0) * u;
                float b = b0 + (b1 - b0) * u;
                Vec3d edge = points[i].add(across.scale(width * half));
                buffer.pos(points[i].x, points[i].y, points[i].z).color(r, g, b, a).endVertex();
                buffer.pos(edge.x, edge.y, edge.z).color(r, g, b, 0F).endVertex();
            }
            tessellator.draw();
        }
    }

    // ------------------------------------------------------------------ the rock

    /** Stone, tinted the blue of deep ice, lit from one side and glowing faintly from within. */
    private static void rock(Vec3d c, double size, float angle, float fade, float wound, float cracking) {
        TextureAtlasSprite stone = stone();
        Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.depthMask(true);
        float glow = 0.12F + wound * 0.12F + cracking * 0.6F;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        Vec3d[][] verts = transform(c, size, angle);
        for (int i = 0; i < STACKS; i++) {
            for (int j = 0; j < SLICES; j++) {
                rockVertex(buffer, verts, i, j, stone.getMinU(), stone.getMinV(), angle, glow, fade);
                rockVertex(buffer, verts, i, j + 1, stone.getMaxU(), stone.getMinV(), angle, glow, fade);
                rockVertex(buffer, verts, i + 1, j + 1, stone.getMaxU(), stone.getMaxV(), angle, glow, fade);
                rockVertex(buffer, verts, i + 1, j, stone.getMinU(), stone.getMaxV(), angle, glow, fade);
            }
        }
        tessellator.draw();
    }

    private static void rockVertex(BufferBuilder buffer, Vec3d[][] verts, int i, int j, double u, double v,
                                   float angle, float glow, float fade) {
        Vec3d at = verts[i][j];
        float light = (float) Math.max(0D, rotate(NORMALS[i][j], angle).dotProduct(LIGHT));
        float shade = SHADE[i][j] * (0.22F + 0.78F * light);
        float r = Math.min(1F, 0.38F * shade + 0.06F * glow);
        float g = Math.min(1F, 0.5F * shade + 0.25F * glow);
        float b = Math.min(1F, 0.78F * shade + 0.55F * glow);
        buffer.pos(at.x, at.y, at.z).tex(u, v).color(r, g, b, fade).endVertex();
    }

    /** Blue light wrapping its silhouette, turning white-hot as it breaks. */
    private static void rim(Vec3d[][] verts, Vec3d c, float fade, float wound, float cracking) {
        float heat = Math.min(1F, wound * 0.3F + cracking);
        float r = 0.35F + 0.6F * heat;
        float g = 0.65F + 0.3F * heat;
        float b = 1F;
        float glow = cracking * 0.35F;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < STACKS; i++) {
            for (int j = 0; j < SLICES; j++) {
                rimVertex(buffer, verts[i][j], c, r, g, b, glow, fade);
                rimVertex(buffer, verts[i][j + 1], c, r, g, b, glow, fade);
                rimVertex(buffer, verts[i + 1][j + 1], c, r, g, b, glow, fade);
                rimVertex(buffer, verts[i + 1][j], c, r, g, b, glow, fade);
            }
        }
        tessellator.draw();
    }

    private static void rimVertex(BufferBuilder buffer, Vec3d v, Vec3d c, float r, float g, float b, float glow, float fade) {
        Vec3d normal = v.subtract(c).normalize();
        double facing = Math.abs(normal.dotProduct(v.normalize()));
        float edge = (float) ((1D - facing) * (1D - facing) * (1D - facing));
        buffer.pos(v.x, v.y, v.z).color(r, g, b, Math.min(1F, edge * 0.9F + glow) * fade).endVertex();
    }

    /**
     * Veins of blue light. A few always glow; more open, and burn brighter, the more
     * it is hurt. As it breaks, light bursts out of every one.
     */
    private static void veins(Vec3d c, double size, float angle, float wound, float cracking, float fade, float time) {
        int open = cracking > 0F ? VEINS.size() : (int) Math.ceil(VEINS.size() * (0.35F + 0.65F * Math.min(1F, wound * 1.3F)));
        float hot = Math.min(1F, wound * 0.6F + cracking);
        for (int k = 0; k < open; k++) {
            Vec3d[] vein = VEINS.get(k);
            float flicker = 0.8F + 0.2F * MathHelper.sin(time * 0.15F + k * 1.7F);
            float bright = Math.min(1F, (0.45F + wound * 0.55F + cracking) * flicker) * fade;
            double width = size * (0.025D + 0.045D * cracking);
            Vec3d prev = null;
            for (Vec3d point : vein) {
                Vec3d at = c.add(rotate(point, angle).scale(size));
                if (prev != null) {
                    ribbon(prev, at, width * 3D, 0.2F, 0.45F, 1F, 0.35F * bright);
                    ribbon(prev, at, width, 0.6F + 0.4F * hot, 0.85F + 0.15F * hot, 1F, bright);
                }
                prev = at;
            }
            if (cracking > 0F) {
                Vec3d mid = c.add(rotate(vein[vein.length / 2], angle).scale(size));
                Vec3d out = mid.subtract(c).normalize();
                ribbon(mid, mid.add(out.scale(size * (0.5D + 4D * cracking))), size * 0.12D * cracking, 0.85F, 0.95F, 1F, cracking * fade);
            }
        }
    }

    // ------------------------------------------------------------------ glare

    /**
     * Its light in your eyes: a starburst and a long blue streak across it, and ghosts
     * of it strung along the line through the middle of your view. Drawn over
     * everything; the caller only asks when nothing stands in the way.
     */
    public static void flare(Vec3d rel, Vec3d look, float radius, float intensity, float time) {
        double[] p = project(rel);
        Vec3d c = new Vec3d(p[0], p[1], p[2]);
        double d = c.length();
        Vec3d middle = look.normalize().scale(d);
        Vec3d[] basis = basis(c);
        float i = intensity;

        GlStateManager.pushMatrix();
        GlStateManager.disableDepth();
        GlStateManager.disableFog();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.disableAlpha();
        GlStateManager.disableTexture2D();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        additive();

        halo(c, basis, d * 0.22D * i, 0.35F, 0.6F, 1F, 0.35F * i);
        halo(c, basis, d * 0.07D * (0.6D + i), 0.8F, 0.92F, 1F, 0.8F * i);
        halo(c, basis, d * 0.025D * (0.6D + i), 1F, 1F, 1F, i);
        // The starburst, turning slowly.
        for (int k = 0; k < 8; k++) {
            double a = Math.PI * k / 4D + time * 0.002D;
            double length = d * (k % 2 == 0 ? 0.3D : 0.16D) * i;
            Vec3d ray = basis[0].scale(Math.cos(a) * length).add(basis[1].scale(Math.sin(a) * length));
            ribbon(c.subtract(ray), c.add(ray), d * 0.004D, 0.85F, 0.93F, 1F, 0.55F * i);
        }
        // The anamorphic streak.
        Vec3d streak = basis[0].scale(d * 0.9D * i);
        ribbon(c.subtract(streak), c.add(streak), d * 0.006D, 0.45F, 0.65F, 1F, 0.6F * i);
        // Ghosts along the line through the middle of the view: {position, size, r, g, b}.
        float[][] ghosts = {{0.45F, 0.02F, 0.4F, 0.7F, 1F}, {0.8F, 0.035F, 0.6F, 0.5F, 1F}, {1.25F, 0.015F, 0.5F, 1F, 0.8F},
                {1.6F, 0.05F, 0.3F, 0.5F, 1F}, {2.1F, 0.03F, 0.8F, 0.6F, 1F}};
        for (float[] g : ghosts) {
            Vec3d at = c.add(middle.subtract(c).scale(g[0]));
            halo(at, basis(at), d * g[1] * (0.7D + i * 0.6D), g[2], g[3], g[4], 0.16F * i);
        }

        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.depthMask(true);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableBlend();
        GlStateManager.enableDepth();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.enableCull();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }

    // ------------------------------------------------------------------ what it leaves behind

    /**
     * The Astral Regulator where it hangs: a beacon of light reaching up and down, and a
     * glow seen through anything, from any distance. From far away the crystal itself
     * is drawn too, since its entity only renders up close.
     */
    public static void relic(Vec3d rel, float time) {
        double dist = rel.length();
        double[] p = project(rel);
        Vec3d c = new Vec3d(p[0], p[1], p[2]);
        float pulse = 0.8F + 0.2F * MathHelper.sin(time * 0.12F);

        GlStateManager.pushMatrix();
        GlStateManager.disableFog();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.disableAlpha();
        GlStateManager.disableTexture2D();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
        additive();

        Vec3d up = UP.scale(Math.max(60D, dist * 0.6D) * p[3]);
        ribbon(c.subtract(up), c.add(up), Math.max(0.6D, dist * 0.004D) * p[3], 0.5F, 0.8F, 1F, 0.45F * pulse);
        ribbon(c.subtract(up), c.add(up), Math.max(0.15D, dist * 0.001D) * p[3], 1F, 1F, 1F, 0.8F * pulse);
        GlStateManager.disableDepth();
        Vec3d[] basis = basis(c);
        halo(c, basis, (3D + dist * 0.02D) * p[3], 0.45F, 0.75F, 1F, 0.45F * pulse);
        halo(c, basis, (1.2D + dist * 0.006D) * p[3], 0.9F, 0.97F, 1F, 0.9F * pulse);
        GlStateManager.enableDepth();

        if (dist > 40D) {
            // Kept to a readable size however far away it is.
            GlStateManager.translate(c.x, c.y, c.z);
            GlStateManager.rotate(time * 1.5F, 0F, 1F, 0F);
            OrbRenderer.draw((float) (0.5D * Math.max(1D, dist / 120D) * p[3]), time, 0F);
        }

        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.depthMask(true);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.enableCull();
        GlStateManager.enableLighting();
        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.popMatrix();
    }

    // ------------------------------------------------------------------ after it breaks

    /** Pieces of the rock flung outward, still glowing blue at their broken edges. */
    public static class Shards extends FxRenderer.Fx {
        final Vec3d centre;
        final double radius;
        final Vec3d[] dirs;
        final double[] reach;
        final double[] sizes;
        final Vec3d[][] shapes;
        final Vec3d[] axes;
        final float[] spins;

        public Shards(Vec3d centre, double radius) {
            super(260);
            this.centre = centre;
            this.radius = radius;
            Random random = new Random();
            int n = 56;
            dirs = new Vec3d[n];
            reach = new double[n];
            sizes = new double[n];
            shapes = new Vec3d[n][];
            axes = new Vec3d[n];
            spins = new float[n];
            for (int i = 0; i < n; i++) {
                dirs[i] = randomUnit(random);
                reach[i] = radius * (2D + random.nextDouble() * 7D);
                sizes[i] = radius * (0.08D + random.nextDouble() * 0.22D);
                axes[i] = randomUnit(random);
                spins[i] = (random.nextFloat() - 0.5F) * 0.3F;
                Vec3d[] shape = new Vec3d[4];
                for (int v = 0; v < 4; v++) {
                    shape[v] = randomUnit(random).scale(0.6D + random.nextDouble() * 0.6D);
                }
                shapes[i] = shape;
            }
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            float p = progress(partialTicks);
            float fade = Math.min(1F, (1F - p) * 3F);
            double[] pr = project(centre.subtract(camera));
            Vec3d c = new Vec3d(pr[0], pr[1], pr[2]);
            double s = pr[3];
            double travel = 1D - Math.pow(1D - p, 2.5D);
            float age = this.age + partialTicks;
            int n = dirs.length;
            Vec3d[] positions = new Vec3d[n];
            for (int i = 0; i < n; i++) {
                positions[i] = c.add(dirs[i].scale(reach[i] * travel * s));
            }

            TextureAtlasSprite stone = stone();
            Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
            GlStateManager.enableTexture2D();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            GlStateManager.depthMask(true);
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_TEX_COLOR);
            for (int i = 0; i < n; i++) {
                Vec3d[] q = new Vec3d[4];
                for (int v = 0; v < 4; v++) {
                    q[v] = positions[i].add(rotate(shapes[i][v], axes[i], age * spins[i]).scale(sizes[i] * s));
                }
                float cool = 0.25F + 0.75F * (1F - p);
                shardFace(buffer, stone, q[0], q[1], q[2], 0.5F, cool, fade);
                shardFace(buffer, stone, q[0], q[1], q[3], 0.35F, cool, fade);
                shardFace(buffer, stone, q[0], q[2], q[3], 0.65F, cool, fade);
                shardFace(buffer, stone, q[1], q[2], q[3], 0.45F, cool, fade);
            }
            tessellator.draw();

            additive();
            GlStateManager.disableTexture2D();
            float glow = (1F - p) * (1F - p);
            for (int i = 0; i < n; i++) {
                Vec3d trail = positions[i].subtract(dirs[i].scale(sizes[i] * s * 6D * (1D - p)));
                ribbon(trail, positions[i], sizes[i] * s * 0.6D, 0.4F, 0.7F, 1F, 0.5F * glow);
                halo(positions[i], basis(positions[i]), sizes[i] * s * 2.5D, 0.5F, 0.8F, 1F, 0.5F * glow);
            }
        }

        private static void shardFace(BufferBuilder buffer, TextureAtlasSprite stone, Vec3d a, Vec3d b, Vec3d c,
                                      float light, float glow, float fade) {
            float r = 0.38F * light + 0.05F * glow;
            float g = 0.5F * light + 0.2F * glow;
            float bl = 0.78F * light + 0.45F * glow;
            buffer.pos(a.x, a.y, a.z).tex(stone.getMinU(), stone.getMinV()).color(r, g, bl, fade).endVertex();
            buffer.pos(b.x, b.y, b.z).tex(stone.getMaxU(), stone.getMinV()).color(r, g, bl, fade).endVertex();
            buffer.pos(c.x, c.y, c.z).tex(stone.getMaxU(), stone.getMaxV()).color(r, g, bl, fade).endVertex();
        }
    }

    /** The cloud of dust and ice it leaves behind, swelling and slowly going out. */
    public static class Nebula extends FxRenderer.Fx {
        final Vec3d centre;
        final double radius;
        final Vec3d[] puffs;
        final float[][] colours;

        public Nebula(Vec3d centre, double radius) {
            super(900);
            this.centre = centre;
            this.radius = radius;
            Random random = new Random();
            float[][] palette = {{0.4F, 0.6F, 1F}, {0.7F, 0.85F, 1F}, {0.55F, 0.45F, 1F}, {1F, 0.82F, 0.88F}};
            puffs = new Vec3d[18];
            colours = new float[puffs.length][];
            for (int i = 0; i < puffs.length; i++) {
                puffs[i] = randomUnit(random).scale(random.nextDouble());
                colours[i] = palette[random.nextInt(palette.length)];
            }
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            float p = progress(partialTicks);
            double grow = 1D - Math.pow(1D - p, 3D);
            float alpha = Math.min(1F, p * 20F) * (float) Math.pow(1F - p, 1.5D);
            double[] pr = project(centre.subtract(camera));
            Vec3d c = new Vec3d(pr[0], pr[1], pr[2]);
            double s = pr[3];
            halo(c, basis(c), radius * (3D + grow * 9D) * s, 0.7F, 0.85F, 1F, 0.45F * alpha);
            for (int i = 0; i < puffs.length; i++) {
                Vec3d at = c.add(puffs[i].scale(radius * (1.5D + grow * 7D) * s));
                float[] col = colours[i];
                halo(at, basis(at), radius * (1D + grow * 3.5D) * s, col[0], col[1], col[2], 0.28F * alpha);
            }
        }
    }

    /** A pillar of light marking where the Regulator hangs. */
    public static class Reveal extends FxRenderer.Fx {
        final Vec3d centre;
        final double radius;

        public Reveal(Vec3d centre, double radius) {
            super(500);
            this.centre = centre;
            this.radius = radius;
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            float p = progress(partialTicks);
            float time = age + partialTicks;
            float alpha = Math.min(1F, p * 10F) * (1F - p) * (0.85F + 0.15F * MathHelper.sin(time * 0.3F));
            double[] pr = project(centre.subtract(camera));
            Vec3d c = new Vec3d(pr[0], pr[1], pr[2]);
            double s = pr[3];
            Vec3d up = UP.scale(radius * 8D * s);
            ribbon(c.subtract(up), c.add(up), radius * 0.35D * s, 0.6F, 0.85F, 1F, 0.5F * alpha);
            ribbon(c.subtract(up), c.add(up), radius * 0.08D * s, 1F, 1F, 1F, alpha);
            halo(c, basis(c), radius * 1.6D * s, 1F, 1F, 1F, 0.7F * alpha);
        }
    }

    // ------------------------------------------------------------------ geometry

    private static TextureAtlasSprite stone() {
        return Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite("minecraft:blocks/stone");
    }

    private static void additive() {
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        GlStateManager.depthMask(false);
    }

    /** Right and up vectors for a billboard at a camera-relative point. */
    private static Vec3d[] basis(Vec3d at) {
        Vec3d forward = at.lengthSquared() < 1.0E-8 ? new Vec3d(0, 0, 1) : at.normalize();
        Vec3d right = forward.crossProduct(UP);
        right = right.lengthSquared() < 1.0E-6 ? new Vec3d(1, 0, 0) : right.normalize();
        return new Vec3d[]{right, right.crossProduct(forward).normalize()};
    }

    /** A soft disc of light facing the camera. */
    private static void halo(Vec3d c, Vec3d[] basis, double radius, float r, float g, float b, float alpha) {
        if (alpha <= 0.002F) {
            return;
        }
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(c.x, c.y, c.z).color(r, g, b, alpha).endVertex();
        for (int i = 0; i <= 24; i++) {
            double a = Math.PI * 2D * i / 24D;
            Vec3d edge = c.add(basis[0].scale(Math.cos(a) * radius)).add(basis[1].scale(Math.sin(a) * radius));
            buffer.pos(edge.x, edge.y, edge.z).color(r, g, b, 0F).endVertex();
        }
        tessellator.draw();
    }

    /** Camera-facing strip between two camera-relative points, bright down its middle. */
    private static void ribbon(Vec3d a, Vec3d b, double width, float r, float g, float bl, float alpha) {
        if (alpha <= 0.002F) {
            return;
        }
        Vec3d side = b.subtract(a).crossProduct(a.add(b).scale(0.5D));
        side = side.lengthSquared() < 1.0E-10 ? UP.scale(width) : side.normalize().scale(width);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        for (int half = -1; half <= 1; half += 2) {
            buffer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
            buffer.pos(a.x + side.x * half, a.y + side.y * half, a.z + side.z * half).color(r, g, bl, 0F).endVertex();
            buffer.pos(a.x, a.y, a.z).color(r, g, bl, alpha).endVertex();
            buffer.pos(b.x + side.x * half, b.y + side.y * half, b.z + side.z * half).color(r, g, bl, 0F).endVertex();
            buffer.pos(b.x, b.y, b.z).color(r, g, bl, alpha).endVertex();
            tessellator.draw();
        }
    }

    private static Vec3d[][] transform(Vec3d c, double size, float angle) {
        Vec3d[][] out = new Vec3d[STACKS + 1][SLICES + 1];
        for (int i = 0; i <= STACKS; i++) {
            for (int j = 0; j <= SLICES; j++) {
                out[i][j] = c.add(rotate(SURFACE[i][j], angle).scale(size));
            }
        }
        return out;
    }

    private static Vec3d rotate(Vec3d v, float angle) {
        return rotate(v, SPIN_AXIS, angle);
    }

    /** Rodrigues' rotation of v about a unit axis. */
    private static Vec3d rotate(Vec3d v, Vec3d axis, float angle) {
        double cos = MathHelper.cos(angle);
        double sin = MathHelper.sin(angle);
        Vec3d cross = axis.crossProduct(v);
        double dot = axis.dotProduct(v);
        return v.scale(cos).add(cross.scale(sin)).add(axis.scale(dot * (1D - cos)));
    }

    private static double surfaceRadius(Vec3d dir, Vec3d[] lumps, double[] heights, Vec3d[] craters, double[] widths) {
        double r = 1D;
        for (int k = 0; k < lumps.length; k++) {
            double d = Math.max(0D, dir.dotProduct(lumps[k]));
            r += heights[k] * d * d * d * d;
        }
        // Craters: a dished floor inside a slightly raised rim.
        for (int k = 0; k < craters.length; k++) {
            double angle = Math.acos(MathHelper.clamp(dir.dotProduct(craters[k]), -1D, 1D)) / widths[k];
            if (angle < 1D) {
                r -= 0.09D * (1D - angle * angle);
            } else if (angle < 1.4D) {
                r += 0.03D * Math.sin((angle - 1D) / 0.4D * Math.PI);
            }
        }
        r += 0.04D * Math.sin(dir.x * 13D) * Math.sin(dir.y * 11D) * Math.sin(dir.z * 17D);
        r += 0.02D * Math.sin(dir.x * 31D + dir.z * 7D) * Math.sin(dir.y * 29D);
        return MathHelper.clamp(r, 0.65D, 1.35D);
    }

    private static Vec3d tangent(Vec3d normal, Vec3d v) {
        Vec3d t = v.subtract(normal.scale(v.dotProduct(normal)));
        return t.lengthSquared() < 1.0E-8 ? normal.crossProduct(UP).normalize() : t.normalize();
    }

    private static Vec3d randomUnit(Random random) {
        Vec3d v;
        do {
            v = new Vec3d(random.nextDouble() * 2D - 1D, random.nextDouble() * 2D - 1D, random.nextDouble() * 2D - 1D);
        } while (v.lengthSquared() > 1D || v.lengthSquared() < 1.0E-4);
        return v.normalize();
    }
}
