package com.matoon.eternity.client.render;

import com.matoon.eternity.entity.EntityMagus;
import com.matoon.eternity.entity.EntityMagus.Pose;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import static com.matoon.eternity.client.render.ModelAdamWarlock.*;

/**
 * The Magus, jointed exactly as Adam Warlock is (he is what Warlock grows into, and the
 * likeness has to read), with none of Warlock's armour: bare arms and legs, a tunic whose
 * shoulders flare up to points, silver wristbands, an ornate belt and buckle, silver boots to
 * the knee with flared tops, white hair swept high off a widow's peak, and the gem at his
 * brow. Skinned in a 128x128 layout of its own (see tools/MagusSkin.java), which this file's
 * box offsets must match.
 * <p>
 * Posed the way Warlock is, from the same array, built from scratch each frame out of his
 * synced pose and how far into it he is, and blended out of whatever he was last drawn in.
 * Where Warlock is all anticipation and follow-through, the Magus barely moves until he
 * does: long holds, then a blow that is over before it has started.
 */
@SideOnly(Side.CLIENT)
public class ModelMagus extends ModelBase {

    private static final float PI = (float) Math.PI;

    public final ModelRenderer root, abdomen, chest, head, rArm, rFore, lArm, lFore, rLeg, rShin, lLeg, lShin;
    private final float[] pose = new float[N];
    private float partial;

    public ModelMagus() {
        textureWidth = 128;
        textureHeight = 128;

        root = new ModelRenderer(this);
        root.setRotationPoint(0F, 12F, 0F);

        abdomen = box(root, 28, 16, -4F, -5F, -2F, 8, 5, 4);
        ModelRenderer belt = box(abdomen, 52, 16, -4.5F, -1.5F, -2.5F, 9, 2, 5);
        box(belt, 80, 16, -1.5F, -2.2F, -3.1F, 3, 3, 1);

        chest = box(abdomen, 0, 16, -4.5F, -7F, -2.5F, 9, 7, 5);
        chest.setRotationPoint(0F, -5F, 0F);

        head = box(chest, 0, 0, -4F, -8F, -4F, 8, 8, 8);
        head.setRotationPoint(0F, -7F, -0.2F);
        box(head, 32, 0, -4.5F, -9F, -4.5F, 9, 2, 9);
        // Swept up off the forehead, high, and back.
        ModelRenderer quiff = box(head, 68, 0, -4F, -3.4F, -4.4F, 8, 4, 5);
        quiff.setRotationPoint(0F, -8.4F, -0.2F);
        quiff.rotateAngleX = -0.42F;
        box(head, 94, 0, -4.5F, -8.8F, 3F, 9, 6, 2);
        box(head, 94, 8, -4.6F, -8.4F, -2F, 1, 4, 6);
        box(head, 108, 8, 3.6F, -8.4F, -2F, 1, 4, 6);
        box(head, 122, 0, -0.5F, -6.6F, -4.45F, 1, 1, 1);

        // The tunic's shoulders, flaring up to points: part of the tunic, so they stay with the chest.
        ModelRenderer rFlare = box(chest, 0, 44, -4F, -1F, -3F, 4, 2, 6);
        rFlare.setRotationPoint(-4.2F, -6.6F, 0F);
        rFlare.rotateAngleZ = 0.42F;
        ModelRenderer rTip = box(rFlare, 112, 44, -2F, -1.4F, -2F, 2, 2, 4);
        rTip.setRotationPoint(-3.6F, -0.2F, 0F);
        rTip.rotateAngleZ = 0.5F;
        ModelRenderer lFlare = box(chest, 20, 44, 0F, -1F, -3F, 4, 2, 6);
        lFlare.setRotationPoint(4.2F, -6.6F, 0F);
        lFlare.rotateAngleZ = -0.42F;
        ModelRenderer lTip = box(lFlare, 112, 50, 0F, -1.4F, -2F, 2, 2, 4);
        lTip.setRotationPoint(3.6F, -0.2F, 0F);
        lTip.rotateAngleZ = -0.5F;

        rArm = box(chest, 0, 32, -3F, -1.5F, -2F, 4, 6, 4);
        rArm.setRotationPoint(-5.5F, -5.5F, 0F);
        rFore = box(rArm, 32, 32, -2F, 0F, -2F, 4, 7, 4);
        rFore.setRotationPoint(-1F, 4.5F, 0F);
        box(rFore, 64, 32, -2.5F, 3.4F, -2.5F, 5, 2, 5);

        lArm = box(chest, 16, 32, -1F, -1.5F, -2F, 4, 6, 4);
        lArm.setRotationPoint(5.5F, -5.5F, 0F);
        lFore = box(lArm, 48, 32, -2F, 0F, -2F, 4, 7, 4);
        lFore.setRotationPoint(1F, 4.5F, 0F);
        box(lFore, 84, 32, -2.5F, 3.4F, -2.5F, 5, 2, 5);

        rLeg = box(root, 48, 44, -2F, 0F, -2F, 4, 6, 4);
        rLeg.setRotationPoint(-2F, 0F, 0F);
        rShin = box(rLeg, 80, 44, -2F, 0F, -2F, 4, 6, 4);
        rShin.setRotationPoint(0F, 6F, 0F);
        box(rShin, 0, 56, -2.5F, 0F, -2.5F, 5, 6, 5);
        box(rShin, 40, 56, -3F, -1F, -3F, 6, 2, 6);
        box(rShin, 88, 56, -2F, -2.4F, -3.3F, 4, 3, 1);

        lLeg = box(root, 64, 44, -2F, 0F, -2F, 4, 6, 4);
        lLeg.setRotationPoint(2F, 0F, 0F);
        lShin = box(lLeg, 96, 44, -2F, 0F, -2F, 4, 6, 4);
        lShin.setRotationPoint(0F, 6F, 0F);
        box(lShin, 20, 56, -2.5F, 0F, -2.5F, 5, 6, 5);
        box(lShin, 64, 56, -3F, -1F, -3F, 6, 2, 6);
        box(lShin, 98, 56, -2F, -2.4F, -3.3F, 4, 3, 1);
    }

