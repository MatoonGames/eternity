package com.matoon.eternity.world;

import com.matoon.eternity.compat.Galacticraft;
import com.matoon.eternity.compat.Tickrate;
import com.matoon.eternity.entity.EntityAstralRegulator;
import com.matoon.eternity.entity.EntityEternity;
import com.matoon.eternity.item.ItemAstralRegulator;
import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.network.MsgFinale;
import com.matoon.eternity.network.MsgRift;
import lucraft.mods.lucraftcore.infinity.EntityItemIndestructible;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * The death of Eternity, lived through by every player in every dimension. Time
 * slows as the universe fractures and rifts tear open across every sky.
 *
 * If someone sets the Astral Regulator into his heart before the end, the countdown
 * stops, it drinks his energy back in, and the universe is saved. If not, everything
 * collapses into a supernova, and reality begins to unmake itself.
 */
final class Finale {

    static final int UNRAVEL_AT = 200;
    static final int COLLAPSE_AT = 500;
    static final int SUPERNOVA_AT = 560;
    static final int SILENCE_AT = 660;
    static final int UNMAKING_AT = 760;

    /** After containment: time recovers, then the quiet aftermath. */
    private static final int SAVED_AFTERMATH_AT = 200;
    private static final int SAVED_END_AT = 700;

    /** Tickrate at the instant before the supernova. */
    private static final float SLOWEST = 3F;

    private final Director director;
    private final EntityEternity body;
    private final Set<UUID> participants;
    private final int dimension;
    private final double x, y, z;
    private final Random rand = new Random();
    private final List<Integer> rifts = new ArrayList<>();
    private int stage = -1;
    private int t;

    private boolean contained;
    private int containTicks;
    private float rateAtContainment = Tickrate.NORMAL;
    private boolean saved;
    private int savedTicks;
    private boolean doomed;

    Finale(Director director, EntityEternity body, Set<UUID> participants) {
        this.director = director;
        this.body = body;
        this.participants = participants;
        this.dimension = body.dimension;
        this.x = body.posX;
        this.y = body.posY + body.height * 0.6D;
        this.z = body.posZ;
    }

    boolean isDone() {
        return saved ? savedTicks > SAVED_END_AT : doomed;
    }

    EntityEternity body() {
        return body;
    }

    /** Whether the Astral Regulator can still be used: only before the supernova. */
    boolean canContain() {
        return !contained && !saved && t < SUPERNOVA_AT && !body.isDead;
    }

    /** The Regulator has been released toward him. The countdown stops here. */
    void contain() {
        contained = true;
        rateAtContainment = Tickrate.current();
        advance(MsgFinale.CONTAIN, 0F);
    }

    /** Someone logged in mid-finale: catch them up. */
    void join(EntityPlayerMP player) {
        if (stage >= 0) {
            EternityNetwork.send(new MsgFinale(stage, dimension, x, y, z), player);
        }
    }

    void tick(MinecraftServer server) {
        if (saved) {
            tickSaved();
            return;
        }
        if (contained) {
            tickContained();
            return;
        }

        if (t == 0) advance(MsgFinale.FRACTURE, 0F);
        if (t == UNRAVEL_AT) advance(MsgFinale.UNRAVEL, 0F);
        if (t == COLLAPSE_AT) {
            advance(MsgFinale.COLLAPSE, 0F);
            closeRifts();
        }
        if (t == SUPERNOVA_AT) {
            advance(MsgFinale.SUPERNOVA, 0F);
            supernova(server);
        }
        if (t == SILENCE_AT) advance(MsgFinale.SILENCE, 0F);
        if (t == UNMAKING_AT) {
            doomed = true;
            director.beginUnmaking();
            return;
        }

        // Time itself slows as he comes apart, until it almost stops.
        if (t <= SUPERNOVA_AT && t % 4 == 0) {
            float p = t / (float) SUPERNOVA_AT;
            Tickrate.set(Tickrate.NORMAL - (Tickrate.NORMAL - SLOWEST) * (float) Math.pow(p, 1.6D));
        }
        if (t > 40 && t < COLLAPSE_AT) {
            unravel(server);
        }
        t++;
    }

    /** The Regulator drinks him in; the rifts seal one by one and time starts to move again. */
    private void tickContained() {
        containTicks++;
        float progress = Math.min(1F, containTicks / (float) (EntityAstralRegulator.FLIGHT_TICKS + EntityAstralRegulator.ABSORB_TICKS));
        if (containTicks % 4 == 0) {
            Tickrate.set(rateAtContainment + (12F - rateAtContainment) * progress);
        }
        if (containTicks % 20 == 0) {
            EternityNetwork.sendToAll(new MsgFinale(MsgFinale.CONTAIN, dimension, x, y, z, progress));
        }
        if (containTicks % 12 == 0 && !rifts.isEmpty()) {
            director.closeRift(rifts.remove(rifts.size() - 1));
        }
        // If the Regulator was lost (its chunk unloaded, say), the containment still completes.
        if (containTicks > EntityAstralRegulator.FLIGHT_TICKS + EntityAstralRegulator.ABSORB_TICKS + 200) {
            director.containmentComplete(body.chest());
        }
    }

