package com.matoon.eternity.client.render;

import com.matoon.eternity.item.ItemCasket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;
import java.util.Random;

/**
 * The Casket of Ancient Winters: a chest of frosted blue glass, its panels bowed outward by
 * what is inside them and leaded with dark knotwork, held in a frame of black iron, clawed
 * at the corners and looped at the ends, a clasp on its lid. The winter inside lights it
 * from within, brightest low down, moving slowly through the glass like weather. Out in the
 * world cold mist spills over its rim and runs down its sides, and frost glitters off it.
 * <p>
 * Opened (held down, its winter gathering to be let out) the lid lifts on its hinge, shaking,
 * and the light inside pours out of the gap, blinding white at the heart.
 * <p>
 * Drawn wherever the item is drawn, and on its pedestal in Odin's Vault. The casket stands on
 * the origin, its length along x, its front (the clasp) toward +z.
 */
@SideOnly(Side.CLIENT)
public class CasketRenderer extends TileEntityItemStackRenderer {

    public static final CasketRenderer INSTANCE = new CasketRenderer();

    // The glass, inside the frame: half its length and depth, its foot, the seam under the lid, and the lid's top.
    static final double X = 0.47D, Z = 0.27D, Y0 = 0.05D, YS = 0.315D, Y1 = 0.55D;
    /** Where the lid hinges, along the back of the seam. */
    private static final double HINGE_Y = 0.335D, HINGE_Z = -Z - 0.02D;
    private static final int MIST = 12, GLINTS = 16;

    private static final float[] IRON = {0.27F, 0.28F, 0.32F};
    private static final float[] DEEP = {0.03F, 0.11F, 0.5F};

    private static final FloatBuffer MODELVIEW = GLAllocation.createDirectFloatBuffer(16);
    private static final FloatBuffer PROJECTION = GLAllocation.createDirectFloatBuffer(16);

    private static DynamicTexture glass;
    private static DynamicTexture iron;

    private float rx, ry, rz, ux, uy, uz;
    private boolean mirrored, inWorld;
    /** The light inside, this frame: how bright, and how far the winter has gathered. */
    private float pulse, charge, time;

    @Override
    public void renderByItem(ItemStack stack, float partialTicks) {
        float t = (Minecraft.getSystemTime() % 3600000L) / 50F;
        GlStateManager.pushMatrix();
        GlStateManager.translate(0.5F, 0.21F, 0.5F);
        GlStateManager.scale(0.72F, 0.72F, 0.72F);
        draw(t, gathered(stack, partialTicks));
        GlStateManager.popMatrix();
    }

