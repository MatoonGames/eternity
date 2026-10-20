package com.matoon.eternity.world;

import com.matoon.eternity.EternityConfig;
import com.matoon.eternity.compat.Galacticraft;
import com.matoon.eternity.compat.Tickrate;
import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.network.MsgFinale;
import com.matoon.eternity.network.MsgRift;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.block.BlockStainedGlass;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * The end of the world. Eternity died and nobody held him together, so reality comes
 * apart: time stutters, gravity fails, the land turns to things that should not be,
 * holes open onto nothing, and the sky is open space. When it has run its course,
 * everything collapses into nothingness and the world is gone.
 */
final class Unmaking {

    static final int OBLIVION_TICKS = 1200;

    private final Random rand = new Random();
    private final Director director;
    private final Map<UUID, Long> fallSafe = new HashMap<>();
    private final Map<UUID, BlockPos> oblivionCentre = new HashMap<>();
    private final Map<UUID, Integer> oblivionEaten = new HashMap<>();
    private final Map<Integer, List<MsgRift>> fractures = new HashMap<>();
    private final Map<Integer, Long> nextFracture = new HashMap<>();
    private int fractureIds = 1_000_000;
    private long nextStutter = -1;
    private long stutterEnds = -1;

    private static final IBlockState[] CORRUPTION = {
            Blocks.OBSIDIAN.getDefaultState(),
            Blocks.END_STONE.getDefaultState(),
            Blocks.PURPUR_BLOCK.getDefaultState(),
            Blocks.MAGMA.getDefaultState(),
            Blocks.SOUL_SAND.getDefaultState(),
            Blocks.SEA_LANTERN.getDefaultState(),
            Blocks.STAINED_GLASS.getDefaultState().withProperty(BlockStainedGlass.COLOR, EnumDyeColor.BLACK),
            Blocks.STAINED_GLASS.getDefaultState().withProperty(BlockStainedGlass.COLOR, EnumDyeColor.PURPLE),
            Blocks.STAINED_GLASS.getDefaultState().withProperty(BlockStainedGlass.COLOR, EnumDyeColor.MAGENTA),
    };

    Unmaking(Director director) {
        this.director = director;
    }

    static long duration() {
        return EternityConfig.apocalypse.unmakingMinutes * 1200L;
    }

    void tick(MinecraftServer server, EternityData d) {
        long elapsed = d.clock - d.doomAt;
        long duration = duration();
        List<EntityPlayerMP> players = server.getPlayerList().getPlayers();
        protectFalls(players, d.clock);

        if (elapsed < duration) {
            float p = elapsed / (float) duration;
            if (d.clock % 20 == 0) {
                EternityNetwork.sendToAll(new MsgFinale(MsgFinale.UNMAKING, 0, 0, 0, 0, p));
                for (EntityPlayerMP player : players) {
                    if (!player.isSpectator()) {
                        corrupt(player, p, d.clock);
                    }
                }
            }
            breakTheSky(players, p, d.clock, false);
            stutterTime(p, d.clock);
            bendTheSky(p);
            return;
        }

        long oblivion = elapsed - duration;
        if (oblivion < OBLIVION_TICKS) {
            float p = oblivion / (float) OBLIVION_TICKS;
            if (oblivion == 0) {
                Tickrate.restore();
            }
            if (d.clock % 10 == 0) {
                EternityNetwork.sendToAll(new MsgFinale(MsgFinale.OBLIVION, 0, 0, 0, 0, p));
            }
            breakTheSky(players, p, d.clock, true);
            if (EternityConfig.apocalypse.oblivionErasesTerrain) {
                erase(players, p);
            }
            return;
        }

        // Nothing is left.
        d.phase = EternityData.Phase.VOID;
        d.markDirty();
        Tickrate.restore();
        for (EntityPlayerMP player : players) {
            entombInVoid(player);
        }
    }

    // ------------------------------------------------------------------ the sky

