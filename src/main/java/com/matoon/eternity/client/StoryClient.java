package com.matoon.eternity.client;

import com.matoon.eternity.Eternity;
import com.matoon.eternity.client.Cutscene.Key;
import com.matoon.eternity.client.gui.GuiStoryChoice;
import com.matoon.eternity.client.gui.SpeakerPanel;
import com.matoon.eternity.client.render.StoryFx;
import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.network.MsgStory;
import com.matoon.eternity.network.MsgStoryAct;
import com.matoon.eternity.story.Barrow;
import com.matoon.eternity.world.OdinsVault;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * The story, as this player sees it: whoever is speaking, the answers they may give, the scenes
 * the camera plays; and what the story has done to the world and to them. The cube hanging in its
 * failing ward under the mound; snow still falling where a piece of the north came down; the
 * Aether in its column in the vault, and once it is in you, red veining in at the edges of your
 * sight (leaning toward the one fire that can draw it out) and the world flickering into
 * something else for an instant at a time. And, holding the cube once the chapter is done, the
 * room it was kept in, shown to you for a few heartbeats at a time.
 */
@SideOnly(Side.CLIENT)
public final class StoryClient {

    public static final StoryClient INSTANCE = new StoryClient();

    private static final ResourceLocation VISION = new ResourceLocation(Eternity.MODID, "textures/gui/vision_vault.png");

    private final Minecraft mc = Minecraft.getMinecraft();
    private final Random rand = new Random();

    // What the server has told us.
    private boolean aether, aetherTaken, flameSpent, ward;
    @Nullable
    private Barrow barrow;

    // What is happening on this client.
    private int ticks;
    private int visionLeft, visionLength;
    /** Where snow is still coming down after a piece of the north landed, and for how long. */
    private final List<double[]> snowfall = new ArrayList<>();
    /** A block shown as something else for a moment, and what it really is. */
    @Nullable
    private BlockPos flickered;
    @Nullable
    private IBlockState flickerWas, flickerAs;
    private int flickerLeft;
    private float veins, prevVeins;
    private final List<float[]> veinPaths = new ArrayList<>();

    private StoryClient() {
    }

    public boolean flameSpent() {
        return flameSpent;
    }

    public boolean aetherInPlayer() {
        return aether;
    }

    // ------------------------------------------------------------------ messages

