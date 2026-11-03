package com.matoon.eternity.entity;

/**
 * Everything the people of the first chapter, living and dead, do with their bodies. The server
 * says which; each client builds the pose out of it and how long it has been held.
 */
public enum NorsePose {
    /** Standing, breathing, looking at whatever they are attending to. */
    STAND,
    /** Head down, hands folded in front: at a grave. */
    MOURN,
    /** Down on one knee. */
    KNEEL,
    /** Kneeling, holding someone's hand in both of hers. */
    HOLD,
    /** Kneeling, reaching down to close someone's eyes, and drawing the hand back. */
    CLOSE,
    /** Kneeling, a hand laid flat on someone's chest, and left there. */
    TOUCH,
    /** On their back, alive: the head lifting, a hand reaching. */
    LIE,
    /** On their back and still. */
    DEAD,
    /** The face turned up and up, past the sky, and held there. */
    SKY,
    /** A fist raised slowly overhead, as if holding up a belt. */
    FIST,
    /** Both hands cupped round something small, and closed on it. */
    CUP,
    /** A palm pressed flat to the stone in front. */
    PALM,
    /** Crouched, a palm flat on the ground. */
    GROUND,
    /** The head inclined: the most she gives anyone. */
    NOD,
    /** Pointing, arm straight, at whatever they attend to. */
    POINT,
    /** The axe drawn from the belt, into a guard. */
    DRAW,
    /** On guard with the axe. */
    FIGHT,
    /** A blow with the axe. */
    SWING,
    /** The dead: sitting up out of the grave and getting to their feet. */
    RISE,
    /** The dead: on their feet and coming. */
    HUNT,
    /** The dead: a blow. */
    CLAW,
    /** Knocked down, and lying there until they get up again. */
    FALLEN,
    /** Kneeling at a bed, working the soil with a trowel, the other hand pressing it down. */
    TEND,
    /** Kneeling, something small held in both hands, brought to the chest and pressed into it. */
    PRESS,
    /** Standing, both arms rising slowly, palms up: and the ground answering. */
    RAISE,
    /** Standing asleep in a capsule: the head bowed, the eyes shut, the arms down, breathing slowly. */
    SLEEP,
    /** Asleep, and then not quite: the eyes open, the head comes up, and he looks at whoever is looking at him. */
    STIR,
    /** Holding forth: chin up, one hand turning in the air as if laying out the shape of things. */
    LECTURE,
    /** One hand held out, palm up, for something to be put in it. */
    REACH,
    /** Back a step, the hands up between them and whatever is coming. */
    RECOIL,
    /** Both hands open, held out, the head bowed a little: asking. */
    PLEAD,
    /** Hands clasped behind the back, the chin lowered to look down at someone smaller. */
    REGARD,
    /** One hand up at the height of someone's throat, closed on it: holding them off the ground. */
    GRIP,
    /** Both hands drawn back to the hip, then thrown forward open: what he holds, let go. */
    BLAST,
    /** In the air: the legs hanging, the arms a little out, the whole body easy. */
    HOVER,
    /** Reaching, closing the hand on something, drawing it back to the chest. */
    TAKE,
    /** Taken hold of by something she cannot see, lifted off her feet: arching, clawing at her own throat, the mouth open. */
    SEIZED;

    /** How long each timed pose lasts, in ticks; the model times its frames to the same numbers. */
    public int ticks() {
        switch (this) {
            case CLOSE:
                return 60;
            case NOD:
                return 40;
            case DRAW:
                return 22;
            case SWING:
                return 16;
            case RISE:
                return 36;
            case CLAW:
                return 18;
            case PRESS:
                return 140;
            case BLAST:
                return 26;
            case TAKE:
                return 40;
            default:
                return -1;
        }
    }

    public boolean lying() {
        return this == LIE || this == DEAD || this == FALLEN;
    }
}
