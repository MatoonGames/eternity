package com.matoon.eternity.client;

import com.matoon.eternity.EternitySounds;
import com.matoon.eternity.client.sound.LoopSound;
import com.matoon.eternity.entity.EntityEternity;
import com.matoon.eternity.entity.EntityLivingTribunal;
import com.matoon.eternity.entity.EntityOrderChaos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.entity.Entity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The music of the great fights, and of the void left after the world ends. One track at a
 * time, crossfading as a fight turns; while one plays no other music starts, and the
 * silences that hush everything else around these beings do not hush it.
 */
@SideOnly(Side.CLIENT)
public final class FightMusic {

    public static final FightMusic INSTANCE = new FightMusic();

    private final Minecraft mc = Minecraft.getMinecraft();
    private Track track;
    private int startedAt = -1000;
    private int tick;

    /** A fight track, so the handlers that silence music can tell it apart. */
    private static final class Track extends LoopSound {
        final SoundEvent event;

        Track(SoundEvent event) {
            super(event, SoundCategory.MUSIC, true, 0.02F);
            this.event = event;
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
        }
        play(want, volume);
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
        boolean crossing = false;
        if (track != null && (track.event != want || !alive)) {
            crossing = alive;
            track.fadeOut();
            track = null;
        }
        if (track == null) {
            if (!crossing) {
                // Whatever music the world was playing makes way.
                handler.stop("", SoundCategory.MUSIC);
            }
            track = new Track(want);
            handler.playSound(track);
            startedAt = tick;
        }
        track.setTarget(volume);
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

    private void stop() {
        if (track != null) {
            mc.getSoundHandler().stopSound(track);
            track = null;
        }
    }

    /** While a fight track plays, no other music starts over it. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onPlaySound(PlaySoundEvent event) {
        ISound sound = event.getSound();
        if (sound != null && playing() && sound.getCategory() == SoundCategory.MUSIC && !isFightMusic(sound)) {
            event.setResultSound(null);
        }
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        mc.addScheduledTask(this::stop);
    }
}
