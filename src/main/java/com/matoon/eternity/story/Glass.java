package com.matoon.eternity.story;

import com.matoon.eternity.compat.InfinityStones;
import com.matoon.eternity.entity.EntityAdamWarlock;
import com.matoon.eternity.entity.EntityInBetweener;
import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.network.MsgStory;
import com.matoon.eternity.world.Director;
import com.matoon.eternity.world.Nexus;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.matoon.eternity.story.Speakers.DIRECTION;
import static com.matoon.eternity.story.Speakers.IN_BETWEENER;
import static com.matoon.eternity.story.Speakers.WARLOCK;
import static com.matoon.eternity.story.Talk.act;
import static com.matoon.eternity.story.Talk.say;

/**
 * Chapter 4, the Nexus, laid over the In-Betweener's glass as it already stands.
 * <p>
 * The Champion told them where: a thing in a glass jar, where all the worlds touch. With three
 * stones on them the world starts to give way round them, now and then, onto that place: a rift
 * tears open in front of them, and through it is the grey disc and every sky at once. It closes
 * again before long, and opens again later, until they go through.
 * <p>
 * If Warlock is their rival he got there first. He has been at the glass for an hour, telling the
 * In-Betweener not to deal with them, and the In-Betweener, who has been bored for ten thousand
 * years, has worked out what he is. It is the first time anyone has said it to his face.
 * <p>
 * What follows inside the glass is the In-Betweener's own conversation, with one new turn in it
 * (InBetweenerDialogue.IMBALANCE): he can see her standing behind them, and he tells them what
 * the stones are for. Order and Chaos, if they come, warn them Eternity will.
 */
final class Glass {

    /** Stones on someone before the world starts giving way onto the Nexus round them. */
    static final int STONES_FOR_RIFTS = 2;
    private static final int FIRST_RIFT = 20 * 30, RIFT_EVERY = 20 * 60 * 5;
    /** Where a rift onto the Nexus lets out: the edge of the grey disc, facing his glass. */
    private static final double ARRIVE_X = Nexus.JUNCTION_X - 13.5D, ARRIVE_Z = Nexus.JUNCTION_Z + 13.5D;

    private final StoryDirector story;
    private final Map<UUID, Long> nextRift = new HashMap<>();
    @Nullable
    private EntityAdamWarlock warlock;
    private long warlockGoesAt = -1;

    Glass(StoryDirector story) {
        this.story = story;
    }

    void reset() {
        nextRift.clear();
        warlock = null;
        warlockGoesAt = -1;
    }

    /** Whether someone is far enough along to be pulled toward the jar. */
    static boolean drawn(StoryData.Player p) {
        return p.seen("death_revealed") && p.seen("champion_debt") && !p.seen("nexus_reached");
    }

