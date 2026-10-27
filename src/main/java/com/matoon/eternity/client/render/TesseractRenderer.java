package com.matoon.eternity.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;
import java.util.Random;

/**
 * The Tesseract, replacing HeroesExpansion's: a cube of clear glass, and in the middle of
 * it the Space Stone, a white star. Its light fills the cube to the glass in deep blue and
 * turquoise clouds, and flakes of light come loose from it and drift slowly outward,
 * turning and fading before they reach the walls. The glass shows itself only as glass
 * does: brighter toward its edges, and a soft sheen where the light falls on it.
 * <p>
 * Drawn wherever the item is drawn: in hand, in the inventory, in a frame, and on the
 * ground as HeroesExpansion's Tesseract entity.
 */
@SideOnly(Side.CLIENT)
public class TesseractRenderer extends TileEntityItemStackRenderer {

    /** Half the cube's edge, in item space (a block is 1). */
    private static final float H = 0.25F;
    private static final int SHELLS = 11;
    private static final int FLAKES = 60;
    private static final float[] LIGHT = normalize(new float[]{-0.45F, 0.8F, 0.4F});
    /** The faces of a unit cube: normal axis, sign. */
    private static final int[][] FACES = {{0, 1}, {0, -1}, {1, 1}, {1, -1}, {2, 1}, {2, -1}};
    /** How finely each face of the glass is divided, so its sheen can vary across it. */
    private static final int GLASS_GRID = 8;

    private static final FloatBuffer MODELVIEW = GLAllocation.createDirectFloatBuffer(16);
    private static final FloatBuffer PROJECTION = GLAllocation.createDirectFloatBuffer(16);
    private static final FloatBuffer PLANE = GLAllocation.createDirectFloatBuffer(4);

    /** The soft inner shape around the stone: quads of a cube pushed out toward a sphere. */
    private static final float[][] CORE = roundedCube(8, 6F);
    /** Per shell: three texgen plane pairs, then two drift speeds and a spin. */
    private static final float[][] SHELL_PLANES = new float[SHELLS][];

    private static DynamicTexture clouds;
    private static DynamicTexture motes;
    private static DynamicTexture glow;
    private static DynamicTexture flake;
    private static DynamicTexture ring;

    static {
        Random rand = new Random(1012L);
        for (int i = 0; i < SHELLS; i++) {
            float[] p = new float[21];
            for (int k = 0; k < 3; k++) {
                float[] a = randomUnit(rand), b = normalize(cross(a, randomUnit(rand)));
                System.arraycopy(a, 0, p, k * 6, 3);
                System.arraycopy(b, 0, p, k * 6 + 3, 3);
            }
            p[18] = (rand.nextFloat() - 0.5F) * 0.01F;
            p[19] = (rand.nextFloat() - 0.5F) * 0.01F;
            p[20] = (rand.nextFloat() - 0.5F) * 0.4F;
            SHELL_PLANES[i] = p;
        }
    }

    // Right and up on screen, and toward the viewer, in the space being drawn in.
    private float rx, ry, rz, ux, uy, uz, vx, vy, vz;
    /** Whether the space is mirrored (items in a GUI are drawn upside down), which swaps which faces are near. */
    private boolean mirrored;
    /** Whether it is seen through a perspective, in the world or in hand, rather than flat in a slot or on a screen. */
    private boolean inWorld;

    /**
     * The overload everything ends in: vanilla's one-argument form calls it, and items that
     * hold stones (Lucraft's gauntlet, Star-Tech's Cosmi-Rod) call it directly. Overriding
     * only the other left those vanilla's fallback, which draws a chest.
     */
    @Override
    public void renderByItem(ItemStack stack, float partialTicks) {
        float time = (Minecraft.getSystemTime() % 3600000L) / 50F;
        GlStateManager.pushMatrix();
        GlStateManager.translate(0.5F, 0.5F, 0.5F);
        draw(time);
        GlStateManager.popMatrix();
    }

