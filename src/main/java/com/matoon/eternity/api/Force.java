package com.matoon.eternity.api;

import lucraft.mods.lucraftcore.infinity.EnumInfinityStone;
import lucraft.mods.lucraftcore.superpowers.abilities.Ability;
import lucraft.mods.lucraftcore.util.abilitybar.EnumAbilityBarColor;

/**
 * Who or what a power belongs to, and the colour of the frame Lucraft's ability bar draws round it, so that a bar full
 * of borrowed powers still says at a glance whose each one is. The stones keep InfinityCraft's own colours; the beings
 * the Astral Regulator holds each take one no stone uses.
 * <p>
 * An addon's own being or force picks any {@link EnumAbilityBarColor} and tints its abilities with
 * {@link EternityAPI#tint(Ability, EnumAbilityBarColor)}; these are only the ones Eternity itself uses.
 */
public enum Force {
    ETERNITY(EnumAbilityBarColor.MAGENTA),
    LIVING_TRIBUNAL(EnumAbilityBarColor.BROWN),
    MASTER_ORDER(EnumAbilityBarColor.WHITE),
    LORD_CHAOS(EnumAbilityBarColor.CYAN),
    IN_BETWEENER(EnumAbilityBarColor.GRAY),
    DEATH(EnumAbilityBarColor.BLACK),
    SPACE_STONE(EnumAbilityBarColor.LIGHT_BLUE),
    MIND_STONE(EnumAbilityBarColor.YELLOW),
    REALITY_STONE(EnumAbilityBarColor.RED),
    POWER_STONE(EnumAbilityBarColor.PURPLE),
    TIME_STONE(EnumAbilityBarColor.GREEN),
    SOUL_STONE(EnumAbilityBarColor.ORANGE);

    public final EnumAbilityBarColor color;

    Force(EnumAbilityBarColor color) {
        this.color = color;
    }

    /** Frames the ability in this force's colour, and hands it back. */
    public <T extends Ability> T tint(T ability) {
        return EternityAPI.tint(ability, color);
    }

    public static Force of(EnumInfinityStone stone) {
        switch (stone) {
            case SPACE:
                return SPACE_STONE;
            case MIND:
                return MIND_STONE;
            case REALITY:
                return REALITY_STONE;
            case POWER:
                return POWER_STONE;
            case TIME:
                return TIME_STONE;
            default:
                return SOUL_STONE;
        }
    }
}
