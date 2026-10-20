package com.matoon.eternity.world;

import com.matoon.eternity.EternityConfig;
import com.matoon.eternity.EternitySounds;
import com.matoon.eternity.compat.InfinityStones;
import com.matoon.eternity.entity.EntityEternity;
import com.matoon.eternity.entity.EternityDamage;
import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.network.MsgCue;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * A black hole. It swells for fifteen seconds, dragging in everything around it and
 * devouring the ground shell by shell, holds, then collapses and detonates.
 */
public final class Singularity {

    public static final int GROW_TICKS = 320;
    public static final int HOLD_TICKS = 100;
    public static final int COLLAPSE_TICKS = 16;
    public static final int LIFETIME = GROW_TICKS + HOLD_TICKS + COLLAPSE_TICKS;

    final WorldServer world;
    final Vec3d centre;
    final double maxRadius;
    private final WeakReference<Entity> source;
    private final List<EntityFallingBlock> debris = new ArrayList<>();
    private int age;
    private int eaten;

    public Singularity(WorldServer world, Vec3d centre, double maxRadius, Entity source) {
        this.world = world;
        this.centre = centre;
        this.maxRadius = maxRadius;
        this.source = new WeakReference<>(source);
    }

    /** Eternity, the one who opened it, and spectators are untouched. */
    private boolean immune(Entity entity) {
        return entity instanceof EntityEternity || entity == source.get()
                || entity instanceof EntityPlayer && ((EntityPlayer) entity).isSpectator();
    }

    /** Shared with the client so both agree on the size at every moment. */
    public static double radius(double maxRadius, float age) {
        if (age < GROW_TICKS) {
            return Math.max(1.2D, maxRadius * Math.pow(age / GROW_TICKS, 0.7D));
        }
        if (age < GROW_TICKS + HOLD_TICKS) {
            return maxRadius;
        }
        float t = Math.min(1F, (age - GROW_TICKS - HOLD_TICKS) / (float) COLLAPSE_TICKS);
        return Math.max(0.5D, maxRadius * (1D - t));
    }

    public boolean isDone() {
        return age > LIFETIME;
    }

    void tick() {
        double r = radius(maxRadius, age);
        DamageSource damage = EternityDamage.collapse(source.get());
        pull(r, damage);
        if (Terrain.enabled() && age < GROW_TICKS + HOLD_TICKS) {
            devour(Math.min(r, Terrain.MAX_RADIUS));
        }
        if (age % 40 == 0) {
            world.playSound(null, centre.x, centre.y, centre.z, EternitySounds.RIFT_IMPACT, SoundCategory.HOSTILE,
                    (float) (4D + r * 0.2D), 0.4F);
        }
        if (age == LIFETIME) {
            detonate(damage);
        }
        age++;
    }

    private void pull(double r, DamageSource damage) {
        double reach = r * 3.5D + 8D;
        AxisAlignedBB box = new AxisAlignedBB(centre.x - reach, centre.y - reach, centre.z - reach,
                centre.x + reach, centre.y + reach, centre.z + reach);
        for (Entity entity : world.getEntitiesWithinAABB(Entity.class, box)) {
            if (immune(entity)) {
                continue;
            }
            Vec3d to = centre.subtract(entity.posX, entity.posY + entity.height / 2D, entity.posZ);
            double d = to.length();
            if (d > reach || d < 1.0E-3) {
                continue;
            }
            double strength = 0.06D + 0.4D * (1D - d / reach) * (1D - d / reach);
            Vec3d pull = to.scale(strength / d);
            entity.motionX += pull.x;
            entity.motionY += pull.y + 0.03D;
            entity.motionZ += pull.z;
            entity.velocityChanged = true;
            entity.fallDistance = 0F;
            if (d < r) {
                if (entity instanceof EntityLivingBase) {
                    if (age % 10 == 0) {
                        EntityLivingBase living = (EntityLivingBase) entity;
                        float fraction = InfinityStones.scan(living) == InfinityStones.ALL ? 0.08F : 0.35F;
                        living.attackEntityFrom(damage, living.getMaxHealth() * fraction);
                    }
                } else if (d < r * 0.6D) {
                    entity.setDead();
                }
            }
        }
        debris.removeIf(block -> block.isDead);
    }

    /** Eats outward from the centre, nearest blocks first, within the per-tick budget. */
    private void devour(double r) {
        int[] sphere = Terrain.sphere();
        int budget = EternityConfig.combat.singularityBlocksPerTick;
        int scanned = 0;
        double r2 = r * r;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int cx = (int) Math.floor(centre.x), cy = (int) Math.floor(centre.y), cz = (int) Math.floor(centre.z);
        while (eaten < sphere.length && budget > 0 && scanned < budget * 6) {
            int p = sphere[eaten];
            int x = Terrain.unpackX(p), y = Terrain.unpackY(p), z = Terrain.unpackZ(p);
            if (x * x + y * y + z * z > r2) {
                break;
            }
            eaten++;
            scanned++;
            int by = cy + y;
            if (by < 1 || by > 255) {
                continue;
            }
            pos.setPos(cx + x, by, cz + z);
            if (debris.size() < 40 && world.rand.nextInt(30) == 0) {
                // A few blocks are torn loose and spiral in instead of vanishing.
                IBlockState state = world.getBlockState(pos);
                if (world.isBlockLoaded(pos) && state.getBlockHardness(world, pos) >= 0F && !state.getBlock().hasTileEntity(state)
                        && state.getMaterial().blocksMovement()) {
                    EntityFallingBlock block = new EntityFallingBlock(world, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, state);
                    block.shouldDropItem = false;
                    block.fallTime = 1;
                    block.setNoGravity(true);
                    world.setBlockToAir(pos);
                    world.spawnEntity(block);
                    debris.add(block);
                    budget--;
                    continue;
                }
            }
            if (Terrain.erase(world, pos) != null) {
                budget--;
            }
        }
    }

    private void detonate(DamageSource damage) {
        double r = maxRadius;
        world.newExplosion(source.get(), centre.x, centre.y, centre.z, (float) Math.min(14D, 5D + r * 0.2D), false, Terrain.enabled());
        double reach = r * 4D;
        AxisAlignedBB box = new AxisAlignedBB(centre.x - reach, centre.y - reach, centre.z - reach,
                centre.x + reach, centre.y + reach, centre.z + reach);
        for (Entity entity : world.getEntitiesWithinAABB(Entity.class, box)) {
            if (immune(entity)) {
                continue;
            }
            Vec3d away = entity.getPositionVector().subtract(centre);
            double d = Math.max(1D, away.length());
            if (d > reach) {
                continue;
            }
            Vec3d push = away.scale((3.5D * (1D - d / reach)) / d);
            entity.motionX += push.x;
            entity.motionY += push.y + 1.2D * (1D - d / reach);
            entity.motionZ += push.z;
            entity.velocityChanged = true;
            if (entity instanceof EntityLivingBase && d < r * 1.5D) {
                EntityLivingBase living = (EntityLivingBase) entity;
                float fraction = InfinityStones.scan(living) == InfinityStones.ALL ? 0.25F : 1F;
                living.attackEntityFrom(damage, living.getMaxHealth() * fraction);
            }
        }
        for (EntityFallingBlock block : debris) {
            block.setDead();
        }
        debris.clear();
        EternityNetwork.sendAround(new MsgCue(MsgCue.SINGULARITY_BURST, -1, centre.x, centre.y, centre.z, r, 0, 0),
                world.provider.getDimension(), centre.x, centre.y, centre.z, 2000D);
    }
}