    public void handle(MsgStory m) {
        if (mc.world == null || mc.player == null) {
            return;
        }
        switch (m.type) {
            case MsgStory.LINE:
                if (m.tag.getBoolean("a")) {
                    // Said in the thick of a fight: the panel shows it, the view stays where it is.
                    SpeakerPanel.INSTANCE.say(m.tag.getByte("s"), m.value, m.tag.getString("t"));
                } else {
                    SpeakerPanel.INSTANCE.sayInScene(m.tag.getByte("s"), m.value, m.tag.getString("t"));
                }
                break;
            case MsgStory.ASK: {
                net.minecraft.nbt.NBTTagList list = m.tag.getTagList("a", net.minecraftforge.common.util.Constants.NBT.TAG_STRING);
                String[] answers = new String[list.tagCount()];
                for (int i = 0; i < answers.length; i++) {
                    answers[i] = list.getStringTagAt(i);
                }
                mc.displayGuiScreen(new GuiStoryChoice(m.value, m.tag.getByte("s"), m.tag.getInteger("e"), answers, m.tag.getInteger("n")));
                break;
            }
            case MsgStory.VOTES:
                if (mc.currentScreen instanceof GuiStoryChoice && ((GuiStoryChoice) mc.currentScreen).talk() == m.value) {
                    ((GuiStoryChoice) mc.currentScreen).votes(m.tag);
                }
                break;
            case MsgStory.CHOSEN:
                if (mc.currentScreen instanceof GuiStoryChoice && ((GuiStoryChoice) mc.currentScreen).talk() == m.value) {
                    ((GuiStoryChoice) mc.currentScreen).chosen(m.tag);
                }
                break;
            case MsgStory.UNASK:
                if (mc.currentScreen instanceof GuiStoryChoice) {
                    mc.displayGuiScreen(null);
                }
                break;
            case MsgStory.STATE:
                state(m.tag);
                break;
            case MsgStory.FX_LEAK:
                leak(m.a, m.b, m.value);
                break;
            case MsgStory.FX_TAKEN:
                taken(m.a, m.value);
                break;
            case MsgStory.CUTSCENE:
                cutscene(m.value, m.a, m.b);
                break;
            case MsgStory.FX_DOOR:
                door(m.a, m.b, m.value);
                break;
            case MsgStory.FX_RUNE:
                StoryFx.burst(m.value > 0 ? StoryFx.RUNE_LIT : StoryFx.RUNE_FAIL, m.a, 0);
                for (int i = 0; i < 12; i++) {
                    mc.world.spawnParticle(EnumParticleTypes.END_ROD, m.a.x + rand.nextGaussian() * 0.8D, m.a.y + rand.nextGaussian() * 0.8D,
                            m.a.z + rand.nextGaussian() * 0.8D, 0D, 0.01D, 0D);
                }
                break;
            case MsgStory.FX_WARD_OUT:
                ward = false;
                StoryFx.burst(StoryFx.WARD_OUT, m.a, 0);
                for (int i = 0; i < 40; i++) {
                    mc.world.spawnParticle(EnumParticleTypes.END_ROD, m.a.x, m.a.y, m.a.z, rand.nextGaussian() * 0.2D, rand.nextGaussian() * 0.2D,
                            rand.nextGaussian() * 0.2D);
                }
                break;
            case MsgStory.FX_AETHER:
                aetherTaken = true;
                StoryFx.burst(StoryFx.AETHER_TAKEN, m.a, m.value);
                for (int i = 0; i < 30; i++) {
                    mc.world.spawnParticle(EnumParticleTypes.BLOCK_CRACK, m.a.x, m.a.y, m.a.z, rand.nextGaussian() * 0.25D, rand.nextDouble() * 0.3D,
                            rand.nextGaussian() * 0.25D, net.minecraft.block.Block.getStateId(Blocks.STAINED_GLASS.getStateFromMeta(15)));
                }
                if (m.value == mc.player.getEntityId()) {
                    veins = 1.5F;
                }
                break;
            case MsgStory.FX_FLAME:
                StoryFx.burst(StoryFx.FLAME_SET, m.a, m.value);
                break;
            case MsgStory.FX_RISE:
                for (int i = 0; i < 16; i++) {
                    mc.world.spawnParticle(EnumParticleTypes.BLOCK_DUST, m.a.x + rand.nextGaussian() * 0.4D, m.a.y + 0.3D, m.a.z + rand.nextGaussian() * 0.7D,
                            0D, 0.06D, 0D, net.minecraft.block.Block.getStateId(Blocks.GRAVEL.getDefaultState()));
                }
                break;
            case MsgStory.FX_BELOW:
                below(m.a, m.value);
                break;
            case MsgStory.VISION:
                visionLength = visionLeft = Math.max(20, m.value);
                mc.getSoundHandler().playSound(net.minecraft.client.audio.PositionedSoundRecord.getMasterRecord(SoundEvents.BLOCK_PORTAL_AMBIENT, 1.6F));
                break;
            default:
                break;
        }
    }

    private void state(NBTTagCompound tag) {
        boolean was = aether;
        aether = tag.getBoolean("aether");
        aetherTaken = tag.getBoolean("aetherTaken");
        flameSpent = tag.getBoolean("flameSpent");
        ward = tag.getBoolean("ward");
        barrow = tag.hasKey("barrow") ? new Barrow(BlockPos.fromLong(tag.getLong("barrow")), tag.getInteger("facing")) : null;
        ticking = tag.getBoolean("ticking");
        hedge = tag.hasKey("hedge") ? BlockPos.fromLong(tag.getLong("hedge")) : null;
        hedgeDim = tag.getInteger("hedgeDim");
        if (was && !aether) {
            veins = 0F;
        }
        if (tag.hasKey("journal")) {
            com.matoon.eternity.client.gui.StoryJournal.INSTANCE.receive(tag.getCompoundTag("journal"));
        }
    }

    /** The middle of the Gardener's garden while his hedge stands whole round it, and the world it is in. */
    @Nullable
    private BlockPos hedge;
    private int hedgeDim;

