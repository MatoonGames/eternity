package com.matoon.eternity.client;

import com.matoon.eternity.EternityConfig;
import com.matoon.eternity.EternitySounds;
import com.matoon.eternity.client.render.FxRenderer;
import com.matoon.eternity.client.render.InBetweenerFx;
import com.matoon.eternity.client.render.RenderInBetweener;
import com.matoon.eternity.client.render.RenderOrderChaos;
import com.matoon.eternity.client.sound.SoundFx;
import com.matoon.eternity.entity.EntityInBetweener;
import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.network.MsgInBetweener;
import com.matoon.eternity.network.MsgInBetweenerAct;
import com.matoon.eternity.world.InBetweenerDialogue;
import com.matoon.eternity.world.Nexus;
import com.matoon.eternity.world.WorldProviderNexus;
import net.minecraft.client.Minecraft;
import net.minecraft.init.SoundEvents;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.entity.Entity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.FOVUpdateEvent;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * The In-Betweener from where you stand: his words, and what he does as he says them
 * (frost on his glass, darkness but for his eyes, stars, echoes of him, the gem flaring or
 * held out, the glass thinning toward you); his glass, the cracks you put in it
 * and the shards it bursts into; his masters rising at the edges of everything while the
 * clock runs down, and their arrival. And, if you defy him, the inside of his bubble,
 * where he is everything: life and death trading places in your body, up and down
 * trading places in the world, copies of him everywhere, time stopping for you and not
 * for him, and at the end the world splitting into white and black around you.
 */
@SideOnly(Side.CLIENT)
public final class InBetweenerClient {

    public static final InBetweenerClient INSTANCE = new InBetweenerClient();

    /** How fast his words appear, in characters per tick. */
    public static final float WORDS_PER_TICK = 1.4F;
    private static final String[] PHASE_NAMES = {"LIFE  AND  DEATH", "UP  AND  DOWN", "HERE  AND  THERE", "BEFORE  AND  AFTER", "BEING  AND  NOTHING"};

    private final Minecraft mc = Minecraft.getMinecraft();
    private final List<FxRenderer.Fx> effects = new ArrayList<>();

    private int tick;
    /** What he is saying, or -1, which answers you may give, and for how long he has been saying it. */
    private int node = -1;
    private int mask;
    private int speakTicks;
    /** While you are meant to be striking his glass: ticks left, or -1. */
    private int shatterTicks = -1;
    private int lastStrike;
    /** The weakest blow the glass notices. */
    private int shatterBlow;
    /** Inside his bubble: ticks since it swallowed you, or -1; and which of his warpings has begun. */
    private int battleTicks = -1;
    private int phase;
    private int phaseAt;
    /** Ticks until Order and Chaos arrive, or -1; and the longest wait heard of, to measure their approach. */
    private int wrathTicks = -1;
    private int wrathTotal;

    private float shake;
    private float flash;
    private float black;
    private float roll;
    /** What he is doing as he speaks (a beat from InBetweenerDialogue), for how long, and how far it has faded since he stopped. */
    private byte cue;
    private int cueTicks = -1;
    private boolean cueEnding;
    private float cueFade = 1F;
    /** Master Order's edict, while it holds: which, ticks left, and how long it was laid down for. */
    private int edictRule;
    private int edictTicks = -1;
    private int edictTotal;
    @Nullable
    private EntityInBetweener body;

    private InBetweenerClient() {
    }

