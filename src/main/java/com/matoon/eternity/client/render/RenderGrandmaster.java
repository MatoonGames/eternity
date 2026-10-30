package com.matoon.eternity.client.render;

import com.matoon.eternity.Eternity;
import com.matoon.eternity.entity.EntityGrandmaster;
import com.matoon.eternity.entity.EntityGrandmaster.State;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector4f;

import java.nio.FloatBuffer;
import java.util.Random;

/**
 * The Grandmaster as everyone sees him: a tall blue man in gold, his white eyes lit, never
 * still. His cards are drawn here too: the waterfall between his hands when he shuffles, the
 * storm he steps out of, the gold threads from his fingertip to whatever he is moving without
 * touching it. As his board breaks, light breaks through him along fissures; when he dies he
 * comes apart into cards and blows away.
 */
@SideOnly(Side.CLIENT)
public class RenderGrandmaster extends RenderLiving<EntityGrandmaster> {

    private static final ResourceLocation SKIN = new ResourceLocation(Eternity.MODID, "textures/entity/grandmaster.png");
    private static final ResourceLocation GLOW = new ResourceLocation(Eternity.MODID, "textures/entity/grandmaster_glow.png");
    private static final ResourceLocation CRACKS = new ResourceLocation(Eternity.MODID, "textures/entity/grandmaster_cracks.png");

    /** He stands a head over everyone. */
    public static final float SCALE = 1.12F;

    /** Set while he is drawn in a portrait, not the world: his effects in the world are left out. */
    public static boolean portrait;

    private static final FloatBuffer MATRIX = GLAllocation.createDirectFloatBuffer(16);
    private final Matrix4f view = new Matrix4f();
    private float partial;
    /** Camera-relative, this frame: his palms and his right fingertip. */
    private Vec3d handR = Vec3d.ZERO, handL = Vec3d.ZERO, fingertip = Vec3d.ZERO;

    public RenderGrandmaster(RenderManager manager) {
        super(manager, new ModelGrandmaster(), 0.5F);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityGrandmaster entity) {
        return SKIN;
    }

    private ModelGrandmaster model() {
        return (ModelGrandmaster) mainModel;
    }

    @Override
    protected void preRenderCallback(EntityGrandmaster entity, float partialTickTime) {
        GlStateManager.scale(SCALE, SCALE, SCALE);
    }

    @Override
    public void doRender(EntityGrandmaster entity, double x, double y, double z, float entityYaw, float partialTicks) {
        partial = partialTicks;
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, MATRIX);
        view.load(MATRIX);
        MATRIX.rewind();
        view.invert();
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
        if (portrait) {
            return;
        }