    /** Whether the player stands where a stone in their hand could open the Gardener's hedge, as the server has told us. */
    public boolean nearHedge(net.minecraft.entity.Entity entity) {
        return hedge != null && com.matoon.eternity.story.StoryDirector.nearHedge(hedge, hedgeDim, entity);
    }

    /** On the Gardener's planet, while his stone is still to be found, the stones you carry tick like a clock. */
    private boolean ticking;
    private int tock;

    private void tick() {
        if (!ticking || mc.player == null) {
            return;
        }
        if (++tock % 20 == 0) {
            boolean tick = (tock / 20) % 2 == 0;
            mc.getSoundHandler().playSound(new net.minecraft.client.audio.PositionedSoundRecord(net.minecraft.init.SoundEvents.UI_BUTTON_CLICK.getSoundName(),
                    net.minecraft.util.SoundCategory.MASTER, 0.12F, tick ? 1.9F : 1.6F, false, 0, net.minecraft.client.audio.ISound.AttenuationType.NONE, 0F, 0F, 0F));
        }
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        mc.addScheduledTask(() -> {
            aether = aetherTaken = flameSpent = ward = ticking = false;
            barrow = null;
            snowfall.clear();
            visionLeft = 0;
            flickered = null;
            StoryFx.clear();
        });
    }

