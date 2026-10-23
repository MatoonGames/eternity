package com.matoon.eternity.client.render;

import com.matoon.eternity.world.Nexus;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * The living things of the Nexus of All Realities, drawn around whoever is in it:
 * <ul>
 * <li>eyeballs drifting in clusters, every pupil turning to follow you;</li>
 * <li>gigantic green clawed tentacles rising out of the void below, swaying;</li>
 * <li>framed windows onto other realities: a swamp with something watching from it,
 * fire, a lattice of molecules, open space, neon waves, a blinking eye;</li>
 * <li>the vortex over the hub, the way back out;</li>
 * <li>the grey seam standing at the junction where the In-Betweener is kept ({@link InBetweenerFx#seam}),
 * and fewer and fewer living things the nearer it is;</li>
 * <li>and the abyss under everything, full of hands reaching up ({@link NexusAbyss}).</li>
 * </ul>
 * Where each one is comes from a hash of its 96-block cell, so they are always in the
 * same places, for everyone.
 */
@SideOnly(Side.CLIENT)
public final class NexusRenderer {

    private static final int CELL = 96;
    private static final int RANGE = 2;
    private static final Vec3d UP = new Vec3d(0, 1, 0);
    private static final Vec3d LIGHT = new Vec3d(0.3D, 0.8D, 0.5D).normalize();
    private static final Map<Long, Cell> CELLS = new HashMap<>();
    /** Display lists for shapes drawn many times over: a unit eyeball looking down +Z, and a unit wart. */
    private static int eyeList = -1;
    private static int wartList = -1;

    private static final int SWAMP = 0, FIRE = 1, MOLECULES = 2, STARS = 3, WAVES = 4, EYE = 5;

    private NexusRenderer() {
    }

    private static final class Tentacle {
        Vec3d base;
        double height, phase;
    }

    private static final class Window {
        Vec3d centre;
        double w, h, yaw, spin, phase;
        int type;
    }

    private static final class Eyeball {
        Vec3d centre;
        double radius, phase;
    }

    private static final class Cell {
        final List<Tentacle> tentacles = new ArrayList<>();
        final List<Window> windows = new ArrayList<>();
        final List<Eyeball> eyes = new ArrayList<>();
    }

    private static Cell cell(int cx, int cz) {
        long key = ((long) cx << 32) ^ (cz & 0xFFFFFFFFL);
        Cell cell = CELLS.get(key);
        if (cell != null) {
            return cell;
        }
        if (CELLS.size() > 256) {
            CELLS.clear();
        }
        cell = new Cell();
        Random random = new Random(cx * 341873128712L + cz * 132897987541L + 99L);
        boolean hub = cx == 0 && cz == 0;
        if (!hub && random.nextFloat() < 0.4F) {
            Tentacle t = new Tentacle();
            t.base = new Vec3d(cx * CELL + 16 + random.nextInt(CELL - 32), -10D, cz * CELL + 16 + random.nextInt(CELL - 32));
            t.height = 100D + random.nextDouble() * 60D;
            t.phase = random.nextDouble() * 10D;
            cell.tentacles.add(t);
        }
        int windows = random.nextInt(3);
        for (int i = 0; i < windows; i++) {
            Window w = new Window();
            w.centre = new Vec3d(cx * CELL + random.nextInt(CELL), 30 + random.nextInt(190), cz * CELL + random.nextInt(CELL));
            w.w = 9D + random.nextDouble() * 7D;
            w.h = w.w * (1.2D + random.nextDouble() * 0.4D);
            w.yaw = random.nextDouble() * Math.PI * 2D;
            w.spin = (random.nextDouble() - 0.5D) * 0.006D;
            w.phase = random.nextDouble() * 100D;
            w.type = random.nextInt(6);
            cell.windows.add(w);
        }
        if (random.nextFloat() < 0.55F) {
            Vec3d centre = new Vec3d(cx * CELL + random.nextInt(CELL), 30 + random.nextInt(190), cz * CELL + random.nextInt(CELL));
            int count = 3 + random.nextInt(4);
            for (int i = 0; i < count; i++) {
                Eyeball e = new Eyeball();
                e.radius = 1.5D + random.nextDouble() * 2.5D;
                e.centre = centre.add((random.nextDouble() - 0.5D) * 16D, (random.nextDouble() - 0.5D) * 12D, (random.nextDouble() - 0.5D) * 16D);
                e.phase = random.nextDouble() * 10D;
                cell.eyes.add(e);
            }
        }
        CELLS.put(key, cell);
        return cell;
    }

    // ------------------------------------------------------------------ the world

    public static void renderWorld(Vec3d camera, float time) {
        GlStateManager.pushMatrix();
        GlStateManager.disableFog();
        GlStateManager.disableLighting();
        GlStateManager.disableTexture2D();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.shadeModel(GL11.GL_FLAT);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
        NexusAbyss.render(camera, time);

        int ccx = Math.floorDiv((int) Math.floor(camera.x), CELL);
        int ccz = Math.floorDiv((int) Math.floor(camera.z), CELL);
        for (int cx = ccx - RANGE; cx <= ccx + RANGE; cx++) {
            for (int cz = ccz - RANGE; cz <= ccz + RANGE; cz++) {
                Cell cell = cell(cx, cz);
                for (Tentacle t : cell.tentacles) {
                    if (lively(t.base)) {
                        tentacle(t, camera, time);
                    }
                }
                for (Eyeball e : cell.eyes) {
                    Vec3d at = e.centre.add(0D, Math.sin(time * 0.02D + e.phase) * 1.5D, 0D);
                    Vec3d look = camera.subtract(at);
                    if (look.lengthSquared() < 150D * 150D && lively(e.centre)) {
                        eye(at.subtract(camera), e.radius, look.lengthSquared() < 1.0E-4 ? UP : look.normalize());
                    }
                }
                for (Window w : cell.windows) {
                    if (lively(w.centre)) {
                        window(w, camera, time);
                    }
                }
            }
        }
        InBetweenerFx.seam(camera, time);
        InBetweenerFx.convergence(camera, time);
        Vec3d vortex = new Vec3d(Nexus.VORTEX_X, Nexus.VORTEX_Y, Nexus.VORTEX_Z);
        if (vortex.distanceTo(camera) < 400D) {
            vortex(vortex.subtract(camera), time);
        }

        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.depthMask(true);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.enableCull();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }

    /** Nothing lives near the junction; the closer, the fewer things that move. */
    private static boolean lively(Vec3d at) {
        float neutral = Nexus.neutrality(at.x, at.z);
        return neutral < 0.15F || neutral < 0.6F && ((int) (at.x * 7D + at.z * 13D) & 3) == 0;
    }

    // ------------------------------------------------------------------ eyes

    /** An eyeball: white, veined pink toward the back, a blue iris and black pupil turned along {@code look}. */
    public static void eye(Vec3d c, double r, Vec3d look) {
        if (eyeList < 0) {
            eyeList = compile(() -> eyeGeometry(new Vec3d(0, 0, 1)));
        }
        GlStateManager.pushMatrix();
        GlStateManager.translate(c.x, c.y, c.z);
        GlStateManager.rotate((float) Math.toDegrees(Math.atan2(look.x, look.z)), 0F, 1F, 0F);
        GlStateManager.rotate((float) -Math.toDegrees(Math.asin(MathHelper.clamp(look.y, -1D, 1D))), 1F, 0F, 0F);
        GlStateManager.scale(r, r, r);
        outsideOnly(() -> GlStateManager.callList(eyeList));
        GlStateManager.popMatrix();
    }

    private static void eyeGeometry(Vec3d look) {
        Vec3d glint = look.add(UP.scale(0.22D)).add(new Vec3d(0.2D, 0D, 0D)).normalize();
        sphereGeometry(Vec3d.ZERO, 1D, 18, 14, n -> {
            double facing = n.dotProduct(look);
            if (n.dotProduct(glint) > 0.993D) {
                return new float[]{1F, 1F, 1F};
            }
            if (facing > 0.955D) {
                return new float[]{0.04F, 0.03F, 0.1F};
            }
            if (facing > 0.86D) {
                return new float[]{0.3F, 0.55F, 1F};
            }
            if (facing > 0.82D) {
                return new float[]{0.12F, 0.25F, 0.7F};
            }
            if (facing < -0.35D) {
                return new float[]{0.96F, 0.78F, 0.86F};
            }
            return new float[]{0.97F, 0.95F, 1F};
        });
    }

    // ------------------------------------------------------------------ tentacles

    /** A clawed limb of swamp-green flesh, rising out of the void and swaying as if underwater. */
    private static void tentacle(Tentacle t, Vec3d camera, float time) {
        if (t.base.add(0D, t.height * 0.5D, 0D).distanceTo(camera) > 240D) {
            return;
        }
        int n = 20;
        Vec3d[] spine = new Vec3d[n + 1];
        for (int i = 0; i <= n; i++) {
            double s = i / (double) n;
            double sway = time * 0.016D + t.phase;
            double x = Math.sin(s * 2.6D + sway) * s * 18D;
            double z = Math.cos(s * 2.1D + sway * 1.3D) * s * 14D;
            // The last stretch hooks over, like a hand reaching.
            double hook = Math.max(0D, s - 0.75D) * 4D;
            x += hook * hook * 16D * Math.cos(t.phase);
            z += hook * hook * 16D * Math.sin(t.phase);
            spine[i] = t.base.add(x, s * t.height - hook * hook * 10D, z).subtract(camera);
        }
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        int sides = 8;
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        Vec3d[] previous = null;
        for (int i = 0; i <= n; i++) {
            double s = i / (double) n;
            double radius = 5.4D * Math.pow(1D - s, 0.85D) + 0.7D;
            Vec3d tangent = i < n ? spine[i + 1].subtract(spine[i]) : spine[i].subtract(spine[i - 1]);
            tangent = tangent.normalize();
            Vec3d u = tangent.crossProduct(Math.abs(tangent.y) > 0.9D ? new Vec3d(1, 0, 0) : UP).normalize();
            Vec3d v = tangent.crossProduct(u).normalize();
            Vec3d[] ring = new Vec3d[sides + 1];
            for (int j = 0; j <= sides; j++) {
                double a = Math.PI * 2D * j / sides;
                ring[j] = spine[i].add(u.scale(Math.cos(a) * radius)).add(v.scale(Math.sin(a) * radius));
            }
            if (previous != null) {
                for (int j = 0; j < sides; j++) {
                    Vec3d normal = previous[j].add(ring[j + 1]).scale(0.5D).subtract(spine[i]).normalize();
                    float shade = shade(normal);
                    boolean mottle = ((i * 7 + j * 3) % 5) == 0;
                    float r = mottle ? 0.42F : 0.6F, g = mottle ? 0.52F : 0.72F, b = mottle ? 0.1F : 0.16F;
                    if (normal.dotProduct(LIGHT) > 0.75D) {
                        r = 0.82F;
                        g = 0.88F;
                        b = 0.32F;
                    }
                    quad(buffer, previous[j], previous[j + 1], ring[j + 1], ring[j], r * shade, g * shade, b * shade, 1F);
                }
            }
            previous = ring;
        }
        tessellator.draw();

        // Warts along its length.
        if (wartList < 0) {
            wartList = compile(() -> sphereGeometry(Vec3d.ZERO, 1D, 8, 6, normal -> new float[]{0.7F, 0.75F, 0.25F}));
        }
        for (int i = 2; i < n - 2; i += 2) {
            double s = i / (double) n;
            double radius = 5.4D * Math.pow(1D - s, 0.85D) + 0.7D;
            Vec3d tangent = spine[i + 1].subtract(spine[i]).normalize();
            Vec3d u = tangent.crossProduct(Math.abs(tangent.y) > 0.9D ? new Vec3d(1, 0, 0) : UP).normalize();
            Vec3d v = tangent.crossProduct(u).normalize();
            double a = i * 1.7D;
            Vec3d at = spine[i].add(u.scale(Math.cos(a) * radius)).add(v.scale(Math.sin(a) * radius));
            GlStateManager.pushMatrix();
            GlStateManager.translate(at.x, at.y, at.z);
            GlStateManager.scale(radius * 0.28D, radius * 0.28D, radius * 0.28D);
            outsideOnly(() -> GlStateManager.callList(wartList));
            GlStateManager.popMatrix();
        }

        // Long ivory claws splayed at its tip.
        Vec3d tip = spine[n];
        Vec3d dir = spine[n].subtract(spine[n - 1]).normalize();
        Vec3d u = dir.crossProduct(Math.abs(dir.y) > 0.9D ? new Vec3d(1, 0, 0) : UP).normalize();
        Vec3d v = dir.crossProduct(u).normalize();
        double flex = 0.35D + 0.15D * Math.sin(time * 0.05D + t.phase);
        for (int k = 0; k < 4; k++) {
            double a = Math.PI * 2D * k / 4D + 0.4D;
            Vec3d out = u.scale(Math.cos(a)).add(v.scale(Math.sin(a)));
            Vec3d clawDir = dir.add(out.scale(flex)).normalize();
            cone(tip.add(out.scale(0.6D)), clawDir, 8D, 0.55D, new float[]{0.95F, 0.9F, 0.74F});
        }
    }

    // ------------------------------------------------------------------ windows onto other realities

    /** A comic panel hanging in the air, framed in cream and inked black, showing somewhere else entirely. */
    private static void window(Window wdw, Vec3d camera, float time) {
        Vec3d at = wdw.centre.add(0D, Math.sin(time * 0.02D + wdw.phase) * 2D, 0D).subtract(camera);
        if (at.length() > 170D) {
            return;
        }
        double yaw = wdw.yaw + time * wdw.spin;
        Vec3d right = new Vec3d(Math.cos(yaw), 0D, Math.sin(yaw));
        Vec3d up = new Vec3d(0D, 1D, 0D).add(right.scale(0.15D * Math.sin(wdw.phase))).normalize();
        Vec3d normal = right.crossProduct(up).normalize();
        Panel p = new Panel(at, right, up, normal);
        double hw = wdw.w / 2D, hh = wdw.h / 2D;
        p.rect(-hw - 0.6D, -hh - 0.6D, hw + 0.6D, hh + 0.6D, -0.03D, 0.02F, 0.01F, 0.04F);
        p.rect(-hw - 0.4D, -hh - 0.4D, hw + 0.4D, hh + 0.4D, 0D, 0.98F, 0.94F, 0.84F);
        float t = time + (float) wdw.phase * 20F;
        switch (wdw.type) {
            case SWAMP:
                swamp(p, hw, hh, t);
                break;
            case FIRE:
                fire(p, hw, hh, t);
                break;
            case MOLECULES:
                molecules(p, hw, hh, t);
                break;
            case STARS:
                Cosmic.draw(() -> p.plain(-hw, -hh, hw, hh, 0.03D), 6, 1F, 1.6F, 1F, 0.7F, 1F, true);
                GlStateManager.disableTexture2D();
                GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                break;
            case WAVES:
                waves(p, hw, hh, t);
                break;
            default:
                bigEye(p, hw, hh, t);
                break;
        }
    }

    /** A drowned green world, and something tall and shaggy watching from it with red eyes. */
    private static void swamp(Panel p, double hw, double hh, float t) {
        p.gradient(-hw, -hh, hw, hh, 0.03D, new float[]{0.12F, 0.3F, 0.05F}, new float[]{0.78F, 0.92F, 0.22F});
        for (int i = 0; i < 7; i++) {
            double u = Math.sin(t * 0.01D + i * 2.1D) * hw * 0.6D;
            double v = Math.cos(t * 0.013D + i * 1.7D) * hh * 0.6D;
            p.circle(u, v, hw * 0.16D, 0.04D, new float[]{0.3F, 0.55F, 0.1F}, new float[]{0.45F, 0.7F, 0.15F});
        }
        double sx = -hw * 0.1D + Math.sin(t * 0.004D) * hw * 0.2D;
        p.rect(sx - hw * 0.18D, -hh, sx + hw * 0.18D, hh * 0.15D, 0.05D, 0.16F, 0.2F, 0.06F);
        p.circle(sx, hh * 0.25D, hw * 0.24D, 0.05D, new float[]{0.2F, 0.24F, 0.08F}, new float[]{0.16F, 0.2F, 0.06F});
        p.circle(sx - hw * 0.08D, hh * 0.27D, hw * 0.04D, 0.06D, new float[]{1F, 0.2F, 0.15F}, new float[]{0.8F, 0.05F, 0.05F});
        p.circle(sx + hw * 0.08D, hh * 0.27D, hw * 0.04D, 0.06D, new float[]{1F, 0.2F, 0.15F}, new float[]{0.8F, 0.05F, 0.05F});
    }

    /** A world on fire: tongues of flame licking up from below. */
    private static void fire(Panel p, double hw, double hh, float t) {
        p.gradient(-hw, -hh, hw, hh, 0.03D, new float[]{0.95F, 0.3F, 0.05F}, new float[]{0.3F, 0.02F, 0.05F});
        for (int i = 0; i < 9; i++) {
            double u = -hw + (i + 0.5D) * hw * 2D / 9D;
            // Slow, rolling flames: nothing here flickers.
            double height = hh * (0.9D + 0.4D * Math.sin(t * 0.025D + i * 1.3D));
            p.triangle(u - hw * 0.14D, -hh, u + hw * 0.14D, -hh, u + Math.sin(t * 0.02D + i) * hw * 0.05D, -hh + height, 0.04D, 1F, 0.55F, 0.05F);
            p.triangle(u - hw * 0.07D, -hh, u + hw * 0.07D, -hh, u, -hh + height * 0.6D, 0.05D, 1F, 0.92F, 0.3F);
        }
    }

    /** A lattice of glowing atoms bound together, slowly turning. */
    private static void molecules(Panel p, double hw, double hh, float t) {
        p.rect(-hw, -hh, hw, hh, 0.03D, 0.05F, 0.08F, 0.3F);
        int n = 9;
        double[][] nodes = new double[n][];
        for (int i = 0; i < n; i++) {
            nodes[i] = new double[]{Math.sin(t * 0.008D + i * 2.4D) * hw * 0.75D, Math.cos(t * 0.006D + i * 1.9D) * hh * 0.75D};
        }
        for (int i = 0; i < n; i++) {
            p.line(nodes[i][0], nodes[i][1], nodes[(i + 1) % n][0], nodes[(i + 1) % n][1], 0.18D, 0.04D, 0.4F, 0.75F, 1F);
            p.line(nodes[i][0], nodes[i][1], nodes[(i + 3) % n][0], nodes[(i + 3) % n][1], 0.12D, 0.04D, 0.3F, 0.5F, 0.9F);
        }
        for (double[] node : nodes) {
            p.circle(node[0], node[1], hw * 0.09D, 0.05D, new float[]{0.85F, 0.97F, 1F}, new float[]{0.3F, 0.65F, 1F});
        }
    }

    /** Neon bands rippling across a black field. */
    private static void waves(Panel p, double hw, double hh, float t) {
        p.rect(-hw, -hh, hw, hh, 0.03D, 0.03F, 0.01F, 0.06F);
        float[][] colours = {{1F, 0.2F, 0.75F}, {0.2F, 0.9F, 1F}, {1F, 0.9F, 0.2F}, {1F, 0.5F, 0.1F}, {0.5F, 1F, 0.3F}, {0.7F, 0.3F, 1F}};
        double band = hh * 2D / 9D;
        for (int k = 0; k < colours.length; k++) {
            double base = -hh + band * (1.5D + k * 1.1D);
            p.wave(-hw, hw, base, band * 0.55D, hh * 0.08D, t * 0.012D + k, 0.04D, colours[k]);
        }
    }

    /** One great eye on a pink field, staring out, blinking now and then. */
    private static void bigEye(Panel p, double hw, double hh, float t) {
        p.rect(-hw, -hh, hw, hh, 0.03D, 0.95F, 0.5F, 0.75F);
        double cycle = (t * 0.02D) % 5D;
        double open = cycle > 4.8D ? Math.abs(cycle - 4.9D) * 10D : 1D;
        double rx = hw * 0.8D, ry = hh * 0.45D * open;
        p.ellipse(0, 0, rx, ry, 0.04D, new float[]{1F, 1F, 1F}, new float[]{0.92F, 0.88F, 0.95F});
        if (open > 0.3D) {
            double look = Math.sin(t * 0.01D) * hw * 0.25D;
            p.ellipse(look, 0, hw * 0.3D, Math.min(hw * 0.3D, ry * 0.9D), 0.05D, new float[]{0.35F, 0.6F, 1F}, new float[]{0.15F, 0.3F, 0.8F});
            p.ellipse(look, 0, hw * 0.14D, Math.min(hw * 0.14D, ry * 0.5D), 0.06D, new float[]{0.03F, 0.02F, 0.08F}, new float[]{0.03F, 0.02F, 0.08F});
            p.circle(look + hw * 0.08D, ry * 0.25D, hw * 0.05D, 0.07D, new float[]{1F, 1F, 1F}, new float[]{1F, 1F, 1F});
        }
    }

    // ------------------------------------------------------------------ the vortex

    /** The way out: a whirlpool of every colour at once, standing upright over the hub. */
    private static void vortex(Vec3d c, float time) {
        double r = Nexus.VORTEX_RADIUS;
        Cosmic.draw(() -> {
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION);
            buffer.pos(c.x, c.y, c.z).endVertex();
            for (int i = 0; i <= 48; i++) {
                double a = Math.PI * 2D * i / 48D;
                buffer.pos(c.x + Math.cos(a) * r, c.y + Math.sin(a) * r, c.z).endVertex();
            }
            tessellator.draw();
        }, 8, 1F, 2F, 1F, 0.6F, 1F, true);
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        GlStateManager.depthMask(false);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        for (int arm = 0; arm < 5; arm++) {
            float[] colour = hue(arm / 5F + time * 0.0005F, 0.8F, 1F);
            buffer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
            // An arm spiralling in toward the centre, widening and brightening as it goes out.
            for (int i = 0; i <= 40; i++) {
                double s = i / 40D;
                double a = arm * Math.PI * 2D / 5D + s * 5D - time * 0.015D;
                double width = 0.3D + s * 0.9D;
                double inner = r * s - width;
                double outer = r * s + width;
                buffer.pos(c.x + Math.cos(a) * inner, c.y + Math.sin(a) * inner, c.z + 0.08D)
                        .color(colour[0], colour[1], colour[2], 0.9F * (float) s).endVertex();
                buffer.pos(c.x + Math.cos(a) * outer, c.y + Math.sin(a) * outer, c.z + 0.08D)
                        .color(colour[0], colour[1], colour[2], 0F).endVertex();
            }
            tessellator.draw();
        }
        buffer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i <= 64; i++) {
            double a = Math.PI * 2D * i / 64D;
            float[] colour = hue((float) (a / (Math.PI * 2D)) + time * 0.001F, 0.9F, 1F);
            buffer.pos(c.x + Math.cos(a) * r, c.y + Math.sin(a) * r, c.z).color(colour[0], colour[1], colour[2], 0.9F).endVertex();
            buffer.pos(c.x + Math.cos(a) * (r + 3D), c.y + Math.sin(a) * (r + 3D), c.z).color(colour[0], colour[1], colour[2], 0F).endVertex();
        }
        tessellator.draw();
        GlStateManager.depthMask(true);
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
    }

    // ------------------------------------------------------------------ geometry

    /** Colour for a point on a sphere, by its outward normal. */
    public interface Shader {
        float[] at(Vec3d normal);
    }

    /** A flat-shaded sphere, each facet coloured by {@code shader} and lit from above; only its near side is drawn. */
    public static void sphere(Vec3d c, double r, int slices, int stacks, Shader shader) {
        outsideOnly(() -> sphereGeometry(c, r, slices, stacks, shader));
    }

    /**
     * The sphere's facets wind clockwise seen from outside, so culling front faces leaves
     * exactly its near side. That draws correctly even where nothing writes depth, as in
     * the sky, without sorting anything.
     */
    public static void outsideOnly(Runnable draw) {
        GlStateManager.enableCull();
        GlStateManager.cullFace(GlStateManager.CullFace.FRONT);
        draw.run();
        GlStateManager.cullFace(GlStateManager.CullFace.BACK);
        GlStateManager.disableCull();
    }

    /** Records some geometry once into a display list, to be drawn again cheaply. */
    public static int compile(Runnable draw) {
        int list = GLAllocation.generateDisplayLists(1);
        GlStateManager.glNewList(list, GL11.GL_COMPILE);
        draw.run();
        GlStateManager.glEndList();
        return list;
    }

    /** The facets of a sphere, without touching any state. */
    public static void sphereGeometry(Vec3d c, double r, int slices, int stacks, Shader shader) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < stacks; i++) {
            double t0 = -Math.PI / 2D + Math.PI * i / stacks;
            double t1 = -Math.PI / 2D + Math.PI * (i + 1) / stacks;
            for (int j = 0; j < slices; j++) {
                double p0 = Math.PI * 2D * j / slices;
                double p1 = Math.PI * 2D * (j + 1) / slices;
                Vec3d n = onSphere((t0 + t1) / 2D, (p0 + p1) / 2D);
                float[] colour = shader.at(n);
                float s = shade(n);
                quad(buffer, c.add(onSphere(t0, p0).scale(r)), c.add(onSphere(t0, p1).scale(r)), c.add(onSphere(t1, p1).scale(r)),
                        c.add(onSphere(t1, p0).scale(r)), colour[0] * s, colour[1] * s, colour[2] * s, 1F);
            }
        }
        tessellator.draw();
    }

    private static Vec3d onSphere(double theta, double phi) {
        double cos = Math.cos(theta);
        return new Vec3d(cos * Math.cos(phi), Math.sin(theta), cos * Math.sin(phi));
    }

    /** A pointed cone, for claws. */
    private static void cone(Vec3d base, Vec3d dir, double length, double radius, float[] colour) {
        Vec3d u = dir.crossProduct(Math.abs(dir.y) > 0.9D ? new Vec3d(1, 0, 0) : UP).normalize();
        Vec3d v = dir.crossProduct(u).normalize();
        Vec3d tip = base.add(dir.scale(length));
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
        for (int j = 0; j < 6; j++) {
            double a0 = Math.PI * 2D * j / 6D, a1 = Math.PI * 2D * (j + 1) / 6D;
            Vec3d p0 = base.add(u.scale(Math.cos(a0) * radius)).add(v.scale(Math.sin(a0) * radius));
            Vec3d p1 = base.add(u.scale(Math.cos(a1) * radius)).add(v.scale(Math.sin(a1) * radius));
            float s = shade(p0.add(p1).scale(0.5D).subtract(base).normalize());
            buffer.pos(p0.x, p0.y, p0.z).color(colour[0] * s, colour[1] * s, colour[2] * s, 1F).endVertex();
            buffer.pos(p1.x, p1.y, p1.z).color(colour[0] * s, colour[1] * s, colour[2] * s, 1F).endVertex();
            buffer.pos(tip.x, tip.y, tip.z).color(1F, 1F, 0.92F, 1F).endVertex();
        }
        tessellator.draw();
    }

    private static float shade(Vec3d normal) {
        return 0.55F + 0.45F * (float) Math.max(0D, normal.dotProduct(LIGHT));
    }

    private static void quad(BufferBuilder buffer, Vec3d a, Vec3d b, Vec3d c, Vec3d d, float r, float g, float bl, float alpha) {
        buffer.pos(a.x, a.y, a.z).color(r, g, bl, alpha).endVertex();
        buffer.pos(b.x, b.y, b.z).color(r, g, bl, alpha).endVertex();
        buffer.pos(c.x, c.y, c.z).color(r, g, bl, alpha).endVertex();
        buffer.pos(d.x, d.y, d.z).color(r, g, bl, alpha).endVertex();
    }

    /** A colour from hue, saturation and brightness, each 0 to 1. */
    public static float[] hue(float h, float s, float b) {
        int rgb = Color.HSBtoRGB(h - (float) Math.floor(h), s, b);
        return new float[]{((rgb >> 16) & 0xFF) / 255F, ((rgb >> 8) & 0xFF) / 255F, (rgb & 0xFF) / 255F};
    }

    /** A flat surface in the air, drawn on in its own two dimensions: u to the right, v up, d out of it. */
    private static final class Panel {
        final Vec3d origin, right, up, normal;

        Panel(Vec3d origin, Vec3d right, Vec3d up, Vec3d normal) {
            this.origin = origin;
            this.right = right;
            this.up = up;
            this.normal = normal;
        }

        Vec3d at(double u, double v, double d) {
            return origin.add(right.scale(u)).add(up.scale(v)).add(normal.scale(d));
        }

        void put(BufferBuilder buffer, double u, double v, double d, float r, float g, float b, float a) {
            Vec3d p = at(u, v, d);
            buffer.pos(p.x, p.y, p.z).color(r, g, b, a).endVertex();
        }

        void rect(double u0, double v0, double u1, double v1, double d, float r, float g, float b) {
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            put(buffer, u0, v0, d, r, g, b, 1F);
            put(buffer, u1, v0, d, r, g, b, 1F);
            put(buffer, u1, v1, d, r, g, b, 1F);
            put(buffer, u0, v1, d, r, g, b, 1F);
            tessellator.draw();
        }

        /** Positions only, for Cosmic to texture. */
        void plain(double u0, double v0, double u1, double v1, double d) {
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
            Vec3d a = at(u0, v0, d), b = at(u1, v0, d), c = at(u1, v1, d), e = at(u0, v1, d);
            buffer.pos(a.x, a.y, a.z).endVertex();
            buffer.pos(b.x, b.y, b.z).endVertex();
            buffer.pos(c.x, c.y, c.z).endVertex();
            buffer.pos(e.x, e.y, e.z).endVertex();
            tessellator.draw();
        }

        void gradient(double u0, double v0, double u1, double v1, double d, float[] bottom, float[] top) {
            GlStateManager.shadeModel(GL11.GL_SMOOTH);
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            put(buffer, u0, v0, d, bottom[0], bottom[1], bottom[2], 1F);
            put(buffer, u1, v0, d, bottom[0], bottom[1], bottom[2], 1F);
            put(buffer, u1, v1, d, top[0], top[1], top[2], 1F);
            put(buffer, u0, v1, d, top[0], top[1], top[2], 1F);
            tessellator.draw();
            GlStateManager.shadeModel(GL11.GL_FLAT);
        }

        void circle(double u, double v, double radius, double d, float[] centre, float[] edge) {
            ellipse(u, v, radius, radius, d, centre, edge);
        }

        void ellipse(double u, double v, double rx, double ry, double d, float[] centre, float[] edge) {
            GlStateManager.shadeModel(GL11.GL_SMOOTH);
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
            put(buffer, u, v, d, centre[0], centre[1], centre[2], 1F);
            for (int i = 0; i <= 24; i++) {
                double a = Math.PI * 2D * i / 24D;
                put(buffer, u + Math.cos(a) * rx, v + Math.sin(a) * ry, d, edge[0], edge[1], edge[2], 1F);
            }
            tessellator.draw();
            GlStateManager.shadeModel(GL11.GL_FLAT);
        }

        void triangle(double u0, double v0, double u1, double v1, double u2, double v2, double d, float r, float g, float b) {
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
            put(buffer, u0, v0, d, r, g, b, 1F);
            put(buffer, u1, v1, d, r, g, b, 1F);
            put(buffer, u2, v2, d, r, g, b, 1F);
            tessellator.draw();
        }

        void line(double u0, double v0, double u1, double v1, double width, double d, float r, float g, float b) {
            double du = u1 - u0, dv = v1 - v0;
            double len = Math.sqrt(du * du + dv * dv);
            if (len < 1.0E-4) {
                return;
            }
            double nu = -dv / len * width, nv = du / len * width;
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            put(buffer, u0 - nu, v0 - nv, d, r, g, b, 1F);
            put(buffer, u1 - nu, v1 - nv, d, r, g, b, 1F);
            put(buffer, u1 + nu, v1 + nv, d, r, g, b, 1F);
            put(buffer, u0 + nu, v0 + nv, d, r, g, b, 1F);
            tessellator.draw();
        }

        void wave(double u0, double u1, double base, double thickness, double amplitude, double phase, double d, float[] colour) {
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(GL11.GL_QUAD_STRIP, DefaultVertexFormats.POSITION_COLOR);
            for (int i = 0; i <= 32; i++) {
                double u = u0 + (u1 - u0) * i / 32D;
                double v = base + Math.sin(u * 0.6D + phase) * amplitude;
                put(buffer, u, v, d, colour[0], colour[1], colour[2], 1F);
                put(buffer, u, v + thickness, d, colour[0], colour[1], colour[2], 1F);
            }
            tessellator.draw();
        }
    }
}
