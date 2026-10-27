package com.matoon.eternity.entity;

import com.matoon.eternity.EternityConfig;
import com.matoon.eternity.EternitySounds;
import com.matoon.eternity.item.ItemAstralRegulator;
import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.network.MsgTribunalFx;
import com.matoon.eternity.world.Nexus;
import com.matoon.eternity.world.OrderAndChaos;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Master Order or Lord Chaos, come to put the In-Betweener back: a vast disembodied head
 * wreathed in fire, golden for Order and pale blue for Chaos. They are less than Eternity,
 * and they can be beaten, but they are the heart of Eternity and they fight like it.
 * Everything they do takes a share of your health, through any armour. Each fights as
 * what it is.
 *
 * Order is law, pattern and line, and everything he does can be read if you watch it:
 * <ul>
 * <li><b>Decree</b>: the glare of his eyes, a burning line whose end crawls after you.</li>
 * <li><b>Edict</b>: a law for everyone at the junction, for a little while: STAND STILL, DO
 * NOT STRIKE, or LOOK UPON ME. Break it and a bolt of punishment falls on you.</li>
 * <li><b>Tessellation</b>: the floor around you divides into a perfect grid of golden tiles,
 * in a pattern symmetrical both ways; the lit ones erupt, and then every other one does.</li>
 * <li><b>Alignment</b>: a golden line is drawn across the floor through you, everything is
 * drawn toward it, and then judgement runs its whole length.</li>
 * </ul>
 * Chaos is chance, appetite and mockery, and nothing he does goes the same way twice:
 * <ul>
 * <li><b>The Dice</b>: two vast dice of deep space tumble down, and whatever they show
 * happens. A shower of stars, everything flung skyward, everyone shuffled across the
 * junction, a burst under everyone's feet; or he heals you by mistake, or hurts himself
 * laughing, or rolls double sixes and attacks twice.</li>
 * <li><b>The Maw</b>: he opens his mouth onto deep space and inhales.</li>
 * <li><b>Wanderers</b>: little rifts of deep space set loose across the floor, wandering at
 * random and leaping without warning.</li>
 * <li><b>Masks</b>: he hides among copies of himself. Strike a mask and it bursts in your
 * face; strike the real one and the masks fall away.</li>
 * </ul>
 * Together, when either is badly hurt, they cross their beams on you, gold and deep space,
 * and the crossing hunts you across the floor.
 *
 * Neither can die. Beaten, each lies dazed for a little while, when a charged Astral
 * Regulator can draw it in; left alone, it withdraws, and the other fights on enraged.
 */
public class EntityOrderChaos extends EntityLiving {

    /** Blocks per model pixel: each head is nearly seven blocks tall. */
    public static final float SCALE = 0.38F;
    public static final float HEIGHT_PX = 18F;
    public static final float SIZE = HEIGHT_PX * SCALE;
    public static final int WITHDRAW_TICKS = 60;
    public static final int DOWN_TICKS = 240;
    public static final int ABSORB_TICKS = 100;
    private static final int TILE = 3;

    public enum Kind { ORDER, CHAOS }

    public enum State {
        ARRIVE, TALK, IDLE,
        DECREE, EDICT, TESSELLATE, ALIGN,
        DICE, MAW, WANDER, MASKS,
        CROSSING,
        DOWNED, ABSORBING, WITHDRAW
    }

    private static final State[] ORDER_ATTACKS = {State.DECREE, State.EDICT, State.TESSELLATE, State.ALIGN};
    private static final State[] CHAOS_ATTACKS = {State.DICE, State.MAW, State.WANDER, State.MASKS};
    private static final String[] EDICTS = {"STAND STILL.", "DO NOT STRIKE.", "LOOK UPON ME."};

