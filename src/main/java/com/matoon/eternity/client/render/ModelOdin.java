package com.matoon.eternity.client.render;

import com.matoon.eternity.entity.EntityOdin;
import com.matoon.eternity.entity.EntityOdin.Pose;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Odin on Sleipnir, as {@link OdinBoxes} lays them out. Sleipnir is a war horse bigger than any horse, with eight
 * legs: they ripple when he runs, front pair after hind pair, each pair a beat apart, which is the strangest and best
 * thing about him. Odin rides high and still on him whatever he does, cape streaming, Gungnir in his right hand.
 * <p>
 * Posed from one array of values, built each frame from his synced pose and how far into it he is, blended out of
 * whatever he was last drawn in; Sleipnir's stride and the cape and the tail come on top of that from how fast he is
 * actually going.
 */
@SideOnly(Side.CLIENT)
public class ModelOdin extends ModelBase {

    private static final float PI = (float) Math.PI;
    private final VaultBoxes.Sheet sheet = OdinBoxes.SHEET;

    // What the pose array holds.
    static final int H_PITCH = 0, H_DROP = 1, H_ROLL = 2, NECK_X = 3, NECK_Y = 4, HHEAD_X = 5, TAIL_X = 6, TAIL_Y = 7,
            FORE_LIFT = 8, FORE_FOLD = 9, HIND_BEND = 10, STRIDE = 11, R_LEAN = 12, R_TWIST = 13, R_HEAD_X = 14, R_HEAD_Y = 15,
            RA_X = 16, RA_Y = 17, RA_Z = 18, R_ELBOW = 19, LA_X = 20, LA_Y = 21, LA_Z = 22, L_ELBOW = 23, SPEAR_PITCH = 24,
            SPEAR_YAW = 25, CAPE_LIFT = 26, LOOK = 27, PAW = 28, N = 29;

    public final ModelRenderer root, neck, horseHead, hips, abdomen, chest, head, beard, beardTip, helm, rArm, rFore, lArm, lFore,
            rThigh, rShin, lThigh, lShin, gungnir;
    private final ModelRenderer[] tail = new ModelRenderer[OdinBoxes.TAIL_SEGMENTS];
    private final ModelRenderer[] cape = new ModelRenderer[OdinBoxes.CAPE_SEGMENTS];
    /** Sleipnir's legs, by LEGS index then side (0 right, 1 left): upper, lower, hoof. */
    private final ModelRenderer[][] up = new ModelRenderer[4][2], low = new ModelRenderer[4][2];
    private final float[] pose = new float[N];
    private float partial;

    /** Where each leg is set on him: across, and along (hind legs near the hip, fore legs under the chest). */
    private static final float[][] LEG_AT = {{3.6F, -21.5F}, {1.4F, -19F}, {3.4F, -2F}, {1.3F, 0.5F}};
    /** Each leg's place in the stride: hind pairs lead, fore pairs follow, every leg a little after its neighbour. */
    private static final float[][] LEG_PHASE = {{2.6F, 2.95F}, {2.78F, 3.13F}, {0F, 0.35F}, {0.18F, 0.53F}};

