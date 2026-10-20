package com.matoon.eternity.client.render;

import com.matoon.eternity.entity.EntityCosmicComet;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

/**
 * Draws nothing. The comet is drawn whole, body or no body, by
 * {@link com.matoon.eternity.client.CometClient}, so it looks the same from across
 * the sky as it does within reach.
 */
@SideOnly(Side.CLIENT)
public class RenderCosmicComet extends Render<EntityCosmicComet> {

    public RenderCosmicComet(RenderManager manager) {
        super(manager);
        shadowSize = 0F;
    }

    @Override
    public boolean shouldRender(EntityCosmicComet entity, ICamera camera, double x, double y, double z) {
        return false;
    }

    @Nullable
    @Override
    protected ResourceLocation getEntityTexture(EntityCosmicComet entity) {
        return null;
    }
}
