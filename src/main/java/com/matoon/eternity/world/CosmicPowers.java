package com.matoon.eternity.world;

import com.matoon.eternity.EternityConfig;
import com.matoon.eternity.EternitySounds;
import com.matoon.eternity.ability.AbilityRiftGate;
import com.matoon.eternity.entity.EntityDestroyerArmor;
import com.matoon.eternity.entity.EternityDamage;
import com.matoon.eternity.network.MsgCue;
import lucraft.mods.lucraftcore.superpowers.abilities.Ability;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import java.lang.ref.WeakReference;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Eternity's powers, wielded by someone holding an Astral Regulator that absorbed
 * him. Same shapes as his own attacks, scaled for a mortal hand: fixed damage rather
 * than a share of the victim's life, and the wielder is never caught in them.
 */
public final class CosmicPowers {

    public static final CosmicPowers INSTANCE = new CosmicPowers();

    private final Random rand = new Random();
    private final List<Strike> strikes = new ArrayList<>();

    private enum Kind { LANCE, STAR, ERUPT }

    private static final class Strike {
        final WorldServer world;
        final WeakReference<EntityLivingBase> caster;
        final Kind kind;
        final Vec3d from, to;
        int delay;

        Strike(WorldServer world, EntityLivingBase caster, Kind kind, Vec3d from, Vec3d to, int delay) {
            this.world = world;
            this.caster = new WeakReference<>(caster);
            this.kind = kind;
            this.from = from;
            this.to = to;
            this.delay = delay;
        }
    }

    private CosmicPowers() {
    }

    public void reset() {
        strikes.clear();
    }

    /**
     * Where the caster is looking: the first living thing in the way, the first block hit,
     * or a point at full range. Without the first, anything in the air (Adam Warlock, most
     * of all) could never be aimed at: the powers went straight past it to the ground.
     */
    public static Vec3d aim(EntityLivingBase caster, double range) {
        Vec3d eyes = caster.getPositionEyes(1F);
        Vec3d end = eyes.add(caster.getLook(1F).scale(range));
        RayTraceResult hit = caster.world.rayTraceBlocks(eyes, end, false, true, false);
        Vec3d wall = hit != null && hit.hitVec != null ? hit.hitVec : end;
        EntityLivingBase target = sighted(caster, eyes.distanceTo(wall));
        return target != null ? target.getPositionVector().add(0D, target.height / 2D, 0D) : wall;
    }

