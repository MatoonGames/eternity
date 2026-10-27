package com.matoon.eternity.client.render;

import com.matoon.eternity.Eternity;
import com.matoon.eternity.entity.EntityDestroyer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * The Destroyer as everyone sees it: dark banded metal, and the fire behind its face. Dormant there
 * is none. Awake, it shows through the slits of its face plates; when the plates swing open for
 * the ray, the furnace behind them is too bright to look at.
 */
@SideOnly(Side.CLIENT)
public class RenderDestroyer extends RenderLiving<EntityDestroyer> {

    private static final ResourceLocation SKIN = new ResourceLocation(Eternity.MODID, "textures/entity/destroyer.png");
    private static final ResourceLocation GLOW = new ResourceLocation(Eternity.MODID, "textures/entity/destroyer_glow.png");

    private float partial;

    public RenderDestroyer(RenderManager manager) {
        super(manager, new ModelDestroyer(), 1.1F);
        addLayer(new Furnace());
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityDestroyer entity) {
        return SKIN;
    }

    private ModelDestroyer model() {
        return (ModelDestroyer) mainModel;
    }

    @Override
    public void doRender(EntityDestroyer entity, double x, double y, double z, float entityYaw, float partialTicks) {
        partial = partialTicks;
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    @Override
    protected void preRenderCallback(EntityDestroyer entity, float partialTickTime) {
        GlStateManager.scale(EntityDestroyer.SCALE, EntityDestroyer.SCALE, EntityDestroyer.SCALE);
    }

    @Override
    protected void applyRotations(EntityDestroyer entity, float ageInTicks, float rotationYaw, float partialTicks) {
        GlStateManager.rotate(180F - rotationYaw, 0F, 1F, 0F);
    }

    /** Over the dark metal: the fire behind its face, through its slits or, open, all of it. */
    @Override
    protected void renderModel(EntityDestroyer entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
                               float headPitch, float scaleFactor) {
        super.renderModel(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor);
        float heat = entity.prevHeat + (entity.heat - entity.prevHeat) * partial;
        if (heat < 0.01F || entity.isInvisible()) {
            return;
        }
        GlSnapshot before = GlSnapshot.take();
        GlStateManager.enableBlend();
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.depthFunc(GL11.GL_LEQUAL);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
        bindTexture(GLOW);
        float flicker = 0.9F + 0.1F * MathHelper.sin(ageInTicks * 1.7F) * MathHelper.sin(ageInTicks * 0.63F);
        GlStateManager.color(1F, 1F, 1F, Math.min(1F, heat * flicker));
        model().renderPose(scaleFactor);
        GlStateManager.depthMask(true);
        GlStateManager.depthFunc(GL11.GL_LEQUAL);
        before.restore();
    }

    /** The light of the furnace standing out in front of its face. */
    private class Furnace implements LayerRenderer<EntityDestroyer> {
        @Override
        public void doRenderLayer(EntityDestroyer entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
                                  float netHeadYaw, float headPitch, float scale) {
            float heat = entity.prevHeat + (entity.heat - entity.prevHeat) * partialTicks;
            float open = entity.prevOpen + (entity.open - entity.prevOpen) * partialTicks;
            if (heat < 0.05F) {
                return;
            }
            GlSnapshot before = GlSnapshot.take();
            GlStateManager.disableTexture2D();
            GlStateManager.disableLighting();
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            GlStateManager.depthMask(false);
            GlStateManager.disableCull();
            GlStateManager.shadeModel(GL11.GL_SMOOTH);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
            GlStateManager.pushMatrix();
            model().toFace(scale);
            float flicker = 0.85F + 0.15F * MathHelper.sin(ageInTicks * 2.3F);
            float bright = heat * (0.25F + 0.75F * open);
            disc(0.12F + 0.45F * bright * flicker, VaultFx.ORANGE, 0.35F * heat);
            disc(0.08F + 0.25F * bright * flicker, VaultFx.YELLOW, 0.5F * bright);
            disc(0.05F + 0.12F * bright, VaultFx.WHITE, 0.8F * bright);
            GlStateManager.popMatrix();
            before.restore();
        }

        /** A soft disc of light facing out of its face, and two across it, so it glows from the side as well. */
        private void disc(float radius, float[] col, float alpha) {
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            for (int axis = 0; axis < 3; axis++) {
                buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
                buffer.pos(0D, 0D, 0D).color(col[0], col[1], col[2], alpha).endVertex();
                for (int i = 0; i <= 18; i++) {
                    double a = Math.PI * 2D * i / 18D;
                    double u = Math.cos(a) * radius, v = Math.sin(a) * radius;
                    if (axis == 0) {
                        buffer.pos(u, v, 0D).color(col[0], col[1], col[2], 0F).endVertex();
                    } else if (axis == 1) {
                        buffer.pos(u, 0D, v).color(col[0], col[1], col[2], 0F).endVertex();
                    } else {
                        buffer.pos(0D, u, v).color(col[0], col[1], col[2], 0F).endVertex();
                    }
                }
                tessellator.draw();
            }
        }

        @Override
        public boolean shouldCombineTextures() {
            return false;
        }
    }
}
