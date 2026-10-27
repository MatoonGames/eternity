package com.matoon.eternity.world;

import com.matoon.eternity.Eternity;
import com.matoon.eternity.EternityConfig;
import com.matoon.eternity.EternitySounds;
import com.matoon.eternity.compat.Relics;
import com.matoon.eternity.compat.galacticraft.GcRockets;
import com.matoon.eternity.entity.EntityChampion;
import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.network.MsgTribunalFx;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.EntityHusk;
import net.minecraft.entity.monster.EntityWitherSkeleton;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * The Champion of the Universe and his planet. There is one of him per world, and his story
 * happens once.
 * <ul>
 * <li>While anyone is on Tamarata near his Colosseum he is there in it, fighting: the last
 * warriors of Tamarata still come for him in waves, and he demolishes them.</li>
 * <li>Come close and he finishes them and calls you out. Then it is a duel, and you cannot win
 * it with your fists; only by making him so angry that he loses himself.</li>
 * <li>At the top of his rage he drives both fists into Tamarata, and it breaks apart.</li>
 * <li>Stranded in the wreck, unable to move, he bargains: the Power Stone for a rocket.</li>
 * <li>Bring him one and he climbs in and is gone, for good. The stone is yours.</li>
 * </ul>
 */
public final class ChampionDirector {

    public static final ChampionDirector INSTANCE = new ChampionDirector();

    /** The tag on the armies that come for him, so he and this director know them. */
    public static final String ARMY = "eternity_tamaratan";
    /** The tag on the rocket he leaves in, so nobody else climbs into it, and a stray one left in a save is removed. */
    public static final String ROCKET = "eternity_champion_rocket";

    /** He is only there at all while someone is this close. */
    private static final double PRESENCE_RANGE = 170D;
    /** The armies start coming once someone is this close... */
    private static final double ARMY_RANGE = 110D;
    /** ...and he calls you out once you are this close, or the armies are done and you are within sight. */
    private static final double CHALLENGE_RANGE = 26D, SIGHT_RANGE = 70D;
    /** Walk farther than this from him and the duel is over. */
    private static final double DUEL_RANGE = 130D;
    private static final int WAVES = 3;

    private static final String[] STRANDED = {
            "...Well. That was my planet.",
            "Every arena I ever built. Every warrior who fell in them. I should have pulled that punch.",
            "No ground to push off. I never needed to fly. There was always somewhere to stand.",
            "You. You did this on purpose. Made me miss, and miss, until I swung at the world instead.",
            "Clever. I'll give you that. Hollow way to win, but clever.",
            "You have a ship. Bring me a rocket and the stone in my brow is yours. Never knew what it was for anyway."};
    private static final String[] STRANDED_EMPTY = {
            "...Well. That was my planet.",
            "No ground to push off. I never needed to fly. There was always somewhere to stand.",
            "You did this on purpose. Clever. Hollow way to win, but clever.",
            "Bring me a rocket. I'll owe you, and I've never left a debt unpaid."};

    private final Random rand = new Random();
    private ChampionData data;
    @Nullable
    private EntityChampion body;
    private final List<EntityLiving> army = new ArrayList<>();
    private int wavesLeft;
    private int nextWave;
    private boolean visiting;
    private int strandedLine;
    private int nextLine;
    /** The trade, once a rocket is handed over: the rocket, who gave it, and how far along it is. */
    @Nullable
    private Entity rocket;
    @Nullable
    private UUID trader;
    private int tradeTicks = -1;
    private boolean tradeForReal;
    private Vec3d rocketAt;
    private double rocketSpeed;
    private int tick;
    /** Whether the breaking wave has been set going this session (after a restart, it picks up where it was). */
    private boolean waveRunning;

    private ChampionDirector() {
    }

    public void reset() {
        data = null;
        body = null;
        army.clear();
        wavesLeft = 0;
        visiting = false;
        rocket = null;
        trader = null;
        tradeTicks = -1;
        waveRunning = false;
        TamarataBreaker.INSTANCE.reset();
    }

    public ChampionData data() {
        if (data == null) {
            data = ChampionData.get();
        }
        return data;
    }

    /** Whether Tamarata is broken. False before any world is loaded. */
    public boolean planetBroken() {
        return DimensionManager.getWorld(0) != null && data().planetDestroyed;
    }

