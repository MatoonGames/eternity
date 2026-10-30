package com.matoon.eternity.entity;

import com.matoon.eternity.EternityConfig;
import com.matoon.eternity.EternitySounds;
import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.network.MsgTribunalFx;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
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
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
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
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Adam Warlock: grown in a cocoon by the Sovereign to be perfect, and sure that he is. The
 * stones are his by right, and whoever has been collecting them is in his way. He is a
 * man's size and fights like a comet: golden, burning, never still.
 * <ul>
 * <li><b>Fly-by</b>: he coils in the air, then leaves with a sonic boom, fist first, through
 * you and out the other side, banks round, and comes again.</li>
 * <li><b>Combo</b>: in close, a jab, a cross, an uppercut that launches you, and if it lands,
 * he is above you before you stop rising and hammers you back down.</li>
 * <li><b>Quantum volley</b>: bolts of quantum energy from alternating palms.</li>
 * <li><b>Quantum beam</b>: palms together, a sun between them, then a beam that follows you.</li>
 * <li><b>Meteor dive</b>: he climbs out of sight, a ring of gold marks the ground, and he
 * comes down fists first and lands like a hero.</li>
 * <li><b>Cocoon</b>: badly hurt, he wraps himself in gold. Break it before it hatches or he
 * comes out healed and <i>evolved</i>: brighter, faster, and harder.</li>
 * </ul>
 * He cannot be killed, only driven off: beaten, he swears it is not over and leaves.
 * Everything everyone sees is synced: what he is doing, whether he has evolved, and how
 * much of his cocoon still holds. How he moves (his lean into flight, his banking, his
 * cape, his trail) is worked out on each client from where he goes.
 */
public class EntityAdamWarlock extends EntityLiving {

    public enum State {
        ARRIVE, HOVER, TAUNT,
        WINDUP, FLYBY, TURN,
        COMBO, VOLLEY, BEAM,
        RISE, DIVE, SLAM,
        COCOON, EMERGE,
        STAGGER, RETREAT,
        // Only when someone else is directing him: holding a piece up over a board, standing on the ground, walking on it.
        CARRY, STAND, WALK
    }

    /** What he can be told to do, for testing. */
    public enum Move { TAUNT, FLYBY, COMBO, VOLLEY, BEAM, DIVE, COCOON }

    // How long everything takes, in ticks; the model times its frames to the same numbers.
    public static final int ARRIVE_STREAK = 14;
    public static final int ARRIVE_TICKS = 50;
    public static final int TAUNT_TICKS = 56;
    public static final int WINDUP_TICKS = 16;
    public static final int WINDUP_EVOLVED = 11;
    public static final int WINDUP_CHAINED = 9;
    public static final int TURN_TICKS = 18;
    public static final int COMBO_JAB = 6;
    public static final int COMBO_CROSS = 12;
    public static final int COMBO_UPPER = 20;
    public static final int COMBO_BLINK = 27;
    public static final int COMBO_HAMMER = 32;
    public static final int COMBO_TICKS = 44;
    public static final int VOLLEY_GATHER = 14;
    public static final int VOLLEY_GAP = 7;
    public static final int BEAM_CHARGE = 26;
    public static final int BEAM_FIRE = 40;
    public static final int BEAM_TICKS = 80;
    public static final int RISE_CLIMB = 16;
    public static final int RISE_TICKS = 28;
    public static final int SLAM_TICKS = 26;
    public static final int COCOON_WRAP = 30;
    public static final int COCOON_TICKS = 190;
    public static final int COCOON_SHOWCASE = 110;
    public static final int EMERGE_TICKS = 50;
    public static final int STAGGER_TICKS = 34;
    public static final int RETREAT_LAUNCH = 50;
    public static final int RETREAT_TICKS = 90;

    /** How many points of each trail he keeps, client side. */
    public static final int TRAIL = 12;
    /** Segments in his cape, top to bottom. */
    public static final int CAPE = 4;

