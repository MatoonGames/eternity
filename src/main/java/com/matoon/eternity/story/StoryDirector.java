package com.matoon.eternity.story;

import com.matoon.eternity.EternityConfig;
import com.matoon.eternity.entity.EntityDraugr;
import com.matoon.eternity.entity.EntityNorse;
import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.network.MsgStory;
import com.matoon.eternity.network.MsgStoryAct;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The story, on the server. It keeps everyone's conversations going, hands what happens in the
 * world to the chapter it belongs to ({@link Oath} under the mound, {@link VaultStory} in Odin's
 * vault, {@link Presence} wherever she is needed next), and tells each player's client what the
 * story has made of them.
 */
public final class StoryDirector {

    public static final StoryDirector INSTANCE = new StoryDirector();

    final Oath oath = new Oath(this);
    final VaultStory vault = new VaultStory(this);
    final Presence presence = new Presence(this);
    final Obsession obsession = new Obsession(this);
    final Betrayal betrayal = new Betrayal(this);
    final Reckoning reckoning = new Reckoning(this);
    final Aftermath aftermath = new Aftermath(this);
    final Arena arena = new Arena(this);
    final Glass glass = new Glass(this);
    final Garden garden = new Garden(this);
    final Sovereign sovereign = new Sovereign(this);
    final MagusFight magusFight = new MagusFight(this);
    final Companion companion = new Companion(this);

    /** Everyone in a conversation, by player: those standing together share one. */
    private final Map<UUID, Talk> talks = new HashMap<>();
    /** Whoever an operator has put in front of them to look at. */
    @Nullable
    private Entity posed;
    /** /eternity story forms: Death showing every shape she has, one after another, and when she began. */
    private EntityNorse forms;
    private long formsAt;
    /** When, in the parade of her shapes, she tears the pretty one off; and when she shows the skull after. */
    private static final int FORMS_TEAR = 900, FORMS_SKULL = FORMS_TEAR + com.matoon.eternity.entity.SkinTear.TICKS + 100;
    private static final int FORM_TICKS = 100;
    /** Answers an operator has given ahead of the question, for the next conversation each player starts. */
    private final Map<UUID, java.util.ArrayDeque<Integer>> presets = new HashMap<>();

    /** The next answer an operator gave ahead of its question, if any. */
    @Nullable
    Integer takePreset(EntityPlayerMP player) {
        java.util.ArrayDeque<Integer> q = presets.get(player.getUniqueID());
        return q == null ? null : q.poll();
    }
    /** Players whose client has told us the scene it was showing them is over, by scene. */
    private final Map<UUID, Set<Integer>> scenesDone = new HashMap<>();
    @Nullable
    private StoryData data;
    private long ticks;

    private StoryDirector() {
    }

    public void reset() {
        for (Talk t : new HashSet<>(talks.values())) {
            t.stop(null);
        }
        talks.clear();
        scenesDone.clear();
        sceneTalks.clear();
        data = null;
        oath.reset();
        vault.reset();
        presence.reset();
        obsession.reset();
        betrayal.reset();
        reckoning.reset();
        aftermath.reset();
        arena.reset();
        glass.reset();
        garden.reset();
        sovereign.reset();
        magusFight.reset();
        companion.reset();
        journals.clear();
        Fellowship.reset();
    }

    StoryData data() {
        if (data == null) {
            data = StoryData.get();
        }
        return data;
    }

    public static boolean enabled() {
        return EternityConfig.story.enabled;
    }


    /**
     * Something big is playing out: the Grandmaster's game, the Magus's last fight (and what follows it), the
     * Champion's duel, the reckoning over six stones, Eternity hunting or dying, or the Tribunal. Death keeps out of
     * it: no visits, no whispers, no standing over what dies; and nothing she asked for runs out while it lasts.
     */
    boolean setPiece() {
        com.matoon.eternity.world.EternityData.Phase eternity = com.matoon.eternity.world.Director.INSTANCE.data().phase;
        return com.matoon.eternity.world.GrandmasterData.get().active()
                || magusFight.underway()
                || com.matoon.eternity.world.ChampionData.get().phase == com.matoon.eternity.world.ChampionData.Phase.DUEL
                || reckoning.busy()
                || eternity == com.matoon.eternity.world.EternityData.Phase.HUNTING || eternity == com.matoon.eternity.world.EternityData.Phase.DYING
                || com.matoon.eternity.world.TribunalDirector.INSTANCE.isJudging();
    }
    /**
     * Whether Eternity keeps away: while the Magus's last fight is on, and for a minute after it is won, so that its
     * end (his dust, whatever is said over it) is not trodden on by what comes next. Lost, it never comes at all: the
     * Magus has all six (or Odin has, if it was his), and there is nothing left here for it to come for. And while
     * Death and Warlock are putting it to whoever has six, and the little while they are given to choose (see {@link Reckoning}).
     */
    public static boolean eternityWaits() {
        com.matoon.eternity.world.SovereignData s = com.matoon.eternity.world.SovereignData.get();
        return s.fight == com.matoon.eternity.world.SovereignData.Fight.FIGHTING
                || s.fight == com.matoon.eternity.world.SovereignData.Fight.LOST
                || s.fight == com.matoon.eternity.world.SovereignData.Fight.TAKEN
                || s.fight == com.matoon.eternity.world.SovereignData.Fight.WON && now() - s.fightSettled < 20L * 60L
                || enabled() && INSTANCE.reckoning.holds();
    }

    static long now() {
        return FMLCommonHandler.instance().getMinecraftServerInstance().getEntityWorld().getTotalWorldTime();
    }

    @Nullable
    static EntityPlayerMP player(@Nullable UUID id) {
        if (id == null) {
            return null;
        }
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        return server == null ? null : server.getPlayerList().getPlayerByUUID(id);
    }

