package com.matoon.eternity.client.render;

import com.matoon.eternity.Eternity;
import com.matoon.eternity.world.OdinsVault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import static com.matoon.eternity.client.render.WarlockFx.flat;
import static com.matoon.eternity.client.render.WarlockFx.glow;
import static com.matoon.eternity.client.render.WarlockFx.ring;
import static com.matoon.eternity.client.render.WarlockFx.strip;

/**
 * What hangs and burns and drifts in Odin's Vault, beyond its blocks: Odin's banners down the
 * walls of the hall, stirring in a draught nobody feels; fire in the braziers below them, and the
 * embers going up; circles of runes turning slowly in the floor under the Casket and the Destroyer;
 * and loose runes drifting up through the air, the cold blue of the ones cut into the walls.
 */
@SideOnly(Side.CLIENT)
public final class VaultHall {

    private static final ResourceLocation BANNER = new ResourceLocation(Eternity.MODID, "textures/entity/vault_banner.png");
    private static final ResourceLocation GLYPHS = new ResourceLocation(Eternity.MODID, "textures/entity/vault_glyphs.png");
    private static final List<BlockPos> BRAZIERS = OdinsVault.braziers();
    private static final List<Vec3d> BANNERS = OdinsVault.banners();
    private static final float[] SEIDR = {0.35F, 0.72F, 1F};
    private static final float[] EMBER = {1F, 0.62F, 0.22F};
    private static final float[] POOL = {1F, 0.55F, 0.2F};
    private static final int RUNES = 26;

    /** Embers off the braziers: x, y, z, motion x, y, z, age, lifetime. */
    private static final List<double[]> embers = new ArrayList<>();
    /** Runes adrift: x, y, z, rise, age, lifetime, which rune, sway phase. */
    private static final List<double[]> drifting = new ArrayList<>();

    private VaultHall() {
    }

    public static void clear() {
        embers.clear();
        drifting.clear();
    }

    // ------------------------------------------------------------------ every tick

    public static void tick(Minecraft mc, Random rand) {
        Vec3d me = mc.player.getPositionVector();
        for (BlockPos b : BRAZIERS) {
            double d = me.squareDistanceTo(b.getX() + 0.5D, b.getY(), b.getZ() + 0.5D);
            if (d > 48D * 48D) {
                continue;
            }
            if (rand.nextInt(3) == 0) {
                embers.add(new double[]{b.getX() + 0.5D + rand.nextGaussian() * 0.2D, b.getY() + 1D, b.getZ() + 0.5D + rand.nextGaussian() * 0.2D,
                        rand.nextGaussian() * 0.01D, 0.035D + rand.nextDouble() * 0.04D, rand.nextGaussian() * 0.01D, 0D, 30 + rand.nextInt(50)});
            }
            if (d < 10D * 10D && rand.nextInt(90) == 0) {
                mc.world.playSound(b.getX() + 0.5D, b.getY() + 1D, b.getZ() + 0.5D, SoundEvents.BLOCK_FIRE_AMBIENT, SoundCategory.BLOCKS,
                        0.35F + rand.nextFloat() * 0.3F, 0.6F + rand.nextFloat() * 0.4F, false);
            }
        }
        Iterator<double[]> it = embers.iterator();
        while (it.hasNext()) {
            double[] e = it.next();
            e[0] += e[3];
            e[1] += e[4];
            e[2] += e[5];
            e[3] += rand.nextGaussian() * 0.003D;
            e[5] += rand.nextGaussian() * 0.003D;
            e[4] *= 0.985D;
            if (++e[6] > e[7]) {
                it.remove();
            }
        }
        it = drifting.iterator();
        while (it.hasNext()) {
            double[] r = it.next();
            r[1] += r[3];
            if (++r[4] > r[5] || (r[0] - me.x) * (r[0] - me.x) + (r[2] - me.z) * (r[2] - me.z) > 24D * 24D) {
                it.remove();
            }
        }
        // Only where there is open air and some light to see them by: in the vault, not in its walls.
        for (int tries = 0; drifting.size() < RUNES && tries < 4; tries++) {
            double x = me.x + rand.nextGaussian() * 9D, y = me.y + rand.nextDouble() * 8D, z = me.z + rand.nextGaussian() * 9D;
            BlockPos at = new BlockPos(x, y, z);
            if (mc.world.isAirBlock(at) && mc.world.getLightFor(EnumSkyBlock.BLOCK, at) > 4) {
                drifting.add(new double[]{x, y, z, 0.006D + rand.nextDouble() * 0.01D, 0D, 140 + rand.nextInt(160), rand.nextInt(16), rand.nextDouble() * 6.28D});
            }
        }
    }

