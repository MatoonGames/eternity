package com.matoon.eternity.client.render;

import com.matoon.eternity.Eternity;
import com.matoon.eternity.network.MsgGrandmaster;
import com.matoon.eternity.world.GrandmasterDirector;
import com.matoon.eternity.world.Kingstable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * The Grandmaster's board as this client sees it: the inlay between its squares, its pieces
 * standing on them and moving between them, the Mind Stone leaping from square to square with
 * the faint mark of where it goes next, the squares a chosen piece may go to, the cracks the
 * board takes, the stakes floating over the pedestal, and everything that happens on it. The
 * blocks themselves are real; everything here is drawn over them.
 */
@SideOnly(Side.CLIENT)
public final class GrandmasterBoard {

    public static final GrandmasterBoard INSTANCE = new GrandmasterBoard();

    private static final ResourceLocation[] PIECES = {null,
            new ResourceLocation(Eternity.MODID, "textures/entity/tafl_raider.png"),
            new ResourceLocation(Eternity.MODID, "textures/entity/tafl_guard.png"),
            new ResourceLocation(Eternity.MODID, "textures/entity/tafl_king.png")};
    private static final int SQ = GrandmasterDirector.SQ, N = Kingstable.SIZE, WIDTH = GrandmasterDirector.WIDTH;
    private static final float[] STONE = {1F, 0.82F, 0.12F};
    private static final float[] PALE = {1F, 0.95F, 0.75F};
    private static final float[] BLUE = {0.35F, 0.6F, 1F};
    private static final float[] RED = {1F, 0.25F, 0.15F};

    private final Minecraft mc = Minecraft.getMinecraft();
    private final ModelTaflPiece model = new ModelTaflPiece();
    private final Random rand = new Random();

    // The board as last told.
    public boolean active;
    public int ox, oy, oz;
    public int phase;
    public boolean built;
    public final byte[] cells = new byte[Kingstable.CELLS];
    public int stone = -1, next = -1;
    public boolean stopped;
    public int selected = -1;
    public int[] targets = new int[0];
    public int[] cracks = new int[Kingstable.CELLS];
    public boolean yourMove;
    public String player = "";
    public int[] anim;
    public long animStart;
    public int gmId = -1;
    public final List<ItemStack> pot = new ArrayList<>();
    public boolean potLost;
    /** Warlock has walked off: the Grandmaster is playing them himself. */
    public boolean alone;
    /** Client: the square under the crosshair, or -1. */
    public int hovered = -1;

    // How the stone moves between where it was and where it is.
    private int stoneFrom = -1;
    private long stoneLeapt;
    private final List<Effect> effects = new ArrayList<>();
    private int tick;

    private GrandmasterBoard() {
    }

    // ------------------------------------------------------------------ what the server tells us

    public void sync(NBTTagCompound tag) {
        if (demo) {
            stashed = tag;
            return;
        }
        boolean was = active;
        active = true;
        ox = tag.getInteger("x");
        oy = tag.getInteger("y");
        oz = tag.getInteger("z");
        phase = tag.getInteger("phase");
        built = tag.getBoolean("built");
        int[] c = tag.getIntArray("cells");
        for (int i = 0; i < Kingstable.CELLS && i < c.length; i++) {
            byte before = cells[i];
            cells[i] = (byte) c[i];
            if (was && before == Kingstable.EMPTY && cells[i] != Kingstable.EMPTY && !movedInto(i)) {
                // Appeared from nowhere: it rises out of the board.
                effects.add(new Rise(i, tick));
            }
        }
        int newStone = tag.hasKey("stone") ? tag.getInteger("stone") : -1;
        if (newStone != stone && stone >= 0 && newStone >= 0) {
            stoneFrom = stone;
            stoneLeapt = time();
        }
        stone = newStone;
        next = tag.hasKey("next") ? tag.getInteger("next") : -1;
        stopped = tag.getBoolean("stopped");
        selected = tag.getInteger("sel");
        targets = tag.getIntArray("targets");
        int[] k = tag.getIntArray("cracks");
        cracks = k.length == Kingstable.CELLS ? k : new int[Kingstable.CELLS];
        yourMove = tag.getBoolean("yourMove");
        player = tag.getString("player");
        anim = tag.hasKey("anim") ? tag.getIntArray("anim") : null;
        animStart = tag.getLong("animStart");
        gmId = tag.getInteger("gm");
        pot.clear();
        NBTTagList items = tag.getTagList("pot", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < items.tagCount(); i++) {
            pot.add(new ItemStack(items.getCompoundTagAt(i)));
        }
        potLost = tag.getBoolean("potLost");
        alone = tag.getBoolean("alone");
    }

    private boolean movedInto(int square) {
        return lastAnim != null && lastAnim[1] == square;
    }

    private int[] lastAnim;

    public void clear() {
        demo = false;
        stashed = null;
        active = false;
        effects.clear();
        anim = null;
        hovered = -1;
    }

