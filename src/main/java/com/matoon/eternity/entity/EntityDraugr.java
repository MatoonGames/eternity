package com.matoon.eternity.entity;

import com.matoon.eternity.client.render.NorseBoxes;
import com.matoon.eternity.story.StoryDirector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * One of the dead under the mound: Hrafn's people, laid in the barrow with the thing they swore
 * to keep, and kept by the oath as much as keeping it. While the oath holds they lie in their
 * graves. Wrong words at the door wake the one whose words should have come, and it gets up to
 * deal with whoever is in its barrow; once the oath is broken every one of them gets up for the
 * one who broke it.
 * <p>
 * They can be knocked down but not killed: whatever brings one down, it lies there a while and
 * gets up again. The story says whom they hunt, and when they lie down for good.
 */
public class EntityDraugr extends EntityCreature implements IMob, NorseBody.Figure {

    private static final DataParameter<Byte> POSE = EntityDataManager.createKey(EntityDraugr.class, DataSerializers.BYTE);
    private static final DataParameter<Boolean> ARMED = EntityDataManager.createKey(EntityDraugr.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> HELMED = EntityDataManager.createKey(EntityDraugr.class, DataSerializers.BOOLEAN);
    /** Which of the dead it is: the barrow's own, or what came out of the Sovereign's vats. */
    private static final DataParameter<Byte> LOOK = EntityDataManager.createKey(EntityDraugr.class, DataSerializers.BYTE);
    /** The vats' failures die, as the barrow's dead do not. */
    private boolean mortal;

    private static final int FALLEN_TICKS = 90, CLAW_HIT = 9, CLAW_COOLDOWN = 14;

    /** Which grave is theirs, so the story can find them again; and which keeper, or -1 for kin. */
    public int grave = -1, keeper = -1;
    @Nullable
    private Vec3d home;
    private float homeYaw;
    @Nullable
    private EntityLivingBase hunting;
    private int poseTicks, cooldown;
    /** Lie down again for good, after the next time they fall. */
    private boolean stayDown;
    /** Up only to see off an intruder: back to the grave once they have been knocked down, or given up. */
    private boolean warden;
    private int huntedFor;

    private final NorseBody body = new NorseBody();

    public EntityDraugr(World world) {
        super(world);
        setSize(0.6F, 1.8F);
        experienceValue = 0;
        enablePersistence();
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(POSE, (byte) NorsePose.DEAD.ordinal());
        dataManager.register(ARMED, false);
        dataManager.register(HELMED, false);
        dataManager.register(LOOK, (byte) NorseBoxes.DRAUGR);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(30D);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.27D);
        getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(48D);
        getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(0.6D);
        getEntityAttribute(SharedMonsterAttributes.ARMOR).setBaseValue(6D);
    }

    @Override
    public void notifyDataManagerChange(DataParameter<?> key) {
        super.notifyDataManagerChange(key);
        if (POSE.equals(key) && world.isRemote) {
            body.changed(pose());
        }
    }

    @Override
    public int look() {
        return dataManager.get(LOOK);
    }

    /**
     * One of the Sovereign's failures, crouched in the wreck of its vat: it gets up when someone
     * comes, and unlike the barrow's dead, it can be killed.
     */
    public EntityDraugr grown(Vec3d at, float yaw) {
        dataManager.set(LOOK, (byte) NorseBoxes.SPAWN);
        mortal = true;
        laid(at, yaw, -1, -1, false, false);
        return this;
    }

    public boolean isMortal() {
        return mortal;
    }

    @Override
    public NorsePose pose() {
        int i = dataManager.get(POSE);
        return i >= 0 && i < NorsePose.values().length ? NorsePose.values()[i] : NorsePose.DEAD;
    }

    @Override
    public NorseBody body() {
        return body;
    }

    public boolean isArmed() {
        return dataManager.get(ARMED);
    }

    public boolean isHelmed() {
        return dataManager.get(HELMED);
    }