    public void handle(MsgInBetweener m) {
        if (mc.world == null || mc.player == null) {
            return;
        }
        switch (m.type) {
            case MsgInBetweener.SPEAK:
                node = m.value;
                mask = m.mask;
                speakTicks = 0;
                shatterTicks = -1;
                beginBeat(m.cue);
                mc.displayGuiScreen(new GuiInBetweener(node, mask));
                break;
            case MsgInBetweener.CLOSE:
                close();
                break;
            case MsgInBetweener.SHATTER:
                close();
                shatterTicks = m.value;
                shatterBlow = m.mask;
                SoundFx.everywhere(EternitySounds.AMBIENT_RUMBLE, SoundCategory.MASTER, 1F, 0.6F);
                break;
            case MsgInBetweener.SHATTERED:
                shatterTicks = -1;
                effects.add(new InBetweenerFx.Shards(EntityInBetweener.bubbleCentre(), Nexus.BUBBLE_RADIUS, tick));
                effects.add(new FxRenderer.Shockwave(EntityInBetweener.bubbleCentre(), 40D, 30, false));
                double near = mc.player.getPositionVector().distanceTo(EntityInBetweener.bubbleCentre());
                shake = Math.max(shake, 3F * closeness(near, 80D));
                flash = Math.max(flash, 0.7F * closeness(near, 120D));
                break;
            case MsgInBetweener.BATTLE:
                if (m.value == 0) {
                    close();
                    battleTicks = 0;
                    flash = 1F;
                    shake = 2F;
                }
                phase = m.value;
                phaseAt = tick;
                shake = Math.max(shake, 1.2F);
                break;
            case MsgInBetweener.BATTLE_END:
                battleTicks = -1;
                phase = 0;
                black = 1F;
                break;
            case MsgInBetweener.WRATH_SOON:
                wrathTicks = m.value;
                wrathTotal = Math.max(wrathTotal, m.value);
                break;
            case MsgInBetweener.WRATH:
                wrathTicks = -1;
                wrathTotal = 0;
                close();
                effects.add(new InBetweenerFx.Arrival(EntityInBetweener.bubbleCentre(), 18D));
                effects.add(new FxRenderer.Shockwave(EntityInBetweener.bubbleCentre(), 80D, 60, false));
                shake = Math.max(shake, 4F);
                flash = Math.max(flash, 0.9F);
                SoundFx.everywhere(EternitySounds.FINALE_DRUMS, SoundCategory.MASTER, 1.2F, 0.5F);
                break;
            case MsgInBetweener.CAST_OUT:
                close();
                flash = 1F;
                shake = Math.max(shake, 2F);
                SoundFx.everywhere(EternitySounds.RIFT_OPEN, SoundCategory.MASTER, 1.2F, 1.4F);
                break;
            default:
                break;
        }
    }

    private void close() {
        node = -1;
        cueEnding = true;
        if (mc.currentScreen instanceof GuiInBetweener) {
            mc.displayGuiScreen(null);
        }
    }

    // ------------------------------------------------------------------ talking

    /** Characters of his current speech shown so far, not counting formatting codes. */
    public int typed() {
        return node < 0 ? 0 : (int) (speakTicks * WORDS_PER_TICK);
    }

    public boolean doneSpeaking() {
        return node < 0 || typed() >= GuiInBetweener.visibleLength(InBetweenerDialogue.node(node).speech);
    }

    /** Clicking while he speaks lets the rest of his words land at once. */
    public void hurry() {
        if (node >= 0) {
            speakTicks = (int) Math.ceil(GuiInBetweener.visibleLength(InBetweenerDialogue.node(node).speech) / WORDS_PER_TICK);
        }
    }

    public void answer(int question, int choice) {
        EternityNetwork.sendToServer(new MsgInBetweenerAct(MsgInBetweenerAct.ANSWER, question, choice));
        node = -1;
        mc.displayGuiScreen(null);
    }

    public void goOn(int question) {
        EternityNetwork.sendToServer(new MsgInBetweenerAct(MsgInBetweenerAct.CONTINUE, question, 0));
    }


    /** Master Order has laid down an edict: which, and for how long. */
    public void edict(int rule, int ticks) {
        edictRule = rule;
        edictTicks = ticks;
        edictTotal = ticks;
        SoundFx.everywhere(EternitySounds.FINALE_DRUMS, SoundCategory.HOSTILE, 0.9F, 0.5F);
    }

    /** His law, written across the sky in gold, and how long it has left to hold. */
    private void edictHud(int w, int h) {
        String[] laws = {"STAND STILL", "DO NOT STRIKE", "LOOK UPON ME"};
        String law = laws[Math.max(0, Math.min(laws.length - 1, edictRule))];
        float pulse = 0.75F + 0.25F * MathHelper.sin(tick * 0.4F);
        int alpha = Math.max(4, (int) (255F * pulse * Math.min(1F, edictTicks / 10F))) << 24;
        GlStateManager.pushMatrix();
        GlStateManager.translate(w / 2F, h * 0.18F, 0F);
        GlStateManager.scale(1.2F, 1.2F, 1F);
        String title = "E D I C T";
        mc.fontRenderer.drawStringWithShadow(title, -mc.fontRenderer.getStringWidth(title) / 2F, -12F, alpha | 0xC8962A);
        GlStateManager.scale(1.6F, 1.6F, 1F);
        mc.fontRenderer.drawStringWithShadow(law, -mc.fontRenderer.getStringWidth(law) / 2F, 0F, alpha | 0xFFD45A);
        GlStateManager.popMatrix();
        int barWidth = 140;
        int x = (w - barWidth) / 2, y = (int) (h * 0.18F) + 24;
        float left = edictTicks / (float) Math.max(1, edictTotal);
        Gui.drawRect(x - 1, y - 1, x + barWidth + 1, y + 3, 0x90000000);
        Gui.drawRect(x, y, x + (int) (barWidth * left), y + 2, 0xFFFFD45A);
    }
    public int wrathTicks() {
        return wrathTicks;
    }

