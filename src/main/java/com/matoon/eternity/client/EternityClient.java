package com.matoon.eternity.client;

import com.matoon.eternity.Eternity;
import com.matoon.eternity.EternityConfig;
import com.matoon.eternity.EternitySounds;
import com.matoon.eternity.client.render.Cosmic;
import com.matoon.eternity.client.render.FractureRenderer;
import com.matoon.eternity.client.render.FxRenderer;
import com.matoon.eternity.client.render.RenderEternity;
import com.matoon.eternity.client.render.RiftRenderer;
import com.matoon.eternity.client.render.ScreenFx;
import com.matoon.eternity.client.sound.LoopSound;
import com.matoon.eternity.client.sound.SoundFx;
import com.matoon.eternity.compat.InfinityStones;
import com.matoon.eternity.entity.EntityAstralRegulator;
import com.matoon.eternity.entity.EntityEternity;
import com.matoon.eternity.network.MsgCue;
import com.matoon.eternity.network.MsgFinale;
import com.matoon.eternity.network.MsgOmen;
import com.matoon.eternity.network.MsgOpenRiftGate;
import com.matoon.eternity.network.MsgPresence;
import com.matoon.eternity.network.MsgRift;
import com.matoon.eternity.network.MsgRelicTarget;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.FOVUpdateEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

/**
 * Everything the player perceives. Eternity never speaks; the world around you
 * changes instead: it dims, the wind carries things, the music stops, the sky opens.
 */
@SideOnly(Side.CLIENT)
public final class EternityClient {

    public static final EternityClient INSTANCE = new EternityClient();

    private static final SoundCategory[] WORLD_CATEGORIES = {SoundCategory.MUSIC, SoundCategory.RECORDS,
            SoundCategory.WEATHER, SoundCategory.BLOCKS, SoundCategory.NEUTRAL, SoundCategory.PLAYERS};

    private final Minecraft mc = Minecraft.getMinecraft();
    private final Random rand = new Random();

    // Where he is.
    private boolean present;
    private boolean hunted;
    private boolean space;
    private double ex, ey, ez;
    private EntityEternity nearby;

    // How it feels.
    private float intensity;
    private int heldStones;
    private float darkBoost;
    private float skyBleed;
    private float cosmicPulse;
    private float finaleCosmic;
    private float vignettePulse;
    private float flash;
    private float flashR = 1F, flashG = 1F, flashB = 1F;
    private float shake;
    private float cracks;
    private float fovPull;
    private int silenceUntil;
    private int tick;
    private int whisperTimer = 600;
    private int heraldStart = -1, heraldEnd = -1;
    private int stareEntity = -1, stareUntil = -1;
    private String activeShader;

    // The finale.
    private int finaleStage = -1;
    private int finaleTicks;
    private LoopSound drums;
    private LoopSound containLoop;
    private float containLevel;

    // The end of the world.
    private int doomStage = -1;
    private float doomLevel;
    private int doomWhisper = 200;

    private final Bed drone = new Bed(EternitySounds.AMBIENT_DRONE, 0.01F);
    private final Bed wind = new Bed(EternitySounds.AMBIENT_WIND, 0.01F);
    private final Bed abyss = new Bed(EternitySounds.AMBIENT_ABYSS, 0.01F);
    private final Bed encounter = new Bed(EternitySounds.AMBIENT_ENCOUNTER, 0.03F);

    private final Map<Integer, Rift> rifts = new HashMap<>();
    private final Map<Integer, SkyFracture> fractures = new LinkedHashMap<>();
    private int glitchUntil;
    private final List<FxRenderer.Fx> effects = new ArrayList<>();
    private final List<Timed> timeline = new ArrayList<>();
    private int localIds = -1;
    private int lastFarRiftSound = -1000;

    private static final class Timed {
        final int at;
        final Runnable task;

        Timed(int at, Runnable task) {
            this.at = at;
            this.task = task;
        }
    }

    /** A looping ambient bed that fades in and out and restarts itself if the sound engine drops it. */
    private final class Bed {
        final SoundEvent event;
        final float fade;
        LoopSound sound;
        int startedAt = -1000;

        Bed(SoundEvent event, float fade) {
            this.event = event;
            this.fade = fade;
        }

        void update(float volume) {
            SoundHandler handler = mc.getSoundHandler();
            boolean alive = sound != null && !sound.isDonePlaying() && (handler.isSoundPlaying(sound) || tick - startedAt < 40);
            if (volume > 0.004F) {
                if (!alive) {
                    if (tick - startedAt < 40) {
                        return;
                    }
                    sound = new LoopSound(event, SoundCategory.AMBIENT, true, fade);
                    handler.playSound(sound);
                    startedAt = tick;
                }
                sound.setTarget(volume);
            } else if (sound != null) {
                sound.fadeOut();
                if (!alive) {
                    sound = null;
                }
            }
        }

        void stop() {
            if (sound != null) {
                mc.getSoundHandler().stopSound(sound);
                sound = null;
            }
        }
    }

    private EternityClient() {
    }

    // ------------------------------------------------------------------ messages

    public void handle(IMessage message) {
        if (mc.world == null || mc.player == null) {
            return;
        }
        if (message instanceof MsgPresence) {
            MsgPresence m = (MsgPresence) message;
            present = m.present;
            hunted = m.hunted;
            space = m.space;
            ex = m.x;
            ey = m.y;
            ez = m.z;
        } else if (message instanceof MsgRift) {
            onRift((MsgRift) message);
        } else if (message instanceof MsgOmen) {
            onOmen((MsgOmen) message);
        } else if (message instanceof MsgCue) {
            onCue((MsgCue) message);
        } else if (message instanceof MsgFinale) {
            onFinale((MsgFinale) message);
        } else if (message instanceof MsgRelicTarget) {
            RelicTrackerAngle.receive((MsgRelicTarget) message);
        } else if (message instanceof MsgOpenRiftGate) {
            mc.displayGuiScreen(new GuiRiftGate());
        }
    }

    private void onRift(MsgRift m) {
        if ((m.flags & MsgRift.FLAG_SKY_FRACTURE) != 0) {
            if (m.action == MsgRift.OPEN) {
                onFracture(m);
            }
            return;
        }
        if (m.action == MsgRift.CLOSE) {
            Rift rift = rifts.get(m.id);
            if (rift != null) {
                rift.close();
                if (rift.loop != null) {
                    rift.loop.fadeOut();
                }
            }
            return;
        }
        if (rifts.containsKey(m.id)) {
            return;
        }
        Rift rift = new Rift(m.id, m.x, m.y, m.z, m.height, m.seed, (m.flags & MsgRift.FLAG_FINALE) != 0);
        rifts.put(m.id, rift);
        double dist = horizontalDistance(m.x, m.z);
        if (!rift.finale && dist < 150D) {
            rift.loop = new LoopSound(EternitySounds.RIFT_LOOP, SoundCategory.HOSTILE, m.x, m.y, m.z, 0.05F);
            rift.loop.setTarget(4F);
            SoundFx.play(rift.loop);
        } else if (!rift.finale || tick - lastFarRiftSound > 30) {
            farRiftSound(m.x, m.z, dist);
        }
        if (!rift.finale) {
            darkBoost = Math.max(darkBoost, 0.05F);
        }
    }

