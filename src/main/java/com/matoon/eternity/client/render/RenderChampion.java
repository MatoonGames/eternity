package com.matoon.eternity.client.render;

import com.matoon.eternity.Eternity;
import com.matoon.eternity.entity.EntityChampion;
import com.matoon.eternity.entity.EntityChampion.State;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * The Champion as everyone sees him. Calm, he is a pale blue-grey giant with a red mane, gold
 * at his collar, wrists and waist, and the Power Stone glinting in his brow. As his rage climbs
 * the stone burns brighter, his eyes light, the veins stand out purple on his arms and neck, and
 * at the top of it a crackling aura of the stone's light stands out around him. Flying through
 * the air he leans into it and leaves purple afterimages behind; adrift, he turns end over end.
 */
@SideOnly(Side.CLIENT)
public class RenderChampion extends RenderLiving<EntityChampion> {

    private static final ResourceLocation SKIN = new ResourceLocation(Eternity.MODID, "textures/entity/champion.png");
    private static final ResourceLocation RAGE = new ResourceLocation(Eternity.MODID, "textures/entity/champion_rage.png");
    private static final ResourceLocation STONE = new ResourceLocation(Eternity.MODID, "textures/entity/champion_stone.png");

    /** How much bigger than the model he is drawn. */
    static final float SCALE = 1.6F;
    /** His middle, in blocks above his feet: where he turns about when he leans, flies or tumbles. */
    private static final float CENTRE = 1.6F;

    private float partial;

    public RenderChampion(RenderManager manager) {
        super(manager, new ModelChampion(), 1.0F);
        addLayer(new Power());
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityChampion entity) {
        return SKIN;
    }

    private ModelChampion model() {
        return (ModelChampion) mainModel;
    }

    @Override
    protected void preRenderCallback(EntityChampion entity, float partialTickTime) {
        GlStateManager.scale(SCALE, SCALE, SCALE);
    }

    /** Leaning into his flight, and adrift, turning end over end, all about his middle. */
    @Override
    protected void applyRotations(EntityChampion entity, float ageInTicks, float rotationYaw, float partialTicks) {
        GlStateManager.rotate(180F - rotationYaw, 0F, 1F, 0F);
        float pitch = entity.prevBodyPitch + (entity.bodyPitch - entity.prevBodyPitch) * partialTicks;
        float roll = entity.prevBodyRoll + (entity.bodyRoll - entity.prevBodyRoll) * partialTicks;
        GlStateManager.translate(0F, CENTRE, 0F);
        GlStateManager.rotate(-pitch, 1F, 0F, 0F);
        GlStateManager.rotate(roll, 0F, 0F, 1F);
        GlStateManager.translate(0F, -CENTRE, 0F);
    }

    @Override
    public void doRender(EntityChampion entity, double x, double y, double z, float entityYaw, float partialTicks) {
        partial = partialTicks;
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
        if (entity.isInvisible()) {
            return;
        }
        float rage = entity.prevShownRage + (entity.shownRage - entity.prevShownRage) * partialTicks;
        float glow = entity.prevGlow + (entity.glow - entity.prevGlow) * partialTicks;
        Vec3d base = new Vec3d(entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks,
                entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks,
                entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks);
        Vec3d at = new Vec3d(x, y, z);

        GlSnapshot before = GlSnapshot.take();
        GlStateManager.pushMatrix();
        GlStateManager.disableLighting();
        GlStateManager.disableFog();
        GlStateManager.disableCull();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
        afterimages(entity, at, base);
        WarlockFx.additive();
        corona(entity, at, rage, glow);
        GlStateManager.popMatrix();
        before.restore();
    }

