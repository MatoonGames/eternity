package com.matoon.eternity.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * What the story leaves in the world that is not a block or a body: the cube under the mound,
 * hanging over its altar in a ring of ward-light that keeps failing; the Aether in the vault, a
 * sealed column of dark glass with something red moving slowly inside it; and the moments that
 * pass: the ward going out, the column breaking and the Aether pouring into someone, the Flame
 * drawing it out of them and going dark, a rune of the oath door taking light.
 * <p>
 * Everything is drawn camera-relative, additively unless said otherwise, and puts the GL state
 * back as it found it.
 */
@SideOnly(Side.CLIENT)
public final class StoryFx {

    static final float[] WARD = {0.55F, 0.75F, 1F};
    static final float[] AETHER = {0.85F, 0.06F, 0.08F};
    static final float[] AETHER_DEEP = {0.35F, 0.0F, 0.04F};
    static final float[] WHITE = {1F, 0.97F, 0.92F};
    static final float[] RUNE = {0.6F, 0.85F, 1F};

    private static final TesseractRenderer CUBE = new TesseractRenderer();

    /** A passing effect: what kind, where, toward what, and how far through it is. */
    private static final class Burst {
        final int kind;
        final Vec3d a;
        final int entity;
        final int life;
        int age;

        Burst(int kind, Vec3d a, int entity, int life) {
            this.kind = kind;
            this.a = a;
            this.entity = entity;
            this.life = life;
        }
    }

    public static final int WARD_OUT = 0, AETHER_TAKEN = 1, FLAME_SET = 2, RUNE_LIT = 3, RUNE_FAIL = 4;

    private static final List<Burst> BURSTS = new ArrayList<>();

    private StoryFx() {
    }

    public static void burst(int kind, Vec3d at, int entity) {
        int life = kind == WARD_OUT ? 50 : kind == AETHER_TAKEN ? 70 : kind == FLAME_SET ? 90 : 30;
        BURSTS.add(new Burst(kind, at, entity, life));
    }

    public static void tick() {
        Iterator<Burst> it = BURSTS.iterator();
        while (it.hasNext()) {
            Burst b = it.next();
            if (++b.age > b.life) {
                it.remove();
            }
        }
    }

    public static void clear() {
        BURSTS.clear();
    }

    private static void begin() {
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.depthMask(false);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
    }