    void tick(MinecraftServer server) {
        long now = StoryDirector.now();
        if (now % 20 == 0) {
            for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
                StoryData.Player p = story.data().player(player.getUniqueID());
                if (player.dimension == Nexus.dimension()) {
                    if (p.seen("death_revealed") && p.beats.add("nexus_reached")) {
                        story.data().markDirty();
                    }
                    glassSide(player, p);
                } else {
                    pull(player, p, now);
                }
            }
        }
        EntityAdamWarlock w = warlock;
        if (w != null && (w.isDead || warlockGoesAt >= 0 && now >= warlockGoesAt)) {
            if (!w.isDead && w.isScripted()) {
                w.setDead();
            }
            warlock = null;
            warlockGoesAt = -1;
        }
    }

    // ------------------------------------------------------------------ the way in

    /** Now and then, once they carry enough, the world gives way in front of them onto the Nexus. */
    private void pull(EntityPlayerMP player, StoryData.Player p, long now) {
        if (!StoryDirector.enabled() || !drawn(p) || player.dimension != 0 || player.isSpectator() || !player.onGround
                || story.talking(player) || InfinityStones.countHeld(player) < STONES_FOR_RIFTS
                || (InfinityStones.scan(player) & InfinityStones.bit(lucraft.mods.lucraftcore.infinity.EnumInfinityStone.SOUL)) != 0) {
            nextRift.remove(player.getUniqueID());
            return;
        }
        Long at = nextRift.get(player.getUniqueID());
        if (at == null) {
            nextRift.put(player.getUniqueID(), now + FIRST_RIFT);
            return;
        }
        if (now < at) {
            return;
        }
        nextRift.put(player.getUniqueID(), now + RIFT_EVERY);
        rift(player, !p.seen("nexus_rift"));
        p.beats.add("nexus_rift");
        story.data().markDirty();
    }

    /** A rift onto the edge of the grey disc, torn open in front of them. */
    void rift(EntityPlayerMP player, boolean first) {
        Director.INSTANCE.openGate(player, new Director.Destination(Nexus.dimension(), ARRIVE_X, Nexus.JUNCTION_Y, ARRIVE_Z));
        player.world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.BLOCK_PORTAL_TRIGGER, SoundCategory.AMBIENT, 0.8F, 0.5F);
        EternityNetwork.send(MsgStory.line(DIRECTION, -1, first
                ? "The air in front of you splits. Through it: a grey floor, and every sky there is, all at once."
                : "It's opened again. The grey floor, and every sky."), player);
        if (first) {
            Talk t = new Talk(player);
            t.pause(80).direction("Where all the worlds touch. It won't stay open long.");
            story.talk(player, t);
        }
    }

    // ------------------------------------------------------------------ two made things

    @Nullable
    private static EntityInBetweener inBetweener(World world) {
        Vec3d c = EntityInBetweener.bubbleCentre();
        List<EntityInBetweener> found = world.getEntitiesWithinAABB(EntityInBetweener.class, new AxisAlignedBB(c.x - 24, c.y - 24, c.z - 24,
                c.x + 24, c.y + 24, c.z + 24));
        return found.isEmpty() ? null : found.get(0);
    }

    private static double fromGlass(EntityPlayerMP player) {
        double dx = player.posX - Nexus.JUNCTION_X, dz = player.posZ - Nexus.JUNCTION_Z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    /**
     * Coming up to the glass: Warlock is there already, whatever he is to them. A rival or a man
     * they betrayed, at the glass for the stone himself; an ally, there ahead of them to warn them.
     * Not once the stone is off the In-Betweener's head: there is nothing left to be there for.
     */
    private void glassSide(EntityPlayerMP player, StoryData.Player p) {
        if (!StoryDirector.enabled() || !p.seen("death_revealed") || p.seen("glass_warlock") || story.talking(player)
                || warlock != null && !warlock.isDead || !com.matoon.eternity.world.InBetweenerDirector.INSTANCE.wearsGem()) {
            return;
        }
        boolean met = p.warlock != StoryData.Stance.UNMET && p.warlock != StoryData.Stance.REFUSED;
        double d = fromGlass(player);
        if (!met || d > 30D || d < 6D || Math.abs(player.posY - Nexus.JUNCTION_Y) > 12D) {
            return;
        }
        WorldServer world = player.getServerWorld();
        EntityInBetweener him = inBetweener(world);
        if (him == null || him.isDead) {
            return;
        }
        twoMadeThings(player, world, him, p.warlock == StoryData.Stance.BETRAYED);
    }

    void twoMadeThings(EntityPlayerMP player, WorldServer world, EntityInBetweener him, boolean betrayed) {
        story.mark(player, "glass_warlock");
        Vec3d glass = EntityInBetweener.bubbleCentre();
        // Warlock hangs just off the glass on the player's side of it, his back to them.
        Vec3d off = new Vec3d(player.posX - glass.x, 0D, player.posZ - glass.z);
        Vec3d toPlayer = off.lengthSquared() < 1.0E-4 ? new Vec3d(-1D, 0D, 1D) : off.normalize();
        Vec3d side = new Vec3d(-toPlayer.z, 0D, toPlayer.x);
        Vec3d at = glass.add(toPlayer.scale(8.5D)).add(side.scale(3.5D));
        Vec3d post = new Vec3d(at.x, Nexus.JUNCTION_Y + 1.6D, at.z);
        EntityAdamWarlock w = warlock = new EntityAdamWarlock(world);
        w.script(true);
        w.setPosition(post.x, post.y, post.z);
        if (betrayed) {
            w.evolve();
        }
        world.spawnEntity(w);
        w.direct(EntityAdamWarlock.State.HOVER, post, glass, 0.1D);
        warlockGoesAt = -1;

        Talk t = new Talk(player).at(him, 48D);
        t.direction("Someone is already at the glass, both fists against it. Gold skin. A red cape.")
                .pause(30)
                .run(() -> w.direct(EntityAdamWarlock.State.HOVER, post, player.getPositionEyes(1F), 0.1D))
                .line(IN_BETWEENER, him, "Another one.")
                .line(IN_BETWEENER, him, "He's been shouting at my glass about you for an hour.")
                .pause(10)
                .line(IN_BETWEENER, him, "He's made, isn't he? You can always tell. They stand like they're waiting to be told what to do.")
                .run(() -> w.direct(EntityAdamWarlock.State.HOVER, post, glass, 0.1D))
                .line(WARLOCK, w, "I am not...")
                .line(IN_BETWEENER, him, "Order and Chaos made me. Somebody made you. And you're still doing as you're told.")
                .line(IN_BETWEENER, him, "At least I'm in a jar. What's your excuse?")
                .pause(30)
                .direction("Warlock has no answer. He looks at you as if you'd said it.")
                .run(() -> w.direct(EntityAdamWarlock.State.HOVER, post, player.getPositionEyes(1F), 0.1D))
                .ask(WARLOCK, w, 20 * 12, 2,
                        say("Leave him alone.", c -> c
                                .line(IN_BETWEENER, him, "Defending him! Oh, that's sweet. You two should get a jar of your own.")
                                .run(() -> trust(player, 1))),
                        say("He's got a point.", c -> c
                                .line(WARLOCK, w, "You'd know.")
                                .run(() -> trust(player, -1))),
                        act("(Say nothing.)", c -> c.pause(20)));
        boolean ally = story.data().player(player.getUniqueID()).warlock == StoryData.Stance.ALLY;
        if (ally) {
            t.line(WARLOCK, w, "He has one of the stones on his head. The Magus will come for it, if we leave it there.")
                    .line(WARLOCK, w, "He'll want to play. Play. He's bored, not cruel. Just don't step into the glass.")
                    .run(() -> {
                        Vec3d up = post.add(toPlayer.scale(-30D)).add(0D, 40D, 0D);
                        w.direct(EntityAdamWarlock.State.HOVER, up, up.add(toPlayer.scale(-10D)), 0.35D);
                        warlockGoesAt = StoryDirector.now() + 120;
                    })
                    .pause(40)
                    .line(IN_BETWEENER, him, "Friends! How awful. Now. Where were we?");
        } else if (betrayed) {
            t.line(WARLOCK, w, "You gave me your word, and broke it. I'm not leaving here empty-handed twice.")
                    .run(() -> {
                        w.script(false);
                        w.setCalm(false);
                    });
        } else {
            t.line(WARLOCK, w, "Don't deal with him. Whatever he offers you, it's hers by the time it's in your hand.")
                    .run(() -> {
                        Vec3d up = post.add(toPlayer.scale(-30D)).add(0D, 40D, 0D);
                        w.direct(EntityAdamWarlock.State.HOVER, up, up.add(toPlayer.scale(-10D)), 0.35D);
                        warlockGoesAt = StoryDirector.now() + 120;
                    })
                    .pause(40)
                    .line(IN_BETWEENER, him, "Finally. Now. Where were we?");
        }
        t.abandoned(() -> {
            if (!betrayed) {
                warlockGoesAt = StoryDirector.now() + 20;
            }
        });
        story.talk(player, t);
    }

    private void trust(EntityPlayerMP player, int by) {
        for (EntityPlayerMP each : story.party(player)) {
            story.data().player(each.getUniqueID()).trust += by;
        }
        story.data().markDirty();
    }

    // ------------------------------------------------------------------ the operator's handles

    @Nullable
    String command(EntityPlayerMP player, String action, String[] args) {
        StoryData.Player p = story.data().player(player.getUniqueID());
        switch (action) {
            case "rift":
                p.beats.add("death_revealed");
                p.beats.add("champion_debt");
                p.beats.remove("nexus_reached");
                story.data().markDirty();
                rift(player, true);
                return "A rift onto the Nexus.";
            case "glass": {
                WorldServer world = DimensionManager.getWorld(Nexus.dimension());
                EntityInBetweener him = world == null || player.dimension != Nexus.dimension() ? null : inBetweener(world);
                if (him == null) {
                    return "Go to the Nexus first (/eternity nexus).";
                }
                p.beats.add("death_revealed");
                p.beats.remove("glass_warlock");
                story.data().markDirty();
                if (warlock != null) {
                    // Played again: the one already at the glass makes way.
                    warlock.setDead();
                    warlock = null;
                }
                story.hush(player);
                twoMadeThings(player, world, him, args.length > 0 && "betrayed".equals(args[0]));
                return "Warlock is at the glass.";
            }
            default:
                return null;
        }
    }
}
