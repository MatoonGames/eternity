package com.matoon.eternity.entity;

import com.matoon.eternity.EternityConfig;
import com.matoon.eternity.EternitySounds;
import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.network.MsgTribunalFx;
import com.matoon.eternity.story.Speakers;
import com.matoon.eternity.story.StoryDirector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Rotations;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BossInfo;
import net.minecraft.world.BossInfoServer;
import net.minecraft.world.World;
import net.minecraft.util.text.TextComponentString;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Odin Borson, the Allfather, woken out of the Odinsleep, on Sleipnir. He comes down the Bifrost into the Magus's last
 * fight and makes it three ways: he wants the stones, and he wants everyone else who has a claim on them dead, the
 * Magus and whoever holds the gauntlet alike.
 * <p>
 * Sleipnir runs on the air as well as the ground, so he is never stuck: he circles whoever he means to strike next at
 * a gallop, and between gallops he does one of six things to them.
 * <ul>
 * <li><b>Charge:</b> Sleipnir rears, and comes on at a dead run with Gungnir levelled, through whatever is in the way.</li>
 * <li><b>Gungnir:</b> thrown. It turns after them for most of the way, and comes back to his hand.</li>
 * <li><b>The Odinforce:</b> Gungnir raised, the power gathering on its blade; then rammed into the ground, and a ring of
 * it goes out along the ground. Be off the ground when it reaches you.</li>
 * <li><b>The Bifrost:</b> Gungnir raised to the sky; rings of light marked on the ground round them, and the Bifrost
 * comes down on each in turn.</li>
 * <li><b>Huginn and Muninn:</b> his ravens, loosed at them; they come at the eyes, again and again.</li>
 * <li><b>Sleipnir:</b> close, he rears, and brings all eight hooves down.</li>
 * </ul>
 * Nobody here can kill him. Run his health out and he is driven back, Sleipnir rearing, and comes again at half.
 * <p>
 * Everything the story needs to stage him is here too: put him somewhere, have him strike a pose, give him the gauntlet
 * to carry, take him out of the fight and put him back in it.
 */
public class EntityOdin extends EntityLiving {

    public enum Pose {
        /** Sleipnir standing, shifting his weight; Odin upright, Gungnir grounded at his side. */
        STAND,
        /** At a run, Gungnir upright. */
        GALLOP,
        /** At a run, Gungnir levelled like a lance. */
        CHARGE,
        /** Coming down the Bifrost at a run, nose down. */
        DESCEND,
        /** Sleipnir up on his hind legs, forelegs striking at the air; Odin high in the saddle with Gungnir raised. */
        REAR,
        /** Out of the rear, all eight hooves coming down at once. */
        STOMP,
        /** Gungnir drawn back over his shoulder, and thrown. */
        THROW,
        /** Gungnir coming back into his hand. */
        CATCH,
        /** Gungnir raised to the sky. */
        CALL,
        /** Gungnir rammed point-down into the ground beside him. */
        SLAM,
        /** Gungnir levelled at someone, the arm straight. */
        POINT,
        /** The gauntlet held against him in the crook of his left arm, Gungnir in his right. */
        CRADLE;

        public int ticks() {
            switch (this) {
                case REAR:
                    return REAR_TICKS;
                case STOMP:
                    return STOMP_TICKS;
                case THROW:
                    return THROW_TICKS;
                case CATCH:
                    return CATCH_TICKS;
                case CALL:
                    return CALL_TICKS;
                case SLAM:
                    return SLAM_TICKS;
                case POINT:
                    return POINT_TICKS;
                default:
                    return 30;
            }
        }
    }

    public static final int REAR_TICKS = 34, STOMP_TICKS = 22, STOMP_HIT = 8, THROW_TICKS = 26, THROW_RELEASE = 13, CATCH_TICKS = 12,
            CALL_TICKS = 34, SLAM_TICKS = 24, SLAM_HIT = 9, POINT_TICKS = 40;
    /** Taller than any man, on a horse taller than any horse. */
    public static final float SCALE = 1.15F;
    /** Where Gungnir's blade is held, in blocks above his feet, as he sits in the saddle. */
    public static final double HAND_HEIGHT = 2.9D;

    private enum Attack {CHARGE, THROW, ODINFORCE, BIFROST, RAVENS, STOMP}

