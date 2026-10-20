package com.matoon.eternity.client.render;

import com.matoon.eternity.entity.EntityEternity;
import com.matoon.eternity.entity.EntityEternity.State;
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
import java.util.Random;

/**
 * Draws Eternity as a hole in the world shaped like a god: a body of moving stars,
 * a faint violet corona, two burning eyes. When he dies, light pours out of him.
 */
@SideOnly(Side.CLIENT)
public class RenderEternity extends Render<EntityEternity> {

    /** Blocks per model unit at scale 1, so that the figure stands {@link EntityEternity#BASE_HEIGHT} tall. */
    public static final float MODEL_SCALE = 2.56F;

    private static final ModelEternity MODEL = new ModelEternity();
    private static final Random RAYS = new Random();

    public RenderEternity(RenderManager manager) {
        super(manager);
        shadowSize = 0F;
    }

    @Override
    public void doRender(EntityEternity entity, double x, double y, double z, float entityYaw, float partialTicks) {
        float yaw = interpolate(entity.prevRenderYawOffset, entity.renderYawOffset, partialTicks);
        float headYaw = MathHelper.wrapDegrees(interpolate(entity.prevRotationYawHead, entity.rotationYawHead, partialTicks) - yaw);
        float pitch = entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks;
        State state = entity.getState();
        int stateTicks = entity.clientStateTicks;
        float containment = entity.getContainment();
        // As the Regulator drinks him in, he fades and his light is pulled back inside.
        float presence = presence(state, stateTicks + partialTicks) * (1F - 0.95F * containment);
        double distSq = x * x + y * y + z * z;

        drawFigure(x, y, z, yaw, headYaw, pitch, entity.getScale(), presence, entity.ticksExisted + partialTicks, state, stateTicks, distSq, true);

        if (state == State.DYING) {
            drawDeathLight(x, y + entity.height * 0.65D, z, entity.getScale() * (1F - containment),
                    (stateTicks + partialTicks) / 560F * (1F - containment));
        }
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    /** 0 to 1 as he unfolds out of a rift, 1 to 0 as he folds back into one. */
    private static float presence(State state, float ticks) {
        if (state == State.EMERGE) {
            return MathHelper.clamp(ticks / 60F, 0F, 1F);
        }
        if (state == State.DEPART) {
            return 1F - MathHelper.clamp(ticks / 45F, 0F, 1F);
        }
        return 1F;
    }

    /** Also used for the colossal silhouette seen across the void in space. */
    public static void drawFigure(double x, double y, double z, float yaw, float headYaw, float pitch, float scale,
                                  float presence, float age, State state, int stateTicks, double distSq, boolean inWorldPass) {
        if (presence <= 0.01F) {
            return;
        }
        int layers = distSq > 300 * 300 ? 6 : distSq > 90 * 90 ? 10 : 15;
        float size = MODEL_SCALE * scale * (0.3F + 0.7F * presence);

        GlStateManager.disableFog();
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.rotate(180F - yaw, 0F, 1F, 0F);
        GlStateManager.scale(size, size, size);
        GlStateManager.scale(-1F, -1F, 1F);
        GlStateManager.translate(0F, -1.501F, 0F);
        MODEL.pose(age, state, stateTicks, headYaw, pitch);

        GlStateManager.enableCull();
        GlStateManager.cullFace(GlStateManager.CullFace.BACK);
        Cosmic.draw(() -> MODEL.renderBody(0.0625F), layers, presence, presence * 1.15F, 0.8F, 0.55F, 1F, true);

        // Corona: back faces of slightly swollen copies, so only the rim shows.
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        GlStateManager.depthMask(false);
        GlStateManager.cullFace(GlStateManager.CullFace.FRONT);
        for (int k = 1; k <= 3; k++) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(0F, 0.1F, 0F);
            float swell = 1F + 0.03F * k;
            GlStateManager.scale(swell, swell, swell);
            GlStateManager.translate(0F, -0.1F, 0F);
            GlStateManager.color(0.32F, 0.1F, 0.62F, 0.16F / k * presence);
            MODEL.renderBody(0.0625F);
            GlStateManager.popMatrix();
        }
        GlStateManager.cullFace(GlStateManager.CullFace.BACK);

        // Eyes, drawn twice: a hot core and a swollen glow.
        float flicker = 0.85F + 0.15F * MathHelper.sin(age * 0.7F);
        GlStateManager.color(1F, 0.92F, 1F, presence * flicker);
        MODEL.renderEyes(0.0625F);
        GlStateManager.pushMatrix();
        GlStateManager.scale(1.04F, 1.04F, 1.04F);
        GlStateManager.color(0.7F, 0.35F, 1F, 0.5F * presence * flicker);
        MODEL.renderEyes(0.0625F);
        GlStateManager.popMatrix();

        GlStateManager.depthMask(true);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.popMatrix();
        if (inWorldPass) {
            GlStateManager.enableFog();
        }
    }

    /** Shafts of light tearing out of his chest, the way the dragon dies, but more. */
    private static void drawDeathLight(double x, double y, double z, float scale, float progress) {
        float f = MathHelper.clamp(progress, 0F, 1F);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        float size = 0.5F * scale * (1F + f * 2F);
        GlStateManager.scale(size, size, size);
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableFog();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        GlStateManager.disableAlpha();
        GlStateManager.enableCull();
        GlStateManager.depthMask(false);
        RAYS.setSeed(432L);
        int rays = (int) ((f + f * f) / 2F * 90F) + 4;
        for (int i = 0; i < rays; i++) {
            GlStateManager.rotate(RAYS.nextFloat() * 360F, 1F, 0F, 0F);
            GlStateManager.rotate(RAYS.nextFloat() * 360F, 0F, 1F, 0F);
            GlStateManager.rotate(RAYS.nextFloat() * 360F, 0F, 0F, 1F);
            GlStateManager.rotate(RAYS.nextFloat() * 360F, 1F, 0F, 0F);
            GlStateManager.rotate(RAYS.nextFloat() * 360F, 0F, 1F, 0F);
            GlStateManager.rotate(RAYS.nextFloat() * 360F + f * 90F, 0F, 0F, 1F);
            float length = RAYS.nextFloat() * 20F + 5F + f * 14F;
            float width = RAYS.nextFloat() * 2F + 1F + f * 2F;
            buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
            buffer.pos(0D, 0D, 0D).color(255, 255, 255, (int) (255F * (1F - f * 0.4F))).endVertex();
            buffer.pos(-0.866D * width, length, -0.5F * width).color(150, 40, 255, 0).endVertex();
            buffer.pos(0.866D * width, length, -0.5F * width).color(150, 40, 255, 0).endVertex();
            buffer.pos(0D, length, width).color(150, 40, 255, 0).endVertex();
            buffer.pos(-0.866D * width, length, -0.5F * width).color(150, 40, 255, 0).endVertex();
            tessellator.draw();
        }
        GlStateManager.depthMask(true);
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
        GlStateManager.enableFog();
        GlStateManager.popMatrix();
    }

    private static float interpolate(float prev, float now, float partialTicks) {
        return prev + MathHelper.wrapDegrees(now - prev) * partialTicks;
    }

    @Nullable
    @Override
    protected ResourceLocation getEntityTexture(EntityEternity entity) {
        return null;
    }
}
