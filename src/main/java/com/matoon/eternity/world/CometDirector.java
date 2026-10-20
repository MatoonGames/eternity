package com.matoon.eternity.world;

import com.matoon.eternity.EternityConfig;
import com.matoon.eternity.EternitySounds;
import com.matoon.eternity.entity.EntityCosmicComet;
import com.matoon.eternity.entity.EternityDamage;
import com.matoon.eternity.item.EternityItems;
import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.network.MsgComet;
import com.matoon.eternity.world.CometData.Phase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Runs the Cosmic Comet: the rare roll that brings it into someone's sky, its flight,
 * the heat that burns everything near it, every blow struck against it, and the
 * moment it breaks open and gives up the Astral Regulator. After that it is gone
 * from the world for good.
 */
public final class CometDirector {

    public static final CometDirector INSTANCE = new CometDirector();

    private static final int TICKS_PER_MINUTE = 1200;
    private static final int CRACK_TICKS = 100;
    private static final int DEPART_TICKS = 120;
    private static final int SYNC_INTERVAL = 5;

    private final Random rand = new Random();

    private CometData data;
    private EntityCosmicComet body;
    /** Share of its cruising speed it keeps. It slows to a crawl while someone is close. */
    private double pace = 1D;
    /** How far it actually moved last tick, for clients to extrapolate with. */
    private double lastDx, lastDy, lastDz;
    /** Set while this class swings a player's weapon at the comet on their behalf. */
    private boolean striking;

    private CometDirector() {
    }

    public void reset() {
        data = null;
        body = null;
        pace = 1D;
        lastDx = lastDy = lastDz = 0D;
        striking = false;
    }

    public CometData data() {
        if (data == null) {
            data = CometData.get();
        }
        return data;
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
        CometData d = data();
        d.clock++;
        if (d.clock % 200 == 0) {
            d.markDirty();
        }
        switch (d.phase) {
            case DORMANT:
                if (d.clock % TICKS_PER_MINUTE == 0 && EternityConfig.comet.enabled) {
                    rollAppearance(players);
                }
                break;
            case PASSING:
                tickPassing(d);
                break;
            case CRACKING:
                tickCracking(server, d);
                break;
            case DEPARTING:
                tickDeparting(d);
                break;
            default:
                break;
        }
        if (d.isVisible() && d.clock % SYNC_INTERVAL == 0) {
            sync(d);
        }
        if (d.relic != null && d.clock % 20 == 0) {
            tickRelic(d);
        }
    }

    /** Once a minute, for each place someone is, a small chance it comes. */
    private void rollAppearance(List<EntityPlayerMP> players) {
        List<EntityPlayerMP> inSpace = new ArrayList<>();
        List<EntityPlayerMP> overworld = new ArrayList<>();
        for (EntityPlayerMP player : players) {
            if (player.isSpectator()) {
                continue;
            }
            if (player.dimension == EternityConfig.comet.spaceDimension) {
                inSpace.add(player);
            } else if (player.dimension == 0) {
                overworld.add(player);
            }
        }
        if (!inSpace.isEmpty() && rand.nextDouble() < 1D / (EternityConfig.comet.spaceMeanHours * 60D)) {
            appear(inSpace.get(rand.nextInt(inSpace.size())), true);
        } else if (!overworld.isEmpty() && rand.nextDouble() < 1D / (EternityConfig.comet.overworldMeanHours * 60D)) {
            appear(overworld.get(rand.nextInt(overworld.size())), false);
        }
    }

    /** Brings it into this player's sky, already on its way across. */
    public void appear(EntityPlayerMP player, boolean space) {
        CometData d = data();
        vanish();
        d.dimension = player.dimension;
        d.space = space;
        double heading = rand.nextDouble() * Math.PI * 2D;
        double hx = Math.cos(heading);
        double hz = Math.sin(heading);
        double back;
        double across;
        if (space) {
            back = 500D + rand.nextDouble() * 600D;
            across = (rand.nextDouble() - 0.5D) * 300D;
        } else {
            // Just inside the edge of what they can see, so the chase begins at once.
            double sight = sightRange(d);
            back = sight * 0.95D;
            across = (rand.nextDouble() - 0.5D) * sight * 0.6D;
        }
        d.x = player.posX - hx * back - hz * across;
        d.z = player.posZ - hz * back + hx * across;
        d.y = space ? EternityConfig.comet.spaceAltitude + (rand.nextDouble() - 0.5D) * 60D
                : EternityConfig.comet.overworldAltitude + (rand.nextDouble() - 0.5D) * 80D;
        double speed = (space ? EternityConfig.comet.spaceSpeed : EternityConfig.comet.overworldSpeed) / 20D;
        d.vx = hx * speed;
        d.vy = 0D;
        d.vz = hz * speed;
        d.phase = Phase.PASSING;
        d.stageTicks = 0;
        d.lostTicks = 0;
        d.markDirty();
        pace = 1D;
        EternityNetwork.sendToDimension(MsgComet.event(MsgComet.APPEAR, space, d.x, d.y, d.z, (float) radius(d)), d.dimension);
        sync(d);
    }

