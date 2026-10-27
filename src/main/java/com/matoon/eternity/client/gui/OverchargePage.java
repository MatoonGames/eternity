package com.matoon.eternity.client.gui;

import com.matoon.eternity.client.ArmorClient;
import com.matoon.eternity.entity.ArmorUpgrade;
import com.matoon.eternity.entity.Overcharge;
import com.matoon.eternity.inventory.ContainerDestroyerArmor;
import com.matoon.eternity.item.ItemAstralRegulator;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

/**
 * Its overcharge: each part of it (the beam, the jets, the fists, the plating) turned past what it was built for, as
 * far as its pilot dares. Each part on its own row: what it does at its level, what holding it draws, and buttons to
 * turn it. Beside them, its dynamo (what it is fed against what the overcharge draws) and its core, how hot it runs
 * and where it would settle; and a button to cut it all.
 */
@SideOnly(Side.CLIENT)
class OverchargePage extends DestroyerPage {

    /** How many parts show at once, and how tall each row is; more than fit scroll. */
    private static final int ROWS = 4, ROW_H = 40, ROW_W = 166;
    private int first;

    OverchargePage(GuiDestroyerArmor gui) {
        super(gui);
    }

    @Override
    public String title() {
        return "Overcharge";
    }

    @Override
    public int icon() {
        return 4;
    }

    @Override
    public String locked() {
        if (armor.canOvercharge()) {
            return null;
        }
        return armor.has(ArmorUpgrade.DYNAMO) ? "Forge the Overcharge (Energy line) to overcharge its parts."
                : "Forge the Dynamo, then the Overcharge (Energy line), to overcharge its parts.";
    }

    private int rowX() {
        return left + 10;
    }

    private int rowY(int row) {
        return top + 38 + row * ROW_H;
    }

    private int minusX() {
        return rowX() + ROW_W - 64;
    }

    private int plusX() {
        return rowX() + ROW_W - 18;
    }

