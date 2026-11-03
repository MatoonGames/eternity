package com.matoon.eternity.story;

import com.matoon.eternity.block.BlockStrangeBloom;
import com.matoon.eternity.block.EternityBlocks;
import com.matoon.eternity.client.render.NorseBoxes;
import com.matoon.eternity.compat.InfinityStones;
import com.matoon.eternity.compat.Relics;
import com.matoon.eternity.block.BlockThornHusk;
import com.matoon.eternity.entity.EntityAdamWarlock;
import com.matoon.eternity.entity.EntityChampion;
import com.matoon.eternity.entity.EntityNorse;
import com.matoon.eternity.entity.NorsePose;
import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.network.MsgStory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockStainedGlass;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static com.matoon.eternity.story.Speakers.CHAMPION;
import static com.matoon.eternity.story.Speakers.DIRECTION;
import static com.matoon.eternity.story.Speakers.GARDENER;
import static com.matoon.eternity.story.Speakers.WARLOCK;
import static com.matoon.eternity.story.Talk.act;
import static com.matoon.eternity.story.Talk.say;

/**
 * Chapter 5, the Garden. The Grandmaster warned the Elders after the Champion fell; the
 * Gardener's answer wasn't to hide. It was to grow.
 * <p>
 * Once the player has the Soul Stone, plants that don't exist start coming up in the overworld,
 * in bare stone and sand, near wherever they are: he is seeding the inner worlds. Death crouches
 * at a kill and lays her palm on the ground, and points at the red planet. On Mars, from wherever
 * they come down, there is green on the horizon; and the stones they carry tick, softly, like a
 * clock.
 * <p>
 * He is kind. He talks to them while he works, and then he tries to kill them: the ground opens
 * under their feet and his roots take them down. Death won't let them in: they wake at home, and
 * now he knows what he is dealing with. His hedge is up when they come back, and nothing they can cut
 * or burn gets through it: the Power Stone breaks it; the Champion, if he owes them, tears it open; the
 * Casket's winter freezes it through so it breaks like glass; and the Reality or the Soul Stone, held
 * up within reach of it, unmakes it or drinks the whole garden dry. On the far side of it he is on his knees among his ruined plants, and he
 * presses the Time Stone into his own chest so that it is part of something living. They can have
 * it. They will have to take it out of him.
 */
final class Garden {

    private static final int BLOOM_EVERY = 20 * 150, ROOT_EVERY = 14;
    static final DamageSource ROOTS = new DamageSource("eternity.roots").setDamageBypassesArmor().setDamageIsAbsolute();

    private final StoryDirector story;
    private final Random rand = new Random();
    @Nullable
    private EntityNorse gardener, death;
    @Nullable
    private EntityAdamWarlock warlock;
    private long warlockGoesAt = -1;
    /** Whom his roots have hold of, and where they are holding them. */
    private final Map<UUID, Vec3d> held = new HashMap<>();
    private final Map<UUID, Long> nextBloom = new HashMap<>(), lastThrown = new HashMap<>();
    /** How much more of the garden is still to die, once the stone is out of him. */
    private int ruinLeft;
    private boolean brokeThrough;
    /** While the Champion is being walked up to the hedge and tearing through it. */
    private boolean ripping;
    /** The Reality Stone rewriting the hedge out of the world, round from where they stood: ticks in, or -1. */
    private int rewriting = -1;
    private double rewriteFrom;
    /** The Soul Stone drinking the garden dry: ticks in, or -1; and whose hand it is in. */
    private int reaping = -1;
    @Nullable
    private UUID reaper;

    Garden(StoryDirector story) {
        this.story = story;
    }

    void reset() {
        gardener = death = null;
        warlock = null;
        warlockGoesAt = -1;
        held.clear();
        nextBloom.clear();
        lastThrown.clear();
        ruinLeft = 0;
        brokeThrough = false;
        ripping = false;
        rewriting = reaping = -1;
        reaper = null;
    }

    private StoryData data() {
        return story.data();
    }

    // ------------------------------------------------------------------ where

    /** Galacticraft's Mars, if it is there; the overworld if it isn't. */
    static int marsDimension() {
        try {
            Class<?> c = Class.forName("micdoodle8.mods.galacticraft.planets.mars.ConfigManagerMars");
            int id = c.getField("dimensionIDMars").getInt(null);
            if (DimensionManager.isDimensionRegistered(id)) {
                return id;
            }
        } catch (Throwable ignored) {
            // No Mars.
        }
        return 0;
    }

    @Nullable
    GardenPlot plot() {
        StoryData d = data();
        return d.gardenAt == null ? null : new GardenPlot(d.gardenAt, EnumFacing.byHorizontalIndex(d.gardenFacing));
    }

    /** Whether someone is far enough along for the Gardener's chapter to be theirs. */
    static boolean drawn(StoryData.Player p) {
        return p.seen("dir_regulator") && !p.seen("took_time");
    }

    // ------------------------------------------------------------------ ticking

