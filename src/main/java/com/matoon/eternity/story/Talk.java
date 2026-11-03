package com.matoon.eternity.story;

import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.network.MsgStory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;

/**
 * A conversation, played out on the server a tick at a time: lines said and given time to be
 * read, things done between them, and questions that wait for an answer. An answer splices what
 * follows from it in where the question was, so a script reads in the order it plays, branches
 * and all.
 * <p>
 * It is begun with one player, its lead, but it belongs to everyone standing with them: anyone
 * who comes within {@link #JOIN} of it while it plays is drawn in (StoryDirector does the
 * drawing), and a question goes to all of them. Each votes; the answer is the one most of them
 * gave, settled as soon as nobody left to vote could change it, or a while after the first vote
 * if some never do. A tie goes to the lead. Everyone who voted is shown what was chosen, and by
 * whom, before the conversation goes on.
 * <p>
 * Lines go to everyone near enough to overhear. A conversation ends when its steps run out, or
 * when nobody is left in it: whoever wanders off, leaves the dimension or logs out drops out,
 * the lead passing to someone still there; when the last goes, whatever it was set to do when
 * abandoned is done.
 */
public final class Talk {

    /** What follows from an answer, written onto the conversation it belongs to. */
    public interface Branch {
        void then(Talk t);
    }

    /** One answer the player may give. Text starting with '*' is something done, not said. */
    public static final class Choice {
        final String text;
        final Branch then;

        Choice(String text, Branch then) {
            this.text = text;
            this.then = then;
        }
    }

    public static Choice say(String text, Branch then) {
        return new Choice(text, then);
    }

    /** An answer that is something done, not said: shown in italics. */
    public static Choice act(String text, Branch then) {
        return new Choice("*" + text, then);
    }

    private interface Step {
        /** Does this step's work; true when it is finished and the next may start. */
        boolean tick(Talk talk, EntityPlayerMP player);
    }

    private static int nextId = 1;

    /** How far anyone can be from a line and still hear it. */
    static final double EARSHOT = 32D;
    /** How near a conversation someone has to come to be drawn into it. */
    static final double JOIN = 14D;
    /** How long after the first vote the rest have to cast theirs. */
    static final int VOTE_WINDOW = 300;
    /** How long everyone is shown what a party chose before the conversation goes on. */
    static final int SHOW_CHOICE = 34;

    /**
     * For testing a party's vote with one client: this many phantom voters join every question,
     * and vote (answer {@link #phantomAnswer}, or one at random if that is -1) a few seconds in.
     */
    static int phantoms;
    static int phantomAnswer = -1;