    // ------------------------------------------------------------------ drawing

    public static void render(World world, Vec3d camera, float pt, float t) {
        banners(world, camera, t);
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        GlStateManager.depthMask(false);
        GlStateManager.disableFog();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        GlStateManager.disableAlpha();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
        GlStateManager.disableTexture2D();
        fires(camera, t);
        sigil(camera, t);
        embers(camera, pt);
        GlStateManager.enableTexture2D();
        Minecraft.getMinecraft().getTextureManager().bindTexture(GLYPHS);
        circles(camera, t);
        drifting(camera, pt, t);
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.depthMask(true);
        GlStateManager.enableAlpha();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableCull();
    }

    /**
     * Each banner a sheet of cloth eight blocks long, hanging an inch off the wall, lit by the
     * light where it hangs; it bellies out a little toward its foot and the hem stirs.
     */
    private static void banners(World world, Vec3d camera, float t) {
        Minecraft mc = Minecraft.getMinecraft();
        GlStateManager.enableTexture2D();
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
        GlStateManager.disableCull();
        GlStateManager.disableLighting();
        GlStateManager.enableFog();
        GlStateManager.color(1F, 1F, 1F, 1F);
        mc.entityRenderer.enableLightmap();
        mc.getTextureManager().bindTexture(BANNER);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int rows = 16;
        for (Vec3d top : BANNERS) {
            if (top.squareDistanceTo(camera) > 96D * 96D) {
                continue;
            }
            // Out from the wall, toward the hall's centre line.
            double out = top.x > 0.5D ? -1D : 1D;
            float phase = (float) (top.z * 0.7D + (out > 0 ? 2D : 0D));
            buffer.begin(GL11.GL_QUAD_STRIP, DefaultVertexFormats.BLOCK);
            for (int i = 0; i <= rows; i++) {
                double v = i / (double) rows;
                double y = top.y - v * 8D;
                double sway = v * v * (0.1D + 0.05D * Math.sin(t * 0.045F + phase + v * 2.4D));
                double ripple = 0.025D * v * Math.sin(t * 0.11F + v * 9D + phase);
                double x = top.x + out * (sway + ripple) - camera.x;
                pos.setPos(top.x + out * 0.6D, y, top.z);
                int light = world.getCombinedLight(pos, 0);
                int sky = light >> 16 & 65535, block = light & 65535;
                // The folds catch the light unevenly.
                float shade = 0.82F + 0.18F * (float) Math.cos(v * 7D + phase + t * 0.03F);
                for (int side = -1; side <= 1; side += 2) {
                    buffer.pos(x, y - camera.y, top.z + side - camera.z).color(shade, shade, shade, 1F)
                            .tex((side < 0) == (out < 0) ? 0D : 1D, v).lightmap(sky, block).endVertex();
                }
            }
            tessellator.draw();
        }
        mc.entityRenderer.disableLightmap();
        GlStateManager.disableFog();
    }

