package com.matoon.eternity.client.render;

import com.matoon.eternity.Eternity;
import com.matoon.eternity.entity.EntityDraugr;
import com.matoon.eternity.entity.EntityNorse;
import com.matoon.eternity.entity.NorseBody;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityCreature;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The people of the first chapter, and the dead under the mound, each in their own look. The
 * dead's eyes give a little light; so does nothing about her.
 */
@SideOnly(Side.CLIENT)
public class RenderNorse extends RenderLiving<EntityCreature> {

    /** Set while one is drawn in a portrait, not the world. */
    public static boolean portrait;

    private static final String[] NAMES = {"stranger", "death", "ragna", "hrafn", "draugr", "gardener", "ayesha", "magus", "spawn"};
    private static final ResourceLocation[] SKINS = new ResourceLocation[NAMES.length];
    private static final ResourceLocation DRAUGR_GLOW = new ResourceLocation(Eternity.MODID, "textures/entity/norse_draugr_glow.png");
    private static final ResourceLocation GARDENER_GLOW = new ResourceLocation(Eternity.MODID, "textures/entity/norse_gardener_glow.png");
    private static final ResourceLocation MAGUS_GLOW = new ResourceLocation(Eternity.MODID, "textures/entity/norse_magus_glow.png");
    private static final ResourceLocation SPAWN_GLOW = new ResourceLocation(Eternity.MODID, "textures/entity/norse_spawn_glow.png");

    static {
        for (int i = 0; i < NAMES.length; i++) {
            SKINS[i] = new ResourceLocation(Eternity.MODID, "textures/entity/norse_" + NAMES[i] + ".png");
        }
    }

    private final ModelNorse[] models = new ModelNorse[NAMES.length];

    public RenderNorse(RenderManager manager) {
        super(manager, new ModelNorse(0), 0.4F);
        for (int i = 0; i < models.length; i++) {
            models[i] = new ModelNorse(i);
        }
    }

    private static int look(EntityCreature e) {
        int look = e instanceof NorseBody.Figure ? ((NorseBody.Figure) e).look() : 0;
        return look >= 0 && look < NAMES.length ? look : 0;
    }

    @Override
    public void doRender(EntityCreature entity, double x, double y, double z, float entityYaw, float partialTicks) {
        mainModel = models[look(entity)];
        ModelNorse.portraitPose = portrait;
        try {
            super.doRender(entity, x, y, z, entityYaw, partialTicks);
        } finally {
            ModelNorse.portraitPose = false;
        }
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityCreature entity) {
        return SKINS[look(entity)];
    }

    @Override
    protected void preRenderCallback(EntityCreature entity, float partialTickTime) {
        int look = look(entity);
        // The Gardener stands a head over anyone in the north; the Sovereign taller still, and what came out of their vats any size at all.
        float s = look == NorseBoxes.GARDENER || look == NorseBoxes.AYESHA ? 1.08F : look == NorseBoxes.MAGUS ? 1.12F
                : look == NorseBoxes.SPAWN ? 0.9F + (entity.getEntityId() * 7 % 5) * 0.08F
                : look == NorseBoxes.HRAFN || entity instanceof EntityDraugr ? 0.97F : 0.92F;
        GlStateManager.scale(s, s, s);
    }

    @Override
    protected void applyRotations(EntityCreature entity, float ageInTicks, float rotationYaw, float partialTicks) {
        GlStateManager.rotate(180F - rotationYaw, 0F, 1F, 0F);
    }

    @Override
    protected int getColorMultiplier(EntityCreature entity, float lightBrightness, float partialTickTime) {
        return 0;
    }

    @Override
    protected boolean canRenderName(EntityCreature entity) {
        return false;
    }

    @Override
    protected void renderModel(EntityCreature entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
                               float headPitch, float scaleFactor) {
        super.renderModel(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor);
        if (entity.isInvisible()) {
            return;
        }
        float ash = entity instanceof EntityNorse ? ((EntityNorse) entity).ash() : 0F;
        if (ash > 0.01F) {
            // The life gone out of her: the same body under a skin of ash, every colour in it dulled to grey.
            GlStateManager.disableTexture2D();
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            GlStateManager.depthFunc(org.lwjgl.opengl.GL11.GL_LEQUAL);
            GlStateManager.disableLighting();
            GlStateManager.color(0.6F, 0.58F, 0.6F, 0.72F * ash);
            mainModel.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor);
            GlStateManager.color(1F, 1F, 1F, 1F);
            GlStateManager.disableBlend();
            GlStateManager.enableLighting();
            GlStateManager.enableTexture2D();
        }
        float lit;
        ResourceLocation glow;
        if (entity instanceof EntityDraugr) {
            // The dead's eyes: a pale light that gutters, brighter once they are up.
            EntityDraugr d = (EntityDraugr) entity;
            lit = d.isLaidDown() ? 0.25F : 0.95F;
            lit *= 0.8F + 0.2F * MathHelper.sin(ageInTicks * 0.9F + entity.getEntityId()) * MathHelper.sin(ageInTicks * 0.13F);
            glow = d.look() == NorseBoxes.SPAWN ? SPAWN_GLOW : DRAUGR_GLOW;
        } else if (look(entity) == NorseBoxes.MAGUS) {
            // The gem in his brow, and his eyes: steady, never once flickering.
            lit = 0.85F + 0.15F * MathHelper.sin(ageInTicks * 0.05F);
            glow = MAGUS_GLOW;
        } else if (entity instanceof EntityNorse && look(entity) == NorseBoxes.GARDENER && ((EntityNorse) entity).isShining()) {
            // The stone in his chest, beating slowly, like a heart or a clock; the roots out from it lit with each beat.
            float beat = (ageInTicks % 30F) / 30F;
            lit = 0.55F + 0.45F * (float) Math.exp(-beat * 6F);
            glow = GARDENER_GLOW;
        } else {
            return;
        }
        GlStateManager.enableBlend();
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        float lx = OpenGlHelper.lastBrightnessX, ly = OpenGlHelper.lastBrightnessY;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
        bindTexture(glow);
        GlStateManager.color(1F, 1F, 1F, lit);
        mainModel.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor);
        GlStateManager.color(1F, 1F, 1F, 1F);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lx, ly);
        GlStateManager.depthMask(true);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
    }
}
