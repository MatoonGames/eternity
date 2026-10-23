package com.matoon.eternity.item;

import com.matoon.eternity.Eternity;
import com.matoon.eternity.client.RelicTrackerAngle;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A golden compass whose needle finds the nearest relic in your dimension, wherever it
 * lies: an Infinity Stone, the Tesseract, or an Astral Regulator. The needle itself is
 * driven client-side; see {@code client.RelicTrackerAngle}.
 */
public class ItemRelicTracker extends Item {

    public ItemRelicTracker() {
        setRegistryName(Eternity.MODID, "relic_tracker");
        setTranslationKey(Eternity.MODID + ".relic_tracker");
        setMaxStackSize(1);
        setCreativeTab(CreativeTabs.TOOLS);
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.EPIC;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(TextFormatting.DARK_PURPLE + "" + TextFormatting.ITALIC + "It always knows where they are.");
        String target = RelicTrackerAngle.targetName();
        if (target != null) {
            tooltip.add(TextFormatting.GRAY + I18n.format("item.eternity.relic_tracker.drawn", TextFormatting.AQUA + target));
        }
    }
}
