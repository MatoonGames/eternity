package com.matoon.eternity.client.render;

import com.matoon.eternity.client.gui.SpeakerPanel;
import com.matoon.eternity.entity.EntityDraugr;
import com.matoon.eternity.entity.EntityNorse;
import com.matoon.eternity.entity.NorseBody;
import com.matoon.eternity.entity.NorsePose;
import com.matoon.eternity.entity.Morph;
import com.matoon.eternity.entity.Rot;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Arrays;

import static com.matoon.eternity.client.render.VaultParts.clamp;
import static com.matoon.eternity.client.render.VaultParts.set;
import static com.matoon.eternity.client.render.VaultParts.smooth;
import static com.matoon.eternity.client.render.VaultParts.strike;

/**
 * A Norse body, living or dead, jointed down to the fingers (see {@link NorseBoxes} for what
 * each look hangs off it).
 * <p>
 * Every frame the pose is built from scratch out of the figure's synced pose, how long it has
 * been held, and who it is: the stranger is composed and almost still, and holds a look a beat
 * longer than she needs to; Hrafn stands like a man who has not slept, thumbs in his belt, and
 * sighs; Ragna, dying, breathes shallow and fast and her hand keeps closing on nothing; Death
 * does not breathe at all, and only her robe moves; the dead twitch and lurch and their jaws
 * hang. Whenever the pose changes the body blends out of the one it was last drawn in, so
 * kneeling, lying down and getting up out of a grave all happen as movements.
 * <p>
 * The living blink, and their mouths move while their words are being spoken; they nod and
 * move their hands as they talk. Skirts, cloaks, hair, braids, beards and sleeves hang plumb
 * whatever the body under them does, are pushed out by the legs and swing on their springs.
 */
@SideOnly(Side.CLIENT)
public class ModelNorse extends ModelBase {

    static final int LEAN = 0, TWIST = 1, TILT = 2, DROP = 3, WAIST_X = 4, WAIST_Y = 5, CHEST_X = 6, CHEST_Y = 7, CHEST_Z = 8,
            HEAD_X = 9, HEAD_Y = 10, HEAD_Z = 11, RA_X = 12, RA_Y = 13, RA_Z = 14, R_ELBOW = 15, LA_X = 16, LA_Y = 17, LA_Z = 18,
            L_ELBOW = 19, RL_X = 20, RL_Y = 21, RL_Z = 22, R_KNEE = 23, LL_X = 24, LL_Y = 25, LL_Z = 26, L_KNEE = 27, LOOK = 28,
            WALK = 29, AXE = 30, LIFT = 31, R_WRIST = 32, L_WRIST = 33, R_CURL = 34, L_CURL = 35, R_TWIST = 36, L_TWIST = 37,
            R_FOOT = 38, L_FOOT = 39, JAW = 40, LIDS = 41, R_THUMB = 42, L_THUMB = 43, TALK = 44, HEAD_LIMIT = 45, N = 46;

    private static final float PI = (float) Math.PI;

    /** Set by the renderer while it draws a portrait for the speaker panel: upright, whatever they are doing. */
    static boolean portraitPose;

    private final VaultBoxes.Sheet sheet;
    private final int look;
    private final boolean living, skirted;
    public final ModelRenderer root, abdomen, chest, head, rArm, rFore, rHand, lArm, lFore, lHand, rLeg, rShin, lLeg, lShin;
    private final ModelRenderer rFingers, lFingers, rThumb, lThumb, rFoot, lFoot, lid, mouth, jaw, lockR, lockL, apron, apron2,
            sleeveR, sleeveL, axeHand, axeBelt, helm, brim, trowelHand, trowelBelt, gem, collar, eye, flapR, flapL, gut;
    private final ModelRenderer[] upper = new ModelRenderer[6], lower = new ModelRenderer[6];
    private final ModelRenderer[] cloak = new ModelRenderer[NorseBoxes.CLOAK_SEGMENTS];
    /** Her cape: a row for each length; in each, its pleats, right then left, from the middle out; and the falls of it from her wrists, right then left. */
    private final ModelRenderer[] rows = new ModelRenderer[NorseBoxes.CAPE_SEGMENTS];
    private final ModelRenderer[][][] pleats = new ModelRenderer[2][NorseBoxes.CAPE_PLEATS][NorseBoxes.CAPE_SEGMENTS];
    private final ModelRenderer[][] drapes = new ModelRenderer[2][NorseBoxes.DRAPES];
    private final ModelRenderer[] braid = new ModelRenderer[NorseBoxes.BRAID];
    private final ModelRenderer[] beard = new ModelRenderer[NorseBoxes.BEARD_SEGMENTS];
    private final float[] pose = new float[N];
    private float partial;
    /** Where the hips sit: higher on her, whose legs are longer than anyone's. */
    private final float hips;
    /** Every part by name, so pieces of the skin can be taken off (see {@link #peel}). */
    private final java.util.Map<String, Peel> named = new java.util.HashMap<>();
    /** Every part, however many share a name, so all of her can be changed at once (see {@link #each}). */
    private final java.util.List<Peel> all = new java.util.ArrayList<>();

    public ModelNorse(int look) {
        this.look = look;
        sheet = NorseBoxes.SHEETS[look];
        textureWidth = sheet.width;
        textureHeight = sheet.height;
        living = look == NorseBoxes.STRANGER || look == NorseBoxes.RAGNA || look == NorseBoxes.HRAFN || look == NorseBoxes.GARDENER
                || look == NorseBoxes.AYESHA || look == NorseBoxes.MAGUS || look == NorseBoxes.TEMPTRESS || look == NorseBoxes.UNVEILED;
        skirted = !sheet.of("skirtFR2").isEmpty();
        boolean broad = look == NorseBoxes.HRAFN || look == NorseBoxes.MAGUS;
        float shoulder = broad ? 6F : 5.5F;
        float half = broad ? 2F : 1.5F;

        // Her legs are longer than anyone's: the hips sit higher, and the renderer scales her down to match.
        boolean leggy = look == NorseBoxes.TEMPTRESS || look == NorseBoxes.UNVEILED;
        hips = leggy ? 10F : 12F;
        root = part("pelvis", null, 0F, hips, 0F);
        float[][] at = {{-2.4F, 0.6F, -2.45F}, {2.4F, 0.6F, -2.45F}, {-2.4F, 0.6F, 2.45F}, {2.4F, 0.6F, 2.45F}, {-4.15F, 0.6F, 0F}, {4.15F, 0.6F, 0F}};
        for (int i = 0; i < 6; i++) {
            String name = "skirt" + NorseBoxes.PANELS[i];
            upper[i] = part(name, root, at[i][0], at[i][1], at[i][2]);
            int length = sheet.of(name).isEmpty() ? 6 : sheet.of(name).get(0).h;
            lower[i] = part(name + "2", upper[i], 0F, length - 0.2F, 0F);
        }
        apron = part("apron", root, 0F, 0.3F, -3.05F);
        apron2 = part("apron2", apron, 0F, 5.8F, 0F);
        abdomen = part("abdomen", root, 0F, -2F, 0F);
        chest = part("chest", abdomen, 0F, -5F, 0F);
        part("broochR", chest, -2.2F, -5.3F, -2.55F);
        part("broochL", chest, 2.2F, -5.3F, -2.55F);
        part("beads", chest, 0F, -4.1F, -2.75F);
        head = part("head", chest, 0F, -7F, 0F);
        part("nose", head, 0F, -4.3F, -4F);
        lid = part("lid", head, 0F, -4F, -4.04F);
        // The Sovereign's gem at the brow, under the circlet.
        part("brow", head, 0F, -6.6F, -4.3F);
        mouth = part("mouth", head, 0F, -2F, -4.04F);
        jaw = part("jaw", head, 0F, -2F, 0.2F);
        helm = part("helm", head, 0F, 0F, 0F);
        part("hood", head, 0F, 0F, 0F);
        part("hoodR", head, 0F, 0F, 0F);
        part("hoodL", head, 0F, 0F, 0F);
        brim = part("brim", head, 0F, -8.7F, -5.1F);
        brim.rotateAngleX = 0.35F;
        part("bun", head, 0F, -4.6F, 4.3F);
        part("knot", head, 0F, -3.8F, 4.6F);
        lockR = part("lockR", head, -3.7F, -6.6F, -3.3F);
        lockL = part("lockL", head, 3.7F, -6.6F, -3.3F);
        ModelRenderer parent = head;
        boolean dead = look == NorseBoxes.DRAUGR;
        // The dead's hair and the Gardener's hang from high on the back of the head, the whole width of it.
        boolean mane = dead || look == NorseBoxes.GARDENER || look == NorseBoxes.MAGUS;
        for (int i = 0; i < braid.length; i++) {
            braid[i] = part("braid" + i, parent, 0F, i == 0 ? (mane ? -5.5F : -3.6F) : (sheet.of("braid" + (i - 1)).isEmpty() ? 4F
                    : sheet.of("braid" + (i - 1)).get(0).h - 0.3F), i == 0 ? (mane ? 4.4F : 4.5F) : 0F);
            parent = braid[i];
        }
        parent = dead ? jaw : head;
        for (int i = 0; i < beard.length; i++) {
            beard[i] = part("beard" + i, parent, 0F, i == 0 ? (dead ? 1.6F : -0.6F) : (sheet.of("beard0").isEmpty() ? 2.8F
                    : sheet.of("beard0").get(0).h - 0.2F), i == 0 ? (dead ? -2.9F : -3.4F) : 0F);
            parent = beard[i];
        }
        parent = chest;
        for (int i = 0; i < cloak.length; i++) {
            int len = sheet.of("cloak0").isEmpty() ? 6 : sheet.of("cloak0").get(0).h;
            cloak[i] = part("cloak" + i, parent, 0F, i == 0 ? -7.2F : len - 0.2F, i == 0 ? 2.65F : 0F);
            parent = cloak[i];
        }

        rArm = part("upperR", chest, -shoulder, -6F, 0F);
        rFore = part("foreR", rArm, 0F, 4.5F, 0F);
        rHand = part("handR", rFore, 0F, 3.8F, 0F);
        rFingers = part("fingersR", rHand, 0F, 0.9F, -0.3F);
        rThumb = part("thumbR", rHand, half - 0.2F, 0.3F, -0.6F);
        sleeveR = part("sleeveR", rFore, 0F, 0.4F, 0F);
        lArm = part("upperL", chest, shoulder, -6F, 0F);
        lFore = part("foreL", lArm, 0F, 4.5F, 0F);
        lHand = part("handL", lFore, 0F, 3.8F, 0F);
        lFingers = part("fingersL", lHand, 0F, 0.9F, -0.3F);
        lThumb = part("thumbL", lHand, -(half - 0.2F), 0.3F, -0.6F);
        sleeveL = part("sleeveL", lFore, 0F, 0.4F, 0F);
        // Her cape: a row for each length, hanging from the end of the row above, the first from across her shoulders;
        // in each row, the pleats hinged edge to edge out from the middle of her back, her right then her left. A fall
        // of it from each wrist, in lengths the same way. All of it sheets, drawn from the side that faces you only.
        // Her sash.
        ModelRenderer row = chest;
        for (int i = 0; i < NorseBoxes.CAPE_SEGMENTS; i++) {
            rows[i] = row = part("caperow" + i, row, 0F, i == 0 ? -7.1F : NorseBoxes.CAPE_LENGTH - 0.25F, i == 0 ? 2.5F : 0F);
            for (int side = 0; side < 2; side++) {
                ModelRenderer hinge = row;
                for (int j = 0; j < NorseBoxes.CAPE_PLEATS; j++) {
                    float edge = j == 0 ? 0F : NorseBoxes.PLEAT_WIDTHS[j - 1] * (side == 0 ? -1F : 1F);
                    pleats[side][j][i] = hinge = sheet(part("cape" + (side == 0 ? "R" : "L") + j + "_" + i, hinge, edge, 0F, 0F));
                }
            }
        }
        for (int side = 0; side < 2; side++) {
            ModelRenderer link = side == 0 ? rFore : lFore;
            for (int i = 0; i < NorseBoxes.DRAPES; i++) {
                drapes[side][i] = link = sheet(part("drape" + i + (side == 0 ? "R" : "L"), link, 0F, i == 0 ? 3.4F : NorseBoxes.DRAPE_LENGTH, 0F));
            }
        }
        part("sash", root, 0F, 0F, 0F);
        // What comes loose on her as the pretty one rots: a flap of skin off each forearm, her left eye out of its
        // socket on its stalk, and a loop of something out of the split in her belly. Each hangs plumb, and swings.
        flapR = part("flapR", rFore, -1.9F, 0.6F, 0F);
        flapL = part("flapL", lFore, 1.9F, 0.6F, 0F);
        eye = part("eye", head, 2F, -4F, -4.2F);
        gut = part("gut", abdomen, 0.6F, -1.2F, -2.3F);
        // The axe: gripped in his right hand once drawn, at his right hip until then.
        axeHand = part("axe", rHand, 0F, 1.1F, -0.2F);
        axeBelt = part("axe", root, -4.7F, 0.6F, 0.4F);
        // The Gardener's trowel: in his hand while he works, through his belt at the left hip when he doesn't.
        trowelHand = part("trowel", rHand, 0F, 1.1F, -0.2F);
        trowelBelt = part("trowel", root, 4.7F, 0.2F, -0.8F);
        gem = part("gem", chest, 0F, -4.4F, -2.25F);
        // The Sovereign's standing collars, fanned behind the head, and their wings round the neck.
        collar = part("collar", chest, 0F, -6.4F, 4.7F);
        collar.rotateAngleX = -0.18F;
        part("wingR", chest, -3.4F, -6.6F, -0.6F).rotateAngleZ = 0.12F;
        part("wingL", chest, 3.4F, -6.6F, -0.6F).rotateAngleZ = -0.12F;

        rLeg = part("thighR", root, -2F, 0F, 0F);
        rShin = part("shinR", rLeg, 0F, leggy ? 7F : 6F, 0F);
        rFoot = part("footR", rShin, 0F, leggy ? 6F : 5F, 0F);
        lLeg = part("thighL", root, 2F, 0F, 0F);
        lShin = part("shinL", lLeg, 0F, leggy ? 7F : 6F, 0F);
        lFoot = part("footL", lShin, 0F, leggy ? 6F : 5F, 0F);
    }

