package com.matoon.eternity.client;

import com.matoon.eternity.client.render.FxRenderer;
import com.matoon.eternity.client.render.VaultFx;
import com.matoon.eternity.entity.EntityDestroyerArmor;
import com.matoon.eternity.entity.EntityDestroyerArmor.Condition;
import com.matoon.eternity.entity.EntityDestroyerArmor.State;
import com.matoon.eternity.entity.Overcharge;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;
import java.util.Random;

/**
 * The Destroyer Armor coming apart as it is beaten down, worse at every stage of its {@link Condition}, from a few
 * wounds of its own that stay where they are (chosen by which armour it is, so everyone sees the same ones):
 * <ul>
 * <li><b>Strained</b>: now and then a spurt of sparks from a wounded joint, and a wisp of smoke from its back.</li>
 * <li><b>Failing</b>: smoke pouring steadily from its wounds, sparks spitting, molten metal dripping, and loose power
 * crackling from one wound to another.</li>
 * <li><b>Critical</b>: black smoke boiling off it, fire licking out of its wounds, its power arcing all over it, and
 * every few seconds something inside it bursting, throwing sparks and shards of plate and jolting all of it.</li>
 * <li><b>Down</b>: the wreck smoulders.</li>
 * </ul>
 * Its painted wounds (dents, gouges, soot, cracks, holes over the fire) are {@link com.matoon.eternity.client.render.RenderDestroyer}'s.
 */
@SideOnly(Side.CLIENT)
final class ArmorWounds {

    /** Where it can be wounded, on its body: how far to its side, how high, how far forward. */
    private static final Vec3d[] JOINTS = {
            new Vec3d(-0.95D, 3.2D, 0D), new Vec3d(0.95D, 3.2D, 0D), new Vec3d(-1.15D, 2.35D, 0.1D), new Vec3d(1.15D, 2.35D, 0.1D),
            new Vec3d(-0.45D, 1.5D, 0.05D), new Vec3d(0.45D, 1.5D, 0.05D), new Vec3d(-0.4D, 0.75D, 0.2D), new Vec3d(0.4D, 0.75D, 0.2D),
            new Vec3d(-0.35D, 2.9D, -0.55D), new Vec3d(0.35D, 2.9D, -0.55D), new Vec3d(-0.5D, 2.6D, 0.45D), new Vec3d(0.5D, 2.6D, 0.45D)};
    /** How many of those it has, at each stage. */
    private static final int[] WOUNDS = {0, 2, 3, 5};

    private static final int IRON = Block.getStateId(Blocks.IRON_BLOCK.getDefaultState());

    private ArmorWounds() {
    }

    /** Its wound points: the same few for the same armour, for as long as it lasts. */
    private static Vec3d[] wounds(EntityDestroyerArmor armor, int count) {
        Random pick = new Random(armor.getEntityId() * 341873128712L + 132897987541L);
        Vec3d[] out = new Vec3d[count];
        boolean[] taken = new boolean[JOINTS.length];
        for (int i = 0; i < count; i++) {
            int j = pick.nextInt(JOINTS.length);
            while (taken[j]) {
                j = (j + 1) % JOINTS.length;
            }
            taken[j] = true;
            out[i] = JOINTS[j];
        }
        return out;
    }

