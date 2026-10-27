package com.matoon.eternity.client.render;

/**
 * How Eternity holds himself, as a set of channels {@link ModelEternity} poses his joints from.
 * He is vast and unhurried: everything he does is slow and certain, with a long gathering
 * before each act and the weight of it after. Each of his attacks has its own gesture, read
 * from across a battlefield: a hand reaching out to unmake you, palms cupped around a dark
 * star, a hand flung up to call the lances, arms raised to bring down the sky, fists brought
 * down to break the ground.
 *
 * Arm and leg channels are written for his right side; the rig mirrors them for his left, so the same
 * number means the same gesture on either side.
 */
public final class EternityPose {

    public static final int LEAN = 0, TWIST = 1, TILT = 2, DROP = 3, WAIST_X = 4, WAIST_Y = 5, CHEST_X = 6, CHEST_Y = 7, CHEST_Z = 8,
            NECK_X = 9, HEAD_X = 10, HEAD_Y = 11, HEAD_Z = 12,
    // Each arm: swing forward (negative) or back, swing in, swing out from the body, elbow (negative bends),
    // wrist (positive bends toward the palm), forearm twist, finger curl, finger spread.
    RA_X = 13, RA_Y = 14, RA_Z = 15, R_ELBOW = 16, R_WRIST = 17, R_TWIST = 18, R_CURL = 19, R_SPREAD = 20,
            LA_X = 21, LA_Y = 22, LA_Z = 23, L_ELBOW = 24, L_WRIST = 25, L_TWIST = 26, L_CURL = 27, L_SPREAD = 28,
    // Each leg: swing forward (negative) or back, in, out, knee (positive bends), foot (positive points the toes).
    RL_X = 29, RL_Y = 30, RL_Z = 31, R_KNEE = 32, R_FOOT = 33,
            LL_X = 34, LL_Y = 35, LL_Z = 36, L_KNEE = 37, L_FOOT = 38,
    /** How far the crests stand out from the hood. */
    CRESTS = 39,
    /** How far his cloak stands out from his body. */
    CLOAK_OPEN = 40,
    /** How much his cloak floats instead of hanging. */
    CLOAK_LIFT = 41,
    /** How much his head follows whoever he is looking at. */
    LOOK = 42,
    /** How brightly his eyes burn. */
    GLOW = 43,
            N = 44;

    /** What he is doing. The first six follow his state; the rest are his attacks. */
    public static final int DRIFT = 0, EMERGE = 1, STARE = 2, HUNT = 3, DEPART = 4, DYING = 5,
            UNMAKE = 6, SINGULARITY = 7, LANCES = 8, STARFALL = 9, SHATTER = 10;

    /** Offsets from an arm's or a leg's first channel. */
    private static final int X = 0, Y = 1, Z = 2, ELBOW = 3, WRIST = 4, TWIST_ = 5, CURL = 6, SPREAD = 7, KNEE = 3, FOOT = 4;

    private EternityPose() {
    }

    /**
     * The pose for one thing he does, t ticks into it.
     *
     * @param length how long an attack's gathering lasts, in ticks, before it is loosed
     * @param pitch  how far he is looking down at his target, in radians
     */
    public static void animate(int kind, float t, float length, float age, float pitch, float[] p) {
        java.util.Arrays.fill(p, 0F);
        switch (kind) {
            case EMERGE:
                emerge(p, t, age);
                break;
            case STARE:
                stare(p, t, age);
                break;
            case HUNT:
                hunt(p, age);
                break;
            case DEPART:
                depart(p, t, age);
                break;
            case DYING:
                dying(p, t, age);
                break;
            case UNMAKE:
                unmake(p, t, length, age, pitch);
                break;
            case SINGULARITY:
                singularity(p, t, length, age, pitch);
                break;
            case LANCES:
                lances(p, t, length, age, pitch);
                break;
            case STARFALL:
                starfall(p, t, length, age);
                break;
            case SHATTER:
                shatter(p, t, length, age);
                break;
            default:
                drift(p, age);
                break;
        }
    }

