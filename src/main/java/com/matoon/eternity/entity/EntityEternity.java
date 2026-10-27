package com.matoon.eternity.entity;

import com.matoon.eternity.client.render.EternityPose;
import com.matoon.eternity.EternityConfig;
import com.matoon.eternity.EternitySounds;
import com.matoon.eternity.compat.Galacticraft;
import com.matoon.eternity.compat.InfinityStones;
import com.matoon.eternity.network.MsgCue;
import com.matoon.eternity.world.Director;
import com.matoon.eternity.world.Singularity;
import com.matoon.eternity.world.Terrain;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
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
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.BossInfo;
import net.minecraft.world.BossInfoServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The body of Eternity. It is never saved with the chunk: the {@link Director} conjures
 * it when a player comes close and dissolves it again when they leave.
 */
public class EntityEternity extends EntityLiving {

    public static final float BASE_WIDTH = 1.8F;
    public static final float BASE_HEIGHT = 7.2F;
    /** His health. Whoever holds the Regulator he is caged in gains as much. */
    public static final double MAX_HEALTH = 1024D;

    private static final DataParameter<Float> SCALE = EntityDataManager.createKey(EntityEternity.class, DataSerializers.FLOAT);
    private static final DataParameter<Byte> STATE = EntityDataManager.createKey(EntityEternity.class, DataSerializers.BYTE);
    private static final DataParameter<Integer> TARGET = EntityDataManager.createKey(EntityEternity.class, DataSerializers.VARINT);
    private static final DataParameter<Float> CONTAINMENT = EntityDataManager.createKey(EntityEternity.class, DataSerializers.FLOAT);
    private static final DataParameter<Byte> ATTACK = EntityDataManager.createKey(EntityEternity.class, DataSerializers.BYTE);

    public enum State { DRIFT, EMERGE, STARE, HUNT, CHANNEL, DEPART, DYING }

    /** His attacks, and how long each is gathered before it is loosed. */
    public enum Attack {
        UNMAKE(14), SINGULARITY(45), LANCES(26), STARFALL(30), SHATTER(30);

        public final int ticks;

        Attack(int ticks) {
            this.ticks = ticks;
        }
    }

    private enum StrikeType { LANCE, STAR, ERUPT }

    private static final int STARE_TICKS = 90;

    private final BossInfoServer bossInfo = (BossInfoServer) new BossInfoServer(
            new TextComponentString("Eternity"), BossInfo.Color.PURPLE, BossInfo.Overlay.PROGRESS)
            .setDarkenSky(true).setCreateFog(true);

    private int stateTicks;
    private int attackCooldown = 40;
    private int blinkCooldown = 100;
    private int singularityCooldown = 300;
    private int lanceCooldown = 120;
    private int starfallCooldown = 200;
    private int shatterCooldown = 160;
    private int gazeTicks;
    private Attack channelling;
    private int channelLength;
    private boolean enraged;
    private final List<Strike> strikes = new ArrayList<>();

    /** Client: ticks since the synced state last changed. */
    public int clientStateTicks;
    /** Client: the pose he was last drawn in, and the pose he was in when his state last changed, to blend out of. */
    public final float[] poseLast = new float[EternityPose.N], poseFrom = new float[EternityPose.N];
    /** Client: the air against him as he moves, in his own frame, smoothed; it streams his cloak. */
    public final double[] wind = new double[3], prevWind = new double[3];
    /** Client: how far each segment of his cloak swings back, its sideways sway, the shoulder panels, the crests. */
    public static final int CAPE = 5, PANEL = 3;
    public final float[] cape = new float[CAPE], prevCape = new float[CAPE];
    private final float[] capeSpin = new float[CAPE];
    public float capeSway, prevCapeSway;
    public final float[][] panel = new float[2][PANEL], prevPanel = new float[2][PANEL];
    private final float[][] panelSpin = new float[2][PANEL];
    public final float[] crest = new float[2], prevCrest = new float[2];
    private final float[] crestSpin = new float[2], lastArm = new float[2];
    private float lastLean;

    /** A blow that lands a moment after it is foreshadowed. */
    private static final class Strike {
        final StrikeType type;
        final Vec3d from;
        final Vec3d to;
        final int at;

        Strike(StrikeType type, Vec3d from, Vec3d to, int at) {
            this.type = type;
            this.from = from;
            this.to = to;
            this.at = at;
        }
    }

    public EntityEternity(World world) {
        super(world);
        setSize(BASE_WIDTH, BASE_HEIGHT);
        noClip = true;
        isImmuneToFire = true;
        experienceValue = 0;
        ignoreFrustumCheck = true;
        setNoGravity(true);
        enablePersistence();
        bossInfo.setVisible(false);
        if (!world.isRemote && Galacticraft.isSpace(world)) {
            dataManager.set(SCALE, (float) EternityConfig.roaming.spaceScale);
        }
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(SCALE, 1F);
        dataManager.register(STATE, (byte) State.DRIFT.ordinal());
        dataManager.register(TARGET, -1);
        dataManager.register(CONTAINMENT, 0F);
        dataManager.register(ATTACK, (byte) 0);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(MAX_HEALTH);
        getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(1D);
        getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(256D);
    }

