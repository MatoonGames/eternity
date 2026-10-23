package com.matoon.eternity.client.render;

import com.matoon.eternity.entity.EntityInBetweener;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The In-Betweener as a Minecraft mob: exactly a player's shape and size, skinned in the
 * vanilla 64x64 player layout (see tools/InBetweenerSkin.java), with one box more: the
 * Soul Gem, a single pixel standing proud of his brow, shown only while he wears it.
 * The skin's overlay layers are left empty, so the overlay boxes are never drawn.
 *
 * How he holds himself comes from his synced state: cross-legged in the air while he
 * waits; talking with his hands; palms flat against the glass, pushing; arms flung wide
 * the moment it breaks; walking out; limp where he fell; and spread like a crucifix while
 * he is everything, inside his bubble, with you.
 */
@SideOnly(Side.CLIENT)
public class ModelInBetweener extends ModelPlayer {

    private final ModelRenderer gem;

    public ModelInBetweener() {
        super(0F, false);
        bipedHeadwear.showModel = false;
        bipedBodyWear.showModel = false;
        bipedLeftArmwear.showModel = false;
        bipedRightArmwear.showModel = false;
        bipedLeftLegwear.showModel = false;
        bipedRightLegwear.showModel = false;
        gem = new ModelRenderer(this, 56, 16);
        gem.addBox(-0.5F, -6.5F, -4.6F, 1, 1, 1);
        bipedHead.addChild(gem);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch,
                                  float scaleFactor, Entity entity) {
        super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entity);
        if (!(entity instanceof EntityInBetweener)) {
            return;
        }
        EntityInBetweener in = (EntityInBetweener) entity;
        gem.showModel = in.hasGem();
        float t = ageInTicks;
        float since = in.clientStateTicks;
        bipedBody.rotateAngleX = 0F;
        switch (in.getState()) {
            case HOVER:
                // Cross-legged on nothing, hands on his knees, breathing.
                bipedRightLeg.rotateAngleX = bipedLeftLeg.rotateAngleX = -1.41F;
                bipedRightLeg.rotateAngleY = 0.31F;
                bipedLeftLeg.rotateAngleY = -0.31F;
                bipedRightLeg.rotateAngleZ = bipedLeftLeg.rotateAngleZ = 0F;
                bipedRightArm.rotateAngleX = bipedLeftArm.rotateAngleX = -0.63F + MathHelper.sin(t * 0.05F) * 0.04F;
                bipedRightArm.rotateAngleY = bipedLeftArm.rotateAngleY = 0F;
                bipedRightArm.rotateAngleZ = 0.12F;
                bipedLeftArm.rotateAngleZ = -0.12F;
                break;
            case SPEAK:
                // His legs hang; his hands are never still.
                dangle(t);
                bipedRightArm.rotateAngleX = -0.8F + MathHelper.sin(t * 0.11F) * 0.45F;
                bipedRightArm.rotateAngleZ = 0.2F + MathHelper.sin(t * 0.07F) * 0.15F;
                bipedLeftArm.rotateAngleX = -0.45F + MathHelper.cos(t * 0.09F) * 0.35F;
                bipedLeftArm.rotateAngleZ = -0.25F - MathHelper.cos(t * 0.06F) * 0.15F;
                bipedRightArm.rotateAngleY = bipedLeftArm.rotateAngleY = 0F;
                break;
            case PRESS: {
                // Palms on the glass ahead of him, shaking with the effort.
                float shake = MathHelper.sin(t * 2.3F) * 0.03F;
                dangle(t);
                bipedRightLeg.rotateAngleX = bipedLeftLeg.rotateAngleX = 0.25F;
                bipedBody.rotateAngleX = 0.12F;
                bipedRightArm.rotateAngleX = bipedLeftArm.rotateAngleX = -1.62F + shake;
                bipedRightArm.rotateAngleY = -0.22F;
                bipedLeftArm.rotateAngleY = 0.22F;
                bipedRightArm.rotateAngleZ = bipedLeftArm.rotateAngleZ = 0F;
                bipedHead.rotateAngleX = 0.15F + shake;
                break;
            }
            case FREE: {
                float up = Math.min(1F, since / 10F);
                dangle(t);
                bipedRightArm.rotateAngleX = bipedLeftArm.rotateAngleX = -0.2F * up;
                bipedRightArm.rotateAngleY = bipedLeftArm.rotateAngleY = 0F;
                bipedRightArm.rotateAngleZ = 2.25F * up;
                bipedLeftArm.rotateAngleZ = -2.25F * up;
                bipedHead.rotateAngleX = -0.4F * up;
                break;
            }
            case STEP:
                // Walking down out of the air, arms still half raised.
                bipedRightArm.rotateAngleZ = 0.9F;
                bipedLeftArm.rotateAngleZ = -0.9F;
                bipedHead.rotateAngleX = -0.15F;
                break;
            case FALLEN:
                // Limp; his head lolls toward whoever stands over him.
                bipedRightLeg.rotateAngleX = bipedLeftLeg.rotateAngleX = 0F;
                bipedRightLeg.rotateAngleY = bipedLeftLeg.rotateAngleY = 0F;
                bipedRightLeg.rotateAngleZ = 0.08F;
                bipedLeftLeg.rotateAngleZ = -0.12F;
                bipedRightArm.rotateAngleX = bipedLeftArm.rotateAngleX = 0F;
                bipedRightArm.rotateAngleY = bipedLeftArm.rotateAngleY = 0F;
                bipedRightArm.rotateAngleZ = 0.45F;
                bipedLeftArm.rotateAngleZ = -0.2F + MathHelper.sin(t * 0.03F) * 0.03F;
                bipedHead.rotateAngleX = 0F;
                bipedHead.rotateAngleY = MathHelper.clamp(netHeadYaw * 0.017453292F, -0.6F, 0.6F) * 0.5F;
                break;
            case WARP: {
                // Arms spread, perfectly still but for a tremor of power.
                float tremor = MathHelper.sin(t * 3.1F) * 0.02F;
                dangle(t);
                bipedRightLeg.rotateAngleX = bipedLeftLeg.rotateAngleX = 0F;
                bipedRightArm.rotateAngleX = bipedLeftArm.rotateAngleX = -0.15F + tremor;
                bipedRightArm.rotateAngleY = bipedLeftArm.rotateAngleY = 0F;
                bipedRightArm.rotateAngleZ = 1.45F + tremor;
                bipedLeftArm.rotateAngleZ = -1.45F - tremor;
                break;
            }
            case ASCEND: {
                // Risen to his full height, arms out and open, looking down his nose at you.
                float up = Math.min(1F, since / 12F);
                dangle(t);
                bipedRightArm.rotateAngleX = bipedLeftArm.rotateAngleX = -0.3F * up;
                bipedRightArm.rotateAngleY = bipedLeftArm.rotateAngleY = 0F;
                bipedRightArm.rotateAngleZ = 1.1F * up;
                bipedLeftArm.rotateAngleZ = -1.1F * up;
                bipedHead.rotateAngleX = -0.25F * up;
                break;
            }
            case TAP: {
                // One knuckle rapping on the inside of the glass, the other hand behind his back.
                float knock = Math.max(0F, MathHelper.sin(since * 0.9F)) * (since < 30F ? 1F : 0F);
                dangle(t);
                bipedRightArm.rotateAngleX = -1.45F - 0.25F * knock;
                bipedRightArm.rotateAngleY = -0.15F;
                bipedRightArm.rotateAngleZ = 0F;
                bipedLeftArm.rotateAngleX = 0.45F;
                bipedLeftArm.rotateAngleY = 0F;
                bipedLeftArm.rotateAngleZ = 0.25F;
                break;
            }
            case BECKON: {
                // One hand held out, palm up, the fingers curling him toward you, over and over.
                float curl = MathHelper.sin(since * 0.25F);
                dangle(t);
                bipedRightArm.rotateAngleX = -1.2F + 0.18F * curl;
                bipedRightArm.rotateAngleY = 0.2F;
                bipedRightArm.rotateAngleZ = 0F;
                bipedLeftArm.rotateAngleX = 0.1F;
                bipedLeftArm.rotateAngleY = 0F;
                bipedLeftArm.rotateAngleZ = -0.1F;
                bipedHead.rotateAngleX = 0.1F;
                bipedBody.rotateAngleX = 0.08F;
                break;
            }
            case OFFER:
                // Both hands forward and cupped, as if something precious lay in them, held against the glass.
                dangle(t);
                bipedRightArm.rotateAngleX = bipedLeftArm.rotateAngleX = -1.5F;
                bipedRightArm.rotateAngleY = -0.35F;
                bipedLeftArm.rotateAngleY = 0.35F;
                bipedRightArm.rotateAngleZ = bipedLeftArm.rotateAngleZ = 0F;
                bipedHead.rotateAngleX = 0.2F;
                break;
            case LAUGH: {
                // Head thrown back, shoulders shaking.
                float shake = MathHelper.sin(since * 1.6F) * 0.08F;
                dangle(t);
                bipedHead.rotateAngleX = -0.45F + shake;
                bipedRightArm.rotateAngleX = bipedLeftArm.rotateAngleX = 0.15F + shake;
                bipedRightArm.rotateAngleY = bipedLeftArm.rotateAngleY = 0F;
                bipedRightArm.rotateAngleZ = 0.35F + shake;
                bipedLeftArm.rotateAngleZ = -0.35F - shake;
                bipedBody.rotateAngleX = -0.06F + shake * 0.5F;
                break;
            }
            case ABSORBING:
                // Lying as he fell, lifting one hand against the light.
                bipedRightLeg.rotateAngleX = bipedLeftLeg.rotateAngleX = 0F;
                bipedRightArm.rotateAngleX = -2.4F;
                bipedRightArm.rotateAngleZ = 0.2F;
                bipedLeftArm.rotateAngleX = 0F;
                bipedLeftArm.rotateAngleZ = -0.2F;
                break;
            case RISE:
                dangle(t);
                bipedRightArm.rotateAngleX = bipedLeftArm.rotateAngleX = 0.1F;
                bipedRightArm.rotateAngleZ = 0.05F;
                bipedLeftArm.rotateAngleZ = -0.05F;
                bipedHead.rotateAngleX = 0.55F;
                break;
            case ADRIFT: {
                // Limp, as if floating in water: arms drifting up and out, knees a little bent, nothing held.
                float sway = MathHelper.sin(t * 0.021F), sway2 = MathHelper.cos(t * 0.017F);
                bipedRightLeg.rotateAngleX = 0.25F + sway * 0.08F;
                bipedLeftLeg.rotateAngleX = 0.15F - sway2 * 0.08F;
                bipedRightLeg.rotateAngleY = bipedLeftLeg.rotateAngleY = 0F;
                bipedRightLeg.rotateAngleZ = 0.12F;
                bipedLeftLeg.rotateAngleZ = -0.16F;
                bipedRightArm.rotateAngleX = -0.5F + sway2 * 0.12F;
                bipedLeftArm.rotateAngleX = -0.35F + sway * 0.12F;
                bipedRightArm.rotateAngleY = bipedLeftArm.rotateAngleY = 0F;
                bipedRightArm.rotateAngleZ = 1.05F + sway * 0.1F;
                bipedLeftArm.rotateAngleZ = -0.95F - sway2 * 0.1F;
                bipedHead.rotateAngleX = 0.35F + sway * 0.05F;
                bipedHead.rotateAngleY = 0F;
                break;
            }
        }
        copyModelAngles(bipedHead, bipedHeadwear);
    }

    /** Legs hanging loose in the air, swaying a little. */
    private void dangle(float t) {
        bipedRightLeg.rotateAngleX = -0.12F + MathHelper.sin(t * 0.04F) * 0.06F;
        bipedLeftLeg.rotateAngleX = -0.05F + MathHelper.sin(t * 0.04F + 1.3F) * 0.06F;
        bipedRightLeg.rotateAngleY = bipedLeftLeg.rotateAngleY = 0F;
        bipedRightLeg.rotateAngleZ = 0.04F;
        bipedLeftLeg.rotateAngleZ = -0.04F;
    }
}
