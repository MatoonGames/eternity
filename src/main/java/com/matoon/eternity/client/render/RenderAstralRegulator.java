package com.matoon.eternity.client.render;

import com.matoon.eternity.entity.EntityAstralRegulator;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;

/** The Regulator in flight and at work: it swells and blazes as it fills. */
@SideOnly(Side.CLIENT)
public class RenderAstralRegulator extends Render<EntityAstralRegulator> {

    public RenderAstralRegulator(RenderManager manager) {
        super(manager);
        shadowSize = 0F;
    }

    @Override
    public void doRender(EntityAstralRegulator entity, double x, double y, double z, float entityYaw, float partialTicks) {
        float time = entity.ticksExisted + partialTicks;
        float progress = entity.getProgress();
        float swell = 1F + progress * 0.8F;
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y + 0.3D, z);

        GlStateManager.pushMatrix();
        GlStateManager.rotate(time * (2F + progress * 10F), 0F, 1F, 0F);
        GlStateManager.rotate(15F + MathHelper.sin(time * 0.05F) * 10F, 1F, 0F, 0F);
        OrbRenderer.draw(0.5F * swell, time, progress);
        GlStateManager.popMatrix();

        // A blinding halo that grows as it drinks.
        GlStateManager.rotate(-renderManager.playerViewY, 0F, 1F, 0F);
        GlStateManager.rotate(renderManager.playerViewX, 1F, 0F, 0F);
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableFog();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        GlStateManager.depthMask(false);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        float flicker = 0.8F + 0.2F * MathHelper.sin(time * 0.9F);
        halo(1.2F + progress * 7F, 0.85F, 0.95F, 1F, (0.35F + progress * 0.45F) * flicker);
        halo(0.6F + progress * 2F, 1F, 1F, 1F, (0.5F + progress * 0.5F) * flicker);

        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.depthMask(true);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableBlend();
        GlStateManager.enableFog();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    private static void halo(float radius, float r, float g, float b, float alpha) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(0, 0, 0).color(r, g, b, alpha).endVertex();
        for (int i = 0; i <= 32; i++) {
            double angle = Math.PI * 2D * i / 32D;
            buffer.pos(Math.cos(angle) * radius, Math.sin(angle) * radius, 0).color(r * 0.6F, g * 0.4F, b, 0F).endVertex();
        }
        tessellator.draw();
    }

    @Nullable
    @Override
    protected ResourceLocation getEntityTexture(EntityAstralRegulator entity) {
        return null;
    }
}
