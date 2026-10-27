package com.matoon.eternity.client.render;

import com.matoon.eternity.Eternity;
import com.matoon.eternity.entity.EntityEternity;
import com.matoon.eternity.entity.EntityEternity.Attack;
import com.matoon.eternity.entity.EntityEternity.State;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.nio.FloatBuffer;
import java.util.Random;

/**
 * Eternity as he is drawn in the books, made of blocks: a gaunt giant whose body is a window
 * onto deep space, painted with drifting gas and stars and with more stars moving inside it,
 * a blue mask of a face in a tall hood, two great hooked crests, and a pale tattered cloak.
 * The stars in his skin flare and fade in turn; a dim violet glow stands out around his
 * outline. Around him, splinters of broken reality turn in the air and stardust drifts off
 * his fingers, crests and hem; his eyes burn; and each of his attacks shows in his hands
 * before it lands. When he dies, light pours out of him.
 */
@SideOnly(Side.CLIENT)
public class RenderEternity extends Render<EntityEternity> {

    /** Blocks per model unit at scale 1, so that he stands {@link EntityEternity#BASE_HEIGHT} tall to the top of his hood. */
    public static final float MODEL_SCALE = 2.1F;
    private static final float S = 0.0625F;

    private static final ResourceLocation SKIN = new ResourceLocation(Eternity.MODID, "textures/entity/eternity.png");
    private static final ResourceLocation GLOW_A = new ResourceLocation(Eternity.MODID, "textures/entity/eternity_glow_a.png");
    private static final ResourceLocation GLOW_B = new ResourceLocation(Eternity.MODID, "textures/entity/eternity_glow_b.png");

    private static final ModelEternity MODEL = new ModelEternity();
    private static final Random RAYS = new Random();
    private static final FloatBuffer MATRIX = GLAllocation.createDirectFloatBuffer(16);

    static final float[] WHITE = {1F, 0.97F, 0.92F};
    static final float[] GOLD = {1F, 0.86F, 0.52F};
    static final float[] ICE = {0.6F, 0.85F, 1F};
    static final float[] VIOLET = {0.58F, 0.32F, 1F};
    static final float[] INDIGO = {0.24F, 0.18F, 0.7F};
    static final float[] EMBER = {1F, 0.52F, 0.16F};
    static final float[] ROSE = {1F, 0.55F, 0.78F};
    static final float[] MINT = {0.58F, 1F, 0.78F};
    static final float[] BLACK = {0F, 0F, 0F};
    private static final float[][] SHARD_COLOURS = {GOLD, ROSE, MINT, ICE, {0.85F, 0.7F, 1F}, {1F, 0.75F, 0.5F}};

    /** The eye-space matrix at his feet, before the model's own scale, column-major. */
    private static final float[] M = new float[16];
    /** One model unit, in eye-space units. */
    private static float unit;

    public RenderEternity(RenderManager manager) {
        super(manager);
        shadowSize = 0F;
    }