    public ModelOdin() {
        textureWidth = sheet.width;
        textureHeight = sheet.height;

        root = part("barrel", null, 0F, 4F, 12F);
        neck = part("neck", root, 0F, -1F, -23F);
        horseHead = part("horseHead", neck, 0F, -12F, -0.5F);
        part("earR", horseHead, -1.6F, -4F, -1.5F).rotateAngleZ = -0.25F;
        part("earL", horseHead, 1.6F, -4F, -1.5F).rotateAngleZ = 0.25F;
        ModelRenderer parent = root;
        for (int i = 0; i < tail.length; i++) {
            tail[i] = part("tail" + i, parent, 0F, i == 0 ? -3F : i == 1 ? 5F : 6F, i == 0 ? 1.5F : 0F);
            parent = tail[i];
        }
        for (int l = 0; l < 4; l++) {
            String leg = OdinBoxes.LEGS[l];
            boolean fore = leg.charAt(0) == 'F';
            for (int side = 0; side < 2; side++) {
                String s = side == 0 ? "R" : "L";
                float x = LEG_AT[l][0] * (side == 0 ? -1F : 1F);
                up[l][side] = part("up" + leg + s, root, x, 4F, LEG_AT[l][1]);
                low[l][side] = part("low" + leg + s, up[l][side], 0F, fore ? 7F : 8F, 0F);
                part("hoof" + leg + s, low[l][side], 0F, fore ? 7F : 6F, 0F);
            }
        }

        hips = part("hips", root, 0F, -7.4F, -12.5F);
        part("fauldR", hips, -4.6F, -1.4F, 0F);
        part("fauldL", hips, 4.6F, -1.4F, 0F);
        rThigh = part("thighR", hips, -2.6F, -0.5F, 0F);
        rShin = part("shinR", rThigh, 0F, 7F, 0F);
        lThigh = part("thighL", hips, 2.6F, -0.5F, 0F);
        lShin = part("shinL", lThigh, 0F, 7F, 0F);
        abdomen = part("abdomen", hips, 0F, -2F, 0F);
        chest = part("chest", abdomen, 0F, -5F, 0F);
        part("claspR", chest, -3.3F, -7F, -3.6F);
        part("claspL", chest, 3.3F, -7F, -3.6F);

        head = part("head", chest, 0F, -8.6F, 0F);
        beard = part("beard", head, 0F, 0F, -4F);
        beardTip = part("beardTip", beard, 0F, 5.5F, 0F);
        helm = part("helm", head, 0F, 0F, 0F);
        part("cheekR", helm, -4.6F, -5F, -1.7F);
        part("cheekL", helm, 4.6F, -5F, -1.7F);
        // The horns: out from the sides of the helm, then up and sweeping back, narrowing to points.
        float[][] horn = {{-1.05F, 0F}, {0.55F, -0.1F}, {0.45F, -0.2F}, {0.4F, -0.25F}};
        for (int side = 0; side < 2; side++) {
            String s = side == 0 ? "R" : "L";
            float flip = side == 0 ? 1F : -1F;
            ModelRenderer at = part("horn0" + s, helm, 4.4F * -flip, -7.4F, 0.3F);
            at.rotateAngleZ = horn[0][0] * flip;
            for (int i = 1; i < OdinBoxes.HORN_SEGMENTS; i++) {
                at = part("horn" + i + s, at, 0F, i == 1 ? -1.5F : -5F, 0F);
                at.rotateAngleZ = horn[i][0] * flip;
                at.rotateAngleX = horn[i][1];
            }
        }

        rArm = part("upperR", chest, -5.6F, -6.4F, 0F);
        part("pauldronR", rArm, 0F, 0F, 0F);
        rFore = part("foreR", rArm, -1F, 4.5F, 0F);
        gungnir = part("gungnir", rFore, 0F, 6.3F, 0F);
        lArm = part("upperL", chest, 5.6F, -6.4F, 0F);
        part("pauldronL", lArm, 0F, 0F, 0F);
        lFore = part("foreL", lArm, 1F, 4.5F, 0F);

        parent = chest;
        for (int i = 0; i < cape.length; i++) {
            cape[i] = part("cape" + i, parent, 0F, i == 0 ? -7.6F : 7F, i == 0 ? 3.4F : 0F);
            parent = cape[i];
        }
    }

    private ModelRenderer part(String name, ModelRenderer parent, float x, float y, float z) {
        return VaultParts.part(this, sheet, name, parent, x, y, z);
    }

    @Override
    public void setLivingAnimations(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partialTicks) {
        partial = partialTicks;
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
        root.render(scale);
    }

