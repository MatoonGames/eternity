package com.matoon.eternity.client;

import com.matoon.eternity.EternityConfig;
import com.matoon.eternity.client.render.FxRenderer;
import com.matoon.eternity.client.render.GlSnapshot;
import com.matoon.eternity.client.render.WarlockFx;
import com.matoon.eternity.client.sound.EntityLoopSound;
import com.matoon.eternity.client.sound.SoundFx;
import com.matoon.eternity.entity.EntityAdamWarlock;
import com.matoon.eternity.network.MsgTribunalFx;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.FOVUpdateEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Adam Warlock from where you stand: his effects, the embers that stream off him, the roar
 * of the air around him and the hum of his power, and what it is like when he passes close
 * or hits you: the view jolting, a flash of gold, the world rushing past at the edges.
 */
@SideOnly(Side.CLIENT)
public final class WarlockClient {

    public static final WarlockClient INSTANCE = new WarlockClient();

    private final Minecraft mc = Minecraft.getMinecraft();
    private final List<FxRenderer.Fx> effects = new ArrayList<>();
    private final List<Ember> embers = new ArrayList<>();
    private final Map<Integer, Sounds> sounds = new HashMap<>();
    private final Random rand = new Random();

    private int tick;
    private float shake;
    private float flash;
    /** How hard the world is rushing past at the edges of the view, 0 to 1. */
    private float rush;
    private float fovKick;

    /** A spark of his power, cooling from white through gold to ember as it drifts. */
    private static final class Ember {
        double x, y, z, vx, vy, vz;
        int age;
        final int life;
        final float size;