    // ------------------------------------------------------------------ ticking

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null || server.getEntityWorld() == null) {
            return;
        }
        ticks++;
        List<UUID> dropped = new ArrayList<>();
        for (Talk t : new LinkedHashSet<>(talks.values())) {
            dropped.clear();
            boolean going = t.tick(dropped);
            for (UUID id : dropped) {
                talks.remove(id, t);
            }
            if (!going) {
                talks.values().removeIf(v -> v == t);
            } else if (ticks % 10 == 0) {
                gather(server, t);
            }
        }
        oath.tick(server);
        vault.tick(server);
        presence.tick(server);
        obsession.tick(server);
        betrayal.tick(server);
        reckoning.tick(server);
        aftermath.tick(server);
        arena.tick(server);
        glass.tick(server);
        garden.tick(server);
        sovereign.tick(server);
        magusFight.tick(server);
        magusFight.tickAfter();
        companion.tick(server);
        tickForms();
        if (ticks % 40 == 0) {
            data().settleStances();
            // Whoever's journal has moved on since their client last heard is told.
            for (EntityPlayerMP p : server.getPlayerList().getPlayers()) {
                String was = journals.get(p.getUniqueID());
                NBTTagCompound journal = Journal.of(this, p);
                if (was == null || !was.equals(journal.toString())) {
                    sendState(p);
                }
                if (enabled() && !journal.getString("chapter").isEmpty()) {
                    // The story has found them: if others are here, would they go on together?
                    Fellowship.offer(server, p);
                }
            }
        }
    }

    /** Every five seconds the next shape, and after the last she goes up in smoke. */
    private void tickForms() {
        EntityNorse d = forms;
        if (d == null) {
            return;
        }
        long held = ticks - formsAt;
        if (d.isDead) {
            forms = null;
        } else if (held >= FORMS_SKULL + FORM_TICKS) {
            ((net.minecraft.world.WorldServer) d.world).spawnParticle(net.minecraft.util.EnumParticleTypes.SMOKE_LARGE,
                    d.posX, d.posY + 1D, d.posZ, 60, 0.3D, 0.9D, 0.3D, 0.02D);
            d.world.playSound(null, d.posX, d.posY + 1D, d.posZ, net.minecraft.init.SoundEvents.ENTITY_ILLAGER_MIRROR_MOVE,
                    net.minecraft.util.SoundCategory.NEUTRAL, 1.2F, 0.5F);
            d.setDead();
            forms = null;
        } else if (held == FORMS_SKULL) {
            d.shapeshift(com.matoon.eternity.client.render.NorseBoxes.DEATH);
        } else if (held == FORMS_TEAR) {
            d.tear();
        } else if (held < FORMS_TEAR && held % FORM_TICKS == 0) {
            // The woman in mourning; then the pretty one, worse each time, as if you were further gone each time.
            int step = (int) (held / FORM_TICKS);
            if (step > 0) {
                d.setRot(step - 1);
                d.shapeshift(com.matoon.eternity.client.render.NorseBoxes.TEMPTRESS);
            }
        }
    }

    /** Falling off the Sovereign station: caught every tick, at the start of it. */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            Sovereign.catchFalling(event.player);
        }
    }

    /** What each player's client was last told their journal says. */
    private final Map<UUID, String> journals = new HashMap<>();

    // ------------------------------------------------------------------ conversations

    /**
     * Starts a conversation with someone, taking them out of any they were already in (which
     * ends it, if they were alone in it). Whoever is standing with them is drawn in at once.
     */
    public void talk(EntityPlayerMP player, Talk talk) {
        Talk was = talks.put(player.getUniqueID(), talk);
        if (was != null && was != talk) {
            if (was.members.size() > 1) {
                was.leave(player.getUniqueID());
            } else {
                was.stop(player);
            }
        }
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server != null) {
            gather(server, talk);
        }
    }

    /** Draws anyone near a conversation, and not in one of their own, into it. */
    private void gather(MinecraftServer server, Talk t) {
        Entity c = t.center();
        if (c == null) {
            return;
        }
        for (EntityPlayerMP p : server.getPlayerList().getPlayers()) {
            if (p.dimension == c.dimension && !p.isSpectator() && !talks.containsKey(p.getUniqueID()) && p.getDistance(c) < t.reach) {
                talks.put(p.getUniqueID(), t);
                t.join(p);
            }
        }
    }

    /**
     * Brings someone into a conversation wherever they are, out of any other they were in: for the
     * story's turns that everyone questing together should be there for. They still drop out if
     * they stray further from it than it allows.
     */
    void draw(Talk talk, EntityPlayerMP player) {
        Talk was = talks.get(player.getUniqueID());
        if (was == talk) {
            return;
        }
        if (was != null) {
            if (was.members.size() > 1) {
                was.leave(player.getUniqueID());
            } else {
                was.abandon(player);
            }
        }
        talks.put(player.getUniqueID(), talk);
        talk.join(player);
    }

    /** Draws in everyone questing with someone who is in the same dimension as them. */
    void drawFellows(Talk talk, EntityPlayerMP player) {
        for (EntityPlayerMP p : fellowship(player)) {
            if (p != player && p.dimension == player.dimension && !p.isSpectator()) {
                draw(talk, p);
            }
        }
    }

    /** Everyone questing with someone who is online, them first. */
    List<EntityPlayerMP> fellowship(EntityPlayerMP player) {
        List<EntityPlayerMP> out = new ArrayList<>();
        for (UUID id : data().fellows(player.getUniqueID())) {
            EntityPlayerMP p = id.equals(player.getUniqueID()) ? player : player(id);
            if (p != null) {
                out.add(p);
            }
        }
        return out;
    }

    /** Players who have just been through something in the story together: from now on they quest together. */
    void together(List<EntityPlayerMP> players) {
        if (players.size() < 2 || !EternityConfig.story.fellowships) {
            return;
        }
        StoryData d = data();
        EntityPlayerMP first = players.get(0);
        for (EntityPlayerMP p : players) {
            if (p != first && d.bond(first.getUniqueID(), p.getUniqueID())) {
                for (EntityPlayerMP each : fellowship(first)) {
                    each.sendMessage(Fellowship.joined(p, first, each));
                    sendState(each);
                }
                Fellowship.refresh(first.getServer());
            }
        }
    }

    /** Marks a beat seen by someone and everyone sharing their conversation. */
    void mark(EntityPlayerMP player, String beat) {
        StoryData d = data();
        for (EntityPlayerMP p : party(player)) {
            d.player(p.getUniqueID()).beats.add(beat);
        }
        d.markDirty();
    }

    /** Everyone sharing someone's conversation, them included; just them if they are in none. */
    List<EntityPlayerMP> party(EntityPlayerMP player) {
        Talk t = talks.get(player.getUniqueID());
        List<EntityPlayerMP> out = t == null ? new ArrayList<>() : t.present();
        if (!out.contains(player)) {
            out.add(player);
        }
        return out;
    }

    public boolean talking(EntityPlayer player) {
        return talks.containsKey(player.getUniqueID());
    }

    /** Ends whatever conversation someone is in, for everyone in it, without doing anything it would have done. */
    void cut(EntityPlayerMP player) {
        Talk was = talks.get(player.getUniqueID());
        if (was != null) {
            cut(was);
        }
    }

    /** Ends a conversation for everyone in it, without doing anything it would have done. */
    void cut(Talk t) {
        t.stop(null);
        talks.values().removeIf(v -> v == t);
    }

    /** Ends whatever conversation someone is in, as if they had walked away. */
    public void hush(EntityPlayerMP player) {
        Talk was = talks.get(player.getUniqueID());
        if (was != null) {
            was.abandon(player);
            talks.values().removeIf(v -> v == was);
        }
    }

    /** Says a line to whoever it is for and everyone near enough to overhear. */
    static void line(EntityPlayerMP to, Entity at, byte who, int entity, String text) {
        line(to, at, who, entity, text, false);
    }

    /** As {@link #line}; said in the thick of a fight, it leaves everyone's view where it is. */
    static void line(EntityPlayerMP to, Entity at, byte who, int entity, String text, boolean inAction) {
        line(java.util.Collections.singletonList(to), at, who, entity, text, inAction);
    }

    /** As {@link #line}, to everyone in a conversation however far they are from whoever is speaking, and anyone in earshot. */
    static void line(List<EntityPlayerMP> to, Entity at, byte who, int entity, String text, boolean inAction) {
        MsgStory msg = MsgStory.line(who, entity, text);
        if (inAction) {
            msg.tag.setBoolean("a", true);
        }
        for (EntityPlayerMP p : to) {
            EternityNetwork.send(msg, p);
        }
        for (EntityPlayer other : at.world.playerEntities) {
            if (!to.contains(other) && other instanceof EntityPlayerMP && other.getDistance(at) < Talk.EARSHOT) {
                EternityNetwork.send(msg, (EntityPlayerMP) other);
            }
        }
    }

    /**
     * Something said in the middle of a fight, in the panel, to everyone near enough to hear,
     * leaving their view alone; never over a conversation they are already in. False if the
     * story is off, so whoever said it can fall back on chat.
     */
    public static boolean shout(Entity at, byte who, int entity, String text, double range) {
        if (!enabled() || at.world.isRemote) {
            return false;
        }
        MsgStory msg = MsgStory.line(who, entity, text);
        msg.tag.setBoolean("a", true);
        for (EntityPlayer other : at.world.playerEntities) {
            if (other instanceof EntityPlayerMP && other.getDistance(at) < range && !INSTANCE.talking(other)) {
                EternityNetwork.send(msg, (EntityPlayerMP) other);
            }
        }
        return true;
    }

    /** What a player's client sends back: an answer, or a scene finished. */
    public void act(EntityPlayerMP player, byte type, int id, int value) {
        if (type == MsgStoryAct.ANSWER) {
            Talk t = talks.get(player.getUniqueID());
            if (t != null && t.id == id) {
                t.vote(player, value);
            }
        } else if (type == MsgStoryAct.LIFT) {
            oath.reachForCube(player);
        } else if (type == MsgStoryAct.REACH) {
            oath.reachForVision(player);
        } else if (type == MsgStoryAct.SKIP) {
            Talk t = talks.get(player.getUniqueID());
            if (t != null) {
                t.skip(player);
            }
        } else if (type == MsgStoryAct.STRUGGLE) {
            magusFight.press(player);
        } else if (type == MsgStoryAct.SCENE_DONE) {
            scenesDone.computeIfAbsent(player.getUniqueID(), k -> new HashSet<>()).add(id);
        }
    }

    /**
     * Plays a cutscene for someone and everyone sharing their conversation; whether the one it
     * was played for has seen it through is {@link #sceneDone}.
     */
    void scene(EntityPlayerMP player, int scene, Vec3d a, Vec3d b) {
        MsgStory msg = MsgStory.fx(MsgStory.CUTSCENE, a, b, scene);
        Talk t = talks.get(player.getUniqueID());
        if (t != null) {
            // Whoever is drawn into the conversation while it plays sees it too.
            t.playing(msg);
            sceneTalks.put(scene, t);
        } else {
            sceneTalks.remove(scene);
        }
        for (EntityPlayerMP p : party(player)) {
            Set<Integer> done = scenesDone.get(p.getUniqueID());
            if (done != null) {
                done.remove(scene);
            }
            EternityNetwork.send(msg, p);
        }
    }

    /**
     * Whether a scene has been seen through: by whoever it was played for, or by anyone in the conversation it was
     * played in (they may have dropped out of it, and someone else be leading it now).
     */
    boolean sceneDone(EntityPlayerMP player, int scene) {
        if (sawScene(player.getUniqueID(), scene)) {
            return true;
        }
        Talk t = sceneTalks.get(scene);
        if (t != null) {
            for (UUID id : t.members) {
                if (sawScene(id, scene)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean sawScene(UUID player, int scene) {
        Set<Integer> done = scenesDone.get(player);
        return done != null && done.contains(scene);
    }

    /** Which conversation each scene was last played in. */
    private final Map<Integer, Talk> sceneTalks = new HashMap<>();

    // ------------------------------------------------------------------ the world, handed on

    /** Someone lays a hand on one of the gallery's sleepers: it is the capsule's glass they mean. */
    public void touchSleeper(EntityPlayerMP player, net.minecraft.util.math.BlockPos foot) {
        if (!talking(player)) {
            sovereign.readPod(player, foot);
        }
    }

    /** Someone lays a hand on one of the story's people. */
    public void interact(EntityPlayerMP player, EntityNorse npc) {
        if (betrayal.raised(player, npc)) {
            // The Regulator, turned on her: whatever she was in the middle of saying.
            return;
        }
        if (talking(player)) {
            return;
        }
        if ("sleeper".equals(npc.role)) {
            // A hand on the capsule's glass reaches him first: it is the capsule they mean.
            sovereign.readPod(player, new net.minecraft.util.math.BlockPos(npc.posX, npc.posY, npc.posZ));
            return;
        }
        if (!aftermath.interact(player, npc) && !oath.interact(player, npc) && !vault.interact(player, npc) && !arena.interact(player, npc) && !garden.interact(player, npc)) {
            presence.interact(player, npc);
        }
    }

    /** Someone strikes one of the story's people. Nothing happens to them; it is noticed. */
    public void struck(EntityPlayerMP player, EntityNorse npc) {
        oath.struck(player, npc);
    }

    /** One of the dead lands a blow on one of the living people of the story. */
    public void draugrStruck(EntityDraugr draugr, EntityNorse npc) {
        oath.draugrStruck(draugr, npc);
    }

    /** Someone reads what is cut into a stone. */
    public void readStone(EntityPlayerMP player, BlockPos pos, int stone) {
        if (talking(player)) {
            return;
        }
        EternityNetwork.send(MsgStory.line(Speakers.STONE, -1, Stones.carved(stone)), player);
        oath.read(player, stone);
    }

    /** Someone lays a hand on one of the barrow's doors. */
    public void touchSeal(EntityPlayerMP player, BlockPos pos) {
        if (!talking(player)) {
            oath.touchSeal(player, pos);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getWorld().isRemote || !(event.getEntityPlayer() instanceof EntityPlayerMP) || event.getHand() != net.minecraft.util.EnumHand.MAIN_HAND) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.getEntityPlayer();
        if (oath.useBlock(player, event.getPos()) || vault.useBlock(player, event.getPos())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onUseItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getWorld().isRemote || !(event.getEntityPlayer() instanceof EntityPlayerMP)) {
            return;
        }
        if (oath.useItem((EntityPlayerMP) event.getEntityPlayer(), event.getItemStack())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        EntityLivingBase dead = event.getEntityLiving();
        if (dead.world.isRemote) {
            return;
        }
        if (dead instanceof EntityPlayerMP && magusFight.spare((EntityPlayerMP) dead)) {
            // Felled with Odin in the Magus's last fight: not dead. He has what he came for.
            event.setCanceled(true);
            return;
        }
        vault.died(dead, event.getSource());
        presence.died(dead, event.getSource());
        obsession.killed(dead, event.getSource());
        if (dead instanceof EntityPlayerMP) {
            garden.died((EntityPlayerMP) dead);
            arena.fell((EntityPlayerMP) dead);
            magusFight.died((EntityPlayerMP) dead);
        }
    }

    /** What someone the Gardener's roots took down was carrying, his roots push out through the hedge. */
    @SubscribeEvent
    public void onDrops(net.minecraftforge.event.entity.player.PlayerDropsEvent event) {
        if (event.getEntityPlayer() instanceof EntityPlayerMP && !event.getEntityPlayer().world.isRemote) {
            garden.drops((EntityPlayerMP) event.getEntityPlayer(), event.getDrops());
        }
    }

    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            garden.respawned((EntityPlayerMP) event.player);
            arena.respawned((EntityPlayerMP) event.player);
            magusFight.respawned((EntityPlayerMP) event.player);
            sendState((EntityPlayerMP) event.player);
        }
    }

    // ------------------------------------------------------------------ ways through his hedge

    /** The Champion's ticket held up: he comes, or goes. */
    public void ticket(EntityPlayerMP player) {
        companion.ticket(player);
    }

    /** A stem of his hedge, frozen through by the Casket, broken by hand. */
    public void hedgeShattered(EntityPlayerMP player, BlockPos pos) {
        garden.shattered(player, pos);
    }

    /** The Casket's winter, poured out along a cone or all round someone: whatever of his hedge it reaches freezes through. */
    public void frostHedge(EntityPlayer by, Vec3d from, Vec3d dir, double length, double spread, double burst) {
        garden.frost(by, from, dir, length, spread, burst);
    }

    /** Whether someone stands where the stones in their hands could open his hedge (server side). */
    public boolean nearHedge(EntityPlayer player) {
        StoryData d = data();
        return d.gardenAt != null && d.garden == StoryData.Garden.HEDGED && d.gardenBreach.isEmpty()
                && nearHedge(d.gardenAt, d.gardenDim, player);
    }

    /** Whether someone is close enough to a standing hedge round a garden centred on a point to reach it with a stone. */
    public static boolean nearHedge(BlockPos middle, int dim, Entity entity) {
        double dx = entity.posX - middle.getX() - 0.5D, dz = entity.posZ - middle.getZ() - 0.5D;
        double r = GardenPlot.HEDGE + 16D;
        return entity.dimension == dim && dx * dx + dz * dz < r * r && Math.abs(entity.posY - middle.getY()) < 24D;
    }

    /** The Reality Stone, at his hedge: there was never a hedge there at all. */
    public boolean rewriteHedge(EntityPlayer player) {
        return player instanceof EntityPlayerMP && nearHedge(player) && garden.rewrite((EntityPlayerMP) player);
    }

    /** The Soul Stone, at his hedge: everything in the garden that is alive, drunk dry. */
    public boolean reapGarden(EntityPlayer player) {
        return player instanceof EntityPlayerMP && nearHedge(player) && garden.reap((EntityPlayerMP) player);
    }

    /** The Power Stone in a fist against the Gardener's hedge. */
    @SubscribeEvent
    public void onPunch(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getWorld().isRemote || !(event.getEntityPlayer() instanceof EntityPlayerMP)) {
            return;
        }
        if (garden.punch((EntityPlayerMP) event.getEntityPlayer(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    /** Something with the Aether in it is now and then taken by the world's creatures for one of their own. */
    @SubscribeEvent
    public void onTarget(LivingSetAttackTargetEvent event) {
        if (event.getTarget() instanceof EntityPlayerMP && event.getEntityLiving() instanceof net.minecraft.entity.EntityLiving
                && !event.getEntityLiving().world.isRemote && obsession.spares(event.getEntityLiving(), (EntityPlayerMP) event.getTarget())) {
            // One of the dead, turning on someone she has claimed: it thinks better of it.
            net.minecraft.entity.EntityLiving undead = (net.minecraft.entity.EntityLiving) event.getEntityLiving();
            undead.setAttackTarget(null);
            undead.setRevengeTarget(null);
            return;
        }
        if (event.getTarget() instanceof EntityPlayerMP && !event.getEntityLiving().world.isRemote
                && event.getEntityLiving() instanceof net.minecraft.entity.EntityLiving && !(event.getEntityLiving() instanceof EntityDraugr)) {
            vault.targeted((net.minecraft.entity.EntityLiving) event.getEntityLiving(), (EntityPlayerMP) event.getTarget());
        }
    }

    /** Whatever the dead were already throwing at someone she has claimed doesn't land. */
    @SubscribeEvent
    public void onAttacked(net.minecraftforge.event.entity.living.LivingAttackEvent event) {
        Entity from = event.getSource().getTrueSource();
        if (event.getEntityLiving() instanceof EntityPlayerMP && from instanceof EntityLivingBase && !from.world.isRemote
                && obsession.spares((EntityLivingBase) from, (EntityPlayerMP) event.getEntityLiving())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            inherit((EntityPlayerMP) event.player);
            vault.caughtUp((EntityPlayerMP) event.player);
            sendState((EntityPlayerMP) event.player);
        }
    }

    /**
     * A single-player world remembers its one player's story by who they are; a launcher that
     * hands out a different name (and so a different identity) each time would leave them a
     * stranger to their own save. So in a world nobody else has ever played, someone with no story
     * of their own takes up the one that is there.
     */
    private void inherit(EntityPlayerMP player) {
        MinecraftServer server = player.getServer();
        if (server == null || server.isDedicatedServer() || server.getPlayerList().getCurrentPlayerCount() > 1) {
            return;
        }
        StoryData d = data();
        StoryData.Player mine = d.players().get(player.getUniqueID());
        if (mine != null && (!mine.beats.isEmpty() || mine.warlock != StoryData.Stance.UNMET)) {
            return;
        }
        UUID only = null;
        for (Map.Entry<UUID, StoryData.Player> e : d.players().entrySet()) {
            if (e.getKey().equals(player.getUniqueID()) || e.getValue().beats.isEmpty() && e.getValue().warlock == StoryData.Stance.UNMET) {
                continue;
            }
            if (only != null) {
                return; // more than one other: a world shared once, not ours to guess at
            }
            only = e.getKey();
        }
        if (only == null) {
            return;
        }
        d.players().put(player.getUniqueID(), d.players().remove(only));
        d.markDirty();
        com.matoon.eternity.Eternity.LOG.info("{} takes up the story of this world's only other player ({}).", player.getName(), only);
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Talk t = talks.remove(event.player.getUniqueID());
        if (t != null) {
            if (t.members.size() > 1) {
                t.leave(event.player.getUniqueID());
            } else {
                t.abandon(null);
            }
        }
    }

    @SubscribeEvent
    public void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            if (event.toDim == com.matoon.eternity.world.OdinsVault.dimension()) {
                vault.entered((EntityPlayerMP) event.player);
            } else if (event.fromDim == com.matoon.eternity.world.OdinsVault.dimension()) {
                vault.caughtUp((EntityPlayerMP) event.player);
            }
            sendState((EntityPlayerMP) event.player);
        }
    }

    // ------------------------------------------------------------------ what each client knows

    /** What the story has made of a player, and of the world round them, for their client. */
    public void sendState(EntityPlayerMP player) {
        StoryData d = data();
        NBTTagCompound tag = new NBTTagCompound();
        StoryData.Player p = d.player(player.getUniqueID());
        tag.setBoolean("aether", p.aether);
        tag.setBoolean("aetherTaken", d.aetherTaken);
        tag.setBoolean("flameSpent", d.flameSpent);
        tag.setBoolean("story", enabled());
        if (d.barrow != null) {
            tag.setLong("barrow", d.barrow.toLong());
            tag.setInteger("facing", d.barrowFacing);
        }
        tag.setBoolean("ward", d.wardLit && d.barrow != null);
        tag.setInteger("spoken", d.spoken);
        BlockPos home = player.getBedLocation(0);
        if (home == null) {
            home = player.world.getSpawnPoint();
        }
        tag.setLong("home", home.toLong());
        tag.setBoolean("ticking", garden.ticking(player));
        StoryData.Garden standing = d.garden;
        if (d.gardenAt != null && standing == StoryData.Garden.HEDGED && d.gardenBreach.isEmpty()) {
            // Where his hedge stands whole, so their client knows when the stones they hold could open it.
            tag.setLong("hedge", d.gardenAt.toLong());
            tag.setInteger("hedgeDim", d.gardenDim);
        }
        NBTTagCompound journal = Journal.of(this, player);
        journals.put(player.getUniqueID(), journal.toString());
        tag.setTag("journal", journal);
        EternityNetwork.send(MsgStory.state(tag), player);
    }

    public void sendStateToAll() {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server != null) {
            for (EntityPlayerMP p : server.getPlayerList().getPlayers()) {
                sendState(p);
            }
        }
    }

    // ------------------------------------------------------------------ for the rest of the mod

    /** True while the vault's fallen guards should lie where they fell rather than be carried off. */
    public boolean keepsFallen(Entity guard) {
        return vault.keepsFallen(guard);
    }

    /** The Champion has called someone out (chapter 3): true if there is a word before the bell, and he should keep his own quiet. */
    /** A hand on one of the Sovereign station's doors. */
    public void sovereignDoor(EntityPlayer player, net.minecraft.util.math.BlockPos pos) {
        if (player instanceof EntityPlayerMP) {
            sovereign.touchDoor((EntityPlayerMP) player, pos);
        }
    }

    /** A hand on one of the station's records consoles. */
    public void sovereignConsole(EntityPlayer player, net.minecraft.util.math.BlockPos pos) {
        if (player instanceof EntityPlayerMP) {
            sovereign.readConsole((EntityPlayerMP) player, pos);
        }
    }

    /** Someone looking into one of the gallery's capsules. */
    public void sovereignPod(EntityPlayer player, net.minecraft.util.math.BlockPos pos) {
        if (player instanceof EntityPlayerMP) {
            sovereign.readPod((EntityPlayerMP) player, pos);
        }
    }

    public boolean duelBegins(com.matoon.eternity.entity.EntityChampion champion, EntityPlayerMP player) {
        return arena.duelBegins(champion, player);
    }

    /** Planet Breaker has broken Tamarata under everyone on it: true if the story says so, instead of him in chat. */
    public boolean planetBreaks(com.matoon.eternity.entity.EntityChampion champion, java.util.List<EntityPlayerMP> near) {
        return arena.breaks(champion, near);
    }

    /** The Champion, adrift in his broken planet: true while a scene has the floor and he should keep quiet. */
    public boolean championStranded(com.matoon.eternity.entity.EntityChampion champion, java.util.List<EntityPlayerMP> near) {
        return arena.stranded(champion, near);
    }

    /** The Champion has given the stone for the rocket: true while he is paying his debt, and the rocket should wait. */
    public boolean championDebt(com.matoon.eternity.entity.EntityChampion champion, EntityPlayerMP player) {
        return arena.debt(champion, player);
    }

    /** Whether Death stands behind this visitor to the In-Betweener's glass, and he hasn't yet said so (chapter 4). */
    public boolean behindThem(UUID player) {
        StoryData.Player p = data().player(player);
        if (!enabled() || !p.seen("death_revealed") || !p.beats.add("imbalance")) {
            return false;
        }
        data().markDirty();
        return true;
    }

    /** Whether Order and Chaos warn this player that Eternity is coming, before anything else (chapter 4). */
    public boolean warnOfEternity(EntityPlayerMP player) {
        StoryData.Player p = data().player(player.getUniqueID());
        if (!enabled() || !p.seen("death_revealed") || !p.beats.add("masters_warned")) {
            return false;
        }
        data().markDirty();
        return true;
    }

    /** The vault's Destroyer has fallen. */
    public void destroyerFell() {
        vault.destroyerFell();
    }

    /** Whether this player saw the Magus rise (chapter 3, the Sovereign): Warlock stands with them, and the Power Stone is his. */
    public boolean magusRisen(EntityPlayerMP player) {
        return enabled() && data().player(player.getUniqueID()).seen("magus_risen");
    }

    /** Whom the Magus's stones are claimed under, as if he were a player: no copy of them will ever count. */
    /** The end of the Grandmaster's game on the Magus's road: true if his last fight begins right there, with these two. */
    public boolean magusAfterGame(EntityPlayerMP player, @Nullable com.matoon.eternity.entity.EntityMagus magus,
                                  @Nullable com.matoon.eternity.entity.EntityAdamWarlock warlock) {
        return enabled() && magusFight.afterGame(player, magus, warlock);
    }

    /** The gauntlet raised in front of him during the game: true if he took it, and the fight is on. */
    public boolean magusSnatch(EntityPlayerMP player, com.matoon.eternity.entity.EntityMagus magus,
                               @Nullable com.matoon.eternity.entity.EntityAdamWarlock warlock) {
        return enabled() && magusFight.snatch(player, magus, warlock);
    }

    /** Whether the gauntlet in someone's hand would be taken from them if they raised it now. */
    public static boolean gauntletRaised(EntityPlayerMP player) {
        return MagusFight.raised(player);
    }

    /** A snap: if it is theirs, in the Magus's last fight, with all six, it is only for him. True if it was. */
    public boolean magusFightSnap(net.minecraft.entity.EntityLivingBase entity) {
        return magusFight.snap(entity);
    }

    /** The stones were broken, with Eternity coming: there is nothing left for him to hide behind (see {@link Reckoning}). */
    public static boolean stonesBroken() {
        return enabled() && INSTANCE.data().seen(Reckoning.BROKE);
    }

    /** Someone snapped: Death and Warlock, if they were waiting to see which way, answer it. */
    public void snapped(net.minecraft.entity.EntityLivingBase entity, boolean broke, boolean culled) {
        if (enabled()) {
            reckoning.snapped(entity, broke, culled);
        }
    }

    /** Home from the Living Tribunal: stripped of everything, let go with all of it, or with him in the Regulator (see {@link Aftermath}). */
    public void judged(EntityPlayerMP player, boolean stripped, boolean absorbed, boolean acquitted) {
        if (enabled()) {
            aftermath.judged(player, stripped, absorbed, acquitted);
        }
    }

    /** Eternity is dead, caged or not: Warlock, if he was at their side, goes. */
    public void eternityFell(boolean contained) {
        if (enabled()) {
            reckoning.eternityFell(contained);
        }
    }

    public static UUID magusId() {
        return Sovereign.MAGUS_ID;
    }

    /** Warlock walked off the Grandmaster's board in front of them (chapter 6): he has refused it, and their game with it. */
    public void warlockRefused(EntityPlayerMP player) {
        if (!enabled()) {
            return;
        }
        data().player(player.getUniqueID()).warlock = StoryData.Stance.REFUSED;
        data().markDirty();
        sendState(player);
    }

    /** True while someone is held at the vault's doors because something is about to happen there. */
    public boolean holdsAtDoors(EntityPlayerMP player) {
        return vault.holdsAtDoors(player);
    }

    /** The vault's Tesseract stand holds the Aether instead, while the story runs. */
    public boolean aetherInVault() {
        return enabled();
    }

    /** The village Tesseracts never set out: the oath's lies under the mound. */
    public boolean suppressesVillageTesseracts() {
        return enabled() && data().oath != StoryData.Oath.SKIPPED;
    }

    // ------------------------------------------------------------------ the operator's handles

    public String status() {
        StoryData d = data();
        StringBuilder s = new StringBuilder();
        s.append("Story ").append(enabled() ? "on" : "off").append(". The oath: ").append(d.oath)
                .append(", ").append(d.leaks).append(" leaks, ").append(d.taken).append(" from home");
        if (d.village != null) {
            s.append(". Village at ").append(d.village.getX()).append(' ').append(d.village.getZ());
        }
        if (d.barrow != null) {
            s.append(". Barrow door at ").append(d.barrow.getX()).append(' ').append(d.barrow.getY()).append(' ').append(d.barrow.getZ());
        }
        s.append(". Lines read ").append(Integer.bitCount(d.linesFound)).append("/5, spoken ").append(d.spoken)
                .append(", doors ").append(d.outerOpen ? "open" : "shut").append('/').append(d.innerOpen ? "open" : "shut")
                .append(", ward ").append(d.wardLit ? "lit" : "out");
        s.append(". The vault: Aether ").append(d.aetherTaken ? "taken" : "in its lantern").append(", flame ")
                .append(d.flameSpent ? "spent" : "burning").append('.');
        for (Map.Entry<UUID, StoryData.Player> e : d.players().entrySet()) {
            EntityPlayerMP p = player(e.getKey());
            s.append(' ').append(p != null ? p.getName() : e.getKey().toString().substring(0, 8)).append(": Warlock ")
                    .append(e.getValue().warlock).append(" (trust ").append(e.getValue().trust).append(')')
                    .append(e.getValue().aether ? ", the Aether in them" : "").append('.');
        }
        return s.toString();
    }

    public String command(EntityPlayerMP player, String action, String[] args) {
        switch (action) {
            case "reset":
                for (Talk t : new HashSet<>(talks.values())) {
                    t.stop(null);
                }
                talks.clear();
                oath.clear(player.getServerWorld());
                vault.clear();
                data().forget();
                sendStateToAll();
                return "The story has forgotten everything. Nothing it built has been taken down.";
            case "pose": {
                // For looking at: one of the story's people in front of you, doing one thing.
                if (posed != null) {
                    posed.setDead();
                }
                String look = args.length > 0 ? args[0] : "stranger";
                com.matoon.eternity.entity.NorsePose pose = args.length > 1 ? com.matoon.eternity.entity.NorsePose.valueOf(args[1].toUpperCase())
                        : com.matoon.eternity.entity.NorsePose.STAND;
                net.minecraft.util.math.Vec3d at = player.getPositionVector().add(player.getLookVec().scale(3D).subtract(0D, player.getLookVec().y * 3D, 0D));
                String[] looks = {"stranger", "death", "ragna", "hrafn", "draugr", "gardener", "ayesha", "magus", "spawn", "temptress", "unveiled"};
                int i = java.util.Arrays.asList(looks).indexOf(look);
                if (i == 4) {
                    EntityDraugr g = new EntityDraugr(player.world);
                    g.laid(at, player.rotationYaw + 180F, -1, -1, true, true);
                    player.world.spawnEntity(g);
                    if (pose != com.matoon.eternity.entity.NorsePose.DEAD) {
                        g.wake(player, true);
                        g.setNoAI(pose != com.matoon.eternity.entity.NorsePose.HUNT);
                    }
                    posed = g;
                } else {
                    EntityNorse n = EntityNorse.of(player.world, Math.max(0, i), "posed");
                    n.setLocationAndAngles(at.x, at.y, at.z, player.rotationYaw + 180F, 0F);
                    n.face(player.getPositionVector());
                    player.world.spawnEntity(n);
                    n.watch(player);
                    n.act(pose).rest(pose);
                    if (args.length > 2 && "walk".equals(args[2])) {
                        n.walkTo(at.add(player.getLookVec().x * 12D, 0D, player.getLookVec().z * 12D), 0.45D);
                    } else if (args.length > 2) {
                        n.setArmed(!"shine".equals(args[2]));
                        n.setShining("shine".equals(args[2]) || "both".equals(args[2]));
                    }
                    posed = n;
                }
                return "Posed.";
            }
            case "forms":
            case "tear": {
                // Death in front of you, changing into each of her shapes in turn (rotting, then out of it), then gone;
                // or, for "tear", already at the worst, and about to take it off.
                boolean tear = "tear".equals(action);
                if (forms != null) {
                    forms.setDead();
                }
                Vec3d at = player.getPositionVector().add(player.getLookVec().scale(3D).subtract(0D, player.getLookVec().y * 3D, 0D));
                EntityNorse d = EntityNorse.of(player.world, tear ? com.matoon.eternity.client.render.NorseBoxes.TEMPTRESS
                        : com.matoon.eternity.client.render.NorseBoxes.STRANGER, "posed");
                if (tear) {
                    d.setRot(4);
                }
                d.setLocationAndAngles(at.x, at.y, at.z, player.rotationYaw + 180F, 0F);
                d.face(player.getPositionVector());
                player.world.spawnEntity(d);
                d.watch(player);
                forms = d;
                formsAt = tear ? ticks - FORMS_TEAR + 30 : ticks;
                return tear ? "Death, at the worst of it, about to take it off." : "Death: in mourning, then the pretty one rotting by stages, "
                        + FORM_TICKS / 20 + " seconds apiece; then out of it, and the skull.";
            }
            case "reckoning":
                // Warlock and Death come to have it out over your hand, now, six stones or not.
                return reckoning.force(player);
            case "aftermath":
                // Home from the Tribunal after the cull: "aftermath" stripped of everything, "aftermath powerful" with him in the Regulator, "aftermath acquitted" let go.
                return aftermath.force(player, args.length > 0 ? args[0] : "");
            case "visit":
                // She comes now, unasked, as she does once someone is far enough gone; "visit fury" and she is angry.
                return obsession.force(player, args.length > 0 && "fury".equalsIgnoreCase(args[0]));
            case "hush":
                hush(player);
                return "Conversation ended.";
            case "companion":
                return companion.command(player, args);
            case "magusfight":
                return magusFight.command(player, args);
            case "journal":
                return Journal.of(this, player).toString() + " beats=" + data().player(player.getUniqueID()).beats;
            case "phantoms":
                // phantoms <n> [answer]: n make-believe players join every vote, for testing a party with one client.
                Talk.phantoms = args.length > 0 ? Integer.parseInt(args[0]) : 0;
                Talk.phantomAnswer = args.length > 1 ? Integer.parseInt(args[1]) - 1 : -1;
                return Talk.phantoms + " phantom voters.";
            case "answer": {
                Talk t = talks.get(player.getUniqueID());
                int n = args.length > 0 ? Integer.parseInt(args[0]) - 1 : 0;
                if (t == null || !t.isAsking()) {
                    presets.computeIfAbsent(player.getUniqueID(), k -> new java.util.ArrayDeque<>()).add(n);
                    return "Answer " + (n + 1) + " will be given when next asked.";
                }
                t.vote(player, n);
                return "Voted " + (n + 1) + ".";
            }
            default:
                String r = oath.command(player, action, args);
                if (r == null) {
                    r = vault.command(player, action, args);
                }
                if (r == null) {
                    r = presence.command(player, action, args);
                }
                if (r == null) {
                    r = arenaCommand(player, action, args);
                }
                if (r == null) {
                    r = glass.command(player, action, args);
                }
                if (r == null) {
                    r = garden.command(player, action, args);
                }
                if (r == null) {
                    r = sovereign.command(player, action, args);
                }
                return r != null ? r : "Unknown: " + action;
        }
    }

    @Nullable
    private String arenaCommand(EntityPlayerMP player, String action, String[] args) {
        StoryData.Player p = data().player(player.getUniqueID());
        switch (action) {
            case "devotion":
                // devotion <n>: how far they have fallen for Death; unveiled forgets that she has shown herself.
                p.devotion = args.length > 0 ? Integer.parseInt(args[0]) : 0;
                p.beats.remove(Devotion.UNVEILED);
                if (p.devotion > 0) {
                    // She can only want someone who knows what she is.
                    p.beats.add("death_revealed");
                }
                p.nextVisit = -1;
                data().markDirty();
                return "Devotion " + p.devotion + " (rot " + Devotion.rot(p.devotion) + (p.devotion >= Obsession.UNHINGED ? ", unhinged" : "")
                        + "); she has not shown herself.";
            case "stance":
                p.warlock = StoryData.Stance.valueOf(args.length > 0 ? args[0].toUpperCase() : "RIVAL");
                p.beats.add("death_revealed");
                data().markDirty();
                return "Warlock is your " + p.warlock + ".";
            case "champion": {
                // Back to arriving on Tamarata: the Champion and his world whole, the stone in his brow, and
                // nobody yet called out, fallen, robbed or owed.
                for (Talk t : new HashSet<>(talks.values())) {
                    t.stop(null);
                }
                talks.clear();
                arena.clear();
                com.matoon.eternity.world.ChampionDirector.INSTANCE.restore();
                String[] beats = {"arena_called", "first_loss", "warlock_lost_champion", "warlock_in_arena", "warlock_took", "dir_station",
                        "champion_helped", "champion_debt", Arena.EMPOWERED, Arena.DISARMED};
                for (StoryData.Player o : data().players().values()) {
                    o.beats.removeAll(java.util.Arrays.asList(beats));
                }
                data().beats.removeAll(java.util.Arrays.asList(beats));
                data().markDirty();
                com.matoon.eternity.world.SovereignData s = com.matoon.eternity.world.SovereignData.get();
                String sovereign = "";
                if (s.station == com.matoon.eternity.world.SovereignData.Station.ABANDONED
                        || s.station == com.matoon.eternity.world.SovereignData.Station.UNSEEN && s.warlockHasStone()) {
                    // He only got as far as taking it home (or out of the old man's brow): he never took it.
                    s.station = com.matoon.eternity.world.SovereignData.Station.UNSEEN;
                    s.stone = net.minecraft.item.ItemStack.EMPTY;
                    s.takenFrom = "";
                    s.markDirty();
                    com.matoon.eternity.compat.Relics.release(com.matoon.eternity.compat.Relics.Kind.POWER_STONE);
                } else if (s.station != com.matoon.eternity.world.SovereignData.Station.UNSEEN) {
                    sovereign = " (The Sovereign has already been confronted; that was left as it is.)";
                }
                sendStateToAll();
                return "Tamarata is whole and the Champion is waiting, stone in his brow. Nobody has been called out yet." + sovereign;
            }
            case "arena":
            case "debt": {
                com.matoon.eternity.entity.EntityChampion c = null;
                for (Entity e : player.world.loadedEntityList) {
                    if (e instanceof com.matoon.eternity.entity.EntityChampion && (c == null || e.getDistance(player) < c.getDistance(player))) {
                        c = (com.matoon.eternity.entity.EntityChampion) e;
                    }
                }
                if (c == null) {
                    return "No Champion here.";
                }
                p.beats.add("death_revealed");
                p.beats.remove("arena_called");
                p.beats.remove("champion_debt");
                if ("arena".equals(action)) {
                    arena.duelBegins(c, player);
                } else {
                    arena.debt(c, player);
                }
                return "The Champion.";
            }
            default:
                return null;
        }
    }

    static boolean near(Entity a, Vec3d b, double r) {
        return a.getDistanceSq(b.x, b.y, b.z) < r * r;
    }

    static World overworld() {
        return FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(0);
    }
}