    /** The post-processing his bubble asks for: the world inside out, then drained, while time stops. */
    @Nullable
    public String shader() {
        if (battleTicks < 0) {
            return null;
        }
        return phase == 1 ? "invert" : phase >= 3 ? "desaturate" : null;
    }

    private boolean inNexus() {
        return mc.world != null && mc.world.provider instanceof WorldProviderNexus;
    }

    // ------------------------------------------------------------------ ticking

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || mc.world == null || mc.player == null || mc.isGamePaused()) {
            return;
        }
        tick++;
        FxRenderer.tickAll(effects);
        tickBeat();
        if (edictTicks >= 0) {
            edictTicks--;
        }
        if (tick % 10 == 0) {
            body = findBody();
        }
        shake *= 0.92F;
        flash *= 0.9F;
        black = Math.max(0F, black - 0.03F);
        if (node >= 0) {
            speakTicks++;
            if (mc.currentScreen == null) {
                mc.displayGuiScreen(new GuiInBetweener(node, mask));
            }
        }
        if (shatterTicks >= 0) {
            shatterTicks--;
        }
        if (wrathTicks >= 0) {
            wrathTicks--;
            if (inNexus() && wrathTicks > 0 && wrathTicks % 100 == 0) {
                // Their coming can be heard, louder and nearer.
                float near = 1F - wrathTicks / (float) Math.max(1, wrathTotal);
                SoundFx.carried(EternitySounds.FAR_HORN, 0.4F + 0.6F * near, 0.5F, SoundFx.angleTo(Nexus.JUNCTION_X + 300D, Nexus.JUNCTION_Z));
                SoundFx.carried(EternitySounds.FAR_GROAN, 0.4F + 0.6F * near, 0.5F, SoundFx.angleTo(Nexus.JUNCTION_X - 300D, Nexus.JUNCTION_Z));
            }
        }
        battle(mc.player);
        float target = battleTicks >= 0 && phase == 1 && !EternityConfig.client.reduceFlashing ? 180F : 0F;
        roll += (target - roll) * 0.05F;
    }

    /** What his bubble does to you, from the inside. */
    private void battle(EntityPlayerSP player) {
        if (battleTicks < 0) {
            return;
        }
        battleTicks++;
        player.fallDistance = 0F;
        switch (phase) {
            case 0:
                if (battleTicks % 25 == 0) {
                    SoundFx.everywhere(EternitySounds.ENTITY_GLITCH, SoundCategory.MASTER, 0.6F, (battleTicks / 25) % 2 == 0 ? 1.4F : 0.5F);
                }
                break;
            case 1:
                // Up and down change places: you fall toward the top of the glass.
                if (!player.capabilities.isFlying) {
                    player.motionY = Math.min(0.6D, player.motionY + 0.13D);
                }
                break;
            case 2:
                if (tick % 30 == 0) {
                    shake = Math.max(shake, 0.8F);
                }
                break;
            default:
                // Before and after: nothing moves but him.
                player.motionX = player.motionY = player.motionZ = 0D;
                player.capabilities.isFlying = false;
                break;
        }
    }

    /** Where his eyes are, for {@link DialogueCamera}; null if he is nowhere near. */
    @Nullable
    public Vec3d headAt(float partialTicks) {
        if (body == null || body.isDead) {
            body = findBody();
        }
        return body == null ? null : body.getPositionEyes(partialTicks);
    }

    @Nullable
    private EntityInBetweener findBody() {
        if (!inNexus()) {
            return null;
        }
        for (Entity entity : mc.world.loadedEntityList) {
            if (entity instanceof EntityInBetweener) {
                return (EntityInBetweener) entity;
            }
        }
        return null;
    }

    /** Held still while time has stopped for you. */
    @SubscribeEvent
    public void onInput(InputUpdateEvent event) {
        if (battleTicks >= 0 && phase >= 3 && event.getEntityPlayer() == mc.player) {
            event.getMovementInput().moveForward = 0F;
            event.getMovementInput().moveStrafe = 0F;
            event.getMovementInput().jump = false;
            event.getMovementInput().sneak = false;
        }
    }

    // ------------------------------------------------------------------ striking the glass

    @SubscribeEvent
    public void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        strike();
    }

    @SubscribeEvent
    public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getWorld().isRemote) {
            strike();
        }
    }

    /** A swing at the air in front of his glass; the server decides whether it reached. */
    private void strike() {
        if (shatterTicks < 0 || tick - lastStrike < 4 || mc.player == null) {
            return;
        }
        lastStrike = tick;
        EternityNetwork.sendToServer(new MsgInBetweenerAct(MsgInBetweenerAct.STRIKE, 0, 0));
        shake = Math.max(shake, 0.5F);
    }

    // ------------------------------------------------------------------ rendering

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Entity view = mc.getRenderViewEntity();
        if (!inNexus() || view == null) {
            return;
        }
        float pt = event.getPartialTicks();
        float time = tick + pt;
        Vec3d camera = new Vec3d(
                view.lastTickPosX + (view.posX - view.lastTickPosX) * pt,
                view.lastTickPosY + (view.posY - view.lastTickPosY) * pt,
                view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * pt);

        if (wrathTicks >= 0) {
            // His masters, rising at opposite edges of everything and drawing nearer, vaster, the closer they come.
            float near = 1F - wrathTicks / (float) Math.max(1, wrathTotal);
            float grow = near * near;
            Vec3d junction = new Vec3d(Nexus.JUNCTION_X, Nexus.BUBBLE_Y, Nexus.JUNCTION_Z);
            Vec3d order = junction.add(lerp(InBetweenerFx.ORDER_FROM, InBetweenerFx.ORDER_AT, grow)).subtract(camera);
            Vec3d chaos = junction.add(lerp(InBetweenerFx.CHAOS_FROM, InBetweenerFx.CHAOS_AT, grow)).subtract(camera);
            float scale = 1.5F + 3.5F * grow;
            float viewYaw = mc.getRenderManager().playerViewY, viewPitch = mc.getRenderManager().playerViewX;
            boolean front = mc.gameSettings.thirdPersonView == 2;
            RenderOrderChaos.draw(true, order.x, order.y, order.z, 90F, scale, time, false, viewYaw, viewPitch, front);
            RenderOrderChaos.draw(false, chaos.x, chaos.y, chaos.z, -90F, scale, time, false, viewYaw, viewPitch, front);
        }

        GlStateManager.pushMatrix();
        GlStateManager.disableFog();
        GlStateManager.disableLighting();
        GlStateManager.disableTexture2D();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.depthMask(false);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
        if (body != null && !body.isDead) {
            float power = body.getState() == EntityInBetweener.State.WARP ? 1F
                    : body.getState() == EntityInBetweener.State.PRESS ? 0.8F : 0.35F;
            InBetweenerFx.bubble(EntityInBetweener.bubbleCentre().subtract(camera), body.shownBubble, body.shownCrack, power, time, look(pt));
            beatExtras(camera, pt, time);
        }
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.depthMask(true);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.enableCull();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();

        if (battleTicks >= 0 && phase == 2 && body != null && mc.player != null) {
            echoes(camera, pt, time);
        }
        FxRenderer.renderAll(effects, camera, pt);
    }

    // ------------------------------------------------------------------ what he does as he speaks

    /** A new thing he does as he begins to speak; anything that happens once happens now. */
    private void beginBeat(byte next) {
        cue = next;
        cueTicks = 0;
        cueEnding = false;
        cueFade = 1F;
        if (body == null) {
            body = findBody();
        }
        Vec3d head = body != null ? body.getPositionVector().add(0D, 1.7D, 0D) : EntityInBetweener.bubbleCentre();
        Vec3d hand = mc.player.getPositionVector().add(0D, 1.1D, 0D);
        switch (next) {
            case InBetweenerDialogue.B_TAP:
                SoundFx.at(SoundEvents.BLOCK_GLASS_HIT, SoundCategory.HOSTILE, head.x, head.y, head.z, 1.2F, 1.9F);
                break;
            case InBetweenerDialogue.B_LAUGH:
                SoundFx.everywhere(EternitySounds.ENTITY_VOICE, SoundCategory.HOSTILE, 0.9F, 1.7F);
                break;
            case InBetweenerDialogue.B_GEM:
            case InBetweenerDialogue.B_OFFER:
                SoundFx.everywhere(EternitySounds.OMEN_SHIMMER, SoundCategory.HOSTILE, 0.8F, 0.8F);
                break;
            case InBetweenerDialogue.B_GEM_TAKE:
                effects.add(new InBetweenerFx.ChaosMaw(head, hand, tick, 30).tint(1F, 0.55F, 0.1F));
                break;
            case InBetweenerDialogue.B_GEM_RETURN:
                effects.add(new InBetweenerFx.ChaosMaw(hand, head, tick, 30).tint(1F, 0.55F, 0.1F));
                break;
            case InBetweenerDialogue.B_SWALLOW:
                flash = Math.max(flash, 0.8F);
                shake = Math.max(shake, 2F);
                break;
            case InBetweenerDialogue.B_FROST:
                SoundFx.everywhere(EternitySounds.AMBIENT_WIND, SoundCategory.HOSTILE, 0.6F, 1.6F);
                break;
            case InBetweenerDialogue.B_DIM:
                SoundFx.everywhere(EternitySounds.AMBIENT_RUMBLE, SoundCategory.HOSTILE, 0.7F, 0.5F);
                break;
            default:
                break;
        }
    }

    /** Ticks the current beat: its sounds that repeat, and its fading once he has stopped speaking. */
    private void tickBeat() {
        if (cueTicks < 0) {
            return;
        }
        cueTicks++;
        if (cueEnding) {
            cueFade -= 1F / 30F;
            if (cueFade <= 0F) {
                cueTicks = -1;
                cue = InBetweenerDialogue.B_NONE;
                return;
            }
        }
        if (body == null) {
            return;
        }
        Vec3d head = body.getPositionVector().add(0D, 1.7D, 0D);
        if (cue == InBetweenerDialogue.B_TAP && (cueTicks == 7 || cueTicks == 14)) {
            SoundFx.at(SoundEvents.BLOCK_GLASS_HIT, SoundCategory.HOSTILE, head.x, head.y, head.z, 1.2F, 1.9F);
        }
        if (cue == InBetweenerDialogue.B_LAUGH && cueTicks < 40 && cueTicks % 8 == 0) {
            SoundFx.everywhere(EternitySounds.ENTITY_VOICE, SoundCategory.HOSTILE, 0.6F, cueTicks % 16 == 0 ? 1.8F : 0.6F);
        }
    }

    /** How strongly the current beat shows: rising as it begins, falling away after he stops. */
    private float envelope(float pt) {
        if (cueTicks < 0) {
            return 0F;
        }
        float in = Math.min(1F, (cueTicks + pt) / 20F);
        return cueEnding ? in * Math.max(0F, cueFade) : in;
    }

    /** What the glass looks like this frame, from what he is doing. */
    private InBetweenerFx.Look look(float pt) {
        InBetweenerFx.Look look = new InBetweenerFx.Look();
        float e = envelope(pt);
        Vec3d toward = mc.player.getPositionVector().add(0D, 1.6D, 0D).subtract(EntityInBetweener.bubbleCentre());
        look.toward = toward.lengthSquared() < 1.0E-4 ? new Vec3d(0D, 0D, 1D) : toward.normalize();
        switch (cue) {
            case InBetweenerDialogue.B_ASCEND:
            case InBetweenerDialogue.B_STARS:
                look.bright = e;
                look.stars = cue == InBetweenerDialogue.B_STARS ? e : 0F;
                break;
            case InBetweenerDialogue.B_FROST:
                look.frost = e * Math.min(1F, (cueTicks + pt) / 60F);
                break;
            case InBetweenerDialogue.B_DIM:
                look.dim = e;
                break;
            case InBetweenerDialogue.B_BECKON:
            case InBetweenerDialogue.B_OFFER:
                look.thin = e * (0.75F + 0.25F * MathHelper.sin((tick + pt) * 0.2F));
                break;
            case InBetweenerDialogue.B_CRACK:
                look.fakeCrack = e * Math.min(1F, (cueTicks + pt) / 25F);
                look.thin = e * 0.4F;
                break;
            case InBetweenerDialogue.B_LAUGH:
                look.ripple = e * Math.max(0F, 1F - (cueTicks + pt) / 60F);
                break;
            case InBetweenerDialogue.B_TAP:
                float since = (cueTicks + pt) % 7F + (cueTicks >= 21 ? 99F : 0F);
                look.tap = since / 7F;
                break;
            case InBetweenerDialogue.B_SWALLOW:
                look.ripple = e;
                look.bright = e * 0.5F;
                break;
            default:
                break;
        }
        return look;
    }

    /**
     * The parts of a beat that are not the glass itself: his eyes burning in the dark, the
     * gem flaring on his brow or held out in his hands, his echoes crowding the inside.
     */
    private void beatExtras(Vec3d camera, float pt, float time) {
        float e = envelope(pt);
        if (body == null || e <= 0.01F) {
            return;
        }
        Vec3d facing = Vec3d.fromPitchYaw(0F, body.rotationYawHead);
        Vec3d side = new Vec3d(-facing.z, 0D, facing.x);
        Vec3d head = new Vec3d(body.posX, body.posY + 1.55D, body.posZ).add(facing.scale(0.26D));
        if (cue == InBetweenerDialogue.B_DIM) {
            // Only his eyes, in the dark: the white one on his white side, the black one rimmed in white on his black side.
            FxRenderer.additiveState();
            glow(head.add(side.scale(-0.12D)).subtract(camera), 0.18D * e, 1F, 1F, 1F, 0.9F * e);
            glow(head.add(side.scale(0.12D)).subtract(camera), 0.2D * e, 0.8F, 0.8F, 0.85F, 0.6F * e);
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            glow(head.add(side.scale(0.12D)).subtract(camera), 0.08D * e, 0F, 0F, 0F, 0.95F * e);
        }
        if (cue == InBetweenerDialogue.B_GEM || cue == InBetweenerDialogue.B_OFFER) {
            FxRenderer.additiveState();
            Vec3d gem = cue == InBetweenerDialogue.B_OFFER
                    ? new Vec3d(body.posX, body.posY + 1.2D, body.posZ).add(facing.scale(0.7D))
                    : head.add(0D, 0.18D, 0D).add(facing.scale(0.05D));
            float pulse = 0.8F + 0.2F * MathHelper.sin(time * 0.3F);
            glow(gem.subtract(camera), 0.9D * e * pulse, 1F, 0.55F, 0.1F, 0.55F * e);
            glow(gem.subtract(camera), 0.25D * e, 1F, 0.9F, 0.6F, 0.9F * e);
        }
        if (cue == InBetweenerDialogue.B_ECHOES) {
            // Copies of him all around the inside of the glass, every one of them looking at you.
            Vec3d centre = EntityInBetweener.bubbleCentre();
            Vec3d you = mc.player.getPositionVector();
            int count = (int) (8 * e);
            GlStateManager.enableTexture2D();
            for (int i = 0; i < count; i++) {
                double a = i * Math.PI * 2D / 8D + time * 0.004D;
                Vec3d p = centre.add(Math.cos(a) * 4.2D, -1.6D + Math.sin(i * 1.7D) * 1.4D, Math.sin(a) * 4.2D);
                RenderInBetweener.ghost = i % 2;
                RenderInBetweener.ghostAlpha = 0.35F * e;
                float yaw = (float) Math.toDegrees(Math.atan2(you.z - p.z, you.x - p.x)) - 90F;
                mc.getRenderManager().renderEntity(body, p.x - camera.x, p.y - camera.y, p.z - camera.z, yaw, pt, false);
            }
            RenderInBetweener.ghost = -1;
        }
    }

    /** A soft disc of light, turned to face the camera. */
    private void glow(Vec3d at, double radius, float r, float g, float b, float a) {
        GlStateManager.disableTexture2D();
        GlStateManager.disableCull();
        GlStateManager.depthMask(false);
        Vec3d view = at.normalize();
        Vec3d u = view.crossProduct(new Vec3d(0D, 1D, 0D));
        u = u.lengthSquared() < 1.0E-4 ? new Vec3d(1D, 0D, 0D) : u.normalize();
        Vec3d v = u.crossProduct(view).normalize();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(at.x, at.y, at.z).color(r, g, b, a).endVertex();
        for (int i = 0; i <= 20; i++) {
            double ang = Math.PI * 2D * i / 20D;
            Vec3d p = at.add(u.scale(Math.cos(ang) * radius)).add(v.scale(Math.sin(ang) * radius));
            buffer.pos(p.x, p.y, p.z).color(r, g, b, 0F).endVertex();
        }
        tessellator.draw();
        GlStateManager.depthMask(true);
    }

    /** Here and there: copies of him, one white and one black, wheeling round you. */
    private void echoes(Vec3d camera, float pt, float time) {
        Vec3d at = mc.player.getPositionVector();
        for (int i = 0; i < 4; i++) {
            double a = time * 0.07D + i * Math.PI / 2D;
            double r = 3.2D + Math.sin(time * 0.05D + i) * 0.6D;
            Vec3d p = at.add(Math.cos(a) * r, Math.sin(time * 0.09D + i) * 0.8D, Math.sin(a) * r);
            RenderInBetweener.ghost = i % 2;
            RenderInBetweener.ghostAlpha = 0.45F + 0.15F * MathHelper.sin(time * 0.2F + i);
            float yaw = (float) Math.toDegrees(Math.atan2(at.z - p.z, at.x - p.x)) - 90F;
            mc.getRenderManager().renderEntity(body, p.x - camera.x, p.y - camera.y, p.z - camera.z, yaw, pt, false);
        }
        RenderInBetweener.ghost = -1;
    }

    // ------------------------------------------------------------------ the screen

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL || mc.world == null || mc.player == null) {
            return;
        }
        ScaledResolution screen = event.getResolution();
        int w = screen.getScaledWidth();
        int h = screen.getScaledHeight();
        float pt = event.getPartialTicks();
        if (battleTicks >= 0) {
            battleOverlay(w, h, pt);
        }
        if (!(mc.currentScreen instanceof GuiInBetweener)) {
            if (shatterTicks >= 0) {
                shatterHud(w, h);
            }
            if (edictTicks > 0 && inNexus()) {
                edictHud(w, h);
            }
            if (wrathTicks >= 0 && inNexus()) {
                wrathHud(w);
            }
        }
        float shownFlash = EternityConfig.client.reduceFlashing ? Math.min(flash, 0.3F) : flash;
        if (black > 0.01F) {
            Gui.drawRect(0, 0, w, h, (int) (Math.min(1F, black) * 255F) << 24);
        }
        if (shownFlash > 0.01F) {
            Gui.drawRect(0, 0, w, h, ((int) (Math.min(1F, shownFlash) * 255F) << 24) | 0xFFFFFF);
        }
    }

    private void battleOverlay(int w, int h, float pt) {
        float t = battleTicks + pt;
        boolean gentle = EternityConfig.client.reduceFlashing;
        if (phase == 0) {
            // Life and death trading places: the world washes white, then black, in time with your heart.
            boolean life = (battleTicks / 25) % 2 == 0;
            float beat = 1F - (t % 25F) / 25F;
            int a = (int) (beat * (gentle ? 50F : 120F));
            Gui.drawRect(0, 0, w, h, (a << 24) | (life ? 0xFFFFFF : 0x000000));
        } else if (phase == 4) {
            // Being and nothing: the world splits down the middle, white from one side, black from the other.
            float p = MathHelper.clamp((tick + pt - phaseAt) / 95F, 0F, 1F);
            int half = (int) (w / 2F * p * p);
            Gui.drawRect(w / 2 - half, 0, w / 2, h, 0xFF000000 | 0xF4F4F6);
            Gui.drawRect(w / 2, 0, w / 2 + half, h, 0xFF050506);
        }
        float since = tick + pt - phaseAt;
        if (since < 80F && phase < PHASE_NAMES.length) {
            float a = MathHelper.clamp(since / 10F, 0F, 1F) * MathHelper.clamp((80F - since) / 20F, 0F, 1F);
            String name = PHASE_NAMES[phase];
            int split = name.length() / 2;
            GlStateManager.pushMatrix();
            GlStateManager.translate(w / 2F, h * 0.3F, 0F);
            GlStateManager.scale(2.4F, 2.4F, 1F);
            int width = mc.fontRenderer.getStringWidth(name);
            int alpha = Math.max(4, (int) (a * 255F)) << 24;
            String left = name.substring(0, split);
            mc.fontRenderer.drawStringWithShadow(left, -width / 2F, -4F, alpha | 0xFFFFFF);
            mc.fontRenderer.drawStringWithShadow(name.substring(split), -width / 2F + mc.fontRenderer.getStringWidth(left), -4F, alpha | 0x6A6A72);
            GlStateManager.popMatrix();
        }
    }

    /** Strike the glass: how near it is to breaking, and how long you have. */
    private void shatterHud(int w, int h) {
        float crack = body != null ? body.shownCrack : 0F;
        float pulse = 0.6F + 0.4F * MathHelper.sin(tick * 0.35F);
        String title = "STRIKE THE GLASS";
        int y = h - 72;
        mc.fontRenderer.drawStringWithShadow(title, (w - mc.fontRenderer.getStringWidth(title)) / 2F, y,
                (Math.max(4, (int) (pulse * 255F)) << 24) | 0xFFFFFF);
        int barWidth = 120;
        int x = (w - barWidth) / 2;
        Gui.drawRect(x - 1, y + 11, x + barWidth + 1, y + 16, 0xC0000000);
        Gui.drawRect(x, y + 12, x + (int) (barWidth * crack), y + 15, 0xFFF0F0F4);
        String left = (shatterTicks + 19) / 20 + "s";
        mc.fontRenderer.drawStringWithShadow(left, x + barWidth + 6, y + 10, 0xFF8A8A92);
        String how = "face it and swing; anything weaker than " + shatterBlow + " glances off";
        mc.fontRenderer.drawStringWithShadow(how, (w - mc.fontRenderer.getStringWidth(how)) / 2F, y + 20, 0xFF6A6A72);
    }

    /** Order and Chaos are coming: how long, and whether you will be near enough to have to face them. */
    private void wrathHud(int w) {
        int seconds = (wrathTicks + 19) / 20;
        float pulse = 0.65F + 0.35F * MathHelper.sin(tick * 0.3F);
        String title = String.format("MASTER ORDER AND LORD CHAOS ARE COMING   %d:%02d", seconds / 60, seconds % 60);
        mc.fontRenderer.drawStringWithShadow(title, (w - mc.fontRenderer.getStringWidth(title)) / 2F, 22,
                (Math.max(4, (int) (pulse * 255F)) << 24) | 0xE8443A);
        double dist = mc.player.getPositionVector().distanceTo(EntityInBetweener.bubbleCentre());
        int radius = EternityConfig.inbetweener.wrathRadius;
        String line = dist < radius
                ? TextFormatting.WHITE + "Stay within " + radius + " blocks and you will have to face them (" + (int) dist + ")"
                : TextFormatting.GRAY + "You are far enough away to escape them.";
        mc.fontRenderer.drawStringWithShadow(line, (w - mc.fontRenderer.getStringWidth(line)) / 2F, 33, 0xFFFFFFFF);
    }

    @SubscribeEvent
    public void onCamera(EntityViewRenderEvent.CameraSetup event) {
        if (Math.abs(roll) > 0.05F) {
            event.setRoll(event.getRoll() + roll);
        }
        if (!EternityConfig.client.screenShake || shake < 0.001F) {
            return;
        }
        float t = tick + (float) event.getRenderPartialTicks();
        event.setYaw(event.getYaw() + (MathHelper.sin(t * 3.1F) + MathHelper.sin(t * 7.3F) * 0.5F) * shake * 0.5F);
        event.setPitch(event.getPitch() + (MathHelper.sin(t * 2.7F) + MathHelper.sin(t * 5.9F) * 0.5F) * shake * 0.5F);
        event.setRoll(event.getRoll() + MathHelper.sin(t * 1.7F) * shake);
    }

    /** Time stopping draws the world in close; at the very end it narrows to a point. */
    @SubscribeEvent
    public void onFov(FOVUpdateEvent event) {
        if (battleTicks < 0) {
            return;
        }
        if (phase == 3) {
            event.setNewfov(event.getNewfov() * 0.85F);
        } else if (phase == 4) {
            float p = MathHelper.clamp((tick - phaseAt) / 100F, 0F, 1F);
            event.setNewfov(event.getNewfov() * (0.85F - 0.45F * p));
        }
    }

    // ------------------------------------------------------------------ cleanup

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.getWorld().isRemote) {
            body = null;
            shatterTicks = -1;
            roll = 0F;
        }
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        mc.addScheduledTask(() -> {
            node = -1;
            shatterTicks = -1;
            battleTicks = -1;
            wrathTicks = -1;
            wrathTotal = 0;
            shake = flash = black = roll = 0F;
            body = null;
            effects.clear();
        });
    }

    private static Vec3d lerp(Vec3d a, Vec3d b, float t) {
        return a.add(b.subtract(a).scale(t));
    }

    private static float closeness(double dist, double range) {
        return (float) MathHelper.clamp(1D - dist / range, 0D, 1D);
    }
}
