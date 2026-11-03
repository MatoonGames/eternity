package com.matoon.eternity.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.List;

/**
 * The story journal, on this side: what the server says the player knows (see story/Journal),
 * a key to read it, and a note at the top left of the screen whenever what they are doing moves
 * on, so nobody who skimmed a conversation is left wondering what they were told.
 */
@SideOnly(Side.CLIENT)
public final class StoryJournal {

    public static final StoryJournal INSTANCE = new StoryJournal();
    public static final KeyBinding OPEN = new KeyBinding("key.eternity.journal", Keyboard.KEY_J, "key.categories.eternity");
    /** Done reading: finish the line typing out, or move on to the next. */
    public static final KeyBinding SKIP = new KeyBinding("key.eternity.skip_line", Keyboard.KEY_RETURN, "key.categories.eternity");

    /** The journal's colour: old gold, the colour of the stones' runes. */
    static final int ACCENT = 0xD8B86A;
    private static final int TOAST_TICKS = 220;

    private final Minecraft mc = Minecraft.getMinecraft();
    String chapter = "", goal = "";
    final List<String> notes = new ArrayList<>();
    private String toastChapter = "", toastGoal = "";
    private int tick, toastAt = -10000;

    private StoryJournal() {
    }

    public void register() {
        ClientRegistry.registerKeyBinding(OPEN);
        ClientRegistry.registerKeyBinding(SKIP);
    }

    public boolean hasAnything() {
        return !goal.isEmpty() || !notes.isEmpty();
    }

    /** What the server says the journal says now. */
    public void receive(NBTTagCompound tag) {
        String was = goal;
        chapter = tag.getString("chapter");
        goal = tag.getString("goal");
        notes.clear();
        NBTTagList list = tag.getTagList("notes", Constants.NBT.TAG_STRING);
        for (int i = 0; i < list.tagCount(); i++) {
            notes.add(list.getStringTagAt(i));
        }
        if (!goal.isEmpty() && !goal.equals(was)) {
            toastChapter = chapter;
            toastGoal = goal;
            toastAt = tick;
            if (!was.isEmpty()) {
                mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_TOAST_IN, 1F));
            }
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        tick++;
        while (OPEN.isPressed()) {
            if (mc.currentScreen == null && mc.player != null) {
                mc.displayGuiScreen(new GuiStoryJournal());
            }
        }
        while (SKIP.isPressed()) {
            if (mc.currentScreen == null && mc.player != null && SpeakerPanel.INSTANCE.skip()) {
                com.matoon.eternity.network.EternityNetwork.sendToServer(new com.matoon.eternity.network.MsgStoryAct(
                        com.matoon.eternity.network.MsgStoryAct.SKIP, 0, 0));
            }
        }
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        mc.addScheduledTask(() -> {
            chapter = goal = "";
            notes.clear();
            toastAt = -10000;
        });
    }

    /** The note at the top left: the chapter, what you are doing now, and which key opens the rest. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL || mc.player == null || mc.gameSettings.hideGUI) {
            return;
        }
        float age = tick - toastAt + event.getPartialTicks();
        if (age < 0F || age > TOAST_TICKS || toastGoal.isEmpty()) {
            return;
        }
        float in = SpeakerPanel.ease(age / 10F) * (1F - SpeakerPanel.clamp((age - TOAST_TICKS + 20F) / 20F));
        if (in <= 0.02F) {
            return;
        }
        ScaledResolution res = event.getResolution();
        FontRenderer font = mc.fontRenderer;
        int w = Math.min(220, res.getScaledWidth() / 2);
        List<String> lines = font.listFormattedStringToWidth(toastGoal, w - 16);
        int h = 26 + lines.size() * 10 + 10;
        float x0 = 6 - (1F - in) * 30F, y0 = 6;
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.shadeModel(org.lwjgl.opengl.GL11.GL_SMOOTH);
        SpeakerPanel.rect(x0, y0, x0 + w, y0 + h, 0x0D1330, 0.88F * in, 0x04060E, 0.9F * in);
        SpeakerPanel.rect(x0, y0, x0 + 2, y0 + h, ACCENT, in, ACCENT, in);
        SpeakerPanel.hgrad(x0 + 2, y0 + 17, x0 + w - 8, y0 + 17.6F, ACCENT, 0.6F * in, ACCENT, 0F);
        // A pulse along the edge as it arrives.
        float flash = 1F - SpeakerPanel.clamp(age / 16F);
        if (flash > 0F) {
            SpeakerPanel.border(x0, y0, x0 + w, y0 + h, ACCENT, flash * in);
        }
        GlStateManager.enableTexture2D();
        int a = (int) (in * 255F) << 24;
        font.drawString("JOURNAL", (int) x0 + 8, (int) y0 + 5, ACCENT | a);
        String ch = toastChapter.isEmpty() ? "" : toastChapter;
        GlStateManager.pushMatrix();
        GlStateManager.translate(x0 + 8 + font.getStringWidth("JOURNAL") + 6, y0 + 6, 0F);
        GlStateManager.scale(0.75F, 0.75F, 1F);
        font.drawString("§o" + ch, 0, 0, 0x9AA4C0 | a);
        GlStateManager.popMatrix();
        int y = (int) y0 + 22;
        for (String line : lines) {
            font.drawStringWithShadow(line, x0 + 8, y, 0xF2F4FA | a);
            y += 10;
        }
        GlStateManager.pushMatrix();
        GlStateManager.translate(x0 + 8, y + 1, 0F);
        GlStateManager.scale(0.75F, 0.75F, 1F);
        font.drawString(Keyboard.getKeyName(OPEN.getKeyCode()) + ": the whole journal", 0, 0, 0x7A84A0 | a);
        GlStateManager.popMatrix();
        GlStateManager.enableAlpha();
        GlStateManager.disableBlend();
    }
}
