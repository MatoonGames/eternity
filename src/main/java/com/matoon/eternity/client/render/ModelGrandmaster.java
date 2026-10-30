package com.matoon.eternity.client.render;

import com.matoon.eternity.entity.EntityGrandmaster;
import com.matoon.eternity.entity.EntityGrandmaster.State;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The Grandmaster, jointed for a showman: a long-limbed body with a waist that bends and
 * twists, slender bare arms with wrists and a pointing finger on each hand, a high flared
 * collar and a gold breastplate standing proud of his chest, an engraved pauldron on his
 * left shoulder, coils of gold up his right forearm and a bangle on his left, a belt with a
 * red jewel and a ring of keys swinging from it, long coat tails, and silver hair: swept
 * back off his brow, a lock down each side of his face and a mane down his back, every
 * piece of it on springs the entity swings. Skinned in a 128x128 layout of its own (see
 * tools/GrandmasterSkin.java), which this file's box offsets must match.
 * <p>
 * As with Adam Warlock, each frame's pose is built from scratch out of his synced state and
 * how far into it he is, with anticipation and follow-through, and blended out of whatever
 * pose he was in before. Walking is laid over whatever he is doing from how fast his feet
 * are actually carrying him, so he can stroll and shuffle cards at once.
 */
@SideOnly(Side.CLIENT)
public class ModelGrandmaster extends ModelBase {

    static final int LEAN = 0, TWIST = 1, TILT = 2, DROP = 3, WAIST_X = 4, WAIST_Y = 5, CHEST_X = 6, CHEST_Y = 7, CHEST_Z = 8,
            HEAD_X = 9, HEAD_Y = 10, HEAD_Z = 11, RA_X = 12, RA_Y = 13, RA_Z = 14, R_ELBOW = 15, LA_X = 16, LA_Y = 17, LA_Z = 18,
            L_ELBOW = 19, RL_X = 20, RL_Y = 21, RL_Z = 22, R_KNEE = 23, LL_X = 24, LL_Y = 25, LL_Z = 26, L_KNEE = 27, LOOK = 28,
            R_WRIST = 29, L_WRIST = 30, R_CURL = 31, L_CURL = 32, R_FORE_Y = 33, L_FORE_Y = 34, N = 35;

    private static final float PI = (float) Math.PI;

    public final ModelRenderer root, abdomen, chest, head, rArm, rFore, rHand, rFinger, lArm, lFore, lHand, lFinger, rLeg, rShin, lLeg, lShin;
    public final ModelRenderer lockR, lockL, keyRing, keyA, keyB;
    public final ModelRenderer[] mane = new ModelRenderer[EntityGrandmaster.MANE];
    public final ModelRenderer[] tails = new ModelRenderer[EntityGrandmaster.TAILS];
    private final float[] pose = new float[N];
    private float partial;

