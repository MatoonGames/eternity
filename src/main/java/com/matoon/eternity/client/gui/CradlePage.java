package com.matoon.eternity.client.gui;

import com.matoon.eternity.compat.StoneAbilities;
import com.matoon.eternity.entity.ArmorPowers;
import com.matoon.eternity.entity.ArmorUpgrade;
import com.matoon.eternity.entity.EntityDestroyerArmor;
import lucraft.mods.lucraftcore.infinity.items.ItemInfinityStone;
import lucraft.mods.lucraftcore.superpowers.abilities.Ability;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The cavity in its chest, the Astral Regulator in it and the six stone sockets arched over it; for each stone, the
 * gift it lends the armour and the powers it lends you.
 */
@SideOnly(Side.CLIENT)
class CradlePage extends DestroyerPage {

    /** Where the Regulator hangs in the cavity, and the arch of sockets over it, against the panel's corner. */
    private static final int CAVITY_X = 78, CAVITY_Y = 106, ARCH = 46;
    /** Which the right-hand panel tells of: a stone by index, or the Regulator. Whatever the mouse was last over. */
    private static int told = ArmorPowers.POWER;
    private static final int REGULATOR = -1;
    /** The names of the powers each kind of stone lends, worked out once. */
    private static final Map<Item, List<String>> POWERS = new HashMap<>();

    CradlePage(GuiDestroyerArmor gui) {
        super(gui);
    }

    @Override
    public String title() {
        return "Cradle";
    }

    @Override
    public int icon() {
        return 3;
    }

    @Override
    public String locked() {
        return armor.has(ArmorUpgrade.CRADLE) ? null : "Forge the Cradle (Infinity line) to set stones in its chest.";
    }

    @Override
    public boolean showsInventory() {
        return true;
    }

    /** The middle of a stone's socket, arched over the top of the cavity from left to right. */
    private static int[] socketAt(int stone) {
        double a = Math.toRadians(165D - stone * 30D);
        return new int[]{CAVITY_X + (int) Math.round(Math.cos(a) * ARCH), CAVITY_Y - 4 - (int) Math.round(Math.sin(a) * ARCH)};
    }

    @Override
    public int[] slot(int index) {
        if (index == EntityDestroyerArmor.SLOT_REGULATOR) {
            return new int[]{CAVITY_X - 8, CAVITY_Y - 8};
        }
        if (index >= EntityDestroyerArmor.SLOT_STONES && index < EntityDestroyerArmor.SLOT_STONES + ArmorPowers.COUNT) {
            int[] at = socketAt(index - EntityDestroyerArmor.SLOT_STONES);
            return new int[]{at[0] - 8, at[1] - 8};
        }
        return null;
    }

