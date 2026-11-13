package com.matoon.eternity.world;

import com.matoon.eternity.EternityConfig;
import com.matoon.eternity.item.ItemCasket;
import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.network.MsgTribunalFx;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockSnow;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeBeach;
import net.minecraft.world.biome.BiomeForest;
import net.minecraft.world.biome.BiomeHills;
import net.minecraft.world.biome.BiomeOcean;
import net.minecraft.world.biome.BiomeRiver;
import net.minecraft.world.biome.BiomeTaiga;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * The Fimbulwinter: the Casket of Ancient Winters, held open until it is full and then let go,
 * pouring out everything in it at once. A wall of winter races out from it across the land,
 * {@link #SPEED} blocks a tick, out to {@code casketWinterRadius}. Whatever it passes over is
 * frozen solid. The ground is buried in snow, deepest near the casket; lakes and seas ice over,
 * lava crusts to obsidian, fires go out, and glaciers heave up out of the land in spires. Where
 * {@code casketIceAge} allows it, the land stays in an ice age: its biomes become their frozen
 * kin, and snow falls there from then on. Over it all, the sky closes into a blizzard.
 */
public final class Fimbulwinter {

    public static final Fimbulwinter INSTANCE = new Fimbulwinter();

    /** How fast the wall of winter runs out, in blocks a tick. */
    public static final double SPEED = 2.5D;
    /** How long the blizzard it brings lasts, in ticks. */
    public static final int BLIZZARD_TICKS = 20 * 75;

    private final List<Wave> waves = new ArrayList<>();
    private final Random random = new Random();

    private Fimbulwinter() {
    }

    /** Lets the winter in the casket out, all at once, round whoever held it. */
    public void unleash(EntityPlayer by) {
        World world = by.world;
        if (!(world instanceof WorldServer)) {
            return;
        }
        double radius = EternityConfig.vault.casketWinterRadius;
        boolean terrain = ItemCasket.changesTerrain(world);
        boolean iceAge = terrain && EternityConfig.vault.casketIceAge && world.provider.isSurfaceWorld();
        Vec3d at = new Vec3d(by.posX, by.posY, by.posZ);
        int ticks = (int) Math.ceil(radius / SPEED);
        EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.CASKET_WINTER, at.x, at.y, at.z, radius, SPEED, iceAge ? 1D : 0D, 2, ticks),
                world.provider.getDimension());
        world.playSound(null, at.x, at.y, at.z, SoundEvents.ENTITY_LIGHTNING_THUNDER, SoundCategory.PLAYERS, 8F, 0.5F);
        world.playSound(null, at.x, at.y, at.z, SoundEvents.ENTITY_ENDERDRAGON_GROWL, SoundCategory.PLAYERS, 6F, 0.45F);
        world.playSound(null, at.x, at.y, at.z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 6F, 0.4F);
        world.playSound(null, at.x, at.y, at.z, SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 6F, 0.3F);
        if (world.provider.isSurfaceWorld() && !world.provider.isNether()) {
            // The sky closes over it.
            world.getWorldInfo().setCleanWeatherTime(0);
            world.getWorldInfo().setRainTime(BLIZZARD_TICKS + 20 * 60 * 3);
            world.getWorldInfo().setRaining(true);
        }
        com.matoon.eternity.story.StoryDirector.INSTANCE.frostHedge(by, at.add(0D, 1D, 0D), Vec3d.ZERO, 0D, 0D, radius);
        waves.add(new Wave((WorldServer) world, at, radius, by, terrain, iceAge));
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || waves.isEmpty()) {
            return;
        }
        for (Iterator<Wave> it = waves.iterator(); it.hasNext(); ) {
            Wave wave = it.next();
            if (wave.world.getMinecraftServer() == null || wave.step()) {
                it.remove();
            }
        }
    }

    /** Forgets every winter still running out, when the server stops. */
    public void reset() {
        waves.clear();
    }

    private final class Wave {
        final WorldServer world;
        final Vec3d at;
        final double radius;
        final EntityPlayer by;
        final boolean terrain, iceAge;
        final Set<Integer> struck = new HashSet<>();
        double reached;

        Wave(WorldServer world, Vec3d at, double radius, EntityPlayer by, boolean terrain, boolean iceAge) {
            this.world = world;
            this.at = at;
            this.radius = radius;
            this.by = by;
            this.terrain = terrain;
            this.iceAge = iceAge;
        }

        /** Runs the wall out one tick further. True once it has run its course. */
        boolean step() {
            double from = reached;
            reached = Math.min(radius, reached + SPEED);
            strike(from, reached);
            if (terrain) {
                int r = MathHelper.ceil(reached);
                int cx = MathHelper.floor(at.x), cz = MathHelper.floor(at.z);
                double in2 = from * from, out2 = reached * reached;
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        double d2 = dx * dx + dz * dz;
                        if (d2 >= in2 && d2 < out2 || from == 0D && d2 == 0D) {
                            column(cx + dx, cz + dz, Math.sqrt(d2) / radius);
                        }
                    }
                }
            }
            return reached >= radius;
        }

        /** Everything the wall passes over this tick is frozen solid, and badly hurt. */
        private void strike(double from, double to) {
            AxisAlignedBB box = new AxisAlignedBB(at.x - to - 2D, at.y - 48D, at.z - to - 2D, at.x + to + 2D, at.y + 64D, at.z + to + 2D);
            for (EntityLivingBase victim : world.getEntitiesWithinAABB(EntityLivingBase.class, box)) {
                if (victim == by || struck.contains(victim.getEntityId())) {
                    continue;
                }
                double dx = victim.posX - at.x, dz = victim.posZ - at.z;
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d >= from - 1.5D && d < to + 1.5D) {
                    struck.add(victim.getEntityId());
                    Vec3d away = d < 1.0E-3 ? Vec3d.ZERO : new Vec3d(dx / d, 0.35D, dz / d);
                    float damage = 24F + victim.getMaxHealth() * 0.2F;
                    ItemCasket.freeze(victim, by, damage, away.scale(0.9D * (1D - d / radius) + 0.3D), 200);
                    victim.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 400, 2, false, false));
                }
            }
        }

        /** One column of the land, {@code share} of the way out: frozen, buried, and maybe a glacier raised from it. */
        private void column(int x, int z, double share) {
            BlockPos probe = new BlockPos(x, 64, z);
            if (!world.isBlockLoaded(probe)) {
                return;
            }
            BlockPos top = world.getPrecipitationHeight(probe);
            if (top.getY() <= 1) {
                return;
            }
            BlockPos ground = top.down();
            IBlockState g = world.getBlockState(ground);
            Material m = g.getMaterial();
            if (m == Material.WATER) {
                world.setBlockState(ground, Blocks.ICE.getDefaultState(), 2);
                if (share < 0.5D && random.nextInt(3) == 0) {
                    // Near the casket, deeper.
                    BlockPos under = ground.down();
                    if (world.getBlockState(under).getMaterial() == Material.WATER) {
                        world.setBlockState(under, Blocks.PACKED_ICE.getDefaultState(), 2);
                    }
                }
                if (random.nextInt(260) == 0) {
                    spire(top, 2 + random.nextInt(4), 1);
                }
            } else if (m == Material.LAVA) {
                world.setBlockState(ground, g.getBlock() == Blocks.LAVA && g.getValue(BlockLiquid.LEVEL) == 0
                        ? Blocks.OBSIDIAN.getDefaultState() : Blocks.COBBLESTONE.getDefaultState(), 2);
            } else if (m != Material.AIR) {
                IBlockState over = world.getBlockState(top);
                if (over.getMaterial() == Material.FIRE) {
                    world.setBlockToAir(top);
                    over = Blocks.AIR.getDefaultState();
                }
                boolean open = over.getBlock() == Blocks.AIR || over.getBlock() == Blocks.SNOW_LAYER
                        || over.getMaterial().isReplaceable() && !over.getMaterial().isLiquid();
                if (open && (g.isSideSolid(world, ground, net.minecraft.util.EnumFacing.UP) || m == Material.LEAVES)) {
                    // Deepest near the casket.
                    int depth = 1 + (int) (4D * (1D - share) * (1D - share)) + random.nextInt(2);
                    if (over.getBlock() == Blocks.SNOW_LAYER) {
                        depth += over.getValue(BlockSnow.LAYERS);
                    }
                    world.setBlockState(top, Blocks.SNOW_LAYER.getDefaultState().withProperty(BlockSnow.LAYERS, Math.min(8, depth)), 2);
                    if (m != Material.LEAVES && share > 0.08D && random.nextInt(share < 0.4D ? 110 : 170) == 0) {
                        spire(top, 5 + random.nextInt(9) + (int) (8D * (1D - share)), 1 + random.nextInt(2));
                    }
                }
            }
            if (iceAge) {
                freezeBiome(world, x, z);
            }
        }

        /** A glacier heaving up out of the ground: a tapering spire of packed ice, capped with clear. */
        private void spire(BlockPos foot, int height, int girth) {
            for (int y = 0; y < height; y++) {
                double r = girth * (1D - y / (double) height) + 0.35D;
                int ri = MathHelper.ceil(r);
                for (int dx = -ri; dx <= ri; dx++) {
                    for (int dz = -ri; dz <= ri; dz++) {
                        if (dx * dx + dz * dz > r * r + 0.2D) {
                            continue;
                        }
                        BlockPos p = foot.add(dx, y, dz);
                        IBlockState s = world.getBlockState(p);
                        if (s.getBlock() != Blocks.AIR && !s.getMaterial().isReplaceable()) {
                            continue;
                        }
                        world.setBlockState(p, (y >= height - 2 ? Blocks.ICE : Blocks.PACKED_ICE).getDefaultState(), 2);
                    }
                }
            }
        }
    }

    /** Turns the biome of one column to its frozen kin. Done the same on both sides, so they agree. */
    public static void freezeBiome(World world, int x, int z) {
        Chunk chunk = world.getChunk(x >> 4, z >> 4);
        byte[] biomes = chunk.getBiomeArray();
        int i = (z & 15) << 4 | (x & 15);
        Biome now = Biome.getBiome(biomes[i] & 255, Biomes.PLAINS);
        Biome frozen = frozen(now);
        if (frozen != now) {
            biomes[i] = (byte) Biome.getIdForBiome(frozen);
            chunk.markDirty();
        }
    }

    /** What a biome becomes in an ice age. */
    public static Biome frozen(Biome biome) {
        if (biome == null || biome.isSnowyBiome()) {
            return biome;
        }
        if (biome instanceof BiomeOcean) {
            return Biomes.FROZEN_OCEAN;
        }
        if (biome instanceof BiomeRiver) {
            return Biomes.FROZEN_RIVER;
        }
        if (biome instanceof BiomeBeach) {
            return Biomes.COLD_BEACH;
        }
        if (biome instanceof BiomeTaiga || biome instanceof BiomeForest) {
            return Biomes.COLD_TAIGA;
        }
        if (biome instanceof BiomeHills) {
            return Biomes.ICE_MOUNTAINS;
        }
        return Biomes.ICE_PLAINS;
    }
}
