package com.matoon.eternity.stones;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.util.Constants;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Everyone the snap has turned to dust, kept with the world until a snap brings them back: each creature whole, as it
 * was the moment it went; each player by where they fell, with whatever they carried and their experience, which the
 * dust kept rather than scattering.
 * <p>
 * A player called back while dead or away stays here, marked called, until they next stand in the world.
 */
public class TheDusted extends WorldSavedData {

    private static final String NAME = "eternity_dusted";
    /** More creatures than this are let go rather than kept: a world of them would weigh on every save. */
    static final int LIMIT = 4096;

    private final List<NBTTagCompound> records = new ArrayList<>();

    public TheDusted(String name) {
        super(name);
    }

    public static TheDusted get(MinecraftServer server) {
        MapStorage storage = server.getWorld(0).getMapStorage();
        TheDusted data = (TheDusted) storage.getOrLoadData(TheDusted.class, NAME);
        if (data == null) {
            data = new TheDusted(NAME);
            storage.setData(NAME, data);
        }
        return data;
    }

    void add(NBTTagCompound record) {
        if (record.getBoolean("Player") || creatures() < LIMIT) {
            records.add(record);
            markDirty();
        }
    }

    /** Everyone waiting to come back, for a snap to take them all. */
    List<NBTTagCompound> takeAll() {
        List<NBTTagCompound> all = new ArrayList<>(records);
        records.clear();
        markDirty();
        return all;
    }

    /** A player's record, once they have been called back, taken when they can come. */
    NBTTagCompound takeCalled(String uuid) {
        for (Iterator<NBTTagCompound> it = records.iterator(); it.hasNext(); ) {
            NBTTagCompound record = it.next();
            if (record.getBoolean("Player") && record.getBoolean("Called") && uuid.equals(record.getString("UUID"))) {
                it.remove();
                markDirty();
                return record;
            }
        }
        return null;
    }

    /** Players who could not come yet, kept for later. */
    void keep(NBTTagCompound record) {
        records.add(record);
        markDirty();
    }

    int size() {
        return records.size();
    }

    private int creatures() {
        int count = 0;
        for (NBTTagCompound record : records) {
            if (!record.getBoolean("Player")) {
                count++;
            }
        }
        return count;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        records.clear();
        NBTTagList list = nbt.getTagList("Dusted", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            records.add(list.getCompoundTagAt(i));
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        NBTTagList list = new NBTTagList();
        for (NBTTagCompound record : records) {
            list.appendTag(record);
        }
        nbt.setTag("Dusted", list);
        return nbt;
    }
}
