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
 * The In-Betweener's memory: whether his glass is whole, whether the gem is still on his
 * brow and who took it, how each visitor left him, and whom he will not see for a while.
 */
public class InBetweenerData extends WorldSavedData {

    public static final String NAME = "eternity_inbetweener";

    /**
     * How someone left things with him, which decides how he greets them next time: met him
     * and went, answered his paradoxes, died in his glass, stole his eye, made him doubt, or
     * walked into his glass and walked out again.
     */
    public enum Memory { NONE, MET, WORTHY, FOUGHT, THIEF, DOUBTER, LURED }

    /** Ticks that have passed while at least one player was online. */
    public long clock;
    /** Whether his glass is broken. There is only one of him and only one glass; once broken, it stays broken. */
    public boolean broken;
    /**
     * Whether his empty body has been cast off into the Nexus, and where it is drifting: from
     * where he fell, starting at driftStart, out to the drift point, where it hangs.
     */
    public boolean adrift;
    public double driftX, driftY, driftZ, driftFromX, driftFromY, driftFromZ;
    public long driftStart;
    /** Clock time Order and Chaos arrive, or -1. */
    public long wrathAt = -1;
    /** Whether the Soul Gem has left his brow, and who has it now. */
    public boolean gemTaken;
    public String gemHolder = "";
    /** Whether the In-Betweener, Master Order or Lord Chaos has been drawn into an Astral Regulator. Those never come back. */
    public boolean inBetweenerAbsorbed, orderAbsorbed, chaosAbsorbed;
    public final Map<UUID, Memory> memories = new HashMap<>();
    /** Clock time until which he will not see someone. */
    public final Map<UUID, Long> castOut = new HashMap<>();

    public InBetweenerData() {
        super(NAME);
    }

    public InBetweenerData(String name) {
        super(name);
    }

    public static InBetweenerData get() {
        World overworld = DimensionManager.getWorld(0);
        MapStorage storage = overworld.getMapStorage();
        InBetweenerData data = (InBetweenerData) storage.getOrLoadData(InBetweenerData.class, NAME);
        if (data == null) {
            data = new InBetweenerData();
            storage.setData(NAME, data);
        }
        return data;
    }

    public Memory memory(UUID id) {
        Memory memory = memories.get(id);
        return memory == null ? Memory.NONE : memory;
    }

    public void remember(UUID id, Memory memory) {
        memories.put(id, memory);
        markDirty();
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        clock = nbt.getLong("clock");
        broken = nbt.getBoolean("broken");
        adrift = nbt.getBoolean("adrift");
        driftX = nbt.getDouble("driftX");
        driftY = nbt.getDouble("driftY");
        driftZ = nbt.getDouble("driftZ");
        driftFromX = nbt.getDouble("driftFromX");
        driftFromY = nbt.getDouble("driftFromY");
        driftFromZ = nbt.getDouble("driftFromZ");
        driftStart = nbt.getLong("driftStart");
        wrathAt = nbt.hasKey("wrathAt") ? nbt.getLong("wrathAt") : -1;
        gemTaken = nbt.getBoolean("gemTaken");
        gemHolder = nbt.getString("gemHolder");
        inBetweenerAbsorbed = nbt.getBoolean("inBetweenerAbsorbed");
        orderAbsorbed = nbt.getBoolean("orderAbsorbed");
        chaosAbsorbed = nbt.getBoolean("chaosAbsorbed");
        memories.clear();
        castOut.clear();
        NBTTagList list = nbt.getTagList("visitors", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            UUID id = tag.getUniqueId("id");
            int memory = tag.getInteger("memory");
            if (memory > 0 && memory < Memory.values().length) {
                memories.put(id, Memory.values()[memory]);
            }
            if (tag.hasKey("castOut")) {
                castOut.put(id, tag.getLong("castOut"));
            }
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        nbt.setLong("clock", clock);
        nbt.setBoolean("broken", broken);
        nbt.setBoolean("adrift", adrift);
        nbt.setDouble("driftX", driftX);
        nbt.setDouble("driftY", driftY);
        nbt.setDouble("driftZ", driftZ);
        nbt.setDouble("driftFromX", driftFromX);
        nbt.setDouble("driftFromY", driftFromY);
        nbt.setDouble("driftFromZ", driftFromZ);
        nbt.setLong("driftStart", driftStart);
        nbt.setLong("wrathAt", wrathAt);
        nbt.setBoolean("gemTaken", gemTaken);
        nbt.setString("gemHolder", gemHolder);
        nbt.setBoolean("inBetweenerAbsorbed", inBetweenerAbsorbed);
        nbt.setBoolean("orderAbsorbed", orderAbsorbed);
        nbt.setBoolean("chaosAbsorbed", chaosAbsorbed);
        Map<UUID, NBTTagCompound> visitors = new HashMap<>();
        for (Map.Entry<UUID, Memory> entry : memories.entrySet()) {
            visitor(visitors, entry.getKey()).setInteger("memory", entry.getValue().ordinal());
        }
        for (Map.Entry<UUID, Long> entry : castOut.entrySet()) {
            visitor(visitors, entry.getKey()).setLong("castOut", entry.getValue());
        }
        NBTTagList list = new NBTTagList();
        for (NBTTagCompound tag : visitors.values()) {
            list.appendTag(tag);
        }
        nbt.setTag("visitors", list);
        return nbt;
    }

    private static NBTTagCompound visitor(Map<UUID, NBTTagCompound> visitors, UUID id) {
        return visitors.computeIfAbsent(id, key -> {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setUniqueId("id", key);
            return tag;
        });
    }
}
