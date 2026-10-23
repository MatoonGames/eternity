package com.matoon.eternity.entity;

import com.matoon.eternity.world.InBetweenerDirector;
import com.matoon.eternity.world.Nexus;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

/**
 * The In-Betweener: a man no bigger than you, split down the middle into white and black,
 * hanging in the air inside his crystal bubble at the point of absolute neutrality. He is
 * a puppet of {@link InBetweenerDirector}, which moves him, poses him and speaks for him;
 * he only exists while someone is near enough to see him. Nothing harms him, inside his
 * glass or out. Outside it, nothing needs to.
 *
 * What everyone can see is synced: how he holds himself, whether the Soul Gem still burns
 * on his brow, how big his bubble is and how cracked, and how much of his colour the
 * neutral nothing has drained out of him.
 */
public class EntityInBetweener extends EntityLiving {

    public enum State {
        /** Floating cross-legged in the middle of his glass. */
        HOVER,
        /** Talking, with his hands. */
        SPEAK,
        /** Both palms pressed to the inside of the glass, pushing. */
        PRESS,
        /** Arms flung wide: the glass is gone. */
        FREE,
        /** Walking out into the nothing. */
        STEP,
        /** Lying grey and powerless outside. */
        FALLEN,
        /** Everything, inside his bubble, with you. */
        WARP,
        /** Unused: he is never put back into glass. Kept so the synced state numbers stay the same. */
        RISE,
        /** Risen to his full height, arms wide. */
        ASCEND,
        /** Rapping a knuckle on the inside of his glass. */
        TAP,
        /** Beckoning you closer. */
        BECKON,
        /** Holding the gem out against the glass. */
        OFFER,
        /** Laughing. */
        LAUGH,
        /** Being drawn into an Astral Regulator, and shrinking away. */
        ABSORBING,
        /** Empty and grey, cast off into the Nexus, turning slowly in the void. */
        ADRIFT
    }

