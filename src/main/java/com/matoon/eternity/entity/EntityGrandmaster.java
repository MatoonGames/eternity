package com.matoon.eternity.entity;

import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.network.MsgGrandmaster;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
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
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Rotations;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

/**
 * The Grandmaster: En Dwi Gast, an Elder of the Universe, who has spent longer than this
 * world has existed playing games, and cheats at them when it suits him. Blue-skinned, silver
 * hair to his shoulders, a high gold collar and a breastplate over a dark suit, rings of gold
 * up one forearm, keys at his belt. He is never still: he strolls between the pieces of his
 * board, shuffles cards from hand to hand without looking at them, conducts pieces across the
 * squares with a finger, and laughs with his whole body.
 * <p>
 * He does not fight. His board does that for him, and his life is bound up in it: what
 * breaks the board breaks him (see {@link com.matoon.eternity.world.GrandmasterDirector},
 * which moves him about and tells him what to do). His state, the point he is attending to,
 * and how badly his board is broken are synced; how he moves is worked out on each client.
 */
public class EntityGrandmaster extends EntityLiving {

    public enum State {
        ARRIVE, IDLE, PRESENT, SHUFFLE, POINT, CONDUCT, LAUGH, CLAP, THINK, WINCE, FALL, LIE, FADE, BOW
    }

    // How long each gesture lasts, in ticks; the model times its frames to the same numbers.
    public static final int ARRIVE_TICKS = 70;
    public static final int PRESENT_TICKS = 44;
    public static final int SHUFFLE_TICKS = 46;
    public static final int POINT_TICKS = 34;
    public static final int LAUGH_TICKS = 46;
    public static final int CLAP_TICKS = 44;
    public static final int WINCE_TICKS = 26;
    public static final int FALL_TICKS = 70;
    public static final int FADE_TICKS = 90;
    public static final int BOW_TICKS = 36;

    /** Segments in his mane, his coat tails, and the chain of keys at his belt. */
    public static final int MANE = 3, TAILS = 3, KEYS = 2;

    private static final DataParameter<Byte> STATE = EntityDataManager.createKey(EntityGrandmaster.class, DataSerializers.BYTE);
    private static final DataParameter<Rotations> FOCUS = EntityDataManager.createKey(EntityGrandmaster.class, DataSerializers.ROTATIONS);
    private static final DataParameter<Float> BROKEN = EntityDataManager.createKey(EntityGrandmaster.class, DataSerializers.FLOAT);

    private static final State[] SHOWCASE = {State.PRESENT, State.SHUFFLE, State.POINT, State.CONDUCT, State.LAUGH, State.THINK,
            State.CLAP, State.WINCE, State.BOW, State.SHUFFLE};

    // ------------------------------------------------------------------ server state
    private int stateTicks;
    /** How long the current gesture is held before he goes back to standing, or -1 to hold it until told. */
    private int holdFor = -1;
    @Nullable
    private Vec3d walkTo;
    @Nullable
    private Vec3d lookAt;
    private double walkSpeed = 0.13D;
    private boolean managed;
    private boolean showcase;
    private int showIndex;
    private int lastSpoke = -1000;
    private int lastStruck = -1000;

    // ------------------------------------------------------------------ client state
    public int clientStateTicks;
    private State seenState = State.ARRIVE;
    public State previousState = State.ARRIVE;
    /** Client: his velocity, smoothed, in blocks per tick, and how far his feet have carried him, for the stride. */
    public double velX, velZ;
    public float stride, prevStride, gait, prevGait;
    /** Client: each spring's angle back from rest, how fast it swings, and the sideways sway of each chain. */
    public final float[] mane = new float[MANE], prevMane = new float[MANE], maneSpin = new float[MANE];
    public final float[] tails = new float[TAILS], prevTails = new float[TAILS], tailSpin = new float[TAILS];
    public final float[] keys = new float[KEYS], prevKeys = new float[KEYS], keySpin = new float[KEYS];
    public float sway, prevSway, keySway, prevKeySway;
    /** Client: the pose he was in when his state last changed, which the model blends out of; and the pose last drawn. */
    public final float[] poseFrom = new float[48], poseLast = new float[48];

    public EntityGrandmaster(World world) {
        super(world);
        setSize(0.7F, 2.2F);
        isImmuneToFire = true;
        experienceValue = 0;
        ignoreFrustumCheck = true;
        setNoGravity(true);
        noClip = true;
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(STATE, (byte) State.IDLE.ordinal());
        dataManager.register(FOCUS, new Rotations(0F, 0F, 0F));
        dataManager.register(BROKEN, 0F);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(100D);
        getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(1D);
    }

