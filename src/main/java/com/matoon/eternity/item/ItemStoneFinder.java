package com.matoon.eternity.item;

import com.matoon.eternity.Eternity;
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
 * A golden compass whose needle finds the nearest Infinity Stone in your dimension,
 * wherever it lies. The needle itself is driven client-side; see
 * {@code client.StoneFinderAngle}.
 */
public class ItemStoneFinder extends Item {

    public ItemStoneFinder() {
        setRegistryName(Eternity.MODID, "stone_finder");
        setTranslationKey(Eternity.MODID + ".stone_finder");
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
    }
}
