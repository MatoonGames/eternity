package com.matoon.eternity.story;

import com.matoon.eternity.EternitySounds;
import com.matoon.eternity.block.BlockSovereignDoor;
import com.matoon.eternity.block.BlockSovereignLight;
import com.matoon.eternity.block.EternityBlocks;
import com.matoon.eternity.client.render.NorseBoxes;
import com.matoon.eternity.compat.Relics;
import com.matoon.eternity.entity.EntityAdamWarlock;
import com.matoon.eternity.entity.EntityDraugr;
import com.matoon.eternity.entity.EntityMagus;
import com.matoon.eternity.entity.EntityNorse;
import com.matoon.eternity.entity.EntityOrderChaos;
import com.matoon.eternity.entity.NorsePose;
import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.network.MsgTribunalFx;
import com.matoon.eternity.world.SovereignData;
import com.matoon.eternity.world.SovereignStation;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static com.matoon.eternity.story.Speakers.CHAOS;
import static com.matoon.eternity.story.Speakers.MAGUS;
import static com.matoon.eternity.story.Speakers.MOTHER;
import static com.matoon.eternity.story.Speakers.RECORD;
import static com.matoon.eternity.story.Speakers.WARLOCK;
import static com.matoon.eternity.story.Talk.act;
import static com.matoon.eternity.story.Talk.say;

/**
 * Warlock's home, when they have chased the Power Stone to it: the Sovereign station, gold and
 * empty. Nobody meets them. They follow what was left: her recorded voice on the consoles, the
 * cocoons he and his brothers came out of, the vats the failures went into, and a gallery of
 * sleepers who look like him grown old and wrong. Each hall opens when they have seen enough of
 * the last; the doors light when they are ready. At the far end, in the Sanctum, two voices.
 * <p>
 * The records never say the word Magus. They do not need to.
 */
final class Sovereign {

    /** What each console keeps: her voice, in her own words, as the station kept it. */
    private static final Map<String, String[]> RECORDS = new LinkedHashMap<>();
    /** What each capsule's plaque says, and the one direction under it. */
    private static final String[] PLAQUES = {
            "PROJECTION. THE ASCENDED, GROWN FORWARD A THOUSAND YEARS, TO SEE WHAT HE BECOMES.",
            "PROJECTION. FORM: COMPLETE. WILL: ABSOLUTE. DO NOT WAKE.",
            "PROJECTION. HIS CHURCH, AT ITS HEIGHT: TEN THOUSAND WORLDS. ONE PRAYER.",
            "PROJECTION. HE WILL CALL HIMSELF ————",
            "PROJECTION. FAILED. HE WOKE, AND LOOKED AT US, AND WE WERE AFRAID.",
            "PROJECTION. HE DOES NOT REMEMBER HER. THIS IS ACCEPTABLE.",
            "PROJECTION. THE ASCENDED, ENTHRONED.",
            "PROJECTION. THE ASCENDED, AT THE END OF EVERYTHING. STILL RIGHT."};

    static {
        RECORDS.put("welcome", new String[]{
                "Welcome, child of the Sovereign. You stand in the house of perfection.",
                "Everything aboard was made by us, and everything we make, we make toward one end.",
                "Go in. Be seen. Be found worthy."});
        RECORDS.put("cocoons", new String[]{
                "Generation nine. Forty cocoons. Thirty-one opened.",
                "Of the thirty-one, none were him.",
                "Keep the shells where the next generation will grow up looking at them."});
        RECORDS.put("his_cocoon", new String[]{
                "Him. The finest thing we have ever grown.",
                "He was meant to wake when I chose. He woke when he chose.",
                "He left before he was finished. I tell the others he is a prodigy. He is not. He is unfinished.",
                "When he comes home, he will come home to this. He will be put back in it."});
        RECORDS.put("vats", new String[]{
                "The vats are for what the cocoons reject.",
                "Nothing is wasted. What fails as a man becomes something useful. A guard. A servant. A lesson.",
                "Seal the northern vats. Something in them has learned to count the keeper's steps."});
        RECORDS.put("subjects", new String[]{
                "Subjects one to forty. Perfect in form. Empty in will.",
                "We do not need them to want anything. We need them to want what we want."});
        RECORDS.put("rejected", new String[]{
                "Rejected, generation seven: those who asked why.",
                "Rejected, generation eight: those who asked twice."});
        RECORDS.put("recycled", new String[]{
                "Recycled into the vats: four hundred and twelve.",
                "Do not name them. A thing with a name is a thing someone will mourn."});
        RECORDS.put("the_one_who_left", new String[]{
                "He has gone out to gather the stones. He believes he goes for me.",
                "Let him believe it. A son who serves for love serves longer than one who serves for fear.",
                "And when he has them all, he will kneel, and give them to me, and I will give him back what he is for."});
        RECORDS.put("him", new String[]{
                "I dreamed of him grown. Robed in white. A gold sun at his brow.",
                "Worlds kneeling to him, all of them saying the same words, at the same hour.",
                "There will be order, under him. There will be truth. One truth. It is what I made him for."});
        RECORDS.put("church", new String[]{
                "We will build him a church, when he is ready. One faith. One truth. One face on every altar.",
                "He will not understand at first. They never understand what they are for.",
                "But he will. In the end, he will believe it more than I do."});
        RECORDS.put("sanctum", new String[]{
                "Enter, and kneel."});
    }

    /** The doors, in order, and what has to have been seen before each one lights. */
    private static final String[] ORDER = {"atrium", "cocoons", "vats", "archive", "gallery", "sanctum"};

    /** Who the Magus is, to the ledger of stones. */
    static final java.util.UUID MAGUS_ID = java.util.UUID.nameUUIDFromBytes("eternity:magus".getBytes(java.nio.charset.StandardCharsets.UTF_8));

    private final StoryDirector story;
    private final Random rand = new Random();
    /** Seams of light stuttering just now, and when each comes back. */
    private final Map<BlockPos, Long> failing = new LinkedHashMap<>();
    /** The gallery's sleepers, by the foot of their capsule. */
    private final Map<BlockPos, EntityMagus> sleepers = new LinkedHashMap<>();
    /** What came out of the broken vats, while any of it is still moving. */
    private final List<EntityDraugr> spawn = new ArrayList<>();
    private boolean spawnWoken;
    /** The people of the scene in the Sanctum, while it is playing. */
    @Nullable
    private EntityAdamWarlock warlock;
    @Nullable
    private EntityNorse mother;
    @Nullable
    private EntityOrderChaos chaos;
    @Nullable
    private EntityMagus magus;
    /** The window into his future, while it is open; and whether time is holding still. */
    private boolean windowOpen, stopped;
    /** Who the scene was played for, and whether the end of it has begun. */
    @Nullable
    private EntityPlayerMP fighting;
    private boolean ending;
    /** Warlock against the Magus, while it is going on. */
    @Nullable
    private MagusDuel duel;
    /** Whatever bodies are doing between the words, staged to the tick. */
    private final List<Timeline> playing = new ArrayList<>();

    Sovereign(StoryDirector story) {
        this.story = story;
    }

    void reset() {
        failing.clear();
        sleepers.clear();
        spawn.clear();
        spawnWoken = false;
        warlock = null;
        mother = null;
        chaos = null;
        magus = null;
        windowOpen = stopped = ending = false;
        fighting = null;
        duel = null;
        playing.clear();
        if (DimensionManager.getWorld(0) != null) {
            // A scene cut off by the server going down starts over the next time someone walks in.
            SovereignData s = SovereignData.get();
            if (s.station == SovereignData.Station.CONFRONTED) {
                s.station = SovereignData.Station.ABANDONED;
                s.markDirty();
            }
        }
    }

    @Nullable
    private static WorldServer world() {
        return DimensionManager.getWorld(SovereignStation.dimension());
    }

    void tick(MinecraftServer server) {
        WorldServer world = world();
        if (world == null || world.playerEntities.isEmpty()) {
            return;
        }
        for (EntityPlayer p : world.playerEntities) {
            if (p instanceof EntityPlayerMP && p.isEntityAlive()) {
                arrived((EntityPlayerMP) p);
            }
        }
        flicker(world);
        if (StoryDirector.now() % 20 == 0) {
            lightDoors(world);
        }
        vats(world);
        sleepers(world);
        sanctum(world);
    }

    // ------------------------------------------------------------------ the vats: what came out of them

