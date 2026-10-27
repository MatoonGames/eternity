package com.matoon.eternity.client;

import com.matoon.eternity.Eternity;
import java.util.ArrayList;
import com.matoon.eternity.client.render.RenderOrderChaos;
import com.matoon.eternity.client.render.RenderInBetweener;
import com.matoon.eternity.ability.MastersAbilities;
import com.matoon.eternity.ability.EternityAbilities;
import com.matoon.eternity.ability.TribunalAbilities;
import com.matoon.eternity.client.render.Cosmic;
import com.matoon.eternity.client.render.ModelTribunal;
import com.matoon.eternity.client.render.OrbRenderer;
import com.matoon.eternity.client.render.RenderEternity;
import com.matoon.eternity.entity.EntityEternity;
import com.matoon.eternity.item.EternityItems;
import com.matoon.eternity.item.ItemAstralRegulator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.List;

/**
 * The charged Astral Regulator's tooltip: you are looking into the crystal. Through a
 * round window ringed in gold you see the universe inside it, and suspended there every
 * cosmic being it has absorbed (Eternity, the Living Tribunal, Master Order, Lord Chaos,
 * the In-Betweener) wheeling slowly round one another, with the faceted shell catching
 * the light in front of them. The powers each grants are listed beside the window, in a
 * second column once there are more than two of them.
 */
@SideOnly(Side.CLIENT)
public final class RegulatorTooltip {

    public static final RegulatorTooltip INSTANCE = new RegulatorTooltip();

    private static final int PANEL_W = 204;
    private static final int PANEL_H = 132;
    /** How much wider the panel grows for its second column of powers. */
    private static final int COLUMN_W = 112;
    private static final ModelTribunal TRIBUNAL_MODEL = new ModelTribunal();
    private static final int LENS = 48;
    private static final int GOLD = 0xFFD49A1C;
    private static final int GOLD_DIM = 0xFF5A3A08;

    private RegulatorTooltip() {
    }

    @SubscribeEvent
    public void onPre(RenderTooltipEvent.Pre event) {
        ItemStack stack = event.getStack();
        if (stack.getItem() != EternityItems.ASTRAL_REGULATOR || !ItemAstralRegulator.isCharged(stack)) {
            return;
        }
        event.setCanceled(true);
        draw(stack, event.getX(), event.getY(), event.getScreenWidth(), event.getScreenHeight());
    }

    /** One being the Regulator can hold: how it is named in the tooltip, and the powers it grants. */
    private static final class Being {
        final String id, nameKey, headerKey;
        final int nameColour, headerColour, powerColour;
        final String[] powers;

        Being(String id, String nameKey, int nameColour, String headerKey, int headerColour, int powerColour, String[] powers) {
            this.id = id;
            this.nameKey = nameKey;
            this.nameColour = nameColour;
            this.headerKey = headerKey;
            this.headerColour = headerColour;
            this.powerColour = powerColour;
            this.powers = powers;
        }

        int height() {
            return 11 + powers.length * 10 + 4;
        }
    }

    /** Every being it can hold, in the order they are shown. */
    private static final Being[] BEINGS = {
            new Being(ItemAstralRegulator.ETERNITY, "entity.eternity.eternity.name", 0xC48AFF, "tooltip.eternity.powers", 0xD49A1C, 0xE6D9FF,
                    EternityAbilities.ETERNITY_POWERS),
            new Being(ItemAstralRegulator.TRIBUNAL, "entity.eternity.living_tribunal.name", 0xF0C24A, "tooltip.eternity.judgements", 0xF0C24A,
                    0xFFF0C8, TribunalAbilities.POWERS),
            new Being(ItemAstralRegulator.ORDER, "entity.eternity.master_order.name", 0xFFC640, "tooltip.eternity.laws", 0xFFC640, 0xFFEEB8,
                    MastersAbilities.ORDER_POWERS),
            new Being(ItemAstralRegulator.CHAOS, "entity.eternity.lord_chaos.name", 0x7CC8F0, "tooltip.eternity.whims", 0x7CC8F0, 0xD6F0FF,
                    MastersAbilities.CHAOS_POWERS),
            new Being(ItemAstralRegulator.INBETWEENER, "entity.eternity.inbetweener.name", 0xE8E8EC, "tooltip.eternity.paradoxes", 0xB8B8C0,
                    0xF4F4F6, MastersAbilities.INBETWEENER_POWERS)};

