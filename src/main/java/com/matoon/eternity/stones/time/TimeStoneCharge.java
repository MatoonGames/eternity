package com.matoon.eternity.stones.time;

import com.matoon.eternity.EternityConfig;
import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.stones.network.MsgTimeStoneCharge;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The Time Stone's charge, from 0 (spent) to 1 (full), when {@link EternityConfig.StonesSettings#timeStoneCharge} is
 * on. It drains while its holder's dimension runs bent and refills once time flows normally. Spent, it puts time back
 * at once, and slowing or speeding it stays locked until the charge is full again.
 * <p>
 * Slowed time drains it by sqrt(20 / rate): rate 1 about 4.5 times as fast as rate 15. Sped-up time drains it in
 * proportion to how far it is sped.
 */
public final class TimeStoneCharge {

    private static final Map<UUID, Float> CHARGES = new HashMap<>();
    private static final Map<UUID, Boolean> DEPLETED = new HashMap<>();
    private static final Map<UUID, Integer> SINCE_SYNC = new HashMap<>();

    /** From the natural 20 to the fastest the stone allows, 100. */
    private static final float MAX_DEVIATION = 80F;
    private static final int SYNC_INTERVAL = 4;

    // The holder's own charge, as last synced, for their ability bar.
    private static float clientCharge = 1F;

    private TimeStoneCharge() {
    }

    public static boolean active() {
        return EternityConfig.stones.timeStoneCharge;
    }

    /** Each server tick, for each player with the Time Stone's abilities. */
    public static void tickPlayer(EntityPlayerMP player) {
        if (!active()) {
            return;
        }
        UUID id = player.getUniqueID();
        float charge = CHARGES.getOrDefault(id, 1F);
        boolean wasDepleted = DEPLETED.getOrDefault(id, false);
        float rate = TimeStoneRates.getRate(player.dimension), delta = rate - TimeStoneRates.normal();
        float recharge = EternityConfig.stones.timeStoneRechargeSeconds * 20F;

        if (delta != 0F) {
            float factor = delta < 0F
                    ? (float) (Math.sqrt(20F / rate) * EternityConfig.stones.timeStoneSlowDrain)
                    : (float) (Math.abs(delta) / MAX_DEVIATION * EternityConfig.stones.timeStoneSpeedDrain);
            charge = Math.max(0F, charge - factor / recharge);
            if (charge == 0F && !wasDepleted) {
                DEPLETED.put(id, true);
                CHARGES.put(id, 0F);
                TimeStoneRates.resetRate(player, player.dimension);
                player.world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1F, 0.8F);
                EternityNetwork.send(new MsgTimeStoneCharge(0F, true), player);
                SINCE_SYNC.put(id, 0);
                return;
            }
        } else {
            charge = Math.min(1F, charge + 1F / recharge);
            if (charge >= 1F) {
                DEPLETED.remove(id);
            }
        }
        CHARGES.put(id, charge);

        boolean depleted = DEPLETED.getOrDefault(id, false);
        int ticks = SINCE_SYNC.getOrDefault(id, SYNC_INTERVAL);
        if (ticks >= SYNC_INTERVAL || depleted != wasDepleted) {
            EternityNetwork.send(new MsgTimeStoneCharge(charge, depleted), player);
            SINCE_SYNC.put(id, 0);
        } else {
            SINCE_SYNC.put(id, ticks + 1);
        }
    }

    /** Whether bending time is locked for them, telling them so if it is. */
    public static boolean refuse(EntityPlayerMP player) {
        if (!active() || !DEPLETED.getOrDefault(player.getUniqueID(), false)) {
            return false;
        }
        player.sendStatusMessage(new TextComponentString(TextFormatting.RED + "Time Stone Recharging..."), true);
        player.world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 0.6F, 1.2F);
        return true;
    }

    public static void onPlayerLeft(UUID id) {
        CHARGES.remove(id);
        DEPLETED.remove(id);
        SINCE_SYNC.remove(id);
    }

    public static void reset() {
        CHARGES.clear();
        DEPLETED.clear();
        SINCE_SYNC.clear();
    }

    public static void setClientCharge(float charge) {
        clientCharge = charge;
    }

    public static float clientCharge() {
        return clientCharge;
    }
}
