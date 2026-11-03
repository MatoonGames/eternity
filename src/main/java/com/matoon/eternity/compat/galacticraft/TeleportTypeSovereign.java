package com.matoon.eternity.compat.galacticraft;

import com.matoon.eternity.world.SovereignStation;
import micdoodle8.mods.galacticraft.api.vector.Vector3;
import micdoodle8.mods.galacticraft.api.world.ITeleportType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.Random;

/**
 * Docking at the Sovereign: no balloons, no landers. The station brings you in itself, onto
 * its landing ring, as it was built to bring in its own.
 */
public class TeleportTypeSovereign implements ITeleportType {

    @Override
    public boolean useParachute() {
        return false;
    }

    @Override
    public Vector3 getPlayerSpawnLocation(WorldServer world, EntityPlayerMP player) {
        return new Vector3(SovereignStation.LANDING.getX() + 0.5D, SovereignStation.LANDING.getY() + 0.1D, SovereignStation.LANDING.getZ() + 0.5D);
    }

    @Override
    public Vector3 getEntitySpawnLocation(WorldServer world, Entity entity) {
        return new Vector3(SovereignStation.LANDING.getX() + 0.5D, SovereignStation.LANDING.getY() + 1D, SovereignStation.LANDING.getZ() - 3.5D);
    }

    @Override
    public Vector3 getParaChestSpawnLocation(WorldServer world, EntityPlayerMP player, Random rand) {
        return null;
    }

    @Override
    public void onSpaceDimensionChanged(World newWorld, EntityPlayerMP player, boolean ridingAutoRocket) {
    }

    @Override
    public void setupAdventureSpawn(EntityPlayerMP player) {
    }
}
