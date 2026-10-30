package com.matoon.eternity.world;

import com.matoon.eternity.EternitySounds;
import com.matoon.eternity.entity.EntityOrderChaos;
import com.matoon.eternity.item.ItemAstralRegulator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Master Order and Lord Chaos, come after the In-Betweener's glass broke. They are not
 * Eternity, and they do not simply unmake whoever is there. They arrive, find nothing left in
 * him worth putting back, let his empty body drift off into the Nexus, and turn to whoever is
 * still near the junction: the thief, if the thief stayed, or anyone else who did. They speak first (see
 * {@link InBetweenerDialogue}), and what is said changes how they fight: anger makes them
 * hit harder, amusement makes Chaos play with you, handing back the gem costs them a
 * quarter of their strength before the fight begins, and Eternity in your hand makes your
 * blows land twice over. Then they fight everyone who stays.
 *
 * Beaten, each lies dazed for a little while, and a charged Astral Regulator can draw it
 * in; left alone, it withdraws. Whichever is drawn in never comes again. Flee far enough
 * from the junction and they let you go, for now.
 *
 * This class also holds what the two of them share during the fight: Order's edict, the
 * point where their beams cross, and the masks Chaos hides among.
 */
public final class OrderAndChaos {

    public static final OrderAndChaos INSTANCE = new OrderAndChaos();

    /** Beyond this far from the junction, they do not follow. */
    private static final double LEASH = 120D;
    private static final int TALK_TIMEOUT = 20 * 120;

    public static final int EDICT_STILL = 0, EDICT_PEACE = 1, EDICT_GAZE = 2;

    private enum Phase { NONE, TALK, FIGHT, DEPART }

    private Phase phase = Phase.NONE;
    private int ticks;
    private UUID speaker;
    private EntityOrderChaos order, chaos;
    private final List<EntityOrderChaos> masks = new ArrayList<>();
    private boolean orderAngered, chaosAngered, chaosAmused, resonance, returned;
    private final Set<String> fought = new LinkedHashSet<>();
    /** Whom they have already noticed carrying an Astral Regulator this time. */
    private final Set<UUID> sawRegulator = new HashSet<>();
    private int edict = -1;
    private int edictLeft;
    private Vec3d crossing;

    private OrderAndChaos() {
    }

    public void reset() {
        phase = Phase.NONE;
        sawRegulator.clear();
        speaker = null;
        order = chaos = null;
        masks.clear();
        fought.clear();
        edict = -1;
        crossing = null;
    }

    public boolean active() {
        return phase != Phase.NONE;
    }

    /**
     * They arrive at the junction: whichever of them is not inside a Regulator. Someone
     * near must answer for the breaking; if nobody is, they go, and the thief has got
     * away.
     */
    public void arrive(WorldServer nexus, @Nullable EntityPlayerMP who) {
        vanish();
        orderAngered = chaosAngered = chaosAmused = resonance = returned = false;
        fought.clear();
        InBetweenerData d = InBetweenerDirector.INSTANCE.data();
        if (who == null) {
            if (!d.orderAbsorbed) {
                say(nexus, true, "Nobody answers for this. Noted.");
            }
            if (!d.chaosAbsorbed) {
                say(nexus, false, "They RAN! Oh, I love it when they run. We'll see them again. Everyone sees us again.");
            }
            return;
        }
        order = d.orderAbsorbed ? null : spawn(nexus, EntityOrderChaos.Kind.ORDER);
        chaos = d.chaosAbsorbed ? null : spawn(nexus, EntityOrderChaos.Kind.CHAOS);
        if (order == null && chaos == null) {
            return;
        }
        speaker = who.getUniqueID();
        phase = Phase.TALK;
        ticks = 0;
        if (order == null) {
            say(nexus, false, "My brother is... indisposed. In a jar. So it's just me, and I don't do speeches.");
        } else if (chaos == null) {
            say(nexus, true, "My brother has been contained. I will manage alone.");
        }
    }

    private static EntityOrderChaos spawn(WorldServer nexus, EntityOrderChaos.Kind kind) {
        EntityOrderChaos head = new EntityOrderChaos(nexus);
        head.setKind(kind);
        double side = kind == EntityOrderChaos.Kind.ORDER ? 1D : -1D;
        head.setLocationAndAngles(Nexus.JUNCTION_X + side * 30D, Nexus.BUBBLE_Y + 25D, Nexus.JUNCTION_Z, side > 0 ? 90F : -90F, 0F);
        nexus.spawnEntity(head);
        return head;
    }

    /** The one they are speaking to. */
    @Nullable
    public EntityPlayerMP speaker() {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        return server == null || speaker == null ? null : server.getPlayerList().getPlayerByUUID(speaker);
    }