    private void tickPassing(CometData d) {
        d.stageTicks++;
        if (!EternityConfig.comet.enabled) {
            depart();
            return;
        }
        WorldServer world = DimensionManager.getWorld(d.dimension);
        double nearest = world == null ? Double.MAX_VALUE : nearestHorizontal(world, d.x, d.z);
        boolean engaged = world != null && nearestSurface(world, d) < heatRange(d);
        pace += ((engaged ? 0.12D : 1D) - pace) * 0.02D;
        lastDx = d.vx * pace;
        lastDy = d.vy * pace + Math.sin(d.stageTicks * 0.01D) * 0.03D * pace;
        lastDz = d.vz * pace;
        d.x += lastDx;
        d.y += lastDy;
        d.z += lastDz;

        // Let it slip beyond everyone's sight for too long and it is gone.
        d.lostTicks = nearest < sightRange(d) ? 0 : d.lostTicks + 1;
        if (d.lostTicks > EternityConfig.comet.escapeSeconds * 20) {
            depart();
            return;
        }
        if (d.space && !engaged && d.stageTicks > EternityConfig.comet.spaceVisitMinutes * TICKS_PER_MINUTE) {
            depart();
            return;
        }
        if (world != null) {
            updateBody(world, d, nearest);
            if (d.stageTicks % 10 == 0) {
                burn(world, d, 1F);
            }
        }
    }

    private void tickCracking(MinecraftServer server, CometData d) {
        d.stageTicks++;
        lastDx = lastDy = lastDz = 0D;
        WorldServer world = DimensionManager.getWorld(d.dimension);
        if (world != null) {
            updateBody(world, d, nearestHorizontal(world, d.x, d.z));
            if (d.stageTicks % 10 == 0) {
                burn(world, d, 1.5F);
            }
            if (d.stageTicks % 20 == 0) {
                world.playSound(null, d.x, d.y, d.z, EternitySounds.COMET_IMPACT, SoundCategory.HOSTILE, 10F,
                        0.4F + d.stageTicks / (float) CRACK_TICKS);
            }
        }
        if (d.stageTicks >= CRACK_TICKS) {
            shatter(server, d);
        }
    }

    /** It streaks away, faster and faster, climbing out of reach. */
    private void tickDeparting(CometData d) {
        d.stageTicks++;
        double horizontal = Math.sqrt(d.vx * d.vx + d.vz * d.vz);
        double hx = horizontal < 1.0E-6 ? 1D : d.vx / horizontal;
        double hz = horizontal < 1.0E-6 ? 0D : d.vz / horizontal;
        double speed = 0.5D + d.stageTicks * 0.08D;
        lastDx = hx * speed;
        lastDy = speed * 0.35D;
        lastDz = hz * speed;
        d.x += lastDx;
        d.y += lastDy;
        d.z += lastDz;
        if (d.stageTicks >= DEPART_TICKS) {
            d.phase = Phase.DORMANT;
            d.stageTicks = 0;
            d.markDirty();
            EternityNetwork.sendToDimension(MsgComet.hide(), d.dimension);
        }
    }

    /** It gets away. It may be seen again one day, wounds and all. */
    public void depart() {
        CometData d = data();
        if (d.phase != Phase.PASSING) {
            return;
        }
        vanish();
        d.phase = Phase.DEPARTING;
        d.stageTicks = 0;
        d.lostTicks = 0;
        d.markDirty();
        EternityNetwork.sendToDimension(MsgComet.event(MsgComet.DEPART, d.space, d.x, d.y, d.z, (float) radius(d)), d.dimension);
        sync(d);
    }

    // ------------------------------------------------------------------ the body

    /** Called by every comet entity each tick. Anything but the one true body dissolves. */
    public boolean claim(EntityCosmicComet entity) {
        return entity == body;
    }

    @Nullable
    public EntityCosmicComet body() {
        return body;
    }

