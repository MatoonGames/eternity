package com.matoon.eternity.client.render;

import com.matoon.eternity.Eternity;
import com.matoon.eternity.entity.EntityMagus;
import com.matoon.eternity.entity.EntityMagus.Pose;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.Random;

/**
 * The Magus as everyone sees him. At rest a tall man in violet and silver with lavender skin
 * and blank white eyes; as what he holds rises in him he goes white-violet from head to foot,
 * a cold corona stands out around him, and his hands burn. Moving fast he leaves violet
 * echoes of himself behind.
 * <p>
 * He does not fly from place to place when he can help it. He folds: his body draws in to a
 * line of white light as tall as he is, the air rings out around it, and the line goes out.
 * Unfolding is the same, backwards, with a shock at the end of it.
 */
@SideOnly(Side.CLIENT)
public class RenderMagus extends RenderLiving<EntityMagus> {

    private static final ResourceLocation SKIN = new ResourceLocation(Eternity.MODID, "textures/entity/magus.png");
    private static final ResourceLocation SLEEPER = new ResourceLocation(Eternity.MODID, "textures/entity/magus_sleeper.png");
    private static final ResourceLocation POWER = new ResourceLocation(Eternity.MODID, "textures/entity/magus_power.png");
    private static final ResourceLocation GLOW = new ResourceLocation(Eternity.MODID, "textures/entity/magus_glow.png");

    /** His palette: white-violet, violet, deep violet. */
    static final float[] WHITE = {0.96F, 0.92F, 1F};
    static final float[] VIOLET = {0.72F, 0.46F, 1F};
    static final float[] DEEP = {0.42F, 0.18F, 0.86F};

    /** Taller than a man, as he means to be. */
    private static final float SCALE = 1.08F;
    /** His middle, in blocks above his feet. */
    private static final float CENTRE = 1.1F;
    /** Of a fold, the share spent drawing in to the line (out) or growing the line (in). */
    private static final float LINE_AT = 0.62F;

    /** Set while he is drawn in a portrait, not the world: his corona and the rest are left out. */
    public static boolean portrait;

    private float partial;

    public RenderMagus(RenderManager manager) {
        super(manager, new ModelMagus(), 0.45F);
        addLayer(new Hands());
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityMagus entity) {
        return asleep(entity) ? SLEEPER : SKIN;
    }

    private static boolean asleep(EntityMagus m) {
        int t = m.clientPoseTicks;
        return m.pose() == Pose.SLEEP || m.pose() == Pose.STIR && (t < 12 || t > 52);
    }

    private ModelMagus model() {
        return (ModelMagus) mainModel;
    }

    // ------------------------------------------------------------------ folding

    /** How far through folding he is: 0 whole, 1 a line of light; and whether the body is to be drawn at all. */
    private static float fold(EntityMagus m, float partialTicks) {
        Pose p = m.pose();
        float t = m.clientPoseTicks + partialTicks;
        switch (p) {
            case VANISH:
            case BLINK_OUT: {
                float k = t / p.ticks();
                return k >= LINE_AT ? 1F : smooth(k / LINE_AT);
            }
            case APPEAR:
            case BLINK_IN: {
                float k = t / p.ticks();
                return k <= 1F - LINE_AT ? 1F : 1F - outBack((k - (1F - LINE_AT)) / LINE_AT);
            }
            default:
                return 0F;
        }
    }

    private static float smooth(float x) {
        return ModelAdamWarlock.smooth(x);
    }

    private static float outBack(float x) {
        return ModelAdamWarlock.outBack(x);
    }

    @Override
    protected void preRenderCallback(EntityMagus entity, float partialTicks) {
        float f = MathHelper.clamp(fold(entity, partialTicks), -0.2F, 1F);
        float across = SCALE * Math.max(0.03F, 1F - f);
        float tall = SCALE * (1F + 0.55F * Math.max(0F, f));
        GlStateManager.translate(0F, -CENTRE, 0F);
        GlStateManager.scale(across, tall, across);
        GlStateManager.translate(0F, CENTRE, 0F);
    }

    /** Leaning a little into where he drifts, about his middle. */
    @Override
    protected void applyRotations(EntityMagus entity, float ageInTicks, float rotationYaw, float partialTicks) {
        GlStateManager.rotate(180F - rotationYaw, 0F, 1F, 0F);
        float pitch = entity.prevBodyPitch + (entity.bodyPitch - entity.prevBodyPitch) * partialTicks;
        float roll = entity.prevBodyRoll + (entity.bodyRoll - entity.prevBodyRoll) * partialTicks;
        GlStateManager.translate(0F, CENTRE, 0F);
        GlStateManager.rotate(-pitch, 1F, 0F, 0F);
        GlStateManager.rotate(roll, 0F, 0F, 1F);
        GlStateManager.translate(0F, -CENTRE, 0F);
    }

