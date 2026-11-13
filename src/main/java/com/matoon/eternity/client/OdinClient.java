package com.matoon.eternity.client;

import com.matoon.eternity.EternityConfig;
import com.matoon.eternity.EternitySounds;
import com.matoon.eternity.client.render.FxRenderer;
import com.matoon.eternity.client.render.GlSnapshot;
import com.matoon.eternity.client.render.OdinFx;
import com.matoon.eternity.entity.EntityOdin;
import com.matoon.eternity.network.MsgTribunalFx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Shader;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.client.shader.ShaderUniform;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.block.Block;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Odin from where you stand. When he comes, things go dark: the light drains out of the sky and the fog closes in
 * blue-black, and what light there is stands out hard against it, the shadows crushed and the colour thinned (a post
 * pass of the game's own colour shader, its uniforms eased each frame; without shaders, a veil over the screen does what
 * it can). The Bifrost and everything he does are drawn here (see {@link OdinFx}); the view shakes when it comes down
 * and when his blows land near you; and Sleipnir leaves runes of light where his hooves fall.
 * <p>
 * It also says what music is wanted: Prisms while the sky opens and after he has gone, his own while he fights.
 */
@SideOnly(Side.CLIENT)
public final class OdinClient {

    public static final OdinClient INSTANCE = new OdinClient();

    private final Minecraft mc = Minecraft.getMinecraft();
    private final List<FxRenderer.Fx> effects = new ArrayList<>();
    private final Map<Integer, OdinFx.Spear> spears = new HashMap<>();
    private final Map<Integer, OdinFx.Raven> ravens = new HashMap<>();
    private final Random rand = new Random();

    private int tick;
    /** How dark it is round him now, where it is going, and how fast. */
    private float dark, darkTo, darkStep;
    /** The music the server last asked for (0 none, 1 Prisms, 2 his), and until when Prisms lingers after he has gone. */
    private int music;
    private float shake, flash;

    private OdinClient() {
    }

    // ------------------------------------------------------------------ messages

