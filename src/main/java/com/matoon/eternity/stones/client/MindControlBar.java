package com.matoon.eternity.stones.client;

import com.matoon.eternity.Eternity;
import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.stones.network.MsgMindControlAct;
import com.matoon.eternity.stones.network.MsgMindControlEntries;
import lucraft.mods.lucraftcore.util.abilitybar.EnumAbilityBarColor;
import lucraft.mods.lucraftcore.util.abilitybar.IAbilityBarEntry;
import lucraft.mods.lucraftcore.util.abilitybar.IAbilityBarProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Everyone the Mind and Soul Stones' holder controls, on their ability bar, each by their face, to look into. A player's
 * face is cut from their skin; a creature's is drawn once into a little framebuffer and kept.
 */
public class MindControlBar implements IAbilityBarProvider {

    private static final ResourceLocation FALLBACK = new ResourceLocation(Eternity.MODID, "textures/abilities/mind_control.png");

    private static List<MsgMindControlEntries.Entry> entries = Collections.emptyList();
    private static int revision;
    private static int nextGlobalCapture;

    private final Map<UUID, Companion> cached = new HashMap<>();
    private List<IAbilityBarEntry> displayed = Collections.emptyList();
    private int seen = -1;

    static void set(List<MsgMindControlEntries.Entry> incoming) {
        entries = Collections.unmodifiableList(new ArrayList<>(incoming));
        revision++;
    }

