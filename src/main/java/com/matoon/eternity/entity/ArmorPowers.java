package com.matoon.eternity.entity;

import com.matoon.eternity.compat.StoneAbilities;
import lucraft.mods.lucraftcore.infinity.EnumInfinityStone;
import lucraft.mods.lucraftcore.infinity.items.ItemInfinityStone;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The Infinity Stones, set in the Destroyer Armor's chest. Each lends the armour a gift of its own, always; and each
 * lends whoever is inside it that stone's own powers, the very ones it would lend set in a gauntlet (see
 * {@link StoneAbilities}). Indexed as Lucraft's {@link EnumInfinityStone}.
 */
public final class ArmorPowers {

    public static final int SPACE = 0, SOUL = 1, REALITY = 2, TIME = 3, POWER = 4, MIND = 5, COUNT = 6;

    /** {@link com.matoon.eternity.network.MsgTribunalFx#DESTROYER_STONE} kind: the ray carrying the stones' light. */
    public static final int FX_RAY = 0;

    private static final String[] NAMES = {"Space Stone", "Soul Stone", "Reality Stone", "Time Stone", "Power Stone", "Mind Stone"};
    private static final String[] PASSIVE = {
            "Its fists reach further.",
            "Every life it takes mends it a little.",
            "Its blows ignore armour.",
            "Its furnace cools faster.",
            "Everything it strikes lands half again as hard.",
            "Hostile creatures near it lose their strength."};
    private static final TextFormatting[] COLORS = {TextFormatting.BLUE, TextFormatting.GOLD, TextFormatting.RED, TextFormatting.GREEN,
            TextFormatting.DARK_PURPLE, TextFormatting.YELLOW};
    /** Each stone's light, as RGB. */
    public static final int[] RGB = {0x3F7BFF, 0xFF8A1E, 0xFF2A2A, 0x2EE86A, 0xB040FF, 0xFFE03A};

    private ArmorPowers() {
    }

    public static String name(int stone) {
        return NAMES[stone];
    }

    public static String passive(int stone) {
        return PASSIVE[stone];
    }

    public static TextFormatting color(int stone) {
        return COLORS[stone];
    }

    /** Which stone an item is, by index, or -1 if it is none. */
    public static int stoneOf(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemInfinityStone)) {
            return -1;
        }
        EnumInfinityStone type = ((ItemInfinityStone) stack.getItem()).getType();
        return type == null ? -1 : type.ordinal();
    }

    /** How much further its fists reach for the Space Stone. */
    static double reach(EntityDestroyerArmor armor) {
        return armor.holdsStone(SPACE) ? (armor.has(ArmorUpgrade.CONDUITS) ? 3D : 1.5D) : 0D;
    }

    /** How much faster its furnace cools for the Time Stone. */
    static float cooling(EntityDestroyerArmor armor) {
        return armor.holdsStone(TIME) ? (armor.has(ArmorUpgrade.CONDUITS) ? 2F : 1.5F) : 1F;
    }

    /** How much of itself it mends for each life the Soul Stone takes. */
    static float harvest(EntityDestroyerArmor armor) {
        return armor.has(ArmorUpgrade.CONDUITS) ? 0.1F : 0.05F;
    }

    /** Every second: the Mind Stone sapping whatever hostile thing comes near it. */
    static void tick(EntityDestroyerArmor armor, @Nullable EntityPlayer pilot) {
        if (!armor.holdsStone(MIND) || armor.ticksExisted % 20 != 0) {
            return;
        }
        boolean conduits = armor.has(ArmorUpgrade.CONDUITS);
        double reach = conduits ? 16D : 10D;
        List<EntityLivingBase> near = armor.world.getEntitiesWithinAABB(EntityLivingBase.class, armor.getEntityBoundingBox().grow(reach),
                e -> e != armor && e != pilot && e instanceof IMob && e.isEntityAlive() && e.getDistanceSq(armor) <= reach * reach);
        for (EntityLivingBase mob : near) {
            mob.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, 40, conduits ? 2 : 1, false, true));
        }
    }
}
