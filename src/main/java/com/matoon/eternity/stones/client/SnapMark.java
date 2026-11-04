package com.matoon.eternity.stones.client;

import com.matoon.eternity.stones.network.MsgScars;
import lucraft.mods.lucraftcore.superpowers.render.RenderSuperpowerLayerEvent;
import lucraft.mods.lucraftcore.superpowers.render.SuperpowerRenderer;
import lucraft.mods.lucraftcore.util.helper.LCRenderHelper;
import lucraft.mods.lucraftcore.util.helper.PlayerHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.RenderSpecificHandEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * The burns snapping leaves on a player's arms (see {@code SnapScars}), drawn as {@link BurnPainter} paints them for
 * each arm's burn, and painted again whenever a burn deepens or heals enough to show. The cracks in a deep burn glow
 * like embers and slowly throb; the arm that has just snapped flares white-hot and cools over a few seconds, and smokes
 * and throws sparks for a while after. A deep burn goes on smouldering, a wisp of smoke now and then, until it heals.
 * <p>
 * Every burn is drawn for its owner alone, from noise seeded by who they are: the burn itself, lit by the world, then
 * the glowing cracks and the flare, which make their own light. It shows on the player in the world, and on their own
 * bare arm in first person.
 */
public final class SnapMark {

    public static final SnapMark INSTANCE = new SnapMark();

    private static final int SIZE = 64;
    /** How long a fresh burn takes to cool, in seconds: the flare, and the glow settling to its embers. */
    private static final float FLARE = 2.6F, COOLING = 14F;
    /** Burns are painted again when they change by this much. */
    private static final float STEP = 0.25F;

    private final Map<UUID, Scars> scars = new HashMap<>();
    private final Random random = new Random();

    private SnapMark() {
    }

    private static final class Scars {
        float right, left;
        /** When each arm last snapped, by this clock. */
        long rightSnap, leftSnap;
        final float phase;
        Burn burn;

        Scars(UUID id) {
            phase = (id.hashCode() & 1023) / 1023F * 6.28F;
            // Burns already there when first seen are old ones.
            rightSnap = leftSnap = System.currentTimeMillis() - 600_000L;
        }

        float level(EnumHandSide side) {
            return side == EnumHandSide.RIGHT ? right : left;
        }

        /** Seconds since either arm snapped. */
        float age() {
            return (System.currentTimeMillis() - Math.max(rightSnap, leftSnap)) / 1000F;
        }

        float worst() {
            return BurnPainter.severity(Math.max(right, left));
        }

        float flare() {
            float f = Math.max(0F, 1F - age() / FLARE);
            return f * f;
        }

        /** How brightly the cracks burn: bright from a snap, settling to embers that last as long as the burn is deep. */
        float glow() {
            float seconds = System.currentTimeMillis() / 1000F;
            float embers = Math.max(0F, worst() - 0.35F) * 0.7F;
            float fresh = 0.75F * (float) Math.exp(-age() / COOLING);
            float throb = 0.8F + 0.14F * MathHelper.sin(seconds * 2.2F + phase) + 0.06F * MathHelper.sin(seconds * 5.3F + phase * 2F);
            return MathHelper.clamp((embers + fresh) * throb, 0F, 1F);
        }
    }

    /** One player's burns, as three textures on the skin's layout. */
    private static final class Burn {
        final float right, left;
        final boolean slim;
        final DynamicTexture[] textures = new DynamicTexture[3];
        final ResourceLocation[] locations = new ResourceLocation[3];

        Burn(UUID id, float right, float left, boolean slim) {
            this.right = right;
            this.left = left;
            this.slim = slim;
            int[][] layers = BurnPainter.paint(id.getMostSignificantBits() ^ id.getLeastSignificantBits(), right, left, slim);
            String[] names = {"char", "glow", "heat"};
            for (int i = 0; i < 3; i++) {
                textures[i] = new DynamicTexture(SIZE, SIZE);
                System.arraycopy(layers[i], 0, textures[i].getTextureData(), 0, SIZE * SIZE);
                textures[i].updateDynamicTexture();
                locations[i] = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation("eternity_snap_" + names[i], textures[i]);
            }
        }

        void dispose() {
            for (ResourceLocation location : locations) {
                Minecraft.getMinecraft().getTextureManager().deleteTexture(location);
            }
        }
    }

    // ------------------------------------------------------------------ burns

    void update(MsgScars message) {
        Scars scar = scars.get(message.player);
        if (message.right <= 0F && message.left <= 0F) {
            if (scar != null && scar.burn != null) {
                scar.burn.dispose();
            }
            scars.remove(message.player);
            return;
        }
        if (scar == null) {
            scar = new Scars(message.player);
            scars.put(message.player, scar);
        }
        scar.right = message.right;
        scar.left = message.left;
        if (message.flare == MsgScars.FLARE_RIGHT) {
            scar.rightSnap = System.currentTimeMillis();
        } else if (message.flare == MsgScars.FLARE_LEFT) {
            scar.leftSnap = System.currentTimeMillis();
        }
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        Minecraft.getMinecraft().addScheduledTask(this::clear);
    }

