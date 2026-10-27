package com.matoon.eternity.client.gui;

import com.matoon.eternity.entity.EntityDestroyerArmor;
import com.matoon.eternity.entity.Overcharge;
import com.matoon.eternity.inventory.ContainerDestroyerArmor;
import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.network.MsgArmorButton;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The Destroyer Armor's systems, as its pilot sees them: a rail of tabs down the left, one for each page, and the
 * open page beside it. Along the top, its name, the page's, and its vital signs at a glance from every page.
 * <ul>
 * <li><b>Status</b> ({@link StatusPage}): the armour itself, its vital signs and how each system holds up.</li>
 * <li><b>Supply</b> ({@link SupplyPage}): what feeds it, and Uru plate to mend it.</li>
 * <li><b>Forge</b> ({@link ForgePage}): its upgrades.</li>
 * <li><b>Cradle</b> ({@link CradlePage}): the Astral Regulator and the stones in its chest.</li>
 * <li><b>Overcharge</b> ({@link OverchargePage}): each part of it turned past what it was built for, and its core.</li>
 * <li><b>Paint</b> ({@link PaintPage}): its finishes.</li>
 * </ul>
 * A page is a {@link DestroyerPage}; another is one more class and one more line in {@link #pages}. The rail holds
 * seven before it would need to scroll. Everything is drawn here, in the Destroyer's own dark banded steel and the
 * orange of its fire.
 */
@SideOnly(Side.CLIENT)
public class GuiDestroyerArmor extends GuiContainer {

    /** How wide the rail of tabs is, left of the pages; and each tab on it. */
    static final int RAIL = 28, TAB_W = 24, TAB_H = 26, TAB_GAP = 3, HEADER = 20;

    /** Which page was open last, so it opens there again. */
    public static int tab;

    final EntityDestroyerArmor armor;
    final InventoryPlayer playerInventory;
    private final List<DestroyerPage> pages;
    private float time;

    public GuiDestroyerArmor(InventoryPlayer playerInventory, EntityDestroyerArmor armor) {
        super(new ContainerDestroyerArmor(playerInventory, armor));
        this.armor = armor;
        this.playerInventory = playerInventory;
        this.pages = Arrays.asList(new StatusPage(this), new SupplyPage(this), new ForgePage(this), new CradlePage(this), new OverchargePage(this),
                new PaintPage(this));
        xSize = RAIL + DestroyerPage.WIDTH;
        ySize = DestroyerPage.HEIGHT;
    }

    private DestroyerPage page() {
        tab = MathHelper.clamp(tab, 0, pages.size() - 1);
        return pages.get(tab);
    }

    private void open(int index) {
        if (index != tab && index >= 0 && index < pages.size()) {
            tab = index;
            place();
            click();
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        place();
    }

    /** Lays the slots out for the open page; slots that belong to another page go out of reach. */
    private void place() {
        DestroyerPage page = page();
        List<Slot> slots = inventorySlots.inventorySlots;
        for (int i = 0; i < slots.size(); i++) {
            Slot slot = slots.get(i);
            int x = -10000, y = -10000;
            if (i >= ContainerDestroyerArmor.PLAYER) {
                int n = i - ContainerDestroyerArmor.PLAYER;
                if (page.showsInventory()) {
                    x = RAIL + 62 + (n % 9) * 18;
                    y = n < 27 ? DestroyerPage.INVENTORY_TOP + 10 + (n / 9) * 18 : DestroyerPage.INVENTORY_TOP + 70;
                }
            } else {
                int[] at = page.slot(i);
                if (at != null) {
                    x = RAIL + at[0];
                    y = at[1];
                }
            }
            slot.xPos = x;
            slot.yPos = y;
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (armor.isDead) {
            mc.player.closeScreen();
        }
    }

    /** A button on a page pressed: to the server, for the menu open there. */
    void press(int id) {
        EternityNetwork.sendToServer(new MsgArmorButton(inventorySlots.windowId, id));
    }

    void click() {
        mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 0.8F));
    }

    /** An item's own tooltip, for pages that show items outside slots. */
    void itemTip(ItemStack stack, int x, int y) {
        renderToolTip(stack, x, y);
    }

    // ------------------------------------------------------------------ drawing

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < pages.size(); i++) {
            if (inside(mouseX, mouseY, tabX(), tabY(i), TAB_W, TAB_H)) {
                DestroyerPage page = pages.get(i);
                lines.add("§6" + page.title() + " §8[" + (i + 1) + "]");
                if (page.locked() != null) {
                    lines.add("§7" + page.locked());
                }
            }
        }
        if (lines.isEmpty() && getSlotUnderMouse() == null) {
            DestroyerPage page = page();
            page.at(guiLeft + RAIL, guiTop, time);
            page.hover(mouseX, mouseY, lines);
        }
        if (!lines.isEmpty()) {
            drawHoveringText(lines, mouseX, mouseY);
        } else {
            renderHoveredToolTip(mouseX, mouseY);
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        time = mc.player.ticksExisted + partialTicks;
        GlStateManager.color(1F, 1F, 1F, 1F);
        DestroyerPage page = page();
        frame(guiLeft, guiTop, xSize, ySize);
        header(page);
        rail(mouseX, mouseY);
        page.at(guiLeft + RAIL, guiTop, time);
        page.draw(mouseX, mouseY);
        if (page.showsInventory()) {
            page.small("INVENTORY", guiLeft + RAIL + 62, guiTop + DestroyerPage.INVENTORY_TOP, DestroyerPage.DIM);
            for (int i = ContainerDestroyerArmor.PLAYER; i < inventorySlots.inventorySlots.size(); i++) {
                Slot slot = inventorySlots.inventorySlots.get(i);
                page.socket(guiLeft + slot.xPos - 1, guiTop + slot.yPos - 1);
            }
        }
        GlStateManager.color(1F, 1F, 1F, 1F);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
    }

    /** The whole panel: dark banded steel, bevelled, rivets at its corners. */
    private void frame(int x, int y, int w, int h) {
        drawRect(x - 2, y - 2, x + w + 2, y + h + 2, DestroyerPage.VOID);
        drawRect(x - 1, y - 1, x + w + 1, y + h + 1, DestroyerPage.RIM);
        drawGradientRect(x, y, x + w, y + h, 0xFF17181C, 0xFF0E0F12);
        // Bands across it, the way the Destroyer's own plates run.
        for (int band = y + 24; band < y + h; band += 26) {
            drawRect(x + RAIL, band, x + w - 1, band + 1, 0x14FFFFFF);
        }
        drawRect(x, y, x + w, y + 1, DestroyerPage.SHINE);
        drawRect(x, y, x + 1, y + h, DestroyerPage.EDGE);
        drawRect(x + w - 1, y, x + w, y + h, DestroyerPage.VOID);
        drawRect(x, y + h - 1, x + w, y + h, DestroyerPage.VOID);
        int[][] corners = {{x + w - 6, y + h - 6}, {x + 3, y + h - 6}};
        for (int[] c : corners) {
            drawRect(c[0], c[1], c[0] + 3, c[1] + 3, DestroyerPage.RIM);
            drawRect(c[0], c[1], c[0] + 2, c[1] + 2, DestroyerPage.SHINE);
        }
    }

    /** Its name and the page's along the top, its vital signs beside them, and a line of the furnace's light under them. */
    private void header(DestroyerPage page) {
        int x = guiLeft, y = guiTop;
        drawGradientRect(x + 1, y + 1, x + xSize - 1, y + HEADER - 1, 0xFF24262C, 0xFF131418);
        fontRenderer.drawString("DESTROYER", x + 8, y + 6, DestroyerPage.GOLD);
        fontRenderer.drawString(page.title().toUpperCase(), x + 8 + fontRenderer.getStringWidth("DESTROYER "), y + 6, DestroyerPage.PALE);
        // Its vital signs, from every page.
        List<Object[]> signs = new ArrayList<>();
        float integrity = armor.getHealth() / armor.getMaxHealth();
        signs.add(new Object[]{"FRAME", integrity, armor.condition().color, armor.condition().color});
        signs.add(new Object[]{armor.electric() ? "ENERGY" : "LAVA", armor.getLava() / armor.lavaCapacity(), armor.electric() ? 0x8A1A24 : 0xA8380C,
                armor.electric() ? 0xFF4050 : 0xFFB040});
        signs.add(new Object[]{"FURNACE", armor.getFurnace() / EntityDestroyerArmor.HEAT_MAX, 0xB04010, 0xFFF0A0});
        if (armor.canOvercharge()) {
            float core = armor.core() / Overcharge.CORE_MAX;
            signs.add(new Object[]{armor.breach() > 0 ? "BREACH" : "CORE", core, 0x5A1008, DestroyerPage.coreColor(core) & 0xFFFFFF});
        }
        int w = 34, gap = 6, sx = x + xSize - 8 - signs.size() * (w + gap) + gap;
        DestroyerPage any = page;
        for (Object[] sign : signs) {
            float f = (Float) sign[1];
            boolean alarm = "BREACH".equals(sign[0]);
            any.small((String) sign[0], sx, y + 4, alarm ? DestroyerPage.RED : DestroyerPage.DIM);
            any.bar(sx, y + 12, w, 3, f, (Integer) sign[2], (Integer) sign[3]);
            sx += w + gap;
        }
        float pulse = 0.6F + 0.4F * MathHelper.sin(time * 0.15F);
        drawGradientRect(x + 1, y + HEADER - 1, x + xSize - 1, y + HEADER, DestroyerPage.alpha(DestroyerPage.EMBER, 0.85F * pulse),
                DestroyerPage.alpha(DestroyerPage.EMBER, 0.85F * pulse));
        drawGradientRect(x + RAIL, y + HEADER, x + xSize - 1, y + HEADER + 3, DestroyerPage.alpha(DestroyerPage.EMBER, 0.25F * pulse),
                DestroyerPage.alpha(DestroyerPage.EMBER, 0F));
    }

    private int tabX() {
        return guiLeft + 2;
    }

    private int tabY(int i) {
        return guiTop + HEADER + 4 + i * (TAB_H + TAB_GAP);
    }

    /** The rail of tabs: each page's picture, the open one lit and joined to its page, a padlock on those not yet forged. */
    private void rail(int mouseX, int mouseY) {
        int x = guiLeft, y = guiTop + HEADER;
        drawGradientRect(x + 1, y, x + RAIL - 1, y + ySize - HEADER - 1, 0xFF101114, 0xFF0B0C0E);
        drawRect(x + RAIL - 1, y, x + RAIL, y + ySize - HEADER - 1, DestroyerPage.EDGE);
        for (int i = 0; i < pages.size(); i++) {
            DestroyerPage page = pages.get(i);
            int tx = tabX(), ty = tabY(i);
            boolean open = tab == i, over = inside(mouseX, mouseY, tx, ty, TAB_W, TAB_H);
            boolean locked = page.locked() != null;
            if (open) {
                // Joined to the page: no edge between them.
                drawRect(tx, ty, x + RAIL, ty + TAB_H, 0xFF2A2420);
                drawRect(tx, ty, tx + 2, ty + TAB_H, 0xFF000000 | DestroyerPage.EMBER);
            } else {
                drawRect(tx, ty, tx + TAB_W, ty + TAB_H, over ? 0xFF22242A : 0xFF16171B);
                drawRect(tx, ty, tx + TAB_W, ty + 1, DestroyerPage.RIM);
                drawRect(tx + TAB_W - 1, ty, tx + TAB_W, ty + TAB_H, DestroyerPage.VOID);
            }
            page.icon(DestroyerPage.TAB_ICONS + page.icon(), tx + 4, ty + 3);
            if (!open && !over) {
                drawRect(tx + 4, ty + 3, tx + 20, ty + 19, locked ? 0xA0000000 : 0x50000000);
            }
            if (locked) {
                // A small padlock in its corner.
                int lx = tx + TAB_W - 9, ly = ty + 2;
                drawRect(lx + 1, ly, lx + 5, ly + 1, DestroyerPage.SHINE);
                drawRect(lx + 1, ly, lx + 2, ly + 3, DestroyerPage.SHINE);
                drawRect(lx + 4, ly, lx + 5, ly + 3, DestroyerPage.SHINE);
                drawRect(lx, ly + 3, lx + 6, ly + 7, DestroyerPage.SHINE);
            }
            page.small(String.valueOf(i + 1), tx + 4, ty + TAB_H - 6, open ? DestroyerPage.GOLD : DestroyerPage.DIM);
        }
    }

    // ------------------------------------------------------------------ input

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        if (button == 0) {
            for (int i = 0; i < pages.size(); i++) {
                if (inside(mouseX, mouseY, tabX(), tabY(i), TAB_W, TAB_H)) {
                    open(i);
                    return;
                }
            }
        }
        DestroyerPage page = page();
        page.at(guiLeft + RAIL, guiTop, time);
        if (getSlotUnderMouse() == null && page.click(mouseX, mouseY, button)) {
            return;
        }
        super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            int mouseX = Mouse.getEventX() * width / mc.displayWidth;
            int mouseY = height - Mouse.getEventY() * height / mc.displayHeight - 1;
            if (inside(mouseX, mouseY, guiLeft, guiTop + HEADER, RAIL, ySize - HEADER)) {
                // The wheel over the rail pages through the tabs.
                open(MathHelper.clamp(tab + (wheel > 0 ? -1 : 1), 0, pages.size() - 1));
                return;
            }
            DestroyerPage page = page();
            page.at(guiLeft + RAIL, guiTop, time);
            page.scroll(mouseX, mouseY, wheel > 0 ? 1 : -1);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        // Number keys page through it, as well as the inventory key closing it.
        if (keyCode >= Keyboard.KEY_1 && keyCode < Keyboard.KEY_1 + pages.size() && !isShiftKeyDown() && getSlotUnderMouse() == null) {
            open(keyCode - Keyboard.KEY_1);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
