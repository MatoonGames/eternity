package com.matoon.eternity.client.render;

import com.matoon.eternity.Eternity;
import com.matoon.eternity.world.OdinsVault;
import com.matoon.eternity.world.OdinsVault.Relic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.Random;

/**
 * The treasures of Odin's Vault, drawn where {@link OdinsVault.Relic} stands them, lit by the
 * vault's own lamps. What burns or shines in them is drawn again over the top at full brightness:
 * the stones of the fake gauntlet, the embers in the chalice, the runes of the Tablet, the eye in
 * the orb, the fire in the Crown's cracks, the cold heart of the Casket. And above them what they
 * give off: the Eternal Flame, the Crown's black fire, the Casket's freezing mist, the shimmer of
 * the Tuning Fork as it rings. The Warlock's Eye turns to watch you.
 */
@SideOnly(Side.CLIENT)
public final class VaultTreasures {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Eternity.MODID, "textures/entity/vault_relics.png");
    private static final ResourceLocation GLOW = new ResourceLocation(Eternity.MODID, "textures/entity/vault_relics_glow.png");
    private static final float PX = 0.0625F;

    private static final ModelVaultRelics MODEL = new ModelVaultRelics();
    private static float eyeYaw, eyePitch, prevEyeYaw, prevEyePitch;

    private VaultTreasures() {
    }

    /** Turns the Warlock's Eye a little further toward whoever is nearest it; every tick. */
    public static void tick(Minecraft mc) {
        prevEyeYaw = eyeYaw;
        prevEyePitch = eyePitch;
        Entity viewer = mc.player;
        if (viewer == null) {
            return;
        }
        Vec3d orb = at(Relic.WARLOCKS_EYE, 16.5D);
        Vec3d to = viewer.getPositionEyes(1F).subtract(orb);
        double theta = Math.toRadians(Relic.WARLOCKS_EYE.yaw());
        double lx = to.x * Math.cos(theta) - to.z * Math.sin(theta);
        double lz = to.x * Math.sin(theta) + to.z * Math.cos(theta);
        double mx = -lx, my = -to.y, mz = lz;
        float wantYaw = (float) Math.atan2(-mx, -mz);
        float wantPitch = (float) Math.atan2(my, Math.sqrt(mx * mx + mz * mz));
        boolean near = to.length() < 28D;
        // It is in no hurry. It knows you are there.
        eyeYaw += MathHelper.wrapDegrees((float) Math.toDegrees((near ? wantYaw : 0F) - eyeYaw)) * 0.017453292F * 0.06F;
        eyePitch += ((near ? MathHelper.clamp(wantPitch, -0.7F, 0.7F) : 0F) - eyePitch) * 0.06F;
    }

    public static void render(World world, Vec3d camera, float partialTicks, boolean casket) {
        float age = world.getTotalWorldTime() + partialTicks;
        Minecraft mc = Minecraft.getMinecraft();
        MODEL.look(prevEyeYaw + (eyeYaw - prevEyeYaw) * partialTicks, prevEyePitch + (eyePitch - prevEyePitch) * partialTicks);
        MODEL.ring(age, 1F);

        GlSnapshot before = GlSnapshot.take();
        GlStateManager.pushMatrix();
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableCull();
        GlStateManager.enableRescaleNormal();
        GlStateManager.color(1F, 1F, 1F, 1F);
        RenderHelper.enableStandardItemLighting();
        mc.getTextureManager().bindTexture(TEXTURE);
        for (Relic relic : Relic.values()) {
            if (!near(relic, camera)) {
                continue;
            }
            int light = world.getCombinedLight(new BlockPos(relic.base()).up(relic.height), 0);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light % 65536, light / 65536);
            draw(relic, camera, casket);
        }
        RenderHelper.disableStandardItemLighting();

        // What burns and shines in them, at full brightness.
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        GlStateManager.depthMask(false);
        GlStateManager.depthFunc(GL11.GL_LEQUAL);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
        mc.getTextureManager().bindTexture(GLOW);
        for (Relic relic : Relic.values()) {
            if (!near(relic, camera)) {
                continue;
            }
            float pulse = 0.75F + 0.25F * MathHelper.sin(age * 0.05F + relic.ordinal() * 1.7F);
            GlStateManager.color(1F, 1F, 1F, pulse);
            draw(relic, camera, casket);
        }
        GlStateManager.color(1F, 1F, 1F, 1F);

        // What they give off.
        GlStateManager.disableTexture2D();
        GlStateManager.disableCull();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        Vec3d flame = at(Relic.ETERNAL_FLAME, 34.5D).subtract(camera);
        flame(flame, age);
        Vec3d crown = at(Relic.CROWN, 15D).subtract(camera);
        blackFire(crown, age);
        Vec3d fork = at(Relic.TUNING_FORK, 27D).subtract(camera);
        WarlockFx.glow(fork, 1.1D, new float[]{1F, 0.85F, 0.5F}, 0.06F + 0.04F * MathHelper.sin(age * 0.21F));
        Vec3d orb = at(Relic.WARLOCKS_EYE, 16.5D).subtract(camera);
        WarlockFx.glow(orb, 0.7D, new float[]{0.75F, 0.55F, 1F}, 0.12F + 0.05F * MathHelper.sin(age * 0.07F));
        Vec3d gauntlet = at(Relic.GAUNTLET, 19D).subtract(camera);
        WarlockFx.glow(gauntlet, 0.9D, new float[]{1F, 0.85F, 0.55F}, 0.08F);
        if (casket) {
            Vec3d heart = at(Relic.CASKET, 20.5D).subtract(camera);
            WarlockFx.glow(heart, 1.4D, VaultFx.ICE, 0.22F + 0.06F * MathHelper.sin(age * 0.09F));
            WarlockFx.glow(heart, 0.6D, VaultFx.FROST, 0.3F);
            mist(heart, age);
        }

        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.depthMask(true);
        GlStateManager.disableRescaleNormal();
        GlStateManager.popMatrix();
        before.restore();
    }

    /** How much larger than its model each treasure stands, so it holds its own in its bay. */
    public static float scale(Relic relic) {
        switch (relic) {
            case GAUNTLET:
            case TABLET:
                return 1.6F;
            case WARLOCKS_EYE:
                return 1.5F;
            case CROWN:
                return 1.45F;
            case TUNING_FORK:
                return 1.35F;
            case CASKET:
                return 1.25F;
            default:
                return 1F;
        }
    }

    /** A point that many model pixels up the treasure, in the world. */
    public static Vec3d at(Relic relic, double pixels) {
        return relic.base().add(0D, pixels * PX * scale(relic), 0D);
    }

    private static boolean near(Relic relic, Vec3d camera) {
        return relic.base().squareDistanceTo(camera) < 96D * 96D;
    }

    private static void draw(Relic relic, Vec3d camera, boolean casket) {
        Vec3d at = relic.base().subtract(camera);
        GlStateManager.pushMatrix();
        GlStateManager.translate(at.x, at.y, at.z);
        GlStateManager.rotate(relic == Relic.CASKET ? 180F : relic.yaw(), 0F, 1F, 0F);
        float s = scale(relic);
        GlStateManager.scale(-s, -s, s);
        switch (relic) {
            case GAUNTLET:
                MODEL.gauntlet.render(PX);
                break;
            case ETERNAL_FLAME:
                MODEL.flame.render(PX);
                break;
            case TUNING_FORK:
                MODEL.fork.render(PX);
                break;
            case TABLET:
                MODEL.tablet.render(PX);
                break;
            case WARLOCKS_EYE:
                MODEL.eye.render(PX);
                break;
            case CROWN:
                MODEL.crown.render(PX);
                break;
            case MJOLNIR:
                MODEL.plinth.render(PX);
                break;
            case TESSERACT:
                MODEL.plinth.render(PX);
                MODEL.cradle.render(PX);
                break;
            case CASKET:
                MODEL.pedestal.render(PX);
                if (casket) {
                    GlStateManager.translate(0F, -17F * PX, 0F);
                    MODEL.casket.render(PX);
                }
                break;
        }
        GlStateManager.popMatrix();
    }

    /** The casket on its own, for drawing it as an item. Its glow is drawn over it, at full brightness. */
    public static void casket(float time) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.getTextureManager().bindTexture(TEXTURE);
        MODEL.casket.render(PX);
        GlSnapshot before = GlSnapshot.take();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        GlStateManager.depthFunc(GL11.GL_LEQUAL);
        float lastX = OpenGlHelper.lastBrightnessX, lastY = OpenGlHelper.lastBrightnessY;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
        mc.getTextureManager().bindTexture(GLOW);
        GlStateManager.color(1F, 1F, 1F, 0.8F + 0.2F * MathHelper.sin(time * 0.09F));
        MODEL.casket.render(PX);
        GlStateManager.color(1F, 1F, 1F, 1F);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lastX, lastY);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        before.restore();
    }

    public static ModelRenderer casketModel() {
        return MODEL.casket;
    }

    // ------------------------------------------------------------------ what they give off

    /**
     * The Eternal Flame: tongues of fire licking up out of the chalice, never the same shape twice,
     * white at the heart, through gold and orange to red at the tips.
     */
    private static void flame(Vec3d c, float age) {
        Vec3d toCamera = c.scale(-1D);
        Vec3d side = new Vec3d(toCamera.z, 0D, -toCamera.x);
        side = side.lengthSquared() < 1.0E-6 ? new Vec3d(1D, 0D, 0D) : side.normalize();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        Random rand = new Random(7L);
        float[][] layers = {{0.9F, 0.18F, 0.04F, 0.28F, 1F}, {1F, 0.5F, 0.1F, 0.4F, 0.78F}, {1F, 0.82F, 0.35F, 0.55F, 0.55F}, {1F, 0.97F, 0.85F, 0.7F, 0.3F}};
        for (float[] layer : layers) {
            buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
            for (int i = 0; i < 9; i++) {
                double offset = (i - 4) / 4D * 0.32D * layer[4] + (rand.nextDouble() - 0.5D) * 0.08D;
                float phase = rand.nextFloat() * 10F;
                double height = (0.55D + 0.45D * rand.nextDouble()) * layer[4] * (0.9D + 0.25D * Math.sin(age * 0.37F + phase))
                        * (1D - Math.abs(offset) * 1.4D) * 1.35D;
                double width = 0.11D * layer[4] + 0.04D;
                double sway = Math.sin(age * 0.23F + phase) * 0.08D * height;
                Vec3d base = c.add(side.scale(offset));
                Vec3d tip = base.add(side.scale(sway)).add(0D, height, 0D);
                buffer.pos(base.x - side.x * width, base.y, base.z - side.z * width).color(layer[0], layer[1], layer[2], layer[3]).endVertex();
                buffer.pos(base.x + side.x * width, base.y, base.z + side.z * width).color(layer[0], layer[1], layer[2], layer[3]).endVertex();
                buffer.pos(tip.x, tip.y, tip.z).color(layer[0], layer[1], layer[2], 0F).endVertex();
            }
            tessellator.draw();
        }
        WarlockFx.glow(c.add(0D, 0.3D, 0D), 0.9D, VaultFx.ORANGE, 0.14F + 0.04F * MathHelper.sin(age * 0.6F));
        WarlockFx.glow(c.add(0D, 0.15D, 0D), 0.45D, VaultFx.YELLOW, 0.25F);
    }

    /** The Crown's black fire: dark tongues that eat the light around them, red glinting at their roots. */
    private static void blackFire(Vec3d c, float age) {
        Vec3d toCamera = c.scale(-1D);
        Vec3d side = new Vec3d(toCamera.z, 0D, -toCamera.x);
        side = side.lengthSquared() < 1.0E-6 ? new Vec3d(1D, 0D, 0D) : side.normalize();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        Random rand = new Random(13L);
        // A sullen red light behind them, so the dark of them shows against it.
        WarlockFx.glow(c.add(0D, 0.25D, 0D), 0.6D, VaultFx.RED, 0.2F + 0.06F * MathHelper.sin(age * 0.31F));
        WarlockFx.glow(c.add(0D, 0.1D, 0D), 0.35D, VaultFx.ORANGE, 0.16F);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < 12; i++) {
            double offset = (i - 5.5D) / 5.5D * 0.42D;
            float phase = rand.nextFloat() * 10F;
            double height = (0.35D + 0.4D * rand.nextDouble()) * (0.85D + 0.3D * Math.sin(age * 0.29F + phase)) * (1D - Math.abs(offset));
            double width = 0.09D;
            double sway = Math.sin(age * 0.17F + phase) * 0.12D * height;
            Vec3d base = c.add(side.scale(offset));
            Vec3d tip = base.add(side.scale(sway)).add(0D, height, 0D);
            buffer.pos(base.x - side.x * width, base.y, base.z - side.z * width).color(0.06F, 0.02F, 0.05F, 0.75F).endVertex();
            buffer.pos(base.x + side.x * width, base.y, base.z + side.z * width).color(0.06F, 0.02F, 0.05F, 0.75F).endVertex();
            buffer.pos(tip.x, tip.y, tip.z).color(0.02F, 0F, 0.02F, 0F).endVertex();
        }
        tessellator.draw();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        WarlockFx.glow(c.add(0D, -0.05D, 0D), 0.8D, VaultFx.RED, 0.18F + 0.08F * MathHelper.sin(age * 0.43F));
    }

    /** Cold mist spilling over the Casket's sides and down the pedestal. */
    private static void mist(Vec3d c, float age) {
        for (int i = 0; i < 6; i++) {
            float local = (age * 0.012F + i / 6F) % 1F;
            double a = i * 1.9D + age * 0.01D;
            Vec3d p = c.add(Math.cos(a) * (0.3D + local * 0.5D), -local * 1.1D, Math.sin(a) * (0.3D + local * 0.5D));
            WarlockFx.glow(p, 0.35D + local * 0.5D, VaultFx.FROST, 0.1F * (1F - local) * Math.min(1F, local * 5F));
        }
    }
}
