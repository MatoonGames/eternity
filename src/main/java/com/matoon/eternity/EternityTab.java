package com.matoon.eternity;

import com.matoon.eternity.client.gui.EternityTabArt;
import com.matoon.eternity.item.ItemAstralRegulator;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Everything of the mod's that can be carried, in one place, under the fullest Regulator there is. Its
 * panel is a living window onto space ({@link EternityTabArt}); its title, its aura and its scroll knob
 * are drawn over the screen by {@link com.matoon.eternity.client.gui.EternityTabFx}.
 */
public final class EternityTab extends CreativeTabs {

    public static final EternityTab INSTANCE = new EternityTab();

    private EternityTab() {
        super(Eternity.MODID);
        // The title is written over the panel in its own colours, not in vanilla's grey.
        setNoTitle();
    }

    @SideOnly(Side.CLIENT)
    @Override
    public ItemStack createIcon() {
        return ItemAstralRegulator.fullest();
    }

    @SideOnly(Side.CLIENT)
    @Override
    public ResourceLocation getBackgroundImage() {
        return EternityTabArt.panel();
    }
}
