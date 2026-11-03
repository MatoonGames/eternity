package com.matoon.eternity.story;

import com.matoon.eternity.EternitySounds;
import com.matoon.eternity.entity.EntityAdamWarlock;
import com.matoon.eternity.entity.EntityAdamWarlock.State;
import com.matoon.eternity.entity.EntityMagus;
import com.matoon.eternity.entity.EntityMagus.Pose;
import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.network.MsgTribunalFx;
import com.matoon.eternity.world.SovereignStation;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;

import java.util.List;
import java.util.Random;

import static com.matoon.eternity.story.Speakers.DIRECTION;
import static com.matoon.eternity.story.Speakers.MAGUS;
import static com.matoon.eternity.story.Speakers.WARLOCK;
import static com.matoon.eternity.story.Timeline.in;
import static com.matoon.eternity.story.Timeline.lerp;
import static com.matoon.eternity.story.Timeline.out;
import static com.matoon.eternity.story.Timeline.smooth;

/**
 * Warlock against the Magus, in the Sanctum, staged to the tick: what he is about to become
 * against what he was always going to be. Nobody else is in it; whoever is watching is only
 * watching.
 * <ol>
 * <li>Warlock flares: everything he has comes out of him at once, gold, and every mirror in
 * the room bursts.</li>
 * <li>He throws the first punch, and it lands: the Magus goes through the far wall's
 * mirror. Warlock is on him again before he is up: jab, cross, and an uppercut that puts
 * him through the roof. The top of the dome comes off.</li>
 * <li>Up there, over the hole, with the Earth under them, Warlock pours everything into a
 * beam; the Magus holds it off with one hand, struggling with it, and then not.</li>
 * <li>Warlock comes at him again. The Magus catches the fist, and remembers throwing it.
 * Then he takes him apart: a palm to the chest, gone and behind him, a backhand, gone and in
 * front of him, a knee, gone and above him, both fists down.</li>
 * <li>Warlock goes through the Sanctum floor. The Magus comes down after him, unhurried.</li>
 * </ol>
 * Then it is the scene's again (the throat, the stone).
 */
final class MagusDuel {

    private final WorldServer world;
    private final EntityAdamWarlock w;
    private final EntityMagus m;
    private final Timeline line = new Timeline();
    private final Random rand = new Random();
    private final int dim;

    // Where things were when a movement started, set by the moment that starts it.
    private Vec3d wFrom, wTo, mFrom, mTo, dir, side;
    /** Where Warlock comes down through the floor. */
    private final Vec3d crater;

    MagusDuel(WorldServer world, EntityAdamWarlock w, EntityMagus m, Runnable done) {
        this.world = world;
        this.w = w;
        this.m = m;
        this.dim = world.provider.getDimension();
        this.crater = new Vec3d(SovereignStation.SANCTUM_X + 3.5D, SovereignStation.FLOOR, 1.5D);
        flare();
        firstBlood();
        throughTheRoof();
        theBeam();
        theTurn();
        theFall(done);
    }

    /** One tick on; false once it is over. */
    boolean tick() {
        if (w.isDead || m.isDead) {
            return false;
        }
        return line.tick();
    }

    void stop() {
        line.stop();
    }

    // ------------------------------------------------------------------ 1. the flare

