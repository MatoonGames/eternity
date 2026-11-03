package com.matoon.eternity.world;

import com.matoon.eternity.Eternity;
import com.matoon.eternity.EternityConfig;
import com.matoon.eternity.EternitySounds;
import com.matoon.eternity.block.BlockBoard;
import com.matoon.eternity.block.EternityBlocks;
import com.matoon.eternity.compat.InfinityStones;
import com.matoon.eternity.compat.Relics;
import com.matoon.eternity.entity.EntityAdamWarlock;
import com.matoon.eternity.entity.EntityGrandmaster;
import com.matoon.eternity.entity.EntityGrandmaster.State;
import com.matoon.eternity.entity.EntityMagus;
import com.matoon.eternity.entity.NorsePose;
import com.matoon.eternity.item.ItemAstralRegulator;
import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.network.MsgGrandmaster;
import com.matoon.eternity.story.Speakers;
import com.matoon.eternity.story.StoryDirector;
import lucraft.mods.lucraftcore.infinity.EnumInfinityStone;
import lucraft.mods.lucraftcore.infinity.items.InventoryInfinityGauntlet;
import lucraft.mods.lucraftcore.infinity.items.ItemInfinityGauntlet;
import lucraft.mods.lucraftcore.infinity.items.ItemInfinityStone;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Chapter six: the Grandmaster comes to whoever holds five stones and lays his board out
 * around them. Everything the encounter is happens here, on the server.
 * <ol>
 * <li><b>The board.</b> Seven squares by seven, three blocks to a square, gold and blue with
 * the throne at its heart and the refuges at its corners, raised in rings out of wherever the
 * player stands. Everything it covers is kept and put back afterwards.</li>
 * <li><b>The opening.</b> He names the stones they hold and his brothers who held them, the
 * Mind Stone leaps about his board, and Adam Warlock arrives to play against them: he has
 * been promised the stone, for Mother, if he wins.</li>
 * <li><b>The stakes.</b> His life is on his side of the table. They lay their own on the
 * pedestal at the board's edge: every stone they carry, or the Astral Regulator.</li>
 * <li><b>The game.</b> {@link Kingstable}, played with the pieces themselves: they lay a hand on
 * one of their own and then on the square it should go to. Warlock flies to his raiders and
 * carries them.</li>
 * <li><b>The cheats.</b> He wants the best game in a million years, not a short one, and he
 * decides who wins. Whenever the king is about to escape the board shifts under him; while
 * Warlock plays, whenever the king is about to fall, the board saves him. After the second
 * time, Warlock sees it, and walks off the board. The Grandmaster plays on himself.</li>
 * <li><b>The way out.</b> The board is the device that keeps the stone in play, and his life
 * is wound into it. Every blow on a square cracks it and makes the stone stutter; enough of
 * them and the board breaks, and he dies among his pieces, pleased. The stone lies still,
 * the pot comes back, and if Warlock walked, he has one thing to say before he goes.</li>
 * </ol>
 * On the Magus's road (whoever holds the stones saw him rise, and Warlock stands with them,
 * and the Power Stone is gone for good, so four stones bring him) it goes another way. A
 * challenger has asked for the game by name: the Magus comes down out of the sky to play the
 * raiders, and Warlock lands at their side, to talk them through it and never touch a piece.
 * The first time the board shifts, the Magus names it, and stops playing by the rules: he moves
 * raiders when he likes, and the squares he touches go white. Frightened for the first time in
 * a very long time, the Grandmaster takes their side and throws everything he has at him, and
 * none of it is enough. The Magus takes the Mind Stone from him and goes; the Grandmaster
 * lives, and wishes he had not seen it.
 */
public final class GrandmasterDirector {

    public static final GrandmasterDirector INSTANCE = new GrandmasterDirector();

    /** Blocks to a square, squares to a side, and the board's width with its rim. */
    public static final int SQ = 3, N = Kingstable.SIZE, WIDTH = N * SQ + 2;
    /** The cleared box: the board, the walk to the pedestal, and the air over both. */
    private static final int DEPTH = WIDTH + 2, HEIGHT = 9;

    private static final int BUILD_TICKS = 50;
    private static final int PLAYER_SLIDE = 14, CARRY_TICKS = 34, CONDUCT_TICKS = 30, CONJURE_TICKS = 26;
    /** Piece animations, as the client draws them. */
    public static final int ANIM_SLIDE = 0, ANIM_CARRY = 1, ANIM_CONDUCT = 2, ANIM_CONJURE = 3;

    /** SEIZED: nobody's move any more; the Magus moves raiders whenever he likes. */
    private enum Stage { WAIT, PLAYER_MOVE, THINK, APPROACH, CARRY, SETTLE, CHEAT, SCENE, PAUSE, SEIZED }

    private final Random rand = new Random();
    private GrandmasterData data;
    private Kingstable game;

    // Transient: rebuilt from the data after a restart.
    @Nullable
    private EntityGrandmaster body;
    @Nullable
    private EntityAdamWarlock warlock;
    private int phaseTicks;
    private Stage stage = Stage.WAIT;
    private int stageTicks;
    private int selected = -1;
    private int[] targets = new int[0];
    private int raiderMove = -1;
    private final List<int[]> cheats = new ArrayList<>();
    private int[] anim;
    private long animStart;
    private int[] animTaken = new int[0];
    private final List<Beat> script = new ArrayList<>();
    private int scriptTicks;
    private Runnable scriptDone;
    private int lastStrike = -100;
    private int strikes;
    private int lastGroan = -1000;
    /** Cheats he had made when Warlock walked, and whether Warlock has said his piece about the board since. */
    private int cheatsAtWalk;
    private boolean warlockHinted;
    private int buildRing = -1;
    private int restoreRing = -1;
    private boolean scene6b;
    private boolean pendingWake;
    @Nullable
    private java.util.UUID rulesWatcher;
    private boolean rulesOver;
    private int hintTicks;
    private int dirty;
    /** The board is broken and the stone has stopped where it fell; it is picked up once the ground is back. */
    private boolean stopped;
    @Nullable
    private Vec3d restAt;
    @Nullable
    private EntityMagus magus;
    /** The Magus has named the cheat: the rules are off, and he moves what he likes, when he likes. */
    private boolean seized;
    /** Raiders he has moved out of turn, and how long until the next. */
    private int outOfTurn;
    private int nextSeize;
    /** The player's move Warlock last said something about, so he says something only now and then. */
    private int advisedAt = -2;
    /** The squares Warlock has lit for them, until they move: from, to, and what it takes. */
    @Nullable
    private int[] hinted;
    /** The board stood up on end around the Magus, block by block, for him to break. */
    private final List<BlockPos> walls = new ArrayList<>();

    private static final class Beat {
        final int at;
        final Runnable run;

        Beat(int at, Runnable run) {
            this.at = at;
            this.run = run;
        }
    }

    private GrandmasterDirector() {
    }

    private GrandmasterData data() {
        if (data == null) {
            data = GrandmasterData.get();
        }
        return data;
    }

    public void reset() {
        data = null;
        game = null;
        body = null;
        warlock = null;
        script.clear();
        cheats.clear();
        stage = Stage.WAIT;
        selected = -1;
        targets = new int[0];
        anim = null;
        buildRing = restoreRing = -1;
        scene6b = false;
        warlockHinted = false;
        cheatsAtWalk = 0;
        stopped = false;
        restAt = null;
        strikes = 0;
        magus = null;
        seized = false;
        outOfTurn = 0;
        advisedAt = -2;
        hinted = null;
        walls.clear();
    }

    // ------------------------------------------------------------------ geometry

    private BlockPos origin() {
        return data().origin;
    }

    /** The middle of a square, on its surface. */
    public Vec3d centre(int square) {
        BlockPos o = origin();
        return new Vec3d(o.getX() + 1 + Kingstable.x(square) * SQ + SQ / 2D, o.getY() + 1, o.getZ() + 1 + Kingstable.y(square) * SQ + SQ / 2D);
    }

    private Vec3d middle() {
        BlockPos o = origin();
        return new Vec3d(o.getX() + WIDTH / 2D, o.getY() + 1, o.getZ() + WIDTH / 2D);
    }

    /** Where stakes are laid: a pedestal off the south edge. */
    private BlockPos pedestal() {
        BlockPos o = origin();
        return new BlockPos(o.getX() + WIDTH / 2, o.getY() + 1, o.getZ() + WIDTH + 1);
    }

    /** The square a block of the board belongs to, or -1 for the rim and anything else. */
    public int squareAt(BlockPos pos) {
        BlockPos o = origin();
        if (pos.getY() != o.getY()) {
            return -1;
        }
        int dx = pos.getX() - o.getX() - 1, dz = pos.getZ() - o.getZ() - 1;
        if (dx < 0 || dz < 0 || dx >= N * SQ || dz >= N * SQ) {
            return -1;
        }
        return Kingstable.at(dx / SQ, dz / SQ);
    }

    private boolean onBoard(BlockPos pos) {
        BlockPos o = origin();
        return pos.getX() >= o.getX() && pos.getX() < o.getX() + WIDTH && pos.getZ() >= o.getZ() && pos.getZ() < o.getZ() + DEPTH
                && pos.getY() >= o.getY() && pos.getY() < o.getY() + HEIGHT;
    }

    private static IBlockState face(int dx, int dz) {
        BlockBoard.Face face;
        if (dx == 0 || dz == 0 || dx == WIDTH - 1 || dz == WIDTH - 1) {
            face = BlockBoard.Face.RIM;
        } else {
            int sq = Kingstable.at((dx - 1) / SQ, (dz - 1) / SQ);
            face = Kingstable.isThrone(sq) ? BlockBoard.Face.THRONE : Kingstable.isRefuge(sq) ? BlockBoard.Face.REFUGE
                    : (Kingstable.x(sq) + Kingstable.y(sq)) % 2 == 0 ? BlockBoard.Face.GOLD : BlockBoard.Face.BLUE;
        }
        return EternityBlocks.BOARD.getDefaultState().withProperty(BlockBoard.COLOR, face);
    }

    @Nullable
    private WorldServer world() {
        return DimensionManager.getWorld(data().dimension);
    }

    @Nullable
    private EntityPlayerMP player() {
        WorldServer world = world();
        if (world == null || data().player == null) {
            return null;
        }
        EntityPlayer player = world.getPlayerEntityByUUID(data().player);
        return player instanceof EntityPlayerMP && !player.isDead ? (EntityPlayerMP) player : null;
    }

    // ------------------------------------------------------------------ beginning

    /**
     * Whether he will come for this player, now or soon: five stones and no Mind Stone, and he is still alive. On the
     * Magus's road the Power Stone is gone for good, so one fewer will do.
     */
    private boolean due(EntityPlayerMP player) {
        int mask = InfinityStones.scan(player);
        int need = EternityConfig.grandmaster.stonesToCome - (StoryDirector.INSTANCE.magusRisen(player) ? 1 : 0);
        return EternityConfig.grandmaster.enabled && !data().finished && !data().active() && player.dimension == 0
                && !player.isSpectator() && InfinityStones.count(mask) >= need
                && (mask & InfinityStones.bit(EnumInfinityStone.MIND)) == 0 && !Relics.isClaimed(Relics.Kind.MIND_STONE);
    }

    /** He lays his board out around the player; the Magus comes to play if they saw him rise. Returns why not, or null. */
    @Nullable
    public String begin(EntityPlayerMP player) {
        return begin(player, StoryDirector.INSTANCE.magusRisen(player));
    }

    /** As {@link #begin(EntityPlayerMP)}, with the Magus to play them whatever they have seen (for testing). */
    @Nullable
    public String beginMagus(EntityPlayerMP player) {
        return begin(player, true);
    }

    @Nullable
    private String begin(EntityPlayerMP player, boolean magusPlays) {
        GrandmasterData d = data();
        if (d.active()) {
            return "The Grandmaster is already playing " + d.playerName + ".";
        }
        if (EternityBlocks.BOARD == null) {
            return "The board is not registered.";
        }
        reset();
        d = data();
        WorldServer world = player.getServerWorld();
        BlockPos feet = new BlockPos(player.posX, player.posY + 0.2D, player.posZ);
        BlockPos o = new BlockPos(MathHelper.floor(player.posX) - WIDTH / 2, feet.getY() - 1, MathHelper.floor(player.posZ) - WIDTH / 2);
        if (o.getY() < 2 || o.getY() + HEIGHT > world.getHeight()) {
            return "There is no room here for his board.";
        }
        d.origin = o;
        d.dimension = player.dimension;
        d.player = player.getUniqueID();
        d.playerName = player.getName();
        d.phase = GrandmasterData.Phase.ARRIVING;
        d.integrity = EternityConfig.grandmaster.boardStrength;
        d.cracks = new int[Kingstable.CELLS];
        d.cheats = 0;
        d.games = 0;
        d.playerMoves = 0;
        d.potLost = false;
        d.staked = false;
        d.pot.clear();
        d.warlock = GrandmasterData.Warlock.ABSENT;
        d.magus = magusPlays;
        cover(world);
        game = Kingstable.opening(rand);
        d.game = game.save();
        d.markDirty();
        phaseTicks = 0;
        buildRing = 0;
        Eternity.LOG.info("The Grandmaster lays his board out around {} at {}{}.", player.getName(), o, magusPlays ? ", for the Magus" : "");
        return null;
    }

    /** Remembers everything the board and the air over it will cover. */
    private void cover(WorldServer world) {
        GrandmasterData d = data();
        BlockPos o = d.origin;
        d.covered = new int[WIDTH * DEPTH * HEIGHT];
        d.coveredTiles = new NBTTagList();
        for (int dy = 0; dy < HEIGHT; dy++) {
            for (int dz = 0; dz < DEPTH; dz++) {
                for (int dx = 0; dx < WIDTH; dx++) {
                    BlockPos pos = o.add(dx, dy, dz);
                    IBlockState state = world.getBlockState(pos);
                    d.covered[(dy * DEPTH + dz) * WIDTH + dx] = Block.getStateId(state);
                    TileEntity te = world.getTileEntity(pos);
                    if (te != null) {
                        d.coveredTiles.appendTag(te.writeToNBT(new NBTTagCompound()));
                    }
                }
            }
        }
    }

    /** Whether this block of the cleared box is part of the board, the walk or the pedestal, and if so what it is. */
    @Nullable
    private static IBlockState planned(int dx, int dy, int dz) {
        if (dy == 0 && dz < WIDTH) {
            return face(dx, dz);
        }
        if (dy == 0 && dx == WIDTH / 2 || dy == 1 && dx == WIDTH / 2 && dz == WIDTH + 1) {
            return EternityBlocks.BOARD.getDefaultState().withProperty(BlockBoard.COLOR, BlockBoard.Face.RIM);
        }
        return null;
    }

    /** Raises one ring of the board, counted out from its middle, clearing the air over it. */
    private void buildRing(WorldServer world, int ring) {
        BlockPos o = origin();
        int c = WIDTH / 2;
        for (int dz = 0; dz < DEPTH; dz++) {
            for (int dx = 0; dx < WIDTH; dx++) {
                if (Math.max(Math.abs(dx - c), Math.abs(dz - c)) != ring) {
                    continue;
                }
                for (int dy = 0; dy < HEIGHT; dy++) {
                    BlockPos pos = o.add(dx, dy, dz);
                    IBlockState want = planned(dx, dy, dz);
                    boolean walk = dz >= WIDTH && (dx != WIDTH / 2);
                    if (walk && want == null) {
                        // Off the board's edge, beside the walk, the world is left alone.
                        continue;
                    }
                    set(world, pos, want == null ? Blocks.AIR.getDefaultState() : want);
                }
            }
        }
        EternityNetwork.sendAround(MsgGrandmaster.fx(MsgGrandmaster.FX_BUILD, middle(), Vec3d.ZERO, ring), data().dimension,
                middle().x, middle().y, middle().z, 160D);
    }

