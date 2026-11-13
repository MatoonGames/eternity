package com.matoon.eternity.story;

import com.matoon.eternity.EternitySounds;
import com.matoon.eternity.client.render.NorseBoxes;
import com.matoon.eternity.compat.InfinityStones;
import com.matoon.eternity.entity.EntityAdamWarlock;
import com.matoon.eternity.entity.EntityEternity;
import com.matoon.eternity.entity.EntityNorse;
import com.matoon.eternity.entity.NorsePose;
import com.matoon.eternity.world.Director;
import com.matoon.eternity.world.EternityData;
import com.matoon.eternity.world.OdinsVault;
import com.matoon.eternity.world.SovereignData;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.matoon.eternity.story.Speakers.DEATH;
import static com.matoon.eternity.story.Speakers.WARLOCK;
import static com.matoon.eternity.story.Talk.act;
import static com.matoon.eternity.story.Talk.say;

/**
 * Six stones on one hand, the Magus done with or never come, and Eternity on his way. Warlock comes
 * down out of the sky to tell them to break the stones; Death is there before he has finished, to
 * tell them to close their hand on half of everything alive. They fight over it in front of them,
 * and she is so close to what she wants that anything but listening sets her off. Eternity is held
 * off while it is put to them, and a little while after: that is all the time there is.
 * <p>
 * Broken, in time: she comes apart, and when Eternity comes, she wishes them luck and goes, and
 * Warlock stands with them against him (and can die doing it). The cull, in time: she has what she
 * wanted and is gone for good, and Warlock fights them a last time, to the death; if Eternity comes
 * first, he tells them he told them so, and leaves. Nothing, in time: she leaves them to it, and
 * Warlock stays. With Eternity dead, Warlock at their side says goodbye if he is caged, and is
 * simply gone if he is not.
 * <p>
 * What is said and decided is kept in world beats; where everyone is in it is not, so a server
 * stopped in the middle of it lets Eternity come as he would have anyway.
 */
final class Reckoning {

    /** World beats: it has been put to them; which way they closed their hand; who is gone for good. */
    static final String BEGUN = "reckoning", BROKE = "stones_broken", CULLED = "the_cull", DEATH_GONE = "death_gone", WARLOCK_GONE = "warlock_gone";
    /** How long they have once it has been put to them; how long after the cull before he comes anyway; how long he takes, once called. */
    private static final int WINDOW = 20 * 90, AFTER_CULL = 20 * 60 * 4, COMING = 20 * 25;

    private enum Stage {
        /** Nobody has six yet (or it has all been played out). */
        NONE,
        /** It is being put to them: Eternity holds off. */
        PUT,
        /** Put, and the clock running: Eternity holds off a little longer. */
        WAITING,
        /** They broke them: she is coming apart, and Eternity waits for that. */
        BROKEN,
        /** They culled: Warlock is fighting them, and Eternity holds off for a while yet. */
        CULLED,
        /** Eternity is coming, or here. */
        COMING,
        /** All over. */
        DONE
    }

    /** What she says while they stand there not deciding, in order: less patient each time. */
    private static final String[] HER_NAGS = {"Well? I'm waiting. Look how good I'm being about waiting.",
            "Why are you looking at your hand like that? It isn't a hard question. It isn't even a question.",
            "Are you HESITATING? With him on his way? With ME standing right here?",
            "Close it. Close it. Close it, close it, close it, CLOSE IT.",
            "I can feel him now. The stars are going out behind him, one at a time. Stop THINKING and DO IT."};
    /** And him: as if it were obvious, which it is. */
    private static final String[] HIS_NAGS = {"What are you waiting for? This isn't a hard one.",
            "She wants half of everything dead. I want nobody to have them. Which of those sounds right to you?",
            "There's a god coming to take your arm off, and a woman who wants a massacre. This is not a difficult choice.",
            "I'm not going to explain it again. Break them.",
            "Any time now would be good. Any time at all."};

    /** Something said a little later, so that it isn't said over the top of what came before. */
    private static final class Cue {
        final long at;
        final Runnable run;

        Cue(long at, Runnable run) {
            this.at = at;
            this.run = run;
        }
    }

    private final List<Cue> cues = new ArrayList<>();

    private final StoryDirector story;
    private Stage stage = Stage.NONE;
    @Nullable
    private UUID lead;
    /** When Eternity stops waiting (for the stages that hold him off by the clock). */
    private long until = -1;
    private long nagAt = -1;
    private int nags;
    @Nullable
    private EntityNorse death;
    @Nullable
    private EntityAdamWarlock warlock;
    @Nullable
    private Talk scene;
    /** Warlock is fighting them (after the cull); standing with them (against Eternity). */
    private boolean duel, beside;
    /** Whether Warlock is at Eternity right now, rather than waiting at their shoulder. */
    private boolean engaged;
    /** Whether Warlock has been at Eternity since he last came. */
    private boolean fought;
    /** The time ran out with nothing decided: Eternity is on his way, and when he is here, they both go. */
    private boolean undecided;
    /** When Warlock says goodbye, after Eternity is caged. */
    private long farewellAt = -1;
    /** Someone has six and it has not been put to them yet (they are busy): Eternity waits for it. */
    private boolean pending;

