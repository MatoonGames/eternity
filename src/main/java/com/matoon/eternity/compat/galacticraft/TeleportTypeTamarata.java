package com.matoon.eternity.compat.galacticraft;

import com.matoon.eternity.world.Tamarata;
import micdoodle8.mods.galacticraft.api.vector.Vector3;
import micdoodle8.mods.galacticraft.core.util.ConfigManagerCore;
import micdoodle8.mods.galacticraft.planets.mars.dimension.TeleportTypeMars;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldServer;

import java.util.Random;

/**
 * Arriving at Tamarata: down in landing balloons, as on Mars, but always onto the landing
 * site, a few blocks off the launch pad, wherever in the sky you came from.
 */
public class TeleportTypeTamarata extends TeleportTypeMars {

    @Override
    public Vector3 getPlayerSpawnLocation(WorldServer world, EntityPlayerMP player) {
        if (player == null) {
            return null;
        }
        Random random = new Random(player.getUniqueID().getLeastSignificantBits() ^ world.getTotalWorldTime());
        double a = random.nextDouble() * Math.PI * 2D;
        double r = 5D + random.nextDouble() * 3D;
        return new Vector3(Tamarata.LANDING_X + 0.5D + Math.cos(a) * r, ConfigManagerCore.disableLander ? 250D : 900D,
                Tamarata.LANDING_Z + 0.5D + Math.sin(a) * r);
    }

    @Override
    public Vector3 getEntitySpawnLocation(WorldServer world, Entity entity) {
        return new Vector3(Tamarata.LANDING_X + 0.5D, 250D, Tamarata.LANDING_Z + 4.5D);
    }
}
