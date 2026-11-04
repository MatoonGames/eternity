package com.matoon.eternity.stones;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * How far snapping has worn an Infinity Gauntlet, kept on the gauntlet itself: every snap scorches and cracks it a stage
 * further, and destroying the stones melts it at once. Metal does not heal.
 * <ol start="0">
 * <li>As made.</li>
 * <li>Scorched.</li>
 * <li>Charred.</li>
 * <li>Cracked.</li>
 * <li>Fused: melted, run together, and into the hand inside it.</li>
 * </ol>
 */
public final class GauntletWear {

    public static final String TAG = "EternityWear";
    public static final int FUSED = 4;
    private static final String[] NAMES = {"", "Scorched", "Charred", "Cracked", "Fused"};

    private GauntletWear() {
    }

    public static int stage(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag == null ? 0 : Math.max(0, Math.min(FUSED, tag.getInteger(TAG)));
    }

    public static String name(int stage) {
        return NAMES[Math.max(0, Math.min(FUSED, stage))];
    }

    /** One snap's wear, or all of it at once. */
    static void wear(ItemStack stack, boolean melt) {
        if (stack.isEmpty()) {
            return;
        }
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        stack.getTagCompound().setInteger(TAG, melt ? FUSED : Math.min(FUSED, stage(stack) + 1));
    }
}
