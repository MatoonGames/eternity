package com.matoon.eternity.client.render;

import com.matoon.eternity.Eternity;
import com.matoon.eternity.entity.EntityAdamWarlock;
import com.matoon.eternity.entity.EntityAdamWarlock.State;
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

import java.util.Random;

/**
 * Adam Warlock as everyone sees him. At rest he is a man in crimson armour; as his power
 * rises his skin turns to burnished gold, his eyes and the gem on his brow blaze, the seams
 * of his armour light, and a corona of golden fire stands out around him. Streaking through
 * the air he leans into his flight, banks through his turns, and leaves a trail of fire and
 * golden afterimages of himself behind.
 */
@SideOnly(Side.CLIENT)
public class RenderAdamWarlock extends RenderLiving<EntityAdamWarlock> {

    private static final ResourceLocation SKIN = new ResourceLocation(Eternity.MODID, "textures/entity/adam_warlock.png");
    private static final ResourceLocation GOLDEN = new ResourceLocation(Eternity.MODID, "textures/entity/adam_warlock_gold.png");
    private static final ResourceLocation GLOW = new ResourceLocation(Eternity.MODID, "textures/entity/adam_warlock_glow.png");

    /** His middle, in blocks above his feet: where he turns about when he leans or banks. */
    private static final float CENTRE = 1.0F;
    /** How many ticks back his trail of fire reaches. */
    private static final int TRAIL_POINTS = 11;

    private float partial;

    public RenderAdamWarlock(RenderManager manager) {
        super(manager, new ModelAdamWarlock(), 0.45F);
        addLayer(new Fists());
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityAdamWarlock entity) {
        return SKIN;
    }

    private ModelAdamWarlock model() {
        return (ModelAdamWarlock) mainModel;
    }

    /** Leaning into his flight and banking through his turns, both about his middle. */
    @Override
    protected void applyRotations(EntityAdamWarlock entity, float ageInTicks, float rotationYaw, float partialTicks) {
        GlStateManager.rotate(180F - rotationYaw, 0F, 1F, 0F);
        float pitch = entity.prevBodyPitch + (entity.bodyPitch - entity.prevBodyPitch) * partialTicks;
        float roll = entity.prevBodyRoll + (entity.bodyRoll - entity.prevBodyRoll) * partialTicks;
        GlStateManager.translate(0F, CENTRE, 0F);
        GlStateManager.rotate(-pitch, 1F, 0F, 0F);
        GlStateManager.rotate(roll, 0F, 0F, 1F);
        GlStateManager.translate(0F, -CENTRE, 0F);
    }

    @Override
    public void doRender(EntityAdamWarlock entity, double x, double y, double z, float entityYaw, float partialTicks) {
        partial = partialTicks;
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
        if (entity.isInvisible()) {
            return;
        }
        float power = entity.prevPower + (entity.power - entity.prevPower) * partialTicks;
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

        afterimages(entity, at, base, partialTicks);
        WarlockFx.additive();
        trail(entity, at, base, partialTicks);
        corona(entity, at, power);
        if (entity.getState() == State.BEAM) {
            orb(entity, at, partialTicks);
        }
        if (entity.getState() == State.COCOON) {
            cocoon(entity, at, partialTicks);
        }

        GlStateManager.popMatrix();
        before.restore();
    }

