package com.matoon.eternity.story;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Where the story stands. Chapter 1 happens once to the whole world (there is one village, one
 * barrow and one cube); chapter 2 likewise in the one vault. What each player has seen, said and
 * become (who Warlock thinks they are, whether the Aether is in them) is kept per player.
 */
public class StoryData extends WorldSavedData {

    public static final String NAME = "eternity_story";

    /** Chapter 1, the oath, step by step. */
    public enum Oath {
        /** Nothing yet: the ward is holding, as far as anyone knows. */
        DORMANT,
        /** Pieces of the north are coming loose and landing across the world. */
        LEAKING,
        /** A woman is lying in a piece of the north somewhere, and someone is with her. */
        RAGNA,
        /** Ragna is dead; the player has been told where to look. */
        SEEKING,
        /** The village has been found, and Hrafn. */
        VILLAGE,
        /** The cube is lifted; the dead are up. */
        BROKEN,
        /** The barrow has shut behind the player, until dawn. */
        SEALED,
        /** The stranger has knelt by Hrafn; the cube shows the way on. */
        MOURNED,
        /** The cube has taken someone to the vault. */
        DONE,
        /** The Tesseract was claimed before any of this could happen; the chapter is passed over. */
        SKIPPED
    }

    /** Chapter 5, the Gardener, step by step. */
    public enum Garden {
        /** Nobody has gone looking for it yet. */
        UNSEEN,
        /** It grows, and he tends it, and nobody has spoken to him. */
        GROWING,
        /** He has tried to kill someone who came for his stone; the hedge is up. */
        HEDGED,
        /** He has bound the stone into himself and is waiting for them to decide. */
        BOUND,
        /** They left it in him. He tends his garden again, with it in his chest. */
        SPARED,
        /** It was taken out of him. */
        TAKEN
    }

    /** What Adam Warlock makes of a player. */
    public enum Stance {
        UNMET, TRUCE, RIVAL, BETRAYED, REFUSED, ALLY
    }

    /** One player's story. */
    public static final class Player {
        public final Set<String> beats = new HashSet<>();
        public Stance warlock = Stance.UNMET;
        public int trust;
        /** The Aether is in them. */
        public boolean aether;
        /** The stones they held when they told Warlock they would stop. */
        public int truceMask;
        /** When Warlock will come for them for breaking their word, or -1. */
        public long betrayalAt = -1;

        public boolean seen(String beat) {
            return beats.contains(beat);
        }
    }

    // ------------------------------------------------------------------ chapter 1
    public Oath oath = Oath.DORMANT;
    /** The world time the first leak may come, once the ward starts failing; -1 until then. */
    public long leaksFrom = -1;
    public long nextLeak = -1;
    public int leaks;
    /** Leaks that have reached as far as someone's home. */
    public int taken;
    /** The middle of the Norse village the cube is kept under. */
    @Nullable
    public BlockPos village;
    /** The foot of the barrow's door, on the ground, and which way the door faces (a horizontal index). */
    @Nullable
    public BlockPos barrow;
    public int barrowFacing;
    public boolean dressed;
    /** Where Ragna came down. */
    @Nullable
    public BlockPos ragna;
    /** Which keepers' words have been read (a bit for each of the five). */
    public int linesFound;
    /** How many lines of the oath have been spoken at the inner door, in order. */
    public int spoken;
    /** How many times the wrong words have been given there: Hrafn's help gets plainer each time. */
    public int misses;
    public boolean outerOpen, innerOpen, wardLit = true;
    /** When the barrow opens again after it seals. */
    public long reopenAt = -1;
    /** Whom the chapter is following: the player who saw Ragna die, and who carries the cube. */
    @Nullable
    public UUID lead;
    public final Set<String> beats = new HashSet<>();

    // ------------------------------------------------------------------ chapter 2
    public boolean aetherTaken, flameSpent;

