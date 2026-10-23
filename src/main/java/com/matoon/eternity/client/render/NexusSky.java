package com.matoon.eternity.client.render;

import com.matoon.eternity.client.NexusClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.IRenderHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * The sky of the Nexus of All Realities: a neon dome whose colours drift over minutes,
 * starlight over it, nebula arms slowly wheeling at the zenith, a vast geodesic moon,
 * little banded planets (one ringed), and great orange tubes looping through the heavens.
 *
 * Everything that does not move is recorded once into display lists; the dome is
 * re-recorded every twenty seconds to follow its drift. Nothing here moves fast.
 */
@SideOnly(Side.CLIENT)
public class NexusSky extends IRenderHandler {

    public static final NexusSky INSTANCE = new NexusSky();

    private static final int DOME_REFRESH = 400;
    private static final float[][] BANDS = {{1F, 0.45F, 0.75F}, {0.85F, 0.25F, 0.6F}, {1F, 0.65F, 0.8F}};

    private int domeList = -1;
    private float domeBuilt;
    private int moonList = -1;
    private int planetList = -1;
    private int ringList = -1;
    private int loopsList = -1;

    @Override
    public void render(float partialTicks, WorldClient world, Minecraft mc) {
        float time = world.getTotalWorldTime() + partialTicks;
        build(time);
        GlStateManager.pushMatrix();
        GlStateManager.disableFog();
        GlStateManager.disableAlpha();
        GlStateManager.disableCull();
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);

        GlStateManager.callList(domeList);
        Cosmic.draw(() -> FxRenderer.skySphere(95D), 6, 0F, 1.3F, 1F, 0.85F, 1F, false);
        // Cosmic leaves depth writing on; the sky must never write depth, or it would hide the land.
        GlStateManager.depthMask(false);
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();

        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        nebula(time);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.callList(loopsList);
        for (int i = 0; i < 4; i++) {
            double a = i * 1.7D + time * 0.0002D * (i + 1);
            float radius = 4F + i * 1.5F;
            GlStateManager.pushMatrix();
            GlStateManager.translate(Math.cos(a) * 75D, -20D + i * 14D, Math.sin(a) * 75D);
            GlStateManager.scale(radius, radius, radius);
            NexusRenderer.outsideOnly(() -> GlStateManager.callList(planetList));
            if (i == 2) {
                GlStateManager.callList(ringList);
            }
            GlStateManager.popMatrix();
        }
        NexusRenderer.outsideOnly(() -> GlStateManager.callList(moonList));

        // Toward the junction the sky's colours go out, one by one, until it is a flat grey dome.
        float neutral = NexusClient.INSTANCE.neutrality();
        if (neutral > 0.01F) {
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            GlStateManager.color(0.5F, 0.5F, 0.52F, 0.95F * neutral);
            FxRenderer.skySphere(90D);
            GlStateManager.color(1F, 1F, 1F, 1F);
        }

