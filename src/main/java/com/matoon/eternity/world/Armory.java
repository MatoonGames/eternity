package com.matoon.eternity.world;

import com.matoon.eternity.entity.EntityDestroyerArmor;
import com.matoon.eternity.item.ItemEinherjarShield;
import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.network.MsgTribunalFx;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.play.server.SPacketSetPassengers;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
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
import java.util.WeakHashMap;

/**
 * What the vault's arms do once they are out of it. The Einherjar's shield throws back whoever
 * strikes it from close in. The Destroyer Armor keeps whoever is inside it: blows meant for them
 * land on the armour, sneaking does not tip them out of it (only its own way out does), and it
 * comes with them to any other dimension.
 */
public final class Armory {

    public static final Armory INSTANCE = new Armory();

    /** Pilots carried to another dimension, to be put back into their armour once both have arrived. */
    private final List<Object[]> arriving = new ArrayList<>();
    /** Armour lifted out of a world with its pilot, until they arrive in the next. */
    private final Map<UUID, NBTTagCompound> carried = new HashMap<>();
    /** When each attacker was last thrown off a shield, so one blow throws them only once. */
    private final Map<Entity, Long> thrown = new WeakHashMap<>();

    private Armory() {
    }

    // ------------------------------------------------------------------ the Destroyer Armor

    /**
     * Whoever is inside is untouchable: what would have struck them strikes the armour. Late, so that whatever spares
     * them (the Star Chamber sparing its defendant, say) spares the armour too.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onAttack(LivingAttackEvent event) {
        EntityLivingBase victim = event.getEntityLiving();
        if (victim.world.isRemote) {
            return;
        }
        if (victim.getRidingEntity() instanceof EntityDestroyerArmor) {
            EntityDestroyerArmor armor = (EntityDestroyerArmor) victim.getRidingEntity();
            DamageSource source = event.getSource();
            if (armor.hidesPilot() && source != DamageSource.OUT_OF_WORLD && source != DamageSource.STARVE) {
                event.setCanceled(true);
                // A blast reaches the armour itself, all of it; only what is aimed at them is passed on to it.
                if (!source.isExplosion()) {
                    armor.attackEntityFrom(source, event.getAmount());
                }
            }
            return;
        }
        shieldThrows(victim, event.getSource());
    }

    /**
     * What its pilot looses (a stone's energy blast, an arrow, a fireball) leaves from inside it, and passes through it
     * rather than bursting on it.
     */
    @SubscribeEvent
    public void onProjectile(ProjectileImpactEvent event) {
        RayTraceResult hit = event.getRayTraceResult();
        if (hit == null || hit.entityHit == null) {
            return;
        }
        Entity projectile = event.getEntity();
        Entity shooter = projectile instanceof EntityThrowable ? ((EntityThrowable) projectile).getThrower()
                : projectile instanceof EntityArrow ? ((EntityArrow) projectile).shootingEntity
                : projectile instanceof EntityFireball ? ((EntityFireball) projectile).shootingEntity : null;
        EntityDestroyerArmor armor = EntityDestroyerArmor.carrier(shooter);
        if (armor != null && (hit.entityHit == armor || hit.entityHit == shooter)) {
            event.setCanceled(true);
        }
    }

    /** Sneaking does not tip anyone out of it; only its own way out does, or its going down. */
    @SubscribeEvent
    public void onMount(EntityMountEvent event) {
        if (event.isDismounting() && !event.getWorldObj().isRemote && event.getEntityBeingMounted() instanceof EntityDestroyerArmor
                && event.getEntityMounting().isSneaking() && ((EntityDestroyerArmor) event.getEntityBeingMounted()).holds(event.getEntityMounting())) {
            event.setCanceled(true);
        }
    }

    /**
     * Anyone inside the armour who goes to another dimension, however they go (the vault's doors, a command,
     * another mod's teleport), takes it with them: it is lifted out of this world as they leave, and set
     * down around them on the other side, and they are back inside it.
     */
    @SubscribeEvent
    public void onTravel(EntityTravelToDimensionEvent event) {
        if (!(event.getEntity() instanceof EntityPlayerMP) || !(event.getEntity().getRidingEntity() instanceof EntityDestroyerArmor)) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.getEntity();
        EntityDestroyerArmor armor = (EntityDestroyerArmor) player.getRidingEntity();
        if (armor.isDead || !armor.isManned()) {
            return;
        }
        NBTTagCompound tag = armor.writeToNBT(new NBTTagCompound());
        tag.removeTag("UUIDMost");
        tag.removeTag("UUIDLeast");
        armor.release(player);
        armor.setDead();
        carried.put(player.getUniqueID(), tag);
    }