    /**
     * The nearest living thing under the crosshair, forgivingly: its box is grown by a block,
     * so something small and fast still counts. Null if there is none before a wall or range.
     */
    @Nullable
    public static EntityLivingBase sighted(EntityLivingBase caster, double range) {
        Vec3d eyes = caster.getPositionEyes(1F);
        Vec3d look = caster.getLook(1F);
        Vec3d end = eyes.add(look.scale(range));
        RayTraceResult wall = caster.world.rayTraceBlocks(eyes, end, false, true, false);
        double reach = wall != null && wall.hitVec != null ? eyes.distanceTo(wall.hitVec) : range;
        AxisAlignedBB sweep = caster.getEntityBoundingBox().expand(look.x * reach, look.y * reach, look.z * reach).grow(1.5D);
        EntityLivingBase best = null;
        double bestDistance = reach;
        for (EntityLivingBase entity : caster.world.getEntitiesWithinAABB(EntityLivingBase.class, sweep, e -> isVictim(caster, e))) {
            AxisAlignedBB box = entity.getEntityBoundingBox().grow(1D);
            RayTraceResult through = box.calculateIntercept(eyes, end);
            double distance = box.contains(eyes) ? 0D : through != null ? eyes.distanceTo(through.hitVec) : Double.MAX_VALUE;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = entity;
            }
        }
        return best;
    }

    // ------------------------------------------------------------------ the powers

    /** A touch that unmakes whatever you reach for, and hurls everything around it. */
    public boolean unmake(EntityLivingBase caster) {
        EntityLivingBase victim = lookedAt(caster, 14D);
        if (victim == null) {
            return false;
        }
        victim.attackEntityFrom(EternityDamage.wielded("unmade", caster), 40F);
        Vec3d push = victim.getPositionVector().subtract(caster.getPositionVector()).normalize().scale(2.4D);
        EntityDestroyerArmor.shove(victim, push.x, 0.9D, push.z);
        hurtAround(caster, victim.getPositionVector(), 5D, EternityDamage.wielded("unmade", caster), 10F, 0.9D);
        Director.INSTANCE.cue(caster, new MsgCue(MsgCue.UNMAKE, caster.getEntityId(), victim.posX, victim.posY + victim.height / 2, victim.posZ, 0, 0, 0));
        caster.world.playSound(null, victim.posX, victim.posY, victim.posZ, EternitySounds.RIFT_IMPACT, SoundCategory.PLAYERS, 3F, 1.1F);
        return true;
    }

    /** Rifts open around the target and fire lances of void that carve the ground. */
    public boolean lances(EntityLivingBase caster) {
        Vec3d target = aim(caster, 80D);
        for (int i = 0; i < 6; i++) {
            double angle = rand.nextDouble() * Math.PI * 2D;
            double r = 8D + rand.nextDouble() * 8D;
            Vec3d from = new Vec3d(target.x + Math.cos(angle) * r, target.y + 6D + rand.nextDouble() * 10D, target.z + Math.sin(angle) * r);
            Vec3d to = from.add(target.subtract(from).scale(2.2D));
            schedule(caster, Kind.LANCE, from, to, 14 + i * 3);
            Director.INSTANCE.cue(caster, new MsgCue(MsgCue.LANCE_WARN, caster.getEntityId(), from.x, from.y, from.z, to.x, to.y, to.z));
        }
        return true;
    }

    /** Stars rain down on wherever you look. */
    public boolean starfall(EntityLivingBase caster) {
        Vec3d target = aim(caster, 96D);
        for (int i = 0; i < 14; i++) {
            double spread = i < 2 ? 1.5D : 14D;
            double gx = target.x + (rand.nextDouble() - 0.5D) * 2D * spread;
            double gz = target.z + (rand.nextDouble() - 0.5D) * 2D * spread;
            Vec3d ground = new Vec3d(gx, groundY(caster, gx, target.y, gz), gz);
            Vec3d sky = ground.add((rand.nextDouble() - 0.5D) * 30D, 110D, (rand.nextDouble() - 0.5D) * 30D);
            schedule(caster, Kind.STAR, sky, ground, 24 + i * 3 + rand.nextInt(5));
            Director.INSTANCE.cue(caster, new MsgCue(MsgCue.STAR_WARN, caster.getEntityId(), ground.x, ground.y, ground.z, sky.x, sky.y, sky.z));
        }
        return true;
    }

    /** The ground breaks apart in rings racing outward from where you look. */
    public boolean shatter(EntityLivingBase caster) {
        Vec3d origin = aim(caster, 48D);
        for (int ring = 0; ring < 4; ring++) {
            double radius = 3D + ring * 5D;
            int points = 6 + ring * 3;
            for (int p = 0; p < points; p++) {
                double angle = Math.PI * 2D * p / points + ring * 0.4D;
                double gx = origin.x + Math.cos(angle) * radius;
                double gz = origin.z + Math.sin(angle) * radius;
                Vec3d ground = new Vec3d(gx, groundY(caster, gx, origin.y, gz), gz);
                schedule(caster, Kind.ERUPT, ground, ground.add(0, 26D, 0), 6 + ring * 6);
            }
        }
        return true;
    }

    /** A black hole, smaller than his, at the point you look at. */
    public boolean singularity(EntityLivingBase caster) {
        if (!(caster.world instanceof WorldServer)) {
            return false;
        }
        Vec3d centre = aim(caster, 64D).add(0, 3D, 0);
        if (centre.distanceTo(caster.getPositionVector()) < 12D) {
            // Too close: it would swallow its own maker.
            return false;
        }
        double radius = EternityConfig.combat.singularityRadius * 0.6D;
        Director.INSTANCE.addSingularity(new Singularity((WorldServer) caster.world, centre, radius, caster));
        Director.INSTANCE.cue(caster, new MsgCue(MsgCue.SINGULARITY, caster.getEntityId(), centre.x, centre.y, centre.z, radius, 0, 0));
        return true;
    }

    /** Step through a rift to wherever you are looking. */
    public boolean riftStep(EntityLivingBase caster) {
        Vec3d target = aim(caster, 80D);
        Vec3d back = caster.getLook(1F).scale(-1.2D);
        Vec3d land = target.add(back);
        double y = Terrain.safeY(caster.world, land.x, land.y, land.z);
        Vec3d from = caster.getPositionVector();
        if (caster instanceof EntityPlayerMP) {
            ((EntityPlayerMP) caster).connection.setPlayerLocation(land.x, y, land.z, caster.rotationYaw, caster.rotationPitch);
        } else {
            caster.setPositionAndUpdate(land.x, y, land.z);
        }
        caster.fallDistance = 0F;
        Director.INSTANCE.cue(caster, new MsgCue(MsgCue.BLINK, caster.getEntityId(), from.x, from.y + 1D, from.z, land.x, y + 1D, land.z));
        caster.world.playSound(null, from.x, from.y, from.z, EternitySounds.ENTITY_GLITCH, SoundCategory.PLAYERS, 2F, 0.8F);
        caster.world.playSound(null, land.x, y, land.z, EternitySounds.RIFT_CLOSE, SoundCategory.PLAYERS, 2F, 1.4F);
        return true;
    }

    /**
     * Opens a Rift Gate where the player asked, if they really can: they must hold a
     * Regulator granting the ability, it must be off cooldown, and the destination
     * must exist. Requests come from the client, so nothing in them is trusted.
     */
    public void riftGate(EntityPlayerMP player, int dimension, double x, double y, double z) {
        AbilityRiftGate gate = null;
        for (Ability ability : Ability.getAbilities(player)) {
            if (ability instanceof AbilityRiftGate) {
                gate = (AbilityRiftGate) ability;
                break;
            }
        }
        if (gate == null || gate.isCoolingdown() || player.isSpectator()) {
            return;
        }
        if (!DimensionManager.isDimensionRegistered(dimension)) {
            refuse(player, "gui.eternity.rift_gate.no_dimension");
            return;
        }
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                || Math.abs(x) > 29_999_000D || Math.abs(z) > 29_999_000D) {
            refuse(player, "gui.eternity.rift_gate.out_of_bounds");
            return;
        }
        double safeY = Math.max(1D, Math.min(255D, y));
        Director.INSTANCE.openGate(player, new Director.Destination(dimension, x, safeY, z));
        gate.spend();
    }

    private static void refuse(EntityPlayerMP player, String key) {
        TextComponentTranslation text = new TextComponentTranslation(key);
        text.getStyle().setColor(TextFormatting.DARK_PURPLE);
        player.sendStatusMessage(text, true);
    }

    // ------------------------------------------------------------------ strikes

    private void schedule(EntityLivingBase caster, Kind kind, Vec3d from, Vec3d to, int delay) {
        if (caster.world instanceof WorldServer) {
            strikes.add(new Strike((WorldServer) caster.world, caster, kind, from, to, delay));
        }
    }

    void tick() {
        if (strikes.isEmpty()) {
            return;
        }
        List<Strike> due = new ArrayList<>();
        Iterator<Strike> it = strikes.iterator();
        while (it.hasNext()) {
            Strike strike = it.next();
            if (--strike.delay <= 0) {
                due.add(strike);
                it.remove();
            }
        }
        for (Strike strike : due) {
            EntityLivingBase caster = strike.caster.get();
            if (caster != null && caster.world == strike.world) {
                land(strike, caster);
            }
        }
    }

    private void land(Strike strike, EntityLivingBase caster) {
        WorldServer world = strike.world;
        switch (strike.kind) {
            case LANCE: {
                Director.INSTANCE.cue(caster, new MsgCue(MsgCue.LANCE_FIRE, caster.getEntityId(), strike.from.x, strike.from.y, strike.from.z, strike.to.x, strike.to.y, strike.to.z));
                world.playSound(null, strike.from.x, strike.from.y, strike.from.z, EternitySounds.RIFT_CLOSE, SoundCategory.PLAYERS, 3F, 1.4F);
                AxisAlignedBB sweep = new AxisAlignedBB(strike.from.x, strike.from.y, strike.from.z, strike.to.x, strike.to.y, strike.to.z).grow(1.5D);
                for (EntityLivingBase victim : world.getEntitiesWithinAABB(EntityLivingBase.class, sweep, e -> isVictim(caster, e))) {
                    if (victim.getEntityBoundingBox().grow(1D).calculateIntercept(strike.from, strike.to) != null) {
                        victim.attackEntityFrom(EternityDamage.wielded("lance", caster), 24F);
                    }
                }
                Terrain.carveLine(world, strike.from, strike.to, 1.4D, 300);
                break;
            }
            case STAR:
                Director.INSTANCE.cue(caster, new MsgCue(MsgCue.LANCE_FIRE, caster.getEntityId(), strike.from.x, strike.from.y, strike.from.z, strike.to.x, strike.to.y, strike.to.z));
                world.newExplosion(caster, strike.to.x, strike.to.y, strike.to.z, 3.5F, false, Terrain.enabled());
                hurtAround(caster, strike.to, 4.5D, EternityDamage.wielded("starfall", caster), 18F, 1D);
                break;
            case ERUPT:
                Director.INSTANCE.cue(caster, new MsgCue(MsgCue.ERUPT, caster.getEntityId(), strike.from.x, strike.from.y, strike.from.z, strike.to.x, strike.to.y, strike.to.z));
                world.newExplosion(caster, strike.from.x, strike.from.y, strike.from.z, 2.5F, false, Terrain.enabled());
                hurtAround(caster, strike.from, 3.5D, EternityDamage.wielded("shatter", caster), 14F, 1.6D);
                break;
        }
    }

    // ------------------------------------------------------------------ helpers

    private static void hurtAround(EntityLivingBase caster, Vec3d at, double radius, DamageSource source, float damage, double lift) {
        AxisAlignedBB box = new AxisAlignedBB(at.x - radius, at.y - radius, at.z - radius, at.x + radius, at.y + radius, at.z + radius);
        for (EntityLivingBase victim : caster.world.getEntitiesWithinAABB(EntityLivingBase.class, box, e -> isVictim(caster, e))) {
            if (victim.getPositionVector().distanceTo(at) > radius) {
                continue;
            }
            victim.attackEntityFrom(source, damage);
            Vec3d push = victim.getPositionVector().subtract(at);
            push = push.lengthSquared() < 1.0E-4 ? Vec3d.ZERO : push.normalize().scale(1.1D);
            EntityDestroyerArmor.shove(victim, push.x, lift, push.z);
        }
    }

    private static boolean isVictim(EntityLivingBase caster, EntityLivingBase entity) {
        return entity != null && entity != caster && entity.isEntityAlive()
                && !(entity instanceof EntityPlayer && ((EntityPlayer) entity).isSpectator())
                && !EntityDestroyerArmor.sheltered(entity) && entity != caster.getRidingEntity();
    }

    /** The living thing nearest the centre of the caster's view, within range. */
    private static EntityLivingBase lookedAt(EntityLivingBase caster, double range) {
        // Whatever is right under the crosshair first; failing that, whatever is nearest the middle of the view.
        EntityLivingBase sighted = sighted(caster, range);
        if (sighted != null) {
            return sighted;
        }
        Vec3d eyes = caster.getPositionEyes(1F);
        Vec3d look = caster.getLook(1F);
        EntityLivingBase best = null;
        double bestDot = 0.93D;
        AxisAlignedBB box = caster.getEntityBoundingBox().grow(range);
        for (EntityLivingBase entity : caster.world.getEntitiesWithinAABB(EntityLivingBase.class, box, e -> isVictim(caster, e))) {
            Vec3d to = entity.getPositionVector().add(0, entity.height / 2D, 0).subtract(eyes);
            double dist = to.length();
            if (dist > range + entity.width) {
                continue;
            }
            double dot = look.dotProduct(to.normalize());
            if (dot > bestDot && caster.canEntityBeSeen(entity)) {
                bestDot = dot;
                best = entity;
            }
        }
        return best;
    }

    private static double groundY(Entity caster, double x, double nearY, double z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int start = (int) Math.min(255, nearY + 12);
        for (int y = start; y > Math.max(0, start - 60); y--) {
            pos.setPos(x, y, z);
            if (caster.world.isBlockLoaded(pos) && caster.world.getBlockState(pos).getMaterial().blocksMovement()) {
                return y + 1;
            }
        }
        return nearY;
    }
}