    @Override
    public void notifyDataManagerChange(DataParameter<?> key) {
        super.notifyDataManagerChange(key);
        if (STATE.equals(key) && world.isRemote) {
            clientStateTicks = 0;
            System.arraycopy(poseLast, 0, poseFrom, 0, poseLast.length);
            previousState = seenState;
            seenState = getState();
        }
    }

    // ------------------------------------------------------------------ accessors

    public State getState() {
        int ordinal = dataManager.get(STATE);
        return ordinal >= 0 && ordinal < State.values().length ? State.values()[ordinal] : State.IDLE;
    }

    /** The point he is attending to: the piece he is conducting, the player he points at. */
    public Vec3d getFocus() {
        Rotations r = dataManager.get(FOCUS);
        return new Vec3d(r.getX(), r.getY(), r.getZ());
    }

    /** How broken his board is, 0 to 1. His life goes with it. */
    public float getBroken() {
        return dataManager.get(BROKEN);
    }

    public boolean isShowcase() {
        return showcase;
    }

    public int getStateTicks() {
        return stateTicks;
    }

    // ------------------------------------------------------------------ what the director tells him

    /** Kept by the director: he goes where he is told and never wanders off. */
    public void setManaged(boolean managed) {
        this.managed = managed;
    }

    public void setShowcase(boolean showcase) {
        this.showcase = showcase;
        showIndex = 0;
    }

    /** Begins a gesture, held for its natural length, then back to standing. */
    public void act(State state) {
        act(state, natural(state));
    }

    /** Begins a gesture held for {@code ticks}, or until told otherwise if -1. */
    public void act(State state, int ticks) {
        dataManager.set(STATE, (byte) state.ordinal());
        stateTicks = 0;
        holdFor = ticks;
    }

    public void focus(Vec3d at) {
        dataManager.set(FOCUS, new Rotations((float) at.x, (float) at.y, (float) at.z));
    }

    public void setBroken(float broken) {
        dataManager.set(BROKEN, MathHelper.clamp(broken, 0F, 1F));
    }

    /** Strolls to a point, at his own pace. */
    public void walkTo(@Nullable Vec3d to, double speed) {
        walkTo = to;
        walkSpeed = speed;
    }

    public void walkTo(@Nullable Vec3d to) {
        walkTo(to, 0.13D);
    }

    public boolean isWalking() {
        return walkTo != null;
    }

    public void lookAt(@Nullable Vec3d at) {
        lookAt = at;
    }

