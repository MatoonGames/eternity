package com.matoon.eternity.client.gui;

import com.matoon.eternity.Eternity;
import com.matoon.eternity.EternityTab;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.client.event.GuiContainerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * What the Eternity creative tab draws over the creative screen, on top of its painted panel
 * ({@link EternityTabArt}): an aura round the tab itself on whatever tab is open, with three sparks
 * circling its Regulator, so it stands out from every other tab; and, while it is open, its title
 * written in a wave of gold and violet, and a crystal knob on the scrollbar.
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = Eternity.MODID, value = Side.CLIENT)
public final class EternityTabFx {

    private EternityTabFx() {
    }

    @SubscribeEvent
    public static void onForeground(GuiContainerEvent.DrawForeground event) {
        if (!(event.getGuiContainer() instanceof GuiContainerCreative)) {
            return;
        }
        GuiContainerCreative gui = (GuiContainerCreative) event.getGuiContainer();
        EternityTab tab = EternityTab.INSTANCE;
        boolean open = gui.getSelectedTabIndex() == tab.getIndex();
        boolean shown = tab.getTabPage() == page();
        if (!open && !shown) {
            return;
        }
        double t = (System.currentTimeMillis() % 3_600_000L) / 1000D;

        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.pushMatrix();
        GlStateManager.translate(0F, 0F, 300F);
        if (shown) {
            aura(gui, tab, open, t);
        }
        if (open) {
            title(t);
            knob(gui);
        }
        GlStateManager.popMatrix();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.enableDepth();
        RenderHelper.enableGUIStandardItemLighting();
    }

    /** Which page of tabs the creative screen is showing. */
    private static int page() {
        try {
            Integer page = ObfuscationReflectionHelper.getPrivateValue(GuiContainerCreative.class, null, "tabPage");
            return page == null ? 0 : page;
        } catch (RuntimeException e) {
            return 0;
        }
    }

    /** A violet haze round the tab, breathing, a gold heart to it, and three sparks circling the icon, each with a tail. */
    private static void aura(GuiContainerCreative gui, EternityTab tab, boolean open, double t) {
        int col = tab.getColumn();
        boolean top = tab.isOnTopRow();
        int x = tab.isAlignedRight() ? gui.getXSize() - 28 * (6 - col) : 28 * col + (col > 0 ? col : 0);
        int y = top ? -28 : gui.getYSize() - 4;
        int iconY = y + 8 + (top ? 1 : -1);
        float cx = x + 14F, cy = iconY + 8F;
        Minecraft mc = Minecraft.getMinecraft();

        // Its own button, over vanilla's grey one. Shut, the end of it that tucks under the panel stays under it.
        mc.getTextureManager().bindTexture(EternityTabArt.panel());
        GlStateManager.color(1F, 1F, 1F, 1F);
        int u = (top ? 0 : 56) + (open ? 0 : 28);
        if (open) {
            gui.drawTexturedModalRect(x, y, u, EternityTabArt.TAB_V, 28, 32);
        } else if (top) {
            gui.drawTexturedModalRect(x, y, u, EternityTabArt.TAB_V, 28, 28);
        } else {
            gui.drawTexturedModalRect(x, y + 4, u, EternityTabArt.TAB_V + 4, 28, 28);
        }

        float breathe = (float) (0.5D + 0.5D * Math.sin(t * 2.2D));
        float strength = open ? 1F : 0.75F;
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        mc.getTextureManager().bindTexture(EternityTabArt.glow());
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        glow(buf, cx, cy, 26F + 4F * breathe, 0.55F, 0.25F, 1F, (0.35F + 0.20F * breathe) * strength);
        tess.draw();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

        // The Regulator again, over the haze, since the button now covers vanilla's.
        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.enableDepth();
        mc.getRenderItem().renderItemAndEffectIntoGUI(tab.getIcon(), x + 6, iconY);
        GlStateManager.disableDepth();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();

        mc.getTextureManager().bindTexture(EternityTabArt.glow());
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        for (int i = 0; i < 3; i++) {
            double base = t * 1.7D + i * Math.PI * 2D / 3D;
            for (int k = 0; k < 6; k++) {
                double a = base - k * 0.13D;
                float sx = cx + (float) (Math.cos(a) * 12.5D);
                float sy = cy + (float) (Math.sin(a) * 11D);
                float fade = 1F - k / 6F;
                float size = k == 0 ? 7F : 5F * fade;
                // Each spark burns white at its head and cools to violet down its tail.
                glow(buf, sx, sy, size, 1F, 0.75F + 0.25F * fade, 0.55F + 0.45F * fade, 0.95F * fade * strength);
            }
        }
        tess.draw();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
    }

    private static void glow(BufferBuilder buf, float x, float y, float size, float r, float g, float b, float a) {
        float h = size / 2F;
        buf.pos(x - h, y + h, 0D).tex(0D, 1D).color(r, g, b, a).endVertex();
        buf.pos(x + h, y + h, 0D).tex(1D, 1D).color(r, g, b, a).endVertex();
        buf.pos(x + h, y - h, 0D).tex(1D, 0D).color(r, g, b, a).endVertex();
        buf.pos(x - h, y - h, 0D).tex(0D, 0D).color(r, g, b, a).endVertex();
    }

    /** The tab's name, each letter its own colour in a wave rolling through gold, white and violet, over a deep shadow. */
    private static void title(double t) {
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        String name = I18n.format("itemGroup." + Eternity.MODID);
        int x = 8;
        for (int i = 0; i < name.length(); i++) {
            String c = String.valueOf(name.charAt(i));
            double wave = Math.sin(t * 2.4D - i * 0.55D);
            float u = (float) (0.5D + 0.5D * wave);
            int r = (int) (255 + (200 - 255) * u), g = (int) (214 + (140 - 214) * u), b = (int) (110 + (255 - 110) * u);
            if (wave > 0.93D) {
                // The crest of the wave catches the light.
                float f = (float) ((wave - 0.93D) / 0.07D);
                r += (int) ((255 - r) * f);
                g += (int) ((255 - g) * f);
                b += (int) ((255 - b) * f);
            }
            font.drawString(c, x + 1, 7, 0x2A0D45);
            font.drawString(c, x, 6, (r << 16) | (g << 8) | b);
            x += font.getStringWidth(c) + 1;
        }
    }

    /** The crystal knob, over vanilla's grey one, where vanilla's would be. */
    private static void knob(GuiContainerCreative gui) {
        float scroll;
        try {
            Float s = ObfuscationReflectionHelper.getPrivateValue(GuiContainerCreative.class, gui, "currentScroll", "field_147067_x");
            scroll = s == null ? 0F : s;
        } catch (RuntimeException e) {
            scroll = 0F;
        }
        boolean scrolls = ((GuiContainerCreative.ContainerCreative) gui.inventorySlots).canScroll();
        Minecraft.getMinecraft().getTextureManager().bindTexture(EternityTabArt.panel());
        GlStateManager.color(1F, 1F, 1F, 1F);
        gui.drawTexturedModalRect(175, 18 + (int) (95F * scroll), scrolls ? EternityTabArt.KNOB_U : EternityTabArt.KNOB_DIM_U, 0, 12, 15);
    }
}