    private ModelRenderer box(ModelRenderer parent, int u, int v, float x, float y, float z, int w, int h, int d) {
        ModelRenderer part = new ModelRenderer(this, u, v);
        part.addBox(x, y, z, w, h, d);
        parent.addChild(part);
        return part;
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

    /** Draws him again in the pose he was last given, for his glow, his aura and his afterimages. */
    public void renderPose(float scale) {
        root.render(scale);
    }

    /** Moves the matrix to the palm of one hand, in model space. */
    public void toHand(boolean right, float scale) {
        root.postRender(scale);
        abdomen.postRender(scale);
        chest.postRender(scale);
        (right ? rArm : lArm).postRender(scale);
        (right ? rFore : lFore).postRender(scale);
        GlStateManager.translate(0F, 7.2F * scale, 0F);
    }

    // ------------------------------------------------------------------ posing

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch,
                                  float scale, Entity entity) {
        if (!(entity instanceof EntityMagus)) {
            return;
        }
        EntityMagus m = (EntityMagus) entity;
        float t = m.clientPoseTicks + partial;
        float pitch = headPitch * 0.017453292F;
        java.util.Arrays.fill(pose, 0F);
        animate(m.pose(), t, ageInTicks, pitch, pose);

        float blend = smooth(t / blendTicks(m.pose()));
        if (blend < 1F) {
            for (int i = 0; i < N; i++) {
                pose[i] = m.poseFrom[i] + (pose[i] - m.poseFrom[i]) * blend;
            }
        }
        System.arraycopy(pose, 0, m.poseLast, 0, N);

        float[] p = pose.clone();
        float hurt = m.hurtTime > 0 ? (m.hurtTime - partial) / 10F : 0F;
        if (hurt > 0F) {
            hurt *= hurt;
            p[CHEST_X] -= 0.15F * hurt;
            p[HEAD_X] -= 0.2F * hurt;
        }
        p[HEAD_Y] += netHeadYaw * 0.017453292F * p[LOOK];
        p[HEAD_X] += pitch * p[LOOK];
        apply(p);
    }

