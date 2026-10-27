package com.matoon.eternity.entity;

import net.minecraft.util.math.Vec3d;

/**
 * Whatever wears the Destroyer's body: the vault's own guardian, or the armour someone climbs
 * into. Its model and renderer draw either from this, and its ray gathers at {@link #face()}.
 */
public interface DestroyerBody {

    /** Client: how hot the furnace in its face burns, 0 to 1 and a little past it when it fires. */
    float heat(float partialTicks);

    /** Client: how far its face plates stand open, 0 to 1. */
    float faceOpen(float partialTicks);

    /** Client: how far the plates of its back stand open, 0 to 1. */
    float backOpen(float partialTicks);

    /** Where its ray comes from: the furnace in its face. */
    Vec3d face();
}
