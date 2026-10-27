package com.matoon.eternity.world;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.DimensionManager;

/**
 * What has become of Odin's Vault: whether Mjolnir and the Tesseract have been set out on their
 * stands (once ever), whether the Casket of Ancient Winters is on its pedestal or out in the
 * world, and when the Destroyer will be standing in its niche again after it last fell.
 */
public class VaultData extends WorldSavedData {

    public static final String NAME = "eternity_vault";

    public boolean mjolnirSet;
    public boolean tesseractSet;
    public boolean casketTaken;
    public String casketTakenBy = "";
    /** The overworld's total time when the Destroyer is mended and back in its niche; 0 if it never fell. */
    public long destroyerBack;

    public VaultData() {
        super(NAME);
    }

    public VaultData(String name) {
        super(name);
    }

    public static VaultData get() {
        World overworld = DimensionManager.getWorld(0);
        MapStorage storage = overworld.getMapStorage();
        VaultData data = (VaultData) storage.getOrLoadData(VaultData.class, NAME);
        if (data == null) {
            data = new VaultData();
            storage.setData(NAME, data);
        }
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        mjolnirSet = tag.getBoolean("mjolnirSet");
        tesseractSet = tag.getBoolean("tesseractSet");
        casketTaken = tag.getBoolean("casketTaken");
        casketTakenBy = tag.getString("casketTakenBy");
        destroyerBack = tag.getLong("destroyerBack");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        tag.setBoolean("mjolnirSet", mjolnirSet);
        tag.setBoolean("tesseractSet", tesseractSet);
        tag.setBoolean("casketTaken", casketTaken);
        tag.setString("casketTakenBy", casketTakenBy);
        tag.setLong("destroyerBack", destroyerBack);
        return tag;
    }
}
