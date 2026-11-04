package com.matoon.eternity.stones.mind;

import com.google.common.collect.Multimap;
import com.matoon.eternity.EternityConfig;
import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.stones.Stones;
import com.matoon.eternity.stones.network.MsgMindControlAura;
import com.matoon.eternity.stones.network.MsgMindControlEntries;
import com.matoon.eternity.stones.network.MsgMindControlLock;
import com.matoon.eternity.stones.network.MsgMindControlMenu;
import com.matoon.eternity.stones.network.MsgMindControlResistance;
import com.matoon.eternity.stones.network.MsgMindControlResult;
import lucraft.mods.lucraftcore.infinity.EnumInfinityStone;
import lucraft.mods.lucraftcore.superpowers.Superpower;
import lucraft.mods.lucraftcore.superpowers.SuperpowerHandler;
import lucraft.mods.lucraftcore.superpowers.abilities.Ability;
import lucraft.mods.lucraftcore.superpowers.abilities.AbilityFlight;
import lucraft.mods.lucraftcore.superpowers.events.AbilityKeyEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SPacketHeldItemChange;
import net.minecraft.network.play.server.SPacketSoundEffect;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Everyone the Mind Stone holds, and what they do for whoever holds it.
 * <p>
 * A creature follows its controller and fights whatever attacks them. A player is walked as a bodyguard (players have
 * no navigator, so they are stepped along with vanilla collision, and routed with {@link PlayerBotNavigator} when the
 * way is hard), fights with their best weapon, eats when hungry, and can do nothing for themselves. Players get a
 * timed key press to resist being taken, and every twenty seconds another, harder, to break free. Either way, if they
 * have powers, an autopilot uses them in a fight and flies when the way calls for it, and their controller can use
 * them by hand.
 * <p>
 * Control ends when it runs out, when the controlled fall to a heart or die or leave the world, or when the
 * controller lets go of the Mind Stone. Holding the Soul Stone as well puts everyone they control on their ability
 * bar, to look into.
 */
public class MindControlManager {

    private static final Map<UUID, OwnerControl> BY_OWNER = new HashMap<>();
    private static final Map<UUID, UUID> TARGET_OWNER = new HashMap<>();
    private static final Map<UUID, Long> LAST_ROBOT_ATTACK = new HashMap<>();
    private static final Set<UUID> AUTOMATED_ATTACKS = new HashSet<>();
    private static final Map<UUID, ResistanceAttempt> RESISTANCE = new HashMap<>();
    private static final int LOW_HEALTH = 2;
    /** The ticks a player has to press, and how far a controller's protection reaches (18 blocks, squared). */
    private static final int RESIST_TICKS = 40;
    private static final double GUARD_RANGE_SQ = 324D;

    private static class Controlled {
        final EntityLivingBase target;
        final long expires;
        final float startingHealth;
        final PlayerBotNavigator navigator = new PlayerBotNavigator();
        final Set<String> autoToggles = new HashSet<>();
        long nextAi, nextAbilityCast, heldRelease, nextResistance, abilityCacheTick = Long.MIN_VALUE;
        int blockedTicks, resistanceLevel;
        boolean abilityAutocast = true, navigationFlight;
        Ability heldAbility, flightAbility;
        List<Ability> activeAbilities = Collections.emptyList();

        Controlled(EntityLivingBase target, long expires) {
            this.target = target;
            this.expires = expires;
            startingHealth = target.getHealth();
            nextResistance = target.world.getTotalWorldTime() + 400;
        }
    }

    private static class ResistanceAttempt {
        final UUID token = UUID.randomUUID();
        final EntityPlayerMP owner, target;
        final boolean escape;
        final long end;
        final int windowStart, windowEnd;

        ResistanceAttempt(EntityPlayerMP owner, EntityPlayerMP target, boolean escape, long now, int level) {
            this.owner = owner;
            this.target = target;
            this.escape = escape;
            end = now + RESIST_TICKS;
            // Each later chance to break free has a wider window: a mind grows harder to keep the longer it is held.
            int width = Math.min(14, 4 + level * 2), center = 12 + owner.getRNG().nextInt(17);
            windowStart = center - width / 2;
            windowEnd = center + width / 2;
        }
    }

    private static class OwnerControl {
        EntityPlayerMP owner;
        final Map<UUID, Controlled> targets = new HashMap<>();
        boolean soulLast;
        EntityLivingBase threat, resolvedThreat;
        long threatUntil, resolvedThreatTick = Long.MIN_VALUE;

        OwnerControl(EntityPlayerMP owner) {
            this.owner = owner;
            soulLast = Stones.wields(owner, EnumInfinityStone.SOUL);
        }
    }

    public static void reset() {
        BY_OWNER.clear();
        TARGET_OWNER.clear();
        LAST_ROBOT_ATTACK.clear();
        AUTOMATED_ATTACKS.clear();
        RESISTANCE.clear();
    }

