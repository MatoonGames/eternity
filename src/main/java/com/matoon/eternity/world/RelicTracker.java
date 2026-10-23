package com.matoon.eternity.world;

import anvil.infinity.config.ModConfig;
import anvil.infinity.worldgen.WorldData;
import com.matoon.eternity.Eternity;
import com.matoon.eternity.EternityConfig;
import com.matoon.eternity.compat.RelicScanner;
import com.matoon.eternity.compat.Relics;
import com.matoon.eternity.entity.EntityAstralRegulator;
import com.matoon.eternity.item.EternityItems;
import com.matoon.eternity.item.ItemRelicTracker;
import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.network.MsgRelicTarget;
import com.matoon.eternity.world.RelicLedger.Sighting;
import lucraft.mods.heroesexpansion.entities.EntityTesseract;
import lucraft.mods.heroesexpansion.items.HEItems;
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
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
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
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Knows where every relic is, for the Relic Tracker: the Infinity Stones (the Tesseract
 * among them) and the Astral Regulators. Every loaded world is searched: players (and
 * their ender chests), dropped items including Lucraft's indestructible item entities,
 * item frames, mobs' hands, minecarts and every container block, looking inside gauntlets
 * and shulker boxes too. Stones out of reach fall back to where they were last seen, or to
 * the temple that holds them.
 * <p>
 * It also keeps each relic single: the first copy someone takes is claimed as the true
 * one (see {@link Relics}), Norse villages stop setting out Tesseracts once one has been
 * claimed, and relics are held at the edge of the void instead of falling out of the world.
 */
public final class RelicTracker {

    public static final RelicTracker INSTANCE = new RelicTracker();

    private static final int SCAN_INTERVAL = 60;
    private static final int POINT_INTERVAL = 10;
    private static final int CLAIM_INTERVAL = 10;

    private final List<Candidate> candidates = new ArrayList<>();
    private final Map<UUID, Candidate> lastSent = new HashMap<>();
    private int ticks;
    /** Whether a Soul Stone was found anywhere in the world, or is known to lie somewhere, at the last search. */
    private boolean soulSeen;

    private static final class Candidate {
        final String item;
        final int dimension;
        final double x, y, z;
        /** The player carrying it, so their own tracker ignores it. */
        @Nullable
        final UUID holder;
        final String holderName;

        Candidate(String item, int dimension, double x, double y, double z, @Nullable EntityPlayer holder) {
            this.item = item;
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
            this.holder = holder == null ? null : holder.getUniqueID();
            this.holderName = holder == null ? "" : holder.getName();
        }
    }

    private RelicTracker() {
    }

    public void reset() {
        candidates.clear();
        lastSent.clear();
        ticks = 0;
        soulSeen = false;
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
        if (ticks % CLAIM_INTERVAL == 0) {
            claim(server);
        }
        if (ticks % SCAN_INTERVAL == 1) {
            rescan();
        }
        if (ticks % POINT_INTERVAL == 0) {
            point(server);
        }
    }

    // ------------------------------------------------------------------ claiming