    @Override
    public void doRender(EntityEternity entity, double x, double y, double z, float entityYaw, float partialTicks) {
        float yaw = interpolate(entity.prevRenderYawOffset, entity.renderYawOffset, partialTicks);
        float headYaw = MathHelper.wrapDegrees(interpolate(entity.prevRotationYawHead, entity.rotationYawHead, partialTicks) - yaw);
        float pitch = entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks;
        State state = entity.getState();
        float ticks = entity.clientStateTicks + partialTicks;
        float containment = entity.getContainment();
        // As the Regulator drinks him in, he fades and his light is pulled back inside.
        float presence = presence(state, ticks) * (1F - 0.95F * containment);
        double distSq = x * x + y * y + z * z;

        drawFigure(entity, null, x, y, z, yaw, headYaw, pitch, entity.getScale(), presence, entity.ticksExisted + partialTicks, partialTicks,
                state, ticks, distSq, true);

        if (state == State.DYING) {
            drawDeathLight(x, y + entity.height * 0.65D, z, entity.getScale() * (1F - containment), ticks / 560F * (1F - containment));
        }
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    /** 0 to 1 as he unfolds out of a rift, 1 to 0 as he folds back into one. */
    private static float presence(State state, float ticks) {
        if (state == State.EMERGE) {
            return MathHelper.clamp(ticks / 60F, 0F, 1F);
        }
        if (state == State.DEPART) {
            return 1F - MathHelper.clamp(ticks / 45F, 0F, 1F);
        }
        return 1F;
    }

    /** Also used for the colossal silhouette seen across the void in space, and the figure in the Regulator. */
    public static void drawFigure(double x, double y, double z, float yaw, float headYaw, float pitch, float scale,
                                  float presence, float age, State state, int stateTicks, double distSq, boolean inWorldPass) {
        drawFigure(null, null, x, y, z, yaw, headYaw, pitch, scale, presence, age, 0F, state, stateTicks, distSq, inWorldPass);
    }

    /**
     * Draws him without a body to ask about his pose.
     *
     * @param attack what he is gathering, if his state is {@link State#CHANNEL}
     */
    public static void drawFigure(@Nullable EntityEternity entity, @Nullable Attack attack, double x, double y, double z, float yaw,
                                  float headYaw, float pitch, float scale, float presence, float age, float partial, State state, float ticks,
                                  double distSq, boolean inWorldPass) {
        if (presence <= 0.01F) {
            return;
        }
        int layers = distSq > 300 * 300 ? 6 : distSq > 90 * 90 ? 10 : 15;
        float size = MODEL_SCALE * scale * (0.3F + 0.7F * presence);
        MODEL.pose(entity, state, attack, ticks, age, headYaw, pitch, partial);
        Minecraft mc = Minecraft.getMinecraft();

        GlSnapshot before = GlSnapshot.take();
        GlStateManager.disableFog();
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.rotate(180F - yaw, 0F, 1F, 0F);
        GlStateManager.scale(size, size, size);
        GlStateManager.scale(-1F, -1F, 1F);
        GlStateManager.translate(0F, -1.501F, 0F);
        captureMatrix();

        GlStateManager.disableLighting();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
        GlStateManager.enableCull();
        GlStateManager.cullFace(GlStateManager.CullFace.BACK);
        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
        GlStateManager.depthFunc(GL11.GL_LEQUAL);

        // His skin.
        mc.getTextureManager().bindTexture(SKIN);
        if (presence < 0.99F) {
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        }
        GlStateManager.color(1F, 1F, 1F, presence);
        MODEL.renderAll(S);

        // The stars moving inside him, over the stars painted on him.
        Cosmic.draw(() -> MODEL.renderBody(S), layers, 0F, 0.85F * presence, 0.8F, 0.6F, 1F, false);

        // The brightest stars in his skin flare and fade, half of them as the other half dims.
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        GlStateManager.depthMask(false);
        float twinkle = 0.5F + 0.5F * MathHelper.sin(age * 0.11F);
        float eyes = MODEL.glow();
        mc.getTextureManager().bindTexture(GLOW_A);
        GlStateManager.color(1F, 1F, 1F, (0.3F + 0.7F * twinkle) * presence);
        MODEL.renderAll(S);
        mc.getTextureManager().bindTexture(GLOW_B);
        GlStateManager.color(1F, 1F, 1F, (0.3F + 0.7F * (1F - twinkle)) * presence);
        MODEL.renderAll(S);

        // A dim violet glow standing out around his outline: larger copies of him, drawn from the inside only.
        float dying = state == State.DYING ? MathHelper.clamp(ticks / 400F, 0F, 1F) : 0F;
        float hurt = entity != null && entity.hurtTime > 0 ? (entity.hurtTime - partial) / 10F : 0F;
        GlStateManager.disableTexture2D();
        GlStateManager.cullFace(GlStateManager.CullFace.FRONT);
        float[][] shells = {{1.02F, 0.12F}};
        for (float[] shell : shells) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(0F, 0.6F, 0F);
            GlStateManager.scale(shell[0], shell[0], shell[0]);
            GlStateManager.translate(0F, -0.6F, 0F);
            float[] col = mix(mix(INDIGO, VIOLET, 0.4F), WHITE, Math.max(dying, hurt));
            GlStateManager.color(col[0], col[1], col[2], shell[1] * presence * (1F + hurt + dying * 2F));
            MODEL.renderAll(S);
            GlStateManager.popMatrix();
        }
        GlStateManager.cullFace(GlStateManager.CullFace.BACK);
        if (dying > 0F) {
            // Dying, he fills with light from inside.
            GlStateManager.color(1F, 0.92F, 1F, 0.6F * dying);
            MODEL.renderAll(S);
        }
        GlStateManager.color(1F, 1F, 1F, 1F);

        // Everything around him, in eye space, so it can always face the camera.
        Anchors at = new Anchors(eyes);
        GlStateManager.popMatrix();
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        GlStateManager.disableCull();
        WarlockFx.additive();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        eyes(at, presence, age);
        if (inWorldPass) {
            aura(at, presence, age);
            shards(at, presence * (1F - dying), age);
            dust(at, entity, presence, age);
            if (state == State.CHANNEL) {
                Attack gathering = entity != null ? entity.getAttack() : attack;
                if (gathering != null) {
                    attack(at, gathering, ticks, age);
                }
            }
        }
        GlStateManager.popMatrix();
        before.restore();
        GlStateManager.depthFunc(GL11.GL_LEQUAL);
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
        if (inWorldPass) {
            GlStateManager.enableFog();
        }
    }