    private void apply(float[] p) {
        root.rotationPointY = 12F + p[DROP];
        root.rotateAngleX = p[LEAN];
        root.rotateAngleY = p[TWIST];
        root.rotateAngleZ = p[TILT];
        abdomen.rotateAngleX = p[WAIST_X];
        abdomen.rotateAngleY = p[WAIST_Y];
        chest.rotateAngleX = p[CHEST_X];
        chest.rotateAngleY = p[CHEST_Y];
        chest.rotateAngleZ = p[CHEST_Z];
        head.rotateAngleX = MathHelper.clamp(p[HEAD_X], -1.4F, 1.1F);
        head.rotateAngleY = MathHelper.clamp(p[HEAD_Y], -1.2F, 1.2F);
        head.rotateAngleZ = p[HEAD_Z];
        rArm.rotateAngleX = p[RA_X];
        rArm.rotateAngleY = p[RA_Y];
        rArm.rotateAngleZ = p[RA_Z];
        rFore.rotateAngleX = Math.min(0F, p[R_ELBOW]);
        lArm.rotateAngleX = p[LA_X];
        lArm.rotateAngleY = p[LA_Y];
        lArm.rotateAngleZ = p[LA_Z];
        lFore.rotateAngleX = Math.min(0F, p[L_ELBOW]);
        rLeg.rotateAngleX = p[RL_X];
        rLeg.rotateAngleY = p[RL_Y];
        rLeg.rotateAngleZ = p[RL_Z];
        rShin.rotateAngleX = Math.max(0F, p[R_KNEE]);
        lLeg.rotateAngleX = p[LL_X];
        lLeg.rotateAngleY = p[LL_Y];
        lLeg.rotateAngleZ = p[LL_Z];
        lShin.rotateAngleX = Math.max(0F, p[L_KNEE]);
    }

    private static float blendTicks(Pose pose) {
        switch (pose) {
            case PALM:
            case BACKHAND:
            case KNEE:
            case CATCH:
            case STAGGER:
            case FLUNG:
                return 2F;
            case HAMMER:
            case BLAST:
            case BLINK_OUT:
            case BLINK_IN:
                return 3F;
            case SLEEP:
            case STIR:
                return 14F;
            case FLOAT:
            case DESCEND:
            case REGARD:
                return 10F;
            default:
                return 6F;
        }
    }

    /** The pose for each of his, t ticks into it. */
    static void animate(Pose pose, float t, float age, float pitch, float[] p) {
        switch (pose) {
            case FLOAT:
                hang(p, age);
                break;
            case STAND:
                stand(p, age);
                break;
            case REGARD:
                regard(p, age);
                break;
            case POINT:
                point(p, t, age, pitch);
                break;
            case CRUSH:
                crush(p, t, age, pitch);
                break;
            case GUARD:
                guard(p, age);
                break;
            case BLOCK:
                block(p, t, age, pitch);
                break;
            case STAGGER:
                stagger(p, t, age);
                break;
            case FLUNG:
                flung(p, age);
                break;
            case CATCH:
                caught(p, t, age);
                break;
            case PALM:
                palm(p, t, age);
                break;
            case BACKHAND:
                backhand(p, t, age);
                break;
            case KNEE:
                knee(p, t, age);
                break;
            case HAMMER:
                hammer(p, t, age);
                break;
            case BLAST:
                blast(p, t, age, pitch);
                break;
            case GRIP:
                grip(p, t, age);
                break;
            case TAKE:
                take(p, t, age);
                break;
            case VANISH:
            case APPEAR:
            case BLINK_OUT:
            case BLINK_IN:
                fold(p, age);
                break;
            case DESCEND:
                descend(p, age);
                break;
            case SLEEP:
                sleep(p, age);
                break;
            case STIR:
                stir(p, t, age);
                break;
            case CARRY:
                carry(p, age);
                break;
        }
    }

    // ------------------------------------------------------------------ the poses themselves

    /** In the air at ease: chest out, chin up, the arms a little out from his sides, legs together, toes down. */
    private static void hang(float[] p, float age) {
        float breath = MathHelper.sin(age * 0.07F);
        p[DROP] = breath * 0.5F;
        p[CHEST_X] = -0.08F + MathHelper.sin(age * 0.07F + 0.6F) * 0.02F;
        p[HEAD_X] = -0.1F;
        p[RA_X] = 0.04F + breath * 0.02F;
        p[RA_Z] = 0.16F;
        p[R_ELBOW] = -0.18F;
        p[LA_X] = 0.04F - breath * 0.02F;
        p[LA_Z] = -0.16F;
        p[L_ELBOW] = -0.18F;
        p[RL_X] = 0.06F;
        p[R_KNEE] = 0.1F;
        p[RL_Z] = 0.02F;
        p[LL_X] = 0.16F;
        p[L_KNEE] = 0.24F;
        p[LL_Z] = -0.02F;
        p[LOOK] = 1F;
    }