    private static final DataParameter<Byte> STATE = EntityDataManager.createKey(EntityAdamWarlock.class, DataSerializers.BYTE);
    private static final DataParameter<Boolean> EVOLVED = EntityDataManager.createKey(EntityAdamWarlock.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Float> COCOON = EntityDataManager.createKey(EntityAdamWarlock.class, DataSerializers.FLOAT);

    private static final Move[] SHOWCASE = {Move.TAUNT, Move.FLYBY, Move.COMBO, Move.VOLLEY, Move.DIVE, Move.BEAM, Move.COCOON,
            Move.FLYBY, Move.COMBO, Move.VOLLEY, Move.BEAM, Move.DIVE};

    private static final String[] ARRIVALS = {"So you're the one who's been collecting them.",
            "You've been picking up things that don't belong to you.", "Those stones are mine. I was made to carry them."};
    private static final String[] TAUNTS = {"I was made to be perfect. What were you made for?", "Is that it? I expected more.",
            "Hand them over and I'll make this quick.", "Mother said you'd be beneath me. She was right."};
    private static final String[] LANDED = {"Too slow.", "Stay down.", "Did you see that? Of course you didn't."};
    private static final String[] HURT = {"You... you hit me.", "Nobody hits me!", "That's it. No more holding back."};
    private static final String[] LEAVING = {"This isn't over. The stones are MINE.", "Next time I'll be ready for you.",
            "Enjoy them while you can."};

    private final BossInfoServer bossInfo = (BossInfoServer) new BossInfoServer(
            new TextComponentString("Adam Warlock"), BossInfo.Color.YELLOW, BossInfo.Overlay.NOTCHED_10);

    // ------------------------------------------------------------------ server state
    @Nullable
    private UUID targetId;
    /** Pulls his punches: nothing he does harms anyone. */
    private boolean calm;
    /** Runs through every move in turn, harmlessly, for looking at him. */
    private boolean showcase;
    private int showIndex;
    @Nullable
    private Move pendingMove;
    private Move lastMove = Move.TAUNT;
    private int stateTicks;
    private int cooldown = 40;
    private int lonely;
    private double orbit;
    private int passes;
    private Vec3d launchAt;
    private Vec3d dir = new Vec3d(0D, 0D, 1D);
    private Vec3d velocity = Vec3d.ZERO;
    private double speed;
    private double travelled, distance;
    private boolean landed;
    private int hitStop;
    private boolean launched;
    private Vec3d aim;
    private Vec3d mark;
    private Vec3d arriveFrom, arriveTo;
    private float cocoonHealth;
    private boolean cocoonUsed;
    private int lastSpoke = -1000;
    private int staggerFor = STAGGER_TICKS;
    private int nextBolt;
    private final List<Bolt> bolts = new ArrayList<>();
    /** Directed by someone else (the Grandmaster's board): he fights nobody, and goes and does what he is told. */
    private boolean scripted;
    @Nullable
    private Vec3d scriptAt, scriptFace;
    private double scriptRate = 0.1D;

    /** A bolt of quantum energy in flight. Clients draw it from where and how fast it left his hand. */
    private static final class Bolt {
        final int id;
        Vec3d pos;
        final Vec3d vel;
        int age;

        Bolt(int id, Vec3d pos, Vec3d vel) {
            this.id = id;
            this.pos = pos;
            this.vel = vel;
        }
    }

    // ------------------------------------------------------------------ client state (how he moves, worked out where he is drawn)
    /** Client: ticks since the synced state last changed. */
    public int clientStateTicks;
    /** Client: what he was doing before this, so a fly-by chained out of a turn can wind up faster. */
    public State previousState = State.ARRIVE;
    private State seenState = State.ARRIVE;
    /** Client: his velocity, smoothed, in blocks per tick. */
    public double velX, velY, velZ;
    /** Client: how far he leans into his flight and banks through turns, in degrees. */
    public float bodyPitch, prevBodyPitch, bodyRoll, prevBodyRoll;
    /** Client: how brightly his power burns, 0 to a little over 1. */
    public float power, prevPower;
    /** Client: each cape segment's angle back from his body, and how fast it swings. */
    public final float[] cape = new float[CAPE], prevCape = new float[CAPE], capeSpin = new float[CAPE];
    public float capeSway, prevCapeSway;
    /** Client: where he has been, newest first: x, y, z, yaw, pitch, roll, heat. */
    public final double[][] trail = new double[TRAIL][7];
    public int trailCount;
    /** Client: the pose he was in when his state last changed, which the model blends out of; and the pose last drawn. */
    public final float[] poseFrom = new float[32], poseLast = new float[32];
    public boolean hasPoseFrom;

    public EntityAdamWarlock(World world) {
        super(world);
        setSize(0.7F, 2.0F);
        noClip = true;
        isImmuneToFire = true;
        experienceValue = 0;
        ignoreFrustumCheck = true;
        setNoGravity(true);
        orbit = rand.nextDouble() * Math.PI * 2D;
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(STATE, (byte) State.ARRIVE.ordinal());
        dataManager.register(EVOLVED, false);
        dataManager.register(COCOON, 0F);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(EternityConfig.warlock.health);
        getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(1D);
    }

    @Override
    public void notifyDataManagerChange(DataParameter<?> key) {
        super.notifyDataManagerChange(key);
        if (STATE.equals(key) && world.isRemote) {
            clientStateTicks = 0;
            System.arraycopy(poseLast, 0, poseFrom, 0, poseLast.length);
            previousState = seenState;
            seenState = getState();
        }
    }

    // ------------------------------------------------------------------ accessors

    public State getState() {
        int ordinal = dataManager.get(STATE);
        return ordinal >= 0 && ordinal < State.values().length ? State.values()[ordinal] : State.HOVER;
    }

    private void setState(State state) {
        dataManager.set(STATE, (byte) state.ordinal());
        stateTicks = 0;
    }

    public boolean isEvolved() {
        return dataManager.get(EVOLVED);
    }

    /** How much of his cocoon still holds, 0 to 1; 0 when he is not in one. */
    public float getCocoon() {
        return dataManager.get(COCOON);
    }

    /** How many bolts each volley fires; the model times his arms to it. */
    public int volleyBolts() {
        return isEvolved() ? 7 : 5;
    }

    public int volleyTicks() {
        return VOLLEY_GATHER + volleyBolts() * VOLLEY_GAP + 12;
    }

    /** Whether he is streaking through the air, head first, so his body lines up with where he is going. */
    public boolean isStreaking() {
        int t = clientStateTicks;
        switch (getState()) {
            case FLYBY:
            case DIVE:
                return true;
            case ARRIVE:
                return t < ARRIVE_STREAK;
            case RISE:
                return t < RISE_CLIMB;
            case TURN:
                return t < 8;
            case RETREAT:
                return t >= RETREAT_LAUNCH;
            default:
                return false;
        }
    }

    // ------------------------------------------------------------------ setting him loose (the command's handles)

    /** He streaks down out of the sky at someone and stops in front of them. */
    public void arrive(EntityPlayerMP player, boolean calm, boolean showcase) {
        targetId = player.getUniqueID();
        this.calm = calm;
        this.showcase = showcase;
        Vec3d look = flat(player.getLookVec());
        double y = Math.max(player.posY + 2D, floor(player.posX + look.x * 8D, player.posY + 4D, player.posZ + look.z * 8D) + 2D);
        arriveTo = new Vec3d(player.posX + look.x * 8D, y, player.posZ + look.z * 8D);
        Vec3d side = new Vec3d(-look.z, 0D, look.x);
        arriveFrom = arriveTo.add(look.scale(30D)).add(side.scale(18D * (rand.nextBoolean() ? 1D : -1D))).add(0D, 34D, 0D);
        setPosition(arriveFrom.x, arriveFrom.y, arriveFrom.z);
        faceAlong(arriveTo.subtract(arriveFrom));
        setState(State.ARRIVE);
        playSound(EternitySounds.COMET_FLYBY, 6F, 1.3F);
    }

    public void setCalm(boolean calm) {
        this.calm = calm;
        if (!calm) {
            showcase = false;
        }
    }

    public boolean isCalm() {
        return calm;
    }

    public boolean isShowcase() {
        return showcase;
    }

    /** Makes him do something next, as soon as he is free to. */
    public void order(Move move) {
        pendingMove = move;
        if (getState() == State.HOVER) {
            cooldown = 0;
        }
    }

    /** He leaves now, without a word. */
    public void dismiss() {
        retreat(false);
        stateTicks = RETREAT_LAUNCH - 1;
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void onLivingUpdate() {
        noClip = true;
        motionX = motionY = motionZ = 0D;
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
        move(MoverType.SELF, motionX, motionY, motionZ);
    }

    private void serverTick() {
        stateTicks++;
        if (scripted) {
            scriptedTick();
            return;
        }
        EntityPlayerMP target = target();
        tickBolts();
        if (ticksExisted % 5 == 0) {
            bossInfo.setPercent(getHealth() / getMaxHealth());
            State state = getState();
            bossInfo.setName(new TextComponentString(state == State.COCOON ? "Adam Warlock (cocooned)"
                    : state == State.RETREAT ? "Adam Warlock (beaten)" : isEvolved() ? "Adam Warlock (Evolved)" : "Adam Warlock"));
            bossInfo.setColor(isEvolved() ? BossInfo.Color.RED : BossInfo.Color.YELLOW);
        }
        if (hitStop > 0) {
            // The moment a blow lands, everything holds still for it.
            hitStop--;
            stateTicks--;
            return;
        }
        if (target == null && getState() != State.RETREAT) {
            if (++lonely > 100) {
                say("Hm. Gone already.");
                dismiss();
            }
        } else {
            lonely = 0;
        }
        switch (getState()) {
            case ARRIVE:
                arriving(target);
                break;
            case HOVER:
                hovering(target);
                break;
            case TAUNT:
                taunting(target);
                break;
            case WINDUP:
                windup(target);
                break;
            case FLYBY:
                flyby(target);
                break;
            case TURN:
                turning(target);
                break;
            case COMBO:
                combo(target);
                break;
            case VOLLEY:
                volley(target);
                break;
            case BEAM:
                beam(target);
                break;
            case RISE:
                rising(target);
                break;
            case DIVE:
                diving();
                break;
            case SLAM:
                slammed(target);
                break;
            case COCOON:
                cocooned(target);
                break;
            case EMERGE:
                emerging(target);
                break;
            case STAGGER:
                staggered(target);
                break;
            case RETREAT:
                retreating(target);
                break;
        }
    }

    /** Whoever he came for, while they are still here and still standing; failing that, whoever is nearest. */
    @Nullable
    private EntityPlayerMP target() {
        if (targetId != null) {
            EntityPlayer player = world.getPlayerEntityByUUID(targetId);
            if (player instanceof EntityPlayerMP && isFair(player) && player.getDistanceSq(this) < 96D * 96D) {
                return (EntityPlayerMP) player;
            }
            if (player != null && player.isDead && getState() != State.RETREAT) {
                say("Pathetic.");
                targetId = null;
                dismiss();
                return null;
            }
        }
        EntityPlayerMP best = null;
        double bestDistance = 64D * 64D;
        for (EntityPlayer player : world.playerEntities) {
            double d = player.getDistanceSq(this);
            if (player instanceof EntityPlayerMP && isFair(player) && d < bestDistance) {
                best = (EntityPlayerMP) player;
                bestDistance = d;
            }
        }
        if (best != null) {
            targetId = best.getUniqueID();
        }
        return best;
    }

    private static boolean isFair(EntityPlayer player) {
        return !player.isDead && !player.isSpectator() && player.getHealth() > 0F;
    }

    // ------------------------------------------------------------------ arriving, hovering, taunting

    /** Streaking down out of the sky, then stopping dead in the air in front of you. */
    private void arriving(@Nullable EntityPlayerMP target) {
        if (arriveFrom == null || arriveTo == null) {
            hover(30);
            return;
        }
        if (stateTicks <= ARRIVE_STREAK) {
            double p = stateTicks / (double) ARRIVE_STREAK;
            // Picking up speed the whole way, he does not slow until he stops.
            Vec3d at = arriveFrom.add(arriveTo.subtract(arriveFrom).scale(p * p * (0.4D + 0.6D * p)));
            setPosition(at.x, at.y, at.z);
            faceAlong(arriveTo.subtract(arriveFrom));
            if (stateTicks == ARRIVE_STREAK) {
                fx(MsgTribunalFx.WARLOCK_LAND, posX, posY + 1D, posZ, 3.5D, 0, 0, 0, 30);
                sound(SoundEvents.ENTITY_GENERIC_EXPLODE, 4F, 1.4F);
                sound(EternitySounds.COMET_IMPACT, 3F, 1.6F);
                for (EntityPlayer player : world.playerEntities) {
                    if (player.getDistance(this) < 7D && !player.isSpectator()) {
                        push(player, flat(player.getPositionVector().subtract(getPositionVector())), 0.6D, 0.3D);
                    }
                }
            }
            return;
        }
        if (target != null) {
            face(target, 0.15F);
        }
        if (stateTicks == ARRIVE_STREAK + 6 && !scripted) {
            say(ARRIVALS[rand.nextInt(ARRIVALS.length)]);
        }
        if (stateTicks >= ARRIVE_TICKS) {
            hover(showcase ? 20 : 25);
        }
    }

    private void hover(int ticks) {
        setState(State.HOVER);
        cooldown = Math.max(8, (int) (ticks * (isEvolved() ? 0.65F : 1F)));
    }

    /** Hanging in the air a little above and away from you, circling, sizing you up. */
    private void hovering(@Nullable EntityPlayerMP target) {
        if (target == null) {
            return;
        }
        face(target, 0.3F);
        orbit += (isEvolved() ? 0.022D : 0.014D) * (Math.sin(ticksExisted * 0.013D) > -0.3D ? 1D : -1D);
        double radius = showcase ? 7D : 7.5D + 1.5D * Math.sin(ticksExisted * 0.03D);
        double x = target.posX + Math.cos(orbit) * radius, z = target.posZ + Math.sin(orbit) * radius;
        double y = Math.max(target.posY + 2.2D, floor(x, target.posY + 4D, z) + 2D);
        glide(new Vec3d(x, y, z), 0.1D, 0.6D);
        if (--cooldown <= 0) {
            choose(target);
        }
    }

    private void choose(EntityPlayerMP target) {
        Move next;
        if (pendingMove != null) {
            next = pendingMove;
            pendingMove = null;
        } else if (!cocoonUsed && !showcase && getHealth() < getMaxHealth() * 0.35F) {
            next = Move.COCOON;
        } else if (showcase) {
            next = SHOWCASE[showIndex++ % SHOWCASE.length];
        } else {
            double d = target.getDistance(this);
            int flyby = d > 10D ? 40 : 25, combo = d < 9D ? 40 : 10, volley = d > 8D ? 25 : 12, dive = 18;
            int beam = isEvolved() ? 22 : 8;
            int taunt = lastMove == Move.TAUNT ? 0 : 6;
            do {
                int roll = rand.nextInt(flyby + combo + volley + dive + beam + taunt);
                next = (roll -= flyby) < 0 ? Move.FLYBY : (roll -= combo) < 0 ? Move.COMBO : (roll -= volley) < 0 ? Move.VOLLEY
                        : (roll -= dive) < 0 ? Move.DIVE : (roll -= beam) < 0 ? Move.BEAM : Move.TAUNT;
                // He loves his fly-by too much to mind doing it twice; anything else, he mixes up.
            } while (next == lastMove && next != Move.FLYBY);
        }
        start(next, target);
    }

    private void start(Move move, EntityPlayerMP target) {
        lastMove = move;
        switch (move) {
            case TAUNT:
                setState(State.TAUNT);
                say(TAUNTS[rand.nextInt(TAUNTS.length)]);
                break;
            case FLYBY:
                passes = isEvolved() ? 3 : 2;
                launchAt = null;
                setState(State.WINDUP);
                break;
            case COMBO:
                launched = false;
                setState(State.COMBO);
                break;
            case VOLLEY:
                setState(State.VOLLEY);
                break;
            case BEAM:
                aim = chest(target);
                setState(State.BEAM);
                break;
            case DIVE:
                mark = null;
                setState(State.RISE);
                break;
            case COCOON:
                cocoonUsed = true;
                cocoonHealth = (float) (getMaxHealth() * EternityConfig.warlock.cocoonStrength);
                dataManager.set(COCOON, 1F);
                setState(State.COCOON);
                say("Not... like this.");
                sound(SoundEvents.EVOCATION_ILLAGER_PREPARE_SUMMON, 3F, 0.6F);
                break;
        }
    }

    private void taunting(@Nullable EntityPlayerMP target) {
        if (target != null) {
            face(target, 0.2F);
        }
        if (stateTicks == 38) {
            sound(SoundEvents.ENTITY_PLAYER_ATTACK_NODAMAGE, 1.5F, 0.6F);
        }
        if (stateTicks >= TAUNT_TICKS) {
            hover(20);
        }
    }

    // ------------------------------------------------------------------ the fly-by

    /** Coiling in the air, fist drawn back, before he goes. */
    private void windup(@Nullable EntityPlayerMP target) {
        if (target == null) {
            hover(20);
            return;
        }
        int ticks = passes < (isEvolved() ? 3 : 2) ? WINDUP_CHAINED : isEvolved() ? WINDUP_EVOLVED : WINDUP_TICKS;
        if (stateTicks == 1) {
            if (launchAt == null) {
                // Out to one side of you, far enough to build up speed.
                Vec3d away = flat(getPositionVector().subtract(target.getPositionVector()));
                double spread = (rand.nextDouble() - 0.5D) * 1.2D;
                away = new Vec3d(away.x * Math.cos(spread) - away.z * Math.sin(spread), 0D, away.x * Math.sin(spread) + away.z * Math.cos(spread));
                Vec3d at = target.getPositionVector().add(away.scale(15D));
                launchAt = new Vec3d(at.x, Math.max(target.posY + 1.2D + rand.nextDouble() * 1.5D, floor(at.x, target.posY + 4D, at.z) + 1.5D), at.z);
            } else {
                launchAt = getPositionVector();
            }
            Vec3d c = chest();
            fx(MsgTribunalFx.WARLOCK_CHARGE, c.x, c.y, c.z, 2.5D, 0, 0, getEntityId(), ticks);
            sound(SoundEvents.ENTITY_ENDERDRAGON_FLAP, 2F, 1.4F);
            sound(SoundEvents.ENTITY_ILLAGER_PREPARE_BLINDNESS, 2F, 1.5F);
        }
        glide(launchAt, 0.3D, 1.2D);
        face(target, 0.5F);
        if (stateTicks >= ticks) {
            launch(target);
        }
    }

    private void launch(EntityPlayerMP target) {
        Vec3d lead = new Vec3d(target.posX - target.prevPosX, 0D, target.posZ - target.prevPosZ).scale(5D);
        Vec3d at = chest(target).add(lead);
        if (calm) {
            // Close enough to feel the wind of it, never close enough to touch.
            Vec3d side = flat(at.subtract(chest())).crossProduct(new Vec3d(0D, 1D, 0D));
            at = at.add(side.scale(rand.nextBoolean() ? 3.5D : -3.5D)).add(0D, 1.2D, 0D);
        }
        Vec3d c = chest();
        dir = at.subtract(c).normalize();
        distance = at.distanceTo(c) + 13D;
        travelled = 0D;
        speed = 0.5D;
        landed = false;
        passes--;
        faceAlong(dir);
        setState(State.FLYBY);
        fx(MsgTribunalFx.WARLOCK_BOOM, c.x, c.y, c.z, dir.x, dir.y, dir.z, 0, 24);
        sound(SoundEvents.ENTITY_GENERIC_EXPLODE, 3F, 1.7F);
        sound(SoundEvents.ENTITY_FIREWORK_LARGE_BLAST, 4F, 0.6F);
        sound(EternitySounds.COMET_FLYBY, 4F, 1.8F);
    }

    /** Fist first, faster than you can turn, straight through where you were standing. */
    private void flyby(@Nullable EntityPlayerMP target) {
        double top = isEvolved() ? 3.0D : 2.6D;
        speed = Math.min(top, speed + 0.7D);
        Vec3d from = getPositionVector();
        Vec3d to = from.add(dir.scale(speed));
        // Skimming the ground rather than ploughing through it.
        double ground = floor(to.x, to.y + 2D, to.z);
        if (to.y < ground + 0.3D && travelled < distance - 4D) {
            to = new Vec3d(to.x, ground + 0.3D, to.z);
        }
        if (!landed) {
            for (EntityPlayer player : world.playerEntities) {
                if (!isFair(player) || player.getDistanceSq(this) > 64D) {
                    continue;
                }
                Vec3d body = new Vec3d(player.posX, player.posY + player.height * 0.5D, player.posZ);
                Vec3d near = closest(from.add(0D, 1.1D, 0D), to.add(0D, 1.1D, 0D), body);
                if (near.distanceTo(body) < 1.8D) {
                    landed = true;
                    setPosition(near.x, near.y - 1.1D, near.z);
                    hitStop = 4;
                    Vec3d at = near.add(body).scale(0.5D);
                    fx(MsgTribunalFx.WARLOCK_IMPACT, at.x, at.y, at.z, dir.x, dir.y, dir.z, player.getEntityId(), 1);
                    sound(EternitySounds.COMET_IMPACT, 4F, 1.5F);
                    sound(SoundEvents.ENTITY_IRONGOLEM_ATTACK, 3F, 0.6F);
                    sound(SoundEvents.ENTITY_GENERIC_EXPLODE, 2.5F, 1.3F);
                    harm(player, 0.30F);
                    push(player, dir, 2.4D, 0.8D);
                    if (rand.nextInt(3) == 0) {
                        say(LANDED[rand.nextInt(LANDED.length)]);
                    }
                    return;
                }
            }
        }
        setPosition(to.x, to.y, to.z);
        travelled += speed;
        if (travelled >= distance || stateTicks > 30) {
            velocity = dir.scale(speed);
            setState(State.TURN);
        }
    }

    /** Overshooting, bleeding off speed in a long banking turn that brings him back round to face you. */
    private void turning(@Nullable EntityPlayerMP target) {
        Vec3d want = target != null ? chest(target).subtract(chest()).normalize() : dir;
        velocity = velocity.scale(0.82D).add(want.scale(0.12D)).add(0D, 0.03D, 0D);
        Vec3d to = getPositionVector().add(velocity);
        double ground = floor(to.x, to.y + 2D, to.z);
        setPosition(to.x, Math.max(to.y, ground + 1D), to.z);
        if (stateTicks < TURN_TICKS - 6 && velocity.lengthSquared() > 0.04D) {
            float yaw = (float) Math.toDegrees(Math.atan2(velocity.z, velocity.x)) - 90F;
            rotationYaw += MathHelper.wrapDegrees(yaw - rotationYaw) * 0.5F;
            rotationYawHead = rotationYaw;
        } else if (target != null) {
            face(target, 0.35F);
        }
        if (stateTicks >= TURN_TICKS) {
            if (passes > 0 && target != null && target.getDistance(this) < 48D) {
                launchAt = getPositionVector();
                setState(State.WINDUP);
            } else {
                hover(40);
            }
        }
    }

    // ------------------------------------------------------------------ the combo

    /** In close: jab, cross, an uppercut that launches you, and the hammer that brings you back down. */
    private void combo(@Nullable EntityPlayerMP target) {
        if (target == null) {
            hover(20);
            return;
        }
        int t = stateTicks;
        if (t < COMBO_BLINK || !launched) {
            // Right in front of you, keeping up with every step back you take.
            Vec3d from = flat(getPositionVector().subtract(target.getPositionVector()));
            double reach = calm ? 3.2D : t < COMBO_JAB ? 1.7D : 1.5D;
            double y = Math.max(target.posY, floor(target.posX, target.posY + 2D, target.posZ) + 0.05D);
            glide(new Vec3d(target.posX + from.x * reach, y, target.posZ + from.z * reach), t < COMBO_JAB ? 0.45D : 0.3D, 1.6D);
            face(target, 0.6F);
        }
        if (t == 2) {
            sound(SoundEvents.ENTITY_ENDERDRAGON_FLAP, 1.5F, 1.8F);
        }
        if (t == COMBO_JAB) {
            strike(target, 0.07F, 0.4D, 0.15D, false);
        } else if (t == COMBO_CROSS) {
            strike(target, 0.09F, 0.7D, 0.2D, false);
        } else if (t == COMBO_UPPER) {
            launched = strike(target, 0.14F, 0.35D, 1.25D, true) && !calm;
        } else if (t == COMBO_BLINK && launched) {
            // Above you before you stop rising.
            Vec3d from = chest();
            Vec3d back = flat(getPositionVector().subtract(target.getPositionVector())).scale(0.8D);
            Vec3d to = target.getPositionVector().add(back).add(0D, 3.4D, 0D);
            setPosition(to.x, to.y, to.z);
            fx(MsgTribunalFx.WARLOCK_BLINK, from.x, from.y, from.z, to.x, to.y + 1.1D, to.z, 0, 10);
            sound(SoundEvents.ENTITY_ENDERMEN_TELEPORT, 2F, 1.5F);
            face(target, 1F);
        } else if (t > COMBO_BLINK && t < COMBO_HAMMER && launched) {
            Vec3d back = flat(getPositionVector().subtract(target.getPositionVector())).scale(0.8D);
            glide(target.getPositionVector().add(back).add(0D, 2.6D, 0D), 0.5D, 1.5D);
            face(target, 0.8F);
        } else if (t == COMBO_HAMMER && launched) {
            Vec3d c = chest(target);
            if (c.distanceTo(chest()) < 4.5D) {
                fx(MsgTribunalFx.WARLOCK_IMPACT, c.x, c.y + 0.4D, c.z, 0D, -1D, 0D, target.getEntityId(), 2);
                sound(EternitySounds.COMET_IMPACT, 4F, 1.2F);
                sound(SoundEvents.ENTITY_GENERIC_EXPLODE, 2F, 1.1F);
                harm(target, 0.20F);
                if (EntityDestroyerArmor.carrier(target) != null) {
                    EntityDestroyerArmor.fling(target, 0D, -2.4D, 0D);
                } else {
                    target.motionX *= 0.2D;
                    target.motionZ *= 0.2D;
                    target.motionY = -2.4D;
                    target.velocityChanged = true;
                }
                hitStop = 3;
            } else {
                sound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 2F, 0.6F);
            }
        }
        if (t >= COMBO_TICKS || t >= COMBO_BLINK && !launched) {
            hover(35);
        }
    }

    /** One blow from the combo, at whatever is right in front of him. Returns whether it landed. */
    private boolean strike(EntityPlayerMP target, float fraction, double knock, double lift, boolean big) {
        Vec3d c = chest(target);
        Vec3d forward = facing();
        double reach = chest().distanceTo(c);
        if (calm || reach > 3.3D || forward.dotProduct(flat(c.subtract(chest()))) < 0.3D) {
            sound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1.5F, big ? 0.6F : 0.9F);
            if (calm) {
                Vec3d at = chest().add(forward.scale(1.3D)).add(0D, big ? 0.6D : 0D, 0D);
                fx(MsgTribunalFx.WARLOCK_IMPACT, at.x, at.y, at.z, forward.x, big ? 0.8D : 0D, forward.z, -1, big ? 1 : 0);
            }
            return false;
        }
        Vec3d at = chest().add(c).scale(0.5D);
        fx(MsgTribunalFx.WARLOCK_IMPACT, at.x, at.y, at.z, forward.x, big ? 0.8D : 0D, forward.z, target.getEntityId(), big ? 1 : 0);
        sound(SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, 2F, big ? 0.6F : 0.85F);
        sound(SoundEvents.ENTITY_IRONGOLEM_ATTACK, 2F, big ? 0.7F : 1.1F);
        if (big) {
            sound(EternitySounds.COMET_IMPACT, 3F, 1.6F);
        }
        harm(target, fraction);
        push(target, forward, knock, lift);
        hitStop = big ? 3 : 1;
        return true;
    }