    // ------------------------------------------------------------------ eye space

    private static void captureMatrix() {
        MATRIX.clear();
        GlStateManager.getFloat(GL11.GL_MODELVIEW_MATRIX, MATRIX);
        MATRIX.get(M);
        unit = (float) Math.sqrt(M[0] * M[0] + M[1] * M[1] + M[2] * M[2]) * S;
    }

    /** A point in model units, measured in his frame at his feet, where it is before the camera. */
    private static Vec3d eye(double x, double y, double z) {
        x *= S;
        y *= S;
        z *= S;
        return new Vec3d(M[0] * x + M[4] * y + M[8] * z + M[12], M[1] * x + M[5] * y + M[9] * z + M[13], M[2] * x + M[6] * y + M[10] * z + M[14]);
    }

    /** A direction in his frame, before the camera. */
    private static Vec3d eyeDir(double x, double y, double z) {
        return new Vec3d(M[0] * x + M[4] * y + M[8] * z, M[1] * x + M[5] * y + M[9] * z, M[2] * x + M[6] * y + M[10] * z).normalize();
    }

    /**
     * Where the parts of him that effects hang from are, before the camera, found by walking
     * the model's joints down to each one. Built while his matrix is still current.
     */
    private static final class Anchors {
        final Vec3d[] eye = new Vec3d[2], palm = new Vec3d[2], palmNormal = new Vec3d[2];
        final Vec3d[][] fingertip = new Vec3d[2][5];
        final Vec3d[] crestTip = new Vec3d[4];
        final Vec3d[] hem = new Vec3d[11];
        final Vec3d heart, head, up;

        Anchors(float glow) {
            ModelEternity m = MODEL;
            eye[0] = find(m.toHead(), -1.5F, -5F, -4.7F);
            eye[1] = find(m.toHead(), 1.5F, -5F, -4.7F);
            head = find(m.toHead(), 0F, -4F, -1F);
            heart = find(m.toChest(), 0F, -5F, 0F);
            for (int side = 0; side < 2; side++) {
                float s = side == 0 ? -1F : 1F;
                palm[side] = find(m.toHand(side), -s * 0.9F, 1.6F, 0F);
                palmNormal[side] = find(m.toHand(side), -s * 2F, 1.6F, 0F).subtract(palm[side]).normalize();
                for (int f = 0; f < 5; f++) {
                    fingertip[side][f] = find(m.toFinger(side, f), 0F, f == 4 ? 2F : 3F, 0F);
                }
                crestTip[side * 2] = find(m.toCrestTip(side, true), 0F, -2F, 0F);
                crestTip[side * 2 + 1] = find(m.toCrestTip(side, false), 0F, 2F, 0F);
            }
            for (int i = 0; i < 5; i++) {
                hem[i] = find(m.toHem(0), -8F + i * 4F, 6F, 0.5F);
            }
            for (int i = 0; i < 3; i++) {
                hem[5 + i] = find(m.toHem(1), -3F + i * 3F, 6F, 0.5F);
                hem[8 + i] = find(m.toHem(2), -3F + i * 3F, 6F, 0.5F);
            }
            up = eyeDir(0D, -1D, 0D);
        }

        private static Vec3d find(ModelRenderer[] chain, float x, float y, float z) {
            GlStateManager.pushMatrix();
            ModelEternity.locate(S, chain, x, y, z);
            MATRIX.clear();
            GlStateManager.getFloat(GL11.GL_MODELVIEW_MATRIX, MATRIX);
            Vec3d at = new Vec3d(MATRIX.get(12), MATRIX.get(13), MATRIX.get(14));
            GlStateManager.popMatrix();
            return at;
        }
    }