    private void updateBody(WorldServer world, CometData d, double nearest) {
        if (body != null && (body.isDead || !body.isAddedToWorld())) {
            body = null;
        }
        double range = radius(d) + heatRange(d) + 48D;
        boolean loaded = world.isBlockLoaded(new BlockPos(d.x, 64, d.z));
        if (body != null && (body.world != world || !loaded || nearest > range + 48D)) {
            vanish();
        }
        float r = (float) hitRadius(d);
        if (body == null && loaded && nearest < range) {
            EntityCosmicComet entity = new EntityCosmicComet(world);
            entity.setRadius(r);
            entity.setLocationAndAngles(d.x, d.y - r, d.z, 0F, 0F);
            body = entity;
            world.spawnEntity(entity);
        }
        if (body != null) {
            if (body.getRadius() != r) {
                body.setRadius(r);
            }
            body.setPosition(d.x, d.y - r, d.z);
            body.setIntegrity(integrity(d));
        }
    }

    private void vanish() {
        if (body != null) {
            body.setDead();
            body = null;
        }
    }

    // ------------------------------------------------------------------ heat

    /** Everything near it burns. The closer, the faster; only absorption buys time. */
    private void burn(WorldServer world, CometData d, float multiplier) {
        Vec3d c = centre(d);
        double reach = radius(d) + heatRange(d);
        AxisAlignedBB box = new AxisAlignedBB(c.x - reach, c.y - reach, c.z - reach, c.x + reach, c.y + reach, c.z + reach);
        for (EntityLivingBase victim : world.getEntitiesWithinAABB(EntityLivingBase.class, box, CometDirector::burnable)) {
            double heat = heat(d, victim);
            if (heat <= 0D) {
                continue;
            }
            // Two pulses a second, each ignoring the usual moment of invulnerability.
            victim.hurtResistantTime = 0;
            victim.attackEntityFrom(EternityDamage.cometHeat(), heatPerSecond(d, victim) * (float) (heat * heat) * multiplier / 2F);
            if (heat > 0.3D) {
                victim.setFire(3);
            }
        }
    }

    /** How fiercely it burns at this entity's distance: 0 at the edge of its heat, 1 at its surface. */
    private double heat(CometData d, Entity entity) {
        double surface = entityCentre(entity).distanceTo(centre(d)) - radius(d);
        return MathHelper.clamp(1D - surface / heatRange(d), 0D, 1D);
    }

    private static float heatPerSecond(CometData d, EntityLivingBase victim) {
        double scale = d.space ? 1D : EternityConfig.comet.overworldHeatScale;
        return (float) ((EternityConfig.comet.heatPerSecond + EternityConfig.comet.heatMaxHealthPerSecond * victim.getMaxHealth()) * scale);
    }

    /** Creative and spectating players watch; everything else burns. */
    private static boolean burnable(@Nullable EntityLivingBase entity) {
        if (entity == null || entity instanceof EntityCosmicComet || !entity.isEntityAlive()) {
            return false;
        }
        return !(entity instanceof EntityPlayer) || !((EntityPlayer) entity).isSpectator() && !((EntityPlayer) entity).isCreative();
    }

    // ------------------------------------------------------------------ blows

    /**
     * Every blow against the body comes here. Only blows struck from close to its
     * surface count; anything from farther away burns up before it arrives.
     */
    public boolean strike(EntityCosmicComet entity, DamageSource source, float amount) {
        CometData d = data();
        if (entity != body || d.phase != Phase.PASSING) {
            return false;
        }
        Entity attacker = source.getTrueSource();
        if (!(attacker instanceof EntityLivingBase) || attacker.world != entity.world
                || attacker instanceof EntityPlayer && ((EntityPlayer) attacker).isSpectator()) {
            return false;
        }
        World world = entity.world;
        Vec3d c = centre(d);
        double r = radius(d);
        Vec3d from = attacker.getPositionEyes(1F);
        Vec3d out = from.subtract(c);
        out = out.lengthSquared() < 1.0E-6 ? new Vec3d(0, 1, 0) : out.normalize();
        Vec3d hit = c.add(out.scale(r));
        if (from.distanceTo(c) - hitRadius(d) > strikeReach(d)) {
            cue(d, MsgComet.REPELLED, hit, 0F);
            world.playSound(null, hit.x, hit.y, hit.z, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.HOSTILE, 3F, 0.6F);
            return false;
        }
        double max = EternityConfig.comet.health;
        float dealt = (float) Math.min(amount, max * EternityConfig.comet.maxHitFraction);
        if (!(dealt > 0F)) {
            return false;
        }
        d.damage = (float) Math.min(max, d.damage + dealt);
        if (attacker instanceof EntityPlayerMP) {
            d.breaker = attacker.getName();
        }
        d.markDirty();
        entity.setIntegrity(integrity(d));
        cue(d, MsgComet.HIT, hit, (float) r);
        if (d.damage >= max) {
            beginCracking(d);
            return true;
        }
        int tenths = (int) (d.damage / max * 10D);
        if (tenths > d.flares) {
            d.flares = tenths;
            flare(world, d);
        }
        return true;
    }

