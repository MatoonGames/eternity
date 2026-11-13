package com.matoon.eternity.client.gui;

import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.network.MsgFellowship;
import com.matoon.eternity.network.MsgFellowshipAct;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The fellowship, open: who you quest with (their faces, whether they are here, how far into the
 * story they are), who has asked you along, and everyone else in the world, each with a button to
 * ask them. One dark page in the journal's manner, edged in old gold, the fellowship's green on
 * everything that joins people together. The server keeps it current while it is open.
 */
@SideOnly(Side.CLIENT)
public class GuiFellowship extends GuiScreen {

    /** The fellowship's colour: the green of the party's voice in the speaker panel. */
    private static final int GREEN = 0x8FD6A8;
    private static final int ROW = 24, FACE = 16;

    /** What the server last said; kept between openings so the menu never opens empty. */
    private static NBTTagCompound roster = new NBTTagCompound();
    private static String status = "";
    private static int statusAt = -1000;

    /** Clickable areas this frame: x0, y0, x1, y1, and what each does. */
    private final List<float[]> boxes = new ArrayList<>();
    private final List<Runnable> actions = new ArrayList<>();
    /** Whom an invitation has just been sent to, before the server says so. */
    private final Set<UUID> pending = new HashSet<>();
    private int ticks;
    private int scroll;

    /** The server's answer: who is who, and whether to open the menu. */
    public static void receive(MsgFellowship msg) {
        roster = msg.tag;
        String s = msg.tag.getString("status");
        Minecraft mc = Minecraft.getMinecraft();
        if (!s.isEmpty()) {
            status = s;
            statusAt = mc.player != null ? mc.player.ticksExisted : 0;
        }
        if (msg.open && mc.player != null && !(mc.currentScreen instanceof GuiFellowship)) {
            mc.displayGuiScreen(new GuiFellowship());
        }
    }

