package com.matoon.eternity.client.gui;

import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.network.MsgStoryAct;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.entity.Entity;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * What you may say back. The answers rise in a column just above the speaker panel, each on a
 * dark card edged in the colours of whoever asked; the one you point at turns light and leans
 * out a little. Something done rather than said is in italics. Nothing marks an answer as kind or
 * cruel, wise or not: they differ in what you want, not in tone.
 * <p>
 * When several of you are in the conversation, it is a vote. A strip over the cards says how
 * many have voted and, once someone has, how long the rest have; each voter's face sits on the
 * card they chose, and you can change yours until it is settled. Then the answer chosen is lit
 * and stamped with its count, the rest fall away, and it stays a moment so everyone sees what
 * the party said before the conversation goes on.
 * <p>
 * The world goes on behind it, and whoever asked stays in view; the game doesn't pause. Choose
 * with the mouse, the number keys, or up, down and enter. Escape answers with silence, where
 * silence is one of the answers, and does nothing where it isn't.
 */
@SideOnly(Side.CLIENT)
public class GuiStoryChoice extends GuiScreen {

    private static final int FACE = 9, FACE_GAP = 2, MAX_FACES = 5;
    /** How long the chosen answer is shown: the server holds the conversation about as long, for a party. */
    private static final int SHOW_SOLO = 6, SHOW_PARTY = 38;

    private static final class Vote {
        final UUID id;
        final String name;
        final int answer;

        Vote(UUID id, String name, int answer) {
            this.id = id;
            this.name = name;
            this.answer = answer;
        }
    }

    private final int talk;
    private final byte speaker;
    private final int entity;
    private final String[] answers;
    private int voters;
    private final List<int[]> cards = new ArrayList<>();
    private final List<List<String>> lines = new ArrayList<>();
    private final List<Vote> votes = new ArrayList<>();
    private int ticks;
    private int selected = -1;
    private int mine = -1, minedAt = -1;
    private int chosen = -1, chosenAt = -1;
    /** Ticks until the vote closes when last told, and when that was; -1 = nobody has voted. */
    private int left = -1, leftAt;
    private boolean keyboard;

    public GuiStoryChoice(int talk, byte speaker, int entity, String[] answers, int voters) {
        this.talk = talk;
        this.speaker = speaker;
        this.entity = entity;
        this.answers = answers;
        this.voters = Math.max(1, voters);
    }

    public int talk() {
        return talk;
    }

    private boolean party() {
        return voters > 1;
    }

    /** Where whoever asked is, so the view can stay on them. */
    @Nullable
    public Vec3d speakerAt(float partialTicks) {
        if (mc.world == null || entity < 0) {
            return null;
        }
        Entity e = mc.world.getEntityByID(entity);
        return e == null ? null : e.getPositionEyes(partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void initGui() {
        layout();
    }

    private static boolean act(String s) {
        return s.startsWith("*");
    }

    private static String text(String s) {
        return act(s) ? s.substring(1) : s;
    }

    private int facesWidth() {
        return party() ? Math.min(voters, MAX_FACES) * (FACE + FACE_GAP) + 6 : 0;
    }

    private void layout() {
        cards.clear();
        lines.clear();
        int w = Math.min(width - 40, party() ? 360 : 330);
        int x = (width - w) / 2;
        int bottom = height - 46 - 62 - 10;
        List<int[]> reversed = new ArrayList<>();
        int y = bottom;
        for (int i = answers.length - 1; i >= 0; i--) {
            List<String> wrapped = fontRenderer.listFormattedStringToWidth((act(answers[i]) ? "§o" : "") + text(answers[i]), w - 34 - facesWidth());
            int h = Math.max(8 + wrapped.size() * 10, party() ? FACE + 9 : 0);
            y -= h;
            reversed.add(0, new int[]{x, y, w, h});
            lines.add(0, wrapped);
            y -= 4;
        }
        cards.addAll(reversed);
    }

    // ------------------------------------------------------------------ what the server says

    /** How the party is voting. */
    public void votes(NBTTagCompound tag) {
        votes.clear();
        NBTTagList list = tag.getTagList("v", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound v = list.getCompoundTagAt(i);
            if (v.hasUniqueId("id")) {
                votes.add(new Vote(v.getUniqueId("id"), v.getString("n"), v.getInteger("i")));
            }
        }
        int n = Math.max(1, tag.getInteger("n"));
        if (n != voters) {
            voters = n;
            layout();
        }
        left = tag.getInteger("left");
        leftAt = ticks;
    }

    /** What was answered. */
    public void chosen(NBTTagCompound tag) {
        votes(tag);
        chosen = tag.getInteger("c");
        chosenAt = ticks;
        selected = chosen;
        if (party()) {
            mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.BLOCK_NOTE_CHIME, mine == chosen || mine < 0 ? 1.2F : 0.8F));
        }
    }