    @SubscribeEvent
    public void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        NBTTagCompound tag = carried.remove(event.player.getUniqueID());
        if (tag == null || !(event.player instanceof EntityPlayerMP)) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        WorldServer world = player.getServerWorld();
        world.getChunk(player.getPosition());
        EntityDestroyerArmor armor = new EntityDestroyerArmor(world);
        armor.readFromNBT(tag);
        // Its saved self still says it is in the world it left; everything it sends would go back there.
        armor.dimension = world.provider.getDimension();
        armor.setLocationAndAngles(player.posX, player.posY, player.posZ, player.rotationYaw, 0F);
        armor.rotationYawHead = armor.renderYawOffset = player.rotationYaw;
        world.spawnEntity(armor);
        arriving.add(new Object[]{player, armor, 0});
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        // Vanilla kicks anyone whose vehicle hangs in the air for four seconds, on servers that do not allow flight.
        // The armour glides and flies by right; keep its pilots off that count.
        for (EntityPlayerMP player : FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayers()) {
            if (player.getRidingEntity() instanceof EntityDestroyerArmor && player.connection != null) {
                try {
                    ObfuscationReflectionHelper.setPrivateValue(NetHandlerPlayServer.class, player.connection, 0, "field_184346_E");
                } catch (RuntimeException ignored) {
                    // Nothing worse than vanilla's own rule.
                }
            }
        }
        if (arriving.isEmpty()) {
            return;
        }
        Iterator<Object[]> it = arriving.iterator();
        while (it.hasNext()) {
            Object[] a = it.next();
            EntityPlayerMP player = (EntityPlayerMP) a[0];
            EntityDestroyerArmor armor = (EntityDestroyerArmor) a[1];
            int waited = (Integer) a[2] + 1;
            a[2] = waited;
            if (player.isDead || armor.isDead || player.connection == null || player.world != armor.world) {
                it.remove();
                continue;
            }
            // Only once their client knows the armour is there: seated before that, it never hears of it, and they
            // could neither steer it nor fire it.
            boolean seen = ((WorldServer) armor.world).getEntityTracker().getTrackingPlayers(armor).contains(player);
            if (seen && waited >= 2 || waited >= 100) {
                player.startRiding(armor, true);
                player.connection.sendPacket(new SPacketSetPassengers(armor));
                it.remove();
            }
        }
    }

    // ------------------------------------------------------------------ the Einherjar's shield

    /** Strike an Einherjar's shield from close in while it is raised, and you are thrown back off it. */
    private void shieldThrows(EntityLivingBase victim, DamageSource source) {
        if (!victim.isActiveItemStackBlocking() || !(victim.getActiveItemStack().getItem() instanceof ItemEinherjarShield) || source.isUnblockable()) {
            return;
        }
        Entity direct = source.getImmediateSource();
        if (!(direct instanceof EntityLivingBase) || direct.getDistance(victim) > 3.5F) {
            return;
        }
        Vec3d look = victim.getLook(1F);
        Vec3d to = new Vec3d(direct.posX - victim.posX, 0D, direct.posZ - victim.posZ).normalize();
        if (to.dotProduct(new Vec3d(look.x, 0D, look.z).normalize()) < 0.2D) {
            return;
        }
        long now = victim.world.getTotalWorldTime();
        Long last = thrown.get(direct);
        if (last != null && now - last < 5L) {
            return;
        }
        thrown.put(direct, now);
        ((EntityLivingBase) direct).knockBack(victim, 0.9F, -to.x, -to.z);
        direct.motionY = Math.max(direct.motionY, 0.25D);
        direct.velocityChanged = true;
        Vec3d at = victim.getPositionVector().add(to.scale(0.6D)).add(0D, 1.2D, 0D);
        EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.EINHERJAR_BLOCK, at.x, at.y, at.z, to.x, 0D, to.z, victim.getEntityId(), 0),
                victim.dimension);
        victim.world.playSound(null, victim.posX, victim.posY + 1D, victim.posZ, SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 1F, 0.7F);
    }
}
