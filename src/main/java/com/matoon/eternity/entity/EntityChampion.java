package com.matoon.eternity.entity;

import com.matoon.eternity.EternityConfig;
import com.matoon.eternity.EternitySounds;
import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.network.MsgTribunalFx;
import com.matoon.eternity.world.ChampionDirector;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.BossInfo;
import net.minecraft.world.BossInfoServer;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tryco Slatterus, the Champion of the Universe: an Elder of the Universe who has spent
 * billions of years doing the one thing he cares about, which is fighting. He wears the Power
 * Stone in his brow without understanding it, and the stone feeds on his temper.
 * <p>
 * He fights like a boxer the size of a house: jab-jab-cross, a hook to the liver, an uppercut
 * that launches you, a superman punch that crosses the arena, a clinch and a throw, a shoulder
 * charge, a ground pound, a counter stance that dares you to hit him. He roars. Between bouts
 * he flexes, because he can. Head-on he cannot be beaten: he blocks or slips most of what comes
 * at him from the front, no blow takes more than a sliver of him, and knocked down he gets back
 * up healed by the stone, and angrier.
 * <p>
 * Everything turns on his <b>rage</b>. Make a fool of him (slip his blows, stay out of reach,
 * pick at him from range, taunt him) and it climbs: Composed, Heated, Furious, Berserk, each
 * faster and harder than the last, the stone brighter, the veins standing out. Trade blows with
 * him up close, honourably, and it cools, because he respects that. At the top of it he loses
 * himself entirely and drives both fists into his own planet.
 * <p>
 * What everyone sees is synced (what he is doing, how angry he is, whether he wears the stone,
 * and how fast his temper is driving him); how he moves is worked out on each client.
 */
public class EntityChampion extends EntityLiving {

    public enum State {
        IDLE, CHALLENGE, FLEX,
        JAB_CROSS, HOOK, UPPERCUT, HAYMAKER, GRAB, RUSH, POUND, COUNTER, ROAR, HAMMER,
        BREAKER, IMPACT,
        GUARD, SLIP, KNOCKDOWN, RISE,
        DRIFT, BOARD
    }

    /** What he can be told to do, for testing. */
    public enum Move { FLEX, CHALLENGE, JAB_CROSS, HOOK, UPPERCUT, HAYMAKER, GRAB, RUSH, POUND, COUNTER, ROAR, HAMMER, BREAKER }

    // How long everything takes, in beats; a beat is a tick at his calmest, and less as his temper drives him.
    // The model times its frames to the same numbers.
    public static final int CHALLENGE_TICKS = 72;
    public static final int FLEX_TICKS = 66;
    public static final int JAB_1 = 7, JAB_2 = 13, CROSS = 21, JAB_CROSS_TICKS = 34;
    public static final int HOOK_HIT = 15, HOOK_TICKS = 30;
    public static final int UPPER_HIT = 13, UPPER_TICKS = 32;
    public static final int HAY_LEAP = 12, HAY_LAND = 28, HAY_TICKS = 46;
    public static final int GRAB_REACH = 10, GRAB_THROW = 30, GRAB_TICKS = 42, GRAB_MISS_TICKS = 26;
    public static final int RUSH_GO = 10, RUSH_STOP = 30, RUSH_TICKS = 40;
    public static final int POUND_HIT = 18, POUND_TICKS = 36;
    public static final int COUNTER_OPEN = 6, COUNTER_TICKS = 62, COUNTER_STRIKE = 4, COUNTER_AFTER = 18;
    public static final int ROAR_GO = 14, ROAR_TICKS = 58;
    public static final int HAMMER_HIT = 20, HAMMER_TICKS = 40;
    public static final int BREAKER_LEAP = 40, BREAKER_APEX = 64, BREAKER_DIVE = 84, BREAKER_HIT = 96;
    public static final int GUARD_TICKS = 10, SLIP_TICKS = 11;
    public static final int KNOCKDOWN_TICKS = 70, RISE_TICKS = 44;
    public static final int BOARD_TICKS = 60;
    /** How high he leaps before Planet Breaker. */
    public static final double BREAKER_HEIGHT = 58D;

    /** Segments in his mane, top to bottom. */
    public static final int MANE = 8;
    /** How many points of his afterimage trail he keeps, client side. */
    public static final int TRAIL = 10;

