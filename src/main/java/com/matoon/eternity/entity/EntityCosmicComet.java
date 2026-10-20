package com.matoon.eternity.entity;

import com.matoon.eternity.world.CometDirector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.BossInfo;
import net.minecraft.world.BossInfoServer;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

/**
 * The body of the Cosmic Comet: something to strike, and nothing more. The
 * {@link CometDirector} moves it, burns everything around it and decides what every
 * blow does. It is never saved with the chunk, and it is drawn by the comet's own
 * renderer rather than as an entity.
 */
public class EntityCosmicComet extends EntityLiving {

    private static final DataParameter<Float> RADIUS = EntityDataManager.createKey(EntityCosmicComet.class, DataSerializers.FLOAT);

    private final BossInfoServer bossInfo = (BossInfoServer) new BossInfoServer(
            new TextComponentString("Cosmic Comet"), BossInfo.Color.BLUE, BossInfo.Overlay.NOTCHED_10)
            .setDarkenSky(false);

    public EntityCosmicComet(World world) {
        super(world);
        setSize(2F, 2F);
        noClip = true;
        isImmuneToFire = true;
        experienceValue = 0;
        ignoreFrustumCheck = true;
        setNoGravity(true);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(RADIUS, 1F);
    }

    @Override
    public void notifyDataManagerChange(DataParameter<?> key) {
        super.notifyDataManagerChange(key);
        if (RADIUS.equals(key)) {
            float r = getRadius();
            setSize(r * 2F, r * 2F);
            // Entity lookups only search this far past the chunks a box touches. Without
            // this, blows and beams aimed at its edge would never find it.
            World.MAX_ENTITY_RADIUS = Math.max(World.MAX_ENTITY_RADIUS, r + 2D);
        }
    }

    public float getRadius() {
        return dataManager.get(RADIUS);
    }

    public void setRadius(float radius) {
        dataManager.set(RADIUS, radius);
    }

    public Vec3d centre() {
        return new Vec3d(posX, posY + height / 2D, posZ);
    }

    public void setIntegrity(float integrity) {
        bossInfo.setPercent(integrity);
    }

    @Override
    public void onLivingUpdate() {
        noClip = true;
        motionX = motionY = motionZ = 0;
        if (!world.isRemote && !CometDirector.INSTANCE.claim(this)) {
            setDead();
            return;
        }
        super.onLivingUpdate();
    }

    @Override
    public void travel(float strafe, float vertical, float forward) {
        move(MoverType.SELF, motionX, motionY, motionZ);
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (world.isRemote || isEntityInvulnerable(source)) {
            return false;
        }
        return CometDirector.INSTANCE.strike(this, source, amount);
    }

    @Override
    public void onKillCommand() {
        // Commands cannot break it. Only blows can.
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
    public boolean canBeCollidedWith() {
        return true;
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
    public boolean canRenderOnFire() {
        return false;
    }

    @Override
    protected boolean canDespawn() {
        return false;
    }

    @Override
    public boolean isPushedByWater() {
        return false;
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Nullable
    @Override
    public Entity changeDimension(int dimension, ITeleporter teleporter) {
        return null;
    }

    /** Never written to chunks; {@link com.matoon.eternity.world.CometData} remembers it. */
    @Override
    public boolean writeToNBTOptional(NBTTagCompound compound) {
        return false;
    }

    @Override
    public boolean isInRangeToRenderDist(double distance) {
        return true;
    }

    @Override
    public SoundCategory getSoundCategory() {
        return SoundCategory.HOSTILE;
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
        return getEntityBoundingBox().grow(width * 4D);
    }
}