    @Override
    public void notifyDataManagerChange(DataParameter<?> key) {
        super.notifyDataManagerChange(key);
        if (SCALE.equals(key)) {
            float s = getScale();
            setSize(BASE_WIDTH * s, BASE_HEIGHT * s);
        } else if (STATE.equals(key) && world.isRemote) {
            clientStateTicks = 0;
            System.arraycopy(poseLast, 0, poseFrom, 0, poseLast.length);
        }
    }

    // ------------------------------------------------------------------ accessors

    public float getScale() {
        return dataManager.get(SCALE);
    }

    public State getState() {
        int ordinal = dataManager.get(STATE);
        return ordinal >= 0 && ordinal < State.values().length ? State.values()[ordinal] : State.DRIFT;
    }

    /** What he is gathering, while he channels. */
    public Attack getAttack() {
        int ordinal = dataManager.get(ATTACK);
        return ordinal >= 0 && ordinal < Attack.values().length ? Attack.values()[ordinal] : Attack.UNMAKE;
    }

    public void setState(State state) {
        if (getState() != state) {
            dataManager.set(STATE, (byte) state.ordinal());
            stateTicks = 0;
        }
    }

    /** How much of his escaping energy the Astral Regulator has drawn back in, 0 to 1. */
    public float getContainment() {
        return dataManager.get(CONTAINMENT);
    }

    public void setContainment(float progress) {
        dataManager.set(CONTAINMENT, progress);
    }

    public boolean isDying() {
        return getState() == State.DYING;
    }

    @Nullable
    public Entity getWatched() {
        int id = dataManager.get(TARGET);
        return id < 0 ? null : world.getEntityByID(id);
    }

    /** How far his touch reaches. Grows with him. */
    public double reach() {
        return 3.5D * getScale() + 1.5D;
    }

    public Vec3d chest() {
        return new Vec3d(posX, posY + height * 0.68D, posZ);
    }

    private double sqrtScale() {
        return Math.sqrt(getScale());
    }

    // ------------------------------------------------------------------ director hooks

    /** The Director chose a target for him. */
    public void onHuntStart() {
        if (getState() != State.DYING && getState() != State.DEPART && getState() != State.EMERGE) {
            setState(State.STARE);
        }
    }

    public void depart() {
        if (!isDying()) {
            setState(State.DEPART);
            playSound(EternitySounds.ENTITY_VOICE, 6F, 0.6F);
        }
    }

    public void emerge() {
        setState(State.EMERGE);
    }

