package com.matoon.eternity.world;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.Constants;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Eternity's persistent state. He exists here, not as a saved entity: the body is
 * only conjured when someone is close enough to see it.
 */
public class EternityData extends WorldSavedData {

    public static final String NAME = "eternity";

    public enum Phase {
        /** Wandering the worlds. */
        ROAMING,
        /** Pursuing {@link #target}. */
        HUNTING,
        /** The finale is under way. */
        DYING,
        /** Gone. Either the universe was saved, or it has already ended. */
        SLAIN,
        /** He died uncontained. Reality is coming apart. */
        UNMAKING,
        /** Nothing is left. */
        VOID
    }

    public Phase phase = Phase.ROAMING;
    public int dimension = 0;
    public double x, y = 150, z;
    /** Wander heading in radians. */
    public float heading;
    /** Remaining health as a fraction of max. */
    public float health = 1F;

    /** Ticks that have passed while at least one player was online. Drives every timer. */
    public long clock;
    public long nextRiftAt = -1;

    public UUID target;
    public long huntUntil;

    public long slainAt = -1;
    /** Clock time reality began to unmake, while {@link Phase#UNMAKING}. */
    public long doomAt = -1;
    public final Map<UUID, Mark> marks = new HashMap<>();
    /** Just created for a new world, and not yet placed anywhere. Never saved. */
    public boolean fresh;

    /** What Eternity knows about one player. */
    public static class Mark {
        /** Every stone type this player has ever held. */
        public int everMask;
        /** Clock time Eternity will come for them, or -1. */
        public long encounterAt = -1;
        public long lastEncounter = -1;

        NBTTagCompound write() {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("ever", everMask);
            tag.setLong("encounterAt", encounterAt);
            tag.setLong("lastEncounter", lastEncounter);
            return tag;
        }

        static Mark read(NBTTagCompound tag) {
            Mark mark = new Mark();
            mark.everMask = tag.getInteger("ever");
            mark.encounterAt = tag.getLong("encounterAt");
            mark.lastEncounter = tag.getLong("lastEncounter");
            return mark;
        }
    }

    public EternityData() {
        super(NAME);
    }

    public EternityData(String name) {
        super(name);
    }

    public static EternityData get() {
        World overworld = DimensionManager.getWorld(0);
        MapStorage storage = overworld.getMapStorage();
        EternityData data = (EternityData) storage.getOrLoadData(EternityData.class, NAME);
        if (data == null) {
            data = new EternityData();
            data.fresh = true;
            storage.setData(NAME, data);
        }
        return data;
    }

    public Mark mark(UUID player) {
        return marks.computeIfAbsent(player, id -> new Mark());
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        int ordinal = nbt.getInteger("phase");
        phase = ordinal >= 0 && ordinal < Phase.values().length ? Phase.values()[ordinal] : Phase.ROAMING;
        dimension = nbt.getInteger("dim");
        x = nbt.getDouble("x");
        y = nbt.getDouble("y");
        z = nbt.getDouble("z");
        heading = nbt.getFloat("heading");
        health = nbt.hasKey("health") ? nbt.getFloat("health") : 1F;
        clock = nbt.getLong("clock");
        nextRiftAt = nbt.getLong("nextRiftAt");
        target = nbt.hasUniqueId("target") ? nbt.getUniqueId("target") : null;
        huntUntil = nbt.getLong("huntUntil");
        slainAt = nbt.getLong("slainAt");
        marks.clear();
        NBTTagList list = nbt.getTagList("marks", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            marks.put(tag.getUniqueId("id"), Mark.read(tag));
        }
        doomAt = nbt.hasKey("doomAt") ? nbt.getLong("doomAt") : -1;
        // A finale interrupted by a shutdown was never contained.
        if (phase == Phase.DYING) {
            phase = Phase.UNMAKING;
            doomAt = clock;
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        nbt.setInteger("phase", phase.ordinal());
        nbt.setInteger("dim", dimension);
        nbt.setDouble("x", x);
        nbt.setDouble("y", y);
        nbt.setDouble("z", z);
        nbt.setFloat("heading", heading);
        nbt.setFloat("health", health);
        nbt.setLong("clock", clock);
        nbt.setLong("nextRiftAt", nextRiftAt);
        if (target != null) {
            nbt.setUniqueId("target", target);
        }
        nbt.setLong("huntUntil", huntUntil);
        nbt.setLong("slainAt", slainAt);
        nbt.setLong("doomAt", doomAt);
        NBTTagList list = new NBTTagList();
        for (Map.Entry<UUID, Mark> entry : marks.entrySet()) {
            NBTTagCompound tag = entry.getValue().write();
            tag.setUniqueId("id", entry.getKey());
            list.appendTag(tag);
        }
        nbt.setTag("marks", list);
        return nbt;
    }
}
