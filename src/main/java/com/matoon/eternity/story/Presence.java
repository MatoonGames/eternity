package com.matoon.eternity.story;

import com.matoon.eternity.client.render.NorseBoxes;
import com.matoon.eternity.entity.EntityNorse;
import com.matoon.eternity.entity.NorsePose;
import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.network.MsgStory;
import com.matoon.eternity.world.ChampionData;
import com.matoon.eternity.world.ChampionDirector;
import com.matoon.eternity.world.OdinsVault;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import com.matoon.eternity.compat.InfinityStones;
import lucraft.mods.lucraftcore.infinity.EnumInfinityStone;
import net.minecraft.block.BlockBush;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static com.matoon.eternity.story.Speakers.DEATH;
import static com.matoon.eternity.story.Talk.act;
import static com.matoon.eternity.story.Talk.say;

/**
 * Death, after the vault. She doesn't speak any more: she has been believed, and doesn't need
 * to be. She is simply there when something dies at your hand, standing over it in what she
 * really is, and what she wants of you next she shows you.
 * <p>
 * The first time: her face turned up past the sky, past the moon and the red planet, to the last
 * blue world and beyond it; and a fist raised, as if holding up a champion's belt. Once they have
 * the Soul Stone: her face up as if waiting for something to cross the sky, a finger pointing
 * straight up, her hands closing round something small (the Comet, and the Regulator that holds
 * what it carries). Then her palm on the ground, killing what grows under it, and her arm toward
 * the red planet. And if they left the Gardener his stone, she is there, and only waits.
 */
final class Presence {

    private final StoryDirector story;
    @Nullable
    private EntityNorse death;
    /** What she is showing them, while she shows it. */
    @Nullable
    private Talk scene;

    Presence(StoryDirector story) {
        this.story = story;
    }

    void reset() {
        death = null;
        scene = null;
    }

    void tick(MinecraftServer server) {
        EntityNorse d = Oath.alive(death);
        if (d != null && StoryDirector.now() % 20 == 0) {
            EntityPlayer near = d.world.getClosestPlayerToEntity(d, 64D);
            // Busy while what she came to show is still being shown, whoever happens to be standing nearest.
            Talk t = scene;
            boolean busy = t != null && !t.isOver();
            if (!busy && d.ticksExisted > 200 && !Oath.watched(d.world, d) || near == null) {
                d.setDead();
                death = null;
            }
        }
    }