    public void effect(MsgGrandmaster m) {
        switch (m.type) {
            case MsgGrandmaster.FX_CARDS:
                effects.add(new CardBurst(m.a, m.value, rand.nextLong()));
                break;
            case MsgGrandmaster.FX_LEAP:
                effects.add(new Streak(m.a, m.b, m.value == 1));
                break;
            case MsgGrandmaster.FX_STRIKE:
                effects.add(new Ring(m.a.add(0D, 0.02D, 0D), 2.2D, 10, m.b.x >= 3 ? RED : PALE, 0.8F));
                effects.add(new Flash(m.a.add(0D, 0.3D, 0D), m.b.x >= 3 ? 2.5D : 1.4D, 8));
                dust(m.a, m.b.x >= 3 ? 50 : 14);
                break;
            case MsgGrandmaster.FX_TAKEN:
                effects.add(new Shards(m.a, (byte) m.value, rand.nextLong(), 1F));
                effects.add(new Ring(m.a.add(0D, 0.05D, 0D), 2.5D, 14, m.value == Kingstable.RAIDER ? STONE : BLUE, 0.7F));
                dust(m.a, 20);
                break;
            case MsgGrandmaster.FX_KING_FALLS:
                effects.add(new Shards(m.a, Kingstable.KING, rand.nextLong(), 1.6F));
                effects.add(new Ring(m.a.add(0D, 0.05D, 0D), 6D, 24, BLUE, 0.9F));
                effects.add(new Flash(m.a.add(0D, 1.5D, 0D), 4D, 14));
                dust(m.a, 40);
                break;
            case MsgGrandmaster.FX_BUILD:
                effects.add(new Ring(m.a.add(0D, 0.06D, 0D), m.value + 1D, 8, STONE, 0.6F));
                break;
            case MsgGrandmaster.FX_COLLAPSE:
                effects.add(new Ring(m.a.add(0D, 0.1D, 0D), 22D, m.value, PALE, 1F));
                effects.add(new Flash(m.a.add(0D, 1D, 0D), 6D, 20));
                break;
            case MsgGrandmaster.FX_STAKE:
                effects.add(new Ring(m.a, 1.6D, 16, STONE, 0.9F));
                effects.add(new Flash(m.a, 1.5D, 12));
                break;
            case MsgGrandmaster.FX_REFUSE:
                effects.add(new Ring(m.a.add(0D, 0.04D, 0D), 1.4D, 10, RED, 0.7F));
                break;
            case MsgGrandmaster.FX_CHEAT:
                effects.add(new Flash(m.a, 1.6D, 14));
                effects.add(new Flash(m.b, 2.2D, 30));
                effects.add(new Ring(new Vec3d(m.b.x, oy + 1.04D, m.b.z), 2.4D, 24, STONE, 0.8F));
                effects.add(new CardBurst(m.b, 10, rand.nextLong()));
                break;
            case MsgGrandmaster.FX_FADE:
                effects.add(new CardBurst(m.a, 30, rand.nextLong()));
                break;
            default:
                break;
        }
    }

    private void dust(Vec3d at, int count) {
        if (mc.world == null) {
            return;
        }
        for (int i = 0; i < count; i++) {
            double a = rand.nextDouble() * Math.PI * 2D, r = rand.nextDouble() * 1.4D;
            BlockPos ground = new BlockPos(at.x + Math.cos(a) * r, oy, at.z + Math.sin(a) * r);
            int id = net.minecraft.block.Block.getStateId(mc.world.getBlockState(ground));
            mc.world.spawnParticle(net.minecraft.util.EnumParticleTypes.BLOCK_CRACK, at.x + Math.cos(a) * r, oy + 1.1D, at.z + Math.sin(a) * r,
                    Math.cos(a) * 0.15D, 0.2D + rand.nextDouble() * 0.3D, Math.sin(a) * 0.15D, id);
        }
    }

    // ------------------------------------------------------------------ the demonstration

    /** While a cutscene borrows the board to show how it is played: the real position, set aside. */
    public boolean demo;
    private byte[] savedCells;
    private int savedStone, savedNext;
    private NBTTagCompound stashed;
    /** Squares lit for the demonstration: a colour (0 none, 1 blue, 2 gold, 3 red, 4 white) and until when. */
    private final int[] glow = new int[Kingstable.CELLS];
    private final int[] glowUntil = new int[Kingstable.CELLS];

    /** Sets the real position aside so the cutscene can move pieces freely. Syncs that arrive meanwhile wait. */
    public void beginDemo() {
        if (demo) {
            return;
        }
        demo = true;
        savedCells = cells.clone();
        savedStone = stone;
        savedNext = next;
        stashed = null;
        selected = -1;
        targets = new int[0];
        anim = null;
    }

    /** The real position back, every piece rising into its place, and anything the server said meanwhile. */
    public void endDemo() {
        if (!demo) {
            return;
        }
        demo = false;
        System.arraycopy(savedCells, 0, cells, 0, cells.length);
        stone = savedStone;
        next = savedNext;
        anim = null;
        java.util.Arrays.fill(glowUntil, 0);
        if (stashed != null) {
            NBTTagCompound tag = stashed;
            stashed = null;
            sync(tag);
        }
        for (int sq = 0; sq < Kingstable.CELLS; sq++) {
            if (cells[sq] != Kingstable.EMPTY) {
                effects.add(new Rise(sq, tick));
            }
        }
        effects.add(new CardBurst(middle().add(0D, 1.5D, 0D), 24, rand.nextLong()));
    }

