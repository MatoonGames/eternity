package com.matoon.eternity.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

/**
 * An in-world cutscene: the view leaves the player's eyes for a camera that flies a path of
 * timed keys (where it is, what it looks at, how wide it sees), and things happen on cue along
 * the way. The path is a cubic through every key with its speed carried smoothly across each
 * one, so the camera never jerks between shots; it eases out of its first key and into its last.
 * <p>
 * It opens with the letterbox coming in and a fade through black; it ends by flying back into
 * the player's own eyes, at their own field of view, and simply handing the view back, so there
 * is no cut at all. Holding jump skips: a fade to black, the view home, and a fade back up.
 * While it runs the player stands still, the HUD and hand are hidden, and clicks do nothing.
 */
@SideOnly(Side.CLIENT)
public final class Cutscene {

    public static final Cutscene INSTANCE = new Cutscene();

    private static final int SKIP_HOLD = 22, SKIP_FADE = 9, POST_FADE = 12, FADE_IN = 14;

    /** Where the camera is at a moment, what it looks at, and its field of view. */
    public static final class Key {
        final float t;
        final Vec3d pos, look;
        final float fov;

        public Key(float t, Vec3d pos, Vec3d look, float fov) {
            this.t = t;
            this.pos = pos;
            this.look = look;
            this.fov = fov;
        }
    }

    public static final class Cue {
        final float t;
        final Runnable run;

        Cue(float t, Runnable run) {
            this.t = t;
            this.run = run;
        }
    }

    private final Minecraft mc = Minecraft.getMinecraft();
    private boolean active;
    private float time;
    private int preroll, length;
    private final List<Key> keys = new ArrayList<>();
    private final List<Cue> cues = new ArrayList<>();
    private int nextCue;
    private Runnable onEnd;
    private EntityArmorStand cam;
    private float fov = 70F;
    private float yawLock, pitchLock;
    // Presentation.
    private float bars, prevBars;
    private int skipHeld, prevSkipHeld;
    private int skipping = -1;
    private int postFade;

    private Cutscene() {
    }

    public boolean isActive() {
        return active;
    }

    /** True while the view is the camera's, not the player's. */
    public boolean isFilming() {
        return active && cam != null && mc.getRenderViewEntity() == cam;
    }

    /**
     * Runs a cutscene: {@code preroll} ticks of fade before its clock reaches 0, then
     * {@code length} ticks along the keys, the last of which should be the player's own eyes.
     */
    public void play(List<Key> path, List<Cue> cueList, int preroll, int length, Runnable onEnd) {
        if (mc.world == null || mc.player == null) {
            return;
        }
        keys.clear();
        keys.addAll(path);
        cues.clear();
        cues.addAll(cueList);
        cues.sort((a, b) -> Float.compare(a.t, b.t));
        nextCue = 0;
        this.preroll = preroll;
        this.length = length;
        this.onEnd = onEnd;
        time = -preroll;
        skipHeld = prevSkipHeld = 0;
        skipping = -1;
        postFade = 0;
        yawLock = mc.player.rotationYaw;
        pitchLock = mc.player.rotationPitch;
        cam = new EntityArmorStand(mc.world);
        cam.setInvisible(true);
        cam.setNoGravity(true);
        active = true;
    }

    public static Cue cue(float t, Runnable run) {
        return new Cue(t, run);
    }

    /** The player's eyes and where they look, as the last key of a path. */
    public Key home(float t) {
        Vec3d eye = mc.player.getPositionEyes(1F);
        return new Key(t, eye, eye.add(mc.player.getLook(1F).scale(10D)), mc.gameSettings.fovSetting);
    }

