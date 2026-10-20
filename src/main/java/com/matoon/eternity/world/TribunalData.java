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

/** The Living Tribunal's memory: whom he is coming for, whom he is judging, and whom he has cast out. */
public class TribunalData extends WorldSavedData {

    public static final String NAME = "eternity_tribunal";

    /** Ticks that have passed while at least one player was online. */
    public long clock;
    /** Clock time he comes for {@link #target}, or -1. */
    public long summonAt = -1;
    public UUID target;
    /** Whether the target is in the Star Chamber awaiting judgement. */
    public boolean judging;
    /** How many times he has been drawn into an Astral Regulator in this world. */
    public int absorbed;

    /** Where to send them home to. */
    public int returnDim;
    public double returnX, returnY, returnZ;

    /** Everyone banished from this world, by id, with their name for operators. */
    public final Map<UUID, String> banished = new HashMap<>();

    public TribunalData() {
        super(NAME);
    }

    public TribunalData(String name) {
        super(name);
    }

    public static TribunalData get() {
        World overworld = DimensionManager.getWorld(0);
        MapStorage storage = overworld.getMapStorage();
        TribunalData data = (TribunalData) storage.getOrLoadData(TribunalData.class, NAME);
        if (data == null) {
            data = new TribunalData();
            storage.setData(NAME, data);
        }
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        clock = nbt.getLong("clock");
        summonAt = nbt.getLong("summonAt");
        target = nbt.hasUniqueId("target") ? nbt.getUniqueId("target") : null;
        judging = nbt.getBoolean("judging");
        // Worlds saved before the count existed kept a flag instead.
        absorbed = nbt.hasKey("absorbed", 1) ? (nbt.getBoolean("absorbed") ? 1 : 0) : nbt.getInteger("absorbed");
        returnDim = nbt.getInteger("returnDim");
        returnX = nbt.getDouble("returnX");
        returnY = nbt.getDouble("returnY");
        returnZ = nbt.getDouble("returnZ");
        banished.clear();
        NBTTagList list = nbt.getTagList("banished", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            banished.put(tag.getUniqueId("id"), tag.getString("name"));
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        nbt.setLong("clock", clock);
        nbt.setLong("summonAt", summonAt);
        if (target != null) {
            nbt.setUniqueId("target", target);
        }
        nbt.setBoolean("judging", judging);
        nbt.setInteger("absorbed", absorbed);
        nbt.setInteger("returnDim", returnDim);
        nbt.setDouble("returnX", returnX);
        nbt.setDouble("returnY", returnY);
        nbt.setDouble("returnZ", returnZ);
        NBTTagList list = new NBTTagList();
        for (Map.Entry<UUID, String> entry : banished.entrySet()) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setUniqueId("id", entry.getKey());
            tag.setString("name", entry.getValue());
            list.appendTag(tag);
        }
        nbt.setTag("banished", list);
        return nbt;
    }
}