    private void flare() {
        line.at(0, () -> {
            w.puppet(true);
            w.perform(State.FLARE);
            wFrom = w.getPositionVector();
            wTo = wFrom.add(0D, 1.6D, 0D);
            m.rest(Pose.REGARD).perform(Pose.REGARD);
            m.watch(w);
            fx(MsgTribunalFx.WARLOCK_CHARGE, w.chest(), new Vec3d(4D, 0D, 0D), w.getEntityId(), EntityAdamWarlock.FLARE_BURST);
            sound(w.chest(), SoundEvents.BLOCK_PORTAL_TRIGGER, 2.5F, 1.3F);
            sound(w.chest(), SoundEvents.EVOCATION_ILLAGER_PREPARE_SUMMON, 2F, 0.6F);
        });
        line.span(0, EntityAdamWarlock.FLARE_BURST, k -> w.place(lerp(wFrom, wTo, smooth(k)), eyes(m)));
        line.at(EntityAdamWarlock.FLARE_BURST, () -> {
            w.evolve();
            fx(MsgTribunalFx.WARLOCK_EMERGE, w.chest(), new Vec3d(10D, 0D, 0D), w.getEntityId(), 40);
            fx(MsgTribunalFx.WARLOCK_BOOM, w.chest(), new Vec3d(0D, 1D, 0D), 0, 22);
            sound(w.chest(), SoundEvents.ENTITY_GENERIC_EXPLODE, 3F, 0.7F);
            sound(w.chest(), EternitySounds.COMET_BLAST, 3F, 1.3F);
            sound(w.chest(), SoundEvents.ENTITY_ENDERDRAGON_GROWL, 1.6F, 1.7F);
            // Every mirror in the room goes at once.
            for (BlockPos pane : SovereignStation.mirrors()) {
                Vec3d out = new Vec3d(pane).add(0.5D, 0.5D, 0.5D).subtract(w.chest()).normalize();
                smash(pane, out.scale(0.25D), rand.nextInt(3) == 0);
            }
            StoryDirector.shout(w, WARLOCK, w.getEntityId(), "You don't get to be me.", 96D);
        });
        line.span(EntityAdamWarlock.FLARE_BURST, 56, k -> w.place(wTo.add(0D, Math.sin(k * 30D) * 0.04D, 0D), eyes(m)));
    }

    // ------------------------------------------------------------------ 2. the first punch, which lands

    private void firstBlood() {
        line.at(56, () -> w.perform(State.WINDUP));
        line.span(56, 66, k -> w.place(w.getPositionVector(), eyes(m)));
        line.at(67, () -> {
            w.perform(State.FLYBY);
            wFrom = w.getPositionVector();
            Vec3d toward = flat(wFrom.subtract(m.getPositionVector()));
            wTo = m.getPositionVector().add(toward.scale(1.1D));
            sound(wFrom, EternitySounds.COMET_FLYBY, 3F, 1.6F);
            fx(MsgTribunalFx.WARLOCK_BOOM, w.chest(), wTo.subtract(wFrom), 0, 14);
        });
        line.span(67, 71, k -> w.place(lerp(wFrom, wTo, in(k)), eyes(m)));
        line.at(71, () -> {
            mFrom = m.getPositionVector();
            mTo = new Vec3d(SovereignStation.SANCTUM_X - 6.5D, SovereignStation.FLOOR + 3.5D, -10.5D);
            dir = mTo.subtract(mFrom).normalize();
            hit(MsgTribunalFx.WARLOCK_IMPACT, chest(m), dir, m, 1);
            sound(chest(m), SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, 3F, 0.5F);
            sound(chest(m), SoundEvents.ENTITY_GENERIC_EXPLODE, 2.5F, 1.1F);
            m.perform(Pose.STAGGER);
            w.perform(State.TURN);
        });
        line.span(71, 79, k -> m.place(lerp(mFrom, mTo, out(k)), eyes(w)));
        line.span(72, 80, k -> w.place(w.getPositionVector(), eyes(m)));
        line.at(79, () -> {
            // Into the wall, through the frame of what was a mirror.
            Vec3d wall = mTo.add(dir.scale(1.6D));
            hit(MsgTribunalFx.WARLOCK_IMPACT, wall, dir, null, 2);
            sound(wall, SoundEvents.ENTITY_GENERIC_EXPLODE, 3F, 0.9F);
            sound(wall, SoundEvents.BLOCK_ANVIL_LAND, 1.5F, 0.5F);
            crumble(new BlockPos(wall), 2.2D, dir.scale(-0.35D));
            m.rest(Pose.GUARD).perform(Pose.GUARD);
        });
    }

    // ------------------------------------------------------------------ 3. after him, and the roof

