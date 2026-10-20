package com.matoon.eternity.entity;

import com.matoon.eternity.EternityConfig;
import com.matoon.eternity.EternitySounds;
import com.matoon.eternity.item.ItemAstralRegulator;
import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.network.MsgTribunalFx;
import com.matoon.eternity.world.StarChamber;
import com.matoon.eternity.world.TribunalDirector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The Living Tribunal, risen from his throne to fight someone who refused his
 * judgement. He cannot die. Beaten, he falls to his knees for a few seconds; in that
 * moment a charged Astral Regulator can draw him in. Miss it and he rises, whole again,
 * and ascends: every time he gets back up he wields more of his power, harder and faster.
 *
 * His judgements, by the ascension that unlocks them:
 * <ol start="0">
 * <li><b>The Gavel</b>, a sigil and then his fist. <b>Equity's Gaze</b>, beams of white
 * fire sweeping after you. <b>Necessity's Storm</b>, lightning wherever a sigil burns.
 * <b>The Scales of Balance</b>, two great scale-pans of light crashing down in turn.</li>
 * <li><b>Sentence</b>, a cage of light that holds you and bursts. <b>Edict of Exile</b>,
 * the floor opening onto the void and dragging you in. <b>Convergence</b>, golden
 * blades circling you and closing.</li>
 * <li><b>Pillars of Creation</b>, columns of light marching at you. <b>Cosmic Verdict</b>,
 * a ring of light racing across the whole floor: jump it. <b>Weight of Judgement</b>,
 * crushing gravity that pins you down.</li>
 * <li><b>Erasure</b>, a wall of light sweeping the length of the hall, with one gap in
 * it. <b>Multiversal Echoes</b>, ghosts of him from other universes bringing their fists
 * down beside his.</li>
 * </ol>
 */
public class EntityLivingTribunal extends EntityLiving {

    /** Blocks per model pixel: he stands nearly sixty blocks tall. */
    public static final float SCALE = 1.2F;
    public static final float HEIGHT_PX = 49F;
    public static final float WIDTH_PX = 14F;
    /** How close to his body a blow must be struck from. */
    public static final double STRIKE_REACH = 8D;
    public static final int MAX_ASCENSION = 3;

    public enum State {
        EMERGE, IDLE, GAVEL, GAZE, STORM, SCALES,
        SENTENCE, EXILE, CONVERGENCE,
        PILLARS, VERDICT, WEIGHT,
        ERASURE, ECHOES,
        DOWNED, RISING, ABSORBING
    }

    /** Which judgements each ascension adds. */
    private static final State[][] TIERS = {
            {State.GAVEL, State.GAZE, State.STORM, State.SCALES},
            {State.SENTENCE, State.EXILE, State.CONVERGENCE},
            {State.PILLARS, State.VERDICT, State.WEIGHT},
            {State.ERASURE, State.ECHOES}};

    public static final int GAVEL_TICKS = 55;
    public static final int GAVEL_STRIKE = 30;
    public static final int RISE_TICKS = 60;
    public static final int ABSORB_TICKS = 140;

    private static final DataParameter<Byte> STATE = EntityDataManager.createKey(EntityLivingTribunal.class, DataSerializers.BYTE);
    private static final DataParameter<Float> INTEGRITY = EntityDataManager.createKey(EntityLivingTribunal.class, DataSerializers.FLOAT);
    private static final DataParameter<Byte> ASCENSION = EntityDataManager.createKey(EntityLivingTribunal.class, DataSerializers.BYTE);

    private final BossInfoServer bossInfo = (BossInfoServer) new BossInfoServer(
            new TextComponentString("The Living Tribunal"), BossInfo.Color.YELLOW, BossInfo.Overlay.NOTCHED_20)
            .setDarkenSky(true);

    private int stateTicks;
    private int cooldown = 60;
    private Vec3d focus;
    private Vec3d second;
    private Vec3d gazeEnd;
    private double gapX;
    private final Set<UUID> struck = new HashSet<>();
    private EntityPlayerMP absorber;
    private EnumHand absorbHand = EnumHand.MAIN_HAND;
    private final List<Strike> strikes = new ArrayList<>();

    /** Client: ticks since the synced state last changed. */
    public int clientStateTicks;

    private enum Kind { BOLT, PILLAR, FIST }

