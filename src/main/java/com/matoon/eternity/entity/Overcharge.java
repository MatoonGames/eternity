package com.matoon.eternity.entity;

/**
 * The parts of the Destroyer Armor its dynamo can be overcharged into, once the Overcharge is forged, each turned up
 * or down on its own, as far as its pilot dares: there is no top to it. Every level held draws its upkeep in Forge
 * Energy every tick they are inside, steeper the higher it goes, and heats its core (see
 * {@link EntityDestroyerArmor#core()}): run it hotter than it can shed, or draw more than it makes once its store is
 * dry, and the core breaches, and the armour implodes.
 * <p>
 * Their order is the order they are listed and paged through in. More can be added at the end.
 */
public enum Overcharge {
    BEAM("beam", "Beam", 0xFF5A30, "The ray: harder, further, wider, and Molten Heart's bursts bigger."),
    JETS("jets", "Jets", 0x60C8FF, "Its jets: faster flight. A stone's own flight is never overcharged."),
    FISTS("fists", "Fists", 0xFFB040, "Its punches and its slam land harder."),
    PLATING("plating", "Plating", 0x8AE070, "Charge run through its plate: it takes less from every blow.");

    /** As high as any part goes. Nobody should get near it. */
    public static final int MAX = 9999;
    /** Ticks its core can stand at its limit before it breaches. */
    public static final int BREACH_TICKS = 200;
    public static final float CORE_MAX = 100F;
    /** Held at or above this heat for {@link #SCORCH_GRACE} ticks, its core starts tearing at its own frame. */
    public static final float SCORCH_AT = 75F;
    public static final int SCORCH_GRACE = 100;

    /** Its key in saved data. */
    public final String key;
    public final String title;
    public final int color;
    public final String description;

    Overcharge(String key, String title, int color, String description) {
        this.key = key;
        this.title = title;
        this.color = color;
        this.description = description;
    }

    /** Forge Energy a tick to hold a part at a level: a little at first, then a lot. */
    public static int upkeep(int level) {
        long fe = 20L * level + (long) level * level;
        return (int) Math.min(Integer.MAX_VALUE, fe);
    }

    /**
     * How much harder the part works at a level: its damage for the beam and the fists, its speed for the jets (which
     * levels off, past a point), and for the plating, how much of a blow still gets through.
     */
    public float factor(int level) {
        switch (this) {
            case JETS:
                return 1F + 3F * (1F - (float) Math.exp(-level / 30F));
            case PLATING:
                return 1F / (1F + 0.05F * level);
            default:
                return 1F + 0.15F * level;
        }
    }

    /** What a level does, in a few words. */
    public String effect(int level) {
        if (level <= 0) {
            return "as built";
        }
        switch (this) {
            case BEAM:
                return "x" + trim(factor(level)) + " damage, x" + trim(beamRange(level)) + " range";
            case JETS:
                return "x" + trim(factor(level)) + " flight speed";
            case FISTS:
                return "x" + trim(factor(level)) + " punch and slam";
            default:
                return Math.round((1F - factor(level)) * 100F) + "% less damage taken";
        }
    }

    /** How much further the ray reaches overcharged. */
    public static float beamRange(int level) {
        return Math.min(2.5F, 1F + 0.01F * level);
    }

    /**
     * How fast its core heats, a tick, with this many levels held across all its parts: barely at first, then
     * sharply. Its core sheds a share of its heat every tick, so it settles where the two meet, unless that is past
     * its limit.
     */
    public static float heating(int total) {
        float f = total / 100F;
        return 0.6F * f * f;
    }

    /** The share of its heat its core sheds a tick, before any cooling it has been given. */
    public static final float SHED = 0.004F;

    public static String trim(float f) {
        if (f >= 100F) {
            return String.valueOf(Math.round(f));
        }
        float r = Math.round(f * 100F) / 100F;
        return r == (int) r ? String.valueOf((int) r) : String.valueOf(r);
    }
}
