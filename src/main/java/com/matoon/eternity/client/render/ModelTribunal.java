package com.matoon.eternity.client.render;

import com.matoon.eternity.Eternity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.Random;

/**
 * The Living Tribunal as a Minecraft mob: textured boxes, like any creature in the
 * game, with his skin painted in pixels (see tools/TribunalSkin.java). A golden giant
 * with a burning white void in his chest; his head floats free of his shoulders under
 * a purple hood, with three faces: Equity looking ahead with blazing white eyes,
 * Vengeance in profile on his left, and Necessity hidden under the cloth on his right.
 * His eyes and heart are drawn again from a glow layer, full bright, like an
 * enderman's eyes.
 *
 * Model space is the vanilla one: pixels, Y down, faces looking toward -Z. The origin
 * is on the floor between his feet. Seated, he sits on a throne of rough stone;
 * standing, his right arm can swing up and reach out, fingers spreading.
 */
@SideOnly(Side.CLIENT)
public class ModelTribunal extends ModelBase {

    private static final ResourceLocation SKIN = new ResourceLocation(Eternity.MODID, "textures/entity/tribunal.png");
    private static final ResourceLocation GLOW = new ResourceLocation(Eternity.MODID, "textures/entity/tribunal_glow.png");
    private static final float HALF_PI = (float) Math.PI / 2F;
    private static final Random RANDOM = new Random();

    private final ModelRenderer head;
    private final ModelRenderer body;
    private final ModelRenderer rightArm;
    private final ModelRenderer rightForearm;
    private final ModelRenderer[] fingers = new ModelRenderer[4];
    private final ModelRenderer leftArm;
    private final ModelRenderer leftForearm;
    private final ModelRenderer rightLeg;
    private final ModelRenderer rightShin;
    private final ModelRenderer leftLeg;
    private final ModelRenderer leftShin;
    private final ModelRenderer throne;

    public ModelTribunal() {
        textureWidth = 128;
        textureHeight = 128;

        head = new ModelRenderer(this);
        head.setTextureOffset(0, 0).addBox(-5F, -10F, -5F, 10, 10, 10);
        head.setTextureOffset(40, 0).addBox(-6F, -13F, -6F, 12, 3, 12);
        head.setTextureOffset(88, 0).addBox(-6F, -11F, 5F, 12, 13, 2);
        head.setTextureOffset(88, 15).addBox(-6F, -10F, -6.5F, 12, 2, 2);
        head.setTextureOffset(0, 20).addBox(-7F, -11F, -4.5F, 2, 13, 9);
        head.setTextureOffset(22, 20).addBox(5F, -11F, -4.5F, 2, 13, 9);
        // Vengeance on his left (+X), Necessity veiled on his right (-X).
        head.setTextureOffset(44, 20).addBox(7F, -8F, -3F, 1, 6, 6);
        head.setTextureOffset(58, 20).addBox(8F, -5F, -0.5F, 1, 2, 1);
        head.setTextureOffset(62, 20).addBox(-8F, -9F, -3F, 1, 7, 6);
        head.setTextureOffset(76, 20).addBox(-9F, -6F, -0.5F, 1, 2, 1);

        body = new ModelRenderer(this);
        body.setTextureOffset(36, 42).addBox(-5F, -6F, -2.5F, 10, 6, 5);
        body.setTextureOffset(0, 42).addBox(-6F, -18F, -3F, 12, 12, 6);

        rightArm = arm(false);
        rightForearm = forearm(rightArm, false);
        for (int i = 0; i < fingers.length; i++) {
            ModelRenderer finger = new ModelRenderer(this);
            finger.setRotationPoint(-1.5F + i, 11F, -1F);
            finger.setTextureOffset(112, 37).addBox(-0.5F, 0F, -0.5F, 1, 3, 1);
            rightForearm.addChild(finger);
            fingers[i] = finger;
        }
        leftArm = arm(true);
        leftForearm = forearm(leftArm, true);

        rightLeg = leg(false);
        rightShin = shin(rightLeg, false);
        leftLeg = leg(true);
        leftShin = shin(leftLeg, true);

        throne = new ModelRenderer(this);
        throne.setTextureOffset(0, 64).addBox(-8F, -6F, -4F, 16, 6, 12);
        throne.setTextureOffset(0, 82).addBox(-11F, -34F, 8F, 22, 34, 3);
        throne.setTextureOffset(50, 100).addBox(-12F, -37F, 7F, 24, 3, 5);
        throne.setTextureOffset(56, 64).addBox(-12F, -14F, -6F, 5, 14, 14);
        throne.mirror = true;
        throne.setTextureOffset(56, 64).addBox(7F, -14F, -6F, 5, 14, 14);
    }