    /** Lays them in their grave. */
    public EntityDraugr laid(Vec3d at, float yaw, int grave, int keeper, boolean armed, boolean helmed) {
        home = at;
        homeYaw = yaw;
        this.grave = grave;
        this.keeper = keeper;
        dataManager.set(ARMED, armed);
        dataManager.set(HELMED, helmed);
        setLocationAndAngles(at.x, at.y, at.z, yaw, 0F);
        rotationYawHead = renderYawOffset = yaw;
        setPose(NorsePose.DEAD);
        return this;
    }

    public boolean isAwake() {
        NorsePose p = pose();
        return p == NorsePose.RISE || p == NorsePose.HUNT || p == NorsePose.CLAW || p == NorsePose.FALLEN && !stayDown;
    }

    public boolean isLaidDown() {
        return pose() == NorsePose.DEAD;
    }

    @Nullable
    public EntityLivingBase hunting() {
        return hunting;
    }

    /**
     * Gets up out of the grave for someone. A warden goes back to its grave once it has been
     * knocked down or has hunted long enough; otherwise it keeps coming.
     */
    public void wake(EntityLivingBase target, boolean warden) {
        hunting = target;
        this.warden = warden;
        stayDown = false;
        huntedFor = 0;
        setAttackTarget(target instanceof EntityPlayer ? target : null);
        if (pose() == NorsePose.DEAD || pose() == NorsePose.FALLEN) {
            setPose(NorsePose.RISE);
            setHealth(getMaxHealth());
            sound(SoundEvents.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 0.9F, 0.5F);
            sound(SoundEvents.BLOCK_GRAVEL_BREAK, 1F, 0.6F);
        }
    }

    /** Turns them on someone else without lying down first. */
    public void hunt(EntityLivingBase target) {
        hunting = target;
        setAttackTarget(target instanceof EntityPlayer ? target : null);
    }

    /** Lies down where they are, and stays down: the oath has what it was owed. */
    public void lieStill() {
        hunting = null;
        stayDown = true;
        getNavigator().clearPath();
        setPose(NorsePose.FALLEN);
    }

    private void setPose(NorsePose pose) {
        poseTicks = 0;
        dataManager.set(POSE, (byte) pose.ordinal());
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
        if (cooldown > 0) {
            cooldown--;
        }
        NorsePose pose = pose();
        switch (pose) {
            case DEAD:
                getNavigator().clearPath();
                if (home != null && getDistanceSq(home.x, home.y, home.z) > 0.25D) {
                    setPosition(home.x, home.y, home.z);
                }
                rotationYaw = renderYawOffset = rotationYawHead = homeYaw;
                break;
            case RISE:
                getNavigator().clearPath();
                if (poseTicks >= NorsePose.RISE.ticks()) {
                    setPose(NorsePose.HUNT);
                }
                break;
            case HUNT:
                pursue();
                break;
            case CLAW:
                getNavigator().clearPath();
                if (hunting != null) {
                    faceTowards(hunting);
                }
                if (poseTicks == CLAW_HIT && hunting != null && getDistance(hunting) < 2.7D) {
                    strike(hunting);
                }
                if (poseTicks >= NorsePose.CLAW.ticks()) {
                    cooldown = CLAW_COOLDOWN + rand.nextInt(10);
                    setPose(NorsePose.HUNT);
                }
                break;
            case FALLEN:
                getNavigator().clearPath();
                if (!stayDown && poseTicks >= FALLEN_TICKS + rand.nextInt(3)) {
                    if (warden && home != null) {
                        goHome();
                    } else {
                        setHealth(getMaxHealth());
                        setPose(NorsePose.RISE);
                    }
                }
                break;
            default:
                break;
        }
    }

    private void pursue() {
        huntedFor++;
        if (warden && huntedFor > 20 * 40) {
            goHome();
            return;
        }
        if (hunting == null || hunting.isDead || hunting.dimension != dimension
                || hunting instanceof EntityPlayer && (((EntityPlayer) hunting).isSpectator() || hunting.getHealth() <= 0F)) {
            if (warden && home != null) {
                goHome();
            }
            return;
        }
        faceTowards(hunting);
        double d = getDistance(hunting);
        if (d < 2.1D && cooldown <= 0) {
            setPose(NorsePose.CLAW);
            sound(SoundEvents.ENTITY_ZOMBIE_AMBIENT, 0.8F, 0.55F);
        } else if (d >= 1.6D && (ticksExisted % 8 == 0 || getNavigator().noPath())) {
            getNavigator().tryMoveToEntityLiving(hunting, 1.0D);
        }
        if (ticksExisted % 70 == 0 && rand.nextInt(3) == 0) {
            sound(SoundEvents.ENTITY_ZOMBIE_AMBIENT, 0.7F, 0.45F + rand.nextFloat() * 0.1F);
        }
    }