    @Override
    public void doRender(EntityMagus entity, double x, double y, double z, float entityYaw, float partialTicks) {
        partial = partialTicks;
        float f = fold(entity, partialTicks);
        boolean away = entity.isAway();
        if (!away && f < 0.999F) {
            super.doRender(entity, x, y, z, entityYaw, partialTicks);
        }
        if (entity.isInvisible() || portrait) {
            return;
        }
        Vec3d at = new Vec3d(x, y, z);
        Vec3d base = new Vec3d(entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks,
                entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks,
                entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks);
        GlSnapshot before = GlSnapshot.take();
        GlStateManager.pushMatrix();
        GlStateManager.disableLighting();
        GlStateManager.disableFog();
        GlStateManager.disableCull();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
        if (!away) {
            afterimages(entity, at, base);
        }
        WarlockFx.additive();
        if (!away) {
            corona(entity, at);
        }
        folding(entity, at, partialTicks);
        GlStateManager.popMatrix();
        before.restore();
    }

    @Override
    public void doRenderShadowAndFire(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
        EntityMagus m = (EntityMagus) entity;
        if (!m.isAway() && fold(m, partialTicks) < 0.5F) {
            super.doRenderShadowAndFire(entity, x, y, z, yaw, partialTicks);
        }
    }

    @Override
    protected boolean canRenderName(EntityMagus entity) {
        return false;
    }

    /**
     * Over the plain skin: the white-violet coming into it as what he holds rises, then his
     * eyes, gem and the bolt on his chest lit from within, then shells of cold light standing
     * out round his body. Folding, he is all light.
     */
    @Override
    protected void renderModel(EntityMagus entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
                               float headPitch, float scaleFactor) {
        super.renderModel(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor);
        if (entity.isInvisible()) {
            return;
        }
        float power = entity.prevPower + (entity.power - entity.prevPower) * partial;
        float f = fold(entity, partial);
        float white = Math.max(smooth((power - 0.55F) / 0.6F) * 0.85F, Math.min(1F, f * 2.5F));
        GlSnapshot before = GlSnapshot.take();
        GlStateManager.enableBlend();
        GlStateManager.depthFunc(GL11.GL_LEQUAL);
        if (white > 0.01F) {
            bindTexture(POWER);
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            GlStateManager.color(1F, 1F, 1F, white);
            model().renderPose(scaleFactor);
        }
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
        if (!asleep(entity)) {
            bindTexture(GLOW);
            float gem = entity.isCrowned() ? 1F : 0.55F;
            GlStateManager.color(1F, 1F, 1F, Math.min(1F, gem + 0.5F * power));
            model().renderPose(scaleFactor);
        }
        float aura = Math.max(smooth((power - 0.3F) / 0.6F) * Math.min(1.25F, power), f);
        if (aura > 0.01F) {
            bindTexture(POWER);
            GlStateManager.enableCull();
            GlStateManager.cullFace(GlStateManager.CullFace.FRONT);
            float[] alphas = {0.6F, 0.32F, 0.15F};
            float[][] colours = {WHITE, VIOLET, DEEP};
            for (int i = 0; i < alphas.length; i++) {
                float s = 1.05F + i * 0.065F + 0.012F * MathHelper.sin(ageInTicks * 0.7F + i * 2.1F) * (1 + i);
                GlStateManager.pushMatrix();
                GlStateManager.translate(0F, 1.501F - CENTRE, 0F);
                GlStateManager.scale(s, s, s);
                GlStateManager.translate(0F, CENTRE - 1.501F, 0F);
                GlStateManager.color(colours[i][0], colours[i][1], colours[i][2], alphas[i] * aura);
                model().renderPose(scaleFactor);
                GlStateManager.popMatrix();
            }
        }
        before.restore();
    }