    private ModelRenderer part(String name, ModelRenderer parent, float x, float y, float z) {
        ModelRenderer part = VaultParts.part(this, sheet, name, parent, x, y, z);
        if (part instanceof Peel) {
            all.add((Peel) part);
            named.putIfAbsent(name, (Peel) part);
        }
        return part;
    }

    /** A part that is a sheet of cloth: drawn only from whichever side faces you, so its two faces never fight. */
    private static ModelRenderer sheet(ModelRenderer part) {
        if (part instanceof Peel) {
            ((Peel) part).sheet = true;
        }
        return part;
    }

    /** Takes away the boxes of every part that passes, leaving what hangs from them; until {@link #unpeel}. */
    void peel(java.util.function.Predicate<String> gone) {
        for (java.util.Map.Entry<String, Peel> e : named.entrySet()) {
            e.getValue().peeled = gone.test(e.getKey());
        }
    }

    void unpeel() {
        for (Peel p : named.values()) {
            p.peeled = false;
        }
    }

    /** Does something to every part; each knows its name by its boxName. */
    void each(java.util.function.Consumer<Peel> f) {
        all.forEach(f);
    }

    /** Every part back to as it was made. */
    void plain() {
        all.forEach(Peel::plain);
    }

    /** One part by name, to draw on its own as a piece torn off; null if there is none. */
    Peel named(String name) {
        return named.get(name);
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
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch,
                                  float scale, Entity entity) {
        if (!(entity instanceof NorseBody.Figure) || !(entity instanceof EntityLivingBase)) {
            return;
        }
        NorseBody.Figure f = (NorseBody.Figure) entity;
        NorseBody b = f.body();
        EntityLivingBase body = (EntityLivingBase) entity;
        NorsePose state = f.pose();
        int seed = entity.getEntityId();
        float t = b.stateTicks + partial;
        Arrays.fill(pose, 0F);
        animate(look, state, t, age, pose, seed);
        float blend = smooth(t / blendTicks(state, b.previous));
        if (blend < 1F) {
            for (int i = 0; i < N; i++) {
                pose[i] = b.poseFrom[i] + (pose[i] - b.poseFrom[i]) * blend;
            }
        }
        System.arraycopy(pose, 0, b.poseLast, 0, N);

        float[] p = pose.clone();
        if (portraitPose) {
            // In the speaker's window they are shown from the chest up, upright, whatever they are doing.
            p[DROP] = p[LEAN] = p[TILT] = p[TWIST] = p[LIFT] = 0F;
            p[RL_X] = p[LL_X] = p[R_KNEE] = p[L_KNEE] = 0F;
            p[HEAD_X] = MathHelper.clamp(p[HEAD_X], -0.25F, 0.25F);
            p[HEAD_Y] = MathHelper.clamp(p[HEAD_Y], -0.35F, 0.35F) * 0.5F + 0.2F;
            p[WAIST_X] = p[CHEST_X] = 0F;
        }
        float gait = limbSwingAmount * p[WALK];
        if (gait > 0.01F) {
            walk(p, limbSwing, Math.min(1F, gait * 1.4F), look);
        }
        // Talking: the mouth moves with the words, the head with the sense of them, a hand now and then.
        float talk = living && !state.lying() ? SpeakerPanel.INSTANCE.speaking(seed) : 0F;
        if (talk > 0F) {
            talking(p, age, talk, look, state);
        }
        int rot = look == NorseBoxes.TEMPTRESS && entity instanceof EntityNorse ? ((EntityNorse) entity).rot() : 0;
        float twitch = 0F;
        int joint = 0;
        if (rot >= Rot.TWITCHES && state != NorsePose.TEAR && !portraitPose) {
            twitch = Rot.twitch(age, seed, rot);
            joint = Rot.which(age, seed, rot);
            if (joint == 0) {
                p[HEAD_Z] += 1.05F * twitch * (seed % 2 == 0 ? 1F : -1F);
                p[HEAD_X] += 0.2F * twitch;
            } else if (joint == 2) {
                p[HEAD_Y] += 1.6F * twitch;
                p[HEAD_LIMIT] = 1F;
            }
        }
        if (entity instanceof EntityNorse && !portraitPose) {
            float shudder = (((EntityNorse) entity).thrilledAt - body.ticksExisted) + 34F - partial;
            if (shudder > 0F) {
                // Something died for her: it goes through her all the way down, the eyes roll shut, the head goes back.
                float k = shudder / 34F;
                float bump = (float) Math.sin(Math.min(1F, (1F - k) * 3F) * Math.PI / 2F) * k;
                p[CHEST_X] += MathHelper.sin(age * 3.1F) * 0.06F * k - 0.12F * bump;
                p[HEAD_X] -= 0.4F * bump;
                p[HEAD_Z] += MathHelper.sin(age * 2.3F) * 0.05F * k;
                p[RA_Z] += 0.1F * bump;
                p[LA_Z] -= 0.1F * bump;
                p[R_CURL] = Math.max(p[R_CURL], 0.8F * bump);
                p[L_CURL] = Math.max(p[L_CURL], 0.8F * bump);
                p[JAW] = Math.max(p[JAW], 0.35F * bump);
                if (bump > 0.3F) {
                    p[LIDS] = 1F;
                }
            }
        }
        float morph = entity instanceof EntityNorse && !portraitPose ? ((EntityNorse) entity).morphTicks(partial) : -1F;
        if (morph >= 0F) {
            morph(p, morph, age, seed);
        }
        float hurt = body.hurtTime > 0 ? (body.hurtTime - partial) / 10F : 0F;
        if (hurt > 0F && !state.lying()) {
            hurt *= hurt;
            p[CHEST_X] -= 0.22F * hurt;
            p[HEAD_X] -= 0.35F * hurt;
            p[DROP] += 0.5F * hurt;
        }
        if (!state.lying() && !portraitPose) {
            p[HEAD_Y] += netHeadYaw * 0.017453292F * p[LOOK];
            p[HEAD_X] += headPitch * 0.017453292F * p[LOOK];
        }
        // The living blink: every few seconds, a little irregularly; Death and the dead never do.
        if (living && p[LIDS] < 0.5F) {
            float cycle = (age + seed * 37) % 83F;
            float second = (age + seed * 17) % 211F;
            if (cycle < 2.4F || second < 2.2F || second > 6F && second < 8.2F) {
                p[LIDS] = 1F;
            }
        }
        apply(p, b, entity, age, talk);
        if (twitch > 0F && joint == 1) {
            // The elbow, backwards.
            rFore.rotateAngleX = 1.1F * twitch;
        }
        if (morph >= 0F) {
            // As each piece of her goes, its joint goes the wrong way: the elbows back, the knees forward.
            rFore.rotateAngleX += 1.5F * Morph.snap(Morph.ARM_R, morph);
            lFore.rotateAngleX += 1.5F * Morph.snap(Morph.ARM_L, morph);
            float knees = Morph.snap(Morph.LEGS, morph);
            rShin.rotateAngleX -= 1.1F * knees;
            lShin.rotateAngleX -= 0.8F * knees;
        }
        hanging(rot, age);
    }

    /** What has come loose on her, shown once it has, hanging plumb from wherever it hangs, swinging a little. */
    private void hanging(int rot, float age) {
        boolean her = look == NorseBoxes.TEMPTRESS;
        flapR.showModel = flapL.showModel = her && rot >= 5;
        eye.showModel = her && rot >= 6;
        gut.showModel = her && rot >= 7;
        if (her) {
            jaw.showModel = rot >= 7;
            if (rot >= 7) {
                // Her jaw has come away at one hinge: it hangs, open, crooked, and swings.
                jaw.rotateAngleX = Math.max(jaw.rotateAngleX, 0.7F + MathHelper.sin(age * 0.09F) * 0.08F);
                jaw.rotateAngleZ = 0.28F;
            }
        }
        float body = root.rotateAngleX + abdomen.rotateAngleX;
        eye.rotateAngleX = -(body + chest.rotateAngleX + head.rotateAngleX) + MathHelper.sin(age * 0.15F) * 0.25F;
        eye.rotateAngleZ = -(root.rotateAngleZ + chest.rotateAngleZ + head.rotateAngleZ) + MathHelper.sin(age * 0.11F + 1F) * 0.2F;
        gut.rotateAngleX = -body + MathHelper.sin(age * 0.07F) * 0.12F;
        gut.rotateAngleZ = -root.rotateAngleZ + MathHelper.sin(age * 0.05F + 2F) * 0.1F;
        flapR.rotateAngleX = -(body + chest.rotateAngleX + rArm.rotateAngleX + rFore.rotateAngleX) + MathHelper.sin(age * 0.13F) * 0.15F;
        flapR.rotateAngleZ = -rArm.rotateAngleZ;
        flapL.rotateAngleX = -(body + chest.rotateAngleX + lArm.rotateAngleX + lFore.rotateAngleX) + MathHelper.sin(age * 0.13F + 2F) * 0.15F;
        flapL.rotateAngleZ = -lArm.rotateAngleZ;
    }

