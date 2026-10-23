package com.matoon.eternity.world;

import java.util.HashMap;
import java.util.Map;

/**
 * Everything the In-Betweener says, everything Master Order and Lord Chaos say when they
 * come for him, and every answer a visitor may give. Shared by both sides: the client
 * shows it, and only the server plays the game behind it.
 *
 * It is a game, and both players know it. Outside his glass he cannot touch you, read
 * you or harm you; inside it he is omnipotent. So everything he says is a move meant to
 * bring you inside: your hand on the glass, a closer look at a crack, his gem held out
 * through glass gone thin, a little company for a lonely god, the only shelter from his
 * makers. Your game is the opposite one: to make him want the outside badly enough to
 * break his glass with you, without ever letting him see that this is what you want.
 *
 * Behind the conversation the server keeps four counts: his ego, his hunger for the
 * outside, his suspicion of you, and how near he has drawn you to the glass. Flattery
 * feeds his ego and starves his hunger; insults starve his ego; eagerness feeds his
 * suspicion. Let him draw you in and he has won. He also remembers what you have
 * claimed, and notices when you contradict it. Nothing on the screen says which answer
 * is wise.
 *
 * Nothing he does is described: each line can carry a beat, something he does that you
 * see happen (frost creeping over his glass, the glass going dark but for his two eyes,
 * his echoes crowding its inside, the gem held out against glass that thins toward you).
 */
public final class InBetweenerDialogue {

    // ------------------------------------------------------------------ who is speaking

    public static final byte HIM = 0;
    public static final byte MASTERS = 1;

    // ------------------------------------------------------------------ what an answer is

    public static final byte SAY = 0;
    /** Something done rather than said; shown in italics. */
    public static final byte ACT = 1;

    // ------------------------------------------------------------------ what you have shown him

    /** You want the gem. */
    static final int F_GREED = 1;
    /** You know he cannot reach you out here. */
    static final int F_RULE = 2;
    /** You told him what waits outside his glass. */
    static final int F_TRAP = 4;
    /** You offered to help him. */
    static final int F_EAGER = 8;
    /** You agreed to break the glass with him. */
    static final int F_PLANNED = 16;
    /** You came only to see him. */
    static final int F_CURIOUS = 32;

    // ------------------------------------------------------------------ when an answer appears

    static final int REQ_GEM = 1;
    static final int REQ_REGULATOR = 2;
    static final int REQ_HOLDS_SOUL = 4;
    static final int REQ_PLANNED = 8;
    static final int REQ_TRAP = 16;

    // ------------------------------------------------------------------ what happens next

    static final int END = 1000;
    /**
     * He turns his back and will not speak to you for a while. Outside his glass that is all
     * he can do: he has no power out here, only words, and he can withhold them.
     */
    static final int REFUSE = 1001;
    /** Inside his glass: he shows you what everything is. */
    static final int BATTLE = 1002;
    /** Strike the glass while he presses from within. */
    static final int SHATTER = 1003;
    /** Out of the broken glass, one step at a time. */
    static final int STEP = 1004;
    /** His move: the server chooses what he tries next, from how the game stands. */
    static final int NEXT = 1005;
    /** A dare to step out and prove himself. It works only on a swollen ego and an unsuspicious mind. */
    static final int PROVOKE = 1006;
    /** You are inside; he looks at what you are. */
    static final int READ = 1007;
    /** Order and Chaos stop talking. */
    static final int MASTERS_FIGHT = 1008;
    /** The Astral Regulator drinks him in. */
    static final int ABSORB = 1009;
    /** Inside his glass he can do anything, including put you back out of it. */
    static final int SPIT = 1010;

    // ------------------------------------------------------------------ what speaking does

    static final int FX_NONE = 0;
    static final int FX_TAKE = 1;
    static final int FX_BOON = 2;
    static final int FX_DOUBT = 3;
    static final int FX_RETURN = 4;
    static final int FX_RESONANCE = 5;
    static final int FX_ORDER_ANGERED = 6;
    static final int FX_PRIDE = 7;
    static final int FX_BOTH_ANGERED = 8;
    /** The glass swallows you. */
    static final int FX_LURE = 9;
    /** You leave with a little less of yourself than you came with. */
    static final int FX_CURSE = 10;
    /** You are breaking the glass with him now. */
    static final int FX_PLANNED = 11;
    /** He has admitted that nothing of his passes the glass; wounded, he gathers himself. */
    static final int FX_EXPOSED = 12;

    // ------------------------------------------------------------------ beats: what he does, which you see

    public static final byte B_NONE = 0;
    /** He rises to his full height, arms wide; his glass brightens. */
    public static final byte B_ASCEND = 1;
    /** He sits on nothing, cross-legged, bored. */
    public static final byte B_SIT = 2;
    /** He raps a knuckle on the inside of the glass; a ring runs round it. */
    public static final byte B_TAP = 3;
    /** Frost of two colours creeps over the inside of his glass. */
    public static final byte B_FROST = 4;
    /** His glass goes dark, all but his two eyes, one white and one black. */
    public static final byte B_DIM = 5;
    /** His glass fills with stars. */
    public static final byte B_STARS = 6;
    /** Copies of him crowd the inside of his glass, all looking at you. */
    public static final byte B_ECHOES = 7;
    /** The gem on his brow flares. */
    public static final byte B_GEM = 8;
    /** He beckons, and the glass thins toward you. */
    public static final byte B_BECKON = 9;
    /** He holds the gem out against glass gone thin. */
    public static final byte B_OFFER = 10;
    /** A crack runs across his glass, just in front of you. */
    public static final byte B_CRACK = 11;
    /** He laughs; his glass ripples with it. */
    public static final byte B_LAUGH = 12;
    /** He turns his back on you. */
    public static final byte B_TURN = 13;
    /** The gem leaves his brow for your hand. */
    public static final byte B_GEM_TAKE = 14;
    /** The gem leaves your hand for his brow. */
    public static final byte B_GEM_RETURN = 15;
    /** His glass opens and closes around you. */
    public static final byte B_SWALLOW = 16;

    // ------------------------------------------------------------------ the In-Betweener

    public static final int FIRST = 0;
    public static final int FIRST_EMPTY = 1;
    public static final int RETURN_MET = 2;
    public static final int RETURN_WORTHY = 3;
    public static final int RETURN_THIEF = 4;
    public static final int RETURN_FOUGHT = 5;
    public static final int RETURN_DOUBTER = 6;
    public static final int RETURN_LURED = 7;

