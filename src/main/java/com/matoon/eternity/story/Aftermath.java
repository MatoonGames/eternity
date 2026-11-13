package com.matoon.eternity.story;

import com.matoon.eternity.client.render.NorseBoxes;
import com.matoon.eternity.entity.EntityNorse;
import com.matoon.eternity.entity.NorsePose;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.matoon.eternity.story.Speakers.DEATH;
import static com.matoon.eternity.story.Speakers.PARTY;
import static com.matoon.eternity.story.Talk.act;
import static com.matoon.eternity.story.Talk.say;

/**
 * After the cull, after Eternity, after the Tribunal: Death, once, the way you'd run into an ex.
 * <p>
 * Stripped of everything and sent home: they can curse her name into the empty air, or not. Cursed,
 * she comes; left uncursed, she comes anyway, a while later, because nobody ignores her. Either way
 * she comes looking better than she ever has, and treats them like the strange obsessive ex they
 * are to her now. She's seeing someone. Thanos. He has a great many things they don't, and she lists
 * them. Anything they say back only makes it weirder for her, and she leaves.
 * <p>
 * Home with the Living Tribunal in the Regulator, and Eternity with him, or let go by him with all
 * they had: she comes on her own, and stands a little way off, for a while. They can draw her into
 * the Regulator (see {@link Betrayal}), or walk up to her and talk. Talked to, she has gone off them: they are bigger than she is now, and she likes
 * being the one in charge. Winning her back is hopeless; she is seeing someone; and lashing out at
 * her only makes them the stalker in her story.
 */
final class Aftermath {

    /** World beat: she has been back, the once. */
    static final String AFTER = "death_after";
    /** How long after they are home before it comes; how long she waits to be spoken to; how long she can stand being ignored. */
    private static final int SETTLE = 20 * 5, WINDOW = 20 * 75, SNUB = 20 * 90;
    /** How near they come before she starts in on them. */
    private static final double NEAR = 4D;

    /** Sent home with nothing; home with the Tribunal in the Regulator; let go by him with everything. */
    private enum Kind { STRIPPED, POWERFUL, ACQUITTED }

    private final StoryDirector story;
    @Nullable
    private UUID who;
    private Kind kind = Kind.STRIPPED;
    private long startAt = -1;
    @Nullable
    private EntityNorse her;
    @Nullable
    private Talk scene;
    private long leaveAt = -1;
    /** They said nothing, and she is coming anyway. */
    private boolean snub;

    Aftermath(StoryDirector story) {
        this.story = story;
    }

    void reset() {
        who = null;
        startAt = -1;
        her = null;
        scene = null;
        leaveAt = -1;
        snub = false;
    }

    /** Back from the Tribunal, judged: if it was the cull that brought them there, she has something to say about it. */
    void judged(EntityPlayerMP player, boolean stripped, boolean absorbed, boolean acquitted) {
        if (!due() || !stripped && !absorbed && !acquitted) {
            return;
        }
        who = player.getUniqueID();
        kind = stripped ? Kind.STRIPPED : absorbed ? Kind.POWERFUL : Kind.ACQUITTED;
        snub = false;
        startAt = StoryDirector.now() + SETTLE;
    }

    private boolean due() {
        StoryData data = story.data();
        return data.seen(Reckoning.CULLED) && !Betrayal.contained() && !data.seen(AFTER) && Oath.alive(her) == null;
    }

    boolean has(EntityNorse d) {
        return d == her;
    }

    /** Drawn into the Regulator mid-way: nothing more of this to play. */
    void taken() {
        her = null;
        scene = null;
        leaveAt = -1;
    }

    void tick(MinecraftServer server) {
        long now = StoryDirector.now();
        if (startAt >= 0 && now >= startAt) {
            EntityPlayerMP p = StoryDirector.player(who);
            if (p == null || !p.isEntityAlive()) {
                return;
            }
            if (story.talking(p) || story.setPiece()) {
                startAt = now + 20;
                return;
            }
            startAt = -1;
            StoryData data = story.data();
            if (!data.seen(AFTER)) {
                data.beats.add(AFTER);
                data.markDirty();
            }
            if (snub) {
                snub = false;
                snubbed(p);
            } else if (kind == Kind.STRIPPED) {
                home(p);
            } else {
                arrive(p);
            }
            return;
        }
        EntityNorse d = Oath.alive(her);
        if (d == null) {
            her = null;
            return;
        }
        if (scene != null && scene.isOver()) {
            scene = null;
        }
        if (scene == null && kind != Kind.STRIPPED) {
            // Walked up to her: that's talking to her, whatever they meant by it.
            EntityPlayerMP p = StoryDirector.player(who);
            if (p != null && p.isEntityAlive() && p.world == d.world && p.getDistanceSq(d) < NEAR * NEAR && !story.talking(p)) {
                talkTo(p, d);
                return;
            }
        }
        if (scene == null && leaveAt >= 0 && now >= leaveAt) {
            leaveAt = -1;
            StoryDirector.shout(d, DEATH, d.getEntityId(), "Okay. Well. That was... okay. Bye.", 32D);
            Obsession.vanish(d);
            her = null;
        }
    }