    public void effect(MsgTribunalFx m) {
        if (mc.world == null || mc.player == null) {
            return;
        }
        Vec3d at = new Vec3d(m.x, m.y, m.z);
        Vec3d to = new Vec3d(m.a, m.b, m.c);
        double dist = mc.player.getPositionVector().distanceTo(at);
        long seed = rand.nextLong();
        switch (m.type) {
            case MsgTribunalFx.ODIN_BIFROST:
                effects.add(new OdinFx.Bifrost(at, m.a, (int) m.b, m.time, seed));
                if (m.b > 0D) {
                    // Heard everywhere at once: it is the sky itself.
                    mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(EternitySounds.ODIN_BIFROST, 1F));
                    shake = Math.max(shake, 7F * closeness(dist, 90D) + 1.5F);
                    flash = Math.max(flash, 0.9F * closeness(dist, 120D));
                } else {
                    mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(EternitySounds.ODIN_BIFROST, 1.15F));
                    shake = Math.max(shake, 3F * closeness(dist, 60D));
                }
                break;
            case MsgTribunalFx.ODIN_SEAL:
                effects.add(new OdinFx.Seal(at, m.a, m.time, seed));
                break;
            case MsgTribunalFx.ODIN_DARK:
                darkTo = (float) MathHelper.clamp(m.a, 0D, 1D);
                darkStep = (float) (Math.abs(darkTo - dark) / Math.max(1D, m.b));
                music = (int) m.c;
                break;
            case MsgTribunalFx.ODIN_SPEAR: {
                OdinFx.Spear s = spears.get(m.id);
                if (s == null || s.isDead()) {
                    s = new OdinFx.Spear(m.id, at);
                    spears.put(m.id, s);
                    effects.add(s);
                }
                s.update(at, to, m.time == 1);
                break;
            }
            case MsgTribunalFx.ODIN_SPEAR_HIT: {
                OdinFx.Spear s = spears.get(m.id);
                if (s != null) {
                    s.end();
                    spears.remove(m.id);
                }
                double size = m.a > 0D ? m.a : 1.2D;
                effects.add(new OdinFx.SpearHit(at, size, seed));
                shake = Math.max(shake, 3F * closeness(dist, 24D));
                flash = Math.max(flash, 0.25F * closeness(dist, 14D));
                break;
            }
            case MsgTribunalFx.ODIN_RING:
                effects.add(new OdinFx.Ring(at, m.a, m.time, seed));
                shake = Math.max(shake, 4F * closeness(dist, 30D));
                break;
            case MsgTribunalFx.ODIN_MARK:
                effects.add(new OdinFx.Mark(at, m.a, Math.max(1, m.time), seed));
                break;
            case MsgTribunalFx.ODIN_STRIKE:
                effects.add(new OdinFx.Strike(at, m.a, seed));
                effects.add(new OdinFx.Flare(at.add(0D, 1D, 0D), 1.2D, seed));
                shake = Math.max(shake, 3.5F * closeness(dist, 30D));
                flash = Math.max(flash, 0.3F * closeness(dist, 16D));
                break;
            case MsgTribunalFx.ODIN_RAVEN: {
                OdinFx.Raven r = ravens.get(m.id);
                if (m.time == 2) {
                    if (r != null) {
                        r.end();
                        ravens.remove(m.id);
                    }
                    for (int i = 0; i < 12; i++) {
                        mc.world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, m.x, m.y, m.z, rand.nextGaussian() * 0.05D, 0.05D, rand.nextGaussian() * 0.05D);
                    }
                    break;
                }
                if (r == null || r.isDead()) {
                    r = new OdinFx.Raven(m.id, at);
                    ravens.put(m.id, r);
                    effects.add(r);
                }
                r.update(at, to, m.time == 1);
                if (m.time == 1) {
                    for (int i = 0; i < 6; i++) {
                        mc.world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, m.x, m.y, m.z, rand.nextGaussian() * 0.08D, rand.nextGaussian() * 0.08D,
                                rand.nextGaussian() * 0.08D);
                    }
                }
                break;
            }
            case MsgTribunalFx.ODIN_STOMP: {
                boolean step = m.time == 1;
                effects.add(new OdinFx.Stomp(at, step ? 0.9D : m.a, step, seed));
                if (!step) {
                    shake = Math.max(shake, 5F * closeness(dist, 28D));
                    int state = Block.getStateId(mc.world.getBlockState(new net.minecraft.util.math.BlockPos(at).down()));
                    for (int i = 0; i < 40; i++) {
                        double ang = rand.nextDouble() * Math.PI * 2D, r = rand.nextDouble() * m.a;
                        mc.world.spawnParticle(EnumParticleTypes.BLOCK_DUST, at.x + Math.cos(ang) * r, at.y + 0.1D, at.z + Math.sin(ang) * r,
                                Math.cos(ang) * 0.2D, 0.3D + rand.nextDouble() * 0.3D, Math.sin(ang) * 0.2D, state);
                    }
                } else {
                    shake = Math.max(shake, 0.6F * closeness(dist, 10D));
                }
                break;
            }
            case MsgTribunalFx.ODIN_GATHER:
                effects.add(new OdinFx.Gather(m.id, Math.max(1, m.time), seed));
                break;
            case MsgTribunalFx.ODIN_FLARE:
                effects.add(new OdinFx.Flare(at, m.a, seed));
                flash = Math.max(flash, 0.5F * closeness(dist, 40D));
                break;
            case MsgTribunalFx.ODIN_VORTEX:
                effects.add(new OdinFx.Vortex(at, m.b, m.a, Math.max(1, m.time), seed));
                break;
            case MsgTribunalFx.ODIN_PULL:
                effects.add(new OdinFx.Pull((int) m.a, at, m.id, Math.max(1, m.time), seed));
                break;
            case MsgTribunalFx.ODIN_DUST: {
                Entity e = mc.world.getEntityByID(m.id);
                Vec3d c = e != null ? e.getPositionVector() : at;
                int sand = Block.getStateId(Blocks.SOUL_SAND.getDefaultState());
                for (int i = 0; i < 160; i++) {
                    mc.world.spawnParticle(EnumParticleTypes.FALLING_DUST, c.x + rand.nextGaussian() * 1.2D, c.y + rand.nextDouble() * 3.6D,
                            c.z + rand.nextGaussian() * 1.4D, 0D, 0D, 0D, sand);
                }
                break;
            }
            default:
                break;
        }
    }

    // ------------------------------------------------------------------ what is wanted

    /** How dark it is round him, 0 to 1. */
    public float darkness() {
        return dark;
    }

    /** The music wanted now, or null for none of his. */
    @Nullable
    public SoundEvent wantsMusic() {
        if (music == 1) {
            return EternitySounds.MUSIC_PRISMS;
        }
        if (music == 2 && odinNear()) {
            return EternitySounds.MUSIC_ODIN;
        }
        return null;
    }

    /** Whether the post pass that darkens everything round him is wanted. */
    public boolean wantsShader() {
        return dark > 0.01F;
    }

    private boolean odinNear() {
        for (Entity e : mc.world.loadedEntityList) {
            if (e instanceof EntityOdin && !e.isDead && e.getDistanceSq(mc.player) < 192D * 192D) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ ticking

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (mc.world == null) {
            if (!effects.isEmpty() || dark > 0F) {
                clear();
            }
            return;
        }
        if (mc.isGamePaused()) {
            return;
        }
        tick++;
        if (dark < darkTo) {
            dark = Math.min(darkTo, dark + darkStep);
        } else if (dark > darkTo) {
            dark = Math.max(darkTo, dark - darkStep);
        }
        shake *= 0.86F;
        flash *= 0.85F;
        FxRenderer.tickAll(effects);
        spears.values().removeIf(FxRenderer.Fx::isDead);
        ravens.values().removeIf(FxRenderer.Fx::isDead);
        // Where Sleipnir runs, runes of light where his hooves come down.
        for (Entity e : mc.world.loadedEntityList) {
            if (!(e instanceof EntityOdin) || e.isDead || e.getDistanceSq(mc.player) > 96D * 96D) {
                continue;
            }
            EntityOdin o = (EntityOdin) e;
            double speed = Math.sqrt(o.velX * o.velX + o.velZ * o.velZ);
            if (o.running > 0.5F && speed > 0.25D && tick % 3 == 0) {
                Vec3d fwd = o.forwardOf(), right = new Vec3d(-fwd.z, 0D, fwd.x);
                double along = rand.nextBoolean() ? 1.3D : -0.9D;
                Vec3d hoof = o.getPositionVector().add(fwd.scale(along)).add(right.scale(rand.nextBoolean() ? 0.3D : -0.3D));
                effects.add(new OdinFx.Stomp(hoof.add(0D, 0.02D, 0D), 0.9D, true, rand.nextLong()));
            }
        }
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Entity view = mc.getRenderViewEntity();
        if (mc.world == null || view == null || effects.isEmpty()) {
            return;
        }
        float pt = event.getPartialTicks();
        Vec3d camera = new Vec3d(view.lastTickPosX + (view.posX - view.lastTickPosX) * pt,
                view.lastTickPosY + (view.posY - view.lastTickPosY) * pt,
                view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * pt);
        GlSnapshot before = GlSnapshot.take();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
        FxRenderer.renderAll(effects, camera, pt);
        before.restore();
    }

    /**
     * Each frame, while the darkening pass is on: the colour shader's scale pushed up and its offset down (the
     * mid-tones sink, the shadows go to black, the light stays light), the colour thinned, a cold cast over it.
     */
    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !"eternity:odin".equals(EternityClient.INSTANCE.shaderName())) {
            return;
        }
        ShaderGroup group = mc.entityRenderer.getShaderGroup();
        if (group == null) {
            return;
        }
        float k = dark;
        try {
            List<Shader> shaders = ObfuscationReflectionHelper.getPrivateValue(ShaderGroup.class, group, "field_148031_d");
            for (Shader shader : shaders) {
                ShaderUniform scale = shader.getShaderManager().getShaderUniform("ColorScale");
                ShaderUniform offset = shader.getShaderManager().getShaderUniform("Offset");
                ShaderUniform saturation = shader.getShaderManager().getShaderUniform("Saturation");
                if (scale != null && offset != null && saturation != null) {
                    scale.set(1F + 0.42F * k, 1F + 0.42F * k, 1F + 0.5F * k);
                    offset.set(-0.27F * k, -0.26F * k, -0.22F * k);
                    saturation.set(1F - 0.38F * k);
                }
            }
        } catch (RuntimeException ignored) {
            // A shader of another shape: leave it be.
        }
    }

    /** The fog closes in, blue-black. */
    @SubscribeEvent
    public void onFogColour(EntityViewRenderEvent.FogColors event) {
        if (dark <= 0.001F) {
            return;
        }
        float k = dark * 0.88F;
        event.setRed(event.getRed() + (0.02F - event.getRed()) * k);
        event.setGreen(event.getGreen() + (0.025F - event.getGreen()) * k);
        event.setBlue(event.getBlue() + (0.05F - event.getBlue()) * k);
    }

    @SubscribeEvent
    public void onFog(EntityViewRenderEvent.RenderFogEvent event) {
        if (dark <= 0.001F) {
            return;
        }
        float far = event.getFarPlaneDistance();
        net.minecraft.client.renderer.GlStateManager.setFogStart(far * (0.75F - 0.55F * dark) * 0.25F);
        net.minecraft.client.renderer.GlStateManager.setFogEnd(far * (1F - 0.45F * dark));
    }

    @SubscribeEvent
    public void onCamera(EntityViewRenderEvent.CameraSetup event) {
        if (!EternityConfig.client.screenShake || shake < 0.001F) {
            return;
        }
        float t = tick + (float) event.getRenderPartialTicks();
        event.setYaw(event.getYaw() + (MathHelper.sin(t * 3.9F) + MathHelper.sin(t * 8.1F) * 0.5F) * shake * 0.5F);
        event.setPitch(event.getPitch() + (MathHelper.sin(t * 3.3F) + MathHelper.sin(t * 7.3F) * 0.5F) * shake * 0.5F);
        event.setRoll(event.getRoll() + MathHelper.sin(t * 2.1F) * shake * 0.7F);
    }

    /** Over the screen: a veil of dark where there is no shader to darken things, and the white of the Bifrost striking. */
    @SubscribeEvent
    public void onOverlay(net.minecraftforge.client.event.RenderGameOverlayEvent.Post event) {
        if (event.getType() != net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }
        float veil = !"eternity:odin".equals(EternityClient.INSTANCE.shaderName()) ? dark * 0.5F : 0F;
        if (veil < 0.005F && flash < 0.01F) {
            return;
        }
        int w = event.getResolution().getScaledWidth(), h = event.getResolution().getScaledHeight();
        net.minecraft.client.renderer.GlStateManager.disableDepth();
        net.minecraft.client.renderer.GlStateManager.enableBlend();
        if (veil >= 0.005F) {
            net.minecraft.client.gui.Gui.drawRect(0, 0, w, h, (int) (veil * 255F) << 24 | 0x04060C);
        }
        if (flash >= 0.01F) {
            net.minecraft.client.gui.Gui.drawRect(0, 0, w, h, (int) (Math.min(1F, flash) * 200F) << 24 | 0xEAF6FF);
        }
        net.minecraft.client.renderer.GlStateManager.enableDepth();
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        mc.addScheduledTask(this::clear);
    }

    private void clear() {
        effects.clear();
        spears.clear();
        ravens.clear();
        dark = darkTo = 0F;
        music = 0;
        shake = flash = 0F;
    }

    private static float closeness(double dist, double range) {
        return (float) MathHelper.clamp(1D - dist / range, 0D, 1D);
    }
}
