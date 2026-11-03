package com.matoon.eternity.story;

import com.matoon.eternity.client.render.NorseBoxes;
import com.matoon.eternity.entity.EntityNorse;
import com.matoon.eternity.entity.NorsePose;
import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.network.MsgStory;
import com.matoon.eternity.world.OdinsVault;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import com.matoon.eternity.compat.InfinityStones;
import lucraft.mods.lucraftcore.infinity.EnumInfinityStone;
import net.minecraft.block.BlockBush;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;

import static com.matoon.eternity.story.Speakers.DEATH;
import static com.matoon.eternity.story.Talk.act;
import static com.matoon.eternity.story.Talk.say;

/**
 * Death, after the vault. She doesn't speak any more: she has been believed, and doesn't need
 * to be. She is simply there when something dies at your hand, standing over it in what she
 * really is, and what she wants of you next she shows you.
 * <p>
 * The first time: her face turned up past the sky, past the moon and the red planet, to the last
 * blue world and beyond it; and a fist raised, as if holding up a champion's belt. Once they have
 * the Soul Stone: her face up as if waiting for something to cross the sky, a finger pointing
 * straight up, her hands closing round something small (the Comet, and the Regulator that holds
 * what it carries). Then her palm on the ground, killing what grows under it, and her arm toward
 * the red planet. And if they left the Gardener his stone, she is there, and only waits.
 */
final class Presence {

    private final StoryDirector story;
    @Nullable
    private EntityNorse death;

    Presence(StoryDirector story) {
        this.story = story;
    }

    void reset() {
        death = null;
    }

    void tick(MinecraftServer server) {
        EntityNorse d = Oath.alive(death);
        if (d != null && StoryDirector.now() % 20 == 0) {
            EntityPlayer near = d.world.getClosestPlayerToEntity(d, 64D);
            boolean busy = near != null && story.talking(near);
            if (!busy && d.ticksExisted > 200 && !Oath.watched(d.world, d) || near == null) {
                d.setDead();
                death = null;
            }
        }
    }

