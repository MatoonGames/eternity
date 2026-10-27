package com.matoon.eternity.client.render;

import com.matoon.eternity.entity.EntityEternity;
import com.matoon.eternity.entity.EntityEternity.Attack;
import com.matoon.eternity.entity.EntityEternity.State;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

import static com.matoon.eternity.client.render.EternityPose.*;

/**
 * Eternity, jointed: a long gaunt body (pelvis, waist, ribs, the broad line of the shoulders,
 * a spine down his back), a narrow skull with the mask of his face on it (heavy brow, long
 * nose, cheekbones, chin), a tall squared hood with a mantle across his shoulders, the two
 * great crests standing out of it in jointed segments that curl into hooks, long arms with
 * narrow hands and two-jointed splinter fingers, lean legs with pointed feet, and a cloak in
 * five segments down his back with three more hanging from each shoulder, which lift with his
 * arms like wings. Skinned from {@link EternityBoxes} (see tools/EternitySkin.java).
 *
 * Every frame his pose is built from scratch by {@link EternityPose} out of his synced state
 * and attack and how far into them he is, with easing on everything he does: a long gathering
 * before, a snap through, the weight of it after. Whenever his state changes he blends out of
 * whatever pose he was last drawn in, so nothing ever pops. His cloak, its shoulder panels and
 * his crests hang on springs the entity swings, so they trail and settle after him.
 */
@SideOnly(Side.CLIENT)
public class ModelEternity extends ModelBase {

    public final ModelRenderer root, abdomen, chest, neck, head, hood;
    public final ModelRenderer[] upper = new ModelRenderer[2], fore = new ModelRenderer[2], hand = new ModelRenderer[2];
    public final ModelRenderer[][] finger = new ModelRenderer[2][5], fingerTip = new ModelRenderer[2][5];
    public final ModelRenderer[] thigh = new ModelRenderer[2], shin = new ModelRenderer[2], foot = new ModelRenderer[2];
    public final ModelRenderer[] crest = new ModelRenderer[2];
    public final ModelRenderer[][] crestUp = new ModelRenderer[2][EternityBoxes.CREST_UP], crestDown = new ModelRenderer[2][EternityBoxes.CREST_DOWN];
    public final ModelRenderer[] cape = new ModelRenderer[EternityBoxes.CAPE];
    public final ModelRenderer[][] panel = new ModelRenderer[2][EternityBoxes.PANEL];

    private final float[] pose = new float[N];
    private float partial;

    /** Resting curl of each crest segment, outward positive: the stalk nearly straight, then the hook. */
    private static final float[] UP_REST = {0.12F, -0.06F, -0.08F, -0.75F, -0.85F, -0.7F};
    private static final float[] DOWN_REST = {0.1F, 0.55F, 0.7F, 0.65F};

    public ModelEternity() {
        textureWidth = EternityBoxes.SIZE;
        textureHeight = EternityBoxes.SIZE;

        root = part(null, "pelvis", 0F, 1.5F, 0F);
        abdomen = part(root, "abdomen", 0F, -1.5F, 0F);
        chest = part(abdomen, "chest", 0F, -5.5F, 0F);
        neck = part(chest, "neck", 0F, -9.5F, 0F);
        head = part(neck, "head", 0F, -2.8F, -0.3F);
        extra(head, "headR");
        extra(head, "headL");
        hood = part(chest, "hood", 0F, -9.5F, 0.5F);
        extra(hood, "hoodR");
        extra(hood, "hoodL");

        for (int side = 0; side < 2; side++) {
            String k = side == 0 ? "R" : "L";
            float s = side == 0 ? -1F : 1F;

            crest[side] = part(hood, "crest" + k, s * 5.5F, -9.5F, 1.5F);
            ModelRenderer parent = crest[side];
            for (int i = 0; i < EternityBoxes.CREST_UP; i++) {
                float at = i == 0 ? 0F : -(new int[]{4, 4, 3, 3, 3, 2}[i - 1] - 0.4F);
                crestUp[side][i] = part(parent, "crestUp" + i + k, i == 0 ? s * 4.5F : 0F, at, 0F);
                parent = crestUp[side][i];
            }
            parent = crest[side];
            for (int i = 0; i < EternityBoxes.CREST_DOWN; i++) {
                float at = i == 0 ? 0F : new int[]{4, 3, 3}[i - 1] - 0.4F;
                crestDown[side][i] = part(parent, "crestDown" + i + k, i == 0 ? s * 4.5F : 0F, at, 0F);
                parent = crestDown[side][i];
            }

            ModelRenderer shoulder = part(chest, "shoulder" + k, s * 6.3F, -8.3F, 0F);
            upper[side] = shoulder;
            extra(shoulder, "upper" + k);
            fore[side] = part(shoulder, "fore" + k, 0F, 9.5F, 0F);
            hand[side] = part(fore[side], "hand" + k, 0F, 8.8F, 0F);
            for (int f = 0; f < 4; f++) {
                finger[side][f] = part(hand[side], "finger" + f + k, 0F, 2.6F, -1.1F + f * 0.73F);
                fingerTip[side][f] = part(finger[side][f], "fingerTip" + f + k, 0F, 2.55F, 0F);
            }
            finger[side][4] = part(hand[side], "thumb" + k, -s * 0.35F, 0.8F, -1.2F);
            fingerTip[side][4] = part(finger[side][4], "thumbTip" + k, 0F, 2.6F, 0F);

            thigh[side] = part(root, "thigh" + k, s * 2.3F, 0F, 0F);
            shin[side] = part(thigh[side], "shin" + k, 0F, 10.5F, 0F);
            foot[side] = part(shin[side], "foot" + k, 0F, 10.5F, 0F);

            parent = chest;
            for (int i = 0; i < EternityBoxes.PANEL; i++) {
                panel[side][i] = part(parent, "panel" + i + k, i == 0 ? s * 5.6F : 0F, i == 0 ? -9.2F : 6.6F, i == 0 ? 2.6F : 0F);
                parent = panel[side][i];
            }
        }
        ModelRenderer parent = chest;
        for (int i = 0; i < EternityBoxes.CAPE; i++) {
            cape[i] = part(parent, "cape" + i, 0F, i == 0 ? -9.1F : 5.7F, i == 0 ? 3.1F : 0F);
            parent = cape[i];
        }
    }