    void clear() {
        for (Scars scar : scars.values()) {
            if (scar.burn != null) {
                scar.burn.dispose();
            }
        }
        scars.clear();
    }

    /** Their burns' textures, painted the first time they are seen, and again when they have changed enough to show. */
    private static Burn burn(EntityPlayer player, Scars scar) {
        boolean slim = PlayerHelper.hasSmallArms(player);
        float right = Math.round(scar.right / STEP) * STEP, left = Math.round(scar.left / STEP) * STEP;
        if (scar.burn == null || scar.burn.right != right || scar.burn.left != left || scar.burn.slim != slim) {
            if (scar.burn != null) {
                scar.burn.dispose();
            }
            scar.burn = new Burn(player.getUniqueID(), right, left, slim);
        }
        return scar.burn;
    }

    // ------------------------------------------------------------------ on the player

    @SubscribeEvent
    public void onRenderSuperpowerLayer(RenderSuperpowerLayerEvent event) {
        EntityPlayer player = event.getPlayer();
        Scars scar = scars.get(player.getUniqueID());
        if (scar == null || player.isInvisible()) {
            return;
        }
        Burn burn = burn(player, scar);
        RenderPlayer renderer = event.getRenderPlayer();
        GlStateManager.pushMatrix();
        SuperpowerRenderer.overrideSkin = false;
        for (int layer = 0; layer < 3; layer++) {
            float strength = strength(scar, layer);
            if (strength <= 0.01F) {
                continue;
            }
            ModelPlayer model = model(layer, burn.slim);
            model.setModelAttributes(renderer.getMainModel());
            begin(layer, strength, burn);
            model.render(player, event.getLimbSwing(), event.getLimbSwingAmount(), event.getAgeInTicks(), event.getNetHeadYaw(),
                    event.getHeadPitch(), event.getScale());
            end(layer);
        }
        SuperpowerRenderer.overrideSkin = true;
        GlStateManager.popMatrix();
    }

    /** Their own burnt arm, bare, in first person: drawn as vanilla draws it, then burnt over. */
    @SubscribeEvent
    public void onRenderHand(RenderSpecificHandEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        AbstractClientPlayer player = mc.player;
        Scars scar = player == null ? null : scars.get(player.getUniqueID());
        if (scar == null || event.getHand() != EnumHand.MAIN_HAND || !event.getItemStack().isEmpty() || player.isInvisible()
                || scar.level(player.getPrimaryHand()) <= 0.05F) {
            return;
        }
        event.setCanceled(true);
        Burn burn = burn(player, scar);
        boolean right = player.getPrimaryHand() == EnumHandSide.RIGHT;
        float flip = right ? 1F : -1F, swing = event.getSwingProgress(), root = MathHelper.sqrt(swing);

        // As ItemRenderer.renderArmFirstPerson places the bare arm.
        GlStateManager.pushMatrix();
        GlStateManager.translate(flip * (-0.3F * MathHelper.sin(root * (float) Math.PI) + 0.64000005F),
                0.4F * MathHelper.sin(root * ((float) Math.PI * 2F)) - 0.6F + event.getEquipProgress() * -0.6F,
                -0.4F * MathHelper.sin(swing * (float) Math.PI) - 0.71999997F);
        GlStateManager.rotate(flip * 45F, 0F, 1F, 0F);
        GlStateManager.rotate(flip * MathHelper.sin(root * (float) Math.PI) * 70F, 0F, 1F, 0F);
        GlStateManager.rotate(flip * MathHelper.sin(swing * swing * (float) Math.PI) * -20F, 0F, 0F, 1F);
        mc.getTextureManager().bindTexture(player.getLocationSkin());
        GlStateManager.translate(flip * -1F, 3.6F, 3.5F);
        GlStateManager.rotate(flip * 120F, 0F, 0F, 1F);
        GlStateManager.rotate(200F, 1F, 0F, 0F);
        GlStateManager.rotate(flip * -135F, 0F, 1F, 0F);
        GlStateManager.translate(flip * 5.6F, 0F, 0F);
        RenderPlayer renderer = (RenderPlayer) mc.getRenderManager().<AbstractClientPlayer>getEntityRenderObject(player);
        GlStateManager.disableCull();
        if (right) {
            renderer.renderRightArm(player);
        } else {
            renderer.renderLeftArm(player);
        }
        for (int layer = 0; layer < 3; layer++) {
            float strength = strength(scar, layer);
            if (strength <= 0.01F) {
                continue;
            }
            ModelPlayer model = model(layer, burn.slim);
            model.swingProgress = 0F;
            model.isSneak = false;
            model.setRotationAngles(0F, 0F, 0F, 0F, 0F, 0.0625F, player);
            ModelRenderer arm = right ? model.bipedRightArm : model.bipedLeftArm;
            arm.rotateAngleX = 0F;
            begin(layer, strength, burn);
            arm.render(0.0625F);
            end(layer);
        }
        GlStateManager.enableCull();
        GlStateManager.popMatrix();
    }

