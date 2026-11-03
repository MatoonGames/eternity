package com.matoon.eternity.story;

import com.matoon.eternity.EternityConfig;
import com.matoon.eternity.block.BlockBarrowSeal;
import com.matoon.eternity.block.EternityBlocks;
import com.matoon.eternity.compat.Relics;
import com.matoon.eternity.client.render.NorseBoxes;
import com.matoon.eternity.entity.EntityDraugr;
import com.matoon.eternity.entity.EntityNorse;
import com.matoon.eternity.entity.NorsePose;
import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.network.MsgStory;
import com.matoon.eternity.world.OdinsVault;
import com.matoon.eternity.world.VaultDirector;
import lucraft.mods.heroesexpansion.HEConfig;
import lucraft.mods.heroesexpansion.items.HEItems;
import lucraft.mods.heroesexpansion.worldgen.norsevillage.MapGenNorseVillage;
import net.minecraft.advancements.Advancement;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
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

import static com.matoon.eternity.story.Speakers.DIRECTION;
import static com.matoon.eternity.story.Speakers.HRAFN;
import static com.matoon.eternity.story.Speakers.RAGNA;
import static com.matoon.eternity.story.Speakers.STRANGER;
import static com.matoon.eternity.story.Talk.act;
import static com.matoon.eternity.story.Talk.say;

/**
 * Chapter 1, the oath. Odin sleeps, and the ward on the cube his people keep under their mound
 * is failing: pieces of the north come loose round it and land across the world, and then a
 * person does. A woman in mourning clothes is already kneeling by her when you come over the
 * rise. She tells you the truth, and not much of it.
 * <p>
 * The village: Ragna's brother, Hrafn, who keeps the oath and means to break it. The doors of
 * the barrow open to his blood and to the oath's own words, which are on his ancestors' stones,
 * one keeper's line on each; the dead the oath binds lie in their graves round the hall. Lift
 * the cube and the oath is broken, and every one of them gets up for the one who broke it, which
 * is Hrafn, who has known that since before he opened the first door.
 * <p>
 * Afterwards: the stranger, kneeling by him; and the cube, showing whoever holds it the room it
 * was kept in before it came here.
 */
final class Oath {

    private static final int RAGNA_SCENE = 1;
    /** How long the dead fight Hrafn with nobody to see it before he falls, and with someone there. */
    private static final int FALL_SEALED = 20 * 18, FALL_WATCHED = 20 * 50;

    private final StoryDirector story;
    private final Random rand = new Random();

    @Nullable
    private EntityNorse ragna, stranger, hrafn;
    private final EntityDraugr[] dead = new EntityDraugr[Barrow.graves()];
    @Nullable
    private Barrow barrow;
    private long nextLook, nextRagna;
    /** The fight below: how long it has gone on, how many blows he has taken, when he can next swing. */
    private int fight, blows, swingIn;
    private long sealedAt = -1;
    /** Who is getting the cube's visions, until when this one lasts, and when the next comes. */
    private final Map<UUID, Long> visionUntil = new HashMap<>(), nextVision = new HashMap<>();
    private final Map<UUID, Long> lastCold = new HashMap<>();

    Oath(StoryDirector story) {
        this.story = story;
    }

    void reset() {
        ragna = stranger = hrafn = null;
        java.util.Arrays.fill(dead, null);
        barrow = null;
        fight = blows = swingIn = 0;
        sealedAt = -1;
        visionUntil.clear();
        nextVision.clear();
    }

    /** Takes everyone the chapter put in the world back out of it. */
    void clear(WorldServer world) {
        for (EntityNorse e : new EntityNorse[]{ragna, stranger, hrafn}) {
            if (e != null) {
                e.setDead();
            }
        }
        for (EntityDraugr d : dead) {
            if (d != null) {
                d.setDead();
            }
        }
        reset();
    }

    private StoryData data() {
        return story.data();
    }

    @Nullable
    private Barrow barrow() {
        StoryData d = data();
        if (d.barrow == null) {
            return null;
        }
        if (barrow == null || !barrow.origin.equals(d.barrow)) {
            barrow = new Barrow(d.barrow, d.barrowFacing);
        }
        return barrow;
    }

    // ------------------------------------------------------------------ ticking

    void tick(MinecraftServer server) {
        WorldServer world = server.getWorld(0);
        if (world == null) {
            return;
        }
        StoryData d = data();
        long now = world.getTotalWorldTime();
        if (!StoryDirector.enabled() && d.oath == StoryData.Oath.DORMANT) {
            return;
        }
        switch (d.oath) {
            case DORMANT:
                if (now % 200 == 0) {
                    begin(world, null, false);
                }
                break;
            case LEAKING:
                leaking(world, now);
                if (arrivedEarly(world)) {
                    break;
                }
                if (now >= nextRagna) {
                    nextRagna = now + 100;
                    tryRagna(world);
                }
                break;
            case RAGNA:
                leaking(world, now);
                if (!arrivedEarly(world)) {
                    ragnaSite(world);
                }
                break;
            case SEEKING:
            case VILLAGE:
                leaking(world, now);
                village(world, now);
                break;
            case BROKEN:
                village(world, now);
                fight(world, now);
                break;
            case SEALED:
                village(world, now);
                sealed(world, now);
                break;
            case MOURNED:
            case DONE:
                village(world, now);
                break;
            default:
                break;
        }
        if (d.oath == StoryData.Oath.MOURNED || d.oath == StoryData.Oath.DONE) {
            visions(server, now);
        }
        // Whoever has wandered away from the stranger, Ragna or the mound no longer keeps them in the world.
        if (now % 20 == 0) {
            tidy(world);
        }
    }

    // ------------------------------------------------------------------ the ward fails

    /**
     * The ward starts failing: the village it is kept in is found (the nearest Norse village to
     * whoever is about), and pieces of the north start coming loose. Returns what happened.
     */
    String begin(WorldServer world, @Nullable EntityPlayerMP near, boolean now) {
        StoryData d = data();
        if (!now && world.getTotalWorldTime() < EternityConfig.story.leakStartDays * 24000L) {
            return "Not yet.";
        }
        if (Relics.isClaimed(Relics.Kind.TESSERACT) && !now) {
            d.oath = StoryData.Oath.SKIPPED;
            d.markDirty();
            return "The Tesseract has already been claimed; the oath's chapter is passed over.";
        }
        if (!HEConfig.worldGeneration.GENERATE_NORSE_VILLAGES) {
            d.oath = StoryData.Oath.SKIPPED;
            d.markDirty();
            return "HeroesExpansion makes no Norse villages in this world; the oath's chapter is passed over.";
        }
        EntityPlayer about = near != null ? near : anyoneOutside(world);
        if (about == null) {
            return "Nobody is about.";
        }
        BlockPos village = new MapGenNorseVillage().getNearestStructurePos(world, new BlockPos(about), false);
        if (village == null) {
            return "No Norse village anywhere near.";
        }
        d.village = new BlockPos(village.getX(), 64, village.getZ());
        d.oath = StoryData.Oath.LEAKING;
        d.leaksFrom = world.getTotalWorldTime();
        d.nextLeak = world.getTotalWorldTime() + (now ? 40 : interval(1000D));
        d.markDirty();
        return "The ward is failing. The village is at " + village.getX() + " " + village.getZ() + ".";
    }

    @Nullable
    private static EntityPlayer anyoneOutside(World world) {
        for (EntityPlayer p : world.playerEntities) {
            if (!p.isSpectator()) {
                return p;
            }
        }
        return null;
    }

    private long interval(double distance) {
        int lo = EternityConfig.story.leakMinutesMin * 1200, hi = Math.max(lo + 1, EternityConfig.story.leakMinutesMax * 1200);
        long t = lo + rand.nextInt(hi - lo);
        // Nearer the village they come thicker.
        double near = MathHelper.clamp(1D - distance / 1500D, 0D, 0.75D);
        return (long) (t * (1D - near));
    }

    private void leaking(WorldServer world, long now) {
        StoryData d = data();
        if (now < d.nextLeak || d.village == null) {
            return;
        }
        List<EntityPlayer> out = new ArrayList<>();
        for (EntityPlayer p : world.playerEntities) {
            if (!p.isSpectator() && p.dimension == 0 && world.canSeeSky(new BlockPos(p).up())) {
                out.add(p);
            }
        }
        if (out.isEmpty()) {
            d.nextLeak = now + 600;
            return;
        }
        EntityPlayerMP p = (EntityPlayerMP) out.get(rand.nextInt(out.size()));
        double toVillage = Math.sqrt(p.getDistanceSq(d.village.getX(), p.posY, d.village.getZ()));
        int reach = EternityConfig.story.leaksReachHome;
        BlockPos home = p.getBedLocation(0);
        if (reach >= 0 && d.leaks >= EternityConfig.story.ragnaAfterLeaks + reach && home != null && d.oath.ordinal() < StoryData.Oath.VILLAGE.ordinal()
                && rand.nextInt(3) == 0 && world.isBlockLoaded(home)) {
            d.taken += Leaks.takeFromHome(world, p, home, rand);
        } else if (toVillage > 40D) {
            double a = rand.nextDouble() * Math.PI * 2D;
            double r = 24D + rand.nextDouble() * 40D;
            BlockPos at = new BlockPos(p.posX + Math.cos(a) * r, p.posY, p.posZ + Math.sin(a) * r);
            if (world.isBlockLoaded(at)) {
                int kind = Leaks.kind(d.leaks, rand);
                boolean eirik = kind == Leaks.STONE && !d.seen("eirik_landed");
                if (eirik) {
                    d.see("eirik_landed");
                }
                int radius = 2 + Math.min(3, d.leaks / 3) + (toVillage < 500D ? 1 : 0);
                Leaks.land(world, at, new Vec3d(d.village), kind, radius, rand, eirik);
            }
        }
        d.leaks++;
        d.nextLeak = now + interval(toVillage);
        d.markDirty();
    }