    /** The first time someone who has read his cocoon walks in among the vats, what is in the broken ones gets up. */
    private void vats(WorldServer world) {
        spawn.removeIf(e -> e.isDead || e.getHealth() <= 0F);
        if (spawnWoken && spawn.isEmpty()) {
            spawnWoken = false;
            for (EntityPlayer p : world.playerEntities) {
                if (p instanceof EntityPlayerMP && SovereignStation.hallAt(p.posX, p.posY, p.posZ) >= 3) {
                    story.mark((EntityPlayerMP) p, "sov_spawn_cleared");
                }
            }
            for (EntityPlayer p : world.playerEntities) {
                if (p instanceof EntityPlayerMP && !story.talking(p) && SovereignStation.hallAt(p.posX, p.posY, p.posZ) == 3) {
                    Talk t = new Talk((EntityPlayerMP) p).inAction();
                    t.direction("The last of them goes down, and stays down. The vats bubble on, patient, empty.");
                    story.talk((EntityPlayerMP) p, t);
                    break;
                }
            }
            return;
        }
        if (spawnWoken || StoryDirector.now() % 10 != 0) {
            return;
        }
        for (EntityPlayer p : world.playerEntities) {
            StoryData.Player sp = story.data().player(p.getUniqueID());
            if (!(p instanceof EntityPlayerMP) || !p.isEntityAlive() || sp.seen("sov_spawn_cleared") || !sp.seen("sov_his_cocoon")
                    || SovereignStation.hallAt(p.posX, p.posY, p.posZ) != 3 || p.posX < 95) {
                continue;
            }
            spawnWoken = true;
            for (BlockPos vat : SovereignStation.brokenVats()) {
                Vec3d out = new Vec3d(vat.getX() + 0.5D, vat.getY(), vat.getZ() + 0.5D);
                Vec3d toward = new Vec3d(SovereignStation.VATS_X + 0.5D - out.x, 0D, -out.z).normalize();
                Vec3d at = out.add(toward.scale(1.4D));
                EntityDraugr d = new EntityDraugr(world).grown(at, (float) Math.toDegrees(Math.atan2(-toward.x, toward.z)));
                world.spawnEntity(d);
                d.wake(p, false);
                spawn.add(d);
            }
            world.playSound(null, p.posX, p.posY, p.posZ, SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 2F, 0.5F);
            if (!story.talking(p)) {
                Talk t = new Talk((EntityPlayerMP) p).inAction();
                t.direction("Behind the broken glass, something has been waiting for the keeper's steps. They are not the keeper's.");
                story.talk((EntityPlayerMP) p, t);
            }
            return;
        }
    }

    // ------------------------------------------------------------------ the Sanctum