    private void throughTheRoof() {
        line.at(84, () -> {
            w.perform(State.FLYBY);
            wFrom = w.getPositionVector();
            dir = flat(m.getPositionVector().subtract(wFrom));
            wTo = m.getPositionVector().subtract(dir.scale(1.5D));
            sound(wFrom, EternitySounds.COMET_FLYBY, 3F, 1.8F);
        });
        line.span(84, 88, k -> w.place(lerp(wFrom, wTo, in(k)), eyes(m)));
        line.at(88, () -> w.perform(State.COMBO));
        line.span(88, 108, k -> w.place(w.getPositionVector(), eyes(m)));
        // The jab, the cross: each one lands, and each one moves him.
        jolt(88 + EntityAdamWarlock.COMBO_JAB, 0, 0.45D);
        jolt(88 + EntityAdamWarlock.COMBO_CROSS, 1, 0.8D);
        int upper = 88 + EntityAdamWarlock.COMBO_UPPER;
        line.at(upper, () -> {
            hit(MsgTribunalFx.WARLOCK_IMPACT, chest(m).add(0D, -0.3D, 0D), new Vec3d(0D, 1D, 0D), m, 2);
            sound(chest(m), SoundEvents.ENTITY_GENERIC_EXPLODE, 3F, 0.8F);
            sound(chest(m), SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK, 3F, 0.5F);
            m.perform(Pose.FLUNG);
            mFrom = m.getPositionVector();
            mTo = new Vec3d(SovereignStation.SANCTUM_X - 3.5D, SovereignStation.FLOOR + 36D, -5.5D);
        });
        line.span(upper, upper + 14, k -> m.place(lerp(mFrom, mTo, out(k)), null));
        // He goes through the dome, and the top of it goes with him.
        int breach = upper + 3;
        List<BlockPos> cap = SovereignStation.domeCap(18, world.getTotalWorldTime());
        line.at(breach, () -> {
            Vec3d top = new Vec3d(SovereignStation.SANCTUM_X, SovereignStation.FLOOR + 20D, 0D);
            fx(MsgTribunalFx.MAGUS_BREACH, top, new Vec3d(12D, 0D, 0D), 0, 44);
            sound(top, SoundEvents.ENTITY_GENERIC_EXPLODE, 6F, 0.5F);
            sound(top, SoundEvents.ENTITY_GENERIC_EXPLODE, 6F, 0.75F);
            sound(top, SoundEvents.ENTITY_LIGHTNING_THUNDER, 5F, 0.8F);
            sound(top, SoundEvents.BLOCK_GLASS_BREAK, 4F, 0.6F);
            sound(top, EternitySounds.COMET_IMPACT, 5F, 0.9F);
        });
        for (int part = 0; part < 4; part++) {
            int from = cap.size() * part / 4, to = cap.size() * (part + 1) / 4;
            line.at(breach + part, () -> {
                Vec3d middle = new Vec3d(SovereignStation.SANCTUM_X + 0.5D, SovereignStation.FLOOR + 8D, 0.5D);
                for (int i = from; i < to; i++) {
                    BlockPos p = cap.get(i);
                    Vec3d away = new Vec3d(p).add(0.5D, 0.5D, 0.5D).subtract(middle).normalize();
                    smash(p, away.scale(0.5D + rand.nextDouble() * 0.9D).add(0D, 0.3D, 0D), i % 3 == 0);
                }
            });
        }
        line.at(upper + 4, () -> {
            w.perform(State.RISE);
            wFrom = w.getPositionVector();
            wTo = new Vec3d(SovereignStation.SANCTUM_X + 3.5D, SovereignStation.FLOOR + 33D, 3.5D);
            sound(wFrom, EternitySounds.COMET_FLYBY, 3F, 1.4F);
            fx(MsgTribunalFx.WARLOCK_BOOM, w.chest(), new Vec3d(0D, 1D, 0D), 0, 18);
        });
        line.span(upper + 4, upper + 18, k -> w.place(lerp(wFrom, wTo, smooth(k)), eyes(m)));
        line.at(upper + 14, () -> {
            // Over the hole, with the Earth under him: he rights himself as if nothing had happened.
            m.perform(Pose.GUARD);
            mFrom = m.getPositionVector();
            mTo = mFrom.add(0.5D, -1.5D, 0.5D);
            fx(MsgTribunalFx.WARLOCK_LAND, chest(m), new Vec3d(5D, 0D, 0D), 0, 20);
        });
        line.span(upper + 14, upper + 30, k -> m.place(lerp(mFrom, mTo, smooth(k)), eyes(w)));
        line.at(upper + 19, () -> {
            w.perform(State.HOVER);
            fx(MsgTribunalFx.WARLOCK_LAND, w.chest(), new Vec3d(5D, 0D, 0D), 0, 20);
        });
        line.span(upper + 19, upper + 30, k -> w.place(w.getPositionVector(), eyes(m)));
    }

