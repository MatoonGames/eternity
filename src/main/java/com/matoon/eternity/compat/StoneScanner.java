package com.matoon.eternity.compat;

import lucraft.mods.lucraftcore.infinity.items.InventoryInfinityGauntlet;
import lucraft.mods.lucraftcore.infinity.items.ItemInfinityGauntlet;
import lucraft.mods.lucraftcore.infinity.items.ItemInfinityStone;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import java.util.function.Consumer;

/**
 * Finds Infinity Stones inside an item stack: the stack itself, a gauntlet's
 * sockets, or the stored inventory of an item like a shulker box (which may in turn
 * hold a gauntlet).
 */
public final class StoneScanner {

    private StoneScanner() {
    }

    public static void forEachStone(ItemStack stack, Consumer<ItemStack> found) {
        visit(stack, found, 0);
    }

    private static void visit(ItemStack stack, Consumer<ItemStack> found, int depth) {
        if (stack.isEmpty() || depth > 3) {
            return;
        }
        if (stack.getItem() instanceof ItemInfinityStone) {
            found.accept(stack);
            return;
        }
        if (stack.getItem() instanceof ItemInfinityGauntlet) {
            if (stack.hasTagCompound()) {
                InventoryInfinityGauntlet gauntlet = new InventoryInfinityGauntlet(stack);
                for (int i = 0; i < gauntlet.getSizeInventory(); i++) {
                    visit(gauntlet.getStackInSlot(i), found, depth + 1);
                }
            }
            return;
        }
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null && tag.hasKey("BlockEntityTag", Constants.NBT.TAG_COMPOUND)) {
            NBTTagList items = tag.getCompoundTag("BlockEntityTag").getTagList("Items", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < items.tagCount(); i++) {
                visit(new ItemStack(items.getCompoundTagAt(i)), found, depth + 1);
            }
        }
    }
}
