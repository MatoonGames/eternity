package com.matoon.eternity.entity;

import com.matoon.eternity.EternityConfig;
import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.network.MsgTribunalFx;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemAxe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ITeleporter;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * One of the Einherjar, the guard of Asgard's royal palace, posted at the head of the steps into
 * Odin's Vault. Blackened steel and gold, a mustard cloak, a helm crowned with a ring of horns,
 * a round bronze shield and a spear taller than he is. He challenges whoever comes up the steps,
 * then comes at them behind his shield.
 * <ul>
 * <li><b>Thrust</b>: the spear drawn back past his hip, then driven in with his whole weight behind it.</li>
 * <li><b>Jabs</b>: two quick stabs from behind the shield.</li>
 * <li><b>Sweep</b>: the shaft swung flat across in front of him, sending you sideways.</li>
 * <li><b>Shield bash</b>: in close, the shield slammed into you.</li>
 * <li><b>Guard</b>: crouched behind his shield with the spear levelled over it. Strike the shield
 * and the spear comes straight back at you.</li>
 * </ul>
 * Blows from in front of him land on the shield whenever it is up, unless an axe knocks it aside.
 * Unlike the vault's other guardian, he is only mortal.
 */
public class EntityEinherjar extends EntityLiving {

    public enum State { POST, CHALLENGE, ADVANCE, THRUST, JAB, SWEEP, BASH, GUARD, STAGGER }

    /** What he can be told to do, for testing. */
    public enum Move { THRUST, JAB, SWEEP, BASH, GUARD }

    // How long everything takes, in ticks; the model times its frames to the same numbers.
    public static final int CHALLENGE_SLAM = 14;
    public static final int CHALLENGE_TICKS = 34;
    public static final int THRUST_HIT = 13;
    public static final int THRUST_TICKS = 26;
    public static final int JAB_FIRST = 6;
    public static final int JAB_SECOND = 13;
    public static final int JAB_TICKS = 22;
    public static final int SWEEP_HIT = 13;
    public static final int SWEEP_TICKS = 28;
    public static final int BASH_HIT = 8;
    public static final int BASH_TICKS = 20;
    public static final int GUARD_TICKS = 40;
    public static final int STAGGER_TICKS = 18;
    /** How long he takes to fall when he dies: to his knees, then down. */
    public static final int FALL_TICKS = 40;

    /** Segments in his cloak, top to bottom. */
    public static final int CAPE = 4;

    private static final DataParameter<Byte> STATE = EntityDataManager.createKey(EntityEinherjar.class, DataSerializers.BYTE);

    private static final String[] HALT = {"Halt! None may enter the vault of Odin!", "Stand where you are, thief!",
            "You have no leave to be here. Turn back, or fall."};
    private static final String[] FALLING = {"For... Asgard...", "Odin... forgive me...", "Valhalla..."};

    // ------------------------------------------------------------------ server state
    @Nullable
    private UUID targetId;
    private int stateTicks;
    private int cooldown = 20;
    /** Which side of whoever he fights he keeps to, so the two of them come at you from either side. */
    private int flank = 1;
    /** Pulls his blows: nothing he does harms anyone. */
    private boolean calm;
    @Nullable
    private Move pendingMove;
    private boolean riposte;
    private double postX, postY, postZ;
    private float postYaw;
    private boolean hasPost;

    // ------------------------------------------------------------------ client state
    /** Client: ticks since the synced state last changed. */
    public int clientStateTicks;
    /** Client: each cloak segment's angle back from his body, and how fast it swings. */
    public final float[] cape = new float[CAPE], prevCape = new float[CAPE], capeSpin = new float[CAPE];
    public float capeSway, prevCapeSway;
    /** Client: the pose he was in when his state last changed, which the model blends out of; and the pose last drawn. */
    public final float[] poseFrom = new float[40], poseLast = new float[40];
    private double velX, velZ;

    public EntityEinherjar(World world) {
        super(world);
        setSize(0.7F, 2.0F);
        stepHeight = 1.05F;
        experienceValue = 12;
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(STATE, (byte) State.POST.ordinal());
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(EternityConfig.vault.einherjarHealth);
        getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(0.6D);
        getEntityAttribute(SharedMonsterAttributes.ARMOR).setBaseValue(10D);
    }