    public ModelGrandmaster() {
        textureWidth = 128;
        textureHeight = 128;

        root = new ModelRenderer(this);
        root.setRotationPoint(0F, 12F, 0F);

        abdomen = box(root, 28, 16, -4F, -5F, -2F, 8, 5, 4);
        ModelRenderer belt = box(abdomen, 52, 16, -4.5F, -1.5F, -2.5F, 9, 2, 5);
        box(belt, 112, 0, -1F, -1.6F, -3.1F, 2, 2, 1);
        // A ring of keys at his left hip, swinging as he walks.
        keyRing = box(belt, 122, 0, -1F, 0F, -0.5F, 2, 2, 1);
        keyRing.setRotationPoint(2.6F, 0.2F, -2.7F);
        keyA = box(keyRing, 118, 0, -0.5F, 0F, -0.5F, 1, 3, 1);
        keyA.setRotationPoint(-0.4F, 1.6F, 0F);
        keyB = box(keyRing, 118, 0, -0.5F, 0F, -0.5F, 1, 3, 1);
        keyB.setRotationPoint(0.6F, 1.6F, 0.1F);
        // Coat tails, from the back of the belt to his ankles.
        ModelRenderer parent = belt;
        int[][] uv = {{0, 50}, {22, 50}, {46, 50}};
        for (int i = 0; i < tails.length; i++) {
            int w = 10 + i;
            tails[i] = box(parent, uv[i][0], uv[i][1], -w / 2F, 0F, -0.5F, w, 5, 1);
            tails[i].setRotationPoint(0F, i == 0 ? 0.2F : 5F, i == 0 ? 2.6F : 0F);
            parent = tails[i];
        }

        chest = box(abdomen, 0, 16, -4.5F, -7F, -2.5F, 9, 7, 5);
        chest.setRotationPoint(0F, -5F, 0F);
        // The gold breastplate, standing proud of the suit.
        box(chest, 102, 30, -4F, -6.6F, -3.1F, 8, 6, 1);
        // The high collar: a plate behind the neck and one flaring up at each side of it.
        ModelRenderer collar = box(chest, 28, 25, -5F, -4F, 0F, 10, 4, 1);
        collar.setRotationPoint(0F, -6.4F, 1.9F);
        collar.rotateAngleX = -0.32F;
        // Its wings stand up either side of the back of his neck and open forward, like a fan.
        ModelRenderer collarR = box(chest, 110, 16, -0.5F, -5F, 0F, 1, 5, 6);
        collarR.setRotationPoint(-4.6F, -6.3F, -0.8F);
        collarR.rotateAngleZ = 0.22F;
        collarR.rotateAngleY = -0.42F;
        collarR.rotateAngleX = -0.2F;
        ModelRenderer collarL = box(chest, 96, 16, -0.5F, -5F, 0F, 1, 5, 6);
        collarL.setRotationPoint(4.6F, -6.3F, -0.8F);
        collarL.rotateAngleZ = -0.22F;
        collarL.rotateAngleY = 0.42F;
        collarL.rotateAngleX = -0.2F;

        head = box(chest, 0, 0, -4F, -8F, -4F, 8, 8, 8);
        head.setRotationPoint(0F, -7F, -0.3F);
        // Hair swept straight back off the brow, a little lifted in front.
        ModelRenderer crown = box(head, 32, 0, -4.5F, -1.2F, -3.9F, 9, 2, 9);
        crown.setRotationPoint(0F, -7.6F, 0F);
        crown.rotateAngleX = -0.16F;
        box(head, 68, 0, -4.5F, -8.6F, 3F, 9, 5, 2);
        lockR = box(head, 80, 16, -0.5F, 0F, -1.5F, 1, 8, 3);
        lockR.setRotationPoint(-4.4F, -6.5F, -0.6F);
        lockL = box(head, 88, 16, -0.5F, 0F, -1.5F, 1, 8, 3);
        lockL.setRotationPoint(4.4F, -6.5F, -0.6F);
        int[][] maneUv = {{90, 0}, {90, 7}, {68, 7}};
        int[] maneW = {10, 9, 7};
        int[] maneH = {6, 6, 5};
        parent = head;
        for (int i = 0; i < mane.length; i++) {
            mane[i] = box(parent, maneUv[i][0], maneUv[i][1], -maneW[i] / 2F, 0F, -0.5F, maneW[i], maneH[i], 1);
            mane[i].setRotationPoint(0F, i == 0 ? -4.2F : maneH[i - 1], i == 0 ? 4.6F : 0F);
            parent = mane[i];
        }

        rArm = box(chest, 0, 30, -2F, -1.5F, -1.5F, 3, 6, 3);
        rArm.setRotationPoint(-5.4F, -5.6F, 0F);
        rFore = box(rArm, 24, 30, -1.5F, 0F, -1.5F, 3, 6, 3);
        rFore.setRotationPoint(-0.5F, 4.5F, 0F);
        box(rFore, 48, 30, -2F, 0.6F, -2F, 4, 4, 4);
        rHand = box(rFore, 52, 23, -1.5F, 0F, -1F, 3, 3, 2);
        rHand.setRotationPoint(0F, 6F, 0F);
        rFinger = box(rHand, 110, 7, -0.5F, -0.5F, -0.5F, 1, 3, 1);
        rFinger.setRotationPoint(-0.8F, 2.6F, -0.4F);

        lArm = box(chest, 12, 30, -1F, -1.5F, -1.5F, 3, 6, 3);
        lArm.setRotationPoint(5.4F, -5.6F, 0F);
        ModelRenderer lPad = box(lArm, 80, 30, -1.6F, -2.8F, -3F, 5, 3, 6);
        lPad.rotateAngleZ = -0.22F;
        lFore = box(lArm, 36, 30, -1.5F, 0F, -1.5F, 3, 6, 3);
        lFore.setRotationPoint(0.5F, 4.5F, 0F);
        box(lFore, 64, 30, -2F, 4F, -2F, 4, 2, 4);
        lHand = box(lFore, 62, 23, -1.5F, 0F, -1F, 3, 3, 2);
        lHand.setRotationPoint(0F, 6F, 0F);
        lFinger = box(lHand, 114, 7, -0.5F, -0.5F, -0.5F, 1, 3, 1);
        lFinger.setRotationPoint(0.8F, 2.6F, -0.4F);

        rLeg = box(root, 0, 40, -2F, 0F, -2F, 4, 6, 4);
        rLeg.setRotationPoint(-2F, 0F, 0F);
        rShin = box(rLeg, 32, 40, -2F, 0F, -2F, 4, 6, 4);
        rShin.setRotationPoint(0F, 6F, 0F);
        box(rShin, 64, 40, -2.5F, 3F, -2.9F, 5, 3, 5);

        lLeg = box(root, 16, 40, -2F, 0F, -2F, 4, 6, 4);
        lLeg.setRotationPoint(2F, 0F, 0F);
        lShin = box(lLeg, 48, 40, -2F, 0F, -2F, 4, 6, 4);
        lShin.setRotationPoint(0F, 6F, 0F);
        box(lShin, 84, 40, -2.5F, 3F, -2.9F, 5, 3, 5);
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

    /** Draws him again in the pose he was last given, for his glow and the light breaking through him. */
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
        (right ? rHand : lHand).postRender(scale);
        GlStateManager.translate(0F, 2.2F * scale, 0F);
    }