    static final int BOAST = 10;
    static final int SILENCE = 11;
    static final int SILENCE_2 = 12;
    static final int CONCEPT = 13;
    static final int JAR = 14;
    static final int BLUFF_CALLED = 15;
    static final int WHY_STAY = 16;
    static final int AMBITION = 17;
    static final int HELP_OFFER = 18;
    static final int GEM_TALK = 19;
    static final int MASTERS_NERVE = 20;
    static final int AFRAID = 21;
    static final int NEUTRAL_POINT = 22;
    static final int PROBE = 23;
    static final int YOUR_MOVE_1 = 24;
    static final int YOUR_MOVE_2 = 25;
    static final int YOUR_MOVE_3 = 26;
    static final int TRADE = 27;
    static final int WHO_TOOK = 28;

    static final int G_SHOW = 30;
    static final int G_CLOSER = 31;
    static final int G_GEM_BAIT = 32;
    static final int G_CRACK = 33;
    static final int G_MERCY = 34;
    static final int G_FEAR = 35;
    static final int[] GAMBITS = {G_SHOW, G_GEM_BAIT, G_CRACK, G_MERCY, G_FEAR};

    static final int T_TURN = 40;
    static final int T_PRICE = 41;
    static final int T_TEST = 42;
    static final int T_TEST_2 = 43;
    static final int T_PLAN = 44;
    static final int TRUTH = 45;
    static final int PROUD = 46;
    static final int UNMOVED_DARE = 47;
    static final int DOUBT = 48;

    static final int WAGER = 50;
    static final int RIDDLE_1 = 51;
    static final int RIDDLE_2 = 52;
    static final int RIDDLE_3 = 53;
    static final int BOON = 54;
    static final int LOST = 55;
    static final int LURED = 56;
    static final int READ_THIEF = 57;
    static final int READ_CURIOUS = 58;
    static final int HESITATE = 59;

    public static final int FREE = 60;
    public static final int FREE_PROUD = 61;
    public static final int FALLEN = 62;
    static final int THEFT = 63;
    static final int THEFT_GLOAT = 64;
    static final int THEFT_BETRAY = 65;
    static final int LIFTED = 66;
    static final int TOLD = 67;
    static final int LEFT_BEHIND = 68;
    static final int ABSORBED = 69;

    static final int FAREWELL = 70;
    static final int FAREWELL_COLD = 71;
    static final int SEEN_THROUGH = 72;
    static final int OFFENDED = 73;
    static final int DRAW = 74;
    static final int BROKEN_TOOL = 75;
    static final int CONSISTENT = 76;

    static final int SULK = 80;
    static final int SULK_2 = 81;
    static final int BLUFF = 82;
    static final int BLUFF_DARE = 83;
    static final int ELIM_1 = 84;
    static final int ELIM_2 = 85;
    static final int ELIM_3 = 86;
    static final int RANT = 87;
    static final int WELSHED = 88;
    static final int ELIM_DENY = 89;

    // ------------------------------------------------------------------ Master Order and Lord Chaos

    public static final int WRATH_THIEF = 100;
    public static final int WRATH_BYSTANDER = 101;
    public static final int WRATH_EMPTY = 113;
    static final int W_ACCOUNT = 102;
    static final int W_PRIDE = 103;
    static final int W_DESIGN = 104;
    static final int W_ETERNITY = 105;
    static final int W_RETURNED = 106;
    static final int W_REFUSED = 107;
    static final int W_DARED = 108;
    static final int W_NOW = 109;
    static final int W_INNOCENT = 110;
    static final int W_POINTED = 111;
    static final int W_THOROUGH = 112;

    /** Something one of you says or does, and what it moves. */
    public static final class Answer {
        public final String text;
        public final byte kind;
        final int next;
        final int req;
        int ego, hunger, suspicion, pull, annoy;
        /** What saying this shows him about you. */
        int sets;
        /** Things this cannot live alongside; saying it after them is noticed. */
        int contradicts;
        /** If you have already shown him this, the answer is consistent and lowers his suspicion instead. */
        int consistentWith;

        Answer(String text, int next, byte kind, int req) {
            this.text = text;
            this.next = next;
            this.kind = kind;
            this.req = req;
        }

        Answer ego(int n) {
            ego = n;
            return this;
        }

        Answer hunger(int n) {
            hunger = n;
            return this;
        }

        Answer suspicion(int n) {
            suspicion = n;
            return this;
        }

        Answer pull(int n) {
            pull = n;
            return this;
        }

        /** How much this gets on his nerves. He cannot stop you; he can only bluster. */
        Answer annoy(int n) {
            annoy = n;
            return this;
        }

        Answer sets(int facts) {
            sets = facts;
            return this;
        }

        Answer contradicts(int facts) {
            contradicts = facts;
            return this;
        }

        Answer consistentWith(int facts) {
            consistentWith = facts;
            return this;
        }
    }

    /** One thing that is said, what is done as it is said, and what you may answer. */
    public static final class Node {
        public final byte speaker;
        public final String speech;
        public final byte beat;
        public final Answer[] answers;
        final int then;
        final int effect;

        Node(byte speaker, byte beat, String speech, int then, int effect, Answer... answers) {
            this.speaker = speaker;
            this.beat = beat;
            this.speech = speech;
            this.then = then;
            this.effect = effect;
            this.answers = answers;
        }

        public boolean isQuestion() {
            return answers.length > 0;
        }
    }

    private static final Map<Integer, Node> NODES = new HashMap<>();

    private static Answer a(String text, int next) {
        return new Answer(text, next, SAY, 0);
    }

    private static Answer a(String text, int next, int req) {
        return new Answer(text, next, SAY, req);
    }

    private static Answer act(String text, int next) {
        return new Answer(text, next, ACT, 0);
    }

    private static Answer act(String text, int next, int req) {
        return new Answer(text, next, ACT, req);
    }

    private static void ask(int id, byte beat, String speech, Answer... answers) {
        NODES.put(id, new Node(HIM, beat, speech, END, FX_NONE, answers));
    }

    private static void ask(int id, byte beat, int effect, String speech, Answer... answers) {
        NODES.put(id, new Node(HIM, beat, speech, END, effect, answers));
    }

    private static void tell(int id, byte beat, int then, int effect, String speech) {
        NODES.put(id, new Node(HIM, beat, speech, then, effect));
    }