    private static boolean contains(UUID id) {
        for (MsgMindControlEntries.Entry entry : entries) {
            if (entry.id.equals(id)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<IAbilityBarEntry> getEntries() {
        if (seen == revision) {
            return displayed;
        }
        seen = revision;
        Set<UUID> keep = new HashSet<>();
        List<IAbilityBarEntry> next = new ArrayList<>();
        for (MsgMindControlEntries.Entry entry : entries) {
            keep.add(entry.id);
            Companion companion = cached.get(entry.id);
            if (companion == null || !companion.matches(entry)) {
                if (companion != null) {
                    companion.dispose();
                }
                companion = new Companion(entry);
                cached.put(entry.id, companion);
            }
            next.add(companion);
        }
        for (Iterator<Map.Entry<UUID, Companion>> it = cached.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Companion> entry = it.next();
            if (!keep.contains(entry.getKey())) {
                entry.getValue().dispose();
                it.remove();
            }
        }
        displayed = Collections.unmodifiableList(next);
        return displayed;
    }

    private static class Companion implements IAbilityBarEntry {
        final MsgMindControlEntries.Entry entry;
        Framebuffer head;
        int nextCapture;

        Companion(MsgMindControlEntries.Entry entry) {
            this.entry = entry;
        }

        boolean matches(MsgMindControlEntries.Entry other) {
            return entry.entityId == other.entityId && entry.player == other.player && entry.name.equals(other.name);
        }

        void dispose() {
            if (head != null) {
                head.deleteFramebuffer();
                head = null;
            }
        }

        @Override
        public boolean isActive() {
            return contains(entry.id);
        }

        @Override
        public void onButtonPress() {
            EternityNetwork.sendToServer(MsgMindControlAct.of(MsgMindControlAct.OPEN, entry.id));
        }

        @Override
        public void onButtonRelease() {
        }

        @Override
        public void drawIcon(Minecraft mc, Gui gui, int x, int y) {
            Entity found = mc.world == null ? null : mc.world.getEntityByID(entry.entityId);
            if (found instanceof AbstractClientPlayer) {
                mc.getTextureManager().bindTexture(((AbstractClientPlayer) found).getLocationSkin());
                GlStateManager.color(1F, 1F, 1F, 1F);
                Gui.drawScaledCustomSizeModalRect(x, y, 8, 8, 8, 8, 16, 16, 64, 64);
                Gui.drawScaledCustomSizeModalRect(x, y, 40, 8, 8, 8, 16, 16, 64, 64);
                return;
            }
            // A creature's renderer can be missing the tick it arrives; try again now and then, a couple of ticks
            // apart across all of them.
            if (head == null && found instanceof EntityLivingBase && mc.player != null && mc.player.ticksExisted >= nextCapture
                    && mc.player.ticksExisted >= nextGlobalCapture) {
                nextGlobalCapture = mc.player.ticksExisted + 2;
                capture(mc, (EntityLivingBase) found);
            }
            if (head != null) {
                drawFramebuffer(head, x, y);
                return;
            }
            mc.getTextureManager().bindTexture(FALLBACK);
            Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, 16, 16, 16, 16);
        }

        /** Their head, eye line centred, into a 32 pixel framebuffer. */
        private void capture(Minecraft mc, EntityLivingBase entity) {
            nextCapture = mc.player == null ? 20 : mc.player.ticksExisted + 40;
            boolean pushed = false;
            try {
                head = new Framebuffer(32, 32, true);
                head.setFramebufferColor(0F, 0F, 0F, 0F);
                head.framebufferClear();
                head.bindFramebuffer(true);
                GlStateManager.clearColor(0F, 0F, 0F, 0F);
                GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
                GlStateManager.matrixMode(GL11.GL_PROJECTION);
                GlStateManager.pushMatrix();
                GlStateManager.loadIdentity();
                GlStateManager.ortho(0D, 32D, 32D, 0D, 1000D, 3000D);
                GlStateManager.matrixMode(GL11.GL_MODELVIEW);
                GlStateManager.pushMatrix();
                pushed = true;
                GlStateManager.loadIdentity();
                GlStateManager.translate(0F, 0F, -2000F);
                GlStateManager.color(1F, 1F, 1F, 1F);
                RenderHelper.enableGUIStandardItemLighting();
                int scale = (int) Math.max(16D, Math.min(42D, 22D / Math.max(0.45D, entity.width)));
                int feet = (int) Math.round(16D + entity.getEyeHeight() * scale);
                GuiInventory.drawEntityOnScreen(16, feet, scale, 0F, 0F, entity);
            } catch (Throwable t) {
                dispose();
            } finally {
                RenderHelper.disableStandardItemLighting();
                if (pushed) {
                    GlStateManager.matrixMode(GL11.GL_MODELVIEW);
                    GlStateManager.popMatrix();
                    GlStateManager.matrixMode(GL11.GL_PROJECTION);
                    GlStateManager.popMatrix();
                    GlStateManager.matrixMode(GL11.GL_MODELVIEW);
                }
                mc.getFramebuffer().bindFramebuffer(true);
                GlStateManager.color(1F, 1F, 1F, 1F);
            }
        }

        private static void drawFramebuffer(Framebuffer framebuffer, int x, int y) {
            GlStateManager.enableTexture2D();
            GlStateManager.enableAlpha();
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GlStateManager.color(1F, 1F, 1F, 1F);
            GlStateManager.bindTexture(framebuffer.framebufferTexture);
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
            buffer.pos(x, y + 16, 0D).tex(0D, 0D).endVertex();
            buffer.pos(x + 16, y + 16, 0D).tex(1D, 0D).endVertex();
            buffer.pos(x + 16, y, 0D).tex(1D, 1D).endVertex();
            buffer.pos(x, y, 0D).tex(0D, 1D).endVertex();
            tessellator.draw();
            GlStateManager.disableBlend();
        }

        @Override
        public String getDescription() {
            return "Manage " + entry.name;
        }

        @Override
        public boolean renderCooldown() {
            return false;
        }

        @Override
        public float getCooldownPercentage() {
            return 0F;
        }

        @Override
        public Vec3d getCooldownColor() {
            return new Vec3d(1D, 0.5D, 0D);
        }

        @Override
        public boolean showKey() {
            return true;
        }

        @Override
        public EnumAbilityBarColor getColor() {
            return EnumAbilityBarColor.ORANGE;
        }
    }
}
