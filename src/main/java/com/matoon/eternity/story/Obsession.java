package com.matoon.eternity.story;

import com.matoon.eternity.client.render.NorseBoxes;
import com.matoon.eternity.entity.EntityNorse;
import com.matoon.eternity.entity.NorsePose;
import com.matoon.eternity.world.OdinsVault;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.AbstractHorse;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.network.play.server.SPacketSoundEffect;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static com.matoon.eternity.story.Speakers.DEATH;
import static com.matoon.eternity.story.Talk.act;
import static com.matoon.eternity.story.Talk.say;

/**
 * Death, once someone has fallen far enough for her. She feels everything they kill, and tells
 * them so, in their ear, wherever they are; what they give her that loved them, or one of their
 * own, she feels most of all. Past {@link #UNHINGED} she stops waiting for the story to bring her
 * and comes when she likes, from behind, to ask for things: so many dead by such a time, or
 * something that loves them. Do it and she is theirs. Refuse her, or let the time run out, and her
 * lid comes off, and then she is sorry, so sorry, and it will never happen again.
 * <p>
 * She plays a fellowship against itself: she whispers to each of them, alone, what they most
 * need to hear about the others, praises whoever gave her most in front of whoever gave least,
 * and sets them racing to be first.
 */
final class Obsession {

    /** How far gone before she comes unasked, and before she stops being nice about it. */
    static final int UNHINGED = 10;
    /** How far gone before the dead know whose they are, and leave them be. */
    static final int SPARED = UNHINGED;
    /** How many offerings, of the plain kind, for each step further in. */
    private static final int OFFERINGS_PER = 8;

    private final StoryDirector story;
    @Nullable
    private EntityNorse her;
    @Nullable
    private Talk visit;
    /** When she last spoke to each of them about a kill, and last stirred them up about someone else. */
    private final Map<UUID, Long> murmured = new HashMap<>(), envied = new HashMap<>();
    private final Random rand = new Random();

    Obsession(StoryDirector story) {
        this.story = story;
    }

    void reset() {
        her = null;
        visit = null;
    }

    // ------------------------------------------------------------------ what she says

