package com.matoon.eternity.world;

import com.matoon.eternity.EternityConfig;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.server.SPacketChunkData;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Tamarata coming apart. From where his fists came down a wave runs outward, and every chunk it
 * reaches is torn into the drifting fragments the planet becomes, a few of its blocks flung up
 * as it goes; nearest first, a budgeted number of chunks each tick. The island the landing pad
 * stands on holds.
 * <p>
 * Chunks out of reach when the wave passed are broken as they are next loaded, and chunks
 * generated after it are born broken. Each broken chunk is marked in its saved data, so it is
 * never broken twice. None of this ever touches any other dimension.
 */
public final class TamarataBreaker {

    public static final TamarataBreaker INSTANCE = new TamarataBreaker();

    /** How long the wave takes to reach the edge of the destruction, in ticks. */
    public static final int WAVE_TICKS = 320;
    private static final String MARK = "EternityShattered";
    private static final int MAX_DEBRIS = 64;

    private final Set<Long> shattered = Collections.synchronizedSet(new HashSet<>());
    private final Deque<Long> pending = new ArrayDeque<>();
    private final Random rand = new Random();
    /** The wave's chunks, nearest first, with when it reaches each. */
    private final List<long[]> wave = new ArrayList<>();
    private int waveIndex;
    /** Where the wave started, and whether everything else already loaded has been sent after it. */
    private double centreX, centreZ;
    private boolean swept;
    private final List<EntityFallingBlock> debris = new ArrayList<>();

    private TamarataBreaker() {
    }

    public void reset() {
        shattered.clear();
        pending.clear();
        wave.clear();
        waveIndex = 0;
        debris.clear();
    }

