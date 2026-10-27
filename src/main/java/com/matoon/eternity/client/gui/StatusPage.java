package com.matoon.eternity.client.gui;

import com.matoon.eternity.EternityConfig;
import com.matoon.eternity.client.render.RenderDestroyer;
import com.matoon.eternity.entity.ArmorUpgrade;
import com.matoon.eternity.entity.EntityDestroyerArmor;
import com.matoon.eternity.entity.EntityDestroyerArmor.Condition;
import com.matoon.eternity.entity.Overcharge;
import com.matoon.eternity.item.ItemAstralRegulator;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

/**
 * The armour itself, turning to follow your eye; its vital signs (integrity, what feeds it, its furnace, its core once
 * it can be overcharged); how each of its systems is holding up; and what it needs next, if anything.
 */
@SideOnly(Side.CLIENT)
class StatusPage extends DestroyerPage {

    StatusPage(GuiDestroyerArmor gui) {
        super(gui);
    }

    @Override
    public String title() {
        return "Status";
    }

    @Override
    public int icon() {
        return 0;
    }

    @Override
    public void draw(int mouseX, int mouseY) {
        int x = left, y = top;
        // The armour, turning to follow your eye, its condition over it and its state under it.
        inset(x + 6, y + 24, 104, 206);
        int cx = x + 58, base = y + 200;
        drawGradientRect(x + 7, y + 25, x + 109, y + 110, 0x18FFFFFF, 0x00FFFFFF);
        drawGradientRect(x + 7, y + 150, x + 109, y + 229, 0x00000000, alpha(EMBER, 0.1F + 0.08F * armor.heat(1F)));
        RenderDestroyer.showAlways = true;
        try {
            GuiInventory.drawEntityOnScreen(cx, base, 34, cx - mouseX, base - 120 - mouseY, armor);
        } finally {
            RenderDestroyer.showAlways = false;
        }
        Condition condition = armor.condition();
        smallCentered(condition.title.toUpperCase(), cx, y + 29, condition.color);
        smallCentered(state(), cx, y + 214, stateColor());
        smallCentered(armor.skin().title, cx, y + 221, DIM);

        // Its vital signs.
        int px = x + 114, pw = 164, gx = px + 6, gw = pw - 12;
        boolean overcharge = armor.canOvercharge();
        int vitals = 16 + (overcharge ? 4 : 3) * 18;
        panel(px, y + 24, pw, vitals, "VITALS");
        int gy = y + 39;
        float integrity = armor.getHealth() / armor.getMaxHealth();
        label("INTEGRITY", gx, gy, gw, String.format("%.0f / %.0f", armor.getHealth(), armor.getMaxHealth()), condition.color);
        bar(gx, gy + 8, gw, 4, integrity, condition.color & 0xFFFFFF, lighten(condition.color, 0.3F) & 0xFFFFFF);
        gy += 18;
        float fuel = armor.getLava() / armor.lavaCapacity();
        if (armor.electric()) {
            label("ENERGY", gx, gy, gw, ItemAstralRegulator.formatEnergy(armor.energy()) + " / " + ItemAstralRegulator.formatEnergy(armor.energyCapacity()) + " FE",
                    armor.cold() ? RED : fuel < 0.15F ? AMBER : ENERGY);
            energyBar(gx, gy + 8, gw, 4, fuel);
        } else {
            label("LAVA", gx, gy, gw, String.format("%.1f / %.0f buckets", armor.getLava() / EntityDestroyerArmor.BUCKET,
                    armor.lavaCapacity() / EntityDestroyerArmor.BUCKET), armor.cold() ? RED : fuel < 0.15F ? AMBER : GOLD);
            bar(gx, gy + 8, gw, 4, fuel, 0xA8380C, 0xFFB040);
        }
        gy += 18;
        float heat = armor.getFurnace() / EntityDestroyerArmor.HEAT_MAX;
        boolean venting = armor.getState() == EntityDestroyerArmor.State.VENT;
        label("FURNACE", gx, gy, gw, venting ? "VENTING" : String.format("%.0f%%", heat * 100F), venting ? RED : heat > 0.75F ? AMBER : GOLD);
        heatBar(gx, gy + 8, gw, 4, heat);
        gy += 18;
        if (overcharge) {
            label("CORE", gx, gy, gw, String.format("%.0f%%  %s", armor.core(), coreWord()), coreWordColor());
            coreBar(gx, gy + 8, gw, 4);
        }

        // Its systems.
        int sy = y + 24 + vitals + 3;
        String[][] systems = systems();
        int sh = 15 + systems.length * 8;
        panel(px, sy, pw, sh, "SYSTEMS");
        for (int i = 0; i < systems.length; i++) {
            int ry = sy + 15 + i * 8;
            small(systems[i][0], gx, ry, PALE);
            int color = Integer.parseInt(systems[i][2], 16);
            small(systems[i][1], gx + gw - (int) (font.getStringWidth(systems[i][1]) * 0.75F), ry, color);
        }

        // What it needs next.
        int ny = sy + sh + 3, nh = y + 230 - ny;
        if (nh >= 18) {
            panel(px, ny, pw, nh, "NEEDS");
            List<String[]> needs = needs();
            int ty = ny + 16;
            for (String[] need : needs) {
                if (ty + 7 > ny + nh - 2) {
                    break;
                }
                small(need[0], gx, ty, Integer.parseInt(need[1], 16));
                ty += 8;
            }
        }
    }

    private String state() {
        if (armor.getState() == EntityDestroyerArmor.State.DOWN) {
            return "DOWN";
        }
        if (armor.cold()) {
            return armor.electric() ? "DRAINED" : "COLD";
        }
        return armor.isBeingRidden() ? "MANNED" : "EMPTY";
    }