    /** One of the combo's blows landing on him: the light of it, and him moved back by it. */
    private void jolt(int at, int kind, double back) {
        line.at(at, () -> {
            Vec3d d = flat(m.getPositionVector().subtract(w.getPositionVector()));
            hit(MsgTribunalFx.WARLOCK_IMPACT, chest(m).subtract(d.scale(0.3D)), d, m, kind);
            sound(chest(m), kind == 0 ? SoundEvents.ENTITY_PLAYER_ATTACK_STRONG : SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, 2.5F, 0.6F);
            m.perform(Pose.STAGGER);
            mFrom = m.getPositionVector();
            mTo = mFrom.add(d.scale(back));
            wFrom = w.getPositionVector();
        });
        line.span(at, at + 3, k -> {
            m.place(lerp(mFrom, mTo, out(k)), eyes(w));
            w.place(lerp(wFrom, wFrom.add(mTo.subtract(mFrom)), out(k)), eyes(m));
        });
    }

    // ------------------------------------------------------------------ 4. everything he has, held off with one hand

    private void theBeam() {
        int start = 138, charge = start + EntityAdamWarlock.BEAM_CHARGE, end = charge + EntityAdamWarlock.BEAM_FIRE;
        line.at(start - 4, () -> StoryDirector.shout(w, WARLOCK, w.getEntityId(), "You're not what I become. You're nothing!", 160D));
        line.at(start, () -> {
            w.perform(State.BEAM);
            fx(MsgTribunalFx.WARLOCK_CHARGE, hands(), new Vec3d(3D, 0D, 0D), w.getEntityId(), EntityAdamWarlock.BEAM_CHARGE);
            sound(w.chest(), SoundEvents.EVOCATION_ILLAGER_PREPARE_SUMMON, 3F, 1.4F);
        });
        line.span(start, end + 12, k -> w.place(w.getPositionVector(), eyes(m)));
        line.at(charge - 4, () -> {
            m.perform(Pose.BLOCK);
            m.faceNow(w.chest());
        });
        line.at(charge, () -> {
            mFrom = m.getPositionVector();
            mTo = mFrom.add(m.getPositionVector().subtract(w.getPositionVector()).normalize().scale(3.2D));
            sound(w.chest(), SoundEvents.BLOCK_END_PORTAL_SPAWN, 4F, 0.6F);
            sound(w.chest(), SoundEvents.ENTITY_GUARDIAN_ATTACK, 3F, 0.5F);
        });
        line.span(charge, end, k -> {
            // He gives ground, a little, and then none.
            m.place(lerp(mFrom, mTo, out(Math.min(1F, k * 1.6F))), w.chest());
            Vec3d from = hands(), palm = m.palm();
            fx(MsgTribunalFx.WARLOCK_BEAM, from, palm, line.now(), 3);
            if (line.now() % 5 == 0) {
                hit(MsgTribunalFx.MAGUS_IMPACT, palm, palm.subtract(from), null, 0);
            }
            if (line.now() % 8 == 0) {
                sound(palm, SoundEvents.BLOCK_LAVA_EXTINGUISH, 2F, 0.5F);
                sound(palm, SoundEvents.ENTITY_GUARDIAN_ATTACK, 1.5F, 0.6F);
            }
        });
        line.at(end + 6, () -> m.rest(Pose.FLOAT).perform(Pose.FLOAT));
        line.at(end + 14, () -> StoryDirector.shout(m, MAGUS, m.getEntityId(), "Are you finished?", 160D));
    }

    // ------------------------------------------------------------------ 5. the turn