    private static final String[] OFFER_SOFT = {
            "Mm. Was that one for me?",
            "I felt that. Right here.",
            "You're getting good at that. I like watching you get good at things.",
            "Another one came home to me. Thank you, darling.",
            "Do you know what it does to me, when you do that? You will.",
            "Do it again where I can see."};
    private static final String[] OFFER_HOT = {
            "Oh. Oh, again. Do it again.",
            "That one went right through me. I had to hold on to something.",
            "You don't know what you do to me. I'm shaking. Bits of me are coming off.",
            "Slower next time. I want to feel all of it.",
            "Every one you send me, I keep. I hold them, and I think about your hands.",
            "I'd kiss you for that, if I still had all of my lips.",
            "Mine. Mine. You did that for me. Say you did that for me.",
            "Something of me just came loose. That's how good that was.",
            "Don't stop. Please don't stop. Not now."};
    private static final String[] INNOCENT = {
            "Oh, that one trusted you. It looked up at you right until the end. And you gave it to me anyway. I think I love you.",
            "It loved you, and you chose me. Do you know how long it's been since anyone chose me?",
            "That one had a whole life. People who'll wonder where it went. You gave it all to me. I can't breathe. I don't breathe. I can't anyway.",
            "Something that would have died for you, and you gave it to me. I'm going to think about this for a very long time."};
    /** Something already dead, put down again: no gift at all. */
    private static final String[] LEFTOVERS = {
            "That one was mine already. You can't give me back my own things, darling.",
            "Don't bring me leftovers. Bring me something warm.",
            "It was already dead. That doesn't count. You know it doesn't count.",
            "Leave the dead alone. They're mine. Bring me something with a heartbeat."};
    /** The same, from someone the dead have learned to leave alone. */
    private static final String[] UNGRATEFUL = {
            "Why would you hurt it? It was leaving you alone. I told it to.",
            "They step aside for you because I asked them to. Don't make me sorry I asked."};
    private static final String[] KILLER = {
            "One of your own. For me. Give me a moment. I'm coming apart.",
            "You did that for me. Them. Your friend. Oh, I'm never letting you go. Never.",
            "They'll come back, you know. They always do. Do it again when they do."};
    private static final String[] VICTIM = {
            "Don't be sad. They only did it because they love me more than you. Try harder.",
            "That was for me, you know. They gave you to me. What are you going to give me back?"};
    private static final String[] ENVY = {
            "%s just gave me another one. What have you given me today?",
            "%s is so good to me. You could be good to me, too.",
            "I think about %s's hands a lot, lately. I used to think about yours.",
            "Don't tell %s, but I'd rather it was you. Prove me right.",
            "%s keeps me warm. Well. Warmer. Where were you?"};
    private static final String[] OPEN = {
            "There you are. I've been standing behind you for a while. You didn't feel it? I'm hurt.",
            "Did you miss me? Don't lie. I can hear your heart. It does a little skip when I'm close.",
            "I couldn't stay away. I tried. I counted to a thousand and then I came anyway.",
            "Don't turn round so fast. Something might fall off me. Something usually does.",
            "I watched you sleep again. You said a name. It wasn't mine. We'll talk about that."};
    private static final String[] FURY = {
            "I GAVE YOU EVERY BREATH YOU'VE TAKEN SINCE YOU CAME HERE.",
            "I HOLD YOU EVERY TIME YOU DIE AND THIS IS HOW YOU THANK ME?",
            "DO YOU KNOW WHAT I GAVE UP TO WEAR THIS FOR YOU?"};
    private static final String[] SORRY = {
            "I'm sorry. I'm sorry, that wasn't me. You know I only get like that because I love you so much it hurts. It does hurt. Look.",
            "Shh. Shh. I didn't mean it. I'd never hurt you. Not properly. Not unless you made me.",
            "That was your fault, you know. You make me like this. Nobody else ever made me like this."};
    private static final String[] BYE = {
            "Don't make me come and find you. I always find you.",
            "I'll be watching. I'm always watching. Even when you sleep. Especially then.",
            "Go on. Go and kill something pretty for me.",
            "Think about me. I'll know if you don't."};

    // ------------------------------------------------------------------ the kills