    /** Whether the Power Stone is still in his brow. There is only one, so not if it is anywhere else. */
    public boolean wearsStone() {
        if (DimensionManager.getWorld(0) == null) {
            return false;
        }
        ChampionData d = data();
        return EternityConfig.champion.enabled && EternityConfig.champion.holdsPowerStone && !d.stoneTaken
                && d.phase != ChampionData.Phase.DEPARTED && anvil.infinity.items.Items.POWER_STONE != null
                && !RelicTracker.INSTANCE.powerElsewhere();
    }

    /** Where the stone in his brow is, for the Relic Tracker: on him, or where he will be. Null if he does not wear it. */
    @Nullable
    public Vec3d stonePosition() {
        if (!wearsStone()) {
            return null;
        }
        if (body != null && !body.isDead) {
            return body.brow();
        }
        return planetBroken() ? driftPoint() : new Vec3d(Tamarata.ARENA_X + 0.5D, Tamarata.ARENA_Y + 3D, Tamarata.ARENA_Z + 0.5D);
    }

    @Nullable
    public EntityChampion body() {
        return body;
    }

    /** Called by every managed Champion each tick; any but the one this director moves is gone. */
    public boolean claim(EntityChampion entity) {
        return entity == body;
    }

    public static boolean isArmy(@Nullable Entity entity) {
        return entity != null && entity.getTags().contains(ARMY);
    }

    private Vec3d driftPoint() {
        ChampionData d = data();
        return new Vec3d(d.breakX, d.breakY + 6D, d.breakZ);
    }

    private static Vec3d arena() {
        return new Vec3d(Tamarata.ARENA_X + 0.5D, Tamarata.ARENA_Y, Tamarata.ARENA_Z + 0.5D);
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
        tick++;
        WorldServer world = DimensionManager.getWorld(Tamarata.dimension());
        ChampionData d = data();
        if (world == null) {
            if (tradeTicks < 0) {
                body = null;
            }
            return;
        }
        if (d.planetDestroyed) {
            if (!waveRunning) {
                waveRunning = true;
                if (d.phase == ChampionData.Phase.BREAKING) {
                    TamarataBreaker.INSTANCE.begin(d.breakX, d.breakZ, d.breakTicks);
                }
            }
            if (TamarataBreaker.INSTANCE.busy()) {
                TamarataBreaker.INSTANCE.tick(world, d.breakTicks);
            }
            keepSafe(world);
        }
        if (tradeTicks >= 0) {
            trading(world);
            return;
        }
        if (!EternityConfig.champion.enabled || d.phase == ChampionData.Phase.DEPARTED) {
            vanish();
            return;
        }
        switch (d.phase) {
            case RESIDENT:
            case DUEL:
                resident(world);
                break;
            case BREAKING:
                breaking(world);
                break;
            case STRANDED:
                stranded(world);
                break;
            default:
                break;
        }
    }

    /** Everyone on Tamarata within reach of where he is. */
    private List<EntityPlayerMP> near(WorldServer world, Vec3d at, double range) {
        List<EntityPlayerMP> found = new ArrayList<>();
        for (EntityPlayer player : world.playerEntities) {
            if (player instanceof EntityPlayerMP && !player.isSpectator() && player.isEntityAlive()) {
                double dx = player.posX - at.x, dz = player.posZ - at.z;
                if (Math.sqrt(dx * dx + dz * dz) < range) {
                    found.add((EntityPlayerMP) player);
                }
            }
        }
        return found;
    }

    @Nullable
    private EntityChampion summon(WorldServer world, Vec3d at) {
        if (body != null && (body.isDead || body.world != world)) {
            body = null;
        }
        if (body == null) {
            EntityChampion entity = new EntityChampion(world);
            entity.setManaged(true);
            entity.setLocationAndAngles(at.x, at.y, at.z, 270F, 0F);
            body = entity;
            if (!world.spawnEntity(entity)) {
                body = null;
                return null;
            }
        }
        body.setStone(wearsStone());
        return body;
    }

    private void vanish() {
        if (body != null) {
            body.setDead();
            body = null;
        }
        clearArmy();
        visiting = false;
    }

    // ------------------------------------------------------------------ in his arena