    // ------------------------------------------------------------------ quantum energy

    /** Bolts of quantum energy, from one palm and then the other. */
    private void volley(@Nullable EntityPlayerMP target) {
        if (target == null) {
            hover(20);
            return;
        }
        int t = stateTicks;
        face(target, 0.4F);
        // Hanging back at a comfortable distance to shoot from.
        Vec3d away = flat(getPositionVector().subtract(target.getPositionVector()));
        double y = Math.max(target.posY + 2.5D, floor(posX, target.posY + 4D, posZ) + 2D);
        glide(new Vec3d(target.posX + away.x * 10D, y, target.posZ + away.z * 10D), 0.06D, 0.4D);
        if (t == 1) {
            Vec3d c = chest();
            fx(MsgTribunalFx.WARLOCK_CHARGE, c.x, c.y, c.z, 1.8D, 0, 0, getEntityId(), VOLLEY_GATHER);
            sound(SoundEvents.EVOCATION_ILLAGER_PREPARE_SUMMON, 2F, 1.6F);
        }
        int shot = t - VOLLEY_GATHER;
        if (shot >= 0 && shot % VOLLEY_GAP == 0 && shot / VOLLEY_GAP < volleyBolts()) {
            int k = shot / VOLLEY_GAP;
            Vec3d hand = hand(k % 2 == 0);
            double pace = isEvolved() ? 1.5D : 1.15D;
            Vec3d at = chest(target);
            double flight = at.distanceTo(hand) / pace;
            at = at.add(new Vec3d(target.posX - target.prevPosX, 0D, target.posZ - target.prevPosZ).scale(flight * 0.8D));
            if (calm) {
                Vec3d side = flat(at.subtract(hand)).crossProduct(new Vec3d(0D, 1D, 0D));
                at = at.add(side.scale(k % 2 == 0 ? 3D : -3D));
            }
            Vec3d vel = at.subtract(hand).normalize().scale(pace);
            Bolt bolt = new Bolt(nextBolt++, hand, vel);
            bolts.add(bolt);
            fx(MsgTribunalFx.WARLOCK_BOLT, hand.x, hand.y, hand.z, vel.x, vel.y, vel.z, bolt.id, 50);
            sound(SoundEvents.ENTITY_BLAZE_SHOOT, 2F, 1.5F + rand.nextFloat() * 0.2F);
            sound(SoundEvents.ENTITY_FIREWORK_BLAST, 1.5F, 0.7F);
        }
        if (t >= volleyTicks()) {
            hover(35);
        }
    }

