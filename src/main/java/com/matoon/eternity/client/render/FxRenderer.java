package com.matoon.eternity.client.render;

import com.matoon.eternity.EternitySounds;
import com.matoon.eternity.client.Rift;
import com.matoon.eternity.client.sound.LoopSound;
import com.matoon.eternity.client.sound.SoundFx;
import com.matoon.eternity.world.Singularity;
import com.matoon.eternity.entity.EntityAstralRegulator;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

/** Short-lived combat effects: lances of void, collapsing space, shockwaves. */
@SideOnly(Side.CLIENT)
public final class FxRenderer {

    private FxRenderer() {
    }

    public abstract static class Fx {
        public int age;
        public final int lifetime;

        protected Fx(int lifetime) {
            this.lifetime = lifetime;
        }

        public boolean isDead() {
            return age >= lifetime;
        }

        protected float progress(float partialTicks) {
            return MathHelper.clamp((age + partialTicks) / lifetime, 0F, 1F);
        }

        void tick() {
        }

        void onRemoved() {
        }

        abstract void render(Vec3d camera, float partialTicks);
    }

    /** A faint line tracing where a lance will strike. */
    public static class Warning extends Fx {
        final Vec3d from, to;

        public Warning(Vec3d from, Vec3d to, int lifetime) {
            super(lifetime);
            this.from = from;
            this.to = to;
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            float p = progress(partialTicks);
            float alpha = (0.15F + 0.35F * p) * (0.6F + 0.4F * MathHelper.sin((age + partialTicks) * 1.3F));
            ribbon(from.subtract(camera), to.subtract(camera), 0.05D + 0.08D * p, 0.8F, 0.25F, 1F, alpha);
        }
    }

    /** The lance itself: a shaft of open space, white hot at its core. */
    public static class Beam extends Fx {
        final Vec3d from, to;

        final double thickness;

        public Beam(Vec3d from, Vec3d to, double thickness) {
            super(14);
            this.from = from;
            this.to = to;
            this.thickness = thickness;
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            float p = progress(partialTicks);
            float fade = 1F - p;
            double width = thickness * (1D - p * 0.7D);
            Vec3d a = from.subtract(camera);
            Vec3d b = to.subtract(camera);
            Cosmic.draw(() -> ribbonGeometry(a, b, width), 10, fade, 1.5F * fade, 1F, 0.7F, 1F, false);
            additive();
            ribbon(a, b, width * 1.8D, 0.55F, 0.15F, 1F, 0.45F * fade);
            ribbon(a, b, width * 0.25D, 1F, 0.95F, 1F, 0.95F * fade);
        }
    }

    /**
     * A black hole: a sphere of perfect dark ringed by a burning photon ring, with a
     * tilted accretion disk of light spiralling into it. Grows exactly as the server's does.
     */
    public static class BlackHole extends Fx {
        public final Vec3d centre;
        final double maxRadius;
        LoopSound hum;

        public BlackHole(Vec3d centre, double maxRadius) {
            super(Singularity.LIFETIME);
            this.centre = centre;
            this.maxRadius = maxRadius;
        }

        public double radius(float partialTicks) {
            return Singularity.radius(maxRadius, age + partialTicks);
        }

        @Override
        void tick() {
            Minecraft mc = Minecraft.getMinecraft();
            if (hum == null) {
                hum = new LoopSound(EternitySounds.AMBIENT_RUMBLE, SoundCategory.HOSTILE, centre.x, centre.y, centre.z, 0.05F).pitch(0.5F);
                SoundFx.play(hum);
            }
            double r = radius(0F);
            hum.setTarget((float) Math.min(16D, 4D + r * 0.3D));
            if (mc.world == null) {
                return;
            }
            Random rand = mc.world.rand;
            int count = (int) Math.min(40D, 4D + r);
            for (int i = 0; i < count; i++) {
                double theta = rand.nextDouble() * Math.PI * 2D;
                double dist = r * (1.6D + rand.nextDouble() * 2D);
                double px = centre.x + Math.cos(theta) * dist;
                double py = centre.y + (rand.nextDouble() - 0.5D) * r * 0.6D;
                double pz = centre.z + Math.sin(theta) * dist;
                // Portal particles drift toward their spawn offset, so aim them at the centre.
                mc.world.spawnParticle(EnumParticleTypes.PORTAL, px, py, pz,
                        (centre.x - px) * 0.9D, (centre.y - py) * 0.9D, (centre.z - pz) * 0.9D);
            }
        }

