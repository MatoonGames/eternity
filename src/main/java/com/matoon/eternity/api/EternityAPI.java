package com.matoon.eternity.api;

import anvil.infinity.snap.SnapResult;
import com.matoon.eternity.EternityConfig;
import com.matoon.eternity.entity.EntityDestroyerArmor;
import com.matoon.eternity.entity.EternityDamage;
import com.matoon.eternity.item.EternityItems;
import com.matoon.eternity.item.ItemAstralRegulator;
import com.matoon.eternity.item.RegulatorBeings;
import com.matoon.eternity.stones.SnapPowers;
import com.matoon.eternity.stones.Stones;
import com.matoon.eternity.stones.time.TimeStoneRates;
import com.matoon.eternity.story.StoryData;
import com.matoon.eternity.story.StoryDirector;
import com.matoon.eternity.world.CosmicPowers;
import com.matoon.eternity.world.Nexus;
import com.matoon.eternity.world.OdinsVault;
import com.matoon.eternity.world.SovereignStation;
import com.matoon.eternity.world.StarChamber;
import com.matoon.eternity.world.Tamarata;
import com.matoon.eternity.world.Teleport;
import com.matoon.eternity.world.Terrain;
import lucraft.mods.lucraftcore.infinity.EnumInfinityStone;
import lucraft.mods.lucraftcore.superpowers.abilities.Ability;
import lucraft.mods.lucraftcore.util.abilitybar.EnumAbilityBarColor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Everything an addon needs from Eternity, in one place: the Astral Regulator and the beings it holds, the colours
 * of the ability bar, the stones' extra powers, the story's record, the mod's dimensions, its block-safety switch,
 * its damage, and the Destroyer Armor's rules for hitting whoever is inside it.
 * <p>
 * Extend with {@link #registerBeing} (in pre-init) and {@link CosmicAbility}; listen with the events in
 * {@code com.matoon.eternity.api.event} on {@code MinecraftForge.EVENT_BUS}. Everything here keeps working across
 * Eternity's updates; reaching past it into the mod's other classes works too, but those may change. See
 * {@code docs/API.md}.
 */
public final class EternityAPI {

    /** Bumped when anything here changes: the major part only when something is taken away or changes meaning. */
    public static final String API_VERSION = "1.0.0";

    private static final Map<String, RegulatorBeing> BEINGS = new LinkedHashMap<>();

    static {
        RegulatorBeings.registerBuiltIns(EternityAPI::registerBeing);
    }

    private EternityAPI() {
    }

    // ------------------------------------------------------------------ the Astral Regulator

    /** Adds a being the Regulator can hold. In pre-init, before any entity's abilities are laid out. */
    public static synchronized RegulatorBeing registerBeing(RegulatorBeing being) {
        if (BEINGS.containsKey(being.id)) {
            throw new IllegalArgumentException("A Regulator being called " + being.id + " is already registered");
        }
        BEINGS.put(being.id, being);
        return being;
    }

    @Nullable
    public static RegulatorBeing being(String id) {
        return BEINGS.get(id);
    }

    /** Every being the Regulator can hold, Eternity's own first, in the order they are shown and tried. */
    public static Collection<RegulatorBeing> beings() {
        return Collections.unmodifiableCollection(BEINGS.values());
    }

    public static Item regulator() {
        return EternityItems.ASTRAL_REGULATOR;
    }

    public static boolean isRegulator(ItemStack stack) {
        return stack.getItem() instanceof ItemAstralRegulator;
    }

    /** The ids of the beings a Regulator holds, in the order it took them in. */
    public static List<String> absorbed(ItemStack regulator) {
        return ItemAstralRegulator.absorbed(regulator);
    }

    /** Puts a being into a Regulator (posting {@link com.matoon.eternity.api.event.RegulatorAbsorbEvent} if it was not already there). */
    public static void absorb(ItemStack regulator, String being) {
        ItemAstralRegulator.absorb(regulator, being);
    }

    /** A new Regulator already holding these beings, without announcing them: for loot, rewards and creative. */
    public static ItemStack newRegulator(String... beings) {
        ItemStack stack = new ItemStack(EternityItems.ASTRAL_REGULATOR);
        for (String id : beings) {
            ItemAstralRegulator.store(stack, id);
        }
        return stack;
    }

    /** Whether someone holds (in a hand, or in the chest of the Destroyer Armor they pilot) a Regulator with this being in it. */
    public static boolean holds(@Nullable Entity entity, String being) {
        return ItemAstralRegulator.holds(entity, being);
    }

    public static boolean holdsCharged(@Nullable Entity entity) {
        return ItemAstralRegulator.holdsCharged(entity);
    }

    /** Forge Energy a tick a Regulator gives off: a little on its own, and each being's share on top. */
    public static int energyPerTick(ItemStack regulator) {
        return ItemAstralRegulator.energyPerTick(regulator);
    }

    // ------------------------------------------------------------------ the ability bar

    /** Frames an ability on Lucraft's ability bar in this colour, so it reads as whose it is. Hands it back. */
    public static <T extends Ability> T tint(T ability, EnumAbilityBarColor color) {
        ability.getDataManager().set(Ability.BAR_COLOR, color);
        return ability;
    }

    public static EnumAbilityBarColor color(EnumInfinityStone stone) {
        return Force.of(stone).color;
    }

    // ------------------------------------------------------------------ the stones

    /** Whether a stone's power is someone's: in a hand, in a gauntlet, or set in their Destroyer Armor. */
    public static boolean wields(EntityLivingBase entity, EnumInfinityStone stone) {
        return Stones.wields(entity, stone);
    }

    /** The snap, as if with all six stones in hand (which they must have). Whether it worked. Server only. */
    public static boolean snap(EntityLivingBase entity) {
        return SnapPowers.snap(entity);
    }

    public static SnapResult snapMode(EntityLivingBase entity) {
        return SnapPowers.mode(entity);
    }

    /** Turns someone to dust the way the snap does: they can be brought back by a snap that brings back the dusted. */
    public static void dust(WorldServer world, Entity entity) {
        SnapPowers.crumble(world, entity);
    }

    /** How fast time runs in a dimension, as the Time Stone has set it (1 is normal). */
    public static float timeRate(int dimension) {
        return TimeStoneRates.getRate(dimension);
    }

    /** Sets how fast time runs in a dimension, on behalf of whoever set it; undone when they leave it. */
    public static void setTimeRate(EntityPlayerMP actor, int dimension, float rate) {
        TimeStoneRates.applyRate(actor, dimension, rate);
    }

    public static void resetTimeRate(EntityPlayerMP actor, int dimension) {
        TimeStoneRates.resetRate(actor, dimension);
    }

    // ------------------------------------------------------------------ the world

    /**
     * Whether Eternity's powers may break or rewrite blocks at all: the server's no-grief switch and the combat
     * terrain setting. Any addon power that changes the world should check it.
     */
    public static boolean mayDestroyBlocks() {
        return Terrain.enabled();
    }

    /** Whether the world itself may be broken, story set pieces aside: the server's no-grief switch alone. */
    public static boolean mayGrief() {
        return EternityConfig.destroyBlocks;
    }

    /** Removes a block the way Eternity's powers do, if blocks may be destroyed and this one can be. The state taken, or null. */
    @Nullable
    public static IBlockState erase(World world, BlockPos pos) {
        return mayDestroyBlocks() ? Terrain.erase(world, pos) : null;
    }

    /** Burns a tube through the world along a segment, like a void lance; nothing if blocks may not be destroyed. */
    public static void carve(World world, Vec3d from, Vec3d to, double radius, int budget) {
        Terrain.carveLine(world, from, to, radius, budget);
    }

    /** Takes a player anywhere, across dimensions, landing safely on the ground if {@code safe}. */
    public static void teleport(EntityPlayerMP player, int dimension, double x, double y, double z, boolean safe) {
        Teleport.player(player, dimension, x, y, z, safe);
    }

    public static int nexusDimension() {
        return Nexus.dimension();
    }

    public static int vaultDimension() {
        return OdinsVault.dimension();
    }

    public static int starChamberDimension() {
        return StarChamber.dimension();
    }

    public static int sovereignDimension() {
        return SovereignStation.dimension();
    }

    public static int tamarataDimension() {
        return Tamarata.dimension();
    }

    /** Where someone is looking, out to a range: the first living thing in the way, else the first block, else the end. */
    public static Vec3d aim(EntityLivingBase caster, double range) {
        return CosmicPowers.aim(caster, range);
    }

    /** The living thing under someone's crosshair, forgivingly, or null. */
    @Nullable
    public static EntityLivingBase sighted(EntityLivingBase caster, double range) {
        return CosmicPowers.sighted(caster, range);
    }

    // ------------------------------------------------------------------ harm

    /**
     * Cosmic power in mortal hands: ignores armour, is magic, and is named {@code death.attack.eternity.<type>} in
     * the lang file. Eternity's own powers held through the Regulator use this.
     */
    public static DamageSource cosmicDamage(String type, Entity wielder) {
        return EternityDamage.wielded(type, wielder);
    }

    /**
     * Hits someone for a share (0 to 1) of their health; if they are inside the Destroyer Armor, the armour takes a
     * share of its own instead. Bosses use this so a fight is the same fight in the armour or out of it.
     */
    public static boolean strike(EntityLivingBase victim, DamageSource source, float share) {
        return EntityDestroyerArmor.strike(victim, source, share);
    }

    /** Pushes someone; the Destroyer Armor, if they are in it, goes only part as far. */
    public static void shove(Entity victim, double x, double y, double z) {
        EntityDestroyerArmor.shove(victim, x, y, z);
    }

    /** Puts someone somewhere in this world; the Destroyer Armor with them, if they are inside it. */
    public static void place(Entity who, double x, double y, double z, float yaw, float pitch) {
        EntityDestroyerArmor.place(who, x, y, z, yaw, pitch);
    }

    /** The Destroyer Armor someone pilots, or null. */
    @Nullable
    public static EntityLivingBase pilotedArmor(@Nullable Entity entity) {
        return EntityDestroyerArmor.carrier(entity);
    }

    /** Whether someone is sealed inside the Destroyer Armor, so an area attack that hit the armour should skip them. */
    public static boolean sheltered(@Nullable Entity entity) {
        return EntityDestroyerArmor.sheltered(entity);
    }

    // ------------------------------------------------------------------ the story

    /** Whether the story runs on this server at all. */
    public static boolean storyEnabled() {
        return StoryDirector.enabled();
    }

    /** Whether a player (or their fellowship) has seen a beat of the story. Server only; false before a world loads. */
    public static boolean hasSeen(UUID player, String beat) {
        StoryData data = data();
        return data != null && data.player(player).seen(beat);
    }

    /**
     * Records a beat for a player (shared with their fellowship, unless it is one only a player's own), posting
     * {@code StoryBeatEvent} if it is new. Name an addon's beats {@code <modid>:<name>}. True if it was new.
     */
    public static boolean see(UUID player, String beat) {
        StoryData data = data();
        if (data == null || !data.player(player).beats.add(beat)) {
            return false;
        }
        data.markDirty();
        return true;
    }

    /** Whether the world as a whole has seen a beat. */
    public static boolean worldHasSeen(String beat) {
        StoryData data = data();
        return data != null && data.seen(beat);
    }

    public static boolean worldSee(String beat) {
        StoryData data = data();
        return data != null && data.see(beat);
    }

    /** Whether two players quest together. */
    public static boolean fellows(UUID a, UUID b) {
        StoryData data = data();
        return a.equals(b) || data != null && data.fellows(a, b);
    }

    /** Everyone questing with a player, them first. */
    public static List<UUID> fellowship(UUID player) {
        StoryData data = data();
        if (data == null) {
            List<UUID> alone = new ArrayList<>();
            alone.add(player);
            return alone;
        }
        return data.fellows(player);
    }

    /** How far a player has fallen for Death. */
    public static int devotion(UUID player) {
        StoryData data = data();
        return data == null ? 0 : data.player(player).devotion;
    }

    /**
     * A line said aloud in the story's speaker panel, to everyone near enough to hear, under one of the story's
     * speakers (see {@code story.Speakers}). False if the story is off, so the caller can fall back on chat.
     */
    public static boolean shout(Entity at, byte speaker, String text, double range) {
        return StoryDirector.shout(at, speaker, at.getEntityId(), text, range);
    }

    @Nullable
    private static StoryData data() {
        return DimensionManager.getWorld(0) == null ? null : StoryData.get();
    }
}