    // ------------------------------------------------------------------ sent home with nothing

    private void home(EntityPlayerMP player) {
        Talk t = scene = new Talk(player).reach(32D);
        t.direction("You're back where he took you from. Your hands are empty. Your pockets are empty. Everything you were is gone, and the world goes on around you as if it never noticed.")
                .ask(PARTY, null, 20 * 40, 1,
                        say("Curse her name.", c -> {
                            EntityNorse d = her = appear(player, NorseBoxes.TEMPTRESS);
                            d.setRot(0);
                            c.direction("You say her name, and not kindly. Then you say it again, louder, with a few other things after it. The last word isn't out of your mouth before someone behind you sighs, long and put-upon.")
                                    .run(() -> pose(NorsePose.REGARD))
                                    .line(DEATH, d, "Oh. It's you.")
                                    .line(DEATH, d, "I heard my name and I thought, no. No, it couldn't be. They wouldn't. Not after everything. And yet.")
                                    .direction("She looks wonderful. Rested. The rot is gone off her as if it never was, and she doesn't look at you so much as near you, the way you'd look at a stranger talking to themselves in the street.")
                                    .line(DEATH, d, "Shouting for me in the middle of nowhere. With your little empty hands. Do you know how that looks?")
                                    .line(DEATH, d, "I'm seeing someone, actually. Since you ask.")
                                    .direction("You didn't ask.");
                            rub(c, d);
                        }),
                        act("(Say nothing.)", c -> c.direction("You keep it behind your teeth. Whatever she is to anyone now, you're not going to be the one standing in a field shouting for her.")
                                .run(() -> {
                                    // Nobody ignores her. She'll give it a while, so it doesn't look like she's coming because of it.
                                    snub = true;
                                    startAt = StoryDirector.now() + SNUB;
                                })));
        t.abandoned(this::walkedOff);
        story.talk(player, t);
        story.drawFellows(t, player);
    }

    /** They said nothing, and it got to her: she comes anyway, to make sure they know she doesn't care. */
    private void snubbed(EntityPlayerMP player) {
        EntityNorse d = her = appear(player, NorseBoxes.TEMPTRESS);
        d.setRot(0);
        Talk t = scene = new Talk(player).reach(32D);
        t.direction("It takes her a while. Long enough that you'd almost stopped waiting, if you were waiting. Then someone behind you clears their throat, pointedly.")
                .run(() -> pose(NorsePose.REGARD))
                .line(DEATH, d, "Oh. Hi. Wow. You're still here.")
                .line(DEATH, d, "I was passing. I pass a lot of places. It's sort of the job. Don't read into it.")
                .direction("She looks wonderful. Rested. The rot is gone off her as if it never was, and she doesn't look at you so much as near you, the way you'd look at a stranger talking to themselves in the street.")
                .line(DEATH, d, "I could feel you, you know. Standing about out here, all quiet and meaningful, NOT saying my name. Very loudly.")
                .line(DEATH, d, "The brooding. The silent treatment. Like you're in a film. Do you think I don't know what that is? I invented what that is.")
                .line(DEATH, d, "It's a bit much, honestly. It's a bit stalkery. You just standing here, thinking about me. I can tell. It's all over you.")
                .line(DEATH, d, "And it's not going to work, whatever it is. I'm seeing someone.");
        rub(t, d);
        t.abandoned(this::walkedOff);
        story.talk(player, t);
        story.drawFellows(t, player);
    }

    /** Him, and everything he has; and what they've lost; and then whatever they've got to say about it. */
    private void rub(Talk t, EntityNorse d) {
        thanos(t, d);
        t.run(() -> pose(NorsePose.REGARD))
                .line(DEATH, d, "What happened to all your stuff? Did you lose all your stuff? Oh, that's sad. That's genuinely so sad.");
        callOut(t, d, 0, new boolean[4]);
    }

