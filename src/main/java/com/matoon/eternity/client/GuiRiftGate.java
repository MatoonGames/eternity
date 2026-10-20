package com.matoon.eternity.client;

import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.network.MsgTearRift;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

/**
 * Choose where a Rift Gate leads: a dimension and a place in it. Coordinates accept
 * "~" for where you stand now, and "~10" style offsets from it.
 */
@SideOnly(Side.CLIENT)
public class GuiRiftGate extends GuiScreen {

    private static final int PANEL_W = 220;
    private static final int PANEL_H = 150;
    private static final int GOLD = 0xFFD49A1C;
    private static final int GOLD_DIM = 0xFF5A3A08;

    private GuiTextField dimension;
    private GuiTextField fieldX, fieldY, fieldZ;
    private GuiTextField[] fields;
    private GuiButton tear;
    private String problem;

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        EntityPlayer player = mc.player;
        int left = (width - PANEL_W) / 2;
        int top = (height - PANEL_H) / 2;

        dimension = field(0, left + 92, top + 34, 116, String.valueOf(player.dimension));
        fieldX = field(1, left + 20, top + 78, 58, String.valueOf((int) Math.floor(player.posX)));
        fieldY = field(2, left + 82, top + 78, 58, String.valueOf((int) Math.floor(player.posY)));
        fieldZ = field(3, left + 144, top + 78, 58, String.valueOf((int) Math.floor(player.posZ)));
        fields = new GuiTextField[]{dimension, fieldX, fieldY, fieldZ};
        dimension.setFocused(true);

        buttonList.clear();
        tear = addButton(new GuiButton(0, left + 20, top + PANEL_H - 30, 100, 20, I18n.format("gui.eternity.rift_gate.tear")));
        addButton(new GuiButton(1, left + PANEL_W - 100, top + PANEL_H - 30, 80, 20, I18n.format("gui.cancel")));
        validate();
    }

    private GuiTextField field(int id, int x, int y, int w, String text) {
        GuiTextField field = new GuiTextField(id, fontRenderer, x, y, w, 16);
        field.setMaxStringLength(14);
        field.setText(text);
        return field;
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void updateScreen() {
        for (GuiTextField field : fields) {
            field.updateCursorCounter();
        }
    }

    @Override
    protected void keyTyped(char typed, int key) throws IOException {
        if (key == Keyboard.KEY_TAB) {
            // Tab cycles through the fields.
            for (int i = 0; i < fields.length; i++) {
                if (fields[i].isFocused()) {
                    fields[i].setFocused(false);
                    fields[(i + (isShiftKeyDown() ? fields.length - 1 : 1)) % fields.length].setFocused(true);
                    return;
                }
            }
            return;
        }
        if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) {
            if (tear.enabled) {
                actionPerformed(tear);
            }
            return;
        }
        for (GuiTextField field : fields) {
            field.textboxKeyTyped(typed, key);
        }
        validate();
        super.keyTyped(typed, key);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        super.mouseClicked(mouseX, mouseY, button);
        for (GuiTextField field : fields) {
            field.mouseClicked(mouseX, mouseY, button);
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0 && validate()) {
            EntityPlayer player = mc.player;
            EternityNetwork.CHANNEL.sendToServer(new MsgTearRift(Integer.parseInt(dimension.getText().trim()),
                    coordinate(fieldX.getText(), player.posX) + 0.5D,
                    coordinate(fieldY.getText(), player.posY),
                    coordinate(fieldZ.getText(), player.posZ) + 0.5D));
        }
        mc.displayGuiScreen(null);
    }

    /** Checks every field and remembers what is wrong, if anything. */
    private boolean validate() {
        problem = null;
        try {
            int dim = Integer.parseInt(dimension.getText().trim());
            if (!DimensionManager.isDimensionRegistered(dim)) {
                problem = I18n.format("gui.eternity.rift_gate.no_dimension");
            }
        } catch (NumberFormatException e) {
            problem = I18n.format("gui.eternity.rift_gate.bad_dimension");
        }
        if (problem == null) {
            EntityPlayer player = mc.player;
            double x = coordinate(fieldX.getText(), player.posX);
            double y = coordinate(fieldY.getText(), player.posY);
            double z = coordinate(fieldZ.getText(), player.posZ);
            if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z)) {
                problem = I18n.format("gui.eternity.rift_gate.bad_coordinates");
            } else if (Math.abs(x) > 29_999_000D || Math.abs(z) > 29_999_000D) {
                problem = I18n.format("gui.eternity.rift_gate.out_of_bounds");
            }
        }
        if (tear != null) {
            tear.enabled = problem == null;
        }
        return problem == null;
    }

    /** Parses "120", "~" or "~-8". Returns NaN if it cannot. */
    private static double coordinate(String text, double here) {
        String s = text.trim();
        try {
            if (s.startsWith("~")) {
                String rest = s.substring(1);
                return Math.floor(here) + (rest.isEmpty() ? 0D : Double.parseDouble(rest));
            }
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int left = (width - PANEL_W) / 2;
        int top = (height - PANEL_H) / 2;
        Gui.drawRect(left - 2, top - 2, left + PANEL_W + 2, top + PANEL_H + 2, 0xF0040008);
        drawGradientRect(left, top, left + PANEL_W, top + PANEL_H, 0xF4140628, 0xF4070212);
        frame(left - 1, top - 1, PANEL_W + 2, PANEL_H + 2, GOLD_DIM);
        frame(left, top, PANEL_W, PANEL_H, GOLD);

        drawCenteredString(fontRenderer, I18n.format("gui.eternity.rift_gate.title"), width / 2, top + 10, 0xD89CFF);
        drawString(fontRenderer, I18n.format("gui.eternity.rift_gate.dimension"), left + 20, top + 38, 0xD49A1C);
        drawString(fontRenderer, I18n.format("gui.eternity.rift_gate.coordinates"), left + 20, top + 64, 0xD49A1C);
        drawString(fontRenderer, "X", left + 20, top + 96, 0x8A6AAB);
        drawString(fontRenderer, "Y", left + 82, top + 96, 0x8A6AAB);
        drawString(fontRenderer, "Z", left + 144, top + 96, 0x8A6AAB);
        for (GuiTextField field : fields) {
            field.drawTextBox();
        }
        if (problem != null) {
            drawCenteredString(fontRenderer, problem, width / 2, top + 108, 0xE0507A);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private static void frame(int x, int y, int w, int h, int color) {
        Gui.drawRect(x, y, x + w, y + 1, color);
        Gui.drawRect(x, y + h - 1, x + w, y + h, color);
        Gui.drawRect(x, y, x + 1, y + h, color);
        Gui.drawRect(x + w - 1, y, x + w, y + h, color);
    }
}