    private void tickBolts() {
        Iterator<Bolt> it = bolts.iterator();
        while (it.hasNext()) {
            Bolt bolt = it.next();
            bolt.age++;
            Vec3d next = bolt.pos.add(bolt.vel);
            boolean burst = bolt.age > 50;
            RayTraceResult wall = world.rayTraceBlocks(bolt.pos, next, false, true, false);
            if (wall != null && wall.typeOfHit == RayTraceResult.Type.BLOCK) {
                next = wall.hitVec;
                burst = true;
            }
            for (EntityPlayer player : world.playerEntities) {
                if (isFair(player) && closest(bolt.pos, next, player.getPositionVector().add(0D, player.height * 0.5D, 0D))
                        .distanceTo(player.getPositionVector().add(0D, player.height * 0.5D, 0D)) < 1.1D) {
                    burst = true;
                }
            }
            bolt.pos = next;
            if (burst) {
                it.remove();
                fx(MsgTribunalFx.WARLOCK_BURST, next.x, next.y, next.z, 2.6D, 0, 0, bolt.id, 14);
                world.playSound(null, next.x, next.y, next.z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1.6F, 1.6F);
                for (EntityPlayer player : world.playerEntities) {
                    double d = player.getDistance(next.x, next.y, next.z);
                    if (isFair(player) && d < 2.8D) {
                        harm(player, 0.09F);
                        push(player, flat(player.getPositionVector().subtract(next)), 0.7D, 0.35D);
                    }
                }
            }
        }
    }