    private void resident(WorldServer world) {
        ChampionData d = data();
        List<EntityPlayerMP> near = near(world, arena(), PRESENCE_RANGE);
        if (near.isEmpty()) {
            if (d.phase == ChampionData.Phase.DUEL) {
                endDuel();
            }
            vanish();
            return;
        }
        EntityChampion champion = summon(world, arena());
        if (champion == null) {
            return;
        }
        if (d.phase == ChampionData.Phase.DUEL) {
            // Once he has leapt for Planet Breaker he is committed, whoever is still watching.
            boolean committed = champion.getState() == EntityChampion.State.BREAKER || champion.getState() == EntityChampion.State.IMPACT;
            EntityPlayer duel = champion.getDuel() == null ? null : world.getPlayerEntityByUUID(champion.getDuel());
            if (!committed && (duel == null || !duel.isEntityAlive() || horizontal(duel, champion) > DUEL_RANGE)) {
                endDuel();
            }
            return;
        }
        // Resident: the armies, and whoever comes close enough to be called out.
        EntityPlayerMP closest = null;
        double best = Double.MAX_VALUE;
        for (EntityPlayerMP player : near) {
            double dist = player.getDistance(champion);
            if (dist < best) {
                best = dist;
                closest = player;
            }
        }
        if (!visiting && best < ARMY_RANGE) {
            visiting = true;
            wavesLeft = WAVES;
            nextWave = tick + 40;
            for (EntityPlayerMP player : near) {
                status(player, "Ahead, in the ruined Colosseum, something enormous is fighting an army.", TextFormatting.GRAY);
            }
        }
        army.removeIf(e -> e.isDead || e.getHealth() <= 0F);
        for (EntityLiving soldier : army) {
            soldier.setAttackTarget(champion);
        }
        if (visiting && army.isEmpty() && wavesLeft > 0 && tick >= nextWave) {
            wave(world, champion);
            wavesLeft--;
            nextWave = tick + 160;
        }
        if (closest != null && (best < CHALLENGE_RANGE || visiting && wavesLeft == 0 && army.isEmpty() && best < SIGHT_RANGE)) {
            if (!army.isEmpty()) {
                // He clears the floor for you with a single roar.
                champion.order(EntityChampion.Move.ROAR);
            }
            beginDuel(champion, closest);
        }
    }

    /** The last warriors of Tamarata, dead or as good as, pouring through his gates. */
    private void wave(WorldServer world, EntityChampion champion) {
        int count = 5 + rand.nextInt(4);
        champion.say(rand.nextBoolean() ? "Still coming. Tamarata never did learn." : "Line them up. Same as last age.");
        for (int i = 0; i < count; i++) {
            int gate = rand.nextInt(4);
            double a = gate * Math.PI / 2D + (rand.nextDouble() - 0.5D) * 0.12D;
            double r = Tamarata.ARENA_RADIUS - 2D - rand.nextDouble() * 3D;
            double x = Tamarata.ARENA_X + 0.5D + Math.cos(a) * r, z = Tamarata.ARENA_Z + 0.5D + Math.sin(a) * r;
            EntityLiving soldier = rand.nextInt(3) == 0 ? new EntityWitherSkeleton(world) : new EntityHusk(world);
            soldier.setLocationAndAngles(x, Tamarata.ARENA_Y, z, (float) Math.toDegrees(a) + 90F, 0F);
            soldier.targetTasks.taskEntries.clear();
            soldier.addTag(ARMY);
            arm(soldier);
            soldier.setDropChance(EntityEquipmentSlot.HEAD, 0F);
            soldier.setDropChance(EntityEquipmentSlot.CHEST, 0F);
            soldier.setDropChance(EntityEquipmentSlot.MAINHAND, 0F);
            soldier.setAttackTarget(champion);
            if (world.spawnEntity(soldier)) {
                army.add(soldier);
            }
        }
        sound(world, arena(), EternitySounds.FAR_HORN, 4F, 0.8F);
    }

