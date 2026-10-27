package com.matoon.eternity.world;

import com.matoon.eternity.entity.EntityDestroyerArmor;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.common.util.ITeleporter;

/** Moves a player to an exact spot, in any dimension, without vanilla portal logic. */
public final class Teleport {

    private Teleport() {
    }

    /**
     * @param safe when true, {@code y} is only a hint and the player lands on the
     *             nearest solid ground with room to stand
     */
    public static void player(EntityPlayerMP player, int dimension, double x, double y, double z, boolean safe) {
        if (player.dimension == dimension) {
            double landY = safe ? Terrain.safeY(player.world, x, y, z) : y;
            // Inside the Destroyer Armor, the armour goes, with them in it.
            EntityDestroyerArmor.place(player, x, landY, z, player.rotationYaw, player.rotationPitch);
        } else {
            ITeleporter teleporter = (world, entity, yaw) -> {
                double landY = safe ? Terrain.safeY(world, x, y, z) : y;
                entity.setLocationAndAngles(x, landY, z, entity.rotationYaw, entity.rotationPitch);
                entity.motionX = entity.motionY = entity.motionZ = 0;
            };
            player.changeDimension(dimension, teleporter);
        }
        player.fallDistance = 0F;
    }
}
