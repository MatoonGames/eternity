package com.matoon.eternity.inventory;

import com.matoon.eternity.entity.ArmorPowers;
import com.matoon.eternity.entity.ArmorSkin;
import com.matoon.eternity.entity.ArmorUpgrade;
import com.matoon.eternity.entity.EntityDestroyerArmor;
import com.matoon.eternity.entity.Overcharge;
import com.matoon.eternity.item.ItemAstralRegulator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

/**
 * The Destroyer Armor's own slots beside the player's: what feeds it going in (lava, or with its dynamo anything
 * holding Forge Energy) and what is spent of it coming out, Uru plate to mend it, the six sockets of its cradle, one for
 * each stone, and the Astral Regulator in its chest. Upgrades are forged and finishes made through {@link #enchantItem},
 * the way vanilla's enchanting table takes a button press.
 */
public class ContainerDestroyerArmor extends Container {

    /** Button ids: forge upgrade n, or make or put on finish n. */
    public static final int PAINT = 200;
    /**
     * Button ids for its overcharge: {@code SURGE + (part + 1) * 8 + step}, part -1 for every part, step an index
     * into {@link #SURGE_STEPS}.
     */
    public static final int SURGE = 400;
    public static final int[] SURGE_STEPS = {-1, 1, -10, 10, -100, 100, EntityDestroyerArmor.CUT};

    public static int surgeButton(int part, int step) {
        return SURGE + (part + 1) * 8 + step;
    }
    /** Where the player's own inventory starts among the slots. */
    public static final int PLAYER = EntityDestroyerArmor.SLOTS;

    public final EntityDestroyerArmor armor;

    public ContainerDestroyerArmor(InventoryPlayer playerInventory, EntityDestroyerArmor armor) {
        this.armor = armor;
        IInventory inv = armor.getInventory();
        addSlotToContainer(new Slot(inv, EntityDestroyerArmor.SLOT_FUEL, 0, 0) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return feeds(stack);
            }
        });
        addSlotToContainer(new Slot(inv, EntityDestroyerArmor.SLOT_SPENT, 0, 0) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return false;
            }
        });
        addSlotToContainer(new Slot(inv, EntityDestroyerArmor.SLOT_URU, 0, 0) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return EntityDestroyerArmor.mending(stack) > 0F;
            }
        });
        for (int i = 0; i < ArmorPowers.COUNT; i++) {
            final int stone = i;
            addSlotToContainer(new Slot(inv, EntityDestroyerArmor.SLOT_STONES + i, 0, 0) {
                @Override
                public boolean isItemValid(ItemStack stack) {
                    return armor.has(ArmorUpgrade.CRADLE) && ArmorPowers.stoneOf(stack) == stone;
                }

                @Override
                public int getSlotStackLimit() {
                    return 1;
                }
            });
        }
        addSlotToContainer(new Slot(inv, EntityDestroyerArmor.SLOT_REGULATOR, 0, 0) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return armor.has(ArmorUpgrade.CRADLE) && stack.getItem() instanceof ItemAstralRegulator;
            }

            @Override
            public int getSlotStackLimit() {
                return 1;
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(new Slot(playerInventory, col + row * 9 + 9, 0, 0));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlotToContainer(new Slot(playerInventory, col, 0, 0));
        }
    }

    /** What its feed takes: lava, or with its dynamo anything Forge Energy can be drawn from. */
    private boolean feeds(ItemStack stack) {
        if (armor.electric()) {
            IEnergyStorage cell = stack.getCapability(CapabilityEnergy.ENERGY, null);
            return cell != null && cell.canExtract();
        }
        return EntityDestroyerArmor.lavaIn(stack) > 0F;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return armor.reachableBy(player);
    }

    @Override
    public boolean enchantItem(EntityPlayer player, int id) {
        ArmorUpgrade[] upgrades = ArmorUpgrade.values();
        if (id >= 0 && id < upgrades.length) {
            boolean forged = armor.forge(player, upgrades[id]);
            if (forged) {
                detectAndSendChanges();
            }
            return forged;
        }
        if (id >= SURGE && id < SURGE + (Overcharge.values().length + 1) * 8 && (id - SURGE) % 8 < SURGE_STEPS.length) {
            int part = (id - SURGE) / 8 - 1;
            armor.surge(player, part < 0 ? null : Overcharge.values()[part], SURGE_STEPS[(id - SURGE) % 8]);
            return true;
        }
        if (id >= PAINT && id < PAINT + ArmorSkin.values().length) {
            boolean painted = armor.paint(player, ArmorSkin.values()[id - PAINT]);
            if (painted) {
                detectAndSendChanges();
            }
            return painted;
        }
        return false;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        Slot slot = inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getStack();
        ItemStack copy = stack.copy();
        int end = inventorySlots.size();
        if (index < PLAYER) {
            if (!mergeItemStack(stack, PLAYER, end, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            int stone = ArmorPowers.stoneOf(stack);
            boolean moved;
            if (stack.getItem() instanceof ItemAstralRegulator && armor.has(ArmorUpgrade.CRADLE) && !getSlot(EntityDestroyerArmor.SLOT_REGULATOR).getHasStack()) {
                moved = mergeItemStack(stack, EntityDestroyerArmor.SLOT_REGULATOR, EntityDestroyerArmor.SLOT_REGULATOR + 1, false);
            } else if (feeds(stack)) {
                moved = mergeItemStack(stack, EntityDestroyerArmor.SLOT_FUEL, EntityDestroyerArmor.SLOT_FUEL + 1, false);
            } else if (EntityDestroyerArmor.mending(stack) > 0F) {
                moved = mergeItemStack(stack, EntityDestroyerArmor.SLOT_URU, EntityDestroyerArmor.SLOT_URU + 1, false);
            } else if (stone >= 0 && armor.has(ArmorUpgrade.CRADLE) && !getSlot(EntityDestroyerArmor.SLOT_STONES + stone).getHasStack()) {
                moved = mergeItemStack(stack, EntityDestroyerArmor.SLOT_STONES + stone, EntityDestroyerArmor.SLOT_STONES + stone + 1, false);
            } else if (index < PLAYER + 27) {
                moved = mergeItemStack(stack, PLAYER + 27, end, false);
            } else {
                moved = mergeItemStack(stack, PLAYER, PLAYER + 27, false);
            }
            if (!moved) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) {
            slot.putStack(ItemStack.EMPTY);
        } else {
            slot.onSlotChanged();
        }
        if (stack.getCount() == copy.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return copy;
    }
}
