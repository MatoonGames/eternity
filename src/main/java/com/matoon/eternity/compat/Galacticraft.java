package com.matoon.eternity.compat;

import net.minecraft.world.World;

/**
 * Galacticraft Legacy hooks. Accessed reflectively so the mod compiles without the
 * (coremod-carrying, reobfuscated) Galacticraft jar on the classpath.
 */
public final class Galacticraft {

    private static final String SPACE_PROVIDER = "micdoodle8.mods.galacticraft.api.world.IGalacticraftWorldProvider";

    private static Class<?> spaceProvider;
    private static boolean resolved;

    private Galacticraft() {
    }

    /** True for the Moon, planets, asteroids and orbital space stations. */
    public static boolean isSpace(World world) {
        if (world == null) {
            return false;
        }
        if (!resolved) {
            resolved = true;
            try {
                spaceProvider = Class.forName(SPACE_PROVIDER, false, Galacticraft.class.getClassLoader());
            } catch (ClassNotFoundException e) {
                spaceProvider = null;
            }
        }
        return spaceProvider != null && spaceProvider.isInstance(world.provider);
    }
}