    /** Fists on his hips, elbows out, feet apart, chin up: the way he means to be painted. */
    private static void stand(float[] p, float age) {
        float breath = MathHelper.sin(age * 0.06F);
        p[CHEST_X] = -0.1F + breath * 0.02F;
        p[HEAD_X] = -0.12F;
        hips(p);
        p[RL_Z] = 0.14F;
        p[LL_Z] = -0.14F;
        p[RL_Y] = 0.1F;
        p[LL_Y] = -0.1F;
        p[LOOK] = 1F;
    }

    /** Both fists set on the hips, elbows thrown wide. */
    private static void hips(float[] p) {
        p[RA_X] = 0.2F;
        p[RA_Y] = 0.35F;
        p[RA_Z] = 0.85F;
        p[R_ELBOW] = -1.75F;
        p[LA_X] = 0.2F;
        p[LA_Y] = -0.35F;
        p[LA_Z] = -0.85F;
        p[L_ELBOW] = -1.75F;
    }

    /** Hands clasped behind his back, chin down, looking along his nose at whoever is in front of him. */
    private static void regard(float[] p, float age) {
        hang(p, age);
        p[RA_X] = 0.55F;
        p[RA_Y] = 0.5F;
        p[RA_Z] = 0.15F;
        p[R_ELBOW] = -0.9F;
        p[LA_X] = 0.55F;
        p[LA_Y] = -0.5F;
        p[LA_Z] = -0.15F;
        p[L_ELBOW] = -0.9F;
        p[CHEST_X] = -0.14F;
        p[HEAD_X] = 0.12F;
        p[HEAD_Z] = 0.06F;
    }

    /** One arm straight out at what he means, the shoulder turned into it. */
    private static void point(float[] p, float t, float age, float pitch) {
        hang(p, age);
        float out = outCubic(clamp(t / 6F));
        set(p, RA_X, -1.57F + pitch, out);
        set(p, RA_Y, -0.08F, out);
        set(p, RA_Z, 0F, out);
        set(p, R_ELBOW, -0.04F, out);
        p[CHEST_Y] -= 0.25F * out;
        p[LOOK] = 1F;
    }

    /**
     * Mother's death: the hand comes up open, and holds, trembling with what is going out of
     * it; the arm rising as she rises; then the fist closes and wrenches down, and it is done,
     * and the arm falls to his side as if it had never been raised.
     */
    private static void crush(float[] p, float t, float age, float pitch) {
        hang(p, age);
        float raise = outCubic(clamp(t / 12F));
        float lift = smooth((t - 12F) / 40F);
        float close = outBack(clamp((t - EntityMagus.CRUSH_CLOSE) / 4F));
        float after = smooth((t - EntityMagus.CRUSH_CLOSE - 14F) / 20F);
        float tremor = MathHelper.sin(age * 2.9F) * 0.03F * raise * (1F - close);
        float arm = raise * (1F - after);
        set(p, RA_X, -1.5F - 0.45F * lift + 0.55F * close + pitch * 0.5F + tremor, arm);
        set(p, RA_Y, -0.1F - 0.25F * close, arm);
        set(p, RA_Z, 0.05F, arm);
        set(p, R_ELBOW, -0.1F - 0.5F * close, arm);
        p[CHEST_Y] -= (0.3F + 0.25F * close) * arm;
        p[CHEST_X] -= 0.08F * lift * arm;
        p[HEAD_X] += 0.08F * close * arm;
        p[LEAN] -= 0.05F * lift * arm;
        p[LOOK] = 1F;
    }

