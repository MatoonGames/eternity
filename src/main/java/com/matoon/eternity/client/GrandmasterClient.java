package com.matoon.eternity.client;

import com.matoon.eternity.client.render.GrandmasterBoard;
import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.network.MsgGrandmaster;
import com.matoon.eternity.network.MsgGrandmasterAct;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The Grandmaster's board from where you stand: it keeps {@link GrandmasterBoard} up to date,
 * draws it, and turns your hand on a piece or a square into a move. The square is worked out
 * here, from the pieces as they are drawn: look at a piece and you lay your hand on it, even
 * though the floor behind it is the block your crosshair is on.
 */
@SideOnly(Side.CLIENT)
public final class GrandmasterClient {

    public static final GrandmasterClient INSTANCE = new GrandmasterClient();

    private static final double REACH = 7D;

    private final Minecraft mc = Minecraft.getMinecraft();
    private long lastPick;

    private GrandmasterClient() {
    }

    public void handle(MsgGrandmaster m) {
        GrandmasterBoard board = GrandmasterBoard.INSTANCE;
        if (m.type == MsgGrandmaster.SYNC && m.tag != null) {
            board.sync(m.tag);
        } else if (m.type == MsgGrandmaster.LINE && m.tag != null) {
            com.matoon.eternity.client.gui.SpeakerPanel.INSTANCE.say(m.tag.getByte("s"), m.value, m.tag.getString("t"));
        } else if (m.type == MsgGrandmaster.CUTSCENE) {
            RulesCutscene.play();
        } else if (m.type == MsgGrandmaster.CLEAR) {
            board.clear();
        } else {
            board.effect(m);
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || mc.world == null || mc.player == null || mc.isGamePaused()) {
            return;
        }
        GrandmasterBoard board = GrandmasterBoard.INSTANCE;
        board.tick();
        board.hovered = board.active && board.built && board.isMine() && !board.demo && !Cutscene.INSTANCE.isActive() ? pick() : -1;
    }

    private int pick() {
        Entity view = mc.player;
        Vec3d eye = view.getPositionEyes(1F);
        if (!GrandmasterBoard.INSTANCE.near(eye, 40D)) {
            return -1;
        }
        return GrandmasterBoard.INSTANCE.pick(eye, view.getLook(1F), REACH);
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Entity view = mc.getRenderViewEntity();
        if (mc.world == null || view == null) {
            return;
        }
        float pt = event.getPartialTicks();
        Vec3d camera = new Vec3d(view.lastTickPosX + (view.posX - view.lastTickPosX) * pt,
                view.lastTickPosY + (view.posY - view.lastTickPosY) * pt,
                view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * pt);
        GrandmasterBoard.INSTANCE.render(camera, pt);
    }

    // ------------------------------------------------------------------ laying a hand on the board

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getWorld().isRemote || event.getHand() != EnumHand.MAIN_HAND) {
            return;
        }
        GrandmasterBoard board = GrandmasterBoard.INSTANCE;
        if (!board.active || board.isPedestal(event.getPos())) {
            return;
        }
        if (layHand()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
        if (event.getHand() == EnumHand.MAIN_HAND) {
            layHand();
        }
    }

    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getWorld().isRemote && event.getHand() == EnumHand.MAIN_HAND && GrandmasterBoard.INSTANCE.active && layHand()) {
            event.setCanceled(true);
        }
    }

    /** Sends the square the player is reaching for, if it is theirs to play. */
    private boolean layHand() {
        GrandmasterBoard board = GrandmasterBoard.INSTANCE;
        if (!board.active || !board.built || !board.isMine() || mc.world == null || board.demo || Cutscene.INSTANCE.isActive()) {
            return false;
        }
        int square = pick();
        if (square < 0) {
            return false;
        }
        long now = mc.world.getTotalWorldTime();
        if (now - lastPick >= 4) {
            lastPick = now;
            EternityNetwork.sendToServer(new MsgGrandmasterAct(square));
            mc.player.swingArm(EnumHand.MAIN_HAND);
        }
        return true;
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        mc.addScheduledTask(GrandmasterBoard.INSTANCE::clear);
    }
}