    /**
     * Over the plain skin: the gold coming into it as his power rises, then his eyes, gem and
     * armour seams lit from within, then shells of golden light standing out from his body.
     */
    @Override
    protected void renderModel(EntityAdamWarlock entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
                               float headPitch, float scaleFactor) {
        super.renderModel(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor);
        if (entity.isInvisible()) {
            return;
        }
        float power = entity.prevPower + (entity.power - entity.prevPower) * partial;
        float gold = ModelAdamWarlock.smooth((power - 0.42F) / 0.5F);
        GlSnapshot before = GlSnapshot.take();
        GlStateManager.enableBlend();
        GlStateManager.depthFunc(GL11.GL_LEQUAL);
        if (gold > 0.01F) {
            bindTexture(GOLDEN);
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            GlStateManager.color(1F, 1F, 1F, gold);
            model().renderPose(scaleFactor);
        }

        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
        bindTexture(GLOW);
        GlStateManager.color(1F, 1F, 1F, 0.3F + 0.7F * Math.min(1F, power));
        model().renderPose(scaleFactor);

        // The aura: larger copies of him drawn from the inside only, so his own body hides all of
        // them but the rim standing out around his silhouette.
        float aura = ModelAdamWarlock.smooth((power - 0.25F) / 0.6F) * Math.min(1.25F, power);
        if (aura > 0.01F) {
            bindTexture(GOLDEN);
            GlStateManager.enableCull();
            GlStateManager.cullFace(GlStateManager.CullFace.FRONT);
            float[] alphas = {0.75F, 0.4F, 0.18F};
            float[][] colours = {{1F, 0.92F, 0.6F}, {1F, 0.72F, 0.28F}, {1F, 0.45F, 0.1F}};
            for (int i = 0; i < alphas.length; i++) {
                float s = 1.05F + i * 0.06F + 0.012F * MathHelper.sin(ageInTicks * 0.9F + i * 2.1F) * (1 + i);
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

    /** Golden echoes of him where he was a moment ago, fading fast. Only when he is really moving. */
    private void afterimages(EntityAdamWarlock entity, Vec3d at, Vec3d base, float partialTicks) {
        if (entity.trailCount < 3) {
            return;
        }
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        GlStateManager.depthMask(false);
        GlStateManager.enableCull();
        bindTexture(GOLDEN);
        for (int k = 1; k <= 7 && k < entity.trailCount; k += 2) {
            double[] h = entity.trail[k];
            float heat = (float) h[6];
            if (heat < 0.05F) {
                continue;
            }
            Vec3d where = new Vec3d(at.x + h[0] - base.x, at.y + h[1] - base.y + CENTRE, at.z + h[2] - base.z);
            float alpha = heat * 0.38F * (1F - (k - 1) / 8F) * nearFade(where);
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
            GlStateManager.scale(-1F, -1F, 1F);
            GlStateManager.translate(0F, -1.501F, 0F);
            GlStateManager.color(1F, 0.72F, 0.3F, alpha);
            model().renderPose(0.0625F);
            GlStateManager.popMatrix();
        }
        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.disableCull();
    }

    /** The fire he leaves behind him: a white-hot core in a wider amber flame, narrowing and cooling as it trails off. */
    private void trail(EntityAdamWarlock entity, Vec3d at, Vec3d base, float partialTicks) {
        int count = entity.trailCount;
        if (count < 3) {
            return;
        }
        // From where he is drawn now, back through where he has been, smoothed into a curve.
        int n = Math.min(count, TRAIL_POINTS);
        Vec3d[] points = new Vec3d[n];
        float[] heat = new float[n];
        points[0] = at.add(0D, CENTRE, 0D);
        heat[0] = (float) entity.trail[0][6];
        for (int i = 1; i < n; i++) {
            double[] h = entity.trail[i];
            points[i] = new Vec3d(at.x + h[0] - base.x, at.y + h[1] - base.y + CENTRE, at.z + h[2] - base.z);
            heat[i] = (float) h[6];
        }
        Vec3d last = null;
        float lastFade = 0F;
        double lastWidth = 0D;
        int steps = 3;
        for (int i = 0; i < n - 1; i++) {
            Vec3d p0 = points[Math.max(0, i - 1)], p1 = points[i], p2 = points[i + 1], p3 = points[Math.min(n - 1, i + 2)];
            for (int s = 0; s <= steps; s++) {
                if (i > 0 && s == 0) {
                    continue;
                }
                float f = s / (float) steps;
                Vec3d p = catmull(p0, p1, p2, p3, f);
                float along = (i + f) / (n - 1);
                float h = heat[i] + (heat[i + 1] - heat[i]) * f;
                // Fading as it trails off, and right up close to your eyes, where it would only blind you.
                float fade = h * (1F - along) * (1F - along) * nearFade(p);
                double width = 0.42D * (1D - along * 0.7D) * Math.min(1F, h * 1.5F);
                if (last != null && (fade > 0.01F || lastFade > 0.01F)) {
                    WarlockFx.strip(last, p, lastWidth * 1.7D, width * 1.7D, WarlockFx.AMBER, WarlockFx.EMBER, lastFade * 0.28F, fade * 0.28F);
                    WarlockFx.strip(last, p, lastWidth, width, WarlockFx.GOLD, WarlockFx.AMBER, lastFade * 0.5F, fade * 0.5F);
                    WarlockFx.strip(last, p, lastWidth * 0.28D, width * 0.28D, WarlockFx.WHITE, WarlockFx.GOLD, lastFade * 0.75F, fade * 0.75F);
                }
                last = p;
                lastFade = fade;
                lastWidth = width;
            }
        }
    }

    /** 0 at the camera, 1 from four blocks out. */
    private static float nearFade(Vec3d relative) {
        return (float) MathHelper.clamp((relative.length() - 1D) / 3D, 0D, 1D);
    }

    private static Vec3d catmull(Vec3d p0, Vec3d p1, Vec3d p2, Vec3d p3, float t) {
        float t2 = t * t, t3 = t2 * t;
        return p1.scale(2D).add(p2.subtract(p0).scale(t)).add(p0.scale(2D).subtract(p1.scale(5D)).add(p2.scale(4D)).subtract(p3).scale(t2))
                .add(p1.scale(3D).subtract(p0).subtract(p2.scale(3D)).add(p3).scale(t3)).scale(0.5D);
    }

    /**
     * The light behind him, as if he were lit from within: a soft backglow, and while he
     * gathers his power without going anywhere, tongues of golden flame that never hold still.
     */
    private void corona(EntityAdamWarlock entity, Vec3d at, float power) {
        float strength = ModelAdamWarlock.smooth((power - 0.3F) / 0.7F) * Math.min(1.3F, power);
        if (strength < 0.02F || entity.getState() == State.COCOON) {
            return;
        }
        Vec3d c = at.add(0D, CENTRE, 0D);
        Vec3d behind = c.add(c.normalize().scale(0.7D));
        WarlockFx.glow(behind, 1.3D + 0.5D * strength, WarlockFx.GOLD, 0.16F * strength);
        WarlockFx.glow(behind, 2.2D * strength, WarlockFx.AMBER, 0.07F * strength);
        if (entity.isStreaking()) {
            return;
        }
        float time = entity.ticksExisted + partial;
        long flicker = entity.ticksExisted / 2;
        float flames = ModelAdamWarlock.smooth((strength - 0.55F) / 0.4F);
        WarlockFx.star(behind, 0.45D, 1.05D + 0.45D * strength, 18, flicker * 7919L + entity.getEntityId(), time * 0.02F,
                WarlockFx.AMBER, 0.28F * flames);
        WarlockFx.star(behind, 0.4D, 0.85D + 0.3D * strength, 12, flicker * 104729L, -time * 0.03F, WarlockFx.GOLD, 0.22F * flames);
    }

    /** The sun he cups between his palms before the beam, and holds there while it pours out. */
    private void orb(EntityAdamWarlock entity, Vec3d at, float partialTicks) {
        float t = entity.clientStateTicks + partialTicks;
        int charge = EntityAdamWarlock.BEAM_CHARGE;
        float grow = t < charge ? ModelAdamWarlock.smooth(t / charge) : 1F - ModelAdamWarlock.smooth((t - charge - EntityAdamWarlock.BEAM_FIRE) / 6F);
        if (grow <= 0.01F) {
            return;
        }
        float yaw = (float) Math.toRadians(entity.prevRotationYaw + MathHelper.wrapDegrees(entity.rotationYaw - entity.prevRotationYaw) * partialTicks);
        float pitch = (float) Math.toRadians(entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks);
        Vec3d look = new Vec3d(-MathHelper.sin(yaw) * MathHelper.cos(t < charge ? 0F : pitch), -MathHelper.sin(t < charge ? 0F : pitch),
                MathHelper.cos(yaw) * MathHelper.cos(t < charge ? 0F : pitch));
        Vec3d c = at.add(0D, 1.3D, 0D).add(look.scale(t < charge ? 0.5D : 0.95D));
        float flicker = 0.85F + 0.15F * MathHelper.sin(t * 2.7F);
        double r = (0.08D + 0.22D * grow) * flicker;
        WarlockFx.glow(c, r * 2.6D, WarlockFx.AMBER, 0.3F * grow);
        WarlockFx.glow(c, r * 1.6D, WarlockFx.GOLD, 0.6F * grow);
        WarlockFx.glow(c, r, WarlockFx.WHITE, grow);
        // Crackling arcs of quantum energy, never the same twice.
        Random rand = new Random(entity.ticksExisted * 31L);
        for (int i = 0; i < 4; i++) {
            Vec3d d = new Vec3d(rand.nextGaussian(), rand.nextGaussian(), rand.nextGaussian()).normalize();
            Vec3d last = c;
            for (int s = 1; s <= 4; s++) {
                Vec3d next = c.add(d.scale(r * (0.8D + s * 0.6D))).add(rand.nextGaussian() * 0.08D, rand.nextGaussian() * 0.08D, rand.nextGaussian() * 0.08D);
                WarlockFx.strip(last, next, 0.04D, WarlockFx.WHITE, 0.8F * grow);
                last = next;
            }
        }
    }

    /**
     * His cocoon: gold threads winding up around him, then a shell of amber light closing
     * over them, translucent enough to see him curled inside, brightest at its rim, beating
     * like a heart. Cracks open in it as it is struck.
     */
    private void cocoon(EntityAdamWarlock entity, Vec3d at, float partialTicks) {
        float t = entity.clientStateTicks + partialTicks;
        float threads = ModelAdamWarlock.smooth((t - 4F) / 18F);
        float shell = ModelAdamWarlock.smooth((t - 16F) / 14F);
        float integrity = entity.getCocoon();
        // Around him as he hangs curled up, a little forward of his feet.
        float yaw = (float) Math.toRadians(entity.rotationYaw);
        Vec3d c = at.add(-MathHelper.sin(yaw) * 0.15D, 1.2D, MathHelper.cos(yaw) * 0.15D);
        double rx = 1.0D, ry = 1.35D;
        // The threads.
        for (int k = 0; k < 7; k++) {
            Vec3d last = null;
            int steps = 28;
            for (int s = 0; s <= steps * threads; s++) {
                float f = s / (float) steps;
                double y = (f * 2D - 1D) * ry;
                double ring = Math.sqrt(Math.max(0D, 1D - (y * y) / (ry * ry))) * rx * 1.04D;
                double a = f * Math.PI * 5D + k * Math.PI * 2D / 7D + t * 0.02D;
                Vec3d p = c.add(Math.cos(a) * ring, y, Math.sin(a) * ring);
                if (last != null) {
                    WarlockFx.strip(last, p, 0.05D, WarlockFx.WHITE, 0.7F * (1F - shell * 0.6F));
                }
                last = p;
            }
        }
        if (shell <= 0.01F) {
            return;
        }
        float beat = (float) Math.pow(Math.max(0F, MathHelper.sin(t * (float) Math.PI * 2F / 24F)), 10D);
        // A translucent shell first, so he shows through it dimly...
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        ellipsoid(c, rx, ry, t, new float[]{0.85F, 0.5F, 0.12F}, 0.45F * shell, false);
        // ...then the light in it, brightest toward the rim.
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        ellipsoid(c, rx * 1.01D, ry * 1.01D, t, WarlockFx.GOLD, (0.5F + 0.5F * beat) * shell, true);
        WarlockFx.glow(c, 2.4D + beat * 0.8D, WarlockFx.GOLD, (0.25F + 0.3F * beat) * shell);
        // The cracks, white-hot, more of them the closer it is to breaking.
        int cracks = (int) ((1F - integrity) * 14F);
        Random rand = new Random(entity.getEntityId() * 977L);
        for (int k = 0; k < cracks; k++) {
            double theta = rand.nextDouble() * Math.PI * 2D, phi = (rand.nextDouble() - 0.5D) * 2.4D;
            Vec3d last = null;
            for (int s = 0; s < 5; s++) {
                theta += (rand.nextDouble() - 0.5D) * 0.5D;
                phi += (rand.nextDouble() - 0.5D) * 0.45D;
                Vec3d p = c.add(Math.cos(theta) * Math.cos(phi) * rx * 1.03D, Math.sin(phi) * ry * 1.03D, Math.sin(theta) * Math.cos(phi) * rx * 1.03D);
                if (last != null) {
                    WarlockFx.strip(last, p, 0.06D, WarlockFx.WHITE, 0.95F * shell);
                }
                last = p;
            }
        }
    }

    private static void ellipsoid(Vec3d c, double rx, double ry, float t, float[] col, float alpha, boolean rim) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        int stacks = 14, slices = 20;
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < stacks; i++) {
            for (int j = 0; j < slices; j++) {
                for (int[] corner : new int[][]{{0, 0}, {0, 1}, {1, 1}, {1, 0}}) {
                    double phi = Math.PI * (i + corner[0]) / stacks - Math.PI / 2D;
                    double theta = Math.PI * 2D * (j + corner[1]) / slices;
                    double nx = Math.cos(phi) * Math.cos(theta), ny = Math.sin(phi), nz = Math.cos(phi) * Math.sin(theta);
                    Vec3d p = c.add(nx * rx, ny * ry, nz * rx);
                    float a = alpha;
                    if (rim) {
                        Vec3d n = new Vec3d(nx / rx, ny / ry, nz / rx).normalize();
                        double facing = Math.abs(n.dotProduct(p.normalize()));
                        float band = 0.75F + 0.25F * MathHelper.sin((float) (ny * 9D) - t * 0.15F);
                        a = alpha * (float) Math.pow(1D - facing, 1.6D) * band;
                    }
                    buffer.pos(p.x, p.y, p.z).color(col[0], col[1], col[2], a).endVertex();
                }
            }
        }
        tessellator.draw();
    }

    /** His fists burn when he means to use them: the leading one in a fly-by, both in a dive, his palms in a volley. */
    private class Fists implements LayerRenderer<EntityAdamWarlock> {
        @Override
        public void doRenderLayer(EntityAdamWarlock entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
                                  float netHeadYaw, float headPitch, float scale) {
            float t = entity.clientStateTicks + partialTicks;
            float right, left;
            switch (entity.getState()) {
                case WINDUP:
                    right = ModelAdamWarlock.smooth(t / 10F);
                    left = 0.2F;
                    break;
                case FLYBY:
                case ARRIVE:
                    right = entity.isStreaking() ? 1F : 0.2F;
                    left = 0F;
                    break;
                case DIVE:
                    right = left = 1F;
                    break;
                case RISE:
                    right = t < EntityAdamWarlock.RISE_CLIMB ? 0.8F : ModelAdamWarlock.smooth((t - EntityAdamWarlock.RISE_TICKS + 6F) / 5F);
                    left = Math.max(0F, ModelAdamWarlock.smooth((t - EntityAdamWarlock.RISE_TICKS + 6F) / 5F));
                    break;
                case COMBO:
                    right = left = 0.45F;
                    break;
                case VOLLEY:
                    right = left = 0.6F + 0.4F * MathHelper.sin(t * 0.6F);
                    break;
                case EMERGE:
                    right = left = 1F - ModelAdamWarlock.smooth((t - 25F) / 20F);
                    break;
                default:
                    right = left = entity.isEvolved() ? 0.25F : 0F;
                    break;
            }
            if (right <= 0.02F && left <= 0.02F) {
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
            float flicker = 0.85F + 0.15F * MathHelper.sin(ageInTicks * 2.1F);
            for (int side = 0; side < 2; side++) {
                float strength = side == 0 ? right : left;
                if (strength <= 0.02F) {
                    continue;
                }
                GlStateManager.pushMatrix();
                model().toFist(side == 0, scale);
                star(0.12F + 0.14F * strength * flicker, WarlockFx.WHITE, 0.85F * strength);
                star(0.3F + 0.25F * strength * flicker, WarlockFx.GOLD, 0.35F * strength);
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
