package com.matoon.eternity.world;

import com.matoon.eternity.EternitySounds;
import com.matoon.eternity.entity.EternityDamage;
import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.network.MsgTribunalFx;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * The Living Tribunal's powers, wielded by someone holding an Astral Regulator that
 * absorbed him. The same judgements he used in the Star Chamber, scaled for a mortal
 * hand: fixed damage rather than a share of the victim's life, and never touching the
 * one who wields them.
 */
public final class TribunalPowers {

    public static final TribunalPowers INSTANCE = new TribunalPowers();

    private final Random rand = new Random();
    private final List<Pending> pending = new ArrayList<>();

    /** Part of a power that lands a moment after it is called down. */
    private static final class Pending {
        final WeakReference<EntityLivingBase> caster;
        final Runnable action;
        int delay;

        Pending(EntityLivingBase caster, int delay, Runnable action) {
            this.caster = new WeakReference<>(caster);
            this.delay = delay;
            this.action = action;
        }
    }

    private TribunalPowers() {
    }

    public void reset() {
        pending.clear();
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || pending.isEmpty()) {
            return;
        }
        List<Runnable> due = new ArrayList<>();
        Iterator<Pending> it = pending.iterator();
        while (it.hasNext()) {
            Pending p = it.next();
            EntityLivingBase caster = p.caster.get();
            if (caster == null || caster.isDead) {
                it.remove();
            } else if (--p.delay <= 0) {
                due.add(p.action);
                it.remove();
            }
        }
        for (Runnable action : due) {
            action.run();
        }
    }

    private void later(EntityLivingBase caster, int delay, Runnable action) {
        pending.add(new Pending(caster, delay, action));
    }

    // ------------------------------------------------------------------ the powers

    /** The Gavel: a golden sigil where you look, and then a fist out of nowhere. */
    public boolean gavel(EntityLivingBase caster) {
        Vec3d at = ground(caster.world, CosmicPowers.aim(caster, 48D));
        fx(caster.world, MsgTribunalFx.SIGIL, at, 6D, 0, 0, 0, 20);
        later(caster, 20, () -> slam(caster, at, 6D, 40F));
        return true;
    }

    /** Equity's Gaze: twin beams of white fire from your eyes, following where you look. */
    public boolean gaze(EntityLivingBase caster) {
        sound(caster.world, new Vec3d(caster.posX, caster.posY, caster.posZ), EternitySounds.COMET_BLAST, 3F, 1.2F);
        for (int k = 0; k < 12; k++) {
            later(caster, k * 3, () -> {
                Vec3d from = caster.getPositionEyes(1F);
                Vec3d to = CosmicPowers.aim(caster, 64D);
                fx(caster.world, MsgTribunalFx.GAZE, from, to.x, to.y, to.z, 3, 5);
                for (EntityLivingBase victim : victims(caster, new AxisAlignedBB(from, to).grow(2D))) {
                    if (distanceToSegment(victim.getPositionVector().add(0D, victim.height / 2D, 0D), from, to) < 1.5D) {
                        victim.hurtResistantTime = 0;
                        victim.attackEntityFrom(EternityDamage.wielded("tribunal", caster), 6F);
                        victim.setFire(3);
                    }
                }
            });
        }
        return true;
    }

    /** Necessity's Storm: sigils burn around where you look, and lightning finds every one. */
    public boolean storm(EntityLivingBase caster) {
        Vec3d centre = ground(caster.world, CosmicPowers.aim(caster, 48D));
        for (int i = 0; i < 8; i++) {
            Vec3d at = i == 0 ? centre : ground(caster.world, centre.add((rand.nextDouble() - 0.5D) * 12D, 0D, (rand.nextDouble() - 0.5D) * 12D));
            later(caster, i * 4, () -> {
                fx(caster.world, MsgTribunalFx.SIGIL, at, 2.5D, 0, 0, 0, 12);
                later(caster, 12, () -> {
                    Vec3d from = caster.getPositionEyes(1F).add(0D, 6D, 0D);
                    fx(caster.world, MsgTribunalFx.BOLT, from, at.x, at.y, at.z, rand.nextInt(), 10);
                    sound(caster.world, at, SoundEvents.ENTITY_LIGHTNING_THUNDER, 3F, 1F + rand.nextFloat() * 0.3F);
                    hurt(caster, at, 3D, 18F, 0D);
                });
            });
        }
        return true;
    }

    /** The Scales of Balance: two pans of light come down either side of where you look, one after the other. */
    public boolean scales(EntityLivingBase caster) {
        Vec3d centre = ground(caster.world, CosmicPowers.aim(caster, 48D));
        Vec3d look = caster.getLookVec();
        Vec3d side = new Vec3d(-look.z, 0D, look.x);
        side = side.lengthSquared() < 1.0E-4 ? new Vec3d(5D, 0D, 0D) : side.normalize().scale(5D);
        Vec3d[] pans = {ground(caster.world, centre.add(side)), ground(caster.world, centre.subtract(side))};
        for (int k = 0; k < 2; k++) {
            Vec3d at = pans[k];
            later(caster, 1 + k * 20, () -> {
                fx(caster.world, MsgTribunalFx.SIGIL, at, 6D, 0, 0, 0, 25);
                fx(caster.world, MsgTribunalFx.PAN, at, 6D, 0, 0, 0, 25);
                later(caster, 25, () -> slam(caster, at, 6D, 35F));
            });
        }
        return true;
    }

    /** Sentence: a cage of light springs up around whatever you look at, holds it, and bursts. */
    public boolean sentence(EntityLivingBase caster) {
        EntityLivingBase victim = lookedAt(caster, 40D);
        if (victim == null) {
            return false;
        }
        Vec3d at = new Vec3d(victim.posX, victim.posY, victim.posZ);
        fx(caster.world, MsgTribunalFx.CAGE, at, 2.5D, 0, 0, 0, 50);
        sound(caster.world, at, EternitySounds.RIFT_OPEN, 3F, 1.4F);
        for (int k = 0; k < 25; k++) {
            later(caster, k * 2, () -> {
                if (victim.isEntityAlive()) {
                    victim.motionX = victim.motionY = victim.motionZ = 0D;
                    if (victim instanceof EntityPlayerMP) {
                        ((EntityPlayerMP) victim).connection.setPlayerLocation(at.x, at.y, at.z, victim.rotationYaw, victim.rotationPitch);
                    } else {
                        victim.setPositionAndUpdate(at.x, at.y, at.z);
                    }
                }
            });
        }
        later(caster, 50, () -> slam(caster, at, 3.5D, 50F));
        return true;
    }

    /** Convergence: golden blades circle where you look, closing until they meet. */
    public boolean convergence(EntityLivingBase caster) {
        Vec3d at = ground(caster.world, CosmicPowers.aim(caster, 48D));
        fx(caster.world, MsgTribunalFx.BLADES, at, 10D, 0, 0, 0, 40);
        sound(caster.world, at, EternitySounds.OMEN_SHIMMER, 3F, 0.8F);
        later(caster, 40, () -> slam(caster, at, 5D, 45F));
        return true;
    }

    /** Pillars of Creation: columns of light erupt in a line marching away from you. */
    public boolean pillars(EntityLivingBase caster) {
        Vec3d look = caster.getLookVec();
        Vec3d dir = new Vec3d(look.x, 0D, look.z);
        dir = dir.lengthSquared() < 1.0E-4 ? new Vec3d(0D, 0D, 1D) : dir.normalize();
        Vec3d start = new Vec3d(caster.posX, caster.posY, caster.posZ);
        for (int i = 0; i < 12; i++) {
            Vec3d at = ground(caster.world, start.add(dir.scale(3D + 3D * i)));
            later(caster, 4 + i * 2, () -> {
                fx(caster.world, MsgTribunalFx.PILLAR, at, 14D, 0, 0, 0, 14);
                sound(caster.world, at, EternitySounds.RIFT_IMPACT, 2F, 1.3F);
                hurt(caster, at, 2.5D, 20F, 1.2D);
            });
        }
        return true;
    }

    /** Cosmic Verdict: a ring of light bursts out from you across the ground, striking everything it crosses. */
    public boolean verdict(EntityLivingBase caster) {
        Vec3d centre = new Vec3d(caster.posX, caster.posY, caster.posZ);
        fx(caster.world, MsgTribunalFx.SIGIL, centre, 8D, 0, 0, 0, 20);
        sound(caster.world, centre, EternitySounds.COMET_BLAST, 4F, 0.6F);
        Set<Entity> struck = new HashSet<>();
        later(caster, 20, () -> {
            fx(caster.world, MsgTribunalFx.NOVA, centre, 30D, 0, 0, 0, 30);
            sound(caster.world, centre, SoundEvents.ENTITY_GENERIC_EXPLODE, 4F, 0.6F);
        });
        for (int k = 1; k <= 30; k++) {
            double r = k;
            later(caster, 20 + k, () -> {
                for (EntityLivingBase victim : victims(caster, new AxisAlignedBB(centre, centre).grow(r + 2D, 4D, r + 2D))) {
                    double dx = victim.posX - centre.x;
                    double dz = victim.posZ - centre.z;
                    if (Math.abs(Math.sqrt(dx * dx + dz * dz) - r) < 1.5D && struck.add(victim)) {
                        victim.hurtResistantTime = 0;
                        victim.attackEntityFrom(EternityDamage.wielded("tribunal", caster), 30F);
                        victim.motionY += 0.8D;
                        victim.velocityChanged = true;
                    }
                }
            });
        }
        return true;
    }

    // ------------------------------------------------------------------ helpers

    /** A blow landing on a point: a shockwave, and everything near it struck and thrown. */
    private void slam(EntityLivingBase caster, Vec3d at, double radius, float damage) {
        fx(caster.world, MsgTribunalFx.GAVEL, at, radius * 1.5D, 0, 0, 0, 30);
        sound(caster.world, at, EternitySounds.COMET_IMPACT, 4F, 0.7F);
        sound(caster.world, at, SoundEvents.ENTITY_LIGHTNING_THUNDER, 3F, 0.7F);
        hurt(caster, at, radius, damage, 1D);
    }

    private void hurt(EntityLivingBase caster, Vec3d at, double radius, float damage, double lift) {
        for (EntityLivingBase victim : victims(caster, new AxisAlignedBB(at, at).grow(radius))) {
            if (victim.getDistance(at.x, at.y, at.z) > radius) {
                continue;
            }
            victim.hurtResistantTime = 0;
            victim.attackEntityFrom(EternityDamage.wielded("tribunal", caster), damage);
            if (lift > 0D) {
                Vec3d push = victim.getPositionVector().subtract(at);
                push = push.lengthSquared() < 1.0E-4 ? Vec3d.ZERO : push.normalize();
                victim.motionX += push.x * lift;
                victim.motionY += lift * 0.8D;
                victim.motionZ += push.z * lift;
                victim.velocityChanged = true;
            }
        }
    }

    /** Everything living in a box, except the one wielding the power, and spectators. */
    private static List<EntityLivingBase> victims(EntityLivingBase caster, AxisAlignedBB box) {
        return caster.world.getEntitiesWithinAABB(EntityLivingBase.class, box, e -> e != null && e != caster && e.isEntityAlive()
                && !(e instanceof EntityPlayer && ((EntityPlayer) e).isSpectator()));
    }

    /** The living thing nearest the middle of the caster's view, within range. */
    @Nullable
    private static EntityLivingBase lookedAt(EntityLivingBase caster, double range) {
        Vec3d eyes = caster.getPositionEyes(1F);
        Vec3d look = caster.getLook(1F);
        EntityLivingBase best = null;
        double bestScore = 0.97D;
        for (EntityLivingBase e : victims(caster, caster.getEntityBoundingBox().grow(range))) {
            Vec3d to = e.getPositionVector().add(0D, e.height / 2D, 0D).subtract(eyes);
            double d = to.length();
            if (d > range || d < 1.0E-3) {
                continue;
            }
            double score = look.dotProduct(to.scale(1D / d));
            if (score > bestScore) {
                bestScore = score;
                best = e;
            }
        }
        return best;
    }

    /** The top of the ground at or below a point, so marks lie on the floor rather than in the air. */
    private static Vec3d ground(World world, Vec3d at) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int start = MathHelper.floor(at.y + 2D);
        for (int y = start; y > start - 24 && y > 0; y--) {
            pos.setPos(at.x, y, at.z);
            if (world.isBlockLoaded(pos) && world.getBlockState(pos).getMaterial().blocksMovement()) {
                return new Vec3d(at.x, y + 1D, at.z);
            }
        }
        return at;
    }

    private static double distanceToSegment(Vec3d p, Vec3d a, Vec3d b) {
        Vec3d ab = b.subtract(a);
        double len = ab.lengthSquared();
        double t = len < 1.0E-6 ? 0D : MathHelper.clamp(p.subtract(a).dotProduct(ab) / len, 0D, 1D);
        return p.distanceTo(a.add(ab.scale(t)));
    }

    private static void fx(World world, byte type, Vec3d at, double a, double b, double c, int id, int time) {
        EternityNetwork.sendToDimension(new MsgTribunalFx(type, at.x, at.y, at.z, a, b, c, id, time), world.provider.getDimension());
    }

    private static void sound(World world, Vec3d at, SoundEvent sound, float volume, float pitch) {
        world.playSound(null, at.x, at.y, at.z, sound, SoundCategory.PLAYERS, volume, pitch);
    }
}