    /** Whatever they say to her: and the weirder she finds them for it. Two, and she's had enough. */
    private void callOut(Talk t, EntityNorse d, int asked, boolean[] used) {
        if (asked >= 2) {
            weirdo(t, d);
            return;
        }
        List<Talk.Choice> choices = new ArrayList<>();
        if (!used[0]) {
            choices.add(say("You used me.", c -> {
                used[0] = true;
                c.line(DEATH, d, "Used you? Sweetheart, you have to be worth something to be used. I borrowed you. Briefly. And I gave you back.")
                        .line(DEATH, d, "Mostly in one piece. Less stuff, but that's not on me.");
                callOut(c, d, asked + 1, used);
            }));
        }
        if (!used[1]) {
            choices.add(say("You said you loved me.", c -> {
                used[1] = true;
                c.line(DEATH, d, "Did I? I say a lot of things. I said them to a hand with six stones on it. Is that you? Have you got six stones on you right now?")
                        .direction("She makes a show of looking you up and down.")
                        .line(DEATH, d, "No. No, you haven't.");
                callOut(c, d, asked + 1, used);
            }));
        }
        if (!used[2]) {
            choices.add(say("What does he have that I don't?", c -> {
                used[2] = true;
                c.line(DEATH, d, "I literally just told you. I went through it. In order. Were you not listening?")
                        .line(DEATH, d, "You were never very good at listening. Unless I was telling you who to kill. You were great at that bit.");
                callOut(c, d, asked + 1, used);
            }));
        }
        if (!used[3]) {
            choices.add(say("Come back.", c -> {
                used[3] = true;
                c.run(() -> pose(NorsePose.RECOIL))
                        .line(DEATH, d, "Come back. Come BACK? To what? You haven't got anything. You haven't got shoes.")
                        .line(DEATH, d, "Okay, no, this is getting a little uncomfortable.");
                callOut(c, d, asked + 1, used);
            }));
        }
        choices.add(act("(Let her go.)", c -> c.line(DEATH, d, "...Right. Good. That's the first normal thing you've done since I got here.")
                .direction("She goes without saying goodbye, the way you'd leave a party that's gone strange.").run(() -> leave(d, "...weirdo."))));
        t.ask(PARTY, d, 20 * 30, choices.size() - 1, choices.toArray(new Talk.Choice[0]));
    }

    private void weirdo(Talk t, EntityNorse d) {
        t.run(() -> pose(NorsePose.RECOIL))
                .line(DEATH, d, "Okay. I'm going to stop you there, because honestly, this is getting a little uncomfortable.")
                .line(DEATH, d, "You can't just shout people's names into the dark when they've moved on. It's weird. People notice. I noticed.")
                .line(DEATH, d, "Don't call me again. I mean it. If I hear my name in your voice one more time, I'll have to tell him about you, and he'll want to meet you, and you do not want him to want to meet you.")
                .direction("She goes without saying goodbye, the way you'd leave a party that's gone strange: quickly, already telling somebody about it.")
                .run(() -> leave(d, "...so weird."));
    }

    // ------------------------------------------------------------------ home with the court in their pocket

    private void arrive(EntityPlayerMP player) {
        EntityNorse d = her = appear(player, -1);
        leaveAt = StoryDirector.now() + WINDOW;
        Talk t = scene = new Talk(player).reach(32D);
        t.direction(jar() ? "Death is standing a little way off. Not close. She's looking at the Regulator on you the way you'd look at a dog you didn't know."
                        : "Death is standing a little way off. Not close. She's looking at you the way you'd look at a dog you used to know, that's got a great deal bigger since.")
                .run(() -> pose(NorsePose.REGARD))
                .line(DEATH, d, "...Hi.")
                .line(DEATH, d, jar() ? "Wow. Okay. So you're... that, now." : "Wow. Okay. So they just... let you go. With all of it.")
                .direction("She doesn't come any closer. She doesn't go, either. If there's something to be said, you'll have to walk over and say it.");
        if (jar()) {
            t.direction("And if there's something to be done instead, the Regulator is right there in your hand. She's watching it.");
        }
        story.talk(player, t);
        story.drawFellows(t, player);
    }

    /** Someone lays a hand on her while she is here: true if it was her. */
    boolean interact(EntityPlayerMP player, EntityNorse npc) {
        if (npc != her || kind == Kind.STRIPPED || scene != null) {
            return npc == her;
        }
        talkTo(player, npc);
        return true;
    }

