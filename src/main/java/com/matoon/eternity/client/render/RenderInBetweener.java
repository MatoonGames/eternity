package com.matoon.eternity.client.render;

import com.matoon.eternity.Eternity;
import com.matoon.eternity.entity.EntityInBetweener;
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
 * The In-Betweener, as anyone near the junction sees him. Outside his glass the neutral
 * nothing drinks his colour: a second, grey skin fades in over the first. Falling, he
 * topples onto his back; cast adrift, he turns slowly in the void; drawn into a Regulator, he dwindles away. The Soul
 * Gem on his brow glows, whatever colour he is.
 *
 * Inside his bubble he can be in several places at once; those copies are drawn through
 * this renderer too, as pure white or pure black silhouettes (see {@link #ghost}).
 */
@SideOnly(Side.CLIENT)
public class RenderInBetweener extends RenderLiving<EntityInBetweener> {

    private static final ResourceLocation SKIN = new ResourceLocation(Eternity.MODID, "textures/entity/inbetweener.png");
    private static final ResourceLocation DRAINED = new ResourceLocation(Eternity.MODID, "textures/entity/inbetweener_drained.png");

    /** While drawing his echoes: 0 for a white silhouette, 1 for a black one; -1 for himself. */
    public static int ghost = -1;
    /** How solid an echo is. */
    public static float ghostAlpha = 0.5F;

    public RenderInBetweener(RenderManager manager) {
        super(manager, new ModelInBetweener(), 0.3F);
        addLayer(new GemGlow());
    }

    private static ModelInBetweener standing;

    /**
     * Him standing straight, in model space, for the inside of a Regulator's tooltip: arms at
     * his sides, the gem on his brow.
     */
    public static void drawStanding(float time) {
        if (standing == null) {
            standing = new ModelInBetweener();
        }
        GlStateManager.enableTexture2D();
        GlStateManager.color(1F, 1F, 1F, 1F);
        net.minecraft.client.Minecraft.getMinecraft().getTextureManager().bindTexture(SKIN);
        standing.bipedRightArm.rotateAngleZ = 0.08F + MathHelper.sin(time * 0.05F) * 0.03F;
        standing.bipedLeftArm.rotateAngleZ = -0.08F - MathHelper.sin(time * 0.05F) * 0.03F;
        standing.bipedHead.render(1F);
        standing.bipedBody.render(1F);
        standing.bipedRightArm.render(1F);
        standing.bipedLeftArm.render(1F);
        standing.bipedRightLeg.render(1F);
        standing.bipedLeftLeg.render(1F);
    }


    /** Adrift, a faint grey glimmer hangs about him: the last of the neutral point clinging on, and the only way to spot him from far off. */
    @Override
    public void doRender(EntityInBetweener entity, double x, double y, double z, float entityYaw, float partialTicks) {
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
        if (entity.getState() != EntityInBetweener.State.ADRIFT || ghost >= 0) {
            return;
        }
        float time = entity.ticksExisted + partialTicks;
        float pulse = 0.7F + 0.3F * MathHelper.sin(time * 0.045F);
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y + 0.9D, z);
        GlStateManager.rotate(-renderManager.playerViewY, 0F, 1F, 0F);
        GlStateManager.rotate(renderManager.playerViewX, 1F, 0F, 0F);
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.disableFog();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        GlStateManager.depthMask(false);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
        // Farther off it reads larger, so that a speck in the void can still be found.
        double distance = Math.sqrt(x * x + y * y + z * z);
        float reach = (float) MathHelper.clamp(distance / 40D, 1D, 4D);
        glow(1.8F * reach, 0.85F, 0.86F, 0.9F, 0.22F * pulse);
        glow(0.7F, 1F, 1F, 1F, 0.35F * pulse);
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.depthMask(true);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableBlend();
        GlStateManager.enableFog();
        GlStateManager.enableCull();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    private static void glow(float radius, float r, float g, float b, float a) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(0D, 0D, 0D).color(r, g, b, a).endVertex();
        for (int i = 0; i <= 24; i++) {
            double angle = Math.PI * 2D * i / 24D;
            buffer.pos(Math.cos(angle) * radius, Math.sin(angle) * radius, 0D).color(r, g, b, 0F).endVertex();
        }
        tessellator.draw();
    }
    @Override
    protected ResourceLocation getEntityTexture(EntityInBetweener entity) {
        return SKIN;
    }

    /** Falling, he goes over backward and lies on his back. Adrift, he turns slowly over and over in the void. */
    @Override
    protected void applyRotations(EntityInBetweener entity, float ageInTicks, float rotationYaw, float partialTicks) {
        if (entity.getState() == EntityInBetweener.State.ADRIFT) {
            GlStateManager.translate(0F, 0.9F, 0F);
            GlStateManager.rotate(ageInTicks * 0.15F, 0F, 1F, 0F);
            GlStateManager.rotate(70F + 18F * MathHelper.sin(ageInTicks * 0.01F), 1F, 0F, 0F);
            GlStateManager.rotate(22F * MathHelper.sin(ageInTicks * 0.0071F), 0F, 0F, 1F);
            GlStateManager.translate(0F, -0.9F, 0F);
            return;
        }
        boolean absorbing = entity.getState() == EntityInBetweener.State.ABSORBING;
        if (entity.getState() != EntityInBetweener.State.FALLEN && !absorbing) {
            super.applyRotations(entity, ageInTicks, rotationYaw, partialTicks);
            return;
        }
        float t = absorbing ? 1F : MathHelper.clamp((entity.clientStateTicks + partialTicks) / 12F, 0F, 1F);
        t = t * t;
        if (absorbing) {
            // Drawn into the crystal: he dwindles to nothing where he lies.
            float p = MathHelper.clamp((entity.clientStateTicks + partialTicks) / 100F, 0F, 1F);
            float s = 1F - p * p;
            GlStateManager.scale(s, s, s);
        }
        GlStateManager.rotate(180F - rotationYaw, 0F, 1F, 0F);
        GlStateManager.translate(0F, 0.13F * t, 0F);
        GlStateManager.rotate(90F * t, 1F, 0F, 0F);
    }

    @Override
    protected void renderModel(EntityInBetweener entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
                               float headPitch, float scaleFactor) {
        if (ghost >= 0) {
            echo(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor);
            return;
        }
        super.renderModel(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor);
        float drain = entity.getDrain();
        if (drain > 0.01F && !entity.isInvisible()) {
            // The grey comes in over him, the same shape exactly, until it is all there is.
            bindTexture(DRAINED);
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            GlStateManager.depthFunc(GL11.GL_LEQUAL);
            GlStateManager.color(1F, 1F, 1F, drain);
            mainModel.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor);
            GlStateManager.color(1F, 1F, 1F, 1F);
            GlStateManager.disableBlend();
        }
    }

    private void echo(EntityInBetweener entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
                      float headPitch, float scaleFactor) {
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        if (ghost == 0) {
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            GlStateManager.color(1F, 1F, 1F, ghostAlpha);
        } else {
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            GlStateManager.color(0F, 0F, 0F, ghostAlpha);
        }
        mainModel.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor);
        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.depthMask(true);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
    }

    /** The light of the Soul Gem on his brow. */
    private class GemGlow implements LayerRenderer<EntityInBetweener> {
        @Override
        public void doRenderLayer(EntityInBetweener entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
                                  float netHeadYaw, float headPitch, float scale) {
            if (!entity.hasGem() || ghost >= 0) {
                return;
            }
            float pulse = 0.75F + 0.25F * MathHelper.sin(ageInTicks * 0.15F);
            GlStateManager.pushMatrix();
            ((ModelInBetweener) mainModel).bipedHead.postRender(scale);
            GlStateManager.translate(0F, -6F * scale, -5.2F * scale);
            GlStateManager.disableTexture2D();
            GlStateManager.disableLighting();
            GlStateManager.disableCull();
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            GlStateManager.depthMask(false);
            GlStateManager.shadeModel(GL11.GL_SMOOTH);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
            disc(0.1F, 1F, 0.85F, 0.55F, 0.9F * pulse);
            disc(0.32F, 1F, 0.5F, 0.1F, 0.45F * pulse);
            GlStateManager.shadeModel(GL11.GL_FLAT);
            GlStateManager.depthMask(true);
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            GlStateManager.disableBlend();
            GlStateManager.enableCull();
            GlStateManager.enableLighting();
            GlStateManager.enableTexture2D();
            GlStateManager.popMatrix();
        }

        private void disc(float radius, float r, float g, float b, float a) {
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
            buffer.pos(0D, 0D, 0D).color(r, g, b, a).endVertex();
            for (int i = 0; i <= 16; i++) {
                double angle = Math.PI * 2D * i / 16D;
                buffer.pos(Math.cos(angle) * radius, Math.sin(angle) * radius, 0D).color(r, g, b, 0F).endVertex();
            }
            tessellator.draw();
        }

        @Override
        public boolean shouldCombineTextures() {
            return false;
        }
    }
}