    private static float strength(Scars scar, int layer) {
        return layer == 0 ? 1F : layer == 1 ? scar.glow() : scar.flare();
    }

    private static final Map<String, ModelPlayer> MODELS = new HashMap<>();

    /** Each layer a hair outside the one before, so none of them flicker through another or the sleeve. */
    private static ModelPlayer model(int layer, boolean slim) {
        return MODELS.computeIfAbsent(layer + "_" + slim, key -> new ModelPlayer(0.28F + layer * 0.03F, slim));
    }

    private static void begin(int layer, float strength, Burn burn) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(burn.locations[layer]);
        GlStateManager.enableBlend();
        if (layer == 0) {
            // The burn, lit like the rest of them.
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GlStateManager.color(1F, 1F, 1F, 1F);
            return;
        }
        // The embers and the flare make their own light.
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        LCRenderHelper.setLightmapTextureCoords(240F, 240F);
        GlStateManager.color(1F, 1F, 1F, strength);
    }

    private static void end(int layer) {
        if (layer > 0) {
            LCRenderHelper.restoreLightmapTextureCoords();
            GlStateManager.depthMask(true);
            GlStateManager.enableLighting();
        }
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableBlend();
        GlStateManager.color(1F, 1F, 1F, 1F);
    }

    // ------------------------------------------------------------------ smoke and sparks

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null) {
            // Burns sent as they join can arrive before their world does; they are cleared on leaving, not here.
            return;
        }
        if (mc.isGamePaused()) {
            return;
        }
        for (Map.Entry<UUID, Scars> entry : scars.entrySet()) {
            EntityPlayer player = mc.world.getPlayerEntityByUUID(entry.getKey());
            if (player == null || player.isInvisible() || mc.player.getDistanceSq(player) > 64D * 64D
                    || player == mc.player && mc.gameSettings.thirdPersonView == 0) {
                continue;
            }
            Scars scar = entry.getValue();
            // Smoke rises from the arm that snapped last, or the worse one.
            EnumHandSide side = scar.age() < 30F ? (scar.rightSnap >= scar.leftSnap ? EnumHandSide.RIGHT : EnumHandSide.LEFT)
                    : scar.right >= scar.left ? EnumHandSide.RIGHT : EnumHandSide.LEFT;
            smoke(mc, player, scar, side);
        }
    }

    /** Thick smoke and showers of sparks just after a snap, thinning to the odd wisp from a burn deep enough to smoulder. */
    private void smoke(Minecraft mc, EntityPlayer player, Scars scar, EnumHandSide side) {
        float age = scar.age(), severity = BurnPainter.severity(scar.level(side));
        float fresh = Math.max(0F, 1F - age / 8F);
        float smoulder = Math.max(0F, severity - 0.3F);
        if (fresh <= 0F && smoulder <= 0F) {
            return;
        }
        float yaw = player.renderYawOffset * 0.017453292F;
        double sign = side == EnumHandSide.RIGHT ? 1D : -1D;
        // The hand hangs a little out from the body, at the end of the arm.
        double x = player.posX - MathHelper.cos(yaw) * 0.36D * sign;
        double z = player.posZ - MathHelper.sin(yaw) * 0.36D * sign;
        double y = player.posY + (player.isSneaking() ? 0.5D : 0.7D);
        if (age < 0.15F) {
            for (int i = 0; i < 8; i++) {
                mc.world.spawnParticle(EnumParticleTypes.LAVA, x, y + 0.1D, z, 0D, 0D, 0D);
            }
        }
        int smoke = fresh > 0F ? (random.nextFloat() < 0.35F + fresh * 0.5F ? 1 : 0) + (fresh > 0.6F ? 1 : 0) : random.nextFloat() < smoulder * 0.15F ? 1 : 0;
        for (int i = 0; i < smoke; i++) {
            mc.world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL,
                    x + (random.nextDouble() - 0.5D) * 0.12D, y + random.nextDouble() * 0.45D, z + (random.nextDouble() - 0.5D) * 0.12D,
                    (random.nextDouble() - 0.5D) * 0.01D, 0.015D + random.nextDouble() * 0.02D, (random.nextDouble() - 0.5D) * 0.01D);
        }
        if (random.nextFloat() < smoulder * 0.08F + fresh * 0.6F) {
            // A spark, orange, drifting up off the char.
            mc.world.spawnParticle(EnumParticleTypes.REDSTONE, x + (random.nextDouble() - 0.5D) * 0.15D, y + random.nextDouble() * 0.5D,
                    z + (random.nextDouble() - 0.5D) * 0.15D, 1D, 0.42D + random.nextDouble() * 0.2D, 0.05D);
        }
    }
}
