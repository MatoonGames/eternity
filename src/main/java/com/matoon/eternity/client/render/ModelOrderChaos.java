package com.matoon.eternity.client.render;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A head and nothing else: Master Order's or Lord Chaos's, as the comics draw them. Boxes
 * like any mob, skinned from tools/OrderChaosSkin.java: a broad skull, a heavy brow and a
 * nose standing proud of the face, a jaw beneath. Order has ears; Chaos has lumps where a
 * forehead and cheeks should be.
 *
 * Model space is the vanilla one, pixels, Y down, the face toward -Z, but the origin is the
 * middle of the head, so it can be hung anywhere and turned on the spot.
 */
@SideOnly(Side.CLIENT)
public class ModelOrderChaos extends ModelBase {

    private final ModelRenderer skull;
    private final ModelRenderer ears;
    private final ModelRenderer lumps;

    public ModelOrderChaos() {
        textureWidth = 64;
        textureHeight = 64;
        skull = new ModelRenderer(this);
        skull.setTextureOffset(0, 0).addBox(-6F, -8F, -6F, 12, 14, 12);
        skull.setTextureOffset(0, 26).addBox(-5F, 6F, -5.5F, 10, 3, 10);
        skull.setTextureOffset(0, 39).addBox(-6F, -3F, -7F, 12, 2, 1);
        skull.setTextureOffset(26, 39).addBox(-1F, -1F, -7F, 2, 3, 1);

        ears = new ModelRenderer(this);
        ears.setTextureOffset(32, 39).addBox(-7F, -2F, -1F, 1, 3, 2);
        ears.mirror = true;
        ears.setTextureOffset(32, 39).addBox(6F, -2F, -1F, 1, 3, 2);

        lumps = new ModelRenderer(this);
        lumps.setTextureOffset(0, 44).addBox(-5F, -9F, -4F, 10, 2, 8);
        lumps.setTextureOffset(36, 44).addBox(-7F, 0F, -4F, 1, 4, 6);
        lumps.mirror = true;
        lumps.setTextureOffset(36, 44).addBox(6F, 0F, -4F, 1, 4, 6);
    }

    /** Draws the head in model space; the caller binds the right skin, then flips and scales. */
    public void draw(boolean order) {
        skull.render(1F);
        if (order) {
            ears.render(1F);
        } else {
            lumps.render(1F);
        }
    }
}