    /** The first player found carrying a relic of an unclaimed kind claims it. */
    private void claim(MinecraftServer server) {
        for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
            boolean changed = false;
            for (ItemStack stack : player.inventory.mainInventory) {
                changed |= claim(stack, player);
            }
            for (ItemStack stack : player.inventory.armorInventory) {
                changed |= claim(stack, player);
            }
            for (ItemStack stack : player.inventory.offHandInventory) {
                changed |= claim(stack, player);
            }
            if (changed) {
                player.inventoryContainer.detectAndSendChanges();
            }
        }
    }

    private static boolean claim(ItemStack stack, EntityPlayer player) {
        Relics.Kind kind = Relics.kind(stack);
        return kind != null && !Relics.isClaimed(kind) && Relics.claim(stack, player);
    }

    /**
     * Tesseracts and Space Stones as they enter a world. A Norse village only sets out a
     * Tesseract while none has been claimed (HeroesExpansion would otherwise set out
     * another whenever the church's chunks generate again after the first was taken), and
     * the unclaimed copies it set out earlier are gone once one has been. The Space Stone
     * that falls out of the true Tesseract when it is broken open is the true Space Stone.
     */
    @SubscribeEvent
    public void onEntityJoin(EntityJoinWorldEvent event) {
        if (event.getWorld().isRemote || !(event.getEntity() instanceof EntityItemIndestructible)
                || DimensionManager.getWorld(0) == null) {
            return;
        }
        EntityItemIndestructible entity = (EntityItemIndestructible) event.getEntity();
        ItemStack stack = entity.getItem();
        if (stack.getItem() == HEItems.TESSERACT && Relics.mark(stack) == null) {
            boolean claimed = Relics.isClaimed(Relics.Kind.TESSERACT);
            if (fromNorseVillage()) {
                if (claimed) {
                    entity.setDead();
                } else {
                    ItemStack copy = stack.copy();
                    Relics.markVillageCopy(copy);
                    entity.setItem(copy);
                }
            } else if (claimed && Relics.isVillageCopy(stack)) {
                entity.setDead();
            }
        } else if (stack.getItem() == HEItems.SPACE_STONE && Relics.mark(stack) == null) {
            AxisAlignedBB box = entity.getEntityBoundingBox().grow(2D);
            for (EntityTesseract tesseract : entity.world.getEntitiesWithinAABB(EntityTesseract.class, box)) {
                UUID mark = Relics.mark(tesseract.getItem());
                if (tesseract.getBreakTimer() >= 98 && mark != null) {
                    ItemStack copy = stack.copy();
                    Relics.stamp(copy, mark);
                    entity.setItem(copy);
                    if (Relics.isTrue(copy)) {
                        Relics.opened();
                    }
                    Eternity.LOG.info("The Tesseract has been broken open; the Space Stone inside it is free.");
                    return;
                }
            }
        }
    }

    /** Whether HeroesExpansion's village generation is what is spawning this. */
    private static boolean fromNorseVillage() {
        for (StackTraceElement frame : Thread.currentThread().getStackTrace()) {
            if (frame.getClassName().startsWith("lucraft.mods.heroesexpansion.worldgen")) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ scanning

    private void rescan() {
        List<Candidate> found = new ArrayList<>();
        boolean tesseractClaimed = Relics.isClaimed(Relics.Kind.TESSERACT);
        for (WorldServer world : DimensionManager.getWorlds()) {
            int dim = world.provider.getDimension();
            for (Entity entity : new ArrayList<>(world.loadedEntityList)) {
                try {
                    keep(entity, tesseractClaimed);
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

        RelicLedger ledger = RelicLedger.get();
        Set<String> seen = new HashSet<>();
        for (Candidate c : found) {
            seen.add(c.item);
            ledger.lastSeen.put(c.item, new Sighting(c.dimension, c.x, c.y, c.z, c.holderName));
        }
        if (!found.isEmpty()) {
            ledger.markDirty();
        }

        candidates.clear();
        candidates.addAll(found);
        for (Map.Entry<String, Sighting> entry : ledger.lastSeen.entrySet()) {
            Sighting s = entry.getValue();
            // If that spot is loaded and the relic was not found there, it has moved on.
            if (!seen.contains(entry.getKey()) && !isLoaded(s.dimension, s.x, s.y, s.z)) {
                candidates.add(new Candidate(entry.getKey(), s.dimension, s.x, s.y, s.z, null));
            }
        }
        addTemples(ledger, seen);
        addInBetweener();
    }

    /**
     * Relics lying in the world are never lost. One sinking into the void is held at its
     * edge (Lucraft stops them falling at y=0, but they still creep down until the void
     * takes them), and an unclaimed village Tesseract still out after one was claimed fades.
     */
    private static void keep(Entity entity, boolean tesseractClaimed) {
        if (!(entity instanceof EntityItemIndestructible) || entity.isDead) {
            return;
        }
        if (entity.posY < 0D) {
            entity.setNoGravity(true);
            entity.motionX = entity.motionY = entity.motionZ = 0D;
            entity.setPositionAndUpdate(entity.posX, 0D, entity.posZ);
        }
        ItemStack stack = ((EntityItemIndestructible) entity).getItem();
        if (tesseractClaimed && entity instanceof EntityTesseract && Relics.mark(stack) == null && Relics.isVillageCopy(stack)) {
            entity.setDead();
        }
    }

    private void scanEntity(Entity entity, int dim, List<Candidate> found) {
        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;
            for (ItemStack stack : player.inventory.mainInventory) {
                add(stack, dim, entity, player, found);
            }
            for (ItemStack stack : player.inventory.armorInventory) {
                add(stack, dim, entity, player, found);
            }
            for (ItemStack stack : player.inventory.offHandInventory) {
                add(stack, dim, entity, player, found);
            }
            add(player.inventory.getItemStack(), dim, entity, player, found);
            scanInventory(player.getInventoryEnderChest(), dim, entity, player, found);
            return;
        }
        if (entity instanceof EntityAstralRegulator) {
            found.add(new Candidate(id(EternityItems.ASTRAL_REGULATOR), dim, entity.posX, entity.posY, entity.posZ, null));
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
            RelicScanner.forEachRelic(stack, relic -> found.add(new Candidate(id(relic), dim, x, y, z, null)));
        }
    }

    private void scanHandler(@Nullable IItemHandler handler, int dim, Entity at, List<Candidate> found) {
        if (handler != null) {
            for (int i = 0; i < handler.getSlots(); i++) {
                add(handler.getStackInSlot(i), dim, at, null, found);
            }
        }
    }

    private void scanInventory(IInventory inventory, int dim, Entity at, @Nullable EntityPlayer holder, List<Candidate> found) {
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            add(inventory.getStackInSlot(i), dim, at, holder, found);
        }
    }

    private void add(ItemStack stack, int dim, Entity at, @Nullable EntityPlayer holder, List<Candidate> found) {
        RelicScanner.forEachRelic(stack, relic -> found.add(new Candidate(id(relic), dim, at.posX, at.posY, at.posZ, holder)));
    }

    /** InfinityCraft stones still waiting in the temples they were generated into. */
    private void addTemples(RelicLedger ledger, Set<String> seen) {
        WorldData data = WorldData.get(DimensionManager.getWorld(0));
        temple(ledger, seen, anvil.infinity.items.Items.POWER_STONE, data.power, ModConfig.Worldgen.powerDim, data.powerx, data.powerz);
        temple(ledger, seen, anvil.infinity.items.Items.SPACE_STONE, data.space, ModConfig.Worldgen.spaceDim, data.spacex, data.spacez);
        temple(ledger, seen, anvil.infinity.items.Items.REALITY_STONE, data.reality, ModConfig.Worldgen.realityDim, data.realityx, data.realityz);
        temple(ledger, seen, anvil.infinity.items.Items.SOUL_STONE, data.soul, ModConfig.Worldgen.soulDim, data.soulx, data.soulz);
        temple(ledger, seen, anvil.infinity.items.Items.TIME_STONE, data.time, ModConfig.Worldgen.timeDim, data.timex, data.timez);
        temple(ledger, seen, anvil.infinity.items.Items.MIND_STONE, data.mind, ModConfig.Worldgen.mindDim, data.mindx, data.mindz);
    }

    private void temple(RelicLedger ledger, Set<String> seen, @Nullable Item stone, boolean generated, int dim, int x, int z) {
        if (stone == null || !generated || stone.getRegistryName() == null) {
            return;
        }
        String id = stone.getRegistryName().toString();
        if (!seen.contains(id) && !ledger.lastSeen.containsKey(id)) {
            candidates.add(new Candidate(id, dim, x + 0.5D, 64D, z + 0.5D, null));
        }
    }


    /**
     * The Soul Gem on the In-Betweener's brow, at the junction in the Nexus, while it is
     * still there. There is only ever one Soul Stone: if one turned up anywhere else, his
     * brow is empty.
     */
    private void addInBetweener() {
        Item soul = InBetweenerDirector.soulStone();
        String id = soul == null || soul.getRegistryName() == null ? null : soul.getRegistryName().toString();
        soulSeen = false;
        for (Candidate c : candidates) {
            if (c.item.equals(id)) {
                soulSeen = true;
            }
        }
        if (id != null && EternityConfig.inbetweener.enabled && InBetweenerDirector.INSTANCE.wearsGem()) {
            candidates.add(new Candidate(id, Nexus.dimension(), Nexus.JUNCTION_X, Nexus.BUBBLE_Y, Nexus.JUNCTION_Z, null));
        }
    }

    /** Whether a Soul Stone exists somewhere other than the In-Betweener's brow. */
    public boolean soulElsewhere() {
        return soulSeen;
    }
    private static boolean isLoaded(int dim, double x, double y, double z) {
        WorldServer world = DimensionManager.getWorld(dim);
        return world != null && world.isBlockLoaded(new BlockPos(x, y, z));
    }

    private static String id(ItemStack stack) {
        return id(stack.getItem());
    }

    private static String id(Item item) {
        return item.getRegistryName() == null ? "unknown" : item.getRegistryName().toString();
    }

    // ------------------------------------------------------------------ pointing

    private void point(MinecraftServer server) {
        for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
            if (!holdsTracker(player)) {
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
                    : previous == null || Math.abs(previous.x - nearest.x) > 0.5D || Math.abs(previous.z - nearest.z) > 0.5D
                    || !Objects.equals(previous.item, nearest.item);
            if (changed) {
                lastSent.put(player.getUniqueID(), nearest);
                EternityNetwork.send(nearest == null ? new MsgRelicTarget() : new MsgRelicTarget(nearest.x, nearest.z, nearest.item), player);
            }
        }
    }

    private static boolean holdsTracker(EntityPlayer player) {
        for (int i = 0; i < 9; i++) {
            if (player.inventory.mainInventory.get(i).getItem() instanceof ItemRelicTracker) {
                return true;
            }
        }
        return player.getHeldItemOffhand().getItem() instanceof ItemRelicTracker;
    }
}
