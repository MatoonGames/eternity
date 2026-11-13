package com.matoon.eternity.client;

import com.matoon.eternity.Eternity;
import com.matoon.eternity.EternitySounds;
import com.matoon.eternity.client.render.ModelMagus;
import com.matoon.eternity.entity.EntityMagus;
import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.network.MsgGauntletFight;
import com.matoon.eternity.network.MsgStoryAct;
import lucraft.mods.lucraftcore.infinity.items.ItemInfinityGauntlet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderSpecificHandEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * The Magus's last fight, from where you stand.
 * <ul>
 * <li>The gauntlet's power-up sounding off him while he holds it before his chest, cut off the
 * instant he is stopped; and the snap.</li>
 * <li>A struggle for the gauntlet: you can't move or look away; it is between your hand and his,
 * close, shaking, and you pull (jump, as fast as you can) against a bar he is dragging his way.</li>
 * <li>When he snaps: everything white, and a ringing, and the world coming back out of it.</li>
 * </ul>
 */
@SideOnly(Side.CLIENT)
public final class GauntletFightClient {

    public static final GauntletFightClient INSTANCE = new GauntletFightClient();

    private static final ResourceLocation MAGUS_SKIN = new ResourceLocation(Eternity.MODID, "textures/entity/magus.png");

    private final Minecraft mc = Minecraft.getMinecraft();
    /** The power-up playing off each Magus who is powering up, by entity id. */
    private final Map<Integer, ISound> powering = new HashMap<>();
    /** The tick each Magus last snapped at, so each snap sounds once. */
    private final Map<Integer, Integer> snapped = new HashMap<>();
    private int tick;

    // A struggle.
    private boolean struggling;
    private int magusId = -1;
    private float bar = 0.5F, shownBar = 0.5F, prevShownBar = 0.5F;
    private int struggleStart, endedAt = -1000;
    private boolean won;
    private float yank;
    private final ModelMagus magusModel = new ModelMagus();
    private ModelPlayer armModel, slimArmModel;

    // The white of his snap: when, and how long it takes to clear.
    private int whiteAt = -1, whiteFor = 100;

    private GauntletFightClient() {
    }

    // ------------------------------------------------------------------ messages

