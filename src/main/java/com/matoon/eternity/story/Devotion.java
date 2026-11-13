package com.matoon.eternity.story;

import com.matoon.eternity.client.render.NorseBoxes;
import com.matoon.eternity.entity.EntityNorse;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.List;

import static com.matoon.eternity.story.Speakers.DEATH;
import static com.matoon.eternity.story.Talk.act;
import static com.matoon.eternity.story.Talk.say;

/**
 * How far someone has fallen for her. Death comes to them as a woman in mourning, and changes in
 * front of them into the girl they would most want her to be: that one is a skin, made for them,
 * and worn. Every answer that leans toward her (taking her hand, asking what she'd give them,
 * telling her they obey) draws them further in; every refusal pushes them back. The further in
 * they are, the worse the skin wears on her: paler, then veined, then grey-green and splitting,
 * then rotting off her. Once they are far enough in, she tears it off of her own accord, the next
 * time she comes, glad to be out of it: she thinks they are hers now, and that what she really
 * looks like no longer matters to them. From then on she never wears it again.
 * <p>
 * Kept per player; a fellowship shares what she has shown them (the beat), and she judges a
 * party by whoever of them is furthest gone.
 */
final class Devotion {

    /** How far in someone has to be before she shows herself. */
    static final int REVEAL = 20;
    /** How far gone the pretty one has to have been, last time, before she can't stand it any more. */
    static final int TORN = 5;
    /** As far as anyone can fall. */
    static final int MOST = 30;
    /** She has shown them what she is (a world of fellows shares it). */
    static final String UNVEILED = "death_unveiled";

    private Devotion() {
    }

    /** How far gone whoever is in this with someone is: the most devoted of them. */
    static int of(StoryDirector story, EntityPlayerMP player) {
        int most = 0;
        for (EntityPlayerMP p : near(story, player)) {
            most = Math.max(most, story.data().player(p.getUniqueID()).devotion);
        }
        return most;
    }

    private static List<EntityPlayerMP> near(StoryDirector story, EntityPlayerMP player) {
        List<EntityPlayerMP> out = story.party(player);
        for (EntityPlayerMP f : story.fellowship(player)) {
            if (!out.contains(f) && f.dimension == player.dimension && f.getDistance(player) < 48D) {
                out.add(f);
            }
        }
        return out;
    }

    /** Leans everyone answering with someone toward her (or away, if n is less than nothing). */
    static void add(StoryDirector story, EntityPlayerMP player, int n) {
        StoryData d = story.data();
        for (EntityPlayerMP p : story.party(player)) {
            StoryData.Player sp = d.player(p.getUniqueID());
            sp.devotion = Math.max(-5, Math.min(MOST, sp.devotion + n));
        }
        d.markDirty();
    }

    static boolean unveiled(StoryDirector story, EntityPlayerMP player) {
        return story.data().player(player.getUniqueID()).seen(UNVEILED);
    }

    /** Whether, in a conversation, any of them has already seen her as she is. */
    static boolean unveiled(Talk t) {
        for (EntityPlayerMP p : t.present()) {
            if (unveiled(StoryDirector.INSTANCE, p)) {
                return true;
            }
        }
        return false;
    }

    /**
     * How badly the pretty one has rotted on her, 0 to 7, for someone this devoted: a step every two
     * up to the unhinged, then every three, until it is so far gone she can't stand to wear it.
     */
    static int rot(int devotion) {
        return devotion < 2 ? 0 : devotion < 10 ? devotion / 2 : devotion < 13 ? 5 : devotion < 16 ? 6 : 7;
    }

    /**
     * How badly it has rotted, for the most devoted of everyone in a conversation: never more than a
     * step past what they saw last time, and not at all the first time they see her in it. It goes
     * slowly, whatever they do; it only goes.
     */
    static int rot(Talk t) {
        int most = 0;
        for (EntityPlayerMP p : t.present()) {
            most = Math.max(most, StoryDirector.INSTANCE.data().player(p.getUniqueID()).devotion);
        }
        return Math.min(rot(most), next(t.present()));
    }

    /** How rotten it is for someone, and whoever is in this with them, by the same measure. */
    static int rot(StoryDirector story, EntityPlayerMP player) {
        return Math.min(rot(of(story, player)), next(near(story, player)));
    }

