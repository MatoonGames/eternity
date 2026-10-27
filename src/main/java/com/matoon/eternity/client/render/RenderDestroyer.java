package com.matoon.eternity.client.render;

import com.matoon.eternity.Eternity;
import com.matoon.eternity.compat.StoneAbilities;
import com.matoon.eternity.entity.ArmorPowers;
import com.matoon.eternity.entity.ArmorSkin;
import com.matoon.eternity.entity.ArmorUpgrade;
import com.matoon.eternity.entity.DestroyerBody;
import com.matoon.eternity.entity.EntityDestroyer;
import com.matoon.eternity.entity.EntityDestroyerArmor;
import com.matoon.eternity.entity.Overcharge;
import com.matoon.eternity.item.ItemAstralRegulator;
import lucraft.mods.heroesexpansion.abilities.AbilityForceField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * The Destroyer as everyone sees it: dark banded metal, and the fire behind its face. Dormant there
 * is none. Awake, it shows through the slits of its face plates; when the plates swing open for
 * the ray, the furnace behind them is too bright to look at. The Destroyer Armor is drawn the same
 * way, except to whoever is inside it, who sees out of its face.
 */
@SideOnly(Side.CLIENT)
public class RenderDestroyer extends RenderLivingBase<EntityLivingBase> {

    private static final ResourceLocation SKIN = new ResourceLocation(Eternity.MODID, "textures/entity/destroyer.png");
    private static final ResourceLocation GLOW = new ResourceLocation(Eternity.MODID, "textures/entity/destroyer_glow.png");
    /** Its wounds at each stage, and the fire showing through them. */
    private static final ResourceLocation[] DAMAGE = new ResourceLocation[3], DAMAGE_GLOW = new ResourceLocation[3];

    static {
        for (int i = 0; i < 3; i++) {
            DAMAGE[i] = new ResourceLocation(Eternity.MODID, "textures/entity/destroyer_damage_" + (i + 1) + ".png");
            DAMAGE_GLOW[i] = new ResourceLocation(Eternity.MODID, "textures/entity/destroyer_damage_glow_" + (i + 1) + ".png");
        }
    }

    /** Drawn whoever is looking, inside it or not: in its systems menu. */
    public static boolean showAlways;

    private float partial;

    /** For its Paint page: drawn in this finish rather than its own, while it is set. */
    public static ArmorSkin preview;

    private static final ResourceLocation[] SKINS = new ResourceLocation[ArmorSkin.values().length];
    private static final ResourceLocation[] GLOWS = new ResourceLocation[ArmorSkin.values().length];

    /** Which finish it is drawn in: the vault's Destroyer always in its own gunmetal. */
    public static ArmorSkin paintOf(EntityLivingBase entity) {
        if (!(entity instanceof EntityDestroyerArmor)) {
            return ArmorSkin.GUNMETAL;
        }
        return preview != null ? preview : ((EntityDestroyerArmor) entity).skin();
    }

    public static ResourceLocation skinTexture(ArmorSkin skin) {
        if (skin == ArmorSkin.GUNMETAL) {
            return SKIN;
        }
        if (SKINS[skin.ordinal()] == null) {
            SKINS[skin.ordinal()] = new ResourceLocation(Eternity.MODID, "textures/entity/destroyer_" + skin.key + ".png");
        }
        return SKINS[skin.ordinal()];
    }

    public static ResourceLocation glowTexture(ArmorSkin skin) {
        if (!skin.glows) {
            return GLOW;
        }
        if (GLOWS[skin.ordinal()] == null) {
            GLOWS[skin.ordinal()] = new ResourceLocation(Eternity.MODID, "textures/entity/destroyer_" + skin.key + "_glow.png");
        }
        return GLOWS[skin.ordinal()];
    }

    public RenderDestroyer(RenderManager manager) {
        super(manager, new ModelDestroyer(), 1.1F);
        addLayer(new Furnace());
        addLayer(new Cradle());
        addLayer(new ForceField());
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityLivingBase entity) {
        return skinTexture(paintOf(entity));
    }

    private ModelDestroyer model() {
        return (ModelDestroyer) mainModel;
    }

