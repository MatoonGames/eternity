package com.matoon.eternity.story;

import com.matoon.eternity.EternityConfig;
import com.matoon.eternity.client.render.NorseBoxes;
import com.matoon.eternity.compat.InfinityStones;
import com.matoon.eternity.compat.Relics;
import com.matoon.eternity.entity.EntityAdamWarlock;
import com.matoon.eternity.entity.EntityDestroyer;
import com.matoon.eternity.entity.EntityEinherjar;
import com.matoon.eternity.entity.EntityNorse;
import com.matoon.eternity.entity.NorsePose;
import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.network.MsgStory;
import com.matoon.eternity.world.OdinsVault;
import com.matoon.eternity.world.VaultDirector;
import net.minecraft.block.BlockDirt;
import net.minecraft.block.BlockStainedGlass;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static com.matoon.eternity.story.Speakers.DEATH;
import static com.matoon.eternity.story.Speakers.STRANGER;
import static com.matoon.eternity.story.Speakers.WARLOCK;
import static com.matoon.eternity.story.Talk.act;
import static com.matoon.eternity.story.Talk.say;

/**
 * Chapter 2, the vault. Odin sleeps; his treasure room is kept by the Einherjar and the
 * Destroyer. The cube's stand holds the Aether now, sealed in a lantern of bronze and black stone with
 * red moving in the slit round its middle; touch it and it breaks, and the Aether goes into whoever touched it.
 * <p>
 * When the guards die, the woman in mourning is there, kneeling by each in turn. Then Adam
 * Warlock comes up the steps, sent for the same thing, and sees what nobody else in the room
 * can: the guards' souls going into her. She doesn't deny it. She never lies.
 * <p>
 * The Eternal Flame on its pillar is the only fire hot enough to set the Aether into a stone,
 * and it takes all the fire it has.
 */
final class VaultStory {

    private final StoryDirector story;
    private final Random rand = new Random();

    @Nullable
    private EntityNorse stranger;
    @Nullable
    private EntityAdamWarlock warlock;
    /** The guards who have fallen, in the order they fell. */
    private final List<EntityEinherjar> fallen = new ArrayList<>();
    private long warlockAt = -1;
    /** What he came up the steps to say, while he is saying it. */
    @Nullable
    private Talk warlockTalk;
    @Nullable
    private UUID warlockFor;
    private boolean wasCalm;
    private final Map<Integer, Long> ignoring = new HashMap<>();
    private final Map<UUID, Long> nextRewrite = new HashMap<>();

    VaultStory(StoryDirector story) {
        this.story = story;
    }

    void reset() {
        stranger = null;
        warlock = null;
        fallen.clear();
        warlockAt = -1;
        warlockFor = null;
        warlockTalk = null;
        ignoring.clear();
        nextRewrite.clear();
    }

    void clear() {
        if (stranger != null) {
            stranger.setDead();
        }
        if (warlock != null) {
            warlock.setDead();
        }
        reset();
    }

    private StoryData data() {
        return story.data();
    }

    // ------------------------------------------------------------------ ticking

    void tick(MinecraftServer server) {
        StoryData d = data();
        long now = StoryDirector.now();
        WorldServer vault = net.minecraftforge.common.DimensionManager.getWorld(OdinsVault.dimension());
        if (vault != null && StoryDirector.enabled()) {
            if (warlockAt >= 0 && now >= warlockAt) {
                warlockAt = -1;
                EntityPlayerMP p = StoryDirector.player(warlockFor);
                if (p != null && p.dimension == OdinsVault.dimension()) {
                    warlockComes(vault, p);
                }
            }
            if (now % 20 == 0) {
                tidy(vault);
            }
        }
        aether(server, now);
        betrayal(server, now);
    }

    // ------------------------------------------------------------------ the guards fall, and she is there

