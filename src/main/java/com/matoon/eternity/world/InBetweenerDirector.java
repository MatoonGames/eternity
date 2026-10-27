package com.matoon.eternity.world;

import com.matoon.eternity.EternityConfig;
import com.matoon.eternity.EternitySounds;
import com.matoon.eternity.compat.InfinityStones;
import com.matoon.eternity.compat.Relics;
import com.matoon.eternity.entity.EntityDestroyerArmor;
import com.matoon.eternity.entity.EntityInBetweener;
import com.matoon.eternity.entity.EntityOrderChaos;
import com.matoon.eternity.entity.EternityDamage;
import com.matoon.eternity.item.ItemAstralRegulator;
import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.network.MsgInBetweener;
import com.matoon.eternity.network.MsgInBetweenerAct;
import com.matoon.eternity.network.MsgTribunalFx;
import com.matoon.eternity.world.InBetweenerData.Memory;
import com.matoon.eternity.world.InBetweenerDialogue.Answer;
import com.matoon.eternity.world.InBetweenerDialogue.Node;
import lucraft.mods.lucraftcore.infinity.EnumInfinityStone;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.IProjectile;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SPacketSoundEffect;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Runs the In-Betweener. Made by Master Order and Lord Chaos to bridge them, and grown
 * greater than either, he was set at the one point in the Nexus where their powers meet
 * and cancel: a place with no opposites, no transitions, no dualities. To mock him they
 * left him a crystal bubble. Inside it he is omnipotent. Outside it, the neutrality
 * strips him of everything he is. He has never been outside it, and does not know.
 *
 * Walk up to his glass and the game begins (see {@link InBetweenerDialogue}). He cannot
 * reach you out there, so every move he makes is meant to draw you in; every move you
 * make should be meant to draw him out, without his seeing it. This class plays his side:
 * it keeps his ego, his hunger for the outside, his suspicion of you and how near he has
 * drawn you, and when it is his move it chooses what to try next, from how the game
 * stands and what he has already tried. Step into his glass and he reads you: a thief he
 * unmakes; anyone else he sends away with less of themselves than they came with.
 *
 * Win, and he breaks his glass with you, which takes a real blow; steps out, and falls.
 * Then the Soul Gem is there to be plucked from his brow, or he himself, to be drawn into
 * a charged Astral Regulator. Breaking his glass brings his masters; see
 * {@link OrderAndChaos}.
 */
public final class InBetweenerDirector {

    public static final InBetweenerDirector INSTANCE = new InBetweenerDirector();

    /** He turns to watch anyone this close. */
    private static final double NOTICE_RANGE = 48D;
    /** He speaks to anyone who comes this close to the middle of his glass. */
    private static final double TALK_RANGE = Nexus.BUBBLE_RADIUS + 7D;
    /** Walk farther than this and the conversation is over. */
    private static final double KEEP_RANGE = 30D;
    /** He is only there at all while someone is this close. */
    private static final double PRESENCE_RANGE = 96D;

    private static final int START_EGO = 3;
    /** How much he must want the outside before he speaks of leaving, and how little he may suspect you when he does. */
    private static final int HUNGER_TO_TURN = 6;
    private static final int SUSPICION_TO_TURN = 3;
    /** Past this much suspicion he sees your hand; this near the glass and you are in it. */
    private static final int SUSPICION_LIMIT = 6;
    private static final int PULL_LIMIT = 3;
    /** Moves before he declares the game a draw. */
    private static final int MAX_ROUNDS = 16;

    public static final int SHATTER_TICKS = 800;
    private static final int STEP_TICKS = 70;
    private static final int COLLAPSE_TICKS = 40;
    public static final int ABSORB_TICKS = 100;
    /** How long his empty body takes to drift out to where it comes to rest, how far out that is, and from how far it can be seen. */
    private static final int DRIFT_TICKS = 1800;
    private static final double DRIFT_MIN = 320D, DRIFT_MAX = 720D, DRIFT_SEEN = 160D;

    /** Inside his bubble with him: how big it swells, and when each of his warpings begins. */
    public static final double BATTLE_RADIUS = 15D;
    public static final int[] PHASES = {0, 160, 340, 520, 660, 760};
    private static final int TICKS_PER_MINUTE = 1200;
    private static final int[] YOUR_MOVES = {InBetweenerDialogue.YOUR_MOVE_1, InBetweenerDialogue.YOUR_MOVE_2, InBetweenerDialogue.YOUR_MOVE_3};

    private enum Stage { NONE, SPEAKING, SHATTERING, STEPPING, BATTLE, ABSORBING }

    private final Random rand = new Random();
    /** People who must step back from the glass before he will speak to them again. */
    private final Set<UUID> lingering = new HashSet<>();
    /** People he has already turned to look at, this approach. */
    private final Set<UUID> noticed = new HashSet<>();
    /** The gambits he has already tried on this visitor. */
    private final Set<Integer> tried = new HashSet<>();

    private InBetweenerData data;
    private EntityInBetweener body;
    private Stage stage = Stage.NONE;
    private int stageTicks;
    private UUID visitor;
    private int node = -1;
    private int mask;
    private byte beat;
    private int ego, hunger, suspicion, pull, facts, rounds, moves;
    /** How far you have got on his nerves, and whether he has blustered at you yet, and ranted. */
    private int irritation;
    private boolean bluffed, ranted;
    private boolean turned;
    /** The visitor is inside his glass. */
    private boolean inside;
    /** From the moment the glass breaks until he falls: broken, but not yet knowing it. */
    private boolean freeing;
    private float crack;
    private int lastStrike;
    private Vec3d fallenAt;
    private Vec3d stepFrom;
    private Vec3d frozenAt;
    private int lastMock;
    private EnumHand absorbHand = EnumHand.MAIN_HAND;

    private InBetweenerDirector() {
    }

    public void reset() {
        data = null;
        body = null;
        stage = Stage.NONE;
        visitor = null;
        node = -1;
        lingering.clear();
        noticed.clear();
        fallenAt = null;
        freeing = false;
        inside = false;
        OrderAndChaos.INSTANCE.reset();
    }

    public InBetweenerData data() {
        if (data == null) {
            data = InBetweenerData.get();
        }
        return data;
    }

    /** Whether the Soul Gem still burns on his brow. There is only one, so not if it is anywhere else. */
    public boolean wearsGem() {
        return EternityConfig.inbetweener.holdsSoulStone && !data().gemTaken && !data().inBetweenerAbsorbed && soulStone() != null
                && !RelicTracker.INSTANCE.soulElsewhere();
    }

    @Nullable
    public static Item soulStone() {
        return anvil.infinity.items.Items.SOUL_STONE;
    }

    /** Whom he is speaking to, or fighting, if anyone; for /eternity inbetweener status. */
    @Nullable
    public String visitorName(MinecraftServer server) {
        EntityPlayerMP player = visitor == null ? null : server.getPlayerList().getPlayerByUUID(visitor);
        return player == null || stage == Stage.NONE ? null : player.getName() + " (" + stage.name().toLowerCase() + ")";
    }

    /** Whole glass, gem on his brow, nobody remembered, nobody absorbed. For operators. */
    public void restore() {
        InBetweenerData d = data();
        abandon();
        OrderAndChaos.INSTANCE.vanish();
        d.broken = false;
        d.adrift = false;
        d.wrathAt = -1;
        d.gemTaken = false;
        d.gemHolder = "";
        d.inBetweenerAbsorbed = d.orderAbsorbed = d.chaosAbsorbed = false;
        d.memories.clear();
        d.castOut.clear();
        d.markDirty();
        lingering.clear();
        fallenAt = null;
        freeing = false;
        if (body != null) {
            body.setDead();
            body = null;
        }
    }

