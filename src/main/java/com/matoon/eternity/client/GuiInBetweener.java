package com.matoon.eternity.client;

import com.matoon.eternity.entity.EntityOrderChaos;
import com.matoon.eternity.world.InBetweenerDialogue;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Talking to the In-Betweener, or to his masters. His words arrive in a charcoal panel cut
 * by a seam down the middle, white and black for him, gold and blue for Master Order and
 * Lord Chaos. Your answers rise beneath it, and the one you point at turns inside out,
 * black on white. Nothing marks an answer as a lie, a mistake or a death sentence; you
 * have to know who you are talking to. There is no closing this screen; walk away by
 * choosing to.
 *
 * Once his glass has broken, a clock in the corner counts down to Order and Chaos.
 */
@SideOnly(Side.CLIENT)
public class GuiInBetweener extends GuiScreen {

    private static final String[] NUMERALS = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII"};
    private static final int SILVER = 0xD8D8DE;
    private static final int ASH = 0x8A8A92;
    private static final int BLOOD = 0xD8382E;

    private final int node;
    private final int mask;
    private int ticks;
    private int doneAt = -1;
    private int selected = -1;
    private int chosenAt = -1;
    private boolean continued;

    /** The answers you may give, by their index in the node. */
    private final List<Integer> offered = new ArrayList<>();
    private final List<int[]> cards = new ArrayList<>();
    private final List<List<String>> cardLines = new ArrayList<>();

    public GuiInBetweener(int node, int mask) {
        this.node = node;
        this.mask = mask;
        InBetweenerDialogue.Answer[] answers = InBetweenerDialogue.node(node).answers;
        for (int i = 0; i < answers.length; i++) {
            if ((mask & (1 << i)) != 0) {
                offered.add(i);
            }
        }
    }

    private boolean isQuestion() {
        return !offered.isEmpty();
    }

    private InBetweenerDialogue.Answer answer(int card) {
        return InBetweenerDialogue.node(node).answers[offered.get(card)];
    }

    @Override
    public void updateScreen() {
        ticks++;
        if (doneAt < 0 && InBetweenerClient.INSTANCE.doneSpeaking()) {
            doneAt = ticks;
        }
        if (chosenAt >= 0 && ticks - chosenAt >= 6) {
            InBetweenerClient.INSTANCE.answer(node, offered.get(selected));
        }
    }

    // ------------------------------------------------------------------ drawing

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        float t = ticks + partialTicks;
        float open = ease(Math.min(1F, t / 12F));
        int bar = (int) (height * 0.07F * open);
        drawRect(0, 0, width, bar, 0xFF000000);
        drawRect(0, height - bar, width, height, 0xFF000000);

