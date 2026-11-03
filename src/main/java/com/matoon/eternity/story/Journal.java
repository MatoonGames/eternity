package com.matoon.eternity.story;

import com.matoon.eternity.EternityConfig;
import com.matoon.eternity.compat.InfinityStones;
import com.matoon.eternity.world.ChampionData;
import com.matoon.eternity.world.ChampionDirector;
import com.matoon.eternity.world.Nexus;
import com.matoon.eternity.world.OdinsVault;
import com.matoon.eternity.world.SovereignData;
import com.matoon.eternity.world.Tamarata;
import lucraft.mods.lucraftcore.infinity.EnumInfinityStone;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * What a player knows, written down: the chapter they are in, what they are doing now, and what
 * they have been told or found that bears on it. It is what someone who had read every line
 * would remember, no more: where a scene has told them a direction, the journal keeps it; where
 * a puzzle is in what they have read, the journal keeps what they have read.
 * <p>
 * Sent with the rest of their story state (StoryDirector.sendState), and again whenever it
 * changes. Their client shows it on a key, and says so when the goal moves on.
 */
final class Journal {

    private final List<String> notes = new ArrayList<>();
    private String chapter = "", goal = "";

    private Journal() {
    }

    static NBTTagCompound of(StoryDirector story, EntityPlayerMP player) {
        Journal j = new Journal();
        if (StoryDirector.enabled()) {
            j.write(story, player);
        }
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("chapter", j.chapter);
        tag.setString("goal", j.goal);
        NBTTagList list = new NBTTagList();
        for (String n : j.notes) {
            list.appendTag(new NBTTagString(n));
        }
        tag.setTag("notes", list);
        return tag;
    }

    private void at(String chapter, String goal) {
        this.chapter = chapter;
        this.goal = goal;
    }

    private void note(String s) {
        notes.add(s);
    }

    /** Whether something is anywhere in their inventory. */
    private static boolean carries(EntityPlayerMP player, net.minecraft.item.Item item) {
        for (net.minecraft.item.ItemStack stack : player.inventory.mainInventory) {
            if (stack.getItem() == item) {
                return true;
            }
        }
        return player.inventory.offHandInventory.get(0).getItem() == item;
    }

    private static String far(Vec3d from, Vec3d to) {
        double d = Math.sqrt((to.x - from.x) * (to.x - from.x) + (to.z - from.z) * (to.z - from.z));
        String how = d < 40 ? "close by" : d < 200 ? "about " + (int) (Math.round(d / 10D) * 10) + " blocks" : "about "
                + (int) (Math.round(d / 50D) * 50) + " blocks";
        return d < 40 ? how : how + " to the " + Compass.of(from, to);
    }

