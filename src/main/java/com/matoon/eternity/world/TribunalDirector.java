package com.matoon.eternity.world;

import com.matoon.eternity.EternityConfig;
import com.matoon.eternity.entity.EntityLivingTribunal;
import com.matoon.eternity.item.ItemAstralRegulator;
import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.network.MsgTribunal;
import lucraft.mods.lucraftcore.extendedinventory.capabilities.CapabilityExtendedInventory;
import lucraft.mods.lucraftcore.extendedinventory.capabilities.IExtendedInventoryCapability;
import lucraft.mods.lucraftcore.superpowers.SuperpowerHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Runs the Living Tribunal. Some time after Eternity is caged in the Astral Regulator,
 * he comes for whoever caged him: he looms out of nowhere, reaches out, and drags them
 * through a wormhole into the Star Chamber. There they answer for what they have done,
 * and nothing lets them leave until he has passed judgement. Acquittal sends them home.
 * Conviction strips them of everything they own, or, for the defiant, casts them out of
 * their world forever.
 */
public final class TribunalDirector {

    public static final TribunalDirector INSTANCE = new TribunalDirector();

    public static final int ABDUCT_TICKS = 150;
    private static final int AWAKEN_TICKS = 120;
    private static final int PAUSE_TICKS = 50;
    private static final int VERDICT_TICKS = 260;
    public static final int RETURN_TICKS = 90;
    private static final int TICKS_PER_MINUTE = 1200;
    private static final String CAST_OUT = "The Living Tribunal has cast you out of this world. You may never return.";

    private static final int CHALLENGE_TICKS = 140;

    private enum Stage { NONE, ABDUCTING, AWAKENING, ASKING, PAUSING, VERDICT, RETURNING, CHALLENGED, FIGHTING }

    private final Random rand = new Random();
    private final List<UUID> kicks = new ArrayList<>();

    private TribunalData data;
    private Stage stage = Stage.NONE;
    private int stageTicks;
    private int node = -1;
    private int score;
    private int defiance;
    private int verdict;
    private Vec3d anchor;
    /** Set while this class moves the defendant itself, so its own teleports are not refused. */
    private boolean moving;
    /** The Tribunal risen from his throne to fight, while the defendant dares. */
    private EntityLivingTribunal boss;
    /** Set while this class swings a player's weapon at him on their behalf. */
    private boolean striking;

    private TribunalDirector() {
    }

    public void reset() {
        data = null;
        stage = Stage.NONE;
        stageTicks = 0;
        node = -1;
        kicks.clear();
        moving = false;
        boss = null;
        striking = false;
    }

    public TribunalData data() {
        if (data == null) {
            data = TribunalData.get();
        }
        return data;
    }

    /** Eternity has been contained. In a while, the Tribunal will come for whoever did it. */
    public void schedule(UUID who) {
        if (!EternityConfig.tribunal.enabled) {
            return;
        }
        TribunalData d = data();
        if (exhausted()) {
            // Every Living Tribunal this world will ever have is inside a Regulator. There is no one left to come.
            return;
        }
        int min = EternityConfig.tribunal.minDelayMinutes;
        int max = Math.max(min, EternityConfig.tribunal.maxDelayMinutes);
        d.target = who;
        d.summonAt = d.clock + (long) ((min + rand.nextDouble() * (max - min)) * TICKS_PER_MINUTE);
        d.markDirty();
    }

    /**
     * Whether this world has no Living Tribunal left: as many have been absorbed as it
     * holds (tribunal.perWorld; -1 for no limit).
     */
    public boolean exhausted() {
        int limit = EternityConfig.tribunal.perWorld;
        return limit >= 0 && data().absorbed >= limit;
    }

    /** Takes this player now. For testing. */
    public boolean summon(EntityPlayerMP player) {
        if (stage != Stage.NONE) {
            return false;
        }
        TribunalData d = data();
        d.target = player.getUniqueID();
        d.markDirty();
        abduct(player);
        return true;
    }

