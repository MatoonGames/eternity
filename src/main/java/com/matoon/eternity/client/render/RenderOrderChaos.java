package com.matoon.eternity.client.render;

import com.matoon.eternity.Eternity;
import com.matoon.eternity.entity.EntityOrderChaos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;

/**
 * Master Order and Lord Chaos as the comics draw them: vast disembodied heads, each
 * wreathed in a ragged halo of fire speckled with bubbles, gold around Order and pale blue
 * around Chaos. Chaos has no eyes and no mouth, only hollows that open onto deep space.
 * Arriving, they swell out of nothing; withdrawing, they rise and dwindle away.
 *
 * {@link #draw} is also used on its own, for the two heads rising at the edges of the sky
 * while they are on their way.
 */
@SideOnly(Side.CLIENT)
public class RenderOrderChaos extends Render<EntityOrderChaos> {

    private static final ResourceLocation ORDER = new ResourceLocation(Eternity.MODID, "textures/entity/master_order.png");
    private static final ResourceLocation CHAOS = new ResourceLocation(Eternity.MODID, "textures/entity/lord_chaos.png");
    private static final ModelOrderChaos MODEL = new ModelOrderChaos();

    public RenderOrderChaos(RenderManager manager) {
        super(manager);
        shadowSize = 0F;
    }

    @Override
    public void doRender(EntityOrderChaos entity, double x, double y, double z, float entityYaw, float partialTicks) {
        float ticks = entity.clientStateTicks + partialTicks;
        float grow = 1F;
        if (entity.getState() == EntityOrderChaos.State.ARRIVE) {
            grow = Math.min(1F, ticks / 30F);
        } else if (entity.getState() == EntityOrderChaos.State.WITHDRAW) {
            grow = 1F - Math.min(1F, ticks / EntityOrderChaos.WITHDRAW_TICKS);
        } else if (entity.getState() == EntityOrderChaos.State.ABSORBING) {
            float p = Math.min(1F, ticks / EntityOrderChaos.ABSORB_TICKS);
            grow = 1F - p * p;
        }
        // Beaten, he sags forward, his fire guttering.
        float tilt = entity.isDown() || entity.getState() == EntityOrderChaos.State.ABSORBING ? Math.min(1F, ticks / 20F) * 28F : 0F;
        float yaw = entity.prevRotationYaw + MathHelper.wrapDegrees(entity.rotationYaw - entity.prevRotationYaw) * partialTicks;
        draw(entity.isOrder(), x, y + EntityOrderChaos.SIZE / 2D, z, yaw, EntityOrderChaos.SCALE * grow,
                entity.ticksExisted + partialTicks, entity.isEnraged(), renderManager.playerViewY, renderManager.playerViewX,
                renderManager.options.thirdPersonView == 2, tilt, tilt > 0F ? 0.35F : 1F);
    }

    /**
     * Draws one head, its middle at (x, y, z) relative to the camera.
     *
     * @param scale blocks per model pixel
     */
    public static void draw(boolean order, double x, double y, double z, float yaw, float scale, float time, boolean enraged,
                            float viewYaw, float viewPitch, boolean frontView) {
        draw(order, x, y, z, yaw, scale, time, enraged, viewYaw, viewPitch, frontView, 0F, 1F);
    }

    /**
     * Draws one head, sagging forward by tilt degrees, its halo burning at fire of its strength.
     */
    public static void draw(boolean order, double x, double y, double z, float yaw, float scale, float time, boolean enraged,
                            float viewYaw, float viewPitch, boolean frontView, float tilt, float fire) {
        if (scale <= 0.001F) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.disableLighting();
        GlStateManager.disableFog();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);

        // The halo of fire behind him, always turned to face whoever is looking.
        GlStateManager.pushMatrix();
        GlStateManager.rotate(180F - viewYaw, 0F, 1F, 0F);
        GlStateManager.rotate((frontView ? -1F : 1F) * -viewPitch, 1F, 0F, 0F);
        aura(order, scale * 9F * (0.6F + 0.4F * fire), time, enraged);
        GlStateManager.popMatrix();