    // ------------------------------------------------------------------ main loop

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null || DimensionManager.getWorld(0) == null || server.getPlayerList().getPlayers().isEmpty()) {
            return;
        }
        InBetweenerData d = data();
        d.clock++;
        if (d.clock % 200 == 0) {
            d.markDirty();
        }
        WorldServer nexus = DimensionManager.getWorld(Nexus.dimension());
        if (d.wrathAt >= 0 && d.clock >= d.wrathAt) {
            wrath(nexus);
        }
        if (nexus == null || !EternityConfig.inbetweener.enabled) {
            abandon();
            vanish();
            return;
        }
        if (d.wrathAt >= 0 && d.clock % 100 == 0) {
            // Anyone who wandered in since the glass broke learns how long they have.
            EternityNetwork.sendToDimension(new MsgInBetweener(MsgInBetweener.WRATH_SOON, (int) (d.wrathAt - d.clock)), Nexus.dimension());
        }

        EntityPlayerMP player = visitorPlayer();
        if (d.adrift) {
            // Nothing is left at the junction but, for a while, his masters. He is out in the Nexus somewhere.
            mastersTalk(nexus, player);
            EntityInBetweener entity = drift(nexus);
            if (stage == Stage.ABSORBING) {
                stageTicks++;
                if (entity == null) {
                    abandon();
                } else {
                    absorbing(entity, player);
                }
            }
            return;
        }

        List<EntityPlayerMP> near = new ArrayList<>();
        for (EntityPlayer other : nexus.playerEntities) {
            if (other instanceof EntityPlayerMP && horizontal(other) < PRESENCE_RANGE) {
                near.add((EntityPlayerMP) other);
            }
        }
        if (near.isEmpty() && stage == Stage.NONE) {
            vanish();
            return;
        }
        mastersTalk(nexus, player);
        EntityInBetweener entity = summon(nexus);
        if (entity == null) {
            return;
        }
        if (!mastersSpeaking()) {
            stageTicks++;
        }
        barrier(nexus, entity);

        player = visitorPlayer();
        if (stage != Stage.NONE && !mastersSpeaking() && (player == null || player.world != nexus
                || stage != Stage.BATTLE && (!player.isEntityAlive() || horizontal(player) > KEEP_RANGE))) {
            abandon();
            player = null;
        }
        switch (stage) {
            case NONE:
                hold(entity);
                watch(entity, near);
                if (!OrderAndChaos.INSTANCE.active()) {
                    approach(near);
                }
                break;
            case SPEAKING:
                hold(entity);
                if (!mastersSpeaking()) {
                    if (beat == InBetweenerDialogue.B_TURN) {
                        entity.faceAway(player, 0.15F);
                    } else {
                        entity.face(player, 0.2F);
                    }
                    if (inside) {
                        confine(player, EntityInBetweener.bubbleCentre(), Nexus.BUBBLE_RADIUS - 1.2D);
                    }
                    speaking(player);
                }
                break;
            case SHATTERING:
                hold(entity);
                entity.face(player, 0.25F);
                shattering(entity, player);
                break;
            case STEPPING:
                stepping(entity, player);
                break;
            case BATTLE:
                battle(entity, player);
                break;
            case ABSORBING:
                absorbing(entity, player);
                break;
        }
    }

    /** Order and Chaos may speak to someone even when he himself is gone, into a Regulator or out into the Nexus. */
    private void mastersTalk(WorldServer nexus, @Nullable EntityPlayerMP player) {
        if (!mastersSpeaking()) {
            return;
        }
        if (player == null || player.world != nexus || !player.isEntityAlive()
                || horizontal(player) > EternityConfig.inbetweener.wrathRadius * 1.5D) {
            abandon();
        } else {
            stageTicks++;
            speaking(player);
        }
    }

    private void setStage(Stage next) {
        stage = next;
        stageTicks = 0;
    }

    private boolean mastersSpeaking() {
        return stage == Stage.SPEAKING && InBetweenerDialogue.node(node).speaker == InBetweenerDialogue.MASTERS;
    }

    @Nullable
    private EntityPlayerMP visitorPlayer() {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        return server == null || visitor == null ? null : server.getPlayerList().getPlayerByUUID(visitor);
    }

    private static double horizontal(Entity entity) {
        double dx = entity.posX - Nexus.JUNCTION_X, dz = entity.posZ - Nexus.JUNCTION_Z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    // ------------------------------------------------------------------ his body

    /** He is there while someone is near: in his glass, or lying where he fell. Not once he is in a Regulator. */
    @Nullable
    private EntityInBetweener summon(WorldServer nexus) {
        if (data().inBetweenerAbsorbed) {
            vanish();
            return null;
        }
        if (body != null && (body.isDead || body.world != nexus)) {
            body = null;
        }
        if (body == null) {
            EntityInBetweener entity = new EntityInBetweener(nexus);
            Vec3d at = data().broken ? fallenSpot() : hoverSpot();
            entity.setLocationAndAngles(at.x, at.y, at.z, 180F, 0F);
            entity.setState(data().broken ? EntityInBetweener.State.FALLEN : EntityInBetweener.State.HOVER);
            entity.setBubble(data().broken ? 0F : (float) Nexus.BUBBLE_RADIUS);
            entity.setDrain(data().broken ? 1F : 0F);
            body = entity;
            if (!nexus.spawnEntity(entity)) {
                body = null;
                return null;
            }
        }
        body.setGem(wearsGem());
        return body;
    }

    private void vanish() {
        if (body != null) {
            body.setDead();
            body = null;
        }
        noticed.clear();
    }

    /** Called by every In-Betweener each tick; any but the one this director moves dissolves. */
    public boolean claim(EntityInBetweener entity) {
        return entity == body;
    }

    private static Vec3d hoverSpot() {
        return new Vec3d(Nexus.JUNCTION_X, Nexus.BUBBLE_Y - 0.9D, Nexus.JUNCTION_Z);
    }

    private Vec3d fallenSpot() {
        return fallenAt != null ? fallenAt : new Vec3d(Nexus.JUNCTION_X, Nexus.JUNCTION_Y, Nexus.JUNCTION_Z + Nexus.BUBBLE_RADIUS + 1.5D);
    }

    // ------------------------------------------------------------------ adrift

    /**
     * There is only one In-Betweener and only one glass. Once his masters have come and found
     * nothing in him worth putting back, his empty body is cast off: it drifts away from the
     * junction, slowly, out over the islands, and hangs somewhere in the Nexus, turning over and
     * over, until someone finds it. A charged Regulator can still draw him in there, and the gem,
     * if nobody has taken it, is still on his brow.
     */
    private void castAdrift() {
        InBetweenerData d = data();
        if (d.adrift || d.inBetweenerAbsorbed) {
            return;
        }
        Vec3d from = body != null ? body.getPositionVector() : fallenSpot();
        double angle = rand.nextDouble() * Math.PI * 2D;
        double distance = DRIFT_MIN + rand.nextDouble() * (DRIFT_MAX - DRIFT_MIN);
        d.adrift = true;
        d.driftFromX = from.x;
        d.driftFromY = from.y;
        d.driftFromZ = from.z;
        d.driftX = Nexus.JUNCTION_X + Math.cos(angle) * distance;
        d.driftY = Nexus.JUNCTION_Y - 20D + rand.nextDouble() * 55D;
        d.driftZ = Nexus.JUNCTION_Z + Math.sin(angle) * distance;
        d.driftStart = d.clock;
        d.markDirty();
        fallenAt = null;
        freeing = false;
        if (body != null) {
            body.setState(EntityInBetweener.State.ADRIFT);
            body.world.playSound(null, body.posX, body.posY, body.posZ, EternitySounds.FAR_REVERSE, SoundCategory.HOSTILE, 3F, 0.4F);
        }
    }

    /** Where his body is now: on its way out, or hanging where it came to rest, rocking a little. */
    private Vec3d driftPosition() {
        InBetweenerData d = data();
        Vec3d to = new Vec3d(d.driftX, d.driftY, d.driftZ);
        double t = MathHelper.clamp((d.clock - d.driftStart) / (double) DRIFT_TICKS, 0D, 1D);
        double ease = t * t * (3D - 2D * t);
        Vec3d bob = new Vec3d(Math.sin(d.clock * 0.004D) * 1.5D, Math.sin(d.clock * 0.011D) * 0.6D, Math.cos(d.clock * 0.004D) * 1.5D);
        Vec3d from = new Vec3d(d.driftFromX, d.driftFromY, d.driftFromZ);
        return from.add(to.subtract(from).scale(ease)).add(bob.scale(ease));
    }

    /** His body is there while someone is near enough to see it; nobody else needs it. */
    @Nullable
    private EntityInBetweener drift(WorldServer nexus) {
        if (data().inBetweenerAbsorbed) {
            vanish();
            return null;
        }
        Vec3d at = driftPosition();
        boolean seen = false;
        for (EntityPlayer player : nexus.playerEntities) {
            if (!player.isSpectator() && player.getDistanceSq(at.x, at.y, at.z) < DRIFT_SEEN * DRIFT_SEEN) {
                seen = true;
                break;
            }
        }
        if (body != null && (body.isDead || body.world != nexus)) {
            body = null;
        }
        if (!seen && stage != Stage.ABSORBING) {
            vanish();
            return null;
        }
        if (body == null) {
            // Where nobody has loaded the void around him yet, there is nowhere to put him; try again shortly.
            if (data().clock % 10 != 0) {
                return null;
            }
            EntityInBetweener entity = new EntityInBetweener(nexus);
            entity.setLocationAndAngles(at.x, at.y, at.z, rand.nextFloat() * 360F, 0F);
            entity.setState(EntityInBetweener.State.ADRIFT);
            entity.setBubble(0F);
            entity.setDrain(1F);
            if (!nexus.spawnEntity(entity)) {
                return null;
            }
            body = entity;
        }
        body.setGem(wearsGem());
        if (stage != Stage.ABSORBING) {
            body.setState(EntityInBetweener.State.ADRIFT);
            body.setBubble(0F);
            body.setDrain(1F);
            body.setPosition(at.x, at.y, at.z);
        }
        return body;
    }

    /** Where his body hangs, for /eternity inbetweener status; null unless it has been cast adrift. */
    @Nullable
    public Vec3d driftPoint() {
        return data().adrift && !data().inBetweenerAbsorbed ? driftPosition() : null;
    }

    /**
     * Puts him back: whole glass, colour back in him, the gem on his brow if it is still his to
     * wear. What he remembers of everyone stays. The way in for whatever brings him back one day.
     */
    public void revive() {
        InBetweenerData d = data();
        abandon();
        d.broken = false;
        d.adrift = false;
        d.inBetweenerAbsorbed = false;
        d.wrathAt = -1;
        d.markDirty();
        fallenAt = null;
        freeing = false;
        if (body != null) {
            body.setDead();
            body = null;
        }
    }

    /** Where he should be when nothing is happening: standing in his glass, or lying where he fell. */
    private void hold(EntityInBetweener entity) {
        if (freeing) {
            entity.setBubble(0F);
            entity.glide(hoverSpot(), 0.1D);
            return;
        }
        if (data().broken) {
            if (stage != Stage.ABSORBING) {
                entity.setState(EntityInBetweener.State.FALLEN);
            }
            entity.setBubble(0F);
            entity.setDrain(1F);
            Vec3d at = fallenSpot();
            entity.setPosition(at.x, at.y, at.z);
            return;
        }
        entity.setBubble((float) Nexus.BUBBLE_RADIUS);
        entity.setDrain(0F);
        entity.glide(hoverSpot().add(0D, Math.sin(entity.ticksExisted * 0.05D) * 0.15D, 0D), 0.1D);
        if (stage == Stage.NONE || mastersSpeaking()) {
            entity.setState(EntityInBetweener.State.HOVER);
        }
    }

    /** He watches whoever is nearest, and lets them know he has seen them. */
    private void watch(EntityInBetweener entity, List<EntityPlayerMP> near) {
        EntityPlayerMP nearest = null;
        double best = NOTICE_RANGE;
        for (EntityPlayerMP player : near) {
            double d = player.getDistance(entity);
            if (!player.isSpectator() && d < best) {
                best = d;
                nearest = player;
            }
            if (d < NOTICE_RANGE && !player.isSpectator() && noticed.add(player.getUniqueID()) && !data().broken) {
                status(player, "At the heart of the grey, something deigns to notice you.", TextFormatting.GRAY);
                player.connection.sendPacket(new SPacketSoundEffect(EternitySounds.FAR_REVERSE,
                        SoundCategory.HOSTILE, entity.posX, entity.posY, entity.posZ, 1.5F, 0.6F));
            } else if (d > NOTICE_RANGE + 16D) {
                noticed.remove(player.getUniqueID());
            }
        }
        if (nearest != null && !data().broken) {
            entity.face(nearest, 0.08F);
        }
    }

    /** Someone has come right up to the glass. */
    private void approach(List<EntityPlayerMP> near) {
        InBetweenerData d = data();
        for (EntityPlayerMP player : near) {
            UUID id = player.getUniqueID();
            double h = horizontal(player);
            if (h > TALK_RANGE + 4D) {
                lingering.remove(id);
            }
            if (player.isSpectator() || !player.isEntityAlive() || h > TALK_RANGE || Math.abs(player.posY - Nexus.JUNCTION_Y) > 9D
                    || lingering.contains(id)) {
                continue;
            }
            begin(player);
            return;
        }
    }

    // ------------------------------------------------------------------ the game

    private void begin(EntityPlayerMP player) {
        InBetweenerData d = data();
        visitor = player.getUniqueID();
        ego = START_EGO;
        hunger = suspicion = pull = facts = rounds = moves = irritation = 0;
        turned = bluffed = ranted = false;
        inside = false;
        tried.clear();
        Memory memory = d.memory(visitor);
        int opening;
        if (d.broken) {
            opening = InBetweenerDialogue.FALLEN;
        } else if (refusing(visitor)) {
            // He has his back to you, and is not speaking to you. That is all he can do; it does not make you go.
            opening = InBetweenerDialogue.SULK;
            suspicion = 2;
        } else if (memory == Memory.THIEF) {
            opening = InBetweenerDialogue.RETURN_THIEF;
        } else if (memory == Memory.WORTHY) {
            opening = InBetweenerDialogue.RETURN_WORTHY;
            ego++;
        } else if (memory == Memory.DOUBTER) {
            opening = InBetweenerDialogue.RETURN_DOUBTER;
        } else if (memory == Memory.FOUGHT) {
            opening = InBetweenerDialogue.RETURN_FOUGHT;
        } else if (memory == Memory.LURED) {
            opening = InBetweenerDialogue.RETURN_LURED;
        } else if (memory == Memory.MET) {
            opening = InBetweenerDialogue.RETURN_MET;
            // He has seen your face before; he trusts it a little less.
            suspicion = 1;
        } else {
            opening = wearsGem() ? InBetweenerDialogue.FIRST : InBetweenerDialogue.FIRST_EMPTY;
        }
        speak(player, opening);
    }

    private void speak(EntityPlayerMP player, int id) {
        node = id;
        if ((id == InBetweenerDialogue.BLUFF || id == InBetweenerDialogue.ELIM_1) && visitor != null) {
            // He has broken his own silence; whatever he said about not speaking to you is over.
            data().castOut.remove(visitor);
        }
        Node n = InBetweenerDialogue.node(id);
        beat = n.beat;
        effect(player, n.effect);
        mask = 0;
        for (int i = 0; i < n.answers.length; i++) {
            if (allowed(player, n.answers[i])) {
                mask |= 1 << i;
            }
        }
        setStage(Stage.SPEAKING);
        if (n.speaker == InBetweenerDialogue.MASTERS) {
            // Two voices that fill the whole junction: one like law being read aloud, one like laughter in a storm.
            voice(player, SoundEvents.ENTITY_ENDERDRAGON_GROWL, 1.2F, 0.4F);
            voice(player, EternitySounds.ENTITY_GLITCH, 1F, 0.6F);
        } else {
            if (body != null && body.getState() != EntityInBetweener.State.FALLEN) {
                body.setState(pose(id, beat));
            }
            // Two voices, one high and one low, a breath apart; fallen, only one.
            boolean faint = data().broken;
            voice(player, EternitySounds.ENTITY_VOICE, faint ? 0.6F : 1.4F, faint ? 0.75F : 1.1F);
            if (!faint) {
                voice(player, EternitySounds.ENTITY_VOICE, 1.2F, 0.55F);
            }
        }
        EternityNetwork.send(new MsgInBetweener(MsgInBetweener.SPEAK, id, mask, beat), player);
    }

    /** How he holds himself while he says something. */
    private static EntityInBetweener.State pose(int id, byte beat) {
        if (id == InBetweenerDialogue.FREE || id == InBetweenerDialogue.FREE_PROUD) {
            return EntityInBetweener.State.FREE;
        }
        switch (beat) {
            case InBetweenerDialogue.B_ASCEND:
            case InBetweenerDialogue.B_STARS:
                return EntityInBetweener.State.ASCEND;
            case InBetweenerDialogue.B_SIT:
                return EntityInBetweener.State.HOVER;
            case InBetweenerDialogue.B_TAP:
                return EntityInBetweener.State.TAP;
            case InBetweenerDialogue.B_BECKON:
                return EntityInBetweener.State.BECKON;
            case InBetweenerDialogue.B_OFFER:
                return EntityInBetweener.State.OFFER;
            case InBetweenerDialogue.B_CRACK:
                return EntityInBetweener.State.PRESS;
            case InBetweenerDialogue.B_LAUGH:
                return EntityInBetweener.State.LAUGH;
            case InBetweenerDialogue.B_SWALLOW:
                return EntityInBetweener.State.WARP;
            default:
                return EntityInBetweener.State.SPEAK;
        }
    }

    private boolean allowed(EntityPlayerMP player, Answer answer) {
        int req = answer.req;
        if ((req & InBetweenerDialogue.REQ_GEM) != 0 && !wearsGem()) {
            return false;
        }
        if ((req & InBetweenerDialogue.REQ_REGULATOR) != 0 && !ItemAstralRegulator.holdsRegulator(player)) {
            return false;
        }
        if ((req & InBetweenerDialogue.REQ_ETERNITY) != 0 && !ItemAstralRegulator.holds(player, ItemAstralRegulator.ETERNITY)) {
            return false;
        }
        if ((req & InBetweenerDialogue.REQ_PLANNED) != 0 && (facts & InBetweenerDialogue.F_PLANNED) == 0) {
            return false;
        }
        if ((req & InBetweenerDialogue.REQ_TRAP) != 0 && (facts & InBetweenerDialogue.F_TRAP) == 0) {
            return false;
        }
        return (req & InBetweenerDialogue.REQ_HOLDS_SOUL) == 0 || findSoulStone(player) >= 0;
    }

    /** How long a speech without answers is left on screen before it moves on. */
    private static int readingTicks(int id) {
        String plain = TextFormatting.getTextWithoutFormattingCodes(InBetweenerDialogue.node(id).speech);
        return (int) ((plain == null ? 0 : plain.length()) / 1.4F) + 20;
    }

    private void speaking(EntityPlayerMP player) {
        if (!InBetweenerDialogue.isQuestion(node) || mask == 0) {
            if (stageTicks >= readingTicks(node) + 100) {
                go(player, InBetweenerDialogue.node(node).then);
            }
        }
    }

    /** The visitor answers, asks him to go on, or strikes the glass. Only the visitor, only when it makes sense. */
    public void act(EntityPlayerMP player, byte action, int question, int choice) {
        if (!player.getUniqueID().equals(visitor) || player.world.provider.getDimension() != Nexus.dimension()) {
            return;
        }
        if (action == MsgInBetweenerAct.STRIKE) {
            strike(player);
            return;
        }
        if (stage != Stage.SPEAKING || question != node) {
            return;
        }
        Node n = InBetweenerDialogue.node(node);
        if (action == MsgInBetweenerAct.CONTINUE) {
            if ((!n.isQuestion() || mask == 0) && stageTicks >= Math.min(readingTicks(node), 30)) {
                go(player, n.then);
            }
            return;
        }
        if (choice < 0 || choice >= n.answers.length || (mask & (1 << choice)) == 0) {
            return;
        }
        Answer answer = n.answers[choice];
        ego += answer.ego;
        hunger += answer.hunger;
        pull = Math.max(0, pull + answer.pull);
        irritation += answer.annoy;
        if (answer.consistentWith != 0 && (facts & answer.consistentWith) != 0) {
            // It fits everything you have shown him so far. That, he believes.
            suspicion -= 1;
        } else {
            suspicion += answer.suspicion;
        }
        if ((facts & answer.contradicts) != 0) {
            // You said otherwise, not long ago. He is a being of contradictions; he knows one when he hears it.
            suspicion += 3;
        }
        suspicion = Math.max(0, suspicion);
        facts |= answer.sets;
        go(player, n.speaker == InBetweenerDialogue.HIM && !data().broken ? checked(answer.next) : answer.next);
    }

    /** Whatever was meant to come next, the state of the game can overrule it. */
    private int checked(int next) {
        if (pull >= PULL_LIMIT) {
            return InBetweenerDialogue.LURED;
        }
        if (suspicion >= SUSPICION_LIMIT) {
            return InBetweenerDialogue.SEEN_THROUGH;
        }
        if (ego <= 0 && !cornered(next)) {
            return InBetweenerDialogue.OFFENDED;
        }
        // Get on his nerves and he blusters; keep at it and he loses his composure. Neither does anything to you.
        if (irritation >= 5 && !ranted && next != InBetweenerDialogue.OFFENDED) {
            ranted = true;
            return InBetweenerDialogue.RANT;
        }
        if (irritation >= 3 && !bluffed && next != InBetweenerDialogue.RANT && next != InBetweenerDialogue.BLUFF_DARE) {
            bluffed = true;
            return InBetweenerDialogue.BLUFF;
        }
        if (next == InBetweenerDialogue.BLUFF) {
            bluffed = true;
        }
        if (next == InBetweenerDialogue.RANT) {
            ranted = true;
        }
        return next;
    }

    /**
     * Whether the next thing he must say is part of his argument coming apart, or the truth
     * about himself. He cannot storm off from that: storming off is something he cannot do.
     */
    private static boolean cornered(int next) {
        return next == InBetweenerDialogue.ELIM_1 || next == InBetweenerDialogue.ELIM_2 || next == InBetweenerDialogue.ELIM_3
                || next == InBetweenerDialogue.ELIM_DENY || next == InBetweenerDialogue.BLUFF || next == InBetweenerDialogue.BLUFF_DARE
                || next == InBetweenerDialogue.RANT || next == InBetweenerDialogue.TRUTH;
    }

    private void go(EntityPlayerMP player, int next) {
        switch (next) {
            case InBetweenerDialogue.END:
                finish(player);
                break;
            case InBetweenerDialogue.REFUSE:
                refuse(player);
                break;
            case InBetweenerDialogue.SPIT:
                spit(player);
                break;
            case InBetweenerDialogue.BATTLE:
                beginBattle(player);
                break;
            case InBetweenerDialogue.NEXT:
                speak(player, hisMove());
                break;
            case InBetweenerDialogue.PROVOKE:
                // Only a swollen ego, and a mind that does not suspect the trap, storms out of its castle to answer a dare.
                if (ego >= 4 && suspicion <= SUSPICION_TO_TURN) {
                    speak(player, InBetweenerDialogue.PROUD);
                } else {
                    suspicion += 2;
                    speak(player, suspicion >= SUSPICION_LIMIT ? InBetweenerDialogue.SEEN_THROUGH : InBetweenerDialogue.UNMOVED_DARE);
                }
                break;
            case InBetweenerDialogue.READ:
                // Inside, he sees what you are: a thief, or only curiosity.
                boolean thief = (facts & (InBetweenerDialogue.F_GREED | InBetweenerDialogue.F_EAGER)) != 0
                        || data().memory(player.getUniqueID()) == Memory.THIEF;
                speak(player, thief ? InBetweenerDialogue.READ_THIEF : InBetweenerDialogue.READ_CURIOUS);
                break;
            case InBetweenerDialogue.SHATTER:
                beginShatter(player);
                break;
            case InBetweenerDialogue.STEP:
                beginStep(player);
                break;
            case InBetweenerDialogue.MASTERS_FIGHT:
                OrderAndChaos.INSTANCE.fight();
                finish(player);
                break;
            case InBetweenerDialogue.ABSORB:
                speak(player, InBetweenerDialogue.ABSORBED);
                beginAbsorb(player, isRegulator(player.getHeldItemMainhand()) ? EnumHand.MAIN_HAND : EnumHand.OFF_HAND);
                break;
            default:
                speak(player, next);
                break;
        }
    }

    /**
     * His move. Once he wants the outside enough, and suspects you little enough, he raises
     * leaving himself. Otherwise he alternates between trying to draw you in, choosing the
     * lure that suits what he has seen of you, and leaving the next move to you. Past
     * MAX_ROUNDS, he calls it a draw.
     */
    private int hisMove() {
        rounds++;
        if (rounds > MAX_ROUNDS) {
            return InBetweenerDialogue.DRAW;
        }
        if (!turned && hunger >= HUNGER_TO_TURN && suspicion <= SUSPICION_TO_TURN) {
            turned = true;
            return InBetweenerDialogue.T_TURN;
        }
        int gambit = chooseGambit();
        if (rounds % 2 == 1 && gambit >= 0) {
            tried.add(gambit);
            return gambit;
        }
        return YOUR_MOVES[moves++ % YOUR_MOVES.length];
    }

    /** The lure that suits what he has seen: the gem for the greedy, fear for the proud, subtlety for the wary. */
    private int chooseGambit() {
        if ((facts & InBetweenerDialogue.F_GREED) != 0 && wearsGem() && !tried.contains(InBetweenerDialogue.G_GEM_BAIT)) {
            return InBetweenerDialogue.G_GEM_BAIT;
        }
        if (ego <= 2 && !tried.contains(InBetweenerDialogue.G_FEAR)) {
            return InBetweenerDialogue.G_FEAR;
        }
        if ((facts & InBetweenerDialogue.F_RULE) != 0) {
            if (!tried.contains(InBetweenerDialogue.G_CRACK)) {
                return InBetweenerDialogue.G_CRACK;
            }
            if (!tried.contains(InBetweenerDialogue.G_MERCY)) {
                return InBetweenerDialogue.G_MERCY;
            }
        }
        if (!tried.contains(InBetweenerDialogue.G_SHOW)) {
            return InBetweenerDialogue.G_SHOW;
        }
        for (int gambit : InBetweenerDialogue.GAMBITS) {
            if (!tried.contains(gambit) && (gambit != InBetweenerDialogue.G_GEM_BAIT || wearsGem())) {
                return gambit;
            }
        }
        return -1;
    }

    private void effect(EntityPlayerMP player, int effect) {
        InBetweenerData d = data();
        UUID id = player.getUniqueID();
        switch (effect) {
            case InBetweenerDialogue.FX_TAKE:
                if (giveGem(player)) {
                    d.remember(id, Memory.THIEF);
                    TextComponentString notice = new TextComponentString(player.getName() + " has stolen the Soul Gem from the In-Betweener.");
                    notice.getStyle().setColor(TextFormatting.GOLD);
                    player.server.getPlayerList().sendMessage(notice);
                }
                break;
            case InBetweenerDialogue.FX_BOON:
                boon(player);
                d.remember(id, Memory.WORTHY);
                break;
            case InBetweenerDialogue.FX_DOUBT:
                d.remember(id, Memory.DOUBTER);
                break;
            case InBetweenerDialogue.FX_LURE:
                swallow(player);
                break;
            case InBetweenerDialogue.FX_CURSE:
                curse(player);
                d.remember(id, Memory.LURED);
                break;
            case InBetweenerDialogue.FX_PLANNED:
                facts |= InBetweenerDialogue.F_PLANNED;
                break;
            case InBetweenerDialogue.FX_EXPOSED:
                // The truth is out, and he is the In-Betweener still. He draws himself back up.
                facts |= InBetweenerDialogue.F_RULE;
                ego = Math.max(ego, START_EGO);
                break;
            case InBetweenerDialogue.FX_RETURN:
                returnGem(player);
                OrderAndChaos.INSTANCE.gemReturned();
                break;
            case InBetweenerDialogue.FX_RESONANCE:
                OrderAndChaos.INSTANCE.resonate();
                break;
            case InBetweenerDialogue.FX_ORDER_ANGERED:
                OrderAndChaos.INSTANCE.angerOrder();
                break;
            case InBetweenerDialogue.FX_PRIDE:
                OrderAndChaos.INSTANCE.angerOrder();
                OrderAndChaos.INSTANCE.amuseChaos();
                break;
            case InBetweenerDialogue.FX_BOTH_ANGERED:
                OrderAndChaos.INSTANCE.angerBoth();
                break;
            default:
                break;
        }
        if (d.memory(id) == Memory.NONE && InBetweenerDialogue.node(node).speaker == InBetweenerDialogue.HIM) {
            d.remember(id, Memory.MET);
        }
    }

    /** The glass opens, and closes around them. They are on his board now. */
    private void swallow(EntityPlayerMP player) {
        inside = true;
        Vec3d centre = EntityInBetweener.bubbleCentre();
        Vec3d away = new Vec3d(player.posX - centre.x, 0D, player.posZ - centre.z);
        away = away.lengthSquared() < 1.0E-4 ? new Vec3d(0D, 0D, 1D) : away.normalize();
        Vec3d at = centre.add(away.scale(3.5D)).add(0D, -1D, 0D);
        EntityDestroyerArmor.place(player, at.x, at.y, at.z, player.rotationYaw, player.rotationPitch);
        player.world.playSound(null, centre.x, centre.y, centre.z, EternitySounds.RIFT_OPEN, SoundCategory.HOSTILE, 4F, 0.6F);
        player.world.playSound(null, centre.x, centre.y, centre.z, SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 2F, 0.4F);
    }

    /** Less of yourself than you came with: weak, slow and dull for a long while. */
    private static void curse(EntityPlayerMP player) {
        player.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, 20 * TICKS_PER_MINUTE, 1));
        player.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 20 * TICKS_PER_MINUTE, 0));
        player.addPotionEffect(new PotionEffect(MobEffects.MINING_FATIGUE, 20 * TICKS_PER_MINUTE, 1));
        player.addExperienceLevel(-Math.min(player.experienceLevel, 10));
    }

    /**
     * Something of the inside of his lens, poured out into a mortal: thirty levels of
     * experience, and for a while the strength and endurance of something not quite mortal.
     */
    private static void boon(EntityPlayerMP player) {
        player.addExperienceLevel(30);
        player.heal(player.getMaxHealth());
        player.addPotionEffect(new PotionEffect(MobEffects.STRENGTH, 20 * TICKS_PER_MINUTE, 1));
        player.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, 20 * TICKS_PER_MINUTE, 1));
        player.addPotionEffect(new PotionEffect(MobEffects.REGENERATION, 1200, 1));
        player.addPotionEffect(new PotionEffect(MobEffects.ABSORPTION, 2400, 3));
        voice(player, SoundEvents.ENTITY_PLAYER_LEVELUP, 1F, 0.5F);
        voice(player, EternitySounds.OMEN_SHIMMER, 1.5F, 0.7F);
    }

    /** The Soul Gem leaves his brow and is yours. */
    private boolean giveGem(EntityPlayerMP player) {
        InBetweenerData d = data();
        Item stone = soulStone();
        if (!wearsGem() || stone == null) {
            return false;
        }
        d.gemTaken = true;
        d.gemHolder = player.getName();
        d.markDirty();
        ItemStack stack = new ItemStack(stone);
        // The gem from his brow is the one true Soul Stone.
        Relics.claim(stack, player);
        if (!player.inventory.addItemStackToInventory(stack)) {
            player.dropItem(stack, false);
        }
        player.inventoryContainer.detectAndSendChanges();
        if (body != null) {
            body.setGem(false);
        }
        voice(player, EternitySounds.OMEN_SHIMMER, 1.5F, 0.8F);
        voice(player, EternitySounds.RIFT_CLOSE, 1F, 1.4F);
        return true;
    }

    /** Where in their inventory someone carries a loose Soul Stone, the true one first, or -1. */
    private static int findSoulStone(EntityPlayerMP player) {
        Item stone = soulStone();
        if (stone == null) {
            return -1;
        }
        int found = -1;
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (stack.getItem() == stone) {
                if (Relics.isTrue(stack)) {
                    return i;
                }
                if (found < 0) {
                    found = i;
                }
            }
        }
        return found;
    }

    /** Order and Chaos tear the gem out of your hand and send it back to his brow. */
    private void returnGem(EntityPlayerMP player) {
        int slot = findSoulStone(player);
        if (slot < 0) {
            return;
        }
        if (Relics.isTrue(player.inventory.getStackInSlot(slot))) {
            // Back on his brow, it is nobody's until it is taken again.
            Relics.release(Relics.Kind.SOUL_STONE);
        }
        player.inventory.getStackInSlot(slot).shrink(1);
        player.inventoryContainer.detectAndSendChanges();
        InBetweenerData d = data();
        d.gemTaken = false;
        d.gemHolder = "";
        d.markDirty();
        if (body != null) {
            body.setGem(true);
        }
        voice(player, EternitySounds.RIFT_OPEN, 1.2F, 1.5F);
    }

    /** The conversation is over. They may go, and must step back before he speaks to them again. */
    private void finish(EntityPlayerMP player) {
        setStage(Stage.NONE);
        visitor = null;
        inside = false;
        lingering.add(player.getUniqueID());
        EternityNetwork.send(new MsgInBetweener(MsgInBetweener.CLOSE, 0), player);
    }

    /** Whoever he was dealing with has gone: walked off, logged out, died. It simply stops. */
    private void abandon() {
        if (stage == Stage.NONE) {
            return;
        }
        boolean masters = mastersSpeaking();
        freeing = false;
        inside = false;
        EntityPlayerMP player = visitorPlayer();
        if (player != null) {
            EternityNetwork.send(new MsgInBetweener(stage == Stage.BATTLE ? MsgInBetweener.BATTLE_END : MsgInBetweener.CLOSE, 0), player);
            lingering.add(player.getUniqueID());
        }
        if (stage == Stage.SHATTERING || stage == Stage.BATTLE) {
            crack = 0F;
            if (body != null) {
                body.setCrack(0F);
                body.setBubble((float) Nexus.BUBBLE_RADIUS);
            }
        }
        if (stage == Stage.STEPPING && body != null) {
            // However far he got, he falls where he stands.
            fallenAt = new Vec3d(body.posX, Nexus.JUNCTION_Y, body.posZ);
        }
        if (stage == Stage.ABSORBING && body != null) {
            body.setState(EntityInBetweener.State.FALLEN);
        }
        setStage(Stage.NONE);
        visitor = null;
        if (masters) {
            // Walking away from Order and Chaos mid-sentence does not end the conversation. It ends the talking.
            OrderAndChaos.INSTANCE.fight();
        }
    }

    /**
     * He turns his back and will not speak to you for a while. Outside his glass that is the
     * whole of his power over you: he can withhold his words. He cannot make you go, and
     * nothing stops you standing there, or tapping on the glass, until he speaks again.
     */
    private void refuse(EntityPlayerMP player) {
        InBetweenerData d = data();
        long until = d.clock + (long) EternityConfig.inbetweener.castOutMinutes * TICKS_PER_MINUTE;
        Long current = d.castOut.get(player.getUniqueID());
        if (current == null || current < until) {
            d.castOut.put(player.getUniqueID(), until);
        }
        if (d.memory(player.getUniqueID()) == Memory.NONE) {
            d.remember(player.getUniqueID(), Memory.MET);
        }
        d.markDirty();
        finish(player);
        if (body != null) {
            body.faceAway(player, 1F);
        }
    }

    /** Whether he has his back to someone. */
    private boolean refusing(UUID id) {
        Long until = data().castOut.get(id);
        return until != null && until > data().clock;
    }

    /** Inside his glass he can do anything; he puts them back out of it, just outside where they came in. */
    private void spit(EntityPlayerMP player) {
        Vec3d centre = EntityInBetweener.bubbleCentre();
        Vec3d away = new Vec3d(player.posX - centre.x, 0D, player.posZ - centre.z);
        away = away.lengthSquared() < 1.0E-4 ? new Vec3d(0D, 0D, 1D) : away.normalize();
        Vec3d out = new Vec3d(centre.x, Nexus.JUNCTION_Y, centre.z).add(away.scale(Nexus.BUBBLE_RADIUS + 2.5D));
        inside = false;
        EntityDestroyerArmor.place(player, out.x, out.y, out.z, player.rotationYaw, player.rotationPitch);
        player.world.playSound(null, out.x, out.y, out.z, EternitySounds.RIFT_CLOSE, SoundCategory.HOSTILE, 3F, 1.4F);
        EternityNetwork.send(new MsgInBetweener(MsgInBetweener.CAST_OUT, 0), player);
        finish(player);
    }

    // ------------------------------------------------------------------ the shattering

    private void beginShatter(EntityPlayerMP player) {
        setStage(Stage.SHATTERING);
        crack = 0F;
        lastStrike = 0;
        if (body != null) {
            body.setState(EntityInBetweener.State.PRESS);
            body.setCrack(0F);
        }
        player.world.playSound(null, Nexus.JUNCTION_X, Nexus.BUBBLE_Y, Nexus.JUNCTION_Z, EternitySounds.AMBIENT_RUMBLE, SoundCategory.HOSTILE, 4F, 0.7F);
        EternityNetwork.send(new MsgInBetweener(MsgInBetweener.SHATTER, SHATTER_TICKS, (int) Math.ceil(EternityConfig.inbetweener.shatterBlow)), player);
    }

    /** He presses from within; the cracks grow only under real blows, and close up when they stop. */
    private void shattering(EntityInBetweener entity, EntityPlayerMP player) {
        if (stageTicks - lastStrike > 60) {
            crack = Math.max(0F, crack - 0.003F);
        }
        entity.setCrack(crack);
        if (stageTicks % 30 == 0) {
            player.world.playSound(null, Nexus.JUNCTION_X, Nexus.BUBBLE_Y, Nexus.JUNCTION_Z, SoundEvents.BLOCK_GLASS_HIT, SoundCategory.HOSTILE,
                    2F, 0.5F + crack * 0.5F);
        }
        if (crack >= 1F) {
            shattered(entity, player);
        } else if (stageTicks >= SHATTER_TICKS) {
            crack = 0F;
            entity.setCrack(0F);
            entity.setState(EntityInBetweener.State.HOVER);
            EternityNetwork.send(new MsgInBetweener(MsgInBetweener.CLOSE, 0), player);
            speak(player, InBetweenerDialogue.HESITATE);
        }
    }

    /**
     * A blow at the glass. It counts only from close to it and facing it, and only if it
     * truly lands: the force of the weapon in hand, its enchantments, how fully the swing was
     * drawn back, a critical if you came down on it, doubled by a charged Regulator or the
     * Power Stone. Anything weaker than shatterBlow glances off and is not even noticed.
     */
    private void strike(EntityPlayerMP player) {
        if (stage != Stage.SHATTERING || stageTicks - lastStrike < 4 || player.isSpectator()) {
            return;
        }
        Vec3d eyes = player.getPositionEyes(1F);
        Vec3d toCentre = EntityInBetweener.bubbleCentre().subtract(eyes);
        double surface = toCentre.length() - Nexus.BUBBLE_RADIUS;
        if (surface > 8D || surface < -1D || player.getLook(1F).dotProduct(toCentre.normalize()) < 0.55D) {
            return;
        }
        lastStrike = stageTicks;
        float charge = player.getCooledAttackStrength(0.5F);
        float force = (float) player.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue();
        float enchant = EnchantmentHelper.getModifierForCreature(player.getHeldItemMainhand(), EnumCreatureAttribute.UNDEFINED);
        force = force * (0.2F + charge * charge * 0.8F) + enchant * charge;
        if (charge > 0.9F && player.fallDistance > 0F && !player.onGround) {
            force *= 1.5F;
        }
        if (ItemAstralRegulator.holdsCharged(player) || (InfinityStones.scan(player) & InfinityStones.bit(EnumInfinityStone.POWER)) != 0) {
            force *= 2F;
        }
        player.resetCooldown();
        double needed = EternityConfig.inbetweener.shatterBlow;
        if (force < needed) {
            status(player, String.format("The blow glances off. It does not even notice. (%.0f of %.0f)", force, needed), TextFormatting.GRAY);
            player.world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.BLOCK_GLASS_HIT, SoundCategory.HOSTILE, 1F, 1.6F);
            return;
        }
        crack += (float) (force / EternityConfig.inbetweener.shatterStrength);
        status(player, String.format("The glass cracks. (%.0f)", force), TextFormatting.WHITE);
        player.world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE,
                1.2F, 0.4F + crack * 0.6F);
        player.world.playSound(null, player.posX, player.posY, player.posZ, EternitySounds.COMET_IMPACT, SoundCategory.HOSTILE, 0.8F, 1.4F);
    }

    private void shattered(EntityInBetweener entity, EntityPlayerMP player) {
        InBetweenerData d = data();
        d.broken = true;
        d.wrathAt = d.clock + EternityConfig.inbetweener.wrathSeconds * 20L;
        d.markDirty();
        crack = 0F;
        entity.setCrack(0F);
        entity.setBubble(0F);
        WorldServer world = (WorldServer) entity.world;
        world.playSound(null, Nexus.JUNCTION_X, Nexus.BUBBLE_Y, Nexus.JUNCTION_Z, SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 8F, 0.5F);
        world.playSound(null, Nexus.JUNCTION_X, Nexus.BUBBLE_Y, Nexus.JUNCTION_Z, EternitySounds.COMET_BLAST, SoundCategory.HOSTILE, 8F, 0.8F);
        world.playSound(null, Nexus.JUNCTION_X, Nexus.BUBBLE_Y, Nexus.JUNCTION_Z, EternitySounds.RIFT_IMPACT, SoundCategory.HOSTILE, 8F, 0.6F);
        EternityNetwork.sendToDimension(new MsgInBetweener(MsgInBetweener.SHATTERED, 0), Nexus.dimension());
        if (!mastersGone()) {
            EternityNetwork.sendToDimension(new MsgInBetweener(MsgInBetweener.WRATH_SOON, (int) (d.wrathAt - d.clock)), Nexus.dimension());
        }
        speak(player, (facts & InBetweenerDialogue.F_TRAP) != 0 ? InBetweenerDialogue.FREE_PROUD : InBetweenerDialogue.FREE);
        stepFrom = entity.getPositionVector();
        freeing = true;
    }

    /** Whether both of his makers are inside Regulators, so nobody is left to come and put him back. */
    private boolean mastersGone() {
        return data().orderAbsorbed && data().chaosAbsorbed;
    }

    // ------------------------------------------------------------------ the step, and the fall

    private void beginStep(EntityPlayerMP player) {
        setStage(Stage.STEPPING);
        if (body != null) {
            stepFrom = body.getPositionVector();
            Vec3d toward = new Vec3d(player.posX - Nexus.JUNCTION_X, 0D, player.posZ - Nexus.JUNCTION_Z);
            toward = toward.lengthSquared() < 1.0E-4 ? new Vec3d(0D, 0D, 1D) : toward.normalize();
            fallenAt = new Vec3d(Nexus.JUNCTION_X, Nexus.JUNCTION_Y, Nexus.JUNCTION_Z).add(toward.scale(Nexus.BUBBLE_RADIUS + 1.5D));
            body.setState(EntityInBetweener.State.STEP);
        }
        EternityNetwork.send(new MsgInBetweener(MsgInBetweener.CLOSE, 0), player);
    }

    /** He walks down out of the air, triumphant, crosses where the glass was, and falls. */
    private void stepping(EntityInBetweener entity, @Nullable EntityPlayerMP player) {
        Vec3d to = fallenSpot();
        if (stageTicks <= STEP_TICKS) {
            float t = stageTicks / (float) STEP_TICKS;
            float ease = t * t * (3F - 2F * t);
            Vec3d from = stepFrom != null ? stepFrom : hoverSpot();
            entity.setPosition(from.x + (to.x - from.x) * ease, from.y + (to.y - from.y) * ease, from.z + (to.z - from.z) * ease);
            if (player != null) {
                entity.face(player, 0.2F);
            }
            if (stageTicks == STEP_TICKS) {
                entity.setState(EntityInBetweener.State.FALLEN);
                entity.world.playSound(null, entity.posX, entity.posY, entity.posZ, EternitySounds.RIFT_CLOSE, SoundCategory.HOSTILE, 4F, 0.5F);
                entity.world.playSound(null, entity.posX, entity.posY, entity.posZ, SoundEvents.ENTITY_PLAYER_HURT, SoundCategory.HOSTILE, 2F, 0.5F);
            }
            return;
        }
        // The neutral nothing drinks the black and the white out of him.
        entity.setDrain((stageTicks - STEP_TICKS) / (float) COLLAPSE_TICKS);
        if (stageTicks >= STEP_TICKS + COLLAPSE_TICKS) {
            freeing = false;
            if (player != null) {
                speak(player, InBetweenerDialogue.FALLEN);
            } else {
                setStage(Stage.NONE);
            }
        }
    }

    /**
     * Someone right-clicks him. Fallen, the gem on his brow is there for the taking; or,
     * with an Astral Regulator in hand, empty or not, he himself is.
     */
    public boolean touched(EntityPlayerMP player) {
        if (body == null || !data().broken || player.isSpectator() || freeing) {
            return false;
        }
        boolean regulator = isRegulator(player.getHeldItemMainhand());
        if (stage == Stage.SPEAKING && player.getUniqueID().equals(visitor) && node == InBetweenerDialogue.FALLEN) {
            if (regulator) {
                speak(player, InBetweenerDialogue.ABSORBED);
                beginAbsorb(player, EnumHand.MAIN_HAND);
            } else if (wearsGem()) {
                speak(player, InBetweenerDialogue.THEFT);
            }
            return true;
        }
        if (stage != Stage.NONE) {
            return false;
        }
        if (regulator) {
            visitor = player.getUniqueID();
            beginAbsorb(player, EnumHand.MAIN_HAND);
            return true;
        }
        if (wearsGem() && giveGem(player)) {
            data().remember(player.getUniqueID(), Memory.THIEF);
            status(player, "You pluck the Soul Gem from his brow. He cannot even lift a hand to stop you.", TextFormatting.GOLD);
            TextComponentString notice = new TextComponentString(player.getName() + " has stolen the Soul Gem from the In-Betweener.");
            notice.getStyle().setColor(TextFormatting.GOLD);
            player.server.getPlayerList().sendMessage(notice);
        } else if (data().adrift) {
            status(player, "He turns slowly in the void, grey and empty. He does not know you are there.", TextFormatting.GRAY);
        }
        return true;
    }

    /** Someone with a Regulator, from the Regulator's side: absorbs him if he lies fallen near them. */
    public boolean tryAbsorb(EntityPlayerMP player, EnumHand hand) {
        if (body == null || !data().broken || freeing || body.world != player.world || body.getDistance(player) > 12F
                || stage == Stage.ABSORBING || stage == Stage.BATTLE) {
            return false;
        }
        if (stage == Stage.SPEAKING && !player.getUniqueID().equals(visitor)) {
            return false;
        }
        visitor = player.getUniqueID();
        if (stage == Stage.SPEAKING) {
            speak(player, InBetweenerDialogue.ABSORBED);
        }
        beginAbsorb(player, hand);
        return true;
    }

    private static boolean isRegulator(ItemStack stack) {
        return stack.getItem() instanceof ItemAstralRegulator;
    }

    private void beginAbsorb(EntityPlayerMP player, EnumHand hand) {
        absorbHand = hand;
        // The words he is saying run on while he goes; the drawing-in is its own stage.
        setStage(Stage.ABSORBING);
        if (body != null) {
            body.setState(EntityInBetweener.State.ABSORBING);
        }
        player.world.playSound(null, player.posX, player.posY, player.posZ, EternitySounds.RIFT_OPEN, SoundCategory.HOSTILE, 4F, 0.5F);
        player.world.playSound(null, player.posX, player.posY, player.posZ, EternitySounds.FINALE_DRUMS, SoundCategory.HOSTILE, 2F, 0.9F);
    }

    /** He streams into the crystal, grey and then nothing. The gem, if he still wore it, falls free. */
    private void absorbing(EntityInBetweener entity, @Nullable EntityPlayerMP player) {
        if (player == null || !ItemAstralRegulator.holdsRegulator(player)) {
            abandon();
            return;
        }
        if (stageTicks % 4 == 0) {
            EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.ABSORB, entity.posX, entity.posY + 0.4D, entity.posZ, 0, 0, 0,
                    player.getEntityId(), 8), Nexus.dimension());
        }
        if (stageTicks % 25 == 0) {
            player.world.playSound(null, entity.posX, entity.posY, entity.posZ, EternitySounds.RIFT_IMPACT, SoundCategory.HOSTILE, 3F,
                    0.6F + stageTicks / (float) ABSORB_TICKS);
        }
        if (stageTicks < ABSORB_TICKS) {
            return;
        }
        ItemStack stack = player.getHeldItem(absorbHand);
        if (!isRegulator(stack)) {
            stack = player.getHeldItem(absorbHand == EnumHand.MAIN_HAND ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND);
        }
        if (wearsGem()) {
            giveGem(player);
        }
        ItemAstralRegulator.absorb(stack, ItemAstralRegulator.INBETWEENER);
        InBetweenerData d = data();
        d.inBetweenerAbsorbed = true;
        d.markDirty();
        player.world.playSound(null, entity.posX, entity.posY, entity.posZ, EternitySounds.COMET_BLAST, SoundCategory.HOSTILE, 6F, 0.7F);
        TextComponentString notice = new TextComponentString(player.getName() + " has drawn the In-Betweener into the Astral Regulator.");
        notice.getStyle().setColor(TextFormatting.GOLD);
        player.server.getPlayerList().sendMessage(notice);
        EternityNetwork.send(new MsgInBetweener(MsgInBetweener.CLOSE, 0), player);
        setStage(Stage.NONE);
        visitor = null;
        vanish();
    }

    /** Someone struck him. Inside his glass it passes through him; outside, there is nothing left to hurt. */
    public void struck(EntityPlayerMP player) {
        if (body == null || body.ticksExisted - lastMock < 20) {
            return;
        }
        lastMock = body.ticksExisted;
        if (data().broken) {
            status(player, "He does not even flinch. There is nothing left in him to hurt.", TextFormatting.GRAY);
        } else {
            status(player, "Your blow lands between one moment and the next, and never arrives.", TextFormatting.GRAY);
            voice(player, EternitySounds.ENTITY_VOICE, 0.8F, 1.6F);
        }
    }

    // ------------------------------------------------------------------ inside his bubble

    /** Inside his glass, where he is everything, he shows a thief what everything is. Nobody leaves. */
    private void beginBattle(EntityPlayerMP player) {
        setStage(Stage.BATTLE);
        data().remember(player.getUniqueID(), Memory.FOUGHT);
        frozenAt = null;
        inside = false;
        if (body != null) {
            body.setState(EntityInBetweener.State.WARP);
            body.setBubble((float) BATTLE_RADIUS);
        }
        Vec3d centre = EntityInBetweener.bubbleCentre();
        player.world.playSound(null, centre.x, centre.y, centre.z, EternitySounds.RIFT_OPEN, SoundCategory.HOSTILE, 6F, 0.5F);
        player.world.playSound(null, centre.x, centre.y, centre.z, EternitySounds.FINALE_DRUMS, SoundCategory.HOSTILE, 4F, 0.6F);
        EternityNetwork.send(new MsgInBetweener(MsgInBetweener.BATTLE, 0), player);
    }

    private static int phase(int ticks) {
        int phase = 0;
        for (int i = 0; i < PHASES.length - 1; i++) {
            if (ticks >= PHASES[i]) {
                phase = i;
            }
        }
        return phase;
    }

    private void battle(EntityInBetweener entity, @Nullable EntityPlayerMP player) {
        if (player == null) {
            endBattle(null);
            return;
        }
        int t = stageTicks;
        int phase = phase(t);
        if (t > 0 && Arrays.binarySearch(PHASES, t) > 0 && t < PHASES[PHASES.length - 1]) {
            EternityNetwork.send(new MsgInBetweener(MsgInBetweener.BATTLE, phase), player);
            voice(player, EternitySounds.ENTITY_VOICE, 1.6F, phase % 2 == 0 ? 1.1F : 0.55F);
            player.world.playSound(null, entity.posX, entity.posY, entity.posZ, EternitySounds.RIFT_IMPACT, SoundCategory.HOSTILE, 3F, 0.6F + 0.15F * phase);
        }
        Vec3d centre = EntityInBetweener.bubbleCentre();
        player.fallDistance = 0F;
        if (phase != 3) {
            confine(player, centre, BATTLE_RADIUS - 1.5D);
        }
        entity.face(player, 0.3F);
        float max = player.getMaxHealth();
        switch (phase) {
            case 0:
                // Life and death: full, then all but gone, then full again.
                entity.glide(centre.add(0D, -0.9D, 0D), 0.1D);
                if (t % 25 == 0) {
                    if ((t / 25) % 2 == 0) {
                        player.setHealth(max);
                    } else {
                        wound(player, 0.65F);
                    }
                }
                break;
            case 1:
                // Up and down: the client turns the world over; he throws them about in it.
                entity.glide(centre.add(0D, -0.9D, 0D), 0.1D);
                if (t % 40 == 20) {
                    wound(player, 0.12F);
                }
                break;
            case 2:
                // Here and there: everywhere in the glass at once, and so are you.
                if (t % 30 == 0) {
                    Vec3d at = centre.add(randomInside(BATTLE_RADIUS - 4D));
                    EntityDestroyerArmor.place(player, at.x, at.y, at.z, player.rotationYaw, player.rotationPitch);
                    Vec3d opposite = centre.add(centre.subtract(at).scale(0.6D));
                    entity.setPosition(opposite.x, opposite.y - 0.9D, opposite.z);
                    player.world.playSound(null, at.x, at.y, at.z, SoundEvents.ENTITY_ENDERMEN_TELEPORT, SoundCategory.HOSTILE, 1.5F, 0.6F);
                    wound(player, 0.1F);
                }
                break;
            case 3:
                // Before and after: time stops for you, and not for him.
                if (frozenAt == null) {
                    frozenAt = EntityDestroyerArmor.mass(player).getPositionVector();
                }
                player.motionX = player.motionY = player.motionZ = 0D;
                if (t % 2 == 0) {
                    EntityDestroyerArmor.place(player, frozenAt.x, frozenAt.y, frozenAt.z, player.rotationYaw, player.rotationPitch);
                }
                entity.glide(new Vec3d(player.posX, player.posY + 0.1D, player.posZ).add(player.getLookVec().scale(1.6D)), 0.03D);
                break;
            default:
                // Being and nothing: a touch on the brow.
                if (frozenAt != null && t % 2 == 0) {
                    EntityDestroyerArmor.place(player, frozenAt.x, frozenAt.y, frozenAt.z, player.rotationYaw, player.rotationPitch);
                }
                entity.glide(new Vec3d(player.posX, player.posY + 0.1D, player.posZ).add(player.getLookVec().scale(1.1D)), 0.08D);
                if (t >= PHASES[PHASES.length - 1]) {
                    unmake(player, EternityDamage.inBetweener(entity));
                    endBattle(player);
                }
                break;
        }
    }

    private Vec3d randomInside(double radius) {
        double x, y, z;
        do {
            x = rand.nextDouble() * 2D - 1D;
            y = rand.nextDouble() * 2D - 1D;
            z = rand.nextDouble() * 2D - 1D;
        } while (x * x + y * y + z * z > 1D);
        return new Vec3d(x * radius, y * radius * 0.6D, z * radius);
    }

    /** Keeps someone inside a sphere, gently. */
    private static void confine(EntityPlayerMP player, Vec3d centre, double radius) {
        // Inside the Destroyer Armor, it is the armour that has to stay in.
        Entity body = EntityDestroyerArmor.mass(player);
        double half = body.height / 2D;
        Vec3d off = body.getPositionVector().add(0D, half, 0D).subtract(centre);
        double room = Math.max(1D, radius - (body == player ? 0D : half));
        if (off.length() > room) {
            Vec3d back = centre.add(off.normalize().scale(room - 0.5D)).add(0D, -half, 0D);
            EntityDestroyerArmor.place(player, back.x, back.y, back.z, player.rotationYaw, player.rotationPitch);
        }
    }

    /** Hurts without killing; only the end of it kills. */
    private static void wound(EntityPlayerMP player, float fraction) {
        if (player.isSpectator()) {
            return;
        }
        // Inside the Destroyer Armor, it is the armour he wears away.
        EntityDestroyerArmor.wound(player, fraction);
        player.world.setEntityState(player, (byte) 2);
        player.world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_PLAYER_HURT, SoundCategory.HOSTILE, 1F, 0.8F);
    }

    /** Nothing saves them: not armour, not creative mode, not a totem, not a Regulator. */
    private static void unmake(EntityLivingBase living, DamageSource source) {
        if (living instanceof EntityPlayer && ((EntityPlayer) living).isSpectator()) {
            return;
        }
        living.attackEntityFrom(source, 1.0E9F);
        if (living.getHealth() > 0F) {
            living.setHealth(0F);
            living.onDeath(source);
        }
    }

    private void endBattle(@Nullable EntityPlayerMP player) {
        if (player != null) {
            EternityNetwork.send(new MsgInBetweener(MsgInBetweener.BATTLE_END, 0), player);
            lingering.add(player.getUniqueID());
        }
        if (body != null) {
            body.setBubble((float) Nexus.BUBBLE_RADIUS);
            body.setState(EntityInBetweener.State.HOVER);
        }
        frozenAt = null;
        setStage(Stage.NONE);
        visitor = null;
    }

    /** Dying inside his glass any other way still ends it. */
    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        if (stage == Stage.BATTLE && event.getEntityLiving() instanceof EntityPlayerMP && event.getEntityLiving().getUniqueID().equals(visitor)) {
            endBattle((EntityPlayerMP) event.getEntityLiving());
        }
    }

    // ------------------------------------------------------------------ the glass

    /**
     * Nothing passes through his glass unless he lets it. People and creatures are pushed
     * back out of it, anything thrown at it breaks on it, and whatever is dropped inside
     * rolls out. The one he has drawn in stays in.
     */
    private void barrier(WorldServer nexus, EntityInBetweener entity) {
        double radius = entity.getBubble();
        if (radius < 1D) {
            return;
        }
        Vec3d centre = EntityInBetweener.bubbleCentre();
        AxisAlignedBB box = new AxisAlignedBB(centre.x - radius - 3D, centre.y - radius - 3D, centre.z - radius - 3D,
                centre.x + radius + 3D, centre.y + radius + 3D, centre.z + radius + 3D);
        for (Entity entityIn : nexus.getEntitiesWithinAABB(Entity.class, box)) {
            if (entityIn == entity || entityIn.isDead || entityIn instanceof EntityOrderChaos
                    || (stage == Stage.BATTLE || inside) && EntityDestroyerArmor.soul(entityIn).getUniqueID().equals(visitor)
                    || EntityDestroyerArmor.carrier(entityIn) != null) {
                continue;
            }
            if (entityIn instanceof EntityPlayer && ((EntityPlayer) entityIn).isSpectator()) {
                continue;
            }
            Vec3d mid = entityIn.getPositionVector().add(0D, entityIn.height / 2D, 0D);
            Vec3d off = mid.subtract(centre);
            double d = off.length();
            if (d >= radius + 0.3D) {
                continue;
            }
            if (entityIn instanceof IProjectile) {
                nexus.playSound(null, mid.x, mid.y, mid.z, SoundEvents.BLOCK_GLASS_HIT, SoundCategory.HOSTILE, 1F, 1.5F);
                entityIn.setDead();
                continue;
            }
            Vec3d dir = d < 1.0E-3 ? new Vec3d(1D, 0D, 0D) : off.scale(1D / d);
            Vec3d out = centre.add(dir.scale(radius + 0.6D)).add(0D, -entityIn.height / 2D, 0D);
            if (entityIn instanceof EntityDestroyerArmor) {
                ((EntityDestroyerArmor) entityIn).put(out.x, out.y, out.z);
            } else if (entityIn instanceof EntityPlayerMP) {
                ((EntityPlayerMP) entityIn).connection.setPlayerLocation(out.x, out.y, out.z, entityIn.rotationYaw, entityIn.rotationPitch);
            } else {
                entityIn.setPositionAndUpdate(out.x, out.y, out.z);
            }
            entityIn.motionX = dir.x * 0.3D;
            entityIn.motionY = Math.max(0D, dir.y * 0.3D);
            entityIn.motionZ = dir.z * 0.3D;
            entityIn.velocityChanged = true;
        }
    }

    // ------------------------------------------------------------------ Order and Chaos

    /**
     * Master Order and Lord Chaos arrive. There is nothing left in him worth putting back, and
     * there is no second glass: they let his empty body go, and it drifts off into the Nexus.
     * Then they turn to whoever is still near: the thief first, if the thief stayed. Anyone
     * farther than wrathRadius has escaped. If he is inside a Regulator, they come all the same,
     * and they are not pleased. If both of them are inside Regulators, nobody comes, and his
     * body drifts off on its own.
     */
    private void wrath(@Nullable WorldServer nexus) {
        InBetweenerData d = data();
        d.wrathAt = -1;
        d.markDirty();
        if (stage != Stage.NONE && stage != Stage.BATTLE) {
            abandon();
        }
        castAdrift();
        if (mastersGone() || nexus == null) {
            return;
        }
        EternityNetwork.sendToDimension(new MsgInBetweener(MsgInBetweener.WRATH, 0), Nexus.dimension());
        Vec3d centre = EntityInBetweener.bubbleCentre();
        nexus.playSound(null, centre.x, centre.y, centre.z, EternitySounds.FINALE_DRUMS, SoundCategory.HOSTILE, 16F, 0.5F);
        nexus.playSound(null, centre.x, centre.y, centre.z, EternitySounds.COMET_BLAST, SoundCategory.HOSTILE, 16F, 0.5F);
        if (!d.inBetweenerAbsorbed) {
            if (!d.orderAbsorbed) {
                OrderAndChaos.say(nexus, true, "Look at him. There is nothing left in him to put back.");
            }
            if (!d.chaosAbsorbed) {
                OrderAndChaos.say(nexus, false, "Let him float. He always did want to see the place.");
            }
        }

        // Whom they turn to: the thief, if still here; otherwise whoever is nearest.
        double radius = EternityConfig.inbetweener.wrathRadius;
        EntityPlayerMP thief = null, nearest = null;
        double best = Double.MAX_VALUE;
        for (EntityPlayer player : nexus.playerEntities) {
            double dist = player.getDistance(centre.x, centre.y, centre.z);
            if (!(player instanceof EntityPlayerMP) || player.isSpectator() || !player.isEntityAlive() || dist > radius) {
                continue;
            }
            if (player.getName().equals(d.gemHolder) && findSoulStone((EntityPlayerMP) player) >= 0) {
                thief = (EntityPlayerMP) player;
            }
            if (dist < best) {
                best = dist;
                nearest = (EntityPlayerMP) player;
            }
        }
        EntityPlayerMP speaker = thief != null ? thief : nearest;
        OrderAndChaos.INSTANCE.arrive(nexus, speaker);
        if (speaker != null && stage != Stage.BATTLE) {
            converse(speaker, d.inBetweenerAbsorbed ? InBetweenerDialogue.WRATH_EMPTY
                    : thief != null ? InBetweenerDialogue.WRATH_THIEF : InBetweenerDialogue.WRATH_BYSTANDER);
        }
    }

    /** Order and Chaos speak to someone; or, if only one of them is left to come, it does not bother talking. */
    private void converse(EntityPlayerMP speaker, int opening) {
        if (data().orderAbsorbed || data().chaosAbsorbed) {
            OrderAndChaos.INSTANCE.fight();
            return;
        }
        visitor = speaker.getUniqueID();
        ego = START_EGO;
        hunger = suspicion = pull = facts = 0;
        speak(speaker, opening);
    }

    /** Sends someone to the edge of the grey disc, facing his glass, from the side of the hub. For operators. */
    public static void visit(EntityPlayerMP player) {
        double x = Nexus.JUNCTION_X - 13.5D, z = Nexus.JUNCTION_Z + 13.5D;
        Teleport.player(player, Nexus.dimension(), x, Nexus.JUNCTION_Y, z, false);
        EntityDestroyerArmor.place(player, x, Nexus.JUNCTION_Y, z, -135F, -5F);
    }

    /** Ends the wait for Order and Chaos now. For operators. */
    public void hastenWrath() {
        InBetweenerData d = data();
        if (d.wrathAt >= 0) {
            d.wrathAt = d.clock;
        }
    }

    /** Brings Order and Chaos to the junction now, to answer to this player. For operators. */
    public void summonMasters(EntityPlayerMP player) {
        WorldServer nexus = DimensionManager.getWorld(Nexus.dimension());
        if (nexus == null || player.world != nexus || mastersGone()) {
            return;
        }
        abandon();
        OrderAndChaos.INSTANCE.arrive(nexus, player);
        boolean thief = findSoulStone(player) >= 0;
        converse(player, data().inBetweenerAbsorbed ? InBetweenerDialogue.WRATH_EMPTY
                : thief ? InBetweenerDialogue.WRATH_THIEF : InBetweenerDialogue.WRATH_BYSTANDER);
    }

    /** Order and Chaos have stopped waiting for an answer; whatever was being said is over. */
    void silenceMasters() {
        EntityPlayerMP player = visitorPlayer();
        if (mastersSpeaking() && player != null) {
            finish(player);
        }
    }

    // ------------------------------------------------------------------ helpers

    private static void voice(EntityPlayerMP player, SoundEvent sound, float volume, float pitch) {
        player.connection.sendPacket(new SPacketSoundEffect(sound, SoundCategory.HOSTILE, player.posX, player.posY, player.posZ, volume, pitch));
    }

    private static void status(EntityPlayerMP player, String text, TextFormatting colour) {
        TextComponentString message = new TextComponentString(text);
        message.getStyle().setColor(colour);
        player.sendStatusMessage(message, true);
    }
}