    private void draw(ItemStack stack, int mouseX, int mouseY, int screenW, int screenH) {
        Minecraft mc = Minecraft.getMinecraft();
        List<String> ids = ItemAstralRegulator.absorbed(stack);
        List<Being> held = new ArrayList<>();
        for (Being being : BEINGS) {
            if (ids.contains(being.id)) {
                held.add(being);
            }
        }
        boolean golden = !ids.contains(ItemAstralRegulator.ETERNITY) || held.size() > 1;
        // Past two beings, their powers run on into a second column.
        boolean twoColumns = held.size() > 2;
        int width = twoColumns ? PANEL_W + COLUMN_W : PANEL_W;
        int namesHeight = held.size() * 9;
        int columnA = 0, columnB = 0;
        for (int i = 0; i < held.size(); i++) {
            if (twoColumns && i >= 2) {
                columnB += held.get(i).height();
            } else {
                columnA += held.get(i).height();
            }
        }
        // The left column holds the names, then the first beings' powers; the right, the rest of them.
        int height = Math.max(PANEL_H, Math.max(38 + namesHeight + columnA, 23 + columnB)) + 18;

        int x = mouseX + 12;
        int y = mouseY - 12;
        if (x + width > screenW - 4) {
            x = Math.max(4, mouseX - 16 - width);
        }
        y = Math.max(4, Math.min(y, screenH - height - 4));
        float time = (Minecraft.getSystemTime() % 3600000L) / 50F;

        // Forge raises this event before it sets up 2D state, so do it here: no item
        // lighting (it would turn everything black) and no depth test against the items.
        GlStateManager.pushMatrix();
        GlStateManager.disableRescaleNormal();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.translate(0F, 0F, 400F);

        panel(x, y, width, height, time, golden);
        int cx = x + 8 + LENS;
        int cy = y + Math.min(height, PANEL_H + 20) / 2;
        lens(mc, cx, cy, time, held, golden);

        FontRenderer font = mc.fontRenderer;
        int tx = x + 16 + LENS * 2;
        font.drawStringWithShadow(TextFormatting.LIGHT_PURPLE + stack.getDisplayName(), tx, y + 9, 0xFFFFFF);
        font.drawStringWithShadow(I18n.translateToLocal("tooltip.eternity.absorbed"), tx, y + 23, 0x8A6AAB);
        int row = y + 33;
        for (Being being : held) {
            font.drawStringWithShadow(TextFormatting.BOLD + I18n.translateToLocal(being.nameKey), tx + 4, row, being.nameColour);
            row += 9;
        }
        row += 5;
        int bx = x + PANEL_W - 4, by = y + 23;
        for (int i = 0; i < held.size(); i++) {
            Being being = held.get(i);
            boolean right = twoColumns && i >= 2;
            int px = right ? bx : tx;
            int py = right ? by : row;
            font.drawStringWithShadow(I18n.translateToLocal(being.headerKey), px, py, being.headerColour);
            py += 11;
            for (String power : being.powers) {
                icon(mc, power, px + 2, py);
                font.drawStringWithShadow(I18n.translateToLocal(Eternity.MODID + ".abilities." + power + ".name"), px + 13, py + 1, being.powerColour);
                py += 10;
            }
            py += 4;
            if (right) {
                by = py;
            } else {
                row = py;
            }
        }

        // What pours out of it, at the foot of the panel.
        String energy = I18n.translateToLocal("tooltip.eternity.energy") + " " + ItemAstralRegulator.formatEnergy(ItemAstralRegulator.energyPerTick(stack)) + " FE/t";
        font.drawStringWithShadow(energy, x + width - 8 - font.getStringWidth(energy), y + height - 13, golden ? 0xFFE08A : 0xB89BFF);

        GlStateManager.popMatrix();
        // Put back what vanilla's tooltip code would have left behind.
        GlStateManager.enableLighting();
        GlStateManager.enableDepth();
        RenderHelper.enableStandardItemLighting();
        GlStateManager.enableRescaleNormal();
        GlStateManager.color(1F, 1F, 1F, 1F);
    }

