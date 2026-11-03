package com.matoon.eternity.entity;

import com.matoon.eternity.story.StoryDirector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
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
        CARRY;

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

    private static final DataParameter<Integer> POSE = EntityDataManager.createKey(EntityMagus.class, DataSerializers.VARINT);
    // Three floats, not Rotations: Rotations wraps every value modulo 360, which turned a point far from the origin into nonsense.
    private static final DataParameter<Float> FOCUS_X = EntityDataManager.createKey(EntityMagus.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> FOCUS_Y = EntityDataManager.createKey(EntityMagus.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> FOCUS_Z = EntityDataManager.createKey(EntityMagus.class, DataSerializers.FLOAT);
    private static final DataParameter<Boolean> CROWNED = EntityDataManager.createKey(EntityMagus.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Rotations> FACING = EntityDataManager.createKey(EntityMagus.class, DataSerializers.ROTATIONS);
    /** Where he is, for the client (see syncSpot): three floats, not Rotations, which wraps every value to within 360. */
    private static final DataParameter<Float> SPOT_X = EntityDataManager.createKey(EntityMagus.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> SPOT_Y = EntityDataManager.createKey(EntityMagus.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> SPOT_Z = EntityDataManager.createKey(EntityMagus.class, DataSerializers.FLOAT);

    private enum Attack {
        BLAST, BOLT, DIVE
    }

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
    @Nullable
    private EntityLivingBase foe;
    @Nullable
    private Attack attack;
    private int attackTicks;
    private int nextAttack = 50;
    private int hits;
    private int fightTicks;
    private float circle;

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

    public EntityMagus(World world) {
        super(world);
        setSize(0.7F, 2.1F);
        setNoGravity(true);
        noClip = true;
        isImmuneToFire = true;
        experienceValue = 0;
        ignoreFrustumCheck = true;
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(POSE, Pose.FLOAT.ordinal());
        dataManager.register(FOCUS_X, 0F);
        dataManager.register(FOCUS_Y, -1000F);
        dataManager.register(FOCUS_Z, 0F);
        dataManager.register(CROWNED, false);
        dataManager.register(FACING, new Rotations(0F, 0F, 0F));
        dataManager.register(SPOT_X, 0F);
        dataManager.register(SPOT_Y, -1000F);
        dataManager.register(SPOT_Z, 0F);
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
            clientPoseTicks = 0;
            System.arraycopy(poseLast, 0, poseFrom, 0, poseLast.length);
            previousPose = seenPose;
            seenPose = pose();
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
        int seq = (dataManager.get(POSE) >>> 8) + 1 & 0xFFFF;
        dataManager.set(POSE, seq << 8 | pose.ordinal());
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

    /** At someone, for real, with whoever else stands with them as a second target. */
    public void fight(EntityPlayer target, @Nullable EntityLivingBase second) {
        fighting = true;
        targetId = target.getUniqueID();
        foe = second;
        hits = 0;
        fightTicks = 0;
        nextAttack = 40;
        rest(Pose.GUARD);
        perform(Pose.GUARD);
    }

    /** Out of the fight and back under the story's hand, wherever he is. */
    public void stand() {
        fighting = false;
        attack = null;
        rest(Pose.FLOAT);
        hoverTo(getPositionVector(), 0.1D);
    }

    public boolean isFighting() {
        return fighting;
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
        if (fighting) {
            fightTicks++;
            fightTick();
        }
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
            return;
        }
        // He circles them slowly, high, the way a judge's bench sits over the dock.
        circle += 0.012F + phase() * 0.004F;
        double r = attack == Attack.DIVE ? 2.5D : 7.5D - phase();
        Vec3d at = target.getPositionVector().add(Math.cos(circle) * r, attack == Attack.DIVE ? 1.2D : 3.6D, Math.sin(circle) * r);
        hoverTo(at, attack == Attack.DIVE ? 0.22D : 0.06D);
        watch(target);
        if (attack == null) {
            if (--nextAttack <= 0) {
                double roll = rand.nextDouble();
                attack = roll < 0.4D ? Attack.BOLT : roll < 0.75D ? Attack.BLAST : Attack.DIVE;
                attackTicks = 0;
                if (attack == Attack.BOLT) {
                    perform(Pose.POINT, 26);
                } else if (attack == Attack.BLAST) {
                    perform(Pose.BLAST, 0);
                } else {
                    // He does not fly down to them: he is simply there.
                    Vec3d near = target.getPositionVector().add(Math.cos(circle) * 2.5D, 1.2D, Math.sin(circle) * 2.5D);
                    blink(near, target.getPositionEyes(1F), false);
                }
                sound(SoundEvents.ENTITY_ILLAGER_PREPARE_MIRROR, 1.5F, 0.6F);
            }
            return;
        }
        attackTicks++;
        switch (attack) {
            case BOLT:
                if (attackTicks == 10) {
                    bolt(rand.nextInt(4) == 0 && foe != null && foe.isEntityAlive() ? foe : target);
                }
                if (attackTicks >= 26) {
                    done();
                }
                break;
            case BLAST:
                if (attackTicks == BLAST_HIT) {
                    blast(7.5D, 0.2F);
                }
                if (attackTicks >= 26) {
                    done();
                }
                break;
            case DIVE:
                if (attackTicks == 10) {
                    perform(Pose.HAMMER, 0);
                }
                if (attackTicks == 10 + HAMMER_HIT) {
                    blast(4.5D, 0.28F);
                }
                if (attackTicks >= 40) {
                    done();
                }
                break;
        }
    }

    private void done() {
        attack = null;
        nextAttack = 45 + rand.nextInt(30) - phase() * 8;
        perform(Pose.GUARD);
    }

    /** One bolt of what he holds, straight at someone. Walls stop it; nothing else does. */
    private void bolt(EntityLivingBase at) {
        Vec3d from = palm();
        Vec3d to = at.getPositionVector().add(0D, at.height * 0.6D, 0D);
        RayTraceResult wall = world.rayTraceBlocks(from, to, false, true, false);
        Vec3d end = wall != null && wall.typeOfHit == RayTraceResult.Type.BLOCK ? wall.hitVec : to;
        WorldServer ws = (WorldServer) world;
        Vec3d step = end.subtract(from);
        int n = (int) (step.length() * 3D);
        for (int i = 0; i <= n; i++) {
            Vec3d p = from.add(step.scale(i / (double) Math.max(1, n)));
            ws.spawnParticle(EnumParticleTypes.SPELL_WITCH, p.x, p.y, p.z, 2, 0.05D, 0.05D, 0.05D, 0D);
            if (i % 3 == 0) {
                ws.spawnParticle(EnumParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.02D, 0.02D, 0.02D, 0D);
            }
        }
        sound(SoundEvents.EVOCATION_ILLAGER_CAST_SPELL, 2F, 0.5F);
        if (end == to) {
            harm(at, 0.14F, step.normalize(), 0.8D);
        }
    }

    /** Both hands thrown open: everything near him is thrown back, hard. */
    private void blast(double reach, float share) {
        WorldServer ws = (WorldServer) world;
        ws.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE, posX, posY + 1.2D, posZ, 6, 1.5D, 1D, 1.5D, 0D);
        ws.spawnParticle(EnumParticleTypes.DRAGON_BREATH, posX, posY + 1.2D, posZ, 120, reach * 0.4D, 1D, reach * 0.4D, 0.08D);
        sound(SoundEvents.ENTITY_GENERIC_EXPLODE, 3F, 0.55F);
        sound(SoundEvents.ENTITY_ENDERDRAGON_GROWL, 1.2F, 1.6F);
        for (EntityLivingBase e : world.getEntitiesWithinAABB(EntityLivingBase.class, getEntityBoundingBox().grow(reach))) {
            if (e == this || e.getDistance(this) > reach) {
                continue;
            }
            Vec3d away = e.getPositionVector().subtract(getPositionVector());
            away = away.lengthSquared() < 1.0E-4 ? new Vec3d(1D, 0D, 0D) : away.normalize();
            harm(e, share, away, 1.6D);
        }
    }

    private void harm(EntityLivingBase e, float share, Vec3d away, double strength) {
        if (e instanceof EntityAdamWarlock) {
            ((EntityAdamWarlock) e).knocked(away, strength, 0.8D);
            return;
        }
        if (!(e instanceof EntityPlayer) || ((EntityPlayer) e).isSpectator()) {
            return;
        }
        EntityPlayer player = (EntityPlayer) e;
        EntityDestroyerArmor.strike(player, EternityDamage.wielded("magus", this), share);
        player.motionX += away.x * strength;
        player.motionY = Math.max(player.motionY, 0D) + 0.45D;
        player.motionZ += away.z * strength;
        player.velocityChanged = true;
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
    }

    // ------------------------------------------------------------------ being struck

    /** Blows land, and are counted, in the one fight he lets them. None of them matters. */
    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (world.isRemote || source == DamageSource.OUT_OF_WORLD) {
            return source == DamageSource.OUT_OF_WORLD && super.attackEntityFrom(source, amount);
        }
        Entity by = source.getTrueSource();
        if (!fighting || !(by instanceof EntityPlayerMP || by instanceof EntityAdamWarlock) || hurtResistantTime > 10) {
            return false;
        }
        hits++;
        hurtResistantTime = 20;
        world.setEntityState(this, (byte) 2);
        sound(SoundEvents.BLOCK_ANVIL_LAND, 0.5F, 1.8F);
        return true;
    }

    @Override
    public boolean isEntityInvulnerable(DamageSource source) {
        return !fighting && source != DamageSource.OUT_OF_WORLD;
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