    private ModelRenderer arm(boolean mirror) {
        ModelRenderer arm = new ModelRenderer(this);
        arm.mirror = mirror;
        arm.setTextureOffset(96, 24).addBox(-2.5F, -3F, -3F, 5, 5, 6);
        arm.setTextureOffset(80, 24).addBox(-2F, -1F, -2F, 4, 9, 4);
        return arm;
    }

    private ModelRenderer forearm(ModelRenderer arm, boolean mirror) {
        ModelRenderer forearm = new ModelRenderer(this);
        forearm.mirror = mirror;
        forearm.setRotationPoint(0F, 8F, 0F);
        forearm.setTextureOffset(80, 37).addBox(-2F, 0F, -2F, 4, 8, 4);
        forearm.setTextureOffset(96, 37).addBox(-2F, 8F, -2F, 4, 3, 4);
        arm.addChild(forearm);
        return forearm;
    }

    private ModelRenderer leg(boolean mirror) {
        ModelRenderer leg = new ModelRenderer(this);
        leg.mirror = mirror;
        leg.setTextureOffset(66, 49).addBox(-2.5F, 0F, -2.5F, 5, 8, 5);
        return leg;
    }

    private ModelRenderer shin(ModelRenderer leg, boolean mirror) {
        ModelRenderer shin = new ModelRenderer(this);
        shin.mirror = mirror;
        shin.setRotationPoint(0F, 8F, 0F);
        shin.setTextureOffset(86, 49).addBox(-2F, 0F, -2F, 4, 8, 4);
        shin.setTextureOffset(102, 49).addBox(-2F, 6F, -4F, 4, 2, 6);
        leg.addChild(shin);
        return shin;
    }

    /** How he holds himself. */
    public enum Pose {
        /** On his throne in judgement. */
        SEATED,
        /** Standing, his right arm reaching out by t. */
        REACH,
        /** Standing, breathing. */
        IDLE,
        /** Raising his fist overhead and bringing it down, over t. */
        GAVEL,
        /** Head bowed, glaring at the floor. */
        GAZE,
        /** Both arms flung up and out. */
        CAST,
        /** Sinking to his knees, by t. */
        KNEEL
    }

