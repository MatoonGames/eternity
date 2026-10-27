package com.matoon.eternity.client.render;

import com.matoon.eternity.entity.EntityEinherjar;
import com.matoon.eternity.entity.EntityEinherjar.State;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import static com.matoon.eternity.client.render.VaultParts.clamp;
import static com.matoon.eternity.client.render.VaultParts.outBack;
import static com.matoon.eternity.client.render.VaultParts.outCubic;
import static com.matoon.eternity.client.render.VaultParts.set;
import static com.matoon.eternity.client.render.VaultParts.smooth;
import static com.matoon.eternity.client.render.VaultParts.strike;

/**
 * One of the Einherjar, jointed for fighting with a spear and shield: a waist that bends and
 * twists, elbows and knees, tassets that swing from his belt, a cloak in four segments the entity
 * swings on springs, and his helm's crest, a ring of gold horns open at the top with a blade
 * standing up through it. Boxes and texture layout from {@link VaultBoxes#EINHERJAR}.
 *
 * Every frame his pose is built from scratch out of his synced state and how far into it he is,
 * and whenever his state changes he blends out of whatever pose he was last drawn in. His spear
 * is given as the angle it should make in the world (0 upright, a quarter turn levelled in front
 * of him), and turned in his hand to make it, whatever his arm is doing.
 */
@SideOnly(Side.CLIENT)
public class ModelEinherjar extends ModelBase {

    static final int LEAN = 0, TWIST = 1, DROP = 2, WAIST_X = 3, WAIST_Y = 4, CHEST_X = 5, CHEST_Y = 6, CHEST_Z = 7,
            HEAD_X = 8, HEAD_Y = 9, HEAD_Z = 10, RA_X = 11, RA_Y = 12, RA_Z = 13, R_ELBOW = 14, LA_X = 15, LA_Y = 16, LA_Z = 17,
            L_ELBOW = 18, RL_X = 19, RL_Z = 20, R_KNEE = 21, LL_X = 22, LL_Z = 23, L_KNEE = 24, LOOK = 25, SPEAR = 26, SPEAR_Z = 27,
            SHIELD_Y = 28, SHIELD_X = 29, WALK = 30, TILT = 31, N = 32;

    private static final float PI = (float) Math.PI;
    /** The ring of the crest: its radius, and where its middle is above the crown of the helm. */
    private static final float RING_RADIUS = 3.2F, RING_Y = -15.3F;

    private final VaultBoxes.Sheet sheet = VaultBoxes.EINHERJAR;
    public final ModelRenderer root, tassetF, tassetB, abdomen, chest, head, rArm, rFore, lArm, lFore, rLeg, rShin, lLeg, lShin, spear, shield;
    public final ModelRenderer[] cape = new ModelRenderer[EntityEinherjar.CAPE];
    private final float[] pose = new float[N];
    private float partial;