    Reckoning(StoryDirector story) {
        this.story = story;
    }

    void reset() {
        stage = Stage.NONE;
        lead = null;
        until = -1;
        death = null;
        warlock = null;
        scene = null;
        duel = beside = engaged = fought = pending = undecided = false;
        cues.clear();
        farewellAt = -1;
    }

    /** Whether Eternity is held off: until it is put to them, while it is, and for the little time they are given after. */
    boolean holds() {
        switch (stage) {
            case NONE:
                return pending;
            case PUT:
                return true;
            case BROKEN:
                return scene != null && !scene.isOver();
            case WAITING:
            case CULLED:
                return StoryDirector.now() < until;
            default:
                return false;
        }
    }

    /** Whether that is her, here for this (she is not to be caged in the middle of it). */
    boolean has(net.minecraft.entity.Entity e) {
        return e != null && e == death;
    }

    /** Whether it is being played out right now (from Warlock coming down to Eternity getting there). */
    boolean busy() {
        return stage != Stage.NONE && stage != Stage.DONE;
    }

    static boolean broken() {
        return StoryDirector.INSTANCE.data().seen(BROKE);
    }

    /** Warlock is still about: not dead for good, and not gone for good. */
    static boolean warlockAlive() {
        return !SovereignData.get().warlockFell && !StoryDirector.INSTANCE.data().seen(WARLOCK_GONE);
    }

    // ------------------------------------------------------------------ ticking

    void tick(MinecraftServer server) {
        if (!StoryDirector.enabled()) {
            return;
        }
        long now = StoryDirector.now();
        if (!cues.isEmpty()) {
            for (Cue c : new ArrayList<>(cues)) {
                if (now >= c.at) {
                    cues.remove(c);
                    c.run.run();
                }
            }
        }
        if (stage == Stage.NONE) {
            if (now % 20 == 0) {
                pending = false;
                if (!story.data().seen(BEGUN) && settled()) {
                    for (EntityPlayerMP p : server.getPlayerList().getPlayers()) {
                        if (InfinityStones.scan(p) != InfinityStones.ALL || p.dimension == OdinsVault.dimension()) {
                            continue;
                        }
                        pending = true;
                        if (ready(p)) {
                            pending = false;
                            begin(p);
                            break;
                        }
                    }
                }
            }
            return;
        }
        EntityPlayerMP p = StoryDirector.player(lead);
        tickWarlock(p, now);
        switch (stage) {
            case PUT:
                if (scene == null || scene.isOver()) {
                    // Cut short some other way than walking off: the clock starts anyway.
                    waiting(p);
                }
                break;
            case WAITING:
                nag(now);
                if (now >= until) {
                    tooLate(p);
                }
                break;
            case BROKEN:
                if (!holds()) {
                    coming(p);
                }
                break;
            case CULLED:
                if (now >= until) {
                    coming(p);
                }
                break;
            case COMING:
                if (Director.INSTANCE.arrived()) {
                    arrived();
                }
                break;
            default:
                break;
        }
        if (farewellAt >= 0 && now >= farewellAt) {
            farewellAt = -1;
            farewell(true);
        }
    }

    /** The Magus's last fight is behind them (won, and a minute gone), or was never begun; and Eternity has not come yet. */
    private boolean settled() {
        SovereignData s = SovereignData.get();
        boolean magus = s.fight == SovereignData.Fight.NONE || s.fight == SovereignData.Fight.WON && StoryDirector.now() - s.fightSettled >= 20L * 60L;
        return magus && Director.INSTANCE.data().phase == EternityData.Phase.ROAMING;
    }

    private boolean ready(EntityPlayerMP p) {
        return p.isEntityAlive() && !p.isSpectator() && !story.talking(p) && p.dimension != OdinsVault.dimension();
    }

    // ------------------------------------------------------------------ it is put to them