    private int stateColor() {
        return armor.getState() == EntityDestroyerArmor.State.DOWN || armor.cold() ? RED : armor.isBeingRidden() ? EMBER : PALE;
    }

    /** Each of its systems, how it is holding up, and in what colour to say so. */
    private String[][] systems() {
        Condition condition = armor.condition();
        boolean cold = armor.cold();
        boolean critical = condition == Condition.CRITICAL;
        String hex = hex(condition.color);
        float fuel = armor.getLava() / armor.lavaCapacity();
        String furnace = cold ? (armor.electric() ? "DRAINED" : "COLD") : armor.getState() == EntityDestroyerArmor.State.VENT ? "Venting"
                : fuel < 0.15F ? "Low" : armor.electric() ? "Charged" : "Burning";
        String furnaceColor = cold ? hex(RED) : fuel < 0.15F || armor.getState() == EntityDestroyerArmor.State.VENT ? hex(AMBER) : hex(GREEN);
        String stride = cold ? "Seized" : condition == Condition.NOMINAL ? (armor.has(ArmorUpgrade.ASCENSION) ? "Flight" : "Nominal")
                : String.format("%s -%.0f%%", condition.title, (1F - condition.stride) * 100F);
        String fists = cold || critical ? "Weakened" : armor.has(ArmorUpgrade.HAMMERFALL) ? "Hammerfall" : armor.has(ArmorUpgrade.URU_KNUCKLES) ? "Uru" : "Nominal";
        String ray = cold || critical ? "OFFLINE" : condition == Condition.FAILING ? "Sputtering" : "Ready";
        String rayColor = cold || critical ? hex(RED) : condition == Condition.FAILING ? hex(AMBER) : hex(GREEN);
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Frame", condition.title, hex});
        rows.add(new String[]{armor.electric() ? "Dynamo" : "Furnace", furnace, furnaceColor});
        rows.add(new String[]{"Actuators", stride, cold ? hex(RED) : hex});
        rows.add(new String[]{"Fists", fists + boost(Overcharge.FISTS), cold || critical ? hex(AMBER) : hex(GREEN)});
        rows.add(new String[]{"Ray", ray + boost(Overcharge.BEAM), rayColor});
        if (armor.canOvercharge()) {
            int total = armor.surgeTotal();
            rows.add(new String[]{"Overcharge", total == 0 ? "Off" : total + " levels, " + coreWord(), hex(total == 0 ? DIM : coreWordColor())});
        }
        if (armor.has(ArmorUpgrade.CRADLE)) {
            int stones = Integer.bitCount(armor.stones());
            rows.add(new String[]{"Cradle", stones + "/6 stones" + (armor.regulator().isEmpty() ? "" : ", Regulator"), hex(stones > 0 ? VIOLET : DIM)});
        }
        return rows.toArray(new String[0][]);
    }

    /** A part's overcharge, after its state, if it has any. */
    private String boost(Overcharge part) {
        int level = armor.surge(part);
        return level > 0 ? " +" + level : "";
    }

    /** What it needs next, most pressing first, each with its colour: or that it wants for nothing. */
    private List<String[]> needs() {
        List<String[]> needs = new ArrayList<>();
        if (armor.breach() > 0) {
            needs.add(new String[]{"Its core is breaching: cut the overcharge!", hex(RED)});
        } else if (armor.scorching()) {
            needs.add(new String[]{"Its core is tearing it apart: cool it!", hex(RED)});
        } else if (armor.core() >= 65F) {
            needs.add(new String[]{"Its core runs hot: turn the overcharge down.", hex(0xFF8040)});
        }
        if (armor.canOvercharge() && armor.surgeDraw() > armor.supply() && armor.surgeTotal() > 0) {
            needs.add(new String[]{"Overcharge draws more than it is fed.", hex(AMBER)});
        }
        float missing = (armor.getMaxHealth() - armor.getHealth()) / (float) EternityConfig.vault.armorHealth;
        int plates = (int) Math.ceil(missing / EntityDestroyerArmor.PLATE - 0.01F);
        if (plates > 0) {
            needs.add(new String[]{plates + " Uru plate" + (plates == 1 ? "" : "s") + " to mend it (SUPPLY)", hex(armor.condition().color)});
        }
        float fuel = armor.getLava() / armor.lavaCapacity();
        if (fuel < 0.25F) {
            needs.add(new String[]{(armor.electric() ? "Forge Energy" : "Lava") + " to feed it (SUPPLY)", hex(fuel <= 0F ? RED : AMBER)});
        }
        int left = ArmorUpgrade.values().length - Integer.bitCount(armor.forged());
        if (left > 0) {
            needs.add(new String[]{left + " upgrade" + (left == 1 ? "" : "s") + " still to forge (FORGE)", hex(DIM)});
        }
        if (needs.isEmpty()) {
            needs.add(new String[]{"Nothing. It is ready.", hex(GREEN)});
        }
        return needs;
    }

    @Override
    public void hover(int mouseX, int mouseY, List<String> lines) {
        if (inside(mouseX, mouseY, left + 6, top + 24, 104, 206)) {
            lines.add("§6The Destroyer Armor");
            lines.add(String.format("§7Integrity %.0f / %.0f", armor.getHealth(), armor.getMaxHealth()));
            lines.add(armor.electric() ? String.format("§7Energy %d / %d FE", armor.energy(), armor.energyCapacity())
                    : String.format("§7Lava %.0f / %.0f mB", armor.getLava(), armor.lavaCapacity()));
            lines.add(String.format("§7Forged %d / %d", Integer.bitCount(armor.forged()), ArmorUpgrade.values().length));
        }
    }
}