    private static final DataParameter<Byte> STATE = EntityDataManager.createKey(EntityChampion.class, DataSerializers.BYTE);
    private static final DataParameter<Float> RAGE = EntityDataManager.createKey(EntityChampion.class, DataSerializers.FLOAT);
    private static final DataParameter<Boolean> STONE = EntityDataManager.createKey(EntityChampion.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Float> TEMPO = EntityDataManager.createKey(EntityChampion.class, DataSerializers.FLOAT);
    private static final DataParameter<Integer> HELD = EntityDataManager.createKey(EntityChampion.class, DataSerializers.VARINT);

    private static final Move[] SHOWCASE = {Move.CHALLENGE, Move.JAB_CROSS, Move.HOOK, Move.UPPERCUT, Move.FLEX, Move.HAYMAKER, Move.GRAB,
            Move.RUSH, Move.POUND, Move.COUNTER, Move.ROAR, Move.HAMMER, Move.FLEX};

    // ------------------------------------------------------------------ what he says
    private static final String[] CHALLENGES = {"You came into my ring. Then you came to fight. Hands up.",
            "I have fought every kind of creature there is. Let us see which kind you are.",
            "No crowd left to watch. It doesn't matter. Show me your guard."};
    private static final String[] TAUNTS = {"You're thinking. Stop thinking. Throw something.", "That's footwork, not fighting.",
            "I've had longer bouts with my own shadow.", "Close the distance. That's where it's settled.", "Chin down. You'll want it later."};
    private static final String[] FLEXES = {"Undefeated. Since before your star was lit.", "Every one of these muscles has an opponent's name on it.",
            "Champion of the Universe. Not a title I gave myself. I took it."};
    private static final String[] LANDED = {"Mm.", "Stay down if you want. No shame in it.", "You felt that one in your ancestors.",
            "Breathe through it."};
    private static final String[] DODGED = {"Quick.", "You slip well. Running is a different thing.", "Make me miss all you like. Eventually you have to hit back.",
            "I have chased worse than you across whole galaxies."};
    private static final String[] CHEAP = {"From over there. That isn't a fight, it's a complaint.", "Throwing things. The Kree fought like that. They're gone too.",
            "Come into range. That's where the respect is."};
    private static final String[] HONOUR = {"Better.", "There it is. Again.", "Good hands. Somebody taught you properly."};
    private static final String[] BLOCKED = {"Telegraphed.", "I saw that in your shoulders.", "Older than your sun, that one."};
    private static final String[] RISEN = {"I have been knocked down by better. Not often.", "Good. Now it's a fight.",
            "The count never reaches ten. Not for me."};
    private static final String[] STAGE_UP = {"", "Alright. Now I'm paying attention.", "You don't want to win. You want to make me angry. It's working.",
            "Enough dancing. I'll break the floor out from under you."};
    private static final String[] WON = {"Another name for the arms.", "Not bad. Not enough.", "Rest. You fought, which is more than most."};

    private final BossInfoServer bossInfo = (BossInfoServer) new BossInfoServer(
            new TextComponentString("The Champion of the Universe"), BossInfo.Color.WHITE, BossInfo.Overlay.NOTCHED_10);

    // ------------------------------------------------------------------ server state
    /** Whether the director moves him; a summoned one, for testing, is his own master and never breaks anything. */
    private boolean managed;
    @Nullable
    private UUID duelId;
    @Nullable
    private EntityLivingBase brawl;
    private boolean calm;
    private boolean showcase;
    private int showIndex;
    @Nullable
    private Move pendingMove;
    private Move lastMove = Move.FLEX;
    private int stateTicks;
    private float clock, prevClock;
    private int cooldown = 30;
    private int hitStop;
    private float rage;
    private int lastSpoke = -1000;
    private int lastHonour = -1000;
    private int lastTaunted = -1000;
    private int lonely;
    private int lastStage;
    private double strafe = 1D;
    private Vec3d lunge = new Vec3d(0D, 0D, 1D);
    private Vec3d leapFrom, leapTo;
    @Nullable
    private EntityLivingBase grabbed;
    private float counterAt = -1F;
    @Nullable
    private EntityPlayer counterOn;
    private Vec3d anchor;
    /** Whether his planet is breaking under his fists, so he stays down in the crater until the director moves him. */
    private boolean holdImpact;
    private final Map<UUID, Boolean> sneaking = new HashMap<>();

    // ------------------------------------------------------------------ client state (how he moves, worked out where he is drawn)
    /** Client: ticks since the synced state last changed. */
    public int clientStateTicks;
    public State previousState = State.IDLE;
    private State seenState = State.IDLE;
    /** Client: his velocity, smoothed, in blocks per tick. */
    public double velX, velY, velZ;
    /** Client: how far through a stride he is, and how hard he is walking, 0 to 1. */
    public float walkPhase, prevWalkPhase, walkAmount, prevWalkAmount;
    /** Client: leaning into where he goes, and tumbling when he drifts, in degrees. */
    public float bodyPitch, prevBodyPitch, bodyRoll, prevBodyRoll;
    /** Client: his rage as drawn, and how brightly the stone burns. */
    public float shownRage, prevShownRage, glow, prevGlow;
    /** Client: each mane segment's angle back from his head, and how fast it swings. */
    public final float[] mane = new float[MANE], prevMane = new float[MANE], maneSpin = new float[MANE];
    public float maneSway, prevManeSway;
    /** Client: where he has been, newest first: x, y, z, yaw, pitch, heat. */
    public final double[][] trail = new double[TRAIL][6];
    public int trailCount;
    /** Client: the pose he was in when his state last changed, which the model blends out of; and the pose last drawn. */
    public final float[] poseFrom = new float[40], poseLast = new float[40];
    /** Client: the tick into his counter stance when someone took the bait, or -1. */
    public int counterTick = -1;

    public EntityChampion(World world) {
        super(world);
        setSize(1.1F, 3.2F);
        stepHeight = 1.6F;
        isImmuneToFire = true;
        experienceValue = 0;
        ignoreFrustumCheck = true;
        setNoGravity(true);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(STATE, (byte) State.IDLE.ordinal());
        dataManager.register(RAGE, 0F);
        dataManager.register(STONE, false);
        dataManager.register(TEMPO, 1F);
        dataManager.register(HELD, -1);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(EternityConfig.champion.health);
        getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(1D);
    }

    @Override
    public void notifyDataManagerChange(DataParameter<?> key) {
        super.notifyDataManagerChange(key);
        if (STATE.equals(key) && world.isRemote) {
            clientStateTicks = 0;
            System.arraycopy(poseLast, 0, poseFrom, 0, poseLast.length);
            counterTick = -1;
            previousState = seenState;
            seenState = getState();
        }
    }

    // ------------------------------------------------------------------ accessors

    public State getState() {
        int ordinal = dataManager.get(STATE);
        return ordinal >= 0 && ordinal < State.values().length ? State.values()[ordinal] : State.IDLE;
    }

    private void setState(State state) {
        float tempo = 1F + 0.42F * (float) Math.pow(rage, 1.3D);
        if (state == State.BREAKER || state == State.KNOCKDOWN || state == State.RISE || state == State.DRIFT || state == State.BOARD
                || state == State.FLEX || state == State.CHALLENGE || state == State.IMPACT) {
            tempo = 1F;
        }
        dataManager.set(TEMPO, tempo);
        dataManager.set(STATE, (byte) state.ordinal());
        stateTicks = 0;
        clock = prevClock = 0F;
    }

    /** How fast his temper is driving him through what he is doing, 1 at his calmest. */
    public float getTempo() {
        return dataManager.get(TEMPO);
    }

    public float getRage() {
        return dataManager.get(RAGE);
    }

    /** 0 Composed, 1 Heated, 2 Furious, 3 Berserk. */
    public int getStage() {
        return stageOf(getRage());
    }

    public static int stageOf(float rage) {
        return rage < 0.3F ? 0 : rage < 0.6F ? 1 : rage < 0.9F ? 2 : 3;
    }

    public static String stageName(int stage) {
        return stage == 0 ? "Composed" : stage == 1 ? "Heated" : stage == 2 ? "Furious" : "Berserk";
    }

    public boolean wearsStone() {
        return dataManager.get(STONE);
    }

    public void setStone(boolean stone) {
        dataManager.set(STONE, stone);
    }

    /** Whoever he has in his hands, mid-throw, or -1. */
    public int getHeld() {
        return dataManager.get(HELD);
    }

    public boolean isManaged() {
        return managed;
    }

    public boolean isCalm() {
        return calm;
    }

    public boolean isShowcase() {
        return showcase;
    }

    @Nullable
    public UUID getDuel() {
        return duelId;
    }

    /** Whether he flies through the air head- or fist-first, so his body lines up with where he is going. */
    public boolean isAirborne() {
        float t = clientStateTicks * getTempo();
        switch (getState()) {
            case HAYMAKER:
                return t >= HAY_LEAP && t < HAY_LAND;
            case BREAKER:
                return t >= BREAKER_LEAP && t < BREAKER_HIT;
            default:
                return false;
        }
    }

    // ------------------------------------------------------------------ the director's and the command's handles

    /** Put on Tamarata and moved by the director. */
    public void setManaged(boolean managed) {
        this.managed = managed;
    }

    /** He turns on someone and calls them out. */
    public void challenge(EntityPlayer player) {
        duelId = player.getUniqueID();
        brawl = null;
        if (canAct()) {
            say(CHALLENGES[rand.nextInt(CHALLENGES.length)]);
            setState(State.CHALLENGE);
        }
    }

    /** He forgets whoever he was fighting. */
    public void endDuel() {
        duelId = null;
    }

    public void setCalm(boolean calm, boolean showcase) {
        this.calm = calm;
        this.showcase = showcase;
    }

    public void order(Move move) {
        pendingMove = move;
        if (getState() == State.IDLE) {
            cooldown = 0;
        }
    }

    public void setRage(float value) {
        rage = MathHelper.clamp(value, 0F, 1F);
        dataManager.set(RAGE, rage);
        lastStage = stageOf(rage);
    }

    /** Stranded: no ground, no way to move, turning slowly where he hangs. */
    public void drift(Vec3d at) {
        anchor = at;
        grabbed = null;
        dataManager.set(HELD, -1);
        if (getState() != State.DRIFT && getState() != State.BOARD) {
            setState(State.DRIFT);
        }
    }

    /** Planet Breaker has broken his planet: he stays down in the crater until it is gone from under him. */
    public void holdImpact() {
        holdImpact = true;
    }

    /** Pulling himself across to a rocket and climbing in. The director moves him. */
    public void board() {
        setState(State.BOARD);
    }

    /** Whether he is free to start something new. */
    private boolean canAct() {
        State s = getState();
        return s == State.IDLE || s == State.FLEX;
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void onLivingUpdate() {
        if (!world.isRemote) {
            serverTick();
        }
        super.onLivingUpdate();
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        renderYawOffset = rotationYaw;
        prevRenderYawOffset = prevRotationYaw;
        if (world.isRemote) {
            clientTick();
        }
    }

    @Override
    public void travel(float strafe, float vertical, float forward) {
        if (noClip) {
            setPosition(posX + motionX, posY + motionY, posZ + motionZ);
        } else {
            move(MoverType.SELF, motionX, motionY, motionZ);
        }
    }

    private void serverTick() {
        if (managed && !ChampionDirector.INSTANCE.claim(this)) {
            setDead();
            return;
        }
        stateTicks++;
        bossBar();
        State state = getState();
        boolean flying = state == State.DRIFT || state == State.BOARD || state == State.IMPACT || state == State.BREAKER && clock >= BREAKER_LEAP
                || state == State.HAYMAKER && clock >= HAY_LEAP && clock < HAY_LAND;
        noClip = flying;
        if (hitStop > 0) {
            // The instant a blow lands, everything holds still for it.
            hitStop--;
            motionX = motionY = motionZ = 0D;
            return;
        }
        prevClock = clock;
        clock += getTempo();
        // Gravity, his own: the world's would float him about like a balloon.
        if (!flying) {
            motionY = onGround ? -0.08D : Math.max(-2.5D, motionY - 0.09D);
        } else {
            motionY = 0D;
        }
        motionX *= 0.5D;
        motionZ *= 0.5D;

        EntityLivingBase target = target();
        if (target instanceof EntityPlayer && duelId != null && !showcase) {
            temper((EntityPlayer) target);
        } else if (duelId == null) {
            setRage(rage - 0.0008F * (float) EternityConfig.champion.rageDecay);
        }
        int stage = stageOf(rage);
        if (stage > lastStage && duelId != null) {
            say(STAGE_UP[stage]);
            if (stage >= 2) {
                Vec3d brow = brow();
                fx(MsgTribunalFx.CHAMPION_ARCS, brow.x, brow.y, brow.z, stage / 3D, 0, 0, getEntityId(), 40);
                sound(SoundEvents.ENTITY_LIGHTNING_THUNDER, 1.5F, 1.4F);
            }
        }
        lastStage = stage;

        switch (state) {
            case IDLE:
                idle(target);
                break;
            case CHALLENGE:
                challenging(target);
                break;
            case FLEX:
                flexing(target);
                break;
            case JAB_CROSS:
                jabCross(target);
                break;
            case HOOK:
                hook(target);
                break;
            case UPPERCUT:
                uppercut(target);
                break;
            case HAYMAKER:
                haymaker(target);
                break;
            case GRAB:
                grab(target);
                break;
            case RUSH:
                rush(target);
                break;
            case POUND:
                pound(target);
                break;
            case COUNTER:
                counter(target);
                break;
            case ROAR:
                roaring(target);
                break;
            case HAMMER:
                hammer(target);
                break;
            case BREAKER:
                breaker(target);
                break;
            case IMPACT:
                // Nothing broke after all (away from Tamarata, or the fight was over): he gets back up.
                if (!holdImpact && clock >= 70) {
                    recover(20);
                }
                break;
            case GUARD:
            case SLIP:
                reacting(target);
                break;
            case KNOCKDOWN:
                knockedDown();
                break;
            case RISE:
                rising(target);
                break;
            case DRIFT:
                drifting();
                break;
            case BOARD:
                break;
        }
    }

    private boolean at(float beat) {
        return prevClock < beat && clock >= beat;
    }

    private void bossBar() {
        if (ticksExisted % 5 != 0) {
            return;
        }
        State state = getState();
        boolean visible = state != State.DRIFT && state != State.BOARD && state != State.IMPACT;
        bossInfo.setVisible(visible);
        int stage = stageOf(rage);
        bossInfo.setName(new TextComponentString("The Champion of the Universe — " + (state == State.KNOCKDOWN ? "Down!" : stageName(stage))));
        bossInfo.setPercent(rage);
        bossInfo.setColor(stage == 0 ? BossInfo.Color.WHITE : stage == 1 ? BossInfo.Color.YELLOW : stage == 2 ? BossInfo.Color.RED : BossInfo.Color.PURPLE);
    }

    /** Whoever he is fighting: the one he challenged while they are here and standing; failing that, the nearest of the armies. */
    @Nullable
    private EntityLivingBase target() {
        if (duelId != null) {
            EntityPlayer player = world.getPlayerEntityByUUID(duelId);
            if (player != null && isFair(player) && player.getDistanceSq(this) < 80D * 80D) {
                lonely = 0;
                return player;
            }
            if (player != null && player.getHealth() <= 0F && canAct()) {
                say(WON[rand.nextInt(WON.length)]);
                duelId = null;
                setState(State.FLEX);
                return null;
            }
            if (!managed && ++lonely > 120) {
                say("Gone. They always leave before the last round.");
                setDead();
            }
            return null;
        }
        if (brawl != null && (brawl.isDead || brawl.getHealth() <= 0F || brawl.getDistanceSq(this) > 48D * 48D)) {
            brawl = null;
        }
        if (brawl == null) {
            double best = 40D * 40D;
            for (EntityLivingBase other : world.getEntitiesWithinAABB(EntityLivingBase.class, getEntityBoundingBox().grow(40D))) {
                if (ChampionDirector.isArmy(other) && other.isEntityAlive() && other.getDistanceSq(this) < best) {
                    brawl = other;
                    best = other.getDistanceSq(this);
                }
            }
        }
        if (brawl == null && !managed) {
            EntityPlayer nearest = world.getClosestPlayer(posX, posY, posZ, 48D, false);
            if (nearest != null && isFair(nearest)) {
                challenge(nearest);
                return nearest;
            }
        }
        return brawl;
    }

    private static boolean isFair(EntityPlayer player) {
        return !player.isDead && !player.isSpectator() && player.getHealth() > 0F;
    }

    // ------------------------------------------------------------------ his temper

    /**
     * What you do to his temper, each tick you fight him. Keep out of his reach and it climbs;
     * taunt him and it jumps; stand in front of him and trade blows, and it cools.
     */
    private void temper(EntityPlayer player) {
        double gain = EternityConfig.champion.rageGain, decay = EternityConfig.champion.rageDecay;
        double d = player.getDistance(this);
        if (getState() != State.BREAKER && getState() != State.IMPACT && getState() != State.KNOCKDOWN && getState() != State.RISE) {
            if (d > 14D) {
                addRage((float) (0.00022D * gain));
            } else if (d > 7D) {
                addRage((float) (0.0001D * gain));
            } else if (d < 4.5D && ticksExisted - lastHonour < 40) {
                addRage((float) (-0.0004D * decay));
            }
        }
        // Sneaking up and down in front of him, like a fool: he takes that as the insult it is.
        boolean was = sneaking.getOrDefault(player.getUniqueID(), false);
        boolean now = player.isSneaking();
        sneaking.put(player.getUniqueID(), now);
        if (now && !was && d < 20D && ticksExisted - lastTaunted > 50) {
            Vec3d look = facing();
            Vec3d to = flat(player.getPositionVector().subtract(getPositionVector()));
            if (look.dotProduct(to) > 0.5D) {
                taunted(0.008F);
            }
        }
    }

    private void addRage(float amount) {
        if (calm && showcase) {
            return;
        }
        setRage(rage + amount);
        if (managed) {
            ChampionDirector.INSTANCE.rememberRage(this, rage);
        }
    }

    /** Someone has made a fool of him on purpose. */
    public void taunted(float amount) {
        lastTaunted = ticksExisted;
        addRage(amount * (float) EternityConfig.champion.rageGain);
        if (rand.nextInt(4) == 0) {
            say(rand.nextBoolean() ? "Clowning. In my ring." : "Keep that up. See where it gets you.");
        }
    }

    // ------------------------------------------------------------------ standing his ground

    /** Up on his toes, guard up, closing the distance, circling; then he picks his moment. */
    private void idle(@Nullable EntityLivingBase target) {
        if (target == null) {
            if (anchor != null && getDistanceSq(anchor.x, anchor.y, anchor.z) > 9D) {
                walkToward(anchor, 0.12D);
            }
            return;
        }
        face(target, 0.35F);
        double d = horizontal(target);
        double speed = 0.15D + 0.13D * rage;
        if (d > 3.1D) {
            walkToward(target.getPositionVector(), speed);
        } else if (d < 1.9D) {
            Vec3d back = flat(getPositionVector().subtract(target.getPositionVector()));
            motionX = back.x * 0.12D;
            motionZ = back.z * 0.12D;
        } else if (cooldown > 6) {
            // Circling, the way a fighter does while he waits.
            if (rand.nextInt(50) == 0) {
                strafe = -strafe;
            }
            Vec3d to = flat(target.getPositionVector().subtract(getPositionVector()));
            motionX = -to.z * 0.06D * strafe;
            motionZ = to.x * 0.06D * strafe;
        }
        if (--cooldown <= 0) {
            choose(target, d);
        }
    }

    private void walkToward(Vec3d at, double speed) {
        Vec3d to = flat(at.subtract(getPositionVector()));
        motionX = to.x * speed;
        motionZ = to.z * speed;
    }

    private void choose(EntityLivingBase target, double d) {
        Move next;
        boolean player = target instanceof EntityPlayer;
        int stage = stageOf(rage);
        if (pendingMove != null) {
            next = pendingMove;
            pendingMove = null;
        } else if (rage >= 1F && player && duelId != null) {
            next = Move.BREAKER;
        } else if (showcase) {
            next = SHOWCASE[showIndex++ % SHOWCASE.length];
        } else {
            int jab, hook, upper, grab, pound, counter, haymaker, rush, roar, flex, hammer;
            if (d <= 3.8D) {
                jab = 30;
                hook = 22;
                upper = 18;
                grab = 14;
                pound = 9;
                counter = player ? 8 : 0;
                haymaker = 0;
                rush = 0;
            } else {
                jab = hook = upper = grab = 0;
                pound = d < 8D ? 8 : 0;
                counter = player && d < 9D ? 7 : 0;
                haymaker = (int) (20 + 30 * rage);
                rush = d < 18D ? 22 : 8;
            }
            roar = stage >= 1 && lastMove != Move.ROAR ? 6 + stage * 3 : 0;
            flex = player && stage == 0 && lastMove != Move.FLEX ? 6 : 0;
            hammer = stage >= 3 && d < 7D ? 22 : 0;
            int total = jab + hook + upper + grab + pound + counter + haymaker + rush + roar + flex + hammer;
            if (total <= 0) {
                cooldown = 10;
                return;
            }
            do {
                int roll = rand.nextInt(total);
                next = (roll -= jab) < 0 ? Move.JAB_CROSS : (roll -= hook) < 0 ? Move.HOOK : (roll -= upper) < 0 ? Move.UPPERCUT
                        : (roll -= grab) < 0 ? Move.GRAB : (roll -= pound) < 0 ? Move.POUND : (roll -= counter) < 0 ? Move.COUNTER
                        : (roll -= haymaker) < 0 ? Move.HAYMAKER : (roll -= rush) < 0 ? Move.RUSH : (roll -= roar) < 0 ? Move.ROAR
                        : (roll -= flex) < 0 ? Move.FLEX : Move.HAMMER;
            } while (next == lastMove && next != Move.JAB_CROSS && total > 30);
        }
        start(next, target);
    }

    private void start(Move move, @Nullable EntityLivingBase target) {
        lastMove = move;
        Vec3d facing = facing();
        switch (move) {
            case FLEX:
                setState(State.FLEX);
                say(FLEXES[rand.nextInt(FLEXES.length)]);
                break;
            case CHALLENGE:
                setState(State.CHALLENGE);
                say(CHALLENGES[rand.nextInt(CHALLENGES.length)]);
                break;
            case JAB_CROSS:
                setState(State.JAB_CROSS);
                break;
            case HOOK:
                lunge = target != null ? flat(target.getPositionVector().subtract(getPositionVector())) : facing;
                setState(State.HOOK);
                break;
            case UPPERCUT:
                setState(State.UPPERCUT);
                break;
            case HAYMAKER:
                leapFrom = null;
                setState(State.HAYMAKER);
                break;
            case GRAB:
                lunge = target != null ? flat(target.getPositionVector().subtract(getPositionVector())) : facing;
                grabbed = null;
                setState(State.GRAB);
                break;
            case RUSH:
                lunge = target != null ? flat(target.getPositionVector().subtract(getPositionVector())) : facing;
                setState(State.RUSH);
                break;
            case POUND:
                setState(State.POUND);
                break;
            case COUNTER:
                counterAt = -1F;
                counterOn = null;
                setState(State.COUNTER);
                say(TAUNTS[rand.nextInt(TAUNTS.length)]);
                break;
            case ROAR:
                setState(State.ROAR);
                break;
            case HAMMER:
                setState(State.HAMMER);
                break;
            case BREAKER:
                leapFrom = getPositionVector();
                setState(State.BREAKER);
                say("Hold your footing, little one. If you can find it.");
                break;
        }
    }

    private void recover(int ticks) {
        setState(State.IDLE);
        float stage = stageOf(rage);
        cooldown = Math.max(4, (int) (ticks * (1F - stage * 0.2F)));
    }

    private void challenging(@Nullable EntityLivingBase target) {
        if (target != null) {
            face(target, 0.2F);
        }
        if (at(30)) {
            sound(SoundEvents.ENTITY_IRONGOLEM_ATTACK, 2F, 0.5F);
        }
        if (at(42) || at(50)) {
            // Fists against his chest.
            sound(SoundEvents.ENTITY_ZOMBIE_ATTACK_DOOR_WOOD, 1.5F, 0.5F);
        }
        if (clock >= CHALLENGE_TICKS) {
            recover(10);
        }
    }

    private void flexing(@Nullable EntityLivingBase target) {
        if (target != null) {
            face(target, 0.12F);
        }
        if (at(14) || at(40)) {
            sound(SoundEvents.ENTITY_PLAYER_ATTACK_NODAMAGE, 1.5F, 0.4F);
        }
        if (clock >= FLEX_TICKS) {
            recover(14);
        }
    }

    // ------------------------------------------------------------------ fists

    /** Jab, jab, cross: stepping in behind each one. */
    private void jabCross(@Nullable EntityLivingBase target) {
        if (target != null) {
            face(target, 0.5F);
            if (horizontal(target) > 2.6D && clock < CROSS) {
                walkToward(target.getPositionVector(), 0.2D);
            }
        }
        if (at(JAB_1)) {
            strike(target, 0.06F, 0.35D, 0.15D, 0, 3.6D);
        } else if (at(JAB_2)) {
            strike(target, 0.06F, 0.35D, 0.15D, 0, 3.6D);
        } else if (at(CROSS - 4)) {
            step(0.45D);
        } else if (at(CROSS)) {
            strike(target, 0.10F, 0.8D, 0.2D, 1, 3.9D);
        }
        if (clock >= JAB_CROSS_TICKS) {
            recover(22);
        }
    }

    /** A step to the side and in, then the hook, low, into the liver. */
    private void hook(@Nullable EntityLivingBase target) {
        if (target != null && clock < HOOK_HIT - 3) {
            face(target, 0.4F);
            lunge = flat(target.getPositionVector().subtract(getPositionVector()));
        }
        if (clock < 8) {
            motionX = lunge.x * 0.22D;
            motionZ = lunge.z * 0.22D;
        }
        if (at(HOOK_HIT)) {
            Vec3d side = new Vec3d(lunge.z, 0D, -lunge.x);
            strike(target, 0.14F, 0.9D, 0.35D, 1, 3.7D, lunge.scale(0.6D).add(side.scale(0.6D)));
        }
        if (clock >= HOOK_TICKS) {
            recover(24);
        }
    }

    /** Down and coiled, then up through you. It launches you. */
    private void uppercut(@Nullable EntityLivingBase target) {
        if (target != null && clock < UPPER_HIT - 2) {
            face(target, 0.5F);
            if (horizontal(target) > 2.6D) {
                walkToward(target.getPositionVector(), 0.18D);
            }
        }
        if (at(UPPER_HIT - 3)) {
            step(0.4D);
        }
        if (at(UPPER_HIT)) {
            strike(target, 0.16F, 0.3D, 1.35D, 1, 3.7D);
        }
        if (clock >= UPPER_TICKS) {
            recover(26);
        }
    }

    /** The superman punch: a crouch, a leap across the arena, and his fist out in front of him all the way. */
    private void haymaker(@Nullable EntityLivingBase target) {
        if (clock < HAY_LEAP) {
            if (target != null) {
                face(target, 0.4F);
            }
            if (at(4)) {
                sound(SoundEvents.ENTITY_IRONGOLEM_ATTACK, 2F, 0.4F);
            }
            return;
        }
        if (leapFrom == null) {
            leapFrom = getPositionVector();
            Vec3d to = target != null ? target.getPositionVector().add(new Vec3d(target.posX - target.prevPosX, 0D, target.posZ - target.prevPosZ).scale(6D))
                    : leapFrom.add(facing().scale(12D));
            if (calm && target != null) {
                Vec3d side = flat(to.subtract(leapFrom)).crossProduct(new Vec3d(0D, 1D, 0D));
                to = to.add(side.scale(4D));
            }
            Vec3d along = to.subtract(leapFrom);
            double length = Math.min(26D, Math.sqrt(along.x * along.x + along.z * along.z));
            Vec3d dir = flat(along);
            // He comes down a little short, his fist out ahead of him.
            to = leapFrom.add(dir.scale(Math.max(0D, length - 1.6D)));
            leapTo = new Vec3d(to.x, floor(to.x, Math.max(to.y, posY) + 6D, to.z), to.z);
            lunge = dir;
            faceAlong(dir);
            fx(MsgTribunalFx.CHAMPION_LEAP, posX, posY, posZ, leapTo.x, leapTo.y, leapTo.z, 0, HAY_LAND - HAY_LEAP);
            sound(SoundEvents.ENTITY_ENDERDRAGON_FLAP, 2.5F, 0.6F);
            sound(SoundEvents.ENTITY_GENERIC_EXPLODE, 1.5F, 1.5F);
        }
        if (clock < HAY_LAND) {
            double p = MathHelper.clamp((clock - HAY_LEAP) / (double) (HAY_LAND - HAY_LEAP), 0D, 1D);
            Vec3d at = leapFrom.add(leapTo.subtract(leapFrom).scale(p));
            double height = Math.min(7D, leapFrom.distanceTo(leapTo) * 0.28D);
            setPosition(at.x, at.y + Math.sin(p * Math.PI) * height, at.z);
            // Anyone in his path takes the fist.
            for (EntityLivingBase victim : victims(3.2D)) {
                if (victim.getDistance(posX + lunge.x * 1.5D, posY + 1.8D, posZ + lunge.z * 1.5D) < 2.2D) {
                    land(victim, 0.30F, lunge, 2.2D, 0.6D, 2);
                    leapFrom = getPositionVector();
                    leapTo = new Vec3d(posX, floor(posX, posY + 2D, posZ), posZ);
                    clock = HAY_LAND - 2F;
                    break;
                }
            }
            return;
        }
        if (at(HAY_LAND)) {
            setPosition(leapTo.x, leapTo.y, leapTo.z);
            fx(MsgTribunalFx.CHAMPION_QUAKE, posX, posY, posZ, 5D, 0, 0, 1, 20);
            sound(EternitySounds.COMET_IMPACT, 3F, 0.7F);
            sound(SoundEvents.ENTITY_GENERIC_EXPLODE, 2F, 0.8F);
            quake(3.8D, 0.08F, 0.7D, false);
        }
        if (clock >= HAY_TICKS) {
            recover(30);
        }
    }

    /** He reaches for you; if he gets hold, up you go over his head, and then you are thrown. */
    private void grab(@Nullable EntityLivingBase target) {
        if (clock < GRAB_REACH) {
            if (target != null) {
                face(target, 0.5F);
                lunge = flat(target.getPositionVector().subtract(getPositionVector()));
            }
            motionX = lunge.x * 0.24D;
            motionZ = lunge.z * 0.24D;
        }
        if (at(GRAB_REACH)) {
            EntityLivingBase caught = null;
            if (target != null && !calm && horizontal(target) < 3.2D && facing().dotProduct(flat(target.getPositionVector().subtract(getPositionVector()))) > 0.4D) {
                caught = target;
            }
            if (caught == null) {
                sound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1.5F, 0.5F);
                whiffed(target, 1.2F);
                clock = GRAB_TICKS - (GRAB_MISS_TICKS - GRAB_REACH);
                grabbed = null;
            } else {
                grabbed = caught;
                dataManager.set(HELD, caught.getEntityId());
                sound(SoundEvents.ENTITY_IRONGOLEM_ATTACK, 2F, 0.6F);
                fx(MsgTribunalFx.CHAMPION_IMPACT, caught.posX, caught.posY + caught.height * 0.6D, caught.posZ, 0D, 0.2D, 0D, caught.getEntityId(), 0);
            }
        }
        if (grabbed != null && clock > GRAB_REACH && clock < GRAB_THROW) {
            // Lifted, struggling, overhead.
            double lift = MathHelper.clamp((clock - GRAB_REACH) / 14D, 0D, 1D);
            lift = lift * lift * (3D - 2D * lift);
            Vec3d f = facing();
            Vec3d hold = new Vec3d(posX + f.x * (1.3D - 0.9D * lift), posY + 1.6D + 2.4D * lift, posZ + f.z * (1.3D - 0.9D * lift));
            hold(grabbed, hold);
        }
        if (at(GRAB_THROW) && grabbed != null) {
            EntityLivingBase thrown = grabbed;
            grabbed = null;
            dataManager.set(HELD, -1);
            Vec3d f = facing();
            hold(thrown, new Vec3d(posX + f.x * 1.6D, posY + 3.4D, posZ + f.z * 1.6D));
            harm(thrown, 0.18F);
            push(thrown, f, 2.1D, 0.9D);
            fx(MsgTribunalFx.CHAMPION_IMPACT, thrown.posX, thrown.posY + 1D, thrown.posZ, f.x, 0.4D, f.z, thrown.getEntityId(), 1);
            sound(SoundEvents.ENTITY_IRONGOLEM_ATTACK, 2.5F, 0.4F);
            sound(SoundEvents.ENTITY_GENERIC_EXPLODE, 1.5F, 1.3F);
            if (thrown instanceof EntityPlayer && rand.nextInt(2) == 0) {
                say(LANDED[rand.nextInt(LANDED.length)]);
            }
        }
        if (clock >= GRAB_TICKS) {
            grabbed = null;
            dataManager.set(HELD, -1);
            recover(28);
        }
    }

    private void hold(EntityLivingBase victim, Vec3d at) {
        victim.fallDistance = 0F;
        victim.motionX = victim.motionY = victim.motionZ = 0D;
        EntityDestroyerArmor armor = EntityDestroyerArmor.carrier(victim);
        if (armor != null) {
            // Inside the Destroyer Armor, he hoists the whole armour, by its middle, with them in it.
            armor.put(at.x, at.y - armor.height * 0.45D, at.z);
        } else if (victim instanceof EntityPlayerMP) {
            ((EntityPlayerMP) victim).connection.setPlayerLocation(at.x, at.y, at.z, victim.rotationYaw, victim.rotationPitch);
        } else {
            victim.setPositionAndUpdate(at.x, at.y, at.z);
        }
    }

    /** Shoulder down, and he comes, straight, through whatever is in the way. */
    private void rush(@Nullable EntityLivingBase target) {
        if (clock < RUSH_GO) {
            if (target != null) {
                face(target, 0.45F);
                lunge = flat(target.getPositionVector().subtract(getPositionVector()));
            }
            if (at(3)) {
                sound(SoundEvents.ENTITY_POLAR_BEAR_WARNING, 2F, 0.5F);
            }
            return;
        }
        if (at(RUSH_GO)) {
            faceAlong(lunge);
            fx(MsgTribunalFx.CHAMPION_RUSH, posX, posY, posZ, lunge.x, 0D, lunge.z, 0, RUSH_STOP - RUSH_GO);
            sound(SoundEvents.ENTITY_ENDERDRAGON_FLAP, 2F, 0.5F);
        }
        if (clock < RUSH_STOP) {
            double speed = 0.85D + 0.35D * rage;
            motionX = lunge.x * speed;
            motionZ = lunge.z * speed;
            if (ticksExisted % 3 == 0) {
                sound(SoundEvents.ENTITY_IRONGOLEM_STEP, 1.5F, 0.6F);
            }
            for (EntityLivingBase victim : victims(2.4D)) {
                if (facing().dotProduct(flat(victim.getPositionVector().subtract(getPositionVector()))) > 0.2D) {
                    land(victim, 0.15F, lunge, 1.8D, 0.55D, 1);
                    clock = RUSH_STOP;
                    break;
                }
            }
            if (collidedHorizontally && clock > RUSH_GO + 3) {
                // Into a wall, shoulder first. The wall comes off worse.
                fx(MsgTribunalFx.CHAMPION_IMPACT, posX + lunge.x * 1.2D, posY + 1.5D, posZ + lunge.z * 1.2D, -lunge.x, 0D, -lunge.z, -1, 2);
                sound(SoundEvents.ENTITY_GENERIC_EXPLODE, 2F, 0.9F);
                clock = RUSH_STOP;
            }
            return;
        }
        if (at(RUSH_STOP)) {
            whiffed(target, 1F);
        }
        // Skidding to a stop.
        motionX = lunge.x * 0.2D * (RUSH_TICKS - clock) / (RUSH_TICKS - RUSH_STOP);
        motionZ = lunge.z * 0.2D * (RUSH_TICKS - clock) / (RUSH_TICKS - RUSH_STOP);
        if (clock >= RUSH_TICKS) {
            recover(26);
        }
    }

    /** Both fists up, a hop, and down on the ground: rings of force race out across it. */
    private void pound(@Nullable EntityLivingBase target) {
        if (target != null && clock < POUND_HIT - 6) {
            face(target, 0.3F);
        }
        if (at(6)) {
            motionY = 0.45D;
            sound(SoundEvents.ENTITY_IRONGOLEM_ATTACK, 2F, 0.45F);
        }
        if (at(POUND_HIT)) {
            fx(MsgTribunalFx.CHAMPION_QUAKE, posX, posY, posZ, 8D, 0, 0, 2, 24);
            fx(MsgTribunalFx.CHAMPION_CRACK, posX, posY, posZ, 6D, 0, 0, rand.nextInt(1000), 60);
            sound(EternitySounds.COMET_IMPACT, 4F, 0.6F);
            sound(SoundEvents.ENTITY_GENERIC_EXPLODE, 3F, 0.6F);
            quake(7.5D, 0.2F, 1.1D, true);
        }
        if (clock >= POUND_TICKS) {
            recover(28);
        }
    }

    /** Guard down, chin out: he dares you to hit him. Do, and he makes you pay for it. */
    private void counter(@Nullable EntityLivingBase target) {
        if (target != null && counterAt < 0F) {
            face(target, 0.25F);
        }
        if (counterAt >= 0F) {
            EntityPlayer victim = counterOn;
            if (victim != null) {
                face(victim, 0.7F);
            }
            if (prevClock < counterAt + COUNTER_STRIKE && clock >= counterAt + COUNTER_STRIKE) {
                if (victim != null && victim.getDistance(this) < 5D) {
                    land(victim, 0.25F, flat(victim.getPositionVector().subtract(getPositionVector())), 1.7D, 0.5D, 2);
                    say("Thank you. I needed you that close.");
                } else {
                    sound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 2F, 0.5F);
                }
            }
            if (clock >= counterAt + COUNTER_AFTER) {
                recover(18);
            }
            return;
        }
        if (at(30) && target instanceof EntityPlayer && horizontal(target) > 6D) {
            // You will not even come and take a swing at him.
            addRage(0.02F * (float) EternityConfig.champion.rageGain);
            say("I left my chin open. You walked away from it.");
        }
        if (clock >= COUNTER_TICKS) {
            recover(14);
        }
    }