    private void arm(EntityLiving soldier) {
        boolean gold = rand.nextBoolean();
        soldier.setItemStackToSlot(EntityEquipmentSlot.HEAD, new ItemStack(gold ? Items.GOLDEN_HELMET : Items.CHAINMAIL_HELMET));
        soldier.setItemStackToSlot(EntityEquipmentSlot.CHEST, new ItemStack(gold ? Items.GOLDEN_CHESTPLATE : Items.CHAINMAIL_CHESTPLATE));
        Item weapon = rand.nextInt(3) == 0 ? Items.GOLDEN_AXE : rand.nextBoolean() ? Items.IRON_SWORD : Items.GOLDEN_SWORD;
        soldier.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(weapon));
    }

    private void clearArmy() {
        for (EntityLiving soldier : army) {
            soldier.setDead();
        }
        army.clear();
    }

    private void beginDuel(EntityChampion champion, EntityPlayerMP player) {
        ChampionData d = data();
        d.phase = ChampionData.Phase.DUEL;
        d.markDirty();
        // He remembers how badly you made him lose his temper last time, a little.
        champion.setRage(d.rage(player.getUniqueID()) * 0.5F);
        champion.challenge(player);
    }

    private void endDuel() {
        ChampionData d = data();
        if (d.phase == ChampionData.Phase.DUEL) {
            d.phase = ChampionData.Phase.RESIDENT;
            d.markDirty();
        }
        if (body != null) {
            body.endDuel();
        }
        visiting = false;
    }

    /** Someone has hit him before he called them out. That will do instead. */
    public void provoked(EntityChampion champion, EntityPlayer player) {
        if (champion != body || !(player instanceof EntityPlayerMP) || data().phase != ChampionData.Phase.RESIDENT) {
            return;
        }
        beginDuel(champion, (EntityPlayerMP) player);
        champion.say("From behind. Turn around, so I can see who that was.");
    }

    public void rememberRage(EntityChampion champion, float rage) {
        if (champion != body || champion.getDuel() == null || tick % 20 != 0) {
            return;
        }
        data().rage.put(champion.getDuel(), rage);
        data().markDirty();
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        if (body != null && event.getEntityLiving() instanceof EntityPlayerMP && event.getEntityLiving().getUniqueID().equals(body.getDuel())) {
            ChampionData d = data();
            d.respect.merge(event.getEntityLiving().getUniqueID(), 1, Integer::sum);
            d.markDirty();
        }
    }

    // ------------------------------------------------------------------ Planet Breaker

    /** His fists have come down on Tamarata with everything he has. It breaks. */
    public void planetBreaker(EntityChampion champion) {
        ChampionData d = data();
        if (champion != body || d.phase != ChampionData.Phase.DUEL || champion.dimension != Tamarata.dimension()) {
            champion.setRage(0.55F);
            return;
        }
        breakPlanet(champion.world, champion.posX, champion.posY, champion.posZ);
        champion.holdImpact();
        d.phase = ChampionData.Phase.BREAKING;
        d.markDirty();
        clearArmy();
    }

    /** Sets the breaking going from a point, and tells everyone on Tamarata. For the command, too. */
    public void breakPlanet(net.minecraft.world.World world, double x, double y, double z) {
        ChampionData d = data();
        d.planetDestroyed = true;
        d.breakTicks = 0;
        d.breakX = x;
        d.breakY = y;
        d.breakZ = z;
        d.markDirty();
        TamarataBreaker.INSTANCE.begin(x, z, 0);
        waveRunning = true;
        // The ground goes, not the sky: a fractured sky belongs to the end of the universe alone.
        EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.CHAMPION_WORLD, x, y, z, 0, 0, 0, 1, 0), Tamarata.dimension());
        TextComponentString notice = new TextComponentString("Far away, a planet called Tamarata breaks apart.");
        notice.getStyle().setColor(TextFormatting.DARK_PURPLE).setItalic(true);
        FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().sendMessage(notice);
        Eternity.LOG.info("The Champion of the Universe has broken Tamarata.");
    }

    private void breaking(WorldServer world) {
        ChampionData d = data();
        d.breakTicks++;
        if (d.breakTicks % 100 == 0) {
            d.markDirty();
        }
        EntityChampion champion = summon(world, new Vec3d(d.breakX, d.breakY, d.breakZ));
        if (champion == null) {
            return;
        }
        champion.endDuel();
        if (d.breakTicks == 70) {
            champion.say("That wasn't the floor cracking. That was the world.");
        }
        if (d.breakTicks == 160) {
            champion.say("Ah. I hit it too hard.");
        }
        boolean inCrater = champion.getState() == EntityChampion.State.IMPACT;
        if (champion.getState() != EntityChampion.State.DRIFT && (!inCrater || d.breakTicks > 60)) {
            // The ground has gone out from under him (or he is back after everyone left, and there was none).
            champion.drift(new Vec3d(champion.posX, inCrater ? champion.posY + 0.5D : d.breakY + 6D, champion.posZ));
        }
        if (d.breakTicks >= TamarataBreaker.WAVE_TICKS + 60 && TamarataBreaker.INSTANCE.done()) {
            d.phase = ChampionData.Phase.STRANDED;
            d.markDirty();
            strandedLine = 0;
            nextLine = tick + 40;
        }
    }

    // ------------------------------------------------------------------ stranded

    private void stranded(WorldServer world) {
        Vec3d at = driftPoint();
        List<EntityPlayerMP> near = near(world, at, PRESENCE_RANGE);
        if (near.isEmpty()) {
            if (body != null) {
                body.setDead();
                body = null;
            }
            return;
        }
        EntityChampion champion = summon(world, at);
        if (champion == null) {
            return;
        }
        champion.endDuel();
        champion.drift(at);
        // He talks to whoever floats close enough to listen: fury first, then bargaining.
        boolean listening = false;
        for (EntityPlayerMP player : near) {
            listening |= player.getDistance(champion) < 64D;
        }
        if (listening && tick >= nextLine) {
            String[] lines = wearsStone() ? STRANDED : STRANDED_EMPTY;
            champion.say(lines[Math.min(strandedLine, lines.length - 1)]);
            strandedLine = strandedLine < lines.length - 1 ? strandedLine + 1 : lines.length - 2 + (strandedLine + 1) % 2;
            nextLine = tick + 160 + rand.nextInt(80);
        }
    }

    // ------------------------------------------------------------------ touching him, and the trade

    /**
     * Someone right-clicks him. Stranded, a rocket in hand is the trade he has been begging
     * for. In a fight, an empty hand to the face is an insult he will not forget.
     */
    public boolean touched(EntityChampion champion, EntityPlayerMP player) {
        if (player.isSpectator() || tradeTicks >= 0) {
            return false;
        }
        ItemStack held = player.getHeldItemMainhand();
        boolean stranded = champion.getState() == EntityChampion.State.DRIFT;
        boolean real = champion == body && data().phase == ChampionData.Phase.STRANDED;
        if (GcRockets.tier(held) > 0 && (stranded || !champion.isManaged())) {
            trade(champion, player, held, real);
            return true;
        }
        if (stranded) {
            champion.say("A rocket. Your hands are empty.");
            return true;
        }
        if (held.isEmpty() && champion.getDuel() != null && player.getUniqueID().equals(champion.getDuel())) {
            // A slap. In front of everyone.
            champion.taunted(0.06F);
            player.world.playSound(null, champion.posX, champion.posY + 2D, champion.posZ, SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK,
                    SoundCategory.PLAYERS, 1.5F, 1.4F);
            return true;
        }
        return false;
    }

    private void trade(EntityChampion champion, EntityPlayerMP player, ItemStack held, boolean real) {
        Vec3d f = champion.facing();
        Vec3d side = new Vec3d(-f.z, 0D, f.x);
        rocketAt = champion.getPositionVector().add(side.scale(4D)).add(f.scale(1D));
        Entity ship = GcRockets.rocket(champion.world, held, rocketAt.x, rocketAt.y, rocketAt.z);
        if (ship == null) {
            return;
        }
        ship.addTag(ROCKET);
        ship.rotationYaw = champion.rotationYaw;
        // Known as his before it joins the world, or the guard against stray rockets turns it away.
        rocket = ship;
        if (!champion.world.spawnEntity(ship)) {
            rocket = null;
            return;
        }
        if (!player.capabilities.isCreativeMode) {
            held.shrink(1);
            player.inventoryContainer.detectAndSendChanges();
        }
        trader = player.getUniqueID();
        tradeTicks = 0;
        tradeForReal = real;
        rocketSpeed = 0D;
        body = champion;
        fx(champion.world, MsgTribunalFx.CHAMPION_EXHAUST, rocketAt.x, rocketAt.y, rocketAt.z, 0, 0, 0, ship.getEntityId(), 80);
        EntityChampion.speak(champion.world, champion, "A ship. You kept your word. Most don't.", 128D);
    }

    /** The stone for the ship; then he climbs in, and is gone. */
    private void trading(WorldServer world) {
        tradeTicks++;
        EntityChampion champion = body;
        EntityPlayerMP player = trader == null ? null : FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayerByUUID(trader);
        if (rocket != null) {
            rocket.motionX = rocket.motionY = rocket.motionZ = 0D;
            rocket.setPositionAndRotation(rocketAt.x, rocketAt.y, rocketAt.z, rocket.rotationYaw, 0F);
            rocket.fallDistance = 0F;
        }
        if (champion != null && !champion.isDead && tradeTicks < 80) {
            if (tradeTicks == 18 && champion.wearsStone() && player != null) {
                EntityChampion.speak(world, champion, "Here. It burned hot whenever I was angry. Maybe it likes you better.", 128D);
                Vec3d brow = champion.brow();
                fx(world, MsgTribunalFx.CHAMPION_STONE, brow.x, brow.y, brow.z, 0, 0, 0, player.getEntityId(), 24);
            }
            if (tradeTicks == 36 && champion.wearsStone()) {
                champion.setStone(false);
                if (tradeForReal && player != null) {
                    giveStone(player);
                }
            }
            if (tradeTicks == 46) {
                champion.board();
            }
            if (tradeTicks > 46) {
                // He hauls himself across to it, hand over hand through nothing.
                double p = (tradeTicks - 46) / 34D;
                p = p * p * (3D - 2D * p);
                Vec3d from = champion.getPositionVector();
                Vec3d to = rocketAt.add(0D, 3D, 0D);
                Vec3d at = from.add(to.subtract(from).scale(Math.min(1D, p * 0.5D + 0.08D)));
                champion.setPosition(at.x, at.y, at.z);
            }
        }
        if (tradeTicks == 80 && champion != null) {
            champion.setDead();
            body = null;
            world.playSound(null, rocketAt.x, rocketAt.y + 3D, rocketAt.z, SoundEvents.BLOCK_IRON_DOOR_CLOSE, SoundCategory.NEUTRAL, 2F, 0.6F);
            EntityChampion.speak(world, rocket != null ? rocket : player, "Next time, find a planet you don't mind losing. We'll go the full twelve.", 128D);
        }
        if (tradeTicks == 96 && rocket != null) {
            fx(world, MsgTribunalFx.CHAMPION_EXHAUST, rocketAt.x, rocketAt.y, rocketAt.z, 1, 0, 0, rocket.getEntityId(), 170);
            world.playSound(null, rocketAt.x, rocketAt.y, rocketAt.z, SoundEvents.ENTITY_FIREWORK_LAUNCH, SoundCategory.NEUTRAL, 4F, 0.4F);
            world.playSound(null, rocketAt.x, rocketAt.y, rocketAt.z, EternitySounds.COMET_FLYBY, SoundCategory.NEUTRAL, 4F, 0.5F);
        }
        if (tradeTicks > 110 && rocket != null) {
            rocketSpeed = Math.min(3D, rocketSpeed + 0.025D);
            rocketAt = rocketAt.add(0D, rocketSpeed, 0D);
            if (tradeTicks % 10 == 0) {
                world.playSound(null, rocketAt.x, rocketAt.y, rocketAt.z, SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.NEUTRAL, 3F, 0.4F);
            }
        }
        if (tradeTicks >= 270 || rocket != null && rocketAt.y > 400D) {
            finishTrade(player);
        }
    }

    private void finishTrade(@Nullable EntityPlayerMP player) {
        if (rocket != null) {
            rocket.setDead();
        }
        if (tradeForReal) {
            ChampionData d = data();
            d.phase = ChampionData.Phase.DEPARTED;
            d.markDirty();
            String name = player != null ? player.getName() : "someone";
            TextComponentString notice = new TextComponentString("The Champion of the Universe has left the wreck of Tamarata in "
                    + name + "'s rocket. He will not be back.");
            notice.getStyle().setColor(TextFormatting.DARK_PURPLE);
            FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().sendMessage(notice);
            Eternity.LOG.info("The Champion of the Universe has departed in {}'s rocket.", name);
        }
        rocket = null;
        trader = null;
        tradeTicks = -1;
        body = null;
    }

    /** The Power Stone leaves his brow and is yours, the one true Power Stone. */
    private boolean giveStone(EntityPlayerMP player) {
        Item stone = anvil.infinity.items.Items.POWER_STONE;
        if (stone == null || !wearsStone()) {
            return false;
        }
        ChampionData d = data();
        d.stoneTaken = true;
        d.stoneHolder = player.getName();
        d.markDirty();
        ItemStack stack = new ItemStack(stone);
        Relics.claim(stack, player);
        if (!player.inventory.addItemStackToInventory(stack)) {
            player.dropItem(stack, false);
        }
        player.inventoryContainer.detectAndSendChanges();
        player.world.playSound(null, player.posX, player.posY, player.posZ, EternitySounds.OMEN_SHIMMER, SoundCategory.PLAYERS, 1.5F, 0.6F);
        TextComponentString notice = new TextComponentString(player.getName() + " now holds the Power Stone.");
        notice.getStyle().setColor(TextFormatting.LIGHT_PURPLE);
        player.server.getPlayerList().sendMessage(notice);
        return true;
    }

    // ------------------------------------------------------------------ keeping people safe in the wreck

    /** Nobody falls out of the broken planet: anyone sinking toward the bottom of the world is brought back to the island. */
    private void keepSafe(WorldServer world) {
        for (EntityPlayer player : world.playerEntities) {
            player.fallDistance = 0F;
            if (player instanceof EntityPlayerMP && player.posY < 12D && !player.isSpectator()) {
                ((EntityPlayerMP) player).connection.setPlayerLocation(Tamarata.LANDING_X + 0.5D, Tamarata.LANDING_Y + 2D,
                        Tamarata.LANDING_Z + 5.5D, player.rotationYaw, player.rotationPitch);
                player.motionX = player.motionY = player.motionZ = 0D;
                status((EntityPlayerMP) player, "You drift back to the island where your journey began.", TextFormatting.GRAY);
            }
        }
    }

    // ------------------------------------------------------------------ events

    @SubscribeEvent
    public void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.toDim == Tamarata.dimension() && event.player instanceof EntityPlayerMP) {
            sendWorld((EntityPlayerMP) event.player);
        }
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player.dimension == Tamarata.dimension() && event.player instanceof EntityPlayerMP) {
            sendWorld((EntityPlayerMP) event.player);
        }
    }

    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.player.dimension == Tamarata.dimension() && event.player instanceof EntityPlayerMP) {
            sendWorld((EntityPlayerMP) event.player);
        }
    }

    public void sendWorld(EntityPlayerMP player) {
        EternityNetwork.send(new MsgTribunalFx(MsgTribunalFx.CHAMPION_WORLD, 0, 0, 0, 0, 0, 0, planetBroken() ? 1 : 0, 0), player);
    }

    /** A rocket of his left in a save does not outlive the moment it belonged to. */
    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent event) {
        Entity entity = event.getEntity();
        if (event.getWorld().isRemote) {
            return;
        }
        if (entity.getTags().contains(ROCKET) && entity != rocket) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getTarget().getTags().contains(ROCKET)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onInteractAt(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getTarget().getTags().contains(ROCKET)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        if (event.getTarget().getTags().contains(ROCKET)) {
            event.setCanceled(true);
        }
    }

    // ------------------------------------------------------------------ operators

    /** For /eternity champion status. */
    public String describe() {
        ChampionData d = data();
        StringBuilder line = new StringBuilder("The Champion is ");
        switch (d.phase) {
            case RESIDENT:
                line.append("in his Colosseum");
                break;
            case DUEL:
                line.append("fighting");
                break;
            case BREAKING:
                line.append("breaking Tamarata (").append(d.breakTicks / 20).append("s in)");
                break;
            case STRANDED:
                line.append("stranded in the wreck of Tamarata");
                break;
            case DEPARTED:
                line.append("gone, in someone's rocket");
                break;
        }
        if (body != null) {
            line.append(String.format(", %s at %.0f %.0f %.0f, rage %.0f%% (%s)", body.getState().name().toLowerCase(), body.posX, body.posY,
                    body.posZ, body.getRage() * 100F, EntityChampion.stageName(body.getStage())));
        } else {
            line.append(", not present");
        }
        return line.toString();
    }

    /** Takes someone to the Colosseum, or once the planet is broken, to what is left of the landing site. */
    public static void visit(EntityPlayerMP player) {
        if (INSTANCE.planetBroken()) {
            Teleport.player(player, Tamarata.dimension(), Tamarata.LANDING_X + 0.5D, Tamarata.LANDING_Y + 1D, Tamarata.LANDING_Z + 5.5D, false);
        } else {
            Teleport.player(player, Tamarata.dimension(), Tamarata.ARENA_X - Tamarata.WALL_RADIUS - 6D, Tamarata.ARENA_Y + 2D,
                    Tamarata.ARENA_Z + 0.5D, true);
        }
    }

    /** Makes him lose his temper completely and break the planet, as soon as he can. */
    public boolean forceBreak(EntityPlayerMP player) {
        ChampionData d = data();
        if (d.planetDestroyed) {
            return false;
        }
        if (body != null && d.phase == ChampionData.Phase.DUEL) {
            body.setRage(1F);
            body.order(EntityChampion.Move.BREAKER);
            return true;
        }
        if (body != null && player.world == body.world && d.phase == ChampionData.Phase.RESIDENT) {
            beginDuel(body, player);
            body.setRage(1F);
            body.order(EntityChampion.Move.BREAKER);
            return true;
        }
        WorldServer world = DimensionManager.getWorld(Tamarata.dimension());
        if (world == null) {
            return false;
        }
        breakPlanet(world, Tamarata.ARENA_X + 0.5D, Tamarata.ARENA_Y, Tamarata.ARENA_Z + 0.5D);
        d.phase = ChampionData.Phase.BREAKING;
        d.markDirty();
        return true;
    }

    /** Skips straight to him stranded in the wreck, the planet breaking around him. */
    public void forceStrand() {
        ChampionData d = data();
        WorldServer world = DimensionManager.getWorld(Tamarata.dimension());
        if (!d.planetDestroyed && world != null) {
            breakPlanet(world, Tamarata.ARENA_X + 0.5D, Tamarata.ARENA_Y, Tamarata.ARENA_Z + 0.5D);
        }
        d.phase = ChampionData.Phase.STRANDED;
        d.breakTicks = Math.max(d.breakTicks, 1);
        d.markDirty();
        strandedLine = 0;
        nextLine = tick + 20;
        if (body != null) {
            body.drift(driftPoint());
        }
    }

    /** The planet whole again (as far as anything not yet broken goes), and him back in his arena, stone and all. */
    public void restore() {
        ChampionData d = data();
        vanish();
        if (rocket != null) {
            rocket.setDead();
        }
        rocket = null;
        trader = null;
        tradeTicks = -1;
        d.phase = ChampionData.Phase.RESIDENT;
        d.planetDestroyed = false;
        d.breakTicks = 0;
        d.stoneTaken = false;
        d.stoneHolder = "";
        d.rage.clear();
        d.markDirty();
        TamarataBreaker.INSTANCE.reset();
        EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.CHAMPION_WORLD, 0, 0, 0, 0, 0, 0, 0, 0), Tamarata.dimension());
    }

    // ------------------------------------------------------------------ helpers

    private static void fx(net.minecraft.world.World world, byte type, double x, double y, double z, double a, double b, double c, int id, int time) {
        EternityNetwork.sendToDimension(new MsgTribunalFx(type, x, y, z, a, b, c, id, time), world.provider.getDimension());
    }

    private static void sound(WorldServer world, Vec3d at, SoundEvent sound, float volume, float pitch) {
        if (volume > 0F) {
            world.playSound(null, at.x, at.y, at.z, sound, SoundCategory.HOSTILE, volume, pitch);
        }
    }

    private static double horizontal(Entity a, Entity b) {
        double dx = a.posX - b.posX, dz = a.posZ - b.posZ;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static void status(EntityPlayerMP player, String text, TextFormatting colour) {
        TextComponentString message = new TextComponentString(text);
        message.getStyle().setColor(colour);
        player.sendStatusMessage(message, true);
    }
}
