package com.matoon.eternity.client;

import com.matoon.eternity.EternityConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Locale;
import java.util.Random;

/**
 * Health past ten hearts, held the way the stones and the Astral Regulator lend it.
 * <p>
 * Vanilla draws a heart for every two points of maximum health, stacking the rows ever closer together: a million
 * points is half a million hearts, drawn every frame, and the game crawls. Past one row, the hearts above the hotbar
 * are drawn here instead, always one row of ten, filled as far as their health is, with vanilla's own look (poison,
 * wither, hardcore, regeneration's wave, the blink of a blow, the shake when low). The true numbers go in the bottom
 * left: a heart, then health and maximum, counted in hearts as the row is (a full row of ten reads 10).
 */
public final class HealthHud {

    public static final HealthHud INSTANCE = new HealthHud();

    /** One row of hearts: what vanilla draws without stacking. */
    private static final float ROW = 20F;
    /** The counter's distance from the screen's left and bottom edges. */
    private static final int PADDING = 10;
    private static final int TEXT = 0xFFFFE4E4, OUTLINE = 0xFF2A1416, ABSORB = 0xFFFFD84A;
    private static final int HURT_TINT = 0xFFFF6A6A, HEAL_TINT = 0xFF9CF5A8;

    private final Random rand = new Random();
    private float lastHealth = -1F;
    private int blinkUntil;

    // The counter's easing.
    private long lastFrame;
    private int counted = Integer.MIN_VALUE;
    private double shownHealth, shownMax = -1D;
    private float shownAbsorb, seenHealth, absorbAlpha, hurt, healed, bump;