    // ------------------------------------------------------------------ what surrounds him

    /** His eyes: a white-gold core in a halo, and when he stares, light spiking out of them. */
    private static void eyes(Anchors at, float presence, float age) {
        float glow = MODEL.glow() * presence;
        float flicker = 0.88F + 0.12F * MathHelper.sin(age * 0.7F);
        float stare = MathHelper.clamp((MODEL.glow() - 0.85F) / 0.15F, 0F, 1F) * presence;
        for (int i = 0; i < 2; i++) {
            Vec3d c = at.eye[i];
            c = c.subtract(c.normalize().scale(0.4D * unit));
            WarlockFx.glow(c, 0.7D * unit * flicker, WHITE, glow);
            WarlockFx.glow(c, 2.2D * unit, GOLD, 0.45F * glow * flicker);
            if (stare > 0.01F) {
                WarlockFx.star(c, 0.3D * unit, (4.5D + 2D * flicker) * unit, 8, (long) (age / 3F) * 31L + i, age * 0.01F, GOLD, 0.5F * stare);
                WarlockFx.glow(c, 3.5D * unit, GOLD, 0.18F * stare);
            }
        }
    }

    /** A haze of dark violet light behind him, as if the space he is made of spilled a little past his edges. */
    private static void aura(Anchors at, float presence, float age) {
        Vec3d behind = at.heart.add(at.heart.normalize().scale(8D * unit));
        float breathe = 0.85F + 0.15F * MathHelper.sin(age * 0.04F);
        WarlockFx.glow(behind, 30D * unit * breathe, INDIGO, 0.13F * presence);
        WarlockFx.glow(behind, 16D * unit, VIOLET, 0.07F * presence);
        WarlockFx.glow(at.head.add(at.head.normalize().scale(5D * unit)), 11D * unit, ICE, 0.05F * presence);
    }

    /**
     * Splinters of reality turning slowly in the air around him, in pale colours, each with a
     * short crossbar: the world where it is coming apart near him.
     */
    private static void shards(Anchors at, float presence, float age) {
        if (presence <= 0.01F) {
            return;
        }
        for (int i = 0; i < 11; i++) {
            double h1 = hash(i * 11 + 1), h2 = hash(i * 11 + 2), h3 = hash(i * 11 + 3);
            double h4 = hash(i * 11 + 4), h5 = hash(i * 11 + 5), h6 = hash(i * 11 + 6);
            double cycle = age / (260D + 140D * h6) + h5;
            float fade = (float) Math.pow(Math.sin((cycle - Math.floor(cycle)) * Math.PI), 0.7D);
            double radius = 14D + 11D * h1;
            double angle = h2 * Math.PI * 2D + age * (0.003D + 0.004D * h3) * (i % 2 == 0 ? 1D : -1D);
            double cy = -26D + 44D * h4 + Math.sin(age * 0.012D + i) * 2D;
            double cx = Math.cos(angle) * radius, cz = Math.sin(angle) * radius;
            double tumbleA = age * 0.017D * (1D + h3) + i, tumbleB = age * 0.011D * (1D + h1) + i * 2D;
            double dx = Math.cos(tumbleA) * Math.cos(tumbleB), dy = Math.sin(tumbleB), dz = Math.sin(tumbleA) * Math.cos(tumbleB);
            double half = (2.5D + 4.5D * h5) / 2D;
            float[] col = SHARD_COLOURS[i % SHARD_COLOURS.length];
            Vec3d a = eye(cx - dx * half, cy - dy * half, cz - dz * half), b = eye(cx + dx * half, cy + dy * half, cz + dz * half);
            WarlockFx.strip(a, b, 0.18D * unit, col, 0.6F * fade * presence);
            // The crossbar, a little way along.
            double mx = cx + dx * half * 0.4D, my = cy + dy * half * 0.4D, mz = cz + dz * half * 0.4D;
            double ax = -dz, ay = dx * 0.3D, az = dx, cl = 0.8D + 0.8D * h6;
            WarlockFx.strip(eye(mx - ax * cl, my - ay * cl, mz - az * cl), eye(mx + ax * cl, my + ay * cl, mz + az * cl), 0.13D * unit, col,
                    0.45F * fade * presence);
        }
    }