    private void begin(EntityPlayerMP player) {
        StoryData data = story.data();
        data.beats.add(BEGUN);
        data.markDirty();
        boolean her = !Betrayal.absent();
        boolean him = warlockAlive();
        if (!her && !him) {
            // Nobody left to tell them anything: he just comes.
            stage = Stage.DONE;
            return;
        }
        lead = player.getUniqueID();
        stage = Stage.PUT;
        WorldServer world = player.getServerWorld();
        Vec3d look = flat(player.getLookVec());
        Vec3d side = new Vec3d(-look.z, 0D, look.x);
        EntityAdamWarlock w = null;
        if (him) {
            w = warlock = new EntityAdamWarlock(world);
            Vec3d at = ground(world, player.getPositionVector().add(look.scale(5D)).add(side.scale(2.5D))).add(0D, 1.2D, 0D);
            w.evolve();
            w.script(true);
            w.arriveAt(at, player.getPositionEyes(1F));
            world.spawnEntity(w);
        }
        // She is there from the start, a few steps behind them, where they would have to turn round to see her.
        EntityNorse d = her ? death = appear(player, ground(world, player.getPositionVector().subtract(look.scale(4D)).subtract(side.scale(1.5D)))) : null;
        Talk t = scene = new Talk(player).reach(32D);
        t.run(() -> story.mark(player, BEGUN));
        if (w != null) {
            t.direction("Something gold comes down out of the sky like a thrown spear, and stops dead in the air in front of you.")
                    .line(WARLOCK, w, "Six. You actually did it. Part of me hoped you'd never manage it.")
                    .line(WARLOCK, w, "Listen to me, and listen fast. He's coming. Eternity. He can feel them on you, the way you'd feel a stone in your shoe.")
                    .line(WARLOCK, w, "There's one thing you can do with them that nobody can ever undo. Not the Magus, not anyone. Break them. Close your hand and break them.");
        }
        if (d != null) {
            if (w != null) {
                argue(t, w, d);
            } else {
                t.direction("The air goes cold. Someone behind you lets out a long, delighted breath.")
                        .line(DEATH, d, "Six. Six! Oh, look at you. Look at your hand.")
                        .line(DEATH, d, "Do you know how long I've waited for someone to stand where you're standing? Since before the stars. Since before the dark.")
                        .line(DEATH, d, "He's coming, you know. Eternity. Minutes, not days. He'll take them off you along with the arm.");
            }
            t.line(DEATH, d, "Half. Close your hand on half of everything alive, and he can't touch you. There'll be nothing left here worth his coming for. I'll see to it.");
            pester(t, d, w, 0, new boolean[QUESTIONS]);
            if (w != null) {
                t.line(WARLOCK, w, "Break them. Please. I've never said please to anyone in my life.");
            }
            t.line(DEATH, d, "Kill half. For me. For us. And I'll hold every one of them so gently, I promise. I'll hold you last.");
        } else {
            t.line(WARLOCK, w, "She isn't here. I don't know where she's got to, and I don't care. It's just you and me, and him on his way.")
                    .line(WARLOCK, w, "I was made to hold them. I know better than anyone what they do to whoever does. Break them.");
        }
        t.direction("(The Mind Stone sets what the snap will do. Destroy the Stones, or Kill half: snap before Eternity gets here.)")
                .direction("Somewhere a very long way off, something enormous turns its head toward you.");
        t.abandoned(() -> waiting(player));
        t.run(() -> waiting(player));
        story.talk(player, t);
        story.drawFellows(t, player);
    }

    /** Warlock and Death at each other over their head. */
    private void argue(Talk t, EntityAdamWarlock w, EntityNorse d) {
        t.direction("The air goes cold. Someone behind you laughs, soft and delighted, and Warlock's face goes hard.")
                .line(DEATH, d, "Break them. Listen to him. Grown in a vat, ten thousand years of thinking time, and his big idea is to throw the present away unopened.")
                .line(WARLOCK, w, "Get away from them.")
                .line(DEATH, d, "Or what, little gold boy? You'll hit me? I've held you. Do you remember? I do. You were so light.")
                .line(WARLOCK, w, "Don't listen to her. She wants half of everything alive dead. Not some. Half. Every dog. Every child. Every bird on every branch.")
                .line(DEATH, d, "And they'd come to me! Gently! All at once, so nobody's left behind crying at anybody's grave! It's the kindest thing anyone has ever done!")
                .line(WARLOCK, w, "Call it what it is. Murder. The biggest murder there's ever been, and you want them to do it for you.")
                .run(() -> pose(NorsePose.FURY))
                .line(DEATH, d, "DON'T YOU CALL IT THAT. IT'S A MERCY. IT'S A GIFT. IT'S MY GIFT.")
                .run(() -> pose(NorsePose.STAND))
                .line(DEATH, d, "Sorry. Sorry. He makes me so... Sweetheart, look at me. Not at him. At me.")
                .line(WARLOCK, w, "Yes. Look at her. Properly, once. Look at what she is.")
                .line(DEATH, d, "He's jealous. He's been jealous since the Garden. He wanted to be the one holding them. Didn't you, Adam? Didn't you?")
                .line(WARLOCK, w, "I was made to hold them. That's how I know what they do to whoever does. That's why I'm telling them to break them.")
                .line(DEATH, d, "He's coming, you know. Eternity. Not in a year. Not in a day. Minutes. He'll take them off you along with your arm, and then neither of us gets anything.")
                .line(WARLOCK, w, "For once she's telling the truth. He's coming. Whatever you're going to do, do it soon.");
    }

    private static final int QUESTIONS = 4;

