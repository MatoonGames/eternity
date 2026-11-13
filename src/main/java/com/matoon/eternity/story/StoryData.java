package com.matoon.eternity.story;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import com.matoon.eternity.api.event.StoryBeatEvent;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Where the story stands. Chapter 1 happens once to the whole world (there is one village, one
 * barrow and one cube); chapter 2 likewise in the one vault. What each player has seen, said and
 * become (who Warlock thinks they are, whether the Aether is in them) is kept per player.
 * <p>
 * Players who quest together are a fellowship: they share one record of what they have seen, so
 * that what happens to one of them in the story has happened to them all, and Warlock takes them
 * as one. A fellowship forms when players answer or see a conversation through together, or by
 * invitation (CommandFellowship), and lasts until someone leaves it.
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
    /**
     * Beats that are only ever one player's own, fellowship or not: what happened to their body (Death
     * sent them back from the Gardener's roots), their own first sight of somewhere, the Champion at
     * their own side.
     */
    private static final Set<String> PERSONAL = new HashSet<>(java.util.Arrays.asList(
            "gardener_killed", "sent_back", "garden_told", "sov_arrived", Companion.CALLED, Fellowship.OFFERED));

    /**
     * What one player has seen: their fellowship's shared record, and the few beats that are theirs
     * alone, read and written as one set.
     */
    public static final class Beats extends java.util.AbstractSet<String> {
        Set<String> shared = new HashSet<>();
        private final Set<String> own = new HashSet<>();
        /** Whose they are, for StoryBeatEvent. */
        @Nullable
        UUID owner;

        private Set<String> of(Object beat) {
            return PERSONAL.contains(beat) ? own : shared;
        }

        @Override
        public boolean add(String beat) {
            boolean fresh = of(beat).add(beat);
            if (fresh) {
                announce(owner, beat);
            }
            return fresh;
        }

        @Override
        public boolean remove(Object beat) {
            return of(beat).remove(beat);
        }

        @Override
        public boolean contains(Object beat) {
            return of(beat).contains(beat);
        }

        @Override
        public void clear() {
            shared.clear();
            own.clear();
        }

        @Override
        public java.util.Iterator<String> iterator() {
            // Over a copy: what is removed while going through it is removed from where it really lives.
            List<String> all = new ArrayList<>(shared);
            all.addAll(own);
            java.util.Iterator<String> it = all.iterator();
            return new java.util.Iterator<String>() {
                private String last;

                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public String next() {
                    return last = it.next();
                }

                @Override
                public void remove() {
                    Beats.this.remove(last);
                }
            };
        }

        @Override
        public int size() {
            return shared.size() + own.size();
        }
    }

    public static final class Player {
        /** What they have seen: their fellowship's shared record, and what is theirs alone. */
        public final Beats beats = new Beats();
        /** The fellowship they quest in, or null if they quest alone. */
        @Nullable
        public UUID band;
        public Stance warlock = Stance.UNMET;
        public int trust;
        /** The Aether is in them. */
        public boolean aether;
        /** The stones they held when they told Warlock they would stop; -1 until they are next looked at. */
        public int truceMask;
        /** When Warlock will come for them for breaking their word, or -1. */
        public long betrayalAt = -1;
        /** How far they have fallen for Death (Devotion). */
        public int devotion;
        /** What they have killed for her, all told; and of it, what loved them or trusted them, or was one of their own. */
        public int offerings, sacrifices;
        /** How often they have told her no, or let her down. */
        public int spurned;
        /** What she has asked of them (Obsession): how many more, of what (0 anything with a heartbeat, 1 something that loves them), by when. */
        public int demandLeft, demandKind;
        public long demandBy = -1;
        /** Whether what she asked is a race against the rest of them. */
        public boolean demandRace;
        /** They let her down, and she is coming to tell them so. */
        public boolean wronged;
        /** When she next comes for them unasked; -1 until they are far enough gone for her to. */
        public long nextVisit = -1;
        /** The shape Death was in when they last saw her (-1 if never changed in front of them), and how rotten it was. */
        public int deathLook = -1, deathRot;

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
    public final Set<String> beats = new HashSet<String>() {
        @Override
        public boolean add(String beat) {
            boolean fresh = super.add(beat);
            if (fresh) {
                announce(null, beat);
            }
            return fresh;
        }
    };

    /** Set while loading, so beats read back from disk are not announced as new. */
    private static boolean loading;

    /** Tells addons the story has moved on (see api.event.StoryBeatEvent). */
    private static void announce(@Nullable UUID player, String beat) {
        if (!loading) {
            MinecraftForge.EVENT_BUS.post(new StoryBeatEvent(player, beat));
        }
    }

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
        return players.computeIfAbsent(id, k -> {
            Player p = new Player();
            p.beats.owner = k;
            return p;
        });
    }

    public Map<UUID, Player> players() {
        return players;
    }

    // ------------------------------------------------------------------ fellowships

    /** Everyone questing with someone, them first: their fellowship, or just them. */
    public List<UUID> fellows(UUID id) {
        List<UUID> out = new ArrayList<>();
        out.add(id);
        Player p = players.get(id);
        if (p != null && p.band != null) {
            for (Map.Entry<UUID, Player> e : players.entrySet()) {
                if (!e.getKey().equals(id) && p.band.equals(e.getValue().band)) {
                    out.add(e.getKey());
                }
            }
        }
        return out;
    }

    public boolean fellows(UUID a, UUID b) {
        Player pa = players.get(a), pb = players.get(b);
        return a.equals(b) || pa != null && pb != null && pa.band != null && pa.band.equals(pb.band);
    }

    /**
     * Two players quest together from now on: their fellowships become one, and so does what they
     * have seen, each taking up whatever of the story the others had got to. False if they already did.
     */
    public boolean bond(UUID a, UUID b) {
        if (fellows(a, b)) {
            return false;
        }
        Player pa = player(a), pb = player(b);
        UUID band = pa.band != null ? pa.band : pb.band != null ? pb.band : UUID.randomUUID();
        List<Player> members = new ArrayList<>();
        for (Player p : players.values()) {
            if (p == pa || p == pb || pa.band != null && pa.band.equals(p.band) || pb.band != null && pb.band.equals(p.band)) {
                members.add(p);
            }
        }
        Set<String> shared = new HashSet<>();
        for (Player p : members) {
            shared.addAll(p.beats.shared);
        }
        for (Player p : members) {
            p.band = band;
            p.beats.shared = shared;
        }
        bandStance.remove(band);
        settleStances();
        markDirty();
        return true;
    }

    /** Someone goes their own way, keeping what they had seen; a fellowship left with one in it is none. */
    public boolean part(UUID id) {
        Player p = players.get(id);
        if (p == null || p.band == null) {
            return false;
        }
        UUID band = p.band;
        p.band = null;
        p.beats.shared = new HashSet<>(p.beats.shared);
        List<Player> rest = new ArrayList<>();
        for (Player o : players.values()) {
            if (band.equals(o.band)) {
                rest.add(o);
            }
        }
        if (rest.size() == 1) {
            rest.get(0).band = null;
            rest.get(0).beats.shared = new HashSet<>(rest.get(0).beats.shared);
            bandStance.remove(band);
        }
        markDirty();
        return true;
    }

    /** What Warlock last made of each fellowship, to tell when one of them has changed his mind. */
    private final Map<UUID, Stance> bandStance = new HashMap<>();

    /**
     * Warlock takes a fellowship as one: whatever he has lately come to make of any of them (they
     * answered him, or broke their word), he makes of them all. Called now and then; true if anyone changed.
     */
    public boolean settleStances() {
        Map<UUID, List<Player>> bands = new HashMap<>();
        for (Player p : players.values()) {
            if (p.band != null) {
                bands.computeIfAbsent(p.band, k -> new ArrayList<>()).add(p);
            }
        }
        bandStance.keySet().retainAll(bands.keySet());
        boolean changed = false;
        for (Map.Entry<UUID, List<Player>> e : bands.entrySet()) {
            Stance was = bandStance.get(e.getKey());
            Player lead = null;
            for (Player p : e.getValue()) {
                // Newly bound together: whoever is furthest along with him. After that: whoever he changed his mind about.
                if (was == null ? p.warlock != Stance.UNMET && (lead == null || p.warlock.ordinal() > lead.warlock.ordinal())
                        : p.warlock != was) {
                    lead = p;
                }
            }
            if (lead == null) {
                bandStance.putIfAbsent(e.getKey(), e.getValue().get(0).warlock);
                continue;
            }
            bandStance.put(e.getKey(), lead.warlock);
            for (Player p : e.getValue()) {
                if (p != lead && (p.warlock != lead.warlock || p.trust != lead.trust)) {
                    if (lead.warlock == Stance.TRUCE && p.warlock != Stance.TRUCE) {
                        // What they carry when he hears of it is what they promised to stop at.
                        p.truceMask = -1;
                        p.betrayalAt = -1;
                    }
                    p.warlock = lead.warlock;
                    p.trust = lead.trust;
                    changed = true;
                }
            }
        }
        if (changed) {
            markDirty();
        }
        return changed;
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
        bandStance.clear();
        markDirty();
    }

    // ------------------------------------------------------------------ saving

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        loading = true;
        try {
            read(nbt);
        } finally {
            loading = false;
        }
    }

    private void read(NBTTagCompound nbt) {
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
            p.devotion = tag.getInteger("devotion");
            p.offerings = tag.getInteger("offerings");
            p.sacrifices = tag.getInteger("sacrifices");
            p.spurned = tag.getInteger("spurned");
            p.demandLeft = tag.getInteger("demandLeft");
            p.demandKind = tag.getInteger("demandKind");
            p.demandBy = tag.hasKey("demandBy") ? tag.getLong("demandBy") : -1;
            p.demandRace = tag.getBoolean("demandRace");
            p.wronged = tag.getBoolean("wronged");
            p.nextVisit = tag.hasKey("nextVisit") ? tag.getLong("nextVisit") : -1;
            p.deathLook = tag.hasKey("deathLook") ? tag.getInteger("deathLook") : -1;
            p.deathRot = tag.getInteger("deathRot");
            p.band = tag.hasUniqueId("band") ? tag.getUniqueId("band") : null;
        }
        // Each fellowship's record was written out once for each of them: make it one again.
        Map<UUID, Set<String>> shared = new HashMap<>();
        for (Player p : players.values()) {
            if (p.band != null) {
                Set<String> set = shared.computeIfAbsent(p.band, k -> new HashSet<>());
                set.addAll(p.beats.shared);
                p.beats.shared = set;
            }
        }
        bandStance.clear();
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
            tag.setInteger("devotion", p.devotion);
            tag.setInteger("offerings", p.offerings);
            tag.setInteger("sacrifices", p.sacrifices);
            tag.setInteger("spurned", p.spurned);
            tag.setInteger("demandLeft", p.demandLeft);
            tag.setInteger("demandKind", p.demandKind);
            tag.setLong("demandBy", p.demandBy);
            tag.setBoolean("demandRace", p.demandRace);
            tag.setBoolean("wronged", p.wronged);
            tag.setLong("nextVisit", p.nextVisit);
            tag.setInteger("deathLook", p.deathLook);
            tag.setInteger("deathRot", p.deathRot);
            if (p.band != null) {
                tag.setUniqueId("band", p.band);
            }
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
