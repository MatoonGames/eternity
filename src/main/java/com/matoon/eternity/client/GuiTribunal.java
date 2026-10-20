package com.matoon.eternity.client;

import com.matoon.eternity.world.TribunalDialogue;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

/**
 * Standing before the Living Tribunal. Black bars close in from above and below like a
 * film. His words appear in a gold-trimmed panel at the top as he speaks; when he has
 * finished, your answers rise one by one from the bottom. Point at one, or choose
 * with the arrow keys or its number, and commit to it. There is no closing this screen
 * and no walking away: he will have his answer.
 */
@SideOnly(Side.CLIENT)
public class GuiTribunal extends GuiScreen {

    private static final String[] NUMERALS = {"I", "II", "III", "IV", "V", "VI"};
    private static final int GOLD = 0xE8B85A;
    private static final int GOLD_PALE = 0xF6DFA0;
    private static final int SPEECH = 0xE9E2FF;
    private static final int ANSWER = 0xC9BEDF;

    private final int node;
    private int ticks;
    private int doneAt = -1;
    private int selected = -1;
    private int chosenAt = -1;

    private final List<int[]> cards = new ArrayList<>();
    private final List<List<String>> cardLines = new ArrayList<>();

    public GuiTribunal(int node) {
        this.node = node;
    }

    private String[] answers() {
        return TribunalDialogue.node(node).answers;
    }

    @Override
    public void updateScreen() {
        ticks++;
        if (doneAt < 0 && TribunalClient.INSTANCE.doneSpeaking()) {
            doneAt = ticks;
        }
        if (chosenAt >= 0 && ticks - chosenAt >= 6) {
            TribunalClient.INSTANCE.answer(node, selected);
        }
    }

    // ------------------------------------------------------------------ drawing

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        float t = ticks + partialTicks;
        float open = ease(Math.min(1F, t / 12F));

        // The film closes in around him.
        int bar = (int) (height * 0.085F * open);
        drawRect(0, 0, width, bar, 0xFF000000);
        drawRect(0, height - bar, width, height, 0xFF000000);