    private static int natural(State state) {
        switch (state) {
            case ARRIVE:
                return ARRIVE_TICKS;
            case PRESENT:
                return PRESENT_TICKS;
            case SHUFFLE:
                return SHUFFLE_TICKS;
            case POINT:
                return POINT_TICKS;
            case LAUGH:
                return LAUGH_TICKS;
            case CLAP:
                return CLAP_TICKS;
            case WINCE:
                return WINCE_TICKS;
            case BOW:
                return BOW_TICKS;
            case FALL:
            case LIE:
            case FADE:
            default:
                return -1;
        }
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void onLivingUpdate() {
        noClip = true;
        motionX = motionY = motionZ = 0D;
        if (!world.isRemote) {
            serverTick();
        }
        super.onLivingUpdate();
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (world.isRemote) {
            clientTick();
        }
    }

    @Override
    public void travel(float strafe, float vertical, float forward) {
        move(MoverType.SELF, motionX, motionY, motionZ);
    }

    private void serverTick() {
        stateTicks++;
        State state = getState();
        if (holdFor >= 0 && stateTicks >= holdFor) {
            if (state == State.FALL) {
                act(State.LIE, -1);
            } else if (state != State.LIE && state != State.FADE) {
                act(State.IDLE, -1);
            }
        }
        if (state == State.FALL && stateTicks >= FALL_TICKS) {
            act(State.LIE, -1);
        }
        boolean down = state == State.FALL || state == State.LIE || state == State.FADE;
        if (walkTo != null && !down) {
            Vec3d to = new Vec3d(walkTo.x, posY, walkTo.z);
            Vec3d step = to.subtract(getPositionVector());
            double d = step.length();
            if (d < walkSpeed) {
                setPosition(walkTo.x, posY, walkTo.z);
                walkTo = null;
            } else {
                step = step.scale(walkSpeed / d);
                setPosition(posX + step.x, posY, posZ + step.z);
                turnTo((float) Math.toDegrees(Math.atan2(step.z, step.x)) - 90F, 0.25F);
            }
        } else if (lookAt != null && !down) {
            turnTo((float) Math.toDegrees(Math.atan2(lookAt.z - posZ, lookAt.x - posX)) - 90F, 0.12F);
        }
        if (lookAt != null) {
            double dx = lookAt.x - posX, dz = lookAt.z - posZ, dy = lookAt.y - (posY + getEyeHeight());
            float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
            rotationPitch += (MathHelper.clamp(pitch, -50F, 50F) - rotationPitch) * 0.2F;
            float headYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90F;
            rotationYawHead = rotationYaw + MathHelper.clamp(MathHelper.wrapDegrees(headYaw - rotationYaw), -70F, 70F);
        } else {
            rotationYawHead = rotationYaw;
        }
        // Feet on whatever is under him: the board, or the ground once it is gone.
        if (!down || state == State.FALL) {
            double floor = floor(posX, posY + 1.5D, posZ);
            setPosition(posX, posY + (floor - posY) * 0.35D, posZ);
        }
        if (showcase && !managed && state == State.IDLE && stateTicks > 30) {
            State next = SHOWCASE[showIndex++ % SHOWCASE.length];
            act(next, natural(next) > 0 ? natural(next) : 70);
        }
        if (showcase && !managed && ticksExisted % 40 == 0) {
            EntityPlayer near = world.getClosestPlayerToEntity(this, 32D);
            if (near != null) {
                lookAt = near.getPositionEyes(1F);
                focus(near.getPositionVector().add(0D, 1D, 0D));
            }
        }
        if (!managed && !showcase && ticksExisted > 200 && world.getClosestPlayerToEntity(this, 64D) == null) {
            setDead();
        }
    }

    private void turnTo(float yaw, float rate) {
        rotationYaw += MathHelper.wrapDegrees(yaw - rotationYaw) * rate;
        renderYawOffset = rotationYaw;
    }

    /** The top of the ground under a point, looking down from y. */
    private double floor(double x, double y, double z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(MathHelper.floor(x), MathHelper.floor(y), MathHelper.floor(z));
        for (int i = 0; i < 24 && pos.getY() > 0; i++) {
            IBlockState state = world.getBlockState(pos);
            AxisAlignedBB box = state.getCollisionBoundingBox(world, pos);
            if (box != null && box != net.minecraft.block.Block.NULL_AABB) {
                return pos.getY() + box.maxY;
            }
            pos.move(net.minecraft.util.EnumFacing.DOWN);
        }
        return y - 1.5D;
    }

    // ------------------------------------------------------------------ voice

    /** He speaks to everyone near enough to hear. */
    public void say(String line) {
        speak(world, this, getEntityId(), line, 96D);
        lastSpoke = ticksExisted;
    }

    public int sinceSpoke() {
        return ticksExisted - lastSpoke;
    }

    /** A line of his, shown to everyone near enough to hear it in the speaker panel, with his face beside it. */
    public static void speak(World world, Entity at, int speaker, String line, double range) {
        for (EntityPlayer player : world.playerEntities) {
            if (player.getDistance(at) < range && player instanceof EntityPlayerMP) {
                EternityNetwork.send(MsgGrandmaster.line(com.matoon.eternity.world.GrandmasterScenes.GRANDMASTER, speaker, line), (EntityPlayerMP) player);
            }
        }
    }

    public void sound(SoundEvent sound, float volume, float pitch) {
        world.playSound(null, posX, posY + 1D, posZ, sound, SoundCategory.NEUTRAL, volume, pitch);
    }

    // ------------------------------------------------------------------ he is not the game

    /**
     * Nothing touches him. A blow meant for him meets a burst of cards where he was, and he
     * is already a step to one side, amused.
     */
    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (world.isRemote) {
            return false;
        }
        Entity by = source.getTrueSource();
        if (by instanceof EntityPlayer && ticksExisted - lastStruck > 10) {
            lastStruck = ticksExisted;
            Vec3d c = new Vec3d(posX, posY + 1.3D, posZ);
            EternityNetwork.sendAround(MsgGrandmaster.fx(MsgGrandmaster.FX_CARDS, c, Vec3d.ZERO, 18), dimension, posX, posY, posZ, 64D);
            sound(SoundEvents.ENTITY_ENDERMEN_TELEPORT, 0.6F, 1.8F);
            Vec3d away = new Vec3d(posX - by.posX, 0D, posZ - by.posZ);
            if (away.lengthSquared() > 1.0E-4) {
                away = away.normalize().scale(1.2D);
                setPosition(posX + away.x, posY, posZ + away.z);
            }
            if (by instanceof EntityPlayerMP) {
                com.matoon.eternity.world.GrandmasterDirector.INSTANCE.struck(this, (EntityPlayerMP) by);
            }
        }
        return false;
    }

