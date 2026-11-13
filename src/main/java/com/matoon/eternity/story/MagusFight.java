package com.matoon.eternity.story;

import com.matoon.eternity.EternityConfig;
import com.matoon.eternity.EternitySounds;
import com.matoon.eternity.compat.InfinityStones;
import com.matoon.eternity.compat.Relics;
import com.matoon.eternity.entity.EntityAdamWarlock;
import com.matoon.eternity.entity.EntityMagus;
import com.matoon.eternity.entity.EntityMagus.Pose;
import com.matoon.eternity.entity.EntityOdin;
import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.network.MsgGauntletFight;
import com.matoon.eternity.network.MsgTribunalFx;
import com.matoon.eternity.world.SovereignData;
import lucraft.mods.lucraftcore.infinity.EnumInfinityStone;
import lucraft.mods.lucraftcore.infinity.ModuleInfinity;
import lucraft.mods.lucraftcore.infinity.items.InventoryInfinityGauntlet;
import lucraft.mods.lucraftcore.infinity.items.ItemInfinityGauntlet;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.matoon.eternity.story.Speakers.MAGUS;
import static com.matoon.eternity.story.Speakers.WARLOCK;

/**
 * The Magus's last fight: the struggle for the gauntlet.
 * <p>
 * He holds Power and Mind; they hold the rest, in a gauntlet. Whoever has it is the one the
 * other goes for. He, with it: flies off with it held to his chest, comes down somewhere
 * quiet, sets his own two stones in it one by one, puts it on, and lets it come up through
 * him for twenty seconds; then he snaps, and he has won: he goes, with all six. They, close enough to reach
 * it: a struggle, hand to hand, for it; whoever loses it is thrown a long way. Warlock goes
 * at him whenever he can reach him, and never gets it, and every time he catches him it costs
 * the Magus his landing. With all six in it, on their hand, their snap is only for him: he is
 * dust, and it ends.
 * <p>
 * It begins at the end of the Grandmaster's game if they have it on them, all their stones in
 * it; or the moment he sees it raised during the game, when he takes it out of their hand; or,
 * if neither, he goes, and comes for them later, out of nowhere, once they have it ready.
 */
final class MagusFight {

    /** What he is doing. */
    enum Act {
        /** After whoever has it, fighting. */
        HUNT,
        /** Away with it, to somewhere quiet. */
        FLEE,
        /** Coming down there. */
        LAND,
        /** Setting his stones in it. */
        SLOT,
        /** Putting it on. */
        DON,
        /** Letting it come up through him. */
        POWER,
        /** The fingers. */
        SNAP,
        /** Thrown back, out of a struggle he lost. */
        FLUNG,
        /** Hand to hand with someone for it. */
        STRUGGLE,
        /** With Warlock, a few seconds of it. */
        CLASH,
        /** His health run out: reeling, the gauntlet out of his hands if he had it, and coming back. */
        REEL,
        /** Warlock, beaten by him, dying: the fight stops for it. */
        ELEGY,
        /** Odin coming down the Bifrost into it: the fight stops for him. */
        ODIN,
        /** He has snapped, and won: what it did to him, the question he doesn't answer, and him gone. */
        AFTERMATH,
        /** Odin has beaten them both, and takes it, and goes. */
        TAKEN,
        /** It is over, one way or the other. */
        OVER
    }

    /** The gauntlet that is fought over: marked, so it can be found wherever it is put. */
    static final String MARK = "EternityFightGauntlet";
    private static final double REACH = 3.2D;
    private static final int STRUGGLE_TICKS = 90, STRUGGLE_COOLDOWN = 160, CLASH_COOLDOWN = 420;
    private static final int WARLOCK_LATE = 300;
    /** The camera on Warlock's death (StoryClient.ELEGY). */
    private static final int ELEGY_SCENE = 4;
    /** The camera after his snap (StoryClient.SNAPPED). */
    private static final int SNAPPED_SCENE = 5;
    /** The cameras on Odin's coming and on his taking it (StoryClient.ODIN, ODIN_WIN). */
    private static final int ODIN_SCENE = 6, ODIN_WIN_SCENE = 7;

    private final StoryDirector story;
    private final Random rand = new Random();
    @Nullable
    private EntityMagus magus;
    @Nullable
    private EntityAdamWarlock warlock;
    private Act act = Act.OVER;
    private int actTicks;
    @Nullable
    private Vec3d landAt;
    private Vec3d from = Vec3d.ZERO, to = Vec3d.ZERO;
    /** The stones he has still to set, in turn, by ordinal. */
    private int slotting = -1;
    private long warlockDue = -1;
    private int struggleCooldown, clashCooldown, boltIn = 40;
    /**
     * While they have it he fights them, and only now and then comes in for it: after so many attacks since he last
     * did (counted from attacksAtGrab), or as soon as a heavy blow of his has opened them up. How long he has been
     * coming, if he is.
     */
    private int attacksAtGrab, grabAfter = 3, grabbing = -1;
    private boolean open;
    /** Warlock has streaked in and stopped; until then he is only arriving. */
    private boolean warlockIn;
    // A struggle under way: how it stands (0 lost, 1 theirs), who held it going in, and where everyone is.
    private float bar;
    private boolean heHadIt;
    private Vec3d holdPlayer = Vec3d.ZERO;
    private int presses, lastPress;
    @Nullable
    private Timeline clash;
    /** What a clash took him away from, and how far into it he was: he takes it up again after. */
    private Act resumeAct = Act.FLEE;
    private int resumeTicks;
    private Vec3d resumeAt = Vec3d.ZERO;
    /** What is being staged (Warlock falling, held, let fall), ticked every tick until it is done. */
    private final List<Timeline> playing = new ArrayList<>();
    /** Odin, once he has come into it. */
    @Nullable
    private EntityOdin odin;
    /** The second time the Magus's health has run out: once he is whole again, Odin comes. */
    private boolean odinNext;
    /** What plays out once it is over (Odin going after it is won, or after it is lost and started over). */
    private final List<Timeline> exits = new ArrayList<>();
    /** The one put up for looking at, if any. */
    @Nullable
    private EntityMagus looked;
    /** When the one whose fight it was went out of the world with nobody left to take it up (-1 if not). */
    private long goneSince = -1L;
    /** Those felled with Odin in it and spared, and until when they are down: out of it, while the rest fight on. */
    private final java.util.Map<java.util.UUID, Long> felled = new java.util.HashMap<>();

    MagusFight(StoryDirector story) {
        this.story = story;
    }

    void reset() {
        playing.clear();
        magus = null;
        warlock = null;
        act = Act.OVER;
        clash = null;
        odin = null;
        odinNext = false;
        exits.clear();
        goneSince = -1L;
        felled.clear();
    }

    private static SovereignData data() {
        return SovereignData.get();
    }

    // ------------------------------------------------------------------ the gauntlet

    /** Whether something is a gauntlet. */
    private static boolean isGauntlet(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ItemInfinityGauntlet;
    }

    /** Whether the last fight is on, or what follows it (his dust, the elegy, whatever is said) is still playing out. */
    boolean underway() {
        SovereignData.Fight f = data().fight;
        return f == SovereignData.Fight.FIGHTING || act != Act.OVER;
    }

    /**
     * Ready: a gauntlet on them (anywhere in their inventory) with every stone they hold set in
     * it, and that is every stone but his two. The index of its slot (offhand: -2), or -1 if not.
     */
    static int ready(EntityPlayer player) {
        int held = InfinityStones.scan(player);
        if (InfinityStones.count(held) < 1
                || (held | InfinityStones.bit(EnumInfinityStone.POWER) | InfinityStones.bit(EnumInfinityStone.MIND)) != InfinityStones.ALL) {
            return -1;
        }
        for (int i = 0; i < player.inventory.mainInventory.size(); i++) {
            ItemStack s = player.inventory.mainInventory.get(i);
            if (isGauntlet(s) && InfinityStones.maskOf(s) == held) {
                return i;
            }
        }
        ItemStack off = player.inventory.offHandInventory.get(0);
        return isGauntlet(off) && InfinityStones.maskOf(off) == held ? -2 : -1;
    }

    /** Whether they have it ready and in their hand, raised. */
    static boolean raised(EntityPlayer player) {
        int i = ready(player);
        return i >= 0 && i == player.inventory.currentItem;
    }

    /** The marked gauntlet's slot on them, or -1 (offhand: -2). */
    private static int marked(EntityPlayer player) {
        for (int i = 0; i < player.inventory.mainInventory.size(); i++) {
            ItemStack s = player.inventory.mainInventory.get(i);
            if (isGauntlet(s) && s.hasTagCompound() && s.getTagCompound().getBoolean(MARK)) {
                return i;
            }
        }
        ItemStack off = player.inventory.offHandInventory.get(0);
        return isGauntlet(off) && off.hasTagCompound() && off.getTagCompound().getBoolean(MARK) ? -2 : -1;
    }

    private static ItemStack at(EntityPlayer player, int slot) {
        return slot == -2 ? player.inventory.offHandInventory.get(0) : slot >= 0 ? player.inventory.mainInventory.get(slot) : ItemStack.EMPTY;
    }

    /** Out of their inventory, whole. */
    private static ItemStack takeFrom(EntityPlayerMP player, int slot) {
        ItemStack s = at(player, slot).copy();
        if (slot == -2) {
            player.inventory.offHandInventory.set(0, ItemStack.EMPTY);
        } else if (slot >= 0) {
            player.inventory.mainInventory.set(slot, ItemStack.EMPTY);
        }
        player.inventoryContainer.detectAndSendChanges();
        return s;
    }

    private static void mark(ItemStack gauntlet) {
        if (!gauntlet.hasTagCompound()) {
            gauntlet.setTagCompound(new NBTTagCompound());
        }
        gauntlet.getTagCompound().setBoolean(MARK, true);
    }

    private static boolean isMarked(ItemStack stack) {
        return isGauntlet(stack) && stack.hasTagCompound() && stack.getTagCompound().getBoolean(MARK);
    }

    private static void unmark(ItemStack stack) {
        if (stack.hasTagCompound()) {
            stack.getTagCompound().removeTag(MARK);
        }
    }

    /** This one, of all the gauntlets on them, is the one fought over: any marked from before are not. */
    private static void markOnly(EntityPlayer player, int slot) {
        for (ItemStack s : player.inventory.mainInventory) {
            unmark(s);
        }
        unmark(player.inventory.offHandInventory.get(0));
        unmark(player.inventory.getItemStack());
        mark(at(player, slot));
    }

    /** Whether he has it. */
    private boolean hisNow() {
        return !data().fightGauntlet.isEmpty();
    }

    /** Whether all six are in it. */
    private static boolean whole(ItemStack gauntlet) {
        return InfinityStones.count(InfinityStones.maskOf(gauntlet)) >= 6;
    }

    /** His two stones that are not in it yet, in the order he sets them: Power, then Mind. */
    private static int nextOfHis(ItemStack gauntlet) {
        int mask = InfinityStones.maskOf(gauntlet);
        if ((mask & InfinityStones.bit(EnumInfinityStone.POWER)) == 0) {
            return EnumInfinityStone.POWER.ordinal();
        }
        if ((mask & InfinityStones.bit(EnumInfinityStone.MIND)) == 0) {
            return EnumInfinityStone.MIND.ordinal();
        }
        return -1;
    }

    /** One of his stones, made real and set in its socket, the one true stone of its kind, still his. */
    private static void setInto(ItemStack gauntlet, int ordinal) {
        ItemStack stone = InfinityStones.stack(ordinal);
        Relics.Kind kind = Relics.kind(stone);
        if (stone.isEmpty() || kind == null) {
            return;
        }
        Relics.release(kind);
        Relics.claim(stone, Sovereign.MAGUS_ID, "The Magus");
        InventoryInfinityGauntlet sockets = new InventoryInfinityGauntlet(gauntlet);
        for (int i = 0; i < sockets.getSizeInventory(); i++) {
            if (InventoryInfinityGauntlet.SLOTS[i].ordinal() == ordinal) {
                sockets.setInventorySlotContents(i, stone);
            }
        }
        sockets.markDirty();
    }

    /** His stones out of it again (they are his; the claims stay his), leaving theirs. */
    private static void takeOutHis(ItemStack gauntlet) {
        if (!gauntlet.hasTagCompound()) {
            return;
        }
        InventoryInfinityGauntlet sockets = new InventoryInfinityGauntlet(gauntlet);
        for (int i = 0; i < sockets.getSizeInventory(); i++) {
            EnumInfinityStone type = InventoryInfinityGauntlet.SLOTS[i];
            if (type == EnumInfinityStone.POWER || type == EnumInfinityStone.MIND) {
                sockets.setInventorySlotContents(i, ItemStack.EMPTY);
            }
        }
        sockets.markDirty();
    }

    /** All of it theirs now: his two stones in it claimed for them. */
    private static void claimAll(ItemStack gauntlet, EntityPlayerMP player) {
        InventoryInfinityGauntlet sockets = new InventoryInfinityGauntlet(gauntlet);
        for (int i = 0; i < sockets.getSizeInventory(); i++) {
            EnumInfinityStone type = InventoryInfinityGauntlet.SLOTS[i];
            ItemStack s = sockets.getStackInSlot(i);
            if (!s.isEmpty() && (type == EnumInfinityStone.POWER || type == EnumInfinityStone.MIND)) {
                Relics.Kind kind = Relics.kind(s);
                if (kind != null) {
                    Relics.release(kind);
                    Relics.claim(s, player);
                    sockets.setInventorySlotContents(i, s);
                }
            }
        }
        sockets.markDirty();
    }

    // ------------------------------------------------------------------ how it begins

    /** Whether the fight is on. */
    boolean on() {
        return data().fight == SovereignData.Fight.FIGHTING;
    }

    /**
     * The end of the Grandmaster's game. Ready, it begins here, with whoever is already on the
     * board (the Grandmaster's director lets go of them): true if it did. Not ready, he goes,
     * and will come for them later.
     */
    boolean afterGame(EntityPlayerMP player, @Nullable EntityMagus m, @Nullable EntityAdamWarlock w) {
        SovereignData d = data();
        if (d.fight != SovereignData.Fight.NONE && d.fight != SovereignData.Fight.HUNTING) {
            return false;
        }
        int slot = ready(player);
        if (slot == -1) {
            // Not them, perhaps; but whoever else is at the board with it.
            for (EntityPlayerMP o : standing(player.getUniqueID(), m != null ? m : player, 96D)) {
                int s = ready(o);
                if (s != -1) {
                    player = o;
                    slot = s;
                    break;
                }
            }
        }
        if (slot == -1) {
            d.fight = SovereignData.Fight.HUNTING;
            d.fightPlayer = player.getUniqueID();
            d.nextAmbush = StoryDirector.now() + 200L;
            d.markDirty();
            story.sendState(player);
            return false;
        }
        markOnly(player, slot);
        begin(player, m, w, false);
        if (m != null) {
            StoryDirector.shout(m, MAGUS, m.getEntityId(), "You brought it with you. Good.", 128D);
        }
        return true;
    }

    /** Raised during the game: he comes down and takes it out of their hand. True if he did. */
    boolean snatch(EntityPlayerMP player, EntityMagus m, @Nullable EntityAdamWarlock w) {
        SovereignData d = data();
        if (d.fight == SovereignData.Fight.FIGHTING || d.fight == SovereignData.Fight.WON || d.fight == SovereignData.Fight.LOST
                || !raised(player)) {
            return false;
        }
        markOnly(player, player.inventory.currentItem);
        ItemStack g = takeFrom(player, player.inventory.currentItem);
        d.fightGauntlet = g;
        Vec3d front = player.getPositionEyes(1F).add(flat(player.getLookVec()).scale(1.4D)).subtract(0D, 1.6D, 0D);
        m.blink(front, player.getPositionEyes(1F), false);
        m.holdGauntlet(g, EntityMagus.HOLD_CRADLE);
        sound(player.getPositionVector(), SoundEvents.ENTITY_ITEM_PICKUP, 1.5F, 0.5F);
        begin(player, m, w, true);
        act(Act.FLEE);
        StoryDirector.shout(m, MAGUS, m.getEntityId(), "Thank you.", 128D);
        return true;
    }

    /** Out of nowhere: once they have it ready, he is behind them. */
    private void ambush(EntityPlayerMP player) {
        int slot = ready(player);
        if (slot == -1) {
            return;
        }
        markOnly(player, slot);
        Vec3d behind = player.getPositionVector().subtract(flat(player.getLookVec()).scale(4D)).add(0D, 0.5D, 0D);
        EntityMagus m = new EntityMagus(player.world);
        m.setPosition(behind.x, behind.y, behind.z);
        player.world.spawnEntity(m);
        m.blink(behind, player.getPositionEyes(1F), true);
        begin(player, m, null, false);
        warlockDue = StoryDirector.now() + WARLOCK_LATE;
        StoryDirector.shout(m, MAGUS, m.getEntityId(), "You put it together for me. Thank you.", 128D);
    }