    public ModelEinherjar() {
        textureWidth = sheet.width;
        textureHeight = sheet.height;

        root = part("pelvis", null, 0F, 12F, 0F);
        tassetF = part("tassetF", root, 0F, 0.6F, -2.7F);
        tassetB = part("tassetB", root, 0F, 0.6F, 2.7F);
        part("tassetR", root, 0F, 0F, 0F);
        part("tassetL", root, 0F, 0F, 0F);
        abdomen = part("abdomen", root, 0F, -2F, 0F);
        chest = part("chest", abdomen, 0F, -5F, 0F);
        part("claspR", chest, 0F, 0F, 0F);
        part("claspL", chest, 0F, 0F, 0F);

        head = part("head", chest, 0F, -8F, 0F);
        part("crestStem", head, 0F, -9.6F, 0F);
        part("crestBlade", head, 0F, -10F, 0F);
        // The ring of horns, open at the top, its ends curling outward.
        int n = VaultBoxes.CREST_RING;
        for (int i = 0; i < n; i++) {
            float a = (float) Math.toRadians(45D + i * 270D / (n - 1));
            ModelRenderer seg = part("ring" + i, head, RING_RADIUS * MathHelper.sin(a), RING_Y - RING_RADIUS * MathHelper.cos(a), 0F);
            seg.rotateAngleZ = a - PI / 2F;
        }
        for (int side = -1; side <= 1; side += 2) {
            float a = (float) Math.toRadians(side < 0 ? 315D : 45D);
            ModelRenderer horn = part(side < 0 ? "hornR" : "hornL", head, (RING_RADIUS + 0.4F) * MathHelper.sin(a),
                    RING_Y - (RING_RADIUS + 0.4F) * MathHelper.cos(a) + 0.3F, 0F);
            horn.rotateAngleZ = 0.75F * side;
        }

        rArm = part("upperR", chest, -5.5F, -6.6F, 0F);
        part("pauldronR", rArm, 0F, 0F, 0F);
        rFore = part("foreR", rArm, -0.5F, 4.5F, 0F);
        spear = part("spear", rFore, 0F, 6.4F, 0F);
        lArm = part("upperL", chest, 5.5F, -6.6F, 0F);
        part("pauldronL", lArm, 0F, 0F, 0F);
        lFore = part("foreL", lArm, 0.5F, 4.5F, 0F);
        shield = part("shield", lFore, 2.1F, 3F, 0F);

        rLeg = part("thighR", root, -2F, 0F, 0F);
        rShin = part("shinR", rLeg, 0F, 6F, 0F);
        lLeg = part("thighL", root, 2F, 0F, 0F);
        lShin = part("shinL", lLeg, 0F, 6F, 0F);

        ModelRenderer parent = chest;
        for (int i = 0; i < cape.length; i++) {
            cape[i] = part("cape" + i, parent, 0F, i == 0 ? -7.4F : 6F, i == 0 ? 3.1F : 0F);
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

    // ------------------------------------------------------------------ posing

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch,
                                  float scale, Entity entity) {
        if (!(entity instanceof EntityEinherjar)) {
            return;
        }
        EntityEinherjar e = (EntityEinherjar) entity;
        float t = e.clientStateTicks + partial;
        java.util.Arrays.fill(pose, 0F);
        if (e.deathTime > 0) {
            fall(pose, e.deathTime + partial, ageInTicks);
        } else {
            animate(e.getState(), t, ageInTicks, pose);
            float blend = smooth(t / blendTicks(e.getState()));
            if (blend < 1F) {
                for (int i = 0; i < N; i++) {
                    pose[i] = e.poseFrom[i] + (pose[i] - e.poseFrom[i]) * blend;
                }
            }
        }
        System.arraycopy(pose, 0, e.poseLast, 0, N);

        float[] p = pose.clone();
        // His feet, when he moves: a soldier's measured stride.
        float swing = limbSwingAmount * p[WALK];
        if (swing > 0.01F) {
            float s = MathHelper.cos(limbSwing * 0.6662F);
            float c = MathHelper.sin(limbSwing * 0.6662F);
            p[RL_X] += s * 0.75F * swing;
            p[LL_X] -= s * 0.75F * swing;
            p[R_KNEE] += Math.max(0F, -c) * 0.8F * swing;
            p[L_KNEE] += Math.max(0F, c) * 0.8F * swing;
            p[DROP] += Math.abs(c) * 0.5F * swing;
            p[CHEST_Y] += s * 0.08F * swing;
        }
        float hurt = e.hurtTime > 0 ? (e.hurtTime - partial) / 10F : 0F;
        if (hurt > 0F && e.deathTime == 0) {
            hurt *= hurt;
            p[CHEST_X] -= 0.2F * hurt;
            p[HEAD_X] -= 0.3F * hurt;
            p[LEAN] -= 0.08F * hurt;
        }
        p[HEAD_Y] += netHeadYaw * 0.017453292F * p[LOOK];
        p[HEAD_X] += headPitch * 0.017453292F * p[LOOK];
        apply(p, e);
    }

    private void apply(float[] p, EntityEinherjar e) {
        root.rotationPointY = 12F + p[DROP];
        root.rotateAngleX = p[LEAN];
        root.rotateAngleY = p[TWIST];
        root.rotateAngleZ = p[TILT];
        abdomen.rotateAngleX = p[WAIST_X];
        abdomen.rotateAngleY = p[WAIST_Y];
        chest.rotateAngleX = p[CHEST_X];
        chest.rotateAngleY = p[CHEST_Y];
        chest.rotateAngleZ = p[CHEST_Z];
        head.rotateAngleX = MathHelper.clamp(p[HEAD_X], -1F, 0.9F);
        head.rotateAngleY = MathHelper.clamp(p[HEAD_Y], -1.1F, 1.1F);
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
        rLeg.rotateAngleZ = p[RL_Z];
        rShin.rotateAngleX = Math.max(0F, p[R_KNEE]);
        lLeg.rotateAngleX = p[LL_X];
        lLeg.rotateAngleZ = p[LL_Z];
        lShin.rotateAngleX = Math.max(0F, p[L_KNEE]);
        // The spear makes the angle it is meant to in the world, however his arm is bent.
        spear.rotateAngleX = p[SPEAR] - (p[LEAN] + p[WAIST_X] + p[CHEST_X] + p[RA_X] + Math.min(0F, p[R_ELBOW]));
        spear.rotateAngleZ = p[SPEAR_Z];
        shield.rotateAngleY = p[SHIELD_Y];
        shield.rotateAngleX = p[SHIELD_X];
        // The tassets hang straight down from his belt whatever his legs do, pushed out by his thighs.
        tassetF.rotateAngleX = -p[LEAN] - p[WAIST_X] * 0.3F + Math.min(0F, Math.min(p[RL_X], p[LL_X])) * 0.7F;
        tassetB.rotateAngleX = -p[LEAN] + Math.max(0F, Math.max(p[RL_X], p[LL_X])) * 0.6F;
        float previous = 0F;
        for (int i = 0; i < cape.length; i++) {
            float angle = e.prevCape[i] + (e.cape[i] - e.prevCape[i]) * partial;
            cape[i].rotateAngleX = angle - previous;
            cape[i].rotateAngleZ = i == 0 ? (e.prevCapeSway + (e.capeSway - e.prevCapeSway) * partial) * 0.5F : 0F;
            previous = angle;
        }
        // Hung from his shoulders, it hangs plumb however he leans, and his legs push it out as they swing back.
        cape[0].rotateAngleX -= p[LEAN] + p[WAIST_X] + p[CHEST_X];
        cape[0].rotateAngleX += Math.max(0F, Math.max(p[RL_X], p[LL_X])) * 0.35F;
    }

    private static float blendTicks(State state) {
        switch (state) {
            case THRUST:
            case JAB:
            case SWEEP:
            case BASH:
                return 3F;
            case STAGGER:
                return 2F;
            case POST:
                return 10F;
            default:
                return 6F;
        }
    }

    static void animate(State state, float t, float age, float[] p) {
        switch (state) {
            case POST:
                post(p, age);
                break;
            case CHALLENGE:
                challenge(p, t, age);
                break;
            case ADVANCE:
                ready(p, age);
                break;
            case THRUST:
                thrust(p, t, age);
                break;
            case JAB:
                jab(p, t, age);
                break;
            case SWEEP:
                sweep(p, t, age);
                break;
            case BASH:
                bash(p, t, age);
                break;
            case GUARD:
                guard(p, t, age);
                break;
            case STAGGER:
                stagger(p, t, age);
                break;
        }
    }

    // ------------------------------------------------------------------ the poses themselves

    /** At attention: upright, the spear held straight up at his side, the shield at rest against his leg. */
    private static void post(float[] p, float age) {
        float breath = MathHelper.sin(age * 0.07F);
        p[CHEST_X] = -0.03F + breath * 0.012F;
        p[RA_X] = -0.32F;
        p[RA_Z] = 0.1F;
        p[R_ELBOW] = -1.05F;
        p[SPEAR] = 0F;
        p[LA_X] = 0.04F;
        p[LA_Z] = -0.12F;
        p[L_ELBOW] = -0.18F;
        p[SHIELD_Y] = 0.1F;
        p[LOOK] = 0.15F;
        p[WALK] = 1F;
    }

    /** Ready for you: low behind the shield, the spear levelled past it, the weight on the front foot. */
    private static void ready(float[] p, float age) {
        float breath = MathHelper.sin(age * 0.12F);
        p[DROP] = 0.9F + breath * 0.1F;
        p[LEAN] = 0.1F;
        p[CHEST_Y] = -0.18F;
        p[RA_X] = -0.55F;
        p[RA_Y] = 0.12F;
        p[RA_Z] = 0.18F;
        p[R_ELBOW] = -0.9F;
        p[SPEAR] = 1.42F + breath * 0.02F;
        p[LA_X] = -1.1F;
        p[LA_Y] = 1.25F;
        p[LA_Z] = 0F;
        p[L_ELBOW] = -0.47F;
        p[SHIELD_Y] = 0.3F;
        p[LL_X] = -0.32F;
        p[L_KNEE] = 0.38F;
        p[RL_X] = 0.26F;
        p[R_KNEE] = 0.28F;
        p[HEAD_X] = 0.05F;
        p[LOOK] = 0.85F;
        p[WALK] = 1F;
    }

    /** The spear lifted, its butt brought down hard on the stone, and into his guard. */
    private static void challenge(float[] p, float t, float age) {
        post(p, age);
        float lift = smooth(t / 8F) * (1F - smooth((t - EntityEinherjar.CHALLENGE_SLAM + 2F) / 2F));
        set(p, RA_X, -0.75F, lift);
        set(p, R_ELBOW, -1.55F, lift);
        set(p, HEAD_X, -0.12F, lift);
        p[CHEST_X] -= 0.08F * lift;
        float slam = clamp((t - EntityEinherjar.CHALLENGE_SLAM + 2F) / 2F) * (1F - smooth((t - EntityEinherjar.CHALLENGE_SLAM - 4F) / 6F));
        set(p, RA_X, -0.18F, slam);
        set(p, R_ELBOW, -0.9F, slam);
        p[DROP] += 0.6F * slam;
        p[R_KNEE] += 0.15F * slam;
        float[] ready = new float[N];
        ready(ready, age);
        float into = smooth((t - 20F) / 12F);
        for (int i = 0; i < N; i++) {
            p[i] += (ready[i] - p[i]) * into;
        }
        p[LOOK] = 0.6F + 0.4F * into;
    }

    /** Drawn back past his hip, then driven in with his weight behind it, the back foot left behind. */
    private static void thrust(float[] p, float t, float age) {
        ready(p, age);
        int hit = EntityEinherjar.THRUST_HIT;
        float wind = smooth(t / (hit - 3F)) * (1F - smooth((t - hit + 3F) / 2F));
        set(p, RA_X, 0.3F, wind);
        set(p, RA_Y, 0.3F, wind);
        set(p, R_ELBOW, -1.45F, wind);
        set(p, SPEAR, 1.48F, wind);
        set(p, CHEST_Y, 0.45F, wind);
        p[WAIST_Y] += 0.15F * wind;
        p[DROP] += 0.4F * wind;
        set(p, LL_X, -0.45F, wind);
        set(p, LA_X, -1.0F, wind);
        float lunge = strike(t, hit - 3F, hit, EntityEinherjar.THRUST_TICKS - 5F);
        set(p, RA_X, -1.5F, lunge);
        set(p, RA_Y, -0.05F, lunge);
        set(p, RA_Z, 0.05F, lunge);
        set(p, R_ELBOW, -0.04F, lunge);
        set(p, SPEAR, 1.6F, lunge);
        set(p, CHEST_Y, -0.42F, lunge);
        set(p, LEAN, 0.32F, lunge);
        set(p, RL_X, 0.78F, lunge);
        set(p, R_KNEE, 0.18F, lunge);
        set(p, LL_X, -0.8F, lunge);
        set(p, L_KNEE, 0.75F, lunge);
        set(p, LA_X, -0.95F, lunge);
        set(p, LA_Y, 1.05F, lunge);
        p[DROP] += 1.2F * lunge;
        p[WALK] = 1F - Math.max(wind, lunge);
    }

    /** Two quick stabs from behind the shield. */
    private static void jab(float[] p, float t, float age) {
        ready(p, age);
        float a = strike(t, EntityEinherjar.JAB_FIRST - 2.5F, EntityEinherjar.JAB_FIRST, EntityEinherjar.JAB_FIRST + 3F);
        float b = strike(t, EntityEinherjar.JAB_SECOND - 2.5F, EntityEinherjar.JAB_SECOND, EntityEinherjar.JAB_SECOND + 4F);
        float jab = Math.max(a, b);
        set(p, RA_X, -1.25F, jab);
        set(p, RA_Y, 0F, jab);
        set(p, R_ELBOW, -0.2F, jab);
        set(p, SPEAR, 1.57F, jab);
        p[CHEST_Y] -= 0.22F * jab;
        p[LEAN] += 0.1F * jab;
        p[WALK] = 1F - jab;
    }

    /** The shaft swung back to his right, then flat across in front of him. */
    private static void sweep(float[] p, float t, float age) {
        ready(p, age);
        int hit = EntityEinherjar.SWEEP_HIT;
        float wind = smooth(t / (hit - 3F)) * (1F - smooth((t - hit + 3F) / 2F));
        set(p, RA_X, -1.2F, wind);
        set(p, RA_Y, 1.05F, wind);
        set(p, R_ELBOW, -0.15F, wind);
        set(p, SPEAR, 1.57F, wind);
        set(p, CHEST_Y, 0.65F, wind);
        p[DROP] += 0.5F * wind;
        float swing = strike(t, hit - 3F, hit + 1F, EntityEinherjar.SWEEP_TICKS - 6F);
        set(p, RA_X, -1.3F, swing);
        set(p, RA_Y, -1.05F, swing);
        set(p, R_ELBOW, -0.1F, swing);
        set(p, SPEAR, 1.6F, swing);
        set(p, CHEST_Y, -0.7F, swing);
        set(p, LEAN, 0.18F, swing);
        set(p, SPEAR_Z, -0.25F, swing);
        set(p, RL_X, 0.5F, swing);
        set(p, LL_X, -0.55F, swing);
        set(p, L_KNEE, 0.6F, swing);
        p[DROP] += 0.7F * swing;
        p[WALK] = 1F - Math.max(wind, swing);
    }

    /** The shield driven forward into you, his shoulder behind it. */
    private static void bash(float[] p, float t, float age) {
        ready(p, age);
        int hit = EntityEinherjar.BASH_HIT;
        float coil = smooth(t / (hit - 2F)) * (1F - smooth((t - hit + 2F) / 2F));
        set(p, CHEST_Y, 0.35F, coil);
        set(p, LA_X, -0.95F, coil);
        p[DROP] += 0.6F * coil;
        float drive = strike(t, hit - 2F, hit, EntityEinherjar.BASH_TICKS - 4F);
        set(p, CHEST_Y, -0.55F, drive);
        set(p, LA_X, -1.3F, drive);
        set(p, LA_Y, 1.15F, drive);
        set(p, L_ELBOW, -0.3F, drive);
        set(p, SHIELD_Y, 0.45F, drive);
        set(p, LEAN, 0.32F, drive);
        set(p, LL_X, -0.75F, drive);
        set(p, L_KNEE, 0.7F, drive);
        set(p, RL_X, 0.6F, drive);
        p[DROP] += 0.8F * drive;
        p[WALK] = 1F - drive;
    }

    /** Crouched behind the shield, the spear held overhand over its rim. */
    private static void guard(float[] p, float t, float age) {
        ready(p, age);
        float in = smooth(t / 5F) * (1F - smooth((t - EntityEinherjar.GUARD_TICKS + 5F) / 5F));
        float tremor = MathHelper.sin(age * 0.9F) * 0.01F;
        p[DROP] += 1.6F * in;
        set(p, LEAN, 0.2F, in);
        set(p, LA_X, -1.2F + tremor, in);
        set(p, LA_Y, 1.3F, in);
        set(p, L_ELBOW, -0.4F, in);
        set(p, SHIELD_Y, 0.3F, in);
        set(p, RA_X, -2.45F, in);
        set(p, RA_Y, 0.15F, in);
        set(p, RA_Z, 0.1F, in);
        set(p, R_ELBOW, -0.75F, in);
        set(p, SPEAR, 1.78F, in);
        set(p, HEAD_X, 0.15F, in);
        set(p, L_KNEE, 0.7F, in);
        set(p, R_KNEE, 0.6F, in);
        p[WALK] = 1F - in * 0.6F;
    }

    /** Knocked back a step, shield flung wide, the spear's point wavering. */
    private static void stagger(float[] p, float t, float age) {
        ready(p, age);
        float hit = outCubic(clamp(t / 3F)) * (1F - smooth((t - 8F) / 10F));
        set(p, LEAN, -0.25F, hit);
        set(p, HEAD_X, -0.4F, hit);
        set(p, CHEST_X, -0.3F, hit);
        set(p, LA_X, -0.4F, hit);
        set(p, LA_Y, 0F, hit);
        set(p, LA_Z, -0.9F, hit);
        set(p, SHIELD_Y, 0F, hit);
        set(p, RA_Z, 0.6F, hit);
        set(p, SPEAR, 0.6F, hit);
        set(p, RL_X, -0.4F, hit);
        set(p, LL_X, 0.4F, hit);
        p[WALK] = 1F - hit;
    }

    /** Dying: down onto one knee, the spear's butt to the floor to hold himself up; then he falls. */
    private static void fall(float[] p, float t, float age) {
        ready(p, age);
        float kneel = outBack(clamp(t / 10F));
        set(p, DROP, 6.5F, kneel);
        set(p, RL_X, 0.2F, kneel);
        set(p, R_KNEE, 1.55F, kneel);
        set(p, LL_X, -1.3F, kneel);
        set(p, L_KNEE, 1.5F, kneel);
        set(p, HEAD_X, 0.45F, kneel);
        set(p, CHEST_X, 0.25F, kneel);
        set(p, SPEAR, 0.25F, kneel);
        set(p, RA_X, -0.7F, kneel);
        set(p, R_ELBOW, -0.5F, kneel);
        set(p, LA_X, 0.1F, kneel);
        set(p, LA_Y, 0F, kneel);
        set(p, LA_Z, -0.3F, kneel);
        set(p, SHIELD_Y, 0F, kneel);
        float topple = smooth((t - EntityEinherjar.FALL_TICKS * 0.45F) / 10F);
        p[LEAN] += 1.45F * topple;
        p[DROP] += 3F * topple;
        set(p, RA_X, -2.6F, topple);
        set(p, LA_X, -2.4F, topple);
        set(p, HEAD_X, -0.3F, topple);
        set(p, SPEAR, 1.5F, topple);
        p[WALK] = 0F;
        p[LOOK] = 0F;
    }
}
