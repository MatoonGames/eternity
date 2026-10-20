package com.matoon.eternity.entity;

import com.matoon.eternity.EternitySounds;
import com.matoon.eternity.world.Director;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * The Astral Regulator in flight: it rises from the hand that released it, drifts
 * into the heart of the dying god, and drinks back everything he is pouring out.
 */
public class EntityAstralRegulator extends Entity {

    public static final int FLIGHT_TICKS = 70;
    public static final int ABSORB_TICKS = 360;

    private static final DataParameter<Integer> TARGET = EntityDataManager.createKey(EntityAstralRegulator.class, DataSerializers.VARINT);
    private static final DataParameter<Float> PROGRESS = EntityDataManager.createKey(EntityAstralRegulator.class, DataSerializers.FLOAT);
    private static final DataParameter<Boolean> ABSORBING = EntityDataManager.createKey(EntityAstralRegulator.class, DataSerializers.BOOLEAN);

    private Vec3d start;
    private Vec3d goal;
    private int age;
    private int absorbed;

    public EntityAstralRegulator(World world) {
        super(world);
        setSize(0.6F, 0.6F);
        noClip = true;
        ignoreFrustumCheck = true;
        setNoGravity(true);
    }

    public EntityAstralRegulator(World world, Vec3d from, EntityEternity target) {
        this(world);
        setPosition(from.x, from.y, from.z);
        start = from;
        goal = target.chest();
        dataManager.set(TARGET, target.getEntityId());
    }

    @Override
    protected void entityInit() {
        dataManager.register(TARGET, -1);
        dataManager.register(PROGRESS, 0F);
        dataManager.register(ABSORBING, false);
    }

    public float getProgress() {
        return dataManager.get(PROGRESS);
    }

    public boolean isAbsorbing() {
        return dataManager.get(ABSORBING);
    }

    @Nullable
    public EntityEternity getTarget() {
        Entity entity = world.getEntityByID(dataManager.get(TARGET));
        return entity instanceof EntityEternity ? (EntityEternity) entity : null;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        prevPosX = posX;
        prevPosY = posY;
        prevPosZ = posZ;
        age++;
        if (world.isRemote) {
            spawnInflow();
            return;
        }
        if (start == null) {
            setDead();
            return;
        }
        EntityEternity target = getTarget();
        if (target != null && !target.isDead) {
            goal = target.chest();
        }
        if (!isAbsorbing()) {
            float t = MathHelper.clamp(age / (float) FLIGHT_TICKS, 0F, 1F);
            float eased = t * t * (3F - 2F * t);
            Vec3d along = start.add(goal.subtract(start).scale(eased));
            double lift = Math.sin(t * Math.PI) * 3D;
            setPosition(along.x, along.y + lift, along.z);
            if (age >= FLIGHT_TICKS) {
                dataManager.set(ABSORBING, true);
                world.playSound(null, posX, posY, posZ, EternitySounds.RIFT_OPEN, SoundCategory.MASTER, 10F, 1.6F);
                Director.INSTANCE.containmentBegan();
            }
            return;
        }
        absorbed++;
        float progress = Math.min(1F, absorbed / (float) ABSORB_TICKS);
        dataManager.set(PROGRESS, progress);
        setPosition(goal.x, goal.y + Math.sin(age * 0.1D) * 0.15D, goal.z);
        if (target != null) {
            target.setContainment(progress);
        }
        if (absorbed % 40 == 0) {
            world.playSound(null, posX, posY, posZ, EternitySounds.RIFT_IMPACT, SoundCategory.MASTER, 6F, 0.5F + progress);
        }
        if (progress >= 1F) {
            Director.INSTANCE.containmentComplete(getPositionVector());
            setDead();
        }
    }

    /** Light from every direction streaming into the crystal. */
    private void spawnInflow() {
        int count = isAbsorbing() ? 18 : 3;
        float reach = isAbsorbing() ? 18F : 2F;
        for (int i = 0; i < count; i++) {
            double dx = (rand.nextDouble() - 0.5D) * 2D * reach;
            double dy = (rand.nextDouble() - 0.5D) * 2D * reach;
            double dz = (rand.nextDouble() - 0.5D) * 2D * reach;
            // Portal particles travel from their offset back to where they were spawned.
            world.spawnParticle(EnumParticleTypes.PORTAL, posX, posY, posZ, dx, dy, dz);
        }
        if (isAbsorbing() && rand.nextBoolean()) {
            world.spawnParticle(EnumParticleTypes.END_ROD, posX + (rand.nextDouble() - 0.5D) * 30D,
                    posY + (rand.nextDouble() - 0.5D) * 30D, posZ + (rand.nextDouble() - 0.5D) * 30D,
                    0D, 0D, 0D);
        }
    }

    @Override
    public boolean isInRangeToRenderDist(double distance) {
        return true;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean writeToNBTOptional(NBTTagCompound compound) {
        return false;
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {
    }
}