    // ------------------------------------------------------------------ ticking

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || mc.world == null || mc.player == null || mc.isGamePaused()) {
            return;
        }
        ticks++;
        StoryFx.tick();
        tick();
        if (visionLeft > 0) {
            visionLeft--;
        }
        snow();
        prevVeins = veins;
        float goal = aether && mc.player.dimension == OdinsVault.dimension() ? 1F : aether ? 0.75F : 0F;
        veins += (goal - veins) * 0.05F;
        flicker();
    }

    /** Snow still coming down where the north landed, thinning out. */
    private void snow() {
        Iterator<double[]> it = snowfall.iterator();
        while (it.hasNext()) {
            double[] s = it.next();
            if (--s[4] <= 0) {
                it.remove();
                continue;
            }
            if (mc.player.getDistanceSq(s[0], s[1], s[2]) > 160D * 160D) {
                continue;
            }
            int n = s[4] > 300 ? 4 : s[4] > 100 ? 2 : rand.nextInt(2);
            for (int i = 0; i < n; i++) {
                double r = s[3] + 2D;
                double x = s[0] + (rand.nextDouble() - 0.5D) * 2D * r, z = s[2] + (rand.nextDouble() - 0.5D) * 2D * r;
                mc.world.spawnAlwaysVisibleParticle(EnumParticleTypes.FIREWORKS_SPARK.getParticleID(), x, s[1] + 6D + rand.nextDouble() * 10D, z,
                        (rand.nextDouble() - 0.5D) * 0.02D, -0.06D, (rand.nextDouble() - 0.5D) * 0.02D);
            }
            if (rand.nextInt(3) == 0) {
                mc.world.spawnParticle(EnumParticleTypes.CLOUD, s[0] + (rand.nextDouble() - 0.5D) * s[3] * 2D, s[1] + 0.2D,
                        s[2] + (rand.nextDouble() - 0.5D) * s[3] * 2D, 0D, 0.005D, 0D);
            }
        }
    }

    private void leak(Vec3d at, Vec3d from, int radius) {
        snowfall.add(new double[]{at.x, at.y, at.z, radius, 500});
        for (int i = 0; i < 60; i++) {
            mc.world.spawnParticle(EnumParticleTypes.SNOW_SHOVEL, at.x + rand.nextGaussian() * radius, at.y + 0.5D + rand.nextDouble() * 2D,
                    at.z + rand.nextGaussian() * radius, rand.nextGaussian() * 0.1D, rand.nextDouble() * 0.25D, rand.nextGaussian() * 0.1D);
        }
        for (int i = 0; i < 16; i++) {
            mc.world.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL, at.x + rand.nextGaussian() * radius * 0.6D, at.y + 0.6D,
                    at.z + rand.nextGaussian() * radius * 0.6D, 0D, 0.04D, 0D);
        }
    }

    private void taken(Vec3d at, int how) {
        if (how < 0) {
            // The Aether rewriting a block: a breath of red where it was.
            for (int i = 0; i < 10; i++) {
                mc.world.spawnParticle(EnumParticleTypes.REDSTONE, at.x + rand.nextGaussian() * 0.4D, at.y + rand.nextDouble() * 0.6D,
                        at.z + rand.nextGaussian() * 0.4D, 0D, 0D, 0D);
            }
            return;
        }
        snowfall.add(new double[]{at.x, at.y, at.z, 2D, 120});
        for (int i = 0; i < 40; i++) {
            mc.world.spawnParticle(EnumParticleTypes.SNOW_SHOVEL, at.x + rand.nextGaussian() * 1.2D, at.y + 0.3D + rand.nextDouble() * 1.5D,
                    at.z + rand.nextGaussian() * 1.2D, rand.nextGaussian() * 0.08D, rand.nextDouble() * 0.15D, rand.nextGaussian() * 0.08D);
        }
    }

    private void door(Vec3d at, Vec3d in, int open) {
        for (int i = 0; i < 50; i++) {
            mc.world.spawnParticle(EnumParticleTypes.BLOCK_DUST, at.x + rand.nextGaussian() * 1.1D, at.y + rand.nextGaussian() * 1.1D,
                    at.z + rand.nextGaussian() * 1.1D, in.x * -0.04D, 0.02D, in.z * -0.04D, net.minecraft.block.Block.getStateId(Blocks.STONEBRICK.getDefaultState()));
        }
    }

    /** The fight under the mound, heard through the stone. */
    private void below(Vec3d at, int force) {
        if (force <= 0) {
            play(SoundEvents.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, at, 0.5F, 0.4F);
            return;
        }
        SoundEvent[] blows = {SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, SoundEvents.ENTITY_ZOMBIE_ATTACK_DOOR_WOOD, SoundEvents.ENTITY_SKELETON_HURT,
                SoundEvents.ENTITY_PLAYER_HURT};
        play(blows[rand.nextInt(blows.length)], at, 0.18F * force, 0.45F + rand.nextFloat() * 0.15F);
    }

    private void play(SoundEvent sound, Vec3d at, float volume, float pitch) {
        mc.world.playSound(at.x, at.y, at.z, sound, SoundCategory.HOSTILE, volume, pitch, false);
    }

    /** With the Aether in you, now and then a block near you is something else for a moment. */
    private void flicker() {
        if (flickered != null) {
            if (--flickerLeft <= 0) {
                if (mc.world.getBlockState(flickered) == flickerAs && flickerWas != null) {
                    mc.world.setBlockState(flickered, flickerWas, 2);
                }
                flickered = null;
            }
            return;
        }
        if (!aether || rand.nextInt(50) != 0) {
            return;
        }
        BlockPos p = new BlockPos(mc.player).add(rand.nextInt(11) - 5, rand.nextInt(5) - 2, rand.nextInt(11) - 5);
        IBlockState s = mc.world.getBlockState(p);
        if (s.getMaterial() == net.minecraft.block.material.Material.AIR || mc.world.getTileEntity(p) != null || !s.isFullCube()) {
            return;
        }
        IBlockState[] as = {Blocks.RED_SANDSTONE.getDefaultState(), Blocks.NETHER_WART_BLOCK.getDefaultState(), Blocks.BONE_BLOCK.getDefaultState(),
                Blocks.SAND.getStateFromMeta(1), Blocks.STAINED_GLASS.getStateFromMeta(14)};
        flickerAs = as[rand.nextInt(as.length)];
        flickerWas = s;
        flickered = p;
        flickerLeft = 2 + rand.nextInt(4);
        mc.world.setBlockState(p, flickerAs, 2);
    }

    // ------------------------------------------------------------------ the world

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (mc.world == null || mc.player == null) {
            return;
        }
        float pt = event.getPartialTicks();
        Entity view = mc.getRenderViewEntity() != null ? mc.getRenderViewEntity() : mc.player;
        Vec3d camera = new Vec3d(view.lastTickPosX + (view.posX - view.lastTickPosX) * pt, view.lastTickPosY + (view.posY - view.lastTickPosY) * pt,
                view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * pt);
        float age = ticks + pt;
        if (barrow != null && ward && mc.player.dimension == 0) {
            Vec3d cube = barrow.cube();
            if (cube.squareDistanceTo(camera) < 64D * 64D) {
                Vec3d floor = barrow.vec(0D, Barrow.FLOOR + 1, Barrow.WARD);
                StoryFx.ward(cube.subtract(camera), floor.subtract(camera), 3.2D, age);
            }
        }
        if (!aetherTaken && mc.player.dimension == OdinsVault.dimension()) {
            Vec3d foot = OdinsVault.Relic.TESSERACT.base().add(0D, OdinsVault.Relic.TESSERACT.height + 1D / 16D, 0D);
            if (foot.squareDistanceTo(camera) < 64D * 64D) {
                StoryFx.aether(foot.subtract(camera), age);
            }
        }
        StoryFx.bursts(camera, pt);
    }

    // ------------------------------------------------------------------ the screen

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onOverlay(RenderGameOverlayEvent.Pre event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL || mc.player == null) {
            return;
        }
        float pt = event.getPartialTicks();
        ScaledResolution res = event.getResolution();
        float v = prevVeins + (veins - prevVeins) * pt;
        if (v > 0.01F) {
            veins(res, v, pt);
        }
        if (visionLeft > 0) {
            vision(res, pt);
        }
    }

    /**
     * Red veining in from the edges of sight, beating slowly. In the vault, the veins on the side
     * facing the Eternal Flame grow longer and brighter, as if pulled toward it.
     */
    private void veins(ScaledResolution res, float strength, float pt) {
        int w = res.getScaledWidth(), h = res.getScaledHeight();
        if (veinPaths.isEmpty()) {
            buildVeins();
        }
        float beat = (ticks + pt) % 26F / 26F;
        float pulse = 0.65F + 0.35F * (float) Math.exp(-beat * 9F) + 0.15F * (float) Math.exp(-Math.max(0F, beat - 0.22F) * 12F);
        // Where the flame is on the screen, as an angle round its middle, if it is anywhere near.
        float pullAngle = 0F, pull = 0F;
        if (mc.player.dimension == OdinsVault.dimension() && !flameSpent) {
            Vec3d flame = OdinsVault.Relic.ETERNAL_FLAME.base().add(0D, OdinsVault.Relic.ETERNAL_FLAME.height + 0.5D, 0D);
            Vec3d to = flame.subtract(mc.player.getPositionEyes(pt));
            double dist = to.length();
            float yawTo = (float) Math.toDegrees(Math.atan2(-to.x, to.z));
            float rel = MathHelper.wrapDegrees(yawTo - mc.player.rotationYaw);
            pullAngle = (float) Math.toRadians(rel);
            pull = (float) MathHelper.clamp(1D - dist / 40D, 0D, 1D);
        }
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        Tessellator t = Tessellator.getInstance();
        BufferBuilder b = t.getBuffer();
        // A dim red bleed at the very edges first.
        edge(b, t, w, h, 0.18F * strength * pulse);
        b.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        for (float[] path : veinPaths) {
            // Which side of the screen it grows from, as an angle (0 = right, up = negative).
            float side = path[0];
            float lean = pull > 0F ? (float) Math.max(0D, Math.cos(side - (pullAngle - (float) Math.PI / 2F) - (float) Math.PI / 2F)) : 0F;
            float grow = MathHelper.clamp(strength * (0.75F + 0.6F * lean * pull), 0F, 1.3F);
            int points = (path.length - 1) / 2;
            int shown = (int) (points * Math.min(1F, grow));
            for (int i = 0; i < shown - 1; i++) {
                float x0 = path[1 + i * 2] * w, y0 = path[2 + i * 2] * h, x1 = path[3 + i * 2] * w, y1 = path[4 + i * 2] * h;
                float along = i / (float) points;
                float width = (2.6F - along * 2.2F) * (0.8F + 0.4F * lean * pull);
                float a = (0.85F - along * 0.7F) * strength * pulse * (1F + lean * pull * 0.6F);
                float dx = y1 - y0, dy = -(x1 - x0);
                float len = (float) Math.sqrt(dx * dx + dy * dy);
                if (len < 1.0E-3F) {
                    continue;
                }
                dx = dx / len * width * 0.5F;
                dy = dy / len * width * 0.5F;
                b.pos(x0 - dx, y0 - dy, 0).color(0.55F, 0.02F, 0.03F, Math.min(0.9F, a)).endVertex();
                b.pos(x0 + dx, y0 + dy, 0).color(0.55F, 0.02F, 0.03F, Math.min(0.9F, a)).endVertex();
                b.pos(x1 + dx * 0.85F, y1 + dy * 0.85F, 0).color(0.75F, 0.05F, 0.05F, Math.min(0.9F, a * 0.9F)).endVertex();
                b.pos(x1 - dx * 0.85F, y1 - dy * 0.85F, 0).color(0.75F, 0.05F, 0.05F, Math.min(0.9F, a * 0.9F)).endVertex();
            }
        }
        t.draw();
        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();
        GlStateManager.disableBlend();
    }

    private static void edge(BufferBuilder b, Tessellator t, int w, int h, float a) {
        float d = Math.min(w, h) * 0.12F;
        b.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        quad(b, 0, 0, w, d, a, a, 0F, 0F);
        quad(b, 0, h - d, w, h, 0F, 0F, a, a);
        b.pos(d, 0, 0).color(0.4F, 0F, 0.02F, 0F).endVertex();
        b.pos(0, 0, 0).color(0.4F, 0F, 0.02F, a).endVertex();
        b.pos(0, h, 0).color(0.4F, 0F, 0.02F, a).endVertex();
        b.pos(d, h, 0).color(0.4F, 0F, 0.02F, 0F).endVertex();
        b.pos(w, 0, 0).color(0.4F, 0F, 0.02F, a).endVertex();
        b.pos(w - d, 0, 0).color(0.4F, 0F, 0.02F, 0F).endVertex();
        b.pos(w - d, h, 0).color(0.4F, 0F, 0.02F, 0F).endVertex();
        b.pos(w, h, 0).color(0.4F, 0F, 0.02F, a).endVertex();
        t.draw();
    }

    private static void quad(BufferBuilder b, float x0, float y0, float x1, float y1, float aTopL, float aTopR, float aBotR, float aBotL) {
        b.pos(x1, y0, 0).color(0.4F, 0F, 0.02F, aTopR).endVertex();
        b.pos(x0, y0, 0).color(0.4F, 0F, 0.02F, aTopL).endVertex();
        b.pos(x0, y1, 0).color(0.4F, 0F, 0.02F, aBotL).endVertex();
        b.pos(x1, y1, 0).color(0.4F, 0F, 0.02F, aBotR).endVertex();
    }

    /** Branching walks in from the screen's edges, in screen fractions, made once. */
    private void buildVeins() {
        Random r = new Random(1949L);
        for (int i = 0; i < 26; i++) {
            float side = (float) (i / 26D * Math.PI * 2D + r.nextGaussian() * 0.1D);
            // Start on the edge in that direction, and wander inward.
            float cx = 0.5F + 0.5F * MathHelper.cos(side) * 1.02F, cy = 0.5F + 0.5F * MathHelper.sin(side) * 1.02F;
            float x = MathHelper.clamp(cx, -0.02F, 1.02F), y = MathHelper.clamp(cy, -0.02F, 1.02F);
            int n = 7 + r.nextInt(6);
            float[] path = new float[1 + n * 2];
            path[0] = side;
            float heading = side + (float) Math.PI;
            for (int k = 0; k < n; k++) {
                path[1 + k * 2] = x;
                path[2 + k * 2] = y;
                heading += (float) r.nextGaussian() * 0.5F;
                float step = 0.018F + r.nextFloat() * 0.02F;
                x += MathHelper.cos(heading) * step * 0.65F;
                y += MathHelper.sin(heading) * step;
            }
            veinPaths.add(path);
        }
    }

    /**
     * The room the cube was kept in, shown through it: it flickers in, holds a few heartbeats
     * through a wash of the cube's blue, and is gone.
     */
    private void vision(ScaledResolution res, float pt) {
        int w = res.getScaledWidth(), h = res.getScaledHeight();
        float left = visionLeft - pt;
        float k = 1F - left / visionLength;
        float in = MathHelper.clamp(k / 0.18F, 0F, 1F), out = MathHelper.clamp(left / 14F, 0F, 1F);
        float flick = (k < 0.18F || left < 14F) && rand.nextInt(3) == 0 ? 0.35F : 1F;
        float a = in * out * flick;
        if (a <= 0.01F) {
            return;
        }
        mc.getTextureManager().bindTexture(VISION);
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        // A slow drift inward, as if walking into it.
        float zoom = 1.04F + 0.06F * k;
        float iw = w * zoom, ih = h * zoom;
        float x0 = (w - iw) / 2F, y0 = (h - ih) / 2F;
        Tessellator t = Tessellator.getInstance();
        BufferBuilder b = t.getBuffer();
        GlStateManager.color(0.75F, 0.85F, 1F, 0.82F * a);
        tex(b, t, x0, y0, iw, ih);
        // The cube's light through it, offset a little: red and blue never quite agree.
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        GlStateManager.color(0.15F, 0.35F, 1F, 0.25F * a);
        tex(b, t, x0 - 3F, y0, iw, ih);
        GlStateManager.color(0.6F, 0.1F, 0.3F, 0.12F * a);
        tex(b, t, x0 + 3F, y0, iw, ih);
        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        // Dark at the edges: you are seeing it through something.
        b.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        b.pos(w / 2F, h / 2F, 0).color(0F, 0.02F, 0.08F, 0F).endVertex();
        for (int i = 0; i <= 32; i++) {
            double ang = i * Math.PI * 2D / 32D;
            b.pos(w / 2F + Math.cos(ang) * w * 0.75D, h / 2F + Math.sin(ang) * h * 0.75D, 0).color(0F, 0.02F, 0.08F, 0.9F * a).endVertex();
        }
        t.draw();
        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();
        GlStateManager.disableBlend();
    }

    private static void tex(BufferBuilder b, Tessellator t, float x, float y, float w, float h) {
        b.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        b.pos(x, y + h, 0).tex(0, 1).endVertex();
        b.pos(x + w, y + h, 0).tex(1, 1).endVertex();
        b.pos(x + w, y, 0).tex(1, 0).endVertex();
        b.pos(x, y, 0).tex(0, 0).endVertex();
        t.draw();
    }

    /** A right-click toward the cube in its ward, over nothing solid: the server is told, and decides. */
    @SubscribeEvent
    public void onMouse(net.minecraftforge.client.event.MouseEvent event) {
        if (event.getButton() != 1 || !event.isButtonstate() || mc.player == null || mc.currentScreen != null) {
            return;
        }
        if (visionLeft > 0 && aether && mc.player.getHeldItemMainhand().isEmpty()) {
            // Reaching into the vision with nothing in hand: the Aether in them answers it.
            EternityNetwork.sendToServer(new MsgStoryAct(MsgStoryAct.REACH, 0, 0));
            return;
        }
        if (!ward || barrow == null) {
            return;
        }
        Vec3d to = barrow.cube().subtract(mc.player.getPositionEyes(1F));
        if (to.length() < 4.5D && mc.player.getLookVec().dotProduct(to.normalize()) > 0.9D) {
            EternityNetwork.sendToServer(new MsgStoryAct(MsgStoryAct.LIFT, 0, 0));
        }
    }

    /** Using the cube while it shows the vault: the server takes you there, and nothing else happens here. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onUseItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getWorld().isRemote && visionLeft > 0 && (aether || event.getItemStack().getItem() == lucraft.mods.heroesexpansion.items.HEItems.TESSERACT)) {
            // The server has already been told of the click (it is sent before this fires) and takes them there.
            event.setCanceled(true);
        }
    }

    // ------------------------------------------------------------------ scenes

    public static final int RAGNA = 1, VAULT_BEAT = 2, ASCENT = 3;

    /**
     * Somewhere for the camera to stand to see a point: as near as can be to the way it would
     * like to look from, at that distance and height over whatever ground is there, in open air,
     * and with nothing in the way. Tried round the point both ways from the preferred side.
     */
    private Vec3d vantage(Vec3d target, Vec3d preferred, double distance, double height) {
        Vec3d dir = new Vec3d(preferred.x, 0D, preferred.z);
        dir = dir.lengthSquared() < 1.0E-4 ? new Vec3d(1D, 0D, 0D) : dir.normalize();
        double base = Math.atan2(dir.z, dir.x);
        Vec3d fallback = target.add(dir.scale(distance)).add(0D, height + 2D, 0D);
        for (int i = 0; i < 24; i++) {
            double a = base + (i % 2 == 0 ? 1 : -1) * ((i + 1) / 2) * Math.PI / 12D;
            double x = target.x + Math.cos(a) * distance, z = target.z + Math.sin(a) * distance;
            BlockPos col = new BlockPos(x, target.y + 6D, z);
            int y = col.getY();
            while (y > target.y - 6D && mc.world.isAirBlock(new BlockPos(x, y - 1, z))) {
                y--;
            }
            Vec3d at = new Vec3d(x, Math.max(y + height, target.y + height * 0.6D), z);
            BlockPos p = new BlockPos(at);
            if (!mc.world.isAirBlock(p) && mc.world.getBlockState(p).isFullCube() || !mc.world.isAirBlock(p.up())) {
                continue;
            }
            if (mc.world.rayTraceBlocks(at, target, false, true, false) == null) {
                return at;
            }
        }
        return fallback;
    }

    private void cutscene(int id, Vec3d a, Vec3d b) {
        List<Key> keys = new ArrayList<>();
        Cutscene cut = Cutscene.INSTANCE;
        float fov = mc.gameSettings.fovSetting;
        int length;
        if (id == RAGNA) {
            // Over the rise toward them, slowly, and in close while she dies; then back.
            Vec3d mid = a.add(b).scale(0.5D);
            Vec3d across = new Vec3d(-(a.z - b.z), 0D, a.x - b.x);
            across = across.lengthSquared() < 1.0E-4 ? new Vec3d(1D, 0D, 0D) : across.normalize();
            Vec3d eye = mc.player.getPositionEyes(1F);
            if (across.dotProduct(eye.subtract(mid)) < 0D) {
                across = across.scale(-1D);
            }
            length = 300;
            Vec3d look = mid.add(0D, 0.35D, 0D);
            Vec3d wide = vantage(look, across, 4.2D, 1.7D), close = vantage(look, wide.subtract(look), 2.8D, 1.2D);
            keys.add(cut.home(0));
            keys.add(new Key(80, wide, look, Math.min(fov, 62F)));
            keys.add(new Key(230, close, a.add(0D, 0.3D, 0D), Math.min(fov, 50F)));
            keys.add(cut.home(length));
        } else if (id == VAULT_BEAT) {
            // Round in front of the player, and hold on them a long moment.
            Vec3d eye = a;
            Vec3d toward = new Vec3d(b.x - eye.x, 0D, b.z - eye.z);
            toward = toward.lengthSquared() < 1.0E-4 ? mc.player.getLookVec() : toward.normalize();
            Vec3d side = new Vec3d(-toward.z, 0D, toward.x);
            length = 130;
            keys.add(cut.home(0));
            keys.add(new Key(40, eye.add(toward.scale(2.4D)).add(side.scale(0.5D)).add(0D, 0.1D, 0D), eye, Math.min(fov, 45F)));
            keys.add(new Key(95, eye.add(toward.scale(2.0D)).add(side.scale(0.4D)).add(0D, 0.05D, 0D), eye, Math.min(fov, 40F)));
            keys.add(cut.home(length));
        } else if (id == ASCENT) {
            ascent(a, b);
            return;
        } else {
            return;
        }
        int scene = id;
        cut.play(keys, new ArrayList<>(), 6, length, () -> EternityNetwork.sendToServer(new MsgStoryAct(MsgStoryAct.SCENE_DONE, scene, 0)));
    }

    /** Warlock going, seen from your own eyes: see TitleCard. */
    private void ascent(Vec3d kneel, Vec3d her) {
        TitleCard.INSTANCE.ascent(kneel, her);
    }
}