    @Override
    public void draw(int mouseX, int mouseY) {
        int x = left, y = top;
        Overcharge[] parts = Overcharge.values();
        first = MathHelper.clamp(first, 0, Math.max(0, parts.length - ROWS));
        panel(x + 6, y + 24, 174, 206, "PARTS");
        if (parts.length > ROWS) {
            String more = (first + 1) + "-" + Math.min(parts.length, first + ROWS) + " of " + parts.length + ", wheel to scroll";
            small(more, x + 174 - (int) (font.getStringWidth(more) * 0.75F), y + 28, DIM);
        }
        for (int row = 0; row < ROWS && first + row < parts.length; row++) {
            row(parts[first + row], rowY(row), mouseX, mouseY);
        }
        int hy = y + 202;
        drawRect(x + 10, hy - 3, x + 176, hy - 2, 0xFF1A1C21);
        hy = wrapSmall("Click -/+: 1 level, SHIFT 10, CTRL 100; or the wheel over a part. Right-click a level: off.",
                x + 10, hy, 166, DIM);
        wrapSmall(String.format("Inside: %s/%s pick, %s/%s turn (hold: faster), %s cuts all.",
                ArmorClient.OVERCHARGE_PREV.getDisplayName(), ArmorClient.OVERCHARGE_NEXT.getDisplayName(), ArmorClient.OVERCHARGE_UP.getDisplayName(),
                ArmorClient.OVERCHARGE_DOWN.getDisplayName(), ArmorClient.OVERCHARGE_CUT.getDisplayName()), x + 10, hy, 166, PALE);

        // Its dynamo: what it is fed, against what the overcharge draws.
        int px = x + 184, pw = 94, tx = px + 5, tw = pw - 10;
        panel(px, y + 24, pw, 76, "DYNAMO");
        int fed = armor.supply(), draw = armor.surgeDraw();
        label("FED", tx, y + 39, tw, ItemAstralRegulator.formatEnergy(fed) + "/t", fed > 0 ? GREEN : DIM);
        label("DRAW", tx, y + 47, tw, ItemAstralRegulator.formatEnergy(draw) + "/t", draw == 0 ? DIM : draw > fed ? RED : AMBER);
        int net = fed - draw;
        label("NET", tx, y + 55, tw, (net >= 0 ? "+" : "-") + ItemAstralRegulator.formatEnergy(Math.abs((long) net)) + "/t", net >= 0 ? GREEN : RED);
        energyBar(tx, y + 66, tw, 5, armor.getLava() / armor.lavaCapacity());
        String store;
        int storeColor;
        if (draw == 0) {
            store = "Nothing overcharged";
            storeColor = DIM;
        } else if (net >= 0) {
            store = "It keeps up";
            storeColor = GREEN;
        } else if (armor.energy() > 0) {
            store = String.format("Dry in %.0fs", armor.energy() / (float) -net / 20F);
            storeColor = AMBER;
        } else {
            store = "STARVING";
            storeColor = RED;
        }
        smallCentered(store, px + pw / 2, y + 76, storeColor);
        small("only while inside", tx, y + 88, DIM);

        // Its core.
        panel(px, y + 104, pw, 80, "CORE");
        String word = coreWord();
        font.drawStringWithShadow(String.format("%.0f%%", armor.core()), tx, y + 120, coreWordColor());
        small(word, px + pw - 5 - (int) (font.getStringWidth(word) * 0.75F), y + 121, coreWordColor());
        coreBar(tx, y + 131, tw, 8);
        // Where it would settle, held as it is.
        int total = armor.surgeTotal();
        float settle = Overcharge.heating(total) / armor.coreShed();
        String where = total == 0 ? "Cooling" : settle >= Overcharge.CORE_MAX ? "Held so: it BREACHES" : String.format("Held so: settles ~%.0f%%", settle);
        int whereColor = total == 0 ? DIM : settle >= Overcharge.CORE_MAX ? RED : settle >= 65F ? 0xFF8040 : PALE;
        if (draw > fed && total > 0) {
            where = "Starved, it runs away";
            whereColor = RED;
        }
        wrapSmall(where, tx, y + 145, tw, whereColor);
        if (armor.scorching()) {
            wrapSmall("Too hot too long: its integrity is failing!", tx, y + 160, tw, RED);
        } else {
            wrapSmall(total + " level" + (total == 1 ? "" : "s") + " held. Over " + (int) Overcharge.SCORCH_AT + "% for long, it harms itself.", tx, y + 160, tw, DIM);
        }
        if (armor.breach() > 0) {
            float flash = 1F;
            drawRect(px + 1, y + 105, px + pw - 1, y + 106, alpha(RED, flash));
            drawRect(px + 1, y + 182, px + pw - 1, y + 183, alpha(RED, flash));
        }

        // Cutting it all.
        int bx = px + 2, by = y + 190, bw = pw - 4;
        boolean can = total > 0;
        button(bx, by, bw, 18, "CUT ALL", can, can && inside(mouseX, mouseY, bx, by, bw, 18), 0xFF3020);
        smallCentered("or " + ArmorClient.OVERCHARGE_CUT.getDisplayName() + " inside", px + pw / 2, y + 213, DIM);

        if (locked() != null) {
            lockedOver(x + 7, y + 25, 270, 204, locked() + " Then turn each part up here, or with its keys from inside. Every level draws Forge Energy and heats its core: run it hotter than it sheds, or draw more than it is fed, and it implodes.");
        }
    }

    private void row(Overcharge part, int ry, int mouseX, int mouseY) {
        int rx = rowX();
        int level = armor.surge(part);
        boolean picked = ArmorClient.surgePart == part.ordinal();
        boolean over = inside(mouseX, mouseY, rx, ry, ROW_W, ROW_H - 4);
        drawRect(rx - 1, ry - 1, rx + ROW_W + 1, ry + ROW_H - 3, picked ? alpha(EMBER, 0.9F) : over ? SHINE : EDGE);
        drawGradientRect(rx, ry, rx + ROW_W, ry + ROW_H - 4, level > 0 ? lighten(part.color, -0.8F) : 0xFF17181C, 0xFF0D0E11);
        drawRect(rx, ry, rx + 2, ry + ROW_H - 4, 0xFF000000 | (level > 0 ? part.color : lighten(part.color, -0.6F) & 0xFFFFFF));
        drawRect(rx + 4, ry + 4, rx + 22, ry + 22, VOID);
        icon(PART_ICONS + part.ordinal(), rx + 5, ry + 5);
        font.drawString(part.title.toUpperCase(), rx + 26, ry + 4, level > 0 ? part.color : PALE);
        if (picked) {
            small("KEYS", rx + 26 + font.getStringWidth(part.title.toUpperCase()) + 4, ry + 5, EMBER);
        }
        int upkeep = Overcharge.upkeep(level);
        small(level > 0 ? "draws " + ItemAstralRegulator.formatEnergy(upkeep) + " FE/t" : "draws nothing", rx + 26, ry + 14, level > 0 ? 0xD06050 : DIM);
        // What it does, the whole width of its row.
        small(shorten(part.effect(level), ROW_W - 30), rx + 26, ry + 25, level > 0 ? 0xE8DCC8 : DIM);
        // Its level, and the buttons either side.
        int mx = minusX(), px = plusX(), by = ry + 4;
        boolean canDown = level > 0;
        button(mx, by, 14, 14, "-", canDown, canDown && inside(mouseX, mouseY, mx, by, 14, 14), part.color);
        button(px, by, 14, 14, "+", true, inside(mouseX, mouseY, px, by, 14, 14), part.color);
        drawRect(mx + 16, by - 1, px - 2, by + 15, VOID);
        String n = String.valueOf(level);
        int nw = font.getStringWidth(n);
        int nx = (mx + 16 + px - 2) / 2;
        if (nw > px - mx - 20) {
            smallCentered(n, nx, by + 4, level > 0 ? 0xFFFFFF : DIM);
        } else {
            font.drawStringWithShadow(n, nx - nw / 2F, by + 3, level > 0 ? 0xFFFFFF : DIM);
        }
    }