    // ------------------------------------------------------------------ taking and letting go

    /** A creature is taken at once; a player gets their chance to resist first. */
    public static void control(EntityPlayerMP owner, EntityLivingBase target) {
        if (target instanceof EntityPlayerMP) {
            startResistance(owner, (EntityPlayerMP) target, false, 0);
        } else {
            applyControl(owner, target);
        }
    }

    private static void applyControl(EntityPlayerMP owner, EntityLivingBase target) {
        if (target == owner || target.isDead || !(target instanceof EntityCreature || target instanceof EntityPlayerMP)
                || target instanceof EntityArmorStand) {
            return;
        }
        UUID previous = TARGET_OWNER.get(target.getUniqueID());
        if (previous != null) {
            releaseInternal(previous, target.getUniqueID(), false);
        }
        OwnerControl state = BY_OWNER.computeIfAbsent(owner.getUniqueID(), id -> new OwnerControl(owner));
        state.owner = owner;
        int duration = EternityConfig.stones.mindControlSeconds * 20;
        state.targets.put(target.getUniqueID(), new Controlled(target, owner.world.getTotalWorldTime() + duration));
        TARGET_OWNER.put(target.getUniqueID(), owner.getUniqueID());
        aura(target, true);
        if (target instanceof EntityPlayerMP) {
            EternityNetwork.send(new MsgMindControlLock(true, duration), (EntityPlayerMP) target);
            success((EntityPlayerMP) target, "You've been mind controlled by " + owner.getName());
        }
        success(owner, "You have mind controlled " + target.getName());
        sync(state);
    }

    private static void startResistance(EntityPlayerMP owner, EntityPlayerMP target, boolean escape, int level) {
        if (owner == target || RESISTANCE.containsKey(target.getUniqueID())) {
            return;
        }
        ResistanceAttempt attempt = new ResistanceAttempt(owner, target, escape, owner.world.getTotalWorldTime(), level);
        RESISTANCE.put(target.getUniqueID(), attempt);
        EternityNetwork.send(new MsgMindControlResistance(attempt.token, RESIST_TICKS, attempt.windowStart, attempt.windowEnd, escape), target);
    }

    /** When a player pressed, {@code elapsed} ticks into their chance; inside the window, they resist. */
    public static void resistanceResponse(EntityPlayerMP target, UUID token, int elapsed) {
        ResistanceAttempt attempt = RESISTANCE.get(target.getUniqueID());
        if (attempt == null || !attempt.token.equals(token) || elapsed < 0 || elapsed > RESIST_TICKS) {
            return;
        }
        RESISTANCE.remove(target.getUniqueID());
        EternityNetwork.send(new MsgMindControlResult(), target);
        if (elapsed >= attempt.windowStart && elapsed <= attempt.windowEnd) {
            if (attempt.escape) {
                failure(attempt.owner, target.getName() + " has broken your mind control");
                failure(target, "You broke free from " + attempt.owner.getName() + "'s mind control");
                release(attempt.owner, target.getUniqueID(), false);
            } else {
                failure(attempt.owner, target.getName() + " has resisted your mind control attempt");
                failure(target, "You resisted " + attempt.owner.getName() + "'s mind control attempt");
            }
        } else if (!attempt.escape) {
            applyControl(attempt.owner, target);
        }
    }

    /** Lets them go; drained first, all but half a heart of their health going to the controller. */
    public static boolean release(EntityPlayerMP owner, UUID targetId, boolean drain) {
        if (!owner.getUniqueID().equals(TARGET_OWNER.get(targetId))) {
            return false;
        }
        Controlled control = get(owner.getUniqueID(), targetId);
        if (control == null) {
            return false;
        }
        if (drain && control.target.getHealth() > 1F) {
            float stolen = control.target.getHealth() - 1F;
            control.target.setHealth(1F);
            owner.heal(stolen);
        }
        releaseInternal(owner.getUniqueID(), targetId, true);
        return true;
    }

    private static void releaseInternal(UUID ownerId, UUID targetId, boolean sync) {
        OwnerControl state = BY_OWNER.get(ownerId);
        if (state == null) {
            return;
        }
        Controlled removed = state.targets.remove(targetId);
        TARGET_OWNER.remove(targetId);
        if (removed != null) {
            letGo(removed);
        }
        if (sync) {
            sync(state);
        }
        // Owners left with nobody are pruned by the server tick, never from inside the death and combat events that
        // may run while it iterates them.
    }

    /** Everything control put on someone, taken off. */
    private static void letGo(Controlled control) {
        stopAutomaticAbilities(control, false);
        TARGET_OWNER.remove(control.target.getUniqueID());
        if (control.target instanceof EntityCreature) {
            ((EntityCreature) control.target).setAttackTarget(null);
        }
        aura(control.target, false);
        if (control.target instanceof EntityPlayerMP) {
            EternityNetwork.send(new MsgMindControlLock(false, 0), (EntityPlayerMP) control.target);
        }
    }