    /**
     * Over the plain skin: his veins and eyes lit by his rage, the stone burning, and at the top
     * of his temper shells of the stone's light standing out from his body.
     */
    @Override
    protected void renderModel(EntityChampion entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
                               float headPitch, float scaleFactor) {
        super.renderModel(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor);
        if (entity.isInvisible()) {
            return;
        }
        float rage = entity.prevShownRage + (entity.shownRage - entity.prevShownRage) * partial;
        float glow = entity.prevGlow + (entity.glow - entity.prevGlow) * partial;
        GlSnapshot before = GlSnapshot.take();
        GlStateManager.enableBlend();
        GlStateManager.depthFunc(GL11.GL_LEQUAL);
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
        float veins = ModelChampion.smooth((rage - 0.18F) / 0.6F);
        if (veins > 0.01F) {
            bindTexture(RAGE);
            float pulse = 0.8F + 0.2F * MathHelper.sin(ageInTicks * (0.2F + 0.3F * rage));
            GlStateManager.color(1F, 1F, 1F, veins * pulse);
            model().renderPose(scaleFactor);
        }
        if (entity.wearsStone()) {
            bindTexture(STONE);
            GlStateManager.color(1F, 1F, 1F, Math.min(1F, 0.55F + 0.45F * glow));
            model().renderPose(scaleFactor);
        }
        // The aura: larger copies of him drawn from the inside only, so his own body hides all of
        // them but the rim standing out around his silhouette.
        float aura = Math.max(ModelChampion.smooth((rage - 0.55F) / 0.4F), ModelChampion.smooth((glow - 1F) / 0.4F));
        if (aura > 0.01F) {
            bindTexture(SKIN);
            GlStateManager.enableCull();
            GlStateManager.cullFace(GlStateManager.CullFace.FRONT);
            float[] alphas = {0.32F, 0.18F, 0.09F};
            float[][] colours = {ChampionFx.PALE, ChampionFx.MAGENTA, ChampionFx.VIOLET};
            for (int i = 0; i < alphas.length; i++) {
                float crackle = 0.85F + 0.15F * MathHelper.sin(ageInTicks * 1.7F + i * 2.3F) * MathHelper.sin(ageInTicks * 0.53F + i);
                float s = 1.04F + i * 0.05F + 0.012F * MathHelper.sin(ageInTicks * 0.9F + i * 2.1F) * (1 + i);
                GlStateManager.pushMatrix();
                GlStateManager.translate(0F, 1.501F - CENTRE / SCALE, 0F);
                GlStateManager.scale(s, s, s);
                GlStateManager.translate(0F, CENTRE / SCALE - 1.501F, 0F);
                GlStateManager.color(colours[i][0], colours[i][1], colours[i][2], alphas[i] * aura * crackle);
                model().renderPose(scaleFactor);
                GlStateManager.popMatrix();
            }
        }
        before.restore();
    }

    /** Echoes of him where he was a moment ago, in the stone's purple. Only when he is really moving. */
    private void afterimages(EntityChampion entity, Vec3d at, Vec3d base) {
        if (entity.trailCount < 3) {
            return;
        }
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        GlStateManager.depthMask(false);
        GlStateManager.enableCull();
        bindTexture(SKIN);
        for (int k = 1; k <= 7 && k < entity.trailCount; k += 2) {
            double[] h = entity.trail[k];
            float heat = (float) h[5];
            if (heat < 0.05F) {
                continue;
            }
            Vec3d where = new Vec3d(at.x + h[0] - base.x, at.y + h[1] - base.y + CENTRE, at.z + h[2] - base.z);
            float alpha = heat * 0.32F * (1F - (k - 1) / 8F) * (float) MathHelper.clamp((where.length() - 1.5D) / 3D, 0D, 1D);
            if (alpha < 0.01F) {
                continue;
            }
            GlStateManager.pushMatrix();
            GlStateManager.translate(at.x + h[0] - base.x, at.y + h[1] - base.y, at.z + h[2] - base.z);
            GlStateManager.rotate(180F - (float) h[3], 0F, 1F, 0F);
            GlStateManager.translate(0F, CENTRE, 0F);
            GlStateManager.rotate(-(float) h[4], 1F, 0F, 0F);
            GlStateManager.translate(0F, -CENTRE, 0F);
            GlStateManager.scale(-SCALE, -SCALE, SCALE);
            GlStateManager.translate(0F, -1.501F, 0F);
            GlStateManager.color(ChampionFx.MAGENTA[0], ChampionFx.MAGENTA[1], ChampionFx.MAGENTA[2], alpha);
            model().renderPose(0.0625F);
            GlStateManager.popMatrix();
        }
        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.disableCull();
    }