    private void begin(EntityPlayerMP player, @Nullable EntityMagus m, @Nullable EntityAdamWarlock w, boolean his) {
        if (looked != null) {
            looked.setDead();
            looked = null;
        }
        SovereignData d = data();
        d.fight = SovereignData.Fight.FIGHTING;
        d.fightPlayer = player.getUniqueID();
        if (!his) {
            d.fightGauntlet = ItemStack.EMPTY;
        }
        d.markDirty();
        magus = m;
        warlock = w;
        goneSince = -1L;
        felled.clear();
        warlockDue = w != null || d.warlockFell ? -1 : StoryDirector.now() + 40L;
        if (w != null) {
            w.mortal(EternityConfig.magus.warlockCanDie);
        }
        if (m != null) {
            m.stand();
            m.crown(true);
            m.boss(true);
        }
        struggleCooldown = 40;
        clashCooldown = 120;
        grabbing = -1;
        open = false;
        grabAfter = 2 + rand.nextInt(2);
        attacksAtGrab = m != null ? m.attacksDone() : 0;
        warlockIn = w != null;
        act(his ? Act.FLEE : Act.HUNT);
        story.sendState(player);
    }

    // ------------------------------------------------------------------ ticking

    void tick(MinecraftServer server) {
        if (act == Act.AFTERMATH || act == Act.TAKEN) {
            // It is over, one way or the other; what follows is still being played out.
            for (Timeline t : new ArrayList<>(playing)) {
                if (!t.tick()) {
                    playing.remove(t);
                }
            }
            EntityPlayerMP p = StoryDirector.player(data().fightPlayer);
            if (playing.isEmpty() && (p == null || !story.talking(p))) {
                act = Act.OVER;
            }
            return;
        }
        SovereignData d = data();
        if (d.fight == SovereignData.Fight.HUNTING) {
            if (StoryDirector.now() % 20 != 0) {
                return;
            }
            // What he took, back to whoever it was taken from, once they are about again.
            EntityPlayerMP owed = StoryDirector.player(owner(d));
            if (!d.giveBack.isEmpty() && owed != null && owed.isEntityAlive()) {
                giveBack(owed);
            }
            if (StoryDirector.now() >= d.nextAmbush) {
                // Whoever has it ready first, those he was after first.
                for (EntityPlayerMP o : standing(d.fightPlayer, null, 0D)) {
                    if (o.dimension == 0 && !story.talking(o) && ready(o) != -1) {
                        ambush(o);
                        break;
                    }
                }
            }
            return;
        }
        if (d.fight != SovereignData.Fight.FIGHTING) {
            return;
        }
        EntityPlayerMP p = StoryDirector.player(d.fightPlayer);
        // It is whoever has the gauntlet he is after: picked up by someone else, it is their fight now.
        if (act != Act.STRUGGLE && act != Act.ELEGY && !hisNow()) {
            EntityPlayerMP holder = holder(server, p);
            if (holder != null && holder != p) {
                d.fightPlayer = holder.getUniqueID();
                d.markDirty();
                p = holder;
                grabbing = -1;
                if (magus != null && magus.isFighting()) {
                    magus.fight(p, warlock);
                }
                story.sendState(p);
            }
        }
        for (Timeline t : new ArrayList<>(playing)) {
            if (!t.tick()) {
                playing.remove(t);
            }
        }
        if (p == null) {
            // Gone out of the world: whoever else is in it takes it up, those who were with them first.
            EntityPlayerMP next = successor(d.fightPlayer);
            if (next != null) {
                takeOver(next);
                p = next;
            }
        }
        if (p == null || !p.isEntityAlive()) {
            // Away, or fallen: he waits where he is; or, if they were not there to see it, he goes until they are back.
            if (p == null && magus != null) {
                magus.setDead();
                magus = null;
            }
            if (p == null && odin != null) {
                odin.setDead();
                odin = null;
            }
            if (p == null) {
                // Nobody back for it in half a minute: it is as if they had lost, and he waits for them to be ready again.
                if (goneSince < 0L) {
                    goneSince = StoryDirector.now();
                } else if (StoryDirector.now() - goneSince > 20L * 30L) {
                    goneSince = -1L;
                    lost(null);
                }
            }
            return;
        }
        goneSince = -1L;
        EntityMagus m = magus;
        if (m == null || m.isDead || m.world != p.world) {
            if (m != null && !m.isDead) {
                // Not left behind in the world they went from.
                m.setDead();
            }
            m = magus = spawnNear(p);
        }
        if (warlock != null && (warlock.isDead || warlock.world != p.world)) {
            if (!warlock.isDead) {
                warlock.dismiss();
            }
            warlock = null;
            warlockDue = d.warlockFell ? -1L : StoryDirector.now() + 60L;
        }
        if (warlock == null && warlockDue >= 0 && StoryDirector.now() >= warlockDue) {
            warlockArrives(p, m);
        }
        if (warlock != null && !warlockIn && warlock.getState() != EntityAdamWarlock.State.ARRIVE && !warlock.fallen()) {
            // Down, and at him.
            warlockIn = true;
            warlock.turnOn(m);
        }
        if (warlock != null && warlockIn && clash == null && !warlock.fallen() && StoryDirector.now() % 20 == 0 && warlock.getDistance(m) > 40D) {
            // Left behind: he comes after him again, across the sky, and never loses him for anyone else.
            warlock.script(true);
            warlock.arriveAt(m.getPositionVector().add(0D, 2.5D, 0D).add(flat(warlock.getPositionVector().subtract(m.getPositionVector())).scale(6D)),
                    m.getPositionEyes(1F));
            warlockIn = false;
        }
        // Odin, if he has come: lost track of them, he comes down again; and every second he is told who is in it.
        if (odin != null && (odin.isDead || odin.world != p.world)) {
            if (!odin.isDead) {
                odin.setDead();
            }
            odin = null;
        }
        if (odin == null && d.odinCame && act != Act.ODIN && act != Act.ELEGY && EternityConfig.odin.enabled) {
            odin = odinReturns(p, m);
        }
        if (odin != null && act != Act.ODIN) {
            if (StoryDirector.now() % 20 == 0) {
                odin.fight(p, m, warlock);
                m.rival(odin);
            }
            odin.hold(act == Act.STRUGGLE || act == Act.ELEGY);
        }
        if (act == Act.ELEGY || act == Act.ODIN) {
            return;
        }
        if (warlock != null && warlock.fallen() && clash == null && act != Act.STRUGGLE) {
            elegy(p, m, warlock);
            return;
        }
        actTicks++;
        if (struggleCooldown > 0) {
            struggleCooldown--;
        }
        if (clashCooldown > 0) {
            clashCooldown--;
        }
        if (clash != null) {
            if (!clash.tick()) {
                clash = null;
                afterClash(p, m);
            }
            return;
        }
        if (act == Act.REEL) {
            reeling(p, m);
            return;
        }
        if (act != Act.STRUGGLE && m.takeBroken()) {
            reel(p, m);
            return;
        }
        if (act == Act.STRUGGLE) {
            struggling(p, m);
            return;
        }
        if (act == Act.FLUNG) {
            flung(p, m);
            return;
        }
        if (actTicks % 10 == 0) {
            sweep(p);
        }
        ItemStack g = d.fightGauntlet;
        if (g.isEmpty() && act != Act.HUNT) {
            // Whatever he was doing with it, he hasn't got it: after it again.
            act(Act.HUNT);
        }
        if (!g.isEmpty()) {
            m.holdGauntlet(g, m.hold() == EntityMagus.HOLD_NONE ? EntityMagus.HOLD_CRADLE : m.hold());
        } else if (m.hold() != EntityMagus.HOLD_NONE) {
            m.holdGauntlet(ItemStack.EMPTY, EntityMagus.HOLD_NONE);
        }
        if (act != Act.SNAP || actTicks < EntityMagus.SNAP_AT) {
            if (reached(p, m)) {
                return;
            }
        }
        switch (act) {
            case HUNT:
                hunting(p, m);
                break;
            case FLEE:
                fleeing(p, m);
                break;
            case LAND:
                landing(p, m);
                break;
            case SLOT:
                slotIn(p, m);
                break;
            case DON:
                donning(p, m);
                break;
            case POWER:
                powering(m);
                if (actTicks >= EntityMagus.POWER_TICKS) {
                    act(Act.SNAP);
                    m.rest(Pose.SNAP).perform(Pose.SNAP);
                    m.faceNow(p.getPositionEyes(1F));
                }
                break;
            case SNAP:
                if (actTicks == EntityMagus.SNAP_AT) {
                    snapped(p, m);
                }
                break;
            default:
                break;
        }
    }

    private void act(Act next) {
        com.matoon.eternity.Eternity.LOG.debug("[MagusFight] {} -> {} (warlock {}, clash cd {})", act, next, warlock != null ? String.format("%.1f", magus != null ? warlock.getDistance(magus) : -1F) : "none", clashCooldown);
        act = next;
        actTicks = 0;
    }

    /** Within arm's length of him: near enough across, and not far above or below. */
    private static boolean close(EntityPlayerMP p, EntityMagus m) {
        double dx = p.posX - m.posX, dz = p.posZ - m.posZ;
        return dx * dx + dz * dz < REACH * REACH && Math.abs(p.posY + 1D - (m.posY + 1D)) < 3D;
    }