    /** How far the winter in this stack has gathered, if someone is holding it open; 0 if not. */
    public static float gathered(ItemStack stack, float partialTicks) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null) {
            return 0F;
        }
        for (EntityPlayer player : mc.world.playerEntities) {
            if (player.isHandActive() && player.getActiveItemStack() == stack) {
                return ItemCasket.gathered(player, partialTicks);
            }
        }
        return 0F;
    }

    /** Draws the casket standing on the current origin. */
    public void draw(float t, float gathered) {
        GlSnapshot before = GlSnapshot.take();
        ensureTextures();
        readView();
        time = t;
        relight(t);
        charge = gathered;
        pulse = (0.85F + 0.1F * MathHelper.sin(t * 0.07F) + 0.05F * MathHelper.sin(t * 0.23F)) * (1F + 1.4F * charge);
        float lid = lidAngle(t);

        GlStateManager.disableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.enableDepth();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        float lightX = OpenGlHelper.lastBrightnessX, lightY = OpenGlHelper.lastBrightnessY;

        // The iron, lit by the place it is in.
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.disableCull();
        GlStateManager.depthMask(true);
        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.bindTexture(iron.getGlTextureId());
        Solid.begin(2D);
        Solid.color(IRON[0], IRON[1], IRON[2]);
        bodyFrame();
        Solid.end();
        GlStateManager.pushMatrix();
        hinge(lid);
        Solid.begin(2D);
        Solid.color(IRON[0], IRON[1], IRON[2]);
        lidFrame();
        Solid.end();
        GlStateManager.popMatrix();

        // Everything from here on burns with its own light.
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.depthMask(false);
        GlStateManager.enableCull();
        cullNear(false);

        if (lid > 0.5F) {
            // Opened, the inside: a pool of white light where the lid was.
            inside(lid);
        }
        GlStateManager.bindTexture(glass.getGlTextureId());
        for (int pass = 0; pass < 2; pass++) {
            if (pass == 0) {
                GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            } else {
                GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            }
            bodyGlass(pass);
            GlStateManager.pushMatrix();
            hinge(lid);
            lidGlass(pass);
            GlStateManager.popMatrix();
        }

        // The clasp's eyes, and the light at the seam.
        GlStateManager.disableTexture2D();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        GlStateManager.pushMatrix();
        hinge(lid);
        claspEyes();
        GlStateManager.popMatrix();

        GlStateManager.disableCull();
        GlStateManager.enableTexture2D();
        if (lid > 0.5F) {
            rays(lid);
        }
        if (inWorld) {
            GlStateManager.bindTexture(ProcTex.glow().getGlTextureId());
            billboard(0F, 0.3F, 0F, 1.25F + 1.6F * charge, 0F, 0.3F, 0.6F, 1F, (0.1F + 0.25F * charge) * Math.min(pulse, 1.6F));
            glints();
            GlStateManager.bindTexture(ProcTex.wisp().getGlTextureId());
            mist();
        }

        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightX, lightY);
        before.restore();
    }

    /** How far the lid stands open, in degrees: shut, or forced up by the winter gathering in it, rattling. */
    private float lidAngle(float t) {
        if (charge <= 0F) {
            return 0F;
        }
        float open = ProcTex.smooth(charge * 1.4F) * 34F;
        float rattle = (MathHelper.sin(t * 2.9F) * 0.6F + MathHelper.sin(t * 4.7F) * 0.4F) * (1.5F + 3F * charge);
        return Math.max(0F, open + rattle * Math.min(1F, charge * 4F));
    }

    private static void hinge(float degrees) {
        if (degrees <= 0F) {
            return;
        }
        GlStateManager.translate(0D, HINGE_Y, HINGE_Z);
        GlStateManager.rotate(-degrees, 1F, 0F, 0F);
        GlStateManager.translate(0D, -HINGE_Y, -HINGE_Z);
    }

    // ------------------------------------------------------------------ the iron

    private static void bodyFrame() {
        // Its foot: a heavy plate, stepped.
        Solid.box(-X - 0.05D, 0D, -Z - 0.05D, X + 0.05D, 0.03D, Z + 0.05D);
        Solid.box(-X - 0.03D, 0.03D, -Z - 0.03D, X + 0.03D, Y0 + 0.012D, Z + 0.03D);
        // Rims round the bottom of the glass and under the lid.
        rim(Y0, Y0 + 0.03D, 0.022D);
        rim(YS - 0.022D, YS, 0.026D);
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                double cx = sx * X, cz = sz * Z;
                // A post up each corner, thickening into a knuckle at the seam.
                Solid.boxAt(cx, (Y0 + YS) / 2D, cz, 0.07D, YS - Y0, 0.07D);
                Solid.boxAt(cx, YS - 0.035D, cz, 0.1D, 0.08D, 0.1D);
                Solid.boxAt(cx + sx * 0.012D, YS - 0.085D, cz + sz * 0.012D, 0.07D, 0.03D, 0.07D);
                Solid.boxAt(cx, Y0 + 0.02D, cz, 0.09D, 0.05D, 0.09D);
                // And from the knuckle a claw curling down the end to the foot, leaving a loop to lift it by.
                double[][] claw = Solid.curve(new double[]{cx + sx * 0.035D, YS - 0.07D, cz - sz * 0.01D},
                        new double[]{cx + sx * 0.16D, (Y0 + YS) * 0.42D, cz - sz * 0.04D}, new double[]{cx + sx * 0.03D, Y0 + 0.01D, cz - sz * 0.11D}, 9);
                Solid.tube(claw, 0.021D, 6);
            }
            // A boss in the middle of each end, where the claws come together under the glass.
            Solid.boxAt(sx * (X + 0.02D), Y0 + 0.035D, 0D, 0.05D, 0.05D, 0.16D);
        }
        // Down the middle of the front and the back, a thin bar of lead holds the glass.
        Solid.boxAt(0D, (Y0 + YS) / 2D, Z + 0.004D, 0.018D, YS - Y0, 0.012D);
        Solid.boxAt(0D, (Y0 + YS) / 2D, -Z - 0.004D, 0.018D, YS - Y0, 0.012D);
        // The hinge, two barrels along the back.
        for (int sx = -1; sx <= 1; sx += 2) {
            Solid.tube(new double[][]{{sx * 0.38D, HINGE_Y, HINGE_Z}, {sx * 0.18D, HINGE_Y, HINGE_Z}}, 0.022D, 6);
        }
    }

    private static void lidFrame() {
        // Its lip over the seam, and the rim round its top.
        rim(YS, YS + 0.024D, 0.03D);
        rim(Y1 - 0.02D, Y1 + 0.012D, 0.026D);
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                double cx = sx * X, cz = sz * Z;
                Solid.boxAt(cx, (YS + Y1) / 2D, cz, 0.064D, Y1 - YS, 0.064D);
                // Its corners capped, and the caps peaked.
                Solid.boxAt(cx, Y1 + 0.01D, cz, 0.095D, 0.05D, 0.095D);
                Solid.boxAt(cx, Y1 + 0.045D, cz, 0.05D, 0.025D, 0.05D);
            }
        }
        Solid.boxAt(0D, (YS + Y1) / 2D, Z + 0.004D, 0.018D, Y1 - YS, 0.012D);
        Solid.boxAt(0D, (YS + Y1) / 2D, -Z - 0.004D, 0.018D, Y1 - YS, 0.012D);
        // The clasp: a carved block over the front of the lid, a tongue hanging from it over the seam.
        Solid.box(-0.14D, Y1 - 0.012D, Z - 0.13D, 0.14D, Y1 + 0.06D, Z + 0.045D);
        Solid.box(-0.11D, Y1 + 0.06D, Z - 0.1D, 0.11D, Y1 + 0.08D, Z + 0.02D);
        Solid.box(-0.165D, Y1 + 0.005D, Z - 0.02D, -0.14D, Y1 + 0.045D, Z + 0.035D);
        Solid.box(0.14D, Y1 + 0.005D, Z - 0.02D, 0.165D, Y1 + 0.045D, Z + 0.035D);
        Solid.box(-0.065D, YS + 0.02D, Z + 0.01D, 0.065D, Y1 - 0.012D, Z + 0.042D);
        Solid.box(-0.03D, YS - 0.03D, Z + 0.015D, 0.03D, YS + 0.02D, Z + 0.045D);
        // A boss in the middle of the back of the lid, and the hinge's leaf.
        Solid.box(-0.16D, HINGE_Y - 0.01D, HINGE_Z - 0.012D, 0.16D, HINGE_Y + 0.05D, HINGE_Z + 0.02D);
    }

    /** Four bars round the glass between two heights, standing out from it by {@code out}. */
    private static void rim(double y0, double y1, double out) {
        Solid.box(-X - out, y0, Z - 0.01D, X + out, y1, Z + out);
        Solid.box(-X - out, y0, -Z - out, X + out, y1, -Z + 0.01D);
        Solid.box(X - 0.01D, y0, -Z, X + out, y1, Z);
        Solid.box(-X - out, y0, -Z, -X + 0.01D, y1, Z);
    }

    // ------------------------------------------------------------------ the glass

    /** The lower panes: front, back and both ends. {@code pass} 0 the glass itself, 1 the light through it. */
    private void bodyGlass(int pass) {
        panes(pass, 0, 4);
    }

    /** The lid's panes: its four sides and its top. */
    private void lidGlass(int pass) {
        panes(pass, 4, PANES.length);
    }

    private void panes(int pass, int from, int to) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        for (int p = from; p < to; p++) {
            Pane pane = PANES[p];
            for (int i = 0; i < Pane.NU; i++) {
                for (int j = 0; j < Pane.NV; j++) {
                    vertex(buffer, pass, pane, i, j);
                    vertex(buffer, pass, pane, i + 1, j);
                    vertex(buffer, pass, pane, i + 1, j + 1);
                    vertex(buffer, pass, pane, i, j + 1);
                }
            }
        }
        tessellator.draw();
    }

    private void vertex(BufferBuilder buffer, int pass, Pane pane, int i, int j) {
        int k = i * (Pane.NV + 1) + j;
        float light = pane.light[k] * pulse;
        if (pass == 0) {
            // Deep blue, paler where the light inside is strong.
            float l = Math.min(1F, light);
            buffer.pos(pane.x[k], pane.y[k], pane.z[k]).tex(pane.s[k], pane.t[k])
                    .color(DEEP[0] + 0.16F * l, DEEP[1] + 0.36F * l, DEEP[2] + 0.45F * l, 0.94F).endVertex();
        } else {
            // The light itself, through the frost; white where it is fiercest, and the glass bright at its edges.
            float hot = MathHelper.clamp(light - 0.8F, 0F, 1F);
            float sheen = pane.sheen[k];
            buffer.pos(pane.x[k], pane.y[k], pane.z[k]).tex(pane.s[k], pane.t[k])
                    .color(0.18F + 0.7F * hot + sheen, 0.5F + 0.45F * hot + sheen, 1F, Math.min(1F, light * 0.5F + sheen)).endVertex();
        }
    }

    /**
     * Brings every pane's light up to {@code t}. It is the same for every casket drawn at the same
     * moment (in a slot, in a hand, on the ground), so it is worked out once a frame, not once each.
     */
    private static void relight(float t) {
        if (t == litAt) {
            return;
        }
        litAt = t;
        for (Pane pane : PANES) {
            for (int k = 0; k < pane.x.length; k++) {
                float cloud = noise(pane.cu[k] + t * 0.012F, pane.cv[k] - t * 0.02F);
                pane.light[k] = pane.base[k] + 0.62F * cloud * cloud + 0.22F * noise(pane.su[k] - t * 0.03F + 17.3F, pane.sv[k] + t * 0.017F + 5.1F);
            }
        }
    }

    /** Smooth noise in [0, 1], tiling every 4 units, read from a table painted once. */
    private static float noise(float u, float v) {
        float fx = u * NOISE_SIZE / 4F, fy = v * NOISE_SIZE / 4F;
        int x0 = MathHelper.floor(fx), y0 = MathHelper.floor(fy);
        float ax = fx - x0, ay = fy - y0;
        int m = NOISE_SIZE - 1;
        int x1 = (x0 + 1) & m, y1 = (y0 + 1) & m;
        x0 &= m;
        y0 &= m;
        float a = NOISE[y0 * NOISE_SIZE + x0] + (NOISE[y0 * NOISE_SIZE + x1] - NOISE[y0 * NOISE_SIZE + x0]) * ax;
        float b = NOISE[y1 * NOISE_SIZE + x0] + (NOISE[y1 * NOISE_SIZE + x1] - NOISE[y1 * NOISE_SIZE + x0]) * ax;
        return a + (b - a) * ay;
    }

    /**
     * One pane of the glass, worked out once: the points of its grid, bowed outward along its
     * normal at the middle; where on its face's pattern each falls; the part of its light that
     * never changes; and where each samples the weather moving inside.
     */
    private static final class Pane {
        static final int NU = 10, NV = 6;
        final float[] x, y, z, s, t, sheen, base, cu, cv, su, sv, light;

        Pane(double[] o, double[] u, double[] v, double[] n, double bulge, float v0, float v1) {
            int count = (NU + 1) * (NV + 1);
            x = new float[count];
            y = new float[count];
            z = new float[count];
            s = new float[count];
            t = new float[count];
            sheen = new float[count];
            base = new float[count];
            cu = new float[count];
            cv = new float[count];
            su = new float[count];
            sv = new float[count];
            light = new float[count];
            for (int i = 0; i <= NU; i++) {
                for (int j = 0; j <= NV; j++) {
                    int k = i * (NV + 1) + j;
                    double a = i / (double) NU, b = j / (double) NV;
                    double swell = Math.sin(Math.PI * a) * Math.sin(Math.PI * b) * bulge;
                    double px = o[0] + u[0] * a + v[0] * b + n[0] * swell;
                    double py = o[1] + u[1] * a + v[1] * b + n[1] * swell;
                    double pz = o[2] + u[2] * a + v[2] * b + n[2] * swell;
                    x[k] = (float) px;
                    y[k] = (float) py;
                    z[k] = (float) pz;
                    s[k] = (float) a;
                    t[k] = 1F - (v0 + (v1 - v0) * (float) b);
                    float edge = (float) Math.max(Math.abs(a * 2D - 1D), Math.abs(b * 2D - 1D));
                    sheen[k] = edge * edge * edge * 0.18F;
                    float low = (float) Math.exp(-Math.pow((py - Y0 - 0.02D) / 0.2D, 2D));
                    // Two hot spots toward the foot, either side, as in a lantern lit from below.
                    float spots = (float) (Math.exp(-(Math.pow((Math.abs(px) - 0.27D) / 0.17D, 2D) + Math.pow((py - 0.1D) / 0.12D, 2D))) * 0.55D);
                    base[k] = 0.12F + 0.3F * low + spots;
                    cu[k] = (float) (px * 2.6D + pz * 1.3D);
                    cv[k] = (float) (py * 3.2D - pz * 1.7D);
                    su[k] = (float) (px * 5.1D - py * 1.1D);
                    sv[k] = (float) (pz * 5.3D + py * 2.4D);
                }
            }
        }
    }

    private static final int NOISE_SIZE = 64;
    private static final float[] NOISE = new float[NOISE_SIZE * NOISE_SIZE];
    private static float litAt = Float.NaN;

    static {
        for (int y = 0; y < NOISE_SIZE; y++) {
            for (int x = 0; x < NOISE_SIZE; x++) {
                NOISE[y * NOISE_SIZE + x] = ProcTex.fbm(x * 4F / NOISE_SIZE, y * 4F / NOISE_SIZE, 4, 3, 5) * 0.5F + 0.5F;
            }
        }
    }

    // Each face's pattern runs on up into the lid's: the body has the lower part of it.
    private static final Pane[] PANES = {
            new Pane(new double[]{-X, Y0, Z}, new double[]{2 * X, 0, 0}, new double[]{0, YS - Y0, 0}, new double[]{0, 0, 1}, 0.026D, 0F, 0.53F),
            new Pane(new double[]{X, Y0, -Z}, new double[]{-2 * X, 0, 0}, new double[]{0, YS - Y0, 0}, new double[]{0, 0, -1}, 0.026D, 0F, 0.53F),
            new Pane(new double[]{X, Y0, Z}, new double[]{0, 0, -2 * Z}, new double[]{0, YS - Y0, 0}, new double[]{1, 0, 0}, 0.034D, 0F, 0.53F),
            new Pane(new double[]{-X, Y0, -Z}, new double[]{0, 0, 2 * Z}, new double[]{0, YS - Y0, 0}, new double[]{-1, 0, 0}, 0.034D, 0F, 0.53F),
            new Pane(new double[]{-X, YS, Z}, new double[]{2 * X, 0, 0}, new double[]{0, Y1 - YS, 0}, new double[]{0, 0, 1}, 0.022D, 0.53F, 1F),
            new Pane(new double[]{X, YS, -Z}, new double[]{-2 * X, 0, 0}, new double[]{0, Y1 - YS, 0}, new double[]{0, 0, -1}, 0.022D, 0.53F, 1F),
            new Pane(new double[]{X, YS, Z}, new double[]{0, 0, -2 * Z}, new double[]{0, Y1 - YS, 0}, new double[]{1, 0, 0}, 0.03D, 0.53F, 1F),
            new Pane(new double[]{-X, YS, -Z}, new double[]{0, 0, 2 * Z}, new double[]{0, Y1 - YS, 0}, new double[]{-1, 0, 0}, 0.03D, 0.53F, 1F),
            new Pane(new double[]{-X, Y1, Z}, new double[]{2 * X, 0, 0}, new double[]{0, 0, -2 * Z}, new double[]{0, 1, 0}, 0.04D, 0F, 1F)};

    /** Opened: the inside is nothing but light. */
    private void inside(float lid) {
        float open = MathHelper.clamp(lid / 30F, 0F, 1F);
        GlStateManager.disableCull();
        GlStateManager.disableTexture2D();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(0D, YS - 0.01D, 0D).color(1F, 1F, 1F, 0.95F * open).endVertex();
        for (int i = 0; i <= 24; i++) {
            double a = Math.PI * 2D * i / 24D;
            double cx = MathHelper.clamp(Math.cos(a) * 1.5D, -1D, 1D) * X, cz = MathHelper.clamp(Math.sin(a) * 1.5D, -1D, 1D) * Z;
            buffer.pos(cx, YS - 0.01D, cz).color(0.45F, 0.8F, 1F, 0.75F * open).endVertex();
        }
        tessellator.draw();
        GlStateManager.enableTexture2D();
        GlStateManager.enableCull();
    }

    /** Light pouring out of the gap under the lid: a fan of rays up and out, flickering. */
    private void rays(float lid) {
        float open = MathHelper.clamp(lid / 30F, 0F, 1F);
        GlStateManager.disableTexture2D();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        Random rand = new Random(31L);
        float reach = (inWorld ? 1.6F : 0.55F) * (0.4F + 0.8F * charge);
        for (int i = 0; i < 14; i++) {
            double x = (rand.nextDouble() * 2D - 1D) * X * 0.92D;
            double lean = (rand.nextDouble() - 0.5D) * 0.6D;
            float flicker = 0.55F + 0.45F * MathHelper.sin(time * (0.4F + rand.nextFloat() * 0.5F) + i * 2.1F);
            double len = reach * (0.5D + rand.nextDouble() * 0.7D);
            double w = 0.025D + rand.nextDouble() * 0.05D;
            double dy = 0.8D, dz = 0.6D;
            float a = 0.3F * open * flicker;
            buffer.pos(x - w, YS, Z * 0.9D).color(0.7F, 0.9F, 1F, a).endVertex();
            buffer.pos(x + w, YS, Z * 0.9D).color(0.7F, 0.9F, 1F, a).endVertex();
            buffer.pos(x + w * 3D + lean * len, YS + dy * len, Z + dz * len).color(0.3F, 0.6F, 1F, 0F).endVertex();
            buffer.pos(x - w * 3D + lean * len, YS + dy * len, Z + dz * len).color(0.3F, 0.6F, 1F, 0F).endVertex();
        }
        tessellator.draw();
        GlStateManager.enableTexture2D();
    }

    /** The two slots in the clasp, and the light showing through them. */
    private void claspEyes() {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        float a = Math.min(1F, 0.55F + 0.35F * pulse);
        double z = Z + 0.046D;
        for (int sx = -1; sx <= 1; sx += 2) {
            double x0 = sx * 0.055D - 0.017D, x1 = sx * 0.055D + 0.017D, y0 = Y1 + 0.004D, y1 = Y1 + 0.042D;
            buffer.pos(x0, y0, z).color(0.55F, 0.85F, 1F, a).endVertex();
            buffer.pos(x1, y0, z).color(0.55F, 0.85F, 1F, a).endVertex();
            buffer.pos(x1, y1, z).color(0.3F, 0.6F, 1F, a * 0.8F).endVertex();
            buffer.pos(x0, y1, z).color(0.3F, 0.6F, 1F, a * 0.8F).endVertex();
        }
        tessellator.draw();
    }

    // ------------------------------------------------------------------ what comes off it

    /** Cold mist spilling over its rim and running down its sides, never the same twice. */
    private void mist() {
        for (int i = 0; i < MIST; i++) {
            Random seed = new Random(i * 7919L + 3L);
            float life = 60F + seed.nextFloat() * 50F;
            float clock = time + seed.nextFloat() * life;
            int cycle = (int) (clock / life);
            float t = (clock - cycle * life) / life;
            Random rand = new Random(i * 104729L + cycle * 15485863L);
            // From a point on the rim, out a little and then down past the foot.
            double side = rand.nextDouble() * 4D;
            double px, pz, nx, nz;
            if (side < 1D) {
                px = (side * 2D - 1D) * X;
                pz = Z;
                nx = 0D;
                nz = 1D;
            } else if (side < 2D) {
                px = ((side - 1D) * 2D - 1D) * X;
                pz = -Z;
                nx = 0D;
                nz = -1D;
            } else if (side < 3D) {
                px = X;
                pz = ((side - 2D) * 2D - 1D) * Z;
                nx = 1D;
                nz = 0D;
            } else {
                px = -X;
                pz = ((side - 3D) * 2D - 1D) * Z;
                nx = -1D;
                nz = 0D;
            }
            double out = 0.05D + 0.25D * Math.sqrt(t);
            double y = Y1 - (Y1 + 0.25D) * t * t + 0.03D;
            float fade = MathHelper.sin((float) Math.PI * t);
            float size = 0.18F + 0.4F * t;
            billboard((float) (px + nx * out), (float) y, (float) (pz + nz * out), size, rand.nextFloat() * 6.28F + t, 0.75F, 0.88F, 1F,
                    0.12F * fade * (1F + charge));
        }
    }

    /** Frost glittering off it: points of light coming loose and drifting away, winking. */
    private void glints() {
        for (int i = 0; i < GLINTS; i++) {
            Random seed = new Random(i * 6151L + 11L);
            float life = 40F + seed.nextFloat() * 50F;
            float clock = time + seed.nextFloat() * life;
            int cycle = (int) (clock / life);
            float t = (clock - cycle * life) / life;
            Random rand = new Random(i * 31337L + cycle * 7727L);
            double a = rand.nextDouble() * Math.PI * 2D;
            double r = 0.45D + 0.35D * t;
            float x = (float) (Math.cos(a) * r * 1.2D), z = (float) (Math.sin(a) * r * 0.8D);
            float y = (float) (0.1D + rand.nextDouble() * 0.5D + 0.15D * t);
            float wink = Math.max(0F, MathHelper.sin(t * (float) Math.PI * 3F + i));
            billboard(x, y, z, 0.05F + 0.04F * wink, 0F, 0.85F, 0.95F, 1F, 0.8F * wink * MathHelper.sin((float) Math.PI * t));
        }
    }

    // ------------------------------------------------------------------ sprites and the view

    private void billboard(float x, float y, float z, float size, float angle, float r, float g, float b, float alpha) {
        if (alpha <= 0.003F) {
            return;
        }
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

    private void cullNear(boolean near) {
        GlStateManager.cullFace(near != mirrored ? GlStateManager.CullFace.FRONT : GlStateManager.CullFace.BACK);
    }

    private void readView() {
        GlStateManager.getFloat(GL11.GL_MODELVIEW_MATRIX, MODELVIEW);
        GlStateManager.getFloat(GL11.GL_PROJECTION_MATRIX, PROJECTION);
        inWorld = PROJECTION.get(11) != 0F;
        float[] right = unit(MODELVIEW.get(0), MODELVIEW.get(4), MODELVIEW.get(8));
        float[] up = unit(MODELVIEW.get(1), MODELVIEW.get(5), MODELVIEW.get(9));
        float ax = MODELVIEW.get(0), ay = MODELVIEW.get(1), az = MODELVIEW.get(2);
        float bx = MODELVIEW.get(4), by = MODELVIEW.get(5), bz = MODELVIEW.get(6);
        float cx = MODELVIEW.get(8), cy = MODELVIEW.get(9), cz = MODELVIEW.get(10);
        mirrored = (ay * bz - az * by) * cx + (az * bx - ax * bz) * cy + (ax * by - ay * bx) * cz < 0F;
        rx = right[0];
        ry = right[1];
        rz = right[2];
        ux = up[0];
        uy = up[1];
        uz = up[2];
    }

    private static float[] unit(float x, float y, float z) {
        float len = MathHelper.sqrt(x * x + y * y + z * z);
        return len < 1.0E-6F ? new float[]{0F, 1F, 0F} : new float[]{x / len, y / len, z / len};
    }

    // ------------------------------------------------------------------ textures

    private static void ensureTextures() {
        if (glass != null) {
            return;
        }
        glass = ProcTex.texture(256, CasketRenderer::glassPixel);
        iron = ProcTex.texture(64, (u, v) -> {
            // Old black iron: mottled, pitted, a little bright where it has been worn smooth.
            float f = ProcTex.fbm(u * 4F, v * 4F, 4, 4, 41) * 0.5F + 0.5F;
            float[] c = ProcTex.cells(u * 9F, v * 9F, 9, 43);
            float pit = c[0] < 0.12F ? 0.55F : 1F;
            float worn = ProcTex.smooth((f - 0.62F) / 0.2F) * 0.35F;
            float g = (0.62F + 0.32F * f + worn) * pit;
            return ProcTex.argb(1F, g, g, g * 1.04F);
        });
    }

    /**
     * The pattern leaded through the glass, one face of it, {@code v} 0 at the top: a seam down
     * the middle; three onion-domed shapes one inside another, rising from low in the middle; a web
     * of spokes and threads round them; great arcs sweeping in from the corners; fine cracks
     * between; and the frost itself, mottled and glittering like sugar.
     */
    private static int glassPixel(float u, float v) {
        float x = (u - 0.5F) * 1.8F, y = 1F - v;
        float ax = Math.abs(x);
        float line = 0F;
        line = Math.max(line, stroke(ax, 0.007F));
        // The domes.
        float qy = y - 0.36F;
        float r = MathHelper.sqrt(ax * ax + qy * qy);
        float theta = (float) Math.atan2(ax, qy);
        float[] sizes = {0.11F, 0.21F, 0.33F};
        for (float s : sizes) {
            float rim = s * (0.82F + 0.62F * (float) Math.exp(-theta * theta / 0.05F) - 0.16F * MathHelper.cos(theta));
            line = Math.max(line, stroke(Math.abs(r - rim), 0.0075F));
        }
        // Spokes out from the domes, and the web strung between them.
        if (r > 0.3F) {
            float[] spokes = {0.62F, 1.15F, 1.7F, 2.3F, 2.85F};
            for (float a : spokes) {
                float d = theta - a;
                if (MathHelper.cos(d) > 0F) {
                    line = Math.max(line, stroke(r * Math.abs(MathHelper.sin(d)), 0.006F));
                }
            }
            float[] rings = {0.5F, 0.7F, 0.92F};
            for (float ring : rings) {
                float rr = ring * (1F + 0.07F * MathHelper.cos(theta * 7F));
                line = Math.max(line, stroke(Math.abs(r - rr), 0.005F) * 0.9F);
            }
        }
        // Sweeps in from the corners.
        float[][] arcs = {{1.02F, 1.08F, 0.52F}, {1.02F, 1.08F, 0.72F}, {1.05F, -0.12F, 0.46F}, {1.05F, -0.12F, 0.66F}};
        for (float[] a : arcs) {
            float dx = ax - a[0], dy = y - a[1];
            line = Math.max(line, stroke(Math.abs(MathHelper.sqrt(dx * dx + dy * dy) - a[2]), 0.006F));
        }
        // Fine cracks between it all.
        float[] c = ProcTex.cells(ax * 4F, y * 3.5F, 64, 17);
        line = Math.max(line, stroke(c[1] - c[0], 0.02F) * 0.35F);
        // And the lead framing the pane.
        float border = Math.min(Math.min(u, 1F - u), Math.min(v, 1F - v));
        line = Math.max(line, stroke(border, 0.014F));

        float mottle = ProcTex.fbm(u * 6F, v * 6F, 6, 4, 3) * 0.5F + 0.5F;
        float grain = ProcTex.hash((int) (u * 256F), (int) (v * 256F), 77);
        float sugar = grain > 0.9F ? 0.3F : grain > 0.75F ? 0.12F : 0F;
        float frost = MathHelper.clamp(0.5F + 0.38F * mottle + sugar, 0F, 1F);
        float bright = frost * (1F - line);
        return ProcTex.argb(0.86F + 0.14F * line, bright, bright, bright);
    }

    /** How much of a line {@code width} wide covers a point {@code d} from its middle, softened over a pixel. */
    private static float stroke(float d, float width) {
        return MathHelper.clamp(1F - (d - width) / 0.006F, 0F, 1F);
    }
}