    /** What she says to anything but listening: everything at once, all of it at the top of her voice. Three questions is all she'll take. */
    private void pester(Talk t, EntityNorse d, @Nullable EntityAdamWarlock w, int asked, boolean[] used) {
        if (asked >= 3) {
            t.run(() -> pose(NorsePose.FURY))
                    .line(DEATH, d, "NO MORE QUESTIONS. No more. Close your hand. Close it. CLOSE IT.")
                    .run(() -> pose(NorsePose.STAND))
                    .line(DEATH, d, "...Please?");
            return;
        }
        List<Talk.Choice> choices = new ArrayList<>();
        choices.add(act("(Listen.)", c -> c.run(() -> pose(NorsePose.CUP))
                .line(DEATH, d, asked == 0 ? "Good. Good. You listen. You're the only one who ever just listens."
                        : "There. See? That's all I wanted. That's all I ever want. Just you, listening.")
                .direction("She looks at you the way you'd look at something you were afraid might come apart in your hands.")));
        if (!used[0]) {
            choices.add(say("Why half?", c -> {
                used[0] = true;
                c.run(() -> pose(NorsePose.FURY))
                        .line(DEATH, d, "Why HALF? Why do you need a REASON? Did I ask you for reasons? Every time you came back, did I ask you WHY?")
                        .run(() -> pose(NorsePose.PLEAD))
                        .line(DEATH, d, "Because I asked. Isn't that enough? It used to be enough. You used to think I was enough.");
                pester(c, d, w, asked + 1, used);
            }));
        }
        if (!used[1]) {
            choices.add(say("What happens to you, after?", c -> {
                used[1] = true;
                c.run(() -> pose(NorsePose.PLEAD))
                        .line(DEATH, d, "You... you care? Nobody has ever asked me that. Not once. Not in all the time there's been.")
                        .direction("Her eyes fill. Then, between one breath and the next, they're dry again, and narrow.")
                        .line(DEATH, d, "Why are you asking? Who told you to ask that? Was it him? It was him. He's in your head. GET OUT OF THEIR HEAD.");
                if (w != null) {
                    c.line(WARLOCK, w, "I haven't said a word.");
                }
                pester(c, d, w, asked + 1, used);
            }));
        }
        if (!used[2]) {
            choices.add(say(w != null ? "Why should I trust you over him?" : "Why should I trust you?", c -> {
                used[2] = true;
                c.run(() -> pose(NorsePose.FURY))
                        .line(DEATH, d, "TRUST? I held you every single time you died. Every time. Who else did that? Name one. NAME ONE.")
                        .line(DEATH, d, w != null ? "He's tried to kill you. Did you count? I counted. I count everything about you."
                                : "Nobody else came. Nobody. Just me, every time, holding you in the dark.");
                if (w != null) {
                    c.line(WARLOCK, w, "And told you to your face every time I was going to. She's never told you anything to your face in her life.");
                }
                pester(c, d, w, asked + 1, used);
            }));
        }
        if (!used[3]) {
            choices.add(say("What if I say no?", c -> {
                used[3] = true;
                c.run(() -> pose(NorsePose.CLAW))
                        .line(DEATH, d, "Then I'll... no. No, I won't say it. I'm not going to say it.")
                        .direction("She laughs, high and bright, and doesn't stop for a beat too long.")
                        .line(DEATH, d, "You won't say no. You love me. You never said it, but you SAID it. I heard. I hear everything.");
                pester(c, d, w, asked + 1, used);
            }));
        }
        t.ask(DEATH, d, 20 * 20, 0, choices.toArray(new Talk.Choice[0]));
    }

    /** Put to them: the clock is running, and they are standing there with it on their hand. */
    private void waiting(EntityPlayerMP player) {
        if (stage != Stage.PUT) {
            return;
        }
        scene = null;
        stage = Stage.WAITING;
        until = StoryDirector.now() + WINDOW;
        nagAt = StoryDirector.now() + 20 * 10;
        nags = 0;
        EntityPlayerMP p = StoryDirector.player(lead);
        if (p != null) {
            Director.INSTANCE.summon(p, WINDOW + COMING);
        }
    }

    /** Every few seconds they stand there not deciding: her losing patience, him wondering what there is to think about. */
    private void nag(long now) {
        if (now < nagAt) {
            return;
        }
        nagAt = now + 20 * 10;
        EntityNorse d = Oath.alive(death);
        EntityAdamWarlock w = alive(warlock);
        boolean both = d != null && w != null;
        boolean hers = d != null && (w == null || nags % 2 == 0);
        int i = Math.min(both ? nags / 2 : nags, HER_NAGS.length - 1);
        nags++;
        if (hers) {
            if (i >= 2) {
                d.act(NorsePose.FURY);
            }
            StoryDirector.shout(d, DEATH, d.getEntityId(), HER_NAGS[i], 40D);
        } else if (w != null) {
            StoryDirector.shout(w, WARLOCK, w.getEntityId(), HIS_NAGS[Math.min(i, HIS_NAGS.length - 1)], 40D);
        }
    }

    /** The time is up, and still nothing: he is called in, and they make one last try before he gets here. */
    private void tooLate(@Nullable EntityPlayerMP p) {
        undecided = true;
        EntityNorse d = Oath.alive(death);
        EntityAdamWarlock w = alive(warlock);
        if (d != null) {
            d.act(NorsePose.FURY);
            StoryDirector.shout(d, DEATH, d.getEntityId(), "That's him. Do you feel that? That's HIM. Do it NOW.", 48D);
        }
        if (w != null) {
            cue(d != null ? 60 : 0, () -> {
                EntityAdamWarlock still = alive(warlock);
                if (still != null) {
                    StoryDirector.shout(still, WARLOCK, still.getEntityId(), "Last chance. Break them, or he takes them, and your arm with them.", 48D);
                }
            });
        }
        coming(p);
    }