    private void write(StoryDirector story, EntityPlayerMP player) {
        StoryData d = story.data();
        StoryData.Player p = d.player(player.getUniqueID());
        int held = InfinityStones.scan(player);
        int count = InfinityStones.count(held);
        boolean soul = (held & InfinityStones.bit(EnumInfinityStone.SOUL)) != 0;
        boolean mind = (held & InfinityStones.bit(EnumInfinityStone.MIND)) != 0;
        Vec3d here = player.getPositionVector();

        if (count >= 6 || mind && count >= 5) {
            at("The Finale", "Six stones. Eternity is coming for you.");
            note("Order and Chaos: \"He won't talk. He can't. You'll just see the sky go.\"");
            note("Death needs you to live through him without killing him: kill him, and everything goes with him.");
            warlock(p);
            return;
        }
        // On the Magus's road the Power Stone is gone for good: four will bring him.
        boolean magus = p.seen("magus_risen");
        int need = magus ? 4 : 5;
        if (p.seen("took_time") || count >= need && !mind) {
            if (magus && SovereignData.get().magusHasMind) {
                at("Chapter 6: The Game", "The Magus holds two stones now. He'll come for yours.");
                note("He took the Mind Stone off the Grandmaster's board, and the Grandmaster by the throat. He didn't even want to play.");
                warlock(p);
                return;
            }
            at("Chapter 6: The Game", count >= need ? (magus ? "Four" : "Five") + " stones. The last of the Elders will come to you, at dawn."
                    : "The last of the Elders is waiting. When you carry " + (magus ? "four" : "five") + " stones, he comes to you at dawn.");
            note("The Grandmaster keeps the Mind Stone on his board. He wants to play for it.");
            if (p.seen("took_time")) {
                note("You took the Time Stone out of the Gardener. Death knelt by him.");
            }
            warlock(p);
            return;
        }
        if (Garden.drawn(p)) {
            garden(story, player, p, d, here);
            return;
        }
        if (p.seen("warlock_took") && !p.seen("magus_risen")) {
            sovereign(player, p);
            return;
        }
        if (p.seen("champion_debt")) {
            at("Chapter 4: The Nexus", "");
            note("The Champion: \"There's a thing in a glass jar, where all the worlds touch. Order and Chaos put it there.\"");
            if (soul) {
                goal = "Death will show you the way on. She comes when something dies at your hand.";
                if (p.seen("imbalance")) {
                    note("The In-Betweener: \"About half. Half of every living thing, on every world, all at once. Your hand. Her idea.\"");
                }
            } else if (player.dimension == Nexus.dimension() || p.seen("nexus_reached")) {
                goal = "Win the Soul Stone from the In-Betweener, in his glass at the heart of the grey disc.";
                note("He has been in that glass ten thousand years, and he is bored. Talk to him. He likes to play.");
            } else if (count < Glass.STONES_FOR_RIFTS) {
                goal = "Find where all the worlds touch. Carry " + Glass.STONES_FOR_RIFTS + " stones, in the Overworld, and the air will tear open in front of you onto it.";
                note("You carry " + count + " of the " + Glass.STONES_FOR_RIFTS + " it takes.");
            } else {
                goal = "Stand on open ground in the Overworld. Within a minute the air in front of you tears open onto where all the worlds touch: walk through before it closes.";
                note("It opens again every five minutes or so, if you miss it.");
            }
            warlock(p);
            return;
        }
        if (player.dimension == OdinsVault.dimension() && !p.seen("dir_champion")) {
            vault(d, p, true);
            return;
        }
        if (p.seen("death_revealed")) {
            champion(player, p, held, here);
            note("Warlock: \"Ask her what for.\" Death: \"Balance.\"");
            if (p.aether && !d.flameSpent) {
                note("The Aether is still in you. The Eternal Flame in the vault can set it into a stone: when the vault flickers in front of you, reach for it.");
            }
            warlock(p);
            return;
        }
        if (p.seen("vault_by_cube") || d.oath == StoryData.Oath.DONE || d.aetherTaken || player.dimension == OdinsVault.dimension()) {
            vault(d, p, player.dimension == OdinsVault.dimension());
            return;
        }
        oath(d, p, here, player);
    }

    // ------------------------------------------------------------------ chapter 3

