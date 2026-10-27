package com.matoon.eternity.client.render;

import com.matoon.eternity.entity.ArmorUpgrade;
import com.matoon.eternity.entity.DestroyerBody;
import com.matoon.eternity.entity.EntityDestroyer;
import com.matoon.eternity.entity.EntityDestroyerArmor;
import com.matoon.eternity.entity.EntityDestroyer.State;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
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
 * The Destroyer, jointed like the others but built to its own scale: banded plate everywhere, a
 * broad chest with pectoral plates over it, spiked pauldrons and forearms, and a tall narrow head
 * whose face is two plates hinged at its sides. They swing open like doors when it fires its ray,
 * showing the furnace behind them, and the folded crown over its brow tips back. Its back is two
 * more, hinged at its shoulder blades over a hollow: the Destroyer Armor opens them to take someone in.
 * Boxes and texture layout from {@link VaultBoxes#DESTROYER}.
 *
 * It moves like what it is: no breath, no fidget. Dormant, it does not move at all. Awake, every
 * step lands with its whole weight, and it sways over the foot that takes it. The same body draws
 * both the vault's Destroyer and the {@link EntityDestroyerArmor}, posed from whichever it is.
 */
@SideOnly(Side.CLIENT)
public class ModelDestroyer extends ModelBase {

    static final int LEAN = 0, TWIST = 1, DROP = 2, WAIST_X = 3, WAIST_Y = 4, CHEST_X = 5, CHEST_Y = 6, CHEST_Z = 7,
            NECK_X = 8, HEAD_X = 9, HEAD_Y = 10, HEAD_Z = 11, RA_X = 12, RA_Y = 13, RA_Z = 14, R_ELBOW = 15, LA_X = 16, LA_Y = 17,
            LA_Z = 18, L_ELBOW = 19, RL_X = 20, RL_Z = 21, R_KNEE = 22, LL_X = 23, LL_Z = 24, L_KNEE = 25, LOOK = 26, WALK = 27,
            TILT = 28, CROWN = 29, R_WRIST = 30, L_WRIST = 31, N = 32;

    private final VaultBoxes.Sheet sheet = VaultBoxes.DESTROYER;
    public final ModelRenderer root, abdomen, chest, backR, backL, trapR, trapL, neck, head, faceR, faceL, crownR, crownL, rShoulder, rArm, rFore, rFist,
            lShoulder, lArm, lFore, lFist, rLeg, rShin, lLeg, lShin;
    /** The middle of its chest, and the cavity behind it once that is taken out (see {@link #hollow}). */
    private final ModelRenderer[] plug;
    private final ModelRenderer cavity;
    private final float[] pose = new float[N];
    private float partial;

    public ModelDestroyer() {
        textureWidth = sheet.width;
        textureHeight = sheet.height;

        root = part("pelvis", null, 0F, 11F, 0F);
        abdomen = part("abdomen", root, 0F, -1F, 0F);
        part("abdomenR", abdomen, 0F, 0F, 0F);
        part("abdomenL", abdomen, 0F, 0F, 0F);
        chest = part("chest", abdomen, 0F, -6.5F, 0F);
        part("chestR", chest, 0F, 0F, 0F);
        part("chestL", chest, 0F, 0F, 0F);
        plug = new ModelRenderer[]{part("plug", chest, 0F, 0F, 0F), part("plugR", chest, 0F, 0F, 0F), part("plugL", chest, 0F, 0F, 0F)};
        cavity = part("cavity", chest, 0F, 0F, 0F);
        cavity.isHidden = true;
        backR = part("backR", chest, -5.5F, -5.5F, 4.5F);
        backL = part("backL", chest, 5.5F, -5.5F, 4.5F);
        // Its traps slope from beside its head down onto its shoulders; its head sits low between them, set forward.
        trapR = part("trapR", chest, -2F, -12.4F, 0.2F);
        trapR.rotateAngleZ = -0.5F;
        trapL = part("trapL", chest, 2F, -12.4F, 0.2F);
        trapL.rotateAngleZ = 0.5F;
        neck = part("neck", chest, 0F, -8.8F, -1.4F);
        head = part("head", neck, 0F, -2F, -0.6F);
        part("headR", head, 0F, 0F, 0F);
        part("headL", head, 0F, 0F, 0F);
        part("core", head, 0F, 0F, 0F);
        faceR = part("faceR", head, -3F, 0F, -3F);
        faceL = part("faceL", head, 3F, 0F, -3F);
        crownR = part("crownR", head, 0F, -9.2F, 0F);
        crownL = part("crownL", head, 0F, -9.2F, 0F);

        rShoulder = part("shoulderR", chest, -7.5F, -7.5F, 0F);
        rArm = part("upperR", rShoulder, -1F, 2.5F, 0F);
        rFore = part("foreR", rArm, 0F, 8F, 0F);
        rFist = part("fistR", rFore, 0F, 8F, 0F);
        lShoulder = part("shoulderL", chest, 7.5F, -7.5F, 0F);
        lArm = part("upperL", lShoulder, 1F, 2.5F, 0F);
        lFore = part("foreL", lArm, 0F, 8F, 0F);
        lFist = part("fistL", lFore, 0F, 8F, 0F);

        rLeg = part("thighR", root, -3F, 0F, 0F);
        rShin = part("shinR", rLeg, 0F, 7F, 0F);
        lLeg = part("thighL", root, 3F, 0F, 0F);
        lShin = part("shinL", lLeg, 0F, 7F, 0F);
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

    /** Opens its chest to the cavity in it, or closes it up whole: the Destroyer Armor once its cradle is forged. */
    public void hollow(boolean open) {
        for (ModelRenderer part : plug) {
            part.isHidden = open;
        }
        cavity.isHidden = !open;
    }

    /** Draws it again in the pose it was last given, for its glow. */
    public void renderPose(float scale) {
        root.render(scale);
    }

    /** Moves the matrix into its chest's own space, in model space. */
    public void toChest(float scale) {
        root.postRender(scale);
        abdomen.postRender(scale);
        chest.postRender(scale);
    }

    /** Moves the matrix to the furnace in its face, in model space. */
    public void toFace(float scale) {
        root.postRender(scale);
        abdomen.postRender(scale);
        chest.postRender(scale);
        neck.postRender(scale);
        head.postRender(scale);
        GlStateManager.translate(0F, -7F * scale, -4F * scale);
    }

    // ------------------------------------------------------------------ posing

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch,
                                  float scale, Entity entity) {
        float[] poseFrom, poseLast;
        float t, blend;
        java.util.Arrays.fill(pose, 0F);
        if (entity instanceof EntityDestroyer) {
            EntityDestroyer d = (EntityDestroyer) entity;
            t = d.clientStateTicks + partial;
            animate(d, d.getState(), t, ageInTicks, pose);
            blend = smooth(t / blendTicks(d.getState()));
            poseFrom = d.poseFrom;
            poseLast = d.poseLast;
        } else if (entity instanceof EntityDestroyerArmor) {
            EntityDestroyerArmor a = (EntityDestroyerArmor) entity;
            t = a.clientStateTicks + partial;
            animateArmor(a.getState(), t, ageInTicks, pose);
            blend = smooth(t / armorBlendTicks(a.getState()));
            poseFrom = a.poseFrom;
            poseLast = a.poseLast;
        } else {
            return;
        }
        if (blend < 1F) {
            for (int i = 0; i < N; i++) {
                pose[i] = poseFrom[i] + (pose[i] - poseFrom[i]) * blend;
            }
        }
        System.arraycopy(pose, 0, poseLast, 0, N);

        float[] p = pose.clone();
        if (entity instanceof EntityDestroyerArmor) {
            air((EntityDestroyerArmor) entity, p, ageInTicks);
        }
        // Its stride: slow, long, every step landing with its whole weight, swaying over the foot that takes it.
        float swing = limbSwingAmount * p[WALK];
        if (swing > 0.01F) {
            float phase = limbSwing * 0.42F;
            float s = MathHelper.cos(phase);
            float c = MathHelper.sin(phase);
            p[RL_X] += s * 0.55F * swing;
            p[LL_X] -= s * 0.55F * swing;
            p[R_KNEE] += Math.max(0F, -c) * 0.7F * swing;
            p[L_KNEE] += Math.max(0F, c) * 0.7F * swing;
            p[RA_X] -= s * 0.22F * swing;
            p[LA_X] += s * 0.22F * swing;
            p[TILT] += s * 0.06F * swing;
            p[CHEST_Y] += s * 0.09F * swing;
            p[DROP] += (1F - Math.abs(s)) * 0.6F * swing;
        }
        if (entity instanceof EntityDestroyerArmor) {
            wounded((EntityDestroyerArmor) entity, p, limbSwing, limbSwingAmount * p[WALK], ageInTicks);
        }
        EntityLivingBase living = (EntityLivingBase) entity;
        float hurt = living.hurtTime > 0 ? (living.hurtTime - partial) / 10F : 0F;
        if (hurt > 0F) {
            p[CHEST_X] -= 0.06F * hurt * hurt;
            p[HEAD_X] -= 0.1F * hurt * hurt;
        }
        p[HEAD_Y] += netHeadYaw * 0.017453292F * p[LOOK];
        p[HEAD_X] += headPitch * 0.017453292F * p[LOOK];
        if (entity instanceof EntityDestroyer && ((EntityDestroyer) entity).getState() == State.SWEEP) {
            float aim = ((EntityDestroyer) entity).getAimYaw() * 0.017453292F;
            p[HEAD_Y] += aim * 0.6F;
            p[CHEST_Y] += aim * 0.4F;
        }
        hollow(entity instanceof EntityDestroyerArmor && ((EntityDestroyerArmor) entity).has(ArmorUpgrade.CRADLE));
        DestroyerBody body = (DestroyerBody) entity;
        apply(p, body.faceOpen(partial), body.backOpen(partial));
    }

    /**
     * The Destroyer Armor beaten down moves like it: failing, it favours its right leg, dipping as its weight comes onto
     * it, lists, and lets its head and arms sag; critical, it trembles. Whatever bursts inside it jolts all of it.
     */
    private static void wounded(EntityDestroyerArmor a, float[] p, float limbSwing, float swing, float age) {
        EntityDestroyerArmor.State state = a.getState();
        if (state == EntityDestroyerArmor.State.DOWN || state == EntityDestroyerArmor.State.EMPTY || state == EntityDestroyerArmor.State.RISE) {
            return;
        }
        int stage = a.condition().ordinal();
        if (stage >= 2) {
            float limp = stage == 3 ? 1F : 0.55F;
            float s = MathHelper.cos(limbSwing * 0.42F), c = MathHelper.sin(limbSwing * 0.42F);
            p[RL_X] -= s * 0.55F * swing * 0.45F * limp;
            p[R_KNEE] -= Math.max(0F, -c) * 0.7F * swing * 0.6F * limp;
            float weight = Math.max(0F, -s) * swing;
            p[TILT] += -0.03F * limp - weight * 0.09F * limp;
            p[DROP] += weight * 0.7F * limp;
            p[HEAD_X] += 0.1F * limp;
            p[RA_X] += 0.08F * limp;
            p[RA_Z] += 0.05F * limp;
        }
        if (stage == 3) {
            p[CHEST_Z] += MathHelper.sin(age * 2.7F) * 0.008F;
            p[HEAD_Z] += MathHelper.sin(age * 3.3F) * 0.012F;
        }
        float jolt = a.jolt;
        if (jolt > 0.01F) {
            p[CHEST_X] -= 0.14F * jolt;
            p[HEAD_X] -= 0.3F * jolt * MathHelper.sin(age * 3F);
            p[HEAD_Z] += 0.18F * jolt * MathHelper.sin(age * 4.1F);
            p[RA_Z] -= 0.25F * jolt;
            p[LA_Z] += 0.25F * jolt;
            p[DROP] += 0.7F * jolt;
        }
    }

    private void apply(float[] p, float open, float back) {
        root.rotationPointY = 11F + p[DROP];
        root.rotateAngleX = p[LEAN];
        root.rotateAngleY = p[TWIST];
        root.rotateAngleZ = p[TILT];
        abdomen.rotateAngleX = p[WAIST_X];
        abdomen.rotateAngleY = p[WAIST_Y];
        chest.rotateAngleX = p[CHEST_X];
        chest.rotateAngleY = p[CHEST_Y];
        chest.rotateAngleZ = p[CHEST_Z];
        neck.rotateAngleX = p[NECK_X];
        head.rotateAngleX = MathHelper.clamp(p[HEAD_X], -0.9F, 0.8F);
        head.rotateAngleY = MathHelper.clamp(p[HEAD_Y], -1.2F, 1.2F);
        head.rotateAngleZ = p[HEAD_Z];
        // The face plates swing out on their hinges; the crown tips back off its brow.
        float swing = outBack(open) * 1.35F;
        // Shut, they meet in a shallow prow behind the ridge down its face.
        faceR.rotateAngleY = -0.22F + swing;
        faceL.rotateAngleY = 0.22F - swing;
        // The plates of its back swing out on hinges at its shoulder blades, like doors.
        float doors = outBack(back) * 1.75F;
        backR.rotateAngleY = -doors;
        backL.rotateAngleY = doors;
        crownR.rotateAngleZ = 0.24F;
        crownL.rotateAngleZ = -0.24F;
        crownR.rotateAngleX = crownL.rotateAngleX = p[CROWN] - 0.35F * smooth(open);
        rArm.rotateAngleX = p[RA_X];
        rArm.rotateAngleY = p[RA_Y];
        rArm.rotateAngleZ = p[RA_Z];
        rFore.rotateAngleX = Math.min(0F, p[R_ELBOW]);
        rFist.rotateAngleX = p[R_WRIST];
        lArm.rotateAngleX = p[LA_X];
        lArm.rotateAngleY = p[LA_Y];
        lArm.rotateAngleZ = p[LA_Z];
        lFore.rotateAngleX = Math.min(0F, p[L_ELBOW]);
        lFist.rotateAngleX = p[L_WRIST];
        rLeg.rotateAngleX = p[RL_X];
        rLeg.rotateAngleZ = p[RL_Z];
        rShin.rotateAngleX = Math.max(0F, p[R_KNEE]);
        lLeg.rotateAngleX = p[LL_X];
        lLeg.rotateAngleZ = p[LL_Z];
        lShin.rotateAngleX = Math.max(0F, p[L_KNEE]);
    }

    private static float blendTicks(State state) {
        switch (state) {
            case PUNCH:
            case BACKHAND:
            case STOMP:
                return 5F;
            case STAGGER:
                return 3F;
            case WAKE:
            case DORMANT:
                return 1F;
            default:
                return 8F;
        }
    }

    static void animate(EntityDestroyer d, State state, float t, float age, float[] p) {
        switch (state) {
            case DORMANT:
                dormant(p);
                break;
            case WAKE:
                wake(p, t, age);
                break;
            case STALK:
                stalk(p, age);
                break;
            case PUNCH:
                punch(p, t, age);
                break;
            case BACKHAND:
                backhand(p, t, age);
                break;
            case HAMMER:
                hammer(p, t, age, EntityDestroyer.HAMMER_HIT);
                break;
            case STOMP:
                stomp(p, t, age);
                break;
            case RAY:
                ray(p, t, age, EntityDestroyer.RAY_FIRE, EntityDestroyer.RAY_END);
                break;
            case SWEEP:
                ray(p, t, age, EntityDestroyer.SWEEP_FIRE, EntityDestroyer.SWEEP_END);
                break;
            case STAGGER:
                stagger(p, t, age);
                break;
            case FALL:
                fall(p, t);
                break;
            case WRECK:
                fall(p, EntityDestroyer.FALL_TICKS);
                break;
        }
    }

    // ------------------------------------------------------------------ the Destroyer Armor

    private static float armorBlendTicks(EntityDestroyerArmor.State state) {
        switch (state) {
            case PUNCH_R:
            case PUNCH_L:
                return 3F;
            case RISE:
                return 1F;
            case EMPTY:
            case DOWN:
                return 16F;
            default:
                return 8F;
        }
    }

    /** Its own body, worn: empty and waiting, rising out of its wreck, taking someone in, and fighting as they ask. */
    static void animateArmor(EntityDestroyerArmor.State state, float t, float age, float[] p) {
        switch (state) {
            case EMPTY:
                empty(p);
                break;
            case RISE:
                fall(p, EntityDestroyer.FALL_TICKS * (1F - clamp(t / EntityDestroyerArmor.RISE_TICKS)));
                break;
            case ENTER: {
                empty(p);
                float[] ready = new float[N];
                stalk(ready, age);
                float into = smooth((t - EntityDestroyerArmor.ENTER_SHUT) / 12F);
                for (int i = 0; i < N; i++) {
                    p[i] += (ready[i] - p[i]) * into;
                }
                // Its back slams shut on them and it shudders all through.
                float shut = clamp((t - EntityDestroyerArmor.ENTER_SHUT) / 2F) * (1F - smooth((t - EntityDestroyerArmor.ENTER_SHUT - 2F) / 8F));
                p[DROP] += 0.6F * shut;
                p[CHEST_X] += MathHelper.sin(age * 3.7F) * 0.03F * shut;
                break;
            }
            case EXIT: {
                stalk(p, age);
                float[] still = new float[N];
                empty(still);
                float out = smooth(t / 14F);
                for (int i = 0; i < N; i++) {
                    p[i] += (still[i] - p[i]) * out;
                }
                break;
            }
            case PUNCH_R:
                jab(p, t, age, true);
                break;
            case PUNCH_L:
                jab(p, t, age, false);
                break;
            case SLAM:
                hammer(p, t, age, EntityDestroyerArmor.SLAM_HIT);
                break;
            case RAY:
                ray(p, t, age, EntityDestroyerArmor.RAY_OPEN, Integer.MAX_VALUE / 2);
                p[WALK] = 0.5F;
                break;
            case VENT:
                vent(p, t, age);
                break;
            case IMPLODE:
                fall(p, Math.min(t * 2.5F, 40F));
                break;
            case DOWN:
                fall(p, Math.min(t, 40F));
                break;
            default:
                stalk(p, age);
                break;
        }
    }

    /**
     * The armour in the air, laid over whatever it is doing. Flying, it hangs on its jets like a man standing on
     * nothing, one knee bent, arms down and out with its palms to the ground, bobbing; as it gathers speed it
     * leans into it until it lies almost flat, arms swept back along its sides and legs trailing straight, its
     * head raised to see where it goes; it banks into its turns and rears back to climb. It crouches as it
     * launches and as it lands. Leaping or falling, its arms go out for balance and its knees come up.
     * A punch, slam or ray in flight keeps its own arms.
     */
    private void air(EntityDestroyerArmor a, float[] p, float age) {
        float w = a.flight(partial), up = a.airborne(partial);
        if (w < 0.001F && up < 0.001F) {
            return;
        }
        EntityDestroyerArmor.State state = a.getState();
        boolean busy = state == EntityDestroyerArmor.State.PUNCH_R || state == EntityDestroyerArmor.State.PUNCH_L
                || state == EntityDestroyerArmor.State.SLAM || state == EntityDestroyerArmor.State.RAY || state == EntityDestroyerArmor.State.RAY_END;
        if (w > 0.001F) {
            float lean = a.lean(partial), bank = a.bank(partial), climb = a.climb(partial);
            float speed = clamp((lean - 0.1F) / 1F);
            float hover = 1F - speed;
            float[] f = new float[N];
            f[LOOK] = p[LOOK];
            f[CROWN] = p[CROWN];
            f[LEAN] = lean;
            f[TILT] = bank;
            f[CHEST_Z] = bank * 0.15F;
            f[HEAD_Z] = -bank * 0.5F;
            f[CHEST_X] = 0.05F - 0.12F * climb * hover;
            f[NECK_X] = -0.3F * lean;
            f[HEAD_X] = -0.55F * lean;
            f[DROP] = MathHelper.sin(age * 0.1F) * 0.6F * hover;
            // Arms: down and out with its palms to the ground hovering; swept back along its sides at speed.
            float flutter = MathHelper.sin(age * 1.7F) * 0.025F * speed;
            f[RA_X] = lerp(-0.2F, 0.6F, speed) + 0.15F * climb * hover;
            f[LA_X] = lerp(-0.2F, 0.6F, speed) + 0.15F * climb * hover;
            f[RA_Z] = lerp(0.45F, 0.17F, speed) + flutter;
            f[LA_Z] = -lerp(0.45F, 0.17F, speed) - flutter;
            f[R_ELBOW] = lerp(-0.45F, -0.1F, speed);
            f[L_ELBOW] = lerp(-0.45F, -0.1F, speed);
            f[R_WRIST] = lerp(0.35F, 0F, speed);
            f[L_WRIST] = lerp(0.35F, 0F, speed);
            // Legs: one knee bent, the other hanging, swaying a little; trailing straight and together at speed.
            float sway = MathHelper.sin(age * 0.07F);
            f[RL_X] = lerp(-0.15F + 0.06F * sway, 0.12F, speed);
            f[LL_X] = lerp(0.12F + 0.06F * MathHelper.sin(age * 0.07F + 1.5F), 0.06F, speed);
            f[R_KNEE] = lerp(0.55F, 0.12F, speed);
            f[L_KNEE] = lerp(0.28F, 0.08F, speed);
            f[RL_Z] = lerp(0.07F, 0.03F, speed);
            f[LL_Z] = -lerp(0.07F, 0.03F, speed);
            f[WALK] = 0F;
            for (int i = 0; i < N; i++) {
                if (busy && arm(i)) {
                    continue;
                }
                p[i] += (f[i] - p[i]) * w;
            }
            // Launching and landing: a deep crouch as it takes its weight onto its jets, or off them.
            float crouch = MathHelper.sin((float) Math.PI * clamp(w * 1.4F)) * (w < 0.999F ? 1F : 0F);
            p[DROP] += 2.4F * crouch;
            p[R_KNEE] += 0.9F * crouch;
            p[L_KNEE] += 0.9F * crouch;
            p[RL_X] -= 0.5F * crouch;
            p[LL_X] -= 0.5F * crouch;
            p[LEAN] += 0.3F * crouch;
            if (!busy) {
                p[RA_X] += 0.5F * crouch;
                p[LA_X] += 0.5F * crouch;
            }
        }
        if (up > 0.001F) {
            // Leaping or falling: arms out for balance, knees drawn up.
            float k = up * (1F - w);
            if (!busy) {
                p[RA_Z] += (0.7F - p[RA_Z]) * k;
                p[LA_Z] += (-0.7F - p[LA_Z]) * k;
                p[RA_X] += (-0.35F - p[RA_X]) * k;
                p[LA_X] += (-0.35F - p[LA_X]) * k;
            }
            p[R_KNEE] += (0.6F - p[R_KNEE]) * k;
            p[L_KNEE] += (0.35F - p[L_KNEE]) * k;
            p[RL_X] += (-0.35F - p[RL_X]) * k;
            p[LL_X] += (0.1F - p[LL_X]) * k;
            p[WALK] *= 1F - k;
        }
    }

    private static boolean arm(int i) {
        return i == RA_X || i == RA_Y || i == RA_Z || i == R_ELBOW || i == R_WRIST || i == LA_X || i == LA_Y || i == LA_Z || i == L_ELBOW
                || i == L_WRIST;
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    /** Empty: upright, its head bowed, its arms hanging a little away from its sides, its back standing open. */
    private static void empty(float[] p) {
        dormant(p);
        p[HEAD_X] = 0.15F;
        p[NECK_X] = 0.1F;
        p[RA_X] = -0.1F;
        p[RA_Z] = 0.22F;
        p[LA_X] = -0.1F;
        p[LA_Z] = -0.22F;
        p[R_KNEE] = 0.08F;
        p[L_KNEE] = 0.08F;
        p[DROP] = 0.3F;
    }

    /** One fist drawn back and driven straight out, the other pulled in to its chest; quick, for a body this size. */
    private static void jab(float[] p, float t, float age, boolean right) {
        stalk(p, age);
        int hit = EntityDestroyerArmor.PUNCH_HIT;
        float s = right ? 1F : -1F;
        int ax = right ? RA_X : LA_X, ay = right ? RA_Y : LA_Y, az = right ? RA_Z : LA_Z, elbow = right ? R_ELBOW : L_ELBOW;
        int other = right ? LA_X : RA_X, otherElbow = right ? L_ELBOW : R_ELBOW;
        float wind = smooth(t / (hit - 3F)) * (1F - smooth((t - hit + 3F) / 2F));
        set(p, ax, 0.25F, wind);
        set(p, az, 0.4F * s, wind);
        set(p, elbow, -1.8F, wind);
        set(p, CHEST_Y, 0.4F * s, wind);
        float blow = strike(t, hit - 3F, hit, EntityDestroyerArmor.PUNCH_TICKS - 3F);
        set(p, ax, -1.55F, blow);
        set(p, ay, -0.12F * s, blow);
        set(p, az, 0.05F * s, blow);
        set(p, elbow, -0.1F, blow);
        set(p, CHEST_Y, -0.45F * s, blow);
        set(p, LEAN, 0.12F, blow);
        float guard = Math.max(wind, blow);
        set(p, other, -0.55F, guard);
        set(p, otherElbow, -1.4F, guard);
        p[DROP] += 0.5F * blow;
        p[WALK] = 1F - 0.5F * guard;
    }

    /** Overheated: bowed over, arms hanging, the heat shuddering out of it through its back. */
    private static void vent(float[] p, float t, float age) {
        stalk(p, age);
        float in = smooth(t / 8F) * (1F - smooth((t - EntityDestroyerArmor.VENT_TICKS + 10F) / 10F));
        float shudder = MathHelper.sin(age * 2.9F) * 0.02F * in;
        set(p, CHEST_X, 0.3F, in);
        set(p, HEAD_X, 0.35F, in);
        set(p, RA_X, 0.05F, in);
        set(p, LA_X, 0.05F, in);
        set(p, R_ELBOW, -0.15F, in);
        set(p, L_ELBOW, -0.15F, in);
        p[CHEST_X] += shudder;
        p[RA_Z] += shudder * 2F;
        p[LA_Z] -= shudder * 2F;
        p[DROP] += 0.6F * in;
    }

    // ------------------------------------------------------------------ the poses themselves

    /** A statue in its niche: upright, arms at its sides, fists closed, its head level and blind. */
    private static void dormant(float[] p) {
        p[RA_Z] = 0.1F;
        p[LA_Z] = -0.1F;
        p[R_ELBOW] = -0.08F;
        p[L_ELBOW] = -0.08F;
        p[LOOK] = 0F;
    }

    /** A shudder through every plate, its head coming up and round, its fists closing, and a first step. */
    private static void wake(float[] p, float t, float age) {
        dormant(p);
        float shudder = smooth((t - 4F) / 4F) * (1F - smooth((t - EntityDestroyer.WAKE_GLOW) / 6F));
        float buzz = MathHelper.sin(age * 3.7F) * 0.025F * shudder;
        p[CHEST_X] += buzz;
        p[RA_Z] += buzz * 2F;
        p[LA_Z] -= buzz * 2F;
        p[HEAD_Z] += buzz;
        float lift = smooth((t - EntityDestroyer.WAKE_GLOW) / 10F);
        p[HEAD_X] = -0.12F * lift;
        p[LOOK] = smooth((t - EntityDestroyer.WAKE_LOOK) / 14F);
        float clench = smooth((t - 40F) / 8F);
        set(p, R_ELBOW, -0.35F, clench);
        set(p, L_ELBOW, -0.35F, clench);
        set(p, RA_Z, 0.18F, clench);
        set(p, LA_Z, -0.18F, clench);
        p[CHEST_X] += 0.05F * clench;
        float stepUp = smooth((t - EntityDestroyer.WAKE_STEP + 10F) / 6F) * (1F - smooth((t - EntityDestroyer.WAKE_STEP + 1F) / 3F));
        p[RL_X] -= 0.55F * stepUp;
        p[R_KNEE] += 0.75F * stepUp;
        p[TILT] -= 0.04F * stepUp;
        float landed = smooth((t - EntityDestroyer.WAKE_STEP) / 3F) * (1F - smooth((t - EntityDestroyer.WAKE_STEP - 4F) / 10F));
        p[DROP] += 0.8F * landed;
        float[] stalk = new float[N];
        stalk(stalk, age);
        float into = smooth((t - EntityDestroyer.WAKE_STEP - 4F) / 16F);
        for (int i = 0; i < N; i++) {
            p[i] += (stalk[i] - p[i]) * into;
        }
    }

    /** Hunched a little forward, arms hanging ready, fists half closed, its head fixed on you. */
    private static void stalk(float[] p, float age) {
        p[CHEST_X] = 0.07F;
        p[NECK_X] = 0.08F;
        p[HEAD_X] = 0.05F;
        p[RA_X] = -0.12F;
        p[RA_Z] = 0.2F;
        p[R_ELBOW] = -0.38F;
        p[LA_X] = -0.12F;
        p[LA_Z] = -0.2F;
        p[L_ELBOW] = -0.38F;
        p[RL_Z] = 0.03F;
        p[LL_Z] = -0.03F;
        p[LOOK] = 1F;
        p[WALK] = 1F;
    }

    /** The right fist drawn back by its ear, then through you with its whole turning weight. */
    private static void punch(float[] p, float t, float age) {
        stalk(p, age);
        int hit = EntityDestroyer.PUNCH_HIT;
        float wind = smooth(t / (hit - 4F)) * (1F - smooth((t - hit + 4F) / 3F));
        set(p, RA_X, 0.35F, wind);
        set(p, RA_Z, 0.55F, wind);
        set(p, R_ELBOW, -1.95F, wind);
        set(p, CHEST_Y, 0.55F, wind);
        p[WAIST_Y] += 0.2F * wind;
        p[DROP] += 0.6F * wind;
        set(p, LA_X, -0.6F, wind);
        set(p, L_ELBOW, -1.1F, wind);
        float blow = strike(t, hit - 4F, hit, EntityDestroyer.PUNCH_TICKS - 8F);
        set(p, RA_X, -1.6F, blow);
        set(p, RA_Y, -0.1F, blow);
        set(p, RA_Z, 0.05F, blow);
        set(p, R_ELBOW, -0.1F, blow);
        set(p, CHEST_Y, -0.5F, blow);
        set(p, LEAN, 0.16F, blow);
        set(p, LL_X, -0.5F, blow);
        set(p, L_KNEE, 0.45F, blow);
        set(p, RL_X, 0.38F, blow);
        set(p, LA_X, 0.25F, blow);
        p[DROP] += 1F * blow;
        p[WALK] = 1F - Math.max(wind, blow);
    }

    /** The left arm swung across its chest, then flung out, the back of the fist leading. */
    private static void backhand(float[] p, float t, float age) {
        stalk(p, age);
        int hit = EntityDestroyer.BACKHAND_HIT;
        float wind = smooth(t / (hit - 4F)) * (1F - smooth((t - hit + 4F) / 3F));
        set(p, LA_X, -1.2F, wind);
        set(p, LA_Y, 1.15F, wind);
        set(p, L_ELBOW, -1.3F, wind);
        set(p, CHEST_Y, 0.6F, wind);
        p[DROP] += 0.5F * wind;
        float swing = strike(t, hit - 4F, hit + 1F, EntityDestroyer.BACKHAND_TICKS - 8F);
        set(p, LA_X, -1.45F, swing);
        set(p, LA_Y, -1.05F, swing);
        set(p, L_ELBOW, -0.12F, swing);
        set(p, CHEST_Y, -0.65F, swing);
        set(p, TILT, 0.08F, swing);
        set(p, RL_X, -0.3F, swing);
        set(p, LL_X, 0.25F, swing);
        p[WALK] = 1F - Math.max(wind, swing);
    }

    /** Both fists locked over its head, rising onto its toes; then down into the stone, the whole body following them. */
    private static void hammer(float[] p, float t, float age, int hit) {
        stalk(p, age);
        float raise = smooth(t / (hit - 6F)) * (1F - smooth((t - hit + 4F) / 3F));
        set(p, RA_X, -2.95F, raise);
        set(p, RA_Y, -0.45F, raise);
        set(p, RA_Z, 0.05F, raise);
        set(p, R_ELBOW, -0.55F, raise);
        set(p, LA_X, -2.95F, raise);
        set(p, LA_Y, 0.45F, raise);
        set(p, LA_Z, -0.05F, raise);
        set(p, L_ELBOW, -0.55F, raise);
        set(p, LEAN, -0.16F, raise);
        set(p, HEAD_X, -0.25F, raise);
        p[DROP] -= 1.2F * raise;
        float slam = outCubic(clamp((t - hit + 3F) / 3F)) * (1F - smooth((t - hit - 10F) / 12F));
        set(p, RA_X, -0.85F, slam);
        set(p, RA_Y, -0.4F, slam);
        set(p, R_ELBOW, -0.05F, slam);
        set(p, LA_X, -0.85F, slam);
        set(p, LA_Y, 0.4F, slam);
        set(p, L_ELBOW, -0.05F, slam);
        set(p, LEAN, 0.42F, slam);
        set(p, WAIST_X, 0.38F, slam);
        set(p, HEAD_X, 0.15F, slam);
        set(p, RL_X, -0.55F, slam);
        set(p, R_KNEE, 1F, slam);
        set(p, LL_X, -0.55F, slam);
        set(p, L_KNEE, 1F, slam);
        p[DROP] += 4F * slam;
        p[WALK] = 1F - Math.max(raise, slam);
    }

    /** A foot lifted high and driven into the stone, its arms out to balance it. */
    private static void stomp(float[] p, float t, float age) {
        stalk(p, age);
        int hit = EntityDestroyer.STOMP_HIT;
        float lift = smooth(t / (hit - 3F)) * (1F - smooth((t - hit + 3F) / 2F));
        set(p, RL_X, -1.15F, lift);
        set(p, R_KNEE, 1.4F, lift);
        set(p, RA_Z, 0.65F, lift);
        set(p, LA_Z, -0.65F, lift);
        set(p, TILT, -0.07F, lift);
        set(p, LEAN, -0.08F, lift);
        p[DROP] -= 0.6F * lift;
        float down = outCubic(clamp((t - hit + 3F) / 2F)) * (1F - smooth((t - hit - 4F) / 10F));
        set(p, RL_X, -0.25F, down);
        set(p, R_KNEE, 0.25F, down);
        set(p, LEAN, 0.15F, down);
        set(p, LL_X, 0.1F, down);
        set(p, L_KNEE, 0.4F, down);
        p[DROP] += 1.4F * down;
        p[WALK] = 1F - Math.max(lift, down);
    }

    /** Braced wide, arms back and out, its head thrust at you while the ray pours out of it, shaking with it. */
    private static void ray(float[] p, float t, float age, int fire, int end) {
        stalk(p, age);
        float brace = smooth(t / 12F) * (1F - smooth((t - end - 6F) / 12F));
        set(p, RL_Z, 0.16F, brace);
        set(p, LL_Z, -0.16F, brace);
        set(p, R_KNEE, 0.35F, brace);
        set(p, L_KNEE, 0.35F, brace);
        set(p, RL_X, -0.15F, brace);
        set(p, LL_X, 0.12F, brace);
        set(p, RA_X, 0.3F, brace);
        set(p, RA_Z, 0.5F, brace);
        set(p, R_ELBOW, -0.7F, brace);
        set(p, LA_X, 0.3F, brace);
        set(p, LA_Z, -0.5F, brace);
        set(p, L_ELBOW, -0.7F, brace);
        set(p, NECK_X, 0.2F, brace);
        set(p, CHEST_X, -0.05F, brace);
        p[DROP] += 0.9F * brace;
        float firing = smooth((t - fire) / 2F) * (1F - smooth((t - end) / 4F));
        float shake = MathHelper.sin(age * 2.9F) * 0.02F * firing;
        p[LEAN] -= 0.07F * firing;
        p[HEAD_X] += shake;
        p[CHEST_X] += shake * 0.5F;
        p[RA_Z] += 0.1F * firing;
        p[LA_Z] -= 0.1F * firing;
        p[LOOK] = 1F;
        p[WALK] = 1F - brace;
    }

    /** Rocked back on its heels, arms thrown out; then it rights itself. */
    private static void stagger(float[] p, float t, float age) {
        stalk(p, age);
        float hit = outCubic(clamp(t / 4F)) * (1F - smooth((t - 10F) / 14F));
        set(p, LEAN, -0.22F, hit);
        set(p, HEAD_X, -0.35F, hit);
        set(p, CHEST_X, -0.2F, hit);
        set(p, RA_Z, 0.8F, hit);
        set(p, LA_Z, -0.8F, hit);
        set(p, RA_X, -0.5F, hit);
        set(p, LA_X, -0.4F, hit);
        set(p, RL_X, -0.35F, hit);
        set(p, R_KNEE, 0.4F, hit);
        p[WALK] = 1F - hit;
    }

    /** Down onto both knees, its arms hanging; then on its face. */
    private static void fall(float[] p, float t) {
        dormant(p);
        float kneel = outBack(clamp(t / EntityDestroyer.FALL_KNEE));
        set(p, DROP, 9F, kneel);
        set(p, RL_X, -0.05F, kneel);
        set(p, R_KNEE, 1.55F, kneel);
        set(p, LL_X, -0.05F, kneel);
        set(p, L_KNEE, 1.55F, kneel);
        set(p, HEAD_X, 0.5F, kneel);
        set(p, CHEST_X, 0.2F, kneel);
        set(p, RA_Z, 0.25F, kneel);
        set(p, LA_Z, -0.25F, kneel);
        float topple = smooth((t - EntityDestroyer.FALL_TICKS + 20F) / 8F);
        float bounce = topple * MathHelper.sin(clamp((t - EntityDestroyer.FALL_TICKS + 12F) / 6F) * (float) Math.PI) * 0.06F;
        p[LEAN] += 1.48F * topple - bounce;
        p[DROP] += 3.5F * topple;
        set(p, RA_X, -2.8F, topple);
        set(p, RA_Z, 0.4F, topple);
        set(p, LA_X, -2.6F, topple);
        set(p, LA_Z, -0.5F, topple);
        set(p, HEAD_X, 0F, topple);
        set(p, HEAD_Y, 0.5F, topple);
        p[LOOK] = 0F;
        p[WALK] = 0F;
    }
}