    /** The charged Regulator falls; the universe exhales. */
    void complete(MinecraftServer server, Vec3d at) {
        if (saved) {
            return;
        }
        saved = true;
        savedTicks = 0;
        closeRifts();
        if (!body.isDead) {
            body.setDead();
        }
        WorldServer world = server.getWorld(dimension);
        if (world != null) {
            EntityItemIndestructible orb = new EntityItemIndestructible(world, at.x, at.y, at.z, ItemAstralRegulator.charged());
            orb.setEntitySize(0.5F, 0.5F);
            world.spawnEntity(orb);
            for (EntityPlayerMP player : world.getPlayers(EntityPlayerMP.class, p -> p != null && p.getDistance(x, y, z) < 200D)) {
                participants.add(player.getUniqueID());
            }
        }
        for (UUID id : participants) {
            EntityPlayerMP player = server.getPlayerList().getPlayerByUUID(id);
            if (player != null) {
                Advancements.grant(player, Advancements.SLAYER);
                Advancements.grant(player, Advancements.KEEPER);
            }
        }
        advance(MsgFinale.SAVED, 1F);
    }

    private void tickSaved() {
        savedTicks++;
        if (savedTicks <= SAVED_AFTERMATH_AT && savedTicks % 4 == 0) {
            float p = savedTicks / (float) SAVED_AFTERMATH_AT;
            Tickrate.set(12F + (Tickrate.NORMAL - 12F) * p);
        }
        if (savedTicks == SAVED_AFTERMATH_AT) {
            Tickrate.restore();
            advance(MsgFinale.AFTERMATH, 0F);
        }
        if (savedTicks == SAVED_END_AT) {
            advance(MsgFinale.END, 0F);
            director.finishFinale();
        }
    }

    private void advance(int next, float level) {
        stage = next;
        EternityNetwork.sendToAll(new MsgFinale(next, dimension, x, y, z, level));
    }

    private void closeRifts() {
        for (int id : rifts) {
            director.closeRift(id);
        }
        rifts.clear();
    }

    /** Rifts tear open around every player in every dimension, and the sky splits with light. */
    private void unravel(MinecraftServer server) {
        float progress = t / (float) COLLAPSE_AT;
        int riftEvery = t < UNRAVEL_AT ? 30 : 10;
        for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
            if (t % riftEvery == 0 && rand.nextFloat() < 0.35F + progress * 0.5F) {
                double angle = rand.nextDouble() * Math.PI * 2D;
                double radius = 220D + rand.nextDouble() * 2600D;
                double height = 450D + rand.nextDouble() * 900D * (0.5D + progress);
                rifts.add(director.openRift(player.dimension,
                        player.posX + Math.cos(angle) * radius,
                        player.posY + 30D + rand.nextDouble() * 90D,
                        player.posZ + Math.sin(angle) * radius,
                        (float) height, MsgRift.FLAG_FINALE));
            }
            if (t > UNRAVEL_AT && t % 14 == 0 && rand.nextFloat() < 0.5F && !Galacticraft.isSpace(player.world)) {
                strike(player);
            }
        }
    }

    private void strike(EntityPlayerMP player) {
        WorldServer world = player.getServerWorld();
        double angle = rand.nextDouble() * Math.PI * 2D;
        double radius = 24D + rand.nextDouble() * 110D;
        double sx = player.posX + Math.cos(angle) * radius;
        double sz = player.posZ + Math.sin(angle) * radius;
        BlockPos column = new BlockPos(sx, 0, sz);
        if (!world.isBlockLoaded(column)) {
            return;
        }
        double sy = world.provider.isNether() ? player.posY : world.getHeight(column).getY();
        // Effect-only: thunder and light, no fire, no damage.
        world.addWeatherEffect(new EntityLightningBolt(world, sx, sy, sz, true));
    }

    private void supernova(MinecraftServer server) {
        // The blast snaps time back to full speed; from here on it will not stay still.
        Tickrate.restore();
        if (!body.isDead) {
            body.setDead();
        }
        WorldServer world = server.getWorld(dimension);
        if (world != null) {
            int total = 12000;
            for (int i = 0; i < 40; i++) {
                EntityXPOrb orb = new EntityXPOrb(world, x + rand.nextGaussian() * 3D, y, z + rand.nextGaussian() * 3D, total / 40);
                orb.motionX = rand.nextGaussian() * 0.4D;
                orb.motionY = rand.nextDouble() * 0.6D;
                orb.motionZ = rand.nextGaussian() * 0.4D;
                world.spawnEntity(orb);
            }
            for (EntityPlayerMP player : world.getPlayers(EntityPlayerMP.class, p -> p != null && p.getDistance(x, y, z) < 160D)) {
                participants.add(player.getUniqueID());
            }
        }
        for (UUID id : participants) {
            EntityPlayerMP player = server.getPlayerList().getPlayerByUUID(id);
            if (player != null) {
                Advancements.grant(player, Advancements.SLAYER);
            }
        }
    }
}
