package com.matoon.eternity.world;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.Constants;

import java.util.HashSet;
import java.util.Set;

/**
 * Warlock's home, and what he brought back to it, once per world: the Power Stone he took when
 * someone fell to him in the wreck of Tamarata (held here, as the one true stone, while it is
 * nobody's to carry), and how far the story on his station has gone.
 */
public class SovereignData extends WorldSavedData {

    public static final String NAME = "eternity_sovereign";

    public enum Station {
        /** Nobody has been taken there yet: he has not fled home with anything. */
        UNSEEN,
        /** He has gone home with the stone; the station waits, mostly empty. */
        ABANDONED,
        /** Someone has found him there, with her. */
        CONFRONTED,
        /** After: she is dead, and the Magus has gone with the stone. */
        AFTER
    }

    public Station station = Station.UNSEEN;
    /** The stone Warlock carried home, while it is his; empty once it has gone on. */
    public ItemStack stone = ItemStack.EMPTY;
    /** Where he took it from: the Champion's brow, or someone who fell. */
    public String takenFrom = "";
    /** Whether the Magus has it (he does not give things back). */
    public boolean magusHasPower, magusHasMind;
    public boolean motherDead;
    /** The Magus's last fight, for the gauntlet. */
    public enum Fight {
        /** Not begun: he has not yet had the Mind Stone, or not yet gone. */
        NONE,
        /** He has gone, and waits for someone to have a gauntlet with all their stones in it. */
        HUNTING,
        /** Under way. */
        FIGHTING,
        /** He is dust. */
        WON,
        /** He snapped: all six are his, and he is gone with them. */
        LOST,
        /** Odin took it: both of them beaten, and the gauntlet gone up the Bifrost with him. */
        TAKEN
    }

    public Fight fight = Fight.NONE;
    /** Whose fight it is. */
    @javax.annotation.Nullable
    public java.util.UUID fightPlayer;
    /** The gauntlet, while he has it (empty while they do). */
    public ItemStack fightGauntlet = ItemStack.EMPTY;
    /** After he snapped: their gauntlet, with their own stones, for them when they wake. */
    public ItemStack giveBack = ItemStack.EMPTY;
    /** Whom it is to be given back to (null: whose fight it was, as before this was kept). */
    @javax.annotation.Nullable
    public java.util.UUID giveBackTo;
    /** Not before this world time will he come for them again. */
    public long nextAmbush;
    /** Warlock died in it, at the Magus's hands: he does not come again. */
    public boolean warlockFell;
    /** Odin has come down into the fight (he comes once). */
    public boolean odinCame;
    /** When it was won (world time), so what comes after can wait until it has been taken in. */
    public long fightSettled;
    /** The station's doors that have been opened, by name (SovereignStation.doors). They stay open. */
    public final Set<String> opened = new HashSet<>();

    public SovereignData() {
        super(NAME);
    }

    public SovereignData(String name) {
        super(name);
    }

    public static SovereignData get() {
        World overworld = DimensionManager.getWorld(0);
        MapStorage storage = overworld.getMapStorage();
        SovereignData data = (SovereignData) storage.getOrLoadData(SovereignData.class, NAME);
        if (data == null) {
            data = new SovereignData();
            storage.setData(NAME, data);
        }
        return data;
    }

    /** Whether Warlock has fled home with the Power Stone and still has it. */
    public boolean warlockHasStone() {
        return !stone.isEmpty();
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        int s = nbt.getInteger("station");
        station = s >= 0 && s < Station.values().length ? Station.values()[s] : Station.UNSEEN;
        stone = nbt.hasKey("stone") ? new ItemStack(nbt.getCompoundTag("stone")) : ItemStack.EMPTY;
        takenFrom = nbt.getString("takenFrom");
        magusHasPower = nbt.getBoolean("magusHasPower");
        magusHasMind = nbt.getBoolean("magusHasMind");
        motherDead = nbt.getBoolean("motherDead");
        int f = nbt.getInteger("fight");
        fight = f >= 0 && f < Fight.values().length ? Fight.values()[f] : Fight.NONE;
        fightPlayer = nbt.hasUniqueId("fightPlayer") ? nbt.getUniqueId("fightPlayer") : null;
        fightGauntlet = nbt.hasKey("fightGauntlet") ? new ItemStack(nbt.getCompoundTag("fightGauntlet")) : ItemStack.EMPTY;
        giveBack = nbt.hasKey("giveBack") ? new ItemStack(nbt.getCompoundTag("giveBack")) : ItemStack.EMPTY;
        giveBackTo = nbt.hasUniqueId("giveBackTo") ? nbt.getUniqueId("giveBackTo") : null;
        nextAmbush = nbt.getLong("nextAmbush");
        warlockFell = nbt.getBoolean("warlockFell");
        odinCame = nbt.getBoolean("odinCame");
        fightSettled = nbt.getLong("fightSettled");
        opened.clear();
        NBTTagList list = nbt.getTagList("opened", Constants.NBT.TAG_STRING);
        for (int i = 0; i < list.tagCount(); i++) {
            opened.add(list.getStringTagAt(i));
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        nbt.setInteger("station", station.ordinal());
        if (!stone.isEmpty()) {
            nbt.setTag("stone", stone.writeToNBT(new NBTTagCompound()));
        }
        nbt.setString("takenFrom", takenFrom);
        nbt.setBoolean("magusHasPower", magusHasPower);
        nbt.setBoolean("magusHasMind", magusHasMind);
        nbt.setBoolean("motherDead", motherDead);
        nbt.setInteger("fight", fight.ordinal());
        if (fightPlayer != null) {
            nbt.setUniqueId("fightPlayer", fightPlayer);
        }
        if (!fightGauntlet.isEmpty()) {
            nbt.setTag("fightGauntlet", fightGauntlet.writeToNBT(new NBTTagCompound()));
        }
        if (!giveBack.isEmpty()) {
            nbt.setTag("giveBack", giveBack.writeToNBT(new NBTTagCompound()));
        }
        if (giveBackTo != null) {
            nbt.setUniqueId("giveBackTo", giveBackTo);
        }
        nbt.setLong("nextAmbush", nextAmbush);
        nbt.setBoolean("warlockFell", warlockFell);
        nbt.setBoolean("odinCame", odinCame);
        nbt.setLong("fightSettled", fightSettled);
        NBTTagList list = new NBTTagList();
        for (String door : opened) {
            list.appendTag(new NBTTagString(door));
        }
        nbt.setTag("opened", list);
        return nbt;
    }
}
