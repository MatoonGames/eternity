package com.matoon.eternity.client.render;

import com.matoon.eternity.entity.EntityAdamWarlock;
import com.matoon.eternity.entity.EntityAdamWarlock.State;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Adam Warlock, jointed for fighting: a waist that bends and twists, elbows and knees, the
 * armour standing proud of him (pauldrons, vambraces, boots, knee guards, the belt and its
 * buckle), his swept-back hair, the gem on his brow, the high collar, and a cape in four
 * segments that the entity swings on springs. Skinned in a 128x128 layout of its own (see
 * tools/WarlockSkin.java), which this file's box offsets must match.
 *
 * Every frame his pose is built from scratch out of his synced state and how far into it he
 * is, with easing on every blow: anticipation before, a snap through, follow-through after.
 * Whenever his state changes he blends out of whatever pose he was last drawn in, so nothing
 * ever pops.
 */
@SideOnly(Side.CLIENT)
public class ModelAdamWarlock extends ModelBase {

    // The pose, as one array so it can be blended and kept on the entity.
    static final int LEAN = 0, TWIST = 1, TILT = 2, DROP = 3, WAIST_X = 4, WAIST_Y = 5, CHEST_X = 6, CHEST_Y = 7, CHEST_Z = 8,
            HEAD_X = 9, HEAD_Y = 10, HEAD_Z = 11, RA_X = 12, RA_Y = 13, RA_Z = 14, R_ELBOW = 15, LA_X = 16, LA_Y = 17, LA_Z = 18,
            L_ELBOW = 19, RL_X = 20, RL_Y = 21, RL_Z = 22, R_KNEE = 23, LL_X = 24, LL_Y = 25, LL_Z = 26, L_KNEE = 27, LOOK = 28, N = 29;

    private static final float PI = (float) Math.PI;

    public final ModelRenderer root, abdomen, chest, head, rArm, rFore, lArm, lFore, rLeg, rShin, lLeg, lShin;
    public final ModelRenderer[] cape = new ModelRenderer[EntityAdamWarlock.CAPE];
    private final float[] pose = new float[N];
    private float partial;