    @Override
    public void doRender(EntityLivingBase entity, double x, double y, double z, float entityYaw, float partialTicks) {
        // From inside the armour you see out of its face, not the inside of its head.
        Minecraft mc = Minecraft.getMinecraft();
        if (!showAlways && entity instanceof EntityDestroyerArmor && entity.isPassenger(mc.player) && mc.gameSettings.thirdPersonView == 0
                && mc.getRenderViewEntity() == mc.player) {
            return;
        }
        // Made unseen with its pilot (the Reality Stone), it is not drawn at all, its fire and its stones with it.
        if (!showAlways && entity instanceof EntityDestroyerArmor && entity.isInvisible()) {
            return;
        }
        partial = partialTicks;
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    @Override
    protected boolean canRenderName(EntityLivingBase entity) {
        return entity.hasCustomName() && entity.getAlwaysRenderNameTagForRender() && super.canRenderName(entity);
    }

    @Override
    protected void preRenderCallback(EntityLivingBase entity, float partialTickTime) {
        float s = EntityDestroyer.SCALE * (entity instanceof EntityDestroyerArmor ? ((EntityDestroyerArmor) entity).scale() : 1F);
        if (entity instanceof EntityDestroyerArmor && ((EntityDestroyerArmor) entity).getState() == EntityDestroyerArmor.State.IMPLODE) {
            // Falling in on itself: crushed smaller and smaller, shuddering, narrower faster than it is shorter.
            float p = Math.min(1F, (((EntityDestroyerArmor) entity).clientStateTicks + partialTickTime) / EntityDestroyerArmor.IMPLODE_TICKS);
            float crush = p * p;
            float shake = 0.04F * p;
            GlStateManager.translate((entity.world.rand.nextFloat() - 0.5F) * shake, 1.2F * crush, (entity.world.rand.nextFloat() - 0.5F) * shake);
            GlStateManager.scale(s * (1F - 0.85F * crush), s * (1F - 0.7F * crush), s * (1F - 0.85F * crush));
            return;
        }
        GlStateManager.scale(s, s, s);
    }

    @Override
    protected void applyRotations(EntityLivingBase entity, float ageInTicks, float rotationYaw, float partialTicks) {
        GlStateManager.rotate(180F - rotationYaw, 0F, 1F, 0F);
    }

    /** Over the dark metal: the fire behind its face, through its slits or, open, all of it. */
    @Override
    protected void renderModel(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
                               float headPitch, float scaleFactor) {
        super.renderModel(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor);
        scars(entity, ageInTicks, scaleFactor);
        core(entity, ageInTicks, scaleFactor);
        float heat = ((DestroyerBody) entity).heat(partial);
        if (heat < 0.01F || entity.isInvisible()) {
            return;
        }
        GlSnapshot before = GlSnapshot.take();
        GlStateManager.enableBlend();
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.depthFunc(GL11.GL_LEQUAL);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
        bindTexture(glowTexture(paintOf(entity)));
        float flicker = 0.9F + 0.1F * MathHelper.sin(ageInTicks * 1.7F) * MathHelper.sin(ageInTicks * 0.63F);
        GlStateManager.color(1F, 1F, 1F, Math.min(1F, heat * flicker));
        model().renderPose(scaleFactor);
        GlStateManager.depthMask(true);
        GlStateManager.depthFunc(GL11.GL_LEQUAL);
        before.restore();
    }

    /**
     * Its core running hot with its overcharge: its plate glowing through dull red and orange to white as the core
     * nears its limit, breathing faster as it breaches, and white-hot all over as it falls in on itself.
     */
    private void core(EntityLivingBase entity, float ageInTicks, float scaleFactor) {
        if (!(entity instanceof EntityDestroyerArmor) || entity.isInvisible()) {
            return;
        }
        EntityDestroyerArmor armor = (EntityDestroyerArmor) entity;
        float core = armor.getState() == EntityDestroyerArmor.State.IMPLODE ? 1.15F : armor.core() / Overcharge.CORE_MAX;
        if (core < 0.15F) {
            return;
        }
        float k = Math.min(1.2F, (core - 0.15F) / 0.85F);
        float pulse = 0.8F + 0.2F * MathHelper.sin(ageInTicks * 0.12F);
        // Deep red, brightening toward orange; only falling in does it burn white.
        float hot = Math.min(1F, k), white = Math.max(0F, k - 1F) * 5F;
        float r = 1F, g = Math.min(1F, 0.08F + 0.2F * hot + 0.7F * white), b = Math.min(1F, 0.02F + 0.7F * white);
        GlSnapshot before = GlSnapshot.take();
        GlStateManager.enableBlend();
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.depthFunc(GL11.GL_LEQUAL);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
        // The plate itself heating: first scorched darker and warmer, then its own detail glowing red over that, the edges brightest.
        GlStateManager.disableTexture2D();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.DST_COLOR, GlStateManager.DestFactor.ZERO);
        GlStateManager.color(1F - 0.3F * hot, 1F - 0.6F * hot, 1F - 0.7F * hot, 1F);
        model().renderPose(scaleFactor);
        GlStateManager.enableTexture2D();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        bindTexture(skinTexture(paintOf(entity)));
        GlStateManager.color(r, Math.min(1F, g + 0.15F * hot), b, Math.min(1F, k * pulse));
        model().renderPose(scaleFactor);
        // The fire inside blazing through every seam.
        bindTexture(glowTexture(paintOf(entity)));
        GlStateManager.color(1F, 0.75F + 0.25F * white, 0.45F + 0.55F * white, Math.min(1F, 1.4F * k * pulse));
        model().renderPose(scaleFactor);
        if (k > 0.35F) {
            model().renderPose(scaleFactor);
        }
        // And heat standing off it: shells of glow a little larger than it, like light bleeding past its edges.
        GlStateManager.disableTexture2D();
        GlStateManager.disableCull();
        for (float grow : new float[]{1.03F, 1.08F}) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(0F, 0.6F, 0F);
            GlStateManager.scale(grow, grow, grow);
            GlStateManager.translate(0F, -0.6F, 0F);
            GlStateManager.color(1F, 0.2F + 0.2F * hot + 0.6F * white, 0.04F + 0.6F * white, (grow < 1.05F ? 0.3F : 0.14F) * k * pulse);
            model().renderPose(scaleFactor);
            GlStateManager.popMatrix();
        }
        GlStateManager.enableTexture2D();
        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.depthMask(true);
        before.restore();
    }

    /** How badly it is scarred: none, then three stages, as its integrity falls below a half, a quarter and a tenth. */
    public static int scarred(EntityLivingBase entity) {
        float f = entity.getHealth() / entity.getMaxHealth();
        return f >= 0.5F ? 0 : f >= 0.25F ? 1 : f >= 0.1F ? 2 : 3;
    }

    public static ResourceLocation scarTexture(int stage, boolean glow) {
        return glow ? DAMAGE_GLOW[stage - 1] : DAMAGE[stage - 1];
    }

    /**
     * Its wounds, worse as it is beaten down: dents and bright gouges, then soot and cracks, then plate burst open.
     * Laid over whatever finish it wears, and the fire inside showing through the deepest of them, breathing with it.
     */
    private void scars(EntityLivingBase entity, float ageInTicks, float scaleFactor) {
        int stage = scarred(entity);
        if (stage == 0 || entity.isInvisible() || entity instanceof EntityDestroyer && !((EntityDestroyer) entity).isAwake()) {
            return;
        }
        GlSnapshot before = GlSnapshot.take();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.depthFunc(GL11.GL_LEQUAL);
        GlStateManager.depthMask(false);
        GlStateManager.color(1F, 1F, 1F, 1F);
        bindTexture(DAMAGE[stage - 1]);
        model().renderPose(scaleFactor);
        if (stage > 1) {
            GlStateManager.disableLighting();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
            float breath = 0.65F + 0.35F * MathHelper.sin(ageInTicks * (stage == 3 ? 0.35F : 0.18F)) * MathHelper.sin(ageInTicks * 0.07F + 1F);
            GlStateManager.color(1F, 1F, 1F, MathHelper.clamp(breath, 0.3F, 1F));
            bindTexture(DAMAGE_GLOW[stage - 1]);
            model().renderPose(scaleFactor);
        }
        GlStateManager.depthMask(true);
        before.restore();
    }

    /** The light of the furnace standing out in front of its face. */
    private class Furnace implements LayerRenderer<EntityLivingBase> {
        @Override
        public void doRenderLayer(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
                                  float netHeadYaw, float headPitch, float scale) {
            float heat = ((DestroyerBody) entity).heat(partialTicks);
            float open = ((DestroyerBody) entity).faceOpen(partialTicks);
            if (heat < 0.05F) {
                return;
            }
            GlSnapshot before = GlSnapshot.take();
            GlStateManager.disableTexture2D();
            GlStateManager.disableLighting();
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            GlStateManager.depthMask(false);
            GlStateManager.disableCull();
            GlStateManager.shadeModel(GL11.GL_SMOOTH);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
            GlStateManager.pushMatrix();
            model().toFace(scale);
            float flicker = 0.85F + 0.15F * MathHelper.sin(ageInTicks * 2.3F);
            float bright = heat * (0.25F + 0.75F * open);
            disc(0.12F + 0.45F * bright * flicker, VaultFx.ORANGE, 0.35F * heat);
            disc(0.08F + 0.25F * bright * flicker, VaultFx.YELLOW, 0.5F * bright);
            disc(0.05F + 0.12F * bright, VaultFx.WHITE, 0.8F * bright);
            GlStateManager.popMatrix();
            before.restore();
        }

        /** A soft disc of light facing out of its face, and two across it, so it glows from the side as well. */
        private void disc(float radius, float[] col, float alpha) {
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            for (int axis = 0; axis < 3; axis++) {
                buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
                buffer.pos(0D, 0D, 0D).color(col[0], col[1], col[2], alpha).endVertex();
                for (int i = 0; i <= 18; i++) {
                    double a = Math.PI * 2D * i / 18D;
                    double u = Math.cos(a) * radius, v = Math.sin(a) * radius;
                    if (axis == 0) {
                        buffer.pos(u, v, 0D).color(col[0], col[1], col[2], 0F).endVertex();
                    } else if (axis == 1) {
                        buffer.pos(u, 0D, v).color(col[0], col[1], col[2], 0F).endVertex();
                    } else {
                        buffer.pos(0D, u, v).color(col[0], col[1], col[2], 0F).endVertex();
                    }
                }
                tessellator.draw();
            }
        }

        @Override
        public boolean shouldCombineTextures() {
            return false;
        }
    }

    /**
     * The cavity in its chest, once its cradle is forged: the Astral Regulator hanging in it, turning, and the arch of
     * six sockets over its top, ringed in old gold, each Infinity Stone set there burning in its own colour.
     */
    private class Cradle implements LayerRenderer<EntityLivingBase> {
        @Override
        public void doRenderLayer(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
                                  float netHeadYaw, float headPitch, float scale) {
            if (!(entity instanceof EntityDestroyerArmor) || !((EntityDestroyerArmor) entity).has(ArmorUpgrade.CRADLE)) {
                return;
            }
            EntityDestroyerArmor armor = (EntityDestroyerArmor) entity;
            GlSnapshot before = GlSnapshot.take();
            GlStateManager.pushMatrix();
            model().toChest(scale);
            GlStateManager.scale(scale, scale, scale);
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            // The Regulator, turning slowly in the cavity, lit by nothing but itself.
            ItemStack regulator = armor.regulator();
            if (!regulator.isEmpty()) {
                GlStateManager.pushMatrix();
                GlStateManager.translate(0F, CAVITY_Y, -3.7F);
                GlStateManager.rotate(ageInTicks * 1.5F, 0F, 1F, 0F);
                GlStateManager.rotate(12F, 1F, 0F, 0F);
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
                boolean tribunal = ItemAstralRegulator.absorbed(regulator).contains(ItemAstralRegulator.TRIBUNAL);
                OrbRenderer.draw(2.3F, ageInTicks, ItemAstralRegulator.isCharged(regulator) ? 1F : 0F, true, tribunal);
                GlStateManager.popMatrix();
            }
            GlStateManager.disableTexture2D();
            // The sockets, lit like the rest of it.
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            for (int i = 0; i < ArmorPowers.COUNT; i++) {
                float[] at = socket(i);
                box(buffer, at[0] - 0.85F, at[1] - 0.85F, SOCKET_Z, at[0] + 0.85F, at[1] + 0.85F, SOCKET_Z + 0.8F, 0.42F, 0.31F, 0.14F, 1F);
                box(buffer, at[0] - 0.55F, at[1] - 0.55F, SOCKET_Z - 0.08F, at[0] + 0.55F, at[1] + 0.55F, SOCKET_Z + 0.05F, 0.05F, 0.04F, 0.04F, 1F);
                // The gold band that joins each to the next along the arch.
                if (i < ArmorPowers.COUNT - 1) {
                    float[] next = socket(i + 1);
                    for (int k = 1; k < 4; k++) {
                        float bx = at[0] + (next[0] - at[0]) * k / 4F, by = at[1] + (next[1] - at[1]) * k / 4F;
                        box(buffer, bx - 0.3F, by - 0.3F, SOCKET_Z + 0.15F, bx + 0.3F, by + 0.3F, SOCKET_Z + 0.75F, 0.55F, 0.4F, 0.18F, 1F);
                    }
                }
            }
            tessellator.draw();
            // The stones, burning.
            GlStateManager.disableLighting();
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            for (int i = 0; i < ArmorPowers.COUNT; i++) {
                if (!armor.holdsStone(i)) {
                    continue;
                }
                float[] at = socket(i);
                float[] c = rgb(ArmorPowers.RGB[i]);
                box(buffer, at[0] - 0.48F, at[1] - 0.48F, SOCKET_Z - 0.5F, at[0] + 0.48F, at[1] + 0.48F, SOCKET_Z, c[0], c[1], c[2], 1F);
            }
            tessellator.draw();
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            GlStateManager.depthMask(false);
            GlStateManager.shadeModel(GL11.GL_SMOOTH);
            // The model is drawn mirrored, which turns these flat glows away from anyone looking at its chest: draw both sides.
            GlStateManager.disableCull();
            for (int i = 0; i < ArmorPowers.COUNT; i++) {
                if (!armor.holdsStone(i)) {
                    continue;
                }
                float[] at = socket(i);
                float[] c = rgb(ArmorPowers.RGB[i]);
                float pulse = 0.7F + 0.25F * MathHelper.sin(ageInTicks * 0.15F + i * 1.1F);
                fan(buffer, at[0], at[1], SOCKET_Z - 0.6F, 2.4F, c, pulse);
                fan(buffer, at[0], at[1], SOCKET_Z - 0.65F, 1F, new float[]{1F, 1F, 1F}, pulse * 0.5F);
            }
            // The cavity glowing with whatever hangs in it.
            if (!regulator.isEmpty()) {
                float pulse = 0.35F + 0.15F * MathHelper.sin(ageInTicks * 0.1F);
                fan(buffer, 0F, CAVITY_Y, -1.1F, 3.2F, new float[]{0.8F, 0.6F, 1F}, pulse);
            }
            GlStateManager.popMatrix();
            before.restore();
        }

        /** A soft disc of light facing out of its chest. */
        private void fan(BufferBuilder buffer, float x, float y, float z, float radius, float[] c, float alpha) {
            Tessellator tessellator = Tessellator.getInstance();
            buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
            buffer.pos(x, y, z).color(c[0], c[1], c[2], alpha).endVertex();
            for (int k = 0; k <= 16; k++) {
                double a = Math.PI * 2D * k / 16D;
                buffer.pos(x + Math.cos(a) * radius, y + Math.sin(a) * radius, z).color(c[0], c[1], c[2], 0F).endVertex();
            }
            tessellator.draw();
        }

        private float[] rgb(int color) {
            return new float[]{(color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F, (color & 255) / 255F};
        }

        private void box(BufferBuilder b, float x0, float y0, float z0, float x1, float y1, float z1, float r, float g, float bl, float a) {
            float dark = 0.7F;
            // Front (toward -z), back, then the four sides, the sides a little darker.
            quad(b, x0, y1, z0, x1, y1, z0, x1, y0, z0, x0, y0, z0, r, g, bl, a);
            quad(b, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, r * dark, g * dark, bl * dark, a);
            quad(b, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, r * dark, g * dark, bl * dark, a);
            quad(b, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, r * dark, g * dark, bl * dark, a);
            quad(b, x0, y0, z1, x0, y0, z0, x0, y1, z0, x0, y1, z1, r * dark, g * dark, bl * dark, a);
            quad(b, x1, y1, z1, x1, y1, z0, x1, y0, z0, x1, y0, z1, r * dark, g * dark, bl * dark, a);
        }

        private void quad(BufferBuilder b, float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2,
                          float x3, float y3, float z3, float r, float g, float bl, float a) {
            b.pos(x0, y0, z0).color(r, g, bl, a).endVertex();
            b.pos(x1, y1, z1).color(r, g, bl, a).endVertex();
            b.pos(x2, y2, z2).color(r, g, bl, a).endVertex();
            b.pos(x3, y3, z3).color(r, g, bl, a).endVertex();
        }

        @Override
        public boolean shouldCombineTextures() {
            return false;
        }
    }

    /** The middle of the cavity in its chest, and how far forward the sockets over it stand, in its chest's own pixels. */
    private static final float CAVITY_Y = -5F, SOCKET_Z = -6.6F, ARCH = 4.6F;

    /** Where a stone's socket sits on the arch over the cavity, left to right across its chest. */
    private static float[] socket(int stone) {
        double a = Math.toRadians(165D - stone * 30D);
        // Its right is -x, which from in front is on the left: the same order as on its Cradle page.
        return new float[]{(float) (Math.cos(a) * ARCH), (float) (CAVITY_Y - Math.sin(a) * ARCH)};
    }

    /**
     * Its pilot's force field (the Space Stone's), standing around the whole of it as it would around them: its own
     * shape a little larger, in the field's colour, faint until something strikes it and it flares.
     */
    private class ForceField implements LayerRenderer<EntityLivingBase> {
        @Override
        public void doRenderLayer(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
                                  float netHeadYaw, float headPitch, float scale) {
            if (!(entity instanceof EntityDestroyerArmor) || !((EntityDestroyerArmor) entity).hidesPilot()) {
                return;
            }
            AbilityForceField field = StoneAbilities.shield(((EntityDestroyerArmor) entity).pilot());
            if (field == null) {
                return;
            }
            java.awt.Color color = field.getDataManager().get(AbilityForceField.COLOR);
            Float flare = field.getDataManager().get(AbilityForceField.OPACITY);
            float opacity = MathHelper.clamp(MathHelper.sin(ageInTicks / 10F) * 0.1F + 0.1F + (flare == null ? 0F : flare), 0.11F, 1F);
            GlSnapshot before = GlSnapshot.take();
            GlStateManager.pushMatrix();
            GlStateManager.disableTexture2D();
            GlStateManager.disableLighting();
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            GlStateManager.depthMask(false);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
            float r = color == null ? 0.01F : color.getRed() / 255F, g = color == null ? 0.33F : color.getGreen() / 255F, b = color == null ? 1F : color.getBlue() / 255F;
            // Two shells: one close over its plate, one standing a little off it.
            for (float grow : new float[]{1.03F, 1.09F}) {
                GlStateManager.pushMatrix();
                GlStateManager.translate(0F, 0.6F, 0F);
                GlStateManager.scale(grow, grow, grow);
                GlStateManager.translate(0F, -0.6F, 0F);
                GlStateManager.color(r, g, b, grow < 1.05F ? opacity : opacity * 0.6F);
                model().renderPose(scale);
                GlStateManager.popMatrix();
            }
            GlStateManager.color(1F, 1F, 1F, 1F);
            GlStateManager.popMatrix();
            before.restore();
        }

        @Override
        public boolean shouldCombineTextures() {
            return false;
        }
    }
}
