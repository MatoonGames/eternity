package com.matoon.eternity.world;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The Grandmaster's game, kept with the world: where his board lies and what it covered, the
 * position on it, what each side has staked, how broken it is, and how it all ended. His
 * pieces, his body and Warlock are never saved; the director puts them back from this.
 */
public class GrandmasterData extends WorldSavedData {

    public static final String NAME = "eternity_grandmaster";

    /** Where the encounter has got to. Appended to only, since it is saved by ordinal. */
    public enum Phase { NONE, ARRIVING, OPENING, STAKES, RULES, PLAYING, LOST, COLLAPSE, DONE }

    /** Adam Warlock's part in it. */
    public enum Warlock { ABSENT, PLAYING, WALKED, GONE }

    public Phase phase = Phase.NONE;
    public int dimension;
    /** The board's north-west corner, at the level of its surface. */
    public BlockPos origin = BlockPos.ORIGIN;
    public UUID player;
    public String playerName = "";
    public int[] game = new int[0];
    /** Times he has moved the board himself, this game and all games. */
    public int cheats;
    public Warlock warlock = Warlock.ABSENT;
    /** Whatever the player put on the table, and whether he has won it. */
    public final List<ItemStack> pot = new ArrayList<>();
    public boolean potLost;
    public boolean staked;
    /** How many more blows the board can take, and how cracked each square is (3 = gone). */
    public int integrity = -1;
    public int[] cracks = new int[Kingstable.CELLS];
    /** What was there before the board: block state ids over the cleared box, and its tile entities. */
    public int[] covered = new int[0];
    public NBTTagList coveredTiles = new NBTTagList();
    public int games;
    public int playerMoves;
    /** He is dead, and his stone has been taken from the board. Never again. */
    public boolean finished;
    /** Warlock walked off his board: he refused it, whatever he was before. For the finale. */
    public boolean warlockRefused;

    public GrandmasterData() {
        super(NAME);
    }

    public GrandmasterData(String name) {
        super(name);
    }

    public static GrandmasterData get() {
        World overworld = DimensionManager.getWorld(0);
        MapStorage storage = overworld.getMapStorage();
        GrandmasterData data = (GrandmasterData) storage.getOrLoadData(GrandmasterData.class, NAME);
        if (data == null) {
            data = new GrandmasterData();
            storage.setData(NAME, data);
        }
        return data;
    }

    public boolean active() {
        return phase != Phase.NONE && phase != Phase.DONE;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        int p = nbt.getInteger("phase");
        phase = p >= 0 && p < Phase.values().length ? Phase.values()[p] : Phase.NONE;
        dimension = nbt.getInteger("dim");
        origin = new BlockPos(nbt.getInteger("x"), nbt.getInteger("y"), nbt.getInteger("z"));
        player = nbt.hasUniqueId("player") ? nbt.getUniqueId("player") : null;
        playerName = nbt.getString("playerName");
        game = nbt.getIntArray("game");
        cheats = nbt.getInteger("cheats");
        int w = nbt.getInteger("warlock");
        warlock = w >= 0 && w < Warlock.values().length ? Warlock.values()[w] : Warlock.ABSENT;
        pot.clear();
        NBTTagList items = nbt.getTagList("pot", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < items.tagCount(); i++) {
            ItemStack stack = new ItemStack(items.getCompoundTagAt(i));
            if (!stack.isEmpty()) {
                pot.add(stack);
            }
        }
        potLost = nbt.getBoolean("potLost");
        staked = nbt.getBoolean("staked");
        integrity = nbt.hasKey("integrity") ? nbt.getInteger("integrity") : -1;
        int[] c = nbt.getIntArray("cracks");
        cracks = c.length == Kingstable.CELLS ? c : new int[Kingstable.CELLS];
        covered = nbt.getIntArray("covered");
        coveredTiles = nbt.getTagList("coveredTiles", Constants.NBT.TAG_COMPOUND);
        games = nbt.getInteger("games");
        playerMoves = nbt.getInteger("playerMoves");
        finished = nbt.getBoolean("finished");
        warlockRefused = nbt.getBoolean("warlockRefused");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        nbt.setInteger("phase", phase.ordinal());
        nbt.setInteger("dim", dimension);
        nbt.setInteger("x", origin.getX());
        nbt.setInteger("y", origin.getY());
        nbt.setInteger("z", origin.getZ());
        if (player != null) {
            nbt.setUniqueId("player", player);
        }
        nbt.setString("playerName", playerName);
        nbt.setIntArray("game", game);
        nbt.setInteger("cheats", cheats);
        nbt.setInteger("warlock", warlock.ordinal());
        NBTTagList items = new NBTTagList();
        for (ItemStack stack : pot) {
            items.appendTag(stack.writeToNBT(new NBTTagCompound()));
        }
        nbt.setTag("pot", items);
        nbt.setBoolean("potLost", potLost);
        nbt.setBoolean("staked", staked);
        nbt.setInteger("integrity", integrity);
        nbt.setIntArray("cracks", cracks);
        nbt.setIntArray("covered", covered);
        nbt.setTag("coveredTiles", coveredTiles);
        nbt.setInteger("games", games);
        nbt.setInteger("playerMoves", playerMoves);
        nbt.setBoolean("finished", finished);
        nbt.setBoolean("warlockRefused", warlockRefused);
        return nbt;
    }
}