        @Override
        void onRemoved() {
            if (hum != null) {
                hum.fadeOut();
            }
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            double r = radius(partialTicks);
            Vec3d c = centre.subtract(camera);
            float time = age + partialTicks;

            // Accretion disk.
            GlStateManager.pushMatrix();
            GlStateManager.translate(c.x, c.y, c.z);
            GlStateManager.rotate(18F, 1F, 0F, 0.3F);
            GlStateManager.rotate(time * 2.2F, 0F, 1F, 0F);
            disk(r * 1.25D, r * 3.2D, time);
            GlStateManager.popMatrix();

            // Lensing halo and photon ring.
            ring(c, r * 1.06D, r * 0.08D + 0.2D, 1F, 0.9F, 1F, 0.9F);
            ring(c, r * 1.15D, r * 0.6D, 0.45F, 0.15F, 0.95F, 0.3F);

            // The horizon itself: almost nothing comes back out.
            Cosmic.draw(() -> sphere(c, r), 6, 1F, 0.25F, 0.6F, 0.4F, 1F, true);
        }
    }

    /** A burning mark on the ground where a star is about to land. */
    public static class GroundMark extends Fx {
        final Vec3d at;
        final double radius;

        public GroundMark(Vec3d at, double radius, int lifetime) {
            super(lifetime);
            this.at = at;
            this.radius = radius;
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            float p = progress(partialTicks);
            float pulse = 0.6F + 0.4F * MathHelper.sin((age + partialTicks) * 1.2F);
            Vec3d c = at.subtract(camera).add(0, 0.1D, 0);
            ring(c, radius * (1.2D - p * 0.4D), 0.6D, 1F, 0.5F, 1F, 0.7F * pulse * p);
            ring(c, radius * 0.4D * p, 0.3D, 1F, 0.9F, 1F, 0.5F * p);
        }
    }

    /**
     * The Astral Regulator at work: rivers of light spiralling in from every direction,
     * and beams torn out of every open rift, all pouring into the crystal.
     */
    public static class Absorption extends Fx {
        final int entityId;
        final Supplier<Collection<Rift>> rifts;
        private int missing;

        public Absorption(int entityId, Supplier<Collection<Rift>> rifts) {
            super(Integer.MAX_VALUE);
            this.entityId = entityId;
            this.rifts = rifts;
        }

        @Override
        public boolean isDead() {
            return missing > 40;
        }

        @Override
        void tick() {
            Minecraft mc = Minecraft.getMinecraft();
            missing = mc.world != null && mc.world.getEntityByID(entityId) instanceof EntityAstralRegulator ? 0 : missing + 1;
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            Minecraft mc = Minecraft.getMinecraft();
            Entity entity = mc.world == null ? null : mc.world.getEntityByID(entityId);
            if (!(entity instanceof EntityAstralRegulator)) {
                return;
            }
            EntityAstralRegulator orb = (EntityAstralRegulator) entity;
            float progress = orb.isAbsorbing() ? orb.getProgress() : 0F;
            float fadeIn = orb.isAbsorbing() ? Math.min(1F, (age + partialTicks) / 40F) : 0.15F;
            Vec3d c = new Vec3d(
                    entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks,
                    entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks + 0.3D,
                    entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks).subtract(camera);
            float time = age + partialTicks;

            GlStateManager.disableDepth();
            int streams = 56;
            double reach = 60D;
            for (int i = 0; i < streams; i++) {
                // Evenly spread start directions on a sphere.
                double y = 1D - 2D * (i + 0.5D) / streams;
                double ring = Math.sqrt(1D - y * y);
                double phi = i * 2.399963D;
                Vec3d dir = new Vec3d(Math.cos(phi) * ring, y, Math.sin(phi) * ring);
                double spin = (i % 2 == 0 ? 1D : -1D) * (0.015D + progress * 0.04D);
                Vec3d previous = null;
                int segments = 18;
                for (int s = 0; s <= segments; s++) {
                    double t = s / (double) segments;
                    double angle = (1D - t) * 2.4D + time * spin;
                    double r = reach * Math.pow(1D - t, 1.3D) + 0.2D;
                    double cos = Math.cos(angle), sin = Math.sin(angle);
                    Vec3d p = c.add(new Vec3d(dir.x * cos - dir.z * sin, dir.y, dir.x * sin + dir.z * cos).scale(r));
                    if (previous != null) {
                        float flow = 0.5F + 0.5F * MathHelper.sin((float) (t * 14D - time * 0.45D + i));
                        float alpha = (float) (0.1D + 0.9D * t) * flow * (0.35F + 0.65F * progress) * fadeIn;
                        float white = (float) t;
                        double width = (0.12D + 0.7D * (1D - t)) * (0.6D + progress);
                        ribbon(previous, p, width, 0.55F + 0.45F * white, 0.3F + 0.7F * white, 1F, alpha);
                    }
                    previous = p;
                }
            }
            // Every open rift bleeds into it.
            for (Rift rift : rifts.get()) {
                if (rift.closeAge >= 0 || !rift.seenThroughWorld()) {
                    continue;
                }
                double[] pr = RiftRenderer.project(rift.x - camera.x, rift.y - camera.y, rift.z - camera.z, rift.height);
                Vec3d from = new Vec3d(pr[0], pr[1], pr[2]);
                float flicker = 0.6F + 0.4F * MathHelper.sin(time * 0.7F + rift.id);
                ribbon(from, c, 1.5D + progress * 3D, 0.7F, 0.4F, 1F, 0.35F * flicker * fadeIn);
                ribbon(from, c, 0.3D + progress, 1F, 0.95F, 1F, 0.6F * flicker * fadeIn);
            }
            GlStateManager.enableDepth();
        }
    }