    /** Sets a block without dropping what was there or waking its neighbours: no item spills, no water floods in. */
    private static void set(World world, BlockPos pos, IBlockState state) {
        if (world.getTileEntity(pos) != null) {
            world.removeTileEntity(pos);
        }
        world.setBlockState(pos, state, 2 | 16);
    }

    /** Puts back one ring of whatever the board covered. */
    private void restoreRing(WorldServer world, int ring) {
        GrandmasterData d = data();
        BlockPos o = d.origin;
        int c = WIDTH / 2;
        if (d.covered.length != WIDTH * DEPTH * HEIGHT) {
            return;
        }
        for (int dz = 0; dz < DEPTH; dz++) {
            for (int dx = 0; dx < WIDTH; dx++) {
                if (Math.max(Math.abs(dx - c), Math.abs(dz - c)) != ring) {
                    continue;
                }
                for (int dy = 0; dy < HEIGHT; dy++) {
                    BlockPos pos = o.add(dx, dy, dz);
                    IBlockState was = Block.getStateById(d.covered[(dy * DEPTH + dz) * WIDTH + dx]);
                    if (world.getBlockState(pos) != was) {
                        set(world, pos, was);
                    }
                }
            }
        }
        for (int i = 0; i < d.coveredTiles.tagCount(); i++) {
            NBTTagCompound tag = d.coveredTiles.getCompoundTagAt(i);
            BlockPos pos = new BlockPos(tag.getInteger("x"), tag.getInteger("y"), tag.getInteger("z"));
            int dx = pos.getX() - o.getX(), dz = pos.getZ() - o.getZ();
            if (Math.max(Math.abs(dx - c), Math.abs(dz - c)) == ring) {
                TileEntity te = TileEntity.create(world, tag);
                if (te != null) {
                    world.setTileEntity(pos, te);
                }
            }
        }
    }

    private void restoreAll(WorldServer world) {
        for (int ring = 0; ring <= WIDTH; ring++) {
            restoreRing(world, ring);
        }
    }

    // ------------------------------------------------------------------ ending it early (operators)

    /**
     * Takes the board away and puts back what it covered, gives the pot back, and sends
     * everyone home. Nothing is decided: he can come again.
     */
    public String end() {
        GrandmasterData d = data();
        if (!d.active()) {
            return "The Grandmaster is not playing anyone.";
        }
        WorldServer world = world();
        if (world != null) {
            restoreAll(world);
        }
        returnPot();
        dismissAll();
        d.phase = GrandmasterData.Phase.NONE;
        d.markDirty();
        EternityNetwork.sendToAll(MsgGrandmaster.clear());
        String who = d.playerName;
        reset();
        return "The Grandmaster has packed his board away; everything it covered around " + who + " is back.";
    }

    /** Forgets that he ever came, so he can come again (after an end, or for testing). */
    public String forget() {
        end();
        GrandmasterData d = data();
        d.finished = false;
        d.warlockRefused = false;
        d.phase = GrandmasterData.Phase.NONE;
        d.markDirty();
        return "The Grandmaster's game is forgotten. He can come again.";
    }

    private void dismissAll() {
        if (body != null && !body.isDead) {
            body.setDead();
        }
        body = null;
        if (warlock != null && !warlock.isDead) {
            warlock.dismiss();
        }
        warlock = null;
        if (magus != null && !magus.isDead) {
            magus.setDead();
        }
        magus = null;
    }

    private void returnPot() {
        GrandmasterData d = data();
        EntityPlayerMP player = player();
        for (ItemStack stack : d.pot) {
            give(player, stack.copy());
        }
        d.pot.clear();
        d.potLost = false;
        d.markDirty();
    }

    private void give(@Nullable EntityPlayerMP player, ItemStack stack) {
        if (player != null && player.inventory.addItemStackToInventory(stack)) {
            player.inventoryContainer.detectAndSendChanges();
            return;
        }
        WorldServer world = world();
        if (world == null) {
            Eternity.LOG.error("The Grandmaster's pot could not be returned: {} is lost.", stack);
            return;
        }
        Vec3d at = player != null ? player.getPositionVector() : new Vec3d(pedestal()).add(0.5D, 1.5D, 0.5D);
        EntityItem item = new EntityItem(world, at.x, at.y + 0.5D, at.z, stack);
        item.setNoDespawn();
        world.spawnEntity(item);
    }

