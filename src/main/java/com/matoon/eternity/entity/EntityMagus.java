package com.matoon.eternity.entity;

import com.matoon.eternity.EternitySounds;
import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.network.MsgTribunalFx;
import com.matoon.eternity.story.StoryDirector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Rotations;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The Magus: what Adam Warlock would have grown into, stepped out of a future that will now
 * never have to happen. Cut from the same cloth as Warlock (the same build, the same way of
 * hanging in the air) and wrong in every particular: lavender where Warlock is gold, white
 * hair, blank white eyes, violet and silver. He does not walk if he can help it.
 * <p>
 * Everything he does with his body is a {@link Pose}, synced with a count so the same pose
 * can be struck twice running; each client builds the pose out of it and how long it has
 * been held, blending out of the last. Scenes move him (hoverTo, place, perform, blink); in
 * the Grandmaster's game fight() turns him loose on someone, with Warlock as a second, and the
 * story watches his patience and calls it off when it is spent. He goes from one place to
 * another, when he cannot be bothered to cross the space between, by folding himself into a
 * line of light and unfolding somewhere else.
 */
public class EntityMagus extends EntityLiving {

    public enum Pose {
        /** In the air at the height of a pulpit, at ease and entirely sure of himself. */
        FLOAT,
        /** On his feet, fists on his hips, chin up. */
        STAND,
        /** Hands clasped behind his back, looking down at someone smaller. */
        REGARD,
        /** One arm straight out at whatever he means. */
        POINT,
        /** An open hand raised at someone, and what he holds closing it on them, slowly; then the fist. */
        CRUSH,
        /** In the air, on guard: one open hand forward, the other drawn back. */
        GUARD,
        /** One palm up against what is coming at him, holding it off; struggling with it, then not. */
        BLOCK,
        /** Struck, thrown back, and righting himself at once. */
        STAGGER,
        /** Launched: arched back, arms and legs trailing. */
        FLUNG,
        /** A fist caught in his open hand, and held there. */
        CATCH,
        /** A palm driven into someone's chest. */
        PALM,
        /** A backhand, from across the body out. */
        BACKHAND,
        /** Both hands pulling someone down onto a rising knee. */
        KNEE,
        /** Both fists overhead, then down. */
        HAMMER,
        /** Both hands drawn back to the hip, then thrown forward open: what he holds, let go. */
        BLAST,
        /** One hand up at the height of someone's throat, closed on it: holding them off the ground. */
        GRIP,
        /** Still gripping: the other hand reaches into them, closes on something, and brings it to his brow. */
        TAKE,
        /** Folding into a line of light, slowly, for anyone watching. */
        VANISH,
        /** Unfolding out of one. */
        APPEAR,
        /** As VANISH, in an eyeblink, mid-fight. */
        BLINK_OUT,
        /** As APPEAR, in an eyeblink. */
        BLINK_IN,
        /** Coming down out of the air, arms a little out, palms down, unhurried. */
        DESCEND,
        /** Asleep standing in a capsule: head bowed, arms down. */
        SLEEP,
        /** Asleep, and then not quite: the head comes up and he looks at whoever is looking. */
        STIR,
        /** Bent over a piece of the Grandmaster's game, both hands spread under him, holding it up in his light. */
        CARRY,
        /** Flying with the gauntlet held to his chest in both arms, curled round it, as if anyone could take it. */
        CRADLE,
        /** Landed: the gauntlet held out in his right hand, a stone in his left, and the stone drawn into its socket. */
        SLOT,
        /** Holding the gauntlet out in his left hand and driving his right hand into it; then the fist, turned, looked at. */
        DON,
        /** The fist held before his chest, the other hand on the wrist, braced, while everything in it comes up through him. */
        POWER,
        /** The arm up, high; and the fingers. */
        SNAP,
        /** Both hands on the gauntlet, pulling, against someone pulling the other way. */
        STRUGGLE,
        /** After his snap: the arm that made it hanging, burnt, shaking; bent over it, breathing hard. */
        SPENT;

        /** How long the gesture lasts, for act(pose, 0). */
        public int ticks() {
            switch (this) {
                case PALM:
                case BACKHAND:
                case KNEE:
                    return 18;
                case HAMMER:
                    return 24;
                case BLAST:
                    return 26;
                case STAGGER:
                    return 20;
                case CRUSH:
                    return CRUSH_TICKS;
                case TAKE:
                    return 50;
                case VANISH:
                    return VANISH_TICKS;
                case APPEAR:
                    return APPEAR_TICKS;
                case BLINK_OUT:
                    return BLINK_OUT_TICKS;
                case BLINK_IN:
                    return BLINK_IN_TICKS;
                case STIR:
                    return 60;
                case SLOT:
                    return SLOT_TICKS;
                case DON:
                    return DON_TICKS;
                case POWER:
                    return POWER_TICKS;
                case SNAP:
                    return SNAP_TICKS;
                default:
                    return 30;
            }
        }

        /** Whether he is out of the world (folded away) once this has run its course. */
        public boolean leaves() {
            return this == VANISH || this == BLINK_OUT;
        }
    }

    public static final int VANISH_TICKS = 16, APPEAR_TICKS = 18, BLINK_OUT_TICKS = 4, BLINK_IN_TICKS = 6;
    /** Ticks into each blow at which it lands. */
    public static final int PALM_HIT = 5, BACKHAND_HIT = 7, KNEE_HIT = 6, HAMMER_HIT = 10, BLAST_HIT = 12;
    /** When the open hand, raised at someone, closes, and how long the whole of it takes. */
    public static final int CRUSH_CLOSE = 56, CRUSH_TICKS = 90;
    public static final int TRAIL = 8;
    /** Of slotting a stone: when it leaves his hand, and when it is in. */
    public static final int SLOT_TICKS = 44, SLOT_GO = 16, SLOT_IN = 30;
    /** Of putting it on: when his hand is in it. */
    public static final int DON_TICKS = 40, DON_IN = 18;
    /** The power coming up through him, as long as it takes the gauntlet's own: twenty seconds. */
    public static final int POWER_TICKS = com.matoon.eternity.stones.network.MsgPowerUp.DURATION;
    /** Of the snap: when the fingers go. */
    public static final int SNAP_TICKS = 40, SNAP_AT = 26;

    /** How he holds the gauntlet, if he does. */
    public static final int HOLD_NONE = 0, HOLD_CRADLE = 1, HOLD_HAND = 2, HOLD_WORN = 3;