    private void apply(float[] p, NorseBody b, Entity entity, float age, float talk) {
        root.rotationPointY = hips + p[DROP];
        root.rotationPointZ = p[LIFT];
        root.rotateAngleX = p[LEAN];
        root.rotateAngleY = p[TWIST];
        root.rotateAngleZ = p[TILT];
        abdomen.rotateAngleX = p[WAIST_X];
        abdomen.rotateAngleY = p[WAIST_Y];
        chest.rotateAngleX = p[CHEST_X];
        chest.rotateAngleY = p[CHEST_Y];
        chest.rotateAngleZ = p[CHEST_Z];
        head.rotateAngleX = MathHelper.clamp(p[HEAD_X], -1.4F, 1F);
        // Her neck, mid-twitch, goes further round than any neck should.
        head.rotateAngleY = p[HEAD_LIMIT] > 0.5F ? p[HEAD_Y] : MathHelper.clamp(p[HEAD_Y], -1.25F, 1.25F);
        head.rotateAngleZ = p[HEAD_Z];
        rArm.rotateAngleX = p[RA_X];
        rArm.rotateAngleY = p[RA_Y];
        rArm.rotateAngleZ = p[RA_Z];
        rFore.rotateAngleX = Math.min(0F, p[R_ELBOW]);
        lArm.rotateAngleX = p[LA_X];
        lArm.rotateAngleY = p[LA_Y];
        lArm.rotateAngleZ = p[LA_Z];
        lFore.rotateAngleX = Math.min(0F, p[L_ELBOW]);
        // Palms turned in toward the body unless something turns them; the wrist bends toward the palm.
        rHand.rotateAngleY = -1.25F + p[R_TWIST];
        rHand.rotateAngleX = p[R_WRIST];
        lHand.rotateAngleY = 1.25F + p[L_TWIST];
        lHand.rotateAngleX = p[L_WRIST];
        rFingers.rotateAngleX = -clamp(p[R_CURL]) * 1.5F;
        lFingers.rotateAngleX = -clamp(p[L_CURL]) * 1.5F;
        rThumb.rotateAngleX = -clamp(p[R_CURL]) * 0.9F;
        rThumb.rotateAngleZ = 0.35F - p[R_THUMB];
        lThumb.rotateAngleX = -clamp(p[L_CURL]) * 0.9F;
        lThumb.rotateAngleZ = -0.35F + p[L_THUMB];
        rLeg.rotateAngleX = p[RL_X];
        rLeg.rotateAngleY = p[RL_Y];
        rLeg.rotateAngleZ = p[RL_Z];
        rShin.rotateAngleX = Math.max(0F, p[R_KNEE]);
        lLeg.rotateAngleX = p[LL_X];
        lLeg.rotateAngleY = p[LL_Y];
        lLeg.rotateAngleZ = p[LL_Z];
        lShin.rotateAngleX = Math.max(0F, p[L_KNEE]);
        // Feet: flat on the ground whatever the knee does, unless rolled onto the toes or the heel.
        rFoot.rotateAngleX = -(p[LEAN] + p[RL_X] + Math.max(0F, p[R_KNEE])) * (p[LEAN] < -1F ? 0F : 1F) + p[R_FOOT];
        lFoot.rotateAngleX = -(p[LEAN] + p[LL_X] + Math.max(0F, p[L_KNEE])) * (p[LEAN] < -1F ? 0F : 1F) + p[L_FOOT];

        boolean draugr = entity instanceof EntityDraugr;
        boolean armed = entity instanceof EntityNorse ? ((EntityNorse) entity).isArmed() : draugr && ((EntityDraugr) entity).isArmed();
        boolean hasAxe = look == NorseBoxes.HRAFN || draugr && armed;
        axeHand.showModel = hasAxe && (armed || p[AXE] > 0.5F);
        axeBelt.showModel = look == NorseBoxes.HRAFN && !axeHand.showModel;
        // The haft lies across the palm, its head forward and its edge down.
        axeHand.rotateAngleX = PI / 2F;
        axeHand.rotateAngleY = 1.25F;
        axeHand.rotateAngleZ = PI;
        axeBelt.rotateAngleX = PI - 0.15F;
        axeBelt.rotateAngleZ = 0.1F;
        boolean gardener = look == NorseBoxes.GARDENER;
        trowelHand.showModel = gardener && armed;
        trowelBelt.showModel = gardener && !armed;
        trowelHand.rotateAngleX = PI / 2F;
        trowelHand.rotateAngleY = 1.25F;
        trowelHand.rotateAngleZ = PI;
        trowelBelt.rotateAngleX = PI - 0.3F;
        trowelBelt.rotateAngleZ = -0.2F;
        gem.showModel = gardener && entity instanceof EntityNorse && ((EntityNorse) entity).isShining();
        helm.showModel = !draugr || ((EntityDraugr) entity).isHelmed();
        jaw.rotateAngleX = clamp(p[JAW]) * 0.55F;
        lid.showModel = p[LIDS] > 0.5F;
        mouth.showModel = talk > 0.05F && MathHelper.sin(age * 1.9F + MathHelper.sin(age * 0.7F) * 2F) > -0.2F || p[JAW] > 0.3F && living;

        // Skirts hang plumb from the hips whatever the body does above them, and each is pushed out by the leg behind it.
        boolean lying = p[LEAN] < -1F;
        float stride = b.prevSkirt + (b.skirt - b.prevSkirt) * partial;
        float hemLag = b.prevHem + (b.hem - b.prevHem) * partial;
        float sway = b.prevSway + (b.sway - b.prevSway) * partial;
        float[] legX = {p[RL_X], p[LL_X], p[RL_X], p[LL_X], p[RL_X], p[LL_X]};
        float[] legZ = {p[RL_Z], p[LL_Z], p[RL_Z], p[LL_Z], p[RL_Z], p[LL_Z]};
        for (int i = 0; i < 6; i++) {
            boolean front = i < 2, back = i >= 2 && i < 4;
            float out;
            if (lying) {
                upper[i].rotateAngleX = upper[i].rotateAngleZ = 0F;
                lower[i].rotateAngleX = lower[i].rotateAngleZ = 0F;
                continue;
            }
            if (front) {
                out = Math.min(0F, legX[i]) * 0.92F - Math.max(0F, legX[i]) * 0.1F;
            } else if (back) {
                out = Math.max(0F, legX[i]) * 0.85F + stride - Math.min(0F, legX[i]) * 0.12F;
            } else {
                out = (legX[i] < 0F ? legX[i] * 0.35F : legX[i] * 0.3F) + stride * 0.4F;
            }
            upper[i].rotateAngleX = out - p[LEAN];
            float spread = i == 4 ? Math.max(0F, legZ[i]) + Math.max(0F, -sway) * 0.4F : i == 5 ? Math.min(0F, legZ[i]) + Math.min(0F, -sway) * 0.4F : 0F;
            upper[i].rotateAngleZ = i == 4 ? 0.05F + spread : i == 5 ? -0.05F + spread : (i % 2 == 0 ? 0.03F : -0.03F) + sway * 0.2F;
            // The lower piece hangs plumb from wherever the upper left off, lagging behind the stride.
            float plumb = -(out - p[LEAN]) - p[LEAN];
            float lag = (back || i >= 4 ? hemLag : hemLag * 0.4F) + MathHelper.sin(age * 0.07F + i * 1.3F) * 0.012F;
            lower[i].rotateAngleX = plumb * 0.88F + lag + (front ? -0.04F : back ? 0.04F : 0F);
            lower[i].rotateAngleZ = i == 4 ? 0.06F : i == 5 ? -0.06F : 0F;
        }
        apron.rotateAngleX = lying ? 0F : Math.min(0F, Math.min(p[RL_X], p[LL_X])) * 0.85F - p[LEAN] - 0.02F;
        apron2.rotateAngleX = lying ? 0F : -apron.rotateAngleX - p[LEAN] + hemLag * 0.3F;

        // Cloak and mantle, from the shoulders.
        float previous = 0F;
        float kneel = Math.max(0F, -Math.min(p[RL_X], p[LL_X]));
        for (int i = 0; i < cloak.length; i++) {
            float angle = b.prevCloak[i] + (b.cloak[i] - b.prevCloak[i]) * partial;
            cloak[i].rotateAngleX = angle - previous;
            cloak[i].rotateAngleZ = i == 0 ? sway * 0.5F : 0F;
            previous = angle;
        }
        if (!lying) {
            cloak[0].rotateAngleX -= p[LEAN] + p[WAIST_X] + p[CHEST_X];
            cloak[0].rotateAngleX += Math.max(0F, Math.max(p[RL_X], p[LL_X])) * 0.3F + kneel * 0.35F;
        } else {
            cloak[0].rotateAngleX = 0F;
        }

        // Hair, braids and beards hang plumb from the head and swing.
        float headTilt = lying ? 0F : p[LEAN] + p[WAIST_X] + p[CHEST_X] + head.rotateAngleX;
        float turn = b.prevTurn + (b.turn - b.prevTurn) * partial;
        lockR.rotateAngleX = -headTilt * 0.9F;
        lockR.rotateAngleZ = 0.06F + Math.max(0F, sway - turn) * 0.6F - head.rotateAngleZ;
        lockL.rotateAngleX = -headTilt * 0.9F;
        lockL.rotateAngleZ = -0.06F + Math.min(0F, sway - turn) * 0.6F - head.rotateAngleZ;
        previous = 0F;
        for (int i = 0; i < braid.length; i++) {
            float angle = b.prevBraid[i] + (b.braid[i] - b.prevBraid[i]) * partial;
            braid[i].rotateAngleX = angle - previous + (i == 0 ? 0.12F - headTilt : 0F);
            braid[i].rotateAngleZ = i == 0 ? sway * 0.4F - head.rotateAngleZ : 0F;
            previous = angle;
        }
        previous = 0F;
        for (int i = 0; i < beard.length; i++) {
            float angle = b.prevBeard[i] + (b.beard[i] - b.prevBeard[i]) * partial;
            beard[i].rotateAngleX = angle - previous + (i == 0 ? -headTilt * 0.35F - jaw.rotateAngleX * (draugr ? 0F : 0F) : 0F) + talk * 0.03F
                    * MathHelper.sin(age * 1.9F);
            previous = angle;
        }
        // A robe's open sleeves hang plumb from the elbow, swinging after it.
        float sleeve = b.prevSleeve + (b.sleeve - b.prevSleeve) * partial;
        float up = lying ? 0F : p[LEAN] + p[WAIST_X] + p[CHEST_X];
        sleeveR.rotateAngleX = -(up + p[RA_X] + Math.min(0F, p[R_ELBOW])) * 0.85F + sleeve;
        sleeveR.rotateAngleZ = -p[RA_Z] * 0.8F + 0.05F;
        sleeveL.rotateAngleX = -(up + p[LA_X] + Math.min(0F, p[L_ELBOW])) * 0.85F + sleeve;
        sleeveL.rotateAngleZ = -p[LA_Z] * 0.8F - 0.05F;
        cloth(p, b, age, lying, up, sleeve, sway);
    }

    /**
     * Her cape. Its rows hang from her shoulders and from each other, and swing as any cloak does,
     * held off her back so they fall clear of her, a slow ripple running down them so the cloth never
     * quite stops moving; the pleats in every row are turned the same, so they run unbroken from
     * the shoulders to the hem, back and forward, the outermost wrapped round her sides. The falls
     * from her wrists hang plumb, stretched to reach the ground from however high her wrists are,
     * each length below the first billowing a little on its own. Each row hangs from wherever the one
     * above it has unrolled to, so cloth still coming out of the dark onto her (see {@link Peel#grow})
     * unrolls from the top.
     */
    private void cloth(float[] p, NorseBody b, float age, boolean lying, float up, float swing, float sway) {
        float push = Math.max(0F, Math.max(p[RL_X], p[LL_X])) * 0.3F;
        float previous = 0F;
        for (int i = 0; i < NorseBoxes.CAPE_SEGMENTS; i++) {
            ModelRenderer r = rows[i];
            if (i > 0) {
                r.rotationPointY = (NorseBoxes.CAPE_LENGTH - 0.25F) * grow(pleats[0][0][i - 1]);
            }
            for (int side = 0; side < 2; side++) {
                float turned = 0F;
                for (int j = 0; j < NorseBoxes.CAPE_PLEATS; j++) {
                    float turn = lying ? 0F : NorseBoxes.PLEAT_TURNS[j] * (side == 0 ? 1F : -1F);
                    pleats[side][j][i].rotateAngleY = turn - turned;
                    turned = turn;
                }
            }
            if (lying) {
                r.rotateAngleX = r.rotateAngleZ = 0F;
                continue;
            }
            int j = Math.min(i, NorseBoxes.CLOAK_SEGMENTS - 1);
            float angle = b.prevCloak[j] + (b.cloak[j] - b.prevCloak[j]) * partial;
            float ripple = MathHelper.sin(age * 0.085F + i * 1.15F) * 0.015F * i + MathHelper.sin(age * 0.047F) * 0.02F;
            // Off her back at the shoulders, and on down at the same angle: the less each row bends from the one
            // above, the closer its pleats meet the ones above them.
            float a = angle + ripple + 0.17F;
            r.rotateAngleX = a - previous - (i == 0 ? up - push : 0F);
            previous = a;
            r.rotateAngleZ = i == 0 ? sway * 0.4F : 0F;
        }
        float shoulder = root.rotationPointY - 13F, roll = p[TILT] + p[CHEST_Z];
        for (int side = 0; side < 2; side++) {
            boolean right = side == 0;
            float x = right ? p[RA_X] : p[LA_X], z = right ? p[RA_Z] : p[LA_Z];
            float elbow = (right ? rFore : lFore).rotateAngleX;
            // Turned in toward her back, so that from in front it opens out like a wing rather than hanging edge-on.
            ModelRenderer top = drapes[side][0];
            top.rotateAngleX = -(up + x + elbow) + swing * 0.6F;
            top.rotateAngleY = -(right ? p[RA_Y] - 0.75F : p[LA_Y] + 0.75F);
            top.rotateAngleZ = -(z + roll);
            float c = MathHelper.cos(z + roll);
            float wrist = shoulder + 4.5F * MathHelper.cos(x + up) * c + 3.4F * MathHelper.cos(x + up + elbow) * c;
            float stretch = Math.max(0.12F, (23.4F - wrist) / (NorseBoxes.DRAPE_LENGTH * NorseBoxes.DRAPES));
            for (int i = 0; i < NorseBoxes.DRAPES; i++) {
                ModelRenderer d = drapes[side][i];
                if (d instanceof Peel) {
                    ((Peel) d).sy = stretch;
                }
                if (i > 0) {
                    float t = age * 0.075F + i * 1.3F + side * 2.4F;
                    d.rotationPointY = NorseBoxes.DRAPE_LENGTH * stretch * grow(drapes[side][i - 1]);
                    // Billowing out from her and back, a little more each length down; never swung across its own plane.
                    d.rotateAngleX = 0F;
                    d.rotateAngleZ = (MathHelper.sin(t * 0.8F + 1F) * 0.04F + 0.03F) * (right ? 1F : -1F);
                }
            }
        }
    }

    private static float grow(ModelRenderer part) {
        return part instanceof Peel ? ((Peel) part).grow : 1F;
    }

    private static float blendTicks(NorsePose to, NorsePose from) {
        if (to == NorsePose.FALLEN) {
            return 5F;
        }
        if (from.lying() && to != NorsePose.RISE && to != NorsePose.DEAD) {
            return 30F;
        }
        if (to == NorsePose.DEAD && from == NorsePose.LIE) {
            // The life going out: the head falls, the hand drops, slowly.
            return 26F;
        }
        if (to.lying()) {
            return 20F;
        }
        switch (to) {
            case SWING:
            case CLAW:
                return 3F;
            case RISE:
                return 1F;
            case KNEEL:
            case HOLD:
            case TOUCH:
            case GROUND:
            case TEND:
            case PRESS:
                return 24F;
            case SKY:
            case FIST:
            case CUP:
            case RAISE:
                return 26F;
            case NOD:
                return 6F;
            default:
                return 12F;
        }
    }

    // ------------------------------------------------------------------ the poses

    static void animate(int look, NorsePose state, float t, float age, float[] p, int seed) {
        switch (state) {
            case STAND:
                stand(p, look, age, seed);
                break;
            case MOURN:
                mourn(p, look, age);
                break;
            case KNEEL:
                kneel(p, look, age);
                break;
            case HOLD:
                hold(p, look, age);
                break;
            case CLOSE:
                close(p, look, t, age);
                break;
            case TOUCH:
                touch(p, look, age);
                break;
            case LIE:
                lie(p, t, age);
                break;
            case DEAD:
                dead(p, seed);
                break;
            case SKY:
                sky(p, look, t, age);
                break;
            case FIST:
                fist(p, look, t, age);
                break;
            case CUP:
                cup(p, look, t, age);
                break;
            case PALM:
                palm(p, look, t, age);
                break;
            case GROUND:
                ground(p, look, t, age);
                break;
            case NOD:
                nod(p, look, t, age, seed);
                break;
            case POINT:
                point(p, look, t, age, seed);
                break;
            case DRAW:
                draw(p, t, age);
                break;
            case FIGHT:
                guard(p, age);
                break;
            case SWING:
                swing(p, t, age);
                break;
            case RISE:
                rise(p, t, age, seed);
                break;
            case HUNT:
                hunt(p, age, seed);
                break;
            case CLAW:
                claw(p, t, age, seed);
                break;
            case FALLEN:
                fallen(p, t, seed);
                break;
            case TEND:
                tend(p, look, age, seed);
                break;
            case PRESS:
                press(p, look, t, age);
                break;
            case RAISE:
                raise(p, look, t, age);
                break;
            case SLEEP:
                sleep(p, age, seed);
                break;
            case STIR:
                stir(p, t, age, seed);
                break;
            case LECTURE:
                lecture(p, look, age, seed);
                break;
            case REACH:
                reach(p, look, t, age, seed);
                break;
            case RECOIL:
                recoil(p, look, t, age, seed);
                break;
            case PLEAD:
                plead(p, look, t, age, seed);
                break;
            case REGARD:
                regard(p, t, age);
                break;
            case GRIP:
                grip(p, t, age);
                break;
            case BLAST:
                blast(p, t, age, seed);
                break;
            case HOVER:
                hover(p, age, seed);
                break;
            case TAKE:
                take(p, t, age, seed);
                break;
            case SEIZED:
                seized(p, t, age);
                break;
            case TEAR:
                tear(p, t, age);
                break;
            case FURY:
                fury(p, t, age, seed);
                break;
        }
    }