    /** Draws him again in the pose he was last given, for his glow. */
    public void renderPose(float scale) {
        root.render(scale);
    }

    /** Moves the matrix to his left hand, where he carries the gauntlet, in model space. */
    public void toLeftHand(float scale) {
        root.postRender(scale);
        hips.postRender(scale);
        abdomen.postRender(scale);
        chest.postRender(scale);
        lArm.postRender(scale);
        lFore.postRender(scale);
        GlStateManager.translate(0F, 6.5F * scale, 0F);
    }

    // ------------------------------------------------------------------ posing

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch,
                                  float scale, Entity entity) {
        if (!(entity instanceof EntityOdin)) {
            return;
        }
        EntityOdin o = (EntityOdin) entity;
        float t = o.clientPoseTicks + partial;
        java.util.Arrays.fill(pose, 0F);
        animate(o.pose(), t, ageInTicks, pose);
        float blend = smooth(t / blendTicks(o.pose()));
        if (blend < 1F) {
            for (int i = 0; i < N; i++) {
                pose[i] = o.poseFrom[i] + (pose[i] - o.poseFrom[i]) * blend;
            }
        }
        System.arraycopy(pose, 0, o.poseLast, 0, N);
        float[] p = pose.clone();
        float hurt = o.hurtTime > 0 ? (o.hurtTime - partial) / 10F : 0F;
        if (hurt > 0F) {
            hurt *= hurt;
            p[HHEAD_X] -= 0.35F * hurt;
            p[NECK_X] -= 0.2F * hurt;
            p[R_LEAN] -= 0.15F * hurt;
        }
        p[R_HEAD_Y] += MathHelper.clamp(netHeadYaw, -60F, 60F) * 0.017453292F * p[LOOK];
        float running = o.prevRunning + (o.running - o.prevRunning) * partial;
        float gait = o.prevGait + (o.gait - o.prevGait) * partial;
        float pitch = (o.prevBodyPitch + (o.bodyPitch - o.prevBodyPitch) * partial) * 0.017453292F;
        double speed = Math.sqrt(o.velX * o.velX + o.velY * o.velY + o.velZ * o.velZ);
        apply(p, running * Math.max(p[STRIDE], 0F), gait, pitch, (float) Math.min(1D, speed * 1.4D), ageInTicks, o.hasSpear());
    }

    private void apply(float[] p, float run, float gait, float pitch, float wind, float age, boolean spear) {
        // Sleipnir.
        float bob = MathHelper.sin(gait * 2F) * 0.9F * run;
        root.rotationPointY = 4F + p[H_DROP] + bob;
        root.rotateAngleX = p[H_PITCH] + pitch + MathHelper.sin(gait) * 0.07F * run;
        root.rotateAngleZ = p[H_ROLL];
        neck.rotateAngleX = 0.55F + p[NECK_X] + 0.3F * run + MathHelper.sin(gait + 0.6F) * 0.12F * run;
        neck.rotateAngleY = p[NECK_Y];
        horseHead.rotateAngleX = 0.75F + p[HHEAD_X] - 0.15F * run;
        for (int l = 0; l < 4; l++) {
            boolean fore = l < 2;
            for (int side = 0; side < 2; side++) {
                float phase = gait + LEG_PHASE[l][side];
                float swing = MathHelper.sin(phase) * (fore ? 0.95F : 0.8F) * run;
                float flex = Math.max(0F, MathHelper.sin(phase + 1.3F)) * (fore ? 1.3F : 0.9F) * run;
                float x, knee;
                if (fore) {
                    float paw = p[PAW] * MathHelper.sin(age * 0.55F + side * 1.7F + l * 0.6F);
                    x = swing - p[FORE_LIFT] + paw * 0.45F - root.rotateAngleX * Math.min(1F, p[FORE_LIFT]) * 0.3F;
                    knee = flex + p[FORE_FOLD] * (0.9F + 0.3F * paw);
                } else {
                    x = swing + p[HIND_BEND] - root.rotateAngleX * Math.min(1F, Math.abs(p[HIND_BEND]) * 2F);
                    knee = -flex - p[HIND_BEND] * 0.9F;
                }
                up[l][side].rotateAngleX = x;
                low[l][side].rotateAngleX = knee;
            }
        }
        // The tail and the mane go where the wind of his going puts them.
        float lift = 0.35F + p[TAIL_X] + 1.0F * wind;
        for (int i = 0; i < tail.length; i++) {
            tail[i].rotateAngleX = (i == 0 ? lift : 0.12F * wind) + MathHelper.sin(age * 0.2F + i * 0.9F) * (0.05F + 0.08F * wind);
            tail[i].rotateAngleY = (i == 0 ? p[TAIL_Y] : 0F) + MathHelper.sin(age * 0.13F + i) * 0.06F;
        }

        // Odin.
        hips.rotateAngleX = p[R_LEAN] - root.rotateAngleX * 0.55F;
        hips.rotateAngleY = p[R_TWIST] * 0.4F;
        abdomen.rotateAngleY = p[R_TWIST] * 0.6F;
        chest.rotateAngleX = 0F;
        head.rotateAngleX = MathHelper.clamp(p[R_HEAD_X] + root.rotateAngleX * 0.4F, -1F, 0.9F);
        head.rotateAngleY = MathHelper.clamp(p[R_HEAD_Y] - p[R_TWIST], -1F, 1F);
        beard.rotateAngleX = -head.rotateAngleX * 0.5F + 0.15F * wind + MathHelper.sin(age * 0.25F) * 0.04F;
        beardTip.rotateAngleX = 0.1F * wind;
        rThigh.rotateAngleX = -1.25F - hips.rotateAngleX * 0.3F;
        rThigh.rotateAngleZ = 0.5F;
        rShin.rotateAngleX = 1.2F;
        rShin.rotateAngleZ = -0.3F;
        lThigh.rotateAngleX = -1.25F - hips.rotateAngleX * 0.3F;
        lThigh.rotateAngleZ = -0.5F;
        lShin.rotateAngleX = 1.2F;
        lShin.rotateAngleZ = 0.3F;
        rArm.rotateAngleX = p[RA_X];
        rArm.rotateAngleY = p[RA_Y];
        rArm.rotateAngleZ = p[RA_Z];
        rFore.rotateAngleX = Math.min(0F, p[R_ELBOW]);
        lArm.rotateAngleX = p[LA_X];
        lArm.rotateAngleY = p[LA_Y];
        lArm.rotateAngleZ = p[LA_Z];
        lFore.rotateAngleX = Math.min(0F, p[L_ELBOW]);
        // Gungnir is held at the pitch the pose gives it in the world, whatever the arm and the horse are doing.
        float carried = root.rotateAngleX + hips.rotateAngleX + rArm.rotateAngleX + rFore.rotateAngleX;
        gungnir.rotateAngleX = p[SPEAR_PITCH] - carried;
        gungnir.rotateAngleY = p[SPEAR_YAW];
        gungnir.showModel = spear;
        // The cape streams out behind him the faster he goes, and stirs even when he is still.
        float capeLift = p[CAPE_LIFT] + 0.75F * wind;
        for (int i = 0; i < cape.length; i++) {
            float base = i == 0 ? 0.12F - hips.rotateAngleX - root.rotateAngleX * 0.5F : 0F;
            cape[i].rotateAngleX = base + capeLift * (i == 0 ? 0.55F : 0.22F)
                    + MathHelper.sin(age * (0.18F + 0.25F * wind) + i * 0.9F) * (0.05F + 0.1F * wind);
        }
    }

    private static float blendTicks(Pose pose) {
        return pose == Pose.STOMP || pose == Pose.THROW ? 3F : 7F;
    }

    /** The pose for each of his, t ticks into it. */
    static void animate(Pose pose, float t, float age, float[] p) {
        switch (pose) {
            case STAND:
                stand(p, t, age);
                break;
            case GALLOP:
                gallop(p, age, false);
                break;
            case CHARGE:
                gallop(p, age, true);
                break;
            case DESCEND:
                descend(p, age);
                break;
            case REAR:
                rear(p, t, age);
                break;
            case STOMP:
                stomp(p, t, age);
                break;
            case THROW:
                throwing(p, t, age);
                break;
            case CATCH:
                caught(p, t, age);
                break;
            case CALL:
                call(p, t, age);
                break;
            case SLAM:
                slam(p, t, age);
                break;
            case POINT:
                point(p, t, age);
                break;
            case CRADLE:
                cradle(p, age);
                break;
        }
    }

    // ------------------------------------------------------------------ the poses themselves

    /** Odin's right hand with Gungnir grounded at his side, the left on the reins. */
    private static void rider(float[] p, float age) {
        float breath = MathHelper.sin(age * 0.06F);
        p[R_LEAN] = -0.04F + breath * 0.015F;
        p[RA_X] = -0.25F;
        p[RA_Z] = 0.18F;
        p[R_ELBOW] = -0.7F;
        p[SPEAR_PITCH] = 0.06F;
        p[LA_X] = -0.65F;
        p[LA_Z] = -0.1F;
        p[L_ELBOW] = -0.85F;
        p[R_HEAD_X] = -0.05F;
        p[LOOK] = 1F;
    }

    /** Sleipnir standing, shifting, tossing his head now and then; Odin still. */
    private static void stand(float[] p, float t, float age) {
        rider(p, age);
        float breath = MathHelper.sin(age * 0.07F);
        p[H_DROP] = breath * 0.25F;
        p[NECK_X] = MathHelper.sin(age * 0.05F) * 0.06F;
        p[NECK_Y] = MathHelper.sin(age * 0.031F) * 0.12F;
        float toss = Math.max(0F, MathHelper.sin(age * 0.045F) - 0.85F) * 4F;
        p[HHEAD_X] = -0.3F * toss;
        p[TAIL_Y] = MathHelper.sin(age * 0.09F) * 0.3F;
        p[STRIDE] = 1F;
    }

    /** At a run: Gungnir held up and a little forward, or levelled like a lance, Odin leaning into it. */
    private static void gallop(float[] p, float age, boolean lance) {
        rider(p, age);
        p[STRIDE] = 1F;
        p[R_LEAN] = lance ? 0.38F : 0.18F;
        p[LA_X] = -0.95F;
        p[L_ELBOW] = -0.55F;
        p[CAPE_LIFT] = 0.5F;
        if (lance) {
            p[RA_X] = -0.45F;
            p[RA_Y] = -0.15F;
            p[RA_Z] = 0.1F;
            p[R_ELBOW] = -1.15F;
            p[SPEAR_PITCH] = 1.45F;
            p[SPEAR_YAW] = 0.08F;
            p[R_HEAD_X] = -0.15F;
        } else {
            p[RA_X] = -0.4F;
            p[R_ELBOW] = -1.1F;
            p[SPEAR_PITCH] = 0.28F;
        }
    }

    /** Down the Bifrost at a run, nose down, Gungnir held high. */
    private static void descend(float[] p, float age) {
        gallop(p, age, false);
        p[H_PITCH] = 0.15F;
        p[R_LEAN] = -0.25F;
        p[RA_X] = -2.55F;
        p[RA_Z] = 0.15F;
        p[R_ELBOW] = -0.25F;
        p[SPEAR_PITCH] = -0.15F;
        p[CAPE_LIFT] = 1.4F;
        p[R_HEAD_X] = 0.2F;
    }

    /** Up on his hind legs, forelegs striking at the air; Odin high in the saddle with Gungnir raised. */
    private static void rear(float[] p, float t, float age) {
        rider(p, age);
        float up = outBack(clamp(t / 10F));
        p[H_PITCH] = -0.95F * up;
        p[H_DROP] = -1.5F * up;
        p[FORE_LIFT] = 0.45F * up;
        p[FORE_FOLD] = 1.25F * up;
        p[PAW] = up;
        p[HIND_BEND] = 0.35F * up;
        p[NECK_X] = -0.55F * up;
        p[HHEAD_X] = -0.35F * up;
        p[TAIL_X] = 0.6F * up;
        p[R_LEAN] = 0.5F * up;
        p[RA_X] = -0.25F - 2.4F * up;
        p[RA_Z] = 0.18F + 0.1F * up;
        p[R_ELBOW] = -0.7F + 0.45F * up;
        p[SPEAR_PITCH] = -0.25F * up;
        p[CAPE_LIFT] = 0.6F * up;
        p[R_HEAD_X] = -0.2F * up;
    }

    /** Out of the rear, down: all eight hooves at once, and the ground goes. */
    private static void stomp(float[] p, float t, float age) {
        rear(p, 30F, age);
        float down = smooth(t / EntityOdin.STOMP_HIT);
        float settle = smooth((t - EntityOdin.STOMP_HIT - 2F) / 10F);
        float jolt = t > EntityOdin.STOMP_HIT ? (float) Math.exp(-(t - EntityOdin.STOMP_HIT) * 0.4F) : 0F;
        float k = down * (1F - 0F);
        p[H_PITCH] = -0.95F * (1F - k) + 0.12F * jolt;
        p[H_DROP] = -1.5F * (1F - k) + 1.2F * jolt;
        p[FORE_LIFT] = 0.45F * (1F - k) - 0.25F * jolt * (1F - settle);
        p[FORE_FOLD] = 1.25F * (1F - k);
        p[PAW] = 1F - k;
        p[HIND_BEND] = 0.35F * (1F - k) + 0.2F * jolt;
        p[NECK_X] = -0.55F * (1F - k) + 0.35F * jolt;
        p[HHEAD_X] = -0.35F * (1F - k);
        p[R_LEAN] = 0.5F * (1F - k) + 0.25F * jolt;
        p[RA_X] = -2.65F * (1F - k) - 0.25F * k;
        p[R_ELBOW] = -0.25F * (1F - k) - 0.7F * k;
        p[SPEAR_PITCH] = -0.25F * (1F - k) + 0.06F * k;
    }

    /** Gungnir drawn back over his shoulder, the body turned with it; then the throw, and the hand empty. */
    private static void throwing(float[] p, float t, float age) {
        rider(p, age);
        float back = smooth(t / (EntityOdin.THROW_RELEASE - 2F));
        float whip = strike(t, EntityOdin.THROW_RELEASE - 2F, EntityOdin.THROW_RELEASE, EntityOdin.THROW_TICKS);
        float after = smooth((t - EntityOdin.THROW_RELEASE) / 10F);
        float drawn = back * (1F - after);
        p[R_TWIST] = 0.55F * drawn - 0.45F * whip;
        p[R_LEAN] = -0.15F * drawn + 0.35F * whip;
        p[RA_X] = -0.25F - 2.55F * drawn - 1.1F * whip * (1F - drawn);
        p[RA_Z] = 0.18F + 0.25F * drawn;
        p[R_ELBOW] = -0.7F - 0.6F * drawn + 0.6F * whip;
        p[SPEAR_PITCH] = 0.06F + 1.45F * back;
        p[LA_X] = -0.65F - 0.7F * drawn;
        p[LA_Y] = 0.4F * drawn;
        p[STRIDE] = 1F;
        p[R_HEAD_X] = -0.1F;
    }

    /** Gungnir slapping back into his raised hand. */
    private static void caught(float[] p, float t, float age) {
        rider(p, age);
        float up = 1F - smooth((t - 2F) / 10F);
        p[RA_X] = -0.25F - 2F * up;
        p[R_ELBOW] = -0.7F + 0.4F * up;
        p[SPEAR_PITCH] = 0.06F + 0.3F * up;
        p[STRIDE] = 1F;
    }

    /** Gungnir raised to the sky, his face turned up after it; Sleipnir half rearing. */
    private static void call(float[] p, float t, float age) {
        rider(p, age);
        float up = outCubic(clamp(t / 10F));
        float tremor = MathHelper.sin(age * 2.7F) * 0.015F * up;
        p[RA_X] = -0.25F - 2.8F * up + tremor;
        p[RA_Z] = 0.18F - 0.06F * up;
        p[R_ELBOW] = -0.7F + 0.62F * up;
        p[SPEAR_PITCH] = 0.06F - 0.06F * up;
        p[R_HEAD_X] = -0.45F * up;
        p[LOOK] = 1F - up;
        p[H_PITCH] = -0.22F * up;
        p[FORE_LIFT] = 0.12F * up;
        p[FORE_FOLD] = 0.5F * up;
        p[PAW] = 0.3F * up;
        p[NECK_X] = -0.25F * up;
        p[TAIL_X] = 0.3F * up;
        p[CAPE_LIFT] = 0.3F * up;
    }

    /** From high overhead, Gungnir rammed point-down into the ground beside him; Sleipnir braced under the blow. */
    private static void slam(float[] p, float t, float age) {
        call(p, 30F, age);
        float down = strike(t, 0F, EntityOdin.SLAM_HIT, EntityOdin.SLAM_TICKS);
        float k = smooth(t / EntityOdin.SLAM_HIT);
        p[RA_X] = -3.05F * (1F - k) - 0.35F * k;
        p[RA_Z] = 0.12F + 0.25F * k;
        p[R_ELBOW] = -0.08F * (1F - k);
        p[SPEAR_PITCH] = PI * k;
        p[R_LEAN] = 0.45F * down;
        p[R_HEAD_X] = -0.45F * (1F - k) + 0.25F * down;
        p[LOOK] = k;
        p[H_PITCH] = -0.22F * (1F - k) + 0.06F * down;
        p[H_DROP] = 1.6F * down;
        p[FORE_LIFT] = 0.12F * (1F - k);
        p[FORE_FOLD] = 0.5F * (1F - k);
        p[HIND_BEND] = 0.2F * down;
    }

    /** Gungnir levelled at someone, the arm straight out along it. */
    private static void point(float[] p, float t, float age) {
        rider(p, age);
        float out = outCubic(clamp(t / 8F));
        p[RA_X] = -0.25F - 1.3F * out;
        p[RA_Y] = -0.1F * out;
        p[RA_Z] = 0.18F - 0.18F * out;
        p[R_ELBOW] = -0.7F + 0.65F * out;
        p[SPEAR_PITCH] = 0.06F + 1.5F * out;
        p[R_TWIST] = -0.2F * out;
        p[R_LEAN] = 0.1F * out;
        p[STRIDE] = 1F;
    }

    /** The gauntlet held against him in the crook of his left arm. */
    private static void cradle(float[] p, float age) {
        stand(p, 0F, age);
        p[LA_X] = -1.15F;
        p[LA_Y] = 0.55F;
        p[LA_Z] = 0.15F;
        p[L_ELBOW] = -1.3F;
        p[R_HEAD_X] = 0.15F;
    }

    // ------------------------------------------------------------------ easing

    static float clamp(float x) {
        return VaultParts.clamp(x);
    }

    static float smooth(float x) {
        return VaultParts.smooth(x);
    }

    static float outCubic(float x) {
        return VaultParts.outCubic(x);
    }

    static float outBack(float x) {
        return VaultParts.outBack(x);
    }

    static float strike(float t, float start, float contact, float end) {
        return VaultParts.strike(t, start, contact, end);
    }
}