    /** One tick of it coming apart. */
    static void tick(EntityDestroyerArmor armor, List<FxRenderer.Fx> effects, Random rand) {
        World world = armor.world;
        State state = armor.getState();
        if (state == State.RISE) {
            return;
        }
        Condition condition = armor.condition();
        int stage = state == State.DOWN ? 3 : condition.ordinal();
        if (stage == 0) {
            return;
        }
        Vec3d[] wounds = wounds(armor, WOUNDS[stage]);
        float s = armor.scale();
        if (state == State.DOWN) {
            // The wreck smouldering: smoke, and now and then a last spark.
            if (rand.nextInt(3) == 0) {
                Vec3d at = at(armor, wounds[rand.nextInt(wounds.length)].scale(0.6D), s);
                world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, at.x, at.y, at.z, rand.nextGaussian() * 0.01D, 0.06D, rand.nextGaussian() * 0.01D);
            }
            if (rand.nextInt(50) == 0) {
                Vec3d at = at(armor, wounds[rand.nextInt(wounds.length)].scale(0.6D), s);
                effects.add(new VaultFx.Sparks(at, new Vec3d(0D, 1D, 0D), 5, 0.2D, rand.nextLong()));
            }
            return;
        }
        // Sparks spitting from a wound, more often the worse it is.
        int sparkEvery = stage == 1 ? 45 : stage == 2 ? 16 : 6;
        if (rand.nextInt(sparkEvery) == 0) {
            Vec3d local = wounds[rand.nextInt(wounds.length)];
            Vec3d at = at(armor, local, s);
            effects.add(new VaultFx.Sparks(at, outward(armor, local), 5 + stage * 3, 0.18D + 0.06D * stage, rand.nextLong()));
            if (rand.nextInt(3) == 0) {
                world.playSound(at.x, at.y, at.z, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.NEUTRAL, 0.15F + 0.1F * stage, 1.6F + rand.nextFloat() * 0.4F, false);
            }
        }
        // Smoke: a wisp from its back at first, then pouring from its wounds, black and boiling at the last.
        if (stage == 1) {
            if (rand.nextInt(12) == 0) {
                Vec3d at = at(armor, new Vec3d(rand.nextGaussian() * 0.25D, 2.9D, -0.6D), s);
                world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, at.x, at.y, at.z, 0D, 0.05D, 0D);
            }
        } else {
            for (int i = 0; i < wounds.length; i++) {
                if (rand.nextInt(stage == 2 ? 6 : 3) != 0) {
                    continue;
                }
                Vec3d at = at(armor, wounds[i], s);
                world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, at.x + rand.nextGaussian() * 0.08D, at.y, at.z + rand.nextGaussian() * 0.08D,
                        rand.nextGaussian() * 0.01D, 0.04D + rand.nextDouble() * 0.04D, rand.nextGaussian() * 0.01D);
                if (stage == 3 && rand.nextInt(8) == 0) {
                    world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, at.x, at.y + 0.3D, at.z, 0D, 0.09D, 0D);
                }
            }
        }
        // Molten metal dripping from its wounds.
        if (stage >= 2 && rand.nextInt(stage == 2 ? 10 : 4) == 0) {
            Vec3d at = at(armor, wounds[rand.nextInt(wounds.length)], s);
            world.spawnParticle(EnumParticleTypes.DRIP_LAVA, at.x, at.y - 0.1D, at.z, 0D, 0D, 0D);
        }
        // Loose power crackling from one wound to another.
        if (stage >= 2 && rand.nextInt(stage == 2 ? 70 : 22) == 0) {
            Vec3d a = wounds[rand.nextInt(wounds.length)], b = JOINTS[rand.nextInt(JOINTS.length)];
            if (a.distanceTo(b) > 0.3D) {
                effects.add(new VaultFx.Arc(armor, a.scale(s), b.scale(s), 2 + rand.nextInt(4), rand.nextLong()));
                Vec3d at = at(armor, a, s);
                world.playSound(at.x, at.y, at.z, SoundEvents.ENTITY_FIREWORK_TWINKLE, SoundCategory.NEUTRAL, 0.5F, 1.8F + rand.nextFloat() * 0.2F, false);
                world.playSound(at.x, at.y, at.z, SoundEvents.BLOCK_REDSTONE_TORCH_BURNOUT, SoundCategory.NEUTRAL, 0.4F, 1.5F + rand.nextFloat() * 0.4F, false);
            }
        }
        if (stage < 3) {
            return;
        }
        // Fire licking out of its wounds.
        for (Vec3d local : wounds) {
            if (rand.nextInt(3) == 0) {
                Vec3d at = at(armor, local, s);
                Vec3d out = outward(armor, local).scale(0.03D);
                world.spawnParticle(EnumParticleTypes.FLAME, at.x, at.y, at.z, out.x, 0.02D + rand.nextDouble() * 0.03D, out.z);
            }
        }
        // Something inside it bursting.
        if (rand.nextInt(90) == 0) {
            Vec3d local = wounds[rand.nextInt(wounds.length)];
            burst(armor, at(armor, local, s), outward(armor, local), effects, rand, 1F);
        }
    }

    /**
     * Something bursting at a wound, or a blow tearing into it: a flash, a spray of sparks, shards of its plate flung
     * out, smoke, and the bang of it; a hard one jolts all of it and sends its power arcing.
     */
    static void burst(EntityDestroyerArmor armor, Vec3d at, Vec3d out, List<FxRenderer.Fx> effects, Random rand, float severity) {
        World world = armor.world;
        effects.add(new VaultFx.Sparks(at, out.add(0D, 0.3D, 0D), (int) (12 + 22 * severity), 0.3D + 0.2D * severity, rand.nextLong()));
        world.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL, at.x, at.y, at.z, out.x * 0.05D, 0.03D, out.z * 0.05D);
        for (int i = 0; i < 6 + 10 * severity; i++) {
            world.spawnParticle(EnumParticleTypes.BLOCK_CRACK, at.x, at.y, at.z, out.x * 0.3D + rand.nextGaussian() * 0.2D, 0.15D + rand.nextDouble() * 0.25D,
                    out.z * 0.3D + rand.nextGaussian() * 0.2D, IRON);
        }
        for (int i = 0; i < 3 + 4 * severity; i++) {
            world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, at.x, at.y, at.z, out.x * 0.08D + rand.nextGaussian() * 0.03D, 0.05D,
                    out.z * 0.08D + rand.nextGaussian() * 0.03D);
        }
        world.playSound(at.x, at.y, at.z, SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.NEUTRAL, 0.3F + 0.15F * severity, 1.2F + rand.nextFloat() * 0.5F, false);
        if (severity >= 1F) {
            world.playSound(at.x, at.y, at.z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.NEUTRAL, 0.35F + 0.1F * severity, 1.5F + rand.nextFloat() * 0.3F, false);
            world.playSound(at.x, at.y, at.z, SoundEvents.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.NEUTRAL, 0.5F, 0.5F + rand.nextFloat() * 0.2F, false);
            armor.jolt = Math.max(armor.jolt, Math.min(1F, 0.5F * severity));
            Vec3d a = JOINTS[rand.nextInt(JOINTS.length)], b = JOINTS[rand.nextInt(JOINTS.length)];
            if (a.distanceTo(b) > 0.3D) {
                float s = armor.scale();
                effects.add(new VaultFx.Arc(armor, a.scale(s), b.scale(s), 3 + rand.nextInt(4), rand.nextLong()));
            }
        }
    }

    /**
     * Its core running hot with its overcharge: heat shimmering off it and its seams smoking at first, then flames
     * licking out of its joints and molten metal dripping, sparks spitting; breaching, its power arcing all over it,
     * things bursting inside it, and it shaking.
     */
    static void core(EntityDestroyerArmor armor, List<FxRenderer.Fx> effects, Random rand) {
        if (armor.getState() == State.IMPLODE) {
            imploding(armor, effects, rand);
            return;
        }
        float core = armor.core() / Overcharge.CORE_MAX;
        if (core < 0.3F) {
            return;
        }
        World world = armor.world;
        float s = armor.scale();
        float k = (core - 0.3F) / 0.7F;
        if (rand.nextFloat() < 0.2F + 0.8F * k) {
            Vec3d at = at(armor, JOINTS[rand.nextInt(JOINTS.length)], s);
            world.spawnParticle(k > 0.6F ? EnumParticleTypes.SMOKE_LARGE : EnumParticleTypes.SMOKE_NORMAL, at.x, at.y, at.z, 0D, 0.06D, 0D);
        }
        if (k > 0.3F && rand.nextFloat() < k) {
            Vec3d local = JOINTS[rand.nextInt(JOINTS.length)];
            Vec3d at = at(armor, local, s), out = outward(armor, local).scale(0.04D);
            world.spawnParticle(EnumParticleTypes.FLAME, at.x, at.y, at.z, out.x, 0.03D + rand.nextDouble() * 0.03D, out.z);
        }
        if (k > 0.5F && rand.nextInt(4) == 0) {
            Vec3d at = at(armor, JOINTS[rand.nextInt(JOINTS.length)], s);
            world.spawnParticle(EnumParticleTypes.DRIP_LAVA, at.x, at.y - 0.1D, at.z, 0D, 0D, 0D);
        }
        if (k > 0.6F && rand.nextInt(k > 0.95F ? 4 : 14) == 0) {
            Vec3d local = JOINTS[rand.nextInt(JOINTS.length)];
            effects.add(new VaultFx.Sparks(at(armor, local, s), outward(armor, local), 6, 0.25D, rand.nextLong()));
        }
        boolean breach = armor.breach() > 0;
        if (breach || k > 0.9F) {
            if (rand.nextInt(breach ? 5 : 20) == 0) {
                Vec3d a = JOINTS[rand.nextInt(JOINTS.length)], b = JOINTS[rand.nextInt(JOINTS.length)];
                if (a.distanceTo(b) > 0.3D) {
                    effects.add(new VaultFx.Arc(armor, a.scale(s), b.scale(s), 3 + rand.nextInt(4), rand.nextLong()));
                    Vec3d at = at(armor, a, s);
                    world.playSound(at.x, at.y, at.z, SoundEvents.BLOCK_REDSTONE_TORCH_BURNOUT, SoundCategory.NEUTRAL, 0.5F, 1.4F + rand.nextFloat() * 0.4F, false);
                }
            }
            armor.jolt = Math.max(armor.jolt, breach ? 0.25F + 0.5F * armor.breach() / Overcharge.BREACH_TICKS : 0.1F);
        }
        if (breach && rand.nextInt(Math.max(4, 30 - armor.breach() / 8)) == 0) {
            Vec3d local = JOINTS[rand.nextInt(JOINTS.length)];
            burst(armor, at(armor, local, s), outward(armor, local), effects, rand, 0.6F + armor.breach() / (float) Overcharge.BREACH_TICKS);
        }
    }

    /** Falling in on itself: everything round it streaming into its chest. */
    private static void imploding(EntityDestroyerArmor armor, List<FxRenderer.Fx> effects, Random rand) {
        World world = armor.world;
        float s = armor.scale();
        Vec3d c = new Vec3d(armor.posX, armor.posY + 2.2D * s, armor.posZ);
        for (int i = 0; i < 10; i++) {
            Vec3d dir = new Vec3d(rand.nextGaussian(), rand.nextGaussian(), rand.nextGaussian()).normalize();
            double r = (3D + rand.nextDouble() * 4D) * s;
            Vec3d at = c.add(dir.scale(r));
            Vec3d in = dir.scale(-r * 0.12D);
            world.spawnParticle(i % 3 == 0 ? EnumParticleTypes.FLAME : EnumParticleTypes.END_ROD, at.x, at.y, at.z, in.x, in.y, in.z);
        }
        if (rand.nextInt(2) == 0) {
            Vec3d a = JOINTS[rand.nextInt(JOINTS.length)], b = JOINTS[rand.nextInt(JOINTS.length)];
            effects.add(new VaultFx.Arc(armor, a.scale(s * 0.6F), b.scale(s * 0.6F), 3 + rand.nextInt(3), rand.nextLong()));
        }
        armor.jolt = 1F;
    }

    /** A point on its body, in the world. */
    private static Vec3d at(EntityDestroyerArmor armor, Vec3d local, float scale) {
        float yaw = (float) Math.toRadians(armor.rotationYaw);
        double fx = -MathHelper.sin(yaw), fz = MathHelper.cos(yaw);
        Vec3d l = local.scale(scale);
        return new Vec3d(armor.posX - fz * l.x + fx * l.z, armor.posY + l.y, armor.posZ + fx * l.x + fz * l.z);
    }

    /** Away from its middle through a point on its body, flat. */
    private static Vec3d outward(EntityDestroyerArmor armor, Vec3d local) {
        Vec3d at = at(armor, local, 1F);
        Vec3d out = new Vec3d(at.x - armor.posX, 0D, at.z - armor.posZ);
        return out.lengthSquared() < 1.0E-4 ? new Vec3d(0D, 1D, 0D) : out.normalize();
    }
}