    @Override
    public void updateScreen() {
        ticks++;
        if (chosen >= 0 && ticks - chosenAt >= (party() ? SHOW_PARTY : SHOW_SOLO)) {
            mc.displayGuiScreen(null);
        }
    }

    // ------------------------------------------------------------------ drawing

    private int count(int answer) {
        int n = 0;
        for (Vote v : votes) {
            n += v.answer == answer ? 1 : 0;
        }
        return n;
    }

    /** How present a card is: rising in, then, once something is chosen, the rest falling away. */
    private float alpha(int i, float t) {
        float in = SpeakerPanel.ease((t - i * 2.5F) / 8F);
        if (chosen >= 0) {
            float out = SpeakerPanel.clamp((t - chosenAt) / 6F);
            return i == chosen ? in : in * (1F - out * (party() ? 0.8F : 1F));
        }
        if (!party() && mine >= 0 && i != mine) {
            return in * (1F - SpeakerPanel.clamp((t - minedAt) / 5F));
        }
        return in;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (cards.size() != answers.length) {
            layout();
        }
        float t = ticks + partialTicks;
        boolean open = chosen < 0 && (party() || mine < 0);
        if (open && !keyboard) {
            for (int i = 0; i < cards.size(); i++) {
                int[] c = cards.get(i);
                if (mouseX >= c[0] && mouseX < c[0] + c[2] && mouseY >= c[1] && mouseY < c[1] + c[3]) {
                    selected = i;
                }
            }
        }
        int accent = SpeakerPanel.accent(speaker);
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.shadeModel(org.lwjgl.opengl.GL11.GL_SMOOTH);
        for (int i = 0; i < cards.size(); i++) {
            int[] c = cards.get(i);
            float in = SpeakerPanel.ease((t - i * 2.5F) / 8F);
            float alpha = alpha(i, t);
            if (alpha <= 0.01F) {
                continue;
            }
            boolean won = i == chosen;
            boolean on = won || chosen < 0 && (i == selected || !party() && i == mine);
            float lean = on ? 6F : 0F;
            float x0 = c[0] - lean, y0 = c[1] + (1F - in) * 10F, x1 = c[0] + c[2] - lean, y1 = y0 + c[3];
            if (won && party()) {
                // Chosen: a pulse of the asker's colour round it, as the rest fall away.
                float pulse = 1F - SpeakerPanel.clamp((t - chosenAt) / 14F);
                SpeakerPanel.rect(x0 - 3 - pulse * 5, y0 - 3 - pulse * 5, x1 + 3 + pulse * 5, y1 + 3 + pulse * 5, accent, 0.5F * pulse + 0.15F, accent, 0.5F * pulse + 0.15F);
            }
            if (on) {
                SpeakerPanel.rect(x0 - 4, y0 - 3, x1 + 4, y1 + 3, 0x000000, 0.2F * alpha, 0x000000, 0.2F * alpha);
                SpeakerPanel.rect(x0, y0, x1, y1, 0xEEF0F6, 0.95F * alpha, 0xC8CCD8, 0.95F * alpha);
                SpeakerPanel.rect(x0, y0, x0 + 2, y1, accent, alpha, accent, alpha);
            } else {
                SpeakerPanel.rect(x0, y0, x1, y1, 0x0D1020, 0.86F * alpha, 0x05060C, 0.9F * alpha);
                SpeakerPanel.border(x0, y0, x1, y1, accent, (i == mine ? 0.9F : 0.45F) * alpha);
                if (i == mine) {
                    // Your vote, while it isn't the one being pointed at.
                    SpeakerPanel.rect(x0, y0, x0 + 2, y1, accent, alpha, accent, alpha);
                }
            }
            // The number to press, in a small diamond.
            SpeakerPanel.diamond(x0 + 11, y0 + c[3] / 2F, 6F, on ? 0x10121C : accent, (on ? 0.9F : 0.55F) * alpha);
            if (party()) {
                // Behind the faces, a groove for each vote there could be.
                float fx = x1 - 4 - Math.min(voters, MAX_FACES) * (FACE + FACE_GAP);
                float fy = y0 + (c[3] - FACE) / 2F;
                for (int k = 0; k < Math.min(voters, MAX_FACES); k++) {
                    float gx = fx + k * (FACE + FACE_GAP);
                    SpeakerPanel.rect(gx, fy, gx + FACE, fy + FACE, on ? 0x9098AA : 0x1A1E30, 0.5F * alpha, on ? 0x9098AA : 0x1A1E30, 0.5F * alpha);
                }
            }
        }
        if (party()) {
            strip(t, accent);
        }
        GlStateManager.enableTexture2D();
        String hover = null;
        for (int i = 0; i < cards.size(); i++) {
            int[] c = cards.get(i);
            float in = SpeakerPanel.ease((t - i * 2.5F) / 8F);
            float alpha = alpha(i, t);
            if (alpha <= 0.05F) {
                continue;
            }
            boolean won = i == chosen;
            boolean on = won || chosen < 0 && (i == selected || !party() && i == mine);
            int a = (int) (Math.max(0.05F, alpha) * 255F) << 24;
            float lean = on ? 6F : 0F;
            float y0 = c[1] + (1F - in) * 10F;
            String n = String.valueOf(i + 1);
            fontRenderer.drawString(n, (int) (c[0] - lean + 11 - fontRenderer.getStringWidth(n) / 2F), (int) (y0 + c[3] / 2F - 3.5F),
                    (on ? accent : 0x0B0D16) | a);
            int y = (int) (y0 + (c[3] - lines.get(i).size() * 10) / 2F) + 1;
            int color = on ? 0x10121C : act(answers[i]) ? 0xA8ADBC : 0xE8EAF2;
            for (String row : lines.get(i)) {
                if (on) {
                    fontRenderer.drawString(row, (int) (c[0] - lean + 24), y, color | a);
                } else {
                    fontRenderer.drawStringWithShadow(row, c[0] - lean + 24, y, color | a);
                }
                y += 10;
            }
            if (party()) {
                // The faces of whoever voted for it, in the order they did.
                float fx = c[0] + c[2] - lean - 4 - Math.min(voters, MAX_FACES) * (FACE + FACE_GAP);
                float fy = y0 + (c[3] - FACE) / 2F;
                int k = 0;
                for (Vote v : votes) {
                    if (v.answer != i || k >= MAX_FACES) {
                        continue;
                    }
                    float gx = fx + k * (FACE + FACE_GAP);
                    face(v.id, gx, fy, alpha);
                    if (mouseX >= gx && mouseX < gx + FACE && mouseY >= fy && mouseY < fy + FACE) {
                        hover = v.name;
                    }
                    k++;
                }
                if (won) {
                    // Stamped: CHOSEN, and how many chose it.
                    float stamp = SpeakerPanel.ease((t - chosenAt) / 6F);
                    String s = "CHOSEN · " + count(i) + " of " + voters;
                    int sw = fontRenderer.getStringWidth(s);
                    GlStateManager.pushMatrix();
                    GlStateManager.translate(c[0] + c[2] - lean - 2, y0 - 9 + (1F - stamp) * 4F, 0F);
                    GlStateManager.scale(0.75F, 0.75F, 1F);
                    fontRenderer.drawStringWithShadow(s, -sw, 0, accent | (int) (stamp * 255F) << 24);
                    GlStateManager.popMatrix();
                }
            }
        }
        if (hover != null && chosen < 0) {
            int hw = fontRenderer.getStringWidth(hover);
            GlStateManager.disableTexture2D();
            SpeakerPanel.rect(mouseX + 6, mouseY - 12, mouseX + 10 + hw, mouseY - 1, 0x05060C, 0.9F, 0x05060C, 0.9F);
            GlStateManager.enableTexture2D();
            fontRenderer.drawString(hover, mouseX + 8, mouseY - 10, 0xE8EAF2);
        }
        // How to answer, faintly, once, under the cards.
        if (ticks < 120 && chosen < 0) {
            float hint = SpeakerPanel.clamp(ticks / 20F) * (1F - SpeakerPanel.clamp((ticks - 90) / 30F)) * 0.6F;
            String s = "1-" + answers.length + ", or click" + (party() ? "; you can change your vote" : "");
            int last = cards.isEmpty() ? height / 2 : cards.get(cards.size() - 1)[1] + cards.get(cards.size() - 1)[3];
            fontRenderer.drawString(s, (width - fontRenderer.getStringWidth(s)) / 2, last + 3, 0x9AA0B4 | (int) (hint * 255F) << 24);
        }
        GlStateManager.disableBlend();
    }