        Ember(double x, double y, double z, double vx, double vy, double vz, int life, float size) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.vx = vx;
            this.vy = vy;
            this.vz = vz;
            this.life = life;
            this.size = size;
        }
    }

    /** The air tearing round him and the hum of his power, riding along with him. */
    private static final class Sounds {
        EntityLoopSound wind;
        EntityLoopSound hum;
        int seen;
    }

    private WarlockClient() {
    }

    // ------------------------------------------------------------------ what the server tells us

    public void effect(MsgTribunalFx m) {
        if (mc.world == null || mc.player == null) {
            return;
        }
        Vec3d at = new Vec3d(m.x, m.y, m.z);
        Vec3d to = new Vec3d(m.a, m.b, m.c);
        double dist = mc.player.getPositionVector().distanceTo(at);
        long seed = rand.nextLong();
        switch (m.type) {
            case MsgTribunalFx.WARLOCK_BOOM:
                effects.add(new WarlockFx.Boom(at, to, m.time, seed));
                shake = Math.max(shake, 1.6F * closeness(dist, 30D));
                burst(at, 26, 0.6D, to.scale(-0.5D));
                break;
            case MsgTribunalFx.WARLOCK_IMPACT: {
                boolean me = m.id == mc.player.getEntityId();
                float weight = m.time == 0 ? 0.6F : 1F;
                if (me) {
                    // Struck yourself, you feel it rather than see it: the view jolts, a flash of gold,
                    // and on a big hit the world streaks past as you go flying.
                    shake = Math.max(shake, 4F * weight);
                    flash = Math.max(flash, 0.45F * weight);
                    if (m.time > 0) {
                        rush = 1F;
                        fovKick = Math.max(fovKick, 0.25F);
                    }
                    burst(at.add(to.scale(1.5D)), 12, 0.6D, to.scale(0.8D));
                    break;
                }
                effects.add(new WarlockFx.Impact(at, to, m.time, seed));
                shake = Math.max(shake, 2F * weight * closeness(dist, 24D));
                flash = Math.max(flash, 0.2F * weight * closeness(dist, 12D));
                burst(at, m.time == 0 ? 10 : 30, m.time == 0 ? 0.35D : 0.7D, to.scale(0.6D));
                break;
            }
            case MsgTribunalFx.WARLOCK_LAND:
                effects.add(new WarlockFx.Land(at, m.a, m.time));
                shake = Math.max(shake, 2.2F * closeness(dist, 40D));
                flash = Math.max(flash, 0.25F * closeness(dist, 16D));
                burst(at, 30, 0.5D, Vec3d.ZERO);
                dust(at.subtract(0D, 2.5D, 0D), m.a * 0.8D, 30);
                break;
            case MsgTribunalFx.WARLOCK_CHARGE:
                effects.add(new WarlockFx.Charge(at, m.a, m.id, m.time, seed));
                break;
            case MsgTribunalFx.WARLOCK_BOLT:
                effects.add(new WarlockFx.Bolt(at, to, m.id, m.time));
                break;
            case MsgTribunalFx.WARLOCK_BURST:
                for (FxRenderer.Fx fx : effects) {
                    if (fx instanceof WarlockFx.Bolt && ((WarlockFx.Bolt) fx).id == m.id) {
                        ((WarlockFx.Bolt) fx).burst = true;
                    }
                }
                effects.add(new WarlockFx.Burst(at, m.a, m.time, seed));
                shake = Math.max(shake, 1.2F * closeness(dist, 14D));
                burst(at, 22, 0.45D, Vec3d.ZERO);
                break;
            case MsgTribunalFx.WARLOCK_BEAM:
                // Only the newest instant of the beam is drawn, so it never piles up into a white bar.
                for (FxRenderer.Fx fx : effects) {
                    if (fx instanceof WarlockFx.Beam) {
                        ((WarlockFx.Beam) fx).replaced = true;
                    }
                }
                effects.add(new WarlockFx.Beam(at, to, m.id, m.time));
                shake = Math.max(shake, 0.5F * closeness(dist, 20D));
                Vec3d eye = mc.player.getPositionEyes(1F);
                Vec3d axis = to.subtract(at);
                double along = MathHelper.clamp(eye.subtract(at).dotProduct(axis) / Math.max(1.0E-6, axis.lengthSquared()), 0D, 1D);
                if (at.add(axis.scale(along)).distanceTo(eye) < 1.8D) {
                    // Caught in it: the view shudders and floods with gold.
                    shake = Math.max(shake, 1.6F);
                    flash = Math.max(flash, 0.22F);
                    rush = Math.max(rush, 0.35F);
                }
                if (tick % 2 == 0) {
                    burst(to, 4, 0.4D, at.subtract(to).normalize().scale(0.3D));
                    dust(to, 1.2D, 2);
                }
                break;
            case MsgTribunalFx.WARLOCK_MARK:
                effects.add(new WarlockFx.Mark(at, m.a, m.time));
                break;
            case MsgTribunalFx.WARLOCK_SLAM:
                effects.add(new WarlockFx.Slam(at, m.a, m.time, seed));
                shake = Math.max(shake, 4F * closeness(dist, 40D));
                flash = Math.max(flash, 0.35F * closeness(dist, 14D));
                burst(at.add(0D, 0.5D, 0D), 40, 0.8D, new Vec3d(0D, 0.4D, 0D));
                dust(at, m.a, 90);
                break;
            case MsgTribunalFx.WARLOCK_BLINK:
                effects.add(new WarlockFx.Blink(at, to, m.time));
                burst(at, 10, 0.3D, Vec3d.ZERO);
                burst(to, 14, 0.4D, Vec3d.ZERO);
                break;
            case MsgTribunalFx.WARLOCK_EMERGE:
                effects.add(new WarlockFx.Shards(at, true, m.a, m.time, seed));
                shake = Math.max(shake, 3.5F * closeness(dist, 50D));
                flash = Math.max(flash, 0.7F * closeness(dist, 30D));
                burst(at, 80, 1.1D, Vec3d.ZERO);
                break;
            case MsgTribunalFx.WARLOCK_SHATTER:
                effects.add(new WarlockFx.Shards(at, false, 4D, m.time, seed));
                shake = Math.max(shake, 1.5F * closeness(dist, 20D));
                burst(at, 30, 0.5D, Vec3d.ZERO);
                break;
            default:
                break;
        }
    }

    // ------------------------------------------------------------------ ticking

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || mc.world == null || mc.player == null || mc.isGamePaused()) {
            return;
        }
        tick++;
        FxRenderer.tickAll(effects);
        shake *= 0.88F;
        flash *= 0.86F;
        rush *= 0.9F;
        fovKick *= 0.85F;
        for (FxRenderer.Fx fx : effects) {
            if (fx instanceof WarlockFx.Bolt && tick % 2 == 0) {
                Vec3d p = ((WarlockFx.Bolt) fx).at(0F);
                embers.add(new Ember(p.x, p.y, p.z, rand.nextGaussian() * 0.03D, rand.nextGaussian() * 0.03D, rand.nextGaussian() * 0.03D,
                        8 + rand.nextInt(8), 0.08F));
            }
        }
        for (Entity entity : mc.world.loadedEntityList) {
            if (entity instanceof EntityAdamWarlock) {
                follow((EntityAdamWarlock) entity);
            }
        }
        Iterator<Map.Entry<Integer, Sounds>> it = sounds.entrySet().iterator();
        while (it.hasNext()) {
            Sounds s = it.next().getValue();
            if (tick - s.seen > 5) {
                s.wind.fadeOut();
                s.hum.fadeOut();
                it.remove();
            }
        }
        Iterator<Ember> ei = embers.iterator();
        while (ei.hasNext()) {
            Ember e = ei.next();
            e.x += e.vx;
            e.y += e.vy;
            e.z += e.vz;
            e.vx *= 0.9D;
            e.vy = e.vy * 0.9D + 0.004D;
            e.vz *= 0.9D;
            if (++e.age >= e.life) {
                ei.remove();
            }
        }
        if (embers.size() > 1500) {
            embers.subList(0, embers.size() - 1500).clear();
        }
    }

    /** Everything that rides along with him: embers, wind, hum, debris, and how it feels when he passes close. */
    private void follow(EntityAdamWarlock w) {
        Sounds s = sounds.get(w.getEntityId());
        if (s == null) {
            s = new Sounds();
            s.wind = new EntityLoopSound(SoundEvents.ITEM_ELYTRA_FLYING, SoundCategory.HOSTILE, w, 0F, 0.3F).pitch(1.1F);
            s.hum = new EntityLoopSound(SoundEvents.BLOCK_PORTAL_AMBIENT, SoundCategory.HOSTILE, w, 0F, 0.1F).pitch(0.7F);
            SoundFx.play(s.wind);
            SoundFx.play(s.hum);
            sounds.put(w.getEntityId(), s);
        }
        s.seen = tick;
        double speed = Math.sqrt(w.velX * w.velX + w.velY * w.velY + w.velZ * w.velZ);
        s.wind.setTarget((float) MathHelper.clamp((speed - 0.25D) * 1.4D, 0D, 2.5D));
        s.hum.setTarget(w.getState() == EntityAdamWarlock.State.COCOON ? 1.2F : MathHelper.clamp(w.power, 0F, 1.2F) * 0.9F);

        // Embers stream off him, more the brighter he burns and the faster he goes, left behind as he moves.
        float power = w.power;
        double rate = Math.max(0D, power - 0.2F) * 1.6D + speed * power * 2.5D;
        int count = (int) rate;
        if (rand.nextFloat() < rate - count) {
            count++;
        }
        boolean cocooned = w.getState() == EntityAdamWarlock.State.COCOON;
        for (int i = 0; i < count && !cocooned; i++) {
            double px = w.posX + (rand.nextDouble() - 0.5D) * 0.8D;
            double py = w.posY + rand.nextDouble() * 2D;
            double pz = w.posZ + (rand.nextDouble() - 0.5D) * 0.8D;
            embers.add(new Ember(px, py, pz, -w.velX * 0.25D + rand.nextGaussian() * 0.04D, -w.velY * 0.25D + rand.nextGaussian() * 0.04D + 0.01D,
                    -w.velZ * 0.25D + rand.nextGaussian() * 0.04D, 10 + rand.nextInt(18), 0.045F + rand.nextFloat() * 0.06F));
        }
        if (cocooned && tick % 3 == 0) {
            embers.add(new Ember(w.posX + rand.nextGaussian() * 0.5D, w.posY + 0.2D + rand.nextDouble() * 2.2D, w.posZ + rand.nextGaussian() * 0.5D,
                    0D, 0.02D, 0D, 25, 0.06F));
        }

        // Tearing through anything in his way, or skimming the ground, he throws up what he touches.
        if (speed > 0.8D) {
            BlockPos at = new BlockPos(w.posX, w.posY + 0.5D, w.posZ);
            for (BlockPos pos : new BlockPos[]{at, at.down(), at.up()}) {
                IBlockState state = mc.world.getBlockState(pos);
                if (state.getMaterial() != Material.AIR && state.getMaterial().isSolid()) {
                    int id = Block.getStateId(state);
                    for (int i = 0; i < 6; i++) {
                        mc.world.spawnParticle(EnumParticleTypes.BLOCK_CRACK, w.posX + rand.nextGaussian() * 0.5D, pos.getY() + 1D,
                                w.posZ + rand.nextGaussian() * 0.5D, w.velX * 0.4D + rand.nextGaussian() * 0.3D, 0.3D + rand.nextDouble() * 0.4D,
                                w.velZ * 0.4D + rand.nextGaussian() * 0.3D, id);
                    }
                }
            }
        }

        // When he roars past close by: the view jolts, the world rushes at the edges, the air hits you.
        if (speed > 1D) {
            double d = w.getDistance(mc.player);
            if (d < 7D) {
                float near = (float) (1D - d / 7D);
                rush = Math.max(rush, near);
                fovKick = Math.max(fovKick, 0.12F * near);
                shake = Math.max(shake, 1.2F * near);
                if (d < 5D && tick % 6 == 0) {
                    SoundFx.at(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, w.posX, w.posY + 1D, w.posZ, 1.5F, 0.5F);
                }
            }
        }
    }

    /** A spray of embers out of a point. */
    private void burst(Vec3d at, int count, double force, Vec3d drift) {
        for (int i = 0; i < count; i++) {
            Vec3d d = new Vec3d(rand.nextGaussian(), rand.nextGaussian(), rand.nextGaussian()).normalize().scale(force * (0.3D + rand.nextDouble()));
            embers.add(new Ember(at.x, at.y, at.z, d.x + drift.x, d.y + drift.y, d.z + drift.z, 12 + rand.nextInt(16), 0.045F + rand.nextFloat() * 0.06F));
        }
    }

    /** Ground thrown up in a ring around a point: whatever it is made of, and clouds of dust. */
    private void dust(Vec3d at, double reach, int count) {
        for (int i = 0; i < count; i++) {
            double a = rand.nextDouble() * Math.PI * 2D;
            double r = rand.nextDouble() * reach;
            double x = at.x + Math.cos(a) * r, z = at.z + Math.sin(a) * r;
            BlockPos ground = new BlockPos(x, at.y - 0.5D, z);
            IBlockState state = mc.world.getBlockState(ground);
            if (state.getMaterial() == Material.AIR) {
                state = mc.world.getBlockState(ground.down());
            }
            double out = 0.15D + rand.nextDouble() * 0.35D;
            if (state.getMaterial() != Material.AIR) {
                mc.world.spawnParticle(EnumParticleTypes.BLOCK_CRACK, x, at.y + 0.2D, z, Math.cos(a) * out, 0.3D + rand.nextDouble() * 0.5D,
                        Math.sin(a) * out, Block.getStateId(state));
            }
            if (i % 3 == 0) {
                mc.world.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL, x, at.y + 0.3D, z, Math.cos(a) * out * 0.5D, 0.05D, Math.sin(a) * out * 0.5D);
            }
        }
    }

    // ------------------------------------------------------------------ drawing

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Entity view = mc.getRenderViewEntity();
        if (mc.world == null || view == null || effects.isEmpty() && embers.isEmpty()) {
            return;
        }
        float pt = event.getPartialTicks();
        Vec3d camera = new Vec3d(view.lastTickPosX + (view.posX - view.lastTickPosX) * pt,
                view.lastTickPosY + (view.posY - view.lastTickPosY) * pt,
                view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * pt);
        // Everything drawn here comes after the world, before the hand and the HUD: whatever is
        // switched on or off must be put back exactly, or they are drawn wrong (fogged black, at night).
        GlSnapshot before = GlSnapshot.take();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
        FxRenderer.renderAll(effects, camera, pt);
        renderEmbers(camera, pt);
        before.restore();
    }

    private void renderEmbers(Vec3d camera, float pt) {
        if (embers.isEmpty()) {
            return;
        }
        float rx = ActiveRenderInfo.getRotationX(), rz = ActiveRenderInfo.getRotationZ();
        float ryz = ActiveRenderInfo.getRotationYZ(), rxy = ActiveRenderInfo.getRotationXY(), rxz = ActiveRenderInfo.getRotationXZ();
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableFog();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        GlStateManager.depthMask(false);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
        for (Ember e : embers) {
            float life = (e.age + pt) / e.life;
            if (life >= 1F) {
                continue;
            }
            // White, then gold, then the red of a dying spark.
            float r = 1F, g = life < 0.3F ? 0.95F - life * 0.6F : Math.max(0.2F, 0.77F - (life - 0.3F) * 0.9F);
            float b = Math.max(0F, life < 0.2F ? 0.75F - life * 3F : 0.12F * (1F - life));
            float a = (1F - life) * (life < 0.1F ? life / 0.1F : 1F);
            float s = e.size * (1F - life * 0.5F);
            double x = e.x + e.vx * pt - camera.x, y = e.y + e.vy * pt - camera.y, z = e.z + e.vz * pt - camera.z;
            // One drifting right past your eye would fill the screen; those fade out.
            a *= (float) MathHelper.clamp((Math.sqrt(x * x + y * y + z * z) - 1D) / 3D, 0D, 1D);
            if (a < 0.01F) {
                continue;
            }
            // A soft point: bright in the middle, fading to nothing at the corners.
            double[][] corners = {
                    {x - rx * s - ryz * s, y - rxz * s, z - rz * s - rxy * s},
                    {x - rx * s + ryz * s, y + rxz * s, z - rz * s + rxy * s},
                    {x + rx * s + ryz * s, y + rxz * s, z + rz * s + rxy * s},
                    {x + rx * s - ryz * s, y - rxz * s, z + rz * s - rxy * s}};
            for (int i = 0; i < 4; i++) {
                double[] c0 = corners[i], c1 = corners[(i + 1) % 4];
                buffer.pos(x, y, z).color(r, g, b, a).endVertex();
                buffer.pos(c0[0], c0[1], c0[2]).color(r, g, b, 0F).endVertex();
                buffer.pos(c1[0], c1[1], c1[2]).color(r, g, b, 0F).endVertex();
            }
        }
        tessellator.draw();
        GlStateManager.popMatrix();
    }

    /** A gold flash when he hits hard, and the world streaking past at the edges of the view when he roars by. */
    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Pre event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL || mc.world == null) {
            return;
        }
        float shownFlash = EternityConfig.client.reduceFlashing ? Math.min(flash, 0.15F) : flash;
        if (shownFlash < 0.01F && rush < 0.02F) {
            return;
        }
        ScaledResolution screen = event.getResolution();
        int w = screen.getScaledWidth(), h = screen.getScaledHeight();
        GlSnapshot before = GlSnapshot.take();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        if (shownFlash > 0.01F) {
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            buffer.pos(0, h, 0).color(1F, 0.85F, 0.5F, shownFlash).endVertex();
            buffer.pos(w, h, 0).color(1F, 0.85F, 0.5F, shownFlash).endVertex();
            buffer.pos(w, 0, 0).color(1F, 0.85F, 0.5F, shownFlash).endVertex();
            buffer.pos(0, 0, 0).color(1F, 0.85F, 0.5F, shownFlash).endVertex();
            tessellator.draw();
        }
        if (rush > 0.02F) {
            // Streaks rushing out from the middle of the view, only toward its edges.
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            Random lines = new Random(tick * 7919L);
            double cx = w / 2D, cy = h / 2D, reach = Math.sqrt(cx * cx + cy * cy);
            buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
            for (int i = 0; i < 46; i++) {
                double a = lines.nextDouble() * Math.PI * 2D;
                double r0 = reach * (0.45D + lines.nextDouble() * 0.3D), r1 = reach * 1.05D;
                double spread = 0.004D + lines.nextDouble() * 0.008D;
                float alpha = rush * (0.25F + lines.nextFloat() * 0.35F);
                buffer.pos(cx + Math.cos(a) * r0, cy + Math.sin(a) * r0, 0).color(1F, 0.95F, 0.8F, 0F).endVertex();
                buffer.pos(cx + Math.cos(a + spread) * r1, cy + Math.sin(a + spread) * r1, 0).color(1F, 0.9F, 0.7F, alpha).endVertex();
                buffer.pos(cx + Math.cos(a - spread) * r1, cy + Math.sin(a - spread) * r1, 0).color(1F, 0.9F, 0.7F, alpha).endVertex();
            }
            tessellator.draw();
        }
        before.restore();
    }

    @SubscribeEvent
    public void onCamera(EntityViewRenderEvent.CameraSetup event) {
        if (!EternityConfig.client.screenShake || shake < 0.001F) {
            return;
        }
        float t = tick + (float) event.getRenderPartialTicks();
        event.setYaw(event.getYaw() + (MathHelper.sin(t * 3.7F) + MathHelper.sin(t * 8.3F) * 0.5F) * shake * 0.5F);
        event.setPitch(event.getPitch() + (MathHelper.sin(t * 3.1F) + MathHelper.sin(t * 6.9F) * 0.5F) * shake * 0.5F);
        event.setRoll(event.getRoll() + MathHelper.sin(t * 2.3F) * shake * 0.8F);
    }

    @SubscribeEvent
    public void onFov(FOVUpdateEvent event) {
        if (fovKick > 0.005F) {
            event.setNewfov(event.getNewfov() * (1F + fovKick));
        }
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        mc.addScheduledTask(() -> {
            effects.clear();
            embers.clear();
            sounds.clear();
            shake = flash = rush = fovKick = 0F;
        });
    }

    private static float closeness(double dist, double range) {
        return (float) MathHelper.clamp(1D - dist / range, 0D, 1D);
    }
}
