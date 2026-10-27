package com.matoon.eternity.client.gui;

import com.matoon.eternity.Eternity;
import com.matoon.eternity.entity.EntityDestroyerArmor;
import com.matoon.eternity.entity.Overcharge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

/**
 * One page of the Destroyer Armor's systems menu ({@link GuiDestroyerArmor}), behind its own tab on the rail down
 * its left side. A page draws itself into the panel right of the rail, {@link #WIDTH} wide; one that shows the
 * player's inventory along the bottom keeps above {@link #INVENTORY_TOP}. Everything shared (the Destroyer's palette,
 * its insets, slots, gauges, buttons and small writing) is here, so a new page is only what is new about it.
 * <p>
 * Coordinates handed to a page are the screen's; {@link #left} and {@link #top} are the corner of its panel.
 */
@SideOnly(Side.CLIENT)
public abstract class DestroyerPage extends Gui {

    /** The panel's size, right of the rail; and where the inventory starts on pages that show it. */
    public static final int WIDTH = 284, HEIGHT = 236, INVENTORY_TOP = 142;
    static final ResourceLocation ICONS = new ResourceLocation(Eternity.MODID, "textures/gui/destroyer_icons.png");
    /** Where the tabs' and the overcharge parts' pictures start on the icon sheet. */
    static final int TAB_ICONS = 32, PART_ICONS = 48;

    // Its palette: dark steel, lighter steel at the edges, old gold for writing, and the furnace's orange.
    static final int VOID = 0xFF07070A, IRON = 0xFF121317, STEEL = 0xFF1D1F24, EDGE = 0xFF2B2E35, RIM = 0xFF41454E, SHINE = 0xFF666C76;
    static final int GOLD = 0xD8B070, PALE = 0x9A9387, DIM = 0x5E5A52, EMBER = 0xFF7A20, RED = 0xFF3B2B, GREEN = 0x7FD86A, AMBER = 0xE8C050,
            ENERGY = 0xE05060, VIOLET = 0xC890FF;

    protected final GuiDestroyerArmor gui;
    protected final EntityDestroyerArmor armor;
    protected final Minecraft mc;
    protected final FontRenderer font;
    protected final RenderItem items;
    /** The corner of its panel on screen, and the time, set before each draw, click and hover. */
    protected int left, top;
    protected float time;

    protected DestroyerPage(GuiDestroyerArmor gui) {
        this.gui = gui;
        this.armor = gui.armor;
        this.mc = Minecraft.getMinecraft();
        this.font = mc.fontRenderer;
        this.items = mc.getRenderItem();
    }

    /** Its name, on its tab and over the panel. */
    public abstract String title();

    /** Its tab's picture: which, after {@link #TAB_ICONS}, on the icon sheet. */
    public abstract int icon();

    /** Why it has nothing to offer yet, or nothing. It still opens, to say so. */
    public String locked() {
        return null;
    }

    /** Whether the player's own inventory shows along the bottom of it. */
    public boolean showsInventory() {
        return false;
    }

    /** Where one of the armour's own slots sits on it, against its panel's corner, or nothing if not here. */
    public int[] slot(int index) {
        return null;
    }

    public abstract void draw(int mouseX, int mouseY);

    /** A click; whether it was taken. */
    public boolean click(int mouseX, int mouseY, int button) {
        return false;
    }

    /** The mouse wheel turned over it, up for positive; whether it was taken. */
    public boolean scroll(int mouseX, int mouseY, int direction) {
        return false;
    }

    /** Words for whatever is under the mouse. */
    public void hover(int mouseX, int mouseY, List<String> lines) {
    }

    /** Set before anything is asked of it. */
    void at(int left, int top, float time) {
        this.left = left;
        this.top = top;
        this.time = time;
    }

    // ------------------------------------------------------------------ shared pieces

    static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** A recessed box. */
    void inset(int x, int y, int w, int h) {
        drawRect(x, y, x + w, y + h, VOID);
        drawRect(x + 1, y + 1, x + w, y + h, EDGE);
        drawGradientRect(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF0C0D10, 0xFF101114);
    }

    /** A recessed box with a heading along its top. */
    void panel(int x, int y, int w, int h, String heading) {
        inset(x, y, w, h);
        if (heading != null) {
            small(heading, x + 5, y + 4, DIM);
            drawRect(x + 4, y + 11, x + w - 4, y + 12, 0xFF1A1C21);
        }
    }

    /** An 18-pixel slot hole. */
    void socket(int x, int y) {
        drawRect(x, y, x + 18, y + 18, VOID);
        drawRect(x + 1, y + 1, x + 18, y + 18, RIM);
        drawRect(x + 1, y + 1, x + 17, y + 17, 0xFF0A0A0D);
    }