    private void champion(EntityPlayerMP player, StoryData.Player p, int held, Vec3d here) {
        at("Chapter 3: The Champion", "");
        ChampionData c = ChampionDirector.INSTANCE.data();
        boolean power = (held & InfinityStones.bit(EnumInfinityStone.POWER)) != 0;
        boolean onTamarata = player.dimension == Tamarata.dimension();
        if (!p.seen("dir_champion") && !onTamarata && !p.seen("arena_called") && !power) {
            goal = "Death will show you the way on. She comes when something dies at your hand, under an open sky.";
            return;
        }
        if (power) {
            goal = "The Power Stone is yours. The Champion owes you a word before he goes: hear it.";
        } else if (c.phase == ChampionData.Phase.BREAKING) {
            goal = "Tamarata is coming apart under you. Hold on to something.";
        } else if (c.phase == ChampionData.Phase.STRANDED) {
            goal = "He's adrift in what's left of Tamarata, and can't go anywhere. He wants a rocket: right-click him with one.";
            note("The Champion: \"Bring me a rocket and the stone in my brow is yours.\"");
        } else if (c.phase == ChampionData.Phase.DEPARTED) {
            goal = "The Champion has left the wreck in someone's rocket, with the stone gone from his brow.";
        } else if (onTamarata && p.seen("arena_called")) {
            goal = "He can't be knocked down. Make him angry enough, and he'll do something he regrets.";
            note("Every blow you land, and every one of his you make miss, makes him angrier. The bar over him is his temper, not his health.");
        } else if (onTamarata) {
            goal = "Find the Champion in his Colosseum.";
            note("The Colosseum is " + far(here, new Vec3d(Tamarata.ARENA_X + 0.5D, Tamarata.ARENA_Y, Tamarata.ARENA_Z + 0.5D)) + ".");
        } else {
            goal = "Go past the last blue world: Tamarata, beyond Neptune. A tier-" + EternityConfig.champion.tierRequired + " rocket reaches it.";
            note("She turned her face to the sky, past the last blue world, and raised a fist as if holding up a belt.");
        }
        if (!c.planetDestroyed && c.phase != ChampionData.Phase.DEPARTED) {
            note("The Champion of the Universe keeps the Power Stone in his brow. He gives it to whoever earns it.");
        }
        if (p.seen("arena_called") && (p.warlock == StoryData.Stance.RIVAL || p.warlock == StoryData.Stance.BETRAYED)) {
            note(p.seen("warlock_lost_champion") ? "When you fell, Warlock fought on against the Champion alone, and lost. He's gone."
                    : "Warlock came down onto the sand. The Champion fights you both, and you both fight him.");
        }
        if (p.seen("first_loss")) {
            note("The Champion, to Death, in the wreck: \"You waited long enough.\"");
        }
    }

    /** Warlock took the Power Stone home, when they fell in the wreck. */
    private void sovereign(EntityPlayerMP player, StoryData.Player p) {
        at("Chapter 3: The Sovereign", "");
        SovereignData s = SovereignData.get();
        ChampionData c = ChampionDirector.INSTANCE.data();
        boolean aboard = player.dimension == com.matoon.eternity.world.SovereignStation.dimension();
        if (!p.seen("dir_station") && !p.seen("sov_arrived")) {
            goal = "Warlock has gone home with the Power Stone. Death will show you where: she comes when you wake.";
        } else if (!p.seen("sov_arrived") || !aboard && s.station != SovereignData.Station.CONFRONTED) {
            goal = "Warlock took the stone home: the Sovereign, a gold station in orbit beside your own world. A tier-"
                    + com.matoon.eternity.EternityConfig.sovereign.tierRequired + " rocket reaches it.";
            note("She looked back at your own world, and just beside it, where something gold catches the sun. Then her hands closed on nothing.");
        } else if (s.station == SovereignData.Station.CONFRONTED) {
            goal = "Stand with Warlock against the Magus. You can't beat him. You can last.";
        } else if (!p.seen("sov_welcome")) {
            goal = "Nobody came to meet you. Find out what happened here: start at the console by her statue, under the first dome.";
        } else if (!p.seen("sov_his_cocoon")) {
            goal = "Go down the Hall of Cocoons, to the great one at the end of it.";
        } else if (!p.seen("sov_spawn_cleared")) {
            goal = p.seen("sov_vats") ? "Put down what came out of the broken vats." : "Go on, into the room of vats.";
        } else if (Sovereign.archiveRead(p) < 3) {
            goal = "Read her records in the archive (" + Sovereign.archiveRead(p) + " of 3 at least).";
        } else if (Sovereign.podsSeen(p) < 2) {
            goal = "Look into the capsules in the Gallery of the Ascended.";
        } else {
            goal = "Two voices, under the Sanctum's dome, at the end of the station. One of them is his. Go in.";
        }
        recordNotes(p);
        note("the Champion".equals(s.takenFrom)
                ? "You fell in the wreck, and Warlock tore the Power Stone out of the Champion's brow while he hung there, unable to stop him."
                : "You fell in the wreck, and Warlock took the Power Stone off you.");
        if (p.seen("champion_helped")) {
            note("The Champion: \"Wherever she is, that's where he ran. Hit him low. He never guards low.\"");
        } else if (c.phase == ChampionData.Phase.STRANDED && "Adam Warlock".equals(c.stoneHolder)) {
            note("The Champion is still adrift in the wreck of Tamarata, without his stone. He'd still take a rocket, if you brought one.");
        }
        note("Warlock: \"Mother's growing another one. Another me.\"");
        warlock(p);
    }

