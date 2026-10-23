package com.matoon.eternity.world;

import com.matoon.eternity.entity.EternityDamage;
import lucraft.mods.lucraftcore.infinity.EntityItemIndestructible;
import lucraft.mods.lucraftcore.infinity.items.ItemIndestructible;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IProjectile;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;

/**
 * The rules of the Nexus of All Realities. Falls hurt far less here, but fall all the way
 * down into the abyss beneath it and you do not come back. And the vortex above the hub
 * leads back to wherever you came from.
 */
public final class NexusDirector {

    public static final NexusDirector INSTANCE = new NexusDirector();

    private static final String RETURN = "eternityNexusReturn";
    private static final String SINKING = "eternityAbyssSinking";
    /** However hard they struggle, the abyss has them within twenty seconds. */
    private static final int MAX_SINKING = 400;

    private NexusDirector() {
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof EntityPlayerMP)) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        if (player.dimension != Nexus.dimension()) {
            // Remember where they stand in the real worlds, so the vortex knows where to send them back.
            if (player.ticksExisted % 40 == 0) {
                NBTTagCompound tag = new NBTTagCompound();
                tag.setInteger("dim", player.dimension);
                tag.setDouble("x", player.posX);
                tag.setDouble("y", player.posY);
                tag.setDouble("z", player.posZ);
                player.getEntityData().setTag(RETURN, tag);
            }
            return;
        }
        stillness(player);
        double dx = player.posX - Nexus.VORTEX_X;
        double dy = player.posY + 1D - Nexus.VORTEX_Y;
        double dz = player.posZ - Nexus.VORTEX_Z;
        if (dx * dx + dy * dy < Nexus.VORTEX_RADIUS * Nexus.VORTEX_RADIUS * 0.85D && Math.abs(dz) < 1.5D) {
            leave(player);
        }
    }

    /**
     * The abyss beneath everything. It is quicksand: the living sink into it slowly, held
     * by the hands, and when they have gone under they are gone. Whatever they were holding
     * is lost with them; only the indestructible (the stones, the Regulator) are given back,
     * at the Nexus's heart. Beings like Eternity and the Tribunal are beyond its reach.
     *
     * Players move themselves, so their own client drags them down (see NexusClient); here
     * we only keep count, and take them once they are under, or once they have struggled
     * too long.
     */
    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote || event.world.provider.getDimension() != Nexus.dimension()) {
            return;
        }
        // Copied: deaths drop items into the world while we look.
        for (Entity entity : new ArrayList<>(event.world.loadedEntityList)) {
            if (entity.isDead) {
                continue;
            }
            if (entity.posY >= Nexus.ABYSS_Y) {
                if (entity.posY > Nexus.ABYSS_Y + 1.5D) {
                    // Got out somehow, by a pearl or a command. It lets go.
                    entity.getEntityData().removeTag(SINKING);
                }
                continue;
            }
            if (entity instanceof EntityLivingBase) {
                EntityLivingBase living = (EntityLivingBase) entity;
                if (living.getHealth() > 0F && living.isNonBoss() && !(living instanceof EntityPlayer && ((EntityPlayer) living).isSpectator())) {
                    sink(living);
                }
            } else if (entity instanceof EntityItemIndestructible
                    || entity instanceof EntityItem && ((EntityItem) entity).getItem().getItem() instanceof ItemIndestructible) {
                entity.motionX = entity.motionY = entity.motionZ = 0D;
                entity.setPositionAndUpdate(Nexus.HUB_X, Nexus.HUB_Y + 1D, Nexus.HUB_Z);
            } else if (entity instanceof EntityItem || entity instanceof EntityXPOrb || entity instanceof IProjectile
                    || entity instanceof EntityFallingBlock) {
                entity.setDead();
            }
        }
    }

    /** One more tick in the hands' grip. */
    private static void sink(EntityLivingBase living) {
        int ticks = living.getEntityData().getInteger(SINKING) + 1;
        living.getEntityData().setInteger(SINKING, ticks);
        living.fallDistance = 0F;
        float sunk = Nexus.sunk(living);
        if (!(living instanceof EntityPlayer)) {
            if (ticks == 1 && living.posY < Nexus.ABYSS_Y - 0.3D) {
                living.setPositionAndUpdate(living.posX, Nexus.ABYSS_Y - 0.3D, living.posZ);
            }
            living.motionX = living.motionZ = 0D;
            living.motionY = -Nexus.sinkSpeed(sunk);
            living.velocityChanged = true;
        }
        if (sunk >= 1F || ticks > MAX_SINKING) {
            living.getEntityData().removeTag(SINKING);
            devour(living);
        }
    }

    /** No armour, no totem, no creative mode and no Regulator saves anyone from it. */
    private static void devour(EntityLivingBase living) {
        DamageSource source = EternityDamage.abyss();
        living.attackEntityFrom(source, 1.0E9F);
        if (living.getHealth() > 0F) {
            living.setHealth(0F);
            living.onDeath(source);
        }
    }

    /** Sends someone into the Nexus, at its heart. */
    public void enter(EntityPlayerMP player) {
        Teleport.player(player, Nexus.dimension(), Nexus.HUB_X, Nexus.HUB_Y, Nexus.HUB_Z, false);
    }

    /** Back through the vortex to wherever they came from, or the overworld's spawn if nobody knows. */
    public void leave(EntityPlayerMP player) {
        NBTTagCompound tag = player.getEntityData().getCompoundTag(RETURN);
        if (tag.hasKey("dim") && DimensionManager.isDimensionRegistered(tag.getInteger("dim")) && tag.getInteger("dim") != Nexus.dimension()) {
            Teleport.player(player, tag.getInteger("dim"), tag.getDouble("x"), tag.getDouble("y"), tag.getDouble("z"), true);
            return;
        }
        WorldServer overworld = DimensionManager.getWorld(0);
        BlockPos spawn = overworld != null ? overworld.getSpawnPoint() : new BlockPos(0, 80, 0);
        Teleport.player(player, 0, spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D, true);
    }


    // ------------------------------------------------------------------ the epicentre

    private static final String STILL_FOOD = "eternityStillFood";

    /**
     * At the epicentre of absolute neutrality there is no before and no after. For anyone
     * standing there, nothing changes: hunger does not grow, no effect wears off, nothing
     * burns, no breath runs short, and nothing falls. What you carry in, you carry out.
     * (Harm still comes; it is not a change in you but something done to you.)
     */
    private static void stillness(EntityPlayerMP player) {
        boolean still = Nexus.inEpicentre(player.posX, player.posY, player.posZ) && !player.isSpectator();
        NBTTagCompound data = player.getEntityData();
        if (!still) {
            data.removeTag(STILL_FOOD);
            return;
        }
        if (!data.hasKey(STILL_FOOD)) {
            NBTTagCompound food = new NBTTagCompound();
            player.getFoodStats().writeNBT(food);
            data.setTag(STILL_FOOD, food);
        } else if (player.ticksExisted % 10 == 0) {
            player.getFoodStats().readNBT(data.getCompoundTag(STILL_FOOD));
        }
        for (PotionEffect effect : new ArrayList<>(player.getActivePotionEffects())) {
            if (effect.getDuration() > 1 && effect.getDuration() < 1000000) {
                player.addPotionEffect(new PotionEffect(effect.getPotion(), effect.getDuration() + 1, effect.getAmplifier(),
                        effect.getIsAmbient(), effect.doesShowParticles()));
            }
        }
        player.extinguish();
        player.setAir(300);
        player.fallDistance = 0F;
    }

    /** Nobody heals at the epicentre: healing is a change, and nothing changes there. */
    @SubscribeEvent
    public void onHeal(LivingHealEvent event) {
        EntityLivingBase living = event.getEntityLiving();
        if (living instanceof EntityPlayer && living.dimension == Nexus.dimension() && Nexus.inEpicentre(living.posX, living.posY, living.posZ)) {
            event.setCanceled(true);
        }
    }
    /** Gravity is half-hearted here; so are falls. */
    @SubscribeEvent
    public void onFall(LivingFallEvent event) {
        if (event.getEntityLiving() instanceof EntityPlayer && event.getEntityLiving().dimension == Nexus.dimension()) {
            event.setDistance(event.getDistance() * 0.3F);
        }
    }
}
