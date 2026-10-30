package com.matoon.eternity.client.render;

import com.matoon.eternity.Eternity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * The Grandmaster's playing cards, wherever they are: a red back on one side and a face on the
 * other (one of seven, from tools/GrandmasterSkin.java's atlas). Drawn as two quads back to back
 * so each side shows its own picture.
 */
@SideOnly(Side.CLIENT)
public final class Cards {

    public static final ResourceLocation TEXTURE = new ResourceLocation(Eternity.MODID, "textures/entity/grandmaster_cards.png");
    public static final double WIDTH = 0.26D, HEIGHT = 0.39D;

    private Cards() {
    }

    public static void bind() {
        Minecraft.getMinecraft().getTextureManager().bindTexture(TEXTURE);
    }

    public static void begin() {
        Tessellator.getInstance().getBuffer().begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
    }

    public static void end() {
        Tessellator.getInstance().draw();
    }

    /**
     * A card at {@code c} (camera-relative), its long side along {@code up} and its short side
     * along {@code across}, both unit and at right angles. {@code face} 1 to 7.
     */
    public static void card(Vec3d c, Vec3d up, Vec3d across, int face, float alpha, double scale) {
        BufferBuilder b = Tessellator.getInstance().getBuffer();
        Vec3d u = up.scale(HEIGHT * 0.5D * scale), a = across.scale(WIDTH * 0.5D * scale);
        Vec3d normal = across.crossProduct(up).normalize().scale(0.004D * scale);
        float faceU0 = (face % 8) * 16F / 128F, faceU1 = faceU0 + 16F / 128F;
        float backU0 = 0F, backU1 = 16F / 128F;
        float v0 = 0F, v1 = 24F / 32F;
        // The face, toward +normal.
        Vec3d f = c.add(normal);
        vertex(b, f.subtract(a).add(u), faceU0, v0, alpha);
        vertex(b, f.subtract(a).subtract(u), faceU0, v1, alpha);
        vertex(b, f.add(a).subtract(u), faceU1, v1, alpha);
        vertex(b, f.add(a).add(u), faceU1, v0, alpha);
        // The back, toward -normal, wound the other way.
        Vec3d k = c.subtract(normal);
        vertex(b, k.add(a).add(u), backU0, v0, alpha);
        vertex(b, k.add(a).subtract(u), backU0, v1, alpha);
        vertex(b, k.subtract(a).subtract(u), backU1, v1, alpha);
        vertex(b, k.subtract(a).add(u), backU1, v0, alpha);
    }

    private static void vertex(BufferBuilder b, Vec3d p, float u, float v, float alpha) {
        b.pos(p.x, p.y, p.z).tex(u, v).color(1F, 1F, 1F, alpha).endVertex();
    }

    /** Two axes for a card spinning about {@code axis}, turned {@code angle} from upright. */
    public static Vec3d[] spun(Vec3d axis, double angle) {
        Vec3d n = axis.lengthSquared() < 1.0E-6 ? new Vec3d(1D, 0D, 0D) : axis.normalize();
        Vec3d ref = Math.abs(n.y) > 0.9D ? new Vec3d(1D, 0D, 0D) : new Vec3d(0D, 1D, 0D);
        Vec3d up0 = ref.subtract(n.scale(ref.dotProduct(n))).normalize();
        Vec3d side0 = n.crossProduct(up0).normalize();
        double c = Math.cos(angle), s = Math.sin(angle);
        Vec3d up = up0.scale(c).add(side0.scale(s));
        Vec3d across = n;
        // Lay the card's short side across the axis so it tumbles end over end along it.
        return new Vec3d[]{up, across.crossProduct(up).normalize()};
    }

    /** Axes for a card tumbling freely: a deterministic jumble of turns from a seed and a time. */
    public static Vec3d[] tumbling(long seed, float time) {
        double a = (seed % 997) * 0.37D + time * (0.08D + (seed % 7) * 0.015D);
        double b = (seed % 571) * 0.53D + time * (0.05D + (seed % 5) * 0.02D);
        Vec3d up = new Vec3d(Math.cos(a) * Math.cos(b), Math.sin(b), Math.sin(a) * Math.cos(b));
        Vec3d ref = Math.abs(up.y) > 0.9D ? new Vec3d(1D, 0D, 0D) : new Vec3d(0D, 1D, 0D);
        Vec3d across = up.crossProduct(ref).normalize();
        double roll = time * 0.11D + seed;
        across = across.scale(Math.cos(roll)).add(up.crossProduct(across).scale(Math.sin(roll))).normalize();
        return new Vec3d[]{up, across};
    }

    /** Which face a card shows, from its seed: never the back, which is on every card's other side. */
    public static int face(long seed) {
        return 1 + (int) Math.floorMod(seed * 31L + 7L, 7L);
    }

    static float clamp(float x) {
        return MathHelper.clamp(x, 0F, 1F);
    }
}
