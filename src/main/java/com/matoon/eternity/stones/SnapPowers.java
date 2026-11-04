package com.matoon.eternity.stones;

import anvil.infinity.capabilities.GauntletUserInformation;
import anvil.infinity.capabilities.ICapabilityPlayerData;
import anvil.infinity.helpers.GauntelHelper;
import anvil.infinity.registry.Effects;
import anvil.infinity.snap.SnapHelper;
import anvil.infinity.snap.SnapResult;
import com.matoon.eternity.world.Teleport;
import lucraft.mods.lucraftcore.infinity.items.ItemInfinityGauntlet;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The snap, all six ways the Mind Stone can set it. InfinityCraft does four of them; bringing back the dusted and
 * recreating the universe it left unfinished, and those are done here.
 * <ul>
 * <li><b>Bring back the dusted.</b> Everyone the snap has ever taken comes back, where they fell: every creature whole,
 * and every player with all they carried and their experience, which the dust kept. A player dead or away when called
 * comes back the moment they next stand in the world.</li>
 * <li><b>Recreate the universe.</b> Everything the snaps did undone, and the world made new: the dusted brought back,
 * the dust settling on anyone still fading stopped, every player and every peaceful creature made whole and cured, the
 * fires out, every burn the snap has left healed but the one it leaves now, the storm gone and a new morning come.</li>
 * </ul>
 * Whichever way it is set, every snap that works is heard and seen by everyone near, burns the arm that made it deeper
 * (see {@link SnapScars}) and wears the gauntlet (see {@link GauntletWear}). InfinityCraft only reported "Kill half" as
 * having worked, so before this nothing else did any of that.
 */
public final class SnapPowers {

    public static final SnapPowers INSTANCE = new SnapPowers();

    /** Those turning to dust right now, with their records, so what they drop goes into the dust with them. */
    private static final Map<UUID, NBTTagCompound> DUSTING = new HashMap<>();
    private static final Map<UUID, Integer> DUSTING_SINCE = new HashMap<>();
    /** Players called back while dead or away, coming back a moment after they next stand in the world. */
    private static final Map<UUID, NBTTagCompound> RETURNING = new HashMap<>();
    private static final Map<UUID, Integer> RETURN_AT = new HashMap<>();

    /** What the dusted die of: "was dusted away". */
    private static final DamageSource DUST = new DamageSource("eternity.dusted").setDamageBypassesArmor().setDamageIsAbsolute();

    private static int snapDimension = Integer.MIN_VALUE;

    private SnapPowers() {
    }

    public static void reset() {
        snapDimension = Integer.MIN_VALUE;
        DUSTING.clear();
        DUSTING_SINCE.clear();
        RETURNING.clear();
        RETURN_AT.clear();
    }

    /** The way the Mind Stone has set the snap. */
    public static SnapResult mode(EntityLivingBase entity) {
        ICapabilityPlayerData data = GauntletUserInformation.getDataByEntity(entity);
        SnapResult mode = data == null ? null : data.getSnapResult();
        return mode == null ? SnapResult.KILLHALF : mode;
    }

    /** The Mind Stone setting the snap to its next way, and saying which. */
    public static boolean cycle(EntityLivingBase entity) {
        ICapabilityPlayerData data = GauntletUserInformation.getDataByEntity(entity);
        if (data == null) {
            return false;
        }
        SnapResult next = mode(entity).next();
        data.setSnapResult(next);
        if (entity instanceof EntityPlayer) {
            ((EntityPlayer) entity).sendStatusMessage(new TextComponentString(TextFormatting.GRAY + "The snap will: "
                    + TextFormatting.GOLD + name(next)), true);
        }
        return true;
    }

    public static String name(SnapResult mode) {
        switch (mode) {
            case DESTROYSTONES:
                return "Destroy the Stones";
            case BRINGBACK:
                return "Bring back the dusted";
            case RECREATE:
                return "Recreate the Universe";
            case KILLEVIL:
                return "Kill evil entities";
            case CREATIVE:
                return "Change to Creative";
            default:
                return "Kill half";
        }
    }

