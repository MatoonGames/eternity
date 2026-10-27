package com.matoon.eternity.client.gui;

import com.matoon.eternity.entity.ArmorUpgrade;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

/**
 * Its upgrades: six lines of them, each forged in order out of what you carry. Pick one to see what it does and what
 * it costs.
 */
@SideOnly(Side.CLIENT)
class ForgePage extends DestroyerPage {

    private static ArmorUpgrade chosen = ArmorUpgrade.URU_PLATING;
    /** Where the chosen upgrade's costs were last drawn, for their tooltips. */
    private final int[] costY = new int[8];
    private int costCount;

    ForgePage(GuiDestroyerArmor gui) {
        super(gui);
    }

    @Override
    public String title() {
        return "Forge";
    }

    @Override
    public int icon() {
        return 2;
    }

    private int nodeX(ArmorUpgrade u) {
        return left + 18 + u.branch.ordinal() * 29;
    }

    private int nodeY(ArmorUpgrade u) {
        return top + 42 + u.tier * 46;
    }

    @Override
    public void draw(int mouseX, int mouseY) {
        int x = left, y = top;
        inset(x + 6, y + 24, 182, 206);
        int forged = armor.forged(), stones = armor.stones();
        for (ArmorUpgrade.Branch branch : ArmorUpgrade.Branch.values()) {
            String title = branch.title.toUpperCase();
            int bx = x + 18 + branch.ordinal() * 29 + 12;
            // Six lines to a narrow page: their names stagger, so none runs into the next.
            int color = branch == ArmorUpgrade.Branch.INFINITY ? VIOLET : branch == ArmorUpgrade.Branch.ENERGY ? ENERGY : PALE;
            smallCentered(title, bx, y + (branch.ordinal() % 2 == 0 ? 27 : 33), color);
        }
        // The lines between steps, lit once the step before is forged.
        for (ArmorUpgrade u : ArmorUpgrade.values()) {
            if (u.parent != null) {
                int lx = nodeX(u) + 11, from = nodeY(u.parent) + 24, to = nodeY(u);
                boolean lit = u.parent.in(forged);
                drawRect(lx, from, lx + 2, to, lit ? 0xFF000000 | (u.in(forged) ? GOLD : 0x8A6A3A) : EDGE);
            }
        }
        RenderHelper.enableGUIStandardItemLighting();
        for (ArmorUpgrade u : ArmorUpgrade.values()) {
            node(u, nodeX(u), nodeY(u), forged, stones, inside(mouseX, mouseY, nodeX(u), nodeY(u), 24, 24));
        }
        RenderHelper.disableStandardItemLighting();
        // A key to the colours.
        int ky = y + 218;
        legend(x + 14, ky, 0xFF000000 | GOLD, "forged");
        legend(x + 62, ky, 0xFF000000 | EMBER, "can forge");
        legend(x + 122, ky, SHINE, "locked");

        details(x + 192, y + 24, 86, 206, mouseX, mouseY);
    }

    private void node(ArmorUpgrade u, int nx, int ny, int forged, int stones, boolean over) {
        boolean done = u.in(forged);
        String blocker = u.blocker(forged, stones);
        boolean open = blocker == null;
        boolean ready = open && u.affordable(mc.player);
        float pulse = 0.5F + 0.5F * MathHelper.sin(time * 0.25F);
        int border = done ? 0xFF000000 | GOLD : ready ? alpha(EMBER, 0.6F + 0.4F * pulse) : open ? SHINE : EDGE;
        if (u == chosen) {
            drawRect(nx - 2, ny - 2, nx + 26, ny + 26, 0xFFE8E0D0);
        }
        drawRect(nx - 1, ny - 1, nx + 25, ny + 25, border);
        drawGradientRect(nx, ny, nx + 24, ny + 24, done ? 0xFF4A3218 : over ? 0xFF26282E : 0xFF1A1B20, done ? 0xFF22160C : 0xFF0D0E11);
        if (u.branch == ArmorUpgrade.Branch.INFINITY && done) {
            drawGradientRect(nx, ny, nx + 24, ny + 24, 0x40B040FF, 0x10B040FF);
        }
        icon(u.ordinal(), nx + 4, ny + 4);
        if (!done && !open) {
            GlStateManager.disableDepth();
            drawRect(nx, ny, nx + 24, ny + 24, 0x98000000);
            GlStateManager.enableDepth();
        }
        if (done) {
            GlStateManager.disableDepth();
            drawRect(nx + 17, ny + 17, nx + 23, ny + 23, 0xFF000000 | GOLD);
            drawRect(nx + 18, ny + 18, nx + 22, ny + 22, 0xFF2A1C0C);
            GlStateManager.enableDepth();
        }
    }

