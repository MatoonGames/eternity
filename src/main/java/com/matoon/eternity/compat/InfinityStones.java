package com.matoon.eternity.compat;

import lucraft.mods.lucraftcore.infinity.EnumInfinityStone;
import lucraft.mods.lucraftcore.infinity.items.InventoryInfinityGauntlet;
import lucraft.mods.lucraftcore.infinity.items.ItemInfinityGauntlet;
import lucraft.mods.lucraftcore.infinity.items.ItemInfinityStone;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

/**
 * Reads which Infinity Stones an entity carries. Every stone from InfinityCraft and
 * HeroesExpansion (including the Tesseract) extends Lucraft's {@link ItemInfinityStone},
 * and stones socketed into a gauntlet live in its {@link InventoryInfinityGauntlet}.
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
        int mask = 0;
        for (ItemStack stack : player.inventory.mainInventory) {
            mask |= of(stack);
        }
        for (ItemStack stack : player.inventory.armorInventory) {
            mask |= of(stack);
        }
        for (ItemStack stack : player.inventory.offHandInventory) {
            mask |= of(stack);
        }
        return mask | of(player.inventory.getItemStack());
    }

    public static int countHeld(Entity entity) {
        return count(scan(entity));
    }

    private static int of(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        if (stack.getItem() instanceof ItemInfinityStone) {
            EnumInfinityStone type = ((ItemInfinityStone) stack.getItem()).getType();
            return type == null ? 0 : bit(type);
        }
        if (stack.getItem() instanceof ItemInfinityGauntlet && stack.hasTagCompound()) {
            InventoryInfinityGauntlet gauntlet = new InventoryInfinityGauntlet(stack);
            int mask = 0;
            for (int i = 0; i < gauntlet.getSizeInventory(); i++) {
                ItemStack socket = gauntlet.getStackInSlot(i);
                if (!socket.isEmpty() && socket.getItem() instanceof ItemInfinityStone) {
                    EnumInfinityStone type = ((ItemInfinityStone) socket.getItem()).getType();
                    mask |= type == null ? 0 : bit(type);
                }
            }
            return mask;
        }
        return 0;
    }
}
