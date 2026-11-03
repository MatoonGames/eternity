package com.matoon.eternity.client;

import com.matoon.eternity.client.render.GlSnapshot;
import com.matoon.eternity.entity.EntityAdamWarlock;
import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.network.MsgStoryAct;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Warlock going, from your own eyes, and the title. You stay where you are; your view is
 * drawn to him on his knees, then taken up after him as he goes, up through the hole he
 * makes and on into the dark; and up there, where he went, ETERNITY comes up out of space:
 * great blocky letters in the old style, made of the dark itself, clear as crystal, their
 * edges drawn in light first and then their faces filling with a cold glow, a sheen running
 * across them. It holds, it goes back into the dark, and your view is your own again.
 */
@SideOnly(Side.CLIENT)
public final class TitleCard {

    /**
     * The word, a glyph to a letter, in the blocky lettering of the old logo: bars three
     * cells thick with a cell between them, rows top to bottom.
     */
    private static final String[][] WORD = {
            {"#########", "#########", "#########", "###......", "########.", "########.", "########.", "###......", "#########",
                    "#########", "#########"},
            {"#########", "#########", "#########", "...###...", "...###...", "...###...", "...###...", "...###...", "...###...",
                    "...###...", "...###..."},
            {"#########", "#########", "#########", "###......", "########.", "########.", "########.", "###......", "#########",
                    "#########", "#########"},
            {"########.", "#########", "#########", "###...###", "########.", "########.", "#######..", "###.###..", "###..###.",
                    "###...###", "###...###"},
            {"####..###", "####..###", "#####.###", "#####.###", "###.#####", "###.#####", "###..####", "###..####", "###...###",
                    "###...###", "###...###"},
            {"###", "###", "###", "###", "###", "###", "###", "###", "###", "###", "###"},
            {"#########", "#########", "#########", "...###...", "...###...", "...###...", "...###...", "...###...", "...###...",
                    "...###...", "...###..."},
            {"###...###", "###...###", "###...###", "####.####", ".#######.", "..#####..", "...###...", "...###...", "...###...",
                    "...###...", "...###..."}};
    private static final int ROWS = 11, GAP = 2, DEPTH = 4;
    /** One cell, in blocks. */
    private static final double CELL = 0.6D;
    /** How far from your eyes the word hangs, toward where he went. */
    private static final double DISTANCE = 40D;
    /** Of the word: its edges drawing in, its faces filling, how long it holds, and its going. */
    private static final int TRACE = 50, FILL = 45, HOLD = 150, GONE = 60;
    /** After Warlock is gone: how long before the word starts to come. */
    private static final int AFTER = 8;

    /** The MARVEL mark over the word: its width as a share of the word's, and its height to its width. */
    private static final net.minecraft.util.ResourceLocation MARVEL = new net.minecraft.util.ResourceLocation(com.matoon.eternity.Eternity.MODID, "textures/gui/marvel.png");
    private static final double LOGO = 0.18D, LOGO_ASPECT = 206D / 512D;

    private static final float[] EDGE = {0.75F, 0.9F, 1F};
    private static final float[] DEEP = {0.1F, 0.16F, 0.5F};

    public static final TitleCard INSTANCE = new TitleCard();

    private final Minecraft mc = Minecraft.getMinecraft();
    private final int[] left = new int[WORD.length];
    private final int width;
    /** The word's edges, in cells: x0 y0 z0 x1 y1 z1, when each draws in, and how bright it burns. */
    private final List<double[]> edges = new ArrayList<>();

    // The sequence, while it runs.
    private int age = -1;
    private Vec3d kneel, gone;
    private int warlockId = -1;
    /** Where the word is, and which way its face looks (at you), fixed when it comes. */
    private Vec3d at, facing;
    private int titleAt = -1;
    private int musicLeft;
    private long lastFrame;

    private TitleCard() {
        int x = 0;
        for (int i = 0; i < WORD.length; i++) {
            left[i] = x;
            x += WORD[i][0].length() + GAP;
        }
        width = x - GAP;
        buildEdges();
    }

