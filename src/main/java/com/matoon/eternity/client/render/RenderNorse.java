package com.matoon.eternity.client.render;

import com.matoon.eternity.Eternity;
import com.matoon.eternity.entity.EntityDraugr;
import com.matoon.eternity.entity.EntityNorse;
import com.matoon.eternity.entity.Morph;
import com.matoon.eternity.entity.NorseBody;
import com.matoon.eternity.entity.NorsePose;
import com.matoon.eternity.entity.Rot;
import com.matoon.eternity.entity.SkinTear;
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

    private static final String[] NAMES = {"stranger", "death", "ragna", "hrafn", "draugr", "gardener", "ayesha", "magus", "spawn", "temptress", "unveiled"};
    private static final ResourceLocation[] SKINS = new ResourceLocation[NAMES.length];
    private static final ResourceLocation DRAUGR_GLOW = new ResourceLocation(Eternity.MODID, "textures/entity/norse_draugr_glow.png");
    private static final ResourceLocation GARDENER_GLOW = new ResourceLocation(Eternity.MODID, "textures/entity/norse_gardener_glow.png");
    private static final ResourceLocation MAGUS_GLOW = new ResourceLocation(Eternity.MODID, "textures/entity/norse_magus_glow.png");
    private static final ResourceLocation SPAWN_GLOW = new ResourceLocation(Eternity.MODID, "textures/entity/norse_spawn_glow.png");
    private static final ResourceLocation UNVEILED_GLOW = new ResourceLocation(Eternity.MODID, "textures/entity/norse_unveiled_glow.png");
    /** Hers again with her skull bare, bone all the way round: for until her hood has come up over it. */
    private static final ResourceLocation UNVEILED_BARE = new ResourceLocation(Eternity.MODID, "textures/entity/norse_unveiled_bare.png");
    /** The pretty one, rotting on her: fresh, then paler, veined, grey-green and split, coming apart, and worse. */
    private static final ResourceLocation[] ROTS = new ResourceLocation[Rot.MAX + 1];
    /** What is under it, where it splits: each texel's alpha says how soon it opens. */
    private static final ResourceLocation SPLITS = new ResourceLocation(Eternity.MODID, "textures/entity/norse_temptress_splits.png");

    static {
        for (int i = 0; i < NAMES.length; i++) {
            SKINS[i] = new ResourceLocation(Eternity.MODID, "textures/entity/norse_" + NAMES[i] + ".png");
        }
        ROTS[0] = SKINS[NorseBoxes.TEMPTRESS];
        for (int i = 1; i < ROTS.length; i++) {
            ROTS[i] = new ResourceLocation(Eternity.MODID, "textures/entity/norse_temptress_rot" + i + ".png");
        }
    }

    private final ModelNorse[] models = new ModelNorse[NAMES.length];
    private float partial;

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

    /** How big each of them stands. */
    private static float size(int look, EntityCreature entity) {
        // The Gardener stands a head over anyone in the north; the Sovereign taller still, and what came out of their vats any size at all.
        return look == NorseBoxes.GARDENER || look == NorseBoxes.AYESHA ? 1.08F : look == NorseBoxes.MAGUS ? 1.12F
                : look == NorseBoxes.UNVEILED ? 0.93F : look == NorseBoxes.TEMPTRESS ? 0.88F
                : look == NorseBoxes.SPAWN ? 0.9F + (entity.getEntityId() * 7 % 5) * 0.08F
                : look == NorseBoxes.HRAFN || entity instanceof EntityDraugr ? 0.97F : 0.92F;
    }

    /** Her skin in one of her shapes: the pretty one as rotten as it is on her. */
    private static ResourceLocation skin(int look, int rot) {
        return look == NorseBoxes.TEMPTRESS ? ROTS[Math.max(0, Math.min(Rot.MAX, rot))] : SKINS[look];
    }

    /** Whether she is changing shape, just now (and not tearing the pretty one off, which is its own thing). */
    private static boolean morphing(EntityCreature e) {
        if (portrait || !(e instanceof EntityNorse)) {
            return false;
        }
        EntityNorse n = (EntityNorse) e;
        return n.shiftFrom >= 0 && n.shiftFrom < NAMES.length && n.shiftFrom != look(e) && n.morphTicks(0F) >= 0F && n.pose() != NorsePose.TEAR;
    }

    /**
     * Changing shape, a piece at a time (see {@link com.matoon.eternity.entity.Morph}): the old
     * shape drawn with every piece that hasn't burst yet, swelling, knotting and bruising as what
     * is inside it moves; the new one drawn with every piece that has, raw and red and too small
     * at first, growing too far and settling, the colour coming back into it.
     */
    private void morph(EntityNorse e, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        float t = e.morphTicks(partial);
        int from = e.shiftFrom, to = look(e);
        ModelNorse old = models[from], now = models[to];
        int seed = e.getEntityId();
        old.each(p -> {
            float u = Morph.u(Morph.region(p.boxName), t);
            p.peeled = u >= Morph.BURST;
            if (SkinTear.cloth(p.boxName)) {
                // Cloth doesn't swell and burst: it rolls back up into the dark.
                p.grow = 1F - Math.min(1F, u / Morph.BURST);
                p.r = p.g = p.b = 0.15F + 0.85F * p.grow;
                return;
            }
            if (u > 0F && !p.peeled) {
                float a = u / Morph.BURST;
                a *= a;
                float h = (p.boxName.hashCode() + seed) * 0.37F;
                p.sx = 1F + a * (0.45F + 0.3F * MathHelper.sin(ageInTicks * 1.9F + h));
                p.sy = 1F + a * (0.28F + 0.15F * MathHelper.sin(ageInTicks * 1.3F + h * 2F));
                p.sz = 1F + a * (0.45F + 0.3F * MathHelper.sin(ageInTicks * 2.3F + h * 3F));
                // Bruising, darker and darker, purple, as it fills.
                p.r = 1F - 0.35F * a;
                p.g = 1F - 0.65F * a;
                p.b = 1F - 0.45F * a;
            }
        });
        now.each(p -> {
            float u = Morph.u(Morph.region(p.boxName), t);
            p.peeled = u < Morph.BURST;
            if (SkinTear.cloth(p.boxName)) {
                // Nor does it grow raw: it unrolls out of the dark, top down.
                float x = Math.max(0F, (u - Morph.BURST) / (1F - Morph.BURST));
                p.grow = x * x * (3F - 2F * x);
                p.r = p.g = p.b = 0.15F + 0.85F * p.grow;
                return;
            }
            if (!p.peeled && u < 1F) {
                float x = (u - Morph.BURST) / (1F - Morph.BURST);
                float h = (p.boxName.hashCode() + seed) * 0.53F;
                float grow = 0.45F + 0.55F * ModelAdamWarlock.outBack(Math.min(1F, x * 1.5F));
                float lump = 0.2F * (1F - x);
                p.sx = grow + lump * MathHelper.sin(ageInTicks * 1.7F + h);
                p.sy = grow + lump * MathHelper.sin(ageInTicks * 1.1F + h * 2F);
                p.sz = grow + lump * MathHelper.sin(ageInTicks * 2.1F + h * 3F);
                // Skinless, wet and red, the colour of her own coming back into it.
                float raw = (float) Math.pow(1F - x, 0.6F);
                p.r = 1F - 0.05F * raw;
                p.g = 1F - 0.8F * raw;
                p.b = 1F - 0.75F * raw;
            }
        });
        old.setLivingAnimations(e, limbSwing, limbSwingAmount, partial);
        float k = size(from, e) / size(to, e);
        bindTexture(skin(from, e.rot()));
        GlStateManager.pushMatrix();
        GlStateManager.translate(0F, 1.501F, 0F);
        GlStateManager.scale(k, k, k);
        GlStateManager.translate(0F, -1.501F, 0F);
        old.render(e, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        GlStateManager.popMatrix();
        // Her skull bare until her hood has come half up over it.
        float hood = Math.max(0F, (Morph.u(Morph.HEAD, t) - Morph.BURST) / (1F - Morph.BURST));
        bindTexture(to == NorseBoxes.UNVEILED && hood < 0.5F ? UNVEILED_BARE : skin(to, e.rot()));
        now.render(e, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        if (to == NorseBoxes.UNVEILED && Morph.u(Morph.HEAD, t) >= Morph.BURST) {
            // Her eyes, lit, as soon as her head has come through.
            now.each(p -> p.r = p.g = p.b = 1F);
            glow(now, e, UNVEILED_GLOW, 0.7F, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        }
        old.plain();
        now.plain();
    }

    @Override
    public void doRender(EntityCreature entity, double x, double y, double z, float entityYaw, float partialTicks) {
        mainModel = models[look(entity)];
        partial = partialTicks;
        ModelNorse.portraitPose = portrait;
        try {
            super.doRender(entity, x, y, z, entityYaw, partialTicks);
        } finally {
            ModelNorse.portraitPose = false;
        }
        if (entity instanceof EntityNorse && !portrait && !((EntityNorse) entity).scraps.isEmpty()) {
            scraps((EntityNorse) entity, x, y, z, partialTicks);
        }
    }

    /** Whether she is tearing the pretty one off, just now. */
    private static boolean tearing(EntityCreature e) {
        return !portrait && e instanceof EntityNorse && ((EntityNorse) e).pose() == NorsePose.TEAR && look(e) == NorseBoxes.TEMPTRESS;
    }

    /**
     * Tearing it off: what she is, inside, drawn first and a little smaller, so that it fits; then
     * the skin over it, with every piece she has torn away gone; then, where it has split, the raw
     * red under it, opening along the seams as the alpha test comes down.
     */
    private void tear(EntityNorse e, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        float t = e.body().stateTicks + partial;
        ModelNorse suit = models[NorseBoxes.TEMPTRESS], inner = models[NorseBoxes.UNVEILED];
        float k = SkinTear.inner(t) / 0.88F;
        // Each part of her shows only once the skin over it is off: her skull with her face,
        // the collar at her throat with the body, her skirts with the legs.
        boolean headOut = SkinTear.gone("head", t);
        inner.peel(p -> !SkinTear.gone(SkinTear.under(p), t));
        // Her hood, her cape and her ribbons come only once she is out of it: out of the dark, unrolling down over her.
        float dressed = SkinTear.dressed(t);
        inner.each(p -> {
            if (SkinTear.cloth(p.boxName)) {
                p.peeled = dressed <= 0F;
                p.grow = dressed;
                p.r = p.g = p.b = 0.15F + 0.85F * dressed;
            }
        });
        inner.setLivingAnimations(e, limbSwing, limbSwingAmount, partial);
        bindTexture(dressed < 0.5F ? UNVEILED_BARE : SKINS[NorseBoxes.UNVEILED]);
        GlStateManager.pushMatrix();
        GlStateManager.translate(0F, 1.501F, 0F);
        GlStateManager.scale(k, k, k);
        GlStateManager.translate(0F, -1.501F, 0F);
        inner.render(e, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        if (headOut) {
            // Her eyes, as soon as her face is bare: violet, and lit.
            glow(inner, e, UNVEILED_GLOW, 0.4F + 0.6F * Math.min(1F, (t - SkinTear.AT[2]) / 30F), limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        }
        GlStateManager.popMatrix();
        inner.plain();
        suit.peel(p -> SkinTear.gone(p, t));
        bindTexture(ROTS[Rot.MAX]);
        suit.render(e, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        bindTexture(SPLITS);
        GlStateManager.alphaFunc(org.lwjgl.opengl.GL11.GL_GREATER, Math.max(0.02F, 1F - SkinTear.split(t)));
        suit.render(e, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        GlStateManager.alphaFunc(org.lwjgl.opengl.GL11.GL_GREATER, 0.1F);
        suit.unpeel();
    }

    /** The pieces of skin she tore off, wherever they flew to, turning as they go and lying where they land. */
    private void scraps(EntityNorse e, double x, double y, double z, float partialTicks) {
        double ex = e.prevPosX + (e.posX - e.prevPosX) * partialTicks, ey = e.prevPosY + (e.posY - e.prevPosY) * partialTicks,
                ez = e.prevPosZ + (e.posZ - e.prevPosZ) * partialTicks;
        GlStateManager.enableRescaleNormal();
        GlStateManager.disableCull();
        for (EntityNorse.Scrap s : e.scraps) {
            int look = s.look >= 0 && s.look < NAMES.length ? s.look : NorseBoxes.TEMPTRESS;
            Peel part = models[look].named(s.part);
            if (part == null) {
                continue;
            }
            double sx = s.prevX + (s.x - s.prevX) * partialTicks, sy = s.prevY + (s.y - s.prevY) * partialTicks, sz = s.prevZ + (s.z - s.prevZ) * partialTicks;
            bindTexture(skin(look, s.rot));
            GlStateManager.pushMatrix();
            GlStateManager.translate(x + sx - ex, y + sy - ey, z + sz - ez);
            GlStateManager.rotate(180F - s.yaw, 0F, 1F, 0F);
            GlStateManager.rotate(s.prevPitch + (s.pitch - s.prevPitch) * partialTicks, 1F, 0F, 0F);
            GlStateManager.rotate(s.prevRoll + (s.roll - s.prevRoll) * partialTicks, 0F, 0F, 1F);
            float size = size(look, e);
            GlStateManager.scale(-size, -size, size);
            part.renderOwn(0.0625F);
            GlStateManager.popMatrix();
        }
        GlStateManager.enableCull();
        GlStateManager.disableRescaleNormal();
    }

    /** A model's glowing parts over it, at full brightness, as bright as lit. */
    private void glow(ModelNorse model, EntityCreature entity, ResourceLocation glow, float lit, float limbSwing, float limbSwingAmount, float ageInTicks,
                      float netHeadYaw, float headPitch, float scale) {
        GlStateManager.enableBlend();
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        float lx = OpenGlHelper.lastBrightnessX, ly = OpenGlHelper.lastBrightnessY;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
        bindTexture(glow);
        GlStateManager.color(1F, 1F, 1F, lit);
        model.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        GlStateManager.color(1F, 1F, 1F, 1F);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lx, ly);
        GlStateManager.depthMask(true);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityCreature entity) {
        int look = look(entity);
        if (look == NorseBoxes.TEMPTRESS && entity instanceof EntityNorse && !portrait) {
            return ROTS[((EntityNorse) entity).rot()];
        }
        return SKINS[look];
    }

    @Override
    protected void preRenderCallback(EntityCreature entity, float partialTickTime) {
        float s = size(look(entity), entity);
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
        if (tearing(entity)) {
            if (!entity.isInvisible()) {
                tear((EntityNorse) entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor);
            }
            return;
        }
        if (morphing(entity)) {
            if (!entity.isInvisible()) {
                morph((EntityNorse) entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor);
            }
            return;
        }
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
        } else if (look(entity) == NorseBoxes.UNVEILED) {
            // Her eyes, violet, and the red stone at her throat: they catch light that isn't there.
            lit = 0.7F + 0.3F * MathHelper.sin(ageInTicks * 0.07F);
            glow = UNVEILED_GLOW;
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
        glow((ModelNorse) mainModel, entity, glow, lit, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor);
    }
}