    private static final DataParameter<Byte> KIND = EntityDataManager.createKey(EntityOrderChaos.class, DataSerializers.BYTE);
    private static final DataParameter<Byte> STATE = EntityDataManager.createKey(EntityOrderChaos.class, DataSerializers.BYTE);
    private static final DataParameter<Float> INTEGRITY = EntityDataManager.createKey(EntityOrderChaos.class, DataSerializers.FLOAT);
    private static final DataParameter<Boolean> ENRAGED = EntityDataManager.createKey(EntityOrderChaos.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> MASK = EntityDataManager.createKey(EntityOrderChaos.class, DataSerializers.BOOLEAN);

    private final BossInfoServer bossInfo = (BossInfoServer) new BossInfoServer(
            new TextComponentString("Master Order"), BossInfo.Color.YELLOW, BossInfo.Overlay.NOTCHED_10).setDarkenSky(true);

    private int stateTicks;
    private int cooldown = 40;
    private State last = State.IDLE;
    private double orbit;
    private Vec3d focus;
    private Vec3d beam;
    private Vec3d lineFrom, lineTo;
    private int pattern;
    private int bonusAttacks;
    private int maskIndex;
    private final List<double[]> wanderers = new ArrayList<>();
    private final List<Pending> pending = new ArrayList<>();
    private final Set<UUID> struck = new HashSet<>();
    private final Map<UUID, Integer> punished = new HashMap<>();
    private EntityPlayerMP absorber;
    private EnumHand absorbHand = EnumHand.MAIN_HAND;
    /** Flinching back from a raised Regulator: how much longer, and which way. */
    private int recoilTicks;
    private Vec3d recoilDir = Vec3d.ZERO;
    private int lastFlinch = -1000;
    /** Whether, lying beaten, it has already seen the Regulator coming. */
    private boolean begged;

    /** Client: ticks since the synced state last changed. */
    public int clientStateTicks;

    /** Something that lands a moment after it is set in motion. */
    private static final class Pending {
        final int when;
        final Runnable action;

        Pending(int when, Runnable action) {
            this.when = when;
            this.action = action;
        }
    }

    public EntityOrderChaos(World world) {
        super(world);
        setSize(SIZE * 0.85F, SIZE);
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
        dataManager.register(KIND, (byte) 0);
        dataManager.register(STATE, (byte) State.ARRIVE.ordinal());
        dataManager.register(INTEGRITY, 1F);
        dataManager.register(ENRAGED, false);
        dataManager.register(MASK, false);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(1024D);
        getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(1D);
    }

    @Override
    public void notifyDataManagerChange(DataParameter<?> key) {
        super.notifyDataManagerChange(key);
        if (STATE.equals(key) && world.isRemote) {
            clientStateTicks = 0;
        }
    }

    // ------------------------------------------------------------------ accessors

    public Kind getKind() {
        return dataManager.get(KIND) == 1 ? Kind.CHAOS : Kind.ORDER;
    }

    public boolean isOrder() {
        return getKind() == Kind.ORDER;
    }

    public void setKind(Kind kind) {
        dataManager.set(KIND, (byte) kind.ordinal());
        bossInfo.setName(new TextComponentString(kind == Kind.ORDER ? "Master Order" : "Lord Chaos"));
        bossInfo.setColor(kind == Kind.ORDER ? BossInfo.Color.YELLOW : BossInfo.Color.BLUE);
    }

    /** Whether this is one of the copies Lord Chaos hides among. */
    public boolean isMask() {
        return dataManager.get(MASK);
    }

    public State getState() {
        int ordinal = dataManager.get(STATE);
        return ordinal >= 0 && ordinal < State.values().length ? State.values()[ordinal] : State.IDLE;
    }

    public void setState(State state) {
        dataManager.set(STATE, (byte) state.ordinal());
        stateTicks = 0;
        struck.clear();
    }

    public float getIntegrity() {
        return dataManager.get(INTEGRITY);
    }

    public boolean isEnraged() {
        return dataManager.get(ENRAGED);
    }

    public boolean isDown() {
        return getState() == State.DOWNED;
    }

    /** The other has gone; this one fights on, faster and harder. */
    public void enrage() {
        dataManager.set(ENRAGED, true);
        playSound(isOrder() ? SoundEvents.ENTITY_ENDERDRAGON_GROWL : EternitySounds.ENTITY_GLITCH, 10F, 0.4F);
    }

    /** Begins fighting. */
    public void engage() {
        idle(30);
    }

    /** Starts the fight with only this much of their strength. */
    public void weaken(float integrity) {
        dataManager.set(INTEGRITY, Math.min(getIntegrity(), integrity));
    }

    /** Leaves, beaten or not. */
    public void withdraw() {
        if (getState() != State.WITHDRAW) {
            endBusiness();
            setState(State.WITHDRAW);
            playSound(EternitySounds.RIFT_CLOSE, 10F, isOrder() ? 0.5F : 0.8F);
        }
    }

    public boolean isWithdrawing() {
        return getState() == State.WITHDRAW;
    }

    /** Whatever it was in the middle of, it stops. */
    private void endBusiness() {
        pending.clear();
        wanderers.clear();
        if (getState() == State.MASKS) {
            OrderAndChaos.INSTANCE.dropMasks();
        }
        if (getState() == State.CROSSING && isOrder()) {
            OrderAndChaos.INSTANCE.setCrossing(null);
        }
    }

    private Vec3d facing() {
        float yaw = (float) Math.toRadians(rotationYaw);
        return new Vec3d(-MathHelper.sin(yaw), 0D, MathHelper.cos(yaw));
    }

    public Vec3d centre() {
        return new Vec3d(posX, posY + SIZE / 2D, posZ);
    }

    /** Between the eyes, where Order's decree comes from. */
    public Vec3d eyes() {
        return centre().add(facing().scale(6.2D * SCALE));
    }

    /** Chaos's maw, open onto deep space. */
    public Vec3d mouth() {
        return centre().add(0D, -3.5D * SCALE, 0D).add(facing().scale(6.2D * SCALE));
    }

    /** Client: whether a dazed head lies near enough a player for a Regulator to reach it. */
    public static boolean downedNear(EntityPlayer player) {
        for (Entity entity : player.world.loadedEntityList) {
            if (entity instanceof EntityOrderChaos && ((EntityOrderChaos) entity).isDown() && entity.getDistance(player) < 48F) {
                return true;
            }
        }
        return false;
    }

    /** Client: whether one of them still stands in front of this player, close enough to flinch from a raised Regulator. */
    public static boolean standingBefore(EntityPlayer player) {
        for (Entity entity : player.world.loadedEntityList) {
            if (entity instanceof EntityOrderChaos && !((EntityOrderChaos) entity).isMask() && !((EntityOrderChaos) entity).isDown()) {
                Vec3d toward = ((EntityOrderChaos) entity).centre().subtract(player.getPositionEyes(1F));
                if (toward.length() < 48D && player.getLook(1F).dotProduct(toward.normalize()) > 0.8D) {
                    return true;
                }
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void onLivingUpdate() {
        noClip = true;
        motionX = motionY = motionZ = 0D;
        if (!world.isRemote) {
            serverTick();
        } else {
            clientStateTicks++;
        }
        super.onLivingUpdate();
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        renderYawOffset = rotationYaw;
        prevRenderYawOffset = prevRotationYaw;
    }

    @Override
    public void travel(float strafe, float vertical, float forward) {
        move(MoverType.SELF, motionX, motionY, motionZ);
    }

    private void serverTick() {
        OrderAndChaos encounter = OrderAndChaos.INSTANCE;
        if (!encounter.claim(this)) {
            setDead();
            return;
        }
        stateTicks++;
        tickPending();
        List<EntityPlayerMP> players = encounter.participants();
        EntityPlayerMP target = nearest(players);
        if (isMask()) {
            maskTick(target);
            return;
        }
        State state = getState();
        if (target != null && state != State.WITHDRAW && state != State.DOWNED && state != State.ABSORBING) {
            face(target);
        }
        if (recoilTicks > 0 && state != State.WITHDRAW && state != State.DOWNED && state != State.ABSORBING) {
            double push = 0.9D * recoilTicks / 12D;
            setPosition(posX + recoilDir.x * push, posY + recoilDir.y * push, posZ + recoilDir.z * push);
            recoilTicks--;
        }
        switch (state) {
            case ARRIVE:
            case TALK: {
                // Hanging over the junction either side of his glass, looking down at whoever answers for it.
                glide(new Vec3d(Nexus.JUNCTION_X + (isOrder() ? 11D : -11D), Nexus.BUBBLE_Y + 1D, Nexus.JUNCTION_Z), 0.08D);
                EntityPlayerMP speaker = encounter.speaker();
                if (speaker != null) {
                    face(speaker);
                }
                break;
            }
            case IDLE:
                if (target != null) {
                    circle(target);
                    if (--cooldown <= 0) {
                        choose(target);
                    }
                }
                break;
            case DECREE:
                decree(target);
                break;
            case EDICT:
                edict(players);
                break;
            case TESSELLATE:
                tessellate(target);
                break;
            case ALIGN:
                align(target, players);
                break;
            case DICE:
                dice(target, players);
                break;
            case MAW:
                maw(target, players);
                break;
            case WANDER:
                wander(target, players);
                break;
            case MASKS:
                masks(target);
                break;
            case CROSSING:
                crossing(target);
                break;
            case DOWNED:
                downed(players);
                break;
            case ABSORBING:
                absorbing();
                break;
            case WITHDRAW:
                setPosition(posX, posY + 0.15D, posZ);
                if (stateTicks >= WITHDRAW_TICKS) {
                    encounter.withdrawn(this);
                    setDead();
                }
                break;
        }
        if (ticksExisted % 5 == 0) {
            bossInfo.setPercent(getIntegrity());
            String name = isOrder() ? "Master Order" : "Lord Chaos";
            bossInfo.setName(new TextComponentString(isDown() ? name + " (beaten)" : isEnraged() ? name + " (enraged)" : name));
            bossInfo.setColor(isDown() ? BossInfo.Color.WHITE : isOrder() ? BossInfo.Color.YELLOW : BossInfo.Color.BLUE);
        }
    }

    private void tickPending() {
        Iterator<Pending> it = pending.iterator();
        List<Runnable> due = new ArrayList<>();
        while (it.hasNext()) {
            Pending p = it.next();
            if (ticksExisted >= p.when) {
                due.add(p.action);
                it.remove();
            }
        }
        for (Runnable action : due) {
            action.run();
        }
    }

    private void later(int delay, Runnable action) {
        pending.add(new Pending(ticksExisted + delay, action));
    }

    @Nullable
    private EntityPlayerMP nearest(List<EntityPlayerMP> players) {
        EntityPlayerMP best = null;
        double bestDistance = Double.MAX_VALUE;
        for (EntityPlayerMP player : players) {
            double d = player.getDistanceSq(this);
            if (d < bestDistance) {
                bestDistance = d;
                best = player;
            }
        }
        return best;
    }

    private void face(Entity target) {
        double dx = target.posX - posX;
        double dz = target.posZ - posZ;
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90F;
        rotationYaw += MathHelper.wrapDegrees(yaw - rotationYaw) * (isOrder() ? 0.12F : 0.25F);
        rotationYawHead = rotationYaw;
    }

    private void glide(Vec3d to, double rate) {
        Vec3d step = to.subtract(getPositionVector()).scale(rate);
        if (step.length() > 1.2D) {
            step = step.normalize().scale(1.2D);
        }
        setPosition(posX + step.x, posY + step.y, posZ + step.z);
    }

    /**
     * Each circles whoever is nearest, close enough to be struck: Order on a steady, exact
     * path; Chaos lurching, swinging in and out, never where you expect.
     */
    private void circle(EntityPlayerMP target) {
        double radius, height;
        if (isOrder()) {
            orbit += 0.012D;
            radius = 6D;
            height = 0.4D;
        } else {
            orbit += 0.03D * MathHelper.sin(ticksExisted * 0.05F) + 0.01D;
            radius = 6.5D + 2.5D * MathHelper.sin(ticksExisted * 0.07F);
            height = 0.4D + 1.5D * MathHelper.sin(ticksExisted * 0.11F);
        }
        if (OrderAndChaos.carriesRegulator(target)) {
            // They know what that crystal is for, and keep a little more room between it and themselves.
            radius += 4D;
        }
        glide(leashed(new Vec3d(target.posX + Math.cos(orbit) * radius, target.posY + height, target.posZ + Math.sin(orbit) * radius)),
                isOrder() ? 0.06D : 0.1D);
    }

    private static Vec3d leashed(Vec3d to) {
        double dx = to.x - Nexus.JUNCTION_X, dz = to.z - Nexus.JUNCTION_Z;
        double reach = Math.sqrt(dx * dx + dz * dz);
        return reach > 50D ? new Vec3d(Nexus.JUNCTION_X + dx / reach * 50D, to.y, Nexus.JUNCTION_Z + dz / reach * 50D) : to;
    }

    private void idle(int ticks) {
        setState(State.IDLE);
        float pace = isEnraged() ? 0.65F : 1F;
        if (!isOrder() && OrderAndChaos.INSTANCE.chaosAmused()) {
            // Delighted, he plays with you a while between blows.
            pace *= 1.3F;
        }
        cooldown = bonusAttacks > 0 ? 5 : Math.max(10, (int) (ticks * pace));
        if (bonusAttacks > 0) {
            bonusAttacks--;
        }
    }

    private void choose(EntityPlayerMP target) {
        // When either of them is badly hurt, Order calls on his brother to cross their beams.
        if (isOrder() && OrderAndChaos.INSTANCE.chaos() != null && rand.nextInt(100) < 35
                && (getIntegrity() < 0.5F || OrderAndChaos.INSTANCE.chaos().getIntegrity() < 0.5F) && OrderAndChaos.INSTANCE.summonCrossing()) {
            setState(State.CROSSING);
            return;
        }
        State[] attacks = isOrder() ? ORDER_ATTACKS : CHAOS_ATTACKS;
        State next;
        do {
            next = attacks[rand.nextInt(attacks.length)];
        } while (next == last);
        last = next;
        setState(next);
    }

    /** Chaos, called to cross beams with his brother: he comes if he is free to. */
    public boolean joinCrossing() {
        if (getState() != State.IDLE) {
            return false;
        }
        setState(State.CROSSING);
        return true;
    }

    /** How hard they hit: each answers to how angered it was, and harder still alone. */
    private float might() {
        return OrderAndChaos.INSTANCE.might(getKind()) * (isEnraged() ? 1.3F : 1F);
    }

    // ------------------------------------------------------------------ Master Order

    /** The glare of his decree: a burning line from his eyes, its end crawling across the floor after you. */
    private void decree(@Nullable EntityPlayerMP target) {
        if (stateTicks == 1) {
            playSound(EternitySounds.COMET_BLAST, 8F, 0.6F);
            beam = target != null ? target.getPositionVector().add(rand.nextDouble() * 10D - 5D, 0D, rand.nextDouble() * 10D - 5D)
                    : getPositionVector();
        }
        if (beam != null && target != null) {
            Vec3d toward = target.getPositionVector().subtract(beam);
            double step = Math.min(toward.length(), isEnraged() ? 0.42D : 0.32D);
            if (step > 1.0E-3) {
                beam = beam.add(toward.normalize().scale(step));
            }
        }
        if (beam != null && stateTicks % 2 == 0) {
            Vec3d from = eyes();
            fx(MsgTribunalFx.GAZE, from.x, from.y, from.z, beam.x, beam.y, beam.z, 14, 3);
        }
        if (beam != null && stateTicks % 5 == 0) {
            hurtNear(beam, 2.2D, 0.07F, 0D);
        }
        if (stateTicks >= 70) {
            idle(50);
        }
    }

    /**
     * He lays down a law for everyone at the junction. For a little while it holds, and he
     * watches; break it, and a bolt of punishment falls on you.
     */
    private void edict(List<EntityPlayerMP> players) {
        int duration = 110;
        if (stateTicks == 1) {
            int rule = rand.nextInt(EDICTS.length);
            OrderAndChaos.INSTANCE.decree(rule, duration);
            fx(MsgTribunalFx.ORDER_EDICT, posX, posY, posZ, 0, 0, 0, rule, duration);
            OrderAndChaos.speak(true, EDICTS[rule]);
            playSound(EternitySounds.FINALE_DRUMS, 10F, 0.5F);
            playSound(SoundEvents.ENTITY_ENDERDRAGON_GROWL, 8F, 0.3F);
            punished.clear();
        }
        int rule = OrderAndChaos.INSTANCE.edict();
        // A moment's grace to hear it, then the law holds.
        if (rule >= 0 && stateTicks > 20) {
            for (EntityPlayerMP player : players) {
                if (rule == OrderAndChaos.EDICT_STILL) {
                    double moved = Math.pow(player.posX - player.lastTickPosX, 2) + Math.pow(player.posZ - player.lastTickPosZ, 2)
                            + Math.pow(Math.max(0D, player.posY - player.lastTickPosY), 2);
                    if (moved > 0.003D) {
                        punish(player);
                    }
                } else if (rule == OrderAndChaos.EDICT_GAZE) {
                    Vec3d toward = eyes().subtract(player.getPositionEyes(1F)).normalize();
                    if (player.getLook(1F).dotProduct(toward) < 0.7D) {
                        punish(player);
                    }
                }
            }
        }
        if (stateTicks >= duration) {
            idle(45);
        }
    }

    /** A bolt of punishment for someone who broke his edict. Not more than once a second. */
    public void punish(EntityPlayerMP player) {
        Integer lastTime = punished.get(player.getUniqueID());
        if (lastTime != null && ticksExisted - lastTime < 20) {
            return;
        }
        punished.put(player.getUniqueID(), ticksExisted);
        fx(MsgTribunalFx.ORDER_BOLT, player.posX, player.posY, player.posZ, 0, 0, 0, 0, 14);
        world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_LIGHTNING_THUNDER, SoundCategory.HOSTILE, 3F, 1.4F);
        judge(player, 0.12F);
    }

    /**
     * The floor around you divides into a perfect grid of golden tiles, lit in a pattern
     * symmetrical both ways. The lit tiles erupt; then every tile that was dark does.
     */
    private void tessellate(@Nullable EntityPlayerMP target) {
        if (stateTicks == 1) {
            if (target == null) {
                idle(20);
                return;
            }
            focus = new Vec3d(Math.floor(target.posX / TILE) * TILE + TILE / 2D, target.posY, Math.floor(target.posZ / TILE) * TILE + TILE / 2D);
            pattern = symmetricPattern();
            fx(MsgTribunalFx.ORDER_TILES, focus.x, focus.y, focus.z, TILE, 0, 0, pattern, 35);
            playSound(EternitySounds.ENTITY_VOICE, 8F, 0.45F);
        }
        if (focus == null) {
            return;
        }
        if (stateTicks == 35) {
            erupt(pattern);
            fx(MsgTribunalFx.ORDER_TILES, focus.x, focus.y, focus.z, TILE, 0, 0, ~pattern & 0x1FFFFFF, 32);
        }
        if (stateTicks == 67) {
            erupt(~pattern & 0x1FFFFFF);
        }
        if (stateTicks >= 85) {
            idle(45);
        }
    }

    /** Twenty-five tiles, lit so the pattern mirrors left to right and front to back; never all, never none. */
    private int symmetricPattern() {
        int mask;
        int count;
        do {
            int quarter = rand.nextInt(1 << 9);
            mask = 0;
            count = 0;
            for (int i = 0; i < 5; i++) {
                for (int j = 0; j < 5; j++) {
                    int qi = Math.min(i, 4 - i), qj = Math.min(j, 4 - j);
                    if ((quarter >> (qi * 3 + qj) & 1) != 0) {
                        mask |= 1 << (i * 5 + j);
                        count++;
                    }
                }
            }
        } while (count < 8 || count > 17);
        return mask;
    }

    private void erupt(int lit) {
        fx(MsgTribunalFx.ORDER_ERUPT, focus.x, focus.y, focus.z, TILE, 0, 0, lit, 16);
        world.playSound(null, focus.x, focus.y, focus.z, EternitySounds.RIFT_IMPACT, SoundCategory.HOSTILE, 6F, 1.1F);
        for (EntityPlayer player : world.playerEntities) {
            int i = MathHelper.floor((player.posX - focus.x) / TILE + 2.5D);
            int j = MathHelper.floor((player.posZ - focus.z) / TILE + 2.5D);
            if (i >= 0 && i < 5 && j >= 0 && j < 5 && Math.abs(player.posY - focus.y) < 4D && (lit >> (i * 5 + j) & 1) != 0) {
                judge(player, 0.18F);
                player.motionY += 0.6D;
                player.velocityChanged = true;
            }
        }
    }

    /** A golden line across the floor through you; everything is drawn to it; then judgement runs its length. */
    private void align(@Nullable EntityPlayerMP target, List<EntityPlayerMP> players) {
        if (stateTicks == 1) {
            if (target == null) {
                idle(20);
                return;
            }
            Vec3d start = new Vec3d(posX, target.posY, posZ);
            Vec3d dir = new Vec3d(target.posX - posX, 0D, target.posZ - posZ);
            dir = dir.lengthSquared() < 1.0E-4 ? facing() : dir.normalize();
            lineFrom = start;
            lineTo = start.add(dir.scale(44D));
            fx(MsgTribunalFx.ORDER_LINE, lineFrom.x, lineFrom.y, lineFrom.z, lineTo.x, lineTo.y, lineTo.z, 0, 46);
            playSound(EternitySounds.ENTITY_VOICE, 8F, 0.5F);
        }
        if (lineFrom == null) {
            return;
        }
        if (stateTicks < 40) {
            for (EntityPlayerMP player : players) {
                Vec3d on = nearestOnLine(player.getPositionVector(), lineFrom, lineTo);
                Vec3d pull = on.subtract(player.getPositionVector());
                pull = new Vec3d(pull.x, 0D, pull.z);
                if (pull.lengthSquared() > 0.04D && pull.length() < 22D) {
                    pull = pull.normalize().scale(0.075D);
                    player.motionX += pull.x;
                    player.motionZ += pull.z;
                    player.velocityChanged = true;
                }
            }
        }
        if (stateTicks == 42) {
            Vec3d from = eyes();
            fx(MsgTribunalFx.ORDER_BEAM, lineFrom.x, lineFrom.y + 0.5D, lineFrom.z, lineTo.x, lineTo.y + 0.5D, lineTo.z, 0, 18);
            fx(MsgTribunalFx.GAZE, from.x, from.y, from.z, lineFrom.x, lineFrom.y, lineFrom.z, 14, 10);
            world.playSound(null, lineFrom.x, lineFrom.y, lineFrom.z, EternitySounds.COMET_BLAST, SoundCategory.HOSTILE, 10F, 0.7F);
            for (EntityPlayer player : world.playerEntities) {
                if (nearestOnLine(player.getPositionVector(), lineFrom, lineTo).distanceTo(player.getPositionVector()) < 1.8D
                        && Math.abs(player.posY - lineFrom.y) < 3D) {
                    judge(player, 0.26F);
                }
            }
        }
        if (stateTicks >= 60) {
            idle(50);
        }
    }

    private static Vec3d nearestOnLine(Vec3d p, Vec3d a, Vec3d b) {
        Vec3d ab = b.subtract(a);
        double len = ab.lengthSquared();
        double t = len < 1.0E-6 ? 0D : MathHelper.clamp(p.subtract(a).dotProduct(ab) / len, 0D, 1D);
        return a.add(ab.scale(t));
    }

    // ------------------------------------------------------------------ together

    /**
     * Gold and deep space, crossed on you. Order and Chaos take opposite sides of you, each
     * throws a beam at the same point, and the point where they cross hunts you across the
     * floor. Only the crossing burns.
     */
    private void crossing(@Nullable EntityPlayerMP target) {
        OrderAndChaos encounter = OrderAndChaos.INSTANCE;
        if (target != null) {
            Vec3d side = new Vec3d(-(target.posZ - Nexus.JUNCTION_Z), 0D, target.posX - Nexus.JUNCTION_X);
            side = side.lengthSquared() < 1.0E-4 ? new Vec3d(1D, 0D, 0D) : side.normalize();
            glide(leashed(target.getPositionVector().add(side.scale(isOrder() ? 12D : -12D)).add(0D, 2D, 0D)), 0.08D);
        }
        if (isOrder()) {
            if (stateTicks == 1) {
                Vec3d start = target != null ? target.getPositionVector().add(rand.nextDouble() * 14D - 7D, 0D, rand.nextDouble() * 14D - 7D)
                        : getPositionVector();
                encounter.setCrossing(start);
                OrderAndChaos.speak(true, "Brother.");
                OrderAndChaos.speak(false, "Oh, THIS one. I love this one.");
            }
            Vec3d at = encounter.crossing();
            if (at != null && target != null) {
                Vec3d toward = target.getPositionVector().subtract(at);
                double step = Math.min(toward.length(), 0.28D);
                at = step > 1.0E-3 ? at.add(toward.normalize().scale(step)) : at;
                encounter.setCrossing(at);
            }
            if (at != null && stateTicks % 2 == 0) {
                Vec3d from = eyes();
                fx(MsgTribunalFx.GAZE, from.x, from.y, from.z, at.x, at.y, at.z, 10, 3);
            }
            if (at != null && stateTicks % 5 == 0) {
                hurtNear(at, 2.4D, 0.12F, 0.3D);
            }
            if (stateTicks >= 80) {
                encounter.setCrossing(null);
                idle(50);
            }
        } else {
            Vec3d at = encounter.crossing();
            if (at != null && stateTicks % 2 == 0) {
                Vec3d from = mouth();
                fx(MsgTribunalFx.CHAOS_BEAM, from.x, from.y, from.z, at.x, at.y, at.z, 0, 3);
            }
            if (at == null && stateTicks > 5 || stateTicks > 90) {
                idle(40);
            }
        }
    }

    // ------------------------------------------------------------------ Lord Chaos

    /** He rolls two vast dice of deep space, and whatever they show happens. */
    private void dice(@Nullable EntityPlayerMP target, List<EntityPlayerMP> players) {
        if (stateTicks == 1) {
            if (target == null) {
                idle(20);
                return;
            }
            int a = 1 + rand.nextInt(6), b = 1 + rand.nextInt(6);
            pattern = a * 10 + b;
            focus = target.getPositionVector().add(rand.nextDouble() * 8D - 4D, 0D, rand.nextDouble() * 8D - 4D);
            fx(MsgTribunalFx.CHAOS_DICE, focus.x, focus.y, focus.z, 0, 0, 0, pattern, 30);
            OrderAndChaos.speak(false, "Let's see what we get!");
            playSound(EternitySounds.ENTITY_GLITCH, 6F, 1.4F);
        }
        if (stateTicks == 32) {
            roll(pattern / 10 + pattern % 10, players);
        }
        if (stateTicks >= 80) {
            idle(30);
        }
    }

    /** What the dice show, happening. */
    private void roll(int sum, List<EntityPlayerMP> players) {
        switch (sum) {
            case 2:
                OrderAndChaos.speak(false, "Snake eyes! Oh, that's bad. For you.");
                for (EntityPlayerMP player : players) {
                    Vec3d at = player.getPositionVector();
                    fx(MsgTribunalFx.CHAOS_BURST, at.x, at.y, at.z, 5D, 0, 0, 0, 16);
                    later(16, () -> hurtNear(at, 5D, 0.3F, 0.8D));
                }
                break;
            case 3:
            case 4:
                OrderAndChaos.speak(false, "Stars! Everyone loves stars.");
                for (int i = 0; i < 10; i++) {
                    EntityPlayerMP player = players.isEmpty() ? null : players.get(rand.nextInt(players.size()));
                    if (player == null) {
                        break;
                    }
                    Vec3d land = player.getPositionVector().add(rand.nextDouble() * 12D - 6D, 0D, rand.nextDouble() * 12D - 6D);
                    Vec3d from = land.add(rand.nextDouble() * 30D - 15D, 45D, rand.nextDouble() * 30D - 15D);
                    int delay = rand.nextInt(40);
                    later(delay, () -> fx(MsgTribunalFx.CHAOS_STAR, from.x, from.y, from.z, land.x, land.y, land.z, 0, 12));
                    later(delay + 12, () -> {
                        fx(MsgTribunalFx.CHAOS_BURST, land.x, land.y, land.z, 2.5D, 0, 0, 0, 1);
                        world.playSound(null, land.x, land.y, land.z, EternitySounds.COMET_IMPACT, SoundCategory.HOSTILE, 4F, 1.2F);
                        hurtNear(land, 2.6D, 0.15F, 0.4D);
                    });
                }
                break;
            case 5:
            case 6:
                OrderAndChaos.speak(false, "Shuffle!");
                for (EntityPlayerMP player : players) {
                    scatter(player);
                }
                break;
            case 7:
                OrderAndChaos.speak(false, "Oops. That was a heal. Forget you saw that.");
                for (EntityPlayerMP player : players) {
                    player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + player.getMaxHealth() * 0.25F));
                }
                break;
            case 8:
            case 9:
                OrderAndChaos.speak(false, "Up you go!");
                for (EntityPlayerMP player : players) {
                    judge(player, 0.12F);
                    player.motionY = 1.3D;
                    player.velocityChanged = true;
                    later(30, () -> {
                        Vec3d at = player.getPositionVector();
                        fx(MsgTribunalFx.CHAOS_BURST, at.x, at.y, at.z, 3D, 0, 0, 0, 1);
                        hurtNear(at, 3D, 0.15F, 0D);
                    });
                }
                break;
            case 10:
            case 11:
                OrderAndChaos.speak(false, "Ow! Ha! Worth it.");
                dataManager.set(INTEGRITY, Math.max(0F, getIntegrity() - 0.06F));
                playSound(EternitySounds.COMET_IMPACT, 6F, 1.4F);
                if (getIntegrity() <= 0F) {
                    fallDazed();
                }
                break;
            default:
                OrderAndChaos.speak(false, "Double sixes! Again, again!");
                bonusAttacks = 2;
                break;
        }
    }

    /** Flung somewhere else on the junction, with everything in your hands rearranged. */
    private void scatter(EntityPlayerMP target) {
        double a = rand.nextDouble() * Math.PI * 2D, r = 4D + rand.nextDouble() * 13D;
        double x = Nexus.JUNCTION_X + Math.cos(a) * r, z = Nexus.JUNCTION_Z + Math.sin(a) * r;
        fx(MsgTribunalFx.CHAOS_SCATTER, target.posX, target.posY, target.posZ, x, Nexus.JUNCTION_Y, z, 0, 20);
        target.connection.setPlayerLocation(x, Nexus.JUNCTION_Y, z, target.rotationYaw + 180F, target.rotationPitch);
        world.playSound(null, x, Nexus.JUNCTION_Y, z, SoundEvents.ENTITY_ENDERMEN_TELEPORT, SoundCategory.HOSTILE, 2F, 0.5F);
        ItemStack[] hotbar = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            hotbar[i] = target.inventory.mainInventory.get(i);
        }
        for (int i = 8; i > 0; i--) {
            int j = rand.nextInt(i + 1);
            ItemStack swap = hotbar[i];
            hotbar[i] = hotbar[j];
            hotbar[j] = swap;
        }
        for (int i = 0; i < 9; i++) {
            target.inventory.mainInventory.set(i, hotbar[i]);
        }
        target.inventoryContainer.detectAndSendChanges();
        judge(target, 0.06F);
    }