    private static void aura(EntityLivingBase entity, boolean enabled) {
        EternityNetwork.sendToAll(new MsgMindControlAura(entity.getUniqueID(), enabled));
    }

    /** Everyone they control onto their ability bar, if they hold the Soul Stone too. */
    private static void sync(OwnerControl state) {
        if (state.owner == null || !state.owner.connection.netManager.isChannelOpen()) {
            return;
        }
        boolean soul = Stones.wields(state.owner, EnumInfinityStone.SOUL);
        List<MsgMindControlEntries.Entry> entries = new ArrayList<>();
        if (soul) {
            for (Map.Entry<UUID, Controlled> entry : state.targets.entrySet()) {
                EntityLivingBase target = entry.getValue().target;
                entries.add(new MsgMindControlEntries.Entry(entry.getKey(), target.getName(), target instanceof EntityPlayerMP, target.getEntityId()));
            }
        }
        EternityNetwork.send(new MsgMindControlEntries(entries), state.owner);
        state.soulLast = soul;
    }

    private static Controlled get(UUID owner, UUID target) {
        OwnerControl state = BY_OWNER.get(owner);
        return state == null ? null : state.targets.get(target);
    }

    private static void status(EntityPlayerMP player, String message, TextFormatting color) {
        player.sendStatusMessage(new TextComponentString(color + message), true);
    }

    private static void sound(EntityPlayerMP player, SoundEvent sound, float pitch) {
        player.connection.sendPacket(new SPacketSoundEffect(sound, SoundCategory.PLAYERS, player.posX, player.posY, player.posZ, 1F, pitch));
    }

    private static void success(EntityPlayerMP player, String message) {
        status(player, message, TextFormatting.LIGHT_PURPLE);
        sound(player, SoundEvents.ENTITY_PLAYER_LEVELUP, 1.2F);
    }

    private static void failure(EntityPlayerMP player, String message) {
        status(player, message, TextFormatting.RED);
        sound(player, SoundEvents.BLOCK_GLASS_BREAK, 0.9F);
    }

    // ------------------------------------------------------------------ the window into them

    /** What the server sees of them, for their controller's window into them. */
    public static void openMenu(EntityPlayerMP owner, UUID targetId) {
        Controlled control = get(owner.getUniqueID(), targetId);
        if (control == null) {
            return;
        }
        EntityLivingBase target = control.target;
        boolean player = target instanceof EntityPlayerMP;
        List<ItemStack> items = new ArrayList<>();
        if (player) {
            EntityPlayerMP held = (EntityPlayerMP) target;
            for (int i = 0; i < held.inventory.getSizeInventory(); i++) {
                items.add(held.inventory.getStackInSlot(i).copy());
            }
        } else {
            for (EntityEquipmentSlot slot : EntityEquipmentSlot.values()) {
                items.add(target.getItemStackFromSlot(slot).copy());
            }
        }
        int remaining = (int) Math.max(0, control.expires - owner.world.getTotalWorldTime());
        List<MsgMindControlMenu.AbilityInfo> abilities = new ArrayList<>();
        for (Ability ability : Ability.getAbilities(target)) {
            if (ability.isUnlocked() && !ability.isHidden()) {
                boolean triggerable = ability.getAbilityType() != Ability.AbilityType.CONSTANT;
                abilities.add(new MsgMindControlMenu.AbilityInfo(ability.getKey(), ability.getDisplayName(), ability.getDisplayDescription(),
                        triggerable && !ability.isCoolingdown(), triggerable));
            }
        }
        Superpower superpower = SuperpowerHandler.getSuperpower(target);
        String superpowerId = superpower == null || superpower.getRegistryName() == null ? "" : superpower.getRegistryName().toString();
        String superpowerName = superpower == null ? "" : superpower.getDisplayName();
        EternityNetwork.send(new MsgMindControlMenu(targetId, target.getEntityId(), target.getName(), player, items, target.getHealth(),
                target.getMaxHealth(), target.getTotalArmorValue(), remaining, abilities, control.abilityAutocast, superpowerId, superpowerName), owner);
    }

    /** One of their powers, used at their controller's word. Only ever on the server, and only for their own thrall. */
    public static void triggerAbility(EntityPlayerMP owner, UUID targetId, String key) {
        Controlled control = get(owner.getUniqueID(), targetId);
        if (control == null || key == null) {
            return;
        }
        long now = owner.world.getTotalWorldTime();
        control.abilityCacheTick = Long.MIN_VALUE;
        for (Ability ability : activeAbilities(control, now)) {
            if (key.equals(ability.getKey()) && !ability.isCoolingdown()) {
                activate(control, ability, now, false);
                return;
            }
        }
    }