    private void theTurn() {
        int windup = 226, go = windup + EntityAdamWarlock.WINDUP_EVOLVED, caught = go + 4;
        line.at(windup, () -> w.perform(State.WINDUP));
        line.span(windup, go - 1, k -> w.place(w.getPositionVector(), eyes(m)));
        line.at(go, () -> {
            w.perform(State.FLYBY);
            wFrom = w.getPositionVector();
            dir = m.getPositionVector().subtract(wFrom).normalize();
            wTo = m.getPositionVector().subtract(dir.scale(1.35D));
            sound(wFrom, EternitySounds.COMET_FLYBY, 3F, 1.9F);
            fx(MsgTribunalFx.WARLOCK_BOOM, w.chest(), dir, 0, 14);
        });
        line.span(go, caught, k -> w.place(lerp(wFrom, wTo, in(k)), eyes(m)));
        line.at(caught - 1, () -> {
            m.rest(Pose.GUARD).perform(Pose.CATCH);
            m.faceNow(w.chest());
        });
        line.at(caught, () -> {
            w.perform(State.CAUGHT);
            Vec3d mid = chest(m).add(chest(w)).scale(0.5D);
            hit(MsgTribunalFx.MAGUS_IMPACT, mid, dir.scale(-1D), null, 1);
            fx(MsgTribunalFx.WARLOCK_LAND, mid, new Vec3d(7D, 0D, 0D), 0, 24);
            sound(mid, SoundEvents.BLOCK_ANVIL_LAND, 2.5F, 0.5F);
            sound(mid, SoundEvents.ENTITY_GENERIC_EXPLODE, 2F, 1.3F);
        });
        line.span(caught, caught + 22, k -> {
            w.place(wTo, eyes(m));
            m.place(m.getPositionVector(), w.chest());
        });
        line.at(caught + 6, () -> StoryDirector.shout(m, MAGUS, m.getEntityId(), "I remember this. I threw this punch, a long time ago.", 160D));

        // A palm to the chest.
        int palm = caught + 22, palmHit = palm + EntityMagus.PALM_HIT;
        line.at(palm, () -> m.perform(Pose.PALM));
        blow(palmHit, () -> dir, 3.2D, 5, 1);

        // Gone, and behind him: the back of the hand.
        int out1 = palmHit + 3, in1 = out1 + EntityMagus.BLINK_OUT_TICKS, back = in1 + EntityMagus.BLINK_IN_TICKS;
        line.at(out1, () -> m.perform(Pose.BLINK_OUT));
        line.at(in1, () -> {
            side = new Vec3d(-dir.z, 0D, dir.x).normalize();
            m.blink(wTo.add(dir.scale(1.5D)).subtract(side.scale(0.9D)).add(0D, -0.2D, 0D), w.chest(), false);
        });
        int backHit = back + EntityMagus.BACKHAND_HIT;
        line.at(back, () -> {
            m.faceNow(w.chest());
            m.perform(Pose.BACKHAND);
        });
        blow(backHit, () -> side, 5.5D, 15, 1);

        // Gone, and where he is going: the knee.
        int out2 = backHit + 2, in2 = out2 + EntityMagus.BLINK_OUT_TICKS, knee = in2 + 2, kneeHit = knee + EntityMagus.KNEE_HIT;
        line.at(out2, () -> m.perform(Pose.BLINK_OUT));
        line.at(in2, () -> m.blink(wTo.add(side.scale(1.1D)).add(0D, -0.4D, 0D), w.chest(), false));
        line.at(knee, () -> {
            m.faceNow(w.chest());
            m.perform(Pose.KNEE);
        });
        line.at(kneeHit, () -> {
            hit(MsgTribunalFx.MAGUS_IMPACT, chest(w).add(0D, -0.4D, 0D), new Vec3d(0D, 1D, 0D), w, 1);
            sound(chest(w), SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK, 3F, 0.5F);
            sound(chest(w), SoundEvents.ENTITY_PLAYER_HURT, 2F, 0.7F);
            w.perform(State.STAGGER);
            wFrom = w.getPositionVector();
            wTo = wFrom.add(0D, 1.8D, 0D).add(side.scale(0.3D));
        });
        line.span(kneeHit, kneeHit + 6, k -> w.place(lerp(wFrom, wTo, out(k)), eyes(m)));

        // Gone, and above him: both fists, down.
        int out3 = kneeHit + 4, in3 = out3 + EntityMagus.BLINK_OUT_TICKS, hammer = in3 + EntityMagus.BLINK_IN_TICKS;
        line.at(out3, () -> m.perform(Pose.BLINK_OUT));
        line.at(in3, () -> m.blink(wTo.add(0D, 2.5D, 0D).subtract(dir.scale(0.6D)), w.chest(), false));
        line.at(hammer, () -> {
            m.faceNow(w.chest());
            m.perform(Pose.HAMMER);
        });
        line.span(kneeHit + 7, hammer + EntityMagus.HAMMER_HIT, k -> w.place(wTo.add(0D, Math.sin(k * 12D) * 0.05D, 0D), eyes(m)));
        int fall = hammer + EntityMagus.HAMMER_HIT;
        line.at(fall, () -> {
            hit(MsgTribunalFx.MAGUS_IMPACT, w.getPositionVector().add(0D, 2D, 0D), new Vec3d(0D, -1D, 0D), w, 2);
            sound(w.chest(), SoundEvents.ENTITY_GENERIC_EXPLODE, 4F, 0.6F);
            sound(w.chest(), SoundEvents.BLOCK_ANVIL_LAND, 2F, 0.4F);
            w.perform(State.STAGGER);
            wFrom = w.getPositionVector();
        });
        line.span(fall, fall + 10, k -> w.place(lerp(wFrom, crater, in(k)), null));
        landing = fall + 10;
    }

