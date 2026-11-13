package com.matoon.eternity.story;

import com.matoon.eternity.client.render.NorseBoxes;
import com.matoon.eternity.entity.EntityAdamWarlock;
import com.matoon.eternity.entity.EntityChampion;
import com.matoon.eternity.entity.EntityNorse;
import com.matoon.eternity.entity.NorsePose;
import com.matoon.eternity.compat.InfinityStones;
import com.matoon.eternity.compat.Relics;
import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.network.MsgTribunalFx;
import com.matoon.eternity.world.ChampionData;
import com.matoon.eternity.world.ChampionDirector;
import com.matoon.eternity.world.SovereignData;
import com.matoon.eternity.world.Tamarata;
import lucraft.mods.lucraftcore.infinity.EnumInfinityStone;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.matoon.eternity.story.Speakers.CHAMPION;
import static com.matoon.eternity.story.Speakers.DEATH;
import static com.matoon.eternity.story.Speakers.DIRECTION;
import static com.matoon.eternity.story.Speakers.WARLOCK;
import static com.matoon.eternity.story.Talk.act;
import static com.matoon.eternity.story.Talk.say;

/**
 * Chapter 3, the Champion, laid over his Colosseum as it already stands.
 * <p>
 * Death sits in the stands while he fights, as she has at every fight he has ever had, waiting
 * for him to lose; he has noticed. If Warlock is the player's rival he comes down onto the sand
 * between them, and the Champion, who cares only who earns the stone, makes it a fight for both.
 * When Planet Breaker has taken everything and the Champion is adrift in what is left, she is
 * standing in the wreck of the stands, and he has two words for her. And when he has had his
 * rocket and given the stone, he pays what he owes: where the next one is, and a warning that
 * is not care, only what an old fighter has noticed.
 */
final class Arena {

