package com.matoon.eternity.world;

import com.matoon.eternity.EternityConfig;
import com.matoon.eternity.EternitySounds;
import com.matoon.eternity.compat.Galacticraft;
import com.matoon.eternity.compat.InfinityStones;
import com.matoon.eternity.compat.Tickrate;
import com.matoon.eternity.entity.EntityAstralRegulator;
import com.matoon.eternity.entity.EntityEternity;
import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.network.MsgCue;
import com.matoon.eternity.network.MsgFinale;
import com.matoon.eternity.network.MsgOmen;
import com.matoon.eternity.network.MsgPresence;
import com.matoon.eternity.network.MsgRift;
import com.matoon.eternity.world.EternityData.Mark;
import com.matoon.eternity.world.EternityData.Phase;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Runs Eternity on the server: wandering, tearing rifts between dimensions, choosing
 * whom to visit, conjuring and dissolving his body, and the death finale.
 */
public final class Director {

    public static final Director INSTANCE = new Director();

    private static final double MATERIALIZE_RANGE = 112D;
    private static final double VANISH_RANGE = 176D;
    private static final int TICKS_PER_MINUTE = 1200;

    private final Random rand = new Random();
    private final List<Scheduled> scheduled = new ArrayList<>();
    private final Map<Integer, OpenRift> openRifts = new HashMap<>();
    private final Set<UUID> participants = new HashSet<>();

    private EternityData data;
    private EntityEternity body;
    private Finale finale;
    private Unmaking unmaking = new Unmaking(this);
    private boolean travelling;
    private boolean emergePending;
    private long followAt = -1;
    /** Whether the current target has actually stood before him. */
    private boolean met;
    private int riftIds;
    /** Who released the Astral Regulator into his heart. */
    private UUID containedBy;

    private static final class Scheduled {
        final long at;
        final Runnable task;

        Scheduled(long at, Runnable task) {
            this.at = at;
            this.task = task;
        }
    }

    private static final class OpenRift {
        final int dimension;
        final MsgRift open;

        /** Where stepping into it takes you, for rifts he left through. */
        Destination exit;

        OpenRift(int dimension, MsgRift open) {
            this.dimension = dimension;
            this.open = open;
        }
    }

    /** Somewhere Eternity can step out of a rift. */
    public static final class Destination {
        final int dimension;
        final double x, y, z;

        public Destination(int dimension, double x, double y, double z) {
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    private Director() {
    }

    public void reset() {
        data = null;
        body = null;
        finale = null;
        travelling = false;
        emergePending = false;
        followAt = -1;
        scheduled.clear();
        openRifts.clear();
        participants.clear();
        singularities.clear();
        traversed.clear();
        CosmicPowers.INSTANCE.reset();
        unmaking = new Unmaking(this);
    }

    public EternityData data() {
        if (data == null) {
            data = EternityData.get();
            if (data.fresh) {
                begin(data);
            }
        }
        return data;
    }

    /** A new world: he could already be anywhere, in any dimension. */
    private void begin(EternityData d) {
        d.fresh = false;
        Destination start = anywhere();
        if (start != null) {
            d.dimension = start.dimension;
            d.x = start.x;
            d.y = start.y;
            d.z = start.z;
        }
        d.heading = rand.nextFloat() * (float) Math.PI * 2F;
        d.markDirty();
    }

    @Nullable
    public EntityEternity body() {
        return body;
    }

    public boolean isTravelling() {
        return travelling;
    }

    // ------------------------------------------------------------------ main loop

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null || DimensionManager.getWorld(0) == null) {
            return;
        }
        List<EntityPlayerMP> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) {
            return;
        }
        EternityData d = data();
        d.clock++;
        if (d.clock % 200 == 0) {
            d.markDirty();
        }
        runScheduled(d.clock);
        tickSingularities();
        CosmicPowers.INSTANCE.tick();
        tickDoors(d.clock);

        if (finale != null) {
            finale.tick(server);
            if (finale.isDone()) {
                finale = null;
            }
        }

        if (d.phase == Phase.SLAIN) {
            tickRebirth(d);
        } else if (d.phase == Phase.UNMAKING) {
            if (EternityConfig.apocalypse.worldCanEnd) {
                unmaking.tick(server, d);
            } else {
                restoreReality(server);
            }
        } else if (d.phase == Phase.VOID) {
            if (d.clock % 100 == 0) {
                for (EntityPlayerMP player : players) {
                    Unmaking.entombInVoid(player);
                }
            }
        } else if (d.phase != Phase.DYING) {
            if (!EternityConfig.roaming.enabled) {
                if (body != null) {
                    vanish();
                }
            } else {
                tickAlive(d, players);
            }
        }