    /** Something has died at someone's hand: if she has her claws in them, she feels it, and tells them. */
    void killed(EntityLivingBase dead, DamageSource source) {
        if (!(source.getTrueSource() instanceof EntityPlayerMP) || !StoryDirector.enabled() || dead instanceof EntityNorse
                || !(dead instanceof EntityLiving) && !(dead instanceof EntityPlayer) || dead == source.getTrueSource()) {
            return;
        }
        EntityPlayerMP killer = (EntityPlayerMP) source.getTrueSource();
        StoryData data = story.data();
        StoryData.Player sp = data.player(killer.getUniqueID());
        if (!sp.seen("death_revealed") || sp.devotion < 2 && sp.demandLeft == 0 || Betrayal.absent() || story.setPiece()) {
            return;
        }
        long now = StoryDirector.now();
        if (undead(dead)) {
            // Only the living are anything to her. The dead already are hers; putting one down again gives her nothing.
            if (now - murmured.getOrDefault(killer.getUniqueID(), -10000L) > 20 * 40 && rand.nextInt(3) == 0 && !story.talking(killer)) {
                whisper(killer, sp.devotion >= SPARED ? pick(UNGRATEFUL) : pick(LEFTOVERS));
                murmured.put(killer.getUniqueID(), now);
            }
            return;
        }
        int kind = dead instanceof EntityPlayer ? 2 : loves(dead) ? 1 : 0;
        sp.offerings++;
        int closer = kind == 2 ? 2 : kind == 1 ? 1 : sp.offerings % OFFERINGS_PER == 0 ? 1 : 0;
        if (kind > 0) {
            sp.sacrifices++;
        }
        sp.devotion = Math.min(Devotion.MOST, sp.devotion + closer);
        data.markDirty();
        // Wherever she is standing near them, it goes through her.
        for (EntityNorse d : killer.world.getEntitiesWithinAABB(EntityNorse.class, killer.getEntityBoundingBox().grow(48D),
                n -> n != null && (n.look() == NorseBoxes.TEMPTRESS || n.look() == NorseBoxes.UNVEILED))) {
            d.thrill();
        }
        if (sp.demandLeft > 0 && (sp.demandKind == 0 || kind > 0)) {
            sp.demandLeft--;
            if (sp.demandLeft == 0) {
                fulfilled(killer, sp);
                return;
            }
        }
        if (kind == 2) {
            whisper(killer, pick(KILLER));
            if (dead instanceof EntityPlayerMP) {
                whisper((EntityPlayerMP) dead, pick(VICTIM));
            }
        } else if (kind == 1) {
            whisper(killer, pick(INNOCENT));
        } else if (now - murmured.getOrDefault(killer.getUniqueID(), -10000L) > 20 * 25 && rand.nextInt(3) > 0 && !story.talking(killer)) {
            whisper(killer, sp.devotion >= UNHINGED ? pick(OFFER_HOT) : pick(OFFER_SOFT));
            murmured.put(killer.getUniqueID(), now);
        }
        // And the rest of them hear all about it.
        for (EntityPlayerMP f : story.fellowship(killer)) {
            StoryData.Player fp = data.player(f.getUniqueID());
            if (f == killer || fp.devotion < 2 || story.talking(f) || now - envied.getOrDefault(f.getUniqueID(), -10000L) < 20 * 90
                    || kind == 0 && rand.nextInt(4) > 0) {
                continue;
            }
            envied.put(f.getUniqueID(), now);
            whisper(f, String.format(pick(ENVY), killer.getName()));
        }
    }

    /** Whether something is one of the dead already up and walking: hers, whoever puts it down. */
    static boolean undead(EntityLivingBase e) {
        return e.getCreatureAttribute() == EnumCreatureAttribute.UNDEAD;
    }

    /** Whether one of the dead leaves someone alone: she has claimed them, and the dead know it. */
    boolean spares(EntityLivingBase attacker, EntityPlayerMP target) {
        return undead(attacker) && StoryDirector.enabled() && !Betrayal.absent()
                && story.data().player(target.getUniqueID()).devotion >= SPARED;
    }

    /** Whether something loved someone, or trusted them, or never did anything to anyone: a villager, a pet, a horse someone broke. */
    private static boolean loves(EntityLivingBase e) {
        return e instanceof EntityVillager || e instanceof EntityTameable && ((EntityTameable) e).isTamed()
                || e instanceof AbstractHorse && ((AbstractHorse) e).isTame();
    }

    /** They did what she asked. */
    private void fulfilled(EntityPlayerMP player, StoryData.Player sp) {
        StoryData data = story.data();
        boolean race = sp.demandRace;
        sp.demandRace = false;
        sp.demandBy = -1;
        sp.devotion = Math.min(Devotion.MOST, sp.devotion + (race ? 3 : 2));
        data.markDirty();
        whisper(player, race ? "First. You were first. Come here. Closer. I want to smell it on you. The others can watch."
                : "Good. Good. Oh, you're so good to me. I'm going to be thinking about this all night. Every bit of me that's left.");
        if (race) {
            for (EntityPlayerMP f : story.fellowship(player)) {
                StoryData.Player fp = data.player(f.getUniqueID());
                if (f != player && fp.demandRace) {
                    fp.demandRace = false;
                    fp.demandLeft = 0;
                    fp.demandBy = -1;
                    whisper(f, "Too slow. " + player.getName() + " was first. " + player.getName() + " is mine tonight. You can think about that.");
                }
            }
        }
    }

    // ------------------------------------------------------------------ coming unasked