    private HealthHud() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onRenderHealth(RenderGameOverlayEvent.Pre event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.HEALTH) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (!(mc.getRenderViewEntity() instanceof EntityPlayer)) {
            return;
        }
        EntityPlayer player = (EntityPlayer) mc.getRenderViewEntity();
        ScaledResolution res = event.getResolution();
        float health = player.getHealth(), max = player.getMaxHealth(), absorb = player.getAbsorptionAmount();
        if (EternityConfig.client.healthCounter) {
            drawCounter(mc, res, player, health, max, absorb);
        }
        if (max + absorb > ROW) {
            event.setCanceled(true);
            drawHearts(mc, res, player, health, max, absorb);
        }
        // Vanilla's hearts, if they draw, expect their sheet.
        mc.getTextureManager().bindTexture(Gui.ICONS);
    }

    // ------------------------------------------------------------------ the row

    private void drawHearts(Minecraft mc, ScaledResolution res, EntityPlayer player, float health, float max, float absorb) {
        int tick = mc.ingameGUI.getUpdateCounter();
        // Blows blink the row, as vanilla does: for a second after a loss, half that after a gain.
        if (lastHealth >= 0F && player.hurtResistantTime > 0 && health != lastHealth) {
            blinkUntil = tick + (health < lastHealth ? 20 : 10);
        }
        float before = lastHealth < 0F ? health : lastHealth;
        lastHealth = health;
        boolean blink = blinkUntil > tick && (blinkUntil - tick) / 3 % 2 == 1;

        // Scaled to one row. Absorption keeps its share of the hearts at the end, at least one, as vanilla's yellow
        // hearts follow the red; the rest are maximum health, filled as far as their health is.
        int absorbHearts = absorb > 0F ? MathHelper.clamp(Math.round(absorb / (max + absorb) * 10F), 1, 9) : 0;
        int healthHalves = (int) ROW - absorbHearts * 2;
        int shown = scaled(health, max, healthHalves), shownBefore = scaled(before, max, healthHalves);
        int absorbFrom = healthHalves, absorbTo = (int) ROW;

        int left = res.getScaledWidth() / 2 - 91, top = res.getScaledHeight() - GuiIngameForge.left_height;
        GuiIngameForge.left_height += 10;
        int regen = player.isPotionActive(MobEffects.REGENERATION) ? tick % 25 : -1;
        int v = mc.world.getWorldInfo().isHardcoreModeEnabled() ? 45 : 0;
        int margin = heartMargin(player), background = blink ? 25 : 16;
        rand.setSeed(tick * 312871L);

        mc.getTextureManager().bindTexture(Gui.ICONS);
        GlStateManager.enableBlend();
        GlStateManager.color(1F, 1F, 1F, 1F);
        for (int i = 9; i >= 0; i--) {
            int x = left + i * 8, y = top;
            if (shown <= 4) {
                y += rand.nextInt(2);
            }
            if (i == regen) {
                y -= 2;
            }
            mc.ingameGUI.drawTexturedModalRect(x, y, background, v, 9, 9);
            int half = i * 2 + 1;
            if (blink) {
                if (half < shownBefore) {
                    mc.ingameGUI.drawTexturedModalRect(x, y, margin + 54, v, 9, 9);
                } else if (half == shownBefore) {
                    mc.ingameGUI.drawTexturedModalRect(x, y, margin + 63, v, 9, 9);
                }
            }
            if (half < shown) {
                mc.ingameGUI.drawTexturedModalRect(x, y, margin + 36, v, 9, 9);
            } else if (half == shown) {
                mc.ingameGUI.drawTexturedModalRect(x, y, margin + 45, v, 9, 9);
            } else if (half > absorbFrom && half <= absorbTo) {
                mc.ingameGUI.drawTexturedModalRect(x, y, half < absorbTo ? margin + 144 : margin + 153, v, 9, 9);
            }
        }
        GlStateManager.disableBlend();
    }

    /** How many of {@code halves} half-hearts a share of maximum health fills: none only when there is none. */
    private static int scaled(float value, float max, int halves) {
        return value > 0F ? MathHelper.clamp(MathHelper.ceil(value / max * halves), 1, halves) : 0;
    }

    /** Where on the icon sheet their hearts are: red, or poison's green, or wither's black. */
    private static int heartMargin(EntityPlayer player) {
        if (player.isPotionActive(MobEffects.POISON)) {
            return 52;
        }
        return player.isPotionActive(MobEffects.WITHER) ? 88 : 16;
    }

    // ------------------------------------------------------------------ the numbers

    /**
     * The counter, eased rather than jumping: the numbers run to their new values, a blow bumps the heart and warms the
     * text red for a moment, healing cools it green, absorption fades in and out, and below a quarter the heart beats,
     * faster the lower they are. All of it small, and settled within a second.
     */
    private void drawCounter(Minecraft mc, ScaledResolution res, EntityPlayer player, float health, float max, float absorb) {
        long now = System.nanoTime();
        float dt = lastFrame == 0L ? 0F : Math.min(0.1F, (now - lastFrame) / 1e9F);
        lastFrame = now;
        if (player.getEntityId() != counted || shownMax < 0D) {
            // Someone new, or a new world: start where they are.
            counted = player.getEntityId();
            shownHealth = health;
            shownMax = max;
            shownAbsorb = absorb;
            seenHealth = health;
            hurt = healed = bump = 0F;
        }
        if (health < seenHealth - 0.01F) {
            hurt = 1F;
            bump = 1F;
            healed = 0F;
        } else if (health > seenHealth + 0.01F) {
            healed = 1F;
            hurt = Math.min(hurt, 0.3F);
        }
        seenHealth = health;
        float ease = 1F - (float) Math.exp(-dt * 9D);
        shownHealth = approach(shownHealth, health, ease);
        shownMax = approach(shownMax, max, ease);
        if (absorb > 0F) {
            shownAbsorb = absorb;
        }
        absorbAlpha += ((absorb > 0F ? 1F : 0F) - absorbAlpha) * (1F - (float) Math.exp(-dt * 8D));
        hurt = Math.max(0F, hurt - dt * 1.6F);
        healed = Math.max(0F, healed - dt * 1.4F);
        bump = Math.max(0F, bump - dt * 3.5F);

        float share = max > 0F ? health / max : 0F;
        float beat = 0F;
        if (share > 0F && share < 0.25F) {
            // Two quick swells a beat, like a pulse; from about once every 1.4 seconds down to once every 0.7.
            float period = 0.7F + share / 0.25F * 0.7F;
            float phase = (now / 1e9F) % period / period;
            beat = (float) (Math.max(0D, Math.sin(phase * Math.PI * 4D)) * (phase < 0.5F ? 1D : 0D)) * 0.08F;
        }
        float heartScale = 1F + bump * bump * 0.16F + beat;
        int color = blend(blend(TEXT, HURT_TINT, hurt * 0.75F), HEAL_TINT, healed * 0.6F);

        FontRenderer font = mc.fontRenderer;
        String current = format((float) shownHealth), maximum = format((float) shownMax);
        // About one and a half times the GUI's size, rounded to whole screen pixels so the font stays crisp.
        int factor = res.getScaleFactor();
        float scale = Math.max(factor + 1, Math.round(factor * 1.5F)) / (float) factor;
        int y = res.getScaledHeight() - PADDING - Math.round(9 * scale);

        GlStateManager.pushMatrix();
        GlStateManager.translate(PADDING, y, 0F);
        GlStateManager.scale(scale, scale, 1F);
        GlStateManager.enableBlend();

        GlStateManager.pushMatrix();
        GlStateManager.translate(4.5F, 4.5F, 0F);
        GlStateManager.scale(heartScale, heartScale, 1F);
        GlStateManager.translate(-4.5F, -4.5F, 0F);
        mc.getTextureManager().bindTexture(Gui.ICONS);
        GlStateManager.color(1F, 1F, 1F, 1F);
        int v = mc.world.getWorldInfo().isHardcoreModeEnabled() ? 45 : 0;
        mc.ingameGUI.drawTexturedModalRect(0, 0, 16, v, 9, 9);
        mc.ingameGUI.drawTexturedModalRect(0, 0, heartMargin(player) + 36, v, 9, 9);
        GlStateManager.popMatrix();

        // The font's own '|' is a broken bar; the divider is drawn solid.
        int at = 12;
        outlined(font, current, at, 1, color, 1F);
        at += font.getStringWidth(current) + 3;
        Gui.drawRect(at - 1, 0, at + 2, 9, OUTLINE);
        Gui.drawRect(at, 1, at + 1, 8, TEXT);
        at += 5;
        outlined(font, maximum, at, 1, TEXT, 1F);
        if (absorbAlpha > 0.05F) {
            outlined(font, " +" + format(shownAbsorb), at + font.getStringWidth(maximum), 1, ABSORB, absorbAlpha);
        }
        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    /** Eased toward a value, by a share of the way each frame, closing the last sliver at once. */
    private static double approach(double from, double to, float ease) {
        double next = from + (to - from) * ease;
        return Math.abs(to - next) < Math.max(0.05D, Math.abs(to) * 1e-4D) ? to : next;
    }

    private static int blend(int from, int to, float amount) {
        amount = MathHelper.clamp(amount, 0F, 1F);
        int r = (int) ((from >> 16 & 255) + ((to >> 16 & 255) - (from >> 16 & 255)) * amount);
        int g = (int) ((from >> 8 & 255) + ((to >> 8 & 255) - (from >> 8 & 255)) * amount);
        int b = (int) ((from & 255) + ((to & 255) - (from & 255)) * amount);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    /** Text with a dark rim all round, so it reads over anything, faded by {@code alpha}. */
    private static void outlined(FontRenderer font, String text, int x, int y, int color, float alpha) {
        int a = MathHelper.clamp((int) (alpha * 255F), 8, 255) << 24;
        int outline = a | OUTLINE & 0xFFFFFF;
        font.drawString(text, x - 1, y, outline);
        font.drawString(text, x + 1, y, outline);
        font.drawString(text, x, y - 1, outline);
        font.drawString(text, x, y + 1, outline);
        font.drawString(text, x, y, a | color & 0xFFFFFF);
    }

    /**
     * Health points told in hearts, two points to a heart, so a full row of ten reads ten: in halves, as the row draws
     * them; shortened past a hundred thousand, and past all reckoning, infinite.
     */
    static String format(float value) {
        if (Float.isInfinite(value) || value >= 1e15F) {
            return "∞";
        }
        double halves = Math.ceil(value), whole = halves / 2D;
        if (whole < 100_000D) {
            long hearts = (long) halves / 2L;
            return (long) halves % 2L == 0L ? String.valueOf(hearts) : hearts + ".5";
        }
        String[] units = {"K", "M", "B", "T"};
        int unit = -1;
        while (whole >= 1000D && unit < units.length - 1) {
            whole /= 1000D;
            unit++;
        }
        return (whole < 100D ? String.format(Locale.ROOT, "%.1f", whole) : String.valueOf((long) whole)) + units[unit];
    }
}