    /** The sky cracks. You hear it tear from the direction it broke, and the world shudders. */
    private void onFracture(MsgRift m) {
        if (fractures.containsKey(m.id)) {
            return;
        }
        SkyFracture fracture = new SkyFracture(m.id, new Vec3d(m.x, m.y, m.z), m.height, m.seed);
        fractures.put(m.id, fracture);
        if (fractures.size() > 14) {
            fractures.remove(fractures.keySet().iterator().next());
        }
        double angle = Math.atan2(m.z, m.x);
        SoundFx.carried(SoundEvents.BLOCK_GLASS_BREAK, 0.6F, 0.4F, angle);
        SoundFx.carried(EternitySounds.RIFT_OPEN, 0.5F, 0.45F, angle);
        later(15, () -> thunder(0.45F, angle));
        shake = Math.max(shake, 0.6F);
        flashColour(0.75F, 0.6F, 1F);
        flash = Math.max(flash, 0.12F);
        glitchUntil = tick + 18;
    }

    private void tickFractures() {
        for (SkyFracture fracture : fractures.values()) {
            fracture.tick();
            // While it is still breaking, panes keep giving way with a distant crackle.
            if (fracture.age < SkyFracture.GROW_TICKS && rand.nextInt(90) == 0 && !isSilenced()) {
                SoundFx.carried(SoundEvents.BLOCK_GLASS_BREAK, 0.25F, 0.35F + rand.nextFloat() * 0.2F,
                        Math.atan2(fracture.centre.z, fracture.centre.x));
            }
        }
    }

    /** The wounds widen as the end nears. */
    private float fractureSpread() {
        if (doomStage == MsgFinale.OBLIVION) {
            return 1.5F + 1.5F * doomLevel;
        }
        return doomStage == MsgFinale.UNMAKING ? 1F + 0.5F * doomLevel : 1F;
    }

    private void farRiftSound(double x, double z, double dist) {
        double angle = SoundFx.angleTo(x, z);
        float volume = (float) MathHelper.clamp(0.6D - 0.12D * Math.log(Math.max(1D, dist / 150D)) / Math.log(2D), 0.12D, 0.6D);
        SoundFx.carried(EternitySounds.FAR_RIFT, volume, 0.85F + rand.nextFloat() * 0.25F, angle);
        lastFarRiftSound = tick;
        if (!space && rand.nextFloat() < 0.4F) {
            later(20 + rand.nextInt(30), () -> thunder(volume * 0.7F, angle));
        }
    }

    /** The wordless reaction to taking a stone. Nothing on screen says what happened. */
    private void onOmen(MsgOmen m) {
        switch (m.level) {
            case 1:
                silence(80);
                darkBoost = Math.max(darkBoost, 0.12F);
                later(70, () -> SoundFx.carried(EternitySounds.OMEN_SHIMMER, 0.4F, 1F, SoundFx.angleBehind()));
                break;
            case 2: {
                double angle = rand.nextDouble() * Math.PI * 2D;
                thunder(0.35F, angle);
                later(60, () -> thunder(0.25F, angle + 0.4D));
                later(140, () -> SoundFx.carried(EternitySounds.FAR_HORN, 0.35F, 0.85F, angle));
                break;
            }
            case 3:
                SoundFx.carried(EternitySounds.FAR_REVERSE, 0.5F, 0.8F, SoundFx.angleBehind());
                skyBleed = Math.max(skyBleed, 0.3F);
                break;
            case 4: {
                // A rift on the horizon that only you saw.
                double angle = rand.nextDouble() * Math.PI * 2D;
                double d = 2600D;
                Rift glimpse = new Rift(localIds--, mc.player.posX + Math.cos(angle) * d, mc.player.posY + 90D,
                        mc.player.posZ + Math.sin(angle) * d, 170F, rand.nextLong(), false);
                glimpse.lifetime = 520;
                rifts.put(glimpse.id, glimpse);
                SoundFx.carried(EternitySounds.FAR_RIFT, 0.4F, 0.9F, angle);
                later(120, () -> SoundFx.carried(EternitySounds.FAR_GROAN, 0.35F, 0.8F, angle));
                break;
            }
            case 5:
                silence(220);
                later(170, () -> {
                    SoundFx.everywhere(EternitySounds.OMEN_STARE, SoundCategory.AMBIENT, 0.3F, 0.85F);
                    vignettePulse = 0.8F;
                });
                break;
            default: {
                cosmicPulse = 0.8F;
                later(8, () -> cosmicPulse = 0.6F);
                later(18, () -> cosmicPulse = 0.7F);
                SoundFx.carried(EternitySounds.FAR_NOISE, 0.45F, 0.8F, SoundFx.angleBehind());
                int delay = m.delay;
                if (delay > 0) {
                    heraldStart = tick;
                    heraldEnd = tick + delay;
                    for (int k = 1; k <= 4; k++) {
                        final int step = k;
                        later(delay * k / 5, () -> {
                            double angle = rand.nextDouble() * Math.PI * 2D;
                            thunder(0.3F + 0.1F * step, angle);
                            SoundFx.carried(step % 2 == 0 ? EternitySounds.FAR_CHAINS : EternitySounds.FAR_GROAN,
                                    0.25F + 0.1F * step, 0.8F, angle + 0.3D);
                            darkBoost += 0.04F;
                        });
                    }
                    later(Math.max(0, delay - 200), () -> silence(260));
                }
                break;
            }
        }
    }