    void died(EntityLivingBase dead, DamageSource source) {
        if (!(dead instanceof EntityEinherjar) || dead.dimension != OdinsVault.dimension() || !StoryDirector.enabled()
                || data().seen("vault_warlock")) {
            return;
        }
        fallen.add((EntityEinherjar) dead);
        WorldServer world = (WorldServer) dead.world;
        EntityNorse she = Oath.alive(stranger);
        Vec3d body = dead.getPositionVector();
        if (she == null) {
            // Behind whoever is fighting, out of their sight: on the steps, coming up, the way anyone would.
            EntityPlayer p = world.getClosestPlayerToEntity(dead, 40D);
            Vec3d at = new Vec3d(OdinsVault.ARRIVE_X, OdinsVault.ARRIVE_Y, OdinsVault.ARRIVE_Z + 2D);
            if (p != null) {
                Vec3d back = p.getLookVec().scale(-1D);
                Vec3d spot = new Vec3d(p.posX + back.x * 7D, p.posY, p.posZ + back.z * 7D);
                if (!Oath.sees(p, spot.add(0D, 1D, 0D)) && OdinsVault.inHall(spot.x, spot.y + 0.5D, spot.z)) {
                    at = spot;
                }
            }
            she = stranger = EntityNorse.of(world, NorseBoxes.STRANGER, "vault");
            she.setLocationAndAngles(at.x, at.y, at.z, 0F, 0F);
            world.spawnEntity(she);
        }
        Vec3d kneel = body.add(1.0D, 0D, 0.2D);
        she.rest(NorsePose.STAND).act(NorsePose.STAND);
        she.walkTo(kneel, 0.45D);
        kneelWhenThere = (EntityEinherjar) dead;
    }

    @Nullable
    private EntityEinherjar kneelWhenThere;

    /** The fallen stay where they fell until the woman has knelt by them and Warlock has seen it. */
    boolean keepsFallen(Entity guard) {
        return StoryDirector.enabled() && guard instanceof EntityEinherjar && guard.dimension == OdinsVault.dimension()
                && !data().seen("vault_warlock");
    }

    // ------------------------------------------------------------------ the Aether, and the Flame

    boolean useBlock(EntityPlayerMP player, BlockPos pos) {
        if (player.dimension != OdinsVault.dimension() || !StoryDirector.enabled()) {
            return false;
        }
        StoryData d = data();
        if (on(OdinsVault.Relic.TESSERACT, pos)) {
            if (!d.aetherTaken) {
                takeAether(player);
            }
            return true;
        }
        if (on(OdinsVault.Relic.ETERNAL_FLAME, pos) && d.player(player.getUniqueID()).aether && !d.flameSpent) {
            setStone(player);
            return true;
        }
        return false;
    }

    private static boolean on(OdinsVault.Relic relic, BlockPos pos) {
        Vec3d base = relic.base();
        return pos.getX() == MathHelper.floor(base.x) && pos.getZ() == MathHelper.floor(base.z)
                && pos.getY() >= OdinsVault.STAND - 1 && pos.getY() <= OdinsVault.STAND + relic.height + 1;
    }

    /** The lantern breaks, and the Aether finds a host. */
    void takeAether(EntityPlayerMP player) {
        StoryData d = data();
        d.aetherTaken = true;
        d.player(player.getUniqueID()).aether = true;
        d.markDirty();
        Vec3d at = OdinsVault.Relic.TESSERACT.base().add(0D, 1.2D, 0D);
        EternityNetwork.sendToDimension(MsgStory.fx(MsgStory.FX_AETHER, at, player.getPositionVector(), player.getEntityId()), player.dimension);
        player.world.playSound(null, at.x, at.y, at.z, SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.BLOCKS, 2F, 0.5F);
        player.world.playSound(null, at.x, at.y, at.z, SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.PLAYERS, 1F, 0.5F);
        EntityDestroyer destroyer = VaultDirector.destroyer(player.world);
        if (destroyer != null) {
            VaultDirector.INSTANCE.wake(destroyer);
        }
        story.sendStateToAll();
        // If the Destroyer is already down, Warlock is already on the steps.
        if (destroyer == null || destroyer.getState() != EntityDestroyer.State.DORMANT && destroyer.getHealth() <= 0F) {
            soon(player, 200);
        }
    }

    /** The Eternal Flame draws the Aether out, closes it into a stone, and goes out for good. */
    void setStone(EntityPlayerMP player) {
        StoryData d = data();
        d.player(player.getUniqueID()).aether = false;
        d.flameSpent = true;
        d.markDirty();
        Vec3d at = OdinsVault.Relic.ETERNAL_FLAME.base().add(0D, OdinsVault.Relic.ETERNAL_FLAME.height + 0.6D, 0D);
        EternityNetwork.sendToDimension(MsgStory.fx(MsgStory.FX_FLAME, at, player.getPositionVector(), player.getEntityId()), player.dimension);
        player.world.playSound(null, at.x, at.y, at.z, SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.BLOCKS, 2F, 0.4F);
        player.world.playSound(null, at.x, at.y, at.z, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 2F, 0.5F);
        ItemStack stone = new ItemStack(anvil.infinity.items.Items.REALITY_STONE);
        Relics.claim(stone, player);
        if (!player.inventory.addItemStackToInventory(stone)) {
            player.dropItem(stone, false);
        }
        story.sendStateToAll();
    }

