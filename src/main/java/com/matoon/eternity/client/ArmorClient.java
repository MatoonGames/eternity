package com.matoon.eternity.client;

import com.matoon.eternity.client.render.ArmorView;
import com.matoon.eternity.entity.ArmorUpgrade;
import com.matoon.eternity.entity.EntityDestroyerArmor;
import com.matoon.eternity.entity.EntityDestroyerArmor.Command;
import com.matoon.eternity.entity.Overcharge;
import com.matoon.eternity.item.ItemEinherjarSpear;
import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.network.MsgArmorCommand;
import com.matoon.eternity.network.MsgSpearStrike;
import lucraft.mods.heroesexpansion.abilities.AbilityGrabEntity;
import lucraft.mods.heroesexpansion.network.HEPacketDispatcher;
import lucraft.mods.heroesexpansion.network.MessageSendInfoToServer;
import lucraft.mods.lucraftcore.LCConfig;
import lucraft.mods.lucraftcore.superpowers.abilities.Ability;
import lucraft.mods.lucraftcore.util.abilitybar.AbilityBarHandler;
import lucraft.mods.lucraftcore.util.abilitybar.AbilityBarPos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.FOVUpdateEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.RenderSpecificHandEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import javax.annotation.Nullable;

/**
 * Inside the Destroyer Armor. Whatever the pilot presses goes to the armour rather than to their
 * own hands: attack punches, use pours out the ray, its own key brings both fists down, jump leaps,
 * sprint charges, and sneak, held, is the only way out. They see out of its face, its fists in
 * front of them, its gauges along the bottom of their sight; pulled back to watch it, the view
 * stands back far enough to take it all in. Anyone inside it is out of sight to everyone.
 *
 * Also the Einherjar's spear, which reaches further than vanilla lets anyone strike.
 */
@SideOnly(Side.CLIENT)
public final class ArmorClient {

    public static final ArmorClient INSTANCE = new ArmorClient();

    public static final KeyBinding SLAM = new KeyBinding("key.eternity.armor_slam", Keyboard.KEY_R, "key.categories.eternity");
    /** Its overcharge, once forged: up and down turn the picked part, left and right pick it, and one key cuts it all. */
    public static final KeyBinding OVERCHARGE_UP = new KeyBinding("key.eternity.armor_overcharge_up", Keyboard.KEY_UP, "key.categories.eternity");
    public static final KeyBinding OVERCHARGE_DOWN = new KeyBinding("key.eternity.armor_overcharge_down", Keyboard.KEY_DOWN, "key.categories.eternity");
    public static final KeyBinding OVERCHARGE_NEXT = new KeyBinding("key.eternity.armor_overcharge_next", Keyboard.KEY_RIGHT, "key.categories.eternity");
    public static final KeyBinding OVERCHARGE_PREV = new KeyBinding("key.eternity.armor_overcharge_prev", Keyboard.KEY_LEFT, "key.categories.eternity");
    public static final KeyBinding OVERCHARGE_CUT = new KeyBinding("key.eternity.armor_overcharge_cut", Keyboard.KEY_BACK, "key.categories.eternity");
    /** Which of its parts the overcharge keys turn, by {@link Overcharge} index; its systems menu picks it too. */
    public static int surgePart;

    /** How long sneak must be held to step out. */
    private static final int EXIT_HOLD = 16;
    /** How far back the view stands to watch it, beyond vanilla's own distance. */
    private static final double STAND_BACK = 4D;

    private final Minecraft mc = Minecraft.getMinecraft();
    private boolean attackSent, fireSent, slamWas, jumpSent, jumpWas;
    /** A swing that threw what a stone's grip held, which does not punch until it is let go. */
    private boolean threw;
    /** How long each overcharge key has been held, and whether the others were down last tick. */
    private int upFor, downFor;
    private boolean nextWas, prevWas, cutWas;
    /** Ticks inside it since they were last put in, and whether they have paged through their powers yet: until then, they are told how. */
    private int insideTicks;
    private boolean paged;
    /** Ticks since jump was last pressed, for a double tap. */
    private int sinceJump = 100;
    private int exitHeld;
    private boolean told;

    private ArmorClient() {
    }