    public boolean isJudging() {
        return stage != Stage.NONE;
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
        TribunalData d = data();
        d.clock++;
        if (d.clock % 200 == 0) {
            d.markDirty();
        }
        castOut(server);

        EntityPlayerMP target = d.target == null ? null : server.getPlayerList().getPlayerByUUID(d.target);
        if (stage != Stage.NONE && target == null) {
            // They left mid-judgement. It resumes the moment they return.
            stage = Stage.NONE;
            return;
        }
        stageTicks++;
        switch (stage) {
            case NONE:
                if (target == null) {
                    break;
                }
                if (d.judging) {
                    convene(target);
                } else if (d.summonAt >= 0 && d.clock >= d.summonAt && EternityConfig.tribunal.enabled
                        && target.isEntityAlive() && !target.isSpectator()) {
                    abduct(target);
                }
                break;
            case ABDUCTING:
                hold(target);
                if (stageTicks >= ABDUCT_TICKS) {
                    arrive(target);
                }
                break;
            case AWAKENING:
                confine(target);
                if (stageTicks >= AWAKEN_TICKS) {
                    ask(target, 0);
                }
                break;
            case ASKING:
                confine(target);
                break;
            case PAUSING:
                confine(target);
                if (stageTicks >= PAUSE_TICKS) {
                    if (TribunalDialogue.isQuestion(node + 1)) {
                        ask(target, node + 1);
                    } else {
                        pronounce(target);
                    }
                }
                break;
            case VERDICT:
                confine(target);
                if (stageTicks >= VERDICT_TICKS) {
                    carryOut(server, target);
                }
                break;
            case RETURNING:
                confine(target);
                if (stageTicks >= RETURN_TICKS) {
                    release(target);
                }
                break;
            case CHALLENGED:
                confine(target);
                if (stageTicks >= CHALLENGE_TICKS) {
                    rise(target);
                }
                break;
            case FIGHTING:
                confine(target);
                if (boss == null || boss.isDead || !boss.isAddedToWorld()) {
                    rise(target);
                }
                break;
        }
    }

    private void setStage(Stage next) {
        stage = next;
        stageTicks = 0;
    }

    // ------------------------------------------------------------------ the taking

    private void abduct(EntityPlayerMP player) {
        TribunalData d = data();
        d.returnDim = player.dimension;
        d.returnX = player.posX;
        d.returnY = player.posY;
        d.returnZ = player.posZ;
        d.markDirty();
        anchor = player.getPositionVector();
        setStage(Stage.ABDUCTING);
        EternityNetwork.send(new MsgTribunal(MsgTribunal.ABDUCT, ABDUCT_TICKS), player);
    }

    /** Held fast in his grip; nothing they do moves them. */
    private void hold(EntityPlayerMP player) {
        player.motionX = player.motionY = player.motionZ = 0D;
        player.fallDistance = 0F;
        if (anchor != null && stageTicks % 2 == 0) {
            player.connection.setPlayerLocation(anchor.x, anchor.y, anchor.z, player.rotationYaw, player.rotationPitch);
        }
    }

    private void arrive(EntityPlayerMP player) {
        TribunalData d = data();
        d.summonAt = -1;
        d.judging = true;
        d.markDirty();
        convene(player);
    }

    /** Places them on the dais and begins, or begins again if they left and came back. */
    private void convene(EntityPlayerMP player) {
        vanishBoss();
        moveTo(player, StarChamber.dimension(), StarChamber.DAIS_X, StarChamber.DAIS_Y, StarChamber.DAIS_Z);
        score = 0;
        defiance = 0;
        node = -1;
        setStage(Stage.AWAKENING);
        EternityNetwork.send(new MsgTribunal(MsgTribunal.CHAMBER, 0), player);
    }

    /** No way out: not by portal, not by death, not by walking off the edge. */
    private void confine(EntityPlayerMP player) {
        player.fallDistance = 0F;
        if (player.dimension != StarChamber.dimension()) {
            moveTo(player, StarChamber.dimension(), StarChamber.DAIS_X, StarChamber.DAIS_Y, StarChamber.DAIS_Z);
        } else if (!StarChamber.inside(player.posX, player.posY, player.posZ)) {
            player.connection.setPlayerLocation(StarChamber.DAIS_X, StarChamber.DAIS_Y, StarChamber.DAIS_Z, player.rotationYaw, player.rotationPitch);
        }
    }