    /** Palms together, a small sun between them; then the beam, following you. */
    private void beam(@Nullable EntityPlayerMP target) {
        int t = stateTicks;
        if (target != null) {
            Vec3d want = chest(target);
            if (calm) {
                want = want.add(flat(want.subtract(chest())).crossProduct(new Vec3d(0D, 1D, 0D)).scale(4D));
            }
            if (t < BEAM_CHARGE) {
                aim = want;
                Vec3d away = flat(getPositionVector().subtract(target.getPositionVector()));
                double y = Math.max(target.posY + 2.2D, floor(posX, target.posY + 4D, posZ) + 2D);
                glide(new Vec3d(target.posX + away.x * 9D, y, target.posZ + away.z * 9D), 0.08D, 0.5D);
            } else if (aim != null) {
                Vec3d toward = want.subtract(aim);
                double step = Math.min(toward.length(), isEvolved() ? 0.5D : 0.34D);
                if (step > 1.0E-3) {
                    aim = aim.add(toward.normalize().scale(step));
                }
            }
        }
        if (aim == null) {
            hover(20);
            return;
        }
        faceToward(aim, 0.5F);
        if (t == 1) {
            Vec3d c = hands();
            fx(MsgTribunalFx.WARLOCK_CHARGE, c.x, c.y, c.z, 3D, 0, 0, getEntityId(), BEAM_CHARGE);
            sound(SoundEvents.EVOCATION_ILLAGER_PREPARE_SUMMON, 3F, 0.8F);
            sound(SoundEvents.ENTITY_GUARDIAN_ATTACK, 2F, 0.5F);
        }
        if (t >= BEAM_CHARGE && t < BEAM_CHARGE + BEAM_FIRE) {
            Vec3d from = hands();
            Vec3d toward = aim.subtract(from).normalize();
            Vec3d far = from.add(toward.scale(40D));
            RayTraceResult wall = world.rayTraceBlocks(from, far, false, true, false);
            Vec3d end = wall != null && wall.typeOfHit == RayTraceResult.Type.BLOCK ? wall.hitVec : far;
            fx(MsgTribunalFx.WARLOCK_BEAM, from.x, from.y, from.z, end.x, end.y, end.z, t, 3);
            if (t == BEAM_CHARGE) {
                sound(SoundEvents.ENTITY_GENERIC_EXPLODE, 3F, 0.8F);
                sound(SoundEvents.ENTITY_FIREWORK_LARGE_BLAST_FAR, 4F, 0.5F);
            }
            if (t % 8 == 0) {
                sound(SoundEvents.ENTITY_BLAZE_BURN, 3F, 0.5F);
            }
            // Pushed back a little by what is pouring out of him.
            Vec3d back = toward.scale(-0.03D);
            setPosition(posX + back.x, posY + back.y * 0.3D, posZ + back.z);
            if (t % 4 == 0) {
                for (EntityPlayer player : world.playerEntities) {
                    Vec3d body = player.getPositionVector().add(0D, player.height * 0.5D, 0D);
                    if (isFair(player) && closest(from, end, body).distanceTo(body) < 1.5D) {
                        harm(player, 0.045F);
                        push(player, toward, 0.35D, 0.1D);
                    }
                }
            }
        }
        if (t >= BEAM_TICKS) {
            hover(40);
        }
    }

    // ------------------------------------------------------------------ the meteor dive

    /** Climbing straight up out of sight, then hanging there, marking where he will land. */
    private void rising(@Nullable EntityPlayerMP target) {
        int t = stateTicks;
        if (t == 1) {
            Vec3d c = chest();
            fx(MsgTribunalFx.WARLOCK_BOOM, c.x, c.y, c.z, 0D, 1D, 0D, 0, 20);
            sound(SoundEvents.ENTITY_ENDERDRAGON_FLAP, 3F, 1.2F);
            sound(SoundEvents.ENTITY_FIREWORK_LAUNCH, 4F, 0.6F);
            sound(EternitySounds.COMET_FLYBY, 3F, 2F);
        }
        if (t <= RISE_CLIMB) {
            double climb = Math.min(2.4D, 0.4D + t * 0.16D);
            Vec3d over = target != null ? new Vec3d(target.posX - posX, 0D, target.posZ - posZ).scale(0.08D) : Vec3d.ZERO;
            setPosition(posX + over.x, posY + climb, posZ + over.z);
        }
        if (t == RISE_CLIMB && target != null) {
            Vec3d lead = new Vec3d(target.posX - target.prevPosX, 0D, target.posZ - target.prevPosZ).scale(6D);
            double x = target.posX + lead.x, z = target.posZ + lead.z;
            if (calm) {
                Vec3d side = flat(new Vec3d(posX - target.posX, 0D, posZ - target.posZ));
                x += side.x * 7D;
                z += side.z * 7D;
            }
            mark = new Vec3d(x, floor(x, target.posY + 3D, z), z);
            fx(MsgTribunalFx.WARLOCK_MARK, mark.x, mark.y, mark.z, 6D, 0, 0, 0, RISE_TICKS - RISE_CLIMB + 12);
            sound(SoundEvents.BLOCK_END_PORTAL_FRAME_FILL, 3F, 0.5F);
        }
        if (mark != null) {
            faceToward(mark, 0.3F);
        }
        if (t >= RISE_TICKS) {
            if (mark == null) {
                hover(20);
                return;
            }
            speed = 0.6D;
            setState(State.DIVE);
            sound(SoundEvents.ENTITY_ENDERDRAGON_FLAP, 3F, 1.6F);
        }
    }

