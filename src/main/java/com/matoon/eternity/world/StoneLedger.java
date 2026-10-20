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

/**
 * Where each stone was last seen. A stone locked in a chest in an unloaded chunk
 * cannot be scanned, so the Stone Finder points at its last known resting place.
 */
public class StoneLedger extends WorldSavedData {

    public static final String NAME = "eternity_stones";

    /** Keyed by the stone item's registry name. */
    public final Map<String, Sighting> lastSeen = new HashMap<>();

    public static final class Sighting {
        public final int dimension;
        public final double x, y, z;

        public Sighting(int dimension, double x, double y, double z) {
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    public StoneLedger() {
        super(NAME);
    }

    public StoneLedger(String name) {
        super(name);
    }

    public static StoneLedger get() {
        World overworld = DimensionManager.getWorld(0);
        MapStorage storage = overworld.getMapStorage();
        StoneLedger ledger = (StoneLedger) storage.getOrLoadData(StoneLedger.class, NAME);
        if (ledger == null) {
            ledger = new StoneLedger();
            storage.setData(NAME, ledger);
        }
        return ledger;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        lastSeen.clear();
        NBTTagList list = nbt.getTagList("stones", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            lastSeen.put(tag.getString("item"), new Sighting(tag.getInteger("dim"),
                    tag.getDouble("x"), tag.getDouble("y"), tag.getDouble("z")));
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        NBTTagList list = new NBTTagList();
        for (Map.Entry<String, Sighting> entry : lastSeen.entrySet()) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("item", entry.getKey());
            tag.setInteger("dim", entry.getValue().dimension);
            tag.setDouble("x", entry.getValue().x);
            tag.setDouble("y", entry.getValue().y);
            tag.setDouble("z", entry.getValue().z);
            list.appendTag(tag);
        }
        nbt.setTag("stones", list);
        return nbt;
    }
}
