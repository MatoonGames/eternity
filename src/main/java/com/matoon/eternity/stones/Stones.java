package com.matoon.eternity.stones;

import anvil.infinity.api.AbilityAdderHandler;
import anvil.infinity.helpers.GauntelHelper;
import com.matoon.eternity.Eternity;
import com.matoon.eternity.entity.EntityDestroyerArmor;
import com.matoon.eternity.stones.mind.AbilityMindControl;
import com.matoon.eternity.stones.mind.MindControlManager;
import com.matoon.eternity.stones.time.AbilityResetTime;
import com.matoon.eternity.stones.time.AbilitySlowTime;
import com.matoon.eternity.stones.time.AbilitySpeedTime;
import com.matoon.eternity.stones.time.AbilityTimeUnlocker;
import lucraft.mods.lucraftcore.infinity.EnumInfinityStone;
import lucraft.mods.lucraftcore.superpowers.abilities.AbilityEntry;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * The Infinity Stones' extra powers, and the fixes to InfinityCraft they need, brought over from Beyonder (modid
 * {@code herosmp}): the full gauntlet's power-up, the snap's flash and mark, the Time Stone's hold on time, the Mind
 * Stone's hold on minds, and the gauntlet's leftover health and strength cleaned off whoever puts it down.
 * <p>
 * Beyonder does all of this itself. With it installed, none of this runs, so nothing happens twice: only the ability
 * entries, sounds and packets are still registered, since those must match between client and server and harm
 * nothing unused.
 */
@Mod.EventBusSubscriber(modid = Eternity.MODID)
public final class Stones {

    private static Boolean active;

    private Stones() {
    }

    /** Whether these powers are Eternity's to run: they are, unless Beyonder is here to run its own. */
    public static boolean active() {
        if (active == null) {
            active = !Loader.isModLoaded("herosmp");
        }
        return active;
    }

    /** In pre-init. */
    public static void register() {
        if (!active()) {
            Eternity.LOG.info("Beyonder is installed; leaving the Infinity Stones' extra powers to it.");
            return;
        }
        MinecraftForge.EVENT_BUS.register(new InfinityPatches());
        MinecraftForge.EVENT_BUS.register(new MindControlManager());
        MinecraftForge.EVENT_BUS.register(SnapPowers.INSTANCE);
        MinecraftForge.EVENT_BUS.register(SnapScars.INSTANCE);
        AbilityAdderHandler.register(new StoneAbilityAdder());
    }

    @SubscribeEvent
    public static void onRegisterAbilities(RegistryEvent.Register<AbilityEntry> event) {
        event.getRegistry().registerAll(
                new AbilityEntry(AbilitySlowTime.class, new ResourceLocation(Eternity.MODID, "slow_time")),
                new AbilityEntry(AbilitySpeedTime.class, new ResourceLocation(Eternity.MODID, "speed_time")),
                new AbilityEntry(AbilityResetTime.class, new ResourceLocation(Eternity.MODID, "reset_time")),
                new AbilityEntry(AbilityTimeUnlocker.class, new ResourceLocation(Eternity.MODID, "time_unlocker")),
                new AbilityEntry(AbilityMindControl.class, new ResourceLocation(Eternity.MODID, "mind_control")),
                new AbilityEntry(AbilityEternitySnap.class, new ResourceLocation(Eternity.MODID, "snap")),
                new AbilityEntry(AbilitySnapMode.class, new ResourceLocation(Eternity.MODID, "snap_mode")));
    }

    /**
     * Whether a stone's power is someone's to use: in either hand, alone or in a gauntlet, as InfinityCraft counts
     * it, or set in the chest of the Destroyer Armor they are piloting, which lends them its stones' powers.
     */
    public static boolean wields(EntityLivingBase entity, EnumInfinityStone stone) {
        boolean held;
        switch (stone) {
            case SOUL:
                held = GauntelHelper.hasSoulStone(entity);
                break;
            case POWER:
                held = GauntelHelper.hasPowerStone(entity);
                break;
            case MIND:
                held = GauntelHelper.hasMindStone(entity);
                break;
            case TIME:
                held = GauntelHelper.hasTimeStone(entity);
                break;
            case SPACE:
                held = GauntelHelper.hasSpaceStone(entity);
                break;
            default:
                held = GauntelHelper.hasRealityStone(entity);
                break;
        }
        if (held) {
            return true;
        }
        EntityDestroyerArmor armor = EntityDestroyerArmor.carrier(entity);
        return armor != null && armor.holdsStone(stone.ordinal());
    }
}