    /** He opens his mouth onto deep space and inhales. Everything near is dragged toward it; what reaches it is bitten. */
    private void maw(@Nullable EntityPlayerMP target, List<EntityPlayerMP> players) {
        if (target != null && stateTicks < 70) {
            Vec3d away = new Vec3d(posX - target.posX, 0D, posZ - target.posZ);
            away = away.lengthSquared() < 1.0E-4 ? new Vec3d(1D, 0D, 0D) : away.normalize();
            glide(target.getPositionVector().add(away.scale(10D)).add(0D, -1D, 0D), 0.08D);
        }
        if (stateTicks == 8) {
            playSound(EternitySounds.AMBIENT_RUMBLE, 10F, 0.5F);
            OrderAndChaos.speak(false, "Come here. Come HERE.");
        }
        if (stateTicks >= 10 && stateTicks <= 70) {
            Vec3d mouth = mouth();
            for (EntityPlayerMP player : players) {
                Vec3d to = mouth.subtract(player.getPositionVector().add(0D, 1D, 0D));
                double d = to.length();
                if (d > 26D) {
                    continue;
                }
                Vec3d pull = to.normalize().scale(0.1D * (1D - d / 30D));
                player.motionX += pull.x;
                player.motionY += pull.y * 0.5D;
                player.motionZ += pull.z;
                player.velocityChanged = true;
                if (stateTicks % 5 == 0) {
                    fx(MsgTribunalFx.CHAOS_MAW, player.posX, player.posY + 1D, player.posZ, mouth.x, mouth.y, mouth.z, 0, 8);
                }
                if (d < 4.5D && struck.add(player.getUniqueID())) {
                    world.playSound(null, mouth.x, mouth.y, mouth.z, EternitySounds.COMET_IMPACT, SoundCategory.HOSTILE, 6F, 1.2F);
                    judge(player, 0.25F);
                    player.motionX = -pull.x * 15D;
                    player.motionY = 0.9D;
                    player.motionZ = -pull.z * 15D;
                    player.velocityChanged = true;
                }
            }
        }
        if (stateTicks >= 80) {
            idle(40);
        }
    }