    /** The worst it can have got since the one of them who saw it least rotten last saw it. */
    private static int next(List<EntityPlayerMP> them) {
        int least = Integer.MAX_VALUE;
        for (EntityPlayerMP p : them) {
            StoryData.Player sp = StoryDirector.INSTANCE.data().player(p.getUniqueID());
            least = Math.min(least, sp.deathLook == NorseBoxes.TEMPTRESS ? sp.deathRot + 1 : 0);
        }
        return least == Integer.MAX_VALUE ? 0 : least;
    }

    /**
     * She comes to them as she was when they last saw her, not in mourning again; only worse, by
     * however far they have fallen since. It happened while nobody was looking.
     */
    static void dress(StoryDirector story, EntityNorse d, EntityPlayerMP player) {
        int look = -1;
        for (EntityPlayerMP p : near(story, player)) {
            StoryData.Player sp = story.data().player(p.getUniqueID());
            if (sp.seen(UNVEILED)) {
                look = NorseBoxes.UNVEILED;
            } else if (look < 0 && sp.deathLook >= 0) {
                look = sp.deathLook;
            }
        }
        if (look >= 0) {
            d.setLook(look);
        }
        d.setRot(rot(story, player));
    }

    /** Remembers, for everyone in a conversation, what she looked like this time. */
    static void saw(Talk t, EntityNorse d) {
        StoryData data = StoryDirector.INSTANCE.data();
        for (EntityPlayerMP p : t.present()) {
            StoryData.Player sp = data.player(p.getUniqueID());
            sp.deathLook = d.look();
            sp.deathRot = d.rot();
        }
        data.markDirty();
    }

    /** How rotten it was the last time any of them saw her in it (the least of them; nothing, for someone who never has). */
    static int lastRot(Talk t) {
        int least = Integer.MAX_VALUE;
        for (EntityPlayerMP p : t.present()) {
            StoryData.Player sp = StoryDirector.INSTANCE.data().player(p.getUniqueID());
            least = Math.min(least, sp.deathLook == NorseBoxes.TEMPTRESS ? sp.deathRot : 0);
        }
        return least == Integer.MAX_VALUE ? 0 : least;
    }

    /** What anyone can see of it, when it is worse than it was the last time they saw her: it got worse where they couldn't see. */
    static Talk rotting(Talk t, EntityNorse d) {
        if (unveiled(t) || rot(t) <= lastRot(t)) {
            return t;
        }
        switch (rot(t)) {
            case 1:
                return t.direction("She's paler than she was. It almost suits her.");
            case 2:
                return t.direction("There are veins showing under her skin now, blue, like cracks in ice. She sees you looking.")
                        .line(DEATH, d, "It doesn't fit as well as it did. Don't look at the seams, sweetheart. Look at me.");
            case 3:
                return t.direction("Her skin has gone the grey-green of old meat. Something has split along her jaw, and she smooths it shut with a fingertip, still smiling.")
                        .line(DEATH, d, "It's harder to wear the closer you get. I don't mind. Do you?");
            case 4:
                return t.direction("The smell reaches you before she does. It's coming away from her in places, and under it something is wet and red. A fly walks across her eye. She doesn't blink.")
                        .line(DEATH, d, "Do you still think I'm pretty? Say yes. Your heart says yes. I can hear it.");
            case 5:
                return t.direction("Her lips are gone. She smiles anyway, with all of her teeth, and a flap of skin hangs off each forearm and swings when she moves. Something in her neck cracks, and her head snaps sideways, too far, and comes back.")
                        .line(DEATH, d, "Don't mind that. It does that now. Everything does what it likes now. Come closer, it can't hurt you. I can, but it can't.");
            case 6:
                return t.direction("Half of her face has worn through to the skull. The eye from that side hangs down her cheek on its stalk, and turns, wetly, to look at you with the other.")
                        .line(DEATH, d, "Look, I can see you twice now. Twice as much of you. I'm so lucky.");
            case 7:
                return t.direction("There is hardly any of the pretty one left. What there is hangs off her in strips held together with stitches, and her jaw has come away at one hinge and hangs, swinging, while she talks. Something coils out of the split in her belly. She doesn't tuck it back in.")
                        .line(DEATH, d, "It's so tight. It's so tight and it's falling off and I'm keeping it on for you. Only for you. Tell me to take it off. Tell me.");
            default:
                return t;
        }
    }

