package com.matoon.eternity.client;

import com.matoon.eternity.EternitySounds;
import com.matoon.eternity.client.sound.EntityLoopSound;
import lucraft.mods.heroesexpansion.items.HEItems;
import lucraft.mods.lucraftcore.infinity.EntityItemIndestructible;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * The Tesseract's hum. Whoever holds it in either hand hums with it, the local player
 * included; lying on the ground (on its pedestal in a Norse village, say) it hums more
 * quietly. Each source fades in and out as it is picked up, put away or left behind.
 */
@SideOnly(Side.CLIENT)
public final class TesseractClient {

    public static final TesseractClient INSTANCE = new TesseractClient();

    private static final float HELD_VOLUME = 0.8F;
    private static final float GROUND_VOLUME = 0.4F;
    private static final double HEAR_RANGE = 24D;
    private static final int CHECK_INTERVAL = 5;

    private final Map<Integer, EntityLoopSound> hums = new HashMap<>();
    private World lastWorld;
    private int ticks;

    private TesseractClient() {
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world != lastWorld) {
            stopAll();
            lastWorld = mc.world;
        }
        if (mc.world == null || mc.player == null || ++ticks % CHECK_INTERVAL != 0) {
            return;
        }
        Map<Integer, Float> wanted = new HashMap<>();
        for (EntityPlayer player : mc.world.playerEntities) {
            if (!player.isDead && player.getDistanceSq(mc.player) < HEAR_RANGE * HEAR_RANGE && holds(player)) {
                wanted.put(player.getEntityId(), HELD_VOLUME);
            }
        }
        for (Entity entity : mc.world.loadedEntityList) {
            if (entity instanceof EntityItemIndestructible && !entity.isDead
                    && ((EntityItemIndestructible) entity).getItem().getItem() == HEItems.TESSERACT
                    && entity.getDistanceSq(mc.player) < HEAR_RANGE * HEAR_RANGE) {
                wanted.put(entity.getEntityId(), GROUND_VOLUME);
            }
        }

        Set<Integer> playing = new HashSet<>();
        for (Iterator<Map.Entry<Integer, EntityLoopSound>> it = hums.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, EntityLoopSound> entry = it.next();
            EntityLoopSound hum = entry.getValue();
            if (!wanted.containsKey(entry.getKey()) || hum.isDonePlaying() || !mc.getSoundHandler().isSoundPlaying(hum)) {
                hum.fadeOut();
                it.remove();
            } else {
                playing.add(entry.getKey());
            }
        }
        for (Map.Entry<Integer, Float> entry : wanted.entrySet()) {
            Entity entity = mc.world.getEntityByID(entry.getKey());
            if (entity == null || playing.contains(entry.getKey())) {
                continue;
            }
            SoundCategory category = entity instanceof EntityPlayer ? SoundCategory.PLAYERS : SoundCategory.NEUTRAL;
            EntityLoopSound hum = new EntityLoopSound(EternitySounds.TESSERACT_HUM, category, entity, entry.getValue(), 0.12F);
            hums.put(entry.getKey(), hum);
            mc.getSoundHandler().playSound(hum);
        }
    }

    private static boolean holds(EntityPlayer player) {
        return player.getHeldItemMainhand().getItem() == HEItems.TESSERACT || player.getHeldItemOffhand().getItem() == HEItems.TESSERACT;
    }

    private void stopAll() {
        for (EntityLoopSound hum : hums.values()) {
            hum.fadeOut();
        }
        hums.clear();
    }
}
