package com.matoon.eternity.stones.client;

import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.stones.network.MsgMindControlAct;
import com.matoon.eternity.stones.network.MsgMindControlMenu;
import com.matoon.eternity.stones.network.MsgMindControlMenu.AbilityInfo;
import lucraft.mods.lucraftcore.superpowers.Superpower;
import lucraft.mods.lucraftcore.superpowers.SuperpowerHandler;
import lucraft.mods.lucraftcore.superpowers.abilities.Ability;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * The Mind Stone's window into someone it holds: them, live, in a golden portal; their inventory or equipment; their
 * health, armour, distance and the time left; and buttons to drain them or let them go. If they have a superpower, its
 * sigil opens a second page of their powers, to use by hand, and the autopilot's switch.
 */
public class GuiMindControl extends GuiScreen {

    private static final int DRAIN = 0, RELEASE = 1, BACK = 2;
    private static final int ROWS = 4;

    private final MsgMindControlMenu menu;
    private boolean autocast, abilitiesPage;
    private int left, top, w, h, scroll;
    private long openedAt;
    private List<Ability> frameAbilities = Collections.emptyList();

    public GuiMindControl(MsgMindControlMenu menu) {
        this.menu = menu;
        autocast = menu.abilityAutocast;
    }

    @Override
    public void initGui() {
        w = Math.min(356, width - 16);
        h = Math.min(218, height - 16);
        left = (width - w) / 2;
        top = (height - h) / 2;
        openedAt = mc.world == null ? 0L : mc.world.getTotalWorldTime();
        int gap = 8, buttonWidth = (w - 32 - gap) / 2, buttonY = top + h - 27;
        buttonList.add(new PortalButton(DRAIN, left + 16, buttonY, buttonWidth, 18, "DRAIN HEALTH", 0xFFD7A928));
        buttonList.add(new PortalButton(RELEASE, left + 16 + buttonWidth + gap, buttonY, buttonWidth, 18, "RELEASE CONTROL", 0xFFC94B58));
        buttonList.add(new PortalButton(BACK, left + w - 61, top + 9, 49, 15, "RETURN", 0xFFFFD86A));
        updateButtons();
    }