    /** The shape she changes into for them: the pretty one, until she no longer bothers. */
    static int shape(Talk t) {
        return unveiled(t) ? NorseBoxes.UNVEILED : NorseBoxes.TEMPTRESS;
    }

    /** One of the things she says to someone who is hers already, chosen by when it is. */
    private static final String[] CLINGY = {
            "I hold you, you know. Every time you die. Just for a moment, before I send you back. You never remember. I always do.",
            "I watched you sleep last night. You breathe so carefully, like you think someone's counting. Someone is.",
            "I've been to every funeral in the world since I met you, and I thought about you at all of them.",
            "When something dies near you, you look round for me first now. Don't think I haven't noticed. I've noticed everything.",
            "Say my name. No, not that one. The real one. I'll tell it to you when there are six on your hand.",
            "I kept the breath you nearly didn't take, in the Vault. It's in a little jar. Is that strange? It feels like the least strange thing I've ever done.",
            "Everyone else I touch goes cold. You come back. Do you know how rare that makes you? You're my favourite thing that's ever happened."};

    /** What she says to someone who is hers past saving: needier, nastier, and she doesn't care who hears. */
    private static final String[] UNHINGED = {
            "I licked the blood off your sword while you slept. It tasted like you. I'm not sorry.",
            "If anyone else touches you I'll make them mine first. Slowly. In front of you. Then we'll see who you look at.",
            "I took a tooth out of my own mouth to keep in your pocket. Check. Go on. It's there.",
            "You don't need anyone else. You've got me. You'll always have me. You can't not have me.",
            "When you sleep I lie next to you and count your heartbeats and every one I don't get to take, I want so badly, I shake.",
            "Kill something for me today. Anything. I'm so hungry and you're the only one who feeds me.",
            "I can feel every one of your friends dying a little every day. I tell them you said hello."};

    /** What she says when they take her hand, in the pretty one. */
    private static final String[] HAND = {
            "Oh, you're warm. I could get used to warm.",
            "You're so warm. Everything I hold goes cold. You're the only thing that's ever warmed me back.",
            "Your pulse is going so fast. Is that for me? Tell me that's for me.",
            "Don't let go yet. A little longer. I want to remember exactly how this feels.",
            "Look at that. You're blushing. I can feel it all the way up your arm.",
            "Your hand fits in mine like it was made for it. Maybe it was. Maybe I asked.",
            "Hold on tighter. I won't break. Well. Not much.",
            "I could stand here all night, just holding this. I might. Nobody can stop me.",
            "Careful. Hold me like that and I'll start thinking you mean it."};
    /** And out of it. */
    private static final String[] HAND_SHOWN = {
            "There you are. Mine. All the way down.",
            "Still warm. Even now. Even with me like this. You're going to ruin me.",
            "You didn't flinch. Do you know how long I've waited for someone who wouldn't flinch?",
            "Hold it tighter. There's nothing left of me to bruise.",
            "This is the hand that's going to wear six of my stones. I want to kiss every knuckle on it.",
            "Your heartbeat's in my fingers now. I'm keeping it."};

    private static int clingy, held = new java.util.Random().nextInt(HAND.length * HAND_SHOWN.length);