    /** Little rifts of deep space, set loose across the floor, wandering at random and leaping without warning. */
    private void wander(@Nullable EntityPlayerMP target, List<EntityPlayerMP> players) {
        if (stateTicks == 1) {
            wanderers.clear();
            if (target == null) {
                idle(20);
                return;
            }
            for (int i = 0; i < 5; i++) {
                double a = rand.nextDouble() * Math.PI * 2D, r = 4D + rand.nextDouble() * 6D;
                wanderers.add(new double[]{target.posX + Math.cos(a) * r, target.posY + 0.8D, target.posZ + Math.sin(a) * r, 0D, 0D});
            }
            OrderAndChaos.speak(false, "Go on, little ones. Go play.");
            playSound(EternitySounds.ENTITY_GLITCH, 6F, 0.8F);
        }
        for (double[] w : wanderers) {
            // A drunkard's walk, and now and then a leap.
            w[3] = MathHelper.clamp(w[3] + (rand.nextDouble() - 0.5D) * 0.12D, -0.35D, 0.35D);
            w[4] = MathHelper.clamp(w[4] + (rand.nextDouble() - 0.5D) * 0.12D, -0.35D, 0.35D);
            if (rand.nextInt(35) == 0) {
                w[0] += rand.nextDouble() * 10D - 5D;
                w[2] += rand.nextDouble() * 10D - 5D;
            }
            w[0] += w[3];
            w[2] += w[4];
            Vec3d at = leashed(new Vec3d(w[0], w[1], w[2]));
            w[0] = at.x;
            w[2] = at.z;
            if (stateTicks % 2 == 0) {
                fx(MsgTribunalFx.CHAOS_WANDER, w[0], w[1], w[2], 1.3D, 0, 0, 0, 3);
            }
            for (EntityPlayerMP player : players) {
                if (player.getDistance(w[0], w[1], w[2]) < 1.8D) {
                    Integer hit = punished.get(player.getUniqueID());
                    if (hit == null || ticksExisted - hit >= 10) {
                        punished.put(player.getUniqueID(), ticksExisted);
                        judge(player, 0.1F);
                        player.motionX += w[3] * 3D;
                        player.motionY += 0.5D;
                        player.motionZ += w[4] * 3D;
                        player.velocityChanged = true;
                    }
                }
            }
        }
        if (target != null) {
            circle(target);
        }
        if (stateTicks >= 140) {
            wanderers.clear();
            idle(40);
        }
    }