    /** How quickly he moves out of whatever he was doing into this, in ticks. */
    public static float blendTicks(int kind) {
        switch (kind) {
            case UNMAKE:
            case SHATTER:
                return 5F;
            case EMERGE:
                return 2F;
            case DYING:
                return 10F;
            case DRIFT:
            case HUNT:
                return 16F;
            default:
                return 9F;
        }
    }

    // ------------------------------------------------------------------ at rest

    /**
     * Drifting: upright, unhurried, faintly stooped as a very tall thing stoops. His arms hang
     * a little away from him, the long fingers loosely curled and never quite still; his legs
     * hang one a little before the other, toes pointed, as if he had never needed to stand.
     */
    private static void drift(float[] p, float age) {
        float breath = sin(age * 0.035F);
        p[DROP] = breath * 0.5F;
        p[LEAN] = 0.05F;
        p[CHEST_X] = 0.06F + breath * 0.025F;
        p[NECK_X] = 0.12F;
        p[HEAD_X] = -0.06F;
        p[HEAD_Z] = sin(age * 0.021F) * 0.06F;
        arm(p, RA_X, -0.06F + sin(age * 0.03F) * 0.05F, 0F, 0.14F + sin(age * 0.027F) * 0.03F, -0.3F, 0.15F, 0.2F,
                0.4F + sin(age * 0.05F) * 0.12F, 0.06F + sin(age * 0.041F) * 0.04F);
        arm(p, LA_X, -0.1F + sin(age * 0.03F + 2.1F) * 0.05F, 0F, 0.13F + sin(age * 0.027F + 1F) * 0.03F, -0.38F, 0.1F, 0.2F,
                0.5F + sin(age * 0.05F + 1.7F) * 0.12F, 0.05F + sin(age * 0.037F + 1F) * 0.04F);
        leg(p, RL_X, 0.06F + sin(age * 0.018F) * 0.04F, 0.04F, 0.04F, 0.22F, 0.6F);
        leg(p, LL_X, -0.16F + sin(age * 0.018F + 3F) * 0.04F, -0.02F, 0.04F, 0.5F, 0.7F);
        p[CLOAK_OPEN] = 0.15F;
        p[LOOK] = 1F;
        p[GLOW] = 0.55F;
    }

    /**
     * He has seen you. A slow tilt of the head, wrong and curious, and then a stillness worse
     * than any movement; the hands lift a little away from him, open; the crests rise.
     */
    private static void stare(float[] p, float t, float age) {
        drift(p, age);
        float k = smooth(t / 30F);
        float still = 1F - 0.8F * k;
        p[DROP] *= still;
        p[HEAD_Z] = 0.48F * k + sin(age * 1.9F) * 0.012F * k;
        p[HEAD_X] = -0.06F + 0.1F * k;
        p[LEAN] += 0.07F * k;
        for (int o : new int[]{RA_X, LA_X}) {
            set(p, o + X, -0.32F, k);
            set(p, o + Z, 0.32F, k);
            set(p, o + ELBOW, -0.55F, k);
            set(p, o + WRIST, -0.25F, k);
            set(p, o + CURL, 0.12F, k);
            set(p, o + SPREAD, 0.32F, k);
        }
        p[CRESTS] = 0.55F * k;
        p[CLOAK_OPEN] += 0.25F * k;
        p[GLOW] = 0.55F + 0.45F * k;
        p[LOOK] = 1F;
    }

    /**
     * Hunting: leaning into you as he comes, head low and thrust forward, both long hands out
     * before him, the fingers closing and opening on nothing; his legs trail behind him.
     */
    private static void hunt(float[] p, float age) {
        float breath = sin(age * 0.05F);
        p[DROP] = breath * 0.4F;
        p[LEAN] = 0.3F;
        p[CHEST_X] = 0.12F;
        p[NECK_X] = 0.22F;
        p[HEAD_X] = -0.42F;
        float grasp = sin(age * 0.06F), other = sin(age * 0.06F + 2.4F);
        arm(p, RA_X, -0.75F + grasp * 0.12F, 0.12F, 0.3F, -0.5F, -0.1F, 0.6F, 0.3F + 0.45F * Math.max(0F, grasp), 0.25F);
        arm(p, LA_X, -0.55F + other * 0.12F, 0.1F, 0.32F, -0.6F, -0.1F, 0.6F, 0.3F + 0.45F * Math.max(0F, other), 0.25F);
        leg(p, RL_X, 0.35F + sin(age * 0.04F) * 0.06F, 0.03F, 0.05F, 0.55F, 0.85F);
        leg(p, LL_X, 0.18F + sin(age * 0.04F + 2F) * 0.06F, -0.02F, 0.05F, 0.4F, 0.8F);
        p[CLOAK_OPEN] = 0.35F;
        p[CRESTS] = 0.3F;
        p[LOOK] = 1F;
        p[GLOW] = 0.85F;
    }