    /** Draws the Tesseract centred on the current origin. */
    public void draw(float time) {
        // It is drawn inside other items' renderers too, so it puts back exactly what it found.
        GlSnapshot before = GlSnapshot.take();
        ensureTextures();
        readView();
        float pulse = 0.9F + 0.07F * MathHelper.sin(time * 0.09F) + 0.03F * MathHelper.sin(time * 0.31F);
        // A slow swell, some six seconds from bright to dim and back, for the light it throws.
        float swell = 0.5F + 0.5F * MathHelper.sin(time * 0.052F);
        swell = swell * swell * (3F - 2F * swell);

        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        GlStateManager.depthMask(false);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);

        // The light it throws around itself.
        GlStateManager.enableTexture2D();
        GlStateManager.disableCull();
        GlStateManager.bindTexture(glow.getGlTextureId());
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        billboard(0F, 0F, 0F, H * (2F + 0.4F * swell), 0F, 0.2F, 0.5F, 1F, (0.2F + 0.14F * swell) * pulse);

        // The far walls, deep blue: the body of the light, seen through.
        GlStateManager.disableTexture2D();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableCull();
        cullNear(true);
        GlStateManager.color(0.02F, 0.1F, 0.36F, 0.7F);
        cube(H * 0.995F);

        // The inside of the far glass catches a little light too.
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        glass(0.5F);
        cullNear(false);

        // The light filling it, cloud upon cloud.
        GlStateManager.enableTexture2D();
        GlStateManager.disableCull();
        storm(time, pulse);

        // A soft, rounded brightness around the stone.
        GlStateManager.disableTexture2D();
        GlStateManager.enableCull();
        float[][] core = {{0.5F, 0.1F}, {0.64F, 0.06F}, {0.8F, 0.04F}};
        for (float[] layer : core) {
            float breathe = 1F + 0.04F * MathHelper.sin(time * 0.1F + layer[0] * 9F);
            GlStateManager.color(0.6F, 0.85F, 1F, layer[1] * pulse);
            roundedCube(H * layer[0] * breathe);
        }

        // Flakes of light coming loose from the stone and drifting out.
        GlStateManager.enableTexture2D();
        GlStateManager.disableCull();
        flakes(time);

        // The stone itself: a white star.
        GlStateManager.bindTexture(glow.getGlTextureId());
        billboard(0F, 0F, 0F, H * 1.25F, 0F, 0.55F, 0.85F, 1F, 0.42F * pulse);
        billboard(0F, 0F, 0F, H * 0.6F, 0F, 0.9F, 0.97F, 1F, 0.6F * pulse);
        billboard(0F, 0F, 0F, H * 0.24F, 0F, 1F, 1F, 1F, 0.95F);

        // The near glass.
        GlStateManager.disableTexture2D();
        GlStateManager.enableCull();
        glass(1F);

        // Its light flaring in the eye. Not in a slot or any other screen, where it would spill over everything beside it.
        if (inWorld) {
            GlStateManager.enableTexture2D();
            GlStateManager.disableCull();
            flare(time, swell);
        }

