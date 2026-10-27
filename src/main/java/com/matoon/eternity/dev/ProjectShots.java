package com.matoon.eternity.dev;

import com.matoon.eternity.Eternity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.ChunkRenderContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.client.IRenderHandler;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * DEV ONLY: drives the client for promotional screenshots. Only runs when run/shots/ENABLED exists.
 * Commands are appended to run/shots/queue.txt, one per line, and run in order; each one is
 * echoed to run/shots/log.txt when done. Shots render at 2x and are box-filtered down to the
 * output size; mattes render the frame twice, over black and over white, and recover alpha
 * from the difference, so additive glows keep their soft edges.
 *
 * <pre>
 * world NAME [void|flat|normal] [seed]   create or load a creative world (from the main menu)
 * leave                                  back to the main menu
 * chat TEXT                              send a chat line or command
 * wait TICKS
 * cam X Y Z YAW PITCH                    lock the camera (eye position)
 * orbit SEL DIST YAW PITCH [H] [follow]  lock the camera on an entity, looking at it from YAW/PITCH;
 *                                        H is the height fraction looked at (0.5); SEL is a type or #id
 * orbitr ...                             as orbit, YAW relative to the entity's facing (0 = in front)
 * shift RIGHT UP FWD                     nudge the locked camera in its own frame
 * free                                   unlock the camera
 * anchor X Y Z | anchor off              keep the player at X Y Z for the game while the camera renders elsewhere
 * fov DEGREES
 * use [sneak]                            right-click with the main hand, sneaking or not
 * click                                  right-click the block in view
 * look YAW PITCH                         turn the player (not the camera)
 * press KEY TICKS                        hold a key for the game (forward, jump, sneak, attack, use, or the end of a
 *                                        key binding's name, e.g. armor_slam)
 * view N                                 vanilla's view (0 first person, 1 behind, 2 in front), camera free only
 * harm on|off                            let blows reach the player (still healed every tick), to test what reacts to them
 * ents [RADIUS]                          log nearby entities
 * shot NAME [+gui]                       screenshot to run/shots/out/NAME.png; +gui keeps the HUD and hand
 * matte NAME [PAD] [-client...] [+terrain] [@id]  transparent screenshot (NAME.png full frame, NAME_crop.png
 *                                        tight); terrain is hidden unless +terrain, and -nexus etc. silences
 *                                        that client's world renderer (NexusClient) for the matte
 * size W H                               output size (default 1920 1080)
 * srv call SEL METHOD [ARGS]             call a method (or read/set a field) on the server copy of an entity
 * srv static a.b.Class.FIELD.method [ARGS]  the same on a static or singleton, on the server thread;
 *                                        ARGS: numbers, true/false, enum names, 'player', #id, strings
 * quit
 * </pre>
 */
@Mod.EventBusSubscriber(modid = Eternity.MODID, value = Side.CLIENT)
public final class ProjectShots {

    private static final IRenderHandler NOTHING = new IRenderHandler() {
        @Override
        public void render(float partialTicks, WorldClient world, Minecraft mc) {
        }
    };
    private static final ChunkRenderContainer NO_TERRAIN = new ChunkRenderContainer() {
        @Override
        public void renderChunkLayer(BlockRenderLayer layer) {
        }
    };
    /** Randoms that renderers draw from without a seed; reseeded before each matte pass so both agree. */
    private static final String[][] RANDOMS = {
            {"com.matoon.eternity.client.render.RenderEternity", "RAYS"},
            {"com.matoon.eternity.client.render.ModelTribunal", "RANDOM"},
            {"com.matoon.eternity.client.render.Cosmic", "RANDOM"},
            {"com.matoon.eternity.client.render.ScreenFx", "CRACKS"},
    };

    private static Boolean active;
    private static File dir;
    private static final Deque<String> queue = new ArrayDeque<>();
    private static int waiting;
    private static boolean launched;

    // Camera lock.
    private static boolean locked;
    private static double camX, camY, camZ;
    private static float camYaw, camPitch;
    private static Entity target;
    private static boolean follow, relative;
    private static double orbitDist, orbitH = 0.5D;
    private static float orbitYaw, orbitPitch;
    private static double shiftR, shiftU, shiftF;
    private static float fov = -1F;
    private static boolean anchored;
    private static double anchorX, anchorY, anchorZ;

    // Capture.
    private static String pending;
    private static boolean pendingMatte;
    private static int pendingPad = 24;
    private static final List<String> pendingHide = new ArrayList<>();
    private static boolean pendingTerrain;
    private static int pendingOnly = -1;
    private static boolean skyCleared;
    private static int captureDelay;
    private static int outW = 1920, outH = 1080;
    private static int savedW, savedH;
    private static boolean studio;
    /** Whether blows reach the camera player (it is still healed to full every tick). */
    private static boolean harm;
    /** Keys held down for the game, by name, and for how many more ticks. */
    private static final java.util.Map<String, Integer> held = new java.util.HashMap<>();
    private static float studioShade;

    private ProjectShots() {
    }

    private static boolean active() {
        if (active == null) {
            dir = new File(Minecraft.getMinecraft().gameDir, "shots");
            active = new File(dir, "ENABLED").isFile();
            if (active) {
                new File(dir, "out").mkdirs();
                Minecraft.getMinecraft().gameSettings.pauseOnLostFocus = false;
            }
        }
        return active;
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (!active()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (event.phase == TickEvent.Phase.START) {
            applyCamera(mc, 1F, true);
            holdKeys(mc);
            return;
        }
        if (locked && mc.currentScreen instanceof GuiIngameMenu) {
            mc.displayGuiScreen(null);
        }
        if (locked && mc.world != null && mc.player != null && mc.currentScreen != null && !(mc.currentScreen instanceof GuiMainMenu)
                && !(mc.currentScreen instanceof com.matoon.eternity.client.gui.GuiDestroyerArmor)) {
            // Dialogue screens and the like would block the view.
            log("closed screen " + mc.currentScreen.getClass().getSimpleName());
            mc.displayGuiScreen(null);
        }
        syncServerPlayer(mc);
        // Vanilla only lets the player steer while it is the view entity; the spy camera takes that, so steer for it.
        if (spyTarget != null && mc.player != null) {
            mc.player.moveForward = mc.player.movementInput.moveForward;
            mc.player.moveStrafing = mc.player.movementInput.moveStrafe;
        }
        readQueue();
        if (pending != null) {
            return;
        }
        if (waiting > 0) {
            waiting--;
            return;
        }
        while (!queue.isEmpty() && waiting == 0 && pending == null) {
            String line = queue.poll();
            try {
                if (!run(mc, line)) {
                    queue.push(line);
                    return;
                }
                log("done " + line);
            } catch (Exception e) {
                log("FAIL " + line + " : " + e);
                e.printStackTrace();
            }
        }
    }

    /** Runs one command; false means not yet possible, try again next tick. */
    private static boolean run(Minecraft mc, String line) throws Exception {
        String[] a = line.trim().split("\\s+", 2);
        String cmd = a[0].toLowerCase(Locale.ROOT);
        String rest = a.length > 1 ? a[1] : "";
        String[] p = rest.isEmpty() ? new String[0] : rest.split("\\s+");
        switch (cmd) {
            case "":
            case "#":
                return true;
            case "world": {
                if (mc.world != null) {
                    return true;
                }
                if (!(mc.currentScreen instanceof GuiMainMenu)) {
                    return false;
                }
                if (launched) {
                    return false;
                }
                launched = true;
                String name = p[0];
                if (mc.getSaveLoader().canLoadWorld(name)) {
                    mc.launchIntegratedServer(name, name, null);
                } else {
                    String kind = p.length > 1 ? p[1] : "void";
                    long seed = p.length > 2 ? Long.parseLong(p[2]) : 1234L;
                    WorldType type = "normal".equals(kind) ? WorldType.DEFAULT : WorldType.FLAT;
                    WorldSettings settings = new WorldSettings(seed, GameType.CREATIVE, false, false, type);
                    if ("void".equals(kind)) {
                        settings.setGeneratorOptions("3;minecraft:air;127");
                    }
                    settings.enableCommands();
                    mc.launchIntegratedServer(name, name, settings);
                }
                waiting = 100;
                return true;
            }
            case "leave":
                if (mc.world != null) {
                    locked = false;
                    mc.world.sendQuittingDisconnectingPacket();
                    mc.loadWorld(null);
                    mc.displayGuiScreen(new GuiMainMenu());
                }
                launched = false;
                return true;
            case "chat":
                if (mc.player == null) {
                    return false;
                }
                mc.player.sendChatMessage(rest);
                return true;
            case "wait":
                waiting = Integer.parseInt(p[0]);
                return true;
            case "cam":
                target = null;
                locked = true;
                camX = Double.parseDouble(p[0]);
                camY = Double.parseDouble(p[1]);
                camZ = Double.parseDouble(p[2]);
                camYaw = Float.parseFloat(p[3]);
                camPitch = Float.parseFloat(p[4]);
                shiftR = shiftU = shiftF = 0D;
                return true;
            case "orbit":
            case "orbitr": {
                if (mc.world == null) {
                    return false;
                }
                Entity e = select(mc, p[0]);
                if (e == null) {
                    log("no entity " + p[0]);
                    return true;
                }
                target = e;
                relative = "orbitr".equals(cmd);
                orbitDist = Double.parseDouble(p[1]);
                orbitYaw = Float.parseFloat(p[2]);
                orbitPitch = Float.parseFloat(p[3]);
                orbitH = p.length > 4 ? Double.parseDouble(p[4]) : 0.5D;
                follow = p.length > 5 && "follow".equals(p[5]);
                shiftR = shiftU = shiftF = 0D;
                locked = true;
                orbit(1F);
                log(String.format("orbit %s cam %.1f %.1f %.1f yaw %.1f pitch %.1f", describe(e), camX, camY, camZ, camYaw, camPitch));
                return true;
            }
            case "shift":
                shiftR = Double.parseDouble(p[0]);
                shiftU = Double.parseDouble(p[1]);
                shiftF = Double.parseDouble(p[2]);
                return true;
            case "anchor":
                anchored = !"off".equals(p[0]);
                if (anchored) {
                    anchorX = Double.parseDouble(p[0]);
                    anchorY = Double.parseDouble(p[1]);
                    anchorZ = Double.parseDouble(p[2]);
                }
                return true;
            case "free":
                locked = false;
                target = null;
                return true;
            case "fov":
                fov = Float.parseFloat(p[0]);
                return true;
            case "use": {
                // Right-click with whatever is in the main hand, sneaking if asked, the way a player would.
                if (mc.world == null || mc.player == null) {
                    return false;
                }
                boolean sneak = p.length > 0 && "sneak".equals(p[0]);
                mc.player.setSneaking(sneak);
                mc.player.connection.sendPacket(new net.minecraft.network.play.client.CPacketEntityAction(mc.player, sneak
                        ? net.minecraft.network.play.client.CPacketEntityAction.Action.START_SNEAKING
                        : net.minecraft.network.play.client.CPacketEntityAction.Action.STOP_SNEAKING));
                mc.playerController.processRightClick(mc.player, mc.world, net.minecraft.util.EnumHand.MAIN_HAND);
                return true;
            }
            case "click": {
                // Right-click whatever block the view is on, as a player would.
                if (mc.world == null || mc.player == null || mc.objectMouseOver == null
                        || mc.objectMouseOver.typeOfHit != net.minecraft.util.math.RayTraceResult.Type.BLOCK) {
                    log("nothing to click");
                    return true;
                }
                mc.playerController.processRightClickBlock(mc.player, mc.world, mc.objectMouseOver.getBlockPos(), mc.objectMouseOver.sideHit,
                        mc.objectMouseOver.hitVec, net.minecraft.util.EnumHand.MAIN_HAND);
                return true;
            }
            case "press":
                // Holds a key down for the game for some ticks, as if the player held it.
                if (key(mc, p[0]) == null) {
                    log("no key " + p[0]);
                    return true;
                }
                held.put(p[0], Integer.parseInt(p[1]));
                return true;
            case "harm":
                harm = "on".equals(p[0]);
                return true;
            case "view":
                // Vanilla's view: 0 first person, 1 from behind, 2 from in front. Only while the camera is free.
                mc.gameSettings.thirdPersonView = Integer.parseInt(p[0]);
                return true;
            case "look":
                // Turns the player (not the camera) to a yaw and pitch, for aiming what they use.
                mc.player.rotationYaw = mc.player.rotationYawHead = Float.parseFloat(p[0]);
                mc.player.rotationPitch = Float.parseFloat(p[1]);
                return true;
            case "ents": {
                if (mc.world == null) {
                    return false;
                }
                double r = p.length > 0 ? Double.parseDouble(p[0]) : 512D;
                for (Entity e : mc.world.loadedEntityList) {
                    if (e != mc.player && e.getDistance(mc.player) <= r) {
                        log("  " + describe(e));
                    }
                }
                log(String.format("  player %.1f %.1f %.1f dim %d", mc.player.posX, mc.player.posY, mc.player.posZ, mc.world.provider.getDimension()));
                return true;
            }
            case "size":
                outW = Integer.parseInt(p[0]);
                outH = Integer.parseInt(p[1]);
                return true;
            case "shot":
            case "matte":
                if (mc.world == null) {
                    return false;
                }
                pending = p[0];
                log("screen " + mc.currentScreen);
                pendingMatte = "matte".equals(cmd);
                pendingPad = 24;
                pendingHide.clear();
                pendingTerrain = false;
                pendingOnly = -1;
                boolean gui = false;
                for (int i = 1; i < p.length; i++) {
                    if (p[i].startsWith("-")) {
                        pendingHide.add(p[i].substring(1));
                    } else if (p[i].startsWith("@")) {
                        pendingOnly = Integer.parseInt(p[i].substring(1));
                    } else if ("+gui".equals(p[i])) {
                        gui = true;
                    } else if ("+terrain".equals(p[i])) {
                        pendingTerrain = true;
                    } else {
                        pendingPad = Integer.parseInt(p[i]);
                    }
                }
                mc.gameSettings.hideGUI = !gui;
                savedW = mc.displayWidth;
                savedH = mc.displayHeight;
                resize(mc, outW * 2, outH * 2);
                captureDelay = 3;
                return true;
            case "srv": {
                if (mc.getIntegratedServer() == null) {
                    return false;
                }
                String[] q = rest.split("\\s+");
                mc.getIntegratedServer().addScheduledTask(() -> {
                    try {
                        log("srv " + rest + " -> " + serverCall(mc, q));
                    } catch (Exception e) {
                        log("FAIL srv " + rest + " : " + e);
                        e.printStackTrace();
                    }
                });
                return true;
            }
            case "spy":
                // Watches an entity from beside it without moving the player: spy <sel> <dist> <yaw off its facing> <pitch> [height frac] | spy off.
                if ("off".equals(p[0])) {
                    spyTarget = null;
                    mc.setRenderViewEntity(mc.player);
                    return true;
                }
                spyTarget = select(mc, p[0]);
                spyDist = Double.parseDouble(p[1]);
                spyYaw = Float.parseFloat(p[2]);
                spyPitch = Float.parseFloat(p[3]);
                spyH = p.length > 4 ? Double.parseDouble(p[4]) : 0.5D;
                if (spyCam == null || spyCam.world != mc.world) {
                    spyCam = new net.minecraft.entity.item.EntityArmorStand(mc.world);
                    spyCam.setInvisible(true);
                }
                return true;
            case "close":
                if (mc.player != null) {
                    mc.player.closeScreen();
                }
                return true;
            case "tab":
                // The Destroyer Armor's systems menu: which page is open.
                com.matoon.eternity.client.gui.GuiDestroyerArmor.tab = Integer.parseInt(p[0]);
                if (mc.currentScreen != null) {
                    mc.currentScreen.initGui();
                }
                return true;
            case "armorslot": {
                // Puts an item straight into a slot of the nearest Destroyer Armor: armorslot <slot> <item> [count].
                if (mc.getIntegratedServer() == null) {
                    return false;
                }
                mc.getIntegratedServer().addScheduledTask(() -> {
                    EntityPlayerMP player = mc.getIntegratedServer().getPlayerList().getPlayers().get(0);
                    Entity armor = serverEntity(player, "destroyerarmor");
                    net.minecraft.item.Item item = net.minecraft.item.Item.getByNameOrId(p[1]);
                    if (!(armor instanceof com.matoon.eternity.entity.EntityDestroyerArmor) || item == null) {
                        log("armorslot: no armor or no item " + p[1]);
                        return;
                    }
                    ((com.matoon.eternity.entity.EntityDestroyerArmor) armor).getInventory().setInventorySlotContents(Integer.parseInt(p[0]),
                            new net.minecraft.item.ItemStack(item, p.length > 2 ? Integer.parseInt(p[2]) : 1));
                });
                return true;
            }
            case "ability": {
                // Presses or lets go of one of the pilot's stone powers, as its key on Lucraft's ability bar would: ability <key> press|release.
                if (mc.getIntegratedServer() == null) {
                    return false;
                }
                mc.getIntegratedServer().addScheduledTask(() -> {
                    EntityPlayerMP player = mc.getIntegratedServer().getPlayerList().getPlayers().get(0);
                    lucraft.mods.lucraftcore.superpowers.abilities.supplier.AbilityContainer container =
                            lucraft.mods.lucraftcore.superpowers.abilities.Ability.getAbilityContainer(com.matoon.eternity.compat.StoneAbilities.CONTEXT, player);
                    lucraft.mods.lucraftcore.superpowers.abilities.Ability ability = container == null ? null : container.getAbility(p[0]);
                    if (ability == null) {
                        StringBuilder keys = new StringBuilder();
                        if (container != null) {
                            for (lucraft.mods.lucraftcore.superpowers.abilities.Ability each : container.getAbilities()) {
                                keys.append(' ').append(container.getKeyForAbility(each));
                            }
                        }
                        log("ability: no " + p[0] + "; has" + keys);
                        return;
                    }
                    if ("release".equals(p[1])) {
                        ability.onKeyReleased();
                    } else {
                        ability.onKeyPressed();
                    }
                    log("ability " + p[0] + " " + p[1] + " enabled=" + ability.isEnabled());
                });
                return true;
            }
            case "quit":
                mc.shutdown();
                return true;
            default:
                log("unknown " + line);
                return true;
        }
    }

    @SubscribeEvent
    public static void render(TickEvent.RenderTickEvent event) {
        if (!active()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (event.phase == TickEvent.Phase.START) {
            applyCamera(mc, event.renderTickTime, false);
            spy(mc, event.renderTickTime);
            return;
        }
        if (pending == null || mc.world == null) {
            applyCamera(mc, 1F, true);
            return;
        }
        if (--captureDelay > 0) {
            return;
        }
        String name = pending;
        pending = null;
        try {
            Framebuffer fb = mc.getFramebuffer();
            int w = mc.displayWidth, h = mc.displayHeight;
            File out = new File(dir, "out");
            if (!pendingMatte) {
                BufferedImage img = ScreenShotHelper.createScreenshot(w, h, fb);
                ImageIO.write(downsample(img, null, w, h), "png", new File(out, name + ".png"));
            } else {
                BufferedImage black = studioPass(mc, event.renderTickTime, 0F, w, h, fb);
                BufferedImage white = studioPass(mc, event.renderTickTime, 1F, w, h, fb);
                BufferedImage full = downsample(black, white, w, h);
                ImageIO.write(full, "png", new File(out, name + ".png"));
                BufferedImage crop = crop(downsampleKeep(black, white, w, h), pendingPad * 2);
                if (crop != null) {
                    ImageIO.write(crop, "png", new File(out, name + "_crop.png"));
                }
            }
            log("saved " + name);
        } catch (Exception e) {
            log("FAIL capture " + name + " : " + e);
            e.printStackTrace();
        }
        resize(mc, Math.max(1, Display.getWidth()), Math.max(1, Display.getHeight()));
        log("done capture " + name);
        applyCamera(mc, 1F, true);
    }

    private static BufferedImage studioPass(Minecraft mc, float partial, float shade, int w, int h, Framebuffer fb) {
        IRenderHandler sky = mc.world.provider.getSkyRenderer();
        IRenderHandler clouds = mc.world.provider.getCloudRenderer();
        mc.world.provider.setSkyRenderer(NOTHING);
        mc.world.provider.setCloudRenderer(NOTHING);
        List<Object> silenced = new ArrayList<>();
        for (String name : pendingHide) {
            try {
                String cls = "com.matoon.eternity.client." + Character.toUpperCase(name.charAt(0)) + name.substring(1) + "Client";
                Object handler = Class.forName(cls).getField("INSTANCE").get(null);
                MinecraftForge.EVENT_BUS.unregister(handler);
                silenced.add(handler);
            } catch (ReflectiveOperationException e) {
                log("no client " + name);
            }
        }
        Field containerField = null;
        Object container = null;
        if (!pendingTerrain) {
            for (Field f : RenderGlobal.class.getDeclaredFields()) {
                if (f.getType() == ChunkRenderContainer.class) {
                    try {
                        f.setAccessible(true);
                        container = f.get(mc.renderGlobal);
                        f.set(mc.renderGlobal, NO_TERRAIN);
                        containerField = f;
                    } catch (IllegalAccessException e) {
                        log("FAIL terrain " + e);
                    }
                }
            }
        }
        List<Entity> moved = new ArrayList<>();
        if (pendingOnly >= 0) {
            // Only the chosen entity: everything else is drawn far overhead, out of the frame.
            for (Entity e : mc.world.loadedEntityList) {
                if (e.getEntityId() != pendingOnly && e != mc.player) {
                    e.posY += 5000D;
                    e.prevPosY += 5000D;
                    e.lastTickPosY += 5000D;
                    moved.add(e);
                }
            }
        }
        reseed(mc);
        studio = true;
        studioShade = shade;
        skyCleared = false;
        try {
            fb.bindFramebuffer(true);
            mc.entityRenderer.renderWorld(partial, System.nanoTime());
            return ScreenShotHelper.createScreenshot(w, h, fb);
        } finally {
            studio = false;
            for (Entity e : moved) {
                e.posY -= 5000D;
                e.prevPosY -= 5000D;
                e.lastTickPosY -= 5000D;
            }
            mc.world.provider.setSkyRenderer(sky);
            mc.world.provider.setCloudRenderer(clouds);
            for (Object handler : silenced) {
                MinecraftForge.EVENT_BUS.register(handler);
            }
            if (containerField != null) {
                try {
                    containerField.set(mc.renderGlobal, container);
                } catch (IllegalAccessException ignored) {
                }
            }
        }
    }

    private static void reseed(Minecraft mc) {
        for (String[] r : RANDOMS) {
            try {
                Field f = Class.forName(r[0]).getDeclaredField(r[1]);
                f.setAccessible(true);
                ((Random) f.get(null)).setSeed(4242L);
            } catch (ReflectiveOperationException | ClassCastException ignored) {
            }
        }
        mc.world.rand.setSeed(4242L);
    }

    // ---- studio overrides: a flat black or white void, no fog ----

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void fogColors(EntityViewRenderEvent.FogColors event) {
        if (studio) {
            event.setRed(studioShade);
            event.setGreen(studioShade);
            event.setBlue(studioShade);
        }
    }

    /** A cancelled density skips the rest of setupFog, RenderFogEvent included; let it through. */
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void fogDensity(EntityViewRenderEvent.FogDensity event) {
        if (studio) {
            event.setCanceled(false);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void fog(EntityViewRenderEvent.RenderFogEvent event) {
        if (studio) {
            GlStateManager.setFog(GlStateManager.FogMode.LINEAR);
            GlStateManager.setFogStart(1.0E6F);
            GlStateManager.setFogEnd(2.0E6F);
            GlStateManager.setFogDensity(0F);
            // The first terrain fog comes right after the sky: wipe whatever sky a dimension drew.
            if (event.getFogMode() == 0 && !skyCleared) {
                skyCleared = true;
                GlStateManager.clearColor(studioShade, studioShade, studioShade, 0F);
                GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void camera(EntityViewRenderEvent.CameraSetup event) {
        if (active() && locked) {
            event.setYaw(camYaw + 180F);
            event.setPitch(camPitch);
            event.setRoll(0F);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void fov(EntityViewRenderEvent.FOVModifier event) {
        if (active() && fov > 0F) {
            event.setFOV(fov);
        }
    }

    /** The camera player takes no harm from anything. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void attack(LivingAttackEvent event) {
        if (active() && !harm && event.getEntityLiving() instanceof EntityPlayer) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void hurt(LivingHurtEvent event) {
        if (active() && !harm && event.getEntityLiving() instanceof EntityPlayer) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void heal(TickEvent.PlayerTickEvent event) {
        if (active() && event.side == Side.SERVER && event.phase == TickEvent.Phase.END) {
            event.player.setHealth(event.player.getMaxHealth());
            event.player.getFoodStats().setFoodLevel(20);
            event.player.extinguish();
        }
    }

    /** The mod's dialogue screens would turn and block the camera. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void gui(GuiOpenEvent event) {
        if (active() && event.getGui() != null && event.getGui().getClass().getName().startsWith("com.matoon.eternity")
                && !(event.getGui() instanceof com.matoon.eternity.client.gui.GuiDestroyerArmor)) {
            log("blocked screen " + event.getGui().getClass().getSimpleName());
            event.setCanceled(true);
        }
    }

    // ---- server calls ----

    private static String serverCall(Minecraft mc, String[] q) throws Exception {
        MinecraftServer server = mc.getIntegratedServer();
        EntityPlayerMP player = server.getPlayerList().getPlayers().get(0);
        Object target;
        Class<?> cls;
        String method;
        int argStart;
        if ("call".equals(q[0])) {
            target = serverEntity(player, q[1]);
            if (target == null) {
                return "no entity " + q[1];
            }
            cls = target.getClass();
            method = q[2];
            argStart = 3;
        } else {
            String[] path = q[1].split("\\.");
            int i = path.length - 1;
            cls = null;
            while (i > 0 && cls == null) {
                try {
                    cls = Class.forName(String.join(".", Arrays.copyOfRange(path, 0, i)));
                } catch (ClassNotFoundException e) {
                    i--;
                }
            }
            if (cls == null) {
                return "no class in " + q[1];
            }
            target = null;
            for (int j = i; j < path.length - 1; j++) {
                target = field(cls, path[j]).get(target);
                cls = target.getClass();
            }
            method = path[path.length - 1];
            argStart = 2;
        }
        String[] args = Arrays.copyOfRange(q, argStart, q.length);
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (!m.getName().equals(method) || m.getParameterCount() != args.length) {
                    continue;
                }
                Object[] conv = new Object[args.length];
                boolean ok = true;
                for (int k = 0; k < args.length && ok; k++) {
                    conv[k] = convert(player, args[k], m.getParameterTypes()[k]);
                    ok = conv[k] != null;
                }
                if (ok) {
                    m.setAccessible(true);
                    return String.valueOf(m.invoke(target, conv));
                }
            }
        }
        try {
            Field f = field(cls, method);
            if (args.length == 1) {
                f.set(target, convert(player, args[0], f.getType()));
                return "set " + method;
            }
            return String.valueOf(f.get(target));
        } catch (NoSuchFieldException ignored) {
            return "no method " + method + "/" + args.length + " on " + cls.getSimpleName();
        }
    }

    /** Keeps every held key down for the game until its ticks run out, then lets it go. */
    private static void holdKeys(Minecraft mc) {
        java.util.Iterator<java.util.Map.Entry<String, Integer>> it = held.entrySet().iterator();
        while (it.hasNext()) {
            java.util.Map.Entry<String, Integer> e = it.next();
            net.minecraft.client.settings.KeyBinding key = key(mc, e.getKey());
            int left = e.getValue() - 1;
            net.minecraft.client.settings.KeyBinding.setKeyBindState(key.getKeyCode(), left > 0);
            if (left > 0) {
                e.setValue(left);
            } else {
                it.remove();
            }
        }
    }

    private static net.minecraft.client.settings.KeyBinding key(Minecraft mc, String name) {
        net.minecraft.client.settings.GameSettings gs = mc.gameSettings;
        switch (name) {
            case "forward":
                return gs.keyBindForward;
            case "back":
                return gs.keyBindBack;
            case "left":
                return gs.keyBindLeft;
            case "right":
                return gs.keyBindRight;
            case "jump":
                return gs.keyBindJump;
            case "sprint":
                return gs.keyBindSprint;
            case "sneak":
                return gs.keyBindSneak;
            case "attack":
                return gs.keyBindAttack;
            case "use":
                return gs.keyBindUseItem;
            default:
                for (net.minecraft.client.settings.KeyBinding k : gs.keyBindings) {
                    if (k.getKeyDescription().endsWith(name)) {
                        return k;
                    }
                }
                return null;
        }
    }

    private static Field field(Class<?> cls, String name) throws NoSuchFieldException {
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static Entity serverEntity(Entity player, String sel) {
        Entity best = null;
        double bestD = Double.MAX_VALUE;
        String s = sel.toLowerCase(Locale.ROOT);
        for (Entity e : player.world.loadedEntityList) {
            boolean match = s.startsWith("#") ? e.getEntityId() == Integer.parseInt(s.substring(1))
                    : e != player && e.getClass().getSimpleName().toLowerCase(Locale.ROOT).contains(s);
            if (match && e.getDistanceSq(player) < bestD) {
                best = e;
                bestD = e.getDistanceSq(player);
            }
        }
        return best;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object convert(Entity player, String a, Class<?> t) {
        try {
            if (t == int.class || t == Integer.class) {
                return Integer.parseInt(a);
            } else if (t == long.class || t == Long.class) {
                return Long.parseLong(a);
            } else if (t == float.class || t == Float.class) {
                return Float.parseFloat(a);
            } else if (t == double.class || t == Double.class) {
                return Double.parseDouble(a);
            } else if (t == boolean.class || t == Boolean.class) {
                return "true".equals(a) ? Boolean.TRUE : "false".equals(a) ? Boolean.FALSE : null;
            } else if (t == String.class) {
                return a;
            } else if (t == List.class) {
                return "[]".equals(a) ? new ArrayList<>() : null;
            } else if (t.isEnum()) {
                return Enum.valueOf((Class<Enum>) t, a.toUpperCase(Locale.ROOT));
            } else if (Entity.class.isAssignableFrom(t)) {
                Entity e = "player".equals(a) ? player : serverEntity(player, a);
                return t.isInstance(e) ? e : null;
            }
        } catch (IllegalArgumentException ignored) {
        }
        return null;
    }

    // ---- camera ----


    private static Entity spyTarget, spyCam;
    private static double spyDist, spyH;
    private static float spyYaw, spyPitch;

    /** Puts the spy camera beside its target, facing it, and renders from there. */
    private static void spy(Minecraft mc, float partial) {
        if (spyTarget == null || spyCam == null || mc.world == null) {
            return;
        }
        if (spyTarget.isDead) {
            spyTarget = null;
            mc.setRenderViewEntity(mc.player);
            return;
        }
        Entity t = spyTarget;
        double tx = t.lastTickPosX + (t.posX - t.lastTickPosX) * partial;
        double ty = t.lastTickPosY + (t.posY - t.lastTickPosY) * partial + t.height * spyH;
        double tz = t.lastTickPosZ + (t.posZ - t.lastTickPosZ) * partial;
        float yaw = t.rotationYaw + spyYaw;
        Vec3d dir = Vec3d.fromPitchYaw(spyPitch, yaw);
        double cx = tx + dir.x * spyDist, cy = ty - dir.y * spyDist, cz = tz + dir.z * spyDist;
        float camYaw = yaw + 180F;
        cy -= spyCam.getEyeHeight();
        spyCam.setLocationAndAngles(cx, cy, cz, camYaw, spyPitch);
        spyCam.prevPosX = spyCam.lastTickPosX = cx;
        spyCam.prevPosY = spyCam.lastTickPosY = cy;
        spyCam.prevPosZ = spyCam.lastTickPosZ = cz;
        spyCam.prevRotationYaw = camYaw;
        spyCam.prevRotationPitch = spyPitch;
        mc.gameSettings.thirdPersonView = 0;
        if (mc.getRenderViewEntity() != spyCam) {
            mc.setRenderViewEntity(spyCam);
        }
    }
    private static void applyCamera(Minecraft mc, float partial, boolean forTick) {
        if (!locked || mc.player == null) {
            return;
        }
        if (target != null && (follow || target.isDead)) {
            if (target.isDead) {
                target = null;
            } else {
                orbit(partial);
            }
        }
        mc.gameSettings.viewBobbing = false;
        mc.gameSettings.thirdPersonView = 0;
        Vec3d look = Vec3d.fromPitchYaw(camPitch, camYaw);
        Vec3d right = new Vec3d(-look.z, 0D, look.x).normalize();
        Vec3d up = right.crossProduct(look).normalize();
        double x = camX + right.x * shiftR + up.x * shiftU + look.x * shiftF;
        double y = camY + right.y * shiftR + up.y * shiftU + look.y * shiftF;
        double z = camZ + right.z * shiftR + up.z * shiftU + look.z * shiftF;
        if (forTick && anchored) {
            // The game (and whatever hunts the player) sees the anchor; only the frame sees the camera.
            x = anchorX;
            y = anchorY + mc.player.getEyeHeight();
            z = anchorZ;
        }
        Entity p = mc.player;
        y -= p.getEyeHeight();
        p.setPosition(x, y, z);
        p.prevPosX = p.lastTickPosX = x;
        p.prevPosY = p.lastTickPosY = y;
        p.prevPosZ = p.lastTickPosZ = z;
        p.motionX = p.motionY = p.motionZ = 0D;
        p.noClip = true;
        p.rotationYaw = p.prevRotationYaw = camYaw;
        p.rotationPitch = p.prevRotationPitch = camPitch;
        mc.player.rotationYawHead = mc.player.prevRotationYawHead = camYaw;
        mc.player.renderYawOffset = mc.player.prevRenderYawOffset = camYaw;
        mc.player.capabilities.isFlying = true;
    }

    /** The server rejects the locked camera's jumps as moving too fast; put its copy of the player there too. */
    private static void syncServerPlayer(Minecraft mc) {
        MinecraftServer server = mc.getIntegratedServer();
        if (!locked || mc.player == null || server == null) {
            return;
        }
        double x = mc.player.posX, y = mc.player.posY, z = mc.player.posZ;
        float yaw = camYaw, pitch = camPitch;
        java.util.UUID id = mc.player.getUniqueID();
        server.addScheduledTask(() -> {
            EntityPlayerMP sp = server.getPlayerList().getPlayerByUUID(id);
            if (sp != null) {
                sp.capabilities.isFlying = true;
                sp.fallDistance = 0F;
                if (sp.getDistanceSq(x, y, z) > 4D) {
                    sp.connection.setPlayerLocation(x, y, z, yaw, pitch);
                }
            }
        });
    }

    private static void orbit(float partial) {
        Entity e = target;
        double ex = e.prevPosX + (e.posX - e.prevPosX) * partial;
        double ey = e.prevPosY + (e.posY - e.prevPosY) * partial;
        double ez = e.prevPosZ + (e.posZ - e.prevPosZ) * partial;
        AxisAlignedBB box = e.getEntityBoundingBox();
        double cy = ey + (box.maxY - box.minY) * orbitH;
        float yaw = orbitYaw;
        if (relative) {
            float facing = e instanceof EntityLivingBase ? ((EntityLivingBase) e).renderYawOffset : e.rotationYaw;
            yaw = MathHelper.wrapDegrees(facing + 180F + orbitYaw);
        }
        Vec3d look = Vec3d.fromPitchYaw(orbitPitch, yaw);
        camX = ex - look.x * orbitDist;
        camY = cy - look.y * orbitDist;
        camZ = ez - look.z * orbitDist;
        camYaw = yaw;
        camPitch = orbitPitch;
    }

    private static Entity select(Minecraft mc, String sel) {
        Entity best = null;
        double bestD = Double.MAX_VALUE;
        String s = sel.toLowerCase(Locale.ROOT);
        for (Entity e : mc.world.loadedEntityList) {
            if (e == mc.player) {
                continue;
            }
            boolean match = s.startsWith("#") ? e.getEntityId() == Integer.parseInt(s.substring(1))
                    : e.getClass().getSimpleName().toLowerCase(Locale.ROOT).contains(s);
            double d = e.getDistanceSq(mc.player);
            if (match && d < bestD) {
                best = e;
                bestD = d;
            }
        }
        return best;
    }

    private static String describe(Entity e) {
        AxisAlignedBB b = e.getEntityBoundingBox();
        return String.format("#%d %s at %.1f %.1f %.1f size %.1fx%.1f yaw %.0f dist %.0f%s", e.getEntityId(), e.getClass().getSimpleName(),
                e.posX, e.posY, e.posZ, b.maxX - b.minX, b.maxY - b.minY, e.rotationYaw, e.getDistance(Minecraft.getMinecraft().player),
                e instanceof EntityLivingBase ? String.format(" hp %.0f/%.0f", ((EntityLivingBase) e).getHealth(), ((EntityLivingBase) e).getMaxHealth()) : "");
    }

    // ---- images ----

    private static void resize(Minecraft mc, int w, int h) {
        try {
            Method m;
            try {
                m = Minecraft.class.getDeclaredMethod("resize", int.class, int.class);
            } catch (NoSuchMethodException e) {
                m = Minecraft.class.getDeclaredMethod("func_71370_a", int.class, int.class);
            }
            m.setAccessible(true);
            m.invoke(mc, w, h);
        } catch (ReflectiveOperationException e) {
            log("FAIL resize " + e);
        }
    }

    /** 2x box filter; with a white pass, recovers straight alpha from the black/white pair. */
    private static BufferedImage downsample(BufferedImage black, BufferedImage white, int w, int h) {
        int ow = w / 2, oh = h / 2;
        BufferedImage out = new BufferedImage(ow, oh, white == null ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < oh; y++) {
            for (int x = 0; x < ow; x++) {
                double r = 0, g = 0, b = 0, a = 0;
                for (int dy = 0; dy < 2; dy++) {
                    for (int dx = 0; dx < 2; dx++) {
                        double[] px = pixel(black, white, x * 2 + dx, y * 2 + dy);
                        r += px[0];
                        g += px[1];
                        b += px[2];
                        a += px[3];
                    }
                }
                out.setRGB(x, y, pack(r / 4, g / 4, b / 4, a / 4, white != null));
            }
        }
        return out;
    }

    /** Full resolution matte, for the tight crop. */
    private static BufferedImage downsampleKeep(BufferedImage black, BufferedImage white, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                double[] px = pixel(black, white, x, y);
                out.setRGB(x, y, pack(px[0], px[1], px[2], px[3], true));
            }
        }
        return out;
    }

    /** Premultiplied r, g, b and alpha, 0-1. */
    private static double[] pixel(BufferedImage black, BufferedImage white, int x, int y) {
        int cb = black.getRGB(x, y);
        double br = (cb >> 16 & 255) / 255D, bg = (cb >> 8 & 255) / 255D, bb = (cb & 255) / 255D;
        if (white == null) {
            return new double[]{br, bg, bb, 1D};
        }
        int cw = white.getRGB(x, y);
        double wr = (cw >> 16 & 255) / 255D, wg = (cw >> 8 & 255) / 255D, wb = (cw & 255) / 255D;
        double a = 1D - Math.min(wr - br, Math.min(wg - bg, wb - bb));
        a = Math.max(0D, Math.min(1D, a));
        return new double[]{Math.min(br, a), Math.min(bg, a), Math.min(bb, a), a};
    }

    private static int pack(double r, double g, double b, double a, boolean alpha) {
        if (alpha && a > 0D) {
            r /= a;
            g /= a;
            b /= a;
        }
        int ir = clamp(r), ig = clamp(g), ib = clamp(b), ia = alpha ? clamp(a) : 255;
        if (ia == 0) {
            ir = ig = ib = 0;
        }
        return ia << 24 | ir << 16 | ig << 8 | ib;
    }

    private static int clamp(double v) {
        return Math.max(0, Math.min(255, (int) Math.round(v * 255D)));
    }

    private static BufferedImage crop(BufferedImage img, int pad) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = -1, maxY = -1;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                if ((img.getRGB(x, y) >>> 24) > 3) {
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                    minY = Math.min(minY, y);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        if (maxX < 0) {
            return null;
        }
        minX = Math.max(0, minX - pad);
        minY = Math.max(0, minY - pad);
        maxX = Math.min(img.getWidth() - 1, maxX + pad);
        maxY = Math.min(img.getHeight() - 1, maxY + pad);
        return img.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    // ---- queue and log ----

    private static void readQueue() {
        File f = new File(dir, "queue.txt");
        if (!f.isFile()) {
            return;
        }
        try {
            List<String> lines = new ArrayList<>(Files.readAllLines(f.toPath(), StandardCharsets.UTF_8));
            Files.delete(f.toPath());
            for (String l : lines) {
                if (!l.trim().isEmpty()) {
                    queue.add(l.trim());
                }
            }
        } catch (IOException ignored) {
            // Still being written; next tick.
        }
    }

    private static void log(String s) {
        System.out.println("SHOTS " + s);
        try (PrintWriter w = new PrintWriter(new FileWriter(new File(dir, "log.txt"), true))) {
            w.println(s);
        } catch (IOException ignored) {
        }
    }
}