    /** Fists first, straight down onto the mark. */
    private void diving() {
        if (mark == null) {
            hover(20);
            return;
        }
        speed = Math.min(3.4D, speed + 0.45D);
        Vec3d toward = mark.subtract(getPositionVector());
        if (toward.length() <= speed || posY <= mark.y) {
            setPosition(mark.x, mark.y, mark.z);
            impact();
            return;
        }
        Vec3d step = toward.normalize().scale(speed);
        setPosition(posX + step.x, posY + step.y, posZ + step.z);
        faceToward(mark, 0.5F);
    }

    private void impact() {
        setState(State.SLAM);
        fx(MsgTribunalFx.WARLOCK_SLAM, posX, posY, posZ, 8D, 0, 0, 0, 40);
        sound(EternitySounds.COMET_IMPACT, 6F, 0.9F);
        sound(SoundEvents.ENTITY_GENERIC_EXPLODE, 5F, 0.7F);
        sound(EternitySounds.RIFT_IMPACT, 4F, 1.3F);
        for (EntityPlayer player : world.playerEntities) {
            double d = player.getDistance(posX, posY, posZ);
            if (isFair(player) && d < 8D) {
                float near = (float) (1D - d / 8D);
                harm(player, 0.10F + 0.22F * near);
                push(player, flat(player.getPositionVector().subtract(getPositionVector())), 0.5D + 1.2D * near, 0.5D + 0.6D * near);
            }
        }
    }

    /** Down on one knee, fist in the ground, the way heroes land. For a moment he is open. */
    private void slammed(@Nullable EntityPlayerMP target) {
        if (target != null && stateTicks > 12) {
            face(target, 0.2F);
        }
        if (stateTicks >= SLAM_TICKS) {
            hover(30);
        }
    }

    // ------------------------------------------------------------------ the cocoon

    /** Wrapped in gold, mending. Break it before it hatches. */
    private void cocooned(@Nullable EntityPlayerMP target) {
        int t = stateTicks;
        double y = Math.max(posY, floor(posX, posY + 2D, posZ) + 2.5D);
        glide(new Vec3d(posX, y, posZ), 0.1D, 0.2D);
        if (t == COCOON_WRAP) {
            sound(SoundEvents.BLOCK_CHORUS_FLOWER_GROW, 3F, 0.5F);
        }
        if (t > COCOON_WRAP && t % 24 == 0) {
            // A heartbeat inside it.
            sound(SoundEvents.ENTITY_IRONGOLEM_STEP, 3F, 0.5F);
            sound(SoundEvents.ENTITY_WITHER_AMBIENT, 0.4F, 2F);
        }
        if (cocoonHealth <= 0F) {
            dataManager.set(COCOON, 0F);
            Vec3d c = chest();
            fx(MsgTribunalFx.WARLOCK_SHATTER, c.x, c.y, c.z, 0, 0, 0, 0, 40);
            sound(SoundEvents.BLOCK_GLASS_BREAK, 4F, 0.6F);
            sound(SoundEvents.ENTITY_ZOMBIE_VILLAGER_CURE, 1F, 2F);
            say("No! It wasn't finished!");
            staggerFor = STAGGER_TICKS + 36;
            setState(State.STAGGER);
            return;
        }
        if (t >= (showcase ? COCOON_SHOWCASE : COCOON_TICKS)) {
            hatch();
        }
    }

    private void hatch() {
        dataManager.set(COCOON, 0F);
        dataManager.set(EVOLVED, true);
        setHealth(Math.max(getHealth(), getMaxHealth() * (float) EternityConfig.warlock.evolvedHealth));
        setState(State.EMERGE);
        Vec3d c = chest();
        fx(MsgTribunalFx.WARLOCK_EMERGE, c.x, c.y, c.z, 12D, 0, 0, getEntityId(), 50);
        sound(SoundEvents.ITEM_TOTEM_USE, 4F, 0.7F);
        sound(EternitySounds.COMET_BLAST, 4F, 1.2F);
        sound(SoundEvents.BLOCK_GLASS_BREAK, 4F, 0.5F);
        for (EntityPlayer player : world.playerEntities) {
            if (isFair(player) && player.getDistance(this) < 10D) {
                push(player, flat(player.getPositionVector().subtract(getPositionVector())), 1.4D, 0.5D);
            }
        }
    }

    private void emerging(@Nullable EntityPlayerMP target) {
        if (stateTicks == 24) {
            say("I am Warlock. And now... I have evolved.");
        }
        if (target != null && stateTicks > 30) {
            face(target, 0.15F);
        }
        if (stateTicks >= EMERGE_TICKS) {
            hover(15);
        }
    }

    private void staggered(@Nullable EntityPlayerMP target) {
        if (stateTicks < 8) {
            // Knocked back through the air.
            Vec3d back = facing().scale(-0.35D * (8 - Math.max(0, stateTicks)) / 8D);
            setPosition(posX + back.x, posY + 0.04D, posZ + back.z);
        }
        if (target != null && stateTicks > 20) {
            face(target, 0.1F);
        }
        if (stateTicks >= staggerFor) {
            staggerFor = STAGGER_TICKS;
            hover(25);
        }
    }

    // ------------------------------------------------------------------ directed

    /** Puts him under someone else's direction, or lets him go back to fighting. Directed, he shows no boss bar. */
    public void script(boolean on) {
        if (scripted == on) {
            return;
        }
        scripted = on;
        bossInfo.setVisible(!on);
        if (on) {
            calm = true;
            bolts.clear();
        }
    }

    public boolean isScripted() {
        return scripted;
    }

    /**
     * Where to be and what to be doing there: hanging in the air or carrying something
     * (gliding there at {@code rate}), or standing and walking on the ground ({@code rate} in
     * blocks a tick), facing {@code face}.
     */
    public void direct(State state, Vec3d at, @Nullable Vec3d face, double rate) {
        scriptAt = at;
        scriptFace = face;
        scriptRate = rate;
        State now = getState();
        if (now != state && now != State.ARRIVE && now != State.RETREAT && !(state == State.STAND && now == State.WALK)) {
            setState(state);
        }
    }

    /** Streaks down out of the sky to a point and stops dead there. */
    public void arriveAt(Vec3d at, @Nullable Vec3d face) {
        scriptAt = at;
        scriptFace = face;
        arriveTo = at;
        Vec3d away = face != null ? flat(at.subtract(face)) : new Vec3d(0D, 0D, -1D);
        Vec3d side = new Vec3d(-away.z, 0D, away.x);
        arriveFrom = at.add(away.scale(30D)).add(side.scale(16D * (rand.nextBoolean() ? 1D : -1D))).add(0D, 34D, 0D);
        setPosition(arriveFrom.x, arriveFrom.y, arriveFrom.z);
        faceAlong(arriveTo.subtract(arriveFrom));
        setState(State.ARRIVE);
        playSound(EternitySounds.COMET_FLYBY, 6F, 1.3F);
    }

    /** Says a line now, to everyone near enough to hear it. */
    public void speak(String line) {
        lastSpoke = -1000;
        say(line);
    }