    public void receive(MsgGauntletFight m) {
        switch (m.type) {
            case MsgGauntletFight.START:
                struggling = true;
                magusId = m.id;
                bar = shownBar = prevShownBar = m.value / 1000F;
                struggleStart = tick;
                mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK, 0.6F));
                break;
            case MsgGauntletFight.BAR:
                bar = m.value / 1000F;
                break;
            case MsgGauntletFight.END:
                struggling = false;
                won = m.value == 1;
                endedAt = tick;
                mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(won ? SoundEvents.ENTITY_PLAYER_LEVELUP
                        : SoundEvents.ENTITY_PLAYER_HURT, won ? 0.6F : 0.8F));
                break;
            case MsgGauntletFight.WHITE:
                whiteAt = tick;
                whiteFor = Math.max(20, m.value);
                // The ears ringing with it.
                mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(EternitySounds.OMEN_SHIMMER, 2F));
                mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.BLOCK_NOTE_CHIME, 1.9F));
                break;
            default:
                break;
        }
    }

    // ------------------------------------------------------------------ ticking

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (mc.world == null) {
            stopAll();
            return;
        }
        tick++;
        prevShownBar = shownBar;
        shownBar += (bar - shownBar) * 0.35F;
        yank *= 0.7F;
        if (struggling) {
            // Every jump is a pull.
            while (mc.gameSettings.keyBindJump.isPressed()) {
                EternityNetwork.sendToServer(new MsgStoryAct(MsgStoryAct.STRUGGLE, 0, 0));
                yank = 1F;
                bar = Math.min(1F, bar + 0.02F);
            }
            clearKeys();
            Entity e = mc.world.getEntityByID(magusId);
            if (!(e instanceof EntityMagus) || e.isDead || tick - struggleStart > 200) {
                struggling = false;
            }
        }
        if (whiteAt >= 0 && tick - whiteAt > whiteFor + 10) {
            whiteAt = -1;
        }
        for (Entity e : mc.world.loadedEntityList) {
            if (!(e instanceof EntityMagus)) {
                continue;
            }
            EntityMagus m = (EntityMagus) e;
            if (m.pose() == EntityMagus.Pose.POWER && !powering.containsKey(m.getEntityId())) {
                ISound s = new PositionedSoundRecord(EternitySounds.STONES_POWER_UP, SoundCategory.HOSTILE, 3F, 1F,
                        new BlockPos(m.posX, m.posY + 1.5D, m.posZ));
                mc.getSoundHandler().playSound(s);
                powering.put(m.getEntityId(), s);
            }
            if (m.pose() == EntityMagus.Pose.SNAP && m.clientPoseTicks == EntityMagus.SNAP_AT
                    && !Integer.valueOf(m.ticksExisted - m.clientPoseTicks).equals(snapped.get(m.getEntityId()))) {
                snapped.put(m.getEntityId(), m.ticksExisted - m.clientPoseTicks);
                mc.getSoundHandler().playSound(new PositionedSoundRecord(EternitySounds.STONES_SNAP_1, SoundCategory.HOSTILE, 4F, 1F,
                        new BlockPos(m.posX, m.posY + 2.5D, m.posZ)));
            }
        }
        // Stopped, or gone: the sound goes with it.
        Iterator<Map.Entry<Integer, ISound>> it = powering.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, ISound> en = it.next();
            Entity e = mc.world.getEntityByID(en.getKey());
            if (!(e instanceof EntityMagus) || e.isDead || ((EntityMagus) e).pose() != EntityMagus.Pose.POWER) {
                mc.getSoundHandler().stopSound(en.getValue());
                it.remove();
            }
        }
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        mc.addScheduledTask(() -> {
            stopAll();
            struggling = false;
            whiteAt = -1;
        });
    }

    private void stopAll() {
        for (ISound s : powering.values()) {
            mc.getSoundHandler().stopSound(s);
        }
        powering.clear();
        snapped.clear();
    }

    // ------------------------------------------------------------------ held in the struggle

    private void clearKeys() {
        GameSettings s = mc.gameSettings;
        for (KeyBinding key : new KeyBinding[]{s.keyBindForward, s.keyBindBack, s.keyBindLeft, s.keyBindRight, s.keyBindSneak,
                s.keyBindSprint, s.keyBindAttack, s.keyBindUseItem, s.keyBindDrop, s.keyBindSwapHands}) {
            KeyBinding.setKeyBindState(key.getKeyCode(), false);
        }
    }

    /**
     * Keys that would take it out of their hand mid-struggle (drop, swap, the hotbar, the inventory) are swallowed as
     * they are pressed, before the game acts on them.
     */
    @SubscribeEvent
    public void onKey(net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent event) {
        if (!struggling) {
            return;
        }
        GameSettings s = mc.gameSettings;
        for (KeyBinding key : new KeyBinding[]{s.keyBindDrop, s.keyBindSwapHands, s.keyBindInventory, s.keyBindPickBlock}) {
            while (key.isPressed()) {
                // Swallowed.
            }
        }
        for (KeyBinding key : s.keyBindsHotbar) {
            while (key.isPressed()) {
                // Swallowed.
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onMovement(InputUpdateEvent event) {
        if (struggling) {
            event.getMovementInput().moveForward = 0F;
            event.getMovementInput().moveStrafe = 0F;
            event.getMovementInput().jump = false;
            event.getMovementInput().sneak = false;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onMouse(MouseEvent event) {
        if (struggling && (event.getButton() >= 0 || event.getDwheel() != 0)) {
            event.setCanceled(true);
        }
    }

    /** Your eyes on it: between your hand and his, at the height of your chest. */
    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !struggling || mc.player == null || mc.world == null) {
            return;
        }
        Entity e = mc.world.getEntityByID(magusId);
        if (!(e instanceof EntityMagus)) {
            return;
        }
        Vec3d eye = mc.player.getPositionEyes(event.renderTickTime);
        Vec3d his = e.getPositionVector().add(0D, 1.45D, 0D);
        Vec3d mid = eye.add(his.subtract(eye).scale(0.45D)).subtract(0D, 0.3D, 0D);
        Vec3d d = mid.subtract(eye);
        float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
        mc.player.rotationYaw = mc.player.prevRotationYaw = yaw;
        mc.player.rotationYawHead = mc.player.prevRotationYawHead = yaw;
        mc.player.rotationPitch = mc.player.prevRotationPitch = pitch;
    }

    /**
     * Close, in front of you: the gauntlet, his lavender hand on it from across, yours from
     * below; shaking with the pull, coming toward you as you win it and going as you lose it.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onHand(RenderSpecificHandEvent event) {
        if (!struggling) {
            return;
        }
        event.setCanceled(true);
        if (event.getHand() != EnumHand.MAIN_HAND || mc.player == null) {
            return;
        }
        Entity e = mc.world.getEntityByID(magusId);
        ItemStack gauntlet = e instanceof EntityMagus ? ((EntityMagus) e).gauntlet() : ItemStack.EMPTY;
        if (gauntlet.isEmpty()) {
            gauntlet = marked();
        }
        float pt = event.getPartialTicks();
        float t = tick + pt;
        float shown = prevShownBar + (shownBar - prevShownBar) * pt;
        float shake = 0.6F + 0.8F * (1F - Math.abs(shown - 0.5F) * 2F);
        float sx = MathHelper.sin(t * 2.9F) * 0.012F * shake, sy = MathHelper.sin(t * 3.7F + 1F) * 0.01F * shake;
        float pull = (shown - 0.5F) * 0.5F + 0.05F * yank;

        GlStateManager.pushMatrix();
        GlStateManager.enableRescaleNormal();
        RenderHelper.enableStandardItemLighting();
        GlStateManager.translate(sx, -0.2F + sy, -0.95F + pull);

        if (!gauntlet.isEmpty()) {
            GlStateManager.pushMatrix();
            // Upright between the two hands, the back of it to you, the stones toward him.
            GlStateManager.rotate(15F * (shown - 0.5F), 0F, 0F, 1F);
            GlStateManager.rotate(180F, 0F, 1F, 0F);
            GlStateManager.translate(0F, -0.05F, 0F);
            GlStateManager.scale(0.95F, 0.95F, 0.95F);
            mc.getRenderItem().renderItem(gauntlet, ItemCameraTransforms.TransformType.THIRD_PERSON_RIGHT_HAND);
            GlStateManager.popMatrix();
        }

        // His hand, from across: a lavender forearm and its silver band, gripping the top of it.
        mc.getTextureManager().bindTexture(MAGUS_SKIN);
        GlStateManager.pushMatrix();
        GlStateManager.translate(0.16F, 0.32F, -0.35F);
        GlStateManager.rotate(-155F, 1F, 0F, 0F);
        GlStateManager.rotate(-25F, 0F, 0F, 1F);
        GlStateManager.scale(1.2F, 1.2F, 1.2F);
        magusModel.lFore.rotateAngleX = 0F;
        magusModel.lFore.rotateAngleY = 0F;
        magusModel.lFore.rotateAngleZ = 0F;
        float rx = magusModel.lFore.rotationPointX, ry = magusModel.lFore.rotationPointY;
        magusModel.lFore.rotationPointX = 0F;
        magusModel.lFore.rotationPointY = 0F;
        magusModel.lFore.render(0.0625F);
        magusModel.lFore.rotationPointX = rx;
        magusModel.lFore.rotationPointY = ry;
        GlStateManager.popMatrix();

        // Yours, from below, on the other side of it.
        AbstractClientPlayer player = mc.player;
        boolean slim = "slim".equals(player.getSkinType());
        if (armModel == null) {
            armModel = new ModelPlayer(0F, false);
            slimArmModel = new ModelPlayer(0F, true);
        }
        ModelPlayer arm = slim ? slimArmModel : armModel;
        mc.getTextureManager().bindTexture(player.getLocationSkin());
        GlStateManager.pushMatrix();
        GlStateManager.translate(-0.1F, -0.42F, 0.35F);
        GlStateManager.rotate(-70F, 1F, 0F, 0F);
        GlStateManager.rotate(20F, 0F, 0F, 1F);
        GlStateManager.scale(1.2F, 1.2F, 1.2F);
        arm.swingProgress = 0F;
        arm.isSneak = false;
        arm.bipedRightArm.rotateAngleX = 0F;
        arm.bipedRightArm.rotateAngleY = 0F;
        arm.bipedRightArm.rotateAngleZ = 0F;
        arm.bipedRightArm.rotationPointX = 0F;
        arm.bipedRightArm.rotationPointY = 0F;
        arm.bipedRightArm.rotationPointZ = 0F;
        arm.bipedRightArm.render(0.0625F);
        arm.bipedRightArmwear.rotateAngleX = 0F;
        arm.bipedRightArmwear.rotationPointX = 0F;
        arm.bipedRightArmwear.rotationPointY = 0F;
        arm.bipedRightArmwear.rotationPointZ = 0F;
        arm.bipedRightArmwear.render(0.0625F);
        GlStateManager.popMatrix();

        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.popMatrix();
    }

    /** The gauntlet that is being fought over, on you. */
    private ItemStack marked() {
        for (ItemStack s : mc.player.inventory.mainInventory) {
            if (!s.isEmpty() && s.getItem() instanceof ItemInfinityGauntlet && s.hasTagCompound()
                    && s.getTagCompound().getBoolean("EternityFightGauntlet")) {
                return s;
            }
        }
        return ItemStack.EMPTY;
    }

    // ------------------------------------------------------------------ on the screen

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }
        ScaledResolution res = event.getResolution();
        int w = res.getScaledWidth(), h = res.getScaledHeight();
        FontRenderer font = mc.fontRenderer;
        float pt = event.getPartialTicks();
        if (struggling) {
            float shown = prevShownBar + (shownBar - prevShownBar) * pt;
            int width = 220, left = w / 2 - width / 2, y = h - 132;
            Gui.drawRect(left - 4, y - 16, left + width + 4, y + 16, 0xC0080810);
            int split = left + (int) (width * shown);
            // Yours from the left, gold; his from the right, violet.
            Gui.drawRect(left, y, split, y + 8, 0xFFE0B84A);
            Gui.drawRect(split, y, left + width, y + 8, 0xFF8A4FE0);
            Gui.drawRect(split - 1, y - 3, split + 2, y + 11, 0xFFFFFFFF);
            float pulse = 0.6F + 0.4F * MathHelper.sin((tick + pt) * 0.6F);
            String prompt = "PULL!  Jump, again and again";
            int a = (int) (255 * pulse) << 24;
            font.drawStringWithShadow(prompt, w / 2F - font.getStringWidth(prompt) / 2F, y - 13, 0x00FFFFFF | a);
        } else if (tick - endedAt < 40) {
            String text = won ? "You have it." : "He has it.";
            float fade = 1F - (tick - endedAt + pt) / 40F;
            int a = Math.max(4, (int) (255 * fade)) << 24;
            font.drawStringWithShadow(text, w / 2F - font.getStringWidth(text) / 2F, h - 140, (won ? 0x00E0B84A : 0x00B58AF0) | a);
        }
        if (whiteAt >= 0) {
            // In at once, held a moment, then out slowly, the last of it lingering.
            float t = tick - whiteAt + pt;
            float in = MathHelper.clamp(t / 2F, 0F, 1F);
            float out = 1F - MathHelper.clamp((t - 8F) / whiteFor, 0F, 1F);
            float alpha = in * out * out;
            if (alpha > 0.003F) {
                GlStateManager.disableDepth();
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                Gui.drawRect(0, 0, w, h, (int) (alpha * 255F) << 24 | 0xFFF8F0);
                GlStateManager.enableDepth();
            }
        }
    }
}
