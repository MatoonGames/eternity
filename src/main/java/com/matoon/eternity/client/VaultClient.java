package com.matoon.eternity.client;

import com.matoon.eternity.EternityConfig;
import com.matoon.eternity.client.render.FxRenderer;
import com.matoon.eternity.client.render.GlSnapshot;
import com.matoon.eternity.client.render.VaultFx;
import com.matoon.eternity.entity.ArmorPowers;
import com.matoon.eternity.client.render.VaultTreasures;
import com.matoon.eternity.entity.EntityDestroyer;
import com.matoon.eternity.entity.EntityDestroyerArmor;
import com.matoon.eternity.item.EternityItems;
import com.matoon.eternity.network.MsgTribunalFx;
import com.matoon.eternity.world.OdinsVault;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Odin's Vault from where you stand. The dark of it: a cold haze that swallows the far end of the
 * hall, shafts of pale light falling from the lattice in the roof and warmer ones onto each
 * treasure, dust turning slowly in them, the far wall of light glowing through the gloom. The
 * treasures on their stands. And everything that happens there: the Destroyer's fire and the
 * weight of it through the floor, the Einherjar's spears on their shields, the Casket's winter.
 */
@SideOnly(Side.CLIENT)
public final class VaultClient {

    public static final VaultClient INSTANCE = new VaultClient();

    private static final int MOTES = 140;

    private final Minecraft mc = Minecraft.getMinecraft();
    private final List<FxRenderer.Fx> effects = new ArrayList<>();
    private final List<double[]> motes = new ArrayList<>();
    private final Random rand = new Random();

    private int tick;
    private float shake;
    private boolean casket = true;
    private VaultFx.Ray lastRay;
    private int nextGroan = 400;

    private VaultClient() {
    }