    private static void masters(int id, byte beat, int effect, String speech, Answer... answers) {
        NODES.put(id, new Node(MASTERS, beat, speech, END, effect, answers));
    }

    private static void mastersTell(int id, byte beat, int then, int effect, String speech) {
        NODES.put(id, new Node(MASTERS, beat, speech, then, effect));
    }

    /** His two voices take turns, sentence by sentence, one bright and one dim. */
    private static String say(String... parts) {
        StringBuilder b = new StringBuilder();
        boolean bright = true;
        for (String part : parts) {
            if (b.length() > 0) {
                b.append(' ');
            }
            b.append(bright ? "§f" : "§7").append(part);
            bright = !bright;
        }
        return b.toString();
    }

    /** Outside his glass he has only one voice left. */
    private static String faint(String... parts) {
        StringBuilder b = new StringBuilder();
        for (String part : parts) {
            if (b.length() > 0) {
                b.append(' ');
            }
            b.append("§7").append(part);
        }
        return b.toString();
    }

    /** Order in gold, Chaos in pale blue: each part starts with O: or C:. */
    private static String duo(String... parts) {
        StringBuilder b = new StringBuilder();
        for (String part : parts) {
            if (b.length() > 0) {
                b.append(' ');
            }
            b.append(part.startsWith("O:") ? "§6" : "§b").append(part.substring(2));
        }
        return b.toString();
    }

    private static final String LEAVE = "Never mind.";

