package com.matoon.eternity.entity;

import com.matoon.eternity.Eternity;
import com.matoon.eternity.EternityConfig;
import com.matoon.eternity.EternitySounds;
import com.matoon.eternity.compat.Relics;
import com.matoon.eternity.compat.StoneAbilities;
import com.matoon.eternity.inventory.EternityGuis;
import com.matoon.eternity.item.ItemAstralRegulator;
import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.network.MsgTribunalFx;
import com.matoon.eternity.world.Nexus;
import com.matoon.eternity.world.OdinsVault;
import com.matoon.eternity.world.StarChamber;
import lucraft.mods.heroesexpansion.abilities.AbilityForceField;
import lucraft.mods.heroesexpansion.entities.EntityBlackHole;
import lucraft.mods.lucraftcore.infinity.items.ItemInfinityStone;
import lucraft.mods.lucraftcore.sizechanging.capabilities.CapabilitySizeChanging;
import lucraft.mods.lucraftcore.superpowers.abilities.AbilityHealing;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.network.play.server.SPacketMoveVehicle;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSourceIndirect;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The Destroyer Armor: the Destroyer's own body, emptied of whatever drove it and open at the
 * back for someone to climb into. Beaten in Odin's Vault, the Destroyer can be claimed where it
 * lies; it rises, empty, and waits.
 * <ul>
 * <li><b>Getting in</b>: use it. You step up into its back, it lifts you into its chest, its back
 * plates swing shut, and the fire behind its face kindles. You see through its face.</li>
 * <li><b>Walking</b>: slow, enormous strides that shake the ground, steps you would have to climb
 * taken in one, and nothing slows it in water. Sprint, and it charges, throwing aside anything in
 * its path. Jump, and it leaps; wherever it comes down the ground breaks in rings.</li>
 * <li><b>Attack</b>: punch, one fist then the other as long as you keep swinging. Its fists break
 * soft stone and wood they land on.</li>
 * <li><b>Use, held</b>: its face plates open and the furnace pours out the Destroyer's ray where you
 * look, for as long as you hold it or until the furnace overheats and must vent.</li>
 * <li><b>Slam</b> (its own key): both fists brought down together; rings of force race out across the ground.</li>
 * <li><b>Getting out</b>: hold sneak. Its back opens and it sets you down behind it.</li>
 * </ul>
 * Nothing touches whoever is inside: every blow lands on the armour. Beaten, it goes down on its
 * knees, puts you out, and stays there until it is mended with Uru.
 * <p>
 * Keeping it running: its <b>integrity</b> (its health) falls as it is struck, and the lower it falls
 * the worse it moves and fights, until its ray and its leaps fail altogether; Uru mends it. Its
 * furnace burns <b>lava</b>, slowly standing, faster walking, and fast pouring out the ray; cold,
 * it can barely move. Both are seen to, and its {@link ArmorUpgrade upgrades} forged and the
 * Infinity Stones set in its chest (see {@link ArmorPowers}), from its systems menu.
 */
public class EntityDestroyerArmor extends EntityLivingBase implements DestroyerBody {

    public enum State { EMPTY, RISE, ENTER, EXIT, READY, PUNCH_R, PUNCH_L, SLAM, RAY, RAY_END, VENT, DOWN, IMPLODE }

    /** What its pilot asks of it. */
    public enum Command { ATTACK, FIRE, SLAM, EXIT, MENU, FLY, JUMP, SURGE, ABSORB }

    /** How badly it is hurt, by how much integrity it has left. */
    public enum Condition {
        NOMINAL("Nominal", 0x7FD86A, 1F), STRAINED("Strained", 0xE8C050, 0.85F), FAILING("Failing", 0xF07830, 0.7F), CRITICAL("Critical", 0xFF3B2B, 0.5F);

        public final String title;
        public final int color;
        /** How freely it moves in this condition. */
        public final float stride;

        Condition(String title, int color, float stride) {
            this.title = title;
            this.color = color;
            this.stride = stride;
        }
    }

    /**
     * Its own inventory: what feeds it going in (lava, or with its dynamo anything holding Forge Energy) and what is
     * spent of it coming out, Uru plate to mend it, the six stone sockets, and the Astral Regulator in its chest.
     */
    public static final int SLOT_FUEL = 0, SLOT_SPENT = 1, SLOT_URU = 2, SLOT_STONES = 3, SLOT_REGULATOR = 9, SLOTS = 10;
    public static final float BUCKET = 1000F;
    /** With its dynamo it stores Forge Energy where it stored lava, a millibucket's room holding this much. */
    public static final float FE_PER_MB = 100F;
    /** How fast it drinks from whatever feeds its dynamo, in Forge Energy a tick. */
    private static final int DRAW = 50000;
    private static final UUID LENT_ARMOR = UUID.fromString("6a0f6d0e-3c1e-4d7a-9a51-2f5d2b7b0e11");
    private static final UUID LENT_LIFE = UUID.fromString("b1e2c4a8-5d2f-4e0b-8c1d-7a9e3f6b2c44");

    // How long everything takes, in ticks; the model times its frames to the same numbers.
    public static final int RISE_TICKS = 70;
    /** Getting in: stepping up into its back, being lifted into its chest, its back shutting, the fire kindling. */
    public static final int ENTER_STEP = 12, ENTER_LIFT = 26, ENTER_SHUT = 30, ENTER_TICKS = 44;
    /** Getting out: its back opening, being lowered and set down behind it, the back left standing open. */
    public static final int EXIT_OPEN = 8, EXIT_OUT = 26, EXIT_TICKS = 34;
    public static final int PUNCH_HIT = 6, PUNCH_TICKS = 14;
    public static final int SLAM_HIT = 18, SLAM_TICKS = 38;
    public static final int RAY_OPEN = 10, RAY_CLOSE = 12;
    public static final int VENT_TICKS = 80;
    public static final float HEAT_MAX = 100F;
    /** Falling in on itself, its core breached, before it is gone. */
    public static final int IMPLODE_TICKS = 24;

    /** Where whoever is inside sits: their eyes at its face, a little forward of its middle. */
    private static final double SEAT_UP = 2.55D, SEAT_FORWARD = 0.25D, BEHIND = 1.7D;
    /** How far its fists reach, and its ray. */
    private static final double REACH = 4.8D, RAY_RANGE = 48D;

