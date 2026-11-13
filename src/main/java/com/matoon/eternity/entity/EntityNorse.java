package com.matoon.eternity.entity;

import com.matoon.eternity.client.render.NorseBoxes;
import com.matoon.eternity.story.StoryDirector;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityCreature;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * Someone the story needs a face for: the woman in mourning clothes (and, once she has said what
 * she is, what is under them), Ragna, and Hrafn. They do nothing of their own accord; the story
 * moves them about, turns their heads and tells them what to do with their hands (see
 * {@link com.matoon.eternity.story.StoryDirector}). Nothing hurts them.
 * <p>
 * They are never saved with the world. The story knows who should be standing where, and puts
 * them there again whenever someone comes near.
 */
public class EntityNorse extends EntityCreature implements NorseBody.Figure {

    private static final DataParameter<Byte> LOOK = EntityDataManager.createKey(EntityNorse.class, DataSerializers.BYTE);
    private static final DataParameter<Byte> POSE = EntityDataManager.createKey(EntityNorse.class, DataSerializers.BYTE);
    // Three floats, not Rotations: Rotations wraps every value modulo 360, which turned a point far from the origin into nonsense.
    private static final DataParameter<Float> FOCUS_X = EntityDataManager.createKey(EntityNorse.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> FOCUS_Y = EntityDataManager.createKey(EntityNorse.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> FOCUS_Z = EntityDataManager.createKey(EntityNorse.class, DataSerializers.FLOAT);
    private static final DataParameter<Boolean> ARMED = EntityDataManager.createKey(EntityNorse.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> SHINING = EntityDataManager.createKey(EntityNorse.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Float> ASH = EntityDataManager.createKey(EntityNorse.class, DataSerializers.FLOAT);
    private static final DataParameter<Byte> ROT = EntityDataManager.createKey(EntityNorse.class, DataSerializers.BYTE);
    private static final DataParameter<Byte> THRILL = EntityDataManager.createKey(EntityNorse.class, DataSerializers.BYTE);

    // ------------------------------------------------------------------ server
    /** What the story calls them by, so it can find them again. */
    public String role = "";
    private int poseTicks;
    private int holdFor = -1;
    private NorsePose rest = NorsePose.STAND;
    @Nullable
    private Vec3d walkTo;
    private double walkSpeed = 0.5D;
    @Nullable
    private Entity following;
    private double followAt = 3D;
    @Nullable
    private Entity watching;
    /** Lying people keep the heading they were laid down with. */
    private float laidYaw;

    // ------------------------------------------------------------------ client
    private final NorseBody body = new NorseBody();
    /** Client: the rot last drawn, and when it last got worse. */
    private int shownRot = -1;
    private int rotted = -1000;
    /** Client: when she last shuddered at something killed for her. */
    public int thrilledAt = -1000;
    /** Client: the pieces of skin she has torn off, flying or lying where they fell. */
    public final java.util.List<Scrap> scraps = new java.util.ArrayList<>();

    public EntityNorse(World world) {
        super(world);
        setSize(0.6F, 1.8F);
        experienceValue = 0;
        enablePersistence();
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(LOOK, (byte) NorseBoxes.STRANGER);
        dataManager.register(POSE, (byte) NorsePose.STAND.ordinal());
        dataManager.register(FOCUS_X, 0F);
        dataManager.register(FOCUS_Y, -1000F);
        dataManager.register(FOCUS_Z, 0F);
        dataManager.register(ARMED, false);
        dataManager.register(SHINING, false);
        dataManager.register(ASH, 0F);
        dataManager.register(ROT, (byte) 0);
        dataManager.register(THRILL, (byte) 0);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(40D);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.3D);
        getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(1D);
        getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(64D);
    }

    public static EntityNorse of(World world, int look, String role) {
        EntityNorse e = new EntityNorse(world);
        e.dataManager.set(LOOK, (byte) look);
        e.role = role;
        return e;
    }

    @Override
    public void notifyDataManagerChange(DataParameter<?> key) {
        super.notifyDataManagerChange(key);
        if (POSE.equals(key) && world.isRemote) {
            body.changed(pose());
        }
        if (ROT.equals(key) && world.isRemote) {
            int now = rot();
            if (shownRot >= 0 && now > shownRot && ticksExisted > 2) {
                rotted = ticksExisted;
            }
            shownRot = now;
        }
        if (THRILL.equals(key) && world.isRemote && ticksExisted > 2) {
            thrilled();
        }
        if (LOOK.equals(key) && world.isRemote) {
            // Changed in front of whoever is watching (not just arriving looking like it): she changes before their eyes.
            int now = look();
            // Out of her skin, she needs no smoke: she is simply what was under it.
            if (shownLook >= 0 && shownLook != now && ticksExisted > 2 && pose() != NorsePose.TEAR) {
                shiftFrom = shownLook;
                shiftAt = ticksExisted;
            }
            shownLook = now;
        }
    }

    // ------------------------------------------------------------------ what they are

    @Override
    public int look() {
        return dataManager.get(LOOK);
    }

    public void setLook(int look) {
        dataManager.set(LOOK, (byte) look);
    }

    /** Client: the look last drawn, the one changed from, and when it changed. */
    private int shownLook = -1;
    public int shiftFrom = -1;
    public int shiftAt = -1000;

    /** Client: how many ticks into changing shape she is (see {@link Morph}), or -1 if she isn't. */
    public float morphTicks(float partialTicks) {
        float t = ticksExisted - shiftAt + partialTicks;
        return shiftFrom >= 0 && t >= 0F && t < Morph.TICKS ? t : -1F;
    }

    /**
     * Becomes something else in front of whoever is there to see it, a piece at a time, from the
     * inside out (see {@link Morph}; the clients do the rest).
     */
    public void shapeshift(int look) {
        if (look() == look) {
            return;
        }
        setLook(look);
        world.playSound(null, posX, posY + 1D, posZ, SoundEvents.ENTITY_ZOMBIE_VILLAGER_CONVERTED, SoundCategory.NEUTRAL, 0.9F, 0.5F);
    }

    @Override
    public NorsePose pose() {
        int i = dataManager.get(POSE);
        return i >= 0 && i < NorsePose.values().length ? NorsePose.values()[i] : NorsePose.STAND;
    }

    @Override
    public NorseBody body() {
        return body;
    }

    public boolean isArmed() {
        return dataManager.get(ARMED);
    }

    public void setArmed(boolean armed) {
        dataManager.set(ARMED, armed);
    }

    /** Whether something is lit in them: the Time Stone, once the Gardener has pressed it into his chest. */
    public boolean isShining() {
        return dataManager.get(SHINING);
    }

    /** How far the life has gone out of their skin, 0 to 1: grey, and dull, and done. */
    public float ash() {
        return dataManager.get(ASH);
    }

    public void setAsh(float ash) {
        dataManager.set(ASH, ash);
    }

    /** How far the pretty one has rotted on her, 0 to {@link Rot#MAX}: the closer someone comes to her, the worse it gets. */
    public int rot() {
        return dataManager.get(ROT);
    }

    public void setRot(int rot) {
        dataManager.set(ROT, (byte) Math.max(0, Math.min(Rot.MAX, rot)));
    }

    /**
     * Something has been killed for her, and she feels it: a shudder all the way down, her eyes
     * rolling shut; and, if she is wearing the pretty one, something of it comes loose and drops.
     */
    public void thrill() {
        dataManager.set(THRILL, (byte) (dataManager.get(THRILL) + 1));
    }

    /**
     * Tears the pretty one off, in front of whoever is watching, and is what was under it (see
     * {@link SkinTear}). The clients see it; the server only ends it.
     */
    public void tear() {
        setLook(NorseBoxes.TEMPTRESS);
        setRot(Rot.MAX);
        rest(NorsePose.STAND).act(NorsePose.TEAR, SkinTear.TICKS + 4);
    }

    public void setShining(boolean shining) {
        dataManager.set(SHINING, shining);
    }

    /** The point they are attending to, or null for none. */
    @Nullable
    public Vec3d focus() {
        float y = dataManager.get(FOCUS_Y);
        return y < -999F ? null : new Vec3d(dataManager.get(FOCUS_X), y, dataManager.get(FOCUS_Z));
    }

    private void setFocusPoint(@Nullable Vec3d at) {
        dataManager.set(FOCUS_X, at == null ? 0F : (float) at.x);
        dataManager.set(FOCUS_Y, at == null ? -1000F : (float) at.y);
        dataManager.set(FOCUS_Z, at == null ? 0F : (float) at.z);
    }


    // ------------------------------------------------------------------ the story's handles

    /** Does something and holds it until told otherwise. */
    public EntityNorse act(NorsePose pose) {
        return act(pose, -1);
    }

    /** Does something for so many ticks (or as long as the gesture lasts, if 0), then goes back to resting. */
    public EntityNorse act(NorsePose pose, int ticks) {
        poseTicks = 0;
        holdFor = ticks == 0 ? pose.ticks() : ticks;
        if (pose.lying()) {
            laidYaw = rotationYaw;
        }
        dataManager.set(POSE, (byte) pose.ordinal());
        if (pose == NorsePose.DRAW) {
            setArmed(true);
        }
        return this;
    }

    /** What they go back to after a gesture. */
    public EntityNorse rest(NorsePose pose) {
        rest = pose;
        return this;
    }

    public NorsePose restPose() {
        return rest;
    }

    /** Turns them to look at a point, and keeps them looking. */
    public EntityNorse look(@Nullable Vec3d at) {
        watching = null;
        setFocusPoint(at);
        return this;
    }

    /** Keeps them looking at someone, wherever they go. */
    public EntityNorse watch(@Nullable Entity who) {
        watching = who;
        if (who == null) {
            look(null);
        }
        return this;
    }

    public EntityNorse walkTo(@Nullable Vec3d to, double speed) {
        following = null;
        walkTo = to;
        walkSpeed = speed;
        if (to == null) {
            getNavigator().clearPath();
        } else {
            getNavigator().tryMoveToXYZ(to.x, to.y, to.z, speed);
        }
        return this;
    }

    public EntityNorse follow(@Nullable Entity who, double distance) {
        walkTo = null;
        following = who;
        followAt = distance;
        if (who == null) {
            getNavigator().clearPath();
        }
        return this;
    }

    public boolean arrived() {
        return walkTo == null || getDistanceSq(walkTo.x, walkTo.y, walkTo.z) < 2.25D;
    }

    /** Turns body and head to face a point at once. */
    public void face(Vec3d at) {
        double dx = at.x - posX, dz = at.z - posZ;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        rotationYaw = renderYawOffset = rotationYawHead = prevRotationYaw = prevRenderYawOffset = prevRotationYawHead = yaw;
        laidYaw = yaw;
    }

    /** A flinch, seen by everyone: something has struck them. */
    public void flinch() {
        world.setEntityState(this, (byte) 2);
    }

    /**
     * Client: while she changes shape, the sounds of it (things moving under skin, wet, and the
     * crack of bone going where it can't); and as each piece of her bursts, blood, and the old
     * skin of it sloughing off her and dropping.
     */
    private void morphing() {
        float m = morphTicks(0F);
        if (m < 0F) {
            return;
        }
        int t = (int) m;
        double fx = -MathHelper.sin(renderYawOffset * 0.017453292F), fz = MathHelper.cos(renderYawOffset * 0.017453292F);
        if (t == 1) {
            // A breath in, through her teeth, as it starts.
            world.playSound(posX, posY + 1.6D, posZ, SoundEvents.ENTITY_ZOMBIE_INFECT, SoundCategory.NEUTRAL, 0.9F, 0.5F, false);
            world.playSound(posX, posY + 1.6D, posZ, SoundEvents.ENTITY_VEX_HURT, SoundCategory.NEUTRAL, 0.5F, 0.4F, false);
        }
        if (t % 3 == 0 && t < Morph.TICKS - 10) {
            // Something moving under the skin: wet, and never still.
            world.playSound(posX, posY + 1D, posZ, rand.nextBoolean() ? SoundEvents.BLOCK_SLIME_STEP : SoundEvents.ENTITY_SMALL_SLIME_SQUISH,
                    SoundCategory.NEUTRAL, 0.6F, 0.45F + rand.nextFloat() * 0.4F, false);
            blood(posX + (rand.nextDouble() - 0.5D) * 0.5D, posY + 0.3D + rand.nextDouble() * 1.4D, posZ + (rand.nextDouble() - 0.5D) * 0.5D, 1, 0.02D);
        }
        if (t % 7 == 2 && t > 6 && t < Morph.TICKS - 12) {
            world.playSound(posX, posY + 1.2D, posZ, SoundEvents.ENTITY_ZOMBIE_ATTACK_DOOR_WOOD, SoundCategory.NEUTRAL, 0.3F, 1.8F + rand.nextFloat() * 0.2F, false);
        }
        int r = Morph.burstingAt(t);
        if (r >= 0 && m - t < 1F) {
            double lx = fz, lz = -fx;
            double[] f = Morph.FROM[r];
            double x = posX + lx * f[0] + fx * f[2], y = posY + f[1], z = posZ + lz * f[0] + fz * f[2];
            world.playSound(x, y, z, SoundEvents.ENTITY_ZOMBIE_ATTACK_DOOR_WOOD, SoundCategory.NEUTRAL, 0.7F, 1.6F, false);
            world.playSound(x, y, z, SoundEvents.BLOCK_SLIME_BREAK, SoundCategory.NEUTRAL, 1.1F, 0.55F, false);
            world.playSound(x, y, z, SoundEvents.ENTITY_SLIME_SQUISH, SoundCategory.NEUTRAL, 1F, 0.5F, false);
            if (r == Morph.HEAD) {
                world.playSound(x, y, z, SoundEvents.ENTITY_VEX_CHARGE, SoundCategory.NEUTRAL, 0.6F, 0.45F, false);
            }
            blood(x, y, z, 14, 0.12D);
            for (String part : Morph.SLOUGH[r]) {
                Scrap s = new Scrap(part, x, y, z, lx * f[0] * 0.25D + fx * 0.04D + (rand.nextDouble() - 0.5D) * 0.06D, 0.06D,
                        lz * f[0] * 0.25D + fz * 0.04D + (rand.nextDouble() - 0.5D) * 0.06D, renderYawOffset, posY, rand);
                s.look = shiftFrom;
                s.rot = rot();
                scraps.add(s);
            }
        }
    }

    /** Client: as she rots, flakes come off her when it gets worse; at the worst, flies, and something drips. */
    private void rotting() {
        if (look() != NorseBoxes.TEMPTRESS) {
            return;
        }
        int r = rot();
        if (ticksExisted - rotted < 30) {
            // Worse, in front of you: grey flakes of her lift off and drift down.
            for (int i = 0; i < 3; i++) {
                world.spawnParticle(EnumParticleTypes.SPELL_MOB, posX + (rand.nextDouble() - 0.5D) * 0.7D, posY + rand.nextDouble() * 1.8D,
                        posZ + (rand.nextDouble() - 0.5D) * 0.7D, 0.35D, 0.38D, 0.3D);
            }
            if (ticksExisted - rotted == 1) {
                world.playSound(posX, posY + 1D, posZ, SoundEvents.ENTITY_ZOMBIE_VILLAGER_CONVERTED, SoundCategory.NEUTRAL, 0.35F, 1.6F, false);
            }
        }
        if (r >= 3 && rand.nextInt(r == 4 ? 4 : 9) == 0) {
            // Flies, in a little cloud round her head and her hips.
            double a = rand.nextDouble() * Math.PI * 2D;
            world.spawnParticle(EnumParticleTypes.SUSPENDED_DEPTH, posX + Math.cos(a) * 0.45D, posY + (rand.nextBoolean() ? 1.5D : 0.8D) + rand.nextDouble() * 0.3D,
                    posZ + Math.sin(a) * 0.45D, -Math.sin(a) * 0.05D, 0D, Math.cos(a) * 0.05D);
        }
        if (r == 4 && rand.nextInt(14) == 0) {
            blood(posX + (rand.nextDouble() - 0.5D) * 0.4D, posY + 0.4D + rand.nextDouble() * 1.1D, posZ + (rand.nextDouble() - 0.5D) * 0.4D, 1, 0.01D);
        }
        if (r >= 3 && rand.nextInt(500) == 0) {
            world.playSound(posX, posY + 1D, posZ, SoundEvents.ENTITY_SLIME_SQUISH, SoundCategory.NEUTRAL, 0.25F, 0.5F, false);
        }
        if (r >= 5) {
            // Past the worst of the rot: maggots drop off her, things drip from her as she moves, and the
            // more there is of it the more often.
            if (rand.nextInt(12 - r) == 0) {
                world.spawnParticle(EnumParticleTypes.ITEM_CRACK, posX + (rand.nextDouble() - 0.5D) * 0.5D, posY + 0.3D + rand.nextDouble() * 1.3D,
                        posZ + (rand.nextDouble() - 0.5D) * 0.5D, 0D, -0.05D, 0D, Item.getIdFromItem(Items.ROTTEN_FLESH));
            }
            if (rand.nextInt(Math.max(2, 10 - r)) == 0) {
                world.spawnParticle(EnumParticleTypes.TOWN_AURA, posX + (rand.nextDouble() - 0.5D) * 0.6D, posY + 0.2D + rand.nextDouble() * 1.6D,
                        posZ + (rand.nextDouble() - 0.5D) * 0.6D, 0D, 0D, 0D);
            }
            if (Rot.snaps(ticksExisted, getEntityId(), r) && pose() != NorsePose.TEAR) {
                // A joint going where it can't: a crack of bone, wet.
                world.playSound(posX, posY + 1.4D, posZ, SoundEvents.ENTITY_ZOMBIE_ATTACK_DOOR_WOOD, SoundCategory.NEUTRAL, 0.35F, 1.9F, false);
                world.playSound(posX, posY + 1.4D, posZ, SoundEvents.BLOCK_SLIME_STEP, SoundCategory.NEUTRAL, 0.7F, 0.6F, false);
            }
        }
    }

    /** Client: the shudder when something is killed for her; a piece of the pretty one dropping off her if she has it on. */
    private void thrilled() {
        thrilledAt = ticksExisted;
        world.playSound(posX, posY + 1.6D, posZ, SoundEvents.ENTITY_VEX_AMBIENT, SoundCategory.NEUTRAL, 0.7F, 0.55F, false);
        if (look() != NorseBoxes.TEMPTRESS) {
            return;
        }
        String[] loose = rot() >= 5 ? new String[]{"fingersL", "fingersR", "flapL", "flapR", "thumbL"} : new String[]{"fingersL", "fingersR", "thumbR"};
        String part = loose[rand.nextInt(loose.length)];
        double fx = -MathHelper.sin(renderYawOffset * 0.017453292F), fz = MathHelper.cos(renderYawOffset * 0.017453292F);
        double side = part.endsWith("L") ? 0.3D : -0.3D;
        double x = posX + fz * side + fx * 0.15D, z = posZ - fx * side + fz * 0.15D;
        Scrap s = new Scrap(part, x, posY + 0.85D, z, fz * side * 0.1D + fx * 0.03D, 0.08D, -fx * side * 0.1D + fz * 0.03D, renderYawOffset, posY, rand);
        s.rot = rot();
        scraps.add(s);
        blood(x, posY + 0.85D, z, 6, 0.06D);
        world.playSound(x, posY + 0.85D, z, SoundEvents.BLOCK_SLIME_BREAK, SoundCategory.NEUTRAL, 0.6F, 0.9F, false);
    }

    /** Client: while her lid is flipped, the scream, and blood off her teeth with every jerk of her head. */
    private void raging() {
        if (pose() != NorsePose.FURY) {
            return;
        }
        int t = body.stateTicks;
        if (t == 1) {
            world.playSound(posX, posY + 1.6D, posZ, SoundEvents.ENTITY_GHAST_SCREAM, SoundCategory.HOSTILE, 1.2F, 0.45F, false);
            world.playSound(posX, posY + 1.6D, posZ, SoundEvents.ENTITY_VEX_CHARGE, SoundCategory.HOSTILE, 1F, 0.5F, false);
        }
        if (t % 9 == 4) {
            world.playSound(posX, posY + 1.6D, posZ, SoundEvents.ENTITY_ZOMBIE_ATTACK_DOOR_WOOD, SoundCategory.HOSTILE, 0.4F, 1.7F, false);
        }
        if (t % 2 == 0) {
            double fx = -MathHelper.sin(renderYawOffset * 0.017453292F), fz = MathHelper.cos(renderYawOffset * 0.017453292F);
            blood(posX + fx * 0.3D, posY + 1.45D, posZ + fz * 0.3D, 2, 0.06D);
        }
    }

    /** Client: blood, in the colour redstone dust takes from its "speed", and wet red bits of her. */
    private void blood(double x, double y, double z, int n, double spread) {
        for (int i = 0; i < n; i++) {
            double vx = (rand.nextDouble() - 0.5D) * spread, vy = rand.nextDouble() * spread, vz = (rand.nextDouble() - 0.5D) * spread;
            world.spawnParticle(EnumParticleTypes.REDSTONE, x + vx * 3D, y + vy * 3D, z + vz * 3D, 0.45D + rand.nextDouble() * 0.15D, 0.001D, 0.02D);
            if (spread > 0.05D && i % 2 == 0) {
                world.spawnParticle(EnumParticleTypes.BLOCK_CRACK, x, y, z, vx * 2D, vy * 2D + 0.1D, vz * 2D,
                        Block.getStateId((i % 4 == 0 ? Blocks.REDSTONE_BLOCK : Blocks.NETHER_WART_BLOCK).getDefaultState()));
            }
            if (spread > 0.05D && i % 3 == 0) {
                world.spawnParticle(EnumParticleTypes.ITEM_CRACK, x, y, z, vx * 2D, vy * 2D + 0.12D, vz * 2D, Item.getIdFromItem(Items.ROTTEN_FLESH));
            }
        }
    }

    /** Client: while she tears it off, the shudders, the blood and the sounds of it, each on its tick. */
    private void tearing() {
        if (pose() != NorsePose.TEAR || look() != NorseBoxes.TEMPTRESS) {
            return;
        }
        int t = body.stateTicks;
        double fx = -MathHelper.sin(renderYawOffset * 0.017453292F), fz = MathHelper.cos(renderYawOffset * 0.017453292F);
        if (t == 2) {
            world.playSound(posX, posY + 1D, posZ, SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.NEUTRAL, 0.5F, 0.5F, false);
        }
        if (t < 50 && t % 7 == 3) {
            // The skin splitting along its seams as she shudders.
            world.playSound(posX, posY + 1D, posZ, SoundEvents.BLOCK_SLIME_STEP, SoundCategory.NEUTRAL, 0.8F, 0.5F + rand.nextFloat() * 0.3F, false);
            blood(posX, posY + 0.6D + rand.nextDouble() * 1.1D, posZ, 2, 0.02D);
        }
        if (t > 24 && t < 54 && t % 5 == 0) {
            // Clawing at her own face: wet scratching, and a thin shriek through her teeth now and then.
            world.playSound(posX, posY + 1.5D, posZ, SoundEvents.BLOCK_SLIME_HIT, SoundCategory.NEUTRAL, 0.7F, 1.3F + rand.nextFloat() * 0.3F, false);
            blood(posX + fx * 0.25D, posY + 1.55D, posZ + fz * 0.25D, 3, 0.04D);
            if (t % 15 == 0) {
                world.playSound(posX, posY + 1.5D, posZ, SoundEvents.ENTITY_VEX_HURT, SoundCategory.NEUTRAL, 0.45F, 0.55F, false);
            }
        }
        if (t > 76 && t < 96 && t % 4 == 0) {
            // Fingers in at the hairline, working the face loose.
            world.playSound(posX, posY + 1.6D, posZ, SoundEvents.BLOCK_SLIME_STEP, SoundCategory.NEUTRAL, 0.9F, 0.7F, false);
            blood(posX + fx * 0.2D, posY + 1.75D, posZ + fz * 0.2D, 2, 0.03D);
        }
        int piece = SkinTear.pieceAt(t);
        if (piece >= 0) {
            world.playSound(posX, posY + 1D, posZ, SoundEvents.BLOCK_SLIME_BREAK, SoundCategory.NEUTRAL, 1.2F, 0.6F, false);
            world.playSound(posX, posY + 1D, posZ, SoundEvents.BLOCK_CLOTH_BREAK, SoundCategory.NEUTRAL, 1F, 0.5F, false);
            world.playSound(posX, posY + 1D, posZ, SoundEvents.ENTITY_ZOMBIE_INFECT, SoundCategory.NEUTRAL, 0.8F, 0.6F, false);
            double[][] from = SkinTear.FROM[piece];
            double lx = fz, lz = -fx;
            for (int i = 0; i < from.length; i++) {
                double[] f = from[i];
                double x = posX + lx * f[0] + fx * f[2], z = posZ + lz * f[0] + fz * f[2];
                scraps.add(new Scrap(SkinTear.FLUNG[piece][i], x, posY + f[1], z, lx * f[3] + fx * f[5], f[4], lz * f[3] + fz * f[5],
                        renderYawOffset, posY, rand));
                blood(x, posY + f[1], z, 10, 0.12D);
            }
        }
        if (t == 150) {
            // Out: she straightens, and breathes out, long and slow, like something set down at last.
            world.playSound(posX, posY + 1.7D, posZ, SoundEvents.ENTITY_VEX_AMBIENT, SoundCategory.NEUTRAL, 0.8F, 0.45F, false);
            world.playSound(posX, posY + 1.7D, posZ, SoundEvents.ENTITY_ILLAGER_MIRROR_MOVE, SoundCategory.NEUTRAL, 0.6F, 0.4F, false);
        }
    }

    /** Client: the pieces fly, land, lie there a while bleeding, and sink into the ground. */
    private void tickScraps() {
        for (java.util.Iterator<Scrap> it = scraps.iterator(); it.hasNext(); ) {
            Scrap s = it.next();
            if (s.tick()) {
                blood(s.x, s.y + 0.05D, s.z, 6, 0.08D);
                world.playSound(s.x, s.y, s.z, SoundEvents.ENTITY_SLIME_SQUISH, SoundCategory.NEUTRAL, 0.7F, 0.6F, false);
            }
            if (s.age > 40 && s.age < 160 && rand.nextInt(20) == 0) {
                blood(s.x, s.y + 0.05D, s.z, 1, 0.01D);
            }
            if (s.age > Scrap.LIFE) {
                it.remove();
            }
        }
    }

    /** A piece of the skin she took off: where it is, how it is turning, and whether it has landed. */
    public static final class Scrap {
        public static final int LIFE = 300;
        public final String part;
        public double x, y, z, prevX, prevY, prevZ, vx, vy, vz;
        public float yaw, pitch, roll, prevPitch, prevRoll;
        private final float pitchSpin, rollSpin, ground;
        public boolean landed;
        public int age;
        /** How rotten the skin was when it came off, to draw it the same. */
        public int rot = Rot.MAX;
        /** Which of her shapes it was a piece of. */
        public int look = NorseBoxes.TEMPTRESS;

        Scrap(String part, double x, double y, double z, double vx, double vy, double vz, float yaw, double ground, java.util.Random rand) {
            this.part = part;
            this.x = prevX = x;
            this.y = prevY = y;
            this.z = prevZ = z;
            this.vx = vx;
            this.vy = vy;
            this.vz = vz;
            this.yaw = yaw;
            this.ground = (float) ground + 0.08F;
            boolean thrown = Math.abs(vx) + Math.abs(vz) + vy > 0.15D;
            pitchSpin = (thrown ? 18F : 6F) * (rand.nextBoolean() ? 1F : -1F) * (0.6F + rand.nextFloat() * 0.6F);
            rollSpin = (thrown ? 12F : 2F) * (rand.nextFloat() - 0.5F);
        }

        /** Moves it on a tick; true on the tick it lands. */
        boolean tick() {
            age++;
            prevX = x;
            prevY = y;
            prevZ = z;
            prevPitch = pitch;
            prevRoll = roll;
            if (landed) {
                // Lying in a heap, it settles flat; at the end, it sinks into the ground.
                pitch += ((pitch >= 0F ? 90F : -90F) - pitch) * 0.25F;
                roll *= 0.75F;
                if (age > LIFE - 60) {
                    y -= 0.006D;
                }
                return false;
            }
            vy -= 0.045D;
            x += vx;
            y += vy;
            z += vz;
            pitch += pitchSpin;
            roll += rollSpin;
            if (y <= ground) {
                y = ground;
                landed = true;
                pitch = ((pitch % 360F) + 540F) % 360F - 180F;
                return true;
            }
            return false;
        }
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (world.isRemote) {
            body.tick(this);
            morphing();
            rotting();
            raging();
            tearing();
            tickScraps();
            return;
        }
        poseTicks++;
        if (pose() == NorsePose.TEAR && poseTicks == SkinTear.TICKS) {
            // Out of it: while the pose still says she is tearing, so nobody sees smoke.
            setLook(NorseBoxes.UNVEILED);
            setRot(0);
        }
        if (holdFor > 0 && poseTicks >= holdFor && pose() != rest) {
            holdFor = -1;
            poseTicks = 0;
            dataManager.set(POSE, (byte) rest.ordinal());
        }
        if (following != null) {
            if (following.isDead || following.dimension != dimension) {
                following = null;
            } else if (getDistance(following) > followAt + 1.5D) {
                if (ticksExisted % 10 == 0 || getNavigator().noPath()) {
                    getNavigator().tryMoveToEntityLiving(following, getDistance(following) > 10D ? 0.75D : 0.55D);
                }
            } else if (getDistance(following) < followAt) {
                getNavigator().clearPath();
            }
        } else if (walkTo != null) {
            if (arrived()) {
                getNavigator().clearPath();
            } else if (getNavigator().noPath() && ticksExisted % 20 == 0) {
                if (!getNavigator().tryMoveToXYZ(walkTo.x, walkTo.y, walkTo.z, walkSpeed)) {
                    // Nowhere to walk: step straight there.
                    getMoveHelper().setMoveTo(walkTo.x, walkTo.y, walkTo.z, walkSpeed);
                }
            }
        }
        if (watching != null) {
            if (watching.isDead) {
                watching = null;
            } else {
                Vec3d eye = watching.getPositionEyes(1F);
                Vec3d was = focus();
                if (was == null || was.distanceTo(eye) > 0.2D) {
                    setFocusPoint(eye);
                }
            }
        }
        Vec3d focus = focus();
        if (pose().lying()) {
            rotationYaw = renderYawOffset = laidYaw;
        } else if (focus != null && getNavigator().noPath()) {
            // Standing still, they turn their head to what they attend to, and their body after it.
            double dx = focus.x - posX, dz = focus.z - posZ;
            float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            rotationYawHead = approach(rotationYawHead, yaw, 12F);
            if (Math.abs(MathHelper.wrapDegrees(yaw - renderYawOffset)) > 50F) {
                renderYawOffset = approach(renderYawOffset, yaw, 4F);
                rotationYaw = renderYawOffset;
            }
            double dy = focus.y - (posY + getEyeHeight());
            rotationPitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        }
    }

    private static float approach(float from, float to, float max) {
        return from + MathHelper.clamp(MathHelper.wrapDegrees(to - from), -max, max);
    }

    // ------------------------------------------------------------------ the world can't touch them

    @Override
    protected boolean processInteract(EntityPlayer player, EnumHand hand) {
        if (!world.isRemote && hand == EnumHand.MAIN_HAND && player instanceof EntityPlayerMP) {
            StoryDirector.INSTANCE.interact((EntityPlayerMP) player, this);
        }
        return true;
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (!world.isRemote && source.getTrueSource() instanceof EntityPlayerMP) {
            StoryDirector.INSTANCE.struck((EntityPlayerMP) source.getTrueSource(), this);
        }
        return false;
    }

    @Override
    public boolean isEntityInvulnerable(DamageSource source) {
        return source != DamageSource.OUT_OF_WORLD;
    }

    @Override
    public boolean canBePushed() {
        return false;
    }

    @Override
    protected void collideWithEntity(Entity entity) {
    }

    @Override
    public boolean canDespawn() {
        return false;
    }

    @Override
    protected boolean canDropLoot() {
        return false;
    }

    @Override
    public boolean writeToNBTOptional(NBTTagCompound compound) {
        return false;
    }

    @Override
    public boolean isInRangeToRenderDist(double distance) {
        return distance < 160D * 160D;
    }

    @Override
    public float getEyeHeight() {
        switch (pose()) {
            case KNEEL:
            case HOLD:
            case CLOSE:
            case TOUCH:
                return 1.05F;
            case GROUND:
                return 0.9F;
            case LIE:
            case DEAD:
            case FALLEN:
                return 0.25F;
            default:
                return 1.62F;
        }
    }

    @Override
    public float getRenderSizeModifier() {
        return look() == NorseBoxes.HRAFN ? 1F : 0.94F;
    }
}
