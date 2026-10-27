package com.matoon.eternity.client.gui;

import com.matoon.eternity.client.render.RenderDestroyer;
import com.matoon.eternity.entity.ArmorSkin;
import com.matoon.eternity.entity.ArmorUpgrade;
import com.matoon.eternity.inventory.ContainerDestroyerArmor;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

/** Its finishes: the armour itself turning slowly in the one picked, a swatch of each, and what the picked one takes to make. */
@SideOnly(Side.CLIENT)
class PaintPage extends DestroyerPage {

    private static ArmorSkin picked;
    private final int[] costY = new int[8];
    private int costCount;

    PaintPage(GuiDestroyerArmor gui) {
        super(gui);
    }

    @Override
    public String title() {
        return "Paint";
    }

    @Override
    public int icon() {
        return 5;
    }

    private int cardX(int i) {
        return left + 108 + (i % 3) * 57;
    }

    private int cardY(int i) {
        return top + 24 + (i / 3) * 43;
    }

    @Override
    public void draw(int mouseX, int mouseY) {
        int x = left, y = top;
        if (picked == null) {
            picked = armor.skin();
        }
        // The armour in the picked finish.
        inset(x + 6, y + 24, 98, 206);
        drawGradientRect(x + 7, y + 25, x + 103, y + 110, 0x1CFFFFFF, 0x00FFFFFF);
        drawGradientRect(x + 7, y + 160, x + 103, y + 229, 0x00000000, alpha(EMBER, 0.12F));
        int cx = x + 55, base = y + 206;
        RenderDestroyer.showAlways = true;
        RenderDestroyer.preview = picked;
        try {
            GuiInventory.drawEntityOnScreen(cx, base, 34, MathHelper.sin(time * 0.025F) * 90F, -12F, armor);
        } finally {
            RenderDestroyer.showAlways = false;
            RenderDestroyer.preview = null;
        }
        smallCentered(picked.title.toUpperCase(), cx, y + 214, armor.skin() == picked ? GOLD : PALE);
        String state = armor.skin() == picked ? "WORN" : armor.ownsSkin(picked) ? "MADE" : "NOT YET MADE";
        smallCentered(state, cx, y + 221, armor.skin() == picked ? EMBER : armor.ownsSkin(picked) ? GREEN : DIM);

        ArmorSkin[] all = ArmorSkin.values();
        for (int i = 0; i < all.length; i++) {
            card(all[i], cardX(i), cardY(i), inside(mouseX, mouseY, cardX(i), cardY(i), 54, 40));
        }

        // What the picked one is, and takes.
        int px = x + 108, py = y + 156;
        inset(px, py, 170, 74);
        int tx = px + 5, ty = py + 5;
        font.drawString(picked.title, tx, ty, GOLD);
        wrapSmall(picked.description, tx, ty + 10, 160, PALE);
        boolean owned = armor.ownsSkin(picked), worn = armor.skin() == picked;
        costCount = 0;
        if (!owned) {
            RenderHelper.enableGUIStandardItemLighting();
            for (int i = 0; i < picked.costs.length; i++) {
                ArmorUpgrade.Cost cost = picked.costs[i];
                int ix = tx + i * 19, iy = py + 33;
                int have = mc.player.capabilities.isCreativeMode ? cost.count : cost.carried(mc.player);
                drawRect(ix, iy, ix + 18, iy + 18, have >= cost.count ? 0xFF16301A : 0xFF3A1210);
                ItemStack icon = cost.icon();
                items.renderItemAndEffectIntoGUI(icon, ix + 1, iy + 1);
                items.renderItemOverlayIntoGUI(font, icon, ix + 1, iy + 1, String.valueOf(cost.count));
                costY[costCount++] = iy;
            }
            RenderHelper.disableStandardItemLighting();
        } else {
            small(worn ? "It is wearing this finish." : "Made. Wear it whenever you like.", tx, py + 38, worn ? EMBER : GREEN);
        }
        int bx = px + 4, by = py + 54, bw = 162;
        boolean can = owned ? !worn : ArmorUpgrade.affordable(mc.player, picked.costs);
        button(bx, by, bw, 16, worn ? "WORN" : owned ? "WEAR IT" : "MAKE AND WEAR IT", can, can && inside(mouseX, mouseY, bx, by, bw, 16), EMBER);
    }