    /** One line kept from each of her records they have heard, and what they saw in the gallery. */
    private void recordNotes(StoryData.Player p) {
        String[][] kept = {
                {"sov_his_cocoon", "Her record, at his cocoon: \"He left before he was finished. When he comes home, he will be put back in it.\""},
                {"sov_vats", "Her record, among the vats: \"What fails as a man becomes something useful.\""},
                {"sov_subjects", "Her record: \"We do not need them to want anything. We need them to want what we want.\""},
                {"sov_rejected", "Her record: \"Rejected: those who asked why.\""},
                {"sov_recycled", "Her record: \"A thing with a name is a thing someone will mourn.\""},
                {"sov_the_one_who_left", "Her record: \"A son who serves for love serves longer than one who serves for fear.\""},
                {"sov_him", "Her record: \"Robed in white. A gold sun at his brow. Worlds kneeling to him.\""},
                {"sov_church", "Her record: \"One faith. One truth. One face on every altar.\""}};
        for (String[] k : kept) {
            if (p.seen(k[0])) {
                note(k[1]);
            }
        }
        if (Sovereign.podsSeen(p) > 0) {
            note("In the gallery's capsules, asleep behind frost: men with Warlock's face, older, violet, robed in white. Something in it is wrong.");
        }
    }

    private void warlock(StoryData.Player p) {
        if (p.seen("magus_risen")) {
            note("The Magus has the Power Stone in the gem at his brow. He will want the rest, and what you carry.");
        }
        switch (p.warlock) {
            case TRUCE:
                note("Adam Warlock: a truce. You told him you'd stop taking stones.");
                break;
            case RIVAL:
                note("Adam Warlock: your rival. He means to take the stones from you.");
                break;
            case BETRAYED:
                note("Adam Warlock: you gave him your word and broke it. He'll come for you.");
                break;
            case ALLY:
                note(p.seen("magus_risen")
                        ? "Adam Warlock: he went up out of the Sovereign over his mother's body without a word, and took most of it with him. He has not said he is with you. He will be there when you need him."
                        : "Adam Warlock: your ally. He has seen what his Mother was making of him, and he is not going to be it.");
                break;
            default:
                break;
        }
    }

    // ------------------------------------------------------------------ chapter 1