    /** Fire in every brazier near enough to see: tongues of flame, a hot heart, a glow, and its light pooled round it on the floor. */
    private static void fires(Vec3d camera, float t) {
        for (BlockPos b : BRAZIERS) {
            Vec3d c = new Vec3d(b.getX() + 0.5D, b.getY() + 0.88D, b.getZ() + 0.5D).subtract(camera);
            if (c.lengthSquared() > 64D * 64D) {
                continue;
            }
            float seed = (b.getX() * 7 + b.getZ() * 13) % 17;
            float breathe = 0.85F + 0.1F * MathHelper.sin(t * 0.21F + seed) + 0.05F * MathHelper.sin(t * 0.67F + seed * 2F);
            for (int k = 0; k < 6; k++) {
                double a = k * Math.PI / 3D + seed;
                float flick = 0.7F + 0.22F * MathHelper.sin(t * 0.37F + k * 1.7F + seed) + 0.12F * MathHelper.sin(t * 0.93F + k * 2.3F);
                Vec3d base = c.add(Math.cos(a) * 0.2D, 0D, Math.sin(a) * 0.2D);
                double height = (0.55D + 0.2D * ((k * 5 + (int) seed) % 3) / 2D) * flick * breathe;
                Vec3d tip = base.add(Math.sin(t * 0.13F + k) * 0.1D, height, Math.cos(t * 0.11F + k * 1.3F) * 0.1D);
                strip(base, tip, 0.2D, 0.02D, VaultFx.ORANGE, VaultFx.RED, 0.5F, 0F);
            }
            Vec3d heart = c.add(0D, 0.48D * breathe, 0D);
            strip(c, heart, 0.24D, 0.04D, VaultFx.YELLOW, VaultFx.ORANGE, 0.7F, 0F);
            strip(c, c.add(0D, 0.26D, 0D), 0.13D, 0.03D, VaultFx.WHITE, VaultFx.YELLOW, 0.65F, 0F);
            glow(c.add(0D, 0.35D, 0D), 1.1D, VaultFx.ORANGE, 0.24F * breathe);
            glow(c.add(0D, 0.4D, 0D), 2.8D, VaultFx.RED, 0.07F * breathe);
            flat(c.add(0D, -0.86D, 0D), 3.2D, POOL, 0.06F * breathe);
        }
    }

    /**
     * Odin's sigil over the Destroyer's niche: the Valknut, three triangles locked together, traced
     * in light on the dark of the far wall, breathing slowly, a brighter gleam running round its lines.
     */
    private static void sigil(Vec3d camera, float t) {
        Vec3d c = new Vec3d(OdinsVault.NICHE_X, OdinsVault.STAND + 17.5D, OdinsVault.SIGIL_Z).subtract(camera);
        if (c.lengthSquared() > 110D * 110D) {
            return;
        }
        float breathe = 0.8F + 0.2F * MathHelper.sin(t * 0.03F);
        glow(c, 7D, VaultFx.GOLD, 0.1F * breathe);
        double size = 3D;
        for (int tri = 0; tri < 3; tri++) {
            double off = Math.PI * 2D * tri / 3D + Math.PI / 2D + Math.PI / 6D;
            Vec3d o = c.add(Math.cos(off) * size * 0.48D, Math.sin(off) * size * 0.48D, 0D);
            Vec3d[] p = new Vec3d[3];
            for (int k = 0; k < 3; k++) {
                double a = Math.PI / 2D + Math.PI * 2D * k / 3D;
                p[k] = o.add(Math.cos(a) * size, Math.sin(a) * size, 0D);
            }
            for (int k = 0; k < 3; k++) {
                Vec3d a = p[k], b = p[(k + 1) % 3];
                strip(a, b, 0.32D, VaultFx.ORANGE, 0.22F * breathe);
                strip(a, b, 0.11D, VaultFx.GOLD, 0.75F * breathe);
                strip(a, b, 0.04D, VaultFx.WHITE, 0.8F * breathe);
                // The gleam running round it.
                double round = (t * 0.008D + tri / 3D) % 1D * 3D;
                if ((int) round == k) {
                    glow(a.add(b.subtract(a).scale(round - k)), 0.45D, VaultFx.WHITE, 0.45F);
                }
            }
        }
    }