    private static final DataParameter<Byte> STATE = EntityDataManager.createKey(EntityInBetweener.class, DataSerializers.BYTE);
    private static final DataParameter<Boolean> GEM = EntityDataManager.createKey(EntityInBetweener.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Float> BUBBLE = EntityDataManager.createKey(EntityInBetweener.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> CRACK = EntityDataManager.createKey(EntityInBetweener.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> DRAIN = EntityDataManager.createKey(EntityInBetweener.class, DataSerializers.FLOAT);

    /** Client: ticks since the synced state last changed. */
    public int clientStateTicks;
    /** Client: the bubble's radius and crack as last drawn, eased toward the synced values. */
    public float shownBubble = (float) Nexus.BUBBLE_RADIUS, shownCrack;

    public EntityInBetweener(World world) {
        super(world);
        setSize(0.6F, 1.8F);
        noClip = true;
        isImmuneToFire = true;
        experienceValue = 0;
        setNoGravity(true);
        setEntityInvulnerable(true);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(STATE, (byte) State.HOVER.ordinal());
        dataManager.register(GEM, true);
        dataManager.register(BUBBLE, (float) Nexus.BUBBLE_RADIUS);
        dataManager.register(CRACK, 0F);
        dataManager.register(DRAIN, 0F);
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
        }
    }

    // ------------------------------------------------------------------ what everyone sees

    public State getState() {
        int ordinal = dataManager.get(STATE);
        return ordinal >= 0 && ordinal < State.values().length ? State.values()[ordinal] : State.HOVER;
    }

    public void setState(State state) {
        if (getState() != state) {
            dataManager.set(STATE, (byte) state.ordinal());
        }
    }

    public boolean hasGem() {
        return dataManager.get(GEM);
    }

    public void setGem(boolean gem) {
        dataManager.set(GEM, gem);
    }

    /** His bubble's radius; 0 once it is broken. */
    public float getBubble() {
        return dataManager.get(BUBBLE);
    }

    public void setBubble(float radius) {
        dataManager.set(BUBBLE, radius);
    }

    /** How near his glass is to breaking, 0 to 1. */
    public float getCrack() {
        return dataManager.get(CRACK);
    }

    public void setCrack(float crack) {
        dataManager.set(CRACK, MathHelper.clamp(crack, 0F, 1F));
    }

    /** How much of his black and white the neutral nothing has drained into grey, 0 to 1. */
    public float getDrain() {
        return dataManager.get(DRAIN);
    }

    public void setDrain(float drain) {
        dataManager.set(DRAIN, MathHelper.clamp(drain, 0F, 1F));
    }

    /** The middle of his glass, where it always is. */
    public static Vec3d bubbleCentre() {
        return new Vec3d(Nexus.JUNCTION_X, Nexus.BUBBLE_Y, Nexus.JUNCTION_Z);
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void onLivingUpdate() {
        noClip = true;
        motionX = motionY = motionZ = 0D;
        if (!world.isRemote) {
            if (!InBetweenerDirector.INSTANCE.claim(this)) {
                setDead();
                return;
            }
        } else {
            clientStateTicks++;
            shownBubble += (getBubble() - shownBubble) * 0.12F;
            shownCrack += (getCrack() - shownCrack) * 0.25F;
        }
        super.onLivingUpdate();
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        renderYawOffset = rotationYaw;
        prevRenderYawOffset = prevRotationYaw;
    }

    @Override
    public void travel(float strafe, float vertical, float forward) {
        move(MoverType.SELF, motionX, motionY, motionZ);
    }

    /** Turns him, a little at a time, to look at someone. */
    public void face(Entity target, float rate) {
        double dx = target.posX - posX;
        double dz = target.posZ - posZ;
        double dy = target.posY + target.getEyeHeight() - (posY + getEyeHeight());
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90F;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        rotationYaw += MathHelper.wrapDegrees(yaw - rotationYaw) * rate;
        rotationYawHead = rotationYaw;
        rotationPitch += (pitch - rotationPitch) * rate;
    }

    /** Client: whether he lies fallen or drifts near this player, so a raised Regulator may draw him in. */
    public static boolean fallenNear(EntityPlayer player) {
        for (Entity entity : player.world.loadedEntityList) {
            if (entity instanceof EntityInBetweener && ((EntityInBetweener) entity).isEmpty() && entity.getDistance(player) < 12F) {
                return true;
            }
        }
        return false;
    }

    /** Turns him, a little at a time, to put his back to someone. */
    public void faceAway(Entity target, float rate) {
        double dx = posX - target.posX;
        double dz = posZ - target.posZ;
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90F;
        rotationYaw += MathHelper.wrapDegrees(yaw - rotationYaw) * rate;
        rotationYawHead = rotationYaw;
    }

    /** Glides toward a point, a fraction of the way each tick. */
    public void glide(Vec3d to, double rate) {
        setPosition(posX + (to.x - posX) * rate, posY + (to.y - posY) * rate, posZ + (to.z - posZ) * rate);
    }

    // ------------------------------------------------------------------ nothing touches him

    /** Blows pass between one moment and the next and never arrive; the director answers them. */
    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (!world.isRemote && source.getTrueSource() instanceof EntityPlayerMP) {
            InBetweenerDirector.INSTANCE.struck((EntityPlayerMP) source.getTrueSource());
        }
        return false;
    }

    /** Fallen, he can be reached; and the gem on his brow can be taken. */
    @Override
    protected boolean processInteract(EntityPlayer player, EnumHand hand) {
        if (!world.isRemote && hand == EnumHand.MAIN_HAND && player instanceof EntityPlayerMP) {
            return InBetweenerDirector.INSTANCE.touched((EntityPlayerMP) player);
        }
        return isEmpty();
    }

    /** Fallen outside his broken glass, or drifting in the Nexus: either way, nothing left in him. */
    public boolean isEmpty() {
        return getState() == State.FALLEN || getState() == State.ADRIFT;
    }

    @Override
    public boolean isEntityInvulnerable(DamageSource source) {
        return true;
    }

    @Override
    public void onKillCommand() {
        // He is put away, not killed; the director brings him back whenever someone is near.
        setDead();
    }

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

    /** Never written to chunks; the director puts him back whenever someone comes near. */
    @Override
    public boolean writeToNBTOptional(NBTTagCompound compound) {
        return false;
    }

    @Override
    public boolean isInRangeToRenderDist(double distance) {
        return distance < 256D * 256D;
    }

    @Override
    public SoundCategory getSoundCategory() {
        return SoundCategory.HOSTILE;
    }

    @Override
    public float getEyeHeight() {
        return isEmpty() ? 0.3F : 1.62F;
    }

    /** His bubble is drawn with him, and is far bigger than he is. */
    @SideOnly(Side.CLIENT)
    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        if (getBubble() <= 0F && shownBubble < 0.05F) {
            return getEntityBoundingBox().grow(1.5D);
        }
        double r = Math.max(shownBubble, 2F) + 2D;
        Vec3d c = bubbleCentre();
        return new AxisAlignedBB(c.x - r, c.y - r, c.z - r, c.x + r, c.y + r, c.z + r).union(getEntityBoundingBox());
    }
}