    /** Stardust shed from his fingertips, the tips of his crests and the hem of his cloak, drifting up and trailing away behind him. */
    private static void dust(Anchors at, @Nullable EntityEternity entity, float presence, float age) {
        double[] wind = entity != null ? entity.wind : new double[3];
        Vec3d drift = eyeDir(wind[0], wind[1], wind[2]).scale(Math.sqrt(wind[0] * wind[0] + wind[1] * wind[1] + wind[2] * wind[2]));
        Vec3d behind = eyeDir(0D, 0D, 1D);
        for (int i = 0; i < 40; i++) {
            double h = hash(i * 7 + 101), h2 = hash(i * 7 + 102), h3 = hash(i * 7 + 103);
            double phase = age * (0.008D + 0.01D * h2) + h;
            phase -= Math.floor(phase);
            Vec3d from;
            int pick = i % 4;
            if (pick == 0) {
                from = at.crestTip[i / 4 % 4];
            } else if (pick == 1) {
                from = at.fingertip[i / 4 % 2][i / 8 % 5];
            } else {
                from = at.hem[(int) (h3 * at.hem.length) % at.hem.length];
            }
            Vec3d p = from.add(drift.scale(10D * unit * phase)).add(at.up.scale(7D * unit * phase))
                    .add(behind.scale(2D * unit * phase)).add(new Vec3d(h2 - 0.5D, 0D, h3 - 0.5D).scale(4D * unit * phase));
            float a = (float) Math.sin(phase * Math.PI) * presence;
            float[] col = i % 3 == 0 ? ICE : i % 3 == 1 ? VIOLET : GOLD;
            WarlockFx.glow(p, (0.4D + 0.35D * h) * unit, col, 0.55F * a);
            WarlockFx.glow(p, 0.14D * unit, WHITE, 0.8F * a);
        }
    }

    // ------------------------------------------------------------------ his attacks, in his hands

    private static void attack(Anchors at, Attack attack, float t, float age) {
        float length = attack.ticks;
        switch (attack) {
            case UNMAKE:
                unmake(at, t, length, age);
                break;
            case SINGULARITY:
                singularity(at, t, length, age);
                break;
            case LANCES:
                lances(at, t, length, age);
                break;
            case STARFALL:
                starfall(at, t, length, age);
                break;
            case SHATTER:
                shatter(at, t, length, age);
                break;
        }
    }

    /** Unmake: light gathering in his drawn-back hand, a ring closing in on it, and a flash as it reaches you. */
    private static void unmake(Anchors at, float t, float length, float age) {
        float draw = EternityPose.smooth(t / (length * 0.55F));
        float reach = EternityPose.smooth((t - length * 0.58F) / 3F);
        Vec3d palm = at.palm[0];
        float flicker = 0.85F + 0.15F * MathHelper.sin(age * 2.3F);
        WarlockFx.glow(palm, (1.5D + 3.5D * draw) * unit * flicker, VIOLET, 0.35F * draw);
        WarlockFx.glow(palm, (0.6D + 1.4D * draw) * unit, WHITE, 0.85F * draw);
        WarlockFx.ring(palm, at.palmNormal[0], (7D - 6D * draw) * unit, 0.5D * unit, VIOLET, 0.55F * draw * (1F - reach));
        WarlockFx.ring(palm, at.palmNormal[0], (4.5D - 3.8D * draw) * unit, 0.3D * unit, WHITE, 0.4F * draw * (1F - reach));
        if (reach > 0F) {
            WarlockFx.star(palm, 0.5D * unit, 9D * unit * reach, 14, (long) age, age * 0.05F, WHITE, 0.6F * (1F - reach * 0.6F));
            WarlockFx.glow(palm, 7D * unit, VIOLET, 0.3F * (1F - reach * 0.5F));
        }
    }