        int speechBottom = drawSpeech(t, open, bar);
        if (isQuestion()) {
            layoutCards(speechBottom);
            if (chosenAt < 0) {
                hover(mouseX, mouseY);
            }
            drawCards(t, bar);
        } else if (doneAt >= 0 && !continued) {
            float blink = 0.45F + 0.3F * MathHelper.sin(t * 0.2F);
            String hint = "click to go on";
            fontRenderer.drawString(hint, (width - fontRenderer.getStringWidth(hint)) / 2, height - bar + (bar - 8) / 2,
                    argb((int) (blink * 255F), ASH, open));
        }
        drawWrathClock(open);
    }

    /** What is said, in a charcoal panel with a seam down the middle; returns where the panel ends. */
    private int drawSpeech(float t, float open, int bar) {
        String speech = InBetweenerDialogue.node(node).speech;
        int panelWidth = Math.min(width - 60, 500);
        float scale = height < 300 ? 1F : 1.1F;
        // Wrapped once, in full, so lines never jump about as his words arrive; the font carries each
        // voice's colour over onto the next line by itself.
        List<String> full = fontRenderer.listFormattedStringToWidth(speech, (int) ((panelWidth - 30) / scale));
        int textHeight = (int) (full.size() * 11 * scale);
        int left = (width - panelWidth) / 2;
        int top = bar + 10;
        int bottom = top + 28 + textHeight + 12;

        drawGradientRect(left, top, left + panelWidth, bottom, argb(0xE0, 0x141416, open), argb(0xD8, 0x060607, open));
        // Split down the middle: before him, his white side and his black; before his masters, Order's gold and Chaos's blue.
        boolean masters = masters();
        int leftEdge = masters ? 0xF0B840 : 0xF2F2F4, rightEdge = masters ? 0x7CC8F0 : 0x000000;
        int leftRule = masters ? 0xE8A830 : 0xF2F2F4, rightRule = masters ? 0x5AA8E0 : 0x303034;
        drawRect(left, top, left + 2, bottom, argb(0xFF, leftEdge, open));
        drawRect(left + panelWidth - 2, top, left + panelWidth, bottom, argb(0xFF, rightEdge, open));
        drawRect(left + panelWidth - 3, top, left + panelWidth - 2, bottom, argb(0x60, SILVER, open));
        drawRect(left, top, left + panelWidth / 2, top + 1, argb(0xE0, leftRule, open));
        drawRect(left + panelWidth / 2, top, left + panelWidth, top + 1, argb(0xE0, rightRule, open));
        drawRect(left, bottom - 1, left + panelWidth / 2, bottom, argb(0xE0, leftRule, open));
        drawRect(left + panelWidth / 2, bottom - 1, left + panelWidth, bottom, argb(0xE0, rightRule, open));

        // The name, in two halves, the seam between.
        String bright = masters ? "M A S T E R   O R D E R" : "T H E   I N -";
        String dim = masters ? "   ·   L O R D   C H A O S" : "  B E T W E E N E R";
        int titleWidth = fontRenderer.getStringWidth(bright + dim);
        int titleX = (width - titleWidth) / 2;
        int titleY = top + 9;
        fontRenderer.drawString(bright, titleX, titleY, argb(0xFF, masters ? 0xF0B840 : 0xFFFFFF, open));
        fontRenderer.drawString(dim, titleX + fontRenderer.getStringWidth(bright), titleY, argb(0xFF, masters ? 0x7CC8F0 : 0x707078, open));
        int wing = Math.min(70, (panelWidth - titleWidth) / 2 - 16);
        if (wing > 8) {
            drawRect(titleX - 10 - wing, titleY + 4, titleX - 10, titleY + 5, argb(0xA0, leftRule, open));
            drawRect(titleX + titleWidth + 10, titleY + 4, titleX + titleWidth + 10 + wing, titleY + 5, argb(0xA0, masters ? rightRule : 0x505058, open));
            diamond(titleX - 6, titleY + 4, leftEdge, open);
            diamond(titleX + titleWidth + 5, titleY + 4, masters ? rightEdge : 0x505058, open);
        }

        GlStateManager.pushMatrix();
        GlStateManager.translate(width / 2F, top + 27F, 0F);
        GlStateManager.scale(scale, scale, 1F);
        int remaining = InBetweenerClient.INSTANCE.doneSpeaking() ? Integer.MAX_VALUE : InBetweenerClient.INSTANCE.typed();
        for (int i = 0; i < full.size() && remaining > 0; i++) {
            String line = full.get(i);
            int visible = visibleLength(line);
            String shown = remaining >= visible ? line : visiblePrefix(line, remaining);
            remaining -= visible + 1;
            int lineWidth = fontRenderer.getStringWidth(line);
            fontRenderer.drawStringWithShadow(shown, -lineWidth / 2F, i * 11F, argb(0xFF, 0xFFFFFF, open));
        }
        GlStateManager.popMatrix();

        if (doneAt < 0) {
            float blink = 0.35F + 0.25F * MathHelper.sin(t * 0.25F);
            String hint = "click to hasten";
            fontRenderer.drawString(hint, left + panelWidth - fontRenderer.getStringWidth(hint) - 10, bottom - 11,
                    argb((int) (blink * 255F), ASH, open));
        }
        return bottom;
    }

    /** Once his glass is broken: how long until his masters arrive. */
    private void drawWrathClock(float open) {
        int left = InBetweenerClient.INSTANCE.wrathTicks();
        if (left < 0) {
            return;
        }
        int seconds = (left + 19) / 20;
        String text = String.format("ORDER AND CHAOS  %d:%02d", seconds / 60, seconds % 60);
        float pulse = 0.7F + 0.3F * MathHelper.sin(ticks * 0.3F);
        fontRenderer.drawStringWithShadow(text, width - fontRenderer.getStringWidth(text) - 8, 6, argb((int) (255F * pulse), BLOOD, open));
    }

    /** Lays your answers out as cards beneath his words, as tightly as the screen needs. */
    private void layoutCards(int speechBottom) {
        cards.clear();
        cardLines.clear();
        int cardWidth = Math.min(width - 80, 460);
        int textWidth = cardWidth - 76;
        int bar = (int) (height * 0.07F);
        int gap = 4;
        int total = 0;
        for (int i = 0; i < offered.size(); i++) {
            List<String> lines = fontRenderer.listFormattedStringToWidth(text(i), textWidth);
            cardLines.add(lines);
            total += lines.size() * 10 + 10 + gap;
        }
        int room = height - bar - 10 - (speechBottom + 8);
        if (total > room) {
            gap = 2;
        }
        int y = height - bar - 10;
        for (int i = offered.size() - 1; i >= 0; i--) {
            int h = cardLines.get(i).size() * 10 + 10;
            y -= h;
            cards.add(0, new int[]{(width - cardWidth) / 2, y, cardWidth, h});
            y -= gap;
        }
    }

    private String text(int card) {
        InBetweenerDialogue.Answer a = answer(card);
        return a.kind == InBetweenerDialogue.ACT ? "§o" + a.text : a.text;
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
            boolean chosen = on && chosenAt >= 0;
            boolean faded = chosenAt >= 0 && !on;
            float a = appear * (faded ? Math.max(0.15F, 1F - (t - chosenAt) / 5F) : 1F);
            int x = c[0] + (on ? 5 : 0) + (int) ((1F - appear) * 30F);
            int y = c[1];

            // The one you point at turns inside out: black on white. Nothing says whether it is wise.
            drawRect(x, y, x + c[2], y + c[3], argb(on ? 0xF0 : 0xB8, on ? 0xF0F0F2 : 0x111113, a));
            drawRect(x, y, x + 2, y + c[3], argb(0xFF, on ? 0x000000 : accent(), a));
            if (chosen) {
                float flash = 1F - (t - chosenAt) / 6F;
                drawRect(x, y, x + c[2], y + c[3], argb((int) (0xA0 * Math.max(0F, flash)), 0xFFFFFF, 1F));
            }
            int ink = on ? 0x0A0A0C : SILVER;

            String numeral = NUMERALS[Math.min(i, NUMERALS.length - 1)];
            fontRenderer.drawString(numeral, x + 15 - fontRenderer.getStringWidth(numeral) / 2, y + c[3] / 2 - 4, argb(0xFF, ink, a));
            drawRect(x + 27, y + 4, x + 28, y + c[3] - 4, argb(0x60, on ? 0x000000 : SILVER, a));

            List<String> lines = cardLines.get(i);
            for (int l = 0; l < lines.size(); l++) {
                if (on) {
                    fontRenderer.drawString(lines.get(l), x + 35, y + 6 + l * 10, argb(0xFF, ink, a));
                } else {
                    fontRenderer.drawStringWithShadow(lines.get(l), x + 35, y + 6 + l * 10, argb(0xFF, ink, a));
                }
            }
        }
        float show = ease(MathHelper.clamp((t - doneAt - cards.size() * 3F) / 8F, 0F, 1F));
        if (show > 0F && chosenAt < 0) {
            String help = "↑ ↓  choose     ENTER  answer";
            fontRenderer.drawString(help, (width - fontRenderer.getStringWidth(help)) / 2, height - bar + (bar - 8) / 2, argb(0xFF, 0x6A6A72, show));
        }
    }

    /** Where whoever has the line now is looking out from, for {@link DialogueCamera}. */
    @Nullable
    Vec3d speakerAt(float partialTicks) {
        InBetweenerDialogue.Node n = InBetweenerDialogue.node(node);
        if (n.speaker == InBetweenerDialogue.HIM) {
            return InBetweenerClient.INSTANCE.headAt(partialTicks);
        }
        int typed = InBetweenerClient.INSTANCE.doneSpeaking() ? Integer.MAX_VALUE : InBetweenerClient.INSTANCE.typed();
        boolean order = orderHasLine(n.speech, typed);
        EntityOrderChaos head = master(order);
        if (head == null) {
            head = master(!order);
        }
        return head == null ? null : head.centre();
    }

    /** Whether Master Order (in gold) rather than Lord Chaos (in blue) is saying the part being typed out. */
    private static boolean orderHasLine(String speech, int typed) {
        boolean order = true;
        int count = 0;
        for (int i = 0; i < speech.length(); i++) {
            if (speech.charAt(i) == '§' && i + 1 < speech.length()) {
                char code = speech.charAt(++i);
                if (code == '6') {
                    order = true;
                } else if (code == 'b') {
                    order = false;
                }
            } else if (++count >= typed) {
                break;
            }
        }
        return order;
    }

    @Nullable
    private EntityOrderChaos master(boolean order) {
        EntityOrderChaos best = null;
        for (Entity entity : mc.world.loadedEntityList) {
            if (entity instanceof EntityOrderChaos && !((EntityOrderChaos) entity).isMask() && ((EntityOrderChaos) entity).isOrder() == order
                    && (best == null || entity.getDistanceSq(mc.player) < best.getDistanceSq(mc.player))) {
                best = (EntityOrderChaos) entity;
            }
        }
        return best;
    }

    private boolean masters() {
        return InBetweenerDialogue.node(node).speaker == InBetweenerDialogue.MASTERS;
    }

    /** The edge colour of an unchosen card: silver before him, gold before his masters. */
    private int accent() {
        return masters() ? 0xC89A3A : 0x8A8A92;
    }

    // ------------------------------------------------------------------ input

    private boolean ready() {
        return doneAt >= 0 && chosenAt < 0 && isQuestion() && ticks - doneAt >= 4;
    }

    private void hover(int mouseX, int mouseY) {
        if (!ready()) {
            return;
        }
        for (int i = 0; i < cards.size(); i++) {
            int[] c = cards.get(i);
            if (mouseX >= c[0] && mouseX < c[0] + c[2] + 5 && mouseY >= c[1] && mouseY < c[1] + c[3]) {
                selected = i;
            }
        }
    }

    private void choose(int index) {
        if (ready() && index >= 0 && index < offered.size()) {
            selected = index;
            chosenAt = ticks;
            mc.player.playSound(SoundEvents.UI_BUTTON_CLICK, 0.6F, 0.8F);
        }
    }

    /** Once he has finished saying something that wants no answer, you may ask him to go on. */
    private void goOn() {
        if (!isQuestion() && doneAt >= 0 && !continued) {
            continued = true;
            InBetweenerClient.INSTANCE.goOn(node);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (!InBetweenerClient.INSTANCE.doneSpeaking()) {
            InBetweenerClient.INSTANCE.hurry();
            return;
        }
        if (!isQuestion()) {
            goOn();
            return;
        }
        for (int i = 0; i < cards.size(); i++) {
            int[] c = cards.get(i);
            if (mouseX >= c[0] && mouseX < c[0] + c[2] + 5 && mouseY >= c[1] && mouseY < c[1] + c[3]) {
                choose(i);
                return;
            }
        }
    }

    /** Escape does nothing. If you want to walk away, say so. */
    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        boolean enter = keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_SPACE || keyCode == Keyboard.KEY_NUMPADENTER;
        if (!InBetweenerClient.INSTANCE.doneSpeaking()) {
            if (enter) {
                InBetweenerClient.INSTANCE.hurry();
            }
            return;
        }
        if (!isQuestion()) {
            if (enter) {
                goOn();
            }
            return;
        }
        int count = offered.size();
        if (keyCode == Keyboard.KEY_UP || keyCode == Keyboard.KEY_W) {
            selected = selected <= 0 ? count - 1 : selected - 1;
        } else if (keyCode == Keyboard.KEY_DOWN || keyCode == Keyboard.KEY_S) {
            selected = selected < 0 || selected >= count - 1 ? 0 : selected + 1;
        } else if (enter) {
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

    /** Characters a reader sees in a line, not counting formatting codes. */
    static int visibleLength(String line) {
        int count = 0;
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) == '§' && i + 1 < line.length()) {
                i++;
            } else {
                count++;
            }
        }
        return count;
    }

    /** The start of a line holding this many visible characters, formatting codes and all. */
    private static String visiblePrefix(String line, int visible) {
        int count = 0;
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) == '§' && i + 1 < line.length()) {
                i++;
                continue;
            }
            if (count == visible) {
                return line.substring(0, i);
            }
            count++;
        }
        return line;
    }

    private void diamond(int cx, int cy, int rgb, float alpha) {
        int colour = argb(0xFF, rgb, alpha);
        drawRect(cx - 1, cy - 2, cx + 1, cy + 2, colour);
        drawRect(cx - 2, cy - 1, cx + 2, cy + 1, colour);
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
