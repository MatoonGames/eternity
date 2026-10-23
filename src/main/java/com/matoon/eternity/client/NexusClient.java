package com.matoon.eternity.client;

import com.matoon.eternity.EternityConfig;
import com.matoon.eternity.EternitySounds;
import com.matoon.eternity.client.render.NexusRenderer;
import com.matoon.eternity.client.render.ScreenFx;
import com.matoon.eternity.client.sound.LoopSound;
import com.matoon.eternity.client.sound.SoundFx;
import com.matoon.eternity.world.Nexus;
import com.matoon.eternity.world.WorldProviderNexus;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.audio.ISound;
import com.matoon.eternity.Eternity;
import net.minecraft.client.audio.MusicTicker;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.MovementInput;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.EnumHelperClient;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.FOVUpdateEvent;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * Being in the Nexus of All Realities: gravity only half believes in you, colour drifts
 * in the air, voices from other realities pass by, the horizon leans gently one way and
 * the other, and the fog wanders slowly between violets and pinks.
 *
 * Toward the point of absolute neutrality, where the In-Betweener is kept, all of that
 * thins away: the motes and voices grow rare, the hum fades to silence, the music stops,
 * the fog and sky turn grey and the horizon stops leaning, until at its heart there is
 * nothing to tell one direction, or one moment, from another.
 *
 * Everything here is slow and soft on purpose. Nothing flashes or strobes, there is no
 * screen distortion, and with the client option reduceFlashing (or nexusWarp off) the
 * view holds perfectly still. The one exception is the abyss: sinking into it, the view
 * shakes and corrupts as you go, and even then reduceFlashing stills the shaking and
 * slows the corruption to a crawl.
 */
@SideOnly(Side.CLIENT)
public final class NexusClient {

    public static final NexusClient INSTANCE = new NexusClient();

    private static final SoundEvent[] WHISPERS = {EternitySounds.FAR_REVERSE, EternitySounds.OMEN_SHIMMER, EternitySounds.FAR_CHAINS, EternitySounds.FAR_NOISE};

    private final Minecraft mc = Minecraft.getMinecraft();
    private final Random rand = new Random();
    private int tick;
    private LoopSound hum;
    private LoopSound screams;
    /** Caught in the abyss, and how far under. */
    private boolean taken;
    private float sunk;
    private int nextGlitch;
    /** How near the point of absolute neutrality you stand, from 0 to 1. */
    private float neutral;
    private boolean silent;
    @Nullable
    private static MusicTicker.MusicType music;

    private NexusClient() {
    }

    /** The Nexus's own music, played in place of the overworld's, a track at a time with short silences between. */
    public static MusicTicker.MusicType music() {
        if (music == null) {
            music = EnumHelperClient.addMusicType("ETERNITY_NEXUS", EternitySounds.MUSIC_NEXUS, 200, 1200);
        }
        return music;
    }

    private boolean inNexus() {
        return mc.world != null && mc.world.provider instanceof WorldProviderNexus;
    }

