package com.matoon.eternity.client;

import com.matoon.eternity.EternitySounds;
import com.matoon.eternity.client.sound.LoopSound;
import com.matoon.eternity.entity.EntityChampion;
import com.matoon.eternity.entity.EntityEternity;
import com.matoon.eternity.entity.EntityLivingTribunal;
import com.matoon.eternity.entity.EntityOrderChaos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import paulscode.sound.SoundSystem;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * The music of the great fights, and of the void left after the world ends. One track at a
 * time, crossfading as a fight turns; the silences that hush everything else around these
 * beings do not hush it.
 *
 * It also keeps the world's own music (Minecraft's, the Nexus's, any other mod's) out of the
 * way. Whatever is already playing when a fight begins, or when he comes close, fades out
 * rather than playing on underneath; nothing new starts while a fight track is playing or
 * fading; and the world stays quiet for a while after, so a calm overworld piece never
 * starts up over the last bars of a boss theme.
 */
@SideOnly(Side.CLIENT)
public final class FightMusic {

    public static final FightMusic INSTANCE = new FightMusic();

    /** How long the world's music stays quiet after a fight or a silence ends. */
    private static final int QUIET_AFTER = 900;
    /** How long the world's music takes to fade away when it is told to make way. */
    private static final int FADE_TICKS = 50;

    private final Minecraft mc = Minecraft.getMinecraft();
    private Track track;
    private int startedAt = -1000;
    private int tick;
    private int quietUntil = -1;
    /** The world's music that was let through and may still be playing. */
    private final List<Held> heard = new ArrayList<>();
    private boolean fadeBroken;

    /** A fight track, so the handlers that silence music can tell it apart. */
    private static final class Track extends LoopSound {
        final SoundEvent event;

        Track(SoundEvent event) {
            super(event, SoundCategory.MUSIC, true, 0.02F);
            this.event = event;
        }
    }

    /** A piece of the world's music, and how far it has faded. */
    private static final class Held {
        final ISound sound;
        final int since;
        float gain = 1F;

        Held(ISound sound, int since) {
            this.sound = sound;
            this.since = since;
        }
    }

    private FightMusic() {
    }

    public static boolean isFightMusic(ISound sound) {
        return sound instanceof Track;
    }

    public boolean playing() {
        return track != null && !track.isStopping();
    }

    /** Whether the world's music must keep quiet right now: a fight track is up or still fading, or he is close. */
    private boolean holding() {
        return track != null || EternityClient.INSTANCE.holdsMusic() || NexusClient.INSTANCE.holdsMusic();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || mc.isGamePaused()) {
            return;
        }
        tick++;
        if (mc.world == null || mc.player == null) {
            stop();
            return;
        }
        SoundEvent want = null;
        float volume = 1F;