        GlStateManager.rotate(-yaw, 0F, 1F, 0F);
        GlStateManager.rotate(tilt, 1F, 0F, 0F);
        GlStateManager.scale(scale, -scale, -scale);
        GlStateManager.enableTexture2D();
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.color(1F, 1F, 1F, 1F);
        mc.getTextureManager().bindTexture(order ? ORDER : CHAOS);
        MODEL.draw(order);
        if (!order) {
            // Where his eyes and mouth should be, there is only deep space.
            Cosmic.draw(RenderOrderChaos::hollows, 8, 1F, 1.6F, 0.85F, 0.75F, 1F, true);
        }
        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
        GlStateManager.enableFog();
        GlStateManager.popMatrix();
    }

    /** Just the head, in model space, for the inside of a Regulator's tooltip. */
    public static void drawModel(boolean order) {
        GlStateManager.enableTexture2D();
        GlStateManager.color(1F, 1F, 1F, 1F);
        Minecraft.getMinecraft().getTextureManager().bindTexture(order ? ORDER : CHAOS);
        MODEL.draw(order);
        if (!order) {
            Cosmic.draw(RenderOrderChaos::hollows, 6, 1F, 1.6F, 0.85F, 0.75F, 1F, true);
            GlStateManager.enableTexture2D();
        }
    }

    /** Chaos's eye sockets and maw, just in front of his face, ragged at the edges. */
    private static void hollows() {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        double z = -6.03D;
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        quad(buffer, -5D, -2.6D, -1.2D, 1.4D, z);
        quad(buffer, 1.2D, -2.6D, 5D, 1.4D, z);
        // The maw: columns of different depths, like broken teeth.
        double[] depth = {4.2D, 5.4D, 5.8D, 4.8D, 5.9D, 5.6D, 5.2D, 4.4D};
        for (int i = 0; i < depth.length; i++) {
            double x0 = -4D + i;
            quad(buffer, x0, 2D, x0 + 1D, 2D + depth[i] * 0.62D, z);
        }
        tessellator.draw();
    }

    private static void quad(BufferBuilder buffer, double x0, double y0, double x1, double y1, double z) {
        buffer.pos(x0, y0, z).endVertex();
        buffer.pos(x0, y1, z).endVertex();
        buffer.pos(x1, y1, z).endVertex();
        buffer.pos(x1, y0, z).endVertex();
    }

    /** A ragged ring of fire, brightest at his silhouette, with bubbles drifting in it, like the comics' halos. */
    private static void aura(boolean order, float radius, float time, boolean enraged) {
        float r = order ? 1F : 0.55F, g = order ? 0.82F : 0.85F, b = order ? 0.2F : 1F;
        if (enraged) {
            r = Math.min(1F, r + 0.25F);
            g *= 0.75F;
        }
        GlStateManager.disableTexture2D();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        GlStateManager.depthMask(false);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        int segments = 64;
        float inner = radius * 0.72F;
        float flare = enraged ? 1.5F : 1.25F;
        buffer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i <= segments; i++) {
            double a = Math.PI * 2D * i / segments;
            double ragged = 1D + 0.12D * Math.sin(a * 9D + time * 0.21D) + 0.08D * Math.sin(a * 17D - time * 0.33D)
                    + 0.05D * Math.sin(a * 31D + time * 0.5D);
            double outer = radius * flare * ragged;
            buffer.pos(Math.cos(a) * inner, Math.sin(a) * inner, 0D).color(r, g, b, 0.95F).endVertex();
            buffer.pos(Math.cos(a) * outer, Math.sin(a) * outer, 0D).color(r, g * 0.8F, b, 0F).endVertex();
        }
        tessellator.draw();
        buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(0D, 0D, 0D).color(r, g, b, 0.5F).endVertex();
        for (int i = 0; i <= segments; i++) {
            double a = Math.PI * 2D * i / segments;
            buffer.pos(Math.cos(a) * inner, Math.sin(a) * inner, 0D).color(r, g, b, 0.95F).endVertex();
        }
        tessellator.draw();
        for (int i = 0; i < 14; i++) {
            double a = i * 2.39D + Math.sin(time * 0.02D + i) * 0.3D;
            double d = radius * (1.02D + 0.2D * Math.sin(time * 0.05D + i * 1.7D));
            double size = radius * (0.035D + 0.03D * ((i * 7) % 5) / 4D);
            bubble(buffer, Math.cos(a) * d, Math.sin(a) * d, size, r, g, b);
        }
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.depthMask(true);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
    }

    private static void bubble(BufferBuilder buffer, double x, double y, double size, float r, float g, float b) {
        Tessellator tessellator = Tessellator.getInstance();
        buffer.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < 10; i++) {
            double a = Math.PI * 2D * i / 10D;
            buffer.pos(x + Math.cos(a) * size, y + Math.sin(a) * size, 0D).color(1F, 1F, 1F, 0.9F).endVertex();
        }
        tessellator.draw();
        buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(x, y, 0D).color(r, g, b, 0.15F).endVertex();
        for (int i = 0; i <= 10; i++) {
            double a = Math.PI * 2D * i / 10D;
            buffer.pos(x + Math.cos(a) * size, y + Math.sin(a) * size, 0D).color(r, g, b, 0.5F).endVertex();
        }
        tessellator.draw();
    }

    @Override
    public boolean shouldRender(EntityOrderChaos entity, ICamera camera, double x, double y, double z) {
        return true;
    }

    @Nullable
    @Override
    protected ResourceLocation getEntityTexture(EntityOrderChaos entity) {
        return null;
    }
}
