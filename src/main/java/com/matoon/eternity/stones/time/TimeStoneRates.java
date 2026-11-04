package com.matoon.eternity.stones.time;

import com.matoon.eternity.compat.Tickrate;
import lucraft.mods.lucraftcore.superpowers.abilities.Ability;
import me.guichaguri.tickratechanger.api.TickrateAPI;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * How fast time runs in each dimension the Time Stone has bent.
 * <p>
 * Tickrate Changer's server rate is one for the whole server, so it is left alone: the Time Stone only sets the rate
 * of the clients of players in the dimension it was used in. Everyone elsewhere keeps their own dimension's rate. When
 * the last Time Stone holder leaves a dimension, or loses the stone, its time flows normally again.
 */
public final class TimeStoneRates {

    /** Dimension id to its bent rate, only while it is bent. */
    private static final Map<Integer, Float> RATES = new HashMap<>();

    private TimeStoneRates() {
    }

    /** The natural flow of time: normally 20, unless Eternity himself is bending it for everyone (see {@link Tickrate}). */
    public static float normal() {
        float rate = Tickrate.current();
        return rate > 0F ? rate : Tickrate.NORMAL;
    }

    public static float getRate(int dimension) {
        Float rate = RATES.get(dimension);
        return rate != null ? rate : normal();
    }

    /** Bends a dimension's time to a rate, for everyone in it, and tells whoever did it. */
    public static void applyRate(EntityPlayerMP actor, int dimension, float rate) {
        RATES.put(dimension, rate);
        broadcast(dimension, rate);
        actionBar(actor, rate);
    }

    /** Lets a dimension's time flow normally again, and tells whoever did it. */
    public static void resetRate(EntityPlayerMP actor, int dimension) {
        RATES.remove(dimension);
        broadcast(dimension, normal());
        actionBar(actor, normal());
    }

    /** A Time Stone holder has left a dimension (or the game): if nobody else there holds it, its time flows normally. */
    public static void onUserLeftDimension(EntityPlayerMP leaving, int dimension) {
        if (RATES.containsKey(dimension) && !othersHoldTime(dimension, leaving)) {
            RATES.remove(dimension);
            broadcast(dimension, normal());
        }
    }

    public static void onAbilityDeactivated(EntityPlayerMP player, int dimension) {
        onUserLeftDimension(player, dimension);
    }

    public static void reset() {
        RATES.clear();
    }

    private static void broadcast(int dimension, float rate) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) {
            return;
        }
        for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
            TickrateAPI.changeClientTickrate(player, player.dimension == dimension ? rate : getRate(player.dimension));
        }
    }

    private static void actionBar(EntityPlayerMP player, float rate) {
        float normal = normal();
        String text;
        if (rate == normal) {
            text = TextFormatting.GREEN + "Time Speed: Normal";
        } else {
            text = (rate < normal ? TextFormatting.AQUA : TextFormatting.YELLOW) + "Time Speed: " + format(rate) + "x";
        }
        player.sendStatusMessage(new TextComponentString(text), true);
    }

    private static String format(float rate) {
        return rate == Math.floor(rate) ? String.valueOf((int) rate) : String.format("%.1f", rate);
    }

    private static boolean othersHoldTime(int dimension, EntityPlayer exclude) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) {
            return false;
        }
        for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
            if (player != exclude && player.dimension == dimension
                    && (Ability.hasAbility(player, AbilitySlowTime.class) || Ability.hasAbility(player, AbilitySpeedTime.class))) {
                return true;
            }
        }
        return false;
    }
}