    private void onCue(MsgCue c) {
        Vec3d at = new Vec3d(c.x, c.y, c.z);
        Vec3d other = new Vec3d(c.a, c.b, c.c);
        double dist = mc.player.getPositionEyes(1F).distanceTo(at);
        switch (c.type) {
            case MsgCue.STARE:
                if (dist < 160D) {
                    stareEntity = c.entityId;
                    stareUntil = tick + (int) c.a;
                    silence((int) c.a + 10);
                    Entity entity = mc.world.getEntityByID(c.entityId);
                    double sy = entity != null ? entity.posY + entity.height * 0.85D : c.y;
                    SoundFx.at(EternitySounds.OMEN_STARE, SoundCategory.HOSTILE, c.x, sy, c.z, 5F, 0.9F);
                    vignettePulse = 0.7F;
                }
                break;
            case MsgCue.BLINK:
                miniRift(at, 18F, 30);
                miniRift(other, 18F, 30);
                burst(at, 40, 2.5D);
                burst(other, 40, 2.5D);
                break;
            case MsgCue.PHASE:
                burst(at, 18, 0.8D);
                if (dist < 6D) {
                    cracks = Math.max(cracks, 0.2F);
                }
                break;
            case MsgCue.LANCE_WARN:
                miniRift(at, 4.5F, 44);
                effects.add(new FxRenderer.Warning(at, other, 34));
                SoundFx.at(EternitySounds.RIFT_OPEN, SoundCategory.HOSTILE, c.x, c.y, c.z, 1.5F, 1.6F);
                break;
            case MsgCue.LANCE_FIRE:
                effects.add(new FxRenderer.Beam(at, other, 1.1D));
                if (distanceToSegment(mc.player.getPositionEyes(1F), at, other) < 8D) {
                    shake = Math.max(shake, 0.8F);
                    flashColour(0.7F, 0.4F, 1F);
                    flash = Math.max(flash, 0.18F);
                }
                break;
            case MsgCue.SINGULARITY:
                effects.add(new FxRenderer.BlackHole(at, c.a));
                miniRift(at, 30F, 60);
                SoundFx.at(EternitySounds.RIFT_OPEN, SoundCategory.HOSTILE, c.x, c.y, c.z, 8F, 0.4F);
                if (dist < 120D) {
                    flashColour(0.4F, 0.1F, 0.8F);
                    flash = Math.max(flash, 0.2F);
                    cracks = Math.max(cracks, 0.3F);
                }
                break;
            case MsgCue.SINGULARITY_BURST:
                effects.add(new FxRenderer.Shockwave(at, c.a * 6D, 50, true));
                effects.add(new FxRenderer.Shockwave(at, c.a * 3D, 30, false));
                burst(at, 200, 6D);
                if (dist < c.a * 8D) {
                    float k = (float) (1D - dist / (c.a * 8D));
                    shake = Math.max(shake, 2.2F * k);
                    flashColour(1F, 0.9F, 1F);
                    flash = Math.max(flash, 0.7F * k);
                    cracks = Math.max(cracks, 0.7F * k);
                }
                if (dist < 600D) {
                    SoundFx.everywhere(EternitySounds.RIFT_IMPACT, SoundCategory.HOSTILE, (float) Math.max(0.2D, 1D - dist / 600D), 0.45F);
                    SoundFx.everywhere(SoundEvents.ENTITY_LIGHTNING_THUNDER, SoundCategory.HOSTILE, (float) Math.max(0.2D, 1D - dist / 600D), 0.5F);
                }
                break;
            case MsgCue.STAR_WARN:
                effects.add(new FxRenderer.GroundMark(at, 3.5D, 40));
                miniRift(other, 16F, 50);
                if (dist < 40D && rand.nextInt(3) == 0) {
                    SoundFx.at(EternitySounds.RIFT_OPEN, SoundCategory.HOSTILE, c.a, c.b, c.c, 4F, 1.2F);
                }
                break;
            case MsgCue.ERUPT:
                miniRift(at.add(0, 6D, 0), 14F, 30);
                effects.add(new FxRenderer.Beam(at, other, 1.6D));
                burst(at, 25, 2D);
                if (dist < 10D) {
                    shake = Math.max(shake, 1.2F);
                }
                break;
            case MsgCue.TRAVERSE:
                flashColour(0.75F, 0.55F, 1F);
                flash = Math.max(flash, 0.8F);
                shake = Math.max(shake, 1.4F);
                cracks = Math.max(cracks, 0.5F);
                SoundFx.everywhere(EternitySounds.RIFT_OPEN, SoundCategory.MASTER, 0.8F, 1.3F);
                SoundFx.everywhere(EternitySounds.FAR_REVERSE, SoundCategory.MASTER, 0.6F, 1.2F);
                break;
            case MsgCue.UNMAKE:
                burst(at, 30, 1.5D);
                if (dist < 3.5D) {
                    shake = Math.max(shake, 1.6F);
                    flashColour(0.85F, 0.7F, 1F);
                    flash = Math.max(flash, 0.35F);
                    cracks = Math.max(cracks, 0.6F);
                }
                break;
            default:
                break;
        }
    }

    private void onFinale(MsgFinale m) {
        switch (m.stage) {
            case MsgFinale.CONTAIN:
                onContain(m);
                return;
            case MsgFinale.UNMAKING:
            case MsgFinale.OBLIVION:
            case MsgFinale.VOID:
                onDoom(m);
                return;
            default:
                break;
        }
        finaleStage = m.stage;
        finaleTicks = 0;
        switch (m.stage) {
            case MsgFinale.FRACTURE:
                containLevel = 0F;
                stopWorldSounds();
                drums = new LoopSound(EternitySounds.FINALE_DRUMS, SoundCategory.MASTER, false, 0.06F);
                drums.setNow(0.9F);
                SoundFx.play(drums);
                SoundFx.everywhere(EternitySounds.ENTITY_GLITCH, SoundCategory.MASTER, 0.8F, 0.5F);
                cracks = Math.max(cracks, 0.15F);
                break;
            case MsgFinale.UNRAVEL:
                SoundFx.everywhere(EternitySounds.RIFT_OPEN, SoundCategory.MASTER, 0.7F, 0.6F);
                break;
            case MsgFinale.COLLAPSE:
                if (drums != null) {
                    drums.fadeOut();
                }
                SoundFx.everywhere(EternitySounds.FAR_REVERSE, SoundCategory.MASTER, 1F, 0.7F);
                break;
            case MsgFinale.SUPERNOVA:
                if (drums != null) {
                    mc.getSoundHandler().stopSound(drums);
                    drums = null;
                }
                flashColour(1F, 0.96F, 1F);
                flash = 1F;
                shake = 2.5F;
                cracks = 1F;
                fovPull = -0.3F;
                SoundFx.everywhere(EternitySounds.RIFT_IMPACT, SoundCategory.MASTER, 1F, 0.5F);
                SoundFx.everywhere(EternitySounds.RIFT_CLOSE, SoundCategory.MASTER, 1F, 0.4F);
                SoundFx.everywhere(SoundEvents.ENTITY_LIGHTNING_THUNDER, SoundCategory.MASTER, 1F, 0.6F);
                if (mc.world.provider.getDimension() == m.dimension) {
                    effects.add(new FxRenderer.Shockwave(new Vec3d(m.x, m.y, m.z), 900D, 120, true));
                }
                break;
            case MsgFinale.SILENCE:
                silence(100);
                break;
            case MsgFinale.SAVED:
                // The Regulator is full. The universe exhales.
                if (containLoop != null) {
                    containLoop.fadeOut();
                    containLoop = null;
                }
                if (drums != null) {
                    mc.getSoundHandler().stopSound(drums);
                    drums = null;
                }
                flashColour(1F, 0.95F, 0.8F);
                flash = 1F;
                shake = 2F;
                cracks = 0F;
                fovPull = -0.2F;
                SoundFx.everywhere(EternitySounds.RIFT_CLOSE, SoundCategory.MASTER, 1F, 0.4F);
                SoundFx.everywhere(EternitySounds.RIFT_IMPACT, SoundCategory.MASTER, 1F, 0.7F);
                SoundFx.everywhere(EternitySounds.OMEN_SHIMMER, SoundCategory.MASTER, 1F, 1.2F);
                break;
            case MsgFinale.AFTERMATH:
                LoopSound aftermath = new LoopSound(EternitySounds.FINALE_AFTERMATH, SoundCategory.MASTER, false, 0.02F);
                aftermath.setTarget(0.8F);
                SoundFx.play(aftermath);
                break;
            default:
                finaleStage = -1;
                finaleCosmic = 0F;
                doomStage = -1;
                doomLevel = 0F;
                fractures.clear();
                break;
        }
    }