        drawSpeech(t, open, bar);
        if (TribunalDialogue.isQuestion(node)) {
            layoutCards();
            if (chosenAt < 0) {
                hover(mouseX, mouseY);
            }
            drawCards(t, bar);
        }
    }

    /** His words, in a dark panel edged with gold, his name above them. */
    private void drawSpeech(float t, float open, int bar) {
        String speech = TribunalDialogue.node(node).speech;
        int panelWidth = Math.min(width - 60, 480);
        float scale = 1.15F;
        // Wrapped once, in full, so lines never jump about as his words arrive.
        List<String> full = fontRenderer.listFormattedStringToWidth(speech, (int) ((panelWidth - 28) / scale));
        int textHeight = (int) (full.size() * 11 * scale);
        int left = (width - panelWidth) / 2;
        int top = bar + 14;
        int bottom = top + 30 + textHeight + 12;

        fillGradient(left, top, left + panelWidth, bottom, argb(0xD8, 0x170A28, open), argb(0xC8, 0x07030F, open));
        goldRule(left, top, panelWidth, open);
        goldRule(left, bottom - 1, panelWidth, open);

        String title = "T H E   L I V I N G   T R I B U N A L";
        int titleWidth = fontRenderer.getStringWidth(title);
        int titleY = top + 9;
        fontRenderer.drawString(title, (width - titleWidth) / 2, titleY, argb(0xFF, GOLD, open));
        int wing = Math.min(70, (panelWidth - titleWidth) / 2 - 16);
        if (wing > 8) {
            fadeLine((width - titleWidth) / 2 - 8 - wing, titleY + 4, wing, true, open);
            fadeLine((width + titleWidth) / 2 + 8, titleY + 4, wing, false, open);
            diamond((width - titleWidth) / 2 - 6, titleY + 4, open);
            diamond((width + titleWidth) / 2 + 5, titleY + 4, open);
        }

        GlStateManager.pushMatrix();
        GlStateManager.translate(width / 2F, top + 28F, 0F);
        GlStateManager.scale(scale, scale, 1F);
        int remaining = TribunalClient.INSTANCE.typed();
        for (int i = 0; i < full.size() && remaining > 0; i++) {
            String line = full.get(i);
            String shown = line.substring(0, Math.min(line.length(), remaining));
            remaining -= line.length() + 1;
            fontRenderer.drawStringWithShadow(shown, -fontRenderer.getStringWidth(line) / 2F, i * 11F, argb(0xFF, SPEECH, open));
        }
        GlStateManager.popMatrix();

        if (doneAt < 0) {
            float blink = 0.35F + 0.25F * MathHelper.sin(t * 0.25F);
            String hint = "click to hasten";
            fontRenderer.drawString(hint, left + panelWidth - fontRenderer.getStringWidth(hint) - 8, bottom - 11,
                    argb((int) (blink * 255F), 0x9A8BB8, open));
        }
    }

    /** Lays your answers out as cards stacked above the lower bar. */
    private void layoutCards() {
        cards.clear();
        cardLines.clear();
        String[] answers = answers();
        int cardWidth = Math.min(width - 80, 440);
        int textWidth = cardWidth - 40;
        int bar = (int) (height * 0.085F);
        int y = height - bar - 12;
        for (int i = answers.length - 1; i >= 0; i--) {
            List<String> lines = fontRenderer.listFormattedStringToWidth(answers[i], textWidth);
            int h = lines.size() * 10 + 12;
            y -= h;
            cards.add(0, new int[]{(width - cardWidth) / 2, y, cardWidth, h});
            cardLines.add(0, lines);
            y -= 5;
        }
    }

    private void drawCards(float t, int bar) {
        if (doneAt < 0) {
            return;
        }
        for (int i = 0; i < cards.size(); i++) {
            float appear = ease(MathHelper.clamp((t - doneAt - i * 3F) / 7F, 0F, 1F));
            if (appear <= 0F) {
                continue;
            }
            int[] c = cards.get(i);
            boolean on = i == selected;
            // The challenge is marked in blood red: it is not an answer, it is a refusal.
            boolean defy = TribunalDialogue.isChallenge(node, i);
            boolean chosen = on && chosenAt >= 0;
            boolean faded = chosenAt >= 0 && !on;
            float a = appear * (faded ? Math.max(0.15F, 1F - (t - chosenAt) / 5F) : 1F);
            int x = c[0] + (on ? 6 : 0) + (int) ((1F - appear) * 30F);
            int y = c[1];

            if (on) {
                // A soft gold glow behind the chosen card.
                fillGradient(x - 3, y - 3, x + c[2] + 3, y + c[3] + 3, argb(0x40, 0xE8B85A, a), argb(0x18, 0xE8B85A, a));
            }
            fillGradient(x, y, x + c[2], y + c[3], argb(on ? 0xD8 : 0xA8, on ? 0x2A1544 : 0x120A1E, a),
                    argb(on ? 0xC8 : 0x98, on ? 0x1A0C2C : 0x0A0612, a));
            drawRect(x, y, x + 2, y + c[3], argb(0xFF, defy ? (on ? 0xFF4A3A : 0xA02828) : on ? GOLD : 0x5C3C8C, a));
            if (chosen) {
                float flash = 1F - (t - chosenAt) / 6F;
                drawRect(x, y, x + c[2], y + c[3], argb((int) (0x90 * Math.max(0F, flash)), 0xFFF2CC, 1F));
            }

            String numeral = NUMERALS[Math.min(i, NUMERALS.length - 1)];
            int numeralColor = on ? GOLD_PALE : GOLD;
            int numeralX = x + 16 - fontRenderer.getStringWidth(numeral) / 2;
            fontRenderer.drawString(numeral, numeralX, y + c[3] / 2 - 4, argb(0xFF, numeralColor, a));
            drawRect(x + 28, y + 4, x + 29, y + c[3] - 4, argb(0x60, GOLD, a));

            List<String> lines = cardLines.get(i);
            for (int l = 0; l < lines.size(); l++) {
                fontRenderer.drawStringWithShadow(lines.get(l), x + 36, y + 7 + l * 10, argb(0xFF, on ? (defy ? 0xFFD0C8 : 0xFFFFFF) : defy ? 0xE09A92 : ANSWER, a));
            }
        }
        float show = ease(MathHelper.clamp((t - doneAt - cards.size() * 3F) / 8F, 0F, 1F));
        if (show > 0F && chosenAt < 0) {
            String help = "↑ ↓  choose     ENTER  answer";
            fontRenderer.drawString(help, (width - fontRenderer.getStringWidth(help)) / 2, height - bar + (bar - 8) / 2,
                    argb(0xFF, 0x7E6E9C, show));
        }
    }

    // ------------------------------------------------------------------ input

    private boolean ready() {
        return doneAt >= 0 && chosenAt < 0 && TribunalDialogue.isQuestion(node) && ticks - doneAt >= 4;
    }

    private void hover(int mouseX, int mouseY) {
        if (!ready()) {
            return;
        }
        for (int i = 0; i < cards.size(); i++) {
            int[] c = cards.get(i);
            if (mouseX >= c[0] && mouseX < c[0] + c[2] + 6 && mouseY >= c[1] && mouseY < c[1] + c[3]) {
                selected = i;
            }
        }
    }

    private void choose(int index) {
        if (ready() && index >= 0 && index < answers().length) {
            selected = index;
            chosenAt = ticks;
            mc.player.playSound(SoundEvents.UI_BUTTON_CLICK, 0.6F, 0.6F);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (!TribunalClient.INSTANCE.doneSpeaking()) {
            TribunalClient.INSTANCE.hurry();
            return;
        }
        for (int i = 0; i < cards.size(); i++) {
            int[] c = cards.get(i);
            if (mouseX >= c[0] && mouseX < c[0] + c[2] + 6 && mouseY >= c[1] && mouseY < c[1] + c[3]) {
                choose(i);
                return;
            }
        }
    }

    /** Escape does nothing. You will answer. */
    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (!TribunalClient.INSTANCE.doneSpeaking()) {
            if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_SPACE || keyCode == Keyboard.KEY_NUMPADENTER) {
                TribunalClient.INSTANCE.hurry();
            }
            return;
        }
        int count = answers().length;
        if (count == 0) {
            return;
        }
        if (keyCode == Keyboard.KEY_UP || keyCode == Keyboard.KEY_W) {
            selected = selected <= 0 ? count - 1 : selected - 1;
        } else if (keyCode == Keyboard.KEY_DOWN || keyCode == Keyboard.KEY_S) {
            selected = selected < 0 || selected >= count - 1 ? 0 : selected + 1;
        } else if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_SPACE || keyCode == Keyboard.KEY_NUMPADENTER) {
            choose(selected);
        } else if (keyCode >= Keyboard.KEY_1 && keyCode < Keyboard.KEY_1 + count) {
            choose(keyCode - Keyboard.KEY_1);
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    // ------------------------------------------------------------------ helpers

    /** A thin gold line across the panel, fading out at both ends. */
    private void goldRule(int left, int y, int w, float alpha) {
        fadeLine(left, y, w / 2, true, alpha);
        fadeLine(left + w / 2, y, w - w / 2, false, alpha);
    }

    /** A one-pixel gold line, fading toward one end. */
    private void fadeLine(int x, int y, int w, boolean fadeLeft, float alpha) {
        int solid = argb(0xE0, GOLD, alpha);
        int clear = argb(0x00, GOLD, alpha);
        horizontalGradient(x, y, x + w, y + 1, fadeLeft ? clear : solid, fadeLeft ? solid : clear);
    }

    private void diamond(int cx, int cy, float alpha) {
        int colour = argb(0xFF, GOLD, alpha);
        drawRect(cx - 1, cy - 2, cx + 1, cy + 2, colour);
        drawRect(cx - 2, cy - 1, cx + 2, cy + 1, colour);
    }

    private void fillGradient(int left, int top, int right, int bottom, int topColour, int bottomColour) {
        drawGradientRect(left, top, right, bottom, topColour, bottomColour);
    }

    private static void horizontalGradient(int left, int top, int right, int bottom, int leftColour, int rightColour) {
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        colour(buffer.pos(right, top, 0), rightColour);
        colour(buffer.pos(left, top, 0), leftColour);
        colour(buffer.pos(left, bottom, 0), leftColour);
        colour(buffer.pos(right, bottom, 0), rightColour);
        tessellator.draw();
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }

    private static void colour(BufferBuilder vertex, int argb) {
        vertex.color((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, (argb >>> 24) & 0xFF).endVertex();
    }

    /** A colour with its alpha scaled; the font renderer ignores alpha below 4, so it never goes quite to zero. */
    private static int argb(int alpha, int rgb, float scale) {
        int a = MathHelper.clamp((int) (alpha * scale), 4, 255);
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    private static float ease(float x) {
        return 1F - (1F - x) * (1F - x) * (1F - x);
    }
}