    /** Back into the grave, lying down as they were laid. */
    private void goHome() {
        hunting = null;
        setAttackTarget(null);
        if (home != null) {
            setPositionAndUpdate(home.x, home.y, home.z);
        }
        rotationYaw = renderYawOffset = rotationYawHead = homeYaw;
        setHealth(getMaxHealth());
        setPose(NorsePose.DEAD);
    }

    private void faceTowards(Entity e) {
        double dx = e.posX - posX, dz = e.posZ - posZ;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        rotationYaw = renderYawOffset = rotationYawHead = yaw + MathHelper.wrapDegrees(rotationYaw - yaw) * 0.6F;
    }

    private void strike(EntityLivingBase target) {
        sound(isArmed() ? SoundEvents.ENTITY_PLAYER_ATTACK_STRONG : SoundEvents.ENTITY_ZOMBIE_ATTACK_DOOR_WOOD, 0.9F, 0.7F);
        if (target instanceof EntityNorse) {
            ((EntityNorse) target).flinch();
            StoryDirector.INSTANCE.draugrStruck(this, (EntityNorse) target);
            return;
        }
        float damage = isArmed() ? 6F : mortal ? 5F : 4.5F;
        target.attackEntityFrom(DamageSource.causeMobDamage(this), damage);
        Vec3d away = new Vec3d(target.posX - posX, 0D, target.posZ - posZ);
        if (away.lengthSquared() > 1.0E-4) {
            away = away.normalize();
            target.knockBack(this, 0.5F, -away.x, -away.z);
        }
    }

    // ------------------------------------------------------------------ they cannot be killed

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (world.isRemote || source == DamageSource.OUT_OF_WORLD) {
            return source == DamageSource.OUT_OF_WORLD && super.attackEntityFrom(source, amount);
        }
        NorsePose pose = pose();
        if (pose == NorsePose.DEAD || pose == NorsePose.FALLEN || pose == NorsePose.RISE) {
            return false;
        }
        if (mortal) {
            return super.attackEntityFrom(source, amount);
        }
        if (source.getTrueSource() instanceof EntityPlayerMP && hunting instanceof EntityNorse && !warden) {
            // Bound to the oath-breaker, they barely notice anyone else.
            amount *= 0.5F;
        }
        if (amount >= getHealth()) {
            setHealth(1F);
            hurtResistantTime = 20;
            world.setEntityState(this, (byte) 2);
            sound(SoundEvents.ENTITY_SKELETON_DEATH, 1F, 0.6F);
            sound(SoundEvents.BLOCK_GRAVEL_BREAK, 1F, 0.5F);
            getNavigator().clearPath();
            setPose(NorsePose.FALLEN);
            if (warden && source.getTrueSource() instanceof EntityPlayer) {
                huntedFor = Integer.MAX_VALUE / 2;
            }
            return true;
        }
        return super.attackEntityFrom(source, amount);
    }

    @Override
    public void onDeath(DamageSource cause) {
        if (cause == DamageSource.OUT_OF_WORLD || mortal) {
            super.onDeath(cause);
            return;
        }
        setHealth(1F);
        setPose(NorsePose.FALLEN);
    }

    private void sound(SoundEvent sound, float volume, float pitch) {
        world.playSound(null, posX, posY + 1D, posZ, sound, SoundCategory.HOSTILE, volume, pitch);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_SKELETON_HURT;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected float getSoundPitch() {
        return 0.6F;
    }

    @Override
    public boolean canBePushed() {
        return isAwake();
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
    public float getEyeHeight() {
        return pose().lying() ? 0.25F : 1.6F;
    }
}