    private static void end() {
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.depthMask(true);
        GlStateManager.enableAlpha();
        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    // ------------------------------------------------------------------ the cube in its ward

    /**
     * The cube, turning slowly over the altar, and the ward round it: eight posts of cold light
     * and a ring on the floor between them, all of it dropping out and coming back, never
     * together, as if something were pulling it apart.
     */
    public static void ward(Vec3d cube, Vec3d floor, double radius, float age) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(cube.x, cube.y + 0.06D * MathHelper.sin(age * 0.05F), cube.z);
        GlStateManager.rotate(age * 0.6F, 0F, 1F, 0F);
        GlStateManager.rotate(14F, 1F, 0F, 1F);
        GlStateManager.scale(0.55F, 0.55F, 0.55F);
        CUBE.draw(age);
        GlStateManager.popMatrix();

        begin();
        Random rand = new Random((long) (age / 3F));
        Tessellator t = Tessellator.getInstance();
        BufferBuilder b = t.getBuffer();
        // The posts: shafts of light standing up out of them, each guttering on its own.
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4D + Math.PI / 8D;
            Vec3d base = floor.add(Math.cos(a) * radius, 0.9D, Math.sin(a) * radius);
            float gutter = flicker(age, i);
            if (gutter <= 0.02F) {
                continue;
            }
            float h = 2.4F * (0.7F + 0.3F * gutter);
            shaft(b, t, base, 0.14D, h, WARD, 0.32F * gutter);
            WarlockFx.glow(base, 0.5D, WARD, 0.22F * gutter);
        }
        // The ring on the floor: segments of it, whichever are holding.
        b.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        int n = 48;
        for (int i = 0; i < n; i++) {
            float hold = flicker(age * 0.8F, i * 7 + 3);
            if (hold < 0.15F) {
                continue;
            }
            double a0 = i * Math.PI * 2D / n, a1 = (i + 1) * Math.PI * 2D / n;
            double r0 = radius - 0.12D, r1 = radius + 0.12D;
            float al = 0.5F * hold;
            b.pos(floor.x + Math.cos(a0) * r0, floor.y + 0.03D, floor.z + Math.sin(a0) * r0).color(WARD[0], WARD[1], WARD[2], al).endVertex();
            b.pos(floor.x + Math.cos(a0) * r1, floor.y + 0.03D, floor.z + Math.sin(a0) * r1).color(WARD[0], WARD[1], WARD[2], 0F).endVertex();
            b.pos(floor.x + Math.cos(a1) * r1, floor.y + 0.03D, floor.z + Math.sin(a1) * r1).color(WARD[0], WARD[1], WARD[2], 0F).endVertex();
            b.pos(floor.x + Math.cos(a1) * r0, floor.y + 0.03D, floor.z + Math.sin(a1) * r0).color(WARD[0], WARD[1], WARD[2], al).endVertex();
        }
        t.draw();
        // Threads of it reaching in to the cube from the posts that are holding, and the strain on it.
        for (int i = 0; i < 8; i++) {
            float hold = flicker(age, i);
            if (hold < 0.4F) {
                continue;
            }
            double a = i * Math.PI / 4D + Math.PI / 8D;
            Vec3d from = floor.add(Math.cos(a) * radius, 2.2D, Math.sin(a) * radius);
            beam(b, t, from, cube, 0.03D, WARD, 0.18F * hold);
        }
        WarlockFx.glow(cube, 1.6D, WARD, 0.12F + 0.06F * MathHelper.sin(age * 0.11F));
        if (rand.nextInt(6) == 0) {
            // Now and then the whole of it shudders.
            WarlockFx.glow(cube, 2.6D, WHITE, 0.1F);
        }
        end();
    }

    /** How much of the ward a post or segment is holding this frame: mostly some, sometimes none. */
    private static float flicker(float age, int i) {
        float slow = 0.5F + 0.5F * MathHelper.sin(age * 0.045F + i * 2.3F);
        float fast = MathHelper.sin(age * 0.9F + i * 5.1F) * MathHelper.sin(age * 0.37F + i);
        float v = slow * 0.8F + fast * 0.35F;
        // Dropouts: whole stretches where it is simply gone.
        if (MathHelper.sin(age * 0.07F + i * 1.7F) > 0.72F) {
            v *= 0.05F;
        }
        return MathHelper.clamp(v, 0F, 1F);
    }

    // ------------------------------------------------------------------ the Aether

    /**
     * The Aether, sealed: a column of dark glass bound top, middle and foot in iron, and inside it
     * something red that moves too slowly to be a liquid and too much to be anything else.
     */
    public static void aether(Vec3d foot, float age) {
        begin();
        Tessellator t = Tessellator.getInstance();
        BufferBuilder b = t.getBuffer();
        double r = 0.24D, h = 1.15D;
        // The red inside, first: slow bands of it drifting up and folding back.
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        int sides = 12, rows = 14;
        b.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        for (int j = 0; j < rows; j++) {
            double y0 = foot.y + 0.08D + h * j / rows, y1 = foot.y + 0.08D + h * (j + 1) / rows;
            for (int i = 0; i < sides; i++) {
                double a0 = i * Math.PI * 2D / sides, a1 = (i + 1) * Math.PI * 2D / sides;
                float swirl0 = 0.5F + 0.5F * MathHelper.sin((float) (a0 * 2D) + age * 0.03F - j * 0.6F + MathHelper.sin(age * 0.011F + j) * 2F);
                float swirl1 = 0.5F + 0.5F * MathHelper.sin((float) (a1 * 2D) + age * 0.03F - j * 0.6F + MathHelper.sin(age * 0.011F + j) * 2F);
                double ri = r * 0.82D;
                color(b, foot.x + Math.cos(a0) * ri, y0, foot.z + Math.sin(a0) * ri, swirl0);
                color(b, foot.x + Math.cos(a1) * ri, y0, foot.z + Math.sin(a1) * ri, swirl1);
                color(b, foot.x + Math.cos(a1) * ri, y1, foot.z + Math.sin(a1) * ri, swirl1);
                color(b, foot.x + Math.cos(a0) * ri, y1, foot.z + Math.sin(a0) * ri, swirl0);
            }
        }
        t.draw();
        // The glass round it: dark, with the light catching its edges.
        b.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < sides; i++) {
            double a0 = i * Math.PI * 2D / sides, a1 = (i + 1) * Math.PI * 2D / sides;
            float edge = 0.18F + 0.1F * (float) Math.max(0D, Math.cos(a0 - 0.8D));
            b.pos(foot.x + Math.cos(a0) * r, foot.y, foot.z + Math.sin(a0) * r).color(0.05F, 0.03F, 0.04F, 0.55F).endVertex();
            b.pos(foot.x + Math.cos(a1) * r, foot.y, foot.z + Math.sin(a1) * r).color(0.05F, 0.03F, 0.04F, 0.55F).endVertex();
            b.pos(foot.x + Math.cos(a1) * r, foot.y + h + 0.16D, foot.z + Math.sin(a1) * r).color(0.2F, 0.16F, 0.18F, edge).endVertex();
            b.pos(foot.x + Math.cos(a0) * r, foot.y + h + 0.16D, foot.z + Math.sin(a0) * r).color(0.2F, 0.16F, 0.18F, edge).endVertex();
        }
        t.draw();
        // Iron bands: foot, waist and crown.
        for (double y : new double[]{0D, h * 0.5D, h + 0.08D}) {
            band(b, t, foot.add(0D, y, 0D), r + 0.03D, 0.09D);
        }
        // What leaks out of it, a little, into the dark.
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        float breathe = 0.5F + 0.5F * MathHelper.sin(age * 0.04F);
        WarlockFx.glow(foot.add(0D, h * 0.5D, 0D), 1.2D, AETHER, 0.12F + 0.08F * breathe);
        WarlockFx.glow(foot.add(0D, h * 0.5D, 0D), 0.45D, AETHER, 0.18F);
        end();
    }

    private static void color(BufferBuilder b, double x, double y, double z, float swirl) {
        float r = AETHER_DEEP[0] + (AETHER[0] - AETHER_DEEP[0]) * swirl;
        float g = AETHER_DEEP[1] + (AETHER[1] - AETHER_DEEP[1]) * swirl * swirl;
        float bl = AETHER_DEEP[2] + (AETHER[2] - AETHER_DEEP[2]) * swirl;
        b.pos(x, y, z).color(r, g, bl, 0.92F).endVertex();
    }

    private static void band(BufferBuilder b, Tessellator t, Vec3d c, double r, double h) {
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        b.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        int sides = 12;
        for (int i = 0; i < sides; i++) {
            double a0 = i * Math.PI * 2D / sides, a1 = (i + 1) * Math.PI * 2D / sides;
            float lit = 0.12F + 0.1F * (float) Math.max(0D, Math.cos(a0 - 0.8D));
            b.pos(c.x + Math.cos(a0) * r, c.y, c.z + Math.sin(a0) * r).color(lit, lit, lit * 1.05F, 1F).endVertex();
            b.pos(c.x + Math.cos(a1) * r, c.y, c.z + Math.sin(a1) * r).color(lit, lit, lit * 1.05F, 1F).endVertex();
            b.pos(c.x + Math.cos(a1) * r, c.y + h, c.z + Math.sin(a1) * r).color(lit * 1.4F, lit * 1.4F, lit * 1.45F, 1F).endVertex();
            b.pos(c.x + Math.cos(a0) * r, c.y + h, c.z + Math.sin(a0) * r).color(lit * 1.4F, lit * 1.4F, lit * 1.45F, 1F).endVertex();
        }
        t.draw();
    }

    // ------------------------------------------------------------------ the moments

    /** Everything passing, drawn where it happens; the camera is at the origin. */
    public static void bursts(Vec3d camera, float pt) {
        if (BURSTS.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        begin();
        Tessellator t = Tessellator.getInstance();
        BufferBuilder b = t.getBuffer();
        for (Burst burst : BURSTS) {
            float k = (burst.age + pt) / burst.life;
            Vec3d c = burst.a.subtract(camera);
            switch (burst.kind) {
                case WARD_OUT: {
                    // A flash, and the light falling inward to nothing.
                    float flash = (1F - k) * (1F - k);
                    WarlockFx.glow(c, 1D + 5D * k, WHITE, 0.6F * flash);
                    WarlockFx.glow(c, 3D * (1D - k), WARD, 0.4F * flash);
                    break;
                }
                case AETHER_TAKEN: {
                    // Shards outward; the red pours out of the broken column and into whoever touched it.
                    net.minecraft.entity.Entity host = mc.world.getEntityByID(burst.entity);
                    Vec3d to = host != null ? host.getPositionEyes(pt).add(0D, -0.4D, 0D).subtract(camera) : c;
                    WarlockFx.glow(c, 0.8D + 1.6D * k, AETHER, 0.4F * (1F - k));
                    Random rand = new Random(burst.entity * 31L);
                    for (int i = 0; i < 9; i++) {
                        float lag = i * 0.05F;
                        float local = MathHelper.clamp((k - lag) / 0.55F, 0F, 1F);
                        if (local <= 0F || local >= 1F) {
                            continue;
                        }
                        Vec3d bend = new Vec3d(rand.nextGaussian() * 0.9D, 0.6D + rand.nextDouble(), rand.nextGaussian() * 0.9D);
                        Vec3d head = curve(c, to, bend, local);
                        Vec3d tail = curve(c, to, bend, Math.max(0F, local - 0.25F));
                        beam(b, t, tail, head, 0.05D, AETHER, 0.7F);
                        WarlockFx.glow(head, 0.22D, AETHER, 0.6F);
                    }
                    if (k > 0.5F) {
                        WarlockFx.glow(to, 1.2D, AETHER, 0.5F * (1F - k) * 2F);
                    }
                    break;
                }
                case FLAME_SET: {
                    // Drawn out of them into the fire, the fire flaring white round it, and then nothing.
                    net.minecraft.entity.Entity host = mc.world.getEntityByID(burst.entity);
                    Vec3d from = host != null ? host.getPositionEyes(pt).add(0D, -0.4D, 0D).subtract(camera) : c;
                    float draw = MathHelper.clamp(k / 0.45F, 0F, 1F);
                    if (draw < 1F) {
                        beam(b, t, from.add(c.subtract(from).scale(Math.max(0F, draw - 0.3F))), from.add(c.subtract(from).scale(draw)), 0.08D, AETHER, 0.75F);
                    }
                    float flare = MathHelper.clamp((k - 0.4F) / 0.15F, 0F, 1F) * (1F - MathHelper.clamp((k - 0.55F) / 0.35F, 0F, 1F));
                    WarlockFx.glow(c, 0.6D + 2.4D * flare, WHITE, 0.8F * flare);
                    WarlockFx.glow(c, 0.5D, AETHER, 0.6F * flare);
                    break;
                }
                case RUNE_LIT:
                    WarlockFx.glow(c, 1.6D * (0.5D + k), RUNE, 0.35F * (1F - k));
                    break;
                case RUNE_FAIL:
                    WarlockFx.glow(c, 2D, new float[]{1F, 0.25F, 0.15F}, 0.5F * (1F - k));
                    break;
                default:
                    break;
            }
        }
        end();
    }

    private static Vec3d curve(Vec3d a, Vec3d b, Vec3d bend, float s) {
        Vec3d mid = a.add(b).scale(0.5D).add(bend);
        double u = 1D - s;
        return a.scale(u * u).add(mid.scale(2D * u * s)).add(b.scale(s * s));
    }

    // ------------------------------------------------------------------ shapes

    /** A vertical shaft of light: a crossed pair of quads, bright at the foot, gone at the top. */
    private static void shaft(BufferBuilder b, Tessellator t, Vec3d base, double w, double h, float[] col, float a) {
        b.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < 2; i++) {
            double dx = i == 0 ? w : 0D, dz = i == 0 ? 0D : w;
            b.pos(base.x - dx, base.y, base.z - dz).color(col[0], col[1], col[2], a).endVertex();
            b.pos(base.x + dx, base.y, base.z + dz).color(col[0], col[1], col[2], a).endVertex();
            b.pos(base.x + dx, base.y + h, base.z + dz).color(col[0], col[1], col[2], 0F).endVertex();
            b.pos(base.x - dx, base.y + h, base.z - dz).color(col[0], col[1], col[2], 0F).endVertex();
        }
        t.draw();
    }

    /** A line of light from one point to another, turned to face the camera. */
    private static void beam(BufferBuilder b, Tessellator t, Vec3d from, Vec3d to, double w, float[] col, float a) {
        Vec3d along = to.subtract(from);
        Vec3d mid = from.add(to).scale(0.5D);
        Vec3d side = along.crossProduct(mid);
        if (side.lengthSquared() < 1.0E-8) {
            return;
        }
        side = side.normalize().scale(w);
        b.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        b.pos(from.x - side.x, from.y - side.y, from.z - side.z).color(col[0], col[1], col[2], a * 0.4F).endVertex();
        b.pos(from.x + side.x, from.y + side.y, from.z + side.z).color(col[0], col[1], col[2], a * 0.4F).endVertex();
        b.pos(to.x + side.x, to.y + side.y, to.z + side.z).color(col[0], col[1], col[2], a).endVertex();
        b.pos(to.x - side.x, to.y - side.y, to.z - side.z).color(col[0], col[1], col[2], a).endVertex();
        t.draw();
    }
}
