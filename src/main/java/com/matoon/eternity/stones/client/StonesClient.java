package com.matoon.eternity.stones.client;

import anvil.infinity.abilities.AbilitySnap;
import com.matoon.eternity.Eternity;
import com.matoon.eternity.EternityConfig;
import com.matoon.eternity.EternitySounds;
import com.matoon.eternity.stones.network.MsgMindControlAura;
import com.matoon.eternity.stones.network.MsgMindControlEntries;
import com.matoon.eternity.stones.network.MsgMindControlLock;
import com.matoon.eternity.stones.network.MsgMindControlMenu;
import com.matoon.eternity.stones.network.MsgMindControlResistance;
import com.matoon.eternity.stones.network.MsgMindControlResult;
import com.matoon.eternity.stones.network.MsgPowerUp;
import com.matoon.eternity.stones.network.MsgSnapEffect;
import com.matoon.eternity.stones.network.MsgScars;
import com.matoon.eternity.stones.network.MsgTimeStoneCharge;
import com.matoon.eternity.stones.network.StoneMessage;
import com.matoon.eternity.stones.time.TimeStoneCharge;
import lucraft.mods.lucraftcore.superpowers.render.RenderSuperpowerLayerEvent;
import lucraft.mods.lucraftcore.superpowers.render.SuperpowerRenderer;
import lucraft.mods.lucraftcore.util.abilitybar.AbilityBarHandler;
import lucraft.mods.lucraftcore.util.abilitybar.IAbilityBarEntry;
import lucraft.mods.lucraftcore.util.helper.LCRenderHelper;
import lucraft.mods.lucraftcore.util.helper.PlayerHelper;
import lucraft.mods.lucraftcore.util.render.ModelCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The stones' powers on the client: the full gauntlet's power-up (its sound, its two-frame screen, the snap filling up
 * on the ability bar, and a glow on whoever is powering up), the snap's flash (the burn it leaves is {@link SnapMark}'s), and
 * the halo around anything under the Mind Stone's control.
 */
public final class StonesClient {

    public static final StonesClient INSTANCE = new StonesClient();

    private static final ResourceLocation POWER_UP_1 = texture("power_up_1"), POWER_UP_2 = texture("power_up_2");
    private static final ResourceLocation POWER_UP_SKIN_1 = texture("power_up_skin_1"), POWER_UP_SKIN_2 = texture("power_up_skin_2");

    /** Ticks each power-up frame shows before the other. */
    private static final int CYCLE = 4;
    /** The snap's flash: in over a fifth of a second, out over five. */
    private static final int FLASH_IN = 4, FLASH_OUT = 100;

    /** Everyone powering up, to how far along they are. */
    private final Map<UUID, Integer> poweringUp = new ConcurrentHashMap<>();
    private final Set<UUID> controlled = new HashSet<>();
    private ISound powerUpSound;
    private int flash = -1;

    private StonesClient() {
    }

    private static ResourceLocation texture(String name) {
        return new ResourceLocation(Eternity.MODID, "textures/stones/" + name + ".png");
    }

    public void register() {
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(SnapMark.INSTANCE);
        MinecraftForge.EVENT_BUS.register(WornGauntlet.INSTANCE);
        MinecraftForge.EVENT_BUS.register(MindControlLock.INSTANCE);
        AbilityBarHandler.registerProvider(new MindControlBar());
    }