    /**
     * Someone has found the village before anyone found Ragna: she was taken from the washing
     * line all the same, somewhere nobody saw, and her stone is already standing with her brother
     * at it. The chapter goes on from there.
     */
    private boolean arrivedEarly(WorldServer world) {
        StoryData d = data();
        if (d.village == null || d.seen("ragna_scene") || world.getTotalWorldTime() % 20 != 0) {
            return false;
        }
        for (EntityPlayer p : world.playerEntities) {
            if (!p.isSpectator() && p.dimension == 0 && p.getDistanceSq(d.village.getX(), p.posY, d.village.getZ()) < 110D * 110D) {
                d.see("ragna_scene");
                d.see("ragna_dead");
                d.see("ragna_unseen");
                d.oath = StoryData.Oath.VILLAGE;
                d.lead = p.getUniqueID();
                d.markDirty();
                if (alive(ragna) != null) {
                    ragna.setDead();
                    ragna = null;
                }
                if (alive(stranger) != null) {
                    stranger.setDead();
                    stranger = null;
                }
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ Ragna

    /** Someone has made enough of a start in the world, and is out under the sky: she comes down near them. */
    private void tryRagna(WorldServer world) {
        StoryData d = data();
        if (d.leaks < EternityConfig.story.ragnaAfterLeaks || d.village == null) {
            return;
        }
        for (EntityPlayer p : world.playerEntities) {
            if (p instanceof EntityPlayerMP && !p.isSpectator() && ready((EntityPlayerMP) p) && !story.talking(p)
                    && world.canSeeSky(new BlockPos(p).up()) && p.getDistanceSq(d.village.getX(), p.posY, d.village.getZ()) > 200D * 200D) {
                if (placeRagna(world, (EntityPlayerMP) p) != null) {
                    return;
                }
            }
        }
    }

    /** Smelted iron, or been to the Nether: not somebody still punching trees. */
    private static boolean ready(EntityPlayerMP p) {
        for (String id : new String[]{"minecraft:story/smelt_iron", "minecraft:story/enter_the_nether"}) {
            Advancement a = p.server.getAdvancementManager().getAdvancement(new ResourceLocation(id));
            if (a != null && p.getAdvancements().getProgress(a).isDone()) {
                return true;
            }
        }
        return false;
    }

    /** Lays her down behind someone, out of their sight, far enough that they hear it before they see it. */
    @Nullable
    String placeRagna(WorldServer world, EntityPlayerMP p) {
        StoryData d = data();
        Vec3d look = p.getLookVec();
        double base = Math.atan2(-look.z, -look.x);
        BlockPos best = null;
        for (int i = 0; i < 24 && best == null; i++) {
            double a = base + (rand.nextDouble() - 0.5D) * Math.PI * (i < 12 ? 0.8D : 2D);
            double r = 26D + rand.nextDouble() * 10D;
            BlockPos c = Barrow.surface(world, MathHelper.floor(p.posX + Math.cos(a) * r), MathHelper.floor(p.posZ + Math.sin(a) * r));
            if (!world.isBlockLoaded(c) || !world.canSeeSky(c) || !Leaks.natural(world.getBlockState(c.down()))) {
                continue;
            }
            boolean flat = true;
            for (int k = 0; k < 8 && flat; k++) {
                double ka = k * Math.PI / 4D;
                BlockPos s = Barrow.surface(world, c.getX() + (int) Math.round(Math.cos(ka) * 3D), c.getZ() + (int) Math.round(Math.sin(ka) * 3D));
                flat = Math.abs(s.getY() - c.getY()) <= 1;
            }
            if (flat) {
                best = c;
            }
        }
        if (best == null) {
            return null;
        }
        Vec3d from = d.village != null ? new Vec3d(d.village) : new Vec3d(best.getX(), best.getY(), best.getZ() - 100D);
        Leaks.land(world, best, from, Leaks.TURF, 4, rand, false);
        BlockPos c = Barrow.surface(world, best.getX(), best.getZ());
        // Her feet point the way she was thrown, into a stone that came with her.
        Vec3d along = new Vec3d(c.getX() - from.x, 0D, c.getZ() - from.z).normalize();
        EnumFacing f = EnumFacing.getFacingFromVector((float) along.x, 0F, (float) along.z);
        world.setBlockState(c.offset(f), Blocks.MOSSY_COBBLESTONE.getDefaultState(), 2);
        world.setBlockState(c.offset(f).offset(f.rotateY()), Blocks.STONE.getDefaultState(), 2);
        // The washing line she was at, its posts and the line, and what was on it.
        EnumFacing across = f.rotateY();
        BlockPos post = c.offset(f.getOpposite(), 3).offset(across, -2);
        for (int i = 0; i < 2; i++) {
            BlockPos at = Barrow.surface(world, post.getX(), post.getZ());
            world.setBlockState(at, Blocks.SPRUCE_FENCE.getDefaultState(), 2);
            world.setBlockState(at.up(), Blocks.SPRUCE_FENCE.getDefaultState(), 2);
            post = post.offset(across, 4);
        }
        BlockPos lineAt = Barrow.surface(world, c.offset(f.getOpposite(), 3).getX(), c.offset(f.getOpposite(), 3).getZ()).up();
        for (int i = -1; i <= 1; i++) {
            BlockPos s = lineAt.offset(across, i);
            if (world.isAirBlock(s)) {
                world.setBlockState(s, Blocks.TRIPWIRE.getDefaultState(), 2);
            }
        }
        BlockPos sheet = Barrow.surface(world, c.offset(across, 2).getX(), c.offset(across, 2).getZ());
        world.setBlockState(sheet, Blocks.CARPET.getDefaultState(), 2);
        d.ragna = c;
        d.lead = p.getUniqueID();
        d.oath = StoryData.Oath.RAGNA;
        d.markDirty();
        return "Ragna is down at " + c.getX() + " " + c.getY() + " " + c.getZ() + ".";
    }

    /** Keeps Ragna and the stranger at her side until someone comes, then plays what happens. */
    private void ragnaSite(WorldServer world) {
        StoryData d = data();
        if (d.ragna == null || d.seen("ragna_scene")) {
            return;
        }
        Vec3d c = new Vec3d(d.ragna).add(0.5D, 0D, 0.5D);
        if (!world.isBlockLoaded(d.ragna)) {
            return;
        }
        Vec3d from = d.village != null ? new Vec3d(d.village) : c.add(0D, 0D, -100D);
        Vec3d along = new Vec3d(c.x - from.x, 0D, c.z - from.z).normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(-along.x, along.z));
        Vec3d right = new Vec3d(-along.z, 0D, along.x).scale(-1D);
        if (alive(ragna) == null) {
            ragna = EntityNorse.of(world, NorseBoxes.RAGNA, "ragna");
            ragna.setLocationAndAngles(c.x, c.y + 0.02D, c.z, yaw, 0F);
            ragna.act(NorsePose.LIE).rest(NorsePose.LIE);
            world.spawnEntity(ragna);
        }
        if (alive(stranger) == null) {
            stranger = EntityNorse.of(world, NorseBoxes.STRANGER, "stranger");
            Vec3d at = c.subtract(along.scale(0.55D)).add(right.scale(0.95D));
            stranger.setLocationAndAngles(at.x, c.y, at.z, 0F, 0F);
            stranger.face(c.subtract(along.scale(0.55D)));
            stranger.act(NorsePose.HOLD).rest(NorsePose.HOLD);
            world.spawnEntity(stranger);
        }
        EntityPlayerMP lead = StoryDirector.player(d.lead);
        for (EntityPlayer p : world.playerEntities) {
            if (p instanceof EntityPlayerMP && !p.isSpectator() && p.getDistanceSq(c.x, c.y, c.z) < 14D * 14D && !story.talking(p)) {
                if (lead == null || lead.dimension != 0 || lead.getDistanceSq(c.x, c.y, c.z) > 60D * 60D) {
                    d.lead = p.getUniqueID();
                }
                if (p.getUniqueID().equals(d.lead)) {
                    ragnaScene((EntityPlayerMP) p, c, along);
                }
                return;
            }
        }
    }

    private void ragnaScene(EntityPlayerMP player, Vec3d site, Vec3d along) {
        StoryData d = data();
        d.see("ragna_scene");
        EntityNorse her = ragna, she = stranger;
        Vec3d village = d.village != null ? new Vec3d(d.village) : site.add(0D, 0D, -1000D);
        double far = Math.sqrt(site.squareDistanceTo(village.x, site.y, village.z));
        String fromHome = Compass.of(village, site);
        String toVillage = Compass.of(site, village);
        story.scene(player, RAGNA_SCENE, her.getPositionVector(), she.getPositionVector());
        Talk t = new Talk(player).at(her, 48D);
        t.run(() -> she.watch(null).look(her.getPositionEyes(1F)))
                .pause(36)
                .line(RAGNA, her, "Where is this?")
                .line(STRANGER, she, far > 600D ? "A long way " + fromHome + ". You're not in pain." : far > 250D
                        ? Compass.capital(fromHome) + " of home. You're not in pain." : "Not far from home. You're not in pain.")
                .line(RAGNA, her, "I was at the line. Hanging the washing.")
                .run(() -> her.look(player.getPositionEyes(1F)))
                .pause(24)
                .line(RAGNA, her, "Tell Hrafn. Tell him it's worse.")
                .run(() -> {
                    her.look(null);
                    her.act(NorsePose.DEAD).rest(NorsePose.DEAD);
                    player.world.playSound(null, her.posX, her.posY, her.posZ, SoundEvents.ENTITY_PLAYER_BREATH, SoundCategory.NEUTRAL, 0.6F, 0.7F);
                })
                .pause(50)
                .run(() -> she.act(NorsePose.CLOSE, 0))
                .pause(64)
                .run(() -> she.rest(NorsePose.KNEEL).act(NorsePose.KNEEL).watch(player))
                .pause(20)
                .line(STRANGER, she, "Her name was Ragna.")
                .until(() -> story.sceneDone(player, RAGNA_SCENE), 200);
        askAboutRagna(t, player, she, toVillage, new boolean[3]);
        t.run(() -> {
            d.oath = StoryData.Oath.SEEKING;
            d.see("ragna_dead");
            d.markDirty();
            // She goes the way she said, and is ordinary all the way out of sight.
            Vec3d away = site.add(new Vec3d(village.x - site.x, 0D, village.z - site.z).normalize().scale(60D));
            she.rest(NorsePose.STAND).act(NorsePose.STAND).watch(null).look(null);
            she.walkTo(away, 0.55D);
        });
        t.abandoned(() -> {
            if (d.oath == StoryData.Oath.RAGNA) {
                d.oath = StoryData.Oath.SEEKING;
                d.see("ragna_dead");
                d.markDirty();
                her.act(NorsePose.DEAD).rest(NorsePose.DEAD);
                she.rest(NorsePose.STAND).act(NorsePose.STAND).watch(null);
                she.walkTo(site.add(new Vec3d(village.x - site.x, 0D, village.z - site.z).normalize().scale(60D)), 0.55D);
            }
        });
        story.talk(player, t);
    }

    /** What happened, who Hrafn is, who she is: as many of them as the player asks. */
    private void askAboutRagna(Talk t, EntityPlayerMP player, EntityNorse she, String toVillage, boolean[] asked) {
        List<Talk.Choice> choices = new ArrayList<>();
        if (!asked[0]) {
            choices.add(say("What happened to her?", b -> {
                asked[0] = true;
                b.line(STRANGER, she, "There's a village " + toVillage + " of here. They keep something for Odin.")
                        .line(STRANGER, she, "It's started throwing people across the world.")
                        .pause(16)
                        .line(STRANGER, she, "She's the ninth.");
                askAboutRagna(b, player, she, toVillage, asked);
            }));
        }
        if (!asked[1]) {
            choices.add(say("Who's Hrafn?", b -> {
                asked[1] = true;
                b.line(STRANGER, she, "Her brother. He keeps what the village keeps.");
                askAboutRagna(b, player, she, toVillage, asked);
            }));
        }
        if (!asked[2]) {
            choices.add(say("Who are you?", b -> {
                asked[2] = true;
                b.line(STRANGER, she, "Someone who keeps arriving too late.")
                        .run(() -> she.rest(NorsePose.STAND).act(NorsePose.STAND))
                        .pause(30)
                        .line(STRANGER, she, "You saw her come through?");
                askAboutRagna(b, player, she, toVillage, asked);
            }));
        }
        if (asked[0] || asked[1] || asked[2]) {
            choices.add(act("(Say nothing more.)", b -> tellOfTheMound(b, player, she, toVillage)));
        }
        if (choices.size() == 1 && asked[0] && asked[1] && asked[2]) {
            tellOfTheMound(t, player, she, toVillage);
            return;
        }
        t.ask(STRANGER, she, choices.toArray(new Talk.Choice[0]));
    }

    private void tellOfTheMound(Talk t, EntityPlayerMP player, EntityNorse she, String toVillage) {
        boolean[] asked = new boolean[2];
        t.run(() -> she.rest(NorsePose.STAND).act(NorsePose.STAND))
                .line(STRANGER, she, "Her brother is called Hrafn. The thing they keep is under their burial mound.")
                .line(STRANGER, she, "Their oath won't let them take it out, and it won't stop until somebody does.");
        askWhy(t, player, she, toVillage, asked);
    }

    private void askWhy(Talk t, EntityPlayerMP player, EntityNorse she, String toVillage, boolean[] asked) {
        List<Talk.Choice> choices = new ArrayList<>();
        if (!asked[0]) {
            choices.add(say("Why are you telling me?", b -> {
                asked[0] = true;
                b.line(STRANGER, she, "You were here.");
                askWhy(b, player, she, toVillage, asked);
            }));
        }
        if (!asked[1]) {
            choices.add(say("Where is it?", b -> {
                asked[1] = true;
                b.line(STRANGER, she, Compass.capital(toVillage) + ". Follow what doesn't belong.");
                askWhy(b, player, she, toVillage, asked);
            }));
        }
        if (asked[0] && asked[1]) {
            t.direction("She looks at you a moment longer than she needs to. Then she goes.");
            return;
        }
        choices.add(act(asked[0] || asked[1] ? "(Let her go.)" : "(Say nothing.)", b -> {
            if (!asked[1]) {
                b.direction("She looks at you a moment longer than she needs to.")
                        .line(STRANGER, she, Compass.capital(toVillage) + ". Follow what doesn't belong.");
            } else {
                b.direction("She looks at you a moment longer than she needs to. Then she goes.");
            }
        }));
        t.ask(STRANGER, she, choices.toArray(new Talk.Choice[0]));
    }

    // ------------------------------------------------------------------ the village

    private void village(WorldServer world, long now) {
        StoryData d = data();
        if (d.village == null) {
            return;
        }
        EntityPlayer near = null;
        for (EntityPlayer p : world.playerEntities) {
            if (!p.isSpectator() && p.getDistanceSq(d.village.getX(), p.posY, d.village.getZ()) < 150D * 150D) {
                near = p;
                break;
            }
        }
        if (!d.dressed && near != null) {
            dress(world);
        }
        Barrow b = barrow();
        if (b == null) {
            return;
        }
        if (d.oath == StoryData.Oath.SEEKING && near != null && near.getDistanceSq(d.village.getX(), near.posY, d.village.getZ()) < 70D * 70D) {
            d.oath = StoryData.Oath.VILLAGE;
            d.markDirty();
        }
        if (now % 10 == 0) {
            people(world, b);
        }
        hrafnWaits(world, b);
    }

    /** Puts up the mound, the barrow under it and the stones beside its door. Once. */
    String dress(WorldServer world) {
        StoryData d = data();
        if (d.village == null) {
            return "There is no village yet.";
        }
        Barrow b = Barrow.site(world, d.village);
        if (!b.loaded(world)) {
            // Make sure every piece of it is there to be built on.
            for (int z = -12; z <= Barrow.WARD + 8; z += 8) {
                for (int x = -12; x <= 12; x += 8) {
                    world.getChunk(b.at(x, 0, z));
                }
            }
        }
        b.build(world, rand);
        d.barrow = b.origin;
        d.barrowFacing = b.in.getHorizontalIndex();
        d.dressed = true;
        d.markDirty();
        barrow = b;
        // The cube the village kept in its hall is not there: it never left the mound.
        for (Entity e : new ArrayList<>(world.loadedEntityList)) {
            if (e instanceof lucraft.mods.heroesexpansion.entities.EntityTesseract && e.getDistanceSq(d.village) < 120D * 120D) {
                ItemStack s = ((lucraft.mods.heroesexpansion.entities.EntityTesseract) e).getItem();
                if (Relics.isVillageCopy(s)) {
                    e.setDead();
                }
            }
        }
        story.sendStateToAll();
        return "The mound is up, its door at " + b.origin.getX() + " " + b.origin.getY() + " " + b.origin.getZ() + ".";
    }

    /** Who should be standing where, given how far the chapter has gone; put there if anyone is near. */
    private void people(WorldServer world, Barrow b) {
        StoryData d = data();
        boolean anyoneNear = false;
        for (EntityPlayer p : world.playerEntities) {
            if (!p.isSpectator() && p.getDistanceSq(b.origin) < 110D * 110D) {
                anyoneNear = true;
            }
        }
        if (!anyoneNear || !b.loaded(world)) {
            return;
        }
        StoryData.Oath stage = d.oath;
        // Hrafn: at Ragna's stone; then at the door; then wherever the player goes; then fighting; then lying with his own.
        if (stage.ordinal() >= StoryData.Oath.SEEKING.ordinal() && stage.ordinal() <= StoryData.Oath.BROKEN.ordinal() && !d.seen("hrafn_fell")) {
            if (alive(hrafn) == null) {
                hrafn = EntityNorse.of(world, NorseBoxes.HRAFN, "hrafn");
                Vec3d at;
                if (stage == StoryData.Oath.BROKEN) {
                    at = b.vec(0D, Barrow.FLOOR + 1, 13D);
                } else if (d.seen("hrafn_met")) {
                    at = b.step();
                } else {
                    at = graveside(b);
                }
                hrafn.setLocationAndAngles(at.x, at.y + 0.1D, at.z, 0F, 0F);
                world.spawnEntity(hrafn);
                if (stage == StoryData.Oath.BROKEN) {
                    hrafn.setArmed(true);
                    hrafn.rest(NorsePose.FIGHT).act(NorsePose.FIGHT);
                    hrafn.face(b.vec(0D, Barrow.FLOOR + 1, 20D));
                } else if (d.seen("hrafn_met")) {
                    hrafn.face(b.doorMiddle());
                    hrafn.rest(NorsePose.STAND).act(NorsePose.STAND);
                } else {
                    hrafn.face(b.vec(5D, 0D, -6D));
                    hrafn.rest(NorsePose.MOURN).act(NorsePose.MOURN);
                }
            }
        }
        // Afterwards he lies with his own, where he fell, for good.
        if (d.seen("hrafn_fell") && d.seen("mourned") && alive(hrafn) == null) {
            Vec3d at = b.vec(0D, Barrow.FLOOR + 1.02D, 13D);
            hrafn = EntityNorse.of(world, NorseBoxes.HRAFN, "hrafn");
            hrafn.setLocationAndAngles(at.x, at.y, at.z, b.yawIn() + 160F, 0F);
            hrafn.act(NorsePose.DEAD).rest(NorsePose.DEAD);
            world.spawnEntity(hrafn);
        }
        // The dead, in their graves; or up, or lying where they fell.
        boolean near = false;
        for (EntityPlayer p : world.playerEntities) {
            if (!p.isSpectator() && p.getDistanceSq(b.vec(0D, Barrow.FLOOR, 18D).x, b.vec(0D, Barrow.FLOOR, 18D).y,
                    b.vec(0D, Barrow.FLOOR, 18D).z) < 72D * 72D) {
                near = true;
            }
        }
        if (near) {
            for (int i = 0; i < dead.length; i++) {
                if (alive(dead[i]) == null) {
                    EntityDraugr g = new EntityDraugr(world);
                    int keeper = Barrow.keeperOf(i);
                    g.laid(b.grave(i), b.yawIn(), i, keeper, i % 3 != 1, keeper >= 0 || i % 2 == 0);
                    world.spawnEntity(g);
                    dead[i] = g;
                    if (stage == StoryData.Oath.BROKEN && !d.seen("hrafn_fell") && alive(hrafn) != null) {
                        g.wake(hrafn, false);
                    } else if (d.seen("hrafn_fell")) {
                        // Where they fell round him, and staying there.
                        Vec3d spot = b.vec((i % 3 - 1) * 1.6D + (i % 2 == 0 ? 0.5D : -0.4D), Barrow.FLOOR + 1.05D, 13.5D + i * 0.9D);
                        g.setPositionAndUpdate(spot.x, spot.y, spot.z);
                        g.lieStill();
                    }
                }
            }
        }
        // The stranger, at the graves among the mourners, until Hrafn has asked about her.
        if (stage.ordinal() >= StoryData.Oath.SEEKING.ordinal() && stage.ordinal() < StoryData.Oath.BROKEN.ordinal()
                && !d.seen("hrafn_asked") && alive(stranger) == null && d.seen("ragna_dead")) {
            boolean watched = false;
            Vec3d at = b.vec(6.5D, 0D, -3.3D);
            for (EntityPlayer p : world.playerEntities) {
                watched |= sees(p, at) && p.getDistanceSq(at.x, at.y, at.z) < 48D * 48D;
            }
            if (!watched) {
                stranger = EntityNorse.of(world, NorseBoxes.STRANGER, "stranger");
                BlockPos top = Barrow.surface(world, MathHelper.floor(at.x), MathHelper.floor(at.z));
                stranger.setLocationAndAngles(at.x, top.getY(), at.z, 0F, 0F);
                stranger.face(b.vec(5D, 0D, -6D));
                stranger.rest(NorsePose.MOURN).act(NorsePose.MOURN);
                world.spawnEntity(stranger);
            }
        }
    }

    /** Beside Ragna's stone, on the path's side of it. */
    private static Vec3d graveside(Barrow b) {
        return b.vec(3.4D, 0D, -6D);
    }

    /** What Hrafn does when nobody is talking to him: waits at the door, or keeps up with whoever he is taking down. */
    private void hrafnWaits(WorldServer world, Barrow b) {
        StoryData d = data();
        EntityNorse h = alive(hrafn);
        if (h == null || d.oath.ordinal() >= StoryData.Oath.BROKEN.ordinal()) {
            return;
        }
        EntityPlayerMP lead = StoryDirector.player(d.lead);
        if (!d.seen("hrafn_met")) {
            // Near enough to speak to him, and he turns to them.
            for (EntityPlayer p : world.playerEntities) {
                if (p instanceof EntityPlayerMP && !p.isSpectator() && p.getDistance(h) < 4.5D && !story.talking(p)) {
                    meetHrafn((EntityPlayerMP) p, h, b);
                    return;
                }
            }
            return;
        }
        if (d.outerOpen) {
            if (lead != null && lead.dimension == 0 && lead.getDistance(h) < 40D && !story.talking(lead)) {
                h.follow(lead, 2.6D);
                h.watch(lead);
            } else if (lead == null || lead.dimension != 0 || lead.getDistance(h) > 48D) {
                h.follow(null, 0D);
                if (h.getDistanceSq(b.step().x, b.step().y, b.step().z) > 4D) {
                    h.walkTo(b.step(), 0.6D);
                }
            }
            return;
        }
        if (!d.seen("hrafn_asked") && lead != null && lead.getDistance(h) < 7D && h.getDistanceSq(b.step().x, b.step().y, b.step().z) < 9D
                && !story.talking(lead)) {
            askAboutHer(lead, h, b);
        }
    }

    // ------------------------------------------------------------------ Hrafn

    private void meetHrafn(EntityPlayerMP player, EntityNorse h, Barrow b) {
        StoryData d = data();
        d.see("hrafn_met");
        d.lead = player.getUniqueID();
        d.markDirty();
        boolean sawHer = d.seen("ragna_scene") && !d.seen("ragna_unseen");
        boolean carriesStone = carries(player, Stones.EIRIK);
        Talk t = new Talk(player).at(h, 24D);
        t.run(() -> h.rest(NorsePose.STAND).act(NorsePose.STAND).watch(player))
                .pause(16)
                .line(HRAFN, h, "You're not from here.");
        List<Talk.Choice> first = new ArrayList<>();
        if (sawHer) {
            first.add(say("Ragna asked me to find you.", c -> ragnaToHrafn(c, player, h, true)));
            first.add(say("I watched a woman die. She said your name.", c -> ragnaToHrafn(c, player, h, false)));
        }
        if (carriesStone) {
            first.add(act("(Hold out the stone.)", c -> c.direction("He takes it, turns it to the light, and stops at the name.")
                    .line(HRAFN, h, "My father's.")
                    .line(HRAFN, h, "The sea had him. Now it's had his stone.")
                    .pause(20)
                    .line(HRAFN, h, "Where?")
                    .ask(HRAFN, h,
                            say("Out in the world, with other things from here.", e -> e.line(HRAFN, h, "Then it's taking the dead now too.")),
                            act("(Tell him about Ragna.)", e -> ragnaToHrafn(e, player, h, false)))));
        }
        first.add(say("Whose stone is this?", c -> c.line(HRAFN, h, "My sister's.")
                .line(HRAFN, h, "We put stones up for the ones it takes. There's nothing to put under them.")));
        t.ask(HRAFN, h, first.toArray(new Talk.Choice[0]));
        t.pause(10)
                .run(() -> h.look(b.vec(0D, 4D, 9D)))
                .line(HRAFN, h, "My grandfather's grandfather swore it. Keep it under the mound until Odin asks for it back.")
                .pause(24)
                .line(HRAFN, h, "Odin isn't asking. It's taking us instead.")
                .run(() -> h.watch(player))
                .line(HRAFN, h, "The doors only open for our blood. And the ones who guard it are ours.")
                .line(HRAFN, h, "They know the oath. Whoever breaks it, they'll come for.")
                .ask(HRAFN, h,
                        say("Then I'll carry it out.", c -> c.line(HRAFN, h, "It has to be you.")
                                .line(HRAFN, h, "If it's in my hands when the oath breaks, they'll tear it out of them.")),
                        say("What happens to you?", c -> c.direction("He doesn't answer that.")
                                .pause(20)
                                .line(HRAFN, h, "You'll carry it. It can't be me.")
                                .line(HRAFN, h, "If it's in my hands when the oath breaks, they'll tear it out of them.")),
                        say("Why not leave it?", c -> c.line(HRAFN, h, "Leif. Gunnhild. Ragna.")
                                .pause(16)
                                .line(HRAFN, h, "There were forty of us at midsummer.")
                                .line(HRAFN, h, "You'll carry it. If it's in my hands when the oath breaks, they'll tear it out of them.")))
                .pause(10)
                .line(HRAFN, h, "There's a second door inside. It opens to the oath, in the words they swore it in.")
                .line(HRAFN, h, "Every keeper put a line to it when they took it on. It's on their stones. Ulf swore it first.")
                .line(HRAFN, h, "My father was meant to teach me the whole of it. The sea took him first.");
        askAboutWords(t, player, h, b, new boolean[2]);
        t.run(() -> {
            h.watch(null);
            h.rest(NorsePose.STAND);
            h.walkTo(b.step(), 0.55D);
        }).abandoned(() -> {
            h.watch(null);
            h.rest(NorsePose.STAND).act(NorsePose.STAND);
            h.walkTo(b.step(), 0.55D);
        });
        story.talk(player, t);
    }

    private void ragnaToHrafn(Talk t, EntityPlayerMP player, EntityNorse h, boolean asked) {
        t.direction(asked ? "He looks at you for a long moment." : "He doesn't need you to say which woman.")
                .line(HRAFN, h, asked ? "Where is she?" : "Ragna.")
                .ask(HRAFN, h,
                        say("She said to tell you it's worse.", c -> c.line(HRAFN, h, "It is.")
                                .pause(16)
                                .line(HRAFN, h, "Was she in pain?")
                                .ask(HRAFN, h,
                                        say("No.", e -> e.direction("He nods, once.")),
                                        say("I don't know.", e -> e.pause(20).line(HRAFN, h, "No. You'd know.")))),
                        say("A long way from here.", c -> c.line(HRAFN, h, "They're all a long way from here.")),
                        act("(Say nothing.)", c -> c.pause(30).line(HRAFN, h, "...All right.")));
    }

    private void askAboutWords(Talk t, EntityPlayerMP player, EntityNorse h, Barrow b, boolean[] asked) {
        StoryData d = data();
        List<Talk.Choice> choices = new ArrayList<>();
        if (!asked[0]) {
            choices.add(say("What were his words?", c -> {
                asked[0] = true;
                c.line(HRAFN, h, "\"" + Stones.OATH[Stones.EIRIK] + "\"")
                        .pause(20)
                        .line(HRAFN, h, "He said it was the line nobody wanted.")
                        .run(() -> found(player, Stones.EIRIK, false));
                askAboutWords(c, player, h, b, asked);
            }));
        }
        if (!asked[1]) {
            choices.add(say("Where are the stones?", c -> {
                asked[1] = true;
                c.line(HRAFN, h, "My grandmother's is here, by Ragna's. The old ones are inside, with their keepers.")
                        .direction("He doesn't say what he means by \"with\".");
                askAboutWords(c, player, h, b, asked);
            }));
        }
        choices.add(say(asked[0] && asked[1] ? "Then let's go." : "Let's go.", c -> c.line(HRAFN, h,
                d.seen("hrafn_asked") ? "I'll be at the door." : "I'll be at the door. When you're ready.")));
        t.ask(HRAFN, h, choices.toArray(new Talk.Choice[0]));
    }

    /** At the door, before it is open: the woman at the graves. */
    private void askAboutHer(EntityPlayerMP player, EntityNorse h, Barrow b) {
        StoryData d = data();
        d.see("hrafn_asked");
        d.markDirty();
        EntityNorse she = alive(stranger);
        Talk t = new Talk(player).at(h, 20D);
        t.run(() -> {
            h.walkTo(null, 0D);
            h.watch(player);
        }).line(HRAFN, h, "The woman at the graves. Dark hair. Brooches.");
        if (she != null) {
            t.run(() -> h.look(she.getPositionEyes(1F))).pause(20).run(() -> h.watch(player));
        }
        t.line(HRAFN, h, "She was at Ragna's. She was at Leif's the week before.")
                .pause(24)
                .line(HRAFN, h, "She was at Gunnhild's house the day before Gunnhild went. Gunnhild wasn't sick yet.")
                .pause(16)
                .line(HRAFN, h, "Who is she?")
                .ask(HRAFN, h,
                        say("I don't know.", c -> c.line(HRAFN, h, "Nobody does. She came with the first of it.")
                                .line(HRAFN, h, "Nobody asks her name. I don't know why that is.")),
                        say("She sent me here.", c -> {
                            d.see("told_hrafn_sent");
                            c.line(HRAFN, h, "Then find out what she wants for it.");
                        }),
                        say("She helped me.", c -> c.line(HRAFN, h, "She helped Gunnhild too.")))
                .run(() -> {
                    h.look(b.doorMiddle());
                    if (stranger != null) {
                        stranger.rest(NorsePose.STAND).act(NorsePose.STAND);
                    }
                })
                .direction("He turns back to the door.");
        story.talk(player, t);
    }

    // ------------------------------------------------------------------ the doors

    void touchSeal(EntityPlayerMP player, BlockPos pos) {
        Barrow b = barrow();
        StoryData d = data();
        if (b == null) {
            return;
        }
        EntityNorse h = alive(hrafn);
        if (b.isOuterSeal(pos)) {
            if (d.oath == StoryData.Oath.SEALED) {
                cold(player, "The stone is shut. Through it, nothing.");
            } else if (h != null && h.getDistance(player) < 9D && d.seen("hrafn_met")) {
                bloodDoor(player, h, b);
            } else {
                cold(player, "The stone is cold. Nothing in it answers your hand.");
            }
        } else if (b.isInnerSeal(pos)) {
            if (h != null && h.getDistance(player) < 10D) {
                oathDoor(player, h, b);
            } else {
                cold(player, "Nothing in it answers your hand. It wants their blood, and their words.");
            }
        }
    }

    private void cold(EntityPlayerMP player, String text) {
        long now = StoryDirector.now();
        Long last = lastCold.get(player.getUniqueID());
        if (last == null || now - last > 100) {
            lastCold.put(player.getUniqueID(), now);
            EternityNetwork.send(MsgStory.line(DIRECTION, -1, text), player);
        }
    }

    /** Hrafn cuts his palm and lays it on the stone, and the stone lets them in. */
    private void bloodDoor(EntityPlayerMP player, EntityNorse h, Barrow b) {
        StoryData d = data();
        Talk t = new Talk(player).at(h, 20D);
        t.run(() -> {
            h.walkTo(b.vec(0.6D, 0D, -1.2D), 0.5D);
            h.watch(null);
        }).until(h::arrived, 80)
                .run(() -> {
                    h.face(b.doorMiddle());
                    h.look(b.doorMiddle());
                })
                .line(HRAFN, h, "Stand back.")
                .direction("He draws his knife across his palm and lays the hand flat on the stone.")
                .run(() -> h.act(NorsePose.PALM, 70))
                .pause(30)
                .run(() -> openOuter(player.getServerWorld(), b))
                .pause(50)
                .run(() -> {
                    d.see("barrow_opened");
                    h.rest(NorsePose.STAND).act(NorsePose.STAND).watch(player);
                })
                .line(HRAFN, h, "After you.");
        story.talk(player, t);
    }

    void openOuter(WorldServer world, Barrow b) {
        StoryData d = data();
        for (BlockPos p : b.outerSeal()) {
            world.setBlockToAir(p);
        }
        d.outerOpen = true;
        d.markDirty();
        world.playSound(null, b.doorMiddle().x, b.doorMiddle().y, b.doorMiddle().z, SoundEvents.BLOCK_PISTON_CONTRACT, SoundCategory.BLOCKS, 2F, 0.3F);
        world.playSound(null, b.doorMiddle().x, b.doorMiddle().y, b.doorMiddle().z, SoundEvents.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.BLOCKS, 1F, 0.4F);
        EternityNetwork.sendAround(MsgStory.fx(MsgStory.FX_DOOR, b.doorMiddle(), new Vec3d(b.in.getDirectionVec()), 1), 0,
                b.doorMiddle().x, b.doorMiddle().y, b.doorMiddle().z, 96D);
    }

    void sealOuter(WorldServer world, Barrow b) {
        StoryData d = data();
        for (BlockPos p : b.outerSeal()) {
            world.setBlockState(p, Barrow.seal(false), 3);
        }
        d.outerOpen = false;
        d.markDirty();
        world.playSound(null, b.doorMiddle().x, b.doorMiddle().y, b.doorMiddle().z, SoundEvents.BLOCK_PISTON_EXTEND, SoundCategory.BLOCKS, 2F, 0.3F);
        EternityNetwork.sendAround(MsgStory.fx(MsgStory.FX_DOOR, b.doorMiddle(), new Vec3d(b.in.getDirectionVec()), 0), 0,
                b.doorMiddle().x, b.doorMiddle().y, b.doorMiddle().z, 96D);
    }

    /** The oath door: Hrafn's hand on it, and the player telling him the words, oldest first. */
    private void oathDoor(EntityPlayerMP player, EntityNorse h, Barrow b) {
        StoryData d = data();
        if (d.innerOpen) {
            return;
        }
        Talk t = new Talk(player).at(h, 16D);
        t.run(() -> {
            h.follow(null, 0D);
            h.walkTo(b.vec(-1.6D, Barrow.FLOOR + 1, Barrow.INNER - 1.1D), 0.5D);
        }).until(h::arrived, 80)
                .run(() -> {
                    h.face(b.innerMiddle());
                    h.look(b.innerMiddle());
                    h.act(NorsePose.PALM);
                })
                .direction("He lays his palm on the carved stone.");
        if (d.spoken == 0) {
            t.line(HRAFN, h, "The oath. Oldest first.");
        } else {
            t.line(HRAFN, h, "From where we left it.");
        }
        nextWords(t, player, h, b);
        t.run(() -> {
            h.rest(NorsePose.STAND).act(NorsePose.STAND);
            h.follow(player, 2.6D);
        }).abandoned(() -> {
            h.rest(NorsePose.STAND).act(NorsePose.STAND);
            h.follow(player, 2.6D);
        });
        story.talk(player, t);
    }

    /** The lines read so far, in no order but their keepers' names, and a way to step back. */
    private void nextWords(Talk t, EntityPlayerMP player, EntityNorse h, Barrow b) {
        StoryData d = data();
        List<Talk.Choice> choices = new ArrayList<>();
        int[] alphabetical = {Stones.ASGERD, Stones.BJORN, Stones.EIRIK, Stones.SIGRID, Stones.ULF};
        for (int keeper : alphabetical) {
            if (!d.found(keeper)) {
                continue;
            }
            choices.add(say(Stones.asAnswer(keeper), c -> spoke(c, player, h, b, keeper)));
        }
        if (choices.isEmpty()) {
            t.line(HRAFN, h, "We don't have any of it yet. Read the stones.")
                    .direction("Each keeper lies under a standing stone in the hall. Their words are cut into it.");
            return;
        }
        choices.add(say("How does it go?", c -> {
            hint(c, h);
            nextWords(c, player, h, b);
        }));
        choices.add(act("(Step back.)", c -> c.line(HRAFN, h, d.spoken > 0 ? "It'll hold what we've given it." : "When you have it.")));
        t.ask(HRAFN, h, choices.toArray(new Talk.Choice[0]));
    }

    /**
     * What Hrafn can tell them about the oath: that it is in the stones, and the order, which is
     * in the names. Plainer each time someone has got it wrong, until he simply says whose words
     * come next, as near as he can remember.
     */
    void hint(Talk t, EntityNorse h) {
        StoryData d = data();
        int found = Integer.bitCount(d.linesFound);
        if (found < Stones.KEEPERS) {
            t.line(HRAFN, h, found == 0 ? "Five keepers lie in here. Ulf, and the four who kept it after him."
                    : "We have " + found + " of the five. The rest are on the other stones in here.")
                    .direction("Each keeper lies under a standing stone in the hall. Their words are cut into it.");
        }
        if (d.spoken >= Stones.KEEPERS) {
            return;
        }
        if (d.misses == 0) {
            t.line(HRAFN, h, "Oldest first. Ulf swore it. Every keeper after him was the child of the one before.")
                    .line(HRAFN, h, "It's in their names.");
        } else if (d.misses == 1) {
            t.line(HRAFN, h, "Asgerd Ulfsdottir. Ulf's daughter. Bjorn was Asgerd's son, so: Asgerdsson.")
                    .line(HRAFN, h, "Follow the names down to my father.");
        } else {
            int next = d.spoken;
            t.line(HRAFN, h, d.spoken == 0 ? "Ulf's first. Always Ulf's." : "We've said " + d.spoken + ". " + Stones.fullName(next) + "'s next. I'm sure of that.");
        }
    }

    private void spoke(Talk t, EntityPlayerMP player, EntityNorse h, Barrow b, int keeper) {
        StoryData d = data();
        WorldServer world = player.getServerWorld();
        if (keeper == d.spoken) {
            t.line(HRAFN, h, Stones.OATH[keeper])
                    .run(() -> {
                        d.spoken++;
                        d.markDirty();
                        lightRunes(world, b, d.spoken);
                        world.playSound(null, b.innerMiddle().x, b.innerMiddle().y, b.innerMiddle().z, SoundEvents.BLOCK_END_PORTAL_FRAME_FILL,
                                SoundCategory.BLOCKS, 1.2F, 0.5F + d.spoken * 0.1F);
                    })
                    .pause(14);
            t.run(() -> {
                if (d.spoken >= Stones.KEEPERS) {
                    openInner(world, b);
                }
            });
            if (keeper + 1 >= Stones.KEEPERS) {
                t.pause(40).line(HRAFN, h, "That's all of it.").run(() -> h.act(NorsePose.STAND));
                return;
            }
            if (!d.found(keeper + 1)) {
                t.pause(10).line(HRAFN, h, "There's more. I don't know it.").line(HRAFN, h, "Find it.");
                return;
            }
            nextWords(t, player, h, b);
            return;
        }
        // Wrong: the one whose words should have come gets up to see who is in their barrow.
        int whose = d.spoken;
        t.line(HRAFN, h, Stones.OATH[keeper].substring(0, Math.min(Stones.OATH[keeper].length(), 14)) + "...")
                .run(() -> {
                    d.spoken = 0;
                    d.markDirty();
                    lightRunes(world, b, 0);
                    world.playSound(null, b.innerMiddle().x, b.innerMiddle().y, b.innerMiddle().z, SoundEvents.ENTITY_ZOMBIE_VILLAGER_CURE,
                            SoundCategory.BLOCKS, 1F, 0.4F);
                    wakeFor(world, b, whose, player);
                })
                .direction("The runes flare, and go dark. Somewhere in the hall behind you, something sits up.")
                .line(HRAFN, h, "That wasn't it.")
                .line(HRAFN, h, Stones.fullName(whose) + "'s words should have come next.")
                .line(HRAFN, h, whose == Stones.ULF ? "Ulf swore it. It starts with him." : "Each of them was the last one's child. It's in the names.")
                .run(() -> {
                    d.misses++;
                    d.markDirty();
                });
    }

    /** The grave of the keeper whose words were missed, or one of their kin's. */
    private void wakeFor(WorldServer world, Barrow b, int keeper, EntityPlayerMP player) {
        EntityDraugr pick = null;
        for (EntityDraugr g : dead) {
            if (alive(g) != null && g.isLaidDown() && g.keeper == keeper) {
                pick = g;
            }
        }
        if (pick == null) {
            List<EntityDraugr> kin = new ArrayList<>();
            for (EntityDraugr g : dead) {
                if (alive(g) != null && g.isLaidDown() && g.keeper < 0) {
                    kin.add(g);
                }
            }
            if (!kin.isEmpty()) {
                pick = kin.get(rand.nextInt(kin.size()));
            }
        }
        if (pick != null) {
            pick.wake(player, true);
            EternityNetwork.sendAround(MsgStory.fx(MsgStory.FX_RISE, pick.getPositionVector(), Vec3d.ZERO, 0), 0, pick.posX, pick.posY, pick.posZ, 64D);
        }
    }

    private void lightRunes(WorldServer world, Barrow b, int spoken) {
        BlockPos[] seal = b.innerSeal();
        int lit = Math.round(seal.length * spoken / (float) Stones.KEEPERS);
        for (int i = 0; i < seal.length; i++) {
            if (world.getBlockState(seal[i]).getBlock() == EternityBlocks.BARROW_SEAL) {
                world.setBlockState(seal[i], Barrow.seal(i < lit), 3);
            }
        }
        EternityNetwork.sendAround(MsgStory.fx(MsgStory.FX_RUNE, b.innerMiddle(), new Vec3d(b.in.getDirectionVec()), spoken), 0,
                b.innerMiddle().x, b.innerMiddle().y, b.innerMiddle().z, 48D);
    }

    void openInner(WorldServer world, Barrow b) {
        StoryData d = data();
        for (BlockPos p : b.innerSeal()) {
            world.setBlockToAir(p);
        }
        d.innerOpen = true;
        d.spoken = Stones.KEEPERS;
        d.markDirty();
        world.playSound(null, b.innerMiddle().x, b.innerMiddle().y, b.innerMiddle().z, SoundEvents.BLOCK_PISTON_CONTRACT, SoundCategory.BLOCKS, 2F, 0.25F);
        EternityNetwork.sendAround(MsgStory.fx(MsgStory.FX_DOOR, b.innerMiddle(), new Vec3d(b.in.getDirectionVec()), 1), 0,
                b.innerMiddle().x, b.innerMiddle().y, b.innerMiddle().z, 64D);
        story.sendStateToAll();
    }

    // ------------------------------------------------------------------ reading the stones

    void read(EntityPlayerMP player, int stone) {
        if (Stones.isKeeper(stone)) {
            found(player, stone, true);
        }
        EntityNorse h = alive(hrafn);
        StoryData d = data();
        if (h != null && h.getDistance(player) < 8D && !story.talking(player) && d.see("hrafn_read_" + stone)) {
            String said = null;
            switch (stone) {
                case Stones.ULF:
                    said = "Ulf. Everything since is because of him.";
                    break;
                case Stones.SIGRID:
                    said = "My grandmother. She sang it to us. Only her line.";
                    break;
                case Stones.RAGNA:
                    said = "I cut that yesterday.";
                    break;
                case Stones.EIRIK:
                    said = "That was meant to stand here.";
                    break;
                default:
                    break;
            }
            if (said != null) {
                Talk t = new Talk(player).at(h, 20D);
                t.pause(36).line(HRAFN, h, said);
                story.talk(player, t);
            }
        }
    }

    private void found(EntityPlayerMP player, int keeper, boolean read) {
        StoryData d = data();
        if (!d.found(keeper)) {
            d.linesFound |= 1 << keeper;
            d.markDirty();
        }
    }

    private static boolean carries(EntityPlayerMP player, int stone) {
        for (ItemStack s : player.inventory.mainInventory) {
            if (s.getItem() == EternityBlocks.RUNE_STONE_ITEM && com.matoon.eternity.block.BlockRuneStone.stoneOf(s) == stone) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ the cube

    boolean useBlock(EntityPlayerMP player, BlockPos pos) {
        Barrow b = barrow();
        StoryData d = data();
        if (b == null || player.dimension != 0 || !d.innerOpen || !d.wardLit) {
            return false;
        }
        Vec3d cube = b.cube();
        if (!b.inWard(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) || player.getDistanceSq(cube.x, cube.y, cube.z) > 4.5D * 4.5D) {
            return false;
        }
        lift(player, b);
        return true;
    }

    /** Someone reached for the cube itself, in the air over the altar. */
    void reachForCube(EntityPlayerMP player) {
        Barrow b = barrow();
        StoryData d = data();
        if (b == null || player.dimension != 0 || !d.innerOpen || !d.wardLit) {
            return;
        }
        Vec3d cube = b.cube();
        Vec3d to = cube.subtract(player.getPositionEyes(1F));
        if (to.length() < 4.5D && player.getLookVec().dotProduct(to.normalize()) > 0.9D) {
            lift(player, b);
        }
    }

    /** The cube lifted out of the ward: the oath is broken, and the dead are up. */
    void lift(EntityPlayerMP player, Barrow b) {
        StoryData d = data();
        WorldServer world = player.getServerWorld();
        d.wardLit = false;
        d.oath = StoryData.Oath.BROKEN;
        d.lead = player.getUniqueID();
        d.markDirty();
        fight = blows = 0;
        swingIn = 40;
        ItemStack cube = new ItemStack(HEItems.TESSERACT);
        Relics.claim(cube, player);
        if (!player.inventory.addItemStackToInventory(cube)) {
            player.dropItem(cube, false);
        }
        Vec3d at = b.cube();
        EternityNetwork.sendAround(MsgStory.fx(MsgStory.FX_WARD_OUT, at, Vec3d.ZERO, 0), 0, at.x, at.y, at.z, 96D);
        world.playSound(null, at.x, at.y, at.z, SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.BLOCKS, 2F, 0.5F);
        world.playSound(null, at.x, at.y, at.z, SoundEvents.ENTITY_LIGHTNING_THUNDER, SoundCategory.BLOCKS, 1F, 0.3F);
        story.sendStateToAll();
        EntityNorse h = alive(hrafn);
        if (h == null) {
            // He has to be there: he came down with them.
            h = hrafn = EntityNorse.of(world, NorseBoxes.HRAFN, "hrafn");
            Vec3d by = b.vec(0D, Barrow.FLOOR + 1, Barrow.INNER - 2D);
            h.setLocationAndAngles(by.x, by.y, by.z, b.yawIn(), 0F);
            world.spawnEntity(h);
        }
        EntityNorse him = h;
        Vec3d stand = b.vec(0D, Barrow.FLOOR + 1, 13D);
        for (EntityDraugr g : dead) {
            if (alive(g) != null) {
                g.wake(him, false);
                EternityNetwork.sendAround(MsgStory.fx(MsgStory.FX_RISE, g.getPositionVector(), Vec3d.ZERO, 1), 0, g.posX, g.posY, g.posZ, 64D);
            }
        }
        Talk t = new Talk(player).at(him, 64D);
        t.run(() -> {
            him.follow(null, 0D);
            him.watch(player);
            him.act(NorsePose.DRAW, 0).rest(NorsePose.FIGHT);
        }).direction("The ward goes out. All along the hall, the dead sit up in their graves, and every one of them turns to Hrafn.")
                .line(HRAFN, him, "Go. Up, and don't stop.")
                .ask(HRAFN, him, 20 * 9, 2,
                        say("Come with me.", c -> c.line(HRAFN, him, "If I go up, they follow me up. Into the village.")
                                .run(() -> him.look(b.vec(0D, Barrow.FLOOR + 2, 20D)))
                                .line(HRAFN, him, "They're mine. I'll keep them down here.")),
                        say("I'll hold them.", c -> c.line(HRAFN, him, "They're not yours to hold.")
                                .line(HRAFN, him, "If I go up, they follow me up. Into the village.")
                                .line(HRAFN, him, "They're mine. I'll keep them down here.")),
                        act("(Go.)", c -> c.pause(10)))
                .run(() -> him.watch(player))
                .line(HRAFN, him, "Tell them I broke it. Not you. Me.")
                .run(() -> {
                    him.watch(null);
                    him.walkTo(stand, 0.7D);
                });
        t.abandoned(() -> {
            him.watch(null);
            him.walkTo(stand, 0.7D);
        });
        story.talk(player, t);
    }

    /** The dead against Hrafn, below, until he falls. */
    private void fight(WorldServer world, long now) {
        Barrow b = barrow();
        StoryData d = data();
        if (b == null || d.seen("hrafn_fell")) {
            return;
        }
        EntityNorse h = alive(hrafn);
        fight++;
        // He fights where he stands; the first of them reach him soon enough.
        if (h != null) {
            EntityDraugr nearest = null;
            double best = 9D;
            for (EntityDraugr g : dead) {
                if (alive(g) != null && g.isAwake() && g.pose() != NorsePose.FALLEN && g.getDistanceSq(h) < best) {
                    best = g.getDistanceSq(h);
                    nearest = g;
                }
            }
            if (nearest != null) {
                h.look(nearest.getPositionEyes(1F));
                if (--swingIn <= 0 && h.pose() == NorsePose.FIGHT) {
                    h.act(NorsePose.SWING, 0);
                    swingIn = 22 + rand.nextInt(14);
                    pendingStrike = nearest;
                    strikeAt = fight + 7;
                }
            }
            if (pendingStrike != null && fight >= strikeAt) {
                if (alive(pendingStrike) != null && pendingStrike.getDistance(h) < 3D) {
                    pendingStrike.attackEntityFrom(DamageSource.causeMobDamage(h), 13F);
                    world.playSound(null, h.posX, h.posY + 1D, h.posZ, SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.NEUTRAL, 1F, 0.7F);
                }
                pendingStrike = null;
            }
        }
        EntityPlayerMP lead = StoryDirector.player(d.lead);
        // Out through the door while he fights: it shuts behind them.
        if (sealedAt < 0 && lead != null && lead.dimension == 0) {
            double[] l = b.local(lead.posX, lead.posY, lead.posZ);
            if (l[2] < -0.8D && Math.abs(l[0]) < 4D && l[1] > -2D && !anyoneElseInside(world, b, lead)) {
                sealOuter(world, b);
                sealedAt = now;
                d.oath = StoryData.Oath.SEALED;
                d.reopenAt = dawn(world);
                d.markDirty();
            }
        }
        boolean watched = lead != null && lead.dimension == 0 && b.inside(lead.posX, lead.posY, lead.posZ);
        boolean falls = h != null && (fight > FALL_WATCHED || blows > 30 && fight > FALL_WATCHED / 2);
        if (falls) {
            hrafnFalls(world, b, watched ? lead : null);
        }
    }

    @Nullable
    private EntityDraugr pendingStrike;
    private int strikeAt;

    private boolean anyoneElseInside(WorldServer world, Barrow b, EntityPlayer except) {
        for (EntityPlayer p : world.playerEntities) {
            if (p != except && !p.isSpectator() && b.inside(p.posX, p.posY, p.posZ)) {
                return true;
            }
        }
        return false;
    }

    /** Next dawn, or sooner if that is a long way off: hours later, at least. */
    private static long dawn(WorldServer world) {
        long time = world.getWorldTime();
        long day = time % 24000L;
        long toDawn = (23000L - day + 24000L) % 24000L;
        long wait = Math.max(2400L, Math.min(toDawn, 12000L));
        return world.getTotalWorldTime() + wait;
    }

    void draugrStruck(EntityDraugr draugr, EntityNorse npc) {
        if (npc == hrafn) {
            blows++;
        }
    }

    /** He goes down among them, and they lie down where they stand: the oath has had what it was owed. */
    private void hrafnFalls(WorldServer world, Barrow b, @Nullable EntityPlayerMP watching) {
        StoryData d = data();
        d.see("hrafn_fell");
        d.markDirty();
        EntityNorse h = alive(hrafn);
        if (h != null) {
            h.walkTo(null, 0D);
            h.watch(null).look(null);
            h.setArmed(false);
            h.act(NorsePose.DEAD).rest(NorsePose.DEAD);
            world.playSound(null, h.posX, h.posY, h.posZ, SoundEvents.ENTITY_PLAYER_DEATH, SoundCategory.NEUTRAL, 0.8F, 0.75F);
        }
        for (EntityDraugr g : dead) {
            if (alive(g) != null) {
                g.lieStill();
            }
        }
        EternityNetwork.sendAround(MsgStory.fx(MsgStory.FX_BELOW, b.doorMiddle(), Vec3d.ZERO, 0), 0, b.doorMiddle().x, b.doorMiddle().y,
                b.doorMiddle().z, 64D);
        if (watching != null && h != null) {
            // She is there when it happens; she was always going to be.
            mourning(world, b, watching, h, true);
        }
    }

    private void sealed(WorldServer world, long now) {
        Barrow b = barrow();
        StoryData d = data();
        if (b == null) {
            return;
        }
        if (!d.seen("hrafn_fell")) {
            fight(world, now);
            // Through the stone, the fight: less and less of it.
            if (now % 25 == 0) {
                int force = Math.max(1, 3 - (int) ((now - sealedAt) / 140));
                EternityNetwork.sendAround(MsgStory.fx(MsgStory.FX_BELOW, b.doorMiddle(), Vec3d.ZERO, force), 0, b.doorMiddle().x,
                        b.doorMiddle().y, b.doorMiddle().z, 40D);
            }
            if (sealedAt < 0 || now - sealedAt > FALL_SEALED) {
                hrafnFalls(world, b, null);
            }
            return;
        }
        if (now < d.reopenAt || d.seen("reopened")) {
            if (d.seen("reopened")) {
                laidOut(world, b);
            }
            return;
        }
        boolean anyone = false;
        for (EntityPlayer p : world.playerEntities) {
            anyone |= !p.isSpectator() && p.getDistanceSq(b.origin) < 96D * 96D;
        }
        if (!anyone || !b.loaded(world)) {
            return;
        }
        d.see("reopened");
        openOuter(world, b);
        laidOut(world, b);
    }

    /** After: Hrafn among his ancestors, and her by him, until someone comes down. */
    private void laidOut(WorldServer world, Barrow b) {
        StoryData d = data();
        if (d.seen("mourned")) {
            return;
        }
        EntityNorse h = alive(hrafn);
        if (h == null) {
            Vec3d at = b.vec(0D, Barrow.FLOOR + 1.02D, 13D);
            h = hrafn = EntityNorse.of(world, NorseBoxes.HRAFN, "hrafn");
            h.setLocationAndAngles(at.x, at.y, at.z, b.yawIn() + 160F, 0F);
            h.act(NorsePose.DEAD).rest(NorsePose.DEAD);
            world.spawnEntity(h);
        }
        EntityNorse she = alive(stranger);
        if (she == null || !"mourner".equals(she.role)) {
            if (she != null) {
                she.setDead();
            }
            she = stranger = EntityNorse.of(world, NorseBoxes.STRANGER, "mourner");
            Vec3d at = h.getPositionVector().add(new Vec3d(b.right.getDirectionVec()).scale(0.95D));
            she.setLocationAndAngles(at.x, h.posY, at.z, 0F, 0F);
            she.face(h.getPositionVector());
            she.act(NorsePose.TOUCH).rest(NorsePose.TOUCH);
            world.spawnEntity(she);
        }
        EntityPlayerMP lead = StoryDirector.player(d.lead);
        for (EntityPlayer p : world.playerEntities) {
            if (p instanceof EntityPlayerMP && !p.isSpectator() && p.getDistance(h) < 5.5D && !story.talking(p)) {
                mourning(world, b, (EntityPlayerMP) p, h, false);
                return;
            }
        }
    }

    /** The stranger by Hrafn: what she knew, and where the cube was kept before. */
    private void mourning(WorldServer world, Barrow b, EntityPlayerMP player, EntityNorse h, boolean comesIn) {
        StoryData d = data();
        d.see("mourned");
        d.markDirty();
        EntityNorse she;
        if (comesIn) {
            // Down the stair behind them, as if she had been coming all along.
            Vec3d top = b.vec(0D, -1D, 2D);
            she = EntityNorse.of(world, NorseBoxes.STRANGER, "mourner");
            she.setLocationAndAngles(top.x, top.y, top.z, b.yawIn(), 0F);
            world.spawnEntity(she);
            if (stranger != null) {
                stranger.setDead();
            }
            stranger = she;
        } else {
            she = stranger;
        }
        EntityNorse her = she;
        Vec3d kneel = h.getPositionVector().add(new Vec3d(b.right.getDirectionVec()).scale(0.95D));
        Talk t = new Talk(player).at(h, 32D);
        if (comesIn) {
            t.pause(60)
                    .run(() -> her.walkTo(kneel, 0.5D))
                    .until(her::arrived, 400)
                    .run(() -> {
                        her.face(h.getPositionVector());
                        her.act(NorsePose.TOUCH).rest(NorsePose.TOUCH);
                    })
                    .pause(40);
        }
        t.line(STRANGER, her, "He knew what it would cost before he opened the first door. He came anyway.")
                .ask(STRANGER, her,
                        say("You knew.", c -> c.pause(16).line(STRANGER, her, "Yes.")),
                        say("Did you know this would happen?", c -> c.pause(16).line(STRANGER, her, "Yes.")),
                        act("(Say nothing.)", c -> c.run(() -> her.act(NorsePose.CLOSE, 0)).pause(64)
                                .run(() -> her.act(NorsePose.TOUCH))))
                .line(STRANGER, her, "You'd have gone down there whether I told you or not. So would he.")
                .ask(STRANGER, her,
                        say("Who are you?", c -> c.run(() -> her.watch(player)).pause(20).line(STRANGER, her, "I came for the funeral.")),
                        say("What happens now?", c -> c.run(() -> her.watch(player))),
                        act("(Leave her with him.)", c -> c.direction("You turn to go. She speaks before you reach the stair.")))
                .line(STRANGER, her, "The cube was kept in Odin's treasure room a long time before he sent it here.")
                .line(STRANGER, her, "There's another one in there.")
                .pause(20)
                .line(STRANGER, her, "Odin is asleep. He won't sleep for ever.")
                .run(() -> {
                    her.watch(null).look(h.getPositionEyes(1F));
                    her.act(NorsePose.TOUCH).rest(NorsePose.TOUCH);
                    d.oath = StoryData.Oath.MOURNED;
                    d.markDirty();
                    nextVision.put(player.getUniqueID(), StoryDirector.now() + 200);
                });
        t.abandoned(() -> {
            d.oath = StoryData.Oath.MOURNED;
            d.markDirty();
        });
        story.talk(player, t);
    }

    // ------------------------------------------------------------------ the way on

    /** Whoever holds the cube is shown, now and then, the room it was kept in. */
    private void visions(MinecraftServer server, long now) {
        if (now % 10 != 0) {
            return;
        }
        for (EntityPlayerMP p : server.getPlayerList().getPlayers()) {
            // The cube shows where it was kept; so does the Aether in someone, which wants to go back to the Flame
            // (and is their only way back if they lost the cube in the vault).
            if (p.dimension == OdinsVault.dimension() || p.isSpectator() || !holdsCube(p) && !aetherCalls(p)) {
                continue;
            }
            Long next = nextVision.get(p.getUniqueID());
            if (next == null) {
                nextVision.put(p.getUniqueID(), now + 200 + rand.nextInt(400));
                continue;
            }
            if (now >= next) {
                int length = 50 + rand.nextInt(30);
                visionUntil.put(p.getUniqueID(), now + length + 6);
                nextVision.put(p.getUniqueID(), now + length + 500 + rand.nextInt(700));
                EternityNetwork.send(MsgStory.fx(MsgStory.VISION, p.getPositionVector(), Vec3d.ZERO, length), p);
            }
        }
    }

    private boolean aetherCalls(EntityPlayer p) {
        StoryData d = data();
        return StoryDirector.enabled() && d.player(p.getUniqueID()).aether && !d.flameSpent;
    }

    /** Someone reached for the vault while it was showing, with nothing in hand: the Aether in them takes them. */
    void reachForVision(EntityPlayerMP player) {
        if (aetherCalls(player)) {
            enterVision(player);
        }
    }

    private static boolean holdsCube(EntityPlayer p) {
        for (EnumHand hand : EnumHand.values()) {
            if (p.getHeldItem(hand).getItem() == HEItems.TESSERACT) {
                return true;
            }
        }
        return false;
    }

    /** Using the cube while it is showing the vault takes you there. */
    boolean useItem(EntityPlayerMP player, ItemStack stack) {
        if (stack.getItem() != HEItems.TESSERACT && !aetherCalls(player)) {
            return false;
        }
        return enterVision(player);
    }

    /** While the vault is showing, reaching for it takes them there. */
    private boolean enterVision(EntityPlayerMP player) {
        Long until = visionUntil.get(player.getUniqueID());
        if (until == null || StoryDirector.now() > until) {
            return false;
        }
        visionUntil.remove(player.getUniqueID());
        StoryData d = data();
        player.world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.BLOCK_PORTAL_TRAVEL, SoundCategory.PLAYERS, 0.4F, 1.6F);
        VaultDirector.INSTANCE.enter(player);
        if (d.oath == StoryData.Oath.MOURNED) {
            d.oath = StoryData.Oath.DONE;
        }
        d.player(player.getUniqueID()).beats.add("vault_by_cube");
        d.markDirty();
        return true;
    }

    // ------------------------------------------------------------------ people the world can't touch

    boolean interact(EntityPlayerMP player, EntityNorse npc) {
        StoryData d = data();
        Barrow b = barrow();
        if (npc == hrafn && b != null) {
            if (!d.seen("hrafn_met")) {
                meetHrafn(player, npc, b);
            } else if (npc.pose() == NorsePose.DEAD) {
                return true;
            } else if (d.oath == StoryData.Oath.BROKEN) {
                aside(player, npc, "Go!");
            } else if (d.innerOpen) {
                aside(player, npc, "Don't let me touch it.");
            } else if (d.outerOpen && b.inside(player.posX, player.posY, player.posZ)) {
                Talk t = new Talk(player).at(npc, 16D);
                hint(t, npc);
                story.talk(player, t);
            } else if (!d.outerOpen) {
                aside(player, npc, "When you're ready.");
            } else {
                aside(player, npc, "After you.");
            }
            return true;
        }
        if (npc == stranger) {
            if (d.oath == StoryData.Oath.SEEKING || d.oath == StoryData.Oath.VILLAGE) {
                EternityNetwork.send(MsgStory.line(DIRECTION, -1, "She doesn't look at you. Her eyes stay on the stones."), player);
            } else if (npc.pose() == NorsePose.TOUCH && d.seen("mourned")) {
                EternityNetwork.send(MsgStory.line(DIRECTION, -1, "She doesn't look up. Her hand stays where it is."), player);
            }
            return true;
        }
        return npc == ragna;
    }

    private void aside(EntityPlayerMP player, EntityNorse npc, String text) {
        Talk t = new Talk(player).at(npc, 16D);
        t.line(HRAFN, npc, text);
        story.talk(player, t);
    }

    void struck(EntityPlayerMP player, EntityNorse npc) {
        if (npc == hrafn && npc.pose() != NorsePose.DEAD && data().oath != StoryData.Oath.BROKEN) {
            cold(player, "He looks at you, and at your hand, and says nothing.");
        } else if (npc == stranger) {
            cold(player, "Your hand closes on nothing it can hold. She hasn't moved.");
        }
    }

    // ------------------------------------------------------------------ housekeeping

    /** People nobody is near any longer leave the world; a stranger nobody is watching goes. */
    private void tidy(WorldServer world) {
        StoryData d = data();
        if (alive(ragna) != null && d.seen("ragna_dead") && nobodyWithin(world, ragna, 64D)) {
            ragna.setDead();
            ragna = null;
        }
        EntityNorse she = alive(stranger);
        if (she != null) {
            boolean leaving = she.getNavigator().getPath() != null || she.restPose() == NorsePose.STAND && d.seen("ragna_dead")
                    && "stranger".equals(she.role) && (d.seen("hrafn_asked") || d.oath == StoryData.Oath.SEEKING && she.getDistanceSq(d.ragna) < 120D * 120D);
            EntityPlayer closest = world.getClosestPlayerToEntity(she, 32D);
            boolean done = "mourner".equals(she.role) && d.oath.ordinal() >= StoryData.Oath.MOURNED.ordinal() && (closest == null || !story.talking(closest));
            if ((leaving || done) && !watched(world, she)) {
                she.setDead();
                stranger = null;
            } else if (nobodyWithin(world, she, 128D)) {
                she.setDead();
                stranger = null;
            }
        }
        for (int i = 0; i < dead.length; i++) {
            if (alive(dead[i]) != null && nobodyWithin(world, dead[i], 96D)) {
                dead[i].setDead();
                dead[i] = null;
            }
        }
        if (alive(hrafn) != null && nobodyWithin(world, hrafn, 128D)) {
            hrafn.setDead();
            hrafn = null;
        }
    }

    private static boolean nobodyWithin(World world, Entity e, double r) {
        for (EntityPlayer p : world.playerEntities) {
            if (!p.isSpectator() && p.getDistance(e) < r) {
                return false;
            }
        }
        return true;
    }

    /** Somebody can see them: they are in front of someone, and nothing is in the way. */
    static boolean watched(World world, Entity e) {
        for (EntityPlayer p : world.playerEntities) {
            if (!p.isSpectator() && p.getDistance(e) < 64D && sees(p, e.getPositionEyes(1F)) && p.canEntityBeSeen(e)) {
                return true;
            }
        }
        return false;
    }

    static boolean sees(EntityPlayer p, Vec3d at) {
        Vec3d to = at.subtract(p.getPositionEyes(1F));
        double len = to.length();
        return len < 1.5D || p.getLookVec().dotProduct(to.scale(1D / len)) > 0.45D;
    }

    @Nullable
    static <T extends Entity> T alive(@Nullable T e) {
        return e != null && !e.isDead && e.world.loadedEntityList.contains(e) ? e : null;
    }

    // ------------------------------------------------------------------ commands

    @Nullable
    String command(EntityPlayerMP player, String action, String[] args) {
        StoryData d = data();
        WorldServer world = player.getServerWorld();
        switch (action) {
            case "begin":
                return begin(world, player, true);
            case "leak": {
                if (d.village == null) {
                    begin(world, player, true);
                }
                if (d.village == null) {
                    return "No village to leak from.";
                }
                Vec3d look = player.getLookVec();
                BlockPos at = new BlockPos(player.posX + look.x * 14D, player.posY, player.posZ + look.z * 14D);
                int kind = args.length > 0 ? MathHelper.clamp(Integer.parseInt(args[0]), 0, 5) : Leaks.kind(d.leaks, rand);
                Leaks.land(world, at, new Vec3d(d.village), kind, 3, rand, kind == Leaks.STONE);
                d.leaks++;
                d.markDirty();
                return "A piece of the north lands in front of you.";
            }
            case "home": {
                BlockPos home = player.getBedLocation(0);
                if (home == null) {
                    return "You have no bed.";
                }
                d.taken += Leaks.takeFromHome(world, player, home, rand);
                d.markDirty();
                return "The north takes from round your bed.";
            }
            case "ragna": {
                if (d.village == null) {
                    begin(world, player, true);
                }
                d.beats.remove("ragna_scene");
                d.beats.remove("ragna_dead");
                String r = placeRagna(world, player);
                return r != null ? r : "Nowhere open enough behind you.";
            }
            case "village": {
                if (d.village == null) {
                    begin(world, player, true);
                }
                if (d.village == null) {
                    return "No village.";
                }
                if (d.oath.ordinal() < StoryData.Oath.SEEKING.ordinal()) {
                    d.oath = StoryData.Oath.SEEKING;
                    d.see("ragna_scene");
                    d.see("ragna_dead");
                }
                d.lead = player.getUniqueID();
                if (!d.dressed) {
                    dress(world);
                }
                Barrow b = barrow();
                Vec3d to = b.vec(0D, 0D, -12D);
                com.matoon.eternity.world.Teleport.player(player, 0, to.x, to.y + 2D, to.z, true);
                return "At the mound.";
            }
            case "goto": {
                String where = args.length > 0 ? args[0] : "mound";
                Barrow b = barrow();
                Vec3d to, face;
                if ("ragna".equals(where) && d.ragna != null) {
                    Vec3d c = new Vec3d(d.ragna).add(0.5D, 0D, 0.5D);
                    to = c.add(new Vec3d(player.posX - c.x, 0D, player.posZ - c.z).normalize().scale(16D));
                    face = c;
                } else if (b != null && "step".equals(where)) {
                    to = b.vec(0D, 0D, -1.6D);
                    face = b.doorMiddle();
                } else if (b != null && "graves".equals(where)) {
                    to = b.vec(1.2D, 0D, -8.6D);
                    face = b.vec(4D, 1.2D, -6D);
                } else if (b != null && "hall".equals(where)) {
                    to = b.vec(0D, Barrow.FLOOR + 1, 11D);
                    face = b.vec(0D, Barrow.FLOOR + 1, 20D);
                } else if (b != null && "door".equals(where)) {
                    to = b.vec(0D, Barrow.FLOOR + 1, Barrow.INNER - 3D);
                    face = b.innerMiddle();
                } else if (b != null && "ward".equals(where)) {
                    to = b.vec(0D, Barrow.FLOOR + 1, Barrow.WARD - 3D);
                    face = b.cube();
                } else if (b != null) {
                    to = b.vec(0D, 0D, -12D);
                    face = b.doorMiddle();
                } else {
                    return "Nowhere to go.";
                }
                double dx = face.x - to.x, dz = face.z - to.z;
                player.rotationYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
                player.rotationPitch = 0F;
                com.matoon.eternity.world.Teleport.player(player, 0, to.x, to.y + 0.1D, to.z, !"hall".equals(where) && !"door".equals(where) && !"ward".equals(where));
                return "There.";
            }
            case "dress":
                d.dressed = false;
                return dress(world);
            case "words":
                d.linesFound = (1 << Stones.KEEPERS) - 1;
                d.markDirty();
                return "Every keeper's words are known.";
            case "open": {
                Barrow b = barrow();
                if (b == null) {
                    return "No barrow.";
                }
                d.see("hrafn_met");
                d.see("hrafn_asked");
                d.see("barrow_opened");
                openOuter(world, b);
                return "The outer door is open.";
            }
            case "inner": {
                Barrow b = barrow();
                if (b == null) {
                    return "No barrow.";
                }
                openInner(world, b);
                lightRunes(world, b, Stones.KEEPERS);
                return "The oath door is open.";
            }
            case "lift": {
                Barrow b = barrow();
                if (b == null) {
                    return "No barrow.";
                }
                if (!d.outerOpen) {
                    openOuter(world, b);
                }
                if (!d.innerOpen) {
                    openInner(world, b);
                }
                Vec3d c = b.vec(0D, Barrow.FLOOR + 1, Barrow.WARD - 2.5D);
                com.matoon.eternity.world.Teleport.player(player, 0, c.x, c.y, c.z, false);
                lift(player, b);
                return "The cube is lifted.";
            }
            case "fall": {
                Barrow b = barrow();
                if (b != null && !d.seen("hrafn_fell")) {
                    hrafnFalls(world, b, b.inside(player.posX, player.posY, player.posZ) ? player : null);
                }
                return "Hrafn falls.";
            }
            case "reopen":
                d.reopenAt = StoryDirector.now();
                d.markDirty();
                return "The barrow opens.";
            case "vision":
                nextVision.put(player.getUniqueID(), StoryDirector.now());
                if (d.oath.ordinal() < StoryData.Oath.MOURNED.ordinal()) {
                    d.oath = StoryData.Oath.MOURNED;
                    d.markDirty();
                }
                return "The cube shows you where it was kept, if you are holding it.";
            default:
                return null;
        }
    }
}