    /** The Regulator was released. Everything he poured out starts flowing back. */
    private void onContain(MsgFinale m) {
        if (finaleStage != MsgFinale.CONTAIN) {
            finaleStage = MsgFinale.CONTAIN;
            finaleTicks = 0;
            if (drums != null) {
                drums.fadeOut();
            }
            SoundFx.everywhere(EternitySounds.FAR_REVERSE, SoundCategory.MASTER, 1F, 0.6F);
            containLoop = new LoopSound(EternitySounds.RIFT_LOOP, SoundCategory.MASTER, true, 0.05F).pitch(0.5F);
            containLoop.setTarget(0.9F);
            SoundFx.play(containLoop);
        }
        containLevel = Math.max(containLevel, m.level);
    }

    private void onDoom(MsgFinale m) {
        if (doomStage != m.stage) {
            if (doomStage < 0) {
                finaleStage = -1;
                finaleTicks = 0;
                if (drums != null) {
                    mc.getSoundHandler().stopSound(drums);
                    drums = null;
                }
            }
            if (m.stage == MsgFinale.OBLIVION) {
                SoundFx.everywhere(EternitySounds.FAR_REVERSE, SoundCategory.MASTER, 1F, 0.4F);
                SoundFx.everywhere(EternitySounds.RIFT_IMPACT, SoundCategory.MASTER, 1F, 0.3F);
            }
            if (m.stage == MsgFinale.VOID) {
                stopWorldSounds();
                effects.clear();
                fractures.clear();
            }
            doomStage = m.stage;
        }
        doomLevel = m.level;
    }