    // ------------------------------------------------------------------ chapter 5
    public Garden garden = Garden.UNSEEN;
    /** The middle of the Gardener's garden, the dimension it is in, and which way it is laid out. */
    @Nullable
    public BlockPos gardenAt;
    public int gardenDim, gardenFacing;
    public boolean gardenBuilt;
    /**
     * How his hedge was got through, once it has been: "power", "champion", "casket", "reality" or "soul".
     * Empty while it still stands whole. Once through, his roots inside stop throwing anyone out.
     */
    public String gardenBreach = "";

    private final Map<UUID, Player> players = new HashMap<>();

    public StoryData() {
        super(NAME);
    }

    public StoryData(String name) {
        super(name);
    }

    public static StoryData get() {
        World overworld = DimensionManager.getWorld(0);
        MapStorage storage = overworld.getMapStorage();
        StoryData data = (StoryData) storage.getOrLoadData(StoryData.class, NAME);
        if (data == null) {
            data = new StoryData();
            storage.setData(NAME, data);
        }
        return data;
    }

    public Player player(UUID id) {
        return players.computeIfAbsent(id, k -> new Player());
    }

    public Map<UUID, Player> players() {
        return players;
    }

    public boolean seen(String beat) {
        return beats.contains(beat);
    }

    /** Marks a world beat seen; true if it had not been. */
    public boolean see(String beat) {
        boolean fresh = beats.add(beat);
        if (fresh) {
            markDirty();
        }
        return fresh;
    }

    public boolean found(int keeper) {
        return (linesFound & 1 << keeper) != 0;
    }

    public void forget() {
        oath = Oath.DORMANT;
        leaksFrom = nextLeak = reopenAt = -1;
        leaks = taken = linesFound = spoken = misses = 0;
        village = barrow = ragna = null;
        barrowFacing = 0;
        dressed = outerOpen = innerOpen = false;
        wardLit = true;
        lead = null;
        beats.clear();
        aetherTaken = flameSpent = false;
        garden = Garden.UNSEEN;
        gardenAt = null;
        gardenDim = gardenFacing = 0;
        gardenBuilt = false;
        gardenBreach = "";
        players.clear();
        markDirty();
    }