    /** On guard in the air: the left hand forward and open, the right drawn back, turned side-on, knees a little up. */
    private static void guard(float[] p, float age) {
        float bob = MathHelper.sin(age * 0.12F);
        p[DROP] = bob * 0.4F;
        p[CHEST_Y] = 0.3F;
        p[WAIST_Y] = 0.1F;
        p[CHEST_X] = 0.04F;
        p[LA_X] = -1.15F + bob * 0.03F;
        p[LA_Y] = 0.15F;
        p[LA_Z] = -0.05F;
        p[L_ELBOW] = -0.5F;
        p[RA_X] = -0.55F;
        p[RA_Y] = -0.3F;
        p[RA_Z] = 0.2F;
        p[R_ELBOW] = -1.6F;
        p[RL_X] = -0.35F;
        p[R_KNEE] = 0.55F;
        p[LL_X] = 0.15F;
        p[L_KNEE] = 0.35F;
        p[HEAD_X] = 0.08F;
        p[HEAD_Y] = -0.2F;
        p[LOOK] = 0.85F;
    }

    /**
     * One palm up against what is coming at him, the other arm back for balance, braced; at
     * first the arm shakes with it and he gives ground, and then the shaking stops.
     */
    private static void block(float[] p, float t, float age, float pitch) {
        hang(p, age);
        float up = outCubic(clamp(t / 4F));
        float strain = 1F - smooth((t - 30F) / 20F);
        float shake = MathHelper.sin(age * 3.3F) * 0.06F * strain;
        set(p, RA_X, -1.62F + pitch + shake, up);
        set(p, RA_Y, -0.05F, up);
        set(p, RA_Z, 0F, up);
        set(p, R_ELBOW, -0.25F - 0.25F * strain, up);
        set(p, LA_X, 0.35F, up);
        set(p, LA_Z, -0.45F, up);
        set(p, L_ELBOW, -0.4F, up);
        set(p, RL_X, -0.45F * strain, up);
        set(p, R_KNEE, 0.4F + 0.3F * strain, up);
        set(p, LL_X, 0.35F, up);
        set(p, L_KNEE, 0.35F, up);
        p[LEAN] += (-0.1F * strain + 0.06F * (1F - strain)) * up;
        p[CHEST_Y] -= 0.2F * up;
        p[HEAD_X] += (0.15F * strain - 0.05F) * up;
        p[LOOK] = 1F;
    }

    /** Struck: thrown back, arms flung, head snapped back; and at once righting himself. */
    private static void stagger(float[] p, float t, float age) {
        float hit = outCubic(clamp(t / 2F)) * (1F - smooth((t - 4F) / 12F));
        float[] back = new float[N];
        guard(back, age);
        float recover = smooth((t - 4F) / 12F);
        for (int i = 0; i < N; i++) {
            p[i] = back[i] * recover;
        }
        p[HEAD_X] += -0.8F * hit;
        p[CHEST_X] += -0.45F * hit;
        p[LEAN] += -0.35F * hit;
        p[RA_X] += -1.2F * hit;
        p[RA_Z] += 0.8F * hit;
        p[R_ELBOW] += -0.4F * hit;
        p[LA_X] += -1F * hit;
        p[LA_Z] += -0.9F * hit;
        p[L_ELBOW] += -0.4F * hit;
        p[RL_X] += -0.7F * hit;
        p[R_KNEE] += 0.9F * hit;
        p[LL_X] += -0.4F * hit;
        p[L_KNEE] += 0.7F * hit;
        p[LOOK] = recover;
    }

    /** Launched: arched back, head back, arms and legs trailing below him. */
    private static void flung(float[] p, float age) {
        float flail = MathHelper.sin(age * 0.9F) * 0.06F;
        p[CHEST_X] = -0.5F;
        p[WAIST_X] = -0.25F;
        p[HEAD_X] = -0.9F;
        p[RA_X] = 0.5F + flail;
        p[RA_Z] = 0.9F;
        p[R_ELBOW] = -0.3F;
        p[LA_X] = 0.6F - flail;
        p[LA_Z] = -0.9F;
        p[L_ELBOW] = -0.35F;
        p[RL_X] = 0.45F;
        p[R_KNEE] = 0.7F;
        p[LL_X] = 0.25F;
        p[L_KNEE] = 0.9F;
    }

