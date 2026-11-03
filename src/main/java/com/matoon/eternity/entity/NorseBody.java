package com.matoon.eternity.entity;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.MathHelper;

/**
 * How a Norse figure's body moves on the client, apart from its pose: a cloak in three
 * segments; the long skirt's lower panels, which lag behind every stride and settle after it;
 * a braid or the hair down a back; a beard; a robe's open sleeves; and the pose it was last drawn
 * in, which the next one blends out of. Shared by the living and the dead.
 */
public final class NorseBody {

    /** Something with a Norse body. */
    public interface Figure {
        /** Which look (NorseBoxes.STRANGER and the rest). */
        int look();

        NorsePose pose();

        NorseBody body();
    }

    public static final int CLOAK = 3, BRAID = 3, BEARD = 2;

    public int stateTicks;
    public NorsePose seen = NorsePose.STAND, previous = NorsePose.STAND;
    public final float[] poseFrom = new float[64], poseLast = new float[64];
    public final float[] cloak = new float[CLOAK], prevCloak = new float[CLOAK], cloakSpin = new float[CLOAK];
    public final float[] braid = new float[BRAID], prevBraid = new float[BRAID], braidSpin = new float[BRAID];
    public final float[] beard = new float[BEARD], prevBeard = new float[BEARD], beardSpin = new float[BEARD];
    /** Sideways sway of everything that hangs; the skirt's swing behind a stride; the hem's lag behind that; the sleeves'. */
    public float sway, prevSway, skirt, prevSkirt, skirtSpin, hem, prevHem, hemSpin, sleeve, prevSleeve, sleeveSpin;
    /** How fast and which way the body is turning, which throws hair and hems outward. */
    public float turn, prevTurn;
    private double velX, velZ;
    private float lastYaw = Float.NaN;

    /** Its pose changed: blend out of whatever was last drawn. */
    public void changed(NorsePose to) {
        stateTicks = 0;
        System.arraycopy(poseLast, 0, poseFrom, 0, poseLast.length);
        previous = seen;
        seen = to;
    }

    public void tick(EntityLivingBase e) {
        stateTicks++;
        velX += (e.posX - e.prevPosX - velX) * 0.5D;
        velZ += (e.posZ - e.prevPosZ - velZ) * 0.5D;
        float yaw = (float) Math.toRadians(e.renderYawOffset);
        double fwd = -velX * MathHelper.sin(yaw) + velZ * MathHelper.cos(yaw);
        double side = velX * MathHelper.cos(yaw) + velZ * MathHelper.sin(yaw);
        double speed = Math.sqrt(velX * velX + velZ * velZ);
        float strength = (float) Math.min(1D, speed * 3D);
        float t = e.ticksExisted;
        boolean lying = seen.lying();
        prevTurn = turn;
        float dYaw = Float.isNaN(lastYaw) ? 0F : MathHelper.wrapDegrees(e.renderYawOffset - lastYaw);
        lastYaw = e.renderYawOffset;
        turn += (MathHelper.clamp(dYaw * 0.05F, -0.6F, 0.6F) - turn) * 0.25F;

        // Wool hangs; it only lifts behind someone who moves, and swings out when they turn.
        float theta = lying ? 0F : (float) MathHelper.clamp(0.04D + Math.max(0D, fwd) * 2.4D, 0.02D, 0.85D);
        prevSway = sway;
        sway += ((float) MathHelper.clamp(-side * 2D, -0.3D, 0.3D) - turn * 0.6F + MathHelper.sin(t * 0.031F) * 0.018F - sway) * 0.15F;
        System.arraycopy(cloak, 0, prevCloak, 0, CLOAK);
        for (int i = 0; i < CLOAK; i++) {
            float flutter = MathHelper.sin(t * (0.21F + 0.3F * strength) - i * 1.1F) * (0.01F + 0.07F * strength) * (0.5F + i * 0.4F);
            float goal = (i == 0 ? theta : prevCloak[i - 1] * 0.75F + theta * 0.25F) + flutter;
            cloakSpin[i] += (goal - cloak[i]) * 0.25F;
            cloakSpin[i] *= 0.6F;
            cloak[i] += cloakSpin[i];
            cloak[i] = i == 0 ? Math.max(0.0F, cloak[i]) : cloak[i - 1] + MathHelper.clamp(cloak[i] - cloak[i - 1], -0.3F, 0.6F);
        }
        // A long skirt swings back behind each stride, a little late; its hem later still.
        prevSkirt = skirt;
        float skirtGoal = lying ? 0F : (float) MathHelper.clamp(Math.max(0D, fwd) * 1.6D, 0D, 0.45D);
        skirtSpin += (skirtGoal - skirt) * 0.2F;
        skirtSpin *= 0.65F;
        skirt += skirtSpin;
        prevHem = hem;
        hemSpin += (skirt * 0.8F + MathHelper.sin(t * (0.2F + strength * 0.3F)) * 0.02F * (0.3F + strength) - hem) * 0.16F;
        hemSpin *= 0.7F;
        hem += hemSpin;
        // A robe's sleeves hang from the elbow and swing after the arm.
        prevSleeve = sleeve;
        sleeveSpin += ((float) Math.max(0D, fwd) * 1.2F + MathHelper.sin(t * 0.04F) * 0.02F - sleeve) * 0.2F;
        sleeveSpin *= 0.65F;
        sleeve += sleeveSpin;
        // A braid or long hair swings behind the head; a beard, a little.
        System.arraycopy(braid, 0, prevBraid, 0, BRAID);
        for (int i = 0; i < BRAID; i++) {
            float goal = (float) Math.max(0D, fwd) * 1.3F * (i + 1) - turn * 0.4F * (i + 1) + MathHelper.sin(t * 0.05F + i) * 0.025F;
            braidSpin[i] += (goal - braid[i]) * 0.28F;
            braidSpin[i] *= 0.62F;
            braid[i] += braidSpin[i];
        }
        System.arraycopy(beard, 0, prevBeard, 0, BEARD);
        for (int i = 0; i < BEARD; i++) {
            float goal = (float) Math.max(0D, fwd) * 0.5F * (i + 1);
            beardSpin[i] += (goal - beard[i]) * 0.3F;
            beardSpin[i] *= 0.6F;
            beard[i] += beardSpin[i];
        }
    }
}