    /** Over the cards, for a party: how many have voted, and how long the rest have. */
    private void strip(float t, int accent) {
        if (cards.isEmpty()) {
            return;
        }
        int[] top = cards.get(0);
        float in = SpeakerPanel.ease(t / 8F);
        float y1 = top[1] - 5, y0 = y1 - 13;
        float x0 = top[0], x1 = top[0] + top[2];
        SpeakerPanel.hgrad(x0, y0, (x0 + x1) / 2F, y1, 0x05060C, 0F, 0x05060C, 0.8F * in);
        SpeakerPanel.hgrad((x0 + x1) / 2F, y0, x1, y1, 0x05060C, 0.8F * in, 0x05060C, 0F);
        if (left >= 0 && chosen < 0) {
            // The window after the first vote, running out.
            float rem = Math.max(0F, left - (t - leftAt)) / VOTE_WINDOW;
            float mid = (x0 + x1) / 2F, half = (x1 - x0) / 2F * rem;
            SpeakerPanel.rect(mid - half, y1 - 1.5F, mid + half, y1 - 0.5F, accent, 0.8F * in, accent, 0.8F * in);
        }
        GlStateManager.enableTexture2D();
        String s;
        if (chosen >= 0) {
            s = "THE PARTY HAS ANSWERED";
        } else {
            s = "THE PARTY DECIDES  ·  " + votes.size() + " of " + voters + " voted";
            if (left >= 0) {
                int secs = (int) Math.ceil(Math.max(0F, left - (t - leftAt)) / 20F);
                s += "  ·  " + secs + "s";
            }
        }
        int sw = fontRenderer.getStringWidth(s);
        GlStateManager.pushMatrix();
        GlStateManager.translate((x0 + x1) / 2F, y0 + 3.5F, 0F);
        GlStateManager.scale(0.75F, 0.75F, 1F);
        fontRenderer.drawString(s, -sw / 2, 0, (chosen >= 0 ? accent : 0xC8CCD8) | (int) (in * 255F) << 24);
        GlStateManager.popMatrix();
        GlStateManager.disableTexture2D();
    }

