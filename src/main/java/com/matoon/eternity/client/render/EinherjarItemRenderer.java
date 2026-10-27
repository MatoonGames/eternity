package com.matoon.eternity.client.render;

import com.matoon.eternity.Eternity;
import com.matoon.eternity.item.EternityItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Draws everything an Einherjar carries wherever an item is drawn, out of the same boxes as he is:
 * each piece of his armour on its own, his spear and his shield. The shield is laid out the way
 * vanilla's is, so it sits in the hand and blocks with vanilla's display transforms.
 */
@SideOnly(Side.CLIENT)
public class EinherjarItemRenderer extends TileEntityItemStackRenderer {

    private static final ResourceLocation SKIN = new ResourceLocation(Eternity.MODID, "textures/entity/einherjar.png");
    private static final float S = 0.0625F;

    private final ModelEinherjar model = new ModelEinherjar();

    @Override
    public void renderByItem(ItemStack stack, float partialTicks) {
        Item item = stack.getItem();
        Minecraft.getMinecraft().getTextureManager().bindTexture(SKIN);
        GlStateManager.pushMatrix();
        GlStateManager.enableRescaleNormal();
        if (item == EternityItems.EINHERJAR_SHIELD) {
            // As vanilla's: the face of the shield toward +z, centred on the corner of the item's space.
            GlStateManager.scale(1F, -1F, -1F);
            GlStateManager.rotate(90F, 0F, 1F, 0F);
            GlStateManager.translate(-1F * S, 0F, 0F);
            alone(model.shield, () -> model.shield.render(S));
        } else if (item == EternityItems.EINHERJAR_SPEAR) {
            // Its grip at the middle of the item's space, the blade up.
            GlStateManager.translate(0.5F, 0.5F, 0.5F);
            GlStateManager.scale(-1F, -1F, 1F);
            alone(model.spear, () -> model.spear.render(S));
        } else {
            GlStateManager.translate(0.5F, 0.5F, 0.5F);
            GlStateManager.scale(-1F, -1F, 1F);
            armour(item);
        }
        GlStateManager.disableRescaleNormal();
        GlStateManager.popMatrix();
    }

    /** One piece of the armour, as it would be on him, centred. */
    private void armour(Item item) {
        if (item == EternityItems.EINHERJAR_HELM) {
            GlStateManager.translate(0F, 7F * S, 0F);
            GlStateManager.scale(0.62F, 0.62F, 0.62F);
            alone(model.head, () -> model.head.render(S));
        } else if (item == EternityItems.EINHERJAR_CUIRASS) {
            GlStateManager.translate(0F, 4F * S, 0F);
            GlStateManager.scale(0.62F, 0.62F, 0.62F);
            GlStateManager.translate(0F, 2F * S, 0F);
            hiding(model.head, () -> hiding(model.spear, () -> hiding(model.shield, () -> alone(model.abdomen, () -> model.abdomen.render(S)))));
        } else if (item == EternityItems.EINHERJAR_GREAVES) {
            GlStateManager.translate(0F, -6F * S, 0F);
            GlStateManager.scale(0.75F, 0.75F, 0.75F);
            hiding(model.abdomen, () -> hiding(model.rShin, () -> hiding(model.lShin, () -> alone(model.root, () -> model.root.render(S)))));
        } else {
            GlStateManager.translate(0F, -4F * S, 0F);
            GlStateManager.scale(0.8F, 0.8F, 0.8F);
            GlStateManager.translate(-2.4F * S, 0F, 0F);
            alone(model.rShin, () -> model.rShin.render(S));
            GlStateManager.translate(4.8F * S, 0F, 0F);
            alone(model.lShin, () -> model.lShin.render(S));
        }
    }

    /** Draws a part away from its parent, at the origin. */
    private static void alone(ModelRenderer part, Runnable draw) {
        float x = part.rotationPointX, y = part.rotationPointY, z = part.rotationPointZ;
        part.setRotationPoint(0F, 0F, 0F);
        draw.run();
        part.setRotationPoint(x, y, z);
    }

    private static void hiding(ModelRenderer part, Runnable draw) {
        boolean was = part.isHidden;
        part.isHidden = true;
        draw.run();
        part.isHidden = was;
    }
}
