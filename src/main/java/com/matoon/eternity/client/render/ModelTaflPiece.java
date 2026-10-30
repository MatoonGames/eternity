package com.matoon.eternity.client.render;

import com.matoon.eternity.world.Kingstable;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * One of the Grandmaster's pieces, taller than whoever plays them: a figure without a face on a
 * stone plinth. Raiders are gold, with a horned helm, a crest and a round shield; guards are blue,
 * with a tall cross-shield and a spear; the king is blue and gold, crowned and caped, larger than
 * either. One shape, dressed three ways; built upward from y = 0 (negative is up) and skinned
 * from the layout in tools/GrandmasterSkin.java.
 */
@SideOnly(Side.CLIENT)
public class ModelTaflPiece extends ModelBase {

    private final ModelRenderer plinth, step, robe, chest, head, helm, hornR, hornL, crest, shield, spear, cape;

    public ModelTaflPiece() {
        textureWidth = 64;
        textureHeight = 64;
        plinth = box(0, 0, -6F, -2F, -6F, 12, 2, 12);
        step = box(0, 14, -5F, -4F, -5F, 10, 2, 10);
        robe = box(0, 26, -4F, -12F, -4F, 8, 8, 8);
        chest = box(32, 26, -3.5F, -18F, -3.5F, 7, 6, 7);
        head = box(40, 14, -2.5F, -23F, -2.5F, 5, 5, 5);
        helm = box(20, 42, -3F, -25F, -3F, 6, 3, 6);
        hornR = box(48, 0, -0.5F, -4F, -0.5F, 1, 4, 1);
        hornR.setRotationPoint(-3F, -23.5F, 0F);
        hornR.rotateAngleZ = -0.55F;
        hornL = box(48, 0, -0.5F, -4F, -0.5F, 1, 4, 1);
        hornL.setRotationPoint(3F, -23.5F, 0F);
        hornL.rotateAngleZ = 0.55F;
        crest = box(0, 54, -0.5F, -28F, -2.5F, 1, 3, 5);
        shield = box(0, 42, -4F, -17F, -5.1F, 8, 11, 1);
        spear = box(60, 0, -0.5F, -27F, -0.5F, 1, 24, 1);
        spear.setRotationPoint(5F, 0F, -1F);
        cape = box(44, 39, -4F, -18F, 3.6F, 8, 12, 1);
        cape.rotateAngleX = 0.06F;
    }

    private ModelRenderer box(int u, int v, float x, float y, float z, int w, int h, int d) {
        ModelRenderer part = new ModelRenderer(this, u, v);
        part.addBox(x, y, z, w, h, d);
        return part;
    }

    /** Draws a piece of the given kind (see {@link Kingstable}) at the current matrix, in model pixels. */
    public void render(byte kind, float scale) {
        plinth.render(scale);
        step.render(scale);
        robe.render(scale);
        chest.render(scale);
        head.render(scale);
        helm.render(scale);
        if (kind == Kingstable.RAIDER) {
            hornR.render(scale);
            hornL.render(scale);
            crest.render(scale);
        }
        if (kind == Kingstable.GUARD) {
            spear.render(scale);
        }
        if (kind == Kingstable.KING) {
            cape.render(scale);
        }
        shield.render(scale);
    }

    /** Blocks tall, for each kind as drawn. */
    public static float height(byte kind) {
        return 28F / 16F * scale(kind);
    }

    /** How much larger than a player's pixel each kind is drawn. */
    public static float scale(byte kind) {
        return kind == Kingstable.KING ? 2.2F : 1.8F;
    }
}