    /** A piece sets off from one square for another, along the curve of the given kind, for ticks. */
    public void demoMove(int from, int to, int kind, int ticks) {
        anim = new int[]{from, to, kind, ticks};
        animStart = time();
    }

    /** The piece in flight arrives. */
    public void demoLand(int from, int to) {
        cells[to] = cells[from];
        cells[from] = Kingstable.EMPTY;
        anim = null;
        dust(centre(to), 8);
    }

    /** A piece is taken. */
    public void demoTake(int square) {
        byte kind = cells[square];
        cells[square] = Kingstable.EMPTY;
        effect(MsgGrandmaster.fx(kind == Kingstable.KING ? MsgGrandmaster.FX_KING_FALLS : MsgGrandmaster.FX_TAKEN, centre(square), Vec3d.ZERO, kind));
    }

    /** The stone leaps to a square, and shows where it goes after. */
    public void demoStone(int to, int then) {
        if (stone >= 0) {
            effect(MsgGrandmaster.fx(MsgGrandmaster.FX_LEAP, centre(stone).add(0D, 1.1D, 0D), centre(to).add(0D, 1.1D, 0D), 0));
            stoneFrom = stone;
            stoneLeapt = time();
        }
        stone = to;
        next = then;
    }

    /** Lights squares for a while. */
    public void demoGlow(int colour, int ticks, int... squares) {
        for (int sq : squares) {
            glow[sq] = colour;
            glowUntil[sq] = tick + ticks;
        }
    }

    public void demoFx(byte type, Vec3d at, int value) {
        effect(MsgGrandmaster.fx(type, at, Vec3d.ZERO, value));
    }

    private void glows(Vec3d camera, float pt) {
        float t = tick + pt;
        for (int sq = 0; sq < Kingstable.CELLS; sq++) {
            if (glowUntil[sq] <= tick || glow[sq] == 0) {
                continue;
            }
            float left = MathHelper.clamp((glowUntil[sq] - t) / 10F, 0F, 1F);
            float[] col = glow[sq] == 1 ? BLUE : glow[sq] == 2 ? STONE : glow[sq] == 3 ? RED : PALE;
            Vec3d c = centre(sq).add(0D, 0.05D, 0D).subtract(camera);
            float pulse = 0.7F + 0.3F * MathHelper.sin(t * 0.3F + sq);
            WarlockFx.flat(c, 1.5D, col, 0.35F * left * pulse);
            WarlockFx.ring(c, new Vec3d(0D, 1D, 0D), 1.25D, 0.1D, col, 0.85F * left);
        }
    }

    // ------------------------------------------------------------------ geometry

    public Vec3d centre(int square) {
        return new Vec3d(ox + 1 + Kingstable.x(square) * SQ + SQ / 2D, oy + 1, oz + 1 + Kingstable.y(square) * SQ + SQ / 2D);
    }

    public Vec3d middle() {
        return new Vec3d(ox + WIDTH / 2D, oy + 1, oz + WIDTH / 2D);
    }

    public int squareAt(double x, double z) {
        int dx = MathHelper.floor(x) - ox - 1, dz = MathHelper.floor(z) - oz - 1;
        if (dx < 0 || dz < 0 || dx >= N * SQ || dz >= N * SQ) {
            return -1;
        }
        return Kingstable.at(dx / SQ, dz / SQ);
    }

    public boolean isPedestal(BlockPos pos) {
        return pos.getX() == ox + WIDTH / 2 && pos.getZ() == oz + WIDTH + 1 && (pos.getY() == oy + 1 || pos.getY() == oy);
    }

    public boolean near(Vec3d at, double range) {
        return active && at.squareDistanceTo(middle()) < range * range;
    }

    /**
     * What the board should sound like to whoever is near it: his theme while he arrives, talks and
     * teaches; the Sarabande or All the King's Men while Warlock plays; Danse Macabre once he plays
     * them himself; and nothing at all as his board breaks and he dies.
     */
    public net.minecraft.util.SoundEvent music(Vec3d listener) {
        if (!active || !near(listener, 72D)) {
            return null;
        }
        switch (phase) {
            case 1:
            case 2:
            case 3:
            case 4:
                return com.matoon.eternity.EternitySounds.MUSIC_GRANDMASTER;
            case 5:
            case 6:
                return alone ? com.matoon.eternity.EternitySounds.MUSIC_GRANDMASTER_ALONE : com.matoon.eternity.EternitySounds.MUSIC_GRANDMASTER_GAME;
            default:
                return null;
        }
    }

    public boolean isMine() {
        return mc.player != null && mc.player.getUniqueID().toString().equals(player);
    }

    private long time() {
        return mc.world != null ? mc.world.getTotalWorldTime() : 0L;
    }

    /** Where the piece standing on a square is drawn right now, and how high off the board. */
    public Vec3d piecePos(int square, float pt) {
        Vec3d at = centre(square);
        if (anim != null && anim[0] == square) {
            float f = MathHelper.clamp((time() - animStart + pt) / Math.max(1F, anim[3]), 0F, 1F);
            return arc(anim[0], anim[1], f, anim[2]);
        }
        if (square == selected) {
            float t = tick + pt;
            return at.add(0D, 0.35D + MathHelper.sin(t * 0.15F) * 0.06D, 0D);
        }
        return at;
    }