    /** Violet echoes of him where he was a moment ago, when he is really moving. */
    private void afterimages(EntityMagus entity, Vec3d at, Vec3d base) {
        if (entity.trailCount < 3) {
            return;
        }
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        GlStateManager.depthMask(false);
        GlStateManager.enableCull();
        bindTexture(POWER);
        for (int k = 1; k <= 7 && k < entity.trailCount; k += 2) {
            double[] h = entity.trail[k];
            float heat = (float) h[6];
            if (heat < 0.05F) {
                continue;
            }
            Vec3d where = new Vec3d(at.x + h[0] - base.x, at.y + h[1] - base.y + CENTRE, at.z + h[2] - base.z);
            float alpha = heat * 0.42F * (1F - (k - 1) / 8F) * (float) MathHelper.clamp((where.length() - 1D) / 3D, 0D, 1D);
            if (alpha < 0.01F) {
                continue;
            }
            GlStateManager.pushMatrix();
            GlStateManager.translate(at.x + h[0] - base.x, at.y + h[1] - base.y, at.z + h[2] - base.z);
            GlStateManager.rotate(180F - (float) h[3], 0F, 1F, 0F);
            GlStateManager.translate(0F, CENTRE, 0F);
            GlStateManager.rotate(-(float) h[4], 1F, 0F, 0F);
            GlStateManager.rotate((float) h[5], 0F, 0F, 1F);
            GlStateManager.translate(0F, -CENTRE, 0F);
            GlStateManager.scale(-SCALE, -SCALE, SCALE);
            GlStateManager.translate(0F, -1.501F, 0F);
            GlStateManager.color(VIOLET[0], VIOLET[1], VIOLET[2], alpha);
            model().renderPose(0.0625F);
            GlStateManager.popMatrix();
        }
        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.disableCull();
    }

    /** A cold light behind him, and when what he holds is up, thin spikes of it that never hold still. */
    private void corona(EntityMagus entity, Vec3d at) {
        float power = entity.prevPower + (entity.power - entity.prevPower) * partial;
        float strength = smooth((power - 0.35F) / 0.65F) * Math.min(1.3F, power);
        if (strength < 0.02F) {
            return;
        }
        Vec3d c = at.add(0D, CENTRE * SCALE, 0D);
        Vec3d behind = c.add(c.normalize().scale(0.7D));
        WarlockFx.glow(behind, 1.3D + 0.5D * strength, VIOLET, 0.16F * strength);
        WarlockFx.glow(behind, 2.3D * strength, DEEP, 0.08F * strength);
        float time = entity.ticksExisted + partial;
        long flicker = entity.ticksExisted / 2;
        float spikes = smooth((strength - 0.5F) / 0.4F);
        WarlockFx.star(behind, 0.45D, 1.1D + 0.5D * strength, 16, flicker * 7919L + entity.getEntityId(), time * 0.015F, VIOLET, 0.26F * spikes);
        WarlockFx.star(behind, 0.4D, 0.8D + 0.3D * strength, 10, flicker * 104729L, -time * 0.025F, WHITE, 0.2F * spikes);
    }

    /**
     * Folding: drawing in, a line of white light as tall as he is, with the air ringing out
     * round its middle and streaks of light pulled in to it; then the line thinning to nothing.
     * Unfolding: streaks thrown out, the line growing out of a point, and the body opening out
     * of it with a shock.
     */
    private void folding(EntityMagus entity, Vec3d at, float partialTicks) {
        Pose p = entity.pose();
        boolean out = p == Pose.VANISH || p == Pose.BLINK_OUT;
        boolean in = p == Pose.APPEAR || p == Pose.BLINK_IN;
        if (!out && !in) {
            return;
        }
        float length = p.ticks();
        float k = (entity.clientPoseTicks + partialTicks) / length;
        if (k > 1.6F) {
            return;
        }
        boolean big = p == Pose.VANISH || p == Pose.APPEAR;
        double tall = (big ? 3.6D : 3D) * SCALE;
        Vec3d foot = at.add(0D, -0.4D, 0D);
        Vec3d head = foot.add(0D, tall, 0D);
        Vec3d mid = at.add(0D, CENTRE * SCALE, 0D);
        // How bright the line is, and how wide: brightest at the instant he is nothing but it.
        float pivot = out ? LINE_AT : 1F - LINE_AT;
        float line;
        double width;
        if (out) {
            line = k < pivot ? smooth(k / pivot) : 1F - smooth((k - pivot) / (1.15F - pivot));
            width = k < pivot ? 0.5D - 0.3D * k / pivot : 0.2D * (1D - smooth((k - pivot) / (1F - pivot)));
        } else {
            line = k < pivot ? smooth(k / pivot) : 1F - smooth((k - pivot) / (1F - pivot));
            width = k < pivot ? 0.2D * smooth(k / pivot) : 0.2D + 0.3D * (k - pivot) / (1F - pivot);
        }
        if (line > 0.01F && width > 0.005D) {
            WarlockFx.strip(foot, head, width * 3D, DEEP, 0.35F * line);
            WarlockFx.strip(foot, head, width * 1.6D, VIOLET, 0.6F * line);
            WarlockFx.strip(foot, head, width * 0.5D, WHITE, 0.95F * line);
        }
        // The flash at the pivot, and the ring of struck air going out from it.
        float since = k - pivot;
        if (since > -0.1F) {
            float flash = Math.max(0F, 1F - Math.abs(since) * 5F);
            WarlockFx.glow(mid, (big ? 3.2D : 2D) * flash + 0.3D, WHITE, 0.9F * flash);
            WarlockFx.glow(mid, (big ? 6D : 3.5D) * flash, VIOLET, 0.35F * flash);
            if (since > 0F) {
                float r = since / (1.6F - pivot);
                double grow = 1D - Math.pow(1D - Math.min(1F, r), 3D);
                float fade = 1F - Math.min(1F, r);
                WarlockFx.ring(mid, new Vec3d(0D, 1D, 0D), (big ? 7D : 3.5D) * grow + 0.3D, 0.4D + 0.6D * grow, WHITE, 0.7F * fade * fade);
                WarlockFx.ring(mid.add(0D, 0.4D, 0D), new Vec3d(0D, 1D, 0D), (big ? 5D : 2.5D) * grow + 0.2D, 0.5D, VIOLET, 0.5F * fade);
                if (big) {
                    WarlockFx.ring(mid, new Vec3d(1D, 0D, 0D).rotateYaw((float) Math.toRadians(-entity.rotationYaw)),
                            4D * grow + 0.2D, 0.3D, VIOLET, 0.35F * fade);
                }
            }
        }
        // Streaks of light: pulled in to him, folding; thrown out, unfolding.
        if (k < pivot + 0.15F) {
            Random rand = new Random(entity.getEntityId() * 31L + entity.ticksExisted / 40);
            int count = big ? 22 : 10;
            float travel = out ? smooth(k / pivot) : 1F - smooth(k / pivot);
            for (int i = 0; i < count; i++) {
                double a = rand.nextDouble() * Math.PI * 2D;
                double y = (rand.nextDouble() - 0.3D) * tall * 0.8D;
                double far = (big ? 3.5D : 2D) + rand.nextDouble() * 2D;
                double r0 = far * (1D - travel), r1 = Math.max(0.05D, r0 - (big ? 1.4D : 0.9D));
                Vec3d dir = new Vec3d(Math.cos(a), 0D, Math.sin(a));
                Vec3d s0 = mid.add(dir.scale(r0)).add(0D, y * (1D - travel * 0.6D), 0D);
                Vec3d s1 = mid.add(dir.scale(r1)).add(0D, y * (1D - travel * 0.6D), 0D);
                float alpha = (1F - Math.abs(travel - 0.5F) * 1.6F) * 0.8F;
                WarlockFx.strip(s0, s1, 0.05D, 0.03D, VIOLET, WHITE, 0F, alpha);
            }
        }
    }