    private ModelRenderer part(@Nullable ModelRenderer parent, String name, float x, float y, float z) {
        ModelRenderer part = new ModelRenderer(this, name);
        part.setRotationPoint(x, y, z);
        extra(part, name);
        if (parent != null) {
            parent.addChild(part);
        }
        return part;
    }

    private void extra(ModelRenderer part, String name) {
        for (EternityBoxes.Box b : EternityBoxes.of(name)) {
            part.cubeList.add(new ModelBox(part, b.u, b.v, b.x, b.y, b.z, b.w, b.h, b.d, b.delta, false));
        }
    }

    // ------------------------------------------------------------------ drawing

    public void renderAll(float scale) {
        root.render(scale);
    }

    /** Everything but his cloak, for the layers of open space inside him. */
    public void renderBody(float scale) {
        cape[0].showModel = false;
        for (int side = 0; side < 2; side++) {
            panel[side][0].showModel = false;
        }
        root.render(scale);
        cape[0].showModel = true;
        for (int side = 0; side < 2; side++) {
            panel[side][0].showModel = true;
        }
    }

    /** How brightly his eyes burn in the current pose, 0 to 1. */
    public float glow() {
        return pose[GLOW];
    }

    /** Moves the matrix into a part, through every part above it. */
    public void into(float scale, ModelRenderer... chain) {
        for (ModelRenderer part : chain) {
            part.postRender(scale);
        }
    }

    public ModelRenderer[] toHand(int side) {
        return new ModelRenderer[]{root, abdomen, chest, upper[side], fore[side], hand[side]};
    }

    public ModelRenderer[] toHead() {
        return new ModelRenderer[]{root, abdomen, chest, neck, head};
    }

    public ModelRenderer[] toChest() {
        return new ModelRenderer[]{root, abdomen, chest};
    }

    public ModelRenderer[] toCrestTip(int side, boolean up) {
        ModelRenderer[] hook = up ? crestUp[side] : crestDown[side];
        ModelRenderer[] chain = new ModelRenderer[5 + hook.length];
        chain[0] = root;
        chain[1] = abdomen;
        chain[2] = chest;
        chain[3] = hood;
        chain[4] = crest[side];
        System.arraycopy(hook, 0, chain, 5, hook.length);
        return chain;
    }

    public ModelRenderer[] toFinger(int side, int f) {
        return new ModelRenderer[]{root, abdomen, chest, upper[side], fore[side], hand[side], finger[side][f], fingerTip[side][f]};
    }

