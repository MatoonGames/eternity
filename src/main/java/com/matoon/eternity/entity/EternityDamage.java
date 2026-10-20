package com.matoon.eternity.entity;

import net.minecraft.entity.Entity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;

import javax.annotation.Nullable;

/**
 * Damage Eternity deals. None of it can be blocked, armoured against or enchanted
 * away, and creative mode is no protection.
 */
public final class EternityDamage {

    private EternityDamage() {
    }

    public static DamageSource unmake(@Nullable Entity eternity) {
        return absolute("eternity.unmade", eternity);
    }

    public static DamageSource gaze(@Nullable Entity eternity) {
        return absolute("eternity.gaze", eternity);
    }

    public static DamageSource lance(@Nullable Entity eternity) {
        return absolute("eternity.lance", eternity);
    }

    public static DamageSource collapse(@Nullable Entity eternity) {
        return absolute("eternity.collapse", eternity);
    }

    public static DamageSource starfall(@Nullable Entity eternity) {
        return absolute("eternity.starfall", eternity);
    }

    public static DamageSource shatter(@Nullable Entity eternity) {
        return absolute("eternity.shatter", eternity);
    }

    /**
     * His power in mortal hands, through the Astral Regulator. It ignores armour like
     * his own, but unlike his own it does not reach into creative mode.
     */
    public static DamageSource wielded(String type, Entity wielder) {
        return new EntityDamageSource("eternity." + type, wielder).setDamageBypassesArmor().setMagicDamage();
    }

    /**
     * The Cosmic Comet's heat. Armour, resistance, enchantments and fire resistance
     * are all useless against it. Only absorption stands between it and you.
     */
    public static DamageSource cometHeat() {
        return new DamageSource("eternity.comet").setDamageBypassesArmor().setDamageIsAbsolute().setMagicDamage();
    }

    /** The comet bursting apart. */
    public static DamageSource cometBlast() {
        return new DamageSource("eternity.comet_blast").setDamageBypassesArmor().setExplosion();
    }

    /** The Living Tribunal's judgement, delivered by hand. Armour means nothing to him. */
    public static DamageSource tribunal(Entity tribunal) {
        return new EntityDamageSource("eternity.tribunal", tribunal).setDamageBypassesArmor().setDamageIsAbsolute().setMagicDamage();
    }

    private static DamageSource absolute(String type, @Nullable Entity eternity) {
        DamageSource source = eternity != null ? new EntityDamageSource(type, eternity) : new DamageSource(type);
        return source.setDamageBypassesArmor().setDamageIsAbsolute().setMagicDamage().setDamageAllowedInCreativeMode();
    }
}