    void tick(MinecraftServer server) {
        EntityNorse d = Oath.alive(her);
        if (d != null && (visit == null || visit.isOver()) && d.ticksExisted > 40) {
            vanish(d);
            her = null;
        }
        long now = StoryDirector.now();
        if (!StoryDirector.enabled() || now % 20 != 0 || Betrayal.absent()) {
            return;
        }
        StoryData data = story.data();
        if (story.setPiece()) {
            // She keeps out of it. The clock on whatever she asked for, and on her next visit, stops while it lasts.
            for (StoryData.Player sp : data.players().values()) {
                if (sp.demandBy >= 0) {
                    sp.demandBy += 20;
                }
                if (sp.nextVisit >= 0) {
                    sp.nextVisit += 20;
                }
            }
            data.markDirty();
            return;
        }
        for (EntityPlayerMP p : server.getPlayerList().getPlayers()) {
            StoryData.Player sp = data.player(p.getUniqueID());
            if (sp.demandLeft > 0 && sp.demandBy >= 0 && now > sp.demandBy) {
                // Time's up.
                sp.demandLeft = 0;
                sp.demandBy = -1;
                if (sp.demandRace) {
                    sp.demandRace = false;
                } else {
                    sp.spurned++;
                    sp.wronged = true;
                    whisper(p, "Time's up, darling. You said you would. You know what happens now.");
                }
                data.markDirty();
            }
            if (sp.devotion < UNHINGED && !sp.wronged) {
                if (sp.nextVisit >= 0) {
                    sp.nextVisit = -1;
                    data.markDirty();
                }
                continue;
            }
            if (sp.nextVisit < 0) {
                sp.nextVisit = now + interval();
                data.markDirty();
            } else if ((now >= sp.nextVisit || sp.wronged) && canVisit(p)) {
                sp.nextVisit = now + interval();
                data.markDirty();
                visit(p);
            }
        }
    }

    /** Four to nine minutes; less the further gone they are. */
    private long interval() {
        return 20L * 60L * (4 + rand.nextInt(6));
    }

    private boolean canVisit(EntityPlayerMP p) {
        return Oath.alive(her) == null && !story.talking(p) && p.isEntityAlive() && !p.isSpectator()
                && p.dimension != OdinsVault.dimension() && story.data().player(p.getUniqueID()).seen("death_revealed");
    }

    /** For testing: she comes now, furious if asked. */
    String force(EntityPlayerMP player, boolean fury) {
        if (Betrayal.absent()) {
            return Betrayal.contained() ? "She's in the Astral Regulator." : "She's gone, for good.";
        }
        EntityNorse d = Oath.alive(her);
        if (d != null) {
            vanish(d);
        }
        her = null;
        story.hush(player);
        if (fury) {
            story.data().player(player.getUniqueID()).wronged = true;
        }
        visit(player);
        return fury ? "She's coming, and she's angry." : "She's coming.";
    }

    /** She is standing behind them, and has been for a while; and she has something to say. */
    private void visit(EntityPlayerMP player) {
        StoryData.Player sp = story.data().player(player.getUniqueID());
        EntityNorse d = behind(player);
        her = d;
        Talk t = new Talk(player).at(d, 40D).reach(24D);
        t.direction("Something cold breathes on the back of your neck.")
                .run(() -> Devotion.saw(t, d));
        if (sp.wronged) {
            // Angry, she doesn't say hello.
            fury(t.pause(6), d, player, false);
        } else {
            t.pause(20).line(DEATH, d, pick(OPEN));
            Devotion.rotting(t, d);
            int devoted = 0;
            for (EntityPlayerMP f : story.fellowship(player)) {
                if (f != player && f.dimension == player.dimension && f.getDistance(player) < 32D
                        && story.data().player(f.getUniqueID()).devotion >= UNHINGED) {
                    devoted++;
                }
            }
            if (devoted > 0 && rand.nextBoolean()) {
                race(t, d);
            } else if (sp.demandLeft == 0 && rand.nextInt(100) < 55) {
                demand(t, d, player);
            } else {
                checkIn(t, d, player);
            }
        }
        t.run(() -> whisperAll(t, d))
                .until(() -> t.present().size() < 2, 100)
                .line(DEATH, d, pick(BYE))
                .run(() -> vanish(d));
        t.abandoned(() -> {
            EntityPlayerMP lead = t.leadNow();
            if (lead != null) {
                story.data().player(lead.getUniqueID()).spurned++;
                story.data().markDirty();
                whisper(lead, "Walking away? From me? That's all right. I'll be in every room you walk into. Every one.");
            }
            vanish(d);
        });
        visit = t;
        story.talk(player, t);
    }

