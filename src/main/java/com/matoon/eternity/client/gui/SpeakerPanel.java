package com.matoon.eternity.client.gui;

import com.matoon.eternity.client.Cutscene;
import com.matoon.eternity.client.render.RenderAdamWarlock;
import com.matoon.eternity.client.render.RenderGrandmaster;
import com.matoon.eternity.world.GrandmasterScenes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.List;

/**
 * Who is speaking, and what they say, at the foot of the screen without stopping anything: a
 * dark panel trimmed in the speaker's colours, a window in it onto the speaker themself (the
 * real one, drawn live, so the Grandmaster shuffles and laughs in his frame as he does on his
 * board), their name and what they are, and the line typed out a few letters at a time. A new
 * speaker slides in as the last one fades; the same speaker only changes his words. Stage
 * directions have no panel: a line in grey italics between two fading rules.
 */
@SideOnly(Side.CLIENT)
public final class SpeakerPanel {

    public static final SpeakerPanel INSTANCE = new SpeakerPanel();

    private static final float CHARS_PER_TICK = 1.6F;
    private static final int FADE_IN = 7, FADE_OUT = 9;

    private final Minecraft mc = Minecraft.getMinecraft();
    private Line current, leaving;
    private int tick;

    /** Who each speaker is, and their colours: accent, the window's dark and light. */
    private static final class Who {
        final String name, title;
        final int accent, deep, light;

        Who(String name, String title, int accent, int deep, int light) {
            this.name = name;
            this.title = title;
            this.accent = accent;
            this.deep = deep;
            this.light = light;
        }
    }

    private static final Who GRANDMASTER = new Who("THE GRANDMASTER", "En Dwi Gast, Elder of the Universe", 0xF2C24A, 0x081436, 0x2C64B4);
    private static final Who WARLOCK = new Who("ADAM WARLOCK", "Son of the Sovereign", 0xF4A84A, 0x2A0408, 0x9A2A22);

    private static final class Line {
        final byte speaker;
        final int entity;
        final String text;
        final int born;
        final int life;
        List<String> wrapped;
        int leftAt = -1;

        Line(byte speaker, int entity, String text, int born) {
            this.speaker = speaker;
            this.entity = entity;
            this.text = text;
            this.born = born;
            // Long enough to type, and to read twice over.
            this.life = (int) (text.length() / CHARS_PER_TICK) + 50 + text.length();
        }

        Who who() {
            return speaker == GrandmasterScenes.WARLOCK ? WARLOCK : GRANDMASTER;
        }
    }

    private SpeakerPanel() {
    }

    /** Someone says something. */
    public void say(byte speaker, int entity, String text) {
        if (current != null) {
            current.leftAt = tick;
            leaving = current;
        }
        current = new Line(speaker, entity, text, tick);
    }