    /** Moves the matrix to the tip of one forefinger, in model space. */
    public void toFingertip(boolean right, float scale) {
        root.postRender(scale);
        abdomen.postRender(scale);
        chest.postRender(scale);
        (right ? rArm : lArm).postRender(scale);
        (right ? rFore : lFore).postRender(scale);
        (right ? rHand : lHand).postRender(scale);
        (right ? rFinger : lFinger).postRender(scale);
        GlStateManager.translate(0F, 3F * scale, 0F);
    }

    // ------------------------------------------------------------------ posing

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch,
                                  float scale, Entity entity) {
        if (!(entity instanceof EntityGrandmaster)) {
            return;
        }
        EntityGrandmaster g = (EntityGrandmaster) entity;
        float t = g.clientStateTicks + partial;
        java.util.Arrays.fill(pose, 0F);
        State state = g.getState();
        animate(g, state, t, ageInTicks, pose);

        float blend = smooth(t / blendTicks(state));
        if (blend < 1F) {
            for (int i = 0; i < N; i++) {
                pose[i] = g.poseFrom[i] + (pose[i] - g.poseFrom[i]) * blend;
            }
        }
        System.arraycopy(pose, 0, g.poseLast, 0, N);

        float[] p = pose.clone();
        float gait = g.prevGait + (g.gait - g.prevGait) * partial;
        if (gait > 0.01F && state != State.FALL && state != State.LIE && state != State.FADE) {
            stroll(p, g.prevStride + (g.stride - g.prevStride) * partial, gait, state);
        }
        float broken = g.getBroken();
        if (broken > 0.05F && state != State.LIE && state != State.FADE) {
            // His life is going out of him with his board: a tremor, and he holds himself a little lower.
            float tremor = MathHelper.sin(ageInTicks * 2.3F) * 0.012F * broken;
            p[CHEST_X] += 0.1F * broken + tremor;
            p[HEAD_X] += 0.08F * broken - tremor;
            p[RA_Z] += tremor;
            p[LA_Z] -= tremor;
        }
        p[HEAD_Y] += netHeadYaw * 0.017453292F * p[LOOK];
        p[HEAD_X] += headPitch * 0.017453292F * p[LOOK];
        apply(p, g);
    }

    /** Laid over any gesture: long, unhurried strides, a little bounce, the hips and shoulders counter-swinging. */
    private static void stroll(float[] p, float stride, float gait, State state) {
        float s = MathHelper.sin(stride), c = MathHelper.cos(stride);
        p[RL_X] += s * 0.62F * gait;
        p[LL_X] -= s * 0.62F * gait;
        p[R_KNEE] += Math.max(0F, -c) * 0.85F * gait;
        p[L_KNEE] += Math.max(0F, c) * 0.85F * gait;
        p[DROP] -= Math.abs(c) * 0.55F * gait;
        p[WAIST_Y] += s * 0.08F * gait;
        p[CHEST_Y] -= s * 0.1F * gait;
        if (state == State.IDLE || state == State.THINK) {
            // Hands clasped behind him, they only sway with him.
            p[RA_X] -= s * 0.06F * gait;
            p[LA_X] += s * 0.06F * gait;
        }
    }

    private void apply(float[] p, EntityGrandmaster g) {
        root.rotationPointY = 12F + p[DROP];
        root.rotateAngleX = p[LEAN];
        root.rotateAngleY = p[TWIST];
        root.rotateAngleZ = p[TILT];
        abdomen.rotateAngleX = p[WAIST_X];
        abdomen.rotateAngleY = p[WAIST_Y];
        chest.rotateAngleX = p[CHEST_X];
        chest.rotateAngleY = p[CHEST_Y];
        chest.rotateAngleZ = p[CHEST_Z];
        head.rotateAngleX = MathHelper.clamp(p[HEAD_X], -1.3F, 1.1F);
        head.rotateAngleY = MathHelper.clamp(p[HEAD_Y], -1.2F, 1.2F);
        head.rotateAngleZ = p[HEAD_Z];
        rArm.rotateAngleX = p[RA_X];
        rArm.rotateAngleY = p[RA_Y];
        rArm.rotateAngleZ = p[RA_Z];
        rFore.rotateAngleX = Math.min(0F, p[R_ELBOW]);
        rFore.rotateAngleY = p[R_FORE_Y];
        rHand.rotateAngleX = p[R_WRIST];
        rFinger.rotateAngleX = -p[R_CURL] * 1.6F;
        lArm.rotateAngleX = p[LA_X];
        lArm.rotateAngleY = p[LA_Y];
        lArm.rotateAngleZ = p[LA_Z];
        lFore.rotateAngleX = Math.min(0F, p[L_ELBOW]);
        lFore.rotateAngleY = p[L_FORE_Y];
        lHand.rotateAngleX = p[L_WRIST];
        lFinger.rotateAngleX = -p[L_CURL] * 1.6F;
        rLeg.rotateAngleX = p[RL_X];
        rLeg.rotateAngleY = p[RL_Y];
        rLeg.rotateAngleZ = p[RL_Z];
        rShin.rotateAngleX = Math.max(0F, p[R_KNEE]);
        lLeg.rotateAngleX = p[LL_X];
        lLeg.rotateAngleY = p[LL_Y];
        lLeg.rotateAngleZ = p[LL_Z];
        lShin.rotateAngleX = Math.max(0F, p[L_KNEE]);

        // Hair and tails hang straight down whatever his body is doing, then swing on their springs.
        float upright = p[LEAN] + p[WAIST_X] + p[CHEST_X];
        float headTilt = upright + head.rotateAngleX;
        float previous = 0F;
        for (int i = 0; i < mane.length; i++) {
            float angle = g.prevMane[i] + (g.mane[i] - g.prevMane[i]) * partial;
            mane[i].rotateAngleX = angle - previous + (i == 0 ? 0.12F - headTilt : 0F);
            mane[i].rotateAngleZ = i == 0 ? (g.prevSway + (g.sway - g.prevSway) * partial) * 0.4F : 0F;
            previous = angle;
        }
        float lock = g.prevMane[0] + (g.mane[0] - g.prevMane[0]) * partial;
        float sway = g.prevSway + (g.sway - g.prevSway) * partial;
        lockR.rotateAngleX = lock * 0.4F - headTilt * 0.9F;
        lockR.rotateAngleZ = 0.08F + Math.max(0F, sway) * 0.5F;
        lockL.rotateAngleX = lock * 0.4F - headTilt * 0.9F;
        lockL.rotateAngleZ = -0.08F + Math.min(0F, sway) * 0.5F;
        previous = 0F;
        float hips = p[LEAN] + p[WAIST_X];
        for (int i = 0; i < tails.length; i++) {
            float angle = g.prevTails[i] + (g.tails[i] - g.prevTails[i]) * partial;
            tails[i].rotateAngleX = angle - previous + (i == 0 ? Math.max(-p[LEAN] * 0.6F, 0F) + Math.max(p[RL_X], p[LL_X]) * -0.45F
                    + Math.max(0F, -Math.min(p[RL_X], p[LL_X])) * 0.2F + 0.04F - Math.min(0F, hips) * 0.3F : 0F);
            tails[i].rotateAngleZ = i == 0 ? sway * 0.3F : 0F;
            previous = angle;
        }
        float key = g.prevKeys[0] + (g.keys[0] - g.prevKeys[0]) * partial;
        float key2 = g.prevKeys[1] + (g.keys[1] - g.prevKeys[1]) * partial;
        float keySway = g.prevKeySway + (g.keySway - g.prevKeySway) * partial;
        keyRing.rotateAngleX = key - hips;
        keyRing.rotateAngleZ = keySway;
        keyA.rotateAngleX = key2 - key;
        keyA.rotateAngleZ = keySway * 0.5F + 0.1F;
        keyB.rotateAngleX = (key2 - key) * 1.2F;
        keyB.rotateAngleZ = keySway * 0.7F - 0.12F;
    }

    private static float blendTicks(State state) {
        switch (state) {
            case WINCE:
                return 1.5F;
            case POINT:
            case CLAP:
                return 4F;
            case FALL:
                return 3F;
            case IDLE:
            case THINK:
                return 10F;
            default:
                return 6F;
        }
    }

    static void animate(EntityGrandmaster g, State state, float t, float age, float[] p) {
        switch (state) {
            case ARRIVE:
                arrive(p, t, age);
                break;
            case IDLE:
                idle(p, age);
                break;
            case PRESENT:
                present(p, t, age);
                break;
            case SHUFFLE:
                shuffle(p, t, age);
                break;
            case POINT:
                point(p, t, age);
                break;
            case CONDUCT:
                conduct(p, t, age);
                break;
            case LAUGH:
                laugh(p, t, age);
                break;
            case CLAP:
                clap(p, t, age);
                break;
            case THINK:
                think(p, t, age);
                break;
            case WINCE:
                wince(p, t, age);
                break;
            case FALL:
                fall(p, t, age);
                break;
            case LIE:
            case FADE:
                lie(p, age);
                break;
            case BOW:
                bow(p, t, age);
                break;
        }
    }

    // ------------------------------------------------------------------ the poses themselves

    /**
     * Standing easy and perfectly pleased with himself: hands clasped behind his back, weight
     * on one hip, head cocked as though everything were a little funny; every so often his
     * fingers drum, he rocks onto his toes, or he rolls his shoulders.
     */
    private static void idle(float[] p, float age) {
        float breath = MathHelper.sin(age * 0.07F);
        float shift = MathHelper.sin(age * 0.021F);
        p[TILT] = shift * 0.035F;
        p[WAIST_Y] = shift * 0.05F;
        p[CHEST_X] = -0.06F + breath * 0.02F;
        p[CHEST_Z] = -shift * 0.03F;
        p[HEAD_Z] = 0.1F + MathHelper.sin(age * 0.017F + 1F) * 0.07F;
        p[HEAD_X] = -0.05F;
        behindBack(p, true, 1F);
        behindBack(p, false, 1F);
        p[RA_X] += breath * 0.02F;
        p[LA_X] -= breath * 0.02F;
        p[R_CURL] = 0.6F + Math.max(0F, MathHelper.sin(age * 0.9F)) * 0.4F * Math.max(0F, MathHelper.sin(age * 0.05F));
        p[L_CURL] = 0.7F;
        // Weight onto one leg, the other knee soft.
        p[RL_Z] = 0.04F + shift * 0.03F;
        p[LL_Z] = -0.04F + shift * 0.03F;
        p[L_KNEE] = Math.max(0F, shift) * 0.16F;
        p[R_KNEE] = Math.max(0F, -shift) * 0.16F;
        // Now and then up onto his toes.
        float rock = Math.max(0F, MathHelper.sin(age * 0.031F)) ;
        rock = rock * rock * rock * rock;
        p[DROP] = -rock * 0.6F + breath * 0.15F;
        p[LEAN] = -rock * 0.04F;
        p[LOOK] = 1F;
    }

    /** One hand clasped behind his back: the arm swung back, the forearm turned in across the small of it. */
    private static void behindBack(float[] p, boolean right, float f) {
        set(p, right ? RA_X : LA_X, 0.62F, f);
        set(p, right ? RA_Y : LA_Y, 0F, f);
        set(p, right ? RA_Z : LA_Z, right ? 0.06F : -0.06F, f);
        set(p, right ? R_ELBOW : L_ELBOW, -1.45F, f);
        set(p, right ? R_FORE_Y : L_FORE_Y, right ? -1.5F : 1.5F, f);
        set(p, right ? R_CURL : L_CURL, 0.8F, f);
    }

    /** Out of a whirl of cards: crouched with his arms crossed over his face, then flung wide, head back, delighted. */
    private static void arrive(float[] p, float t, float age) {
        float hidden = 1F - smooth((t - 18F) / 6F);
        float burst = outBack(clamp((t - 20F) / 8F)) * (1F - smooth((t - 44F) / 18F));
        p[DROP] = 4F * hidden;
        p[WAIST_X] = 0.5F * hidden;
        p[CHEST_X] = 0.25F * hidden - 0.25F * burst;
        p[HEAD_X] = 0.5F * hidden - 0.55F * burst;
        p[RL_X] = -0.9F * hidden;
        p[R_KNEE] = 1.4F * hidden;
        p[LL_X] = -0.6F * hidden;
        p[L_KNEE] = 1.2F * hidden;
        p[RA_X] = -1.7F * hidden - 0.2F * burst;
        p[RA_Y] = -0.7F * hidden;
        p[R_ELBOW] = -1.6F * hidden;
        p[RA_Z] = 1.9F * burst;
        p[LA_X] = -1.7F * hidden - 0.2F * burst;
        p[LA_Y] = 0.7F * hidden;
        p[L_ELBOW] = -1.6F * hidden;
        p[LA_Z] = -1.9F * burst;
        p[R_WRIST] = -0.4F * burst;
        p[L_WRIST] = -0.4F * burst;
        float[] rest = new float[N];
        present(rest, 30F, age);
        float settle = smooth((t - 46F) / 20F);
        for (int i = 0; i < N; i++) {
            p[i] += (rest[i] - p[i]) * settle;
        }
        p[LOOK] = smooth((t - 40F) / 12F);
    }

    /** Arms opened wide to his board, palms up, the host welcoming his guest; a small bow of the head at the end. */
    private static void present(float[] p, float t, float age) {
        idleBase(p, age);
        float open = outBack(clamp(t / 9F)) * (1F - smooth((t - EntityGrandmaster.PRESENT_TICKS + 8F) / 8F));
        set(p, RA_X, -0.55F, open);
        set(p, RA_Y, 0F, open);
        set(p, RA_Z, 1.05F, open);
        set(p, R_ELBOW, -0.35F, open);
        set(p, R_FORE_Y, -0.9F, open);
        set(p, LA_X, -0.55F, open);
        set(p, LA_Y, 0F, open);
        set(p, LA_Z, -1.05F, open);
        set(p, L_ELBOW, -0.35F, open);
        set(p, L_FORE_Y, 0.9F, open);
        set(p, R_WRIST, -0.5F, open);
        set(p, L_WRIST, -0.5F, open);
        set(p, R_CURL, 0F, open);
        set(p, L_CURL, 0F, open);
        p[CHEST_X] -= 0.14F * open;
        p[HEAD_X] -= 0.12F * open;
        float nod = MathHelper.sin(clamp((t - 22F) / 12F) * PI);
        p[HEAD_X] += 0.3F * nod * open;
        p[WAIST_X] += 0.12F * nod * open;
        p[LOOK] = 1F - 0.3F * nod;
    }

    /** The same stance as standing at ease, but with his hands free. */
    private static void idleBase(float[] p, float age) {
        float breath = MathHelper.sin(age * 0.07F);
        float shift = MathHelper.sin(age * 0.021F);
        p[TILT] = shift * 0.03F;
        p[CHEST_X] = -0.05F + breath * 0.02F;
        p[HEAD_Z] = 0.06F;
        p[RA_X] = -0.05F;
        p[RA_Z] = 0.12F;
        p[R_ELBOW] = -0.3F;
        p[LA_X] = -0.05F;
        p[LA_Z] = -0.12F;
        p[L_ELBOW] = -0.3F;
        p[R_CURL] = 0.5F;
        p[L_CURL] = 0.5F;
        p[RL_Z] = 0.04F;
        p[LL_Z] = -0.04F;
        p[L_KNEE] = Math.max(0F, shift) * 0.14F;
        p[R_KNEE] = Math.max(0F, -shift) * 0.14F;
        p[DROP] = breath * 0.15F;
        p[LOOK] = 1F;
    }

    /**
     * A waterfall of cards from his right hand, held high, down into his left: the wrists
     * flicking in time, his eyes never on them. The cards themselves are drawn by the renderer.
     */
    private static void shuffle(float[] p, float t, float age) {
        idleBase(p, age);
        float in = smooth(t / 7F) * (1F - smooth((t - EntityGrandmaster.SHUFFLE_TICKS + 7F) / 7F));
        float flick = MathHelper.sin(t * 1.9F);
        set(p, RA_X, -1.15F + flick * 0.03F, in);
        set(p, RA_Y, -0.3F, in);
        set(p, RA_Z, 0.12F, in);
        set(p, R_ELBOW, -1.05F, in);
        set(p, R_WRIST, -0.4F + flick * 0.12F, in);
        set(p, R_CURL, 0.35F, in);
        set(p, LA_X, -0.75F, in);
        set(p, LA_Y, 0.45F, in);
        set(p, LA_Z, -0.05F, in);
        set(p, L_ELBOW, -1.15F, in);
        set(p, L_WRIST, 0.2F - flick * 0.08F, in);
        set(p, L_CURL, 0.5F, in);
        p[HEAD_Z] += 0.06F * in;
        p[CHEST_Y] += MathHelper.sin(t * 0.25F) * 0.04F * in;
        p[DROP] += MathHelper.sin(t * 0.95F) * 0.12F * in;
    }

    /** A long finger levelled at what he means, the rest of him leaning after it. */
    private static void point(float[] p, float t, float age) {
        idleBase(p, age);
        float out = outBack(clamp((t - 3F) / 5F)) * (1F - smooth((t - EntityGrandmaster.POINT_TICKS + 8F) / 8F));
        float coil = smooth(t / 3F) * (1F - smooth((t - 3F) / 3F));
        set(p, RA_X, 0.3F, coil);
        set(p, R_ELBOW, -1.2F, coil);
        set(p, RA_X, -1.55F, out);
        set(p, RA_Y, -0.08F, out);
        set(p, RA_Z, 0F, out);
        set(p, R_ELBOW, -0.04F, out);
        set(p, R_WRIST, 0.05F, out);
        set(p, R_CURL, 0F, out);
        set(p, R_FORE_Y, 0F, out);
        p[CHEST_Y] -= 0.3F * out;
        p[LEAN] += 0.06F * out;
        p[HEAD_X] += 0.06F * out;
        behindBack(p, false, out);
        p[LOOK] = 1F;
    }

    /**
     * Moving a piece without touching it: his right arm up and out toward it, palm open and
     * fingers spread, drawing it along through the air with small turns of the wrist, his
     * other hand tucked behind him. The gold threads from his fingers are drawn by the renderer.
     */
    private static void conduct(float[] p, float t, float age) {
        idleBase(p, age);
        float up = outBack(clamp(t / 6F));
        float weave = MathHelper.sin(t * 0.32F);
        set(p, RA_X, -1.85F + weave * 0.12F, up);
        set(p, RA_Y, -0.25F + weave * 0.18F, up);
        set(p, RA_Z, 0.1F, up);
        set(p, R_ELBOW, -0.25F, up);
        set(p, R_WRIST, -0.7F + MathHelper.sin(t * 0.5F) * 0.15F, up);
        set(p, R_CURL, 0F, up);
        set(p, R_FORE_Y, -0.6F + weave * 0.3F, up);
        behindBack(p, false, up);
        p[CHEST_Y] -= 0.22F * up;
        p[CHEST_X] -= 0.08F * up;
        p[HEAD_X] += 0.05F * up;
        p[TILT] -= 0.04F * up;
        // Up onto the toes of one foot, as if it were all very light.
        p[DROP] -= 0.3F * up;
        p[LL_X] = 0.18F * up;
        p[L_KNEE] = 0.3F * up;
        p[LOOK] = 1F;
    }

    /** Head thrown back and shoulders shaking, one hand on his stomach, the other slapping his thigh. */
    private static void laugh(float[] p, float t, float age) {
        idleBase(p, age);
        float in = outBack(clamp(t / 5F)) * (1F - smooth((t - EntityGrandmaster.LAUGH_TICKS + 10F) / 10F));
        float shake = MathHelper.sin(t * 1.45F) * (1F - clamp(t / EntityGrandmaster.LAUGH_TICKS) * 0.6F);
        p[CHEST_X] -= (0.35F + shake * 0.06F) * in;
        p[HEAD_X] -= (0.45F + shake * 0.08F) * in;
        p[WAIST_X] -= 0.08F * in;
        p[DROP] += shake * 0.25F * in;
        set(p, LA_X, -0.55F, in);
        set(p, LA_Y, 0.9F, in);
        set(p, L_ELBOW, -1.3F, in);
        set(p, L_CURL, 0.3F, in);
        float slap = Math.max(0F, MathHelper.sin(t * 0.5F));
        set(p, RA_X, -0.35F + slap * 0.5F, in);
        set(p, RA_Z, 0.18F, in);
        set(p, R_ELBOW, -0.55F, in);
        set(p, R_WRIST, -0.3F * slap, in);
        set(p, R_CURL, 0.2F, in);
        p[LOOK] = 1F - 0.7F * in;
    }

    /** Slow applause, hands high and the claps far apart: for a good move, or for himself. */
    private static void clap(float[] p, float t, float age) {
        idleBase(p, age);
        float in = smooth(t / 5F) * (1F - smooth((t - EntityGrandmaster.CLAP_TICKS + 8F) / 8F));
        float beat = (t % 10F) / 10F;
        float together = beat < 0.35F ? smooth(beat / 0.35F) : 1F - smooth((beat - 0.35F) / 0.65F);
        set(p, RA_X, -1.15F, in);
        set(p, RA_Y, -0.35F - 0.45F * together, in);
        set(p, R_ELBOW, -0.75F, in);
        set(p, R_CURL, 0F, in);
        set(p, R_FORE_Y, 0.6F, in);
        set(p, LA_X, -1.15F, in);
        set(p, LA_Y, 0.35F + 0.45F * together, in);
        set(p, L_ELBOW, -0.75F, in);
        set(p, L_CURL, 0F, in);
        set(p, L_FORE_Y, -0.6F, in);
        p[HEAD_Z] += 0.08F * in;
        p[CHEST_X] -= 0.06F * in;
    }

    /** Chin on one hand, elbow propped on the other arm across him; the finger on his cheek tapping. */
    private static void think(float[] p, float t, float age) {
        idleBase(p, age);
        float in = smooth(t / 10F);
        float tap = Math.max(0F, MathHelper.sin(age * 0.8F)) * Math.max(0F, MathHelper.sin(age * 0.07F));
        set(p, RA_X, -1.25F, in);
        set(p, RA_Y, -0.55F, in);
        set(p, RA_Z, 0F, in);
        set(p, R_ELBOW, -2.1F, in);
        set(p, R_WRIST, -0.25F, in);
        set(p, R_CURL, 0.3F - tap * 0.3F, in);
        set(p, R_FORE_Y, 0.2F, in);
        set(p, LA_X, -0.75F, in);
        set(p, LA_Y, 0.95F, in);
        set(p, L_ELBOW, -1.45F, in);
        set(p, L_CURL, 0.8F, in);
        p[HEAD_X] += 0.12F * in;
        p[HEAD_Z] += 0.12F * in;
        p[CHEST_X] += 0.05F * in;
        p[WAIST_Y] += 0.06F * in;
    }

    /** Struck through his board: a jolt back, one hand to his chest, a knee giving, then he straightens. */
    private static void wince(float[] p, float t, float age) {
        idleBase(p, age);
        float hit = outCubic(clamp(t / 2F)) * (1F - smooth((t - 8F) / 16F));
        p[CHEST_X] += 0.32F * hit;
        p[WAIST_X] += 0.18F * hit;
        p[HEAD_X] += 0.25F * hit;
        p[HEAD_Z] -= 0.12F * hit;
        p[DROP] += 1.2F * hit;
        p[R_KNEE] += 0.5F * hit;
        p[RL_X] -= 0.25F * hit;
        set(p, LA_X, -1.05F, hit);
        set(p, LA_Y, 0.75F, hit);
        set(p, L_ELBOW, -1.6F, hit);
        set(p, L_CURL, 0.1F, hit);
        set(p, RA_Z, 0.55F, hit);
        set(p, RA_X, -0.25F, hit);
        set(p, R_CURL, 0F, hit);
        p[LOOK] = 1F - hit * 0.6F;
    }

    /** His board gone: his knees go, he kneels a moment with a hand to his chest, then goes over onto his back. */
    private static void fall(float[] p, float t, float age) {
        idleBase(p, age);
        float kneel = outCubic(clamp(t / 10F));
        float over = smooth((t - 26F) / 22F);
        float[] down = new float[N];
        lie(down, age);
        // Kneeling.
        set(p, RL_X, -1.5F, kneel);
        set(p, R_KNEE, 1.55F, kneel);
        set(p, LL_X, -0.2F, kneel);
        set(p, L_KNEE, 1.9F, kneel);
        p[DROP] = 5.2F * kneel + p[DROP] * (1F - kneel);
        set(p, LA_X, -1.0F, kneel);
        set(p, LA_Y, 0.8F, kneel);
        set(p, L_ELBOW, -1.6F, kneel);
        set(p, RA_X, 0.1F, kneel);
        set(p, RA_Z, 0.35F, kneel);
        p[CHEST_X] += 0.25F * kneel;
        p[HEAD_X] += 0.3F * kneel;
        for (int i = 0; i < N; i++) {
            p[i] += (down[i] - p[i]) * over;
        }
        p[LOOK] = 0.3F * (1F - over) + down[LOOK] * over;
    }

    /** On his back among the pieces, one hand on his chest, breathing shallowly, his head turned to whoever is there. */
    private static void lie(float[] p, float age) {
        float breath = MathHelper.sin(age * 0.11F);
        p[LEAN] = -1.5F;
        p[DROP] = 9.6F;
        p[HEAD_X] = -0.15F;
        p[CHEST_X] = breath * 0.03F;
        p[LA_X] = -1.1F;
        p[LA_Y] = 0.8F;
        p[L_ELBOW] = -1.4F;
        p[L_CURL] = 0.2F;
        p[RA_X] = -0.15F;
        p[RA_Z] = 0.55F;
        p[R_ELBOW] = -0.25F;
        p[R_CURL] = 0.3F;
        p[R_WRIST] = 0.2F;
        p[RL_X] = 0.05F;
        p[R_KNEE] = 0.35F;
        p[RL_Z] = 0.12F;
        p[LL_X] = 0.2F;
        p[L_KNEE] = 0.1F;
        p[LL_Z] = -0.05F;
        p[LOOK] = 0.6F;
    }

    /** A showman's bow: right hand to his chest, left arm swept out, down from the waist, and up again. */
    private static void bow(float[] p, float t, float age) {
        idleBase(p, age);
        float in = smooth(t / 8F) * (1F - smooth((t - EntityGrandmaster.BOW_TICKS + 9F) / 9F));
        float down = smooth((t - 4F) / 8F) * (1F - smooth((t - 18F) / 10F));
        set(p, RA_X, -0.9F, in);
        set(p, RA_Y, -0.9F, in);
        set(p, R_ELBOW, -1.6F, in);
        set(p, R_CURL, 0F, in);
        set(p, LA_X, -0.3F, in);
        set(p, LA_Z, -1.25F, in);
        set(p, L_ELBOW, -0.2F, in);
        set(p, L_WRIST, -0.4F, in);
        set(p, L_CURL, 0F, in);
        p[WAIST_X] += 0.65F * down;
        p[CHEST_X] += 0.15F * down;
        p[HEAD_X] += 0.15F * down;
        p[RL_X] -= 0.1F * in;
        p[LL_X] += 0.25F * in;
        p[L_KNEE] += 0.2F * in;
        p[LOOK] = 1F - down;
    }

    // ------------------------------------------------------------------ easing

    private static void set(float[] p, int i, float value, float f) {
        p[i] += (value - p[i]) * f;
    }

    static float clamp(float x) {
        return x < 0F ? 0F : x > 1F ? 1F : x;
    }

    static float smooth(float x) {
        x = clamp(x);
        return x * x * (3F - 2F * x);
    }

    static float outCubic(float x) {
        x = 1F - clamp(x);
        return 1F - x * x * x;
    }

    static float outBack(float x) {
        x = clamp(x) - 1F;
        return 1F + 2.70158F * x * x * x + 1.70158F * x * x;
    }
}