    private int landing;

    /** One of the Magus's blows landing on Warlock, throwing him {@code far} along a way worked out when it lands. */
    private void blow(int at, java.util.function.Supplier<Vec3d> way, double far, int ticks, int kind) {
        line.at(at, () -> {
            Vec3d d = way.get();
            hit(MsgTribunalFx.MAGUS_IMPACT, chest(w), d, w, kind);
            sound(chest(w), SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK, 3F, 0.6F);
            sound(chest(w), SoundEvents.ENTITY_GENERIC_EXPLODE, 1.8F, 1.4F);
            w.perform(State.STAGGER);
            wFrom = w.getPositionVector();
            wTo = wFrom.add(d.scale(far)).add(0D, 0.2D, 0D);
        });
        line.span(at, at + ticks, k -> w.place(lerp(wFrom, wTo, out(k)), eyes(m)));
    }

    // ------------------------------------------------------------------ 6. through the floor, and him coming down after

    private void theFall(Runnable done) {
        // Worked out once the turn has placed it.
        int land = landing;
        line.at(land, () -> {
            w.perform(State.DOWNED);
            w.place(crater, crater.add(-4D, 0D, 0D));
            crater(new BlockPos(crater), 2.6D);
            fx(MsgTribunalFx.WARLOCK_SLAM, crater, new Vec3d(9D, 0D, 0D), 0, 30);
            hit(MsgTribunalFx.MAGUS_IMPACT, crater.add(0D, 0.6D, 0D), new Vec3d(0D, 1D, 0D), null, 2);
            sound(crater, SoundEvents.ENTITY_GENERIC_EXPLODE, 5F, 0.45F);
            sound(crater, EternitySounds.COMET_IMPACT, 4F, 0.8F);
            sound(crater, SoundEvents.BLOCK_ANVIL_LAND, 2F, 0.3F);
            for (EntityPlayer p : world.playerEntities) {
                double d = p.getDistance(crater.x, crater.y, crater.z);
                if (d < 8D && !p.isSpectator()) {
                    Vec3d away = flat(p.getPositionVector().subtract(crater));
                    double push = 0.9D * (1D - d / 8D);
                    p.motionX += away.x * push;
                    p.motionY += 0.35D * (1D - d / 8D);
                    p.motionZ += away.z * push;
                    p.velocityChanged = true;
                }
            }
            m.rest(Pose.FLOAT).perform(Pose.FLOAT);
            m.watch(w);
        });
        line.span(land, land + 20, k -> w.place(crater, crater.add(-4D, 0D, 0D)));
        line.at(land + 12, () -> w.spent());
        line.at(land + 18, () -> StoryDirector.shout(m, DIRECTION, -1,
                "He looks down at what is left of Warlock for a long moment. Then he comes down after him.", 160D));
        int descend = land + 30, arrive = descend + 56;
        line.at(descend, () -> {
            m.perform(Pose.DESCEND);
            mFrom = m.getPositionVector();
            mTo = crater.add(-2.1D, 1.2D, -0.4D);
        });
        line.span(descend, arrive, k -> m.place(lerp(mFrom, mTo, smooth(k)), w.getPositionVector().add(0D, 0.4D, 0D)));
        line.span(land + 21, arrive + 2, k -> w.place(crater, null));
        line.at(arrive + 4, () -> {
            m.rest(Pose.FLOAT).perform(Pose.FLOAT);
            done.run();
        });
    }

