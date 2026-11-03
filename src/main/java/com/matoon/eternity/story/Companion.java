package com.matoon.eternity.story;

import com.matoon.eternity.EternitySounds;
import com.matoon.eternity.entity.EntityChampion;
import com.matoon.eternity.item.EternityItems;
import com.matoon.eternity.network.EternityNetwork;
import com.matoon.eternity.network.MsgStory;
import com.matoon.eternity.network.MsgTribunalFx;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static com.matoon.eternity.story.Speakers.DIRECTION;

/**
 * The Champion, on someone's side. Robbed of his stone and brought home anyway, he has nothing to pay
 * with but himself: a ticket stub from his Colosseum, and whoever holds it up, he comes. No stone in
 * his brow and none of its powers; just an Elder of the Universe, very strong, who fights whatever
 * comes for them until they send him off.
 * <p>
 * He is never saved with the world. While someone has called him, he is put back at their side
 * whenever he isn't there: after they log in, change worlds, or get too far ahead of him.
 */
final class Companion {

    /** Beat: they have called him and not yet sent him off. */
    static final String CALLED = "champion_called";
    /** Beat: they have been given his ticket. */
    static final String TICKET = "champion_ticket";

    private static final String[] COMING = {"You called. I came.", "Point me at it.", "Good. I was getting bored of the stars."};
    private static final String[] GOING = {"Call when there's something worth hitting.", "I'll be near. I'm always near a fight.",
            "Mind your guard without me."};

    private final StoryDirector story;
    private final Random rand = new Random();
    /** Who he is with, and him. */
    private final Map<UUID, EntityChampion> with = new HashMap<>();

    Companion(StoryDirector story) {
        this.story = story;
    }

    void reset() {
        with.clear();
    }

    /** He is at their side right now, if he is. */
    @Nullable
    EntityChampion of(EntityPlayerMP player) {
        EntityChampion c = with.get(player.getUniqueID());
        return c != null && !c.isDead && !c.isLeaving() && c.world == player.world ? c : null;
    }

    /** The ticket held up: he comes, or, if he is already here, he goes. */
    void ticket(EntityPlayerMP player) {
        StoryData d = story.data();
        StoryData.Player p = d.player(player.getUniqueID());
        if (p.beats.remove(CALLED)) {
            d.markDirty();
            EntityChampion c = with.remove(player.getUniqueID());
            if (c != null && !c.isDead) {
                c.say(GOING[rand.nextInt(GOING.length)]);
                c.leave();
            }
            return;
        }
        p.beats.add(CALLED);
        d.markDirty();
        EntityChampion c = summon(player);
        if (c != null) {
            c.say(COMING[rand.nextInt(COMING.length)]);
        }
    }