    private static final DataParameter<Byte> STATE = EntityDataManager.createKey(EntityDestroyerArmor.class, DataSerializers.BYTE);
    private static final DataParameter<Float> HEAT = EntityDataManager.createKey(EntityDestroyerArmor.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> LAVA = EntityDataManager.createKey(EntityDestroyerArmor.class, DataSerializers.FLOAT);
    private static final DataParameter<Integer> FORGED = EntityDataManager.createKey(EntityDestroyerArmor.class, DataSerializers.VARINT);
    private static final DataParameter<Byte> STONES = EntityDataManager.createKey(EntityDestroyerArmor.class, DataSerializers.BYTE);
    private static final DataParameter<Boolean> FLYING = EntityDataManager.createKey(EntityDestroyerArmor.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> GLIDING = EntityDataManager.createKey(EntityDestroyerArmor.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Byte> SKIN = EntityDataManager.createKey(EntityDestroyerArmor.class, DataSerializers.BYTE);
    private static final DataParameter<Integer> SKINS = EntityDataManager.createKey(EntityDestroyerArmor.class, DataSerializers.VARINT);
    /** Held where something else puts it (a fist, a cage, a freeze): its pilot cannot steer it out. */
    private static final DataParameter<Boolean> HELD = EntityDataManager.createKey(EntityDestroyerArmor.class, DataSerializers.BOOLEAN);
    /** Flying by its pilot's own power (a stone's flight), not on its jets. */
    private static final DataParameter<Boolean> SOARING = EntityDataManager.createKey(EntityDestroyerArmor.class, DataSerializers.BOOLEAN);
    /** The stones in its sockets, by their items' registry names in socket order, for the powers they lend its pilot. */
    private static final DataParameter<String> STONE_KINDS = EntityDataManager.createKey(EntityDestroyerArmor.class, DataSerializers.STRING);
    /** How far each of its parts is overcharged, by {@link Overcharge#key}. */
    private static final DataParameter<NBTTagCompound> SURGE = EntityDataManager.createKey(EntityDestroyerArmor.class, DataSerializers.COMPOUND_TAG);
    /** How hot its core runs with its overcharge, and how long it has stood at its limit. */
    private static final DataParameter<Float> CORE = EntityDataManager.createKey(EntityDestroyerArmor.class, DataSerializers.FLOAT);
    private static final DataParameter<Integer> BREACH = EntityDataManager.createKey(EntityDestroyerArmor.class, DataSerializers.VARINT);
    /** What everything feeding its dynamo could give it in a tick, measured now and then. */
    private static final DataParameter<Integer> SUPPLY = EntityDataManager.createKey(EntityDestroyerArmor.class, DataSerializers.VARINT);
    /** Its core held too hot too long: it is tearing itself apart. */
    private static final DataParameter<Boolean> SCORCHING = EntityDataManager.createKey(EntityDestroyerArmor.class, DataSerializers.BOOLEAN);
    /** What an overheated core does to its own frame. */
    private static final DamageSource OVERHEAT = new DamageSource("eternity.overheat").setDamageBypassesArmor();
    /** The Astral Regulator in its chest, for everyone to see. */
    private static final DataParameter<ItemStack> REGULATOR = EntityDataManager.createKey(EntityDestroyerArmor.class, DataSerializers.ITEM_STACK);

    private final NonNullList<ItemStack> nothing = NonNullList.withSize(4, ItemStack.EMPTY);
    private final InventoryBasic inventory = new InventoryBasic("The Destroyer Armor", false, SLOTS);

    // ------------------------------------------------------------------ server state
    private int stateTicks;
    private boolean attackHeld, fireHeld, slamQueued, jumpHeld;
    /** The last time its pilot was told something was offline, so they are not told every tick. */
    private long lastWarned;
    private boolean wasOnGround = true;
    private Vec3d upkeepAt = Vec3d.ZERO;
    /** Letting its pilot out: the one time a dismount is allowed through. */
    private boolean releasing;
    private double lastX, lastZ, stride;
    private boolean leftFoot;
    private final Map<Integer, Integer> trampled = new HashMap<>();
    /** Ticks its core has run too hot, toward {@link Overcharge#SCORCH_GRACE}. */
    private int hotTicks;

    // ------------------------------------------------------------------ client state
    /** Client: ticks since the synced state last changed. */
    public int clientStateTicks;
    /** Client: how hot the furnace burns, how far its face plates and its back stand open. */
    public float heatGlow, prevHeatGlow, open, prevOpen, back, prevBack;
    /** Client: how hard it was last jolted by something bursting inside it, which its body twitches with. */
    public float jolt;
    /** Client: the pose it was in when its state last changed, which the model blends out of; and the pose last drawn. */
    public final float[] poseFrom = new float[40], poseLast = new float[40];
    /** Client: what its pilot is pressing, for steering it. Set every tick by whoever is inside. */
    public boolean clientJump, clientSprint, clientDescend;
    private int jumpCooldown;
    /** Client: ticks it has been flying by its pilot's own power. */
    private int soaredFor;
    /** Server: ticks it is still held where it was last put. Client: ticks it still tumbles from a blow, past steering. */
    private int heldTicks, flung;
    /** The size it was last made, against its pilot's. */
    private float sized = 1F;

    public EntityDestroyerArmor(World world) {
        super(world);
        setSize(1.6F, 4.3F);
        stepHeight = 1.6F;
        isImmuneToFire = true;
        ignoreFrustumCheck = true;
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(STATE, (byte) State.EMPTY.ordinal());
        dataManager.register(HEAT, 0F);
        dataManager.register(LAVA, 0F);
        dataManager.register(FORGED, 0);
        dataManager.register(STONES, (byte) 0);
        dataManager.register(FLYING, false);
        dataManager.register(GLIDING, false);
        dataManager.register(SKIN, (byte) 0);
        dataManager.register(SKINS, ArmorSkin.GUNMETAL.bit());
        dataManager.register(HELD, false);
        dataManager.register(SOARING, false);
        dataManager.register(STONE_KINDS, "");
        dataManager.register(REGULATOR, ItemStack.EMPTY);
        dataManager.register(SURGE, new NBTTagCompound());
        dataManager.register(CORE, 0F);
        dataManager.register(BREACH, 0);
        dataManager.register(SUPPLY, 0);
        dataManager.register(SCORCHING, false);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(EternityConfig.vault.armorHealth);
        getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(1D);
        getEntityAttribute(SharedMonsterAttributes.ARMOR).setBaseValue(20D);
        getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS).setBaseValue(12D);
    }

    @Override
    public void notifyDataManagerChange(DataParameter<?> key) {
        super.notifyDataManagerChange(key);
        if (STATE.equals(key) && world.isRemote) {
            clientStateTicks = 0;
            System.arraycopy(poseLast, 0, poseFrom, 0, poseLast.length);
        }
    }

    // ------------------------------------------------------------------ accessors

    public State getState() {
        int ordinal = dataManager.get(STATE);
        return ordinal >= 0 && ordinal < State.values().length ? State.values()[ordinal] : State.EMPTY;
    }

    private void setState(State state) {
        dataManager.set(STATE, (byte) state.ordinal());
        stateTicks = 0;
    }

    /** Ticks into its present state, on whichever side asks. */
    public int ticksInState() {
        return world.isRemote ? clientStateTicks : stateTicks;
    }

    /** How hot its furnace runs, 0 to {@link #HEAT_MAX}: the ray heats it, and at the top it must vent. */
    public float getFurnace() {
        return dataManager.get(HEAT);
    }

    private void setFurnace(float heat) {
        dataManager.set(HEAT, MathHelper.clamp(heat, 0F, HEAT_MAX));
    }

    /** How much lava its furnace holds, in millibuckets. */
    public float getLava() {
        return dataManager.get(LAVA);
    }

    public void setLava(float lava) {
        dataManager.set(LAVA, MathHelper.clamp(lava, 0F, lavaCapacity()));
    }

    public float lavaCapacity() {
        return (has(ArmorUpgrade.DEEP_FURNACE) ? 16F : 8F) * BUCKET;
    }

    /** Its furnace has gone out: nothing left to burn. */
    public boolean cold() {
        return getLava() <= 0F;
    }

    public Condition condition() {
        float f = getHealth() / getMaxHealth();
        return f >= 0.5F ? Condition.NOMINAL : f >= 0.25F ? Condition.STRAINED : f >= 0.1F ? Condition.FAILING : Condition.CRITICAL;
    }

    /** Which upgrades have been forged into it, one bit each. */
    public int forged() {
        return dataManager.get(FORGED);
    }

    public boolean has(ArmorUpgrade upgrade) {
        return upgrade.in(forged());
    }

    /** Which Infinity Stones sit in its chest, one bit each by {@link ArmorPowers} index; none without the cradle. */
    public int stones() {
        return has(ArmorUpgrade.CRADLE) ? dataManager.get(STONES) : 0;
    }

    public boolean holdsStone(int stone) {
        return (stones() & (1 << stone)) != 0;
    }

    /** All six, and the upgrade that makes them one. */
    public boolean infinite() {
        return has(ArmorUpgrade.INFINITE) && stones() == (1 << ArmorPowers.COUNT) - 1;
    }

    /** The stone in a socket, by {@link ArmorPowers} index, counted or not. */
    public ItemStack stone(int stone) {
        return inventory.getStackInSlot(SLOT_STONES + stone);
    }

    /** The stones it holds, by their items' registry names in socket order, empty for none: what they lend its pilot. */
    public String stoneKinds() {
        return has(ArmorUpgrade.CRADLE) ? dataManager.get(STONE_KINDS) : "";
    }

    /** Its snap turned on itself: every stone in it spent and gone. */
    public void spendStones() {
        for (int i = 0; i < ArmorPowers.COUNT; i++) {
            inventory.setInventorySlotContents(SLOT_STONES + i, ItemStack.EMPTY);
        }
        tend();
    }

    /**
     * The Astral Regulator in its chest, live for whoever is inside: counted as theirs (see
     * {@link ItemAstralRegulator#slotted}). On the server, the stack itself, so whatever it drinks in is kept.
     */
    public ItemStack lentRegulator(@Nullable Entity rider) {
        if (rider == null || rider != pilot() || !has(ArmorUpgrade.CRADLE) || getState() == State.IMPLODE) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = world.isRemote ? dataManager.get(REGULATOR) : inventory.getStackInSlot(SLOT_REGULATOR);
        return stack.getItem() instanceof ItemAstralRegulator ? stack : ItemStack.EMPTY;
    }

    /** What the Regulator in its chest has drunk in, for the powers it lends; empty for none. */
    public String regulatorKinds() {
        ItemStack stack = has(ArmorUpgrade.CRADLE) ? regulator() : ItemStack.EMPTY;
        return stack.getItem() instanceof ItemAstralRegulator && ItemAstralRegulator.isCharged(stack) ? String.join(",", ItemAstralRegulator.absorbed(stack)) : "";
    }

    /** The Astral Regulator in its chest, or nothing. */
    public ItemStack regulator() {
        return dataManager.get(REGULATOR);
    }

    /** Flying, on its jets or by its pilot's own power. */
    public boolean isFlying() {
        return dataManager.get(FLYING) || soaring();
    }

    /**
     * Flying by its pilot's own power, a stone's flight: it costs it nothing. Its pilot's side does not wait to be
     * told, since it steers there.
     */
    public boolean soaring() {
        return dataManager.get(SOARING) || world.isRemote && hidesPilot() && StoneAbilities.flying(pilot());
    }

    /** It runs on Forge Energy, its furnace given up for its dynamo. */
    public boolean electric() {
        return has(ArmorUpgrade.DYNAMO);
    }

    // ------------------------------------------------------------------ its overcharge

    /** A step that turns a part all the way down instead. */
    public static final int CUT = Integer.MIN_VALUE;

    /** Its dynamo can be overcharged: the Overcharge forged, and a dynamo to overcharge. */
    public boolean canOvercharge() {
        return has(ArmorUpgrade.OVERCHARGE) && electric();
    }

    /** How far a part is overcharged, 0 as built. */
    public int surge(Overcharge part) {
        return canOvercharge() ? MathHelper.clamp(dataManager.get(SURGE).getInteger(part.key), 0, Overcharge.MAX) : 0;
    }

    /** Every level held, across all its parts. */
    public int surgeTotal() {
        int total = 0;
        for (Overcharge part : Overcharge.values()) {
            total += surge(part);
        }
        return total;
    }

    /** What holding them draws, in Forge Energy a tick, while someone is inside. */
    public int surgeDraw() {
        long fe = 0L;
        for (Overcharge part : Overcharge.values()) {
            fe += Overcharge.upkeep(surge(part));
        }
        return (int) Math.min(Integer.MAX_VALUE, fe);
    }

    /** Its overcharge alive: someone inside it, and something turned up. */
    public boolean surging() {
        return hidesPilot() && isManned() && surgeTotal() > 0;
    }

    /** How hot its core runs, 0 to {@link Overcharge#CORE_MAX}: only its overcharge heats it. */
    public float core() {
        return dataManager.get(CORE);
    }

    /** Ticks its core has stood at its limit: at {@link Overcharge#BREACH_TICKS} it breaches. */
    public int breach() {
        return dataManager.get(BREACH);
    }

    /** Its core held too hot too long ({@link Overcharge#SCORCH_AT}), and its integrity failing for it. */
    public boolean scorching() {
        return dataManager.get(SCORCHING);
    }

    /** What everything feeding its dynamo could give it in a tick. */
    public int supply() {
        return dataManager.get(SUPPLY);
    }

    /**
     * A part of it turned up or down a step, or with {@link #CUT} all the way down; every part at once for none.
     * Whoever is inside is told where it stands.
     */
    public void surge(EntityPlayer by, @Nullable Overcharge part, int step) {
        if (!canOvercharge()) {
            by.sendStatusMessage(new TextComponentString(TextFormatting.GRAY + (has(ArmorUpgrade.DYNAMO)
                    ? "Its dynamo cannot be overcharged yet: forge the Overcharge." : "It has no dynamo to overcharge.")), true);
            return;
        }
        NBTTagCompound levels = dataManager.get(SURGE).copy();
        int before = surgeTotal();
        for (Overcharge each : Overcharge.values()) {
            if (part == null || each == part) {
                long to = step == CUT ? 0L : (long) levels.getInteger(each.key) + step;
                levels.setInteger(each.key, (int) Math.max(0L, Math.min(Overcharge.MAX, to)));
            }
        }
        dataManager.set(SURGE, levels);
        int after = surgeTotal();
        if (after != before) {
            sound(after > before ? SoundEvents.BLOCK_NOTE_PLING : SoundEvents.BLOCK_FIRE_EXTINGUISH, 0.8F,
                    after == 0 ? 0.5F : 0.5F + Math.min(1.5F, after / 120F));
        }
        if (by != pilot()) {
            return;
        }
        String draw = ItemAstralRegulator.formatEnergy(surgeDraw()) + " of " + ItemAstralRegulator.formatEnergy(supply()) + " FE/t";
        if (part == null || after == 0) {
            by.sendStatusMessage(new TextComponentString(TextFormatting.GRAY + "Overcharge cut: every part as built."), true);
        } else {
            int level = surge(part);
            by.sendStatusMessage(new TextComponentString(String.format("%s%s %d%s: %s  -  drawing %s", level > 0 ? TextFormatting.RED : TextFormatting.GRAY,
                    part.title.toUpperCase(), level, TextFormatting.GRAY, part.effect(level), draw)), true);
        }
    }

    /**
     * Its overcharge, every tick: what it draws paid out of its store, and its core heating with how much is held,
     * shedding a share of its heat as it goes. Its store dry and drawing more than it makes, it starves, and its core
     * runs away. Held at its limit too long, it breaches.
     */
    private void overdrive(@Nullable EntityPlayer pilot) {
        if (ticksExisted % 10 == 0) {
            int measured = electric() ? measure() : 0;
            if (measured != supply()) {
                dataManager.set(SUPPLY, measured);
            }
        }
        float heating = 0F;
        if (pilot != null && surging()) {
            int total = surgeTotal(), demand = surgeDraw();
            heating = Overcharge.heating(total);
            int paid = Math.min(energy(), demand);
            setLava(getLava() - paid / FE_PER_MB);
            if (paid < demand) {
                heating += 0.4F + 1.6F * (demand - paid) / (float) demand;
            }
        }
        float shed = coreShed();
        float core = core() + heating - core() * shed - (heating <= 0F ? 0.05F : 0F);
        core = MathHelper.clamp(core, 0F, Overcharge.CORE_MAX);
        if (Math.abs(core - core()) > 0.001F) {
            dataManager.set(CORE, core);
        }
        scorch(pilot, core);
        if (isDead || getState() == State.DOWN) {
            if (breach() > 0) {
                dataManager.set(BREACH, 0);
            }
            return;
        }
        int breach = core >= Overcharge.CORE_MAX - 0.01F && heating > 0F ? breach() + 1 : Math.max(0, breach() - 4);
        if (breach != breach()) {
            dataManager.set(BREACH, breach);
        }
        if (breach >= Overcharge.BREACH_TICKS) {
            implode(pilot);
            return;
        }
        // It tells of it: hissing as it heats, an alarm while it breaches.
        if (core > 50F && ticksExisted % (core > 85F ? 8 : 24) == 0) {
            sound(SoundEvents.BLOCK_FIRE_EXTINGUISH, 0.4F + core / 200F, 0.6F + core / 200F);
        }
        if (breach > 0 && ticksExisted % 10 == 0) {
            sound(SoundEvents.BLOCK_NOTE_PLING, 2F, 2F);
            sound(SoundEvents.BLOCK_NOTE_BASS, 2F, 0.5F);
        }
        if (breach == 1) {
            sound(SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, 2F, 0.6F);
            if (pilot != null) {
                pilot.sendMessage(new TextComponentString(TextFormatting.RED + "" + TextFormatting.BOLD + "CORE BREACH. "
                        + TextFormatting.RESET + TextFormatting.RED + "Turn the overcharge down, or it implodes in "
                        + Overcharge.BREACH_TICKS / 20 + " seconds."));
            }
        }
    }

    /**
     * Its core run too hot for too long tears at its own frame: a little integrity every second, more the hotter it
     * runs, until it is cooled below {@link Overcharge#SCORCH_AT}. Beaten to nothing this way, it goes down like any other.
     */
    private void scorch(@Nullable EntityPlayer pilot, float core) {
        hotTicks = core >= Overcharge.SCORCH_AT ? hotTicks + 1 : Math.max(0, hotTicks - 2);
        boolean scorching = hotTicks > Overcharge.SCORCH_GRACE && getState() != State.DOWN;
        if (scorching != scorching()) {
            dataManager.set(SCORCHING, scorching);
            if (scorching && pilot != null) {
                pilot.sendMessage(new TextComponentString(TextFormatting.GOLD + "Its core has run too hot for too long: it is tearing itself apart. "
                        + "Turn the overcharge down to below " + (int) Overcharge.SCORCH_AT + "% heat."));
            }
        }
        if (scorching && ticksExisted % 20 == 0) {
            float share = 0.01F + 0.03F * (core - Overcharge.SCORCH_AT) / (Overcharge.CORE_MAX - Overcharge.SCORCH_AT);
            hurtResistantTime = 0;
            attackEntityFrom(OVERHEAT, getMaxHealth() * share);
            sound(SoundEvents.BLOCK_LAVA_EXTINGUISH, 1F, 0.6F);
        }
    }

    /** The share of its heat its core sheds a tick: more with its Bellows, and the Time Stone. */
    public float coreShed() {
        return Overcharge.SHED * (has(ArmorUpgrade.BELLOWS) ? 2F : 1F) * ArmorPowers.cooling(this);
    }

    /** What everything feeding its dynamo could give it in a tick, as {@link #power} would draw it. */
    private int measure() {
        long fe = 0L;
        ItemStack in = inventory.getStackInSlot(SLOT_FUEL);
        IEnergyStorage cell = in.isEmpty() ? null : in.getCapability(CapabilityEnergy.ENERGY, null);
        if (cell != null && cell.canExtract()) {
            fe += cell.extractEnergy(DRAW, true);
        }
        ItemStack regulator = inventory.getStackInSlot(SLOT_REGULATOR);
        IEnergyStorage heart = regulator.isEmpty() ? null : regulator.getCapability(CapabilityEnergy.ENERGY, null);
        if (heart != null) {
            fe += heart.extractEnergy(DRAW, true);
        }
        for (int i = 0; i < ArmorPowers.COUNT; i++) {
            ItemStack stone = inventory.getStackInSlot(SLOT_STONES + i);
            if (holdsStone(i) && stone.getItem() instanceof ItemInfinityStone) {
                fe += ((ItemInfinityStone) stone.getItem()).getEnergyPerTick(stone);
            }
        }
        return (int) Math.min(fe, energyCapacity());
    }

    /**
     * Its core breached: whoever is inside is thrown clear, and it falls in on itself, pulling everything near in with
     * it, until it is gone (see {@link #collapse}). Nothing mends it after.
     */
    private void implode(@Nullable EntityPlayer pilot) {
        if (getState() == State.IMPLODE) {
            return;
        }
        if (pilot != null) {
            release(pilot);
            Vec3d away = facing().scale(-1.4D);
            pilot.motionX = away.x;
            pilot.motionY = 0.7D;
            pilot.motionZ = away.z;
            pilot.velocityChanged = true;
            pilot.sendStatusMessage(new TextComponentString(TextFormatting.RED + "Its core breaches. Get clear!"), true);
        }
        setState(State.IMPLODE);
        attackHeld = fireHeld = slamQueued = jumpHeld = false;
        dataManager.set(FLYING, false);
        dataManager.set(CORE, Overcharge.CORE_MAX);
        sound(SoundEvents.BLOCK_PORTAL_TRIGGER, 3F, 0.5F);
        sound(SoundEvents.ENTITY_WITHER_SPAWN, 2F, 1.6F);
        EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.DESTROYER_IMPLODE, posX, posY + 2.2D * sized, posZ, sized, 0D, 0D,
                getEntityId(), IMPLODE_TICKS + 30), dimension);
    }

    /** Falling in: everything near it dragged toward it. */
    private void imploding(int t) {
        motionX = motionY = motionZ = 0D;
        Vec3d c = new Vec3d(posX, posY + 2.2D * sized, posZ);
        double reach = 10D * sized;
        for (Entity near : world.getEntitiesWithinAABBExcludingEntity(this, getEntityBoundingBox().grow(reach))) {
            Vec3d to = c.subtract(near.posX, near.posY + near.height * 0.5D, near.posZ);
            double d = Math.sqrt(to.lengthSquared());
            if (d < 0.5D || d > reach || near instanceof EntityPlayer && ((EntityPlayer) near).capabilities.isFlying) {
                continue;
            }
            Vec3d pull = to.normalize().scale(0.12D * (1D - d / reach) * (0.5D + t / (double) IMPLODE_TICKS));
            near.motionX += pull.x;
            near.motionY += pull.y + 0.02D;
            near.motionZ += pull.z;
            near.velocityChanged = true;
        }
        if (t % 4 == 0) {
            sound(SoundEvents.BLOCK_ANVIL_LAND, 2F, 0.3F + t * 0.03F);
            sound(SoundEvents.ENTITY_IRONGOLEM_HURT, 2F, 0.3F);
        }
        if (t >= IMPLODE_TICKS) {
            collapse();
        }
    }

