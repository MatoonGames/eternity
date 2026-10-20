package com.matoon.eternity.world;

import anvil.infinity.config.ModConfig;
import anvil.infinity.worldgen.WorldData;
import com.matoon.eternity.compat.StoneScanner;
import com.matoon.eternity.item.ItemStoneFinder;
import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.network.MsgStoneTarget;
import com.matoon.eternity.world.StoneLedger.Sighting;
import lucraft.mods.lucraftcore.infinity.EntityItemIndestructible;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Knows where every Infinity Stone is, for the Stone Finder. Every loaded world is
 * searched: players (and their ender chests), dropped items including Lucraft's
 * indestructible item entities, item frames, mobs' hands, minecarts and every
 * container block, looking inside gauntlets and shulker boxes too. Stones out of
 * reach fall back to where they were last seen, or to the temple that holds them.
 */
public final class StoneTracker {

    public static final StoneTracker INSTANCE = new StoneTracker();

    private static final int SCAN_INTERVAL = 60;
    private static final int POINT_INTERVAL = 10;

    private final List<Candidate> candidates = new ArrayList<>();
    private final Map<UUID, Candidate> lastSent = new HashMap<>();
    private int ticks;

    private static final class Candidate {
        final String item;
        final int dimension;
        final double x, y, z;
        /** The player carrying it, so their own finder ignores it. */
        @Nullable
        final UUID holder;

        Candidate(String item, int dimension, double x, double y, double z, @Nullable UUID holder) {
            this.item = item;
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
            this.holder = holder;
        }
    }

    private StoneTracker() {
    }

