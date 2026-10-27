package com.matoon.eternity.entity;

import com.matoon.eternity.EternityConfig;
import com.matoon.eternity.EternitySounds;
import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.network.MsgTribunalFx;
import com.matoon.eternity.world.VaultDirector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.BossInfo;
import net.minecraft.world.BossInfoServer;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * The Destroyer: the deadliest thing in Odin's Vault, an enchanted automaton of banded Uru that
 * stands in its niche at the far end of the hall until something wakes it. Nearly four blocks of
 * dark metal, spikes down its shoulders and arms, a tall narrow head under a folded crown, and a
 * furnace behind its face.
 * <ul>
 * <li><b>Punch</b> and <b>backhand</b>: slow, enormous, and they send you across the hall.</li>
 * <li><b>Hammer</b>: both fists locked over its head and brought down; rings of force race out across the floor.</li>
 * <li><b>Stomp</b>: a foot lifted and driven into the stone at anything too close to its feet.</li>
 * <li><b>Ray</b>: its face opens, the furnace behind it gathers, and it pours out a ray of
 * disintegrating fire that follows whoever it is looking at.</li>
 * <li><b>Sweep</b>: the same ray, swung across the hall at chest height.</li>
 * </ul>
 * Almost nothing hurts it. Only a blow from someone wielding something worthy of it (the Power
 * Stone, Mjolnir, a charged Astral Regulator) really bites; anything else barely scratches it.
 * Beaten, it falls to its knees and then on its face, and the fire in it goes out.
 */
public class EntityDestroyer extends EntityLiving {

    public enum State {
        DORMANT, WAKE, STALK,
        PUNCH, BACKHAND, HAMMER, STOMP,
        RAY, SWEEP,
        STAGGER, FALL, WRECK
    }

    /** What it can be told to do, for testing. */
    public enum Move { PUNCH, BACKHAND, HAMMER, STOMP, RAY, SWEEP }

    /** How much bigger than its model it is drawn; its blows and its ray are placed to match. */
    public static final float SCALE = 1.6F;

    // How long everything takes, in ticks; the model times its frames to the same numbers.
    public static final int WAKE_GLOW = 18;
    public static final int WAKE_LOOK = 34;
    public static final int WAKE_STEP = 52;
    public static final int WAKE_TICKS = 74;
    public static final int PUNCH_HIT = 17;
    public static final int PUNCH_TICKS = 34;
    public static final int BACKHAND_HIT = 16;
    public static final int BACKHAND_TICKS = 34;
    public static final int HAMMER_HIT = 25;
    public static final int HAMMER_TICKS = 48;
    public static final int STOMP_HIT = 14;
    public static final int STOMP_TICKS = 30;
    /** The ray: its face opening, the furnace gathering, the ray pouring out, its face closing. */
    public static final int RAY_OPEN = 14;
    public static final int RAY_FIRE = 36;
    public static final int RAY_END = 96;
    public static final int RAY_TICKS = 118;
    public static final int SWEEP_FIRE = 32;
    public static final int SWEEP_END = 76;
    public static final int SWEEP_TICKS = 96;
    public static final int STAGGER_TICKS = 30;
    public static final int FALL_KNEE = 22;
    public static final int FALL_TICKS = 70;

    private static final DataParameter<Byte> STATE = EntityDataManager.createKey(EntityDestroyer.class, DataSerializers.BYTE);
    private static final DataParameter<Float> AIM_YAW = EntityDataManager.createKey(EntityDestroyer.class, DataSerializers.FLOAT);

    private final BossInfoServer bossInfo = (BossInfoServer) new BossInfoServer(
            new TextComponentString("The Destroyer"), BossInfo.Color.WHITE, BossInfo.Overlay.NOTCHED_12);

    // ------------------------------------------------------------------ server state
    @Nullable
    private UUID targetId;
    private int stateTicks;
    private int cooldown = 20;
    private int rayCooldown = 60;
    private int strideTicks;
    private boolean leftFoot;
    /** Pulls its blows: nothing it does harms anyone. */
    private boolean calm;
    @Nullable
    private Move pendingMove;
    private Vec3d aim;
    private float sweepFrom;
    private int hitStop;
    private double homeX, homeY, homeZ;
    private float homeYaw;