    /** A swatch of a finish: banded plate in its colours, with its name under it. */
    private void card(ArmorSkin skin, int cx, int cy, boolean over) {
        boolean worn = armor.skin() == skin, owned = armor.ownsSkin(skin);
        int border = skin == picked ? 0xFFE8E0D0 : worn ? 0xFF000000 | GOLD : over ? SHINE : EDGE;
        drawRect(cx - 1, cy - 1, cx + 55, cy + 41, border);
        drawRect(cx, cy, cx + 54, cy + 40, 0xFF0A0A0C);
        int[] c = skin.swatch;
        for (int b = 0; b < 5; b++) {
            int y0 = cy + 2 + b * 5;
            int light = c[2], dark = c[0], lip = c[3];
            if (skin == ArmorSkin.BIFROST) {
                int hue = java.awt.Color.HSBtoRGB(0.78F - b * 0.18F, 0.72F, 0.95F) & 0xFFFFFF;
                light = hue;
                dark = blend(0xFF000000 | hue, 0xFF101010, 0.6F) & 0xFFFFFF;
                lip = blend(0xFF000000 | hue, 0xFFFFFFFF, 0.4F) & 0xFFFFFF;
            } else if (skin == ArmorSkin.WARPAINT && b % 2 == 1) {
                light = c[3];
                dark = c[2];
                lip = 0xF08070;
            } else if (skin == ArmorSkin.ROYAL || skin == ArmorSkin.CELESTIAL) {
                light = c[1];
                dark = c[0];
                lip = c[3];
            }
            drawGradientRect(cx + 2, y0, cx + 52, y0 + 4, 0xFF000000 | light, 0xFF000000 | dark);
            drawRect(cx + 2, y0, cx + 52, y0 + 1, 0xFF000000 | lip);
        }
        // What sets some apart: Muspelheim's cracks, the Celestial's stars.
        if (skin == ArmorSkin.MUSPEL) {
            int[][] crack = {{8, 4}, {9, 5}, {10, 6}, {11, 8}, {12, 9}, {12, 10}, {13, 12}, {14, 13}, {15, 14}, {30, 9}, {31, 10}, {32, 11}, {33, 13},
                    {34, 14}, {35, 16}, {36, 17}, {44, 20}, {45, 21}, {45, 22}, {46, 24}};
            for (int[] k : crack) {
                drawRect(cx + k[0], cy + k[1], cx + k[0] + 1, cy + k[1] + 1, 0xFF000000 | c[3]);
            }
        } else if (skin == ArmorSkin.CELESTIAL) {
            int[][] stars = {{7, 5}, {19, 13}, {33, 6}, {44, 18}, {26, 21}, {12, 23}, {48, 9}};
            for (int[] k : stars) {
                drawRect(cx + k[0], cy + k[1], cx + k[0] + 1, cy + k[1] + 1, 0xFF000000 | c[3]);
            }
        }
        if (!owned) {
            drawRect(cx, cy, cx + 54, cy + 28, 0x70000000);
            // A small padlock.
            drawRect(cx + 46, cy + 3, cx + 51, cy + 4, SHINE);
            drawRect(cx + 46, cy + 3, cx + 47, cy + 6, SHINE);
            drawRect(cx + 50, cy + 3, cx + 51, cy + 6, SHINE);
            drawRect(cx + 45, cy + 6, cx + 52, cy + 11, SHINE);
            drawRect(cx + 48, cy + 8, cx + 49, cy + 10, 0xFF0A0A0C);
        }
        String name = skin.title;
        if (font.getStringWidth(name) * 0.75F > 52F) {
            // Too long for its card: its last word says enough (Gold, Frost, Gunmetal).
            name = name.substring(name.lastIndexOf(' ') + 1);
        }
        smallCentered(name, cx + 27, cy + 31, worn ? GOLD : owned ? PALE : DIM);
    }

    @Override
    public boolean click(int mouseX, int mouseY, int button) {
        if (button != 0) {
            return false;
        }
        ArmorSkin[] all = ArmorSkin.values();
        for (int i = 0; i < all.length; i++) {
            if (inside(mouseX, mouseY, cardX(i), cardY(i), 54, 40)) {
                picked = all[i];
                gui.click();
                return true;
            }
        }
        if (picked != null && inside(mouseX, mouseY, left + 112, top + 210, 162, 16)) {
            boolean owned = armor.ownsSkin(picked);
            if (owned ? armor.skin() != picked : ArmorUpgrade.affordable(mc.player, picked.costs)) {
                gui.press(ContainerDestroyerArmor.PAINT + picked.ordinal());
                gui.click();
                return true;
            }
        }
        return false;
    }

    @Override
    public void hover(int mouseX, int mouseY, List<String> lines) {
        ArmorSkin[] all = ArmorSkin.values();
        for (int i = 0; i < all.length; i++) {
            if (inside(mouseX, mouseY, cardX(i), cardY(i), 54, 40)) {
                lines.add((armor.skin() == all[i] ? "§6" : "§f") + all[i].title);
                lines.add(armor.skin() == all[i] ? "§6Worn" : armor.ownsSkin(all[i]) ? "§aMade" : "§7Not yet made");
            }
        }
        for (int i = 0; picked != null && i < costCount; i++) {
            if (inside(mouseX, mouseY, left + 113 + i * 19, costY[i], 18, 18)) {
                ArmorUpgrade.Cost cost = picked.costs[i];
                int have = cost.carried(mc.player);
                lines.add("§f" + cost.name());
                lines.add((have >= cost.count ? "§a" : "§c") + Math.min(have, 999) + " / " + cost.count);
            }
        }
    }
}