    private void scriptedTick() {
        State s = getState();
        switch (s) {
            case ARRIVE:
                arriving(null);
                if (stateTicks > ARRIVE_STREAK && scriptFace != null) {
                    faceToward(scriptFace, 0.15F);
                }
                break;
            case RETREAT:
                retreating(null);
                break;
            case HOVER:
            case CARRY:
            case TAUNT:
                if (scriptAt != null) {
                    glide(scriptAt, scriptRate, s == State.CARRY ? 2D : 0.8D);
                }
                if (scriptFace != null) {
                    faceToward(scriptFace, 0.25F);
                }
                if (s == State.TAUNT && stateTicks >= TAUNT_TICKS) {
                    setState(State.HOVER);
                }
                break;
            case STAND:
            case WALK: {
                if (scriptAt != null) {
                    Vec3d step = new Vec3d(scriptAt.x - posX, 0D, scriptAt.z - posZ);
                    double d = step.length();
                    if (d > 0.2D) {
                        step = step.scale(Math.min(scriptRate, d) / d);
                        setPosition(posX + step.x, posY, posZ + step.z);
                        float yaw = (float) Math.toDegrees(Math.atan2(step.z, step.x)) - 90F;
                        rotationYaw += MathHelper.wrapDegrees(yaw - rotationYaw) * 0.3F;
                        rotationYawHead = rotationYaw;
                        if (s == State.STAND && d > 1D) {
                            setState(State.WALK);
                        }
                    } else if (s == State.WALK) {
                        setState(State.STAND);
                    }
                }
                if (s == State.STAND && scriptFace != null) {
                    faceToward(scriptFace, 0.12F);
                }
                // Down onto the ground, and his feet on it.
                double ground = floor(posX, posY + 2D, posZ);
                double high = posY - ground;
                setPosition(posX, high > 0.05D ? posY - Math.min(high, Math.max(0.05D, high * 0.12D)) : ground, posZ);
                break;
            }
            default:
                setState(State.HOVER);
                break;
        }
    }

    // ------------------------------------------------------------------ beaten

    private void retreat(boolean words) {
        if (getState() == State.RETREAT) {
            return;
        }
        bolts.clear();
        dataManager.set(COCOON, 0F);
        setState(State.RETREAT);
        if (words) {
            say(LEAVING[rand.nextInt(LEAVING.length)]);
            sound(SoundEvents.ENTITY_PLAYER_HURT, 2F, 0.7F);
        }
    }

    /** Hanging there, hurt, one hand pressed to his side; then he is gone, straight up. */
    private void retreating(@Nullable EntityPlayerMP target) {
        int t = stateTicks;
        if (t < RETREAT_LAUNCH) {
            if (target != null) {
                face(target, 0.15F);
            }
            return;
        }
        if (t == RETREAT_LAUNCH) {
            Vec3d c = chest();
            fx(MsgTribunalFx.WARLOCK_BOOM, c.x, c.y, c.z, 0D, 1D, 0D, 0, 24);
            sound(SoundEvents.ENTITY_GENERIC_EXPLODE, 3F, 1.6F);
            sound(EternitySounds.COMET_FLYBY, 4F, 1.6F);
        }
        double climb = Math.min(3.5D, 0.3D + (t - RETREAT_LAUNCH) * 0.2D);
        setPosition(posX, posY + climb, posZ);
        if (t >= RETREAT_TICKS) {
            setDead();
        }
    }

    // ------------------------------------------------------------------ harm

    /** A share of the victim's maximum health, through armour. Unlike the cosmic beings, he cannot touch creative mode. */
    private void harm(EntityPlayer player, float fraction) {
        if (calm || !isFair(player)) {
            return;
        }
        float scale = (float) EternityConfig.warlock.damageScale * (isEvolved() ? 1.25F : 1F);
        EntityDestroyerArmor.strike(player, EternityDamage.warlock(this), fraction * scale);
    }

    /** Sends someone flying. Even pulled punches move the air around you. */
    private void push(EntityPlayer player, Vec3d direction, double strength, double lift) {
        if (player.isSpectator()) {
            return;
        }
        double scale = calm ? 0.25D : 1D;
        if (EntityDestroyerArmor.carrier(player) != null) {
            EntityDestroyerArmor.shove(player, direction.x * strength * scale, (direction.y * strength + lift) * scale, direction.z * strength * scale);
            return;
        }
        player.motionX += direction.x * strength * scale;
        player.motionY = Math.max(player.motionY, 0D) + (direction.y * strength + lift) * scale;
        player.motionZ += direction.z * strength * scale;
        player.velocityChanged = true;
        player.fallDistance = 0F;
    }

    /**
     * Only players' blows count, and none takes more than a sliver of him; he is wide open
     * while he lands and while he reels, and takes half again as much then. His cocoon takes
     * the blows meant for him; break it and he reels.
     */
    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (world.isRemote) {
            return false;
        }
        if (scripted) {
            return false;
        }
        State state = getState();
        if (!(source.getTrueSource() instanceof EntityPlayer) || state == State.ARRIVE || state == State.EMERGE || state == State.RETREAT) {
            return false;
        }
        boolean open = state == State.SLAM || state == State.STAGGER;
        float cap = (float) (getMaxHealth() * EternityConfig.warlock.maxHitFraction) * (open ? 1.5F : 1F);
        float dealt = Math.min(amount * (open ? 1.5F : 1F), cap);
        if (!(dealt > 0F)) {
            return false;
        }
        if (state == State.COCOON) {
            cocoonHealth -= dealt;
            dataManager.set(COCOON, MathHelper.clamp(cocoonHealth / (float) (getMaxHealth() * EternityConfig.warlock.cocoonStrength), 0F, 1F));
            sound(SoundEvents.BLOCK_GLASS_HIT, 2F, 0.6F + rand.nextFloat() * 0.3F);
            return true;
        }
        world.setEntityState(this, (byte) 2);
        float left = getHealth() - dealt;
        if (left <= 1F) {
            setHealth(1F);
            retreat(true);
            return true;
        }
        setHealth(left);
        if (dealt >= cap * 0.9F && rand.nextInt(4) == 0) {
            say(HURT[rand.nextInt(HURT.length)]);
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
        retreat(true);
    }

    // ------------------------------------------------------------------ helpers

    private void fx(byte type, double x, double y, double z, double a, double b, double c, int id, int time) {
        EternityNetwork.sendToDimension(new MsgTribunalFx(type, x, y, z, a, b, c, id, time), dimension);
    }

    private void sound(SoundEvent sound, float volume, float pitch) {
        world.playSound(null, posX, posY + 1D, posZ, sound, SoundCategory.HOSTILE, volume, pitch);
    }

    /** He speaks to everyone near enough to hear him. Never twice in two seconds. */
    private void say(String line) {
        if (ticksExisted - lastSpoke < 40) {
            return;
        }
        lastSpoke = ticksExisted;
        TextComponentString name = new TextComponentString("<Adam Warlock> ");
        name.getStyle().setColor(TextFormatting.GOLD).setBold(true);
        TextComponentString text = new TextComponentString(line);
        text.getStyle().setColor(TextFormatting.YELLOW).setBold(false);
        name.appendSibling(text);
        for (EntityPlayer player : world.playerEntities) {
            if (player.getDistance(this) < 128D) {
                player.sendMessage(name);
            }
        }
    }

    private Vec3d facing() {
        float yaw = (float) Math.toRadians(rotationYaw);
        return new Vec3d(-MathHelper.sin(yaw), 0D, MathHelper.cos(yaw));
    }

    public Vec3d chest() {
        return new Vec3d(posX, posY + 1.35D, posZ);
    }

    private static Vec3d chest(EntityPlayer player) {
        // Inside the Destroyer Armor, the armour's chest is what he has to reach.
        Entity body = EntityDestroyerArmor.mass(player);
        return new Vec3d(body.posX, body.posY + body.height * 0.6D, body.posZ);
    }

    /** Roughly where one of his palms is, thrust out in front of him. */
    private Vec3d hand(boolean right) {
        Vec3d forward = facing();
        Vec3d side = new Vec3d(-forward.z, 0D, forward.x).scale(right ? -0.4D : 0.4D);
        return chest().add(forward.scale(0.9D)).add(side);
    }

    /** Between his palms, held out together. */
    private Vec3d hands() {
        float yaw = (float) Math.toRadians(rotationYaw), pitch = (float) Math.toRadians(rotationPitch);
        Vec3d look = new Vec3d(-MathHelper.sin(yaw) * MathHelper.cos(pitch), -MathHelper.sin(pitch), MathHelper.cos(yaw) * MathHelper.cos(pitch));
        return chest().add(look.scale(0.95D));
    }

    private static Vec3d flat(Vec3d v) {
        Vec3d f = new Vec3d(v.x, 0D, v.z);
        return f.lengthSquared() < 1.0E-6 ? new Vec3d(0D, 0D, 1D) : f.normalize();
    }

