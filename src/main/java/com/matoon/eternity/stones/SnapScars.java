package com.matoon.eternity.stones;

import anvil.infinity.snap.SnapResult;
import com.matoon.eternity.EternityConfig;
import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.stones.network.MsgScars;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumHandSide;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The burns snapping leaves, one for each arm, kept with the player through death and away, and deepening with every
 * snap made with that hand: a little for most, far more for destroying the stones. A burn heals by itself, slowly,
 * over days. Badly burnt, they are weakened and slowed; burnt to the bone, which destroying the stones does at a stroke,
 * they are crippled, the gauntlet's metal run into the flesh of their arm.
 * <p>
 * The burn is a number: 1 is a fresh scald, {@link #FUSED} the metal fused in, {@link #MAX} as far as it goes.
 */
public final class SnapScars {

    public static final SnapScars INSTANCE = new SnapScars();

    /** At this, the arm is burnt through, and the gauntlet's metal has run into it. */
    public static final float FUSED = 7F;
    public static final float MAX = 12F;

    private static final String RIGHT = "EternityScarRight", LEFT = "EternityScarLeft";
    private static final UUID CRIPPLED = UUID.fromString("3c4f8a21-7d0e-4b5a-9e63-1f2a8b7c5d90");
    /** Each burn as last sent, so healing is only sent when it shows. */
    private static final Map<UUID, Float> SENT = new HashMap<>();

    private SnapScars() {
    }

    /** What a snap costs the arm that makes it, by what it does. */
    static float cost(SnapResult mode) {
        float cost;
        switch (mode) {
            case DESTROYSTONES:
                cost = FUSED + 0.5F;
                break;
            case RECREATE:
                cost = 2F;
                break;
            case KILLHALF:
                cost = 1.5F;
                break;
            case CREATIVE:
                cost = 0.5F;
                break;
            default:
                cost = 1F;
                break;
        }
        return cost * (float) EternityConfig.stones.scarSeverity;
    }

    public static float get(EntityPlayer player, EnumHandSide side) {
        return data(player).getFloat(side == EnumHandSide.RIGHT ? RIGHT : LEFT);
    }

    /** The persistent tag Forge carries over death. */
    private static NBTTagCompound data(EntityPlayer player) {
        NBTTagCompound entity = player.getEntityData();
        if (!entity.hasKey(EntityPlayer.PERSISTED_NBT_TAG)) {
            entity.setTag(EntityPlayer.PERSISTED_NBT_TAG, new NBTTagCompound());
        }
        return entity.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
    }

    private static void set(EntityPlayer player, EnumHandSide side, float burn) {
        data(player).setFloat(side == EnumHandSide.RIGHT ? RIGHT : LEFT, Math.max(0F, Math.min(MAX, burn)));
    }

    /** A snap made with the hand on {@code side}, burning it deeper, the burn flaring for everyone to see. */
    static void burn(EntityPlayerMP player, EnumHandSide side, float amount) {
        set(player, side, get(player, side) + amount);
        cripple(player);
        send(player, side == EnumHandSide.RIGHT ? MsgScars.FLARE_RIGHT : MsgScars.FLARE_LEFT, null);
    }

    /** Every burn but this player's healed: the universe made new. */
    static void healAllBut(MinecraftServer server, EntityPlayerMP spared) {
        for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
            if (player != spared && (get(player, EnumHandSide.RIGHT) > 0F || get(player, EnumHandSide.LEFT) > 0F)) {
                set(player, EnumHandSide.RIGHT, 0F);
                set(player, EnumHandSide.LEFT, 0F);
                cripple(player);
                send(player, MsgScars.NO_FLARE, null);
            }
        }
    }

    private static void send(EntityPlayerMP player, int flare, EntityPlayerMP only) {
        float right = get(player, EnumHandSide.RIGHT), left = get(player, EnumHandSide.LEFT);
        MsgScars message = new MsgScars(player.getUniqueID(), right, left, flare);
        if (only != null) {
            EternityNetwork.send(message, only);
        } else {
            EternityNetwork.sendToAll(message);
            SENT.put(player.getUniqueID(), right + left);
        }
    }

    /** Healing, a little at a time; the burn's effects kept up while it lasts. */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof EntityPlayerMP) || event.player.ticksExisted % 100 != 0) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        float right = get(player, EnumHandSide.RIGHT), left = get(player, EnumHandSide.LEFT);
        if (right <= 0F && left <= 0F) {
            return;
        }
        // Healing is slowest where it is deepest: the metal comes out of the flesh last of all.
        float heal = (float) EternityConfig.stones.scarHealPerDay * 100F / 24000F;
        set(player, EnumHandSide.RIGHT, right - heal * (right >= FUSED ? 0.5F : 1F));
        set(player, EnumHandSide.LEFT, left - heal * (left >= FUSED ? 0.5F : 1F));
        cripple(player);
        Float sent = SENT.get(player.getUniqueID());
        float now = get(player, EnumHandSide.RIGHT) + get(player, EnumHandSide.LEFT);
        if (sent == null || Math.abs(sent - now) >= 0.25F || now <= 0F) {
            send(player, MsgScars.NO_FLARE, null);
        }
    }

    /**
     * Weakened past a burn of 2.5; slowed and clumsy past 5; past {@link #FUSED}, crippled: weaker and slower still,
     * and with less of themselves, a third of their health gone, half past 10.
     */
    private static void cripple(EntityPlayerMP player) {
        float burn = Math.max(get(player, EnumHandSide.RIGHT), get(player, EnumHandSide.LEFT));
        IAttributeInstance health = player.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
        AttributeModifier lost = health.getModifier(CRIPPLED);
        double share = !EternityConfig.stones.scarsCripple || burn < FUSED ? 0D : burn >= 10F ? -0.5D : -0.33D;
        if (lost != null && lost.getAmount() != share) {
            health.removeModifier(lost);
            lost = null;
        }
        if (lost == null && share != 0D) {
            health.applyModifier(new AttributeModifier(CRIPPLED, "Burnt by the snap", share, 2).setSaved(false));
        }
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
        if (!EternityConfig.stones.scarsCripple || burn < 2.5F) {
            return;
        }
        boolean crippled = burn >= FUSED;
        effect(player, MobEffects.WEAKNESS, crippled ? 2 : burn >= 5F ? 1 : 0);
        if (burn >= 5F) {
            effect(player, MobEffects.SLOWNESS, crippled ? 1 : 0);
            effect(player, MobEffects.MINING_FATIGUE, crippled ? 1 : 0);
        }
    }

    private static void effect(EntityPlayerMP player, Potion potion, int amplifier) {
        player.addPotionEffect(new PotionEffect(potion, 140, amplifier, true, false));
    }

    /** Someone arriving sees every burn, and everyone sees theirs. */
    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.player instanceof EntityPlayerMP)) {
            return;
        }
        EntityPlayerMP joined = (EntityPlayerMP) event.player;
        MinecraftServer server = joined.getServer();
        if (server == null) {
            return;
        }
        for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
            if (player != joined && (get(player, EnumHandSide.RIGHT) > 0F || get(player, EnumHandSide.LEFT) > 0F)) {
                send(player, MsgScars.NO_FLARE, joined);
            }
        }
        if (get(joined, EnumHandSide.RIGHT) > 0F || get(joined, EnumHandSide.LEFT) > 0F) {
            cripple(joined);
            send(joined, MsgScars.NO_FLARE, null);
        }
    }

    /** The crippling's lost health goes with the body it was on; the new one is crippled again. */
    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            cripple((EntityPlayerMP) event.player);
            send((EntityPlayerMP) event.player, MsgScars.NO_FLARE, null);
        }
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SENT.remove(event.player.getUniqueID());
    }

    public static void reset() {
        SENT.clear();
    }
}