    /** A slot hole, with a faint picture of what goes in it while it is empty. */
    void slotFrame(int x, int y, ItemStack ghost) {
        socket(x, y);
        if (ghost != null) {
            RenderHelper.enableGUIStandardItemLighting();
            items.renderItemAndEffectIntoGUI(ghost, x + 1, y + 1);
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableDepth();
            drawRect(x + 1, y + 1, x + 17, y + 17, 0xB00A0A0D);
            GlStateManager.enableDepth();
        }
    }

    void arrow(int x, int y, int color) {
        drawRect(x, y + 3, x + 14, y + 6, color);
        for (int i = 0; i < 5; i++) {
            drawRect(x + 14 + i, y + i, x + 15 + i, y + 9 - i, color);
        }
    }

    void legend(int x, int y, int color, String text) {
        drawRect(x, y + 1, x + 6, y + 7, color);
        drawRect(x + 1, y + 2, x + 5, y + 6, 0xFF101114);
        small(text, x + 9, y + 1, PALE);
    }

    /** A button: lit in the furnace's colour when it can be pressed, or in another; dark when it cannot. */
    void button(int x, int y, int w, int h, String text, boolean can, boolean over, int lit) {
        int top = can ? (over ? lighten(lit, -0.35F) : lighten(lit, -0.5F)) : 0xFF1A1B20;
        int bottom = can ? lighten(lit, -0.78F) : 0xFF111215;
        drawRect(x - 1, y - 1, x + w + 1, y + h + 1, can ? alpha(lit, over ? 1F : 0.8F) : EDGE);
        drawGradientRect(x, y, x + w, y + h, top, bottom);
        font.drawStringWithShadow(text, x + w / 2F - font.getStringWidth(text) / 2F, y + (h - 8) / 2F + 1F, can ? 0xFFE8C8 : DIM);
    }

    /** An icon off the Destroyer's sheet, by its place on it. */
    void icon(int index, int x, int y) {
        mc.getTextureManager().bindTexture(ICONS);
        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.enableBlend();
        GlStateManager.disableLighting();
        drawModalRectWithCustomSizedTexture(x, y, (index % 16) * 16, (index / 16) * 16, 16, 16, 256F, 64F);
    }

    /** A name and its value on one line, the value against the right edge. */
    void label(String name, int x, int y, int width, String value, int color) {
        small(name, x, y, PALE);
        small(value, x + width - (int) (font.getStringWidth(value) * 0.75F), y, color);
    }

