package com.matoon.eternity.world;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.DimensionManager;

import java.util.UUID;

/**
 * The Cosmic Comet's persistent state. There is exactly one per universe. Like
 * Eternity, it exists here rather than as a saved entity, and its body is only
 * conjured when someone is close enough to reach it.
 */
public class CometData extends WorldSavedData {

    public static final String NAME = "eternity_comet";

    public enum Phase {
        /** Somewhere out in the dark, beyond anyone's sight. */
        DORMANT,
        /** Crossing someone's sky. */
        PASSING,
        /** It slipped away and is streaking off. */
        DEPARTING,
        /** It has taken all it can. The heart is breaking open. */
        CRACKING,
        /** Broken open. It will never be seen again in this world. */
        SHATTERED
    }

    public Phase phase = Phase.DORMANT;
    /** Ticks that have passed while at least one player was online. */
    public long clock;

    public int dimension;
    public double x, y, z;
    /** Cruising velocity in blocks per tick. */
    public double vx, vy, vz;
    public boolean space;

    /** Damage taken so far. Wounds persist between visits. */
    public float damage;
    /** Tenths of its health lost, each answered with a flare. */
    public int flares;

    /** Ticks into the current phase. */
    public int stageTicks;
    /** Ticks since it was last within anyone's sight. */
    public int lostTicks;

    /** Who broke it open, once someone has. */
    public String breaker = "";

    /** The Astral Regulator it left behind, until someone takes it. */
    public UUID relic;
    public int relicDim;
    public double relicX, relicY, relicZ;

    public CometData() {
        super(NAME);
    }

    public CometData(String name) {
        super(name);
    }

    public static CometData get() {
        World overworld = DimensionManager.getWorld(0);
        MapStorage storage = overworld.getMapStorage();
        CometData data = (CometData) storage.getOrLoadData(CometData.class, NAME);
        if (data == null) {
            data = new CometData();
            storage.setData(NAME, data);
        }
        return data;
    }

    public boolean isVisible() {
        return phase == Phase.PASSING || phase == Phase.CRACKING || phase == Phase.DEPARTING;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        int ordinal = nbt.getInteger("phase");
        phase = ordinal >= 0 && ordinal < Phase.values().length ? Phase.values()[ordinal] : Phase.DORMANT;
        clock = nbt.getLong("clock");
        dimension = nbt.getInteger("dim");
        x = nbt.getDouble("x");
        y = nbt.getDouble("y");
        z = nbt.getDouble("z");
        vx = nbt.getDouble("vx");
        vy = nbt.getDouble("vy");
        vz = nbt.getDouble("vz");
        space = nbt.getBoolean("space");
        damage = nbt.getFloat("damage");
        flares = nbt.getInteger("flares");
        stageTicks = nbt.getInteger("stageTicks");
        lostTicks = nbt.getInteger("lostTicks");
        breaker = nbt.getString("breaker");
        relic = nbt.hasUniqueId("relic") ? nbt.getUniqueId("relic") : null;
        relicDim = nbt.getInteger("relicDim");
        relicX = nbt.getDouble("relicX");
        relicY = nbt.getDouble("relicY");
        relicZ = nbt.getDouble("relicZ");
        // A departure interrupted by a shutdown simply finished while nobody was looking.
        if (phase == Phase.DEPARTING) {
            phase = Phase.DORMANT;
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        nbt.setInteger("phase", phase.ordinal());
        nbt.setLong("clock", clock);
        nbt.setInteger("dim", dimension);
        nbt.setDouble("x", x);
        nbt.setDouble("y", y);
        nbt.setDouble("z", z);
        nbt.setDouble("vx", vx);
        nbt.setDouble("vy", vy);
        nbt.setDouble("vz", vz);
        nbt.setBoolean("space", space);
        nbt.setFloat("damage", damage);
        nbt.setInteger("flares", flares);
        nbt.setInteger("stageTicks", stageTicks);
        nbt.setInteger("lostTicks", lostTicks);
        nbt.setString("breaker", breaker);
        if (relic != null) {
            nbt.setUniqueId("relic", relic);
            nbt.setInteger("relicDim", relicDim);
            nbt.setDouble("relicX", relicX);
            nbt.setDouble("relicY", relicY);
            nbt.setDouble("relicZ", relicZ);
        }
        return nbt;
    }
}