    /** Warlock is about to go up out of the Sovereign from where he kneels by her: watch him go, and then the title. */
    public void ascent(Vec3d kneel, Vec3d her) {
        if (mc.player == null || mc.world == null) {
            return;
        }
        this.kneel = kneel;
        this.gone = kneel.add(0D, EntityAdamWarlock.ASCENT_HEIGHT, 0D);
        warlockId = -1;
        double best = 16D;
        for (Entity e : mc.world.loadedEntityList) {
            if (e instanceof EntityAdamWarlock && e.getDistanceSq(kneel.x, kneel.y, kneel.z) < best) {
                best = e.getDistanceSq(kneel.x, kneel.y, kneel.z);
                warlockId = e.getEntityId();
            }
        }
        age = 0;
        titleAt = -1;
        at = null;
    }

    public boolean wantsMusic() {
        return musicLeft > 0;
    }

    private static int burst() {
        return EntityAdamWarlock.KNEEL_BURST;
    }

    private static int length() {
        return burst() + EntityAdamWarlock.ASCENT_RISE + AFTER + TRACE + FILL + HOLD + GONE + 30;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || mc.isGamePaused()) {
            return;
        }
        if (musicLeft > 0) {
            musicLeft--;
        }
        if (age < 0) {
            return;
        }
        age++;
        if (age == burst()) {
            musicLeft = length() - burst() + 400;
        }
        if (age == burst() + EntityAdamWarlock.ASCENT_RISE + AFTER && mc.player != null) {
            // Up there, toward where he went, as far off as the sky seems.
            Vec3d eye = mc.player.getPositionEyes(1F);
            Vec3d toward = gone.subtract(eye).normalize();
            at = eye.add(toward.scale(DISTANCE));
            facing = toward.scale(-1D);
            titleAt = age;
        }
        if (age > length()) {
            age = -1;
            at = null;
            EternityNetwork.sendToServer(new MsgStoryAct(MsgStoryAct.SCENE_DONE, StoryClient.ASCENT, 0));
        }
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        mc.addScheduledTask(() -> {
            age = -1;
            at = null;
            musicLeft = 0;
        });
    }

    // ------------------------------------------------------------------ where you look

    /** Your view drawn to him, then up after him, then held on the word; let go of at the end. */
    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        long now = System.nanoTime();
        float seconds = lastFrame == 0L ? 0F : Math.min(0.1F, (now - lastFrame) / 1.0E9F);
        lastFrame = now;
        EntityPlayerSP player = mc.player;
        if (age < 0 || player == null || mc.world == null) {
            return;
        }
        float t = age + event.renderTickTime;
        Vec3d target;
        float rate;
        int rise = burst() + EntityAdamWarlock.ASCENT_RISE;
        Entity w = warlockId >= 0 ? mc.world.getEntityByID(warlockId) : null;
        if (at != null && titleAt >= 0) {
            if (t > titleAt + TRACE + FILL + HOLD + GONE) {
                return;
            }
            target = at;
            rate = 3F;
        } else if (t >= burst() - 20 && t < rise + AFTER) {
            target = w != null && !w.isDead ? w.getPositionEyes(event.renderTickTime) : gone;
            rate = t < burst() ? 2F : 7F;
        } else if (t < burst() - 20) {
            target = w != null ? w.getPositionEyes(event.renderTickTime) : kneel.add(0D, 1D, 0D);
            rate = 0.8F;
        } else {
            target = gone;
            rate = 4F;
        }
        Vec3d to = target.subtract(player.getPositionEyes(event.renderTickTime));
        double flat = Math.sqrt(to.x * to.x + to.z * to.z);
        float yaw = (float) Math.toDegrees(Math.atan2(-to.x, to.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(to.y, flat));
        float k = 1F - (float) Math.exp(-seconds * rate);
        float turnYaw = MathHelper.wrapDegrees(yaw - player.rotationYaw) * k;
        float turnPitch = (MathHelper.clamp(pitch, -89F, 89F) - player.rotationPitch) * k;
        player.rotationYaw += turnYaw;
        player.prevRotationYaw += turnYaw;
        player.rotationPitch += turnPitch;
        player.prevRotationPitch += turnPitch;
        player.rotationYawHead += turnYaw;
        player.prevRotationYawHead += turnYaw;
    }

    /** Nothing on the screen but the sky, once he goes. */
    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Pre event) {
        if (age < burst() || event.getType() == RenderGameOverlayEvent.ElementType.ALL || event.getType() == RenderGameOverlayEvent.ElementType.CHAT) {
            return;
        }
        event.setCanceled(true);
    }

    // ------------------------------------------------------------------ the word

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Entity view = mc.getRenderViewEntity();
        if (at == null || titleAt < 0 || view == null) {
            return;
        }
        float pt = event.getPartialTicks();
        float t = age - titleAt + pt;
        float going = 1F - smooth((t - TRACE - FILL - HOLD) / GONE);
        if (going <= 0.002F) {
            return;
        }
        Vec3d camera = new Vec3d(view.lastTickPosX + (view.posX - view.lastTickPosX) * pt,
                view.lastTickPosY + (view.posY - view.lastTickPosY) * pt,
                view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * pt);
        // Its face to you, upright to the sky; turning very slowly, so its crystal catches the light.
        Vec3d f = facing;
        Vec3d r = new Vec3d(0D, 1D, 0D).crossProduct(f);
        r = r.lengthSquared() < 1.0E-6 ? new Vec3d(1D, 0D, 0D) : r.normalize();
        Vec3d u = f.crossProduct(r).normalize();
        double sway = Math.sin(t * 0.02D) * 0.12D;
        Vec3d r2 = r.scale(Math.cos(sway)).add(f.scale(Math.sin(sway)));
        Vec3d f2 = f.scale(Math.cos(sway)).subtract(r.scale(Math.sin(sway)));
        Vec3d origin = at.subtract(camera).subtract(r2.scale(width * CELL / 2D)).subtract(u.scale(ROWS * CELL / 2D))
                .add(f2.scale(DEPTH * CELL / 2D));
        Basis b = new Basis(origin, r2, u, f2);

        GlSnapshot before = GlSnapshot.take();
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GlStateManager.disableLighting();
        GL11.glDisable(GL11.GL_LIGHTING);
        GlStateManager.disableFog();
        GL11.glDisable(GL11.GL_FOG);
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        GlStateManager.depthMask(false);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);

        float fill = smooth((t - TRACE * 0.6F) / FILL) * going;
        double sheen = ((t % 120F) / 120F) * (width + 40D) - 20D;
        Vec3d eyeDir = b.at(width / 2D, ROWS / 2D, 0D).normalize();
        faces(b, fill, sheen, eyeDir);
        lines(b, t / TRACE, going, sheen, t);
        logo(b, fill);
        GlStateManager.popMatrix();
        before.restore();
    }

    /** The crystal: faint where you look straight through it, bright at its glancing faces and along its top. */
    private void faces(Basis b, float fill, double sheen, Vec3d eyeDir) {
        if (fill <= 0.003F) {
            return;
        }
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < WORD.length; i++) {
            String[] g = WORD[i];
            for (int row = 0; row < ROWS; row++) {
                for (int col = 0; col < g[0].length(); col++) {
                    if (!solid(g, col, row)) {
                        continue;
                    }
                    double x0 = left[i] + col, y0 = ROWS - 1 - row;
                    float high = 0.6F + 0.4F * (1F - row / (float) ROWS);
                    // Front and back: you look through them.
                    face(buf, b, eyeDir, b.fwd, x0, y0, 0, x0 + 1, y0, 0, x0 + 1, y0 + 1, 0, x0, y0 + 1, 0, high, fill, sheen);
                    face(buf, b, eyeDir, b.fwd, x0, y0, -DEPTH, x0 + 1, y0, -DEPTH, x0 + 1, y0 + 1, -DEPTH, x0, y0 + 1, -DEPTH, high * 0.6F,
                            fill, sheen);
                    if (!solid(g, col, row - 1)) {
                        face(buf, b, eyeDir, b.up, x0, y0 + 1, 0, x0 + 1, y0 + 1, 0, x0 + 1, y0 + 1, -DEPTH, x0, y0 + 1, -DEPTH, 1.3F, fill, sheen);
                    }
                    if (!solid(g, col, row + 1)) {
                        face(buf, b, eyeDir, b.up, x0, y0, 0, x0 + 1, y0, 0, x0 + 1, y0, -DEPTH, x0, y0, -DEPTH, 0.5F, fill, sheen);
                    }
                    if (!solid(g, col - 1, row)) {
                        face(buf, b, eyeDir, b.right, x0, y0, 0, x0, y0 + 1, 0, x0, y0 + 1, -DEPTH, x0, y0, -DEPTH, 0.8F, fill, sheen);
                    }
                    if (!solid(g, col + 1, row)) {
                        face(buf, b, eyeDir, b.right, x0 + 1, y0, 0, x0 + 1, y0 + 1, 0, x0 + 1, y0 + 1, -DEPTH, x0 + 1, y0, -DEPTH, 0.8F, fill,
                                sheen);
                    }
                }
            }
        }
        tess.draw();
    }

    private static void face(BufferBuilder buf, Basis b, Vec3d eyeDir, Vec3d normal, double x0, double y0, double z0, double x1, double y1,
                             double z1, double x2, double y2, double z2, double x3, double y3, double z3, float light, float fill, double sheen) {
        // Glancing faces of crystal throw back more light than ones seen straight on.
        float glance = (float) Math.pow(1D - Math.abs(normal.dotProduct(eyeDir)), 1.5D);
        double[][] corners = {{x0, y0, z0}, {x1, y1, z1}, {x2, y2, z2}, {x3, y3, z3}};
        for (double[] c : corners) {
            Vec3d p = b.at(c[0], c[1], c[2]);
            float s = band(c[0], c[1], sheen);
            float a = (0.24F + 0.4F * glance) * light * fill + s * 0.6F * fill;
            float rr = DEEP[0] + 0.5F * s + 0.25F * glance, gg = DEEP[1] + 0.6F * s + 0.3F * glance, bb = DEEP[2] + 0.5F * s + 0.4F * glance;
            buf.pos(p.x, p.y, p.z).color(Math.min(1F, rr), Math.min(1F, gg), Math.min(1F, bb), Math.min(1F, a)).endVertex();
        }
    }

    /** The edges, drawn in light: first traced in, one after another along the word; then steady, brighter where the sheen is. */
    private void lines(Basis b, float trace, float going, double sheen, float t) {
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        for (double[] e : edges) {
            float grow = MathHelper.clamp((trace - (float) e[6]) * 4F, 0F, 1F);
            if (grow <= 0F) {
                continue;
            }
            boolean drawing = grow < 1F;
            double mx = e[3] * grow + e[0] * (1D - grow), my = e[4] * grow + e[1] * (1D - grow), mz = e[5] * grow + e[2] * (1D - grow);
            Vec3d a = b.at(e[0], e[1], e[2]), c = b.at(mx, my, mz);
            float s = band((e[0] + e[3]) / 2D, (e[1] + e[4]) / 2D, sheen);
            float shimmer = 0.85F + 0.15F * MathHelper.sin(t * 0.15F + (float) e[6] * 40F);
            float alpha = ((float) e[7] * shimmer + s * 0.8F + (drawing ? 0.6F : 0F)) * going;
            strip(buf, a, c, CELL * 0.13D, EDGE, Math.min(1F, alpha));
            strip(buf, a, c, CELL * 0.45D, DEEP, Math.min(1F, alpha * 0.6F));
            if (drawing) {
                // The point of light drawing it.
                strip(buf, c.subtract(b.right.scale(CELL * 0.3D)), c.add(b.right.scale(CELL * 0.3D)), CELL * 0.3D, EDGE, 0.8F * going);
            }
        }
        tess.draw();
    }

    /** The publisher's mark, small, centred over the word, coming up with its crystal. */
    private void logo(Basis b, float alpha) {
        if (alpha <= 0.003F) {
            return;
        }
        double w = width * LOGO, h = w * LOGO_ASPECT, x0 = (width - w) / 2D, y0 = ROWS + 1.6D;
        Vec3d p00 = b.at(x0, y0, 0D), p10 = b.at(x0 + w, y0, 0D), p11 = b.at(x0 + w, y0 + h, 0D), p01 = b.at(x0, y0 + h, 0D);
        GlStateManager.enableTexture2D();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        mc.getTextureManager().bindTexture(MARVEL);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(1F, 1F, 1F, alpha);
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        buf.pos(p01.x, p01.y, p01.z).tex(0D, 0D).endVertex();
        buf.pos(p11.x, p11.y, p11.z).tex(1D, 0D).endVertex();
        buf.pos(p10.x, p10.y, p10.z).tex(1D, 1D).endVertex();
        buf.pos(p00.x, p00.y, p00.z).tex(0D, 1D).endVertex();
        tess.draw();
        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.disableTexture2D();
    }

    /** A camera-facing strip between two camera-relative points, fading to its edges. */
    private static void strip(BufferBuilder buf, Vec3d a, Vec3d b, double width, float[] col, float alpha) {
        if (alpha <= 0.003F) {
            return;
        }
        Vec3d side = b.subtract(a).crossProduct(a.add(b).scale(0.5D));
        if (side.lengthSquared() < 1.0E-10) {
            return;
        }
        side = side.normalize().scale(width);
        for (int half = -1; half <= 1; half += 2) {
            buf.pos(a.x, a.y, a.z).color(col[0], col[1], col[2], alpha).endVertex();
            buf.pos(b.x, b.y, b.z).color(col[0], col[1], col[2], alpha).endVertex();
            buf.pos(b.x + side.x * half, b.y + side.y * half, b.z + side.z * half).color(col[0], col[1], col[2], 0F).endVertex();
            buf.pos(a.x + side.x * half, a.y + side.y * half, a.z + side.z * half).color(col[0], col[1], col[2], 0F).endVertex();
        }
    }

    /**
     * Every edge of the word: round each letter's face, round its back, and along its depth
     * at every corner; each given a moment to be traced in (left to right, with some wander)
     * and how bright it burns (the face brightest, the back faintest).
     */
    private void buildEdges() {
        Random rand = new Random(1987L);
        for (int i = 0; i < WORD.length; i++) {
            String[] g = WORD[i];
            int w = g[0].length();
            for (int row = 0; row < ROWS; row++) {
                for (int col = 0; col < w; col++) {
                    if (!solid(g, col, row)) {
                        continue;
                    }
                    double x0 = left[i] + col, y0 = ROWS - 1 - row;
                    double when = (left[i] + col) / (double) width * 0.75D + rand.nextDouble() * 0.12D;
                    if (!solid(g, col, row - 1)) {
                        edge(x0, y0 + 1, x0 + 1, y0 + 1, when);
                    }
                    if (!solid(g, col, row + 1)) {
                        edge(x0 + 1, y0, x0, y0, when);
                    }
                    if (!solid(g, col - 1, row)) {
                        edge(x0, y0, x0, y0 + 1, when);
                    }
                    if (!solid(g, col + 1, row)) {
                        edge(x0 + 1, y0 + 1, x0 + 1, y0, when);
                    }
                }
            }
            // Along the depth at every corner of the outline.
            for (int row = 0; row <= ROWS; row++) {
                for (int col = 0; col <= w; col++) {
                    boolean a = solid(g, col - 1, row - 1), bb = solid(g, col, row - 1), c = solid(g, col - 1, row), d = solid(g, col, row);
                    int n = (a ? 1 : 0) + (bb ? 1 : 0) + (c ? 1 : 0) + (d ? 1 : 0);
                    if (n == 1 || n == 3 || n == 2 && a == d) {
                        double x = left[i] + col, y = ROWS - row;
                        double when = x / width * 0.75D + 0.1D + rand.nextDouble() * 0.1D;
                        edges.add(new double[]{x, y, 0D, x, y, -DEPTH, when, 0.5D});
                    }
                }
            }
        }
    }

    private void edge(double x0, double y0, double x1, double y1, double when) {
        edges.add(new double[]{x0, y0, 0D, x1, y1, 0D, when, 0.95D});
        edges.add(new double[]{x0, y0, -DEPTH, x1, y1, -DEPTH, when + 0.08D, 0.35D});
    }

    /** How bright the sheen is at a point of the word: a soft diagonal band crossing it. */
    private static float band(double x, double y, double phase) {
        double d = Math.abs(x + y * 0.8D - phase);
        float f = (float) Math.max(0D, 1D - d / 5D);
        return f * f;
    }

    private static boolean solid(String[] g, int col, int row) {
        return row >= 0 && row < g.length && col >= 0 && col < g[row].length() && g[row].charAt(col) == '#';
    }

    /** The word's own frame, in camera-relative blocks: x right along it, y up, z out of its face toward you. */
    private static final class Basis {
        final Vec3d origin, right, up, fwd;

        Basis(Vec3d origin, Vec3d right, Vec3d up, Vec3d fwd) {
            this.origin = origin;
            this.right = right;
            this.up = up;
            this.fwd = fwd;
        }

        Vec3d at(double x, double y, double z) {
            return origin.add(right.scale(x * CELL)).add(up.scale(y * CELL)).add(fwd.scale(z * CELL));
        }
    }

    private static float smooth(float x) {
        x = Math.max(0F, Math.min(1F, x));
        return x * x * (3F - 2F * x);
    }
}