    /**
     * The sky itself breaks, a little more each time, in every dimension anyone is in.
     * Every player in that dimension sees the same wound in the same place.
     */
    private void breakTheSky(List<EntityPlayerMP> players, float p, long clock, boolean oblivion) {
        if (clock % 20 != 0) {
            return;
        }
        for (EntityPlayerMP player : players) {
            int dim = player.dimension;
            long next = nextFracture.getOrDefault(dim, clock);
            if (clock < next) {
                continue;
            }
            long interval = oblivion ? 100 + rand.nextInt(80) : (long) ((900 + rand.nextInt(900)) * (1.2F - p));
            nextFracture.put(dim, clock + interval);

            double azimuth = rand.nextDouble() * Math.PI * 2D;
            double elevation = Math.toRadians(12D + rand.nextDouble() * 55D);
            double x = Math.cos(azimuth) * Math.cos(elevation), y = Math.sin(elevation), z = Math.sin(azimuth) * Math.cos(elevation);
            float span = oblivion ? 120F + rand.nextFloat() * 50F : 60F + rand.nextFloat() * 50F + 50F * p;
            MsgRift msg = MsgRift.open(++fractureIds, x, y, z, span, rand.nextLong(), MsgRift.FLAG_SKY_FRACTURE);
            List<MsgRift> list = fractures.computeIfAbsent(dim, k -> new ArrayList<>());
            list.add(msg);
            if (list.size() > 14) {
                list.remove(0);
            }
            EternityNetwork.sendToDimension(msg, dim);
        }
    }

    /** Someone arrived: show them the wounds already in this sky. */
    void sendFractures(EntityPlayerMP player) {
        for (MsgRift msg : fractures.getOrDefault(player.dimension, new ArrayList<>())) {
            EternityNetwork.send(msg, player);
        }
    }

    /** Called for every player while the world is gone, including anyone who logs in. */
    static void entombInVoid(EntityPlayerMP player) {
        if (player.interactionManager.getGameType() != GameType.SPECTATOR) {
            player.setGameType(GameType.SPECTATOR);
        }
        EternityNetwork.send(new MsgFinale(MsgFinale.VOID, 0, 0, 0, 0, 1F), player);
    }

    // ------------------------------------------------------------------ the unmaking

    private void corrupt(EntityPlayerMP player, float p, long clock) {
        WorldServer world = player.getServerWorld();
        boolean terrain = EternityConfig.apocalypse.corruptTerrain;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        if (terrain) {
            // The land around you turns into things it was never meant to be.
            int count = (int) (4 + p * 60);
            for (int i = 0; i < count; i++) {
                pos.setPos(player.posX + (rand.nextDouble() - 0.5D) * 56D, player.posY + (rand.nextDouble() - 0.5D) * 24D,
                        player.posZ + (rand.nextDouble() - 0.5D) * 56D);
                if (pos.getY() < 1 || pos.getY() > 254 || !world.isBlockLoaded(pos) || player.getDistanceSq(pos) < 9D) {
                    continue;
                }
                IBlockState state = world.getBlockState(pos);
                if (!state.getMaterial().blocksMovement() || state.getBlockHardness(world, pos) < 0F) {
                    continue;
                }
                world.setBlockState(pos, rand.nextFloat() < 0.25F ? Blocks.AIR.getDefaultState() : CORRUPTION[rand.nextInt(CORRUPTION.length)], 2);
            }
            // Pieces of the world drift up into the sky.
            if (p > 0.2F) {
                int floaters = (int) (p * 6);
                for (int i = 0; i < floaters; i++) {
                    pos.setPos(player.posX + (rand.nextDouble() - 0.5D) * 40D, player.posY - 1 - rand.nextInt(4),
                            player.posZ + (rand.nextDouble() - 0.5D) * 40D);
                    IBlockState state = world.getBlockState(pos);
                    if (world.isBlockLoaded(pos) && state.getMaterial().blocksMovement() && state.getBlockHardness(world, pos) >= 0F
                            && !state.getBlock().hasTileEntity(state) && player.getDistanceSq(pos) > 16D) {
                        EntityFallingBlock block = new EntityFallingBlock(world, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, state);
                        block.shouldDropItem = false;
                        block.fallTime = 1;
                        block.setNoGravity(true);
                        block.motionY = 0.05D + rand.nextDouble() * 0.15D;
                        world.setBlockToAir(pos);
                        world.spawnEntity(block);
                    }
                }
            }
            // Holes open onto nothing.
            if (p > 0.35F && rand.nextFloat() < 0.1F + p * 0.25F) {
                double angle = rand.nextDouble() * Math.PI * 2D;
                double dist = 14D + rand.nextDouble() * 30D;
                BlockPos centre = new BlockPos(player.posX + Math.cos(angle) * dist, player.posY - 2 + rand.nextInt(6),
                        player.posZ + Math.sin(angle) * dist);
                int r = 2 + (int) (p * 7) + rand.nextInt(3);
                for (int dx = -r; dx <= r; dx++) {
                    for (int dy = -r; dy <= r; dy++) {
                        for (int dz = -r; dz <= r; dz++) {
                            if (dx * dx + dy * dy + dz * dz <= r * r) {
                                Terrain.erase(world, centre.add(dx, dy, dz));
                            }
                        }
                    }
                }
            }
        }

        // Gravity forgets you.
        if (p > 0.15F && rand.nextFloat() < 0.06F + p * 0.18F) {
            player.addPotionEffect(new PotionEffect(MobEffects.LEVITATION, 30 + rand.nextInt(60), rand.nextInt(2), false, false));
            fallSafe.put(player.getUniqueID(), clock + 400);
        }
        if (rand.nextFloat() < p * 0.35F && !Galacticraft.isSpace(world)) {
            double angle = rand.nextDouble() * Math.PI * 2D;
            double radius = 16D + rand.nextDouble() * 80D;
            BlockPos column = new BlockPos(player.posX + Math.cos(angle) * radius, 0, player.posZ + Math.sin(angle) * radius);
            if (world.isBlockLoaded(column)) {
                world.addWeatherEffect(new EntityLightningBolt(world, column.getX(), world.getHeight(column).getY(), column.getZ(), true));
            }
        }
    }