    private void panel(int x, int y, int width, int height, float time, boolean tribunal) {
        Gui.drawRect(x - 2, y - 2, x + width + 2, y + height + 2, 0xF0040008);
        gradient(x, y, x + width, y + height, tribunal ? 0xF41C1206 : 0xF4140628, 0xF4070212);
        border(x, y, width, height, time, tribunal);
    }

    /**
     * The frame: a triple line of gold, lights chasing one another around it, a pulsing
     * diamond with brackets at every corner, and, once it holds the Tribunal, runes
     * kindling one after another along the top and bottom.
     */
    private void border(int x, int y, int w, int h, float time, boolean tribunal) {
        int dim = tribunal ? 0xFFB8862A : GOLD_DIM;
        int main = tribunal ? 0xFFF4C860 : GOLD;
        frame(x - 3, y - 3, w + 6, h + 6, (dim & 0xFFFFFF) | 0x50000000);
        frame(x - 1, y - 1, w + 2, h + 2, dim);
        frame(x, y, w, h, main);

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        int perimeter = 2 * (w + h);
        int comets = tribunal ? 3 : 2;
        float speed = tribunal ? 2.4F : 1.6F;
        float tail = tribunal ? 60F : 44F;
        for (int i = 0; i < perimeter; i++) {
            float glow = 0F;
            for (int c = 0; c < comets; c++) {
                float head = (time * speed + c * perimeter / (float) comets) % perimeter;
                float d = head - i;
                if (d < 0F) {
                    d += perimeter;
                }
                if (d < tail) {
                    glow = Math.max(glow, 1F - d / tail);
                }
            }
            if (glow > 0.02F) {
                int[] at = along(x, y, w, h, i);
                float g = glow * glow;
                quad(buffer, at[0] - 1, at[1] - 1, 3, 3, 1F, tribunal ? 0.92F : 0.8F, tribunal ? 0.6F : 0.35F, g);
            }
        }
        if (tribunal) {
            for (int i = 6; i < w - 6; i += 10) {
                float kindle = Math.max(0F, MathHelper.sin(time * 0.12F - i * 0.08F));
                quad(buffer, x + i, y + 2, 2, 2, 1F, 0.85F, 0.45F, 0.7F * kindle);
                quad(buffer, x + w - i - 2, y + h - 4, 2, 2, 1F, 0.85F, 0.45F, 0.7F * kindle);
            }
        }
        tessellator.draw();

        float pulse = 0.6F + 0.4F * MathHelper.sin(time * 0.15F);
        int[][] corners = {{x, y, 1, 1}, {x + w, y, -1, 1}, {x, y + h, 1, -1}, {x + w, y + h, -1, -1}};
        buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
        for (int[] c : corners) {
            diamond(buffer, c[0], c[1], tribunal ? 5F : 4F, 1F, tribunal ? 0.9F : 0.78F, tribunal ? 0.55F : 0.3F, pulse);
            diamond(buffer, c[0], c[1], 2F, 1F, 1F, 1F, pulse);
        }
        tessellator.draw();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableTexture2D();
        for (int[] c : corners) {
            // Brackets reaching out from each corner.
            int bx = c[0] - c[2] * 4;
            int by = c[1] - c[3] * 4;
            Gui.drawRect(Math.min(bx, bx + c[2] * 12), by, Math.max(bx, bx + c[2] * 12), by + 1, main);
            Gui.drawRect(bx, Math.min(by, by + c[3] * 12), bx + 1, Math.max(by, by + c[3] * 12), main);
        }
    }

    /** A point the given distance clockwise around a rectangle's edge, from its top-left corner. */
    private static int[] along(int x, int y, int w, int h, int i) {
        if (i < w) {
            return new int[]{x + i, y};
        }
        if (i < w + h) {
            return new int[]{x + w - 1, y + i - w};
        }
        if (i < 2 * w + h) {
            return new int[]{x + w - 1 - (i - w - h), y + h - 1};
        }
        return new int[]{x, y + h - 1 - (i - 2 * w - h)};
    }