    public void register() {
        ClientRegistry.registerKeyBinding(SLAM);
        ClientRegistry.registerKeyBinding(OVERCHARGE_UP);
        ClientRegistry.registerKeyBinding(OVERCHARGE_DOWN);
        ClientRegistry.registerKeyBinding(OVERCHARGE_NEXT);
        ClientRegistry.registerKeyBinding(OVERCHARGE_PREV);
        ClientRegistry.registerKeyBinding(OVERCHARGE_CUT);
    }

    @Nullable
    private EntityDestroyerArmor armor() {
        return mc.player != null && mc.player.getRidingEntity() instanceof EntityDestroyerArmor ? (EntityDestroyerArmor) mc.player.getRidingEntity() : null;
    }

    // ------------------------------------------------------------------ the pilot's hands on it

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (mc.player == null || mc.world == null) {
            return;
        }
        EntityDestroyerArmor armor = armor();
        if (event.phase == TickEvent.Phase.END) {
            tellControls(armor);
            return;
        }
        if (armor == null) {
            attackSent = fireSent = jumpSent = false;
            exitHeld = 0;
            insideTicks = 0;
            return;
        }
        if (armor.hidesPilot()) {
            insideTicks++;
        }
        GameSettings gs = mc.gameSettings;
        boolean free = mc.currentScreen == null;
        armor.clientJump = free && gs.keyBindJump.isKeyDown();
        armor.clientSprint = free && gs.keyBindSprint.isKeyDown();
        boolean attack = free && held(gs.keyBindAttack);
        boolean fire = free && held(gs.keyBindUseItem);
        // Vanilla never sees the pilot's clicks: their own hands are not theirs to use in here.
        while (gs.keyBindAttack.isPressed()) {
            attack = free;
        }
        while (gs.keyBindUseItem.isPressed()) {
            fire = free;
        }
        KeyBinding.setKeyBindState(gs.keyBindAttack.getKeyCode(), false);
        KeyBinding.setKeyBindState(gs.keyBindUseItem.getKeyCode(), false);
        // Holding something in a stone's grip, a swing throws it rather than punching.
        if (attack && (threw || gripping())) {
            if (!threw) {
                HEPacketDispatcher.sendToServer(new MessageSendInfoToServer(MessageSendInfoToServer.ServerMessageType.THROW_GRABBED_ENTITY));
                threw = true;
            }
            attack = false;
        } else if (!attack) {
            threw = false;
        }
        if (attack != attackSent) {
            EternityNetwork.sendToServer(new MsgArmorCommand(Command.ATTACK, attack));
            attackSent = attack;
        }
        if (fire != fireSent) {
            EternityNetwork.sendToServer(new MsgArmorCommand(Command.FIRE, fire));
            fireSent = fire;
        }
        // A press, however brief, brings its fists down once.
        boolean slam = SLAM.isPressed() || free && held(SLAM);
        while (SLAM.isPressed()) {
            slam = true;
        }
        if (slam && free && !slamWas) {
            EternityNetwork.sendToServer(new MsgArmorCommand(Command.SLAM, true));
        }
        slamWas = slam;
        surgeKeys(armor, free);
        // Jump: held, for its jets; tapped twice, it takes to the air or comes out of it.
        boolean jump = armor.clientJump;
        if (jump != jumpSent) {
            EternityNetwork.sendToServer(new MsgArmorCommand(Command.JUMP, jump));
            jumpSent = jump;
        }
        sinceJump++;
        if (jump && !jumpWas) {
            if (sinceJump < 7 && armor.has(ArmorUpgrade.ASCENSION) && armor.isManned()) {
                EternityNetwork.sendToServer(new MsgArmorCommand(Command.FLY, !armor.isFlying()));
                sinceJump = 100;
            } else {
                sinceJump = 0;
            }
        }
        jumpWas = jump;
        boolean sneak = free && held(gs.keyBindSneak);
        armor.clientDescend = sneak;
        // Flying, sneak only brings it down; it has to land to let anyone out.
        if (sneak && armor.isManned() && !armor.isFlying()) {
            if (++exitHeld == EXIT_HOLD) {
                EternityNetwork.sendToServer(new MsgArmorCommand(Command.EXIT, true));
            }
        } else {
            exitHeld = 0;
        }
    }

    /** Whether a stone's grip (the Space Stone's, the Mind Stone's telekinesis) holds anything. */
    private boolean gripping() {
        for (AbilityGrabEntity grip : Ability.getAbilitiesFromClass(Ability.getAbilities(mc.player), AbilityGrabEntity.class)) {
            if (grip.isUnlocked() && grip.isEnabled()) {
                return true;
            }
        }
        return false;
    }

    /** Whether a key is down: by the hand on it, or as the game holds it (which we clear for attack and use every tick). */
    /**
     * Its overcharge: left and right pick a part, up and down turn it, a step a press and faster the longer they are
     * held, so a high level is a few seconds away rather than a hundred presses; its cut key turns everything down.
     */
    private void surgeKeys(EntityDestroyerArmor armor, boolean free) {
        boolean up = free && held(OVERCHARGE_UP), down = free && held(OVERCHARGE_DOWN);
        boolean next = free && held(OVERCHARGE_NEXT), prev = free && held(OVERCHARGE_PREV), cut = free && held(OVERCHARGE_CUT);
        while (OVERCHARGE_UP.isPressed()) {
            up = free;
        }
        while (OVERCHARGE_DOWN.isPressed()) {
            down = free;
        }
        while (OVERCHARGE_NEXT.isPressed()) {
            next = free;
        }
        while (OVERCHARGE_PREV.isPressed()) {
            prev = free;
        }
        while (OVERCHARGE_CUT.isPressed()) {
            cut = free;
        }
        upFor = up ? upFor + 1 : 0;
        downFor = down ? downFor + 1 : 0;
        int parts = Overcharge.values().length;
        surgePart = Math.floorMod(surgePart, parts);
        if (armor.canOvercharge() && (next && !nextWas || prev && !prevWas)) {
            surgePart = Math.floorMod(surgePart + (next && !nextWas ? 1 : -1), parts);
            Overcharge part = Overcharge.values()[surgePart];
            int level = armor.surge(part);
            mc.ingameGUI.setOverlayMessage(TextFormatting.GOLD + "Overcharge: " + part.title.toUpperCase() + " " + level + TextFormatting.GRAY + "  -  "
                    + part.effect(level) + "  -  " + OVERCHARGE_UP.getDisplayName() + "/" + OVERCHARGE_DOWN.getDisplayName() + " to turn it", false);
            mc.player.playSound(net.minecraft.init.SoundEvents.UI_BUTTON_CLICK, 0.4F, 1.6F);
        }
        nextWas = next;
        prevWas = prev;
        int step = repeat(upFor) - repeat(downFor);
        // Without it forged, only a fresh press goes through, to be told why nothing happens.
        if (step != 0 && (armor.canOvercharge() || upFor == 1 || downFor == 1)) {
            EternityNetwork.sendToServer(new MsgArmorCommand(Command.SURGE, true, (surgePart + 1) << 16 | step & 0xFFFF));
        }
        if (cut && !cutWas && armor.canOvercharge()) {
            EternityNetwork.sendToServer(new MsgArmorCommand(Command.SURGE, true, Short.MIN_VALUE & 0xFFFF));
        }
        cutWas = cut;
    }

    /** How far a key held this many ticks turns a part this tick: a step as it goes down, then repeating, then running. */
    private static int repeat(int ticks) {
        if (ticks == 1) {
            return 1;
        }
        if (ticks < 10 || ticks % 2 != 0) {
            return 0;
        }
        return ticks < 40 ? 1 : ticks < 80 ? 5 : 25;
    }

    private static boolean held(KeyBinding key) {
        return GameSettings.isKeyDown(key) || key.isKeyDown();
    }

    /**
     * Lucraft pages through its ability bar on SHIFT and the mouse wheel, but SHIFT is the armour's own (sinking, and
     * held, stepping out). Inside it, the hotbar is no use to anyone, so the wheel alone pages the ability bar, with or
     * without SHIFT. Whoever is inside is told so beside the bar (see {@link #scrollHint}).
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onMouse(MouseEvent event) {
        EntityDestroyerArmor armor = armor();
        if (armor == null || !armor.hidesPilot() || event.getDwheel() == 0 || mc.currentScreen != null || AbilityBarHandler.getActiveEntries().isEmpty()) {
            return;
        }
        AbilityBarHandler.scroll(event.getDwheel() > 0);
        paged = true;
        event.setCanceled(true);
    }

    /** The inventory key, inside it, opens its systems rather than the pilot's own pack (which those show as well). */
    @SubscribeEvent
    public void onOpenGui(GuiOpenEvent event) {
        EntityDestroyerArmor armor = armor();
        if (armor != null && armor.hidesPilot() && (event.getGui() instanceof GuiInventory || event.getGui() instanceof GuiContainerCreative)) {
            event.setCanceled(true);
            EternityNetwork.sendToServer(new MsgArmorCommand(Command.MENU, true));
        }
    }

    /** Flying flat out, the view stretches with its speed. */
    @SubscribeEvent
    public void onFov(FOVUpdateEvent event) {
        EntityDestroyerArmor armor = armor();
        if (armor != null && event.getEntity() == mc.player) {
            float speed = MathHelper.clamp((armor.lean(1F) - 0.1F) / 1F, 0F, 1F) * armor.flight(1F);
            event.setNewfov(event.getNewfov() * (1F + 0.18F * speed));
        }
    }

    /** Sneaking does not tip the pilot out; holding it is how they ask to be let out. */
    @SubscribeEvent
    public void onInput(InputUpdateEvent event) {
        if (event.getEntityPlayer() == mc.player && armor() != null) {
            event.getMovementInput().sneak = false;
        }
    }

    /** Once it has closed around them, how to drive it, over vanilla's word about dismounting. */
    private void tellControls(@Nullable EntityDestroyerArmor armor) {
        if (armor == null || armor.getState() != EntityDestroyerArmor.State.ENTER) {
            told = armor != null && told;
            return;
        }
        if (!told && armor.clientStateTicks >= EntityDestroyerArmor.ENTER_SHUT + 8) {
            GameSettings gs = mc.gameSettings;
            mc.ingameGUI.setOverlayMessage(new TextComponentString(String.format(
                    "%s punch  -  %s ray  -  %s slam  -  %s leap  -  %s charge  -  %s systems  -  hold %s to step out%s",
                    gs.keyBindAttack.getDisplayName(), gs.keyBindUseItem.getDisplayName(), SLAM.getDisplayName(), gs.keyBindJump.getDisplayName(),
                    gs.keyBindSprint.getDisplayName(), gs.keyBindInventory.getDisplayName(), gs.keyBindSneak.getDisplayName(),
                    AbilityBarHandler.getActiveEntries().isEmpty() ? "" : "  -  mouse wheel pages your powers")), false);
            told = true;
        }
    }

    // ------------------------------------------------------------------ what the pilot sees

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Pre event) {
        EntityDestroyerArmor armor = armor();
        if (armor == null || !armor.hidesPilot()) {
            return;
        }
        switch (event.getType()) {
            case HEALTH:
            case ARMOR:
            case FOOD:
            case AIR:
            case HEALTHMOUNT:
            case JUMPBAR:
                event.setCanceled(true);
                break;
            case ALL: {
                ScaledResolution res = event.getResolution();
                boolean firstPerson = mc.gameSettings.thirdPersonView == 0;
                ArmorView.visor(armor, res.getScaledWidth_double(), res.getScaledHeight_double(), event.getPartialTicks(), firstPerson);
                scrollHint(armor, res);
                if (exitHeld > 0) {
                    ArmorView.leaving(res.getScaledWidth_double(), res.getScaledHeight_double(), Math.min(1F, (exitHeld + event.getPartialTicks()) / EXIT_HOLD));
                }
                GlStateManager.color(1F, 1F, 1F, 1F);
                break;
            }
            default:
                break;
        }
    }

    /**
     * Beside Lucraft's ability bar, how to page through it in here, since its own way (SHIFT and the wheel) is not:
     * spelled out and pulsing for the first while inside, until they have paged once; then a small label that stays.
     */
    private void scrollHint(EntityDestroyerArmor armor, ScaledResolution res) {
        int count = AbilityBarHandler.getActiveEntries().size();
        if (count == 0 || LCConfig.abilityBar == AbilityBarPos.DISABLED || mc.gameSettings.showDebugInfo || !armor.hidesPilot()) {
            return;
        }
        boolean right = LCConfig.abilityBar == AbilityBarPos.RIGHT;
        int y = 12 + Math.min(5, count) * 21 + (count > 5 ? 14 : 3);
        boolean tell = !paged && insideTicks < 400;
        String[] lines = tell ? new String[]{"MOUSE WHEEL", "pages your powers", "(SHIFT is the suit's)"} : new String[]{"WHEEL: POWERS"};
        float pulse = tell ? 0.85F + 0.15F * MathHelper.sin(insideTicks * 0.25F) : 0.75F;
        GlStateManager.pushMatrix();
        GlStateManager.scale(0.75F, 0.75F, 1F);
        for (int i = 0; i < lines.length; i++) {
            int w = mc.fontRenderer.getStringWidth(lines[i]);
            float x = right ? (res.getScaledWidth() - 3F) / 0.75F - w : 3F / 0.75F;
            int alpha = (int) (255F * pulse) << 24;
            mc.fontRenderer.drawStringWithShadow(lines[i], x, (y + i * 8) / 0.75F, alpha | (i == 0 ? 0xFFD870 : 0xC8BCA8));
        }
        GlStateManager.popMatrix();
    }

    /** Its fists where the pilot's hands would be. */
    @SubscribeEvent
    public void onHand(RenderSpecificHandEvent event) {
        EntityDestroyerArmor armor = armor();
        if (armor == null || armor.getState() == EntityDestroyerArmor.State.EMPTY) {
            return;
        }
        event.setCanceled(true);
        if (event.getHand() == EnumHand.MAIN_HAND && armor.hidesPilot()) {
            ArmorView.arms(armor, event.getPartialTicks());
        }
    }

    /** Whoever is inside it cannot be seen. */
    @SubscribeEvent
    public void onRenderPlayer(RenderPlayerEvent.Pre event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player.getRidingEntity() instanceof EntityDestroyerArmor && ((EntityDestroyerArmor) player.getRidingEntity()).hidesPilot()) {
            event.setCanceled(true);
        }
    }

    /** Pulled back to watch it, the view stands far enough off to take in all of it, short of any wall. */
    @SubscribeEvent
    public void onCamera(EntityViewRenderEvent.CameraSetup event) {
        EntityDestroyerArmor armor = armor();
        int view = mc.gameSettings.thirdPersonView;
        if (armor == null || view == 0 || event.getEntity() != mc.player) {
            return;
        }
        float pt = (float) event.getRenderPartialTicks();
        Vec3d eye = mc.player.getPositionEyes(pt);
        Vec3d look = mc.player.getLook(pt).scale(view == 2 ? 1D : -1D);
        double vanilla = 4D;
        double room = STAND_BACK;
        RayTraceResult wall = mc.world.rayTraceBlocks(eye, eye.add(look.scale(vanilla + STAND_BACK + 0.5D)), false, true, false);
        if (wall != null && wall.hitVec != null) {
            room = MathHelper.clamp(wall.hitVec.distanceTo(eye) - vanilla - 0.5D, 0D, STAND_BACK);
        }
        // From in front, vanilla's own distance lies along +z by the time this is applied; from behind, along -z.
        GlStateManager.translate(0F, 0F, (float) (view == 2 ? room : -room));
    }

    // ------------------------------------------------------------------ the Einherjar's spear

    /** A strike at the air with the spear: if something is within its reach, though beyond a sword's, it lands. */
    @SubscribeEvent
    public void onSwingAtAir(PlayerInteractEvent.LeftClickEmpty event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player != mc.player || !ItemEinherjarSpear.wields(player) || player.isRiding()) {
            return;
        }
        Vec3d eye = player.getPositionEyes(1F);
        Vec3d look = player.getLook(1F);
        double reach = ItemEinherjarSpear.REACH;
        Vec3d far = eye.add(look.scale(reach));
        RayTraceResult wall = mc.world.rayTraceBlocks(eye, far, false, true, false);
        double limit = wall != null && wall.hitVec != null ? wall.hitVec.distanceTo(eye) : reach;
        Entity best = null;
        double bestDistance = limit;
        AxisAlignedBB sweep = player.getEntityBoundingBox().expand(look.x * reach, look.y * reach, look.z * reach).grow(1D);
        for (Entity entity : mc.world.getEntitiesWithinAABBExcludingEntity(player, sweep)) {
            if (!(entity instanceof EntityLivingBase) || !entity.canBeCollidedWith() || entity.isRidingOrBeingRiddenBy(player)) {
                continue;
            }
            AxisAlignedBB box = entity.getEntityBoundingBox().grow(entity.getCollisionBorderSize() + 0.1D);
            RayTraceResult hit = box.calculateIntercept(eye, far);
            double d = box.contains(eye) ? 0D : hit != null ? hit.hitVec.distanceTo(eye) : Double.MAX_VALUE;
            if (d < bestDistance) {
                best = entity;
                bestDistance = d;
            }
        }
        if (best != null) {
            EternityNetwork.sendToServer(new MsgSpearStrike(best.getEntityId()));
        }
    }
}