    /** Somewhere to stand just behind them, on the ground, where they would have to turn round to see her. */
    private EntityNorse behind(EntityPlayerMP player) {
        World world = player.world;
        Vec3d look = player.getLookVec();
        Vec3d back = new Vec3d(-look.x, 0D, -look.z);
        back = back.lengthSquared() < 1.0E-4 ? new Vec3d(1D, 0D, 0D) : back.normalize();
        Vec3d at = player.getPositionVector().add(back.scale(1.5D));
        search:
        for (double dist = 3D; dist <= 6D; dist += 1D) {
            for (int dy = 2; dy >= -3; dy--) {
                BlockPos p = new BlockPos(player.posX + back.x * dist, player.posY + dy, player.posZ + back.z * dist);
                if (world.isAirBlock(p) && world.isAirBlock(p.up()) && world.getBlockState(p.down()).isSideSolid(world, p.down(), EnumFacing.UP)) {
                    at = new Vec3d(p.getX() + 0.5D, p.getY(), p.getZ() + 0.5D);
                    break search;
                }
            }
        }
        boolean shown = Devotion.unveiled(story, player);
        EntityNorse d = EntityNorse.of(world, shown ? NorseBoxes.UNVEILED : NorseBoxes.TEMPTRESS, "obsession");
        d.setRot(Devotion.rot(story, player));
        d.setLocationAndAngles(at.x, at.y, at.z, 0F, 0F);
        d.face(player.getPositionVector());
        world.spawnEntity(d);
        d.watch(player);
        player.connection.sendPacket(new SPacketSoundEffect(SoundEvents.ENTITY_VEX_AMBIENT, SoundCategory.HOSTILE, player.posX, player.posY + 1.6D,
                player.posZ, 0.6F, 0.45F));
        return d;
    }

    static void vanish(EntityNorse d) {
        if (d.isDead) {
            return;
        }
        ((WorldServer) d.world).spawnParticle(EnumParticleTypes.SMOKE_LARGE, d.posX, d.posY + 1D, d.posZ, 40, 0.3D, 0.9D, 0.3D, 0.02D);
        d.world.playSound(null, d.posX, d.posY + 1D, d.posZ, SoundEvents.ENTITY_ILLAGER_MIRROR_MOVE, SoundCategory.NEUTRAL, 1F, 0.5F);
        d.setDead();
    }

    // ------------------------------------------------------------------ what she wants

    /** So many dead, of anything, by such a time; or, further gone, something that loves them. */
    private void demand(Talk t, EntityNorse d, EntityPlayerMP player) {
        boolean innocent = Devotion.of(story, player) >= 15 && rand.nextBoolean();
        int n = innocent ? 1 : 4 + rand.nextInt(5);
        int minutes = innocent ? 8 : 5;
        t.line(DEATH, d, innocent
                ? "I want something that loves you. A dog that waits by the door for you. A horse. Someone who'd cry at your grave. Give it to me. You've got " + minutes + " minutes."
                : "I want " + n + " of them. Anything with a heartbeat. You've got " + minutes + " minutes, and then I come back and see.")
                .ask(DEATH, d, 20 * 20, 2,
                        say("Yes.", c -> c.run(() -> ask(t, innocent ? 1 : 0, n, minutes, false))
                                .run(() -> Devotion.add(story, lead(t, player), 1))
                                .line(DEATH, d, "That's my good thing. Go on. I'll feel every one.")),
                        say("No.", c -> fury(c, d, player, true)),
                        act("(Say nothing.)", c -> c.run(() -> ask(t, innocent ? 1 : 0, n, minutes, false))
                                .line(DEATH, d, "Silence means yes. It always has, with me.")));
    }