    // ------------------------------------------------------------------ ticking

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || DimensionManager.getWorld(0) == null) {
            return;
        }
        GrandmasterData d = data();
        if (!d.active()) {
            return;
        }
        WorldServer world = world();
        if (world == null) {
            return;
        }
        if (game == null) {
            game = Kingstable.load(d.game);
            resume();
        }
        EntityPlayerMP player = player();
        boolean here = player != null && player.dimension == d.dimension && player.getDistanceSq(middle().x, middle().y, middle().z) < 96D * 96D
                && world.isBlockLoaded(new BlockPos(middle()));
        if (!here && buildRing < 0 && restoreRing < 0 && d.phase != GrandmasterData.Phase.COLLAPSE) {
            // Nobody to play: he waits, as long as it takes.
            return;
        }
        phaseTicks++;
        ensureBodies(world);
        if (buildRing >= 0) {
            if (phaseTicks % 3 == 0) {
                buildRing(world, buildRing++);
                if (buildRing > WIDTH) {
                    buildRing = -1;
                    sync();
                }
            }
        }
        if (restoreRing >= 0) {
            if (phaseTicks % 2 == 0) {
                restoreRing(world, restoreRing++);
                if (restoreRing > WIDTH) {
                    restoreRing = -1;
                    finish();
                    return;
                }
            }
        }
        tickScript();
        switch (d.phase) {
            case ARRIVING:
                arriving(world, player);
                break;
            case STAKES:
                stakes(player);
                break;
            case PLAYING:
                playing(world, player);
                break;
            default:
                break;
        }
        if (body != null) {
            body.setBroken(1F - d.integrity / (float) Math.max(1, EternityConfig.grandmaster.boardStrength));
        }
        if (dirty > 0 && --dirty == 0) {
            sync();
        }
        if (phaseTicks % 100 == 0) {
            // Anyone who has just come within sight of the board sees it as it is.
            sync();
        }
    }

    /** After a restart: whatever was happening picks up at the start of its part. */
    private void resume() {
        GrandmasterData d = data();
        stage = Stage.WAIT;
        phaseTicks = 0;
        switch (d.phase) {
            case ARRIVING:
            case OPENING:
                d.phase = GrandmasterData.Phase.OPENING;
                opening();
                break;
            case STAKES:
                stakesBegin();
                break;
            case RULES:
                rules();
                break;
            case PLAYING:
                stage = game.raidersToMove ? Stage.THINK : Stage.WAIT;
                if (d.magus && d.cheats > 0) {
                    // He had cheated in front of the Magus: the board is the Magus's again, from the first of it.
                    seize();
                }
                break;
            case LOST:
                newGame();
                break;
            case COLLAPSE:
                restoreRing = 0;
                break;
            default:
                break;
        }
        sync();
    }

    /** Puts him, and Warlock while he is still part of it, back on the board if they are not there. */
    private void ensureBodies(WorldServer world) {
        GrandmasterData d = data();
        if (body == null || body.isDead || body.world != world || !body.addedToChunk) {
            body = null;
            for (Entity e : world.loadedEntityList) {
                if (e instanceof EntityGrandmaster && !e.isDead) {
                    body = (EntityGrandmaster) e;
                }
            }
            if (body == null && d.phase != GrandmasterData.Phase.ARRIVING && d.phase != GrandmasterData.Phase.DONE
                    && !(d.phase == GrandmasterData.Phase.COLLAPSE && phaseTicks > 260)) {
                body = new EntityGrandmaster(world);
                Vec3d at = centre(Kingstable.at(4, 4));
                body.setPosition(at.x, at.y, at.z);
                body.act(State.IDLE, -1);
                world.spawnEntity(body);
            }
            if (body != null) {
                body.setManaged(true);
            }
        }
        boolean wanted = d.warlock == GrandmasterData.Warlock.PLAYING || d.warlock == GrandmasterData.Warlock.WALKED
                || d.warlock == GrandmasterData.Warlock.ALLIED;
        if (wanted && (warlock == null || warlock.isDead || warlock.world != world || !warlock.addedToChunk)) {
            warlock = null;
            for (Entity e : world.loadedEntityList) {
                if (e instanceof EntityAdamWarlock && !e.isDead) {
                    warlock = (EntityAdamWarlock) e;
                }
            }
            if (warlock == null) {
                warlock = new EntityAdamWarlock(world);
                Vec3d post = d.warlock == GrandmasterData.Warlock.WALKED ? warlockAside()
                        : d.warlock == GrandmasterData.Warlock.ALLIED ? warlockBeside() : warlockPost();
                warlock.setPosition(post.x, post.y, post.z);
                world.spawnEntity(warlock);
                warlock.script(true);
                if (d.warlock == GrandmasterData.Warlock.WALKED || d.warlock == GrandmasterData.Warlock.ALLIED) {
                    warlock.direct(EntityAdamWarlock.State.STAND, post, middle(), 0.1D);
                } else {
                    warlock.direct(EntityAdamWarlock.State.HOVER, post, middle(), 0.1D);
                }
            }
            warlock.script(true);
        }
        // The Magus, from the moment he has come down to the board until he leaves it.
        boolean present = d.magus && (d.phase == GrandmasterData.Phase.STAKES || d.phase == GrandmasterData.Phase.RULES
                || d.phase == GrandmasterData.Phase.PLAYING || d.phase == GrandmasterData.Phase.LOST);
        if (present && (magus == null || magus.isDead || magus.world != world || !magus.addedToChunk)) {
            magus = null;
            for (Entity e : world.loadedEntityList) {
                if (e instanceof EntityMagus && !e.isDead && e.getDistanceSq(middle().x, middle().y, middle().z) < 64D * 64D) {
                    magus = (EntityMagus) e;
                }
            }
            if (magus == null) {
                magus = new EntityMagus(world);
                Vec3d post = warlockPost();
                magus.setPosition(post.x, post.y, post.z);
                magus.rest(NorsePose.HOVER);
                magus.act(NorsePose.HOVER);
                magus.hoverTo(post, 0.1D);
                magus.look(middle());
                if (!world.spawnEntity(magus)) {
                    magus = null;
                }
            }
        }
    }

    /** Where Warlock hangs in the air when it is not his move: over the far edge, looking down the board. */
    private Vec3d warlockPost() {
        BlockPos o = origin();
        return new Vec3d(o.getX() + WIDTH / 2D, o.getY() + 4.5D, o.getZ() - 1.5D);
    }

    /** Where he stands once he has walked off it. */
    private Vec3d warlockAside() {
        BlockPos o = origin();
        return new Vec3d(o.getX() + WIDTH + 3.5D, o.getY() + 1D, o.getZ() + 3.5D);
    }

    /** Where he stands on their side of it: on the rim at the near edge, a little along from the walk to the pedestal. */
    private Vec3d warlockBeside() {
        BlockPos o = origin();
        return new Vec3d(o.getX() + WIDTH / 2D + 3.5D, o.getY() + 1D, o.getZ() + WIDTH - 0.5D);
    }

    // ------------------------------------------------------------------ the script

    private void at(int tick, Runnable run) {
        script.add(new Beat(tick, run));
    }

    private void play(Runnable done) {
        scriptTicks = 0;
        scriptDone = done;
    }

    private void tickScript() {
        if (script.isEmpty() && scriptDone == null) {
            return;
        }
        Iterator<Beat> it = script.iterator();
        List<Beat> due = new ArrayList<>();
        while (it.hasNext()) {
            Beat b = it.next();
            if (b.at <= scriptTicks) {
                due.add(b);
                it.remove();
            }
        }
        scriptTicks++;
        for (Beat b : due) {
            b.run.run();
        }
        if (script.isEmpty() && scriptDone != null) {
            Runnable done = scriptDone;
            scriptDone = null;
            done.run();
        }
    }

    private void gm(String line) {
        if (body != null) {
            body.say(line);
        } else if (world() != null && player() != null) {
            EntityGrandmaster.speak(world(), player(), -1, line, 96D);
        }
    }

    private void wl(String line) {
        lineToOthers(GrandmasterScenes.WARLOCK, warlock != null ? warlock.getEntityId() : -1, line, null);
    }

    private void ml(String line) {
        lineToOthers(Speakers.MAGUS, magus != null ? magus.getEntityId() : -1, line, null);
    }

    /** A stage direction: what nobody says. */
    private void direction(String text) {
        lineToOthers(GrandmasterScenes.DIRECTION, -1, text, null);
    }

    private void hint(EntityPlayerMP player, String text) {
        TextComponentString line = new TextComponentString(text);
        line.getStyle().setColor(TextFormatting.GOLD);
        player.sendStatusMessage(line, true);
    }

    private void act(State state) {
        if (body != null) {
            body.act(state);
        }
    }

    private void act(State state, int ticks) {
        if (body != null) {
            body.act(state, ticks);
        }
    }

    private void walk(Vec3d to) {
        if (body != null) {
            body.walkTo(to);
        }
    }

    private void look(@Nullable Vec3d at) {
        if (body != null) {
            body.lookAt(at);
            if (at != null) {
                body.focus(at);
            }
        }
    }

    private Vec3d eyes(@Nullable EntityPlayerMP player) {
        return player != null ? player.getPositionEyes(1F) : middle().add(0D, 1.6D, 0D);
    }

    private void sound(SoundEvent sound, Vec3d at, float volume, float pitch) {
        WorldServer world = world();
        if (world != null) {
            world.playSound(null, at.x, at.y, at.z, sound, SoundCategory.NEUTRAL, volume, pitch);
        }
    }

    private void fx(byte type, Vec3d a, Vec3d b, int value) {
        EternityNetwork.sendAround(MsgGrandmaster.fx(type, a, b, value), data().dimension, middle().x, middle().y, middle().z, 160D);
    }

    // ------------------------------------------------------------------ 6A: the board

    private void arriving(WorldServer world, @Nullable EntityPlayerMP player) {
        if (phaseTicks == 1) {
            Vec3d c = middle().add(0D, 1.5D, 0D);
            fx(MsgGrandmaster.FX_CARDS, c, Vec3d.ZERO, 60);
            sound(SoundEvents.ENTITY_ILLAGER_MIRROR_MOVE, c, 2F, 0.8F);
            sound(EternitySounds.OMEN_SHIMMER, c, 2F, 1F);
        }
        if (phaseTicks == 26) {
            body = new EntityGrandmaster(world);
            Vec3d at = centre(Kingstable.at(4, 4));
            body.setPosition(at.x, at.y + 0.1D, at.z);
            body.rotationYaw = player != null ? (float) Math.toDegrees(Math.atan2(player.posZ - at.z, player.posX - at.x)) - 90F : 0F;
            body.setManaged(true);
            world.spawnEntity(body);
            body.act(State.ARRIVE);
            look(eyes(player));
            fx(MsgGrandmaster.FX_CARDS, at.add(0D, 1.2D, 0D), Vec3d.ZERO, 80);
            sound(SoundEvents.EVOCATION_ILLAGER_CAST_SPELL, at, 2F, 1.3F);
        }
        if (phaseTicks >= BUILD_TICKS + 40 && buildRing < 0) {
            data().phase = GrandmasterData.Phase.OPENING;
            data().markDirty();
            phaseTicks = 0;
            opening();
        }
    }

    private void opening() {
        EntityPlayerMP player = player();
        int held = player != null ? InfinityStones.countHeld(player) : 0;
        boolean champion = player != null && ChampionData.get().stoneTaken && player.getName().equals(ChampionData.get().stoneHolder);
        script.clear();
        at(10, () -> {
            act(State.PRESENT);
            look(eyes(player()));
            gm(held < 2 ? "There you are. I told them you'd come." : number(held) + "! " + (champion ? "One of them off my brother. I told them." : "I told them."));
        });
        at(50, () -> walk(centre(Kingstable.at(2, 2)).add(1.5D, 0D, 0D)));
        at(90, () -> {
            act(State.LAUGH);
            // What became of Ord, the Gardener, if the story took them to him (chapter 5).
            EntityPlayerMP p = player();
            com.matoon.eternity.story.StoryData.Player sp = p == null || !com.matoon.eternity.EternityConfig.story.enabled ? null
                    : com.matoon.eternity.story.StoryData.get().player(p.getUniqueID());
            if (sp != null && sp.seen("took_time")) {
                gm("Tryco said, \"Let them come.\" Ord said, \"Peace.\" I said, \"Game.\" And look. Ord got his peace.");
            } else if (sp != null && sp.seen("left_gardener")) {
                gm("Tryco said, \"Let them come.\" Ord said, \"Peace.\" You let him keep his. I said, \"Game.\"");
            } else {
                gm("Tryco said, \"Let them come.\" Ord said, \"Peace.\" I said, \"Game.\"");
            }
        });
        at(150, () -> {
            walk(centre(Kingstable.at(4, 3)).add(0D, 0D, 1.5D));
            look(eyes(player()));
        });
        at(185, () -> {
            act(State.POINT, 40);
            if (game.stone >= 0) {
                look(centre(game.stone).add(0D, 1D, 0D));
            }
            gm("You want the last one.");
        });
        at(240, () -> {
            act(State.SHUFFLE);
            look(eyes(player()));
            gm("It stays in play until the game ends. My rules.");
        });
        at(300, () -> gm("And my life's on the table, so you know I'm serious."));
        if (data().magus) {
            magusOpening();
            play(() -> {
                data().phase = GrandmasterData.Phase.STAKES;
                data().markDirty();
                stakesBegin();
            });
            return;
        }
        at(360, () -> {
            WorldServer world = world();
            if (world == null) {
                return;
            }
            // Placed on his way in before he is added: added anywhere else first (the world's origin, say) he can land in a
            // chunk nobody has loaded, and the world silently refuses him.
            warlock = new EntityAdamWarlock(world);
            warlock.script(true);
            warlock.arriveAt(warlockPost(), middle());
            if (!world.spawnEntity(warlock)) {
                // Wherever he would have streaked in from is not loaded either: he is simply there.
                Vec3d post = warlockPost();
                warlock.setPosition(post.x, post.y, post.z);
                warlock.direct(EntityAdamWarlock.State.HOVER, post, middle(), 0.1D);
                if (!world.spawnEntity(warlock)) {
                    warlock = null;
                }
            }
            data().warlock = GrandmasterData.Warlock.PLAYING;
            data().markDirty();
        });
        at(430, () -> {
            act(State.PRESENT);
            look(warlockPost());
            gm("And your opponent! I asked who wanted it more than you. He said yes before I'd finished asking.");
        });
        at(500, () -> {
            act(State.POINT, 30);
            gm("Win, and it's yours. For Mother.");
        });
        at(560, () -> {
            if (warlock != null) {
                warlock.direct(EntityAdamWarlock.State.HOVER, warlockPost(), eyes(player()), 0.1D);
            }
            wl("This is the last chance I'll get.");
        });
        play(() -> {
            data().phase = GrandmasterData.Phase.STAKES;
            data().markDirty();
            stakesBegin();
        });
    }

    /**
     * The opponent, on the Magus's road: somebody asked for this game. He comes down out of the
     * sky over the far edge of the board to where Warlock would have hung, and Warlock streaks in
     * after him and lands beside them, on their side of the table.
     */
    private void magusOpening() {
        at(360, () -> {
            act(State.PRESENT);
            look(warlockPost().add(0D, 8D, 0D));
            gm("And your opponent! A challenger who asked for you by name. I never say no to that.");
            WorldServer world = world();
            if (world == null) {
                return;
            }
            // High over the far edge, in a chunk the board has already loaded: added anywhere else first, he can be refused.
            Vec3d post = warlockPost();
            magus = new EntityMagus(world);
            magus.setPosition(post.x, post.y + 26D, post.z);
            magus.rest(NorsePose.HOVER);
            magus.act(NorsePose.HOVER);
            magus.hoverTo(post, 0.035D);
            magus.watch(player());
            if (!world.spawnEntity(magus)) {
                magus = null;
            }
            sound(SoundEvents.ENTITY_ILLAGER_PREPARE_MIRROR, post, 3F, 0.5F);
            sound(EternitySounds.OMEN_SHIMMER, post, 2F, 0.6F);
        });
        at(440, () -> {
            look(magus != null ? magus.getPositionEyes(1F) : warlockPost());
            act(State.THINK, 50);
            direction("He comes down slowly, as if the sky were lowering him, and stops where Warlock should have been.");
        });
        at(500, () -> {
            WorldServer world = world();
            if (world == null) {
                return;
            }
            warlock = new EntityAdamWarlock(world);
            warlock.script(true);
            warlock.arriveAt(warlockBeside(), middle());
            if (!world.spawnEntity(warlock)) {
                // Wherever he would have streaked in from is not loaded: he is simply there.
                Vec3d at = warlockBeside();
                warlock.setPosition(at.x, at.y, at.z);
                warlock.direct(EntityAdamWarlock.State.STAND, at, middle(), 0.1D);
                if (!world.spawnEntity(warlock)) {
                    warlock = null;
                }
            }
            data().warlock = GrandmasterData.Warlock.ALLIED;
            data().markDirty();
        });
        at(570, () -> {
            if (warlock != null) {
                warlock.direct(EntityAdamWarlock.State.STAND, warlockBeside(), magus != null ? magus.getPositionEyes(1F) : middle(), 0.1D);
            }
            act(State.LAUGH);
            look(warlockBeside().add(0D, 1.6D, 0D));
            gm("Oh, this is better. Two of you. One of him.");
        });
        at(640, () -> {
            if (warlock != null) {
                warlock.direct(EntityAdamWarlock.State.STAND, warlockBeside(), eyes(player()), 0.1D);
            }
            wl("Don't watch him. Watch the board.");
        });
        at(710, () -> {
            if (magus != null) {
                magus.look(middle());
                magus.act(NorsePose.REGARD, 60);
            }
            look(eyes(player()));
            ml("Begin.");
        });
    }

    private static String number(int n) {
        String[] words = {"None", "One", "Two", "Three", "Four", "Five", "Six"};
        return n >= 0 && n < words.length ? words[n] : Integer.toString(n);
    }

    // ------------------------------------------------------------------ the stakes

    private void stakesBegin() {
        phaseTicks = 0;
        script.clear();
        Vec3d p = new Vec3d(pedestal()).add(0.5D, 1.2D, 0.5D);
        at(20, () -> {
            walk(p.add(-2.5D, -1.2D, -3D));
            look(p);
        });
        at(60, () -> {
            act(State.CONDUCT, 40);
            look(p);
            gm("My life's on my side of the table. Put yours on the other.");
        });
        at(120, () -> {
            look(eyes(player()));
            EntityPlayerMP player = player();
            if (player != null && !stakeable(player)) {
                gm("Nothing? Then play for nothing. You'll still want to win.");
                data().staked = true;
                data().markDirty();
            } else {
                gm("Every stone you carry. Or the Regulator. Nothing less is worth my life.");
            }
        });
        play(() -> {
        });
    }

    private void stakes(@Nullable EntityPlayerMP player) {
        GrandmasterData d = data();
        if (d.staked && script.isEmpty()) {
            d.phase = GrandmasterData.Phase.RULES;
            d.markDirty();
            rules();
            return;
        }
        if (player != null && script.isEmpty() && phaseTicks % 400 == 200) {
            hint(player, "Hold what you will stake and lay a hand on the pedestal at the board's edge.");
        }
    }

    /** Whether the player has anything he would take as a stake. */
    private static boolean stakeable(EntityPlayerMP player) {
        for (ItemStack stack : player.inventory.mainInventory) {
            if (isStake(stack)) {
                return true;
            }
        }
        for (ItemStack stack : player.inventory.offHandInventory) {
            if (isStake(stack)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isStake(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.getItem() instanceof ItemAstralRegulator) {
            return true;
        }
        if (stack.getItem() instanceof ItemInfinityStone) {
            return Relics.recognized(stack, false);
        }
        return stack.getItem() instanceof ItemInfinityGauntlet && gauntletHolds(stack);
    }

    /** Whether a gauntlet has a stone in it that counts. */
    private static boolean gauntletHolds(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            return false;
        }
        InventoryInfinityGauntlet gauntlet = new InventoryInfinityGauntlet(stack);
        for (int i = 0; i < gauntlet.getSizeInventory(); i++) {
            ItemStack socket = gauntlet.getStackInSlot(i);
            if (!socket.isEmpty() && socket.getItem() instanceof ItemInfinityStone && Relics.recognized(socket, false)) {
                return true;
            }
        }
        return false;
    }

    /** The player lays something on the table. */
    private void stake(EntityPlayerMP player, EnumHand hand) {
        GrandmasterData d = data();
        ItemStack held = player.getHeldItem(hand);
        if (!isStake(held)) {
            if (d.phase == GrandmasterData.Phase.STAKES && body != null && body.sinceSpoke() > 60) {
                gm(d.pot.isEmpty() ? "That isn't worth my life." : "The stones, or the Regulator.");
            }
            return;
        }
        ItemStack stack = held.copy();
        player.setHeldItem(hand, ItemStack.EMPTY);
        player.inventoryContainer.detectAndSendChanges();
        d.pot.add(stack);
        d.markDirty();
        Vec3d p = new Vec3d(pedestal()).add(0.5D, 1.6D, 0.5D);
        fx(MsgGrandmaster.FX_STAKE, p, Vec3d.ZERO, 0);
        sound(SoundEvents.BLOCK_END_PORTAL_FRAME_FILL, p, 1.5F, 1.2F);
        sound(EternitySounds.OMEN_SHIMMER, p, 1F, 1.6F);
        sync();
        if (d.phase != GrandmasterData.Phase.STAKES || d.staked) {
            return;
        }
        boolean regulator = stack.getItem() instanceof ItemAstralRegulator;
        boolean everything = InfinityStones.countHeld(player) == 0 && !regulator;
        if (regulator) {
            act(State.LAUGH);
            gm("The Regulator. You'd bet the only way out there is on a board game. Good.");
            d.staked = true;
        } else if (everything) {
            act(State.CLAP);
            gm("All of them. Now it's a game.");
            d.staked = true;
        } else {
            act(State.THINK, 30);
            gm("And the rest.");
        }
        d.markDirty();
        if (d.staked) {
            script.clear();
            at(60, () -> {
            });
            play(() -> {
            });
        }
    }

    // ------------------------------------------------------------------ the rules

    /**
     * He walks them through it. Whoever he is playing watches it as a cutscene, the board showing
     * each rule as he says it (client/RulesCutscene); he keeps the same time here, going through
     * his gestures on its beats, and everyone else nearby simply hears him. Skipped, or done, the
     * game begins.
     */
    private void rules() {
        phaseTicks = 0;
        script.clear();
        EntityPlayerMP watcher = player();
        rulesWatcher = watcher != null ? watcher.getUniqueID() : null;
        rulesOver = false;
        if (watcher != null) {
            EternityNetwork.send(MsgGrandmaster.fx(MsgGrandmaster.CUTSCENE, middle(), Vec3d.ZERO, 0), watcher);
        }
        int o = GrandmasterScenes.PREROLL;
        at(0, () -> {
            walk(centre(Kingstable.at(4, 4)));
            look(eyes(player()));
        });
        // What his hands are on as he says each thing: the pieces the board is moving for him.
        Vec3d[] focus = {null, null, centre(Kingstable.THRONE), middle(), centre(Kingstable.at(4, 4)), centre(Kingstable.at(5, 2)),
                centre(Kingstable.at(6, 6)), centre(Kingstable.at(1, 2)), new Vec3d(pedestal()), centre(Kingstable.at(5, 5)), null};
        for (int i = 0; i < GrandmasterScenes.RULES.length; i++) {
            GrandmasterScenes.Beat beat = GrandmasterScenes.RULES[i];
            Vec3d at = i < focus.length ? focus[i] : null;
            at(o + beat.tick, () -> {
                act(beat.gesture, beat.hold);
                if (at != null) {
                    look(at.add(0D, 1.5D, 0D));
                } else {
                    look(eyes(player()));
                }
                lineToOthers(GrandmasterScenes.GRANDMASTER, body != null ? body.getEntityId() : -1, beat.line, rulesWatcher);
            });
        }
        // Should the client never say it is done, the game starts anyway.
        at(o + GrandmasterScenes.RULES_LENGTH + 60, () -> rulesDone(false));
        play(() -> {
        });
    }

    /** The cutscene is over for whoever was watching it: finished, or skipped. */
    private void cutsceneOver(EntityPlayerMP player) {
        GrandmasterData d = data();
        if (d.phase != GrandmasterData.Phase.RULES || rulesOver || !player.getUniqueID().equals(rulesWatcher)) {
            return;
        }
        boolean finished = scriptTicks >= GrandmasterScenes.PREROLL + GrandmasterScenes.RULES_LENGTH - 10;
        script.clear();
        scriptDone = null;
        rulesDone(!finished);
    }

    private void rulesDone(boolean skipped) {
        if (rulesOver || data().phase != GrandmasterData.Phase.RULES) {
            return;
        }
        rulesOver = true;
        script.clear();
        EntityPlayerMP player = player();
        if (!skipped && player != null && body != null && !body.isDead) {
            askAgain(player);
            return;
        }
        startPlaying(skipped);
    }

    /**
     * Told all the way through: he asks whether they have it, or want it again. He is enjoying
     * this, and would happily tell it twice; he would rather they lost fairly.
     */
    private void askAgain(EntityPlayerMP player) {
        EntityGrandmaster gm = body;
        look(eyes(player));
        act(State.PRESENT, 60);
        com.matoon.eternity.story.Talk t = new com.matoon.eternity.story.Talk(player).at(gm, 48D);
        t.line(GrandmasterScenes.GRANDMASTER, gm, "That's all of it. Do you want it again, or have you got it?")
                .ask(GrandmasterScenes.GRANDMASTER, gm,
                        com.matoon.eternity.story.Talk.say("Once more, from the start.", c -> c
                                .line(GrandmasterScenes.GRANDMASTER, gm, "From the top, then. Watch the board, not me.")
                                .run(() -> {
                                    if (data().phase == GrandmasterData.Phase.RULES) {
                                        rulesOver = false;
                                        rules();
                                    }
                                })),
                        com.matoon.eternity.story.Talk.say("I understand.", c -> c
                                .line(GrandmasterScenes.GRANDMASTER, gm, "Do you. We'll see.")
                                .run(() -> startPlaying(false))));
        t.abandoned(() -> startPlaying(false));
        com.matoon.eternity.story.StoryDirector.INSTANCE.talk(player, t);
    }

    /** The rules are told (or skipped): the book in their hands, and raiders first. */
    private void startPlaying(boolean skipped) {
        if (data().phase != GrandmasterData.Phase.RULES) {
            return;
        }
        EntityPlayerMP player = player();
        if (player != null) {
            give(player, rulebook());
        }
        look(eyes(player));
        if (skipped) {
            act(State.BOW);
            gm("Impatient. Good. It's all in the book. Raiders first.");
        }
        data().phase = GrandmasterData.Phase.PLAYING;
        data().markDirty();
        stage = Stage.THINK;
        stageTicks = skipped ? -30 : 0;
        sync();
    }

    /** A line to everyone near the board except one, who is hearing it some other way. */
    private void lineToOthers(byte speaker, int entity, String text, @Nullable java.util.UUID except) {
        WorldServer world = world();
        if (world == null) {
            return;
        }
        for (EntityPlayer p : world.playerEntities) {
            if (p instanceof EntityPlayerMP && !p.getUniqueID().equals(except) && p.getDistanceSq(middle().x, middle().y, middle().z) < 96D * 96D) {
                EternityNetwork.send(MsgGrandmaster.line(speaker, entity, text), (EntityPlayerMP) p);
            }
        }
    }

    private static ItemStack rulebook() {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("title", "The King's Table");
        tag.setString("author", "En Dwi Gast");
        tag.setInteger("generation", 0);
        NBTTagList pages = new NBTTagList();
        String[] text = {
                "THE KING'S TABLE\n\nAs it should be played.\n\nSeven squares by seven. The throne is the middle square, the refuges are the four corners.\n\nYou hold the king (tallest, crowned) and his four guards. Your opponent holds eight raiders. Raiders move first.",
                "MOVING\n\nEvery piece moves in a straight line along its row or column, as far as you like, never through another piece.\n\nOnly the king may stop on the throne or a refuge. Raiders may cross the empty throne, never stop on it.\n\nLay a hand on your piece, then on its square.",
                "TAKING\n\nClose an enemy between two of yours, along a line, and it is taken.\n\nMoving between two enemies is safe.\n\nThe refuges, the empty throne and the Mind Stone's square count as an enemy of everyone. The king is armed: he takes raiders too.",
                "THE KING\n\nHe falls like anyone else, closed between two raiders.\n\nOn the throne, or beside it, it takes raiders on all four sides of him; the throne or the stone may stand in for one.\n\nReach a refuge with him and you win.",
                "THE STONE\n\nThe stone stays in play. After every move it leaps to an empty square. The faint one is where it goes next.\n\nNothing may enter or cross its square.\n\nThe raiders win by taking the king, or if your side cannot move, or if the game runs too long.",
                "THE STAKES\n\nMy life and my stone, against what you laid on the table.\n\nThe stone stays in play until the game ends.\n\nMy rules.\n\n- G."};
        for (String page : text) {
            pages.appendTag(new NBTTagString("{\"text\":\"" + page.replace("\n", "\\n").replace("\"", "\\\"") + "\"}"));
        }
        tag.setTag("pages", pages);
        book.setTagCompound(tag);
        return book;
    }

    // ------------------------------------------------------------------ the game

    private void playing(WorldServer world, @Nullable EntityPlayerMP player) {
        stageTicks++;
        switch (stage) {
            case WAIT:
                waiting(player);
                break;
            case PLAYER_MOVE:
                if (stageTicks >= PLAYER_SLIDE) {
                    resolve();
                }
                break;
            case THINK:
                think(player);
                break;
            case APPROACH:
                approach();
                break;
            case CARRY:
                carry();
                break;
            case SETTLE:
                if (stageTicks >= 8) {
                    resolve();
                }
                break;
            case CHEAT:
                cheating();
                break;
            case SCENE:
                break;
            case PAUSE:
                break;
            case SEIZED:
                seizing();
                break;
        }
        idle(player);
    }

    /** When nothing else has him, he wanders the board's edge and watches. */
    private void idle(@Nullable EntityPlayerMP player) {
        if (body == null || body.isWalking() || body.getState() != State.IDLE || stage == Stage.CHEAT || stage == Stage.SCENE) {
            return;
        }
        if (stage == Stage.WAIT && stageTicks % 140 == 70) {
            // Around the board, never on a square anyone wants.
            BlockPos o = origin();
            int side = rand.nextInt(4);
            double along = 2D + rand.nextDouble() * (WIDTH - 4D);
            Vec3d to = side == 0 ? new Vec3d(o.getX() + along, o.getY() + 1, o.getZ() + 0.5D)
                    : side == 1 ? new Vec3d(o.getX() + WIDTH - 0.5D, o.getY() + 1, o.getZ() + along)
                    : side == 2 ? new Vec3d(o.getX() + 0.5D, o.getY() + 1, o.getZ() + along)
                    : new Vec3d(o.getX() + along, o.getY() + 1, o.getZ() + WIDTH - 0.5D);
            walk(to);
            look(eyes(player));
        } else if (stage == Stage.WAIT && stageTicks % 140 == 20) {
            act(rand.nextBoolean() ? State.THINK : State.SHUFFLE, 50);
            look(eyes(player));
        }
    }

    private void waiting(@Nullable EntityPlayerMP player) {
        if (player != null && data().playerMoves == 0 && stageTicks % 200 == 30) {
            hint(player, "Your move. Lay a hand on a blue piece, then on the square it should go to.");
        }
        if (player != null) {
            hintTicks++;
        }
    }

    /** The player lays a hand on a square. */
    public void pick(EntityPlayerMP player, int square) {
        GrandmasterData d = data();
        if (square == -2) {
            cutsceneOver(player);
            return;
        }
        if (!d.active() || game == null || square < 0 || square >= Kingstable.CELLS || !player.getUniqueID().equals(d.player)
                || player.dimension != d.dimension || player.getPositionVector().squareDistanceTo(centre(square)) > 12D * 12D) {
            return;
        }
        if (d.phase != GrandmasterData.Phase.PLAYING || stage != Stage.WAIT) {
            if (d.phase == GrandmasterData.Phase.PLAYING && stage != Stage.WAIT) {
                hint(player, seized ? "Nobody is playing by the rules now." : "Not your move.");
            }
            return;
        }
        byte there = game.cells[square];
        if (Kingstable.isDefender(there)) {
            if (selected == square) {
                selected = -1;
                targets = new int[0];
            } else {
                selected = square;
                List<Integer> to = game.destinations(square);
                targets = new int[to.size()];
                for (int i = 0; i < targets.length; i++) {
                    targets[i] = to.get(i);
                }
                sound(SoundEvents.BLOCK_STONE_HIT, centre(square), 1F, 0.8F);
                sound(SoundEvents.BLOCK_NOTE_CHIME, centre(square), 0.5F, 1.4F);
            }
            sync();
            return;
        }
        if (selected >= 0 && game.isLegal(selected, square)) {
            int from = selected;
            selected = -1;
            targets = new int[0];
            unhint();
            d.playerMoves++;
            raiderMove = from * Kingstable.CELLS + square;
            animate(from, square, ANIM_SLIDE, PLAYER_SLIDE);
            stage = Stage.PLAYER_MOVE;
            stageTicks = 0;
            sound(SoundEvents.BLOCK_PISTON_EXTEND, centre(from), 0.6F, 0.6F);
            sound(SoundEvents.BLOCK_STONE_PLACE, centre(square), 1F, 0.7F);
            return;
        }
        fx(MsgGrandmaster.FX_REFUSE, centre(square), Vec3d.ZERO, square);
        sound(SoundEvents.BLOCK_NOTE_BASS, centre(square), 0.8F, 0.5F);
        if (Kingstable.isRaider(there)) {
            hint(player, "That one is his.");
        } else if (selected >= 0) {
            hint(player, square == game.stone ? "Nothing goes through the stone." : "It can't go there.");
        }
    }

    private void animate(int from, int to, int kind, int ticks) {
        anim = new int[]{from, to, kind, ticks};
        WorldServer world = world();
        animStart = world != null ? world.getTotalWorldTime() : 0L;
        animTaken = new int[0];
        sync();
    }

    /** Whoever plays the raiders decides what to do. */
    private void think(@Nullable EntityPlayerMP player) {
        GrandmasterData d = data();
        if (stageTicks < 14) {
            return;
        }
        if (game.outcome != Kingstable.Outcome.PLAYING) {
            ended();
            return;
        }
        boolean warlockPlays = d.warlock == GrandmasterData.Warlock.PLAYING && warlock != null;
        boolean magusPlays = magusPlays();
        // The Magus sees one move further than the house does.
        int depth = warlockPlays ? EternityConfig.grandmaster.warlockDepth
                : EternityConfig.grandmaster.grandmasterDepth + (magusPlays ? 1 : 0);
        int move = game.bestMove(depth, rand);
        if (move < 0) {
            // Nothing the raiders can do. The house does not lose that way.
            conjure(-1);
            return;
        }
        Kingstable after = game.copy();
        after.play(move / Kingstable.CELLS, move % Kingstable.CELLS, null);
        if (after.outcome == Kingstable.Outcome.KING_TAKEN && (warlockPlays || magusPlays) && save(move)) {
            // Too soon. He saves the king from whoever is playing him, and they see it.
            return;
        }
        raiderMove = move;
        if (warlockPlays) {
            stage = Stage.APPROACH;
            stageTicks = 0;
            Vec3d over = centre(move / Kingstable.CELLS).add(0D, PIECE_TOP + 0.6D, 0D);
            warlock.direct(EntityAdamWarlock.State.HOVER, over.add(0D, 0.4D, 0D), centre(move % Kingstable.CELLS), 0.22D);
        } else if (magusPlays) {
            // He comes down over it and carries it himself, as Warlock would have: in violet, where Warlock's light is gold.
            stage = Stage.APPROACH;
            stageTicks = 0;
            Vec3d over = centre(move / Kingstable.CELLS).add(0D, PIECE_TOP + 0.6D, 0D);
            magus.perform(EntityMagus.Pose.DESCEND);
            magus.hoverTo(over.add(0D, 0.4D, 0D), 0.22D);
            magus.look(centre(move % Kingstable.CELLS).add(0D, 1D, 0D));
            sound(EternitySounds.OMEN_SHIMMER, magus.getPositionVector(), 1F, 0.8F);
        } else {
            stage = Stage.CARRY;
            stageTicks = 0;
            int from = move / Kingstable.CELLS, to = move % Kingstable.CELLS;
            Vec3d c = centre(from);
            look(c.add(0D, 1.5D, 0D));
            act(State.CONDUCT, CONDUCT_TICKS + 6);
            if (body != null) {
                body.walkTo(null);
            }
            animate(from, to, ANIM_CONDUCT, CONDUCT_TICKS);
            sound(SoundEvents.ENTITY_ILLAGER_CAST_SPELL, c, 1F, 1.4F);
        }
    }

    /** How high a piece stands, roughly, for whoever carries it. */
    private static final double PIECE_TOP = 3.2D;

    private void approach() {
        int from = raiderMove / Kingstable.CELLS;
        Vec3d over = centre(from).add(0D, PIECE_TOP + 0.6D, 0D);
        if (magusPlays()) {
            if (magus.getPositionVector().distanceTo(over.add(0D, 0.4D, 0D)) < 0.8D || stageTicks > 50) {
                stage = Stage.CARRY;
                stageTicks = 0;
                animate(from, raiderMove % Kingstable.CELLS, ANIM_CARRY, CARRY_TICKS);
                magus.perform(EntityMagus.Pose.CARRY);
                magusCarry(over, from, raiderMove % Kingstable.CELLS);
                sound(SoundEvents.ENTITY_ILLAGER_CAST_SPELL, over, 1F, 0.6F);
                sound(EternitySounds.OMEN_SHIMMER, over, 1F, 1.2F);
            }
            return;
        }
        if (warlock == null) {
            stage = Stage.THINK;
            return;
        }
        if (warlock.getPositionVector().distanceTo(over) < 0.8D || stageTicks > 50) {
            stage = Stage.CARRY;
            stageTicks = 0;
            animate(from, raiderMove % Kingstable.CELLS, ANIM_CARRY, CARRY_TICKS);
            warlock.direct(EntityAdamWarlock.State.CARRY, over, centre(raiderMove % Kingstable.CELLS), 1D);
            sound(SoundEvents.ENTITY_ENDERDRAGON_FLAP, over, 1F, 1.6F);
        }
    }

    /** The piece in flight, with Warlock over it holding it up, or under the Grandmaster's hand. */
    private void carry() {
        int from = raiderMove / Kingstable.CELLS, to = raiderMove % Kingstable.CELLS;
        float f = Math.min(1F, stageTicks / (float) (anim != null ? anim[3] : CARRY_TICKS));
        if (warlock != null && data().warlock == GrandmasterData.Warlock.PLAYING && anim != null && anim[2] == ANIM_CARRY) {
            Vec3d at = arc(from, to, f, ANIM_CARRY).add(0D, PIECE_TOP + 0.5D, 0D);
            warlock.direct(EntityAdamWarlock.State.CARRY, at, centre(to), 1D);
        } else if (magusPlays() && anim != null && anim[2] == ANIM_CARRY) {
            Vec3d at = arc(from, to, f, ANIM_CARRY).add(0D, PIECE_TOP + 0.5D, 0D);
            magusCarry(at, from, to);
            threads(arc(from, to, f, ANIM_CARRY).add(0D, 1.6D, 0D));
        } else if (body != null) {
            body.focus(arc(from, to, f, ANIM_CONDUCT).add(0D, 1.5D, 0D));
        }
        if (f >= 1F) {
            stage = Stage.SETTLE;
            stageTicks = 0;
            sound(SoundEvents.BLOCK_STONE_PLACE, centre(to), 1.2F, 0.6F);
            sound(SoundEvents.BLOCK_ANVIL_LAND, centre(to), 0.3F, 0.5F);
            if (warlock != null && data().warlock == GrandmasterData.Warlock.PLAYING) {
                warlock.direct(EntityAdamWarlock.State.HOVER, warlockPost(), middle(), 0.12D);
            }
            if (magusPlays()) {
                WorldServer w = world();
                if (w != null) {
                    w.spawnParticle(EnumParticleTypes.SPELL_WITCH, centre(to).x, centre(to).y + 0.3D, centre(to).z, 24, 0.7D, 0.1D, 0.7D, 0.02D);
                }
                magus.perform(EntityMagus.Pose.FLOAT);
                magus.hoverTo(warlockPost(), 0.12D);
                magus.look(middle());
            }
        }
    }

    /** Where a piece is, a fraction of the way through a move: the same curve the client draws. */
    public Vec3d arc(int from, int to, float f, int kind) {
        Vec3d a = centre(from), b = centre(to);
        double lift = kind == ANIM_SLIDE ? 0.5D : kind == ANIM_CARRY ? 2.2D : 1.6D;
        double e = f * f * (3D - 2D * f);
        Vec3d p = a.add(b.subtract(a).scale(e));
        return p.add(0D, Math.sin(Math.PI * f) * lift, 0D);
    }

    /** A move has landed: take what it takes, move the stone on, then see where that leaves everyone. */
    private void resolve() {
        GrandmasterData d = data();
        int from = raiderMove / Kingstable.CELLS, to = raiderMove % Kingstable.CELLS;
        int stoneWas = game.stone;
        boolean raiders = game.raidersToMove;
        Kingstable.Result r = game.play(from, to, rand);
        anim = null;
        for (int sq : r.taken) {
            boolean king = r.outcome == Kingstable.Outcome.KING_TAKEN && sq == r.taken.get(r.taken.size() - 1);
            fx(king ? MsgGrandmaster.FX_KING_FALLS : MsgGrandmaster.FX_TAKEN, centre(sq), Vec3d.ZERO, king ? Kingstable.KING
                    : raiders ? Kingstable.GUARD : Kingstable.RAIDER);
            sound(SoundEvents.BLOCK_GLASS_BREAK, centre(sq), 1.2F, 0.6F);
            sound(SoundEvents.ENTITY_ZOMBIE_BREAK_DOOR_WOOD, centre(sq), 0.6F, 1.4F);
        }
        if (!r.taken.isEmpty() && body != null) {
            act(raiders ? State.CLAP : State.LAUGH);
        }
        leapFx(stoneWas, false);
        d.game = game.save();
        d.markDirty();
        raiderMove = -1;
        sync();
        if (game.outcome != Kingstable.Outcome.PLAYING) {
            ended();
            return;
        }
        // The house never lets a game end before it is ready.
        if (!game.raidersToMove && game.escapes() > 0 && deny()) {
            return;
        }
        if (!game.raidersToMove && d.cheats == 0 && d.playerMoves >= 7 && rand.nextInt(3) == 0 && bored()) {
            return;
        }
        if (d.warlock == GrandmasterData.Warlock.ALLIED) {
            advise();
        }
        stage = game.raidersToMove ? Stage.THINK : Stage.WAIT;
        stageTicks = 0;
    }

    private void leapFx(int from, boolean stutter) {
        if (from >= 0 && game.stone >= 0) {
            fx(MsgGrandmaster.FX_LEAP, centre(from).add(0D, 1.1D, 0D), centre(game.stone).add(0D, 1.1D, 0D), stutter ? 1 : 0);
            sound(SoundEvents.ENTITY_ENDERMEN_TELEPORT, centre(game.stone), 0.5F, 1.9F);
        }
    }

    private void ended() {
        GrandmasterData d = data();
        switch (game.outcome) {
            case KING_ESCAPED:
            case RAIDERS_STUCK:
                // It should never come to this. A wager is a wager.
                won();
                break;
            default:
                lost();
                break;
        }
        d.markDirty();
    }

    // ------------------------------------------------------------------ cheating

    /**
     * The king has an open road to a refuge: the board shifts, and raiders stand in the way.
     * False if he can't any more (the Magus has taken the board off him), and nothing happened.
     */
    private boolean deny() {
        if (seized) {
            return false;
        }
        cheats.clear();
        // Planned on a copy first, so that every road is shut by the time he is done: no raider is moved twice,
        // and none is taken off a road it was already shutting.
        Kingstable plan = game.copy();
        boolean[] placed = new boolean[Kingstable.CELLS];
        for (int round = 0; round < 6 && plan.escapes() > 0; round++) {
            int k = plan.king();
            int to = plan.escapeSquare();
            // The square beside the refuge on the king's road.
            int dx = Integer.signum(Kingstable.x(k) - Kingstable.x(to)), dy = Integer.signum(Kingstable.y(k) - Kingstable.y(to));
            int block = Kingstable.at(Kingstable.x(to) + dx, Kingstable.y(to) + dy);
            if (block == k) {
                // He is one step from it: the king is back on his throne, or as near it as there is room.
                int home = plan.cells[Kingstable.THRONE] == Kingstable.EMPTY && !plan.isHole(Kingstable.THRONE) ? Kingstable.THRONE : farthest(plan);
                if (home < 0) {
                    break;
                }
                cheats.add(new int[]{k, home, ANIM_CONDUCT});
                plan.cells[home] = Kingstable.KING;
                plan.cells[k] = Kingstable.EMPTY;
                continue;
            }
            int raider = rand.nextInt(4) == 0 ? -1 : safeRaider(plan, block, placed);
            if (raider >= 0) {
                cheats.add(new int[]{raider, block, ANIM_CONDUCT});
                plan.cells[raider] = Kingstable.EMPTY;
            } else {
                cheats.add(new int[]{-1, block, ANIM_CONJURE});
            }
            plan.cells[block] = Kingstable.RAIDER;
            placed[block] = true;
        }
        if (cheats.isEmpty()) {
            stage = Stage.WAIT;
            stageTicks = 0;
            return true;
        }
        beginCheat(0);
        return true;
    }

    /**
     * The raider nearest a square that can be moved onto it without opening a road it was
     * shutting, and that he has not already put somewhere this turn; -1 for none.
     */
    private int safeRaider(Kingstable plan, int block, boolean[] placed) {
        int roads = plan.escapes();
        int best = -1, bestDist = Integer.MAX_VALUE;
        for (int i = 0; i < Kingstable.CELLS; i++) {
            if (plan.cells[i] != Kingstable.RAIDER || placed[i]) {
                continue;
            }
            Kingstable test = plan.copy();
            test.cells[i] = Kingstable.EMPTY;
            test.cells[block] = Kingstable.RAIDER;
            if (test.escapes() >= roads) {
                continue;
            }
            int d = Math.abs(Kingstable.x(i) - Kingstable.x(block)) + Math.abs(Kingstable.y(i) - Kingstable.y(block));
            if (d < bestDist) {
                bestDist = d;
                best = i;
            }
        }
        return best;
    }

    /** Warlock, or the Magus, is about to take the king: the king steps aside, and nobody touched him. False if he can't any more. */
    private boolean save(int killing) {
        if (seized) {
            return false;
        }
        cheats.clear();
        int k = game.king();
        int best = -1;
        for (int d = 0; d < 4; d++) {
            int x = Kingstable.x(k) + (d == 0 ? 1 : d == 1 ? -1 : 0), y = Kingstable.y(k) + (d == 2 ? 1 : d == 3 ? -1 : 0);
            if (!Kingstable.inside(x, y)) {
                continue;
            }
            int sq = Kingstable.at(x, y);
            if (game.cells[sq] != Kingstable.EMPTY || Kingstable.isRefuge(sq) || sq == game.stone) {
                continue;
            }
            Kingstable test = game.copy();
            test.cells[k] = Kingstable.EMPTY;
            test.cells[sq] = Kingstable.KING;
            if (test.killingMove() < 0) {
                best = sq;
                break;
            }
        }
        if (best >= 0) {
            cheats.add(new int[]{k, best, ANIM_CONDUCT});
        } else {
            // Nowhere to step: the raider that would have done it is somewhere else instead.
            int from = killing / Kingstable.CELLS;
            int far = game.randomFree(rand, game.stone);
            cheats.add(new int[]{from, far, ANIM_CONDUCT});
        }
        beginCheat(1);
        return true;
    }

    /** Nothing has happened for too long. False if he can't do anything about it any more. */
    private boolean bored() {
        if (seized) {
            return false;
        }
        cheats.clear();
        int k = game.king();
        int raider = -1, far = -1;
        for (int i = 0; i < Kingstable.CELLS; i++) {
            if (game.cells[i] == Kingstable.RAIDER) {
                int dist = Math.abs(Kingstable.x(i) - Kingstable.x(k)) + Math.abs(Kingstable.y(i) - Kingstable.y(k));
                if (dist > far) {
                    far = dist;
                    raider = i;
                }
            }
        }
        int beside = -1;
        for (int d = 0; d < 4 && raider >= 0; d++) {
            int x = Kingstable.x(k) + (d == 0 ? 1 : d == 1 ? -1 : 0), y = Kingstable.y(k) + (d == 2 ? 1 : d == 3 ? -1 : 0);
            if (Kingstable.inside(x, y) && game.cells[Kingstable.at(x, y)] == Kingstable.EMPTY && !Kingstable.isRestricted(Kingstable.at(x, y))
                    && Kingstable.at(x, y) != game.stone) {
                beside = Kingstable.at(x, y);
                break;
            }
        }
        if (raider < 0 || beside < 0) {
            stage = Stage.WAIT;
            return true;
        }
        cheats.add(new int[]{raider, beside, ANIM_CONDUCT});
        beginCheat(2);
        return true;
    }

    /** The raiders have nowhere to go: one more appears where it can. */
    private void conjure(int square) {
        if (seized) {
            return;
        }
        cheats.clear();
        int sq = square >= 0 ? square : game.randomFree(rand, game.stone);
        if (sq < 0) {
            lost();
            return;
        }
        cheats.add(new int[]{-1, sq, ANIM_CONJURE});
        beginCheat(3);
    }

    private int cheatKind;

    private void beginCheat(int kind) {
        GrandmasterData d = data();
        d.cheats++;
        d.markDirty();
        cheatKind = kind;
        stage = Stage.CHEAT;
        stageTicks = 0;
        // Against Warlock, or the Magus, he is offhand about it; against them alone, he enjoys it.
        boolean warlockPlays = d.warlock == GrandmasterData.Warlock.PLAYING || d.magus;
        if (body != null) {
            body.walkTo(null);
        }
        if (kind == 0) {
            String[] lines = warlockPlays ? new String[]{"Mm. No.", "The board shifts. It does that."}
                    : new String[]{"No.", "The board shifts. It does that.", "Rule twelve.", "Did you think I wouldn't?", "Again? Good."};
            gm(lines[(d.cheats - 1) % lines.length]);
            // Warlock, off the board now, has seen this before. Once.
            if (d.warlock == GrandmasterData.Warlock.WALKED && !warlockHinted && d.cheats - cheatsAtWalk >= 2) {
                warlockHinted = true;
                at(scriptTicks + 55, () -> wl("You're still playing on his board."));
            }
        } else if (kind == 1) {
            gm("Not yet. Too soon.");
        } else if (kind == 2) {
            gm("Let's make it interesting.");
        } else {
            gm("Hm.");
        }
    }

    /** One piece at a time, under his hand; a beat between each so his line lands first. */
    private void cheating() {
        if (anim == null) {
            if (cheats.isEmpty()) {
                afterCheat();
                return;
            }
            if (stageTicks < 12) {
                return;
            }
            int[] c = cheats.get(0);
            Vec3d at = c[0] >= 0 ? centre(c[0]) : centre(c[1]);
            act(State.CONDUCT, CONDUCT_TICKS + 8);
            look(at.add(0D, 1.5D, 0D));
            fx(MsgGrandmaster.FX_CHEAT, at.add(0D, 1.5D, 0D), centre(c[1]).add(0D, 1.5D, 0D), body != null ? body.getEntityId() : -1);
            // The board itself does it: light cracks across the squares under the move.
            fx(MsgGrandmaster.FX_STRIKE, centre(c[1]), new Vec3d(1D, 0D, 0D), c[1]);
            if (c[0] >= 0) {
                fx(MsgGrandmaster.FX_STRIKE, centre(c[0]), new Vec3d(1D, 0D, 0D), c[0]);
            }
            sound(SoundEvents.ENTITY_ILLAGER_CAST_SPELL, at, 1.5F, 1.2F);
            sound(SoundEvents.BLOCK_PISTON_CONTRACT, centre(c[1]), 1F, 0.5F);
            animate(c[0], c[1], c[2], c[2] == ANIM_CONJURE ? CONJURE_TICKS : CONDUCT_TICKS);
            stageTicks = 0;
            return;
        }
        if (body != null) {
            body.focus(arc(anim[0] >= 0 ? anim[0] : anim[1], anim[1], Math.min(1F, stageTicks / (float) anim[3]), anim[2]).add(0D, 1.5D, 0D));
        }
        if (stageTicks >= anim[3]) {
            int[] c = cheats.remove(0);
            byte piece = c[0] >= 0 ? game.cells[c[0]] : Kingstable.RAIDER;
            if (c[0] >= 0) {
                game.cells[c[0]] = Kingstable.EMPTY;
            }
            game.cells[c[1]] = piece;
            if (game.stone == c[1]) {
                int was = game.stone;
                game.leap(rand);
                leapFx(was, true);
            }
            anim = null;
            data().game = game.save();
            data().markDirty();
            sound(SoundEvents.BLOCK_STONE_PLACE, centre(c[1]), 1.2F, 0.6F);
            sync();
            stageTicks = 6;
        }
    }

    /** The free square farthest from every refuge. */
    private int farthest() {
        return farthest(game);
    }

    private static int farthest(Kingstable game) {
        int best = -1, bestDist = -1;
        for (int i = 0; i < Kingstable.CELLS; i++) {
            if (game.cells[i] != Kingstable.EMPTY || Kingstable.isRefuge(i) || game.isHole(i) || i == game.stone) {
                continue;
            }
            int x = Kingstable.x(i), y = Kingstable.y(i);
            int d = Math.min(Math.min(x, N - 1 - x), Math.min(y, N - 1 - y));
            if (d > bestDist) {
                bestDist = d;
                best = i;
            }
        }
        return best;
    }

    private int nearestRaider(int square) {
        int best = -1, bestDist = Integer.MAX_VALUE;
        for (int i = 0; i < Kingstable.CELLS; i++) {
            if (game.cells[i] == Kingstable.RAIDER) {
                int d = Math.abs(Kingstable.x(i) - Kingstable.x(square)) + Math.abs(Kingstable.y(i) - Kingstable.y(square));
                if (d < bestDist) {
                    bestDist = d;
                    best = i;
                }
            }
        }
        return best;
    }

    private void afterCheat() {
        GrandmasterData d = data();
        if (!game.raidersToMove && game.escapes() > 0) {
            // Moving one raider opened another road. The board shifts again.
            deny();
            return;
        }
        if (d.magus && !seized) {
            // The first time, in front of the Magus. Once is enough.
            magusNamesIt();
            return;
        }
        boolean warlockPlays = d.warlock == GrandmasterData.Warlock.PLAYING;
        if (warlockPlays && d.cheats >= 2 && !scene6b) {
            warlockWalks();
            return;
        }
        if (warlockPlays && cheatKind == 1) {
            // Once only, before he understands.
            stage = Stage.SCENE;
            script.clear();
            at(10, () -> wl("That was my move."));
            at(45, () -> {
                act(State.SHUFFLE);
                gm("And this is my board.");
            });
            play(() -> {
                stage = game.raidersToMove ? Stage.THINK : Stage.WAIT;
                stageTicks = 0;
            });
            return;
        }
        stage = game.raidersToMove ? Stage.THINK : Stage.WAIT;
        stageTicks = 0;
    }

    // ------------------------------------------------------------------ 6B: Warlock walks

    private void warlockWalks() {
        scene6b = true;
        stage = Stage.SCENE;
        script.clear();
        Vec3d edge = new Vec3d(origin().getX() + WIDTH - 1.5D, origin().getY() + 1D, origin().getZ() + 3.5D);
        at(10, () -> {
            if (warlock != null) {
                warlock.direct(EntityAdamWarlock.State.HOVER, warlock.getPositionVector().add(0D, -1D, 0D), centre(game.king()), 0.05D);
            }
            wl("He's moving the board.");
        });
        at(55, () -> {
            look(warlock != null ? warlock.getPositionEyes(1F) : middle());
            act(State.PRESENT);
            gm("It's my board.");
        });
        at(105, () -> {
            if (warlock != null) {
                warlock.direct(EntityAdamWarlock.State.HOVER, warlock.getPositionVector(), eyes(player()), 0.05D);
            }
            wl("It doesn't matter who wins. He decides who wins.");
        });
        at(170, () -> wl("That's what Mother does."));
        at(220, () -> {
            act(State.POINT, 40);
            gm("Walk off and you forfeit. No stone. Nothing to take home.");
        });
        at(285, () -> {
            wl("I know.");
            if (warlock != null) {
                warlock.direct(EntityAdamWarlock.State.STAND, edge, warlockAside(), 0.1D);
            }
        });
        at(340, () -> {
            if (warlock != null) {
                warlock.direct(EntityAdamWarlock.State.WALK, warlockAside(), warlockAside().add(3D, 0D, 0D), 0.1D);
            }
            direction("He steps off the board.");
            GrandmasterData d = data();
            d.warlock = GrandmasterData.Warlock.WALKED;
            d.warlockRefused = true;
            cheatsAtWalk = d.cheats;
            d.markDirty();
            // He has refused them too, with the game: the story remembers it.
            EntityPlayerMP p = player();
            if (p != null) {
                StoryDirector.INSTANCE.warlockRefused(p);
            }
        });
        at(420, () -> {
            if (warlock != null) {
                warlock.direct(EntityAdamWarlock.State.STAND, warlockAside(), middle(), 0.1D);
            }
            wl("She'll grow another one. Let her.");
        });
        at(490, () -> {
            look(eyes(player()));
            act(State.SHUFFLE);
            gm("Then you're playing me.");
        });
        play(() -> {
            stage = game.raidersToMove ? Stage.THINK : Stage.WAIT;
            stageTicks = 0;
            sync();
        });
    }

    // ------------------------------------------------------------------ the Magus's road

    private boolean magusPlays() {
        return data().magus && magus != null && !seized;
    }

    /** Him over the piece he carries, put there each tick and facing the way it goes. */
    private void magusCarry(Vec3d at, int from, int to) {
        Vec3d way = flat(centre(to).subtract(centre(from)));
        if (way.lengthSquared() < 1.0E-4D) {
            way = flat(middle().subtract(at));
        }
        magus.place(at, at.add(way.scale(6D)).add(0D, -1.5D, 0D));
    }

    /** His hand on a piece, from the air: a line of violet light to it, where the Grandmaster's would be gold. */
    private void threads(Vec3d piece) {
        WorldServer world = world();
        if (world == null || magus == null || stageTicks % 2 != 0) {
            return;
        }
        Vec3d hand = magus.getPositionVector().add(0D, magus.pose() == EntityMagus.Pose.CARRY ? 0.6D : 1.5D, 0D);
        Vec3d step = piece.subtract(hand);
        int n = (int) (step.length() * 2D);
        for (int i = 0; i <= n; i++) {
            Vec3d p = hand.add(step.scale(i / (double) Math.max(1, n)));
            world.spawnParticle(EnumParticleTypes.SPELL_WITCH, p.x, p.y, p.z, 1, 0.03D, 0.03D, 0.03D, 0D);
        }
        world.spawnParticle(EnumParticleTypes.END_ROD, piece.x, piece.y, piece.z, 2, 0.15D, 0.15D, 0.15D, 0.01D);
    }

    /**
     * Warlock, at their side and never touching a piece, every other move of theirs shows them one:
     * he lights the piece and the square it should go to, and says why it is good, in a sentence.
     * True if he said anything.
     */
    private boolean advise() {
        GrandmasterData d = data();
        if (warlock == null || seized || game.raidersToMove || d.playerMoves - advisedAt < 2) {
            return false;
        }
        Coaching.Advice a = Coaching.advise(game, rand, d.playerMoves == 0);
        if (a == null) {
            return false;
        }
        advisedAt = d.playerMoves;
        hinted = new int[]{a.from, a.to, a.takes.isEmpty() ? -1 : a.takes.get(0), a.takes.size() > 1 ? a.takes.get(1) : -1};
        fx(MsgGrandmaster.FX_HINT, new Vec3d(hinted[0], hinted[1], 0D), new Vec3d(hinted[2], hinted[3], 0D), HINT_TICKS);
        sound(SoundEvents.BLOCK_NOTE_CHIME, centre(a.to), 0.6F, 1.2F);
        warlock.direct(EntityAdamWarlock.State.STAND, warlockBeside(), centre(a.to).add(0D, 1D, 0D), 0.1D);
        wl(a.line);
        return true;
    }

    /** How long his lit squares stay lit, at most: they go out sooner, the moment the player moves. */
    private static final int HINT_TICKS = 20 * 90;

    /** Whatever he lit goes out. */
    private void unhint() {
        if (hinted != null) {
            fx(MsgGrandmaster.FX_HINT, new Vec3d(hinted[0], hinted[1], 0D), new Vec3d(hinted[2], hinted[3], 0D), 0);
            hinted = null;
        }
    }

    /** The first time the board shifts in front of him, the Magus says what it is, and stops playing by its rules. */
    private void magusNamesIt() {
        stage = Stage.SCENE;
        script.clear();
        at(10, () -> {
            if (magus != null) {
                magus.watch(body);
                magus.act(NorsePose.REGARD);
            }
            ml("He moved his own board.");
        });
        at(60, () -> {
            look(magus != null ? magus.getPositionEyes(1F) : warlockPost());
            act(State.SHUFFLE);
            gm("It's my board.");
        });
        at(110, () -> ml("Then the rules were never the game. Only the hand that enforces them."));
        at(200, () -> {
            WorldServer world = world();
            if (magus != null && world != null) {
                magus.act(NorsePose.BLAST, 0);
                world.spawnParticle(EnumParticleTypes.DRAGON_BREATH, magus.posX, magus.posY + 1.2D, magus.posZ, 80, 2D, 1D, 2D, 0.05D);
                sound(SoundEvents.ENTITY_ENDERDRAGON_GROWL, magus.getPositionVector(), 1.5F, 0.6F);
            }
            ml("I am no longer bound by them.");
        });
        play(this::seize);
    }

    /** From now on it is nobody's move: he moves raiders when he likes, and the Grandmaster can do nothing about it. */
    private void seize() {
        seized = true;
        stage = Stage.SEIZED;
        stageTicks = 0;
        nextSeize = 30;
        outOfTurn = 0;
        anim = null;
        raiderMove = -1;
        selected = -1;
        targets = new int[0];
        sync();
    }

    /** The board is his: every so often he moves a raider out of turn, it takes what it takes, and the squares go white under it. */
    private void seizing() {
        GrandmasterData d = data();
        if (anim == null) {
            if (stageTicks < nextSeize) {
                return;
            }
            int move = outOfTurnMove();
            if (move < 0) {
                // Nothing left for him to move: as good as three.
                gmTurns();
                return;
            }
            raiderMove = move;
            int from = move / Kingstable.CELLS, to = move % Kingstable.CELLS;
            Vec3d c = centre(from).add(0D, 1.5D, 0D);
            if (magus != null) {
                magus.look(c);
                magus.act(NorsePose.BLAST, 0);
            }
            whiten(from);
            animate(from, to, ANIM_CONDUCT, CONDUCT_TICKS);
            stageTicks = 0;
            sound(SoundEvents.ENTITY_ILLAGER_CAST_SPELL, c, 1.5F, 0.5F);
            fright(outOfTurn);
            return;
        }
        Vec3d piece = arc(anim[0], anim[1], Math.min(1F, stageTicks / (float) anim[3]), anim[2]).add(0D, 1.5D, 0D);
        if (magus != null) {
            magus.look(piece);
        }
        threads(piece);
        if (stageTicks < anim[3]) {
            return;
        }
        int from = anim[0], to = anim[1];
        int stoneWas = game.stone;
        // Played as if it were the raiders' move; it is still theirs after, as it was before.
        game.raidersToMove = true;
        Kingstable.Result r = game.play(from, to, rand);
        game.raidersToMove = false;
        game.outcome = Kingstable.Outcome.PLAYING;
        anim = null;
        raiderMove = -1;
        for (int sq : r.taken) {
            fx(MsgGrandmaster.FX_TAKEN, centre(sq), Vec3d.ZERO, Kingstable.GUARD);
            sound(SoundEvents.BLOCK_GLASS_BREAK, centre(sq), 1.2F, 0.6F);
        }
        sound(SoundEvents.BLOCK_STONE_PLACE, centre(to), 1.2F, 0.5F);
        leapFx(stoneWas, true);
        whiten(to);
        d.game = game.save();
        d.markDirty();
        sync();
        outOfTurn++;
        if (outOfTurn >= 3) {
            gmTurns();
            return;
        }
        stageTicks = 0;
        nextSeize = 60 + rand.nextInt(21);
    }

    /** A raider's move for him to make out of turn: one that takes something if there is one, never the king (that would end it). -1 for none. */
    private int outOfTurnMove() {
        Kingstable plan = game.copy();
        plan.raidersToMove = true;
        List<Integer> takes = new ArrayList<>(), others = new ArrayList<>();
        for (int move : plan.moves()) {
            Kingstable test = plan.copy();
            Kingstable.Result r = test.play(move / Kingstable.CELLS, move % Kingstable.CELLS, null);
            if (r.outcome == Kingstable.Outcome.KING_TAKEN) {
                continue;
            }
            (r.taken.isEmpty() ? others : takes).add(move);
        }
        List<Integer> pick = !takes.isEmpty() ? takes : others;
        return pick.isEmpty() ? -1 : pick.get(rand.nextInt(pick.size()));
    }

    /** Where he has laid a hand on the board, it is not the Grandmaster's any more. */
    private void whiten(int square) {
        WorldServer world = world();
        if (world == null || data().cracks[square] >= 3) {
            return;
        }
        BlockPos o = origin();
        int x0 = o.getX() + 1 + Kingstable.x(square) * SQ, z0 = o.getZ() + 1 + Kingstable.y(square) * SQ;
        IBlockState white = EternityBlocks.BOARD.getDefaultState().withProperty(BlockBoard.COLOR, BlockBoard.Face.WHITE);
        for (int dx = 0; dx < SQ; dx++) {
            for (int dz = 0; dz < SQ; dz++) {
                BlockPos pos = new BlockPos(x0 + dx, o.getY(), z0 + dz);
                if (world.getBlockState(pos).getBlock() == EternityBlocks.BOARD) {
                    set(world, pos, white);
                }
            }
        }
        Vec3d c = centre(square);
        world.spawnParticle(EnumParticleTypes.SPELL_WITCH, c.x, c.y + 0.2D, c.z, 30, 0.9D, 0.1D, 0.9D, 0.02D);
        world.spawnParticle(EnumParticleTypes.END_ROD, c.x, c.y + 0.3D, c.z, 8, 0.8D, 0.1D, 0.8D, 0.02D);
        sound(SoundEvents.BLOCK_END_PORTAL_FRAME_FILL, c, 1F, 0.5F);
    }

    /** Watching someone else move his pieces, for the first time in a very long time, he is afraid. */
    private void fright(int n) {
        if (body == null) {
            return;
        }
        Vec3d from = magus != null ? magus.getPositionEyes(1F) : warlockPost();
        act(State.AFRAID, -1);
        look(from);
        backAway(from);
        String[] lines = {"That's not- you can't- it's MY board.", "Stop. Stop that.", "Those are my pieces. Nobody has ever- NOBODY moves my-"};
        gm(lines[Math.min(n, lines.length - 1)]);
        if (n == 0) {
            body.sound(EternitySounds.GRANDMASTER_FRUSTRATED, 2F, 1.1F);
        }
    }

    /** A few steps back from something, never off his board. */
    private void backAway(Vec3d from) {
        if (body == null) {
            return;
        }
        Vec3d to = body.getPositionVector().add(flat(body.getPositionVector().subtract(from)).scale(2.5D));
        BlockPos o = origin();
        double x = MathHelper.clamp(to.x, o.getX() + 1.5D, o.getX() + WIDTH - 1.5D);
        double z = MathHelper.clamp(to.z, o.getZ() + 1.5D, o.getZ() + WIDTH - 1.5D);
        body.walkTo(new Vec3d(x, body.posY, z), 0.06D);
    }

    /**
     * Three moves out of turn, and the Grandmaster has had enough: he takes their side and
     * throws everything he has at the Magus. The board stands up around him, and he breaks it;
     * a storm of cards, and he stands in it; the whole table, square by square from the stone
     * outward, and he is still there. They and Warlock may hit him while it lasts. None of it
     * matters.
     */
    private void gmTurns() {
        stage = Stage.SCENE;
        anim = null;
        script.clear();
        walls.clear();
        Vec3d cage = middle().add(0D, 2.2D, 0D);
        int stone = game.stone;
        at(10, () -> {
            act(State.POINT, 40);
            look(eyes(player()));
            gm("Fine. FINE. You. With me.");
        });
        at(50, () -> {
            if (magus != null) {
                magus.hoverTo(cage, 0.08D);
                magus.act(NorsePose.HOVER);
                magus.watch(body);
            }
            act(State.CONDUCT, 80);
            look(cage);
            gm("Sit DOWN.");
        });
        // The board stands up around him, a layer at a time.
        for (int layer = 0; layer < 3; layer++) {
            int l = layer;
            at(80 + layer * 6, () -> raiseWalls(l));
        }
        at(130, () -> {
            if (magus != null) {
                magus.act(NorsePose.BLAST, 0);
            }
            sound(SoundEvents.ENTITY_ILLAGER_PREPARE_MIRROR, cage, 2F, 0.5F);
        });
        at(142, this::breakWalls);
        at(165, () -> {
            EntityPlayerMP p = player();
            if (magus != null && p != null) {
                magus.fight(p, warlock);
            }
            if (warlock != null && magus != null) {
                warlock.turnOn(magus);
            }
            wl("Now. While he's busy with the old man.");
        });
        // Cards: every one he has.
        at(200, () -> {
            act(State.SHUFFLE, 90);
            look(magus != null ? magus.getPositionEyes(1F) : cage);
            gm("Every card I have. Every one.");
        });
        for (int i = 0; i < 9; i++) {
            at(210 + i * 9, this::cardStorm);
        }
        at(300, () -> direction("Not one of them touches him."));
        // The table itself, square by square from the stone outward; the stone's own square is left, and the stone on it.
        int from = stone >= 0 ? stone : Kingstable.THRONE;
        at(340, () -> {
            act(State.CONDUCT, 60);
            look(centre(from).add(0D, 1.5D, 0D));
            gm("Then have the whole TABLE.");
            fx(MsgGrandmaster.FX_COLLAPSE, centre(from), Vec3d.ZERO, 50);
            sound(EternitySounds.RIFT_IMPACT, middle(), 3F, 0.9F);
        });
        for (int sq = 0; sq < Kingstable.CELLS; sq++) {
            if (sq == stone) {
                continue;
            }
            int square = sq;
            int ring = Math.max(Math.abs(Kingstable.x(sq) - Kingstable.x(from)), Math.abs(Kingstable.y(sq) - Kingstable.y(from)));
            at(350 + ring * 6, () -> throwSquare(square));
        }
        at(420, () -> {
            WorldServer world = world();
            if (magus != null && world != null) {
                world.spawnParticle(EnumParticleTypes.DRAGON_BREATH, magus.posX, magus.posY + 1.2D, magus.posZ, 120, 2.5D, 1.5D, 2.5D, 0.06D);
            }
        });
        play(this::magusEnding);
    }

    /** One layer of the squares around him, stood up on end: the board's own blocks, in its own colours. */
    private void raiseWalls(int layer) {
        WorldServer world = world();
        if (world == null) {
            return;
        }
        BlockPos o = origin();
        int cx = Kingstable.x(Kingstable.THRONE), cy = Kingstable.y(Kingstable.THRONE);
        for (int sy = cy - 1; sy <= cy + 1; sy++) {
            for (int sx = cx - 1; sx <= cx + 1; sx++) {
                if (sx == cx && sy == cy) {
                    continue;
                }
                for (int dz = 0; dz < SQ; dz++) {
                    for (int dx = 0; dx < SQ; dx++) {
                        int bx = 1 + sx * SQ + dx, bz = 1 + sy * SQ + dz;
                        BlockPos pos = o.add(bx, 1 + layer, bz);
                        // Never through anyone standing there.
                        if (!world.isAirBlock(pos) || !world.getEntitiesWithinAABB(EntityLivingBase.class, new AxisAlignedBB(pos)).isEmpty()) {
                            continue;
                        }
                        set(world, pos, face(bx, bz));
                        walls.add(pos);
                    }
                }
            }
        }
        sound(SoundEvents.BLOCK_PISTON_EXTEND, middle(), 2F, 0.5F + layer * 0.1F);
        sound(SoundEvents.BLOCK_STONE_PLACE, middle(), 2F, 0.6F);
    }

    /** He throws his hands open, and the walls are gone. */
    private void breakWalls() {
        WorldServer world = world();
        if (world == null) {
            walls.clear();
            return;
        }
        for (int i = 0; i < walls.size(); i++) {
            BlockPos pos = walls.get(i);
            IBlockState state = world.getBlockState(pos);
            if (state.getBlock() != EternityBlocks.BOARD) {
                continue;
            }
            set(world, pos, Blocks.AIR.getDefaultState());
            if (i % 3 == 0) {
                world.spawnParticle(EnumParticleTypes.BLOCK_CRACK, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 6, 0.3D, 0.3D, 0.3D, 0.15D,
                        Block.getStateId(state));
            }
        }
        walls.clear();
        Vec3d c = middle().add(0D, 2.2D, 0D);
        world.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE, c.x, c.y, c.z, 8, 3D, 1D, 3D, 0D);
        world.spawnParticle(EnumParticleTypes.DRAGON_BREATH, c.x, c.y, c.z, 120, 3D, 1D, 3D, 0.08D);
        sound(SoundEvents.ENTITY_GENERIC_EXPLODE, c, 3F, 0.55F);
        sound(SoundEvents.BLOCK_GLASS_BREAK, c, 3F, 0.5F);
        if (body != null) {
            act(State.AFRAID, -1);
            body.sound(EternitySounds.GRANDMASTER_FRUSTRATED, 2F, 0.9F);
        }
    }

    private void cardStorm() {
        if (magus == null) {
            return;
        }
        Vec3d at = magus.getPositionVector().add(rand.nextGaussian() * 0.8D, 1.2D + rand.nextDouble(), rand.nextGaussian() * 0.8D);
        fx(MsgGrandmaster.FX_CARDS, at, Vec3d.ZERO, 40);
        sound(SoundEvents.ENTITY_ILLAGER_MIRROR_MOVE, at, 1.5F, 0.9F + rand.nextFloat() * 0.4F);
    }

    /** A square of his own board, thrown: it gives way, as it would under a blow, and all of it goes at him. */
    private void throwSquare(int square) {
        WorldServer world = world();
        GrandmasterData d = data();
        if (world == null || d.cracks[square] >= 3) {
            return;
        }
        d.cracks[square] = 3;
        shatter(world, square);
        fx(MsgGrandmaster.FX_STRIKE, centre(square), new Vec3d(3D, 0D, 0D), square);
        if (square % 4 == 0) {
            sound(SoundEvents.BLOCK_GLASS_BREAK, centre(square), 1.5F, 0.5F);
        }
        d.game = game.save();
        d.markDirty();
        dirty = 2;
    }

    /**
     * The board is still. The Magus comes down beside him, takes him by the throat and lifts him,
     * and takes the stone; then lets him go, alive, and leaves. Warlock has one thing to say.
     */
    private void magusEnding() {
        GrandmasterData d = data();
        d.phase = GrandmasterData.Phase.COLLAPSE;
        d.markDirty();
        phaseTicks = 0;
        stage = Stage.SCENE;
        anim = null;
        selected = -1;
        targets = new int[0];
        cheats.clear();
        script.clear();
        stopped = true;
        restAt = null;
        if (magus != null) {
            magus.stand();
            magus.act(NorsePose.HOVER);
            magus.watch(body);
        }
        if (warlock != null) {
            warlock.script(true);
            warlock.direct(EntityAdamWarlock.State.HOVER, warlock.getPositionVector(), magus != null ? magus.getPositionEyes(1F) : middle(), 0.1D);
        }
        at(30, () -> {
            act(State.AFRAID, -1);
            look(magus != null ? magus.getPositionEyes(1F) : middle());
            gm("...That was everything.");
        });
        at(90, () -> {
            if (magus != null && body != null) {
                Vec3d feet = body.getPositionVector();
                Vec3d side = flat(magus.getPositionVector().subtract(feet));
                // Level with the Grandmaster as he will be once he is held up: his hand at the height of the throat.
                magus.hoverTo(feet.add(side.scale(1.6D)).add(0D, EntityGrandmaster.LIFT + 0.2D, 0D), 0.1D);
                magus.watch(body);
                body.walkTo(null);
            }
        });
        at(150, () -> {
            if (magus != null) {
                magus.act(NorsePose.GRIP);
            }
            act(State.LIFTED, -1);
            if (body != null) {
                body.walkTo(null);
                body.sound(EternitySounds.GRANDMASTER_FRUSTRATED, 2F, 1.3F);
            }
            direction("He takes the Grandmaster by the throat, and lifts him off his feet.");
        });
        at(210, () -> ml("You made a game out of the only thing that was ever real. Power."));
        at(310, () -> {
            if (magus != null) {
                magus.act(NorsePose.TAKE, 0);
            }
            if (game.stone >= 0) {
                Vec3d from = centre(game.stone).add(0D, 1.1D, 0D);
                fx(MsgGrandmaster.FX_LEAP, from, magus != null ? magus.getPositionVector().add(0D, 1.4D, 0D) : from.add(0D, 4D, 0D), 1);
                sound(EternitySounds.OMEN_SHIMMER, from, 3F, 0.4F);
                game.stone = -1;
                game.nextStone = -1;
                d.game = game.save();
                d.markDirty();
            }
            sync();
            magusTakesMind();
        });
        at(350, () -> direction("The Mind Stone goes into his hand as if it had always been his."));
        at(410, () -> {
            if (magus != null) {
                magus.act(NorsePose.HOVER);
                magus.watch(player());
            }
            // Set down, not dropped: alive.
            act(State.AFRAID, -1);
        });
        at(450, () -> {
            if (magus != null) {
                magus.hoverTo(magus.getPositionVector().add(0D, 40D, 0D), 0.03D);
                sound(SoundEvents.ENTITY_ILLAGER_PREPARE_MIRROR, magus.getPositionVector(), 2F, 0.5F);
            }
        });
        at(490, () -> {
            look(eyes(player()));
            gm("He didn't even want to PLAY.");
        });
        at(560, () -> {
            if (magus != null) {
                magus.setDead();
                magus = null;
            }
        });
        at(580, () -> {
            returnPot();
            sync();
        });
        at(600, () -> {
            EntityPlayerMP player = player();
            if (warlock != null && player != null) {
                Vec3d front = player.getPositionVector().add(flat(player.getLookVec()).scale(3.5D));
                warlock.direct(EntityAdamWarlock.State.HOVER, front.add(0D, 0.6D, 0D), player.getPositionEyes(1F), 0.12D);
            }
        });
        at(650, () -> wl("Two stones, now. He'll come for yours."));
        at(730, () -> {
            if (warlock != null) {
                warlock.dismiss();
            }
            d.warlock = GrandmasterData.Warlock.GONE;
            d.markDirty();
        });
        at(770, () -> {
            // He goes the way he came, in a burst of cards, and takes his board with him.
            if (body != null) {
                fx(MsgGrandmaster.FX_CARDS, body.getPositionVector().add(0D, 1.2D, 0D), Vec3d.ZERO, 80);
                sound(SoundEvents.ENTITY_ILLAGER_MIRROR_MOVE, body.getPositionVector(), 2F, 0.8F);
                body.setDead();
                body = null;
            }
            restoreRing = 0;
        });
        play(() -> {
        });
        sync();
    }

    /** The Mind Stone is the Magus's now: claimed under him, so that no copy anyone picks up will ever count. */
    private void magusTakesMind() {
        SovereignData s = SovereignData.get();
        if (s.magusHasMind) {
            return;
        }
        if (anvil.infinity.items.Items.MIND_STONE != null) {
            ItemStack stone = new ItemStack(anvil.infinity.items.Items.MIND_STONE);
            Relics.release(Relics.Kind.MIND_STONE);
            Relics.claim(stone, StoryDirector.magusId(), "The Magus");
        }
        s.magusHasMind = true;
        s.markDirty();
        if (StoryDirector.enabled()) {
            StoryDirector.INSTANCE.sendStateToAll();
        }
    }

    // ------------------------------------------------------------------ losing

    private void lost() {
        GrandmasterData d = data();
        d.phase = GrandmasterData.Phase.LOST;
        d.potLost = !d.pot.isEmpty() || d.potLost;
        d.games++;
        d.markDirty();
        stage = Stage.PAUSE;
        script.clear();
        at(10, () -> {
            act(State.LAUGH);
            look(eyes(player()));
            gm(d.pot.isEmpty() ? "The king falls." : "The king falls. The pot's mine.");
        });
        at(90, () -> {
            act(State.SHUFFLE);
            gm("Again. The stone's still in play.");
        });
        play(this::newGame);
        sync();
    }

    private void newGame() {
        GrandmasterData d = data();
        long holes = game != null ? game.holes : 0L;
        game = Kingstable.opening(rand);
        // Broken squares stay broken: whatever would have stood there is gone, and the king starts as near his throne as he can.
        for (int sq = 0; sq < Kingstable.CELLS; sq++) {
            if ((holes >>> sq & 1L) != 0L) {
                boolean king = game.cells[sq] == Kingstable.KING;
                game.breakOut(sq);
                if (king) {
                    int home = farthest();
                    if (home >= 0) {
                        game.cells[home] = Kingstable.KING;
                    }
                }
            }
        }
        if (game.stone < 0 || game.isHole(game.stone)) {
            game.stone = game.randomFree(rand, -1);
            game.nextStone = game.randomFree(rand, game.stone);
        }
        d.game = game.save();
        d.phase = GrandmasterData.Phase.PLAYING;
        d.playerMoves = 0;
        d.markDirty();
        stage = Stage.THINK;
        stageTicks = 0;
        selected = -1;
        targets = new int[0];
        anim = null;
        fx(MsgGrandmaster.FX_CARDS, middle().add(0D, 1D, 0D), Vec3d.ZERO, 40);
        sync();
    }

    // ------------------------------------------------------------------ striking the board

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getWorld().isRemote || !(event.getEntityPlayer() instanceof EntityPlayerMP)) {
            return;
        }
        if (event.getWorld().getBlockState(event.getPos()).getBlock() != EternityBlocks.BOARD) {
            return;
        }
        event.setCanceled(true);
        GrandmasterData d = data();
        if (d.active() && event.getWorld().provider.getDimension() == d.dimension && onBoard(event.getPos())) {
            strike((EntityPlayerMP) event.getEntityPlayer(), event.getPos());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onBreak(BlockEvent.BreakEvent event) {
        if (event.getState().getBlock() == EternityBlocks.BOARD) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (event.getWorld().isRemote || !(event.getEntityPlayer() instanceof EntityPlayerMP)) {
            return;
        }
        GrandmasterData d = data();
        if (!d.active() || event.getWorld().provider.getDimension() != d.dimension || !onBoard(event.getPos())) {
            return;
        }
        // Nothing is built on his board; the pedestal takes stakes.
        event.setCanceled(true);
        BlockPos p = pedestal();
        if ((event.getPos().equals(p) || event.getPos().equals(p.down())) && event.getHand() == EnumHand.MAIN_HAND) {
            stake((EntityPlayerMP) event.getEntityPlayer(), event.getHand());
        }
    }

    /** On his board, during the game, a hand is for the pieces: nothing in it is used. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        // Fires on the client too, where a remote server leaves no overworld to load data from.
        if (event.getWorld().isRemote) {
            return;
        }
        GrandmasterData d = data();
        if (d.active() && d.phase == GrandmasterData.Phase.PLAYING
                && event.getWorld().provider.getDimension() == d.dimension && event.getEntityPlayer().getUniqueID().equals(d.player)
                && onBoard(new BlockPos(event.getEntityPlayer()).down())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onPlace(BlockEvent.PlaceEvent event) {
        if (event.getWorld().isRemote) {
            return;
        }
        GrandmasterData d = data();
        if (d.active() && event.getWorld().provider.getDimension() == d.dimension && onBoard(event.getPos())) {
            event.setCanceled(true);
        }
    }

    private void strike(EntityPlayerMP player, BlockPos pos) {
        GrandmasterData d = data();
        WorldServer world = world();
        int square = squareAt(pos);
        if (world == null || square < 0 || d.phase == GrandmasterData.Phase.ARRIVING || d.phase == GrandmasterData.Phase.COLLAPSE
                || player.ticksExisted - lastStrike < 6 || d.cracks[square] >= 3) {
            return;
        }
        if (d.magus) {
            // Not today. With the Magus at the table, his board is the last thing anyone should be breaking.
            if (body != null && body.sinceSpoke() > 80) {
                gm(seized ? "Not the board. HIM." : "Not my board. Play him.");
            }
            return;
        }
        lastStrike = player.ticksExisted;
        strikes++;
        boolean onStone = square == game.stone;
        d.cracks[square]++;
        d.integrity -= onStone ? 4 : 1;
        d.markDirty();
        Vec3d c = centre(square);
        fx(MsgGrandmaster.FX_STRIKE, c, new Vec3d(d.cracks[square], 0D, 0D), square);
        sound(SoundEvents.BLOCK_GLASS_BREAK, c, 0.8F, 0.4F + d.cracks[square] * 0.15F);
        sound(SoundEvents.BLOCK_STONE_BREAK, c, 1.2F, 0.6F);
        if (d.cracks[square] >= 3) {
            shatter(world, square);
        }
        // The stone stutters: it cannot be caught, but it felt that.
        int was = game.stone;
        if (game.stone >= 0) {
            game.leap(rand);
            leapFx(was, true);
            d.game = game.save();
        }
        if (body != null) {
            body.act(State.WINCE);
            body.walkTo(null);
            // It hurts him. Not every time aloud.
            if (strikes == 1 || body.ticksExisted - lastGroan > 50 && rand.nextInt(3) != 0) {
                lastGroan = body.ticksExisted;
                body.sound(EternitySounds.GRANDMASTER_FRUSTRATED, 2F, 0.82F + rand.nextFloat() * 0.08F);
            }
        }
        if (d.integrity <= 0) {
            collapse(square);
            return;
        }
        String[] lines = {"Careful. That's my board.", null, "That isn't a move.", null, null, "Stop that.", null, null,
                "Sit down and play.", null, null, null, "Please."};
        int i = Math.min(strikes - 1, lines.length - 1);
        if (strikes - 1 < lines.length && lines[i] != null) {
            gm(lines[i]);
        }
        dirty = 2;
    }

    /** A square gives way: its blocks fall in, and whatever stood on it is gone. */
    private void shatter(WorldServer world, int square) {
        BlockPos o = origin();
        int x0 = o.getX() + 1 + Kingstable.x(square) * SQ, z0 = o.getZ() + 1 + Kingstable.y(square) * SQ;
        for (int dx = 0; dx < SQ; dx++) {
            for (int dz = 0; dz < SQ; dz++) {
                set(world, new BlockPos(x0 + dx, o.getY(), z0 + dz), Blocks.AIR.getDefaultState());
            }
        }
        byte piece = game.cells[square];
        game.breakOut(square);
        data().game = game.save();
        if (piece != Kingstable.EMPTY) {
            fx(piece == Kingstable.KING ? MsgGrandmaster.FX_KING_FALLS : MsgGrandmaster.FX_TAKEN, centre(square), Vec3d.ZERO, piece);
        }
        if (selected == square) {
            selected = -1;
            targets = new int[0];
        }
    }

    /** He felt that. */
    public void struck(EntityGrandmaster gm, EntityPlayerMP player) {
        if (gm == body && body.sinceSpoke() > 80) {
            gm("No. Play.");
        }
    }

    public void touched(EntityGrandmaster gm, EntityPlayerMP player) {
        GrandmasterData d = data();
        if (gm != body) {
            return;
        }
        if (d.phase == GrandmasterData.Phase.STAKES) {
            stake(player, EnumHand.MAIN_HAND);
        } else if (d.phase == GrandmasterData.Phase.PLAYING && stage == Stage.WAIT && body.sinceSpoke() > 80) {
            gm("Your move.");
        }
    }

    // ------------------------------------------------------------------ 6C: the last Elder

    private void won() {
        collapse(game.king() >= 0 ? game.king() : Kingstable.THRONE);
    }

    private void collapse(int from) {
        GrandmasterData d = data();
        WorldServer world = world();
        if (world == null) {
            return;
        }
        d.phase = GrandmasterData.Phase.COLLAPSE;
        d.integrity = 0;
        d.markDirty();
        phaseTicks = 0;
        stage = Stage.SCENE;
        anim = null;
        selected = -1;
        targets = new int[0];
        cheats.clear();
        script.clear();
        int stone = game.stone >= 0 ? game.stone : from;
        Vec3d rest = centre(stone);
        boolean walked = d.warlock == GrandmasterData.Warlock.WALKED;
        fx(MsgGrandmaster.FX_COLLAPSE, centre(stone), Vec3d.ZERO, 50);
        sound(EternitySounds.RIFT_IMPACT, middle(), 3F, 0.7F);
        sound(SoundEvents.BLOCK_GLASS_BREAK, middle(), 3F, 0.5F);
        if (body != null) {
            body.act(State.FALL);
            body.walkTo(null);
        }
        // The squares give way outward from the stone, and every piece goes down with its square.
        for (int sq = 0; sq < Kingstable.CELLS; sq++) {
            final int square = sq;
            int ring = Math.max(Math.abs(Kingstable.x(sq) - Kingstable.x(stone)), Math.abs(Kingstable.y(sq) - Kingstable.y(stone)));
            at(4 + ring * 6, () -> {
                WorldServer w = world();
                if (w != null && d.cracks[square] < 3) {
                    d.cracks[square] = 3;
                    shatter(w, square);
                    fx(MsgGrandmaster.FX_STRIKE, centre(square), new Vec3d(3D, 0D, 0D), square);
                }
            });
        }
        at(50, () -> {
            stopped = true;
            restAt = rest;
            sync();
            sound(SoundEvents.BLOCK_ANVIL_LAND, rest, 0.6F, 1.8F);
            if (d.warlock == GrandmasterData.Warlock.PLAYING && warlock != null) {
                // He never got to finish. He has nothing to say to them.
                warlock.dismiss();
                d.warlock = GrandmasterData.Warlock.GONE;
                d.markDirty();
            }
        });
        at(70, () -> {
            look(eyes(player()));
            gm("Not in the rules.");
        });
        at(150, () -> gm("Best game in a million years."));
        at(220, () -> {
            if (body != null) {
                body.act(State.FADE, -1);
                fx(MsgGrandmaster.FX_FADE, body.getPositionVector().add(0D, 0.5D, 0D), Vec3d.ZERO, body.getEntityId());
            }
            direction("Death is there. She doesn't look at the Grandmaster. She looks at you, holding six.");
        });
        at(260, () -> {
            returnPot();
            sync();
        });
        at(300, () -> {
            if (body != null) {
                body.setDead();
                body = null;
            }
        });
        if (walked) {
            at(330, () -> {
                EntityPlayerMP player = player();
                if (warlock != null && player != null) {
                    Vec3d front = player.getPositionVector().add(flat(player.getLookVec()).scale(3.5D));
                    warlock.direct(EntityAdamWarlock.State.HOVER, front.add(0D, 0.6D, 0D), player.getPositionEyes(1F), 0.12D);
                }
            });
            at(380, () -> wl("She'll ask you, at the end. She won't be able to make you."));
            at(440, () -> wl("Answer her as yourself."));
            at(500, () -> {
                if (warlock != null) {
                    warlock.dismiss();
                }
                d.warlock = GrandmasterData.Warlock.GONE;
                d.markDirty();
            });
        }
        at(walked ? 540 : 360, () -> restoreRing = 0);
        play(() -> {
        });
        sync();
    }

    private static Vec3d flat(Vec3d v) {
        Vec3d f = new Vec3d(v.x, 0D, v.z);
        return f.lengthSquared() < 1.0E-6 ? new Vec3d(0D, 0D, 1D) : f.normalize();
    }

    /** The stone stops: an Infinity Stone like any other, lying where its square was. */
    private void stoneFalls(Vec3d at) {
        WorldServer world = world();
        EntityPlayerMP player = player();
        if (world == null || anvil.infinity.items.Items.MIND_STONE == null || Relics.isClaimed(Relics.Kind.MIND_STONE)) {
            return;
        }
        ItemStack stone = new ItemStack(anvil.infinity.items.Items.MIND_STONE);
        if (player != null) {
            Relics.claim(stone, player);
        }
        // On whatever the ground is now the board is gone.
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(MathHelper.floor(at.x), origin().getY() + 1, MathHelper.floor(at.z));
        for (int i = 0; i < 8 && world.getBlockState(p).getMaterial().blocksMovement(); i++) {
            p.move(net.minecraft.util.EnumFacing.UP);
        }
        for (int i = 0; i < 16 && p.getY() > 1 && !world.getBlockState(p.down()).getMaterial().blocksMovement(); i++) {
            p.move(net.minecraft.util.EnumFacing.DOWN);
        }
        EntityItem item = new EntityItem(world, at.x, p.getY() + 0.2D, at.z, stone);
        item.motionX = item.motionY = item.motionZ = 0D;
        item.setNoDespawn();
        world.spawnEntity(item);
        sound(EternitySounds.OMEN_SHIMMER, at, 2F, 0.8F);
    }

    private void finish() {
        GrandmasterData d = data();
        if (d.magus) {
            // The stone went with the Magus, not into the grass; if a restart cut that short, it went with him anyway.
            magusTakesMind();
            restAt = null;
            if (!d.pot.isEmpty()) {
                returnPot();
            }
        } else if (restAt == null && game != null && game.stone >= 0) {
            // After a restart in the middle of it: the stone was where the game left it.
            restAt = centre(game.stone);
        }
        if (restAt != null) {
            stoneFalls(restAt);
            restAt = null;
        }
        stopped = false;
        d.phase = GrandmasterData.Phase.DONE;
        d.finished = true;
        d.covered = new int[0];
        d.coveredTiles = new NBTTagList();
        d.markDirty();
        if (body != null) {
            body.setDead();
        }
        body = null;
        if (warlock != null && !warlock.isDead && d.warlock != GrandmasterData.Warlock.GONE) {
            warlock.dismiss();
        }
        warlock = null;
        if (magus != null && !magus.isDead) {
            magus.setDead();
        }
        magus = null;
        EternityNetwork.sendToAll(MsgGrandmaster.clear());
        Eternity.LOG.info(d.magus ? "The Grandmaster lives, and his board is gone; the Magus has his stone."
                : "The Grandmaster is dead, and his board is gone.");
    }

    // ------------------------------------------------------------------ coming to them

    @SubscribeEvent
    public void onWake(PlayerWakeUpEvent event) {
        if (!event.getEntityPlayer().world.isRemote && event.getEntityPlayer() instanceof EntityPlayerMP
                && due((EntityPlayerMP) event.getEntityPlayer())) {
            pendingWake = true;
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote || !(event.player instanceof EntityPlayerMP)
                || event.player.ticksExisted % 40 != 0) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        if (!due(player)) {
            return;
        }
        long time = player.world.getWorldTime() % 24000L;
        boolean dawn = time < 1200L && player.world.canSeeSky(new BlockPos(player).up());
        boolean woke = pendingWake && !player.isPlayerSleeping();
        if ((dawn || woke) && player.onGround) {
            pendingWake = false;
            String why = begin(player);
            if (why != null) {
                Eternity.LOG.info("The Grandmaster did not come for {}: {}", player.getName(), why);
            }
        }
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (data != null && data().active() && event.player instanceof EntityPlayerMP) {
            EternityNetwork.send(MsgGrandmaster.sync(boardTag()), (EntityPlayerMP) event.player);
        }
    }

    @SubscribeEvent
    public void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (data != null && data().active() && event.player instanceof EntityPlayerMP) {
            EternityNetwork.send(event.toDim == data().dimension ? MsgGrandmaster.sync(boardTag()) : MsgGrandmaster.clear(),
                    (EntityPlayerMP) event.player);
        }
    }

    // ------------------------------------------------------------------ telling everyone

    private void sync() {
        GrandmasterData d = data();
        if (!d.active()) {
            return;
        }
        EternityNetwork.sendToDimension(MsgGrandmaster.sync(boardTag()), d.dimension);
    }

    /** The board as the client draws it. */
    public NBTTagCompound boardTag() {
        GrandmasterData d = data();
        NBTTagCompound tag = new NBTTagCompound();
        BlockPos o = d.origin;
        tag.setInteger("x", o.getX());
        tag.setInteger("y", o.getY());
        tag.setInteger("z", o.getZ());
        tag.setInteger("phase", d.phase.ordinal());
        boolean built = buildRing < 0 && d.phase != GrandmasterData.Phase.ARRIVING;
        tag.setBoolean("built", built);
        if (game != null) {
            int[] cells = new int[Kingstable.CELLS];
            for (int i = 0; i < cells.length; i++) {
                cells[i] = game.cells[i];
            }
            tag.setIntArray("cells", cells);
            tag.setInteger("stone", game.stone);
            tag.setInteger("next", game.nextStone);
            tag.setBoolean("raiders", game.raidersToMove);
            tag.setBoolean("stopped", stopped);
        }
        tag.setInteger("sel", selected);
        tag.setIntArray("targets", targets);
        tag.setIntArray("cracks", d.cracks);
        tag.setInteger("integrity", d.integrity);
        tag.setInteger("strength", EternityConfig.grandmaster.boardStrength);
        tag.setBoolean("yourMove", d.phase == GrandmasterData.Phase.PLAYING && stage == Stage.WAIT);
        tag.setString("player", d.player != null ? d.player.toString() : "");
        if (anim != null) {
            tag.setIntArray("anim", anim);
            tag.setLong("animStart", animStart);
        }
        tag.setInteger("gm", body != null ? body.getEntityId() : -1);
        tag.setInteger("warlock", warlock != null && d.warlock == GrandmasterData.Warlock.PLAYING ? warlock.getEntityId() : -1);
        NBTTagList pot = new NBTTagList();
        for (ItemStack stack : d.pot) {
            pot.appendTag(stack.writeToNBT(new NBTTagCompound()));
        }
        tag.setTag("pot", pot);
        tag.setBoolean("potLost", d.potLost);
        tag.setBoolean("alone", d.warlock == GrandmasterData.Warlock.WALKED);
        return tag;
    }

    // ------------------------------------------------------------------ for operators

    public String describe() {
        GrandmasterData d = data();
        if (d.finished && !d.active()) {
            return d.magus ? "The Grandmaster lives. The Magus took his stone." : "The Grandmaster is dead." + (d.warlockRefused ? " Warlock walked off his board." : "");
        }
        if (!d.active()) {
            return "The Grandmaster has not come for anyone yet.";
        }
        return String.format("The Grandmaster is playing %s (%s, %s). Board at %d %d %d in dimension %d. %d cheats, Warlock %s, "
                        + "board %d/%d, pot of %d%s%s.%n%s",
                d.playerName, d.phase.name().toLowerCase(), stage.name().toLowerCase(), d.origin.getX(), d.origin.getY(), d.origin.getZ(),
                d.dimension, d.cheats, d.warlock.name().toLowerCase(), d.integrity, EternityConfig.grandmaster.boardStrength, d.pot.size(),
                d.potLost ? " (his)" : "", d.magus ? ", the Magus plays" + (seized ? " (out of turn, " + outOfTurn + " moves)" : "") : "",
                game != null ? game.toString() : "");
    }

    /** Skips the talking: straight to the game, nothing staked. */
    public String skipToGame() {
        GrandmasterData d = data();
        if (!d.active()) {
            return "The Grandmaster is not playing anyone.";
        }
        script.clear();
        scriptDone = null;
        buildRing = buildRing >= 0 ? buildRing : -1;
        if (warlock == null && d.warlock == GrandmasterData.Warlock.ABSENT) {
            d.warlock = d.magus ? GrandmasterData.Warlock.ALLIED : GrandmasterData.Warlock.PLAYING;
        }
        d.staked = true;
        d.phase = GrandmasterData.Phase.PLAYING;
        d.markDirty();
        stage = game.raidersToMove ? Stage.THINK : Stage.WAIT;
        stageTicks = 0;
        sync();
        return "Straight to the game.";
    }

    /** Plays the move Warlock last showed them, or any legal one if he showed nothing, for testing. */
    public String forceHint() {
        if (game == null || data().phase != GrandmasterData.Phase.PLAYING || stage != Stage.WAIT) {
            return "It is not the player's move.";
        }
        int[] h = hinted;
        if (h == null) {
            int[] moves = game.moves();
            if (moves.length == 0) {
                return "No moves.";
            }
            return force(moves[0] / Kingstable.CELLS, moves[0] % Kingstable.CELLS) + " (no hint up)";
        }
        unhint();
        return force(h[0], h[1]) + " (his)";
    }

    /** Plays a move for the player, for testing: from and to as squares. */
    public String force(int from, int to) {
        if (game == null || data().phase != GrandmasterData.Phase.PLAYING || stage != Stage.WAIT) {
            return "It is not the player's move.";
        }
        if (!game.isLegal(from, to)) {
            return "That move is not legal.";
        }
        raiderMove = from * Kingstable.CELLS + to;
        data().playerMoves++;
        animate(from, to, ANIM_SLIDE, PLAYER_SLIDE);
        stage = Stage.PLAYER_MOVE;
        stageTicks = 0;
        return "Moved.";
    }

    /** Strikes a square as if a player had, for testing. */
    public String strikeFor(EntityPlayerMP player, int square, int times) {
        if (!data().active()) {
            return "The Grandmaster is not playing anyone.";
        }
        BlockPos o = origin();
        BlockPos pos = new BlockPos(o.getX() + 1 + Kingstable.x(square) * SQ + 1, o.getY(), o.getZ() + 1 + Kingstable.y(square) * SQ + 1);
        for (int i = 0; i < times && data().active() && data().phase != GrandmasterData.Phase.COLLAPSE; i++) {
            int sq = square;
            // Once a square has given way, the next blow lands on the next one along.
            for (int k = 0; k < Kingstable.CELLS && data().cracks[sq] >= 3; k++) {
                sq = (sq + 1) % Kingstable.CELLS;
            }
            pos = new BlockPos(o.getX() + 1 + Kingstable.x(sq) * SQ + 1, o.getY(), o.getZ() + 1 + Kingstable.y(sq) * SQ + 1);
            lastStrike = -100;
            strike(player, pos);
        }
        return "Struck. The board can take " + data().integrity + " more.";
    }

    /** Walks the player through the rules again, cutscene and all. */
    public String rulesNow() {
        GrandmasterData d = data();
        if (!d.active() || game == null) {
            return "The Grandmaster is not playing anyone.";
        }
        d.phase = GrandmasterData.Phase.RULES;
        d.markDirty();
        rules();
        return "He explains the rules.";
    }

    /** Forces the next cheat now, for testing. */
    public String cheatNow() {
        if (game == null || data().phase != GrandmasterData.Phase.PLAYING) {
            return "There is no game.";
        }
        if (seized) {
            return "He can't: the board is the Magus's now.";
        }
        bored();
        if (stage != Stage.CHEAT) {
            conjure(-1);
        }
        return "He cheats.";
    }
}