    /**
     * Taken hold of from across the room and lifted: the jolt of it, the hands flying to her
     * throat and clawing there, her back arching further and further, the feet pointed and
     * kicking at nothing, the mouth open on a scream that does not come out.
     */
    private static void seized(float[] p, float t, float age) {
        float jolt = ModelAdamWarlock.outBack(clamp(t / 5F));
        float arch = smooth((t - 4F) / 40F);
        float kick = MathHelper.sin(age * 0.55F) * 0.28F * (1F - 0.5F * arch);
        float claw = MathHelper.sin(age * 1.3F) * 0.1F;
        p[HEAD_X] = -0.35F * jolt - 0.45F * arch;
        p[HEAD_Z] = MathHelper.sin(age * 0.4F) * 0.08F;
        p[CHEST_X] = -0.25F * jolt - 0.35F * arch;
        p[WAIST_X] = -0.2F * arch;
        p[RA_X] = -2.3F + claw;
        p[RA_Y] = -0.6F;
        p[R_ELBOW] = -1.45F;
        p[LA_X] = -2.3F - claw;
        p[LA_Y] = 0.6F;
        p[L_ELBOW] = -1.45F;
        p[R_CURL] = p[L_CURL] = 0.8F + claw;
        p[RL_X] = 0.15F + kick;
        p[LL_X] = 0.15F - kick;
        p[R_KNEE] = 0.3F + Math.max(0F, kick);
        p[L_KNEE] = 0.3F + Math.max(0F, -kick);
        p[R_FOOT] = p[L_FOOT] = 0.6F;
        p[JAW] = 0.25F + 0.45F * jolt;
        p[LIDS] = 0F;
        p[LOOK] = 0F;
    }

    /**
     * Her lid flipped: she drops into a crouch with her claws out and her jaw open wider than it
     * goes, and her head jerks to a new angle every couple of ticks, too fast to follow, as if
     * something inside it were trying to get out.
     */
    private static void fury(float[] p, float t, float age, int seed) {
        float in = ModelAdamWarlock.outBack(clamp(t / 4F));
        float step = (float) Math.floor(age / 2F);
        float j1 = jerk(step, seed), j2 = jerk(step + 17F, seed), j3 = jerk(step + 41F, seed);
        p[LEAN] = 0.12F * in;
        p[CHEST_X] = 0.25F * in + j1 * 0.05F;
        p[HEAD_X] = -0.45F * in + j1 * 0.25F;
        p[HEAD_Z] = j2 * 0.65F;
        p[HEAD_Y] = j3 * 0.5F;
        p[RA_X] = -1.35F * in;
        p[RA_Z] = 0.65F * in;
        p[R_ELBOW] = -0.45F;
        p[LA_X] = -1.35F * in;
        p[LA_Z] = -0.65F * in;
        p[L_ELBOW] = -0.45F;
        p[R_CURL] = p[L_CURL] = 0.55F;
        p[R_WRIST] = p[L_WRIST] = -0.5F;
        p[R_KNEE] = p[L_KNEE] = 0.4F * in;
        p[RL_Z] = 0.15F * in;
        p[LL_Z] = -0.15F * in;
        p[DROP] = 1F * in;
        p[JAW] = 1F;
        p[LIDS] = 0F;
        p[LOOK] = 0F;
    }

    /**
     * Changing shape (see {@link Morph}): doubled up and sinking at the knees, writhing, and
     * jerking from one angle to the next, a few ticks at each, like something being pushed
     * about from the inside. When her trunk goes, her back arches over until her head hangs
     * behind her; when her head goes, it wrenches all the way round, and back.
     */
    private static void morph(float[] p, float t, float age, int seed) {
        float w = Morph.writhe(t);
        float step = (float) Math.floor(age / 3F);
        float j1 = jerk(step, seed), j2 = jerk(step + 23F, seed), j3 = jerk(step + 57F, seed);
        float s = MathHelper.sin(age * 0.55F);
        p[DROP] += 2.2F * w;
        p[R_KNEE] += 0.6F * w;
        p[L_KNEE] += 0.6F * w;
        p[LEAN] += 0.12F * w;
        p[CHEST_X] += (0.45F + 0.12F * s + 0.15F * j1) * w;
        p[HEAD_X] += 0.35F * j1 * w;
        p[HEAD_Z] += 0.7F * j2 * w;
        p[HEAD_Y] += 0.6F * j3 * w;
        p[RA_X] += (-0.7F + 0.6F * j2) * w;
        p[RA_Z] += (0.35F + 0.35F * j3) * w;
        p[LA_X] += (-0.7F + 0.6F * j3) * w;
        p[LA_Z] -= (0.35F + 0.35F * j1) * w;
        p[R_WRIST] += 0.7F * j1 * w;
        p[L_WRIST] += 0.7F * j2 * w;
        p[R_CURL] = Math.max(p[R_CURL], (0.5F + 0.5F * j3) * w);
        p[L_CURL] = Math.max(p[L_CURL], (0.5F - 0.5F * j3) * w);
        p[JAW] = Math.max(p[JAW], (0.55F + 0.45F * j2) * w);
        p[LIDS] = 0F;
        p[LOOK] = 0F;
        float arch = Morph.snap(Morph.TRUNK, t);
        p[CHEST_X] -= 1.4F * arch;
        p[HEAD_X] -= 0.8F * arch;
        p[RA_Z] += 0.8F * arch;
        p[LA_Z] -= 0.8F * arch;
        float round = Morph.snap(Morph.HEAD, t);
        if (round > 0F) {
            p[HEAD_Y] += (float) Math.PI * round * (seed % 2 == 0 ? 1F : -1F);
            p[HEAD_LIMIT] = 1F;
        }
    }

    /** A new random angle, -1 to 1, held for one step. */
    private static float jerk(float step, int seed) {
        float s = MathHelper.sin(step * 12.9898F + seed * 78.233F) * 43758.547F;
        return (s - (float) Math.floor(s)) * 2F - 1F;
    }

    /** Where each stage of taking off the skin begins (see {@link com.matoon.eternity.entity.SkinTear}); the last runs to the end. */
    private static final float[] TEAR_AT = {0F, 24F, 52F, 63F, 76F, 98F, 118F, 134F, 146F};

    /**
     * Death tearing off the pretty one. Doubled over and shaking as it splits on her; clawing at
     * her own face, the head thrown about; the left hand held out and the skin dragged off it like
     * a glove; then the right; both hands in at the hairline, bowed into it, and the face dragged
     * forward off her skull; arms crossed to the collarbones and the body torn open, thrown off
     * wide; bent double to push the rest down off her hips, and a step out of it with each foot.
     * Then she straightens, the head back and the eyes shut, and breathes out, slow, as if she had
     * been holding that breath the whole time she wore it.
     */
    private static void tear(float[] p, float t, float age) {
        int stage = 0;
        while (stage + 1 < TEAR_AT.length && t >= TEAR_AT[stage + 1]) {
            stage++;
        }
        tearStage(stage, p, t - TEAR_AT[stage], age);
        float blend = smooth((t - TEAR_AT[stage]) / 5F);
        if (stage > 0 && blend < 1F) {
            float[] was = new float[N];
            tearStage(stage - 1, was, t - TEAR_AT[stage - 1], age);
            for (int i = 0; i < N; i++) {
                p[i] = was[i] + (p[i] - was[i]) * blend;
            }
        }
        p[LOOK] = 0F;
    }

    private static void tearStage(int stage, float[] p, float s, float age) {
        // Frantic all the way through, until she is out: a shake through the whole body that never settles.
        float shake = stage < 8 ? MathHelper.sin(age * 2.7F) * MathHelper.sin(age * 1.31F + 1F) : 0F;
        switch (stage) {
            case 0: {
                // It starts to split: doubled over, the arms wrapped round herself, shaking.
                float in = smooth(s / 10F);
                p[WAIST_X] = 0.3F * in;
                p[CHEST_X] = 0.35F * in + shake * 0.06F;
                p[CHEST_Z] = shake * 0.08F;
                p[HEAD_X] = 0.35F * in;
                p[HEAD_Z] = shake * 0.12F;
                p[RA_X] = -0.9F;
                p[RA_Y] = 0.5F;
                p[R_ELBOW] = -1.5F;
                p[LA_X] = -0.9F;
                p[LA_Y] = -0.5F;
                p[L_ELBOW] = -1.5F;
                p[R_CURL] = p[L_CURL] = 0.9F;
                p[R_KNEE] = p[L_KNEE] = 0.25F * in;
                p[DROP] = 0.6F * in;
                p[JAW] = 0.3F;
                break;
            }
            case 1: {
                // Clawing at her face: both hands at it, each dragging down in turn, the head thrown back and side to side.
                float r = MathHelper.sin(age * 1.6F), l = MathHelper.sin(age * 1.6F + 3.1F);
                p[CHEST_X] = -0.15F + shake * 0.05F;
                p[HEAD_X] = -0.3F + MathHelper.sin(age * 0.9F) * 0.15F;
                p[HEAD_Y] = MathHelper.sin(age * 0.53F) * 0.4F;
                p[HEAD_Z] = shake * 0.15F;
                p[RA_X] = -2.35F + r * 0.35F;
                p[RA_Y] = -0.45F;
                p[R_ELBOW] = -1.75F + Math.max(0F, r) * 0.4F;
                p[LA_X] = -2.35F + l * 0.35F;
                p[LA_Y] = 0.45F;
                p[L_ELBOW] = -1.75F + Math.max(0F, l) * 0.4F;
                p[R_CURL] = 0.6F + 0.4F * Math.max(0F, r);
                p[L_CURL] = 0.6F + 0.4F * Math.max(0F, l);
                p[R_KNEE] = 0.2F + shake * 0.08F;
                p[L_KNEE] = 0.3F - shake * 0.08F;
                p[DROP] = 0.4F;
                p[JAW] = 0.8F;
                break;
            }
            case 2:
            case 3: {
                // A hand held out, and the other dragging the skin off it like a glove: the left, then the right.
                boolean left = stage == 2;
                float pull = s < 4F ? 0F : ModelAdamWarlock.outBack(clamp((s - 4F) / 5F));
                int held = left ? LA_X : RA_X, puller = left ? RA_X : LA_X;
                p[CHEST_X] = 0.12F + shake * 0.04F;
                p[CHEST_Y] = (left ? 0.25F : -0.25F) * (1F - pull);
                p[HEAD_X] = 0.3F;
                p[HEAD_Y] = left ? 0.3F : -0.3F;
                p[held] = -1.35F + shake * 0.05F;
                p[left ? L_ELBOW : R_ELBOW] = -0.1F;
                p[left ? LA_Y : RA_Y] = left ? -0.25F : 0.25F;
                p[left ? L_CURL : R_CURL] = 0.1F;
                // Across the body to the wrist, and then the yank, out to the side, the skin in the fist.
                p[puller] = -1.25F + pull * 0.75F;
                p[left ? RA_Y : LA_Y] = (left ? 0.75F : -0.75F) * (1F - pull);
                p[left ? RA_Z : LA_Z] = (left ? 1.0F : -1.0F) * pull;
                p[left ? R_ELBOW : L_ELBOW] = -0.7F + pull * 0.5F;
                p[left ? R_CURL : L_CURL] = 1F;
                p[R_KNEE] = p[L_KNEE] = 0.15F;
                p[DROP] = 0.3F;
                p[JAW] = 0.5F + pull * 0.4F;
                break;
            }
            case 4: {
                // Fingers in at the hairline, bowed into it, working it loose; then dragging the whole face forward and off.
                float drag = smooth((s - 12F) / 9F);
                float dig = MathHelper.sin(age * 2.2F) * 0.12F * (1F - drag);
                p[CHEST_X] = 0.25F - drag * 0.35F + shake * 0.05F;
                p[HEAD_X] = 0.5F - drag * 0.9F;
                p[HEAD_Z] = shake * 0.1F * (1F - drag);
                p[RA_X] = -2.75F + dig + drag * 1.6F;
                p[RA_Y] = -0.6F + drag * 0.4F;
                p[R_ELBOW] = -2.0F + drag * 1.2F;
                p[LA_X] = -2.75F - dig + drag * 1.6F;
                p[LA_Y] = 0.6F - drag * 0.4F;
                p[L_ELBOW] = -2.0F + drag * 1.2F;
                p[R_CURL] = p[L_CURL] = 1F;
                p[R_KNEE] = p[L_KNEE] = 0.2F;
                p[DROP] = 0.4F;
                p[JAW] = 0.2F + drag * 0.8F;
                break;
            }
            case 5: {
                // Arms crossed, a hand at each collarbone, hunched; then the body torn open down the front and thrown off wide.
                float rip = s < 12F ? 0F : ModelAdamWarlock.outBack(clamp((s - 12F) / 6F));
                p[CHEST_X] = 0.3F - rip * 0.6F + shake * 0.05F * (1F - rip);
                p[HEAD_X] = 0.25F - rip * 0.65F;
                p[RA_X] = -1.1F + rip * 0.8F;
                p[RA_Y] = 0.95F * (1F - rip);
                p[RA_Z] = rip * 1.35F;
                p[R_ELBOW] = -1.9F + rip * 1.6F;
                p[LA_X] = -1.1F + rip * 0.8F;
                p[LA_Y] = -0.95F * (1F - rip);
                p[LA_Z] = -rip * 1.35F;
                p[L_ELBOW] = -1.9F + rip * 1.6F;
                p[R_CURL] = p[L_CURL] = 1F - rip * 0.5F;
                p[R_KNEE] = p[L_KNEE] = 0.15F;
                p[DROP] = 0.3F;
                p[JAW] = 0.3F + rip * 0.6F;
                break;
            }
            case 6: {
                // Bent double, pushing the rest down off her hips with both hands.
                float push = smooth(s / 16F);
                p[WAIST_X] = 0.55F;
                p[CHEST_X] = 0.45F + shake * 0.04F;
                p[HEAD_X] = 0.2F;
                p[RA_X] = -0.55F + push * 0.3F;
                p[RA_Z] = 0.3F;
                p[R_ELBOW] = -0.3F;
                p[LA_X] = -0.55F + push * 0.3F;
                p[LA_Z] = -0.3F;
                p[L_ELBOW] = -0.3F;
                p[R_CURL] = p[L_CURL] = 0.8F;
                p[R_KNEE] = p[L_KNEE] = 0.35F + push * 0.45F;
                p[RL_X] = p[LL_X] = -0.25F - push * 0.25F;
                p[DROP] = 1F + push * 1.2F;
                p[JAW] = 0.4F;
                break;
            }
            case 7: {
                // A step out of it with each foot, rising as she goes.
                float up = smooth(s / 12F);
                float r = (float) Math.sin(clamp(s / 6F) * Math.PI), l = (float) Math.sin(clamp((s - 6F) / 6F) * Math.PI);
                p[WAIST_X] = 0.55F * (1F - up);
                p[CHEST_X] = 0.3F * (1F - up);
                p[HEAD_X] = 0.2F * (1F - up);
                p[RA_Z] = 0.3F;
                p[LA_Z] = -0.3F;
                p[R_ELBOW] = p[L_ELBOW] = -0.3F;
                p[RL_X] = -0.7F * r;
                p[R_KNEE] = 0.9F * r;
                p[LL_X] = -0.7F * l;
                p[L_KNEE] = 0.9F * l;
                p[DROP] = 1.2F * (1F - up);
                break;
            }
            default: {
                // Out. She straightens, taller than it was; the head goes back, the eyes shut, the arms open a little,
                // palms out; and she breathes out, long and slow. Then she looks at you again.
                float sigh = (float) Math.sin(clamp((s - 2F) / 16F) * Math.PI);
                float back = 1F - smooth((s - 14F) / 10F);
                p[CHEST_X] = -0.18F * sigh;
                p[HEAD_X] = -0.45F * back;
                p[HEAD_Z] = 0.1F * back;
                p[RA_Z] = 0.25F + 0.15F * sigh;
                p[LA_Z] = -0.25F - 0.15F * sigh;
                p[R_ELBOW] = p[L_ELBOW] = -0.2F;
                p[R_TWIST] = -0.9F * back;
                p[L_TWIST] = 0.9F * back;
                p[R_CURL] = p[L_CURL] = 0.15F;
                p[JAW] = 0.35F * sigh;
                p[LIDS] = back > 0.3F ? 1F : 0F;
                break;
            }
        }
    }