    public ModelRenderer[] toHem(int which) {
        if (which == 0) {
            ModelRenderer[] chain = new ModelRenderer[3 + cape.length];
            chain[0] = root;
            chain[1] = abdomen;
            chain[2] = chest;
            System.arraycopy(cape, 0, chain, 3, cape.length);
            return chain;
        }
        int side = which - 1;
        ModelRenderer[] chain = new ModelRenderer[3 + EternityBoxes.PANEL];
        chain[0] = root;
        chain[1] = abdomen;
        chain[2] = chest;
        System.arraycopy(panel[side], 0, chain, 3, EternityBoxes.PANEL);
        return chain;
    }

    // ------------------------------------------------------------------ posing

    /** What the pose library calls whatever he is doing. */
    public static int kind(State state, @Nullable Attack attack) {
        switch (state) {
            case EMERGE:
                return EMERGE;
            case STARE:
                return STARE;
            case HUNT:
                return HUNT;
            case DEPART:
                return DEPART;
            case DYING:
                return DYING;
            case CHANNEL:
                if (attack == null) {
                    return HUNT;
                }
                switch (attack) {
                    case UNMAKE:
                        return UNMAKE;
                    case SINGULARITY:
                        return SINGULARITY;
                    case LANCES:
                        return LANCES;
                    case STARFALL:
                        return STARFALL;
                    default:
                        return SHATTER;
                }
            default:
                return DRIFT;
        }
    }

    /**
     * Poses him.
     *
     * @param entity  his body, for blending, recoil and the springs of his cloak and crests; null for
     *                the figure seen across space or in the Regulator
     * @param attack  what he is gathering, when there is no entity to ask
     * @param headYaw degrees his head is turned from his body
     * @param pitch   degrees he looks down
     */
    public void pose(@Nullable EntityEternity entity, State state, @Nullable Attack attack, float ticks, float age, float headYaw,
                     float pitch, float partialTicks) {
        partial = partialTicks;
        if (entity != null && state == State.CHANNEL) {
            attack = entity.getAttack();
        }
        int kind = kind(state, state == State.CHANNEL ? attack : null);
        float pitchRad = MathHelper.clamp(pitch * 0.017453292F, -0.8F, 0.8F);
        EternityPose.animate(kind, ticks, attack != null ? attack.ticks : 0F, age, pitchRad, pose);
        if (entity != null) {
            float blend = EternityPose.smooth(ticks / EternityPose.blendTicks(kind));
            if (blend < 1F) {
                for (int i = 0; i < N; i++) {
                    pose[i] = entity.poseFrom[i] + (pose[i] - entity.poseFrom[i]) * blend;
                }
            }
            System.arraycopy(pose, 0, entity.poseLast, 0, N);
        }
        float[] p = pose.clone();
        if (entity != null && entity.hurtTime > 0) {
            // Struck, he recoils, whatever he is doing.
            float hurt = (entity.hurtTime - partial) / 10F;
            hurt *= hurt;
            p[CHEST_X] -= 0.25F * hurt;
            p[HEAD_X] -= 0.35F * hurt;
            p[LEAN] -= 0.12F * hurt;
            p[RA_Z] += 0.25F * hurt;
            p[LA_Z] += 0.25F * hurt;
            p[R_SPREAD] += 0.4F * hurt;
            p[L_SPREAD] += 0.4F * hurt;
        }
        p[HEAD_Y] += MathHelper.clamp(headYaw * 0.017453292F, -1.1F, 1.1F) * p[LOOK];
        p[HEAD_X] += pitchRad * 0.7F * p[LOOK];
        apply(p, entity, age);
    }

