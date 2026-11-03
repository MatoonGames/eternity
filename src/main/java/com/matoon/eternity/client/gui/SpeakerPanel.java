package com.matoon.eternity.client.gui;

import com.matoon.eternity.client.Cutscene;
import com.matoon.eternity.client.render.RenderAdamWarlock;
import com.matoon.eternity.client.render.RenderGrandmaster;
import com.matoon.eternity.client.render.RenderNorse;
import com.matoon.eternity.story.Speakers;
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

    /**
     * Who each speaker is: their name and what they are, their colours (accent, the window's dark
     * and light), the letter shown when they cannot be, and how far down the window their face sits.
     */
    private static final class Who {
        final String name, title, initial;
        final int accent, deep, light;
        final float face;

        Who(String name, String title, int accent, int deep, int light, String initial, float face) {
            this.name = name;
            this.title = title;
            this.accent = accent;
            this.deep = deep;
            this.light = light;
            this.initial = initial;
            this.face = face;
        }
    }

    private static final Who GRANDMASTER = new Who("THE GRANDMASTER", "En Dwi Gast, Elder of the Universe", 0xF2C24A, 0x081436, 0x2C64B4, "G", 1.98F);
    private static final Who WARLOCK = new Who("ADAM WARLOCK", "Son of the Sovereign", 0xF4A84A, 0x2A0408, 0x9A2A22, "W", 1.72F);
    private static final Who STRANGER = new Who("THE STRANGER", "A woman in mourning", 0xC9CED8, 0x050609, 0x2E323E, "?", 1.66F);
    private static final Who RAGNA = new Who("RAGNA", "Eirik's daughter", 0x9CC4DE, 0x06121C, 0x2E5670, "R", 1.62F);
    private static final Who HRAFN = new Who("HRAFN", "Keeper of the oath", 0xD09A56, 0x140B04, 0x5C3E20, "H", 1.66F);
    private static final Who DEATH = new Who("DEATH", "", 0xE8E8EE, 0x000000, 0x24242C, "", 1.66F);
    private static final Who STONE = new Who("CARVED IN THE STONE", "A keeper's grave", 0xA9B4BE, 0x0E1012, 0x3E464E, "", 1.7F);
    private static final Who CHAMPION = new Who("THE CHAMPION", "Tryco Slatterus, Elder of the Universe", 0xE8A23A, 0x220806, 0x8A2A14, "C", 2.2F);
    private static final Who PARTY = new Who("YOUR PARTY", "Chosen together", 0x8FD6A8, 0x04140C, 0x245A3C, "P", 1.62F);
    private static final Who IN_BETWEENER = new Who("THE IN-BETWEENER", "Made by Order and Chaos", 0xD8D8E0, 0x0A0A10, 0x4A4A56, "I", 1.62F);
    private static final Who GARDENER = new Who("THE GARDENER", "Ord Zyonz, Elder of the Universe", 0x9CCB62, 0x0A1606, 0x3E6A24, "G", 1.74F);
    private static final Who MOTHER = new Who("AYESHA", "High Priestess of the Sovereign", 0xFFD670, 0x1E1404, 0xA07A22, "A", 1.86F);
    private static final Who RECORD = new Who("A RECORD", "Her voice, kept by the station", 0x6FE0D2, 0x021412, 0x13706A, "", 1.7F);
    private static final Who MAGUS = new Who("THE MAGUS", "", 0xC9A6FF, 0x0E0418, 0x5A2E8A, "M", 1.86F);
    private static final Who CHAOS = new Who("LORD CHAOS", "One of two", 0x7FE0F0, 0x041418, 0x1A5A6A, "C", 1.9F);
    private static final Who TRIBUNAL = new Who("THE LIVING TRIBUNAL", "Judge of all that is", 0xE8D9A8, 0x10100C, 0x5A5236, "T", 1.9F);
    private static final Who EINHERJAR = new Who("EINHERJAR", "Odin's honoured dead", 0xC8D4E6, 0x0A0E16, 0x3E5270, "E", 1.7F);
    private static final Who ORDER = new Who("MASTER ORDER", "One of two", 0xF2C24A, 0x1A1404, 0x7A5A12, "O", 1.9F);

    private static Who who(byte speaker) {
        switch (speaker) {
            case Speakers.WARLOCK:
                return WARLOCK;
            case Speakers.STRANGER:
                return STRANGER;
            case Speakers.RAGNA:
                return RAGNA;
            case Speakers.HRAFN:
                return HRAFN;
            case Speakers.DEATH:
                return DEATH;
            case Speakers.STONE:
                return STONE;
            case Speakers.CHAMPION:
                return CHAMPION;
            case Speakers.PARTY:
                return PARTY;
            case Speakers.IN_BETWEENER:
                return IN_BETWEENER;
            case Speakers.GARDENER:
                return GARDENER;
            case Speakers.MOTHER:
                return MOTHER;
            case Speakers.RECORD:
                return RECORD;
            case Speakers.MAGUS:
                return MAGUS;
            case Speakers.CHAOS:
                return CHAOS;
            case Speakers.ORDER:
                return ORDER;
            case Speakers.EINHERJAR:
                return EINHERJAR;
            case Speakers.TRIBUNAL:
                return TRIBUNAL;
            default:
                return GRANDMASTER;
        }
    }

    /** A speaker's accent colour, for anything else drawn in their name. */
    public static int accent(byte speaker) {
        return who(speaker).accent;
    }

    private static final class Line {
        final byte speaker;
        final int entity;
        final String text;
        int born;
        final int life;
        List<String> wrapped;
        int leftAt = -1;
        boolean scene;

        Line(byte speaker, int entity, String text, int born) {
            this.speaker = speaker;
            this.entity = entity;
            this.text = text;
            this.born = born;
            // Long enough to type, and to read twice over.
            this.life = (int) (text.length() / CHARS_PER_TICK) + 60 + (int) (text.length() * 1.4F);
        }

        Who who() {
            return SpeakerPanel.who(speaker);
        }
    }

    private SpeakerPanel() {
    }

    /** A line from the story: the view settles on whoever says it (see DialogueCamera). */
    public void sayInScene(byte speaker, int entity, String text) {
        say(speaker, entity, text);
        current.scene = true;
    }

    /** Whom a story scene is attending to: the speaker of the line being said, while it is said and a moment after; -1 for nobody. */
    public int sceneFocus() {
        Line l = current != null ? current : leaving;
        if (l == null || !l.scene || l.entity < 0 || tick - l.born > l.life) {
            return -1;
        }
        return l.entity;
    }

    /** Someone says something. */
    public void say(byte speaker, int entity, String text) {
        if (current != null) {
            current.leftAt = tick;
            leaving = current;
        }
        current = new Line(speaker, entity, text, tick);
    }

    /** How hard someone is talking this moment, 0 to 1: while their words are still being typed out, and easing off after. */
    public float speaking(int entity) {
        if (current == null || current.entity != entity || current.leftAt >= 0 || current.speaker == com.matoon.eternity.story.Speakers.DIRECTION
                || current.speaker == com.matoon.eternity.story.Speakers.STONE) {
            return 0F;
        }
        float typed = (tick - current.born) * CHARS_PER_TICK;
        float left = current.text.length() - typed;
        return left > 0F ? 1F : clamp(1F + left / 12F);
    }

    /**
     * They have read enough: a line still typing out is shown whole at once; one already whole is
     * let go. True if there was a line to skip (so the conversation is told they're done with it).
     */
    public boolean skip() {
        if (current == null) {
            return false;
        }
        int typedBy = current.born + (int) Math.ceil(current.text.length() / CHARS_PER_TICK);
        if (tick < typedBy) {
            current.born -= typedBy - tick;
        } else {
            current.leftAt = tick;
            leaving = current;
            current = null;
        }
        return true;
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
            draw(res, current, Math.min(in, out), sameSpeaker ? 1F : Math.min(in, out), bottom, pt, true);
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
        if (line.speaker == Speakers.DIRECTION) {
            direction(font, line, w, h - bottom - 14, alpha, pt);
            return;
        }
        Who who = line.who();
        int panelW = Math.min(w - 24, 372);
        int portrait = 52;
        int x0 = (w - panelW) / 2;
        int textX = x0 + portrait + 16, textW = panelW - portrait - 26;
        if (line.wrapped == null) {
            line.wrapped = font.listFormattedStringToWidth(line.text, textW);
        }
        // Three rows fit; a longer line makes the panel taller, upward, rather than spilling out of it.
        int panelH = Math.max(62, 36 + line.wrapped.size() * 10);
        float rise = (1F - ease(frame)) * 14F;
        float y0 = h - bottom - panelH + rise;

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
            // Nobody to show: their initial, in their colour; a carving for a stone, a hollow for her.
            if (line.speaker == Speakers.STONE) {
                rune(cx, cy, size, who.accent, alpha);
            }
            GlStateManager.enableTexture2D();
            String initial = who.initial;
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
        float face = who(speaker).face;
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
        RenderGrandmaster.portrait = RenderAdamWarlock.portrait = RenderNorse.portrait = com.matoon.eternity.client.render.RenderMagus.portrait = true;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
        try {
            rm.renderEntity(e, 0D, 0D, 0D, 0F, pt, false);
        } finally {
            RenderGrandmaster.portrait = RenderAdamWarlock.portrait = RenderNorse.portrait = com.matoon.eternity.client.render.RenderMagus.portrait = false;
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
        // Some renderers (Order and Chaos) turn fog back on as they finish; in the HUD it would wash the whole screen out.
        GlStateManager.disableFog();
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

    /** A rune cut into the window: a stave, two twigs off it, and the cut edges catching the light. */
    private static void rune(float cx, float cy, int size, int rgb, float a) {
        float s = size / 52F;
        GlStateManager.disableTexture2D();
        rect(cx - 1.5F * s, cy - 13F * s, cx + 1.5F * s, cy + 13F * s, rgb, 0.85F * a, rgb, 0.55F * a);
        for (int i = 0; i < 2; i++) {
            float y = cy - 11F * s + i * 7F * s;
            Tessellator t = Tessellator.getInstance();
            BufferBuilder b = t.getBuffer();
            b.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            b.pos(cx + 1.5F * s, y, 0).color(r(rgb), g(rgb), bl(rgb), 0.8F * a).endVertex();
            b.pos(cx + 1.5F * s, y + 3F * s, 0).color(r(rgb), g(rgb), bl(rgb), 0.8F * a).endVertex();
            b.pos(cx + 10F * s, y + 8F * s, 0).color(r(rgb), g(rgb), bl(rgb), 0.6F * a).endVertex();
            b.pos(cx + 10F * s, y + 5F * s, 0).color(r(rgb), g(rgb), bl(rgb), 0.6F * a).endVertex();
            t.draw();
        }
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