    void tick(MinecraftServer server) {
        StoryData d = data();
        long now = StoryDirector.now();
        if (!StoryDirector.enabled()) {
            return;
        }
        if (now % 20 == 0) {
            for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
                StoryData.Player p = d.player(player.getUniqueID());
                if (!drawn(p) || player.isSpectator()) {
                    continue;
                }
                if (player.dimension == 0 && d.garden != StoryData.Garden.TAKEN) {
                    seed(player, now);
                }
                if (player.dimension == marsDimension() && p.seen("dir_garden")) {
                    arrive(player, p);
                }
            }
        }
        GardenPlot plot = plot();
        WorldServer world = plot == null ? null : DimensionManager.getWorld(d.gardenDim);
        if (plot == null || world == null) {
            return;
        }
        if (!d.gardenBuilt) {
            if (now % 20 == 0 && plot.loaded(world) && anyoneWithin(world, plot.middle, 120D)) {
                build(world, plot);
            }
            beacon(world, plot);
            return;
        }
        beacon(world, plot);
        tend(world, plot, now);
        hold(world, plot);
        EntityAdamWarlock w = warlock;
        if (w != null && (w.isDead || warlockGoesAt >= 0 && now >= warlockGoesAt)) {
            if (!w.isDead) {
                w.setDead();
            }
            warlock = null;
            warlockGoesAt = -1;
        }
        if (rewriting >= 0) {
            rewriteStep(world, plot);
        }
        if (reaping >= 0) {
            reapStep(world, plot);
        }
        if (ruinLeft > 0 && now % 30 == 0) {
            ruinLeft -= Math.max(1, GardenPlot.ruin(world, plot.middle, GardenPlot.RADIUS, 14, rand));
        }
    }

    /** Plants that don't exist, coming up near them in the overworld, now and then. */
    private void seed(EntityPlayerMP player, long now) {
        Long at = nextBloom.get(player.getUniqueID());
        if (at == null) {
            nextBloom.put(player.getUniqueID(), now + 20 * 40);
            return;
        }
        if (now < at) {
            return;
        }
        nextBloom.put(player.getUniqueID(), now + BLOOM_EVERY + rand.nextInt(BLOOM_EVERY));
        World world = player.world;
        for (int tries = 0; tries < 24; tries++) {
            double a = rand.nextDouble() * Math.PI * 2D, r = 10D + rand.nextDouble() * 22D;
            BlockPos top = Barrow.surface(world, (int) Math.floor(player.posX + Math.cos(a) * r), (int) Math.floor(player.posZ + Math.sin(a) * r));
            IBlockState below = world.getBlockState(top.down());
            boolean wrong = below.getBlock() == Blocks.STONE || below.getBlock() == Blocks.SAND || below.getBlock() == Blocks.GRAVEL
                    || below.getBlock() == Blocks.SANDSTONE || below.getBlock() == Blocks.COBBLESTONE || below.getBlock() == Blocks.GRASS;
            if (!wrong || !world.isAirBlock(top) || !world.canSeeSky(top)) {
                continue;
            }
            world.setBlockState(top, EternityBlocks.STRANGE_BLOOM.getDefaultState().withProperty(BlockStrangeBloom.KIND, rand.nextInt(4)), 3);
            StoryData.Player p = data().player(player.getUniqueID());
            if (p.beats.add("bloom_seen")) {
                data().markDirty();
                EternityNetwork.send(MsgStory.line(DIRECTION, -1, "Something is growing out of the bare " + (below.getBlock() == Blocks.GRASS
                        ? "ground" : "rock") + " " + Compass.of(player.getPositionVector(), new Vec3d(top)) + " of you. Nothing like it grows here."), player);
            }
            return;
        }
    }

    /** On Mars, from wherever they come down: green on the horizon. The first time, the garden takes its place there. */
    private void arrive(EntityPlayerMP player, StoryData.Player p) {
        StoryData d = data();
        if (d.gardenAt == null && (!player.onGround || player.isRiding() || player.posY > 200D)) {
            // Still coming down in the lander: the garden takes its place from where they touch down, not where they launched.
            return;
        }
        if (d.gardenAt == null) {
            double a = rand.nextDouble() * Math.PI * 2D;
            int x = (int) Math.floor(player.posX + Math.cos(a) * 110D), z = (int) Math.floor(player.posZ + Math.sin(a) * 110D);
            d.gardenAt = new BlockPos(x, 0, z);
            d.gardenDim = player.dimension;
            d.gardenFacing = rand.nextInt(4);
            d.gardenBuilt = false;
            if (d.garden == StoryData.Garden.UNSEEN) {
                d.garden = StoryData.Garden.GROWING;
            }
            d.markDirty();
        }
        if (d.gardenDim != player.dimension || !p.beats.add("garden_told")) {
            return;
        }
        d.markDirty();
        Talk t = new Talk(player);
        int far = (int) Math.round(Math.hypot(d.gardenAt.getX() + 0.5D - player.posX, d.gardenAt.getZ() + 0.5D - player.posZ) / 10D) * 10;
        t.pause(40)
                .direction("Off to the " + Compass.of(player.getPositionVector(), new Vec3d(d.gardenAt)) + ", about " + far
                        + " blocks across the plain, against all that red: green. A thread of green light stands up out of it.")
                .pause(20)
                .direction("The stones you carry have started ticking. Softly, like a clock. Faster, the closer you get.");
        story.talk(player, t);
        story.sendState(player);
    }

    private void build(WorldServer world, GardenPlot plot) {
        StoryData d = data();
        BlockPos top = Barrow.surface(world, plot.middle.getX(), plot.middle.getZ()).down();
        GardenPlot placed = new GardenPlot(new BlockPos(plot.middle.getX(), top.getY(), plot.middle.getZ()), plot.facing);
        placed.build(world, world.getSeed() ^ placed.middle.toLong());
        d.gardenAt = placed.middle;
        d.gardenBuilt = true;
        if (d.garden == StoryData.Garden.HEDGED && d.gardenBreach.isEmpty()) {
            placed.raiseHedge(world);
        }
        d.markDirty();
    }

    // ------------------------------------------------------------------ him

    /** Keeps him where he should be while anyone is near, doing what he should be doing. */
    private void tend(WorldServer world, GardenPlot plot, long now) {
        StoryData d = data();
        EntityNorse g = Oath.alive(gardener);
        boolean near = anyoneWithin(world, plot.middle, 96D);
        if (!near) {
            if (g != null && !anyoneWithin(world, plot.middle, 128D)) {
                g.setDead();
                gardener = null;
            }
            return;
        }
        if (g == null) {
            BlockPos w = plot.workPlace();
            g = gardener = EntityNorse.of(world, NorseBoxes.GARDENER, "gardener");
            g.setLocationAndAngles(w.getX() + 0.5D, w.getY(), w.getZ() + 0.5D, 0F, 0F);
            g.face(new Vec3d(plot.at(-8, 1, 0)).add(0.5D, 0D, 0.5D));
            world.spawnEntity(g);
            settle(g, d.garden);
        }
        if (now % 10 != 0) {
            return;
        }
        for (EntityPlayer e : world.playerEntities) {
            if (!(e instanceof EntityPlayerMP) || e.isSpectator() || story.talking(e)) {
                continue;
            }
            EntityPlayerMP player = (EntityPlayerMP) e;
            StoryData.Player p = d.player(player.getUniqueID());
            double dist = player.getDistance(g);
            switch (d.garden) {
                case GROWING:
                    if (dist < 12D && !p.seen("gardener_met") && drawn(p)) {
                        talk(player, g, plot);
                        return;
                    }
                    if (p.seen("gardener_met") && held.isEmpty()) {
                        // He tried and they're out of his roots (got clear, or left the world while held): he knows.
                        hedge(world, plot);
                        return;
                    }
                    break;
                case HEDGED:
                    if (d.gardenBreach.isEmpty() && !ripping) {
                        // The Champion at their side, and the hedge in front of them: he has something to say about it.
                        EntityChampion c = story.companion.of(player);
                        double out = Math.hypot(player.posX - plot.middle.getX() - 0.5D, player.posZ - plot.middle.getZ() - 0.5D);
                        if (c != null && out > GardenPlot.HEDGE + 1.5D && out < GardenPlot.HEDGE + 9D && c.getDistance(player) < 16D
                                && Math.abs(player.posY - plot.middle.getY()) < 8D) {
                            championThrough(player, c, plot);
                            return;
                        }
                    }
                    if (plot.inside(player.posX, player.posZ) && Math.abs(player.posY - plot.middle.getY()) < 24D) {
                        if (!d.gardenBreach.isEmpty() && !InfinityStones.wieldsPower(player)) {
                            // His hedge is open: his roots have nothing left to throw anyone out with.
                            if (dist < 9D) {
                                bound(player, g, plot);
                                return;
                            }
                        } else if (InfinityStones.wieldsPower(player)) {
                            if (p.beats.add("roots_broken")) {
                                d.markDirty();
                                EternityNetwork.send(MsgStory.line(DIRECTION, -1,
                                        "Roots come up through the soil at you. The stone in your fist breaks them before they close."), player);
                            }
                            if (dist < 9D) {
                                bound(player, g, plot);
                                return;
                            }
                        } else {
                            throwOut(player, plot, now);
                        }
                    }
                    break;
                case SPARED:
                    if (dist < 5D && p.beats.add("came_back")) {
                        d.markDirty();
                        cameBack(player, g, plot);
                        return;
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /** What he is doing when someone comes, by where things stand. */
    private static void settle(EntityNorse g, StoryData.Garden state) {
        switch (state) {
            case HEDGED:
                g.setArmed(false);
                g.rest(NorsePose.KNEEL).act(NorsePose.KNEEL);
                break;
            case BOUND:
                g.setArmed(false);
                g.setShining(true);
                g.rest(NorsePose.KNEEL).act(NorsePose.KNEEL);
                break;
            case TAKEN:
                g.setArmed(false);
                g.rest(NorsePose.DEAD).act(NorsePose.DEAD);
                break;
            case SPARED:
                g.setShining(true);
                g.setArmed(true);
                g.rest(NorsePose.TEND).act(NorsePose.TEND);
                break;
            default:
                g.setArmed(true);
                g.rest(NorsePose.TEND).act(NorsePose.TEND);
                break;
        }
    }

    @Nullable
    private EntityAdamWarlock warlockBeside(EntityPlayerMP player, EntityNorse g, GardenPlot plot) {
        StoryData.Player p = data().player(player.getUniqueID());
        if (p.warlock != StoryData.Stance.RIVAL && p.warlock != StoryData.Stance.BETRAYED) {
            return null;
        }
        EntityAdamWarlock w = warlock;
        if (w == null || w.isDead) {
            w = warlock = new EntityAdamWarlock(g.world);
            w.script(true);
            Vec3d at = g.getPositionVector().add(new Vec3d(plot.facing.rotateY().getDirectionVec()).scale(1.8D));
            w.setPosition(at.x, at.y, at.z);
            g.world.spawnEntity(w);
            w.direct(EntityAdamWarlock.State.STAND, at, player.getPositionEyes(1F), 0.05D);
        }
        warlockGoesAt = -1;
        return w;
    }

    private void warlockLeaves(EntityAdamWarlock w) {
        Vec3d up = w.getPositionVector().add(0D, 60D, 0D);
        w.direct(EntityAdamWarlock.State.HOVER, up, up.add(10D, 0D, 0D), 0.3D);
        warlockGoesAt = StoryDirector.now() + 100;
    }

    // ------------------------------------------------------------------ scene 5A: the Gardener

    private void talk(EntityPlayerMP player, EntityNorse g, GardenPlot plot) {
        story.mark(player, "gardener_met");
        EntityAdamWarlock w = warlockBeside(player, g, plot);
        Talk t = new Talk(player).at(g, 40D);
        t.run(() -> g.watch(null))
                .line(GARDENER, g, "You came a long way. Sit, if you like.")
                .direction("He goes on working.")
                .pause(30)
                .line(GARDENER, g, "Nothing grew here for four billion years. Now look.")
                .pause(20)
                .run(() -> {
                    g.rest(NorsePose.STAND).act(NorsePose.STAND);
                    g.face(player.getPositionVector());
                    g.watch(player);
                })
                .pause(30)
                .line(GARDENER, g, "I know who you carry for. She stood at the edge of this garden once, the way she does.")
                .line(GARDENER, g, "Waiting for something of mine to die.");
        if (w != null) {
            t.direction("Warlock is standing at his shoulder. He doesn't look at you.");
        }
        t.pause(16)
                .line(GARDENER, g, "I won't give you the stone.")
                .ask(GARDENER, g,
                        say("I can make you.", c -> c.line(GARDENER, g, "Everyone who's come here has thought that.")),
                        say("She'll find another way.", c -> c.line(GARDENER, g, "She always does. That's never been a reason to help her.")),
                        say("What would you do?", c -> c.line(GARDENER, g, "I'd put them down. Every one. I'd go home and grow something.")));
        if (w != null) {
            t.run(() -> w.direct(EntityAdamWarlock.State.STAND, w.getPositionVector(), g.getPositionEyes(1F), 0.05D))
                    .line(WARLOCK, w, "They won't.");
        }
        t.pause(20)
                .line(GARDENER, g, "No.")
                .direction("He stands, still holding his trowel.")
                .line(GARDENER, g, "I didn't think so.")
                .run(() -> g.act(NorsePose.RAISE))
                .pause(56)
                .run(() -> {
                    for (EntityPlayerMP each : story.party(player)) {
                        if (each.isEntityAlive() && each.dimension == g.dimension
                                && (plot.inside(each.posX, each.posZ) || each.getDistance(g) < 28D)) {
                            strangle(each);
                        }
                    }
                    if (held.isEmpty() && g.world instanceof WorldServer) {
                        // Everyone got clear before it closed: he has shown his hand all the same.
                        for (EntityPlayerMP each : story.party(player)) {
                            EternityNetwork.send(MsgStory.line(DIRECTION, -1, "Roots come up where you were standing, and close on nothing."), each);
                        }
                        hedge((WorldServer) g.world, plot);
                    }
                })
                .direction("The ground opens under your feet.");
        t.abandoned(() -> {
            if (data().garden == StoryData.Garden.GROWING && held.isEmpty()) {
                for (EntityPlayerMP each : story.party(player)) {
                    data().player(each.getUniqueID()).beats.remove("gardener_met");
                }
                data().markDirty();
                settle(g, StoryData.Garden.GROWING);
            }
        });
        story.talk(player, t);
    }

    /** The ground opens under someone and his roots take hold of them. */
    void strangle(EntityPlayerMP player) {
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        World world = player.world;
        // Where they stand, not where they are mid-jump.
        BlockPos feet = new BlockPos(player.posX, player.posY + 0.2D, player.posZ);
        for (int i = 0; i < 8 && world.isAirBlock(feet.down()); i++) {
            feet = feet.down();
        }
        for (int y = 1; y <= 4; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    world.setBlockState(feet.add(x, -y, z), Blocks.AIR.getDefaultState(), 3);
                }
            }
        }
        for (int y = 0; y <= 5; y++) {
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    if ((Math.abs(x) == 2 || Math.abs(z) == 2) && y > 0) {
                        world.setBlockState(feet.add(x, -y, z), Blocks.LEAVES.getDefaultState()
                                .withProperty(net.minecraft.block.BlockLeaves.CHECK_DECAY, false), 3);
                    }
                }
            }
        }
        world.playSound(null, feet, SoundEvents.BLOCK_GRAVEL_BREAK, SoundCategory.BLOCKS, 1.5F, 0.4F);
        // Over the middle of the pit, so walking on doesn't carry them onto its rim.
        player.motionX = player.motionZ = 0D;
        player.setPositionAndUpdate(feet.getX() + 0.5D, feet.getY(), feet.getZ() + 0.5D);
        held.put(player.getUniqueID(), new Vec3d(feet.getX() + 0.5D, feet.getY() - 4D, feet.getZ() + 0.5D));
    }

    /** His roots, holding whoever they have, and tightening. */
    private void hold(WorldServer world, GardenPlot plot) {
        if (held.isEmpty()) {
            return;
        }
        long now = StoryDirector.now();
        for (Map.Entry<UUID, Vec3d> e : new HashMap<>(held).entrySet()) {
            EntityPlayerMP p = StoryDirector.player(e.getKey());
            Vec3d at = e.getValue();
            if (p == null || p.dimension != world.provider.getDimension() || p.getDistanceSq(at.x, at.y, at.z) > 40D * 40D) {
                held.remove(e.getKey());
                closePit(world, at);
                hedge(world, plot);
                continue;
            }
            if (!p.isEntityAlive()) {
                continue;
            }
            p.motionX = p.motionZ = 0D;
            p.motionY = Math.min(p.motionY, 0D);
            double dx = p.posX - at.x, dz = p.posZ - at.z;
            if (dx * dx + dz * dz > 0.36D || p.posY < at.y - 0.5D || p.posY > at.y + 4D) {
                // Off the middle, or climbed out: back over the pit, to fall in again.
                p.setPositionAndUpdate(at.x, Math.max(at.y, Math.min(p.posY, at.y + 4D)), at.z);
            }
            p.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 30, 6, false, false));
            p.addPotionEffect(new PotionEffect(MobEffects.MINING_FATIGUE, 30, 3, false, false));
            if (now % ROOT_EVERY == 0) {
                if (p.posY < at.y + 1.5D) {
                    p.addPotionEffect(new PotionEffect(MobEffects.BLINDNESS, 60, 0, false, false));
                }
                p.attackEntityFrom(ROOTS, 3F);
                world.spawnParticle(EnumParticleTypes.BLOCK_CRACK, p.posX, p.posY + 0.6D, p.posZ, 14, 0.3D, 0.4D, 0.3D, 0.05D,
                        net.minecraft.block.Block.getStateId(Blocks.DIRT.getDefaultState()));
                world.playSound(null, p.posX, p.posY, p.posZ, SoundEvents.BLOCK_WOOD_STEP, SoundCategory.BLOCKS, 1F, 0.5F);
            }
        }
    }

    /** Once the roots let go, the ground closes over where they were: earth, and grass on it. */
    private static void closePit(World world, Vec3d at) {
        BlockPos feet = new BlockPos(at.x, at.y + 4D, at.z);
        for (int y = 1; y <= 5; y++) {
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    BlockPos p = feet.add(x, -y, z);
                    boolean dug = Math.abs(x) <= 1 && Math.abs(z) <= 1 && y <= 4;
                    if (dug ? world.isAirBlock(p) : world.getBlockState(p).getBlock() == Blocks.LEAVES) {
                        world.setBlockState(p, y == 1 ? Blocks.GRASS.getDefaultState() : Blocks.DIRT.getDefaultState(), 3);
                    }
                }
            }
        }
    }

    /** Someone his roots had has died. */
    void died(EntityPlayerMP player) {
        if (!held.containsKey(player.getUniqueID())) {
            return;
        }
        closePit(player.world, held.remove(player.getUniqueID()));
        StoryData.Player p = data().player(player.getUniqueID());
        p.beats.add("gardener_killed");
        p.beats.remove("sent_back");
        data().markDirty();
        GardenPlot plot = plot();
        WorldServer world = DimensionManager.getWorld(data().gardenDim);
        if (held.isEmpty() && plot != null && world != null) {
            hedge(world, plot);
        }
    }

    /** What they dropped, the roots push out through the hedge: he doesn't want any of it. */
    boolean drops(EntityPlayerMP player, List<EntityItem> drops) {
        GardenPlot plot = plot();
        if (plot == null || !data().player(player.getUniqueID()).seen("gardener_met") || player.dimension != data().gardenDim
                || !plot.inside(player.posX, player.posZ)) {
            return false;
        }
        BlockPos out = plot.at(0, 0, -GardenPlot.HEDGE - 4);
        BlockPos top = Barrow.surface(player.world, out.getX(), out.getZ());
        for (EntityItem item : drops) {
            item.setPosition(top.getX() + 0.5D, top.getY() + 0.5D, top.getZ() + 0.5D);
            item.motionX = item.motionY = item.motionZ = 0D;
        }
        return true;
    }

    /** Now he knows: the hedge goes up, and he goes down on his knees among what it cost. */
    private void hedge(WorldServer world, GardenPlot plot) {
        StoryData d = data();
        if (d.garden != StoryData.Garden.GROWING) {
            return;
        }
        d.garden = StoryData.Garden.HEDGED;
        d.markDirty();
        plot.raiseHedge(world);
        story.sendStateToAll();
        world.playSound(null, plot.middle, SoundEvents.BLOCK_CHORUS_FLOWER_GROW, SoundCategory.BLOCKS, 4F, 0.5F);
        EntityNorse g = Oath.alive(gardener);
        if (g != null) {
            g.watch(null);
            settle(g, StoryData.Garden.HEDGED);
        }
    }

    /** They wake at home: Death sent them back. */
    void respawned(EntityPlayerMP player) {
        StoryData.Player p = data().player(player.getUniqueID());
        if (!p.seen("gardener_killed") || !p.beats.add("sent_back")) {
            return;
        }
        data().markDirty();
        Talk t = new Talk(player);
        t.pause(40)
                .direction("There was nothing. For a long time, nothing at all.")
                .pause(20)
                .direction("Then something cold and flat against your chest, pushing. She won't have you. Not yet.")
                .pause(30)
                .direction("Your things are on the red plain, outside his hedge. He didn't want any of it.");
        story.talk(player, t);
    }

    /** Pushing into the garden without the stone that breaks his roots: they throw you back out. */
    private void throwOut(EntityPlayerMP player, GardenPlot plot, long now) {
        Vec3d away = new Vec3d(player.posX - plot.middle.getX() - 0.5D, 0D, player.posZ - plot.middle.getZ() - 0.5D);
        away = away.lengthSquared() < 1.0E-4 ? new Vec3d(1D, 0D, 0D) : away.normalize();
        player.motionX = away.x * 1.6D;
        player.motionY = 0.55D;
        player.motionZ = away.z * 1.6D;
        player.velocityChanged = true;
        player.attackEntityFrom(com.matoon.eternity.block.BlockGardenThorn.THORNS, 2F);
        Long last = lastThrown.get(player.getUniqueID());
        if (last == null || now - last > 200) {
            lastThrown.put(player.getUniqueID(), now);
            EternityNetwork.send(MsgStory.line(DIRECTION, -1, "Roots come up through the soil and close on your ankles, and throw you back. "
                    + "Nothing you're holding is strong enough to break them."), player);
        }
    }

    /** The Power Stone in a fist against the hedge: it comes apart. */
    boolean punch(EntityPlayerMP player, BlockPos pos) {
        World world = player.world;
        if (world.getBlockState(pos).getBlock() != EternityBlocks.GARDEN_THORN) {
            return false;
        }
        if (!InfinityStones.wieldsPower(player)) {
            EternityNetwork.send(MsgStory.line(DIRECTION, -1, "It doesn't give. It's alive, and he's keeping it so."), player);
            return true;
        }
        int broken = 0;
        for (BlockPos p : BlockPos.getAllInBox(pos.add(-3, -3, -3), pos.add(3, 3, 3))) {
            if (p.distanceSq(pos) <= 12.5D && world.getBlockState(p).getBlock() == EternityBlocks.GARDEN_THORN) {
                world.setBlockState(p, Blocks.AIR.getDefaultState(), 3);
                broken++;
            }
        }
        if (world instanceof WorldServer) {
            ((WorldServer) world).spawnParticle(EnumParticleTypes.SPELL_WITCH, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 80,
                    1.5D, 1.5D, 1.5D, 0.3D);
            ((WorldServer) world).spawnParticle(EnumParticleTypes.EXPLOSION_LARGE, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 3,
                    1D, 1D, 1D, 0D);
        }
        world.playSound(null, pos, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.2F, 0.6F);
        world.playSound(null, pos, SoundEvents.BLOCK_WOOD_BREAK, SoundCategory.BLOCKS, 2F, 0.4F);
        if (broken > 0) {
            breach("power");
        }
        if (broken > 0 && !brokeThrough) {
            brokeThrough = true;
            EternityNetwork.send(MsgStory.line(DIRECTION, -1, "The stone in your fist flares, and the hedge comes apart round it like wet paper."), player);
        }
        return true;
    }

    // ------------------------------------------------------------------ other ways through

    /** His hedge has been got through, and how: his roots inside stop throwing anyone out. */
    private void breach(String how) {
        StoryData d = data();
        if (!d.gardenBreach.isEmpty()) {
            return;
        }
        d.gardenBreach = how;
        d.markDirty();
        story.sendStateToAll();
    }

    /**
     * The Champion, called to their side by his ticket, walked up to the hedge. He wades into it; it takes hold of
     * him, all of him, and holds; and then it doesn't.
     */
    private void championThrough(EntityPlayerMP player, EntityChampion c, GardenPlot plot) {
        WorldServer world = player.getServerWorld();
        ripping = true;
        Vec3d mid = new Vec3d(plot.middle.getX() + 0.5D, plot.middle.getY() + 1D, plot.middle.getZ() + 0.5D);
        Vec3d out = new Vec3d(player.posX - mid.x, 0D, player.posZ - mid.z).normalize();
        Vec3d stand = ground(world, mid.add(out.scale(GardenPlot.HEDGE + 3.4D)));
        Vec3d into = ground(world, mid.add(out.scale(GardenPlot.HEDGE + 2.2D)));
        Vec3d face = mid.add(out.scale(GardenPlot.HEDGE + 0.5D));
        Talk t = new Talk(player);
        t.abandoned(() -> {
            ripping = false;
            c.free();
            c.post(null);
        })
                .run(() -> c.post(stand))
                .line(CHAMPION, c, "A hedge. He grew a hedge.")
                .until(c::atPost, 100)
                .line(CHAMPION, c, "Stand back. I've been wanting to hit something that won't hit back.")
                .run(() -> c.post(into))
                .until(c::atPost, 40)
                .run(() -> {
                    c.snare(face);
                    world.playSound(null, c.posX, c.posY, c.posZ, SoundEvents.BLOCK_CHORUS_FLOWER_GROW, SoundCategory.BLOCKS, 2F, 0.4F);
                    world.playSound(null, c.posX, c.posY, c.posZ, SoundEvents.BLOCK_WOOD_PLACE, SoundCategory.BLOCKS, 2F, 0.5F);
                })
                .direction("The hedge moves. Stems as thick as your arm come off it and wrap him: his legs, his waist, his arms, until he's held standing in it.")
                .pause(30)
                .line(CHAMPION, c, "Huh.")
                .run(c::strain)
                .direction("He pulls. The stems creak, and tighten, and hold.")
                .pause(EntityChampion.HEAVE * 2)
                .line(CHAMPION, c, "Strong. For a gardener.")
                .pause(10)
                .run(c::burst)
                .pause(EntityChampion.TEAR_RIP)
                .run(() -> {
                    rip(world, plot, face);
                    breach("champion");
                    ripping = false;
                })
                .direction("He roars, and tears his arms apart, and the hedge comes apart with them.")
                .pause(40)
                .run(() -> c.post(null))
                .line(CHAMPION, c, "There. Go and see your gardener.");
        story.talk(player, t);
    }

    /** The top of the ground under a point, as somewhere to stand. */
    private static Vec3d ground(World world, Vec3d at) {
        return new Vec3d(at.x, Barrow.surface(world, MathHelper.floor(at.x), MathHelper.floor(at.z)).getY(), at.z);
    }

    /** The hedge in front of him, torn apart with the vines that held him: a gap a giant could walk through. */
    private static void rip(WorldServer world, GardenPlot plot, Vec3d face) {
        int thorn = Block.getStateId(EternityBlocks.GARDEN_THORN.getDefaultState());
        for (BlockPos p : plot.hedge()) {
            double dx = p.getX() + 0.5D - face.x, dz = p.getZ() + 0.5D - face.z;
            if (dx * dx + dz * dz < 4.2D * 4.2D && world.getBlockState(p).getBlock() == EternityBlocks.GARDEN_THORN) {
                world.setBlockState(p, Blocks.AIR.getDefaultState(), 2);
                world.spawnParticle(EnumParticleTypes.BLOCK_CRACK, p.getX() + 0.5D, p.getY() + 0.5D, p.getZ() + 0.5D, 8, 0.4D, 0.4D, 0.4D, 0.25D, thorn);
            }
        }
        world.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE, face.x, face.y + 2D, face.z, 4, 1.5D, 1.2D, 1.5D, 0D);
        world.playSound(null, face.x, face.y, face.z, SoundEvents.BLOCK_WOOD_BREAK, SoundCategory.BLOCKS, 3F, 0.4F);
        world.playSound(null, face.x, face.y, face.z, SoundEvents.ENTITY_ZOMBIE_BREAK_DOOR_WOOD, SoundCategory.BLOCKS, 2.5F, 0.5F);
    }

    /**
     * The Casket's winter, along a cone from a point (or, with {@code burst} above zero, all round it): whatever of his
     * hedge it reaches freezes through. Frozen, it isn't alive, so he can't keep it so: it is only wood, and breaks.
     */
    void frost(EntityPlayer by, Vec3d from, Vec3d dir, double length, double spread, double burst) {
        StoryData d = data();
        GardenPlot plot = plot();
        if (plot == null || d.garden != StoryData.Garden.HEDGED || by.dimension != d.gardenDim
                || Math.hypot(from.x - plot.middle.getX(), from.z - plot.middle.getZ()) > GardenPlot.HEDGE + Math.max(length, burst) + 4D) {
            return;
        }
        World world = by.world;
        int frozen = 0;
        for (BlockPos p : plot.hedge()) {
            if (world.getBlockState(p).getBlock() != EternityBlocks.GARDEN_THORN) {
                continue;
            }
            Vec3d to = new Vec3d(p.getX() + 0.5D - from.x, p.getY() + 0.5D - from.y, p.getZ() + 0.5D - from.z);
            boolean reached;
            if (burst > 0D) {
                reached = to.length() < burst;
            } else {
                double along = to.dotProduct(dir);
                reached = along > 0D && along < length && to.subtract(dir.scale(along)).length() < 1.5D + along * spread;
            }
            if (reached) {
                world.setBlockState(p, EternityBlocks.THORN_HUSK.getDefaultState(), 2);
                frozen++;
                if (world instanceof WorldServer && rand.nextInt(3) == 0) {
                    ((WorldServer) world).spawnParticle(EnumParticleTypes.SNOW_SHOVEL, p.getX() + 0.5D, p.getY() + 0.5D, p.getZ() + 0.5D, 6,
                            0.4D, 0.4D, 0.4D, 0.02D);
                }
            }
        }
        if (frozen > 0 && by instanceof EntityPlayerMP) {
            world.playSound(null, by.posX, by.posY, by.posZ, SoundEvents.BLOCK_GLASS_PLACE, SoundCategory.BLOCKS, 1.5F, 0.5F);
            StoryData.Player p = d.player(by.getUniqueID());
            if (p.beats.add("hedge_frozen")) {
                d.markDirty();
                EternityNetwork.send(MsgStory.line(DIRECTION, -1, "The winter goes into the hedge. The stems go white and stiff, and stop moving. "
                        + "Frozen through, they aren't alive. They're only wood."), (EntityPlayerMP) by);
            }
        }
    }

    /** A frozen stem broken: it goes like glass, and the frozen stems round it with it. Nothing grows back into the gap. */
    void shattered(EntityPlayerMP player, BlockPos pos) {
        World world = player.world;
        IBlockState frozen = EternityBlocks.THORN_HUSK.getDefaultState();
        for (BlockPos p : BlockPos.getAllInBox(pos.add(-1, -1, -1), pos.add(1, 1, 1))) {
            if (p.distanceSq(pos) <= 2D && world.getBlockState(p) == frozen) {
                world.setBlockState(p, Blocks.AIR.getDefaultState(), 3);
                ((WorldServer) world).spawnParticle(EnumParticleTypes.BLOCK_CRACK, p.getX() + 0.5D, p.getY() + 0.5D, p.getZ() + 0.5D, 10,
                        0.3D, 0.3D, 0.3D, 0.15D, Block.getStateId(Blocks.PACKED_ICE.getDefaultState()));
            }
        }
        world.playSound(null, pos, SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.BLOCKS, 1.5F, 0.6F);
        StoryData d = data();
        GardenPlot plot = plot();
        if (plot == null || d.garden != StoryData.Garden.HEDGED || !d.gardenBreach.isEmpty()) {
            return;
        }
        breach("casket");
        EternityNetwork.send(MsgStory.line(DIRECTION, -1, "The frozen stems break like glass. Where they stood there's a gap, and nothing grows into it."),
                player);
    }

    /** The Reality Stone, held up at his hedge: from where they stand, round both ways, it was never there. */
    boolean rewrite(EntityPlayerMP player) {
        StoryData d = data();
        GardenPlot plot = plot();
        if (plot == null || d.garden != StoryData.Garden.HEDGED || !d.gardenBreach.isEmpty() || rewriting >= 0 || reaping >= 0) {
            return false;
        }
        breach("reality");
        rewriting = 0;
        rewriteFrom = Math.atan2(player.posZ - plot.middle.getZ() - 0.5D, player.posX - plot.middle.getX() - 0.5D);
        WorldServer world = player.getServerWorld();
        world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_ILLAGER_MIRROR_MOVE, SoundCategory.PLAYERS, 2F, 0.5F);
        world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.BLOCK_PORTAL_TRIGGER, SoundCategory.PLAYERS, 0.6F, 1.6F);
        EternityNetwork.send(MsgStory.line(DIRECTION, -1, "You close your hand on the stone and want the hedge gone. Not broken. Gone; never there at all."),
                player);
        return true;
    }

    /** The hedge, going: red, and glassy, and then not there, a wave running round the ring both ways from where they stood. */
    private void rewriteStep(WorldServer world, GardenPlot plot) {
        rewriting++;
        double reach = rewriting * 0.045D;
        double cx = plot.middle.getX() + 0.5D, cz = plot.middle.getZ() + 0.5D;
        IBlockState glass = Blocks.STAINED_GLASS.getDefaultState().withProperty(BlockStainedGlass.COLOR, EnumDyeColor.RED);
        boolean left = false;
        for (BlockPos p : plot.hedge()) {
            IBlockState state = world.getBlockState(p);
            boolean thorn = state.getBlock() == EternityBlocks.GARDEN_THORN || state.getBlock() == EternityBlocks.THORN_HUSK;
            boolean red = state == glass;
            if (!thorn && !red) {
                continue;
            }
            double a = Math.abs(MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(p.getZ() + 0.5D - cz, p.getX() + 0.5D - cx) - rewriteFrom)));
            a = Math.toRadians(a);
            if (red && a <= reach - 0.3D) {
                world.setBlockState(p, Blocks.AIR.getDefaultState(), 2);
                if (rand.nextInt(3) == 0) {
                    world.spawnParticle(EnumParticleTypes.REDSTONE, p.getX() + 0.5D, p.getY() + 0.5D, p.getZ() + 0.5D, 0, 1D, 0.05D, 0.1D, 1D);
                }
            } else if (thorn && a <= reach) {
                world.setBlockState(p, glass, 2);
                left = true;
            } else {
                left = true;
            }
        }
        // The edge of it, running round the ring both ways: a shimmer in the air, where the world is being written over.
        for (int side = -1; side <= 1; side += 2) {
            double a = rewriteFrom + side * Math.min(reach, Math.PI);
            double x = cx + Math.cos(a) * (GardenPlot.HEDGE + 0.5D), z = cz + Math.sin(a) * (GardenPlot.HEDGE + 0.5D);
            for (int i = 0; i < 6; i++) {
                world.spawnParticle(EnumParticleTypes.REDSTONE, x + rand.nextGaussian() * 0.6D, plot.middle.getY() + 1D + rand.nextDouble() * GardenPlot.HEDGE_HEIGHT,
                        z + rand.nextGaussian() * 0.6D, 0, 1D, 0.05D, 0.1D, 1D);
            }
        }
        if (rewriting % 6 == 0) {
            world.playSound(null, cx + Math.cos(rewriteFrom) * GardenPlot.HEDGE, plot.middle.getY() + 2D, cz + Math.sin(rewriteFrom) * GardenPlot.HEDGE,
                    SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.BLOCKS, 1F, 0.5F + rand.nextFloat() * 0.3F);
        }
        if (!left || rewriting > 200) {
            rewriting = -1;
            for (EntityPlayer p : world.playerEntities) {
                if (p instanceof EntityPlayerMP && p.getDistanceSq(cx, p.posY, cz) < 60D * 60D) {
                    EternityNetwork.send(MsgStory.line(DIRECTION, -1, "Where the hedge stood there's only the plain, running right up to the edge of "
                            + "his lawn, as if it always had."), (EntityPlayerMP) p);
                }
            }
        }
    }

    /** The Soul Stone, held up at his hedge: everything alive in his garden, drunk dry. */
    boolean reap(EntityPlayerMP player) {
        StoryData d = data();
        GardenPlot plot = plot();
        if (plot == null || d.garden != StoryData.Garden.HEDGED || !d.gardenBreach.isEmpty() || rewriting >= 0 || reaping >= 0) {
            return false;
        }
        breach("soul");
        reaping = 0;
        reaper = player.getUniqueID();
        WorldServer world = player.getServerWorld();
        world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_WITHER_AMBIENT, SoundCategory.PLAYERS, 1.2F, 0.5F);
        EternityNetwork.send(MsgStory.line(DIRECTION, -1, "You close your hand on the stone, and it opens, and it's hungry."), player);
        return true;
    }

    /** Leaves, stems, grass, the hedge: all of it giving up its life, in threads of light, to the stone in their hand. */
    private void reapStep(WorldServer world, GardenPlot plot) {
        reaping++;
        EntityPlayerMP holder = StoryDirector.player(reaper);
        int r = GardenPlot.HEDGE + 2;
        for (int i = 0; i < 40; i++) {
            int x = rand.nextInt(r * 2 + 1) - r, z = rand.nextInt(r * 2 + 1) - r;
            if (x * x + z * z <= r * r) {
                decayColumn(world, plot.middle.add(x, 0, z));
            }
        }
        if (holder != null && holder.world == world) {
            Vec3d chest = new Vec3d(holder.posX, holder.posY + 1.2D, holder.posZ);
            for (int i = 0; i < 10; i++) {
                double a = rand.nextDouble() * Math.PI * 2D, d = Math.sqrt(rand.nextDouble()) * r;
                Vec3d from = new Vec3d(plot.middle.getX() + 0.5D + Math.cos(a) * d, plot.middle.getY() + 1.2D + rand.nextDouble() * 3D,
                        plot.middle.getZ() + 0.5D + Math.sin(a) * d);
                Vec3d to = chest.subtract(from);
                double len = to.length();
                if (len < 4D) {
                    // Not in their face: the threads are seen coming, not arriving.
                    continue;
                }
                to = to.scale(1D / len);
                world.spawnParticle(EnumParticleTypes.FLAME, from.x, from.y, from.z, 0, to.x, to.y, to.z, Math.min(1.2D, len / 16D));
            }
            if (reaping == 50) {
                EternityNetwork.send(MsgStory.line(DIRECTION, -1, "Everything in the garden gives up its life to it: leaves, stems, grass, the hedge. "
                        + "It all comes to the stone, and through it, into you."), holder);
            }
        }
        if (reaping % 10 == 0) {
            world.playSound(null, plot.middle.getX() + 0.5D, plot.middle.getY() + 2D, plot.middle.getZ() + 0.5D, SoundEvents.BLOCK_GRASS_BREAK,
                    SoundCategory.BLOCKS, 3F, 0.4F);
        }
        if (reaping < 110) {
            return;
        }
        // The last of it, all at once: nothing in the garden left alive but him.
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                if (x * x + z * z <= r * r) {
                    decayColumn(world, plot.middle.add(x, 0, z));
                }
            }
        }
        for (EntityLivingBase e : world.getEntitiesWithinAABB(EntityLivingBase.class, new net.minecraft.util.math.AxisAlignedBB(plot.middle).grow(r, 12D, r))) {
            if (e instanceof EntityAnimal && plot.inside(e.posX, e.posZ)) {
                e.attackEntityFrom(DamageSource.WITHER, Float.MAX_VALUE);
            }
        }
        if (holder != null) {
            holder.addPotionEffect(new PotionEffect(MobEffects.ABSORPTION, 20 * 120, 3, true, false));
            holder.addPotionEffect(new PotionEffect(MobEffects.REGENERATION, 20 * 20, 1, true, false));
            EternityNetwork.send(MsgStory.line(DIRECTION, -1, "It's done. There's nothing green left anywhere in the garden but him, and not much of that."),
                    holder);
        }
        reaping = -1;
        reaper = null;
    }

    /** One column of the garden, dead: whatever grew there gone to stalks and dust, the grass to bare earth, the hedge to husks. */
    private static void decayColumn(World world, BlockPos at) {
        for (int y = 12; y >= -3; y--) {
            BlockPos p = at.up(y);
            IBlockState s = world.getBlockState(p);
            Block b = s.getBlock();
            if (b == Blocks.AIR) {
                continue;
            }
            if (b == EternityBlocks.GARDEN_THORN || b == EternityBlocks.THORN_HUSK && !s.getValue(BlockThornHusk.DEAD)) {
                world.setBlockState(p, EternityBlocks.THORN_HUSK.getDefaultState().withProperty(BlockThornHusk.DEAD, true), 2);
            } else if (b == Blocks.DOUBLE_PLANT || s.getMaterial() == Material.LEAVES || b == Blocks.VINE || b == Blocks.REEDS || b == Blocks.CACTUS) {
                world.setBlockState(p, Blocks.AIR.getDefaultState(), 2);
            } else if (b instanceof BlockBush && b != Blocks.DEADBUSH) {
                Block below = world.getBlockState(p.down()).getBlock();
                boolean earth = below == Blocks.GRASS || below == Blocks.DIRT || below == Blocks.FARMLAND;
                world.setBlockState(p, (earth && (p.getX() + p.getZ()) % 3 == 0 ? Blocks.DEADBUSH : Blocks.AIR).getDefaultState(), 2);
            } else if (b == Blocks.GRASS || b == Blocks.FARMLAND) {
                world.setBlockState(p, Blocks.DIRT.getStateFromMeta(1), 2);
            }
        }
    }

    // ------------------------------------------------------------------ scene 5B: the price

    private void bound(EntityPlayerMP player, EntityNorse g, GardenPlot plot) {
        StoryData d = data();
        story.mark(player, "gardener_bound");
        EntityAdamWarlock w = warlockBeside(player, g, plot);
        WorldServer world = player.getServerWorld();
        String how = d.gardenBreach;
        EntityChampion champion = story.companion.of(player);
        Talk t = new Talk(player).at(g, 40D);
        t.run(() -> {
            g.setArmed(false);
            g.rest(NorsePose.KNEEL).act(NorsePose.KNEEL);
            g.watch(null).look(g.getPositionVector().add(g.getLookVec().scale(2D)));
            GardenPlot.ruin(world, g.getPosition(), 7, 60, rand);
        })
                .direction("soul".equals(how) ? "He's on his knees in the dust that was his garden. There's nothing green left anywhere but him."
                        : "He's on his knees among what's left of his garden. Everything round him has gone grey.")
                .pause(30)
                .run(() -> {
                    g.face(player.getPositionVector());
                    g.watch(player);
                });
        switch (how) {
            case "champion":
                t.line(GARDENER, g, "Tryco. They've put you on a leash.");
                if (champion != null) {
                    t.line(CHAMPION, champion, "Nobody holds my leash, old man. I pay what I owe.");
                }
                t.line(GARDENER, g, "You brought him through my hedge. Of course you did.");
                break;
            case "casket":
                t.line(GARDENER, g, "Winter. You brought winter into a garden.")
                        .line(GARDENER, g, "Of course you did.");
                break;
            case "reality":
                t.line(GARDENER, g, "You didn't even break it. You made it so it was never there.")
                        .line(GARDENER, g, "Of course you did.");
                break;
            case "soul":
                t.line(GARDENER, g, "All of it. Every living thing in it, for one stone.")
                        .line(GARDENER, g, "Of course you did.");
                break;
            default:
                t.line(GARDENER, g, "You broke it. Of course you did.");
                break;
        }
        t.pause(20)
                .run(() -> g.act(NorsePose.PRESS))
                .direction("He takes something out of his robe: a green stone, no bigger than a plum. He holds it a moment.")
                .pause(20)
                .direction("Then he presses it into his own chest.")
                .pause(16)
                .run(() -> {
                    g.setShining(true);
                    d.garden = StoryData.Garden.BOUND;
                    d.markDirty();
                    world.playSound(null, g.posX, g.posY, g.posZ, SoundEvents.BLOCK_CHORUS_FLOWER_GROW, SoundCategory.NEUTRAL, 1.5F, 0.7F);
                })
                .direction("It sinks in. Roots grow out from it under his skin, green, lit from inside.")
                .pause(40)
                .run(() -> g.rest(NorsePose.KNEEL))
                .line(GARDENER, g, "soul".equals(how) ? "There. Now it's part of the last living thing here." : "There. Now it's part of something living.")
                .line(GARDENER, g, "You can have it.")
                .pause(20)
                .line(GARDENER, g, "You'll have to take it out of me.");
        if (w != null) {
            t.run(() -> w.direct(EntityAdamWarlock.State.STAND, w.getPositionVector(), player.getPositionEyes(1F), 0.05D))
                    .line(WARLOCK, w, "Don't.")
                    .run(() -> w.direct(EntityAdamWarlock.State.STAND, w.getPositionVector(), g.getPositionEyes(1F), 0.05D))
                    .line(WARLOCK, w, "Don't make them...")
                    .line(GARDENER, g, "Someone should know what these cost.")
                    .line(GARDENER, g, "Stand aside, child. It has to be their hands.");
        } else {
            t.line(GARDENER, g, "Someone should know what these cost.")
                    .line(GARDENER, g, "It has to be your hands.");
        }
        choose(t, player, g, w);
        story.talk(player, t);
    }

    /** Take it, or leave. */
    private void choose(Talk t, EntityPlayerMP player, EntityNorse g, @Nullable EntityAdamWarlock w) {
        t.ask(GARDENER, g,
                act("(Take it.)", c -> take(c, player, g, w)),
                act("(Leave.)", c -> leave(c, player, g, w)));
    }

    private void take(Talk t, EntityPlayerMP player, EntityNorse g, @Nullable EntityAdamWarlock w) {
        StoryData d = data();
        WorldServer world = player.getServerWorld();
        t.run(() -> {
            g.setArmed(false);
            g.rest(NorsePose.KNEEL).act(NorsePose.KNEEL);
            g.watch(player);
        })
                .direction("You kneel in front of him. He takes your wrist, and puts your hand where the stone is.")
                .pause(40)
                .direction("It comes out of him the way a root comes out of the ground.")
                .run(() -> {
                    giveStone(player);
                    g.setShining(false);
                    g.watch(null).look(null);
                    g.rest(NorsePose.DEAD).act(NorsePose.DEAD);
                    d.garden = StoryData.Garden.TAKEN;
                    d.markDirty();
                    story.mark(player, "took_time");
                    world.playSound(null, g.posX, g.posY, g.posZ, SoundEvents.BLOCK_GRASS_BREAK, SoundCategory.NEUTRAL, 1.5F, 0.4F);
                    deathKneels(world, g, player);
                })
                .pause(30)
                .direction("She is kneeling beside him before he has finished falling. She touches his face. Gently.")
                .pause(70);
        if (w != null) {
            t.direction("Warlock says nothing at all. He goes.")
                    .run(() -> warlockLeaves(w));
        }
        t.pause(20)
                .run(() -> ruinLeft = 900)
                .direction("Round you, slowly, the garden begins to die.");
    }

    private void leave(Talk t, EntityPlayerMP player, EntityNorse g, @Nullable EntityAdamWarlock w) {
        StoryData d = data();
        t.direction("He looks at you for a long time.")
                .pause(30)
                .run(() -> {
                    d.garden = StoryData.Garden.SPARED;
                    d.markDirty();
                    story.mark(player, "left_gardener");
                    for (EntityPlayerMP each : story.party(player)) {
                        story.data().player(each.getUniqueID()).beats.remove("came_back");
                    }
                    GardenPlot plot = plot();
                    if (plot != null) {
                        plot.lowerHedge(player.world);
                    }
                    g.setArmed(true);
                    g.rest(NorsePose.STAND).act(NorsePose.STAND);
                })
                .line(GARDENER, g, "Then go home.")
                .direction("He picks up his trowel, and goes back to his garden.")
                .run(() -> {
                    g.watch(null);
                    g.rest(NorsePose.TEND).act(NorsePose.TEND);
                });
        if (w != null) {
            t.run(() -> warlockLeaves(w));
        }
    }

    /** They come back to him, after leaving it in him. */
    private void cameBack(EntityPlayerMP player, EntityNorse g, GardenPlot plot) {
        EntityAdamWarlock w = warlockBeside(player, g, plot);
        Talk t = new Talk(player).at(g, 40D);
        t.line(GARDENER, g, "You came back.")
                .direction("He doesn't stop working.")
                .pause(20)
                .line(GARDENER, g, "It's still here.");
        choose(t, player, g, w);
        story.talk(player, t);
    }

    private void giveStone(EntityPlayerMP player) {
        ItemStack stone = new ItemStack(anvil.infinity.items.Items.TIME_STONE);
        Relics.claim(stone, player);
        if (!player.inventory.addItemStackToInventory(stone)) {
            player.dropItem(stone, false);
        }
    }

    /** Death, gentle, for the first and only time while they watch: an Elder who opposed her all his life, hers now. */
    private void deathKneels(WorldServer world, EntityNorse g, EntityPlayerMP player) {
        EntityNorse d = Oath.alive(death);
        if (d != null) {
            d.setDead();
        }
        Vec3d side = new Vec3d(-g.getLookVec().z, 0D, g.getLookVec().x).normalize();
        Vec3d at = g.getPositionVector().add(side.scale(1.1D));
        d = death = EntityNorse.of(world, NorseBoxes.DEATH, "garden");
        d.setLocationAndAngles(at.x, at.y, at.z, 0F, 0F);
        d.face(g.getPositionVector());
        world.spawnEntity(d);
        d.look(g.getPositionEyes(1F).subtract(0D, 1.2D, 0D));
        d.rest(NorsePose.TOUCH).act(NorsePose.TOUCH);
    }

    boolean interact(EntityPlayerMP player, EntityNorse npc) {
        if (npc == death) {
            EternityNetwork.send(MsgStory.line(DIRECTION, -1, "She doesn't look up. Her hand stays where it is."), player);
            return true;
        }
        if (npc != gardener) {
            return false;
        }
        GardenPlot plot = plot();
        StoryData d = data();
        switch (d.garden) {
            case TAKEN:
                EternityNetwork.send(MsgStory.line(DIRECTION, -1, "He's still. The roots under his skin have gone dark."), player);
                break;
            case SPARED:
                if (plot != null) {
                    cameBack(player, npc, plot);
                }
                break;
            case HEDGED:
            case BOUND:
                aside(player, npc, "It has to be your hands.");
                break;
            default:
                if (plot != null && !d.player(player.getUniqueID()).seen("gardener_met")) {
                    talk(player, npc, plot);
                } else {
                    aside(player, npc, "Mind the beds.");
                }
                break;
        }
        return true;
    }

    private void aside(EntityPlayerMP player, EntityNorse npc, String text) {
        Talk t = new Talk(player).at(npc, 16D);
        t.line(GARDENER, npc, text);
        story.talk(player, t);
    }

    /**
     * While the garden is still theirs to find: a thread of green light standing up out of it, high
     * enough to be seen from anywhere on that part of the plain, so the green on the red is somewhere
     * you can walk to.
     */
    private void beacon(WorldServer world, GardenPlot plot) {
        StoryData d = data();
        if (StoryDirector.now() % 4 != 0 || d.garden != StoryData.Garden.GROWING && d.garden != StoryData.Garden.HEDGED) {
            return;
        }
        int x = plot.middle.getX(), z = plot.middle.getZ();
        double base = world.isBlockLoaded(plot.middle) ? Barrow.surface(world, x, z).getY() : 72D;
        for (EntityPlayer p : world.playerEntities) {
            if (!(p instanceof EntityPlayerMP) || p.isSpectator() || Math.hypot(p.posX - x, p.posZ - z) > 600D
                    || data().player(p.getUniqueID()).seen("gardener_met")) {
                continue;
            }
            for (int i = 0; i < 6; i++) {
                double y = base + 2D + rand.nextDouble() * 60D;
                world.spawnParticle((EntityPlayerMP) p, EnumParticleTypes.VILLAGER_HAPPY, true, x + 0.5D, y, z + 0.5D, 2, 0.15D, 0.6D, 0.15D, 0D);
            }
            world.spawnParticle((EntityPlayerMP) p, EnumParticleTypes.REDSTONE, true, x + 0.5D, base + 1D + rand.nextDouble() * 50D, z + 0.5D,
                    0, 0.2D, 0.9D, 0.3D, 1D);
        }
    }

    /** Whether anyone is within so many blocks of a point, across the ground (height does not count). */
    private static boolean anyoneWithin(World world, BlockPos c, double r) {
        for (EntityPlayer p : world.playerEntities) {
            double dx = p.posX - c.getX() - 0.5D, dz = p.posZ - c.getZ() - 0.5D;
            if (!p.isSpectator() && dx * dx + dz * dz < r * r) {
                return true;
            }
        }
        return false;
    }

    /** Whether the stones they carry should be ticking: on the planet where the garden is, while it's still theirs to find. */
    boolean ticking(EntityPlayerMP player) {
        StoryData d = data();
        return d.gardenAt != null && player.dimension == d.gardenDim && drawn(d.player(player.getUniqueID()))
                && d.garden != StoryData.Garden.TAKEN && d.player(player.getUniqueID()).seen("garden_told");
    }

    // ------------------------------------------------------------------ the operator's handles

    @Nullable
    String command(EntityPlayerMP player, String action, String[] args) {
        StoryData d = data();
        StoryData.Player p = d.player(player.getUniqueID());
        switch (action) {
            case "garden": {
                // garden [here|reset|hedge|bound|spared|taken]: the garden, built in front of you, in whatever state.
                String how = args.length > 0 ? args[0] : "here";
                if ("near".equals(how) || "outside".equals(how)) {
                    return command(player, how, new String[0]);
                }
                p.beats.add("death_revealed");
                p.beats.add("dir_champion");
                p.beats.add("dir_regulator");
                p.beats.add("dir_garden");
                p.beats.add("garden_told");
                p.beats.remove("took_time");
                if ("here".equals(how) || "reset".equals(how)) {
                    Vec3d look = player.getLookVec();
                    BlockPos at = new BlockPos(player.posX + look.x * 26D, 0, player.posZ + look.z * 26D);
                    EntityNorse g = Oath.alive(gardener);
                    if (g != null) {
                        g.setDead();
                        gardener = null;
                    }
                    d.gardenAt = at;
                    d.gardenDim = player.dimension;
                    d.gardenFacing = EnumFacing.fromAngle(player.rotationYaw).getOpposite().getHorizontalIndex();
                    d.gardenBuilt = false;
                    d.garden = StoryData.Garden.GROWING;
                    held.clear();
                    for (String b : new String[]{"gardener_met", "gardener_killed", "sent_back", "gardener_bound", "left_gardener", "came_back",
                            "roots_broken"}) {
                        p.beats.remove(b);
                    }
                    brokeThrough = false;
                    d.gardenBreach = "";
                    rewriting = reaping = -1;
                    d.markDirty();
                    return "The garden will grow " + (int) player.getDistance(at.getX(), player.posY, at.getZ()) + " blocks in front of you.";
                }
                GardenPlot plot = plot();
                WorldServer world = DimensionManager.getWorld(d.gardenDim);
                if (plot == null || world == null || !d.gardenBuilt) {
                    return "No garden yet (/eternity story garden).";
                }
                EntityNorse g = Oath.alive(gardener);
                switch (how) {
                    case "hedge":
                        // The hedge up and whole again, for trying each way through it.
                        d.garden = StoryData.Garden.GROWING;
                        d.gardenBreach = "";
                        rewriting = reaping = -1;
                        ripping = false;
                        p.beats.add("gardener_met");
                        p.beats.remove("gardener_bound");
                        plot.lowerHedge(world);
                        hedge(world, plot);
                        if (g != null) {
                            settle(g, StoryData.Garden.HEDGED);
                        }
                        break;
                    case "bound":
                        d.garden = StoryData.Garden.BOUND;
                        p.beats.add("gardener_met");
                        plot.lowerHedge(world);
                        if (g != null) {
                            settle(g, StoryData.Garden.BOUND);
                        }
                        break;
                    case "spared":
                        d.garden = StoryData.Garden.SPARED;
                        p.beats.add("gardener_met");
                        p.beats.add("left_gardener");
                        p.beats.remove("came_back");
                        plot.lowerHedge(world);
                        if (g != null) {
                            settle(g, StoryData.Garden.SPARED);
                        }
                        break;
                    default:
                        return "garden [here|hedge|bound|spared]";
                }
                d.markDirty();
                return "The garden: " + d.garden + ".";
            }
            case "near":
            case "outside": {
                // near: six blocks in front of him, facing him. outside: on the path outside the hedge, facing in.
                GardenPlot plot = plot();
                EntityNorse g = Oath.alive(gardener);
                if (plot == null) {
                    return "No garden.";
                }
                Vec3d to, face;
                if ("near".equals(action)) {
                    // On the grass six blocks off the bed he works at, facing him.
                    to = new Vec3d(plot.at(2, 1, 0)).add(0.5D, 0D, 0.5D);
                    face = g != null ? g.getPositionEyes(1F) : new Vec3d(plot.workPlace()).add(0.5D, 1D, 0.5D);
                } else {
                    BlockPos out = plot.at(0, 0, -GardenPlot.HEDGE - 3);
                    BlockPos top = Barrow.surface(player.world, out.getX(), out.getZ());
                    to = new Vec3d(top.getX() + 0.5D, top.getY(), top.getZ() + 0.5D);
                    face = new Vec3d(plot.middle.getX() + 0.5D, top.getY() + 2D, plot.middle.getZ() + 0.5D);
                }
                BlockPos top = Barrow.surface(player.world, (int) Math.floor(to.x), (int) Math.floor(to.z));
                double dx = face.x - to.x, dz = face.z - to.z;
                float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
                float pitch = (float) -Math.toDegrees(Math.atan2(face.y - top.getY() - 1.62D, Math.sqrt(dx * dx + dz * dz)));
                player.connection.setPlayerLocation(to.x, top.getY(), to.z, yaw, pitch);
                return "There.";
            }
            case "roots":
                strangle(player);
                return "The ground opens.";
            case "price": {
                EntityNorse g = Oath.alive(gardener);
                GardenPlot plot = plot();
                if (g == null || plot == null) {
                    return "No Gardener here.";
                }
                d.garden = StoryData.Garden.HEDGED;
                d.markDirty();
                bound(player, g, plot);
                return "The price.";
            }
            case "bloom":
                nextBloom.put(player.getUniqueID(), 0L);
                p.beats.add("dir_regulator");
                p.beats.remove("bloom_seen");
                d.markDirty();
                seed(player, StoryDirector.now());
                return "Something is growing.";
            default:
                return null;
        }
    }
}
