package com.matoon.eternity.stones;

import anvil.infinity.abilities.AbilitySnap;
import anvil.infinity.helpers.GauntelHelper;
import com.matoon.eternity.compat.StoneAbilities;
import com.matoon.eternity.EternityConfig;
import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.stones.network.MsgPowerUp;
import com.matoon.eternity.stones.network.MsgSnapEffect;
import com.matoon.eternity.stones.time.AbilityResetTime;
import com.matoon.eternity.stones.time.AbilitySlowTime;
import com.matoon.eternity.stones.time.AbilitySpeedTime;
import com.matoon.eternity.stones.time.AbilityTimeUnlocker;
import com.matoon.eternity.stones.time.TimeStoneCharge;
import com.matoon.eternity.stones.time.TimeStoneRates;
import lucraft.mods.lucraftcore.infinity.EnumInfinityStone;
import lucraft.mods.lucraftcore.infinity.items.ItemInfinityGauntlet;
import lucraft.mods.lucraftcore.superpowers.SuperpowerHandler;
import lucraft.mods.lucraftcore.superpowers.abilities.Ability;
import lucraft.mods.lucraftcore.superpowers.abilities.supplier.AbilityContainer;
import lucraft.mods.lucraftcore.superpowers.events.InitAbilitiesEvent;
import lucraft.mods.lucraftcore.util.attributes.LCAttributes;
import lucraft.mods.lucraftcore.util.helper.LCEntityHelper;
import me.guichaguri.tickratechanger.api.TickrateAPI;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHandSide;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * The full gauntlet's power-up, the snap's presentation, the Time Stone's bookkeeping, and the fixes InfinityCraft and
 * Lucraft need for gauntlets put down or changed in hand.
 * <ul>
 * <li><b>Power-up.</b> Completing a gauntlet in hand plays a twenty-second power-up, with the snap locked through it.
 * The gauntlet strains its wielder all the while; anyone without Mjolnir's measure of strength dies of it.</li>
 * <li><b>The snap</b> is heard and seen by everyone near, weakens and slows the one who did it, and marks their
 * hand until they die. Its delayed deaths only land in the dimension it was made in.</li>
 * <li><b>Put down.</b> The Soul and Power Stones' huge health and strength bonuses are Lucraft modifiers that outlive
 * the gauntlet leaving its wielder's hand; they are stripped off, and the Soul Stone's healing stopped, the moment it
 * goes.</li>
 * <li><b>Changed in hand.</b> Lucraft only rebuilds a gauntlet's powers when it is equipped, not when stones are set
 * into or taken out of one already held; it is made to rebuild them then too.</li>
 * </ul>
 */
public class InfinityPatches {

    /**
     * More than anything but the stones lends (they lend up to the largest number there is), so a modifier past it is
     * theirs. The Astral Regulator lends exactly this much health, and is left alone.
     */
    private static final double OVERSIZED = 1024D;

    /** Everyone with all six stones in hand: the power-up plays on the edge into this. */
    private static final Set<UUID> FULL = new HashSet<>();

    /**
     * When each power-up ends. The snap is often only made a few ticks after the sixth stone is set, or replaced by a
     * fresh one, so the lock is re-applied for the time left whenever the snap is found unlocked inside this window.
     */
    private static final Map<UUID, Integer> POWER_UP_END = new HashMap<>();
    private static final Map<UUID, Integer> POWER_UP_TICKS = new HashMap<>();
    private static final Map<UUID, Integer> STRAIN_HITS = new HashMap<>();
    /** Until when a full gauntlet found on someone just logged in is the one they came with, already charged. */
    private static final Map<UUID, Integer> ARRIVED = new HashMap<>();
    /** Until when someone just respawned is looked at every tick, for a gauntlet they came back holding to charge up. */
    private static final Map<UUID, Integer> RESPAWNED = new HashMap<>();
    private static final int ARRIVAL_GRACE = 40;

    /** Each held gauntlet's identity and stones, per hand, to see stones set into one already held. */
    private static final Map<UUID, String[]> MAIN_HAND = new HashMap<>();
    private static final Map<UUID, String[]> OFF_HAND = new HashMap<>();

    private static final Random RANDOM = new Random();