    /** Time lurches: it drags, races, then snaps back. */
    private void stutterTime(float p, long clock) {
        if (stutterEnds >= 0 && clock >= stutterEnds) {
            stutterEnds = -1;
            Tickrate.restore();
        }
        if (nextStutter < 0) {
            nextStutter = clock + 200 + rand.nextInt(600);
        }
        if (clock >= nextStutter && stutterEnds < 0) {
            float slowest = Math.max(3F, 20F - 15F * p);
            float fastest = 20F + 30F * p;
            Tickrate.set(rand.nextBoolean() ? slowest + rand.nextFloat() * (20F - slowest) : 20F + rand.nextFloat() * (fastest - 20F));
            stutterEnds = clock + 20 + rand.nextInt(80);
            nextStutter = clock + (long) ((200 + rand.nextInt(800)) * (1.2F - p));
        }
    }

    /** Day and night stop making sense. */
    private void bendTheSky(float p) {
        if (rand.nextFloat() < 0.002F + p * 0.01F) {
            for (WorldServer world : DimensionManager.getWorlds()) {
                world.setWorldTime(world.getWorldTime() + 2000 + rand.nextInt(10000));
            }
        }
        if (rand.nextFloat() < p * 0.002F) {
            WorldServer overworld = DimensionManager.getWorld(0);
            overworld.getWorldInfo().setRaining(!overworld.getWorldInfo().isRaining());
            overworld.getWorldInfo().setThundering(overworld.getWorldInfo().isRaining());
        }
    }

    private void protectFalls(List<EntityPlayerMP> players, long clock) {
        if (fallSafe.isEmpty()) {
            return;
        }
        for (EntityPlayerMP player : players) {
            Long until = fallSafe.get(player.getUniqueID());
            if (until != null) {
                if (clock > until) {
                    fallSafe.remove(player.getUniqueID());
                } else {
                    player.fallDistance = 0F;
                }
            }
        }
    }

    // ------------------------------------------------------------------ oblivion

    /** The world dissolves outward from every player into nothing. */
    private void erase(List<EntityPlayerMP> players, float p) {
        int[] sphere = Terrain.sphere();
        double radius = Math.min(Terrain.MAX_RADIUS, 3D + (Terrain.MAX_RADIUS - 3D) * p);
        double r2 = radius * radius;
        int budget = Math.max(200, 1500 / Math.max(1, players.size()));
        for (EntityPlayerMP player : players) {
            WorldServer world = player.getServerWorld();
            UUID id = player.getUniqueID();
            BlockPos centre = oblivionCentre.computeIfAbsent(id, k -> new BlockPos(player));
            int index = oblivionEaten.getOrDefault(id, 0);
            int left = budget;
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            while (index < sphere.length && left > 0) {
                int packed = sphere[index];
                int x = Terrain.unpackX(packed), y = Terrain.unpackY(packed), z = Terrain.unpackZ(packed);
                if (x * x + y * y + z * z > r2) {
                    break;
                }
                index++;
                pos.setPos(centre.getX() + x, centre.getY() + y, centre.getZ() + z);
                if (pos.getY() >= 0 && pos.getY() < 256 && Terrain.eraseAbsolutely(world, pos)) {
                    left--;
                }
            }
            oblivionEaten.put(id, index);
        }
    }
}
