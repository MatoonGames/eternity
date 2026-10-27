package com.matoon.eternity.world;

import com.matoon.eternity.EternityConfig;
import com.matoon.eternity.compat.InfinityStones;
import com.matoon.eternity.compat.Relics;
import com.matoon.eternity.entity.EntityDestroyer;
import com.matoon.eternity.entity.EntityDestroyerArmor;
import com.matoon.eternity.entity.EntityEinherjar;
import com.matoon.eternity.item.EternityItems;
import com.matoon.eternity.item.ItemAstralRegulator;
import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.network.MsgTribunalFx;
import lucraft.mods.heroesexpansion.entities.EntityTesseract;
import lucraft.mods.heroesexpansion.entities.EntityThorWeapon;
import lucraft.mods.heroesexpansion.entities.EntityThrownThorWeapon;
import lucraft.mods.heroesexpansion.items.HEItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * The rules of Odin's Vault. Whoever comes in is met at the head of the steps by two of the
 * Einherjar; some twenty seconds later the Destroyer wakes in its niche at the far end, and
 * sooner if anyone lifts the Casket of Ancient Winters off its pedestal. Mjolnir and the
 * Tesseract are set out on their stands the first time anyone comes (the Tesseract only while
 * nobody has claimed one). Nothing in the vault can be built or broken. The doors at the foot of
 * the steps lead back to wherever you came from.
 *
 * Nothing here is written to chunks: whenever the vault is entered after standing empty, fresh
 * guards are posted and the Destroyer stands dormant in its niche again, unless it has fallen
 * and Odin's smiths have not yet mended it.
 */
public final class VaultDirector {

    public static final VaultDirector INSTANCE = new VaultDirector();

    private static final String RETURN = "eternityVaultReturn";

    /** Someone is inside, and since when; and whether the guards have taken their posts yet. */
    private boolean occupied;
    private int alarm;
    private boolean posted;
    /** Everything in the vault pulls its blows, for looking at. */
    private boolean calm;
    private final List<EntityEinherjar> guards = new ArrayList<>();

    private VaultDirector() {
    }

    public void reset() {
        occupied = false;
        posted = false;
        alarm = 0;
        calm = false;
        guards.clear();
    }

    // ------------------------------------------------------------------ coming and going

