package com.matoon.eternity.client.gui;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.List;

/**
 * The journal, open: the chapter as a title, what you are doing now under it, and everything you
 * have been told or found that bears on it, in the order it matters. One dark page in the
 * speaker panel's manner, edged in old gold; the mouse wheel scrolls it if it runs long. The
 * journal key or escape closes it.
 */
@SideOnly(Side.CLIENT)
public class GuiStoryJournal extends GuiScreen {

    private int ticks;
    private int scroll;

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void updateScreen() {
        ticks++;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        StoryJournal j = StoryJournal.INSTANCE;
        int accent = StoryJournal.ACCENT;
        float in = SpeakerPanel.ease((ticks + partialTicks) / 8F);
        int w = Math.min(width - 40, 340);
        int x0 = (width - w) / 2;
        int textW = w - 32;
        List<String> rows = new ArrayList<>();
        List<Integer> colours = new ArrayList<>();
        if (!j.hasAnything()) {
            rows.add("§oNothing has found you yet.");
            colours.add(0x9AA4C0);
        } else {
            for (String s : fontRenderer.listFormattedStringToWidth(j.goal, textW)) {
                rows.add(s);
                colours.add(0xF2F4FA);
            }
            if (!j.notes.isEmpty()) {
                rows.add("");
                colours.add(0);
                rows.add("WHAT YOU KNOW");
                colours.add(accent);
                for (String n : j.notes) {
                    boolean indent = n.startsWith("  ");
                    List<String> wrapped = fontRenderer.listFormattedStringToWidth(n.trim(), textW - (indent ? 12 : 8));
                    for (int i = 0; i < wrapped.size(); i++) {
                        rows.add((indent ? "    " : "") + (i == 0 ? (indent ? "" : "• ") : "  ") + wrapped.get(i));
                        colours.add(indent ? 0xE6D8A8 : 0xC8CCD8);
                    }
                }
            }
        }
        int visible = Math.min(rows.size(), Math.max(6, (height - 110) / 10));
        scroll = Math.max(0, Math.min(scroll, rows.size() - visible));
        int h = 44 + visible * 10 + 18;
        float y0 = (height - h) / 2F + (1F - in) * 10F;

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.shadeModel(org.lwjgl.opengl.GL11.GL_SMOOTH);
        SpeakerPanel.rect(0, 0, width, height, 0x000000, 0.35F * in, 0x000000, 0.5F * in);
        SpeakerPanel.rect(x0 - 6, y0 - 6, x0 + w + 6, y0 + h + 6, 0x000000, 0.3F * in, 0x000000, 0F);
        SpeakerPanel.rect(x0, y0, x0 + w, y0 + h, 0x0D1330, 0.95F * in, 0x04060E, 0.96F * in);
        SpeakerPanel.border(x0, y0, x0 + w, y0 + h, accent, 0.8F * in);
        for (int[] c : new int[][]{{x0, (int) y0}, {x0 + w, (int) y0}, {x0, (int) (y0 + h)}, {x0 + w, (int) (y0 + h)}}) {
            SpeakerPanel.diamond(c[0], c[1], 3F, accent, in);
        }
        SpeakerPanel.hgrad(x0 + 16, y0 + 31, x0 + w - 16, y0 + 31.6F, accent, 0.7F * in, accent, 0F);
        if (rows.size() > visible) {
            float bar = visible / (float) rows.size();
            float top = scroll / (float) rows.size();
            float trackY = y0 + 40, trackH = visible * 10;
            SpeakerPanel.rect(x0 + w - 8, trackY + trackH * top, x0 + w - 6, trackY + trackH * (top + bar), accent, 0.6F * in, accent, 0.6F * in);
        }
        GlStateManager.enableTexture2D();
        int a = (int) (Math.max(0.05F, in) * 255F) << 24;
        String title = j.chapter.isEmpty() ? "Journal" : j.chapter;
        GlStateManager.pushMatrix();
        GlStateManager.translate(x0 + 16, y0 + 11, 0F);
        GlStateManager.scale(1.5F, 1.5F, 1F);
        fontRenderer.drawStringWithShadow(title, 0, 0, accent | a);
        GlStateManager.popMatrix();
        int y = (int) y0 + 40;
        for (int i = scroll; i < scroll + visible && i < rows.size(); i++) {
            if (!rows.get(i).isEmpty()) {
                fontRenderer.drawStringWithShadow(rows.get(i), x0 + 16, y, colours.get(i) | a);
            }
            y += 10;
        }
        String foot = Keyboard.getKeyName(StoryJournal.OPEN.getKeyCode()) + " or Esc to close";
        GlStateManager.pushMatrix();
        GlStateManager.translate(x0 + w - 12 - fontRenderer.getStringWidth(foot) * 0.75F, y0 + h - 11, 0F);
        GlStateManager.scale(0.75F, 0.75F, 1F);
        fontRenderer.drawString(foot, 0, 0, 0x7A84A0 | a);
        GlStateManager.popMatrix();
        GlStateManager.disableBlend();
    }

    @Override
    public void handleMouseInput() throws java.io.IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            scroll += wheel > 0 ? -2 : 2;
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws java.io.IOException {
        if (keyCode == StoryJournal.OPEN.getKeyCode()) {
            mc.displayGuiScreen(null);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }
}