    private static final DataParameter<Integer> POSE = EntityDataManager.createKey(EntityOdin.class, DataSerializers.VARINT);
    private static final DataParameter<Rotations> FACING = EntityDataManager.createKey(EntityOdin.class, DataSerializers.ROTATIONS);
    private static final DataParameter<Float> SPOT_X = EntityDataManager.createKey(EntityOdin.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> SPOT_Y = EntityDataManager.createKey(EntityOdin.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> SPOT_Z = EntityDataManager.createKey(EntityOdin.class, DataSerializers.FLOAT);
    /** Whether Gungnir is in his hand (it is not while it flies). */
    private static final DataParameter<Boolean> SPEAR = EntityDataManager.createKey(EntityOdin.class, DataSerializers.BOOLEAN);
    /** The gauntlet, if he has it, carried in the crook of his arm. */
    private static final DataParameter<ItemStack> GAUNTLET = EntityDataManager.createKey(EntityOdin.class, DataSerializers.ITEM_STACK);

    /** Gungnir thrown: where it is, how it moves, who it was thrown at, and whether it is coming back. */
    private static final class Spear {
        final int id;
        Vec3d pos, vel;
        @Nullable
        final EntityLivingBase at;
        int age;
        boolean returning;

        Spear(int id, Vec3d pos, Vec3d vel, @Nullable EntityLivingBase at) {
            this.id = id;
            this.pos = pos;
            this.vel = vel;
            this.at = at;
        }
    }

    /** One of his ravens: where it is, who it is after, how often it has struck, and whether it is going home. */
    private static final class Raven {
        final int id;
        Vec3d pos, vel;
        final EntityLivingBase at;
        int age, pecks, away;

        Raven(int id, Vec3d pos, Vec3d vel, EntityLivingBase at) {
            this.id = id;
            this.pos = pos;
            this.vel = vel;
            this.at = at;
        }
    }

    /** Where the Bifrost will come down, and when. */
    private static final class Mark {
        final Vec3d at;
        final int strikeAt;

        Mark(Vec3d at, int strikeAt) {
            this.at = at;
            this.strikeAt = strikeAt;
        }
    }

    // ------------------------------------------------------------------ server state
    private Pose rest = Pose.STAND;
    private int poseTicks;
    private int holdFor = -1;
    @Nullable
    private Vec3d hoverAt;
    private double rate = 0.1D, maxStep = 1.25D;
    @Nullable
    private Entity watching;
    @Nullable
    private Vec3d focus;
    private boolean facingLocked;
    private boolean fighting, guarded;
    /** Holding off for a moment (someone is locked in a struggle, someone is dying): he circles, and strikes nobody. */
    private boolean holding;
    @Nullable
    private UUID targetId;
    @Nullable
    private EntityMagus magus;
    @Nullable
    private EntityAdamWarlock warlock;
    @Nullable
    private Attack attack, lastAttack;
    private int attackTicks, nextAttack = 60;
    @Nullable
    private EntityLivingBase aimed;
    private float circle;
    private int ravensCooldown;
    private final Map<Integer, Float> threat = new HashMap<>();
    // The attack under way.
    private Vec3d chargeDir = Vec3d.ZERO, chargeTo = Vec3d.ZERO;
    private int chargeEnd;
    private final Set<Integer> struck = new HashSet<>();
    @Nullable
    private Spear spear;
    private final List<Raven> ravens = new ArrayList<>();
    private final List<Mark> marks = new ArrayList<>();
    @Nullable
    private Vec3d ringAt;
    private int ringAge = -1;
    private int nextId;
    // Driven back: when, from where, to where.
    private int driven = -1;
    private Vec3d drivenFrom = Vec3d.ZERO, drivenTo = Vec3d.ZERO;
    private final BossInfoServer bossInfo = (BossInfoServer) new BossInfoServer(new TextComponentString("Odin, the Allfather"),
            BossInfo.Color.YELLOW, BossInfo.Overlay.NOTCHED_10).setDarkenSky(true);

    // ------------------------------------------------------------------ client state
    /** Client: ticks since the synced pose last changed. */
    public int clientPoseTicks;
    /** Client: the pose he was drawn in when it changed, which the model blends out of; and the pose last drawn. */
    public final float[] poseFrom = new float[48], poseLast = new float[48];
    /** Client: how far round Sleipnir's stride is (radians), how much he is running (0 to 1), and how fast he is moving. */
    public float gait, prevGait, running, prevRunning;
    public double velX, velY, velZ;
    /** Client: Sleipnir's pitch into the run, smoothed. */
    public float bodyPitch, prevBodyPitch;

    public EntityOdin(World world) {
        super(world);
        setSize(1.8F, 3.4F);
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
        dataManager.register(POSE, Pose.STAND.ordinal());
        dataManager.register(FACING, new Rotations(0F, 0F, 0F));
        dataManager.register(SPOT_X, 0F);
        dataManager.register(SPOT_Y, -1000F);
        dataManager.register(SPOT_Z, 0F);
        dataManager.register(SPEAR, true);
        dataManager.register(GAUNTLET, ItemStack.EMPTY);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(EternityConfig.odin.health);
        getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(1D);
    }

    @Override
    public void notifyDataManagerChange(DataParameter<?> key) {
        super.notifyDataManagerChange(key);
        if (POSE.equals(key) && world.isRemote) {
            clientPoseTicks = dataManager.get(POSE) >>> 16;
            System.arraycopy(poseLast, 0, poseFrom, 0, poseLast.length);
        }
    }

    // ------------------------------------------------------------------ accessors

    public Pose pose() {
        int ordinal = dataManager.get(POSE) & 0xFF;
        return ordinal < Pose.values().length ? Pose.values()[ordinal] : Pose.STAND;
    }

    public int poseTicks() {
        return world.isRemote ? clientPoseTicks : poseTicks;
    }

    /** Whether Gungnir is in his hand. */
    public boolean hasSpear() {
        return dataManager.get(SPEAR);
    }

    public void spear(boolean inHand) {
        dataManager.set(SPEAR, inHand);
    }

    /** The gauntlet he carries, or empty. */
    public ItemStack gauntlet() {
        return dataManager.get(GAUNTLET);
    }

    public void holdGauntlet(ItemStack gauntlet) {
        dataManager.set(GAUNTLET, gauntlet.copy());
    }

    /** Where Gungnir's blade is, near enough, for what is drawn on it and what is thrown from it. */
    public Vec3d bladeAt() {
        Vec3d forward = forwardOf(), right = new Vec3d(-forward.z, 0D, forward.x);
        double up = pose() == Pose.CALL ? 5.4D : pose() == Pose.CHARGE || pose() == Pose.POINT ? 3D : 4.6D;
        double ahead = pose() == Pose.CHARGE || pose() == Pose.POINT ? 2.6D : 0.2D;
        return getPositionVector().add(0D, up * SCALE * 0.87D, 0D).add(forward.scale(ahead)).add(right.scale(0.55D));
    }

    /** Where his chest is, for what comes out of him or is drawn to him. */
    public Vec3d chest() {
        return getPositionVector().add(0D, 2.55D * SCALE, 0D).add(forwardOf().scale(0.1D));
    }

    // ------------------------------------------------------------------ the story's handles

    /** Strikes a pose now, from its start, and holds it. */
    public EntityOdin perform(Pose pose) {
        return perform(pose, -1);
    }

    /** Strikes a pose for so many ticks (its own length, if 0), then goes back to resting. */
    public EntityOdin perform(Pose pose, int ticks) {
        poseTicks = 0;
        holdFor = ticks == 0 ? pose.ticks() : ticks;
        int seq = (dataManager.get(POSE) >>> 8 & 0xFF) + 1 & 0xFF;
        dataManager.set(POSE, seq << 8 | pose.ordinal());
        return this;
    }

    /** What he goes back to after a gesture. */
    public EntityOdin rest(Pose pose) {
        rest = pose;
        return this;
    }

    /** Exactly here this tick, turning to face there. */
    public void place(Vec3d at, @Nullable Vec3d face) {
        hoverAt = null;
        setPosition(at.x, at.y, at.z);
        if (face != null) {
            faceNow(face);
        }
    }

    /** Where he goes now, and how fast he closes on it (a share of the distance each tick, at most maxStep blocks). */
    public EntityOdin hoverTo(@Nullable Vec3d at, double rate) {
        hoverAt = at;
        this.rate = rate;
        maxStep = 1.25D;
        return this;
    }

    public void faceNow(Vec3d at) {
        facingLocked = true;
        double dx = at.x - posX, dz = at.z - posZ;
        rotationYaw = rotationYawHead = renderYawOffset = (float) Math.toDegrees(Math.atan2(-dx, dz));
    }

    public EntityOdin look(@Nullable Vec3d at) {
        watching = null;
        facingLocked = false;
        focus = at;
        return this;
    }

    public EntityOdin watch(@Nullable Entity who) {
        watching = who;
        facingLocked = false;
        return this;
    }

    /** Into the fight: at whoever holds the gauntlet, the Magus, Warlock. */
    public void fight(@Nullable EntityPlayer target, @Nullable EntityMagus magus, @Nullable EntityAdamWarlock warlock) {
        this.magus = magus;
        this.warlock = warlock;
        if (target != null) {
            targetId = target.getUniqueID();
        }
        if (!fighting) {
            fighting = true;
            attack = null;
            nextAttack = 40;
            rest(Pose.STAND);
        }
    }

    /** Out of the fight and back under the story's hand. What he has loosed is called back. */
    public void stand() {
        fighting = false;
        attack = null;
        aimed = null;
        driven = -1;
        clearShots();
        rest(Pose.STAND);
        hoverTo(getPositionVector(), 0.1D);
    }

    public boolean isFighting() {
        return fighting;
    }

    /** His boss bar up, his health whole; or down. */
    public void boss(boolean on) {
        if (on) {
            setHealth(getMaxHealth());
            threat.clear();
        }
        bossInfo.setPercent(getHealth() / getMaxHealth());
        bossInfo.setVisible(on);
    }

    /** Holds off striking, or not: while held he goes on circling, and lets whatever he has loosed land. */
    public void hold(boolean on) {
        holding = on;
    }

    /** Nothing touches him while the story has him. */
    public void guard(boolean on) {
        guarded = on;
    }

    /** Gungnir back in his hand, his ravens gone home, the marks on the ground gone out. */
    public void clearShots() {
        if (spear != null) {
            fxAt(MsgTribunalFx.ODIN_SPEAR_HIT, spear.pos, 0.6D, 0D, 0D, spear.id, 0);
            spear = null;
        }
        for (Raven r : ravens) {
            fxAt(MsgTribunalFx.ODIN_RAVEN, r.pos, 0D, 0D, 0D, r.id, 2);
        }
        ravens.clear();
        marks.clear();
        ringAge = -1;
        spear(true);
    }

    /** Whether he has been driven back, and is coming again. */
    public boolean isDriven() {
        return driven >= 0;
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
        if (holdFor > 0 && poseTicks >= holdFor) {
            holdFor = -1;
            if (pose() != rest) {
                perform(rest);
            }
        }
        if (ravensCooldown > 0) {
            ravensCooldown--;
        }
        if (!threat.isEmpty()) {
            threat.replaceAll((id, t) -> t * 0.995F);
            threat.values().removeIf(t -> t < 0.05F);
        }
        if (ticksExisted % 5 == 0) {
            bossInfo.setPercent(getHealth() / getMaxHealth());
        }
        if (fighting) {
            fightTick();
        }
        tickSpear();
        tickRavens();
        tickMarks();
        tickRing();
        Vec3d before = getPositionVector();
        if (hoverAt != null) {
            Vec3d to = hoverAt.subtract(getPositionVector());
            double d = to.length();
            if (d > 0.02D) {
                Vec3d step = to.scale(Math.min(1D, rate));
                if (step.length() > maxStep) {
                    step = step.normalize().scale(maxStep);
                }
                setPosition(posX + step.x, posY + step.y, posZ + step.z);
            }
        }
        // Moving at any pace, Sleipnir faces where he is going; otherwise Odin faces whatever he is looking at.
        Vec3d moved = getPositionVector().subtract(before);
        double flatMoved = Math.sqrt(moved.x * moved.x + moved.z * moved.z);
        if (watching != null) {
            if (watching.isDead) {
                watching = null;
            } else {
                focus = watching.getPositionEyes(1F);
            }
        }
        if (!facingLocked) {
            Vec3d toward = flatMoved > 0.18D ? getPositionVector().add(moved.scale(10D)) : focus;
            if (toward != null) {
                double dx = toward.x - posX, dz = toward.z - posZ;
                if (dx * dx + dz * dz > 1.0E-4) {
                    float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
                    renderYawOffset = approach(renderYawOffset, yaw, flatMoved > 0.18D ? 14F : 6F);
                    rotationYaw = renderYawOffset;
                }
            }
            if (focus != null) {
                double dx = focus.x - posX, dz = focus.z - posZ;
                rotationYawHead = approach(rotationYawHead, (float) Math.toDegrees(Math.atan2(-dx, dz)), 10F);
            } else {
                rotationYawHead = rotationYaw;
            }
        } else {
            renderYawOffset = rotationYawHead = rotationYaw;
        }
        if (flatMoved > 0.3D && ticksExisted % 4 == 0) {
            sound(SoundEvents.ENTITY_HORSE_GALLOP, 1.4F, 0.6F);
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

    private void syncFacing() {
        if (world.isRemote) {
            Rotations r = dataManager.get(FACING);
            rotationYaw += MathHelper.wrapDegrees(r.getY() - rotationYaw) * 0.6F;
            rotationYawHead += MathHelper.wrapDegrees(r.getZ() - rotationYawHead) * 0.6F;
            renderYawOffset = rotationYaw;
            return;
        }
        Rotations r = dataManager.get(FACING);
        if (Math.abs(MathHelper.wrapDegrees(r.getY() - rotationYaw)) > 0.5F || Math.abs(MathHelper.wrapDegrees(r.getZ() - rotationYawHead)) > 0.5F) {
            dataManager.set(FACING, new Rotations(0F, rotationYaw, rotationYawHead));
        }
    }

    /** Where he is, carried in his synced data: the tracker's small moves were not enough for the way he moves. */
    private void syncSpot() {
        Vec3d r = new Vec3d(dataManager.get(SPOT_X), dataManager.get(SPOT_Y), dataManager.get(SPOT_Z));
        if (world.isRemote) {
            if (r.y < -999D) {
                return;
            }
            double dx = r.x - posX, dy = r.y - posY, dz = r.z - posZ;
            double d = dx * dx + dy * dy + dz * dz;
            if (d > 64D) {
                setPosition(r.x, r.y, r.z);
                prevPosX = lastTickPosX = posX;
                prevPosY = lastTickPosY = posY;
                prevPosZ = lastTickPosZ = posZ;
            } else if (d > 1.0E-6) {
                setPosition(posX + dx * 0.6D, posY + dy * 0.6D, posZ + dz * 0.6D);
            }
            return;
        }
        if (Math.abs(r.x - posX) + Math.abs(r.y - posY) + Math.abs(r.z - posZ) > 0.002D) {
            dataManager.set(SPOT_X, (float) posX);
            dataManager.set(SPOT_Y, (float) posY);
            dataManager.set(SPOT_Z, (float) posZ);
        }
    }

    @Override
    public void setPositionAndRotationDirect(double x, double y, double z, float yaw, float pitch, int increments, boolean teleport) {
        if (dataManager.get(SPOT_Y) < -999F) {
            super.setPositionAndRotationDirect(x, y, z, yaw, pitch, increments, teleport);
        }
    }

    private static float approach(float from, float to, float max) {
        return from + MathHelper.clamp(MathHelper.wrapDegrees(to - from), -max, max);
    }

    // ------------------------------------------------------------------ the fight

    @Nullable
    private EntityLivingBase target() {
        EntityPlayer p = targetId == null ? null : world.getPlayerEntityByUUID(targetId);
        if (p != null && p.isEntityAlive() && !p.isSpectator() && p.getDistanceSq(this) < 96D * 96D) {
            return p;
        }
        EntityPlayer near = world.getClosestPlayerToEntity(this, 64D);
        if (near != null && near.isEntityAlive() && !near.isSpectator()) {
            targetId = near.getUniqueID();
            return near;
        }
        return null;
    }

    private void fightTick() {
        if (driven >= 0) {
            drivenTick();
            return;
        }
        EntityLivingBase target = target();
        if (target == null) {
            return;
        }
        if (attack == null) {
            EntityLivingBase on = onNow(target);
            circle(on, 11D, 0.05D);
            watch(on);
            if (holding) {
                nextAttack = Math.max(nextAttack, 20);
            }
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
            case CHARGE:
                charge(at);
                break;
            case THROW:
                throwSpear(at);
                break;
            case ODINFORCE:
                odinforce(at);
                break;
            case BIFROST:
                bifrost(at);
                break;
            case RAVENS:
                ravens(at);
                break;
            case STOMP:
                stomp(at);
                break;
        }
    }

    /** At a gallop round someone, on the air at the height of their feet. */
    private void circle(EntityLivingBase on, double r, double rate) {
        circle += 0.02F;
        Vec3d at = on.getPositionVector().add(Math.cos(circle) * r, 0D, Math.sin(circle) * r);
        hoverTo(new Vec3d(at.x, Math.max(groundUnder(at) + 0.2D, on.posY - 1D), at.z), rate);
        maxStep = 0.95D;
        if (pose() == Pose.STAND && getPositionVector().distanceTo(hoverAt) > 3D) {
            rest(Pose.GALLOP).perform(Pose.GALLOP);
        } else if (pose() == Pose.GALLOP && getPositionVector().distanceTo(hoverAt) < 1.5D) {
            rest(Pose.STAND).perform(Pose.STAND);
        }
    }

    /** Who he is on between attacks: whoever the last was at, if still about; else whoever holds it. */
    private EntityLivingBase onNow(EntityLivingBase target) {
        EntityLivingBase a = aimed;
        return a != null && a.isEntityAlive() && a.world == world && a.getDistanceSq(this) < 64D * 64D && fair(a) ? a : target;
    }

    /** Someone he can strike now. */
    private boolean fair(EntityLivingBase e) {
        if (e instanceof EntityPlayer) {
            return !((EntityPlayer) e).isSpectator();
        }
        if (e instanceof EntityMagus) {
            return !((EntityMagus) e).isAway();
        }
        return !(e instanceof EntityAdamWarlock && (((EntityAdamWarlock) e).isScripted() || ((EntityAdamWarlock) e).fallen()));
    }

    /**
     * Who the next attack is at. Whoever holds the gauntlet, most; the Magus nearly as often (he means to end them both);
     * Warlock now and then; and anyone who has been hurting him, the more the more they have.
     */
    private EntityLivingBase pick(EntityLivingBase target) {
        List<EntityLivingBase> who = new ArrayList<>();
        List<Float> weight = new ArrayList<>();
        who.add(target);
        weight.add(5F + threatOf(target));
        for (EntityPlayer p : world.playerEntities) {
            if (p != target && p.isEntityAlive() && fair(p) && p.getDistanceSq(this) < 48D * 48D) {
                who.add(p);
                weight.add(2F + threatOf(p));
            }
        }
        if (magus != null && magus.isEntityAlive() && magus.world == world && magus.getDistanceSq(this) < 64D * 64D && fair(magus)) {
            who.add(magus);
            weight.add(4F + threatOf(magus));
        }
        if (warlock != null && warlock.isEntityAlive() && warlock.world == world && warlock.getDistanceSq(this) < 48D * 48D && fair(warlock)) {
            who.add(warlock);
            weight.add(1F + Math.min(4F, threatOf(warlock)));
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

    /** What next: never the same twice running; the hooves when they are close, the spear and the Bifrost when not. */
    private void choose(EntityLivingBase target) {
        aimed = pick(target);
        double near = getDistance(aimed);
        int[] weight = new int[Attack.values().length];
        weight[Attack.CHARGE.ordinal()] = near > 8D ? 3 : 1;
        weight[Attack.THROW.ordinal()] = near > 9D ? 3 : 1;
        weight[Attack.ODINFORCE.ordinal()] = near < 15D ? 2 : 0;
        weight[Attack.BIFROST.ordinal()] = 3;
        weight[Attack.RAVENS.ordinal()] = ravens.isEmpty() && ravensCooldown <= 0 ? 2 : 0;
        weight[Attack.STOMP.ordinal()] = near < 7D ? 4 : 0;
        if (lastAttack != null) {
            weight[lastAttack.ordinal()] = 0;
        }
        int total = 0;
        for (int w : weight) {
            total += w;
        }
        int roll = rand.nextInt(Math.max(1, total));
        Attack next = Attack.BIFROST;
        for (Attack a : Attack.values()) {
            roll -= weight[a.ordinal()];
            if (roll < 0) {
                next = a;
                break;
            }
        }
        attack = next;
        attackTicks = 0;
        struck.clear();
    }

    private void done() {
        if (attack != null) {
            lastAttack = attack;
        }
        attack = null;
        nextAttack = 24 + rand.nextInt(22);
        rest(Pose.STAND);
        if (pose() != Pose.STAND && pose() != Pose.GALLOP) {
            perform(Pose.STAND);
        }
    }

    /** For testing: one attack (CHARGE, THROW, ODINFORCE, BIFROST, RAVENS, STOMP), now, at whoever he is after. */
    public String attackNow(String name) {
        EntityLivingBase t = target();
        if (t == null) {
            return "nobody to attack";
        }
        try {
            choose(t);
            attack = Attack.valueOf(name.toUpperCase());
            return attack.name() + " at " + aimed.getName();
        } catch (IllegalArgumentException e) {
            return "no such attack";
        }
    }

    // ------------------------------------------------------------------ the charge

    /** Sleipnir rears, and comes on at a dead run, Gungnir levelled, through them and on. */
    private void charge(EntityLivingBase t) {
        int s = attackTicks;
        if (s == 1) {
            hoverTo(getPositionVector(), 0.2D);
            watch(t);
            rest(Pose.STAND).perform(Pose.REAR, 18);
            sound(SoundEvents.ENTITY_HORSE_ANGRY, 3F, 0.5F);
            sound(SoundEvents.ENTITY_HORSE_BREATHE, 2F, 0.5F);
            return;
        }
        if (s == 18) {
            Vec3d from = getPositionVector();
            Vec3d aim = t.getPositionVector();
            chargeDir = aim.subtract(from).normalize();
            chargeTo = aim.add(new Vec3d(chargeDir.x, 0D, chargeDir.z).normalize().scale(9D));
            chargeTo = new Vec3d(chargeTo.x, Math.max(groundUnder(chargeTo) + 0.2D, aim.y - 0.5D), chargeTo.z);
            chargeEnd = s + (int) Math.ceil(from.distanceTo(chargeTo) / 1.2D) + 2;
            rest(Pose.CHARGE).perform(Pose.CHARGE);
            look(chargeTo.add(0D, 1D, 0D));
            sound(SoundEvents.ENTITY_HORSE_ANGRY, 3F, 0.7F);
            sound(EternitySounds.COMET_FLYBY, 4F, 0.6F);
        }
        if (s > 18 && s <= chargeEnd) {
            Vec3d to = chargeTo.subtract(getPositionVector());
            double d = to.length();
            Vec3d step = d < 1.2D ? to : to.scale(1.2D / d);
            setPosition(posX + step.x, posY + step.y, posZ + step.z);
            hoverAt = null;
            if (s % 3 == 0) {
                fxAt(MsgTribunalFx.ODIN_STOMP, getPositionVector(), 1.2D, 0D, 0D, 0, 1);
            }
            Vec3d front = getPositionVector().add(0D, 1.6D, 0D).add(new Vec3d(chargeDir.x, 0D, chargeDir.z).normalize().scale(1.4D));
            for (EntityLivingBase e : near(front, 2.4D)) {
                if (struck.add(e.getEntityId())) {
                    Vec3d away = flat(e.getPositionVector().subtract(getPositionVector()).add(chargeDir.scale(2D)));
                    fx(MsgTribunalFx.ODIN_SPEAR_HIT, e.getPositionVector().add(0D, e.height * 0.6D, 0D), Vec3d.ZERO, -1, 0);
                    sound(SoundEvents.ENTITY_IRONGOLEM_ATTACK, 2.5F, 0.5F);
                    sound(SoundEvents.ENTITY_GENERIC_EXPLODE, 1.5F, 1.2F);
                    harm(e, 0.26F, away, 1.8D, 0.7D);
                }
            }
        }
        if (s > chargeEnd) {
            hoverTo(getPositionVector(), 0.2D);
            done();
        }
    }

    // ------------------------------------------------------------------ Gungnir

    /** Gungnir drawn back and thrown: it turns after them for most of the way, then comes back to his hand. */
    private void throwSpear(EntityLivingBase t) {
        int s = attackTicks;
        if (s == 1) {
            hoverTo(getPositionVector(), 0.2D);
            faceNow(t.getPositionVector());
            rest(Pose.STAND).perform(Pose.THROW, 0);
            sound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 2F, 0.5F);
        }
        if (s < THROW_RELEASE) {
            faceNow(t.getPositionVector());
        }
        if (s == THROW_RELEASE) {
            Vec3d from = getPositionVector().add(0D, HAND_HEIGHT * SCALE * 0.95D, 0D).add(forwardOf().scale(0.6D));
            double pace = 1.7D;
            Vec3d aim = lead(t, from, pace);
            spear = new Spear(nextId++, from, aim.subtract(from).normalize().scale(pace), t);
            spear(false);
            fx(MsgTribunalFx.ODIN_SPEAR, from, spear.vel, spear.id, 0);
            sound(SoundEvents.ENTITY_WITHER_SHOOT, 2F, 0.6F);
            sound(SoundEvents.ENTITY_LIGHTNING_THUNDER, 1F, 1.8F);
            sound(EternitySounds.COMET_FLYBY, 3F, 1.4F);
        }
        if (s > THROW_RELEASE && spear == null && hasSpear()) {
            done();
        }
        if (s > 110) {
            spear = null;
            spear(true);
            done();
        }
    }

    private void tickSpear() {
        Spear sp = spear;
        if (sp == null) {
            return;
        }
        sp.age++;
        if (sp.returning) {
            Vec3d hand = getPositionVector().add(0D, HAND_HEIGHT * SCALE * 0.95D, 0D);
            Vec3d to = hand.subtract(sp.pos);
            double d = to.length();
            if (d < 2.2D || sp.age > 60) {
                spear = null;
                spear(true);
                perform(Pose.CATCH, 0);
                sound(SoundEvents.ENTITY_IRONGOLEM_ATTACK, 1.2F, 1.4F);
                sound(SoundEvents.BLOCK_ANVIL_LAND, 0.6F, 1.6F);
                return;
            }
            sp.vel = to.scale(Math.min(2.4D, d) / d);
            sp.pos = sp.pos.add(sp.vel);
            fx(MsgTribunalFx.ODIN_SPEAR, sp.pos, sp.vel, sp.id, 1);
            return;
        }
        EntityLivingBase t = sp.at;
        if (t != null && t.isEntityAlive() && sp.age < 16) {
            // It turns after them, until it is nearly there: get out of its way late, and it misses.
            Vec3d want = t.getPositionVector().add(0D, t.height * 0.55D, 0D).subtract(sp.pos).normalize();
            sp.vel = sp.vel.normalize().scale(0.82D).add(want.scale(0.18D)).normalize().scale(1.7D);
        }
        Vec3d next = sp.pos.add(sp.vel);
        boolean hit = sp.age > 45;
        RayTraceResult wall = world.rayTraceBlocks(sp.pos, next, false, true, false);
        if (wall != null && wall.typeOfHit == RayTraceResult.Type.BLOCK) {
            next = wall.hitVec;
            hit = true;
        }
        EntityLivingBase struckOne = null;
        for (EntityLivingBase e : near(sp.pos, 6D)) {
            Vec3d body = e.getPositionVector().add(0D, e.height * 0.5D, 0D);
            if (closest(sp.pos, next, body).distanceTo(body) < 0.8D + e.width * 0.4D) {
                hit = true;
                struckOne = e;
            }
        }
        sp.pos = next;
        fx(MsgTribunalFx.ODIN_SPEAR, sp.pos, sp.vel, sp.id, 0);
        if (hit) {
            fxAt(MsgTribunalFx.ODIN_SPEAR_HIT, sp.pos, 1.6D, 0D, 0D, sp.id, 0);
            world.playSound(null, sp.pos.x, sp.pos.y, sp.pos.z, SoundEvents.ENTITY_LIGHTNING_THUNDER, SoundCategory.HOSTILE, 2F, 1.3F);
            world.playSound(null, sp.pos.x, sp.pos.y, sp.pos.z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 2F, 0.8F);
            if (struckOne != null) {
                harm(struckOne, 0.28F, flat(sp.vel), 1.5D, 0.5D);
            }
            for (EntityLivingBase e : near(sp.pos, 3D)) {
                if (e != struckOne) {
                    harm(e, 0.1F, flat(e.getPositionVector().subtract(sp.pos)), 0.8D, 0.4D);
                }
            }
            sp.returning = true;
            sp.age = 0;
        }
    }

    // ------------------------------------------------------------------ the Odinforce

    /** Gungnir raised, the power gathering on the blade; then rammed into the ground, and a ring of it goes out. */
    private void odinforce(EntityLivingBase t) {
        int s = attackTicks;
        if (s == 1) {
            Vec3d at = getPositionVector();
            hoverTo(new Vec3d(at.x, groundUnder(at) + 0.05D, at.z), 0.2D);
            watch(t);
            rest(Pose.STAND).perform(Pose.CALL, 0);
            fxAt(MsgTribunalFx.ODIN_GATHER, bladeAt(), 0D, 0D, 0D, getEntityId(), 26);
            sound(SoundEvents.BLOCK_END_PORTAL_FRAME_FILL, 2F, 0.5F);
            sound(SoundEvents.ENTITY_LIGHTNING_THUNDER, 1.5F, 0.6F);
        }
        if (s == 26) {
            perform(Pose.SLAM, 0);
        }
        if (s == 26 + SLAM_HIT) {
            Vec3d at = getPositionVector().add(forwardOf().scale(1.5D));
            ringAt = new Vec3d(at.x, groundUnder(at.add(0D, 1D, 0D)), at.z);
            ringAge = 0;
            struck.clear();
            fxAt(MsgTribunalFx.ODIN_RING, ringAt, 17D, 0D, 0D, getEntityId(), 18);
            fxAt(MsgTribunalFx.ODIN_FLARE, ringAt.add(0D, 0.5D, 0D), 2.5D, 0D, 0D, -1, 0);
            sound(SoundEvents.ENTITY_GENERIC_EXPLODE, 3F, 0.5F);
            sound(SoundEvents.ENTITY_LIGHTNING_THUNDER, 3F, 0.7F);
            sound(EternitySounds.RIFT_IMPACT, 2F, 0.5F);
        }
        if (s >= 26 + SLAM_HIT + 22) {
            done();
        }
    }

    private void tickRing() {
        if (ringAge < 0 || ringAt == null) {
            return;
        }
        ringAge++;
        double r = 1D + ringAge / 18D * 16D;
        for (EntityLivingBase e : near(ringAt, r + 2D)) {
            double dx = e.posX - ringAt.x, dz = e.posZ - ringAt.z;
            double across = Math.sqrt(dx * dx + dz * dz);
            boolean low = e.posY - ringAt.y < 1.4D && e.posY - ringAt.y > -2D;
            boolean grounded = !(e instanceof EntityPlayer) || e.onGround;
            if (across > r - 1.5D && across < r + 0.8D && low && grounded && struck.add(e.getEntityId())) {
                harm(e, 0.18F, flat(new Vec3d(dx, 0D, dz)), 1.3D, 0.8D);
            }
        }
        if (ringAge >= 18) {
            ringAge = -1;
        }
    }

    // ------------------------------------------------------------------ the Bifrost

    /** Gungnir raised to the sky: rings of light on the ground round them, and the Bifrost down on each in turn. */
    private void bifrost(EntityLivingBase t) {
        int s = attackTicks;
        if (s == 1) {
            hoverTo(getPositionVector(), 0.2D);
            watch(t);
            rest(Pose.STAND).perform(Pose.CALL, 0);
            sound(SoundEvents.BLOCK_PORTAL_TRIGGER, 0.7F, 1.6F);
            sound(SoundEvents.ENTITY_LIGHTNING_THUNDER, 2F, 0.5F);
            List<Vec3d> spots = new ArrayList<>();
            Vec3d feet = t.getPositionVector();
            Vec3d going = new Vec3d(t.posX - t.prevPosX, 0D, t.posZ - t.prevPosZ).scale(14D);
            spots.add(feet);
            spots.add(feet.add(going).add(rand.nextGaussian() * 2.5D, 0D, rand.nextGaussian() * 2.5D));
            spots.add(feet.add(rand.nextGaussian() * 4D, 0D, rand.nextGaussian() * 4D));
            if (magus != null && magus != t && magus.isEntityAlive() && fair(magus) && magus.getDistance(this) < 48D) {
                spots.add(magus.getPositionVector());
            } else {
                spots.add(feet.add(rand.nextGaussian() * 5D, 0D, rand.nextGaussian() * 5D));
            }
            for (int i = 0; i < spots.size(); i++) {
                Vec3d at = spots.get(i);
                Vec3d ground = new Vec3d(at.x, groundUnder(at.add(0D, 2D, 0D)), at.z);
                int strikeAt = s + 28 + i * 8;
                marks.add(new Mark(ground, strikeAt + ticksExisted - s));
                fxAt(MsgTribunalFx.ODIN_MARK, ground, 2.6D, 0D, 0D, -1, strikeAt - s);
            }
        }
        if (s >= 28 + 4 * 8 + 12) {
            done();
        }
    }

    private void tickMarks() {
        for (Mark m : new ArrayList<>(marks)) {
            if (ticksExisted < m.strikeAt) {
                continue;
            }
            marks.remove(m);
            fxAt(MsgTribunalFx.ODIN_STRIKE, m.at, 2.6D, 0D, 0D, -1, 0);
            world.playSound(null, m.at.x, m.at.y, m.at.z, SoundEvents.ENTITY_LIGHTNING_THUNDER, SoundCategory.HOSTILE, 3F, 1.1F);
            world.playSound(null, m.at.x, m.at.y, m.at.z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 2F, 0.7F);
            world.playSound(null, m.at.x, m.at.y, m.at.z, SoundEvents.EVOCATION_ILLAGER_CAST_SPELL, SoundCategory.HOSTILE, 2F, 1.4F);
            for (EntityLivingBase e : near(m.at, 3.4D)) {
                double dx = e.posX - m.at.x, dz = e.posZ - m.at.z;
                if (dx * dx + dz * dz < 2.8D * 2.8D && e.posY - m.at.y < 8D && e.posY - m.at.y > -2D) {
                    harm(e, 0.22F, flat(new Vec3d(dx, 0D, dz)), 0.9D, 1.1D);
                }
            }
        }
    }

    // ------------------------------------------------------------------ Huginn and Muninn

    /** Gungnir levelled at them, and his two ravens loosed: they come at the eyes, again and again, and go home. */
    private void ravens(EntityLivingBase t) {
        int s = attackTicks;
        if (s == 1) {
            hoverTo(getPositionVector(), 0.2D);
            faceNow(t.getPositionVector());
            rest(Pose.STAND).perform(Pose.POINT, 26);
            sound(SoundEvents.ENTITY_PARROT_AMBIENT, 2F, 0.45F);
        }
        if (s == 8) {
            Vec3d forward = forwardOf(), right = new Vec3d(-forward.z, 0D, forward.x);
            for (int side = -1; side <= 1; side += 2) {
                Vec3d from = getPositionVector().add(0D, 3.3D * SCALE, 0D).add(right.scale(0.7D * side));
                Raven r = new Raven(nextId++, from, right.scale(0.4D * side).add(0D, 0.5D, 0D).add(forward.scale(0.3D)), t);
                ravens.add(r);
                fx(MsgTribunalFx.ODIN_RAVEN, from, r.vel, r.id, 0);
            }
            sound(SoundEvents.ENTITY_BAT_TAKEOFF, 2F, 0.4F);
            sound(SoundEvents.ENTITY_PARROT_HURT, 2F, 0.4F);
            ravensCooldown = 260;
        }
        if (s >= 30) {
            done();
        }
    }

    private void tickRavens() {
        for (Raven r : new ArrayList<>(ravens)) {
            r.age++;
            EntityLivingBase t = r.at;
            boolean home = r.pecks >= 3 || r.age > 160 || !t.isEntityAlive() || t.world != world;
            Vec3d goal;
            if (home) {
                goal = getPositionVector().add(0D, 3.4D * SCALE, 0D);
                if (goal.distanceTo(r.pos) < 1.5D) {
                    ravens.remove(r);
                    fxAt(MsgTribunalFx.ODIN_RAVEN, r.pos, 0D, 0D, 0D, r.id, 2);
                    continue;
                }
            } else if (r.away > 0) {
                // Wheeling off after a strike, to come again.
                r.away--;
                goal = t.getPositionVector().add(Math.cos(r.age * 0.3D + r.id) * 5D, 3.5D, Math.sin(r.age * 0.3D + r.id) * 5D);
            } else {
                goal = t.getPositionVector().add(0D, t.getEyeHeight(), 0D);
            }
            Vec3d want = goal.subtract(r.pos);
            double d = want.length();
            double pace = home ? 1.1D : 0.85D;
            r.vel = r.vel.scale(0.8D).add(want.scale(pace * 0.2D / Math.max(0.01D, d)));
            if (r.vel.length() > pace) {
                r.vel = r.vel.normalize().scale(pace);
            }
            r.pos = r.pos.add(r.vel);
            boolean peck = !home && r.away <= 0 && t.getPositionVector().add(0D, t.getEyeHeight(), 0D).distanceTo(r.pos) < 1.3D;
            fx(MsgTribunalFx.ODIN_RAVEN, r.pos, r.vel, r.id, peck ? 1 : 0);
            if (peck) {
                r.pecks++;
                r.away = 22;
                world.playSound(null, r.pos.x, r.pos.y, r.pos.z, SoundEvents.ENTITY_PARROT_HURT, SoundCategory.HOSTILE, 1.5F, 0.4F);
                harm(t, 0.04F, flat(r.vel), 0.2D, 0.05D);
                if (t instanceof EntityPlayer) {
                    t.addPotionEffect(new PotionEffect(MobEffects.BLINDNESS, 30, 0, false, false));
                }
            }
        }
    }

    // ------------------------------------------------------------------ Sleipnir

    /** Close: Sleipnir rears high, and brings all eight hooves down. */
    private void stomp(EntityLivingBase t) {
        int s = attackTicks;
        if (s == 1) {
            hoverTo(getPositionVector(), 0.2D);
            faceNow(t.getPositionVector());
            rest(Pose.STAND).perform(Pose.REAR, 16);
            sound(SoundEvents.ENTITY_HORSE_ANGRY, 3F, 0.45F);
        }
        if (s == 16) {
            perform(Pose.STOMP, 0);
        }
        if (s == 16 + STOMP_HIT) {
            Vec3d at = getPositionVector().add(forwardOf().scale(2.2D));
            Vec3d ground = new Vec3d(at.x, groundUnder(at.add(0D, 1D, 0D)), at.z);
            fxAt(MsgTribunalFx.ODIN_STOMP, ground, 6D, 0D, 0D, 0, 0);
            sound(SoundEvents.ENTITY_GENERIC_EXPLODE, 3F, 0.6F);
            sound(SoundEvents.BLOCK_ANVIL_LAND, 2F, 0.4F);
            sound(SoundEvents.ENTITY_HORSE_LAND, 3F, 0.5F);
            for (EntityLivingBase e : near(ground, 6.5D)) {
                double d = e.getDistance(ground.x, ground.y, ground.z);
                harm(e, d < 3.2D ? 0.24F : 0.12F, flat(e.getPositionVector().subtract(ground)), d < 3.2D ? 1.4D : 0.8D, d < 3.2D ? 1D : 0.6D);
            }
        }
        if (s >= 16 + STOMP_TICKS + 6) {
            done();
        }
    }

    // ------------------------------------------------------------------ driven back

    /** His health run out: Sleipnir rears, and he goes back a way, and gathers himself, and comes again at half. */
    private void drivenTick() {
        driven++;
        if (driven == 1) {
            clearShots();
            attack = null;
            drivenFrom = getPositionVector();
            EntityLivingBase t = target();
            Vec3d away = t != null ? flat(getPositionVector().subtract(t.getPositionVector())) : forwardOf().scale(-1D);
            drivenTo = drivenFrom.add(away.scale(22D)).add(0D, 6D, 0D);
            rest(Pose.GALLOP).perform(Pose.REAR, 20);
            sound(SoundEvents.ENTITY_HORSE_ANGRY, 3F, 0.6F);
            String[] lines = {"Good.", "You have teeth.", "Again, then."};
            StoryDirector.shout(this, Speakers.ODIN, getEntityId(), lines[rand.nextInt(lines.length)], 128D);
        }
        if (driven > 20 && driven <= 50) {
            float k = (driven - 20) / 30F;
            k = k * k * (3F - 2F * k);
            place(drivenFrom.add(drivenTo.subtract(drivenFrom).scale(k)), null);
        }
        if (driven == 50) {
            rest(Pose.STAND).perform(Pose.CALL, 0);
            fxAt(MsgTribunalFx.ODIN_GATHER, bladeAt(), 0D, 0D, 0D, getEntityId(), 60);
            sound(SoundEvents.BLOCK_END_PORTAL_FRAME_FILL, 2F, 0.4F);
        }
        if (driven > 50) {
            setHealth(Math.max(getHealth(), getMaxHealth() * 0.5F * Math.min(1F, (driven - 50) / 70F)));
        }
        if (driven >= 125) {
            driven = -1;
            nextAttack = 10;
            rest(Pose.STAND).perform(Pose.STAND);
        }
    }

    // ------------------------------------------------------------------ striking and being struck

    /** Whoever any of it can touch near a point: players, the Magus, Warlock. */
    private List<EntityLivingBase> near(Vec3d at, double reach) {
        List<EntityLivingBase> all = new ArrayList<>();
        for (EntityLivingBase e : world.getEntitiesWithinAABB(EntityLivingBase.class, new AxisAlignedBB(at, at).grow(reach + 2D))) {
            if (e == this || !e.isEntityAlive() || e.getDistance(at.x, at.y, at.z) > reach + e.width) {
                continue;
            }
            if (e instanceof EntityPlayer ? !((EntityPlayer) e).isSpectator() : e instanceof EntityMagus || e instanceof EntityAdamWarlock) {
                if (fair(e)) {
                    all.add(e);
                }
            }
        }
        return all;
    }

    /**
     * Struck by something of his: a share of their health, and thrown. The Magus takes it on his own health (it can
     * break him, as anyone's can); Warlock he only knocks about.
     */
    private void harm(EntityLivingBase e, float share, Vec3d away, double strength, double up) {
        if (e instanceof EntityMagus) {
            e.attackEntityFrom(EternityDamage.wielded("odin", this), e.getMaxHealth() * share);
            return;
        }
        if (e instanceof EntityAdamWarlock) {
            ((EntityAdamWarlock) e).knocked(away, strength, 0.8D);
            return;
        }
        if (!(e instanceof EntityPlayer) || ((EntityPlayer) e).isSpectator()) {
            return;
        }
        EntityPlayer player = (EntityPlayer) e;
        EntityDestroyerArmor.strike(player, EternityDamage.wielded("odin", this), share * (float) EternityConfig.odin.damage);
        if (!player.isEntityAlive()) {
            return;
        }
        if (up >= 0D) {
            player.motionY = Math.max(player.motionY, 0D);
        }
        player.fallDistance = 0F;
        EntityDestroyerArmor.shove(player, away.x * strength, up, away.z * strength);
    }

    /**
     * Blows land, a little at a time. Whoever lands them draws his eye. Run his health out and he is driven back; he
     * never dies of it.
     */
    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (world.isRemote || source == DamageSource.OUT_OF_WORLD) {
            return source == DamageSource.OUT_OF_WORLD && super.attackEntityFrom(source, amount);
        }
        Entity by = source.getTrueSource();
        boolean player = by instanceof EntityPlayerMP && !((EntityPlayerMP) by).isSpectator();
        boolean other = by instanceof EntityMagus || by instanceof EntityAdamWarlock;
        if (!fighting || guarded || driven >= 0 || !(player || other) || hurtResistantTime > 10) {
            return false;
        }
        hurtResistantTime = 20;
        world.setEntityState(this, (byte) 2);
        sound(SoundEvents.BLOCK_ANVIL_LAND, 0.5F, 1.4F);
        float dealt = Math.min(player ? amount : amount * (float) EternityConfig.odin.otherShare,
                getMaxHealth() * (float) EternityConfig.odin.maxHitFraction);
        threat.merge(by.getEntityId(), player ? 2.5F : 2F, Float::sum);
        if (!(dealt > 0F)) {
            return true;
        }
        float left = getHealth() - dealt;
        if (left <= 1F) {
            setHealth(1F);
            driven = 0;
        } else {
            setHealth(left);
        }
        bossInfo.setPercent(getHealth() / getMaxHealth());
        return true;
    }

    @Override
    public boolean isEntityInvulnerable(DamageSource source) {
        return !fighting && source != DamageSource.OUT_OF_WORLD;
    }

    @Override
    public void onDeath(DamageSource cause) {
        if (source(cause)) {
            super.onDeath(cause);
            return;
        }
        setHealth(1F);
    }

    /** Only the void, or a command, ends him. */
    private static boolean source(DamageSource cause) {
        return cause == DamageSource.OUT_OF_WORLD;
    }

    @Override
    public void onKillCommand() {
        setDead();
    }

    @Override
    public void addTrackingPlayer(EntityPlayerMP player) {
        super.addTrackingPlayer(player);
        bossInfo.addPlayer(player);
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

    // ------------------------------------------------------------------ geometry

    /** The top of whatever is solid under a point (or the point itself, if nothing is within forty blocks). */
    private double groundUnder(Vec3d at) {
        RayTraceResult hit = world.rayTraceBlocks(at.add(0D, 0.5D, 0D), at.subtract(0D, 40D, 0D), false, true, false);
        return hit != null && hit.typeOfHit == RayTraceResult.Type.BLOCK ? hit.hitVec.y : at.y;
    }

    private static Vec3d lead(EntityLivingBase t, Vec3d from, double pace) {
        Vec3d at = t.getPositionVector().add(0D, t.height * 0.55D, 0D);
        double flight = at.distanceTo(from) / pace;
        return at.add(new Vec3d(t.posX - t.prevPosX, 0D, t.posZ - t.prevPosZ).scale(Math.min(flight, 30D) * 0.6D));
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

    public Vec3d forwardOf() {
        float yaw = (float) Math.toRadians(rotationYaw);
        return new Vec3d(-MathHelper.sin(yaw), 0D, MathHelper.cos(yaw));
    }

    private void fx(byte type, Vec3d at, Vec3d dir, int id, int time) {
        EternityNetwork.sendToDimension(new MsgTribunalFx(type, at.x, at.y, at.z, dir.x, dir.y, dir.z, id, time), dimension);
    }

    private void fxAt(byte type, Vec3d at, double a, double b, double c, int id, int time) {
        EternityNetwork.sendToDimension(new MsgTribunalFx(type, at.x, at.y, at.z, a, b, c, id, time), dimension);
    }

    private void sound(SoundEvent sound, float volume, float pitch) {
        world.playSound(null, posX, posY + 1.5D, posZ, sound, SoundCategory.HOSTILE, volume, pitch);
    }

    // ------------------------------------------------------------------ client

    private void clientTick() {
        clientPoseTicks++;
        double vx = posX - prevPosX, vy = posY - prevPosY, vz = posZ - prevPosZ;
        velX += (vx - velX) * 0.5D;
        velY += (vy - velY) * 0.5D;
        velZ += (vz - velZ) * 0.5D;
        double speed = Math.sqrt(velX * velX + velZ * velZ);
        Pose p = pose();
        boolean runs = p == Pose.GALLOP || p == Pose.CHARGE || p == Pose.DESCEND;
        prevRunning = running;
        running += ((runs || speed > 0.25D ? 1F : 0F) - running) * 0.15F;
        prevGait = gait;
        gait += (float) (0.32D + Math.min(0.45D, speed * 0.4D)) * Math.max(0.15F, running);
        prevBodyPitch = bodyPitch;
        float wantPitch = p == Pose.DESCEND ? 22F : (float) MathHelper.clamp(-vy * 25D, -14D, 14D);
        bodyPitch += (wantPitch - bodyPitch) * 0.12F;
    }

    // ------------------------------------------------------------------ the rest

    @Override
    public boolean isNonBoss() {
        return false;
    }

    @Override
    public boolean canBePushed() {
        return false;
    }

    @Override
    protected void collideWithEntity(Entity entity) {
    }

    @Override
    public void fall(float distance, float damageMultiplier) {
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
        return getEntityBoundingBox().grow(4D, 3D, 4D);
    }

    @Override
    public boolean isEntityInsideOpaqueBlock() {
        return false;
    }

    @Override
    public float getEyeHeight() {
        return 3.3F;
    }
}