    private static UUID phantom(int i) {
        return UUID.nameUUIDFromBytes(("eternity-phantom-" + i).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    final int id = nextId++;
    /** Whom the conversation was begun with, or whoever has taken their place. */
    UUID lead;
    /** Everyone in it, the lead first. */
    final Set<UUID> members = new LinkedHashSet<>();
    /** Who has asked to move on from the line being said now. */
    private final Set<UUID> skips = new java.util.HashSet<>();
    private final LinkedList<Step> steps = new LinkedList<>();
    @Nullable
    private List<Step> inserting;
    private int stepTicks;
    // A question waiting on an answer.
    @Nullable
    private Choice[] asking;
    private int askTimeout, askSilent;
    private int askedAt;
    private byte askWho;
    private int askEntity = -1;
    /** Who may vote on the question being asked, and what each has said so far. */
    private final Set<UUID> voters = new LinkedHashSet<>();
    private final Map<UUID, Integer> votes = new LinkedHashMap<>();
    private int firstVoteAt = -1;
    private int age;
    // Where the conversation is happening, and how far from it anyone may go.
    @Nullable
    private Entity anchor;
    private double leash = 40D;
    @Nullable
    private Runnable onAbandon;
    private boolean over;
    /** Said in the thick of a fight: the lines leave everyone's view where it is. */
    private boolean inAction;

    public Talk(EntityPlayerMP player) {
        this.lead = player.getUniqueID();
        members.add(lead);
    }

    // ------------------------------------------------------------------ writing a conversation

    /** Ties the conversation to where something is: wandering more than leash away from it drops someone out. */
    public Talk at(@Nullable Entity anchor, double leash) {
        this.anchor = anchor;
        this.leash = leash;
        return this;
    }

    /** The conversation happens in the middle of a fight: nobody's view is turned toward who is talking. */
    public Talk inAction() {
        this.inAction = true;
        return this;
    }

    /** What to do if the conversation is abandoned before its end. */
    public Talk abandoned(Runnable run) {
        this.onAbandon = run;
        return this;
    }

    /** Someone says something; the next step waits until it has had time to be read. */
    public Talk line(byte who, @Nullable Entity speaker, String text) {
        return add((talk, player) -> {
            if (talk.stepTicks == 0) {
                StoryDirector.line(player, speaker != null ? speaker : player, who, speaker != null ? speaker.getEntityId() : -1, text, talk.inAction);
            }
            // Read, by everyone in it, and they have said so: on to the next, after a breath to see the last word land.
            return talk.stepTicks >= readTicks(text) || talk.skipped() && talk.stepTicks >= 6;
        });
    }

    /** Someone in the conversation has read the line on screen and wants the next. */
    void skip(EntityPlayerMP player) {
        if (members.contains(player.getUniqueID())) {
            skips.add(player.getUniqueID());
        }
    }

    /** Whether everyone in it has asked to move on from the line in front of them. */
    private boolean skipped() {
        return !skips.isEmpty() && skips.containsAll(members);
    }

    /** What nobody says: an action, a silence, what someone does instead of answering. */
    public Talk direction(String text) {
        return line(Speakers.DIRECTION, null, text);
    }

    public Talk pause(int ticks) {
        return add((talk, player) -> talk.stepTicks >= ticks);
    }

    public Talk run(Runnable run) {
        return add((talk, player) -> {
            run.run();
            return true;
        });
    }

    /** Waits until something is true, or for at most maxTicks. */
    public Talk until(BooleanSupplier done, int maxTicks) {
        return add((talk, player) -> done.getAsBoolean() || talk.stepTicks >= maxTicks);
    }

    /** Asks, and waits for as long as it takes. */
    public Talk ask(byte who, @Nullable Entity toward, Choice... choices) {
        return ask(who, toward, 0, -1, choices);
    }

    /**
     * Asks everyone in the conversation. If {@code timeout} ticks pass without anyone answering,
     * silence is taken for answer number {@code silent}.
     */
    public Talk ask(byte who, @Nullable Entity toward, int timeout, int silent, Choice... choices) {
        return add((talk, player) -> {
            if (talk.stepTicks == 0) {
                talk.asking = choices;
                talk.askTimeout = timeout;
                talk.askSilent = silent;
                talk.askedAt = talk.age;
                talk.askWho = who;
                talk.askEntity = toward != null ? toward.getEntityId() : -1;
                talk.votes.clear();
                talk.voters.clear();
                talk.firstVoteAt = -1;
                List<EntityPlayerMP> present = talk.present();
                for (EntityPlayerMP p : present) {
                    talk.voters.add(p.getUniqueID());
                }
                for (int i = 0; i < phantoms; i++) {
                    talk.voters.add(phantom(i));
                }
                MsgStory ask = MsgStory.ask(talk.id, who, talk.askEntity, talk.texts(), talk.voters.size());
                for (EntityPlayerMP p : present) {
                    EternityNetwork.send(ask, p);
                }
                Integer preset = StoryDirector.INSTANCE.takePreset(player);
                if (preset != null && preset >= 0 && preset < choices.length) {
                    talk.vote(player, preset);
                }
            }
            for (int i = 0; i < phantoms; i++) {
                if (talk.age - talk.askedAt == 50 + i * 30 && talk.voters.contains(phantom(i))) {
                    int pick = phantomAnswer >= 0 && phantomAnswer < choices.length ? phantomAnswer
                            : new java.util.Random(talk.id * 31L + i).nextInt(choices.length);
                    talk.votes.put(phantom(i), pick);
                    if (talk.firstVoteAt < 0) {
                        talk.firstVoteAt = talk.age;
                    }
                    MsgStory msg = MsgStory.votes(talk.id, talk.tally());
                    for (EntityPlayerMP p : present(talk)) {
                        EternityNetwork.send(msg, p);
                    }
                }
            }
            int chosen = talk.settled();
            if (chosen < 0 && talk.votes.isEmpty() && talk.askTimeout > 0 && talk.age - talk.askedAt >= talk.askTimeout) {
                chosen = talk.askSilent;
            }
            if (chosen < 0) {
                return false;
            }
            Choice choice = choices[chosen];
            boolean together = talk.voters.size() > 1;
            talk.announce(chosen);
            talk.asking = null;
            talk.votes.clear();
            talk.voters.clear();
            talk.steps.removeFirst();
            talk.stepTicks = 0;
            talk.splice(choice.then);
            if (together) {
                // A moment for everyone to see what was chosen before anyone answers it.
                talk.steps.addFirst((t, p) -> t.stepTicks >= SHOW_CHOICE);
            }
            return true;
        });
    }

    /** Writes a branch's steps in, to play next. */
    private void splice(@Nullable Branch branch) {
        if (branch == null) {
            return;
        }
        List<Step> was = inserting;
        inserting = new ArrayList<>();
        branch.then(this);
        steps.addAll(0, inserting);
        inserting = was;
    }

    private Talk add(Step step) {
        if (inserting != null) {
            inserting.add(step);
        } else {
            steps.add(step);
        }
        return this;
    }

    /**
     * How long a line is left before the next is said: long enough to be typed out, and read at an
     * unhurried pace (about twelve letters a second), with a breath after it.
     */
    static int readTicks(String text) {
        return 50 + (int) (text.length() * 1.8F);
    }

    // ------------------------------------------------------------------ voting

    private String[] texts() {
        String[] texts = new String[asking.length];
        for (int i = 0; i < asking.length; i++) {
            texts[i] = asking[i].text;
        }
        return texts;
    }

    /**
     * The answer, if it is settled: when everyone has voted; when one answer has more than half
     * of all the votes there could be; or when the window after the first vote has closed. The
     * most votes wins, and a tie goes to the lead's answer, or failing that to whichever of the
     * tied answers was given first. -1 while it is still open.
     */
    private int settled() {
        if (votes.isEmpty()) {
            return -1;
        }
        int[] count = new int[asking.length];
        for (int v : votes.values()) {
            count[v]++;
        }
        int best = 0;
        for (int c : count) {
            best = Math.max(best, c);
        }
        boolean all = votes.keySet().containsAll(voters);
        boolean majority = best * 2 > voters.size();
        boolean late = firstVoteAt >= 0 && age - firstVoteAt >= VOTE_WINDOW;
        if (!all && !majority && !late) {
            return -1;
        }
        Integer leads = votes.get(lead);
        if (leads != null && count[leads] == best) {
            return leads;
        }
        for (int v : votes.values()) {
            if (count[v] == best) {
                return v;
            }
        }
        return -1;
    }

    /** Who has voted for what, and how long the rest have left. */
    private NBTTagCompound tally() {
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList list = new NBTTagList();
        for (Map.Entry<UUID, Integer> e : votes.entrySet()) {
            NBTTagCompound v = new NBTTagCompound();
            v.setUniqueId("id", e.getKey());
            EntityPlayerMP p = StoryDirector.player(e.getKey());
            int ph = -1;
            for (int i = 0; i < phantoms; i++) {
                ph = phantom(i).equals(e.getKey()) ? i : ph;
            }
            v.setString("n", p != null ? p.getName() : ph >= 0 ? "Phantom " + (ph + 1) : "?");
            v.setInteger("i", e.getValue());
            list.appendTag(v);
        }
        tag.setTag("v", list);
        tag.setInteger("n", voters.size());
        tag.setInteger("left", firstVoteAt < 0 ? -1 : Math.max(0, VOTE_WINDOW - (age - firstVoteAt)));
        return tag;
    }

    /** Tells everyone who voted what was chosen; where it was chosen together, everyone near hears the party say it. */
    private void announce(int chosen) {
        NBTTagCompound tag = tally();
        tag.setInteger("c", chosen);
        MsgStory msg = MsgStory.chosen(id, tag);
        for (UUID v : voters) {
            EntityPlayerMP p = StoryDirector.player(v);
            if (p != null) {
                EternityNetwork.send(msg, p);
            }
        }
        EntityPlayerMP leader = StoryDirector.player(lead);
        if (voters.size() > 1 && leader != null) {
            int n = 0;
            UUID voice = lead;
            for (Map.Entry<UUID, Integer> e : votes.entrySet()) {
                if (e.getValue() == chosen) {
                    if (n == 0 && !chosen(lead, chosen)) {
                        voice = e.getKey();
                    }
                    n++;
                }
            }
            EntityPlayerMP speaker = StoryDirector.player(voice);
            String text = asking[chosen].text;
            String said = (text.startsWith("*") ? "§o" + text.substring(1) + "§r" : "“" + text + "”")
                    + "  §7" + n + " of " + voters.size();
            StoryDirector.line(leader, anchor != null ? anchor : leader, Speakers.PARTY,
                    (speaker != null ? speaker : leader).getEntityId(), said);
        }
    }

    private boolean chosen(UUID who, int answer) {
        Integer v = votes.get(who);
        return v != null && v == answer;
    }

    /** One player's vote; they may change it until the question is settled. */
    void vote(EntityPlayerMP player, int index) {
        if (asking == null || index < 0 || index >= asking.length || !voters.contains(player.getUniqueID())) {
            return;
        }
        votes.put(player.getUniqueID(), index);
        if (firstVoteAt < 0) {
            firstVoteAt = age;
        }
        if (voters.size() > 1) {
            MsgStory msg = MsgStory.votes(id, tally());
            for (UUID v : voters) {
                EntityPlayerMP p = StoryDirector.player(v);
                if (p != null) {
                    EternityNetwork.send(msg, p);
                }
            }
        }
    }

    boolean isAsking() {
        return asking != null;
    }

    // ------------------------------------------------------------------ who is in it

    /** Where the conversation is: what it is tied to, or its lead. */
    @Nullable
    Entity center() {
        return anchor != null ? anchor : StoryDirector.player(lead);
    }

    private static List<EntityPlayerMP> present(Talk t) {
        return t.present();
    }

    /** Everyone in it who is online. */
    List<EntityPlayerMP> present() {
        List<EntityPlayerMP> out = new ArrayList<>();
        for (UUID id : members) {
            EntityPlayerMP p = StoryDirector.player(id);
            if (p != null) {
                out.add(p);
            }
        }
        return out;
    }

    /** Someone else is drawn in; if a question is open, they are asked it too. */
    void join(EntityPlayerMP player) {
        if (!members.add(player.getUniqueID())) {
            return;
        }
        if (asking != null) {
            voters.add(player.getUniqueID());
            EternityNetwork.send(MsgStory.ask(id, askWho, askEntity, texts(), voters.size()), player);
            MsgStory msg = MsgStory.votes(id, tally());
            for (UUID v : voters) {
                EntityPlayerMP p = StoryDirector.player(v);
                if (p != null) {
                    EternityNetwork.send(msg, p);
                }
            }
        }
    }

    /** Someone drops out without ending it for the rest. */
    void leave(UUID player) {
        if (!members.remove(player)) {
            return;
        }
        if (voters.remove(player)) {
            votes.remove(player);
            EntityPlayerMP p = StoryDirector.player(player);
            if (p != null) {
                EternityNetwork.send(MsgStory.unask(), p);
            }
        }
        if (player.equals(lead) && !members.isEmpty()) {
            lead = members.iterator().next();
        }
    }

    /** Whether someone is no longer with it: gone, in another dimension, or too far from what it is tied to. */
    private boolean strayed(@Nullable EntityPlayerMP p) {
        if (p == null) {
            return true;
        }
        if (anchor == null) {
            EntityPlayerMP leader = StoryDirector.player(lead);
            return leader != null && leader != p && (leader.dimension != p.dimension || leader.getDistance(p) > EARSHOT);
        }
        return anchor.dimension != p.dimension || anchor.getDistance(p) > leash;
    }

    /**
     * One tick; false once the conversation is over. Whoever has strayed drops out, and is added
     * to {@code dropped} so the director can forget them.
     */
    boolean tick(List<UUID> dropped) {
        if (over) {
            return false;
        }
        age++;
        if (anchor != null && anchor.isDead && !(anchor instanceof EntityPlayerMP)) {
            abandon(StoryDirector.player(lead));
            return false;
        }
        for (UUID id : new ArrayList<>(members)) {
            if (members.size() > 1 && strayed(StoryDirector.player(id))) {
                leave(id);
                dropped.add(id);
            }
        }
        EntityPlayerMP player = StoryDirector.player(lead);
        if (strayed(player)) {
            abandon(player);
            return false;
        }
        // Several instant steps may run in one tick; a step that waits stops the run.
        for (int guard = 0; guard < 32 && !steps.isEmpty(); guard++) {
            Step step = steps.peekFirst();
            int ticksWere = stepTicks;
            boolean done = step.tick(this, player);
            if (steps.peekFirst() != step) {
                // The step rewrote what comes next (an answered question); carry on from there.
                continue;
            }
            if (!done) {
                stepTicks = ticksWere + 1;
                return true;
            }
            steps.removeFirst();
            stepTicks = 0;
            skips.clear();
        }
        if (steps.isEmpty()) {
            over = true;
            return false;
        }
        return true;
    }

    /** Ends it now, as if everyone had walked away. */
    void abandon(@Nullable EntityPlayerMP player) {
        if (over) {
            return;
        }
        over = true;
        if (asking != null) {
            for (EntityPlayerMP p : present()) {
                EternityNetwork.send(MsgStory.unask(), p);
            }
        }
        if (onAbandon != null) {
            onAbandon.run();
        }
    }

    /** Ends it now without doing anything it would have done. */
    void stop(@Nullable EntityPlayerMP player) {
        onAbandon = null;
        abandon(player);
    }
}