    /** The strong are struck every 25 ticks and live through all of it; the weak, every 20, and die on the fifth. */
    private static final int STRONG_INTERVAL = 25, WEAK_INTERVAL = 20, WEAK_KILL_HITS = 5;
    /** What each blow of strain leaves them with: one heart. */
    private static final float STRAIN_HEALTH = 2F;
    /** The last blow for the weak. Absolute, so no resistance or absorption turns its size into infinity or nonsense. */
    private static final DamageSource STRAIN = new DamageSource("eternity.gauntlet").setDamageBypassesArmor().setDamageIsAbsolute();

    // ------------------------------------------------------------------ joining and leaving

    /** Someone joining with a full gauntlet already in hand has had their power-up. */
    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.player instanceof EntityPlayerMP)) {
            return;
        }
        arrived((EntityPlayerMP) event.player);
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.player instanceof EntityPlayerMP)) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        UUID id = player.getUniqueID();
        if (hasTimeAbility(player)) {
            TimeStoneRates.onUserLeftDimension(player, player.dimension);
        }
        TimeStoneCharge.onPlayerLeft(id);
        // Their next session starts at the natural rate.
        TickrateAPI.changeClientTickrate(player, TimeStoneRates.normal());
        stripHealth(player);
        stripStrength(player);
        forget(id);
    }

    /**
     * Death ends the power-up. All of it is kept by their id, not their body, so left alone it went on into the next
     * life: strain landing on the respawned, the overlay still playing, and death again on the fifth blow.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDeath(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getEntityLiving() instanceof EntityPlayerMP)) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.getEntityLiving();
        if (POWER_UP_END.containsKey(player.getUniqueID())) {
            sendPowerUp(player, false);
        }
        forget(player.getUniqueID());
        // Time they bent goes back to flowing; they come back without the power switched on.
        if (hasTimeAbility(player)) {
            TimeStoneRates.onUserLeftDimension(player, player.dimension);
        }
    }

    /**
     * Back with a full gauntlet still in hand (keepInventory): death took its power, and it is charged up again from
     * the start. Lucraft only learns what a new body holds a tick or so in, so it is looked for every tick until then.
     */
    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.player instanceof EntityPlayerMP)) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        if (POWER_UP_END.containsKey(player.getUniqueID())) {
            sendPowerUp(player, false);
        }
        forget(player.getUniqueID());
        if (GauntelHelper.hasFullGauntlet(player)) {
            updateFull(player);
        } else if (player.getServer() != null) {
            RESPAWNED.put(player.getUniqueID(), player.getServer().getTickCounter() + ARRIVAL_GRACE);
        }
    }

    /**
     * Joining with a full gauntlet in hand is no new completing of it: their power-up was had before they left. Lucraft
     * only learns what a new body holds a tick or so in, and until then no gauntlet is full; so a full one found soon
     * after is the one they came with.
     */
    private static void arrived(EntityPlayerMP player) {
        if (GauntelHelper.hasFullGauntlet(player)) {
            FULL.add(player.getUniqueID());
        } else if (player.getServer() != null) {
            ARRIVED.put(player.getUniqueID(), player.getServer().getTickCounter() + ARRIVAL_GRACE);
        }
    }

    private static void forget(UUID id) {
        FULL.remove(id);
        POWER_UP_END.remove(id);
        POWER_UP_TICKS.remove(id);
        STRAIN_HITS.remove(id);
        ARRIVED.remove(id);
        RESPAWNED.remove(id);
        MAIN_HAND.remove(id);
        OFF_HAND.remove(id);
    }

    public static void reset() {
        FULL.clear();
        POWER_UP_END.clear();
        POWER_UP_TICKS.clear();
        STRAIN_HITS.clear();
        ARRIVED.clear();
        RESPAWNED.clear();
        MAIN_HAND.clear();
        OFF_HAND.clear();
        TimeStoneRates.reset();
        TimeStoneCharge.reset();
    }

    // ------------------------------------------------------------------ the power-up and the snap

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof EntityPlayerMP)) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        UUID id = player.getUniqueID();
        Integer end = POWER_UP_END.get(id);
        if (end != null && (!FULL.contains(id) || end - player.getServer().getTickCounter() <= 0)) {
            POWER_UP_END.remove(id);
            end = null;
        }
        if (end == null) {
            POWER_UP_TICKS.remove(id);
            STRAIN_HITS.remove(id);
            return;
        }
        int remaining = end - player.getServer().getTickCounter();
        AbilitySnap snap;
        try {
            snap = findSnap(player);
        } catch (Exception e) {
            // Lucraft's capability may not be there yet, mid-login or between dimensions.
            snap = null;
        }
        if (snap != null && snap.getCooldown() < remaining - 1) {
            snap.setMaxCooldown(MsgPowerUp.DURATION);
            snap.setCooldown(remaining);
        }
        if (EternityConfig.stones.powerUp && player.isEntityAlive()) {
            strain(player);
        }
    }

    /**
     * A blow of the gauntlet's strain, every so often through the power-up: down to a heart for the strong, and in the
     * end death for the weak. The Soul Stone's health is stripped first so it lands on their real health; Lucraft puts
     * it back next tick.
     */
    private static void strain(EntityPlayerMP player) {
        UUID id = player.getUniqueID();
        int tick = POWER_UP_TICKS.getOrDefault(id, 0) + 1;
        POWER_UP_TICKS.put(id, tick);
        boolean strong = strongEnough(player);
        if (tick % (strong ? STRONG_INTERVAL : WEAK_INTERVAL) != 0) {
            return;
        }
        int hits = STRAIN_HITS.getOrDefault(id, 0) + 1;
        STRAIN_HITS.put(id, hits);
        stripHealth(player);
        if (!strong && hits >= WEAK_KILL_HITS) {
            STRAIN_HITS.remove(id);
            // Past any health they have once the Soul Stone's is stripped, and finite all the way through.
            player.attackEntityFrom(STRAIN, 1.0E30F);
        } else {
            player.setHealth(STRAIN_HEALTH);
        }
    }

    /**
     * A snap that worked, whichever way it was set: heard and seen by everyone near (by everyone, everywhere, if it
     * remade the universe); the arm that made it burnt the deeper by {@code burn} (see {@link SnapScars}); them weakened and
     * slowed for ten seconds.
     */
    public static void snapped(EntityPlayerMP player, EnumHandSide side, float burn, boolean everywhere) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        MsgSnapEffect effect = new MsgSnapEffect(RANDOM.nextInt(3));
        for (EntityPlayerMP target : server.getPlayerList().getPlayers()) {
            if (everywhere || target.dimension == player.dimension && target.getDistanceSq(player) <= 80D * 80D) {
                EternityNetwork.send(effect, target);
            }
        }
        SnapScars.burn(player, side, burn);
        player.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 200, 1, false, true));
        player.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, 200, 1, false, true));
        SnapPowers.snappedIn(player.dimension);
    }

    // ------------------------------------------------------------------ the gauntlet in hand

    /**
     * The hands changing (dropped, dragged off, swapped, put away). Lucraft's own clean-up of the stones' modifiers is
     * skipped whenever the ability already thinks itself locked by then, which is usual; this does it regardless.
     */
    @SubscribeEvent
    public void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayerMP)
                || (event.getSlot() != EntityEquipmentSlot.MAINHAND && event.getSlot() != EntityEquipmentSlot.OFFHAND)) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.getEntityLiving();
        cleanUp(player);
        // A swap between hands changes both slots one after the other; between them the gauntlet is in neither, which
        // would end the power-up and free the snap for a tick. Look once both have changed.
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server != null) {
            server.addScheduledTask(() -> updateFull(player));
        } else {
            updateFull(player);
        }
    }

    @SubscribeEvent
    public void onItemToss(ItemTossEvent event) {
        if (event.getPlayer() instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) event.getPlayer();
            cleanUp(player);
            updateFull(player);
        }
    }

    /** The Soul Stone's healing goes on after it leaves the hand, as long as its health does; it is refused. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onHeal(LivingHealEvent event) {
        if (event.getEntityLiving() instanceof EntityPlayerMP && !Stones.wields(event.getEntityLiving(), EnumInfinityStone.SOUL)
                && inflated((EntityPlayerMP) event.getEntityLiving())) {
            event.setCanceled(true);
        }
    }

    /** Whether it was Lucraft rebuilding someone's powers that left them without the Time Stone's. */
    @SubscribeEvent
    public void onInitAbilities(InitAbilitiesEvent.Post event) {
        if (event.getEntityLiving() instanceof EntityPlayerMP && !hasTimeAbility((EntityPlayerMP) event.getEntityLiving())) {
            EntityPlayerMP player = (EntityPlayerMP) event.getEntityLiving();
            TimeStoneRates.onAbilityDeactivated(player, player.dimension);
        }
    }

    @SubscribeEvent
    public void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.player instanceof EntityPlayerMP)) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        if (hasTimeAbility(player)) {
            TimeStoneRates.onUserLeftDimension(player, event.fromDim);
        }
        TickrateAPI.changeClientTickrate(player, TimeStoneRates.getRate(event.toDim));
    }

    /**
     * After every entity has updated, so after Lucraft re-applies the stones' modifiers each tick from powers that
     * outlive the gauntlet: the last word on them. And the Time Stone's charge.
     */
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) {
            return;
        }
        for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
            if (hasTimeAbility(player)) {
                TimeStoneCharge.tickPlayer(player);
            }
            cleanUp(player);
            if (ARRIVED.containsKey(player.getUniqueID()) || RESPAWNED.containsKey(player.getUniqueID())) {
                // Looked for every tick until Lucraft has caught up, so the gauntlet they came with is found in time.
                updateFull(player);
            }
            // Stones change rarely, and comparing them is not free.
            if (player.ticksExisted % 5 == 0) {
                rebuildIfStonesChanged(player, player.getHeldItemMainhand(), MAIN_HAND, Ability.EnumAbilityContext.MAIN_HAND);
                rebuildIfStonesChanged(player, player.getHeldItemOffhand(), OFF_HAND, Ability.EnumAbilityContext.OFF_HAND);
            }
        }
    }

    /**
     * Lucraft only rebuilds a held gauntlet's powers when it is equipped, or when its "AbilityUUID" changes; setting a
     * stone only changes its "Items". And its container keeps the very stack in the hand, so changing that stack's tag
     * goes unseen too: the client would show the new powers while the server never gave them, the snap the power-up
     * needs among them. So when the same gauntlet is still held with different stones, its container is rebuilt, here
     * and now, as an equip would.
     */
    private static void rebuildIfStonesChanged(EntityPlayerMP player, ItemStack stack, Map<UUID, String[]> seen,
                                               Ability.EnumAbilityContext context) {
        UUID id = player.getUniqueID();
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemInfinityGauntlet)) {
            seen.remove(id);
            return;
        }
        NBTTagCompound tag = stack.getTagCompound();
        String stones = tag != null && tag.hasKey("Items") ? tag.getTag("Items").toString() : "";
        String gauntlet = tag != null && tag.hasKey("AbilityUUID") ? tag.getTag("AbilityUUID").toString() : "";
        String[] before = seen.put(id, new String[]{gauntlet, stones});
        if (before == null || gauntlet.isEmpty() || !before[0].equals(gauntlet) || before[1].equals(stones)) {
            // Not yet seen, a different gauntlet (whose equip Lucraft handles), or unchanged.
            return;
        }
        // Clearing the provider runs each power's last tick, taking off the modifiers of stones taken out; the update
        // finds the gauntlet again and builds its powers from the stones in it now, all before anything is synced.
        try {
            AbilityContainer container = Ability.getAbilityContainer(context, player);
            if (container != null) {
                container.switchProvider(null);
                container.onUpdate();
            }
        } catch (Exception e) {
            // Leave it for an equip to fix.
        }
        // Lucraft's last tick misses powers that were locked when it ran; the Soul Stone taken out with the Power
        // Stone left in, for one.
        cleanUp(player);
        updateFull(player);
    }

    /** Whether all six stones are in hand now, and the power-up started or stopped on the change. */
    private static void updateFull(EntityPlayerMP player) {
        if (!player.isEntityAlive()) {
            // A body's hands still change as it dies (keepInventory leaves the gauntlet in them); that completes
            // nothing, and a power-up begun on it would play on into the respawn.
            return;
        }
        UUID id = player.getUniqueID();
        boolean had = FULL.contains(id), has = GauntelHelper.hasFullGauntlet(player);
        Integer graceEnd = ARRIVED.get(id);
        boolean cameWithIt = graceEnd != null && graceEnd - player.getServer().getTickCounter() >= 0;
        if (has || !cameWithIt) {
            ARRIVED.remove(id);
        }
        Integer lookEnd = RESPAWNED.get(id);
        if (lookEnd != null && (has || lookEnd - player.getServer().getTickCounter() < 0)) {
            RESPAWNED.remove(id);
        }
        if (!had && has) {
            FULL.add(id);
            if (EternityConfig.stones.powerUp && !cameWithIt) {
                POWER_UP_END.put(id, player.getServer().getTickCounter() + MsgPowerUp.DURATION);
                AbilitySnap snap = findSnap(player);
                if (snap != null) {
                    snap.setMaxCooldown(MsgPowerUp.DURATION);
                    snap.setCooldown(MsgPowerUp.DURATION);
                }
                sendPowerUp(player, true);
            }
        } else if (had && !has) {
            FULL.remove(id);
            POWER_UP_END.remove(id);
            if (EternityConfig.stones.powerUp) {
                AbilitySnap snap = findSnap(player);
                if (snap != null) {
                    snap.setCooldown(0);
                }
                sendPowerUp(player, false);
            }
        }
    }

    private static void sendPowerUp(EntityPlayerMP holder, boolean start) {
        MinecraftServer server = holder.getServer();
        if (server == null) {
            return;
        }
        MsgPowerUp message = new MsgPowerUp(start, holder.getUniqueID());
        for (EntityPlayerMP target : server.getPlayerList().getPlayers()) {
            if (target.dimension == holder.dimension && target.getDistanceSq(holder) <= 80D * 80D) {
                EternityNetwork.send(message, target);
            }
        }
    }

    /** The snap in their hands. Lucraft only looks up exact classes, and the snap here is Eternity's own. */
    private static AbilitySnap findSnap(EntityPlayerMP player) {
        for (Ability ability : Ability.getAbilities(player)) {
            if (ability instanceof AbilitySnap && !(ability instanceof StoneAbilities.Snap)) {
                return (AbilitySnap) ability;
            }
        }
        return null;
    }

    /** Mjolnir's measure, as Heroes Expansion takes it. Without a superpower nobody has it. */
    private static boolean strongEnough(EntityPlayerMP player) {
        try {
            return SuperpowerHandler.hasSuperpower(player) && LCEntityHelper.isStrongEnough(player, 10.0D);
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean hasTimeAbility(EntityPlayerMP player) {
        return Ability.hasAbility(player, AbilitySlowTime.class) || Ability.hasAbility(player, AbilitySpeedTime.class)
                || Ability.hasAbility(player, AbilityResetTime.class) || Ability.hasAbility(player, AbilityTimeUnlocker.class);
    }

    // ------------------------------------------------------------------ leftover modifiers

    /** Whatever the Soul and Power Stones left on someone no longer wielding them, off. */
    private static void cleanUp(EntityPlayerMP player) {
        boolean soul = Stones.wields(player, EnumInfinityStone.SOUL), power = Stones.wields(player, EnumInfinityStone.POWER);
        if (!soul && !power) {
            stripHealth(player);
        }
        if (!power) {
            stripStrength(player);
        }
    }

    private static boolean inflated(EntityPlayerMP player) {
        IAttributeInstance health = player.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
        if (health != null) {
            for (AttributeModifier modifier : health.getModifiers()) {
                if (Math.abs(modifier.getAmount()) > OVERSIZED) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void stripHealth(EntityPlayerMP player) {
        strip(player.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH));
        // Lucraft takes the Soul Stone's health away but leaves what it filled, until the next blow or heal. Down at
        // once, while the change is plain to see (the client copes; see HealthSync).
        float now = player.getHealth();
        if (Float.isNaN(now) || now > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    private static void stripStrength(EntityPlayerMP player) {
        strip(player.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE));
        strip(player.getEntityAttribute(SharedMonsterAttributes.ARMOR));
        strip(player.getAttributeMap().getAttributeInstance(LCAttributes.PUNCH_DAMAGE));
    }

    private static boolean strip(IAttributeInstance attribute) {
        if (attribute == null) {
            return false;
        }
        List<AttributeModifier> oversized = new ArrayList<>();
        for (AttributeModifier modifier : attribute.getModifiers()) {
            if (Math.abs(modifier.getAmount()) > OVERSIZED) {
                oversized.add(modifier);
            }
        }
        for (AttributeModifier modifier : oversized) {
            attribute.removeModifier(modifier);
        }
        return !oversized.isEmpty();
    }
}
