package com.matoon.eternity.story;

/**
 * The oath, and the stones raised for the people of the village that keeps it.
 * <p>
 * Five keepers have kept the cube since Ulf swore to, each the child of the last, and each added
 * a line to the oath when they took it on; the words are on their stones. The barrow's inner
 * door opens to the whole of it, spoken oldest first. Nobody alive knows it all: Hrafn's father
 * was meant to teach him, and the sea took him first. So the order is in the names, which run
 * each from the last: Ulf, Asgerd Ulfsdottir, Bjorn Asgerdsson, Sigrid Bjornsdottir, Eirik
 * Sigridsson.
 */
public final class Stones {

    /** The keepers, oldest first; their numbers are also the oath's lines. */
    public static final int ULF = 0, ASGERD = 1, BJORN = 2, SIGRID = 3, EIRIK = 4;
    /** Stones raised for the ones the cube took. */
    public static final int RAGNA = 5, LEIF = 6, GUNNHILD = 7;
    /** An old stone too worn to read. */
    public static final int WORN = 8;

    public static final int KEEPERS = 5;

    private static final String[] NAMES = {"Ulf", "Asgerd", "Bjorn", "Sigrid", "Eirik"};
    /** Their names as cut: each but Ulf's says whose child they were, which is the oath's order. */
    private static final String[] FULL = {"Ulf", "Asgerd Ulfsdottir", "Bjorn Asgerdsson", "Sigrid Bjornsdottir", "Eirik Sigridsson"};

    /** The oath, a line for each keeper. */
    public static final String[] OATH = {
            "What the One-Eyed gave into our hands, we keep.",
            "Under the earth, under the stone, out of the light.",
            "We do not open it. We do not sell it. We do not ask what it is.",
            "Until the One-Eyed comes himself and asks for it back.",
            "And whoever of us breaks it, our dead will take."};

    /** What each stone says. */
    private static final String[] CARVED = {
            "ULF, WHO SWORE IT. He spoke: \"" + OATH[ULF] + "\"",
            "ASGERD ULFSDOTTIR, WHO KEPT IT. She spoke: \"" + OATH[ASGERD] + "\"",
            "BJORN ASGERDSSON, WHO KEPT IT. He spoke: \"" + OATH[BJORN] + "\"",
            "SIGRID BJORNSDOTTIR, WHO KEPT IT. She spoke: \"" + OATH[SIGRID] + "\"",
            "EIRIK SIGRIDSSON, WHO KEPT IT. The sea has him. He spoke: \"" + OATH[EIRIK] + "\"",
            "Hrafn raised this stone for Ragna, his sister. She was taken from the washing line.",
            "Thora raised this stone for Leif, her son. He was taken from his boat.",
            "Raised for Gunnhild. She sickened in a day.",
            "The carving has worn too far to read."};

    private static final String[] WHOSE = {
            "Ulf's stone", "Asgerd Ulfsdottir's stone", "Bjorn Asgerdsson's stone", "Sigrid Bjornsdottir's stone",
            "Eirik Sigridsson's stone", "Ragna's stone", "Leif's stone", "Gunnhild's stone", "A worn stone"};

    private Stones() {
    }

    public static String carved(int stone) {
        return CARVED[valid(stone)];
    }

    public static String whose(int stone) {
        return WHOSE[valid(stone)];
    }

    public static boolean isKeeper(int stone) {
        return stone >= ULF && stone <= EIRIK;
    }

    /** Raised this year, for the ones the cube took: pale stone, runes freshly painted. */
    public static boolean fresh(int stone) {
        return stone == RAGNA || stone == LEIF || stone == GUNNHILD;
    }

    /** A keeper's line as an answer at the door: whose it was, by their whole name, and what it says. */
    public static String asAnswer(int keeper) {
        return FULL[keeper] + "'s: \"" + OATH[keeper] + "\"";
    }

    public static String fullName(int keeper) {
        return FULL[keeper];
    }

    public static String name(int keeper) {
        return NAMES[keeper];
    }

    private static int valid(int stone) {
        return stone >= 0 && stone < CARVED.length ? stone : WORN;
    }
}
