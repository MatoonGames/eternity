package com.matoon.eternity.client;

import com.matoon.eternity.EternityConfig;
import com.matoon.eternity.client.render.FxRenderer;
import com.matoon.eternity.client.render.GlSnapshot;
import com.matoon.eternity.client.render.WinterFx;
import com.matoon.eternity.client.sound.EntityLoopSound;
import com.matoon.eternity.item.ItemCasket;
import com.matoon.eternity.network.MsgTribunalFx;
import com.matoon.eternity.world.Fimbulwinter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * The Casket of Ancient Winters, held open, as everyone near sees and hears it: the torrent
 * pouring out of it and the gale's roar; frost closing over the holder's sight as it fills;
 * and the Fimbulwinter when it is let go. The wall racing out, a flash and a shudder as it
 * passes, the land behind it taking its new frozen colours, and the blizzard it leaves: snow
 * driven sideways round you, the fog closing in white.
 */
@SideOnly(Side.CLIENT)
public final class CasketClient {

    public static final CasketClient INSTANCE = new CasketClient();

    private final Minecraft mc = Minecraft.getMinecraft();
    private final List<FxRenderer.Fx> effects = new ArrayList<>();
    private final List<Storm> storms = new ArrayList<>();
    private final Map<Integer, EntityLoopSound> gales = new HashMap<>();
    private final Random rand = new Random();
    private World lastWorld;
    private int tick;
    private float flash, prevFlash, shake;
    /** How thick the blizzard is where the camera is, eased so it never snaps. */
    private float snow, prevSnow;

    /** A Fimbulwinter let out: where, how far, and how long ago. */
    private static final class Storm {
        final Vec3d at;
        final double radius, speed;
        final boolean iceAge;
        final WinterFx.Fimbul wave;
        int age;
        boolean painted;

        Storm(Vec3d at, double radius, double speed, boolean iceAge, WinterFx.Fimbul wave) {
            this.at = at;
            this.radius = radius;
            this.speed = speed;
            this.iceAge = iceAge;
            this.wave = wave;
        }
    }

    private CasketClient() {
    }

    /** The Fimbulwinter, from the server: CASKET_WINTER with id 2. */
    public void unleashed(MsgTribunalFx m) {
        if (mc.world == null || mc.player == null) {
            return;
        }
        Vec3d at = new Vec3d(m.x, m.y, m.z);
        WinterFx.Fimbul wave = new WinterFx.Fimbul(at, m.a, m.b, rand.nextLong());
        effects.add(wave);
        storms.add(new Storm(at, m.a, m.b, m.c > 0.5D, wave));
        double dist = mc.player.getPositionVector().distanceTo(at);
        if (dist < m.a) {
            flash = Math.max(flash, (float) (1D - dist / m.a) * 0.9F + 0.1F);
        }
        shake = Math.max(shake, (float) MathHelper.clamp(1D - dist / (m.a * 1.5D), 0D, 1D) * 4F);
        for (int i = 0; i < 160; i++) {
            double a = rand.nextDouble() * Math.PI * 2D, s = 0.6D + rand.nextDouble() * 1.4D;
            mc.world.spawnParticle(EnumParticleTypes.END_ROD, at.x, at.y + 1D, at.z, Math.cos(a) * s, rand.nextDouble() * 0.6D, Math.sin(a) * s);
            mc.world.spawnParticle(EnumParticleTypes.SNOW_SHOVEL, at.x, at.y + 0.5D, at.z, Math.cos(a) * s * 0.8D, 0.05D, Math.sin(a) * s * 0.8D);
        }
    }

