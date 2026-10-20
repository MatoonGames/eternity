package com.matoon.eternity.client;

import com.matoon.eternity.EternityConfig;
import com.matoon.eternity.EternitySounds;
import com.matoon.eternity.client.render.CometRenderer;
import com.matoon.eternity.client.render.FxRenderer;
import com.matoon.eternity.client.render.ScreenFx;
import com.matoon.eternity.client.sound.LoopSound;
import com.matoon.eternity.client.sound.SoundFx;
import com.matoon.eternity.entity.EntityCosmicComet;
import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.network.MsgComet;
import com.matoon.eternity.network.MsgStrikeComet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Everything the player sees, hears and feels of the Cosmic Comet: the comet itself
 * wherever it is in their dimension, its roar, the heat haze and shaking as they
 * close in, and the breaking open.
 */
@SideOnly(Side.CLIENT)
public final class CometClient {

    public static final CometClient INSTANCE = new CometClient();

    private static final int CRACK_TICKS = 100;
    private static final int DEPART_TICKS = 110;

    private final Minecraft mc = Minecraft.getMinecraft();
    private final Random rand = new Random();
    private final List<FxRenderer.Fx> effects = new ArrayList<>();

    private boolean active;
    private boolean space;
    private byte stage;
    private int stageTicks;
    private int sinceSync;
    /** Where it is drawn, last tick and this tick. */
    private double px, py, pz, x, y, z;
    /** Where the server last put it, carried forward by its velocity. */
    private double tx, ty, tz;
    private double vx, vy, vz;
    private Vec3d heading = new Vec3d(1, 0, 0);
    private float radius = 7F;
    private float integrity = 1F;
    private float fade;
    private EntityCosmicComet body;

    private int tick;
    private float heat;
    private float shake;
    private float flash;
    private float flashR = 1F, flashG = 1F, flashB = 1F;
    private LoopSound roar;
    /** How hard its light is shining into your eyes, 0 to 1. */
    private float glare;
    private boolean occluded;

    /** Where the Regulator it left behind hangs, while nobody has taken it. */
    private Vec3d relic;
    private int relicSeen;

    private CometClient() {
    }