    /** He hides among copies of himself. Strike a mask and it bursts; strike him and the masks fall away. */
    private void masks(@Nullable EntityPlayerMP target) {
        if (stateTicks == 1) {
            OrderAndChaos.speak(false, "Guess which one!");
            playSound(EternitySounds.ENTITY_GLITCH, 8F, 1.2F);
            for (int i = 1; i <= 2; i++) {
                EntityOrderChaos mask = new EntityOrderChaos(world);
                mask.setKind(Kind.CHAOS);
                mask.dataManager.set(MASK, true);
                mask.dataManager.set(ENRAGED, isEnraged());
                mask.dataManager.set(INTEGRITY, getIntegrity());
                mask.maskIndex = i;
                mask.setLocationAndAngles(posX, posY, posZ, rotationYaw, 0F);
                mask.setState(State.IDLE);
                OrderAndChaos.INSTANCE.addMask(mask);
                world.spawnEntity(mask);
                fx(MsgTribunalFx.CHAOS_SCATTER, posX, posY, posZ, posX, posY, posZ, 0, 20);
            }
        }
        if (target != null) {
            orbitAs(0, target);
            if (stateTicks % 40 == 20) {
                // The real one still bites, now and then, from among the masks.
                Vec3d at = target.getPositionVector();
                fx(MsgTribunalFx.CHAOS_BURST, at.x, at.y, at.z, 2D, 0, 0, 0, 14);
                later(14, () -> hurtNear(at, 2.2D, 0.1F, 0.3D));
            }
        }
        if (stateTicks >= 200 || OrderAndChaos.INSTANCE.masks().isEmpty() && stateTicks > 5) {
            OrderAndChaos.INSTANCE.dropMasks();
            idle(40);
        }
    }