    private final StoryDirector story;
    @Nullable
    private EntityNorse death;
    @Nullable
    private EntityAdamWarlock warlock;
    private boolean warlockLeaving;
    /** Who Warlock is, to the ledger of stones, while one is his. */
    /** World beat: Warlock came down onto the sand and has not left it. Whenever the old man is there, so is he. */
    private static final String IN_ARENA = "warlock_in_arena";
    /** World beat: he tore the stone out of the old man's brow when the world broke, and is fighting with it. */
    static final String EMPOWERED = "warlock_empowered";
    /** World beat: someone beat him with it in him, and it came out of him into their hand. */
    static final String DISARMED = "warlock_disarmed";
    public static final UUID WARLOCK_ID = UUID.nameUUIDFromBytes("eternity:adam_warlock".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    /** The theft, once someone has fallen to him in the wreck: how far along, and whose stone it is. */
    private int heist = -1;
    @Nullable
    private EntityChampion heistFrom;
    private ItemStack heistStone = ItemStack.EMPTY;
    private Vec3d heistAt = Vec3d.ZERO;
    /** The stone on its way out of his brow to whoever beat him: who, what, and when it gets there. */
    @Nullable
    private UUID handTo;
    private ItemStack handStone = ItemStack.EMPTY;
    private long handAt;
    private Vec3d handFrom = Vec3d.ZERO;
    /** Those who woke after he took it, and when she is to be standing there. */
    private final Map<UUID, Long> pointFor = new HashMap<>();
    /** Her showing them, while it lasts: she stays until it is over. */
    @Nullable
    private Talk pointing;

    Arena(StoryDirector story) {
        this.story = story;
    }

    /** Everyone of this chapter's taken off the field, and everything it was in the middle of forgotten. */
    void clear() {
        if (warlock != null) {
            warlock.setDead();
        }
        if (death != null) {
            death.setDead();
        }
        reset();
    }

    void reset() {
        death = null;
        warlock = null;
        warlockLeaving = false;
        heist = -1;
        heistFrom = null;
        heistStone = ItemStack.EMPTY;
        handTo = null;
        handStone = ItemStack.EMPTY;
        pointFor.clear();
        pointing = null;
    }

    /** The nearest player still on their feet within {@code range} of something; null if nobody. */
    @Nullable
    private static EntityPlayerMP nearestAlive(net.minecraft.entity.Entity at, double range) {
        EntityPlayerMP best = null;
        for (EntityPlayer p : at.world.playerEntities) {
            if (p instanceof EntityPlayerMP && p.isEntityAlive() && !p.isSpectator() && p.getDistance(at) < range
                    && (best == null || p.getDistance(at) < best.getDistance(at))) {
                best = (EntityPlayerMP) p;
            }
        }
        return best;
    }

    void tick(MinecraftServer server) {
        EntityAdamWarlock w = warlock;
        if (w != null) {
            if (w.isDead) {
                warlock = null;
            } else if (!warlockLeaving && w.getState() == EntityAdamWarlock.State.RETREAT && armed()) {
                warlockLeaving = true;
                if (w.getHealth() <= 1F && w.beatenBy() == null) {
                    disarmed(w);
                } else {
                    fled(w);
                }
            } else if (!warlockLeaving && w.getState() == EntityAdamWarlock.State.RETREAT) {
                warlockLeaving = true;
                story.data().beats.remove(IN_ARENA);
                story.data().markDirty();
                EntityPlayer p = nearestAlive(w, 96D);
                EntityChampion champion = w.beatenBy() instanceof EntityChampion ? (EntityChampion) w.beatenBy() : null;
                if (champion != null) {
                    // Everyone who was there to see it, alive or waking, knows he lost to the old man.
                    for (EntityPlayer o : w.world.playerEntities) {
                        if (o instanceof EntityPlayerMP && story.data().player(o.getUniqueID()).seen("death_revealed")) {
                            story.data().player(o.getUniqueID()).beats.add("warlock_lost_champion");
                        }
                    }
                    story.data().markDirty();
                }
                if (champion != null && !(p instanceof EntityPlayerMP && p.isEntityAlive())) {
                    StoryDirector.shout(champion, CHAMPION, champion.getEntityId(), "Sit down, boy. Grown-ups are fighting.", 220D);
                } else if (p instanceof EntityPlayerMP && p.isEntityAlive() && !story.talking(p)) {
                    Talk t = new Talk((EntityPlayerMP) p).inAction().at(w, 128D);
                    if (champion != null) {
                        // The Champion put him down, not the player: the old fighter has a word for him, and he has one back.
                        t.line(CHAMPION, champion, "Sit down, boy. Grown-ups are fighting.")
                                .line(WARLOCK, w, "This isn't over. Not for either of you.");
                        story.talk((EntityPlayerMP) p, t);
                    } else if (w.getHealth() <= 1F) {
                        // Beaten, he goes; and says one thing to the player on his way.
                        t.line(WARLOCK, w, Judgement.parting(t, "Ask her how many have to die. She'll tell you. She's proud of it."));
                        story.talk((EntityPlayerMP) p, t);
                    }
                }
            }
        }
        keepInArena();
        if (handTo != null && StoryDirector.now() >= handAt) {
            handOver(server);
        }
        if (heist >= 0) {
            heisting();
        }
        EntityNorse d = Oath.alive(death);
        boolean showing = d != null && "pointing".equals(d.role) && pointing != null && !pointing.isOver();
        if (!pointFor.isEmpty()) {
            for (UUID id : new java.util.ArrayList<>(pointFor.keySet())) {
                EntityPlayerMP p = server.getPlayerList().getPlayerByUUID(id);
                if (p == null || story.data().player(id).seen("dir_station")) {
                    // Gone, or shown it already alongside someone else.
                    pointFor.remove(id);
                } else if (!showing && StoryDirector.now() >= pointFor.get(id) && p.isEntityAlive() && !story.talking(p)) {
                    // One at a time: while she is showing someone, the rest wait their turn.
                    pointFor.remove(id);
                    pointHome(p);
                    break;
                }
            }
        }
        d = Oath.alive(death);
        if (d != null && StoryDirector.now() % 20 == 0 && "pointing".equals(d.role) && d.ticksExisted > 200
                && (pointing == null || pointing.isOver())) {
            d.setDead();
            death = null;
            pointing = null;
        }
        if (d != null && StoryDirector.now() % 20 == 0 && "stands".equals(d.role)) {
            EntityPlayer near = d.world.getClosestPlayerToEntity(d, 128D);
            if (near == null) {
                d.setDead();
                death = null;
            }
        }
    }

    /** Somewhere in what is left of the seats, facing the sand, high enough to see everything. */
    private static Vec3d seat(World world, Vec3d facing) {
        Vec3d c = new Vec3d(Tamarata.ARENA_X + 0.5D, Tamarata.ARENA_Y, Tamarata.ARENA_Z + 0.5D);
        Vec3d away = new Vec3d(c.x - facing.x, 0D, c.z - facing.z);
        away = away.lengthSquared() < 1.0E-4 ? new Vec3d(0D, 0D, 1D) : away.normalize();
        double r = (Tamarata.ARENA_RADIUS + Tamarata.SEATS_RADIUS) / 2D;
        BlockPos top = Barrow.surface(world, (int) Math.floor(c.x + away.x * r), (int) Math.floor(c.z + away.z * r));
        return new Vec3d(top.getX() + 0.5D, top.getY(), top.getZ() + 0.5D);
    }

    private EntityNorse deathAt(World world, Vec3d at, Vec3d look, String role) {
        EntityNorse d = Oath.alive(death);
        if (d == null) {
            // The woman in mourning, to anyone looking; only the old man in the wreck, who has seen more than most, sees her as she is.
            d = death = EntityNorse.of(world, "wreck".equals(role) ? NorseBoxes.DEATH : NorseBoxes.STRANGER, role);
            EntityPlayer near = world.getClosestPlayer(at.x, at.y, at.z, 128D, false);
            if (!"wreck".equals(role) && near instanceof EntityPlayerMP) {
                // Anyone who has seen her change sees her as she was then (only worse).
                Devotion.dress(story, d, (EntityPlayerMP) near);
            }
            d.setLocationAndAngles(at.x, at.y, at.z, 0F, 0F);
            d.face(look);
            world.spawnEntity(d);
        }
        d.role = role;
        d.look(look.add(0D, 1.5D, 0D));
        d.rest(NorsePose.STAND).act(NorsePose.STAND);
        return d;
    }

    // ------------------------------------------------------------------ the call-out

    /**
     * The Champion has called someone out: she is in the stands; and if Warlock is their rival, he
     * comes down. The first time, nobody throws anything until everyone has said their piece, and
     * a bell starts it. True while that is happening, so he keeps his own words to himself.
     */
    boolean duelBegins(EntityChampion champion, EntityPlayerMP player) {
        StoryData data = story.data();
        StoryData.Player p = data.player(player.getUniqueID());
        if (!StoryDirector.enabled() || !p.seen("death_revealed") || story.talking(player)) {
            return false;
        }
        World world = champion.world;
        Vec3d mid = new Vec3d(Tamarata.ARENA_X + 0.5D, Tamarata.ARENA_Y, Tamarata.ARENA_Z + 0.5D);
        EntityNorse d = deathAt(world, seat(world, player.getPositionVector()), mid, "stands");
        boolean rival = p.warlock == StoryData.Stance.RIVAL || p.warlock == StoryData.Stance.BETRAYED;
        boolean first = p.beats.add("arena_called");
        data.markDirty();
        if (!first) {
            rejoin(champion, player);
            return false;
        }
        champion.hold(player);
        EntityAdamWarlock w = null;
        if (rival && warlock == null) {
            // He comes down onto the sand between them, off to one side, and waits to be heard.
            w = warlock = new EntityAdamWarlock(world);
            if (p.warlock == StoryData.Stance.BETRAYED) {
                w.evolve();
            }
            w.script(true);
            w.arriveAt(between(world, champion, player, 0.35D, 4D), player.getPositionEyes(1F));
            world.spawnEntity(w);
            warlockLeaving = false;
            data.see(IN_ARENA);
        }
        EntityAdamWarlock him = w;
        Runnable bell = () -> {
            champion.release();
            world.playSound(null, champion.posX, champion.posY + 2D, champion.posZ, SoundEvents.BLOCK_NOTE_BELL, SoundCategory.HOSTILE, 3F, 0.7F);
            if (him != null && !him.isDead) {
                him.engage(player);
                him.setFoe(champion);
                champion.setFoe(him);
            }
        };
        Talk t = new Talk(player).at(champion, 160D);
        t.line(CHAMPION, champion, "You again.")
                .run(() -> d.act(NorsePose.NOD, 0))
                .line(CHAMPION, champion, "She's been at every fight I've ever had. Sits there. Waits for me to lose.")
                .pause(20)
                .line(CHAMPION, champion, "Billions of years. Hasn't happened yet.");
        if (him != null) {
            t.pause(10)
                    .run(() -> champion.hold(him))
                    .line(WARLOCK, him, "Elder. Whatever they offer you, don't give them that stone. They're carrying it to her.")
                    .line(CHAMPION, champion, "I don't care who it goes to. I care who earns it.")
                    .run(() -> him.direct(EntityAdamWarlock.State.HOVER, between(world, champion, player, 0.7D, 3D), player.getPositionEyes(1F), 0.08D))
                    .direction("Warlock turns to you, and lowers his voice.")
                    .line(WARLOCK, him, "Mother's growing another one. Did you know that? Another me.")
                    .line(WARLOCK, him, "If I go back without anything again, there won't be a reason to keep this one.");
            // And what he thinks of whoever she has her hooks in, to their face.
            Judgement.judged(t, him, Judgement.ARENA)
                    .run(() -> champion.hold(player))
                    .line(CHAMPION, champion, "Two challengers. Both of you, then. Whoever's still standing, we'll talk.");
        } else {
            t.pause(10)
                    .line(CHAMPION, champion, "Hands up. Let's give her something to watch.");
        }
        t.pause(10).run(bell);
        t.abandoned(bell);
        story.talk(player, t);
        return true;
    }

    /**
     * Called out again with Warlock still in it (they fell, or went, and came back): he leaves off
     * the old man for the one he came for, and it is the three of them again.
     */
    private void rejoin(EntityChampion champion, EntityPlayerMP player) {
        EntityAdamWarlock w = warlock;
        if (w == null || w.isDead || warlockLeaving || w.world != champion.world || w.isScripted()) {
            return;
        }
        w.engage(player);
        w.setFoe(champion);
        champion.setFoe(w);
    }

    /**
     * While Warlock is in the fight on Tamarata he is there whenever the old man is: back from
     * wherever he went when nobody was near enough to see them (the world closed, or everyone
     * left), still at it with the Champion, or with whoever is standing in front of him.
     */
    private void keepInArena() {
        StoryData data = story.data();
        if (!data.seen(IN_ARENA) || heist >= 0 || warlockLeaving) {
            return;
        }
        EntityAdamWarlock w = warlock;
        if (w != null && (w.isDead || !w.isAddedToWorld() || net.minecraftforge.common.DimensionManager.getWorld(w.dimension) != w.world)) {
            warlock = w = null;
        }
        ChampionDirector champions = ChampionDirector.INSTANCE;
        EntityChampion c = champions.body();
        if (c == null || c.isDead || !c.isAddedToWorld()) {
            // Gone with the old man; back with him.
            if (w != null && !w.isScripted()) {
                w.setDead();
                warlock = null;
            }
            return;
        }
        ChampionData.Phase phase = champions.data().phase;
        if (w != null || StoryDirector.now() % 20 != 0
                || phase != ChampionData.Phase.RESIDENT && phase != ChampionData.Phase.DUEL && phase != ChampionData.Phase.STRANDED) {
            return;
        }
        World world = c.world;
        EntityPlayer near = null;
        for (EntityPlayer p : world.playerEntities) {
            if (p instanceof EntityPlayerMP && p.isEntityAlive() && !p.isSpectator() && p.getDistance(c) < 96D
                    && (near == null || p.getDistance(c) < near.getDistance(c))) {
                near = p;
            }
        }
        boolean broken = champions.planetBroken();
        if (broken && near == null) {
            return;
        }
        w = warlock = new EntityAdamWarlock(world);
        if (near != null && data.player(near.getUniqueID()).warlock == StoryData.Stance.BETRAYED) {
            w.evolve();
        }
        if (armed()) {
            // Back as he went: with the stone in him.
            w.empower();
        }
        Vec3d f = c.facing();
        Vec3d at = c.getPositionVector().add(f.scale(7D)).add(new Vec3d(-f.z, 0D, f.x).scale(5D)).add(0D, 4D, 0D);
        w.arriveAt(at, c.getPositionEyes(1F));
        world.spawnEntity(w);
        EntityPlayer duel = c.getDuel() == null ? null : world.getPlayerEntityByUUID(c.getDuel());
        if (broken) {
            w.engage(near);
        } else if (duel != null && duel.isEntityAlive()) {
            w.engage(duel);
            w.setFoe(c);
            c.setFoe(w);
        } else {
            w.turnOn(c);
        }
    }

    /** A point {@code along} the way from him to them, {@code aside} to one side of the line, a little above the ground. */
    private static Vec3d between(World world, EntityChampion champion, EntityPlayer player, double along, double aside) {
        Vec3d c = champion.getPositionVector(), to = player.getPositionVector().subtract(c);
        Vec3d flat = new Vec3d(to.x, 0D, to.z);
        flat = flat.lengthSquared() < 1.0E-4 ? new Vec3d(1D, 0D, 0D) : flat.normalize();
        Vec3d at = c.add(to.scale(along)).add(new Vec3d(-flat.z, 0D, flat.x).scale(aside));
        BlockPos top = Barrow.surface(world, (int) Math.floor(at.x), (int) Math.floor(at.z));
        return new Vec3d(at.x, Math.max(top.getY(), Math.min(c.y, player.posY)) + 2.2D, at.z);
    }

    // ------------------------------------------------------------------ the world breaking

    /**
     * Planet Breaker has broken Tamarata. Everything stops for it: he says what he has done, in
     * his own words; and if Warlock is still there, he hangs in the air over the wreck and says
     * what he thinks of it, before he turns back to the player, with nobody left to referee.
     * True if the story has it (so the Champion does not say it in chat as well).
     */
    boolean breaks(EntityChampion champion, List<EntityPlayerMP> near) {
        if (!StoryDirector.enabled() || near.isEmpty()) {
            return false;
        }
        // Told to whoever is nearest and free to hear it; everyone else near is drawn in.
        EntityPlayerMP player = null;
        for (EntityPlayerMP other : near) {
            if (!story.talking(other) && (player == null || other.getDistance(champion) < player.getDistance(champion))) {
                player = other;
            }
        }
        EntityAdamWarlock w = warlock;
        if (w != null && (w.isDead || w.getState() == EntityAdamWarlock.State.RETREAT || w.world != champion.world)) {
            w = null;
        }
        if (player == null) {
            StoryDirector.shout(champion, CHAMPION, champion.getEntityId(), "That wasn't the floor cracking. That was the world.", 160D);
            if (w != null) {
                // No time for words: he takes it, and it is in him.
                take(champion, w);
                arm(w);
            }
            return true;
        }
        EntityPlayerMP them = player;
        EntityAdamWarlock him = w;
        champion.setFoe(null);
        Talk t = new Talk(player).at(champion, 220D).reach(170D);
        if (him != null) {
            him.setFoe(null);
            him.script(true);
            him.direct(EntityAdamWarlock.State.HOVER, over(champion, player, 6D, 5D), champion.getPositionEyes(1F), 0.12D);
            // Cut short, it still happens: the stone is his, it is in him, and he comes for them.
            t.abandoned(() -> {
                take(champion, him);
                arm(him);
                him.puppet(false);
                him.engage(them);
            });
            champions().holdPresence(1200);
        }
        t.direction("Under your feet, Tamarata comes apart.")
                .pause(40)
                .line(CHAMPION, champion, "That wasn't the floor cracking. That was the world.")
                .pause(20)
                .line(CHAMPION, champion, "Ah. I hit it too hard.");
        if (him != null && !takeable(champion)) {
            // Nothing in his brow to take: there is only the one, and somebody else has it.
            t.pause(10)
                    .line(WARLOCK, him, "You broke it. Your own world. Over one fight.")
                    .line(CHAMPION, champion, "It was a good fight.")
                    .direction("He looks down at the old man, adrift in what is left of his world, and at his brow. There's nothing in it.")
                    .line(WARLOCK, him, "Where is it? The stone. Where is it?")
                    .line(CHAMPION, champion, "Not here, boy. Somebody beat you to it.")
                    .direction("He turns his back on the wreck, and on the Champion, and looks at you.")
                    .line(WARLOCK, him, "Then somebody's carrying it. Mother will want to know who.")
                    .pause(10)
                    .run(() -> {
                        EntityPlayerMP lead = t.leadNow();
                        him.puppet(false);
                        him.engage(lead != null && lead.isEntityAlive() ? lead : them);
                    });
        } else if (him != null) {
            t.pause(10)
                    .line(WARLOCK, him, "You broke it. Your own world. Over one fight.")
                    .line(CHAMPION, champion, "It was a good fight.")
                    .pause(20)
                    .line(WARLOCK, him, "Mother would call that waste. She'd call me waste, if she saw me now.")
                    .direction("He looks down at the old man, adrift in what is left of his world, and at the stone still burning in his brow.")
                    .line(WARLOCK, him, "Not if I come home with that.")
                    // Down onto him, face to face, close enough to reach.
                    .run(() -> him.direct(EntityAdamWarlock.State.HOVER, champion.brow().add(champion.facing().scale(3.5D)).add(0D, 0.5D, 0D),
                            champion.brow(), 0.22D))
                    .pause(24)
                    .run(() -> take(champion, him))
                    .line(CHAMPION, champion, "No. No! That's mine. I bled for that!")
                    .line(WARLOCK, him, "You broke a world to keep it, old man. You don't get to keep it.")
                    // It goes into the gem in his brow, and it is in him.
                    .run(() -> flare(him))
                    .pause(EntityAdamWarlock.FLARE_BURST)
                    .run(() -> arm(him))
                    .pause(EntityAdamWarlock.FLARE_TICKS - EntityAdamWarlock.FLARE_BURST)
                    .run(() -> {
                        him.puppet(false);
                        him.direct(EntityAdamWarlock.State.HOVER, over(champion, them, 10D, 4D), them.getPositionEyes(1F), 0.1D);
                    })
                    .line(WARLOCK, him, "So this is what he felt. Every fight, for a billion years.")
                    .line(WARLOCK, him, "No wonder he never lost.")
                    .direction("He turns his back on the wreck, and on the Champion, and looks at you.")
                    .line(WARLOCK, him, "No more referee. Just you and me.")
                    .pause(10)
                    .run(() -> {
                        // At whoever is still in it with him, if the one it began with is gone.
                        EntityPlayerMP lead = t.leadNow();
                        him.engage(lead != null && lead.isEntityAlive() ? lead : them);
                    });
        }
        story.talk(player, t);
        return true;
    }

    /** Somewhere in the air {@code out} from him toward them, {@code up} above him. */
    private static Vec3d over(EntityChampion champion, EntityPlayer player, double out, double up) {
        Vec3d to = player.getPositionVector().subtract(champion.getPositionVector());
        Vec3d flat = new Vec3d(to.x, 0D, to.z);
        flat = flat.lengthSquared() < 1.0E-4 ? new Vec3d(1D, 0D, 0D) : flat.normalize();
        return champion.getPositionVector().add(flat.scale(out)).add(new Vec3d(-flat.z, 0D, flat.x).scale(3D)).add(0D, up, 0D);
    }

    // ------------------------------------------------------------------ the first loss

    /**
     * Adrift in the wreck: the first time someone is near, she is standing in what is left of the
     * stands, and he looks at her a long time. True if the scene is playing, so his own
     * bargaining keeps quiet.
     */
    boolean stranded(EntityChampion champion, List<EntityPlayerMP> near) {
        if (!StoryDirector.enabled() || near.isEmpty()) {
            return false;
        }
        StoryData data = story.data();
        if (fighting()) {
            // Nobody looks at him while that is going on over his head.
            return true;
        }
        if (data.seen("first_loss")) {
            for (EntityPlayerMP other : near) {
                if (story.talking(other)) {
                    return true;
                }
            }
            return false;
        }
        EntityPlayerMP player = null;
        for (EntityPlayerMP other : near) {
            if (other.isEntityAlive() && data.player(other.getUniqueID()).seen("death_revealed") && !story.talking(other)
                    && other.getDistance(champion) <= 48D && (player == null || other.getDistance(champion) < player.getDistance(champion))) {
                player = other;
            }
        }
        if (player == null) {
            return false;
        }
        data.see("first_loss");
        story.mark(player, "first_loss");
        World world = champion.world;
        Vec3d away = champion.getPositionVector().subtract(player.getPositionVector());
        away = away.lengthSquared() < 1.0E-4 ? new Vec3d(1D, 0D, 0D) : new Vec3d(away.x, 0D, away.z).normalize();
        Vec3d at = champion.getPositionVector().add(away.scale(9D)).add(new Vec3d(-away.z, 0D, away.x).scale(4D));
        EntityNorse d = deathAt(world, at, champion.getPositionVector(), "wreck");
        d.setNoGravity(true);
        Talk t = new Talk(player).at(champion, 96D).reach(48D);
        t.pause(40)
                .direction("He has seen her. He looks at her a long time.")
                .pause(30)
                .line(CHAMPION, champion, "Well.")
                .pause(40)
                .line(CHAMPION, champion, "You waited long enough.")
                .run(() -> d.act(NorsePose.NOD, 0))
                .pause(60)
                .run(() -> d.role = "leaving");
        story.talk(player, t);
        return true;
    }

    // ------------------------------------------------------------------ the fall, in the wreck: Warlock takes the stone

    /**
     * Someone has fallen in the fight in the wreck, with Warlock there. He does not finish
     * anything; he takes what he came for. The stone in their pack if they have it already;
     * failing that, the stone in the old man's brow, while he hangs there unable to stop him.
     * Then he goes home with it. (Before the world breaks, he fights the Champion instead:
     * EntityAdamWarlock.target.)
     */
    void fell(EntityPlayerMP player) {
        EntityAdamWarlock w = warlock;
        ChampionDirector champions = ChampionDirector.INSTANCE;
        if (!StoryDirector.enabled() || w == null || w.isDead || w.world != player.world || heist >= 0
                || w.getState() == EntityAdamWarlock.State.RETREAT || w.getDistance(player) > 128D || !champions.planetBroken()) {
            return;
        }
        if (fighting() && !warlockLeaving) {
            // With the stone in him, one of them down is enough: he has won it, and he goes home with it now,
            // before the dimension can fall asleep round him with nobody left in it.
            warlockLeaving = true;
            w.dismiss();
            fled(w);
            return;
        }
        for (EntityPlayer o : w.world.playerEntities) {
            if (o != player && o instanceof EntityPlayerMP && o.isEntityAlive() && !o.isSpectator() && o.getDistance(w) < 64D) {
                // Someone is still on their feet against him: he goes on with them, and takes nothing yet.
                return;
            }
        }
        SovereignData s = SovereignData.get();
        if (s.station != SovereignData.Station.UNSEEN || s.warlockHasStone() || story.data().seen(EMPOWERED)) {
            // With the stone already in him there is nothing more to take.
            return;
        }
        ItemStack stone = InfinityStones.take(player, EnumInfinityStone.POWER);
        EntityChampion champion = champions.body();
        if (!stone.isEmpty()) {
            player.inventoryContainer.detectAndSendChanges();
            heistFrom = null;
            heistAt = player.getPositionVector().add(0D, 2.5D, 0D);
            s.takenFrom = player.getName();
        } else if (champion != null && !champion.isDead && champion.world == w.world && champions.wearsStone()) {
            stone = new ItemStack(anvil.infinity.items.Items.POWER_STONE);
            heistFrom = champion;
            champions.holdPresence(220);
            Vec3d f = champion.facing();
            heistAt = champion.brow().add(f.scale(3.5D)).add(0D, 0.5D, 0D);
            s.takenFrom = "the Champion";
        } else {
            // Nothing on them and nothing in the old man: there is nothing here for him, and he goes.
            warlockLeaving = true;
            story.data().beats.remove(IN_ARENA);
            story.data().markDirty();
            w.dismiss();
            StoryDirector.shout(w, WARLOCK, w.getEntityId(), "A broken world, and nothing in it worth taking.", 220D);
            return;
        }
        heistStone = stone;
        heist = 0;
        warlockLeaving = true;
        story.data().beats.remove(IN_ARENA);
        w.setFoe(null);
        w.script(true);
        w.direct(EntityAdamWarlock.State.HOVER, heistAt, heistFrom != null ? heistFrom.brow() : heistAt.subtract(0D, 3D, 0D), 0.12D);
        s.station = SovereignData.Station.ABANDONED;
        s.markDirty();
        for (EntityPlayer o : w.world.playerEntities) {
            if (o instanceof EntityPlayerMP && story.data().player(o.getUniqueID()).seen("death_revealed")) {
                story.data().player(o.getUniqueID()).beats.add("warlock_took");
            }
        }
        story.data().player(player.getUniqueID()).beats.add("warlock_took");
        story.data().markDirty();
        StoryDirector.shout(w, WARLOCK, w.getEntityId(), heistFrom != null ? "Then it falls to me." : "You carried it this far for me. Thank you.", 220D);
    }

    /** He takes it, says what he has to, and is gone. */
    private void heisting() {
        EntityAdamWarlock w = warlock;
        EntityChampion c = heistFrom;
        heist++;
        if (w == null || w.isDead) {
            // Gone already; the stone goes with him all the same.
            if (c != null) {
                c.setStone(false);
                ChampionDirector.INSTANCE.stoneTakenBy("Adam Warlock");
            }
            keepStone();
            heist = -1;
            heistFrom = null;
            return;
        }
        if (heist == 40) {
            if (c != null && !c.isDead) {
                Vec3d brow = c.brow();
                EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.CHAMPION_STONE, brow.x, brow.y, brow.z, 0, 0, 0, w.getEntityId(), 24),
                        w.dimension);
                c.world.playSound(null, brow.x, brow.y, brow.z, com.matoon.eternity.EternitySounds.OMEN_SHIMMER, SoundCategory.HOSTILE, 3F, 0.5F);
                StoryDirector.shout(c, CHAMPION, c.getEntityId(), "No. No! That's mine. I bled for that!", 220D);
            } else {
                w.world.playSound(null, w.posX, w.posY, w.posZ, com.matoon.eternity.EternitySounds.OMEN_SHIMMER, SoundCategory.HOSTILE, 3F, 0.6F);
            }
        }
        if (heist == 58 && c != null) {
            c.setStone(false);
            ChampionDirector.INSTANCE.stoneTakenBy("Adam Warlock");
        }
        if (heist == 60) {
            keepStone();
        }
        if (heist == 80) {
            StoryDirector.shout(w, WARLOCK, w.getEntityId(), c != null ? "You broke a world to keep it, old man. You don't get to keep it."
                    : "Mother will have something to show for me after all.", 220D);
        }
        if (heist == 150 && c != null && !c.isDead) {
            StoryDirector.shout(c, CHAMPION, c.getEntityId(), "Come back here! Come back, boy! I'm not finished!", 220D);
        }
        if (heist == 170) {
            w.script(false);
            w.dismiss();
            heist = -1;
            heistFrom = null;
        }
    }

    /** The stone is his, the one true Power Stone, and goes home with him. */
    private void keepStone() {
        if (heistStone.isEmpty()) {
            return;
        }
        SovereignData s = SovereignData.get();
        Relics.release(Relics.Kind.POWER_STONE);
        Relics.claim(heistStone, WARLOCK_ID, "Adam Warlock");
        s.stone = heistStone;
        s.markDirty();
        heistStone = ItemStack.EMPTY;
        story.sendStateToAll();
    }

    /** Whoever wakes after he has taken it: she will be standing near, a moment later. */
    void respawned(EntityPlayerMP player) {
        StoryData.Player p = story.data().player(player.getUniqueID());
        if (StoryDirector.enabled() && p.seen("warlock_took") && !p.seen("dir_station")) {
            pointFor.put(player.getUniqueID(), StoryDirector.now() + 60L);
        }
    }

    /**
     * Where he took it: she looks back the way they came, to their own world, and just beside it;
     * cups her hands round something; opens them, empty.
     */
    void pointHome(EntityPlayerMP player) {
        World world = player.world;
        Vec3d look = player.getLookVec();
        Vec3d flat = new Vec3d(look.x, 0D, look.z);
        flat = flat.lengthSquared() < 1.0E-4 ? new Vec3d(1D, 0D, 0D) : flat.normalize();
        Vec3d at = player.getPositionVector().add(flat.scale(4D));
        if (Oath.alive(death) != null) {
            death.setDead();
        }
        death = null;
        EntityNorse d = deathAt(world, at, player.getPositionVector(), "pointing");
        d.setNoGravity(player.dimension == Tamarata.dimension());
        d.watch(player);
        // Up and off to one side, so whoever she is showing sees her face turn to it.
        Vec3d sky = d.getPositionEyes(1F).add(new Vec3d(-flat.z, 0D, flat.x).scale(30D)).add(0D, 26D, 0D);
        Talk t = new Talk(player).at(d, 48D).reach(32D);
        Presence.beguile(t.pause(30), d)
                .line(DEATH, d, "Oh, don't look like that. You'll get up. You always do. I make sure of it.")
                .line(DEATH, d, "But the pretty boy has run off with something of mine.")
                .run(() -> d.watch(null).look(sky).act(NorsePose.SKY))
                .direction("She looks up, back the way you came: past the last blue world, past the red one, to the small blue one that is yours.")
                .pause(40)
                .direction("Not at it. Just beside it, where something in orbit catches the sun like gold.")
                .run(() -> d.look(d.getPositionEyes(1F).add(d.getLookVec().scale(2D))).act(NorsePose.CUP))
                .direction("She brings her hands together round something small, and closes them on it.")
                .pause(30)
                .run(() -> d.act(NorsePose.STAND))
                .direction("Then she opens them. They are empty.")
                .line(DEATH, d, "Gone home to his mother, with my stone in his pocket. Go and get it back for me.")
                .ask(DEATH, d,
                        say("He took it home?", c -> c.run(() -> d.look(sky).act(NorsePose.SKY))
                                .line(DEATH, d, "Home. A gold station, right beside your own little world. They always run home.")),
                        say("Where is home?", c -> c.run(() -> d.look(sky).act(NorsePose.POINT))
                                .line(DEATH, d, "There. Gold, in orbit over your own world. You can't miss it. They made sure of that.")),
                        act("(Say nothing.)", c -> c.line(DEATH, d, "Sulking? It suits you. Now go.")))
                .run(() -> d.watch(player).act(NorsePose.STAND).rest(NorsePose.STAND))
                .run(() -> {
                    EntityPlayerMP lead = t.leadNow();
                    story.mark(lead != null ? lead : player, "dir_station");
                });
        Devotion.after(story, t, d, player);
        pointing = t;
        story.talk(player, t);
        // Whoever is questing with them on this world sees it too.
        story.drawFellows(t, player);
    }

    // ------------------------------------------------------------------ the stone in him

    private static ChampionDirector champions() {
        return ChampionDirector.INSTANCE;
    }

    /** He has it, in this fight, and nobody has yet taken it off him or let him take it home. */
    private boolean armed() {
        SovereignData s = SovereignData.get();
        return story.data().seen(EMPOWERED) && s.warlockHasStone() && s.station == SovereignData.Station.UNSEEN;
    }

    /** He is in the wreck with it in him, and not yet beaten or gone. */
    private boolean fighting() {
        EntityAdamWarlock w = warlock;
        return armed() && w != null && !w.isDead && w.getState() != EntityAdamWarlock.State.RETREAT;
    }

    /**
     * Out of the old man's brow and into Warlock's hand, while he hangs there unable to stop it:
     * the one true Power Stone, his now. It stays with him in the fight; it only goes home with
     * him if he wins (fled), and comes out of him if he loses (disarmed).
     */
    private void take(EntityChampion champion, EntityAdamWarlock w) {
        StoryData data = story.data();
        SovereignData s = SovereignData.get();
        ChampionDirector champions = champions();
        if (!takeable(champion)) {
            return;
        }
        Vec3d brow = champion.brow();
        EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.CHAMPION_STONE, brow.x, brow.y, brow.z, 0, 0, 0, w.getEntityId(), 24),
                w.dimension);
        champion.world.playSound(null, brow.x, brow.y, brow.z, com.matoon.eternity.EternitySounds.OMEN_SHIMMER, SoundCategory.HOSTILE, 3F, 0.5F);
        champions.holdPresence(1200);
        champions.stoneTakenBy("Adam Warlock");
        ItemStack stone = new ItemStack(anvil.infinity.items.Items.POWER_STONE);
        Relics.release(Relics.Kind.POWER_STONE);
        Relics.claim(stone, WARLOCK_ID, "Adam Warlock");
        s.stone = stone;
        s.takenFrom = "the Champion";
        s.markDirty();
        data.see(EMPOWERED);
        story.sendStateToAll();
    }

    /**
     * Whether there is a stone in the old man's brow for him to take: not if there is only the one
     * and someone else already has it, nor if he has already had it.
     */
    private boolean takeable(EntityChampion champion) {
        SovereignData s = SovereignData.get();
        return !story.data().seen(EMPOWERED) && !s.warlockHasStone() && s.station == SovereignData.Station.UNSEEN
                && !champion.isDead && champions().wearsStone();
    }

    /** He holds it to his brow, and it goes in, and he coils round it. */
    private void flare(EntityAdamWarlock w) {
        if (!story.data().seen(EMPOWERED)) {
            return;
        }
        w.puppet(true);
        w.place(w.getPositionVector(), null);
        w.perform(EntityAdamWarlock.State.FLARE);
        Vec3d c = w.chest();
        EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.WARLOCK_CHARGE, c.x, c.y, c.z, 4D, 0D, 0D, w.getEntityId(),
                EntityAdamWarlock.FLARE_BURST), w.dimension);
        w.world.playSound(null, c.x, c.y, c.z, SoundEvents.BLOCK_PORTAL_TRIGGER, SoundCategory.HOSTILE, 2.5F, 0.8F);
        w.world.playSound(null, c.x, c.y, c.z, SoundEvents.EVOCATION_ILLAGER_PREPARE_SUMMON, SoundCategory.HOSTILE, 2F, 0.5F);
    }

    /** It bursts out of him: the stone set in his brow, and everything he is, at once. */
    private void arm(EntityAdamWarlock w) {
        if (!story.data().seen(EMPOWERED) || w.isEmpowered()) {
            return;
        }
        w.empower();
        Vec3d c = w.chest();
        int dim = w.dimension;
        EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.WARLOCK_EMERGE, c.x, c.y, c.z, 14D, 0D, 0D, w.getEntityId(), 50), dim);
        EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.WARLOCK_BOOM, c.x, c.y, c.z, 0D, 1D, 0D, 0, 26), dim);
        Vec3d brow = w.brow();
        EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.MAGUS_IMPACT, brow.x, brow.y, brow.z, 0, 1, 0, -1, 1), dim);
        w.world.playSound(null, c.x, c.y, c.z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 4F, 0.6F);
        w.world.playSound(null, c.x, c.y, c.z, com.matoon.eternity.EternitySounds.COMET_BLAST, SoundCategory.HOSTILE, 4F, 0.9F);
        w.world.playSound(null, c.x, c.y, c.z, SoundEvents.ENTITY_ENDERDRAGON_GROWL, SoundCategory.HOSTILE, 2F, 1.4F);
        w.world.playSound(null, c.x, c.y, c.z, SoundEvents.ENTITY_ZOMBIE_VILLAGER_CONVERTED, SoundCategory.HOSTILE, 2F, 0.5F);
        // The air goes out from him all at once.
        for (EntityPlayer p : w.world.playerEntities) {
            if (!p.isSpectator() && p.getDistance(w) < 14D) {
                Vec3d away = new Vec3d(p.posX - w.posX, 0D, p.posZ - w.posZ);
                away = away.lengthSquared() < 1.0E-4 ? new Vec3d(1D, 0D, 0D) : away.normalize();
                p.motionX += away.x * 1.2D;
                p.motionY = Math.max(p.motionY, 0D) + 0.4D;
                p.motionZ += away.z * 1.2D;
                p.velocityChanged = true;
            }
        }
    }

    /**
     * Beaten, with it in him: it tears out of his brow and goes to whoever put him down, and he
     * goes without it. The Champion's chapter goes on; the Sovereign never gets it.
     */
    private void disarmed(EntityAdamWarlock w) {
        // Whoever landed the last blow on him, if they are still standing; failing that, whoever is nearest.
        EntityPlayer to = null;
        for (EntityPlayer p : w.world.playerEntities) {
            if (p instanceof EntityPlayerMP && p.isEntityAlive() && !p.isSpectator() && p.getDistance(w) < 96D && p.getLastAttackedEntity() == w
                    && (to == null || p.getLastAttackedEntityTime() > to.getLastAttackedEntityTime())) {
                to = p;
            }
        }
        if (to == null) {
            to = nearestAlive(w, 96D);
        }
        if (to == null) {
            fled(w);
            return;
        }
        StoryData data = story.data();
        SovereignData s = SovereignData.get();
        w.wearStone(false);
        handFrom = w.brow();
        EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.CHAMPION_STONE, handFrom.x, handFrom.y, handFrom.z, 0, 0, 0,
                to.getEntityId(), 24), w.dimension);
        w.world.playSound(null, handFrom.x, handFrom.y, handFrom.z, com.matoon.eternity.EternitySounds.OMEN_SHIMMER, SoundCategory.HOSTILE, 3F, 0.7F);
        w.world.playSound(null, handFrom.x, handFrom.y, handFrom.z, SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 2F, 0.5F);
        handTo = to.getUniqueID();
        handStone = s.stone;
        handAt = StoryDirector.now() + 22L;
        s.stone = ItemStack.EMPTY;
        s.takenFrom = "";
        s.markDirty();
        data.beats.remove(EMPOWERED);
        data.beats.remove(IN_ARENA);
        data.see(DISARMED);
        if (!story.talking(to)) {
            Talk t = new Talk((EntityPlayerMP) to).inAction().at(w, 128D);
            t.line(WARLOCK, w, "No. No, that's mine, I took it, it's...")
                    .line(WARLOCK, w, "Half of everything. Ask her how many that is. Ask her.");
            story.talk((EntityPlayerMP) to, t);
        }
    }

    /** The stone, out of his brow and into the hand of whoever beat him: theirs now, the one true Power Stone. */
    private void handOver(MinecraftServer server) {
        EntityPlayerMP p = server.getPlayerList().getPlayerByUUID(handTo);
        ItemStack stone = handStone;
        handTo = null;
        handStone = ItemStack.EMPTY;
        if (stone.isEmpty()) {
            return;
        }
        Relics.release(Relics.Kind.POWER_STONE);
        if (p == null) {
            // Gone before it reached them: it falls where he was.
            World world = net.minecraftforge.common.DimensionManager.getWorld(Tamarata.dimension());
            if (world != null) {
                world.spawnEntity(new net.minecraft.entity.item.EntityItem(world, handFrom.x, handFrom.y, handFrom.z, stone));
            }
            story.sendStateToAll();
            return;
        }
        Relics.claim(stone, p);
        ChampionData c = champions().data();
        c.stoneHolder = p.getName();
        c.markDirty();
        net.minecraftforge.items.ItemHandlerHelper.giveItemToPlayer(p, stone);
        p.inventoryContainer.detectAndSendChanges();
        p.world.playSound(null, p.posX, p.posY, p.posZ, com.matoon.eternity.EternitySounds.OMEN_SHIMMER, SoundCategory.PLAYERS, 1.5F, 0.6F);
        net.minecraft.util.text.TextComponentString notice = new net.minecraft.util.text.TextComponentString(p.getName() + " now holds the Power Stone.");
        notice.getStyle().setColor(net.minecraft.util.text.TextFormatting.LIGHT_PURPLE);
        server.getPlayerList().sendMessage(notice);
        story.sendStateToAll();
    }

    /**
     * He won it: everyone who stood against him has fallen or gone, and he goes home with it.
     * From here it is the Sovereign's road.
     */
    private void fled(EntityAdamWarlock w) {
        StoryData data = story.data();
        SovereignData s = SovereignData.get();
        data.beats.remove(EMPOWERED);
        data.beats.remove(IN_ARENA);
        s.station = SovereignData.Station.ABANDONED;
        s.markDirty();
        for (EntityPlayer o : w.world.playerEntities) {
            if (o instanceof EntityPlayerMP && data.player(o.getUniqueID()).seen("death_revealed")) {
                StoryData.Player p = data.player(o.getUniqueID());
                p.beats.add("warlock_took");
                if (o.isEntityAlive() && !p.seen("dir_station")) {
                    // Still on their feet: she comes to them once he is gone. The fallen see her when they wake.
                    pointFor.put(o.getUniqueID(), StoryDirector.now() + 120L);
                }
            }
        }
        data.markDirty();
        StoryDirector.shout(w, WARLOCK, w.getEntityId(), "Mother will have something to show for me after all.", 220D);
        story.sendStateToAll();
    }

    // ------------------------------------------------------------------ the debt

    /** The stone is given: he pays what he owes. True while he is saying so, and the rocket waits. */
    boolean debt(EntityChampion champion, EntityPlayerMP player) {
        StoryData data = story.data();
        if (!StoryDirector.enabled() || !data.player(player.getUniqueID()).beats.add("champion_debt")) {
            return story.talking(player);
        }
        data.markDirty();
        Talk t = new Talk(player).at(champion, 96D);
        if ("Adam Warlock".equals(ChampionDirector.INSTANCE.data().stoneHolder)) {
            // Robbed, and brought home anyway. He has nothing to give but what he knows.
            story.mark(player, "champion_helped");
            t.line(CHAMPION, champion, "Nothing left to give you for it. You came anyway.")
                    .line(CHAMPION, champion, "The boy who took it. Gold, and talks about his mother like the young talk about gods.")
                    .line(CHAMPION, champion, data.seen(DISARMED) ? "And you took it off him, with it burning in him. Hah. I'd have paid to see that from the stands."
                            : "Wherever she is, that's where he ran. Hit him low. He never guards low.")
                    .pause(20)
                    .line(CHAMPION, champion, "And this. A ticket to my ring. There's no ring now, but hold it up when you need a fist, and I'll come.")
                    .pause(10);
        }
        t.line(CHAMPION, champion, "I owe you. I pay what I owe.")
                .line(CHAMPION, champion, "There's a thing in a glass jar, where all the worlds touch.")
                .line(CHAMPION, champion, "It's had a stone on its head longer than I've had mine. Order and Chaos put it there.")
                .pause(20)
                .direction("He looks past you, at where she stood.")
                .line(CHAMPION, champion, "And her. She doesn't sit in the stands for people she likes.")
                .line(CHAMPION, champion, "She sits there for people who are going to lose.");
        story.talk(player, t);
        return true;
    }

    boolean interact(EntityPlayerMP player, EntityNorse npc) {
        if (npc != death) {
            return false;
        }
        StoryDirector.line(player, npc, DIRECTION, -1, "She doesn't take her eyes off the sand.");
        return true;
    }
}
