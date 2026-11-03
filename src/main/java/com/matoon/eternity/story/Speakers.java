package com.matoon.eternity.story;

import com.matoon.eternity.world.GrandmasterScenes;

/**
 * Everyone who can speak in the panel at the foot of the screen, by the byte that names them on
 * the wire. The first three are the Grandmaster's and keep their old numbers.
 */
public final class Speakers {

    public static final byte GRANDMASTER = GrandmasterScenes.GRANDMASTER;
    public static final byte WARLOCK = GrandmasterScenes.WARLOCK;
    /** What nobody says: grey italics, no panel. */
    public static final byte DIRECTION = GrandmasterScenes.DIRECTION;
    /** The woman in mourning clothes, before anyone has named her. */
    public static final byte STRANGER = 3;
    public static final byte RAGNA = 4;
    public static final byte HRAFN = 5;
    /** The stranger, once she has said what she is. */
    public static final byte DEATH = 6;
    /** Words carved into a keeper's stone. */
    public static final byte STONE = 7;
    /** The Champion of the Universe. */
    public static final byte CHAMPION = 8;
    /** Several players, answering together: the panel shows whoever spoke for them. */
    public static final byte PARTY = 9;
    /** The In-Betweener, in his glass. */
    public static final byte IN_BETWEENER = 10;
    /** The Gardener, Ord Zyonz, on Mars. */
    public static final byte GARDENER = 11;
    /** Ayesha, High Priestess of the Sovereign: Warlock's Mother. */
    public static final byte MOTHER = 12;
    /** Her voice, as the station's consoles kept it. */
    public static final byte RECORD = 13;
    /** The Magus: what Warlock would have become. */
    public static final byte MAGUS = 14;
    /** Lord Chaos, without Master Order beside him. */
    public static final byte CHAOS = 15;
    /** Master Order, the other of the two. */
    public static final byte ORDER = 16;
    /** One of Odin's dead guards, in the vault. */
    public static final byte EINHERJAR = 17;
    /** The Living Tribunal, in judgement. */
    public static final byte TRIBUNAL = 18;

    private Speakers() {
    }
}