    /** The three of them, the real one and two masks, wheel round you a third of a turn apart. */
    private void orbitAs(int index, EntityPlayerMP target) {
        double a = ticksExisted * 0.035D + index * Math.PI * 2D / 3D;
        double r = 7D + Math.sin(ticksExisted * 0.06D + index) * 1.5D;
        glide(leashed(new Vec3d(target.posX + Math.cos(a) * r, target.posY + 0.6D, target.posZ + Math.sin(a) * r)), 0.15D);
    }

    /** A mask only wheels with the others; it has no will of its own. */
    private void maskTick(@Nullable EntityPlayerMP target) {
        if (target != null) {
            face(target);
            orbitAs(maskIndex, target);
        }
    }

    // ------------------------------------------------------------------ beaten, absorbed, gone

    /** Beaten, it lies dazed a while. A charged Regulator can draw it in now. */
    private void fallDazed() {
        endBusiness();
        dataManager.set(INTEGRITY, 0F);
        begged = false;
        setState(State.DOWNED);
        playSound(EternitySounds.COMET_IMPACT, 10F, 0.4F);
        playSound(EternitySounds.RIFT_CLOSE, 10F, 0.6F);
        OrderAndChaos.speak(isOrder(), isOrder() ? "This... is... IRREGULAR." : "Ha... ha. Oh. Oh, that actually hurt.");
    }