    /** Sets the wave going from a point, as it was elapsed ticks ago (for picking up again after a restart). */
    public void begin(double x, double z, int elapsed) {
        wave.clear();
        waveIndex = 0;
        centreX = x;
        centreZ = z;
        swept = false;
        int radius = EternityConfig.champion.destructionRadius;
        int r = radius / 16 + 1;
        int cx = (int) Math.floor(x) >> 4, cz = (int) Math.floor(z) >> 4;
        for (int i = -r; i <= r; i++) {
            for (int j = -r; j <= r; j++) {
                double dx = (cx + i) * 16 + 8 - x, dz = (cz + j) * 16 + 8 - z;
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d <= radius + 8D) {
                    wave.add(new long[]{ChunkPos.asLong(cx + i, cz + j), (long) (d / radius * WAVE_TICKS)});
                }
            }
        }
        wave.sort((a, b) -> Long.compare(a[1], b[1]));
        while (waveIndex < wave.size() && wave.get(waveIndex)[1] < elapsed - 40) {
            // Already passed before a restart: those break as they load, like anything else.
            waveIndex++;
        }
    }

    /** Whether the wave has run its course. */
    public boolean done() {
        return waveIndex >= wave.size();
    }

    /** Whether there is anything left to break: the wave, what is waiting since it loaded, or the sweep after the wave. */
    public boolean busy() {
        return !done() || !pending.isEmpty() || !swept && !wave.isEmpty();
    }

    /** One tick of the breaking: the wave at elapsed ticks, then anything waiting to be broken since it loaded. */
    public void tick(WorldServer world, int elapsed) {
        int budget = EternityConfig.champion.chunksPerTick;
        while (waveIndex < wave.size() && wave.get(waveIndex)[1] <= elapsed && budget > 0) {
            long key = wave.get(waveIndex++)[0];
            int cx = (int) key, cz = (int) (key >> 32);
            if (shattered.contains(key) || !world.getChunkProvider().chunkExists(cx, cz)) {
                continue;
            }
            Chunk chunk = world.getChunk(cx, cz);
            fling(world, chunk);
            shatter(world, chunk);
            budget--;
        }
        if (!swept && waveIndex >= wave.size() && !wave.isEmpty()) {
            sweep(world);
        }
        // A chunk someone can see costs them a rebuild of all of it, so only a few of those go each tick.
        int seen = Math.max(1, budget / 4);
        List<Long> later = new ArrayList<>();
        while (!pending.isEmpty() && budget > 0 && later.size() < 64) {
            long key = pending.poll();
            int cx = (int) key, cz = (int) (key >> 32);
            if (shattered.contains(key) || !world.getChunkProvider().chunkExists(cx, cz)) {
                continue;
            }
            if (seen(world, cx, cz)) {
                if (seen == 0) {
                    later.add(key);
                    continue;
                }
                seen--;
            }
            shatter(world, world.getChunk(cx, cz));
            budget--;
        }
        for (int i = later.size() - 1; i >= 0; i--) {
            pending.addFirst(later.get(i));
        }
        debris.removeIf(e -> e.isDead);
    }

    /**
     * Past the edge of the wave, whatever is loaded goes too, nearest first, so nobody is left
     * looking at a cliff of whole planet that only breaks once they walk away and come back.
     */
    private void sweep(WorldServer world) {
        swept = true;
        List<long[]> rest = new ArrayList<>();
        for (Chunk chunk : world.getChunkProvider().getLoadedChunks()) {
            long key = ChunkPos.asLong(chunk.x, chunk.z);
            if (!shattered.contains(key)) {
                double dx = chunk.x * 16 + 8 - centreX, dz = chunk.z * 16 + 8 - centreZ;
                rest.add(new long[]{key, (long) (dx * dx + dz * dz)});
            }
        }
        rest.sort((a, b) -> Long.compare(a[1], b[1]));
        for (long[] entry : rest) {
            pending.add(entry[0]);
        }
    }

    /**
     * Tears one chunk into fragments, straight into its storage, then sends it again to everyone
     * who can see it. Cheap: the whole chunk is emptied at once, and only the blocks of the few
     * fragments that lie in it are worked out and set. Only the island keeps what was there.
     */
    public void shatter(WorldServer world, Chunk chunk) {
        long seed = world.getSeed();
        int baseX = chunk.x * 16, baseZ = chunk.z * 16;
        boolean island = Math.hypot(baseX + 8 - Tamarata.LANDING_X, baseZ + 8 - Tamarata.LANDING_Z) < Tamarata.ISLAND_RADIUS + 12D;
        // Whatever stood in the broken part goes with it.
        for (BlockPos pos : new ArrayList<>(chunk.getTileEntityMap().keySet())) {
            if (!island || !Tamarata.onIsland(seed, pos.getX(), pos.getY(), pos.getZ())) {
                world.removeTileEntity(pos);
            }
        }
        ExtendedBlockStorage[] old = chunk.getBlockStorageArray().clone();
        ExtendedBlockStorage[] storage = chunk.getBlockStorageArray();
        java.util.Arrays.fill(storage, null);
        boolean sky = world.provider.hasSkyLight();
        if (island) {
            // The island holds: only its columns are looked at block by block.
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    int wx = baseX + x, wz = baseZ + z;
                    if (Math.hypot(wx - Tamarata.LANDING_X, wz - Tamarata.LANDING_Z) > Tamarata.ISLAND_RADIUS) {
                        continue;
                    }
                    for (int s = 0; s < old.length; s++) {
                        if (old[s] == null) {
                            continue;
                        }
                        for (int y = 0; y < 16; y++) {
                            IBlockState was = old[s].get(x, y, z);
                            if (was.getMaterial() != Material.AIR && Tamarata.onIsland(seed, wx, (s << 4) + y, wz)) {
                                section(storage, s, sky).set(x, y, z, was);
                                section(storage, s, sky).setBlockLight(x, y, z, old[s].getBlockLight(x, y, z));
                            }
                        }
                    }
                }
            }
        }
        Tamarata.fragmentsInChunk(seed, chunk.x, chunk.z, (wx, wy, wz, state) -> {
            if (!island || !Tamarata.onIsland(seed, wx, wy, wz)) {
                section(storage, wy >> 4, sky).set(wx - baseX, wy & 15, wz - baseZ, state);
            }
        });
        chunk.generateSkylightMap();
        chunk.markDirty();
        shattered.add(ChunkPos.asLong(chunk.x, chunk.z));
        PlayerChunkMapEntry entry = world.getPlayerChunkMap().getEntry(chunk.x, chunk.z);
        if (entry != null && entry.isSentToPlayers()) {
            entry.sendPacket(new SPacketChunkData(chunk, 65535));
        }
    }

    private static boolean seen(WorldServer world, int cx, int cz) {
        PlayerChunkMapEntry entry = world.getPlayerChunkMap().getEntry(cx, cz);
        return entry != null && entry.isSentToPlayers();
    }

    private static ExtendedBlockStorage section(ExtendedBlockStorage[] storage, int s, boolean sky) {
        if (storage[s] == null) {
            storage[s] = new ExtendedBlockStorage(s << 4, sky);
        }
        return storage[s];
    }

    /** As the wave hits a chunk, now and then a piece of its surface is flung up and left drifting. */
    private void fling(WorldServer world, Chunk chunk) {
        if (debris.size() >= MAX_DEBRIS || rand.nextBoolean()) {
            return;
        }
        int x = rand.nextInt(16), z = rand.nextInt(16);
        int top = chunk.getHeightValue(x, z) - 1;
        if (top < 2) {
            return;
        }
        IBlockState state = chunk.getBlockState(x, top, z);
        if (state.getMaterial() == Material.AIR || state.getBlock().hasTileEntity(state)) {
            return;
        }
        EntityFallingBlock block = new EntityFallingBlock(world, chunk.x * 16 + x + 0.5D, top + 1.2D, chunk.z * 16 + z + 0.5D, state);
        block.setNoGravity(true);
        block.shouldDropItem = false;
        block.fallTime = 1 + rand.nextInt(200);
        block.setHurtEntities(false);
        block.motionX = rand.nextGaussian() * 0.12D;
        block.motionY = 0.2D + rand.nextDouble() * 0.5D;
        block.motionZ = rand.nextGaussian() * 0.12D;
        if (world.spawnEntity(block)) {
            debris.add(block);
        }
    }

    // ------------------------------------------------------------------ chunks that come later

    public void markShattered(int chunkX, int chunkZ) {
        shattered.add(ChunkPos.asLong(chunkX, chunkZ));
    }

    public int pending() {
        return pending.size();
    }

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        if (event.getWorld().isRemote || event.getWorld().provider.getDimension() != Tamarata.dimension()
                || !ChampionDirector.INSTANCE.planetBroken()) {
            return;
        }
        long key = ChunkPos.asLong(event.getChunk().x, event.getChunk().z);
        if (!shattered.contains(key)) {
            pending.add(key);
        }
    }

    @SubscribeEvent
    public void onChunkDataLoad(ChunkDataEvent.Load event) {
        if (event.getWorld().provider.getDimension() == Tamarata.dimension() && event.getData().getBoolean(MARK)) {
            shattered.add(ChunkPos.asLong(event.getChunk().x, event.getChunk().z));
        }
    }

    @SubscribeEvent
    public void onChunkDataSave(ChunkDataEvent.Save event) {
        if (event.getWorld().provider.getDimension() == Tamarata.dimension()
                && shattered.contains(ChunkPos.asLong(event.getChunk().x, event.getChunk().z))) {
            event.getData().setBoolean(MARK, true);
        }
    }
}