    public void reset() {
        candidates.clear();
        lastSent.clear();
        ticks = 0;
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null || DimensionManager.getWorld(0) == null || server.getPlayerList().getCurrentPlayerCount() == 0) {
            return;
        }
        ticks++;
        if (ticks % SCAN_INTERVAL == 1) {
            rescan();
        }
        if (ticks % POINT_INTERVAL == 0) {
            point(server);
        }
    }

    // ------------------------------------------------------------------ scanning

    private void rescan() {
        List<Candidate> found = new ArrayList<>();
        for (WorldServer world : DimensionManager.getWorlds()) {
            int dim = world.provider.getDimension();
            for (Entity entity : new ArrayList<>(world.loadedEntityList)) {
                try {
                    scanEntity(entity, dim, found);
                } catch (RuntimeException ignored) {
                    // A misbehaving modded entity must not break the search.
                }
            }
            for (TileEntity tile : new ArrayList<>(world.loadedTileEntityList)) {
                try {
                    scanTile(tile, dim, found);
                } catch (RuntimeException ignored) {
                }
            }
        }

        StoneLedger ledger = StoneLedger.get();
        Set<String> seen = new HashSet<>();
        for (Candidate c : found) {
            seen.add(c.item);
            ledger.lastSeen.put(c.item, new Sighting(c.dimension, c.x, c.y, c.z));
        }
        if (!found.isEmpty()) {
            ledger.markDirty();
        }

        candidates.clear();
        candidates.addAll(found);
        for (Map.Entry<String, Sighting> entry : ledger.lastSeen.entrySet()) {
            Sighting s = entry.getValue();
            // If that spot is loaded and the stone was not found there, it has moved on.
            if (!seen.contains(entry.getKey()) && !isLoaded(s.dimension, s.x, s.y, s.z)) {
                candidates.add(new Candidate(entry.getKey(), s.dimension, s.x, s.y, s.z, null));
            }
        }
        addTemples(ledger, seen);
    }

    private void scanEntity(Entity entity, int dim, List<Candidate> found) {
        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;
            UUID holder = player.getUniqueID();
            for (ItemStack stack : player.inventory.mainInventory) {
                add(stack, dim, entity, holder, found);
            }
            for (ItemStack stack : player.inventory.armorInventory) {
                add(stack, dim, entity, holder, found);
            }
            for (ItemStack stack : player.inventory.offHandInventory) {
                add(stack, dim, entity, holder, found);
            }
            add(player.inventory.getItemStack(), dim, entity, holder, found);
            scanInventory(player.getInventoryEnderChest(), dim, entity, holder, found);
            return;
        }
        if (entity instanceof EntityItem) {
            add(((EntityItem) entity).getItem(), dim, entity, null, found);
            return;
        }
        if (entity instanceof EntityItemIndestructible) {
            add(((EntityItemIndestructible) entity).getItem(), dim, entity, null, found);
            return;
        }
        if (entity instanceof EntityItemFrame) {
            add(((EntityItemFrame) entity).getDisplayedItem(), dim, entity, null, found);
            return;
        }
        if (entity instanceof EntityLivingBase) {
            for (ItemStack stack : entity.getEquipmentAndArmor()) {
                add(stack, dim, entity, null, found);
            }
        }
        if (entity.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)) {
            scanHandler(entity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null), dim, entity, found);
        } else if (entity instanceof IInventory) {
            scanInventory((IInventory) entity, dim, entity, null, found);
        }
    }

    private void scanTile(TileEntity tile, int dim, List<Candidate> found) {
        BlockPos pos = tile.getPos();
        double x = pos.getX() + 0.5D, y = pos.getY() + 0.5D, z = pos.getZ() + 0.5D;
        List<ItemStack> stacks = new ArrayList<>();
        if (tile.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)) {
            IItemHandler handler = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
            if (handler != null) {
                for (int i = 0; i < handler.getSlots(); i++) {
                    stacks.add(handler.getStackInSlot(i));
                }
            }
        } else if (tile instanceof IInventory) {
            IInventory inventory = (IInventory) tile;
            for (int i = 0; i < inventory.getSizeInventory(); i++) {
                stacks.add(inventory.getStackInSlot(i));
            }
        }
        for (ItemStack stack : stacks) {
            StoneScanner.forEachStone(stack, stone -> found.add(new Candidate(id(stone), dim, x, y, z, null)));
        }
    }

    private void scanHandler(@Nullable IItemHandler handler, int dim, Entity at, List<Candidate> found) {
        if (handler != null) {
            for (int i = 0; i < handler.getSlots(); i++) {
                add(handler.getStackInSlot(i), dim, at, null, found);
            }
        }
    }

    private void scanInventory(IInventory inventory, int dim, Entity at, @Nullable UUID holder, List<Candidate> found) {
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            add(inventory.getStackInSlot(i), dim, at, holder, found);
        }
    }

    private void add(ItemStack stack, int dim, Entity at, @Nullable UUID holder, List<Candidate> found) {
        StoneScanner.forEachStone(stack, stone -> found.add(new Candidate(id(stone), dim, at.posX, at.posY, at.posZ, holder)));
    }

    /** InfinityCraft stones still waiting in the temples they were generated into. */
    private void addTemples(StoneLedger ledger, Set<String> seen) {
        WorldData data = WorldData.get(DimensionManager.getWorld(0));
        temple(ledger, seen, anvil.infinity.items.Items.POWER_STONE, data.power, ModConfig.Worldgen.powerDim, data.powerx, data.powerz);
        temple(ledger, seen, anvil.infinity.items.Items.SPACE_STONE, data.space, ModConfig.Worldgen.spaceDim, data.spacex, data.spacez);
        temple(ledger, seen, anvil.infinity.items.Items.REALITY_STONE, data.reality, ModConfig.Worldgen.realityDim, data.realityx, data.realityz);
        temple(ledger, seen, anvil.infinity.items.Items.SOUL_STONE, data.soul, ModConfig.Worldgen.soulDim, data.soulx, data.soulz);
        temple(ledger, seen, anvil.infinity.items.Items.TIME_STONE, data.time, ModConfig.Worldgen.timeDim, data.timex, data.timez);
        temple(ledger, seen, anvil.infinity.items.Items.MIND_STONE, data.mind, ModConfig.Worldgen.mindDim, data.mindx, data.mindz);
    }

    private void temple(StoneLedger ledger, Set<String> seen, @Nullable Item stone, boolean generated, int dim, int x, int z) {
        if (stone == null || !generated || stone.getRegistryName() == null) {
            return;
        }
        String id = stone.getRegistryName().toString();
        if (!seen.contains(id) && !ledger.lastSeen.containsKey(id)) {
            candidates.add(new Candidate(id, dim, x + 0.5D, 64D, z + 0.5D, null));
        }
    }

    private static boolean isLoaded(int dim, double x, double y, double z) {
        WorldServer world = DimensionManager.getWorld(dim);
        return world != null && world.isBlockLoaded(new BlockPos(x, y, z));
    }

    private static String id(ItemStack stack) {
        return stack.getItem().getRegistryName() == null ? "unknown" : stack.getItem().getRegistryName().toString();
    }

    // ------------------------------------------------------------------ pointing

    private void point(MinecraftServer server) {
        for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
            if (!holdsFinder(player)) {
                lastSent.remove(player.getUniqueID());
                continue;
            }
            Candidate nearest = null;
            double best = Double.MAX_VALUE;
            for (Candidate c : candidates) {
                if (c.dimension != player.dimension || player.getUniqueID().equals(c.holder)) {
                    continue;
                }
                double dx = c.x - player.posX, dz = c.z - player.posZ;
                double d = dx * dx + dz * dz;
                if (d < best) {
                    best = d;
                    nearest = c;
                }
            }
            Candidate previous = lastSent.get(player.getUniqueID());
            boolean changed = nearest == null ? previous != null || !lastSent.containsKey(player.getUniqueID())
                    : previous == null || Math.abs(previous.x - nearest.x) > 0.5D || Math.abs(previous.z - nearest.z) > 0.5D;
            if (changed) {
                lastSent.put(player.getUniqueID(), nearest);
                EternityNetwork.send(nearest == null ? new MsgStoneTarget() : new MsgStoneTarget(nearest.x, nearest.z), player);
            }
        }
    }

    private static boolean holdsFinder(EntityPlayer player) {
        for (int i = 0; i < 9; i++) {
            if (player.inventory.mainInventory.get(i).getItem() instanceof ItemStoneFinder) {
                return true;
            }
        }
        return player.getHeldItemOffhand().getItem() instanceof ItemStoneFinder;
    }
}