    // ------------------------------------------------------------------ the Sovereign: her, him, and what he is

    /** Asleep on his feet behind frost: the head bowed, the lids down, the arms hanging, a breath every few seconds. */
    private static void sleep(float[] p, float age, int seed) {
        float br = breath(age, 0.025F);
        p[HEAD_X] = 0.28F + br * 0.02F;
        p[HEAD_Z] = drift(age, 0.003F, seed) * 0.03F;
        p[CHEST_X] = 0.04F + br * 0.012F;
        p[RA_Z] = 0.06F;
        p[LA_Z] = -0.06F;
        p[R_ELBOW] = p[L_ELBOW] = -0.12F;
        p[R_CURL] = p[L_CURL] = 0.5F;
        p[LIDS] = 1F;
        p[LOOK] = 0F;
    }

    /** Still asleep in body; the head comes up, slowly, the lids open, and the eyes find you. Nothing else moves. */
    private static void stir(float[] p, float t, float age, int seed) {
        sleep(p, age, seed);
        float up = smooth(t / 30F);
        p[HEAD_X] = 0.28F - 0.3F * up;
        p[LIDS] = t < 12F ? 1F : 0F;
        p[LOOK] = up;
    }

    /**
     * Her, holding forth: chin up, shoulders back; the right hand rising and turning in slow
     * arcs, as if laying out the shape of things for someone slow; the left at her waist.
     */
    private static void lecture(float[] p, int look, float age, int seed) {
        stand(p, look, age, seed);
        float sweep = MathHelper.sin(age * 0.045F + seed);
        float lift = 0.5F + 0.5F * MathHelper.sin(age * 0.03F + seed * 2);
        p[HEAD_X] = -0.12F;
        p[CHEST_X] = -0.06F;
        p[RA_X] = -0.75F - lift * 0.35F;
        p[RA_Y] = -0.2F + sweep * 0.35F;
        p[RA_Z] = 0.15F;
        p[R_ELBOW] = -0.6F + lift * 0.2F;
        p[R_TWIST] = 0.9F + sweep * 0.3F;
        p[R_WRIST] = -0.2F + sweep * 0.15F;
        p[R_CURL] = 0.15F;
        p[LA_X] = -0.3F;
        p[LA_Y] = 0.3F;
        p[L_ELBOW] = -1.1F;
        p[L_TWIST] = -0.6F;
        p[L_CURL] = 0.35F;
        p[LOOK] = 1F;
    }

    /** A hand held out, palm up, for something to be put in it; the body leaning after it, just a little. */
    private static void reach(float[] p, int look, float t, float age, int seed) {
        stand(p, look, age, seed);
        float out = smooth(t / 14F);
        set(p, RA_X, -1.25F, out);
        set(p, RA_Y, -0.1F, out);
        set(p, RA_Z, 0.05F, out);
        set(p, R_ELBOW, -0.25F, out);
        set(p, R_TWIST, 1.35F, out);
        set(p, R_CURL, 0.12F, out);
        set(p, R_WRIST, -0.25F, out);
        p[CHEST_X] += 0.08F * out;
        p[HEAD_X] += 0.06F * out;
        p[LOOK] = 1F;
    }

    /** Back half a step, the hands coming up between her and what is coming, and held there. */
    private static void recoil(float[] p, int look, float t, float age, int seed) {
        stand(p, look, age, seed);
        float jolt = smooth(t / 5F);
        float shake = (1F - smooth((t - 6F) / 30F)) * MathHelper.sin(age * 1.7F) * 0.02F;
        p[CHEST_X] = -0.22F * jolt + shake;
        p[HEAD_X] = -0.18F * jolt;
        p[WAIST_X] = -0.08F * jolt;
        set(p, RA_X, -1.0F, jolt);
        set(p, RA_Y, 0.35F, jolt);
        set(p, R_ELBOW, -1.35F, jolt);
        set(p, R_TWIST, -0.3F, jolt);
        set(p, R_CURL, 0.25F, jolt);
        set(p, LA_X, -0.85F, jolt);
        set(p, LA_Y, -0.3F, jolt);
        set(p, L_ELBOW, -1.5F, jolt);
        set(p, L_TWIST, 0.3F, jolt);
        set(p, L_CURL, 0.3F, jolt);
        p[RL_X] = -0.1F * jolt;
        p[L_KNEE] = 0.2F * jolt;
        p[LL_X] = 0.18F * jolt;
        p[LOOK] = 1F;
    }

    /** Both hands open and held out, palms up; the head bowed a little and to one side. */
    private static void plead(float[] p, int look, float t, float age, int seed) {
        stand(p, look, age, seed);
        float out = smooth(t / 16F);
        float br = breath(age, 0.08F);
        set(p, RA_X, -0.85F, out);
        set(p, RA_Y, -0.25F, out);
        set(p, R_ELBOW, -0.55F, out);
        set(p, R_TWIST, 1.25F, out);
        set(p, R_CURL, 0.1F, out);
        set(p, LA_X, -0.85F, out);
        set(p, LA_Y, 0.25F, out);
        set(p, L_ELBOW, -0.55F, out);
        set(p, L_TWIST, -1.25F, out);
        set(p, L_CURL, 0.1F, out);
        p[HEAD_X] += 0.16F * out;
        p[HEAD_Z] += 0.12F * out;
        p[CHEST_X] += (0.1F + br * 0.03F) * out;
        p[LOOK] = 1F;
    }

    /**
     * Him, looking someone over: hands clasped at the small of his back, chest out, the chin
     * lowered and the head a little to one side, as at something small and disappointing.
     */
    private static void regard(float[] p, float t, float age) {
        float br = breath(age, 0.035F);
        float in = smooth(t / 20F);
        p[CHEST_X] = -0.1F + br * 0.01F;
        p[HEAD_X] = 0.22F * in;
        p[HEAD_Z] = 0.16F * in;
        behind(p);
        p[RL_Z] = 0.05F;
        p[LL_Z] = -0.05F;
        p[LOOK] = 1F;
        p[WALK] = 1F;
    }

    /** Both hands clasped at the small of the back. */
    private static void behind(float[] p) {
        p[RA_X] = 0.45F;
        p[RA_Y] = 0.35F;
        p[RA_Z] = -0.12F;
        p[R_ELBOW] = -1.05F;
        p[R_TWIST] = -0.4F;
        p[LA_X] = 0.45F;
        p[LA_Y] = -0.35F;
        p[LA_Z] = 0.12F;
        p[L_ELBOW] = -1.05F;
        p[L_TWIST] = 0.4F;
        p[R_CURL] = p[L_CURL] = 0.6F;
    }

    /** One hand raised at the height of someone's throat and closed on it; he looks up the arm at them, unhurried. */
    private static void grip(float[] p, float t, float age) {
        float up = smooth(t / 10F);
        float br = breath(age, 0.04F);
        p[CHEST_X] = -0.1F * up + br * 0.01F;
        p[HEAD_X] = -0.25F * up;
        set(p, RA_X, -2.05F, up);
        set(p, RA_Y, -0.05F, up);
        set(p, RA_Z, 0.05F, up);
        set(p, R_ELBOW, -0.25F, up);
        set(p, R_TWIST, 0.2F, up);
        set(p, R_CURL, 0.85F, up);
        set(p, R_THUMB, 0.6F, up);
        p[LA_Z] = -0.1F;
        p[L_ELBOW] = -0.2F;
        p[L_CURL] = 0.7F;
        p[LOOK] = 0.3F;
    }

    /** Both hands drawn back to the right hip, a beat, and thrown forward open; then they settle. */
    private static void blast(float[] p, float t, float age, int seed) {
        hover(p, age, seed);
        float wind = smooth(t / 9F) * (1F - smooth((t - 10F) / 3F));
        float thrust = smooth((t - 10F) / 3F) * (1F - smooth((t - 18F) / 8F));
        set(p, CHEST_Y, 0.35F, wind);
        set(p, RA_X, -0.2F, wind);
        set(p, RA_Y, 0.4F, wind);
        set(p, R_ELBOW, -1.4F, wind);
        set(p, LA_X, -0.5F, wind);
        set(p, LA_Y, 0.9F, wind);
        set(p, L_ELBOW, -1.5F, wind);
        set(p, CHEST_Y, -0.1F, thrust);
        set(p, CHEST_X, 0.12F, thrust);
        set(p, RA_X, -1.5F, thrust);
        set(p, RA_Y, 0.15F, thrust);
        set(p, R_ELBOW, -0.05F, thrust);
        set(p, R_TWIST, 0.2F, thrust);
        set(p, R_CURL, 0F, thrust);
        set(p, LA_X, -1.5F, thrust);
        set(p, LA_Y, -0.15F, thrust);
        set(p, L_ELBOW, -0.05F, thrust);
        set(p, L_TWIST, -0.2F, thrust);
        set(p, L_CURL, 0F, thrust);
        p[LOOK] = 1F;
    }

    /** In the air: the legs hanging easy, one a little behind the other, the arms a little out, the toes down. */
    private static void hover(float[] p, float age, int seed) {
        float br = breath(age, 0.04F);
        float bob = drift(age, 0.02F, seed);
        p[CHEST_X] = -0.04F + br * 0.012F;
        p[RA_Z] = 0.22F + bob * 0.04F;
        p[LA_Z] = -0.22F - bob * 0.04F;
        p[RA_X] = -0.1F;
        p[LA_X] = -0.1F;
        p[R_ELBOW] = p[L_ELBOW] = -0.3F;
        p[R_CURL] = p[L_CURL] = 0.4F;
        p[R_TWIST] = 0.4F;
        p[L_TWIST] = -0.4F;
        p[RL_X] = 0.1F;
        p[R_KNEE] = 0.25F;
        p[LL_X] = 0.32F;
        p[L_KNEE] = 0.5F;
        p[R_FOOT] = p[L_FOOT] = 0.55F;
        p[LOOK] = 1F;
    }

    /** A hand out, closing on something, and drawn back to the chest with it. */
    private static void take(float[] p, float t, float age, int seed) {
        hover(p, age, seed);
        float out = smooth(t / 12F) * (1F - smooth((t - 20F) / 14F));
        float close = smooth((t - 13F) / 5F);
        float home = smooth((t - 20F) / 14F);
        set(p, RA_X, -1.45F, out);
        set(p, R_ELBOW, -0.1F, out);
        set(p, R_TWIST, 0.8F, out);
        set(p, RA_X, -0.7F, home);
        set(p, RA_Y, 0.55F, home);
        set(p, R_ELBOW, -1.6F, home);
        p[R_CURL] = 0.1F + 0.85F * close;
        p[R_THUMB] = 0.6F * close;
        p[HEAD_X] = 0.1F * home;
        p[LOOK] = 1F - 0.5F * home;
    }

