package com.matoon.eternity.client;

import com.matoon.eternity.EternityConfig;
import com.matoon.eternity.EternitySounds;
import com.matoon.eternity.client.render.FxRenderer;
import com.matoon.eternity.client.render.TribunalFx;
import com.matoon.eternity.client.render.TribunalRenderer;
import com.matoon.eternity.client.sound.LoopSound;
import com.matoon.eternity.client.sound.SoundFx;
import com.matoon.eternity.entity.EntityLivingTribunal;
import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.network.MsgStrikeTribunal;
import com.matoon.eternity.network.MsgTribunal;
import com.matoon.eternity.network.MsgTribunalAnswer;
import com.matoon.eternity.network.MsgTribunalFx;
import com.matoon.eternity.world.StarChamber;
import com.matoon.eternity.world.TribunalDialogue;
import com.matoon.eternity.world.TribunalDirector;
import com.matoon.eternity.world.WorldProviderStarChamber;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.Entity;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.FOVUpdateEvent;
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

/**
 * The Living Tribunal from the defendant's side: the jumpscare, the grasping hand,
 * the wormhole, the courtroom with its seated giant, and his voice, which shakes the
 * whole chamber while he speaks.
 */
@SideOnly(Side.CLIENT)
public final class TribunalClient {

    public static final TribunalClient INSTANCE = new TribunalClient();

    /** How fast his words appear, in characters per tick. */
    public static final float WORDS_PER_TICK = 1.4F;

    /** When his hand closes over you. */
    private static final int GRAB_AT = 56;

    private final Minecraft mc = Minecraft.getMinecraft();

    private int tick;
    /** Ticks since he appeared to take you, or -1. */
    private int abductTicks = -1;
    /** Ticks since you were sent home, or -1. */
    private int returnTicks = -1;
    private boolean onTrial;
    /** What he is saying: a question or a verdict, or -1 for silence. */
    private int node = -1;
    private int speakTicks;

    private Vec3d loomAt;
    private float loomYaw;

    private float shake;
    private float flash;
    private float black;
    private LoopSound hall;
    private final List<FxRenderer.Fx> effects = new ArrayList<>();
    /** Whether he has left his throne to fight; his seat stands empty. */
    private boolean risen;

    private TribunalClient() {
    }

    public void handle(MsgTribunal m) {
        if (mc.world == null || mc.player == null) {
            return;
        }
        switch (m.type) {
            case MsgTribunal.ABDUCT:
                abduct();
                break;
            case MsgTribunal.CHAMBER:
                onTrial = true;
                abductTicks = -1;
                returnTicks = -1;
                node = -1;
                black = 1F;
                flash = 0.8F;
                break;
            case MsgTribunal.SPEAK:
                node = m.value;
                speakTicks = 0;
                boolean verdict = !TribunalDialogue.isQuestion(node);
                SoundFx.everywhere(EternitySounds.ENTITY_VOICE, SoundCategory.MASTER, 1.6F, verdict ? 0.3F : 0.4F);
                if (verdict) {
                    SoundFx.everywhere(EternitySounds.FINALE_DRUMS, SoundCategory.MASTER, 0.9F, 0.7F);
                    flash = 0.6F;
                }
                mc.displayGuiScreen(new GuiTribunal(node));
                break;
            case MsgTribunal.RETURN:
                returnTicks = 0;
                closeCourt();
                SoundFx.everywhere(EternitySounds.RIFT_OPEN, SoundCategory.MASTER, 1.2F, 0.6F);
                break;
            case MsgTribunal.FIGHT:
                closeCourt();
                shake = 2.5F;
                flash = 0.5F;
                SoundFx.everywhere(SoundEvents.ENTITY_ENDERDRAGON_GROWL, SoundCategory.MASTER, 1.5F, 0.35F);
                break;
            case MsgTribunal.RELEASE:
                onTrial = false;
                returnTicks = -1;
                closeCourt();
                black = 1F;
                flash = 1F;
                break;
            default:
                break;
        }
    }

