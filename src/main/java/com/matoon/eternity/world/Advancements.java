package com.matoon.eternity.world;

import com.matoon.eternity.Eternity;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;

/** Grants this mod's advancements, which all use impossible criteria. */
public final class Advancements {

    public static final String ROOT = "root";
    public static final String WITNESS = "witness";
    public static final String SURVIVOR = "survivor";
    public static final String SLAYER = "the_slayer_of_eternity";
    public static final String KEEPER = "keeper_of_reality";
    public static final String COMET = "comet_breaker";

    private Advancements() {
    }

    public static void grant(EntityPlayerMP player, String name) {
        if (player.getServer() == null) {
            return;
        }
        Advancement advancement = player.getServer().getAdvancementManager().getAdvancement(new ResourceLocation(Eternity.MODID, name));
        if (advancement == null) {
            return;
        }
        AdvancementProgress progress = player.getAdvancements().getProgress(advancement);
        if (progress.isDone()) {
            return;
        }
        for (String criterion : progress.getRemaningCriteria()) {
            player.getAdvancements().grantCriterion(advancement, criterion);
        }
    }
}