    /** Fallen: Warlock comes up the steps a little after. */
    void destroyerFell() {
        StoryData d = data();
        if (!StoryDirector.enabled() || !d.aetherTaken || d.seen("vault_warlock") || warlockAt >= 0) {
            return;
        }
        EntityPlayerMP who = null;
        for (UUID id : d.players().keySet()) {
            EntityPlayerMP p = StoryDirector.player(id);
            if (p != null && p.dimension == OdinsVault.dimension()) {
                who = p;
            }
        }
        if (who != null) {
            soon(who, 120);
        }
    }

    private void soon(EntityPlayerMP p, int ticks) {
        warlockFor = p.getUniqueID();
        warlockAt = StoryDirector.now() + ticks;
    }

    /**
     * Someone comes into the vault after the Aether was taken but before Warlock has come (they died
     * to the Destroyer and found their way back, say): he followed it here, and is on the steps soon.
     */
    void entered(EntityPlayerMP player) {
        StoryData d = data();
        if (StoryDirector.enabled() && d.aetherTaken && !d.seen("vault_warlock") && warlockAt < 0 && warlock == null) {
            soon(player, 160);
        }
    }

    /**
     * Someone leaves the vault, or comes back to the world, after Warlock and Death have been and
     * gone without them there to hear it (out of the vault at the time, and questing with nobody who
     * was): what was found out there is no secret, and the story goes on for them too.
     */
    void caughtUp(EntityPlayerMP player) {
        StoryData d = data();
        StoryData.Player p = d.player(player.getUniqueID());
        if (!StoryDirector.enabled() || !d.seen("vault_warlock") || p.seen("death_revealed") || player.dimension == OdinsVault.dimension()
                || warlockTalk != null && !warlockTalk.isOver()) {
            return;
        }
        p.beats.add("death_revealed");
        if (p.warlock == StoryData.Stance.UNMET) {
            p.warlock = StoryData.Stance.RIVAL;
        }
        d.markDirty();
        EternityNetwork.send(MsgStory.line(Speakers.DIRECTION, -1,
                "Whatever was said in the vault, it reaches you anyway: the woman in mourning is Death, and Adam Warlock means to take the stones."), player);
        story.sendState(player);
    }

    /** Someone at the doors with the Aether in them, before he has come: he comes through them. */
    boolean holdsAtDoors(EntityPlayerMP player) {
        StoryData d = data();
        if (!StoryDirector.enabled() || !d.aetherTaken || d.seen("vault_warlock")) {
            return false;
        }
        if (warlock == null && (!story.talking(player) || warlockAt < 0)) {
            warlockFor = player.getUniqueID();
            warlockAt = StoryDirector.now();
        }
        return true;
    }

    // ------------------------------------------------------------------ Warlock