    /** Something said, or done, a little after now. */
    private void cue(int delay, Runnable run) {
        cues.add(new Cue(StoryDirector.now() + delay, run));
    }

    /** Eternity is called in: he will be here soon. */
    private void coming(@Nullable EntityPlayerMP p) {
        stage = Stage.COMING;
        scene = null;
        if (p != null) {
            Director.INSTANCE.summon(p, COMING);
        }
    }

    /** He's here. Whatever is left of her goes; Warlock does whatever he is going to. */
    private void arrived() {
        stage = Stage.DONE;
        EntityNorse d = Oath.alive(death);
        EntityAdamWarlock w = alive(warlock);
        if (undecided) {
            // He came, and they still hadn't: neither of them is staying to see what comes of that.
            undecided = false;
            beside = false;
            if (d != null) {
                StoryDirector.shout(d, DEATH, d.getEntityId(), "You WAITED. I put everything in your hand and you stood there and WAITED. Fine. Have him.", 64D);
                Obsession.vanish(d);
            }
            goneForGood(DEATH_GONE);
            if (w != null) {
                EntityAdamWarlock gone = w;
                warlock = null;
                cue(d != null ? 70 : 0, () -> {
                    if (!gone.isDead) {
                        StoryDirector.shout(gone, WARLOCK, gone.getEntityId(), "I can't help someone who won't help themselves. You're on your own.", 96D);
                        gone.dismiss();
                    }
                });
                goneForGood(WARLOCK_GONE);
            }
            return;
        }
        if (d != null) {
            d.act(NorsePose.STAND);
            StoryDirector.shout(d, DEATH, d.getEntityId(), "Ha. Haha. Here he comes.", 64D);
            EntityNorse going = d;
            cue(50, () -> {
                if (!going.isDead) {
                    StoryDirector.shout(going, DEATH, going.getEntityId(), "Good fucking luck.", 64D);
                    Obsession.vanish(going);
                }
            });
            goneForGood(DEATH_GONE);
        }
        if (duel && w != null && !w.fallen()) {
            // Still at it when he comes: Warlock has nothing left to fight them for.
            duel = false;
            w.script(true);
            w.direct(EntityAdamWarlock.State.HOVER, w.getPositionVector().add(0D, 4D, 0D), Director.INSTANCE.body().chest(), 0.2D);
            StoryDirector.shout(w, WARLOCK, w.getEntityId(), "There he is. I told you. Right at the beginning, I told you what was coming.", 96D);
            EntityAdamWarlock gone = w;
            cue(70, () -> {
                if (!gone.isDead) {
                    StoryDirector.shout(gone, WARLOCK, gone.getEntityId(), "Enjoy what's left of it. I'm not staying to watch.", 96D);
                    gone.dismiss();
                }
            });
            goneForGood(WARLOCK_GONE);
            warlock = null;
        }
    }

    // ------------------------------------------------------------------ the snap