    /** Something has died at someone's hand: if she has something to show them, she is standing over it. */
    void died(EntityLivingBase dead, DamageSource source) {
        if (!(source.getTrueSource() instanceof EntityPlayerMP) || !(dead instanceof EntityLiving) || dead instanceof EntityNorse
                || !StoryDirector.enabled() || Betrayal.absent() || Obsession.undead(dead) || story.setPiece()) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) source.getTrueSource();
        if (player.dimension == OdinsVault.dimension() || story.talking(player) || Oath.alive(death) != null
                || !player.world.provider.hasSkyLight()) {
            return;
        }
        // Whoever made the kill, and the friends standing with them: what she shows is for all of them, by what they carry between them.
        List<StoryData.Player> them = new ArrayList<>();
        int held = 0;
        for (EntityPlayerMP each : story.fellowship(player)) {
            if (each == player || each.dimension == player.dimension && each.getDistance(player) < 48D) {
                them.add(story.data().player(each.getUniqueID()));
                held |= InfinityStones.scan(each);
            }
        }
        if (!seen(them, "death_revealed")) {
            return;
        }
        StoryData.Garden garden = story.data().garden;
        boolean soul = (held & InfinityStones.bit(EnumInfinityStone.SOUL)) != 0;
        if (!seen(them, "dir_champion")) {
            champion(player, dead.getPositionVector());
        } else if (!seen(them, "champion_debt") && !seen(them, "magus_risen") && !seen(them, "warlock_took") && unpaid(held)) {
            glass(player, dead.getPositionVector());
        } else if (soul && !seen(them, "dir_regulator") && (seen(them, "champion_debt") || seen(them, "magus_risen"))) {
            regulator(player, dead.getPositionVector());
        } else if (soul && seen(them, "dir_regulator") && !seen(them, "dir_garden") && garden != StoryData.Garden.TAKEN) {
            garden(player, dead.getPositionVector());
        } else if (seen(them, "left_gardener") && !seen(them, "took_time") && garden == StoryData.Garden.SPARED) {
            waiting(player, dead.getPositionVector());
        }
    }

    /**
     * Whether the Champion owed them where the next stone is, and can't pay it now: gone in
     * someone else's ship, or the Power Stone came to them without him being stranded there to
     * trade it for one.
     */
    static boolean unpaid(int held) {
        ChampionData c = ChampionDirector.INSTANCE.data();
        return c.phase == ChampionData.Phase.DEPARTED || (held & InfinityStones.bit(EnumInfinityStone.POWER)) != 0
                && c.phase != ChampionData.Phase.STRANDED && c.phase != ChampionData.Phase.BREAKING;
    }

    /** Whether any of them has seen a beat. */
    private static boolean seen(List<StoryData.Player> them, String beat) {
        for (StoryData.Player p : them) {
            if (p.seen(beat)) {
                return true;
            }
        }
        return false;
    }

    /**
     * She comes the way she first came: a woman in mourning, standing over what died. Then, for
     * whoever she wants something from, she becomes something else ({@link #beguile}).
     */
    private EntityNorse standOver(EntityPlayerMP player, Vec3d at) {
        World world = player.world;
        EntityNorse d = death = EntityNorse.of(world, NorseBoxes.STRANGER, "death");
        Devotion.dress(story, d, player);
        d.setLocationAndAngles(at.x, at.y, at.z, 0F, 0F);
        d.face(player.getPositionVector());
        world.spawnEntity(d);
        d.watch(player);
        return d;
    }

    /** A conversation of hers, drawing in whoever has come with them. */
    private static Talk hers(EntityPlayerMP player, EntityNorse d) {
        return new Talk(player).at(d, 48D).reach(32D);
    }

    /**
     * She becomes what they would least like to say no to, in front of them, from the inside out
     * (see {@link com.matoon.eternity.entity.Morph}); the directions keep time with it. If she is
     * already in it, as she was when they last saw her, she stays in it: and if it has rotted
     * further since, that happened while they weren't looking.
     */
    static Talk beguile(Talk t, EntityNorse d) {
        int shape = Devotion.shape(t);
        if (d.look() != shape) {
            boolean mourning = d.look() == NorseBoxes.STRANGER;
            t.run(() -> {
                d.setRot(Devotion.rot(t));
                d.shapeshift(shape);
            })
                    .note(mourning ? "Something moves under her mourning. Her right arm swells inside its sleeve, knotting, going dark as a bruise, and the elbow bends backwards with a wet crack."
                            : "Something moves under her skin. Her right arm swells, knotting, going dark as a bruise, and the elbow bends backwards with a wet crack.")
                    .pause(26)
                    .note("It bursts. What comes out of it is red, and wet, and too small, and it grows while you watch. The skin closes over it like water. The old one slides off her hand in a sleeve and drops.")
                    .pause(16)
                    .note("Her back bends over the wrong way, until the top of her head nearly touches her heels. Her knees go forward. Things inside her are moving into new places, and you can hear every one of them.")
                    .pause(26)
                    .note("Her head turns, all the way round, slowly, and back. When it comes round again the face on it isn't the one that went.")
                    .pause(24)
                    .direction("She straightens, and rolls her shoulders like someone settling into a coat. What she was lies round her feet in wet heaps.");
        }
        t.run(() -> Devotion.saw(t, d));
        return Devotion.rotting(t, d);
    }

    /**
     * After the glass: the comet, and what holds it. Her face up, a long time, as if waiting for
     * something to cross the sky; then a finger, straight up; then her hands cupped round
     * something small, and closed on it.
     */
    private void regulator(EntityPlayerMP player, Vec3d at) {
        EntityNorse d = standOver(player, at);
        Talk t = hers(player, d);
        beguile(t.pause(30), d)
                .line(DEATH, d, "Look at you. A stone in your hand that can tear a soul out. And you still flinch when things die.")
                .line(DEATH, d, "Don't. Every one of them comes to me in the end, and I'm very gentle with them.")
                .run(() -> d.watch(null).look(d.getPositionEyes(1F).add(0D, 40D, 0D)).act(NorsePose.SKY))
                .direction("She looks up at the sky, a long time, as if waiting for something to cross it.")
                .pause(40)
                .run(() -> d.act(NorsePose.POINT))
                .line(DEATH, d, "Something's coming across your sky. Something burning, with a gift in it.")
                .run(() -> d.look(d.getPositionEyes(1F).add(d.getLookVec().scale(2D))).act(NorsePose.CUP))
                .line(DEATH, d, "You'll want something to carry it in. Something small, that closes.")
                .ask(DEATH, d,
                        say("The comet?", c -> c.run(() -> d.act(NorsePose.POINT))
                                .line(DEATH, d, "Watch the sky, darling. I'd never send you anywhere I wasn't sure of.")),
                        say("Something to hold it in?", c -> c.run(() -> d.act(NorsePose.CUP))
                                .line(DEATH, d, "Everything worth having needs a little box. You'll see.")),
                        act("(Say nothing.)", c -> c.line(DEATH, d, "I like it when you're quiet. It means you're listening.")))
                .run(() -> d.watch(player).act(NorsePose.STAND).rest(NorsePose.STAND))
                .line(DEATH, d, "More of them in your hand every time I see you. Doesn't it feel good?")
                .run(() -> {
                    story.mark(player, "dir_regulator");
                    // What she pointed at comes: across their own sky, a minute or so after.
                    com.matoon.eternity.world.CometDirector.INSTANCE.promise(player, 20 * 50);
                });
        Devotion.after(story, t, d, player);
        scene = t;
        story.talk(player, t);
    }

    /**
     * Toward the Garden: she crouches and lays her palm flat on the ground, and where she touches
     * it nothing grows; then she points low in the sky, where the red planet rises.
     */
    private void garden(EntityPlayerMP player, Vec3d at) {
        EntityNorse d = standOver(player, at);
        Talk t = hers(player, d);
        beguile(t.pause(30), d)
                .line(DEATH, d, "Do you know what I hate? Things that won't die when they should.")
                .run(() -> d.watch(null).look(d.getPositionVector().add(d.getLookVec().scale(1.5D))).act(NorsePose.GROUND))
                .direction("She crouches, and lays her gloved palm flat on the ground.")
                .pause(40)
                .run(() -> wither(d))
                .direction("Where her hand is, the grass goes grey. Nothing grows there.")
                .line(DEATH, d, "There. That's how it's meant to go.")
                .run(() -> d.rest(NorsePose.STAND).act(NorsePose.STAND))
                .pause(20)
                .run(() -> d.look(d.getPositionEyes(1F).add(30D, 14D, 0D)).act(NorsePose.POINT))
                .direction("She stands, and points low in the sky: east, where the red planet comes up.")
                .line(DEATH, d, "Someone on the red world is making things grow where nothing should. He's keeping time in a jar for it.")
                .ask(DEATH, d,
                        say("Mars?", c -> c.line(DEATH, d, "Mars. Bring me what's in his garden, and I'll be so very grateful.")),
                        say("Who's there?", c -> c.run(() -> d.act(NorsePose.GROUND))
                                .line(DEATH, d, "An old man who thinks he can tend his way out of me. He's sweet. Take it from him.")),
                        act("(Say nothing.)", c -> c.line(DEATH, d, "Go on. I'll be there when it's done. I always am.")))
                .run(() -> d.watch(player).act(NorsePose.STAND).rest(NorsePose.STAND))
                .run(() -> story.mark(player, "dir_garden"));
        Devotion.after(story, t, d, player);
        scene = t;
        story.talk(player, t);
    }

    /** Where she lays her hand, the ground dies: grass to dirt, flowers and grass gone. */
    private static void wither(EntityNorse d) {
        World world = d.world;
        Vec3d hand = d.getPositionVector().add(d.getLookVec().scale(0.9D));
        BlockPos c = new BlockPos(hand.x, d.posY - 0.5D, hand.z);
        for (BlockPos p : BlockPos.getAllInBox(c.add(-1, -1, -1), c.add(1, 1, 1))) {
            if (p.distanceSq(c) > 2.1D) {
                continue;
            }
            IBlockState s = world.getBlockState(p);
            if (s.getBlock() == Blocks.GRASS) {
                world.setBlockState(p, Blocks.DIRT.getStateFromMeta(1), 3);
            } else if (s.getBlock() instanceof BlockBush) {
                world.setBlockState(p, Blocks.DEADBUSH.getDefaultState(), 3);
            }
        }
        world.playSound(null, c, SoundEvents.BLOCK_GRASS_BREAK, SoundCategory.BLOCKS, 0.6F, 0.5F);
    }

    /** They left the Gardener with his stone: she is there at the next death, and says nothing, and waits. */
    private void waiting(EntityPlayerMP player, Vec3d at) {
        EntityNorse d = standOver(player, at);
        Talk t = hers(player, d);
        beguile(t.pause(40), d)
                .line(DEATH, d, "You left it in him. That was sweet of you.")
                .direction("She steps close enough that you feel the cold come off her.")
                .line(DEATH, d, "It doesn't change anything. It only makes you wait. And I am so much better at waiting than you.")
                .pause(40)
                .run(() -> d.watch(null))
                .direction("She waits.");
        Devotion.after(story, t, d, player);
        scene = t;
        story.talk(player, t);
    }

    /** Past the last blue world: a fighter, and a belt. */
    private void champion(EntityPlayerMP player, Vec3d at) {
        story.mark(player, "dir_champion");
        World world = player.world;
        EntityNorse d = death = EntityNorse.of(world, NorseBoxes.STRANGER, "death");
        Devotion.dress(story, d, player);
        d.setLocationAndAngles(at.x, at.y, at.z, 0F, 0F);
        d.face(player.getPositionVector());
        world.spawnEntity(d);
        d.watch(player);
        Talk t = hers(player, d);
        beguile(t.pause(30), d)
                .line(DEATH, d, "Hello again. You did that beautifully. You hardly hesitated at all.")
                .run(() -> d.watch(null).look(d.getPositionEyes(1F).add(0D, 40D, 0D)).act(NorsePose.SKY))
                .direction("She turns her face up toward the sky: past the moon, past the red one, to the last blue world. And keeps going.")
                .pause(40)
                .run(() -> d.act(NorsePose.FIST))
                .direction("Then she raises a fist, slowly, as if holding up a belt.")
                .line(DEATH, d, "Past the last blue world there's a man who's never lost a fight. He wears one of my stones in his brow, like a jewel.")
                .ask(DEATH, d,
                        say("Where?", c -> c.run(() -> d.act(NorsePose.SKY))
                                .line(DEATH, d, "Beyond the last blue one. Build something that flies, and keep going.")),
                        say("What's there?", c -> c.run(() -> d.act(NorsePose.FIST))
                                .line(DEATH, d, "The Champion of the Universe, and a crowd that never stops roaring. He'd love to meet you.")),
                        act("(Say nothing.)", c -> c.line(DEATH, d, "Shy? Don't be. Not with me.")))
                .run(() -> d.watch(player).act(NorsePose.STAND).rest(NorsePose.STAND))
                .line(DEATH, d, "Take it off him. For me.")
                .run(() -> story.mark(player, "dir_champion"));
        Devotion.after(story, t, d, player);
        scene = t;
        story.talk(player, t);
    }

    /**
     * What the Champion never told them: the jar where all the worlds touch. Her hands cupped round
     * it, as if she were holding it, and looking in.
     */
    private void glass(EntityPlayerMP player, Vec3d at) {
        EntityNorse d = standOver(player, at);
        Talk t = hers(player, d);
        beguile(t.pause(30), d)
                .line(DEATH, d, "He didn't tell you, did he? The Champion. He owed you a word, and he never paid it. Men.")
                .line(DEATH, d, "Never mind. I always pay. Ask anyone. Ask everyone.")
                .run(() -> d.watch(null).look(d.getPositionEyes(1F).add(d.getLookVec().scale(2D))).act(NorsePose.CUP))
                .direction("She cups her hands together, as if round a jar, and looks down into them.")
                .pause(30)
                .line(DEATH, d, "There's a place where all the worlds touch. Somebody's sitting there in a glass jar, with one of my stones on his head.")
                .line(DEATH, d, "Order and Chaos put him in it, ages ago, and he's been bored ever since. Be interesting for him, and he'll give you anything.")
                .ask(DEATH, d,
                        say("How do I get there?", c -> c.run(() -> d.act(NorsePose.POINT))
                                .line(DEATH, d, "Carry " + (Glass.STONES_FOR_RIFTS == 2 ? "two" : String.valueOf(Glass.STONES_FOR_RIFTS)) + " of my stones on your own world, under an open sky, and the air will tear for you. Walk through before it heals.")),
                        say("Order and Chaos?", c -> c.line(DEATH, d, "Two old men who think the universe is a pair of scales. Don't let them talk at you. They never stop.")),
                        act("(Say nothing.)", c -> c.line(DEATH, d, "Good. Save your breath. You'll want it for the jar.")))
                .run(() -> d.watch(player).act(NorsePose.STAND).rest(NorsePose.STAND))
                .line(DEATH, d, "Bring me what's on his head. He won't miss it. He'll miss me even less.")
                .run(() -> {
                    story.mark(player, "dir_glass");
                    story.mark(player, "champion_debt");
                    story.sendStateToAll();
                });
        Devotion.after(story, t, d, player);
        scene = t;
        story.talk(player, t);
    }

    boolean interact(EntityPlayerMP player, EntityNorse npc) {
        if (npc != death) {
            return false;
        }
        EternityNetwork.send(MsgStory.line(Speakers.DIRECTION, -1, "She smiles at you, and says nothing. She has already told you where."), player);
        return true;
    }

    @Nullable
    String command(EntityPlayerMP player, String action, String[] args) {
        if ("death".equals(action)) {
            // death [champion|glass|regulator|garden|waiting]: she stands in front of you and shows you that.
            StoryData.Player p = story.data().player(player.getUniqueID());
            p.beats.add("death_revealed");
            String which = args.length > 0 ? args[0] : "champion";
            story.data().markDirty();
            Vec3d look = player.getLookVec();
            Vec3d at = new Vec3d(player.posX + look.x * 4D, player.posY, player.posZ + look.z * 4D);
            if (Oath.alive(death) != null) {
                death.setDead();
                death = null;
            }
            switch (which) {
                case "regulator":
                    regulator(player, at);
                    break;
                case "garden":
                    garden(player, at);
                    break;
                case "waiting":
                    waiting(player, at);
                    break;
                case "glass":
                    glass(player, at);
                    break;
                default:
                    champion(player, at);
                    break;
            }
            return "She is here.";
        }
        return null;
    }
}
