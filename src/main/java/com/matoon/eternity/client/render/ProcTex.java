package com.matoon.eternity.client.render;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * Textures painted in code, once, the first time something needs them: the noise they are made
 * of, and the soft sprites every glow is drawn with. The Casket and the Aether's lantern are
 * built from these, as the Tesseract is from its own.
 */
@SideOnly(Side.CLIENT)
final class ProcTex {

    private static DynamicTexture glow;
    private static DynamicTexture wisp;

    private ProcTex() {
    }

    interface Pixel {
        int argb(float u, float v);
    }

    /** A texture painted pixel by pixel, smooth and tiling. */
    static DynamicTexture texture(int size, Pixel pixel) {
        return texture(size, size, pixel);
    }

    static DynamicTexture texture(int width, int height, Pixel pixel) {
        DynamicTexture texture = new DynamicTexture(width, height);
        int[] data = texture.getTextureData();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                data[y * width + x] = pixel.argb((x + 0.5F) / width, (y + 0.5F) / height);
            }
        }
        texture.updateDynamicTexture();
        GlStateManager.bindTexture(texture.getGlTextureId());
        GlStateManager.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GlStateManager.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GlStateManager.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GlStateManager.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
        return texture;
    }

    /** A soft round spot of white, brightest in the middle. */
    static DynamicTexture glow() {
        if (glow == null) {
            glow = texture(64, (u, v) -> {
                float dx = u - 0.5F, dy = v - 0.5F;
                float d = MathHelper.sqrt(dx * dx + dy * dy) * 2F;
                float a = d >= 1F ? 0F : (float) Math.pow(1F - d, 2.2F);
                return ((int) (a * 255F) << 24) | 0xFFFFFF;
            });
        }
        return glow;
    }

    /** A torn wisp of white: a soft blot frayed at its edges, for mist and smoke. */
    static DynamicTexture wisp() {
        if (wisp == null) {
            wisp = texture(64, (u, v) -> {
                float dx = (u - 0.5F) * 2F, dy = (v - 0.5F) * 2F;
                float d = MathHelper.sqrt(dx * dx + dy * dy);
                float body = d >= 1F ? 0F : (float) Math.pow(1F - d, 1.3F);
                float fray = MathHelper.clamp(fbm(u * 4F, v * 4F, 4, 4, 71) * 0.9F + 0.5F, 0F, 1F);
                float a = MathHelper.clamp(body * fray * 1.5F, 0F, 1F);
                return ((int) (a * 255F) << 24) | 0xFFFFFF;
            });
        }
        return wisp;
    }

    static int argb(float a, float r, float g, float b) {
        return (int) (MathHelper.clamp(a, 0F, 1F) * 255F) << 24 | (int) (MathHelper.clamp(r, 0F, 1F) * 255F) << 16
                | (int) (MathHelper.clamp(g, 0F, 1F) * 255F) << 8 | (int) (MathHelper.clamp(b, 0F, 1F) * 255F);
    }

    static float smooth(float x) {
        x = MathHelper.clamp(x, 0F, 1F);
        return x * x * (3F - 2F * x);
    }

    /** Fractal noise that tiles every {@code period} units, roughly in [-1, 1]. */
    static float fbm(float x, float y, int period, int octaves, int seed) {
        float sum = 0F, amp = 0.65F;
        for (int o = 0; o < octaves; o++) {
            sum += amp * perlin(x * (1 << o), y * (1 << o), period << o, seed + o);
            amp *= 0.5F;
        }
        return sum * 1.4F;
    }

    /** Gradient noise on a lattice that wraps every {@code period} cells. */
    static float perlin(float x, float y, int period, int seed) {
        int x0 = MathHelper.floor(x), y0 = MathHelper.floor(y);
        float fx = x - x0, fy = y - y0;
        float u = fx * fx * fx * (fx * (fx * 6F - 15F) + 10F), v = fy * fy * fy * (fy * (fy * 6F - 15F) + 10F);
        float n00 = gradient(x0, y0, period, seed, fx, fy);
        float n10 = gradient(x0 + 1, y0, period, seed, fx - 1F, fy);
        float n01 = gradient(x0, y0 + 1, period, seed, fx, fy - 1F);
        float n11 = gradient(x0 + 1, y0 + 1, period, seed, fx - 1F, fy - 1F);
        float a = n00 + u * (n10 - n00), b = n01 + u * (n11 - n01);
        return (a + v * (b - a)) * 1.41F;
    }

    private static float gradient(int x, int y, int period, int seed, float dx, float dy) {
        int h = Math.floorMod(x, period) * 374761393 + Math.floorMod(y, period) * 668265263 + seed * 1442695041;
        h = (h ^ (h >>> 13)) * 1274126177;
        h ^= h >>> 16;
        double angle = (h & 1023) / 1024D * Math.PI * 2D;
        return (float) (Math.cos(angle) * dx + Math.sin(angle) * dy);
    }

    /** A hash of a lattice point, in [0, 1). */
    static float hash(int x, int y, int seed) {
        int h = x * 374761393 + y * 668265263 + seed * 1442695041;
        h = (h ^ (h >>> 13)) * 1274126177;
        h ^= h >>> 16;
        return (h & 0xFFFFFF) / (float) 0x1000000;
    }

    /**
     * Cellular noise, tiling every {@code period} cells: the distance to the nearest scattered
     * point and to the next nearest, and which cell the nearest belongs to.
     */
    static float[] cells(float x, float y, int period, int seed) {
        int cx = MathHelper.floor(x), cy = MathHelper.floor(y);
        float d1 = 9F, d2 = 9F, id = 0F;
        for (int j = -1; j <= 1; j++) {
            for (int i = -1; i <= 1; i++) {
                int gx = cx + i, gy = cy + j;
                int wx = Math.floorMod(gx, period), wy = Math.floorMod(gy, period);
                float px = gx + hash(wx, wy, seed), py = gy + hash(wx, wy, seed + 7);
                float d = (px - x) * (px - x) + (py - y) * (py - y);
                if (d < d1) {
                    d2 = d1;
                    d1 = d;
                    id = hash(wx, wy, seed + 13);
                } else if (d < d2) {
                    d2 = d;
                }
            }
        }
        return new float[]{MathHelper.sqrt(d1), MathHelper.sqrt(d2), id};
    }
}