    private static void quad(BufferBuilder buffer, float x, float y, float w, float h, float r, float g, float b, float a) {
        buffer.pos(x, y + h, 0).color(r, g, b, a).endVertex();
        buffer.pos(x + w, y + h, 0).color(r, g, b, a).endVertex();
        buffer.pos(x + w, y, 0).color(r, g, b, a).endVertex();
        buffer.pos(x, y, 0).color(r, g, b, a).endVertex();
    }

    private static void diamond(BufferBuilder buffer, float cx, float cy, float size, float r, float g, float b, float a) {
        buffer.pos(cx, cy - size, 0).color(r, g, b, a).endVertex();
        buffer.pos(cx - size, cy, 0).color(r, g, b, a).endVertex();
        buffer.pos(cx + size, cy, 0).color(r, g, b, a).endVertex();
        buffer.pos(cx - size, cy, 0).color(r, g, b, a).endVertex();
        buffer.pos(cx, cy + size, 0).color(r, g, b, a).endVertex();
        buffer.pos(cx + size, cy, 0).color(r, g, b, a).endVertex();
    }

    /** The round window into the crystal, with everything it holds turning slowly together inside. */
    private void lens(Minecraft mc, int cx, int cy, float time, List<Being> held, boolean golden) {
        ScaledResolution res = new ScaledResolution(mc);
        int scale = res.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((cx - LENS) * scale, mc.displayHeight - (cy + LENS) * scale, LENS * 2 * scale, LENS * 2 * scale);
        // Clear depth inside the window only, so the figures sort against themselves and nothing else.
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);

        // The universe inside the crystal.
        if (golden) {
            Cosmic.draw(() -> disc(cx, cy, LENS - 1), 14, 1F, 1.7F, 1F, 0.78F, 0.4F, false);
        } else {
            Cosmic.draw(() -> disc(cx, cy, LENS - 1), 14, 1F, 1.5F, 0.75F, 0.6F, 1F, false);
        }

        GlStateManager.pushMatrix();
        GlStateManager.translate(cx, cy, 150F);
        GlStateManager.scale(LENS, -LENS, LENS);
        GlStateManager.enableDepth();
        // Flipping Y for screen space mirrors the geometry, so swap which winding counts as the front.
        GL11.glFrontFace(GL11.GL_CW);

        // Alone, a being stands in the middle; together, they wheel slowly round one another.
        int n = held.size();
        float shrink = n <= 1 ? 1F : n == 2 ? 0.78F : 0.62F;
        for (int i = 0; i < n; i++) {
            GlStateManager.pushMatrix();
            if (n > 1) {
                double a = time * 0.012D + i * Math.PI * 2D / n;
                GlStateManager.translate(Math.cos(a) * 0.38D, 0D, Math.sin(a) * 0.38D);
            }
            figure(held.get(i).id, shrink, time);
            GlStateManager.popMatrix();
        }

        // The faceted shell in front, without its blinding heart.
        GlStateManager.rotate(14F, 1F, 0F, 0F);
        GlStateManager.rotate(time * 0.5F, 0F, 1F, 0F);
        OrbRenderer.draw(0.97F, time, 1F, false, golden);