    /** Called by each head every tick; any head this encounter does not own dissolves. */
    public boolean claim(EntityOrderChaos head) {
        return phase != Phase.NONE && (head == order || head == chaos || masks.contains(head));
    }

    @Nullable
    public EntityOrderChaos order() {
        return order != null && !order.isDead ? order : null;
    }

    @Nullable
    public EntityOrderChaos chaos() {
        return chaos != null && !chaos.isDead ? chaos : null;
    }

    // ------------------------------------------------------------------ what was said

    void angerOrder() {
        orderAngered = true;
    }

    void angerBoth() {
        orderAngered = true;
        chaosAngered = true;
    }

    void amuseChaos() {
        chaosAmused = true;
    }

    void resonate() {
        resonance = true;
    }

    void gemReturned() {
        returned = true;
    }

    public boolean chaosAmused() {
        return chaosAmused;
    }

    /** Whether they recognised Eternity in the speaker's hand. */
    public boolean resonance() {
        return resonance;
    }

    /** How hard each hits, by how angered it was. */
    public float might(EntityOrderChaos.Kind kind) {
        boolean angered = kind == EntityOrderChaos.Kind.ORDER ? orderAngered : chaosAngered;
        return angered ? 1.25F : 1F;
    }

    // ------------------------------------------------------------------ what they share in the fight

    /** Master Order lays down an edict; it holds for everyone at the junction until it lapses. */
    public void decree(int rule, int duration) {
        edict = rule;
        edictLeft = duration;
    }

    /** The edict that holds now, or -1. */
    public int edict() {
        return edictLeft > 0 ? edict : -1;
    }

    /** Where Order's beam and Chaos's cross, while they cross. */
    @Nullable
    public Vec3d crossing() {
        return crossing;
    }

    public void setCrossing(@Nullable Vec3d at) {
        crossing = at;
    }

    /** Order calls on his brother to cross their beams on someone. Chaos joins if he is free to. */
    public boolean summonCrossing() {
        EntityOrderChaos c = chaos();
        return c != null && c.joinCrossing();
    }

    /** Chaos hides among copies of himself. */
    public void addMask(EntityOrderChaos mask) {
        masks.add(mask);
    }

    public List<EntityOrderChaos> masks() {
        masks.removeIf(m -> m.isDead);
        return masks;
    }

    /** The masks fall away. */
    public void dropMasks() {
        for (EntityOrderChaos mask : masks) {
            mask.setDead();
        }
        masks.clear();
    }

    // ------------------------------------------------------------------ the fight

    /** The talking is over. */
    public void fight() {
        if (phase != Phase.TALK) {
            return;
        }
        phase = Phase.FIGHT;
        ticks = 0;
        for (EntityOrderChaos head : heads()) {
            head.engage();
            if (returned) {
                // Restitution was noted: taking the gem back into him cost them something.
                head.weaken(0.75F);
            }
        }
        WorldServer nexus = DimensionManager.getWorld(Nexus.dimension());
        if (nexus != null) {
            nexus.playSound(null, Nexus.JUNCTION_X, Nexus.BUBBLE_Y, Nexus.JUNCTION_Z, EternitySounds.FINALE_DRUMS, SoundCategory.HOSTILE, 16F, 0.6F);
        }
    }