    private static final DataParameter<Integer> POSE = EntityDataManager.createKey(EntityMagus.class, DataSerializers.VARINT);
    // Three floats, not Rotations: Rotations wraps every value modulo 360, which turned a point far from the origin into nonsense.
    private static final DataParameter<Float> FOCUS_X = EntityDataManager.createKey(EntityMagus.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> FOCUS_Y = EntityDataManager.createKey(EntityMagus.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> FOCUS_Z = EntityDataManager.createKey(EntityMagus.class, DataSerializers.FLOAT);
    private static final DataParameter<Boolean> CROWNED = EntityDataManager.createKey(EntityMagus.class, DataSerializers.BOOLEAN);
    /** The Infinity Gauntlet, if he has it, as everyone sees it: which stones are in it. */
    private static final DataParameter<ItemStack> GAUNTLET = EntityDataManager.createKey(EntityMagus.class, DataSerializers.ITEM_STACK);
    /** How he holds it (low byte), and the stone in his other hand while he slots it (the next, its ordinal + 1). */
    private static final DataParameter<Integer> HOLD = EntityDataManager.createKey(EntityMagus.class, DataSerializers.VARINT);
    private static final DataParameter<Rotations> FACING = EntityDataManager.createKey(EntityMagus.class, DataSerializers.ROTATIONS);
    /** Where he is, for the client (see syncSpot): three floats, not Rotations, which wraps every value to within 360. */
    private static final DataParameter<Float> SPOT_X = EntityDataManager.createKey(EntityMagus.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> SPOT_Y = EntityDataManager.createKey(EntityMagus.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> SPOT_Z = EntityDataManager.createKey(EntityMagus.class, DataSerializers.FLOAT);
    /** How badly his snap has burnt his right arm, 0 to 1 (see SnapScars: the same burn a player's arm takes). */
    private static final DataParameter<Float> BURN = EntityDataManager.createKey(EntityMagus.class, DataSerializers.FLOAT);

    private enum Attack {
        /** Simply in front of them: a palm, a backhand, and a knee that throws them. */
        COMBO,
        /** Shards of thought hung round his shoulders, aimed, then loosed one after another. */
        SHARDS,
        /** A sphere of the Power Stone's light gathered in both hands and thrown, slow and heavy. */
        ORB,
        /** Gone; a mark on the ground under them; and down on it out of the sky. */
        METEOR,
        /** His hand raised, and them in it, lifted off their feet; then the fist, and they are thrown down. */
        GRASP
    }

    /** A shard or a sphere of his in the air: hung until it is loosed, then flying straight. */
    private static final class Shot {
        final int id;
        final boolean orb;
        final int looseAt;
        @Nullable
        final EntityLivingBase at;
        Vec3d pos;
        Vec3d vel = Vec3d.ZERO;
        int age;

        Shot(int id, boolean orb, Vec3d pos, int looseAt, @Nullable EntityLivingBase at) {
            this.id = id;
            this.orb = orb;
            this.pos = pos;
            this.looseAt = looseAt;
            this.at = at;
        }
    }

    /** Of the meteor: when he is above them, when the fists go up, and when he lands. */
    private static final int METEOR_OVER = 5, METEOR_RAISE = 14, METEOR_LAND = METEOR_RAISE + 10;
    /** Of the grasp: when the hand closes on them (if it can see them), and when the fist closes. */
    private static final int GRASP_CATCH = 12, GRASP_FROM = 26, GRASP_CRUSH = CRUSH_CLOSE - GRASP_FROM + 1;

    // ------------------------------------------------------------------ server state
    private Pose rest = Pose.FLOAT;
    private int poseTicks;
    private int holdFor = -1;
    @Nullable
    private Entity watching;
    /** Turned bodily by the scene each tick, not by what he is looking at. */
    private boolean facingLocked;
    @Nullable
    private Vec3d hoverAt;
    private double rate = 0.1D;
    /** Asleep in one of the gallery's capsules; a hand on him is a hand on its glass. */
    public boolean sleeper;
    private boolean fighting;
    @Nullable
    private UUID targetId;
    /** Who he is after when it is not a player (hunt): only them. */
    @Nullable
    private EntityLivingBase prey;
    @Nullable
    private EntityLivingBase foe;
    /** Odin, if he has come into it: another he fights, and who fights him. */
    @Nullable
    private EntityOdin rival;
    @Nullable
    private Attack attack;
    @Nullable
    private Attack lastAttack;
    private int attackTicks;
    private int nextAttack = 50;
    private int hits;
    private int fightTicks;
    private float circle;
    /** Who the attack under way is at: whoever he is after, or now and then Warlock. */
    @Nullable
    private EntityLivingBase aimed;
    /** Attacks he has finished since the fight began; and a heavy blow of his has landed on whoever he is after, unasked about. */
    private int attacksDone;
    private boolean opening;
    private final List<Shot> shots = new ArrayList<>();
    private int nextShot;
    /** Of the meteor: where he comes down, and where he comes down from. */
    private Vec3d markAt = Vec3d.ZERO, dropFrom = Vec3d.ZERO;
    /** Of the grasp: who is in his hand, where they were taken from, and when he can do it again. */
    @Nullable
    private EntityLivingBase held;
    private Vec3d heldFrom = Vec3d.ZERO;
    private int graspCooldown = 200;
    /**
     * Who has been hurting him lately, by entity id, cooling off over a few seconds: whoever he is after is who he goes
     * at most, but anyone else who keeps at him, Warlock most of all, draws him off now and then.
     */
    private final java.util.Map<Integer, Float> threat = new java.util.HashMap<>();

    /** In the fight for the gauntlet: a boss bar, real health, and blows that tell. */
    private boolean bossFight;
    /** Under the story's hand in the middle of it (a struggle, a clash, a death): nothing touches him. */
    private boolean guarded;
    /** His health run out: he reels, until whoever is directing him has seen to it; and how many times it has. */
    private boolean broken;
    private int breaks;
    private final net.minecraft.world.BossInfoServer bossInfo = (net.minecraft.world.BossInfoServer) new net.minecraft.world.BossInfoServer(
            new net.minecraft.util.text.TextComponentString("The Magus"), net.minecraft.world.BossInfo.Color.PURPLE,
            net.minecraft.world.BossInfo.Overlay.NOTCHED_20).setDarkenSky(true);

    // ------------------------------------------------------------------ client state
    /** Client: ticks since the synced pose last changed. */
    public int clientPoseTicks;
    /** Client: the pose he was drawn in when it changed, which the model blends out of; and the pose last drawn. */
    public final float[] poseFrom = new float[32], poseLast = new float[32];
    /** Client: the pose before this one. */
    public Pose previousPose = Pose.FLOAT;
    private Pose seenPose = Pose.FLOAT;
    public double velX, velY, velZ;
    public float bodyPitch, prevBodyPitch, bodyRoll, prevBodyRoll;
    /** Client: how hard what he holds is burning in him, 0 to a little over 1. */
    public float power, prevPower;
    /** Client: where he has been, newest first: x, y, z, yaw, pitch, roll, heat. */
    public final double[][] trail = new double[TRAIL][7];
    public int trailCount;
    /** Client: when his arm was burnt, for the flare. */
    public int burntAt = -1000;

    public EntityMagus(World world) {
        super(world);
        setSize(0.7F, 2.1F);
        setNoGravity(true);
        noClip = true;
        isImmuneToFire = true;
        experienceValue = 0;
        ignoreFrustumCheck = true;
        bossInfo.setVisible(false);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(POSE, Pose.FLOAT.ordinal());
        dataManager.register(FOCUS_X, 0F);
        dataManager.register(FOCUS_Y, -1000F);
        dataManager.register(FOCUS_Z, 0F);
        dataManager.register(CROWNED, false);
        dataManager.register(GAUNTLET, ItemStack.EMPTY);
        dataManager.register(HOLD, HOLD_NONE);
        dataManager.register(FACING, new Rotations(0F, 0F, 0F));
        dataManager.register(SPOT_X, 0F);
        dataManager.register(SPOT_Y, -1000F);
        dataManager.register(SPOT_Z, 0F);
        dataManager.register(BURN, 0F);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(1000D);
        getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(1D);
    }

    @Override
    public void notifyDataManagerChange(DataParameter<?> key) {
        super.notifyDataManagerChange(key);
        if (POSE.equals(key) && world.isRemote) {
            // Picked up where it was left off, if it was: the tick it starts from rides with it.
            clientPoseTicks = dataManager.get(POSE) >>> 16;
            System.arraycopy(poseLast, 0, poseFrom, 0, poseLast.length);
            previousPose = seenPose;
            seenPose = pose();
        }
        if (BURN.equals(key) && world.isRemote && burn() > 0F) {
            // Fresh: it flares white-hot, and cools.
            burntAt = ticksExisted;
        }
    }

    // ------------------------------------------------------------------ accessors

    public Pose pose() {
        int ordinal = dataManager.get(POSE) & 0xFF;
        return ordinal < Pose.values().length ? Pose.values()[ordinal] : Pose.FLOAT;
    }

    /** Whether he wears the Power Stone in his brow: the gem there burns. */
    public boolean isCrowned() {
        return dataManager.get(CROWNED);
    }

    public void crown(boolean crowned) {
        dataManager.set(CROWNED, crowned);
    }

    /** How badly his snap has burnt his right arm, 0 to 1. */
    public float burn() {
        return dataManager.get(BURN);
    }

    public void burn(float burn) {
        dataManager.set(BURN, burn);
    }

    /** The gauntlet he has, or empty. */
    public ItemStack gauntlet() {
        return dataManager.get(GAUNTLET);
    }

    /** How he holds it: HOLD_NONE, HOLD_CRADLE, HOLD_HAND or HOLD_WORN. */
    public int hold() {
        return dataManager.get(HOLD) & 0xFF;
    }

    /** The stone in his other hand while he slots it, or -1. */
    public int slotting() {
        return (dataManager.get(HOLD) >> 8) - 1;
    }

    /** Gives him the gauntlet (or takes it, with EMPTY), held so. */
    public void holdGauntlet(ItemStack gauntlet, int how) {
        if (!ItemStack.areItemStacksEqual(dataManager.get(GAUNTLET), gauntlet)) {
            dataManager.set(GAUNTLET, gauntlet.copy());
        }
        int hold = gauntlet.isEmpty() ? HOLD_NONE : how;
        if (hold() != hold) {
            dataManager.set(HOLD, hold | dataManager.get(HOLD) & ~0xFF);
        }
    }

    /** Holds it differently; with a stone in his other hand (its ordinal), or none (-1). */
    public void holdAs(int how, int stone) {
        dataManager.set(HOLD, how | (stone + 1) << 8);
    }

    /** Where the gauntlet is, near enough, for effects and for whoever reaches for it. */
    public Vec3d gauntletAt() {
        float yaw = (float) Math.toRadians(rotationYaw);
        Vec3d forward = new Vec3d(-MathHelper.sin(yaw), 0D, MathHelper.cos(yaw));
        Vec3d right = new Vec3d(-forward.z, 0D, forward.x);
        switch (hold()) {
            case HOLD_CRADLE:
                return getPositionVector().add(0D, 1.35D, 0D).add(forward.scale(0.45D));
            case HOLD_WORN:
                if (pose() == Pose.SNAP) {
                    return getPositionVector().add(0D, 2.7D, 0D).add(right.scale(0.35D));
                }
                return getPositionVector().add(0D, 1.45D, 0D).add(forward.scale(0.6D)).add(right.scale(0.05D));
            default:
                return getPositionVector().add(0D, 1.3D, 0D).add(forward.scale(0.65D)).add(right.scale(0.3D));
        }
    }

    /** For whoever is directing him: three shards at someone, hung behind him as he goes, then loosed. */
    public void fire(EntityLivingBase at) {
        hangShards(at, 3, 8);
    }

    @Nullable
    public Vec3d focus() {
        float y = dataManager.get(FOCUS_Y);
        return y < -999F ? null : new Vec3d(dataManager.get(FOCUS_X), y, dataManager.get(FOCUS_Z));
    }

    private void setFocusPoint(@Nullable Vec3d at) {
        dataManager.set(FOCUS_X, at == null ? 0F : (float) at.x);
        dataManager.set(FOCUS_Y, at == null ? -1000F : (float) at.y);
        dataManager.set(FOCUS_Z, at == null ? 0F : (float) at.z);
    }


    /** Whether he is folded away right now, nowhere to be seen. */
    public boolean isAway() {
        Pose p = pose();
        int t = world.isRemote ? clientPoseTicks : poseTicks;
        return p.leaves() && t >= p.ticks();
    }

    /** How far through the pose he is, on whichever side is asking. */
    public int poseTicks() {
        return world.isRemote ? clientPoseTicks : poseTicks;
    }

    // ------------------------------------------------------------------ the scene's handles

    /** Strikes a pose now, from its start, even the one he is already in, and holds it. */
    public EntityMagus perform(Pose pose) {
        return perform(pose, -1);
    }

    /** Strikes a pose for so many ticks (its own length, if 0), then goes back to resting. */
    public EntityMagus perform(Pose pose, int ticks) {
        poseTicks = 0;
        holdFor = ticks == 0 ? pose.ticks() : ticks;
        int seq = (dataManager.get(POSE) >>> 8 & 0xFF) + 1 & 0xFF;
        dataManager.set(POSE, seq << 8 | pose.ordinal());
        return this;
    }

    /** Takes a pose up again part way through, from tick {@code from}, held until told otherwise. */
    public EntityMagus resume(Pose pose, int from) {
        perform(pose, -1);
        poseTicks = Math.max(0, from);
        dataManager.set(POSE, Math.min(0xFFFF, Math.max(0, from)) << 16 | dataManager.get(POSE) & 0xFFFF);
        return this;
    }

    /** What he goes back to after a gesture. */
    public EntityMagus rest(Pose pose) {
        rest = pose;
        return this;
    }

    /** Folds away here and unfolds there, facing what he means to face, a beat later. */
    public void blink(Vec3d to, @Nullable Vec3d face, boolean slow) {
        WorldServer ws = (WorldServer) world;
        Vec3d from = getPositionVector().add(0D, 1.1D, 0D);
        ws.spawnParticle(EnumParticleTypes.END_ROD, from.x, from.y, from.z, slow ? 30 : 12, 0.1D, 1D, 0.1D, 0.05D);
        sound(SoundEvents.ENTITY_ENDERMEN_TELEPORT, slow ? 2F : 1.5F, slow ? 0.5F : 0.8F);
        hoverAt = null;
        setPosition(to.x, to.y, to.z);
        if (face != null) {
            faceNow(face);
        }
        perform(slow ? Pose.APPEAR : Pose.BLINK_IN, 0);
        sound(com.matoon.eternity.EternitySounds.RIFT_IMPACT, slow ? 1.5F : 0.8F, slow ? 1.4F : 1.9F);
    }

    // The same handles the gallery's people answer to, for the scenes written against them.

    public EntityMagus act(NorsePose pose) {
        return perform(of(pose), -1);
    }

    public EntityMagus act(NorsePose pose, int ticks) {
        return perform(of(pose), ticks);
    }

    public EntityMagus rest(NorsePose pose) {
        return rest(of(pose));
    }

    private static Pose of(NorsePose pose) {
        switch (pose) {
            case STAND:
                return Pose.STAND;
            case REGARD:
                return Pose.REGARD;
            case POINT:
                return Pose.POINT;
            case BLAST:
                return Pose.BLAST;
            case GRIP:
                return Pose.GRIP;
            case TAKE:
                return Pose.TAKE;
            case SLEEP:
                return Pose.SLEEP;
            case STIR:
                return Pose.STIR;
            default:
                return Pose.FLOAT;
        }
    }

    /** Turns him to look at a point, and keeps him looking. */
    public EntityMagus look(@Nullable Vec3d at) {
        watching = null;
        facingLocked = false;
        setFocusPoint(at);
        return this;
    }

    /** Keeps him looking at someone, wherever they go. */
    public EntityMagus watch(@Nullable Entity who) {
        watching = who;
        facingLocked = false;
        if (who == null) {
            look(null);
        }
        return this;
    }

    /** Where he goes now, and how fast he closes on it (a share of the distance each tick). */
    public EntityMagus hoverTo(@Nullable Vec3d at, double rate) {
        hoverAt = at;
        this.rate = rate;
        return this;
    }

    /** Put exactly here this tick, facing exactly there, by a scene that is moving him itself. */
    public void place(Vec3d at, @Nullable Vec3d face) {
        hoverAt = null;
        setPosition(at.x, at.y, at.z);
        if (face != null) {
            faceNow(face);
        }
    }

    /** Turns him at once to face a point, and holds him so until he is told to look at something. */
    public void faceNow(Vec3d at) {
        facingLocked = true;
        double dx = at.x - posX, dz = at.z - posZ, dy = at.y - (posY + getEyeHeight());
        rotationYaw = rotationYawHead = renderYawOffset = (float) Math.toDegrees(Math.atan2(-dx, dz));
        rotationPitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
    }

    /** Where his gripping hand is, held up in front of him: what he holds by the throat hangs from here. */
    public Vec3d gripHand() {
        float yaw = (float) Math.toRadians(rotationYaw);
        Vec3d forward = new Vec3d(-MathHelper.sin(yaw), 0D, MathHelper.cos(yaw));
        Vec3d right = new Vec3d(-forward.z, 0D, forward.x);
        return getPositionVector().add(0D, 2.06D, 0D).add(forward.scale(0.55D)).add(right.scale(0.3D));
    }

    /** The gem at his brow. */
    public Vec3d brow() {
        float yaw = (float) Math.toRadians(rotationYaw);
        return getPositionVector().add(-MathHelper.sin(yaw) * 0.3D, 2.0D, MathHelper.cos(yaw) * 0.3D);
    }

    /** His open right hand, raised at whatever he is pointing at. */
    public Vec3d palm() {
        float yaw = (float) Math.toRadians(rotationYaw);
        Vec3d forward = new Vec3d(-MathHelper.sin(yaw), 0D, MathHelper.cos(yaw));
        Vec3d right = new Vec3d(-forward.z, 0D, forward.x);
        return getPositionVector().add(0D, 1.52D, 0D).add(forward.scale(0.8D)).add(right.scale(0.37D));
    }

    // ------------------------------------------------------------------ the Grandmaster's game: loose on someone

    /**
     * At someone who is not a player (the Grandmaster, on his own board): every bolt and blast is
     * at him, and nobody else near is ever touched by any of it.
     */
    public void hunt(EntityLivingBase prey) {
        fighting = true;
        this.prey = prey;
        targetId = null;
        foe = null;
        hits = 0;
        fightTicks = 0;
        nextAttack = 30;
        attack = null;
        held = null;
        rest(Pose.GUARD);
        perform(Pose.GUARD);
    }

    /** At someone, for real, with whoever else stands with them as a second target. */
    public void fight(EntityPlayer target, @Nullable EntityLivingBase second) {
        prey = null;
        fighting = true;
        targetId = target.getUniqueID();
        foe = second;
        hits = 0;
        fightTicks = 0;
        nextAttack = 40;
        attack = null;
        held = null;
        rest(Pose.GUARD);
        perform(Pose.GUARD);
    }

    /** Odin in the fight with him (or not, with null): he strikes at him too, and is struck. */
    public void rival(@Nullable EntityOdin odin) {
        rival = odin;
    }

    /** Out of the fight and back under the story's hand, wherever he is. What he has already thrown flies on. */
    public void stand() {
        fighting = false;
        prey = null;
        attack = null;
        held = null;
        rest(Pose.FLOAT);
        hoverTo(getPositionVector(), 0.1D);
    }

    public boolean isFighting() {
        return fighting;
    }

    // ------------------------------------------------------------------ the fight for the gauntlet

    /**
     * Into the fight for the gauntlet, or out of it: his boss bar (above Warlock's, whose is put up again under it),
     * his health whole, and every blow on him counting.
     */
    public void boss(boolean on) {
        if (bossFight == on) {
            return;
        }
        bossFight = on;
        if (on) {
            getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(com.matoon.eternity.EternityConfig.magus.health);
            setHealth(getMaxHealth());
            broken = false;
            breaks = 0;
            threat.clear();
        }
        bossInfo.setPercent(getHealth() / getMaxHealth());
        bossInfo.setVisible(on);
        if (on) {
            for (EntityPlayerMP player : bossInfo.getPlayers()) {
                underHim(player);
            }
        }
    }

    public boolean isBoss() {
        return bossFight;
    }

    /** Nothing touches him while the story has him (a struggle, a clash, a death), or not. */
    public void guard(boolean on) {
        guarded = on;
    }

    /** Whether his health has run out since this was last asked: he reels, and drops whatever he holds. */
    public boolean takeBroken() {
        boolean was = broken;
        broken = false;
        return was;
    }

    /** How many times his health has run out in this fight. */
    public int breaks() {
        return breaks;
    }

    /** Back up, some of the way, after reeling. */
    public void recover(float share) {
        setHealth(Math.max(getHealth(), getMaxHealth() * share));
    }

    /** Everything of his in the air, gone: nothing he threw before a pause lands after it. */
    public void clearShots() {
        for (Shot shot : shots) {
            fxAt(shot.orb ? MsgTribunalFx.MAGUS_NOVA : MsgTribunalFx.MAGUS_SHATTER, shot.pos, shot.orb ? 1.5D : 0.6D, 0D, 0D, shot.id, 0);
        }
        shots.clear();
    }

    /** Warlock's bar, wherever he is, put up again under this one for someone. */
    private void underHim(EntityPlayerMP player) {
        for (EntityAdamWarlock w : world.getEntities(EntityAdamWarlock.class, e -> e != null && !e.isDead)) {
            w.rebar(player);
        }
    }

    @Override
    public void addTrackingPlayer(EntityPlayerMP player) {
        super.addTrackingPlayer(player);
        bossInfo.addPlayer(player);
        if (bossFight) {
            underHim(player);
        }
    }

    @Override
    public void removeTrackingPlayer(EntityPlayerMP player) {
        super.removeTrackingPlayer(player);
        bossInfo.removePlayer(player);
    }

    @Override
    public void setDead() {
        super.setDead();
        bossInfo.setVisible(false);
    }

    /** For testing: one attack (COMBO, SHARDS, ORB, METEOR, GRASP), now, at whoever he is after. */
    public String attackNow(String name) {
        EntityLivingBase t = target();
        if (!fighting || t == null) {
            return "not fighting";
        }
        release();
        aimed = t;
        attack = Attack.valueOf(name.toUpperCase(java.util.Locale.ROOT));
        attackTicks = 0;
        graspCooldown = 0;
        return attack.name();
    }

    /** In the middle of an attack, not between them. */
    public boolean isAttacking() {
        return fighting && attack != null;
    }

    /** How many attacks he has finished since the fight began. */
    public int attacksDone() {
        return attacksDone;
    }

    /** Whether a heavy blow of his has landed on whoever he is after since this was last asked: they are open. */
    public boolean opening() {
        boolean was = opening;
        opening = false;
        return was;
    }

    /** How many blows have landed on him since the fight began. */
    public int hits() {
        return hits;
    }

    public int fightTicks() {
        return fightTicks;
    }

    /** How far through his patience he is: 0 at the start, one more for every eight blows. */
    public int phase() {
        if (bossFight) {
            // Worse the more he is hurt, and worse each time he has been put on his knees.
            return Math.min(3, (int) ((1F - getHealth() / getMaxHealth()) * 3F) + breaks);
        }
        return Math.min(3, hits / 8);
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void onLivingUpdate() {
        motionX = motionY = motionZ = 0D;
        fallDistance = 0F;
        noClip = true;
        super.onLivingUpdate();
        if (world.isRemote) {
            clientTick();
            return;
        }
        getNavigator().clearPath();
        poseTicks++;
        if (holdFor > 0 && poseTicks >= holdFor && !pose().leaves()) {
            holdFor = -1;
            Pose back = pose() == Pose.BLINK_IN || pose() == Pose.APPEAR ? (fighting ? Pose.GUARD : rest) : rest;
            if (pose() != back) {
                perform(back);
            }
        }
        if (graspCooldown > 0) {
            graspCooldown--;
        }
        if (!threat.isEmpty()) {
            threat.replaceAll((id, t) -> t * 0.996F);
            threat.values().removeIf(t -> t < 0.05F);
        }
        if (bossFight && ticksExisted % 5 == 0) {
            bossInfo.setPercent(getHealth() / getMaxHealth());
        }
        if (fighting) {
            fightTicks++;
            fightTick();
        }
        tickShots();
        if (hoverAt != null) {
            Vec3d to = hoverAt.subtract(getPositionVector());
            double d = to.length();
            if (d > 0.02D) {
                Vec3d step = to.scale(Math.min(1D, rate));
                if (step.length() > 1.4D) {
                    step = step.normalize().scale(1.4D);
                }
                setPosition(posX + step.x, posY + step.y, posZ + step.z);
            }
        }
        if (watching != null) {
            if (watching.isDead) {
                watching = null;
            } else {
                Vec3d eye = watching.getPositionEyes(1F);
                Vec3d was = focus();
                if (was == null || was.distanceTo(eye) > 0.2D) {
                    setFocusPoint(eye);
                }
            }
        }
        Vec3d focus = focus();
        if (focus != null && !facingLocked) {
            double dx = focus.x - posX, dz = focus.z - posZ;
            float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            rotationYawHead = approach(rotationYawHead, yaw, 14F);
            if (pose() != Pose.SLEEP && pose() != Pose.STIR) {
                // Turned a long way, he turns at once; a little way, the body comes round after the head, unhurried.
                boolean far = Math.abs(MathHelper.wrapDegrees(yaw - renderYawOffset)) > 35F || pose() != Pose.FLOAT && pose() != Pose.STAND;
                renderYawOffset = approach(renderYawOffset, yaw, far ? 7F : 1.5F);
                rotationYaw = renderYawOffset;
            }
            double dy = focus.y - (posY + getEyeHeight());
            rotationPitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        } else if (facingLocked) {
            renderYawOffset = rotationYawHead = rotationYaw;
        }
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        syncFacing();
        syncSpot();
        if (facingLocked || world.isRemote) {
            renderYawOffset = rotationYaw;
        }
    }

    /**
     * Which way he faces, carried in his synced data: turned bodily by a scene every tick, the
     * vanilla rotation updates never reached anyone watching, and he stood facing wherever he
     * was spawned facing.
     */
/**
     * Where he is, carried in his synced data too while a scene moves him: in Galacticraft's
     * space dimensions the vanilla tracker's small moves never arrived, and he was drawn
     * wherever he was last moved a long way to.
     */
    private void syncSpot() {
        Vec3d r = new Vec3d(dataManager.get(SPOT_X), dataManager.get(SPOT_Y), dataManager.get(SPOT_Z));
        if (world.isRemote) {
            if (r.y < -999D) {
                return;
            }
            double dx = r.x - posX, dy = r.y - posY, dz = r.z - posZ;
            double d = dx * dx + dy * dy + dz * dz;
            if (d > 36D) {
                setPosition(r.x, r.y, r.z);
                prevPosX = lastTickPosX = posX;
                prevPosY = lastTickPosY = posY;
                prevPosZ = lastTickPosZ = posZ;
            } else if (d > 1.0E-6) {
                setPosition(posX + dx * 0.6D, posY + dy * 0.6D, posZ + dz * 0.6D);
            }
            return;
        }
        if (true) {
            if (Math.abs(r.x - posX) + Math.abs(r.y - posY) + Math.abs(r.z - posZ) > 0.002D) {
                dataManager.set(SPOT_X, (float) posX);
                dataManager.set(SPOT_Y, (float) posY);
                dataManager.set(SPOT_Z, (float) posZ);
            }
        } else if (r.y > -999D) {
            dataManager.set(SPOT_Y, -1000F);
        }
    }

    /** Where the tracker says he is, the client does not listen: SPOT says it, and says it right (see EntityAdamWarlock). */
    @Override
    public void setPositionAndRotationDirect(double x, double y, double z, float yaw, float pitch, int increments, boolean teleport) {
        if (dataManager.get(SPOT_Y) < -999F) {
            super.setPositionAndRotationDirect(x, y, z, yaw, pitch, increments, teleport);
        }
    }

    private void syncFacing() {
        if (world.isRemote) {
            Rotations r = dataManager.get(FACING);
            rotationYaw += MathHelper.wrapDegrees(r.getY() - rotationYaw) * 0.6F;
            rotationYawHead += MathHelper.wrapDegrees(r.getZ() - rotationYawHead) * 0.6F;
            rotationPitch += (r.getX() - rotationPitch) * 0.6F;
            renderYawOffset = rotationYaw;
            return;
        }
        Rotations r = dataManager.get(FACING);
        if (Math.abs(MathHelper.wrapDegrees(r.getY() - rotationYaw)) > 0.5F || Math.abs(MathHelper.wrapDegrees(r.getZ() - rotationYawHead)) > 0.5F
                || Math.abs(r.getX() - rotationPitch) > 0.5F) {
            dataManager.set(FACING, new Rotations(rotationPitch, rotationYaw, rotationYawHead));
        }
    }


    private static float approach(float from, float to, float max) {
        return from + MathHelper.clamp(MathHelper.wrapDegrees(to - from), -max, max);
    }

    @Nullable
    private EntityLivingBase target() {
        if (prey != null) {
            return prey.isEntityAlive() && prey.world == world ? prey : null;
        }
        EntityPlayer p = targetId == null ? null : world.getPlayerEntityByUUID(targetId);
        if (p != null && p.isEntityAlive() && !p.isSpectator() && p.getDistanceSq(this) < 64D * 64D) {
            return p;
        }
        EntityPlayer near = world.getClosestPlayerToEntity(this, 48D);
        if (near != null && near.isEntityAlive() && !near.isSpectator()) {
            targetId = near.getUniqueID();
            return near;
        }
        return null;
    }

    private void fightTick() {
        EntityLivingBase target = target();
        if (target == null) {
            release();
            return;
        }
        if (attack == null) {
            // Between attacks he circles them slowly, high, the way a judge's bench sits over the dock.
            EntityLivingBase on = onNow(target);
            circle(on, 7.5D - phase(), 3.6D, 0.06D);
            watch(on);
            if (--nextAttack <= 0) {
                choose(target);
            }
            return;
        }
        EntityLivingBase at = aimed;
        if (at == null || !at.isEntityAlive() || at.world != world) {
            done();
            return;
        }
        attackTicks++;
        switch (attack) {
            case COMBO:
                combo(at);
                break;
            case SHARDS:
                shards(at);
                break;
            case ORB:
                orb(at);
                break;
            case METEOR:
                meteor(at);
                break;
            case GRASP:
                grasp(at);
                break;
        }
    }

    private void circle(EntityLivingBase target, double r, double up, double rate) {
        circle += 0.012F + phase() * 0.004F;
        hoverTo(target.getPositionVector().add(Math.cos(circle) * r, up, Math.sin(circle) * r), rate);
    }

    /** What next, and at whom: never the same thing twice running; close, as often as not, his hands. */
    private void choose(EntityLivingBase target) {
        aimed = pick(target);
        boolean player = aimed instanceof EntityPlayer;
        double near = getDistance(aimed);
        int[] weight = new int[Attack.values().length];
        weight[Attack.COMBO.ordinal()] = near < 7D ? 5 : 2;
        weight[Attack.SHARDS.ordinal()] = 3;
        weight[Attack.ORB.ordinal()] = 2;
        weight[Attack.METEOR.ordinal()] = 2;
        weight[Attack.GRASP.ordinal()] = player && prey == null && graspCooldown <= 0 && near < 18D ? 3 : 0;
        if (lastAttack != null) {
            weight[lastAttack.ordinal()] = 0;
        }
        int total = 0;
        for (int w : weight) {
            total += w;
        }
        int roll = rand.nextInt(Math.max(1, total));
        Attack next = Attack.SHARDS;
        for (Attack a : Attack.values()) {
            roll -= weight[a.ordinal()];
            if (roll < 0) {
                next = a;
                break;
            }
        }
        attack = next;
        attackTicks = 0;
    }

    /** Who he is on between attacks: whoever the last was at, if they are still about; else whoever he is after. */
    private EntityLivingBase onNow(EntityLivingBase target) {
        EntityLivingBase a = aimed;
        return a != null && a.isEntityAlive() && a.world == world && a.getDistanceSq(this) < 48D * 48D && fair(a) ? a : target;
    }

    /** Someone he can fight now: not a spectator, not Warlock while he is directed or down. */
    private static boolean fair(EntityLivingBase e) {
        if (e instanceof EntityPlayer) {
            return !((EntityPlayer) e).isSpectator();
        }
        return !(e instanceof EntityAdamWarlock && (((EntityAdamWarlock) e).isScripted() || ((EntityAdamWarlock) e).fallen()));
    }

    /**
     * Who the next attack is at. Whoever he is after, most often; but anyone else near who has been hurting him, and
     * Warlock, who always is, draw him off in proportion to how much: it is not all on whoever holds the gauntlet.
     */
    private EntityLivingBase pick(EntityLivingBase target) {
        if (prey != null) {
            return target;
        }
        List<EntityLivingBase> who = new ArrayList<>();
        List<Float> weight = new ArrayList<>();
        who.add(target);
        weight.add(8F);
        for (EntityPlayer p : world.playerEntities) {
            if (p != target && p.isEntityAlive() && fair(p) && p.getDistanceSq(this) < 40D * 40D) {
                who.add(p);
                weight.add(1F + threatOf(p));
            }
        }
        if (foe != null && foe.isEntityAlive() && foe.world == world && foe.getDistanceSq(this) < 48D * 48D && fair(foe)) {
            who.add(foe);
            weight.add(1.5F + Math.min(5F, threatOf(foe)));
        }
        if (rival != null && rival.isEntityAlive() && rival.world == world && rival.getDistanceSq(this) < 64D * 64D && !rival.isDriven()) {
            who.add(rival);
            weight.add(2.5F + Math.min(6F, threatOf(rival)));
        }
        float total = 0F;
        for (float w : weight) {
            total += w;
        }
        float roll = rand.nextFloat() * total;
        for (int i = 0; i < who.size(); i++) {
            roll -= weight.get(i);
            if (roll < 0F) {
                return who.get(i);
            }
        }
        return target;
    }

    private float threatOf(Entity e) {
        Float t = threat.get(e.getEntityId());
        return t == null ? 0F : Math.min(8F, t);
    }

    private void done() {
        if (attack != null) {
            attacksDone++;
            lastAttack = attack;
        }
        attack = null;
        release();
        nextAttack = 22 + rand.nextInt(22) - phase() * 5;
        perform(Pose.GUARD);
    }

    /** Lets go of whoever is in his hand, if anyone is. */
    private void release() {
        if (held != null) {
            held.fallDistance = 0F;
            held = null;
        }
    }

    // ------------------------------------------------------------------ the combo

    /** He does not cross the space between: he is simply in front of them, and then the palm, the backhand, the knee. */
    private void combo(EntityLivingBase t) {
        int s = attackTicks;
        double gap = t.width * 0.5D + 1.3D;
        if (s == 1) {
            blink(front(t, gap), t.getPositionEyes(1F), false);
            return;
        }
        hoverTo(front(t, gap), 0.35D);
        watch(t);
        if (s == 5) {
            perform(Pose.PALM, 0);
        } else if (s == 5 + PALM_HIT) {
            blow(t, 0.06F, 0, 0.5D, 0.25D);
        } else if (s == 17) {
            perform(Pose.BACKHAND, 0);
        } else if (s == 17 + BACKHAND_HIT) {
            blow(t, 0.06F, 0, 0.6D, 0.3D);
        } else if (s == 30) {
            perform(Pose.KNEE, 0);
            sound(SoundEvents.ENTITY_ENDERDRAGON_FLAP, 1.2F, 1.6F);
        } else if (s == 30 + KNEE_HIT) {
            if (blow(t, 0.12F, 1, 1.5D, 0.95D) && t == target()) {
                opening = true;
            }
        } else if (s >= 50) {
            done();
        }
    }

    /** One blow of his hands landing on someone, if they are still within reach of it; true if it did. */
    private boolean blow(EntityLivingBase t, float share, int kind, double out, double up) {
        if (getDistance(t) > t.width * 0.5D + 2.8D) {
            sound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1.5F, 0.6F);
            return false;
        }
        Vec3d away = flat(t.getPositionVector().subtract(getPositionVector()));
        Vec3d at = t.getPositionVector().add(0D, t.height * 0.6D, 0D).subtract(away.scale(t.width * 0.5D));
        fx(MsgTribunalFx.MAGUS_IMPACT, at, away, t.getEntityId(), kind);
        sound(kind > 0 ? SoundEvents.ENTITY_IRONGOLEM_ATTACK : SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, 2F, kind > 0 ? 0.6F : 0.8F);
        sound(SoundEvents.ENTITY_GENERIC_EXPLODE, kind > 0 ? 1.5F : 0.6F, 1.7F);
        harm(t, share, away, out, up);
        return true;
    }

    /** Where to be, to be in front of someone: between them and where he is now. */
    private Vec3d front(EntityLivingBase t, double gap) {
        Vec3d toward = flat(getPositionVector().subtract(t.getPositionVector()));
        return t.getPositionVector().add(toward.scale(gap)).add(0D, 0.15D, 0D);
    }

    // ------------------------------------------------------------------ shards

    /** His arm out at them; shards of thought hung in a fan round his shoulders, aimed, then loosed one after another. */
    private void shards(EntityLivingBase t) {
        int s = attackTicks;
        int n = 5 + phase();
        circle(t, 8.5D - phase(), 4D, 0.03D);
        watch(t);
        if (s == 1) {
            perform(Pose.POINT, 16 + n * 4);
            hangShards(t, n, 16);
        }
        if (s >= 16 + n * 4 + 8) {
            done();
        }
    }

    /** So many shards hung round him in a fan, the first loosed after {@code first} ticks and each after it four ticks later. */
    private void hangShards(EntityLivingBase t, int n, int first) {
        Vec3d forward = flat(t.getPositionVector().subtract(getPositionVector()));
        Vec3d right = new Vec3d(-forward.z, 0D, forward.x);
        for (int i = 0; i < n; i++) {
            double a = n == 1 ? 0D : -1.25D + 2.5D * i / (n - 1);
            Vec3d at = getPositionVector().add(0D, 1.9D + 0.7D * Math.cos(a), 0D).add(right.scale(Math.sin(a) * 2.3D))
                    .add(forward.scale(Math.cos(a) * 0.3D - 0.7D));
            // Out from the middle, alternately, so they go left, right, left.
            int order = i % 2 == 0 ? i / 2 : n - 1 - i / 2;
            Shot shot = new Shot(nextShot++, false, at, first + order * 4, t);
            shots.add(shot);
            Vec3d aim = t.getPositionVector().add(0D, t.height * 0.6D, 0D).subtract(at).normalize().scale(0.001D);
            fx(MsgTribunalFx.MAGUS_SHARD, at, aim, shot.id, shot.looseAt + 50);
        }
        sound(EternitySounds.OMEN_SHIMMER, 2F, 1.5F);
        sound(SoundEvents.BLOCK_GLASS_PLACE, 1.5F, 0.6F);
    }

    // ------------------------------------------------------------------ the orb

    /** Both hands drawn back, the light of the Power Stone gathered between them; then thrown, a sphere of it, slow and heavy. */
    private void orb(EntityLivingBase t) {
        int s = attackTicks;
        circle(t, 8D - phase(), 3.6D, 0.02D);
        watch(t);
        if (s == 1) {
            perform(Pose.BLAST, 0);
            fxAt(MsgTribunalFx.MAGUS_GATHER, getPositionVector(), 1.8D, 1.2D, 0.35D, getEntityId(), BLAST_HIT);
            sound(SoundEvents.EVOCATION_ILLAGER_PREPARE_SUMMON, 2F, 0.6F);
            sound(SoundEvents.BLOCK_PORTAL_TRIGGER, 0.6F, 1.8F);
        }
        if (s == BLAST_HIT) {
            Vec3d from = getPositionVector().add(0D, 1.4D, 0D).add(forwardOf().scale(1.1D));
            double pace = 0.55D + 0.08D * phase();
            Vec3d aim = lead(t, from, pace);
            Shot orb = new Shot(nextShot++, true, from, 0, t);
            orb.vel = aim.subtract(from).normalize().scale(pace);
            shots.add(orb);
            fx(MsgTribunalFx.MAGUS_ORB, from, orb.vel, orb.id, 90);
            sound(SoundEvents.ENTITY_WITHER_SHOOT, 2F, 0.5F);
            sound(EternitySounds.RIFT_IMPACT, 1.5F, 0.6F);
        }
        if (s >= 32) {
            done();
        }
    }

    // ------------------------------------------------------------------ the meteor

    /** Gone; a ring on the ground under them, closing; and then he is above it, both fists up, and down. */
    private void meteor(EntityLivingBase t) {
        int s = attackTicks;
        if (s == 1) {
            Vec3d feet = t.getPositionVector();
            RayTraceResult ground = world.rayTraceBlocks(feet.add(0D, 0.5D, 0D), feet.subtract(0D, 16D, 0D), false, true, false);
            markAt = ground != null && ground.typeOfHit == RayTraceResult.Type.BLOCK ? ground.hitVec : feet;
            dropFrom = markAt.add(0D, 13D, 0D);
            RayTraceResult roof = world.rayTraceBlocks(markAt.add(0D, 1D, 0D), dropFrom, false, true, false);
            if (roof != null && roof.typeOfHit == RayTraceResult.Type.BLOCK) {
                dropFrom = new Vec3d(markAt.x, roof.hitVec.y - 2.4D, markAt.z);
            }
            perform(Pose.BLINK_OUT, 0);
            sound(SoundEvents.ENTITY_ENDERMEN_TELEPORT, 1.5F, 0.6F);
            fxAt(MsgTribunalFx.MAGUS_MARK, markAt, 4.5D, 0D, 0D, -1, METEOR_LAND - 1);
            world.playSound(null, markAt.x, markAt.y, markAt.z, EternitySounds.OMEN_SHIMMER, SoundCategory.HOSTILE, 2F, 0.6F);
            return;
        }
        if (s == METEOR_OVER) {
            blink(dropFrom, markAt, false);
        }
        if (s > METEOR_OVER && s < METEOR_LAND) {
            float k = MathHelper.clamp((s - (METEOR_LAND - 5)) / 5F, 0F, 1F);
            place(dropFrom.add(markAt.subtract(dropFrom).scale(k * k)), markAt);
        }
        if (s == METEOR_RAISE) {
            perform(Pose.HAMMER, 0);
            sound(SoundEvents.ENTITY_ENDERDRAGON_GROWL, 1.2F, 1.5F);
        }
        if (s == METEOR_LAND) {
            place(markAt, null);
            fxAt(MsgTribunalFx.MAGUS_SLAM, markAt, 5D, 0D, 0D, -1, 0);
            sound(SoundEvents.ENTITY_GENERIC_EXPLODE, 3F, 0.55F);
            sound(EternitySounds.RIFT_IMPACT, 2F, 0.5F);
            sound(SoundEvents.ENTITY_LIGHTNING_THUNDER, 1.2F, 1.5F);
            for (EntityLivingBase e : near(markAt, 5.5D)) {
                Vec3d d = e.getPositionVector().subtract(markAt);
                double across = Math.sqrt(d.x * d.x + d.z * d.z);
                if (across > 5D || Math.abs(d.y) > 3D) {
                    continue;
                }
                boolean square = across < 2.5D;
                harm(e, square ? 0.24F : 0.14F, flat(d), square ? 1.4D : 1D, 0.85D);
                if (square && e == target()) {
                    opening = true;
                }
            }
        }
        if (s >= METEOR_LAND + 20) {
            done();
        }
    }

    // ------------------------------------------------------------------ the grasp

    /**
     * His open hand raised at them, trembling with what is in it; if he can see them, it has them,
     * and lifts them off their feet, held there in arcs of it; then the fist, and they are thrown down.
     */
    private void grasp(EntityLivingBase t) {
        int s = attackTicks;
        hoverTo(getPositionVector(), 0.1D);
        watch(t);
        if (s == 1) {
            resume(Pose.CRUSH, GRASP_FROM);
            fxAt(MsgTribunalFx.MAGUS_GATHER, getPositionVector(), 0.9D, 1.75D, 0.8D, getEntityId(), GRASP_CATCH);
            sound(SoundEvents.ENTITY_ILLAGER_PREPARE_BLINDNESS, 2F, 0.6F);
        }
        if (s == GRASP_CATCH) {
            Vec3d eyes = t.getPositionEyes(1F);
            RayTraceResult wall = world.rayTraceBlocks(palm(), eyes, false, true, false);
            if (getDistance(t) > 24D || wall != null && wall.typeOfHit == RayTraceResult.Type.BLOCK) {
                // Out of his sight: the hand closes on nothing.
                sound(SoundEvents.BLOCK_FIRE_EXTINGUISH, 1F, 0.6F);
                graspCooldown = 120;
                attackTicks = GRASP_CRUSH + 4;
            } else {
                held = t;
                heldFrom = t.getPositionVector();
                EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.MAGUS_GRASP, posX, posY, posZ, t.getEntityId(), 0, 0,
                        getEntityId(), GRASP_CRUSH - GRASP_CATCH + 2), dimension);
                sound(EternitySounds.OMEN_STARE, 2F, 0.7F);
                sound(SoundEvents.EVOCATION_ILLAGER_CAST_SPELL, 2F, 0.5F);
            }
        }
        EntityLivingBase h = held;
        if (h != null && s > GRASP_CATCH && s < GRASP_CRUSH) {
            if (!h.isEntityAlive() || h.world != world) {
                release();
            } else {
                // Up off their feet and held there, turning a little, helpless.
                float k = Math.min(1F, (s - GRASP_CATCH) / 10F);
                Vec3d up = heldFrom.add(0D, 3.2D * (1D - (1D - k) * (1D - k)), 0D);
                h.motionX = h.motionY = h.motionZ = 0D;
                h.fallDistance = 0F;
                if (h instanceof EntityPlayerMP) {
                    ((EntityPlayerMP) h).setPositionAndUpdate(up.x, up.y, up.z);
                } else {
                    h.setPosition(up.x, up.y, up.z);
                }
            }
        }
        if (s == GRASP_CRUSH) {
            graspCooldown = 360;
            if (h != null) {
                release();
                Vec3d away = flat(h.getPositionVector().subtract(getPositionVector()));
                fx(MsgTribunalFx.MAGUS_IMPACT, h.getPositionVector().add(0D, h.height * 0.6D, 0D), new Vec3d(away.x, -1D, away.z), h.getEntityId(), 2);
                sound(SoundEvents.ENTITY_GENERIC_EXPLODE, 2.5F, 0.7F);
                sound(SoundEvents.ENTITY_IRONGOLEM_ATTACK, 2F, 0.5F);
                harm(h, 0.18F, away, 1.2D, -1.4D);
                if (h == target()) {
                    opening = true;
                }
            }
        }
        if (s >= GRASP_CRUSH + 16) {
            done();
        }
    }

    // ------------------------------------------------------------------ what he throws

    private void tickShots() {
        // Over a copy: what one of them does can end the fight, and his going with it.
        for (Shot shot : new ArrayList<>(shots)) {
            if (!shots.contains(shot)) {
                continue;
            }
            shot.age++;
            if (shot.age < shot.looseAt) {
                continue;
            }
            if (!shot.orb && shot.age == shot.looseAt) {
                loose(shot);
                continue;
            }
            Vec3d next = shot.pos.add(shot.vel);
            boolean burst = shot.age - shot.looseAt > (shot.orb ? 80 : 40);
            RayTraceResult wall = world.rayTraceBlocks(shot.pos, next, false, true, false);
            if (wall != null && wall.typeOfHit == RayTraceResult.Type.BLOCK) {
                next = wall.hitVec;
                burst = true;
            }
            for (EntityLivingBase e : near(shot.pos, 10D)) {
                Vec3d body = e.getPositionVector().add(0D, e.height * 0.5D, 0D);
                if (closest(shot.pos, next, body).distanceTo(body) < (shot.orb ? 1.5D : 0.9D) + e.width * 0.3D) {
                    burst = true;
                }
            }
            shot.pos = next;
            if (burst) {
                shots.remove(shot);
                if (shot.orb) {
                    nova(shot);
                } else {
                    shatter(shot);
                }
            }
        }
    }

    /** A hung shard loosed at whoever it was for, a little ahead of where they are going. */
    private void loose(Shot shot) {
        EntityLivingBase t = shot.at;
        double pace = 1.5D + 0.1D * phase();
        Vec3d aim = t != null && t.isEntityAlive() && t.world == world ? lead(t, shot.pos, pace) : shot.pos.add(forwardOf().scale(10D));
        shot.vel = aim.subtract(shot.pos).normalize().scale(pace);
        fx(MsgTribunalFx.MAGUS_SHARD, shot.pos, shot.vel, shot.id, 45);
        world.playSound(null, shot.pos.x, shot.pos.y, shot.pos.z, SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.HOSTILE, 1.5F, 0.5F);
        world.playSound(null, shot.pos.x, shot.pos.y, shot.pos.z, SoundEvents.BLOCK_GLASS_HIT, SoundCategory.HOSTILE, 1.5F, 1.6F);
    }

    private void shatter(Shot shot) {
        Vec3d at = shot.pos;
        fxAt(MsgTribunalFx.MAGUS_SHATTER, at, 1D, 0D, 0D, shot.id, 0);
        world.playSound(null, at.x, at.y, at.z, SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 1.5F, 0.8F);
        for (EntityLivingBase e : near(at, 2.2D)) {
            harm(e, 0.06F, shot.vel.lengthSquared() > 0D ? flat(shot.vel) : flat(e.getPositionVector().subtract(at)), 0.5D, 0.25D);
        }
    }

    private void nova(Shot shot) {
        Vec3d at = shot.pos;
        fxAt(MsgTribunalFx.MAGUS_NOVA, at, 5D, 0D, 0D, shot.id, 0);
        world.playSound(null, at.x, at.y, at.z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 3F, 0.6F);
        world.playSound(null, at.x, at.y, at.z, EternitySounds.RIFT_IMPACT, SoundCategory.HOSTILE, 2F, 0.8F);
        for (EntityLivingBase e : near(at, 5D)) {
            double d = e.getDistance(at.x, at.y, at.z);
            harm(e, d < 2.5D ? 0.22F : 0.13F, flat(e.getPositionVector().subtract(at)), 1.5D, 0.7D);
        }
    }

    /** Whoever any of it can touch near a point: players (unless it is not them he is after), Warlock, his prey. */
    private List<EntityLivingBase> near(Vec3d at, double reach) {
        List<EntityLivingBase> all = new ArrayList<>();
        for (EntityLivingBase e : world.getEntitiesWithinAABB(EntityLivingBase.class, new AxisAlignedBB(at, at).grow(reach + 2D))) {
            if (e == this || !e.isEntityAlive() || e.getDistance(at.x, at.y, at.z) > reach + e.width) {
                continue;
            }
            if (e instanceof EntityPlayer ? prey == null && !((EntityPlayer) e).isSpectator() : e == prey || e instanceof EntityAdamWarlock || e instanceof EntityOdin) {
                all.add(e);
            }
        }
        return all;
    }

    /** Where to throw at someone from somewhere, so that it meets them: a little ahead of where they are going. */
    private static Vec3d lead(EntityLivingBase t, Vec3d from, double pace) {
        Vec3d at = t.getPositionVector().add(0D, t.height * 0.55D, 0D);
        double flight = at.distanceTo(from) / pace;
        return at.add(new Vec3d(t.posX - t.prevPosX, 0D, t.posZ - t.prevPosZ).scale(Math.min(flight, 30D) * 0.7D));
    }

    private static Vec3d closest(Vec3d a, Vec3d b, Vec3d p) {
        Vec3d ab = b.subtract(a);
        double len = ab.lengthSquared();
        if (len < 1.0E-9) {
            return a;
        }
        double k = MathHelper.clamp(p.subtract(a).dotProduct(ab) / len, 0D, 1D);
        return a.add(ab.scale(k));
    }

    private static Vec3d flat(Vec3d v) {
        Vec3d f = new Vec3d(v.x, 0D, v.z);
        return f.lengthSquared() < 1.0E-6 ? new Vec3d(1D, 0D, 0D) : f.normalize();
    }

    private Vec3d forwardOf() {
        float yaw = (float) Math.toRadians(rotationYaw);
        return new Vec3d(-MathHelper.sin(yaw), 0D, MathHelper.cos(yaw));
    }

    private void fx(byte type, Vec3d at, Vec3d dir, int id, int time) {
        EternityNetwork.sendToDimension(new MsgTribunalFx(type, at.x, at.y, at.z, dir.x, dir.y, dir.z, id, time), dimension);
    }

    private void fxAt(byte type, Vec3d at, double a, double b, double c, int id, int time) {
        EternityNetwork.sendToDimension(new MsgTribunalFx(type, at.x, at.y, at.z, a, b, c, id, time), dimension);
    }

    /**
     * Struck by something of his: a share of their health, and thrown, out and up (down, if up is negative). Never a
     * player while he is after someone else; Warlock, he only knocks about.
     */
    private void harm(EntityLivingBase e, float share, Vec3d away, double strength, double up) {
        if (prey != null && e instanceof EntityPlayer) {
            // Not them. Not today.
            return;
        }
        if (e instanceof EntityOdin) {
            e.attackEntityFrom(EternityDamage.wielded("magus", this), e.getMaxHealth() * share);
            return;
        }
        if (e instanceof EntityAdamWarlock) {
            EntityAdamWarlock w = (EntityAdamWarlock) e;
            w.knocked(away, strength, 0.8D);
            if (bossFight) {
                w.attackEntityFrom(EternityDamage.wielded("magus", this), w.getMaxHealth() * share * 0.4F);
            }
            return;
        }
        if (!(e instanceof EntityPlayer) || ((EntityPlayer) e).isSpectator()) {
            return;
        }
        EntityPlayer player = (EntityPlayer) e;
        EntityDestroyerArmor.strike(player, EternityDamage.wielded("magus", this), share);
        if (!player.isEntityAlive()) {
            return;
        }
        if (up >= 0D) {
            player.motionY = Math.max(player.motionY, 0D);
        }
        player.fallDistance = 0F;
        EntityDestroyerArmor.shove(player, away.x * strength, up, away.z * strength);
    }

    // ------------------------------------------------------------------ client: how he moves

    /** How hard what he holds burns in him in each pose. Past 1 he is incandescent. */
    private float powerFor(Pose pose, int t) {
        float base;
        switch (pose) {
            case STAND:
            case STAGGER:
            case FLUNG:
                base = 0.15F;
                break;
            case REGARD:
            case FLOAT:
                base = 0.25F;
                break;
            case POINT:
            case GRIP:
            case GUARD:
                base = 0.55F;
                break;
            case CRUSH:
                base = t < CRUSH_CLOSE ? 0.5F + 0.6F * t / CRUSH_CLOSE : t < CRUSH_CLOSE + 10 ? 1.4F : 0.35F;
                break;
            case BLOCK:
            case CATCH:
            case DESCEND:
            case CARRY:
                base = 0.75F;
                break;
            case PALM:
            case BACKHAND:
            case KNEE:
                base = 1F;
                break;
            case HAMMER:
            case BLAST:
                base = 1.25F;
                break;
            case TAKE:
                base = t < 10 ? 0.7F : 1.2F;
                break;
            case VANISH:
            case APPEAR:
            case BLINK_OUT:
            case BLINK_IN:
                base = 1.4F;
                break;
            case SLEEP:
                base = 0F;
                break;
            case CRADLE:
            case SLOT:
            case DON:
                base = 0.8F;
                break;
            case POWER:
                base = 0.7F + 0.9F * Math.min(1F, t / (float) POWER_TICKS);
                break;
            case SNAP:
                base = 1.7F;
                break;
            case STRUGGLE:
                base = 1.1F;
                break;
            case SPENT:
                base = 0.2F;
                break;
            case STIR:
                base = 0.05F;
                break;
            default:
                base = 0.3F;
                break;
        }
        return isCrowned() ? Math.max(base, 0.6F) : base;
    }

    private void clientTick() {
        clientPoseTicks++;
        double vx = posX - prevPosX, vy = posY - prevPosY, vz = posZ - prevPosZ;
        velX += (vx - velX) * 0.6D;
        velY += (vy - velY) * 0.6D;
        velZ += (vz - velZ) * 0.6D;
        double speed = Math.sqrt(velX * velX + velY * velY + velZ * velZ);
        float yaw = (float) Math.toRadians(rotationYaw);
        double fwd = -velX * MathHelper.sin(yaw) + velZ * MathHelper.cos(yaw);
        double side = velX * MathHelper.cos(yaw) + velZ * MathHelper.sin(yaw);
        prevBodyPitch = bodyPitch;
        prevBodyRoll = bodyRoll;
        // He leans a little into where he drifts, never more: he does not hurry.
        bodyPitch += ((float) MathHelper.clamp(fwd * 30D, -14D, 20D) - bodyPitch) * 0.15F;
        bodyRoll += ((float) MathHelper.clamp(-side * 30D, -12D, 12D) - bodyRoll) * 0.15F;
        prevPower = power;
        power += (powerFor(pose(), clientPoseTicks) - power) * 0.2F;
        for (int i = TRAIL - 1; i > 0; i--) {
            System.arraycopy(trail[i - 1], 0, trail[i], 0, 7);
        }
        trail[0][0] = posX;
        trail[0][1] = posY;
        trail[0][2] = posZ;
        trail[0][3] = rotationYaw;
        trail[0][4] = bodyPitch;
        trail[0][5] = bodyRoll;
        trail[0][6] = isAway() ? 0D : MathHelper.clamp((speed - 0.3D) / 1D, 0D, 1D);
        trailCount = Math.min(TRAIL, trailCount + 1);
        if (burn() > 0F && !isAway()) {
            smoulder();
        }
    }

    /** Smoke off the burnt arm, thick and full of sparks at first, thinning to a wisp now and then. */
    private void smoulder() {
        float fresh = Math.max(0F, 1F - (ticksExisted - burntAt) / 160F);
        if (rand.nextFloat() > 0.25F + 0.7F * fresh) {
            return;
        }
        float yaw = (float) Math.toRadians(renderYawOffset);
        Vec3d forward = new Vec3d(-MathHelper.sin(yaw), 0D, MathHelper.cos(yaw));
        Vec3d right = new Vec3d(-forward.z, 0D, forward.x);
        boolean up = pose() == Pose.SNAP;
        Vec3d hand = getPositionVector().add(0D, up ? 2.6D : 0.95D, 0D).add(right.scale(up ? 0.35D : 0.48D)).add(forward.scale(up ? 0D : 0.12D));
        double along = rand.nextDouble() * 0.7D;
        Vec3d at = hand.add(0D, up ? -along : along, 0D);
        world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, at.x + (rand.nextDouble() - 0.5D) * 0.15D, at.y, at.z + (rand.nextDouble() - 0.5D) * 0.15D,
                0D, 0.02D + rand.nextDouble() * 0.02D, 0D);
        if (rand.nextFloat() < 0.15F + 0.6F * fresh) {
            world.spawnParticle(EnumParticleTypes.REDSTONE, at.x + (rand.nextDouble() - 0.5D) * 0.2D, at.y, at.z + (rand.nextDouble() - 0.5D) * 0.2D,
                    1D, 0.42D + rand.nextDouble() * 0.2D, 0.05D);
        }
        if (fresh > 0.7F && rand.nextFloat() < 0.3F) {
            world.spawnParticle(EnumParticleTypes.LAVA, hand.x, hand.y, hand.z, 0D, 0D, 0D);
        }
    }

    // ------------------------------------------------------------------ being struck

    /**
     * Blows land, and are counted, in the fights he lets them. In the Grandmaster's game none of them matters. In the
     * fight for the gauntlet they tell, a little at a time (no blow takes more than a sliver of him), and whoever
     * lands them draws his eye; run his health out and he reels, and drops what he holds. He never dies of it.
     */
    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (world.isRemote || source == DamageSource.OUT_OF_WORLD) {
            return source == DamageSource.OUT_OF_WORLD && super.attackEntityFrom(source, amount);
        }
        Entity by = source.getTrueSource();
        boolean player = by instanceof EntityPlayerMP && !((EntityPlayerMP) by).isSpectator();
        boolean odin = by instanceof EntityOdin;
        if (!(fighting || bossFight) || !(player || odin || by instanceof EntityAdamWarlock) || hurtResistantTime > 10) {
            return false;
        }
        if (bossFight && (guarded || isAway())) {
            return false;
        }
        hits++;
        hurtResistantTime = 20;
        world.setEntityState(this, (byte) 2);
        sound(SoundEvents.BLOCK_ANVIL_LAND, 0.5F, 1.8F);
        if (!bossFight) {
            return true;
        }
        float dealt = Math.min(player || odin ? amount : amount * (float) com.matoon.eternity.EternityConfig.magus.warlockShare,
                getMaxHealth() * (float) com.matoon.eternity.EternityConfig.magus.maxHitFraction);
        threat.merge(by.getEntityId(), player ? 2.5F : odin ? 2F : 1.2F, Float::sum);
        if (!(dealt > 0F)) {
            return true;
        }
        float left = getHealth() - dealt;
        if (left <= 1F) {
            setHealth(1F);
            if (!broken) {
                broken = true;
                breaks++;
            }
        } else {
            setHealth(left);
        }
        bossInfo.setPercent(getHealth() / getMaxHealth());
        return true;
    }