    /** At the top of his temper, the stone's light stands behind him, flickering like a storm. */
    private void corona(EntityChampion entity, Vec3d at, float rage, float glow) {
        float strength = Math.max(ModelChampion.smooth((rage - 0.6F) / 0.4F), ModelChampion.smooth((glow - 1.1F) / 0.4F));
        if (strength < 0.02F || entity.getState() == State.DRIFT) {
            return;
        }
        Vec3d c = at.add(0D, CENTRE, 0D);
        Vec3d behind = c.add(c.lengthSquared() < 1.0E-4 ? Vec3d.ZERO : c.normalize().scale(1D));
        float flicker = 0.75F + 0.25F * MathHelper.sin((entity.ticksExisted + partial) * 1.3F);
        WarlockFx.glow(behind, 2.2D + 1.2D * strength, ChampionFx.MAGENTA, 0.12F * strength * flicker);
        WarlockFx.glow(behind, 3.6D * strength, ChampionFx.VIOLET, 0.06F * strength);
        long seed = entity.ticksExisted / 2 * 7919L + entity.getEntityId();
        WarlockFx.star(behind, 0.6D, 1.6D + 0.8D * strength, 16, seed, (entity.ticksExisted + partial) * 0.02F, ChampionFx.VIOLET,
                0.22F * strength * flicker);
    }

    /** The stone in his brow, a point of purple light; and his fists, when they are about to break something. */
    private class Power implements LayerRenderer<EntityChampion> {
        @Override
        public void doRenderLayer(EntityChampion entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
                                  float netHeadYaw, float headPitch, float scale) {
            float glow = entity.prevGlow + (entity.glow - entity.prevGlow) * partialTicks;
            float rage = entity.prevShownRage + (entity.shownRage - entity.prevShownRage) * partialTicks;
            float t = (entity.clientStateTicks + partialTicks) * entity.getTempo();
            float fists;
            switch (entity.getState()) {
                case POUND:
                    fists = ModelChampion.smooth(t / 8F) * (1F - ModelChampion.smooth((t - EntityChampion.POUND_HIT - 4F) / 8F));
                    break;
                case HAMMER:
                    fists = ModelChampion.smooth(t / 6F) * (1F - ModelChampion.smooth((t - EntityChampion.HAMMER_HIT - 4F) / 8F));
                    break;
                case BREAKER:
                    fists = ModelChampion.smooth((t - 10F) / 20F);
                    break;
                case IMPACT:
                    fists = 1F - ModelChampion.smooth(t / 60F);
                    break;
                default:
                    fists = ModelChampion.smooth((rage - 0.85F) / 0.15F) * 0.5F;
                    break;
            }
            boolean stone = entity.wearsStone();
            if (!stone && fists <= 0.02F) {
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
            float flicker = 0.85F + 0.15F * MathHelper.sin(ageInTicks * 2.3F);
            if (stone) {
                GlStateManager.pushMatrix();
                model().toBrow(scale);
                float g = Math.min(1.8F, glow);
                star(0.025F + 0.03F * g * flicker, ChampionFx.WHITE, 0.85F);
                star(0.05F + 0.09F * g * flicker, ChampionFx.MAGENTA, 0.45F * Math.min(1F, 0.3F + g));
                star(0.12F + 0.22F * g * flicker, ChampionFx.VIOLET, 0.14F * g);
                GlStateManager.popMatrix();
            }
            for (int side = 0; side < 2 && fists > 0.02F; side++) {
                GlStateManager.pushMatrix();
                model().toFist(side == 0, scale);
                star(0.1F + 0.12F * fists * flicker, ChampionFx.PALE, 0.8F * fists);
                star(0.3F + 0.3F * fists * flicker, ChampionFx.MAGENTA, 0.35F * fists);
                GlStateManager.popMatrix();
            }
            before.restore();
        }

        /** Three soft discs through one point, so it glows the same from every side. */
        private void star(float radius, float[] col, float alpha) {
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            for (int axis = 0; axis < 3; axis++) {
                buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
                buffer.pos(0D, 0D, 0D).color(col[0], col[1], col[2], alpha).endVertex();
                for (int i = 0; i <= 16; i++) {
                    double a = Math.PI * 2D * i / 16D;
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
}