    private void updateButtons() {
        for (GuiButton button : buttonList) {
            if (button.id == BACK) {
                button.visible = abilitiesPage;
            }
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == BACK) {
            abilitiesPage = false;
            updateButtons();
        } else {
            EternityNetwork.sendToServer(MsgMindControlAct.of(button.id == DRAIN ? MsgMindControlAct.DRAIN : MsgMindControlAct.RELEASE, menu.target));
            mc.displayGuiScreen(null);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        Gui.drawRect(left - 1, top - 1, left + w + 1, top + h + 1, 0xFF9B7620);
        Gui.drawRect(left, top, left + w, top + h, 0xF408080A);
        drawGradientRect(left + 1, top + 1, left + w - 1, top + 36, 0xFF30230D, 0xFF11100D);
        drawString(fontRenderer, "MIND STONE", left + 12, top + 9, 0xFFFFD86A);
        drawString(fontRenderer, "A window into " + menu.name, left + 12, top + 22, 0xFFE9E3D2);

        Entity found = mc.world == null ? null : mc.world.getEntityByID(menu.entityId);
        EntityLivingBase entity = found instanceof EntityLivingBase ? (EntityLivingBase) found : null;
        if (abilitiesPage) {
            drawAbilities(entity, mouseX, mouseY);
            super.drawScreen(mouseX, mouseY, partialTicks);
            return;
        }

        int portalX = left + 80, portalY = top + 106;
        drawPortal(portalX, portalY, 55, partialTicks);
        drawSigil(entity, portalX - 54, top + 41);
        drawCenteredString(fontRenderer, "CONTROLLED", portalX, top + 171, 0xFFFFCE55);

        int panelX = left + 166, panelY = top + 45, panelRight = left + w - 11;
        drawString(fontRenderer, menu.playerInventory ? "INVENTORY + HOTBAR" : "EQUIPMENT", panelX + 4, panelY + 2, 0xFFFFD86A);
        Gui.drawRect(panelX + 4, panelY + 14, panelRight, panelY + 15, 0xFF72571B);
        drawItems(panelX + 4, panelY + 21);

        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.enableDepth();
        RenderHelper.enableGUIStandardItemLighting();
        if (entity != null) {
            GuiInventory.drawEntityOnScreen(portalX, top + 160, 43, portalX - mouseX, top + 112 - mouseY, entity);
        } else {
            drawCenteredString(fontRenderer, "OUT OF RANGE", portalX, top + 105, 0xFFFFFFFF);
        }
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableDepth();
        GlStateManager.color(1F, 1F, 1F, 1F);
        drawStats(entity, panelX + 4, panelRight, top + 157);
        if (overSigil(mouseX, mouseY)) {
            drawSuperpowerTooltip(mouseX, mouseY);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private boolean overSigil(int mouseX, int mouseY) {
        return !menu.superpowerId.isEmpty() && mouseX >= left + 24 && mouseX < left + 52 && mouseY >= top + 39 && mouseY < top + 69;
    }

    /** Their superpower's own icon, in the corner of the portal: the way to their powers. */
    private void drawSigil(EntityLivingBase entity, int x, int y) {
        if (menu.superpowerId.isEmpty()) {
            return;
        }
        Gui.drawRect(x - 2, y - 2, x + 26, y + 26, 0xA00E0B10);
        Gui.drawRect(x - 1, y - 1, x + 25, y + 25, 0xFF8B681B);
        Superpower superpower = entity == null ? null : SuperpowerHandler.getSuperpower(entity);
        // Superpower icons draw with whatever colour and blending came before; isolate them from the gold.
        GlStateManager.pushMatrix();
        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.enableAlpha();
        if (superpower != null) {
            RenderHelper.enableGUIStandardItemLighting();
            float oldZ = zLevel, oldItemZ = itemRender.zLevel;
            zLevel = 100F;
            itemRender.zLevel = 100F;
            GlStateManager.enableLighting();
            GlStateManager.enableRescaleNormal();
            GlStateManager.translate(x + 4, y + 4, 0F);
            GlStateManager.scale(0.5D, 0.5D, 0.5D);
            superpower.renderIcon(mc, 0, 0);
            itemRender.zLevel = oldItemZ;
            zLevel = oldZ;
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableLighting();
        } else {
            Gui.drawRect(x + 5, y + 5, x + 19, y + 19, 0xFF5C3510);
            drawCenteredString(fontRenderer, "✦", x + 12, y + 6, 0xFFFFD86A);
        }
        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private void drawSuperpowerTooltip(int mouseX, int mouseY) {
        List<String> tip = new ArrayList<>();
        tip.add(TextFormatting.GOLD + menu.superpowerName);
        tip.add(TextFormatting.GRAY + "Injected Lucraft superpower");
        int count = Math.min(8, menu.abilities.size());
        for (int i = 0; i < count; i++) {
            AbilityInfo info = menu.abilities.get(i);
            tip.add(TextFormatting.YELLOW + "- " + TextFormatting.WHITE + info.name
                    + (info.triggerable ? TextFormatting.GRAY + " (active)" : TextFormatting.DARK_GRAY + " (passive)"));
        }
        if (menu.abilities.size() > count) {
            tip.add(TextFormatting.GRAY + "+ " + (menu.abilities.size() - count) + " more abilities");
        }
        tip.add(TextFormatting.GOLD + "Click to command its abilities");
        drawHoveringText(tip, mouseX, mouseY);
    }

    private Ability localAbility(String key) {
        for (Ability ability : frameAbilities) {
            if (key.equals(ability.getKey())) {
                return ability;
            }
        }
        return null;
    }

    private void drawAbilities(EntityLivingBase entity, int mouseX, int mouseY) {
        frameAbilities = entity == null ? Collections.emptyList() : Ability.getAbilities(entity);
        List<AbilityInfo> abilities = menu.abilities;
        int x = left + 13, right = left + w - 13;
        drawString(fontRenderer, menu.superpowerName.isEmpty() ? "NO INJECTED SUPERPOWER" : menu.superpowerName.toUpperCase(Locale.ROOT), x, top + 48, 0xFFFFD86A);
        drawString(fontRenderer, "Its active abilities can be compelled; passive abilities are observed.", x, top + 61, 0xFFE9E3D2);

        int toggleY = top + 75, toggleX = right - 53;
        Gui.drawRect(x, toggleY, right, toggleY + 20, 0xB01B1712);
        drawString(fontRenderer, "COMBAT AUTOPILOT", x + 8, toggleY + 6, 0xFFFFD86A);
        Gui.drawRect(toggleX, toggleY + 4, right - 6, toggleY + 16, autocast ? 0xFF36783A : 0xFF5D2426);
        drawCenteredString(fontRenderer, autocast ? "ON" : "OFF", (toggleX + right - 6) / 2, toggleY + 6, 0xFFFFFFFF);

        int y = top + 102;
        if (abilities.isEmpty()) {
            drawString(fontRenderer, "This mind has no usable Lucraft actions.", x, y + 8, 0xFFB9AF96);
        }
        scroll = Math.max(0, Math.min(scroll, Math.max(0, abilities.size() - ROWS)));
        for (int row = 0; row < ROWS && scroll + row < abilities.size(); row++, y += 25) {
            AbilityInfo info = abilities.get(scroll + row);
            Gui.drawRect(x, y, right, y + 22, 0xB0121015);
            Gui.drawRect(x, y, x + 2, y + 22, info.ready ? 0xFFFFC951 : 0xFF72571B);
            Ability ability = localAbility(info.key);
            GlStateManager.color(1F, 1F, 1F, 1F);
            if (ability != null) {
                ability.drawIcon(mc, this, x + 6, y + 2);
            } else {
                Gui.drawRect(x + 6, y + 2, x + 24, y + 20, 0xFF55320F);
                drawCenteredString(fontRenderer, "✦", x + 15, y + 5, 0xFFFFD86A);
            }
            GlStateManager.color(1F, 1F, 1F, 1F);
            drawString(fontRenderer, info.name, x + 30, y + 4, info.ready ? 0xFFFFFFFF : 0xFFAE9F7D);
            drawString(fontRenderer, !info.triggerable ? "PASSIVE" : info.ready ? "INVOKE" : "RECHARGING", right - 70, y + 7, info.ready ? 0xFFFFD86A : 0xFF8D7A51);
            if (mouseX >= x && mouseY >= y && mouseX < right && mouseY < y + 22) {
                List<String> tip = new ArrayList<>();
                tip.add(info.name);
                tip.addAll(fontRenderer.listFormattedStringToWidth(info.description, 190));
                tip.add(!info.triggerable ? TextFormatting.DARK_GRAY + "Passive ability"
                        : info.ready ? TextFormatting.GREEN + "Click to invoke" : TextFormatting.GOLD + "Still recharging");
                drawHoveringText(tip, mouseX, mouseY);
            }
        }
        if (abilities.size() > ROWS) {
            int trackTop = top + 102, trackBottom = top + 199, thumb = Math.max(14, 97 * ROWS / abilities.size());
            int offset = (int) ((97 - thumb) * (scroll / (float) (abilities.size() - ROWS)));
            Gui.drawRect(right - 3, trackTop, right - 1, trackBottom, 0xFF392A15);
            Gui.drawRect(right - 3, trackTop + offset, right - 1, trackTop + offset + thumb, 0xFFFFC951);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        if (!abilitiesPage && button == 0 && overSigil(mouseX, mouseY)) {
            abilitiesPage = true;
            updateButtons();
            return;
        }
        super.mouseClicked(mouseX, mouseY, button);
        if (!abilitiesPage || button != 0) {
            return;
        }
        int x = left + 13, right = left + w - 13, toggleY = top + 75;
        if (mouseX >= x && mouseX < right && mouseY >= toggleY && mouseY < toggleY + 20) {
            autocast = !autocast;
            EternityNetwork.sendToServer(MsgMindControlAct.of(autocast ? MsgMindControlAct.AUTOCAST_ON : MsgMindControlAct.AUTOCAST_OFF, menu.target));
            return;
        }
        int y = top + 102;
        for (int row = 0; row < ROWS && scroll + row < menu.abilities.size(); row++, y += 25) {
            AbilityInfo info = menu.abilities.get(scroll + row);
            if (mouseX >= x && mouseX < right && mouseY >= y && mouseY < y + 22 && info.triggerable && info.ready) {
                EternityNetwork.sendToServer(MsgMindControlAct.trigger(menu.target, info.key));
                return;
            }
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (abilitiesPage && wheel != 0) {
            scroll = Math.max(0, Math.min(scroll - Integer.signum(wheel), Math.max(0, menu.abilities.size() - ROWS)));
        }
    }

    /** A warm corona, counter-turning warped rings, and broken rune arcs. */
    private void drawPortal(int cx, int cy, int radius, float partialTicks) {
        double time = (mc.world.getTotalWorldTime() + partialTicks) * 0.075D;
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GlStateManager.depthMask(false);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(cx, cy, 0D).color(8, 5, 1, 245).endVertex();
        for (int i = 0; i <= 48; i++) {
            double a = i * Math.PI * 2D / 48D;
            buffer.pos(cx + Math.cos(a) * radius, cy + Math.sin(a) * radius * 0.78D, 0D).color(255, 180, 28, 20).endVertex();
        }
        tessellator.draw();
        for (int ring = 0; ring < 5; ring++) {
            double spin = time * (ring % 2 == 0 ? 1D : -0.68D) + ring * 1.21D, r = radius - ring * 8D + Math.sin(time * 1.4D + ring) * 2.2D;
            buffer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
            for (int i = 0; i <= 48; i++) {
                double a = spin + i * Math.PI * 2D / 48D, warp = Math.sin(a * (2 + ring % 3) + time * 1.8D) * 2.8D;
                buffer.pos(cx + Math.cos(a) * (r + warp), cy + Math.sin(a) * (r + warp) * 0.78D, 0D)
                        .color(1F, 0.48F + ring * 0.07F, 0.05F, 0.88F - ring * 0.10F).endVertex();
            }
            tessellator.draw();
        }
        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        for (int rune = 0; rune < 16; rune++) {
            double a = time * 0.38D + rune * Math.PI * 2D / 16D, r = radius + 5 + Math.sin(rune * 4.7D) * 3D;
            buffer.pos(cx + Math.cos(a) * r, cy + Math.sin(a) * r * 0.78D, 0D).color(1F, 0.86F, 0.28F, 0.9F).endVertex();
            buffer.pos(cx + Math.cos(a + 0.10D) * (r + 4), cy + Math.sin(a + 0.10D) * (r + 4) * 0.78D, 0D).color(1F, 0.42F, 0.03F, 0.18F).endVertex();
        }
        tessellator.draw();
        GlStateManager.depthMask(true);
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.popMatrix();
    }

    /** A player's inventory laid out as vanilla's, hotbar apart below; a creature's equipment in rows of six. */
    private void drawItems(int x0, int y0) {
        List<ItemStack> items = menu.items;
        RenderHelper.enableGUIStandardItemLighting();
        if (menu.playerInventory) {
            for (int i = 9; i < 36; i++) {
                drawSlot(items.size() > i ? items.get(i) : ItemStack.EMPTY, x0 + (i - 9) % 9 * 19, y0 + (i - 9) / 9 * 19);
            }
            for (int i = 0; i < 9; i++) {
                drawSlot(items.size() > i ? items.get(i) : ItemStack.EMPTY, x0 + i * 19, y0 + 62);
            }
        } else {
            for (int i = 0; i < items.size(); i++) {
                drawSlot(items.get(i), x0 + i % 6 * 19, y0 + i / 6 * 19);
            }
        }
        RenderHelper.disableStandardItemLighting();
        GlStateManager.color(1F, 1F, 1F, 1F);
    }

    private void drawSlot(ItemStack stack, int x, int y) {
        Gui.drawRect(x - 1, y - 1, x + 18, y + 18, 0xFF80651F);
        Gui.drawRect(x, y, x + 17, y + 17, 0xFF242116);
        if (!stack.isEmpty()) {
            itemRender.renderItemAndEffectIntoGUI(stack, x, y);
            itemRender.renderItemOverlayIntoGUI(fontRenderer, stack, x, y, null);
        }
    }

    private void drawStats(EntityLivingBase entity, int x, int right, int y) {
        float health = entity == null ? menu.health : entity.getHealth(), maximum = entity == null ? menu.maxHealth : entity.getMaxHealth();
        float share = Math.max(0F, Math.min(1F, health / Math.max(1F, maximum)));
        drawString(fontRenderer, String.format(Locale.ROOT, "HEALTH  %.1f / %.1f", health, maximum), x, y, 0xFFFFFFFF);
        Gui.drawRect(x, y + 11, right, y + 16, 0xFF29120D);
        Gui.drawRect(x, y + 11, x + (int) (share * (right - x)), y + 16, share > 0.3F ? 0xFFE34D35 : 0xFFFFB12E);
        String distance = entity == null || mc.player == null ? "DIST --" : String.format(Locale.ROOT, "DIST %.1fm", mc.player.getDistance(entity));
        long elapsed = mc.world == null ? 0L : Math.max(0L, mc.world.getTotalWorldTime() - openedAt);
        int seconds = (int) Math.max(0L, (menu.remainingTicks - elapsed) / 20L);
        drawString(fontRenderer, "ARMOR " + menu.armor + "   " + distance + "   TIME " + seconds + "s", x, y + 20, 0xFFE8DDBE);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private static class PortalButton extends GuiButton {
        private final int accent;

        PortalButton(int id, int x, int y, int width, int height, String text, int accent) {
            super(id, x, y, width, height, text);
            this.accent = accent;
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
            if (!visible) {
                return;
            }
            hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
            Gui.drawRect(x, y, x + width, y + height, hovered ? 0xFF332B19 : 0xE8161512);
            Gui.drawRect(x, y, x + width, y + 1, accent);
            drawCenteredString(mc.fontRenderer, displayString, x + width / 2, y + 5, hovered ? 0xFFFFFFFF : accent);
        }
    }
}