    /** The snap, with all six stones in hand. Whether it worked. */
    public static boolean snap(EntityLivingBase entity) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (entity.world.isRemote || server == null || !GauntelHelper.hasFullGauntlet(entity)) {
            return false;
        }
        // Read before the snap: destroying the stones swaps the gauntlet for an empty one.
        EnumHand hand = entity.getHeldItemMainhand().getItem() instanceof ItemInfinityGauntlet ? EnumHand.MAIN_HAND : EnumHand.OFF_HAND;
        EnumHandSide side = hand == EnumHand.MAIN_HAND ? entity.getPrimaryHand() : entity.getPrimaryHand().opposite();
        SnapResult mode = mode(entity);
        boolean done;
        switch (mode) {
            case BRINGBACK:
                bringBack(server, entity, true);
                done = true;
                break;
            case RECREATE:
                recreate(server, entity);
                done = true;
                break;
            case DESTROYSTONES:
            case KILLEVIL:
                // InfinityCraft does these, but reports them as having failed.
                SnapHelper.snap(entity);
                done = true;
                break;
            default:
                done = SnapHelper.snap(entity);
                break;
        }
        if (!done) {
            return false;
        }
        // The gauntlet wears, the arm burns.
        ItemStack gauntlet = entity.getHeldItem(hand);
        if (gauntlet.getItem() instanceof ItemInfinityGauntlet) {
            GauntletWear.wear(gauntlet, mode == SnapResult.DESTROYSTONES);
        }
        if (entity instanceof EntityPlayerMP) {
            InfinityPatches.snapped((EntityPlayerMP) entity, side, SnapScars.cost(mode), mode == SnapResult.RECREATE);
        }
        return done;
    }

    // ------------------------------------------------------------------ bringing back the dusted

    private static void bringBack(MinecraftServer server, EntityLivingBase snapper, boolean announce) {
        TheDusted dusted = TheDusted.get(server);
        int creatures = 0, players = 0;
        for (NBTTagCompound record : dusted.takeAll()) {
            if (record.getBoolean("Player")) {
                UUID id = UUID.fromString(record.getString("UUID"));
                EntityPlayerMP player = server.getPlayerList().getPlayerByUUID(id);
                if (player != null && player.isEntityAlive()) {
                    returnPlayer(player, record);
                } else {
                    record.setBoolean("Called", true);
                    dusted.keep(record);
                }
                players++;
            } else if (revive(server, record)) {
                creatures++;
            }
        }
        if (!announce) {
            return;
        }
        if (creatures + players == 0) {
            if (snapper instanceof EntityPlayerMP) {
                ((EntityPlayerMP) snapper).sendStatusMessage(new TextComponentString(TextFormatting.GRAY + "There is no one left in the dust to bring back."), true);
            }
            return;
        }
        String brought = players > 0 && creatures > 0 ? players + (players == 1 ? " player and " : " players and ") + creatures + (creatures == 1 ? " creature" : " creatures")
                : players > 0 ? players + (players == 1 ? " player" : " players") : creatures + (creatures == 1 ? " creature" : " creatures");
        announce(server, snapper, "The dusted return. (" + brought + ")", TextFormatting.GOLD);
    }

    /** A dusted creature, whole again where it fell. */
    private static boolean revive(MinecraftServer server, NBTTagCompound record) {
        int dimension = record.getInteger("Dim");
        if (!DimensionManager.isDimensionRegistered(dimension)) {
            return false;
        }
        WorldServer world = server.getWorld(dimension);
        double x = record.getDouble("X"), y = record.getDouble("Y"), z = record.getDouble("Z");
        // Its chunk may be long unloaded; spawning needs it there.
        world.getChunk(new BlockPos(x, y, z));
        Entity entity = EntityList.createEntityFromNBT(record.getCompoundTag("Entity"), world);
        if (entity == null) {
            return false;
        }
        if (server.getEntityFromUuid(entity.getUniqueID()) != null) {
            entity.setUniqueId(UUID.randomUUID());
        }
        entity.setLocationAndAngles(x, y, z, record.getFloat("Yaw"), record.getFloat("Pitch"));
        if (entity instanceof EntityLivingBase) {
            EntityLivingBase living = (EntityLivingBase) entity;
            living.clearActivePotions();
            living.setHealth(living.getMaxHealth());
            living.deathTime = 0;
            living.hurtTime = 0;
        }
        entity.extinguish();
        if (!world.spawnEntity(entity)) {
            return false;
        }
        reform(world, entity);
        return true;
    }

    /** A dusted player, back where they fell, with what they carried and their experience. */
    private static void returnPlayer(EntityPlayerMP player, NBTTagCompound record) {
        int dimension = record.getInteger("Dim");
        if (DimensionManager.isDimensionRegistered(dimension)) {
            Teleport.player(player, dimension, record.getDouble("X"), record.getDouble("Y"), record.getDouble("Z"), false);
            player.connection.setPlayerLocation(record.getDouble("X"), record.getDouble("Y"), record.getDouble("Z"), record.getFloat("Yaw"), record.getFloat("Pitch"));
        }
        NBTTagList items = record.getTagList("Items", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < items.tagCount(); i++) {
            ItemStack stack = new ItemStack(items.getCompoundTagAt(i));
            if (!stack.isEmpty() && !player.inventory.addItemStackToInventory(stack)) {
                player.dropItem(stack, false);
            }
        }
        if (record.hasKey("Xp")) {
            player.addExperience(record.getInteger("Xp"));
        }
        player.setHealth(player.getMaxHealth());
        player.getFoodStats().addStats(20, 20F);
        player.extinguish();
        player.fallDistance = 0F;
        reform((WorldServer) player.world, player);
        player.sendStatusMessage(new TextComponentString(TextFormatting.GOLD + "You are brought back."), true);
    }

    // ------------------------------------------------------------------ recreating the universe

    private static void recreate(MinecraftServer server, EntityLivingBase snapper) {
        bringBack(server, snapper, false);
        for (WorldServer world : server.worlds) {
            for (Entity entity : new ArrayList<>(world.loadedEntityList)) {
                if (!(entity instanceof EntityLivingBase) || entity.isDead) {
                    continue;
                }
                EntityLivingBase living = (EntityLivingBase) entity;
                // Nobody still fading is taken.
                living.removePotionEffect(Effects.snapEffect);
                // The hostile and the great are left as they are: the universe is not remade for them.
                if (living instanceof IMob || living instanceof EntityDragon || living instanceof EntityWither || !living.isNonBoss()) {
                    continue;
                }
                for (PotionEffect effect : new ArrayList<>(living.getActivePotionEffects())) {
                    if (effect.getPotion().isBadEffect()) {
                        living.removePotionEffect(effect.getPotion());
                    }
                }
                living.setHealth(living.getMaxHealth());
                living.extinguish();
                if (living instanceof EntityPlayer) {
                    ((EntityPlayer) living).getFoodStats().addStats(20, 20F);
                }
            }
        }
        // A new morning, under a clear sky.
        WorldServer overworld = server.getWorld(0);
        long time = overworld.getWorldTime();
        overworld.setWorldTime((time / 24000L + 1L) * 24000L);
        WorldInfo info = overworld.getWorldInfo();
        info.setCleanWeatherTime(12000);
        info.setRainTime(0);
        info.setThunderTime(0);
        info.setRaining(false);
        info.setThundering(false);
        if (snapper instanceof EntityPlayerMP) {
            // Every burn but the one this snap leaves.
            SnapScars.healAllBut(server, (EntityPlayerMP) snapper);
        }
        announce(server, snapper, "The universe is made anew.", TextFormatting.GOLD);
    }

    // ------------------------------------------------------------------ turning to dust

    /**
     * The snap's dust settles on its victims as a potion that runs out a few seconds later; InfinityCraft then just
     * sets their health to nothing, which skips dying altogether: no death, no message, nothing to keep. So the moment
     * before it would, the potion is taken off and they die properly, of dust, and are kept. Someone the snap reached
     * who has since gone to another dimension is spared.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        if (entity.world.isRemote) {
            return;
        }
        PotionEffect dust = entity.getActivePotionEffect(Effects.snapEffect);
        if (dust == null || dust.getDuration() > 1) {
            return;
        }
        entity.removePotionEffect(Effects.snapEffect);
        if (snapDimension != Integer.MIN_VALUE && entity.dimension != snapDimension || entity.isDead || entity.getHealth() <= 0F) {
            return;
        }
        MinecraftServer server = entity.getServer();
        if (server == null) {
            return;
        }
        DUSTING.put(entity.getUniqueID(), record(entity));
        DUSTING_SINCE.put(entity.getUniqueID(), server.getTickCounter());
        entity.setHealth(0F);
        entity.onDeath(DUST);
        NBTTagCompound record = DUSTING.get(entity.getUniqueID());
        if (record != null && record.getBoolean("Spared")) {
            // Something would not let them die.
            DUSTING.remove(entity.getUniqueID());
            entity.setHealth(1F);
        }
    }

    /** Where the snap was made: its dust only takes those still in that dimension. */
    static void snappedIn(int dimension) {
        snapDimension = dimension;
    }

    /** After everything else has had its say about the death: if it is dust, it is kept. */
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onDeath(LivingDeathEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        NBTTagCompound record = DUSTING.get(entity.getUniqueID());
        MinecraftServer server = entity.getServer();
        if (record == null || server == null) {
            return;
        }
        if (event.isCanceled()) {
            record.setBoolean("Spared", true);
            return;
        }
        if (record.getBoolean("Player") || record.hasKey("Entity")) {
            TheDusted.get(server).add(record);
        }
        crumble((WorldServer) entity.world, entity);
        if (!(entity instanceof EntityPlayer)) {
            // Dust leaves no body to fall.
            entity.setDead();
        }
    }

    private static NBTTagCompound record(EntityLivingBase entity) {
        NBTTagCompound record = new NBTTagCompound();
        record.setInteger("Dim", entity.dimension);
        record.setDouble("X", entity.posX);
        record.setDouble("Y", entity.posY);
        record.setDouble("Z", entity.posZ);
        record.setFloat("Yaw", entity.rotationYaw);
        record.setFloat("Pitch", entity.rotationPitch);
        if (entity instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) entity;
            record.setBoolean("Player", true);
            record.setString("UUID", player.getUniqueID().toString());
            if (!entity.world.getGameRules().getBoolean("keepInventory")) {
                record.setInteger("Xp", experiencePoints(player));
                record.setTag("Items", new NBTTagList());
            }
        } else {
            ResourceLocation key = EntityList.getKey(entity);
            if (key == null) {
                // Nothing that could be made again; it goes to dust all the same.
                return record;
            }
            NBTTagCompound data = new NBTTagCompound();
            entity.writeToNBT(data);
            data.setString("id", key.toString());
            data.removeTag("ActiveEffects");
            data.removeTag("Passengers");
            record.setTag("Entity", data);
        }
        return record;
    }

    /** What the dusted carried goes into the dust with them, not onto the ground. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDrops(LivingDropsEvent event) {
        NBTTagCompound record = DUSTING.get(event.getEntityLiving().getUniqueID());
        if (record == null) {
            return;
        }
        if (record.getBoolean("Player") && record.hasKey("Items")) {
            NBTTagList items = record.getTagList("Items", Constants.NBT.TAG_COMPOUND);
            for (EntityItem drop : event.getDrops()) {
                items.appendTag(drop.getItem().writeToNBT(new NBTTagCompound()));
            }
            MinecraftServer server = event.getEntityLiving().getServer();
            if (server != null) {
                TheDusted.get(server).markDirty();
            }
        }
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onExperience(LivingExperienceDropEvent event) {
        if (DUSTING.containsKey(event.getEntityLiving().getUniqueID())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        call(event.player);
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        call(event.player);
    }

    /** A player who was called back while they could not come: they come now, once settled into the world. */
    private static void call(EntityPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null || !(player instanceof EntityPlayerMP)) {
            return;
        }
        NBTTagCompound record = TheDusted.get(server).takeCalled(player.getUniqueID().toString());
        if (record != null) {
            RETURNING.put(player.getUniqueID(), record);
            RETURN_AT.put(player.getUniqueID(), server.getTickCounter() + 20);
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) {
            return;
        }
        int now = server.getTickCounter();
        // A dying player drops their experience a second after they fall.
        DUSTING_SINCE.entrySet().removeIf(entry -> {
            boolean old = now - entry.getValue() > 60;
            if (old) {
                DUSTING.remove(entry.getKey());
            }
            return old;
        });
        for (Iterator<Map.Entry<UUID, Integer>> it = RETURN_AT.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Integer> entry = it.next();
            if (now < entry.getValue()) {
                continue;
            }
            it.remove();
            NBTTagCompound record = RETURNING.remove(entry.getKey());
            EntityPlayerMP player = server.getPlayerList().getPlayerByUUID(entry.getKey());
            if (record == null) {
                continue;
            }
            if (player != null && player.isEntityAlive()) {
                returnPlayer(player, record);
            } else {
                // Gone again before they could come: still called.
                TheDusted.get(server).keep(record);
            }
        }
    }

    // ------------------------------------------------------------------ how it looks and sounds

    /** Ash blowing off them as they go. */
    private static void crumble(WorldServer world, Entity entity) {
        double x = entity.posX, y = entity.posY + entity.height * 0.5D, z = entity.posZ;
        double w = entity.width * 0.5D, h = entity.height * 0.5D;
        world.spawnParticle(EnumParticleTypes.FALLING_DUST, x, y, z, 40 + (int) (entity.height * 25), w, h, w, 0.02D,
                Block.getStateId(Blocks.SOUL_SAND.getDefaultState()));
        world.spawnParticle(EnumParticleTypes.FALLING_DUST, x, y, z, 15, w, h, w, 0.02D,
                Block.getStateId(Blocks.GRAVEL.getDefaultState()));
        world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, x, y, z, 12, w, h, w, 0.01D);
        world.playSound(null, x, y, z, SoundEvents.BLOCK_SAND_BREAK, SoundCategory.NEUTRAL, 0.8F, 0.6F);
    }

    /** The dust gathering back into them. */
    private static void reform(WorldServer world, Entity entity) {
        double x = entity.posX, y = entity.posY + entity.height * 0.5D, z = entity.posZ;
        double w = entity.width * 0.6D, h = entity.height * 0.5D;
        world.spawnParticle(EnumParticleTypes.FALLING_DUST, x, y, z, 50, w, h, w, 0.0D,
                Block.getStateId(Blocks.SOUL_SAND.getDefaultState()));
        world.spawnParticle(EnumParticleTypes.END_ROD, x, y, z, 24, w, h, w, 0.03D);
        world.spawnParticle(EnumParticleTypes.SPELL_INSTANT, x, y, z, 20, w, h, w, 0.0D);
        world.playSound(null, x, y, z, SoundEvents.ENTITY_ILLAGER_MIRROR_MOVE, SoundCategory.NEUTRAL, 0.9F, 1.3F);
    }

    private static void announce(MinecraftServer server, EntityLivingBase snapper, String text, TextFormatting color) {
        TextComponentString message = new TextComponentString(snapper.getName() + ": " + text);
        message.getStyle().setColor(color);
        message.getStyle().setBold(Boolean.TRUE);
        server.getPlayerList().sendMessage((ITextComponent) message);
    }

    /** All the experience a player holds, in points: their levels' worth and the way into the next. */
    private static int experiencePoints(EntityPlayer player) {
        int points = 0;
        for (int level = 0; level < player.experienceLevel; level++) {
            points += level >= 30 ? 112 + (level - 30) * 9 : level >= 15 ? 37 + (level - 15) * 5 : 7 + level * 2;
        }
        return points + Math.round(player.experience * player.xpBarCap());
    }
}
