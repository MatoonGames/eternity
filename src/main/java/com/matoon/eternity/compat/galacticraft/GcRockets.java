package com.matoon.eternity.compat.galacticraft;

import micdoodle8.mods.galacticraft.api.entity.IRocketType;
import micdoodle8.mods.galacticraft.api.prefab.entity.EntitySpaceshipBase;
import micdoodle8.mods.galacticraft.core.entities.EntityTier1Rocket;
import micdoodle8.mods.galacticraft.core.items.ItemTier1Rocket;
import micdoodle8.mods.galacticraft.planets.asteroids.entities.EntityTier3Rocket;
import micdoodle8.mods.galacticraft.planets.asteroids.items.ItemTier3Rocket;
import micdoodle8.mods.galacticraft.planets.mars.entities.EntityTier2Rocket;
import micdoodle8.mods.galacticraft.planets.mars.items.ItemTier2Rocket;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * Galacticraft's rockets, for the Champion's trade: which items are rockets (tiers one to
 * three; not cargo rockets, which carry no one), and the rocket itself, set down in the world
 * beside him for him to climb into. Everything here is in vanilla types to the outside.
 */
public final class GcRockets {

    private GcRockets() {
    }

    /** The tier of rocket an item is, or 0 if it is not one he could fly away in. */
    public static int tier(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        Item item = stack.getItem();
        if (item instanceof ItemTier1Rocket) {
            return 1;
        }
        if (item instanceof ItemTier2Rocket) {
            return 2;
        }
        if (item instanceof ItemTier3Rocket) {
            return 3;
        }
        return 0;
    }

    /**
     * The rocket an item would make, standing at a spot, held there and never launched by
     * Galacticraft itself: whoever spawns it moves it. Null if the item is not a rocket.
     */
    @Nullable
    public static Entity rocket(World world, ItemStack stack, double x, double y, double z) {
        IRocketType.EnumRocketType[] types = IRocketType.EnumRocketType.values();
        IRocketType.EnumRocketType type = types[Math.max(0, Math.min(types.length - 1, stack.getItemDamage()))];
        EntitySpaceshipBase rocket;
        switch (tier(stack)) {
            case 1:
                rocket = new EntityTier1Rocket(world, x, y, z, type);
                break;
            case 2:
                rocket = new EntityTier2Rocket(world, x, y, z, type);
                break;
            case 3:
                rocket = new EntityTier3Rocket(world, x, y, z, type);
                break;
            default:
                return null;
        }
        rocket.setNoGravity(true);
        return rocket;
    }

    /** Whether an entity is one of Galacticraft's rockets. */
    public static boolean isRocket(@Nullable Entity entity) {
        return entity instanceof EntitySpaceshipBase;
    }
}
