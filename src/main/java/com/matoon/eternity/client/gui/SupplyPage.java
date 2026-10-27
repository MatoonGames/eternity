package com.matoon.eternity.client.gui;

import com.matoon.eternity.EternityConfig;
import com.matoon.eternity.entity.EntityDestroyerArmor;
import com.matoon.eternity.entity.EntityDestroyerArmor.Condition;
import com.matoon.eternity.item.ItemAstralRegulator;
import lucraft.mods.lucraftcore.utilities.items.UtilitiesItems;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.OreDictionary;

import java.util.List;

/**
 * Keeping it going: what feeds it (lava, or with its dynamo anything holding Forge Energy), set aside once spent, and
 * Uru plate to mend it.
 */
@SideOnly(Side.CLIENT)
class SupplyPage extends DestroyerPage {

    private static final int FEED_X = 18, SPENT_X = 72, SLOT_Y = 44, URU_X = 160;

    SupplyPage(GuiDestroyerArmor gui) {
        super(gui);
    }

    @Override
    public String title() {
        return "Supply";
    }

    @Override
    public int icon() {
        return 1;
    }

    @Override
    public boolean showsInventory() {
        return true;
    }

    @Override
    public int[] slot(int index) {
        switch (index) {
            case EntityDestroyerArmor.SLOT_FUEL:
                return new int[]{FEED_X + 1, SLOT_Y + 1};
            case EntityDestroyerArmor.SLOT_SPENT:
                return new int[]{SPENT_X + 1, SLOT_Y + 1};
            case EntityDestroyerArmor.SLOT_URU:
                return new int[]{URU_X + 1, SLOT_Y + 1};
            default:
                return null;
        }
    }

    @Override
    public void draw(int mouseX, int mouseY) {
        int x = left, y = top;
        boolean electric = armor.electric();
        // Feeding it.
        panel(x + 6, y + 24, 136, 114, electric ? "ENERGY FEED" : "LAVA FEED");
        boolean empty = armor.getInventory().getStackInSlot(EntityDestroyerArmor.SLOT_FUEL).isEmpty();
        slotFrame(x + FEED_X, y + SLOT_Y, empty ? (electric ? battery() : new ItemStack(Items.LAVA_BUCKET)) : null);
        arrow(x + FEED_X + 26, y + SLOT_Y + 5, empty ? EDGE : 0xFF000000 | (electric ? ENERGY : EMBER));
        slotFrame(x + SPENT_X, y + SLOT_Y, null);
        smallCentered("IN", x + FEED_X + 9, y + SLOT_Y + 21, DIM);
        smallCentered("SPENT", x + SPENT_X + 9, y + SLOT_Y + 21, DIM);
        int ty = wrapSmall(electric ? "Batteries, cells, anything holding Forge Energy. The Regulator and stones in its chest feed it too."
                : "Buckets, tanks, anything holding lava; or wade it through lava.", x + 12, y + 76, 124, PALE);
        float fuel = armor.getLava() / armor.lavaCapacity();
        int by = y + 116;
        if (electric) {
            label("STORED", x + 12, by, 124, ItemAstralRegulator.formatEnergy(armor.energy()) + " / " + ItemAstralRegulator.formatEnergy(armor.energyCapacity()),
                    armor.cold() ? RED : ENERGY);
            energyBar(x + 12, by + 8, 124, 5, fuel);
            label("FED", x + 12, by - 10, 124, ItemAstralRegulator.formatEnergy(armor.supply()) + " FE/t", armor.supply() > 0 ? GREEN : DIM);
        } else {
            label("STORED", x + 12, by, 124, String.format("%.1f / %.0f buckets", armor.getLava() / EntityDestroyerArmor.BUCKET,
                    armor.lavaCapacity() / EntityDestroyerArmor.BUCKET), armor.cold() ? RED : GOLD);
            bar(x + 12, by + 8, 124, 5, fuel, 0xA8380C, 0xFFB040);
        }

        // Mending it.
        panel(x + 146, y + 24, 132, 114, "MENDING");
        slotFrame(x + URU_X, y + SLOT_Y, armor.getInventory().getStackInSlot(EntityDestroyerArmor.SLOT_URU).isEmpty() ? uruPlate() : null);
        smallCentered("URU PLATE", x + URU_X + 9, y + SLOT_Y + 21, DIM);
        float missing = (armor.getMaxHealth() - armor.getHealth()) / (float) EternityConfig.vault.armorHealth;
        int plates = (int) Math.ceil(missing / EntityDestroyerArmor.PLATE - 0.01F);
        int tx = x + URU_X + 24;
        small(plates > 0 ? "It needs" : "Fully", tx, y + SLOT_Y + 2, PALE);
        small(plates > 0 ? plates + " plate" + (plates == 1 ? "" : "s") : "mended", tx, y + SLOT_Y + 10, plates > 0 ? AMBER : GREEN);
        wrapSmall("Lucraft's Uru plate, one at a time, each mending a tenth of its frame. Uru in your hand mends it too, while it stands empty.",
                x + 152, y + 76, 120, PALE);
        Condition condition = armor.condition();
        label("INTEGRITY", x + 152, by, 120, String.format("%.0f%%", armor.getHealth() / armor.getMaxHealth() * 100F), condition.color);
        bar(x + 152, by + 8, 120, 5, armor.getHealth() / armor.getMaxHealth(), condition.color & 0xFFFFFF, lighten(condition.color, 0.3F) & 0xFFFFFF);
    }

    @Override
    public void hover(int mouseX, int mouseY, List<String> lines) {
        if (inside(mouseX, mouseY, left + 6, top + 104, 136, 34) && armor.electric()) {
            lines.add("§6Forge Energy");
            lines.add(String.format("§7Stored %d / %d FE", armor.energy(), armor.energyCapacity()));
            lines.add("§7Fed up to " + ItemAstralRegulator.formatEnergy(armor.supply()) + " FE a tick");
            if (armor.canOvercharge() && armor.surgeTotal() > 0) {
                lines.add("§cOvercharge draws " + ItemAstralRegulator.formatEnergy(armor.surgeDraw()) + " FE a tick");
            }
        }
    }

    static ItemStack uruPlate() {
        List<ItemStack> ores = OreDictionary.getOres("plateUru");
        if (ores.isEmpty()) {
            return new ItemStack(Items.IRON_INGOT);
        }
        ItemStack stack = ores.get(0).copy();
        if (stack.getMetadata() == OreDictionary.WILDCARD_VALUE) {
            stack.setItemDamage(0);
        }
        return stack;
    }

    /** Something that holds Forge Energy, to show its feed takes it: one of Lucraft's capacitors. */
    private static ItemStack battery() {
        return UtilitiesItems.LV_CAPACITOR != null ? new ItemStack(UtilitiesItems.LV_CAPACITOR) : new ItemStack(Items.REDSTONE);
    }
}