    /** Walked up to, or reached for: she says her piece, from as far off as she can say it. */
    private void talkTo(EntityPlayerMP player, EntityNorse d) {
        leaveAt = -1;
        Talk t = scene = new Talk(player).at(d, 48D).reach(32D);
        t.run(() -> pose(NorsePose.RECOIL))
                .line(DEATH, d, "Don't. Don't come any closer. Just say whatever it is from there.")
                .direction("She stands with her arms folded and her weight on her back foot, ready to go.")
                .line(DEATH, d, "Okay. I'm just going to say it, because somebody has to. You've changed.")
                .line(DEATH, d, jar() ? "You've got Eternity in a jar. You've got the TRIBUNAL in a jar. The whole court, in your pocket, rattling around with your keys."
                        : "You stood in front of the Living Tribunal. HIM. And he looked at you, and he let you go. With everything. Nobody gets let go. Nobody.")
                .line(DEATH, d, "And I know that should be... I know. Everyone wants that. But it's a lot. It's just a lot.")
                .ask(PARTY, d, 20 * 40, 0,
                        say("What's wrong with that?", c -> ick(c, d)),
                        say("I did it for you.", c -> {
                            c.line(DEATH, d, "Ew. Don't say that. Why would you say that? I didn't ASK you to.");
                            ick(c, d);
                        }),
                        act("(Step closer.)", c -> {
                            c.run(() -> pose(NorsePose.RECOIL))
                                    .direction("She steps back exactly as far as you stepped forward.")
                                    .line(DEATH, d, "I said DON'T.");
                            ick(c, d);
                        }),
                        say("You came here. What do you want?", c -> {
                            c.line(DEATH, d, "Honestly? To see if it was true. It's true. Okay. I've seen.");
                            ick(c, d);
                        }));
        t.abandoned(this::walkedOff);
        story.talk(player, t);
        story.drawFellows(t, player);
    }

    /** Why: they used to need her, and now they don't, and she can't stand it. */
    private void ick(Talk t, EntityNorse d) {
        t.line(DEATH, d, "You used to need me. That was the whole thing. You'd die, and I'd be there. You'd be lost, and I'd tell you where. You'd stand there shaking with six stones on and I'd tell you what to do with your hand.")
                .line(DEATH, d, "I liked that. I liked being the one who knew. The one in charge.")
                .line(DEATH, d, "Now I look at you and you're bigger than me. You're bigger than EVERYTHING. And it just... gives me the ick. I'm sorry. I don't make the rules. It just does.")
                .direction("She actually shudders. Not frightened. Worse than frightened. Put off.")
                .ask(PARTY, d, 20 * 40, 3,
                        say("Then I'll give it up. All of it. For you.", c -> {
                            c.line(DEATH, d, "You won't. Nobody gives that up. And even if you did, I'd KNOW you could have had it. That's the part. I'd always know.");
                            seeing(c, d);
                        }),
                        say("We can go back to how it was.", c -> {
                            c.line(DEATH, d, "How it was was you doing what I said. You can't go back to that. " + (jar()
                                    ? "You've got the man who judges universes in your pocket." : "The man who judges universes waved you through.") + " You'd be doing it as a favour.")
                                    .line(DEATH, d, "That's worse. That's so much worse.");
                            seeing(c, d);
                        }),
                        say("You don't get to walk away from me.", c -> lash(c, d)),
                        act("(Let her go.)", c -> c.line(DEATH, d, "...Thanks. That's. Yeah. That's actually really mature of you. Weirdly.")
                                .direction("She goes before you can change your mind, and doesn't look back to see if you have.").run(() -> leave(d, null))));
    }

