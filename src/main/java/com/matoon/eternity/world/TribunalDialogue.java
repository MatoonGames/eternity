package com.matoon.eternity.world;

/**
 * Everything the Living Tribunal says and every answer a defendant may give. Shared by
 * both sides: the client shows it, and only the server keeps score. The best answers
 * are humble but unbowed; grovelling earns nothing, and defiance earns banishment.
 */
public final class TribunalDialogue {

    public static final int VERDICT_ACQUITTED = 100;
    public static final int VERDICT_STRIPPED = 101;
    public static final int VERDICT_BANISHED = 102;
    /** He accepts a challenge, and rises. */
    public static final int CHALLENGE_ACCEPTED = 103;
    /** The challenger fell before him. */
    public static final int VERDICT_DEFEATED = 104;

    /** Offered with every question: refuse his judgement and fight him instead. */
    private static final String CHALLENGE = "Enough. I will not be judged by you. Rise, and face me.";

    /** One thing he says, and what you may answer. */
    public static final class Node {
        public final String speech;
        public final String[] answers;
        /** Points for each answer: 2 is the best a mortal can do. */
        final int[] scores;
        /** Which answers defy him. Defy him twice and you will not be allowed to stay. */
        final boolean[] defiant;

        Node(String speech, String[] answers, int[] scores, boolean[] defiant) {
            this.speech = speech;
            // Every question can be answered with defiance of a different kind: a challenge.
            this.answers = scores.length == 0 ? answers : append(answers, CHALLENGE);
            this.scores = scores;
            this.defiant = defiant;
        }
    }

    private static final Node[] QUESTIONS = {
            new Node("MORTAL. YOU STAND BEFORE THE LIVING TRIBUNAL. You slew Eternity, the embodiment of your universe, "
                    + "and caged what remained of him in a crystal. The balance of the multiverse is broken. "
                    + "Speak, before I weigh what you are.",
                    new String[]{
                            "I didn't break the balance. I kept the universe from ending.",
                            "Eternity came for me. I did what I had to, and then I held the universe together.",
                            "I owe you no explanation.",
                            "Please, I didn't know what I was doing!"},
                    new int[]{1, 2, -1, 0},
                    new boolean[]{false, false, true, false}),
            new Node("THE STONES. You gathered all six. No mortal hand was meant to close around them. Why did you take them?",
                    new String[]{
                            "To become strong enough to protect what I love.",
                            "Because I could. Power belongs to those who take it.",
                            "They were scattered and hunted. Better one careful hand than a thousand careless ones.",
                            "I don't know. They called to me."},
                    new int[]{1, -1, 2, 0},
                    new boolean[]{false, true, false, false}),
            new Node("ETERNITY LIES WITHIN THE REGULATOR, AND HIS POWER ANSWERS TO YOU NOW. What will you do with it?",
                    new String[]{
                            "Whatever I want. I earned it.",
                            "Guard it. It must never be turned against the order of things.",
                            "Use it to make my world better.",
                            "Give it to you, if you ask for it."},
                    new int[]{-1, 2, 0, 1},
                    new boolean[]{true, false, false, false}),
            new Node("ONE BEING. ONE UNIVERSE. YOU HAVE PLACED YOURSELF ABOVE BOTH. Are you a god, mortal?",
                    new String[]{
                            "If I am, then you are the one who must answer to me.",
                            "Perhaps I am, now.",
                            "No. I was only the one standing there when it all fell apart.",
                            "I am whatever this universe needed me to be."},
                    new int[]{-1, -1, 2, 1},
                    new boolean[]{true, true, false, false}),
            new Node("INFINITY. DEATH. OBLIVION. EVERY ABSTRACT BEING FELT YOUR HAND. If I return you to your world, "
                    + "what will stop you from doing it again?",
                    new String[]{
                            "Nothing. And nothing ever will.",
                            "Nothing would. That is why you should trust me.",
                            "You will. You will be watching.",
                            "My word. I never wanted this power. I only wanted to survive it."},
                    new int[]{-1, 0, 1, 2},
                    new boolean[]{true, false, false, false}),
            new Node("I HAVE SEEN ALL THAT YOU ARE, ACROSS EVERY ROAD YOU MIGHT HAVE WALKED. "
                    + "Have you anything left to say before I pass judgement?",
                    new String[]{
                            "Spare me. I will give you anything.",
                            "Do your worst. I will find my way back.",
                            "I saved this universe. Remember that.",
                            "Judge me fairly. That is all I ask."},
                    new int[]{0, -1, 1, 2},
                    new boolean[]{false, true, false, false}),
    };

    private static final Node ACQUITTED = new Node("...ENOUGH. THE MULTIVERSE IS NOT IN BALANCE, BUT IT IS IN ORDER. "
            + "You held together what others would have let fall. Go home, mortal. And do not make me summon you again.",
            new String[0], new int[0], new boolean[0]);
    private static final Node STRIPPED = new Node("GUILTY. YOU REACHED FOR WHAT WAS NEVER YOURS. "
            + "Then you shall return to your world with NOTHING. Every stone, every crystal, every power you were given or took, stripped from you.",
            new String[0], new int[0], new boolean[0]);
    private static final Node ACCEPTED = new Node("THEN YOU HAVE CHOSEN. NO MORTAL HAS EVER RAISED A HAND AGAINST THIS COURT AND REMAINED. "
            + "Rise, and be weighed in battle instead of words.",
            new String[0], new int[0], new boolean[0]);
    private static final Node DEFEATED = new Node("YOU RAISED YOUR HAND AGAINST THE BALANCE OF ALL THINGS, AND IT FELL ON YOU. "
            + "Go home with NOTHING. Every stone, every crystal, every power you were given or took, stripped from you.",
            new String[0], new int[0], new boolean[0]);
    private static final Node BANISHED = new Node("GUILTY. YOU WOULD STAND ABOVE THE ORDER OF ALL THINGS. "
            + "Then you shall have no place within it. Your world is closed to you, now and forever. BEGONE.",
            new String[0], new int[0], new boolean[0]);

    private TribunalDialogue() {
    }

    public static int questions() {
        return QUESTIONS.length;
    }

    public static Node node(int id) {
        switch (id) {
            case VERDICT_ACQUITTED:
                return ACQUITTED;
            case VERDICT_STRIPPED:
                return STRIPPED;
            case CHALLENGE_ACCEPTED:
                return ACCEPTED;
            case VERDICT_DEFEATED:
                return DEFEATED;
            case VERDICT_BANISHED:
                return BANISHED;
            default:
                return QUESTIONS[Math.max(0, Math.min(QUESTIONS.length - 1, id))];
        }
    }

    /** Whether this answer is the challenge, which is never scored. */
    public static boolean isChallenge(int id, int answer) {
        return isQuestion(id) && answer == QUESTIONS[id].scores.length;
    }

    private static String[] append(String[] answers, String extra) {
        String[] all = new String[answers.length + 1];
        System.arraycopy(answers, 0, all, 0, answers.length);
        all[answers.length] = extra;
        return all;
    }

    public static boolean isQuestion(int id) {
        return id >= 0 && id < QUESTIONS.length;
    }

    static int score(int node, int answer) {
        return QUESTIONS[node].scores[answer];
    }

    static boolean defiant(int node, int answer) {
        return QUESTIONS[node].defiant[answer];
    }
}