    public boolean inVault() {
        return mc.world != null && mc.world.provider.getDimension() == OdinsVault.dimension();
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
        boolean me = m.id == mc.player.getEntityId();
        switch (m.type) {
            case MsgTribunalFx.VAULT_STATE:
                casket = m.id == 1;
                break;
            case MsgTribunalFx.DESTROYER_WAKE:
                shake = Math.max(shake, 0.8F * closeness(dist, 60D));
                dust(at.add(0D, 0.2D, 0D), 2D, 30);
                break;
            case MsgTribunalFx.DESTROYER_STEP: {
                float weight = (float) m.a;
                shake = Math.max(shake, (0.4F + 2.6F * weight) * closeness(dist, 28D + 30D * weight));
                dust(at, 0.8D + 2.5D * weight, (int) (8 + 60 * weight));
                if (weight >= 1F) {
                    for (int i = 0; i < 40; i++) {
                        mc.world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, at.x + rand.nextGaussian() * 1.2D, at.y + rand.nextDouble() * 2D,
                                at.z + rand.nextGaussian() * 1.2D, 0D, 0.05D, 0D);
                    }
                }
                break;
            }
            case MsgTribunalFx.DESTROYER_IMPACT: {
                float weight = m.time == 2 ? 1.4F : 1F;
                if (me) {
                    shake = Math.max(shake, 5F * weight);
                    sparks(at.add(to.scale(1.2D)), 10, 0.5D);
                    break;
                }
                effects.add(new VaultFx.Impact(at, to, weight, seed));
                shake = Math.max(shake, 2F * weight * closeness(dist, 30D));
                sparks(at, 16, 0.6D);
                break;
            }
            case MsgTribunalFx.DESTROYER_QUAKE:
                effects.add(new VaultFx.Quake(at, m.a, m.id, m.time));
                shake = Math.max(shake, (float) Math.min(5D, m.a * 0.45D) * closeness(dist, m.a * 4D));
                dust(at, m.a * 0.7D, (int) (m.a * 9D));
                break;
            case MsgTribunalFx.DESTROYER_CHARGE:
                effects.add(new VaultFx.Charge(m.id, seed, m.time));
                break;
            case MsgTribunalFx.DESTROYER_RAY: {
                if (lastRay != null) {
                    lastRay.replaced = true;
                }
                // The ray's tick, and in its top byte how far its dynamo is overcharged, which swells it.
                int phase = m.id & 0xFFFFFF, charge = m.id >>> 24;
                lastRay = new VaultFx.Ray(at, to, phase, m.time);
                lastRay.swell = 1F + 3F * (1F - (float) Math.exp(-charge / 40F));
                effects.add(lastRay);
                if (phase % 2 == 0) {
                    effects.add(new VaultFx.Molten(to, 0.9D + rand.nextDouble() * 0.4D, 70));
                }
                for (int i = 0; i < 3; i++) {
                    mc.world.spawnParticle(EnumParticleTypes.LAVA, to.x, to.y + 0.1D, to.z, 0D, 0D, 0D);
                    mc.world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, to.x + rand.nextGaussian() * 0.3D, to.y + 0.2D,
                            to.z + rand.nextGaussian() * 0.3D, 0D, 0.08D, 0D);
                }
                Vec3d near = closest(at, to, mc.player.getPositionEyes(1F));
                shake = Math.max(shake, 0.9F * closeness(near.distanceTo(mc.player.getPositionEyes(1F)), 8D));
                break;
            }
            case MsgTribunalFx.DESTROYER_FALL:
                dust(at, 3D, 80);
                break;
            case MsgTribunalFx.EINHERJAR_BLOCK:
                effects.add(new VaultFx.Clang(at, to, seed));
                for (int i = 0; i < 8; i++) {
                    mc.world.spawnParticle(EnumParticleTypes.CRIT, at.x, at.y, at.z, to.x * 0.3D + rand.nextGaussian() * 0.2D,
                            rand.nextDouble() * 0.3D, to.z * 0.3D + rand.nextGaussian() * 0.2D);
                }
                break;
            case MsgTribunalFx.EINHERJAR_HIT:
                if (me) {
                    shake = Math.max(shake, 1.6F);
                }
                for (int i = 0; i < 10; i++) {
                    mc.world.spawnParticle(EnumParticleTypes.CRIT, at.x, at.y, at.z, to.x * 0.4D + rand.nextGaussian() * 0.25D,
                            rand.nextDouble() * 0.3D, to.z * 0.4D + rand.nextGaussian() * 0.25D);
                }
                break;
            case MsgTribunalFx.CASKET_WINTER:
                if (m.id == 0) {
                    effects.add(new VaultFx.Gale(at, to, m.time, seed));
                    for (int i = 0; i < 50; i++) {
                        double s = 0.5D + rand.nextDouble() * 1.3D;
                        mc.world.spawnParticle(EnumParticleTypes.END_ROD, at.x, at.y, at.z, to.x * s + rand.nextGaussian() * 0.2D,
                                to.y * s + rand.nextGaussian() * 0.2D, to.z * s + rand.nextGaussian() * 0.2D);
                    }
                } else {
                    effects.add(new VaultFx.Burst(at, m.a, m.time, seed));
                    for (int i = 0; i < 120; i++) {
                        double a = rand.nextDouble() * Math.PI * 2D, s = 0.4D + rand.nextDouble() * 0.6D;
                        mc.world.spawnParticle(EnumParticleTypes.END_ROD, at.x, at.y + 0.3D, at.z, Math.cos(a) * s, 0.05D + rand.nextDouble() * 0.2D,
                                Math.sin(a) * s);
                    }
                    shake = Math.max(shake, 1.2F * closeness(dist, 20D));
                }
                break;
            case MsgTribunalFx.CASKET_FREEZE:
                effects.add(new VaultFx.Shell(m.id, m.a, m.b, m.time));
                break;
            case MsgTribunalFx.DESTROYER_WOUND: {
                // The Destroyer Armor hurt: sparks and shards of its plate bursting from where the blow landed.
                Entity hit = mc.world.getEntityByID(m.id);
                if (!(hit instanceof EntityDestroyerArmor)) {
                    break;
                }
                float severity = m.time / 100F;
                ArmorWounds.burst((EntityDestroyerArmor) hit, at, to.normalize(), effects, rand, severity);
                effects.add(new VaultFx.Impact(at, to, 0.25F + 0.2F * Math.min(severity, 2F), seed));
                if (hit.isPassenger(mc.player)) {
                    shake = Math.max(shake, 1.5F + 1.5F * Math.min(severity, 3F));
                }
                break;
            }
            case MsgTribunalFx.DESTROYER_IMPLODE:
                // The Destroyer Armor falling in on itself, its core breached.
                effects.add(new VaultFx.Implosion(at, m.a, EntityDestroyerArmor.IMPLODE_TICKS, m.time, seed));
                shake = Math.max(shake, 3F * closeness(dist, 40D));
                break;
            case MsgTribunalFx.DESTROYER_STONE:
                stones(m, at, to);
                break;
            default:
                break;
        }
    }

    // ------------------------------------------------------------------ every tick

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || mc.world == null || mc.player == null || mc.isGamePaused()) {
            return;
        }
        tick++;
        FxRenderer.tickAll(effects);
        if (lastRay != null && lastRay.isDead()) {
            lastRay = null;
        }
        shake *= 0.86F;
        frost();
        // The Destroyer Armor coming apart, wherever it is, as it is beaten down.
        for (Entity entity : mc.world.loadedEntityList) {
            if (entity instanceof EntityDestroyerArmor && entity.getDistanceSq(mc.player) < 96D * 96D) {
                ArmorWounds.tick((EntityDestroyerArmor) entity, effects, rand);
                ArmorWounds.core((EntityDestroyerArmor) entity, effects, rand);
            }
        }
        if (!inVault()) {
            motes.clear();
            return;
        }
        VaultTreasures.tick(mc);
        drift();
        embers();
        // Somewhere in the dark, the vault settles.
        if (--nextGroan <= 0) {
            nextGroan = 500 + rand.nextInt(900);
            Vec3d p = mc.player.getPositionVector().add(rand.nextGaussian() * 20D, 6D, rand.nextGaussian() * 20D);
            mc.world.playSound(p.x, p.y, p.z, SoundEvents.AMBIENT_CAVE, SoundCategory.AMBIENT, 0.6F, 0.5F + rand.nextFloat() * 0.2F, false);
        }
    }

    /** Frost breathing off the Casket in anyone's hands. */
    private void frost() {
        if (tick % 2 != 0) {
            return;
        }
        for (EntityPlayer player : mc.world.playerEntities) {
            if (player.getDistance(mc.player) > 48D) {
                continue;
            }
            for (EnumHand hand : EnumHand.values()) {
                if (player.getHeldItem(hand).getItem() != EternityItems.CASKET) {
                    continue;
                }
                float yaw = (float) Math.toRadians(player.renderYawOffset);
                double side = (hand == EnumHand.MAIN_HAND) == (player.getPrimaryHand() == net.minecraft.util.EnumHandSide.RIGHT) ? -0.4D : 0.4D;
                double x = player.posX + Math.cos(yaw) * side - Math.sin(yaw) * 0.3D;
                double z = player.posZ + Math.sin(yaw) * side + Math.cos(yaw) * 0.3D;
                mc.world.spawnParticle(EnumParticleTypes.END_ROD, x + rand.nextGaussian() * 0.1D, player.posY + 0.8D, z + rand.nextGaussian() * 0.1D,
                        rand.nextGaussian() * 0.01D, -0.03D, rand.nextGaussian() * 0.01D);
            }
        }
    }

    /** Embers rising off the Eternal Flame; frost spilling off the Casket. */
    private void embers() {
        Vec3d flame = VaultTreasures.at(OdinsVault.Relic.ETERNAL_FLAME, 34.5D);
        if (flame.squareDistanceTo(mc.player.getPositionVector()) < 48D * 48D) {
            if (rand.nextInt(3) == 0) {
                mc.world.spawnParticle(EnumParticleTypes.FLAME, flame.x + rand.nextGaussian() * 0.15D, flame.y + 0.2D, flame.z + rand.nextGaussian() * 0.15D,
                        0D, 0.04D + rand.nextDouble() * 0.03D, 0D);
            }
            if (rand.nextInt(5) == 0) {
                mc.world.spawnParticle(EnumParticleTypes.LAVA, flame.x, flame.y + 0.1D, flame.z, 0D, 0D, 0D);
            }
        }
        Vec3d crown = VaultTreasures.at(OdinsVault.Relic.CROWN, 22D);
        if (rand.nextInt(4) == 0 && crown.squareDistanceTo(mc.player.getPositionVector()) < 40D * 40D) {
            mc.world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, crown.x + rand.nextGaussian() * 0.2D, crown.y + 0.3D, crown.z + rand.nextGaussian() * 0.2D,
                    0D, 0.02D, 0D);
        }
        if (casket && rand.nextInt(3) == 0) {
            Vec3d c = VaultTreasures.at(OdinsVault.Relic.CASKET, 30D);
            mc.world.spawnParticle(EnumParticleTypes.END_ROD, c.x + rand.nextGaussian() * 0.25D, c.y, c.z + rand.nextGaussian() * 0.2D,
                    0D, -0.02D, 0D);
        }
    }

    /** Dust turning slowly in the dark around you, bright only where the light catches it. */
    private void drift() {
        Vec3d me = mc.player.getPositionVector();
        Iterator<double[]> it = motes.iterator();
        while (it.hasNext()) {
            double[] m = it.next();
            m[6]++;
            m[0] += m[3];
            m[1] += m[4];
            m[2] += m[5];
            m[3] += rand.nextGaussian() * 0.0008D;
            m[4] += rand.nextGaussian() * 0.0006D;
            m[5] += rand.nextGaussian() * 0.0008D;
            if (m[6] > m[7] || (m[0] - me.x) * (m[0] - me.x) + (m[2] - me.z) * (m[2] - me.z) > 400D) {
                it.remove();
            }
        }
        while (motes.size() < MOTES) {
            double x = me.x + rand.nextGaussian() * 9D, y = me.y + rand.nextDouble() * 9D - 1D, z = me.z + rand.nextGaussian() * 9D;
            if (!mc.world.isAirBlock(new BlockPos(x, y, z))) {
                motes.add(new double[]{x, y, z, 0D, 0D, 0D, 9999D, 0D});
                continue;
            }
            motes.add(new double[]{x, y, z, rand.nextGaussian() * 0.004D, rand.nextGaussian() * 0.002D - 0.001D, rand.nextGaussian() * 0.004D,
                    0D, 200 + rand.nextInt(300)});
        }
    }

    // ------------------------------------------------------------------ drawing

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Entity view = mc.getRenderViewEntity();
        if (mc.world == null || view == null) {
            return;
        }
        boolean vault = inVault();
        if (!vault && effects.isEmpty()) {
            return;
        }
        float pt = event.getPartialTicks();
        Vec3d camera = new Vec3d(view.lastTickPosX + (view.posX - view.lastTickPosX) * pt,
                view.lastTickPosY + (view.posY - view.lastTickPosY) * pt,
                view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * pt);
        GlSnapshot before = GlSnapshot.take();
        if (vault) {
            VaultTreasures.render(mc.world, camera, pt, casket);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
            atmosphere(camera, pt);
        }
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
        FxRenderer.renderAll(effects, camera, pt);
        before.restore();
    }

    /** The shafts of light from the roof and over each treasure, the far wall's glow, and the dust in the air. */
    private void atmosphere(Vec3d camera, float pt) {
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        GlStateManager.depthMask(false);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        float t = tick + pt;
        // Pale light falling the height of the hall from the lattice in its roof.
        for (int z = 32; z >= -58; z -= 6) {
            float breathe = 0.85F + 0.15F * MathHelper.sin(t * 0.013F + z * 0.4F);
            shaft(new Vec3d(0.5D, OdinsVault.STAND + 26.5D, z + 0.5D).subtract(camera), 25.5D, 2.6D, 0.6D,
                    new float[]{0.72F, 0.8F, 0.95F}, 0.045F * breathe);
        }
        // Warmer light onto each treasure from the lamp over its bay.
        for (OdinsVault.Relic relic : OdinsVault.Relic.values()) {
            if (relic.bay < 0) {
                continue;
            }
            Vec3d top = relic.base().add(0D, 9.5D, 0D).subtract(camera);
            shaft(top, 9.5D, 0.7D, 2.1D, new float[]{1F, 0.88F, 0.66F}, 0.07F);
        }
        // The doors you came in by, glowing.
        glowAt(new Vec3d(0.5D, 61.5D, OdinsVault.DOOR_Z - 0.4D).subtract(camera), 3.5D, new float[]{0.9F, 0.92F, 1F}, 0.18F);
        motes(camera, pt);
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.depthMask(true);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.popMatrix();
    }

    /** A soft upright shaft of light from top down to its foot, turned to face you, widening as it falls. */
    private static void shaft(Vec3d top, double length, double topWidth, double footWidth, float[] col, float alpha) {
        Vec3d side = new Vec3d(top.z, 0D, -top.x);
        if (side.lengthSquared() < 1.0E-6) {
            return;
        }
        side = side.normalize();
        Vec3d foot = top.add(0D, -length, 0D);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        for (int half = -1; half <= 1; half += 2) {
            buffer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
            buffer.pos(top.x, top.y, top.z).color(col[0], col[1], col[2], alpha).endVertex();
            buffer.pos(top.x + side.x * topWidth * half, top.y, top.z + side.z * topWidth * half).color(col[0], col[1], col[2], 0F).endVertex();
            buffer.pos(foot.x, foot.y, foot.z).color(col[0], col[1], col[2], alpha * 0.25F).endVertex();
            buffer.pos(foot.x + side.x * footWidth * half, foot.y, foot.z + side.z * footWidth * half).color(col[0], col[1], col[2], 0F).endVertex();
            tessellator.draw();
        }
    }

    /** A soft disc of light at c, facing the camera. */
    private static void glowAt(Vec3d c, double radius, float[] col, float alpha) {
        Vec3d n = c.normalize();
        Vec3d u = n.crossProduct(Math.abs(n.y) > 0.9D ? new Vec3d(1D, 0D, 0D) : new Vec3d(0D, 1D, 0D)).normalize();
        Vec3d v = n.crossProduct(u).normalize();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(c.x, c.y, c.z).color(col[0], col[1], col[2], alpha).endVertex();
        for (int i = 0; i <= 24; i++) {
            double a = Math.PI * 2D * i / 24D;
            Vec3d o = u.scale(Math.cos(a) * radius).add(v.scale(Math.sin(a) * radius));
            buffer.pos(c.x + o.x, c.y + o.y, c.z + o.z).color(col[0], col[1], col[2], 0F).endVertex();
        }
        tessellator.draw();
    }

    /** Each mote a tiny cross of light, brighter where the vault's light falls on it. */
    private void motes(Vec3d camera, float pt) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (double[] m : motes) {
            if (m[7] <= 0D) {
                continue;
            }
            double x = m[0] + m[3] * pt - camera.x, y = m[1] + m[4] * pt - camera.y, z = m[2] + m[5] * pt - camera.z;
            float life = (float) (m[6] / m[7]);
            float fade = Math.min(1F, life * 6F) * Math.min(1F, (1F - life) * 6F);
            int light = mc.world.getLightFor(net.minecraft.world.EnumSkyBlock.BLOCK, pos.setPos(m[0], m[1], m[2]));
            float a = (0.08F + 0.5F * light / 15F) * fade;
            double s = 0.025D;
            buffer.pos(x - s, y, z).color(1F, 0.95F, 0.85F, a).endVertex();
            buffer.pos(x, y + s, z).color(1F, 0.95F, 0.85F, a).endVertex();
            buffer.pos(x + s, y, z).color(1F, 0.95F, 0.85F, a).endVertex();
            buffer.pos(x, y - s, z).color(1F, 0.95F, 0.85F, a).endVertex();
            buffer.pos(x, y, z - s).color(1F, 0.95F, 0.85F, a).endVertex();
            buffer.pos(x, y + s, z).color(1F, 0.95F, 0.85F, a).endVertex();
            buffer.pos(x, y, z + s).color(1F, 0.95F, 0.85F, a).endVertex();
            buffer.pos(x, y - s, z).color(1F, 0.95F, 0.85F, a).endVertex();
        }
        tessellator.draw();
    }

    // ------------------------------------------------------------------ the air in there

    /** A cold haze: the far end of the hall is only a wall of light glowing through it. */
    @SubscribeEvent
    public void onFog(EntityViewRenderEvent.RenderFogEvent event) {
        if (!inVault()) {
            return;
        }
        float far = event.getFarPlaneDistance();
        GlStateManager.setFogStart(Math.min(far * 0.5F, 8F));
        GlStateManager.setFogEnd(Math.min(far, 115F));
    }

    @SubscribeEvent
    public void onFogColour(EntityViewRenderEvent.FogColors event) {
        if (inVault()) {
            event.setRed(0.035F);
            event.setGreen(0.04F);
            event.setBlue(0.052F);
        }
    }

    @SubscribeEvent
    public void onCamera(EntityViewRenderEvent.CameraSetup event) {
        if (!EternityConfig.client.screenShake || shake < 0.001F) {
            return;
        }
        float t = tick + (float) event.getRenderPartialTicks();
        event.setYaw(event.getYaw() + (MathHelper.sin(t * 3.7F) + MathHelper.sin(t * 8.3F) * 0.5F) * shake * 0.5F);
        event.setPitch(event.getPitch() + (MathHelper.sin(t * 3.1F) + MathHelper.sin(t * 6.9F) * 0.5F) * shake * 0.5F);
        event.setRoll(event.getRoll() + MathHelper.sin(t * 2.3F) * shake * 0.6F);
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        mc.addScheduledTask(() -> {
            effects.clear();
            motes.clear();
            shake = 0F;
            casket = true;
            lastRay = null;
        });
    }

    /** The Destroyer is awake and near enough to be heard. */
    public boolean destroyerFighting() {
        if (mc.world == null || mc.player == null || !inVault()) {
            return false;
        }
        for (Entity entity : mc.world.loadedEntityList) {
            if (entity instanceof EntityDestroyer && ((EntityDestroyer) entity).isAwake() && entity.getDistance(mc.player) < 90F) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ helpers

    /** The Infinity Stones in the Destroyer Armor: their light along its ray. */
    private void stones(MsgTribunalFx m, Vec3d at, Vec3d to) {
        if (m.time == ArmorPowers.FX_RAY) {
            for (int stone = 0; stone < ArmorPowers.COUNT; stone++) {
                if ((m.id & (1 << stone)) == 0) {
                    continue;
                }
                float[] c = rgb(ArmorPowers.RGB[stone]);
                for (int i = 0; i < 3; i++) {
                    Vec3d p = at.add(to.subtract(at).scale(rand.nextDouble()));
                    mc.world.spawnParticle(EnumParticleTypes.SPELL_MOB, p.x + rand.nextGaussian() * 0.25D, p.y + rand.nextGaussian() * 0.25D,
                            p.z + rand.nextGaussian() * 0.25D, c[0], c[1], c[2]);
                }
            }
        }
    }

    private static float[] rgb(int color) {
        return new float[]{(color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F, (color & 255) / 255F};
    }

    private void sparks(Vec3d at, int count, double force) {
        for (int i = 0; i < count; i++) {
            Vec3d d = new Vec3d(rand.nextGaussian(), rand.nextGaussian(), rand.nextGaussian()).normalize().scale(force * (0.3D + rand.nextDouble()));
            mc.world.spawnParticle(EnumParticleTypes.FLAME, at.x, at.y, at.z, d.x * 0.3D, d.y * 0.3D, d.z * 0.3D);
            mc.world.spawnParticle(EnumParticleTypes.CRIT, at.x, at.y, at.z, d.x, d.y, d.z);
        }
    }

    /** The floor thrown up in a ring around a point: chips of whatever it is made of, and dust. */
    private void dust(Vec3d at, double reach, int count) {
        for (int i = 0; i < count; i++) {
            double a = rand.nextDouble() * Math.PI * 2D;
            double r = rand.nextDouble() * reach;
            double x = at.x + Math.cos(a) * r, z = at.z + Math.sin(a) * r;
            BlockPos ground = new BlockPos(x, at.y - 0.5D, z);
            IBlockState state = mc.world.getBlockState(ground);
            double out = 0.1D + rand.nextDouble() * 0.25D;
            if (!mc.world.isAirBlock(ground)) {
                mc.world.spawnParticle(EnumParticleTypes.BLOCK_CRACK, x, at.y + 0.1D, z, Math.cos(a) * out, 0.2D + rand.nextDouble() * 0.3D,
                        Math.sin(a) * out, Block.getStateId(state));
            }
            if (i % 3 == 0) {
                mc.world.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL, x, at.y + 0.2D, z, Math.cos(a) * out * 0.4D, 0.03D, Math.sin(a) * out * 0.4D);
            }
        }
    }

    private static Vec3d closest(Vec3d a, Vec3d b, Vec3d p) {
        Vec3d ab = b.subtract(a);
        double len = ab.lengthSquared();
        if (len < 1.0E-8) {
            return a;
        }
        double t = MathHelper.clamp(p.subtract(a).dotProduct(ab) / len, 0D, 1D);
        return a.add(ab.scale(t));
    }

    private static float closeness(double dist, double range) {
        return (float) MathHelper.clamp(1D - dist / range, 0D, 1D);
    }
}