    /** His hands burn when he means to use them. */
    private class Hands implements LayerRenderer<EntityMagus> {
        @Override
        public void doRenderLayer(EntityMagus entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
                                  float netHeadYaw, float headPitch, float scale) {
            float t = entity.clientPoseTicks + partialTicks;
            float right = 0F, left = 0F;
            switch (entity.pose()) {
                case POINT:
                    right = 0.6F;
                    break;
                case CRUSH:
                    right = t < EntityMagus.CRUSH_CLOSE ? smooth(t / 12F) * (0.6F + 0.4F * t / EntityMagus.CRUSH_CLOSE)
                            : 1.4F * (1F - smooth((t - EntityMagus.CRUSH_CLOSE) / 12F));
                    break;
                case BLOCK:
                    right = 1F;
                    break;
                case CATCH:
                    right = 0.5F;
                    break;
                case GUARD:
                    left = 0.25F;
                    break;
                case PALM:
                    left = t < EntityMagus.PALM_HIT + 4 ? 1F : 0.3F;
                    right = 0.3F;
                    break;
                case BACKHAND:
                    right = t < EntityMagus.BACKHAND_HIT + 4 ? 1F : 0.3F;
                    break;
                case KNEE:
                    right = left = 0.4F;
                    break;
                case HAMMER:
                case BLAST:
                    right = left = Math.min(1F, t / 10F);
                    break;
                case GRIP:
                    right = 0.35F;
                    break;
                case TAKE:
                    right = 0.35F;
                    left = t < 10F ? 0.4F : t < 30F ? 1.2F : 0.6F;
                    break;
                default:
                    break;
            }
            if (right <= 0.02F && left <= 0.02F || entity.isAway()) {
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
            float flicker = 0.85F + 0.15F * MathHelper.sin(ageInTicks * 1.7F);
            for (int side = 0; side < 2; side++) {
                float strength = side == 0 ? right : left;
                if (strength <= 0.02F) {
                    continue;
                }
                GlStateManager.pushMatrix();
                model().toHand(side == 0, scale);
                star(0.12F + 0.14F * strength * flicker, WHITE, Math.min(1F, 0.85F * strength));
                star(0.3F + 0.28F * strength * flicker, VIOLET, Math.min(1F, 0.4F * strength));
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