    private void sanctum(WorldServer world) {
        SovereignData s = SovereignData.get();
        if (windowOpen && StoryDirector.now() % 2 == 0) {
            window(world);
        }
        if (stopped) {
            for (EntityPlayer p : world.playerEntities) {
                if (SovereignStation.hallAt(p.posX, p.posY, p.posZ) == 6) {
                    p.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 6, 5, false, false));
                }
            }
        }
        for (Timeline t : new ArrayList<>(playing)) {
            if (!t.tick()) {
                playing.remove(t);
            }
        }
        if (duel != null && !duel.tick()) {
            duel = null;
        }
        if (s.station != SovereignData.Station.ABANDONED || !s.warlockHasStone()) {
            return;
        }
        for (EntityPlayer p : world.playerEntities) {
            if (p instanceof EntityPlayerMP && p.isEntityAlive() && !story.talking(p) && SovereignStation.hallAt(p.posX, p.posY, p.posZ) == 6
                    && p.posX > SovereignStation.SANCTUM_X - SovereignStation.SANCTUM_R + 3) {
                confront((EntityPlayerMP) p, world);
                return;
            }
        }
    }

    /** The window's edge: a ring of torn air at the mirrors, white and gold light pouring out of it. */
    private void window(WorldServer world) {
        BlockPos w = SovereignStation.WINDOW;
        for (int i = 0; i < 24; i++) {
            double a = rand.nextDouble() * Math.PI * 2D;
            double y = w.getY() + Math.sin(a) * 3.2D, z = w.getZ() + 0.5D + Math.cos(a) * 2.2D;
            world.spawnParticle(EnumParticleTypes.END_ROD, w.getX() + 0.5D, y, z, 1, 0.02D, 0.02D, 0.02D, 0.01D);
        }
        world.spawnParticle(EnumParticleTypes.FIREWORKS_SPARK, w.getX() + 0.5D, w.getY(), w.getZ() + 0.5D, 4, 0.2D, 2.2D, 1.4D, 0.02D);
        if (StoryDirector.now() % 40 == 0) {
            world.playSound(null, w, SoundEvents.BLOCK_PORTAL_AMBIENT, SoundCategory.HOSTILE, 1.5F, 0.6F);
        }
    }

    /**
     * Warlock and his Mother, on her dais, arguing; time stops for everyone but him, and Lord
     * Chaos steps out of a mirror to tell him what she is making; he does not believe it, so
     * Chaos shows him, through the stone he brought home, and the Magus steps through, and will
     * not go back. Warlock turns on her. The Magus looks at him the way a man looks at a picture
     * of himself as a child, and kills her, and then there is a fight.
     */
    private void confront(EntityPlayerMP player, WorldServer world) {
        SovereignData s = SovereignData.get();
        s.station = SovereignData.Station.CONFRONTED;
        s.markDirty();
        BlockPos dais = SovereignStation.DAIS, throne = SovereignStation.THRONE;
        EntityNorse m = mother = EntityNorse.of(world, NorseBoxes.AYESHA, "mother");
        m.setLocationAndAngles(throne.getX() - 1.5D, ground(world, throne.getX() - 1.5D, throne.getY() + 2D, throne.getZ() + 0.5D), throne.getZ() + 0.5D, 90F, 0F);
        world.spawnEntity(m);
        EntityAdamWarlock w = warlock = new EntityAdamWarlock(world);
        w.script(true);
        Vec3d warlockAt = new Vec3d(dais.getX() - 3.5D, dais.getY(), dais.getZ() + 0.5D);
        w.setPosition(warlockAt.x, warlockAt.y, warlockAt.z);
        world.spawnEntity(w);
        w.direct(EntityAdamWarlock.State.STAND, warlockAt, m.getPositionEyes(1F), 0.2D);
        m.watch(w).act(NorsePose.LECTURE);
        // Made now, brought in later: so the scene can turn to each of them when they speak.
        EntityOrderChaos c = chaos = new EntityOrderChaos(world);
        c.setKind(EntityOrderChaos.Kind.CHAOS);
        EntityMagus g = magus = new EntityMagus(world);
        Vec3d mirror = new Vec3d(SovereignStation.SANCTUM_X - 4D, SovereignStation.FLOOR + 3.5D, -9.5D);
        Talk t = new Talk(player).at(m, 64D);
        t.abandoned(() -> abandon(world));
        t.direction("Under the great dome, two voices. One of them is his.")
                .pause(20)
                .line(MOTHER, m, "You left before you were finished. You come home with one stone, and you think that makes you finished.")
                .line(WARLOCK, w, "I brought you the Power Stone, Mother. It cost a world. Not mine. Someone's.")
                .run(() -> m.act(NorsePose.REACH))
                .line(MOTHER, m, "Then give it to me. And go back where you belong.")
                .line(WARLOCK, w, "Where I belong.")
                .line(MOTHER, m, "Your cocoon is waiting. It has always been waiting.")
                .pause(15)
                .line(WARLOCK, w, "I walked through the gallery. The ones asleep in the frost. They have my face. Who are they?")
                .run(() -> m.act(NorsePose.LECTURE))
                .line(MOTHER, m, "What you are for.")
                .pause(20)
                .run(() -> {
                    stopped = true;
                    world.playSound(null, dais, SoundEvents.BLOCK_PORTAL_TRIGGER, SoundCategory.HOSTILE, 2F, 0.5F);
                    m.watch(null);
                })
                .direction("And everything stops. Her hand, halfway through a gesture. The dust in the light. Your own breath.")
                .pause(30)
                .run(() -> {
                    c.setPosition(mirror.x, mirror.y, mirror.z);
                    world.spawnEntity(c);
                    c.puppet(mirror.add(1.5D, 1.5D, 1.5D), w);
                    world.playSound(null, mirror.x, mirror.y, mirror.z, EternitySounds.RIFT_OPEN, SoundCategory.HOSTILE, 3F, 0.7F);
                    w.direct(EntityAdamWarlock.State.STAND, warlockAt, mirror.add(1.5D, 2.5D, 1.5D), 0.2D);
                })
                .direction("Something steps out of a mirror that was only reflecting gold a moment ago.")
                .line(CHAOS, c, "Not everything has stopped. You, boy. You I left moving.")
                .line(WARLOCK, w, "Chaos.")
                .line(CHAOS, c, "Lord Chaos. Your mother is very good at what she does. Do you know what she is making?")
                .line(WARLOCK, w, "Me.")
                .line(CHAOS, c, "No. Him. A priest with your face. A church with your face on every altar. Ten thousand worlds saying one prayer, at one hour, forever.")
                .line(CHAOS, c, "Do you know what that is to me? An ending. Everything that could ever happen, narrowed to the one thing that must.")
                .line(WARLOCK, w, "You're lying. You want me afraid of her so I'll come to you instead.")
                .line(CHAOS, c, "I don't want you at all. But I'll show you.")
                .run(() -> {
                    windowOpen = true;
                    Vec3d chest = w.chest();
                    EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.CHAMPION_STONE, chest.x, chest.y, chest.z, 0, 0, 0,
                            w.getEntityId(), 30), world.provider.getDimension());
                    world.playSound(null, SovereignStation.WINDOW, SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.HOSTILE, 3F, 0.8F);
                })
                .direction("The stone he brought home flares violet. In front of the mirrors, the air tears open, top to bottom.")
                .pause(30)
                .direction("Through it: a cathedral, white and gold, with no end to it. Ranks of figures, kneeling, all facing the same way.")
                .pause(30)
                .run(() -> {
                    BlockPos win = SovereignStation.WINDOW;
                    // Not walking through: unfolding out of a line of light in the window's mouth.
                    g.setPosition(win.getX() - 0.5D, win.getY() - 3D, win.getZ() + 0.5D);
                    world.spawnEntity(g);
                    g.rest(EntityMagus.Pose.FLOAT).perform(EntityMagus.Pose.APPEAR, 0);
                    g.hoverTo(new Vec3d(win.getX() + 3.5D, win.getY() - 2.5D, win.getZ() + 0.5D), 0.03D);
                    g.watch(w);
                    world.playSound(null, win, SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 1.5F, 1.4F);
                    world.playSound(null, win, EternitySounds.RIFT_IMPACT, SoundCategory.HOSTILE, 2F, 1.3F);
                })
                .direction("And someone steps through.")
                .pause(30)
                .line(CHAOS, c, "That was a possibility. A glimpse. You can go back now.")
                .run(() -> g.watch(c).act(NorsePose.REGARD))
                .line(MAGUS, g, "No.")
                .pause(25)
                .run(() -> {
                    c.withdraw();
                })
                .direction("For the first time in anything you have seen of him, Lord Chaos looks uncertain. Then he is gone, back into the glass.")
                .run(() -> stopped = false)
                .direction("And everything moves again.")
                .run(() -> m.watch(g).act(NorsePose.RECOIL))
                .line(MOTHER, m, "What is that?")
                .run(() -> w.direct(EntityAdamWarlock.State.STAND, warlockAt, m.getPositionEyes(1F), 0.2D))
                .line(WARLOCK, w, "You know what it is. You have forty of him asleep in your gallery.")
                .line(WARLOCK, w, "You were making him. Not me. Him.")
                .run(() -> g.watch(w).act(NorsePose.REGARD))
                .line(MAGUS, g, "So small. I had forgotten how small.")
                .run(() -> m.act(NorsePose.PLEAD))
                .line(MOTHER, m, "My son. My finished son. Look at you.")
                .run(() -> g.watch(m))
                .line(MAGUS, g, "I have no mother. I have a church.")
                .run(() -> playing.add(motherDies(player, world, g, m, w, s)))
                .pause(EntityMagus.CRUSH_CLOSE + 26)
                .direction("She hits the floor of her own Sanctum and does not move again. The gold goes out of her skin like the light going out of a window.")
                .pause(20)
                .line(WARLOCK, w, "...Mother?")
                .pause(25)
                .run(() -> begin(player, world));
        story.talk(player, t);
    }

    /**
     * The Magus closes his hand from across the room, and she dies in it: taken hold of and
     * lifted off her own dais, clawing at her throat, his lightning in her; then the fist, and
     * something in her breaks; she falls, and does not get up, and the gold goes out of her, and
     * what was in her goes up and out of the light.
     */
    private Timeline motherDies(EntityPlayerMP player, WorldServer world, EntityMagus g, EntityNorse m, EntityAdamWarlock w, SovereignData s) {
        Timeline t = new Timeline();
        Vec3d[] at = new Vec3d[2];
        int dim = world.provider.getDimension();
        t.at(0, () -> {
            g.faceNow(m.getPositionEyes(1F));
            g.perform(EntityMagus.Pose.CRUSH, 0);
            world.playSound(null, g.posX, g.posY + 1.5D, g.posZ, SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.HOSTILE, 1.4F, 0.6F);
            world.playSound(null, g.posX, g.posY + 1.5D, g.posZ, SoundEvents.EVOCATION_ILLAGER_CAST_SPELL, SoundCategory.HOSTILE, 2F, 0.5F);
        });
        t.at(10, () -> {
            at[0] = new Vec3d(m.posX, ground(world, m.posX, m.posY + 1.5D, m.posZ), m.posZ);
            Vec3d toward = g.getPositionVector().subtract(at[0]);
            toward = new Vec3d(toward.x, 0D, toward.z).normalize();
            at[1] = at[0].add(0D, 2.2D, 0D).add(toward.scale(0.4D));
            m.setNoGravity(true);
            m.watch(null).act(NorsePose.SEIZED);
            EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.MAGUS_GRASP, m.posX, m.posY, m.posZ, m.getEntityId(), 0, 0,
                    g.getEntityId(), EntityMagus.CRUSH_CLOSE - 8), dim);
            world.playSound(null, m.posX, m.posY + 1.5D, m.posZ, SoundEvents.ENTITY_PLAYER_HURT_DROWN, SoundCategory.HOSTILE, 1.5F, 0.9F);
        });
        t.at(16, () -> StoryDirector.line(player, m, MOTHER, m.getEntityId(), "Adam -", false));
        t.span(10, EntityMagus.CRUSH_CLOSE - 2, k -> {
            Vec3d p = Timeline.lerp(at[0], at[1], Timeline.smooth(k));
            m.setPosition(p.x + rand.nextGaussian() * 0.025D, p.y, p.z + rand.nextGaussian() * 0.025D);
            if (t.now() % 3 == 0) {
                world.spawnParticle(EnumParticleTypes.SPELL_WITCH, m.posX, m.posY + 1.2D, m.posZ, 4, 0.35D, 0.7D, 0.35D, 0.02D);
            }
        });
        t.at(32, () -> world.playSound(null, m.posX, m.posY + 1.5D, m.posZ, SoundEvents.ENTITY_VEX_CHARGE, SoundCategory.HOSTILE, 1.5F, 0.5F));
        int snap = EntityMagus.CRUSH_CLOSE;
        t.at(snap, () -> {
            Vec3d chest = m.getPositionVector().add(0D, 1.2D, 0D);
            Vec3d way = chest.subtract(g.getPositionVector()).normalize();
            EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.MAGUS_IMPACT, chest.x, chest.y, chest.z, way.x, way.y, way.z,
                    m.getEntityId(), 2), dim);
            world.playSound(null, chest.x, chest.y, chest.z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 2.5F, 0.5F);
            world.playSound(null, chest.x, chest.y, chest.z, SoundEvents.ENTITY_LIGHTNING_THUNDER, SoundCategory.HOSTILE, 2F, 1.8F);
            world.playSound(null, chest.x, chest.y, chest.z, SoundEvents.ENTITY_WITHER_BREAK_BLOCK, SoundCategory.HOSTILE, 1.2F, 0.6F);
            // Limp, all at once.
            m.act(NorsePose.DEAD);
            s.motherDead = true;
            s.markDirty();
        });
        t.span(snap, snap + 50, k -> m.setAsh(Timeline.smooth(k)));
        t.span(snap + 2, snap + 9, k -> {
            Vec3d p = Timeline.lerp(at[1], at[0], Timeline.in(k));
            m.setPosition(p.x, p.y, p.z);
        });
        t.at(snap + 9, () -> {
            world.playSound(null, m.posX, m.posY, m.posZ, SoundEvents.ENTITY_PLAYER_BIG_FALL, SoundCategory.HOSTILE, 1.5F, 0.7F);
            world.playSound(null, m.posX, m.posY, m.posZ, SoundEvents.BLOCK_STONE_FALL, SoundCategory.HOSTILE, 1.5F, 0.6F);
            world.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL, m.posX, m.posY + 0.2D, m.posZ, 12, 0.6D, 0.1D, 0.6D, 0.02D);
            g.watch(w);
        });
        t.at(snap + 18, () -> EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.MAGUS_SOUL, m.posX, m.posY + 0.35D, m.posZ,
                0, 0, 0, 0, 120), dim));
        return t;
    }

    /** The fight: Warlock against the Magus, and nobody else in it. It cannot be won. It can be watched. */
    private void begin(EntityPlayerMP player, WorldServer world) {
        EntityMagus g = magus;
        EntityAdamWarlock w = warlock;
        if (g == null || w == null) {
            return;
        }
        fighting = player;
        ending = false;
        duel = new MagusDuel(world, w, g, () -> {
            ending = true;
            end(witness(world), world);
        });
    }

    /** Whoever is still there to see the end of it: the one it was played for, or anyone else in the Sanctum. */
    @Nullable
    private EntityPlayerMP witness(WorldServer world) {
        if (fighting != null && fighting.isEntityAlive() && fighting.world == world && !fighting.hasDisconnected()) {
            return fighting;
        }
        for (EntityPlayer p : world.playerEntities) {
            if (p instanceof EntityPlayerMP && p.isEntityAlive() && SovereignStation.hallAt(p.posX, p.posY, p.posZ) == 6) {
                return (EntityPlayerMP) p;
            }
        }
        return null;
    }

    /**
     * He has had enough. He takes Warlock up off the floor by the throat, one-handed, and
     * takes the stone out of him as if it had always been his; looks at whoever is watching;
     * and folds away into nothing.
     */
    private void end(@Nullable EntityPlayerMP player, WorldServer world) {
        EntityMagus g = magus;
        EntityAdamWarlock w = warlock;
        SovereignData s = SovereignData.get();
        if (g == null || w == null) {
            return;
        }
        int dim = world.provider.getDimension();
        if (player == null) {
            // Nobody left to see it: it happens anyway.
            magusTakes(s);
            windowOpen = false;
            g.setDead();
            magus = null;
            w.puppet(false);
            w.dismiss();
            warlock = null;
            return;
        }
        // Held up at the end of his arm, every tick, until he lets go.
        Timeline hold = new Timeline();
        Vec3d[] from = new Vec3d[1];
        hold.at(0, () -> {
            from[0] = w.getPositionVector();
            g.faceNow(w.getPositionVector().add(0D, 1.2D, 0D));
            g.rest(EntityMagus.Pose.GRIP).perform(EntityMagus.Pose.GRIP);
            g.hoverTo(g.getPositionVector().add(0D, 1.1D, 0D), 0.035D);
            w.perform(EntityAdamWarlock.State.HELD);
            world.playSound(null, w.posX, w.posY + 1D, w.posZ, SoundEvents.ENTITY_PLAYER_HURT, SoundCategory.HOSTILE, 1.5F, 0.6F);
            world.playSound(null, w.posX, w.posY + 1D, w.posZ, SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK, SoundCategory.HOSTILE, 1.5F, 0.5F);
        });
        hold.span(0, 20 * 120, k -> {
            Vec3d hand = g.gripHand();
            float yaw = (float) Math.toRadians(g.rotationYaw);
            Vec3d at = hand.subtract(0D, 1.5D, 0D).add(-Math.sin(yaw) * 0.12D, 0D, Math.cos(yaw) * 0.12D);
            int n = hold.now();
            w.place(n < 8 ? Timeline.lerp(from[0], at, Timeline.out(n / 8F)) : at, g.getPositionEyes(1F));
        });
        playing.add(hold);
        // Held to Warlock, not to him: he leaves before it is over.
        Talk t = new Talk(player).at(w, 96D);
        t.abandoned(() -> {
            hold.stop();
            magusTakes(s);
            windowOpen = false;
            if (magus != null) {
                magus.setDead();
                magus = null;
            }
            if (warlock != null) {
                warlock.setDead();
                warlock = null;
            }
            resolve(player);
        });
        t.pause(14)
                .line(MAGUS, g, "Enough.")
                .direction("He has Warlock by the throat, one-handed, off the floor. Warlock's hands claw at his wrist, and it is like clawing at the station.")
                .line(MAGUS, g, "This was always going to be mine.")
                .run(() -> {
                    g.perform(EntityMagus.Pose.TAKE);
                    Vec3d chest = w.chest();
                    EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.CHAMPION_STONE, chest.x, chest.y, chest.z, 0, 0, 0,
                            g.getEntityId(), 24), dim);
                    EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.MAGUS_GRASP, chest.x, chest.y, chest.z, w.getEntityId(), 0, 0,
                            g.getEntityId(), 30), dim);
                    world.playSound(null, g.posX, g.posY, g.posZ, EternitySounds.OMEN_SHIMMER, SoundCategory.HOSTILE, 3F, 0.4F);
                    world.playSound(null, w.posX, w.posY + 1.5D, w.posZ, SoundEvents.ENTITY_PLAYER_HURT_DROWN, SoundCategory.HOSTILE, 1.2F, 0.8F);
                })
                .pause(34)
                .run(() -> {
                    magusTakes(s);
                    g.crown(true);
                    Vec3d brow = g.brow();
                    EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.MAGUS_IMPACT, brow.x, brow.y, brow.z, 0, 1, 0, -1, 1), dim);
                    world.playSound(null, brow.x, brow.y, brow.z, SoundEvents.ENTITY_ZOMBIE_VILLAGER_CONVERTED, SoundCategory.HOSTILE, 2F, 0.5F);
                })
                .direction("The stone goes into the gem at his brow, and the gem burns.")
                .run(() -> {
                    // He lets go.
                    hold.stop();
                    playing.add(drop(world, w));
                    g.rest(EntityMagus.Pose.FLOAT).perform(EntityMagus.Pose.FLOAT);
                    g.watch(player);
                    g.hoverTo(g.getPositionVector().subtract(0D, 1.1D, 0D), 0.05D);
                })
                .pause(16)
                .line(MAGUS, g, "And you. You are in no version of this I remember. How interesting.")
                .pause(8)
                .run(() -> {
                    g.perform(EntityMagus.Pose.VANISH);
                    world.playSound(null, g.posX, g.posY + 1D, g.posZ, SoundEvents.ENTITY_ENDERMEN_TELEPORT, SoundCategory.HOSTILE, 2F, 0.4F);
                    world.playSound(null, g.posX, g.posY + 1D, g.posZ, EternitySounds.RIFT_IMPACT, SoundCategory.HOSTILE, 2F, 0.7F);
                })
                .pause(EntityMagus.VANISH_TICKS + 4)
                .run(() -> {
                    windowOpen = false;
                    world.playSound(null, SovereignStation.WINDOW, EternitySounds.RIFT_CLOSE, SoundCategory.HOSTILE, 3F, 0.6F);
                    g.setDead();
                    magus = null;
                })
                .direction("He folds into a line of light, and the line goes out. Behind where he stood, the window closes.")
                .pause(20)
                .direction("Through the hole in the roof, the Earth turns. The station is very quiet.")
                .pause(20)
                .run(() -> aftermath(player, world));
        story.talk(player, t);
    }

    /** Let go of, he drops like something thrown away, and lies where he lands. */
    private Timeline drop(WorldServer world, EntityAdamWarlock w) {
        Timeline t = new Timeline();
        Vec3d[] path = new Vec3d[2];
        t.at(0, () -> {
            path[0] = w.getPositionVector();
            path[1] = new Vec3d(w.posX, ground(world, w.posX, w.posY + 1D, w.posZ), w.posZ);
            w.perform(EntityAdamWarlock.State.STAGGER);
        });
        t.span(0, 7, k -> w.place(Timeline.lerp(path[0], path[1], Timeline.in(k)), null));
        t.at(7, () -> {
            w.perform(EntityAdamWarlock.State.DOWNED);
            world.playSound(null, w.posX, w.posY, w.posZ, SoundEvents.ENTITY_PLAYER_BIG_FALL, SoundCategory.HOSTILE, 1.5F, 0.6F);
            world.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL, w.posX, w.posY + 0.2D, w.posZ, 10, 0.6D, 0.1D, 0.6D, 0.02D);
        });
        return t;
    }

    /** The top of whatever is solid under a point. */
    private static double ground(WorldServer world, double x, double y, double z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(new BlockPos(x, y, z));
        for (int i = 0; i < 40 && pos.getY() > 0; i++) {
            IBlockState state = world.getBlockState(pos);
            net.minecraft.util.math.AxisAlignedBB box = state.getCollisionBoundingBox(world, pos);
            if (box != null && box != net.minecraft.block.Block.NULL_AABB) {
                return pos.getY() + box.maxY;
            }
            pos.move(net.minecraft.util.EnumFacing.DOWN);
        }
        return y - 1D;
    }

    /** The Magus has the Power Stone, now, and nobody else can claim it. */
    private void magusTakes(SovereignData s) {
        // His brow holds it now; the claim stays his, and no other copy will ever count.
        if (!s.stone.isEmpty()) {
            Relics.release(Relics.Kind.POWER_STONE);
            Relics.claim(s.stone, MAGUS_ID, "The Magus");
            s.stone = net.minecraft.item.ItemStack.EMPTY;
        }
        s.magusHasPower = true;
        s.station = SovereignData.Station.AFTER;
        s.markDirty();
        story.sendStateToAll();
    }

    /**
     * No words. Warlock gets up, and goes to her, and goes down on his knees beside her; and
     * after a while what is in him has nowhere to go but out. It goes up out of him all at
     * once, and he goes with it, straight up through what is left of the Sanctum and on into
     * the dark over the world, and takes most of the station with him. Everyone watching is
     * taken up after him, to where he went, and the title.
     * <p>
     * He is their ally after it (he backs them up from here on) without a word ever being said.
     */
    private void aftermath(EntityPlayerMP player, WorldServer world) {
        EntityAdamWarlock w = warlock;
        EntityNorse m = mother;
        if (w == null || m == null) {
            resolve(player);
            return;
        }
        // Down on his knees on her near side, facing her.
        Vec3d her = m.getPositionVector();
        Vec3d toward = new Vec3d(SovereignStation.SANCTUM_X - her.x, 0D, -her.z);
        toward = toward.lengthSquared() < 1.0E-3 ? new Vec3d(-1D, 0D, 0D) : toward.normalize();
        Vec3d spot = her.add(toward.scale(1.3D));
        Vec3d kneel = new Vec3d(spot.x, ground(world, spot.x, her.y + 1.5D, spot.z), spot.z);
        Vec3d gone = kneel.add(0D, EntityAdamWarlock.ASCENT_HEIGHT, 0D);
        w.puppet(false);
        w.spent();
        Talk t = new Talk(player).at(player, 1000D);
        t.abandoned(() -> {
            if (warlock != null) {
                warlock.setDead();
                warlock = null;
            }
            resolve(player);
        });
        t.run(() -> w.direct(EntityAdamWarlock.State.STAND, kneel, her, 0.07D))
                .direction("Warlock gets up. It takes him a long time.")
                .pause(20)
                .direction("He goes to her, and goes down on his knees beside her.")
                .pause(10)
                .run(() -> {
                    w.puppet(true);
                    w.place(kneel, her.add(0D, 0.3D, 0D));
                    w.perform(EntityAdamWarlock.State.KNEEL);
                    playing.add(ascent(world, w, m, kneel, gone));
                    story.scene(player, ASCENT_SCENE, kneel, her);
                })
                .until(() -> story.sceneDone(player, ASCENT_SCENE), EntityAdamWarlock.ASCENT_LENGTH + 80)
                .pause(20)
                .direction("Where the Sanctum was, there is mostly sky. Somewhere out in it, he is still going.")
                .run(() -> resolve(player));
        story.talk(player, t);
    }

    /** The cutscene that follows Warlock up, as the client knows it. */
    private static final int ASCENT_SCENE = 3;

    /**
     * It builds in him where he kneels: power streaming in, the floor cracking, the station's
     * lights bursting one after another; he throws his head back; and then he goes, and the
     * Sanctum and everything round it goes with him.
     */
    private Timeline ascent(WorldServer world, EntityAdamWarlock w, EntityNorse m, Vec3d kneel, Vec3d gone) {
        Timeline t = new Timeline();
        int dim = world.provider.getDimension();
        int burst = EntityAdamWarlock.KNEEL_BURST, rise = EntityAdamWarlock.ASCENT_RISE;
        List<BlockPos> ruin = new ArrayList<>();
        for (int tick = 20; tick < burst; tick += 12) {
            int at = tick;
            t.at(at, () -> {
                Vec3d chest = w.chest();
                float k = at / (float) burst;
                EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.WARLOCK_CHARGE, chest.x, chest.y, chest.z, 3D + 5D * k, 0, 0,
                        w.getEntityId(), 14), dim);
                world.playSound(null, kneel.x, kneel.y, kneel.z, EternitySounds.AMBIENT_RUMBLE, SoundCategory.HOSTILE, 1F + 2F * k, 0.6F + 0.3F * k);
                if (at >= EntityAdamWarlock.KNEEL_RAGE) {
                    EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.WARLOCK_LAND, kneel.x, kneel.y + 0.3D, kneel.z, 2D + 7D * k, 0, 0,
                            0, 18), dim);
                    world.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL, kneel.x, kneel.y + 0.2D, kneel.z, (int) (10 * k), 1.2D * k, 0.1D,
                            1.2D * k, 0.05D);
                }
            });
        }
        // The station's lights go, one after another, outward from him.
        t.at(EntityAdamWarlock.KNEEL_RAGE + 20, () -> {
            List<BlockPos> lights = new ArrayList<>();
            for (BlockPos l : SovereignStation.lights()) {
                if (l.distanceSq(kneel.x, kneel.y, kneel.z) < 30D * 30D) {
                    lights.add(l);
                }
            }
            lights.sort((p, q) -> Double.compare(p.distanceSq(kneel.x, kneel.y, kneel.z), q.distanceSq(kneel.x, kneel.y, kneel.z)));
            for (int i = 0; i < lights.size(); i++) {
                BlockPos l = lights.get(i);
                int when = i * 30 / Math.max(1, lights.size());
                t.at(t.now() + 1 + when, () -> {
                    if (world.getBlockState(l).getBlock() == EternityBlocks.SOVEREIGN_LIGHT) {
                        world.setBlockState(l, EternityBlocks.SOVEREIGN_LIGHT.of(false), 2);
                        world.playSound(null, l, SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.BLOCKS, 0.8F, 0.7F + rand.nextFloat() * 0.4F);
                        world.spawnParticle(EnumParticleTypes.FIREWORKS_SPARK, l.getX() + 0.5D, l.getY() + 0.5D, l.getZ() + 0.5D, 6, 0.2D, 0.2D, 0.2D, 0.08D);
                    }
                });
            }
        });
        t.at(burst, () -> {
            Vec3d chest = w.chest();
            w.perform(EntityAdamWarlock.State.RISE);
            w.evolve();
            EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.WARLOCK_EMERGE, chest.x, chest.y, chest.z, 16D, 0, 0, w.getEntityId(), 50), dim);
            EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.WARLOCK_BOOM, chest.x, chest.y, chest.z, 0, 1, 0, 0, 30), dim);
            EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.MAGUS_BREACH, kneel.x, kneel.y + 5D, kneel.z, 22D, 1D, 0, 0, 60), dim);
            world.playSound(null, chest.x, chest.y, chest.z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 8F, 0.45F);
            world.playSound(null, chest.x, chest.y, chest.z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 8F, 0.7F);
            world.playSound(null, chest.x, chest.y, chest.z, EternitySounds.COMET_BLAST, SoundCategory.HOSTILE, 8F, 0.9F);
            world.playSound(null, chest.x, chest.y, chest.z, SoundEvents.ENTITY_LIGHTNING_THUNDER, SoundCategory.HOSTILE, 8F, 0.6F);
            world.playSound(null, chest.x, chest.y, chest.z, SoundEvents.ENTITY_ENDERDRAGON_GROWL, SoundCategory.HOSTILE, 3F, 1.4F);
            ruin.addAll(ruin(world, kneel, m));
        });
        t.span(burst, burst + rise, k -> w.place(Timeline.lerp(kneel, gone, Timeline.in(k)), gone.add(0D, 10D, 0D)));
        t.at(burst + rise, () -> {
            EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.WARLOCK_BOOM, gone.x, gone.y, gone.z, 0, 1, 0, 0, 30), dim);
            w.setDead();
            if (warlock == w) {
                warlock = null;
            }
        });
        // Most of the station goes, outward from where he knelt.
        int spread = 50;
        int[] next = {0};
        t.span(burst, burst + spread, k -> {
            double reach = 36D * Timeline.out(k);
            Vec3d middle = kneel.add(0D, 4D, 0D);
            while (next[0] < ruin.size() && ruin.get(next[0]).distanceSq(kneel.x, kneel.y, kneel.z) <= reach * reach) {
                BlockPos p = ruin.get(next[0]++);
                IBlockState state = world.getBlockState(p);
                if (state.getBlock() == net.minecraft.init.Blocks.AIR) {
                    continue;
                }
                world.setBlockState(p, net.minecraft.init.Blocks.AIR.getDefaultState(), 2);
                if (next[0] % 9 == 0 && next[0] < 9 * 380) {
                    Vec3d away = new Vec3d(p).add(0.5D, 0.5D, 0.5D).subtract(middle).normalize().scale(0.6D + rand.nextDouble() * 0.9D);
                    net.minecraft.entity.item.EntityFallingBlock debris = new net.minecraft.entity.item.EntityFallingBlock(world, p.getX() + 0.5D,
                            p.getY(), p.getZ() + 0.5D, state);
                    debris.setNoGravity(true);
                    debris.shouldDropItem = false;
                    debris.fallTime = 1;
                    debris.motionX = away.x;
                    debris.motionY = away.y + 0.2D;
                    debris.motionZ = away.z;
                    world.spawnEntity(debris);
                } else if (next[0] % 4 == 0) {
                    world.spawnParticle(EnumParticleTypes.BLOCK_CRACK, p.getX() + 0.5D, p.getY() + 0.5D, p.getZ() + 0.5D, 3, 0.3D, 0.3D, 0.3D,
                            0.2D, net.minecraft.block.Block.getStateId(state));
                }
            }
        });
        return t;
    }

    /**
     * What goes when he goes: everything of the station above its floors within reach of
     * where he knelt, nearest first; but never the ground under anyone, nor her, nor where
     * she lies.
     */
    private static List<BlockPos> ruin(WorldServer world, Vec3d from, EntityNorse m) {
        List<BlockPos> out = new ArrayList<>();
        int r = 36;
        for (int x = (int) from.x - r; x <= from.x + r; x++) {
            for (int z = (int) from.z - r; z <= from.z + r; z++) {
                for (int y = SovereignStation.FLOOR + 1; y <= SovereignStation.FLOOR + 30; y++) {
                    double dx = x + 0.5D - from.x, dz = z + 0.5D - from.z;
                    if (dx * dx + dz * dz > r * r || SovereignStation.blockAt(x, y, z) == null) {
                        continue;
                    }
                    if (m.getDistanceSq(x + 0.5D, y + 0.5D, z + 0.5D) < 4D * 4D) {
                        continue;
                    }
                    boolean near = false;
                    for (EntityPlayer p : world.playerEntities) {
                        if (p.getDistanceSq(x + 0.5D, y + 0.5D, z + 0.5D) < 3.5D * 3.5D) {
                            near = true;
                            break;
                        }
                    }
                    if (!near) {
                        out.add(new BlockPos(x, y, z));
                    }
                }
            }
        }
        out.sort((p, q) -> Double.compare(p.distanceSq(from.x, from.y, from.z), q.distanceSq(from.x, from.y, from.z)));
        return out;
    }

    /** What the Sanctum leaves them with: Warlock their ally from here on, the Magus known, and the way on. */
    private void resolve(EntityPlayerMP player) {
        alliance(player);
        if (!story.data().player(player.getUniqueID()).seen("champion_debt")) {
            story.mark(player, "champion_debt");
        }
    }

    /** Everyone who was there is Warlock's ally now, and knows what the Magus is. */
    private void alliance(EntityPlayerMP player) {
        WorldServer world = player.getServerWorld();
        for (EntityPlayer o : world.playerEntities) {
            if (o instanceof EntityPlayerMP && SovereignStation.hallAt(o.posX, o.posY, o.posZ) == 6 || o == player) {
                StoryData.Player sp = story.data().player(o.getUniqueID());
                sp.warlock = StoryData.Stance.ALLY;
                sp.beats.add("magus_risen");
            }
        }
        story.data().markDirty();
        story.sendStateToAll();
    }

    /** The scene cut off (they left, or died): everyone in it goes, and it can start again. */
    private void abandon(WorldServer world) {
        SovereignData s = SovereignData.get();
        if (s.station == SovereignData.Station.CONFRONTED && !s.motherDead) {
            s.station = SovereignData.Station.ABANDONED;
            s.markDirty();
            stopped = false;
            windowOpen = false;
            playing.clear();
            if (duel != null) {
                duel.stop();
                duel = null;
            }
            for (net.minecraft.entity.Entity e : new net.minecraft.entity.Entity[]{warlock, mother, chaos, magus}) {
                if (e != null) {
                    e.setDead();
                }
            }
            warlock = null;
            mother = null;
            chaos = null;
            magus = null;
        }
    }

    /**
     * Off the edge of the ring is a long way down to the Earth, and nobody meant to go is let go:
     * the station's own field catches anyone (anything) falling away from it and sets them back on
     * the landing ring. Every tick, for every player, before anything else can take them anywhere.
     */
    static void catchFalling(EntityPlayer p) {
        if (p.world.isRemote || p.dimension != SovereignStation.dimension() || p.posY >= SovereignStation.FLOOR - 24 || p.isSpectator()) {
            return;
        }
        if (p.isRiding()) {
            p.dismountRidingEntity();
        }
        p.motionX = p.motionY = p.motionZ = 0D;
        p.fallDistance = 0F;
        p.setPositionAndUpdate(SovereignStation.LANDING.getX() + 0.5D, SovereignStation.LANDING.getY() + 0.5D, SovereignStation.LANDING.getZ() + 0.5D);
        p.world.playSound(null, p.posX, p.posY, p.posZ, SoundEvents.ENTITY_ENDERMEN_TELEPORT, SoundCategory.PLAYERS, 1F, 0.6F);
        if (p instanceof EntityPlayerMP) {
            StoryDirector.line((EntityPlayerMP) p, p, Speakers.DIRECTION, -1, "Something in the station takes hold of you as you fall, and sets you back on the ring.");
        }
    }

    // ------------------------------------------------------------------ arriving

    private void arrived(EntityPlayerMP player) {
        StoryData.Player p = story.data().player(player.getUniqueID());
        if (p.seen("sov_arrived") || story.talking(player) || player.ticksExisted < 40) {
            return;
        }
        story.mark(player, "sov_arrived");
        SovereignData s = SovereignData.get();
        if (s.station == SovereignData.Station.UNSEEN) {
            s.station = SovereignData.Station.ABANDONED;
            s.markDirty();
        }
        Talk t = new Talk(player);
        t.pause(30)
                .direction("The ring takes you in without asking who you are. Nobody comes out to meet you.")
                .pause(30)
                .direction("Gold, everywhere, all of it polished. None of it has been looked at in a long time.")
                .pause(20)
                .direction("Somewhere deep in the station, the stone you came for is burning. You can feel it from here.");
        story.talk(player, t);
    }

    // ------------------------------------------------------------------ the records, and the capsules

    /** Someone lays a hand on a console: her voice, as it was kept. */
    boolean readConsole(EntityPlayerMP player, BlockPos pos) {
        String record = SovereignStation.consoleAt(pos);
        if (record == null || story.talking(player)) {
            return record != null;
        }
        String[] lines = RECORDS.get(record);
        if (lines == null) {
            return true;
        }
        player.world.playSound(null, pos, SoundEvents.BLOCK_NOTE_CHIME, SoundCategory.BLOCKS, 0.8F, 1.6F);
        Talk t = new Talk(player).at(player, 12D);
        t.direction("The console wakes under your hand. Her voice, kept here.");
        for (String line : lines) {
            t.line(RECORD, null, line).pause(10);
        }
        if ("his_cocoon".equals(record)) {
            t.direction("The great cocoon behind you is split from top to bottom. Whatever was put back in it got out again.");
        } else if ("vats".equals(record)) {
            t.direction("Behind you, in the dark at the edge of the room, something knocks once on glass.");
        }
        t.run(() -> story.mark(player, "sov_" + record));
        story.talk(player, t);
        return true;
    }

    /** Someone looks into a capsule in the gallery: a plaque, and who is asleep inside. */
    boolean readPod(EntityPlayerMP player, BlockPos pos) {
        String pod = SovereignStation.podAt(pos);
        if (pod == null || story.talking(player)) {
            return pod != null;
        }
        int i = Integer.parseInt(pod.substring(pod.lastIndexOf('_') + 1)) % PLAQUES.length;
        WorldServer world = player.getServerWorld();
        EntityMagus sleeper = sleeper(world, pos);
        // Nothing said about him: the frost clears, the view goes to his face, and he looks back.
        Talk t = new Talk(player).at(player, 12D);
        t.run(() -> frost(world, pos, false))
                .pause(12)
                .line(Speakers.STONE, sleeper, PLAQUES[i])
                .pause(30)
                .run(() -> {
                    if (sleeper != null) {
                        sleeper.watch(player).act(NorsePose.STIR);
                        world.playSound(null, pos, SoundEvents.BLOCK_GLASS_HIT, SoundCategory.BLOCKS, 0.6F, 0.5F);
                    }
                })
                .pause(60)
                .run(() -> {
                    if (sleeper != null) {
                        sleeper.watch(null).act(NorsePose.SLEEP);
                    }
                })
                .pause(25)
                .run(() -> frost(world, pos, true))
                .run(() -> story.mark(player, "sov_" + pod));
        t.abandoned(() -> {
            frost(world, pos, true);
            if (sleeper != null) {
                sleeper.watch(null).act(NorsePose.SLEEP);
            }
        });
        story.talk(player, t);
        return true;
    }

    /** The frost on a capsule's glass: cleared for whoever has come to look, and back when they have looked. */
    private static void frost(WorldServer world, BlockPos foot, boolean frosted) {
        for (int part = 0; part < 3; part++) {
            BlockPos at = foot.up(part);
            if (world.getBlockState(at).getBlock() == EternityBlocks.POD) {
                world.setBlockState(at, EternityBlocks.POD.part(part, !frosted), 2);
            }
        }
    }

    /** The sleeper in a capsule: there whenever anyone is near enough to see him, standing exactly where he was grown. */
    @Nullable
    private EntityMagus sleeper(WorldServer world, BlockPos foot) {
        EntityMagus s = sleepers.get(foot);
        if (s != null && !s.isDead && s.world == world) {
            return s;
        }
        net.minecraft.util.EnumFacing facing = SovereignStation.pods().get(foot);
        if (facing == null) {
            return null;
        }
        s = new EntityMagus(world);
        s.sleeper = true;
        s.setLocationAndAngles(foot.getX() + 0.5D, foot.getY() + 0.19D, foot.getZ() + 0.5D, facing.getHorizontalAngle(), 0F);
        s.faceNow(s.getPositionVector().add(-Math.sin(Math.toRadians(facing.getHorizontalAngle())), 1.95D, Math.cos(Math.toRadians(facing.getHorizontalAngle()))));
        s.act(NorsePose.SLEEP).rest(NorsePose.SLEEP);
        world.spawnEntity(s);
        sleepers.put(foot, s);
        return s;
    }

    /** Keeps the gallery's sleepers in their capsules while anyone is near, and gone when nobody is. */
    private void sleepers(WorldServer world) {
        if (StoryDirector.now() % 20 != 0) {
            return;
        }
        for (BlockPos foot : SovereignStation.pods().keySet()) {
            boolean near = world.getClosestPlayer(foot.getX() + 0.5D, foot.getY(), foot.getZ() + 0.5D, 48D, false) != null;
            EntityMagus s = sleepers.get(foot);
            if (near && world.isBlockLoaded(foot)) {
                s = sleeper(world, foot);
                if (s != null && s.getDistanceSq(foot.getX() + 0.5D, foot.getY() + 0.19D, foot.getZ() + 0.5D) > 0.01D) {
                    s.setPosition(foot.getX() + 0.5D, foot.getY() + 0.19D, foot.getZ() + 0.5D);
                }
            } else if (s != null) {
                s.setDead();
                sleepers.remove(foot);
            }
        }
    }

    static int podsSeen(StoryData.Player p) {
        return pods(p);
    }

    static int archiveRead(StoryData.Player p) {
        return archive(p);
    }

    private static int pods(StoryData.Player p) {
        int n = 0;
        for (String b : p.beats) {
            if (b.startsWith("sov_ascended_")) {
                n++;
            }
        }
        return n;
    }

    private static int archive(StoryData.Player p) {
        int n = 0;
        for (String r : new String[]{"subjects", "rejected", "recycled", "the_one_who_left", "him", "church"}) {
            if (p.seen("sov_" + r)) {
                n++;
            }
        }
        return n;
    }

    // ------------------------------------------------------------------ the doors

    /** Whether someone has seen enough on this side of a door for it to let them through. */
    static boolean ready(String door, StoryData.Player p) {
        switch (door) {
            case "atrium":
                return true;
            case "cocoons":
                return p.seen("sov_welcome");
            case "vats":
                return p.seen("sov_his_cocoon");
            case "archive":
                return p.seen("sov_vats") && p.seen("sov_spawn_cleared");
            case "gallery":
                return archive(p) >= 3;
            case "sanctum":
                return pods(p) >= 2;
            default:
                return false;
        }
    }

    /** Each door lights once anyone aboard is ready for it. */
    private void lightDoors(WorldServer world) {
        SovereignData s = SovereignData.get();
        for (SovereignStation.Door door : SovereignStation.doors().values()) {
            if (s.opened.contains(door.name)) {
                continue;
            }
            boolean anyone = false;
            for (EntityPlayer p : world.playerEntities) {
                anyone |= ready(door.name, story.data().player(p.getUniqueID()));
            }
            for (BlockPos pos : door.blocks) {
                IBlockState state = world.getBlockState(pos);
                if (state.getBlock() == EternityBlocks.SOVEREIGN_DOOR && state.getValue(BlockSovereignDoor.LIT) != anyone) {
                    world.setBlockState(pos, EternityBlocks.SOVEREIGN_DOOR.of(anyone), 2);
                    if (anyone && pos.equals(door.blocks.get(door.blocks.size() / 2))) {
                        world.playSound(null, pos, SoundEvents.BLOCK_NOTE_CHIME, SoundCategory.BLOCKS, 1.5F, 0.8F);
                    }
                }
            }
        }
    }

    /** A hand on a door: open, if it is lit; if not, it does not so much as tremble. */
    void touchDoor(EntityPlayerMP player, BlockPos pos) {
        for (SovereignStation.Door door : SovereignStation.doors().values()) {
            if (!door.blocks.contains(pos)) {
                continue;
            }
            if (!ready(door.name, story.data().player(player.getUniqueID()))) {
                StoryDirector.line(player, player, Speakers.DIRECTION, -1, "It doesn't move. Nothing in the station is ready to let you further in.");
                return;
            }
            open(player.getServerWorld(), door);
            return;
        }
    }

    void open(WorldServer world, SovereignStation.Door door) {
        SovereignData s = SovereignData.get();
        s.opened.add(door.name);
        s.markDirty();
        for (BlockPos pos : door.blocks) {
            world.setBlockToAir(pos);
        }
        BlockPos c = door.centre();
        world.playSound(null, c, SoundEvents.BLOCK_PISTON_CONTRACT, SoundCategory.BLOCKS, 1.5F, 0.6F);
        world.playSound(null, c, EternitySounds.OMEN_SHIMMER, SoundCategory.BLOCKS, 1F, 1.4F);
    }

    // ------------------------------------------------------------------ the failing light

    /** Here and there a seam of light stutters, goes out, comes back: the station's power is going. */
    private void flicker(WorldServer world) {
        long now = StoryDirector.now();
        List<BlockPos> back = new ArrayList<>();
        for (Map.Entry<BlockPos, Long> e : failing.entrySet()) {
            if (now >= e.getValue()) {
                back.add(e.getKey());
            }
        }
        for (BlockPos pos : back) {
            failing.remove(pos);
            IBlockState state = world.getBlockState(pos);
            if (state.getBlock() == EternityBlocks.SOVEREIGN_LIGHT) {
                world.setBlockState(pos, EternityBlocks.SOVEREIGN_LIGHT.of(true), 2);
            }
        }
        if (now % 15 != 0 || failing.size() > 3) {
            return;
        }
        List<BlockPos> lights = SovereignStation.lights();
        for (int tries = 0; tries < 4; tries++) {
            BlockPos pos = lights.get(rand.nextInt(lights.size()));
            EntityPlayer near = world.getClosestPlayer(pos.getX(), pos.getY(), pos.getZ(), 24D, false);
            IBlockState state = world.getBlockState(pos);
            if (near != null && state.getBlock() == EternityBlocks.SOVEREIGN_LIGHT && state.getValue(BlockSovereignLight.LIT)) {
                world.setBlockState(pos, EternityBlocks.SOVEREIGN_LIGHT.of(false), 2);
                failing.put(pos, now + 3 + rand.nextInt(rand.nextInt(4) == 0 ? 60 : 8));
                return;
            }
        }
    }

    // ------------------------------------------------------------------ for testing: the scene put back, or skipped to

    /** Everyone of the scene gone from the Sanctum, nothing playing, and the room as it was built. */
    private void clearScene() {
        WorldServer world = world();
        playing.clear();
        if (duel != null) {
            duel.stop();
            duel = null;
        }
        windowOpen = stopped = ending = false;
        warlock = null;
        mother = null;
        chaos = null;
        magus = null;
        if (world == null) {
            return;
        }
        for (net.minecraft.entity.Entity e : new ArrayList<>(world.loadedEntityList)) {
            boolean ours = e instanceof EntityAdamWarlock || e instanceof EntityOrderChaos
                    || e instanceof EntityMagus && !((EntityMagus) e).sleeper
                    || e instanceof EntityNorse && "mother".equals(((EntityNorse) e).role)
                    || e instanceof net.minecraft.entity.item.EntityFallingBlock;
            if (ours && SovereignStation.hallAt(e.posX, Math.min(e.posY, SovereignStation.FLOOR + 20), e.posZ) == 6) {
                e.setDead();
            }
        }
        SovereignStation.restoreSanctum(world);
    }

    /** Warlock and the Magus where the scene leaves them when the fight begins; his mother dead on her dais, or about to be. */
    private void stageDuel(EntityPlayerMP player, WorldServer world, boolean fromHerDeath) {
        SovereignData s = SovereignData.get();
        s.station = SovereignData.Station.CONFRONTED;
        s.markDirty();
        BlockPos dais = SovereignStation.DAIS, throne = SovereignStation.THRONE, win = SovereignStation.WINDOW;
        EntityNorse m = mother = EntityNorse.of(world, NorseBoxes.AYESHA, "mother");
        m.setLocationAndAngles(throne.getX() - 1.5D, ground(world, throne.getX() - 1.5D, throne.getY() + 2D, throne.getZ() + 0.5D), throne.getZ() + 0.5D, 90F, 0F);
        world.spawnEntity(m);
        EntityAdamWarlock w = warlock = new EntityAdamWarlock(world);
        w.script(true);
        Vec3d warlockAt = new Vec3d(dais.getX() - 3.5D, dais.getY(), dais.getZ() + 0.5D);
        w.setPosition(warlockAt.x, warlockAt.y, warlockAt.z);
        world.spawnEntity(w);
        w.direct(EntityAdamWarlock.State.STAND, warlockAt, m.getPositionEyes(1F), 0.2D);
        EntityMagus g = magus = new EntityMagus(world);
        g.setPosition(win.getX() + 3.5D, win.getY() - 2.5D, win.getZ() + 0.5D);
        world.spawnEntity(g);
        g.rest(EntityMagus.Pose.FLOAT).perform(EntityMagus.Pose.FLOAT);
        windowOpen = true;
        fighting = player;
        if (fromHerDeath) {
            g.watch(m);
            Talk t = new Talk(player).at(m, 64D);
            t.pause(20)
                    .line(MAGUS, g, "I have no mother. I have a church.")
                    .run(() -> playing.add(motherDies(player, world, g, m, w, s)))
                    .pause(EntityMagus.CRUSH_CLOSE + 26)
                    .direction("She hits the floor of her own Sanctum and does not move again. The gold goes out of her skin like the light going out of a window.")
                    .pause(20)
                    .line(WARLOCK, w, "...Mother?")
                    .pause(25)
                    .run(() -> begin(player, world));
            story.talk(player, t);
            return;
        }
        m.setNoGravity(true);
        m.act(NorsePose.DEAD);
        m.setAsh(1F);
        s.motherDead = true;
        s.markDirty();
        g.watch(w);
        begin(player, world);
    }


    // ------------------------------------------------------------------ for the operator

    @Nullable
    String command(EntityPlayerMP player, String action, String[] args) {
        if (!"sovereign".equals(action)) {
            return null;
        }
        String what = args.length > 0 ? args[0] : "status";
        SovereignData s = SovereignData.get();
        StoryData.Player p = story.data().player(player.getUniqueID());
        switch (what) {
            case "visit":
                com.matoon.eternity.world.Teleport.player(player, SovereignStation.dimension(), SovereignStation.LANDING.getX() + 0.5D,
                        SovereignStation.LANDING.getY() + 0.1D, SovereignStation.LANDING.getZ() + 0.5D, false);
                return "Aboard the Sovereign.";
            case "open": {
                WorldServer world = world();
                if (world == null) {
                    return "Nobody is on the station.";
                }
                int n = 0;
                for (String door : ORDER) {
                    if (args.length > 1 && !door.equals(args[1])) {
                        continue;
                    }
                    open(world, SovereignStation.doors().get(door));
                    n++;
                }
                return n + " doors opened.";
            }
            case "stage": {
                // stage: as if Warlock had just fled home with the Power Stone off the Champion, and they had been shown where.
                clearScene();
                s.station = SovereignData.Station.ABANDONED;
                s.motherDead = false;
                s.magusHasPower = false;
                s.opened.clear();
                if (s.stone.isEmpty() && anvil.infinity.items.Items.POWER_STONE != null) {
                    net.minecraft.item.ItemStack stone = new net.minecraft.item.ItemStack(anvil.infinity.items.Items.POWER_STONE);
                    Relics.release(Relics.Kind.POWER_STONE);
                    Relics.claim(stone, Arena.WARLOCK_ID, "Adam Warlock");
                    s.stone = stone;
                    s.takenFrom = "the Champion";
                }
                s.markDirty();
                p.beats.removeIf(b -> b.startsWith("sov_") || "magus_risen".equals(b));
                p.beats.add("death_revealed");
                p.beats.add("warlock_took");
                p.beats.add("dir_station");
                story.data().markDirty();
                return "Staged: Warlock is home with the Power Stone.";
            }
            case "duel": {
                // duel: straight to Warlock and the Magus, his mother already dead, with the stone in Warlock's hand.
                WorldServer world = world();
                if (world == null || player.world != world) {
                    return "Go aboard first.";
                }
                command(player, "sovereign", new String[]{"stage"});
                stageDuel(player, world, args.length > 1 && "death".equals(args[1]));
                return "Duel.";
            }
            case "poses": {
                // poses: a row of the Magus in each of his poses down the Sanctum, Warlock in his new ones, and her seized, for looking at.
                WorldServer world = world();
                if (world == null || player.world != world) {
                    return "Go aboard first.";
                }
                clearScene();
                // Nobody's scene starts while they stand among these.
                s.station = SovereignData.Station.AFTER;
                s.markDirty();
                String[] mine = {"FLOAT", "STAND", "REGARD", "GUARD", "BLOCK", "CATCH", "GRIP", "TAKE", "DESCEND", "POINT", "FLUNG", "SLEEP"};
                double x = SovereignStation.SANCTUM_X - 10D;
                for (int i = 0; i < mine.length; i++) {
                    EntityMagus g = new EntityMagus(world);
                    g.setPosition(x + i * 2D, SovereignStation.FLOOR + 1.5D, -7D);
                    world.spawnEntity(g);
                    EntityMagus.Pose pose = EntityMagus.Pose.valueOf(mine[i]);
                    g.rest(pose).perform(pose);
                    g.faceNow(new Vec3d(g.posX, g.posY + 1.9D, 5D));
                }
                Vec3d eye = new Vec3d(x - 6D, SovereignStation.FLOOR + 2.6D, 0D);
                String[] his = {"FLARE", "CAUGHT", "HELD", "DOWNED"};
                for (int i = 0; i < his.length; i++) {
                    EntityAdamWarlock w = new EntityAdamWarlock(world);
                    w.setPosition(x + 2D + i * 3D, SovereignStation.FLOOR + (i == 3 ? 1D : 2D), 7D);
                    world.spawnEntity(w);
                    w.puppet(true);
                    if (i == 0) {
                        w.evolve();
                    }
                    w.perform(EntityAdamWarlock.State.valueOf(his[i]));
                    w.place(w.getPositionVector(), new Vec3d(w.posX, w.posY + 1.6D, -5D));
                }
                EntityNorse m = EntityNorse.of(world, NorseBoxes.AYESHA, "mother");
                m.setLocationAndAngles(x + 14D, SovereignStation.FLOOR + 2.5D, 7D, 180F, 0F);
                m.setNoGravity(true);
                world.spawnEntity(m);
                m.act(NorsePose.SEIZED);
                EntityNorse dead = EntityNorse.of(world, NorseBoxes.AYESHA, "mother");
                dead.setLocationAndAngles(x + 11D, SovereignStation.FLOOR + 1D, 10D, 180F, 0F);
                world.spawnEntity(dead);
                dead.act(NorsePose.DEAD);
                dead.setAsh(1F);
                return "Posed.";
            }
            case "yaws": {
                WorldServer world = world();
                StringBuilder out = new StringBuilder();
                if (world != null) {
                    for (net.minecraft.entity.Entity e : world.loadedEntityList) {
                        if (e instanceof EntityMagus || e instanceof EntityAdamWarlock) {
                            out.append(String.format("%s@%.1f,%.2f,%.1f yaw %.0f body %.0f head %.0f; ", e.getClass().getSimpleName().substring(6), e.posX, e.posY, e.posZ,
                                    e.rotationYaw, ((net.minecraft.entity.EntityLivingBase) e).renderYawOffset, e.getRotationYawHead()));
                        }
                    }
                }
                com.matoon.eternity.Eternity.LOG.info("yaws {}", out);
                return out.toString();
            }
            case "ascent": {
                // ascent: straight to the end of it: the Magus gone, his mother dead, Warlock on the floor, and what Warlock does then.
                WorldServer world = world();
                if (world == null || player.world != world) {
                    return "Go aboard first.";
                }
                command(player, "sovereign", new String[]{"stage"});
                stageDuel(player, world, false);
                if (duel != null) {
                    duel.stop();
                    duel = null;
                }
                if (magus != null) {
                    magus.setDead();
                    magus = null;
                }
                windowOpen = false;
                magusTakes(SovereignData.get());
                warlock.puppet(true);
                warlock.perform(EntityAdamWarlock.State.DOWNED);
                warlock.place(new Vec3d(SovereignStation.SANCTUM_X + 3.5D, SovereignStation.FLOOR + 1D, 1.5D), null);
                aftermath(player, world);
                return "Ascent.";
            }
            case "look": {
                // look: as if their hand were on the nearest capsule's glass.
                BlockPos best = null;
                for (BlockPos foot : SovereignStation.pods().keySet()) {
                    if (best == null || player.getDistanceSq(foot) < player.getDistanceSq(best)) {
                        best = foot;
                    }
                }
                return best != null && readPod(player, best) ? "Looking." : "No capsule.";
            }
            case "seen":
                // seen <beat...>: as if they had read those, for jumping ahead.
                for (int i = 1; i < args.length; i++) {
                    p.beats.add("sov_" + args[i]);
                }
                story.data().markDirty();
                return "Seen.";
            default:
                return "Station " + s.station + ", Warlock " + (s.warlockHasStone() ? "has the stone" : "has nothing") + ", opened " + s.opened
                        + ", read " + archive(p) + " records, " + pods(p) + " capsules.";
        }
    }
}
