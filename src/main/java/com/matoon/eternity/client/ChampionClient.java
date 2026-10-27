package com.matoon.eternity.client;

import com.matoon.eternity.EternityConfig;
import com.matoon.eternity.client.render.ChampionFx;
import com.matoon.eternity.client.render.FxRenderer;
import com.matoon.eternity.client.render.GlSnapshot;
import com.matoon.eternity.client.sound.EntityLoopSound;
import com.matoon.eternity.client.sound.SoundFx;
import com.matoon.eternity.entity.EntityChampion;
import com.matoon.eternity.network.MsgTribunalFx;
import com.matoon.eternity.world.Tamarata;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemElytra;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.FOVUpdateEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * The Champion from where you stand: his effects; the dust his feet kick up and the thud of
 * every step; the sparks the stone throws off when he is angry and the hum of it; what it is
 * like to be hit by him (the view jolting, a white flash, the world rushing past); and once he
 * has broken his planet, drifting through what is left of it, with no up and no down.
 */
@SideOnly(Side.CLIENT)
public final class ChampionClient {

    public static final ChampionClient INSTANCE = new ChampionClient();

    private final Minecraft mc = Minecraft.getMinecraft();
    private final List<FxRenderer.Fx> effects = new ArrayList<>();
    private final List<Spark> sparks = new ArrayList<>();
    private final Map<Integer, Sounds> sounds = new HashMap<>();
    private final Random rand = new Random();

    private int tick;
    private float shake;
    private float flash;
    /** The flash of Planet Breaker, which whites the whole world out and fades slowly. */
    private float whiteout;
    private float rush;
    private float fovKick;
    /** Whether Tamarata is broken, as the server last told us. */
    private boolean broken;

    /** A spark of the stone, or a fleck of dust, drifting and fading. */
    private static final class Spark {
        double x, y, z, vx, vy, vz;
        int age;
        final int life;
        final float size;
        final boolean dust;