    private void warlockComes(WorldServer world, EntityPlayerMP player) {
        StoryData d = data();
        if (d.seen("vault_warlock")) {
            return;
        }
        // Nothing said in the vault before now can stand in his way: an unfinished aside would otherwise hold them at the doors.
        story.hush(player);
        d.see("vault_warlock");
        d.markDirty();
        wasCalm = VaultDirector.INSTANCE.isCalm();
        VaultDirector.INSTANCE.setCalm(world, true);
        EntityAdamWarlock w = warlock = new EntityAdamWarlock(world);
        Vec3d steps = new Vec3d(OdinsVault.ARRIVE_X, OdinsVault.ARRIVE_Y + 1D, OdinsVault.DOOR_Z - 1D);
        w.setPosition(steps.x, steps.y, steps.z);
        w.script(true);
        world.spawnEntity(w);
        // He comes up out of the stairwell and stops a few strides off.
        Vec3d toward = new Vec3d(player.posX - steps.x, 0D, player.posZ - steps.z);
        double dist = Math.max(1D, toward.length());
        toward = toward.lengthSquared() < 1.0E-4 ? new Vec3d(0D, 0D, -1D) : toward.normalize();
        Vec3d land = new Vec3d(player.posX, player.posY, player.posZ).subtract(toward.scale(Math.min(6D, dist * 0.5D)));
        w.direct(EntityAdamWarlock.State.HOVER, land.add(0D, 1.4D, 0D), player.getPositionEyes(1F), 0.09D);

        EntityNorse she = Oath.alive(stranger);
        if (she == null) {
            Vec3d at = fallen.isEmpty() ? player.getPositionVector().add(toward.scale(-4D)) : fallen.get(fallen.size() - 1).getPositionVector().add(0.9D, 0D, 0.3D);
            she = stranger = EntityNorse.of(world, NorseBoxes.STRANGER, "vault");
            she.setLocationAndAngles(at.x, at.y, at.z, 0F, 0F);
            world.spawnEntity(she);
        }
        EntityNorse her = she;
        boolean both = fallen.size() >= 2;
        Entity lastBody = fallen.isEmpty() ? null : fallen.get(fallen.size() - 1);
        boolean cubeKilled = d.oath != StoryData.Oath.SKIPPED && d.oath.ordinal() >= StoryData.Oath.BROKEN.ordinal();

        // The whole hall is in it: nobody in the vault drops out of this for being at the far end.
        Talk t = new Talk(player).at(w, 48D).reach(160D);
        t.run(() -> {
            her.walkTo(null, 0D);
            her.rest(NorsePose.STAND).act(NorsePose.STAND);
            if (lastBody != null) {
                her.look(lastBody.getPositionVector());
            }
        })
                .until(() -> w.getDistanceSq(land.x, land.y + 1.4D, land.z) < 1.5D, 160)
                .run(() -> w.direct(EntityAdamWarlock.State.STAND, land, player.getPositionEyes(1F), 0.1D))
                .pause(20)
                .line(WARLOCK, w, "Mother sent me for the Aether.")
                .pause(10)
                .line(WARLOCK, w, "You've already...")
                .run(() -> w.direct(EntityAdamWarlock.State.STAND, land, her.getPositionEyes(1F), 0.1D))
                .pause(30)
                .direction("He stops. He's looking past you: at the woman, at the guard on the floor, at the air between them.")
                .line(WARLOCK, w, both ? "Their souls went into her. Both of them." : "His soul went into her.")
                .run(() -> w.direct(EntityAdamWarlock.State.STAND, land, player.getPositionEyes(1F), 0.1D))
                .line(WARLOCK, w, "You didn't see it.")
                .run(() -> w.direct(EntityAdamWarlock.State.STAND, land, her.getPositionEyes(1F), 0.1D))
                .pause(10)
                .line(WARLOCK, w, "You're Death.")
                .pause(16)
                .run(() -> closeIn(world, her, lead(t, player)))
                .pause(14)
                .run(() -> her.watch(player))
                .line(STRANGER, her, "Yes.")
                .pause(24)
                .line(STRANGER, her, "But you'd rather not look at a woman in mourning. Nobody ever does.");
        Presence.beguile(t, her)
                .line(DEATH, her, "There. Isn't this better?")
                .pause(16)
                .line(DEATH, her, "You've died, since you came into this world. A fall. Something in the dark.")
                .line(DEATH, her, "And every time, you woke up. You never once asked why.")
                .pause(20)
                .direction("She comes closer, unhurried, as if she had all the time there is. She has.")
                .line(DEATH, her, "I sent you back, sweetheart. Every single time.")
                .ask(DEATH, her,
                        say("Why?", c -> c.line(DEATH, her, "Because I want something only living hands can do. And yours are such good hands.")),
                        say("What do you want?", c -> c.line(DEATH, her, "The stones. All six of them, on one hand. Yours.")),
                        say("Get away from me.", c -> c.run(() -> Devotion.add(story, lead(t, player), -1)).direction("She laughs, low, and comes a step closer instead.")
                                .line(DEATH, her, "Nobody has ever meant that. Not once, in all the time there's been.")))
                .line(WARLOCK, w, "Ask her what for.")
                .run(() -> her.watch(w))
                .line(DEATH, her, "He's sweet. He always asks the right question a moment too late.")
                .run(() -> her.watch(lead(t, player)))
                .line(DEATH, her, "All six, on one hand. And then...")
                .direction("She lifts a gloved hand in front of your face, and snaps her fingers. Softly.")
                .line(DEATH, her, "Half. Half of everything that breathes, on every world, all at once. Quietly. Like blowing out candles.")
                .run(() -> w.direct(EntityAdamWarlock.State.STAND, land, her.getPositionEyes(1F), 0.1D))
                .line(WARLOCK, w, "She wants you to kill half of everything that's alive.")
                .line(DEATH, her, "I want them home. Every one of them comes to me sooner or later. I'm only asking for sooner.")
                .line(DEATH, her, "And you'd never be afraid of anything again. Not the dark. Not the fall. Not me.");
        if (cubeKilled) {
            t.line(DEATH, her, "I haven't lied to you once. The cube was killing that village; you stopped it. I don't need to lie to you. I only need you to want it.");
        } else {
            t.line(DEATH, her, "I haven't lied to you once. I don't need to. I only need you to want it.");
        }
        t.ask(DEATH, her,
                        say("Never.", c -> c.run(() -> Devotion.add(story, lead(t, player), -1)).direction("She smiles as if you'd said yes.")
                                .line(DEATH, her, "Everyone says never. Then they're holding five, and the sixth is right there in front of them.")),
                        say("What would I get?", c -> c.run(() -> story.mark(lead(t, player), "death_tempted")).run(() -> Devotion.add(story, lead(t, player), 2))
                                .line(DEATH, her, "Me. Grateful. For ever. And a universe with room to breathe in it.")),
                        act("(Say nothing.)", c -> c.run(() -> Devotion.add(story, lead(t, player), 1)).line(DEATH, her, "The quiet ones are my favourite.")))
                .run(() -> w.direct(EntityAdamWarlock.State.STAND, land, lead(t, player).getPositionEyes(1F), 0.1D))
                .line(WARLOCK, w, "Don't listen to her. She'll use you until there's nothing left of you, and then she'll have you anyway.")
                .line(WARLOCK, w, "Give me the stones. I'll take them where she can't reach them.")
                .ask(WARLOCK, w,
                        say("Where?", c -> c.line(WARLOCK, w, "Where I'm from. The Sovereign keep what's theirs.")),
                        say("To your mother.", c -> c.pause(36).line(WARLOCK, w, "...Yes.")),
                        say("Why should I trust you?", c -> c.line(WARLOCK, w, "I'm not asking you to trust me. I'm asking you not to trust her.")))
                .pause(10)
                .ask(WARLOCK, w, 20 * 10, 2,
                        say("All right.", c -> {
                            c.run(() -> stance(lead(t, player), StoryData.Stance.TRUCE, 0)).run(() -> Devotion.add(story, lead(t, player), -1))
                                    .direction("He isn't sure he believes you.")
                                    .line(WARLOCK, w, "Then stop. Don't take another one. If you do, I'll know.");
                        }),
                        say("No.", c -> c.run(() -> stance(lead(t, player), StoryData.Stance.RIVAL, 1)).run(() -> Devotion.add(story, lead(t, player), 1))
                                .line(WARLOCK, w, "Then I'll take them from you. I can't kill you; she's made sure of that. But I can take them.")),
                        act("(Say nothing.)", c -> c.run(() -> stance(lead(t, player), StoryData.Stance.RIVAL, 0))
                                .direction("He reads it in your face.")
                                .line(WARLOCK, w, "Then I'll take them from you. I can't kill you; she's made sure of that. But I can take them.")))
                .run(() -> w.direct(EntityAdamWarlock.State.HOVER, steps.add(0D, 1D, 0D), steps, 0.12D))
                .pause(70)
                .run(() -> her.watch(lead(t, player)))
                .line(DEATH, her, "He was made to obey someone. He thinks you were too.")
                .pause(20)
                .line(DEATH, her, "Were you?")
                .ask(DEATH, her, 20 * 12, 2,
                        say("Yes.", c -> c.run(() -> story.mark(lead(t, player), "were_you_yes")).run(() -> Devotion.add(story, lead(t, player), 2))
                                .line(DEATH, her, "Then obey me. I'm much nicer than his mother.")),
                        say("No.", c -> c.run(() -> story.mark(lead(t, player), "were_you_no"))
                                .line(DEATH, her, "Good. Then it's your hand, and your choice. That's what makes it so lovely.")),
                        act("(Say nothing.)", c -> c.run(() -> story.mark(lead(t, player), "were_you_silent")).run(() -> Devotion.add(story, lead(t, player), 1))
                                .direction("She doesn't say what she makes of it. She only smiles.")))
                .direction("She leans in, close enough that you feel the cold come off her.")
                .line(DEATH, her, "Go and find me the next one. I'll be watching. I'm always watching.")
                .run(() -> end(world, lead(t, player), her));
        t.abandoned(() -> end(world, player, her));
        story.talk(player, t);
        warlockTalk = t;
        // Everyone in the vault is there for this, wherever in the hall they were.
        for (EntityPlayer each : new ArrayList<>(world.playerEntities)) {
            if (each instanceof EntityPlayerMP && each != player && !each.isSpectator()) {
                story.draw(t, (EntityPlayerMP) each);
            }
        }
    }

