package com.matoon.eternity.client.render;

import com.matoon.eternity.entity.EntityChampion;
import com.matoon.eternity.entity.EntityChampion.State;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The Champion of the Universe, jointed for fighting and built like a mountain: a waist that
 * bends and twists, elbows and knees; a barrel chest with traps and lats standing out from it,
 * arms thicker than most men's legs, gold cuffs on his wrists, the high gold collar, the
 * championship belt and its black plate, red boots, the stone in his brow, and a great red
 * mane in eight locks that the entity swings on springs. Skinned in a 128x128 layout of its own
 * (see tools/ChampionSkin.java), which this file's box offsets must match. Drawn at 1.6 times
 * this size.
 * <p>
 * Every frame his pose is built from scratch out of his synced state, how far into it he is
 * (at the tempo his temper set it going at), and how he is moving: anticipation before every
 * blow, a snap through, follow-through after, a boxer's bounce and footwork in between. When his
 * state changes he blends out of the pose he was last drawn in, so nothing ever pops.
 */
@SideOnly(Side.CLIENT)
public class ModelChampion extends ModelBase {

    // The pose, as one array so it can be blended and kept on the entity. The same layout as Adam Warlock's.
    static final int LEAN = 0, TWIST = 1, TILT = 2, DROP = 3, WAIST_X = 4, WAIST_Y = 5, CHEST_X = 6, CHEST_Y = 7, CHEST_Z = 8,
            HEAD_X = 9, HEAD_Y = 10, HEAD_Z = 11, RA_X = 12, RA_Y = 13, RA_Z = 14, R_ELBOW = 15, LA_X = 16, LA_Y = 17, LA_Z = 18,
            L_ELBOW = 19, RL_X = 20, RL_Y = 21, RL_Z = 22, R_KNEE = 23, LL_X = 24, LL_Y = 25, LL_Z = 26, L_KNEE = 27, LOOK = 28, N = 29;

    private static final float PI = (float) Math.PI;

    public final ModelRenderer root, abdomen, chest, head, rArm, rFore, lArm, lFore, rLeg, rShin, lLeg, lShin;
    public final ModelRenderer[] mane = new ModelRenderer[EntityChampion.MANE];
    private final float[] pose = new float[N];
    private float partial;

    public ModelChampion() {
        textureWidth = 128;
        textureHeight = 128;

        root = new ModelRenderer(this);
        root.setRotationPoint(0F, 12F, 0F);

        abdomen = box(root, 0, 16, -4.5F, -5F, -2.5F, 9, 5, 5);
        box(abdomen, 28, 16, -5F, -2F, -3F, 10, 3, 6);
        box(abdomen, 114, 16, -2F, -2.6F, -3.7F, 4, 3, 1);

        chest = box(abdomen, 0, 0, -6F, -8F, -3.5F, 12, 8, 7);
        chest.setRotationPoint(0F, -5F, 0F);
        box(chest, 60, 16, -6F, -7.8F, -4.3F, 12, 4, 1);
        box(chest, 60, 21, -4F, -9.3F, -1.6F, 8, 2, 4);
        ModelRenderer latR = box(chest, 86, 16, -7.6F, -7.4F, -2F, 2, 5, 5);
        latR.rotateAngleZ = -0.12F;
        ModelRenderer latL = box(chest, 86, 16, 5.6F, -7.4F, -2F, 2, 5, 5);
        latL.rotateAngleZ = 0.12F;
        // The high gold collar, standing up behind his head and flaring at the sides.
        ModelRenderer collar = box(chest, 98, 0, -5F, -5F, 0F, 10, 5, 1);
        collar.setRotationPoint(0F, -8F, 2.4F);
        collar.rotateAngleX = -0.3F;
        ModelRenderer collarR = box(chest, 114, 6, -0.5F, -4F, -2.5F, 1, 4, 5);
        collarR.setRotationPoint(-4.4F, -8F, 0.4F);
        collarR.rotateAngleZ = 0.32F;
        ModelRenderer collarL = box(chest, 114, 6, -0.5F, -4F, -2.5F, 1, 4, 5);
        collarL.setRotationPoint(4.4F, -8F, 0.4F);
        collarL.rotateAngleZ = -0.32F;

        head = box(chest, 38, 0, -3.5F, -7F, -3.5F, 7, 7, 7);
        head.setRotationPoint(0F, -8.6F, -0.8F);
        box(head, 98, 6, -3.5F, -5.2F, -4.1F, 7, 1, 1);
        ModelRenderer stone = new ModelRenderer(this, 120, 0);
        stone.cubeList.add(new ModelBox(stone, 120, 0, -0.5F, -6.5F, -4.3F, 1, 1, 1, 0.2F));
        head.addChild(stone);
        // The mane: a crown swept back off his forehead, full at the sides, then the long locks behind.
        ModelRenderer crown = box(head, 66, 0, -4F, -1.6F, -4.4F, 8, 3, 8);
        crown.setRotationPoint(0F, -7.2F, 0.3F);
        crown.rotateAngleX = -0.16F;
        box(head, 100, 16, -4.5F, -7F, -2F, 1, 5, 6);
        box(head, 100, 16, 3.5F, -7F, -2F, 1, 5, 6);
        ModelRenderer parent = head;
        for (int i = 0; i < mane.length; i++) {
            boolean tip = i >= mane.length - 2;
            int width = tip ? 8 : 10;
            mane[i] = box(parent, tip ? 24 : 0, 42, -width / 2F, 0F, -1F, width, 3, 2);
            mane[i].setRotationPoint(0F, i == 0 ? -7.4F : 2.3F, i == 0 ? 3.4F : 0F);
            parent = mane[i];
        }

        rArm = box(chest, 0, 30, -2.5F, -1.5F, -2.5F, 5, 7, 5);
        rArm.setRotationPoint(-8.2F, -6.6F, 0F);
        box(rArm, 84, 30, -3F, -2.5F, -3F, 6, 3, 6);
        rFore = box(rArm, 20, 30, -2.5F, 0F, -2.5F, 5, 6, 5);
        rFore.setRotationPoint(0F, 5F, 0F);
        box(rFore, 40, 30, -3F, 2.6F, -3F, 6, 3, 6);
        box(rFore, 64, 30, -2.5F, 5.6F, -2.5F, 5, 3, 5);

        lArm = box(chest, 0, 30, -2.5F, -1.5F, -2.5F, 5, 7, 5);
        lArm.setRotationPoint(8.2F, -6.6F, 0F);
        box(lArm, 84, 30, -3F, -2.5F, -3F, 6, 3, 6);
        lFore = box(lArm, 20, 30, -2.5F, 0F, -2.5F, 5, 6, 5);
        lFore.setRotationPoint(0F, 5F, 0F);
        box(lFore, 40, 30, -3F, 2.6F, -3F, 6, 3, 6);
        box(lFore, 64, 30, -2.5F, 5.6F, -2.5F, 5, 3, 5);

        rLeg = box(root, 44, 42, -2.5F, 0F, -2.5F, 5, 6, 5);
        rLeg.setRotationPoint(-2.7F, 0F, 0F);
        rShin = box(rLeg, 64, 42, -2.5F, 0F, -2.5F, 5, 6, 5);
        rShin.setRotationPoint(0F, 6F, 0F);
        box(rShin, 84, 42, -3F, 2F, -3F, 6, 4, 6);
        box(rShin, 28, 25, -3F, 4F, -5F, 6, 2, 2);

        lLeg = box(root, 44, 42, -2.5F, 0F, -2.5F, 5, 6, 5);
        lLeg.setRotationPoint(2.7F, 0F, 0F);
        lShin = box(lLeg, 64, 42, -2.5F, 0F, -2.5F, 5, 6, 5);
        lShin.setRotationPoint(0F, 6F, 0F);
        box(lShin, 84, 42, -3F, 2F, -3F, 6, 4, 6);
        box(lShin, 28, 25, -3F, 4F, -5F, 6, 2, 2);
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
        GlStateManager.translate(0F, 7.6F * scale, 0F);
    }