    /** A blow that lands where a sigil marked, a moment later. */
    private static final class Strike {
        final Kind kind;
        final Vec3d at;
        final int when;

        Strike(Kind kind, Vec3d at, int when) {
            this.kind = kind;
            this.at = at;
            this.when = when;
        }
    }

    public EntityLivingTribunal(World world) {
        super(world);
        setSize(WIDTH_PX * SCALE, HEIGHT_PX * SCALE);
        noClip = true;
        isImmuneToFire = true;
        experienceValue = 0;
        ignoreFrustumCheck = true;
        setNoGravity(true);
        // Entity lookups only search this far past the chunks a box touches.
        World.MAX_ENTITY_RADIUS = Math.max(World.MAX_ENTITY_RADIUS, width / 2D + 2D);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(STATE, (byte) State.EMERGE.ordinal());
        dataManager.register(INTEGRITY, 1F);
        dataManager.register(ASCENSION, (byte) 0);
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

    public State getState() {
        int ordinal = dataManager.get(STATE);
        return ordinal >= 0 && ordinal < State.values().length ? State.values()[ordinal] : State.IDLE;
    }

    private void setState(State state) {
        dataManager.set(STATE, (byte) state.ordinal());
        stateTicks = 0;
        struck.clear();
    }

    /** What is left of him, 0 to 1. */
    public float getIntegrity() {
        return dataManager.get(INTEGRITY);
    }

    /** How many times he has risen again, each time stronger. */
    public int getAscension() {
        return dataManager.get(ASCENSION);
    }

    public boolean isDown() {
        return getState() == State.DOWNED;
    }

    /** Every judgement hits harder for each time he has risen. */
    private float might() {
        return 1F + 0.25F * getAscension();
    }

    /** The direction he faces. */
    private Vec3d facing() {
        float yaw = (float) Math.toRadians(rotationYaw);
        return new Vec3d(-MathHelper.sin(yaw), 0D, MathHelper.cos(yaw));
    }

    /** Between his eyes, where the light of his gaze comes from. */
    public Vec3d eyes() {
        return new Vec3d(posX, posY + 41.5D * SCALE, posZ).add(facing().scale(5D * SCALE));
    }

    /** The void in his chest. */
    public Vec3d heart() {
        return new Vec3d(posX, posY + 30D * SCALE, posZ).add(facing().scale(3.2D * SCALE));
    }

    /** Distance from a point to the nearest part of his body's box. */
    public static double distanceToBody(Vec3d p, AxisAlignedBB box) {
        double dx = Math.max(Math.max(box.minX - p.x, 0D), p.x - box.maxX);
        double dy = Math.max(Math.max(box.minY - p.y, 0D), p.y - box.maxY);
        double dz = Math.max(Math.max(box.minZ - p.z, 0D), p.z - box.maxZ);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /** Client: whether he lies beaten anywhere near this player, so a right-click may absorb him. */
    public static boolean downedNear(EntityPlayer player) {
        for (Entity entity : player.world.loadedEntityList) {
            if (entity instanceof EntityLivingTribunal && ((EntityLivingTribunal) entity).isDown() && entity.getDistance(player) < 160F) {
                return true;
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
        TribunalDirector director = TribunalDirector.INSTANCE;
        if (!director.claim(this)) {
            setDead();
            return;
        }
        stateTicks++;
        EntityPlayerMP target = director.opponent();
        if (target != null && (target.world != world || target.isSpectator())) {
            target = null;
        }
        tickStrikes();

        State state = getState();
        if (target != null && state != State.DOWNED && state != State.RISING && state != State.ABSORBING) {
            face(target);
        }
        switch (state) {
            case EMERGE:
                if (stateTicks > 60) {
                    idle(30);
                }
                break;
            case IDLE:
                if (target != null) {
                    approach(target);
                    if (--cooldown <= 0) {
                        chooseJudgement(target);
                    }
                }
                break;
            case GAVEL:
                gavel(target);
                break;
            case GAZE:
                gaze(target);
                break;
            case STORM:
                storm(target);
                break;
            case SCALES:
                scales(target);
                break;
            case SENTENCE:
                sentence(target);
                break;
            case EXILE:
                exile(target);
                break;
            case CONVERGENCE:
                convergence(target);
                break;
            case PILLARS:
                pillars(target);
                break;
            case VERDICT:
                verdict();
                break;
            case WEIGHT:
                weight(target);
                break;
            case ERASURE:
                erasure();
                break;
            case ECHOES:
                echoes(target);
                break;
            case DOWNED:
                if (target != null && stateTicks % 20 == 1) {
                    TextComponentString prompt = new TextComponentString("The Living Tribunal has fallen. Raise the Astral Regulator, now!");
                    prompt.getStyle().setColor(TextFormatting.GOLD);
                    target.sendStatusMessage(prompt, true);
                }
                if (stateTicks >= EternityConfig.tribunal.downSeconds * 20) {
                    rise(target);
                }
                break;
            case RISING:
                dataManager.set(INTEGRITY, Math.min(1F, (float) stateTicks / RISE_TICKS));
                if (stateTicks >= RISE_TICKS) {
                    dataManager.set(INTEGRITY, 1F);
                    idle(40);
                }
                break;
            case ABSORBING:
                absorbing();
                break;
        }

        if (ticksExisted % 5 == 0) {
            updateBossBar();
        }
    }

    private void updateBossBar() {
        State state = getState();
        int ascension = getAscension();
        String title = "The Living Tribunal" + (ascension > 0 ? " — Ascension " + roman(ascension) : "");
        bossInfo.setPercent(getIntegrity());
        bossInfo.setName(new TextComponentString(state == State.DOWNED ? title + " (fallen)" : title));
        bossInfo.setColor(state == State.DOWNED ? BossInfo.Color.WHITE
                : ascension >= 3 ? BossInfo.Color.RED : ascension >= 1 ? BossInfo.Color.PURPLE : BossInfo.Color.YELLOW);
    }

    private static String roman(int n) {
        return n == 1 ? "I" : n == 2 ? "II" : n == 3 ? "III" : String.valueOf(n);
    }

    private void idle(int ticks) {
        setState(State.IDLE);
        float pace = 1F - 0.15F * getAscension() - (getIntegrity() < 0.5F ? 0.2F : 0F);
        cooldown = Math.max(8, (int) (ticks * pace));
    }

    private void face(Entity target) {
        double dx = target.posX - posX;
        double dz = target.posZ - posZ;
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90F;
        rotationYaw = rotationYaw + MathHelper.wrapDegrees(yaw - rotationYaw) * 0.15F;
        rotationYawHead = rotationYaw;
    }

    /** He keeps his distance, as a judge does, pacing to keep the defendant before him. */
    private void approach(Entity target) {
        double dx = target.posX - posX;
        double dz = target.posZ - posZ;
        double dist = Math.sqrt(dx * dx + dz * dz);
        double speed = dist > 30D ? 0.22D : dist < 14D ? -0.12D : 0D;
        if (speed == 0D || dist < 1.0E-3) {
            return;
        }
        double nx = posX + dx / dist * speed;
        double nz = posZ + dz / dist * speed;
        if (StarChamber.inside(nx, StarChamber.THRONE_Y + 1D, nz)) {
            setPosition(nx, StarChamber.THRONE_Y, nz);
        }
    }

    /** Picks from every judgement his ascension allows, leaning toward the newest and worst. */
    private void chooseJudgement(EntityPlayerMP target) {
        List<State> choices = new ArrayList<>();
        int ascension = Math.min(getAscension(), MAX_ASCENSION);
        for (int tier = 0; tier <= ascension; tier++) {
            for (State state : TIERS[tier]) {
                if (state == State.GAVEL && getDistance(target) > 30D) {
                    continue;
                }
                // The newest judgements come up twice as often.
                choices.add(state);
                if (tier == ascension && tier > 0) {
                    choices.add(state);
                }
            }
        }
        setState(choices.get(rand.nextInt(choices.size())));
    }

    // ------------------------------------------------------------------ ascension 0

    /** A golden sigil where they stand; then his fist. */
    private void gavel(@Nullable EntityPlayerMP target) {
        if (stateTicks == 1) {
            focus = target != null ? floor(target.posX, target.posZ) : floor(posX, posZ + 10D);
            fx(MsgTribunalFx.SIGIL, focus.x, focus.y, focus.z, 9D, 0, 0, 0, GAVEL_STRIKE);
            playSound(EternitySounds.ENTITY_VOICE, 8F, 0.35F);
        }
        if (stateTicks == GAVEL_STRIKE && focus != null) {
            fist(focus, 9D, 0.4F);
        }
        if (stateTicks >= GAVEL_TICKS) {
            idle(45);
        }
    }

    /** His eyes blaze; then the light of judgement sweeps the floor after them. */
    private void gaze(@Nullable EntityPlayerMP target) {
        if (stateTicks == 1) {
            playSound(EternitySounds.COMET_BLAST, 10F, 0.6F);
            gazeEnd = target != null ? floor(target.posX, target.posZ) : floor(posX, posZ + 20D);
        }
        if (stateTicks >= 30 && stateTicks < 90 && gazeEnd != null) {
            if (target != null) {
                Vec3d want = floor(target.posX, target.posZ);
                Vec3d step = want.subtract(gazeEnd);
                double len = step.length();
                double speed = 0.7D + 0.15D * getAscension();
                gazeEnd = len < speed ? want : gazeEnd.add(step.scale(speed / len));
            }
            Vec3d from = eyes();
            if (stateTicks % 3 == 0) {
                fx(MsgTribunalFx.GAZE, from.x, from.y, from.z, gazeEnd.x, gazeEnd.y, gazeEnd.z, 36, 5);
            }
            if (stateTicks % 5 == 0) {
                for (EntityPlayer player : world.playerEntities) {
                    if (distanceToSegment(player.getPositionVector().add(0, 1D, 0), from, gazeEnd) < 2.4D) {
                        judge(player, 0.07F);
                        player.setFire(3);
                    }
                }
            }
        }
        if (stateTicks >= 100) {
            idle(50);
        }
    }

    /** Lightning from the void in his chest, falling wherever a sigil has burned. */
    private void storm(@Nullable EntityPlayerMP target) {
        if (stateTicks == 1) {
            playSound(SoundEvents.ENTITY_LIGHTNING_THUNDER, 10F, 0.4F);
        }
        int every = Math.max(6, 12 - 2 * getAscension());
        if (target != null && stateTicks % every == 1 && stateTicks < 72) {
            for (int k = 0; k < 3 + getAscension(); k++) {
                double ox = k == 0 ? 0D : (rand.nextDouble() - 0.5D) * 16D;
                double oz = k == 0 ? 0D : (rand.nextDouble() - 0.5D) * 16D;
                Vec3d at = floor(target.posX + ox, target.posZ + oz);
                fx(MsgTribunalFx.SIGIL, at.x, at.y, at.z, 3D, 0, 0, 0, 20);
                strikes.add(new Strike(Kind.BOLT, at, ticksExisted + 20));
            }
        }
        if (stateTicks >= 100) {
            idle(50);
        }
    }

    /** The Scales of Balance: two pans of light, one on each side of them, crashing down in turn. */
    private void scales(@Nullable EntityPlayerMP target) {
        if (stateTicks == 1) {
            Vec3d at = target != null ? floor(target.posX, target.posZ) : floor(posX, posZ + 20D);
            Vec3d side = facing().crossProduct(new Vec3d(0, 1, 0)).normalize().scale(7D);
            focus = at.add(side);
            second = at.subtract(side);
            fx(MsgTribunalFx.SIGIL, focus.x, focus.y, focus.z, 9D, 0, 0, 0, 35);
            fx(MsgTribunalFx.PAN, focus.x, focus.y, focus.z, 9D, 0, 0, 0, 35);
            playSound(EternitySounds.ENTITY_VOICE, 8F, 0.4F);
        }
        if (stateTicks == 35 && focus != null) {
            fist(focus, 9D, 0.3F);
        }
        if (stateTicks == 40 && second != null) {
            fx(MsgTribunalFx.SIGIL, second.x, second.y, second.z, 9D, 0, 0, 0, 30);
            fx(MsgTribunalFx.PAN, second.x, second.y, second.z, 9D, 0, 0, 0, 30);
        }
        if (stateTicks == 70 && second != null) {
            fist(second, 9D, 0.3F);
        }
        if (stateTicks >= 90) {
            idle(50);
        }
    }

    // ------------------------------------------------------------------ ascension I

    /** Sentence: a cage of light springs up around them, holds them, and bursts. */
    private void sentence(@Nullable EntityPlayerMP target) {
        if (stateTicks == 1) {
            focus = target != null ? floor(target.posX, target.posZ) : floor(posX, posZ + 20D);
            fx(MsgTribunalFx.CAGE, focus.x, focus.y, focus.z, 5D, 0, 0, 0, 60);
            playSound(EternitySounds.RIFT_OPEN, 8F, 1.2F);
        }
        if (focus != null && stateTicks < 60) {
            for (EntityPlayer player : world.playerEntities) {
                double dx = player.posX - focus.x;
                double dz = player.posZ - focus.z;
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d < 7D && d > 3.5D) {
                    // The bars hold: pressing against them only pushes you back in.
                    player.motionX -= dx / d * 0.4D;
                    player.motionZ -= dz / d * 0.4D;
                    player.velocityChanged = true;
                }
                if (d < 7D) {
                    player.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 10, 3, false, false));
                }
            }
        }
        if (stateTicks == 60 && focus != null) {
            fist(focus, 6D, 0.35F);
        }
        if (stateTicks >= 75) {
            idle(50);
        }
    }

    /** Edict of Exile: the floor beneath them opens onto the void, and the void pulls. */
    private void exile(@Nullable EntityPlayerMP target) {
        if (stateTicks == 1) {
            focus = target != null ? floor(target.posX, target.posZ) : floor(posX, posZ + 20D);
            fx(MsgTribunalFx.VOID, focus.x, focus.y, focus.z, 8D, 0, 0, 0, 90);
            playSound(EternitySounds.AMBIENT_RUMBLE, 10F, 0.5F);
        }
        if (focus != null && stateTicks > 8 && stateTicks < 90) {
            for (EntityPlayer player : world.playerEntities) {
                Vec3d off = focus.subtract(player.getPositionVector());
                double d = off.length();
                if (d < 16D && d > 0.5D) {
                    Vec3d pull = off.scale(0.06D * (1D - d / 16D) * might() / d * 4D);
                    player.motionX += pull.x;
                    player.motionZ += pull.z;
                    player.velocityChanged = true;
                }
                if (d < 6D && stateTicks % 10 == 0) {
                    judge(player, 0.08F);
                }
            }
        }
        if (stateTicks >= 100) {
            idle(50);
        }
    }

    /** Convergence: twelve golden blades circle them, faster and closer, and meet. */
    private void convergence(@Nullable EntityPlayerMP target) {
        if (stateTicks == 1) {
            focus = target != null ? floor(target.posX, target.posZ) : floor(posX, posZ + 20D);
            fx(MsgTribunalFx.BLADES, focus.x, focus.y, focus.z, 14D, 0, 0, 0, 50);
            playSound(EternitySounds.OMEN_SHIMMER, 10F, 0.6F);
        }
        if (stateTicks == 50 && focus != null) {
            fist(focus, 5D, 0.4F);
        }
        if (stateTicks >= 70) {
            idle(50);
        }
    }

    // ------------------------------------------------------------------ ascension II

    /** Pillars of Creation: columns of light bursting out of the floor in a line, marching at them. */
    private void pillars(@Nullable EntityPlayerMP target) {
        if (stateTicks == 1) {
            Vec3d to = target != null ? floor(target.posX, target.posZ) : floor(posX, posZ + 40D);
            Vec3d from = floor(posX, posZ);
            Vec3d dir = to.subtract(from);
            dir = dir.lengthSquared() < 1.0E-4 ? facing() : dir.normalize();
            for (int i = 0; i < 16; i++) {
                Vec3d at = from.add(dir.scale(8D + 3.2D * i));
                if (!StarChamber.inside(at.x, at.y + 1D, at.z)) {
                    break;
                }
                strikes.add(new Strike(Kind.PILLAR, at, ticksExisted + 12 + i * 2));
                fx(MsgTribunalFx.SIGIL, at.x, at.y, at.z, 2.2D, 0, 0, 0, 12 + i * 2);
            }
            playSound(SoundEvents.ENTITY_ENDERDRAGON_GROWL, 8F, 0.5F);
        }
        if (stateTicks >= 70) {
            idle(45);
        }
    }

    /** Cosmic Verdict: the void in his chest flares, and a ring of light races across the whole floor. */
    private void verdict() {
        int start = 40;
        int span = 40;
        double reach = 70D;
        if (stateTicks == 1) {
            focus = floor(posX, posZ);
            fx(MsgTribunalFx.SIGIL, focus.x, focus.y, focus.z, 12D, 0, 0, 0, start);
            playSound(EternitySounds.COMET_BLAST, 12F, 0.4F);
        }
        if (stateTicks == start && focus != null) {
            fx(MsgTribunalFx.NOVA, focus.x, focus.y, focus.z, reach, 0, 0, 0, span);
            playSound(SoundEvents.ENTITY_GENERIC_EXPLODE, 12F, 0.5F);
        }
        if (focus != null && stateTicks > start && stateTicks <= start + span) {
            double r = reach * (stateTicks - start) / span;
            for (EntityPlayer player : world.playerEntities) {
                double dx = player.posX - focus.x;
                double dz = player.posZ - focus.z;
                double d = Math.sqrt(dx * dx + dz * dz);
                // Only those standing on the floor are caught; those who leapt it pass over.
                if (Math.abs(d - r) < 1.8D && player.onGround && struck.add(player.getUniqueID())) {
                    judge(player, 0.3F);
                    player.motionY += 1D;
                    player.velocityChanged = true;
                }
            }
        }
        if (stateTicks >= start + span + 10) {
            idle(50);
        }
    }

    /** Weight of Judgement: the full weight of his attention presses them into the floor. */
    private void weight(@Nullable EntityPlayerMP target) {
        if (stateTicks == 1) {
            focus = target != null ? floor(target.posX, target.posZ) : floor(posX, posZ + 20D);
            fx(MsgTribunalFx.GRAVITY, focus.x, focus.y, focus.z, 24D, 0, 0, 0, 60);
            playSound(EternitySounds.AMBIENT_RUMBLE, 10F, 0.3F);
        }
        if (focus != null && stateTicks < 60) {
            for (EntityPlayer player : world.playerEntities) {
                if (player.getDistance(focus.x, player.posY, focus.z) < 24D) {
                    player.motionY = Math.min(player.motionY, -0.8D);
                    player.motionX *= 0.5D;
                    player.motionZ *= 0.5D;
                    player.velocityChanged = true;
                    player.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 10, 4, false, false));
                    if (stateTicks % 10 == 0) {
                        judge(player, 0.04F);
                    }
                }
            }
        }
        if (stateTicks == 60 && focus != null) {
            fist(focus, 10D, 0.15F);
        }
        if (stateTicks >= 70) {
            idle(40);
        }
    }

    // ------------------------------------------------------------------ ascension III

    /** Erasure: a wall of light sweeps the whole length of the hall. There is one gap in it. */
    private void erasure() {
        int start = 20;
        int span = 90;
        double fromZ = StarChamber.THRONE_Z - 10D;
        double toZ = StarChamber.DAIS_Z + 12D;
        if (stateTicks == 1) {
            gapX = StarChamber.DAIS_X + (rand.nextDouble() - 0.5D) * 36D;
            fx(MsgTribunalFx.SWEEP, gapX, StarChamber.FLOOR_Y + 1D, fromZ, 0, 0, toZ, 0, span + start);
            playSound(EternitySounds.FINALE_DRUMS, 8F, 0.7F);
        }
        if (stateTicks > start && stateTicks <= start + span) {
            // Exactly where the client draws it: across the hall over the whole of its life.
            double wall = Math.min(toZ, fromZ + (toZ - fromZ) * stateTicks / (double) (span + start));
            for (EntityPlayer player : world.playerEntities) {
                if (Math.abs(player.posZ - wall) < 1.6D && Math.abs(player.posX - gapX) > 4.5D
                        && player.posY < StarChamber.FLOOR_Y + 23D && struck.add(player.getUniqueID())) {
                    judge(player, 0.4F);
                    player.motionZ += 2D;
                    player.motionY += 0.6D;
                    player.velocityChanged = true;
                }
            }
        }
        if (stateTicks >= start + span + 10) {
            idle(50);
        }
    }

    /** Multiversal Echoes: ghosts of him from other universes appear beside him and bring their fists down too. */
    private void echoes(@Nullable EntityPlayerMP target) {
        if (stateTicks == 1) {
            Vec3d at = target != null ? floor(target.posX, target.posZ) : floor(posX, posZ + 20D);
            Vec3d side = facing().crossProduct(new Vec3d(0, 1, 0)).normalize();
            for (int k = -1; k <= 1; k += 2) {
                Vec3d ghost = new Vec3d(posX, posY, posZ).add(side.scale(k * 24D));
                float yaw = (float) Math.toDegrees(Math.atan2(at.z - ghost.z, at.x - ghost.x)) - 90F;
                fx(MsgTribunalFx.ECHO, ghost.x, ghost.y, ghost.z, yaw, 0, 0, 0, 55);
            }
            Vec3d[] marks = {at, at.add(side.scale(9D)), at.subtract(side.scale(9D))};
            for (Vec3d mark : marks) {
                fx(MsgTribunalFx.SIGIL, mark.x, mark.y, mark.z, 7D, 0, 0, 0, 34);
                strikes.add(new Strike(Kind.FIST, mark, ticksExisted + 34));
            }
            playSound(EternitySounds.ENTITY_VOICE, 10F, 0.3F);
        }
        if (stateTicks >= 75) {
            idle(55);
        }
    }

    // ------------------------------------------------------------------ the blows themselves

    /** A fist, a pan, a burst: everything near a point is struck and thrown into the air. */
    private void fist(Vec3d at, double radius, float fraction) {
        fx(MsgTribunalFx.GAVEL, at.x, at.y, at.z, radius * 1.5D, 0, 0, 0, 30);
        world.playSound(null, at.x, at.y, at.z, EternitySounds.COMET_IMPACT, SoundCategory.HOSTILE, 12F, 0.5F);
        world.playSound(null, at.x, at.y, at.z, SoundEvents.ENTITY_LIGHTNING_THUNDER, SoundCategory.HOSTILE, 10F, 0.5F);
        for (EntityPlayer player : world.playerEntities) {
            double d = player.getDistance(at.x, at.y, at.z);
            if (d < radius) {
                judge(player, fraction * (float) (1D - d / (radius * 1.4D)));
                Vec3d push = player.getPositionVector().subtract(at);
                push = push.lengthSquared() < 1.0E-4 ? Vec3d.ZERO : push.normalize();
                player.motionX += push.x * 2D;
                player.motionY += 1.2D;
                player.motionZ += push.z * 2D;
                player.velocityChanged = true;
            }
        }
    }

    private void tickStrikes() {
        Iterator<Strike> it = strikes.iterator();
        while (it.hasNext()) {
            Strike strike = it.next();
            if (ticksExisted < strike.when) {
                continue;
            }
            it.remove();
            switch (strike.kind) {
                case BOLT: {
                    Vec3d from = heart();
                    fx(MsgTribunalFx.BOLT, from.x, from.y, from.z, strike.at.x, strike.at.y, strike.at.z, rand.nextInt(), 10);
                    world.playSound(null, strike.at.x, strike.at.y, strike.at.z, SoundEvents.ENTITY_LIGHTNING_THUNDER, SoundCategory.HOSTILE, 6F,
                            0.8F + rand.nextFloat() * 0.4F);
                    hurtNear(strike.at, 3.2D, 0.12F, 0D);
                    break;
                }
                case PILLAR:
                    fx(MsgTribunalFx.PILLAR, strike.at.x, strike.at.y, strike.at.z, 34D, 0, 0, 0, 16);
                    world.playSound(null, strike.at.x, strike.at.y, strike.at.z, EternitySounds.RIFT_IMPACT, SoundCategory.HOSTILE, 5F, 1.2F);
                    hurtNear(strike.at, 2.6D, 0.15F, 1.4D);
                    break;
                case FIST:
                    fist(strike.at, 7D, 0.3F);
                    break;
            }
        }
    }

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

    /** Takes a share of their max health, more for each ascension. Creative and spectating players are beneath it. */
    private void judge(EntityPlayer player, float fraction) {
        if (player.isCreative() || player.isSpectator() || fraction <= 0F) {
            return;
        }
        player.hurtResistantTime = 0;
        player.attackEntityFrom(EternityDamage.tribunal(this), player.getMaxHealth() * fraction * might());
    }

    private static Vec3d floor(double x, double z) {
        return new Vec3d(x, StarChamber.FLOOR_Y + 1D, z);
    }

    private static double distanceToSegment(Vec3d p, Vec3d a, Vec3d b) {
        Vec3d ab = b.subtract(a);
        double len = ab.lengthSquared();
        double t = len < 1.0E-6 ? 0D : MathHelper.clamp(p.subtract(a).dotProduct(ab) / len, 0D, 1D);
        return p.distanceTo(a.add(ab.scale(t)));
    }

    private void fx(byte type, double x, double y, double z, double a, double b, double c, int id, int time) {
        EternityNetwork.sendToDimension(new MsgTribunalFx(type, x, y, z, a, b, c, id, time), dimension);
    }

    // ------------------------------------------------------------------ beaten, risen, absorbed

    private void fallToKnees() {
        dataManager.set(INTEGRITY, 0F);
        strikes.clear();
        setState(State.DOWNED);
        fx(MsgTribunalFx.FALL, posX, posY, posZ, 0, 0, 0, 0, 0);
        playSound(EternitySounds.ENTITY_VOICE, 12F, 0.3F);
        playSound(EternitySounds.COMET_IMPACT, 12F, 0.4F);
    }

    /** He gets back up, whole again, and more of what he is wakes in him. */
    private void rise(@Nullable EntityPlayerMP target) {
        int ascension = Math.min(MAX_ASCENSION, getAscension() + 1);
        dataManager.set(ASCENSION, (byte) ascension);
        setState(State.RISING);
        fx(MsgTribunalFx.RISE, posX, posY, posZ, 0, 0, 0, ascension, RISE_TICKS);
        playSound(EternitySounds.RIFT_OPEN, 10F, 0.5F);
        playSound(SoundEvents.ENTITY_ENDERDRAGON_GROWL, 8F, 0.4F);
        if (target != null) {
            TextComponentString notice = new TextComponentString("The Living Tribunal rises again. Ascension " + roman(ascension) + ".");
            notice.getStyle().setColor(TextFormatting.RED);
            target.sendStatusMessage(notice, true);
        }
    }

    /** A charged Regulator is raised while he lies beaten. He is drawn into it. */
    public boolean absorbBy(EntityPlayerMP player, EnumHand hand) {
        if (getState() != State.DOWNED) {
            return false;
        }
        absorber = player;
        absorbHand = hand;
        setState(State.ABSORBING);
        playSound(EternitySounds.RIFT_OPEN, 12F, 0.4F);
        playSound(EternitySounds.FINALE_DRUMS, 6F, 0.8F);
        return true;
    }

    private void absorbing() {
        if (absorber == null || absorber.isDead || absorber.world != world) {
            // Whoever held the Regulator is gone; he is beaten still, for a moment.
            setState(State.DOWNED);
            return;
        }
        if (stateTicks % 4 == 0) {
            Vec3d from = heart();
            fx(MsgTribunalFx.ABSORB, from.x, from.y, from.z, 0, 0, 0, absorber.getEntityId(), 8);
        }
        if (stateTicks % 30 == 0) {
            playSound(EternitySounds.RIFT_IMPACT, 10F, 0.5F + stateTicks / (float) ABSORB_TICKS);
        }
        if (stateTicks >= ABSORB_TICKS) {
            playSound(EternitySounds.COMET_BLAST, 14F, 0.5F);
            TribunalDirector.INSTANCE.absorbed(absorber, absorbHand);
            setDead();
        }
    }

    // ------------------------------------------------------------------ damage

    /**
     * Only blows struck from close to his body count. Without a charged Astral Regulator
     * in hand they barely mark him, and none takes more than a sliver of what he is.
     */
    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (world.isRemote || isEntityInvulnerable(source)) {
            return false;
        }
        State state = getState();
        if (state == State.DOWNED || state == State.RISING || state == State.ABSORBING || state == State.EMERGE) {
            return false;
        }
        Entity attacker = source.getTrueSource();
        if (!(attacker instanceof EntityPlayer) || distanceToBody(attacker.getPositionEyes(1F), getEntityBoundingBox()) > STRIKE_REACH + 2D) {
            return false;
        }
        double max = EternityConfig.tribunal.health;
        float scale = ItemAstralRegulator.holdsCharged(attacker) ? 1F : 0.1F;
        float dealt = (float) Math.min(amount * scale, max * 0.025D);
        if (!(dealt > 0F)) {
            return false;
        }
        float left = getIntegrity() - dealt / (float) max;
        playSound(EternitySounds.COMET_IMPACT, 3F, 1.3F + rand.nextFloat() * 0.3F);
        if (left <= 0F) {
            fallToKnees();
        } else {
            dataManager.set(INTEGRITY, left);
        }
        return true;
    }

    @Override
    public void onKillCommand() {
        // Nothing kills him.
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
        return getState() != State.ABSORBING;
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

    /** Never written to chunks; the court summons him anew each time. */
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
        return getEntityBoundingBox().grow(width * 2D, height, width * 2D);
    }
}
