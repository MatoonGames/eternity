package com.matoon.eternity.world;

import com.matoon.eternity.entity.EntityGrandmaster.State;

/**
 * The Grandmaster's scripted scenes, shared by both sides so they keep time together: the server
 * moves him through his gestures on these ticks, and the client speaks his lines, moves its camera
 * and plays out the demonstration on the same ones.
 */
public final class GrandmasterScenes {

    /** Who is speaking a line. */
    public static final byte GRANDMASTER = 0, WARLOCK = 1, DIRECTION = 2;

    /** Ticks of fade before the cutscene's own clock starts at 0. */
    public static final int PREROLL = 14;

    /** One beat of a scene: a line, and what he does while he says it. */
    public static final class Beat {
        public final int tick;
        public final String line;
        public final State gesture;
        public final int hold;

        Beat(int tick, String line, State gesture, int hold) {
            this.tick = tick;
            this.line = line;
            this.gesture = gesture;
            this.hold = hold;
        }
    }

    /**
     * How to play, as he tells it, while the board shows it. The demonstration keyed to these
     * ticks is in client/RulesCutscene; the camera keys sit between them.
     */
    public static final Beat[] RULES = {
            new Beat(0, "Hnefatafl. The king's table. The Norse played it on bone for a thousand years.", State.SHUFFLE, -1),
            new Beat(95, "They learned it from me, and they got it wrong. These are the right rules.", State.LAUGH, -1),
            new Beat(180, "You have the king and his four.", State.POINT, -1),
            new Beat(225, "He has my eight.", State.PRESENT, -1),
            new Beat(270, "Straight lines, as far as you like. Nothing jumped.", State.CONDUCT, 80),
            new Beat(370, "Close a piece between two of yours and it's gone.", State.CONDUCT, 70),
            new Beat(450, "The corners, the empty throne and the stone count against everyone.", State.THINK, 70),
            new Beat(530, "Throne and corners are the king's. Get him to a corner and you win.", State.CONDUCT, 90),
            new Beat(630, "Lose him and I keep the pot.", State.POINT, -1),
            new Beat(690, "The stone moves after every move. You'll always see where it's going next. Nothing goes through it.", State.SHUFFLE, -1),
            new Beat(790, "It's all in there, if you forget. Raiders first.", State.BOW, -1),
    };

    /** The rules cutscene's length, from its clock's 0. */
    public static final int RULES_LENGTH = 870;

    private GrandmasterScenes() {
    }
}
