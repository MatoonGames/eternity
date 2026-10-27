package com.matoon.eternity.dev;

import com.matoon.eternity.Eternity;
import com.matoon.eternity.world.Tamarata;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.io.File;

/** TEMPORARY: the jump key on Tamarata, whole and broken. Only runs when run/gravity_verify.txt exists. */
@Mod.EventBusSubscriber(modid = Eternity.MODID, value = Side.CLIENT)
public final class GravityVerify {

    private static Boolean active;
    private static boolean launched;
    private static int t = -1;
    private static double base, peak;
    private static int jumps;
    private static boolean wasGround;

    private GravityVerify() {
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (active == null) {
            active = new File(mc.gameDir, "gravity_verify.txt").exists();
            mc.gameSettings.pauseOnLostFocus = false;
        }
        if (!active) {
            return;
        }
        if (mc.world == null || mc.player == null) {
            if (!launched && mc.currentScreen instanceof GuiMainMenu) {
                launched = true;
                WorldSettings settings = new WorldSettings(4321L, GameType.CREATIVE, false, false, WorldType.FLAT);
                settings.enableCommands();
                mc.launchIntegratedServer("GravityVerify", "GravityVerify", settings);
            }
            return;
        }
        if (mc.currentScreen instanceof GuiIngameMenu) {
            mc.displayGuiScreen(null);
        }
        t++;
        KeyBinding jump = mc.gameSettings.keyBindJump;
        String pad = Tamarata.LANDING_X + " " + (Tamarata.LANDING_Y + 1) + " " + (Tamarata.LANDING_Z + 4);
        switch (t) {
            case 40:
                chat(mc, "/gamerule doDaylightCycle false");
                chat(mc, "/tpdimension @p " + Tamarata.dimension() + " " + pad);
                break;
            case 300:
                chat(mc, "/gamemode 0");
                break;
            case 340:
                log(String.format("whole, survival: standing, on ground %b", mc.player.onGround));
                start(mc);
                KeyBinding.setKeyBindState(jump.getKeyCode(), true);
                break;
            case 342:
                KeyBinding.setKeyBindState(jump.getKeyCode(), false);
                break;
            case 380:
                log(String.format("whole, survival: one tap rose %.2f blocks, back on ground %b", peak - base, mc.player.onGround));
                start(mc);
                KeyBinding.setKeyBindState(jump.getKeyCode(), true);
                break;
            case 480:
                KeyBinding.setKeyBindState(jump.getKeyCode(), false);
                log(String.format("whole, survival: held 5 s, %d jumps, peak %.2f", jumps, peak - base));
                chat(mc, "/gamemode 1");
                break;
            case 520:
                // Double tap in creative: start flying.
                log(String.format("whole, creative: flying before double tap %b", mc.player.capabilities.isFlying));
                KeyBinding.setKeyBindState(jump.getKeyCode(), true);
                break;
            case 521:
                KeyBinding.setKeyBindState(jump.getKeyCode(), false);
                break;
            case 523:
                KeyBinding.setKeyBindState(jump.getKeyCode(), true);
                break;
            case 524:
                KeyBinding.setKeyBindState(jump.getKeyCode(), false);
                break;
            case 540:
                log(String.format("whole, creative: flying after double tap %b", mc.player.capabilities.isFlying));
                chat(mc, "/eternity champion strand");
                break;
            case 640:
                chat(mc, "/gamemode 0");
                chat(mc, "/effect @p resistance 60 255 true");
                chat(mc, "/tp @p " + pad);
                break;
            case 700:
                start(mc);
                KeyBinding.setKeyBindState(jump.getKeyCode(), true);
                break;
            case 740:
                KeyBinding.setKeyBindState(jump.getKeyCode(), false);
                log(String.format("broken, survival: holding jump 2 s drifted up %.2f blocks, on ground %b", mc.player.posY - base, mc.player.onGround));
                break;
            case 760:
                mc.shutdown();
                break;
            default:
                break;
        }
        peak = Math.max(peak, mc.player.posY);
        if (wasGround && !mc.player.onGround && mc.player.motionY > 0.3D) {
            jumps++;
        }
        wasGround = mc.player.onGround;
    }

    private static void start(Minecraft mc) {
        base = mc.player.posY;
        peak = base;
        jumps = 0;
    }

    private static void chat(Minecraft mc, String s) {
        mc.player.sendChatMessage(s);
    }

    private static void log(String s) {
        System.out.println("GRVERIFY " + s);
    }
}