    /** Something has died at someone's hand: if she has something to show them, she is standing over it. */
    void died(EntityLivingBase dead, DamageSource source) {
        if (!(source.getTrueSource() instanceof EntityPlayerMP) || !(dead instanceof EntityLiving) || dead instanceof EntityNorse
                || !StoryDirector.enabled()) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) source.getTrueSource();
        StoryData.Player p = story.data().player(player.getUniqueID());
        if (!p.seen("death_revealed") || player.dimension == OdinsVault.dimension() || story.talking(player) || Oath.alive(death) != null
                || !player.world.provider.hasSkyLight()) {
            return;
        }
        int held = InfinityStones.scan(player);
        boolean soul = (held & InfinityStones.bit(EnumInfinityStone.SOUL)) != 0;
        if (!p.seen("dir_champion")) {
            champion(player, dead.getPositionVector());
        } else if (soul && !p.seen("dir_regulator") && (p.seen("champion_debt") || p.seen("magus_risen"))) {
            regulator(player, dead.getPositionVector());
        } else if (soul && p.seen("dir_regulator") && !p.seen("dir_garden")) {
            garden(player, dead.getPositionVector());
        } else if (p.seen("left_gardener") && !p.seen("took_time")) {
            waiting(player, dead.getPositionVector());
        }
    }

    private EntityNorse standOver(EntityPlayerMP player, Vec3d at) {
        World world = player.world;
        EntityNorse d = death = EntityNorse.of(world, NorseBoxes.DEATH, "death");
        d.setLocationAndAngles(at.x, at.y, at.z, 0F, 0F);
        d.face(player.getPositionVector());
        world.spawnEntity(d);
        d.watch(player);
        return d;
    }

    /**
     * After the glass: the comet, and what holds it. Her face up, a long time, as if waiting for
     * something to cross the sky; then a finger, straight up; then her hands cupped round
     * something small, and closed on it.
     */
    private void regulator(EntityPlayerMP player, Vec3d at) {
        EntityNorse d = standOver(player, at);
        Talk t = new Talk(player).at(d, 48D);
        t.pause(40)
                .run(() -> d.watch(null).look(d.getPositionEyes(1F).add(0D, 40D, 0D)).act(NorsePose.SKY))
                .direction("She looks up at the sky, a long time, as if waiting for something to cross it.")
                .pause(50)
                .run(() -> d.act(NorsePose.POINT))
                .direction("Then she points. Straight up.")
                .pause(50)
                .run(() -> d.look(d.getPositionEyes(1F).add(d.getLookVec().scale(2D))).act(NorsePose.CUP))
                .direction("She brings her hands together round something small, and closes them on it.")
                .pause(40)
                .ask(DEATH, d,
                        say("The comet?", c -> c.run(() -> d.act(NorsePose.POINT))
                                .direction("Her finger stays up until you look where it points.")),
                        say("Something to hold it in?", c -> c.run(() -> d.act(NorsePose.CUP))
                                .direction("Her closed hands stay closed. Whatever is in them, she means you to keep it.")),
                        act("(Say nothing.)", c -> c.pause(20)))
                .run(() -> d.watch(player).act(NorsePose.STAND).rest(NorsePose.STAND))
                .run(() -> {
                    story.mark(player, "dir_regulator");
                    // What she pointed at comes: across their own sky, a minute or so after.
                    com.matoon.eternity.world.CometDirector.INSTANCE.promise(player, 20 * 50);
                });
        story.talk(player, t);
    }

    /**
     * Toward the Garden: she crouches and lays her palm flat on the ground, and where she touches
     * it nothing grows; then she points low in the sky, where the red planet rises.
     */
    private void garden(EntityPlayerMP player, Vec3d at) {
        EntityNorse d = standOver(player, at);
        Talk t = new Talk(player).at(d, 48D);
        t.pause(40)
                .run(() -> d.watch(null).look(d.getPositionVector().add(d.getLookVec().scale(1.5D))).act(NorsePose.GROUND))
                .direction("She crouches, and lays her palm flat on the ground.")
                .pause(40)
                .run(() -> wither(d))
                .direction("Where her hand is, the grass goes grey. Nothing grows there.")
                .pause(50)
                .run(() -> d.rest(NorsePose.STAND).act(NorsePose.STAND))
                .pause(20)
                .run(() -> d.look(d.getPositionEyes(1F).add(30D, 14D, 0D)).act(NorsePose.POINT))
                .direction("She stands, and points low in the sky: east, where the red planet comes up.")
                .pause(40)
                .ask(DEATH, d,
                        say("Mars?", c -> c.direction("She doesn't lower her arm.")),
                        say("Who's there?", c -> c.run(() -> d.act(NorsePose.GROUND))
                                .direction("She crouches again, and touches the ground where it's grey. Then the green beside it.")),
                        act("(Say nothing.)", c -> c.pause(20)))
                .run(() -> d.watch(player).act(NorsePose.STAND).rest(NorsePose.STAND))
                .run(() -> story.mark(player, "dir_garden"));
        story.talk(player, t);
    }

    /** Where she lays her hand, the ground dies: grass to dirt, flowers and grass gone. */
    private static void wither(EntityNorse d) {
        World world = d.world;
        Vec3d hand = d.getPositionVector().add(d.getLookVec().scale(0.9D));
        BlockPos c = new BlockPos(hand.x, d.posY - 0.5D, hand.z);
        for (BlockPos p : BlockPos.getAllInBox(c.add(-1, -1, -1), c.add(1, 1, 1))) {
            if (p.distanceSq(c) > 2.1D) {
                continue;
            }
            IBlockState s = world.getBlockState(p);
            if (s.getBlock() == Blocks.GRASS) {
                world.setBlockState(p, Blocks.DIRT.getStateFromMeta(1), 3);
            } else if (s.getBlock() instanceof BlockBush) {
                world.setBlockState(p, Blocks.DEADBUSH.getDefaultState(), 3);
            }
        }
        world.playSound(null, c, SoundEvents.BLOCK_GRASS_BREAK, SoundCategory.BLOCKS, 0.6F, 0.5F);
    }

    /** They left the Gardener with his stone: she is there at the next death, and says nothing, and waits. */
    private void waiting(EntityPlayerMP player, Vec3d at) {
        EntityNorse d = standOver(player, at);
        Talk t = new Talk(player).at(d, 48D);
        t.pause(60)
                .direction("She stands over it, and looks at you. She doesn't point anywhere. She has already shown you.")
                .pause(80)
                .run(() -> d.watch(null))
                .direction("She waits.");
        story.talk(player, t);
    }

    /** Past the last blue world: a fighter, and a belt. */
    private void champion(EntityPlayerMP player, Vec3d at) {
        StoryData data = story.data();
        data.player(player.getUniqueID()).beats.add("dir_champion");
        data.markDirty();
        World world = player.world;
        EntityNorse d = death = EntityNorse.of(world, NorseBoxes.DEATH, "death");
        d.setLocationAndAngles(at.x, at.y, at.z, 0F, 0F);
        d.face(player.getPositionVector());
        world.spawnEntity(d);
        d.watch(player);
        Talk t = new Talk(player).at(d, 48D);
        t.pause(40)
                .run(() -> d.watch(null).look(d.getPositionEyes(1F).add(0D, 40D, 0D)).act(NorsePose.SKY))
                .direction("She turns her face up toward the sky: past the moon, past the red one, to the last blue world. And keeps going.")
                .pause(60)
                .run(() -> d.act(NorsePose.FIST))
                .direction("Then she raises a fist, slowly, as if holding up a belt.")
                .pause(40)
                .ask(DEATH, d,
                        say("Where?", c -> c.run(() -> d.act(NorsePose.SKY))
                                .direction("She keeps her face to the sky until you look where she is looking.")),
                        say("What's there?", c -> c.run(() -> d.act(NorsePose.FIST))
                                .direction("She holds the fist where it is. Somewhere a crowd would be roaring.")),
                        act("(Say nothing.)", c -> c.pause(20)))
                .run(() -> d.watch(player).act(NorsePose.STAND).rest(NorsePose.STAND))
                .run(() -> story.mark(player, "dir_champion"));
        story.talk(player, t);
    }

    boolean interact(EntityPlayerMP player, EntityNorse npc) {
        if (npc != death) {
            return false;
        }
        EternityNetwork.send(MsgStory.line(Speakers.DIRECTION, -1, "She looks at you. She has already shown you."), player);
        return true;
    }

    @Nullable
    String command(EntityPlayerMP player, String action, String[] args) {
        if ("death".equals(action)) {
            // death [champion|regulator|garden|waiting]: she stands in front of you and shows you that.
            StoryData.Player p = story.data().player(player.getUniqueID());
            p.beats.add("death_revealed");
            String which = args.length > 0 ? args[0] : "champion";
            story.data().markDirty();
            Vec3d look = player.getLookVec();
            Vec3d at = new Vec3d(player.posX + look.x * 4D, player.posY, player.posZ + look.z * 4D);
            if (Oath.alive(death) != null) {
                death.setDead();
                death = null;
            }
            switch (which) {
                case "regulator":
                    regulator(player, at);
                    break;
                case "garden":
                    garden(player, at);
                    break;
                case "waiting":
                    waiting(player, at);
                    break;
                default:
                    champion(player, at);
                    break;
            }
            return "She is here.";
        }
        return null;
    }
}
