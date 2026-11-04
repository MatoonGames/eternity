package com.matoon.eternity.stones.client;

import com.matoon.eternity.Eternity;
import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.stones.network.MsgMindControlAct;
import com.matoon.eternity.stones.network.MsgMindControlResistance;
import lucraft.mods.lucraftcore.util.abilitybar.AbilityBarKeys;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.util.UUID;

/**
 * A player under the Mind Stone's control: they can still look around, but every other control is taken from them,
 * under a pulsing overlay and the time left. And their chance to resist, or break free: a bar with a green window to
 * press space in.
 */
public final class MindControlLock {

    public static final MindControlLock INSTANCE = new MindControlLock();

    private static final ResourceLocation OVERLAY = new ResourceLocation(Eternity.MODID, "textures/stones/mind_controlled.png");

    private boolean locked;
    private long lockEndsAt;
    private UUID token;
    private long started;
    private int duration, windowStart, windowEnd;
    private boolean escape, sent;

    private MindControlLock() {
    }

    void setLocked(boolean locked, int remainingTicks) {
        this.locked = locked;
        lockEndsAt = locked ? System.currentTimeMillis() + remainingTicks * 50L : 0L;
    }

    void startResistance(MsgMindControlResistance message) {
        token = message.token;
        duration = message.duration;
        windowStart = message.windowStart;
        windowEnd = message.windowEnd;
        escape = message.escape;
        started = System.currentTimeMillis();
        sent = false;
    }

    void finishResistance() {
        token = null;
        sent = false;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        if (Minecraft.getMinecraft().world == null) {
            locked = false;
            token = null;
        } else if (locked) {
            clearAll(Minecraft.getMinecraft());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onKey(InputEvent.KeyInputEvent event) {
        if (token != null && !sent && Keyboard.getEventKey() == Keyboard.KEY_SPACE && Keyboard.getEventKeyState()) {
            sent = true;
            EternityNetwork.sendToServer(MsgMindControlAct.resist(token, (int) ((System.currentTimeMillis() - started) / 50L)));
        }
        if (locked || token != null) {
            clearAll(Minecraft.getMinecraft());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onMouse(MouseEvent event) {
        if (locked) {
            clearAll(Minecraft.getMinecraft());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onMovement(InputUpdateEvent event) {
        if (locked) {
            event.getMovementInput().moveForward = 0F;
            event.getMovementInput().moveStrafe = 0F;
            event.getMovementInput().jump = false;
            event.getMovementInput().sneak = false;
        }
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        FontRenderer font = mc.fontRenderer;
        ScaledResolution res = event.getResolution();
        int cx = res.getScaledWidth() / 2;
        if (locked) {
            drawOverlay(mc, res);
            if (lockEndsAt > 0L) {
                String text = "MIND CONTROL  " + (int) Math.max(0L, (lockEndsAt - System.currentTimeMillis() + 999L) / 1000L) + "s";
                font.drawStringWithShadow(text, cx - font.getStringWidth(text) / 2F, res.getScaledHeight() - 58, 0xFFFFD86A);
            }
        }
        if (token == null) {
            return;
        }
        long elapsed = System.currentTimeMillis() - started;
        if (elapsed > duration * 50L + 750L) {
            token = null;
            return;
        }
        float progress = Math.max(0F, Math.min(1F, elapsed / 50F / duration));
        int width = 170, left = cx - width / 2, y = res.getScaledHeight() / 2 + 34;
        Gui.drawRect(left - 5, y - 20, left + width + 5, y + 18, 0xD008080B);
        String title = escape ? "BREAK THE MIND CONTROL" : "RESIST THE MIND CONTROL";
        font.drawStringWithShadow(title, cx - font.getStringWidth(title) / 2F, y - 16, 0xFFFFD86A);
        Gui.drawRect(left, y, left + width, y + 8, 0xFF251D2F);
        Gui.drawRect(left + (int) (width * (windowStart / (float) duration)), y, left + (int) (width * (windowEnd / (float) duration)), y + 8, 0xFF55C878);
        int marker = left + (int) (width * progress);
        Gui.drawRect(marker - 1, y - 3, marker + 2, y + 11, 0xFFFFFFFF);
        String prompt = sent ? "Attempt registered" : "Press SPACE in the green zone";
        font.drawStringWithShadow(prompt, cx - font.getStringWidth(prompt) / 2F, y + 11, sent ? 0xFFB5ADBF : 0xFFFFFFFF);
    }

    private static void drawOverlay(Minecraft mc, ScaledResolution res) {
        float alpha = 0.65F + 0.35F * (float) Math.sin(System.currentTimeMillis() / 650D);
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(1F, 1F, 1F, alpha);
        mc.getTextureManager().bindTexture(OVERLAY);
        Gui.drawScaledCustomSizeModalRect(0, 0, 0, 0, 512, 288, res.getScaledWidth(), res.getScaledHeight(), 512, 288);
        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.disableBlend();
        GlStateManager.enableDepth();
    }

    private static void clearAll(Minecraft mc) {
        if (mc.player == null) {
            return;
        }
        GameSettings settings = mc.gameSettings;
        for (KeyBinding key : new KeyBinding[]{settings.keyBindForward, settings.keyBindBack, settings.keyBindLeft, settings.keyBindRight,
                settings.keyBindJump, settings.keyBindSneak, settings.keyBindSprint, settings.keyBindAttack, settings.keyBindUseItem,
                settings.keyBindDrop, settings.keyBindInventory, AbilityBarKeys.UP, AbilityBarKeys.DOWN}) {
            clear(key);
        }
        if (AbilityBarKeys.KEYS != null) {
            for (KeyBinding key : AbilityBarKeys.KEYS) {
                clear(key);
            }
        }
        mc.player.movementInput.moveForward = 0F;
        mc.player.movementInput.moveStrafe = 0F;
        mc.player.movementInput.jump = false;
        mc.player.movementInput.sneak = false;
    }

    private static void clear(KeyBinding key) {
        if (key != null) {
            KeyBinding.setKeyBindState(key.getKeyCode(), false);
        }
    }
}
