package com.matoon.eternity.client.render;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * The switches an effect is likely to flip, as they were before it drew, so it can put back
 * exactly what it found instead of guessing. Guessing goes wrong: drawing after the world,
 * where vanilla has fog off, and "restoring" it on, fogs the hand and the whole HUD into the
 * fog colour, which at night is black.
 */
@SideOnly(Side.CLIENT)
public final class GlSnapshot {

    private final boolean fog, lighting, cull, texture, blend, alpha;
    private final int cullFace;
    private final float lightX, lightY;

    private GlSnapshot() {
        fog = GL11.glIsEnabled(GL11.GL_FOG);
        lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
        cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        texture = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        blend = GL11.glIsEnabled(GL11.GL_BLEND);
        alpha = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
        cullFace = GL11.glGetInteger(GL11.GL_CULL_FACE_MODE);
        lightX = OpenGlHelper.lastBrightnessX;
        lightY = OpenGlHelper.lastBrightnessY;
    }

    public static GlSnapshot take() {
        return new GlSnapshot();
    }

    /** Puts every switch back as it was, through GlStateManager so its cache stays true. */
    public void restore() {
        set(fog, GlStateManager::enableFog, GlStateManager::disableFog);
        set(lighting, GlStateManager::enableLighting, GlStateManager::disableLighting);
        set(cull, GlStateManager::enableCull, GlStateManager::disableCull);
        set(texture, GlStateManager::enableTexture2D, GlStateManager::disableTexture2D);
        set(blend, GlStateManager::enableBlend, GlStateManager::disableBlend);
        set(alpha, GlStateManager::enableAlpha, GlStateManager::disableAlpha);
        GlStateManager.cullFace(cullFace == GL11.GL_FRONT ? GlStateManager.CullFace.FRONT
                : cullFace == GL11.GL_FRONT_AND_BACK ? GlStateManager.CullFace.FRONT_AND_BACK : GlStateManager.CullFace.BACK);
        GlStateManager.depthMask(true);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.color(1F, 1F, 1F, 1F);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightX, lightY);
    }

    private static void set(boolean on, Runnable enable, Runnable disable) {
        if (on) {
            enable.run();
        } else {
            disable.run();
        }
    }
}