    @Override
    public void notifyDataManagerChange(DataParameter<?> key) {
        super.notifyDataManagerChange(key);
        if (STATE.equals(key) && world.isRemote) {
            clientStateTicks = 0;
            System.arraycopy(poseLast, 0, poseFrom, 0, poseLast.length);
        }
    }

    // ------------------------------------------------------------------ accessors and handles

    public State getState() {
        int ordinal = dataManager.get(STATE);
        return ordinal >= 0 && ordinal < State.values().length ? State.values()[ordinal] : State.ADVANCE;
    }

    private void setState(State state) {
        dataManager.set(STATE, (byte) state.ordinal());
        stateTicks = 0;
    }

    /** Stands him at his post, facing down the steps, the way he is found. */
    public void post(double x, double y, double z, float yaw, int flank) {
        postX = x;
        postY = y;
        postZ = z;
        postYaw = yaw;
        hasPost = true;
        this.flank = flank;
        setLocationAndAngles(x, y, z, yaw, 0F);
        rotationYawHead = renderYawOffset = yaw;
        setState(State.POST);
    }

    public void setCalm(boolean calm) {
        this.calm = calm;
    }

    public boolean isCalm() {
        return calm;
    }

    public void order(Move move) {
        pendingMove = move;
        cooldown = 0;
    }