        GlSnapshot before = GlSnapshot.take();
        GlStateManager.pushMatrix();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableTexture2D();
        int light = entity.getBrightnessForRender();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light % 65536, light / 65536);
        float t = entity.clientStateTicks + partialTicks;
        Vec3d at = new Vec3d(x, y, z);
        Cards.bind();
        Cards.begin();
        switch (entity.getState()) {
            case SHUFFLE:
                waterfall(t);
                break;
            case ARRIVE:
                storm(entity, at, t);
                break;
            case FADE:
                blowAway(entity, at, t);
                break;
            default:
                break;
        }
        Cards.end();
        if (entity.getState() == State.CONDUCT) {
            threads(entity, t);
        }
        GlStateManager.popMatrix();
        before.restore();
    }

    @Override
    protected void renderModel(EntityGrandmaster entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
                               float headPitch, float scaleFactor) {
        float fade = entity.getState() == State.FADE ? 1F - ModelGrandmaster.smooth((entity.clientStateTicks + partial - 10F) / 60F) : 1F;
        if (fade <= 0.01F) {
            locate(scaleFactor);
            return;
        }
        if (fade < 1F) {
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            GlStateManager.color(1F, 1F, 1F, fade);
            bindTexture(SKIN);
            mainModel.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor);
            GlStateManager.color(1F, 1F, 1F, 1F);
        } else {
            super.renderModel(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor);
        }
        locate(scaleFactor);

        GlSnapshot before = GlSnapshot.take();
        GlStateManager.enableBlend();
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.depthFunc(GL11.GL_LEQUAL);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
        // His eyes, the jewel, the edges of the gold.
        bindTexture(GLOW);
        float flicker = entity.getBroken() > 0.3F ? 0.7F + 0.3F * MathHelper.sin(ageInTicks * 1.7F) * MathHelper.sin(ageInTicks * 0.23F) : 1F;
        GlStateManager.color(1F, 1F, 1F, fade * flicker);
        model().renderPose(scaleFactor);
        // The light of his board going out through him.
        float broken = entity.getBroken();
        if (entity.getState() == State.FALL || entity.getState() == State.LIE || entity.getState() == State.FADE) {
            broken = 1F;
        }
        if (broken > 0.02F) {
            bindTexture(CRACKS);
            float pulse = 0.65F + 0.35F * MathHelper.sin(ageInTicks * 0.5F);
            GlStateManager.color(1F, 1F, 1F, Math.min(0.85F, broken * 1.1F) * pulse * fade);
            model().renderPose(scaleFactor);
        }
        before.restore();
    }

    /** Where his hands are this frame, camera-relative. */
    private void locate(float scale) {
        handR = point(() -> model().toHand(true, scale));
        handL = point(() -> model().toHand(false, scale));
        fingertip = point(() -> model().toFingertip(true, scale));
    }

    private Vec3d point(Runnable transform) {
        GlStateManager.pushMatrix();
        transform.run();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, MATRIX);
        GlStateManager.popMatrix();
        Matrix4f m = new Matrix4f();
        m.load(MATRIX);
        MATRIX.rewind();
        Matrix4f rel = Matrix4f.mul(view, m, null);
        Vector4f p = Matrix4f.transform(rel, new Vector4f(0F, 0F, 0F, 1F), null);
        return new Vec3d(p.x, p.y, p.z);
    }

    // ------------------------------------------------------------------ cards

    /** A stream of cards from his high right hand down into his left, each one turning over as it falls. */
    private void waterfall(float t) {
        float in = ModelGrandmaster.smooth(t / 8F) * (1F - ModelGrandmaster.smooth((t - EntityGrandmaster.SHUFFLE_TICKS + 8F) / 6F));
        if (in <= 0.02F) {
            return;
        }
        Vec3d from = handR, to = handL;
        Vec3d span = to.subtract(from);
        int count = 13;
        for (int i = 0; i < count; i++) {
            float f = ((t * 0.075F) + i / (float) count) % 1F;
            Vec3d c = from.add(span.scale(f)).add(0D, Math.sin(Math.PI * f) * 0.28D, 0D);
            Vec3d[] axes = Cards.spun(span, Math.PI * f * 1.2D + 0.3D);
            float alpha = in * Math.min(1F, Math.min(f, 1F - f) * 8F);
            Cards.card(c, axes[0], axes[1], Cards.face(i), alpha, 0.85D);
        }
        // The deck in each hand.
        Vec3d[] flat = Cards.spun(span, Math.PI * 0.5D);
        for (int k = 0; k < 4; k++) {
            Cards.card(from.add(0D, -0.04D * k, 0D), flat[0], flat[1], 0, in, 0.85D);
            Cards.card(to.add(0D, -0.04D * k, 0D), flat[0], flat[1], 0, in, 0.85D);
        }
    }

    /** He steps out of a storm of cards that spins in round him and is gone into him. */
    private void storm(EntityGrandmaster entity, Vec3d at, float t) {
        if (t > 30F) {
            return;
        }
        float close = ModelGrandmaster.smooth(t / 26F);
        Random r = new Random(entity.getEntityId() * 31L);
        for (int i = 0; i < 70; i++) {
            double a = r.nextDouble() * Math.PI * 2D + t * (0.25D + r.nextDouble() * 0.2D);
            double radius = (1.2D + r.nextDouble() * 2.4D) * (1D - close) + 0.2D;
            double h = r.nextDouble() * 2.6D + Math.sin(t * 0.3D + i) * 0.2D;
            Vec3d c = at.add(Math.cos(a) * radius, h * (1D - close * 0.4D) + 0.2D, Math.sin(a) * radius);
            Vec3d[] axes = Cards.tumbling(i * 7919L, t * 3F);
            Cards.card(c, axes[0], axes[1], Cards.face(i), 1F - ModelGrandmaster.smooth((t - 22F) / 8F), 1D);
        }
    }

    /** He comes apart into cards from the feet up, and they lift and drift off on the wind. */
    private void blowAway(EntityGrandmaster entity, Vec3d at, float t) {
        Random r = new Random(entity.getEntityId() * 977L);
        float yaw = (float) Math.toRadians(entity.rotationYaw);
        Vec3d wind = new Vec3d(-MathHelper.sin(yaw + 1.2F), 0D, MathHelper.cos(yaw + 1.2F));
        for (int i = 0; i < 90; i++) {
            float start = 4F + i * 0.75F;
            float age = t - start;
            if (age < 0F) {
                continue;
            }
            // Somewhere along his body as he lies there.
            double along = (r.nextDouble() - 0.5D) * 2.2D;
            Vec3d origin = at.add(-MathHelper.sin(yaw) * along + (r.nextDouble() - 0.5D) * 0.5D, 0.2D + r.nextDouble() * 0.35D,
                    MathHelper.cos(yaw) * along + (r.nextDouble() - 0.5D) * 0.5D);
            double rise = age * 0.035D + age * age * 0.0009D;
            Vec3d c = origin.add(wind.scale(age * 0.05D + r.nextDouble() * 0.02D * age)).add(0D, rise, 0D)
                    .add(Math.sin(age * 0.2D + i) * 0.15D, 0D, Math.cos(age * 0.17D + i) * 0.15D);
            float alpha = Math.min(1F, age / 4F) * (1F - ModelGrandmaster.smooth((age - 30F) / 25F));
            if (alpha <= 0.01F) {
                continue;
            }
            Vec3d[] axes = Cards.tumbling(i * 104729L, age * 2F);
            Cards.card(c, axes[0], axes[1], Cards.face(i), alpha, 0.9D);
        }
    }

    // ------------------------------------------------------------------ telekinesis

    /** Fine gold threads from his fingertip to whatever he is moving, rippling, brightening where they meet it. */
    private void threads(EntityGrandmaster entity, float t) {
        float in = ModelGrandmaster.smooth((t - 3F) / 5F);
        if (in <= 0.02F) {
            return;
        }
        Vec3d focus = entity.getFocus();
        Vec3d to = new Vec3d(focus.x - renderManager.viewerPosX, focus.y - renderManager.viewerPosY, focus.z - renderManager.viewerPosZ);
        Vec3d from = fingertip;
        if (to.distanceTo(from) > 40D) {
            return;
        }
        GlStateManager.disableTexture2D();
        WarlockFx.additive();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
        Vec3d[] basis = WarlockFx.basis(to.subtract(from).normalize());
        for (int k = 0; k < 5; k++) {
            Vec3d last = from;
            int steps = 18;
            for (int s = 1; s <= steps; s++) {
                double f = s / (double) steps;
                double wobble = Math.sin(f * Math.PI) * 0.22D;
                double phase = t * 0.35D + k * 1.3D + f * 6D;
                Vec3d p = from.add(to.subtract(from).scale(f)).add(basis[0].scale(Math.cos(phase) * wobble * (0.4D + k * 0.15D)))
                        .add(basis[1].scale(Math.sin(phase * 1.3D) * wobble * (0.4D + k * 0.15D)));
                WarlockFx.strip(last, p, 0.025D, WarlockFx.GOLD, 0.55F * in);
                last = p;
            }
        }
        WarlockFx.glow(from, 0.25D, WarlockFx.WHITE, 0.8F * in);
        WarlockFx.glow(from, 0.6D, WarlockFx.GOLD, 0.35F * in);
        WarlockFx.glow(to, 1.4D, WarlockFx.GOLD, 0.25F * in);
        GlStateManager.enableTexture2D();
    }
}