    /** A fist caught in his open right hand at arm's length, and held; he leans into it, and tilts his head at it. */
    private static void caught(float[] p, float t, float age) {
        guard(p, age);
        float snap = outBack(clamp(t / 3F));
        float tremor = MathHelper.sin(age * 3.1F) * 0.02F * snap;
        set(p, RA_X, -1.52F + tremor, snap);
        set(p, RA_Y, 0.05F, snap);
        set(p, RA_Z, 0F, snap);
        set(p, R_ELBOW, -0.3F, snap);
        set(p, LA_X, 0.15F, snap);
        set(p, LA_Y, 0F, snap);
        set(p, LA_Z, -0.25F, snap);
        set(p, L_ELBOW, -0.3F, snap);
        set(p, CHEST_Y, -0.2F, snap);
        set(p, WAIST_Y, 0F, snap);
        p[LEAN] += 0.1F * snap;
        float tilt = smooth((t - 8F) / 10F);
        p[HEAD_Z] += 0.22F * tilt;
        p[HEAD_X] += 0.06F * tilt;
        p[LOOK] = 1F;
    }

    /** From holding the fist, his left palm straight into the chest in front of him. */
    private static void palm(float[] p, float t, float age) {
        caught(p, 20F, age);
        float drive = strike(t, EntityMagus.PALM_HIT - 3F, EntityMagus.PALM_HIT, EntityMagus.PALM_HIT + 8F);
        set(p, LA_X, -1.6F, drive);
        set(p, LA_Y, -0.15F, drive);
        set(p, LA_Z, 0F, drive);
        set(p, L_ELBOW, -0.03F, drive);
        set(p, RA_X, 0.1F, drive);
        set(p, R_ELBOW, -0.6F, drive);
        p[CHEST_Y] += 0.55F * drive;
        p[LEAN] += 0.15F * drive;
        p[RL_X] += 0.25F * drive;
    }

    /** Wound up across his chest, then the back of the hand out through whatever is beside him. */
    private static void backhand(float[] p, float t, float age) {
        guard(p, age);
        float wind = smooth(t / (EntityMagus.BACKHAND_HIT - 2F)) * (1F - smooth((t - EntityMagus.BACKHAND_HIT + 2F) / 2F));
        set(p, RA_X, -1.3F, wind);
        set(p, RA_Y, 1.15F, wind);
        set(p, RA_Z, -0.2F, wind);
        set(p, R_ELBOW, -1.4F, wind);
        p[CHEST_Y] += 0.5F * wind;
        float sweep = strike(t, EntityMagus.BACKHAND_HIT - 2F, EntityMagus.BACKHAND_HIT, EntityMagus.BACKHAND_HIT + 8F);
        set(p, RA_X, -1.45F, sweep);
        set(p, RA_Y, -0.95F, sweep);
        set(p, RA_Z, 0.6F, sweep);
        set(p, R_ELBOW, -0.05F, sweep);
        set(p, CHEST_Y, -0.55F, sweep);
        p[WAIST_Y] -= 0.2F * sweep;
        p[HEAD_Y] -= 0.15F * sweep;
    }

    /** Both hands come down on someone's shoulders, and his knee comes up into them. */
    private static void knee(float[] p, float t, float age) {
        guard(p, age);
        float reach = outCubic(clamp(t / 3F)) * (1F - smooth((t - 12F) / 5F));
        set(p, RA_X, -1.35F, reach);
        set(p, RA_Y, 0.25F, reach);
        set(p, RA_Z, 0F, reach);
        set(p, R_ELBOW, -0.7F, reach);
        set(p, LA_X, -1.35F, reach);
        set(p, LA_Y, -0.25F, reach);
        set(p, LA_Z, 0F, reach);
        set(p, L_ELBOW, -0.7F, reach);
        set(p, CHEST_Y, 0F, reach);
        float drive = strike(t, EntityMagus.KNEE_HIT - 3F, EntityMagus.KNEE_HIT, EntityMagus.KNEE_HIT + 7F);
        set(p, RL_X, -1.75F, drive);
        set(p, R_KNEE, 2F, drive);
        set(p, RA_X, -0.85F, drive);
        set(p, R_ELBOW, -1.1F, drive);
        set(p, LA_X, -0.85F, drive);
        set(p, L_ELBOW, -1.1F, drive);
        p[LEAN] += 0.3F * drive;
        p[WAIST_X] += 0.2F * drive;
        p[DROP] -= 1.5F * drive;
        p[LL_X] += 0.3F * drive;
    }

