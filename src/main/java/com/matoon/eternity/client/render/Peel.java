package com.matoon.eternity.client.render;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A model part whose own boxes can be taken away while everything hanging from it stays: a hand
 * still on the arm once the glove of skin over the forearm is gone, a head still on a neck. Its
 * own boxes can be stretched, too, without what hangs from it (a forearm swelling as something
 * moves inside it, and the hand on the end its own size), and drawn in another colour. Drawn
 * exactly as any part is until then.
 */
@SideOnly(Side.CLIENT)
final class Peel extends ModelRenderer {

    private static final float DEG = 180F / (float) Math.PI;

    /** Its own boxes are gone; only what hangs from it is drawn. */
    boolean peeled;
    /** How its own boxes are stretched, about its pivot: swelling, or new and still growing. */
    float sx = 1F, sy = 1F, sz = 1F;
    /** How far it has unrolled, top down, over its own length: cloth coming out of the dark onto her. */
    float grow = 1F;
    /** Cloth with no thickness, drawn only from the side that faces you (see {@link #render}). */
    boolean sheet;
    /** What colour its own boxes are drawn in: raw red, or bruised. */
    float r = 1F, g = 1F, b = 1F;

    Peel(ModelBase model, String name) {
        super(model, name);
    }

    /** Back to as it was made: all there, its own size, its own colour. */
    void plain() {
        peeled = false;
        sx = sy = sz = r = g = b = grow = 1F;
    }

    @Override
    public void render(float scale) {
        if (!sheet) {
            draw(scale);
            return;
        }
        // A sheet has no thickness: its two faces lie in one plane, and drawn both at once they fight. Only the one
        // that faces you, then, whatever the renderer had the culling at.
        boolean culled = org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_CULL_FACE);
        GlStateManager.enableCull();
        draw(scale);
        if (!culled) {
            GlStateManager.disableCull();
        }
    }

    private void draw(float scale) {
        boolean stretched = sx != 1F || sy * grow != 1F || sz != 1F, tinted = r != 1F || g != 1F || b != 1F;
        if (!peeled && !stretched && !tinted) {
            super.render(scale);
            return;
        }
        if (isHidden || !showModel) {
            return;
        }
        GlStateManager.translate(offsetX, offsetY, offsetZ);
        GlStateManager.pushMatrix();
        GlStateManager.translate(rotationPointX * scale, rotationPointY * scale, rotationPointZ * scale);
        if (rotateAngleZ != 0F) {
            GlStateManager.rotate(rotateAngleZ * DEG, 0F, 0F, 1F);
        }
        if (rotateAngleY != 0F) {
            GlStateManager.rotate(rotateAngleY * DEG, 0F, 1F, 0F);
        }
        if (rotateAngleX != 0F) {
            GlStateManager.rotate(rotateAngleX * DEG, 1F, 0F, 0F);
        }
        if (!peeled) {
            GlStateManager.pushMatrix();
            GlStateManager.scale(sx, Math.max(0.001F, sy * grow), sz);
            if (tinted) {
                GlStateManager.color(r, g, b, 1F);
            }
            renderOwn(scale);
            if (tinted) {
                GlStateManager.color(1F, 1F, 1F, 1F);
            }
            GlStateManager.popMatrix();
        }
        if (childModels != null) {
            for (ModelRenderer child : childModels) {
                child.render(scale);
            }
        }
        GlStateManager.popMatrix();
        GlStateManager.translate(-offsetX, -offsetY, -offsetZ);
    }

    /** Its own boxes alone, about its pivot, wherever the caller has put it: a piece torn off. */
    void renderOwn(float scale) {
        for (ModelBox box : cubeList) {
            box.render(Tessellator.getInstance().getBuffer(), scale);
        }
    }
}
