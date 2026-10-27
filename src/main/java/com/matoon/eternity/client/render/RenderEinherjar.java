package com.matoon.eternity.client.render;

import com.matoon.eternity.Eternity;
import com.matoon.eternity.entity.EntityEinherjar;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** The Einherjar as everyone sees them. They fall the way the model shows it, not by tipping over. */
@SideOnly(Side.CLIENT)
public class RenderEinherjar extends RenderLiving<EntityEinherjar> {

    private static final ResourceLocation SKIN = new ResourceLocation(Eternity.MODID, "textures/entity/einherjar.png");
    private static final float SCALE = 0.95F;

    public RenderEinherjar(RenderManager manager) {
        super(manager, new ModelEinherjar(), 0.45F);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityEinherjar entity) {
        return SKIN;
    }

    @Override
    protected void preRenderCallback(EntityEinherjar entity, float partialTickTime) {
        GlStateManager.scale(SCALE, SCALE, SCALE);
    }

    @Override
    protected void applyRotations(EntityEinherjar entity, float ageInTicks, float rotationYaw, float partialTicks) {
        GlStateManager.rotate(180F - rotationYaw, 0F, 1F, 0F);
    }

    /** No red flash as he dies: he goes down to his knees instead. */
    @Override
    protected int getColorMultiplier(EntityEinherjar entity, float lightBrightness, float partialTickTime) {
        return 0;
    }

    @Override
    protected boolean setBrightness(EntityEinherjar entity, float partialTicks, boolean combineTextures) {
        return entity.deathTime == 0 && super.setBrightness(entity, partialTicks, combineTextures);
    }
}