    /**
     * Gone: a crack of force where it stood, hurting everything near, and all it held thrown out. The stones and the
     * Astral Regulator cannot be harmed, and do not fade.
     */
    private void collapse() {
        Vec3d c = new Vec3d(posX, posY + 2.2D * sized, posZ);
        world.newExplosion(this, c.x, c.y, c.z, 3F, false, mayChange(getPosition()));
        double reach = 8D * sized;
        DamageSource blast = new DamageSource("eternity.implosion").setExplosion().setDamageBypassesArmor();
        for (EntityLivingBase near : world.getEntitiesWithinAABB(EntityLivingBase.class, getEntityBoundingBox().grow(reach))) {
            double d = near.getDistance(c.x, c.y, c.z);
            if (near != this && d < reach) {
                near.attackEntityFrom(blast, (float) (24D * (1D - d / reach)));
            }
        }
        sound(SoundEvents.ENTITY_GENERIC_EXPLODE, 4F, 0.4F);
        sound(SoundEvents.ENTITY_LIGHTNING_THUNDER, 3F, 0.6F);
        sound(SoundEvents.ENTITY_ENDERDRAGON_GROWL, 2F, 1.6F);
        boolean safe = false;
        for (int i = 0; i < SLOTS; i++) {
            ItemStack stack = inventory.removeStackFromSlot(i);
            if (stack.isEmpty()) {
                continue;
            }
            EntityItem item = new EntityItem(world, c.x, c.y, c.z, stack);
            double a = rand.nextDouble() * Math.PI * 2D;
            item.motionX = Math.cos(a) * 0.25D;
            item.motionY = 0.35D + rand.nextDouble() * 0.2D;
            item.motionZ = Math.sin(a) * 0.25D;
            item.setPickupDelay(30);
            if (i == SLOT_REGULATOR || i >= SLOT_STONES && i < SLOT_STONES + ArmorPowers.COUNT) {
                item.setEntityInvulnerable(true);
                item.setNoDespawn();
                safe = true;
            }
            world.spawnEntity(item);
        }
        String said = "The Destroyer Armor implodes, and is gone." + (safe ? " What was in its chest survives it." : "");
        for (EntityPlayer near : world.getEntitiesWithinAABB(EntityPlayer.class, getEntityBoundingBox().grow(48D))) {
            near.sendMessage(new TextComponentString(TextFormatting.GRAY + said));
        }
        setDead();
    }

    /** Forge Energy stored, with its dynamo: what it would otherwise hold in lava. */
    public int energy() {
        return (int) (getLava() * FE_PER_MB);
    }

    public int energyCapacity() {
        return (int) (lavaCapacity() * FE_PER_MB);
    }

    /** Forge Energy into its dynamo; gives back how much it took. */
    private int charge(int fe) {
        int took = Math.max(0, Math.min(fe, energyCapacity() - energy()));
        if (took > 0) {
            setLava(getLava() + took / FE_PER_MB);
        }
        return took;
    }

    /** How big it is: as big as its pilot has been made, against the size they were. */
    public float scale() {
        EntityPlayer pilot = pilot();
        if (pilot == null || !pilot.hasCapability(CapabilitySizeChanging.SIZE_CHANGING_CAP, null)) {
            return 1F;
        }
        return MathHelper.clamp(pilot.getCapability(CapabilitySizeChanging.SIZE_CHANGING_CAP, null).getSize(), 0.05F, 16F);
    }

    public InventoryBasic getInventory() {
        return inventory;
    }

    /** How hard everything it strikes lands: the Power Stone, and all six together. */
    float might() {
        float m = 1F;
        if (holdsStone(ArmorPowers.POWER)) {
            m *= has(ArmorUpgrade.CONDUITS) ? 2F : 1.5F;
        }
        if (infinite()) {
            m *= 1.5F;
        }
        return m;
    }