        EntityEternity eternity = EternityClient.INSTANCE.foe();
        EntityLivingTribunal tribunal = findTribunal();
        if (EternityClient.INSTANCE.inVoid()) {
            want = EternitySounds.MUSIC_VOID;
            volume = 0.7F;
        } else if (eternity != null) {
            want = eternity.getHealth() < eternity.getMaxHealth() * 0.5F ? EternitySounds.MUSIC_ETERNITY_WOUNDED : EternitySounds.MUSIC_ETERNITY;
        } else if (tribunal != null && tribunal.getState() != EntityLivingTribunal.State.ABSORBING) {
            want = tribunal.getAscension() >= 2 ? EternitySounds.MUSIC_TRIBUNAL_ASCENDED : EternitySounds.MUSIC_TRIBUNAL;
            // Beaten, he kneels: the music draws back for the moment the Regulator can take him.
            volume = tribunal.isDown() ? 0.35F : 1F;
        } else if (tribunal == null && TribunalClient.INSTANCE.onTrial()) {
            want = EternitySounds.MUSIC_TRIBUNAL_TRIAL;
            volume = 0.6F;
        } else if (mastersFighting()) {
            want = EternitySounds.MUSIC_MASTERS;
        } else if (championFighting()) {
            want = EternitySounds.MUSIC_CHAMPION;
        } else if (VaultClient.INSTANCE.destroyerFighting()) {
            want = EternitySounds.MUSIC_DESTROYER;
        }
        play(want, volume);
        quiet();
    }

    private void play(SoundEvent want, float volume) {
        SoundHandler handler = mc.getSoundHandler();
        if (want == null) {
            if (track != null) {
                track.fadeOut();
                if (track.isDonePlaying() || !handler.isSoundPlaying(track)) {
                    track = null;
                }
            }
            return;
        }
        // Streams take a moment to start; after that, a track the engine dropped is started again.
        boolean alive = track != null && !track.isDonePlaying() && (handler.isSoundPlaying(track) || tick - startedAt < 40);
        if (track != null && (track.event != want || !alive)) {
            track.fadeOut();
            track = null;
        }
        if (track == null) {
            track = new Track(want);
            handler.playSound(track);
            startedAt = tick;
        }
        track.setTarget(volume);
    }

    /** Fades out whatever of the world's music is playing while it must keep quiet, and keeps it quiet a while after. */
    private void quiet() {
        if (holding()) {
            quietUntil = tick + QUIET_AFTER;
        }
        SoundHandler handler = mc.getSoundHandler();
        boolean hush = tick < quietUntil;
        for (Iterator<Held> it = heard.iterator(); it.hasNext(); ) {
            Held held = it.next();
            // A stream can take a moment to report itself playing.
            if (tick - held.since > 40 && !handler.isSoundPlaying(held.sound)) {
                it.remove();
            } else if (hush) {
                held.gain -= 1F / FADE_TICKS;
                if (held.gain <= 0F || !setVolume(held)) {
                    handler.stopSound(held.sound);
                    it.remove();
                }
            }
        }
    }

    /**
     * Turns a playing sound down to its faded level. Vanilla music cannot fade by itself, so
     * this reaches into the sound engine; if that ever fails, the music is simply stopped.
     */
    private boolean setVolume(Held held) {
        if (fadeBroken) {
            return false;
        }
        try {
            SoundManager manager = ObfuscationReflectionHelper.getPrivateValue(SoundHandler.class, mc.getSoundHandler(), "sndManager", "field_147694_f");
            Map<ISound, String> playing = ObfuscationReflectionHelper.getPrivateValue(SoundManager.class, manager, "invPlayingSounds", "field_148630_i");
            SoundSystem system = ObfuscationReflectionHelper.getPrivateValue(SoundManager.class, manager, "sndSystem", "field_148620_e");
            String id = playing.get(held.sound);
            if (id == null || system == null) {
                return false;
            }
            float level = MathHelper.clamp(held.sound.getVolume() * mc.gameSettings.getSoundLevel(held.sound.getCategory()), 0F, 1F);
            system.setVolume(id, level * held.gain * held.gain);
            return true;
        } catch (RuntimeException e) {
            fadeBroken = true;
            return false;
        }
    }

    private EntityLivingTribunal findTribunal() {
        if (!TribunalClient.INSTANCE.inChamber()) {
            return null;
        }
        for (Entity entity : mc.world.loadedEntityList) {
            if (entity instanceof EntityLivingTribunal) {
                return (EntityLivingTribunal) entity;
            }
        }
        return null;
    }

    /** Order or Chaos is fighting, rather than arriving, talking or leaving. */
    private boolean mastersFighting() {
        for (Entity entity : mc.world.loadedEntityList) {
            if (entity instanceof EntityOrderChaos && !((EntityOrderChaos) entity).isMask() && entity.getDistance(mc.player) < 200F) {
                switch (((EntityOrderChaos) entity).getState()) {
                    case ARRIVE:
                    case TALK:
                    case WITHDRAW:
                    case ABSORBING:
                        break;
                    default:
                        return true;
                }
            }
        }
        return false;
    }

    /** The Champion is fighting someone, or something, near enough to hear it. Not once his planet is gone. */
    private boolean championFighting() {
        for (Entity entity : mc.world.loadedEntityList) {
            if (entity instanceof EntityChampion && entity.getDistance(mc.player) < 110F) {
                switch (((EntityChampion) entity).getState()) {
                    case DRIFT:
                    case BOARD:
                    case IMPACT:
                        break;
                    default:
                        return true;
                }
            }
        }
        return false;
    }

    private void stop() {
        if (track != null) {
            mc.getSoundHandler().stopSound(track);
            track = null;
        }
        heard.clear();
        quietUntil = -1;
    }

    /**
     * No music but a fight's starts while one plays or fades, for a while after, or while he
     * is close. Whatever else is let through is remembered, so it can be faded out later.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlaySound(PlaySoundEvent event) {
        ISound sound = event.getResultSound();
        if (sound == null || sound.getCategory() != SoundCategory.MUSIC || isFightMusic(sound) || mc.world == null) {
            return;
        }
        if (holding() || tick < quietUntil) {
            event.setResultSound(null);
        } else {
            heard.add(new Held(sound, tick));
        }
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        mc.addScheduledTask(this::stop);
    }
}