    // ------------------------------------------------------------------ the room coming apart

    /** A block torn out: shards of it and dust, and, now and then, the block itself flung away into the air. */
    private void smash(BlockPos pos, Vec3d fling, boolean whole) {
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() == Blocks.AIR) {
            return;
        }
        world.setBlockState(pos, Blocks.AIR.getDefaultState(), 2);
        int id = Block.getStateId(state);
        if (rand.nextInt(4) == 0) {
            world.playEvent(2001, pos, id);
        } else {
            world.spawnParticle(EnumParticleTypes.BLOCK_CRACK, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 6,
                    0.3D, 0.3D, 0.3D, 0.15D, id);
        }
        if (whole) {
            EntityFallingBlock debris = new EntityFallingBlock(world, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, state);
            debris.setNoGravity(true);
            debris.shouldDropItem = false;
            debris.fallTime = 1;
            debris.motionX = fling.x;
            debris.motionY = fling.y;
            debris.motionZ = fling.z;
            world.spawnEntity(debris);
        }
    }

    /** The wall behind him, where he hit it: the frame and the panels round it torn out. */
    private void crumble(BlockPos at, double reach, Vec3d fling) {
        int r = MathHelper.ceil(reach);
        for (BlockPos p : BlockPos.getAllInBoxMutable(at.add(-r, -r, -r), at.add(r, r, r))) {
            if (p.distanceSq(at) <= reach * reach && p.getY() > SovereignStation.FLOOR) {
                smash(p.toImmutable(), fling.add(new Vec3d(rand.nextGaussian(), rand.nextGaussian(), rand.nextGaussian()).scale(0.12D)),
                        rand.nextInt(3) == 0);
            }
        }
    }

    /** Where Warlock went through the floor: the plates of it gone in a ring, flung up, the dark under them showing. */
    private void crater(BlockPos at, double reach) {
        int r = MathHelper.ceil(reach);
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                if (x * x + z * z > reach * reach) {
                    continue;
                }
                BlockPos p = at.add(x, 0, z);
                Vec3d up = new Vec3d(x, 0D, z).scale(0.08D).add(rand.nextGaussian() * 0.05D, 0.45D + rand.nextDouble() * 0.5D,
                        rand.nextGaussian() * 0.05D);
                smash(p, up, rand.nextInt(2) == 0);
            }
        }
    }

    // ------------------------------------------------------------------ helpers

    private void hit(byte type, Vec3d at, Vec3d way, Entity struck, int kind) {
        fx(type, at, way, struck == null ? -1 : struck.getEntityId(), kind);
    }

    private void fx(byte type, Vec3d at, Vec3d to, int id, int time) {
        EternityNetwork.sendToDimension(new MsgTribunalFx(type, at.x, at.y, at.z, to.x, to.y, to.z, id, time), dim);
    }

    private void sound(Vec3d at, SoundEvent sound, float volume, float pitch) {
        world.playSound(null, at.x, at.y, at.z, sound, SoundCategory.HOSTILE, volume, pitch);
    }

    private static Vec3d eyes(Entity e) {
        return e.getPositionEyes(1F);
    }

    private static Vec3d chest(Entity e) {
        return e.getPositionVector().add(0D, e.height * 0.62D, 0D);
    }

    /** Between Warlock's palms, held out in front of him. */
    private Vec3d hands() {
        float yaw = (float) Math.toRadians(w.rotationYaw), pitch = (float) Math.toRadians(w.rotationPitch);
        Vec3d look = new Vec3d(-MathHelper.sin(yaw) * MathHelper.cos(pitch), -MathHelper.sin(pitch), MathHelper.cos(yaw) * MathHelper.cos(pitch));
        return w.chest().add(look.scale(0.95D));
    }

    private static Vec3d flat(Vec3d v) {
        Vec3d f = new Vec3d(v.x, 0D, v.z);
        return f.lengthSquared() < 1.0E-6 ? new Vec3d(1D, 0D, 0D) : f.normalize();
    }
}
