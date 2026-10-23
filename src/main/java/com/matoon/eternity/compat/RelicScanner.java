package com.matoon.eternity.compat;

import com.matoon.eternity.item.ItemAstralRegulator;
import lucraft.mods.lucraftcore.infinity.items.InventoryInfinityGauntlet;
import lucraft.mods.lucraftcore.infinity.items.ItemIndestructible;
import lucraft.mods.lucraftcore.infinity.items.ItemInfinityGauntlet;
import lucraft.mods.lucraftcore.infinity.items.ItemInfinityStone;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Finds relics inside an item stack: the stack itself, a gauntlet's sockets, or the
 * stored inventory of an item like a shulker box (which may in turn hold a gauntlet).
 */
public final class RelicScanner {

    private RelicScanner() {
    }

    /** Every Infinity Stone the mod recognises (see {@link Relics}), and every Astral Regulator. */
    public static void forEachRelic(ItemStack stack, Consumer<ItemStack> found) {
        visit(stack, s -> s.getItem() instanceof ItemInfinityStone && Relics.recognized(s, false)
                || s.getItem() instanceof ItemAstralRegulator, found, 0);
    }

    /** Whether the stack is, or holds, anything that must never be destroyed. */
    public static boolean containsIndestructible(ItemStack stack) {
        boolean[] any = {false};
        visit(stack, s -> s.getItem() instanceof ItemIndestructible, s -> any[0] = true, 0);
        return any[0];
    }

    private static void visit(ItemStack stack, Predicate<ItemStack> match, Consumer<ItemStack> found, int depth) {
        if (stack.isEmpty() || depth > 3) {
            return;
        }
        if (match.test(stack)) {
            found.accept(stack);
            return;
        }
        if (stack.getItem() instanceof ItemInfinityGauntlet) {
            if (stack.hasTagCompound()) {
                InventoryInfinityGauntlet gauntlet = new InventoryInfinityGauntlet(stack);
                for (int i = 0; i < gauntlet.getSizeInventory(); i++) {
                    visit(gauntlet.getStackInSlot(i), match, found, depth + 1);
                }
            }
            return;
        }
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null && tag.hasKey("BlockEntityTag", Constants.NBT.TAG_COMPOUND)) {
            NBTTagList items = tag.getCompoundTag("BlockEntityTag").getTagList("Items", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < items.tagCount(); i++) {
                visit(new ItemStack(items.getCompoundTagAt(i)), match, found, depth + 1);
            }
        }
    }
}