    /** He fills his lungs, and roars: the air itself is thrown back, and so are you. The stone drinks it in. */
    private void roaring(@Nullable EntityLivingBase target) {
        if (target != null && clock < ROAR_GO) {
            face(target, 0.25F);
        }
        if (at(4)) {
            sound(SoundEvents.ENTITY_PLAYER_BREATH, 2F, 0.4F);
        }
        if (at(ROAR_GO)) {
            Vec3d head = brow();
            fx(MsgTribunalFx.CHAMPION_ROAR, head.x, head.y, head.z, 14D, 0, 0, getEntityId(), 40);
            fx(MsgTribunalFx.CHAMPION_ARCS, head.x, head.y, head.z, 0.9D, 0, 0, getEntityId(), 40);
            sound(SoundEvents.ENTITY_ENDERDRAGON_GROWL, 4F, 0.5F);
            sound(SoundEvents.ENTITY_LIGHTNING_THUNDER, 2F, 0.6F);
            for (EntityLivingBase victim : victims(10D)) {
                double d = victim.getDistance(this);
                float near = (float) (1D - d / 10D);
                push(victim, flat(victim.getPositionVector().subtract(getPositionVector())), 0.6D + 1.2D * near, 0.35D + 0.4D * near);
                if (ChampionDirector.isArmy(victim)) {
                    harm(victim, 1F);
                }
            }
            if (duelId != null && !showcase) {
                addRage(0.025F);
            }
        }
        if (clock >= ROAR_TICKS) {
            recover(18);
        }
    }