    void tick(MinecraftServer server) {
        if (StoryDirector.now() % 10 != 0) {
            return;
        }
        StoryData d = story.data();
        for (Iterator<Map.Entry<UUID, EntityChampion>> it = with.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, EntityChampion> e = it.next();
            EntityPlayerMP owner = server.getPlayerList().getPlayerByUUID(e.getKey());
            if (owner == null || !d.player(e.getKey()).seen(CALLED)) {
                if (!e.getValue().isDead && !e.getValue().isLeaving()) {
                    e.getValue().setDead();
                }
                it.remove();
            }
        }
        for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
            StoryData.Player p = d.player(player.getUniqueID());
            if (p.seen("champion_helped") && !story.talking(player) && p.beats.add(TICKET)) {
                d.markDirty();
                give(player);
            }
            if (!p.seen(CALLED) || player.isSpectator() || !player.isEntityAlive() || player.isRiding()) {
                continue;
            }
            EntityChampion c = with.get(player.getUniqueID());
            boolean lost = c == null || c.isDead || c.world != player.world || c.getDistanceSq(player) > 40D * 40D;
            if (lost && c != null && !c.isDead && c.world == player.world && story.talking(player)) {
                // Never pulled out from under a scene that might be using him.
                continue;
            }
            if (lost) {
                if (c != null && !c.isDead) {
                    c.setDead();
                }
                summon(player);
            }
        }
    }

    /** Down beside them, hard enough to crack the ground. */
    @Nullable
    private EntityChampion summon(EntityPlayerMP player) {
        WorldServer world = player.getServerWorld();
        Vec3d look = player.getLookVec();
        Vec3d flat = new Vec3d(look.x, 0D, look.z);
        flat = flat.lengthSquared() < 1.0E-4 ? new Vec3d(1D, 0D, 0D) : flat.normalize();
        // Off to one side and a little behind, so they turn and see him come down; wherever there is room for him.
        Vec3d side = new Vec3d(-flat.z, 0D, flat.x).scale(rand.nextBoolean() ? 1D : -1D);
        Vec3d at = null;
        double y = player.posY;
        for (double[] o : new double[][]{{3D, -1D}, {-3D, -1D}, {3D, -3D}, {-3D, -3D}, {0D, -3.5D}, {4.5D, 0D}, {-4.5D, 0D}, {2D, 2D}, {-2D, 2D}}) {
            Vec3d spot = player.getPositionVector().add(side.scale(o[0])).add(flat.scale(o[1]));
            BlockPos ground = Barrow.surface(world, (int) Math.floor(spot.x), (int) Math.floor(spot.z));
            if (Math.abs(ground.getY() - player.posY) <= 6D && room(world, spot.x, ground.getY(), spot.z)) {
                at = spot;
                y = ground.getY();
                break;
            }
        }
        if (at == null) {
            // Nowhere to stand round them (a tunnel, a cave): beside them, on their level.
            at = player.getPositionVector();
        }
        EntityChampion c = new EntityChampion(world);
        c.setLocationAndAngles(at.x, y, at.z, 0F, 0F);
        float yaw = (float) Math.toDegrees(Math.atan2(player.posZ - at.z, player.posX - at.x)) - 90F;
        c.rotationYaw = c.rotationYawHead = yaw;
        c.befriend(player);
        if (!world.spawnEntity(c)) {
            return null;
        }
        with.put(player.getUniqueID(), c);
        EternityNetwork.sendToDimension(new MsgTribunalFx(MsgTribunalFx.CHAMPION_IMPACT, c.posX, c.posY + 0.2D, c.posZ, 0D, 1D, 0D, -1, 2 + MsgTribunalFx.PLAIN),
                world.provider.getDimension());
        world.spawnParticle(EnumParticleTypes.CLOUD, c.posX, c.posY + 0.3D, c.posZ, 40, 1.2D, 0.2D, 1.2D, 0.12D);
        world.playSound(null, c.posX, c.posY, c.posZ, EternitySounds.COMET_IMPACT, SoundCategory.NEUTRAL, 2F, 0.9F);
        world.playSound(null, c.posX, c.posY, c.posZ, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.NEUTRAL, 1F, 0.7F);
        return c;
    }

    /** Whether there is room for all of him, standing at a point: nothing solid in the way, and none of the Gardener's hedge. */
    private static boolean room(WorldServer world, double x, double y, double z) {
        net.minecraft.util.math.AxisAlignedBB body = new net.minecraft.util.math.AxisAlignedBB(x - 0.6D, y + 0.05D, z - 0.6D, x + 0.6D, y + 3.3D, z + 0.6D);
        if (!world.getCollisionBoxes(null, body).isEmpty() || world.containsAnyLiquid(body)) {
            return false;
        }
        for (BlockPos p : BlockPos.getAllInBox(new BlockPos(body.minX, body.minY, body.minZ), new BlockPos(body.maxX, body.maxY, body.maxZ))) {
            if (world.getBlockState(p).getBlock() == com.matoon.eternity.block.EternityBlocks.GARDEN_THORN) {
                return false;
            }
        }
        return true;
    }

    /** What he gives for being brought home: the only thing he has left to give. */
    private void give(EntityPlayerMP player) {
        ItemStack ticket = new ItemStack(EternityItems.CHAMPION_TICKET);
        if (!player.inventory.addItemStackToInventory(ticket)) {
            player.dropItem(ticket, false);
        }
        EternityNetwork.send(MsgStory.line(DIRECTION, -1, "He pressed something into your hand before he went: a stub of a ticket from his Colosseum, "
                + "his mark stamped through it. Hold it up, and he'll come."), player);
    }

    /** The operator's handle: give the ticket again, or call him or send him off as if by it. */
    String command(EntityPlayerMP player, String[] args) {
        String how = args.length > 0 ? args[0] : "ticket";
        switch (how) {
            case "ticket":
                give(player);
                story.data().player(player.getUniqueID()).beats.add(TICKET);
                story.data().markDirty();
                return "His ticket.";
            case "call":
            case "dismiss":
                if (story.data().player(player.getUniqueID()).seen(CALLED) != "call".equals(how)) {
                    ticket(player);
                }
                return story.data().player(player.getUniqueID()).seen(CALLED) ? "He's with you." : "He's gone.";
            default:
                return "companion [ticket|call|dismiss]";
        }
    }
}