    // ------------------------------------------------------------------ client state
    /** Client: ticks since the synced state last changed. */
    public int clientStateTicks;
    /** Client: how hot the furnace in its face burns, 0 to 1, and how far its face plates stand open. */
    public float heat, prevHeat, open, prevOpen;
    /** Client: the pose it was in when its state last changed, which the model blends out of; and the pose last drawn. */
    public final float[] poseFrom = new float[40], poseLast = new float[40];

    public EntityDestroyer(World world) {
        super(world);
        setSize(1.6F, 3.9F);
        stepHeight = 1.1F;
        isImmuneToFire = true;
        experienceValue = 200;
        ignoreFrustumCheck = true;
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(STATE, (byte) State.DORMANT.ordinal());
        dataManager.register(AIM_YAW, 0F);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(EternityConfig.vault.destroyerHealth);
        getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(1D);
    }

    @Override
    public void notifyDataManagerChange(DataParameter<?> key) {
        super.notifyDataManagerChange(key);
        if (STATE.equals(key) && world.isRemote) {
            clientStateTicks = 0;
            System.arraycopy(poseLast, 0, poseFrom, 0, poseLast.length);
        }
    }

    // ------------------------------------------------------------------ accessors and handles

    public State getState() {
        int ordinal = dataManager.get(STATE);
        return ordinal >= 0 && ordinal < State.values().length ? State.values()[ordinal] : State.STALK;
    }

    private void setState(State state) {
        dataManager.set(STATE, (byte) state.ordinal());
        stateTicks = 0;
    }

    /** Where, relative to its body, its head is turned to aim the ray, in degrees. */
    public float getAimYaw() {
        return dataManager.get(AIM_YAW);
    }

    /** Stands it in its niche, dormant, the way it is found. */
    public void install(double x, double y, double z, float yaw) {
        homeX = x;
        homeY = y;
        homeZ = z;
        homeYaw = yaw;
        setLocationAndAngles(x, y, z, yaw, 0F);
        rotationYawHead = renderYawOffset = yaw;
        setHealth(getMaxHealth());
        targetId = null;
        setState(State.DORMANT);
    }