    private void downed(List<EntityPlayerMP> players) {
        setPosition(posX, posY - (stateTicks < 30 ? 0.08D : 0D), posZ);
        if (stateTicks % 20 == 1) {
            TextComponentString prompt = new TextComponentString((isOrder() ? "Master Order" : "Lord Chaos")
                    + " is beaten. Raise a charged Astral Regulator at him, now!");
            prompt.getStyle().setColor(isOrder() ? TextFormatting.GOLD : TextFormatting.AQUA);
            for (EntityPlayerMP player : players) {
                player.sendStatusMessage(prompt, true);
            }
        }
        if (!begged) {
            for (EntityPlayerMP player : players) {
                if (ItemAstralRegulator.holdsCharged(player) && player.getDistance(this) < 20F) {
                    begged = true;
                    OrderAndChaos.speak(isOrder(), isOrder() ? "Keep that away from me. I am warning you."
                            : "Hey. Hey, hey, hey. Let's talk about this.");
                    break;
                }
            }
        }
        if (stateTicks >= DOWN_TICKS) {
            withdraw();
        }
    }

    /** A Regulator, empty or not, is raised while it lies beaten. It is drawn in. */
    public boolean absorbBy(EntityPlayerMP player, EnumHand hand) {
        if (getState() != State.DOWNED) {
            return false;
        }
        absorber = player;
        absorbHand = hand;
        setState(State.ABSORBING);
        playSound(EternitySounds.RIFT_OPEN, 12F, 0.4F);
        playSound(EternitySounds.FINALE_DRUMS, 6F, isOrder() ? 0.7F : 0.9F);
        OrderAndChaos.speak(isOrder(), isOrder() ? "No. NO. Not a vessel. Not ME." : "Wait. Wait, I don't like jars. I don't like JARS.");
        return true;
    }

    private static final String[] ORDER_FLINCH = {"Do not point that at me.", "Lower it.", "That does not work on us standing up. Lower it."};
    private static final String[] CHAOS_FLINCH = {"Whoa! Careful where you point that thing.", "Ha! Not yet. You have to beat me first.",
            "Put that down. You're making me nervous, and I don't GET nervous."};