    /** A ring of force racing outward. */
    public static class Shockwave extends Fx {
        final Vec3d centre;
        final double maxRadius;
        final boolean projected;

        public Shockwave(Vec3d centre, double maxRadius, int lifetime, boolean projected) {
            super(lifetime);
            this.centre = centre;
            this.maxRadius = maxRadius;
            this.projected = projected;
        }

        @Override
        void render(Vec3d camera, float partialTicks) {
            float p = progress(partialTicks);
            double radius = maxRadius * (1D - Math.pow(1D - p, 3D));
            Vec3d c = centre.subtract(camera);
            double scale = 1D;
            if (projected) {
                double[] pr = RiftRenderer.project(c.x, c.y, c.z, maxRadius);
                c = new Vec3d(pr[0], pr[1], pr[2]);
                scale = pr[3];
            }
            float fade = 1F - p;
            GlStateManager.disableTexture2D();
            ring(c, radius * scale, (3D + radius * 0.08D) * scale, 0.85F, 0.6F, 1F, 0.8F * fade);
            ring(c, radius * scale * 0.97D, (8D + radius * 0.2D) * scale, 0.4F, 0.1F, 0.9F, 0.35F * fade);
        }
    }

    public static void renderAll(List<Fx> effects, Vec3d camera, float partialTicks) {
        if (effects.isEmpty()) {
            return;
        }
        GlStateManager.pushMatrix();
        GlStateManager.disableFog();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        for (Fx fx : effects) {
            GlStateManager.disableTexture2D();
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            GlStateManager.depthMask(false);
            fx.render(camera, partialTicks);
        }
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.depthMask(true);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.enableCull();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }

    /** Puts back the state every effect is drawn in, for effects that change it part way. */
    public static void additiveState() {
        additive();
    }

    private static void additive() {
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        GlStateManager.depthMask(false);
    }

    public static void tickAll(List<Fx> effects) {
        Iterator<Fx> it = effects.iterator();
        while (it.hasNext()) {
            Fx fx = it.next();
            fx.tick();
            fx.age++;
            if (fx.isDead()) {
                fx.onRemoved();
                it.remove();
            }
        }
    }

    // ------------------------------------------------------------------ geometry

    /** Camera-facing strip between two camera-relative points. */
    private static void ribbon(Vec3d a, Vec3d b, double width, float r, float g, float bl, float alpha) {
        Vec3d side = sideVector(a, b).scale(width);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(a.x - side.x, a.y - side.y, a.z - side.z).color(r, g, bl, 0F).endVertex();
        buffer.pos(a.x, a.y, a.z).color(r, g, bl, alpha).endVertex();
        buffer.pos(b.x - side.x, b.y - side.y, b.z - side.z).color(r, g, bl, 0F).endVertex();
        buffer.pos(b.x, b.y, b.z).color(r, g, bl, alpha).endVertex();
        tessellator.draw();
        buffer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(a.x, a.y, a.z).color(r, g, bl, alpha).endVertex();
        buffer.pos(a.x + side.x, a.y + side.y, a.z + side.z).color(r, g, bl, 0F).endVertex();
        buffer.pos(b.x, b.y, b.z).color(r, g, bl, alpha).endVertex();
        buffer.pos(b.x + side.x, b.y + side.y, b.z + side.z).color(r, g, bl, 0F).endVertex();
        tessellator.draw();
    }