    /** What the chosen upgrade does, what stands in its way, what it costs, and the button to forge it. */
    private void details(int x, int y, int w, int h, int mouseX, int mouseY) {
        inset(x, y, w, h);
        ArmorUpgrade u = chosen;
        int tx = x + 5, ty = y + 5, tw = w - 10;
        // Its picture beside its name.
        drawRect(tx - 1, ty - 1, tx + 17, ty + 17, VOID);
        icon(u.ordinal(), tx, ty);
        int titleTop = ty;
        for (String line : font.listFormattedStringToWidth(u.title, tw - 20)) {
            font.drawString(line, tx + 20, ty, u.branch == ArmorUpgrade.Branch.INFINITY ? 0xD8A8FF : GOLD);
            ty += 9;
        }
        ty = Math.max(ty, titleTop + 18);
        small(u.branch.title.toUpperCase() + "  " + roman(u.tier + 1), tx, ty, DIM);
        ty += 10;
        ty = wrapSmall(u.description, tx, ty, tw, PALE) + 4;
        int forged = armor.forged(), stones = armor.stones();
        String blocker = u.blocker(forged, stones);
        if (blocker != null) {
            ty = wrapSmall(blocker, tx, ty, tw, u.in(forged) ? GREEN : RED) + 4;
        }
        costCount = 0;
        if (!u.in(forged)) {
            drawRect(tx, ty, tx + tw, ty + 1, EDGE);
            small("COST", tx, ty + 3, DIM);
            ty += 12;
            RenderHelper.enableGUIStandardItemLighting();
            for (ArmorUpgrade.Cost cost : u.costs) {
                costY[costCount++] = ty;
                ItemStack icon = cost.icon();
                drawRect(tx, ty, tx + 18, ty + 18, 0xFF0A0A0C);
                items.renderItemAndEffectIntoGUI(icon, tx + 1, ty + 1);
                int have = mc.player.capabilities.isCreativeMode ? cost.count : cost.carried(mc.player);
                String count = Math.min(have, 999) + "/" + cost.count;
                font.drawString(count, tx + 22, ty + 1, have >= cost.count ? GREEN : RED);
                small(shorten(cost.name(), 54), tx + 22, ty + 10, DIM);
                ty += 20;
            }
            RenderHelper.disableStandardItemLighting();
        }
        int bx = x + 4, by = y + h - 22, bw = w - 8;
        boolean can = blocker == null && u.affordable(mc.player);
        button(bx, by, bw, 18, u.in(forged) ? "FORGED" : "FORGE", can, can && inside(mouseX, mouseY, bx, by, bw, 18), EMBER);
    }

    @Override
    public boolean click(int mouseX, int mouseY, int button) {
        if (button != 0) {
            return false;
        }
        for (ArmorUpgrade u : ArmorUpgrade.values()) {
            if (inside(mouseX, mouseY, nodeX(u), nodeY(u), 24, 24)) {
                chosen = u;
                gui.click();
                return true;
            }
        }
        int bx = left + 196, by = top + 24 + 206 - 22;
        if (inside(mouseX, mouseY, bx, by, 78, 18) && chosen.blocker(armor.forged(), armor.stones()) == null && chosen.affordable(mc.player)) {
            gui.press(chosen.ordinal());
            gui.click();
            return true;
        }
        return false;
    }

    @Override
    public void hover(int mouseX, int mouseY, List<String> lines) {
        for (ArmorUpgrade u : ArmorUpgrade.values()) {
            if (inside(mouseX, mouseY, nodeX(u), nodeY(u), 24, 24)) {
                lines.add((u.in(armor.forged()) ? "§6" : "§f") + u.title);
                String blocker = u.blocker(armor.forged(), armor.stones());
                lines.add(blocker == null ? (u.affordable(mc.player) ? "§aReady to forge" : "§cMissing materials")
                        : (u.in(armor.forged()) ? "§7" : "§c") + blocker);
            }
        }
        // Cost icons in the details: their items' own names.
        for (int i = 0; i < costCount; i++) {
            if (inside(mouseX, mouseY, left + 197, costY[i], 18, 18)) {
                gui.itemTip(chosen.costs[i].icon(), mouseX, mouseY);
                return;
            }
        }
    }
}