    // ------------------------------------------------------------------ ticking

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || mc.world == null || mc.player == null || mc.isGamePaused()) {
            return;
        }
        tick++;
        runTimeline();
        if (tick % 20 == 0) {
            heldStones = InfinityStones.countHeld(mc.player);
            nearby = findNearby();
        }
        updateIntensity();
        updateFinale();
        updateDoom();
        decay();
        updateBeds();
        updateWhispers();
        tickRifts();
        tickFractures();
        FxRenderer.tickAll(effects);
        feelBlackHoles();
        updateStare();
        updateShader();
    }

    private void updateIntensity() {
        float target = 0F;
        if (present) {
            double dist = horizontalDistance(ex, ez);
            float proximity = 1F - smoothstep(150F, 3500F, (float) dist);
            target = (0.18F + 0.82F * proximity) * (1F + 0.1F * heldStones);
            if (hunted) {
                target = Math.max(target, 0.75F);
            }
        }
        if (heraldEnd > tick) {
            float p = (tick - heraldStart) / (float) Math.max(1, heraldEnd - heraldStart);
            target = Math.max(target, 0.3F + 0.6F * p);
        }
        if (finaleStage >= MsgFinale.FRACTURE && finaleStage <= MsgFinale.COLLAPSE) {
            target = 1F;
        }
        target = Math.min(1.2F, target) * (float) EternityConfig.client.presenceStrength;
        intensity += (target - intensity) * 0.008F;
    }

    private void decay() {
        darkBoost *= 0.993F;
        skyBleed *= 0.993F;
        cosmicPulse *= 0.85F;
        vignettePulse *= 0.97F;
        if (finaleStage < 0) {
            shake *= 0.9F;
            flash *= 0.9F;
            cracks *= 0.96F;
        }
        float fovTarget = 0F;
        if (stareUntil > tick) {
            fovTarget = 0.12F;
        }
        if (finaleStage == MsgFinale.COLLAPSE) {
            fovTarget = 0.45F * Math.min(1F, finaleTicks / 60F);
        }
        if (finaleStage == MsgFinale.CONTAIN) {
            fovTarget = 0.1F;
        }
        if (doomStage == MsgFinale.UNMAKING) {
            fovTarget += MathHelper.sin(tick * 0.03F) * 0.1F * doomLevel;
        }
        fovPull += (fovTarget - fovPull) * 0.12F;
    }

    private void updateFinale() {
        if (finaleStage < 0) {
            return;
        }
        finaleTicks++;
        float p;
        switch (finaleStage) {
            case MsgFinale.FRACTURE:
                p = Math.min(1F, finaleTicks / 200F);
                shake = 0.12F + 0.4F * p;
                cracks = Math.max(cracks, 0.35F * p);
                finaleCosmic = 0.25F * p;
                darkBoost = Math.max(darkBoost, 0.1F * p);
                if (rand.nextInt(25) == 0) {
                    SoundFx.everywhere(EternitySounds.ENTITY_GLITCH, SoundCategory.MASTER, 0.5F + 0.4F * p, 0.5F + rand.nextFloat() * 0.5F);
                }
                break;
            case MsgFinale.UNRAVEL:
                p = Math.min(1F, finaleTicks / 300F);
                shake = 0.55F + 0.45F * p;
                cracks = 0.35F + 0.55F * p;
                finaleCosmic = 0.25F + 0.7F * p;
                if (rand.nextInt(30) == 0) {
                    SoundEvent[] cries = {EternitySounds.FAR_HORN, EternitySounds.FAR_NOISE, EternitySounds.FAR_GROAN, EternitySounds.FAR_CHAINS};
                    SoundFx.carried(cries[rand.nextInt(cries.length)], 0.5F + 0.3F * p, 0.6F + rand.nextFloat() * 0.3F, rand.nextDouble() * Math.PI * 2D);
                }
                if (rand.nextInt(60) == 0) {
                    flashColour(0.8F, 0.6F, 1F);
                    flash = Math.max(flash, 0.12F);
                }
                flash *= 0.85F;
                break;
            case MsgFinale.COLLAPSE:
                p = Math.min(1F, finaleTicks / 60F);
                shake = 1F + 1.2F * p;
                finaleCosmic = 1F;
                cracks = 0.9F + 0.1F * p;
                darkBoost = Math.max(darkBoost, 0.35F * p);
                break;
            case MsgFinale.SUPERNOVA:
                p = Math.min(1F, finaleTicks / 100F);
                flash = (1F - p) * (1F - p);
                shake = 2.5F * (1F - p) * (1F - p);
                cracks = 1F - p;
                darkBoost = 0F;
                finaleCosmic = 1F;
                break;
            case MsgFinale.SILENCE:
                p = Math.min(1F, finaleTicks / 100F);
                finaleCosmic = 1F - 0.3F * p;
                shake = 0F;
                cracks *= 0.9F;
                break;
            case MsgFinale.CONTAIN: {
                EntityAstralRegulator orb = findRegulator();
                if (orb != null) {
                    if (!hasAbsorption()) {
                        effects.add(new FxRenderer.Absorption(orb.getEntityId(), rifts::values));
                    }
                    if (orb.isAbsorbing()) {
                        containLevel = Math.max(containLevel, orb.getProgress());
                    }
                }
                p = containLevel;
                shake = 0.5F + 0.9F * p;
                cracks = 0.9F * (1F - p);
                finaleCosmic = 1F - 0.6F * p;
                darkBoost = Math.max(darkBoost, 0.15F * (1F - p));
                if (rand.nextInt(14) == 0) {
                    flashColour(0.85F, 0.9F, 1F);
                    flash = Math.max(flash, 0.15F + 0.25F * p);
                }
                flash *= 0.85F;
                if (containLoop != null) {
                    containLoop.pitch(0.5F + 1.5F * p);
                }
                break;
            }
            case MsgFinale.SAVED:
                p = Math.min(1F, finaleTicks / 200F);
                flash *= 0.94F;
                shake *= 0.93F;
                cracks = 0F;
                finaleCosmic = 0.4F * (1F - p);
                if (rand.nextInt(3) == 0) {
                    mc.world.spawnParticle(EnumParticleTypes.END_ROD,
                            mc.player.posX + rand.nextGaussian() * 16D, mc.player.posY + 12D + rand.nextDouble() * 8D,
                            mc.player.posZ + rand.nextGaussian() * 16D, 0D, -0.04D, 0D);
                }
                break;
            case MsgFinale.AFTERMATH:
                p = Math.min(1F, finaleTicks / 500F);
                finaleCosmic = 0.7F * (1F - p);
                if (finaleTicks < 450 && rand.nextBoolean()) {
                    mc.world.spawnParticle(EnumParticleTypes.END_ROD,
                            mc.player.posX + rand.nextGaussian() * 20D, mc.player.posY + 15D + rand.nextDouble() * 10D,
                            mc.player.posZ + rand.nextGaussian() * 20D, 0D, -0.06D, 0D);
                }
                break;
            default:
                break;
        }
    }

    /**
     * Reality coming apart. The sky is open space, the horizon tilts, colours bleed,
     * light flickers wrong, silences fall without warning and things scream on the
     * wind from every direction. Then everything goes dark.
     */
    private void updateDoom() {
        if (doomStage < 0) {
            return;
        }
        float p = doomLevel;
        switch (doomStage) {
            case MsgFinale.UNMAKING:
                finaleCosmic = 0.35F + 0.6F * p;
                darkBoost = Math.max(darkBoost, 0.08F + 0.3F * p);
                cracks = Math.max(cracks * 0.98F, (0.12F + 0.5F * p) * (0.6F + 0.4F * MathHelper.sin(tick * 0.05F)));
                if (rand.nextFloat() < 0.01F + 0.02F * p) {
                    shake = Math.max(shake, 0.5F + 1.5F * p);
                }
                if (rand.nextFloat() < 0.004F + 0.02F * p) {
                    flashColour(0.75F, 0.6F, 1F);
                    flash = Math.max(flash, 0.25F + 0.4F * p);
                }
                if (rand.nextFloat() < 0.0008F + 0.003F * p) {
                    silence(60 + rand.nextInt(200));
                }
                if (--doomWhisper <= 0 && !isSilenced()) {
                    doomWhisper = (int) ((60 + rand.nextInt(220)) * (1.25F - p));
                    doomCry(p);
                }
                break;
            case MsgFinale.OBLIVION:
                finaleCosmic = 1F;
                darkBoost = 0.3F + 0.7F * p;
                vignettePulse = 1F;
                shake = 0.4F + 1.2F * p;
                cracks = 1F;
                if (p > 0.9F) {
                    silenceUntil = tick + 40;
                } else if (--doomWhisper <= 0) {
                    doomWhisper = 20 + rand.nextInt(40);
                    doomCry(1F);
                }
                break;
            case MsgFinale.VOID:
                finaleCosmic = 0F;
                shake = 0F;
                cracks = 0F;
                flash = 0F;
                darkBoost = 0F;
                silenceUntil = tick + 40;
                break;
            default:
                break;
        }
    }

    private void doomCry(float p) {
        double angle = rand.nextDouble() * Math.PI * 2D;
        if (rand.nextFloat() < 0.3F) {
            thunder(0.3F + 0.4F * p, angle);
            return;
        }
        SoundEvent[] cries = {EternitySounds.FAR_HORN, EternitySounds.FAR_NOISE, EternitySounds.FAR_GROAN,
                EternitySounds.FAR_CHAINS, EternitySounds.FAR_REVERSE, EternitySounds.ENTITY_GLITCH};
        SoundFx.carried(cries[rand.nextInt(cries.length)], 0.25F + 0.5F * p, 0.5F + rand.nextFloat() * 0.6F, angle);
    }

    private void updateBeds() {
        boolean finale = finaleStage >= 0;
        boolean silenced = isSilenced();
        boolean sky = mc.world.provider.hasSkyLight() && !space;
        float droneVolume = present && !finale ? 0.05F + 0.33F * intensity : 0F;
        float windVolume = sky && present && !finale ? 0.32F * Math.max(0F, intensity - 0.12F) : 0F;
        float abyssVolume = finale ? 0F : space && present ? 0.15F + 0.5F * intensity : Math.max(0F, intensity - 0.55F) * 0.9F;
        float encounterVolume = hunted && nearby != null && !finale ? FightMusic.INSTANCE.playing() ? 0.35F : 0.8F : 0F;
        if (doomStage == MsgFinale.UNMAKING) {
            droneVolume = 0.3F + 0.4F * doomLevel;
            abyssVolume = 0.2F + 0.6F * doomLevel;
            windVolume = sky ? 0.3F * doomLevel : 0F;
        } else if (doomStage == MsgFinale.OBLIVION) {
            droneVolume = 0.7F * (1F - doomLevel);
            abyssVolume = 0.9F * (1F - doomLevel);
            windVolume = 0F;
        } else if (doomStage == MsgFinale.VOID) {
            droneVolume = 0F;
            windVolume = 0F;
            abyssVolume = 0.15F;
        }
        if (silenced) {
            droneVolume *= 0.2F;
            windVolume = 0F;
            abyssVolume *= 0.3F;
        }
        drone.update(droneVolume);
        wind.update(windVolume);
        abyss.update(abyssVolume);
        encounter.update(encounterVolume);
    }

    /** Things heard on the wind, more often and nearer the closer he is, from the direction he is in. */
    private void updateWhispers() {
        if (!present || finaleStage >= 0 || isSilenced()) {
            return;
        }
        if (--whisperTimer > 0) {
            return;
        }
        whisperTimer = (int) ((420 + rand.nextInt(1200)) * (1.4F - Math.min(1F, intensity)));
        double angle = rand.nextFloat() < 0.6F
                ? SoundFx.angleTo(ex, ez) + rand.nextGaussian() * 0.35D
                : rand.nextDouble() * Math.PI * 2D;
        float volume = (0.16F + 0.42F * Math.min(1F, intensity)) * (space ? 0.7F : 1F);
        float pitch = 0.78F + rand.nextFloat() * 0.27F;

        float thunder = space ? 0F : 3F;
        float groan = 2F;
        float chains = 1.5F;
        float horn = 0.8F + intensity;
        float reverse = 1F;
        float noise = intensity > 0.45F ? 1.2F : 0F;
        float roll = rand.nextFloat() * (thunder + groan + chains + horn + reverse + noise);
        if ((roll -= thunder) < 0) {
            thunder(volume * 0.9F, angle);
        } else if ((roll -= groan) < 0) {
            SoundFx.carried(EternitySounds.FAR_GROAN, volume, pitch, angle);
        } else if ((roll -= chains) < 0) {
            SoundFx.carried(EternitySounds.FAR_CHAINS, volume * 0.8F, pitch, angle);
        } else if ((roll -= horn) < 0) {
            SoundFx.carried(EternitySounds.FAR_HORN, volume * 0.85F, pitch * 0.95F, angle);
        } else if ((roll -= reverse) < 0) {
            SoundFx.carried(EternitySounds.FAR_REVERSE, volume, pitch, angle);
        } else {
            SoundFx.carried(EternitySounds.FAR_NOISE, volume * 0.9F, pitch, angle);
        }
    }

    private void thunder(float volume, double angle) {
        if (space) {
            SoundFx.carried(EternitySounds.RIFT_IMPACT, volume * 0.5F, 0.45F, angle);
        } else {
            SoundFx.carried(SoundEvents.ENTITY_LIGHTNING_THUNDER, volume, 0.5F + rand.nextFloat() * 0.2F, angle);
        }
    }

    private void tickRifts() {
        Iterator<Rift> it = rifts.values().iterator();
        while (it.hasNext()) {
            Rift rift = it.next();
            rift.tick();
            if (rift.closeAge == 0 && rift.loop != null) {
                rift.loop.fadeOut();
            }
            if (rift.isDead()) {
                if (rift.loop != null) {
                    rift.loop.fadeOut();
                }
                it.remove();
            }
        }
    }

    /** While he stares, your eyes are drawn to his. */
    private void updateStare() {
        if (stareUntil <= tick || !EternityConfig.client.forcedGaze) {
            return;
        }
        Entity entity = mc.world.getEntityByID(stareEntity);
        EntityPlayer player = mc.player;
        if (entity == null || entity.getDistance(player) > 160F) {
            return;
        }
        double dx = entity.posX - player.posX;
        double dz = entity.posZ - player.posZ;
        double dy = (entity.posY + entity.height * 0.88D) - (player.posY + player.getEyeHeight());
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90F;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        player.rotationYaw += MathHelper.wrapDegrees(yaw - player.rotationYaw) * 0.035F;
        player.rotationPitch += (pitch - player.rotationPitch) * 0.035F;
    }

    private void updateShader() {
        String want = null;
        if (EternityConfig.client.distortionShaders && OpenGlHelper.shadersSupported && OpenGlHelper.isFramebufferEnabled()) {
            if (stareUntil > tick || finaleStage >= MsgFinale.FRACTURE && finaleStage <= MsgFinale.COLLAPSE) {
                want = "deconverge";
            } else if (finaleStage == MsgFinale.SILENCE || finaleStage == MsgFinale.SUPERNOVA && finaleTicks > 20
                    || finaleStage == MsgFinale.AFTERMATH && finaleTicks < 200) {
                want = "desaturate";
            } else if (finaleStage == MsgFinale.CONTAIN) {
                want = "deconverge";
            } else if (doomStage == MsgFinale.UNMAKING) {
                want = pickDoomShader();
            } else if (doomStage == MsgFinale.OBLIVION) {
                want = glitchUntil > tick ? "deconverge" : "desaturate";
            } else if (InBetweenerClient.INSTANCE.shader() != null) {
                // Inside the In-Betweener's bubble: the world turned inside out, then drained while time stands still.
                want = InBetweenerClient.INSTANCE.shader();
            } else if (NexusClient.INSTANCE.sinking() > 0F) {
                // Going under in the Nexus's abyss: the image splits, then the colour drains out of it.
                want = NexusClient.INSTANCE.sinking() > 0.65F ? "desaturate" : "deconverge";
            }
        }
        if (Objects.equals(want, activeShader)) {
            return;
        }
        if (want == null) {
            mc.entityRenderer.stopUseShader();
            activeShader = null;
        } else if (activeShader != null || !mc.entityRenderer.isShaderActive()) {
            mc.entityRenderer.loadShader(new ResourceLocation("shaders/post/" + want + ".json"));
            activeShader = want;
        }
    }

    /**
     * Restrained: a brief split of the image whenever the sky cracks, and colour slowly
     * draining out of the world as the end nears. Nothing more.
     */
    private String pickDoomShader() {
        if (glitchUntil > tick) {
            return "deconverge";
        }
        return doomLevel > 0.55F ? "desaturate" : null;
    }

    // ------------------------------------------------------------------ rendering

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Entity view = mc.getRenderViewEntity();
        if (mc.world == null || view == null) {
            return;
        }
        float pt = event.getPartialTicks();
        Vec3d camera = new Vec3d(
                view.lastTickPosX + (view.posX - view.lastTickPosX) * pt,
                view.lastTickPosY + (view.posY - view.lastTickPosY) * pt,
                view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * pt);
        GlStateManager.disableFog();

        float sky = finaleCosmic + cosmicPulse + skyBleed + Math.max(0F, intensity - 0.8F) * 0.25F;
        if (sky > 0.01F) {
            float amount = Math.min(1F, sky);
            double radius = RiftRenderer.farPlane() * 1.25D;
            GlStateManager.disableCull();
            Cosmic.draw(() -> FxRenderer.skySphere(radius), 12, amount, amount * 1.3F, 1F, 0.8F, 1F, false);
            GlStateManager.enableCull();
        }
        FractureRenderer.renderAll(fractures.values(), pt, fractureSpread(), tick + pt);
        RiftRenderer.renderAll(rifts.values(), camera, pt);
        FxRenderer.renderAll(effects, camera, pt);
        renderFarColossus(camera, pt);
    }

    /** In space you can see him from a very long way off, drifting through the void. */
    private void renderFarColossus(Vec3d camera, float pt) {
        if (!present || !space || nearby != null) {
            return;
        }
        double dx = ex - camera.x;
        double dy = ey - camera.y;
        double dz = ez - camera.z;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist > 9000D) {
            return;
        }
        float scale = (float) EternityConfig.roaming.spaceScale;
        double[] p = RiftRenderer.project(dx, dy, dz, EntityEternity.BASE_HEIGHT * scale);
        float yaw = (float) Math.toDegrees(Math.atan2(-dz, -dx)) - 90F + MathHelper.sin((tick + pt) * 0.002F) * 40F;
        RenderEternity.drawFigure(p[0], p[1], p[2], yaw, 0F, 10F, (float) (scale * p[3]), 1F, tick + pt,
                EntityEternity.State.DRIFT, 0, dist * dist, false);
    }

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Pre event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL || mc.world == null) {
            return;
        }
        ScaledResolution screen = event.getResolution();
        float now = tick + event.getPartialTicks();
        if (doomStage == MsgFinale.VOID) {
            ScreenFx.renderVoid(screen.getScaledWidth_double(), screen.getScaledHeight_double(), now, 1F);
            return;
        }
        float strength = (float) EternityConfig.client.presenceStrength;
        float darkness = intensity * 0.15F * strength + darkBoost;
        float vignette = intensity * 0.5F * strength + vignettePulse + (stareUntil > tick ? 0.5F : 0F);
        float shownFlash = EternityConfig.client.reduceFlashing ? Math.min(flash, 0.3F) : flash;
        if (darkness < 0.005F && vignette < 0.01F && cracks < 0.01F && shownFlash < 0.005F) {
            return;
        }
        ScreenFx.render(screen.getScaledWidth_double(), screen.getScaledHeight_double(), darkness, vignette, cracks,
                shownFlash, flashR, flashG, flashB, now);
        if (doomStage == MsgFinale.OBLIVION && doomLevel > 0.75F) {
            // The last of the light goes out.
            ScreenFx.renderVoid(screen.getScaledWidth_double(), screen.getScaledHeight_double(), now, (doomLevel - 0.75F) * 4F);
        }
    }

    @SubscribeEvent
    public void onFogColour(EntityViewRenderEvent.FogColors event) {
        float k = Math.min(1F, intensity * 0.4F * (float) EternityConfig.client.presenceStrength + finaleCosmic * 0.6F);
        if (k > 0.001F) {
            event.setRed(event.getRed() * (1F - k) + 0.02F * k);
            event.setGreen(event.getGreen() * (1F - k));
            event.setBlue(event.getBlue() * (1F - k) + 0.05F * k);
        }
        if (doomStage == MsgFinale.UNMAKING || doomStage == MsgFinale.OBLIVION) {
            // A bruised dusk: rust-brown air under a violet wound.
            float d = doomStage == MsgFinale.OBLIVION ? 1F : doomLevel;
            float mix = 0.55F * d;
            event.setRed(event.getRed() * (1F - mix) + mix * 0.3F);
            event.setGreen(event.getGreen() * (1F - mix) + mix * 0.13F);
            event.setBlue(event.getBlue() * (1F - mix) + mix * 0.2F);
        }
    }

    @SubscribeEvent
    public void onFog(EntityViewRenderEvent.RenderFogEvent event) {
        if (mc.player == null || mc.player.isPotionActive(MobEffects.BLINDNESS) || event.getState().getMaterial() != Material.AIR) {
            return;
        }
        float far = event.getFarPlaneDistance();
        if (doomStage == MsgFinale.OBLIVION || doomStage == MsgFinale.VOID) {
            // The world dissolves into fog, nearer and nearer.
            float o = doomStage == MsgFinale.VOID ? 1F : doomLevel;
            GlStateManager.setFogStart(far * 0.4F * (1F - o));
            GlStateManager.setFogEnd(Math.max(2F, far * (1F - 0.95F * o)));
            return;
        }
        float k = Math.min(0.7F, intensity * 0.45F * (float) EternityConfig.client.presenceStrength + finaleCosmic * 0.3F
                + (doomStage == MsgFinale.UNMAKING ? 0.3F * doomLevel : 0F));
        if (k < 0.01F) {
            return;
        }
        GlStateManager.setFogStart(far * (0.75F - 0.6F * k));
        GlStateManager.setFogEnd(far * (1F - 0.45F * k));
    }

    @SubscribeEvent
    public void onCamera(EntityViewRenderEvent.CameraSetup event) {
        if (!EternityConfig.client.screenShake) {
            return;
        }
        float amp = shake;
        float t = tick + (float) event.getRenderPartialTicks();
        // As reality unmakes itself, the horizon will not stay level.
        float tilt = 0F;
        if (doomStage == MsgFinale.UNMAKING) {
            tilt = (MathHelper.sin(t * 0.004F) * 7F + MathHelper.sin(t * 0.017F) * 2F) * doomLevel;
        } else if (doomStage == MsgFinale.OBLIVION) {
            tilt = 9F * MathHelper.sin(t * 0.01F) * doomLevel;
        }
        if (amp < 0.001F && tilt == 0F) {
            return;
        }
        event.setYaw(event.getYaw() + (MathHelper.sin(t * 2.3F) + MathHelper.sin(t * 5.1F) * 0.5F) * amp * 0.8F);
        event.setPitch(event.getPitch() + (MathHelper.sin(t * 3.7F) + MathHelper.sin(t * 1.3F) * 0.5F) * amp * 0.8F);
        event.setRoll(event.getRoll() + MathHelper.sin(t * 1.1F) * amp * 1.6F + tilt);
    }

    @SubscribeEvent
    public void onFov(FOVUpdateEvent event) {
        if (Math.abs(fovPull) > 0.001F) {
            event.setNewfov(event.getNewfov() * (1F - fovPull));
        }
    }

    /** During silences the world's own sounds simply do not happen, and no music but his own plays near him. */
    @SubscribeEvent
    public void onPlaySound(PlaySoundEvent event) {
        ISound sound = event.getSound();
        if (sound == null || FightMusic.isFightMusic(sound)) {
            return;
        }
        boolean music = sound.getCategory() == SoundCategory.MUSIC;
        // Our own sounds are part of him, except music: even the Nexus's music falls silent near him.
        if (Eternity.MODID.equals(sound.getSoundLocation().getNamespace()) && !music) {
            return;
        }
        if (isSilenced()) {
            event.setResultSound(null);
        } else if (music && (intensity > 0.3F || finaleStage >= 0 || doomStage >= 0 || hunted)) {
            event.setResultSound(null);
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.getWorld().isRemote) {
            clearWorld();
        }
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        mc.addScheduledTask(this::resetAll);
    }

    // ------------------------------------------------------------------ helpers

    private void clearWorld() {
        for (Rift rift : rifts.values()) {
            if (rift.loop != null) {
                mc.getSoundHandler().stopSound(rift.loop);
            }
        }
        rifts.clear();
        effects.clear();
        fractures.clear();
        present = false;
        hunted = false;
        nearby = null;
        stareUntil = -1;
    }

    private void resetAll() {
        clearWorld();
        timeline.clear();
        drone.stop();
        wind.stop();
        abyss.stop();
        encounter.stop();
        drums = null;
        if (containLoop != null) {
            mc.getSoundHandler().stopSound(containLoop);
            containLoop = null;
        }
        containLevel = 0F;
        doomStage = -1;
        doomLevel = 0F;
        finaleStage = -1;
        finaleCosmic = 0F;
        intensity = darkBoost = skyBleed = cosmicPulse = vignettePulse = flash = shake = cracks = fovPull = 0F;
        heraldStart = heraldEnd = -1;
        silenceUntil = 0;
        if (activeShader != null) {
            mc.entityRenderer.stopUseShader();
            activeShader = null;
        }
    }

    private boolean isSilenced() {
        return silenceUntil > tick || finaleStage == MsgFinale.SILENCE || doomStage == MsgFinale.VOID;
    }

    /** His body, if he is near and hunting you and the world is not already ending. */
    public EntityEternity foe() {
        return hunted && nearby != null && !nearby.isDying() && finaleStage < 0 && doomStage < 0 ? nearby : null;
    }

    /** Whether the world has ended and only the void remains. */
    public boolean inVoid() {
        return doomStage == MsgFinale.VOID;
    }

    private void silence(int ticks) {
        silenceUntil = Math.max(silenceUntil, tick + ticks);
        stopWorldSounds();
    }

    private void stopWorldSounds() {
        SoundHandler handler = mc.getSoundHandler();
        for (SoundCategory category : WORLD_CATEGORIES) {
            handler.stop("", category);
        }
    }

    private void later(int delay, Runnable task) {
        timeline.add(new Timed(tick + delay, task));
    }

    private void runTimeline() {
        if (timeline.isEmpty()) {
            return;
        }
        List<Timed> due = new ArrayList<>();
        Iterator<Timed> it = timeline.iterator();
        while (it.hasNext()) {
            Timed timed = it.next();
            if (timed.at <= tick) {
                due.add(timed);
                it.remove();
            }
        }
        for (Timed timed : due) {
            timed.task.run();
        }
    }

    /** The ground shakes and the edges of sight bend near a black hole. */
    private void feelBlackHoles() {
        Vec3d eyes = mc.player.getPositionEyes(1F);
        for (FxRenderer.Fx fx : effects) {
            if (fx instanceof FxRenderer.BlackHole) {
                FxRenderer.BlackHole hole = (FxRenderer.BlackHole) fx;
                double r = hole.radius(0F);
                double d = eyes.distanceTo(hole.centre);
                double reach = r * 5D + 20D;
                if (d < reach) {
                    float k = (float) (1D - d / reach);
                    shake = Math.max(shake, 1.3F * k);
                    vignettePulse = Math.max(vignettePulse, 0.6F * k);
                    darkBoost = Math.max(darkBoost, 0.2F * k);
                }
            }
        }
    }

    private void miniRift(Vec3d at, float height, int lifetime) {
        Rift rift = new Rift(localIds--, at.x, at.y, at.z, height, rand.nextLong(), false);
        rift.lifetime = lifetime;
        rifts.put(rift.id, rift);
    }

    private void burst(Vec3d at, int count, double speed) {
        for (int i = 0; i < count; i++) {
            mc.world.spawnParticle(EnumParticleTypes.PORTAL, at.x, at.y, at.z,
                    rand.nextGaussian() * speed, rand.nextGaussian() * speed, rand.nextGaussian() * speed);
            if (i % 3 == 0) {
                mc.world.spawnParticle(EnumParticleTypes.END_ROD, at.x, at.y, at.z,
                        rand.nextGaussian() * speed * 0.1D, rand.nextGaussian() * speed * 0.1D, rand.nextGaussian() * speed * 0.1D);
            }
        }
    }

    private void flashColour(float r, float g, float b) {
        flashR = r;
        flashG = g;
        flashB = b;
    }

    private EntityAstralRegulator findRegulator() {
        for (Entity entity : mc.world.loadedEntityList) {
            if (entity instanceof EntityAstralRegulator) {
                return (EntityAstralRegulator) entity;
            }
        }
        return null;
    }

    private boolean hasAbsorption() {
        for (FxRenderer.Fx fx : effects) {
            if (fx instanceof FxRenderer.Absorption) {
                return true;
            }
        }
        return false;
    }

    private EntityEternity findNearby() {
        for (Entity entity : mc.world.loadedEntityList) {
            if (entity instanceof EntityEternity && entity.getDistance(mc.player) < 160F) {
                return (EntityEternity) entity;
            }
        }
        return null;
    }

    private double horizontalDistance(double x, double z) {
        double dx = x - mc.player.posX;
        double dz = z - mc.player.posZ;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static double distanceToSegment(Vec3d p, Vec3d a, Vec3d b) {
        Vec3d ab = b.subtract(a);
        double len = ab.lengthSquared();
        double t = len < 1.0E-6 ? 0D : MathHelper.clamp(p.subtract(a).dotProduct(ab) / len, 0D, 1D);
        return p.distanceTo(a.add(ab.scale(t)));
    }

    private static float smoothstep(float edge0, float edge1, float x) {
        float t = MathHelper.clamp((x - edge0) / (edge1 - edge0), 0F, 1F);
        return t * t * (3F - 2F * t);
    }
}