    /** Breath: in slowly, out a little faster. */
    private static float breath(float age, float rate) {
        float s = MathHelper.sin(age * rate);
        return s > 0F ? s : s * 0.75F;
    }

    /** A slow wandering value, -1 to 1, never quite repeating. */
    private static float drift(float age, float rate, int seed) {
        return MathHelper.sin(age * rate + seed) * 0.6F + MathHelper.sin(age * rate * 2.37F + seed * 1.7F) * 0.4F;
    }

    /** Smoothed bumps that come and go: 0 most of the time, 1 at the top of each, every period ticks. */
    private static float now(float age, float period, float length, int seed) {
        float k = ((age + seed * 31) % period) / length;
        return k >= 1F ? 0F : MathHelper.sin(k * PI);
    }

    /**
     * Standing, each in their own way. The stranger: perfectly still but for her breath, her
     * hands folded at her waist, her weight on one foot, her head tipped a little toward
     * whatever she is attending to. Hrafn: tired, his thumbs hooked in his belt, his weight
     * shifting; now and then a long breath out and his head dropping with it. Death: nothing at
     * all moves. The dead: swaying.
     */
    private static void stand(float[] p, int look, float age, int seed) {
        p[LOOK] = 1F;
        p[WALK] = 1F;
        p[R_CURL] = p[L_CURL] = 0.35F;
        switch (look) {
            case NorseBoxes.STRANGER: {
                float br = breath(age, 0.05F);
                float shift = drift(age, 0.008F, seed);
                p[CHEST_X] = -0.03F + br * 0.012F;
                p[HEAD_Z] = 0.07F + shift * 0.025F;
                p[HEAD_X] = 0.04F;
                p[TILT] = 0.025F;
                p[RL_Z] = 0.03F;
                p[LL_Z] = -0.05F;
                p[L_KNEE] = 0.1F;
                p[LL_X] = -0.04F;
                folded(p, 0.42F);
                break;
            }
            case NorseBoxes.DEATH:
                p[HEAD_X] = 0.1F;
                p[HEAD_Z] = 0.05F;
                folded(p, 0.45F);
                p[R_CURL] = p[L_CURL] = 0.55F;
                break;
            case NorseBoxes.HRAFN: {
                float br = breath(age, 0.06F);
                float shift = drift(age, 0.013F, seed);
                float sigh = now(age, 260F, 60F, seed);
                p[CHEST_X] = 0.08F + br * 0.02F - sigh * 0.1F;
                p[HEAD_X] = 0.1F + sigh * 0.28F;
                p[HEAD_Z] = shift * 0.04F;
                p[TILT] = shift * 0.04F;
                p[WAIST_Y] = shift * 0.05F;
                p[RL_Z] = 0.05F + shift * 0.03F;
                p[LL_Z] = -0.05F + shift * 0.03F;
                p[L_KNEE] = Math.max(0F, shift) * 0.2F;
                p[R_KNEE] = Math.max(0F, -shift) * 0.2F;
                // Thumbs hooked in his belt.
                p[RA_X] = -0.12F;
                p[RA_Z] = 0.28F;
                p[R_ELBOW] = -0.85F;
                p[RA_Y] = -0.25F;
                p[LA_X] = -0.12F;
                p[LA_Z] = -0.28F;
                p[L_ELBOW] = -0.85F;
                p[LA_Y] = 0.25F;
                p[R_CURL] = p[L_CURL] = 0.7F;
                p[R_WRIST] = p[L_WRIST] = 0.3F;
                break;
            }
            case NorseBoxes.DRAUGR:
            case NorseBoxes.SPAWN:
                hunt(p, age, seed);
                p[WALK] = 1F;
                break;
            case NorseBoxes.AYESHA: {
                // A queen being looked at: dead still, chin up, shoulders back, hands clasped
                // low before her; she breathes as if even that were a choice.
                float br = breath(age, 0.035F);
                p[CHEST_X] = -0.07F + br * 0.008F;
                p[HEAD_X] = -0.1F;
                p[HEAD_Z] = drift(age, 0.004F, seed) * 0.02F;
                p[RL_Z] = 0.02F;
                p[LL_Z] = -0.03F;
                p[L_KNEE] = 0.06F;
                folded(p, 0.3F);
                p[R_CURL] = p[L_CURL] = 0.3F;
                break;
            }
            case NorseBoxes.TEMPTRESS: {
                // All her weight on one leg, the hip thrown out over it, one gloved hand resting
                // there and the other loose at her side; her head tilted, her chin down a little,
                // so she looks up at you through her lashes. She sways, slowly, as if to music
                // nobody else can hear.
                float br = breath(age, 0.04F);
                float sway = MathHelper.sin(age * 0.035F + seed);
                p[CHEST_X] = -0.05F + br * 0.012F;
                p[WAIST_Y] = 0.12F + sway * 0.04F;
                p[TILT] = -0.09F + sway * 0.03F;
                p[HEAD_Z] = 0.16F + sway * 0.03F;
                p[HEAD_X] = 0.07F;
                p[RL_Z] = 0.08F;
                p[LL_Z] = -0.02F;
                p[L_KNEE] = 0.32F;
                p[LL_X] = -0.12F;
                // The right hand on her hip.
                p[RA_X] = -0.05F;
                p[RA_Z] = 0.42F;
                p[R_ELBOW] = -1.05F;
                p[RA_Y] = -0.75F;
                p[R_WRIST] = 0.4F;
                p[R_CURL] = 0.2F;
                // The left loose, the fingers a little open.
                p[LA_Z] = -0.26F + sway * 0.02F;
                p[L_ELBOW] = -0.18F;
                p[L_CURL] = 0.2F;
                break;
            }
            case NorseBoxes.UNVEILED: {
                // She doesn't stand; she hangs in the air a hand's breadth off the ground, rising and
                // settling as if on a slow tide. Her arms held out and down, so the cape between her
                // wrists and the ground opens like wings; one knee drawn up, both feet pointed; the
                // skull tilted a little, considering you. Nothing breathes.
                float tide = MathHelper.sin(age * 0.045F + seed);
                p[DROP] = -2.2F + tide * 0.6F;
                p[WALK] = 0.15F;
                p[CHEST_X] = -0.06F;
                p[HEAD_Z] = 0.14F + MathHelper.sin(age * 0.02F + seed) * 0.05F;
                p[HEAD_X] = 0.08F;
                p[RA_Z] = 0.72F + tide * 0.05F;
                p[LA_Z] = -0.72F - tide * 0.05F;
                p[RA_X] = p[LA_X] = -0.15F;
                p[R_ELBOW] = p[L_ELBOW] = -0.2F;
                p[R_WRIST] = p[L_WRIST] = -0.35F;
                p[R_TWIST] = 0.9F;
                p[L_TWIST] = -0.9F;
                p[R_CURL] = p[L_CURL] = 0.12F;
                p[RL_X] = -0.75F;
                p[R_KNEE] = 1.25F;
                p[RL_Z] = 0.06F;
                p[LL_X] = 0.08F;
                p[LL_Z] = -0.03F;
                p[L_KNEE] = 0.1F;
                p[R_FOOT] = 1.0F;
                p[L_FOOT] = 0.85F;
                break;
            }
            case NorseBoxes.MAGUS: {
                // Hands behind his back, his weight dead even, his chin a fraction high. He
                // does not shift, or fidget, or look away first.
                float br = breath(age, 0.03F);
                p[CHEST_X] = -0.1F + br * 0.008F;
                p[HEAD_X] = -0.06F;
                behind(p);
                p[RL_Z] = 0.06F;
                p[LL_Z] = -0.06F;
                break;
            }
            case NorseBoxes.GARDENER: {
                // An old man's patience: a little stooped, his hands folded low, breathing slowly, his
                // weight settling from foot to foot; and every so often his eyes go down to what is
                // growing at his feet, and stay there a while, and come back up to you.
                float br = breath(age, 0.04F);
                float shift = drift(age, 0.006F, seed);
                float down = now(age, 340F, 70F, seed);
                p[CHEST_X] = 0.12F + br * 0.015F;
                p[WAIST_X] = 0.04F;
                p[HEAD_X] = -0.04F + down * 0.45F;
                p[HEAD_Z] = shift * 0.04F + 0.03F;
                p[TILT] = shift * 0.03F;
                p[RL_Z] = 0.04F;
                p[LL_Z] = -0.04F;
                p[L_KNEE] = Math.max(0F, shift) * 0.15F;
                p[R_KNEE] = Math.max(0F, -shift) * 0.15F;
                folded(p, 0.2F);
                p[R_CURL] = 0.5F;
                p[L_CURL] = 0.4F;
                p[LOOK] = 1F - down * 0.8F;
                break;
            }
            default: {
                float br = breath(age, 0.07F);
                p[CHEST_X] = -0.02F + br * 0.015F;
                p[RA_Z] = 0.07F;
                p[LA_Z] = -0.07F;
                p[R_ELBOW] = p[L_ELBOW] = -0.15F;
                break;
            }
        }
    }

    /** Hands loosely folded at the waist, the left over the right. */
    private static void folded(float[] p, float elbow) {
        p[RA_X] = -0.3F;
        p[RA_Y] = -0.22F;
        p[RA_Z] = -0.06F;
        p[R_ELBOW] = -elbow - 0.55F;
        p[R_TWIST] = 0.6F;
        p[LA_X] = -0.36F;
        p[LA_Y] = 0.25F;
        p[LA_Z] = 0.06F;
        p[L_ELBOW] = -elbow - 0.6F;
        p[L_TWIST] = -0.7F;
        p[R_CURL] = 0.45F;
        p[L_CURL] = 0.3F;
    }

    /** At a grave: the head bowed, the hands folded, the breath slow; Hrafn's shoulders down. */
    private static void mourn(float[] p, int look, float age) {
        stand(p, look, age, 0);
        float br = breath(age, 0.04F);
        p[HEAD_X] = 0.48F + br * 0.02F;
        p[HEAD_Z] = 0.04F;
        p[CHEST_X] = 0.07F + br * 0.012F;
        folded(p, 0.35F);
        if (look == NorseBoxes.HRAFN) {
            p[CHEST_X] = 0.14F;
            p[RA_X] = -0.15F;
            p[LA_X] = -0.15F;
        }
        p[LOOK] = 0.15F;
    }

    /** Down on the left knee, the right foot planted. */
    private static void kneel(float[] p, int look, float age) {
        float br = breath(age, look == NorseBoxes.DEATH ? 0F : 0.05F);
        p[DROP] = 5.7F;
        p[RL_X] = -1.42F;
        p[R_KNEE] = 1.42F;
        p[LL_X] = 0.06F;
        p[L_KNEE] = 1.52F;
        p[L_FOOT] = 0.9F;
        p[LL_Z] = -0.04F;
        p[RL_Z] = 0.07F;
        p[WAIST_X] = 0.08F;
        p[CHEST_X] = 0.07F + br * 0.012F;
        p[HEAD_X] = 0.28F;
        // The right forearm along the raised knee, the left hand loose at her side.
        p[RA_X] = -0.55F;
        p[RA_Z] = 0.05F;
        p[R_ELBOW] = -0.5F;
        p[R_WRIST] = 0.3F;
        p[R_CURL] = 0.4F;
        p[LA_X] = -0.1F;
        p[LA_Z] = -0.08F;
        p[L_ELBOW] = -0.3F;
        p[L_CURL] = 0.35F;
        p[LOOK] = 0.6F;
    }

    /** Kneeling, both hands round someone's hand, the head bent over it, a thumb moving across the back of it. */
    private static void hold(float[] p, int look, float age) {
        kneel(p, look, age);
        float br = breath(age, 0.045F);
        p[WAIST_X] = 0.24F;
        p[CHEST_X] = 0.2F + br * 0.012F;
        p[HEAD_X] = 0.42F;
        p[HEAD_Z] = 0.08F;
        p[RA_X] = -0.78F;
        p[RA_Y] = -0.3F;
        p[R_ELBOW] = -0.55F;
        p[R_TWIST] = 0.9F;
        p[R_CURL] = 0.55F;
        p[LA_X] = -0.82F;
        p[LA_Y] = 0.35F;
        p[L_ELBOW] = -0.5F;
        p[L_TWIST] = -0.4F;
        p[L_CURL] = 0.7F;
        p[R_THUMB] = 0.4F + MathHelper.sin(age * 0.11F) * 0.35F;
        p[LOOK] = 0.45F;
    }

    /** Kneeling: the right hand goes down over the eyes, draws down once, and comes to rest on the chest. */
    private static void close(float[] p, int look, float t, float age) {
        hold(p, look, age);
        float reach = smooth((t - 4F) / 16F);
        float stroke = smooth((t - 20F) / 18F);
        float back = smooth((t - 42F) / 16F);
        set(p, RA_X, -1.0F, reach);
        set(p, RA_Y, -0.05F, reach);
        set(p, R_ELBOW, -0.25F, reach);
        set(p, R_TWIST, 1.25F, reach);
        set(p, R_WRIST, 0.9F - stroke * 0.5F, reach);
        set(p, R_CURL, 0.05F, reach);
        set(p, WAIST_X, 0.36F, reach);
        set(p, HEAD_X, 0.6F, reach);
        // The hand comes back up and to her own chest, and stays a moment.
        set(p, RA_X, -0.55F, back);
        set(p, RA_Y, -0.55F, back);
        set(p, R_ELBOW, -1.5F, back);
        set(p, R_WRIST, 0.2F, back);
        set(p, R_CURL, 0.25F, back);
        set(p, LA_X, -0.4F, reach);
    }