        GlStateManager.depthMask(true);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.enableCull();
        GlStateManager.enableAlpha();
        GlStateManager.popMatrix();
    }

    private void build(float time) {
        if (domeList < 0 || Math.abs(time - domeBuilt) > DOME_REFRESH) {
            if (domeList >= 0) {
                GLAllocation.deleteDisplayLists(domeList);
            }
            domeList = NexusRenderer.compile(() -> dome(time));
            domeBuilt = time;
        }
        if (moonList < 0) {
            moonList = NexusRenderer.compile(NexusSky::geodesicMoon);
            planetList = NexusRenderer.compile(() -> NexusRenderer.sphereGeometry(Vec3d.ZERO, 1D, 14, 10,
                    n -> BANDS[Math.floorMod((int) Math.floor((n.y + 1D) * 4D), 3)]));
            ringList = NexusRenderer.compile(() -> ring(1.6D, 2.3D));
            loopsList = NexusRenderer.compile(() -> {
                loop(new Vec3d(-55D, 25D, 50D), 26D, 16D, 0.3D, 0.6D, 2.6D);
                loop(new Vec3d(40D, -10D, 70D), 20D, 30D, -0.9D, -0.4D, 2.2D);
            });
        }
    }

    /** Violet overhead, hot magenta at the horizon, the dark of the abyss below, the hues sliding over minutes. */
    private static void dome(float time) {
        float drift = (float) Math.sin(time * 0.0003D) * 0.05F;
        float[] top = NexusRenderer.hue(0.78F + drift, 0.85F, 0.3F);
        float[] horizon = NexusRenderer.hue(0.9F + drift, 0.75F, 0.85F);
        float[] bottom = NexusRenderer.hue(0.9F + drift, 0.8F, 0.05F);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        int stacks = 16, slices = 24;
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < stacks; i++) {
            double t0 = -Math.PI / 2D + Math.PI * i / stacks;
            double t1 = -Math.PI / 2D + Math.PI * (i + 1) / stacks;
            float[] c0 = blend(Math.sin(t0), top, horizon, bottom);
            float[] c1 = blend(Math.sin(t1), top, horizon, bottom);
            for (int j = 0; j < slices; j++) {
                double p0 = Math.PI * 2D * j / slices, p1 = Math.PI * 2D * (j + 1) / slices;
                vertex(buffer, t0, p0, 100D, c0);
                vertex(buffer, t0, p1, 100D, c0);
                vertex(buffer, t1, p1, 100D, c1);
                vertex(buffer, t1, p0, 100D, c1);
            }
        }
        tessellator.draw();
    }

    private static float[] blend(double height, float[] top, float[] horizon, float[] bottom) {
        float[] far = height > 0D ? top : bottom;
        float k = (float) Math.min(1D, Math.abs(height) * 1.6D);
        return new float[]{horizon[0] + (far[0] - horizon[0]) * k, horizon[1] + (far[1] - horizon[1]) * k, horizon[2] + (far[2] - horizon[2]) * k};
    }

    private static void vertex(BufferBuilder buffer, double theta, double phi, double r, float[] c) {
        double cos = Math.cos(theta);
        buffer.pos(cos * Math.cos(phi) * r, Math.sin(theta) * r, cos * Math.sin(phi) * r).color(c[0], c[1], c[2], 1F).endVertex();
    }

    /** Spiral arms of soft coloured light turning slowly around the zenith. */
    private static void nebula(float time) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        double spin = time * 0.0006D;
        for (int arm = 0; arm < 4; arm++) {
            float[] colour = NexusRenderer.hue(arm * 0.23F + time * 0.0001F, 0.7F, 1F);
            buffer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
            for (int i = 0; i <= 40; i++) {
                double s = i / 40D;
                double a = arm * Math.PI / 2D + s * Math.PI * 3D + spin;
                double r = 4D + s * 70D;
                double w = 2D + s * 12D;
                float alpha = 0.3F * (float) Math.sin(s * Math.PI);
                buffer.pos(Math.cos(a) * (r - w), 80D - s * 30D, Math.sin(a) * (r - w)).color(colour[0], colour[1], colour[2], 0F).endVertex();
                buffer.pos(Math.cos(a) * r, 80D - s * 30D, Math.sin(a) * r).color(colour[0], colour[1], colour[2], alpha).endVertex();
            }
            tessellator.draw();
        }
    }

    /** A thick orange tube bent into a rounded rectangle, with a pale stripe along it. */
    private static void loop(Vec3d centre, double a, double b, double spin, double tilt, double width) {
        Vec3d u = new Vec3d(Math.cos(spin), Math.sin(tilt) * Math.sin(spin), Math.sin(spin));
        Vec3d v = new Vec3d(0D, Math.cos(tilt), Math.sin(tilt) * 0.5D).normalize();
        Vec3d lift = v.crossProduct(u).scale(0.3D);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        for (int pass = 0; pass < 2; pass++) {
            double w = pass == 0 ? width : width * 0.3D;
            float r = 1F, g = pass == 0 ? 0.55F : 0.85F, bl = pass == 0 ? 0.2F : 0.45F;
            buffer.begin(GL11.GL_QUAD_STRIP, DefaultVertexFormats.POSITION_COLOR);
            for (int i = 0; i <= 80; i++) {
                double t = Math.PI * 2D * i / 80D;
                double cos = Math.cos(t), sin = Math.sin(t);
                double x = Math.signum(cos) * Math.pow(Math.abs(cos), 0.4D) * a;
                double y = Math.signum(sin) * Math.pow(Math.abs(sin), 0.4D) * b;
                Vec3d p = centre.add(u.scale(x)).add(v.scale(y));
                Vec3d out = u.scale(x).add(v.scale(y)).normalize();
                Vec3d offset = pass == 1 ? lift : Vec3d.ZERO;
                Vec3d inner = p.subtract(out.scale(w * 0.5D)).add(offset);
                Vec3d outer = p.add(out.scale(w * 0.5D)).add(offset);
                buffer.pos(inner.x, inner.y, inner.z).color(r, g, bl, 1F).endVertex();
                buffer.pos(outer.x, outer.y, outer.z).color(r, g, bl, 1F).endVertex();
            }
            tessellator.draw();
        }
    }

    private static void ring(double inner, double outer) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUAD_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i <= 48; i++) {
            double a = Math.PI * 2D * i / 48D;
            double cos = Math.cos(a), sin = Math.sin(a);
            buffer.pos(cos * inner, sin * inner * 0.3D, sin * inner).color(1F, 0.8F, 0.4F, 0.8F).endVertex();
            buffer.pos(cos * outer, sin * outer * 0.3D, sin * outer).color(1F, 0.6F, 0.3F, 0.5F).endVertex();
        }
        tessellator.draw();
    }

    /** A vast geodesic sphere: pale lavender panels on a dark frame, dark nodes at every joint. */
    private static void geodesicMoon() {
        Vec3d c = new Vec3d(55D, 45D, -55D);
        double r = 24D;
        NexusRenderer.sphereGeometry(c, r, 14, 8, n -> new float[]{0.78F, 0.72F, 0.95F});
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        double rr = r * 1.01D;
        for (int i = 1; i < 8; i++) {
            double t = -Math.PI / 2D + Math.PI * i / 8D;
            for (int j = 0; j < 14; j++) {
                line(buffer, c, rr, t, Math.PI * 2D * j / 14D, t, Math.PI * 2D * (j + 1) / 14D);
            }
        }
        for (int j = 0; j < 14; j++) {
            double p = Math.PI * 2D * j / 14D;
            for (int i = 0; i < 8; i++) {
                line(buffer, c, rr, -Math.PI / 2D + Math.PI * i / 8D, p, -Math.PI / 2D + Math.PI * (i + 1) / 8D, p);
            }
        }
        tessellator.draw();
        for (int i = 1; i < 8; i++) {
            double t = -Math.PI / 2D + Math.PI * i / 8D;
            for (int j = 0; j < 14; j++) {
                double p = Math.PI * 2D * j / 14D;
                double cos = Math.cos(t);
                Vec3d node = c.add(cos * Math.cos(p) * rr, Math.sin(t) * rr, cos * Math.sin(p) * rr);
                NexusRenderer.sphereGeometry(node, 0.7D, 6, 4, n -> new float[]{0.2F, 0.12F, 0.35F});
            }
        }
    }

    private static void line(BufferBuilder buffer, Vec3d c, double r, double t0, double p0, double t1, double p1) {
        double c0 = Math.cos(t0), c1 = Math.cos(t1);
        buffer.pos(c.x + c0 * Math.cos(p0) * r, c.y + Math.sin(t0) * r, c.z + c0 * Math.sin(p0) * r).color(0.3F, 0.2F, 0.45F, 1F).endVertex();
        buffer.pos(c.x + c1 * Math.cos(p1) * r, c.y + Math.sin(t1) * r, c.z + c1 * Math.sin(p1) * r).color(0.3F, 0.2F, 0.45F, 1F).endVertex();
    }
}