    private static void embers(Vec3d camera, float pt) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        for (double[] e : embers) {
            double x = e[0] + e[3] * pt - camera.x, y = e[1] + e[4] * pt - camera.y, z = e[2] + e[5] * pt - camera.z;
            float life = (float) (e[6] / e[7]);
            float a = Math.min(1F, (1F - life) * 1.5F) * 0.9F;
            float g = EMBER[1] * (1F - life * 0.6F), bl = EMBER[2] * (1F - life);
            double s = 0.035D * (1D - life * 0.5D);
            buffer.pos(x - s, y, z).color(1F, g, bl, a).endVertex();
            buffer.pos(x, y + s, z).color(1F, g, bl, a).endVertex();
            buffer.pos(x + s, y, z).color(1F, g, bl, a).endVertex();
            buffer.pos(x, y - s, z).color(1F, g, bl, a).endVertex();
            buffer.pos(x, y, z - s).color(1F, g, bl, a).endVertex();
            buffer.pos(x, y + s, z).color(1F, g, bl, a).endVertex();
            buffer.pos(x, y, z + s).color(1F, g, bl, a).endVertex();
            buffer.pos(x, y - s, z).color(1F, g, bl, a).endVertex();
        }
        tessellator.draw();
    }

    /** A ring of runes turning in the floor round the Casket's pedestal, and a wider one before the Destroyer. */
    private static void circles(Vec3d camera, float t) {
        circle(new Vec3d(OdinsVault.CASKET_X, OdinsVault.STAND + 0.03D, OdinsVault.CASKET_Z).subtract(camera), 2.4D, 14, t * 0.004F, 0.5F, t);
        circle(new Vec3d(OdinsVault.NICHE_X, OdinsVault.STAND + 0.03D, OdinsVault.NICHE_Z + 4D).subtract(camera), 3.6D, 20, -t * 0.0025F, 0.4F, t);
    }

    private static void circle(Vec3d c, double radius, int count, float spin, float alpha, float t) {
        if (c.lengthSquared() > 72D * 72D) {
            return;
        }
        float pulse = 0.75F + 0.25F * MathHelper.sin(t * 0.05F);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        for (int i = 0; i < count; i++) {
            double a = Math.PI * 2D * i / count + spin;
            Vec3d at = c.add(Math.cos(a) * radius, 0D, Math.sin(a) * radius);
            // Each rune standing on the ring, read from outside it.
            Vec3d along = new Vec3d(-Math.sin(a), 0D, Math.cos(a)).scale(0.22D);
            Vec3d up = new Vec3d(Math.cos(a), 0D, Math.sin(a)).scale(0.3D);
            float shimmer = 0.6F + 0.4F * MathHelper.sin(t * 0.08F - i * 0.9F);
            glyph(buffer, at, along, up, (i * 7) % 16, SEIDR, alpha * pulse * shimmer);
        }
        tessellator.draw();
        GlStateManager.disableTexture2D();
        ring(c, new Vec3d(0D, 1D, 0D), radius + 0.45D, 0.05D, SEIDR, 0.5F * alpha * pulse);
        ring(c, new Vec3d(0D, 1D, 0D), radius - 0.45D, 0.04D, SEIDR, 0.4F * alpha * pulse);
        GlStateManager.enableTexture2D();
    }

    /** Runes adrift in the air, each turned to face you, fading in and out as they rise. */
    private static void drifting(Vec3d camera, float pt, float t) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        for (double[] r : drifting) {
            double sway = Math.sin((r[4] + pt) * 0.04D + r[7]) * 0.15D;
            Vec3d at = new Vec3d(r[0] + sway, r[1] + r[3] * pt, r[2] + Math.cos((r[4] + pt) * 0.03D + r[7]) * 0.15D).subtract(camera);
            double dist = at.length();
            if (dist < 1D) {
                continue;
            }
            Vec3d right = new Vec3d(at.z, 0D, -at.x);
            if (right.lengthSquared() < 1.0E-6) {
                continue;
            }
            right = right.normalize().scale(0.1D);
            float life = (float) ((r[4] + pt) / r[5]);
            float a = Math.min(1F, life * 5F) * Math.min(1F, (1F - life) * 3F) * 0.5F * (float) MathHelper.clamp((dist - 1.5D) / 3D, 0D, 1D);
            glyph(buffer, at, right, new Vec3d(0D, 0.13D, 0D), (int) r[6], SEIDR, a);
        }
        tessellator.draw();
    }

    /** One rune from the sheet, centred on c, half its width along u and half its height along v. */
    private static void glyph(BufferBuilder buffer, Vec3d c, Vec3d u, Vec3d v, int which, float[] col, float alpha) {
        double u0 = (which % 8) / 8D, v0 = (which / 8) / 2D, u1 = u0 + 0.125D, v1 = v0 + 0.5D;
        buffer.pos(c.x - u.x + v.x, c.y - u.y + v.y, c.z - u.z + v.z).tex(u0, v0).color(col[0], col[1], col[2], alpha).endVertex();
        buffer.pos(c.x + u.x + v.x, c.y + u.y + v.y, c.z + u.z + v.z).tex(u1, v0).color(col[0], col[1], col[2], alpha).endVertex();
        buffer.pos(c.x + u.x - v.x, c.y + u.y - v.y, c.z + u.z - v.z).tex(u1, v1).color(col[0], col[1], col[2], alpha).endVertex();
        buffer.pos(c.x - u.x - v.x, c.y - u.y - v.y, c.z - u.z - v.z).tex(u0, v1).color(col[0], col[1], col[2], alpha).endVertex();
    }
}