    @Override
    public boolean isEntityInvulnerable(DamageSource source) {
        return !(fighting || bossFight) && source != DamageSource.OUT_OF_WORLD;
    }

    @Override
    public void onDeath(DamageSource cause) {
        if (bossFight) {
            // Only the snap ends him.
            setHealth(1F);
            return;
        }
        super.onDeath(cause);
    }

    @Override
    protected boolean processInteract(EntityPlayer player, EnumHand hand) {
        if (sleeper && !world.isRemote && hand == EnumHand.MAIN_HAND && player instanceof EntityPlayerMP) {
            StoryDirector.INSTANCE.touchSleeper((EntityPlayerMP) player, new BlockPos(posX, posY, posZ));
        }
        return sleeper;
    }

    @Override
    public void knockBack(Entity source, float strength, double x, double z) {
    }

    @Override
    public void fall(float distance, float damageMultiplier) {
    }

    @Override
    public boolean canBePushed() {
        return false;
    }

    @Override
    protected void collideWithEntity(Entity entity) {
    }

    @Override
    public boolean canDespawn() {
        return false;
    }

    @Override
    protected boolean canDropLoot() {
        return false;
    }

    @Override
    public boolean writeToNBTOptional(NBTTagCompound compound) {
        return false;
    }

    @Override
    public boolean isInRangeToRenderDist(double distance) {
        return distance < 256D * 256D;
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return getEntityBoundingBox().grow(4D);
    }

    @Override
    public boolean isEntityInsideOpaqueBlock() {
        return false;
    }

    private void sound(net.minecraft.util.SoundEvent sound, float volume, float pitch) {
        world.playSound(null, posX, posY + 1D, posZ, sound, SoundCategory.HOSTILE, volume, pitch);
    }

    @Override
    public float getEyeHeight() {
        return 1.95F;
    }
}