    private void pose(Pose pose, float t, float time) {
        if (pose == Pose.SEATED) {
            rightLeg.setRotationPoint(-3F, -8F, 3F);
            leftLeg.setRotationPoint(3F, -8F, 3F);
            rightLeg.rotateAngleX = leftLeg.rotateAngleX = -HALF_PI;
            rightShin.rotateAngleX = leftShin.rotateAngleX = HALF_PI;
            body.setRotationPoint(0F, -8F, 3F);
            body.rotateAngleX = 0F;
            rightArm.setRotationPoint(-8.5F, -24F, 3F);
            leftArm.setRotationPoint(8.5F, -24F, 3F);
            rightArm.rotateAngleX = leftArm.rotateAngleX = -0.1F;
            rightArm.rotateAngleZ = leftArm.rotateAngleZ = 0F;
            rightForearm.rotateAngleX = leftForearm.rotateAngleX = -HALF_PI + 0.1F;
            // His head bows a little, to look down the aisle at the one he judges.
            head.setRotationPoint(0F, -28F, 3F);
            head.rotateAngleX = 0.12F;
            for (ModelRenderer finger : fingers) {
                finger.rotateAngleX = 1.1F;
                finger.rotateAngleZ = 0F;
            }
            return;
        }

        // Standing, or sinking toward his knees: thighs swing forward, shins fold back,
        // and everything above the hips drops with them.
        float kneel = pose == Pose.KNEEL ? t : 0F;
        float drop = 7.4F * kneel;
        float lean = 0.35F * kneel;
        rightLeg.setRotationPoint(-3F, -16F + drop, 0F);
        leftLeg.setRotationPoint(3F, -16F + drop, 0F);
        rightLeg.rotateAngleX = leftLeg.rotateAngleX = -1F * kneel;
        rightShin.rotateAngleX = leftShin.rotateAngleX = 2F * kneel;
        body.setRotationPoint(0F, -16F + drop, 0F);
        body.rotateAngleX = lean;
        float shoulderY = -32F + drop + 1.5F * kneel;
        float shoulderZ = -4F * kneel;
        rightArm.setRotationPoint(-8.5F, shoulderY, shoulderZ);
        leftArm.setRotationPoint(8.5F, shoulderY, shoulderZ);
        head.setRotationPoint(0F, -36F + drop + 2F * kneel, -5F * kneel);

        float breath = MathHelper.sin(time * 0.05F) * 0.04F;
        rightArm.rotateAngleX = leftArm.rotateAngleX = breath;
        rightArm.rotateAngleZ = 0.06F;
        leftArm.rotateAngleZ = -0.06F;
        rightForearm.rotateAngleX = leftForearm.rotateAngleX = -0.15F;
        head.rotateAngleX = 0F;
        float spread = 0.15F;
        float curl = 0F;

        switch (pose) {
            case REACH:
                rightArm.rotateAngleX = -1.65F * t;
                rightArm.rotateAngleZ = 0.15F * t;
                rightForearm.rotateAngleX = -0.2F * (1F - t);
                head.rotateAngleX = 0.08F * t;
                spread = 0.15F + 0.35F * t;
                curl = -0.3F * t;
                break;
            case GAVEL: {
                // Up over his head through most of it, then down like a hammer.
                float up = t < 0.5F ? smooth(t / 0.5F) : 1F - smooth(Math.min(1F, (t - 0.5F) / 0.08F));
                rightArm.rotateAngleX = -3F * up - 0.4F * (1F - up) * (t > 0.5F ? 1F : 0F);
                rightForearm.rotateAngleX = -0.4F * up;
                head.rotateAngleX = 0.25F * (1F - up);
                curl = 1.2F;
                spread = 0F;
                break;
            }
            case GAZE:
                head.rotateAngleX = 0.35F;
                rightArm.rotateAngleZ = 0.35F;
                leftArm.rotateAngleZ = -0.35F;
                break;
            case CAST:
                rightArm.rotateAngleX = leftArm.rotateAngleX = -2.2F;
                rightArm.rotateAngleZ = 0.6F;
                leftArm.rotateAngleZ = -0.6F;
                rightForearm.rotateAngleX = leftForearm.rotateAngleX = -0.3F;
                head.rotateAngleX = -0.25F;
                spread = 0.6F;
                break;
            case KNEEL:
                rightArm.rotateAngleX = leftArm.rotateAngleX = -0.35F * t;
                head.rotateAngleX = 0.55F * t;
                break;
            default:
                break;
        }
        for (int i = 0; i < fingers.length; i++) {
            fingers[i].rotateAngleX = curl;
            fingers[i].rotateAngleZ = (i - 1.5F) * spread;
        }
    }

    private static float smooth(float x) {
        float c = MathHelper.clamp(x, 0F, 1F);
        return c * c * (3F - 2F * c);
    }

    /** A translucent golden ghost of him, glowing, for his echoes from other universes. */
    public void drawGhost(Pose pose, float t, float alpha, float time) {
        pose(pose, t, time);
        GlStateManager.pushMatrix();
        GlStateManager.enableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.enableCull();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        GlStateManager.depthMask(false);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
        GlStateManager.color(1F, 0.85F, 0.55F, 0.55F * alpha);
        Minecraft.getMinecraft().getTextureManager().bindTexture(SKIN);
        renderParts(pose == Pose.SEATED);
        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.disableCull();
        GlStateManager.popMatrix();
    }