    // ------------------------------------------------------------------ arriving and leaving

    /**
     * Unfolding out of a rift: curled tight with the cloak wrapped round him, then opening all
     * at once, arms flung wide and head thrown back, the cloak spread like wings; then he
     * settles and looks at the world he has come into.
     */
    private static void emerge(float[] p, float t, float age) {
        drift(p, age);
        float fold = 1F - smooth((t - 10F) / 18F);
        set(p, LEAN, 0.45F, fold);
        set(p, WAIST_X, 0.55F, fold);
        set(p, CHEST_X, 0.3F, fold);
        set(p, NECK_X, 0.35F, fold);
        set(p, HEAD_X, 0.6F, fold);
        for (int o : new int[]{RA_X, LA_X}) {
            set(p, o + X, -1.2F, fold);
            set(p, o + Y, 0.95F, fold);
            set(p, o + Z, 0F, fold);
            set(p, o + ELBOW, -1.6F, fold);
            set(p, o + CURL, 0.9F, fold);
        }
        for (int o : new int[]{RL_X, LL_X}) {
            set(p, o + X, -1.45F, fold);
            set(p, o + KNEE, 2.2F, fold);
        }
        set(p, CLOAK_OPEN, -0.35F, fold);
        set(p, GLOW, 0.1F, fold);

        float open = outBack(clamp((t - 18F) / 14F)) * (1F - smooth((t - 40F) / 26F));
        for (int o : new int[]{RA_X, LA_X}) {
            set(p, o + X, -0.55F, open);
            set(p, o + Y, 0F, open);
            set(p, o + Z, 1.45F, open);
            set(p, o + ELBOW, -0.15F, open);
            set(p, o + WRIST, -0.3F, open);
            set(p, o + CURL, 0F, open);
            set(p, o + SPREAD, 0.6F, open);
        }
        set(p, HEAD_X, -0.5F, open);
        set(p, CHEST_X, -0.22F, open);
        set(p, LEAN, -0.12F, open);
        set(p, CLOAK_OPEN, 1F, open);
        set(p, CLOAK_LIFT, 0.6F, open);
        set(p, CRESTS, 1F, open);
        set(p, GLOW, 1F, open);
        p[LOOK] = smooth((t - 45F) / 20F);
    }

    /** Leaving: he gathers his cloak about himself, bows his head and folds away. */
    private static void depart(float[] p, float t, float age) {
        drift(p, age);
        float k = smooth(t / 30F);
        set(p, LEAN, 0.22F, k);
        set(p, WAIST_X, 0.2F, k);
        set(p, HEAD_X, 0.5F, k);
        set(p, NECK_X, 0.3F, k);
        for (int o : new int[]{RA_X, LA_X}) {
            set(p, o + X, -1.0F, k);
            set(p, o + Y, 0.85F, k);
            set(p, o + Z, 0.05F, k);
            set(p, o + ELBOW, -1.7F, k);
            set(p, o + CURL, 0.8F, k);
        }
        for (int o : new int[]{RL_X, LL_X}) {
            set(p, o + X, -0.7F, k);
            set(p, o + KNEE, 1.4F, k);
        }
        set(p, CLOAK_OPEN, -0.4F, k);
        set(p, CRESTS, -0.25F, k);
        set(p, GLOW, 0.2F, k);
        p[LOOK] = 1F - k;
    }