    /** Berserk: a leap, both fists locked overhead, and down like a hammer. The arena cracks. */
    private void hammer(@Nullable EntityLivingBase target) {
        if (target != null && clock < HAMMER_HIT - 4) {
            face(target, 0.4F);
            if (horizontal(target) > 3D) {
                walkToward(target.getPositionVector(), 0.22D);
            }
        }
        if (at(5)) {
            motionY = 0.7D;
            sound(SoundEvents.ENTITY_ENDERDRAGON_FLAP, 2F, 0.5F);
        }
        if (at(HAMMER_HIT)) {
            Vec3d f = facing();
            double x = posX + f.x * 2D, z = posZ + f.z * 2D;
            fx(MsgTribunalFx.CHAMPION_QUAKE, x, posY, z, 11D, 0, 0, 3, 30);
            fx(MsgTribunalFx.CHAMPION_CRACK, x, posY, z, 10D, 0, 0, rand.nextInt(1000), 100);
            sound(EternitySounds.COMET_IMPACT, 5F, 0.5F);
            sound(SoundEvents.ENTITY_GENERIC_EXPLODE, 4F, 0.5F);
            sound(EternitySounds.RIFT_IMPACT, 3F, 0.7F);
            quake(9.5D, 0.4F, 1.4D, true);
        }
        if (clock >= HAMMER_TICKS) {
            recover(26);
        }
    }