        before.restore();
    }

    /**
     * A lens flare, swelling and fading slowly: a long blue streak across the view with a
     * bright thread at its core, a starburst of uneven rays turning very slowly, and a faint
     * violet ring around it all.
     */
    private void flare(float time, float swell) {
        float strength = 0.25F + 0.75F * swell;
        GlStateManager.bindTexture(glow.getGlTextureId());
        sprite(0F, 0F, 0F, H * (5F + 2.5F * swell), H * 0.16F, 0F, 0.2F, 0.5F, 1F, 0.45F * strength);
        sprite(0F, 0F, 0F, H * (3.2F + 1.5F * swell), H * 0.045F, 0F, 0.7F, 0.9F, 1F, 0.75F * strength);
        Random rand = new Random(5L);
        float turn = time * 0.0035F;
        for (int i = 0; i < 8; i++) {
            float length = H * (1.1F + 1.4F * rand.nextFloat()) * (0.8F + 0.4F * swell);
            float angle = turn + i * (float) Math.PI / 8F + (rand.nextFloat() - 0.5F) * 0.2F;
            sprite(0F, 0F, 0F, length, H * 0.035F, angle, 0.6F, 0.85F, 1F, (0.18F + 0.14F * rand.nextFloat()) * strength);
        }
        GlStateManager.bindTexture(ring.getGlTextureId());
        billboard(0F, 0F, 0F, H * (2.6F + 0.3F * swell), 0F, 0.55F, 0.5F, 1F, 0.15F * strength);
    }

    /**
     * Nested shells, each sampling the cloud fields through its own planes in object
     * space, drifting their own slow way. Seen through, they pile up toward the middle, so
     * the light deepens from blue at the glass to pale turquoise around the stone.
     */
    private void storm(float time, float pulse) {
        GlStateManager.texGen(GlStateManager.TexGen.S, GL11.GL_OBJECT_LINEAR);
        GlStateManager.texGen(GlStateManager.TexGen.T, GL11.GL_OBJECT_LINEAR);
        GlStateManager.enableTexGenCoord(GlStateManager.TexGen.S);
        GlStateManager.enableTexGenCoord(GlStateManager.TexGen.T);
        for (int i = 0; i < SHELLS; i++) {
            float[] p = SHELL_PLANES[i];
            float size = H * (0.98F - 0.055F * i);
            float depth = i / (float) (SHELLS - 1);

            // Broad billows of blue light, filling the glass.
            GlStateManager.bindTexture(clouds.getGlTextureId());
            planes(p, 0, 1.6F + i * 0.2F);
            drift(time, p[19] * 0.6F, -p[18] * 0.6F, p[20] * 0.5F);
            shell(size, i == 0, 0.08F + 0.12F * depth, 0.45F + 0.3F * depth, 1F, (0.1F + 0.02F * depth) * pulse);
            undrift();

            // Finer cloud through them.
            planes(p, 6, 3F + i * 0.3F);
            drift(time, p[18], p[19], p[20]);
            shell(size, i == 0, 0.14F + 0.18F * depth, 0.6F + 0.25F * depth, 1F, (0.08F + 0.02F * depth) * pulse);
            undrift();

            // Pale motes of light caught in the clouds.
            GlStateManager.bindTexture(motes.getGlTextureId());
            planes(p, 12, 2.6F + i * 0.3F);
            drift(time, -p[19] * 1.2F, p[18] * 1.2F, -p[20]);
            shell(size, i == 0, 0.6F + 0.3F * depth, 0.9F, 1F, (0.11F + 0.04F * depth) * pulse);
            undrift();
        }
        GlStateManager.disableTexGenCoord(GlStateManager.TexGen.S);
        GlStateManager.disableTexGenCoord(GlStateManager.TexGen.T);
    }

    private static void planes(float[] p, int offset, float frequency) {
        GlStateManager.texGen(GlStateManager.TexGen.S, GL11.GL_OBJECT_PLANE,
                plane(p[offset] * frequency, p[offset + 1] * frequency, p[offset + 2] * frequency, 0.37F * offset));
        GlStateManager.texGen(GlStateManager.TexGen.T, GL11.GL_OBJECT_PLANE,
                plane(p[offset + 3] * frequency, p[offset + 4] * frequency, p[offset + 5] * frequency, 0.61F * offset));
    }

    private static void drift(float time, float sx, float sy, float spin) {
        GlStateManager.matrixMode(GL11.GL_TEXTURE);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        GlStateManager.translate(time * sx, time * sy, 0F);
        GlStateManager.rotate(time * spin, 0F, 0F, 1F);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
    }

    private static void undrift() {
        GlStateManager.matrixMode(GL11.GL_TEXTURE);
        GlStateManager.popMatrix();
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
    }

    /**
     * One shell of the storm. Faces seen edge-on are faded: squeezed into a sliver of the
     * screen, their texture would show as streaks. Inner shells also fade toward their own
     * borders, so where one ends there is no step in the light.
     */
    private void shell(float s, boolean outer, float r, float g, float b, float alpha) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        float[] view = {vx, vy, vz};
        int grid = outer ? 1 : 6;
        for (int[] face : FACES) {
            float facing = Math.abs(view[face[0]]);
            float a = alpha * (0.15F + 0.85F * facing * facing);
            float[][] c = faceCorners(face, s);
            for (int i = 0; i < grid; i++) {
                for (int j = 0; j < grid; j++) {
                    float[][] uv = {{i, j}, {i + 1, j}, {i + 1, j + 1}, {i, j + 1}};
                    for (float[] corner : uv) {
                        float u = corner[0] / grid, v = corner[1] / grid;
                        float[] p = new float[3];
                        for (int d = 0; d < 3; d++) {
                            p[d] = c[0][d] * (1 - u) * (1 - v) + c[1][d] * u * (1 - v) + c[2][d] * u * v + c[3][d] * (1 - u) * v;
                        }
                        float edge = Math.max(Math.abs(u * 2F - 1F), Math.abs(v * 2F - 1F));
                        float taper = outer ? 1F : 1F - 0.9F * smooth((edge - 0.35F) / 0.65F);
                        buffer.pos(p[0], p[1], p[2]).color(r, g, b, a * taper).endVertex();
                    }
                }
            }
        }
        tessellator.draw();
    }

    private static float smooth(float x) {
        x = MathHelper.clamp(x, 0F, 1F);
        return x * x * (3F - 2F * x);
    }

    /**
     * Flakes of light loosening from the stone. Each drifts out along its own line, curling a
     * little, growing and turning as it goes, and fades before it reaches the glass; then
     * another comes loose somewhere else.
     */
    private void flakes(float time) {
        GlStateManager.bindTexture(flake.getGlTextureId());
        for (int i = 0; i < FLAKES; i++) {
            Random seed = new Random(i * 4211L + 17L);
            float life = 70F + seed.nextFloat() * 80F;
            float clock = time + seed.nextFloat() * life;
            int cycle = (int) (clock / life);
            float t = (clock - cycle * life) / life;

            Random rand = new Random(i * 92821L + cycle * 68917L);
            float[] dir = randomUnit(rand);
            float[] axis = normalize(cross(dir, randomUnit(rand)));
            float reach = H / Math.max(Math.abs(dir[0]), Math.max(Math.abs(dir[1]), Math.abs(dir[2]))) * 0.9F;
            float radial = H * 0.06F + (reach - H * 0.06F) * (float) Math.pow(t, 0.75D);
            float curl = (rand.nextFloat() - 0.5F) * 1.4F * t;
            float[] p = rotate(dir, axis, curl);

            float fade = MathHelper.sin((float) Math.PI * t);
            float alpha = fade * (0.9F - 0.35F * t);
            float size = H * (0.14F + 0.3F * t) * (0.7F + 0.6F * rand.nextFloat());
            float angle = rand.nextFloat() * 6.283F + t * (rand.nextFloat() - 0.5F) * 3F;
            float r = 0.9F - 0.4F * t, g = 0.97F - 0.15F * t;
            billboard(p[0] * radial, p[1] * radial, p[2] * radial, size, angle, r, g, 1F, alpha);
        }
    }

    /**
     * The glass, seen from the side being drawn: faint at the middle of each face and
     * brighter toward its edges, as light skims along glass, with a soft band of sheen
     * across the faces turned toward the light.
     */
    private void glass(float strength) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        float s = H * 1.001F;
        for (int[] face : FACES) {
            float[] n = {0F, 0F, 0F};
            n[face[0]] = face[1];
            float lit = 0.35F + 0.65F * Math.max(0F, dot(n, LIGHT));
            float[][] c = faceCorners(face, s);
            for (int i = 0; i < GLASS_GRID; i++) {
                for (int j = 0; j < GLASS_GRID; j++) {
                    float[][] uv = {{i, j}, {i + 1, j}, {i + 1, j + 1}, {i, j + 1}};
                    for (float[] corner : uv) {
                        float u = corner[0] / GLASS_GRID, v = corner[1] / GLASS_GRID;
                        float[] p = new float[3];
                        for (int d = 0; d < 3; d++) {
                            p[d] = c[0][d] * (1 - u) * (1 - v) + c[1][d] * u * (1 - v) + c[2][d] * u * v + c[3][d] * (1 - u) * v;
                        }
                        float edge = Math.max(Math.abs(u * 2F - 1F), Math.abs(v * 2F - 1F));
                        float rim = 0.03F + 0.17F * edge * edge * edge * edge;
                        float diagonal = (u + v) * 0.5F - 0.32F;
                        float sheen = (float) Math.exp(-diagonal * diagonal / 0.012F) * 0.12F * lit * lit;
                        float alpha = (rim + sheen) * strength;
                        float white = sheen / Math.max(0.001F, rim + sheen);
                        buffer.pos(p[0], p[1], p[2]).color(0.55F + 0.45F * white, 0.85F + 0.15F * white, 1F, alpha).endVertex();
                    }
                }
            }
        }
        tessellator.draw();
    }

    // ------------------------------------------------------------------ geometry

    private static void cube(float s) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        for (int[] face : FACES) {
            for (float[] p : faceCorners(face, s)) {
                buffer.pos(p[0], p[1], p[2]).endVertex();
            }
        }
        tessellator.draw();
    }

    /** A face's corners, wound counter-clockwise seen from outside. */
    private static float[][] faceCorners(int[] face, float s) {
        int axis = face[0], sign = face[1];
        int a = (axis + 1) % 3, b = (axis + 2) % 3;
        float[][] corners = new float[4][3];
        float[][] uv = sign > 0 ? new float[][]{{-1, -1}, {1, -1}, {1, 1}, {-1, 1}} : new float[][]{{-1, -1}, {-1, 1}, {1, 1}, {1, -1}};
        for (int i = 0; i < 4; i++) {
            corners[i][axis] = sign * s;
            corners[i][a] = uv[i][0] * s;
            corners[i][b] = uv[i][1] * s;
        }
        return corners;
    }

    private static void roundedCube(float s) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        for (float[] quad : CORE) {
            for (int i = 0; i < 4; i++) {
                buffer.pos(quad[i * 3] * s, quad[i * 3 + 1] * s, quad[i * 3 + 2] * s).endVertex();
            }
        }
        tessellator.draw();
    }

    /** A sprite facing the viewer, turned by {@code angle} on the screen, using the texture already bound. */
    private void billboard(float x, float y, float z, float size, float angle, float r, float g, float b, float alpha) {
        sprite(x, y, z, size, size, angle, r, g, b, alpha);
    }

    /** A sprite facing the viewer, {@code width} by {@code height} before it is turned by {@code angle}. */
    private void sprite(float x, float y, float z, float width, float height, float angle, float r, float g, float b, float alpha) {
        float cos = MathHelper.cos(angle), sin = MathHelper.sin(angle);
        // The sprite's own right and up: the screen's, turned.
        float ax = (rx * cos + ux * sin) * width, ay = (ry * cos + uy * sin) * width, az = (rz * cos + uz * sin) * width;
        float bx = (ux * cos - rx * sin) * height, by = (uy * cos - ry * sin) * height, bz = (uz * cos - rz * sin) * height;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        buffer.pos(x - ax - bx, y - ay - by, z - az - bz).tex(0, 0).color(r, g, b, alpha).endVertex();
        buffer.pos(x + ax - bx, y + ay - by, z + az - bz).tex(1, 0).color(r, g, b, alpha).endVertex();
        buffer.pos(x + ax + bx, y + ay + by, z + az + bz).tex(1, 1).color(r, g, b, alpha).endVertex();
        buffer.pos(x - ax + bx, y - ay + by, z - az + bz).tex(0, 1).color(r, g, b, alpha).endVertex();
        tessellator.draw();
    }

    /** Culls the near faces (to draw the far ones) or the far faces, whichever way the space is wound. */
    private void cullNear(boolean near) {
        GlStateManager.cullFace(near != mirrored ? GlStateManager.CullFace.FRONT : GlStateManager.CullFace.BACK);
    }

    /** The screen's axes in the space being drawn in, from the rows of the modelview matrix. */
    private void readView() {
        GlStateManager.getFloat(GL11.GL_MODELVIEW_MATRIX, MODELVIEW);
        GlStateManager.getFloat(GL11.GL_PROJECTION_MATRIX, PROJECTION);
        inWorld = PROJECTION.get(11) != 0F;
        float[] right = normalize(new float[]{MODELVIEW.get(0), MODELVIEW.get(4), MODELVIEW.get(8)});
        float[] up = normalize(new float[]{MODELVIEW.get(1), MODELVIEW.get(5), MODELVIEW.get(9)});
        float[] view = normalize(new float[]{MODELVIEW.get(2), MODELVIEW.get(6), MODELVIEW.get(10)});
        mirrored = dot(cross(new float[]{MODELVIEW.get(0), MODELVIEW.get(1), MODELVIEW.get(2)},
                new float[]{MODELVIEW.get(4), MODELVIEW.get(5), MODELVIEW.get(6)}),
                new float[]{MODELVIEW.get(8), MODELVIEW.get(9), MODELVIEW.get(10)}) < 0F;
        rx = right[0];
        ry = right[1];
        rz = right[2];
        ux = up[0];
        uy = up[1];
        uz = up[2];
        vx = view[0];
        vy = view[1];
        vz = view[2];
    }

    /** A cube's faces divided into a grid and pulled toward a sphere: a block with softened edges. */
    private static float[][] roundedCube(int grid, float power) {
        float[][] quads = new float[6 * grid * grid][];
        int n = 0;
        for (int[] face : FACES) {
            float[][] c = faceCorners(face, 1F);
            for (int i = 0; i < grid; i++) {
                for (int j = 0; j < grid; j++) {
                    float[] quad = new float[12];
                    float[][] uv = {{i, j}, {i + 1, j}, {i + 1, j + 1}, {i, j + 1}};
                    for (int k = 0; k < 4; k++) {
                        float u = uv[k][0] / grid, v = uv[k][1] / grid;
                        float[] p = new float[3];
                        for (int d = 0; d < 3; d++) {
                            // Bilinear across the face: corner 0 at (0,0), 1 at (1,0), 2 at (1,1), 3 at (0,1).
                            p[d] = c[0][d] * (1 - u) * (1 - v) + c[1][d] * u * (1 - v) + c[2][d] * u * v + c[3][d] * (1 - u) * v;
                        }
                        float norm = (float) Math.pow(Math.pow(Math.abs(p[0]), power) + Math.pow(Math.abs(p[1]), power)
                                + Math.pow(Math.abs(p[2]), power), 1D / power);
                        quad[k * 3] = p[0] / norm;
                        quad[k * 3 + 1] = p[1] / norm;
                        quad[k * 3 + 2] = p[2] / norm;
                    }
                    quads[n++] = quad;
                }
            }
        }
        return quads;
    }

    // ------------------------------------------------------------------ textures

    private static void ensureTextures() {
        if (clouds != null) {
            return;
        }
        clouds = texture(128, (u, v) -> {
            float f = fbm(u * 4F, v * 4F, 4, 5, 11);
            float c = MathHelper.clamp((f * 0.5F + 0.5F - 0.3F) / 0.55F, 0F, 1F);
            return grey(c * c * (3F - 2F * c));
        });
        motes = texture(256, (u, v) -> {
            // Scattered wisps: a warped field, kept only where it peaks.
            float x = u * 4F, y = v * 4F;
            float wx = x + 1.2F * fbm(x, y, 4, 3, 23), wy = y + 1.2F * fbm(x + 5.2F, y + 1.3F, 4, 3, 29);
            float f = fbm(wx * 2F, wy * 2F, 8, 4, 37);
            float c = MathHelper.clamp((f - 0.25F) / 0.5F, 0F, 1F);
            return grey(c * c);
        });
        ring = texture(128, (u, v) -> {
            float dx = u - 0.5F, dy = v - 0.5F;
            float d = MathHelper.sqrt(dx * dx + dy * dy) * 2F;
            float a = (float) Math.exp(-(d - 0.82F) * (d - 0.82F) / 0.004F) + 0.25F * (float) Math.exp(-(d - 0.7F) * (d - 0.7F) / 0.02F);
            return ((int) (MathHelper.clamp(a, 0F, 1F) * 255F) << 24) | 0xFFFFFF;
        });
        glow = texture(64, (u, v) -> {
            float dx = u - 0.5F, dy = v - 0.5F;
            float d = MathHelper.sqrt(dx * dx + dy * dy) * 2F;
            float a = d >= 1F ? 0F : (float) Math.pow(1F - d, 2.2F);
            return ((int) (a * 255F) << 24) | 0xFFFFFF;
        });
        flake = texture(64, (u, v) -> {
            // A torn wisp: a soft blot, frayed at its edges by noise and drawn out along one axis.
            float dx = (u - 0.5F) * 2F, dy = (v - 0.5F) * 2.8F;
            float d = MathHelper.sqrt(dx * dx + dy * dy);
            float body = d >= 1F ? 0F : (float) Math.pow(1F - d, 1.4F);
            float fray = MathHelper.clamp(fbm(u * 4F, v * 4F, 4, 4, 53) * 0.8F + 0.55F, 0F, 1F);
            float a = MathHelper.clamp(body * fray * 1.6F, 0F, 1F);
            return ((int) (a * 255F) << 24) | 0xFFFFFF;
        });
    }

    private interface Pixel {
        int argb(float u, float v);
    }

    private static DynamicTexture texture(int size, Pixel pixel) {
        DynamicTexture texture = new DynamicTexture(size, size);
        int[] data = texture.getTextureData();
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                data[y * size + x] = pixel.argb((x + 0.5F) / size, (y + 0.5F) / size);
            }
        }
        texture.updateDynamicTexture();
        // Smooth, and tiling across every face.
        GlStateManager.bindTexture(texture.getGlTextureId());
        GlStateManager.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GlStateManager.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GlStateManager.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GlStateManager.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
        return texture;
    }

    private static int grey(float value) {
        int c = (int) (MathHelper.clamp(value, 0F, 1F) * 255F);
        return 0xFF000000 | c << 16 | c << 8 | c;
    }

    /** Fractal noise that tiles every {@code period} units, roughly in [-1, 1]. */
    private static float fbm(float x, float y, int period, int octaves, int seed) {
        float sum = 0F, amp = 0.65F;
        for (int o = 0; o < octaves; o++) {
            sum += amp * perlin(x * (1 << o), y * (1 << o), period << o, seed + o);
            amp *= 0.5F;
        }
        return sum * 1.4F;
    }

    /** Gradient noise on a lattice that wraps every {@code period} cells. */
    private static float perlin(float x, float y, int period, int seed) {
        int x0 = MathHelper.floor(x), y0 = MathHelper.floor(y);
        float fx = x - x0, fy = y - y0;
        float u = fx * fx * fx * (fx * (fx * 6F - 15F) + 10F), v = fy * fy * fy * (fy * (fy * 6F - 15F) + 10F);
        float n00 = gradient(x0, y0, period, seed, fx, fy);
        float n10 = gradient(x0 + 1, y0, period, seed, fx - 1F, fy);
        float n01 = gradient(x0, y0 + 1, period, seed, fx, fy - 1F);
        float n11 = gradient(x0 + 1, y0 + 1, period, seed, fx - 1F, fy - 1F);
        float a = n00 + u * (n10 - n00), b = n01 + u * (n11 - n01);
        return (a + v * (b - a)) * 1.41F;
    }

    private static float gradient(int x, int y, int period, int seed, float dx, float dy) {
        int h = Math.floorMod(x, period) * 374761393 + Math.floorMod(y, period) * 668265263 + seed * 1442695041;
        h = (h ^ (h >>> 13)) * 1274126177;
        h ^= h >>> 16;
        double angle = (h & 1023) / 1024D * Math.PI * 2D;
        return (float) (Math.cos(angle) * dx + Math.sin(angle) * dy);
    }

    // ------------------------------------------------------------------ vectors

    private static FloatBuffer plane(float a, float b, float c, float d) {
        PLANE.clear();
        PLANE.put(a).put(b).put(c).put(d);
        PLANE.flip();
        return PLANE;
    }

    /** Turns v about a unit axis by the given angle. */
    private static float[] rotate(float[] v, float[] axis, float angle) {
        float cos = MathHelper.cos(angle), sin = MathHelper.sin(angle);
        float[] c = cross(axis, v);
        float d = dot(axis, v) * (1F - cos);
        return new float[]{v[0] * cos + c[0] * sin + axis[0] * d, v[1] * cos + c[1] * sin + axis[1] * d, v[2] * cos + c[2] * sin + axis[2] * d};
    }

    private static float[] randomUnit(Random rand) {
        return normalize(new float[]{rand.nextFloat() * 2F - 1F, rand.nextFloat() * 2F - 1F, rand.nextFloat() * 2F - 1F});
    }

    private static float[] cross(float[] a, float[] b) {
        return new float[]{a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    private static float dot(float[] a, float[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private static float[] normalize(float[] v) {
        float len = (float) Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        return len < 1.0E-6F ? new float[]{0F, 1F, 0F} : new float[]{v[0] / len, v[1] / len, v[2] / len};
    }
}