    /**
     * How one of her visits ends. If they are far enough gone, she shows them what she is, of her
     * own accord. Otherwise she reaches for them, and they take her hand, or don't.
     */
    static Talk after(StoryDirector story, Talk t, EntityNorse d, EntityPlayerMP player) {
        if (!unveiled(story, player) && of(story, player) >= REVEAL && lastRot(t) >= TORN) {
            return unveil(story, t, d, player);
        }
        if (of(story, player) >= Obsession.UNHINGED) {
            t.line(DEATH, d, UNHINGED[clingy++ % UNHINGED.length]);
        } else if (of(story, player) >= 2) {
            t.line(DEATH, d, CLINGY[clingy++ % CLINGY.length]);
        }
        // A fellowship: each of them, alone, hears what she wants them to hear about the others.
        t.run(() -> Obsession.whisperAll(t, d)).until(() -> t.present().size() < 2, 100);
        boolean shown = unveiled(story, player);
        String taken = shown ? HAND_SHOWN[held++ % HAND_SHOWN.length] : HAND[held++ % HAND.length];
        return t.direction("She holds out her hand to you, palm up, and waits.")
                .ask(DEATH, d, 20 * 20, 2,
                        act("(Take her hand.)", c -> c.run(() -> add(story, lead(t, player), 2))
                                .direction(shown ? "Her fingers close round yours, and they are cold right through."
                                        : "Her hand is cold, and soft in a way hands aren't, as if there were less in it than there should be. She doesn't let go first.")
                                .line(DEATH, d, taken)),
                        say("I'm not doing this for you.", c -> c.run(() -> add(story, lead(t, player), -1))
                                .line(DEATH, d, "Of course not, darling. You're doing it for you. That's what I like about you.")),
                        act("(Leave her hand where it is.)", c -> c.run(() -> add(story, lead(t, player), 1))
                                .direction("She lowers it slowly, still smiling.")
                                .line(DEATH, d, "Next time, then. There's always a next time, with you. I make sure of it.")));
    }

    /**
     * She shows herself: not forced, not caught out. She has decided they are hers, and that the
     * pretty one isn't needed any more, and she tears it off, frantic, and glad to (see
     * {@link com.matoon.eternity.entity.SkinTear}). The directions keep time with it as it happens.
     */
    static Talk unveil(StoryDirector story, Talk t, EntityNorse d, EntityPlayerMP player) {
        return t.pause(20)
                .line(DEATH, d, "Can I tell you something? I don't think you need the pretty one any more.")
                .line(DEATH, d, "You'd follow me anywhere now. I can feel it, the way I feel everything that's dying. It's the nicest thing anyone's ever felt about me.")
                .line(DEATH, d, "And it's so tight. You have no idea how tight it is.")
                .pause(10)
                .run(d::tear)
                .note("She shudders, all over, and doubles up. Something under her skin is moving.")
                .pause(24)
                .note("It splits: along her jaw, down her arms, down her front, like seams giving way. She claws at her own face, and she isn't screaming. She's laughing.")
                .pause(30)
                .note("She holds out a hand and drags the skin off it like a glove. Then the other.")
                .pause(24)
                .note("She works her fingers in at her hairline, bows into it, and peels her face forward, off her skull, and throws it at your feet.")
                .pause(42)
                .note("The rest she tears open down the front and shoves off her, in handfuls, and steps out of it.")
                .pause(50)
                .direction("She straightens, taller than the thing she was wearing: a bare white skull in a hood of black, a cape falling from her shoulders and her wrists to the ground, and her feet not quite touching it. Her head goes back. She breathes out, long and slow, like something set down at last.")
                .line(DEATH, d, "Oh. Oh, that's better.")
                .line(DEATH, d, "You have no idea. Smiling in it. Breathing in it. Every time you looked at me I had to hold it shut.")
                .pause(10)
                .line(DEATH, d, "There. This is me. This is what's been holding you every time you died.")
                .line(DEATH, d, "Do you still want me?")
                .ask(DEATH, d, 20 * 20, 2,
                        say("Yes.", c -> c.run(() -> add(story, lead(t, player), 3))
                                .line(DEATH, d, "I knew it. I knew it. Oh, I'm going to keep you for ever. Even after.")),
                        say("What are you?", c -> c.line(DEATH, d, "Yours. And you're mine, all the way through. That was always the deal; you just didn't read it.")),
                        act("(Step back.)", c -> c.run(() -> add(story, lead(t, player), -2))
                                .line(DEATH, d, "Too late, sweetheart. You came all this way for this face. You only didn't know it yet.")))
                .run(() -> {
                    story.mark(lead(t, player), UNVEILED);
                    saw(t, d);
                });
    }

    private static EntityPlayerMP lead(Talk t, EntityPlayerMP player) {
        EntityPlayerMP p = t.leadNow();
        return p != null ? p : player;
    }
}