    /** Its frame, as forged: how much integrity it holds and how much its armour turns. */
    private void refit() {
        double scale = 1D + (has(ArmorUpgrade.URU_PLATING) ? 0.3D : 0D) + (has(ArmorUpgrade.BANDED_HULL) ? 0.3D : 0D);
        float health = getHealth();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(EternityConfig.vault.armorHealth * scale);
        getEntityAttribute(SharedMonsterAttributes.ARMOR).setBaseValue(has(ArmorUpgrade.BANDED_HULL) ? 28D : 20D);
        getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS).setBaseValue(has(ArmorUpgrade.BANDED_HULL) ? 16D : 12D);
        // Forging more frame onto it adds room for integrity, not integrity: that still has to be mended in.
        setHealth(Math.min(health, getMaxHealth()));
    }

    @Nullable
    public EntityPlayer pilot() {
        Entity passenger = getControllingPassenger();
        return passenger instanceof EntityPlayer ? (EntityPlayer) passenger : null;
    }

    /** Whoever is inside is out of sight: from just after they are lifted in until just before they are set down. */
    public boolean hidesPilot() {
        int t = ticksInState();
        switch (getState()) {
            case ENTER:
                return t > ENTER_STEP + 3;
            case EXIT:
                return t < EXIT_OUT - 6;
            case EMPTY:
            case RISE:
            case DOWN:
            case IMPLODE:
                return false;
            default:
                return true;
        }
    }

    /** Fully awake, someone inside and in command of it. */
    public boolean isManned() {
        State state = getState();
        return state != State.EMPTY && state != State.RISE && state != State.ENTER && state != State.EXIT && state != State.DOWN && state != State.IMPLODE;
    }

    public boolean isReleasing() {
        return releasing;
    }

    /** Stands it up out of the wreck of the vault's Destroyer, empty. */
    public void rise(double x, double y, double z, float yaw, float health) {
        setLocationAndAngles(x, y, z, yaw, 0F);
        rotationYawHead = renderYawOffset = prevRenderYawOffset = yaw;
        setHealth(health);
        setState(State.RISE);
    }

    // ------------------------------------------------------------------ getting in and out

    @Override
    public boolean processInitialInteract(EntityPlayer player, EnumHand hand) {
        if (isPassenger(player) || getState() == State.IMPLODE) {
            return false;
        }
        ItemStack held = player.getHeldItem(hand);
        float mend = mending(held);
        State state = getState();
        boolean idle = state == State.EMPTY || state == State.DOWN;
        if (mend > 0F && getHealth() < getMaxHealth() && idle) {
            if (!world.isRemote) {
                mend(mend);
                if (!player.capabilities.isCreativeMode) {
                    held.shrink(1);
                }
                player.sendStatusMessage(new TextComponentString(String.format("The Destroyer Armor: %.0f%% integrity", 100F * getHealth() / getMaxHealth())), true);
            }
            return true;
        }
        if (idle && !electric() && getLava() < lavaCapacity() && lavaIn(held) > 0F) {
            if (!world.isRemote) {
                ItemStack emptied = pour(held.copy());
                if (!player.capabilities.isCreativeMode) {
                    held.shrink(1);
                    if (!emptied.isEmpty() && !player.inventory.addItemStackToInventory(emptied)) {
                        player.dropItem(emptied, false);
                    }
                }
                player.sendStatusMessage(new TextComponentString(String.format("The Destroyer Armor: %.1f of %.0f buckets of lava",
                        getLava() / BUCKET, lavaCapacity() / BUCKET)), true);
            }
            return true;
        }
        if (hand != EnumHand.MAIN_HAND) {
            return false;
        }
        // Sneaking, a hand on it from outside: its systems, to mend it, fill it, and forge it.
        if (idle && player.isSneaking() && !isBeingRidden()) {
            if (!world.isRemote) {
                openMenu(player);
            }
            return true;
        }
        if (state == State.DOWN) {
            if (!world.isRemote) {
                player.sendStatusMessage(new TextComponentString(TextFormatting.GRAY + "It is too broken to stand. Uru will mend it."), true);
            }
            return true;
        }
        if (state != State.EMPTY || isBeingRidden() || player.isSneaking() || player.isRiding()) {
            return false;
        }
        if (!world.isRemote) {
            setState(State.ENTER);
            player.startRiding(this, true);
            sound(SoundEvents.ITEM_ARMOR_EQUIP_IRON, 1.5F, 0.5F);
        }
        return true;
    }

    /** How much of its base integrity a plate of Uru mends: Lucraft's Uru plate, hammered from an ingot. Nothing else will. */
    public static final float PLATE = 0.1F;

    public static float mending(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0F;
        }
        for (int id : OreDictionary.getOreIDs(stack)) {
            if ("plateUru".equals(OreDictionary.getOreName(id))) {
                return PLATE;
            }
        }
        return 0F;
    }

    /** Uru worked into it, a share of its base integrity's worth; broken, it stands again once a quarter mended. */
    private void mend(float share) {
        heal((float) EternityConfig.vault.armorHealth * share);
        sound(SoundEvents.BLOCK_ANVIL_USE, 1F, 0.6F + rand.nextFloat() * 0.2F);
        // Sparks where the Uru is worked in, at its waist and out of its pilot's sight.
        Vec3d at = getPositionVector().add(facing().scale(0.9D));
        EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.DESTROYER_IMPACT, at.x, posY + 1.3D, at.z, 0D, 0.3D, 0D, -1, 0), dimension);
        if (getState() == State.DOWN && getHealth() >= getMaxHealth() * 0.25F) {
            setState(State.EMPTY);
            sound(SoundEvents.BLOCK_PISTON_EXTEND, 2F, 0.4F);
        }
    }

    /** How much lava an item would pour into it. */
    public static float lavaIn(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0F;
        }
        FluidStack fluid = FluidUtil.getFluidContained(stack);
        return fluid != null && fluid.getFluid() == FluidRegistry.LAVA ? fluid.amount : 0F;
    }

    /** Pours one container of lava into its furnace, as much as it has room for; gives back what is left of the container. */
    private ItemStack pour(ItemStack container) {
        container.setCount(1);
        IFluidHandlerItem handler = FluidUtil.getFluidHandler(container);
        if (handler == null) {
            return container;
        }
        int room = (int) (lavaCapacity() - getLava());
        FluidStack drained = handler.drain(new FluidStack(FluidRegistry.LAVA, room), true);
        if (drained != null && drained.amount > 0) {
            setLava(getLava() + drained.amount);
            sound(SoundEvents.ITEM_BUCKET_EMPTY_LAVA, 1.5F, 0.7F);
        }
        return handler.getContainer();
    }

    /** What a container would be once poured out, without pouring it. */
    private ItemStack emptiedOf(ItemStack container) {
        ItemStack copy = container.copy();
        copy.setCount(1);
        IFluidHandlerItem handler = FluidUtil.getFluidHandler(copy);
        if (handler == null) {
            return copy;
        }
        handler.drain(new FluidStack(FluidRegistry.LAVA, (int) (lavaCapacity() - getLava())), true);
        return handler.getContainer();
    }

    public void openMenu(EntityPlayer player) {
        player.openGui(Eternity.instance, EternityGuis.ARMOR, world, getEntityId(), 0, 0);
    }

    /** Whether someone can work on it from where they stand. */
    public boolean reachableBy(EntityPlayer player) {
        return !isDead && (isPassenger(player) || player.getDistanceSq(this) < 64D);
    }

    /** Forges an upgrade into it, out of what the player carries, if nothing stands in the way. */
    public boolean forge(EntityPlayer player, ArmorUpgrade upgrade) {
        if (upgrade.blocker(forged(), stones()) != null || !upgrade.affordable(player)) {
            return false;
        }
        upgrade.pay(player);
        dataManager.set(FORGED, forged() | upgrade.bit());
        refit();
        sound(SoundEvents.BLOCK_ANVIL_USE, 2F, 0.5F);
        sound(SoundEvents.ENTITY_BLAZE_SHOOT, 1.5F, 0.4F);
        EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.DESTROYER_STEP, posX, posY, posZ, 0.3D, 0D, 0D, getEntityId(), 0), dimension);
        player.sendStatusMessage(new TextComponentString(TextFormatting.GOLD + upgrade.title + TextFormatting.GRAY + " is forged into the Destroyer Armor."), true);
        return true;
    }

    /** The finish it is painted in. */
    public ArmorSkin skin() {
        return ArmorSkin.of(dataManager.get(SKIN));
    }

    public boolean ownsSkin(ArmorSkin skin) {
        return (dataManager.get(SKINS) & skin.bit()) != 0;
    }

    /** Paints it in a finish: made first, out of the player's pack, if it has never been; then simply put on. */
    public boolean paint(EntityPlayer player, ArmorSkin skin) {
        if (!ownsSkin(skin)) {
            if (!ArmorUpgrade.affordable(player, skin.costs)) {
                return false;
            }
            ArmorUpgrade.pay(player, skin.costs);
            dataManager.set(SKINS, dataManager.get(SKINS) | skin.bit());
            player.sendStatusMessage(new TextComponentString(TextFormatting.GOLD + skin.title + TextFormatting.GRAY
                    + " is made for the Destroyer Armor."), true);
        }
        if (skin() != skin) {
            dataManager.set(SKIN, (byte) skin.ordinal());
            sound(SoundEvents.ENTITY_ITEMFRAME_ADD_ITEM, 1.5F, 0.6F);
            sound(SoundEvents.BLOCK_ANVIL_USE, 0.8F, 1.4F);
        }
        return true;
    }

    /** Gliding down on its leap jets. */
    public boolean isGliding() {
        return dataManager.get(GLIDING);
    }

    /** Sets outright which upgrades are forged into it, for commands. */
    public void setForged(int mask) {
        dataManager.set(FORGED, mask & ((1 << ArmorUpgrade.values().length) - 1));
        refit();
    }

    /**
     * What it does with its own inventory: lava poured in, or with its dynamo Forge Energy drawn from whatever holds it,
     * from the Regulator in its chest and from every stone set there; Uru plate worked in; which stones sit in its chest.
     */
    private void tend() {
        if (electric()) {
            power();
        } else {
            pourIn();
        }
        ItemStack uru = inventory.getStackInSlot(SLOT_URU);
        float share = mending(uru);
        float missing = (getMaxHealth() - getHealth()) / (float) EternityConfig.vault.armorHealth;
        if (share > 0F && missing > 0F && ticksExisted % 10 == 0) {
            mend(share);
            uru.shrink(1);
            inventory.markDirty();
        }
        int mask = 0;
        StringBuilder kinds = new StringBuilder();
        for (int i = 0; i < ArmorPowers.COUNT; i++) {
            ItemStack stone = inventory.getStackInSlot(SLOT_STONES + i);
            boolean counted = ArmorPowers.stoneOf(stone) == i && Relics.recognized(stone, false);
            if (counted) {
                mask |= 1 << i;
            }
            if (i > 0) {
                kinds.append(',');
            }
            if (counted && stone.getItem().getRegistryName() != null) {
                kinds.append(stone.getItem().getRegistryName());
            }
        }
        if (mask != dataManager.get(STONES)) {
            dataManager.set(STONES, (byte) mask);
        }
        String set = mask == 0 ? "" : kinds.toString();
        if (!set.equals(dataManager.get(STONE_KINDS))) {
            dataManager.set(STONE_KINDS, set);
        }
        ItemStack regulator = inventory.getStackInSlot(SLOT_REGULATOR);
        if (!ItemStack.areItemStacksEqual(regulator, dataManager.get(REGULATOR))) {
            dataManager.set(REGULATOR, regulator.copy());
        }
    }

    /** Lava poured into its furnace from whatever holds it, a container every few ticks, the emptied ones set aside. */
    private void pourIn() {
        ItemStack in = inventory.getStackInSlot(SLOT_FUEL);
        if (!in.isEmpty() && getLava() + Math.min(lavaIn(in), BUCKET) <= lavaCapacity() + 0.5F && ticksExisted % 5 == 0) {
            ItemStack out = inventory.getStackInSlot(SLOT_SPENT);
            ItemStack would = emptiedOf(in);
            if (would.isEmpty() || out.isEmpty() || ItemStack.areItemsEqual(out, would) && ItemStack.areItemStackTagsEqual(out, would)
                    && out.getCount() < out.getMaxStackSize()) {
                ItemStack emptied = pour(in.copy());
                in.shrink(1);
                if (!emptied.isEmpty()) {
                    if (out.isEmpty()) {
                        inventory.setInventorySlotContents(SLOT_SPENT, emptied);
                    } else {
                        out.grow(1);
                    }
                }
                inventory.markDirty();
            }
        }
    }

    /**
     * Its dynamo drinking: from a battery or anything else holding Forge Energy (set aside once it is drained), from
     * the Astral Regulator in its chest, which never runs dry, and from each stone set there, which give what they
     * would give an Infinity Generator.
     */
    private void power() {
        ItemStack in = inventory.getStackInSlot(SLOT_FUEL);
        IEnergyStorage cell = in.isEmpty() ? null : in.getCapability(CapabilityEnergy.ENERGY, null);
        if (cell != null && cell.canExtract()) {
            int room = energyCapacity() - energy();
            if (room > 0) {
                charge(cell.extractEnergy(Math.min(room, DRAW), false));
            }
            boolean drained = cell.getEnergyStored() <= 0 && cell.extractEnergy(1, true) <= 0;
            if (drained && inventory.getStackInSlot(SLOT_SPENT).isEmpty()) {
                inventory.setInventorySlotContents(SLOT_SPENT, in);
                inventory.setInventorySlotContents(SLOT_FUEL, ItemStack.EMPTY);
            }
        }
        ItemStack regulator = inventory.getStackInSlot(SLOT_REGULATOR);
        IEnergyStorage heart = regulator.isEmpty() ? null : regulator.getCapability(CapabilityEnergy.ENERGY, null);
        if (heart != null && energy() < energyCapacity()) {
            charge(heart.extractEnergy(Math.min(energyCapacity() - energy(), DRAW), false));
        }
        for (int i = 0; i < ArmorPowers.COUNT && energy() < energyCapacity(); i++) {
            ItemStack stone = inventory.getStackInSlot(SLOT_STONES + i);
            if (holdsStone(i) && stone.getItem() instanceof ItemInfinityStone) {
                charge(((ItemInfinityStone) stone.getItem()).getEnergyPerTick(stone));
            }
        }
    }

    /** Fuel burned, by however hungry it runs: lava, or with its dynamo the energy as much lava would give. All six stones in it, and it burns none. */
    private void burn(float millibuckets) {
        if (infinite() || millibuckets <= 0F) {
            return;
        }
        float rate = (float) EternityConfig.vault.armorLavaBurn * (has(ArmorUpgrade.BELLOWS) ? 0.65F : 1F);
        setLava(getLava() - millibuckets * rate);
    }

    /** Whether it has what something takes: lava, and integrity enough. Its pilot is told if not. */
    boolean able(EntityPlayer pilot, String what, boolean failsCritical) {
        String why = cold() ? (electric() ? "its dynamo is drained: it needs Forge Energy" : "its furnace is cold: it needs lava")
                : failsCritical && condition() == Condition.CRITICAL ? "its integrity is critical: it needs Uru plate" : null;
        if (why == null) {
            return true;
        }
        if (pilot != null && world.getTotalWorldTime() - lastWarned > 30L) {
            lastWarned = world.getTotalWorldTime();
            pilot.sendStatusMessage(new TextComponentString(TextFormatting.RED + what + " is offline: " + why + "."), true);
        }
        return false;
    }

    @Override
    protected boolean canBeRidden(Entity entity) {
        return entity instanceof EntityPlayer;
    }

    @Override
    protected boolean canFitPassenger(Entity passenger) {
        return getPassengers().isEmpty();
    }

    @Nullable
    @Override
    public Entity getControllingPassenger() {
        return getPassengers().isEmpty() ? null : getPassengers().get(0);
    }

    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        // Someone coming back to it: they logged out inside, and the world kept them there.
        if (!world.isRemote && getState() != State.ENTER) {
            setState(State.READY);
        }
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (!world.isRemote && getState() != State.EXIT && getState() != State.DOWN && getState() != State.IMPLODE) {
            setState(State.EMPTY);
        }
        attackHeld = fireHeld = slamQueued = jumpHeld = false;
        if (!world.isRemote) {
            dataManager.set(FLYING, false);
            dataManager.set(SOARING, false);
        }
    }

    /** It never lets its pilot go on a whim: only {@link Command#EXIT} opens its back. */
    public boolean holds(Entity rider) {
        return !releasing && !isDead && isPassenger(rider) && rider.isEntityAlive() && getHealth() > 1F;
    }

    @Override
    public boolean shouldRiderSit() {
        return false;
    }

    @Override
    public boolean shouldDismountInWater(Entity rider) {
        return false;
    }

    @Override
    public double getMountedYOffset() {
        return seat()[1];
    }

    /** Where whoever is inside is, relative to it: how far behind its middle, and how high. */
    private double[] seat() {
        int t = ticksInState();
        double behind = -SEAT_FORWARD, up = SEAT_UP;
        switch (getState()) {
            case ENTER: {
                float in = smooth(t / (float) ENTER_STEP);
                float lift = smooth((t - ENTER_STEP) / (float) (ENTER_LIFT - ENTER_STEP));
                behind = BEHIND + (0.55D - BEHIND) * in + (-SEAT_FORWARD - 0.55D) * lift;
                up = 0.35D + (SEAT_UP - 0.35D) * lift;
                break;
            }
            case EXIT: {
                float lower = smooth((t - EXIT_OPEN) / (float) (EXIT_OUT - EXIT_OPEN - 6));
                float out = smooth((t - EXIT_OUT + 8) / 8F);
                behind = -SEAT_FORWARD + (0.55D + SEAT_FORWARD) * lower + (BEHIND - 0.55D) * out;
                up = SEAT_UP + (0.35D - SEAT_UP) * lower;
                break;
            }
            default:
                break;
        }
        float s = scale();
        return new double[]{behind * s, up * s};
    }

    @Override
    public void updatePassenger(Entity passenger) {
        if (!isPassenger(passenger)) {
            return;
        }
        double[] seat = seat();
        Vec3d f = facing();
        passenger.setPosition(posX - f.x * seat[0], posY + seat[1] + passenger.getYOffset(), posZ - f.z * seat[0]);
        // While it lifts them in, they are turned to look out of its face.
        if (getState() == State.ENTER && ticksInState() < ENTER_SHUT) {
            float turn = MathHelper.wrapDegrees(rotationYaw - passenger.rotationYaw);
            passenger.rotationYaw += turn * 0.25F;
            passenger.prevRotationYaw += turn * 0.25F;
            if (passenger instanceof EntityLivingBase) {
                ((EntityLivingBase) passenger).rotationYawHead = passenger.rotationYaw;
            }
        }
    }

    /** Lets its pilot go at once, where they are: for carrying them both somewhere else. */
    public void release(EntityPlayer pilot) {
        releasing = true;
        pilot.dismountRidingEntity();
        releasing = false;
    }

    /** Its back opens and it sets them down behind it. */
    private void letOut(EntityPlayer pilot) {
        release(pilot);
        Vec3d f = facing();
        double x = posX - f.x * BEHIND, z = posZ - f.z * BEHIND;
        AxisAlignedBB room = pilot.getEntityBoundingBox().offset(x - pilot.posX, posY - pilot.posY, z - pilot.posZ);
        if (world.getCollisionBoxes(pilot, room).isEmpty()) {
            pilot.setPositionAndUpdate(x, posY, z);
        }
        pilot.fallDistance = 0F;
    }

    // ------------------------------------------------------------------ what its pilot asks of it

    /** A command from whoever is inside. Anyone else is ignored. */
    public void command(EntityPlayer from, Command command, boolean down) {
        command(from, command, down, 0);
    }

    /** With a value: for its overcharge, which part (one more than its index, none for all) and how far, packed. */
    public void command(EntityPlayer from, Command command, boolean down, int value) {
        if (from != pilot()) {
            return;
        }
        switch (command) {
            case ATTACK:
                attackHeld = down;
                break;
            case FIRE:
                fireHeld = down;
                break;
            case SLAM:
                slamQueued = down;
                break;
            case EXIT:
                if (down && isManned() && getState() != State.SLAM && !isFlying()) {
                    setState(State.EXIT);
                    sound(SoundEvents.BLOCK_FIRE_EXTINGUISH, 1.5F, 0.5F);
                    sound(SoundEvents.BLOCK_PISTON_CONTRACT, 2F, 0.5F);
                }
                break;
            case MENU:
                if (down && hidesPilot()) {
                    openMenu(from);
                }
                break;
            case FLY:
                if (!down) {
                    dataManager.set(FLYING, false);
                } else if (has(ArmorUpgrade.ASCENSION) && isManned() && able(from, "Flight", true)) {
                    dataManager.set(FLYING, true);
                    sound(SoundEvents.ENTITY_BLAZE_SHOOT, 2F, 0.4F);
                    sound(SoundEvents.ENTITY_ENDERDRAGON_FLAP, 2F, 0.5F);
                }
                break;
            case JUMP:
                jumpHeld = down;
                break;
            case ABSORB:
                if (down && hidesPilot() && from instanceof EntityPlayerMP) {
                    absorb((EntityPlayerMP) from);
                }
                break;
            case SURGE: {
                int part = (value >> 16) - 1;
                short step = (short) value;
                if (part < Overcharge.values().length) {
                    surge(from, part < 0 ? null : Overcharge.values()[part], step == Short.MIN_VALUE ? CUT : step);
                }
                break;
            }
        }
    }

    /**
     * The Regulator in its chest raised at whatever its pilot looks at, as they would raise it in their hand: it
     * drinks in a beaten cosmic being, Eternity as he dies.
     */
    private void absorb(EntityPlayerMP pilot) {
        ItemStack stack = lentRegulator(pilot);
        if (stack.isEmpty()) {
            pilot.sendStatusMessage(new TextComponentString(TextFormatting.GRAY + (has(ArmorUpgrade.CRADLE)
                    ? "There is no Astral Regulator in its chest to raise." : "It has no cradle to hold an Astral Regulator: forge the Cradle.")), true);
            return;
        }
        if (ItemAstralRegulator.raise(pilot, stack, EnumHand.MAIN_HAND) == net.minecraft.util.EnumActionResult.SUCCESS) {
            sound(SoundEvents.BLOCK_PORTAL_TRIGGER, 1.5F, 1.4F);
            tend();
        } else {
            pilot.sendStatusMessage(new TextComponentString(TextFormatting.GRAY + "Nothing it looks at for the Regulator to drink in: a cosmic being, beaten."), true);
        }
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void onLivingUpdate() {
        if (!world.isRemote) {
            serverTick();
        } else if (jumpCooldown > 0) {
            jumpCooldown--;
        }
        super.onLivingUpdate();
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        // Its body follows its pilot's look, but slowly; the model turns its head the rest of the way.
        renderYawOffset = rotationYaw;
        prevRenderYawOffset = prevRotationYaw;
        EntityPlayer pilot = pilot();
        if (pilot != null) {
            // Whatever its pilot does by a power of their own (flight above all) goes by whether it stands on the ground;
            // a rider's own idea of that never changes.
            pilot.onGround = onGround;
        }
        // Made smaller or larger with its pilot (the Reality Stone), it is made so too.
        float s = scale();
        if (Math.abs(s - sized) > 0.01F) {
            sized = s;
            setSize(1.6F * s, 4.3F * s);
        }
        if (world.isRemote) {
            clientTick();
        }
    }

    private void serverTick() {
        stateTicks++;
        EntityPlayer pilot = pilot();
        State state = getState();
        int t = stateTicks;
        if (state != State.RAY) {
            float cooling = (has(ArmorUpgrade.BELLOWS) ? 2F : 1F) * ArmorPowers.cooling(this);
            setFurnace(getFurnace() - (state == State.VENT ? 1.4F : 0.5F) * cooling);
        }
        if (heldTicks > 0 && --heldTicks == 0) {
            dataManager.set(HELD, false);
        }
        if (pilot != null) {
            pilot.setAir(300);
            pilot.extinguish();
            pilot.fallDistance = 0F;
        }
        tend();
        upkeep(pilot);
        lend(pilot);
        overdrive(pilot);
        if (isDead || getState() == State.IMPLODE && state != State.IMPLODE) {
            return;
        }
        ArmorPowers.tick(this, pilot);
        switch (state) {
            case EMPTY:
                motionX = motionZ = 0D;
                break;
            case RISE:
                if (t == 10 || t == 34) {
                    sound(SoundEvents.BLOCK_ANVIL_LAND, 1.5F, 0.3F);
                    sound(SoundEvents.ENTITY_IRONGOLEM_STEP, 3F, 0.4F);
                }
                if (t >= RISE_TICKS) {
                    setState(State.EMPTY);
                }
                break;
            case ENTER:
                if (pilot == null) {
                    setState(State.EMPTY);
                    break;
                }
                if (t == ENTER_SHUT) {
                    sound(SoundEvents.BLOCK_IRON_DOOR_CLOSE, 2.5F, 0.4F);
                    sound(SoundEvents.BLOCK_ANVIL_LAND, 0.8F, 0.4F);
                }
                if (t == ENTER_SHUT + 6) {
                    sound(SoundEvents.ENTITY_BLAZE_AMBIENT, 2F, 0.4F);
                    sound(SoundEvents.BLOCK_FIRE_AMBIENT, 2F, 0.5F);
                }
                if (t >= ENTER_TICKS) {
                    setState(State.READY);
                }
                break;
            case EXIT:
                if (t == EXIT_OPEN) {
                    sound(SoundEvents.BLOCK_IRON_DOOR_OPEN, 2.5F, 0.4F);
                }
                if (t == EXIT_OUT && pilot != null) {
                    letOut(pilot);
                }
                if (t >= EXIT_TICKS) {
                    setState(State.EMPTY);
                }
                break;
            case READY:
                if (pilot == null) {
                    setState(State.EMPTY);
                    break;
                }
                choose();
                break;
            case PUNCH_R:
            case PUNCH_L:
                if (t == PUNCH_HIT - 3) {
                    sound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1.5F, 0.45F);
                }
                if (t == PUNCH_HIT && pilot != null) {
                    punch(pilot, state == State.PUNCH_R ? 0 : 1);
                }
                if (t >= PUNCH_TICKS) {
                    // Keep swinging, and it keeps punching, one fist and then the other.
                    if (attackHeld && pilot != null) {
                        setState(state == State.PUNCH_R ? State.PUNCH_L : State.PUNCH_R);
                    } else {
                        setState(State.READY);
                    }
                }
                break;
            case SLAM:
                if (t == 4) {
                    sound(SoundEvents.BLOCK_PISTON_EXTEND, 2F, 0.4F);
                }
                if (t == SLAM_HIT && pilot != null) {
                    slam(pilot);
                }
                if (t >= SLAM_TICKS) {
                    setState(State.READY);
                }
                break;
            case RAY:
                ray(pilot, t);
                break;
            case RAY_END:
                if (t == 2) {
                    sound(SoundEvents.BLOCK_FIRE_EXTINGUISH, 2F, 0.5F);
                }
                if (t == RAY_CLOSE - 2) {
                    sound(SoundEvents.BLOCK_IRON_TRAPDOOR_CLOSE, 2F, 0.4F);
                }
                if (t >= RAY_CLOSE) {
                    if (getFurnace() >= HEAT_MAX - 0.5F) {
                        setState(State.VENT);
                        sound(SoundEvents.BLOCK_LAVA_EXTINGUISH, 2F, 0.4F);
                    } else {
                        setState(State.READY);
                    }
                }
                break;
            case VENT:
                if (t % 20 == 0) {
                    sound(SoundEvents.BLOCK_FIRE_EXTINGUISH, 1.5F, 0.4F + rand.nextFloat() * 0.2F);
                }
                if (t >= (has(ArmorUpgrade.MOLTEN_HEART) ? VENT_TICKS / 2 : VENT_TICKS)) {
                    setState(pilot != null ? State.READY : State.EMPTY);
                }
                break;
            case DOWN:
                motionX = motionZ = 0D;
                break;
            case IMPLODE:
                imploding(t);
                return;
        }
        walk();
    }

    /**
     * What it burns and what it mends, every tick: its furnace ticking over with someone inside, faster as it
     * walks, charges, glides or flies; lava drunk from wherever it wades in it; Odin's wards mending it.
     */
    private void upkeep(@Nullable EntityPlayer pilot) {
        // Its pilot moves it between ticks, so measure from where it was last tick rather than from prevPos.
        double dx = posX - upkeepAt.x, dy = posY - upkeepAt.y, dz = posZ - upkeepAt.z;
        upkeepAt = getPositionVector();
        double moved = Math.sqrt(dx * dx + dz * dz);
        if (moved > 4D) {
            moved = dy = 0D;
        }
        if (isInLava() && !electric() && getLava() < lavaCapacity()) {
            setLava(getLava() + 25F);
            if (ticksExisted % 20 == 0) {
                sound(SoundEvents.BLOCK_LAVA_POP, 1F, 0.6F);
            }
        }
        if (pilot != null && hidesPilot()) {
            float use = 0.15F + (float) Math.min(1D, moved / 0.2D) * 0.35F;
            if (clientSprintSeen(moved)) {
                use += 0.6F;
            }
            if (isFlying()) {
                // On its jets it burns hard; on its pilot's own flight, nothing.
                if (!soaring()) {
                    use += 2.5F + (moved > 0.05D ? 1.5F : 0F);
                }
                fallDistance = 0F;
            } else if (!onGround && jumpHeld && has(ArmorUpgrade.LEAP_JETS) && dy <= 0D) {
                use += 1.2F;
                fallDistance = Math.min(fallDistance, 3F);
            }
            // A leap: it left the ground going up.
            if (wasOnGround && !onGround && dy > 0.25D && !soaring()) {
                use += 50F;
            }
            // Carried by its pilot's own flight it only ticks over, however far and fast it goes.
            burn(soaring() ? 0.15F : use);
        }
        wasOnGround = onGround;
        boolean glide = pilot != null && !isFlying() && !onGround && jumpHeld && has(ArmorUpgrade.LEAP_JETS) && !cold()
                && condition() != Condition.CRITICAL;
        if (glide != isGliding()) {
            dataManager.set(GLIDING, glide);
        }
        if (dataManager.get(FLYING) && (pilot == null || cold() || condition() == Condition.CRITICAL || !has(ArmorUpgrade.ASCENSION))) {
            dataManager.set(FLYING, false);
        }
        boolean wards = has(ArmorUpgrade.ODINS_WARDS);
        if ((wards || infinite()) && getHealth() < getMaxHealth() && ticksExisted % (infinite() ? 20 : 40) == 0) {
            if (infinite()) {
                heal(getMaxHealth() * 0.01F);
            } else if (getLava() >= 25F) {
                heal(getMaxHealth() * 0.01F);
                burn(25F);
            }
            if (getState() == State.DOWN && getHealth() >= getMaxHealth() * 0.25F) {
                setState(State.EMPTY);
                sound(SoundEvents.BLOCK_PISTON_EXTEND, 2F, 0.4F);
            }
        }
    }

    /**
     * What its pilot's powers do through it, every tick: their flight carries it, their armour plates it, more life
     * than they have is more integrity in it, and their healing mends it as it mends them, by the same share.
     */
    private void lend(@Nullable EntityPlayer pilot) {
        boolean inside = pilot != null && hidesPilot();
        ItemStack heart = inside ? lentRegulator(pilot) : ItemStack.EMPTY;
        if (!heart.isEmpty()) {
            heart.getItem().onUpdate(heart, world, pilot, -1, false);
        }
        boolean soar = inside && StoneAbilities.flying(pilot);
        if (soar != dataManager.get(SOARING)) {
            dataManager.set(SOARING, soar);
            if (soar) {
                dataManager.set(FLYING, false);
                sound(SoundEvents.ENTITY_ENDERDRAGON_FLAP, 2F, 0.6F);
            }
        }
        lent(SharedMonsterAttributes.ARMOR, LENT_ARMOR, inside ? StoneAbilities.resistance(pilot) : 0F, 0);
        lent(SharedMonsterAttributes.MAX_HEALTH, LENT_LIFE, inside && StoneAbilities.immortal(pilot) ? 1F : 0F, 1);
        AbilityHealing healing = inside ? StoneAbilities.healing(pilot) : null;
        if (healing != null && getHealth() < getMaxHealth() && getState() != State.DOWN) {
            Integer every = healing.getDataManager().get(AbilityHealing.FREQUENCY);
            Float amount = healing.getDataManager().get(AbilityHealing.AMOUNT);
            if (every != null && amount != null && every > 0 && ticksExisted % every == 0) {
                heal(amount / Math.max(1F, pilot.getMaxHealth()) * getMaxHealth());
            }
        }
    }

    /**
     * Keeps a modifier its pilot lends it on one of its attributes at an amount, or off it at none. More integrity lent
     * or taken back keeps it as whole as it was, not more or less hurt.
     */
    private void lent(IAttribute attribute, UUID id, float amount, int operation) {
        IAttributeInstance instance = getEntityAttribute(attribute);
        float max = getMaxHealth(), health = getHealth();
        AttributeModifier now = instance.getModifier(id);
        if (now != null && (amount <= 0F || now.getAmount() != amount)) {
            instance.removeModifier(now);
            now = null;
        }
        if (now == null && amount > 0F) {
            instance.applyModifier(new AttributeModifier(id, "Lent by its pilot", amount, operation).setSaved(false));
        }
        if (getMaxHealth() != max && max > 0F) {
            setHealth(health * getMaxHealth() / max);
        }
    }

    /** Charging: going faster than it can walk. */
    private boolean clientSprintSeen(double moved) {
        return moved > 0.25D && onGround;
    }

    /** Ready, and its pilot asking for something. */
    private void choose() {
        EntityPlayer pilot = pilot();
        if (slamQueued) {
            slamQueued = false;
            if (onGround && able(pilot, "The slam", true)) {
                setState(State.SLAM);
                setFurnace(getFurnace() + (has(ArmorUpgrade.SHOCKWAVE) ? 6F : 12F));
                burn(120F);
            }
        } else if (fireHeld && getFurnace() < HEAT_MAX - 10F && able(pilot, "The ray", true)) {
            setState(State.RAY);
            sound(SoundEvents.BLOCK_IRON_TRAPDOOR_OPEN, 2F, 0.35F);
            sound(SoundEvents.BLOCK_PISTON_CONTRACT, 1.5F, 0.5F);
            EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.DESTROYER_CHARGE, posX, posY, posZ, 0D, 0D, 0D, getEntityId(), RAY_OPEN + 4),
                    dimension);
        } else if (attackHeld) {
            setState(rand.nextBoolean() ? State.PUNCH_R : State.PUNCH_L);
        }
    }

    /** Its footfalls, as it moves; and anything in its way when it charges. */
    private void walk() {
        double dx = posX - lastX, dz = posZ - lastZ;
        lastX = posX;
        lastZ = posZ;
        double moved = Math.sqrt(dx * dx + dz * dz);
        if (moved > 2D) {
            return;
        }
        if (onGround && moved > 0.01D) {
            stride += moved;
            if (stride > 2.4D) {
                stride = 0D;
                footfall(0.25F);
            }
        }
        trampled.replaceAll((id, ticks) -> ticks - 1);
        trampled.values().removeIf(ticks -> ticks <= 0);
        EntityPlayer pilot = pilot();
        if (moved > 0.24D && pilot != null && !isFlying()) {
            boolean thunder = has(ArmorUpgrade.THUNDERSTEP);
            Vec3d heading = new Vec3d(dx, 0D, dz).normalize();
            for (EntityLivingBase victim : world.getEntitiesWithinAABB(EntityLivingBase.class, getEntityBoundingBox().grow(0.8D, 0D, 0.8D))) {
                if (victim == this || victim == pilot || trampled.containsKey(victim.getEntityId()) || !victim.isEntityAlive()) {
                    continue;
                }
                Vec3d to = victim.getPositionVector().subtract(getPositionVector());
                if (flat(to).dotProduct(heading) < 0.2D) {
                    continue;
                }
                trampled.put(victim.getEntityId(), 15);
                strike(victim, pilot, thunder ? 16F : 8F, heading.add(flat(to)).normalize(), thunder ? 1.8D : 1.2D, 0.5D, 1);
                sound(SoundEvents.ENTITY_IRONGOLEM_ATTACK, 2F, 0.5F);
            }
        }
    }

    private void footfall(float weight) {
        leftFoot = !leftFoot;
        Vec3d f = facing();
        Vec3d side = new Vec3d(-f.z, 0D, f.x).scale(leftFoot ? 0.45D : -0.45D);
        EternityNetwork.sendAround(new MsgTribunalFx(MsgTribunalFx.DESTROYER_STEP, posX + side.x, posY, posZ + side.z, weight, 0D, 0D,
                getEntityId(), 0), dimension, posX, posY, posZ, 64D);
        sound(SoundEvents.ENTITY_IRONGOLEM_STEP, 2F, 0.4F);
        sound(SoundEvents.BLOCK_ANVIL_LAND, 0.25F, 0.3F);
    }

    // ------------------------------------------------------------------ its blows

    /** One fist, at whatever is in front of its pilot's look and within its reach; failing that, the block there. */
    private void punch(EntityPlayer pilot, int side) {
        Vec3d look = pilot.getLookVec();
        Vec3d flatLook = flat(look);
        Vec3d from = new Vec3d(posX, posY + 2.8D, posZ);
        double reach = REACH + ArmorPowers.reach(this);
        // Its pilot's own strength (the Power Stone's, say) lands with its fists.
        float damage = (float) EternityConfig.vault.armorPunchDamage * (has(ArmorUpgrade.URU_KNUCKLES) ? 1.5F : 1F)
                * (cold() ? 0.6F : condition() == Condition.CRITICAL ? 0.7F : 1F) * Overcharge.FISTS.factor(surge(Overcharge.FISTS))
                + StoneAbilities.strength(pilot);
        boolean hit = false;
        for (EntityLivingBase victim : world.getEntitiesWithinAABB(EntityLivingBase.class, getEntityBoundingBox().grow(reach))) {
            if (victim == this || victim == pilot || !victim.isEntityAlive()) {
                continue;
            }
            Vec3d to = new Vec3d(victim.posX, victim.posY + victim.height * 0.5D, victim.posZ).subtract(from);
            double horizontal = Math.sqrt(to.x * to.x + to.z * to.z);
            if (horizontal > reach + victim.width * 0.5D || flat(to).dotProduct(flatLook) < 0.5D || to.y < -3.2D || to.y > 2.5D) {
                continue;
            }
            Vec3d drive = flatLook.add(new Vec3d(-flatLook.z, 0D, flatLook.x).scale(side == 0 ? 0.25D : -0.25D)).normalize();
            strike(victim, pilot, damage, drive, 1.7D, 0.45D, side);
            if (has(ArmorUpgrade.HAMMERFALL) && !cold()) {
                victim.setFire(5);
                quake(victim.getPositionVector(), 2.5D, damage * 0.4F, pilot, 1, victim);
            }
            hit = true;
        }
        if (hit) {
            sound(EternitySounds.COMET_IMPACT, 2F, 1.1F);
            sound(SoundEvents.ENTITY_IRONGOLEM_ATTACK, 2F, 0.5F);
            return;
        }
        Vec3d eye = pilot.getPositionEyes(1F);
        RayTraceResult wall = world.rayTraceBlocks(eye, eye.add(look.scale(reach + 1D)), false, true, false);
        if (wall != null && wall.typeOfHit == RayTraceResult.Type.BLOCK) {
            BlockPos pos = wall.getBlockPos();
            Vec3d at = wall.hitVec;
            EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.DESTROYER_STEP, at.x, at.y, at.z, 0.15D, 0D, 0D, getEntityId(), 0),
                    dimension);
            sound(SoundEvents.BLOCK_ANVIL_LAND, 1F, 0.5F);
            smash(pos, pilot);
        }
    }

    /** Both fists down together: rings of force out across the ground. */
    private void slam(EntityPlayer pilot) {
        Vec3d at = getPositionVector().add(facing().scale(2.2D));
        boolean shockwave = has(ArmorUpgrade.SHOCKWAVE);
        quake(at, shockwave ? 10.5D : 7D, (float) EternityConfig.vault.armorSlamDamage * Overcharge.FISTS.factor(surge(Overcharge.FISTS))
                + StoneAbilities.strength(pilot), pilot, 3, null);
        sound(EternitySounds.COMET_IMPACT, 3F, 0.6F);
        sound(SoundEvents.ENTITY_GENERIC_EXPLODE, 2.5F, 0.5F);
        sound(SoundEvents.BLOCK_ANVIL_LAND, 2F, 0.3F);
    }

    /** Coming down from a height: the ground breaks in rings wherever it lands. */
    @Override
    public void fall(float distance, float damageMultiplier) {
        if (world.isRemote || distance < 2.5F) {
            if (!world.isRemote && distance > 0.8F) {
                footfall(0.4F);
            }
            return;
        }
        EntityPlayer pilot = pilot();
        double reach = Math.min(8D, 3D + distance * 0.5D) * (has(ArmorUpgrade.THUNDERSTEP) ? 1.5D : 1D);
        quake(getPositionVector(), reach, (float) Math.min(EternityConfig.vault.armorSlamDamage, distance * 2.5F), pilot, distance > 6F ? 3 : 2, null);
        sound(EternitySounds.COMET_IMPACT, 2.5F, 0.7F);
        sound(SoundEvents.ENTITY_GENERIC_EXPLODE, 1.5F, 0.6F);
    }

    /** Rings of force out across the ground from a point, striking everything on it but whoever is spared. */
    void quake(Vec3d at, double reach, float damage, @Nullable EntityPlayer pilot, int rings, @Nullable Entity spared) {
        double floor = Math.min(posY, at.y);
        EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.DESTROYER_QUAKE, at.x, floor + 0.05D, at.z, reach, 0D, 0D, rings, 20),
                dimension);
        double fling = has(ArmorUpgrade.SHOCKWAVE) ? 1.4D : 1D;
        for (EntityLivingBase victim : world.getEntitiesWithinAABB(EntityLivingBase.class, new AxisAlignedBB(at.x - reach, floor - 2D, at.z - reach,
                at.x + reach, floor + 3D, at.z + reach))) {
            if (victim == this || victim == pilot || victim == spared || !victim.isEntityAlive()) {
                continue;
            }
            double d = Math.sqrt((victim.posX - at.x) * (victim.posX - at.x) + (victim.posZ - at.z) * (victim.posZ - at.z));
            if (d > reach) {
                continue;
            }
            float share = d < 2.5D ? 1F : (float) (1D - (d - 2.5D) / (reach - 2.5D)) * 0.6F;
            if (pilot != null) {
                strike(victim, pilot, damage * share, flat(victim.getPositionVector().subtract(at)), 1.1D * fling * (1D - d / (reach + 2D)), 0.7D, 2);
            }
        }
    }

    /** Its face opens, the furnace gathers, and the ray pours out where its pilot looks, as long as they hold it. */
    private void ray(@Nullable EntityPlayer pilot, int t) {
        if (pilot == null) {
            setState(State.RAY_END);
            return;
        }
        if (t < RAY_OPEN) {
            return;
        }
        boolean disintegrator = has(ArmorUpgrade.DISINTEGRATOR);
        setFurnace(getFurnace() + (disintegrator ? 0.75F : 1.1F));
        int charge = surge(Overcharge.BEAM);
        burn(5F);
        double range = (disintegrator ? RAY_RANGE * 1.5D : RAY_RANGE) * Overcharge.beamRange(charge);
        Vec3d look = pilot.getLookVec();
        Vec3d eye = pilot.getPositionEyes(1F);
        // The ray leaves its face, a little below and ahead of its pilot's eyes.
        Vec3d from = eye.add(look.scale(0.9D)).add(0D, -0.25D, 0D);
        RayTraceResult wall = world.rayTraceBlocks(eye, eye.add(look.scale(range)), false, true, false);
        Vec3d end = wall != null && wall.typeOfHit == RayTraceResult.Type.BLOCK ? wall.hitVec : eye.add(look.scale(range));
        // Failing, its furnace sputters: the ray gutters out for a moment now and then.
        boolean sputter = condition() == Condition.FAILING && rand.nextInt(6) == 0;
        if (!sputter) {
            EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.DESTROYER_RAY, from.x, from.y, from.z, end.x, end.y, end.z, t | Math.min(charge, 255) << 24, 3),
                    dimension);
        }
        if (stones() != 0 && t % 2 == 0) {
            EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.DESTROYER_STONE, from.x, from.y, from.z, end.x, end.y, end.z, stones(),
                    ArmorPowers.FX_RAY), dimension);
        }
        if (has(ArmorUpgrade.MOLTEN_HEART) && wall != null && wall.typeOfHit == RayTraceResult.Type.BLOCK && t % 10 == 0) {
            world.newExplosion(this, end.x, end.y, end.z, 2F + Math.min(6F, 0.08F * charge), mayChange(new BlockPos(end)), mayChange(new BlockPos(end)));
        }
        if (sputter) {
            return;
        }
        if (t == RAY_OPEN) {
            sound(SoundEvents.ENTITY_GENERIC_EXPLODE, 2.5F, 0.5F);
            sound(SoundEvents.ENTITY_FIREWORK_LARGE_BLAST_FAR, 3F, 0.4F);
        }
        if (t % 6 == 0) {
            sound(SoundEvents.ENTITY_GUARDIAN_ATTACK, 2.5F, 0.5F);
            sound(SoundEvents.ENTITY_BLAZE_BURN, 2.5F, 0.4F);
        }
        if (t % 4 == 0) {
            for (EntityLivingBase victim : world.getEntitiesWithinAABB(EntityLivingBase.class, new AxisAlignedBB(from.x, from.y, from.z, end.x, end.y, end.z).grow(1.5D))) {
                if (victim == this || victim == pilot || !victim.isEntityAlive()) {
                    continue;
                }
                Vec3d body = new Vec3d(victim.posX, victim.posY + victim.height * 0.5D, victim.posZ);
                if (closest(from, end, body).distanceTo(body) < 1.1D + victim.width * 0.5D) {
                    float burn = (float) EternityConfig.vault.armorRayDamage * (disintegrator ? 1.6F : 1F)
                            * (condition() == Condition.FAILING ? 0.75F : 1F) * Overcharge.BEAM.factor(charge);
                    strike(victim, pilot, burn, look, 0.15D, 0.05D, -1);
                    victim.setFire(4);
                }
            }
            if (wall != null && wall.typeOfHit == RayTraceResult.Type.BLOCK && t % 12 == 0) {
                kindle(wall.getBlockPos().offset(wall.sideHit), pilot);
            }
        }
        if (!fireHeld || getFurnace() >= HEAT_MAX || cold()) {
            setState(State.RAY_END);
        }
    }

    /** One blow landing, credited to whoever is inside, as hard as the stones in its chest make it. */
    void strike(EntityLivingBase victim, EntityPlayer pilot, float damage, Vec3d drive, double knock, double lift, int kind) {
        if (kind >= 0) {
            Vec3d at = new Vec3d(victim.posX, victim.posY + victim.height * 0.6D, victim.posZ);
            EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.DESTROYER_IMPACT, at.x, at.y, at.z, drive.x, drive.y, drive.z,
                    victim.getEntityId(), kind), dimension);
        }
        victim.hurtResistantTime = 0;
        EntityDamageSourceIndirect source = new EntityDamageSourceIndirect("eternity.destroyer_armor", this, pilot);
        // The Reality Stone: what it strikes, it strikes whatever the target is wearing.
        if (holdsStone(ArmorPowers.REALITY)) {
            source.setDamageBypassesArmor();
        }
        if (!victim.attackEntityFrom(source, damage * might())) {
            return;
        }
        // The Soul Stone: every life it takes mends it.
        if (!victim.isEntityAlive() && holdsStone(ArmorPowers.SOUL)) {
            heal(getMaxHealth() * ArmorPowers.harvest(this));
        }
        double steady = 1D - victim.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).getAttributeValue() * 0.5D;
        victim.motionX += drive.x * knock * steady;
        victim.motionY = Math.max(victim.motionY, 0D) + lift * steady;
        victim.motionZ += drive.z * knock * steady;
        victim.velocityChanged = true;
    }

    // ------------------------------------------------------------------ the world under it

    /** Whether it may break and burn things here: never in the vault or the other sealed places, and only where mobs may. */
    private boolean mayChange(BlockPos pos) {
        int dim = world.provider.getDimension();
        return EternityConfig.vault.armorChangesTerrain && world.getGameRules().getBoolean("mobGriefing") && dim != OdinsVault.dimension()
                && dim != StarChamber.dimension() && dim != Nexus.dimension() && world.isBlockModifiable(pilot(), pos);
    }

    /** A fist landing on a block: soft stone, earth and wood give way. */
    private void smash(BlockPos pos, EntityPlayer pilot) {
        IBlockState state = world.getBlockState(pos);
        float hardness = state.getBlockHardness(world, pos);
        float hardest = has(ArmorUpgrade.URU_KNUCKLES) ? 25F : 3F;
        if (hardness < 0F || hardness > hardest || state.getMaterial() == Material.AIR || !mayChange(pos)) {
            return;
        }
        BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(world, pos, state, pilot);
        if (!MinecraftForge.EVENT_BUS.post(event)) {
            world.destroyBlock(pos, true);
        }
    }

    /** The ray setting fire to what it strikes. */
    private void kindle(BlockPos pos, EntityPlayer pilot) {
        if (world.isAirBlock(pos) && mayChange(pos) && Blocks.FIRE.canPlaceBlockAt(world, pos)) {
            world.setBlockState(pos, Blocks.FIRE.getDefaultState());
        }
    }

    // ------------------------------------------------------------------ being struck

    /** Nothing reaches whoever is inside: the armour takes it. Beaten, it goes down on its knees and puts them out. */
    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (world.isRemote || isEntityInvulnerable(source) || getState() == State.DOWN || getState() == State.IMPLODE) {
            return false;
        }
        if (source.isFireDamage() || source == DamageSource.FALL || source == DamageSource.DROWN || source == DamageSource.IN_WALL
                || source == DamageSource.CACTUS || source == DamageSource.CRAMMING || source == DamageSource.FLY_INTO_WALL) {
            return false;
        }
        Entity attacker = source.getTrueSource();
        if (attacker != null && (isPassenger(attacker) || attacker == this)) {
            return false;
        }
        EntityPlayer pilot = pilot();
        // Its pilot's force field stands around all of it, and flashes where it is struck.
        AbilityForceField field = hidesPilot() ? StoneAbilities.shield(pilot) : null;
        if (field != null && source != DamageSource.OUT_OF_WORLD) {
            field.getDataManager().set(AbilityForceField.OPACITY, 1F);
            return false;
        }
        // Its pilot's own black hole, pulling at everything but them.
        if (pilot != null && "black_hole".equals(source.getDamageType()) && !world.getEntitiesWithinAABB(EntityBlackHole.class,
                getEntityBoundingBox().grow(8D), hole -> hole.creator == pilot).isEmpty()) {
            return false;
        }
        if (source.isExplosion()) {
            amount *= has(ArmorUpgrade.BANDED_HULL) ? 0.25F : 0.5F;
        }
        // Its plating overcharged turns some of every blow.
        if (surging() && source != DamageSource.OUT_OF_WORLD && source != OVERHEAT) {
            amount *= Overcharge.PLATING.factor(surge(Overcharge.PLATING));
        }
        if (pilot != null && ItemAstralRegulator.holdsIn(lentRegulator(pilot), ItemAstralRegulator.ETERNITY) && source != DamageSource.OUT_OF_WORLD) {
            amount = Math.min(amount, getMaxHealth() * (float) EternityConfig.combat.maxHitFraction);
        }
        float before = getHealth();
        boolean hurt = super.attackEntityFrom(source, amount);
        if (hurt && before - getHealth() > 0.01F) {
            wounded(source, before - getHealth());
        }
        return hurt;
    }

    /**
     * Struck, and hurt: sparks and shards of its plate burst from where the blow landed, toward whatever struck it,
     * more of both the harder it was hit.
     */
    private void wounded(DamageSource source, float lost) {
        Entity from = source.getImmediateSource() != null ? source.getImmediateSource() : source.getTrueSource();
        Vec3d out;
        double up;
        if (from != null && from != this) {
            out = flat(new Vec3d(from.posX - posX, 0D, from.posZ - posZ));
            up = MathHelper.clamp(from.posY + from.height * 0.5D - posY, 0.8D, 3.6D);
        } else {
            double a = rand.nextDouble() * Math.PI * 2D;
            out = new Vec3d(Math.cos(a), 0D, Math.sin(a));
            up = 1D + rand.nextDouble() * 2.5D;
        }
        float s = scale();
        Vec3d at = getPositionVector().add(out.scale(0.75D * s)).add(0D, up * s, 0D);
        float severity = MathHelper.clamp(lost / (float) (EternityConfig.vault.armorHealth * 0.05D), 0.2F, 4F);
        EternityNetwork.sendAround(new MsgTribunalFx(MsgTribunalFx.DESTROYER_WOUND, at.x, at.y, at.z, out.x, 0.35D, out.z, getEntityId(),
                (int) (severity * 100F)), dimension, posX, posY, posZ, 96D);
    }

    /** Client: a blow hard enough to throw it is stronger than its pilot's steering, for a moment. */
    @SideOnly(Side.CLIENT)
    @Override
    public void setVelocity(double x, double y, double z) {
        super.setVelocity(x, y, z);
        double speed = x * x + y * y + z * z;
        if (speed > 0.16D) {
            flung = Math.max(flung, (int) MathHelper.clamp(8D + Math.sqrt(speed) * 10D, 8D, 30D));
        }
    }

    // ------------------------------------------------------------------ what reaches whoever is inside

    /** The armour they ride, in any state, or null: whatever moves them has to move it. */
    @Nullable
    public static EntityDestroyerArmor carrier(@Nullable Entity entity) {
        return entity != null && entity.getRidingEntity() instanceof EntityDestroyerArmor ? (EntityDestroyerArmor) entity.getRidingEntity() : null;
    }

    /** The armour sealed around them, or null: whatever strikes them strikes it. */
    @Nullable
    public static EntityDestroyerArmor shell(@Nullable Entity entity) {
        EntityDestroyerArmor armor = carrier(entity);
        return armor != null && armor.hidesPilot() ? armor : null;
    }

    /**
     * Sealed inside the armour, out of reach: anything sweeping an area for victims should pass over them, since it
     * finds the armour itself (and a blow on them would land on it a second time).
     */
    public static boolean sheltered(@Nullable Entity entity) {
        return shell(entity) != null;
    }

    /** What a blow aimed at them lands on: the armour around them, or themselves. */
    public static EntityLivingBase body(EntityLivingBase victim) {
        EntityDestroyerArmor armor = shell(victim);
        return armor != null ? armor : victim;
    }

    /** What a push, a throw or a grab moves: the armour they ride, or themselves. */
    public static Entity mass(Entity victim) {
        EntityDestroyerArmor armor = carrier(victim);
        return armor != null ? armor : victim;
    }

    /** Whoever is inside, if this is the armour: the one who counts as struck, for being worthy or being spared. */
    public static Entity soul(Entity victim) {
        if (victim instanceof EntityDestroyerArmor) {
            EntityPlayer pilot = ((EntityDestroyerArmor) victim).pilot();
            return pilot != null ? pilot : victim;
        }
        return victim;
    }

    /**
     * The health a share-of-health blow is measured against. The armour's is its own integrity, tempered by its plate
     * ({@code armorBossShare}) and measured from what it was built with, so Uru forged onto it is real protection
     * rather than more to lose.
     */
    public static float health(EntityLivingBase body) {
        if (body instanceof EntityDestroyerArmor) {
            return (float) (EternityConfig.vault.armorHealth * EternityConfig.vault.armorBossShare);
        }
        return body.getMaxHealth();
    }

    /** A blow of a share of the victim's health; inside the armour, a share of the armour's. */
    public static boolean strike(EntityLivingBase victim, DamageSource source, float share) {
        EntityLivingBase body = body(victim);
        body.hurtResistantTime = 0;
        return body.attackEntityFrom(source, health(body) * share);
    }

    /** Takes a share of their health outright, never quite killing: inside the armour, of the armour's. */
    public static void wound(EntityLivingBase victim, float share) {
        EntityLivingBase body = body(victim);
        body.setHealth(Math.max(1F, body.getHealth() - health(body) * share));
    }

    /** How far the armour goes for a throw that would send a person a block. */
    public static final double HEAVY = 0.6D;

    /**
     * Throws them. Inside the armour it is the armour that is thrown, with them in it; it is far heavier than they
     * are, so it goes {@link #HEAVY} as far. The armour steers on its pilot's side, where a throw takes over from
     * their steering until it lands (see {@link #setVelocity}).
     */
    public static void shove(Entity victim, double x, double y, double z) {
        EntityDestroyerArmor armor = victim instanceof EntityDestroyerArmor ? (EntityDestroyerArmor) victim : carrier(victim);
        if (armor == null) {
            victim.motionX += x;
            victim.motionY += y;
            victim.motionZ += z;
        } else {
            // Its server-side motion is never its own (its pilot moves it), so the throw is all it carries.
            armor.motionX = x * HEAVY;
            armor.motionY = y * HEAVY;
            armor.motionZ = z * HEAVY;
            armor.fallDistance = 0F;
            armor.velocityChanged = true;
        }
        victim.velocityChanged = true;
    }

    /** Sets their motion outright: {@link #shove} from standing still. */
    public static void fling(Entity victim, double x, double y, double z) {
        if (!(victim instanceof EntityDestroyerArmor) && carrier(victim) == null) {
            victim.motionX = victim.motionY = victim.motionZ = 0D;
        }
        shove(victim, x, y, z);
    }

    /**
     * Puts them somewhere in this world. Inside the armour, the armour is put there, standing where they would have
     * stood, and held a moment so its pilot cannot steer straight back out.
     */
    public static void place(Entity who, double x, double y, double z, float yaw, float pitch) {
        EntityDestroyerArmor armor = who instanceof EntityDestroyerArmor ? (EntityDestroyerArmor) who : carrier(who);
        if (armor != null) {
            armor.put(x, y, z);
        } else if (who instanceof EntityPlayerMP) {
            ((EntityPlayerMP) who).connection.setPlayerLocation(x, y, z, yaw, pitch);
        } else {
            who.setPositionAndUpdate(x, y, z);
        }
        who.fallDistance = 0F;
    }

    /** Moves it, its pilot and its pilot's view of it, and holds it there for a few ticks. */
    public void put(double x, double y, double z) {
        setPositionAndUpdate(x, y, z);
    }

    /**
     * However it is moved somewhere (a portal, a command, a boss's grip), its pilot's side is told, and it is held a
     * moment there; or its pilot, steering it, would carry it straight back.
     */
    @Override
    public void setPositionAndUpdate(double x, double y, double z) {
        super.setPositionAndUpdate(x, y, z);
        if (world.isRemote) {
            return;
        }
        motionX = motionY = motionZ = 0D;
        fallDistance = 0F;
        heldTicks = 4;
        dataManager.set(HELD, true);
        EntityPlayer pilot = pilot();
        if (pilot instanceof EntityPlayerMP && ((EntityPlayerMP) pilot).connection != null) {
            ((EntityPlayerMP) pilot).connection.sendPacket(new SPacketMoveVehicle(this));
        }
    }

    @Override
    public void onDeath(DamageSource cause) {
        setHealth(1F);
        down();
    }

    private void down() {
        if (getState() == State.DOWN) {
            return;
        }
        EntityPlayer pilot = pilot();
        setState(State.DOWN);
        setFurnace(0F);
        sound(SoundEvents.BLOCK_ANVIL_LAND, 2.5F, 0.3F);
        sound(SoundEvents.ENTITY_IRONGOLEM_DEATH, 2F, 0.4F);
        EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.DESTROYER_STEP, posX, posY, posZ, 0.7D, 0D, 0D, getEntityId(), 0), dimension);
        if (pilot != null) {
            letOut(pilot);
            pilot.sendStatusMessage(new TextComponentString(TextFormatting.GRAY + "The Destroyer Armor goes down. Uru will mend it."), true);
        }
    }

    /**
     * Gone under the abyss beneath the Nexus. It lets go of whoever is inside, and everything in it is spilled, for the
     * abyss to take or, if it cannot be destroyed, to give back.
     */
    public void swallowed() {
        EntityPlayer pilot = pilot();
        if (pilot != null) {
            release(pilot);
        }
        onKillCommand();
    }

    /** Killed outright: whatever was in it falls out where it stood. */
    @Override
    public void onKillCommand() {
        if (!world.isRemote) {
            for (int i = 0; i < SLOTS; i++) {
                ItemStack stack = inventory.removeStackFromSlot(i);
                if (!stack.isEmpty()) {
                    world.spawnEntity(new EntityItem(world, posX, posY + 1D, posZ, stack));
                }
            }
        }
        setDead();
    }

    // ------------------------------------------------------------------ steering it

    /** Whoever is inside steers it on their own side, the way a horse is steered; nobody inside, it only stands. */
    @Override
    public void travel(float strafe, float vertical, float forward) {
        Entity pilot = getControllingPassenger();
        if (pilot instanceof EntityLivingBase && canPassengerSteer()) {
            steer((EntityLivingBase) pilot);
        } else if (!world.isRemote && pilot == null) {
            motionX *= 0.5D;
            motionZ *= 0.5D;
            fallFreely();
            move(MoverType.SELF, motionX, motionY, motionZ);
        }
        EntityEinherjar.stride(this);
    }

    private void steer(EntityLivingBase pilot) {
        // Held, it hangs where it is put; thrown, it tumbles where it was thrown until it finds its feet.
        if (dataManager.get(HELD)) {
            motionX = motionY = motionZ = 0D;
            return;
        }
        if (flung > 0) {
            flung--;
            if (!onGround || flung > 6) {
                motionX *= 0.96D;
                motionZ *= 0.96D;
                if (!isFlying() || flung > 4) {
                    fallFreely();
                } else {
                    motionY *= 0.9D;
                }
                move(MoverType.SELF, motionX, motionY, motionZ);
                if (onGround && motionY <= 0D && flung > 6) {
                    flung = 6;
                    motionX *= 0.5D;
                    motionZ *= 0.5D;
                }
                return;
            }
        }
        float mobility = mobility();
        boolean cold = cold();
        Condition condition = condition();
        if (mobility > 0F || getState() == State.EXIT) {
            float turn = getState() == State.RAY ? 4F : has(ArmorUpgrade.PISTONS) ? 14F : 10F;
            rotationYaw += MathHelper.clamp(MathHelper.wrapDegrees(pilot.rotationYaw - rotationYaw), -turn, turn);
        }
        rotationYawHead = pilot.rotationYawHead;
        rotationPitch = pilot.rotationPitch * 0.5F;
        float forward = pilot.moveForward, strafe = pilot.moveStrafing;
        // Cold, or critical, it cannot charge; hurt, it moves worse; cold, it barely moves at all.
        boolean sprint = clientSprint && forward > 0F && getState() == State.READY && !cold && condition != Condition.CRITICAL;
        double charge = has(ArmorUpgrade.THUNDERSTEP) ? 0.44D : 0.34D;
        double speed = (sprint ? charge : 0.17D) * mobility * (forward < 0F ? 0.6D : 1D) * (cold ? 0.35D : condition.stride)
                * (has(ArmorUpgrade.PISTONS) ? 1.25D : 1D) * sluggish(pilot);
        if (isFlying()) {
            fly(pilot, forward, strafe, mobility);
            return;
        }
        soaredFor = 0;
        double yaw = Math.toRadians(pilot.rotationYaw);
        double fx = -Math.sin(yaw), fz = Math.cos(yaw);
        double wantX = fx * forward + fz * strafe * 0.7D;
        double wantZ = fz * forward - fx * strafe * 0.7D;
        double length = Math.sqrt(wantX * wantX + wantZ * wantZ);
        if (length > 1D) {
            wantX /= length;
            wantZ /= length;
        }
        wantX *= speed;
        wantZ *= speed;
        // Heavy: it takes a few strides to get going, and a few to stop.
        double grip = isInWater() ? 0.07D : onGround ? 0.2D : 0.035D;
        motionX += (wantX - motionX) * grip;
        motionZ += (wantZ - motionZ) * grip;
        boolean leaps = !cold && condition != Condition.CRITICAL;
        if (clientJump && onGround && jumpCooldown <= 0 && mobility >= 1F && leaps) {
            double leap = has(ArmorUpgrade.LEAP_JETS) ? 0.95D : 0.72D;
            motionY = isInWater() ? 0.45D : leap * (condition == Condition.FAILING ? 0.85D : 1D);
            motionX += fx * (sprint ? 0.35D : 0.15D);
            motionZ += fz * (sprint ? 0.35D : 0.15D);
            jumpCooldown = 16;
            world.playSound(posX, posY, posZ, SoundEvents.BLOCK_PISTON_EXTEND, SoundCategory.NEUTRAL, 1.5F, 0.5F, false);
        }
        fallFreely();
        // Its leap jets: jump held on the way down, and it glides on fire.
        if (!onGround && clientJump && motionY < 0D && has(ArmorUpgrade.LEAP_JETS) && leaps && jumpCooldown < 10) {
            motionY = Math.max(motionY, -0.12D);
            motionX += fx * 0.012D;
            motionZ += fz * 0.012D;
        }
        move(MoverType.SELF, motionX, motionY, motionZ);
    }

    /** Whatever slows its pilot (a curse, the weight of a judgement) slows how they drive it. */
    private static double sluggish(EntityLivingBase pilot) {
        PotionEffect slow = pilot.getActivePotionEffect(MobEffects.SLOWNESS);
        return slow == null ? 1D : Math.max(0.15D, 1D - 0.15D * (slow.getAmplifier() + 1));
    }

    /** Ascended: it flies where its pilot looks, climbing on jump and sinking on sneak, hanging in the air otherwise. */
    private void fly(EntityLivingBase pilot, float forward, float strafe, float mobility) {
        // Heavy even in the air: it gathers speed over a second or so and carries it into its turns.
        double speed = (clientSprint && forward > 0F ? 1.05D : 0.5D) * Math.max(0.4F, mobility) * sluggish(pilot)
                * (soaring() ? 1F : Overcharge.JETS.factor(surge(Overcharge.JETS)));
        double yaw = Math.toRadians(pilot.rotationYaw), pitch = Math.toRadians(pilot.rotationPitch);
        double fx = -Math.sin(yaw), fz = Math.cos(yaw);
        double wantX = (fx * forward + fz * strafe * 0.7D) * speed;
        double wantZ = (fz * forward - fx * strafe * 0.7D) * speed;
        // Flying forward it climbs or dives with its pilot's look; jump and sneak climb and sink outright.
        double wantY = -Math.sin(pitch) * Math.max(0F, forward) * speed * 0.85D + (clientJump ? 0.42D : 0D) - (clientDescend ? 0.42D : 0D);
        // Taking to the air by its pilot's own flight, it rises clear of the ground first, as they would.
        if (soaring() && soaredFor++ < 20 && !clientDescend) {
            wantY = Math.max(wantY, 0.42D);
        }
        double grip = forward == 0F && strafe == 0F ? 0.05D : 0.07D;
        motionX += (wantX - motionX) * grip;
        motionZ += (wantZ - motionZ) * grip;
        motionY += (wantY - motionY) * 0.1D;
        // Hanging there, it bobs gently on its jets.
        if (Math.abs(wantY) < 0.01D) {
            motionY += MathHelper.sin(ticksExisted * 0.1F) * 0.0035D;
        }
        move(MoverType.SELF, motionX, motionY, motionZ);
        fallDistance = 0F;
        // Brought down onto the ground, it lands.
        if (onGround && clientDescend && ticksExisted % 10 == 0) {
            EternityNetwork.sendToServer(new com.matoon.eternity.network.MsgArmorCommand(Command.FLY, false));
        }
    }

    // ------------------------------------------------------------------ client: how it carries itself in the air

    /**
     * Client: how far it has taken to the air (0 standing, 1 flying), and how it carries itself there: leaning
     * into its speed, banking into its turns, climbing or diving. Worked out from how it actually moves, so it
     * looks the same to everyone watching as to its pilot; the model and its jets both read these.
     */
    public float flight, prevFlight, airborne, prevAirborne, lean, prevLean, bank, prevBank, climb, prevClimb;
    private double flownX, flownY, flownZ;
    private float flownYaw;
    private boolean wasFlying;

    public float flight(float pt) {
        return prevFlight + (flight - prevFlight) * pt;
    }

    public float airborne(float pt) {
        return prevAirborne + (airborne - prevAirborne) * pt;
    }

    public float lean(float pt) {
        return prevLean + (lean - prevLean) * pt;
    }

    public float bank(float pt) {
        return prevBank + (bank - prevBank) * pt;
    }

    public float climb(float pt) {
        return prevClimb + (climb - prevClimb) * pt;
    }

    private void flightTick() {
        prevFlight = flight;
        prevAirborne = airborne;
        prevLean = lean;
        prevBank = bank;
        prevClimb = climb;
        double dx = posX - flownX, dy = posY - flownY, dz = posZ - flownZ;
        flownX = posX;
        flownY = posY;
        flownZ = posZ;
        float turn = MathHelper.wrapDegrees(rotationYaw - flownYaw);
        flownYaw = rotationYaw;
        if (dx * dx + dy * dy + dz * dz > 25D) {
            dx = dy = dz = 0D;
            turn = 0F;
        }
        Vec3d f = facing();
        float ahead = (float) (dx * f.x + dz * f.z) / 0.85F, aside = (float) (dz * f.x - dx * f.z) / 0.5F, up = (float) dy / 0.42F;
        boolean flying = isFlying();
        flight += ((flying ? 1F : 0F) - flight) * (flying ? 0.08F : 0.15F);
        boolean air = !flying && !onGround && getState() != State.DOWN && getState() != State.EMPTY && !isInWater();
        airborne += ((air ? 1F : 0F) - airborne) * 0.2F;
        float speed = MathHelper.clamp(ahead, -0.5F, 1F);
        // Leaning into its speed until it lies almost flat at full burn; backing away it rears up; diving it leans further.
        float wantLean = 0F;
        if (flying) {
            wantLean = 0.1F + 1.0F * Math.max(0F, speed) - 0.7F * Math.max(0F, -speed)
                    - 0.3F * MathHelper.clamp(up, 0F, 1F) * (1F - Math.max(0F, speed))
                    + 0.3F * MathHelper.clamp(-up, 0F, 1F) * Math.max(0F, speed);
        }
        lean += (wantLean - lean) * 0.09F;
        // Banking: rolled into a turn, and a little into a sideways slide.
        float wantBank = flying ? MathHelper.clamp(-turn * 0.04F * (0.4F + Math.max(0F, speed)) - aside * 0.3F, -0.75F, 0.75F) : 0F;
        bank += (wantBank - bank) * 0.1F;
        climb += (MathHelper.clamp(up, -1F, 1F) * (flying ? 1F : 0F) - climb) * 0.15F;
        boolean soaring = soaring();
        if (flying && !wasFlying) {
            if (soaring) {
                rise();
            } else {
                launch();
            }
        }
        wasFlying = flying;
        if (flight > 0.05F && soaring) {
            aura(0.45F + 0.55F * MathHelper.clamp(Math.max(speed, Math.abs(up)), 0F, 1F), lean);
        } else if (flight > 0.05F) {
            jets(0.45F + 0.55F * MathHelper.clamp(Math.max(speed, Math.abs(up)), 0F, 1F), lean);
            if (ticksExisted % 8 == 0) {
                world.playSound(posX, posY + 1D, posZ, SoundEvents.ENTITY_BLAZE_BURN, SoundCategory.NEUTRAL, 0.35F + 0.35F * Math.max(0F, speed),
                        0.45F + 0.2F * Math.max(0F, speed), false);
            }
        } else if (isGliding()) {
            jets(0.4F, 0F);
        }
    }

    /** Taking to the air by its pilot's own power (the Mind Stone's): no fire, only a ring of golden light beneath it. */
    private void rise() {
        for (int i = 0; i < 48; i++) {
            double a = Math.PI * 2D * i / 48D;
            world.spawnParticle(EnumParticleTypes.SPELL_MOB, posX + Math.cos(a) * 1.4D, posY + 0.1D, posZ + Math.sin(a) * 1.4D, 1D, 0.88D, 0.23D);
            if (i % 3 == 0) {
                world.spawnParticle(EnumParticleTypes.END_ROD, posX, posY + 0.2D, posZ, Math.cos(a) * 0.25D, 0.02D, Math.sin(a) * 0.25D);
            }
        }
        world.playSound(posX, posY, posZ, SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.NEUTRAL, 1.5F, 0.6F, false);
    }

    /** Carried by its pilot's own flight: a golden light streaming from its heels where its jets would burn. */
    private void aura(float strength, float leanNow) {
        Vec3d f = facing();
        Vec3d right = new Vec3d(-f.z, 0D, f.x);
        Vec3d leg = new Vec3d(-f.x * Math.sin(leanNow), -Math.cos(leanNow), -f.z * Math.sin(leanNow));
        Vec3d hips = new Vec3d(posX, posY + 1.3D, posZ);
        for (int side = -1; side <= 1; side += 2) {
            Vec3d foot = hips.add(leg.scale(1.35D)).add(right.scale(0.3D * side + bank * 0.4D));
            for (int i = 0; i < 2; i++) {
                Vec3d trail = foot.add(leg.scale(rand.nextDouble() * 0.8D * strength));
                world.spawnParticle(EnumParticleTypes.SPELL_MOB, trail.x + rand.nextGaussian() * 0.08D, trail.y + rand.nextGaussian() * 0.08D,
                        trail.z + rand.nextGaussian() * 0.08D, 1D, 0.88D, 0.23D);
            }
            if (rand.nextFloat() < 0.25F + 0.35F * strength) {
                world.spawnParticle(EnumParticleTypes.END_ROD, foot.x, foot.y, foot.z, leg.x * 0.08D + rand.nextGaussian() * 0.01D, leg.y * 0.08D,
                        leg.z * 0.08D + rand.nextGaussian() * 0.01D);
            }
        }
        if (ticksExisted % 30 == 0) {
            world.playSound(posX, posY + 1D, posZ, SoundEvents.BLOCK_PORTAL_AMBIENT, SoundCategory.NEUTRAL, 0.12F, 1.8F, false);
        }
    }

    /** Taking to the air: a blast of fire and smoke down onto the ground under it. */
    private void launch() {
        for (int i = 0; i < 60; i++) {
            double a = rand.nextDouble() * Math.PI * 2D, s = 0.2D + rand.nextDouble() * 0.5D;
            world.spawnParticle(i % 3 == 0 ? EnumParticleTypes.FLAME : EnumParticleTypes.SMOKE_LARGE, posX, posY + 0.1D, posZ,
                    Math.cos(a) * s, 0.02D + rand.nextDouble() * 0.05D, Math.sin(a) * s);
        }
        world.playSound(posX, posY, posZ, SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.NEUTRAL, 2F, 0.4F, false);
        world.playSound(posX, posY, posZ, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.NEUTRAL, 0.6F, 0.6F, false);
    }

    /**
     * Fire from its heels as it glides or flies, from where its feet really are: trailing behind it as it leans
     * into its speed, the jets streaming back along its legs.
     */
    private void jets(float strength, float leanNow) {
        Vec3d f = facing();
        Vec3d right = new Vec3d(-f.z, 0D, f.x);
        // Its legs hang from its hips, 1.3 blocks up, and swing back as it leans forward.
        Vec3d leg = new Vec3d(-f.x * Math.sin(leanNow), -Math.cos(leanNow), -f.z * Math.sin(leanNow));
        Vec3d hips = new Vec3d(posX, posY + 1.3D, posZ);
        for (int side = -1; side <= 1; side += 2) {
            Vec3d foot = hips.add(leg.scale(1.35D)).add(right.scale(0.3D * side + bank * 0.4D));
            int flames = strength > 0.7F ? 3 : 2;
            for (int i = 0; i < flames; i++) {
                double s = 0.2D + 0.3D * strength + rand.nextDouble() * 0.15D;
                world.spawnParticle(EnumParticleTypes.FLAME, foot.x + rand.nextGaussian() * 0.06D, foot.y + rand.nextGaussian() * 0.06D,
                        foot.z + rand.nextGaussian() * 0.06D, leg.x * s + rand.nextGaussian() * 0.02D, leg.y * s, leg.z * s + rand.nextGaussian() * 0.02D);
            }
            if (rand.nextFloat() < 0.3F + 0.4F * strength) {
                Vec3d trail = foot.add(leg.scale(0.6D + rand.nextDouble() * 0.6D));
                world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, trail.x, trail.y, trail.z, leg.x * 0.05D, leg.y * 0.05D, leg.z * 0.05D);
            }
            if (strength > 0.8F && rand.nextFloat() < 0.15F) {
                world.spawnParticle(EnumParticleTypes.LAVA, foot.x, foot.y, foot.z, 0D, 0D, 0D);
            }
        }
    }

    /** Gravity: it sinks in water and walks along the bottom. */
    private void fallFreely() {
        if (isInWater() || isInLava()) {
            motionY = Math.max(-0.3D, motionY - 0.03D);
            motionY *= 0.9D;
        } else {
            motionY = Math.max(-3D, (motionY - 0.09D) * 0.98D);
        }
    }

    /** How freely it moves in what it is doing. */
    private float mobility() {
        switch (getState()) {
            case READY:
                return 1F;
            case PUNCH_R:
            case PUNCH_L:
                return 0.55F;
            case RAY:
                return 0.25F;
            case RAY_END:
            case VENT:
                return 0.45F;
            default:
                return 0F;
        }
    }

    // ------------------------------------------------------------------ client: its fire and its plates

    private void clientTick() {
        flightTick();
        clientStateTicks++;
        int t = clientStateTicks;
        float wantHeat, wantOpen = 0F, wantBack = 0F;
        switch (getState()) {
            case EMPTY:
                wantHeat = 0F;
                wantBack = 1F;
                break;
            case RISE:
                wantHeat = 0F;
                wantBack = t > RISE_TICKS - 20 ? 1F : 0.35F;
                break;
            case ENTER:
                wantBack = t < ENTER_SHUT - 4 ? 1F : 0F;
                wantHeat = t < ENTER_SHUT + 6 ? 0F : t < ENTER_SHUT + 10 ? (t % 3 == 0 ? 0.6F : 0.1F) : 0.45F;
                break;
            case EXIT:
                wantBack = t < EXIT_OPEN - 4 ? 0F : 1F;
                wantHeat = Math.max(0F, 0.45F - t * 0.03F);
                break;
            case RAY:
                wantOpen = 1F;
                wantHeat = t < RAY_OPEN ? 0.5F + 0.5F * t / RAY_OPEN : 1.1F;
                break;
            case RAY_END:
                wantHeat = 0.6F;
                break;
            case VENT:
                wantHeat = t % 6 < 3 ? 0.9F : 0.6F;
                wantBack = 0.25F;
                break;
            case DOWN:
                wantHeat = 0F;
                wantBack = 0.4F;
                break;
            default:
                wantHeat = 0.45F + 0.25F * getFurnace() / HEAT_MAX;
                break;
        }
        State state = getState();
        boolean awake = state != State.EMPTY && state != State.RISE && state != State.DOWN;
        if (awake && cold()) {
            // Nothing left to burn: only embers behind its face.
            wantHeat = Math.min(wantHeat, 0.08F + 0.04F * MathHelper.sin(t * 0.2F));
        } else if (awake && condition() == Condition.CRITICAL && rand.nextInt(12) == 0) {
            wantHeat *= 0.2F;
        }
        jolt *= 0.75F;
        prevHeatGlow = heatGlow;
        heatGlow += (wantHeat - heatGlow) * 0.25F;
        prevOpen = open;
        open += (wantOpen - open) * (wantOpen > open ? 0.3F : 0.2F);
        prevBack = back;
        back += (wantBack - back) * (wantBack > back ? 0.18F : 0.3F);
    }

    @Override
    public float heat(float partialTicks) {
        return prevHeatGlow + (heatGlow - prevHeatGlow) * partialTicks;
    }

    @Override
    public float faceOpen(float partialTicks) {
        return prevOpen + (open - prevOpen) * partialTicks;
    }

    @Override
    public float backOpen(float partialTicks) {
        return prevBack + (back - prevBack) * partialTicks;
    }

    @Override
    public Vec3d face() {
        return new Vec3d(posX, posY + 3.85D * sized, posZ).add(facing().scale(0.6D * sized));
    }

    // ------------------------------------------------------------------ saving

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        // Saved as whole as it is, against what it holds without its pilot's life lent to it.
        AttributeModifier life = getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).getModifier(LENT_LIFE);
        if (life != null) {
            compound.setFloat("Health", getHealth() / (1F + (float) life.getAmount()));
        }
        State state = getState();
        compound.setString("State", (state == State.DOWN ? State.DOWN : isBeingRidden() ? State.READY : State.EMPTY).name());
        compound.setFloat("Furnace", getFurnace());
        compound.setFloat("Lava", getLava());
        compound.setInteger("Forged", forged());
        compound.setByte("Skin", (byte) skin().ordinal());
        compound.setInteger("Skins", dataManager.get(SKINS));
        compound.setTag("Overcharge", dataManager.get(SURGE).copy());
        compound.setFloat("Core", core());
        NBTTagList items = new NBTTagList();
        for (int i = 0; i < SLOTS; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                NBTTagCompound tag = stack.writeToNBT(new NBTTagCompound());
                tag.setByte("Slot", (byte) i);
                items.appendTag(tag);
            }
        }
        compound.setTag("Items", items);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        // Its frame first, so the integrity it was saved with fits back into it.
        dataManager.set(FORGED, compound.getInteger("Forged"));
        float health = compound.getFloat("Health");
        refit();
        super.readEntityFromNBT(compound);
        setHealth(health);
        setState("DOWN".equals(compound.getString("State")) ? State.DOWN : State.EMPTY);
        setFurnace(compound.getFloat("Furnace"));
        setLava(compound.getFloat("Lava"));
        dataManager.set(SKINS, compound.getInteger("Skins") | ArmorSkin.GUNMETAL.bit());
        dataManager.set(SURGE, compound.getCompoundTag("Overcharge").copy());
        dataManager.set(CORE, MathHelper.clamp(compound.getFloat("Core"), 0F, Overcharge.CORE_MAX));
        dataManager.set(SKIN, (byte) ArmorSkin.of(compound.getByte("Skin")).ordinal());
        inventory.clear();
        NBTTagList items = compound.getTagList("Items", 10);
        for (int i = 0; i < items.tagCount(); i++) {
            NBTTagCompound tag = items.getCompoundTagAt(i);
            int slot = tag.getByte("Slot");
            if (slot >= 0 && slot < SLOTS) {
                inventory.setInventorySlotContents(slot, new ItemStack(tag));
            }
        }
    }

    // ------------------------------------------------------------------ helpers

    private void sound(SoundEvent sound, float volume, float pitch) {
        world.playSound(null, posX, posY + 2D, posZ, sound, SoundCategory.NEUTRAL, volume, pitch);
    }

    private Vec3d facing() {
        float yaw = (float) Math.toRadians(rotationYaw);
        return new Vec3d(-MathHelper.sin(yaw), 0D, MathHelper.cos(yaw));
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

    private static float smooth(float x) {
        x = x < 0F ? 0F : x > 1F ? 1F : x;
        return x * x * (3F - 2F * x);
    }

    // ------------------------------------------------------------------ the rest

    @Override
    public Iterable<ItemStack> getArmorInventoryList() {
        return nothing;
    }

    @Override
    public ItemStack getItemStackFromSlot(EntityEquipmentSlot slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItemStackToSlot(EntityEquipmentSlot slot, ItemStack stack) {
    }

    @Override
    public EnumHandSide getPrimaryHand() {
        return EnumHandSide.RIGHT;
    }

    @Override
    public void knockBack(Entity source, float strength, double x, double z) {
    }

    @Override
    public boolean isPotionApplicable(PotionEffect effect) {
        return false;
    }

    @Override
    public boolean canBePushed() {
        return false;
    }

    @Override
    protected void collideWithNearbyEntities() {
    }

    @Override
    public boolean canBeCollidedWith() {
        return !isDead;
    }

    @Override
    public boolean isImmuneToExplosions() {
        return false;
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean isOnLadder() {
        return false;
    }

    /** Through a portal it does not fit; out of the vault it is carried (see {@link com.matoon.eternity.world.Armory}). */
    @Nullable
    @Override
    public Entity changeDimension(int dimension, ITeleporter teleporter) {
        return null;
    }

    @Override
    public boolean isNonBoss() {
        return false;
    }

    /** Its pilot made unseen (the Reality Stone) makes it unseen too. */
    @Override
    public boolean isInvisible() {
        return super.isInvisible() || hidesPilot() && StoneAbilities.unseen(pilot());
    }

    // ------------------------------------------------------------------ its dynamo, to anything that charges

    private final IEnergyStorage dynamo = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            if (!electric()) {
                return 0;
            }
            int took = Math.max(0, Math.min(maxReceive, energyCapacity() - energy()));
            return simulate ? took : charge(took);
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return electric() ? energy() : 0;
        }

        @Override
        public int getMaxEnergyStored() {
            return electric() ? energyCapacity() : 0;
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return electric();
        }
    };

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityEnergy.ENERGY && electric() || super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY && electric()) {
            return CapabilityEnergy.ENERGY.cast(dynamo);
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public boolean isInRangeToRenderDist(double distance) {
        return distance < 200D * 200D;
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_IRONGOLEM_HURT;
    }

    @Override
    protected float getSoundPitch() {
        return 0.4F + rand.nextFloat() * 0.15F;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return null;
    }

    @Override
    public float getEyeHeight() {
        return 3.85F * sized;
    }

    @Override
    public SoundCategory getSoundCategory() {
        return SoundCategory.NEUTRAL;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return getEntityBoundingBox().grow(3D);
    }

    @Override
    public boolean isPushedByWater() {
        return false;
    }
}