    public void clear() {
        current = leaving = null;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || mc.isGamePaused()) {
            return;
        }
        tick++;
        if (current != null && tick - current.born > current.life) {
            current.leftAt = tick;
            leaving = current;
            current = null;
        }
        if (leaving != null && tick - leaving.leftAt > FADE_OUT) {
            leaving = null;
        }
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        mc.addScheduledTask(this::clear);
    }

    // ------------------------------------------------------------------ drawing

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL || mc.world == null) {
            return;
        }
        ScaledResolution res = event.getResolution();
        float pt = event.getPartialTicks();
        Cutscene cut = Cutscene.INSTANCE;
        cut.drawUnder(res, pt);
        int bottom = cut.barHeight(res, pt) > 1F ? (int) cut.barHeight(res, pt) + 8 : 46;
        boolean sameSpeaker = leaving != null && current != null && leaving.speaker == current.speaker && leaving.entity == current.entity;
        if (leaving != null && !sameSpeaker) {
            float out = 1F - clamp((tick - leaving.leftAt + pt) / FADE_OUT);
            draw(res, leaving, out, sameSpeaker ? 1F : out, bottom, pt, !sameSpeaker);
        }
        if (current != null) {
            float in = clamp((tick - current.born + pt) / FADE_IN);
            float out = 1F - clamp((tick - current.born + pt - current.life + FADE_OUT) / FADE_OUT);
            draw(res, current, Math.min(in, out), sameSpeaker ? 1F : Math.min(in, out), bottom, pt, !sameSpeaker || in >= 1F);
        }
        cut.drawOver(res, pt);
    }

    /**
     * One line. {@code alpha} is the words; {@code frame} is the panel round them, which stays
     * put when the same speaker goes on talking.
     */
    private void draw(ScaledResolution res, Line line, float alpha, float frame, int bottom, float pt, boolean drawFrame) {
        if (alpha <= 0.01F && frame <= 0.01F) {
            return;
        }
        FontRenderer font = mc.fontRenderer;
        int w = res.getScaledWidth(), h = res.getScaledHeight();
        if (line.speaker == GrandmasterScenes.DIRECTION) {
            direction(font, line, w, h - bottom - 14, alpha, pt);
            return;
        }
        Who who = line.who();
        int panelW = Math.min(w - 24, 372), panelH = 62;
        float rise = (1F - ease(frame)) * 14F;
        int x0 = (w - panelW) / 2;
        float y0 = h - bottom - panelH + rise;
        int portrait = panelH - 10;
        int textX = x0 + portrait + 16, textW = panelW - portrait - 26;
        if (line.wrapped == null) {
            line.wrapped = font.listFormattedStringToWidth(line.text, textW);
        }

        GlStateManager.pushMatrix();
        GlStateManager.translate(0F, y0, 0F);
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        if (drawFrame) {
            // A soft shadow to sit on, then the panel: deep blue-black, a breath lighter at the top.
            rect(x0 - 6, -6, x0 + panelW + 6, panelH + 6, 0x000000, 0.28F * frame, 0x000000, 0.0F * frame);
            rect(x0, 0, x0 + panelW, panelH, 0x0D1330, 0.92F * frame, 0x04060E, 0.94F * frame);
            // The trim: the speaker's colour, a light running along the top edge.
            border(x0, 0, x0 + panelW, panelH, who.accent, 0.75F * frame);
            float run = ((tick + pt) * 0.012F) % 1F;
            float rx = x0 + panelW * run;
            hgrad(rx - 40, -0.5F, rx, 1F, who.accent, 0F, 0xFFFFFF, 0.9F * frame);
            hgrad(rx, -0.5F, rx + 40, 1F, 0xFFFFFF, 0.9F * frame, who.accent, 0F);
            for (int[] c : new int[][]{{x0, 0}, {x0 + panelW, 0}, {x0, panelH}, {x0 + panelW, panelH}}) {
                diamond(c[0], c[1], 3F, who.accent, frame);
            }
            portraitWindow(line, who, x0 + 5, 5, portrait, frame, pt, y0);
        }

        GlStateManager.enableTexture2D();
        // The name, spaced out, the title under it in grey, a rule fading out to the right.
        int a = (int) (255F * Math.max(0.02F, frame));
        drawSpaced(font, who.name, textX, 7, who.accent | a << 24);
        GlStateManager.pushMatrix();
        GlStateManager.translate(textX, 17, 0F);
        GlStateManager.scale(0.75F, 0.75F, 1F);
        font.drawString("§o" + who.title, 0, 0, 0x9AA4C0 | a << 24);
        GlStateManager.popMatrix();
        GlStateManager.disableTexture2D();
        hgrad(textX, 25, textX + textW, 25.6F, who.accent, 0.6F * frame, who.accent, 0F);

        // Speaking: three bars beside the name rising and falling while the words come.
        int typed = (int) ((tick - line.born + pt) * CHARS_PER_TICK);
        boolean speaking = typed < line.text.length() && line.leftAt < 0;
        int nameW = spacedWidth(font, who.name);
        for (int i = 0; i < 3; i++) {
            float bar = speaking ? 1.5F + 3.5F * Math.abs(MathHelper.sin((tick + pt) * 0.55F + i * 1.3F)) : 1F;
            rect(textX + nameW + 6 + i * 3, 13 - bar, textX + nameW + 8 + i * 3, 13, who.accent, frame, who.accent, frame);
        }
        // How long the line has left, as a hairline along the foot of the panel.
        if (line.leftAt < 0) {
            float left = 1F - clamp((tick - line.born + pt) / line.life);
            hgrad(x0 + 1, panelH - 1.5F, x0 + 1 + (panelW - 2) * left, panelH - 0.5F, who.accent, 0.35F * frame, who.accent, 0.1F * frame);
        }
        GlStateManager.enableTexture2D();

        // The words, typed out.
        int shown = Math.min(line.text.length(), Math.max(0, typed));
        int y = 31;
        int used = 0;
        int ta = (int) (255F * Math.max(0.02F, alpha));
        for (String row : line.wrapped) {
            if (used >= shown) {
                break;
            }
            String part = row.length() <= shown - used ? row : row.substring(0, shown - used);
            font.drawStringWithShadow(part, textX, y, 0xF2F4FA | ta << 24);
            used += row.length() + (row.length() < line.text.length() ? 1 : 0);
            y += 10;
        }
        GlStateManager.enableAlpha();
        GlStateManager.popMatrix();
    }

    /** The window onto the speaker: a lit backdrop in their colours, and them, live, from the chest up. */
    private void portraitWindow(Line line, Who who, int x, int y, int size, float alpha, float pt, float panelY) {
        // Backdrop: their colours, lighter toward the middle, slow rays turning behind them.
        rect(x, y, x + size, y + size, who.light, alpha, who.deep, alpha);
        float cx = x + size / 2F, cy = y + size * 0.42F;
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        Tessellator t = Tessellator.getInstance();
        BufferBuilder b = t.getBuffer();
        startScissor(x, y + panelY, size, size);
        b.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
        float spin = (tick + pt) * 0.006F;
        for (int i = 0; i < 12; i++) {
            double a0 = spin + i * Math.PI / 6D, a1 = a0 + 0.13D;
            float r = (who.accent >> 16 & 255) / 255F, g = (who.accent >> 8 & 255) / 255F, bl = (who.accent & 255) / 255F;
            b.pos(cx, cy, 0).color(r, g, bl, 0.22F * alpha).endVertex();
            b.pos(cx + Math.cos(a0) * size, cy + Math.sin(a0) * size, 0).color(r, g, bl, 0F).endVertex();
            b.pos(cx + Math.cos(a1) * size, cy + Math.sin(a1) * size, 0).color(r, g, bl, 0F).endVertex();
        }
        t.draw();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        Entity e = mc.world.getEntityByID(line.entity);
        if (e instanceof EntityLivingBase && alpha > 0.3F) {
            portrait((EntityLivingBase) e, cx, y + size * 0.47F, size, pt, line.speaker);
        } else {
            // Nobody to show: their initial, in their colour.
            GlStateManager.enableTexture2D();
            String initial = who.name.equals(GRANDMASTER.name) ? "G" : "W";
            GlStateManager.pushMatrix();
            GlStateManager.translate(cx, cy, 0F);
            GlStateManager.scale(3F, 3F, 1F);
            mc.fontRenderer.drawString(initial, -mc.fontRenderer.getStringWidth(initial) / 2, -4, who.accent | (int) (alpha * 255) << 24);
            GlStateManager.popMatrix();
            GlStateManager.disableTexture2D();
        }
        endScissor();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        // A vignette inside the frame, and the frame itself, brighter while they speak.
        vgrad(x, y + size * 0.65F, x + size, y + size, 0x000000, 0F, 0x000000, 0.55F * alpha);
        border(x, y, x + size, y + size, who.accent, alpha);
        border(x + 1, y + 1, x + size - 1, y + size - 1, 0x000000, 0.6F * alpha);
    }

    /** The speaker themself, drawn the way the inventory draws you, turned a little toward us, framed on the face. */
    private void portrait(EntityLivingBase e, float cx, float faceY, int size, float pt, byte speaker) {
        float face = speaker == GrandmasterScenes.WARLOCK ? 1.72F : 1.98F;
        float scale = size * 0.78F;
        float yawWas = e.rotationYaw, prevYawWas = e.prevRotationYaw, bodyWas = e.renderYawOffset, prevBodyWas = e.prevRenderYawOffset;
        float headWas = e.rotationYawHead, prevHeadWas = e.prevRotationYawHead, pitchWas = e.rotationPitch, prevPitchWas = e.prevRotationPitch;
        GlStateManager.enableTexture2D();
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT);
        GlStateManager.enableColorMaterial();
        GlStateManager.pushMatrix();
        GlStateManager.translate(cx, faceY + face * scale, 200F);
        GlStateManager.scale(-scale, scale, scale);
        GlStateManager.rotate(180F, 0F, 0F, 1F);
        // Three-quarters on, his head toward us.
        e.renderYawOffset = e.prevRenderYawOffset = 18F;
        e.rotationYaw = e.prevRotationYaw = 18F;
        e.rotationYawHead = e.prevRotationYawHead = 6F;
        e.rotationPitch = e.prevRotationPitch = 0F;
        GlStateManager.rotate(135F, 0F, 1F, 0F);
        RenderHelper.enableStandardItemLighting();
        GlStateManager.rotate(-135F, 0F, 1F, 0F);
        RenderManager rm = mc.getRenderManager();
        rm.setPlayerViewY(180F);
        rm.setRenderShadow(false);
        RenderGrandmaster.portrait = RenderAdamWarlock.portrait = true;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
        try {
            rm.renderEntity(e, 0D, 0D, 0D, 0F, pt, false);
        } finally {
            RenderGrandmaster.portrait = RenderAdamWarlock.portrait = false;
            rm.setRenderShadow(true);
            e.rotationYaw = yawWas;
            e.prevRotationYaw = prevYawWas;
            e.renderYawOffset = bodyWas;
            e.prevRenderYawOffset = prevBodyWas;
            e.rotationYawHead = headWas;
            e.prevRotationYawHead = prevHeadWas;
            e.rotationPitch = pitchWas;
            e.prevRotationPitch = prevPitchWas;
        }
        GlStateManager.popMatrix();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.disableTexture2D();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.color(1F, 1F, 1F, 1F);
    }

    /** What nobody says: grey italics, centred, between two rules that fade into nothing. */
    private void direction(FontRenderer font, Line line, int w, float y, float alpha, float pt) {
        int typed = (int) ((tick - line.born + pt) * CHARS_PER_TICK * 1.5F);
        String text = "§o" + line.text.substring(0, Math.min(line.text.length(), Math.max(0, typed)));
        int full = font.getStringWidth("§o" + line.text);
        int x = (w - full) / 2;
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        rect(x - 40, y - 5, x + full + 40, y + 13, 0x000000, 0F, 0x000000, 0F);
        hgrad(x - 60, y + 3.5F, x - 8, y + 4.2F, 0xC8CCD8, 0F, 0xC8CCD8, 0.6F * alpha);
        hgrad(x + full + 8, y + 3.5F, x + full + 60, y + 4.2F, 0xC8CCD8, 0.6F * alpha, 0xC8CCD8, 0F);
        GlStateManager.enableTexture2D();
        font.drawStringWithShadow(text, x, y, 0xC8CCD8 | (int) (Math.max(0.02F, alpha) * 255) << 24);
    }

    // ------------------------------------------------------------------ drawing helpers

    private static void drawSpaced(FontRenderer font, String s, int x, int y, int argb) {
        int cx = x;
        for (char c : s.toCharArray()) {
            String ch = String.valueOf(c);
            font.drawStringWithShadow(ch, cx, y, argb);
            cx += font.getStringWidth(ch) + (c == ' ' ? 2 : 1);
        }
    }

    private static int spacedWidth(FontRenderer font, String s) {
        int w = 0;
        for (char c : s.toCharArray()) {
            w += font.getStringWidth(String.valueOf(c)) + (c == ' ' ? 2 : 1);
        }
        return w;
    }

    private void startScissor(float x, float y, float w, float h) {
        ScaledResolution res = new ScaledResolution(mc);
        int f = res.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((int) (x * f), (int) (mc.displayHeight - (y + h) * f), (int) (w * f), (int) (h * f));
    }

    private static void endScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    static void rect(float x0, float y0, float x1, float y1, int top, float topA, int bottom, float bottomA) {
        vgrad(x0, y0, x1, y1, top, topA, bottom, bottomA);
    }

    static void vgrad(float x0, float y0, float x1, float y1, int top, float topA, int bottom, float bottomA) {
        Tessellator t = Tessellator.getInstance();
        BufferBuilder b = t.getBuffer();
        b.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        b.pos(x1, y0, 0).color(r(top), g(top), bl(top), topA).endVertex();
        b.pos(x0, y0, 0).color(r(top), g(top), bl(top), topA).endVertex();
        b.pos(x0, y1, 0).color(r(bottom), g(bottom), bl(bottom), bottomA).endVertex();
        b.pos(x1, y1, 0).color(r(bottom), g(bottom), bl(bottom), bottomA).endVertex();
        t.draw();
    }

    static void hgrad(float x0, float y0, float x1, float y1, int left, float leftA, int right, float rightA) {
        Tessellator t = Tessellator.getInstance();
        BufferBuilder b = t.getBuffer();
        b.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        b.pos(x1, y0, 0).color(r(right), g(right), bl(right), rightA).endVertex();
        b.pos(x0, y0, 0).color(r(left), g(left), bl(left), leftA).endVertex();
        b.pos(x0, y1, 0).color(r(left), g(left), bl(left), leftA).endVertex();
        b.pos(x1, y1, 0).color(r(right), g(right), bl(right), rightA).endVertex();
        t.draw();
    }

    static void border(float x0, float y0, float x1, float y1, int rgb, float a) {
        rect(x0, y0, x1, y0 + 1, rgb, a, rgb, a);
        rect(x0, y1 - 1, x1, y1, rgb, a, rgb, a);
        rect(x0, y0, x0 + 1, y1, rgb, a, rgb, a);
        rect(x1 - 1, y0, x1, y1, rgb, a, rgb, a);
    }

    static void diamond(float x, float y, float r, int rgb, float a) {
        Tessellator t = Tessellator.getInstance();
        BufferBuilder b = t.getBuffer();
        b.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        b.pos(x, y - r, 0).color(r(rgb), g(rgb), bl(rgb), a).endVertex();
        b.pos(x - r, y, 0).color(r(rgb), g(rgb), bl(rgb), a).endVertex();
        b.pos(x, y + r, 0).color(r(rgb), g(rgb), bl(rgb), a).endVertex();
        b.pos(x + r, y, 0).color(r(rgb), g(rgb), bl(rgb), a).endVertex();
        t.draw();
    }

    private static float r(int c) {
        return (c >> 16 & 255) / 255F;
    }

    private static float g(int c) {
        return (c >> 8 & 255) / 255F;
    }

    private static float bl(int c) {
        return (c & 255) / 255F;
    }

    static float clamp(float x) {
        return x < 0F ? 0F : x > 1F ? 1F : x;
    }

    static float ease(float x) {
        x = clamp(x);
        return 1F - (1F - x) * (1F - x) * (1F - x);
    }
}