    /** On the client thread. */
    public void handle(StoneMessage message) {
        Minecraft mc = Minecraft.getMinecraft();
        if (message instanceof MsgPowerUp) {
            MsgPowerUp powerUp = (MsgPowerUp) message;
            if (powerUp.start) {
                startPowerUp(mc, powerUp.holder);
            } else {
                stopPowerUp(mc, powerUp.holder);
            }
        } else if (message instanceof MsgSnapEffect) {
            SoundEvent[] sounds = {EternitySounds.STONES_SNAP_1, EternitySounds.STONES_SNAP_2, EternitySounds.STONES_SNAP_3};
            mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(sounds[Math.max(0, Math.min(((MsgSnapEffect) message).sound, 2))], 1F));
            flash = 0;
        } else if (message instanceof MsgScars) {
            SnapMark.INSTANCE.update((MsgScars) message);
        } else if (message instanceof MsgTimeStoneCharge) {
            TimeStoneCharge.setClientCharge(((MsgTimeStoneCharge) message).charge);
        } else if (message instanceof MsgMindControlAura) {
            MsgMindControlAura aura = (MsgMindControlAura) message;
            if (aura.enabled) {
                controlled.add(aura.entity);
            } else {
                controlled.remove(aura.entity);
            }
        } else if (message instanceof MsgMindControlEntries) {
            MindControlBar.set(((MsgMindControlEntries) message).entries);
        } else if (message instanceof MsgMindControlLock) {
            MsgMindControlLock lock = (MsgMindControlLock) message;
            MindControlLock.INSTANCE.setLocked(lock.locked, lock.remainingTicks);
        } else if (message instanceof MsgMindControlResistance) {
            MindControlLock.INSTANCE.startResistance((MsgMindControlResistance) message);
        } else if (message instanceof MsgMindControlResult) {
            MindControlLock.INSTANCE.finishResistance();
        } else if (message instanceof MsgMindControlMenu) {
            mc.displayGuiScreen(new GuiMindControl((MsgMindControlMenu) message));
        }
    }

    // ------------------------------------------------------------------ the power-up

    /** Everyone near hears it; the holder alone sees the screen, and can have it cut short. */
    private void startPowerUp(Minecraft mc, UUID holder) {
        poweringUp.put(holder, 0);
        PositionedSoundRecord sound = PositionedSoundRecord.getMasterRecord(EternitySounds.STONES_POWER_UP, 1F);
        if (mc.player != null && holder.equals(mc.player.getUniqueID())) {
            stopPowerUpSound(mc);
            powerUpSound = sound;
        }
        mc.getSoundHandler().playSound(sound);
    }

    private void stopPowerUp(Minecraft mc, UUID holder) {
        poweringUp.remove(holder);
        if (mc.player != null && holder.equals(mc.player.getUniqueID())) {
            stopPowerUpSound(mc);
        }
    }

    private void stopPowerUpSound(Minecraft mc) {
        if (powerUpSound != null) {
            mc.getSoundHandler().stopSound(powerUpSound);
            powerUpSound = null;
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null) {
            poweringUp.clear();
            controlled.clear();
            flash = -1;
            return;
        }
        for (UUID holder : poweringUp.keySet()) {
            int ticks = poweringUp.get(holder) + 1;
            if (ticks >= MsgPowerUp.DURATION) {
                stopPowerUp(mc, holder);
            } else {
                poweringUp.put(holder, ticks);
            }
        }
        if (flash >= 0 && ++flash >= FLASH_IN + FLASH_OUT) {
            flash = -1;
        }
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) {
            return;
        }
        ScaledResolution res = event.getResolution();
        Integer ticks = poweringUp.get(mc.player.getUniqueID());
        if (ticks != null) {
            // With flashing reduced, the screen holds on one frame.
            boolean second = !EternityConfig.client.reduceFlashing && (ticks / CYCLE) % 2 == 1;
            mc.getTextureManager().bindTexture(second ? POWER_UP_2 : POWER_UP_1);
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GlStateManager.color(1F, 1F, 1F, 1F);
            Gui.drawModalRectWithCustomSizedTexture(0, 0, 0, 0, res.getScaledWidth(), res.getScaledHeight(), res.getScaledWidth(), res.getScaledHeight());
            GlStateManager.disableBlend();
            drawSnapProgress(res, ticks / (float) MsgPowerUp.DURATION);
        }
        if (flash >= 0) {
            float alpha = flash < FLASH_IN ? flash / (float) FLASH_IN : 1F - (flash - FLASH_IN) / (float) FLASH_OUT;
            if (EternityConfig.client.reduceFlashing) {
                alpha *= 0.35F;
            }
            if (alpha > 0F) {
                int a = (int) (Math.min(1F, alpha) * 255F);
                Gui.drawRect(0, 0, res.getScaledWidth(), res.getScaledHeight(), a << 24 | 0xFFFFFF);
            }
        }
    }

    /** The snap's slot on the ability bar filling, grey, as the power-up runs. */
    private static void drawSnapProgress(ScaledResolution res, float progress) {
        List<IAbilityBarEntry> displayed = AbilityBarHandler.getCurrentDisplayedEntries(AbilityBarHandler.getActiveEntries());
        int index = -1;
        for (int i = 0; i < displayed.size(); i++) {
            if (displayed.get(i) instanceof AbilitySnap) {
                index = i;
                break;
            }
        }
        int fill = (int) (22 * progress);
        if (index < 0 || fill <= 0) {
            return;
        }
        int x = res.getScaledWidth() - 22 - 50 * index, bottom = res.getScaledHeight() / 2 - 11 + 22;
        Gui.drawRect(x, bottom - fill, x + 22, bottom, 0x99808080);
        GlStateManager.color(1F, 1F, 1F, 1F);
    }

    // ------------------------------------------------------------------ on the players themselves

    @SubscribeEvent
    public void onRenderSuperpowerLayer(RenderSuperpowerLayerEvent event) {
        EntityPlayer player = event.getPlayer();
        Integer ticks = poweringUp.get(player.getUniqueID());
        if (ticks != null) {
            boolean second = !EternityConfig.client.reduceFlashing && (ticks / CYCLE) % 2 == 1;
            renderSkin(event, second ? POWER_UP_SKIN_2 : POWER_UP_SKIN_1);
        }
    }

    /** A texture over a player's skin, just outside it, full-bright, as Lucraft draws its own skin effects. */
    private static void renderSkin(RenderSuperpowerLayerEvent event, ResourceLocation texture) {
        EntityPlayer player = event.getPlayer();
        boolean smallArms = PlayerHelper.hasSmallArms(player);
        ModelPlayer model = (ModelPlayer) ModelCache.getOrStoreModel("eternity_stones_skin_" + smallArms, () -> new ModelPlayer(0.5F, smallArms));
        GlStateManager.pushMatrix();
        GlStateManager.color(1F, 1F, 1F, 1F);
        model.setModelAttributes(event.getRenderPlayer().getMainModel());
        Minecraft.getMinecraft().renderEngine.bindTexture(texture);
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        LCRenderHelper.setLightmapTextureCoords(240F, 240F);
        SuperpowerRenderer.overrideSkin = false;
        model.render(player, event.getLimbSwing(), event.getLimbSwingAmount(), event.getAgeInTicks(), event.getNetHeadYaw(),
                event.getHeadPitch(), event.getScale());
        SuperpowerRenderer.overrideSkin = true;
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        LCRenderHelper.restoreLightmapTextureCoords();
        GlStateManager.popMatrix();
    }

    /** Three slow rings of violet and blue turning around anything under the Mind Stone's control. */
    @SubscribeEvent
    public void onRenderLiving(RenderLivingEvent.Post<EntityLivingBase> event) {
        EntityLivingBase entity = event.getEntity();
        if (!controlled.contains(entity.getUniqueID())) {
            return;
        }
        double height = entity.height + 0.18D, phase = (entity.ticksExisted + event.getPartialRenderTick()) * 0.18D;
        GlStateManager.pushMatrix();
        GlStateManager.translate(event.getX(), event.getY(), event.getZ());
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GlStateManager.depthMask(false);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        for (int band = 0; band < 3; band++) {
            double y = 0.22D + band * (height - 0.35D) / 2D, spin = phase + band * 2.09D;
            float red = band == 1 ? 0.18F : 0.55F, green = 0.12F + band * 0.15F;
            for (int i = 0; i < 24; i++) {
                for (int end = 0; end < 2; end++) {
                    int index = i + end;
                    double angle = spin + index * Math.PI * 2D / 24D, radius = 0.48D + Math.sin(phase * 1.7D + index) * 0.035D;
                    buffer.pos(Math.cos(angle) * radius, y + Math.sin(angle * 3D + phase) * 0.055D, Math.sin(angle) * radius)
                            .color(red, green, 1F, 0.85F).endVertex();
                }
            }
        }
        tessellator.draw();
        GlStateManager.depthMask(true);
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }
}