    /** Everyone they are fighting: every living player near enough to the junction. */
    public List<EntityPlayerMP> participants() {
        List<EntityPlayerMP> players = new ArrayList<>();
        WorldServer nexus = DimensionManager.getWorld(Nexus.dimension());
        if (nexus == null || phase != Phase.FIGHT) {
            return players;
        }
        for (EntityPlayer player : nexus.playerEntities) {
            double dx = player.posX - Nexus.JUNCTION_X, dz = player.posZ - Nexus.JUNCTION_Z;
            if (player instanceof EntityPlayerMP && player.isEntityAlive() && !player.isSpectator() && dx * dx + dz * dz < LEASH * LEASH) {
                players.add((EntityPlayerMP) player);
            }
        }
        return players;
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || phase == Phase.NONE) {
            return;
        }
        WorldServer nexus = DimensionManager.getWorld(Nexus.dimension());
        if (nexus == null) {
            reset();
            return;
        }
        ticks++;
        if (edictLeft > 0) {
            edictLeft--;
        }
        if (order != null && order.isDead && !order.isWithdrawing()) {
            order = null;
        }
        if (chaos != null && chaos.isDead && !chaos.isWithdrawing()) {
            chaos = null;
        }
        switch (phase) {
            case TALK: {
                EntityPlayerMP who = speaker();
                if (who == null || who.world != nexus || !who.isEntityAlive() || ticks > TALK_TIMEOUT || order == null || chaos == null) {
                    // Whoever they were speaking to is gone, or one of them came alone and does not talk; the others answer for it.
                    InBetweenerDirector.INSTANCE.silenceMasters();
                    fight();
                }
                break;
            }
            case FIGHT: {
                List<EntityPlayerMP> players = participants();
                for (EntityPlayerMP player : players) {
                    fought.add(player.getName());
                }
                watchRegulators(nexus, players);
                if (heads().isEmpty()) {
                    victory(nexus);
                } else if (players.isEmpty() && ticks > 40) {
                    depart(nexus);
                }
                break;
            }
            case DEPART:
                if (heads().isEmpty()) {
                    dropMasks();
                    reset();
                }
                break;
            default:
                break;
        }
    }

    private List<EntityOrderChaos> heads() {
        List<EntityOrderChaos> heads = new ArrayList<>();
        if (order != null && !order.isDead) {
            heads.add(order);
        }
        if (chaos != null && !chaos.isDead) {
            heads.add(chaos);
        }
        return heads;
    }

    /** One of them has been driven off; the other fights on enraged. */
    public void withdrawn(EntityOrderChaos head) {
        WorldServer nexus = DimensionManager.getWorld(Nexus.dimension());
        if (head == order) {
            order = null;
            crossing = null;
            if (nexus != null && phase == Phase.FIGHT) {
                say(nexus, true, "This is not defeat. It is... POSTPONEMENT.");
                if (chaos != null) {
                    say(nexus, false, "Brother? BROTHER. Oh, now I'm cross.");
                    chaos.enrage();
                }
            }
        } else if (head == chaos) {
            chaos = null;
            crossing = null;
            dropMasks();
            if (nexus != null && phase == Phase.FIGHT) {
                say(nexus, false, "Ow! Ha! I LIKED that. Do it again some time.");
                if (order != null) {
                    say(nexus, true, "Then I will be thorough enough for both of us.");
                    order.enrage();
                }
            }
        }
    }

    /**
     * Someone raised an Astral Regulator, empty or not. It works only on one of them lying beaten,
     * near enough and in front of the one who raised it.
     */
    public boolean tryAbsorb(EntityPlayerMP player, EnumHand hand) {
        for (EntityOrderChaos head : heads()) {
            if (!head.isDown() || head.world != player.world) {
                continue;
            }
            Vec3d toward = head.centre().subtract(player.getPositionEyes(1F));
            if (toward.length() < 48D && player.getLook(1F).dotProduct(toward.normalize()) > 0.6D) {
                return head.absorbBy(player, hand);
            }
        }
        return false;
    }

    /** One of them is inside the Regulator now, and never comes again. */
    public void absorbed(EntityOrderChaos head, EntityPlayerMP player, EnumHand hand) {
        ItemStack stack = ItemAstralRegulator.raised(player, hand);
        boolean isOrder = head.isOrder();
        if (stack.getItem() instanceof ItemAstralRegulator) {
            ItemAstralRegulator.absorb(stack, isOrder ? ItemAstralRegulator.ORDER : ItemAstralRegulator.CHAOS);
        }
        InBetweenerData d = InBetweenerDirector.INSTANCE.data();
        if (isOrder) {
            d.orderAbsorbed = true;
        } else {
            d.chaosAbsorbed = true;
        }
        d.markDirty();
        TextComponentString notice = new TextComponentString(player.getName() + " has drawn " + (isOrder ? "Master Order" : "Lord Chaos")
                + " into the Astral Regulator.");
        notice.getStyle().setColor(TextFormatting.GOLD);
        player.server.getPlayerList().sendMessage(notice);
        WorldServer nexus = DimensionManager.getWorld(Nexus.dimension());
        if (isOrder) {
            order = null;
            if (nexus != null && chaos != null) {
                say(nexus, false, "Brother? Where did you... oh. Oh, you're in the JAR. Oh, I'm going to break that jar. With them inside it.");
                chaos.enrage();
            }
        } else {
            chaos = null;
            dropMasks();
            if (nexus != null && order != null) {
                say(nexus, true, "...You put my brother in a jar. You will regret that.");
                order.enrage();
            }
        }
        crossing = null;
    }

    private void victory(WorldServer nexus) {
        InBetweenerData d = InBetweenerDirector.INSTANCE.data();
        if (!d.orderAbsorbed) {
            say(nexus, true, "That will do. For now.");
        }
        if (!d.chaosAbsorbed) {
            say(nexus, false, "Noted! I'll be thinking about you. That's never good news, but it's always interesting.");
        }
        MinecraftServer server = nexus.getMinecraftServer();
        if (server != null && !fought.isEmpty()) {
            TextComponentString notice = new TextComponentString(String.join(", ", fought) + " drove back Master Order and Lord Chaos.");
            notice.getStyle().setColor(TextFormatting.GOLD);
            server.getPlayerList().sendMessage(notice);
        }
        dropMasks();
        reset();
    }

    private void depart(WorldServer nexus) {
        if (order != null) {
            say(nexus, true, "Running away. Noted.");
        }
        if (chaos != null) {
            say(nexus, false, "Run, run! We'll see you again. Everyone sees us again.");
        }
        phase = Phase.DEPART;
        dropMasks();
        for (EntityOrderChaos head : heads()) {
            head.withdraw();
        }
    }

    /** Takes them away at once. For operators, and for when the world closes. */
    public void vanish() {
        for (EntityOrderChaos head : heads()) {
            head.setDead();
        }
        dropMasks();
        reset();
    }

    // ------------------------------------------------------------------ the Astral Regulator

    /** A Regulator this player wields, in either hand or in the chest of the armour they pilot. */
    private static ItemStack regulatorOf(EntityPlayer player) {
        return ItemAstralRegulator.raised(player, EnumHand.MAIN_HAND);
    }

    /** Whether this player is carrying a Regulator where they can see it. */
    public static boolean carriesRegulator(EntityPlayer player) {
        return !regulatorOf(player).isEmpty();
    }

    /**
     * They know what an Astral Regulator is, and what it can do to them once they are beaten. The
     * first time each of them sees one in someone's hand, they say so, and what they say depends on
     * what is already inside it.
     */
    private void watchRegulators(WorldServer nexus, List<EntityPlayerMP> players) {
        for (EntityPlayerMP player : players) {
            ItemStack stack = regulatorOf(player);
            if (stack.isEmpty() || !sawRegulator.add(player.getUniqueID())) {
                continue;
            }
            List<String> inside = ItemAstralRegulator.absorbed(stack);
            boolean orderHere = order != null, chaosHere = chaos != null;
            if (inside.contains(ItemAstralRegulator.ORDER) && chaosHere) {
                say(nexus, false, "Brother? Brother, is that you in there? Hang on. I'm coming to get you out.");
            } else if (inside.contains(ItemAstralRegulator.CHAOS) && orderHere) {
                say(nexus, true, "My brother is in that crystal. I can feel him. You will give him back.");
            } else if (inside.contains(ItemAstralRegulator.INBETWEENER)) {
                if (chaosHere) {
                    say(nexus, false, "Is that our boy in there? Oh, he must HATE that.");
                }
                if (orderHere) {
                    say(nexus, true, "So that is where he went.");
                }
            } else if (inside.contains(ItemAstralRegulator.TRIBUNAL) && orderHere) {
                say(nexus, true, "The Living Tribunal, in a crystal. Who let you anywhere near him?");
            } else if (ItemAstralRegulator.isCharged(stack)) {
                if (orderHere) {
                    say(nexus, true, "That crystal hums with Eternity. You should not be holding it.");
                }
                if (chaosHere) {
                    say(nexus, false, "Ooh, is that the big fella in there? Cosy.");
                }
            } else if (orderHere) {
                say(nexus, true, "An empty Regulator. Put it away before you hurt yourself.");
            } else {
                say(nexus, false, "An empty one! Adorable.");
            }
        }
    }

    /**
     * Someone raised a Regulator at one of them while it was still standing. It cannot take them
     * like that, and they know it, but nobody likes having one pointed at them: the head it was
     * pointed at flinches back and says something about it.
     */
    public boolean regulatorRaised(EntityPlayerMP player) {
        if (phase != Phase.FIGHT) {
            return false;
        }
        for (EntityOrderChaos head : heads()) {
            if (head.isDown() || head.isWithdrawing() || head.world != player.world) {
                continue;
            }
            Vec3d toward = head.centre().subtract(player.getPositionEyes(1F));
            if (toward.length() < 48D && player.getLook(1F).dotProduct(toward.normalize()) > 0.8D) {
                return head.flinch(player);
            }
        }
        return false;
    }

    /** One of them speaks to everyone in the Nexus. */
    static void say(WorldServer nexus, boolean isOrder, String line) {
        TextComponentString name = new TextComponentString(isOrder ? "<Master Order> " : "<Lord Chaos> ");
        name.getStyle().setColor(isOrder ? TextFormatting.GOLD : TextFormatting.AQUA).setBold(true);
        TextComponentString text = new TextComponentString(line);
        text.getStyle().setColor(isOrder ? TextFormatting.YELLOW : TextFormatting.AQUA).setBold(false);
        name.appendSibling(text);
        for (EntityPlayer player : nexus.playerEntities) {
            player.sendMessage(name);
        }
    }

    /** One of them speaks, for the heads themselves to call. */
    public static void speak(boolean isOrder, String line) {
        WorldServer nexus = DimensionManager.getWorld(Nexus.dimension());
        if (nexus != null) {
            say(nexus, isOrder, line);
        }
    }
}