    // ------------------------------------------------------------------ every tick

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (mc.world != lastWorld) {
            effects.clear();
            storms.clear();
            stopGales();
            snow = prevSnow = flash = prevFlash = 0F;
            lastWorld = mc.world;
        }
        if (mc.world == null || mc.player == null || mc.isGamePaused()) {
            return;
        }
        tick++;
        FxRenderer.tickAll(effects);
        prevFlash = flash;
        flash *= 0.9F;
        shake *= 0.9F;
        if (shake > 0.05F && mc.gameSettings.thirdPersonView == 0 && EternityConfig.client.screenShake) {
            mc.player.rotationPitch += (rand.nextFloat() - 0.5F) * shake * 0.6F;
            mc.player.rotationYaw += (rand.nextFloat() - 0.5F) * shake * 0.6F;
        }
        holders();
        float want = 0F;
        Vec3d me = mc.player.getPositionVector();
        for (Iterator<Storm> it = storms.iterator(); it.hasNext(); ) {
            Storm storm = it.next();
            storm.age++;
            double front = Math.min(storm.radius, storm.speed * storm.age);
            double d = Math.sqrt((me.x - storm.at.x) * (me.x - storm.at.x) + (me.z - storm.at.z) * (me.z - storm.at.z));
            if (d < front + 4D && d > front - storm.speed * 1.5D && front < storm.radius) {
                // The wall going over you.
                flash = Math.max(flash, 0.45F);
                shake = Math.max(shake, 2F);
                mc.world.playSound(me.x, me.y, me.z, SoundEvents.ITEM_ELYTRA_FLYING, SoundCategory.PLAYERS, 1F, 0.6F, false);
            }
            if (!storm.painted && front >= storm.radius) {
                storm.painted = true;
                if (storm.iceAge) {
                    paint(storm);
                }
            }
            float left = 1F - MathHelper.clamp((storm.age - Fimbulwinter.BLIZZARD_TICKS + 200F) / 200F, 0F, 1F);
            if (d < front + 6D) {
                want = Math.max(want, left * (float) MathHelper.clamp((storm.radius + 16D - d) / 16D, 0D, 1D));
            }
            if (storm.age > Fimbulwinter.BLIZZARD_TICKS) {
                it.remove();
            }
        }
        prevSnow = snow;
        snow += (want - snow) * 0.03F;
    }

    /** Everyone near holding the casket open: their torrent's mist and frost, and the roar of it. */
    private void holders() {
        List<Integer> roaring = new ArrayList<>();
        for (EntityPlayer player : mc.world.playerEntities) {
            float full = ItemCasket.gathered(player, 0F);
            if (full <= 0F || player.getDistance(mc.player) > 96D) {
                continue;
            }
            roaring.add(player.getEntityId());
            EntityLoopSound gale = gales.get(player.getEntityId());
            if (gale == null || gale.isDonePlaying()) {
                gale = new EntityLoopSound(SoundEvents.ITEM_ELYTRA_FLYING, SoundCategory.PLAYERS, player, 0.6F, 0.15F).pitch(0.55F);
                gales.put(player.getEntityId(), gale);
                mc.getSoundHandler().playSound(gale);
            }
            gale.setTarget(0.5F + 0.6F * full);
            Vec3d from = mouth(player, 1F);
            Vec3d dir = player.getLook(1F);
            int puffs = 2 + (int) (6 * full);
            for (int i = 0; i < puffs; i++) {
                double s = 0.6D + rand.nextDouble() * (0.8D + full);
                mc.world.spawnParticle(i % 2 == 0 ? EnumParticleTypes.SNOW_SHOVEL : EnumParticleTypes.END_ROD, from.x, from.y, from.z,
                        dir.x * s + rand.nextGaussian() * 0.12D, dir.y * s + rand.nextGaussian() * 0.12D, dir.z * s + rand.nextGaussian() * 0.12D);
            }
            if (full > 0.3F) {
                // Snow whirling round them.
                double r = 2D + 10D * full;
                for (int i = 0; i < 3; i++) {
                    double a = rand.nextDouble() * Math.PI * 2D;
                    mc.world.spawnParticle(EnumParticleTypes.FIREWORKS_SPARK, player.posX + Math.cos(a) * r * rand.nextDouble(), player.posY + rand.nextDouble() * 4D,
                            player.posZ + Math.sin(a) * r * rand.nextDouble(), -Math.sin(a) * 0.3D, -0.05D, Math.cos(a) * 0.3D);
                }
            }
            if (player == mc.player && full >= 1F && tick % 10 == 0) {
                shake = Math.max(shake, 0.6F);
            }
        }
        for (Iterator<Map.Entry<Integer, EntityLoopSound>> it = gales.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, EntityLoopSound> e = it.next();
            if (!roaring.contains(e.getKey())) {
                e.getValue().fadeOut();
                it.remove();
            }
        }
    }

    /** Where the casket's mouth is, held open in someone's hands. */
    private Vec3d mouth(EntityPlayer player, float pt) {
        Vec3d eye = player.getPositionEyes(pt);
        Vec3d look = player.getLook(pt);
        boolean right = player.getPrimaryHand() == EnumHandSide.RIGHT;
        float yaw = (float) Math.toRadians(player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw) * pt);
        Vec3d side = new Vec3d(-Math.cos(yaw), 0D, -Math.sin(yaw)).scale(right ? 1D : -1D);
        boolean firstPerson = player == mc.getRenderViewEntity() && mc.gameSettings.thirdPersonView == 0;
        return firstPerson ? eye.add(look.scale(0.7D)).add(side.scale(0.3D)).add(0D, -0.3D, 0D)
                : eye.add(look.scale(0.6D)).add(side.scale(0.35D)).add(0D, -0.65D, 0D);
    }

    /** The land the Fimbulwinter covered, in its frozen colours: the server has done the same. */
    private void paint(Storm storm) {
        int r = MathHelper.ceil(storm.radius);
        int cx = MathHelper.floor(storm.at.x), cz = MathHelper.floor(storm.at.z);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz < storm.radius * storm.radius && mc.world.isBlockLoaded(new BlockPos(cx + dx, 64, cz + dz))) {
                    Fimbulwinter.freezeBiome(mc.world, cx + dx, cz + dz);
                }
            }
        }
        mc.renderGlobal.markBlockRangeForRenderUpdate(cx - r, 0, cz - r, cx + r, 255, cz + r);
    }

    private void stopGales() {
        for (EntityLoopSound gale : gales.values()) {
            gale.fadeOut();
        }
        gales.clear();
    }

    // ------------------------------------------------------------------ drawing

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Entity view = mc.getRenderViewEntity();
        if (mc.world == null || view == null) {
            return;
        }
        float pt = event.getPartialTicks();
        float snowNow = prevSnow + (snow - prevSnow) * pt;
        boolean torrents = false;
        for (EntityPlayer player : mc.world.playerEntities) {
            torrents |= ItemCasket.gathered(player, pt) > 0F;
        }
        if (effects.isEmpty() && !torrents && snowNow <= 0.01F) {
            return;
        }
        Vec3d camera = new Vec3d(view.lastTickPosX + (view.posX - view.lastTickPosX) * pt, view.lastTickPosY + (view.posY - view.lastTickPosY) * pt,
                view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * pt);
        GlSnapshot before = GlSnapshot.take();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
        WinterFx.begin();
        float time = tick + pt;
        for (EntityPlayer player : mc.world.playerEntities) {
            float full = ItemCasket.gathered(player, pt);
            if (full <= 0F || player.getDistance(view) > 128D) {
                continue;
            }
            Vec3d from = mouth(player, pt);
            Vec3d dir = player.getLook(pt);
            double reach = ItemCasket.REACH + (ItemCasket.REACH_FULL - ItemCasket.REACH) * full;
            RayTraceResult hit = mc.world.rayTraceBlocks(from, from.add(dir.scale(reach)), true, true, false);
            Vec3d end = hit != null && hit.typeOfHit == RayTraceResult.Type.BLOCK ? hit.hitVec : from.add(dir.scale(reach));
            WinterFx.torrent(camera, from, dir, reach, end, full, time, player.getEntityId() * 7919);
        }
        FxRenderer.renderAll(effects, camera, pt);
        WinterFx.begin();
        WinterFx.blizzard(camera, time, snowNow);
        before.restore();
        GlStateManager.depthMask(true);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onOverlay(RenderGameOverlayEvent.Pre event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL || mc.player == null) {
            return;
        }
        float pt = event.getPartialTicks();
        float mine = ItemCasket.gathered(mc.player, pt);
        float snowNow = prevSnow + (snow - prevSnow) * pt;
        float rime = Math.max(mine * mine * 0.6F, snowNow * 0.35F);
        float white = prevFlash + (flash - prevFlash) * pt;
        if (EternityConfig.client.reduceFlashing) {
            white *= 0.3F;
        }
        if (rime > 0.01F || white > 0.01F) {
            WinterFx.frost(rime, white);
        }
    }

    /** In the blizzard, the world fades into white. */
    @SubscribeEvent
    public void onFogColour(EntityViewRenderEvent.FogColors event) {
        float s = prevSnow + (snow - prevSnow) * (float) event.getRenderPartialTicks();
        if (s <= 0.01F) {
            return;
        }
        float k = s * 0.6F;
        event.setRed(event.getRed() + (0.8F - event.getRed()) * k);
        event.setGreen(event.getGreen() + (0.87F - event.getGreen()) * k);
        event.setBlue(event.getBlue() + (0.95F - event.getBlue()) * k);
    }

    @SubscribeEvent
    public void onFog(EntityViewRenderEvent.RenderFogEvent event) {
        float s = prevSnow + (snow - prevSnow) * (float) event.getRenderPartialTicks();
        if (s <= 0.01F) {
            return;
        }
        float far = event.getFarPlaneDistance();
        float end = far + (Math.min(far, 70F) - far) * s - 25F * s;
        GlStateManager.setFogStart(event.getFogMode() == -1 ? 0F : end * 0.05F);
        GlStateManager.setFogEnd(end);
    }
}