    /**
     * Dying: a convulsion, then he rises with his arms flung out and his head thrown back while
     * the light tears its way out of him, shaking harder and harder.
     */
    private static void dying(float[] p, float t, float age) {
        float spasm = outBack(clamp(t / 5F)) * (1F - smooth((t - 12F) / 30F));
        float k = smooth(t / 180F);
        float shake = sin(age * 3.1F) * (0.02F + 0.06F * k) + spasm * sin(age * 2.3F) * 0.12F;
        p[HEAD_X] = -0.4F * spasm - 0.75F * k + shake;
        p[HEAD_Z] = shake * 1.5F;
        p[NECK_X] = -0.25F * k;
        p[CHEST_X] = -0.3F * k - 0.2F * spasm + shake * 0.5F;
        p[LEAN] = -0.1F * k;
        p[DROP] = -2F * k;
        for (int o : new int[]{RA_X, LA_X}) {
            p[o + X] = -0.9F * spasm - 0.35F * k + shake;
            p[o + Z] = 0.7F * spasm + 1.4F * k + shake;
            p[o + ELBOW] = -0.6F * spasm - 0.1F * k;
            p[o + WRIST] = -0.4F * k;
            p[o + CURL] = 0.9F * spasm + 0.25F * k + Math.abs(shake) * 2F;
            p[o + SPREAD] = 0.6F * k;
        }
        for (int o : new int[]{RL_X, LL_X}) {
            p[o + X] = -0.5F * spasm + 0.05F * k + shake * 0.5F;
            p[o + KNEE] = 0.9F * spasm + 0.08F * k;
            p[o + FOOT] = 0.9F;
        }
        p[CLOAK_OPEN] = 0.4F * spasm + 1F * k;
        p[CLOAK_LIFT] = 0.75F * k;
        p[CRESTS] = 0.6F * spasm + 1.2F * k;
        p[GLOW] = 1F;
    }

    // ------------------------------------------------------------------ attacks

    /**
     * Unmake: his right hand draws back and up, fingers wide, the shoulder turning away; then
     * it comes through all at once, reaching for you, and closes.
     */
    private static void unmake(float[] p, float t, float length, float age, float pitch) {
        hunt(p, age);
        float draw = smooth(t / (length * 0.55F));
        float reach = outBack(clamp((t - length * 0.58F) / 3F));
        float close = smooth((t - length * 0.82F) / 2.5F);
        float tremor = sin(age * 2.9F) * 0.04F * draw * (1F - reach);

        arm(p, RA_X, draw, 0.55F + tremor, -0.1F, 0.65F, -1.25F, -0.4F, 0.4F, 0F, 0.6F);
        set(p, CHEST_Y, 0.45F, draw);
        set(p, LEAN, 0.15F, draw);
        set(p, HEAD_X, -0.3F, draw);

        arm(p, RA_X, reach, -1.45F + pitch, 0.18F, 0.05F, -0.04F, -0.45F, 0.6F, 0F, 0.65F);
        set(p, CHEST_Y, -0.4F, reach);
        set(p, LEAN, 0.42F, reach);
        set(p, HEAD_X, -0.55F + pitch * 0.5F, reach);
        arm(p, LA_X, reach, 0.45F, 0F, 0.45F, -0.3F, 0F, 0.3F, 0.3F, 0.4F);
        set(p, R_CURL, 1.35F, close);
        set(p, R_SPREAD, 0F, close);
        p[CLOAK_OPEN] += 0.3F * reach;
        p[GLOW] = 0.85F + 0.15F * draw;
    }