    /** And there's someone else. */
    private void seeing(Talk t, EntityNorse d) {
        t.direction("She looks off to one side, at nothing, and picks at a thread on her sleeve.")
                .line(DEATH, d, "And honestly, it's not just that. I'm seeing someone.");
        thanos(t, d);
        t.ask(PARTY, d, 20 * 40, 4,
                say("Give me another chance.", c -> {
                    c.line(DEATH, d, "Okay, you're doing the thing. The voice. You're doing the voice.")
                            .line(DEATH, d, "There's no chance. There's not even a little chance. This isn't a chance situation.");
                    last(c, d);
                }),
                say("He's using you.", c -> {
                    c.line(DEATH, d, "Oh my god. Wow. Of course you'd say that.")
                            .line(DEATH, d, "He doesn't use me. I'm in charge with him. That's the whole point. That's what I've been SAYING.");
                    last(c, d);
                }),
                say("I can be like him.", c -> {
                    c.line(DEATH, d, "You can't be like him. He's purple.")
                            .run(() -> pose(NorsePose.RECOIL))
                            .line(DEATH, d, "No. Stop. Don't even think about how you'd do that. I can see you thinking about it.");
                    last(c, d);
                }),
                say("Thanos? THANOS?", c -> lash(c, d)),
                act("(Say nothing.)", c -> {
                    c.line(DEATH, d, "...Okay. The staring. You're doing the staring now. That's not better.");
                    last(c, d);
                }));
    }

    /** The last of it: say something you'll regret, or don't. */
    private void last(Talk t, EntityNorse d) {
        t.ask(PARTY, d, 20 * 40, 1,
                say("You were mine. You SAID you were mine.", c -> lash(c, d)),
                act("(Let her go.)", c -> c.line(DEATH, d, "Good. Okay. Good. I'm going to go now, before this gets any sadder.")
                        .direction("She goes. She doesn't look back.").run(() -> leave(d, null))));
    }

    /** They lose their temper, and she has them filed as the stalker before they've finished the sentence. */
    private void lash(Talk t, EntityNorse d) {
        t.run(() -> pose(NorsePose.RECOIL))
                .line(DEATH, d, "Okay. Whoa. Okay.")
                .line(DEATH, d, jar() ? "Can you hear yourself? You've got the Living Tribunal in your pocket, and you're standing in a field yelling at a girl."
                        : "Can you hear yourself? The Living Tribunal let you walk, and you're standing in a field yelling at a girl.")
                .line(DEATH, d, "This is exactly what I mean. This. This is the ick. You're the ick.")
                .ask(PARTY, d, 20 * 40, 4,
                        say("I know where you go. I'll find you.", c -> c.line(DEATH, d, "...Okay, that's actually scary. That's a scary thing to say. I'm going to remember you said that.")
                                .line(DEATH, d, "Do you know how many people sounded exactly like you right now? I've held every one of them. They all said that too.")),
                        say("After everything I did for you!", c -> c.line(DEATH, d, "Everything you did for ME? You killed half of everything because I asked nicely.")
                                .line(DEATH, d, "Is that supposed to be romantic? That's a LOT. Do you hear how that sounds? Out loud?")),
                        say("You manipulated me.", c -> c.line(DEATH, d, "I mean. Yeah? And you let me. A lot. Every single time.")
                                .line(DEATH, d, "That's not really on me, is it.")),
                        act("(Reach for her.)", c -> c.direction("She's out of your reach so fast it's like she was never in it.")
                                .line(DEATH, d, "DON'T touch me. Oh my god. Do not touch me.")),
                        act("(Say nothing.)", c -> c.direction("You just look at her. Breathing a bit hard.")
                                .line(DEATH, d, "Don't look at me like that. That's worse. Somehow that's worse than the yelling.")));
        t.line(DEATH, d, "I'm going now. Don't follow me. Don't call my name. Don't hang around anywhere I might be. If I see you at a graveside, I'm turning round and leaving.")
                .line(DEATH, d, jar() ? "And if I feel your little Regulator anywhere near me, I'm telling him. He's very protective. He'd love an excuse."
                        : "And if I so much as feel you anywhere near me, I'm telling him. He's very protective. He'd love an excuse.")
                .direction("She goes the way you'd leave a party that's gone strange: quickly, without looking back, already telling somebody about it.").run(() -> leave(d, "...so weird."));
    }

    // ------------------------------------------------------------------ him

    /** Thanos: everything he has, that they don't. */
    private void thanos(Talk t, EntityNorse d) {
        t.run(() -> pose(NorsePose.PALM))
                .line(DEATH, d, "Thanos. You won't have heard of him. He's from Titan. Well, he was. There isn't a lot of Titan left. That's sort of his thing.")
                .line(DEATH, d, "He has a ship the size of a city, with a throne on it. He sits on it and thinks about me. You had a bed you kept dying a long way from.")
                .line(DEATH, d, "He has children. Adopted. Every one of them would die for him, and when he asks, they do. Even your gold friend couldn't stand you, at the end.")
                .line(DEATH, d, "He's going after the stones himself. Nobody has to stand next to him for an hour whispering about which way to close his hand.")
                .line(DEATH, d, "And he wants the other half. You stopped at half. He thinks that was a nice start.")
                .line(DEATH, d, "He's tall. He's very tall. He's purple, which I didn't think was going to work for me, and it really, really does.")
                .line(DEATH, d, "He has never once asked me a question. Not one. He just listens.")
                .line(DEATH, d, kind == Kind.STRIPPED ? "And he still has all his things. That's a big one for me, honestly."
                        : jar() ? "And he hasn't got the Tribunal in a jar. He's going to get there, and I get to be the one who tells him how. You just... got there. Where's the fun in that?"
                        : "And nobody's ever let him off anything. He's going to make them, and I get to be the one who tells him how. You just... got waved through. Where's the fun in that?");
    }