    @Override
    protected boolean processInteract(EntityPlayer player, EnumHand hand) {
        if (!world.isRemote && hand == EnumHand.MAIN_HAND && player instanceof EntityPlayerMP) {
            com.matoon.eternity.world.GrandmasterDirector.INSTANCE.touched(this, (EntityPlayerMP) player);
        }
        return true;
    }

    @Override
    public void onKillCommand() {
        setDead();
    }

    // ------------------------------------------------------------------ client: how he moves

    private void clientTick() {
        clientStateTicks++;
        double vx = posX - prevPosX, vz = posZ - prevPosZ;
        velX += (vx - velX) * 0.5D;
        velZ += (vz - velZ) * 0.5D;
        double speed = Math.sqrt(velX * velX + velZ * velZ);
        prevStride = stride;
        prevGait = gait;
        // An unhurried, long-legged stroll: the stride follows the ground covered.
        stride += (float) speed * 3.1F;
        gait += ((float) MathHelper.clamp(speed / 0.12D, 0D, 1D) - gait) * 0.25F;
        renderYawOffset = rotationYaw;
        prevRenderYawOffset = prevRotationYaw;

        float yaw = (float) Math.toRadians(rotationYaw);
        double fwd = -velX * MathHelper.sin(yaw) + velZ * MathHelper.cos(yaw);
        double side = velX * MathHelper.cos(yaw) + velZ * MathHelper.sin(yaw);
        float t = ticksExisted;
        State state = getState();
        boolean lying = state == State.LIE || state == State.FADE || state == State.FALL && clientStateTicks > 40;
        // A soft breeze over his board: the hair and tails never quite hang still.
        float breeze = MathHelper.sin(t * 0.045F) * 0.05F + MathHelper.sin(t * 0.13F) * 0.02F;
        float lift = (float) MathHelper.clamp(fwd * 4.5D, -0.2D, 0.9D);
        float jolt = state == State.WINCE && clientStateTicks < 6 ? 0.5F : state == State.LAUGH ? MathHelper.sin(t * 1.4F) * 0.05F : 0F;
        prevSway = sway;
        sway += ((float) MathHelper.clamp(-side * 3D, -0.4D, 0.4D) + MathHelper.sin(t * 0.06F) * 0.04F - sway) * 0.15F;
        spring(mane, prevMane, maneSpin, lying ? -0.2F : 0.08F + lift * 0.6F + breeze + jolt, 0.75F, 0.18F);
        spring(tails, prevTails, tailSpin, lying ? -0.6F : 0.05F + lift + breeze * 1.4F + jolt * 0.5F, 0.6F, 0.22F);
        prevKeySway = keySway;
        keySway += ((float) MathHelper.clamp(-side * 6D, -0.6D, 0.6D) + MathHelper.sin(t * 0.33F) * 0.12F * gait - keySway) * 0.3F;
        spring(keys, prevKeys, keySpin, (float) MathHelper.clamp(fwd * 6D, -0.4D, 0.6D) + MathHelper.sin(t * 0.33F + 1F) * 0.15F * gait
                + jolt, 0.4F, 0.35F);
    }

    /**
     * A chain of springs: each segment swings toward its share of the goal and toward the one
     * above it, so a ripple runs down it.
     */
    private static void spring(float[] seg, float[] prev, float[] spin, float goal, float follow, float stiffness) {
        System.arraycopy(seg, 0, prev, 0, seg.length);
        for (int i = 0; i < seg.length; i++) {
            float want = i == 0 ? goal : seg[i - 1] * follow + goal * (1F - follow);
            spin[i] += (want - seg[i]) * stiffness;
            spin[i] *= 0.7F;
            seg[i] += spin[i];
        }
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
    public boolean isImmuneToExplosions() {
        return true;
    }

    @Override
    public boolean isEntityInsideOpaqueBlock() {
        return false;
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

    /** Never written to chunks: the director puts him back from its own record. */
    @Override
    public boolean writeToNBTOptional(NBTTagCompound compound) {
        return false;
    }

    @Override
    public boolean isInRangeToRenderDist(double distance) {
        return distance < 256D * 256D;
    }

    @Override
    public float getEyeHeight() {
        return 2.0F;
    }

    @Override
    public SoundCategory getSoundCategory() {
        return SoundCategory.NEUTRAL;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return getEntityBoundingBox().grow(3D);
    }
}