    private void apply(float[] p, @Nullable EntityEternity e, float age) {
        root.rotationPointY = 1.5F + p[DROP];
        set(root, p[LEAN], p[TWIST], p[TILT]);
        set(abdomen, p[WAIST_X], p[WAIST_Y], 0F);
        set(chest, p[CHEST_X], p[CHEST_Y], p[CHEST_Z]);
        neck.rotateAngleX = p[NECK_X] + p[HEAD_X] * 0.3F;
        set(head, MathHelper.clamp(p[HEAD_X] * 0.7F, -1.2F, 1F), MathHelper.clamp(p[HEAD_Y], -1.1F, 1.1F), p[HEAD_Z]);
        // His hood turns with his head only partly: his face moves inside it.
        set(hood, p[HEAD_X] * 0.2F + p[NECK_X] * 0.5F, MathHelper.clamp(p[HEAD_Y], -1.1F, 1.1F) * 0.45F, p[HEAD_Z] * 0.4F);

        for (int side = 0; side < 2; side++) {
            float s = side == 0 ? -1F : 1F;
            int a = side == 0 ? RA_X : LA_X, l = side == 0 ? RL_X : LL_X;
            // Channels are written for his right side; his left mirrors them.
            set(upper[side], p[a], -s * p[a + 1], -s * p[a + 2]);
            fore[side].rotateAngleX = Math.min(0F, p[a + 3]);
            fore[side].rotateAngleY = -s * p[a + 5];
            hand[side].rotateAngleZ = s * p[a + 4];
            float curl = p[a + 6], spread = 0.08F + p[a + 7];
            for (int f = 0; f < 4; f++) {
                // The fingers never quite still: each moves a moment after the one before.
                float ripple = MathHelper.sin(age * 0.07F + f * 0.9F + side * 2F) * 0.07F * (1F - Math.min(1F, curl));
                finger[side][f].rotateAngleX = (f - 1.5F) * (spread * 1.3F + 0.04F);
                finger[side][f].rotateAngleZ = s * (curl * (0.75F + f * 0.08F) + ripple);
                fingerTip[side][f].rotateAngleZ = s * (curl * 1.1F + ripple * 1.5F);
            }
            finger[side][4].rotateAngleX = -0.6F - spread;
            finger[side][4].rotateAngleZ = s * (0.35F + curl * 0.6F);
            fingerTip[side][4].rotateAngleZ = s * curl * 0.8F;

            set(thigh[side], p[l], -s * p[l + 1], -s * p[l + 2]);
            shin[side].rotateAngleX = Math.max(0F, p[l + 3]);
            foot[side].rotateAngleX = p[l + 4];

            // The crests: flaring with his power, swinging on their springs, curling into their hooks.
            float spring = e != null ? lerp(e.prevCrest[side], e.crest[side]) : 0F;
            float sway = MathHelper.sin(age * 0.031F + side * 1.7F) * 0.025F;
            set(crest[side], p[CRESTS] * 0.12F, 0F, -s * (p[CRESTS] * 0.18F + sway + spring * 0.4F));
            for (int i = 0; i < EternityBoxes.CREST_UP; i++) {
                float flare = i >= 3 ? p[CRESTS] * 0.12F : 0F;
                crestUp[side][i].rotateAngleZ = -s * (UP_REST[i] + flare + spring * 0.15F * i);
            }
            for (int i = 0; i < EternityBoxes.CREST_DOWN; i++) {
                crestDown[side][i].rotateAngleZ = -s * (DOWN_REST[i] + p[CRESTS] * 0.08F * i - spring * 0.12F * i);
            }

            // The shoulder panels lift outward as his arm does and follow it forward, then hang on their springs.
            float out = Math.max(0F, p[a + 2]) * 0.85F + p[CLOAK_OPEN] * 0.2F;
            float swing = Math.min(0F, p[a]) * 0.4F;
            for (int i = 0; i < EternityBoxes.PANEL; i++) {
                float hang = e != null ? lerp(e.prevPanel[side][i], e.panel[side][i]) : 0.08F + 0.05F * i;
                if (i == 0) {
                    set(panel[side][i], swing + hang - p[CHEST_X] * 0.5F - p[WAIST_X] * 0.5F, 0F, -s * out);
                } else {
                    float above = e != null ? lerp(e.prevPanel[side][i - 1], e.panel[side][i - 1]) : 0F;
                    set(panel[side][i], hang - above, 0F, -s * out * 0.15F);
                }
            }
        }

        // The cloak down his back, each segment swinging toward the one above it.
        float previous = 0F;
        for (int i = 0; i < cape.length; i++) {
            float angle = e != null ? lerp(e.prevCape[i], e.cape[i]) : 0.1F + p[CLOAK_LIFT] * 0.6F + 0.02F * i;
            cape[i].rotateAngleX = angle - previous;
            cape[i].rotateAngleZ = i == 0 && e != null ? lerp(e.prevCapeSway, e.capeSway) * 0.5F : 0F;
            previous = angle;
        }
        // It hangs from his shoulders, so bending forward swings it with him.
        cape[0].rotateAngleX -= p[WAIST_X] * 0.6F + p[CHEST_X] * 0.6F + p[LEAN] * 0.6F;
    }

    private float lerp(float prev, float now) {
        return prev + (now - prev) * partial;
    }

    private static void set(ModelRenderer part, float x, float y, float z) {
        part.rotateAngleX = x;
        part.rotateAngleY = y;
        part.rotateAngleZ = z;
    }

    /** Draws a part's chain, moving the matrix as it goes, for a point to be found in it. */
    public static void locate(float scale, ModelRenderer[] chain, float x, float y, float z) {
        for (ModelRenderer part : chain) {
            part.postRender(scale);
        }
        GlStateManager.translate(x * scale, y * scale, z * scale);
    }
}
