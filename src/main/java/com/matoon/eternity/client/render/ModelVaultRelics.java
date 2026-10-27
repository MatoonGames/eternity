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

        // The Warlock's Eye: the eye free to turn in its frame, the frame on its plinth.
        eye = part("ePlinth", null, 0F, 0F, 0F);
        ModelRenderer frame = part("eMask", eye, 0F, -21.5F, 0F);
        arch(frame, "eArchOuter", VaultBoxes.ARCH_OUTER, 14F, 9F, -0.5F);
        arch(frame, "eArchInner", VaultBoxes.ARCH_INNER, 10.5F, 5.5F, 1F);
        part("eTabR", frame, -11.5F, -5.5F, 0F).rotateAngleZ = 0.35F;
        part("eTabL", frame, 11.5F, -5.5F, 0F).rotateAngleZ = -0.35F;
        orb = part("eOrb", frame, 0F, 0F, 0F);

        // The Crown of Black Fire: the skull on its slab, the blades spreading from its brow, the horns rising from them.
        crown = part("cStand", null, 0F, 0F, 0F);
        ModelRenderer skull = part("cSkull", crown, 0F, -2F, 0F);
        part("cSkullR", skull, 0F, 0F, 0F);
        part("cSkullL", skull, 0F, 0F, 0F);
        int[] hornLength = {7, 7, 6, 6, 5};
        for (int side = -1; side <= 1; side += 2) {
            String s = side < 0 ? "R" : "L";
            ModelRenderer wing = part("cWing" + s, skull, side * 2F, -15F, -0.5F);
            wing.rotateAngleZ = -side * 0.5F;
            ModelRenderer parent = wing;
            for (int i = 0; i < VaultBoxes.CROWN_HORN; i++) {
                ModelRenderer horn = part("cHorn" + i + s, parent, i == 0 ? side * 21.2F : 0F, i == 0 ? -0.5F : -hornLength[i - 1], 0F);
                // Straight up from the blade's end, leaning out a little, then curving slowly in.
                horn.rotateAngleZ = i == 0 ? side * 0.6F : -side * 0.17F;
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
        for (int i = 0; i < 4; i++) {
            float sx = i % 2 == 0 ? -1F : 1F, sz = i < 2 ? -1F : 1F;
            part("kPost", casket, sx * 8F, 0F, sz * 5F);
            // The panels pinch in at their sides, where the posts swell into them.
            part("kBulge", casket, sx * 6.6F, 0F, sz * 5.3F);
        }
        part("kCrest", casket, 0F, -12.8F, -5.6F);
        part("kCrest", casket, 0F, -12.8F, 5.6F);
        // A pointed oval round each end.
        for (int side = -1; side <= 1; side += 2) {
            int n = VaultBoxes.CASKET_RING;
            for (int i = 0; i < n; i++) {
                double a = Math.PI * 2D * (i + 0.5D) / n;
                double z = 3.3D * Math.cos(a) * (0.75D + 0.25D * Math.abs(Math.cos(a)));
                double y = -6.3D + 4.8D * Math.sin(a);
                double dz = -3.3D * Math.sin(a), dy = 4.8D * Math.cos(a);
                ModelRenderer seg = part("kRing", casket, side * 8.6F, (float) y, (float) z);
                seg.rotateAngleX = (float) Math.atan2(dz, dy);
            }
        }


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

    /** An arch of n segments over the top half of an ellipse, rx across and ry high, its ends at height cy. */
    private void arch(ModelRenderer parent, String name, int n, float rx, float ry, float cy) {
        for (int i = 0; i < n; i++) {
            double a = Math.PI * (i + 0.5D) / n;
            ModelRenderer seg = part(name, parent, (float) (rx * Math.cos(a)), (float) (cy - ry * Math.sin(a)), 0F);
            seg.rotateAngleZ = (float) Math.atan2(-ry * Math.cos(a), -rx * Math.sin(a));
        }
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
