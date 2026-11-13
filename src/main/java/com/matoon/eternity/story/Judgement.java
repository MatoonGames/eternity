package com.matoon.eternity.story;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;

import static com.matoon.eternity.story.Speakers.WARLOCK;
import static com.matoon.eternity.story.Talk.act;
import static com.matoon.eternity.story.Talk.say;

/**
 * What Warlock thinks of someone who keeps saying yes to her. He has seen what she is; he was
 * made to be afraid of her. A little way in, he notices, and says so. Further, past the point
 * where she is coming apart in front of them and they lean in anyway, he calls it what it is,
 * to their face. And once she has torn off the pretty one and they stayed, he gives up on them:
 * not angry any more, only done, as if they were already something of hers.
 */
final class Judgement {

    /** Where he is when he says it. */
    static final int ARENA = 0, GLASS = 1, GARDEN = 2;
    /** How far in someone has to be before he notices; before he says it to their face. He gives up on them once she has shown herself. */
    static final int NOTICES = 5, CALLS_OUT = Obsession.UNHINGED;

    private static final String[][] NOTICING = {
            {"You've been talking to her. I can tell. You stand closer to dead things than you used to."},
            {"She's been at you. You smell like lilies left too long in the water."},
            {"You looked at her when he fell. Not at him. At her. Like you were checking she was pleased."}};
    private static final String[][] CALLING_OUT = {
            {"I heard what she says to you. All of it. And I heard you say yes.",
                    "She's coming apart in front of you, and you said yes. What is wrong with you?"},
            {"She told you to kill something that loved you, and you went and looked for one. Don't tell me you didn't. I was watching.",
                    "You kill things so she'll shiver. Do you understand that's what you've become? Something that kills so she'll shiver."},
            {"She's rotting in front of you and you keep leaning in.",
                    "Do you know what that looks like, from outside? It looks like a fly."}};
    private static final String[][] WRITING_OFF = {
            {"I watched her take her face off for you. Her own face, in handfuls. And you stayed.",
                    "I'm not going to try with you any more. When she's finished with you I'll take the stones out of whatever's left."},
            {"Don't. Don't talk to me.",
                    "I stopped thinking of you as someone. You're just where she keeps the stones until she wants them."},
            {"Mother grew me in a vat, and I've still got more will of my own than you.",
                    "There's nothing in you to save. I checked. I kept checking, longer than I should have."}};

    private Judgement() {
    }

    /** The most anyone in a conversation has fallen for her. */
    private static int most(Talk t) {
        int most = 0;
        for (EntityPlayerMP p : t.present()) {
            most = Math.max(most, StoryDirector.INSTANCE.data().player(p.getUniqueID()).devotion);
        }
        return most;
    }

    /** Whether any of them has seen her tear the pretty one off, and stayed. */
    private static boolean shown(Talk t) {
        return Devotion.unveiled(t);
    }

    /** What he says to them about her, here, by how far they have gone; nothing, if they haven't. */
    static Talk judged(Talk t, Entity w, int where) {
        int d = most(t);
        if (shown(t)) {
            String[] says = WRITING_OFF[where];
            return t.direction("Warlock looks at you the way you'd look at something on the bottom of a boot.")
                    .line(WARLOCK, w, says[0])
                    .line(WARLOCK, w, says[1])
                    .ask(WARLOCK, w, 20 * 12, 2,
                            say("She loves me.", c -> c.line(WARLOCK, w, "She loves you the way a fire loves a house.")),
                            say("You don't understand her.", c -> c.line(WARLOCK, w, "I understand her fine. It's you I gave up on.")),
                            act("(Say nothing.)", c -> c.direction("He turns his back on you, and doesn't turn round again.")));
        }
        if (d >= CALLS_OUT) {
            String[] says = CALLING_OUT[where];
            return t.line(WARLOCK, w, says[0])
                    .line(WARLOCK, w, says[1])
                    .ask(WARLOCK, w, 20 * 12, 2,
                            say("It's none of your business.", c -> c.line(WARLOCK, w, "It will be, when she's done with you and there's nobody in there to stop her.")),
                            say("She needs me.", c -> c.line(WARLOCK, w, "She needs you the way a hook needs a fish. Listen to yourself.")),
                            act("(Look away.)", c -> c.line(WARLOCK, w, "Yes. That's what I thought.")));
        }
        if (d >= NOTICES) {
            return t.line(WARLOCK, w, NOTICING[where][0]);
        }
        return t;
    }

    /** What he says over his shoulder, beaten, on his way: plainer the further gone they are. */
    static String parting(Talk t, String otherwise) {
        int d = most(t);
        if (shown(t)) {
            return "Don't bother asking her anything. You'd only like the answer.";
        }
        if (d >= CALLS_OUT) {
            return "Go on. Run back and show her where I hit you. She'd like that, wouldn't she.";
        }
        return otherwise;
    }
}