    // ------------------------------------------------------------------ Planet Breaker

    /**
     * Past all reason: he gathers himself, the stone blazing; leaps out of sight; hangs there,
     * both fists raised, a second sun; and comes down on Tamarata with everything he has.
     */
    private void breaker(@Nullable EntityLivingBase target) {
        if (clock < BREAKER_LEAP) {
            if (target != null) {
                face(target, 0.1F);
            }
            if (at(2)) {
                Vec3d brow = brow();
                fx(MsgTribunalFx.CHAMPION_ARCS, brow.x, brow.y, brow.z, 1D, 0, 0, getEntityId(), BREAKER_HIT + 20);
                sound(SoundEvents.ENTITY_ENDERDRAGON_GROWL, 5F, 0.4F);
                sound(SoundEvents.ENTITY_LIGHTNING_THUNDER, 3F, 0.5F);
            }
            if (ticksExisted % 8 == 0) {
                sound(SoundEvents.BLOCK_PORTAL_TRIGGER, 1F, 0.5F + clock / BREAKER_LEAP);
            }
            return;
        }
        if (at(BREAKER_LEAP)) {
            leapFrom = getPositionVector();
            fx(MsgTribunalFx.CHAMPION_LEAP, posX, posY, posZ, posX, posY + BREAKER_HEIGHT, posZ, 1, BREAKER_APEX - BREAKER_LEAP);
            fx(MsgTribunalFx.CHAMPION_QUAKE, posX, posY, posZ, 9D, 0, 0, 2, 24);
            sound(SoundEvents.ENTITY_GENERIC_EXPLODE, 4F, 0.5F);
            sound(EternitySounds.COMET_FLYBY, 4F, 0.6F);
            Vec3d mark = new Vec3d(posX, posY, posZ);
            fx(MsgTribunalFx.CHAMPION_MARK, mark.x, mark.y, mark.z, 14D, 0, 0, 0, BREAKER_HIT - BREAKER_LEAP);
        }
        if (leapFrom == null) {
            leapFrom = getPositionVector();
        }
        if (clock < BREAKER_APEX) {
            double p = (clock - BREAKER_LEAP) / (double) (BREAKER_APEX - BREAKER_LEAP);
            double up = 1D - Math.pow(1D - p, 2.4D);
            setPosition(leapFrom.x, leapFrom.y + BREAKER_HEIGHT * up, leapFrom.z);
        } else if (clock < BREAKER_DIVE) {
            // Hanging at the top, a second sun.
            setPosition(leapFrom.x, leapFrom.y + BREAKER_HEIGHT + Math.sin((clock - BREAKER_APEX) * 0.2D) * 0.4D, leapFrom.z);
            if (at(BREAKER_APEX + 4)) {
                sound(SoundEvents.ENTITY_WITHER_SPAWN, 4F, 0.6F);
            }
        } else if (clock < BREAKER_HIT) {
            double p = (clock - BREAKER_DIVE) / (double) (BREAKER_HIT - BREAKER_DIVE);
            setPosition(leapFrom.x, leapFrom.y + BREAKER_HEIGHT * (1D - p * p), leapFrom.z);
        }
        if (at(BREAKER_HIT)) {
            setPosition(leapFrom.x, leapFrom.y, leapFrom.z);
            setState(State.IMPACT);
            fx(MsgTribunalFx.CHAMPION_BREAK, posX, posY, posZ, EternityConfig.champion.destructionRadius, 0, 0, getEntityId(), 200);
            sound(EternitySounds.COMET_BLAST, 8F, 0.5F);
            sound(EternitySounds.COMET_IMPACT, 8F, 0.4F);
            sound(SoundEvents.ENTITY_GENERIC_EXPLODE, 8F, 0.4F);
            for (EntityLivingBase victim : victims(40D)) {
                double d = victim.getDistance(this);
                push(victim, flat(victim.getPositionVector().subtract(getPositionVector())), 1.8D * (1D - d / 45D), 1.2D);
            }
            if (managed && duelId != null && !showcase) {
                ChampionDirector.INSTANCE.planetBreaker(this);
            } else {
                // Away from Tamarata, or only showing off: everything but the end of the world.
                setRage(0.55F);
                stateTicks = 0;
            }
        }
    }