    /** Kneeling, a hand laid flat on a still chest; the head down; nothing else. */
    private static void touch(float[] p, int look, float age) {
        kneel(p, look, age);
        p[WAIST_X] = 0.32F;
        p[CHEST_X] = 0.2F;
        p[HEAD_X] = 0.55F;
        p[HEAD_Z] = 0.1F;
        p[RA_X] = -0.98F;
        p[RA_Y] = -0.12F;
        p[R_ELBOW] = -0.15F;
        p[R_TWIST] = 1.25F;
        p[R_WRIST] = 0.65F;
        p[R_CURL] = 0.05F;
        p[LA_X] = -0.2F;
        p[L_ELBOW] = -0.35F;
        p[L_CURL] = 0.5F;
        p[LOOK] = 0.08F;
    }

    /**
     * On her back and dying: shallow, quick breaths, the head turned toward whoever kneels at her
     * right, her right hand up and closing again and again on the hand that holds it, the lids
     * heavy.
     */
    private static void lie(float[] p, float t, float age) {
        dead(p, 0);
        float br = MathHelper.sin(age * 0.24F);
        p[HEAD_Y] = 0.7F + MathHelper.sin(age * 0.05F) * 0.06F;
        p[HEAD_X] = -0.2F + MathHelper.sin(age * 0.031F) * 0.05F;
        p[CHEST_X] = br * 0.03F;
        p[WAIST_X] = br * 0.01F;
        p[RA_X] = -0.55F;
        p[RA_Z] = 0.45F;
        p[R_ELBOW] = -0.6F;
        p[R_TWIST] = 1.1F;
        p[R_CURL] = 0.45F + Math.max(0F, MathHelper.sin(age * 0.17F)) * 0.45F;
        p[LA_X] = -0.15F;
        p[L_ELBOW] = -0.45F;
        p[L_CURL] = 0.4F + MathHelper.sin(age * 0.13F + 1F) * 0.2F;
        float heavy = Math.max(0F, MathHelper.sin(age * 0.09F));
        p[LIDS] = heavy > 0.85F ? 1F : 0F;
        p[JAW] = 0.25F + br * 0.15F;
        p[LOOK] = 0F;
    }

    /** On their back and still: the eyes shut, the head fallen to one side, the hands half open. */
    private static void dead(float[] p, int seed) {
        p[LEAN] = -PI / 2F;
        p[DROP] = 10.2F;
        p[HEAD_X] = -0.05F;
        p[HEAD_Y] = (seed & 1) == 0 ? 0.62F : -0.62F;
        p[HEAD_Z] = (seed & 2) == 0 ? 0.05F : -0.05F;
        p[RA_Z] = 0.22F;
        p[LA_Z] = -0.3F;
        p[RA_X] = -0.04F;
        p[LA_X] = -0.06F;
        p[R_ELBOW] = -0.12F;
        p[L_ELBOW] = -0.25F;
        p[R_CURL] = 0.4F;
        p[L_CURL] = 0.5F;
        p[R_TWIST] = 0.9F;
        p[L_TWIST] = -0.6F;
        p[RL_Z] = 0.06F;
        p[LL_Z] = -0.08F;
        p[RL_Y] = 0.15F;
        p[LL_Y] = -0.2F;
        p[R_FOOT] = 0.5F;
        p[L_FOOT] = 0.6F;
        p[LIDS] = 1F;
        p[JAW] = 0.2F;
        p[LOOK] = 0F;
    }

    /**
     * The face turned up, slowly, and further than up, until the body leans back after it; the
     * hands come a little away from the sides, palms out, as if the sky were weather.
     */
    private static void sky(float[] p, int look, float t, float age) {
        stand(p, look, age, 0);
        float up = smooth(t / 80F);
        set(p, RA_X, 0.05F, up);
        set(p, RA_Y, 0F, up);
        set(p, R_ELBOW, -0.15F, up);
        set(p, LA_X, 0.05F, up);
        set(p, LA_Y, 0F, up);
        set(p, L_ELBOW, -0.15F, up);
        p[RA_Z] = 0.28F * up;
        p[LA_Z] = -0.28F * up;
        p[R_TWIST] = 1.3F * up;
        p[L_TWIST] = -1.3F * up;
        p[R_CURL] = p[L_CURL] = 0.15F;
        p[HEAD_X] = -1.35F * up;
        p[CHEST_X] = -0.22F * up;
        p[WAIST_X] = -0.1F * up;
        p[LOOK] = 0F;
    }

    /** A fist raised overhead, slowly, closing as it rises, as if holding up a belt for the crowd. */
    private static void fist(float[] p, int look, float t, float age) {
        stand(p, look, age, 0);
        float up = smooth(t / 60F);
        float close = smooth((t - 30F) / 25F);
        set(p, RA_X, -2.85F, up);
        set(p, RA_Y, 0F, up);
        set(p, RA_Z, 0.12F, up);
        set(p, R_ELBOW, -0.18F, up);
        set(p, R_TWIST, 0.2F, up);
        p[R_CURL] = 0.2F + 0.8F * close;
        p[R_THUMB] = close * 0.6F;
        p[HEAD_X] = -0.32F * up;
        p[CHEST_X] = -0.08F * up;
        p[LA_Z] = -0.08F;
        p[LOOK] = 0.3F;
    }

    /** Both hands come together in front of the chest round something small, and close on it. */
    private static void cup(float[] p, int look, float t, float age) {
        stand(p, look, age, 0);
        float in = smooth(t / 32F);
        float shut = smooth((t - 42F) / 22F);
        set(p, RA_X, -1.05F, in);
        set(p, RA_Y, -0.55F, in);
        set(p, R_ELBOW, -0.95F, in);
        set(p, R_TWIST, 1.25F, in);
        set(p, LA_X, -1.05F, in);
        set(p, LA_Y, 0.55F, in);
        set(p, L_ELBOW, -0.95F, in);
        set(p, L_TWIST, -1.25F, in);
        p[R_CURL] = p[L_CURL] = 0.45F + 0.5F * shut;
        p[R_WRIST] = p[L_WRIST] = 0.3F * shut;
        p[HEAD_X] = 0.38F * in;
        p[LOOK] = 0.2F;
    }

    /** A palm flat to the stone in front, the fingers spread, the weight leaning into it. */
    private static void palm(float[] p, int look, float t, float age) {
        stand(p, look, age, 0);
        float in = smooth(t / 20F);
        set(p, RA_X, -1.48F, in);
        set(p, RA_Y, 0.05F, in);
        set(p, RA_Z, 0F, in);
        set(p, R_ELBOW, -0.12F, in);
        set(p, R_TWIST, 1.25F, in);
        set(p, R_WRIST, -0.9F, in);
        set(p, R_CURL, 0F, in);
        p[R_THUMB] = -0.3F * in;
        p[LEAN] = 0.07F * in;
        p[CHEST_X] = 0.08F * in;
        p[HEAD_X] = 0.18F * in;
        p[RL_X] = 0.15F * in;
        p[LL_X] = -0.2F * in;
        p[L_KNEE] = 0.2F * in;
        p[LOOK] = 0.3F;
    }

    /** Crouched down, a palm laid flat on the ground. */
    private static void ground(float[] p, int look, float t, float age) {
        kneel(p, look, age);
        float down = smooth(t / 28F);
        p[WAIST_X] = 0.5F * down;
        p[CHEST_X] = 0.32F * down;
        set(p, RA_X, -0.85F, down);
        set(p, R_ELBOW, -0.05F, down);
        set(p, R_TWIST, 1.25F, down);
        set(p, R_WRIST, -0.5F, down);
        set(p, R_CURL, 0F, down);
        p[HEAD_X] = 0.32F;
    }

    /**
     * Kneeling at a bed: the trowel goes in, turns the soil and comes out, over and over, his
     * other hand flat beside it firming the earth down; his head bent close over the work. Every
     * so often he sits back on his heel, the trowel resting on his knee, and looks at what he has
     * done, and at whoever is watching him do it.
     */
    private static void tend(float[] p, int look, float age, int seed) {
        kneel(p, look, age);
        float stroke = ((age + seed * 13) % 64F) / 64F;
        float dig = MathHelper.sin(stroke * PI * 2F);
        float lift = Math.max(0F, MathHelper.sin(stroke * PI * 2F - 1.2F));
        float work = 1F - now(age, 420F, 90F, seed);
        set(p, WAIST_X, 0.48F, work);
        set(p, CHEST_X, 0.26F, work);
        set(p, HEAD_X, 0.55F, work);
        p[HEAD_Z] = 0.06F;
        // The trowel hand, forward and down, rocking with the stroke; the wrist turning the soil over.
        set(p, RA_X, -0.95F - dig * 0.16F, work);
        set(p, RA_Y, -0.12F, work);
        set(p, R_ELBOW, -0.3F + dig * 0.14F, work);
        set(p, R_WRIST, 0.3F + lift * 0.5F, work);
        p[R_CURL] = 0.9F;
        // The other hand flat on the soil, pressing.
        set(p, LA_X, -0.88F, work);
        set(p, LA_Y, 0.22F, work);
        set(p, L_ELBOW, -0.08F - Math.max(0F, -dig) * 0.06F, work);
        set(p, L_TWIST, -1.25F, work);
        set(p, L_WRIST, -0.5F, work);
        set(p, L_CURL, 0F, work);
        p[LOOK] = 0.25F + (1F - work) * 0.7F;
    }

    /**
     * Kneeling: the stone held out in both hands before him; brought to his chest; pressed in,
     * his back bowing round it as it goes; then slowly he straightens, and his hands fall open
     * on his knees, and he lifts his head.
     */
    private static void press(float[] p, int look, float t, float age) {
        kneel(p, look, age);
        float hold = smooth(t / 24F);
        float bring = smooth((t - 30F) / 26F);
        float push = smooth((t - 58F) / 16F) * (1F - smooth((t - 96F) / 34F));
        float open = smooth((t - 104F) / 26F);
        set(p, RA_X, -1.0F, hold);
        set(p, RA_Y, -0.5F, hold);
        set(p, R_ELBOW, -0.7F, hold);
        set(p, R_TWIST, 1.25F, hold);
        set(p, LA_X, -1.0F, hold);
        set(p, LA_Y, 0.5F, hold);
        set(p, L_ELBOW, -0.7F, hold);
        set(p, L_TWIST, -1.25F, hold);
        p[R_CURL] = p[L_CURL] = 0.5F * (1F - bring) + 0.1F;
        set(p, RA_X, -0.5F, bring);
        set(p, RA_Y, -0.8F, bring);
        set(p, R_ELBOW, -1.65F, bring);
        set(p, R_WRIST, -0.5F, bring);
        set(p, LA_X, -0.5F, bring);
        set(p, LA_Y, 0.8F, bring);
        set(p, L_ELBOW, -1.65F, bring);
        set(p, L_WRIST, -0.5F, bring);
        p[CHEST_X] += 0.38F * push;
        p[WAIST_X] += 0.18F * push;
        p[HEAD_X] = 0.28F + 0.42F * push;
        p[R_THUMB] = p[L_THUMB] = 0.3F * push;
        set(p, RA_X, -0.32F, open);
        set(p, RA_Y, -0.1F, open);
        set(p, R_ELBOW, -0.55F, open);
        set(p, R_WRIST, 0.2F, open);
        set(p, R_TWIST, 1.3F, open);
        set(p, LA_X, -0.32F, open);
        set(p, LA_Y, 0.1F, open);
        set(p, L_ELBOW, -0.55F, open);
        set(p, L_WRIST, 0.2F, open);
        set(p, L_TWIST, -1.3F, open);
        set(p, HEAD_X, 0.08F, open);
        p[LOOK] = 0.15F + open * 0.8F;
    }

    /**
     * Standing: his arms come up from his sides, slowly, palms up, the fingers spreading, until
     * they are out in front of him at the height of his chest; then his hands close, and under
     * whoever he is looking at the ground does what they say.
     */
    private static void raise(float[] p, int look, float t, float age) {
        stand(p, look, age, 0);
        float up = smooth(t / 50F);
        float close = smooth((t - 56F) / 12F);
        set(p, RA_X, -1.3F, up);
        set(p, RA_Y, 0.12F, up);
        set(p, RA_Z, 0.1F, up);
        set(p, R_ELBOW, -0.25F, up);
        set(p, R_TWIST, 1.3F, up);
        set(p, LA_X, -1.3F, up);
        set(p, LA_Y, -0.12F, up);
        set(p, LA_Z, -0.1F, up);
        set(p, L_ELBOW, -0.25F, up);
        set(p, L_TWIST, -1.3F, up);
        p[R_CURL] = p[L_CURL] = 0.05F * (1F - close) + 0.95F * close;
        p[R_THUMB] = p[L_THUMB] = 0.5F * close;
        p[CHEST_X] = 0.12F - 0.16F * up;
        p[HEAD_X] = 0.1F;
        p[LOOK] = 1F;
    }

    /** The head inclined, held a moment, and raised: the most she gives anyone. */
    private static void nod(float[] p, int look, float t, float age, int seed) {
        stand(p, look, age, seed);
        float dip = smooth(t / 12F) * (1F - smooth((t - 24F) / 14F));
        p[HEAD_X] += 0.42F * dip;
        p[CHEST_X] += 0.05F * dip;
        p[LOOK] = 1F - dip;
    }

    /** An arm out straight at whatever they attend to, the fingers together. */
    private static void point(float[] p, int look, float t, float age, int seed) {
        stand(p, look, age, seed);
        float out = smooth(t / 16F);
        set(p, RA_X, -1.52F, out);
        set(p, RA_Y, -0.05F, out);
        set(p, RA_Z, 0F, out);
        set(p, R_ELBOW, -0.04F, out);
        set(p, R_CURL, 0.1F, out);
        set(p, R_TWIST, 1F, out);
        p[LOOK] = 1F;
    }