    /**
     * Singularity: his hands come together before his chest, cupped as if around something
     * small; they ease apart, trembling, as the dark thing between them grows; then both palms
     * thrust out and send it.
     */
    private static void singularity(float[] p, float t, float length, float age, float pitch) {
        drift(p, age);
        float gather = smooth(t / 12F);
        float grow = smooth((t - 12F) / (length - 20F));
        float send = outBack(clamp((t - length + 6F) / 4F));
        float tremor = sin(age * 3.3F) * 0.035F * grow * (1F - send);
        for (int o : new int[]{RA_X, LA_X}) {
            float shake = o == RA_X ? tremor : -tremor;
            arm(p, o, gather, -1.2F + shake, 0.62F, 0F, -0.95F, 0.1F, 0.1F, 0.6F, 0.05F);
            arm(p, o, grow, -1.1F + shake, 0.15F, 0.32F, -0.75F, 0.25F, 0.1F, 0.35F, 0.35F);
            arm(p, o, send, -1.5F + pitch, 0.25F, 0.05F, -0.05F, -0.6F, 0.75F, 0F, 0.5F);
        }
        set(p, LEAN, -0.1F, gather);
        set(p, CHEST_X, -0.08F, gather);
        set(p, HEAD_X, 0.35F, gather);
        set(p, LEAN, 0.2F, send);
        set(p, HEAD_X, -0.1F + pitch * 0.5F, send);
        for (int o : new int[]{RL_X, LL_X}) {
            set(p, o + X, -0.35F, grow);
            set(p, o + KNEE, 0.8F, grow);
        }
        p[CLOAK_OPEN] = 0.15F + 0.45F * grow;
        p[CLOAK_LIFT] = 0.35F * grow;
        p[CRESTS] = 0.65F * grow;
        p[GLOW] = 0.7F + 0.3F * grow;
        p[LOOK] = 1F - 0.6F * gather * (1F - send);
    }

    /**
     * Void lances: his right hand flung up overhead, open, the other held out low to the side,
     * while the lances form in the air; then the raised hand sweeps down to point at you.
     */
    private static void lances(float[] p, float t, float length, float age, float pitch) {
        hunt(p, age);
        float raise = smooth(t / (length * 0.45F));
        float cast = outBack(clamp((t - length * 0.62F) / 4F));
        arm(p, RA_X, raise, -2.75F, 0.05F, 0.35F, -0.25F, -0.35F, 0.2F, 0.05F, 0.6F);
        arm(p, LA_X, raise, -0.25F, 0F, 0.75F, -0.3F, -0.3F, 0.3F, 0.1F, 0.45F);
        set(p, CHEST_X, -0.12F, raise);
        set(p, LEAN, 0.05F, raise);
        set(p, HEAD_X, -0.35F, raise);
        set(p, CHEST_Y, 0.2F, raise);
        arm(p, RA_X, cast, -1.4F + pitch, 0.1F, 0.08F, -0.05F, -0.45F, 0.5F, 0F, 0.6F);
        set(p, CHEST_Y, -0.35F, cast);
        set(p, LEAN, 0.28F, cast);
        set(p, HEAD_X, -0.4F + pitch * 0.5F, cast);
        p[CRESTS] = 0.3F + 0.4F * raise;
        p[CLOAK_OPEN] = 0.35F + 0.3F * Math.max(raise * (1F - cast), cast);
        p[GLOW] = 0.85F + 0.15F * raise;
    }

    /**
     * Starfall: both arms rise slowly, wide, to the sky, his head going back and his body
     * arching, lifting off as his cloak spreads; then they sweep down and out to call the
     * stars down after them.
     */
    private static void starfall(float[] p, float t, float length, float age) {
        drift(p, age);
        float rise = smooth(t / (length * 0.62F));
        float call = smooth((t - length * 0.72F) / (length * 0.22F));
        for (int o : new int[]{RA_X, LA_X}) {
            arm(p, o, rise, -2.55F, 0F, 0.6F, -0.18F, -0.4F, 0.3F, 0.05F, 0.6F);
            arm(p, o, call, -0.75F, 0F, 1.2F, -0.12F, -0.35F, 0.3F, 0.05F, 0.65F);
        }
        set(p, HEAD_X, -0.6F, rise);
        set(p, NECK_X, -0.15F, rise);
        set(p, CHEST_X, -0.25F, rise);
        set(p, LEAN, -0.1F, rise);
        p[DROP] -= 2F * rise;
        for (int o : new int[]{RL_X, LL_X}) {
            set(p, o + X, 0.05F, rise);
            set(p, o + KNEE, 0.1F, rise);
            set(p, o + FOOT, 0.85F, rise);
        }
        set(p, HEAD_X, 0.15F, call);
        set(p, CHEST_X, 0.05F, call);
        set(p, LEAN, 0.12F, call);
        p[CLOAK_OPEN] = 0.15F + 0.85F * rise;
        p[CLOAK_LIFT] = 0.55F * rise * (1F - 0.5F * call);
        p[CRESTS] = rise;
        p[GLOW] = 0.7F + 0.3F * rise;
        p[LOOK] = 1F - rise * (1F - call);
    }