    /**
     * Found out, she doesn't cross the hall to answer him: if she is any distance off, she goes up in
     * black smoke where she stands and comes out of it a few steps from them, on the vault's own floor,
     * on the side she was on.
     */
    private void closeIn(WorldServer world, EntityNorse her, EntityPlayerMP player) {
        if (her.isDead || her.world != world || player.world != world || her.getDistance(player) < 8D) {
            return;
        }
        Vec3d from = new Vec3d(her.posX - player.posX, 0D, her.posZ - player.posZ).normalize();
        double toward = Math.atan2(from.z, from.x);
        BlockPos spot = null;
        search:
        for (double dist : new double[]{3D, 2.5D, 4D, 2D}) {
            // Straight toward where she was first, then swinging further round either way.
            for (int k = 0; k < 12; k++) {
                double a = toward + (k % 2 == 0 ? 1 : -1) * ((k + 1) / 2) * Math.PI / 6D;
                double x = player.posX + Math.cos(a) * dist, z = player.posZ + Math.sin(a) * dist;
                for (int dy = 0; dy >= -1; dy--) {
                    BlockPos p = new BlockPos(x, player.posY + dy, z);
                    if (OdinsVault.standable(p.getX(), p.getY(), p.getZ()) && world.isAirBlock(p) && world.isAirBlock(p.up())) {
                        spot = p;
                        break search;
                    }
                }
            }
        }
        if (spot == null) {
            return;
        }
        smoke(world, her.posX, her.posY, her.posZ);
        her.getNavigator().clearPath();
        her.setPositionAndUpdate(spot.getX() + 0.5D, spot.getY(), spot.getZ() + 0.5D);
        her.face(player.getPositionVector());
        smoke(world, her.posX, her.posY, her.posZ);
    }