    // ------------------------------------------------------------------ reactions, knockdown

    private void reacting(@Nullable EntityLivingBase target) {
        if (target != null) {
            face(target, 0.4F);
        }
        int length = getState() == State.SLIP ? SLIP_TICKS : GUARD_TICKS;
        if (getState() == State.SLIP && clock < 5) {
            // Weaving out of the way, a little back and to the side.
            Vec3d f = facing();
            motionX = -f.x * 0.1D + f.z * 0.08D * strafe;
            motionZ = -f.z * 0.1D - f.x * 0.08D * strafe;
        }
        if (clock >= length) {
            boolean slipped = getState() == State.SLIP;
            setState(State.IDLE);
            // Having slipped you, he is already coming back with something.
            cooldown = slipped || rand.nextInt(3) == 0 ? 0 : 4;
            if (rand.nextInt(3) == 0 && target != null && horizontal(target) < 3.6D) {
                start(rand.nextBoolean() ? Move.HOOK : Move.JAB_CROSS, target);
            }
        }
    }

    private void knockdown() {
        grabbed = null;
        dataManager.set(HELD, -1);
        setState(State.KNOCKDOWN);
        sound(SoundEvents.ENTITY_IRONGOLEM_DEATH, 2F, 0.5F);
        sound(SoundEvents.ENTITY_GENERIC_EXPLODE, 1.5F, 0.6F);
        fx(MsgTribunalFx.CHAMPION_QUAKE, posX, posY, posZ, 4D, 0, 0, 1, 16);
    }

    private void knockedDown() {
        if (at(12)) {
            sound(SoundEvents.ENTITY_IRONGOLEM_STEP, 3F, 0.4F);
            fx(MsgTribunalFx.CHAMPION_QUAKE, posX, posY, posZ, 5D, 0, 0, 1, 20);
        }
        if (at(34)) {
            say("Hah. Now there is a punch.");
        }
        if (clock >= KNOCKDOWN_TICKS) {
            setState(State.RISE);
            Vec3d brow = brow();
            fx(MsgTribunalFx.CHAMPION_ARCS, brow.x, brow.y, brow.z, 0.8D, 0, 0, getEntityId(), RISE_TICKS);
            sound(SoundEvents.BLOCK_PORTAL_TRIGGER, 1.5F, 0.6F);
        }
    }

    private void rising(@Nullable EntityLivingBase target) {
        setHealth(Math.min(getMaxHealth(), getHealth() + getMaxHealth() / RISE_TICKS * 1.2F));
        if (target != null && clock > 24) {
            face(target, 0.2F);
        }
        if (at(30)) {
            Vec3d head = brow();
            fx(MsgTribunalFx.CHAMPION_ROAR, head.x, head.y, head.z, 9D, 0, 0, getEntityId(), 30);
            sound(SoundEvents.ENTITY_ENDERDRAGON_GROWL, 3F, 0.6F);
            say(RISEN[rand.nextInt(RISEN.length)]);
            if (!showcase) {
                addRage(0.05F);
            }
        }
        if (clock >= RISE_TICKS) {
            setHealth(getMaxHealth());
            recover(10);
        }
    }

    /** Hanging in the wreck of his planet, turning slowly over, going nowhere. */
    private void drifting() {
        motionX = motionY = motionZ = 0D;
        if (anchor != null) {
            double t = ticksExisted * 0.01D;
            setPosition(anchor.x + Math.sin(t * 1.3D) * 1.5D, anchor.y + Math.sin(t * 2.1D) * 1.2D, anchor.z + Math.cos(t * 0.9D) * 1.5D);
        }
        EntityPlayer near = world.getClosestPlayer(posX, posY, posZ, 48D, false);
        if (near != null) {
            face(near, 0.04F);
        }
    }

    // ------------------------------------------------------------------ landing blows

    private void step(double distance) {
        Vec3d f = facing();
        motionX += f.x * distance;
        motionZ += f.z * distance;
    }

    private void strike(@Nullable EntityLivingBase target, float fraction, double knock, double lift, int kind, double reach) {
        strike(target, fraction, knock, lift, kind, reach, facing());
    }

