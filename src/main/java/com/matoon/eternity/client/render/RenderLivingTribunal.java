package com.matoon.eternity.client.render;

import com.matoon.eternity.entity.EntityLivingTribunal;
import com.matoon.eternity.entity.EntityLivingTribunal.State;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

/** The Living Tribunal on his feet, fighting; beaten to his knees; drawn down into the Regulator. */
@SideOnly(Side.CLIENT)
public class RenderLivingTribunal extends Render<EntityLivingTribunal> {

    private final ModelTribunal model = new ModelTribunal();

    public RenderLivingTribunal(RenderManager manager) {
        super(manager);
        shadowSize = 0F;
    }

    @Override
    public void doRender(EntityLivingTribunal entity, double x, double y, double z, float entityYaw, float partialTicks) {
        float ticks = entity.clientStateTicks + partialTicks;
        float time = entity.ticksExisted + partialTicks;
        State state = entity.getState();
        ModelTribunal.Pose pose;
        float t = 0F;
        float eyes = 0.6F;
        float shrink = 1F;
        switch (state) {
            case GAVEL:
                pose = ModelTribunal.Pose.GAVEL;
                t = MathHelper.clamp(ticks / EntityLivingTribunal.GAVEL_TICKS, 0F, 1F);
                break;
            case GAZE:
                pose = ModelTribunal.Pose.GAZE;
                eyes = 1F;
                break;
            case STORM:
            case SCALES:
            case CONVERGENCE:
            case VERDICT:
                pose = ModelTribunal.Pose.CAST;
                eyes = 0.9F;
                break;
            case SENTENCE:
            case EXILE:
            case ERASURE:
                // He points, and it is so.
                pose = ModelTribunal.Pose.REACH;
                t = MathHelper.clamp(ticks / 15F, 0F, 1F);
                eyes = 1F;
                break;
            case PILLARS:
            case WEIGHT:
                pose = ModelTribunal.Pose.GAVEL;
                t = MathHelper.clamp(ticks / 70F, 0F, 1F);
                break;
            case ECHOES:
                pose = ModelTribunal.Pose.GAVEL;
                t = MathHelper.clamp(ticks / 75F, 0F, 1F);
                eyes = 1F;
                break;
            case DOWNED:
                pose = ModelTribunal.Pose.KNEEL;
                t = MathHelper.clamp(ticks / 15F, 0F, 1F);
                eyes = 0.1F;
                break;
            case RISING:
                pose = ModelTribunal.Pose.KNEEL;
                t = 1F - MathHelper.clamp(ticks / EntityLivingTribunal.RISE_TICKS, 0F, 1F);
                eyes = 0.3F + 0.7F * (1F - t);
                break;
            case ABSORBING:
                pose = ModelTribunal.Pose.KNEEL;
                t = 1F;
                eyes = 1F;
                float p = MathHelper.clamp(ticks / EntityLivingTribunal.ABSORB_TICKS, 0F, 1F);
                shrink = 1F - 0.97F * p * p;
                break;
            default:
                pose = ModelTribunal.Pose.IDLE;
                break;
        }
        float yaw = entity.prevRotationYaw + MathHelper.wrapDegrees(entity.rotationYaw - entity.prevRotationYaw) * partialTicks;
        float scale = EntityLivingTribunal.SCALE * shrink;

        GlStateManager.pushMatrix();
        GlStateManager.disableFog();
        GlStateManager.translate(x, y + (1F - shrink) * EntityLivingTribunal.HEIGHT_PX * EntityLivingTribunal.SCALE * 0.5F, z);
        GlStateManager.rotate(-yaw, 0F, 1F, 0F);
        // Model space is Y down and faces -Z; flipping both stands him up facing where he looks.
        GlStateManager.scale(scale, -scale, -scale);
        model.draw(pose, t, eyes, time);
        GlStateManager.popMatrix();
        GlStateManager.enableLighting();
        GlStateManager.enableFog();
    }

    @Override
    public boolean shouldRender(EntityLivingTribunal entity, ICamera camera, double x, double y, double z) {
        return true;
    }

    @Nullable
    @Override
    protected ResourceLocation getEntityTexture(EntityLivingTribunal entity) {
        return null;
    }
}