    /** Sends someone into the vault, at the foot of the steps, remembering where they came from. */
    public void enter(EntityPlayerMP player) {
        if (player.dimension != OdinsVault.dimension()) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("dim", player.dimension);
            tag.setDouble("x", player.posX);
            tag.setDouble("y", player.posY);
            tag.setDouble("z", player.posZ);
            player.getEntityData().setTag(RETURN, tag);
        }
        player.rotationYaw = 180F;
        player.rotationPitch = 0F;
        Teleport.player(player, OdinsVault.dimension(), OdinsVault.ARRIVE_X, OdinsVault.ARRIVE_Y, OdinsVault.ARRIVE_Z, false);
        send(player);
    }

    /**
     * Back out through the doors to wherever they came from, or the overworld's spawn if nobody knows.
     * Anyone inside the Destroyer Armor takes it with them (see {@link Armory}).
     */
    public void leave(EntityPlayerMP player) {
        NBTTagCompound tag = player.getEntityData().getCompoundTag(RETURN);
        player.getEntityData().removeTag(RETURN);
        if (tag.hasKey("dim") && DimensionManager.isDimensionRegistered(tag.getInteger("dim")) && tag.getInteger("dim") != OdinsVault.dimension()) {
            Teleport.player(player, tag.getInteger("dim"), tag.getDouble("x"), tag.getDouble("y"), tag.getDouble("z"), true);
            return;
        }
        WorldServer overworld = DimensionManager.getWorld(0);
        BlockPos spawn = overworld != null ? overworld.getSpawnPoint() : new BlockPos(0, 80, 0);
        Teleport.player(player, 0, spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D, true);
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof EntityPlayerMP) || event.player.dimension != OdinsVault.dimension()) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        // In the armour, it is the armour's front that reaches the doors.
        Entity body = player.getRidingEntity() instanceof EntityDestroyerArmor ? player.getRidingEntity() : player;
        double reach = body == player ? 0D : body.width * 0.5D + 0.1D;
        if (OdinsVault.atDoors(body.posX, body.posY, body.posZ + reach) && !player.isSpectator()) {
            leave(player);
        }
    }

    @SubscribeEvent
    public void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.toDim == OdinsVault.dimension() && event.player instanceof EntityPlayerMP) {
            send((EntityPlayerMP) event.player);
        }
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player.dimension == OdinsVault.dimension() && event.player instanceof EntityPlayerMP) {
            send((EntityPlayerMP) event.player);
        }
    }

    // ------------------------------------------------------------------ the vault, while anyone is in it

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote || event.world.provider.getDimension() != OdinsVault.dimension()) {
            return;
        }
        WorldServer world = (WorldServer) event.world;
        boolean anyone = false;
        for (EntityPlayer player : world.playerEntities) {
            anyone |= !player.isSpectator();
        }
        if (!anyone) {
            if (occupied) {
                empty(world);
            }
            return;
        }
        VaultData data = VaultData.get();
        if (!occupied) {
            occupied = true;
            alarm = 0;
            posted = false;
        }
        // Only once the head of the steps has loaded, or they would be posted into nothing.
        if (!posted && world.isBlockLoaded(new BlockPos(OdinsVault.POST_X, OdinsVault.POST_Y, OdinsVault.POST_Z))
                && world.isBlockLoaded(new BlockPos(1D - OdinsVault.POST_X, OdinsVault.POST_Y, OdinsVault.POST_Z))) {
            postGuards(world);
            posted = true;
        }
        setOut(world, data);
        EntityDestroyer destroyer = destroyer(world);
        long now = DimensionManager.getWorld(0).getTotalWorldTime();
        if (destroyer == null && now >= data.destroyerBack
                && world.isBlockLoaded(new BlockPos(OdinsVault.NICHE_X, OdinsVault.NICHE_Y, OdinsVault.NICHE_Z))) {
            destroyer = new EntityDestroyer(world);
            destroyer.install(OdinsVault.NICHE_X, OdinsVault.NICHE_Y, OdinsVault.NICHE_Z, 0F);
            destroyer.setCalm(calm);
            world.spawnEntity(destroyer);
        }
        alarm++;
        if (destroyer != null && alarm == EternityConfig.vault.destroyerDelay * 20 && destroyer.getState() == EntityDestroyer.State.DORMANT) {
            wake(destroyer);
        }
        if (world.getTotalWorldTime() % 100 == 0) {
            EternityNetwork.sendToDimension(state(), OdinsVault.dimension());
        }
    }

    /** Everyone has gone: the guards go back to their barracks, and the Destroyer to its niche, unless it lies broken. */
    private void empty(WorldServer world) {
        occupied = false;
        alarm = 0;
        for (Entity entity : new ArrayList<>(world.loadedEntityList)) {
            if (entity instanceof EntityEinherjar || entity instanceof EntityDestroyer) {
                entity.setDead();
            }
        }
        guards.clear();
    }

    /** Two of the Einherjar at the head of the steps, one either side, facing down them. */
    public void postGuards(WorldServer world) {
        for (EntityEinherjar guard : guards) {
            guard.setDead();
        }
        guards.clear();
        for (int side = -1; side <= 1; side += 2) {
            EntityEinherjar guard = new EntityEinherjar(world);
            guard.post(OdinsVault.ARRIVE_X + side * (OdinsVault.POST_X - 0.5D), OdinsVault.POST_Y, OdinsVault.POST_Z, 0F, side);
            guard.setCalm(calm);
            world.spawnEntity(guard);
            guards.add(guard);
        }
    }

    /** Mjolnir and the Tesseract on their stands, the first time anyone comes. */
    private static void setOut(WorldServer world, VaultData data) {
        if (!data.mjolnirSet) {
            Vec3d at = OdinsVault.Relic.MJOLNIR.base().add(0D, OdinsVault.Relic.MJOLNIR.height, 0D);
            if (world.isBlockLoaded(new BlockPos(at))) {
                EntityThorWeapon hammer = new EntityThorWeapon(world, at.x, at.y, at.z, new ItemStack(HEItems.MJOLNIR));
                hammer.motionX = hammer.motionY = hammer.motionZ = 0D;
                world.spawnEntity(hammer);
                data.mjolnirSet = true;
                data.markDirty();
            }
        }
        if (!data.tesseractSet && !Relics.isClaimed(Relics.Kind.TESSERACT)) {
            Vec3d at = OdinsVault.Relic.TESSERACT.base().add(0D, OdinsVault.Relic.TESSERACT.height + 1D / 16D, 0D);
            if (world.isBlockLoaded(new BlockPos(at))) {
                ItemStack stack = new ItemStack(HEItems.TESSERACT);
                // Like a village's, it is only a copy until someone takes it, and gone if someone claims another first.
                Relics.markVillageCopy(stack);
                EntityTesseract tesseract = new EntityTesseract(world, at.x, at.y, at.z, stack);
                tesseract.motionX = tesseract.motionY = tesseract.motionZ = 0D;
                world.spawnEntity(tesseract);
                data.tesseractSet = true;
                data.markDirty();
            }
        }
    }

    @Nullable
    public static EntityDestroyer destroyer(World world) {
        for (Entity entity : world.loadedEntityList) {
            if (entity instanceof EntityDestroyer && !entity.isDead) {
                return (EntityDestroyer) entity;
            }
        }
        return null;
    }

    public static List<EntityEinherjar> einherjar(World world) {
        List<EntityEinherjar> list = new ArrayList<>();
        for (Entity entity : world.loadedEntityList) {
            if (entity instanceof EntityEinherjar && !entity.isDead) {
                list.add((EntityEinherjar) entity);
            }
        }
        return list;
    }

    public void wake(EntityDestroyer destroyer) {
        if (destroyer.getState() != EntityDestroyer.State.DORMANT) {
            return;
        }
        destroyer.wake();
        tell(destroyer.world, "At the far end of the vault, behind the Destroyer's face, a fire kindles.");
    }

    /** It has fallen. Odin's smiths will have it standing in its niche again in a while. */
    public void destroyerFell(EntityDestroyer destroyer) {
        VaultData data = VaultData.get();
        data.destroyerBack = DimensionManager.getWorld(0).getTotalWorldTime() + EternityConfig.vault.destroyerRepairMinutes * 1200L;
        data.markDirty();
        tell(destroyer.world, "The Destroyer has fallen. The fire in it is out, and its back has split open: there is room inside.");
    }

    /**
     * Someone lays hands on the fallen Destroyer, and something in it answers: it rises, empty, its
     * back open, the Destroyer Armor now. Odin's smiths make the vault another in its place.
     */
    public void claim(EntityDestroyer wreck, EntityPlayer player) {
        EntityDestroyerArmor armor = new EntityDestroyerArmor(wreck.world);
        armor.rise(wreck.posX, wreck.posY, wreck.posZ, wreck.rotationYaw, armor.getMaxHealth() * (float) EternityConfig.vault.armorClaimedIntegrity);
        // Beaten as it was, it is barely holding together, and the fire in it all but out.
        armor.setLava(EntityDestroyerArmor.BUCKET);
        wreck.world.spawnEntity(armor);
        wreck.setDead();
        wreck.world.playSound(null, wreck.posX, wreck.posY + 1D, wreck.posZ, SoundEvents.BLOCK_PISTON_EXTEND, SoundCategory.HOSTILE, 2F, 0.3F);
        TextComponentString text = new TextComponentString("Under your hand something in the Destroyer answers. It rises, empty, its back open to you. "
                + "It will go where you take it, but it is barely holding together and its fire is all but out: it wants Uru and lava. "
                + "Sneak and use it, or press your inventory key inside it, to see to its systems.");
        text.getStyle().setColor(TextFormatting.GOLD).setItalic(true);
        player.sendMessage(text);
    }

    public void setCalm(World world, boolean calm) {
        this.calm = calm;
        EntityDestroyer destroyer = destroyer(world);
        if (destroyer != null) {
            destroyer.setCalm(calm);
        }
        for (EntityEinherjar guard : einherjar(world)) {
            guard.setCalm(calm);
        }
    }

    public boolean isCalm() {
        return calm;
    }

    public int alarm() {
        return alarm;
    }

    /** Puts everything back: the Casket on its pedestal, Mjolnir and the Tesseract on their stands if they are gone, the Destroyer mended. */
    public void restore(World world) {
        VaultData data = VaultData.get();
        data.casketTaken = false;
        data.casketTakenBy = "";
        data.destroyerBack = 0L;
        data.mjolnirSet = !world.getEntitiesWithinAABB(EntityThorWeapon.class,
                new net.minecraft.util.math.AxisAlignedBB(new BlockPos(OdinsVault.Relic.MJOLNIR.base())).grow(2D)).isEmpty();
        data.tesseractSet = !world.getEntitiesWithinAABB(EntityTesseract.class,
                new net.minecraft.util.math.AxisAlignedBB(new BlockPos(OdinsVault.Relic.TESSERACT.base())).grow(2D)).isEmpty();
        data.markDirty();
        EntityDestroyer destroyer = destroyer(world);
        if (destroyer != null) {
            destroyer.setDead();
        }
        EternityNetwork.sendToDimension(state(), OdinsVault.dimension());
    }

    private static void tell(World world, String line) {
        TextComponentString text = new TextComponentString(line);
        text.getStyle().setColor(TextFormatting.GRAY).setItalic(true);
        for (EntityPlayer player : world.playerEntities) {
            player.sendMessage(text);
        }
    }

    private static MsgTribunalFx state() {
        return new MsgTribunalFx(MsgTribunalFx.VAULT_STATE, 0D, 0D, 0D, 0D, 0D, 0D, VaultData.get().casketTaken ? 0 : 1, 0);
    }

    private static void send(EntityPlayerMP player) {
        EternityNetwork.send(state(), player);
    }

    // ------------------------------------------------------------------ the Casket's pedestal

    /** Lift the Casket off its pedestal, or set it back. Lifting it wakes the Destroyer at once. */
    @SubscribeEvent
    public void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        World world = event.getWorld();
        if (world.provider.getDimension() != OdinsVault.dimension()) {
            return;
        }
        BlockPos pos = event.getPos();
        Vec3d c = OdinsVault.Relic.CASKET.base();
        if (pos.getX() != MathHelper.floor(c.x) || pos.getZ() != MathHelper.floor(c.z) || pos.getY() != OdinsVault.STAND) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(EnumActionResult.SUCCESS);
        if (world.isRemote || event.getHand() != EnumHand.MAIN_HAND) {
            return;
        }
        EntityPlayer player = event.getEntityPlayer();
        VaultData data = VaultData.get();
        ItemStack held = player.getHeldItemMainhand();
        if (!data.casketTaken) {
            ItemStack casket = new ItemStack(EternityItems.CASKET);
            if (!player.inventory.addItemStackToInventory(casket)) {
                player.dropItem(casket, false);
            }
            data.casketTaken = true;
            data.casketTakenBy = player.getName();
            data.markDirty();
            world.playSound(null, c.x, c.y + 1D, c.z, SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.BLOCKS, 1F, 0.5F);
            world.playSound(null, c.x, c.y + 1D, c.z, SoundEvents.ENTITY_PLAYER_HURT_ON_FIRE, SoundCategory.BLOCKS, 0.3F, 0.4F);
            EntityDestroyer destroyer = destroyer(world);
            if (destroyer != null) {
                wake(destroyer);
            }
        } else if (held.getItem() == EternityItems.CASKET) {
            held.shrink(1);
            data.casketTaken = false;
            data.casketTakenBy = "";
            data.markDirty();
            world.playSound(null, c.x, c.y + 1D, c.z, SoundEvents.BLOCK_GLASS_PLACE, SoundCategory.BLOCKS, 1F, 0.6F);
        } else {
            return;
        }
        EternityNetwork.sendToDimension(state(), OdinsVault.dimension());
    }

    // ------------------------------------------------------------------ nothing built, nothing broken

    @SubscribeEvent
    public void onBreak(BlockEvent.BreakEvent event) {
        if (event.getWorld().provider.getDimension() == OdinsVault.dimension() && !event.getPlayer().isCreative()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onPlace(BlockEvent.PlaceEvent event) {
        if (event.getWorld().provider.getDimension() == OdinsVault.dimension() && !event.getPlayer().isCreative()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onExplode(ExplosionEvent.Detonate event) {
        if (event.getWorld().provider.getDimension() == OdinsVault.dimension()) {
            event.getAffectedBlocks().clear();
        }
    }

    // ------------------------------------------------------------------ what can hurt the Destroyer

    /**
     * Whether a blow comes from something worthy of the Destroyer: the Power Stone in either hand,
     * on its own or in a gauntlet; Mjolnir or Stormbreaker, held or thrown; a charged Astral
     * Regulator; or the fists of the Destroyer Armor, its own body turned against it.
     */
    public static boolean worthy(DamageSource source) {
        if (source.getImmediateSource() instanceof EntityThrownThorWeapon || source.getImmediateSource() instanceof EntityDestroyerArmor) {
            return true;
        }
        Entity attacker = source.getTrueSource();
        if (!(attacker instanceof EntityLivingBase)) {
            return false;
        }
        EntityLivingBase living = (EntityLivingBase) attacker;
        if (InfinityStones.wieldsPower(living) || ItemAstralRegulator.holdsCharged(living)) {
            return true;
        }
        for (ItemStack stack : living.getHeldEquipment()) {
            Item item = stack.getItem();
            if (item == HEItems.MJOLNIR || item == HEItems.ULTIMATE_MJOLNIR || item == HEItems.STORMBREAKER) {
                return true;
            }
        }
        return false;
    }
}