    /** Opens it, asking the server for the latest. */
    public static void open() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen == null && mc.player != null) {
            mc.displayGuiScreen(new GuiFellowship());
        }
    }

    @Override
    public void initGui() {
        EternityNetwork.sendToServer(new MsgFellowshipAct(MsgFellowshipAct.LOOK, ""));
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void updateScreen() {
        ticks++;
        if (ticks % 40 == 0) {
            // Still looking: keep it current.
            EternityNetwork.sendToServer(new MsgFellowshipAct(MsgFellowshipAct.LOOK, ""));
        }
    }

    private void send(byte type, String name) {
        mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1F));
        EternityNetwork.sendToServer(new MsgFellowshipAct(type, name));
    }

    // ------------------------------------------------------------------ drawing

    /** One line of the page: a heading, a person, or a quiet note. */
    private static final class Row {
        final int kind;
        final UUID id;
        final String name, detail;
        final boolean on;
        final int button;

        Row(int kind, UUID id, String name, String detail, boolean on, int button) {
            this.kind = kind;
            this.id = id;
            this.name = name;
            this.detail = detail;
            this.on = on;
            this.button = button;
        }
    }

    private static final int HEADING = 0, PERSON = 1, NOTE = 2;
    private static final int NONE = 0, INVITE = 1, ANSWER = 2, ASKED = 3;

    private List<Row> rows() {
        List<Row> rows = new ArrayList<>();
        NBTTagList members = roster.getTagList("members", Constants.NBT.TAG_COMPOUND);
        NBTTagList asked = roster.getTagList("asked", Constants.NBT.TAG_COMPOUND);
        NBTTagList others = roster.getTagList("others", Constants.NBT.TAG_COMPOUND);
        Set<UUID> sent = new HashSet<>(pending);
        NBTTagList sentList = roster.getTagList("sent", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < sentList.tagCount(); i++) {
            sent.add(sentList.getCompoundTagAt(i).getUniqueId("id"));
        }
        rows.add(new Row(HEADING, null, members.tagCount() > 1 ? "QUESTING TOGETHER" : "YOUR FELLOWSHIP", "", false, NONE));
        if (members.tagCount() <= 1) {
            rows.add(new Row(NOTE, null, "You quest alone. Ask someone along below.", "", false, NONE));
        }
        for (int i = 0; i < members.tagCount(); i++) {
            NBTTagCompound m = members.getCompoundTagAt(i);
            if (members.tagCount() <= 1) {
                break;
            }
            String ch = m.getString("ch");
            String detail = !m.getBoolean("on") ? "away" : ch.isEmpty() ? "nothing has found them yet" : ch;
            rows.add(new Row(PERSON, m.getUniqueId("id"), m.getString("n") + (m.getBoolean("you") ? "  §7(you)" : ""), detail, m.getBoolean("on"), NONE));
        }
        if (asked.tagCount() > 0) {
            rows.add(new Row(HEADING, null, "ASKING YOU ALONG", "", false, NONE));
            for (int i = 0; i < asked.tagCount(); i++) {
                NBTTagCompound a = asked.getCompoundTagAt(i);
                rows.add(new Row(PERSON, a.getUniqueId("id"), a.getString("n"), "wants to quest with you", true, ANSWER));
            }
        }
        rows.add(new Row(HEADING, null, "ALSO IN THIS WORLD", "", false, NONE));
        if (others.tagCount() == 0) {
            rows.add(new Row(NOTE, null, "Nobody else is here right now.", "", false, NONE));
        }
        for (int i = 0; i < others.tagCount(); i++) {
            NBTTagCompound o = others.getCompoundTagAt(i);
            UUID id = o.getUniqueId("id");
            int far = o.getInteger("far");
            String detail = (far < 0 ? "in another world" : far < 16 ? "right here" : far + " blocks away")
                    + (o.getBoolean("band") ? " · has a fellowship" : "");
            rows.add(new Row(PERSON, id, o.getString("n"), detail, true, sent.contains(id) ? ASKED : INVITE));
        }
        return rows;
    }

    private static int height(Row r) {
        return r.kind == PERSON ? ROW : r.kind == HEADING ? 16 : 14;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        boxes.clear();
        actions.clear();
        int accent = StoryJournal.ACCENT;
        float in = SpeakerPanel.ease((ticks + partialTicks) / 8F);
        int w = Math.min(width - 40, 360);
        int x0 = (width - w) / 2;
        List<Row> rows = rows();
        int total = 0;
        for (Row r : rows) {
            total += height(r);
        }
        int room = Math.max(ROW * 4, height - 130);
        int view = Math.min(total, room);
        scroll = Math.max(0, Math.min(scroll, total - view));
        boolean inBand = roster.getTagList("members", Constants.NBT.TAG_COMPOUND).tagCount() > 1;
        int h = 52 + view + 34;
        float y0 = (height - h) / 2F + (1F - in) * 10F;
        int top = (int) y0 + 48;

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
        SpeakerPanel.hgrad(x0 + 16, y0 + 40, x0 + w - 16, y0 + 40.6F, accent, 0.7F * in, accent, 0F);
        if (total > view) {
            float bar = view / (float) total, at = scroll / (float) total;
            SpeakerPanel.rect(x0 + w - 8, top + view * at, x0 + w - 6, top + view * (at + bar), accent, 0.6F * in, accent, 0.6F * in);
        }

        // The rows' backgrounds and buttons, clipped to the page.
        int y = top - scroll;
        List<Object[]> faces = new ArrayList<>();
        List<Object[]> texts = new ArrayList<>();
        for (Row r : rows) {
            int rh = height(r);
            boolean shown = y + rh > top && y < top + view;
            if (shown && r.kind == HEADING) {
                texts.add(new Object[]{r.name, x0 + 16, y + 5, accent, 0.75F});
            } else if (shown && r.kind == NOTE) {
                texts.add(new Object[]{"§o" + r.name, x0 + 20, y + 2, 0x9AA4C0, 1F});
            } else if (shown && r.kind == PERSON && y >= top - 2 && y + rh <= top + view + 2) {
                boolean hover = mouseX >= x0 + 10 && mouseX < x0 + w - 10 && mouseY >= y && mouseY < y + rh - 2;
                SpeakerPanel.rect(x0 + 10, y, x0 + w - 10, y + rh - 2, hover ? 0x1C2850 : 0x111A3A, (hover ? 0.9F : 0.6F) * in,
                        0x0A1026, (hover ? 0.9F : 0.6F) * in);
                if (r.on) {
                    SpeakerPanel.rect(x0 + 10, y, x0 + 11.5F, y + rh - 2, r.button == ANSWER ? accent : GREEN, 0.9F * in, r.button == ANSWER ? accent : GREEN, 0.4F * in);
                }
                faces.add(new Object[]{r.id, x0 + 16F, y + (rh - 2 - FACE) / 2F, r.on ? 1F : 0.35F});
                texts.add(new Object[]{r.name, x0 + 38, y + 3, r.on ? 0xF2F4FA : 0x8890A8, 1F, r.id});
                texts.add(new Object[]{r.detail, x0 + 38, y + 13, r.on ? 0x9AA4C0 : 0x6A7290, 0.75F});
                float bx = x0 + w - 16;
                if (r.button == INVITE) {
                    bx = button(bx, y + 4, "Ask along", GREEN, mouseX, mouseY, in, texts, () -> {
                        pending.add(r.id);
                        send(MsgFellowshipAct.INVITE, plain(r.name));
                    });
                } else if (r.button == ASKED) {
                    texts.add(new Object[]{"§oasked", (int) bx - fontRenderer.getStringWidth("asked") * 3 / 4 - 4, y + 8, 0x7A84A0, 0.75F});
                } else if (r.button == ANSWER) {
                    bx = button(bx, y + 4, "Decline", 0xB07A7A, mouseX, mouseY, in, texts, () -> send(MsgFellowshipAct.DECLINE, plain(r.name)));
                    button(bx - 4, y + 4, "Join them", accent, mouseX, mouseY, in, texts, () -> send(MsgFellowshipAct.ACCEPT, plain(r.name)));
                }
            }
            y += rh;
        }
        // The foot: leaving, what just happened, and how to close.
        int footY = (int) (y0 + h) - 26;
        SpeakerPanel.hgrad(x0 + 16, footY - 4, x0 + w - 16, footY - 3.4F, accent, 0.35F * in, accent, 0F);
        if (inBand) {
            button(x0 + 16 + width("Leave fellowship"), footY + 2, "Leave fellowship", 0xB07A7A, mouseX, mouseY, in, texts,
                    () -> send(MsgFellowshipAct.LEAVE, ""));
        }
        GlStateManager.enableTexture2D();

        int a = (int) (Math.max(0.05F, in) * 255F) << 24;
        GlStateManager.pushMatrix();
        GlStateManager.translate(x0 + 16, y0 + 11, 0F);
        GlStateManager.scale(1.5F, 1.5F, 1F);
        fontRenderer.drawStringWithShadow("Fellowship", 0, 0, accent | a);
        GlStateManager.popMatrix();
        small("What happens to one of you in the story happens to you all.", x0 + 16, (int) y0 + 28, 0x9AA4C0 | a, 0.75F);
        for (Object[] f : faces) {
            face((UUID) f[0], (Float) f[1], (Float) f[2], (Float) f[3] * in);
        }
        for (Object[] t : texts) {
            small((String) t[0], (Integer) t[1], (Integer) t[2], (Integer) t[3] | a, (Float) t[4]);
        }
        int left = (mc.player != null ? mc.player.ticksExisted : 0) - statusAt;
        if (!status.isEmpty() && left < 120) {
            float fade = SpeakerPanel.clamp((120 - left) / 20F) * in;
            int sx = inBand ? x0 + 28 + width("Leave fellowship") : x0 + 16;
            small(status, sx, footY + 6, 0xE6D8A8 | (int) (Math.max(0.05F, fade) * 255F) << 24, 0.75F);
        }
        String foot = Keyboard.getKeyName(StoryJournal.FELLOWSHIP.getKeyCode()) + " or Esc to close";
        small(foot, (int) (x0 + w - 12 - fontRenderer.getStringWidth(foot) * 0.75F), (int) (y0 + h) - 11, 0x7A84A0 | a, 0.75F);
        GlStateManager.disableBlend();
    }

    /** A name as the server knows it, without the colour codes the page adds. */
    private static String plain(String name) {
        int cut = name.indexOf("  §7");
        return cut >= 0 ? name.substring(0, cut) : name;
    }

    private int width(String label) {
        return (int) (fontRenderer.getStringWidth(label) * 0.75F) + 12;
    }

    /** A button drawn right-aligned to x; its left edge is returned, for the next to sit beside. */
    private float button(float right, float y, String label, int colour, int mouseX, int mouseY, float in, List<Object[]> texts, Runnable run) {
        float bw = width(label), x = right - bw;
        boolean hover = mouseX >= x && mouseX < right && mouseY >= y && mouseY < y + 14;
        SpeakerPanel.rect(x, y, right, y + 14, colour, (hover ? 0.42F : 0.16F) * in, colour, (hover ? 0.3F : 0.08F) * in);
        SpeakerPanel.border(x, y, right, y + 14, colour, (hover ? 1F : 0.6F) * in);
        texts.add(new Object[]{label, (int) x + 6, (int) y + 4, hover ? 0xFFFFFF : colour, 0.75F});
        boxes.add(new float[]{x, y, right, y + 14});
        actions.add(run);
        return x - 4;
    }

    private void small(String s, int x, int y, int colour, float scale) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0F);
        GlStateManager.scale(scale, scale, 1F);
        fontRenderer.drawStringWithShadow(s, 0, 0, colour);
        GlStateManager.popMatrix();
    }

    /** A face, from their skin, with their hat layer over it. */
    private void face(UUID id, float x, float y, float alpha) {
        NetworkPlayerInfo info = mc.getConnection() != null ? mc.getConnection().getPlayerInfo(id) : null;
        ResourceLocation skin = info != null ? info.getLocationSkin() : DefaultPlayerSkin.getDefaultSkin(id);
        mc.getTextureManager().bindTexture(skin);
        GlStateManager.enableBlend();
        GlStateManager.color(1F, 1F, 1F, alpha);
        Gui.drawScaledCustomSizeModalRect((int) x, (int) y, 8F, 8F, 8, 8, FACE, FACE, 64F, 64F);
        Gui.drawScaledCustomSizeModalRect((int) x, (int) y, 40F, 8F, 8, 8, FACE, FACE, 64F, 64F);
        GlStateManager.color(1F, 1F, 1F, 1F);
    }

    // ------------------------------------------------------------------ input

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws java.io.IOException {
        if (button == 0) {
            for (int i = 0; i < boxes.size(); i++) {
                float[] b = boxes.get(i);
                if (mouseX >= b[0] && mouseX < b[2] && mouseY >= b[1] && mouseY < b[3]) {
                    actions.get(i).run();
                    return;
                }
            }
        }
        super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void handleMouseInput() throws java.io.IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            scroll += wheel > 0 ? -ROW : ROW;
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws java.io.IOException {
        if (keyCode == StoryJournal.FELLOWSHIP.getKeyCode()) {
            mc.displayGuiScreen(null);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }
}