        if (d.clock % 20 == 0) {
            for (EntityPlayerMP player : players) {
                syncPresence(player);
            }
        }
    }

    private void tickAlive(EternityData d, List<EntityPlayerMP> players) {
        if (d.clock % 40 == 0) {
            for (EntityPlayerMP player : players) {
                scanStones(player, d);
            }
        }
        validateBody();
        if (body == null) {
            if (!travelling) {
                simulate(d);
            }
            if (d.clock % 10 == 0) {
                tryMaterialize(d);
            }
        } else {
            d.x = body.posX;
            d.y = body.posY;
            d.z = body.posZ;
            d.health = MathHelper.clamp(body.getHealth() / body.getMaxHealth(), 0F, 1F);
            EntityPlayerMP target = targetIn(body.world);
            if (target != null && target.getDistance(body) < 64D * Math.sqrt(body.getScale())) {
                met = true;
            }
            if (d.clock % 20 == 0 && !body.isDying() && noPlayerWithin(body.world, body.posX, body.posZ, VANISH_RANGE * rangeScale(body.world))) {
                vanish();
            }
        }
        tickHunt(d);
        tickEncounters(d, players);
        if (!travelling) {
            if (d.nextRiftAt < 0) {
                scheduleNextRift(d);
            } else if (d.clock >= d.nextRiftAt && d.phase == Phase.ROAMING) {
                travel(chooseDestination(d, players));
            }
        }
    }

    private void runScheduled(long now) {
        if (scheduled.isEmpty()) {
            return;
        }
        List<Scheduled> due = new ArrayList<>();
        Iterator<Scheduled> it = scheduled.iterator();
        while (it.hasNext()) {
            Scheduled s = it.next();
            if (s.at <= now) {
                due.add(s);
                it.remove();
            }
        }
        for (Scheduled s : due) {
            s.task.run();
        }
    }

    private void schedule(int delay, Runnable task) {
        scheduled.add(new Scheduled(data().clock + delay, task));
    }

    // ------------------------------------------------------------------ the body

    /**
     * Called by every Eternity entity each tick. Returns false for impostors (for
     * example one made with /summon while the real one exists), which then dissolve.
     * A lone summoned body is adopted and Eternity relocates into it.
     */
    public boolean claim(EntityEternity entity) {
        if (body == entity) {
            return true;
        }
        EternityData d = data();
        if (body == null && !travelling && (d.phase == Phase.ROAMING || d.phase == Phase.HUNTING)) {
            body = entity;
            d.dimension = entity.dimension;
            d.x = entity.posX;
            d.y = entity.posY;
            d.z = entity.posZ;
            entity.setHealth(d.health * entity.getMaxHealth());
            d.markDirty();
            return true;
        }
        return false;
    }

    private void validateBody() {
        if (body != null && (body.isDead || !body.isAddedToWorld())) {
            body = null;
        }
    }

    private void tryMaterialize(EternityData d) {
        if (travelling && !emergePending) {
            return;
        }
        WorldServer world = DimensionManager.getWorld(d.dimension);
        if (world == null) {
            return;
        }
        BlockPos pos = new BlockPos(d.x, MathHelper.clamp(d.y, 0, 255), d.z);
        if (!world.isBlockLoaded(pos) || noPlayerWithin(world, d.x, d.z, MATERIALIZE_RANGE * rangeScale(world))) {
            return;
        }
        EntityEternity entity = new EntityEternity(world);
        EntityPlayer nearest = world.getClosestPlayer(d.x, d.y, d.z, -1D, false);
        float yaw = nearest == null ? 0F : (float) Math.toDegrees(Math.atan2(nearest.posZ - d.z, nearest.posX - d.x)) - 90F;
        entity.setLocationAndAngles(d.x, d.y, d.z, yaw, 0F);
        entity.rotationYawHead = yaw;
        entity.setHealth(Math.max(1F, d.health * entity.getMaxHealth()));
        if (emergePending) {
            entity.emerge();
        }
        body = entity;
        world.spawnEntity(entity);
    }

    /** Dissolves the body; Eternity carries on as data. */
    public void vanish() {
        if (body != null) {
            EternityData d = data();
            d.x = body.posX;
            d.y = body.posY;
            d.z = body.posZ;
            d.health = MathHelper.clamp(body.getHealth() / body.getMaxHealth(), 0.01F, 1F);
            body.setDead();
            body = null;
            d.markDirty();
        }
    }

    private static boolean noPlayerWithin(World world, double x, double z, double range) {
        for (EntityPlayer player : world.playerEntities) {
            if (!player.isSpectator()) {
                double dx = player.posX - x;
                double dz = player.posZ - z;
                if (dx * dx + dz * dz < range * range) {
                    return false;
                }
            }
        }
        return true;
    }

    /** In space he is a colossus, so everything about him happens at a larger distance. */
    private static double rangeScale(World world) {
        return Galacticraft.isSpace(world) ? 1.6D : 1D;
    }

    // ------------------------------------------------------------------ wandering

    private void simulate(EternityData d) {
        EntityPlayerMP target = d.phase == Phase.HUNTING ? player(d.target) : null;
        if (target != null && target.dimension == d.dimension) {
            double dx = target.posX - d.x;
            double dy = (target.posY + 10D) - d.y;
            double dz = target.posZ - d.z;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist > 1D) {
                double speed = MathHelper.clamp(dist * 0.004D, 0.6D, 6D);
                d.x += dx / dist * speed;
                d.y += dy / dist * speed;
                d.z += dz / dist * speed;
            }
            return;
        }
        d.heading += (rand.nextFloat() - 0.5F) * 0.03F;
        double speed = 0.12D;
        d.x += MathHelper.cos(d.heading) * speed;
        d.z += MathHelper.sin(d.heading) * speed;
        double[] band = altitudeBand(d.dimension);
        double wantY = (band[0] + band[1]) / 2D + Math.sin(d.clock / 600D) * (band[1] - band[0]) / 2D;
        d.y += (wantY - d.y) * 0.002D;
    }

    private void scheduleNextRift(EternityData d) {
        int min = EternityConfig.roaming.minRiftMinutes;
        int max = Math.max(min, EternityConfig.roaming.maxRiftMinutes);
        double minutes = min + rand.nextDouble() * (max - min);
        // The more stones are gathered, the more restless he becomes.
        int attention = 0;
        for (Mark mark : d.marks.values()) {
            attention = Math.max(attention, InfinityStones.count(mark.everMask));
        }
        minutes *= 1D - 0.07D * attention;
        d.nextRiftAt = d.clock + (long) (minutes * TICKS_PER_MINUTE);
        d.markDirty();
    }

    private Destination chooseDestination(EternityData d, List<EntityPlayerMP> players) {
        List<EntityPlayerMP> eligible = new ArrayList<>();
        int totalWeight = 0;
        for (EntityPlayerMP player : players) {
            if (!blacklisted(player.dimension) && !player.isSpectator()) {
                eligible.add(player);
                int stones = InfinityStones.count(d.mark(player.getUniqueID()).everMask);
                totalWeight += 1 + stones * stones;
            }
        }
        if (!eligible.isEmpty()) {
            int roll = rand.nextInt(totalWeight);
            EntityPlayerMP chosen = eligible.get(0);
            for (EntityPlayerMP player : eligible) {
                int stones = InfinityStones.count(d.mark(player.getUniqueID()).everMask);
                roll -= 1 + stones * stones;
                if (roll < 0) {
                    chosen = player;
                    break;
                }
            }
            int stones = InfinityStones.count(d.mark(chosen.getUniqueID()).everMask);
            // Without stones he almost never comes close; with five he circles you.
            if (rand.nextDouble() < 0.08D + 0.1D * stones) {
                double radius = (4500D - (4200D * Math.min(stones, 5) / 5D)) * (0.7D + rand.nextDouble() * 0.6D);
                return around(chosen, radius);
            }
            if (rand.nextBoolean()) {
                return around(eligible.get(rand.nextInt(eligible.size())), 1500D + rand.nextDouble() * 6500D);
            }
        }
        return anywhere();
    }

    /** Somewhere in any dimension he may enter, picked at random. */
    @Nullable
    private Destination anywhere() {
        List<Integer> dims = new ArrayList<>();
        for (Map.Entry<DimensionType, IntSortedSet> entry : DimensionManager.getRegisteredDimensions().entrySet()) {
            for (int dim : entry.getValue()) {
                if (!blacklisted(dim)) {
                    dims.add(dim);
                }
            }
        }
        if (dims.isEmpty()) {
            return null;
        }
        int dim = dims.get(rand.nextInt(dims.size()));
        double angle = rand.nextDouble() * Math.PI * 2D;
        double radius = rand.nextDouble() * 9000D;
        double[] band = altitudeBand(dim);
        return new Destination(dim, Math.cos(angle) * radius, band[0] + rand.nextDouble() * (band[1] - band[0]), Math.sin(angle) * radius);
    }

    private Destination around(EntityPlayerMP player, double radius) {
        double angle = rand.nextDouble() * Math.PI * 2D;
        double[] band = altitudeBand(player.dimension);
        return new Destination(player.dimension, player.posX + Math.cos(angle) * radius,
                band[0] + rand.nextDouble() * (band[1] - band[0]), player.posZ + Math.sin(angle) * radius);
    }

    /** A spot in front of the player, where they will see him step out. */
    private Destination before(EntityPlayerMP player, double distance) {
        double scale = Galacticraft.isSpace(player.world) ? EternityConfig.roaming.spaceScale * 0.5D : 1D;
        float yaw = (float) Math.toRadians(player.rotationYaw + (rand.nextFloat() - 0.5F) * 50F);
        double d = distance * scale;
        return new Destination(player.dimension, player.posX - MathHelper.sin(yaw) * d,
                player.posY - 2D * scale, player.posZ + MathHelper.cos(yaw) * d);
    }

    private static double[] altitudeBand(int dimension) {
        WorldServer world = DimensionManager.getWorld(dimension);
        WorldProvider provider = world != null ? world.provider : null;
        if (provider == null) {
            return new double[]{110, 170};
        }
        if (Galacticraft.isSpace(world)) {
            return new double[]{90, 170};
        }
        if (provider.getDimensionType() == DimensionType.NETHER || provider.isNether()) {
            return new double[]{45, 105};
        }
        if (provider.getDimensionType() == DimensionType.THE_END) {
            return new double[]{70, 120};
        }
        return new double[]{110, 170};
    }

    private static boolean blacklisted(int dimension) {
        if (dimension == StarChamber.dimension()) {
            return true;
        }
        for (int id : EternityConfig.roaming.blacklistedDimensions) {
            if (id == dimension) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ rifts

    /** Tears open a rift in front of his body, then another at the destination, and steps through. */
    public void travel(@Nullable Destination to) {
        if (travelling || to == null) {
            return;
        }
        EternityData d = data();
        travelling = true;
        int fromDim = d.dimension;
        double scaleFrom = scaleFor(fromDim);
        double bodyHeight = EntityEternity.BASE_HEIGHT * scaleFrom;
        int departure = openRift(fromDim, d.x, d.y + bodyHeight * 0.55D, d.z, riftHeight(scaleFrom), 0);
        // The way he went stays open for a while. Anyone brave enough can follow.
        OpenRift door = openRifts.get(departure);
        if (door != null) {
            door.exit = to;
        }
        if (body != null) {
            body.depart();
        }
        int linger = EternityConfig.roaming.riftLingerSeconds * 20;
        schedule(55, this::vanish);
        schedule(75 + linger, () -> closeRift(departure));
        schedule(60, () -> {
            double scaleTo = scaleFor(to.dimension);
            d.dimension = to.dimension;
            d.x = to.x;
            d.y = to.y;
            d.z = to.z;
            d.markDirty();
            int arrival = openRift(to.dimension, to.x, to.y + EntityEternity.BASE_HEIGHT * scaleTo * 0.55D, to.z, riftHeight(scaleTo), 0);
            emergePending = true;
            schedule(150 + linger, () -> closeRift(arrival));
            schedule(150, () -> {
                emergePending = false;
                travelling = false;
                scheduleNextRift(d);
            });
        });
    }

    /**
     * A Rift Gate: one of his great rifts torn open a few blocks in front of the player,
     * leading to the destination, with its twin opening there. Anyone can walk through.
     * Both seal themselves after the linger time.
     */
    public void openGate(EntityPlayerMP player, Destination to) {
        Vec3d look = new Vec3d(player.getLookVec().x, 0, player.getLookVec().z);
        look = look.lengthSquared() < 1.0E-4 ? new Vec3d(0, 0, 1) : look.normalize();
        double gx = player.posX + look.x * 6D;
        double gz = player.posZ + look.z * 6D;
        double gy = player.posY + 2.5D;
        int entrance = openRift(player.dimension, gx, gy, gz, riftHeight(scaleFor(player.dimension)), 0);
        OpenRift door = openRifts.get(entrance);
        if (door != null) {
            door.exit = to;
        }
        int exit = openRift(to.dimension, to.x, to.y + 2.5D, to.z, riftHeight(scaleFor(to.dimension)), 0);
        int linger = Math.max(10, EternityConfig.roaming.riftLingerSeconds) * 20;
        closeRiftLater(entrance, linger);
        closeRiftLater(exit, linger);
    }

    /** A crack across the whole sky. */
    private float riftHeight(double scale) {
        return (float) ((280D + rand.nextDouble() * 220D) * Math.max(1D, scale * 0.6D));
    }

    // ------------------------------------------------------------------ following him through

    private final Map<UUID, Long> traversed = new HashMap<>();

    /** Anyone who steps into the mouth of a departure rift is pulled through to where he went. */
    private void tickDoors(long clock) {
        if (openRifts.isEmpty()) {
            return;
        }
        List<EntityPlayerMP> travellers = new ArrayList<>();
        List<Destination> exits = new ArrayList<>();
        for (OpenRift rift : openRifts.values()) {
            if (rift.exit == null) {
                continue;
            }
            WorldServer world = DimensionManager.getWorld(rift.dimension);
            if (world == null) {
                continue;
            }
            double reach = 5D * Math.max(1D, scaleFor(rift.dimension) * 0.5D);
            for (EntityPlayer player : world.playerEntities) {
                if (!(player instanceof EntityPlayerMP) || player.isSpectator() || travellers.contains(player)) {
                    continue;
                }
                double dx = player.posX - rift.open.x;
                double dz = player.posZ - rift.open.z;
                double dy = player.posY + 1D - rift.open.y;
                Long last = traversed.get(player.getUniqueID());
                if (dx * dx + dz * dz < reach * reach && Math.abs(dy) < reach * 2D && (last == null || clock - last > 100)) {
                    travellers.add((EntityPlayerMP) player);
                    exits.add(rift.exit);
                }
            }
        }
        for (int i = 0; i < travellers.size(); i++) {
            traverse(travellers.get(i), exits.get(i));
            traversed.put(travellers.get(i).getUniqueID(), clock);
        }
    }

    private void traverse(EntityPlayerMP player, Destination exit) {
        EternityNetwork.send(new MsgCue(MsgCue.TRAVERSE, -1, player.posX, player.posY, player.posZ, 0, 0, 0), player);
        player.world.playSound(null, player.posX, player.posY, player.posZ, EternitySounds.RIFT_CLOSE, SoundCategory.HOSTILE, 3F, 1.4F);
        Teleport.player(player, exit.dimension, exit.x, exit.y, exit.z, true);
    }

    // ------------------------------------------------------------------ black holes

    private final List<Singularity> singularities = new ArrayList<>();

    public void addSingularity(Singularity singularity) {
        singularities.add(singularity);
    }

    public boolean hasSingularityNear(World world, double x, double z) {
        for (Singularity s : singularities) {
            if (s.world == world && Math.abs(s.centre.x - x) < 200D && Math.abs(s.centre.z - z) < 200D) {
                return true;
            }
        }
        return false;
    }

    private void tickSingularities() {
        if (singularities.isEmpty()) {
            return;
        }
        Iterator<Singularity> it = singularities.iterator();
        while (it.hasNext()) {
            Singularity s = it.next();
            if (DimensionManager.getWorld(s.world.provider.getDimension()) != s.world) {
                it.remove();
                continue;
            }
            s.tick();
            if (s.isDone()) {
                it.remove();
            }
        }
    }

    private static double scaleFor(int dimension) {
        WorldServer world = DimensionManager.getWorld(dimension);
        return world != null && Galacticraft.isSpace(world) ? EternityConfig.roaming.spaceScale : 1D;
    }

    public int openRift(int dimension, double x, double y, double z, float height, int flags) {
        int id = ++riftIds;
        MsgRift msg = MsgRift.open(id, x, y, z, height, rand.nextLong(), flags);
        openRifts.put(id, new OpenRift(dimension, msg));
        EternityNetwork.sendToDimension(msg, dimension);
        WorldServer world = DimensionManager.getWorld(dimension);
        if (world != null) {
            if ((flags & MsgRift.FLAG_FINALE) == 0) {
                world.playSound(null, x, y, z, EternitySounds.RIFT_OPEN, SoundCategory.HOSTILE, 8F, 0.8F);
            }
            for (EntityPlayer player : world.playerEntities) {
                if (player instanceof EntityPlayerMP && player.getDistance(x, player.posY, z) < 4000D) {
                    Advancements.grant((EntityPlayerMP) player, Advancements.WITNESS);
                }
            }
        }
        return id;
    }

    public void closeRift(int id) {
        OpenRift rift = openRifts.remove(id);
        if (rift == null) {
            return;
        }
        EternityNetwork.sendToDimension(MsgRift.close(id), rift.dimension);
        WorldServer world = DimensionManager.getWorld(rift.dimension);
        if (world != null && (rift.open.flags & MsgRift.FLAG_FINALE) == 0) {
            world.playSound(null, rift.open.x, rift.open.y, rift.open.z, EternitySounds.RIFT_CLOSE, SoundCategory.HOSTILE, 8F, 0.7F);
        }
    }

    // ------------------------------------------------------------------ stones and encounters

    private void scanStones(EntityPlayerMP player, EternityData d) {
        int held = InfinityStones.scan(player);
        if (held == 0 && !d.marks.containsKey(player.getUniqueID())) {
            return;
        }
        Mark mark = d.mark(player.getUniqueID());
        int before = mark.everMask;
        int after = before | held;
        int delay = EternityConfig.roaming.encounterDelaySeconds * 20;
        if (after != before) {
            mark.everMask = after;
            d.markDirty();
            int level = InfinityStones.count(after);
            Advancements.grant(player, Advancements.ROOT);
            boolean complete = level >= 6 && InfinityStones.count(before) < 6;
            if (complete) {
                mark.encounterAt = d.clock + delay;
            }
            EternityNetwork.send(new MsgOmen(level, complete ? delay : 0), player);
            return;
        }
        boolean huntingThem = d.phase == Phase.HUNTING && player.getUniqueID().equals(d.target);
        if (held == InfinityStones.ALL && mark.encounterAt < 0 && !huntingThem
                && (mark.lastEncounter < 0 || d.clock - mark.lastEncounter > EternityConfig.roaming.repeatEncounterMinutes * (long) TICKS_PER_MINUTE)) {
            mark.encounterAt = d.clock + delay;
            d.markDirty();
            EternityNetwork.send(new MsgOmen(6, delay), player);
        }
    }

    private void tickEncounters(EternityData d, List<EntityPlayerMP> players) {
        for (EntityPlayerMP player : players) {
            Mark mark = d.marks.get(player.getUniqueID());
            if (mark == null || mark.encounterAt < 0 || d.clock < mark.encounterAt) {
                continue;
            }
            if (d.phase == Phase.HUNTING && player.getUniqueID().equals(d.target)) {
                mark.encounterAt = -1;
                continue;
            }
            if (travelling || blacklisted(player.dimension) || player.isSpectator()) {
                mark.encounterAt = d.clock + 200;
                continue;
            }
            mark.encounterAt = -1;
            mark.lastEncounter = d.clock;
            d.markDirty();
            startHunt(player, 20 * TICKS_PER_MINUTE);
            travel(before(player, 44D));
            return;
        }
    }

    private void tickHunt(EternityData d) {
        if (d.phase != Phase.HUNTING) {
            return;
        }
        EntityPlayerMP target = player(d.target);
        if (target == null) {
            endHunt(false);
            return;
        }
        if (InfinityStones.scan(target) == InfinityStones.ALL) {
            d.huntUntil = Math.max(d.huntUntil, d.clock + TICKS_PER_MINUTE);
        }
        if (d.clock > d.huntUntil) {
            endHunt(true);
            return;
        }
        if (target.dimension != d.dimension && !travelling && !blacklisted(target.dimension)) {
            if (followAt < 0) {
                followAt = d.clock + 160;
            } else if (d.clock >= followAt) {
                followAt = -1;
                travel(before(target, 52D));
            }
        } else {
            followAt = -1;
        }
    }

    /** Someone caught his eye or struck him. */
    public void provoke(EntityPlayerMP player) {
        EternityData d = data();
        if (d.phase == Phase.ROAMING && !player.isSpectator()) {
            startHunt(player, 3 * TICKS_PER_MINUTE);
        } else if (d.phase == Phase.HUNTING && player.getUniqueID().equals(d.target)) {
            d.huntUntil = Math.max(d.huntUntil, d.clock + TICKS_PER_MINUTE);
        }
    }

    public void startHunt(EntityPlayerMP player, long ticks) {
        EternityData d = data();
        d.phase = Phase.HUNTING;
        d.target = player.getUniqueID();
        d.huntUntil = d.clock + ticks;
        d.markDirty();
        met = false;
        if (body != null) {
            body.onHuntStart();
        }
    }

    private void endHunt(boolean survived) {
        EternityData d = data();
        if (survived && met) {
            EntityPlayerMP target = player(d.target);
            if (target != null) {
                Advancements.grant(target, Advancements.SURVIVOR);
            }
        }
        d.phase = Phase.ROAMING;
        d.target = null;
        d.nextRiftAt = d.clock + 160;
        d.markDirty();
        followAt = -1;
    }

    @Nullable
    public EntityPlayerMP targetIn(World world) {
        EternityData d = data();
        if (d.phase != Phase.HUNTING) {
            return null;
        }
        EntityPlayerMP target = player(d.target);
        return target != null && target.world == world ? target : null;
    }

    @Nullable
    private static EntityPlayerMP player(@Nullable UUID id) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        return id == null || server == null ? null : server.getPlayerList().getPlayerByUUID(id);
    }

    /** Forgets every stone this player ever held. */
    public void forget(UUID player) {
        EternityData d = data();
        if (d.marks.remove(player) != null) {
            d.markDirty();
        }
    }

    public void recordParticipant(EntityPlayerMP player) {
        participants.add(player.getUniqueID());
    }

    /** Sends a combat cue to everyone who could see it. */
    public void cue(Entity source, MsgCue cue) {
        double scale = source instanceof EntityEternity ? ((EntityEternity) source).getScale() : 1D;
        EternityNetwork.sendAround(cue, source.dimension, source.posX, source.posY, source.posZ, 320D * Math.sqrt(scale));
    }

    // ------------------------------------------------------------------ death

    public void beginFinale(EntityEternity entity, DamageSource cause) {
        EternityData d = data();
        if (d.phase != Phase.ROAMING && d.phase != Phase.HUNTING) {
            return;
        }
        if (cause.getTrueSource() instanceof EntityPlayerMP) {
            participants.add(cause.getTrueSource().getUniqueID());
        }
        d.phase = Phase.DYING;
        d.target = null;
        d.markDirty();
        entity.beginDying();
        for (Integer id : new ArrayList<>(openRifts.keySet())) {
            closeRift(id);
        }
        scheduled.clear();
        travelling = false;
        emergePending = false;
        finale = new Finale(this, entity, new HashSet<>(participants));
        participants.clear();
    }

    void finishFinale() {
        EternityData d = data();
        d.phase = Phase.SLAIN;
        d.slainAt = d.clock;
        d.health = 1F;
        d.markDirty();
        if (body != null) {
            body.setDead();
            body = null;
        }
    }

    // ------------------------------------------------------------------ containment

    /**
     * Someone used the Astral Regulator. It works only while he is dying and before
     * the supernova, and only if they are near enough to see his heart.
     */
    public boolean tryContain(EntityPlayerMP player) {
        if (finale == null || !finale.canContain()) {
            return false;
        }
        EntityEternity dying = finale.body();
        if (dying.world != player.world) {
            return false;
        }
        Vec3d heart = dying.chest();
        Vec3d eyes = player.getPositionEyes(1F);
        Vec3d toHeart = heart.subtract(eyes);
        double distance = toHeart.length();
        double reach = 48D + 24D * dying.getScale();
        if (distance > reach || player.getLook(1F).dotProduct(toHeart.normalize()) < 0.7D) {
            return false;
        }
        Vec3d from = eyes.add(player.getLook(1F).scale(1.2D));
        player.world.spawnEntity(new EntityAstralRegulator(player.world, from, dying));
        participants.add(player.getUniqueID());
        containedBy = player.getUniqueID();
        finale.contain();
        return true;
    }

    /** The Regulator has reached his heart and begun to drink. */
    public void containmentBegan() {
        if (finale != null) {
            EntityEternity dying = finale.body();
            dying.world.playSound(null, dying.posX, dying.posY, dying.posZ, EternitySounds.ENTITY_VOICE, SoundCategory.MASTER, 12F, 0.4F);
        }
    }

    public void containmentComplete(Vec3d at) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (finale != null && server != null) {
            finale.complete(server, at);
            body = null;
            // Whoever caged him has drawn the attention of something far greater.
            if (containedBy != null) {
                TribunalDirector.INSTANCE.schedule(containedBy);
                containedBy = null;
            }
        }
    }

    /** Nobody held him together. Unless the config forbids it, this is the end of the world. */
    void beginUnmaking() {
        EternityData d = data();
        if (body != null) {
            body.setDead();
            body = null;
        }
        if (!EternityConfig.apocalypse.worldCanEnd) {
            EternityNetwork.sendToAll(new MsgFinale(MsgFinale.AFTERMATH, d.dimension, d.x, d.y, d.z));
            schedule(500, () -> EternityNetwork.sendToAll(new MsgFinale(MsgFinale.END, d.dimension, d.x, d.y, d.z)));
            finishFinale();
            return;
        }
        d.phase = Phase.UNMAKING;
        d.doomAt = d.clock;
        d.markDirty();
    }

    /** Operator escape hatch: heals reality, whatever state it is in. */
    public void restoreReality(MinecraftServer server) {
        EternityData d = data();
        boolean wasVoid = d.phase == Phase.VOID;
        finale = null;
        unmaking = new Unmaking(this);
        for (Integer id : new ArrayList<>(openRifts.keySet())) {
            closeRift(id);
        }
        d.phase = Phase.SLAIN;
        d.slainAt = d.clock;
        d.doomAt = -1;
        d.markDirty();
        Tickrate.restore();
        if (wasVoid) {
            for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
                player.setGameType(server.getGameType());
            }
        }
        EternityNetwork.sendToAll(new MsgFinale(MsgFinale.END, 0, 0, 0, 0));
    }

    /** Skips straight into the unmaking, at the given progress. For testing. */
    public void forceUnmaking(float progress) {
        EternityData d = data();
        vanish();
        finale = null;
        d.phase = Phase.UNMAKING;
        d.doomAt = d.clock - (long) (Unmaking.duration() * Math.max(0F, Math.min(1F, progress)));
        d.markDirty();
    }

    public void closeRiftLater(int id, int delay) {
        schedule(delay, () -> closeRift(id));
    }

    private void tickRebirth(EternityData d) {
        int minutes = EternityConfig.roaming.rebirthMinutes;
        if (minutes < 0 || finale != null || d.clock - d.slainAt < minutes * (long) TICKS_PER_MINUTE) {
            return;
        }
        d.phase = Phase.ROAMING;
        d.health = 1F;
        d.nextRiftAt = -1;
        for (Mark mark : d.marks.values()) {
            mark.lastEncounter = -1;
        }
        d.markDirty();
    }

    // ------------------------------------------------------------------ presence

    private void syncPresence(EntityPlayerMP player) {
        EternityData d = data();
        boolean alive = d.phase == Phase.ROAMING || d.phase == Phase.HUNTING || d.phase == Phase.DYING;
        boolean present = alive && EternityConfig.roaming.enabled && player.dimension == d.dimension;
        boolean hunted = d.phase == Phase.HUNTING && player.getUniqueID().equals(d.target);
        EternityNetwork.send(new MsgPresence(present, d.x, d.y, d.z, hunted, Galacticraft.isSpace(player.world)), player);
    }

    private void sendOpenRifts(EntityPlayerMP player) {
        for (OpenRift rift : openRifts.values()) {
            if (rift.dimension == player.dimension) {
                EternityNetwork.send(rift.open, player);
            }
        }
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            syncPresence((EntityPlayerMP) event.player);
            sendOpenRifts((EntityPlayerMP) event.player);
            if (finale != null) {
                finale.join((EntityPlayerMP) event.player);
            }
            EternityData d = data();
            if (d.phase == Phase.VOID) {
                Unmaking.entombInVoid((EntityPlayerMP) event.player);
            }
            if (d.phase == Phase.UNMAKING || d.phase == Phase.DYING) {
                Tickrate.resend();
            }
            if (d.phase == Phase.UNMAKING) {
                unmaking.sendFractures((EntityPlayerMP) event.player);
            }
        }
    }

    @SubscribeEvent
    public void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            syncPresence((EntityPlayerMP) event.player);
            sendOpenRifts((EntityPlayerMP) event.player);
            if (data().phase == Phase.UNMAKING) {
                unmaking.sendFractures((EntityPlayerMP) event.player);
            }
        }
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayerMP) || data == null) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.getEntityLiving();
        EternityData d = data();
        if (d.phase == Phase.HUNTING && player.getUniqueID().equals(d.target)) {
            Mark mark = d.marks.get(player.getUniqueID());
            if (mark != null) {
                mark.lastEncounter = d.clock;
            }
            endHunt(false);
        }
    }
}