    /** Whether the view may lean and breathe at all. */
    private boolean warp() {
        return EternityConfig.client.nexusWarp && !EternityConfig.client.reduceFlashing;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || mc.world == null || mc.player == null || mc.isGamePaused()) {
            return;
        }
        tick++;
        if (!inNexus()) {
            if (hum != null) {
                hum.fadeOut();
                hum = null;
            }
            if (screams != null) {
                screams.fadeOut();
                screams = null;
            }
            taken = false;
            sunk = 0F;
            neutral = 0F;
            return;
        }
        EntityPlayerSP player = mc.player;
        neutral = Nexus.neutrality(player.posX, player.posZ);
        if (silenced() && !silent && !FightMusic.INSTANCE.playing()) {
            // Near the junction even the Nexus's music has nothing to hold on to.
            mc.getSoundHandler().stop("", SoundCategory.MUSIC);
        }
        silent = silenced();
        // Gravity here is half-hearted: you drift down rather than fall.
        if (!player.onGround && !player.capabilities.isFlying && !player.isInWater()) {
            if (player.motionY < 0D) {
                player.motionY += 0.04D;
            }
            player.motionY = Math.max(player.motionY, -1D);
        }
        weightless(player);
        abyss(player);
        colour(player);
        ambience(player);
    }

    /** A few soft motes drifting around you, fewer and fewer toward the junction, and the vortex shedding light. */
    private void colour(EntityPlayerSP player) {
        for (int i = 0; i < 2; i++) {
            if (rand.nextFloat() < neutral) {
                continue;
            }
            float[] c = NexusRenderer.hue(0.72F + rand.nextFloat() * 0.2F, 0.6F, 1F);
            mc.world.spawnParticle(EnumParticleTypes.SPELL_MOB, player.posX + (rand.nextDouble() - 0.5D) * 24D,
                    player.posY + (rand.nextDouble() - 0.3D) * 12D, player.posZ + (rand.nextDouble() - 0.5D) * 24D, c[0], c[1], c[2]);
        }
        if (rand.nextInt(8) == 0 && rand.nextFloat() > neutral) {
            mc.world.spawnParticle(EnumParticleTypes.END_ROD, player.posX + (rand.nextDouble() - 0.5D) * 30D,
                    player.posY + (rand.nextDouble() - 0.5D) * 20D, player.posZ + (rand.nextDouble() - 0.5D) * 30D,
                    0D, 0.02D, 0D);
        }
        double dx = player.posX - Nexus.VORTEX_X, dz = player.posZ - Nexus.VORTEX_Z;
        if (dx * dx + dz * dz < 80D * 80D) {
            for (int i = 0; i < 2; i++) {
                double a = rand.nextDouble() * Math.PI * 2D;
                double r = Nexus.VORTEX_RADIUS * (0.8D + rand.nextDouble() * 0.6D);
                mc.world.spawnParticle(EnumParticleTypes.PORTAL, true, Nexus.VORTEX_X + Math.cos(a) * r, Nexus.VORTEX_Y + Math.sin(a) * r,
                        Nexus.VORTEX_Z, -Math.cos(a) * r * 0.2D, -Math.sin(a) * r * 0.2D, 0D);
            }
        }
    }

    /** A low hum under everything, and now and then a voice from somewhere else. */
    private void ambience(EntityPlayerSP player) {
        if (hum == null || hum.isDonePlaying()) {
            hum = new LoopSound(EternitySounds.AMBIENT_ABYSS, SoundCategory.AMBIENT, true, 0.03F).pitch(1.35F);
            SoundFx.play(hum);
        }
        // Toward the junction it thins away to nothing: there, there is not even a hum.
        hum.setTarget(0.45F * (1F - neutral));
        if (tick % 140 == 0 && rand.nextBoolean() && rand.nextFloat() > neutral) {
            SoundEvent whisper = WHISPERS[rand.nextInt(WHISPERS.length)];
            SoundFx.carried(whisper, 0.5F + rand.nextFloat() * 0.4F, 0.6F + rand.nextFloat() * 1.2F, rand.nextDouble() * Math.PI * 2D);
        }
    }

    /**
     * The abyss. Come down toward it and you hear it screaming, louder the closer you get, and
     * in the last few blocks the hands have hold of you and pull. Touch it and it is quicksand:
     * they catch you at its surface, you cannot move, jump or fly, and you are dragged under
     * slowly while the view comes apart around you (see {@link ScreenFx#renderCorruption}).
     * The server takes you once you have gone under.
     */
    private void abyss(EntityPlayerSP player) {
        double above = player.posY - Nexus.ABYSS_Y;
        if (player.isSpectator() || above > 1.5D) {
            // Never caught, or pulled out of it somehow.
            taken = false;
        } else if (!taken && above < 0D && player.getHealth() > 0F) {
            taken = true;
            // Caught at the surface, however fast you fell.
            player.setPosition(player.posX, Math.max(player.posY, Nexus.ABYSS_Y - 0.3D), player.posZ);
        }
        sunk = taken ? Nexus.sunk(player) : 0F;

        float loud = taken ? 1F : (float) MathHelper.clamp(1D - above / 56D, 0D, 1D);
        if (loud > 0F && !player.isSpectator()) {
            if (screams == null || screams.isDonePlaying()) {
                screams = new LoopSound(EternitySounds.NEXUS_ABYSS, SoundCategory.AMBIENT, true, 0.05F);
                SoundFx.play(screams);
            }
            screams.setTarget(loud);
        } else if (screams != null) {
            screams.fadeOut();
            screams = null;
        }

        if (!taken) {
            if (above < 8D && !player.capabilities.isFlying && !player.isSpectator()) {
                player.motionY -= 0.06D * (1D - Math.max(0D, above) / 8D);
            }
            return;
        }
        if (player.getHealth() > 0F) {
            player.capabilities.isFlying = false;
            player.motionX = player.motionZ = 0D;
            player.motionY = -Nexus.sinkSpeed(sunk);
            player.fallDistance = 0F;
        }
        // The sound of the view breaking up, coming quicker as it goes.
        if (tick >= nextGlitch) {
            SoundFx.everywhere(EternitySounds.ENTITY_GLITCH, SoundCategory.AMBIENT, 0.25F + 0.6F * sunk, 0.6F + rand.nextFloat() * 0.6F);
            nextGlitch = tick + 40 - (int) (30F * sunk) + rand.nextInt(20);
        }
        // The surface churning where you went in.
        for (int i = 0; i < 3; i++) {
            double a = rand.nextDouble() * Math.PI * 2D, r = 0.4D + rand.nextDouble() * 1.4D;
            mc.world.spawnParticle(EnumParticleTypes.SPELL_MOB, player.posX + Math.cos(a) * r, Nexus.ABYSS_Y + 0.1D,
                    player.posZ + Math.sin(a) * r, 0.25D, 0.02D, 0.12D);
        }
    }

    /** How far the abyss has taken you, from 0 (it has not) to 1 (gone). */
    public float sinking() {
        return taken ? Math.max(sunk, 0.05F) : 0F;
    }

    /** How near the point of absolute neutrality you stand, from 0 (nowhere near) to 1 (at its heart). */
    public float neutrality() {
        return inNexus() ? neutral : 0F;
    }

    private boolean silenced() {
        return neutral > 0.4F;
    }

    /** No music but a fight's starts near the junction, and at its epicentre nearly nothing is heard at all. */
    @SubscribeEvent
    public void onPlaySound(PlaySoundEvent event) {
        if (event.getSound() != null && !FightMusic.isFightMusic(event.getSound())
                && (event.getSound().getCategory() == SoundCategory.MUSIC && inNexus() && silenced() || hushed(event.getSound()))) {
            event.setResultSound(null);
        }
    }


    // ------------------------------------------------------------------ the epicentre

    /** Whether you stand within the epicentre of absolute neutrality. */
    public boolean atEpicentre() {
        return inNexus() && mc.player != null && Nexus.inEpicentre(mc.player.posX, mc.player.posY, mc.player.posZ);
    }

    /**
     * No up and no down: at the epicentre nothing falls. You hang where you are; jump to
     * rise, sneak to sink, and drift wherever you walk.
     */
    private void weightless(EntityPlayerSP player) {
        if (!Nexus.inEpicentre(player.posX, player.posY, player.posZ) || player.capabilities.isFlying || player.onGround
                || player.isSpectator()) {
            return;
        }
        double want = player.movementInput.jump ? 0.14D : player.movementInput.sneak ? -0.14D : 0D;
        player.motionY += (want - player.motionY) * 0.35D;
        player.fallDistance = 0F;
    }

    /**
     * At the epicentre almost nothing is heard: no footsteps, no blocks, no wind, no breath.
     * What is there to hear is the In-Betweener, his masters, and anything they do.
     */
    private boolean hushed(ISound sound) {
        if (!atEpicentre() || Eternity.MODID.equals(sound.getSoundLocation().getNamespace())) {
            return false;
        }
        SoundCategory category = sound.getCategory();
        return category == SoundCategory.BLOCKS || category == SoundCategory.NEUTRAL || category == SoundCategory.PLAYERS
                || category == SoundCategory.AMBIENT || category == SoundCategory.WEATHER || category == SoundCategory.RECORDS;
    }

    /**
     * Dust that never settles: grey motes hanging perfectly still in the air around you,
     * thicker the nearer the junction. Not one of them drifts. Nothing here moves on its own.
     */
    private void stillDust(Vec3d camera) {
        if (neutral < 0.3F) {
            return;
        }
        float density = (neutral - 0.3F) / 0.7F;
        int range = 6;
        int cx = MathHelper.floor(camera.x / 3D), cy = MathHelper.floor(camera.y / 3D), cz = MathHelper.floor(camera.z / 3D);
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.depthMask(false);
        GL11.glPointSize(2.5F);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_POINTS, DefaultVertexFormats.POSITION_COLOR);
        for (int x = cx - range; x <= cx + range; x++) {
            for (int y = cy - range; y <= cy + range; y++) {
                for (int z = cz - range; z <= cz + range; z++) {
                    int h = x * 73856093 ^ y * 19349663 ^ z * 83492791;
                    h ^= h >>> 13;
                    h *= 0x5bd1e995;
                    if (((h >>> 8) & 255) / 255F > density * 0.6F) {
                        continue;
                    }
                    double px = x * 3D + ((h >>> 16) & 15) / 5D, py = y * 3D + ((h >>> 20) & 15) / 5D, pz = z * 3D + ((h >>> 24) & 15) / 5D;
                    float grey = 0.55F + ((h >>> 4) & 15) / 100F;
                    buffer.pos(px - camera.x, py - camera.y, pz - camera.z).color(grey, grey, grey + 0.02F, 0.65F).endVertex();
                }
            }
        }
        tessellator.draw();
        GL11.glPointSize(1F);
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    /**
     * Near the junction the fog closes in to one flat, even grey: no distance, no depth, no
     * difference between near and far.
     */
    @SubscribeEvent
    public void onFog(EntityViewRenderEvent.RenderFogEvent event) {
        if (!inNexus() || neutral < 0.5F) {
            return;
        }
        float k = (neutral - 0.5F) / 0.5F;
        float far = event.getFarPlaneDistance();
        GlStateManager.setFogStart(far * 0.75F + (8F - far * 0.75F) * k);
        GlStateManager.setFogEnd(far + (110F - far) * k);
    }

    /** At the edges of sight, the colour has already gone out of everything. */
    @SubscribeEvent
    public void onVignette(RenderGameOverlayEvent.Pre event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL || !inNexus() || neutral < 0.4F) {
            return;
        }
        float k = (neutral - 0.4F) / 0.6F;
        ScaledResolution screen = event.getResolution();
        int w = screen.getScaledWidth(), h = screen.getScaledHeight();
        int edge = (int) (Math.min(w, h) * 0.22F);
        int grey = 0x808084;
        int solid = ((int) (0x70 * k) << 24) | grey, clear = grey;
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        band(buffer, 0, 0, w, edge, solid, solid, clear, clear);
        band(buffer, 0, h - edge, w, h, clear, clear, solid, solid);
        band(buffer, 0, 0, edge, h, solid, clear, clear, solid);
        band(buffer, w - edge, 0, w, h, clear, solid, solid, clear);
        tessellator.draw();
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.enableAlpha();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
    }

    /** A rectangle shaded corner by corner: top-left, top-right, bottom-right, bottom-left. */
    private static void band(BufferBuilder buffer, int x0, int y0, int x1, int y1, int tl, int tr, int br, int bl) {
        corner(buffer, x1, y0, tr);
        corner(buffer, x0, y0, tl);
        corner(buffer, x0, y1, bl);
        corner(buffer, x1, y1, br);
    }

    private static void corner(BufferBuilder buffer, int x, int y, int argb) {
        buffer.pos(x, y, 0D).color((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, (argb >>> 24) & 0xFF).endVertex();
    }
    /** Held: no walking, no jumping, no climbing out. */
    @SubscribeEvent
    public void onInput(InputUpdateEvent event) {
        if (taken && event.getEntityPlayer() == mc.player) {
            MovementInput input = event.getMovementInput();
            input.moveForward = 0F;
            input.moveStrafe = 0F;
            input.jump = false;
            input.sneak = false;
            input.forwardKeyDown = input.backKeyDown = input.leftKeyDown = input.rightKeyDown = false;
        }
    }

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() == RenderGameOverlayEvent.ElementType.ALL && inNexus() && taken) {
            ScaledResolution screen = event.getResolution();
            ScreenFx.renderCorruption(screen.getScaledWidth_double(), screen.getScaledHeight_double(), sinking(),
                    tick + event.getPartialTicks(), EternityConfig.client.reduceFlashing);
        }
    }

    // ------------------------------------------------------------------ the view

    /** The fog wanders slowly between violet and pink, never quickly, never brightly. */
    @SubscribeEvent
    public void onFogColour(EntityViewRenderEvent.FogColors event) {
        if (!inNexus()) {
            return;
        }
        float t = tick + (float) event.getRenderPartialTicks();
        float[] c = NexusRenderer.hue(0.8F + 0.06F * MathHelper.sin(t * 0.0008F), 0.65F, 0.62F);
        // Toward the junction the colour goes out of it, until it is only grey.
        event.setRed(c[0] + (0.5F - c[0]) * neutral);
        event.setGreen(c[1] + (0.5F - c[1]) * neutral);
        event.setBlue(c[2] + (0.52F - c[2]) * neutral);
    }

    /** The horizon leans a little one way, then the other, over the better part of a minute. */
    @SubscribeEvent
    public void onCamera(EntityViewRenderEvent.CameraSetup event) {
        if (!inNexus()) {
            return;
        }
        float t = tick + (float) event.getRenderPartialTicks();
        if (warp()) {
            // There are no transitions at the junction; even the leaning stops.
            event.setRoll(event.getRoll() + MathHelper.sin(t * 0.004F) * 1.2F * (1F - neutral));
        }
        if (taken && !EternityConfig.client.reduceFlashing) {
            // Shaking in their grip.
            event.setRoll(event.getRoll() + MathHelper.sin(t * 0.7F) * 2.5F * sunk);
            event.setPitch(event.getPitch() + MathHelper.sin(t * 0.53F + 1F) * 1.5F * sunk);
        }
    }

    /** The view breathes in and out, very slowly; and narrows as the abyss takes you. */
    @SubscribeEvent
    public void onFov(FOVUpdateEvent event) {
        if (inNexus() && warp()) {
            event.setNewfov(event.getNewfov() * (1F + 0.015F * (1F - neutral) * MathHelper.sin(tick * 0.005F)));
        }
        if (inNexus() && taken) {
            event.setNewfov(event.getNewfov() * (1F - 0.3F * sunk));
        }
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Entity view = mc.getRenderViewEntity();
        if (!inNexus() || view == null) {
            return;
        }
        float pt = event.getPartialTicks();
        Vec3d camera = new Vec3d(
                view.lastTickPosX + (view.posX - view.lastTickPosX) * pt,
                view.lastTickPosY + (view.posY - view.lastTickPosY) * pt,
                view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * pt);
        NexusRenderer.renderWorld(camera, tick + pt);
        stillDust(camera);
    }

    // ------------------------------------------------------------------ cleanup

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.getWorld().isRemote) {
            hum = null;
            screams = null;
            taken = false;
        }
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        mc.addScheduledTask(() -> {
            hum = null;
            screams = null;
            taken = false;
        });
    }
}