    /**
     * A player swung at the comet (see {@link com.matoon.eternity.network.MsgStrikeComet}).
     * Swing for them, with their own weapon, enchantments and cooldown.
     */
    public void melee(EntityPlayerMP player, int entityId) {
        if (body == null || body.getEntityId() != entityId || body.world != player.world
                || player.isSpectator() || data().phase != Phase.PASSING) {
            return;
        }
        if (player.getPositionEyes(1F).distanceTo(body.centre()) - body.getRadius() > strikeReach(data())) {
            return;
        }
        striking = true;
        try {
            player.attackTargetEntityWithCurrentItem(body);
        } finally {
            striking = false;
        }
    }

    /** Blows reach the comet only through {@link #melee}, so vanilla's own swing must not land twice. */
    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        if (event.getTarget() instanceof EntityCosmicComet && !event.getEntityPlayer().world.isRemote && !striking) {
            event.setCanceled(true);
        }
    }

    /** Every tenth of its health it loses, it answers with a blast of heat that hurls everyone back. */
    private void flare(World world, CometData d) {
        Vec3d c = centre(d);
        double reach = radius(d) + heatRange(d) * 0.5D;
        AxisAlignedBB box = new AxisAlignedBB(c.x - reach, c.y - reach, c.z - reach, c.x + reach, c.y + reach, c.z + reach);
        for (EntityLivingBase victim : world.getEntitiesWithinAABB(EntityLivingBase.class, box,
                e -> e != null && !(e instanceof EntityCosmicComet) && !(e instanceof EntityPlayer && ((EntityPlayer) e).isSpectator()))) {
            Vec3d off = entityCentre(victim).subtract(c);
            double dist = off.length();
            if (dist > reach) {
                continue;
            }
            double k = 1D - dist / reach;
            if (burnable(victim)) {
                victim.hurtResistantTime = 0;
                victim.attackEntityFrom(EternityDamage.cometHeat(), heatPerSecond(d, victim) * (float) k);
            }
            Vec3d push = dist < 1.0E-3 ? new Vec3d(0, 1, 0) : off.scale(1D / dist);
            double strength = 1.5D + 3D * k;
            victim.motionX += push.x * strength;
            victim.motionY += push.y * strength + 0.4D * k;
            victim.motionZ += push.z * strength;
            victim.velocityChanged = true;
        }
        EternityNetwork.sendToDimension(MsgComet.event(MsgComet.FLARE, d.space, c.x, c.y, c.z, (float) reach), d.dimension);
    }

    // ------------------------------------------------------------------ breaking open

    private void beginCracking(CometData d) {
        d.phase = Phase.CRACKING;
        d.stageTicks = 0;
        d.markDirty();
        WorldServer world = DimensionManager.getWorld(d.dimension);
        if (world != null) {
            world.playSound(null, d.x, d.y, d.z, SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.HOSTILE, 24F, 0.5F);
            world.playSound(null, d.x, d.y, d.z, EternitySounds.RIFT_OPEN, SoundCategory.HOSTILE, 16F, 0.5F);
        }
        EternityNetwork.sendToDimension(MsgComet.event(MsgComet.CRACK, d.space, d.x, d.y, d.z, (float) radius(d)), d.dimension);
        sync(d);
    }

    /** It bursts apart, and in the heart of it hangs the Astral Regulator. */
    private void shatter(MinecraftServer server, CometData d) {
        WorldServer world = server.getWorld(d.dimension);
        Vec3d c = centre(d);
        double r = radius(d);
        double blast = r + heatRange(d) * 0.6D;
        vanish();
        AxisAlignedBB box = new AxisAlignedBB(c.x - blast, c.y - blast, c.z - blast, c.x + blast, c.y + blast, c.z + blast);
        for (EntityLivingBase victim : world.getEntitiesWithinAABB(EntityLivingBase.class, box,
                e -> e != null && !(e instanceof EntityPlayer && ((EntityPlayer) e).isSpectator()))) {
            Vec3d off = entityCentre(victim).subtract(c);
            double dist = off.length();
            double k = 1D - dist / blast;
            if (k <= 0D) {
                continue;
            }
            if (burnable(victim)) {
                victim.hurtResistantTime = 0;
                victim.attackEntityFrom(EternityDamage.cometBlast(), (float) ((victim.getMaxHealth() * 0.6D + 20D) * k));
            }
            Vec3d push = dist < 1.0E-3 ? new Vec3d(0, 1, 0) : off.scale(1D / dist);
            double strength = 2D + 5D * k;
            victim.motionX += push.x * strength;
            victim.motionY += push.y * strength + 0.6D * k;
            victim.motionZ += push.z * strength;
            victim.velocityChanged = true;
        }
        Entity relic = reveal(world, c);
        d.relic = relic.getUniqueID();
        d.relicDim = d.dimension;
        d.relicX = c.x;
        d.relicY = c.y;
        d.relicZ = c.z;

        d.phase = Phase.SHATTERED;
        d.stageTicks = 0;
        d.markDirty();
        EternityNetwork.sendToDimension(MsgComet.event(MsgComet.SHATTER, d.space, c.x, c.y, c.z, (float) r), d.dimension);

        TextComponentTranslation text = new TextComponentTranslation("eternity.comet.shattered", d.breaker.isEmpty() ? "Someone" : d.breaker);
        text.getStyle().setColor(TextFormatting.LIGHT_PURPLE);
        server.getPlayerList().sendMessage(text);
        EntityPlayerMP breaker = server.getPlayerList().getPlayerByUsername(d.breaker);
        if (breaker != null) {
            Advancements.grant(breaker, Advancements.ROOT);
            Advancements.grant(breaker, Advancements.COMET);
        }
    }

    /** The Regulator hangs where the heart of the comet was, waiting to be taken. */
    private static Entity reveal(WorldServer world, Vec3d c) {
        // Whoever broke it may already be fleeing the blast; the chunk must still take the Regulator.
        world.getChunk(new BlockPos(c.x, 64, c.z));
        ItemStack stack = new ItemStack(EternityItems.ASTRAL_REGULATOR);
        EntityItem item = new EntityItem(world, c.x, c.y, c.z, stack);
        Entity revealed = item;
        if (stack.getItem().hasCustomEntity(stack)) {
            Entity custom = stack.getItem().createEntity(world, item, stack);
            if (custom != null) {
                revealed = custom;
            }
        }
        if (revealed instanceof EntityItem) {
            ((EntityItem) revealed).setNoDespawn();
        }
        revealed.setNoGravity(true);
        revealed.setGlowing(true);
        revealed.motionX = revealed.motionY = revealed.motionZ = 0D;
        revealed.setPosition(c.x, c.y, c.z);
        world.spawnEntity(revealed);
        return revealed;
    }

    /**
     * Keeps every client in its dimension told where the Regulator hangs, so they can
     * see its light from any distance. It is forgotten once its chunk is loaded and
     * it is no longer there: someone took it.
     */
    private void tickRelic(CometData d) {
        WorldServer world = DimensionManager.getWorld(d.relicDim);
        if (world != null) {
            Entity entity = world.getEntityFromUuid(d.relic);
            if (entity != null && !entity.isDead) {
                d.relicX = entity.posX;
                d.relicY = entity.posY + entity.height / 2D;
                d.relicZ = entity.posZ;
            } else if (world.isBlockLoaded(new BlockPos(d.relicX, 64, d.relicZ))) {
                d.relic = null;
                d.markDirty();
                EternityNetwork.sendToDimension(MsgComet.event(MsgComet.RELIC_GONE, false, 0, 0, 0, 0F), d.relicDim);
                return;
            }
        }
        if (d.clock % 40 == 0) {
            EternityNetwork.sendToDimension(relicMessage(d), d.relicDim);
        }
    }

    private static MsgComet relicMessage(CometData d) {
        return MsgComet.event(MsgComet.RELIC, false, d.relicX, d.relicY, d.relicZ, 0F);
    }

    // ------------------------------------------------------------------ operator tools

    /** Wounds it by a share of its health, as though struck. */
    public void wound(double fraction, String by) {
        CometData d = data();
        double max = EternityConfig.comet.health;
        d.damage = (float) Math.min(max, d.damage + max * fraction);
        d.flares = (int) (d.damage / max * 10D);
        d.breaker = by;
        d.markDirty();
        if (d.phase == Phase.PASSING && d.damage >= max) {
            beginCracking(d);
        }
    }

    /** Forgets everything, even being broken open. */
    public void restore() {
        CometData d = data();
        vanish();
        d.phase = Phase.DORMANT;
        d.damage = 0F;
        d.flares = 0;
        d.stageTicks = 0;
        d.lostTicks = 0;
        d.breaker = "";
        d.relic = null;
        d.markDirty();
        EternityNetwork.sendToAll(MsgComet.hide());
        EternityNetwork.sendToAll(MsgComet.event(MsgComet.RELIC_GONE, false, 0, 0, 0, 0F));
    }

    // ------------------------------------------------------------------ measurements

    public Vec3d centre(CometData d) {
        return new Vec3d(d.x, d.y, d.z);
    }

    public double radius(CometData d) {
        return EternityConfig.comet.radius;
    }

    /** Generous: the box you can strike reaches far past the rock itself. */
    public double hitRadius(CometData d) {
        return radius(d) * 2.2D + 5D;
    }

    public double heatRange(CometData d) {
        return d.space ? EternityConfig.comet.spaceHeatRange : EternityConfig.comet.overworldHeatRange;
    }

    public float integrity(CometData d) {
        return MathHelper.clamp(1F - d.damage / (float) EternityConfig.comet.health, 0F, 1F);
    }

    private static double strikeReach(CometData d) {
        return EternityConfig.comet.strikeReach;
    }

    /**
     * How far away someone can be and still keep it in sight: their render distance,
     * or in space, as far as its light carries.
     */
    public double sightRange(CometData d) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        double view = server == null ? 160D : Math.max(64D, server.getPlayerList().getViewDistance() * 16D);
        return d.space ? Math.max(view, EternityConfig.comet.spaceSightRange) : view;
    }

    private static double nearestHorizontal(World world, double x, double z) {
        double best = Double.MAX_VALUE;
        for (EntityPlayer player : world.playerEntities) {
            if (!player.isSpectator()) {
                double dx = player.posX - x;
                double dz = player.posZ - z;
                best = Math.min(best, Math.sqrt(dx * dx + dz * dz));
            }
        }
        return best;
    }

    private double nearestSurface(World world, CometData d) {
        Vec3d c = centre(d);
        double best = Double.MAX_VALUE;
        for (EntityPlayer player : world.playerEntities) {
            if (!player.isSpectator()) {
                best = Math.min(best, entityCentre(player).distanceTo(c) - radius(d));
            }
        }
        return best;
    }

    private static Vec3d entityCentre(Entity entity) {
        return new Vec3d(entity.posX, entity.posY + entity.height / 2D, entity.posZ);
    }

    // ------------------------------------------------------------------ clients

    private void sync(CometData d) {
        byte stage = d.phase == Phase.CRACKING ? MsgComet.CRACKING : d.phase == Phase.DEPARTING ? MsgComet.DEPARTING : MsgComet.PASSING;
        EternityNetwork.sendToDimension(MsgComet.sync(stage, d.space, d.x, d.y, d.z, lastDx, lastDy, lastDz,
                (float) radius(d), integrity(d)), d.dimension);
    }

    private void cue(CometData d, byte type, Vec3d at, float radius) {
        EternityNetwork.sendAround(MsgComet.event(type, d.space, at.x, at.y, at.z, radius), d.dimension, at.x, at.y, at.z, 512D);
    }

    private void syncFor(EntityPlayerMP player) {
        CometData d = data();
        if (d.isVisible() && player.dimension == d.dimension) {
            byte stage = d.phase == Phase.CRACKING ? MsgComet.CRACKING : d.phase == Phase.DEPARTING ? MsgComet.DEPARTING : MsgComet.PASSING;
            EternityNetwork.send(MsgComet.sync(stage, d.space, d.x, d.y, d.z, lastDx, lastDy, lastDz,
                    (float) radius(d), integrity(d)), player);
        } else {
            EternityNetwork.send(MsgComet.hide(), player);
        }
        EternityNetwork.send(d.relic != null && player.dimension == d.relicDim ? relicMessage(d)
                : MsgComet.event(MsgComet.RELIC_GONE, false, 0, 0, 0, 0F), player);
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            syncFor((EntityPlayerMP) event.player);
        }
    }

    @SubscribeEvent
    public void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            syncFor((EntityPlayerMP) event.player);
        }
    }
}