    public static void setAbilityAutocast(EntityPlayerMP owner, UUID targetId, boolean enabled) {
        Controlled control = get(owner.getUniqueID(), targetId);
        if (control != null) {
            control.abilityAutocast = enabled;
            control.nextAbilityCast = 0;
        }
    }

    // ------------------------------------------------------------------ their powers

    private static List<Ability> activeAbilities(Controlled control, long now) {
        refreshAbilities(control, now);
        return control.activeAbilities;
    }

    private static Ability findFlight(Controlled control, long now) {
        refreshAbilities(control, now);
        return control.flightAbility;
    }

    /**
     * Lucraft replaces ability instances as their providers rebuild, so they are looked up again each tick: kept
     * longer, the autopilot would be pressing powers that are no longer theirs.
     */
    private static void refreshAbilities(Controlled control, long now) {
        if (control.abilityCacheTick == now) {
            return;
        }
        List<Ability> result = new ArrayList<>();
        Ability flight = null;
        for (Ability ability : Ability.getAbilities(control.target)) {
            if (ability.isUnlocked() && !ability.isHidden()) {
                if (flight == null && ability instanceof AbilityFlight) {
                    flight = ability;
                }
                if (ability.getAbilityType() != Ability.AbilityType.CONSTANT) {
                    result.add(ability);
                }
            }
        }
        control.activeAbilities = result;
        control.flightAbility = flight;
        control.abilityCacheTick = now;
    }

    private static void activate(Controlled control, Ability ability, long now, boolean automatic) {
        if (ability.getAbilityType() == Ability.AbilityType.HELD) {
            if (control.heldAbility != null) {
                control.heldAbility.onKeyReleased();
            }
            ability.onKeyPressed();
            control.heldAbility = ability;
            control.heldRelease = now + 30;
        } else {
            ability.onKeyPressed();
            ability.onKeyReleased();
            if (automatic && ability.getAbilityType() == Ability.AbilityType.TOGGLE && ability.isEnabled()) {
                control.autoToggles.add(ability.getKey());
            }
        }
    }

    private static void toggle(Ability ability) {
        ability.onKeyPressed();
        ability.onKeyReleased();
    }

    /** Whatever the autopilot turned on, off; the flight it is navigating with kept if asked. */
    private static void stopAutomaticAbilities(Controlled control, boolean keepNavigationFlight) {
        if (control.heldAbility != null) {
            control.heldAbility.onKeyReleased();
            control.heldAbility = null;
        }
        if (control.autoToggles.isEmpty()) {
            return;
        }
        long now = control.target.world.getTotalWorldTime();
        Ability flight = findFlight(control, now);
        for (Ability ability : activeAbilities(control, now)) {
            if (control.autoToggles.contains(ability.getKey()) && !(keepNavigationFlight && ability == flight)
                    && ability.getAbilityType() == Ability.AbilityType.TOGGLE && ability.isEnabled()) {
                toggle(ability);
            }
        }
        if (keepNavigationFlight) {
            String flightKey = flight == null ? null : flight.getKey();
            control.autoToggles.removeIf(key -> !key.equals(flightKey));
        } else {
            control.autoToggles.clear();
        }
    }

    /** In a fight, a random one of their powers every four to seven seconds. */
    private static void autocast(EntityPlayerMP owner, Controlled control, long now) {
        if (control.heldAbility != null && now >= control.heldRelease) {
            control.heldAbility.onKeyReleased();
            control.heldAbility = null;
        }
        EntityLivingBase enemy = combatThreat(owner, control.target);
        if (!control.abilityAutocast || enemy == null) {
            stopAutomaticAbilities(control, control.navigationFlight);
            return;
        }
        if (now < control.nextAbilityCast || control.heldAbility != null) {
            return;
        }
        face(control.target, enemy);
        Ability selected = null;
        int candidates = 0;
        for (Ability ability : activeAbilities(control, now)) {
            if (!(ability instanceof AbilityFlight) && !ability.isCoolingdown()
                    && (ability.getAbilityType() != Ability.AbilityType.TOGGLE || !ability.isEnabled())
                    && owner.getRNG().nextInt(++candidates) == 0) {
                selected = ability;
            }
        }
        control.nextAbilityCast = now + 80 + owner.getRNG().nextInt(61);
        if (selected != null) {
            activate(control, selected, now, true);
        }
    }

    /** Flight on when the way calls for it (a climb, a gap, a long way, a wall), and off once it does not. */
    private static boolean navigateFlight(Controlled control, Ability flight, long now, boolean needed) {
        control.navigationFlight = needed;
        if (needed && !flight.isEnabled()) {
            activate(control, flight, now, true);
        } else if (!needed && flight != null && control.autoToggles.remove(flight.getKey()) && flight.isEnabled()) {
            toggle(flight);
        }
        return flight != null && flight.isEnabled();
    }