        GL11.glFrontFace(GL11.GL_CCW);
        GlStateManager.disableDepth();
        GlStateManager.disableLighting();
        GlStateManager.popMatrix();
        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        ring(cx, cy, LENS, 2.5F, GOLD);
        ring(cx, cy, LENS + 2, 1F, GOLD_DIM);
    }

    /** One being, upright and turning, at its own size inside the crystal. */
    private static void figure(String id, float shrink, float time) {
        if (ItemAstralRegulator.ETERNITY.equals(id)) {
            float s = 0.15F * shrink;
            GlStateManager.scale(s, s, s);
            GlStateManager.translate(0F, -4.3F, 0F);
            GlStateManager.rotate(time * 0.9F, 0F, 1F, 0F);
            RenderEternity.drawFigure(0, 0, 0, 0F, 0F, 0F, 1F, 1F, time, EntityEternity.State.DRIFT, 0, 400, false);
        } else if (ItemAstralRegulator.TRIBUNAL.equals(id)) {
            float s = 0.0235F * shrink;
            GlStateManager.translate(0F, -0.6F * shrink, 0F);
            GlStateManager.rotate(-time * 0.9F, 0F, 1F, 0F);
            // His model is Y down and faces -Z, like every mob's; flip it upright, facing out.
            GlStateManager.scale(s, -s, -s);
            TRIBUNAL_MODEL.draw(ModelTribunal.Pose.IDLE, 0F, 1F, time);
        } else if (ItemAstralRegulator.ORDER.equals(id) || ItemAstralRegulator.CHAOS.equals(id)) {
            float s = 0.024F * shrink;
            GlStateManager.rotate(-time * 0.7F, 0F, 1F, 0F);
            GlStateManager.scale(s, -s, -s);
            RenderOrderChaos.drawModel(ItemAstralRegulator.ORDER.equals(id));
        } else if (ItemAstralRegulator.INBETWEENER.equals(id)) {
            float s = 0.022F * shrink;
            GlStateManager.translate(0F, 0.5F * shrink, 0F);
            GlStateManager.rotate(time * 0.7F, 0F, 1F, 0F);
            GlStateManager.scale(s, -s, -s);
            RenderInBetweener.drawStanding(time);
        }
    }

    private void icon(Minecraft mc, String power, int x, int y) {
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.color(1F, 1F, 1F, 1F);
        mc.getTextureManager().bindTexture(new ResourceLocation(Eternity.MODID, "textures/abilities/" + power + ".png"));
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0F, 0F, 9, 9, 9, 9);
    }

    // ------------------------------------------------------------------ primitives

    private static void disc(int cx, int cy, int radius) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION);
        buffer.pos(cx, cy, 0).endVertex();
        for (int i = 0; i <= 48; i++) {
            double a = -Math.PI * 2D * i / 48D;
            buffer.pos(cx + Math.cos(a) * radius, cy + Math.sin(a) * radius, 0).endVertex();
        }
        tessellator.draw();
    }

    private static void ring(int cx, int cy, int radius, float width, int argb) {
        float a = (argb >> 24 & 0xFF) / 255F, r = (argb >> 16 & 0xFF) / 255F;
        float g = (argb >> 8 & 0xFF) / 255F, b = (argb & 0xFF) / 255F;
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableCull();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i <= 64; i++) {
            double ang = Math.PI * 2D * i / 64D;
            double cos = Math.cos(ang), sin = Math.sin(ang);
            buffer.pos(cx + cos * (radius - width / 2F), cy + sin * (radius - width / 2F), 0).color(r, g, b, a).endVertex();
            buffer.pos(cx + cos * (radius + width / 2F), cy + sin * (radius + width / 2F), 0).color(r, g, b, a).endVertex();
        }
        tessellator.draw();
        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
    }

    private static void frame(int x, int y, int w, int h, int color) {
        Gui.drawRect(x, y, x + w, y + 1, color);
        Gui.drawRect(x, y + h - 1, x + w, y + h, color);
        Gui.drawRect(x, y, x + 1, y + h, color);
        Gui.drawRect(x + w - 1, y, x + w, y + h, color);
    }

    private static void gradient(int left, int top, int right, int bottom, int topColor, int bottomColor) {
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        colored(buffer, right, top, topColor);
        colored(buffer, left, top, topColor);
        colored(buffer, left, bottom, bottomColor);
        colored(buffer, right, bottom, bottomColor);
        tessellator.draw();
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.enableTexture2D();
    }

    private static void colored(BufferBuilder buffer, int x, int y, int argb) {
        float a = (argb >> 24 & 0xFF) / 255F, r = (argb >> 16 & 0xFF) / 255F;
        float g = (argb >> 8 & 0xFF) / 255F, b = (argb & 0xFF) / 255F;
        buffer.pos(x, y, 0D).color(r, g, b, a).endVertex();
    }
}
