package com.matoon.eternity.compat;

import com.matoon.eternity.entity.ArmorPowers;
import com.matoon.eternity.entity.EntityDestroyerArmor;
import lucraft.mods.lucraftcore.infinity.EnumInfinityStone;
import lucraft.mods.lucraftcore.infinity.items.InventoryInfinityGauntlet;
import lucraft.mods.lucraftcore.infinity.items.ItemInfinityGauntlet;
import lucraft.mods.lucraftcore.infinity.items.ItemInfinityStone;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

/**
 * Reads which Infinity Stones an entity carries. Every stone from InfinityCraft and
 * HeroesExpansion (including the Tesseract) extends Lucraft's {@link ItemInfinityStone},
 * and stones socketed into a gauntlet live in its {@link InventoryInfinityGauntlet}. Once a
 * relic has been claimed, only its true copy counts; see {@link Relics}.
 */
public final class InfinityStones {

    public static final int ALL = (1 << EnumInfinityStone.values().length) - 1;

    private InfinityStones() {
    }

    public static int bit(EnumInfinityStone stone) {
        return 1 << stone.ordinal();
    }

    public static int count(int mask) {
        return Integer.bitCount(mask & ALL);
    }

    /** Bitmask of stone types the entity carries anywhere in its inventory. */
    public static int scan(Entity entity) {
        if (!(entity instanceof EntityPlayer)) {
            return 0;
        }
        EntityPlayer player = (EntityPlayer) entity;
        boolean remote = player.world.isRemote;
        int mask = 0;
        for (ItemStack stack : player.inventory.mainInventory) {
            mask |= of(stack, remote);
        }
        for (ItemStack stack : player.inventory.armorInventory) {
            mask |= of(stack, remote);
        }
        for (ItemStack stack : player.inventory.offHandInventory) {
            mask |= of(stack, remote);
        }
        // Stones set in the chest of the Destroyer Armor they are inside are as good as carried.
        if (player.getRidingEntity() instanceof EntityDestroyerArmor) {
            EntityDestroyerArmor armor = (EntityDestroyerArmor) player.getRidingEntity();
            for (int i = 0; i < ArmorPowers.COUNT; i++) {
                mask |= of(armor.getInventory().getStackInSlot(EntityDestroyerArmor.SLOT_STONES + i), remote);
            }
        }
        return mask | of(player.inventory.getItemStack(), remote);
    }

    public static int countHeld(Entity entity) {
        return count(scan(entity));
    }

    /** Whether the Power Stone is in either of someone's hands, on its own or set in a gauntlet. */
    public static boolean wieldsPower(EntityLivingBase entity) {
        int power = bit(EnumInfinityStone.POWER);
        boolean remote = entity.world.isRemote;
        return (of(entity.getHeldItemMainhand(), remote) & power) != 0 || (of(entity.getHeldItemOffhand(), remote) & power) != 0;
    }

    private static int of(ItemStack stack, boolean remote) {
        if (stack.isEmpty()) {
            return 0;
        }
        if (stack.getItem() instanceof ItemInfinityStone) {
            if (!Relics.recognized(stack, remote)) {
                return 0;
            }
            EnumInfinityStone type = ((ItemInfinityStone) stack.getItem()).getType();
            return type == null ? 0 : bit(type);
        }
        if (stack.getItem() instanceof ItemInfinityGauntlet && stack.hasTagCompound()) {
            InventoryInfinityGauntlet gauntlet = new InventoryInfinityGauntlet(stack);
            int mask = 0;
            for (int i = 0; i < gauntlet.getSizeInventory(); i++) {
                ItemStack socket = gauntlet.getStackInSlot(i);
                if (!socket.isEmpty() && socket.getItem() instanceof ItemInfinityStone && Relics.recognized(socket, remote)) {
                    EnumInfinityStone type = ((ItemInfinityStone) socket.getItem()).getType();
                    mask |= type == null ? 0 : bit(type);
                }
            }
            return mask;
        }
        return 0;
    }
}