    // ------------------------------------------------------------------ each tick

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        for (Iterator<ResistanceAttempt> it = RESISTANCE.values().iterator(); it.hasNext(); ) {
            ResistanceAttempt attempt = it.next();
            boolean invalid = attempt.owner.isDead || attempt.target.isDead || attempt.owner.world != attempt.target.world
                    || !Stones.wields(attempt.owner, EnumInfinityStone.MIND);
            if (invalid || attempt.target.world.getTotalWorldTime() >= attempt.end) {
                it.remove();
                EternityNetwork.send(new MsgMindControlResult(), attempt.target);
                // Missing the chance entirely is not resisting.
                if (!invalid && !attempt.escape) {
                    applyControl(attempt.owner, attempt.target);
                }
            }
        }
        for (Iterator<Map.Entry<UUID, OwnerControl>> owners = BY_OWNER.entrySet().iterator(); owners.hasNext(); ) {
            Map.Entry<UUID, OwnerControl> entry = owners.next();
            OwnerControl state = entry.getValue();
            EntityPlayerMP owner = state.owner;
            // Once a second per controller, never per thrall: whether they logged out, or back in.
            if (owner == null || owner.ticksExisted % 20 == 0) {
                owner = FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayerByUUID(entry.getKey());
            }
            // A fallen controller keeps the gauntlet through death (keepInventory) but not their hold on anyone.
            if (owner == null || !owner.isEntityAlive() || !Stones.wields(owner, EnumInfinityStone.MIND)) {
                for (Controlled control : state.targets.values()) {
                    letGo(control);
                }
                if (owner != null) {
                    EternityNetwork.send(new MsgMindControlEntries(Collections.emptyList()), owner);
                }
                owners.remove();
                continue;
            }
            state.owner = owner;
            if (state.soulLast != Stones.wields(owner, EnumInfinityStone.SOUL)) {
                sync(state);
            }
            long now = owner.world.getTotalWorldTime();
            for (Iterator<Controlled> it = state.targets.values().iterator(); it.hasNext(); ) {
                Controlled control = it.next();
                EntityLivingBase target = control.target;
                // Someone drained and taken again at half a heart is only freed by falling there again.
                boolean broken = target.getHealth() <= LOW_HEALTH && control.startingHealth > LOW_HEALTH;
                if (target.isDead || target.world != owner.world || broken || now >= control.expires) {
                    if (broken) {
                        failure(owner, "Your mind control was broken on " + target.getName());
                        if (target instanceof EntityPlayerMP) {
                            sound((EntityPlayerMP) target, SoundEvents.BLOCK_GLASS_BREAK, 0.9F);
                        }
                    }
                    it.remove();
                    letGo(control);
                    sync(state);
                    continue;
                }
                if (target instanceof EntityPlayerMP) {
                    robot((EntityPlayerMP) target, owner, control, now);
                }
                autocast(owner, control, now);
                if (target instanceof EntityPlayerMP && now >= control.nextResistance && !RESISTANCE.containsKey(target.getUniqueID())) {
                    startResistance(owner, (EntityPlayerMP) target, true, control.resistanceLevel++);
                    control.nextResistance = now + 400;
                }
                if (now >= control.nextAi) {
                    control.nextAi = now + 5;
                    if (target instanceof EntityCreature) {
                        steer((EntityCreature) target, owner, control, now);
                    }
                }
            }
            if (state.targets.isEmpty()) {
                owners.remove();
            }
        }
    }

    // ------------------------------------------------------------------ creatures

    private static void steer(EntityCreature mob, EntityPlayerMP owner, Controlled control, long now) {
        if (mob.getAttackTarget() == owner || !validThreat(mob.getAttackTarget(), owner)) {
            mob.setAttackTarget(null);
        }
        EntityLivingBase threat = threat(owner);
        EntityLivingBase destination = threat != null && threat != mob ? threat : owner;
        if (threat != null && threat != mob) {
            mob.setAttackTarget(threat);
        } else if (mob.getAttackTarget() == mob || isControlled(mob.getAttackTarget())) {
            mob.setAttackTarget(null);
        }
        Ability flight = findFlight(control, now);
        double distance = Math.sqrt(mob.getDistanceSq(destination)), dy = destination.posY - mob.posY;
        boolean needed = control.abilityAutocast && flight != null
                && (Math.abs(dy) > 3D || distance > 20D || hasUnsafeGap(mob, destination.posX, destination.posZ));
        if (navigateFlight(control, flight, now, needed)) {
            double dx = destination.posX - mob.posX, dz = destination.posZ - mob.posZ;
            double fy = destination.posY + Math.max(1D, destination.height * 0.5D) - mob.posY;
            double length = Math.sqrt(dx * dx + fy * fy + dz * dz);
            if (length > 0.4D) {
                face(mob, destination);
                double speed = distance > 12D ? 0.38D : 0.24D;
                mob.moveForward = 1F;
                mob.motionX = dx / length * speed;
                mob.motionY = fy / length * speed;
                mob.motionZ = dz / length * speed;
                mob.fallDistance = 0F;
            } else {
                mob.moveForward = 0F;
                mob.motionX = mob.motionY = mob.motionZ = 0D;
            }
            return;
        }
        if (destination == owner && mob.getDistanceSq(owner) > 25D) {
            mob.getNavigator().tryMoveToEntityLiving(owner, 1.15D);
        }
    }

    // ------------------------------------------------------------------ players

    private static void robot(EntityPlayerMP player, EntityPlayerMP owner, Controlled control, long now) {
        EntityLivingBase threat = threat(owner);
        EntityLivingBase destination = threat != null && owner.getDistanceSq(threat) <= GUARD_RANGE_SQ && player.getDistanceSq(threat) < 400D ? threat : owner;
        double toController = Math.sqrt(player.getDistanceSq(owner));
        // Holding a loose formation, rather than chasing a spot behind a controller who keeps turning, keeps them from
        // circling and crowding.
        boolean holdFormation = destination == owner && toController >= 4D && toController <= 8D;
        double desiredX = destination.posX, desiredY = destination.getEntityBoundingBox().minY, desiredZ = destination.posZ;
        if (destination == owner) {
            Vec3d facing = owner.getLookVec();
            desiredX -= facing.x * 6D;
            desiredZ -= facing.z * 6D;
        }
        Ability flight = findFlight(control, now);
        boolean flying = navigateFlight(control, flight, now, control.abilityAutocast && flight != null
                && (Math.abs(desiredY - player.posY) > 3D || player.getDistance(destination) > 20D || control.blockedTicks >= 6
                || hasUnsafeGap(player, desiredX, desiredZ)));
        if (flying) {
            desiredY = destination.posY + (destination == owner ? 2.5D : Math.max(1D, destination.height * 0.55D));
        }
        BlockPos requested = new BlockPos(desiredX, desiredY, desiredZ);
        // Walk straight at anything near or in sight; only a long, hidden way is worth a path.
        boolean needsRoute = player.getDistanceSq(destination) > 144D && !player.canEntityBeSeen(destination);
        BlockPos waypoint = needsRoute ? control.navigator.next(player, requested, now) : requested;
        double dx = waypoint.getX() + 0.5D - player.posX, dz = waypoint.getZ() + 0.5D - player.posZ, dy = flying ? desiredY - player.posY : 0D;
        double distance = Math.sqrt(dx * dx + dz * dz), travel = flying ? Math.sqrt(dx * dx + dy * dy + dz * dz) : distance;
        boolean sprint = destination == owner && owner.getDistanceSq(player) > 81D || destination == threat && player.getDistanceSq(threat) > 25D;
        player.setSprinting(sprint);
        if (!holdFormation && travel > 0.18D) {
            double speed = flying ? (sprint ? 0.48D : 0.32D) : (sprint ? 0.21D : 0.14D);
            double step = Math.min(speed, travel), mx = dx / travel * step, mz = dz / travel * step;
            float yaw = (float) (Math.atan2(-mx, mz) * 180D / Math.PI);
            player.rotationYaw = yaw;
            // Moved through vanilla's own collision and step-up, with gravity, jumps over single blocks, ladders, and a
            // side-step when blocked; never through anything.
            double vertical;
            if (flying) {
                vertical = dy / travel * step;
                player.fallDistance = 0F;
                player.moveForward = 1F;
                player.rotationPitch = (float) (-Math.atan2(dy, Math.max(0.001D, distance)) * 180D / Math.PI);
            } else if (player.isOnLadder()) {
                vertical = destination.posY > player.posY + 0.25D ? 0.15D : destination.posY < player.posY - 0.25D ? -0.10D : 0D;
                player.fallDistance = 0F;
            } else {
                vertical = player.onGround ? 0D : Math.max(-0.42D, player.motionY - 0.08D);
            }
            double beforeX = player.posX, beforeZ = player.posZ;
            player.move(MoverType.SELF, mx, vertical, mz);
            if (!flying && player.collidedHorizontally && player.onGround && !player.isOnLadder()) {
                player.motionY = 0.42D;
                control.blockedTicks++;
            } else {
                control.blockedTicks = 0;
                player.motionY = flying ? vertical : player.onGround ? 0D : vertical * 0.98D;
            }
            if (control.blockedTicks >= 4 && Math.abs(player.posX - beforeX) + Math.abs(player.posZ - beforeZ) < 0.01D) {
                double side = control.blockedTicks % 16 < 8 ? 1D : -1D;
                player.move(MoverType.SELF, -mz * side, 0D, mx * side);
            }
            player.connection.setPlayerLocation(player.posX, player.posY, player.posZ, yaw, player.rotationPitch);
        } else if (flying) {
            player.moveForward = 0F;
            player.motionX = player.motionY = player.motionZ = 0D;
            player.fallDistance = 0F;
        }
        Long last = LAST_ROBOT_ATTACK.get(player.getUniqueID());
        if (threat != null && owner.getDistanceSq(threat) <= GUARD_RANGE_SQ && player.getDistanceSq(threat) <= 16D && (last == null || now - last >= 10)) {
            equipBestWeapon(player);
            AUTOMATED_ATTACKS.add(player.getUniqueID());
            try {
                // A real player's attack: weapon, enchantments, cooldown, criticals and all.
                player.swingArm(EnumHand.MAIN_HAND);
                player.attackTargetEntityWithCurrentItem(threat);
            } finally {
                AUTOMATED_ATTACKS.remove(player.getUniqueID());
            }
            LAST_ROBOT_ATTACK.put(player.getUniqueID(), now);
        }
        if (now % 40 == 0) {
            feed(player);
        }
    }

    private static void feed(EntityPlayerMP player) {
        if (player.getFoodStats().getFoodLevel() > 16 && player.getHealth() > player.getMaxHealth() * 0.5F) {
            return;
        }
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof ItemFood) {
                player.getFoodStats().addStats((ItemFood) stack.getItem(), stack);
                stack.shrink(1);
                player.heal(1F);
                player.world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_GENERIC_EAT, SoundCategory.PLAYERS, 0.5F, 1F);
                return;
            }
        }
    }

    /** The hotbar's hardest-hitting weapon into their hand, by its attack damage, so modded weapons count too. */
    private static void equipBestWeapon(EntityPlayerMP player) {
        int bestSlot = player.inventory.currentItem;
        double bestDamage = mainHandDamage(player.inventory.getStackInSlot(bestSlot));
        for (int slot = 0; slot < 9; slot++) {
            double damage = mainHandDamage(player.inventory.getStackInSlot(slot));
            if (damage > bestDamage) {
                bestDamage = damage;
                bestSlot = slot;
            }
        }
        if (player.inventory.currentItem != bestSlot) {
            player.inventory.currentItem = bestSlot;
            player.connection.sendPacket(new SPacketHeldItemChange(bestSlot));
            player.inventoryContainer.detectAndSendChanges();
        }
    }

    private static double mainHandDamage(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0D;
        }
        Multimap<String, AttributeModifier> modifiers = stack.getAttributeModifiers(EntityEquipmentSlot.MAINHAND);
        double damage = 0D;
        for (AttributeModifier modifier : modifiers.get(SharedMonsterAttributes.ATTACK_DAMAGE.getName())) {
            damage += modifier.getAmount();
        }
        return damage;
    }

    // ------------------------------------------------------------------ who is the enemy

    private static void face(EntityLivingBase source, EntityLivingBase target) {
        double dx = target.posX - source.posX, dz = target.posZ - source.posZ;
        double dy = target.posY + target.getEyeHeight() - (source.posY + source.getEyeHeight()), flat = Math.sqrt(dx * dx + dz * dz);
        source.rotationYaw = (float) (Math.atan2(-dx, dz) * 180D / Math.PI);
        source.rotationYawHead = source.rotationYaw;
        source.rotationPitch = (float) (-Math.atan2(dy, flat) * 180D / Math.PI);
    }

    /** Who to fight: whoever threatens the controller, or else whoever the thrall is already fighting. */
    private static EntityLivingBase combatThreat(EntityPlayerMP owner, EntityLivingBase companion) {
        EntityLivingBase target = threat(owner);
        if (target != null) {
            return target;
        }
        target = companion.getRevengeTarget();
        if (validThreat(target, owner)) {
            return target;
        }
        if (companion instanceof EntityCreature) {
            target = ((EntityCreature) companion).getAttackTarget();
            if (validThreat(target, owner)) {
                return target;
            }
        }
        if (companion instanceof EntityPlayerMP) {
            target = ((EntityPlayerMP) companion).getLastAttackedEntity();
            if (validThreat(target, owner)) {
                return target;
            }
        }
        return null;
    }

    /**
     * Whoever threatens the controller, within 18 blocks: whoever last struck them, or a monster after them. Never just
     * any monster nearby, or thralls would chase things that were never a danger. Worked out once a tick.
     */
    private static EntityLivingBase threat(EntityPlayerMP owner) {
        OwnerControl state = BY_OWNER.get(owner.getUniqueID());
        long now = owner.world.getTotalWorldTime();
        if (state != null && state.resolvedThreatTick == now) {
            return state.resolvedThreat;
        }
        EntityLivingBase result;
        if (state != null && state.threat != null && state.threatUntil >= now && validThreat(state.threat, owner)
                && owner.getDistanceSq(state.threat) <= GUARD_RANGE_SQ) {
            result = state.threat;
        } else {
            result = owner.getRevengeTarget();
            if (!validThreat(result, owner) || owner.getDistanceSq(result) > GUARD_RANGE_SQ) {
                result = null;
                for (EntityMob mob : owner.world.getEntitiesWithinAABB(EntityMob.class, owner.getEntityBoundingBox().grow(12D))) {
                    if (validThreat(mob, owner) && (mob.getAttackTarget() == owner || mob.getRevengeTarget() == owner)) {
                        result = mob;
                        break;
                    }
                }
            }
        }
        if (state != null) {
            state.resolvedThreat = result;
            state.resolvedThreatTick = now;
        }
        return result;
    }

    private static boolean validThreat(EntityLivingBase entity, EntityPlayerMP owner) {
        return entity != null && !entity.isDead && !entity.isInvisible() && entity != owner && !isControlled(entity)
                && !(entity instanceof EntityArmorStand) && (entity instanceof EntityMob || entity instanceof EntityPlayerMP)
                && (owner == null || owner.canEntityBeSeen(entity));
    }

    private static boolean hasUnsafeGap(EntityLivingBase entity, double targetX, double targetZ) {
        double dx = targetX - entity.posX, dz = targetZ - entity.posZ, length = Math.sqrt(dx * dx + dz * dz);
        if (length < 3D) {
            return false;
        }
        double sample = Math.min(4D, length);
        BlockPos base = new BlockPos(entity.posX + dx / length * sample, entity.getEntityBoundingBox().minY, entity.posZ + dz / length * sample);
        for (int down = 1; down <= 4; down++) {
            if (entity.world.getBlockState(base.down(down)).getMaterial().blocksMovement()) {
                return false;
            }
        }
        return true;
    }

    private static boolean isControlled(Entity entity) {
        return entity != null && TARGET_OWNER.containsKey(entity.getUniqueID());
    }

    // ------------------------------------------------------------------ what thralls may not do

    @SubscribeEvent
    public void onAttacked(LivingAttackEvent event) {
        Entity source = event.getSource().getTrueSource();
        if (!(source instanceof EntityLivingBase)) {
            return;
        }
        EntityLivingBase victim = event.getEntityLiving();
        // A monster's own AI may take its old target back between steers; it never lands a blow on its controller or a
        // fellow thrall.
        UUID sourceOwner = TARGET_OWNER.get(source.getUniqueID());
        if (sourceOwner != null && (sourceOwner.equals(victim.getUniqueID()) || isControlled(victim))) {
            event.setCanceled(true);
            if (source instanceof EntityCreature) {
                ((EntityCreature) source).setAttackTarget(null);
            }
            return;
        }
        if (isControlled(source) || !validThreat((EntityLivingBase) source, victim instanceof EntityPlayerMP ? (EntityPlayerMP) victim : null)) {
            return;
        }
        // Someone struck a controller: every thrall of theirs turns on them.
        OwnerControl state = BY_OWNER.get(victim.getUniqueID());
        if (state != null) {
            state.threat = (EntityLivingBase) source;
            state.threatUntil = victim.world.getTotalWorldTime() + 100;
            state.resolvedThreatTick = Long.MIN_VALUE;
            for (Controlled control : state.targets.values()) {
                if (control.target instanceof EntityCreature && control.target != source) {
                    ((EntityCreature) control.target).setAttackTarget((EntityLivingBase) source);
                }
            }
        }
    }

    @SubscribeEvent
    public void onSetTarget(LivingSetAttackTargetEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        if (!(entity instanceof EntityCreature) || entity.world.isRemote || !isControlled(entity)) {
            return;
        }
        UUID ownerId = TARGET_OWNER.get(entity.getUniqueID());
        EntityLivingBase target = event.getTarget();
        if (ownerId != null && (target != null && ownerId.equals(target.getUniqueID()) || isControlled(target))) {
            ((WorldServer) entity.world).addScheduledTask(() -> ((EntityCreature) entity).setAttackTarget(null));
        }
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        UUID owner = TARGET_OWNER.get(event.getEntityLiving().getUniqueID());
        if (owner != null) {
            releaseInternal(owner, event.getEntityLiving().getUniqueID(), true);
        }
    }

    /** Left-clicking the air cannot be cancelled in this Forge, and crashes the client if tried; the rest are. */
    @SubscribeEvent
    public void onInteract(PlayerInteractEvent event) {
        if (isControlled(event.getEntityPlayer()) && event.isCancelable()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (isControlled(player) && !AUTOMATED_ATTACKS.contains(player.getUniqueID())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (isControlled(event.getEntityPlayer())) {
            event.setNewSpeed(0F);
        }
    }

    /** A thrall's own ability keys do nothing; their controller and the autopilot use their powers directly. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onAbilityKey(AbilityKeyEvent.Server event) {
        if (isControlled(event.entity) && event.isCancelable()) {
            event.setCanceled(true);
        }
    }
}
