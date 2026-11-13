package com.matoon.eternity.world;

import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.Constants;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Everything the end of the world took: every block it turned or tore away, as it was before,
 * so that an operator can put the world back. The first change to a block is the one remembered;
 * whatever the unmaking made of it after that does not matter.
 */
public class Scars extends WorldSavedData {

    public static final String NAME = "eternity_scars";

    /** Blocks put back per tick while the world mends. */
    private static final int HEAL_PER_TICK = 4000;

    private final Map<Integer, Long2IntOpenHashMap> blocks = new HashMap<>();
    private final Map<Integer, Long2ObjectOpenHashMap<NBTTagCompound>> tiles = new HashMap<>();
    /** Whether an operator has asked for it all back, and it is being put back. */
    public boolean mending;

    public Scars() {
        super(NAME);
    }

    public Scars(String name) {
        super(name);
    }

    public static Scars get() {
        World overworld = DimensionManager.getWorld(0);
        MapStorage storage = overworld.getMapStorage();
        Scars data = (Scars) storage.getOrLoadData(Scars.class, NAME);
        if (data == null) {
            data = new Scars();
            storage.setData(NAME, data);
        }
        return data;
    }

    /** Remembers a block as it is, just before the unmaking changes it. */
    void remember(World world, BlockPos pos) {
        int dim = world.provider.getDimension();
        long key = pos.toLong();
        Long2IntOpenHashMap map = blocks.computeIfAbsent(dim, k -> new Long2IntOpenHashMap());
        if (map.containsKey(key)) {
            return;
        }
        IBlockState state = world.getBlockState(pos);
        map.put(key, Block.getStateId(state));
        TileEntity te = world.getTileEntity(pos);
        if (te != null) {
            tiles.computeIfAbsent(dim, k -> new Long2ObjectOpenHashMap<>()).put(key, te.writeToNBT(new NBTTagCompound()));
        }
        markDirty();
    }

    public boolean isEmpty() {
        for (Long2IntOpenHashMap map : blocks.values()) {
            if (!map.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public int size() {
        int n = 0;
        for (Long2IntOpenHashMap map : blocks.values()) {
            n += map.size();
        }
        return n;
    }

    /**
     * Puts back up to a tick's worth of what was taken. Only over air or what the unmaking left
     * there: anything someone has built since stays. Returns true once nothing is left to mend.
     */
    boolean heal(MinecraftServer server, Set<IBlockState> unmade) {
        int budget = HEAL_PER_TICK;
        Iterator<Map.Entry<Integer, Long2IntOpenHashMap>> dims = blocks.entrySet().iterator();
        while (dims.hasNext() && budget > 0) {
            Map.Entry<Integer, Long2IntOpenHashMap> entry = dims.next();
            int dim = entry.getKey();
            WorldServer world = DimensionManager.isDimensionRegistered(dim) ? server.getWorld(dim) : null;
            if (world == null) {
                dims.remove();
                tiles.remove(dim);
                continue;
            }
            Long2ObjectOpenHashMap<NBTTagCompound> dimTiles = tiles.get(dim);
            ObjectIterator<Long2IntMap.Entry> it = entry.getValue().long2IntEntrySet().fastIterator();
            while (it.hasNext() && budget > 0) {
                Long2IntMap.Entry e = it.next();
                long key = e.getLongKey();
                int was = e.getIntValue();
                it.remove();
                budget--;
                BlockPos at = BlockPos.fromLong(key);
                NBTTagCompound tile = dimTiles == null ? null : dimTiles.remove(key);
                IBlockState now = world.getBlockState(at);
                if (now.getMaterial() != Material.AIR && !unmade.contains(now)) {
                    continue;
                }
                world.setBlockState(at, Block.getStateById(was), 2);
                if (tile != null) {
                    TileEntity te = world.getTileEntity(at);
                    if (te != null) {
                        te.readFromNBT(tile);
                        te.markDirty();
                    }
                }
            }
            if (entry.getValue().isEmpty()) {
                dims.remove();
                tiles.remove(dim);
            }
        }
        markDirty();
        return blocks.isEmpty();
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        blocks.clear();
        tiles.clear();
        mending = nbt.getBoolean("mending");
        NBTTagList list = nbt.getTagList("dims", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            int dim = tag.getInteger("dim");
            int[] hi = tag.getIntArray("hi"), lo = tag.getIntArray("lo"), state = tag.getIntArray("state");
            int n = Math.min(hi.length, Math.min(lo.length, state.length));
            Long2IntOpenHashMap map = new Long2IntOpenHashMap(n);
            for (int j = 0; j < n; j++) {
                map.put(((long) hi[j] << 32) | (lo[j] & 0xFFFFFFFFL), state[j]);
            }
            blocks.put(dim, map);
            NBTTagList tileList = tag.getTagList("tiles", Constants.NBT.TAG_COMPOUND);
            if (tileList.tagCount() > 0) {
                Long2ObjectOpenHashMap<NBTTagCompound> dimTiles = new Long2ObjectOpenHashMap<>();
                for (int j = 0; j < tileList.tagCount(); j++) {
                    NBTTagCompound t = tileList.getCompoundTagAt(j);
                    dimTiles.put(t.getLong("pos"), t.getCompoundTag("tile"));
                }
                tiles.put(dim, dimTiles);
            }
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        NBTTagList list = new NBTTagList();
        for (Map.Entry<Integer, Long2IntOpenHashMap> entry : blocks.entrySet()) {
            Long2IntOpenHashMap map = entry.getValue();
            int[] hi = new int[map.size()], lo = new int[map.size()], state = new int[map.size()];
            int j = 0;
            for (Long2IntMap.Entry e : map.long2IntEntrySet()) {
                hi[j] = (int) (e.getLongKey() >>> 32);
                lo[j] = (int) e.getLongKey();
                state[j] = e.getIntValue();
                j++;
            }
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("dim", entry.getKey());
            tag.setIntArray("hi", hi);
            tag.setIntArray("lo", lo);
            tag.setIntArray("state", state);
            Long2ObjectOpenHashMap<NBTTagCompound> dimTiles = tiles.get(entry.getKey());
            if (dimTiles != null) {
                NBTTagList tileList = new NBTTagList();
                for (Long2ObjectMap.Entry<NBTTagCompound> t : dimTiles.long2ObjectEntrySet()) {
                    NBTTagCompound tt = new NBTTagCompound();
                    tt.setLong("pos", t.getLongKey());
                    tt.setTag("tile", t.getValue());
                    tileList.appendTag(tt);
                }
                tag.setTag("tiles", tileList);
            }
            list.appendTag(tag);
        }
        nbt.setTag("dims", list);
        nbt.setBoolean("mending", mending);
        return nbt;
    }
}
