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
 * The Champion's story, which happens once per world: whether he is still on Tamarata fighting
 * whatever comes, whether his planet is whole, whether he still wears the Power Stone and who
 * has it now, and what he thinks of everyone who has stood in front of him.
 */
public class ChampionData extends WorldSavedData {

    public static final String NAME = "eternity_champion";

    public enum Phase {
        /** On Tamarata, in his Colosseum, fighting the armies that still come for him. */
        RESIDENT,
        /** Fighting someone worth his time. */
        DUEL,
        /** His blow has broken the planet, and it is coming apart. */
        BREAKING,
        /** Adrift in what is left of it, unable to go anywhere. */
        STRANDED,
        /** Gone, in someone's rocket. He never comes back. */
        DEPARTED
    }

    public Phase phase = Phase.RESIDENT;
    /** Whether the Power Stone has left his brow, and who has it now. */
    public boolean stoneTaken;
    public String stoneHolder = "";
    /** Whether the planet is broken. Once it is, it stays broken. */
    public boolean planetDestroyed;
    /** How far the breaking has got, in ticks, and where it started. */
    public int breakTicks;
    public double breakX, breakY, breakZ;
    /** How each person who has fought him has earned his respect, and how badly they have made him lose his temper. */
    public final Map<UUID, Integer> respect = new HashMap<>();
    public final Map<UUID, Float> rage = new HashMap<>();

    public ChampionData() {
        super(NAME);
    }

    public ChampionData(String name) {
        super(name);
    }

    public static ChampionData get() {
        World overworld = DimensionManager.getWorld(0);
        MapStorage storage = overworld.getMapStorage();
        ChampionData data = (ChampionData) storage.getOrLoadData(ChampionData.class, NAME);
        if (data == null) {
            data = new ChampionData();
            storage.setData(NAME, data);
        }
        return data;
    }

    public int respect(UUID id) {
        return respect.getOrDefault(id, 0);
    }

    public float rage(UUID id) {
        return rage.getOrDefault(id, 0F);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        int p = nbt.getInteger("phase");
        phase = p >= 0 && p < Phase.values().length ? Phase.values()[p] : Phase.RESIDENT;
        stoneTaken = nbt.getBoolean("stoneTaken");
        stoneHolder = nbt.getString("stoneHolder");
        planetDestroyed = nbt.getBoolean("planetDestroyed");
        breakTicks = nbt.getInteger("breakTicks");
        breakX = nbt.getDouble("breakX");
        breakY = nbt.getDouble("breakY");
        breakZ = nbt.getDouble("breakZ");
        respect.clear();
        rage.clear();
        NBTTagList list = nbt.getTagList("fighters", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            UUID id = tag.getUniqueId("id");
            respect.put(id, tag.getInteger("respect"));
            rage.put(id, tag.getFloat("rage"));
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        nbt.setInteger("phase", phase.ordinal());
        nbt.setBoolean("stoneTaken", stoneTaken);
        nbt.setString("stoneHolder", stoneHolder);
        nbt.setBoolean("planetDestroyed", planetDestroyed);
        nbt.setInteger("breakTicks", breakTicks);
        nbt.setDouble("breakX", breakX);
        nbt.setDouble("breakY", breakY);
        nbt.setDouble("breakZ", breakZ);
        NBTTagList list = new NBTTagList();
        Map<UUID, NBTTagCompound> fighters = new HashMap<>();
        for (UUID id : respect.keySet()) {
            fighters.computeIfAbsent(id, ChampionData::fighter).setInteger("respect", respect.get(id));
        }
        for (UUID id : rage.keySet()) {
            fighters.computeIfAbsent(id, ChampionData::fighter).setFloat("rage", rage.get(id));
        }
        for (NBTTagCompound tag : fighters.values()) {
            list.appendTag(tag);
        }
        nbt.setTag("fighters", list);
        return nbt;
    }

    private static NBTTagCompound fighter(UUID id) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setUniqueId("id", id);
        return tag;
    }
}