    /** They snapped: if it was while it was put to them, and before he came, it is answered. */
    void snapped(EntityLivingBase entity, boolean broke, boolean culled) {
        boolean open = stage == Stage.PUT || stage == Stage.WAITING || stage == Stage.COMING && undecided;
        if (!(entity instanceof EntityPlayerMP) || !open || Director.INSTANCE.arrived() || !broke && !culled) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) entity;
        undecided = false;
        if (scene != null) {
            story.cut(scene);
            scene = null;
        }
        story.cut(player);
        lead = player.getUniqueID();
        // Neither of them is half of anything: the dust passes over them.
        EntityNorse d = Oath.alive(death);
        if (d != null) {
            d.removePotionEffect(anvil.infinity.registry.Effects.snapEffect);
        }
        EntityAdamWarlock w = alive(warlock);
        if (w != null) {
            w.removePotionEffect(anvil.infinity.registry.Effects.snapEffect);
        }
        StoryData data = story.data();
        data.beats.add(broke ? BROKE : CULLED);
        data.markDirty();
        if (broke) {
            broke(player);
        } else {
            culled(player);
        }
        story.sendStateToAll();
    }

    /** The stones go out, all six; and she comes apart. */
    private void broke(EntityPlayerMP player) {
        stage = Stage.BROKEN;
        EntityAdamWarlock w = alive(warlock);
        beside = warlockAlive();
        EntityNorse d = Oath.alive(death);
        if (d == null && !Betrayal.absent()) {
            Vec3d look = flat(player.getLookVec());
            d = death = appear(player, ground(player.world, player.getPositionVector().add(look.scale(4D))));
        }
        Talk t = scene = new Talk(player).reach(32D);
        t.direction("The stones go out one after another, like candles. There's a sound like a choir hitting the wrong note, and then nothing at all.");
        if (d != null) {
            boolean shown = d.look() == NorseBoxes.UNVEILED;
            t.run(() -> pose(NorsePose.RECOIL))
                    .line(DEATH, d, "No.")
                    .line(DEATH, d, "No, no, no, no. Put them back. PUT THEM BACK.")
                    .run(() -> pose(NorsePose.KNEEL))
                    .direction("She drops to her knees in the grey dust of them, and scoops at it with both hands, as if she could cup it back together.")
                    .line(DEATH, d, "I was so close. I was SO close. I could taste it. Half of everything, all at once, all mine, and you...")
                    .line(DEATH, d, shown ? "I took my FACE off for you. With my own hands. Do you have any idea how much WORK you were?"
                            : "I wore a whole shape for you. I wore it until it rotted. Do you have any idea how much WORK you were?")
                    .direction("She stops scooping. She goes very still. When she looks up there's nothing left in her face at all: no sweetness, no hurt. Nothing.")
                    .line(DEATH, d, "You know the funny part? None of it was real. The hand. The whispers. Sweetheart. Darling. Mine.")
                    .line(DEATH, d, "I'd have said anything. I'd have BEEN anything. You were a hand with six stones on it, and I wanted the hand to close.");
            if (w != null) {
                t.line(WARLOCK, w, "It's done. They're gone. Nobody can use them now. Not her. Not me. Not anyone, ever.")
                        .run(() -> pose(NorsePose.FURY))
                        .line(DEATH, d, "You. YOU. You put it in their head with your little gold mouth.")
                        .line(WARLOCK, w, "I did. And I'd do it again.");
            }
            t.run(() -> pose(NorsePose.KNEEL))
                    .line(DEATH, d, "Fine. Fine. It doesn't matter. He's coming anyway, and you threw away the only thing that could have stopped him.")
                    .line(DEATH, d, "I hope you're very happy together. For the next five minutes.")
                    .direction("She sits there in the dust, rocking, laughing quietly at something nobody else can hear.");
        }
        if (w != null) {
            t.line(WARLOCK, w, "Stand with me. When he comes, I'll be right here. He can feel me a little. I'll make sure he does.");
        }
        t.abandoned(() -> coming(StoryDirector.player(lead)));
        story.talk(player, t);
        story.drawFellows(t, player);
    }

    /** Half of everything alive, all at once: she has what she wanted, and is gone; and Warlock is not. */
    private void culled(EntityPlayerMP player) {
        stage = Stage.CULLED;
        until = StoryDirector.now() + AFTER_CULL;
        goneForGood(DEATH_GONE);
        EntityNorse d = Oath.alive(death);
        EntityAdamWarlock w = alive(warlock);
        Talk t = scene = new Talk(player).reach(32D);
        t.direction("You close your hand. For a moment, nothing. Then, everywhere at once, a sound like a very great breath let out.");
        if (d != null) {
            t.run(() -> pose(NorsePose.SKY))
                    .line(DEATH, d, "Oh.")
                    .line(DEATH, d, "Oh, there they are. There they all are. Coming home.")
                    .run(() -> pose(NorsePose.REGARD))
                    .direction("Then she remembers you're there, and looks round, and it's not gratitude on her face. It's the look you give a waiter who's taken too long with the bill.")
                    .line(DEATH, d, "Finally. Was that so hard? You could have done that an hour ago and saved us both a very long, very boring argument.")
                    .line(DEATH, d, "Don't look at me like that. Like you want a medal. I found the stones. I told you where they were. I told you what to do with your hand. You closed it. Anyone can close a hand.")
                    .line(DEATH, d, "Oh, don't sulk. Fine. Good job. Well done. Gold star. Happy?")
                    .line(DEATH, d, "And if it keeps you up at night, later, remember whose hand it was. Not mine. I never touched a thing.")
                    .line(DEATH, d, "I have to go. There are so many of them, and they all need me, and honestly? They're a lot more interesting than you right now.")
                    .run(() -> {
                        for (EntityPlayerMP each : t.present()) {
                            Obsession.whisper(each, "Don't wait up.");
                        }
                    })
                    .pause(40)
                    .run(() -> {
                        EntityNorse her = Oath.alive(death);
                        if (her != null) {
                            her.setDead();
                        }
                        death = null;
                    })
                    .direction("Then she isn't there. Not like smoke. As if she never was, and won't be again.");
        }
        if (w != null) {
            t.pause(30)
                    .line(WARLOCK, w, "...What did you do?")
                    .run(() -> w.direct(EntityAdamWarlock.State.HOVER, w.getPositionVector().add(0D, 2D, 0D), player.getPositionEyes(1F), 0.2D))
                    .direction("The gold comes up in him like a fire somebody's thrown oil on.")
                    .line(WARLOCK, w, "WHAT DID YOU DO?")
                    .line(WARLOCK, w, "Half. HALF. I told you. Right at the very start I told you what she was, and you listened to her anyway.")
                    .line(WARLOCK, w, "I was made to keep the stones from people like you. I thought I'd failed at that already. I hadn't. Not until now.")
                    .line(WARLOCK, w, "No cocoon this time. No flying off. One of us doesn't leave.")
                    .run(() -> duel(w, player));
        }
        story.talk(player, t);
        story.drawFellows(t, player);
    }

    /** Warlock at them, a last time, to the death. */
    private void duel(EntityAdamWarlock w, EntityPlayerMP player) {
        scene = null;
        duel = true;
        w.toTheDeath();
        w.engage(player);
    }

    // ------------------------------------------------------------------ Warlock, while it plays out

    private void tickWarlock(@Nullable EntityPlayerMP p, long now) {
        EntityAdamWarlock w = alive(warlock);
        if (w != null && w.fallen()) {
            fell(w);
            return;
        }
        if (duel && w == null) {
            // Off without falling (whoever he was fighting fell instead): he does not come back from that.
            duel = false;
            warlock = null;
            goneForGood(WARLOCK_GONE);
            return;
        }
        if (!beside || farewellAt >= 0) {
            return;
        }
        EntityEternity e = Director.INSTANCE.body();
        boolean fight = Director.INSTANCE.arrived() && e != null && p != null && e.world == p.world;
        if (w != null && fought && Director.INSTANCE.data().phase == EternityData.Phase.ROAMING) {
            // He went, without dying: Warlock goes too, until he comes again.
            fought = engaged = false;
            warlock = null;
            w.script(true);
            StoryDirector.shout(w, WARLOCK, w.getEntityId(), "He'll be back. When he is, so will I.", 96D);
            w.dismiss();
            return;
        }
        if (w == null) {
            if (!fight || !warlockAlive() || now % 20 != 0) {
                return;
            }
            // Gone from beside them (unloaded, or left behind): he comes back for this.
            w = warlock = new EntityAdamWarlock(p.world);
            w.evolve();
            w.script(true);
            w.arriveAt(p.getPositionVector().add(flat(p.getLookVec()).scale(4D)).add(0D, 2D, 0D), e.chest());
            p.world.spawnEntity(w);
            engaged = false;
            StoryDirector.shout(w, WARLOCK, w.getEntityId(), "I'm here. I said I would be.", 96D);
            return;
        }
        if (w.getState() == EntityAdamWarlock.State.ARRIVE) {
            return;
        }
        if (fight && e.getDistance(w) < 128D) {
            if (!engaged) {
                engaged = fought = true;
                w.ally(true);
                w.mortalTo(EntityEternity.class, 0.05F);
                w.turnOn(e);
                e.rival(w);
                StoryDirector.shout(w, WARLOCK, w.getEntityId(), "Here. Look at me. LOOK AT ME, you overgrown night sky.", 96D);
            }
            return;
        }
        if (engaged) {
            engaged = false;
            w.script(true);
        }
        if (p != null && p.world == w.world && now % 10 == 0) {
            // Waiting at their shoulder.
            Vec3d at = p.getPositionVector().add(flat(p.getLookVec()).rotateYaw(1.2F).scale(2.5D)).add(0D, 1.5D, 0D);
            w.direct(EntityAdamWarlock.State.HOVER, at, p.getPositionEyes(1F), 0.35D);
        }
    }

    /** Beaten, this time for good: by Eternity, or by them. */
    private void fell(EntityAdamWarlock w) {
        boolean byThem = w.beatenBy() instanceof EntityPlayer;
        duel = beside = engaged = fought = false;
        warlock = null;
        SovereignData s = SovereignData.get();
        s.warlockFell = true;
        s.markDirty();
        EntityEternity e = Director.INSTANCE.body();
        if (e != null) {
            e.rival(null);
        }
        // He drops out of the air to the ground, and lies there; and stays, for anyone who comes looking.
        w.lieDead();
        Vec3d from = w.getPositionVector();
        Vec3d to = floor(w.world, from);
        int fallTicks = 14;
        for (int i = 1; i <= fallTicks; i++) {
            double k = i / (double) fallTicks;
            Vec3d at = from.add(to.subtract(from).scale(k * k));
            cue(i, () -> w.place(at, null));
        }
        cue(fallTicks, () -> {
            ((WorldServer) w.world).spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL, to.x, to.y + 0.2D, to.z, 14, 0.7D, 0.1D, 0.7D, 0.02D);
            w.world.playSound(null, to.x, to.y, to.z, EternitySounds.COMET_IMPACT, SoundCategory.HOSTILE, 2F, 1.3F);
        });
        StoryDirector.shout(w, WARLOCK, w.getEntityId(), byThem ? "So that's how it ends. Not with her. With you." : "Keep... fighting. Don't let it... have you...", 64D);
        if (byThem) {
            cue(70, () -> StoryDirector.shout(w, WARLOCK, w.getEntityId(), "I hope he's quick with you. I hope it doesn't hurt. I don't know why I hope that.", 64D));
        }
        story.sendStateToAll();
    }

    /** Eternity is dead: caged, or not. */
    void eternityFell(boolean contained) {
        EntityAdamWarlock w = alive(warlock);
        if (!beside || w == null || w.fallen()) {
            return;
        }
        engaged = false;
        w.ally(false);
        w.script(true);
        if (contained) {
            farewellAt = StoryDirector.now() + 120;
        } else {
            farewell(false);
        }
    }

    /** With him caged, Warlock says goodbye and goes; without, he is simply not there any more. */
    private void farewell(boolean contained) {
        EntityAdamWarlock w = alive(warlock);
        beside = false;
        warlock = null;
        goneForGood(WARLOCK_GONE);
        stage = Stage.DONE;
        if (w == null) {
            return;
        }
        if (!contained) {
            // Nobody sees him go. Nobody sees where.
            w.setDead();
            return;
        }
        EntityPlayerMP p = StoryDirector.player(lead);
        if (p == null || p.world != w.world || p.getDistance(w) > 96D) {
            p = nearest(w);
        }
        if (p == null) {
            w.dismiss();
            return;
        }
        EntityPlayerMP to = p;
        w.direct(EntityAdamWarlock.State.HOVER, to.getPositionVector().add(flat(to.getLookVec()).scale(4D)).add(0D, 1.5D, 0D), to.getPositionEyes(1F), 0.4D);
        boolean broke = broken();
        Talk t = new Talk(to).at(w, 96D).reach(48D);
        t.line(WARLOCK, w, "You did it. He's in there. All that is, in a box in your hand.")
                .line(WARLOCK, w, "When I first met you I thought you'd be the end of everything. I've never been so glad to be wrong.")
                .line(WARLOCK, w, broke ? "Thank you. For listening. For breaking them. For not being what I was afraid you'd be."
                        : "Thank you. For not closing your hand. For not being what I was afraid you'd be.")
                .line(WARLOCK, w, "I have to go. There's a lot of universe out there, and somebody should keep an eye on it. Farewell.")
                .run(w::dismiss)
                .direction("He goes straight up, gold, faster than you've ever seen him move, and keeps going until he's a star, and then not even that.");
        story.talk(to, t);
        story.drawFellows(t, to);
    }

    // ------------------------------------------------------------------ helpers

    /** Her, in front of them, as they last saw her. */
    private EntityNorse appear(EntityPlayerMP player, Vec3d at) {
        World world = player.world;
        EntityNorse d = EntityNorse.of(world, NorseBoxes.STRANGER, "reckoning");
        Devotion.dress(story, d, player);
        d.setLocationAndAngles(at.x, at.y, at.z, 0F, 0F);
        d.face(player.getPositionVector());
        world.spawnEntity(d);
        d.watch(player);
        ((WorldServer) world).spawnParticle(EnumParticleTypes.SMOKE_LARGE, at.x, at.y + 1D, at.z, 30, 0.3D, 0.9D, 0.3D, 0.02D);
        return d;
    }

    /** She does something, if she is still here. */
    private void pose(NorsePose pose) {
        EntityNorse d = Oath.alive(death);
        if (d != null) {
            d.act(pose);
        }
    }

    private void goneForGood(String beat) {
        StoryData data = story.data();
        if (data.beats.add(beat)) {
            data.markDirty();
        }
    }

    @Nullable
    private static EntityAdamWarlock alive(@Nullable EntityAdamWarlock w) {
        return w != null && !w.isDead ? w : null;
    }

    @Nullable
    private static EntityPlayerMP nearest(EntityAdamWarlock w) {
        EntityPlayerMP best = null;
        for (EntityPlayer p : w.world.playerEntities) {
            if (p instanceof EntityPlayerMP && !p.isSpectator() && p.getDistance(w) < 96D && (best == null || p.getDistance(w) < best.getDistance(w))) {
                best = (EntityPlayerMP) p;
            }
        }
        return best;
    }

    private static Vec3d flat(Vec3d v) {
        Vec3d f = new Vec3d(v.x, 0D, v.z);
        return f.lengthSquared() < 1.0E-4 ? new Vec3d(0D, 0D, 1D) : f.normalize();
    }

    /** The ground at or near a point: somewhere two blocks of air stand on something solid. */
    private static Vec3d ground(World world, Vec3d near) {
        for (int dy = 3; dy >= -4; dy--) {
            BlockPos p = new BlockPos(near.x, near.y + dy, near.z);
            if (world.isAirBlock(p) && world.isAirBlock(p.up()) && world.getBlockState(p.down()).isSideSolid(world, p.down(), EnumFacing.UP)) {
                return new Vec3d(p.getX() + 0.5D, p.getY(), p.getZ() + 0.5D);
            }
        }
        return near;
    }

    /** The first solid ground straight down from a point. */
    private static Vec3d floor(World world, Vec3d from) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int y = (int) Math.floor(from.y); y > 0; y--) {
            p.setPos(from.x, y - 1, from.z);
            if (world.getBlockState(p).getMaterial().blocksMovement()) {
                return new Vec3d(from.x, y, from.z);
            }
        }
        return from;
    }

    /** For testing: it is put to them now, six stones or not. */
    String force(EntityPlayerMP player) {
        if (stage != Stage.NONE && stage != Stage.DONE) {
            return "It's already being put to someone.";
        }
        reset();
        story.data().beats.remove(BEGUN);
        story.hush(player);
        begin(player);
        return stage == Stage.DONE ? "Neither Death nor Warlock is left to say anything: Eternity will just come."
                : "Warlock and Death come to have it out over your hand.";
    }
}