    public void beginDying() {
        setState(State.DYING);
        motionX = motionY = motionZ = 0;
        strikes.clear();
        bossInfo.setVisible(true);
        bossInfo.setPercent(0F);
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void onLivingUpdate() {
        noClip = true;
        if (!world.isRemote) {
            serverTick();
        } else {
            clientStateTicks++;
            windTick();
            spawnWisps();
        }
        super.onLivingUpdate();
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        // The body never lags behind the head; he turns as one piece.
        renderYawOffset = rotationYaw;
        prevRenderYawOffset = prevRotationYaw;
    }

    @Override
    public void travel(float strafe, float vertical, float forward) {
        move(MoverType.SELF, motionX, motionY, motionZ);
    }

    private void serverTick() {
        Director director = Director.INSTANCE;
        if (!director.claim(this)) {
            setDead();
            return;
        }
        stateTicks++;
        if (attackCooldown > 0) attackCooldown--;
        if (blinkCooldown > 0) blinkCooldown--;
        if (singularityCooldown > 0) singularityCooldown--;
        if (lanceCooldown > 0) lanceCooldown--;
        if (starfallCooldown > 0) starfallCooldown--;
        if (shatterCooldown > 0) shatterCooldown--;

        EntityPlayerMP target = director.targetIn(world);
        if (target != null && (target.isDead || target.isSpectator())) {
            target = null;
        }
        dataManager.set(TARGET, target == null ? -1 : target.getEntityId());

        float health = getHealth() / getMaxHealth();
        if (!enraged && health < 0.2F && !isDying()) {
            enraged = true;
            dataManager.set(SCALE, getScale() * 1.35F);
            playSound(EternitySounds.ENTITY_VOICE, 8F, 0.45F);
        }

        switch (getState()) {
            case EMERGE:
                motionX *= 0.8;
                motionZ *= 0.8;
                motionY = 0.03;
                if (target != null) face(target);
                if (stateTicks > 70) setState(target != null ? State.STARE : State.DRIFT);
                break;
            case DRIFT:
                drift();
                if (target != null) {
                    setState(State.STARE);
                } else if (stateTicks % 20 == 0) {
                    lookForWitnesses(director);
                }
                break;
            case STARE:
                hover();
                if (target == null) {
                    setState(State.DRIFT);
                    break;
                }
                face(target);
                if (stateTicks == 1) {
                    director.cue(this, new MsgCue(MsgCue.STARE, getEntityId(), posX, posY, posZ, STARE_TICKS, 0, 0));
                }
                if (stateTicks > STARE_TICKS) setState(State.HUNT);
                break;
            case HUNT:
                if (target == null) {
                    setState(State.DRIFT);
                    break;
                }
                hunt(target, health);
                break;
            case CHANNEL:
                channel(target, health);
                break;
            case DEPART:
                motionX *= 0.85;
                motionZ *= 0.85;
                motionY = 0.02;
                break;
            case DYING:
                motionX = motionY = motionZ = 0;
                rotationYaw += 0.4F;
                rotationYawHead = rotationYaw;
                break;
        }

        tickStrikes();
        if (ticksExisted % 10 == 0) updateBossBar(health);
        if (ticksExisted % 20 == 0) regenerate();
    }

    private void hover() {
        motionX *= 0.75;
        motionY *= 0.75;
        motionZ *= 0.75;
    }

    private void drift() {
        float heading = (ticksExisted * 0.004F) + getEntityId();
        double speed = 0.06D * sqrtScale();
        motionX = MathHelper.cos(heading) * speed;
        motionZ = MathHelper.sin(heading) * speed;
        motionY = MathHelper.sin(ticksExisted * 0.02F) * 0.02D;
        rotationYaw = (float) Math.toDegrees(Math.atan2(motionZ, motionX)) - 90F;
        rotationYawHead = rotationYaw + MathHelper.sin(ticksExisted * 0.013F) * 50F;
        rotationPitch = 10F;
    }

    private void face(Entity entity) {
        double dx = entity.posX - posX;
        double dz = entity.posZ - posZ;
        double dy = (entity.posY + entity.getEyeHeight()) - (posY + height * 0.9D);
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90F;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        rotationYaw = yaw;
        rotationYawHead = yaw;
        rotationPitch = pitch;
    }

    private void lookForWitnesses(Director director) {
        double range = 48D * sqrtScale();
        EntityPlayer nearest = world.getClosestPlayer(posX, posY + height / 2, posZ, range, false);
        if (nearest instanceof EntityPlayerMP && !nearest.isSpectator()
                && (nearest.getDistance(this) < 16D * getScale() || nearest.canEntityBeSeen(this))
                && rand.nextFloat() < 0.4F) {
            director.provoke((EntityPlayerMP) nearest);
        }
    }

    private static double phaseMultiplier(float health) {
        return health < 0.2F ? 0.5D : health < 0.5F ? 0.72D : 1D;
    }

    private void hunt(EntityPlayerMP target, float health) {
        double mul = phaseMultiplier(health);
        Vec3d eyes = target.getPositionEyes(1F);
        Vec3d me = new Vec3d(posX, posY, posZ);
        Vec3d away = new Vec3d(posX - target.posX, 0, posZ - target.posZ);
        away = away.lengthSquared() < 1.0E-4 ? new Vec3d(1, 0, 0) : away.normalize();
        // Loom over the target with them at chest height.
        Vec3d desired = new Vec3d(target.posX, target.posY - height * 0.55D + 1D, target.posZ).add(away.scale(reach() * 0.6D));
        Vec3d delta = desired.subtract(me);
        double len = delta.length();
        double maxSpeed = (0.45D + (1D - mul) * 0.6D) * sqrtScale();
        double speed = Math.min(len * 0.08D, maxSpeed);
        Vec3d want = len < 1.0E-3 ? Vec3d.ZERO : delta.scale(speed / len);
        motionX += (want.x - motionX) * 0.15D;
        motionY += (want.y - motionY) * 0.15D;
        motionZ += (want.z - motionZ) * 0.15D;
        face(target);
        gaze(target);

        double dist = chest().distanceTo(eyes);
        if (dist > 60D * sqrtScale() && blinkCooldown <= 0) {
            blink(target, mul);
            return;
        }
        if (attackCooldown > 0) {
            return;
        }
        if (dist <= reach()) {
            startChannel(Attack.UNMAKE);
            return;
        }
        // Weighted choice among whatever is ready.
        double s = sqrtScale();
        float singularity = singularityCooldown <= 0 && dist > 8D && dist < 70D * s && !Director.INSTANCE.hasSingularityNear(world, posX, posZ) ? 3F : 0F;
        float starfall = starfallCooldown <= 0 && dist < 90D * s ? 2F : 0F;
        float shatter = shatterCooldown <= 0 && dist < 36D * s ? 2F : 0F;
        float lances = lanceCooldown <= 0 && dist > 10D ? 2F : 0F;
        float total = singularity + starfall + shatter + lances;
        if (total <= 0F) {
            return;
        }
        float roll = rand.nextFloat() * total;
        if ((roll -= singularity) < 0F) {
            startChannel(Attack.SINGULARITY);
        } else if ((roll -= starfall) < 0F) {
            startChannel(Attack.STARFALL);
        } else if ((roll -= shatter) < 0F) {
            startChannel(Attack.SHATTER);
        } else {
            startChannel(Attack.LANCES);
        }
    }

    private void startChannel(Attack attack) {
        channelling = attack;
        channelLength = attack.ticks;
        dataManager.set(ATTACK, (byte) attack.ordinal());
        setState(State.CHANNEL);
        playSound(EternitySounds.ENTITY_GLITCH, 4F, 0.5F + rand.nextFloat() * 0.3F);
        if (attack == Attack.SINGULARITY || attack == Attack.SHATTER) {
            playSound(EternitySounds.ENTITY_VOICE, 7F, 0.5F);
        }
    }

    private void channel(@Nullable EntityPlayerMP target, float health) {
        double mul = phaseMultiplier(health);
        motionX *= 0.8;
        motionY *= 0.8;
        motionZ *= 0.8;
        if (target != null) face(target);
        if (stateTicks < channelLength) {
            return;
        }
        Director director = Director.INSTANCE;
        switch (channelling) {
            case UNMAKE:
                if (target != null && chest().distanceTo(target.getPositionEyes(1F)) <= reach() * 1.35D) {
                    unmake(target);
                }
                attackCooldown = (int) (34 * mul);
                break;
            case SINGULARITY:
                if (target != null && world instanceof WorldServer) {
                    Vec3d centre = target.getPositionVector().add(0, 2.5D, 0);
                    double radius = EternityConfig.combat.singularityRadius * (getScale() > 1F ? getScale() * 0.5D : 1D) * (enraged ? 1.3D : 1D);
                    director.addSingularity(new Singularity((WorldServer) world, centre, radius, this));
                    director.cue(this, new MsgCue(MsgCue.SINGULARITY, getEntityId(), centre.x, centre.y, centre.z, radius, 0, 0));
                }
                singularityCooldown = (int) (1100 * mul);
                attackCooldown = (int) (60 * mul);
                break;
            case LANCES:
                if (target != null) {
                    int count = 4 + (health < 0.5F ? 3 : 0) + (health < 0.2F ? 4 : 0);
                    Vec3d lead = new Vec3d(target.motionX, 0, target.motionZ).scale(8D);
                    for (int i = 0; i < count; i++) {
                        double angle = rand.nextDouble() * Math.PI * 2D;
                        double r = 10D + rand.nextDouble() * 10D;
                        Vec3d from = new Vec3d(target.posX + Math.cos(angle) * r, target.posY + 6D + rand.nextDouble() * 12D, target.posZ + Math.sin(angle) * r);
                        Vec3d aim = target.getPositionEyes(1F).add(lead).add((rand.nextDouble() - 0.5D) * 2D, -0.6D, (rand.nextDouble() - 0.5D) * 2D);
                        Vec3d to = from.add(aim.subtract(from).scale(2.6D));
                        strike(StrikeType.LANCE, from, to, 30 + i * 4);
                        director.cue(this, new MsgCue(MsgCue.LANCE_WARN, getEntityId(), from.x, from.y, from.z, to.x, to.y, to.z));
                    }
                }
                lanceCooldown = (int) (180 * mul);
                attackCooldown = (int) (30 * mul);
                break;
            case STARFALL:
                if (target != null) {
                    int count = 12 + (health < 0.5F ? 6 : 0) + (health < 0.2F ? 8 : 0);
                    for (int i = 0; i < count; i++) {
                        double spread = i < 3 ? 2D : 22D * sqrtScale();
                        double gx = target.posX + (rand.nextDouble() - 0.5D) * 2D * spread;
                        double gz = target.posZ + (rand.nextDouble() - 0.5D) * 2D * spread;
                        Vec3d ground = new Vec3d(gx, groundY(gx, target.posY, gz), gz);
                        Vec3d sky = ground.add((rand.nextDouble() - 0.5D) * 30D, 110D, (rand.nextDouble() - 0.5D) * 30D);
                        int delay = 30 + i * 4 + rand.nextInt(6);
                        strike(StrikeType.STAR, sky, ground, delay);
                        director.cue(this, new MsgCue(MsgCue.STAR_WARN, getEntityId(), ground.x, ground.y, ground.z, sky.x, sky.y, sky.z));
                    }
                }
                starfallCooldown = (int) (320 * mul);
                attackCooldown = (int) (50 * mul);
                break;
            case SHATTER:
                if (target != null) {
                    // Rings of eruptions racing outward from beneath the target.
                    Vec3d origin = target.getPositionVector();
                    int rings = 4 + (enraged ? 2 : 0);
                    for (int ring = 0; ring < rings; ring++) {
                        double radius = (3D + ring * 6D) * sqrtScale();
                        int points = 6 + ring * 3;
                        for (int p = 0; p < points; p++) {
                            double angle = Math.PI * 2D * p / points + ring * 0.4D;
                            double gx = origin.x + Math.cos(angle) * radius;
                            double gz = origin.z + Math.sin(angle) * radius;
                            Vec3d ground = new Vec3d(gx, groundY(gx, origin.y, gz), gz);
                            strike(StrikeType.ERUPT, ground, ground.add(0, 30D, 0), 10 + ring * 7);
                        }
                    }
                }
                shatterCooldown = (int) (280 * mul);
                attackCooldown = (int) (50 * mul);
                break;
        }
        channelling = null;
        setState(State.HUNT);
    }

    private void strike(StrikeType type, Vec3d from, Vec3d to, int delay) {
        strikes.add(new Strike(type, from, to, ticksExisted + delay));
    }

    private void tickStrikes() {
        Iterator<Strike> it = strikes.iterator();
        List<Strike> due = new ArrayList<>();
        while (it.hasNext()) {
            Strike strike = it.next();
            if (ticksExisted >= strike.at) {
                due.add(strike);
                it.remove();
            }
        }
        Director director = Director.INSTANCE;
        for (Strike strike : due) {
            switch (strike.type) {
                case LANCE: {
                    director.cue(this, new MsgCue(MsgCue.LANCE_FIRE, getEntityId(), strike.from.x, strike.from.y, strike.from.z, strike.to.x, strike.to.y, strike.to.z));
                    world.playSound(null, strike.from.x, strike.from.y, strike.from.z, EternitySounds.RIFT_CLOSE, SoundCategory.HOSTILE, 5F, 1.3F + rand.nextFloat() * 0.3F);
                    AxisAlignedBB sweep = new AxisAlignedBB(strike.from.x, strike.from.y, strike.from.z, strike.to.x, strike.to.y, strike.to.z).grow(1.5D);
                    for (EntityLivingBase victim : world.getEntitiesWithinAABB(EntityLivingBase.class, sweep, this::isVictim)) {
                        if (victim.getEntityBoundingBox().grow(1.1D).calculateIntercept(strike.from, strike.to) != null) {
                            victim.attackEntityFrom(EternityDamage.lance(this), victim.getMaxHealth() * (worthy(victim) ? 0.22F : 0.8F));
                        }
                    }
                    Terrain.carveLine(world, strike.from, strike.to, 1.6D, 500);
                    break;
                }
                case STAR: {
                    director.cue(this, new MsgCue(MsgCue.LANCE_FIRE, getEntityId(), strike.from.x, strike.from.y, strike.from.z, strike.to.x, strike.to.y, strike.to.z));
                    world.newExplosion(this, strike.to.x, strike.to.y, strike.to.z, 4.5F * (float) sqrtScale(), false, Terrain.enabled());
                    hurtAround(strike.to, 5D * sqrtScale(), EternityDamage.starfall(this), 0.3F, 0.85F, 1.2D);
                    break;
                }
                case ERUPT: {
                    director.cue(this, new MsgCue(MsgCue.ERUPT, getEntityId(), strike.from.x, strike.from.y, strike.from.z, strike.to.x, strike.to.y, strike.to.z));
                    world.newExplosion(this, strike.from.x, strike.from.y, strike.from.z, 3.5F * (float) sqrtScale(), false, Terrain.enabled());
                    hurtAround(strike.from, 4D * sqrtScale(), EternityDamage.shatter(this), 0.2F, 0.6F, 1.8D);
                    break;
                }
            }
        }
    }

    /** Damages and hurls everything near a point. */
    private void hurtAround(Vec3d at, double radius, DamageSource source, float worthyFraction, float unworthyFraction, double lift) {
        AxisAlignedBB box = new AxisAlignedBB(at.x - radius, at.y - radius, at.z - radius, at.x + radius, at.y + radius, at.z + radius);
        for (EntityLivingBase victim : world.getEntitiesWithinAABB(EntityLivingBase.class, box, this::isVictim)) {
            double d = victim.getPositionVector().distanceTo(at);
            if (d > radius) {
                continue;
            }
            victim.attackEntityFrom(source, victim.getMaxHealth() * (worthy(victim) ? worthyFraction : unworthyFraction));
            Vec3d push = victim.getPositionVector().subtract(at);
            push = push.lengthSquared() < 1.0E-4 ? Vec3d.ZERO : push.normalize().scale(1.2D);
            victim.motionX += push.x;
            victim.motionY += lift;
            victim.motionZ += push.z;
            victim.velocityChanged = true;
        }
    }

    /** The highest solid surface at or below a little above {@code nearY}. */
    private double groundY(double x, double nearY, double z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int start = (int) Math.min(255, nearY + 12);
        for (int y = start; y > Math.max(0, start - 60); y--) {
            pos.setPos(x, y, z);
            if (world.isBlockLoaded(pos) && world.getBlockState(pos).getMaterial().blocksMovement()) {
                return y + 1;
            }
        }
        return nearY;
    }

    private void gaze(EntityPlayerMP target) {
        if (stateTicks % 5 != 0) {
            return;
        }
        Vec3d look = target.getLook(1F);
        Vec3d toMe = chest().subtract(target.getPositionEyes(1F)).normalize();
        boolean meeting = look.dotProduct(toMe) > 0.975D && target.canEntityBeSeen(this);
        gazeTicks = meeting ? gazeTicks + 5 : Math.max(0, gazeTicks - 5);
        if (gazeTicks >= 50) {
            gazeTicks = 0;
            target.addPotionEffect(new PotionEffect(MobEffects.BLINDNESS, 40, 0, false, false));
            target.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 60, 1, false, false));
            target.attackEntityFrom(EternityDamage.gaze(this), Math.max(2F, target.getMaxHealth() * 0.1F));
            playSound(EternitySounds.ENTITY_VOICE, 5F, 0.7F);
        }
    }

    private void blink(EntityPlayerMP target, double mul) {
        Vec3d from = getPositionVector();
        Vec3d back = new Vec3d(target.getLookVec().x, 0, target.getLookVec().z);
        back = back.lengthSquared() < 1.0E-4 ? new Vec3d(0, 0, 1) : back.normalize();
        double distance = 10D + reach();
        Vec3d to = new Vec3d(target.posX, target.posY - height * 0.4D, target.posZ).subtract(back.scale(distance));
        setPosition(to.x, to.y, to.z);
        motionX = motionY = motionZ = 0;
        face(target);
        blinkCooldown = (int) (170 * mul);
        Director.INSTANCE.cue(this, new MsgCue(MsgCue.BLINK, getEntityId(), from.x, from.y + height / 2, from.z, to.x, to.y + height / 2, to.z));
        world.playSound(null, from.x, from.y, from.z, EternitySounds.ENTITY_GLITCH, SoundCategory.HOSTILE, 4F, 0.6F);
        world.playSound(null, to.x, to.y, to.z, EternitySounds.ENTITY_GLITCH, SoundCategory.HOSTILE, 4F, 0.5F);
    }

    /** His touch. Anyone near the one he reaches for is thrown aside by the blow. */
    private void unmake(EntityLivingBase victim) {
        boolean worthy = worthy(victim);
        double fraction = worthy ? EternityConfig.combat.unmakeFraction : EternityConfig.combat.unmakeFractionUnworthy;
        victim.attackEntityFrom(EternityDamage.unmake(this), (float) (victim.getMaxHealth() * fraction));
        Vec3d at = victim.getPositionVector();
        Vec3d push = at.subtract(getPositionVector());
        push = new Vec3d(push.x, 0, push.z).normalize().scale(2.2D);
        victim.motionX += push.x;
        victim.motionY += 0.8D;
        victim.motionZ += push.z;
        victim.velocityChanged = true;
        hurtAround(at, 6D * sqrtScale(), EternityDamage.unmake(this), 0.1F, 0.4F, 0.9D);
        Director.INSTANCE.cue(this, new MsgCue(MsgCue.UNMAKE, getEntityId(), victim.posX, victim.posY + victim.height / 2, victim.posZ, 0, 0, 0));
        world.playSound(null, victim.posX, victim.posY, victim.posZ, EternitySounds.RIFT_IMPACT, SoundCategory.HOSTILE, 5F, 1.1F);
    }

    /** Creative mode does not save you. Only spectators are beneath his notice. */
    private boolean isVictim(@Nullable EntityLivingBase entity) {
        if (entity == null || entity == this || !entity.isEntityAlive()) {
            return false;
        }
        return !(entity instanceof EntityPlayer) || !((EntityPlayer) entity).isSpectator();
    }

    private static boolean worthy(Entity entity) {
        return InfinityStones.scan(entity) == InfinityStones.ALL;
    }

    private void updateBossBar(float health) {
        State state = getState();
        bossInfo.setVisible(state == State.STARE || state == State.HUNT || state == State.CHANNEL || state == State.DYING);
        bossInfo.setPercent(isDying() ? 0F : health);
        bossInfo.setName(new TextComponentString(rand.nextFloat() < 0.08F ? "§kEternity" : "Eternity"));
    }

    private void regenerate() {
        if (isDying() || getHealth() >= getMaxHealth()) {
            return;
        }
        for (EntityPlayer player : world.playerEntities) {
            if (player.getDistance(this) < 96D * sqrtScale() && InfinityStones.countHeld(player) >= 5) {
                return;
            }
        }
        heal(getMaxHealth() * (float) EternityConfig.combat.regenPerSecond);
    }

    // ------------------------------------------------------------------ damage

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (world.isRemote || isDying() || isEntityInvulnerable(source)) {
            return false;
        }
        if (source == DamageSource.OUT_OF_WORLD || source == DamageSource.IN_WALL || source == DamageSource.DROWN
                || source == DamageSource.FALL || source == DamageSource.CRAMMING || source.isFireDamage()
                || source.isExplosion() && source.getTrueSource() == null) {
            return false;
        }
        Entity attacker = source.getTrueSource();
        int stones = InfinityStones.countHeld(attacker);
        double[] table = EternityConfig.combat.stoneDamageScale;
        double scale = table.length == 0 ? 0D : table[Math.min(stones, table.length - 1)];
        if (attacker instanceof EntityPlayerMP) {
            Director.INSTANCE.provoke((EntityPlayerMP) attacker);
        }
        if (scale <= 0D) {
            Vec3d at = source.getDamageLocation() != null ? source.getDamageLocation() : chest();
            Director.INSTANCE.cue(this, new MsgCue(MsgCue.PHASE, getEntityId(), at.x, at.y, at.z, 0, 0, 0));
            world.playSound(null, posX, posY + height / 2, posZ, EternitySounds.ENTITY_GLITCH, SoundCategory.HOSTILE, 2F, 1.6F);
            return false;
        }
        float damage = (float) Math.min(amount * scale, getMaxHealth() * EternityConfig.combat.maxHitFraction);
        if (attacker instanceof EntityPlayerMP && damage > 0F) {
            Director.INSTANCE.recordParticipant((EntityPlayerMP) attacker);
        }
        return super.attackEntityFrom(source, damage);
    }

    /** Death is intercepted: the finale takes over, and only the finale removes him. */
    @Override
    public void onDeath(DamageSource cause) {
        if (world.isRemote || isDying()) {
            return;
        }
        setHealth(1F);
        Director.INSTANCE.beginFinale(this, cause);
    }

    @Override
    public void onKillCommand() {
        // /kill dissolves him quietly; he is not slain.
        Director.INSTANCE.vanish();
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
        return !isDying();
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
    public boolean canRenderOnFire() {
        return false;
    }

    @Override
    protected boolean canDespawn() {
        return false;
    }

    @Override
    public boolean isPushedByWater() {
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

    /** Never written to chunks; {@link com.matoon.eternity.world.EternityData} remembers him. */
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
    protected SoundEvent getAmbientSound() {
        return EternitySounds.ENTITY_VOICE;
    }

    @Override
    public int getTalkInterval() {
        return 360;
    }

    @Override
    protected float getSoundVolume() {
        return 5F;
    }

    @Override
    protected float getSoundPitch() {
        return 0.55F + rand.nextFloat() * 0.25F;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return EternitySounds.ENTITY_GLITCH;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_LIGHTNING_THUNDER;
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

    // ------------------------------------------------------------------ client

    /**
     * The air against him, turned into his own frame (the model's: x to his left, y down, z
     * behind him), and eased so his cloak swings rather than snaps when he turns or stops.
     */
    @SideOnly(Side.CLIENT)
    private void windTick() {
        System.arraycopy(wind, 0, prevWind, 0, 3);
        double k = 2D / Math.max(1D, Math.sqrt(getScale()));
        double wx = -(posX - prevPosX) * k, wy = -(posY - prevPosY) * k, wz = -(posZ - prevPosZ) * k;
        double theta = Math.toRadians(180F - renderYawOffset);
        double lx = wx * Math.cos(theta) - wz * Math.sin(theta);
        double lz = wx * Math.sin(theta) + wz * Math.cos(theta);
        double[] goal = {-lx, -wy, lz};
        double length = Math.sqrt(goal[0] * goal[0] + goal[1] * goal[1] + goal[2] * goal[2]);
        for (int i = 0; i < 3; i++) {
            goal[i] = length > 1.4D ? goal[i] * 1.4D / length : goal[i];
            wind[i] += (goal[i] - wind[i]) * 0.12D;
        }
        cloakTick();
    }

    /**
     * His cloak, its shoulder panels and his crests, on springs. The cloak streams away from
     * wherever the air is coming from and floats when he gathers his power; each segment
     * swings toward the one above it, so a ripple runs down it. When his arms or body move,
     * the cloth is left behind for a moment and swings after them; when he turns, his crests
     * sway against the turn and settle.
     */
    @SideOnly(Side.CLIENT)
    private void cloakTick() {
        float t = ticksExisted;
        float lift = poseLast[EternityPose.CLOAK_LIFT], open = poseLast[EternityPose.CLOAK_OPEN];
        double back = wind[2] + 0.06D + lift * 1.1D, down = 1D + wind[1] * 0.9D;
        float theta = (float) MathHelper.clamp(Math.atan2(back, down), 0.03D, 1.5D);
        float strength = (float) Math.min(1D, Math.sqrt(wind[0] * wind[0] + wind[2] * wind[2]) + lift * 0.6D);

        // Whatever his body and arms just did, the cloth has yet to do.
        float lean = poseLast[EternityPose.LEAN] + poseLast[EternityPose.CHEST_X] + poseLast[EternityPose.WAIST_X];
        capeSpin[0] += (lean - lastLean) * 0.7F;
        lastLean = lean;

        prevCapeSway = capeSway;
        capeSway += ((float) MathHelper.clamp(-wind[0] * 0.8D, -0.5D, 0.5D) + MathHelper.sin(t * 0.043F) * 0.05F - capeSway) * 0.15F;
        for (int i = 0; i < CAPE; i++) {
            prevCape[i] = cape[i];
        }
        for (int i = 0; i < CAPE; i++) {
            float flutter = MathHelper.sin(t * (0.09F + 0.3F * strength) - i * 1.1F) * (0.04F + 0.18F * strength) * (0.5F + i * 0.35F)
                    + MathHelper.sin(t * 0.037F + i * 0.8F) * 0.03F;
            float goal = (i == 0 ? theta : prevCape[i - 1] * 0.72F + theta * 0.28F) + flutter + open * 0.05F * i;
            capeSpin[i] += (goal - cape[i]) * 0.22F;
            capeSpin[i] *= 0.7F;
            cape[i] += capeSpin[i];
            cape[i] = i == 0 ? Math.max(0.02F, cape[i]) : cape[i - 1] + MathHelper.clamp(cape[i] - cape[i - 1], -0.45F, 0.8F);
        }

        for (int side = 0; side < 2; side++) {
            int arm = side == 0 ? EternityPose.RA_X : EternityPose.LA_X;
            float armNow = poseLast[arm] + poseLast[arm + 2];
            panelSpin[side][0] += (armNow - lastArm[side]) * 0.45F;
            lastArm[side] = armNow;
            for (int i = 0; i < PANEL; i++) {
                prevPanel[side][i] = panel[side][i];
            }
            for (int i = 0; i < PANEL; i++) {
                float flutter = MathHelper.sin(t * (0.11F + 0.3F * strength) - i * 1.3F + side * 2.1F) * (0.04F + 0.15F * strength) * (0.6F + i * 0.4F);
                float goal = (i == 0 ? theta * 0.7F : prevPanel[side][i - 1] * 0.7F + theta * 0.2F) + flutter;
                panelSpin[side][i] += (goal - panel[side][i]) * 0.25F;
                panelSpin[side][i] *= 0.68F;
                panel[side][i] += panelSpin[side][i];
            }

            // The crests lag behind a turn, then swing back past and settle.
            float turn = MathHelper.wrapDegrees(renderYawOffset - prevRenderYawOffset);
            prevCrest[side] = crest[side];
            crestSpin[side] += (MathHelper.clamp(-turn * 0.04F, -0.6F, 0.6F) - crest[side]) * 0.18F;
            if (hurtTime == 9) {
                crestSpin[side] += side == 0 ? 0.25F : -0.25F;
            }
            crestSpin[side] *= 0.8F;
            crest[side] += crestSpin[side];
        }
    }

    private void spawnWisps() {
        float s = getScale();
        int count = 1 + (int) (s * 0.4F);
        for (int i = 0; i < count; i++) {
            double px = posX + (rand.nextDouble() - 0.5D) * width * 1.4D;
            double py = posY + rand.nextDouble() * height;
            double pz = posZ + (rand.nextDouble() - 0.5D) * width * 1.4D;
            world.spawnParticle(EnumParticleTypes.PORTAL, px, py, pz, (rand.nextDouble() - 0.5D) * 2D, -rand.nextDouble(), (rand.nextDouble() - 0.5D) * 2D);
        }
        if (rand.nextInt(3) == 0) {
            world.spawnParticle(EnumParticleTypes.END_ROD, posX + (rand.nextDouble() - 0.5D) * width * 2D,
                    posY + rand.nextDouble() * height, posZ + (rand.nextDouble() - 0.5D) * width * 2D, 0D, -0.01D, 0D);
        }
    }

    @SideOnly(Side.CLIENT)
    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return getEntityBoundingBox().grow(width * 2D, height, width * 2D);
    }
}
