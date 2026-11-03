package com.matoon.eternity.entity;

import com.matoon.eternity.client.render.NorseBoxes;
import com.matoon.eternity.story.StoryDirector;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * Someone the story needs a face for: the woman in mourning clothes (and, once she has said what
 * she is, what is under them), Ragna, and Hrafn. They do nothing of their own accord; the story
 * moves them about, turns their heads and tells them what to do with their hands (see
 * {@link com.matoon.eternity.story.StoryDirector}). Nothing hurts them.
 * <p>
 * They are never saved with the world. The story knows who should be standing where, and puts
 * them there again whenever someone comes near.
 */
public class EntityNorse extends EntityCreature implements NorseBody.Figure {

    private static final DataParameter<Byte> LOOK = EntityDataManager.createKey(EntityNorse.class, DataSerializers.BYTE);
    private static final DataParameter<Byte> POSE = EntityDataManager.createKey(EntityNorse.class, DataSerializers.BYTE);
    // Three floats, not Rotations: Rotations wraps every value modulo 360, which turned a point far from the origin into nonsense.
    private static final DataParameter<Float> FOCUS_X = EntityDataManager.createKey(EntityNorse.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> FOCUS_Y = EntityDataManager.createKey(EntityNorse.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> FOCUS_Z = EntityDataManager.createKey(EntityNorse.class, DataSerializers.FLOAT);
    private static final DataParameter<Boolean> ARMED = EntityDataManager.createKey(EntityNorse.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> SHINING = EntityDataManager.createKey(EntityNorse.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Float> ASH = EntityDataManager.createKey(EntityNorse.class, DataSerializers.FLOAT);

    // ------------------------------------------------------------------ server
    /** What the story calls them by, so it can find them again. */
    public String role = "";
    private int poseTicks;
    private int holdFor = -1;
    private NorsePose rest = NorsePose.STAND;
    @Nullable
    private Vec3d walkTo;
    private double walkSpeed = 0.5D;
    @Nullable
    private Entity following;
    private double followAt = 3D;
    @Nullable
    private Entity watching;
    /** Lying people keep the heading they were laid down with. */
    private float laidYaw;

    // ------------------------------------------------------------------ client
    private final NorseBody body = new NorseBody();

    public EntityNorse(World world) {
        super(world);
        setSize(0.6F, 1.8F);
        experienceValue = 0;
        enablePersistence();
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(LOOK, (byte) NorseBoxes.STRANGER);
        dataManager.register(POSE, (byte) NorsePose.STAND.ordinal());
        dataManager.register(FOCUS_X, 0F);
        dataManager.register(FOCUS_Y, -1000F);
        dataManager.register(FOCUS_Z, 0F);
        dataManager.register(ARMED, false);
        dataManager.register(SHINING, false);
        dataManager.register(ASH, 0F);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(40D);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.3D);
        getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(1D);
        getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(64D);
    }

    public static EntityNorse of(World world, int look, String role) {
        EntityNorse e = new EntityNorse(world);
        e.dataManager.set(LOOK, (byte) look);
        e.role = role;
        return e;
    }

    @Override
    public void notifyDataManagerChange(DataParameter<?> key) {
        super.notifyDataManagerChange(key);
        if (POSE.equals(key) && world.isRemote) {
            body.changed(pose());
        }
    }

    // ------------------------------------------------------------------ what they are

    @Override
    public int look() {
        return dataManager.get(LOOK);
    }

    public void setLook(int look) {
        dataManager.set(LOOK, (byte) look);
    }

    @Override
    public NorsePose pose() {
        int i = dataManager.get(POSE);
        return i >= 0 && i < NorsePose.values().length ? NorsePose.values()[i] : NorsePose.STAND;
    }

    @Override
    public NorseBody body() {
        return body;
    }

    public boolean isArmed() {
        return dataManager.get(ARMED);
    }

    public void setArmed(boolean armed) {
        dataManager.set(ARMED, armed);
    }

    /** Whether something is lit in them: the Time Stone, once the Gardener has pressed it into his chest. */
    public boolean isShining() {
        return dataManager.get(SHINING);
    }

    /** How far the life has gone out of their skin, 0 to 1: grey, and dull, and done. */
    public float ash() {
        return dataManager.get(ASH);
    }

    public void setAsh(float ash) {
        dataManager.set(ASH, ash);
    }

    public void setShining(boolean shining) {
        dataManager.set(SHINING, shining);
    }

    /** The point they are attending to, or null for none. */
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


    // ------------------------------------------------------------------ the story's handles

    /** Does something and holds it until told otherwise. */
    public EntityNorse act(NorsePose pose) {
        return act(pose, -1);
    }

    /** Does something for so many ticks (or as long as the gesture lasts, if 0), then goes back to resting. */
    public EntityNorse act(NorsePose pose, int ticks) {
        poseTicks = 0;
        holdFor = ticks == 0 ? pose.ticks() : ticks;
        if (pose.lying()) {
            laidYaw = rotationYaw;
        }
        dataManager.set(POSE, (byte) pose.ordinal());
        if (pose == NorsePose.DRAW) {
            setArmed(true);
        }
        return this;
    }

    /** What they go back to after a gesture. */
    public EntityNorse rest(NorsePose pose) {
        rest = pose;
        return this;
    }

    public NorsePose restPose() {
        return rest;
    }

    /** Turns them to look at a point, and keeps them looking. */
    public EntityNorse look(@Nullable Vec3d at) {
        watching = null;
        setFocusPoint(at);
        return this;
    }

    /** Keeps them looking at someone, wherever they go. */
    public EntityNorse watch(@Nullable Entity who) {
        watching = who;
        if (who == null) {
            look(null);
        }
        return this;
    }

    public EntityNorse walkTo(@Nullable Vec3d to, double speed) {
        following = null;
        walkTo = to;
        walkSpeed = speed;
        if (to == null) {
            getNavigator().clearPath();
        } else {
            getNavigator().tryMoveToXYZ(to.x, to.y, to.z, speed);
        }
        return this;
    }

    public EntityNorse follow(@Nullable Entity who, double distance) {
        walkTo = null;
        following = who;
        followAt = distance;
        if (who == null) {
            getNavigator().clearPath();
        }
        return this;
    }

    public boolean arrived() {
        return walkTo == null || getDistanceSq(walkTo.x, walkTo.y, walkTo.z) < 2.25D;
    }

    /** Turns body and head to face a point at once. */
    public void face(Vec3d at) {
        double dx = at.x - posX, dz = at.z - posZ;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        rotationYaw = renderYawOffset = rotationYawHead = prevRotationYaw = prevRenderYawOffset = prevRotationYawHead = yaw;
        laidYaw = yaw;
    }

    /** A flinch, seen by everyone: something has struck them. */
    public void flinch() {
        world.setEntityState(this, (byte) 2);
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (world.isRemote) {
            body.tick(this);
            return;
        }
        poseTicks++;
        if (holdFor > 0 && poseTicks >= holdFor && pose() != rest) {
            holdFor = -1;
            poseTicks = 0;
            dataManager.set(POSE, (byte) rest.ordinal());
        }
        if (following != null) {
            if (following.isDead || following.dimension != dimension) {
                following = null;
            } else if (getDistance(following) > followAt + 1.5D) {
                if (ticksExisted % 10 == 0 || getNavigator().noPath()) {
                    getNavigator().tryMoveToEntityLiving(following, getDistance(following) > 10D ? 0.75D : 0.55D);
                }
            } else if (getDistance(following) < followAt) {
                getNavigator().clearPath();
            }
        } else if (walkTo != null) {
            if (arrived()) {
                getNavigator().clearPath();
            } else if (getNavigator().noPath() && ticksExisted % 20 == 0) {
                if (!getNavigator().tryMoveToXYZ(walkTo.x, walkTo.y, walkTo.z, walkSpeed)) {
                    // Nowhere to walk: step straight there.
                    getMoveHelper().setMoveTo(walkTo.x, walkTo.y, walkTo.z, walkSpeed);
                }
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
        if (pose().lying()) {
            rotationYaw = renderYawOffset = laidYaw;
        } else if (focus != null && getNavigator().noPath()) {
            // Standing still, they turn their head to what they attend to, and their body after it.
            double dx = focus.x - posX, dz = focus.z - posZ;
            float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            rotationYawHead = approach(rotationYawHead, yaw, 12F);
            if (Math.abs(MathHelper.wrapDegrees(yaw - renderYawOffset)) > 50F) {
                renderYawOffset = approach(renderYawOffset, yaw, 4F);
                rotationYaw = renderYawOffset;
            }
            double dy = focus.y - (posY + getEyeHeight());
            rotationPitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        }
    }

    private static float approach(float from, float to, float max) {
        return from + MathHelper.clamp(MathHelper.wrapDegrees(to - from), -max, max);
    }

    // ------------------------------------------------------------------ the world can't touch them

    @Override
    protected boolean processInteract(EntityPlayer player, EnumHand hand) {
        if (!world.isRemote && hand == EnumHand.MAIN_HAND && player instanceof EntityPlayerMP) {
            StoryDirector.INSTANCE.interact((EntityPlayerMP) player, this);
        }
        return true;
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (!world.isRemote && source.getTrueSource() instanceof EntityPlayerMP) {
            StoryDirector.INSTANCE.struck((EntityPlayerMP) source.getTrueSource(), this);
        }
        return false;
    }

    @Override
    public boolean isEntityInvulnerable(DamageSource source) {
        return source != DamageSource.OUT_OF_WORLD;
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
        return distance < 160D * 160D;
    }

    @Override
    public float getEyeHeight() {
        switch (pose()) {
            case KNEEL:
            case HOLD:
            case CLOSE:
            case TOUCH:
                return 1.05F;
            case GROUND:
                return 0.9F;
            case LIE:
            case DEAD:
            case FALLEN:
                return 0.25F;
            default:
                return 1.62F;
        }
    }

    @Override
    public float getRenderSizeModifier() {
        return look() == NorseBoxes.HRAFN ? 1F : 0.94F;
    }
}