    /** Writing at three quarters size. */
    void small(String text, int x, int y, int color) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0F);
        GlStateManager.scale(0.75F, 0.75F, 1F);
        font.drawString(text, 0, 0, color);
        GlStateManager.popMatrix();
    }

    void smallCentered(String text, int cx, int y, int color) {
        small(text, cx - (int) (font.getStringWidth(text) * 0.375F), y, color);
    }

    /** Small writing wrapped to a width; gives back where the next line would go. */
    int wrapSmall(String text, int x, int y, int width, int color) {
        for (String line : font.listFormattedStringToWidth(text, (int) (width / 0.75F))) {
            small(line, x, y, color);
            y += 7;
        }
        return y;
    }

    String shorten(String text, int width) {
        int max = (int) (width / 0.75F);
        if (font.getStringWidth(text) <= max) {
            return text;
        }
        return font.trimStringToWidth(text, max - font.getStringWidth("..")) + "..";
    }

    /** Darkened over a page with nothing to offer yet, and why. */
    void lockedOver(int x, int y, int w, int h, String why) {
        GlStateManager.disableDepth();
        drawRect(x, y, x + w, y + h, 0xD0060608);
        int cx = x + w / 2, ty = y + h / 2 - 16;
        String head = "NOT YET FORGED";
        font.drawString(head, cx - font.getStringWidth(head) / 2, ty, GOLD);
        wrapCentered(why, cx, ty + 13, Math.min(w - 20, 180), PALE);
        GlStateManager.enableDepth();
    }

    void wrapCentered(String text, int cx, int y, int width, int color) {
        for (String line : font.listFormattedStringToWidth(text, (int) (width / 0.75F))) {
            smallCentered(line, cx, y, color);
            y += 7;
        }
    }

    // ------------------------------------------------------------------ gauges

    void bar(int x, int y, int w, int h, float f, int from, int to) {
        drawRect(x - 1, y - 1, x + w + 1, y + h + 1, VOID);
        drawRect(x, y, x + w, y + h, 0xFF15161A);
        int fill = (int) (w * MathHelper.clamp(f, 0F, 1F));
        if (fill > 0) {
            drawGradientRect(x, y, x + fill, y + h, 0xFF000000 | from, 0xFF000000 | to);
        }
    }

    void energyBar(int x, int y, int w, int h, float f) {
        drawRect(x - 1, y - 1, x + w + 1, y + h + 1, VOID);
        drawRect(x, y, x + w, y + h, 0xFF15161A);
        int fill = (int) (w * MathHelper.clamp(f, 0F, 1F));
        for (int i = 0; i < fill; i++) {
            // Forge Energy's red, a current running along it.
            float wave = 0.75F + 0.25F * MathHelper.sin(i * 0.4F - time * 0.6F);
            drawRect(x + i, y, x + i + 1, y + h, blend(0xFF5A0A14, 0xFFFF4050, wave));
        }
        if (fill > 0) {
            drawRect(x + fill - 1, y, x + fill, y + h, 0xC0FFC0C8);
        }
    }

    void heatBar(int x, int y, int w, int h, float f) {
        drawRect(x - 1, y - 1, x + w + 1, y + h + 1, VOID);
        drawRect(x, y, x + w, y + h, 0xFF15161A);
        int fill = (int) (w * MathHelper.clamp(f, 0F, 1F));
        for (int i = 0; i < fill; i++) {
            drawRect(x + i, y, x + i + 1, y + h, blend(0xFFB04010, 0xFFFFF0A0, i / (float) w));
        }
        drawRect(x + (int) (w * 0.9F), y, x + (int) (w * 0.9F) + 1, y + h, 0x90FF3020);
    }

    /** Its core's heat: dull red to white, flashing as it breaches. */
    void coreBar(int x, int y, int w, int h) {
        float f = armor.core() / Overcharge.CORE_MAX;
        boolean breach = armor.breach() > 0;
        drawRect(x - 1, y - 1, x + w + 1, y + h + 1, breach ? 0xFFFF3020 : VOID);
        drawRect(x, y, x + w, y + h, 0xFF15161A);
        int fill = (int) (w * MathHelper.clamp(f, 0F, 1F));
        for (int i = 0; i < fill; i++) {
            float at = i / (float) w;
            drawRect(x + i, y, x + i + 1, y + h, coreColor(at));
        }
        if (breach) {
            // How long it has left, eaten from the right.
            int left = (int) (w * (1F - armor.breach() / (float) Overcharge.BREACH_TICKS));
            drawRect(x + left, y + h - 2, x + w, y + h, 0xFFFFFFFF);
        }
    }

    /** The colour of its core's heat at a share of its limit. */
    static int coreColor(float at) {
        return at < 0.5F ? blend(0xFF5A1008, 0xFFE04010, at * 2F) : blend(0xFFE04010, 0xFFFFF4D0, (at - 0.5F) * 2F);
    }

    /** What its core's heat amounts to, in a word, and its colour. */
    String coreWord() {
        float core = armor.core();
        if (armor.breach() > 0) {
            return String.format("BREACH %.1fs", (Overcharge.BREACH_TICKS - armor.breach()) / 20F);
        }
        if (armor.scorching()) {
            return "TEARING APART";
        }
        return core < 1F ? "Cold" : core < 35F ? "Stable" : core < 65F ? "Warm" : core < 90F ? "HOT" : "CRITICAL";
    }

    int coreWordColor() {
        float core = armor.core();
        return armor.breach() > 0 || armor.scorching() || core >= 90F ? RED : core >= 65F ? 0xFF8040 : core >= 35F ? AMBER : GREEN;
    }

    // ------------------------------------------------------------------ colour

    static String hex(int color) {
        return Integer.toHexString(color & 0xFFFFFF);
    }

    static int alpha(int rgb, float a) {
        return ((int) (MathHelper.clamp(a, 0F, 1F) * 255F) << 24) | (rgb & 0xFFFFFF);
    }

    static int lighten(int rgb, float by) {
        int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
        if (by >= 0F) {
            r += (int) ((255 - r) * by);
            g += (int) ((255 - g) * by);
            b += (int) ((255 - b) * by);
        } else {
            r = (int) (r * (1F + by));
            g = (int) (g * (1F + by));
            b = (int) (b * (1F + by));
        }
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    static int blend(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
        int g = (int) (((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
        int bl = (int) ((a & 255) + ((b & 255) - (a & 255)) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    static String roman(int n) {
        return n == 1 ? "I" : n == 2 ? "II" : n == 3 ? "III" : n == 4 ? "IV" : String.valueOf(n);
    }
}