    private void oath(StoryData d, StoryData.Player p, Vec3d here, EntityPlayerMP player) {
        switch (d.oath) {
            case DORMANT:
                return;
            case LEAKING:
                if (d.leaks > 0) {
                    at("Chapter 1: The Oath", "Pieces of somewhere frozen are falling across the world.");
                    note("Snow and turf and stone that don't belong here. Something in the north is coming loose.");
                }
                return;
            case RAGNA:
                at("Chapter 1: The Oath", "Something came down with the last piece of the north. Go to it.");
                if (d.ragna != null && player.dimension == 0) {
                    note("It fell " + far(here, new Vec3d(d.ragna)) + ".");
                }
                return;
            case SEEKING:
                at("Chapter 1: The Oath", "Find Ragna's village, and tell Hrafn she is dead.");
                note("Ragna: \"Tell Hrafn. Tell him it's worse.\"");
                if (d.ragna != null && d.village != null) {
                    note("The stranger said the village lies " + Compass.of(new Vec3d(d.ragna), new Vec3d(d.village)) + " of where Ragna fell ("
                            + d.ragna.getX() + ", " + d.ragna.getZ() + ").");
                }
                note("They keep something for Odin there, and it has started throwing people across the world.");
                return;
            default:
                break;
        }
        at("Chapter 1: The Oath", "");
        if (d.oath == StoryData.Oath.VILLAGE) {
            if (!d.seen("hrafn_met")) {
                goal = "Find Hrafn in the village.";
                note("He keeps the oath. His sister Ragna was the ninth the cube took.");
            } else if (!d.outerOpen && !d.seen("barrow_opened")) {
                goal = "Go with Hrafn to the barrow under the mound, and lay your hand on its door.";
                note("Only the keeper's blood opens the outer door.");
            } else if (!d.innerOpen) {
                goal = "Read the keepers' stones in the barrow, then give Hrafn the oath at the inner door, oldest first.";
                oathNotes(d);
            } else {
                goal = "Take the cube out of its ward, over the altar.";
                note("Hrafn: \"Don't let me touch it.\"");
            }
            return;
        }
        switch (d.oath) {
            case BROKEN:
                goal = "The dead are up. Get the cube out of the barrow.";
                note("The oath: \"Whoever of us breaks it, our dead will take.\"");
                break;
            case SEALED:
                goal = "The barrow has shut. It opens again at dawn.";
                note("Hrafn stayed below, with the cube's old place and the dead.");
                break;
            case MOURNED:
                if (!d.seen("mourned")) {
                    goal = "Go back down into the barrow's hall.";
                } else {
                    goal = "Hold the cube. When it shows you where it was kept, use it.";
                    note("The stranger: \"The cube was kept in Odin's treasure room. There's another one in there.\"");
                    note("Every half a minute or so the cube flickers. Right-click it while the vision shows.");
                }
                break;
            default:
                break;
        }
    }

    /** The oath, as far as it has been read: whose words, by their whole names, and how far it has been said. */
    private void oathNotes(StoryData d) {
        int found = Integer.bitCount(d.linesFound);
        note("Lines read: " + found + " of " + Stones.KEEPERS + (found < Stones.KEEPERS ? ". Each keeper lies under a standing stone in the hall;"
                + " right-click one to read it." : "."));
        for (int k = 0; k < Stones.KEEPERS; k++) {
            // In the order they were cut, not the order they are said; finding that is the point.
            int keeper = new int[]{Stones.SIGRID, Stones.ULF, Stones.EIRIK, Stones.ASGERD, Stones.BJORN}[k];
            if (d.found(keeper)) {
                note("  " + Stones.fullName(keeper) + ": \"" + Stones.OATH[keeper] + "\"");
            }
        }
        note("Hrafn: \"Oldest first. Every keeper after Ulf was the child of the one before. It's in their names.\"");
        if (d.spoken > 0) {
            note("Spoken at the door so far: " + d.spoken + " of " + Stones.KEEPERS + ".");
        }
        if (d.misses > 0) {
            note("Each wrong line wakes one of the dead.");
        }
    }

    // ------------------------------------------------------------------ chapter 2

    private void vault(StoryData d, StoryData.Player p, boolean inVault) {
        at("Chapter 2: The Vault", "");
        if (!d.aetherTaken) {
            goal = "In Odin's Vault: find the cube's old stand.";
            note("The stranger said there was another one in there.");
            note("Something dark sits on the stand now, in a bay on the right: a column of black glass with red moving inside it.");
        } else if (p.aether && !d.flameSpent && !inVault) {
            goal = "The Aether in you wants to go back to the vault. When the vault flickers in front of you, reach for it: right-click with the cube, or with an empty hand.";
            note("It happens every half a minute or so, wherever you are.");
        } else if (p.aether && !d.flameSpent) {
            goal = d.seen("vault_warlock") ? "Bring the Aether in you to the Eternal Flame, in the deepest bay on the right."
                    : "The Aether is in you, and it has woken something in the vault.";
            note("The red at the edges of your sight is the Aether, moving in you.");
        } else {
            goal = "Leave the vault.";
        }
        warlock(p);
    }