    private void renderParts(boolean seated) {
        if (seated) {
            throne.render(1F);
        }
        head.render(1F);
        body.render(1F);
        rightArm.render(1F);
        leftArm.render(1F);
        rightLeg.render(1F);
        leftLeg.render(1F);
    }

    /**
     * Draws him in model space (pixels, Y down, facing -Z); the caller flips and scales.
     *
     * @param t    how far through the pose he is, 0 to 1, where it has a course
     * @param eyes how brightly his eyes and heart burn, 0 to 1
     */
    public void draw(Pose pose, float t, float eyes, float time) {
        boolean seated = pose == Pose.SEATED;
        pose(pose, t, time);
        Minecraft mc = Minecraft.getMinecraft();
        GlStateManager.pushMatrix();
        GlStateManager.enableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.color(1F, 1F, 1F, 1F);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
        mc.getTextureManager().bindTexture(SKIN);
        renderParts(seated);

        // His eyes and heart, again, burning.
        float flicker = 0.85F + 0.15F * MathHelper.sin(time * 0.6F);
        float e = Math.min(1F, 0.4F + 0.6F * eyes) * flicker;
        mc.getTextureManager().bindTexture(GLOW);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        GlStateManager.depthMask(false);
        GlStateManager.color(1F, 1F, 1F, e);
        renderParts(seated);
        GlStateManager.color(1F, 1F, 1F, 1F);

        GlStateManager.disableTexture2D();
        GlStateManager.disableCull();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        auras(e, time);

        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.depthMask(true);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.enableCull();
        GlStateManager.popMatrix();
    }

    /** Light spilling from his eyes and heart, and lightning crawling over his body. */
    private void auras(float e, float time) {
        float eyeY = head.rotationPointY - 5.5F;
        float eyeZ = head.rotationPointZ - 5.2F;
        disc(-3F, eyeY, eyeZ, 2.4F, 0.75F, 0.92F, 1F, 0.5F * e);
        disc(3F, eyeY, eyeZ, 2.4F, 0.75F, 0.92F, 1F, 0.5F * e);

        float heartY = body.rotationPointY - 14F;
        float heartZ = body.rotationPointZ - 3.1F;
        disc(0F, heartY, heartZ, 5F, 0.85F, 0.97F, 1F, 0.55F * e);
        disc(0F, heartY, heartZ - 0.05F, 11F, 0.5F, 0.75F, 1F, 0.22F * e);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder b = tessellator.getBuffer();
        GlStateManager.glLineWidth(2.5F);
        RANDOM.setSeed((long) (time / 3F) * 31L);
        b.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        for (int bolt = 0; bolt < 8; bolt++) {
            float x = (RANDOM.nextFloat() - 0.5F) * 20F;
            float y = body.rotationPointY - 2F - RANDOM.nextFloat() * 16F;
            float z = body.rotationPointZ + (RANDOM.nextFloat() - 0.5F) * 8F;
            for (int seg = 0; seg < 6; seg++) {
                float nx = x + (RANDOM.nextFloat() - 0.5F) * 5F;
                float ny = y - (RANDOM.nextFloat() - 0.3F) * 4F;
                float nz = z + (RANDOM.nextFloat() - 0.5F) * 4F;
                float fade = (1F - seg / 6F) * e;
                b.pos(x, y, z).color(0.8F, 0.75F, 1F, fade).endVertex();
                b.pos(nx, ny, nz).color(0.8F, 0.75F, 1F, fade).endVertex();
                x = nx;
                y = ny;
                z = nz;
            }
        }
        tessellator.draw();
        GlStateManager.glLineWidth(1F);
    }

    /** A soft disc of light in the plane facing -Z. */
    private static void disc(float x, float y, float z, float radius, float r, float g, float bl, float a) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder b = tessellator.getBuffer();
        b.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        b.pos(x, y, z).color(r, g, bl, a).endVertex();
        for (int i = 0; i <= 24; i++) {
            double angle = Math.PI * 2D * i / 24D;
            b.pos(x + Math.cos(angle) * radius, y + Math.sin(angle) * radius, z).color(r, g, bl, 0F).endVertex();
        }
        tessellator.draw();
    }
}