    private Vec3d arc(int from, int to, float f, int kind) {
        Vec3d a = centre(from), b = centre(to);
        double lift = kind == GrandmasterDirector.ANIM_SLIDE ? 0.5D : kind == GrandmasterDirector.ANIM_CARRY ? 2.2D : 1.6D;
        double e = f * f * (3D - 2D * f);
        return a.add(b.subtract(a).scale(e)).add(0D, Math.sin(Math.PI * f) * lift, 0D);
    }

    /**
     * The square whose piece, or failing that whose floor, a ray from the eye first meets
     * within reach; -1 for none.
     */
    public int pick(Vec3d eye, Vec3d look, double reach) {
        int best = -1;
        double bestT = reach;
        for (int sq = 0; sq < Kingstable.CELLS; sq++) {
            if (cells[sq] == Kingstable.EMPTY) {
                continue;
            }
            Vec3d c = piecePos(sq, 0F);
            double r = 0.75D, h = ModelTaflPiece.height(cells[sq]);
            double t = rayBox(eye, look, c.x - r, c.y, c.z - r, c.x + r, c.y + h, c.z + r);
            if (t >= 0D && t < bestT) {
                bestT = t;
                best = sq;
            }
        }
        if (best >= 0) {
            return best;
        }
        // The floor of the board.
        if (Math.abs(look.y) > 1.0E-4) {
            double t = (oy + 1 - eye.y) / look.y;
            if (t > 0D && t < reach) {
                Vec3d p = eye.add(look.scale(t));
                return squareAt(p.x, p.z);
            }
        }
        return -1;
    }

    private static double rayBox(Vec3d o, Vec3d d, double x0, double y0, double z0, double x1, double y1, double z1) {
        double tmin = 0D, tmax = Double.MAX_VALUE;
        double[] os = {o.x, o.y, o.z}, ds = {d.x, d.y, d.z}, lo = {x0, y0, z0}, hi = {x1, y1, z1};
        for (int i = 0; i < 3; i++) {
            if (Math.abs(ds[i]) < 1.0E-9) {
                if (os[i] < lo[i] || os[i] > hi[i]) {
                    return -1D;
                }
                continue;
            }
            double a = (lo[i] - os[i]) / ds[i], b = (hi[i] - os[i]) / ds[i];
            tmin = Math.max(tmin, Math.min(a, b));
            tmax = Math.min(tmax, Math.max(a, b));
            if (tmin > tmax) {
                return -1D;
            }
        }
        return tmin;
    }

    // ------------------------------------------------------------------ ticking

    public void tick() {
        tick++;
        Iterator<Effect> it = effects.iterator();
        while (it.hasNext()) {
            Effect e = it.next();
            e.age++;
            e.tick();
            if (e.age >= e.life) {
                it.remove();
            }
        }
        if (anim != null) {
            lastAnim = anim;
        }
        // Motes of gold rising off the stone.
        if (active && stone >= 0 && mc.world != null && tick % 3 == 0) {
            Vec3d s = stonePos(0F);
            mc.world.spawnParticle(net.minecraft.util.EnumParticleTypes.END_ROD, s.x + rand.nextGaussian() * 0.15D, s.y,
                    s.z + rand.nextGaussian() * 0.15D, 0D, 0.02D, 0D);
        }
    }

    private Vec3d stonePos(float pt) {
        Vec3d at = centre(stone);
        if (stopped) {
            return at.add(0D, -0.7D, 0D);
        }
        float t = tick + pt;
        Vec3d here = at.add(0D, 1.25D + MathHelper.sin(t * 0.09F) * 0.08D, 0D);
        float since = time() - stoneLeapt + pt;
        if (stoneFrom >= 0 && since < 5F) {
            // It does not travel; it is simply elsewhere, after a flicker.
            return since < 2F ? centre(stoneFrom).add(0D, 1.25D, 0D) : here;
        }
        return here;
    }

    // ------------------------------------------------------------------ drawing

    public void render(Vec3d camera, float pt) {
        if (!active || mc.world == null) {
            effectsOnly(camera, pt);
            return;
        }
        GlSnapshot before = GlSnapshot.take();
        GlStateManager.pushMatrix();
        GlStateManager.disableFog();
        if (built) {
            inlay(camera);
            pieces(camera, pt);
            pot(camera, pt);
        }
        GlStateManager.disableLighting();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        GlStateManager.depthMask(false);
        GlStateManager.disableCull();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
        if (built) {
            markers(camera, pt);
            cracks(camera);
            if (stone >= 0) {
                stone(camera, pt);
            }
        }
        for (Effect e : effects) {
            e.draw(camera, pt);
        }
        cardEffects(camera, pt);
        GlStateManager.popMatrix();
        before.restore();
    }

    private void effectsOnly(Vec3d camera, float pt) {
        if (effects.isEmpty()) {
            return;
        }
        GlSnapshot before = GlSnapshot.take();
        GlStateManager.disableLighting();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        GlStateManager.depthMask(false);
        GlStateManager.disableCull();
        for (Effect e : effects) {
            e.draw(camera, pt);
        }
        cardEffects(camera, pt);
        before.restore();
    }