    // ------------------------------------------------------------------ chapter 5

    private void garden(StoryDirector story, EntityPlayerMP player, StoryData.Player p, StoryData d, Vec3d here) {
        at("Chapter 5: The Garden", "");
        if (p.seen("bloom_seen")) {
            note("Plants that don't exist are coming up in bare rock and sand. Someone is seeding the inner worlds.");
        }
        if (!p.seen("dir_garden")) {
            goal = "Death will show you the way on. She comes when something dies at your hand.";
            note("She showed you the Comet, and something to hold what it carries.");
            warlock(p);
            return;
        }
        note("Death laid her palm on the ground, and nothing grew there. Then she pointed at the red planet.");
        boolean mars = Garden.marsDimension() != 0;
        switch (d.garden) {
            case UNSEEN:
                goal = mars ? "Go to the red planet: Galacticraft's Mars. A tier-2 rocket will take you." : "Find the green that shouldn't be there.";
                break;
            case GROWING:
                if (d.gardenAt == null || player.dimension != d.gardenDim) {
                    goal = mars ? (d.gardenAt == null ? "Go to Mars, and land. There is green on the red plain: it shows once you are down."
                            : "Go to Mars. There is green on the red plain, at x " + d.gardenAt.getX() + ", z " + d.gardenAt.getZ() + ".")
                            : "Find the green that shouldn't be there.";
                } else if (!p.seen("gardener_met")) {
                    goal = "Go to the green on the red plain: it is " + far(here, new Vec3d(d.gardenAt)) + " (x " + d.gardenAt.getX() + ", z "
                            + d.gardenAt.getZ() + "). A thread of green light stands over it.";
                    note("The stones you carry tick here, softly, like a clock, and faster the closer you are.");
                } else {
                    goal = "Get out of the ground.";
                }
                break;
            case HEDGED:
                if (!d.gardenBreach.isEmpty()) {
                    goal = "His hedge is open. Go in to him: he's on his knees in the middle of what's left of his garden.";
                } else {
                    goal = "His hedge is up, and nothing you can cut or burn gets through it. Find something stronger than him.";
                    note("His roots throw back anyone who comes into the garden while his hedge stands whole.");
                    note("The Power Stone breaks it: hold it in your hand and strike the hedge.");
                    if (p.seen(Companion.TICKET)) {
                        note(p.seen(Companion.CALLED) ? "The Champion is at your side. Walk him up to the hedge."
                                : "The Champion owes you. Hold up his ticket, and walk him up to the hedge.");
                    }
                    if (carries(player, com.matoon.eternity.item.EternityItems.CASKET)) {
                        note("The Casket of Ancient Winters: frozen through, his hedge isn't alive. It's only wood.");
                    }
                    int stones = InfinityStones.scan(player);
                    if ((stones & (InfinityStones.bit(EnumInfinityStone.REALITY) | InfinityStones.bit(EnumInfinityStone.SOUL))) != 0) {
                        note("Hold the Reality or the Soul Stone in your hand near his hedge, and see what it lets you do.");
                    }
                }
                if (p.seen("gardener_killed")) {
                    note("He tried to kill you. Death sent you back. Your things are on the plain outside his hedge.");
                }
                if (d.gardenAt != null && player.dimension == d.gardenDim) {
                    note("The garden is " + far(here, new Vec3d(d.gardenAt)) + ".");
                }
                break;
            case BOUND:
                goal = "He has bound the Time Stone into himself. Take it out of him, or leave.";
                note("The Gardener: \"Someone should know what these cost.\"");
                break;
            case SPARED:
                goal = "You left the Time Stone in him. Nothing moves on until you go back.";
                note("Death waits at every death you cause now. She doesn't point anywhere.");
                if (d.gardenAt != null && player.dimension == d.gardenDim) {
                    note("The garden is " + far(here, new Vec3d(d.gardenAt)) + ".");
                }
                break;
            default:
                goal = "Leave the garden.";
                break;
        }
        warlock(p);
    }

}