    /**
     * Singularity: a black star between his cupped hands, swelling as they part, a ring of fire
     * turning round it and light streaming into it from all around.
     */
    private static void singularity(Anchors at, float t, float length, float age) {
        float grow = EternityPose.smooth((t - 6F) / (length - 14F));
        float send = EternityPose.smooth((t - length + 4F) / 4F);
        Vec3d c = at.palm[0].add(at.palm[1]).scale(0.5D);
        double r = (0.5D + 2.8D * grow) * unit * (1D - 0.7D * send);
        float strength = grow * (1F - send);
        // Light falling in.
        for (int i = 0; i < 12; i++) {
            double ph = age * 0.045D + hash(i * 3 + 9);
            ph -= Math.floor(ph);
            Vec3d dir = new Vec3d(hash(i * 5 + 1) - 0.5D, hash(i * 5 + 2) - 0.5D, hash(i * 5 + 3) - 0.5D).normalize();
            Vec3d a = c.add(dir.scale(r + 16D * unit * (1D - ph)));
            Vec3d b = a.subtract(dir.scale(3.5D * unit));
            WarlockFx.strip(a, b, 0.14D * unit, 0.14D * unit, VIOLET, WHITE, 0F, 0.6F * strength * (float) Math.sin(ph * Math.PI));
        }
        Vec3d tilt = c.normalize().add(at.up.scale(0.8D)).normalize();
        WarlockFx.ring(c, tilt, r * 1.35D, r * 0.35D, EMBER, 0.6F * strength);
        WarlockFx.ring(c, tilt, r * 1.9D, r * 0.25D, VIOLET, 0.4F * strength);
        WarlockFx.glow(c, r * 3D, VIOLET, 0.25F * strength);
        // The dark itself, drawn over the light.
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        WarlockFx.glow(c, r * 1.25D, BLACK, 0.97F * strength);
        WarlockFx.glow(c, r * 0.9D, BLACK, strength);
        WarlockFx.additive();
        WarlockFx.ring(c, c.normalize(), r * 0.95D, r * 0.12D, WHITE, 0.5F * strength);
    }

    /** Void lances: dark spikes growing out of the air around his raised hand, edged in violet. */
    private static void lances(Anchors at, float t, float length, float age) {
        float raise = EternityPose.smooth(t / (length * 0.45F));
        float cast = EternityPose.smooth((t - length * 0.62F) / 3F);
        float grow = raise * (1F - cast);
        Vec3d palm = at.palm[0];
        WarlockFx.glow(palm, (1.5D + 2D * raise) * unit, VIOLET, 0.45F * raise);
        WarlockFx.glow(palm, 0.8D * unit, WHITE, 0.7F * raise);
        for (int i = 0; i < 7; i++) {
            double a = Math.PI * 2D * i / 7D + age * 0.01D;
            Vec3d dir = eyeDir(Math.cos(a) * 0.9D, -0.7D - 0.3D * Math.sin(a * 2D), Math.sin(a) * 0.9D);
            Vec3d from = palm.add(dir.scale(1.5D * unit));
            Vec3d to = palm.add(dir.scale((2D + 9D * grow) * unit));
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            WarlockFx.strip(from, to, 0.55D * unit, 0.05D * unit, BLACK, BLACK, 0.85F * grow, 0.4F * grow);
            WarlockFx.additive();
            WarlockFx.strip(from, to, 0.2D * unit, 0.02D * unit, VIOLET, WHITE, 0.6F * grow, 0.3F * grow);
        }
        if (cast > 0F) {
            WarlockFx.glow(palm, 6D * unit, VIOLET, 0.35F * (1F - cast));
        }
    }

    /** Starfall: his palms blaze, and columns of light climb out of them into the sky, sparks rising up them. */
    private static void starfall(Anchors at, float t, float length, float age) {
        float rise = EternityPose.smooth(t / (length * 0.62F));
        float call = EternityPose.smooth((t - length * 0.72F) / (length * 0.22F));
        for (int side = 0; side < 2; side++) {
            Vec3d palm = at.palm[side];
            float flicker = 0.85F + 0.15F * MathHelper.sin(age * 1.9F + side);
            WarlockFx.glow(palm, (1D + 2.5D * rise) * unit * flicker, GOLD, 0.5F * rise);
            WarlockFx.glow(palm, (0.5D + 0.8D * rise) * unit, WHITE, 0.9F * rise);
            WarlockFx.star(palm, 0.3D * unit, (2D + 4D * rise) * unit, 10, (long) (age / 2F) * 7L + side, age * 0.02F, WHITE, 0.5F * rise);
            float column = rise * (1F - call);
            WarlockFx.strip(palm, palm.add(at.up.scale(70D * unit)), 0.9D * unit, 0.25D * unit, WHITE, GOLD, 0.45F * column, 0F);
            for (int k = 0; k < 6; k++) {
                double ph = age * 0.03D + k / 6D + side * 0.5D;
                ph -= Math.floor(ph);
                WarlockFx.glow(palm.add(at.up.scale(ph * 60D * unit)), 0.6D * unit, WHITE, 0.7F * column * (float) (1D - ph));
            }
        }
    }