    /** One blow, at whatever is in front of him and within reach. */
    private void strike(@Nullable EntityLivingBase target, float fraction, double knock, double lift, int kind, double reach, Vec3d drive) {
        Vec3d f = facing();
        boolean hit = false;
        for (EntityLivingBase victim : victims(reach + 1D)) {
            // Inside the Destroyer Armor, the armour is what stands in reach of him.
            Entity body = EntityDestroyerArmor.mass(victim);
            Vec3d to = flat(body.getPositionVector().subtract(getPositionVector()));
            if (horizontal(body) <= reach + body.width * 0.5D && f.dotProduct(to) > 0.35D
                    && Math.abs(body.posY - posY) < 3.4D) {
                land(victim, fraction, drive, knock, lift, kind);
                hit = true;
            }
        }
        if (!hit) {
            Vec3d fist = new Vec3d(posX + f.x * 2.2D, posY + (kind == 1 && lift > 1D ? 3D : 2.1D), posZ + f.z * 2.2D);
            if (calm) {
                fx(MsgTribunalFx.CHAMPION_IMPACT, fist.x, fist.y, fist.z, f.x, 0D, f.z, -1, kind);
            }
            sound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1.6F, kind == 0 ? 0.7F : 0.45F);
            whiffed(target, kind == 0 ? 0.8F : 1.2F);
        }
    }

    /** A blow that found someone: the flash, the sound, the hit-stop, and wherever it sends them. */
    private void land(EntityLivingBase victim, float fraction, Vec3d drive, double knock, double lift, int kind) {
        Vec3d at = new Vec3d(victim.posX, victim.posY + victim.height * 0.6D, victim.posZ).add(new Vec3d(posX, posY + 2D, posZ)).scale(0.5D);
        fx(MsgTribunalFx.CHAMPION_IMPACT, at.x, at.y, at.z, drive.x, lift > 1D ? 0.8D : 0D, drive.z, victim.getEntityId(), kind);
        sound(SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, 2.2F, kind == 0 ? 0.7F : 0.5F);
        sound(SoundEvents.ENTITY_IRONGOLEM_ATTACK, 2F, kind == 0 ? 0.9F : 0.55F);
        if (kind > 0) {
            sound(EternitySounds.COMET_IMPACT, 2.5F, kind == 2 ? 0.8F : 1.2F);
        }
        harm(victim, fraction);
        push(victim, drive, knock, lift);
        hitStop = kind == 0 ? 1 : kind == 1 ? 3 : 5;
        if (victim instanceof EntityPlayer && kind > 0 && rand.nextInt(4) == 0) {
            say(LANDED[rand.nextInt(LANDED.length)]);
        }
    }

    /** A blow that found nothing. Slipping him, while close enough to be hit, is what makes him angriest of all. */
    private void whiffed(@Nullable EntityLivingBase target, float weight) {
        if (target instanceof EntityPlayer && duelId != null && !showcase && target.getDistance(this) < 6.5D) {
            addRage(0.012F * weight * (float) EternityConfig.champion.rageGain);
            if (rand.nextInt(4) == 0) {
                say(DODGED[rand.nextInt(DODGED.length)]);
            }
        }
    }

    /** The ground he struck throws everyone standing on it, harder the closer they are. */
    private void quake(double radius, float fraction, double strength, boolean groundedOnly) {
        for (EntityLivingBase victim : victims(radius)) {
            // Inside the Destroyer Armor, it is the armour standing on the ground that the ground throws.
            Entity feet = EntityDestroyerArmor.mass(victim);
            double d = feet.getDistance(posX, posY, posZ);
            if (d > radius || groundedOnly && !feet.onGround && feet.posY - floor(feet.posX, feet.posY + 0.5D, feet.posZ) > 1.2D) {
                continue;
            }
            float near = (float) (1D - d / radius);
            harm(victim, fraction * (0.35F + 0.65F * near));
            push(victim, flat(victim.getPositionVector().subtract(getPositionVector())), strength * (0.4D + 0.6D * near), 0.45D + 0.5D * near);
        }
        if (groundedOnly) {
            whiffedIfClear(radius);
        }
    }

    private void whiffedIfClear(double radius) {
        EntityPlayer target = duelId == null ? null : world.getPlayerEntityByUUID(duelId);
        if (target instanceof EntityPlayer && target.getDistance(this) < radius + 4D && target.getDistance(this) > radius) {
            whiffed(target, 1F);
        }
    }

    private List<EntityLivingBase> victims(double range) {
        List<EntityLivingBase> found = new ArrayList<>();
        AxisAlignedBB box = getEntityBoundingBox().grow(range, 4D, range);
        for (EntityLivingBase other : world.getEntitiesWithinAABB(EntityLivingBase.class, box)) {
            if (other == this || other.isDead || other.getHealth() <= 0F) {
                continue;
            }
            if (other instanceof EntityPlayer ? isFair((EntityPlayer) other) : ChampionDirector.isArmy(other)) {
                found.add(other);
            }
        }
        return found;
    }

    /** A share of the victim's maximum health, through armour. The armies that come for him he simply ends. */
    private void harm(EntityLivingBase victim, float fraction) {
        if (calm) {
            return;
        }
        if (!(victim instanceof EntityPlayer)) {
            victim.hurtResistantTime = 0;
            victim.attackEntityFrom(EternityDamage.champion(this), victim.getMaxHealth() * Math.max(1F, fraction * 6F));
            return;
        }
        float scale = (float) EternityConfig.champion.damageScale * (stageOf(rage) >= 3 ? 1.5F : 1F);
        EntityDestroyerArmor.strike(victim, EternityDamage.champion(this), fraction * scale);
    }

    private void push(EntityLivingBase victim, Vec3d direction, double strength, double lift) {
        if (victim instanceof EntityPlayer && ((EntityPlayer) victim).isSpectator()) {
            return;
        }
        double scale = calm ? 0.25D : 1D;
        if (EntityDestroyerArmor.carrier(victim) != null) {
            EntityDestroyerArmor.shove(victim, direction.x * strength * scale, (direction.y * strength + lift) * scale, direction.z * strength * scale);
            return;
        }
        victim.motionX += direction.x * strength * scale;
        victim.motionY = Math.max(victim.motionY, 0D) + (direction.y * strength + lift) * scale;
        victim.motionZ += direction.z * strength * scale;
        victim.velocityChanged = true;
        victim.fallDistance = 0F;
    }

    // ------------------------------------------------------------------ being hit

    /**
     * Only players' blows count. From the front he blocks most of them and slips most of the
     * rest; what gets through takes a sliver of him at most. From behind, or while he is
     * recovering from a big swing, he is open. Hit him from range and he only gets angrier; hit
     * him up close and he respects it. In his counter stance, hitting him is a mistake.
     */
    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (world.isRemote) {
            return false;
        }
        State state = getState();
        Entity attacker = source.getTrueSource();
        if (!(attacker instanceof EntityPlayer)) {
            if (attacker instanceof EntityLivingBase && state == State.IDLE && rand.nextInt(4) == 0) {
                guarded((EntityLivingBase) attacker, 0);
                setState(State.GUARD);
            }
            return false;
        }
        EntityPlayer player = (EntityPlayer) attacker;
        if (state == State.KNOCKDOWN || state == State.RISE || state == State.BREAKER || state == State.IMPACT || state == State.DRIFT
                || state == State.BOARD || state == State.CHALLENGE) {
            return false;
        }
        if (managed && duelId == null) {
            ChampionDirector.INSTANCE.provoked(this, player);
        } else if (duelId == null) {
            challenge(player);
        }
        boolean ranged = source.isProjectile() || player.getDistance(this) > 5D;
        if (ranged) {
            addRage(0.012F * (float) EternityConfig.champion.rageGain);
            if (rand.nextInt(3) == 0) {
                say(CHEAP[rand.nextInt(CHEAP.length)]);
            }
        } else {
            lastHonour = ticksExisted;
            addRage(-0.01F * (float) EternityConfig.champion.rageDecay);
            if (rand.nextInt(12) == 0) {
                say(HONOUR[rand.nextInt(HONOUR.length)]);
            }
        }
        if (state == State.COUNTER && clock >= COUNTER_OPEN && counterAt < 0F && !ranged) {
            counterAt = clock;
            counterOn = player;
            guarded(player, 2);
            sound(SoundEvents.ENTITY_IRONGOLEM_ATTACK, 2F, 0.8F);
            return false;
        }
        Vec3d to = flat(player.getPositionVector().subtract(getPositionVector()));
        boolean front = facing().dotProduct(to) > 0.15D;
        boolean open = !front || vulnerable(state);
        if (!open) {
            float block = 0.62F - 0.3F * rage, slip = 0.26F;
            float roll = rand.nextFloat();
            if (roll < block || ranged && roll < 0.9F) {
                guarded(player, 0);
                if (canAct()) {
                    setState(State.GUARD);
                }
                if (rand.nextInt(10) == 0) {
                    say(BLOCKED[rand.nextInt(BLOCKED.length)]);
                }
                return false;
            }
            if (roll < block + slip) {
                guarded(player, 1);
                if (canAct()) {
                    strafe = rand.nextBoolean() ? 1D : -1D;
                    setState(State.SLIP);
                }
                return false;
            }
        }
        float cap = (float) (getMaxHealth() * EternityConfig.champion.maxHitFraction) * (open ? 2F : 1F);
        float dealt = Math.min(amount, cap);
        if (!(dealt > 0F)) {
            return false;
        }
        world.setEntityState(this, (byte) 2);
        float left = getHealth() - dealt;
        if (left <= 1F) {
            setHealth(1F);
            knockdown();
            say("Oh. Oh, that was good.");
            return true;
        }
        setHealth(left);
        return true;
    }

    /** Recovering from a big swing, he is open for a moment. */
    private boolean vulnerable(State state) {
        switch (state) {
            case HAYMAKER:
                return clock > HAY_LAND + 2;
            case RUSH:
                return clock > RUSH_STOP;
            case POUND:
                return clock > POUND_HIT + 2;
            case HAMMER:
                return clock > HAMMER_HIT + 2;
            case GRAB:
                return grabbed == null && clock > GRAB_REACH;
            case ROAR:
                return clock > ROAR_GO + 20;
            default:
                return false;
        }
    }

    /** Your blow meets his guard (0), whistles past him as he slips it (1), or is caught and turned (2). */
    private void guarded(EntityLivingBase attacker, int how) {
        Vec3d to = flat(attacker.getPositionVector().subtract(getPositionVector()));
        Vec3d at = new Vec3d(posX + to.x * 1.1D, posY + 2.4D, posZ + to.z * 1.1D);
        fx(MsgTribunalFx.CHAMPION_GUARD, at.x, at.y, at.z, to.x, 0D, to.z, attacker.getEntityId(), how);
        if (how == 0) {
            sound(SoundEvents.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1F, 0.8F + rand.nextFloat() * 0.2F);
        } else if (how == 1) {
            sound(SoundEvents.ENTITY_PLAYER_ATTACK_WEAK, 1.5F, 0.6F);
        } else {
            sound(SoundEvents.ENTITY_IRONGOLEM_HURT, 1.5F, 1.2F);
        }
    }

    /** Right-clicked: with a rocket in hand, a trade; empty-handed, a slap in the face. */
    @Override
    protected boolean processInteract(EntityPlayer player, EnumHand hand) {
        if (!world.isRemote && hand == EnumHand.MAIN_HAND && player instanceof EntityPlayerMP) {
            return ChampionDirector.INSTANCE.touched(this, (EntityPlayerMP) player);
        }
        return true;
    }

    @Override
    public void onKillCommand() {
        setDead();
    }

    @Override
    public void onDeath(DamageSource cause) {
        setHealth(1F);
        if (!world.isRemote && getState() != State.KNOCKDOWN) {
            knockdown();
        }
    }

    // ------------------------------------------------------------------ helpers

    private void fx(byte type, double x, double y, double z, double a, double b, double c, int id, int time) {
        EternityNetwork.sendToDimension(new MsgTribunalFx(type, x, y, z, a, b, c, id, time), dimension);
    }

    private void sound(SoundEvent sound, float volume, float pitch) {
        if (volume > 0F) {
            world.playSound(null, posX, posY + 1.6D, posZ, sound, SoundCategory.HOSTILE, volume, pitch);
        }
    }

    /** He speaks to everyone near enough to hear him. Never twice in two seconds. */
    public void say(String line) {
        if (line.isEmpty() || ticksExisted - lastSpoke < 40) {
            return;
        }
        lastSpoke = ticksExisted;
        speak(world, this, line, 128D);
    }

    /** One of his lines, to everyone within range of where he is. */
    public static void speak(World world, Entity at, String line, double range) {
        TextComponentString name = new TextComponentString("<The Champion> ");
        name.getStyle().setColor(TextFormatting.DARK_PURPLE).setBold(true);
        TextComponentString text = new TextComponentString(line);
        text.getStyle().setColor(TextFormatting.LIGHT_PURPLE).setBold(false);
        name.appendSibling(text);
        for (EntityPlayer player : world.playerEntities) {
            if (player.getDistance(at) < range) {
                player.sendMessage(name);
            }
        }
    }

    public Vec3d facing() {
        float yaw = (float) Math.toRadians(rotationYaw);
        return new Vec3d(-MathHelper.sin(yaw), 0D, MathHelper.cos(yaw));
    }

    /** The stone in his brow. */
    public Vec3d brow() {
        Vec3d f = facing();
        return new Vec3d(posX + f.x * 0.35D, posY + 3.0D, posZ + f.z * 0.35D);
    }

    private double horizontal(Entity other) {
        double dx = other.posX - posX, dz = other.posZ - posZ;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static Vec3d flat(Vec3d v) {
        Vec3d f = new Vec3d(v.x, 0D, v.z);
        return f.lengthSquared() < 1.0E-6 ? new Vec3d(0D, 0D, 1D) : f.normalize();
    }

    /** The top of the ground under a point, looking down from y; far below if there is none. */
    private double floor(double x, double y, double z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(MathHelper.floor(x), MathHelper.floor(y), MathHelper.floor(z));
        for (int i = 0; i < 48 && pos.getY() > 0; i++) {
            IBlockState state = world.getBlockState(pos);
            if (state.getMaterial().isLiquid()) {
                return pos.getY() + 1D;
            }
            AxisAlignedBB box = state.getCollisionBoundingBox(world, pos);
            if (box != null && box != net.minecraft.block.Block.NULL_AABB) {
                return pos.getY() + box.maxY;
            }
            pos.move(net.minecraft.util.EnumFacing.DOWN);
        }
        return y - 48D;
    }

    private void face(Entity target, float rate) {
        faceToward(new Vec3d(target.posX, target.posY + target.getEyeHeight(), target.posZ), rate);
    }

    private void faceToward(Vec3d at, float rate) {
        double dx = at.x - posX, dz = at.z - posZ;
        double dy = at.y - (posY + getEyeHeight());
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90F;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        rotationYaw += MathHelper.wrapDegrees(yaw - rotationYaw) * rate;
        rotationYawHead = rotationYaw;
        rotationPitch += (MathHelper.clamp(pitch, -50F, 50F) - rotationPitch) * rate;
    }

    private void faceAlong(Vec3d v) {
        if (v.x * v.x + v.z * v.z > 1.0E-6) {
            rotationYaw = (float) Math.toDegrees(Math.atan2(v.z, v.x)) - 90F;
            rotationYawHead = rotationYaw;
        }
    }

    // ------------------------------------------------------------------ client: how he moves

    /** How brightly the stone burns in each state, on top of his rage. */
    private float glowFor(State state, float t) {
        float rage = getRage();
        float base = 0.25F + 0.75F * rage;
        switch (state) {
            case ROAR:
                return t > ROAR_GO && t < ROAR_GO + 30 ? 1.3F : base;
            case BREAKER:
                return 0.8F + Math.min(1F, t / BREAKER_LEAP) * 0.9F;
            case IMPACT:
                return 1.4F;
            case RISE:
                return 1.1F;
            case KNOCKDOWN:
                return 0.1F;
            case DRIFT:
            case BOARD:
                return 0.3F;
            case HAMMER:
            case POUND:
                return Math.max(base, t > 10 && t < 30 ? 1F : base);
            default:
                return base;
        }
    }

    private void clientTick() {
        clientStateTicks++;
        float t = clientStateTicks * getTempo();
        double vx = posX - prevPosX, vy = posY - prevPosY, vz = posZ - prevPosZ;
        velX += (vx - velX) * 0.5D;
        velY += (vy - velY) * 0.5D;
        velZ += (vz - velZ) * 0.5D;
        double ground = Math.sqrt(velX * velX + velZ * velZ);
        float yaw = (float) Math.toRadians(rotationYaw);
        double fwd = -velX * MathHelper.sin(yaw) + velZ * MathHelper.cos(yaw);
        double side = velX * MathHelper.cos(yaw) + velZ * MathHelper.sin(yaw);

        // Footwork: a stride for every couple of blocks, and a boxer's bounce when standing.
        prevWalkPhase = walkPhase;
        prevWalkAmount = walkAmount;
        boolean walking = getState() == State.IDLE || getState() == State.RUSH || getState() == State.HOOK || getState() == State.GRAB
                || getState() == State.JAB_CROSS || getState() == State.UPPERCUT || getState() == State.HAMMER;
        float want = walking && !isAirborne() ? (float) MathHelper.clamp(ground / 0.22D, 0D, 1.6D) : 0F;
        walkAmount += (want - walkAmount) * 0.25F;
        walkPhase += (float) Math.min(0.6D, ground * 1.55D) * Math.signum((float) fwd + 0.001F);

        prevBodyPitch = bodyPitch;
        prevBodyRoll = bodyRoll;
        float wantPitch, wantRoll;
        if (getState() == State.DRIFT || getState() == State.BOARD) {
            // Tumbling, slowly, end over end.
            wantPitch = bodyPitch + 0.55F;
            wantRoll = MathHelper.sin(ticksExisted * 0.013F) * 30F;
            bodyPitch = wantPitch;
            bodyRoll += (wantRoll - bodyRoll) * 0.05F;
        } else {
            if (bodyPitch > 180F || bodyPitch < -180F) {
                bodyPitch = MathHelper.wrapDegrees(bodyPitch);
                prevBodyPitch = bodyPitch;
            }
            if (getState() == State.HAYMAKER && isAirborne()) {
                wantPitch = 72F;
            } else if (getState() == State.BREAKER && t >= BREAKER_DIVE && t < BREAKER_HIT) {
                wantPitch = 180F;
            } else {
                wantPitch = (float) MathHelper.clamp(fwd * 30D, -10D, 20D);
            }
            wantRoll = (float) MathHelper.clamp(-side * 25D, -10D, 10D);
            bodyPitch += (wantPitch - bodyPitch) * (getState() == State.BREAKER ? 0.3F : 0.25F);
            bodyRoll += (wantRoll - bodyRoll) * 0.2F;
        }

        prevShownRage = shownRage;
        shownRage += (getRage() - shownRage) * 0.1F;
        prevGlow = glow;
        glow += (glowFor(getState(), t) - glow) * 0.15F;

        for (int i = TRAIL - 1; i > 0; i--) {
            System.arraycopy(trail[i - 1], 0, trail[i], 0, 6);
        }
        double speed = Math.sqrt(velX * velX + velY * velY + velZ * velZ);
        trail[0][0] = posX;
        trail[0][1] = posY;
        trail[0][2] = posZ;
        trail[0][3] = rotationYaw;
        trail[0][4] = bodyPitch;
        trail[0][5] = MathHelper.clamp((speed - 0.45D) / 1.2D, 0D, 1D);
        trailCount = Math.min(TRAIL, trailCount + 1);

        maneTick(fwd, side, speed);
    }

    /**
     * His mane streams back from his head on springs, as Adam Warlock's cape does: each lock
     * swings toward the one above it, so a ripple runs down it. It flies when he leaps, lifts
     * when he roars, and floats free once there is no up or down any more.
     */
    private void maneTick(double fwd, double side, double speed) {
        float t = ticksExisted;
        State state = getState();
        boolean adrift = state == State.DRIFT || state == State.BOARD;
        double p = Math.toRadians(bodyPitch);
        double airF = -fwd * 1.1D, airU = (adrift ? 0D : -0.1D) - velY * 1.1D;
        double down = -(airF * Math.sin(p) + airU * Math.cos(p));
        double back = -airF * Math.cos(p) + airU * Math.sin(p);
        // Swept back even standing still: it never hangs straight down his back.
        float theta = (float) MathHelper.clamp(Math.atan2(back, down) + 0.45D, 0.55D, 1.55D);
        if (adrift) {
            theta = 0.9F + MathHelper.sin(t * 0.05F) * 0.4F;
        }
        if (state == State.ROAR && clientStateTicks * getTempo() > ROAR_GO && clientStateTicks * getTempo() < ROAR_GO + 32) {
            // The roar blows it straight back.
            theta = 1.45F;
        }
        float strength = (float) Math.min(1D, speed * 0.8D + (state == State.ROAR ? 0.6D : 0D) + getRage() * 0.15D);
        prevManeSway = maneSway;
        maneSway += ((float) MathHelper.clamp(-side * 0.9D, -0.5D, 0.5D) + MathHelper.sin(t * 0.04F) * 0.05F - maneSway) * 0.2F;
        for (int i = 0; i < MANE; i++) {
            prevMane[i] = mane[i];
        }
        for (int i = 0; i < MANE; i++) {
            float flutter = MathHelper.sin(t * (0.22F + 0.5F * strength) - i * 0.9F) * (0.03F + 0.16F * strength) * (0.4F + i * 0.18F)
                    + MathHelper.sin(t * 0.06F + i * 0.7F) * 0.03F;
            float goal = (i == 0 ? theta : prevMane[i - 1] * 0.75F + theta * 0.25F) + flutter;
            maneSpin[i] += (goal - mane[i]) * 0.28F;
            maneSpin[i] *= 0.64F;
            mane[i] += maneSpin[i];
            if (i > 0) {
                mane[i] = mane[i - 1] + MathHelper.clamp(mane[i] - mane[i - 1], -0.45F, 0.6F);
            }
        }
    }

    // ------------------------------------------------------------------ the rest

    @Override
    public void knockBack(Entity source, float strength, double x, double z) {
    }

    @Override
    public boolean isPotionApplicable(PotionEffect effect) {
        return false;
    }

    @Override
    protected void collideWithNearbyEntities() {
    }

    @Override
    public boolean canBePushed() {
        return false;
    }

    @Override
    public boolean isNonBoss() {
        return false;
    }

    @Override
    public boolean isImmuneToExplosions() {
        return true;
    }

    @Override
    public boolean isEntityInsideOpaqueBlock() {
        return false;
    }

    @Override
    public void fall(float distance, float damageMultiplier) {
    }

    @Override
    protected boolean canDespawn() {
        return false;
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Nullable
    @Override
    public Entity changeDimension(int dimension, ITeleporter teleporter) {
        return null;
    }

    /** Never written to chunks: the director puts him wherever he should be, every time someone comes. */
    @Override
    public boolean writeToNBTOptional(NBTTagCompound compound) {
        return false;
    }

    @Override
    public boolean isInRangeToRenderDist(double distance) {
        return distance < 400D * 400D;
    }

    @Override
    public SoundCategory getSoundCategory() {
        return SoundCategory.HOSTILE;
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_IRONGOLEM_HURT;
    }

    @Override
    protected float getSoundPitch() {
        return 0.55F + rand.nextFloat() * 0.1F;
    }

    @Override
    public float getEyeHeight() {
        return 2.85F;
    }

    @Override
    public void addTrackingPlayer(EntityPlayerMP player) {
        super.addTrackingPlayer(player);
        bossInfo.addPlayer(player);
    }

    @Override
    public void removeTrackingPlayer(EntityPlayerMP player) {
        super.removeTrackingPlayer(player);
        bossInfo.removePlayer(player);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return getEntityBoundingBox().grow(4D);
    }
}