    private static void ribbonGeometry(Vec3d a, Vec3d b, double width) {
        Vec3d side = sideVector(a, b).scale(width);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        buffer.pos(a.x - side.x, a.y - side.y, a.z - side.z).endVertex();
        buffer.pos(a.x + side.x, a.y + side.y, a.z + side.z).endVertex();
        buffer.pos(b.x + side.x, b.y + side.y, b.z + side.z).endVertex();
        buffer.pos(b.x - side.x, b.y - side.y, b.z - side.z).endVertex();
        tessellator.draw();
    }

    private static Vec3d sideVector(Vec3d a, Vec3d b) {
        Vec3d dir = b.subtract(a);
        Vec3d mid = a.add(b).scale(0.5D);
        Vec3d side = dir.crossProduct(mid);
        return side.lengthSquared() < 1.0E-8 ? new Vec3d(0, 1, 0) : side.normalize();
    }

    /** Flat disk of streaming light, brightest at its inner edge. */
    private static void disk(double inner, double outer, float time) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        int segments = 96;
        buffer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i <= segments; i++) {
            double angle = Math.PI * 2D * i / segments;
            float streak = 0.55F + 0.45F * MathHelper.sin((float) angle * 7F + time * 0.3F) * MathHelper.sin((float) angle * 3F - time * 0.11F);
            double cos = Math.cos(angle), sin = Math.sin(angle);
            buffer.pos(cos * inner, 0, sin * inner).color(1F, 0.85F, 1F, 0.85F * streak).endVertex();
            buffer.pos(cos * outer, 0, sin * outer).color(0.35F, 0.05F, 0.9F, 0F).endVertex();
        }
        tessellator.draw();
    }

    /** Horizontal ring with a soft inner and outer edge. */
    private static void ring(Vec3d c, double radius, double thickness, float r, float g, float b, float alpha) {
        if (radius <= 0D || alpha <= 0F) {
            return;
        }
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        int segments = 64;
        for (int pass = 0; pass < 2; pass++) {
            double inner = pass == 0 ? radius - thickness : radius;
            double outer = pass == 0 ? radius : radius + thickness;
            float innerAlpha = pass == 0 ? 0F : alpha;
            float outerAlpha = pass == 0 ? alpha : 0F;
            buffer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
            for (int i = 0; i <= segments; i++) {
                double angle = Math.PI * 2D * i / segments;
                double cos = Math.cos(angle), sin = Math.sin(angle);
                buffer.pos(c.x + cos * Math.max(0D, inner), c.y, c.z + sin * Math.max(0D, inner)).color(r, g, b, innerAlpha).endVertex();
                buffer.pos(c.x + cos * outer, c.y, c.z + sin * outer).color(r, g, b, outerAlpha).endVertex();
            }
            tessellator.draw();
        }
    }

    private static void sphere(Vec3d c, double radius) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        int stacks = 12, slices = 18;
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        for (int i = 0; i < stacks; i++) {
            double t0 = Math.PI * i / stacks - Math.PI / 2D;
            double t1 = Math.PI * (i + 1) / stacks - Math.PI / 2D;
            for (int j = 0; j < slices; j++) {
                double p0 = Math.PI * 2D * j / slices;
                double p1 = Math.PI * 2D * (j + 1) / slices;
                vertex(buffer, c, radius, t0, p0);
                vertex(buffer, c, radius, t0, p1);
                vertex(buffer, c, radius, t1, p1);
                vertex(buffer, c, radius, t1, p0);
            }
        }
        tessellator.draw();
    }

    private static void vertex(BufferBuilder buffer, Vec3d c, double r, double theta, double phi) {
        double cos = Math.cos(theta);
        buffer.pos(c.x + r * cos * Math.cos(phi), c.y + r * Math.sin(theta), c.z + r * cos * Math.sin(phi)).endVertex();
    }

    /** A sphere around the camera, just inside the far clip, for turning the sky into space. */
    public static void skySphere(double radius) {
        sphere(Vec3d.ZERO, radius);
    }
}