    /** Matches the server's window (Talk.VOTE_WINDOW), for drawing it running out. */
    static final int VOTE_WINDOW = 300;

    /** A voter's face, from their skin, with their hat layer over it. */
    private void face(UUID id, float x, float y, float alpha) {
        NetworkPlayerInfo info = mc.getConnection() != null ? mc.getConnection().getPlayerInfo(id) : null;
        ResourceLocation skin = info != null ? info.getLocationSkin() : DefaultPlayerSkin.getDefaultSkin(id);
        mc.getTextureManager().bindTexture(skin);
        GlStateManager.color(1F, 1F, 1F, alpha);
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0F);
        Gui.drawScaledCustomSizeModalRect(0, 0, 8F, 8F, 8, 8, FACE, FACE, 64F, 64F);
        Gui.drawScaledCustomSizeModalRect(0, 0, 40F, 8F, 8, 8, FACE, FACE, 64F, 64F);
        GlStateManager.popMatrix();
        GlStateManager.color(1F, 1F, 1F, 1F);
    }

    // ------------------------------------------------------------------ answering

    private void choose(int i) {
        if (chosen >= 0 || i < 0 || i >= answers.length || i == mine || !party() && mine >= 0) {
            return;
        }
        selected = i;
        mine = i;
        minedAt = ticks;
        mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 0.7F));
        EternityNetwork.sendToServer(new MsgStoryAct(MsgStoryAct.ANSWER, talk, i));
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) {
            return;
        }
        for (int i = 0; i < cards.size(); i++) {
            int[] c = cards.get(i);
            if (mouseX >= c[0] - 6 && mouseX < c[0] + c[2] && mouseY >= c[1] && mouseY < c[1] + c[3]) {
                choose(i);
                return;
            }
        }
    }

    @Override
    public void handleMouseInput() throws java.io.IOException {
        super.handleMouseInput();
        if (org.lwjgl.input.Mouse.getEventDX() != 0 || org.lwjgl.input.Mouse.getEventDY() != 0) {
            keyboard = false;
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (keyCode >= Keyboard.KEY_1 && keyCode <= Keyboard.KEY_9) {
            choose(keyCode - Keyboard.KEY_1);
        } else if (keyCode == Keyboard.KEY_UP || keyCode == Keyboard.KEY_W) {
            keyboard = true;
            selected = selected <= 0 ? answers.length - 1 : selected - 1;
        } else if (keyCode == Keyboard.KEY_DOWN || keyCode == Keyboard.KEY_S) {
            keyboard = true;
            selected = selected < 0 || selected >= answers.length - 1 ? 0 : selected + 1;
        } else if ((keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER || keyCode == Keyboard.KEY_SPACE) && selected >= 0) {
            choose(selected);
        } else if (keyCode == Keyboard.KEY_ESCAPE) {
            // Silence, if silence is one of the answers.
            for (int i = answers.length - 1; i >= 0; i--) {
                String s = text(answers[i]).toLowerCase();
                if (act(answers[i]) && (s.contains("nothing") || s.contains("step back") || s.contains("let her go") || s.contains("leave"))) {
                    choose(i);
                    return;
                }
            }
        }
    }
}