    private void moveTo(EntityPlayerMP player, int dimension, double x, double y, double z) {
        moving = true;
        try {
            Teleport.player(player, dimension, x, y, z, false);
        } finally {
            moving = false;
        }
    }

    // ------------------------------------------------------------------ the trial

    private void ask(EntityPlayerMP player, int question) {
        node = question;
        setStage(Stage.ASKING);
        rumble(player, 0.45F);
        EternityNetwork.send(new MsgTribunal(MsgTribunal.SPEAK, question), player);
    }

    /** The defendant answers. Only the question being asked, only from the one on trial. */
    public void answer(EntityPlayerMP player, int question, int choice) {
        TribunalData d = data();
        if (stage != Stage.ASKING || question != node || !player.getUniqueID().equals(d.target)
                || choice < 0 || choice >= TribunalDialogue.node(question).answers.length) {
            return;
        }
        if (TribunalDialogue.isChallenge(question, choice)) {
            challenge(player);
            return;
        }
        score += TribunalDialogue.score(question, choice);
        if (TribunalDialogue.defiant(question, choice)) {
            defiance++;
        }
        setStage(Stage.PAUSING);
    }

    private void pronounce(EntityPlayerMP player) {
        if (score >= EternityConfig.tribunal.acquittalScore) {
            verdict = TribunalDialogue.VERDICT_ACQUITTED;
        } else {
            verdict = defiance >= 2 ? TribunalDialogue.VERDICT_BANISHED : TribunalDialogue.VERDICT_STRIPPED;
        }
        node = verdict;
        setStage(Stage.VERDICT);
        rumble(player, 0.35F);
        player.world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_LIGHTNING_THUNDER, SoundCategory.MASTER, 4F, 0.5F);
        EternityNetwork.send(new MsgTribunal(MsgTribunal.SPEAK, verdict), player);
    }

    private void carryOut(MinecraftServer server, EntityPlayerMP player) {
        if (verdict == TribunalDialogue.VERDICT_BANISHED) {
            banish(server, player);
            return;
        }
        if (verdict == TribunalDialogue.VERDICT_STRIPPED || verdict == TribunalDialogue.VERDICT_DEFEATED) {
            strip(player);
        }
        setStage(Stage.RETURNING);
        EternityNetwork.send(new MsgTribunal(MsgTribunal.RETURN, RETURN_TICKS), player);
    }

    /**
     * Everything. Every item they carry, wear or keep in their ender chest or in
     * Lucraft's extra slots; their superpower and every ability it gave them; every
     * level, every effect, every heart of absorption; and Eternity forgets the stones
     * they ever held, as if they never had.
     */
    private static void strip(EntityPlayerMP player) {
        player.inventory.clear();
        player.getInventoryEnderChest().clear();
        IExtendedInventoryCapability extended = player.getCapability(CapabilityExtendedInventory.EXTENDED_INVENTORY_CAP, null);
        if (extended != null) {
            extended.getInventory().clear();
            if (extended instanceof CapabilityExtendedInventory) {
                ((CapabilityExtendedInventory) extended).syncToAll();
            }
        }
        if (SuperpowerHandler.getSuperpower(player) != null) {
            SuperpowerHandler.removeSuperpower(player);
        }
        player.clearActivePotions();
        player.setAbsorptionAmount(0F);
        Director.INSTANCE.forget(player.getUniqueID());
        player.experienceLevel = 0;
        player.experience = 0F;
        player.experienceTotal = 0;
        player.inventoryContainer.detectAndSendChanges();
    }

    private void banish(MinecraftServer server, EntityPlayerMP player) {
        TribunalData d = data();
        d.banished.put(player.getUniqueID(), player.getName());
        d.judging = false;
        d.target = null;
        d.markDirty();
        setStage(Stage.NONE);
        TextComponentString notice = new TextComponentString(player.getName() + " was cast out of this world by the Living Tribunal.");
        notice.getStyle().setColor(TextFormatting.DARK_PURPLE);
        server.getPlayerList().sendMessage(notice);
        player.connection.disconnect(new TextComponentString(CAST_OUT));
    }

    private void release(EntityPlayerMP player) {
        vanishBoss();
        TribunalData d = data();
        d.judging = false;
        d.target = null;
        d.markDirty();
        setStage(Stage.NONE);
        moveTo(player, d.returnDim, d.returnX, d.returnY, d.returnZ);
        EternityNetwork.send(new MsgTribunal(MsgTribunal.RELEASE, 0), player);
    }

    /** When he speaks, the whole chamber shakes with it. */
    private static void rumble(EntityPlayerMP player, float pitch) {
        player.world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_ENDERDRAGON_GROWL, SoundCategory.MASTER, 3F, pitch);
    }

    // ------------------------------------------------------------------ the fight

    /** The defendant refuses to be judged. He rises from his throne. */
    private void challenge(EntityPlayerMP player) {
        node = TribunalDialogue.CHALLENGE_ACCEPTED;
        setStage(Stage.CHALLENGED);
        rumble(player, 0.3F);
        player.world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_LIGHTNING_THUNDER, SoundCategory.MASTER, 4F, 0.4F);
        EternityNetwork.send(new MsgTribunal(MsgTribunal.SPEAK, node), player);
    }

    private void rise(EntityPlayerMP player) {
        vanishBoss();
        EntityLivingTribunal entity = new EntityLivingTribunal(player.world);
        entity.setLocationAndAngles(StarChamber.THRONE_X, StarChamber.THRONE_Y, StarChamber.THRONE_Z + 6D, 0F, 0F);
        entity.rotationYawHead = 0F;
        boss = entity;
        player.world.spawnEntity(entity);
        node = -1;
        setStage(Stage.FIGHTING);
        EternityNetwork.send(new MsgTribunal(MsgTribunal.FIGHT, 0), player);
    }

    private void vanishBoss() {
        if (boss != null) {
            boss.setDead();
            boss = null;
        }
    }

    /** Called by every Tribunal entity each tick; anything but the one he is fighting with dissolves. */
    public boolean claim(EntityLivingTribunal entity) {
        return entity == boss && stage == Stage.FIGHTING;
    }

    /** Whom he is fighting, if anyone. */
    @Nullable
    public EntityPlayerMP opponent() {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        TribunalData d = data();
        if (stage != Stage.FIGHTING || server == null || d.target == null) {
            return null;
        }
        return server.getPlayerList().getPlayerByUUID(d.target);
    }

    /**
     * The defendant swung at him (see {@link com.matoon.eternity.network.MsgStrikeTribunal}).
     * He is far too big for vanilla's reach check, so the server measures to his body itself.
     */
    public void melee(EntityPlayerMP player, int entityId) {
        if (boss == null || boss.getEntityId() != entityId || boss.world != player.world || player.isSpectator()) {
            return;
        }
        if (EntityLivingTribunal.distanceToBody(player.getPositionEyes(1F), boss.getEntityBoundingBox()) > EntityLivingTribunal.STRIKE_REACH) {
            return;
        }
        striking = true;
        try {
            player.attackTargetEntityWithCurrentItem(boss);
        } finally {
            striking = false;
        }
    }

    /**
     * Someone raised a charged Astral Regulator. It works only while he lies beaten, and
     * only for the one he was fighting, looking at him.
     */
    public boolean tryAbsorb(EntityPlayerMP player, EnumHand hand) {
        if (stage != Stage.FIGHTING || boss == null || boss.world != player.world || !isDefendant(player)) {
            return false;
        }
        Vec3d eyes = player.getPositionEyes(1F);
        Vec3d toHeart = boss.heart().subtract(eyes);
        if (toHeart.length() > 120D || player.getLook(1F).dotProduct(toHeart.normalize()) < 0.5D) {
            return false;
        }
        return boss.absorbBy(player, hand);
    }

    /** He is inside the Regulator now. Whoever holds it holds the court itself. */
    public void absorbed(EntityPlayerMP player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!(stack.getItem() instanceof ItemAstralRegulator) || !ItemAstralRegulator.isCharged(stack)) {
            stack = ItemStack.EMPTY;
            for (ItemStack candidate : player.inventory.mainInventory) {
                if (candidate.getItem() instanceof ItemAstralRegulator && ItemAstralRegulator.isCharged(candidate)) {
                    stack = candidate;
                    break;
                }
            }
        }
        if (!stack.isEmpty()) {
            ItemAstralRegulator.absorb(stack, ItemAstralRegulator.TRIBUNAL);
        }
        TribunalData d = data();
        d.absorbed++;
        d.markDirty();
        boss = null;
        MinecraftServer server = player.getServer();
        if (server != null) {
            TextComponentString notice = new TextComponentString(player.getName() + " has absorbed the Living Tribunal.");
            notice.getStyle().setColor(TextFormatting.GOLD);
            server.getPlayerList().sendMessage(notice);
        }
        setStage(Stage.RETURNING);
        EternityNetwork.send(new MsgTribunal(MsgTribunal.RETURN, RETURN_TICKS), player);
    }

    /** The defendant fell. He stands over them and passes the only sentence left. */
    private void defeated(EntityPlayerMP player) {
        player.setHealth(player.getMaxHealth());
        vanishBoss();
        verdict = TribunalDialogue.VERDICT_DEFEATED;
        node = verdict;
        setStage(Stage.VERDICT);
        rumble(player, 0.3F);
        EternityNetwork.send(new MsgTribunal(MsgTribunal.SPEAK, verdict), player);
    }

    // ------------------------------------------------------------------ the banished

    /** Pardons someone the Tribunal cast out. Returns false if they were never banished. */
    public boolean pardon(String name) {
        TribunalData d = data();
        UUID found = null;
        for (Map.Entry<UUID, String> entry : d.banished.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(name)) {
                found = entry.getKey();
            }
        }
        if (found == null) {
            return false;
        }
        d.banished.remove(found);
        d.markDirty();
        return true;
    }

    private void castOut(MinecraftServer server) {
        if (kicks.isEmpty()) {
            return;
        }
        for (UUID id : kicks) {
            EntityPlayerMP player = server.getPlayerList().getPlayerByUUID(id);
            if (player != null) {
                player.connection.disconnect(new TextComponentString(CAST_OUT));
            }
        }
        kicks.clear();
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP && data().banished.containsKey(event.player.getUniqueID())) {
            // Turned away a moment after arriving; disconnecting during login itself is not safe.
            kicks.add(event.player.getUniqueID());
        }
    }

    // ------------------------------------------------------------------ no escape

    @SubscribeEvent
    public void onTravel(EntityTravelToDimensionEvent event) {
        if (!moving && stage != Stage.NONE && isDefendant(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    /**
     * Nothing can harm the defendant before judgement; the verdict is his alone. Unless
     * they have chosen to fight him, in which case he will harm them a great deal.
     */
    @SubscribeEvent
    public void onAttack(LivingAttackEvent event) {
        if (stage == Stage.FIGHTING && isDefendant(event.getEntityLiving())) {
            return;
        }
        if (stage != Stage.NONE && isDefendant(event.getEntityLiving())
                || event.getEntityLiving() instanceof EntityPlayer && event.getEntityLiving().dimension == StarChamber.dimension()) {
            event.setCanceled(true);
        }
    }

    /** Falling to him in battle is not death. It is a verdict. */
    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        if (stage == Stage.FIGHTING && isDefendant(event.getEntityLiving())) {
            event.setCanceled(true);
            defeated((EntityPlayerMP) event.getEntityLiving());
        }
    }

    /** Blows reach him only through {@link #melee}, so vanilla's own swing must not land twice. */
    @SubscribeEvent
    public void onSwing(AttackEntityEvent event) {
        if (event.getTarget() instanceof EntityLivingTribunal && !event.getEntityPlayer().world.isRemote && !striking) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onBreak(BlockEvent.BreakEvent event) {
        if (event.getWorld().provider.getDimension() == StarChamber.dimension() && !event.getPlayer().isCreative()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onPlace(BlockEvent.PlaceEvent event) {
        if (event.getWorld().provider.getDimension() == StarChamber.dimension() && !event.getPlayer().isCreative()) {
            event.setCanceled(true);
        }
    }

    private boolean isDefendant(@Nullable Entity entity) {
        return entity instanceof EntityPlayerMP && entity.getUniqueID().equals(data().target);
    }
}