    /**
     * Someone close enough to reach for it: a struggle, or, Warlock, a clash. True if one began. With him it is theirs
     * to start, by getting to him; with them it is his, and only when he has come for it (a beat after he is there).
     */
    private boolean reached(EntityPlayerMP p, EntityMagus m) {
        boolean his = hisNow();
        int slot = marked(p);
        boolean reaching = his || slot != -1 && grabbing >= 6;
        if (struggleCooldown <= 0 && !m.isAway() && close(p, m) && reaching && !p.isSpectator()) {
            startStruggle(p, m, his);
            return true;
        }
        if (his && struggleCooldown <= 0 && !m.isAway()) {
            // With him it is anyone's to start: whoever gets to him first, it is their struggle.
            for (EntityPlayerMP o : standing(p.getUniqueID(), m, 8D)) {
                if (o != p && close(o, m)) {
                    takeOver(o);
                    startStruggle(o, m, true);
                    return true;
                }
            }
        }
        EntityAdamWarlock w = warlock;
        if (his && w != null && clashCooldown <= 0 && !m.isAway() && w.getDistance(m) < 7D) {
            startClash(p, m, w);
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ he hasn't got it

    /** After whoever has it: his fight, the one he gave them at the Grandmaster's table. */
    private void hunting(EntityPlayerMP p, EntityMagus m) {
        int slot = marked(p);
        if (slot == -1) {
            // Not on them: dropped, then. He goes and picks it up.
            Entity lying = lyingAbout(p);
            if (lying != null) {
                fetch(m, lying);
                return;
            }
        }
        if (grabbing >= 0) {
            // Not across the space between: in front of them, his hand already out for it; and after them if they back off.
            grabbing++;
            Vec3d toward = flat(m.getPositionVector().subtract(p.getPositionVector()));
            if (grabbing == 1) {
                m.blink(p.getPositionVector().add(toward.scale(2.2D)).add(0D, 0.1D, 0D), p.getPositionEyes(1F), false);
                if (rand.nextInt(3) == 0) {
                    StoryDirector.shout(m, MAGUS, m.getEntityId(), GRABS[rand.nextInt(GRABS.length)], 64D);
                }
            } else {
                m.hoverTo(p.getPositionVector().add(toward.scale(1.6D)).add(0D, 0.1D, 0D), 0.4D);
            }
            m.look(p.getPositionEyes(1F));
            if (grabbing > 30 || slot == -1) {
                // They got clear of him: back to hurting them first.
                endGrab(p, m);
            }
            return;
        }
        if (actTicks == 1 || !m.isFighting()) {
            m.fight(p, warlock);
        }
        open |= m.opening();
        int since = m.attacksDone() - attacksAtGrab;
        if (slot != -1 && struggleCooldown <= 0 && !m.isAttacking() && (since >= grabAfter || open && since >= 1)) {
            grabbing = 0;
            m.stand();
            m.rest(Pose.GUARD).perform(Pose.GUARD);
        }
    }

    private static final String[] GRABS = {"Mine.", "Give it here.", "You can barely stand. Let go.", "That was never yours."};

    /** Done coming for it, one way or another: he fights them a while before he tries again. */
    private void endGrab(EntityPlayerMP p, EntityMagus m) {
        grabbing = -1;
        open = false;
        grabAfter = 2 + rand.nextInt(3);
        m.fight(p, warlock);
        attacksAtGrab = m.attacksDone();
    }

    /** Down to it where it lies, out of the fight; and his. A long way off, he folds across to it. */
    private void fetch(EntityMagus m, Entity lying) {
        if (m.isFighting()) {
            m.stand();
        }
        grabbing = -1;
        Vec3d it = lying.getPositionVector();
        if (m.getDistance(lying) > 14D && !m.isAway() && actTicks % 20 == 0) {
            m.blink(it.add(flat(m.getPositionVector().subtract(it)).scale(1.5D)).add(0D, 0.3D, 0D), it, false);
        }
        m.hoverTo(it.add(0D, 0.4D, 0D), 0.25D);
        m.look(it);
        if (m.getDistance(lying) < 2.2D) {
            ItemStack g = itemOf(lying).copy();
            lying.setDead();
            data().fightGauntlet = g;
            data().markDirty();
            m.holdGauntlet(g, EntityMagus.HOLD_CRADLE);
            sound(m.getPositionVector(), SoundEvents.ENTITY_ITEM_PICKUP, 1.5F, 0.5F);
            StoryDirector.shout(m, MAGUS, m.getEntityId(), "Careless.", 128D);
            act(Act.FLEE);
            landAt = null;
        }
    }

    /**
     * What a thing lying on the ground is. A dropped gauntlet is no item entity: Lucraft swaps it for its own
     * indestructible one, which is picked up by hand.
     */
    private static ItemStack itemOf(Entity e) {
        if (e instanceof EntityItem) {
            return ((EntityItem) e).getItem();
        }
        if (e instanceof lucraft.mods.lucraftcore.infinity.EntityItemIndestructible) {
            return ((lucraft.mods.lucraftcore.infinity.EntityItemIndestructible) e).getItem();
        }
        return ItemStack.EMPTY;
    }

    /** The gauntlet, lying on the ground somewhere about them. */
    @Nullable
    private Entity lyingAbout(EntityPlayerMP p) {
        for (Entity e : p.world.loadedEntityList) {
            if (!e.isDead && e.getDistance(p) < 96D && isMarked(itemOf(e))) {
                return e;
            }
        }
        return null;
    }

    /**
     * There is only ever the one. His, if he has it; else the first on them; else one lying about: any other marked
     * gauntlet is a copy, and goes.
     */
    private void sweep(EntityPlayerMP p) {
        boolean found = hisNow();
        boolean changed = false;
        for (int i = 0; i < p.inventory.mainInventory.size(); i++) {
            if (isMarked(p.inventory.mainInventory.get(i))) {
                if (found) {
                    p.inventory.mainInventory.set(i, ItemStack.EMPTY);
                    changed = true;
                }
                found = true;
            }
        }
        if (isMarked(p.inventory.offHandInventory.get(0))) {
            if (found) {
                p.inventory.offHandInventory.set(0, ItemStack.EMPTY);
                changed = true;
            }
            found = true;
        }
        if (isMarked(p.inventory.getItemStack())) {
            if (found) {
                p.inventory.setItemStack(ItemStack.EMPTY);
                p.updateHeldItem();
                changed = true;
            }
            found = true;
        }
        if (changed) {
            p.inventoryContainer.detectAndSendChanges();
        }
        for (Entity e : p.world.loadedEntityList) {
            if (!e.isDead && isMarked(itemOf(e))) {
                if (found) {
                    e.setDead();
                }
                found = true;
            }
        }
    }

    // ------------------------------------------------------------------ he has it

    /** Off with it, held to him, to somewhere quiet; firing at whoever chases him. */
    private void fleeing(EntityPlayerMP p, EntityMagus m) {
        if (actTicks == 1 || landAt == null) {
            m.stand();
            landAt = landingSpot(p, m);
            boolean worn = m.hold() == EntityMagus.HOLD_WORN;
            m.rest(worn ? Pose.FLOAT : Pose.CRADLE).perform(worn ? Pose.FLOAT : Pose.CRADLE);
            boltIn = 30;
        }
        Vec3d at = m.getPositionVector();
        Vec3d goal = landAt.add(0D, 6D, 0D);
        Vec3d flat = new Vec3d(goal.x - at.x, 0D, goal.z - at.z);
        double far = flat.length();
        // High, then down onto it when he is over it.
        Vec3d aim = far < 3D ? goal : new Vec3d(goal.x, Math.max(goal.y, at.y), goal.z);
        Vec3d step = aim.subtract(at);
        double len = step.length();
        if (len > 0.55D) {
            step = step.scale(0.55D / len);
        }
        m.place(at.add(step), at.add(step.scale(10D)).add(0D, 1.5D, 0D));
        if (--boltIn <= 0) {
            boltIn = 40 + rand.nextInt(25);
            EntityLivingBase chaser = chaser(p, m);
            if (chaser != null && chaser.getDistance(m) < 28D) {
                m.fire(chaser);
            }
        }
        if (far < 1.2D && Math.abs(goal.y - at.y) < 1.2D) {
            act(Act.LAND);
            from = m.getPositionVector();
        }
    }

    /** Whoever is nearest him and coming: them, or Warlock. */
    @Nullable
    private EntityLivingBase chaser(EntityPlayerMP p, EntityMagus m) {
        EntityLivingBase near = p;
        for (EntityPlayerMP o : standing(null, m, 28D)) {
            if (o.getDistance(m) < near.getDistance(m)) {
                near = o;
            }
        }
        EntityAdamWarlock w = warlock;
        if (w != null && !w.isDead && w.getDistance(m) < near.getDistance(m)) {
            return w;
        }
        return near;
    }

    /** Somewhere 30 to 50 blocks off, away from them, on open ground. */
    private Vec3d landingSpot(EntityPlayerMP p, EntityMagus m) {
        Vec3d away = flat(m.getPositionVector().subtract(p.getPositionVector()));
        if (away.lengthSquared() < 1.0E-4D) {
            away = new Vec3d(1D, 0D, 0D);
        }
        double turn = (rand.nextDouble() - 0.5D) * 1.6D;
        Vec3d dir = new Vec3d(away.x * Math.cos(turn) - away.z * Math.sin(turn), 0D, away.x * Math.sin(turn) + away.z * Math.cos(turn));
        double dist = 30D + rand.nextDouble() * 20D;
        Vec3d at = m.getPositionVector().add(dir.scale(dist));
        BlockPos top = Barrow.surface(m.world, MathHelper.floor(at.x), MathHelper.floor(at.z));
        return new Vec3d(top.getX() + 0.5D, top.getY(), top.getZ() + 0.5D);
    }

    /** Down onto it, and on his feet. */
    private void landing(EntityPlayerMP p, EntityMagus m) {
        float k = Math.min(1F, actTicks / 16F);
        Vec3d at = from.add(landAt.subtract(from).scale(Timeline.smooth(k)));
        m.place(at, p.getPositionEyes(1F));
        if (actTicks == 1) {
            m.rest(Pose.STAND).perform(Pose.DESCEND, 16);
        }
        if (k >= 1F) {
            sound(at, SoundEvents.ENTITY_GENERIC_EXPLODE, 1F, 1.6F);
            ItemStack g = data().fightGauntlet;
            slotting = nextOfHis(g);
            if (m.hold() == EntityMagus.HOLD_WORN) {
                startPower(p, m);
            } else if (slotting >= 0) {
                startSlot(m);
            } else {
                startDon(m);
            }
        }
    }

    private void startSlot(EntityMagus m) {
        act(Act.SLOT);
        m.holdAs(EntityMagus.HOLD_HAND, slotting);
        m.rest(Pose.STAND).perform(Pose.SLOT, 0);
    }

    /** One stone, out of his hand and into its socket, drawn there like iron to a magnet. */
    private void slotIn(EntityPlayerMP p, EntityMagus m) {
        if (actTicks == EntityMagus.SLOT_GO) {
            Vec3d hand = m.getPositionVector().add(0D, 1.9D, 0D);
            EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.CHAMPION_STONE, hand.x, hand.y, hand.z, 0, 0, 0, m.getEntityId(),
                    EntityMagus.SLOT_IN - EntityMagus.SLOT_GO), m.dimension);
            sound(hand, EternitySounds.OMEN_SHIMMER, 2F, 1.2F);
        }
        if (actTicks == EntityMagus.SLOT_IN) {
            ItemStack g = data().fightGauntlet;
            setInto(g, slotting);
            data().markDirty();
            m.holdGauntlet(g, EntityMagus.HOLD_HAND);
            Vec3d c = m.gauntletAt();
            EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.MAGUS_IMPACT, c.x, c.y, c.z, 0, 1, 0, -1, 1), m.dimension);
            sound(c, SoundEvents.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 1.6F, 0.6F);
            sound(c, SoundEvents.BLOCK_ANVIL_LAND, 0.6F, 1.6F);
        }
        if (actTicks >= EntityMagus.SLOT_TICKS) {
            slotting = nextOfHis(data().fightGauntlet);
            if (slotting >= 0) {
                startSlot(m);
            } else {
                startDon(m);
            }
        }
    }

    private void startDon(EntityMagus m) {
        act(Act.DON);
        m.holdAs(EntityMagus.HOLD_HAND, -1);
        m.rest(Pose.STAND).perform(Pose.DON, 0);
    }

    private void donning(EntityPlayerMP p, EntityMagus m) {
        if (actTicks == EntityMagus.DON_IN) {
            m.holdAs(EntityMagus.HOLD_WORN, -1);
            Vec3d c = m.gauntletAt();
            sound(c, SoundEvents.ITEM_ARMOR_EQUIP_IRON, 2F, 0.6F);
            sound(c, SoundEvents.BLOCK_IRON_TRAPDOOR_CLOSE, 1.5F, 0.5F);
        }
        if (actTicks >= EntityMagus.DON_TICKS) {
            startPower(p, m);
        }
    }

    private void startPower(EntityPlayerMP p, EntityMagus m) {
        act(Act.POWER);
        m.faceNow(p.getPositionEyes(1F));
        m.rest(Pose.POWER).perform(Pose.POWER);
        StoryDirector.shout(m, MAGUS, m.getEntityId(), "Watch.", 128D);
    }

    /** The sky answers it: real lightning, nearer and more often as it fills him, setting nothing alight. */
    private void powering(EntityMagus m) {
        float k = actTicks / (float) EntityMagus.POWER_TICKS;
        int every = k < 0.25F ? 0 : k < 0.6F ? 50 : k < 0.85F ? 28 : 14;
        if (every > 0 && actTicks % every == 0) {
            double a = rand.nextDouble() * Math.PI * 2D, r = 4D + rand.nextDouble() * (14D - 8D * k);
            double x = m.posX + Math.cos(a) * r, z = m.posZ + Math.sin(a) * r;
            int y = Barrow.surface(m.world, MathHelper.floor(x), MathHelper.floor(z)).getY();
            m.world.addWeatherEffect(new net.minecraft.entity.effect.EntityLightningBolt(m.world, x, y, z, true));
        }
        if (actTicks == 110) {
            // Into his arm, up it, and into him: he throws his head back with it.
            sound(m.getPositionVector(), SoundEvents.ENTITY_ENDERDRAGON_GROWL, 2F, 0.6F);
            sound(m.getPositionVector(), SoundEvents.ENTITY_LIGHTNING_THUNDER, 3F, 0.8F);
        }
    }

    // ------------------------------------------------------------------ the snap: he has won

    /**
     * He snaps, and it is his. Everything goes white. It burns the arm that made it, as it burns anyone's, and scorches
     * the gauntlet; he stands bent over it, and says nothing to whoever asks him what he did. Then he goes up and away
     * with all six, and half of everything living about them goes to dust after him. There is no coming back from it:
     * the stones are his, they are left with nothing, and Eternity has nothing left to come for.
     */
    private void snapped(EntityPlayerMP p, EntityMagus m) {
        SovereignData d = data();
        act(Act.AFTERMATH);
        WorldServer world = p.getServerWorld();
        ItemStack g = d.fightGauntlet.copy();
        for (int i = 0; i < 3; i++) {
            com.matoon.eternity.stones.GauntletWear.wear(g, false);
        }
        claimHis(g);
        d.fightGauntlet = g;
        d.fight = SovereignData.Fight.LOST;
        d.fightSettled = StoryDirector.now();
        d.giveBack = ItemStack.EMPTY;
        d.markDirty();
        m.holdGauntlet(g, EntityMagus.HOLD_WORN);
        m.burn(1F);
        odinCrumbles(m.dimension);
        m.boss(false);
        m.guard(true);
        m.clearShots();
        m.stand();
        m.rest(Pose.SPENT);
        Vec3d at = m.getPositionVector();
        for (EntityPlayerMP o : world.getPlayers(EntityPlayerMP.class, o -> o.getDistance(m) < 160D)) {
            EternityNetwork.send(new MsgGauntletFight(MsgGauntletFight.WHITE, m.getEntityId(), 110), o);
        }
        sound(at, SoundEvents.ENTITY_LIGHTNING_THUNDER, 4F, 0.5F);
        sound(at, SoundEvents.ENTITY_GENERIC_EXPLODE, 2F, 0.4F);
        sound(at, SoundEvents.ENTITY_BLAZE_SHOOT, 2F, 0.5F);
        // Under the white, Warlock (if he lives) is down on the ground in front of him, between him and them.
        EntityAdamWarlock w = warlock != null && !warlock.isDead && !warlock.fallen() ? warlock : null;
        warlockDue = -1;
        Vec3d toward = flat(p.getPositionVector().subtract(at));
        if (toward.lengthSquared() < 1.0E-4D) {
            toward = flat(m.getLookVec());
        }
        if (w != null) {
            w.puppet(true);
            Vec3d spot = at.add(toward.scale(4.5D));
            w.place(new Vec3d(spot.x, groundUnder(world, spot.add(0D, 3D, 0D)), spot.z), m.getPositionEyes(1F));
            w.perform(EntityAdamWarlock.State.STAND);
        }
        Entity asker = w != null ? w : p;
        byte who = w != null ? WARLOCK : Speakers.PARTY;
        Vec3d sky = at.add(0D, 60D, 0D);
        // Not anchored on him: he goes in the middle of it, and a conversation ends when its anchor does.
        Talk t = new Talk(p).reach(96D);
        boolean[] done = new boolean[2];
        Runnable go = () -> {
            if (!done[0]) {
                done[0] = true;
                playing.add(goes(world, m));
            }
        };
        Runnable after = () -> {
            if (!done[1]) {
                done[1] = true;
                afterHim(world, at, w);
            }
        };
        t.abandoned(() -> {
            // Walked away from it: it plays out all the same.
            go.run();
            after.run();
        });
        // Timed to the camera (StoryClient.SNAPPED, SNAPPED_TICKS long, which starts about 20 ticks after it is called):
        // the question at about 45 and 145, him looking up at about 285, going at 315, and gone at about 340.
        t.pause(EntityMagus.SNAP_TICKS - EntityMagus.SNAP_AT)
                .run(() -> {
                    m.rest(Pose.SPENT).perform(Pose.SPENT);
                    m.look(asker.getPositionEyes(1F));
                    story.scene(lead(t, p), SNAPPED_SCENE, at, asker.getPositionVector());
                })
                .pause(50)
                .line(who, asker, "What did you do?")
                .pause(24)
                .line(who, asker, "What did you DO?")
                .pause(64)
                .run(() -> m.look(sky))
                .pause(28)
                .run(go)
                .pause(40)
                .run(after)
                .until(() -> story.sceneDone(p, SNAPPED_SCENE), 200)
                .run(() -> story.sendStateToAll());
        story.talk(p, t);
    }

    /** Every stone in it claimed for him: his now, all six. */
    private static void claimHis(ItemStack gauntlet) {
        InventoryInfinityGauntlet sockets = new InventoryInfinityGauntlet(gauntlet);
        for (int i = 0; i < sockets.getSizeInventory(); i++) {
            ItemStack stone = sockets.getStackInSlot(i);
            Relics.Kind kind = stone.isEmpty() ? null : Relics.kind(stone);
            if (kind != null) {
                Relics.release(kind);
                Relics.claim(stone, Sovereign.MAGUS_ID, "The Magus");
                sockets.setInventorySlotContents(i, stone);
            }
        }
        sockets.markDirty();
    }

    /** Theirs again, for an operator putting it all back: every stone in it but his two claimed for them. */
    private static void claimTheirs(ItemStack gauntlet, EntityPlayerMP player) {
        InventoryInfinityGauntlet sockets = new InventoryInfinityGauntlet(gauntlet);
        for (int i = 0; i < sockets.getSizeInventory(); i++) {
            EnumInfinityStone type = InventoryInfinityGauntlet.SLOTS[i];
            ItemStack stone = sockets.getStackInSlot(i);
            Relics.Kind kind = stone.isEmpty() ? null : Relics.kind(stone);
            if (kind != null && type != EnumInfinityStone.POWER && type != EnumInfinityStone.MIND) {
                Relics.release(kind);
                Relics.claim(stone, player);
                sockets.setInventorySlotContents(i, stone);
            }
        }
        sockets.markDirty();
    }

    /**
     * Up, all at once, off ground that breaks under him, straight into the sky with it on his hand; and high over them,
     * folded away, gone.
     */
    private Timeline goes(WorldServer world, EntityMagus m) {
        Timeline t = new Timeline();
        Vec3d[] from = new Vec3d[1];
        int dim = m.dimension;
        t.at(0, () -> {
            Vec3d f = from[0] = m.getPositionVector();
            m.rest(Pose.DESCEND).perform(Pose.DESCEND);
            EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.MAGUS_SLAM, f.x, f.y + 0.1D, f.z, 7D, 0, 0, 0, 0), dim);
            EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.MAGUS_BREACH, f.x, f.y + 1D, f.z, 2.5D, 0D, 0, 0, 50), dim);
            world.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE, f.x, f.y + 0.4D, f.z, 6, 1.4D, 0.2D, 1.4D, 0D);
            sound(f, SoundEvents.ENTITY_GENERIC_EXPLODE, 3F, 0.6F);
            sound(f, SoundEvents.ENTITY_ENDERDRAGON_FLAP, 3F, 0.5F);
            sound(f, EternitySounds.COMET_FLYBY, 5F, 0.7F);
        });
        t.span(0, 22, k -> {
            if (from[0] != null) {
                m.place(from[0].add(0D, 70D * k * k, 0D), null);
            }
        });
        t.at(22, () -> {
            m.perform(Pose.BLINK_OUT, 0);
            Vec3d top = m.getPositionVector().add(0D, 1.1D, 0D);
            EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.MAGUS_NOVA, top.x, top.y, top.z, 10D, 0, 0, -1, 0), dim);
            sound(top, SoundEvents.ENTITY_LIGHTNING_THUNDER, 5F, 0.6F);
            sound(top, EternitySounds.RIFT_IMPACT, 4F, 0.6F);
        });
        t.at(32, () -> {
            m.setDead();
            if (magus == m) {
                magus = null;
            }
        });
        return t;
    }

    /**
     * After him, what it did: half of everything living about them going to dust, one after another, for a while.
     * Nobody's pet, nothing named, and none of the story's own. Warlock, if he lives, watches the sky where he went, and
     * then goes up after him.
     */
    private void afterHim(WorldServer world, Vec3d at, @Nullable EntityAdamWarlock w) {
        List<net.minecraft.entity.EntityLiving> living = world.getEntitiesWithinAABB(net.minecraft.entity.EntityLiving.class,
                new net.minecraft.util.math.AxisAlignedBB(at, at).grow(48D), MagusFight::dustable);
        java.util.Collections.shuffle(living, rand);
        Timeline dust = new Timeline();
        int when = 20;
        for (net.minecraft.entity.EntityLiving e : living.subList(0, living.size() / 2)) {
            dust.at(when, () -> {
                if (e.isEntityAlive()) {
                    com.matoon.eternity.stones.SnapPowers.crumble(world, e);
                    e.setDead();
                }
            });
            when += 6 + rand.nextInt(18);
        }
        if (w != null && !w.isDead) {
            w.place(w.getPositionVector(), at.add(0D, 60D, 0D));
            int up = Math.max(when, 180);
            Vec3d[] from = new Vec3d[1];
            dust.at(up, () -> {
                from[0] = w.getPositionVector();
                w.perform(EntityAdamWarlock.State.HOVER);
                sound(from[0], EternitySounds.COMET_FLYBY, 5F, 1.2F);
                sound(from[0], SoundEvents.ENTITY_GENERIC_EXPLODE, 1.5F, 1.4F);
            });
            dust.span(up, up + 24, k -> {
                if (from[0] != null) {
                    w.place(from[0].add(0D, 80D * k * k, 0D), null);
                }
            });
            dust.at(up + 25, () -> {
                w.setDead();
                if (warlock == w) {
                    warlock = null;
                }
            });
        }
        dust.at(when, () -> {
        });
        playing.add(dust);
    }

    /** What his snap can take: anything living but the story's own, anyone's pet, or anything someone has named. */
    private static boolean dustable(@Nullable net.minecraft.entity.EntityLiving e) {
        if (e == null || !e.isEntityAlive() || !e.isNonBoss() || e.hasCustomName() || e.getClass().getName().startsWith("com.matoon.eternity")) {
            return false;
        }
        if (e instanceof net.minecraft.entity.passive.EntityTameable && ((net.minecraft.entity.passive.EntityTameable) e).isTamed()) {
            return false;
        }
        return !(e instanceof net.minecraft.entity.passive.AbstractHorse && ((net.minecraft.entity.passive.AbstractHorse) e).isTame());
    }

    /** Back to before he came: his stones his, theirs back to them when they wake, and him gone until they are ready again. */
    private void lost(@Nullable EntityPlayerMP p) {
        SovereignData d = data();
        ItemStack g = d.fightGauntlet;
        if (g.isEmpty() && p != null && marked(p) == -1) {
            // Lying where it was dropped: theirs again with the rest.
            Entity lying = lyingAbout(p);
            if (lying != null) {
                g = itemOf(lying).copy();
                lying.setDead();
            }
        }
        if (!g.isEmpty()) {
            takeOutHis(g);
            if (!d.giveBack.isEmpty()) {
                // One still owed from before: to whoever it was taken from, or someone, before this one is kept.
                EntityPlayerMP was = StoryDirector.player(owner(d));
                if (was == null || !was.isEntityAlive()) {
                    List<EntityPlayerMP> any = standing(owner(d), null, 0D);
                    was = any.isEmpty() ? null : any.get(0);
                }
                if (was != null) {
                    giveBack(was);
                } else {
                    com.matoon.eternity.Eternity.LOG.warn("[MagusFight] A gauntlet still owed back had nobody to go to, and was lost.");
                }
            }
            d.giveBack = g;
            d.giveBackTo = p != null ? p.getUniqueID() : d.fightPlayer;
        }
        // Anyone else still carrying it marked (a copy, or the one itself, if they had it): theirs, plain, again.
        MinecraftServer server = net.minecraftforge.fml.common.FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server != null) {
            for (EntityPlayerMP o : server.getPlayerList().getPlayers()) {
                if (o != p) {
                    unmarkAll(o);
                }
            }
        }
        playing.clear();
        felled.clear();
        goneSince = -1L;
        d.fightGauntlet = ItemStack.EMPTY;
        d.fight = SovereignData.Fight.HUNTING;
        d.nextAmbush = StoryDirector.now() + 20L * 120L;
        d.markDirty();
        if (magus != null) {
            magus.setDead();
            magus = null;
        }
        if (warlock != null) {
            warlock.dismiss();
            warlock = null;
        }
        if (odin != null) {
            int dim = odin.dimension;
            exits.add(bifrostAway(odin, () -> odinDark(dim, 0F, 120, 0)));
            odin = null;
        }
        d.odinCame = false;
        odinNext = false;
        act = Act.OVER;
        if (p != null) {
            if (p.isEntityAlive()) {
                giveBack(p);
            }
            story.sendState(p);
        } else {
            story.sendStateToAll();
        }
    }

    /** What he took, theirs again. */
    private void giveBack(EntityPlayerMP p) {
        SovereignData d = data();
        if (!d.giveBack.isEmpty()) {
            ItemStack g = d.giveBack.copy();
            unmark(g);
            net.minecraftforge.items.ItemHandlerHelper.giveItemToPlayer(p, g);
            d.giveBack = ItemStack.EMPTY;
            d.giveBackTo = null;
            d.markDirty();
        }
    }

    /** Whom what he took is owed back to: whoever it was taken from, or (from before that was kept) whose fight it was. */
    @Nullable
    private static java.util.UUID owner(SovereignData d) {
        return d.giveBackTo != null ? d.giveBackTo : d.fightPlayer;
    }

    /** No gauntlet on them is the one fought over any more. */
    private static void unmarkAll(EntityPlayerMP o) {
        boolean any = false;
        for (ItemStack s : o.inventory.mainInventory) {
            if (isMarked(s)) {
                unmark(s);
                any = true;
            }
        }
        if (isMarked(o.inventory.offHandInventory.get(0))) {
            unmark(o.inventory.offHandInventory.get(0));
            any = true;
        }
        if (isMarked(o.inventory.getItemStack())) {
            unmark(o.inventory.getItemStack());
            any = true;
        }
        if (any) {
            o.inventoryContainer.detectAndSendChanges();
        }
    }

    /**
     * Fallen in the fight: they lose it, and he goes, to come again when they are ready. The gauntlet, wherever it is (on them, in his hands, lying
     * about) is taken up before their things fall, to be given back when they wake; he goes.
     */
    void died(EntityPlayerMP p) {
        SovereignData d = data();
        if (d.fight != SovereignData.Fight.FIGHTING || act == Act.AFTERMATH) {
            return;
        }
        boolean theirs = p.getUniqueID().equals(d.fightPlayer);
        if (!theirs && marked(p) == -1 && !isMarked(p.inventory.getItemStack())) {
            return;
        }
        // Someone else still on their feet in it: it is not over. It goes on with them.
        EntityPlayerMP next = next(p);
        if (next != null || !theirs) {
            fallsOut(p, next);
            return;
        }
        if (act == Act.STRUGGLE && magus != null) {
            EternityNetwork.send(new MsgGauntletFight(MsgGauntletFight.END, magus.getEntityId(), 0), p);
        }
        if (!hisNow()) {
            int slot = marked(p);
            if (slot != -1) {
                d.fightGauntlet = takeFrom(p, slot);
            } else if (isMarked(p.inventory.getItemStack())) {
                d.fightGauntlet = p.inventory.getItemStack().copy();
                p.inventory.setItemStack(ItemStack.EMPTY);
            }
        }
        // Any copy left on them goes too, so nothing of it falls with their things.
        for (int i = 0; i < p.inventory.mainInventory.size(); i++) {
            if (isMarked(p.inventory.mainInventory.get(i))) {
                p.inventory.mainInventory.set(i, ItemStack.EMPTY);
            }
        }
        if (isMarked(p.inventory.offHandInventory.get(0))) {
            p.inventory.offHandInventory.set(0, ItemStack.EMPTY);
        }
        d.markDirty();
        lost(p);
    }

    void respawned(EntityPlayerMP p) {
        SovereignData d = data();
        if (p.getUniqueID().equals(owner(d))) {
            giveBack(p);
        }
    }

    // ------------------------------------------------------------------ more than one of them

    /**
     * Who else is in it: everyone living, and not only watching, within range of something (him, usually; anywhere, if
     * nothing), whoever quests with {@code with} first, then the nearest. Nobody felled and spared while it lasts.
     */
    private List<EntityPlayerMP> standing(@Nullable java.util.UUID with, @Nullable Entity near, double range) {
        List<EntityPlayerMP> out = new ArrayList<>();
        MinecraftServer server = net.minecraftforge.fml.common.FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) {
            return out;
        }
        List<java.util.UUID> fellows = with != null ? story.data().fellows(with) : java.util.Collections.<java.util.UUID>emptyList();
        long now = StoryDirector.now();
        for (EntityPlayerMP o : server.getPlayerList().getPlayers()) {
            Long down = felled.get(o.getUniqueID());
            if (o.isEntityAlive() && !o.isSpectator() && (down == null || now >= down)
                    && (near == null || o.world == near.world && o.getDistance(near) < range)) {
                out.add(o);
            }
        }
        out.sort(java.util.Comparator.comparingInt((EntityPlayerMP o) -> {
            int i = fellows.indexOf(o.getUniqueID());
            return i < 0 ? Integer.MAX_VALUE : i;
        }).thenComparingDouble(o -> near != null ? o.getDistanceSq(near) : 0D));
        return out;
    }

    /** Someone else still on their feet in it, near him (or Odin), when this one is out of it; null if nobody is. */
    @Nullable
    private EntityPlayerMP next(EntityPlayerMP out) {
        Entity near = magus != null && !magus.isDead ? magus : odin != null && !odin.isDead ? odin : out;
        for (EntityPlayerMP o : standing(out.getUniqueID(), near, 96D)) {
            if (o != out) {
                return o;
            }
        }
        return null;
    }

    /**
     * Whose fight it is when they have gone out of the world: someone near him, or, with him gone too, anyone online
     * who was questing with them. Null if nobody.
     */
    @Nullable
    private EntityPlayerMP successor(@Nullable java.util.UUID was) {
        Entity near = magus != null && !magus.isDead ? magus : odin != null && !odin.isDead ? odin : null;
        if (near != null) {
            List<EntityPlayerMP> them = standing(was, near, 96D);
            if (!them.isEmpty()) {
                return them.get(0);
            }
        }
        if (was == null) {
            return null;
        }
        for (EntityPlayerMP o : standing(was, null, 0D)) {
            if (story.data().fellows(was, o.getUniqueID()) && !story.talking(o)) {
                return o;
            }
        }
        return null;
    }

    /** It is someone else's fight now: what was theirs to do with him is this one's. */
    private void takeOver(EntityPlayerMP next) {
        SovereignData d = data();
        d.fightPlayer = next.getUniqueID();
        d.markDirty();
        grabbing = -1;
        if (act == Act.STRUGGLE) {
            // Whoever he was hand to hand with is out of it: he has it or he hasn't, and goes on.
            if (magus != null) {
                magus.guard(false);
            }
            struggleCooldown = STRUGGLE_COOLDOWN;
            landAt = null;
            act(hisNow() ? Act.FLEE : Act.HUNT);
        }
        if (magus != null && magus.isFighting()) {
            magus.fight(next, warlock);
        }
        story.sendState(next);
    }

    /**
     * Down, and out of it, while the rest fight on. If they had it, it falls where they are, for whoever gets to it
     * first, him included; nothing else of it stays on them. If it was their fight, it is the next one's.
     */
    private void fallsOut(EntityPlayerMP p, @Nullable EntityPlayerMP next) {
        SovereignData d = data();
        boolean theirs = p.getUniqueID().equals(d.fightPlayer);
        if (theirs && act == Act.STRUGGLE && magus != null) {
            EternityNetwork.send(new MsgGauntletFight(MsgGauntletFight.END, magus.getEntityId(), 0), p);
        }
        ItemStack g = ItemStack.EMPTY;
        int slot = marked(p);
        if (slot != -1) {
            g = takeFrom(p, slot);
        } else if (isMarked(p.inventory.getItemStack())) {
            g = p.inventory.getItemStack().copy();
            p.inventory.setItemStack(ItemStack.EMPTY);
            p.updateHeldItem();
        }
        for (int i = 0; i < p.inventory.mainInventory.size(); i++) {
            if (isMarked(p.inventory.mainInventory.get(i))) {
                p.inventory.mainInventory.set(i, ItemStack.EMPTY);
            }
        }
        if (isMarked(p.inventory.offHandInventory.get(0))) {
            p.inventory.offHandInventory.set(0, ItemStack.EMPTY);
        }
        p.inventoryContainer.detectAndSendChanges();
        if (!g.isEmpty()) {
            EntityItem item = new EntityItem(p.world, p.posX, p.posY + 0.5D, p.posZ, g);
            item.setPickupDelay(40);
            p.world.spawnEntity(item);
        }
        if (theirs && next != null) {
            takeOver(next);
        }
    }

    // ------------------------------------------------------------------ the struggle

    private void startStruggle(EntityPlayerMP p, EntityMagus m, boolean his) {
        act(Act.STRUGGLE);
        m.guard(true);
        grabbing = -1;
        open = false;
        grabAfter = 2 + rand.nextInt(3);
        attacksAtGrab = m.attacksDone();
        heHadIt = his;
        bar = 0.5F;
        presses = 0;
        lastPress = -10;
        holdPlayer = p.getPositionVector();
        m.stand();
        Vec3d toward = flat(m.getPositionVector().subtract(p.getPositionVector()));
        if (toward.lengthSquared() < 1.0E-4D) {
            toward = flat(p.getLookVec());
        }
        Vec3d at = new Vec3d(p.posX, p.posY, p.posZ).add(toward.scale(1.7D));
        m.place(at, p.getPositionEyes(1F));
        m.rest(Pose.STRUGGLE).perform(Pose.STRUGGLE);
        if (!his) {
            m.holdGauntlet(ItemStack.EMPTY, EntityMagus.HOLD_NONE);
        }
        EternityNetwork.send(new MsgGauntletFight(MsgGauntletFight.START, m.getEntityId(), 500), p);
        sound(at, SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.5F, 0.5F);
        sound(at, EternitySounds.OMEN_SHIMMER, 1.5F, 0.7F);
        StoryDirector.shout(m, MAGUS, m.getEntityId(), his ? "Let go." : "Give it to me.", 64D);
    }

    /** One pull of theirs. Faster than a person can press, and it does not count. */
    void press(EntityPlayerMP p) {
        if (act != Act.STRUGGLE || !p.getUniqueID().equals(data().fightPlayer) || actTicks - lastPress < 2) {
            return;
        }
        lastPress = actTicks;
        presses++;
        bar = Math.min(1F, bar + 0.05F);
    }

    private void struggling(EntityPlayerMP p, EntityMagus m) {
        if (!heHadIt && marked(p) == -1) {
            // They let go of it: dropped at their feet, and his.
            Entity lying = lyingAbout(p);
            if (lying != null && lying.getDistance(m) < 8D) {
                data().fightGauntlet = itemOf(lying).copy();
                data().markDirty();
                lying.setDead();
            }
            endStruggle(p, m, false);
            return;
        }
        // Neither of them goes anywhere.
        p.setPositionAndUpdate(holdPlayer.x, holdPlayer.y, holdPlayer.z);
        p.motionX = p.motionY = p.motionZ = 0D;
        // He pulls harder the longer it goes, and harder still if he had it going in.
        bar -= (heHadIt ? 0.0105F : 0.0095F) + 0.004F * actTicks / STRUGGLE_TICKS;
        if (actTicks % 2 == 0) {
            EternityNetwork.send(new MsgGauntletFight(MsgGauntletFight.BAR, m.getEntityId(), (int) (MathHelper.clamp(bar, 0F, 1F) * 1000F)), p);
        }
        if (actTicks % 10 == 0) {
            sound(m.getPositionVector(), SoundEvents.BLOCK_IRON_DOOR_OPEN, 0.6F, 0.5F + rand.nextFloat() * 0.3F);
        }
        if (bar >= 1F || bar <= 0F || actTicks >= STRUGGLE_TICKS) {
            endStruggle(p, m, bar >= 0.5F && bar > 0F);
        }
    }

    private void endStruggle(EntityPlayerMP p, EntityMagus m, boolean won) {
        SovereignData d = data();
        m.guard(false);
        struggleCooldown = STRUGGLE_COOLDOWN;
        EternityNetwork.send(new MsgGauntletFight(MsgGauntletFight.END, m.getEntityId(), won ? 1 : 0), p);
        Vec3d away = flat(m.getPositionVector().subtract(p.getPositionVector()));
        Vec3d c = p.getPositionVector().add(away.scale(0.85D)).add(0D, 1.4D, 0D);
        EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.MAGUS_IMPACT, c.x, c.y, c.z, away.x, 0, away.z, -1, 1), m.dimension);
        sound(c, SoundEvents.ENTITY_GENERIC_EXPLODE, 2F, 0.8F);
        if (won) {
            if (heHadIt) {
                ItemStack g = d.fightGauntlet;
                d.fightGauntlet = ItemStack.EMPTY;
                d.markDirty();
                m.holdGauntlet(ItemStack.EMPTY, EntityMagus.HOLD_NONE);
                if (!p.inventory.addItemStackToInventory(g)) {
                    p.dropItem(g, false);
                }
                p.inventoryContainer.detectAndSendChanges();
            }
            from = m.getPositionVector();
            to = from.add(away.scale(20D)).add(0D, 3D, 0D);
            m.rest(Pose.FLUNG).perform(Pose.FLUNG);
            act(Act.FLUNG);
            StoryDirector.shout(m, MAGUS, m.getEntityId(), "...", 64D);
        } else {
            if (!heHadIt) {
                int slot = marked(p);
                if (slot != -1) {
                    ItemStack g = takeFrom(p, slot);
                    d.fightGauntlet = g;
                    d.markDirty();
                }
            }
            m.holdGauntlet(d.fightGauntlet, EntityMagus.HOLD_CRADLE);
            Vec3d push = away.scale(-1D);
            p.motionX = push.x * 3.2D;
            p.motionY = 0.9D;
            p.motionZ = push.z * 3.2D;
            p.velocityChanged = true;
            p.fallDistance = 0F;
            act(hisNow() ? Act.FLEE : Act.HUNT);
            landAt = null;
        }
        story.sendState(p);
    }

    /** Thrown back, a long way; then up again, and after it. */
    private void flung(EntityPlayerMP p, EntityMagus m) {
        float k = Math.min(1F, actTicks / 12F);
        m.place(from.add(to.subtract(from).scale(Timeline.out(k))), from);
        if (actTicks == 14) {
            m.rest(Pose.FLOAT).perform(Pose.STAGGER, 0);
        }
        if (actTicks >= 40) {
            act(Act.HUNT);
        }
    }

    // ------------------------------------------------------------------ Warlock

    private void warlockArrives(EntityPlayerMP p, EntityMagus m) {
        warlockDue = -1;
        if (data().warlockFell) {
            return;
        }
        EntityAdamWarlock w = warlock = new EntityAdamWarlock(p.world);
        w.evolve();
        w.mortal(EternityConfig.magus.warlockCanDie);
        // Down out of the sky at him, wherever he has got to.
        Vec3d at = m.getPositionVector().add(flat(p.getPositionVector().subtract(m.getPositionVector())).scale(6D)).add(0D, 2.5D, 0D);
        w.script(true);
        w.arriveAt(at, m.getPositionEyes(1F));
        p.world.spawnEntity(w);
        warlockIn = false;
        StoryDirector.shout(w, WARLOCK, w.getEntityId(), "Sorry. I'm here. Keep it away from him.", 160D);
    }

    /** Warlock has caught him with it: a few seconds of the two of them, and he is off again, with it. */
    private void startClash(EntityPlayerMP p, EntityMagus m, EntityAdamWarlock w) {
        resumeAct = act;
        resumeTicks = actTicks;
        resumeAt = m.getPositionVector();
        act(Act.CLASH);
        m.guard(true);
        clashCooldown = CLASH_COOLDOWN;
        int kind = rand.nextInt(3);
        Timeline t = clash = new Timeline();
        Vec3d mAt = m.getPositionVector(), wAt = w.getPositionVector();
        Vec3d dir = flat(mAt.subtract(wAt));
        if (dir.lengthSquared() < 1.0E-4D) {
            dir = new Vec3d(1D, 0D, 0D);
        }
        Vec3d d = dir;
        m.stand();
        boolean worn = m.hold() == EntityMagus.HOLD_WORN;
        Pose rest = worn ? Pose.FLOAT : Pose.CRADLE;
        t.at(0, () -> {
            w.puppet(true);
            m.rest(rest);
        });
        Vec3d meet = mAt.subtract(d.scale(1.3D));
        if (kind == 0) {
            // He catches the fist, and puts a palm through Warlock's chest.
            t.at(0, () -> w.perform(EntityAdamWarlock.State.FLYBY));
            t.span(0, 6, k -> w.place(Timeline.lerp(wAt, meet, Timeline.in(k)), m.getPositionEyes(1F)));
            t.at(6, () -> {
                m.faceNow(w.getPositionEyes(1F));
                m.perform(Pose.CATCH, 18);
                w.perform(EntityAdamWarlock.State.CAUGHT);
                fx(MsgTribunalFx.WARLOCK_IMPACT, w.chest().add(d.scale(0.6D)), d.scale(-1D), 0);
                sound(meet, SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, 2F, 0.5F);
            });
            t.at(14, () -> StoryDirector.shout(m, MAGUS, m.getEntityId(), "No.", 96D));
            t.at(24, () -> {
                m.perform(Pose.PALM, 0);
                sound(meet, SoundEvents.ENTITY_GENERIC_EXPLODE, 2F, 1.2F);
            });
            throwOff(t, w, 24 + EntityMagus.PALM_HIT, d.scale(-1D));
        } else if (kind == 1) {
            // He reaches for it; and the Magus is not there, and then behind him.
            t.at(0, () -> w.perform(EntityAdamWarlock.State.CARRY));
            t.span(0, 10, k -> w.place(Timeline.lerp(wAt, meet, Timeline.in(k)), m.gauntletAt()));
            t.at(9, () -> {
                m.perform(Pose.BLINK_OUT, 0);
                sound(mAt, SoundEvents.ENTITY_ENDERMEN_TELEPORT, 1.5F, 0.8F);
            });
            t.at(13, () -> {
                Vec3d behind = meet.subtract(d.scale(1.6D));
                m.blink(behind, w.getPositionEyes(1F), false);
                w.perform(EntityAdamWarlock.State.HOVER);
            });
            t.at(16, () -> StoryDirector.shout(w, WARLOCK, w.getEntityId(), "Where-", 96D));
            t.at(22, () -> m.perform(Pose.BACKHAND, 0));
            throwOff(t, w, 22 + EntityMagus.BACKHAND_HIT, d);
        } else {
            // Warlock's beam, held off with one palm, pushed back down its own length; then a knee.
            t.at(0, () -> {
                w.place(wAt, m.getPositionEyes(1F));
                w.perform(EntityAdamWarlock.State.BEAM);
            });
            t.at(EntityAdamWarlock.BEAM_CHARGE, () -> {
                m.faceNow(w.getPositionEyes(1F));
                m.perform(Pose.BLOCK, 40);
            });
            t.span(EntityAdamWarlock.BEAM_CHARGE, EntityAdamWarlock.BEAM_CHARGE + 30, k -> {
                if (k > 0.5F) {
                    m.place(Timeline.lerp(mAt, wAt.add(d.scale(1.4D)), (k - 0.5F) * 2F), w.getPositionEyes(1F));
                }
            });
            t.at(EntityAdamWarlock.BEAM_CHARGE + 30, () -> {
                m.blink(wAt.add(d.scale(0.9D)), w.getPositionEyes(1F), false);
                m.perform(Pose.KNEE, 0);
            });
            throwOff(t, w, EntityAdamWarlock.BEAM_CHARGE + 30 + EntityMagus.KNEE_HIT, d.scale(-1D));
        }
    }

    /** Warlock, struck, thrown back the length of a house. */
    private void throwOff(Timeline t, EntityAdamWarlock w, int at, Vec3d dir) {
        Vec3d[] path = new Vec3d[2];
        t.at(at, () -> {
            path[0] = w.getPositionVector();
            path[1] = path[0].add(dir.scale(11D)).add(0D, 2.5D, 0D);
            w.perform(EntityAdamWarlock.State.STAGGER);
            fx(MsgTribunalFx.MAGUS_IMPACT, w.chest(), dir, 1);
            if (magus != null) {
                // Every time, it costs him.
                w.attackEntityFrom(com.matoon.eternity.entity.EternityDamage.wielded("magus", magus), w.getMaxHealth() * 0.1F);
            }
            sound(path[0], SoundEvents.ENTITY_PLAYER_HURT, 2F, 0.6F);
        });
        t.span(at, at + 10, k -> w.place(Timeline.lerp(path[0], path[1], Timeline.out(k)), path[0]));
        t.at(at + 24, () -> {
        });
    }

    private void afterClash(EntityPlayerMP p, EntityMagus m) {
        m.guard(false);
        EntityAdamWarlock w = warlock;
        if (w != null && !w.fallen()) {
            w.puppet(false);
            w.turnOn(m);
        }
        if (!hisNow()) {
            act(Act.HUNT);
            return;
        }
        // Where he was, doing what he was doing, from where he left off.
        switch (resumeAct) {
            case LAND:
            case SLOT:
            case DON:
            case POWER: {
                Vec3d back = resumeAt;
                m.blink(back, p.getPositionEyes(1F), false);
                act(resumeAct);
                actTicks = resumeAct == Act.LAND ? 16 : resumeTicks;
                if (resumeAct == Act.LAND) {
                    from = back;
                } else if (resumeAct == Act.SLOT) {
                    m.holdAs(EntityMagus.HOLD_HAND, slotting);
                    m.rest(Pose.STAND).resume(Pose.SLOT, resumeTicks);
                } else if (resumeAct == Act.DON) {
                    m.rest(Pose.STAND).resume(Pose.DON, resumeTicks);
                } else {
                    m.faceNow(p.getPositionEyes(1F));
                    m.rest(Pose.POWER).resume(Pose.POWER, resumeTicks);
                }
                break;
            }
            default:
                // In the air: on to where he was going.
                act(Act.FLEE);
                actTicks = 2;
                boolean worn = m.hold() == EntityMagus.HOLD_WORN;
                m.rest(worn ? Pose.FLOAT : Pose.CRADLE).perform(worn ? Pose.FLOAT : Pose.CRADLE);
                break;
        }
    }

    // ------------------------------------------------------------------ whoever has it

    /** Whoever has the marked gauntlet on them, them first; null if nobody does. */
    @Nullable
    private static EntityPlayerMP holder(MinecraftServer server, @Nullable EntityPlayerMP them) {
        if (them != null && (marked(them) != -1 || isMarked(them.inventory.getItemStack()))) {
            return them;
        }
        for (EntityPlayerMP o : server.getPlayerList().getPlayers()) {
            if (o.isEntityAlive() && !o.isSpectator() && (marked(o) != -1 || isMarked(o.inventory.getItemStack()))) {
                return o;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ worn down: he reels

    /**
     * His health run out. He is thrown back out of whatever he was doing, and for a few seconds he is nothing but
     * hurt; if he had the gauntlet it goes out of his hands, wherever it falls, his stones in it and all. Then it
     * comes back into him, half of it, and he is worse than before.
     */
    private void reel(EntityPlayerMP p, EntityMagus m) {
        SovereignData d = data();
        boolean his = hisNow();
        act(Act.REEL);
        grabbing = -1;
        m.stand();
        m.guard(true);
        m.clearShots();
        Vec3d away = flat(m.getPositionVector().subtract(p.getPositionVector()));
        from = m.getPositionVector();
        to = from.add(away.scale(6D)).add(0D, 1D, 0D);
        m.rest(Pose.FLOAT).perform(Pose.FLUNG);
        Vec3d c = m.getPositionVector().add(0D, 1.3D, 0D);
        fx(MsgTribunalFx.MAGUS_IMPACT, c, away, 2);
        sound(c, SoundEvents.ENTITY_ENDERDRAGON_HURT, 2F, 0.6F);
        sound(c, SoundEvents.BLOCK_GLASS_BREAK, 2F, 0.5F);
        sound(c, SoundEvents.ENTITY_GENERIC_EXPLODE, 2F, 0.8F);
        if (his) {
            ItemStack g = d.fightGauntlet;
            d.fightGauntlet = ItemStack.EMPTY;
            d.markDirty();
            m.holdGauntlet(ItemStack.EMPTY, EntityMagus.HOLD_NONE);
            EntityItem item = new EntityItem(m.world, c.x, c.y, c.z, g);
            item.motionX = -away.x * 0.25D;
            item.motionY = 0.35D;
            item.motionZ = -away.z * 0.25D;
            item.setPickupDelay(10);
            m.world.spawnEntity(item);
            sound(c, SoundEvents.ENTITY_ITEM_BREAK, 1.5F, 0.5F);
            StoryDirector.shout(m, MAGUS, m.getEntityId(), "No-", 128D);
        } else if (!(m.breaks() == 2 && odinDue())) {
            StoryDirector.shout(m, MAGUS, m.getEntityId(), REELING[rand.nextInt(REELING.length)], 128D);
        }
        odinNext = m.breaks() == 2 && odinDue();
        story.sendState(p);
    }

    private static final String[] REELING = {"...Enough.", "You. Hurt. Me.", "I have died before. It did not take."};

    private void reeling(EntityPlayerMP p, EntityMagus m) {
        float k = Math.min(1F, actTicks / 14F);
        if (actTicks <= 14) {
            m.place(from.add(to.subtract(from).scale(Timeline.out(k))), from);
        }
        if (actTicks == 14) {
            m.rest(Pose.FLOAT).perform(Pose.STAGGER, 0);
        }
        if (actTicks > 20) {
            // It comes back into him, and the bar with it.
            m.recover(0.55F * Math.min(1F, (actTicks - 20) / 50F));
        }
        if (odinNext && actTicks == 26) {
            StoryDirector.shout(m, MAGUS, m.getEntityId(), "You cannot defeat me.", 128D);
        }
        if (odinNext && actTicks == 64) {
            StoryDirector.shout(m, MAGUS, m.getEntityId(), "Give up the stones.", 128D);
        }
        if (actTicks == 70) {
            Vec3d c = m.getPositionVector().add(0D, 1.2D, 0D);
            EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.MAGUS_GATHER, c.x, c.y, c.z, 2.5D, 1.2D, 0D, m.getEntityId(), 14),
                    m.dimension);
            sound(c, SoundEvents.ENTITY_ENDERDRAGON_GROWL, 2F, 0.8F);
        }
        if (actTicks >= (odinNext ? 120 : 84)) {
            m.recover(0.55F);
            m.guard(false);
            landAt = null;
            if (odinNext) {
                odinNext = false;
                odinComes(p, m);
                return;
            }
            act(hisNow() ? Act.FLEE : Act.HUNT);
        }
    }

    // ------------------------------------------------------------------ Warlock falls

    /**
     * Warlock, beaten by him, and not driven off this time. The fight stops for it. He comes down out of the sky and
     * does not get up; the Magus stands over him, and has him up by the throat, and takes the light out of him, and
     * lets him fall. He has a last word for them, and it goes up out of him, gold, and he goes with it. Then it goes
     * on, and nothing is ever going to bring him back.
     */
    private void elegy(EntityPlayerMP p, EntityMagus m, EntityAdamWarlock w) {
        SovereignData d = data();
        act(Act.ELEGY);
        if (odin != null) {
            odin.hold(true);
            odin.guard(true);
        }
        d.warlockFell = true;
        d.markDirty();
        warlockDue = -1;
        grabbing = -1;
        m.stand();
        m.guard(true);
        m.clearShots();
        m.rest(Pose.FLOAT).perform(Pose.FLOAT);
        m.look(w.getPositionEyes(1F));
        WorldServer world = p.getServerWorld();
        int dim = m.dimension;
        // Where he fell from, where he lies.
        Vec3d[] at = new Vec3d[2];
        Timeline fall = new Timeline();
        int fallTicks = 14;
        fall.at(0, () -> {
            at[0] = w.getPositionVector();
            at[1] = new Vec3d(at[0].x, groundUnder(world, at[0]), at[0].z);
            w.perform(EntityAdamWarlock.State.STAGGER);
            fx(MsgTribunalFx.MAGUS_IMPACT, w.chest(), new Vec3d(0D, -1D, 0D), 2);
            sound(at[0], SoundEvents.ENTITY_PLAYER_HURT, 2F, 0.5F);
            sound(at[0], SoundEvents.ENTITY_GENERIC_EXPLODE, 2F, 0.7F);
        });
        fall.span(0, fallTicks, k -> w.place(Timeline.lerp(at[0], at[1], Timeline.in(k)), null));
        fall.at(fallTicks, () -> {
            w.perform(EntityAdamWarlock.State.DOWNED);
            sound(at[1], SoundEvents.ENTITY_PLAYER_BIG_FALL, 2F, 0.5F);
            sound(at[1], SoundEvents.BLOCK_ANVIL_LAND, 0.8F, 0.5F);
            world.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL, at[1].x, at[1].y + 0.2D, at[1].z, 14, 0.7D, 0.1D, 0.7D, 0.02D);
        });
        playing.add(fall);
        Timeline[] hold = new Timeline[1];
        Talk t = new Talk(p).at(m, 160D).reach(96D);
        t.abandoned(() -> endElegy(p));
        // Timed to the camera (StoryClient.ELEGY, ELEGY_TICKS long), from when he stands over him: lifted at about 300,
        // let fall at about 355, last words from 410, gone at about 540, and the light of the Magus stuttering at 620.
        // The Magus is what Warlock would have become: everything Warlock has done, he remembers doing. Not this. In
        // his past Warlock got up again; here he doesn't, and the road that made the Magus ends under his own hand.
        t.pause(fallTicks + 6)
                .run(() -> {
                    // Over him, on the far side of him from them, looking down.
                    Vec3d side = flat(lead(t, p).getPositionVector().subtract(at[1]));
                    Vec3d stand = at[1].subtract(side.scale(1.7D));
                    Vec3d feet = new Vec3d(stand.x, groundUnder(world, stand.add(0D, 2D, 0D)), stand.z);
                    m.blink(feet, at[1], false);
                    m.rest(Pose.REGARD).perform(Pose.REGARD);
                    m.look(at[1].add(0D, 0.3D, 0D));
                    story.scene(lead(t, p), ELEGY_SCENE, at[1], feet);
                })
                .pause(12)
                .line(MAGUS, m, "I don't remember this.")
                .line(WARLOCK, w, "Then kill me. And you never happen.")
                .line(MAGUS, m, "I already happened.")
                .run(() -> {
                    hold[0] = throat(m, w);
                    playing.add(hold[0]);
                })
                .pause(16)
                .run(() -> {
                    // The light taken out of him: the gold goes, and he goes limp.
                    m.perform(Pose.TAKE);
                    Vec3d chest = w.chest();
                    EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.MAGUS_GRASP, chest.x, chest.y, chest.z, w.getEntityId(), 0, 0,
                            m.getEntityId(), 40), dim);
                    w.spent();
                    sound(chest, EternitySounds.OMEN_SHIMMER, 3F, 0.4F);
                    sound(chest, SoundEvents.ENTITY_PLAYER_HURT_DROWN, 1.2F, 0.7F);
                    sound(chest, SoundEvents.BLOCK_END_PORTAL_FRAME_FILL, 2F, 0.5F);
                })
                .pause(40)
                .run(() -> {
                    if (hold[0] != null) {
                        hold[0].stop();
                    }
                    playing.add(letFall(world, w));
                    m.rest(Pose.REGARD).perform(Pose.REGARD);
                    m.look(w.getPositionVector().add(0D, 0.3D, 0D));
                })
                .pause(24)
                .run(() -> w.place(w.getPositionVector(), lead(t, p).getPositionEyes(1F)))
                .pause(30)
                .line(WARLOCK, w, "Don't let him close his hand.")
                .run(() -> {
                    Vec3d body = w.getPositionVector();
                    EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.MAGUS_SOUL, body.x, body.y + 0.35D, body.z, 0, 0, 0, -1, 90), dim);
                    EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.MAGUS_BREACH, body.x, body.y + 0.6D, body.z, 3D, 1D, 0, 0, 60), dim);
                    sound(body, SoundEvents.BLOCK_PORTAL_TRIGGER, 0.8F, 1.6F);
                    sound(body, EternitySounds.OMEN_SHIMMER, 3F, 1.2F);
                })
                .pause(30)
                .run(() -> {
                    Vec3d c = w.chest();
                    EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.WARLOCK_SHATTER, c.x, c.y, c.z, 0, 0, 0, w.getEntityId(), 30), dim);
                    w.setDead();
                    if (warlock == w) {
                        warlock = null;
                    }
                })
                .pause(80)
                .run(() -> {
                    // Something taken out from under him: the light of him stutters, once, and holds.
                    m.blink(m.getPositionVector(), null, false);
                    sound(m.getPositionVector(), EternitySounds.OMEN_SHIMMER, 3F, 0.5F);
                })
                .direction("The light of him stutters, once. Then it holds.")
                .until(() -> story.sceneDone(p, ELEGY_SCENE), 120)
                .run(() -> endElegy(p));
        story.talk(p, t);
    }

    /** Held up at the end of his arm, every tick, until he lets go. */
    private Timeline throat(EntityMagus m, EntityAdamWarlock w) {
        Timeline hold = new Timeline();
        Vec3d[] from = new Vec3d[1];
        hold.at(0, () -> {
            from[0] = w.getPositionVector();
            m.faceNow(w.getPositionVector().add(0D, 1.2D, 0D));
            m.rest(Pose.GRIP).perform(Pose.GRIP);
            m.hoverTo(m.getPositionVector().add(0D, 1.1D, 0D), 0.035D);
            w.perform(EntityAdamWarlock.State.HELD);
            sound(from[0], SoundEvents.ENTITY_PLAYER_HURT, 1.5F, 0.6F);
            sound(from[0], SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.5F, 0.5F);
        });
        hold.span(0, 20 * 60, k -> {
            Vec3d hand = m.gripHand();
            float yaw = (float) Math.toRadians(m.rotationYaw);
            Vec3d at = hand.subtract(0D, 1.5D, 0D).add(-Math.sin(yaw) * 0.12D, 0D, Math.cos(yaw) * 0.12D);
            int n = hold.now();
            w.place(n < 8 ? Timeline.lerp(from[0], at, Timeline.out(n / 8F)) : at, m.getPositionEyes(1F));
        });
        return hold;
    }

    /** Let go of, he drops like something thrown away, and lies where he lands. */
    private Timeline letFall(WorldServer world, EntityAdamWarlock w) {
        Timeline t = new Timeline();
        Vec3d[] path = new Vec3d[2];
        t.at(0, () -> {
            path[0] = w.getPositionVector();
            path[1] = new Vec3d(w.posX, groundUnder(world, path[0]), w.posZ);
            w.perform(EntityAdamWarlock.State.STAGGER);
        });
        t.span(0, 7, k -> w.place(Timeline.lerp(path[0], path[1], Timeline.in(k)), null));
        t.at(7, () -> {
            w.perform(EntityAdamWarlock.State.DOWNED);
            sound(path[1], SoundEvents.ENTITY_PLAYER_BIG_FALL, 1.5F, 0.6F);
            world.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL, path[1].x, path[1].y + 0.2D, path[1].z, 10, 0.6D, 0.1D, 0.6D, 0.02D);
        });
        return t;
    }

    /** It goes on: Warlock gone, for good, and the Magus back at it, wherever he had got to. */
    private void endElegy(EntityPlayerMP p) {
        for (Timeline t : playing) {
            t.stop();
        }
        playing.clear();
        if (warlock != null) {
            warlock.setDead();
            warlock = null;
        }
        warlockDue = -1;
        EntityMagus m = magus;
        if (!on() || act != Act.ELEGY) {
            return;
        }
        landAt = null;
        act(hisNow() ? Act.FLEE : Act.HUNT);
        if (m != null) {
            m.guard(false);
        }
        if (odin != null) {
            odin.hold(false);
            odin.guard(false);
        }
        EntityPlayerMP now = StoryDirector.player(data().fightPlayer);
        story.sendState(now != null ? now : p);
    }

    /** The top of whatever is solid under a point (or the surface, if nothing is within forty blocks). */
    private static double groundUnder(WorldServer world, Vec3d at) {
        net.minecraft.util.math.RayTraceResult hit = world.rayTraceBlocks(at.add(0D, 0.5D, 0D), at.subtract(0D, 40D, 0D), false, true, false);
        if (hit != null && hit.typeOfHit == net.minecraft.util.math.RayTraceResult.Type.BLOCK) {
            return hit.hitVec.y;
        }
        return Barrow.surface(world, MathHelper.floor(at.x), MathHelper.floor(at.z)).getY();
    }

    // ------------------------------------------------------------------ the end: their snap

    /**
     * Their snap, with all six in the gauntlet on their hand, while he is in front of them:
     * for him only. He comes apart, and it is over. True if it was, and nothing else is done.
     */
    boolean snap(EntityLivingBase entity) {
        SovereignData d = data();
        if (!(entity instanceof EntityPlayerMP) || d.fight != SovereignData.Fight.FIGHTING) {
            return false;
        }
        EntityPlayerMP p = (EntityPlayerMP) entity;
        // Theirs whose fight it is, with all six; or anyone's, with all six in the one fought over.
        boolean theirs = p.getUniqueID().equals(d.fightPlayer);
        ItemStack g = p.getHeldItemMainhand();
        if (!isGauntlet(g) || !whole(g) || !theirs && !isMarked(g)) {
            g = p.getHeldItemOffhand();
        }
        if (!isGauntlet(g) || !whole(g) || !theirs && !isMarked(g)) {
            return false;
        }
        if (!theirs) {
            d.fightPlayer = p.getUniqueID();
        }
        EntityMagus m = magus;
        claimAll(g, p);
        if (g.hasTagCompound()) {
            g.getTagCompound().removeTag(MARK);
        }
        p.inventoryContainer.detectAndSendChanges();
        com.matoon.eternity.stones.InfinityPatches.snapped(p, p.getPrimaryHand(), 0.5F, false);
        d.fight = SovereignData.Fight.WON;
        d.fightSettled = StoryDirector.now();
        d.magusHasPower = false;
        d.magusHasMind = false;
        d.markDirty();
        act = Act.OVER;
        if (m != null) {
            dust(p, m);
        }
        if (odin != null && !odin.isDead) {
            odinWithdraws(p, odin);
        }
        story.sendStateToAll();
        return true;
    }

    /** He looks at his hand, and it is going; and then all of him. */
    private void dust(EntityPlayerMP p, EntityMagus m) {
        playing.clear();
        m.boss(false);
        m.guard(true);
        m.clearShots();
        m.stand();
        m.holdGauntlet(ItemStack.EMPTY, EntityMagus.HOLD_NONE);
        m.rest(Pose.STAND).perform(Pose.STAND);
        m.look(m.getPositionVector().add(0D, 1.2D, 0D).add(flat(m.getLookVec()).scale(0.5D)));
        Timeline t = clash = new Timeline();
        WorldServer ws = p.getServerWorld();
        int sand = net.minecraft.block.Block.getStateId(net.minecraft.init.Blocks.SAND.getDefaultState());
        t.at(10, () -> StoryDirector.shout(m, MAGUS, m.getEntityId(), "I... was the hand. I was always the...", 96D));
        t.span(10, 90, k -> {
            Vec3d c = m.getPositionVector().add(0D, 0.4D + 1.8D * (1F - k), 0D);
            ws.spawnParticle(EnumParticleTypes.FALLING_DUST, c.x, c.y, c.z, 10, 0.35D, 0.3D, 0.35D, 0D, sand);
            ws.spawnParticle(EnumParticleTypes.SPELL_WITCH, c.x, c.y, c.z, 2, 0.3D, 0.3D, 0.3D, 0D);
        });
        t.at(30, () -> sound(m.getPositionVector(), SoundEvents.BLOCK_SAND_BREAK, 2F, 0.6F));
        t.at(90, () -> {
            m.setDead();
            magus = null;
            clash = null;
            EntityAdamWarlock w = warlock;
            if (w != null && !w.isDead) {
                w.script(true);
                w.direct(EntityAdamWarlock.State.HOVER, p.getPositionVector().add(flat(p.getLookVec()).scale(3D)).add(0D, 0.8D, 0D),
                        p.getPositionEyes(1F), 0.1D);
                StoryDirector.shout(w, WARLOCK, w.getEntityId(), "He's gone. All of him.", 160D);
            }
        });
        t.at(190, () -> {
            EntityAdamWarlock w = warlock;
            if (w != null && !w.isDead) {
                StoryDirector.shout(w, WARLOCK, w.getEntityId(), "Six, on one hand. Be careful what you close it on.", 160D);
            }
        });
        t.at(300, () -> {
            if (warlock != null) {
                warlock.dismiss();
                warlock = null;
            }
        });
    }

    /** Called every tick even once it is won, so the dust and Warlock's going play out. */
    void tickAfter() {
        if (data().fight == SovereignData.Fight.WON && clash != null && !clash.tick()) {
            clash = null;
        }
        for (Timeline t : new ArrayList<>(exits)) {
            if (!t.tick()) {
                exits.remove(t);
            }
        }
    }

    // ------------------------------------------------------------------ Odin

    /** Odin, as the stones are claimed for him. */
    static final java.util.UUID ODIN_ID = java.util.UUID.nameUUIDFromBytes("eternity:odin".getBytes(java.nio.charset.StandardCharsets.UTF_8));

    /** Whether Odin is due: once, the second time the Magus's health runs out. */
    private boolean odinDue() {
        return EternityConfig.odin.enabled && !data().odinCame && odin == null;
    }

    /** Things go dark round him, or lift, over so many ticks; and the music with it (0 none, 1 Prisms, 2 his). */
    private static void odinDark(int dim, float dark, int over, int music) {
        EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.ODIN_DARK, 0D, 0D, 0D, dark, over, music, -1, 0), dim);
    }

    private static void odinFx(int dim, byte type, Vec3d at, double a, double b, double c, int id, int time) {
        EternityNetwork.sendToDimension(new MsgTribunalFx(type, at.x, at.y, at.z, a, b, c, id, time), dim);
    }

    private static void soundAt(net.minecraft.world.World world, Vec3d at, SoundEvent sound, float volume, float pitch) {
        world.playSound(null, at.x, at.y, at.z, sound, SoundCategory.HOSTILE, volume, pitch);
    }

    /**
     * Odin comes. The light goes out of the sky and it opens over them, wheeling; the Bifrost slams down out of it, and
     * down it at a run, on Sleipnir, comes the Allfather, woken. He has a word for each of them, and then it is three
     * ways: he means to have the stones, and neither of them alive after.
     */
    private void odinComes(EntityPlayerMP p, EntityMagus m) {
        SovereignData d = data();
        act(Act.ODIN);
        d.odinCame = true;
        d.markDirty();
        grabbing = -1;
        m.stand();
        m.guard(true);
        m.clearShots();
        WorldServer world = p.getServerWorld();
        int dim = m.dimension;
        EntityAdamWarlock w = warlock != null && !warlock.isDead && !warlock.fallen() ? warlock : null;
        if (w != null) {
            w.script(true);
            w.direct(EntityAdamWarlock.State.HOVER, w.getPositionVector(), m.getPositionEyes(1F), 0.1D);
        }
        // Where he comes down: off to one side of the two of them, in the open.
        Vec3d mid = p.getPositionVector().add(m.getPositionVector()).scale(0.5D);
        Vec3d across = flat(m.getPositionVector().subtract(p.getPositionVector()));
        Vec3d side = new Vec3d(-across.z, 0D, across.x);
        Vec3d spot = mid.add(side.scale(9D));
        Vec3d land = new Vec3d(spot.x, groundUnder(world, spot.add(0D, 6D, 0D)), spot.z);
        Vec3d sky = land.add(0D, 60D, 0D);
        // He is there from the start, out of sight high in the dark, so the scene can give him his lines.
        EntityOdin od = odin = new EntityOdin(world);
        od.setPosition(sky.x, sky.y + 40D, sky.z);
        od.setInvisible(true);
        world.spawnEntity(od);
        od.guard(true);
        Talk t = new Talk(p).at(m, 160D).reach(96D);
        t.abandoned(() -> endOdinComes(p, od));
        // Timed to the camera (StoryClient.ODIN, ODIN_TICKS long, about 20 ticks behind): the Bifrost at about 80, him down
        // at 128, his first words at 168, the last at about 600.
        t.run(() -> {
                    odinDark(dim, 0.85F, 110, 1);
                    odinFx(dim, MsgTribunalFx.ODIN_VORTEX, land, 30D, 70D, 0D, -1, 230);
                    soundAt(world, land, SoundEvents.ENTITY_LIGHTNING_THUNDER, 4F, 0.4F);
                    m.look(sky);
                    story.scene(lead(t, p), ODIN_SCENE, land, m.getPositionVector());
                })
                .pause(50)
                .run(() -> {
                    soundAt(world, land, SoundEvents.ENTITY_LIGHTNING_THUNDER, 4F, 0.55F);
                    soundAt(world, land, SoundEvents.ENTITY_LIGHTNING_IMPACT, 2F, 0.4F);
                })
                .pause(50)
                .run(() -> {
                    // The Bifrost.
                    odinFx(dim, MsgTribunalFx.ODIN_BIFROST, land, 3.2D, 1D, 0D, -1, 110);
                    odinFx(dim, MsgTribunalFx.ODIN_SEAL, land, 7.5D, 0D, 0D, -1, 2400);
                    odinFx(dim, MsgTribunalFx.ODIN_FLARE, land.add(0D, 1D, 0D), 4D, 0D, 0D, -1, 0);
                    soundAt(world, land, SoundEvents.ENTITY_GENERIC_EXPLODE, 4F, 0.5F);
                    for (EntityPlayerMP near : world.getPlayers(EntityPlayerMP.class, e -> e.getDistanceSq(land.x, land.y, land.z) < 12D * 12D)) {
                        Vec3d away = flat(near.getPositionVector().subtract(land));
                        com.matoon.eternity.entity.EntityDestroyerArmor.shove(near, away.x * 1.2D, 0.5D, away.z * 1.2D);
                    }
                    od.setInvisible(false);
                    od.place(sky, lead(t, p).getPositionVector());
                    od.rest(EntityOdin.Pose.DESCEND).perform(EntityOdin.Pose.DESCEND);
                    playing.add(descend(od, sky, land, lead(t, p)));
                })
                .pause(48)
                .run(() -> m.look(od.getPositionEyes(1F)))
                .pause(40)
                .run(() -> od.look(lead(t, p).getPositionEyes(1F)))
                .line(Speakers.ODIN, od, "I slept, and woke to an empty vault.")
                .run(() -> od.look(m.getPositionEyes(1F)))
                .line(MAGUS, m, "Go back to sleep, old man.")
                .line(Speakers.ODIN, od, "I have buried universes for less.")
                .run(() -> {
                    od.watch(lead(t, p));
                    od.rest(EntityOdin.Pose.STAND).perform(EntityOdin.Pose.POINT, 130);
                })
                .line(Speakers.ODIN, od, "The stones come home with me.")
                .run(() -> od.watch(m))
                .line(Speakers.ODIN, od, "Neither of you does.")
                .until(() -> story.sceneDone(p, ODIN_SCENE), 100)
                .run(() -> endOdinComes(p, od));
        story.talk(p, t);
    }

    /** Down the Bifrost at a run, and into the ground at its foot, Sleipnir rearing. */
    private Timeline descend(EntityOdin od, Vec3d sky, Vec3d land, EntityPlayerMP p) {
        Timeline t = new Timeline();
        int dim = od.dimension;
        t.span(0, 40, k -> {
            float e = 1F - (1F - k) * (1F - k);
            od.place(sky.add(land.subtract(sky).scale(e)), p.getPositionVector());
        });
        t.at(40, () -> {
            od.rest(EntityOdin.Pose.STAND).perform(EntityOdin.Pose.REAR, EntityOdin.REAR_TICKS);
            odinFx(dim, MsgTribunalFx.ODIN_STOMP, land, 8D, 0D, 0D, 0, 0);
            soundAt(od.world, land, SoundEvents.ENTITY_HORSE_ANGRY, 4F, 0.45F);
            soundAt(od.world, land, SoundEvents.BLOCK_ANVIL_LAND, 2F, 0.4F);
            soundAt(od.world, land, SoundEvents.ENTITY_GENERIC_EXPLODE, 3F, 0.6F);
        });
        return t;
    }

    /** It is three ways now: him at all of them, his bar up, the music his. */
    private void endOdinComes(EntityPlayerMP p, EntityOdin od) {
        if (act != Act.ODIN) {
            return;
        }
        // Whoever it is being fought with now (whom it began with may be gone from it).
        EntityPlayerMP now = StoryDirector.player(data().fightPlayer);
        if (now != null) {
            p = now;
        }
        EntityMagus m = magus;
        if (!od.isDead) {
            od.setInvisible(false);
            od.guard(false);
            od.stand();
            od.fight(p, m, warlock);
            od.boss(true);
            if (m != null) {
                m.rival(od);
            }
            odinDark(od.dimension, 0.85F, 20, 2);
        }
        if (m != null) {
            m.guard(false);
        }
        EntityAdamWarlock w = warlock;
        if (w != null && !w.isDead && !w.fallen()) {
            w.script(false);
            if (m != null) {
                w.turnOn(m);
            }
        }
        landAt = null;
        act(hisNow() ? Act.FLEE : Act.HUNT);
        story.sendState(p);
    }

    /** Lost sight of them (they went, and came back): he comes down again, without a word, and goes on with it. */
    private EntityOdin odinReturns(EntityPlayerMP p, EntityMagus m) {
        WorldServer world = p.getServerWorld();
        int dim = p.dimension;
        Vec3d spot = p.getPositionVector().add(flat(p.getLookVec()).scale(14D));
        Vec3d land = new Vec3d(spot.x, groundUnder(world, spot.add(0D, 6D, 0D)), spot.z);
        odinFx(dim, MsgTribunalFx.ODIN_BIFROST, land, 3D, 1D, 0D, -1, 50);
        odinFx(dim, MsgTribunalFx.ODIN_SEAL, land, 6D, 0D, 0D, -1, 1200);
        EntityOdin od = new EntityOdin(world);
        od.setPosition(land.x, land.y, land.z);
        world.spawnEntity(od);
        od.place(land, p.getPositionVector());
        od.rest(EntityOdin.Pose.STAND).perform(EntityOdin.Pose.REAR, EntityOdin.REAR_TICKS);
        od.fight(p, m, warlock);
        od.boss(true);
        m.rival(od);
        odinDark(dim, 0.85F, 40, 2);
        return od;
    }

    /**
     * Fallen with Odin in it: not dead, not yet. He has what he came for. Called before they die; true if they are
     * spared for it, and the end is his.
     */
    boolean spare(EntityPlayerMP p) {
        SovereignData d = data();
        EntityOdin od = odin;
        if (!EternityConfig.odin.canWin || d.fight != SovereignData.Fight.FIGHTING || od == null || od.isDead || !od.isFighting()
                || act == Act.ODIN || act == Act.TAKEN || act == Act.AFTERMATH) {
            return false;
        }
        boolean theirs = p.getUniqueID().equals(d.fightPlayer);
        if (!theirs && !near(p, od, 96D) && !near(p, magus, 96D)) {
            return false;
        }
        p.setHealth(2F);
        p.extinguish();
        p.addPotionEffect(new net.minecraft.potion.PotionEffect(net.minecraft.init.MobEffects.RESISTANCE, 720, 4, false, false));
        p.addPotionEffect(new net.minecraft.potion.PotionEffect(net.minecraft.init.MobEffects.SLOWNESS, 720, 3, false, false));
        p.addPotionEffect(new net.minecraft.potion.PotionEffect(net.minecraft.init.MobEffects.WEAKNESS, 720, 2, false, false));
        EntityPlayerMP next = next(p);
        if (next != null) {
            // Someone else still on their feet: they lie where they fell, out of it, and it goes on without them.
            felled.put(p.getUniqueID(), StoryDirector.now() + 720L);
            fallsOut(p, next);
            return true;
        }
        // The last of them down. It ends over whoever it was being fought for, if they are still there to see it.
        EntityPlayerMP over = theirs ? p : StoryDirector.player(d.fightPlayer);
        if (over == null || !over.isEntityAlive() || !near(over, od, 96D) || marked(p) != -1 || isMarked(p.inventory.getItemStack())) {
            over = p;
        }
        felled.put(p.getUniqueID(), StoryDirector.now() + 720L);
        odinWins(over, od);
        return true;
    }

    /** Whoever leads a scene now: whom it began with, unless they have gone from it. */
    private static EntityPlayerMP lead(Talk t, EntityPlayerMP p) {
        EntityPlayerMP l = t.leadNow();
        return l != null ? l : p;
    }

    /** Whether someone is within so far of something, in the same world. */
    private static boolean near(EntityPlayerMP p, @Nullable Entity e, double range) {
        return e != null && !e.isDead && p.world == e.world && p.getDistance(e) < range;
    }

    /**
     * Odin has beaten them both. They are down. The Magus comes at him for it, and Gungnir goes through him; he unravels.
     * The gauntlet comes to Odin's hand from wherever it is, and he looks down at them, says one thing, and the Bifrost
     * takes him up with it held against him. Nobody has it now but Asgard.
     */
    private void odinWins(EntityPlayerMP p, EntityOdin od) {
        SovereignData d = data();
        EntityMagus m = magus != null && !magus.isDead ? magus : null;
        if (act == Act.STRUGGLE && m != null) {
            EternityNetwork.send(new MsgGauntletFight(MsgGauntletFight.END, m.getEntityId(), 0), p);
        }
        act(Act.TAKEN);
        WorldServer world = p.getServerWorld();
        int dim = od.dimension;
        // Wherever the gauntlet is: in his hands, on them, lying where it fell.
        ItemStack prize = ItemStack.EMPTY;
        Vec3d prizeAt = p.getPositionVector().add(0D, 1D, 0D);
        int prizeFrom = -1;
        Entity lying = null;
        if (hisNow()) {
            prize = d.fightGauntlet.copy();
            if (m != null) {
                prizeAt = m.gauntletAt();
                prizeFrom = m.getEntityId();
            }
        } else {
            int slot = marked(p);
            if (slot != -1) {
                prize = takeFrom(p, slot);
                prizeFrom = p.getEntityId();
            } else if (isMarked(p.inventory.getItemStack())) {
                prize = p.inventory.getItemStack().copy();
                p.inventory.setItemStack(ItemStack.EMPTY);
                p.updateHeldItem();
                prizeFrom = p.getEntityId();
            } else {
                lying = lyingAbout(p);
                if (lying != null) {
                    prize = itemOf(lying).copy();
                    prizeAt = lying.getPositionVector();
                }
            }
        }
        for (int i = 0; i < p.inventory.mainInventory.size(); i++) {
            if (isMarked(p.inventory.mainInventory.get(i))) {
                p.inventory.mainInventory.set(i, ItemStack.EMPTY);
            }
        }
        if (isMarked(p.inventory.offHandInventory.get(0))) {
            p.inventory.offHandInventory.set(0, ItemStack.EMPTY);
        }
        p.inventoryContainer.detectAndSendChanges();
        if (prize.isEmpty()) {
            prize = new ItemStack(ModuleInfinity.INFINITY_GAUNTLET);
            prize.setTagCompound(new NBTTagCompound());
        }
        unmark(prize);
        claimForOdin(prize);
        ItemStack his = prize;
        d.fight = SovereignData.Fight.TAKEN;
        d.fightSettled = StoryDirector.now();
        d.fightGauntlet = ItemStack.EMPTY;
        d.giveBack = ItemStack.EMPTY;
        d.magusHasPower = false;
        d.magusHasMind = false;
        d.markDirty();
        od.stand();
        od.guard(true);
        od.boss(false);
        EntityAdamWarlock w = warlock;
        if (w != null && !w.isDead && !w.fallen()) {
            // Swept out of it, and gone.
            w.knocked(flat(w.getPositionVector().subtract(od.getPositionVector())), 2.5D, 1.2D);
            w.dismiss();
        }
        warlock = null;
        warlockDue = -1;
        // He stands over them.
        Vec3d toward = flat(od.getPositionVector().subtract(p.getPositionVector()));
        Vec3d over = p.getPositionVector().add(toward.scale(4.2D));
        Vec3d stand = new Vec3d(over.x, groundUnder(world, over.add(0D, 4D, 0D)), over.z);
        Vec3d prizeWas = prizeAt;
        int from = prizeFrom;
        Entity lay = lying;
        Timeline[] pinned = new Timeline[1];
        boolean[] done = new boolean[3];
        Talk t = new Talk(p).reach(96D);
        Runnable unravel = () -> {
            if (done[0]) {
                return;
            }
            done[0] = true;
            if (pinned[0] != null) {
                pinned[0].stop();
            }
            EntityMagus mm = magus;
            if (mm != null && !mm.isDead) {
                Vec3d c = mm.getPositionVector().add(0D, 1.2D, 0D);
                odinFx(dim, MsgTribunalFx.ODIN_DUST, c, 0D, 0D, 0D, mm.getEntityId(), 60);
                EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.MAGUS_BREACH, c.x, c.y, c.z, 2D, 0D, 0, 0, 40), dim);
                soundAt(world, c, SoundEvents.BLOCK_GLASS_BREAK, 2F, 0.4F);
                soundAt(world, c, SoundEvents.ENTITY_WITHER_DEATH, 0.8F, 1.6F);
                mm.setDead();
            }
            magus = null;
            playing.add(spearHome(od, prizeWas));
        };
        Runnable take = () -> {
            if (done[1]) {
                return;
            }
            done[1] = true;
            odinFx(dim, MsgTribunalFx.ODIN_PULL, prizeWas, from, 0D, 0D, od.getEntityId(), 30);
            soundAt(world, prizeWas, SoundEvents.BLOCK_END_PORTAL_FRAME_FILL, 2F, 0.6F);
            if (lay != null) {
                lay.setDead();
            }
        };
        Runnable hold = () -> {
            od.holdGauntlet(his);
            od.rest(EntityOdin.Pose.CRADLE).perform(EntityOdin.Pose.CRADLE);
            od.look(p.getPositionEyes(1F));
            soundAt(world, od.getPositionVector(), SoundEvents.ENTITY_ITEM_PICKUP, 1.5F, 0.5F);
        };
        Runnable away = () -> {
            if (done[2]) {
                return;
            }
            done[2] = true;
            playing.add(bifrostAway(od, () -> {
                odinDark(dim, 0F, 400, 1);
                // The music lingers a while after the dark has gone, then goes quiet too.
                Timeline quiet = new Timeline();
                quiet.at(1400, () -> odinDark(dim, 0F, 1, 0));
                playing.add(quiet);
            }));
        };
        t.abandoned(() -> {
            unravel.run();
            take.run();
            hold.run();
            away.run();
        });
        // Timed to the camera (StoryClient.ODIN_WIN, ODIN_WIN_TICKS long, about 20 ticks behind).
        t.run(() -> {
                    od.place(stand, p.getPositionVector());
                    od.rest(EntityOdin.Pose.STAND).perform(EntityOdin.Pose.REAR, 26);
                    soundAt(world, stand, SoundEvents.ENTITY_HORSE_ANGRY, 4F, 0.5F);
                    if (m != null) {
                        m.boss(false);
                        m.guard(true);
                        m.clearShots();
                        m.stand();
                        // He comes for it, for Odin, one last time.
                        Vec3d at = stand.add(flat(m.getPositionVector().subtract(stand)).scale(7D)).add(0D, 1.5D, 0D);
                        m.hoverTo(at, 0.12D);
                        m.watch(od);
                        m.rest(Pose.GUARD).perform(Pose.GUARD);
                    }
                    story.scene(lead(t, p), ODIN_WIN_SCENE, p.getPositionVector(), stand);
                })
                .pause(40)
                .run(() -> {
                    EntityMagus mm = magus;
                    if (mm != null && !mm.isDead) {
                        pinned[0] = impale(od, mm);
                        playing.add(pinned[0]);
                    }
                })
                .pause(30);
        if (m != null) {
            t.line(MAGUS, m, "I don't remember this.");
        }
        t.line(Speakers.ODIN, od, "Every ending you remember was mine to allow.")
                .run(unravel)
                .pause(30)
                .run(take)
                .pause(34)
                .run(hold)
                .pause(16)
                .line(Speakers.ODIN, od, "Sleep. When you wake, the stones will be where they belong.")
                .run(away)
                .pause(60)
                .until(() -> story.sceneDone(p, ODIN_WIN_SCENE), 200)
                .run(() -> {
                    p.removePotionEffect(net.minecraft.init.MobEffects.SLOWNESS);
                    p.removePotionEffect(net.minecraft.init.MobEffects.WEAKNESS);
                    // Everyone else he put down, up again too.
                    for (java.util.UUID id : felled.keySet()) {
                        EntityPlayerMP o = StoryDirector.player(id);
                        if (o != null && o != p) {
                            o.removePotionEffect(net.minecraft.init.MobEffects.SLOWNESS);
                            o.removePotionEffect(net.minecraft.init.MobEffects.WEAKNESS);
                        }
                    }
                    felled.clear();
                    story.sendStateToAll();
                });
        story.talk(p, t);
        story.sendStateToAll();
    }

    /** Gungnir thrown through him, and left there: he hangs on it, arched, in the air. */
    private Timeline impale(EntityOdin od, EntityMagus m) {
        Timeline t = new Timeline();
        int dim = od.dimension;
        int id = 90000;
        Vec3d[] from = new Vec3d[1], dir = {new Vec3d(0D, 0D, 1D)};
        t.at(0, () -> {
            od.faceNow(m.getPositionVector());
            od.rest(EntityOdin.Pose.STAND).perform(EntityOdin.Pose.THROW, 0);
            soundAt(od.world, od.getPositionVector(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 2F, 0.5F);
        });
        t.at(EntityOdin.THROW_RELEASE, () -> {
            od.spear(false);
            from[0] = od.getPositionVector().add(0D, EntityOdin.HAND_HEIGHT * EntityOdin.SCALE * 0.95D, 0D);
            soundAt(od.world, from[0], SoundEvents.ENTITY_WITHER_SHOOT, 2F, 0.6F);
            soundAt(od.world, from[0], EternitySounds.COMET_FLYBY, 3F, 1.4F);
        });
        t.span(EntityOdin.THROW_RELEASE, EntityOdin.THROW_RELEASE + 5, k -> {
            if (from[0] == null) {
                return;
            }
            Vec3d to = m.getPositionVector().add(0D, 1.3D, 0D);
            dir[0] = to.subtract(from[0]).normalize();
            odinFx(dim, MsgTribunalFx.ODIN_SPEAR, from[0].add(to.subtract(from[0]).scale(k)), dir[0].x * 1.7D, dir[0].y * 1.7D, dir[0].z * 1.7D, id, 0);
        });
        t.at(EntityOdin.THROW_RELEASE + 6, () -> {
            Vec3d c = m.getPositionVector().add(0D, 1.3D, 0D);
            odinFx(dim, MsgTribunalFx.ODIN_FLARE, c, 1.6D, 0D, 0D, -1, 0);
            EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.MAGUS_IMPACT, c.x, c.y, c.z, dir[0].x, dir[0].y, dir[0].z, m.getEntityId(), 2), dim);
            soundAt(od.world, c, SoundEvents.ENTITY_IRONGOLEM_ATTACK, 3F, 0.5F);
            soundAt(od.world, c, SoundEvents.ENTITY_LIGHTNING_THUNDER, 2F, 1.2F);
            soundAt(od.world, c, SoundEvents.ENTITY_ENDERDRAGON_HURT, 2F, 0.5F);
            m.rest(Pose.FLUNG).perform(Pose.FLUNG);
            m.hoverTo(m.getPositionVector().add(dir[0].scale(1.6D)), 0.2D);
        });
        // Left in him, through him, every tick until he is gone.
        t.span(EntityOdin.THROW_RELEASE + 7, EntityOdin.THROW_RELEASE + 400, k -> {
            if (!m.isDead) {
                Vec3d c = m.getPositionVector().add(0D, 1.3D, 0D).add(dir[0].scale(1.2D));
                odinFx(dim, MsgTribunalFx.ODIN_SPEAR, c, dir[0].x * 1.7D, dir[0].y * 1.7D, dir[0].z * 1.7D, id, 0);
            }
        });
        return t;
    }

    /** Gungnir drawn back out of where it was, home to his hand. */
    private Timeline spearHome(EntityOdin od, Vec3d from) {
        Timeline t = new Timeline();
        int dim = od.dimension;
        int id = 90000;
        t.span(0, 8, k -> {
            Vec3d hand = od.getPositionVector().add(0D, EntityOdin.HAND_HEIGHT * EntityOdin.SCALE * 0.95D, 0D);
            Vec3d at = from.add(hand.subtract(from).scale(k));
            Vec3d d = hand.subtract(from).normalize().scale(2D);
            odinFx(dim, MsgTribunalFx.ODIN_SPEAR, at, d.x, d.y, d.z, id, 1);
        });
        t.at(9, () -> {
            od.spear(true);
            od.perform(EntityOdin.Pose.CATCH, 0);
            soundAt(od.world, od.getPositionVector(), SoundEvents.ENTITY_IRONGOLEM_ATTACK, 1.2F, 1.4F);
        });
        return t;
    }

    /** The Bifrost round him, and him up it at a run, into the sky and gone; then whatever comes after. */
    private Timeline bifrostAway(EntityOdin od, @Nullable Runnable after) {
        Timeline t = new Timeline();
        int dim = od.dimension;
        Vec3d[] from = new Vec3d[1];
        t.at(0, () -> {
            from[0] = od.getPositionVector();
            odinFx(dim, MsgTribunalFx.ODIN_BIFROST, from[0], 3D, -1D, 0D, -1, 64);
            odinFx(dim, MsgTribunalFx.ODIN_FLARE, from[0].add(0D, 1.5D, 0D), 3D, 0D, 0D, -1, 0);
            od.stand();
            od.guard(true);
            od.boss(false);
            od.rest(EntityOdin.Pose.STAND).perform(EntityOdin.Pose.REAR, 0);
            soundAt(od.world, from[0], SoundEvents.ENTITY_HORSE_ANGRY, 4F, 0.5F);
        });
        t.at(14, () -> {
            od.rest(EntityOdin.Pose.GALLOP).perform(EntityOdin.Pose.GALLOP);
            soundAt(od.world, from[0], EternitySounds.COMET_FLYBY, 5F, 0.6F);
        });
        t.span(14, 40, k -> {
            if (from[0] != null) {
                od.place(from[0].add(0D, 110D * k * k, 0D), null);
            }
        });
        t.at(41, () -> {
            od.setDead();
            if (odin == od) {
                odin = null;
            }
            if (after != null) {
                after.run();
            }
        });
        return t;
    }

    /** It is Odin's: all six, the Magus's two with the rest, claimed for him. */
    private static void claimForOdin(ItemStack gauntlet) {
        if (!gauntlet.hasTagCompound()) {
            gauntlet.setTagCompound(new NBTTagCompound());
        }
        InventoryInfinityGauntlet sockets = new InventoryInfinityGauntlet(gauntlet);
        for (int i = 0; i < sockets.getSizeInventory(); i++) {
            ItemStack stone = sockets.getStackInSlot(i);
            if (stone.isEmpty()) {
                stone = InfinityStones.stack(InventoryInfinityGauntlet.SLOTS[i].ordinal());
            }
            Relics.Kind kind = stone.isEmpty() ? null : Relics.kind(stone);
            if (kind != null) {
                Relics.release(kind);
                Relics.claim(stone, ODIN_ID, "Odin");
                sockets.setInventorySlotContents(i, stone);
            }
        }
        sockets.markDirty();
    }

    /** The child closes its hand: Odin watches the Magus go to dust, and has a word for them, and goes. */
    private void odinWithdraws(EntityPlayerMP p, EntityOdin od) {
        od.stand();
        od.guard(true);
        od.boss(false);
        od.look(p.getPositionEyes(1F));
        int dim = od.dimension;
        Timeline t = new Timeline();
        t.at(100, () -> {
            od.watch(p);
            StoryDirector.shout(od, Speakers.ODIN, od.getEntityId(), "So. You can close your hand.", 160D);
        });
        t.at(210, () -> StoryDirector.shout(od, Speakers.ODIN, od.getEntityId(), "Then I will wait. I have outwaited older things than you.", 160D));
        t.at(340, () -> exits.add(bifrostAway(od, () -> odinDark(dim, 0F, 200, 0))));
        exits.add(t);
    }

    /** The Magus's snap: everything living about them, half of it, goes; the Allfather with it. */
    private void odinCrumbles(int dim) {
        EntityOdin od = odin;
        if (od == null || od.isDead) {
            return;
        }
        odinFx(dim, MsgTribunalFx.ODIN_DUST, od.getPositionVector(), 0D, 0D, 0D, od.getEntityId(), 80);
        com.matoon.eternity.stones.SnapPowers.crumble((WorldServer) od.world, od);
        od.setDead();
        odin = null;
        odinDark(dim, 0F, 90, 0);
    }

    // ------------------------------------------------------------------ helpers

    /** Him, put back where they are: unfolding a little way off. */
    private EntityMagus spawnNear(EntityPlayerMP p) {
        EntityMagus m = new EntityMagus(p.world);
        Vec3d at = p.getPositionVector().add(flat(p.getLookVec()).scale(8D)).add(0D, 1D, 0D);
        m.setPosition(at.x, at.y, at.z);
        p.world.spawnEntity(m);
        m.blink(at, p.getPositionEyes(1F), true);
        m.crown(true);
        m.boss(true);
        if (hisNow()) {
            m.holdGauntlet(data().fightGauntlet, EntityMagus.HOLD_CRADLE);
            act(Act.FLEE);
            landAt = null;
        } else {
            act(Act.HUNT);
        }
        return m;
    }

    private static Vec3d flat(Vec3d v) {
        Vec3d f = new Vec3d(v.x, 0D, v.z);
        return f.lengthSquared() < 1.0E-6 ? new Vec3d(0D, 0D, 1D) : f.normalize();
    }

    private void sound(Vec3d at, SoundEvent sound, float volume, float pitch) {
        if (magus != null) {
            magus.world.playSound(null, at.x, at.y, at.z, sound, SoundCategory.HOSTILE, volume, pitch);
        }
    }

    private void fx(byte type, Vec3d at, Vec3d dir, int kind) {
        if (magus != null) {
            EternityNetwork.sendToDimension(new MsgTribunalFx(type, at.x, at.y, at.z, dir.x, dir.y, dir.z, -1, kind), magus.dimension);
        }
    }

    // ------------------------------------------------------------------ for testing

    /**
     * For starting the fight at once: every stone they have, and the rest of the four of this road, set into one
     * gauntlet in their inventory, so they are ready. Stones they already carry are moved in, keeping their claims.
     */
    private static void equip(EntityPlayerMP player) {
        ItemStack g = new ItemStack(ModuleInfinity.INFINITY_GAUNTLET);
        g.setTagCompound(new NBTTagCompound());
        InventoryInfinityGauntlet sockets = new InventoryInfinityGauntlet(g);
        for (int i = 0; i < sockets.getSizeInventory(); i++) {
            EnumInfinityStone type = InventoryInfinityGauntlet.SLOTS[i];
            ItemStack stone = InfinityStones.take(player, type);
            if (stone.isEmpty() && type != EnumInfinityStone.POWER && type != EnumInfinityStone.MIND) {
                stone = InfinityStones.stack(type.ordinal());
                Relics.Kind kind = Relics.kind(stone);
                if (kind != null) {
                    Relics.release(kind);
                    Relics.claim(stone, player);
                }
            }
            if (!stone.isEmpty()) {
                sockets.setInventorySlotContents(i, stone);
            }
        }
        sockets.markDirty();
        net.minecraftforge.items.ItemHandlerHelper.giveItemToPlayer(player, g);
        player.inventoryContainer.detectAndSendChanges();
    }

    @Nullable
    String command(EntityPlayerMP player, String[] args) {
        String what = args.length > 0 ? args[0] : "status";
        SovereignData d = data();
        switch (what) {
            case "give": {
                // A gauntlet with the four stones of the Magus's road in it, theirs.
                ItemStack g = new ItemStack(ModuleInfinity.INFINITY_GAUNTLET);
                g.setTagCompound(new NBTTagCompound());
                InventoryInfinityGauntlet sockets = new InventoryInfinityGauntlet(g);
                for (int i = 0; i < sockets.getSizeInventory(); i++) {
                    EnumInfinityStone type = InventoryInfinityGauntlet.SLOTS[i];
                    if (type == EnumInfinityStone.POWER || type == EnumInfinityStone.MIND) {
                        continue;
                    }
                    ItemStack stone = InfinityStones.stack(type.ordinal());
                    Relics.Kind kind = Relics.kind(stone);
                    if (kind != null) {
                        Relics.release(kind);
                        Relics.claim(stone, player);
                    }
                    sockets.setInventorySlotContents(i, stone);
                }
                sockets.markDirty();
                net.minecraftforge.items.ItemHandlerHelper.giveItemToPlayer(player, g);
                return "A gauntlet with Space, Soul, Reality and Time in it.";
            }
            case "odin": {
                // Odin, now: down the Bifrost into the fight under way.
                if (!on() || magus == null) {
                    return "No fight on.";
                }
                d.odinCame = false;
                if (odin != null) {
                    odin.setDead();
                    odin = null;
                }
                act(Act.HUNT);
                odinComes(player, magus);
                return "The Allfather comes.";
            }
            case "odinwin": {
                // As if they had fallen with Odin in it.
                if (odin == null) {
                    return "Odin isn't here.";
                }
                return spare(player) ? "He has it." : "Not now.";
            }
            case "start": {
                if (on()) {
                    lost(player);
                }
                d.fight = SovereignData.Fight.NONE;
                if (ready(player) == -1) {
                    equip(player);
                }
                markOnly(player, ready(player));
                EntityMagus m = new EntityMagus(player.world);
                Vec3d at = player.getPositionVector().add(flat(player.getLookVec()).scale(8D)).add(0D, 1D, 0D);
                m.setPosition(at.x, at.y, at.z);
                player.world.spawnEntity(m);
                m.blink(at, player.getPositionEyes(1F), true);
                begin(player, m, null, false);
                return "The Magus is here. Warlock is on his way.";
            }
            case "ambush":
                d.fight = SovereignData.Fight.HUNTING;
                d.fightPlayer = player.getUniqueID();
                d.nextAmbush = 0L;
                d.markDirty();
                return "He will come for you as soon as you are ready.";
            case "grab": {
                d.fight = SovereignData.Fight.NONE;
                if (!raised(player)) {
                    return "Hold the ready gauntlet in your hand.";
                }
                EntityMagus g = new EntityMagus(player.world);
                g.setPosition(player.posX, player.posY + 6D, player.posZ);
                player.world.spawnEntity(g);
                snatch(player, g, null);
                warlockDue = StoryDirector.now() + 60L;
                return "He takes it.";
            }
            case "land":
                if (!on() || magus == null || !hisNow()) {
                    return "He hasn't got it.";
                }
                landAt = magus.getPositionVector().add(flat(magus.getLookVec()).scale(4D));
                landAt = new Vec3d(landAt.x, Barrow.surface(magus.world, MathHelper.floor(landAt.x), MathHelper.floor(landAt.z)).getY(), landAt.z);
                from = magus.getPositionVector();
                act(Act.LAND);
                return "He comes down.";
            case "power":
                if (!on() || magus == null || !hisNow()) {
                    return "He hasn't got it.";
                }
                setInto(d.fightGauntlet, EnumInfinityStone.POWER.ordinal());
                setInto(d.fightGauntlet, EnumInfinityStone.MIND.ordinal());
                magus.holdGauntlet(d.fightGauntlet, EntityMagus.HOLD_WORN);
                startPower(player, magus);
                return "It comes up through him.";
            case "look": {
                // Him standing still in front of you with it, held one way, in one pose, for looking at.
                String how = args.length > 1 ? args[1] : "worn";
                ItemStack g = new ItemStack(ModuleInfinity.INFINITY_GAUNTLET);
                g.setTagCompound(new NBTTagCompound());
                InventoryInfinityGauntlet sockets = new InventoryInfinityGauntlet(g);
                for (int i = 0; i < sockets.getSizeInventory(); i++) {
                    sockets.setInventorySlotContents(i, InfinityStones.stack(InventoryInfinityGauntlet.SLOTS[i].ordinal()));
                }
                sockets.markDirty();
                if (looked != null) {
                    looked.setDead();
                }
                EntityMagus m = looked = new EntityMagus(player.world);
                Vec3d at = player.getPositionVector().add(flat(player.getLookVec()).scale(3.5D));
                m.setPosition(at.x, player.posY, at.z);
                player.world.spawnEntity(m);
                m.place(new Vec3d(at.x, player.posY, at.z), player.getPositionEyes(1F));
                int from = args.length > 2 ? Integer.parseInt(args[2]) : 0;
                switch (how) {
                    case "cradle":
                        m.holdGauntlet(g, EntityMagus.HOLD_CRADLE);
                        m.rest(Pose.CRADLE).perform(Pose.CRADLE);
                        break;
                    case "slot":
                        m.holdGauntlet(g, EntityMagus.HOLD_HAND);
                        m.holdAs(EntityMagus.HOLD_HAND, EnumInfinityStone.MIND.ordinal());
                        m.rest(Pose.SLOT).resume(Pose.SLOT, from);
                        break;
                    case "power":
                        m.holdGauntlet(g, EntityMagus.HOLD_WORN);
                        m.rest(Pose.POWER).resume(Pose.POWER, from);
                        break;
                    case "snap":
                        m.holdGauntlet(g, EntityMagus.HOLD_WORN);
                        m.rest(Pose.SNAP).resume(Pose.SNAP, from);
                        break;
                    default:
                        m.holdGauntlet(g, EntityMagus.HOLD_WORN);
                        m.rest(Pose.STAND).perform(Pose.STAND);
                        break;
                }
                return "He stands there with it (" + how + ").";
            }
            case "reach":
                // Next to him, close enough to reach for it.
                if (magus == null) {
                    return "He isn't here.";
                }
                Vec3d by = magus.getPositionVector().add(flat(player.getPositionVector().subtract(magus.getPositionVector())).scale(2.5D));
                player.setPositionAndUpdate(by.x, Barrow.surface(player.world, MathHelper.floor(by.x), MathHelper.floor(by.z)).getY(), by.z);
                return "You are beside him.";
            case "snap": {
                int slot = marked(player);
                if (slot >= 0) {
                    player.inventory.currentItem = slot;
                    player.connection.sendPacket(new net.minecraft.network.play.server.SPacketHeldItemChange(slot));
                }
                return com.matoon.eternity.stones.SnapPowers.snap(player) ? "Snapped." : "Nothing happened (all six, in your hand?).";
            }
            case "stop":
                if (d.fight == SovereignData.Fight.LOST && !d.fightGauntlet.isEmpty()) {
                    claimTheirs(d.fightGauntlet, player);
                    if (d.fightGauntlet.hasTagCompound()) {
                        d.fightGauntlet.getTagCompound().removeTag(com.matoon.eternity.stones.GauntletWear.TAG);
                    }
                }
                lost(player);
                d.fight = SovereignData.Fight.NONE;
                d.markDirty();
                return "Stopped; your gauntlet is yours again.";
            default:
                return "fight " + d.fight + ", act " + act + " " + actTicks + ", he has it: " + hisNow()
                        + ", you are ready: " + (ready(player) != -1) + ", warlock " + (warlock != null) + ".";
        }
    }
}