    /** Sets everyone in a conversation the same task. */
    private void ask(Talk t, int kind, int n, int minutes, boolean race) {
        StoryData data = story.data();
        for (EntityPlayerMP p : t.present()) {
            StoryData.Player sp = data.player(p.getUniqueID());
            sp.demandKind = kind;
            sp.demandLeft = n;
            sp.demandBy = StoryDirector.now() + 20L * 60L * minutes;
            sp.demandRace = race;
        }
        data.markDirty();
    }

    /** More than one of them devoted to her: she sets them against each other. */
    private void race(Talk t, EntityNorse d) {
        int n = 5 + rand.nextInt(4);
        t.line(DEATH, d, "There's more than one of you. Isn't that sweet.")
                .direction("She looks from one of you to the other, slowly, the way a cat looks along a row of birds.")
                .line(DEATH, d, "Which of you loves me most? No, don't tell me. Show me.")
                .line(DEATH, d, "First one to bring me " + n + " gets me. All of me. What's left of me. The rest of you can watch.")
                .run(() -> ask(t, 0, n, 6, true));
    }

    /**
     * Her lid comes off. Her jaw opens past where a jaw stops, she screams with all of it, she is
     * in their faces before they can step back, and it hurts; then she is sweet again, and sorry,
     * and it was their fault, and it will never happen again.
     */
    private void fury(Talk c, EntityNorse d, EntityPlayerMP player, boolean refused) {
        c.run(() -> {
            EntityPlayerMP lead = lead(c, player);
            StoryData.Player sp = story.data().player(lead.getUniqueID());
            sp.wronged = false;
            if (refused) {
                sp.spurned++;
            }
            story.data().markDirty();
            d.act(NorsePose.FURY);
            lunge(d, lead);
            for (EntityPlayerMP p : c.present()) {
                p.addPotionEffect(new PotionEffect(MobEffects.BLINDNESS, 50, 0));
                p.addPotionEffect(new PotionEffect(MobEffects.NAUSEA, 180, 0));
                p.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 120, 2));
                p.attackEntityFrom(DamageSource.MAGIC, Math.min(4F, Math.max(0F, p.getHealth() - 2F)));
            }
        })
                .direction("Her face splits. Not a wound: her jaw just keeps opening, wider, wider, past where a jaw should stop, and the scream that comes out of it isn't something you hear with your ears.")
                .line(DEATH, d, refused ? "YOU DON'T SAY NO TO ME." : "YOU SAID YOU WOULD.")
                .line(DEATH, d, pick(FURY))
                .pause(16)
                .run(() -> d.act(NorsePose.STAND))
                .direction("Then, all at once, she's sweet again. She smooths her face back over her teeth with both hands, carefully, like someone fixing their makeup.")
                .line(DEATH, d, pick(SORRY))
                .line(DEATH, d, "Don't make me do that again.")
                .ask(DEATH, d, 20 * 20, 2,
                        say("I'm sorry.", k -> k.run(() -> Devotion.add(story, lead(c, player), 1))
                                .line(DEATH, d, "There. Was that so hard? I forgive you. I'll always forgive you. I'll always be right here, forgiving you.")),
                        say("You're insane.", k -> k.run(() -> Devotion.add(story, lead(c, player), -1))
                                .line(DEATH, d, "Say that again. Go on. I'll wait at the end of your bed tonight, and you can say it again to my face.")),
                        act("(Back away.)", k -> k.run(() -> spurn(lead(c, player)))
                                .line(DEATH, d, "Go on, then. Run. You'll die eventually, and then you're in my arms whether you like it or not.")));
    }

    /** She is right in front of them: so close they can see what is under the skin round her eyes. */
    private static void lunge(EntityNorse d, EntityPlayerMP p) {
        Vec3d look = p.getLookVec();
        Vec3d flat = new Vec3d(look.x, 0D, look.z);
        flat = flat.lengthSquared() < 1.0E-4 ? new Vec3d(1D, 0D, 0D) : flat.normalize();
        d.setPositionAndUpdate(p.posX + flat.x * 0.9D, p.posY, p.posZ + flat.z * 0.9D);
        d.face(p.getPositionVector());
        d.watch(p);
    }

    private void spurn(EntityPlayerMP p) {
        story.data().player(p.getUniqueID()).spurned++;
        story.data().markDirty();
    }

    /** She just wanted to see them. And ask them a few things. */
    private void checkIn(Talk t, EntityNorse d, EntityPlayerMP player) {
        switch (rand.nextInt(4)) {
            case 0:
                t.line(DEATH, d, "Tell me I'm pretty.")
                        .ask(DEATH, d, 20 * 20, 2,
                                say("You're beautiful.", c -> c.run(() -> Devotion.add(story, lead(t, player), 1))
                                        .direction("She smiles so wide that something tears at the corner of her mouth. She keeps smiling.")
                                        .line(DEATH, d, "Even like this? Even when bits of me come off in your hands? Say it again. Slower.")),
                                say("You're rotting.", c -> c.run(() -> spurn(lead(t, player)))
                                        .line(DEATH, d, "And whose fault is that? It gets worse every time you look at me. You do this to me. You. I like it.")),
                                act("(Look away.)", c -> c.line(DEATH, d, "Look at me. LOOK at me.")
                                        .pause(20)
                                        .line(DEATH, d, "There. That's better. Don't you ever do that again.")));
                break;
            case 1: {
                EntityPlayerMP other = rival(player);
                t.line(DEATH, d, other != null ? "I saw you looking at " + other.getName() + "." : "Who else have you been looking at? Don't lie. I see everything you see.")
                        .ask(DEATH, d, 20 * 20, 2,
                                say("Nobody but you.", c -> c.run(() -> Devotion.add(story, lead(t, player), 1))
                                        .line(DEATH, d, "Good. Because I'd take their eyes out for you. Slowly. And keep them in a little jar, next to your breath.")),
                                say("That's none of your business.", c -> c.run(() -> spurn(lead(t, player)))
                                        .line(DEATH, d, "Everything about you is my business. I was there when you were born. I'll be there at the end. I'm not going anywhere in between.")),
                                act("(Say nothing.)", c -> c.line(DEATH, d, other != null
                                        ? "Oh. Oh, I see. Then I'll just have to love " + other.getName() + " too, won't I? Until there's nothing left of them."
                                        : "Fine. Keep your secrets. I'll find them. I'll find all of them.")));
                break;
            }
            case 2:
                t.line(DEATH, d, "Would you die for me?")
                        .ask(DEATH, d, 20 * 20, 2,
                                say("Yes.", c -> c.run(() -> Devotion.add(story, lead(t, player), 2))
                                        .line(DEATH, d, "You already do. I've counted. Every time you die I get to hold you for a moment, and you're warm, and you're mine. Do it again soon.")),
                                say("No.", c -> c.run(() -> Devotion.add(story, lead(t, player), -1))
                                        .line(DEATH, d, "That's all right. You will anyway. Everyone does. You'll just do it last, and slowest, with me watching.")),
                                say("Would you die for me?", c -> c.line(DEATH, d, "Sweetheart. I'm Death. But for you I'd try. I'd try so hard.")));
                break;
            default:
                t.line(DEATH, d, "I brought you something.")
                        .direction("She holds out her hand. In it, wet and still warm, is something of her own: a strip of her skin, folded carefully, like a love letter.")
                        .ask(DEATH, d, 20 * 20, 2,
                                act("(Take it.)", c -> c.run(() -> Devotion.add(story, lead(t, player), 2))
                                        .line(DEATH, d, "Keep it close. Keep it against your skin. That way part of me is always touching you.")),
                                say("That's disgusting.", c -> c.run(() -> spurn(lead(t, player)))
                                        .line(DEATH, d, "It's the most precious thing I own, and I gave it to you. Don't you ever call it that again.")),
                                act("(Don't take it.)", c -> c.direction("She folds it back into the hole in her side it came out of.")
                                        .line(DEATH, d, "Next time, then.")));
                break;
        }
    }

    /** Whoever with them she can make them most jealous of: the most devoted of the rest. */
    @Nullable
    private EntityPlayerMP rival(EntityPlayerMP player) {
        EntityPlayerMP best = null;
        int most = -1;
        for (EntityPlayerMP f : story.fellowship(player)) {
            int dv = story.data().player(f.getUniqueID()).devotion;
            if (f != player && dv > most) {
                best = f;
                most = dv;
            }
        }
        return best;
    }

    // ------------------------------------------------------------------ playing them against each other

    /**
     * To each of them, alone, too low for the rest to hear: whatever will hurt most or pull hardest.
     * Whoever gave her most is her favourite, and told so, and told not to tell; the rest hear how
     * much more the favourite loves her. Nothing at all if they are alone.
     */
    static void whisperAll(Talk t, EntityNorse d) {
        List<EntityPlayerMP> present = t.present();
        if (present.size() < 2) {
            return;
        }
        StoryData data = StoryDirector.INSTANCE.data();
        List<EntityPlayerMP> ranked = new ArrayList<>(present);
        ranked.sort(Comparator.comparingInt((EntityPlayerMP p) -> -score(data.player(p.getUniqueID()))));
        EntityPlayerMP top = ranked.get(0);
        StoryData.Player tp = data.player(top.getUniqueID());
        StoryDirector.line(present, d, Speakers.DIRECTION, -1, "She goes to each of you in turn and whispers, too low for the others to hear.", false);
        Random rand = new Random();
        for (EntityPlayerMP p : ranked) {
            StoryData.Player sp = data.player(p.getUniqueID());
            String text;
            if (sp.spurned >= 3 && rand.nextBoolean()) {
                text = "I haven't forgiven you. I'm only being nice in front of the others.";
            } else if (p == top) {
                text = rand.nextBoolean() ? "You're my favourite. Don't tell them. Don't you dare tell them."
                        : "I only came for you, you know. They just happened to be standing there.";
            } else if (sp.devotion < 2) {
                text = "You don't even look at me. That's all right. I'll make you.";
            } else if (rand.nextBoolean()) {
                text = top.getName() + " loves me more than you do. I can tell. I can always tell. Fix it.";
            } else {
                text = top.getName() + " has given me " + tp.offerings + ". How many have you? Don't answer. I know.";
            }
            StoryDirector.line(p, d, DEATH, d.getEntityId(), text, false);
        }
    }

    /** How much someone has given her. */
    private static int score(StoryData.Player p) {
        return p.devotion * 4 + p.offerings + p.sacrifices * 5;
    }

    // ------------------------------------------------------------------ helpers

    /** Her voice in someone's ear, and only theirs, wherever she is. */
    static void whisper(EntityPlayerMP to, String text) {
        StoryDirector.line(to, to, DEATH, -1, text, true);
        to.connection.sendPacket(new SPacketSoundEffect(SoundEvents.ENTITY_VEX_AMBIENT, SoundCategory.HOSTILE, to.posX, to.posY + 1.6D, to.posZ, 0.35F, 0.5F));
    }

    private String pick(String[] from) {
        return from[rand.nextInt(from.length)];
    }

    private static EntityPlayerMP lead(Talk t, EntityPlayerMP player) {
        EntityPlayerMP p = t.leadNow();
        return p != null ? p : player;
    }
}