    /** It wakes, now. */
    public void wake() {
        if (getState() == State.DORMANT) {
            setState(State.WAKE);
            EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.DESTROYER_WAKE, posX, posY, posZ, 0D, 0D, 0D, getEntityId(), WAKE_TICKS),
                    dimension);
            sound(SoundEvents.BLOCK_PISTON_EXTEND, 3F, 0.3F);
            sound(SoundEvents.BLOCK_IRON_DOOR_OPEN, 3F, 0.4F);
        }
    }

    public boolean isAwake() {
        State state = getState();
        return state != State.DORMANT && state != State.FALL && state != State.WRECK;
    }

    public boolean isFallen() {
        State state = getState();
        return state == State.FALL || state == State.WRECK;
    }

    public void setCalm(boolean calm) {
        this.calm = calm;
    }

    public boolean isCalm() {
        return calm;
    }

    public void order(Move move) {
        pendingMove = move;
        if (getState() == State.STALK) {
            cooldown = 0;
            rayCooldown = 0;
        }
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void onLivingUpdate() {
        if (!world.isRemote) {
            serverTick();
        }
        super.onLivingUpdate();
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        renderYawOffset = rotationYaw;
        prevRenderYawOffset = prevRotationYaw;
        if (world.isRemote) {
            clientTick();
        }
    }

    @Override
    public void travel(float strafe, float vertical, float forward) {
        if (!world.isRemote) {
            move(MoverType.SELF, motionX, motionY, motionZ);
            motionY = onGround ? -0.08D : Math.max(-2.5D, motionY - 0.1D);
            motionX *= 0.5D;
            motionZ *= 0.5D;
        }
        EntityEinherjar.stride(this);
    }

    private void serverTick() {
        stateTicks++;
        if (cooldown > 0) {
            cooldown--;
        }
        if (rayCooldown > 0) {
            rayCooldown--;
        }
        if (ticksExisted % 5 == 0) {
            bossInfo.setPercent(getHealth() / getMaxHealth());
            bossInfo.setVisible(isAwake() || getState() == State.FALL);
        }
        if (hitStop > 0) {
            hitStop--;
            stateTicks--;
            motionX = motionZ = 0D;
            return;
        }
        EntityPlayer target = target();
        switch (getState()) {
            case DORMANT:
                dormant();
                break;
            case WAKE:
                waking(target);
                break;
            case STALK:
                stalking(target);
                break;
            case PUNCH:
                punching(target);
                break;
            case BACKHAND:
                backhanding(target);
                break;
            case HAMMER:
                hammering(target);
                break;
            case STOMP:
                stomping(target);
                break;
            case RAY:
                ray(target);
                break;
            case SWEEP:
                sweep(target);
                break;
            case STAGGER:
                if (stateTicks >= STAGGER_TICKS) {
                    setState(State.STALK);
                    cooldown = 10;
                }
                break;
            case FALL:
                falling();
                break;
            case WRECK:
                motionX = motionZ = 0D;
                break;
        }
    }

    @Nullable
    private EntityPlayer target() {
        if (targetId != null) {
            EntityPlayer player = world.getPlayerEntityByUUID(targetId);
            if (player != null && isFair(player) && player.getDistanceSq(this) < 96D * 96D) {
                return player;
            }
            targetId = null;
        }
        EntityPlayer best = null;
        double bestDistance = 96D * 96D;
        for (EntityPlayer player : world.playerEntities) {
            double d = player.getDistanceSq(this);
            if (isFair(player) && d < bestDistance) {
                best = player;
                bestDistance = d;
            }
        }
        if (best != null) {
            targetId = best.getUniqueID();
        }
        return best;
    }

    private static boolean isFair(EntityPlayer player) {
        return !player.isDead && !player.isSpectator() && player.getHealth() > 0F;
    }

    // ------------------------------------------------------------------ dormant and waking

    /** Unmoving in its niche. It does not even turn its head. */
    private void dormant() {
        motionX = motionZ = 0D;
        if (homeX != 0D || homeZ != 0D) {
            rotationYaw = homeYaw;
            rotationYawHead = homeYaw;
        }
    }

    /** A shudder through every plate, fire kindling behind its face, its head coming round to you, and a first step. */
    private void waking(@Nullable EntityPlayer target) {
        int t = stateTicks;
        if (t == WAKE_GLOW) {
            sound(SoundEvents.ENTITY_BLAZE_AMBIENT, 3F, 0.4F);
            sound(SoundEvents.BLOCK_FIRE_AMBIENT, 3F, 0.5F);
        }
        if (t > WAKE_LOOK && target != null) {
            face(target, 0.08F);
        }
        if (t == WAKE_STEP) {
            footfall();
            step(0.5D);
        }
        if (t >= WAKE_TICKS) {
            setState(State.STALK);
            cooldown = 10;
        }
    }

    // ------------------------------------------------------------------ stalking

    /** Toward you, unhurried; every footfall shakes the floor. Then it chooses. */
    private void stalking(@Nullable EntityPlayer target) {
        if (target == null) {
            motionX = motionZ = 0D;
            return;
        }
        face(target, 0.12F);
        double d = horizontal(target);
        boolean walking = d > 3.2D;
        if (walking) {
            Vec3d to = flat(target.getPositionVector().subtract(getPositionVector()));
            double speed = d > 16D ? 0.15D : 0.11D;
            motionX = to.x * speed;
            motionZ = to.z * speed;
            if (++strideTicks % 13 == 0) {
                footfall();
            }
        }
        if (cooldown > 0) {
            return;
        }
        if (pendingMove != null) {
            start(pendingMove);
            pendingMove = null;
            return;
        }
        int roll = rand.nextInt(100);
        if (d <= 4.2D) {
            if (d < 2.6D && roll < 22) {
                start(Move.STOMP);
            } else if (roll < 55) {
                start(Move.PUNCH);
            } else if (roll < 78) {
                start(Move.BACKHAND);
            } else {
                start(Move.HAMMER);
            }
        } else if (rayCooldown <= 0 && canEntityBeSeen(target)) {
            start(roll < 65 ? Move.RAY : Move.SWEEP);
        } else if (d < 9D && roll < 6) {
            start(Move.HAMMER);
        }
    }

    private void start(Move move) {
        aim = null;
        switch (move) {
            case PUNCH:
                setState(State.PUNCH);
                break;
            case BACKHAND:
                setState(State.BACKHAND);
                break;
            case HAMMER:
                setState(State.HAMMER);
                break;
            case STOMP:
                setState(State.STOMP);
                break;
            case RAY:
                setState(State.RAY);
                rayCooldown = 150 + rand.nextInt(80);
                break;
            case SWEEP:
                setState(State.SWEEP);
                rayCooldown = 150 + rand.nextInt(80);
                break;
        }
    }

    private void recover(int ticks) {
        setState(State.STALK);
        cooldown = ticks + rand.nextInt(14);
        dataManager.set(AIM_YAW, 0F);
    }

    // ------------------------------------------------------------------ hand to hand

    /** The right fist drawn back to its ear, then through you. */
    private void punching(@Nullable EntityPlayer target) {
        int t = stateTicks;
        if (target != null && t < PUNCH_HIT - 4) {
            face(target, 0.2F);
        }
        if (t == 4) {
            sound(SoundEvents.ENTITY_IRONGOLEM_ATTACK, 2F, 0.45F);
        }
        if (t == PUNCH_HIT - 3) {
            step(0.6D);
        }
        if (t == PUNCH_HIT) {
            blow(4.2D, 0.18F, facing(), 2.2D, 0.45D, 0);
        }
        if (t >= PUNCH_TICKS) {
            recover(14);
        }
    }

    /** The left arm swung back across its body, the back of the fist leading. */
    private void backhanding(@Nullable EntityPlayer target) {
        int t = stateTicks;
        if (target != null && t < BACKHAND_HIT - 4) {
            face(target, 0.2F);
        }
        if (t == BACKHAND_HIT - 4) {
            sound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 2F, 0.4F);
        }
        if (t == BACKHAND_HIT) {
            Vec3d f = facing();
            Vec3d across = new Vec3d(-f.z, 0D, f.x);
            blow(4.4D, 0.15F, across.add(f.scale(0.3D)).normalize(), 2.6D, 0.6D, 1);
        }
        if (t >= BACKHAND_TICKS) {
            recover(14);
        }
    }

    /** Both fists locked over its head, then down into the stone; rings of force race out from them. */
    private void hammering(@Nullable EntityPlayer target) {
        int t = stateTicks;
        if (target != null && t < HAMMER_HIT - 6) {
            face(target, 0.15F);
        }
        if (t == 6) {
            sound(SoundEvents.BLOCK_PISTON_EXTEND, 2F, 0.4F);
        }
        if (t == HAMMER_HIT) {
            Vec3d at = getPositionVector().add(facing().scale(2.4D));
            EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.DESTROYER_QUAKE, at.x, posY + 0.05D, at.z, 10D, 0D, 0D, 3, 22),
                    dimension);
            sound(EternitySounds.COMET_IMPACT, 4F, 0.6F);
            sound(SoundEvents.ENTITY_GENERIC_EXPLODE, 3F, 0.5F);
            sound(SoundEvents.BLOCK_ANVIL_LAND, 2F, 0.3F);
            for (EntityPlayer player : world.playerEntities) {
                double d = player.getDistance(at.x, posY, at.z);
                if (isFair(player) && d < 10D && player.posY - posY < 3D) {
                    // Right under its fists it is crushing; out at the edge of the rings it only throws you.
                    float share = d < 2.8D ? 0.26F : 0.26F * (float) (1D - (d - 2.8D) / 7.2D) * 0.6F;
                    harm(player, share);
                    push(player, flat(player.getPositionVector().subtract(at)), 1.2D * (1D - d / 12D), 0.7D);
                }
            }
            hitStop = 3;
        }
        if (t >= HAMMER_TICKS) {
            recover(20);
        }
    }

    /** A foot lifted and driven down at whatever is too close. */
    private void stomping(@Nullable EntityPlayer target) {
        int t = stateTicks;
        if (target != null && t < STOMP_HIT) {
            face(target, 0.2F);
        }
        if (t == STOMP_HIT) {
            Vec3d at = getPositionVector().add(facing().scale(1.2D));
            EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.DESTROYER_QUAKE, at.x, posY + 0.05D, at.z, 5D, 0D, 0D, 1, 12),
                    dimension);
            sound(SoundEvents.ENTITY_IRONGOLEM_STEP, 4F, 0.3F);
            sound(SoundEvents.ENTITY_GENERIC_EXPLODE, 2F, 0.7F);
            for (EntityPlayer player : world.playerEntities) {
                double d = player.getDistance(at.x, posY, at.z);
                if (isFair(player) && d < 4.5D && player.onGround) {
                    harm(player, 0.1F);
                    push(player, flat(player.getPositionVector().subtract(at)), 0.9D, 0.9D);
                }
            }
        }
        if (t >= STOMP_TICKS) {
            recover(10);
        }
    }

    /** One blow, at whoever is in front of it and within reach. */
    private void blow(double reach, float share, Vec3d drive, double knock, double lift, int kind) {
        Vec3d f = facing();
        boolean hit = false;
        for (EntityPlayer player : world.playerEntities) {
            Vec3d to = player.getPositionVector().subtract(getPositionVector());
            if (isFair(player) && horizontal(player) < reach && flat(to).dotProduct(f) > 0.2D && to.y > -1.5D && to.y < 4D) {
                Vec3d at = new Vec3d(player.posX, player.posY + player.height * 0.6D, player.posZ);
                EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.DESTROYER_IMPACT, at.x, at.y, at.z, drive.x, drive.y, drive.z,
                        player.getEntityId(), kind), dimension);
                harm(player, share);
                push(player, drive, knock, lift);
                hit = true;
            }
        }
        if (hit) {
            sound(EternitySounds.COMET_IMPACT, 3F, 0.9F);
            sound(SoundEvents.ENTITY_IRONGOLEM_ATTACK, 3F, 0.5F);
            hitStop = 2;
        } else {
            sound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 2F, 0.35F);
        }
    }

    // ------------------------------------------------------------------ the ray

    /** Where its ray comes from: the furnace in its face. */
    public Vec3d face() {
        return new Vec3d(posX, posY + 3.7D, posZ).add(facing().scale(0.55D));
    }

    /** Its face opens, the furnace gathers, and the ray pours out after you; then its face closes. */
    private void ray(@Nullable EntityPlayer target) {
        int t = stateTicks;
        motionX = motionZ = 0D;
        if (target != null) {
            Vec3d want = chest(target);
            if (calm) {
                want = want.add(flat(want.subtract(face())).crossProduct(new Vec3d(0D, 1D, 0D)).scale(4D));
            }
            if (aim == null || t < RAY_FIRE) {
                aim = want;
                face(target, 0.25F);
            } else {
                // It follows you, but it is slow to turn.
                Vec3d toward = want.subtract(aim);
                double step = Math.min(toward.length(), 0.32D);
                if (step > 1.0E-3) {
                    aim = aim.add(toward.normalize().scale(step));
                }
                faceToward(aim, 0.1F);
            }
        }
        if (t == 2) {
            sound(SoundEvents.BLOCK_IRON_TRAPDOOR_OPEN, 3F, 0.35F);
            sound(SoundEvents.BLOCK_PISTON_CONTRACT, 2F, 0.5F);
        }
        if (t == RAY_OPEN) {
            EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.DESTROYER_CHARGE, posX, posY, posZ, 0D, 0D, 0D, getEntityId(),
                    RAY_FIRE - RAY_OPEN), dimension);
            sound(SoundEvents.ENTITY_BLAZE_AMBIENT, 3F, 0.3F);
            sound(SoundEvents.BLOCK_FIRE_AMBIENT, 3F, 0.6F);
        }
        if (aim != null && t >= RAY_FIRE && t < RAY_END) {
            fire(aim.subtract(face()).normalize(), t, t == RAY_FIRE);
        }
        if (t == RAY_END) {
            sound(SoundEvents.BLOCK_FIRE_EXTINGUISH, 3F, 0.5F);
        }
        if (t == RAY_END + 8) {
            sound(SoundEvents.BLOCK_IRON_TRAPDOOR_CLOSE, 3F, 0.35F);
        }
        if (t >= RAY_TICKS) {
            recover(18);
        }
    }

    /** The ray swung flat across the hall at chest height, from one side of you to the other. */
    private void sweep(@Nullable EntityPlayer target) {
        int t = stateTicks;
        motionX = motionZ = 0D;
        if (t < SWEEP_FIRE && target != null) {
            face(target, 0.25F);
            sweepFrom = rand.nextBoolean() ? 1F : -1F;
        }
        if (t == 2) {
            sound(SoundEvents.BLOCK_IRON_TRAPDOOR_OPEN, 3F, 0.35F);
        }
        if (t == RAY_OPEN) {
            EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.DESTROYER_CHARGE, posX, posY, posZ, 0D, 0D, 0D, getEntityId(),
                    SWEEP_FIRE - RAY_OPEN), dimension);
            sound(SoundEvents.ENTITY_BLAZE_AMBIENT, 3F, 0.3F);
        }
        if (t >= SWEEP_FIRE && t < SWEEP_END) {
            float f = (t - SWEEP_FIRE) / (float) (SWEEP_END - SWEEP_FIRE);
            float swing = sweepFrom * (65F - 130F * f);
            dataManager.set(AIM_YAW, swing);
            float yaw = (float) Math.toRadians(rotationYaw + swing);
            double drop = target != null ? MathHelper.clamp((chest(target).y - face().y) / 10D, -0.5D, 0.1D) : -0.25D;
            Vec3d dir = new Vec3d(-MathHelper.sin(yaw), drop, MathHelper.cos(yaw)).normalize();
            fire(dir, t, t == SWEEP_FIRE);
        }
        if (t == SWEEP_END) {
            dataManager.set(AIM_YAW, 0F);
            sound(SoundEvents.BLOCK_FIRE_EXTINGUISH, 3F, 0.5F);
        }
        if (t >= SWEEP_TICKS) {
            recover(18);
        }
    }

    /** One instant of the ray, out of its face along dir to the first wall. */
    private void fire(Vec3d dir, int t, boolean first) {
        Vec3d from = face();
        Vec3d far = from.add(dir.scale(64D));
        RayTraceResult wall = world.rayTraceBlocks(from, far, false, true, false);
        Vec3d end = wall != null && wall.typeOfHit == RayTraceResult.Type.BLOCK ? wall.hitVec : far;
        EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.DESTROYER_RAY, from.x, from.y, from.z, end.x, end.y, end.z, t, 3),
                dimension);
        if (first) {
            sound(SoundEvents.ENTITY_GENERIC_EXPLODE, 3F, 0.5F);
            sound(SoundEvents.ENTITY_FIREWORK_LARGE_BLAST_FAR, 4F, 0.4F);
        }
        if (t % 6 == 0) {
            sound(SoundEvents.ENTITY_GUARDIAN_ATTACK, 3F, 0.5F);
            sound(SoundEvents.ENTITY_BLAZE_BURN, 3F, 0.4F);
        }
        if (t % 4 == 0) {
            for (EntityPlayer player : world.playerEntities) {
                Vec3d body = player.getPositionVector().add(0D, player.height * 0.5D, 0D);
                if (isFair(player) && closest(from, end, body).distanceTo(body) < 1.4D) {
                    harm(player, 0.07F);
                    if (!calm && !player.isCreative()) {
                        player.setFire(3);
                    }
                    push(player, dir, 0.25D, 0.1D);
                }
            }
        }
    }

    // ------------------------------------------------------------------ falling

    private void falling() {
        motionX = motionZ = 0D;
        if (stateTicks == FALL_KNEE) {
            sound(SoundEvents.BLOCK_ANVIL_LAND, 3F, 0.3F);
            sound(SoundEvents.ENTITY_IRONGOLEM_DEATH, 3F, 0.4F);
        }
        if (stateTicks == FALL_TICKS - 14) {
            EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.DESTROYER_STEP, posX, posY, posZ, 1D, 0D, 0D, getEntityId(), 0),
                    dimension);
            sound(EternitySounds.COMET_IMPACT, 4F, 0.5F);
            sound(SoundEvents.BLOCK_ANVIL_LAND, 3F, 0.25F);
        }
        if (stateTicks >= FALL_TICKS) {
            setState(State.WRECK);
            VaultDirector.INSTANCE.destroyerFell(this);
        }
    }

    // ------------------------------------------------------------------ harm

    /** A share of the victim's maximum health, through armour. It cannot touch creative mode. */
    private void harm(EntityPlayer player, float share) {
        if (calm || !isFair(player)) {
            return;
        }
        player.hurtResistantTime = 0;
        float amount = player.getMaxHealth() * share * (float) EternityConfig.vault.destroyerDamageScale;
        player.attackEntityFrom(new EntityDamageSource("eternity.destroyer", this).setDamageBypassesArmor(), amount);
    }

    private void push(EntityPlayer player, Vec3d direction, double strength, double lift) {
        if (player.isSpectator()) {
            return;
        }
        double scale = calm ? 0.3D : 1D;
        player.motionX += direction.x * strength * scale;
        player.motionY = Math.max(player.motionY, 0D) + (direction.y * strength + lift) * scale;
        player.motionZ += direction.z * strength * scale;
        player.velocityChanged = true;
        player.fallDistance = 0F;
    }

    /**
     * Almost nothing hurts it. A blow from someone wielding something worthy of it gets through;
     * anything else is turned by its armour, only a sliver of it landing at all. Struck while it
     * stands dormant, it wakes. It never dies: beaten, it falls.
     */
    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (world.isRemote) {
            return false;
        }
        State state = getState();
        Entity attacker = source.getTrueSource();
        if (state == State.FALL || state == State.WRECK || !(attacker instanceof EntityPlayer)) {
            return false;
        }
        if (state == State.DORMANT) {
            wake();
        }
        boolean worthy = VaultDirector.worthy(source);
        float cap = (float) (getMaxHealth() * EternityConfig.vault.destroyerMaxHitFraction) * (worthy ? 1F : 0.1F);
        float dealt = Math.min(amount * (worthy ? 1F : (float) EternityConfig.vault.destroyerArmour), cap);
        if (!(dealt > 0F)) {
            return false;
        }
        world.setEntityState(this, (byte) 2);
        sound(worthy ? SoundEvents.BLOCK_ANVIL_PLACE : SoundEvents.ENTITY_IRONGOLEM_HURT, 1.5F, worthy ? 0.5F : 0.4F + rand.nextFloat() * 0.2F);
        if (attacker != null) {
            targetId = attacker.getUniqueID();
        }
        float left = getHealth() - dealt;
        if (left <= 1F) {
            setHealth(1F);
            setState(State.FALL);
            return true;
        }
        setHealth(left);
        if (worthy && dealt >= cap * 0.8F && (state == State.STALK || state == State.STOMP) && rand.nextInt(3) == 0) {
            setState(State.STAGGER);
        }
        return true;
    }

    @Override
    public void onKillCommand() {
        setDead();
    }

    @Override
    public void onDeath(DamageSource cause) {
        setHealth(1F);
        if (!isFallen()) {
            setState(State.FALL);
        }
    }

    // ------------------------------------------------------------------ helpers

    private void footfall() {
        leftFoot = !leftFoot;
        Vec3d f = facing();
        Vec3d side = new Vec3d(-f.z, 0D, f.x).scale(leftFoot ? 0.45D : -0.45D);
        EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.DESTROYER_STEP, posX + side.x, posY, posZ + side.z, 0.5D, 0D, 0D,
                getEntityId(), 0), dimension);
        sound(SoundEvents.ENTITY_IRONGOLEM_STEP, 3F, 0.35F);
        sound(SoundEvents.BLOCK_ANVIL_LAND, 0.35F, 0.3F);
    }

    private void sound(SoundEvent sound, float volume, float pitch) {
        world.playSound(null, posX, posY + 2D, posZ, sound, SoundCategory.HOSTILE, volume, pitch);
    }

    private void step(double distance) {
        Vec3d f = facing();
        motionX += f.x * distance;
        motionZ += f.z * distance;
    }

    private Vec3d facing() {
        float yaw = (float) Math.toRadians(rotationYaw);
        return new Vec3d(-MathHelper.sin(yaw), 0D, MathHelper.cos(yaw));
    }

    private double horizontal(Entity e) {
        double dx = e.posX - posX, dz = e.posZ - posZ;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static Vec3d chest(EntityPlayer player) {
        return new Vec3d(player.posX, player.posY + player.height * 0.6D, player.posZ);
    }

    private static Vec3d flat(Vec3d v) {
        Vec3d f = new Vec3d(v.x, 0D, v.z);
        return f.lengthSquared() < 1.0E-6 ? new Vec3d(0D, 0D, 1D) : f.normalize();
    }

    private static Vec3d closest(Vec3d a, Vec3d b, Vec3d p) {
        Vec3d ab = b.subtract(a);
        double len = ab.lengthSquared();
        if (len < 1.0E-8) {
            return a;
        }
        double t = MathHelper.clamp(p.subtract(a).dotProduct(ab) / len, 0D, 1D);
        return a.add(ab.scale(t));
    }

    private void face(Entity target, float rate) {
        faceToward(new Vec3d(target.posX, target.posY + target.getEyeHeight(), target.posZ), rate);
    }

    private void faceToward(Vec3d at, float rate) {
        double dx = at.x - posX, dz = at.z - posZ;
        double dy = at.y - (posY + getEyeHeight());
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90F;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        rotationYaw += MathHelper.wrapDegrees(yaw - rotationYaw) * rate;
        rotationYawHead = rotationYaw;
        rotationPitch += (pitch - rotationPitch) * rate;
    }

    // ------------------------------------------------------------------ client: the furnace in its face

    private void clientTick() {
        clientStateTicks++;
        int t = clientStateTicks;
        float wantHeat, wantOpen = 0F;
        switch (getState()) {
            case DORMANT:
            case WRECK:
                wantHeat = 0F;
                break;
            case WAKE:
                wantHeat = t < WAKE_GLOW ? 0F : t < WAKE_GLOW + 6 ? (t % 3 == 0 ? 0.6F : 0.1F) : 0.45F;
                break;
            case RAY:
                wantOpen = t < RAY_END + 6 ? Math.min(1F, t / (float) RAY_OPEN) : 0F;
                wantHeat = t < RAY_OPEN ? 0.5F : t < RAY_FIRE ? 0.5F + 0.5F * (t - RAY_OPEN) / (RAY_FIRE - RAY_OPEN) : t < RAY_END ? 1.1F : 0.6F;
                break;
            case SWEEP:
                wantOpen = t < SWEEP_END + 6 ? Math.min(1F, t / (float) RAY_OPEN) : 0F;
                wantHeat = t < SWEEP_FIRE ? 0.5F + 0.5F * t / SWEEP_FIRE : t < SWEEP_END ? 1.1F : 0.6F;
                break;
            case FALL:
                wantHeat = Math.max(0F, 0.45F - t / (float) FALL_TICKS * 0.6F) * (t % 7 < 2 ? 0.3F : 1F);
                break;
            case STAGGER:
                wantHeat = t % 4 < 2 ? 0.2F : 0.5F;
                break;
            default:
                wantHeat = 0.45F;
                break;
        }
        prevHeat = heat;
        heat += (wantHeat - heat) * 0.25F;
        prevOpen = open;
        open += (wantOpen - open) * (wantOpen > open ? 0.3F : 0.2F);
    }

    // ------------------------------------------------------------------ the rest

    @Override
    public void knockBack(Entity source, float strength, double x, double z) {
    }

    @Override
    public boolean isPotionApplicable(PotionEffect effect) {
        return false;
    }

    @Override
    protected void collideWithNearbyEntities() {
    }

    @Override
    public boolean canBePushed() {
        return false;
    }

    @Override
    public boolean isNonBoss() {
        return false;
    }

    @Override
    public boolean isImmuneToExplosions() {
        return true;
    }

    @Override
    public void fall(float distance, float damageMultiplier) {
    }

    @Override
    protected boolean canDespawn() {
        return false;
    }

    @Nullable
    @Override
    public Entity changeDimension(int dimension, ITeleporter teleporter) {
        return null;
    }

    /** Never written to chunks: the vault stands it in its niche whenever someone comes in. */
    @Override
    public boolean writeToNBTOptional(NBTTagCompound compound) {
        return false;
    }

    @Override
    public boolean isInRangeToRenderDist(double distance) {
        return distance < 200D * 200D;
    }

    @Override
    public SoundCategory getSoundCategory() {
        return SoundCategory.HOSTILE;
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return null;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return null;
    }

    @Override
    public float getEyeHeight() {
        return 3.6F;
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

    @SideOnly(Side.CLIENT)
    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return getEntityBoundingBox().grow(3D);
    }
}
