package com.matoon.eternity.client.render;

import com.matoon.eternity.entity.EntityEternity;
import com.matoon.eternity.entity.EntityEternity.State;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A gaunt horned giant. Arms long enough to brush his knees, fingers like
 * splinters, legs that taper away to nothing. No texture: every surface is
 * rendered as open space by {@link Cosmic}.
 */
@SideOnly(Side.CLIENT)
public class ModelEternity extends ModelBase {

    private static final float DEG = (float) Math.PI / 180F;

    private final ModelRenderer head;
    private final ModelRenderer eyes;
    private final ModelRenderer neck;
    private final ModelRenderer chest;
    private final ModelRenderer waist;
    private final ModelRenderer[] arms = new ModelRenderer[2];
    private final ModelRenderer[] forearms = new ModelRenderer[2];
    private final ModelRenderer[] hands = new ModelRenderer[2];
    private final ModelRenderer[][] fingers = new ModelRenderer[2][4];
    private final ModelRenderer[] thighs = new ModelRenderer[2];
    private final ModelRenderer[] shins = new ModelRenderer[2];
    private final ModelRenderer[] hornBase = new ModelRenderer[2];
    private final ModelRenderer[] hornMid = new ModelRenderer[2];
    private final ModelRenderer[] hornTip = new ModelRenderer[2];
    private final ModelRenderer[] spurs = new ModelRenderer[2];

    public ModelEternity() {
        textureWidth = 64;
        textureHeight = 64;

        head = part(0F, -12F, 0F);
        head.addBox(-4F, -9F, -4F, 8, 9, 8);
        head.addBox(-3F, -2F, -4.6F, 6, 3, 1);
        head.addBox(-4.5F, -9.5F, -1F, 9, 2, 3);

        eyes = new ModelRenderer(this, 0, 0);
        eyes.addBox(-3.2F, -5.6F, -4.25F, 2, 1, 1);
        eyes.addBox(1.2F, -5.6F, -4.25F, 2, 1, 1);

        for (int i = 0; i < 2; i++) {
            float s = i == 0 ? 1F : -1F;
            hornBase[i] = part(s * 3.4F, -8F, 0.5F);
            hornBase[i].addBox(-1.5F, -4F, -1.5F, 3, 4, 3);
            hornMid[i] = part(0F, -3.6F, 0F);
            hornMid[i].addBox(-1F, -5F, -1F, 2, 5, 2);
            hornTip[i] = part(0F, -4.6F, 0F);
            hornTip[i].addBox(-0.5F, -7F, -0.5F, 1, 7, 1);
            hornMid[i].addChild(hornTip[i]);
            hornBase[i].addChild(hornMid[i]);
            head.addChild(hornBase[i]);

            spurs[i] = part(s * 1.8F, -9F, -2.5F);
            spurs[i].addBox(-0.5F, -3F, -0.5F, 1, 3, 1);
            head.addChild(spurs[i]);
        }

        neck = part(0F, -10F, 0F);
        neck.addBox(-1.5F, -3F, -1.5F, 3, 4, 3);

        chest = part(0F, -2F, 0F);
        chest.addBox(-5F, -8F, -2.5F, 10, 8, 5);
        chest.addBox(-6F, -8.5F, -2F, 12, 3, 4);

        waist = part(0F, 4F, 0F);
        waist.addBox(-3.5F, -6F, -2F, 7, 6, 4);

        for (int i = 0; i < 2; i++) {
            float s = i == 0 ? 1F : -1F;
            arms[i] = part(s * 6.5F, -9F, 0F);
            arms[i].addBox(-1.5F, -1F, -1.5F, 3, 12, 3);
            forearms[i] = part(0F, 11F, 0F);
            forearms[i].addBox(-1F, 0F, -1F, 2, 12, 2, 0.2F);
            hands[i] = part(0F, 12F, 0F);
            hands[i].addBox(-1F, 0F, -1.5F, 2, 2, 3);
            for (int f = 0; f < 4; f++) {
                fingers[i][f] = part(0F, 1.6F, -1.2F + f * 0.8F);
                fingers[i][f].addBox(-0.4F, 0F, -0.4F, 1, f == 0 ? 4 : 6, 1, -0.15F);
                hands[i].addChild(fingers[i][f]);
            }
            forearms[i].addChild(hands[i]);
            arms[i].addChild(forearms[i]);

            thighs[i] = part(s * 2.2F, 4F, 0F);
            thighs[i].addBox(-2F, 0F, -2F, 4, 10, 4);
            shins[i] = part(0F, 10F, 0F);
            shins[i].addBox(-1.5F, 0F, -1.5F, 3, 10, 3, -0.3F);
            ModelRenderer foot = part(0F, 10F, 0F);
            foot.addBox(-1F, -0.5F, -3F, 2, 2, 4, -0.35F);
            shins[i].addChild(foot);
            thighs[i].addChild(shins[i]);
        }
    }

    private ModelRenderer part(float x, float y, float z) {
        ModelRenderer part = new ModelRenderer(this, 0, 0);
        part.setRotationPoint(x, y, z);
        return part;
    }