    /** Home with him in the Regulator, rather than just let go by him. */
    boolean jar() {
        return kind == Kind.POWERFUL;
    }

    // ------------------------------------------------------------------ helpers

    /** Her, a little way off in front of them: as they last saw her, or in this look. */
    private EntityNorse appear(EntityPlayerMP player, int look) {
        World world = player.world;
        Vec3d dir = player.getLookVec();
        dir = new Vec3d(dir.x, 0D, dir.z);
        dir = dir.lengthSquared() < 1.0E-4 ? new Vec3d(0D, 0D, 1D) : dir.normalize();
        if (look == NorseBoxes.TEMPTRESS) {
            // Behind them: she was there before they finished saying it.
            dir = dir.scale(-1D);
        }
        Vec3d at = ground(world, player.getPositionVector().add(dir.scale(5D)));
        EntityNorse d = EntityNorse.of(world, look >= 0 ? look : NorseBoxes.STRANGER, "aftermath");
        if (look < 0) {
            Devotion.dress(story, d, player);
        }
        d.setLocationAndAngles(at.x, at.y, at.z, 0F, 0F);
        d.face(player.getPositionVector());
        world.spawnEntity(d);
        d.watch(player);
        ((WorldServer) world).spawnParticle(EnumParticleTypes.SMOKE_LARGE, at.x, at.y + 1D, at.z, 30, 0.3D, 0.9D, 0.3D, 0.02D);
        return d;
    }

    private void pose(NorsePose pose) {
        EntityNorse d = Oath.alive(her);
        if (d != null) {
            d.act(pose);
        }
    }

    /** She goes, and gets the last word in, under her breath. */
    private void leave(EntityNorse d, @Nullable String last) {
        Talk t = scene;
        if (t != null && last != null) {
            for (EntityPlayerMP each : t.present()) {
                Obsession.whisper(each, last);
            }
        }
        Obsession.vanish(d);
        her = null;
        leaveAt = -1;
    }

    /** They walked off on her, mid-sentence: she doesn't wait around to be walked off on twice. */
    private void walkedOff() {
        scene = null;
        EntityNorse d = Oath.alive(her);
        if (d != null) {
            StoryDirector.shout(d, DEATH, d.getEntityId(), "Wow. Okay. Cool.", 32D);
            Obsession.vanish(d);
        }
        her = null;
        leaveAt = -1;
    }

    private static Vec3d ground(World world, Vec3d near) {
        for (int dy = 3; dy >= -4; dy--) {
            BlockPos p = new BlockPos(near.x, near.y + dy, near.z);
            if (world.isAirBlock(p) && world.isAirBlock(p.up()) && world.getBlockState(p.down()).isSideSolid(world, p.down(), EnumFacing.UP)) {
                return new Vec3d(p.getX() + 0.5D, p.getY(), p.getZ() + 0.5D);
            }
        }
        return near;
    }

    /** For testing: she comes now, as if they had come home from the Tribunal stripped, let go, or with him in the Regulator. */
    String force(EntityPlayerMP player, String mode) {
        if (Oath.alive(her) != null) {
            return "She's already here.";
        }
        who = player.getUniqueID();
        kind = "powerful".equalsIgnoreCase(mode) ? Kind.POWERFUL : "acquitted".equalsIgnoreCase(mode) ? Kind.ACQUITTED : Kind.STRIPPED;
        snub = "ignored".equalsIgnoreCase(mode);
        startAt = StoryDirector.now();
        return kind == Kind.POWERFUL ? "Death, coming to see what you are now."
                : kind == Kind.ACQUITTED ? "Death, coming to see what they let go."
                : snub ? "Home with nothing, having said nothing: she comes anyway." : "Home with nothing: curse her, or don't.";
    }
}