    /** Moves the matrix to the stone in his brow, in model space. */
    public void toBrow(float scale) {
        root.postRender(scale);
        abdomen.postRender(scale);
        chest.postRender(scale);
        head.postRender(scale);
        GlStateManager.translate(0F, -6F * scale, -4.6F * scale);
    }

    // ------------------------------------------------------------------ posing

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch,
                                  float scale, Entity entity) {
        if (!(entity instanceof EntityChampion)) {
            return;
        }
        EntityChampion c = (EntityChampion) entity;
        float t = (c.clientStateTicks + partial) * c.getTempo();
        float pitch = headPitch * 0.017453292F;
        float rage = c.prevShownRage + (c.shownRage - c.prevShownRage) * partial;
        float walk = c.prevWalkAmount + (c.walkAmount - c.prevWalkAmount) * partial;
        float phase = c.prevWalkPhase + (c.walkPhase - c.prevWalkPhase) * partial;
        java.util.Arrays.fill(pose, 0F);
        animate(c, c.getState(), t, ageInTicks, pitch, rage, pose);
        footwork(c.getState(), pose, walk, phase, t);

        // Out of whatever he was doing before, never straight into the new pose.
        float blend = smooth(t / blendTicks(c.getState()));
        if (blend < 1F) {
            for (int i = 0; i < N; i++) {
                pose[i] = c.poseFrom[i] + (pose[i] - c.poseFrom[i]) * blend;
            }
        }
        System.arraycopy(pose, 0, c.poseLast, 0, N);

        float[] p = pose.clone();
        // Struck, he barely rocks.
        float hurt = c.hurtTime > 0 ? (c.hurtTime - partial) / 10F : 0F;
        if (hurt > 0F) {
            hurt = hurt * hurt;
            p[CHEST_X] -= 0.12F * hurt;
            p[HEAD_X] -= 0.25F * hurt;
            p[HEAD_Z] += 0.1F * hurt;
        }
        // In a fury he shakes with it.
        if (rage > 0.6F && c.getState() != State.DRIFT && c.getState() != State.KNOCKDOWN) {
            float shake = (rage - 0.6F) * 0.06F;
            p[CHEST_Z] += MathHelper.sin(ageInTicks * 2.9F) * shake;
            p[HEAD_Z] += MathHelper.sin(ageInTicks * 3.7F) * shake;
            p[RA_Z] += MathHelper.sin(ageInTicks * 3.3F) * shake;
            p[LA_Z] -= MathHelper.sin(ageInTicks * 3.1F) * shake;
        }
        p[HEAD_Y] += netHeadYaw * 0.017453292F * p[LOOK];
        p[HEAD_X] += pitch * p[LOOK];
        apply(p, c);
    }

    private void apply(float[] p, EntityChampion c) {
        root.rotationPointY = 12F + p[DROP];
        root.rotateAngleX = p[LEAN];
        root.rotateAngleY = p[TWIST];
        root.rotateAngleZ = p[TILT];
        abdomen.rotateAngleX = p[WAIST_X];
        abdomen.rotateAngleY = p[WAIST_Y];
        chest.rotateAngleX = p[CHEST_X];
        chest.rotateAngleY = p[CHEST_Y];
        chest.rotateAngleZ = p[CHEST_Z];
        head.rotateAngleX = MathHelper.clamp(p[HEAD_X], -1.2F, 1.0F);
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
        rLeg.rotateAngleY = p[RL_Y];
        rLeg.rotateAngleZ = p[RL_Z];
        rShin.rotateAngleX = Math.max(0F, p[R_KNEE]);
        lLeg.rotateAngleX = p[LL_X];
        lLeg.rotateAngleY = p[LL_Y];
        lLeg.rotateAngleZ = p[LL_Z];
        lShin.rotateAngleX = Math.max(0F, p[L_KNEE]);
        float previous = 0F;
        for (int i = 0; i < mane.length; i++) {
            float angle = c.prevMane[i] + (c.mane[i] - c.prevMane[i]) * partial;
            mane[i].rotateAngleX = angle - previous;
            mane[i].rotateAngleZ = i == 0 ? (c.prevManeSway + (c.maneSway - c.prevManeSway) * partial) * 0.4F : 0F;
            previous = angle;
        }
        // The mane hangs from his head, so whatever bends him swings it with him; it falls the way the world pulls it.
        mane[0].rotateAngleX -= (p[HEAD_X] + p[CHEST_X] + p[WAIST_X] + p[LEAN]) * 0.85F;
    }

    private static float blendTicks(State state) {
        switch (state) {
            case GUARD:
            case SLIP:
                return 1.5F;
            case JAB_CROSS:
            case HOOK:
            case UPPERCUT:
                return 3F;
            case IMPACT:
            case KNOCKDOWN:
                return 2F;
            case IDLE:
                return 7F;
            case DRIFT:
                return 14F;
            default:
                return 5F;
        }
    }

    /** The pose for a state, t beats into it. */
    static void animate(EntityChampion c, State state, float t, float age, float pitch, float rage, float[] p) {
        switch (state) {
            case IDLE:
                guard(p, age, rage);
                break;
            case CHALLENGE:
                challenge(p, t, age, pitch);
                break;
            case FLEX:
                flex(p, t, age);
                break;
            case JAB_CROSS:
                jabCross(p, t, age, pitch, rage);
                break;
            case HOOK:
                hook(p, t, age, rage);
                break;
            case UPPERCUT:
                uppercut(p, t, age, rage);
                break;
            case HAYMAKER:
                haymaker(p, t, age, rage);
                break;
            case GRAB:
                grab(p, t, age, rage, c.getHeld() >= 0 || t < EntityChampion.GRAB_REACH);
                break;
            case RUSH:
                rush(p, t, age, rage);
                break;
            case POUND:
                pound(p, t, age, rage);
                break;
            case COUNTER:
                counter(p, t, age, rage, c.counterTick >= 0 ? (c.clientStateTicks - c.counterTick) * c.getTempo() : -1F);
                break;
            case ROAR:
                roar(p, t, age, rage);
                break;
            case HAMMER:
                hammer(p, t, age, rage);
                break;
            case BREAKER:
                breaker(p, t, age);
                break;
            case IMPACT:
                impact(p, t, age);
                break;
            case GUARD:
                block(p, t, age, rage);
                break;
            case SLIP:
                slip(p, t, age, rage, c.getEntityId() % 2 == 0 ? 1F : -1F);
                break;
            case KNOCKDOWN:
                knockdown(p, t, age);
                break;
            case RISE:
                rise(p, t, age);
                break;
            case DRIFT:
                drift(p, age);
                break;
            case BOARD:
                board(p, t, age);
                break;
        }
    }

    /**
     * Footwork over whatever his arms and body are doing: a stride when he moves, a boxer's
     * bounce when he stands, his shoulders swinging against his hips.
     */
    private static void footwork(State state, float[] p, float walk, float phase, float t) {
        if (walk < 0.01F) {
            return;
        }
        float w = Math.min(1.2F, walk);
        float s = MathHelper.sin(phase), co = MathHelper.cos(phase);
        float stride = state == State.RUSH ? 0.95F : 0.6F;
        p[RL_X] += s * stride * w;
        p[LL_X] -= s * stride * w;
        p[R_KNEE] += Math.max(0F, -MathHelper.sin(phase + 0.6F)) * 0.9F * w;
        p[L_KNEE] += Math.max(0F, MathHelper.sin(phase + 0.6F)) * 0.9F * w;
        p[DROP] += (Math.abs(co) * 0.9F - 0.3F) * w;
        p[CHEST_Y] += s * 0.08F * w;
        p[WAIST_Y] -= s * 0.06F * w;
        p[TILT] += co * 0.03F * w;
        if (state == State.RUSH) {
            // Pumping arms, head down, a runaway wall.
            p[LA_X] -= s * 0.5F * w;
        }
    }

    // ------------------------------------------------------------------ the poses themselves

    /**
     * His guard: up on his toes, chin tucked behind his shoulder, fists up by his face, left
     * foot forward, bouncing. The angrier he is the lower he hunches and the harder he breathes.
     */
    static void guard(float[] p, float age, float rage) {
        float bounce = Math.abs(MathHelper.sin(age * (0.16F + 0.08F * rage)));
        float breath = MathHelper.sin(age * (0.08F + 0.06F * rage));
        p[DROP] = 0.6F + bounce * 0.7F + rage * 0.8F;
        p[LEAN] = 0.08F + 0.06F * rage;
        p[WAIST_X] = 0.1F + 0.06F * rage;
        p[CHEST_X] = 0.04F + breath * (0.03F + 0.03F * rage) + 0.08F * rage;
        p[CHEST_Y] = -0.22F;
        p[WAIST_Y] = -0.1F;
        p[HEAD_X] = 0.14F + 0.06F * rage;
        p[HEAD_Y] = 0.18F;
        // Fists up by his face, the lead (left) a little out in front.
        p[RA_X] = -0.72F + breath * 0.03F;
        p[RA_Y] = -0.38F;
        p[RA_Z] = 0.12F;
        p[R_ELBOW] = -2.15F;
        p[LA_X] = -0.95F - breath * 0.03F;
        p[LA_Y] = 0.32F;
        p[LA_Z] = -0.12F;
        p[L_ELBOW] = -1.95F;
        // Left foot forward, knees soft, weight on the balls of his feet.
        p[LL_X] = -0.32F;
        p[L_KNEE] = 0.42F + bounce * 0.15F;
        p[LL_Z] = -0.06F;
        p[RL_X] = 0.28F;
        p[R_KNEE] = 0.34F + bounce * 0.15F;
        p[RL_Z] = 0.08F;
        p[RL_Y] = 0.25F;
        p[LOOK] = 0.85F;
    }

    /** Tall and proud: a finger levelled at you, then his fists against his chest, twice, then arms wide. */
    private static void challenge(float[] p, float t, float age, float pitch) {
        guard(p, age, 0F);
        float tall = smooth(t / 10F) * (1F - smooth((t - 64F) / 8F));
        set(p, DROP, -0.4F, tall);
        set(p, LEAN, -0.05F, tall);
        set(p, CHEST_X, -0.14F, tall);
        set(p, CHEST_Y, 0F, tall);
        set(p, WAIST_Y, 0F, tall);
        set(p, HEAD_X, -0.12F, tall);
        set(p, HEAD_Y, 0F, tall);
        set(p, LL_X, -0.1F, tall);
        set(p, RL_X, 0.1F, tall);
        set(p, RL_Y, 0F, tall);
        set(p, L_KNEE, 0.1F, tall);
        set(p, R_KNEE, 0.1F, tall);
        set(p, RA_X, 0.05F, tall);
        set(p, RA_Y, 0F, tall);
        set(p, RA_Z, 0.25F, tall);
        set(p, R_ELBOW, -0.3F, tall);
        set(p, LA_X, 0.05F, tall);
        set(p, LA_Y, 0F, tall);
        set(p, LA_Z, -0.25F, tall);
        set(p, L_ELBOW, -0.3F, tall);
        // "You."
        float point = outBack(clamp((t - 10F) / 5F)) * (1F - smooth((t - 28F) / 6F));
        set(p, RA_X, -1.52F + pitch, point);
        set(p, RA_Y, -0.1F, point);
        set(p, RA_Z, 0F, point);
        set(p, R_ELBOW, -0.04F, point);
        p[CHEST_Y] -= 0.25F * point;
        p[HEAD_X] += 0.06F * point;
        // Fists to his chest, left, right.
        float beat = smooth((t - 32F) / 5F) * (1F - smooth((t - 54F) / 4F));
        float left = pulse(t, 42F), right = pulse(t, 50F);
        set(p, RA_X, -1.15F, beat);
        set(p, RA_Y, -0.95F, beat);
        set(p, RA_Z, 0F, beat);
        set(p, R_ELBOW, -1.55F - 0.35F * (1F - right), beat);
        set(p, LA_X, -1.15F, beat);
        set(p, LA_Y, 0.95F, beat);
        set(p, LA_Z, 0F, beat);
        set(p, L_ELBOW, -1.55F - 0.35F * (1F - left), beat);
        p[CHEST_X] += (left + right) * 0.08F * beat;
        p[HEAD_X] -= (left + right) * 0.05F * beat;
        // Arms flung wide: come on, then.
        float wide = outBack(clamp((t - 54F) / 6F)) * (1F - smooth((t - 66F) / 6F));
        set(p, RA_X, -0.3F, wide);
        set(p, RA_Y, 0F, wide);
        set(p, RA_Z, 1.15F, wide);
        set(p, R_ELBOW, -0.4F, wide);
        set(p, LA_X, -0.3F, wide);
        set(p, LA_Y, 0F, wide);
        set(p, LA_Z, -1.15F, wide);
        set(p, L_ELBOW, -0.4F, wide);
        set(p, HEAD_X, -0.32F, wide);
        set(p, CHEST_X, -0.22F, wide);
        p[LOOK] = 1F - 0.6F * wide;
    }

    /** A front double biceps, then into a most-muscular crunch, trembling with the effort of being this magnificent. */
    private static void flex(float[] p, float t, float age) {
        guard(p, age, 0F);
        float up = outBack(clamp((t - 2F) / 8F)) * (1F - smooth((t - 30F) / 6F));
        float tremor = MathHelper.sin(age * 2.6F) * 0.025F;
        set(p, DROP, -0.3F, up);
        set(p, LEAN, -0.04F, up);
        set(p, CHEST_X, -0.2F, up);
        set(p, CHEST_Y, 0F, up);
        set(p, WAIST_Y, 0F, up);
        set(p, HEAD_X, -0.1F, up);
        set(p, HEAD_Y, 0F, up);
        set(p, RA_X, -1.57F + tremor, up);
        set(p, RA_Y, 1.38F, up);
        set(p, RA_Z, 0.1F, up);
        set(p, R_ELBOW, -1.95F - tremor, up);
        set(p, LA_X, -1.57F - tremor, up);
        set(p, LA_Y, -1.38F, up);
        set(p, LA_Z, -0.1F, up);
        set(p, L_ELBOW, -1.95F + tremor, up);
        set(p, LL_X, -0.05F, up);
        set(p, LL_Z, -0.18F, up);
        set(p, RL_X, 0.05F, up);
        set(p, RL_Z, 0.18F, up);
        set(p, RL_Y, 0F, up);
        set(p, L_KNEE, 0.15F, up);
        set(p, R_KNEE, 0.15F, up);
        // Then down into a crunch: fists together in front of his belt, every muscle bunched.
        float crunch = smooth((t - 32F) / 6F) * (1F - smooth((t - 58F) / 8F));
        set(p, DROP, 1.4F, crunch);
        set(p, LEAN, 0.22F, crunch);
        set(p, WAIST_X, 0.2F, crunch);
        set(p, CHEST_X, 0.25F + tremor, crunch);
        set(p, HEAD_X, 0.05F, crunch);
        set(p, RA_X, -0.55F, crunch);
        set(p, RA_Y, -0.65F, crunch);
        set(p, RA_Z, 0.35F + tremor, crunch);
        set(p, R_ELBOW, -1.35F, crunch);
        set(p, LA_X, -0.55F, crunch);
        set(p, LA_Y, 0.65F, crunch);
        set(p, LA_Z, -0.35F - tremor, crunch);
        set(p, L_ELBOW, -1.35F, crunch);
        set(p, L_KNEE, 0.5F, crunch);
        set(p, R_KNEE, 0.5F, crunch);
        p[LOOK] = 1F;
    }

    /** Jab, jab, cross: the lead hand snapping out twice, then the right, all his weight turning behind it. */
    private static void jabCross(float[] p, float t, float age, float pitch, float rage) {
        guard(p, age, rage);
        float aim = pitch * 0.6F;
        for (float contact : new float[]{EntityChampion.JAB_1, EntityChampion.JAB_2}) {
            float jab = strike(t, contact - 2F, contact, contact + 3F);
            set(p, LA_X, -1.58F + aim, jab);
            set(p, LA_Y, 0.06F, jab);
            set(p, LA_Z, 0F, jab);
            set(p, L_ELBOW, -0.04F, jab);
            p[CHEST_Y] += 0.3F * jab;
            p[WAIST_Y] += 0.08F * jab;
            p[LEAN] += 0.06F * jab;
            p[HEAD_Y] -= 0.15F * jab;
        }
        int cross = EntityChampion.CROSS;
        // Loaded onto the back foot before it.
        float load = smooth((t - cross + 6F) / 3F) * (1F - smooth((t - cross + 2F) / 2F));
        p[CHEST_Y] += 0.25F * load;
        p[DROP] += 0.6F * load;
        float hit = strike(t, cross - 2.5F, cross, cross + 4F);
        set(p, RA_X, -1.58F + aim, hit);
        set(p, RA_Y, -0.06F, hit);
        set(p, RA_Z, 0F, hit);
        set(p, R_ELBOW, -0.03F, hit);
        p[CHEST_Y] -= 0.5F * hit;
        p[WAIST_Y] -= 0.16F * hit;
        p[LEAN] += 0.16F * hit;
        p[HEAD_Y] += 0.2F * hit;
        set(p, RL_Y, -0.35F, hit);
        p[R_KNEE] += 0.25F * hit;
        p[LL_X] -= 0.2F * hit;
        // The left comes back to cover his chin.
        set(p, LA_X, -0.85F, hit);
        set(p, LA_Y, 0.55F, hit);
        set(p, L_ELBOW, -2.2F, hit);
    }

    /** Down and to the side, then the left hook whips round, low, into your liver. */
    private static void hook(float[] p, float t, float age, float rage) {
        guard(p, age, rage);
        int hit = EntityChampion.HOOK_HIT;
        // The step in, dipping.
        float dip = smooth(t / 7F) * (1F - smooth((t - hit - 6F) / 8F));
        p[DROP] += 2.2F * dip;
        p[WAIST_X] += 0.25F * dip;
        p[L_KNEE] += 0.5F * dip;
        p[R_KNEE] += 0.4F * dip;
        p[TILT] -= 0.12F * dip;
        // Winding it back.
        float wind = smooth((t - 4F) / 6F) * (1F - smooth((t - hit + 3F) / 2F));
        set(p, LA_X, -0.9F, wind);
        set(p, LA_Y, -0.7F, wind);
        set(p, LA_Z, -0.5F, wind);
        set(p, L_ELBOW, -1.6F, wind);
        p[CHEST_Y] += 0.32F * wind;
        p[WAIST_Y] += 0.12F * wind;
        // Whipping through.
        float whip = strike(t, hit - 3F, hit, hit + 5F);
        set(p, LA_X, -1.15F, whip);
        set(p, LA_Y, 0.85F, whip);
        set(p, LA_Z, -0.2F, whip);
        set(p, L_ELBOW, -1.5F, whip);
        p[CHEST_Y] -= 0.48F * whip;
        p[WAIST_Y] -= 0.16F * whip;
        p[HEAD_Y] += 0.25F * whip;
        set(p, LL_Y, 0.4F, whip);
        p[LOOK] = 0.7F;
    }

    /** Coiled down low, the right fist dropped to his hip; then up, through you, onto his toes. */
    private static void uppercut(float[] p, float t, float age, float rage) {
        guard(p, age, rage);
        int hit = EntityChampion.UPPER_HIT;
        float coil = smooth(t / 8F) * (1F - smooth((t - hit + 2.5F) / 2F));
        p[DROP] += 3F * coil;
        p[WAIST_X] += 0.3F * coil;
        p[R_KNEE] += 0.75F * coil;
        p[L_KNEE] += 0.55F * coil;
        set(p, RA_X, 0.35F, coil);
        set(p, RA_Y, -0.15F, coil);
        set(p, RA_Z, 0.25F, coil);
        set(p, R_ELBOW, -1.6F, coil);
        p[CHEST_Y] += 0.3F * coil;
        p[CHEST_Z] -= 0.12F * coil;
        float up = outBack(clamp((t - hit + 2.5F) / 2.5F)) * (1F - smooth((t - hit - 7F) / 6F));
        set(p, RA_X, -2.7F, up);
        set(p, RA_Y, -0.3F, up);
        set(p, RA_Z, 0F, up);
        set(p, R_ELBOW, -0.75F, up);
        p[CHEST_Y] -= 0.45F * up;
        p[CHEST_X] -= 0.22F * up;
        p[LEAN] -= 0.18F * up;
        p[DROP] -= 1.6F * up;
        p[HEAD_X] -= 0.35F * up;
        set(p, R_KNEE, 0.05F, up);
        set(p, RL_X, 0.45F, up);
        p[LOOK] = 1F - 0.5F * up;
    }

    /** The superman punch: crouched like a spring, then flying flat out, fist first, and down into a lunge. */
    private static void haymaker(float[] p, float t, float age, float rage) {
        guard(p, age, rage);
        int leap = EntityChampion.HAY_LEAP, land = EntityChampion.HAY_LAND;
        float crouch = smooth(t / 9F) * (1F - smooth((t - leap) / 2F));
        set(p, DROP, 4.2F, crouch);
        set(p, LEAN, 0.3F, crouch);
        set(p, WAIST_X, 0.4F, crouch);
        set(p, RA_X, 0.75F, crouch);
        set(p, RA_Z, 0.35F, crouch);
        set(p, R_ELBOW, -1.1F, crouch);
        set(p, LA_X, -1.3F, crouch);
        set(p, L_ELBOW, -0.6F, crouch);
        set(p, LL_X, -0.9F, crouch);
        set(p, L_KNEE, 1.4F, crouch);
        set(p, RL_X, 0.25F, crouch);
        set(p, R_KNEE, 1.3F, crouch);
        p[CHEST_Y] += 0.35F * crouch;
        if (t >= leap && t < land + 0.5F) {
            // In the air his whole body is pitched along his flight; overhead, here, is forward.
            float fly = smooth((t - leap) / 3F);
            set(p, RA_X, -3.0F, fly);
            set(p, RA_Y, 0F, fly);
            set(p, RA_Z, 0.05F, fly);
            set(p, R_ELBOW, -0.02F, fly);
            set(p, LA_X, 0.35F, fly);
            set(p, LA_Z, -0.25F, fly);
            set(p, L_ELBOW, -0.4F, fly);
            set(p, LEAN, 0F, fly);
            set(p, WAIST_X, 0F, fly);
            set(p, DROP, 0F, fly);
            set(p, CHEST_Y, -0.3F, fly);
            set(p, HEAD_X, -0.9F, fly);
            set(p, RL_X, 0.25F, fly);
            set(p, R_KNEE, 0.2F, fly);
            set(p, LL_X, 0.05F, fly);
            set(p, L_KNEE, 1.1F, fly);
            p[LOOK] = 0F;
        }
        if (t >= land) {
            // Down into a lunge, his fist driven out ahead of him, then he straightens.
            float lunge = (1F - clamp((t - land) / 4F)) * 0.6F + 0.4F;
            float out = 1F - smooth((t - land - 8F) / 10F);
            float k = lunge * out;
            set(p, DROP, 4.5F, k);
            set(p, LEAN, 0.32F, k);
            set(p, WAIST_X, 0.3F, k);
            set(p, RA_X, -1.45F, k);
            set(p, RA_Y, 0F, k);
            set(p, R_ELBOW, -0.05F, k);
            set(p, LA_X, 0.5F, k);
            set(p, LA_Z, -0.4F, k);
            set(p, L_ELBOW, -0.3F, k);
            set(p, CHEST_Y, -0.6F, k);
            set(p, HEAD_X, 0.1F, k);
            set(p, LL_X, -1.1F, k);
            set(p, L_KNEE, 1.5F, k);
            set(p, RL_X, 0.85F, k);
            set(p, R_KNEE, 0.6F, k);
            p[LOOK] = 0.6F * (1F - k);
        }
    }

    /** Both arms wide, reaching; if he gets you, up over his head you go, and then you are thrown. */
    private static void grab(float[] p, float t, float age, float rage, boolean holding) {
        guard(p, age, rage);
        int reach = EntityChampion.GRAB_REACH, toss = EntityChampion.GRAB_THROW;
        float lunge = smooth(t / 6F) * (1F - smooth((t - reach - 2F) / 4F));
        set(p, RA_X, -1.35F, lunge);
        set(p, RA_Y, 0.45F, lunge);
        set(p, RA_Z, 0F, lunge);
        set(p, R_ELBOW, -0.35F, lunge);
        set(p, LA_X, -1.35F, lunge);
        set(p, LA_Y, -0.45F, lunge);
        set(p, LA_Z, 0F, lunge);
        set(p, L_ELBOW, -0.35F, lunge);
        set(p, LEAN, 0.32F, lunge);
        set(p, CHEST_Y, 0F, lunge);
        set(p, WAIST_Y, 0F, lunge);
        p[DROP] += 1.2F * lunge;
        if (!holding) {
            // Nothing there: the arms close on empty air and he stumbles on a step.
            float miss = smooth((t - reach) / 3F) * (1F - smooth((t - reach - 10F) / 8F));
            set(p, RA_Y, -0.55F, miss);
            set(p, LA_Y, 0.55F, miss);
            set(p, R_ELBOW, -0.9F, miss);
            set(p, L_ELBOW, -0.9F, miss);
            set(p, LEAN, 0.4F, miss);
            p[HEAD_X] += 0.2F * miss;
            p[LL_X] -= 0.5F * miss;
            p[L_KNEE] += 0.4F * miss;
            return;
        }
        float lift = smooth((t - reach) / 12F) * (1F - smooth((t - toss + 2F) / 2F));
        set(p, RA_X, -2.95F, lift);
        set(p, RA_Y, -0.2F, lift);
        set(p, R_ELBOW, -0.55F, lift);
        set(p, LA_X, -2.95F, lift);
        set(p, LA_Y, 0.2F, lift);
        set(p, L_ELBOW, -0.55F, lift);
        set(p, LEAN, -0.12F, lift);
        set(p, CHEST_X, -0.3F, lift);
        set(p, HEAD_X, -0.45F, lift);
        set(p, LL_Z, -0.2F, lift);
        set(p, RL_Z, 0.2F, lift);
        set(p, DROP, 1F, lift);
        p[LOOK] = 0.3F * (1F - lift);
        float throwing = strike(t, toss - 2F, toss, toss + 6F);
        set(p, RA_X, -0.95F, throwing);
        set(p, RA_Y, -0.2F, throwing);
        set(p, R_ELBOW, -0.05F, throwing);
        set(p, LA_X, -0.95F, throwing);
        set(p, LA_Y, 0.2F, throwing);
        set(p, L_ELBOW, -0.05F, throwing);
        set(p, LEAN, 0.4F, throwing);
        set(p, WAIST_X, 0.3F, throwing);
        set(p, CHEST_X, 0.15F, throwing);
        set(p, HEAD_X, 0.15F, throwing);
        p[DROP] += 1.5F * throwing;
    }

    /** Shoulder down, chin tucked, forearm across his chest; and skidding to a stop after. */
    private static void rush(float[] p, float t, float age, float rage) {
        guard(p, age, rage);
        int go = EntityChampion.RUSH_GO, stop = EntityChampion.RUSH_STOP;
        float set = smooth(t / 6F) * (1F - smooth((t - stop) / 3F));
        set(p, LEAN, 0.48F, set);
        set(p, WAIST_X, 0.25F, set);
        set(p, CHEST_Y, -0.5F, set);
        set(p, WAIST_Y, -0.15F, set);
        set(p, HEAD_X, 0.35F, set);
        set(p, HEAD_Y, 0.55F, set);
        set(p, RA_X, -0.65F, set);
        set(p, RA_Y, -0.9F, set);
        set(p, RA_Z, 0F, set);
        set(p, R_ELBOW, -1.7F, set);
        set(p, LA_X, 0.25F, set);
        set(p, LA_Y, 0F, set);
        set(p, LA_Z, -0.3F, set);
        set(p, L_ELBOW, -1.1F, set);
        // Pawing the ground before he goes.
        float paw = (t < go) ? MathHelper.sin(t * 0.9F) * smooth(t / 3F) : 0F;
        p[RL_X] += 0.35F * Math.max(0F, paw);
        p[R_KNEE] += 0.4F * Math.max(0F, paw);
        float skid = smooth((t - stop) / 2F) * (1F - smooth((t - stop - 6F) / 5F));
        set(p, LEAN, -0.18F, skid);
        set(p, LL_X, -0.7F, skid);
        set(p, L_KNEE, 0.4F, skid);
        set(p, RL_X, 0.5F, skid);
        set(p, R_KNEE, 0.9F, skid);
        p[DROP] += 1.6F * skid;
        set(p, RA_Z, 0.6F, skid);
        set(p, LA_Z, -0.6F, skid);
    }

    /** Both fists high over his head, a hop, and down on the ground; then up out of the crouch. */
    private static void pound(float[] p, float t, float age, float rage) {
        guard(p, age, rage);
        int hit = EntityChampion.POUND_HIT;
        float raise = outCubic(clamp(t / 7F)) * (1F - smooth((t - hit + 3F) / 2.5F));
        set(p, RA_X, -2.95F, raise);
        set(p, RA_Y, -0.45F, raise);
        set(p, RA_Z, 0F, raise);
        set(p, R_ELBOW, -0.45F, raise);
        set(p, LA_X, -2.95F, raise);
        set(p, LA_Y, 0.45F, raise);
        set(p, LA_Z, 0F, raise);
        set(p, L_ELBOW, -0.45F, raise);
        set(p, LEAN, -0.15F, raise);
        set(p, CHEST_X, -0.25F, raise);
        set(p, CHEST_Y, 0F, raise);
        set(p, HEAD_X, -0.2F, raise);
        set(p, RL_X, -0.55F, raise);
        set(p, R_KNEE, 1.1F, raise);
        set(p, LL_X, -0.3F, raise);
        set(p, L_KNEE, 0.9F, raise);
        float slam = outCubic(clamp((t - hit + 2.5F) / 2.5F)) * (1F - smooth((t - hit - 8F) / 9F));
        set(p, RA_X, -1.05F, slam);
        set(p, RA_Y, -0.35F, slam);
        set(p, R_ELBOW, -0.05F, slam);
        set(p, LA_X, -1.05F, slam);
        set(p, LA_Y, 0.35F, slam);
        set(p, L_ELBOW, -0.05F, slam);
        set(p, LEAN, 0.3F, slam);
        set(p, WAIST_X, 0.45F, slam);
        set(p, CHEST_X, 0.12F, slam);
        set(p, HEAD_X, -0.15F, slam);
        set(p, DROP, 3.4F, slam);
        set(p, RL_X, -1.05F, slam);
        set(p, R_KNEE, 1.5F, slam);
        set(p, LL_X, -0.2F, slam);
        set(p, L_KNEE, 1.3F, slam);
        set(p, RL_Z, 0.25F, slam);
        set(p, LL_Z, -0.25F, slam);
        p[LOOK] = 1F - slam;
    }

    /**
     * Guard dropped, chin out, beckoning you in with one hand. If you take the bait, he has
     * already turned it: the right comes over the top of your swing.
     */
    private static void counter(float[] p, float t, float age, float rage, float since) {
        guard(p, age, rage);
        float open = smooth(t / 6F) * (1F - smooth((t - EntityChampion.COUNTER_TICKS + 6F) / 6F));
        float beckon = Math.abs(MathHelper.sin(age * 0.35F));
        set(p, RA_X, 0.05F, open);
        set(p, RA_Y, 0F, open);
        set(p, RA_Z, 0.45F, open);
        set(p, R_ELBOW, -0.35F, open);
        set(p, LA_X, -1.25F, open);
        set(p, LA_Y, 0.15F, open);
        set(p, LA_Z, -0.1F, open);
        set(p, L_ELBOW, -1.0F - 0.65F * beckon, open);
        set(p, CHEST_X, -0.18F, open);
        set(p, CHEST_Y, 0F, open);
        set(p, HEAD_X, -0.25F, open);
        set(p, HEAD_Z, 0.12F, open);
        set(p, LEAN, -0.04F, open);
        p[DROP] -= 0.4F * open;
        if (since >= 0F) {
            // Caught it: a sharp turn of the shoulders and the right comes over the top.
            float turn = smooth(since / 2F);
            float hit = strike(since, EntityChampion.COUNTER_STRIKE - 2F, EntityChampion.COUNTER_STRIKE, EntityChampion.COUNTER_STRIKE + 6F);
            set(p, LA_X, -0.85F, turn);
            set(p, LA_Y, 0.55F, turn);
            set(p, L_ELBOW, -2.1F, turn);
            set(p, HEAD_X, 0.12F, turn);
            set(p, HEAD_Z, 0F, turn);
            set(p, CHEST_X, 0.05F, turn);
            set(p, RA_X, -1.85F, hit);
            set(p, RA_Y, -0.15F, hit);
            set(p, RA_Z, 0F, hit);
            set(p, R_ELBOW, -0.08F, hit);
            p[CHEST_Y] -= 0.5F * hit;
            p[WAIST_Y] -= 0.16F * hit;
            p[LEAN] += 0.22F * hit;
            p[DROP] += 1F * hit;
        }
    }

    /** He fills his chest, throws his arms back and his head up, and roars until the stone blazes. */
    private static void roar(float[] p, float t, float age, float rage) {
        guard(p, age, rage);
        int go = EntityChampion.ROAR_GO;
        float inhale = smooth(t / 10F) * (1F - smooth((t - go) / 2F));
        set(p, CHEST_X, -0.3F, inhale);
        set(p, HEAD_X, -0.45F, inhale);
        set(p, RA_X, 0.35F, inhale);
        set(p, RA_Y, 0F, inhale);
        set(p, RA_Z, 0.5F, inhale);
        set(p, R_ELBOW, -0.7F, inhale);
        set(p, LA_X, 0.35F, inhale);
        set(p, LA_Y, 0F, inhale);
        set(p, LA_Z, -0.5F, inhale);
        set(p, L_ELBOW, -0.7F, inhale);
        set(p, DROP, -0.5F, inhale);
        float bellow = outBack(clamp((t - go) / 3F)) * (1F - smooth((t - go - 30F) / 10F));
        float tremor = MathHelper.sin(age * 3.4F) * 0.04F * bellow;
        set(p, CHEST_X, 0.12F + tremor, bellow);
        set(p, CHEST_Y, 0F, bellow);
        set(p, WAIST_Y, 0F, bellow);
        set(p, HEAD_X, -0.55F + tremor, bellow);
        set(p, HEAD_Y, 0F, bellow);
        set(p, RA_X, -0.15F, bellow);
        set(p, RA_Y, 0.2F, bellow);
        set(p, RA_Z, 0.85F + tremor, bellow);
        set(p, R_ELBOW, -1.2F, bellow);
        set(p, LA_X, -0.15F, bellow);
        set(p, LA_Y, -0.2F, bellow);
        set(p, LA_Z, -0.85F - tremor, bellow);
        set(p, L_ELBOW, -1.2F, bellow);
        set(p, LEAN, 0.1F, bellow);
        set(p, DROP, 2.2F, bellow);
        set(p, LL_X, -0.45F, bellow);
        set(p, L_KNEE, 0.8F, bellow);
        set(p, RL_X, 0.4F, bellow);
        set(p, R_KNEE, 0.8F, bellow);
        set(p, LL_Z, -0.22F, bellow);
        set(p, RL_Z, 0.22F, bellow);
        p[LOOK] = 1F - bellow;
    }

    /** Berserk: up off the ground with both fists locked over his head, and down like a hammer on an anvil. */
    private static void hammer(float[] p, float t, float age, float rage) {
        guard(p, age, rage);
        int hit = EntityChampion.HAMMER_HIT;
        float crouch = smooth(t / 4F) * (1F - smooth((t - 5F) / 2F));
        p[DROP] += 2.5F * crouch;
        p[R_KNEE] += 0.7F * crouch;
        p[L_KNEE] += 0.7F * crouch;
        float up = smooth((t - 4F) / 4F) * (1F - smooth((t - hit + 2.5F) / 2.5F));
        set(p, RA_X, -3.05F, up);
        set(p, RA_Y, -0.55F, up);
        set(p, RA_Z, 0F, up);
        set(p, R_ELBOW, -0.25F, up);
        set(p, LA_X, -3.05F, up);
        set(p, LA_Y, 0.55F, up);
        set(p, LA_Z, 0F, up);
        set(p, L_ELBOW, -0.25F, up);
        set(p, LEAN, -0.3F, up);
        set(p, CHEST_X, -0.28F, up);
        set(p, CHEST_Y, 0F, up);
        set(p, HEAD_X, -0.3F, up);
        set(p, RL_X, -0.9F, up);
        set(p, R_KNEE, 1.4F, up);
        set(p, LL_X, -0.6F, up);
        set(p, L_KNEE, 1.5F, up);
        float down = outCubic(clamp((t - hit + 2.5F) / 2.5F)) * (1F - smooth((t - hit - 8F) / 10F));
        set(p, RA_X, -0.95F, down);
        set(p, RA_Y, -0.5F, down);
        set(p, R_ELBOW, -0.05F, down);
        set(p, LA_X, -0.95F, down);
        set(p, LA_Y, 0.5F, down);
        set(p, L_ELBOW, -0.05F, down);
        set(p, LEAN, 0.36F, down);
        set(p, WAIST_X, 0.5F, down);
        set(p, CHEST_X, 0.15F, down);
        set(p, HEAD_X, -0.1F, down);
        set(p, DROP, 3.8F, down);
        set(p, RL_X, -1.15F, down);
        set(p, R_KNEE, 1.6F, down);
        set(p, LL_X, -0.1F, down);
        set(p, L_KNEE, 1.4F, down);
        p[LOOK] = 0.5F * (1F - down);
    }

    /**
     * Planet Breaker. Gathering himself, crouched, fists at his sides and shaking; the leap,
     * arms thrust overhead; hanging at the top of the sky, fists raised together, back arched;
     * then the dive, fists first, his whole body a hammer.
     */
    private static void breaker(float[] p, float t, float age) {
        guard(p, age, 1F);
        int leap = EntityChampion.BREAKER_LEAP, apex = EntityChampion.BREAKER_APEX, dive = EntityChampion.BREAKER_DIVE;
        float gather = smooth(t / 14F) * (1F - smooth((t - leap) / 2F));
        float shake = MathHelper.sin(age * 3.6F) * 0.06F * clamp(t / leap);
        set(p, DROP, 4F, gather);
        set(p, LEAN, 0.18F, gather);
        set(p, WAIST_X, 0.45F, gather);
        set(p, CHEST_X, 0.2F + shake, gather);
        set(p, CHEST_Y, 0F, gather);
        set(p, WAIST_Y, 0F, gather);
        set(p, HEAD_X, 0.35F, gather);
        set(p, HEAD_Y, 0F, gather);
        set(p, RA_X, 0.4F, gather);
        set(p, RA_Y, 0F, gather);
        set(p, RA_Z, 0.4F + shake, gather);
        set(p, R_ELBOW, -1.3F, gather);
        set(p, LA_X, 0.4F, gather);
        set(p, LA_Y, 0F, gather);
        set(p, LA_Z, -0.4F - shake, gather);
        set(p, L_ELBOW, -1.3F, gather);
        set(p, RL_X, -0.55F, gather);
        set(p, R_KNEE, 1.2F, gather);
        set(p, LL_X, -0.4F, gather);
        set(p, L_KNEE, 1.2F, gather);
        set(p, RL_Z, 0.3F, gather);
        set(p, LL_Z, -0.3F, gather);
        // The last moment: the head comes up, the glare.
        float glare = smooth((t - leap + 10F) / 6F) * (1F - smooth((t - leap) / 2F));
        p[HEAD_X] -= 0.7F * glare;
        if (t >= leap) {
            float up = smooth((t - leap) / 3F);
            set(p, RA_X, -3.05F, up);
            set(p, RA_Y, -0.3F, up);
            set(p, RA_Z, 0F, up);
            set(p, R_ELBOW, -0.05F, up);
            set(p, LA_X, -3.05F, up);
            set(p, LA_Y, 0.3F, up);
            set(p, LA_Z, 0F, up);
            set(p, L_ELBOW, -0.05F, up);
            set(p, DROP, -0.8F, up);
            set(p, LEAN, 0F, up);
            set(p, WAIST_X, 0F, up);
            set(p, CHEST_X, -0.1F, up);
            set(p, HEAD_X, -0.3F, up);
            set(p, RL_X, 0.1F, up);
            set(p, R_KNEE, 0.15F, up);
            set(p, LL_X, 0.05F, up);
            set(p, L_KNEE, 0.35F, up);
            set(p, RL_Z, 0.02F, up);
            set(p, LL_Z, -0.02F, up);
            // At the top: back arched, fists raised together, legs drawn up; a second sun.
            float hang = smooth((t - apex + 4F) / 6F) * (1F - smooth((t - dive) / 3F));
            set(p, CHEST_X, -0.4F, hang);
            set(p, WAIST_X, -0.25F, hang);
            set(p, HEAD_X, -0.6F, hang);
            set(p, RA_Y, -0.5F, hang);
            set(p, LA_Y, 0.5F, hang);
            set(p, R_ELBOW, -0.45F, hang);
            set(p, L_ELBOW, -0.45F, hang);
            set(p, RL_X, -0.9F, hang);
            set(p, R_KNEE, 1.6F, hang);
            set(p, LL_X, -0.4F, hang);
            set(p, L_KNEE, 1.4F, hang);
            // The dive: straight as a spear, fists locked; the renderer turns him head down.
            float down = smooth((t - dive) / 3F);
            set(p, RA_Y, -0.4F, down);
            set(p, LA_Y, 0.4F, down);
            set(p, R_ELBOW, -0.02F, down);
            set(p, L_ELBOW, -0.02F, down);
            set(p, HEAD_X, -0.5F, down);
            set(p, CHEST_X, 0F, down);
            set(p, WAIST_X, 0F, down);
            set(p, RL_X, 0.05F, down);
            set(p, R_KNEE, 0.1F, down);
            set(p, LL_X, 0.1F, down);
            set(p, L_KNEE, 0.25F, down);
            p[LOOK] = 0F;
        }
    }

    /** Down in the crater he made, one knee in the rubble, both fists still buried in his planet; then his head comes up, slowly. */
    private static void impact(float[] p, float t, float age) {
        float squash = 1F - clamp(t / 5F);
        p[DROP] = 4.4F + 1.2F * squash * squash;
        p[LEAN] = 0.18F;
        p[WAIST_X] = 0.55F + 0.15F * squash;
        p[CHEST_X] = 0.15F;
        p[RA_X] = -0.85F;
        p[RA_Y] = -0.35F;
        p[R_ELBOW] = -0.05F;
        p[LA_X] = -0.85F;
        p[LA_Y] = 0.35F;
        p[L_ELBOW] = -0.05F;
        p[LL_X] = -1.45F;
        p[L_KNEE] = 1.55F;
        p[LL_Z] = -0.15F;
        p[RL_X] = 0.15F;
        p[R_KNEE] = 1.85F;
        p[RL_Z] = 0.12F;
        float look = smooth((t - 30F) / 30F);
        p[HEAD_X] = 0.5F - 1F * look + MathHelper.sin(age * 0.2F) * 0.02F;
        p[HEAD_Y] = MathHelper.sin(age * 0.05F) * 0.3F * look;
        p[LOOK] = 0F;
    }

    /** A blow on his guard: forearms tight in front of his face, rocked back half a step. */
    private static void block(float[] p, float t, float age, float rage) {
        guard(p, age, rage);
        float up = 1F - smooth((t - 6F) / 4F);
        float jolt = (1F - clamp(t / 4F)) * up;
        set(p, RA_X, -1.05F, up);
        set(p, RA_Y, -0.62F, up);
        set(p, R_ELBOW, -2.4F, up);
        set(p, LA_X, -1.1F, up);
        set(p, LA_Y, 0.62F, up);
        set(p, L_ELBOW, -2.4F, up);
        set(p, HEAD_X, 0.3F, up);
        set(p, CHEST_Y, 0F, up);
        p[LEAN] -= 0.14F * jolt;
        p[CHEST_X] -= 0.1F * jolt;
        p[DROP] += 0.8F * up;
    }

    /** Your blow whistles past: he dips and rolls under it, and is back up, already loading. */
    private static void slip(float[] p, float t, float age, float rage, float side) {
        guard(p, age, rage);
        float dip = outCubic(clamp(t / 3F)) * (1F - smooth((t - 5F) / 5F));
        p[DROP] += 3F * dip;
        p[WAIST_X] += 0.35F * dip;
        p[TILT] += 0.32F * side * dip;
        p[CHEST_Z] += 0.18F * side * dip;
        p[HEAD_Z] += 0.15F * side * dip;
        p[L_KNEE] += 0.6F * dip;
        p[R_KNEE] += 0.6F * dip;
        p[CHEST_Y] += 0.2F * side * dip;
    }

    /** Knocked flat on his back, the ground jumping under him; lying there, he laughs. */
    private static void knockdown(float[] p, float t, float age) {
        float fall = clamp(t / 11F);
        fall = fall * fall;
        float bounce = t > 11F && t < 17F ? MathHelper.sin((t - 11F) / 6F * PI) * 0.12F : 0F;
        p[LEAN] = -1.48F * fall + bounce;
        p[DROP] = 9.2F * fall;
        p[HEAD_X] = 0.35F * fall;
        p[RA_X] = -0.6F * (1F - fall) - 0.4F * fall;
        p[RA_Z] = 0.3F + 0.9F * fall;
        p[R_ELBOW] = -0.6F * fall;
        p[LA_X] = -0.5F * (1F - fall) - 0.3F * fall;
        p[LA_Z] = -0.3F - 0.95F * fall;
        p[L_ELBOW] = -0.5F * fall;
        p[RL_X] = -0.5F * fall;
        p[R_KNEE] = 0.9F * fall;
        p[LL_X] = -0.1F * fall;
        p[L_KNEE] = 0.3F * fall;
        p[RL_Z] = 0.25F * fall;
        p[LL_Z] = -0.2F * fall;
        // The laugh: his whole chest shaking with it.
        float laugh = smooth((t - 32F) / 3F) * (1F - smooth((t - 52F) / 6F));
        p[CHEST_X] += MathHelper.sin(age * 1.8F) * 0.08F * laugh - 0.15F * laugh;
        p[HEAD_X] -= 0.35F * laugh;
        p[LOOK] = 0F;
    }

    /** Up out of it, the stone mending him: onto a knee, then standing, then a roar at the sky. */
    private static void rise(float[] p, float t, float age) {
        float[] down = new float[N];
        knockdown(down, 60F, age);
        float[] kneel = new float[N];
        impact(kneel, 60F, age);
        kneel[HEAD_X] = -0.3F;
        float[] stand = new float[N];
        roar(stand, EntityChampion.ROAR_GO + 8F, age, 0.5F);
        float a = smooth(t / 14F), b = smooth((t - 16F) / 12F);
        for (int i = 0; i < N; i++) {
            float first = down[i] + (kneel[i] - down[i]) * a;
            p[i] = first + (stand[i] - first) * b;
        }
        p[LOOK] = b * 0.5F;
    }

    /** Adrift, going nowhere: limbs spread, slowly turning, reaching for something to hold that is not there. */
    private static void drift(float[] p, float age) {
        float a = MathHelper.sin(age * 0.045F), b = MathHelper.sin(age * 0.037F + 1.3F);
        p[RA_X] = -0.6F + a * 0.5F;
        p[RA_Z] = 0.9F + b * 0.25F;
        p[R_ELBOW] = -0.6F - Math.max(0F, a) * 0.6F;
        p[LA_X] = -0.4F - b * 0.5F;
        p[LA_Z] = -0.9F - a * 0.25F;
        p[L_ELBOW] = -0.6F - Math.max(0F, b) * 0.6F;
        p[RL_X] = -0.3F + b * 0.25F;
        p[R_KNEE] = 0.5F + a * 0.2F;
        p[RL_Z] = 0.25F;
        p[LL_X] = 0.1F - a * 0.25F;
        p[L_KNEE] = 0.6F - b * 0.2F;
        p[LL_Z] = -0.25F;
        p[CHEST_X] = -0.1F + MathHelper.sin(age * 0.06F) * 0.05F;
        p[HEAD_X] = -0.1F;
        // Now and then, an arm reaches out toward you.
        float reach = smooth((MathHelper.sin(age * 0.021F) - 0.55F) * 3F);
        set(p, RA_X, -1.55F, reach);
        set(p, RA_Z, 0.15F, reach);
        set(p, R_ELBOW, -0.15F, reach);
        p[LOOK] = 0.7F;
    }

    /** Hauling himself hand over hand to the rocket through nothing at all. */
    private static void board(float[] p, float t, float age) {
        drift(p, age);
        float k = smooth(t / 8F);
        float r = MathHelper.sin(t * 0.3F);
        set(p, RA_X, -2.5F + r * 0.5F, k);
        set(p, RA_Z, 0.2F, k);
        set(p, R_ELBOW, -0.5F - Math.max(0F, r) * 0.6F, k);
        set(p, LA_X, -2.5F - r * 0.5F, k);
        set(p, LA_Z, -0.2F, k);
        set(p, L_ELBOW, -0.5F - Math.max(0F, -r) * 0.6F, k);
        set(p, HEAD_X, -0.4F, k);
        set(p, RL_X, 0.3F, k);
        set(p, LL_X, 0.4F, k);
        p[LOOK] = 0F;
    }

    // ------------------------------------------------------------------ easing

    /** Eases one value of the pose toward a target, by f. */
    private static void set(float[] p, int i, float value, float f) {
        p[i] += (value - p[i]) * f;
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

    /** A quick in-and-out around a moment, for fists drumming on a chest. */
    private static float pulse(float t, float at) {
        float d = Math.abs(t - at);
        return d > 3F ? 0F : 1F - smooth(d / 3F);
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