    /** Both fists locked together over his head, then down, all his weight behind them. */
    private static void hammer(float[] p, float t, float age) {
        guard(p, age);
        float raise = outCubic(clamp(t / 6F)) * (1F - smooth((t - EntityMagus.HAMMER_HIT + 2F) / 2F));
        set(p, RA_X, -2.95F, raise);
        set(p, RA_Y, -0.5F, raise);
        set(p, RA_Z, 0.1F, raise);
        set(p, R_ELBOW, -0.45F, raise);
        set(p, LA_X, -2.95F, raise);
        set(p, LA_Y, 0.5F, raise);
        set(p, LA_Z, -0.1F, raise);
        set(p, L_ELBOW, -0.45F, raise);
        set(p, CHEST_Y, 0F, raise);
        p[LEAN] -= 0.3F * raise;
        p[CHEST_X] -= 0.2F * raise;
        p[HEAD_X] += 0.2F * raise;
        float down = outCubic(clamp((t - EntityMagus.HAMMER_HIT + 2F) / 2F)) * (1F - smooth((t - 16F) / 8F));
        set(p, RA_X, -0.6F, down);
        set(p, RA_Y, -0.45F, down);
        set(p, R_ELBOW, -0.1F, down);
        set(p, LA_X, -0.6F, down);
        set(p, LA_Y, 0.45F, down);
        set(p, L_ELBOW, -0.1F, down);
        set(p, CHEST_Y, 0F, down);
        p[LEAN] += 0.45F * down;
        p[WAIST_X] += 0.3F * down;
        p[HEAD_X] += 0.25F * down;
        set(p, RL_X, 0.3F, down);
        set(p, LL_X, -0.4F, down);
        set(p, L_KNEE, 0.9F, down);
    }

    /** Both hands drawn back to the hip, gathering; then thrown forward, open, and held through the recoil. */
    private static void blast(float[] p, float t, float age, float pitch) {
        hang(p, age);
        float gather = smooth(t / 8F) * (1F - smooth((t - EntityMagus.BLAST_HIT + 2F) / 2F));
        set(p, RA_X, 0.5F, gather);
        set(p, RA_Y, -0.3F, gather);
        set(p, RA_Z, 0.15F, gather);
        set(p, R_ELBOW, -1.4F, gather);
        set(p, LA_X, 0.5F, gather);
        set(p, LA_Y, 0.3F, gather);
        set(p, LA_Z, -0.15F, gather);
        set(p, L_ELBOW, -1.4F, gather);
        p[CHEST_Y] -= 0.25F * gather;
        p[LEAN] -= 0.1F * gather;
        float thrown = outBack(clamp((t - EntityMagus.BLAST_HIT + 2F) / 3F)) * (1F - smooth((t - 20F) / 6F));
        set(p, RA_X, -1.57F + pitch, thrown);
        set(p, RA_Y, -0.22F, thrown);
        set(p, RA_Z, 0F, thrown);
        set(p, R_ELBOW, -0.05F, thrown);
        set(p, LA_X, -1.57F + pitch, thrown);
        set(p, LA_Y, 0.22F, thrown);
        set(p, LA_Z, 0F, thrown);
        set(p, L_ELBOW, -0.05F, thrown);
        p[LEAN] += 0.15F * thrown;
        set(p, RL_X, -0.4F, thrown);
        set(p, R_KNEE, 0.5F, thrown);
        set(p, LL_X, 0.4F, thrown);
        p[LOOK] = 1F;
    }

    /**
     * Someone held up by the throat at the end of his right arm, the arm nearly straight, his
     * head tipped back to look up at them, as unhurried about it as if he were holding up a lamp.
     */
    private static void grip(float[] p, float t, float age) {
        hang(p, age);
        float up = outCubic(clamp(t / 8F));
        set(p, RA_X, -2.35F, up);
        set(p, RA_Y, 0.08F, up);
        set(p, RA_Z, 0.1F, up);
        set(p, R_ELBOW, -0.3F, up);
        set(p, LA_X, 0.08F, up);
        set(p, LA_Z, -0.2F, up);
        set(p, L_ELBOW, -0.25F, up);
        p[CHEST_X] -= 0.12F * up;
        p[CHEST_Y] -= 0.1F * up;
        p[HEAD_X] -= 0.25F * up;
        p[LOOK] = 0.5F;
    }