    /** Poses the figure. {@code stateTicks} counts up from the last state change. */
    public void pose(float age, State state, int stateTicks, float headYaw, float headPitch) {
        float breathe = MathHelper.sin(age * 0.035F);

        head.rotateAngleY = headYaw * DEG;
        head.rotateAngleX = headPitch * DEG;
        head.rotateAngleZ = MathHelper.sin(age * 0.021F) * 0.16F;
        neck.rotateAngleY = head.rotateAngleY * 0.4F;
        chest.rotateAngleX = 0.1F + breathe * 0.02F;
        waist.rotateAngleX = 0.04F;

        for (int i = 0; i < 2; i++) {
            float s = i == 0 ? 1F : -1F;
            arms[i].rotateAngleX = -0.1F + MathHelper.sin(age * 0.03F + i * 2.1F) * 0.07F;
            arms[i].rotateAngleY = 0F;
            arms[i].rotateAngleZ = -s * (0.09F + MathHelper.sin(age * 0.027F + s) * 0.03F);
            forearms[i].rotateAngleX = -0.22F - MathHelper.sin(age * 0.04F + i) * 0.08F;
            forearms[i].rotateAngleZ = 0F;
            hands[i].rotateAngleX = 0.1F;
            for (int f = 0; f < 4; f++) {
                fingers[i][f].rotateAngleX = -0.25F - f * 0.08F + MathHelper.sin(age * 0.11F + f * 1.7F + i) * 0.09F;
                fingers[i][f].rotateAngleZ = (f - 1.5F) * 0.06F * s;
            }
            thighs[i].rotateAngleX = 0.1F + MathHelper.sin(age * 0.018F + i * 3F) * 0.05F;
            thighs[i].rotateAngleZ = -s * 0.05F;
            shins[i].rotateAngleX = 0.25F;

            hornBase[i].rotateAngleZ = -s * 0.65F;
            hornBase[i].rotateAngleX = -0.25F;
            hornMid[i].rotateAngleZ = s * 0.5F;
            hornMid[i].rotateAngleX = -0.55F;
            hornTip[i].rotateAngleZ = s * 0.3F;
            hornTip[i].rotateAngleX = -0.7F;
            spurs[i].rotateAngleX = -0.9F;
            spurs[i].rotateAngleZ = -s * 0.2F;
        }

        switch (state) {
            case STARE: {
                // A slow, wrong tilt of the head, then perfect stillness.
                float p = Math.min(1F, stateTicks / 30F);
                head.rotateAngleZ = 0.5F * p + MathHelper.sin(age * 1.9F) * 0.015F;
                for (int i = 0; i < 2; i++) {
                    arms[i].rotateAngleX = -0.05F;
                    for (int f = 0; f < 4; f++) {
                        fingers[i][f].rotateAngleX = -0.1F - p * 0.5F;
                    }
                }
                break;
            }
            case CHANNEL: {
                float p = Math.min(1F, stateTicks / 8F);
                arms[1].rotateAngleX = -1.45F * p;
                arms[1].rotateAngleY = 0.15F * p;
                forearms[1].rotateAngleX = -0.1F;
                arms[0].rotateAngleX = -0.5F * p;
                arms[0].rotateAngleZ = -0.6F * p;
                for (int f = 0; f < 4; f++) {
                    fingers[1][f].rotateAngleX = -0.05F;
                    fingers[1][f].rotateAngleZ = (f - 1.5F) * 0.3F * p;
                }
                break;
            }
            case EMERGE:
            case DEPART: {
                float p = state == State.EMERGE ? 1F - Math.min(1F, stateTicks / 60F) : Math.min(1F, stateTicks / 40F);
                head.rotateAngleX += 0.7F * p;
                for (int i = 0; i < 2; i++) {
                    float s = i == 0 ? 1F : -1F;
                    arms[i].rotateAngleX = -0.1F - 0.9F * p;
                    arms[i].rotateAngleZ = s * 0.35F * p;
                    forearms[i].rotateAngleX = -1.3F * p;
                    thighs[i].rotateAngleX = 0.1F + 0.5F * p;
                    shins[i].rotateAngleX = 0.25F + 0.6F * p;
                }
                break;
            }
            case DYING: {
                float shake = MathHelper.sin(age * 3.1F) * 0.06F;
                float p = Math.min(1F, stateTicks / 200F);
                head.rotateAngleX = -0.7F * p + shake;
                head.rotateAngleZ = shake;
                chest.rotateAngleX = -0.25F * p + shake * 0.5F;
                for (int i = 0; i < 2; i++) {
                    float s = i == 0 ? 1F : -1F;
                    arms[i].rotateAngleZ = -s * (0.2F + 1.25F * p) + shake;
                    arms[i].rotateAngleX = -0.3F * p;
                    forearms[i].rotateAngleX = -0.15F;
                    for (int f = 0; f < 4; f++) {
                        fingers[i][f].rotateAngleX = 0.1F;
                        fingers[i][f].rotateAngleZ = (f - 1.5F) * 0.35F * s;
                    }
                }
                break;
            }
            default:
                break;
        }
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float age, float headYaw, float headPitch,
                                  float scale, Entity entity) {
        if (entity instanceof EntityEternity) {
            EntityEternity eternity = (EntityEternity) entity;
            pose(age, eternity.getState(), eternity.clientStateTicks, headYaw, headPitch);
        } else {
            pose(age, State.DRIFT, 0, headYaw, headPitch);
        }
    }

    /** Everything except the eyes. */
    public void renderBody(float scale) {
        head.render(scale);
        neck.render(scale);
        chest.render(scale);
        waist.render(scale);
        for (int i = 0; i < 2; i++) {
            arms[i].render(scale);
            thighs[i].render(scale);
        }
    }

    public void renderEyes(float scale) {
        net.minecraft.client.renderer.GlStateManager.pushMatrix();
        head.postRender(scale);
        eyes.render(scale);
        net.minecraft.client.renderer.GlStateManager.popMatrix();
    }
}