    // ------------------------------------------------------------------ the clock

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || mc.isGamePaused()) {
            return;
        }
        prevBars = bars;
        prevSkipHeld = skipHeld;
        if (postFade > 0) {
            postFade--;
        }
        if (!active) {
            bars += (0F - bars) * 0.18F;
            return;
        }
        if (mc.world == null || mc.player == null || cam.world != mc.world) {
            stop(false);
            return;
        }
        bars += (1F - bars) * 0.16F;
        if (skipping >= 0) {
            if (++skipping >= SKIP_FADE) {
                stop(true);
            }
            return;
        }
        time++;
        while (nextCue < cues.size() && cues.get(nextCue).t <= time) {
            cues.get(nextCue++).run.run();
        }
        if (time >= 0 && mc.getRenderViewEntity() != cam) {
            place(0F);
            mc.setRenderViewEntity(cam);
            mc.gameSettings.thirdPersonView = 0;
        }
        // Hold jump to skip, once the view is the camera's.
        boolean held = time > 0 && Keyboard.isCreated() && (mc.gameSettings.keyBindJump.isKeyDown() || Keyboard.isKeyDown(Keyboard.KEY_SPACE));
        skipHeld = held ? skipHeld + 1 : Math.max(0, skipHeld - 2);
        if (skipHeld >= SKIP_HOLD) {
            skipping = 0;
        }
        if (time >= length) {
            stop(false);
        }
    }

    /** Ends it: the view back to the player, and the cues that were still to come run so the world is where it should be. */
    private void stop(boolean skipped) {
        if (!active) {
            return;
        }
        active = false;
        while (nextCue < cues.size()) {
            Cue c = cues.get(nextCue++);
            if (c.t >= length - 1) {
                c.run.run();
            }
        }
        if (mc.player != null) {
            mc.setRenderViewEntity(mc.player);
            mc.player.rotationYaw = mc.player.prevRotationYaw = yawLock;
            mc.player.rotationPitch = mc.player.prevRotationPitch = pitchLock;
        }
        if (skipped) {
            postFade = POST_FADE;
        }
        Runnable end = onEnd;
        onEnd = null;
        if (end != null) {
            end.run();
        }
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !active || mc.player == null) {
            return;
        }
        // The player stands as they were left: their eyes are where the camera comes home to.
        mc.player.rotationYaw = mc.player.prevRotationYaw = yawLock;
        mc.player.rotationPitch = mc.player.prevRotationPitch = pitchLock;
        mc.player.rotationYawHead = mc.player.prevRotationYawHead = yawLock;
        if (time >= 0 && cam != null) {
            place(event.renderTickTime);
        }
    }

    /** Puts the camera where the path has it, a fraction of a tick on. */
    private void place(float pt) {
        float t = Math.min(length, time + (skipping >= 0 ? 0F : pt));
        Vec3d[] at = sample(t);
        Vec3d pos = at[0], look = at[1];
        fov = (float) at[2].x;
        double dx = look.x - pos.x, dy = look.y - pos.y, dz = look.z - pos.z;
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90F;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        double y = pos.y - cam.getEyeHeight();
        cam.setLocationAndAngles(pos.x, y, pos.z, yaw, pitch);
        cam.prevPosX = cam.lastTickPosX = pos.x;
        cam.prevPosY = cam.lastTickPosY = y;
        cam.prevPosZ = cam.lastTickPosZ = pos.z;
        cam.prevRotationYaw = cam.rotationYaw = yaw;
        cam.prevRotationPitch = cam.rotationPitch = pitch;
        cam.rotationYawHead = cam.prevRotationYawHead = yaw;
    }

    /** Position, look target, and (fov, 0, 0) at time t, on a cubic Hermite through the keys. */
    private Vec3d[] sample(float t) {
        int n = keys.size();
        if (n == 1 || t <= keys.get(0).t) {
            Key k = keys.get(0);
            return new Vec3d[]{k.pos, k.look, new Vec3d(k.fov, 0D, 0D)};
        }
        if (t >= keys.get(n - 1).t) {
            Key k = keys.get(n - 1);
            return new Vec3d[]{k.pos, k.look, new Vec3d(k.fov, 0D, 0D)};
        }
        int i = 0;
        while (i < n - 2 && keys.get(i + 1).t <= t) {
            i++;
        }
        Key a = keys.get(i), b = keys.get(i + 1);
        double span = b.t - a.t;
        double u = (t - a.t) / span;
        double h00 = 2 * u * u * u - 3 * u * u + 1, h10 = u * u * u - 2 * u * u + u, h01 = -2 * u * u * u + 3 * u * u, h11 = u * u * u - u * u;
        Vec3d pos = hermite(a.pos, b.pos, tangent(i, 0), tangent(i + 1, 0), span, h00, h10, h01, h11);
        Vec3d look = hermite(a.look, b.look, tangent(i, 1), tangent(i + 1, 1), span, h00, h10, h01, h11);
        Vec3d f = hermite(new Vec3d(a.fov, 0D, 0D), new Vec3d(b.fov, 0D, 0D), tangent(i, 2), tangent(i + 1, 2), span, h00, h10, h01, h11);
        return new Vec3d[]{pos, look, f};
    }

    private static Vec3d hermite(Vec3d p0, Vec3d p1, Vec3d m0, Vec3d m1, double span, double h00, double h10, double h01, double h11) {
        return p0.scale(h00).add(m0.scale(h10 * span)).add(p1.scale(h01)).add(m1.scale(h11 * span));
    }

    /** The velocity through key i, per tick: from its neighbours either side; nothing at the two ends, so it eases. */
    private Vec3d tangent(int i, int which) {
        if (i <= 0 || i >= keys.size() - 1) {
            return Vec3d.ZERO;
        }
        Key p = keys.get(i - 1), q = keys.get(i + 1);
        Vec3d a = which == 0 ? p.pos : which == 1 ? p.look : new Vec3d(p.fov, 0D, 0D);
        Vec3d b = which == 0 ? q.pos : which == 1 ? q.look : new Vec3d(q.fov, 0D, 0D);
        return b.subtract(a).scale(1D / Math.max(1D, q.t - p.t));
    }

    // ------------------------------------------------------------------ what the cutscene takes away

    @SubscribeEvent
    public void onFov(EntityViewRenderEvent.FOVModifier event) {
        if (isFilming()) {
            event.setFOV(fov);
        }
    }

    @SubscribeEvent
    public void onInput(InputUpdateEvent event) {
        if (active) {
            event.getMovementInput().moveForward = 0F;
            event.getMovementInput().moveStrafe = 0F;
            event.getMovementInput().forwardKeyDown = event.getMovementInput().backKeyDown = false;
            event.getMovementInput().leftKeyDown = event.getMovementInput().rightKeyDown = false;
            event.getMovementInput().jump = false;
            event.getMovementInput().sneak = false;
        }
    }

    @SubscribeEvent
    public void onMouse(MouseEvent event) {
        if (active && event.getButton() >= 0) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onHand(RenderHandEvent event) {
        if (isFilming()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onHud(RenderGameOverlayEvent.Pre event) {
        if (!active && bars < 0.05F) {
            return;
        }
        switch (event.getType()) {
            case HOTBAR:
            case CROSSHAIRS:
            case HEALTH:
            case ARMOR:
            case FOOD:
            case EXPERIENCE:
            case AIR:
            case HEALTHMOUNT:
            case JUMPBAR:
            case POTION_ICONS:
            case BOSSHEALTH:
            case CHAT:
                event.setCanceled(true);
                break;
            default:
                break;
        }
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        mc.addScheduledTask(() -> {
            active = false;
            onEnd = null;
            bars = prevBars = 0F;
        });
    }

    // ------------------------------------------------------------------ drawing (called by the speaker panel, around itself)

    /** How tall each letterbox bar is right now, in scaled pixels. */
    public float barHeight(ScaledResolution res, float pt) {
        float b = prevBars + (bars - prevBars) * pt;
        return b < 0.01F ? 0F : res.getScaledHeight() * 0.11F * ease(b);
    }

    /** The letterbox, under the speaker panel. */
    public void drawUnder(ScaledResolution res, float pt) {
        float bh = barHeight(res, pt);
        if (bh < 0.5F) {
            return;
        }
        int w = res.getScaledWidth(), h = res.getScaledHeight();
        begin();
        quad(0, 0, w, bh, 0F, 0F, 0F, 1F);
        quad(0, h - bh, w, h, 0F, 0F, 0F, 1F);
        // A hairline of gold on the inner edge of each bar.
        quad(0, bh - 0.5F, w, bh, 0.95F, 0.76F, 0.3F, 0.35F);
        quad(0, h - bh, w, h - bh + 0.5F, 0.95F, 0.76F, 0.3F, 0.35F);
        end();
    }

    /** The fades and the skip prompt, over everything. */
    public void drawOver(ScaledResolution res, float pt) {
        int w = res.getScaledWidth(), h = res.getScaledHeight();
        float black = 0F;
        if (active) {
            float t = time + pt;
            if (t < 0F) {
                black = smooth((t + preroll) / Math.max(1F, preroll - 2F));
            } else if (t < FADE_IN) {
                black = 1F - smooth(t / FADE_IN);
            }
            if (skipping >= 0) {
                black = Math.max(black, smooth((skipping + pt) / SKIP_FADE));
            }
        } else if (postFade > 0) {
            black = smooth((postFade - pt) / POST_FADE);
        }
        if (black > 0.004F) {
            begin();
            quad(0, 0, w, h, 0F, 0F, 0F, black);
            end();
        }
        if (active && time > 6 && skipping < 0) {
            // Hold to skip: a ring that fills while it is held.
            float held = (prevSkipHeld + (skipHeld - prevSkipHeld) * pt) / SKIP_HOLD;
            float bh = barHeight(res, pt);
            FontRenderer font = mc.fontRenderer;
            String label = "Hold " + Keyboard.getKeyName(mc.gameSettings.keyBindJump.getKeyCode()) + " to skip";
            int tw = font.getStringWidth(label);
            float cx = w - 14, cy = h - bh / 2F;
            float appear = smooth((time + pt - 6F) / 20F);
            begin();
            ring(cx, cy, 5.5F, 1.2F, 1F, 0.35F * appear, 1F);
            if (held > 0.01F) {
                ring(cx, cy, 5.5F, 1.6F, 1F, appear, held);
            }
            end();
            GlStateManager.enableTexture2D();
            font.drawString(label, (int) (cx - 10 - tw), (int) (cy - 4), 0xC8CCD8 | (int) (Math.max(0.02F, 0.8F * appear) * 255) << 24);
        }
        GlStateManager.enableTexture2D();
    }

    private static void begin() {
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        Tessellator.getInstance().getBuffer().begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
    }

    private static void end() {
        Tessellator.getInstance().draw();
        GlStateManager.enableAlpha();
    }

    private static void quad(float x0, float y0, float x1, float y1, float r, float g, float b, float a) {
        BufferBuilder buf = Tessellator.getInstance().getBuffer();
        buf.pos(x1, y0, 0).color(r, g, b, a).endVertex();
        buf.pos(x0, y0, 0).color(r, g, b, a).endVertex();
        buf.pos(x0, y1, 0).color(r, g, b, a).endVertex();
        buf.pos(x1, y1, 0).color(r, g, b, a).endVertex();
    }

    /** An arc of a ring, from the top round clockwise, {@code fill} of the way. */
    private static void ring(float cx, float cy, float r, float width, float bright, float a, float fill) {
        BufferBuilder buf = Tessellator.getInstance().getBuffer();
        int seg = 28;
        int steps = Math.max(1, (int) (seg * fill));
        for (int i = 0; i < steps; i++) {
            double a0 = -Math.PI / 2D + Math.PI * 2D * i / seg, a1 = -Math.PI / 2D + Math.PI * 2D * Math.min(fill * seg, i + 1) / seg;
            float ro = r + width / 2F, ri = r - width / 2F;
            buf.pos(cx + Math.cos(a0) * ro, cy + Math.sin(a0) * ro, 0).color(0.95F * bright, 0.78F * bright, 0.35F * bright, a).endVertex();
            buf.pos(cx + Math.cos(a0) * ri, cy + Math.sin(a0) * ri, 0).color(0.95F * bright, 0.78F * bright, 0.35F * bright, a).endVertex();
            buf.pos(cx + Math.cos(a1) * ri, cy + Math.sin(a1) * ri, 0).color(0.95F * bright, 0.78F * bright, 0.35F * bright, a).endVertex();
            buf.pos(cx + Math.cos(a1) * ro, cy + Math.sin(a1) * ro, 0).color(0.95F * bright, 0.78F * bright, 0.35F * bright, a).endVertex();
        }
    }

    private static float smooth(float x) {
        x = MathHelper.clamp(x, 0F, 1F);
        return x * x * (3F - 2F * x);
    }

    private static float ease(float x) {
        x = MathHelper.clamp(x, 0F, 1F);
        return 1F - (1F - x) * (1F - x) * (1F - x);
    }

    /** The camera, for anyone drawing something only it should see. */
    public Entity camera() {
        return cam;
    }
}