    /**
     * Shatter: rising with both fists locked together high over his head, knees drawn up;
     * then he brings them down with his whole body behind them, and the cloak flies up.
     */
    private static void shatter(float[] p, float t, float length, float age) {
        drift(p, age);
        float raise = smooth(t / (length * 0.47F));
        float smash = outCubic(clamp((t - length * 0.57F) / 3F));
        float tremor = sin(age * 3.1F) * 0.03F * raise * (1F - smash);
        for (int o : new int[]{RA_X, LA_X}) {
            arm(p, o, raise, -2.85F + tremor, 0.38F, 0.05F, -0.55F, 0.2F, 0F, 1.35F, 0F);
            arm(p, o, smash, -0.5F, 0.3F, 0.08F, -0.1F, 0.3F, 0F, 1.35F, 0F);
        }
        set(p, LEAN, -0.15F, raise);
        set(p, HEAD_X, -0.25F, raise);
        set(p, CHEST_X, -0.1F, raise);
        p[DROP] -= 3F * raise;
        leg(p, RL_X, raise, -0.75F, 0F, 0.05F, 1.25F, 0.7F);
        leg(p, LL_X, raise, -0.45F, 0F, 0.05F, 1.05F, 0.7F);
        set(p, LEAN, 0.45F, smash);
        set(p, WAIST_X, 0.25F, smash);
        set(p, CHEST_X, 0.2F, smash);
        set(p, HEAD_X, 0.15F, smash);
        p[DROP] += 5F * smash;
        leg(p, RL_X, smash, -0.35F, 0F, 0.08F, 0.9F, 0.7F);
        leg(p, LL_X, smash, 0.25F, 0F, 0.08F, 0.7F, 0.75F);
        p[CLOAK_LIFT] = 0.85F * smash * (1F - smooth((t - length * 0.75F) / 8F));
        p[CLOAK_OPEN] = 0.2F + 0.5F * Math.max(raise, smash);
        p[CRESTS] = 0.4F + 0.4F * raise;
        p[GLOW] = 0.8F + 0.2F * Math.max(raise, smash);
    }

    // ------------------------------------------------------------------ helpers

    /** Sets a whole arm. */
    private static void arm(float[] p, int o, float x, float y, float z, float elbow, float wrist, float twist, float curl, float spread) {
        p[o + X] = x;
        p[o + Y] = y;
        p[o + Z] = z;
        p[o + ELBOW] = elbow;
        p[o + WRIST] = wrist;
        p[o + TWIST_] = twist;
        p[o + CURL] = curl;
        p[o + SPREAD] = spread;
    }

    /** Eases a whole arm toward a gesture by f. */
    private static void arm(float[] p, int o, float f, float x, float y, float z, float elbow, float wrist, float twist, float curl, float spread) {
        if (f <= 0F) {
            return;
        }
        set(p, o + X, x, f);
        set(p, o + Y, y, f);
        set(p, o + Z, z, f);
        set(p, o + ELBOW, elbow, f);
        set(p, o + WRIST, wrist, f);
        set(p, o + TWIST_, twist, f);
        set(p, o + CURL, curl, f);
        set(p, o + SPREAD, spread, f);
    }

    private static void leg(float[] p, int o, float x, float y, float z, float knee, float foot) {
        p[o + X] = x;
        p[o + Y] = y;
        p[o + Z] = z;
        p[o + KNEE] = knee;
        p[o + FOOT] = foot;
    }

    private static void leg(float[] p, int o, float f, float x, float y, float z, float knee, float foot) {
        if (f <= 0F) {
            return;
        }
        set(p, o + X, x, f);
        set(p, o + Y, y, f);
        set(p, o + Z, z, f);
        set(p, o + KNEE, knee, f);
        set(p, o + FOOT, foot, f);
    }

    private static void set(float[] p, int i, float value, float f) {
        p[i] += (value - p[i]) * f;
    }

    private static float sin(float x) {
        return (float) Math.sin(x);
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
