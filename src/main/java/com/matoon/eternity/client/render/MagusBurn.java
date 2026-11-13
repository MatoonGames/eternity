package com.matoon.eternity.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * What his own snap does to the Magus: his right arm burnt the way a player's is (see the stones' BurnPainter), black
 * char climbing from the gauntlet to a ragged front past the elbow, raw red at its edge, split by cracks that glow
 * from the hand up. Painted once onto his 128x128 layout (see ModelMagus) as three layers: the char, lit like the rest
 * of him; the cracks' glow; and the white heat of the flare, both drawn in their own light.
 */
@SideOnly(Side.CLIENT)
final class MagusBurn {

    private static final int SIZE = 128;
    /** Upper arm and forearm, end to end, in pixels: how far along the arm (shoulder 0, hand 1) each row is. */
    private static final float LENGTH = 13F;

    private static ResourceLocation[] layers;

    private MagusBurn() {
    }

    /** The char, glow and heat textures, painted the first time they are wanted. */
    static ResourceLocation[] layers() {
        if (layers == null) {
            int[][] painted = paint();
            layers = new ResourceLocation[3];
            String[] names = {"char", "glow", "heat"};
            for (int i = 0; i < 3; i++) {
                DynamicTexture texture = new DynamicTexture(SIZE, SIZE);
                System.arraycopy(painted[i], 0, texture.getTextureData(), 0, SIZE * SIZE);
                texture.updateDynamicTexture();
                layers[i] = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation("eternity_magus_burn_" + names[i], texture);
            }
        }
        return layers;
    }

    private static int[][] paint() {
        int[][] out = new int[3][SIZE * SIZE];
        // His right arm (ModelMagus.rArm at 0,32), forearm (rFore at 32,32) and wristband (64,32), and the tunic's
        // flared right shoulder (0,44), barely touched.
        box(out, 0, 32, 4, 6, 4, 0F, 6F);
        box(out, 32, 32, 4, 7, 4, 6F, 7F);
        box(out, 64, 32, 5, 2, 5, 10.4F, 2F);
        box(out, 0, 44, 4, 2, 6, -1.5F, 2F);
        return out;
    }

    /** One box of the arm, its sides unrolled round it: rows run from {@code from} down its length of {@code length}. */
    private static void box(int[][] out, int u, int v, int w, int h, int d, float from, float length) {
        int around = 2 * (w + d);
        for (int c = 0; c < around; c++) {
            for (int r = 0; r < h; r++) {
                put(out, u + c, v + d + r, c / (float) around, (from + (r + 0.5F) * length / h) / LENGTH);
            }
        }
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < d; j++) {
                put(out, u + d + i, v + j, (i + j * 0.37F) / (w + d), from / LENGTH);
                put(out, u + d + w + i, v + j, (i + j * 0.37F) / (w + d), (from + length) / LENGTH);
            }
        }
    }

    private static void put(int[][] out, int x, int y, float around, float along) {
        if (x < 0 || y < 0 || x >= SIZE || y >= SIZE) {
            return;
        }
        int i = y * SIZE + x;
        float ragged = noise(around * 7F, along * 9F) - 0.5F;
        // Burnt from the hand nearly to the shoulder, the front of it ragged; the flare of the tunic only singed.
        float burn = clamp((along - 0.08F) / 0.25F + ragged * 1.6F);
        if (burn <= 0.02F) {
            return;
        }
        float ash = noise(around * 23F + 5F, along * 31F);
        int charCol = mix(0x7a2418, 0x150d0b, clamp(burn * 1.6F - 0.3F));
        if (ash > 0.72F && burn > 0.6F) {
            charCol = mix(charCol, 0x5e5852, 0.6F);
        }
        out[0][i] = argb(Math.min(1F, burn * 1.5F), charCol);
        // Cracks, grown up out of the hand: wider and whiter the nearer the hand.
        float crack = Math.abs(noise(around * 11F + 13F, along * 5F) - 0.5F);
        float width = 0.03F + 0.09F * along * along;
        if (crack < width && burn > 0.35F) {
            float core = 1F - crack / width;
            out[1][i] = argb(core * clamp(burn * 1.3F - 0.3F), mix(0xff6a10, 0xfff0b0, along * along * core));
        }
        out[2][i] = argb(burn * burn * along, 0xfff4d0);
    }

    /** Smooth value noise, 0 to 1. */
    private static float noise(float x, float y) {
        int x0 = (int) Math.floor(x), y0 = (int) Math.floor(y);
        float fx = x - x0, fy = y - y0;
        fx = fx * fx * (3F - 2F * fx);
        fy = fy * fy * (3F - 2F * fy);
        float a = hash(x0, y0), b = hash(x0 + 1, y0), c = hash(x0, y0 + 1), e = hash(x0 + 1, y0 + 1);
        return (a + (b - a) * fx) + ((c + (e - c) * fx) - (a + (b - a) * fx)) * fy;
    }

    private static float hash(int x, int y) {
        int h = x * 374761393 + y * 668265263 + 0x5bd1e995;
        h = (h ^ h >>> 13) * 1274126177;
        return ((h ^ h >>> 16) & 0xFFFF) / 65535F;
    }

    private static float clamp(float x) {
        return Math.max(0F, Math.min(1F, x));
    }

    private static int mix(int a, int b, float k) {
        int r = (int) ((a >> 16 & 255) + ((b >> 16 & 255) - (a >> 16 & 255)) * k);
        int g = (int) ((a >> 8 & 255) + ((b >> 8 & 255) - (a >> 8 & 255)) * k);
        int bl = (int) ((a & 255) + ((b & 255) - (a & 255)) * k);
        return r << 16 | g << 8 | bl;
    }

    private static int argb(float alpha, int rgb) {
        return (int) (clamp(alpha) * 255F) << 24 | rgb & 0xFFFFFF;
    }
}