    /** The axe drawn off the belt, across the body and up into a guard. */
    private static void draw(float[] p, float t, float age) {
        stand(p, NorseBoxes.HRAFN, age, 0);
        float reach = smooth(t / 7F) * (1F - smooth((t - 8F) / 5F));
        set(p, RA_X, -0.25F, reach);
        set(p, RA_Y, 0.45F, reach);
        set(p, RA_Z, 0.35F, reach);
        set(p, R_ELBOW, -0.5F, reach);
        set(p, R_CURL, 0.9F, reach);
        set(p, CHEST_Y, -0.2F, reach);
        set(p, HEAD_X, 0.3F, reach);
        float[] g = new float[N];
        guard(g, age);
        float into = smooth((t - 9F) / 11F);
        for (int i = 0; i < N; i++) {
            p[i] += (g[i] - p[i]) * into;
        }
        p[AXE] = t > 8F ? 1F : 0F;
    }

    /** Feet apart, knees bent, the axe up by the right shoulder, the left hand out to fend. */
    private static void guard(float[] p, float age) {
        float br = breath(age, 0.16F);
        p[DROP] = 1.3F + br * 0.15F;
        p[LEAN] = 0.1F;
        p[CHEST_Y] = 0.25F;
        p[CHEST_X] = 0.05F + br * 0.02F;
        p[HEAD_X] = -0.05F;
        p[RA_X] = -2.2F;
        p[RA_Z] = 0.35F;
        p[R_ELBOW] = -1.35F;
        p[R_CURL] = 0.95F;
        p[R_WRIST] = -0.2F;
        p[LA_X] = -0.85F;
        p[LA_Z] = -0.2F;
        p[L_ELBOW] = -0.55F;
        p[L_CURL] = 0.2F;
        p[L_TWIST] = -1.2F;
        p[RL_X] = 0.3F;
        p[R_KNEE] = 0.4F;
        p[LL_X] = -0.4F;
        p[L_KNEE] = 0.35F;
        p[LOOK] = 0.9F;
        p[WALK] = 1F;
        p[AXE] = 1F;
    }

    /** The axe comes over and down with the whole body behind it, through, and back up. */
    private static void swing(float[] p, float t, float age) {
        guard(p, age);
        float wind = smooth(t / 3F) * (1F - smooth((t - 3F) / 2F));
        set(p, RA_X, -2.75F, wind);
        set(p, CHEST_Y, 0.45F, wind);
        float blow = strike(t, 3F, 7F, 15F);
        set(p, RA_X, -0.3F, blow);
        set(p, R_ELBOW, -0.05F, blow);
        set(p, R_WRIST, 0.3F, blow);
        set(p, CHEST_Y, -0.35F, blow);
        set(p, CHEST_X, 0.35F, blow);
        set(p, LEAN, 0.25F, blow);
        set(p, LA_X, -0.2F, blow);
        p[DROP] += 0.9F * blow;
        p[HEAD_X] += 0.15F * blow;
    }

    /** Out of the grave: they sit up all at once, roll onto a knee, and stand in jerks. */
    private static void rise(float[] p, float t, float age, int seed) {
        float[] lying = new float[N], sitting = new float[N], kneeling = new float[N], standing = new float[N];
        dead(lying, seed);
        lying[LIDS] = 0F;
        // Sitting up: the body upright on the ground, legs out straight, head down.
        sitting[DROP] = 10.4F;
        sitting[RL_X] = sitting[LL_X] = -PI / 2F;
        sitting[WAIST_X] = 0.3F;
        sitting[CHEST_X] = 0.25F;
        sitting[HEAD_X] = 0.6F;
        sitting[RA_X] = -0.35F;
        sitting[LA_X] = -0.35F;
        sitting[R_CURL] = sitting[L_CURL] = 0.8F;
        sitting[JAW] = 0.8F;
        kneel(kneeling, NorseBoxes.DRAUGR, age);
        kneeling[HEAD_X] = 0.55F;
        kneeling[CHEST_X] = 0.3F;
        kneeling[JAW] = 0.6F;
        hunt(standing, age, seed);
        // Each stage comes in a jerk, not a glide.
        float a = jerk(t / 10F), bb = jerk((t - 13F) / 9F), c = jerk((t - 24F) / 11F);
        for (int i = 0; i < N; i++) {
            float v = lying[i] + (sitting[i] - lying[i]) * a;
            v += (kneeling[i] - v) * bb;
            v += (standing[i] - v) * c;
            p[i] = v;
        }
        p[HEAD_Z] += MathHelper.sin(t * 1.3F) * 0.15F * (1F - c);
        p[HEAD_Y] += (MathHelper.sin(t * 0.9F) > 0.6F ? 0.3F : 0F) * (1F - c);
    }

    /** A snap rather than an ease: fast out, a little overshoot, settle. */
    private static float jerk(float x) {
        x = clamp(x);
        float k = 1F - (1F - x) * (1F - x) * (1F - x) * (1F - x);
        return k + MathHelper.sin(x * PI) * 0.08F;
    }

    /**
     * On their feet: stooped, the head pushed forward, the jaw hanging, the hands up and clawed;
     * the head snaps to one side now and then and back, as if listening to something underground.
     */
    private static void hunt(float[] p, float age, int seed) {
        float snap = now(age, 47F + (seed % 13), 5F, seed);
        float twitch = MathHelper.sin(age * 0.31F + seed) * MathHelper.sin(age * 0.07F);
        p[LEAN] = 0.2F;
        p[DROP] = 0.9F;
        p[CHEST_X] = 0.16F;
        p[CHEST_Z] = 0.06F * (seed % 2 == 0 ? 1 : -1);
        p[HEAD_X] = -0.3F;
        p[HEAD_Z] = 0.18F + twitch * 0.06F + snap * 0.35F;
        p[HEAD_Y] = snap * 0.4F * (seed % 3 == 0 ? -1 : 1);
        p[RA_X] = -1.0F;
        p[RA_Z] = 0.15F;
        p[R_ELBOW] = -0.4F;
        p[LA_X] = -0.8F;
        p[LA_Z] = -0.12F;
        p[L_ELBOW] = -0.6F;
        p[R_CURL] = p[L_CURL] = 0.55F + twitch * 0.15F;
        p[R_TWIST] = 1.2F;
        p[L_TWIST] = -1.2F;
        p[R_WRIST] = p[L_WRIST] = -0.35F;
        p[R_KNEE] = 0.22F;
        p[L_KNEE] = 0.3F;
        p[JAW] = 0.4F + twitch * 0.15F + snap * 0.4F;
        p[LOOK] = 0.7F;
        p[WALK] = 1F;
        p[AXE] = 1F;
    }

    /** A blow: both arms back over the shoulder, and down. */
    private static void claw(float[] p, float t, float age, int seed) {
        hunt(p, age, seed);
        float wind = smooth(t / 6F) * (1F - smooth((t - 7F) / 2F));
        set(p, RA_X, -2.7F, wind);
        set(p, LA_X, -2.4F, wind);
        set(p, CHEST_X, -0.2F, wind);
        set(p, JAW, 1F, wind);
        float blow = strike(t, 7F, 10F, 17F);
        set(p, RA_X, -0.45F, blow);
        set(p, LA_X, -0.35F, blow);
        set(p, LEAN, 0.45F, blow);
        set(p, CHEST_X, 0.4F, blow);
        set(p, R_CURL, 1F, blow);
        set(p, L_CURL, 1F, blow);
    }

    /** Knocked flat on their back, limbs thrown, twitching as long as it takes them to get up. */
    private static void fallen(float[] p, float t, int seed) {
        dead(p, seed);
        p[LIDS] = 0F;
        float shake = Math.max(0F, 1F - t / 40F);
        p[CHEST_X] += MathHelper.sin(t * 2.1F) * 0.06F * shake;
        p[HEAD_Y] *= 0.6F;
        p[RA_X] -= 0.5F;
        p[RA_Z] += 0.3F;
        p[LL_X] -= 0.35F;
        p[L_KNEE] = 0.6F;
        p[R_CURL] = p[L_CURL] = 0.8F;
        p[JAW] = 0.9F;
    }

    // ------------------------------------------------------------------ laid over the rest

    /**
     * A walk: heel to toe, the arms counter-swinging with a soft elbow, a little bob, the hips
     * rolling; the women's shorter steps with more sway; Hrafn's heavier; the dead's lurching,
     * one leg dragging.
     */
    private static void walk(float[] p, float swing, float gait, int look) {
        boolean dead = look == NorseBoxes.DRAUGR;
        boolean woman = look == NorseBoxes.STRANGER || look == NorseBoxes.RAGNA || look == NorseBoxes.DEATH || look == NorseBoxes.TEMPTRESS || look == NorseBoxes.UNVEILED;
        float phase = swing * 0.6662F;
        float s = MathHelper.cos(phase);
        float c = MathHelper.sin(phase);
        float stride = woman || look == NorseBoxes.GARDENER ? 0.5F : dead ? 0.55F : 0.65F;
        p[RL_X] += s * stride * gait;
        p[LL_X] -= s * stride * (dead ? 0.6F : 1F) * gait;
        p[R_KNEE] += Math.max(0F, -c) * 0.85F * gait;
        p[L_KNEE] += Math.max(0F, c) * (dead ? 0.4F : 0.85F) * gait;
        p[R_FOOT] += Math.max(0F, s) * -0.25F * gait + Math.max(0F, -s) * 0.3F * gait;
        p[L_FOOT] += Math.max(0F, -s) * -0.25F * gait + Math.max(0F, s) * 0.3F * gait;
        p[DROP] += (1F - Math.abs(c)) * -0.35F * gait + 0.2F * gait;
        p[TWIST] += s * (woman ? 0.06F : 0.04F) * gait;
        if (look == NorseBoxes.TEMPTRESS || look == NorseBoxes.UNVEILED) {
            // One foot set before the other, and the hips rolling with every step.
            p[WAIST_Y] += s * 0.14F * gait;
            p[TILT] += c * 0.05F * gait;
        }
        p[CHEST_Y] -= s * 0.09F * gait;
        p[TILT] += c * (woman ? 0.04F : 0.02F) * gait;
        p[HEAD_Y] += s * 0.04F * gait;
        if (dead) {
            p[TILT] += c * 0.1F * gait;
            p[HEAD_Z] += c * 0.12F * gait;
            return;
        }
        // Arms swing only if they hang free.
        float rFree = clamp(1F - Math.abs(p[RA_X] + 0.1F) * 2F - Math.abs(p[R_ELBOW] + 0.15F));
        float lFree = clamp(1F - Math.abs(p[LA_X] + 0.1F) * 2F - Math.abs(p[L_ELBOW] + 0.15F));
        p[RA_X] -= s * 0.45F * gait * rFree;
        p[LA_X] += s * 0.45F * gait * lFree;
        p[R_ELBOW] -= Math.max(0F, s) * 0.3F * gait * rFree;
        p[L_ELBOW] -= Math.max(0F, -s) * 0.3F * gait * lFree;
    }

    /**
     * Talking: the head moves with the sense of what is said, small nods on the stresses; Hrafn's
     * hand comes up and turns over as he makes a point; the stranger barely moves at all.
     */
    private static void talking(float[] p, float age, float k, int look, NorsePose state) {
        float beat = MathHelper.sin(age * 0.45F) * MathHelper.sin(age * 0.17F + 1F);
        float stress = Math.max(0F, MathHelper.sin(age * 0.33F)) * Math.max(0F, MathHelper.sin(age * 0.11F));
        float scale = look == NorseBoxes.STRANGER ? 0.4F : 1F;
        p[HEAD_X] += (beat * 0.04F + stress * 0.06F) * k * scale;
        p[HEAD_Z] += MathHelper.sin(age * 0.09F) * 0.03F * k * scale;
        p[CHEST_X] += stress * 0.02F * k * scale;
        if ((look == NorseBoxes.TEMPTRESS || look == NorseBoxes.UNVEILED) && state == NorsePose.STAND) {
            // The loose hand lifts as she speaks, palm up, the fingers curling toward her: come closer.
            float hand = Math.max(0F, MathHelper.sin(age * 0.045F)) * k;
            set(p, LA_X, -0.85F, hand);
            set(p, LA_Z, 0.05F, hand);
            set(p, L_ELBOW, -0.75F, hand);
            set(p, L_CURL, 0.15F + 0.5F * Math.max(0F, MathHelper.sin(age * 0.3F)), hand);
            p[HEAD_Z] += MathHelper.sin(age * 0.06F) * 0.04F * k;
        }
        if ((look == NorseBoxes.HRAFN || look == NorseBoxes.GARDENER) && (state == NorsePose.STAND || state == NorsePose.MOURN)) {
            // A hand comes up and opens, palm up, as if offering the point.
            float hand = Math.max(0F, MathHelper.sin(age * (look == NorseBoxes.GARDENER ? 0.05F : 0.07F))) * k;
            set(p, LA_X, -0.7F, hand);
            set(p, LA_Z, -0.1F, hand);
            set(p, L_ELBOW, -0.9F, hand);
            set(p, L_TWIST, -1.25F - 0.8F * stress, hand);
            set(p, L_CURL, 0.2F, hand);
            set(p, L_WRIST, -0.2F, hand);
        } else if (look == NorseBoxes.RAGNA) {
            p[JAW] += 0.15F * k;
        } else if (look == NorseBoxes.UNVEILED) {
            // A skull speaking: the whole jaw drops and snaps up again with the words.
            p[JAW] += k * (0.2F + 0.55F * Math.max(0F, MathHelper.sin(age * 1.7F + MathHelper.sin(age * 0.6F) * 1.5F)));
        }
    }
}