    private static Vec3d closest(Vec3d a, Vec3d b, Vec3d p) {
        Vec3d ab = b.subtract(a);
        double len = ab.lengthSquared();
        if (len < 1.0E-8) {
            return a;
        }
        double t = MathHelper.clamp(p.subtract(a).dotProduct(ab) / len, 0D, 1D);
        return a.add(ab.scale(t));
    }

    /** The top of the ground under a point, looking down from y; far below if there is none. */
    private double floor(double x, double y, double z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(MathHelper.floor(x), MathHelper.floor(y), MathHelper.floor(z));
        for (int i = 0; i < 48 && pos.getY() > 0; i++) {
            IBlockState state = world.getBlockState(pos);
            // Water and lava are the surface: he lands on the sea, not on the seabed far below you.
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

    private void glide(Vec3d to, double rate, double max) {
        Vec3d step = to.subtract(getPositionVector()).scale(rate);
        if (step.length() > max) {
            step = step.normalize().scale(max);
        }
        setPosition(posX + step.x, posY + step.y, posZ + step.z);
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
        rotationPitch += (pitch - rotationPitch) * rate;
    }

    private void faceAlong(Vec3d v) {
        if (v.x * v.x + v.z * v.z > 1.0E-6) {
            rotationYaw = (float) Math.toDegrees(Math.atan2(v.z, v.x)) - 90F;
            rotationYawHead = rotationYaw;
        }
        rotationPitch = 0F;
    }

    // ------------------------------------------------------------------ client: how he moves

    /** How brightly his power burns in each state. Past 1 he is incandescent. */
    private float powerFor(State state, int t) {
        float base;
        switch (state) {
            case ARRIVE:
                base = t < ARRIVE_STREAK ? 1.1F : 0.5F;
                break;
            case WINDUP:
                base = 0.4F + 0.7F * Math.min(1F, t / 10F);
                break;
            case FLYBY:
            case RISE:
            case DIVE:
                base = 1.1F;
                break;
            case TURN:
                base = 0.75F;
                break;
            case COMBO:
                base = 0.65F;
                break;
            case VOLLEY:
                base = 0.85F;
                break;
            case BEAM:
                base = t < BEAM_CHARGE ? 0.5F + 0.6F * t / BEAM_CHARGE : t < BEAM_CHARGE + BEAM_FIRE ? 1.2F : 0.4F;
                break;
            case SLAM:
                base = t < 8 ? 1F : 0.45F;
                break;
            case COCOON:
                base = 0.15F;
                break;
            case EMERGE:
                base = 1.3F;
                break;
            case STAGGER:
                base = 0.05F;
                break;
            case RETREAT:
                base = t < RETREAT_LAUNCH ? 0.1F : 1F;
                break;
            case CARRY:
                base = 0.7F;
                break;
            case STAND:
            case WALK:
                base = 0.12F;
                break;
            default:
                base = 0.3F;
                break;
        }
        return isEvolved() ? Math.max(base, 0.65F) : base;
    }

    private void clientTick() {
        clientStateTicks++;
        double vx = posX - prevPosX, vy = posY - prevPosY, vz = posZ - prevPosZ;
        velX += (vx - velX) * 0.6D;
        velY += (vy - velY) * 0.6D;
        velZ += (vz - velZ) * 0.6D;
        double speed = Math.sqrt(velX * velX + velY * velY + velZ * velZ);
        float yaw = (float) Math.toRadians(rotationYaw);
        double fwd = -velX * MathHelper.sin(yaw) + velZ * MathHelper.cos(yaw);
        double side = velX * MathHelper.cos(yaw) + velZ * MathHelper.sin(yaw);

        // Head first along his flight when he streaks; otherwise only leaning into where he drifts.
        prevBodyPitch = bodyPitch;
        prevBodyRoll = bodyRoll;
        float wantPitch;
        float rate;
        if (isStreaking() && speed > 0.3D) {
            wantPitch = (float) Math.toDegrees(Math.atan2(fwd, velY));
            rate = 0.45F;
        } else {
            wantPitch = (float) MathHelper.clamp(fwd * 45D, -18D, 28D);
            rate = getState() == State.ARRIVE ? 0.22F : 0.15F;
        }
        bodyPitch += (wantPitch - bodyPitch) * rate;
        float yawRate = MathHelper.wrapDegrees(rotationYaw - prevRotationYaw);
        float wantRoll = isStreaking() || getState() == State.TURN ? MathHelper.clamp(-yawRate * 2.6F, -70F, 70F)
                : (float) MathHelper.clamp(-side * 40D, -16D, 16D);
        bodyRoll += (wantRoll - bodyRoll) * 0.2F;

        prevPower = power;
        power += (powerFor(getState(), clientStateTicks) - power) * 0.18F;

        for (int i = TRAIL - 1; i > 0; i--) {
            System.arraycopy(trail[i - 1], 0, trail[i], 0, 7);
        }
        trail[0][0] = posX;
        trail[0][1] = posY;
        trail[0][2] = posZ;
        trail[0][3] = rotationYaw;
        trail[0][4] = bodyPitch;
        trail[0][5] = bodyRoll;
        trail[0][6] = MathHelper.clamp((speed - 0.35D) / 1.2D, 0D, 1D) * Math.min(1F, power);
        trailCount = Math.min(TRAIL, trailCount + 1);

        capeTick(fwd, side, speed);
    }

    /**
     * His cape streams away from wherever the air is coming from and hangs where it is
     * still: each segment swings on a spring toward the one above it, so a ripple runs down
     * it; and the faster he goes, the harder it flutters.
     */
    private void capeTick(double fwd, double side, double speed) {
        double p = Math.toRadians(bodyPitch);
        double airF = -fwd * 0.9D, airU = -0.12D - velY * 0.9D, airS = -side * 0.9D;
        double down = -(airF * Math.sin(p) + airU * Math.cos(p));
        double back = -airF * Math.cos(p) + airU * Math.sin(p);
        float theta = (float) MathHelper.clamp(Math.atan2(back, down), 0.06D, 1.45D);
        float strength = (float) Math.min(1D, speed * 0.9D);
        float t = ticksExisted;
        prevCapeSway = capeSway;
        capeSway += ((float) MathHelper.clamp(airS * 0.8D, -0.5D, 0.5D) + MathHelper.sin(t * 0.05F) * 0.04F - capeSway) * 0.2F;
        for (int i = 0; i < CAPE; i++) {
            prevCape[i] = cape[i];
        }
        for (int i = 0; i < CAPE; i++) {
            float flutter = MathHelper.sin(t * (0.3F + 0.55F * strength) - i * 1.15F) * (0.035F + 0.24F * strength) * (0.5F + i * 0.4F)
                    + MathHelper.sin(t * 0.07F + i * 0.8F) * 0.025F;
            float goal = (i == 0 ? theta : prevCape[i - 1] * 0.7F + theta * 0.3F) + flutter;
            capeSpin[i] += (goal - cape[i]) * 0.32F;
            capeSpin[i] *= 0.62F;
            cape[i] += capeSpin[i];
            if (i == 0) {
                cape[i] = Math.max(0.05F, cape[i]);
            } else {
                cape[i] = cape[i - 1] + MathHelper.clamp(cape[i] - cape[i - 1], -0.55F, 0.85F);
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

    @Nullable
    @Override
    public Entity changeDimension(int dimension, ITeleporter teleporter) {
        return null;
    }

    /** Never written to chunks: for now he only comes when called. */
    @Override
    public boolean writeToNBTOptional(NBTTagCompound compound) {
        return false;
    }

    @Override
    public boolean isInRangeToRenderDist(double distance) {
        return distance < 320D * 320D;
    }

    @Override
    public SoundCategory getSoundCategory() {
        return SoundCategory.HOSTILE;
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_PLAYER_HURT;
    }

    @Override
    protected float getSoundPitch() {
        return 0.85F + rand.nextFloat() * 0.1F;
    }

    @Override
    public float getEyeHeight() {
        return 1.75F;
    }

    @Override
    public void addTrackingPlayer(EntityPlayerMP player) {
        super.addTrackingPlayer(player);
        if (!scripted) {
            bossInfo.addPlayer(player);
        }
    }

    @Override
    public void removeTrackingPlayer(EntityPlayerMP player) {
        super.removeTrackingPlayer(player);
        bossInfo.removePlayer(player);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return getEntityBoundingBox().grow(3D);
    }
}
