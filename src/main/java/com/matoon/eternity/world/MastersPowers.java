package com.matoon.eternity.world;

import com.matoon.eternity.EternitySounds;
import com.matoon.eternity.entity.EntityDestroyerArmor;
import com.matoon.eternity.entity.EternityDamage;
import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.network.MsgTribunalFx;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * The powers of Master Order, Lord Chaos and the In-Betweener, wielded by someone holding
 * an Astral Regulator that absorbed them. The same things they did at the junction, scaled
 * for a mortal hand: fixed damage rather than a share of the victim's life, and never
 * touching the one who wields them.
 */
public final class MastersPowers {

    public static final MastersPowers INSTANCE = new MastersPowers();

    private static final int TILE = 3;

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

    private MastersPowers() {
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

    // ------------------------------------------------------------------ Master Order

    /** Decree: a single line of judgement from your eyes, following where you look, for two seconds. */
    public boolean decree(EntityLivingBase caster) {
        sound(caster.world, caster.getPositionVector(), EternitySounds.COMET_BLAST, 3F, 0.7F);
        for (int k = 0; k < 14; k++) {
            later(caster, k * 3, () -> {
                Vec3d from = caster.getPositionEyes(1F);
                Vec3d to = CosmicPowers.aim(caster, 64D);
                fx(caster.world, MsgTribunalFx.GAZE, from, to.x, to.y, to.z, 0, 4);
                for (EntityLivingBase victim : victims(caster, new AxisAlignedBB(to, to).grow(2.5D))) {
                    if (victim.getDistance(to.x, to.y, to.z) < 2.2D) {
                        strike(caster, victim, "decree", 7F);
                    }
                }
            });
        }
        return true;
    }

    /** Edict: everything near where you look is bound by law for six seconds: it cannot move, cannot strike, cannot choose. */
    public boolean edict(EntityLivingBase caster) {
        Vec3d at = CosmicPowers.aim(caster, 40D);
        sound(caster.world, at, EternitySounds.FINALE_DRUMS, 3F, 0.6F);
        for (EntityLivingBase victim : victims(caster, new AxisAlignedBB(at, at).grow(14D))) {
            victim.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 120, 6));
            victim.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, 120, 4));
            victim.addPotionEffect(new PotionEffect(MobEffects.MINING_FATIGUE, 120, 3));
            if (victim instanceof EntityLiving) {
                ((EntityLiving) victim).setAttackTarget(null);
                ((EntityLiving) victim).getNavigator().clearPath();
            }
            fx(caster.world, MsgTribunalFx.ORDER_BOLT, victim.getPositionVector(), 0, 0, 0, 0, 14);
        }
        if (caster instanceof EntityPlayerMP) {
            TextComponentString law = new TextComponentString("BE STILL.");
            law.getStyle().setColor(TextFormatting.GOLD).setBold(true);
            ((EntityPlayerMP) caster).sendStatusMessage(law, true);
        }
        return true;
    }

    /** Tessellation: a grid of golden tiles where you look; the lit ones erupt, then all the others. */
    public boolean tessellation(EntityLivingBase caster) {
        Vec3d hit = ground(caster.world, CosmicPowers.aim(caster, 48D));
        Vec3d centre = new Vec3d(Math.floor(hit.x / TILE) * TILE + TILE / 2D, hit.y, Math.floor(hit.z / TILE) * TILE + TILE / 2D);
        int lit = rand.nextBoolean() ? 0x15AAAD5 : 0xA5552A;
        int other = ~lit & 0x1FFFFFF;
        fx(caster.world, MsgTribunalFx.ORDER_TILES, centre, TILE, 0, 0, lit, 20);
        later(caster, 20, () -> {
            erupt(caster, centre, lit);
            fx(caster.world, MsgTribunalFx.ORDER_TILES, centre, TILE, 0, 0, other, 16);
        });
        later(caster, 36, () -> erupt(caster, centre, other));
        return true;
    }

    private void erupt(EntityLivingBase caster, Vec3d centre, int lit) {
        fx(caster.world, MsgTribunalFx.ORDER_ERUPT, centre, TILE, 0, 0, lit, 16);
        sound(caster.world, centre, EternitySounds.RIFT_IMPACT, 3F, 1.1F);
        for (EntityLivingBase victim : victims(caster, new AxisAlignedBB(centre, centre).grow(8D, 4D, 8D))) {
            int i = MathHelper.floor((victim.posX - centre.x) / TILE + 2.5D);
            int j = MathHelper.floor((victim.posZ - centre.z) / TILE + 2.5D);
            if (i >= 0 && i < 5 && j >= 0 && j < 5 && (lit >> (i * 5 + j) & 1) != 0) {
                strike(caster, victim, "tessellation", 22F);
                victim.motionY += 0.6D;
                victim.velocityChanged = true;
            }
        }
    }

    /** Alignment: a golden line from you along your gaze; everything near is drawn into it; then judgement runs its length. */
    public boolean alignment(EntityLivingBase caster) {
        Vec3d look = caster.getLook(1F);
        Vec3d dir = new Vec3d(look.x, 0D, look.z);
        dir = dir.lengthSquared() < 1.0E-4 ? new Vec3d(0D, 0D, 1D) : dir.normalize();
        Vec3d from = new Vec3d(caster.posX, caster.posY, caster.posZ).add(dir.scale(2D));
        Vec3d to = from.add(dir.scale(34D));
        fx(caster.world, MsgTribunalFx.ORDER_LINE, from, to.x, to.y, to.z, 0, 32);
        for (int k = 0; k < 28; k += 2) {
            later(caster, k, () -> {
                for (EntityLivingBase victim : victims(caster, new AxisAlignedBB(from, to).grow(16D, 4D, 16D))) {
                    Vec3d on = nearestOnLine(victim.getPositionVector(), from, to);
                    Vec3d pull = new Vec3d(on.x - victim.posX, 0D, on.z - victim.posZ);
                    if (pull.lengthSquared() > 0.04D) {
                        pull = pull.normalize().scale(0.18D);
                        victim.motionX += pull.x;
                        victim.motionZ += pull.z;
                        victim.velocityChanged = true;
                    }
                }
            });
        }
        later(caster, 30, () -> {
            fx(caster.world, MsgTribunalFx.ORDER_BEAM, from.add(0D, 0.5D, 0D), to.x, to.y + 0.5D, to.z, 0, 18);
            sound(caster.world, from, EternitySounds.COMET_BLAST, 4F, 0.8F);
            for (EntityLivingBase victim : victims(caster, new AxisAlignedBB(from, to).grow(2D, 3D, 2D))) {
                if (nearestOnLine(victim.getPositionVector(), from, to).distanceTo(victim.getPositionVector()) < 1.9D) {
                    strike(caster, victim, "alignment", 30F);
                }
            }
        });
        return true;
    }

    // ------------------------------------------------------------------ Lord Chaos

    /** The Dice: roll them where you look, and live with what they show. */
    public boolean dice(EntityLivingBase caster) {
        Vec3d at = ground(caster.world, CosmicPowers.aim(caster, 40D));
        int a = 1 + rand.nextInt(6), b = 1 + rand.nextInt(6);
        fx(caster.world, MsgTribunalFx.CHAOS_DICE, at, 0, 0, 0, a * 10 + b, 24);
        sound(caster.world, at, EternitySounds.ENTITY_GLITCH, 2F, 1.4F);
        later(caster, 26, () -> roll(caster, at, a + b));
        return true;
    }

    private void roll(EntityLivingBase caster, Vec3d at, int sum) {
        String result;
        switch (sum) {
            case 2:
                result = "Snake eyes.";
                fx(caster.world, MsgTribunalFx.CHAOS_BURST, at, 6D, 0, 0, 0, 12);
                later(caster, 12, () -> hurt(caster, at, 6D, "dice", 40F, 1D));
                break;
            case 3:
            case 4:
            case 12:
                result = sum == 12 ? "Double sixes! Stars, and more stars." : "Stars.";
                for (int i = 0; i < (sum == 12 ? 20 : 10); i++) {
                    Vec3d land = ground(caster.world, at.add(rand.nextDouble() * 14D - 7D, 2D, rand.nextDouble() * 14D - 7D));
                    Vec3d from = land.add(rand.nextDouble() * 20D - 10D, 40D, rand.nextDouble() * 20D - 10D);
                    int delay = rand.nextInt(30);
                    later(caster, delay, () -> fx(caster.world, MsgTribunalFx.CHAOS_STAR, from, land.x, land.y, land.z, 0, 12));
                    later(caster, delay + 12, () -> {
                        fx(caster.world, MsgTribunalFx.CHAOS_BURST, land, 2.5D, 0, 0, 0, 1);
                        hurt(caster, land, 2.6D, "dice", 12F, 0.4D);
                    });
                }
                break;
            case 5:
            case 6:
                result = "Shuffle.";
                for (EntityLivingBase victim : victims(caster, new AxisAlignedBB(at, at).grow(16D))) {
                    Vec3d to = ground(caster.world, victim.getPositionVector().add(rand.nextDouble() * 20D - 10D, 4D, rand.nextDouble() * 20D - 10D));
                    fx(caster.world, MsgTribunalFx.CHAOS_SCATTER, victim.getPositionVector(), to.x, to.y, to.z, 0, 20);
                    victim.setPositionAndUpdate(to.x, to.y, to.z);
                    strike(caster, victim, "dice", 6F);
                }
                break;
            case 7:
                result = "Oops. A heal.";
                caster.setHealth(Math.min(caster.getMaxHealth(), caster.getHealth() + caster.getMaxHealth() * 0.25F));
                break;
            case 8:
            case 9:
                result = "Up they go.";
                for (EntityLivingBase victim : victims(caster, new AxisAlignedBB(at, at).grow(12D))) {
                    strike(caster, victim, "dice", 10F);
                    victim.motionY = 1.5D;
                    victim.velocityChanged = true;
                }
                break;
            default:
                result = "Ow. You hurt yourself laughing.";
                caster.attackEntityFrom(EternityDamage.wielded("dice", caster), 4F);
                break;
        }
        if (caster instanceof EntityPlayerMP) {
            TextComponentString text = new TextComponentString(sum + ": " + result);
            text.getStyle().setColor(TextFormatting.AQUA);
            ((EntityPlayerMP) caster).sendStatusMessage(text, true);
        }
    }

    /** The Maw: for three seconds you inhale the world; whatever reaches you is bitten. */
    public boolean maw(EntityLivingBase caster) {
        sound(caster.world, caster.getPositionVector(), EternitySounds.AMBIENT_RUMBLE, 3F, 0.6F);
        List<Entity> bitten = new ArrayList<>();
        for (int k = 0; k < 60; k += 2) {
            later(caster, k, () -> {
                Vec3d mouth = caster.getPositionEyes(1F);
                for (EntityLivingBase victim : victims(caster, caster.getEntityBoundingBox().grow(18D))) {
                    Vec3d to = mouth.subtract(victim.getPositionVector().add(0D, victim.height / 2D, 0D));
                    double d = to.length();
                    Vec3d pull = to.normalize().scale(0.16D * (1D - d / 22D));
                    EntityDestroyerArmor.shove(victim, pull.x, pull.y * 0.5D, pull.z);
                    if (caster.ticksExisted % 6 == 0) {
                        fx(caster.world, MsgTribunalFx.CHAOS_MAW, victim.getPositionVector().add(0D, 1D, 0D), mouth.x, mouth.y, mouth.z, 0, 8);
                    }
                    if (d < 3D && !bitten.contains(victim)) {
                        bitten.add(victim);
                        strike(caster, victim, "maw", 16F);
                        sound(caster.world, mouth, EternitySounds.COMET_IMPACT, 2F, 1.3F);
                    }
                }
            });
        }
        return true;
    }

    /** Wanderers: five little rifts set loose where you look, wandering at random for seven seconds. */
    public boolean wanderers(EntityLivingBase caster) {
        Vec3d at = ground(caster.world, CosmicPowers.aim(caster, 32D)).add(0D, 0.8D, 0D);
        sound(caster.world, at, EternitySounds.ENTITY_GLITCH, 2F, 0.8F);
        List<double[]> rifts = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            rifts.add(new double[]{at.x + rand.nextDouble() * 4D - 2D, at.y, at.z + rand.nextDouble() * 4D - 2D, 0D, 0D});
        }
        Map<Entity, Integer> touched = new HashMap<>();
        for (int k = 0; k < 140; k += 2) {
            final int tick = k;
            later(caster, k, () -> {
                for (double[] w : rifts) {
                    w[3] = MathHelper.clamp(w[3] + (rand.nextDouble() - 0.5D) * 0.25D, -0.6D, 0.6D);
                    w[4] = MathHelper.clamp(w[4] + (rand.nextDouble() - 0.5D) * 0.25D, -0.6D, 0.6D);
                    if (rand.nextInt(18) == 0) {
                        w[0] += rand.nextDouble() * 8D - 4D;
                        w[2] += rand.nextDouble() * 8D - 4D;
                    }
                    w[0] += w[3];
                    w[2] += w[4];
                    Vec3d p = new Vec3d(w[0], w[1], w[2]);
                    fx(caster.world, MsgTribunalFx.CHAOS_WANDER, p, 1.1D, 0, 0, 0, 3);
                    for (EntityLivingBase victim : victims(caster, new AxisAlignedBB(p, p).grow(1.8D))) {
                        Integer last = touched.get(victim);
                        if (last == null || tick - last >= 10) {
                            touched.put(victim, tick);
                            strike(caster, victim, "wanderers", 8F);
                        }
                    }
                }
            });
        }
        return true;
    }

    /** Disarray: everything near where you look forgets whose side it is on, and turns on its neighbours. */
    public boolean disarray(EntityLivingBase caster) {
        Vec3d at = CosmicPowers.aim(caster, 40D);
        List<EntityLivingBase> victims = victims(caster, new AxisAlignedBB(at, at).grow(16D));
        if (victims.size() < 2) {
            return false;
        }
        sound(caster.world, at, EternitySounds.ENTITY_GLITCH, 3F, 1.5F);
        for (EntityLivingBase victim : victims) {
            if (victim instanceof EntityLiving && !(victim instanceof EntityPlayer)) {
                EntityLivingBase other;
                do {
                    other = victims.get(rand.nextInt(victims.size()));
                } while (other == victim);
                ((EntityLiving) victim).setAttackTarget(other);
                victim.setRevengeTarget(other);
                fx(caster.world, MsgTribunalFx.CHAOS_SCATTER, victim.getPositionVector(), victim.posX, victim.posY, victim.posZ, 0, 20);
            }
        }
        return true;
    }

    // ------------------------------------------------------------------ the In-Betweener

    /** Inversion: you and what you look at trade places. Here and there are only words. */
    public boolean inversion(EntityLivingBase caster) {
        EntityLivingBase target = lookedAt(caster, 40D);
        if (target == null) {
            return false;
        }
        Vec3d mine = caster.getPositionVector(), theirs = target.getPositionVector();
        float yaw = caster.rotationYaw;
        fx(caster.world, MsgTribunalFx.PARADOX, mine, 1.5D, 0, 0, 0, 20);
        fx(caster.world, MsgTribunalFx.PARADOX, theirs, 1.5D, 0, 0, 0, 20);
        target.setPositionAndUpdate(mine.x, mine.y, mine.z);
        if (caster instanceof EntityPlayerMP) {
            ((EntityPlayerMP) caster).connection.setPlayerLocation(theirs.x, theirs.y, theirs.z, yaw + 180F, caster.rotationPitch);
        } else {
            caster.setPositionAndUpdate(theirs.x, theirs.y, theirs.z);
        }
        caster.fallDistance = 0F;
        sound(caster.world, mine, SoundEvents.ENTITY_ENDERMEN_TELEPORT, 2F, 0.5F);
        sound(caster.world, theirs, SoundEvents.ENTITY_ENDERMEN_TELEPORT, 2F, 1.5F);
        return true;
    }

    /**
     * Equilibrium: what you look at has its life and its death trade places: the health it
     * has becomes the health it lacks. The whole are brought low; the dying are made whole.
     * Beings beyond such things (Eternity, the Tribunal, his makers) are not touched.
     */
    public boolean equilibrium(EntityLivingBase caster) {
        EntityLivingBase target = lookedAt(caster, 32D);
        if (target == null || !target.isNonBoss()) {
            return false;
        }
        float max = target.getMaxHealth();
        float inverted = Math.max(1F, max - target.getHealth());
        fx(caster.world, MsgTribunalFx.PARADOX, target.getPositionVector(), 2.5D, 0, 0, 0, 30);
        sound(caster.world, target.getPositionVector(), EternitySounds.ENTITY_VOICE, 2F, 1.6F);
        if (inverted < target.getHealth()) {
            target.attackEntityFrom(EternityDamage.wielded("equilibrium", caster), 0.01F);
        }
        target.setHealth(inverted);
        return true;
    }

    /**
     * Paradox: for everything around you, up and down trade places. It falls into the sky,
     * hangs there a moment, and the sky lets go.
     */
    public boolean paradox(EntityLivingBase caster) {
        List<EntityLivingBase> victims = victims(caster, caster.getEntityBoundingBox().grow(10D));
        fx(caster.world, MsgTribunalFx.PARADOX, caster.getPositionVector(), 10D, 0, 0, 0, 40);
        sound(caster.world, caster.getPositionVector(), EternitySounds.RIFT_OPEN, 3F, 0.6F);
        for (EntityLivingBase victim : victims) {
            victim.motionY = 1.6D;
            victim.velocityChanged = true;
            later(caster, 22, () -> {
                victim.motionY = -3D;
                victim.velocityChanged = true;
            });
            later(caster, 30, () -> {
                fx(caster.world, MsgTribunalFx.PARADOX, victim.getPositionVector(), 1.5D, 0, 0, 0, 16);
                strike(caster, victim, "paradox", 20F);
            });
        }
        return true;
    }

    // ------------------------------------------------------------------ helpers

    private static void strike(EntityLivingBase caster, EntityLivingBase victim, String type, float damage) {
        victim.hurtResistantTime = 0;
        victim.attackEntityFrom(EternityDamage.wielded(type, caster), damage);
    }

    private void hurt(EntityLivingBase caster, Vec3d at, double radius, String type, float damage, double lift) {
        for (EntityLivingBase victim : victims(caster, new AxisAlignedBB(at, at).grow(radius))) {
            if (victim.getDistance(at.x, at.y, at.z) > radius) {
                continue;
            }
            strike(caster, victim, type, damage);
            if (lift > 0D) {
                victim.motionY += lift;
                victim.velocityChanged = true;
            }
        }
    }

    /** Everything living in a box, except the one wielding the power, and spectators. */
    private static List<EntityLivingBase> victims(EntityLivingBase caster, AxisAlignedBB box) {
        return caster.world.getEntitiesWithinAABB(EntityLivingBase.class, box, e -> e != null && e != caster && e.isEntityAlive()
                && !(e instanceof EntityPlayer && ((EntityPlayer) e).isSpectator())
                && !EntityDestroyerArmor.sheltered(e) && e != caster.getRidingEntity());
    }

    /** The living thing nearest the middle of the caster's view, within range. */
    @Nullable
    private static EntityLivingBase lookedAt(EntityLivingBase caster, double range) {
        // Whatever is right under the crosshair first; failing that, whatever is nearest the middle of the view.
        EntityLivingBase sighted = CosmicPowers.sighted(caster, range);
        if (sighted != null) {
            return sighted;
        }
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

    /** The top of the ground at or below a point. */
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

    private static Vec3d nearestOnLine(Vec3d p, Vec3d a, Vec3d b) {
        Vec3d ab = b.subtract(a);
        double len = ab.lengthSquared();
        double t = len < 1.0E-6 ? 0D : MathHelper.clamp(p.subtract(a).dotProduct(ab) / len, 0D, 1D);
        return a.add(ab.scale(t));
    }

    private static void fx(World world, byte type, Vec3d at, double a, double b, double c, int id, int time) {
        EternityNetwork.sendToDimension(new MsgTribunalFx(type, at.x, at.y, at.z, a, b, c, id, time), world.provider.getDimension());
    }

    private static void sound(World world, Vec3d at, SoundEvent sound, float volume, float pitch) {
        world.playSound(null, at.x, at.y, at.z, sound, SoundCategory.PLAYERS, volume, pitch);
    }
}