    /** One of his judgements, or his fall, or his rising. */
    public void effect(MsgTribunalFx m) {
        if (mc.world == null || mc.player == null) {
            return;
        }
        Vec3d at = new Vec3d(m.x, m.y, m.z);
        Vec3d to = new Vec3d(m.a, m.b, m.c);
        double dist = mc.player.getPositionVector().distanceTo(at);
        switch (m.type) {
            case MsgTribunalFx.SIGIL:
                effects.add(new TribunalFx.Sigil(at, m.a, m.time));
                break;
            case MsgTribunalFx.GAVEL:
                effects.add(new FxRenderer.Shockwave(at.add(0D, 0.2D, 0D), m.a, 18, false));
                effects.add(new FxRenderer.Shockwave(at.add(0D, 0.3D, 0D), m.a * 2.2D, 34, false));
                shake = Math.max(shake, 3F * closeness(dist, 60D));
                flash = Math.max(flash, 0.35F * closeness(dist, 30D));
                for (int i = 0; i < 24; i++) {
                    mc.world.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE, true, at.x + (mc.world.rand.nextDouble() - 0.5D) * m.a,
                            at.y + mc.world.rand.nextDouble() * 2D, at.z + (mc.world.rand.nextDouble() - 0.5D) * m.a, 0D, 0D, 0D);
                }
                break;
            case MsgTribunalFx.GAZE:
                effects.add(new TribunalFx.Gaze(at, to, m.id / 10D, m.time));
                shake = Math.max(shake, 0.6F * closeness(mc.player.getPositionVector().distanceTo(to), 20D));
                break;
            case MsgTribunalFx.BOLT:
                effects.add(new TribunalFx.Bolt(at, to, m.id, m.time));
                flash = Math.max(flash, 0.25F * closeness(mc.player.getPositionVector().distanceTo(to), 12D));
                break;
            case MsgTribunalFx.ABSORB:
                effects.add(new TribunalFx.Absorb(at, m.id, m.time));
                shake = Math.max(shake, 0.8F);
                break;
            case MsgTribunalFx.FALL:
                shake = Math.max(shake, 3F);
                flash = Math.max(flash, 0.5F);
                break;
            case MsgTribunalFx.RISE:
                // He comes back stronger, and the whole chamber knows it.
                shake = Math.max(shake, 2F + m.id);
                flash = Math.max(flash, 0.3F + 0.2F * m.id);
                effects.add(new FxRenderer.Shockwave(at.add(0D, 0.3D, 0D), 30D + 15D * m.id, 40, false));
                SoundFx.everywhere(EternitySounds.FINALE_DRUMS, SoundCategory.MASTER, 0.8F, 0.6F + 0.1F * m.id);
                break;
            case MsgTribunalFx.PAN:
                effects.add(new TribunalFx.Pan(at, m.a, m.time));
                break;
            case MsgTribunalFx.CAGE:
                effects.add(new TribunalFx.Cage(at, m.a, m.time));
                break;
            case MsgTribunalFx.VOID:
                effects.add(new TribunalFx.VoidPool(at, m.a, m.time));
                break;
            case MsgTribunalFx.BLADES:
                effects.add(new TribunalFx.Blades(at, m.a, m.time));
                break;
            case MsgTribunalFx.PILLAR:
                effects.add(new TribunalFx.Pillar(at, m.a, m.time));
                shake = Math.max(shake, 0.8F * closeness(dist, 20D));
                break;
            case MsgTribunalFx.NOVA:
                effects.add(new TribunalFx.Nova(at, m.a, m.time));
                shake = Math.max(shake, 1.5F);
                break;
            case MsgTribunalFx.SWEEP:
                effects.add(new TribunalFx.Sweep(m.x, m.y, m.z, m.c, m.time));
                break;
            case MsgTribunalFx.ECHO:
                effects.add(new TribunalFx.Echo(at, (float) m.a, m.time));
                flash = Math.max(flash, 0.2F);
                break;
            case MsgTribunalFx.GRAVITY:
                effects.add(new TribunalFx.Gravity(at, m.a, m.time));
                shake = Math.max(shake, 1F);
                break;
            default:
                break;
        }
    }

    /** He is simply there, vast, filling the sky in front of you. */
    private void abduct() {
        abductTicks = 0;
        Vec3d eyes = mc.player.getPositionEyes(1F);
        Vec3d look = mc.player.getLookVec();
        Vec3d flat = new Vec3d(look.x, 0, look.z);
        flat = flat.lengthSquared() < 1.0E-4 ? new Vec3d(0, 0, 1) : flat.normalize();
        // His face level with yours, some thirty blocks away.
        loomAt = eyes.add(flat.scale(30D)).subtract(0, TribunalRenderer.LOOM_EYE * TribunalRenderer.LOOM_SCALE, 0);
        loomYaw = (float) Math.toDegrees(Math.atan2(-flat.x, -flat.z));
        shake = 3F;
        flash = 1F;
        SoundFx.everywhere(EternitySounds.ENTITY_GLITCH, SoundCategory.MASTER, 2F, 0.5F);
        SoundFx.everywhere(EternitySounds.RIFT_IMPACT, SoundCategory.MASTER, 2F, 0.5F);
        SoundFx.everywhere(SoundEvents.ENTITY_ENDERDRAGON_GROWL, SoundCategory.MASTER, 1.5F, 0.4F);
        SoundFx.everywhere(EternitySounds.ENTITY_VOICE, SoundCategory.MASTER, 2F, 0.35F);
    }

    private void closeCourt() {
        node = -1;
        if (mc.currentScreen instanceof GuiTribunal) {
            mc.displayGuiScreen(null);
        }
    }

    /** Characters of his current speech shown so far. */
    public int typed() {
        return node < 0 ? 0 : (int) (speakTicks * WORDS_PER_TICK);
    }

    public boolean doneSpeaking() {
        return node < 0 || typed() >= TribunalDialogue.node(node).speech.length();
    }

    /** Clicking while he speaks lets the rest of his words land at once. */
    public void hurry() {
        if (node >= 0) {
            speakTicks = (int) Math.ceil(TribunalDialogue.node(node).speech.length() / WORDS_PER_TICK);
        }
    }

    public void answer(int question, int choice) {
        EternityNetwork.sendToServer(new MsgTribunalAnswer(question, choice));
        node = -1;
        mc.displayGuiScreen(null);
    }

    // ------------------------------------------------------------------ ticking

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || mc.world == null || mc.player == null) {
            return;
        }
        tick++;
        FxRenderer.tickAll(effects);
        if (tick % 10 == 0) {
            risen = findRisen();
        }
        shake *= 0.92F;
        flash *= 0.9F;
        black = Math.max(0F, black - 0.03F);

        if (abductTicks >= 0) {
            abductTicks++;
            if (abductTicks == GRAB_AT) {
                SoundFx.everywhere(EternitySounds.COMET_IMPACT, SoundCategory.MASTER, 1.5F, 0.6F);
                SoundFx.everywhere(EternitySounds.RIFT_OPEN, SoundCategory.MASTER, 1.5F, 0.5F);
            }
            if (abductTicks < GRAB_AT) {
                shake = Math.max(shake, 1.5F);
            } else if (abductTicks < GRAB_AT + 6) {
                black = 1F;
            }
        }
        if (returnTicks >= 0) {
            returnTicks++;
        }
        if (node >= 0) {
            speakTicks++;
            if (!doneSpeaking()) {
                // The chamber shakes with every word.
                shake = Math.max(shake, TribunalDialogue.isQuestion(node) ? 1.1F : 1.8F);
                if (speakTicks % 40 == 0) {
                    SoundFx.everywhere(EternitySounds.AMBIENT_RUMBLE, SoundCategory.MASTER, 0.8F, 0.5F);
                }
            }
            if (mc.currentScreen == null) {
                mc.displayGuiScreen(new GuiTribunal(node));
            }
        }
        updateHall();
    }

    private void updateHall() {
        boolean inChamber = inChamber();
        if (inChamber && (hall == null || hall.isDonePlaying())) {
            hall = new LoopSound(EternitySounds.AMBIENT_ABYSS, SoundCategory.AMBIENT, true, 0.03F).pitch(0.7F);
            SoundFx.play(hall);
        }
        if (hall != null) {
            hall.setTarget(inChamber ? 0.8F : 0F);
            if (!inChamber) {
                hall.fadeOut();
                hall = null;
            }
        }
    }

    private boolean inChamber() {
        return mc.world != null && mc.world.provider instanceof WorldProviderStarChamber;
    }

    private float wormhole(float pt) {
        if (abductTicks >= GRAB_AT) {
            return Math.min(1F, (abductTicks + pt - GRAB_AT) / 15F);
        }
        if (returnTicks >= 0) {
            float t = returnTicks + pt;
            return Math.min(1F, t / 15F) * Math.min(1F, (TribunalDirector.RETURN_TICKS + 10 - t) / 10F);
        }
        return 0F;
    }

    // ------------------------------------------------------------------ rendering

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Entity view = mc.getRenderViewEntity();
        if (mc.world == null || view == null) {
            return;
        }
        float pt = event.getPartialTicks();
        float time = tick + pt;
        Vec3d camera = new Vec3d(
                view.lastTickPosX + (view.posX - view.lastTickPosX) * pt,
                view.lastTickPosY + (view.posY - view.lastTickPosY) * pt,
                view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * pt);
        if (inChamber() && !risen) {
            float eyes = node >= 0 && !doneSpeaking() ? 1F : onTrial ? 0.7F : 0.4F;
            Vec3d throne = new Vec3d(StarChamber.THRONE_X, StarChamber.THRONE_Y, StarChamber.THRONE_Z);
            TribunalRenderer.seated(throne.subtract(camera), eyes, time);
        }
        if (abductTicks >= 0 && abductTicks < GRAB_AT && loomAt != null) {
            float t = abductTicks + pt;
            float reach = smoothstep(8F, GRAB_AT, t);
            // He leans in as he reaches.
            Vec3d toward = camera.subtract(loomAt);
            Vec3d flat = new Vec3d(toward.x, 0, toward.z).normalize();
            Vec3d at = loomAt.add(flat.scale(10D * reach));
            TribunalRenderer.looming(at.subtract(camera), loomYaw, reach, time);
        }
        FxRenderer.renderAll(effects, camera, pt);
        float hole = wormhole(pt);
        if (hole > 0.01F) {
            TribunalRenderer.wormhole(view.getLook(pt), hole, time);
        }
    }

    private boolean findRisen() {
        for (Entity entity : mc.world.loadedEntityList) {
            if (entity instanceof EntityLivingTribunal) {
                return true;
            }
        }
        return false;
    }

    /** He is far too big for vanilla's reach check, so swings at him are sent separately. */
    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        if (event.getEntityPlayer() == mc.player && event.getEntityPlayer().world.isRemote && event.getTarget() instanceof EntityLivingTribunal) {
            EternityNetwork.sendToServer(new MsgStrikeTribunal(event.getTarget().getEntityId()));
        }
    }

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Pre event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL || mc.world == null) {
            return;
        }
        float shownFlash = EternityConfig.client.reduceFlashing ? Math.min(flash, 0.3F) : flash;
        if (black < 0.01F && shownFlash < 0.01F) {
            return;
        }
        ScaledResolution screen = event.getResolution();
        int w = screen.getScaledWidth();
        int h = screen.getScaledHeight();
        if (black > 0.01F) {
            Gui.drawRect(0, 0, w, h, (int) (Math.min(1F, black) * 255F) << 24);
        }
        if (shownFlash > 0.01F) {
            Gui.drawRect(0, 0, w, h, ((int) (Math.min(1F, shownFlash) * 255F) << 24) | 0xF0EEFF);
        }
    }

    @SubscribeEvent
    public void onCamera(EntityViewRenderEvent.CameraSetup event) {
        if (!EternityConfig.client.screenShake || shake < 0.001F) {
            return;
        }
        float t = tick + (float) event.getRenderPartialTicks();
        event.setYaw(event.getYaw() + (MathHelper.sin(t * 3.1F) + MathHelper.sin(t * 7.3F) * 0.5F) * shake * 0.6F);
        event.setPitch(event.getPitch() + (MathHelper.sin(t * 2.7F) + MathHelper.sin(t * 5.9F) * 0.5F) * shake * 0.6F);
        event.setRoll(event.getRoll() + MathHelper.sin(t * 1.7F) * shake * 1.1F);
    }

    /** The tunnel stretches the world around you. */
    @SubscribeEvent
    public void onFov(FOVUpdateEvent event) {
        float hole = wormhole(0F);
        if (hole > 0.01F) {
            event.setNewfov(event.getNewfov() * (1F + 0.35F * hole));
        }
    }

    // ------------------------------------------------------------------ cleanup

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.getWorld().isRemote) {
            abductTicks = -1;
            hall = null;
        }
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        mc.addScheduledTask(() -> {
            abductTicks = -1;
            returnTicks = -1;
            onTrial = false;
            node = -1;
            shake = flash = black = 0F;
            hall = null;
            effects.clear();
            risen = false;
        });
    }

    private static float closeness(double dist, double range) {
        return (float) MathHelper.clamp(1D - dist / range, 0D, 1D);
    }

    private static float smoothstep(float edge0, float edge1, float x) {
        float t = MathHelper.clamp((x - edge0) / (edge1 - edge0), 0F, 1F);
        return t * t * (3F - 2F * t);
    }
}