    /** A column of black smoke boiling up off the floor, and the hiss of it. */
    private static void smoke(WorldServer world, double x, double y, double z) {
        world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, x, y + 1D, z, 70, 0.35D, 0.8D, 0.35D, 0.03D);
        world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, x, y + 0.1D, z, 30, 0.7D, 0.05D, 0.7D, 0.01D);
        world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, x, y + 1D, z, 40, 0.3D, 0.9D, 0.3D, 0.02D);
        world.playSound(null, x, y + 1D, z, SoundEvents.ENTITY_ENDERMEN_TELEPORT, SoundCategory.HOSTILE, 0.7F, 0.45F);
        world.playSound(null, x, y + 1D, z, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.HOSTILE, 0.8F, 0.5F);
    }

    /** Whoever leads a conversation now, or whom it was begun with if nobody does. */
    private static EntityPlayerMP lead(Talk t, EntityPlayerMP player) {
        EntityPlayerMP p = t.leadNow();
        return p != null ? p : player;
    }

    /** What Warlock makes of them, and of everyone who answered him with them. */
    private void stance(EntityPlayerMP player, StoryData.Stance stance, int trust) {
        StoryData d = data();
        for (EntityPlayerMP each : story.party(player)) {
            StoryData.Player p = d.player(each.getUniqueID());
            p.warlock = stance;
            p.trust += trust;
            if (stance == StoryData.Stance.TRUCE) {
                p.truceMask = InfinityStones.scan(each);
            }
        }
        d.markDirty();
    }

    private void end(WorldServer world, EntityPlayerMP player, EntityNorse her) {
        StoryData d = data();
        for (EntityPlayerMP each : story.party(player)) {
            StoryData.Player p = d.player(each.getUniqueID());
            p.beats.add("death_revealed");
            if (p.warlock == StoryData.Stance.UNMET) {
                p.warlock = StoryData.Stance.RIVAL;
            }
        }
        d.markDirty();
        VaultDirector.INSTANCE.setCalm(world, wasCalm);
        her.watch(null).look(null);
        her.rest(NorsePose.STAND).act(NorsePose.STAND);
        her.role = "leaving";
        // Into the dark at the far end, and gone.
        her.walkTo(new Vec3d(0.5D, OdinsVault.STAND, -20D), 0.45D);
    }

    // ------------------------------------------------------------------ housekeeping in the vault

    private void tidy(WorldServer world) {
        EntityNorse she = Oath.alive(stranger);
        if (she != null) {
            if (kneelWhenThere != null && she.arrived() && she.getDistance(kneelWhenThere) < 2.2D && she.pose() == NorsePose.STAND
                    && !data().seen("vault_warlock")) {
                she.face(kneelWhenThere.getPositionVector());
                she.rest(NorsePose.TOUCH).act(NorsePose.TOUCH);
                kneelWhenThere = null;
            }
            boolean leaving = "leaving".equals(she.role);
            if (leaving && !Oath.watched(world, she) || world.playerEntities.isEmpty()) {
                she.setDead();
                stranger = null;
            }
        }
        EntityAdamWarlock w = warlock;
        // Not while he is still saying his piece to anyone, wherever they are in the hall.
        if (w != null && !w.isDead && data().seen("vault_warlock") && (warlockTalk == null || warlockTalk.isOver())) {
            Vec3d steps = new Vec3d(OdinsVault.ARRIVE_X, OdinsVault.ARRIVE_Y + 2D, OdinsVault.ARRIVE_Z);
            if (w.getDistanceSq(steps.x, steps.y, steps.z) < 9D || !Oath.watched(world, w) && w.ticksExisted > 400) {
                w.setDead();
                warlock = null;
            }
        }
        if (world.playerEntities.isEmpty()) {
            fallen.clear();
        }
    }

    // ------------------------------------------------------------------ the Aether, in someone

    /** The world's creatures sometimes take someone with the Aether in them for one of their own. */
    void targeted(EntityLiving mob, EntityPlayerMP player) {
        if (!data().player(player.getUniqueID()).aether || mob instanceof EntityDragon || mob instanceof EntityWither || !mob.isNonBoss()) {
            return;
        }
        long now = StoryDirector.now();
        Long until = ignoring.get(mob.getEntityId());
        if (until == null) {
            until = rand.nextFloat() < 0.4F ? now + 200 + rand.nextInt(400) : -now - 400;
            ignoring.put(mob.getEntityId(), until);
        }
        if (until > 0 && now < until) {
            mob.setAttackTarget(null);
        } else if (until < 0 && now > -until) {
            ignoring.remove(mob.getEntityId());
        }
    }

    /** While it is in someone, the ground round their home is rewritten, a block at a time, and stays rewritten. */
    private void aether(MinecraftServer server, long now) {
        if (now % 40 != 0 || !EternityConfig.story.aetherChangesWorld || !EternityConfig.destroyBlocks) {
            return;
        }
        StoryData d = data();
        for (EntityPlayerMP p : server.getPlayerList().getPlayers()) {
            if (!d.player(p.getUniqueID()).aether || p.dimension != 0) {
                continue;
            }
            Long next = nextRewrite.get(p.getUniqueID());
            if (next == null) {
                nextRewrite.put(p.getUniqueID(), now + 200 + rand.nextInt(400));
                continue;
            }
            if (now < next) {
                continue;
            }
            nextRewrite.put(p.getUniqueID(), now + 400 + rand.nextInt(600));
            BlockPos home = p.getBedLocation(0);
            if (home == null) {
                home = p.world.getSpawnPoint();
            }
            World world = p.world;
            for (int i = 0; i < 1 + rand.nextInt(3); i++) {
                BlockPos col = home.add(rand.nextInt(41) - 20, 0, rand.nextInt(41) - 20);
                if (!world.isBlockLoaded(col)) {
                    continue;
                }
                BlockPos top = Barrow.surface(world, col.getX(), col.getZ()).down();
                IBlockState was = world.getBlockState(top);
                IBlockState now2 = rewrite(was);
                if (now2 != null && world.getTileEntity(top) == null) {
                    world.setBlockState(top, now2, 3);
                    EternityNetwork.sendAround(MsgStory.fx(MsgStory.FX_TAKEN, new Vec3d(top).add(0.5D, 1D, 0.5D), Vec3d.ZERO, -1),
                            0, top.getX(), top.getY(), top.getZ(), 64D);
                }
            }
        }
    }

    /** What the Aether makes of a block: its red twin, more or less. */
    @Nullable
    private static IBlockState rewrite(IBlockState s) {
        Material m = s.getMaterial();
        if (m == Material.GRASS || s.getBlock() == Blocks.DIRT && s.getValue(BlockDirt.VARIANT) == BlockDirt.DirtType.DIRT || m == Material.SAND) {
            return Blocks.SAND.getStateFromMeta(1);
        }
        if (s.getBlock() == Blocks.STONE || s.getBlock() == Blocks.COBBLESTONE || s.getBlock() == Blocks.SANDSTONE) {
            return Blocks.RED_SANDSTONE.getDefaultState();
        }
        if (m == Material.WOOD && s.getBlock() != Blocks.PLANKS) {
            return Blocks.BONE_BLOCK.getDefaultState();
        }
        if (m == Material.LEAVES) {
            return Blocks.NETHER_WART_BLOCK.getDefaultState();
        }
        if (s.getBlock() == Blocks.PLANKS) {
            return Blocks.STAINED_HARDENED_CLAY.getStateFromMeta(EnumDyeColor.RED.getMetadata());
        }
        if (s.getBlock() == Blocks.GLASS) {
            return Blocks.STAINED_GLASS.getDefaultState().withProperty(BlockStainedGlass.COLOR, EnumDyeColor.RED);
        }
        return null;
    }

    // ------------------------------------------------------------------ a promise broken

    /** Someone told Warlock they would stop, and took another: he comes for them, already evolved. */
    private void betrayal(MinecraftServer server, long now) {
        if (now % 40 != 0) {
            return;
        }
        StoryData d = data();
        for (EntityPlayerMP p : server.getPlayerList().getPlayers()) {
            StoryData.Player sp = d.player(p.getUniqueID());
            if (sp.warlock != StoryData.Stance.TRUCE || p.isSpectator()) {
                continue;
            }
            int mask = InfinityStones.scan(p);
            if (sp.truceMask == -1) {
                // Their fellowship gave him the word while they were elsewhere: what they carry now is what they promised to stop at.
                sp.truceMask = mask;
                d.markDirty();
            }
            if (sp.betrayalAt < 0 && (mask & ~sp.truceMask) != 0) {
                sp.betrayalAt = now + 1200 + rand.nextInt(2400);
                d.markDirty();
            }
            if (sp.betrayalAt >= 0 && now >= sp.betrayalAt && p.dimension != OdinsVault.dimension() && !story.talking(p)) {
                betrayed(p);
            }
        }
    }

    void betrayed(EntityPlayerMP player) {
        StoryData d = data();
        StoryData.Player sp = d.player(player.getUniqueID());
        sp.warlock = StoryData.Stance.BETRAYED;
        sp.trust -= 1;
        sp.betrayalAt = -1;
        d.markDirty();
        World world = player.world;
        EntityAdamWarlock w = new EntityAdamWarlock(world);
        w.arrive(player, true, false);
        w.evolve();
        world.spawnEntity(w);
        Talk t = new Talk(player).at(w, 96D);
        t.pause(90)
                .line(WARLOCK, w, "You said you'd stop.")
                .pause(20)
                .line(WARLOCK, w, "I believed you.")
                .pause(30)
                .direction("Quieter.")
                .line(WARLOCK, w, "I didn't know anyone could just... say it, and not mean it.")
                .pause(20)
                .run(() -> w.setCalm(false));
        t.abandoned(() -> w.setCalm(false));
        story.talk(player, t);
    }

    // ------------------------------------------------------------------ people

    boolean interact(EntityPlayerMP player, EntityNorse npc) {
        if (npc != stranger) {
            return false;
        }
        if (!data().seen("vault_warlock")) {
            EternityNetwork.send(MsgStory.line(Speakers.DIRECTION, -1, "She doesn't look up from him."), player);
        }
        return true;
    }

    @Nullable
    String command(EntityPlayerMP player, String action, String[] args) {
        StoryData d = data();
        switch (action) {
            case "vault":
                VaultDirector.INSTANCE.enter(player);
                return "Into the vault.";
            case "aether":
                if (player.dimension != OdinsVault.dimension()) {
                    return "Only in the vault.";
                }
                takeAether(player);
                return "The Aether is in you.";
            case "flame":
                if (player.dimension != OdinsVault.dimension()) {
                    return "Only in the vault.";
                }
                d.player(player.getUniqueID()).aether = true;
                setStone(player);
                return "The Flame sets the stone, and goes out.";
            case "warlock":
                if (player.dimension != OdinsVault.dimension()) {
                    return "Only in the vault.";
                }
                d.aetherTaken = true;
                d.beats.remove("vault_warlock");
                warlockFor = player.getUniqueID();
                warlockAt = StoryDirector.now();
                return "He is on the steps.";
            case "betray":
                betrayed(player);
                return "He has come for his promise.";
            case "relight":
                d.flameSpent = false;
                d.aetherTaken = false;
                d.beats.remove("vault_warlock");
                d.markDirty();
                story.sendStateToAll();
                return "The Flame burns and the Aether is back in its lantern, as if nobody had come.";
            default:
                return null;
        }
    }
}