    /** Shatter: his locked fists burn like a forge, and flash white as they come down. */
    private static void shatter(Anchors at, float t, float length, float age) {
        float raise = EternityPose.smooth(t / (length * 0.47F));
        float smash = MathHelper.clamp((t - length * 0.57F) / 3F, 0F, 1F);
        for (int side = 0; side < 2; side++) {
            Vec3d fist = at.palm[side];
            float flicker = 0.8F + 0.2F * MathHelper.sin(age * 2.7F + side * 2F);
            WarlockFx.glow(fist, (1.2D + 2.6D * raise) * unit * flicker, EMBER, 0.5F * raise);
            WarlockFx.glow(fist, (0.6D + 1D * raise) * unit, WHITE, 0.8F * raise);
            WarlockFx.star(fist, 0.4D * unit, (2.5D + 3D * raise) * unit * flicker, 12, (long) (age / 2F) * 13L + side, age * 0.03F, EMBER, 0.45F * raise);
            if (smash > 0F && smash < 1F) {
                WarlockFx.glow(fist, (4D + 14D * smash) * unit, WHITE, 0.6F * (1F - smash));
            }
        }
    }

    private static double hash(int n) {
        long x = n * 0x9E3779B97F4A7C15L;
        x ^= x >>> 31;
        x *= 0xBF58476D1CE4E5B9L;
        x ^= x >>> 29;
        return (x >>> 11) / (double) (1L << 53);
    }

    private static float[] mix(float[] a, float[] b, float k) {
        return new float[]{a[0] + (b[0] - a[0]) * k, a[1] + (b[1] - a[1]) * k, a[2] + (b[2] - a[2]) * k};
    }

    // ------------------------------------------------------------------ death

    /** Shafts of light tearing out of his chest, the way the dragon dies, but more. */
    private static void drawDeathLight(double x, double y, double z, float scale, float progress) {
        float f = MathHelper.clamp(progress, 0F, 1F);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        float size = 0.5F * scale * (1F + f * 2F);
        GlStateManager.scale(size, size, size);
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableFog();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        GlStateManager.disableAlpha();
        GlStateManager.enableCull();
        GlStateManager.depthMask(false);
        RAYS.setSeed(432L);
        int rays = (int) ((f + f * f) / 2F * 90F) + 4;
        for (int i = 0; i < rays; i++) {
            GlStateManager.rotate(RAYS.nextFloat() * 360F, 1F, 0F, 0F);
            GlStateManager.rotate(RAYS.nextFloat() * 360F, 0F, 1F, 0F);
            GlStateManager.rotate(RAYS.nextFloat() * 360F, 0F, 0F, 1F);
            GlStateManager.rotate(RAYS.nextFloat() * 360F, 1F, 0F, 0F);
            GlStateManager.rotate(RAYS.nextFloat() * 360F, 0F, 1F, 0F);
            GlStateManager.rotate(RAYS.nextFloat() * 360F + f * 90F, 0F, 0F, 1F);
            float length = RAYS.nextFloat() * 20F + 5F + f * 14F;
            float width = RAYS.nextFloat() * 2F + 1F + f * 2F;
            buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
            buffer.pos(0D, 0D, 0D).color(255, 255, 255, (int) (255F * (1F - f * 0.4F))).endVertex();
            buffer.pos(-0.866D * width, length, -0.5F * width).color(150, 40, 255, 0).endVertex();
            buffer.pos(0.866D * width, length, -0.5F * width).color(150, 40, 255, 0).endVertex();
            buffer.pos(0D, length, width).color(150, 40, 255, 0).endVertex();
            buffer.pos(-0.866D * width, length, -0.5F * width).color(150, 40, 255, 0).endVertex();
            tessellator.draw();
        }
        GlStateManager.depthMask(true);
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
        GlStateManager.enableFog();
        GlStateManager.popMatrix();
    }

    private static float interpolate(float prev, float now, float partialTicks) {
        return prev + MathHelper.wrapDegrees(now - prev) * partialTicks;
    }

    @Nullable
    @Override
    protected ResourceLocation getEntityTexture(EntityEternity entity) {
        return null;
    }
}