    /** How far a click turns a part, by the keys held: which of {@link ContainerDestroyerArmor#SURGE_STEPS}. */
    private static int step(boolean up) {
        int size = GuiDestroyerArmor.isCtrlKeyDown() ? 4 : GuiDestroyerArmor.isShiftKeyDown() ? 2 : 0;
        return size + (up ? 1 : 0);
    }

    @Override
    public boolean click(int mouseX, int mouseY, int button) {
        if (locked() != null) {
            return false;
        }
        Overcharge[] parts = Overcharge.values();
        for (int row = 0; row < ROWS && first + row < parts.length; row++) {
            Overcharge part = parts[first + row];
            int ry = rowY(row);
            if (!inside(mouseX, mouseY, rowX(), ry, ROW_W, ROW_H - 4)) {
                continue;
            }
            ArmorClient.surgePart = part.ordinal();
            int by = ry + 4;
            if (button == 0 && inside(mouseX, mouseY, minusX(), by, 14, 14)) {
                gui.press(ContainerDestroyerArmor.surgeButton(part.ordinal(), step(false)));
            } else if (button == 0 && inside(mouseX, mouseY, plusX(), by, 14, 14)) {
                gui.press(ContainerDestroyerArmor.surgeButton(part.ordinal(), step(true)));
            } else if (button == 1 && inside(mouseX, mouseY, minusX() + 16, by, plusX() - minusX() - 18, 14)) {
                gui.press(ContainerDestroyerArmor.surgeButton(part.ordinal(), 6));
            }
            gui.click();
            return true;
        }
        int bx = left + 186, by = top + 190;
        if (button == 0 && inside(mouseX, mouseY, bx, by, 90, 18) && armor.surgeTotal() > 0) {
            gui.press(ContainerDestroyerArmor.surgeButton(-1, 6));
            gui.click();
            return true;
        }
        return false;
    }

    @Override
    public boolean scroll(int mouseX, int mouseY, int direction) {
        if (locked() != null) {
            return false;
        }
        Overcharge[] parts = Overcharge.values();
        for (int row = 0; row < ROWS && first + row < parts.length; row++) {
            if (inside(mouseX, mouseY, rowX(), rowY(row), ROW_W, ROW_H - 4)) {
                Overcharge part = parts[first + row];
                ArmorClient.surgePart = part.ordinal();
                gui.press(ContainerDestroyerArmor.surgeButton(part.ordinal(), step(direction > 0)));
                return true;
            }
        }
        if (parts.length > ROWS && inside(mouseX, mouseY, left + 6, top + 24, 174, 206)) {
            first = MathHelper.clamp(first - direction, 0, parts.length - ROWS);
            return true;
        }
        return false;
    }

    @Override
    public void hover(int mouseX, int mouseY, List<String> lines) {
        if (locked() != null) {
            return;
        }
        Overcharge[] parts = Overcharge.values();
        for (int row = 0; row < ROWS && first + row < parts.length; row++) {
            if (inside(mouseX, mouseY, rowX() + 4, rowY(row) + 4, 18, 18)) {
                Overcharge part = parts[first + row];
                int level = armor.surge(part);
                lines.add("§6" + part.title);
                lines.addAll(font.listFormattedStringToWidth("§7" + part.description, 180));
                lines.add("§8Next level: " + part.effect(level + 1) + ", " + ItemAstralRegulator.formatEnergy(Overcharge.upkeep(level + 1)) + " FE/t");
            }
        }
    }
}