    /** Still holding them up: the left hand reaches into their chest, closes, and is brought back to his brow. */
    private static void take(float[] p, float t, float age) {
        grip(p, 20F, age);
        float reach = outCubic(clamp(t / 10F)) * (1F - smooth((t - 22F) / 8F));
        float tremor = MathHelper.sin(age * 3.5F) * 0.04F * smooth((t - 10F) / 4F) * reach;
        set(p, LA_X, -1.85F + tremor, reach);
        set(p, LA_Y, -0.35F, reach);
        set(p, LA_Z, 0F, reach);
        set(p, L_ELBOW, -0.15F, reach);
        p[CHEST_Y] += 0.2F * reach;
        float brow = smooth((t - 24F) / 10F);
        set(p, LA_X, -2.5F, brow);
        set(p, LA_Y, -0.5F, brow);
        set(p, LA_Z, 0.1F, brow);
        set(p, L_ELBOW, -1.9F, brow);
        p[HEAD_X] += 0.15F * brow;
    }

    /** Taut as a drawn wire, arms in at his sides, head back: folding into a line, or out of one. */
    private static void fold(float[] p, float age) {
        p[RA_Z] = 0.05F;
        p[LA_Z] = -0.05F;
        p[R_ELBOW] = -0.05F;
        p[L_ELBOW] = -0.05F;
        p[HEAD_X] = -0.25F;
        p[CHEST_X] = -0.1F;
        p[LL_X] = 0.05F;
        p[RL_X] = 0.05F;
        p[LOOK] = 0.3F;
    }

    /** Coming down, arms a little out, palms down, legs together, looking down at where he is going. */
    private static void descend(float[] p, float age) {
        hang(p, age);
        p[RA_Z] = 0.5F;
        p[LA_Z] = -0.5F;
        p[RA_X] = -0.15F;
        p[LA_X] = -0.15F;
        p[R_ELBOW] = -0.2F;
        p[L_ELBOW] = -0.2F;
        p[RL_X] = 0.1F;
        p[LL_X] = 0.1F;
        p[R_KNEE] = 0.12F;
        p[L_KNEE] = 0.12F;
        p[HEAD_X] = 0.25F;
    }

    /**
     * Over a piece of the game, carrying it: bent from the waist, both arms down and a little
     * forward with the palms spread under him, as if what he holds were resting on his light;
     * the legs trailing, the head down to watch it. Warlock's way of doing it, without the effort.
     */
    private static void carry(float[] p, float age) {
        float breath = MathHelper.sin(age * 0.08F);
        p[LEAN] = 0.38F;
        p[WAIST_X] = 0.22F;
        p[CHEST_X] = 0.06F + breath * 0.015F;
        p[DROP] = breath * 0.25F;
        p[RA_X] = -0.5F + breath * 0.02F;
        p[RA_Y] = -0.15F;
        p[RA_Z] = 0.3F;
        p[R_ELBOW] = -0.15F;
        p[LA_X] = -0.5F - breath * 0.02F;
        p[LA_Y] = 0.15F;
        p[LA_Z] = -0.3F;
        p[L_ELBOW] = -0.15F;
        p[HEAD_X] = 0.5F;
        p[RL_X] = 0.3F;
        p[R_KNEE] = 0.35F;
        p[LL_X] = 0.42F;
        p[L_KNEE] = 0.55F;
        p[LOOK] = 0.15F;
    }

    /** Standing asleep: the head bowed, the arms straight down, breathing slowly. */
    private static void sleep(float[] p, float age) {
        float breath = MathHelper.sin(age * 0.05F);
        p[HEAD_X] = 0.55F + breath * 0.02F;
        p[CHEST_X] = 0.06F + breath * 0.015F;
        p[RA_Z] = 0.04F;
        p[LA_Z] = -0.04F;
        p[R_ELBOW] = -0.08F;
        p[L_ELBOW] = -0.08F;
        p[LOOK] = 0F;
    }

    /** The head comes up, slowly, and he looks at whoever is looking; then it goes down again. */
    private static void stir(float[] p, float t, float age) {
        sleep(p, age);
        float up = smooth((t - 6F) / 18F) * (1F - smooth((t - 48F) / 12F));
        p[HEAD_X] *= 1F - up;
        p[CHEST_X] -= 0.05F * up;
        p[LOOK] = up;
    }
}
