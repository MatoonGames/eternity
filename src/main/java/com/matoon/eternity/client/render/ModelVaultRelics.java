package com.matoon.eternity.client.render;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The treasures of Odin's Vault, each on its stand, all skinned from {@link VaultBoxes#RELICS}.
 * Every one stands with its foot at the model's origin, in the usual model space (y down, its
 * face toward -z), so it is drawn the way an entity is.
 */
@SideOnly(Side.CLIENT)
public class ModelVaultRelics extends ModelBase {

    private static final float PI = (float) Math.PI;

    private final VaultBoxes.Sheet sheet = VaultBoxes.RELICS;
    public final ModelRenderer gauntlet, flame, fork, tablet, eye, crown, pedestal, casket, plinth, cradle;
    public final ModelRenderer orb, tineR, tineL;
    private final ModelRenderer[] fingers = new ModelRenderer[4], tips = new ModelRenderer[4];

    public ModelVaultRelics() {
        textureWidth = sheet.width;
        textureHeight = sheet.height;

        // The fake gauntlet: a right hand, back toward you, fingers a little curled, a stone on every knuckle.
        gauntlet = part("gStand", null, 0F, 0F, 0F);
        ModelRenderer cuff = part("gCuff", gauntlet, 0F, -9F, 0F);
        ModelRenderer hand = part("gHand", cuff, 0F, -7F, 0F);
        part("gGemCentre", hand, 0F, 0F, 0F);
        String[] knuckles = {"gGemSpace", "gGemReality", "gGemPower", "gGemTime"};
        float[] curl = {0.18F, 0.1F, 0.14F, 0.26F};
        for (int i = 0; i < 4; i++) {
            float x = -3F + i * 2F;
            fingers[i] = part("gFinger" + i, hand, x, -6F, 0F);
            fingers[i].rotateAngleX = -curl[i];
            fingers[i].rotateAngleZ = (i - 1.5F) * 0.05F;
            tips[i] = part("gTip" + i, fingers[i], 0F, -3F, 0F);
            tips[i].rotateAngleX = -curl[i] * 1.6F;
            part(knuckles[i], hand, x, -5.2F, -2.3F);
        }
        ModelRenderer thumb = part("gThumb", hand, -4.4F, -2.5F, -0.5F);
        thumb.rotateAngleZ = -0.6F;
        thumb.rotateAngleX = -0.25F;
        ModelRenderer thumbTip = part("gThumbTip", thumb, 0F, -3F, 0F);
        thumbTip.rotateAngleZ = 0.3F;
        part("gGemSoul", thumb, 0F, -1.6F, -1F);

        // The Eternal Flame's pillar and chalice. The flame itself is drawn as light.
        flame = part("fPillar", null, 0F, 0F, 0F);
        ModelRenderer chalice = part("fChalice", flame, 0F, -24F, 0F);
        part("fEmbers", chalice, 0F, 0F, 0F);
        for (int i = 0; i < 4; i++) {
            float sx = i % 2 == 0 ? -1F : 1F, sz = i < 2 ? -1F : 1F;
            ModelRenderer prong = part("fProng" + i, chalice, sx * 6.8F, -12F, sz * 6.8F);
            prong.rotateAngleZ = sx * 0.32F;
            prong.rotateAngleX = -sz * 0.32F;
        }

        // The Tuning Fork, its tines free to ring.
        fork = part("tStand", null, 0F, 0F, 0F);
        part("tFork", fork, 0F, 0F, 0F);
        tineR = part("tTineR", fork, -3.5F, -19F, 0F);
        tineL = part("tTineL", fork, 3.5F, -19F, 0F);

        // The Tablet of Life and Time, upright in its gilt base.
        tablet = part("lStand", null, 0F, 0F, 0F);
        ModelRenderer slab = part("lSlab", tablet, 0F, -4F, 0F);
        part("lSlabR", slab, 0F, 0F, 0F);
        part("lSlabL", slab, 0F, 0F, 0F);

        // The Warlock's Eye, cradled in three claws, free to turn.
        eye = part("eStand", null, 0F, 0F, 0F);
        for (int i = 0; i < 3; i++) {
            float a = PI * 2F * i / 3F + PI / 6F;
            ModelRenderer claw = part("eClaw" + i, eye, MathHelper.sin(a) * 1.5F, -11F, -MathHelper.cos(a) * 1.5F);
            // Leaning out from the stem, along the direction it stands on.
            claw.rotateAngleY = -a;
            claw.rotateAngleX = 0.55F;
            ModelRenderer tip = part("eClawTip" + i, claw, 0F, -6F, 0F);
            tip.rotateAngleX = -0.95F;
        }
        orb = part("eOrb", eye, 0F, -16.5F, 0F);

        // The Crown of Black Fire on its tripod: points round its brow, horns sweeping up and back.
        crown = part("cStand", null, 0F, 0F, 0F);
        ModelRenderer band = part("cBand", crown, 0F, -10F, 0F);
        for (int i = 0; i < 5; i++) {
            float a = (float) Math.toRadians(-64D + i * 32D);
            ModelRenderer point = part("cPoint" + i, band, MathHelper.sin(a) * 6F, -4F, -MathHelper.cos(a) * 6F);
            point.rotateAngleX = 0.25F * MathHelper.cos(a);
            point.rotateAngleZ = 0.25F * MathHelper.sin(a);
        }
        for (int side = -1; side <= 1; side += 2) {
            String s = side < 0 ? "R" : "L";
            ModelRenderer parent = band;
            for (int i = 0; i < 5; i++) {
                ModelRenderer horn = part("cHorn" + i + s, parent, i == 0 ? side * 6F : 0F, i == 0 ? -2.5F : -3.6F, 0F);
                // Out from the band, then sweeping up and back like a bull's.
                horn.rotateAngleZ = i == 0 ? side * 0.75F : -side * 0.36F;
                horn.rotateAngleX = i == 0 ? 0F : -0.14F;
                parent = horn;
            }
        }

        // The Pedestal of Fimbulvetr, and the Casket of Ancient Winters, which is also drawn on its own.
        pedestal = part("pPedestal", null, 0F, 0F, 0F);
        part("pPedestalR", pedestal, 0F, 0F, 0F);
        part("pPedestalL", pedestal, 0F, 0F, 0F);
        casket = part("casket", null, 0F, 0F, 0F);
        part("casketR", casket, 0F, 0F, 0F);
        part("casketL", casket, 0F, 0F, 0F);
        part("handleR", casket, 0F, 0F, 0F);
        part("handleL", casket, 0F, 0F, 0F);

        // The stands Mjolnir and the Tesseract rest on.
        plinth = part("plinth", null, 0F, 0F, 0F);
        cradle = part("cradle", null, 0F, 0F, 0F);
        for (int i = 0; i < 4; i++) {
            float sx = i % 2 == 0 ? -1F : 1F, sz = i < 2 ? -1F : 1F;
            ModelRenderer prong = part("cradleProng" + i, cradle, sx * 3.5F, -17F, sz * 3.5F);
            prong.rotateAngleZ = sx * 0.2F;
            prong.rotateAngleX = -sz * 0.2F;
        }
    }

    private ModelRenderer part(String name, ModelRenderer parent, float x, float y, float z) {
        return VaultParts.part(this, sheet, name, parent, x, y, z);
    }

    /** The Tuning Fork's tines, shivering as it rings. */
    public void ring(float age, float strength) {
        float shiver = MathHelper.sin(age * 4.1F) * 0.012F * strength;
        tineR.rotateAngleZ = shiver;
        tineL.rotateAngleZ = -shiver;
    }

    /** The Warlock's Eye turned to look along yaw and pitch, in radians, relative to the way it faces. */
    public void look(float yaw, float pitch) {
        orb.rotateAngleY = yaw;
        orb.rotateAngleX = pitch;
    }
}