    /** Whether his shield is up in front of him. */
    public boolean isGuarding() {
        State state = getState();
        return state == State.ADVANCE || state == State.GUARD || state == State.CHALLENGE || state == State.POST;
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void onLivingUpdate() {
        if (!world.isRemote && getHealth() > 0F) {
            serverTick();
        }
        super.onLivingUpdate();
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (world.isRemote) {
            clientTick();
        }
    }

    /** His own footwork; and the stride his legs swing through, which vanilla would have worked out here. */
    @Override
    public void travel(float strafe, float vertical, float forward) {
        if (!world.isRemote) {
            move(MoverType.SELF, motionX, motionY, motionZ);
            motionY = onGround ? -0.08D : Math.max(-2D, motionY - 0.08D);
            motionX *= 0.6D;
            motionZ *= 0.6D;
        }
        stride(this);
    }

    /** The swing of the legs, from how far a body has moved since last tick. */
    static void stride(EntityLivingBase entity) {
        entity.prevLimbSwingAmount = entity.limbSwingAmount;
        double dx = entity.posX - entity.prevPosX, dz = entity.posZ - entity.prevPosZ;
        float moved = Math.min(1F, MathHelper.sqrt(dx * dx + dz * dz) * 4F);
        entity.limbSwingAmount += (moved - entity.limbSwingAmount) * 0.4F;
        entity.limbSwing += entity.limbSwingAmount;
    }

    private void serverTick() {
        stateTicks++;
        if (cooldown > 0) {
            cooldown--;
        }
        EntityPlayer target = target();
        switch (getState()) {
            case POST:
                standing(target);
                break;
            case CHALLENGE:
                challenging(target);
                break;
            case ADVANCE:
                advancing(target);
                break;
            case THRUST:
                thrusting(target);
                break;
            case JAB:
                jabbing(target);
                break;
            case SWEEP:
                sweeping(target);
                break;
            case BASH:
                bashing(target);
                break;
            case GUARD:
                guarding(target);
                break;
            case STAGGER:
                if (stateTicks >= STAGGER_TICKS) {
                    setState(State.ADVANCE);
                }
                break;
        }
    }

    /** Whoever he is fighting, while they are still here and standing; failing that, the nearest intruder. */
    @Nullable
    private EntityPlayer target() {
        if (targetId != null) {
            EntityPlayer player = world.getPlayerEntityByUUID(targetId);
            if (player != null && isFair(player) && player.getDistanceSq(this) < 48D * 48D) {
                return player;
            }
            targetId = null;
        }
        EntityPlayer best = null;
        double bestDistance = 40D * 40D;
        for (EntityPlayer player : world.playerEntities) {
            double d = player.getDistanceSq(this);
            if (isFair(player) && d < bestDistance) {
                best = player;
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

    // ------------------------------------------------------------------ at his post

    /** At attention, spear upright, until someone comes up the steps toward him. */
    private void standing(@Nullable EntityPlayer target) {
        if (hasPost) {
            rotationYaw += MathHelper.wrapDegrees(postYaw - rotationYaw) * 0.2F;
            rotationYawHead = rotationYaw;
            if (getDistanceSq(postX, postY, postZ) > 0.5D) {
                walkToward(new Vec3d(postX, postY, postZ), 0.16D);
            }
        }
        if (target != null && (target.getDistance(this) < 13D && canEntityBeSeen(target) || getHealth() < getMaxHealth())) {
            setState(State.CHALLENGE);
            if (flank > 0) {
                say(HALT[rand.nextInt(HALT.length)]);
            }
        }
    }

    /** He turns on you, lifts his spear and brings its butt down on the stone, and comes. */
    private void challenging(@Nullable EntityPlayer target) {
        if (target != null) {
            face(target, 0.3F);
        }
        if (stateTicks == CHALLENGE_SLAM) {
            sound(SoundEvents.BLOCK_ANVIL_LAND, 0.5F, 0.6F);
            sound(SoundEvents.ENTITY_IRONGOLEM_ATTACK, 1F, 0.7F);
        }
        if (stateTicks >= CHALLENGE_TICKS) {
            setState(State.ADVANCE);
            cooldown = 6 + rand.nextInt(10);
        }
    }

    // ------------------------------------------------------------------ fighting

    /** Behind his shield, closing in on your flank, then choosing his moment. */
    private void advancing(@Nullable EntityPlayer target) {
        if (target == null) {
            if (hasPost) {
                walkToward(new Vec3d(postX, postY, postZ), 0.2D);
                if (getDistanceSq(postX, postY, postZ) < 1D) {
                    setState(State.POST);
                }
            }
            return;
        }
        face(target, 0.35F);
        double d = horizontal(target);
        // Keep to his side of you, a spear's length out.
        Vec3d from = flat(getPositionVector().subtract(target.getPositionVector()));
        double angle = Math.toRadians(32D * flank);
        Vec3d slot = new Vec3d(from.x * Math.cos(angle) - from.z * Math.sin(angle), 0D, from.x * Math.sin(angle) + from.z * Math.cos(angle));
        Vec3d want = target.getPositionVector().add(slot.scale(2.7D));
        if (want.distanceTo(getPositionVector()) > 0.4D) {
            walkToward(want, d > 7D ? 0.27D : 0.19D);
        }
        if (cooldown > 0) {
            return;
        }
        if (pendingMove != null) {
            start(pendingMove);
            pendingMove = null;
            return;
        }
        if (d > 3.6D || Math.abs(target.posY - posY) > 2.5D) {
            return;
        }
        int roll = rand.nextInt(100);
        if (d < 2.2D && roll < 22) {
            start(Move.BASH);
        } else if (roll < 50) {
            start(Move.THRUST);
        } else if (roll < 72) {
            start(Move.JAB);
        } else if (roll < 88 && d < 3.2D) {
            start(Move.SWEEP);
        } else {
            start(Move.GUARD);
        }
    }

    private void start(Move move) {
        riposte = false;
        switch (move) {
            case THRUST:
                setState(State.THRUST);
                break;
            case JAB:
                setState(State.JAB);
                break;
            case SWEEP:
                setState(State.SWEEP);
                break;
            case BASH:
                setState(State.BASH);
                break;
            case GUARD:
                setState(State.GUARD);
                break;
        }
    }

    private void recover(int ticks) {
        setState(State.ADVANCE);
        cooldown = ticks + rand.nextInt(12);
    }

    /** The spear drawn back past his hip, then driven in with his weight behind it. */
    private void thrusting(@Nullable EntityPlayer target) {
        int t = stateTicks;
        if (target != null && t < THRUST_HIT - 2) {
            face(target, 0.4F);
        }
        if (t == THRUST_HIT - 3) {
            step(0.55D);
            sound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1F, 1.3F);
        }
        if (t == THRUST_HIT) {
            stab(target, 3.9D, 1F, 0.7D);
        }
        if (t >= THRUST_TICKS) {
            recover(riposte ? 6 : 16);
        }
    }

    /** Two quick stabs from behind the shield. */
    private void jabbing(@Nullable EntityPlayer target) {
        int t = stateTicks;
        if (target != null) {
            face(target, 0.5F);
        }
        if (t == JAB_FIRST - 2 || t == JAB_SECOND - 2) {
            sound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 0.7F, 1.6F);
        }
        if (t == JAB_FIRST || t == JAB_SECOND) {
            step(0.2D);
            stab(target, 3.4D, 0.55F, 0.3D);
        }
        if (t >= JAB_TICKS) {
            recover(12);
        }
    }

    /** The shaft swung flat across in front of him. */
    private void sweeping(@Nullable EntityPlayer target) {
        int t = stateTicks;
        if (target != null && t < SWEEP_HIT - 3) {
            face(target, 0.4F);
        }
        if (t == SWEEP_HIT - 3) {
            sound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1.2F, 0.8F);
        }
        if (t == SWEEP_HIT) {
            Vec3d f = facing();
            Vec3d across = new Vec3d(f.z, 0D, -f.x).scale(flank);
            for (EntityLivingBase victim : world.getEntitiesWithinAABB(EntityPlayer.class, getEntityBoundingBox().grow(3.5D, 1.5D, 3.5D))) {
                Vec3d to = victim.getPositionVector().subtract(getPositionVector());
                if (victim instanceof EntityPlayer && isFair((EntityPlayer) victim) && to.length() < 3.5D && flat(to).dotProduct(f) > -0.1D) {
                    land(victim, 0.75F, across.add(f.scale(0.4D)), 1.1D, 0.35D);
                }
            }
        }
        if (t >= SWEEP_TICKS) {
            recover(16);
        }
    }

    /** In close, the shield slammed into you. */
    private void bashing(@Nullable EntityPlayer target) {
        int t = stateTicks;
        if (target != null && t < BASH_HIT) {
            face(target, 0.5F);
        }
        if (t == BASH_HIT - 2) {
            step(0.6D);
        }
        if (t == BASH_HIT && target != null) {
            if (horizontal(target) < 2.3D && flat(target.getPositionVector().subtract(getPositionVector())).dotProduct(facing()) > 0.3D) {
                land(target, 0.45F, facing(), 1.4D, 0.4D);
                sound(SoundEvents.ITEM_SHIELD_BLOCK, 1.5F, 0.6F);
            }
        }
        if (t >= BASH_TICKS) {
            recover(14);
        }
    }

    /** Crouched behind the shield, the spear over it. Strike the shield and he answers at once. */
    private void guarding(@Nullable EntityPlayer target) {
        if (target != null) {
            face(target, 0.4F);
        }
        if (stateTicks >= GUARD_TICKS) {
            recover(4);
        }
    }

    /** One stab at whatever is in front of him and within reach. */
    private void stab(@Nullable EntityPlayer target, double reach, float weight, double knock) {
        if (target == null) {
            return;
        }
        Vec3d f = facing();
        Vec3d to = target.getPositionVector().subtract(getPositionVector());
        if (flat(to).dotProduct(f) > 0.55D && horizontal(target) < reach + target.width * 0.5D && Math.abs(target.posY - posY) < 2.2D) {
            land(target, weight, f, knock, 0.2D);
        }
    }

    private void land(EntityLivingBase victim, float weight, Vec3d drive, double knock, double lift) {
        Vec3d at = new Vec3d(victim.posX, victim.posY + victim.height * 0.6D, victim.posZ);
        EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.EINHERJAR_HIT, at.x, at.y, at.z, drive.x, drive.y, drive.z,
                victim.getEntityId(), 0), dimension);
        sound(SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, 1.2F, 0.9F);
        if (calm) {
            return;
        }
        victim.hurtResistantTime = 0;
        victim.attackEntityFrom(new EntityDamageSource("eternity.einherjar", this), (float) EternityConfig.vault.einherjarDamage * weight);
        victim.motionX += drive.x * knock;
        victim.motionY = Math.max(victim.motionY, 0D) + lift;
        victim.motionZ += drive.z * knock;
        victim.velocityChanged = true;
    }

    // ------------------------------------------------------------------ being struck

    /**
     * Blows from in front land on his shield whenever it is up, unless the axe they come from
     * knocks it aside, which leaves him reeling. Blocked in his guard, he answers with the spear.
     */
    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (world.isRemote || getHealth() <= 0F) {
            return false;
        }
        Entity attacker = source.getTrueSource();
        Entity direct = source.getImmediateSource();
        Entity from = direct != null ? direct : attacker;
        if (from != null && isGuarding() && !source.isUnblockable() && !source.isExplosion()) {
            Vec3d to = flat(from.getPositionVector().subtract(getPositionVector()));
            if (to.dotProduct(facing()) > 0.25D) {
                if (attacker instanceof EntityLivingBase && ((EntityLivingBase) attacker).getHeldItemMainhand().getItem() instanceof ItemAxe
                        && direct == attacker) {
                    sound(SoundEvents.ITEM_SHIELD_BREAK, 1F, 0.8F);
                    setState(State.STAGGER);
                    return false;
                }
                Vec3d at = getPositionVector().add(to.scale(0.6D)).add(0D, 1.2D, 0D);
                EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.EINHERJAR_BLOCK, at.x, at.y, at.z, to.x, 0D, to.z, getEntityId(), 0),
                        dimension);
                sound(SoundEvents.ITEM_SHIELD_BLOCK, 1F, 0.8F + rand.nextFloat() * 0.3F);
                if (attacker instanceof EntityPlayer) {
                    targetId = attacker.getUniqueID();
                }
                if (getState() == State.GUARD) {
                    riposte = true;
                    setState(State.THRUST);
                    stateTicks = THRUST_HIT - 4;
                } else if (getState() == State.POST) {
                    setState(State.CHALLENGE);
                    stateTicks = CHALLENGE_TICKS - 4;
                }
                return false;
            }
        }
        boolean hurt = super.attackEntityFrom(source, amount);
        if (hurt && attacker instanceof EntityPlayer) {
            targetId = attacker.getUniqueID();
            State state = getState();
            if (amount >= 6F && state != State.THRUST && state != State.SWEEP && rand.nextInt(3) == 0 && getHealth() > 0F) {
                setState(State.STAGGER);
            } else if (state == State.POST) {
                setState(State.CHALLENGE);
                stateTicks = CHALLENGE_TICKS - 4;
            }
        }
        return hurt;
    }

    @Override
    public void onDeath(DamageSource cause) {
        super.onDeath(cause);
        if (!world.isRemote) {
            say(FALLING[rand.nextInt(FALLING.length)]);
        }
    }

    /** He goes down to his knees, then falls; no vanilla tipping over. */
    @Override
    protected void onDeathUpdate() {
        deathTime++;
        if (deathTime >= FALL_TICKS && !world.isRemote) {
            setDead();
        }
    }

    // ------------------------------------------------------------------ helpers

    private void walkToward(Vec3d at, double speed) {
        Vec3d to = flat(at.subtract(getPositionVector()));
        double gap = Math.sqrt((at.x - posX) * (at.x - posX) + (at.z - posZ) * (at.z - posZ));
        double pace = Math.min(speed, gap * 0.5D);
        motionX = to.x * pace;
        motionZ = to.z * pace;
        // Never through his brother at arms.
        for (EntityEinherjar other : world.getEntitiesWithinAABB(EntityEinherjar.class, getEntityBoundingBox().grow(1.2D))) {
            if (other != this) {
                Vec3d away = flat(getPositionVector().subtract(other.getPositionVector()));
                motionX += away.x * 0.08D;
                motionZ += away.z * 0.08D;
            }
        }
    }

    private void step(double distance) {
        Vec3d f = facing();
        motionX += f.x * distance;
        motionZ += f.z * distance;
    }

    private void face(Entity target, float rate) {
        double dx = target.posX - posX, dz = target.posZ - posZ;
        double dy = target.posY + target.getEyeHeight() - (posY + getEyeHeight());
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90F;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        rotationYaw += MathHelper.wrapDegrees(yaw - rotationYaw) * rate;
        rotationYawHead = rotationYaw;
        renderYawOffset = rotationYaw;
        rotationPitch += (pitch - rotationPitch) * rate;
    }

    private Vec3d facing() {
        float yaw = (float) Math.toRadians(rotationYaw);
        return new Vec3d(-MathHelper.sin(yaw), 0D, MathHelper.cos(yaw));
    }

    private double horizontal(Entity e) {
        double dx = e.posX - posX, dz = e.posZ - posZ;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static Vec3d flat(Vec3d v) {
        Vec3d f = new Vec3d(v.x, 0D, v.z);
        return f.lengthSquared() < 1.0E-6 ? new Vec3d(0D, 0D, 1D) : f.normalize();
    }

    private void sound(SoundEvent sound, float volume, float pitch) {
        world.playSound(null, posX, posY + 1D, posZ, sound, SoundCategory.HOSTILE, volume, pitch);
    }

    private void say(String line) {
        TextComponentString name = new TextComponentString("<Einherjar> ");
        name.getStyle().setColor(TextFormatting.GOLD).setBold(true);
        TextComponentString text = new TextComponentString(line);
        text.getStyle().setColor(TextFormatting.YELLOW).setBold(false);
        name.appendSibling(text);
        for (EntityPlayer player : world.playerEntities) {
            if (player.getDistance(this) < 48D) {
                player.sendMessage(name);
            }
        }
    }

    // ------------------------------------------------------------------ client: how his cloak moves

    private void clientTick() {
        clientStateTicks++;
        velX += (posX - prevPosX - velX) * 0.5D;
        velZ += (posZ - prevPosZ - velZ) * 0.5D;
        float yaw = (float) Math.toRadians(renderYawOffset);
        double fwd = -velX * MathHelper.sin(yaw) + velZ * MathHelper.cos(yaw);
        double side = velX * MathHelper.cos(yaw) + velZ * MathHelper.sin(yaw);
        double speed = Math.sqrt(velX * velX + velZ * velZ);
        // A heavy cloak: it hangs straight when he stands, and only lifts behind him when he moves.
        float theta = (float) MathHelper.clamp(0.06D + Math.max(0D, fwd) * 2.6D, 0.04D, 0.9D);
        float strength = (float) Math.min(1D, speed * 3D);
        float t = ticksExisted;
        prevCapeSway = capeSway;
        capeSway += ((float) MathHelper.clamp(-side * 2D, -0.3D, 0.3D) + MathHelper.sin(t * 0.04F) * 0.02F - capeSway) * 0.15F;
        System.arraycopy(cape, 0, prevCape, 0, CAPE);
        for (int i = 0; i < CAPE; i++) {
            float flutter = MathHelper.sin(t * (0.25F + 0.3F * strength) - i * 1.1F) * (0.015F + 0.08F * strength) * (0.5F + i * 0.4F);
            float goal = (i == 0 ? theta : prevCape[i - 1] * 0.75F + theta * 0.25F) + flutter;
            capeSpin[i] += (goal - cape[i]) * 0.25F;
            capeSpin[i] *= 0.6F;
            cape[i] += capeSpin[i];
            if (i == 0) {
                cape[i] = Math.max(0.02F, cape[i]);
            } else {
                cape[i] = cape[i - 1] + MathHelper.clamp(cape[i] - cape[i - 1], -0.3F, 0.6F);
            }
        }
    }

    // ------------------------------------------------------------------ the rest

    @Override
    public void knockBack(Entity source, float strength, double x, double z) {
        super.knockBack(source, strength * 0.5F, x, z);
    }

    @Override
    protected boolean canDespawn() {
        return false;
    }

    /** Never written to chunks: the vault posts fresh guards whenever someone comes in. */
    @Override
    public boolean writeToNBTOptional(NBTTagCompound compound) {
        return false;
    }

    @Nullable
    @Override
    public Entity changeDimension(int dimension, ITeleporter teleporter) {
        return null;
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

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_PLAYER_DEATH;
    }

    @Override
    protected float getSoundPitch() {
        return 0.75F + rand.nextFloat() * 0.1F;
    }

    @Override
    protected void playStepSound(net.minecraft.util.math.BlockPos pos, net.minecraft.block.Block block) {
        playSound(SoundEvents.ITEM_ARMOR_EQUIP_CHAIN, 0.35F, 0.7F + rand.nextFloat() * 0.2F);
        super.playStepSound(pos, block);
    }

    @Override
    public float getEyeHeight() {
        return 1.75F;
    }
}