    @Override
    public void draw(int mouseX, int mouseY) {
        int x = left, y = top;
        boolean built = armor.has(ArmorUpgrade.CRADLE);
        inset(x + 6, y + 24, 142, 116);
        // The cavity in its chest: a deep frame, the furnace glowing through the ribs at the back of it.
        int cx = x + CAVITY_X, cy = y + CAVITY_Y;
        float heat = 0.35F + 0.25F * armor.heat(1F) + 0.08F * MathHelper.sin(time * 0.2F);
        drawRect(cx - 19, cy - 19, cx + 19, cy + 21, VOID);
        drawRect(cx - 18, cy - 18, cx + 18, cy + 20, RIM);
        drawGradientRect(cx - 16, cy - 16, cx + 16, cy + 18, 0xFF0A0706, 0xFF140A06);
        for (int rib = cy - 13; rib < cy + 17; rib += 4) {
            drawRect(cx - 14, rib, cx + 14, rib + 1, built ? alpha(EMBER, heat) : 0xFF1A1310);
        }
        if (built && !armor.regulator().isEmpty()) {
            float pulse = 0.25F + 0.15F * MathHelper.sin(time * 0.12F);
            drawRect(cx - 11, cy - 11, cx + 11, cy + 11, alpha(VIOLET, pulse));
        }
        socket(cx - 9, cy - 9);
        // The arch of sockets over it, joined by a band of old gold, each stone set there burning in its colour.
        for (int i = 0; i < ArmorPowers.COUNT - 1; i++) {
            int[] a = socketAt(i), b = socketAt(i + 1);
            line(x + a[0], y + a[1], x + b[0], y + b[1], built ? 0xFF8A6A3A : EDGE);
        }
        for (int i = 0; i < ArmorPowers.COUNT; i++) {
            int[] at = socketAt(i);
            int sx = x + at[0], sy = y + at[1];
            int rgb = ArmorPowers.RGB[i];
            if (armor.holdsStone(i)) {
                float glow = 0.35F + 0.15F * MathHelper.sin(time * 0.2F + i);
                drawRect(sx - 11, sy - 11, sx + 11, sy + 11, alpha(rgb, glow));
            }
            drawRect(sx - 10, sy - 10, sx + 10, sy + 10, built ? alpha(rgb, armor.holdsStone(i) ? 0.9F : 0.35F) : EDGE);
            socket(sx - 9, sy - 9);
            if (inside(mouseX, mouseY, sx - 9, sy - 9, 18, 18)) {
                told = i;
            }
        }
        if (inside(mouseX, mouseY, cx - 9, cy - 9, 18, 18)) {
            told = REGULATOR;
        }
        smallCentered("ASTRAL REGULATOR", cx, y + 131, built ? VIOLET : DIM);
        if (!built) {
            lockedOver(x + 7, y + 25, 140, 114, "Forge the Cradle on the FORGE page (the Infinity line) to set the Astral Regulator and the stones in its chest.");
        }

        // Whatever the mouse is over: a stone, what it lends the armour and what it lends you; or the Regulator.
        inset(x + 152, y + 24, 126, 116);
        int tx = x + 157, ty = y + 29, tw = 116;
        if (told == REGULATOR) {
            font.drawString("ASTRAL REGULATOR", tx, ty, built && !armor.regulator().isEmpty() ? VIOLET : DIM);
            ty += 11;
            ty = wrapSmall("It hangs in the cavity in its chest, under the arch of the stones.", tx, ty, tw, PALE) + 3;
            small("WITH ITS DYNAMO:", tx, ty, GOLD);
            ty += 8;
            ty = wrapSmall("It never runs dry: whatever it has drunk in pours through it as Forge Energy, more the more it holds.", tx, ty, tw,
                    armor.electric() ? PALE : DIM) + 3;
            if (!armor.electric()) {
                wrapSmall("Forge the Dynamo to run on it.", tx, ty, tw, DIM);
            }
        } else {
            int stone = told;
            int rgb = ArmorPowers.RGB[stone];
            boolean held = armor.holdsStone(stone);
            font.drawString(ArmorPowers.name(stone).toUpperCase(), tx, ty, built && held ? rgb : DIM);
            ty += 11;
            small("ITS GIFT TO THE ARMOUR:", tx, ty, GOLD);
            ty += 8;
            ty = wrapSmall(ArmorPowers.passive(stone), tx, ty, tw, PALE) + 3;
            small("ITS POWERS, LENT TO YOU:", tx, ty, GOLD);
            ty += 8;
            List<String> powers = powers(held ? armor.stone(stone) : ItemStack.EMPTY, stone);
            for (int i = 0; i < powers.size() && ty < y + 110; i++) {
                small("- " + powers.get(i), tx, ty, held ? rgb : DIM);
                ty += 7;
            }
            if (armor.electric()) {
                small("It powers the dynamo too.", tx, Math.min(ty + 1, y + 112), DIM);
            }
        }
        drawRect(tx, y + 120, tx + tw, y + 121, EDGE);
        small(armor.stones() != 0 ? "On your ability bar inside;" : "Set a stone in a socket", tx, y + 125, armor.stones() != 0 ? EMBER : DIM);
        if (armor.stones() != 0) {
            small("the MOUSE WHEEL pages them", tx, y + 132, EMBER);
        }
    }

    /** The names of the Lucraft powers a stone lends, as it would in a gauntlet; any stone of its kind, if none is set. */
    private List<String> powers(ItemStack stack, int stone) {
        Item item = stack.isEmpty() ? null : stack.getItem();
        if (item == null) {
            for (Item each : Item.REGISTRY) {
                if (each instanceof ItemInfinityStone && !((ItemInfinityStone) each).isContainer() && ((ItemInfinityStone) each).getType() != null
                        && ((ItemInfinityStone) each).getType().ordinal() == stone) {
                    item = each;
                    break;
                }
            }
        }
        if (!(item instanceof ItemInfinityStone)) {
            return Collections.emptyList();
        }
        return POWERS.computeIfAbsent(item, kind -> {
            List<String> names = new ArrayList<>();
            try {
                for (Map.Entry<String, Ability> power : ((ItemInfinityStone) kind).addStoneAbilities(mc.player, new Ability.AbilityMap(), StoneAbilities.CONTEXT).entrySet()) {
                    String name = TextFormatting.getTextWithoutFormattingCodes(power.getValue().getDisplayName());
                    // Not every addon names its abilities: then its own key says it well enough.
                    if (name == null || name.endsWith(".name")) {
                        String key = power.getKey().replace('_', ' ');
                        name = key.isEmpty() ? "?" : Character.toUpperCase(key.charAt(0)) + key.substring(1);
                    }
                    names.add(name);
                }
            } catch (RuntimeException ignored) {
                // A stone that cannot say what it lends outside of a gauntlet still lends it.
            }
            return names;
        });
    }

    /** A line two pixels thick between two points, in steps. */
    private void line(int x0, int y0, int x1, int y1, int color) {
        int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        for (int i = 0; i <= steps; i++) {
            int px = x0 + (x1 - x0) * i / Math.max(1, steps), py = y0 + (y1 - y0) * i / Math.max(1, steps);
            drawRect(px, py, px + 2, py + 2, color);
        }
    }
}