    /** Dark gold lines between the squares, a bright rule round the edge, and the rim glowing with whose move it is. */
    private void inlay(Vec3d camera) {
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableCull();
        int light = mc.world.getCombinedLight(new BlockPos(middle()).up(), 0);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light % 65536, light / 65536);
        double y = oy + 1.003D - camera.y;
        Tessellator t = Tessellator.getInstance();
        BufferBuilder b = t.getBuffer();
        b.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        double x0 = ox + 1 - camera.x, z0 = oz + 1 - camera.z, len = N * SQ;
        for (int i = 0; i <= N; i++) {
            double w = i == 0 || i == N ? 0.09D : 0.06D;
            double p = i * SQ;
            quad(b, x0 + p - w, y, z0, x0 + p + w, y, z0 + len, 0.32F, 0.2F, 0.05F, 0.92F);
            quad(b, x0, y, z0 + p - w, x0 + len, y, z0 + p + w, 0.32F, 0.2F, 0.05F, 0.92F);
        }
        t.draw();
        // Whose move it is, in the light along the rim: blue for yours, gold for theirs.
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
        float pulse = 0.25F + 0.12F * MathHelper.sin(tick * 0.12F);
        float[] col = yourMove ? BLUE : STONE;
        if (phase == 5) {
            b.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            double e0 = ox - camera.x, f0 = oz - camera.z, e1 = e0 + WIDTH, f1 = f0 + WIDTH;
            double yy = y + 0.002D;
            quad(b, e0, yy, f0, e1, yy, f0 + 1, col[0], col[1], col[2], pulse);
            quad(b, e0, yy, f1 - 1, e1, yy, f1, col[0], col[1], col[2], pulse);
            quad(b, e0, yy, f0 + 1, e0 + 1, yy, f1 - 1, col[0], col[1], col[2], pulse);
            quad(b, e1 - 1, yy, f0 + 1, e1, yy, f1 - 1, col[0], col[1], col[2], pulse);
            t.draw();
        }
    }

    private static void quad(BufferBuilder b, double x0, double y0, double z0, double x1, double y1, double z1, float r, float g, float bl, float a) {
        b.pos(x0, y0, z0).color(r, g, bl, a).endVertex();
        b.pos(x0, y1, z1).color(r, g, bl, a).endVertex();
        b.pos(x1, y1, z1).color(r, g, bl, a).endVertex();
        b.pos(x1, y0, z0).color(r, g, bl, a).endVertex();
    }

    /** Every piece, lit by the world, standing on its square or on its way to another. */
    private void pieces(Vec3d camera, float pt) {
        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
        GlStateManager.enableRescaleNormal();
        GlStateManager.disableBlend();
        GlStateManager.enableCull();
        GlStateManager.depthMask(true);
        RenderHelper.enableStandardItemLighting();
        for (int sq = 0; sq < Kingstable.CELLS; sq++) {
            byte kind = cells[sq];
            if (kind == Kingstable.EMPTY) {
                continue;
            }
            float rise = 1F;
            for (Effect e : effects) {
                if (e instanceof Rise && ((Rise) e).square == sq) {
                    rise = ((Rise) e).progress(pt);
                }
            }
            Vec3d at = piecePos(sq, pt);
            if (anim != null && anim[0] == -1 && anim[1] == sq) {
                rise = MathHelper.clamp((time() - animStart + pt) / Math.max(1F, anim[3]), 0F, 1F);
            }
            drawPiece(kind, at.subtract(camera), facing(sq, kind), rise, sq == selected || sq == hovered && yourMove && Kingstable.isDefender(kind));
        }
        // A piece he is bringing into being from nothing.
        if (anim != null && anim[0] == -1 && cells[anim[1]] == Kingstable.EMPTY) {
            float rise = MathHelper.clamp((time() - animStart + pt) / Math.max(1F, anim[3]), 0F, 1F);
            drawPiece(Kingstable.RAIDER, centre(anim[1]).subtract(camera), facing(anim[1], Kingstable.RAIDER), rise, true);
        }
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableLighting();
    }

    private float facing(int square, byte kind) {
        if (kind == Kingstable.KING) {
            return 0F;
        }
        double dx = Kingstable.x(square) - 3, dz = Kingstable.y(square) - 3;
        if (dx == 0 && dz == 0) {
            return 0F;
        }
        float toward = (float) Math.toDegrees(Math.atan2(-dx, -dz));
        return kind == Kingstable.RAIDER ? toward : toward + 180F;
    }

    private void drawPiece(byte kind, Vec3d at, float yaw, float rise, boolean lit) {
        int light = mc.world.getCombinedLight(new BlockPos(at.x + mc.getRenderManager().viewerPosX, oy + 1.5D, at.z + mc.getRenderManager().viewerPosZ), 0);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light % 65536, light / 65536);
        float s = ModelTaflPiece.scale(kind);
        GlStateManager.pushMatrix();
        // Rising out of the board, it comes up through its square.
        GlStateManager.translate(at.x, at.y - (1F - rise) * ModelTaflPiece.height(kind), at.z);
        GlStateManager.rotate(yaw, 0F, 1F, 0F);
        GlStateManager.scale(-s, -s, s);
        mc.getTextureManager().bindTexture(PIECES[kind]);
        GlStateManager.color(1F, 1F, 1F, 1F);
        model.render(kind, 0.0625F);
        if (lit) {
            // Chosen: a soft gold light over it.
            GlStateManager.disableLighting();
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            GlStateManager.depthMask(false);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
            GlStateManager.color(1F, 0.85F, 0.4F, 0.3F + 0.15F * MathHelper.sin(tick * 0.3F));
            model.render(kind, 0.0625F);
            GlStateManager.color(1F, 1F, 1F, 1F);
            GlStateManager.depthMask(true);
            GlStateManager.disableBlend();
            GlStateManager.enableLighting();
        }
        GlStateManager.popMatrix();
    }

    /** What the player has staked, turning slowly in the air over the pedestal. */
    private void pot(Vec3d camera, float pt) {
        if (pot.isEmpty()) {
            return;
        }
        Vec3d c = new Vec3d(ox + WIDTH / 2 + 0.5D, oy + 3.4D, oz + WIDTH + 1.5D).subtract(camera);
        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
        GlStateManager.enableRescaleNormal();
        RenderHelper.enableStandardItemLighting();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
        float t = tick + pt;
        for (int i = 0; i < pot.size(); i++) {
            double a = t * 0.03D + i * Math.PI * 2D / pot.size();
            double r = pot.size() == 1 ? 0D : 0.7D;
            GlStateManager.pushMatrix();
            GlStateManager.translate(c.x + Math.cos(a) * r, c.y + MathHelper.sin(t * 0.07F + i) * 0.1F, c.z + Math.sin(a) * r);
            GlStateManager.rotate(t * 2F + i * 40F, 0F, 1F, 0F);
            GlStateManager.scale(0.8F, 0.8F, 0.8F);
            mc.getTextureManager().bindTexture(net.minecraft.client.renderer.texture.TextureMap.LOCATION_BLOCKS_TEXTURE);
            mc.getRenderItem().renderItem(pot.get(i), ItemCameraTransforms.TransformType.GROUND);
            GlStateManager.popMatrix();
        }
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableLighting();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        GlStateManager.depthMask(false);
        // His, if he has won it: a red light under it. Still in play, gold.
        WarlockFx.glow(c, 1.4D, potLost ? RED : STONE, 0.22F);
        GlStateManager.depthMask(true);
    }

    /** The squares a chosen piece could go to: a ring and a glow on each, the one under the crosshair brightest. */
    private void markers(Vec3d camera, float pt) {
        glows(camera, pt);
        float t = tick + pt;
        for (int sq : targets) {
            Vec3d c = centre(sq).add(0D, 0.04D, 0D).subtract(camera);
            boolean refuge = Kingstable.isRefuge(sq);
            float hover = sq == hovered ? 1F : 0F;
            float pulse = 0.55F + 0.2F * MathHelper.sin(t * 0.25F + sq);
            float[] col = refuge ? BLUE : PALE;
            WarlockFx.flat(c, 1.05D + 0.15D * hover, col, (0.18F + 0.25F * hover) * pulse);
            WarlockFx.ring(c, new Vec3d(0D, 1D, 0D), 0.9D + 0.05D * MathHelper.sin(t * 0.25F + sq), 0.08D, col, 0.6F + 0.4F * hover);
        }
        if (selected >= 0) {
            Vec3d c = centre(selected).add(0D, 0.05D, 0D).subtract(camera);
            WarlockFx.ring(c, new Vec3d(0D, 1D, 0D), 1.3D, 0.1D, STONE, 0.8F);
        } else if (yourMove && hovered >= 0 && Kingstable.isDefender(cells[hovered])) {
            Vec3d c = centre(hovered).add(0D, 0.05D, 0D).subtract(camera);
            WarlockFx.ring(c, new Vec3d(0D, 1D, 0D), 1.2D, 0.06D, PALE, 0.5F);
        }
        // Where the stone goes next: a faint ring and the ghost of its light.
        if (next >= 0 && !stopped) {
            Vec3d c = centre(next).subtract(camera);
            WarlockFx.ring(c.add(0D, 0.05D, 0D), new Vec3d(0D, 1D, 0D), 0.7D, 0.05D, STONE, 0.35F);
            WarlockFx.glow(c.add(0D, 1.25D, 0D), 0.35D, STONE, 0.12F + 0.05F * MathHelper.sin(t * 0.2F));
        }
    }

    /** Cracks in the struck squares, white-hot at their hearts; broken squares rimmed in light over the dark. */
    private void cracks(Vec3d camera) {
        for (int sq = 0; sq < Kingstable.CELLS; sq++) {
            int k = cracks[sq];
            if (k <= 0) {
                continue;
            }
            Vec3d c = centre(sq).add(0D, 0.012D, 0D).subtract(camera);
            if (k >= 3) {
                double h = SQ / 2D;
                Vec3d[] corners = {c.add(-h, 0D, -h), c.add(h, 0D, -h), c.add(h, 0D, h), c.add(-h, 0D, h)};
                for (int i = 0; i < 4; i++) {
                    WarlockFx.strip(corners[i], corners[(i + 1) % 4], 0.12D, PALE, 0.55F);
                }
                continue;
            }
            Random r = new Random(sq * 7919L);
            for (int line = 0; line < k * 4; line++) {
                double a = r.nextDouble() * Math.PI * 2D;
                Vec3d last = c;
                for (int s = 0; s < 4; s++) {
                    a += (r.nextDouble() - 0.5D) * 1.1D;
                    double len = 0.3D + r.nextDouble() * 0.25D;
                    Vec3d p = last.add(Math.cos(a) * len, 0D, Math.sin(a) * len);
                    if (Math.abs(p.x - c.x) > SQ / 2D || Math.abs(p.z - c.z) > SQ / 2D) {
                        break;
                    }
                    WarlockFx.strip(last, p, 0.05D * (1D - s * 0.2D), s == 0 ? WarlockFx.WHITE : PALE, 0.8F - s * 0.15F);
                    last = p;
                }
            }
            WarlockFx.flat(c, 0.6D + k * 0.25D, PALE, 0.08F * k);
        }
    }

    /** The Mind Stone: a gold-yellow gem turning in the air in a halo of its own light, flickering when it leaps. */
    private void stone(Vec3d camera, float pt) {
        Vec3d s = stonePos(pt).subtract(camera);
        float t = tick + pt;
        float since = time() - stoneLeapt + pt;
        boolean flicker = stoneFrom >= 0 && since < 5F && ((int) (since * 3F)) % 2 == 0;
        float bright = flicker ? 0.4F : 1F;
        WarlockFx.glow(s, 1.6D, STONE, 0.18F * bright);
        WarlockFx.glow(s, 0.7D, STONE, 0.45F * bright);
        WarlockFx.glow(s, 0.25D, WarlockFx.WHITE, 0.9F * bright);
        WarlockFx.star(s, 0.12D, 0.9D, 6, (long) (t / 3F), t * 0.02F, STONE, 0.25F * bright);
        gem(s, 0.17D, t * 0.04F, bright);
        if (!stopped) {
            // A beam of faint light down to its square.
            WarlockFx.strip(s, s.add(0D, -1.2D, 0D), 0.08D, 0.02D, STONE, STONE, 0.25F * bright, 0F);
        }
    }

    private static void gem(Vec3d c, double r, float spin, float alpha) {
        Tessellator t = Tessellator.getInstance();
        BufferBuilder b = t.getBuffer();
        b.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
        Vec3d top = c.add(0D, r * 1.5D, 0D), bottom = c.add(0D, -r * 1.5D, 0D);
        for (int i = 0; i < 6; i++) {
            double a0 = spin + i * Math.PI / 3D, a1 = spin + (i + 1) * Math.PI / 3D;
            Vec3d p0 = c.add(Math.cos(a0) * r, 0D, Math.sin(a0) * r), p1 = c.add(Math.cos(a1) * r, 0D, Math.sin(a1) * r);
            float shade = 0.6F + 0.4F * (float) Math.abs(Math.cos(a0 * 1.5D));
            tri(b, top, p0, p1, 1F, 0.9F * shade + 0.1F, 0.3F * shade, alpha);
            tri(b, bottom, p1, p0, 0.9F * shade, 0.65F * shade, 0.05F, alpha);
        }
        t.draw();
    }

    private static void tri(BufferBuilder b, Vec3d a, Vec3d p, Vec3d q, float r, float g, float bl, float alpha) {
        b.pos(a.x, a.y, a.z).color(r, g, bl, alpha).endVertex();
        b.pos(p.x, p.y, p.z).color(r, g, bl, alpha).endVertex();
        b.pos(q.x, q.y, q.z).color(r, g, bl, alpha).endVertex();
    }

    private void cardEffects(Vec3d camera, float pt) {
        boolean any = false;
        for (Effect e : effects) {
            any |= e instanceof CardBurst;
        }
        if (!any) {
            return;
        }
        GlStateManager.enableTexture2D();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.depthMask(true);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
        Cards.bind();
        Cards.begin();
        for (Effect e : effects) {
            if (e instanceof CardBurst) {
                ((CardBurst) e).cards(camera, pt);
            }
        }
        Cards.end();
        GlStateManager.disableTexture2D();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        GlStateManager.depthMask(false);
    }

    // ------------------------------------------------------------------ effects

    private abstract static class Effect {
        int age;
        final int life;

        Effect(int life) {
            this.life = life;
        }

        void tick() {
        }

        void draw(Vec3d camera, float pt) {
        }

        float progress(float pt) {
            return MathHelper.clamp((age + pt) / life, 0F, 1F);
        }
    }

    /** A piece coming up through its square. */
    private static final class Rise extends Effect {
        final int square;

        Rise(int square, int tick) {
            super(24);
            this.square = square;
        }

        @Override
        float progress(float pt) {
            float f = super.progress(pt);
            return 1F - (1F - f) * (1F - f) * (1F - f);
        }
    }

    /** A ring racing out across the board. */
    private static final class Ring extends Effect {
        final Vec3d at;
        final double reach;
        final float[] col;
        final float alpha;

        Ring(Vec3d at, double reach, int life, float[] col, float alpha) {
            super(life);
            this.at = at;
            this.reach = reach;
            this.col = col;
            this.alpha = alpha;
        }

        @Override
        void draw(Vec3d camera, float pt) {
            float f = progress(pt);
            float e = 1F - (1F - f) * (1F - f);
            WarlockFx.ring(at.subtract(camera), new Vec3d(0D, 1D, 0D), 0.2D + reach * e, 0.12D + 0.2D * (1F - f), col, alpha * (1F - f));
        }
    }

    /** A burst of light. */
    private static final class Flash extends Effect {
        final Vec3d at;
        final double size;

        Flash(Vec3d at, double size, int life) {
            super(life);
            this.at = at;
            this.size = size;
        }

        @Override
        void draw(Vec3d camera, float pt) {
            float f = progress(pt);
            WarlockFx.glow(at.subtract(camera), size * (0.6D + 0.4D * f), STONE, 0.5F * (1F - f));
            WarlockFx.glow(at.subtract(camera), size * 0.3D, WarlockFx.WHITE, 0.7F * (1F - f) * (1F - f));
        }
    }

    /** The stone gone from one square and on another: a flicker of light between them. */
    private static final class Streak extends Effect {
        final Vec3d from, to;
        final boolean stutter;

        Streak(Vec3d from, Vec3d to, boolean stutter) {
            super(stutter ? 14 : 8);
            this.from = from;
            this.to = to;
            this.stutter = stutter;
        }

        @Override
        void draw(Vec3d camera, float pt) {
            float f = progress(pt);
            float a = (1F - f) * (stutter ? (age % 2 == 0 ? 0.9F : 0.2F) : 0.6F);
            Vec3d p = from.subtract(camera), q = to.subtract(camera);
            WarlockFx.strip(p, q, 0.05D, STONE, a * 0.6F);
            WarlockFx.glow(p, 0.6D, STONE, a * 0.5F);
            if (stutter) {
                Random r = new Random(age * 31L);
                Vec3d last = p;
                for (int i = 1; i <= 6; i++) {
                    Vec3d n = p.add(q.subtract(p).scale(i / 6D)).add(r.nextGaussian() * 0.3D, r.nextGaussian() * 0.3D, r.nextGaussian() * 0.3D);
                    WarlockFx.strip(last, n, 0.04D, WarlockFx.WHITE, a);
                    last = n;
                }
            }
        }
    }

    /** A piece taken: it breaks into pieces of itself that fly and fall and fade. */
    private static final class Shards extends Effect {
        final double[][] s;
        final float[] col;

        Shards(Vec3d at, byte kind, long seed, float force) {
            super(46);
            Random r = new Random(seed);
            int n = kind == Kingstable.KING ? 46 : 30;
            s = new double[n][7];
            for (int i = 0; i < n; i++) {
                double a = r.nextDouble() * Math.PI * 2D;
                double out = (0.08D + r.nextDouble() * 0.18D) * force;
                s[i] = new double[]{at.x + Math.cos(a) * 0.4D, at.y + 0.3D + r.nextDouble() * 2.3D, at.z + Math.sin(a) * 0.4D,
                        Math.cos(a) * out, 0.1D + r.nextDouble() * 0.25D * force, Math.sin(a) * out, 0.06D + r.nextDouble() * 0.12D};
            }
            col = kind == Kingstable.RAIDER ? STONE : BLUE;
        }

        @Override
        void tick() {
            for (double[] p : s) {
                p[0] += p[3];
                p[1] += p[4];
                p[2] += p[5];
                p[4] -= 0.04D;
                p[3] *= 0.96D;
                p[5] *= 0.96D;
            }
        }

        @Override
        void draw(Vec3d camera, float pt) {
            float f = progress(pt);
            for (double[] p : s) {
                Vec3d c = new Vec3d(p[0] + p[3] * pt, p[1] + p[4] * pt, p[2] + p[5] * pt).subtract(camera);
                WarlockFx.glow(c, p[6] * 2.4D, col, 0.7F * (1F - f));
                WarlockFx.glow(c, p[6], WarlockFx.WHITE, 0.6F * (1F - f));
            }
        }
    }

    /** Cards flung out of a point, tumbling, drifting down. */
    private static final class CardBurst extends Effect {
        final double[][] c;
        final long seed;

        CardBurst(Vec3d at, int count, long seed) {
            super(60);
            this.seed = seed;
            Random r = new Random(seed);
            c = new double[count][6];
            for (int i = 0; i < count; i++) {
                Vec3d d = new Vec3d(r.nextGaussian(), r.nextGaussian() * 0.5D + 0.6D, r.nextGaussian()).normalize().scale(0.15D + r.nextDouble() * 0.25D);
                c[i] = new double[]{at.x, at.y, at.z, d.x, d.y, d.z};
            }
        }

        @Override
        void tick() {
            for (double[] p : c) {
                p[0] += p[3];
                p[1] += p[4];
                p[2] += p[5];
                p[3] *= 0.9D;
                p[5] *= 0.9D;
                p[4] = p[4] * 0.88D - 0.006D;
            }
        }

        void cards(Vec3d camera, float pt) {
            float f = progress(pt);
            for (int i = 0; i < c.length; i++) {
                double[] p = c[i];
                Vec3d at = new Vec3d(p[0] + p[3] * pt, p[1] + p[4] * pt, p[2] + p[5] * pt).subtract(camera);
                Vec3d[] axes = Cards.tumbling(seed + i * 7919L, (age + pt) * 3F);
                Cards.card(at, axes[0], axes[1], Cards.face(seed + i), 1F - f * f, 0.9D);
            }
        }
    }
}