        Spark(double x, double y, double z, double vx, double vy, double vz, int life, float size, boolean dust) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.vx = vx;
            this.vy = vy;
            this.vz = vz;
            this.life = life;
            this.size = size;
            this.dust = dust;
        }
    }

    /** The hum of the stone, riding along with him, and where in his stride he last planted a foot. */
    private static final class Sounds {
        EntityLoopSound hum;
        int seen;
        int lastStep;
    }

    private ChampionClient() {
    }

    public boolean broken() {
        return broken;
    }

    // ------------------------------------------------------------------ what the server tells us

    public void effect(MsgTribunalFx m) {
        if (mc.world == null || mc.player == null) {
            return;
        }
        Vec3d at = new Vec3d(m.x, m.y, m.z);
        Vec3d to = new Vec3d(m.a, m.b, m.c);
        double dist = mc.player.getPositionVector().distanceTo(at);
        long seed = rand.nextLong();
        switch (m.type) {
            case MsgTribunalFx.CHAMPION_IMPACT: {
                boolean me = m.id == mc.player.getEntityId();
                float weight = m.time == 0 ? 0.5F : m.time == 1 ? 1F : 1.4F;
                if (me) {
                    // Hit yourself, you feel it rather than see it.
                    shake = Math.max(shake, 4.5F * weight);
                    flash = Math.max(flash, 0.4F * weight);
                    if (m.time > 0) {
                        rush = 1F;
                        fovKick = Math.max(fovKick, 0.2F * weight);
                    }
                    burst(at.add(to.scale(1.5D)), 8, 0.5D, to.scale(0.8D));
                    break;
                }
                effects.add(new ChampionFx.Impact(at, to, m.time, seed));
                shake = Math.max(shake, 1.8F * weight * closeness(dist, 26D));
                flash = Math.max(flash, 0.15F * weight * closeness(dist, 12D));
                break;
            }
            case MsgTribunalFx.CHAMPION_GUARD:
                effects.add(new ChampionFx.Guard(at, to, m.time, seed));
                if (m.time == 2) {
                    for (Entity entity : mc.world.loadedEntityList) {
                        if (entity instanceof EntityChampion && entity.getDistanceSq(m.x, m.y, m.z) < 16D) {
                            ((EntityChampion) entity).counterTick = ((EntityChampion) entity).clientStateTicks;
                        }
                    }
                }
                if (m.id == mc.player.getEntityId()) {
                    shake = Math.max(shake, m.time == 0 ? 1F : 0.4F);
                }
                break;
            case MsgTribunalFx.CHAMPION_QUAKE:
                effects.add(new ChampionFx.Quake(at, m.a, m.id, m.time));
                shake = Math.max(shake, (float) Math.min(5D, m.a * 0.4D) * closeness(dist, m.a * 4D));
                dust(at, m.a * 0.8D, (int) (m.a * 8D));
                break;
            case MsgTribunalFx.CHAMPION_CRACK:
                effects.add(new ChampionFx.Crack(at, m.a, m.id, m.time));
                break;
            case MsgTribunalFx.CHAMPION_ROAR:
                effects.add(new ChampionFx.Roar(at, m.a, m.id, m.time));
                shake = Math.max(shake, 2.5F * closeness(dist, m.a * 2D));
                if (dist < m.a) {
                    rush = Math.max(rush, 0.7F * closeness(dist, m.a));
                    fovKick = Math.max(fovKick, 0.1F);
                }
                break;
            case MsgTribunalFx.CHAMPION_LEAP:
                effects.add(new ChampionFx.Leap(at, to, m.id, m.time));
                shake = Math.max(shake, 1.2F * closeness(dist, 30D));
                dust(at, 3D, 30);
                break;
            case MsgTribunalFx.CHAMPION_ARCS:
                effects.add(new ChampionFx.Arcs(at, m.id, (float) m.a, m.time));
                break;
            case MsgTribunalFx.CHAMPION_BREAK:
                effects.add(new ChampionFx.Break(at, m.a, seed, m.time));
                shake = Math.max(shake, 9F * closeness(dist, 400D));
                whiteout = EternityConfig.client.reduceFlashing ? 0.25F : 1F;
                broken = true;
                dust(at, 14D, 160);
                break;
            case MsgTribunalFx.CHAMPION_MARK:
                effects.add(new ChampionFx.Mark(at, m.a, m.time));
                break;
            case MsgTribunalFx.CHAMPION_STONE:
                effects.add(new ChampionFx.StoneFlight(at, m.id, m.time));
                break;
            case MsgTribunalFx.CHAMPION_EXHAUST:
                effects.add(new ChampionFx.Exhaust(at, m.id, m.a > 0.5D, m.time));
                if (m.a > 0.5D) {
                    shake = Math.max(shake, 1.5F * closeness(dist, 40D));
                }
                break;
            case MsgTribunalFx.CHAMPION_RUSH:
                dust(at, 2D, 20);
                break;
            case MsgTribunalFx.CHAMPION_WORLD:
                broken = m.id == 1;
                break;
            default:
                break;
        }
    }

    // ------------------------------------------------------------------ ticking

    /**
     * A shattered chunk arrives as a whole new chunk, and the client's old one is thrown away with
     * every entity filed in it. Anything that does not move on to another chunk, like him hanging
     * in the wreck, would never be filed again and never drawn. File them back in.
     */
    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        World world = event.getWorld();
        if (!world.isRemote || world.provider.getDimension() != Tamarata.dimension()) {
            return;
        }
        Chunk chunk = event.getChunk();
        for (Entity entity : world.loadedEntityList) {
            if (entity.addedToChunk && entity.chunkCoordX == chunk.x && entity.chunkCoordZ == chunk.z) {
                chunk.addEntity(entity);
            }
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || mc.world == null || mc.player == null || mc.isGamePaused()) {
            return;
        }
        tick++;
        FxRenderer.tickAll(effects);
        shake *= 0.88F;
        flash *= 0.86F;
        whiteout *= 0.965F;
        rush *= 0.9F;
        fovKick *= 0.85F;
        for (Entity entity : mc.world.loadedEntityList) {
            if (entity instanceof EntityChampion) {
                follow((EntityChampion) entity);
            }
        }
        for (FxRenderer.Fx fx : effects) {
            if (fx instanceof ChampionFx.Exhaust) {
                exhaust((ChampionFx.Exhaust) fx);
            }
        }
        Iterator<Map.Entry<Integer, Sounds>> it = sounds.entrySet().iterator();
        while (it.hasNext()) {
            Sounds s = it.next().getValue();
            if (tick - s.seen > 5) {
                s.hum.fadeOut();
                it.remove();
            }
        }
        Iterator<Spark> si = sparks.iterator();
        while (si.hasNext()) {
            Spark s = si.next();
            s.x += s.vx;
            s.y += s.vy;
            s.z += s.vz;
            s.vx *= s.dust ? 0.94D : 0.9D;
            s.vy = s.dust ? s.vy * 0.94D - 0.004D : s.vy * 0.9D + 0.006D;
            s.vz *= s.dust ? 0.94D : 0.9D;
            if (++s.age >= s.life) {
                si.remove();
            }
        }
        if (sparks.size() > 1500) {
            sparks.subList(0, sparks.size() - 1500).clear();
        }
        if (inWreck()) {
            drift(mc.player);
        }
    }

    /** Everything that rides along with him: the hum of the stone, sparks, footsteps, dust. */
    private void follow(EntityChampion c) {
        Sounds s = sounds.get(c.getEntityId());
        if (s == null) {
            s = new Sounds();
            s.hum = new EntityLoopSound(SoundEvents.BLOCK_PORTAL_AMBIENT, SoundCategory.HOSTILE, c, 0F, 0.1F).pitch(0.45F);
            SoundFx.play(s.hum);
            sounds.put(c.getEntityId(), s);
        }
        s.seen = tick;
        s.hum.setTarget(c.wearsStone() ? MathHelper.clamp(c.glow, 0F, 1.6F) * 0.8F : 0F);

        // A foot comes down every half stride: a thud, and a puff of whatever he is standing on.
        int step = MathHelper.floor(c.walkPhase / (float) Math.PI);
        if (step != s.lastStep && c.walkAmount > 0.25F && c.onGround) {
            s.lastStep = step;
            float weight = Math.min(1F, c.walkAmount);
            SoundFx.at(SoundEvents.ENTITY_IRONGOLEM_STEP, SoundCategory.HOSTILE, c.posX, c.posY, c.posZ, 1.4F * weight, 0.55F + rand.nextFloat() * 0.1F);
            float yaw = (float) Math.toRadians(c.rotationYaw);
            double side = step % 2 == 0 ? 0.5D : -0.5D;
            double fx = c.posX + MathHelper.cos(yaw) * side, fz = c.posZ + MathHelper.sin(yaw) * side;
            dust(new Vec3d(fx, c.posY, fz), 0.7D, 5);
            if (c.getDistance(mc.player) < 10D) {
                shake = Math.max(shake, 0.35F * weight);
            }
        } else if (c.walkAmount <= 0.25F) {
            s.lastStep = step;
        }

        // Sparks of the stone stream off him, more the angrier he is.
        float rage = c.getRage();
        double rate = c.wearsStone() ? Math.max(0D, rage - 0.25F) * 2.2D + Math.max(0F, c.glow - 1F) * 4D : 0D;
        int count = (int) rate;
        if (rand.nextFloat() < rate - count) {
            count++;
        }
        Vec3d brow = c.brow();
        for (int i = 0; i < count; i++) {
            boolean body = rand.nextInt(3) > 0 && rage > 0.6F;
            double px = body ? c.posX + (rand.nextDouble() - 0.5D) * 1.6D : brow.x;
            double py = body ? c.posY + rand.nextDouble() * 3.2D : brow.y;
            double pz = body ? c.posZ + (rand.nextDouble() - 0.5D) * 1.6D : brow.z;
            sparks.add(new Spark(px, py, pz, rand.nextGaussian() * 0.05D, rand.nextGaussian() * 0.04D + 0.02D, rand.nextGaussian() * 0.05D,
                    10 + rand.nextInt(16), 0.04F + rand.nextFloat() * 0.05F, false));
        }

        // Charging, his feet throw up the ground behind him.
        double speed = Math.sqrt(c.velX * c.velX + c.velZ * c.velZ);
        if (c.getState() == EntityChampion.State.RUSH && speed > 0.4D) {
            BlockPos under = new BlockPos(c.posX, c.posY - 0.5D, c.posZ);
            IBlockState state = mc.world.getBlockState(under);
            if (state.getMaterial() != Material.AIR) {
                for (int i = 0; i < 5; i++) {
                    mc.world.spawnParticle(EnumParticleTypes.BLOCK_CRACK, c.posX + rand.nextGaussian() * 0.5D, c.posY + 0.1D,
                            c.posZ + rand.nextGaussian() * 0.5D, -c.velX * 0.4D + rand.nextGaussian() * 0.15D, 0.25D + rand.nextDouble() * 0.3D,
                            -c.velZ * 0.4D + rand.nextGaussian() * 0.15D, Block.getStateId(state));
                }
            }
            double d = c.getDistance(mc.player);
            if (d < 8D) {
                rush = Math.max(rush, (float) (1D - d / 8D) * 0.6F);
                shake = Math.max(shake, 1F * (float) (1D - d / 8D));
            }
        }
        // Near him at the height of his rage, the air itself seems to tremble.
        if (rage > 0.9F && c.getDistance(mc.player) < 20D) {
            shake = Math.max(shake, 0.25F);
        }
    }

    /** Smoke and fire off a rocket's engine. */
    private void exhaust(ChampionFx.Exhaust fx) {
        Vec3d at = fx.nozzle(0F);
        int count = fx.lifting() ? 6 : 1;
        for (int i = 0; i < count; i++) {
            mc.world.spawnParticle(fx.lifting() ? EnumParticleTypes.FLAME : EnumParticleTypes.SMOKE_NORMAL, at.x + rand.nextGaussian() * 0.2D,
                    at.y - 0.3D, at.z + rand.nextGaussian() * 0.2D, rand.nextGaussian() * 0.05D, -0.3D - rand.nextDouble() * 0.3D,
                    rand.nextGaussian() * 0.05D);
            if (fx.lifting()) {
                mc.world.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL, at.x + rand.nextGaussian() * 0.4D, at.y - 1.5D,
                        at.z + rand.nextGaussian() * 0.4D, rand.nextGaussian() * 0.08D, -0.2D, rand.nextGaussian() * 0.08D);
            }
        }
    }

    private void burst(Vec3d at, int count, double force, Vec3d drift) {
        for (int i = 0; i < count; i++) {
            Vec3d d = new Vec3d(rand.nextGaussian(), rand.nextGaussian(), rand.nextGaussian()).normalize().scale(force * (0.3D + rand.nextDouble()));
            sparks.add(new Spark(at.x, at.y, at.z, d.x + drift.x, d.y + drift.y, d.z + drift.z, 10 + rand.nextInt(12), 0.05F + rand.nextFloat() * 0.05F, false));
        }
    }

    /** Ground thrown up in a ring around a point: whatever it is made of, clouds of dust, and drifting flecks. */
    private void dust(Vec3d at, double reach, int count) {
        for (int i = 0; i < count; i++) {
            double a = rand.nextDouble() * Math.PI * 2D;
            double r = rand.nextDouble() * reach;
            double x = at.x + Math.cos(a) * r, z = at.z + Math.sin(a) * r;
            BlockPos ground = new BlockPos(x, at.y - 0.5D, z);
            IBlockState state = mc.world.getBlockState(ground);
            if (state.getMaterial() == Material.AIR) {
                state = mc.world.getBlockState(ground.down());
            }
            double out = 0.15D + rand.nextDouble() * 0.35D;
            if (state.getMaterial() != Material.AIR) {
                mc.world.spawnParticle(EnumParticleTypes.BLOCK_CRACK, x, at.y + 0.2D, z, Math.cos(a) * out, 0.3D + rand.nextDouble() * 0.5D,
                        Math.sin(a) * out, Block.getStateId(state));
            }
            if (i % 3 == 0) {
                mc.world.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL, x, at.y + 0.3D, z, Math.cos(a) * out * 0.5D, 0.05D, Math.sin(a) * out * 0.5D);
            }
            if (i % 4 == 0) {
                sparks.add(new Spark(x, at.y + 0.3D, z, Math.cos(a) * out * 0.6D, 0.1D + rand.nextDouble() * 0.2D, Math.sin(a) * out * 0.6D,
                        30 + rand.nextInt(30), 0.06F + rand.nextFloat() * 0.08F, true));
            }
        }
    }

    // ------------------------------------------------------------------ the wreck

    private boolean inWreck() {
        return broken && mc.player.dimension == Tamarata.dimension();
    }

    /**
     * No up and no down in the wreck: you hang where you are and drift the way you push. Walk
     * to push along where you look; jump to rise, sneak to sink. Galacticraft's own freefall
     * does the rest; this only makes sure you can always get somewhere.
     */
    private void drift(EntityPlayerSP player) {
        if (player.onGround || player.capabilities.isFlying || player.isSpectator()) {
            return;
        }
        float forward = player.movementInput.moveForward, strafe = player.movementInput.moveStrafe;
        float yaw = (float) Math.toRadians(player.rotationYaw), pitch = (float) Math.toRadians(player.rotationPitch);
        Vec3d look = new Vec3d(-MathHelper.sin(yaw) * MathHelper.cos(pitch), -MathHelper.sin(pitch), MathHelper.cos(yaw) * MathHelper.cos(pitch));
        Vec3d side = new Vec3d(MathHelper.cos(yaw), 0D, MathHelper.sin(yaw));
        double push = 0.018D;
        Vec3d thrust = look.scale(forward * push).add(side.scale(strafe * push));
        double up = player.movementInput.jump ? push : player.movementInput.sneak ? -push : 0D;
        player.motionX = (player.motionX + thrust.x) * 0.985D;
        player.motionY = (player.motionY + thrust.y + up) * 0.985D;
        player.motionZ = (player.motionZ + thrust.z) * 0.985D;
        double speed = Math.sqrt(player.motionX * player.motionX + player.motionY * player.motionY + player.motionZ * player.motionZ);
        if (speed > 0.7D) {
            player.motionX *= 0.7D / speed;
            player.motionY *= 0.7D / speed;
            player.motionZ *= 0.7D / speed;
        }
        player.fallDistance = 0F;
    }

    // ------------------------------------------------------------------ the planet whole

    /** Ticks until the next jump, as vanilla waits between jumps when jump is held. */
    private int jumpWait;
    private int flyToggle;
    private boolean jumpWas;

    /**
     * Galacticraft moves the player in any zero-g dimension with its own copy of the vanilla
     * update, which has no jumping in it: no jump, no swimming up, no double-tap to fly, no
     * opening an elytra. While Tamarata is whole, those are done here instead, as vanilla does them.
     */
    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        if (event.getEntityLiving() != mc.player || broken || mc.player.dimension != Tamarata.dimension()) {
            return;
        }
        EntityPlayerSP player = mc.player;
        boolean jump = player.movementInput.jump;
        boolean pressed = jump && !jumpWas;
        jumpWas = jump;
        if (jumpWait > 0) {
            jumpWait--;
        }
        if (flyToggle > 0) {
            flyToggle--;
        }
        if (player.isSpectator() || player.isRiding() || player.isPlayerSleeping()) {
            return;
        }
        if (pressed && player.capabilities.allowFlying) {
            if (flyToggle == 0) {
                flyToggle = 7;
            } else {
                player.capabilities.isFlying = !player.capabilities.isFlying;
                player.sendPlayerAbilities();
                flyToggle = 0;
            }
        }
        if (player.capabilities.isFlying) {
            return;
        }
        if (jump) {
            if (player.isInWater() || player.isInLava()) {
                player.motionY += 0.04D;
            } else if (player.onGround && jumpWait == 0) {
                player.jump();
                jumpWait = 10;
            }
        } else {
            jumpWait = 0;
        }
        if (pressed && !player.onGround && player.motionY < 0D && !player.isElytraFlying() && !player.isOnLadder()) {
            ItemStack chest = player.getItemStackFromSlot(EntityEquipmentSlot.CHEST);
            if (chest.getItem() == Items.ELYTRA && ItemElytra.isUsable(chest)) {
                player.connection.sendPacket(new CPacketEntityAction(player, CPacketEntityAction.Action.START_FALL_FLYING));
            }
        }
    }

    // ------------------------------------------------------------------ drawing

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Entity view = mc.getRenderViewEntity();
        if (mc.world == null || view == null || effects.isEmpty() && sparks.isEmpty()) {
            return;
        }
        float pt = event.getPartialTicks();
        Vec3d camera = new Vec3d(view.lastTickPosX + (view.posX - view.lastTickPosX) * pt,
                view.lastTickPosY + (view.posY - view.lastTickPosY) * pt,
                view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * pt);
        // Drawn after the world, before the hand and the HUD: whatever is switched on or off must
        // be put back exactly as it was, or they are drawn wrong (fogged black, at night).
        GlSnapshot before = GlSnapshot.take();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
        FxRenderer.renderAll(effects, camera, pt);
        renderSparks(camera, pt);
        before.restore();
    }

    private void renderSparks(Vec3d camera, float pt) {
        if (sparks.isEmpty()) {
            return;
        }
        float rx = ActiveRenderInfo.getRotationX(), rz = ActiveRenderInfo.getRotationZ();
        float ryz = ActiveRenderInfo.getRotationYZ(), rxy = ActiveRenderInfo.getRotationXY(), rxz = ActiveRenderInfo.getRotationXZ();
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableFog();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.depthMask(false);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        for (int pass = 0; pass < 2; pass++) {
            boolean dust = pass == 0;
            if (dust) {
                GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            } else {
                GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            }
            buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
            for (Spark e : sparks) {
                if (e.dust != dust) {
                    continue;
                }
                float life = (e.age + pt) / e.life;
                if (life >= 1F) {
                    continue;
                }
                float r, g, b, a;
                if (dust) {
                    r = 0.72F;
                    g = 0.46F;
                    b = 0.32F;
                    a = 0.55F * (1F - life);
                } else {
                    // White, then the stone's magenta, then a dying violet.
                    r = life < 0.3F ? 1F - life * 0.2F : 0.94F - (life - 0.3F) * 0.25F;
                    g = life < 0.2F ? 0.85F - life * 3F : Math.max(0.12F, 0.25F - life * 0.15F);
                    b = 1F;
                    a = (1F - life) * (life < 0.1F ? life / 0.1F : 1F);
                }
                float s = e.size * (dust ? 1F + life : 1F - life * 0.5F);
                double x = e.x + e.vx * pt - camera.x, y = e.y + e.vy * pt - camera.y, z = e.z + e.vz * pt - camera.z;
                a *= (float) MathHelper.clamp((Math.sqrt(x * x + y * y + z * z) - 1D) / 3D, 0D, 1D);
                if (a < 0.01F) {
                    continue;
                }
                double[][] corners = {
                        {x - rx * s - ryz * s, y - rxz * s, z - rz * s - rxy * s},
                        {x - rx * s + ryz * s, y + rxz * s, z - rz * s + rxy * s},
                        {x + rx * s + ryz * s, y + rxz * s, z + rz * s + rxy * s},
                        {x + rx * s - ryz * s, y - rxz * s, z + rz * s - rxy * s}};
                for (int i = 0; i < 4; i++) {
                    double[] c0 = corners[i], c1 = corners[(i + 1) % 4];
                    buffer.pos(x, y, z).color(r, g, b, a).endVertex();
                    buffer.pos(c0[0], c0[1], c0[2]).color(r, g, b, 0F).endVertex();
                    buffer.pos(c1[0], c1[1], c1[2]).color(r, g, b, 0F).endVertex();
                }
            }
            tessellator.draw();
        }
        GlStateManager.popMatrix();
    }

    /** A white flash when he hits you, the world going white when the planet breaks, and the rush of being sent flying. */
    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Pre event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL || mc.world == null) {
            return;
        }
        float shownFlash = EternityConfig.client.reduceFlashing ? Math.min(flash, 0.15F) : flash;
        float white = Math.max(shownFlash, whiteout);
        if (white < 0.01F && rush < 0.02F) {
            return;
        }
        ScaledResolution screen = event.getResolution();
        int w = screen.getScaledWidth(), h = screen.getScaledHeight();
        GlSnapshot before = GlSnapshot.take();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        if (white > 0.01F) {
            // The planet breaking is white first, then the stone's purple as it fades.
            float purple = whiteout > shownFlash ? 1F - whiteout : 0F;
            float r = 1F, g = 1F - purple * 0.7F, b = 1F;
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            buffer.pos(0, h, 0).color(r, g, b, Math.min(1F, white)).endVertex();
            buffer.pos(w, h, 0).color(r, g, b, Math.min(1F, white)).endVertex();
            buffer.pos(w, 0, 0).color(r, g, b, Math.min(1F, white)).endVertex();
            buffer.pos(0, 0, 0).color(r, g, b, Math.min(1F, white)).endVertex();
            tessellator.draw();
        }
        if (rush > 0.02F) {
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            Random lines = new Random(tick * 7919L);
            double cx = w / 2D, cy = h / 2D, reach = Math.sqrt(cx * cx + cy * cy);
            buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
            for (int i = 0; i < 46; i++) {
                double a = lines.nextDouble() * Math.PI * 2D;
                double r0 = reach * (0.45D + lines.nextDouble() * 0.3D), r1 = reach * 1.05D;
                double spread = 0.004D + lines.nextDouble() * 0.008D;
                float alpha = rush * (0.25F + lines.nextFloat() * 0.35F);
                buffer.pos(cx + Math.cos(a) * r0, cy + Math.sin(a) * r0, 0).color(1F, 0.95F, 1F, 0F).endVertex();
                buffer.pos(cx + Math.cos(a + spread) * r1, cy + Math.sin(a + spread) * r1, 0).color(1F, 0.9F, 1F, alpha).endVertex();
                buffer.pos(cx + Math.cos(a - spread) * r1, cy + Math.sin(a - spread) * r1, 0).color(1F, 0.9F, 1F, alpha).endVertex();
            }
            tessellator.draw();
        }
        before.restore();
    }

    @SubscribeEvent
    public void onCamera(EntityViewRenderEvent.CameraSetup event) {
        if (!EternityConfig.client.screenShake || shake < 0.001F) {
            return;
        }
        float t = tick + (float) event.getRenderPartialTicks();
        event.setYaw(event.getYaw() + (MathHelper.sin(t * 3.7F) + MathHelper.sin(t * 8.3F) * 0.5F) * shake * 0.5F);
        event.setPitch(event.getPitch() + (MathHelper.sin(t * 3.1F) + MathHelper.sin(t * 6.9F) * 0.5F) * shake * 0.5F);
        event.setRoll(event.getRoll() + MathHelper.sin(t * 2.3F) * shake * 0.8F);
    }

    @SubscribeEvent
    public void onFov(FOVUpdateEvent event) {
        if (fovKick > 0.005F) {
            event.setNewfov(event.getNewfov() * (1F + fovKick));
        }
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        mc.addScheduledTask(() -> {
            effects.clear();
            sparks.clear();
            sounds.clear();
            shake = flash = whiteout = rush = fovKick = 0F;
            broken = false;
        });
    }

    private static float closeness(double dist, double range) {
        return (float) MathHelper.clamp(1D - dist / range, 0D, 1D);
    }
}