    public void handle(MsgComet m) {
        if (mc.world == null || mc.player == null) {
            return;
        }
        Vec3d at = new Vec3d(m.x, m.y, m.z);
        double dist = mc.player.getPositionEyes(1F).distanceTo(at);
        switch (m.type) {
            case MsgComet.SYNC:
                onSync(m);
                break;
            case MsgComet.HIDE:
                hide();
                break;
            case MsgComet.APPEAR: {
                double angle = SoundFx.angleTo(m.x, m.z);
                SoundFx.carried(EternitySounds.FAR_HORN, 1F, 0.6F, angle);
                SoundFx.carried(EternitySounds.FAR_RIFT, 0.6F, 1.4F, angle);
                announce(m.space ? "eternity.comet.appear_space" : "eternity.comet.appear");
                break;
            }
            case MsgComet.HIT:
                sparks(at, 14, 0.4D);
                stereo(EternitySounds.COMET_IMPACT, closeness(dist, m.radius + 96D), 0.85F + rand.nextFloat() * 0.3F);
                if (dist < m.radius + 16D) {
                    flash(0.12F, 1F, 0.85F, 0.6F);
                    shake = Math.max(shake, 0.4F);
                }
                break;
            case MsgComet.REPELLED:
                for (int i = 0; i < 10; i++) {
                    mc.world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, true, m.x, m.y, m.z,
                            rand.nextGaussian() * 0.1D, rand.nextGaussian() * 0.1D, rand.nextGaussian() * 0.1D);
                    mc.world.spawnParticle(EnumParticleTypes.FLAME, true, m.x, m.y, m.z,
                            rand.nextGaussian() * 0.15D, rand.nextGaussian() * 0.15D, rand.nextGaussian() * 0.15D);
                }
                break;
            case MsgComet.FLARE: {
                effects.add(new FxRenderer.Shockwave(at, m.radius, 40, true));
                float close = closeness(dist, m.radius * 2D);
                stereo(EternitySounds.COMET_BLAST, 0.3F + 0.9F * close, 0.9F + rand.nextFloat() * 0.2F);
                flash(0.25F + 0.4F * close, 1F, 0.65F, 0.3F);
                shake = Math.max(shake, 0.6F + 2F * close);
                break;
            }
            case MsgComet.CRACK:
                flash(0.35F, 1F, 0.9F, 0.7F);
                SoundFx.carried(EternitySounds.FAR_HORN, 1F, 0.45F, SoundFx.angleTo(m.x, m.z));
                break;
            case MsgComet.SHATTER:
                shatter(at, m.radius, dist);
                break;
            case MsgComet.RELIC:
                relic = at;
                relicSeen = 0;
                break;
            case MsgComet.RELIC_GONE:
                relic = null;
                break;
            case MsgComet.DEPART:
                announce("eternity.comet.depart");
                break;
            default:
                break;
        }
    }

    private void onSync(MsgComet m) {
        if (!active) {
            active = true;
            fade = 0F;
            px = x = tx = m.x;
            py = y = ty = m.y;
            pz = z = tz = m.z;
        } else {
            tx = m.x;
            ty = m.y;
            tz = m.z;
        }
        vx = m.vx;
        vy = m.vy;
        vz = m.vz;
        if (vx * vx + vy * vy + vz * vz > 1.0E-6) {
            heading = new Vec3d(vx, vy, vz).normalize();
        }
        radius = m.radius;
        integrity = m.integrity;
        space = m.space;
        if (m.stage != stage) {
            stage = m.stage;
            stageTicks = 0;
        }
        sinceSync = 0;
    }

    /** It bursts: shards of the night flung outward, a blinding flash, and a cloud of light left behind. */
    private void shatter(Vec3d at, float rock, double dist) {
        // The rock is small, but it breaks with the force of something far bigger.
        float r = rock * 2F;
        effects.add(new CometRenderer.Shards(at, r));
        effects.add(new CometRenderer.Nebula(at, r));
        effects.add(new CometRenderer.Reveal(at, r));
        effects.add(new FxRenderer.Shockwave(at, r * 14D, 60, true));
        effects.add(new FxRenderer.Shockwave(at, r * 30D, 140, true));
        float close = closeness(dist, r * 40D);
        flash(1F, 1F, 1F, 1F);
        shake = Math.max(shake, 1F + 3F * close);
        stereo(EternitySounds.COMET_BLAST, 0.5F + 1F * close, 0.6F);
        stereo(EternitySounds.COMET_IMPACT, 0.4F + 1F * close, 0.5F);
        SoundFx.everywhere(SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 0.3F + 0.9F * close, 0.4F);
        SoundFx.carried(EternitySounds.FAR_HORN, 1F, 0.5F, SoundFx.angleTo(at.x, at.z));
        sparks(at, 120, 2D);
        hide();
    }

    private void hide() {
        active = false;
        body = null;
        heat = 0F;
        if (roar != null) {
            roar.fadeOut();
            roar = null;
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
        shake *= 0.9F;
        flash *= 0.88F;
        if (relic != null && ++relicSeen > 200) {
            relic = null;
        }
        if (!active) {
            glare = 0F;
            return;
        }
        if (++sinceSync > 100) {
            hide();
            return;
        }
        stageTicks++;
        tx += vx;
        ty += vy;
        tz += vz;
        px = x;
        py = y;
        pz = z;
        x += (tx - x) * 0.3D;
        y += (ty - y) * 0.3D;
        z += (tz - z) * 0.3D;
        float target = stage == MsgComet.DEPARTING ? Math.max(0F, 1F - stageTicks / (float) DEPART_TICKS) : 1F;
        fade += (target - fade) * (stage == MsgComet.DEPARTING ? 0.2F : 0.04F);

        if (body != null && (body.isDead || body.world != mc.world)) {
            body = null;
        }
        if (body == null && tick % 10 == 0) {
            body = findBody();
        }

        Vec3d c = centre(1F);
        double dist = mc.player.getPositionEyes(1F).distanceTo(c);
        double range = space ? EternityConfig.comet.spaceHeatRange : EternityConfig.comet.overworldHeatRange;
        heat = (float) MathHelper.clamp(1D - (dist - radius) / range, 0D, 1D);
        shake = Math.max(shake, heat * heat * 0.8F);
        if (stage == MsgComet.CRACKING) {
            float p = Math.min(1F, stageTicks / (float) CRACK_TICKS);
            shake = Math.max(shake, p * (0.5F + 2.5F * closeness(dist, radius * 20D)));
        }
        updateRoar(dist);
        updateGlare(c, dist);
        if (dist < radius * 6D + 300D) {
            trail(c);
        }
    }

    /**
     * Look straight at it and its light floods your eyes, unless something stands in
     * the way. Up close it is a rock, not a point of light, and the flare fades out.
     */
    private void updateGlare(Vec3d c, double dist) {
        Vec3d eyes = mc.player.getPositionEyes(1F);
        Vec3d toward = c.subtract(eyes).normalize();
        if (tick % 5 == 0) {
            occluded = mc.world.rayTraceBlocks(eyes, eyes.add(toward.scale(Math.min(dist, 256D))), false, true, false) != null;
        }
        float facing = smoothstep(0.82F, 1F, (float) mc.player.getLook(1F).dotProduct(toward));
        float distant = smoothstep((float) (radius * 12D + 40D), (float) (radius * 40D + 200D), (float) dist);
        float target = occluded ? 0F : facing * fade * distant * 0.85F;
        glare += (target - glare) * 0.3F;
    }

    private void updateRoar(double dist) {
        float volume = (float) MathHelper.clamp(1D - dist / (radius * 40D + 600D), 0D, 1D);
        volume = volume * volume * 1.3F + (stage == MsgComet.CRACKING ? 0.6F : 0F);
        volume *= fade;
        if (roar == null || roar.isDonePlaying()) {
            roar = new LoopSound(EternitySounds.COMET_FLYBY, SoundCategory.HOSTILE, true, 0.05F).pitch(space ? 0.75F : 1F);
            SoundFx.play(roar);
        }
        roar.setTarget(volume);
    }

    /** Sparks shed from its heart and drifting down its tail. */
    private void trail(Vec3d c) {
        for (int i = 0; i < 8; i++) {
            Vec3d dir = new Vec3d(rand.nextGaussian(), rand.nextGaussian(), rand.nextGaussian()).normalize();
            Vec3d at = c.add(dir.scale(radius * (1.05D + rand.nextDouble() * 0.5D)));
            mc.world.spawnParticle(EnumParticleTypes.FIREWORKS_SPARK, true, at.x, at.y, at.z,
                    dir.x * 0.1D - heading.x * 0.4D, dir.y * 0.1D - heading.y * 0.4D, dir.z * 0.1D - heading.z * 0.4D);
        }
        for (int i = 0; i < 10; i++) {
            double u = rand.nextDouble() * rand.nextDouble();
            double spread = radius * (0.5D + u * 5D);
            Vec3d at = c.subtract(heading.scale(radius * (1D + u * 40D)))
                    .add(rand.nextGaussian() * spread, rand.nextGaussian() * spread, rand.nextGaussian() * spread);
            mc.world.spawnParticle(i % 3 == 0 ? EnumParticleTypes.FIREWORKS_SPARK : EnumParticleTypes.END_ROD, true,
                    at.x, at.y, at.z, -heading.x * 0.2D, -heading.y * 0.2D, -heading.z * 0.2D);
        }
    }

    // ------------------------------------------------------------------ rendering

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Entity view = mc.getRenderViewEntity();
        if (mc.world == null || view == null || !active && effects.isEmpty() && relic == null) {
            return;
        }
        float pt = event.getPartialTicks();
        Vec3d camera = new Vec3d(
                view.lastTickPosX + (view.posX - view.lastTickPosX) * pt,
                view.lastTickPosY + (view.posY - view.lastTickPosY) * pt,
                view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * pt);
        if (active && fade > 0.003F) {
            float cracking = stage == MsgComet.CRACKING ? Math.min(1F, (stageTicks + pt) / CRACK_TICKS) : 0F;
            CometRenderer.render(centre(pt).subtract(camera), heading, radius, integrity, cracking, fade, tick + pt);
        }
        FxRenderer.renderAll(effects, camera, pt);
        if (relic != null) {
            CometRenderer.relic(relic.subtract(camera), tick + pt);
        }
        if (active && glare > 0.01F) {
            CometRenderer.flare(centre(pt).subtract(camera), view.getLook(pt), radius, glare, tick + pt);
        }
    }

    /** Within reach, drawn exactly where the body is, so blows land where they look like they should. */
    private Vec3d centre(float pt) {
        if (body != null) {
            return new Vec3d(body.lastTickPosX + (body.posX - body.lastTickPosX) * pt,
                    body.lastTickPosY + (body.posY - body.lastTickPosY) * pt + body.height / 2D,
                    body.lastTickPosZ + (body.posZ - body.lastTickPosZ) * pt);
        }
        return new Vec3d(px + (x - px) * pt, py + (y - py) * pt, pz + (z - pz) * pt);
    }

    /** The air itself burning: a haze of orange closing in from the edges as you near it. */
    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Pre event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL || mc.world == null) {
            return;
        }
        float burn = heat * heat * 0.35F;
        float shown = EternityConfig.client.reduceFlashing ? Math.min(flash, 0.3F) : flash;
        float dazzle = glare * glare * (EternityConfig.client.reduceFlashing ? 0.12F : 0.3F);
        ScaledResolution screen = event.getResolution();
        if (dazzle > 0.005F) {
            ScreenFx.render(screen.getScaledWidth_double(), screen.getScaledHeight_double(), 0F, 0F, 0F,
                    dazzle, 0.75F, 0.88F, 1F, tick + event.getPartialTicks());
        }
        if (burn < 0.005F && shown < 0.005F) {
            return;
        }
        boolean flashing = shown > burn;
        ScreenFx.render(screen.getScaledWidth_double(), screen.getScaledHeight_double(), 0F, heat * 0.6F, 0F,
                Math.max(shown, burn), flashing ? flashR : 1F, flashing ? flashG : 0.5F, flashing ? flashB : 0.15F,
                tick + event.getPartialTicks());
    }

    @SubscribeEvent
    public void onCamera(EntityViewRenderEvent.CameraSetup event) {
        if (!EternityConfig.client.screenShake || shake < 0.001F) {
            return;
        }
        float t = tick + (float) event.getRenderPartialTicks();
        event.setYaw(event.getYaw() + (MathHelper.sin(t * 2.9F) + MathHelper.sin(t * 6.3F) * 0.5F) * shake * 0.7F);
        event.setPitch(event.getPitch() + (MathHelper.sin(t * 3.3F) + MathHelper.sin(t * 1.7F) * 0.5F) * shake * 0.7F);
        event.setRoll(event.getRoll() + MathHelper.sin(t * 1.3F) * shake * 1.2F);
    }

    // ------------------------------------------------------------------ blows

    /** Vanilla refuses blows aimed far from an entity's centre, so swings at the comet are sent separately. */
    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        if (event.getEntityPlayer() == mc.player && event.getEntityPlayer().world.isRemote && event.getTarget() instanceof EntityCosmicComet) {
            EternityNetwork.sendToServer(new MsgStrikeComet(event.getTarget().getEntityId()));
        }
    }

    // ------------------------------------------------------------------ cleanup

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.getWorld().isRemote) {
            clear();
        }
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        mc.addScheduledTask(this::clear);
    }

    private void clear() {
        hide();
        relic = null;
        effects.clear();
        shake = 0F;
        flash = 0F;
    }

    // ------------------------------------------------------------------ helpers

    private EntityCosmicComet findBody() {
        for (Entity entity : mc.world.loadedEntityList) {
            if (entity instanceof EntityCosmicComet && !entity.isDead) {
                return (EntityCosmicComet) entity;
            }
        }
        return null;
    }

    private void sparks(Vec3d at, int count, double speed) {
        for (int i = 0; i < count; i++) {
            mc.world.spawnParticle(EnumParticleTypes.FIREWORKS_SPARK, true, at.x, at.y, at.z,
                    rand.nextGaussian() * speed, rand.nextGaussian() * speed, rand.nextGaussian() * speed);
            if (i % 3 == 0) {
                mc.world.spawnParticle(EnumParticleTypes.END_ROD, true, at.x, at.y, at.z,
                        rand.nextGaussian() * speed * 0.3D, rand.nextGaussian() * speed * 0.3D, rand.nextGaussian() * speed * 0.3D);
            }
        }
    }

    /** The impact and blast are stereo, which the sound engine cannot place, so distance is applied by hand. */
    private static void stereo(SoundEvent sound, float volume, float pitch) {
        if (volume > 0.02F) {
            SoundFx.everywhere(sound, SoundCategory.HOSTILE, volume, pitch);
        }
    }

    private void flash(float amount, float r, float g, float b) {
        if (amount >= flash) {
            flash = amount;
            flashR = r;
            flashG = g;
            flashB = b;
        }
    }

    private void announce(String key) {
        mc.ingameGUI.setOverlayMessage(TextFormatting.AQUA + "" + TextFormatting.ITALIC + I18n.format(key), false);
    }

    private static float smoothstep(float edge0, float edge1, float x) {
        float t = MathHelper.clamp((x - edge0) / (edge1 - edge0), 0F, 1F);
        return t * t * (3F - 2F * t);
    }

    private static float closeness(double dist, double range) {
        return (float) MathHelper.clamp(1D - dist / range, 0D, 1D);
    }
}
