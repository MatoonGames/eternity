package com.matoon.eternity.client.render;

import com.matoon.eternity.Eternity;
import com.matoon.eternity.entity.EntityOdin;
import com.matoon.eternity.entity.EntityOdin.Pose;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * Odin on Sleipnir. Over his skin, what burns from within (his eye, Gungnir's runes and the edge of its blade) at full
 * brightness; when he calls on the Odinforce, shells of gold light standing out round him and his mount, brightest at
 * the moment it goes into the ground; a glint running down Gungnir now and then. Out of the world (a portrait) he is
 * drawn plain.
 */
@SideOnly(Side.CLIENT)
public class RenderOdin extends RenderLiving<EntityOdin> {

    private static final ResourceLocation SKIN = new ResourceLocation(Eternity.MODID, "textures/entity/odin.png");
    private static final ResourceLocation GLOW = new ResourceLocation(Eternity.MODID, "textures/entity/odin_glow.png");
    private static final float[] GOLD = {1F, 0.82F, 0.4F};
    private static final float[] PALE = {1F, 0.96F, 0.8F};

    private float partial;

    public RenderOdin(RenderManager manager) {
        super(manager, new ModelOdin(), 1.3F);
        addLayer(new Carried());
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityOdin entity) {
        return SKIN;
    }

    private ModelOdin model() {
        return (ModelOdin) mainModel;
    }

    @Override
    protected void preRenderCallback(EntityOdin entity, float partialTicks) {
        GlStateManager.scale(EntityOdin.SCALE, EntityOdin.SCALE, EntityOdin.SCALE);
    }

    @Override
    protected void applyRotations(EntityOdin entity, float ageInTicks, float rotationYaw, float partialTicks) {
        GlStateManager.rotate(180F - rotationYaw, 0F, 1F, 0F);
    }

    @Override
    public void doRender(EntityOdin entity, double x, double y, double z, float entityYaw, float partialTicks) {
        partial = partialTicks;
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    @Override
    protected boolean canRenderName(EntityOdin entity) {
        return false;
    }

    /** How much of the Odinforce is in him now, 0 to a little over 1. */
    private static float power(EntityOdin entity, float pt) {
        float t = entity.clientPoseTicks + pt;
        switch (entity.pose()) {
            case CALL:
                return 0.35F + 0.65F * MathHelper.clamp(t / 26F, 0F, 1F);
            case SLAM:
                return t < EntityOdin.SLAM_HIT ? 1F : 1F + 0.4F * (float) Math.exp(-(t - EntityOdin.SLAM_HIT) * 0.25F);
            case DESCEND:
            case REAR:
                return 0.45F;
            case CHARGE:
                return 0.35F;
            default:
                return 0.12F;
        }
    }

    @Override
    protected void renderModel(EntityOdin entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
                               float headPitch, float scaleFactor) {
        super.renderModel(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor);
        if (entity.isInvisible()) {
            return;
        }
        GlSnapshot before = GlSnapshot.take();
        GlStateManager.enableBlend();
        GlStateManager.depthFunc(GL11.GL_LEQUAL);
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
        bindTexture(GLOW);
        float power = power(entity, partial);
        GlStateManager.color(1F, 1F, 1F, Math.min(1F, 0.75F + 0.4F * power));
        model().renderPose(scaleFactor);
        if (power > 0.3F) {
            // Shells of gold standing out round him, the Odinforce in him.
            bindTexture(SKIN);
            GlStateManager.enableCull();
            GlStateManager.cullFace(GlStateManager.CullFace.FRONT);
            float aura = (power - 0.3F) / 0.9F;
            float[] alphas = {0.32F, 0.16F};
            float[][] colours = {PALE, GOLD};
            for (int i = 0; i < alphas.length; i++) {
                float s = 1.035F + i * 0.05F + 0.01F * MathHelper.sin(ageInTicks * 0.6F + i * 2F);
                GlStateManager.pushMatrix();
                GlStateManager.translate(0F, 0.4F, 0F);
                GlStateManager.scale(s, s, s);
                GlStateManager.translate(0F, -0.4F, 0F);
                GlStateManager.color(colours[i][0], colours[i][1], colours[i][2], alphas[i] * Math.min(1.2F, aura));
                model().renderPose(scaleFactor);
                GlStateManager.popMatrix();
            }
        }
        before.restore();
    }

    /** The gauntlet, when he has it: in the crook of his left arm, held against him. */
    private class Carried implements LayerRenderer<EntityOdin> {
        @Override
        public void doRenderLayer(EntityOdin entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
                                  float netHeadYaw, float headPitch, float scale) {
            ItemStack gauntlet = entity.gauntlet();
            if (gauntlet.isEmpty()) {
                return;
            }
            GlStateManager.pushMatrix();
            GlStateManager.enableRescaleNormal();
            model().toLeftHand(scale);
            GlStateManager.rotate(-90F, 1F, 0F, 0F);
            GlStateManager.rotate(180F, 0F, 1F, 0F);
            GlStateManager.translate(-1F / 16F, 1F / 16F, -1F / 16F);
            GlStateManager.scale(1.25F, 1.25F, 1.25F);
            Minecraft.getMinecraft().getRenderItem().renderItem(gauntlet, ItemCameraTransforms.TransformType.THIRD_PERSON_LEFT_HAND);
            GlStateManager.disableRescaleNormal();
            GlStateManager.popMatrix();
        }

        @Override
        public boolean shouldCombineTextures() {
            return false;
        }
    }
}