    /** A Regulator raised at it while it still stands: it cannot take it like this, but it flinches all the same. */
    public boolean flinch(EntityPlayerMP player) {
        if (ticksExisted - lastFlinch < 40) {
            return true;
        }
        Vec3d away = centre().subtract(player.getPositionEyes(1F));
        away = new Vec3d(away.x, 0D, away.z);
        away = away.lengthSquared() < 1.0E-4 ? new Vec3d(0D, 1D, 0D) : away.normalize().add(0D, 0.35D, 0D).normalize();
        recoilDir = away;
        recoilTicks = 12;
        playSound(isOrder() ? SoundEvents.ENTITY_ENDERDRAGON_GROWL : EternitySounds.ENTITY_GLITCH, 6F, isOrder() ? 0.7F : 1.3F);
        if (ticksExisted - lastFlinch > 200) {
            String[] lines = isOrder() ? ORDER_FLINCH : CHAOS_FLINCH;
            OrderAndChaos.speak(isOrder(), lines[rand.nextInt(lines.length)]);
        }
        lastFlinch = ticksExisted;
        return true;
    }

    private void absorbing() {
        if (absorber == null || absorber.isDead || absorber.world != world) {
            setState(State.DOWNED);
            return;
        }
        if (stateTicks % 4 == 0) {
            Vec3d from = centre();
            fx(MsgTribunalFx.ABSORB, from.x, from.y, from.z, 0, 0, 0, absorber.getEntityId(), 8);
        }
        if (stateTicks % 25 == 0) {
            playSound(EternitySounds.RIFT_IMPACT, 10F, 0.5F + stateTicks / (float) ABSORB_TICKS);
        }
        if (stateTicks >= ABSORB_TICKS) {
            playSound(EternitySounds.COMET_BLAST, 14F, isOrder() ? 0.5F : 0.7F);
            OrderAndChaos.INSTANCE.absorbed(this, absorber, absorbHand);
            setDead();
        }
    }

    // ------------------------------------------------------------------ the blows themselves

    private void hurtNear(Vec3d at, double radius, float fraction, double lift) {
        for (EntityPlayer player : world.playerEntities) {
            if (player.getDistance(at.x, at.y, at.z) < radius) {
                judge(player, fraction);
                if (lift > 0D) {
                    player.motionY += lift;
                    player.velocityChanged = true;
                }
            }
        }
    }

    /** A share of their max health, through any armour. Spectators are beneath it; creative mode is not. */
    private void judge(EntityPlayer player, float fraction) {
        if (player.isSpectator() || fraction <= 0F) {
            return;
        }
        player.hurtResistantTime = 0;
        player.attackEntityFrom(EternityDamage.orderAndChaos(this), player.getMaxHealth() * fraction * might());
    }

    private void fx(byte type, double x, double y, double z, double a, double b, double c, int id, int time) {
        EternityNetwork.sendToDimension(new MsgTribunalFx(type, x, y, z, a, b, c, id, time), dimension);
    }

    // ------------------------------------------------------------------ damage

    /**
     * Any blow from any player counts, from any distance, but none takes more than a sliver
     * of what they are. Struck with Eternity himself in hand, after they have recognised him
     * there, blows land twice as hard: the heart of Eternity knows its master. While Order's
     * edict forbids striking, a blow at either of them is answered instead of landing. A mask
     * bursts in your face; the real Chaos, struck, drops his masks.
     */
    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (world.isRemote || isEntityInvulnerable(source)) {
            return false;
        }
        State state = getState();
        if (state == State.ARRIVE || state == State.TALK || state == State.WITHDRAW || state == State.DOWNED || state == State.ABSORBING) {
            return false;
        }
        Entity attacker = source.getTrueSource();
        if (!(attacker instanceof EntityPlayer)) {
            return false;
        }
        if (OrderAndChaos.INSTANCE.edict() == OrderAndChaos.EDICT_PEACE && attacker instanceof EntityPlayerMP) {
            EntityOrderChaos order = OrderAndChaos.INSTANCE.order();
            if (order != null) {
                order.punish((EntityPlayerMP) attacker);
            }
            return false;
        }
        if (isMask()) {
            Vec3d at = centre();
            fx(MsgTribunalFx.CHAOS_BURST, at.x, at.y, at.z, 4D, 0, 0, 0, 1);
            world.playSound(null, at.x, at.y, at.z, EternitySounds.ENTITY_GLITCH, SoundCategory.HOSTILE, 4F, 1.6F);
            hurtNear(at, 4.5D, 0.15F, 0.5D);
            OrderAndChaos.speak(false, "Wrong one!");
            setDead();
            return true;
        }
        if (state == State.MASKS) {
            OrderAndChaos.speak(false, "Oh, you found me. Spoilsport.");
            OrderAndChaos.INSTANCE.dropMasks();
            idle(30);
        }
        double max = EternityConfig.inbetweener.mastersHealth;
        float scale = OrderAndChaos.INSTANCE.resonance() && ItemAstralRegulator.holdsCharged(attacker) ? 2F : 1F;
        float dealt = (float) Math.min(amount * scale, max * EternityConfig.inbetweener.mastersMaxHitFraction);
        if (!(dealt > 0F)) {
            return false;
        }
        float left = getIntegrity() - dealt / (float) max;
        playSound(isOrder() ? EternitySounds.COMET_IMPACT : EternitySounds.ENTITY_GLITCH, 3F, 1.3F + rand.nextFloat() * 0.3F);
        if (left <= 0F) {
            fallDazed();
        } else {
            dataManager.set(INTEGRITY, left);
        }
        return true;
    }

    @Override
    public void onKillCommand() {
        withdraw();
    }

    @Override
    public void onDeath(DamageSource cause) {
        setHealth(getMaxHealth());
    }

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
    public boolean canBeCollidedWith() {
        return getState() != State.WITHDRAW && getState() != State.ABSORBING;
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

    /** Never written to chunks; they come when they are called for. */
    @Override
    public boolean writeToNBTOptional(NBTTagCompound compound) {
        return false;
    }

    @Override
    public boolean isInRangeToRenderDist(double distance) {
        return true;
    }

    @Override
    public SoundCategory getSoundCategory() {
        return SoundCategory.HOSTILE;
    }

    /** The masks have no boss bar of their own: you have to work out which is real. */
    @Override
    public void addTrackingPlayer(EntityPlayerMP player) {
        super.addTrackingPlayer(player);
        if (!isMask()) {
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
        return getEntityBoundingBox().grow(SIZE);
    }
}