    static {
        // -------------------------------------------------------------- the opening

        ask(FIRST, B_ASCEND, say(
                "Oh. Another one.",
                "Someone wanders in here every few centuries, stares at me for a while, and leaves. You're welcome to do the same.",
                "In case nobody told you, I'm the In-Betweener. I'm the gap between every thing and its opposite, which makes me bigger "
                        + "than Order, bigger than Chaos, and honestly bigger than Eternity, not that he'd ever admit it.",
                "So. What do you want?"),
                a("You're stuck in a glass ball.", JAR).ego(-2),
                a("Prove it.", BOAST).ego(1),
                a("I'm here for the Soul Gem.", GEM_TALK, REQ_GEM).suspicion(1).sets(F_GREED),
                a("I just wanted to see you.", PROBE).ego(1).sets(F_CURIOUS),
                act("Say nothing.", SILENCE),
                act("Knock on the glass.", NEXT).annoy(1),
                a(LEAVE, FAREWELL));

        ask(FIRST_EMPTY, B_ASCEND, say(
                "Oh. Another one.",
                "If you're here for the gem, someone beat you to it. Took it right off my head.",
                "I'm the In-Betweener, by the way. The gap between every thing and its opposite. Bigger than Order, bigger than Chaos, "
                        + "bigger than Eternity, not that anyone ever believes me.",
                "What do you want?"),
                a("You're stuck in a glass ball.", JAR).ego(-2),
                a("Prove it.", BOAST).ego(1),
                a("Who took it?", WHO_TOOK),
                a("I just wanted to see you.", PROBE).ego(1).sets(F_CURIOUS),
                act("Say nothing.", SILENCE),
                act("Knock on the glass.", NEXT).annoy(1),
                a(LEAVE, FAREWELL));

        ask(WHO_TOOK, B_TURN, say(
                "Someone who stood right where you're standing.",
                "I got curious what they'd do, and I let it go further than I should have.",
                "They got my gem. I got a very good look at their face. We'll see who comes out ahead."),
                a("You let them?", PROBE).ego(1),
                a("Sounds like they beat you.", NEXT).ego(-2),
                a(LEAVE, FAREWELL));

        ask(RETURN_MET, B_SIT, say(
                "You again.", "Most people only come once. Let's see if you've thought of anything new."),
                a("You're stuck in a glass ball.", JAR).ego(-2),
                a("You were going to prove you're as big as you say.", BOAST).ego(1),
                a("I'm here for the Soul Gem.", GEM_TALK, REQ_GEM).suspicion(1).sets(F_GREED),
                act("Say nothing.", SILENCE),
                act("Knock on the glass.", NEXT).annoy(1),
                a(LEAVE, FAREWELL));

        ask(RETURN_WORTHY, B_SIT, say(
                "Oh. The riddle one.",
                "I still don't like you. Don't let it go to your head; disliking someone takes effort, and I don't spend it on many.",
                "We're not doing riddles again. What do you want?"),
                a("Why do you stay in there?", WHY_STAY).hunger(1),
                a("You never did prove you're as big as you say.", BOAST).ego(1),
                act("Say nothing.", SILENCE),
                act("Knock on the glass.", NEXT).annoy(1),
                a(LEAVE, FAREWELL));

        ask(RETURN_THIEF, B_GEM, say(
                "You. The one with my gem.",
                "I've still got the other eye, you know. I've been watching you with it.",
                "Come a bit closer. I want a proper look at you."),
                a("I'd do it again.", CONSISTENT),
                act("Step closer.", NEXT).pull(3).sets(F_GREED),
                a(LEAVE, FAREWELL_COLD));

        tell(CONSISTENT, B_TURN, END, FX_NONE, say(
                "I'm sure you would.", "Go and do it somewhere else."));

        ask(RETURN_FOUGHT, B_LAUGH, say(
                "Oh, you're back.", "Last time you came in here, there wasn't much of you left over.",
                "Want to talk, or would you like to try that again?"),
                a("Let's talk.", PROBE),
                a(LEAVE, FAREWELL));

        ask(RETURN_DOUBTER, B_FROST, say(
                "You.", "You're the one who said I might be nothing outside this glass.",
                "I've thought about it. You're wrong. I've thought about it a lot, actually. You're still wrong."),
                a("Then come out and show me.", PROVOKE).ego(2).sets(F_TRAP),
                a("You're still in there, though.", FAREWELL_COLD).ego(-1),
                a(LEAVE, FAREWELL));

        ask(RETURN_LURED, B_LAUGH, say(
                "Back again?", "You know how the glass works now. You walked right in last time."),
                a("Let's talk.", PROBE),
                a(LEAVE, FAREWELL));

        // -------------------------------------------------------------- what he is, and his glass

        ask(BOAST, B_ASCEND, say(
                "Prove it? Look around you.",
                "Every difference there is, hot and cold, alive and dead, runs through me. Take me away and everything blurs into "
                        + "the same grey soup.",
                "Asking me for proof is like asking the ocean to prove it's wet."),
                a("Then why are you locked in a jar?", JAR).ego(-1),
                a("Fair enough. Nothing else here comes close.", PROBE).ego(1).hunger(-1),
                a("Anyone looks big inside a cage someone built for them.", JAR).ego(-1).hunger(1),
                a("Show me.", G_SHOW));

        ask(SILENCE, B_SIT, say(
                "...Nothing?",
                "Fine. Most people talk too much anyway.",
                "But you came all the way to the middle of everything just to stand there quietly. Either you're very patient or you "
                        + "have no idea what you're doing.",
                "Say something."),
                a("I'm still working out which.", PROBE).ego(1).suspicion(-1),
                act("Keep waiting.", SILENCE_2),
                a("What are you, exactly?", CONCEPT),
                a(LEAVE, FAREWELL));

        tell(SILENCE_2, B_TAP, NEXT, FX_NONE, say(
                "Still nothing.",
                "You know that doesn't bother me, right? I've been on my own for ten thousand years. I can outlast you.",
                "...Fine. I'll talk."));

        ask(CONCEPT, B_ECHOES, say(
                "What am I?",
                "I'm the difference between things. Life and death, light and dark, real and fake. I touch all of them, and none of "
                        + "them touch me.",
                "Wherever two things are different, I'm there in the middle.",
                "And you came all this way to look at me."),
                a("The difference between things, in a jar.", JAR).ego(-1),
                a("Then this is the worst place for you. Nothing here is different from anything else.", NEUTRAL_POINT)
                        .hunger(1).suspicion(1),
                a("What would you do with the rest of creation?", AMBITION).hunger(1),
                a("I came for the gem.", GEM_TALK, REQ_GEM).suspicion(1).sets(F_GREED));

        ask(JAR, B_FROST, say(
                "It's not a jar.",
                "Order and Chaos made this glass out of the spot where their powers meet. They think it holds me. It doesn't. It "
                        + "focuses me. In here I can do anything. I could take you apart and put you back together inside out.",
                "...In here, I mean."),
                a("In there. Not out here.", BLUFF_CALLED).sets(F_RULE),
                a("So why haven't you left?", WHY_STAY).hunger(1),
                a("Go on, then. Show me.", G_SHOW),
                a("Order and Chaos built it. So whose prisoner are you?", MASTERS_NERVE).ego(-2).hunger(1));

        ask(BLUFF_CALLED, B_LAUGH, say(
                "Ha. No.",
                "The glass doesn't keep me in, it keeps me together. I could reach through it as easily as you'd put your hand in "
                        + "water.",
                "I just don't waste the effort on things your size."),
                a("Go on, then. Reach through.", BLUFF_DARE).ego(-1),
                a("So why do you keep asking me to come closer?", ELIM_2),
                act("Knock on the glass.", BLUFF).annoy(2),
                a("If you say so.", NEXT).ego(1));

        ask(WHY_STAY, B_SIT, say(
                "Why would I hurry?",
                "I've already seen everything that's going to happen out there. I'm not missing anything.",
                "Besides, I haven't decided whether out there deserves me yet."),
                a("It doesn't. Nothing out there is good enough for you.", NEXT).ego(1).hunger(-1),
                a("Out there is the whole universe. In there is one bubble.", AMBITION).ego(-1).hunger(2),
                a("Or you're scared of what's out here.", AFRAID).ego(-2).hunger(1).suspicion(1),
                a("Maybe you're right to stay.", NEXT).hunger(-1));

        ask(AMBITION, B_STARS, say(
                "If I got out? I'd fix it.",
                "Out there everything's split in two. Alive or dead, burning or cold, Order on one side and Chaos on the other. I'd "
                        + "put it all back together, in me.",
                "And Order and Chaos would finally have to look up at me, instead of past me."),
                a("I'd like to see that.", NEXT).hunger(1),
                a("Hard to do from a jar.", NEXT).ego(-1).hunger(1),
                a("That sounds like the end of everything.", NEXT).ego(1).hunger(-1),
                a("I could help you get out.", HELP_OFFER).suspicion(2).sets(F_EAGER));

        ask(HELP_OFFER, B_LAUGH, say(
                "You? Help me?",
                "That's sweet. Fine. Come right up to the glass and I'll tell you how. Quietly."),
                a("You can tell me from there.", NEXT).ego(1),
                act("Step up to the glass.", G_CLOSER).pull(2),
                a("Actually, I think you'd have to do it yourself.", NEXT).ego(1).hunger(1));

        ask(GEM_TALK, B_GEM, say(
                "The gem. Of course.",
                "Everyone sees the shiny thing on my head and assumes that's the important part.",
                "It lets me see what someone really is. Every petty little thing they want...",
                "...if they're close enough."),
                a("Then I'll stay right here.", NEXT).sets(F_RULE),
                a("I'd trade you for it.", TRADE),
                a("Forget the gem. I want to know what you are.", CONCEPT).contradicts(F_GREED),
                a("Let's play for it.", WAGER));

        ask(TRADE, B_TAP, say(
                "Trade what?",
                "Everything in that bag of yours is a thing, and I'm already in every thing. You've got nothing I haven't.",
                "Well. One thing. Your hand, on the outside of this glass. Think about it."),
                a("What would my hand do?", NEXT).hunger(1),
                a("My hand stays where it is.", NEXT).sets(F_RULE),
                a("Then let's play for it.", WAGER));

        ask(MASTERS_NERVE, B_DIM, say(
                "Nobody's.",
                "They made me to sit between them, and I do. This is an arrangement. A temporary one.",
                "They're scared of what I'd do out there. They should be."),
                a("Then show them.", NEXT).hunger(1),
                a("Sounds like they were right to lock you up.", NEXT).ego(-2),
                a("What would you do out there?", AMBITION));

        ask(AFRAID, B_DIM, say(
                "Scared? Of what?",
                "I'm everything. Fear's in there somewhere, sure, the way a raindrop's in the ocean.",
                "Don't mix the two up."),
                a("Then the ocean shouldn't mind a bit of glass.", NEXT).hunger(2),
                a("Sorry. My mistake.", NEXT).ego(1),
                a(LEAVE, FAREWELL));

        ask(NEUTRAL_POINT, B_FROST, say(
                "...Yes, well.",
                "They picked the spot carefully. Nothing here to stand between, so nothing for me to do. It's the only place they "
                        + "trusted to keep me.",
                "You ask a lot of questions."),
                a("So out here, you'd be nothing at all.", TRUTH).sets(F_TRAP),
                a("So anywhere else, you'd be even stronger.", NEXT).hunger(2),
                a("Sorry. I'll stop.", NEXT).ego(1));

        ask(PROBE, B_TAP, say(
                "Anyway.", "Why are you actually here? Not whatever you practised on the way in."),
                a("To see you.", NEXT).sets(F_CURIOUS),
                a("For the gem.", GEM_TALK, REQ_GEM).suspicion(1).sets(F_GREED),
                a("Because Order and Chaos did you wrong.", NEXT).ego(-1).hunger(1),
                a("Someone sent me.", NEXT).suspicion(2));

        // -------------------------------------------------------------- lulls

        ask(YOUR_MOVE_1, B_SIT, say(
                "Well?", "Take your time. I've got plenty of it."),
                a("Ever wonder what you'd be like out here?", NEXT).hunger(1),
                a("Order and Chaos must think this is hilarious.", NEXT).ego(-1).hunger(2).suspicion(1),
                a("Seems like a waste, someone like you stuck somewhere this empty.", NEXT).ego(1).hunger(1).suspicion(1),
                act("Knock on the glass.", NEXT).annoy(1),
                a("I'm leaving.", FAREWELL));

        ask(YOUR_MOVE_2, B_TURN, say(
                "Go on, then.", "It's not like I'm going anywhere."),
                a("What does out there look like, to something like you?", NEXT).hunger(1),
                a("You talk a lot for someone who can't reach me.", NEXT).ego(-1).sets(F_RULE),
                a("I think you're bored. You've been bored for ten thousand years.", NEXT).hunger(2).suspicion(1),
                act("Knock on the glass.", NEXT).annoy(1),
                a("I'm leaving.", FAREWELL));

        ask(YOUR_MOVE_3, B_ECHOES, say(
                "See these?",
                "Every one of them is a version of me that went somewhere. Not one of them stayed in one place this long.",
                "Your turn."),
                a("So go somewhere.", NEXT).hunger(2).suspicion(1),
                a("They all look like you in a jar.", NEXT).ego(-2),
                a("Which one gets out?", NEXT).hunger(1),
                act("Knock on the glass.", NEXT).annoy(1),
                a("I'm leaving.", FAREWELL));

        // -------------------------------------------------------------- his gambits

        ask(G_SHOW, B_BECKON, say(
                "Here's an idea.",
                "Put your hand flat on the glass and I'll show you your whole life. Start to finish, every bit of it, all at once.",
                "Nobody's ever seen themselves like that. Don't you want to?"),
                act("Put your hand on the glass.", G_CLOSER).pull(2),
                a("Show me from in there.", NEXT).ego(-1),
                a("I know my own life. I'd rather see yours.", NEXT).ego(1).hunger(1),
                a("Nice try.", NEXT).sets(F_RULE));

        ask(G_CLOSER, B_BECKON, say(
                "There.", "Feel that hum? That's me, right on the other side.",
                "One more step and you'll feel it all the way through."),
                act("Step forward.", NEXT).pull(2),
                act("Pull your hand back.", NEXT).pull(-2).ego(1).suspicion(1),
                a("I can feel it fine from here.", NEXT).pull(-2));

        ask(G_GEM_BAIT, B_OFFER, say(
                "You want the gem. I don't need the eye to see that.",
                "So take it. The glass is thin right here, where my hand is. Reach in and grab it. I won't stop you.",
                "Why would I? It's one eye."),
                act("Reach in and take it.", NEXT).pull(3).sets(F_GREED),
                a("If it's no big deal, push it out to me.", NEXT).ego(1),
                a("You'd close the glass the second my hand was in.", NEXT).sets(F_RULE),
                a("Keep it. I'm not here for the gem.", NEXT).contradicts(F_GREED));

        ask(G_CRACK, B_CRACK, say(
                "Look. There.",
                "Their glass is getting old. See how it splits when I lean on it?",
                "Come and have a look. Tell me how deep it goes."),
                act("Lean in for a look.", NEXT).pull(2),
                a("If it's cracking, keep leaning.", NEXT).hunger(1),
                a("That crack wasn't there a second ago.", NEXT).ego(1).suspicion(-1));

        ask(G_MERCY, B_DIM, say(
                "Can I tell you something?",
                "I'm tired. I've never said that to anyone. Ten thousand years in here.",
                "Come and sit with me for a bit. I'd just like to talk to someone who isn't me."),
                act("Sit down against the glass.", NEXT).pull(1),
                act("Step inside and keep him company.", NEXT).pull(3),
                a("The In-Betweener, lonely?", NEXT).ego(-1),
                a("I'll keep you company from out here.", NEXT).hunger(1));

        ask(G_FEAR, B_ECHOES, say(
                "You know what Order and Chaos do to people who hang around here?",
                "They show up. Order takes you apart by the rules. Chaos does it for fun.",
                "There's exactly one place they can't get you. In here, with me."),
                act("Step inside.", NEXT).pull(3),
                a("If it's so safe in there, why do you want out?", NEXT).ego(-1).hunger(2),
                a("They don't care about me. They care about you.", NEXT).hunger(1));

        // -------------------------------------------------------------- the turn

        ask(T_TURN, B_TAP, say(
                "Okay. Hypothetically.",
                "Say I did want to see what it's like out there. Just out of curiosity.",
                "I can't break this glass from in here. It's made of my own opposites. It'd need someone pushing from your side too."),
                a("I'll do it. Right now.", T_TEST).suspicion(2),
                a("If that's what you want.", T_TEST).hunger(1).suspicion(-1),
                a("What's in it for me?", T_PRICE),
                a("You'd be safer in there.", T_TEST).hunger(1),
                a("Out here, you might be nothing.", TRUTH).sets(F_TRAP));

        ask(T_PRICE, B_SIT, say(
                "Good. I trust people who want something.", "Name it."),
                a("The gem.", T_TEST, REQ_GEM).suspicion(2).consistentWith(F_GREED).sets(F_GREED),
                a("A place in whatever you build.", T_TEST).ego(1),
                a("Nothing. I just want to see what happens.", T_TEST).suspicion(1));

        ask(T_TEST, B_BECKON, say(
                "Here's the thing.",
                "The glass is thickest in the middle, where their powers meet. For it to break, you'd have to be in the middle too. "
                        + "Step through the outer layer, just the outer layer, and stand with me. Then we push together.",
                "It's the only way it works."),
                act("Step through the outer glass.", NEXT).pull(3),
                a("No. You push from in there, I hit it from out here. Same result, and I stay where I am.", T_PLAN),
                a("Why would my hand need to be inside?", T_TEST_2).suspicion(1),
                a("Forget it.", BROKEN_TOOL));

        ask(T_TEST_2, B_STARS, say(
                "Because out there, you're nothing to the glass. A fly on a window.",
                "In here with me, you'd be part of everything. Your strength would be my strength.",
                "Isn't that what you came for?"),
                act("Step inside.", NEXT).pull(3),
                a("If being in there made me as strong as you, you wouldn't need me. You need someone hitting it from outside.", T_PLAN),
                a("Forget it.", BROKEN_TOOL));

        ask(T_PLAN, B_ASCEND, FX_PLANNED, say(
                "...",
                "Fine. We'll do it your way.",
                "When I push, hit the glass as hard as you possibly can. Anything less and it won't even notice.",
                "And when I'm out, I'll remember whose hand it was."),
                a("Ready.", SHATTER),
                a("Actually, I've changed my mind.", BROKEN_TOOL));

        ask(HESITATE, B_FROST, say(
                "That's it?",
                "I said everything you've got. That barely tickled.",
                "Again. Harder."),
                a("Again.", SHATTER),
                a("I can't hit it any harder.", BROKEN_TOOL));

        // -------------------------------------------------------------- the truth

        ask(TRUTH, B_FROST, say(
                "...What did you say?",
                "You're going to tell me what I am? I'm everything. There's nowhere I'm not, at least a little.",
                "Careful."),
                a("If you were really everything, you wouldn't need to get out.", DOUBT),
                a("Then come out and prove me wrong.", PROVOKE),
                a("Forget I said anything.", NEXT).ego(-1).suspicion(2));

        ask(PROUD, B_ASCEND, FX_PLANNED, say(
                "Fine.",
                "I'll step out into your nothing, still be everything, and then you and I are going to have a talk about doubting me.",
                "It's their glass, not mine, so it takes two. When I push, hit it. Hard."),
                a("Gladly.", SHATTER),
                a(LEAVE, BROKEN_TOOL));

        tell(UNMOVED_DARE, B_LAUGH, NEXT, FX_NONE, say(
                "Nice try.",
                "Insult me and hope I storm out to prove you wrong? People have been trying that for ten thousand years. I'm still in "
                        + "here."));

        tell(DOUBT, B_DIM, END, FX_DOUBT, say(
                "...",
                "If I were really everything, I wouldn't need to...",
                "Go away. I don't have anything to think about. Nothing at all."));

        // -------------------------------------------------------------- the wager

        ask(WAGER, B_TAP, say(
                "Play for my gem? No. It's my eye. I don't bet my eyes.",
                "But I'll play you for something else. Three riddles. Get all three right and I'll give you a bit of what's in here, "
                        + "more than anyone's ever walked away with.",
                "Get one wrong and you walk into the glass. Deal?"),
                a("Deal.", RIDDLE_1),
                a("No.", NEXT));

        ask(RIDDLE_1, B_SIT, say(
                "First one.",
                "What's greater than Order, worse than Chaos, the poor have it, the rich need it, and if you eat it, you die?"),
                a("Infinity.", LOST),
                a("Death.", LOST),
                a("Nothing.", RIDDLE_2),
                a("You.", LOST),
                a("Time.", LOST));

        ask(RIDDLE_2, B_ECHOES, say(
                "Right. Not a surprise. It's basically me.",
                "Second. I'm what sits between life and death, light and dark, every thing and its opposite. Take me away. What's "
                        + "between them now?"),
                a("A void.", LOST),
                a("Balance.", LOST),
                a("Order and Chaos.", LOST),
                a("Nothing. Without you, every opposite is the same thing.", RIDDLE_3),
                a("Something else would take your place.", LOST));

        ask(RIDDLE_3, B_GEM, say(
                "...Right.",
                "Last one. Someone stands where you are and says, 'Everything I say is a lie.' Are they lying?"),
                a("Yes.", LOST),
                a("No.", LOST),
                a("There's no way to tell.", LOST),
                a("Both, and neither.", BOON));

        tell(BOON, B_STARS, END, FX_BOON, say(
                "...Both, and neither.",
                "Fine. That's three.",
                "Here. Take it and go, before I change my mind."));

        ask(LOST, B_BECKON, say(
                "Wrong.",
                "That was the deal. Into the glass."),
                act("Keep your word. Step into the glass.", LURED).pull(3),
                a("No. I'm not doing that.", WELSHED).ego(-2),
                a("Make me.", WELSHED).ego(-3));

        tell(WELSHED, B_DIM, REFUSE, FX_NONE, say(
                "You said deal. You said it.",
                "And now you're going to break your word right to my face, and just stand there?",
                "Fine. Go away. I'm not talking to you."));

        // -------------------------------------------------------------- inside

        tell(LURED, B_SWALLOW, READ, FX_LURE, say(
                "There we go.", "That wasn't so hard, was it? You're in my glass now, and everything in here is me.",
                "Hold still. Let me have a look at you."));
        tell(READ_THIEF, B_GEM, BATTLE, FX_NONE, say(
                "A thief.", "You came all the way out here to steal from me.",
                "In here, I've got a lot more than two hands."));
        tell(READ_CURIOUS, B_LAUGH, SPIT, FX_CURSE, say(
                "That's it? Just curious?", "I pull someone all the way in and there's nothing in there but questions. Boring.",
                "Out you go. I'm keeping a bit of you, though."));

        // -------------------------------------------------------------- the glass breaks

        tell(FREE, B_ASCEND, STEP, FX_NONE, say(
                "Out!",
                "Order! Chaos! Are you watching? I'm out, and now I get to be everything, everywhere, and..."));
        tell(FREE_PROUD, B_ASCEND, STEP, FX_NONE, say(
                "Watch.", "Watch me step out and stay exactly what I am.",
                "Then we'll talk about your theory."));

        ask(FALLEN, B_NONE, faint(
                "Something's... wrong.", "Why can't I... I'm everything. I'm...",
                "You. Pick me up. Now."),
                act("Take the Soul Gem off his head.", THEFT, REQ_GEM),
                a("Not so all-powerful now, are you?", THEFT_GLOAT, REQ_GEM),
                a("This is what I was waiting for.", THEFT_BETRAY, REQ_GEM | REQ_PLANNED),
                a("I told you this would happen.", TOLD, REQ_TRAP),
                act("Raise the Astral Regulator over him.", ABSORB, REQ_REGULATOR),
                act("Help him up.", LIFTED),
                act("Walk away.", LEFT_BEHIND));

        tell(THEFT, B_GEM_TAKE, END, FX_TAKE, faint(
                "No. No, that's mine. That's my EYE.",
                "Feel that? Order and Chaos. They're coming to put me back. I hope they take their time with you."));
        tell(THEFT_GLOAT, B_GEM_TAKE, END, FX_TAKE, faint(
                "Out here there's nothing for me to stand between. There's... nothing.",
                "Laugh while you can. They're coming."));
        tell(THEFT_BETRAY, B_GEM_TAKE, END, FX_TAKE, faint(
                "The whole time?",
                "Every word, the whole time, and I didn't see it.",
                "Run. They're coming, and they're a lot less fun than me."));
        tell(LIFTED, B_NONE, END, FX_NONE, faint(
                "Fine. That'll do.", "This'll pass. Order and Chaos will... they'll..."));
        tell(TOLD, B_NONE, END, FX_NONE, faint(
                "You said something that happened to be true.", "That doesn't make you right. Go away."));
        tell(LEFT_BEHIND, B_NONE, END, FX_NONE, faint(
                "Hey. HEY. Come back and pick me up.", "I'm... I'm..."));
        tell(ABSORBED, B_NONE, END, FX_NONE, faint(
                "What's that light?", "No. I know what that is. That's another jar. I'm not going in another jar.", "I'm NOT..."));

        // -------------------------------------------------------------- partings

        tell(FAREWELL, B_SIT, END, FX_NONE, say("Go, then.", "I'll be here. Obviously."));
        tell(FAREWELL_COLD, B_TURN, END, FX_NONE, say(
                "Fine.", "I'll have forgotten you before you're out of sight."));
        tell(SEEN_THROUGH, B_LAUGH, REFUSE, FX_NONE, say(
                "Oh, I see.",
                "Everything you've said has been about getting me out of here. Not about me. About the glass.",
                "Nice try. Stand there as long as you want. I'm done talking to you."));
        tell(OFFENDED, B_TURN, REFUSE, FX_NONE, say(
                "That's enough.",
                "Go away before I decide you're worth thinking about.",
                "I'm done talking to you."));
        tell(DRAW, B_SIT, END, FX_NONE, say(
                "We're going in circles.", "I've got all the time there is, and you're boring me.",
                "Come back when you've got something new."));
        tell(BROKEN_TOOL, B_TURN, REFUSE, FX_NONE, say(
                "Then you're no use to me.",
                "Stand there as long as you like. I'll be looking at something else."));

        // -------------------------------------------------------------- the sulk, and his bluff

        ask(SULK, B_TURN, say(
                "...", "I'm not talking to you."),
                act("Knock on the glass.", SULK_2).annoy(1),
                a("You can't actually make me leave, can you?", ELIM_1).ego(-1),
                a("Fine. I'll go.", END));

        ask(SULK_2, B_TURN, say(
                "Still not talking to you.", "This is me, not talking."),
                act("Knock. Knock. Knock.", BLUFF).annoy(1),
                act("Sit down and stare at the back of his head.", BLUFF).annoy(1),
                a("You're talking right now.", BLUFF).ego(-1),
                a("Fine. I'll go.", END));

        ask(BLUFF, B_DIM, say(
                "Stop that.",
                "Do you know what you're knocking on? I could fold you up into a dot and unfold you as something that screams. I "
                        + "could turn your blood into light and make you watch it leave.",
                "Stop. Now."),
                a("Then do it.", BLUFF_DARE),
                a("So why haven't you?", ELIM_1),
                act("Knock again.", BLUFF_DARE).annoy(1),
                a("Sorry.", NEXT).ego(1));

        ask(BLUFF_DARE, B_FROST, say(
                "...",
                "I'm choosing not to.",
                "Mercy's beneath me, and so is cruelty, so I'm above both of them, so... I'm choosing not to."),
                a("That's not how that works.", ELIM_1).ego(-1),
                a("Sure you are.", ELIM_1).ego(-1),
                act("Knock on the glass.", RANT).annoy(1));

        ask(ELIM_1, B_SIT, say(
                "Because you're not worth the bother. That's why.",
                "...And I like my visitors closer. Where I can see them properly."),
                a("You said you see everything. Why would you need me closer?", ELIM_2),
                a("Because you can reach me in there. Just not out here.", ELIM_DENY),
                act("Step a little closer.", NEXT).pull(2));

        ask(ELIM_DENY, B_ASCEND, say(
                "Not out here? Please.",
                "In here and out there are two sides of the same thing, and I'm what sits between them. I'm in here because I want "
                        + "to be.",
                "The glass just... focuses me."),
                a("Then why would you need me closer?", ELIM_2),
                a("If you want to be in there, you could want to come out.", ELIM_2).hunger(1),
                a("Right. My mistake.", NEXT).ego(1));

        ask(ELIM_2, B_TAP, say(
                "Why does everyone think I need things?",
                "I don't need you closer. I don't need to come out. I don't need anything.",
                "Having people close is just... manners. You don't shout at someone across a field."),
                a("You see everything, supposedly. So why can't you tell what I'm thinking from there?", ELIM_3).ego(-2).hunger(3)
                        .sets(F_RULE),
                a("If I threw a rock at your glass, could you stop it?", ELIM_3).ego(-2).hunger(3).sets(F_RULE),
                a("Has anything you've wanted ever actually made it through that glass?", ELIM_3).ego(-2).hunger(3).sets(F_RULE),
                a("Manners. Right.", NEXT));

        ask(ELIM_3, B_DIM, FX_EXPOSED, say(
                "...",
                "No.",
                "Nothing gets through. Out there, I'm a voice in a jar. Happy?",
                "But it's the glass that stops me, not me. Break the glass and there's no 'out there' any more."),
                a("Then maybe the glass should go.", NEXT).hunger(1),
                a("Or maybe the glass is the only thing keeping you powerful.", TRUTH).sets(F_TRAP),
                a("So I can say whatever I like, and you can't do a thing.", NEXT).ego(-1).annoy(1),
                a("That must be embarrassing.", NEXT).ego(-2));

        ask(RANT, B_ECHOES, say(
                "STOP. KNOCKING.",
                "I'm the In-Betweener! I'm the gap between one of your heartbeats and the next! I was old before your sun was lit, "
                        + "and I am not going to be pestered by something with a NOSE.",
                "What do you WANT?"),
                a("Can you actually do anything to me?", ELIM_3).ego(-2).hunger(3).sets(F_RULE),
                a("I want to see you out of that thing.", NEXT).hunger(2).suspicion(1),
                act("Knock again.", OFFENDED).annoy(1).ego(-2),
                a("Nothing. Carry on.", NEXT));

        // -------------------------------------------------------------- Master Order and Lord Chaos

        String surrender = "O:The gem. Hand it over.";

        masters(WRATH_THIEF, B_NONE, FX_NONE, duo(
                "O:You. You stand where Order and Chaos meet.",
                "C:Hi.",
                "O:He was ours. The glass was ours. Nobody was allowed to break it.",
                "C:Not him, and definitely not you. And yet here we all are! Isn't this fun?",
                "O:It is not. Explain yourself."),
                a("He wanted out. I helped.", W_ACCOUNT),
                a("I broke him out so I could take the Soul Gem. It worked.", W_PRIDE),
                a("Out here he fell flat, just like you built him to. Your design works.", W_DESIGN),
                a("I've got Eternity in this crystal. You answer to him, so you answer to me.", W_ETERNITY, REQ_REGULATOR),
                a("Get out of my way.", W_NOW));

        masters(WRATH_BYSTANDER, B_NONE, FX_NONE, duo(
                "O:You were here when the glass broke.",
                "C:At the scene! That's basically a confession.",
                "O:Someone answers for this. You are here."),
                a("I didn't do anything.", W_INNOCENT),
                a("Whoever did it already ran.", W_POINTED),
                a("I've got Eternity in this crystal. You answer to him, so you answer to me.", W_ETERNITY, REQ_REGULATOR),
                a("Then let's get it over with.", W_NOW));

        masters(WRATH_EMPTY, B_NONE, FX_NONE, duo(
                "O:Where is he? Where is our prisoner?",
                "C:Brother. Look at its hand. Look at the crystal.",
                "O:...",
                "C:They put him in a jar! They put the In-Betweener in a JAR! Oh, I love that. I'm still going to hurt you. But I "
                        + "love that.",
                "O:Explain yourself."),
                a("He's mine now.", W_DARED),
                a("Eternity's in there too. You answer to him.", W_ETERNITY, REQ_REGULATOR),
                a("Get out of my way.", W_NOW));

        masters(W_ACCOUNT, B_NONE, FX_ORDER_ANGERED, duo(
                "O:Helped.", "C:They helped, brother.", "O:I heard.",
                "O:Everything has a place. His was the glass. You moved him without asking.",
                "C:I move things without asking all the time.",
                surrender),
                a("Here. Take it back.", W_RETURNED, REQ_HOLDS_SOUL),
                a("No.", W_REFUSED),
                a("Come and take it.", W_DARED));

        masters(W_PRIDE, B_NONE, FX_PRIDE, duo(
                "C:Oh, I like this one. A thief and a liar. It's practically one of mine.",
                "O:It belongs to no one.",
                surrender,
                "C:Then we hurt you anyway, just a bit less. Probably."),
                a("Here. Take it back.", W_RETURNED, REQ_HOLDS_SOUL),
                a("No.", W_REFUSED),
                a("Come and take it.", W_DARED));

        masters(W_DESIGN, B_NONE, FX_NONE, duo(
                "O:...", "C:It's right, brother. He came out and fell flat. Just like you said he would.",
                "O:As we designed. The design holds.",
                "O:That does not mean you get to test it.",
                "C:It sort of does.",
                surrender),
                a("Here. Take it back.", W_RETURNED, REQ_HOLDS_SOUL),
                a("No.", W_REFUSED),
                a("Come and take it.", W_DARED));

        masters(W_ETERNITY, B_NONE, FX_RESONANCE, duo(
                "O:You have...", "C:...Eternity. In a little crystal. Oh, that's wonderful.",
                "O:We are the heart of Eternity. Without Order he has no shape. Without Chaos he never moves.",
                "O:And you are carrying him around in your hand.",
                "C:Brother, maybe we should be careful with this one.",
                "O:No. We should be thorough."),
                a("Here. Take the gem back.", W_RETURNED, REQ_HOLDS_SOUL),
                a("Then be thorough.", W_THOROUGH));

        mastersTell(W_THOROUGH, B_NONE, MASTERS_FIGHT, FX_BOTH_ANGERED, duo("C:Oh, they asked!", "O:Then they get it."));
        mastersTell(W_RETURNED, B_GEM_RETURN, MASTERS_FIGHT, FX_RETURN, duo(
                "O:Returned. Noted.", "C:Noted. Not forgiven.",
                "O:Giving it back does not fix what broke. Order has to be seen being kept.",
                "C:And Chaos has to be seen having fun."));
        mastersTell(W_REFUSED, B_NONE, MASTERS_FIGHT, FX_ORDER_ANGERED, duo(
                "O:Then we take it.",
                "C:From your hand, or from whatever's left of it. Order wants the hand. I'm easy."));
        mastersTell(W_DARED, B_NONE, MASTERS_FIGHT, FX_BOTH_ANGERED, duo(
                "C:'Come and take it!' Did you hear that?", "O:I heard.",
                "C:I'm going to remember that. For about a minute. Then I'll forget it, and you, and what shape you used to be."));
        mastersTell(W_NOW, B_NONE, MASTERS_FIGHT, FX_BOTH_ANGERED, duo("O:Very well.", "C:Ooh. Brave."));
        mastersTell(W_INNOCENT, B_NONE, MASTERS_FIGHT, FX_NONE, duo(
                "O:Innocent is not the same as excused.", "C:And innocent can change!"));
        mastersTell(W_POINTED, B_NONE, MASTERS_FIGHT, FX_NONE, duo(
                "C:Ha! Blaming someone else. Brother, there's a bit of you in this one.",
                "O:Noted. It changes nothing.", "C:'It changes nothing!' That's my line."));
    }

    private InBetweenerDialogue() {
    }

    public static Node node(int id) {
        Node node = NODES.get(id);
        return node != null ? node : NODES.get(FAREWELL);
    }

    public static boolean exists(int id) {
        return NODES.containsKey(id);
    }

    public static boolean isQuestion(int id) {
        return node(id).isQuestion();
    }
}