    // ------------------------------------------------------------------ saving

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        int o = nbt.getInteger("oath");
        oath = o >= 0 && o < Oath.values().length ? Oath.values()[o] : Oath.DORMANT;
        leaksFrom = nbt.hasKey("leaksFrom") ? nbt.getLong("leaksFrom") : -1;
        nextLeak = nbt.hasKey("nextLeak") ? nbt.getLong("nextLeak") : -1;
        leaks = nbt.getInteger("leaks");
        taken = nbt.getInteger("taken");
        village = nbt.hasKey("village") ? BlockPos.fromLong(nbt.getLong("village")) : null;
        barrow = nbt.hasKey("barrow") ? BlockPos.fromLong(nbt.getLong("barrow")) : null;
        barrowFacing = nbt.getInteger("barrowFacing");
        ragna = nbt.hasKey("ragna") ? BlockPos.fromLong(nbt.getLong("ragna")) : null;
        dressed = nbt.getBoolean("dressed");
        linesFound = nbt.getInteger("linesFound");
        spoken = nbt.getInteger("spoken");
        misses = nbt.getInteger("misses");
        outerOpen = nbt.getBoolean("outerOpen");
        innerOpen = nbt.getBoolean("innerOpen");
        wardLit = !nbt.hasKey("wardLit") || nbt.getBoolean("wardLit");
        reopenAt = nbt.hasKey("reopenAt") ? nbt.getLong("reopenAt") : -1;
        lead = nbt.hasUniqueId("lead") ? nbt.getUniqueId("lead") : null;
        readSet(nbt.getTagList("beats", Constants.NBT.TAG_STRING), beats);
        aetherTaken = nbt.getBoolean("aetherTaken");
        flameSpent = nbt.getBoolean("flameSpent");
        int g = nbt.getInteger("garden");
        garden = g >= 0 && g < Garden.values().length ? Garden.values()[g] : Garden.UNSEEN;
        gardenAt = nbt.hasKey("gardenAt") ? BlockPos.fromLong(nbt.getLong("gardenAt")) : null;
        gardenDim = nbt.getInteger("gardenDim");
        gardenFacing = nbt.getInteger("gardenFacing");
        gardenBuilt = nbt.getBoolean("gardenBuilt");
        gardenBreach = nbt.getString("gardenBreach");
        players.clear();
        NBTTagList list = nbt.getTagList("players", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            if (!tag.hasUniqueId("id")) {
                continue;
            }
            Player p = player(tag.getUniqueId("id"));
            readSet(tag.getTagList("beats", Constants.NBT.TAG_STRING), p.beats);
            int s = tag.getInteger("warlock");
            p.warlock = s >= 0 && s < Stance.values().length ? Stance.values()[s] : Stance.UNMET;
            p.trust = tag.getInteger("trust");
            p.aether = tag.getBoolean("aether");
            p.truceMask = tag.getInteger("truceMask");
            p.betrayalAt = tag.hasKey("betrayalAt") ? tag.getLong("betrayalAt") : -1;
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        nbt.setInteger("oath", oath.ordinal());
        nbt.setLong("leaksFrom", leaksFrom);
        nbt.setLong("nextLeak", nextLeak);
        nbt.setInteger("leaks", leaks);
        nbt.setInteger("taken", taken);
        if (village != null) {
            nbt.setLong("village", village.toLong());
        }
        if (barrow != null) {
            nbt.setLong("barrow", barrow.toLong());
        }
        nbt.setInteger("barrowFacing", barrowFacing);
        if (ragna != null) {
            nbt.setLong("ragna", ragna.toLong());
        }
        nbt.setBoolean("dressed", dressed);
        nbt.setInteger("linesFound", linesFound);
        nbt.setInteger("spoken", spoken);
        nbt.setInteger("misses", misses);
        nbt.setBoolean("outerOpen", outerOpen);
        nbt.setBoolean("innerOpen", innerOpen);
        nbt.setBoolean("wardLit", wardLit);
        nbt.setLong("reopenAt", reopenAt);
        if (lead != null) {
            nbt.setUniqueId("lead", lead);
        }
        nbt.setTag("beats", writeSet(beats));
        nbt.setBoolean("aetherTaken", aetherTaken);
        nbt.setBoolean("flameSpent", flameSpent);
        nbt.setInteger("garden", garden.ordinal());
        if (gardenAt != null) {
            nbt.setLong("gardenAt", gardenAt.toLong());
        }
        nbt.setInteger("gardenDim", gardenDim);
        nbt.setInteger("gardenFacing", gardenFacing);
        nbt.setBoolean("gardenBuilt", gardenBuilt);
        nbt.setString("gardenBreach", gardenBreach);
        NBTTagList list = new NBTTagList();
        for (Map.Entry<UUID, Player> e : players.entrySet()) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setUniqueId("id", e.getKey());
            Player p = e.getValue();
            tag.setTag("beats", writeSet(p.beats));
            tag.setInteger("warlock", p.warlock.ordinal());
            tag.setInteger("trust", p.trust);
            tag.setBoolean("aether", p.aether);
            tag.setInteger("truceMask", p.truceMask);
            tag.setLong("betrayalAt", p.betrayalAt);
            list.appendTag(tag);
        }
        nbt.setTag("players", list);
        return nbt;
    }

    private static void readSet(NBTTagList list, Set<String> into) {
        into.clear();
        for (int i = 0; i < list.tagCount(); i++) {
            into.add(list.getStringTagAt(i));
        }
    }

    private static NBTTagList writeSet(Set<String> set) {
        NBTTagList list = new NBTTagList();
        for (String s : set) {
            list.appendTag(new NBTTagString(s));
        }
        return list;
    }
}