    public ModelAdamWarlock() {
        textureWidth = 128;
        textureHeight = 128;

        root = new ModelRenderer(this);
        root.setRotationPoint(0F, 12F, 0F);

        abdomen = box(root, 28, 16, -4F, -5F, -2F, 8, 5, 4);
        ModelRenderer belt = box(abdomen, 52, 16, -4.5F, -1.5F, -2.5F, 9, 2, 5);
        box(belt, 80, 16, -1F, -1.8F, -3.1F, 2, 2, 1);

        chest = box(abdomen, 0, 16, -4.5F, -7F, -2.5F, 9, 7, 5);
        chest.setRotationPoint(0F, -5F, 0F);
        ModelRenderer collar = box(chest, 90, 10, -5F, -4F, 0F, 10, 5, 1);
        collar.setRotationPoint(0F, -6.5F, 1.8F);
        collar.rotateAngleX = -0.38F;

        head = box(chest, 0, 0, -4F, -8F, -4F, 8, 8, 8);
        head.setRotationPoint(0F, -7F, -0.2F);
        box(head, 32, 0, -4.5F, -9F, -4.5F, 9, 2, 9);
        // The quiff: swept up off his forehead and back, its front edge raised.
        ModelRenderer quiff = box(head, 68, 0, -4F, -1.8F, -4.3F, 8, 3, 4);
        quiff.setRotationPoint(0F, -8.5F, -0.4F);
        quiff.rotateAngleX = -0.2F;
        box(head, 68, 7, -4.5F, -8.8F, 3F, 9, 5, 2);
        box(head, 90, 0, -4.6F, -8.4F, -2F, 1, 4, 6);
        box(head, 104, 0, 3.6F, -8.4F, -2F, 1, 4, 6);
        box(head, 118, 0, -0.5F, -6F, -4.45F, 1, 1, 1);

        rArm = box(chest, 0, 32, -3F, -1.5F, -2F, 4, 6, 4);
        rArm.setRotationPoint(-5.5F, -5.5F, 0F);
        ModelRenderer rPad = box(rArm, 0, 44, -3.6F, -2.7F, -3F, 6, 3, 6);
        rPad.rotateAngleZ = 0.18F;
        rFore = box(rArm, 32, 32, -2F, 0F, -2F, 4, 7, 4);
        rFore.setRotationPoint(-1F, 4.5F, 0F);
        box(rFore, 64, 32, -2.5F, 0.4F, -2.5F, 5, 4, 5);

        lArm = box(chest, 16, 32, -1F, -1.5F, -2F, 4, 6, 4);
        lArm.setRotationPoint(5.5F, -5.5F, 0F);
        ModelRenderer lPad = box(lArm, 24, 44, -2.4F, -2.7F, -3F, 6, 3, 6);
        lPad.rotateAngleZ = -0.18F;
        lFore = box(lArm, 48, 32, -2F, 0F, -2F, 4, 7, 4);
        lFore.setRotationPoint(1F, 4.5F, 0F);
        box(lFore, 84, 32, -2.5F, 0.4F, -2.5F, 5, 4, 5);

        rLeg = box(root, 48, 44, -2F, 0F, -2F, 4, 6, 4);
        rLeg.setRotationPoint(-2F, 0F, 0F);
        rShin = box(rLeg, 80, 44, -2F, 0F, -2F, 4, 6, 4);
        rShin.setRotationPoint(0F, 6F, 0F);
        box(rShin, 0, 56, -2.5F, 2F, -2.9F, 5, 4, 5);
        box(rShin, 28, 66, -2F, -1.2F, -2.6F, 4, 3, 1);

        lLeg = box(root, 64, 44, -2F, 0F, -2F, 4, 6, 4);
        lLeg.setRotationPoint(2F, 0F, 0F);
        lShin = box(lLeg, 96, 44, -2F, 0F, -2F, 4, 6, 4);
        lShin.setRotationPoint(0F, 6F, 0F);
        box(lShin, 20, 56, -2.5F, 2F, -2.9F, 5, 4, 5);
        box(lShin, 38, 66, -2F, -1.2F, -2.6F, 4, 3, 1);

        // The cape hangs from under the collar, each segment a little wider than the last.
        int[][] uv = {{40, 56}, {62, 56}, {86, 56}, {0, 66}};
        ModelRenderer parent = chest;
        for (int i = 0; i < cape.length; i++) {
            int width = 10 + i;
            cape[i] = box(parent, uv[i][0], uv[i][1], -width / 2F, 0F, 0F, width, 6, 1);
            cape[i].setRotationPoint(0F, i == 0 ? -6.6F : 6F, i == 0 ? 2.5F : 0F);
            parent = cape[i];
        }
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

    /** Draws him again in the pose he was last given, for his afterimages and glow. */
    public void renderPose(float scale) {
        root.render(scale);
    }

    /** Moves the matrix to the knuckles of one fist, in model space. */
    public void toFist(boolean right, float scale) {
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
        if (!(entity instanceof EntityAdamWarlock)) {
            return;
        }
        EntityAdamWarlock w = (EntityAdamWarlock) entity;
        float t = w.clientStateTicks + partial;
        float pitch = headPitch * 0.017453292F;
        java.util.Arrays.fill(pose, 0F);
        animate(w, w.getState(), t, ageInTicks, pitch, pose);

        // Out of whatever he was doing before, never straight into the new pose.
        float blend = smooth(t / blendTicks(w.getState()));
        if (blend < 1F) {
            for (int i = 0; i < N; i++) {
                pose[i] = w.poseFrom[i] + (pose[i] - w.poseFrom[i]) * blend;
            }
        }
        System.arraycopy(pose, 0, w.poseLast, 0, N);

        float[] p = pose.clone();
        // Struck, he recoils, whatever he is in the middle of.
        float hurt = w.hurtTime > 0 ? (w.hurtTime - partial) / 10F : 0F;
        if (hurt > 0F) {
            hurt = hurt * hurt;
            p[CHEST_X] -= 0.3F * hurt;
            p[HEAD_X] -= 0.4F * hurt;
            p[WAIST_X] -= 0.12F * hurt;
            p[RA_Z] += 0.25F * hurt;
            p[LA_Z] -= 0.25F * hurt;
        }
        p[HEAD_Y] += netHeadYaw * 0.017453292F * p[LOOK];
        p[HEAD_X] += pitch * p[LOOK];
        apply(p, w);
    }

    private void apply(float[] p, EntityAdamWarlock w) {
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
        float previous = 0F;
        for (int i = 0; i < cape.length; i++) {
            float angle = w.prevCape[i] + (w.cape[i] - w.prevCape[i]) * partial;
            cape[i].rotateAngleX = angle - previous;
            cape[i].rotateAngleZ = i == 0 ? (w.prevCapeSway + (w.capeSway - w.prevCapeSway) * partial) * 0.5F : 0F;
            previous = angle;
        }
        // The cape is fixed to his back, so bending forward at the waist and chest swings it with him.
        cape[0].rotateAngleX -= p[WAIST_X] * 0.6F + p[CHEST_X] * 0.6F;
    }

    private static float blendTicks(State state) {
        switch (state) {
            case SLAM:
                return 1.5F;
            case FLYBY:
            case EMERGE:
            case STAGGER:
                return 2.5F;
            case COMBO:
            case DIVE:
                return 3F;
            case WINDUP:
                return 4F;
            case HOVER:
                return 9F;
            default:
                return 6F;
        }
    }

    /** The pose for a state, t ticks into it. */
    static void animate(EntityAdamWarlock w, State state, float t, float age, float pitch, float[] p) {
        switch (state) {
            case ARRIVE:
                arrive(p, t, age);
                break;
            case HOVER:
                hover(p, age, w.isEvolved());
                break;
            case TAUNT:
                taunt(p, t, age, pitch);
                break;
            case WINDUP:
                windup(p, t, age, w);
                break;
            case FLYBY:
                flyby(p, age);
                break;
            case TURN:
                turn(p, t, age);
                break;
            case COMBO:
                combo(p, t, age);
                break;
            case VOLLEY:
                volley(p, t, age, pitch, w.volleyBolts());
                break;
            case BEAM:
                beam(p, t, age, pitch);
                break;
            case RISE:
                rise(p, t, age);
                break;
            case DIVE:
                dive(p, age);
                break;
            case SLAM:
                slam(p, t, age);
                break;
            case COCOON:
                cocoon(p, t, age);
                break;
            case EMERGE:
                emerge(p, t, age);
                break;
            case STAGGER:
                stagger(p, t, age);
                break;
            case RETREAT:
                retreat(p, t, age);
                break;
            case CARRY:
                carry(p, age);
                break;
            case STAND:
                stand(p, age);
                break;
            case WALK:
                walk(p, age);
                break;
        }
    }

    // ------------------------------------------------------------------ the poses themselves

    /** Floating at ease and entirely sure of himself: chest out, chin up, one knee drawn up, fists loose. */
    private static void hover(float[] p, float age, boolean evolved) {
        float breath = MathHelper.sin(age * 0.1F);
        p[DROP] = breath * 0.6F;
        p[CHEST_X] = -0.06F + MathHelper.sin(age * 0.1F + 0.6F) * 0.03F;
        p[HEAD_X] = -0.06F;
        p[RA_X] = -0.12F + breath * 0.035F;
        p[RA_Y] = 0.05F;
        p[RA_Z] = evolved ? 0.3F : 0.2F;
        p[R_ELBOW] = -0.38F;
        p[LA_X] = -0.04F + MathHelper.sin(age * 0.1F + 1F) * 0.035F;
        p[LA_Z] = evolved ? -0.28F : -0.18F;
        p[L_ELBOW] = -0.26F;
        p[RL_X] = -0.34F + MathHelper.sin(age * 0.08F) * 0.05F;
        p[R_KNEE] = 0.6F;
        p[RL_Z] = 0.04F;
        p[LL_X] = 0.12F + MathHelper.sin(age * 0.08F + 1.4F) * 0.05F;
        p[L_KNEE] = 0.26F;
        p[LL_Z] = -0.05F;
        p[LOOK] = 1F;
    }

    /** Head first through the air, one fist out in front. In his frame, overhead is the way he is going. */
    private static void flyby(float[] p, float age) {
        float buffet = MathHelper.sin(age * 1.7F) * 0.03F;
        p[RA_X] = -2.95F + buffet;
        p[RA_Z] = 0.1F;
        p[R_ELBOW] = -0.04F;
        p[LA_X] = 0.32F - buffet;
        p[LA_Z] = -0.14F;
        p[L_ELBOW] = -0.22F;
        p[CHEST_Y] = -0.2F;
        p[HEAD_X] = -1.0F;
        p[RL_X] = 0.06F;
        p[R_KNEE] = 0.1F;
        p[RL_Z] = 0.02F;
        p[LL_X] = 0.16F + buffet;
        p[L_KNEE] = 0.55F;
        p[LL_Z] = -0.03F;
    }

    private static void arrive(float[] p, float t, float age) {
        if (t < EntityAdamWarlock.ARRIVE_STREAK) {
            flyby(p, age);
            return;
        }
        // He stops dead: arms flung out against the air, legs swinging down under him, then he settles.
        float[] brake = new float[N];
        turn(brake, 10F, age);
        float[] rest = new float[N];
        hover(rest, age, false);
        rest[HEAD_X] = -0.18F;
        rest[HEAD_Z] = 0.08F;
        mix(p, brake, rest, smooth((t - EntityAdamWarlock.ARRIVE_STREAK - 6F) / 14F));
        float jolt = 1F - clamp((t - EntityAdamWarlock.ARRIVE_STREAK) / 6F);
        p[DROP] += 3F * jolt * jolt;
        p[WAIST_X] += 0.3F * jolt;
        p[LOOK] = smooth((t - 20F) / 10F);
    }

    /** Arms folded, chin up, a roll of the neck, then a finger levelled at you. */
    private static void taunt(float[] p, float t, float age, float pitch) {
        hover(p, age, false);
        float fold = smooth(t / 10F) * (1F - smooth((t - 34F) / 6F));
        set(p, RA_X, -0.4F, fold);
        set(p, RA_Y, -0.9F, fold);
        set(p, RA_Z, 0F, fold);
        set(p, R_ELBOW, -1.25F, fold);
        set(p, LA_X, -0.5F, fold);
        set(p, LA_Y, 0.85F, fold);
        set(p, LA_Z, 0F, fold);
        set(p, L_ELBOW, -1.15F, fold);
        set(p, HEAD_X, -0.22F, fold);
        p[CHEST_X] -= 0.06F * fold;
        float roll = clamp((t - 18F) / 12F);
        p[HEAD_Z] = (0.12F + MathHelper.sin(roll * PI * 2F) * 0.22F * MathHelper.sin(roll * PI)) * fold;
        // "You."
        float point = outBack(clamp((t - 36F) / 5F)) * (1F - smooth((t - 50F) / 6F));
        set(p, RA_X, -1.5F + pitch, point);
        set(p, RA_Y, -0.12F, point);
        set(p, RA_Z, 0F, point);
        set(p, R_ELBOW, -0.04F, point);
        set(p, LA_X, 0.08F, point);
        set(p, LA_Y, 0F, point);
        set(p, LA_Z, -0.2F, point);
        set(p, L_ELBOW, -0.35F, point);
        p[CHEST_Y] -= 0.22F * point;
        p[HEAD_X] += 0.08F * point;
        p[LOOK] = 1F;
    }

    /** Coiling: fist drawn back to his ribs, the other hand out to aim, knees tucked, trembling with it. */
    private static void windup(float[] p, float t, float age, EntityAdamWarlock w) {
        hover(p, age, w.isEvolved());
        float length = w.previousState == State.TURN ? EntityAdamWarlock.WINDUP_CHAINED
                : w.isEvolved() ? EntityAdamWarlock.WINDUP_EVOLVED : EntityAdamWarlock.WINDUP_TICKS;
        float coil = outCubic(clamp(t / (length * 0.55F)));
        float tremor = MathHelper.sin(age * 2.7F) * 0.04F * coil;
        set(p, CHEST_Y, -0.5F, coil);
        set(p, WAIST_X, 0.22F, coil);
        set(p, RA_X, 0.55F + tremor, coil);
        set(p, RA_Z, 0.28F, coil);
        set(p, R_ELBOW, -2.05F, coil);
        set(p, LA_X, -1.35F - tremor, coil);
        set(p, LA_Y, 0.25F, coil);
        set(p, LA_Z, -0.1F, coil);
        set(p, L_ELBOW, -0.15F, coil);
        set(p, RL_X, -1.05F, coil);
        set(p, R_KNEE, 1.65F, coil);
        set(p, LL_X, -0.45F, coil);
        set(p, L_KNEE, 1.25F, coil);
        set(p, HEAD_X, 0.12F, coil);
        p[CHEST_Y] += tremor;
        // The instant before he goes, he rears back.
        float rear = smooth((t - length * 0.7F) / (length * 0.3F));
        p[LEAN] = -0.22F * rear;
        p[DROP] = p[DROP] * (1F - coil) - 1.2F * rear;
        p[LOOK] = 0.8F;
    }

    /** Overshot, braking hard with his arms out, legs swinging under him, looking back for you. */
    private static void turn(float[] p, float t, float age) {
        float k = smooth(t / 10F);
        float buffet = MathHelper.sin(age * 1.3F) * 0.04F * (1F - k);
        p[RA_X] = -0.5F + buffet;
        p[RA_Z] = 0.6F + 0.5F * k;
        p[R_ELBOW] = -0.4F;
        p[LA_X] = -0.45F - buffet;
        p[LA_Z] = -0.6F - 0.5F * k;
        p[L_ELBOW] = -0.45F;
        p[RL_X] = -0.75F * k;
        p[R_KNEE] = 0.3F + 0.6F * k;
        p[LL_X] = -0.35F * k + 0.15F;
        p[L_KNEE] = 0.4F + 0.25F * k;
        p[CHEST_X] = -0.16F;
        p[LOOK] = smooth((t - 4F) / 10F);
    }

    /** Jab, cross, uppercut, then from above, both fists together like a hammer. */
    private static void combo(float[] p, float t, float age) {
        // A boxer's guard, hanging in the air: fists up by his chin.
        p[DROP] = MathHelper.sin(age * 0.2F) * 0.3F;
        p[RA_X] = -1.15F;
        p[RA_Y] = -0.45F;
        p[RA_Z] = 0.25F;
        p[R_ELBOW] = -1.95F;
        p[LA_X] = -1.25F;
        p[LA_Y] = 0.45F;
        p[LA_Z] = -0.25F;
        p[L_ELBOW] = -1.9F;
        p[RL_X] = -0.45F;
        p[R_KNEE] = 0.6F;
        p[LL_X] = 0.3F;
        p[L_KNEE] = 0.5F;
        p[WAIST_X] = 0.12F;
        p[HEAD_X] = 0.1F;
        p[LOOK] = 0.9F;
        p[LEAN] = 0.25F * (1F - smooth((t - 2F) / 4F));

        float jab = strike(t, EntityAdamWarlock.COMBO_JAB - 2F, EntityAdamWarlock.COMBO_JAB, EntityAdamWarlock.COMBO_JAB + 3F);
        set(p, LA_X, -1.62F, jab);
        set(p, LA_Y, 0.08F, jab);
        set(p, L_ELBOW, -0.02F, jab);
        p[CHEST_Y] += 0.42F * jab;
        p[WAIST_Y] += 0.12F * jab;

        float cross = strike(t, EntityAdamWarlock.COMBO_CROSS - 2F, EntityAdamWarlock.COMBO_CROSS, EntityAdamWarlock.COMBO_CROSS + 3F);
        set(p, RA_X, -1.62F, cross);
        set(p, RA_Y, -0.08F, cross);
        set(p, R_ELBOW, -0.02F, cross);
        p[CHEST_Y] -= 0.55F * cross;
        p[WAIST_Y] -= 0.2F * cross;
        p[LEAN] += 0.12F * cross;
        p[LL_X] += 0.25F * cross;

        // Down and coiled, then up through you.
        float dip = smooth((t - 15F) / 3F) * (1F - smooth((t - 18.5F) / 2F));
        p[DROP] += 2.5F * dip;
        set(p, RA_X, 0.35F, dip);
        set(p, RA_Y, -0.1F, dip);
        set(p, R_ELBOW, -1.65F, dip);
        p[WAIST_X] += 0.25F * dip;
        p[R_KNEE] += 0.5F * dip;
        p[CHEST_Y] -= 0.25F * dip;
        float upper = outBack(clamp((t - 18F) / 3F)) * (1F - smooth((t - 24F) / 3F));
        set(p, RA_X, -2.75F, upper);
        set(p, RA_Y, -0.2F, upper);
        set(p, R_ELBOW, -0.55F, upper);
        p[CHEST_Y] -= 0.35F * upper;
        p[CHEST_X] -= 0.2F * upper;
        p[LEAN] -= 0.25F * upper;
        p[DROP] -= 2F * upper;
        p[LL_X] += 0.4F * upper;
        p[HEAD_X] -= 0.35F * upper;

        if (t > EntityAdamWarlock.COMBO_BLINK + 0.5F) {
            // Above you now: fists locked together over his head, then down.
            float raise = smooth((t - EntityAdamWarlock.COMBO_BLINK) / 3F);
            float slam = outCubic(clamp((t - (EntityAdamWarlock.COMBO_HAMMER - 1.5F)) / 2F));
            float ease = 1F - smooth((t - 37F) / 7F);
            float up = raise * (1F - slam);
            float down = slam * ease;
            set(p, RA_X, -2.9F, up);
            set(p, RA_Y, -0.55F, up);
            set(p, RA_Z, 0.1F, up);
            set(p, R_ELBOW, -0.5F, up);
            set(p, LA_X, -2.9F, up);
            set(p, LA_Y, 0.55F, up);
            set(p, LA_Z, -0.1F, up);
            set(p, L_ELBOW, -0.5F, up);
            set(p, RL_X, -0.9F, up);
            set(p, R_KNEE, 1.3F, up);
            set(p, LL_X, -0.6F, up);
            set(p, L_KNEE, 1.4F, up);
            p[LEAN] -= 0.35F * up;
            p[HEAD_X] += 0.35F * up;
            set(p, RA_X, -0.95F, down);
            set(p, RA_Y, -0.45F, down);
            set(p, R_ELBOW, -0.1F, down);
            set(p, LA_X, -0.95F, down);
            set(p, LA_Y, 0.45F, down);
            set(p, L_ELBOW, -0.1F, down);
            set(p, RL_X, 0.35F, down);
            set(p, R_KNEE, 0.5F, down);
            set(p, LL_X, 0.25F, down);
            set(p, L_KNEE, 0.7F, down);
            p[LEAN] += 0.3F * down;
            p[WAIST_X] += 0.25F * down;
            p[HEAD_X] += 0.12F * down;
            p[LOOK] = 0.5F;
        }
    }

    /** Bolts from one palm, then the other, each arm snapping out and drawing back. */
    private static void volley(float[] p, float t, float age, float pitch, int bolts) {
        hover(p, age, true);
        float gather = smooth(t / 8F) * (1F - smooth((t - EntityAdamWarlock.VOLLEY_GATHER + 3F) / 3F));
        set(p, RA_X, 0.45F, gather);
        set(p, RA_Z, 0.4F, gather);
        set(p, R_ELBOW, -0.7F, gather);
        set(p, LA_X, 0.45F, gather);
        set(p, LA_Z, -0.4F, gather);
        set(p, L_ELBOW, -0.7F, gather);
        p[CHEST_X] -= 0.15F * gather;
        float firing = smooth((t - EntityAdamWarlock.VOLLEY_GATHER + 3F) / 3F)
                * (1F - smooth((t - EntityAdamWarlock.VOLLEY_GATHER - bolts * EntityAdamWarlock.VOLLEY_GAP - 4F) / 8F));
        if (firing <= 0F) {
            return;
        }
        // Both arms chambered at his sides, palms forward.
        set(p, RA_X, -0.75F, firing);
        set(p, RA_Z, 0.25F, firing);
        set(p, R_ELBOW, -1.5F, firing);
        set(p, LA_X, -0.75F, firing);
        set(p, LA_Z, -0.25F, firing);
        set(p, L_ELBOW, -1.5F, firing);
        float since = t - EntityAdamWarlock.VOLLEY_GATHER;
        int k = MathHelper.clamp(MathHelper.floor(since / EntityAdamWarlock.VOLLEY_GAP), 0, bolts - 1);
        float local = since - k * EntityAdamWarlock.VOLLEY_GAP;
        float thrust = (local < 0F ? 0F : local < 1.5F ? outCubic(local / 1.5F) : 1F - 0.8F * smooth((local - 2F) / 5F)) * firing;
        boolean right = k % 2 == 0;
        set(p, right ? RA_X : LA_X, -1.57F + pitch, thrust);
        set(p, right ? RA_Y : LA_Y, right ? -0.15F : 0.15F, thrust);
        set(p, right ? RA_Z : LA_Z, 0F, thrust);
        set(p, right ? R_ELBOW : L_ELBOW, -0.03F, thrust);
        p[CHEST_Y] += (right ? -0.35F : 0.35F) * thrust;
        p[LEAN] -= 0.07F * (local < 3F && local >= 0F ? 1F - local / 3F : 0F) * firing;
        p[LOOK] = 1F;
    }

    /** A sun cupped between his palms, then thrust out, and held against the recoil. */
    private static void beam(float[] p, float t, float age, float pitch) {
        hover(p, age, false);
        int charge = EntityAdamWarlock.BEAM_CHARGE, end = charge + EntityAdamWarlock.BEAM_FIRE;
        float cup = smooth(t / 10F) * (1F - smooth((t - charge) / 3F));
        float tremor = MathHelper.sin(age * 3.1F) * 0.045F * clamp(t / charge);
        set(p, RA_X, -1.0F + tremor, cup);
        set(p, RA_Y, -0.75F, cup);
        set(p, RA_Z, 0F, cup);
        set(p, R_ELBOW, -1.2F, cup);
        set(p, LA_X, -1.0F - tremor, cup);
        set(p, LA_Y, 0.75F, cup);
        set(p, LA_Z, 0F, cup);
        set(p, L_ELBOW, -1.2F, cup);
        set(p, LEAN, -0.12F, cup);
        set(p, WAIST_X, 0.15F, cup);
        set(p, HEAD_X, 0.18F, cup);
        float brace = smooth(t / 10F) * (1F - smooth((t - end) / 10F));
        set(p, RL_X, -0.55F, brace);
        set(p, R_KNEE, 0.75F, brace);
        set(p, RL_Z, 0.16F, brace);
        set(p, LL_X, 0.35F, brace);
        set(p, L_KNEE, 0.45F, brace);
        set(p, LL_Z, -0.16F, brace);
        float fire = outBack(clamp((t - charge) / 3F)) * (1F - smooth((t - end) / 6F));
        float shake = MathHelper.sin(age * 3.3F) * 0.05F;
        set(p, RA_X, -1.57F + pitch + shake, fire);
        set(p, RA_Y, -0.3F, fire);
        set(p, R_ELBOW, -0.05F, fire);
        set(p, LA_X, -1.57F + pitch - shake, fire);
        set(p, LA_Y, 0.3F, fire);
        set(p, L_ELBOW, -0.05F, fire);
        set(p, LEAN, 0.1F, fire);
        set(p, CHEST_X, -0.1F, fire);
        float spent = smooth((t - end) / 6F);
        p[CHEST_X] += MathHelper.sin(age * 0.5F) * 0.07F * spent;
        p[HEAD_X] += 0.2F * spent;
        p[LOOK] = 1F - 0.5F * Math.max(cup, fire);
    }

    /** Straight up, fist overhead; then hanging at the top, looking down for the mark, and tucking to dive. */
    private static void rise(float[] p, float t, float age) {
        flyby(p, age);
        p[HEAD_X] = -0.55F;
        float apex = smooth((t - EntityAdamWarlock.RISE_CLIMB) / 4F);
        float tuck = smooth((t - EntityAdamWarlock.RISE_TICKS + 6F) / 5F);
        float spread = apex * (1F - tuck);
        set(p, RA_X, -0.35F, spread);
        set(p, RA_Z, 1.2F, spread);
        set(p, LA_X, -0.35F, spread);
        set(p, LA_Z, -1.2F, spread);
        set(p, L_ELBOW, -0.3F, spread);
        set(p, RL_X, -0.2F, spread);
        set(p, R_KNEE, 0.4F, spread);
        set(p, HEAD_X, 0.75F, apex);
        set(p, LEAN, 0.3F, apex);
        set(p, RA_X, -2.9F, tuck);
        set(p, RA_Z, 0F, tuck);
        set(p, RA_Y, -0.15F, tuck);
        set(p, LA_X, -2.9F, tuck);
        set(p, LA_Z, 0F, tuck);
        set(p, LA_Y, 0.15F, tuck);
        set(p, R_KNEE, 0.9F, tuck);
        set(p, L_KNEE, 1.1F, tuck);
        set(p, LEAN, 0.6F, tuck);
    }

    /** Head down, both fists first, like a meteor. */
    private static void dive(float[] p, float age) {
        flyby(p, age);
        p[LA_X] = p[RA_X];
        p[RA_Y] = -0.16F;
        p[LA_Y] = 0.16F;
        p[LA_Z] = -0.1F;
        p[L_ELBOW] = -0.04F;
        p[CHEST_Y] = 0F;
        p[L_KNEE] = 0.3F;
    }

    /** One knee down, fist in the ground, the other arm thrown back; then his head comes up. */
    private static void slam(float[] p, float t, float age) {
        float squash = 1F - clamp(t / 4F);
        p[DROP] = 5.3F + 1.6F * squash * squash;
        p[WAIST_X] = 0.68F + 0.15F * squash;
        p[CHEST_X] = 0.05F;
        p[LL_X] = -1.45F;
        p[L_KNEE] = 1.5F;
        p[LL_Z] = -0.12F;
        p[RL_X] = -0.15F;
        p[R_KNEE] = 1.6F;
        p[RL_Z] = 0.08F;
        p[RA_X] = -0.5F;
        p[RA_Z] = 0.12F;
        p[R_ELBOW] = -0.1F;
        p[LA_X] = 0.95F;
        p[LA_Z] = -0.4F;
        p[L_ELBOW] = -0.3F;
        float up = smooth((t - 10F) / 6F);
        p[HEAD_X] = 0.45F - 1.05F * up + MathHelper.sin(age * 0.3F) * 0.02F;
        p[LOOK] = 0.35F * up;
        // Rising out of it before he takes off again.
        float rise = smooth((t - EntityAdamWarlock.SLAM_TICKS + 8F) / 8F);
        p[DROP] *= 1F - rise * 0.6F;
        p[WAIST_X] *= 1F - rise * 0.6F;
    }

    /** A spasm, arms thrown out; then he folds in on himself, knees to his chest. */
    private static void cocoon(float[] p, float t, float age) {
        float spasm = outBack(clamp(t / 4F)) * (1F - smooth((t - 6F) / 6F));
        p[RA_Z] = 1.3F * spasm;
        p[LA_Z] = -1.3F * spasm;
        p[RA_X] = -0.3F * spasm;
        p[LA_X] = -0.3F * spasm;
        p[HEAD_X] = -0.55F * spasm;
        p[CHEST_X] = -0.3F * spasm;
        float curl = smooth((t - 6F) / 18F);
        float breath = MathHelper.sin(age * 0.12F) * 0.04F * curl;
        set(p, RL_X, -1.95F, curl);
        set(p, R_KNEE, 2.3F, curl);
        set(p, LL_X, -1.85F, curl);
        set(p, L_KNEE, 2.35F, curl);
        set(p, RA_X, -1.05F + breath, curl);
        set(p, RA_Y, -0.6F, curl);
        set(p, R_ELBOW, -1.35F, curl);
        set(p, LA_X, -1.05F - breath, curl);
        set(p, LA_Y, 0.6F, curl);
        set(p, L_ELBOW, -1.35F, curl);
        set(p, WAIST_X, 0.45F + breath, curl);
        set(p, HEAD_X, 0.7F, curl);
        p[DROP] = -3F * curl;
    }

    /** Bursting out, arms flung wide and head thrown back; then the arms come down and the glare comes back. */
    private static void emerge(float[] p, float t, float age) {
        float burst = outBack(clamp(t / 7F));
        float settle = smooth((t - 30F) / 18F);
        float hold = burst * (1F - settle);
        float tremor = MathHelper.sin(age * 2.9F) * 0.03F * hold;
        p[RA_X] = -0.4F * hold;
        p[RA_Z] = 2.2F * hold + tremor;
        p[R_ELBOW] = -0.2F * hold;
        p[LA_X] = -0.4F * hold;
        p[LA_Z] = -2.2F * hold - tremor;
        p[L_ELBOW] = -0.2F * hold;
        p[HEAD_X] = -0.7F * hold;
        p[CHEST_X] = -0.35F * hold;
        p[LEAN] = -0.15F * hold;
        p[RL_Z] = 0.3F * hold;
        p[LL_Z] = -0.3F * hold;
        p[R_KNEE] = 0.15F * hold;
        p[L_KNEE] = 0.15F * hold;
        float[] after = new float[N];
        hover(after, age, true);
        for (int i = 0; i < N; i++) {
            p[i] += after[i] * settle;
        }
        p[LOOK] = settle;
    }

    /** Thrown back, flailing; then hanging dazed, head down. */
    private static void stagger(float[] p, float t, float age) {
        float hit = outCubic(clamp(t / 3F)) * (1F - smooth((t - 6F) / 10F));
        float dazed = smooth((t - 8F) / 10F);
        p[HEAD_X] = -0.75F * hit + (0.45F + MathHelper.sin(age * 0.15F) * 0.05F) * dazed;
        p[HEAD_Z] = MathHelper.sin(age * 0.11F) * 0.1F * dazed;
        p[CHEST_X] = -0.4F * hit + 0.2F * dazed;
        p[LEAN] = -0.4F * hit + 0.05F * dazed;
        p[RA_X] = -1.6F * hit + 0.1F * dazed;
        p[RA_Z] = 0.7F * hit + 0.12F * dazed;
        p[R_ELBOW] = -0.5F * hit - 0.2F * dazed;
        p[LA_X] = -1.3F * hit + 0.05F * dazed;
        p[LA_Z] = -0.8F * hit - 0.1F * dazed;
        p[L_ELBOW] = -0.5F * hit - 0.15F * dazed;
        p[RL_X] = -0.8F * hit - 0.1F * dazed;
        p[R_KNEE] = 0.8F * hit + 0.3F * dazed;
        p[LL_X] = -0.5F * hit + 0.05F * dazed;
        p[L_KNEE] = 0.8F * hit + 0.2F * dazed;
        p[DROP] = MathHelper.sin(age * 0.09F) * 0.5F * dazed;
        p[LOOK] = 0.2F * dazed;
    }

    /** Beaten: a hand pressed to his side, breathing hard, glaring; then straight up and gone. */
    private static void retreat(float[] p, float t, float age) {
        if (t >= EntityAdamWarlock.RETREAT_LAUNCH) {
            flyby(p, age);
            p[HEAD_X] = -0.5F;
            return;
        }
        float heave = MathHelper.sin(age * 0.35F);
        p[LA_X] = -0.55F;
        p[LA_Y] = 0.95F;
        p[L_ELBOW] = -1.5F;
        p[RA_X] = 0.05F;
        p[RA_Z] = 0.22F;
        p[R_ELBOW] = -0.45F;
        p[WAIST_X] = 0.28F;
        p[CHEST_X] = heave * 0.07F;
        p[HEAD_X] = -0.12F;
        p[RL_X] = -0.5F;
        p[R_KNEE] = 0.9F;
        p[LL_X] = 0.1F;
        p[L_KNEE] = 0.4F;
        p[DROP] = heave * 0.4F;
        p[LOOK] = 1F;
    }

    /** Over a piece, holding it up from above: bent forward at the hips, both hands down on it, legs trailing. */
    private static void carry(float[] p, float age) {
        float breath = MathHelper.sin(age * 0.12F);
        p[LEAN] = 0.32F;
        p[WAIST_X] = 0.3F;
        p[DROP] = breath * 0.3F;
        p[RA_X] = -0.55F + breath * 0.03F;
        p[RA_Y] = -0.1F;
        p[RA_Z] = 0.18F;
        p[R_ELBOW] = -0.3F;
        p[LA_X] = -0.55F - breath * 0.03F;
        p[LA_Y] = 0.1F;
        p[LA_Z] = -0.18F;
        p[L_ELBOW] = -0.3F;
        p[HEAD_X] = 0.55F;
        p[RL_X] = 0.35F;
        p[R_KNEE] = 0.7F;
        p[LL_X] = 0.55F;
        p[L_KNEE] = 0.9F;
        p[LOOK] = 0.2F;
    }

    /** On his own two feet for once, arms folded, weight on one leg, watching. */
    private static void stand(float[] p, float age) {
        float breath = MathHelper.sin(age * 0.08F);
        p[CHEST_X] = breath * 0.025F;
        p[RA_X] = -0.4F;
        p[RA_Y] = -0.9F;
        p[R_ELBOW] = -1.25F;
        p[LA_X] = -0.5F;
        p[LA_Y] = 0.85F;
        p[L_ELBOW] = -1.15F;
        p[HEAD_X] = 0.08F;
        p[TILT] = 0.03F;
        p[RL_Z] = 0.05F;
        p[LL_X] = -0.1F;
        p[L_KNEE] = 0.18F;
        p[LL_Z] = -0.04F;
        p[LOOK] = 1F;
    }

    /** Walking, which he never does: slowly, head down, hands loose. */
    private static void walk(float[] p, float age) {
        float s = MathHelper.sin(age * 0.42F), c = MathHelper.cos(age * 0.42F);
        p[RL_X] = s * 0.55F;
        p[LL_X] = -s * 0.55F;
        p[R_KNEE] = Math.max(0F, -c) * 0.7F + 0.05F;
        p[L_KNEE] = Math.max(0F, c) * 0.7F + 0.05F;
        p[RA_X] = -s * 0.32F;
        p[LA_X] = s * 0.32F;
        p[RA_Z] = 0.06F;
        p[LA_Z] = -0.06F;
        p[R_ELBOW] = -0.2F;
        p[L_ELBOW] = -0.2F;
        p[DROP] = Math.abs(c) * -0.35F;
        p[CHEST_Y] = s * 0.06F;
        p[HEAD_X] = 0.3F;
        p[LOOK] = 0.3F;
    }

    // ------------------------------------------------------------------ easing

    /** Eases one value of the pose toward a target, by f. */
    private static void set(float[] p, int i, float value, float f) {
        p[i] += (value - p[i]) * f;
    }

    private static void mix(float[] out, float[] a, float[] b, float f) {
        for (int i = 0; i < N; i++) {
            out[i] = a[i] + (b[i] - a[i]) * f;
        }
    }

    /** A blow: snapping out between start and contact, held a moment, drawn back by end. */
    private static float strike(float t, float start, float contact, float end) {
        if (t < start || t > end + 2F) {
            return 0F;
        }
        if (t < contact) {
            return outCubic((t - start) / (contact - start));
        }
        return t < contact + 1F ? 1F : 1F - smooth((t - contact - 1F) / (end - contact + 1F));
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
