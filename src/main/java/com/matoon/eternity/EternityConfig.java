package com.matoon.eternity;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Config(modid = Eternity.MODID, name = Eternity.MODID)
public class EternityConfig {

    @Config.Comment("How Eternity moves through the worlds.")
    public static Roaming roaming = new Roaming();

    @Config.Comment("Encounter balance.")
    public static Combat combat = new Combat();

    @Config.Comment("What happens if he dies and nobody contains him with the Astral Regulator.")
    public static Apocalypse apocalypse = new Apocalypse();

    @Config.Comment("The Cosmic Comet, the one thing in the universe that holds an Astral Regulator.")
    public static Comet comet = new Comet();

    @Config.Comment("The Living Tribunal, who comes for whoever caged Eternity.")
    public static Tribunal tribunal = new Tribunal();

    @Config.Comment("Client-side presentation. Each player can change these.")
    public static Client client = new Client();

    public static class Tribunal {
        @Config.Comment("Master switch. When false the Living Tribunal never comes.")
        public boolean enabled = true;

        @Config.Comment("Fewest minutes after Eternity is contained before the Tribunal takes whoever contained him.")
        @Config.RangeInt(min = 0, max = 100000)
        public int minDelayMinutes = 8;

        @Config.Comment("Most minutes after Eternity is contained before the Tribunal takes whoever contained him.")
        @Config.RangeInt(min = 0, max = 100000)
        public int maxDelayMinutes = 15;

        @Config.Comment("Dimension id of the Star Chamber, the Tribunal's courtroom. Change it if another mod uses it.")
        public int chamberDimension = -1931;

        @Config.Comment({"Points needed to be acquitted, out of 12. Every answer scores 2 at best.",
                "At 11 a single imperfect answer is forgiven; nothing more."})
        @Config.RangeInt(min = 0, max = 12)
        public int acquittalScore = 11;

        @Config.Comment({"Damage he takes before he is beaten to his knees, if you choose to fight him.",
                "Without a charged Astral Regulator in hand, your blows do a tenth as much."})
        @Config.RangeDouble(min = 1, max = 10000000)
        public double health = 8000.0;

        @Config.Comment({"How many Living Tribunals each world holds. Once that many have been absorbed into Astral",
                "Regulators, he never comes again. -1 for no limit."})
        @Config.RangeInt(min = -1, max = 100000)
        public int perWorld = 1;

        @Config.Comment("Seconds he stays down once beaten. Absorb him with the Regulator before he rises, fully healed.")
        @Config.RangeInt(min = 1, max = 600)
        public int downSeconds = 12;
    }

    public static class Comet {
        @Config.Comment("Master switch. When false the comet never appears.")
        public boolean enabled = true;

        @Config.Comment({"Average hours players spend in the overworld between sightings. It is only ever seen while",
                "someone is there to see it."})
        @Config.RangeDouble(min = 0.05, max = 100000)
        public double overworldMeanHours = 48.0;

        @Config.Comment("Average hours players spend in the space dimension between its visits there.")
        @Config.RangeDouble(min = 0.05, max = 100000)
        public double spaceMeanHours = 8.0;

        @Config.Comment("The space dimension, where it drifts slowly through the void.")
        public int spaceDimension = -30;

        @Config.Comment("Seconds it may spend beyond every player's render distance before it is gone.")
        @Config.RangeInt(min = 5, max = 3600)
        public int escapeSeconds = 180;

        @Config.Comment("Height it flies at over the overworld.")
        @Config.RangeInt(min = 260, max = 4000)
        public int overworldAltitude = 600;

        @Config.Comment("Height it drifts at in space.")
        @Config.RangeInt(min = 0, max = 4000)
        public int spaceAltitude = 180;

        @Config.Comment("Blocks per second it flies over the overworld.")
        @Config.RangeDouble(min = 0.5, max = 200)
        public double overworldSpeed = 14.0;

        @Config.Comment("Blocks per second it drifts in space.")
        @Config.RangeDouble(min = 0.1, max = 200)
        public double spaceSpeed = 2.5;

        @Config.Comment("In space it is so vast and bright it can be seen, and followed, from this many blocks away.")
        @Config.RangeInt(min = 64, max = 20000)
        public int spaceSightRange = 2500;

        @Config.Comment("Minutes it lingers in space before it moves on, unless someone is close to it.")
        @Config.RangeInt(min = 1, max = 100000)
        public int spaceVisitMinutes = 40;

        @Config.Comment("Radius of the rock, in blocks. There is only one comet, so it is the same size everywhere.")
        @Config.RangeDouble(min = 1, max = 60)
        public double radius = 4.0;

        @Config.Comment("Distance from its surface at which its heat starts to burn, over the overworld.")
        @Config.RangeDouble(min = 4, max = 1000)
        public double overworldHeatRange = 80.0;

        @Config.Comment("Distance from its surface at which its heat starts to burn, in space.")
        @Config.RangeDouble(min = 4, max = 1000)
        public double spaceHeatRange = 240.0;

        @Config.Comment({"Damage per second its heat deals at its surface, before the share of max health below.",
                "Armour, resistance and fire resistance do nothing against it. Only absorption soaks it up."})
        @Config.RangeDouble(min = 0, max = 100000)
        public double heatPerSecond = 30.0;

        @Config.Comment({"Share of the victim's max health its heat burns away per second at its surface.",
                "Because it scales with max health, extra hearts do not help. Absorption does."})
        @Config.RangeDouble(min = 0, max = 100)
        public double heatMaxHealthPerSecond = 1.0;

        @Config.Comment("Heat multiplier over the overworld, where its fire is thinned by the air.")
        @Config.RangeDouble(min = 0, max = 10)
        public double overworldHeatScale = 0.6;

        @Config.Comment({"How close to the edge of its hitbox you must be for blows to land, in blocks.",
                "Anything thrown from farther away burns up before it arrives."})
        @Config.RangeDouble(min = 1, max = 256)
        public double strikeReach = 8.0;

        @Config.Comment("Damage it takes before it cracks open.")
        @Config.RangeDouble(min = 1, max = 10000000)
        public double health = 6000.0;

        @Config.Comment("Largest share of its health a single blow can remove.")
        @Config.RangeDouble(min = 0.0001, max = 1)
        public double maxHitFraction = 0.03;
    }

    public static class Apocalypse {
        @Config.Comment({"Whether an uncontained death ends the world. When false, the universe simply heals",
                "after the supernova, as if it had been contained."})
        public boolean worldCanEnd = true;

        @Config.Comment("Minutes reality spends coming apart before the final collapse.")
        @Config.RangeInt(min = 1, max = 100000)
        public int unmakingMinutes = 30;

        @Config.Comment("Whether the unmaking corrupts and tears apart terrain around players.")
        public boolean corruptTerrain = true;

        @Config.Comment("Whether the final collapse erases the land around every player into nothing.")
        public boolean oblivionErasesTerrain = true;
    }

    public static class Roaming {
        @Config.Comment("Master switch. When false Eternity stops moving and never appears.")
        public boolean enabled = true;

        @Config.Comment("Shortest time, in minutes, between two of Eternity's rifts.")
        @Config.RangeInt(min = 1, max = 600)
        public int minRiftMinutes = 6;

        @Config.Comment("Longest time, in minutes, between two of Eternity's rifts.")
        @Config.RangeInt(min = 1, max = 600)
        public int maxRiftMinutes = 15;

        @Config.Comment("Seconds between a player completing all six stones and Eternity arriving.")
        @Config.RangeInt(min = 10, max = 3600)
        public int encounterDelaySeconds = 100;

        @Config.Comment("Minutes before Eternity returns to someone who still holds all six stones.")
        @Config.RangeInt(min = 1, max = 1440)
        public int repeatEncounterMinutes = 30;

        @Config.Comment("Minutes after being slain before Eternity reforms. -1 means never.")
        @Config.RangeInt(min = -1, max = 100000)
        public int rebirthMinutes = -1;

        @Config.Comment("Dimension ids Eternity will never enter.")
        public int[] blacklistedDimensions = new int[0];

        @Config.Comment("Seconds a rift stays open after Eternity passes through. Anyone who steps into it follows him.")
        @Config.RangeInt(min = 0, max = 600)
        public int riftLingerSeconds = 30;

        @Config.Comment("Size multiplier in Galacticraft space dimensions.")
        @Config.RangeDouble(min = 1, max = 20)
        public double spaceScale = 8.0;
    }

    public static class Combat {
        @Config.Comment({"Damage Eternity takes, scaled by how many stones the attacker holds (index = stone count).",
                "Without stones you cannot touch him."})
        public double[] stoneDamageScale = {0.0, 0.01, 0.03, 0.07, 0.14, 0.3, 1.0};

        @Config.Comment("Largest share of Eternity's health a single hit can remove.")
        @Config.RangeDouble(min = 0.001, max = 1)
        public double maxHitFraction = 0.035;

        @Config.Comment("Share of a victim's max health removed by Eternity's touch, when they hold all six stones.")
        @Config.RangeDouble(min = 0, max = 10)
        public double unmakeFraction = 0.3;

        @Config.Comment("Share of a victim's max health removed by Eternity's touch, when they do not hold all six.")
        @Config.RangeDouble(min = 0, max = 10)
        public double unmakeFractionUnworthy = 1.0;

        @Config.Comment("Whether his attacks tear up the terrain: black holes devour blocks, lances carve trenches, impacts leave craters.")
        public boolean destroyTerrain = true;

        @Config.Comment("Event horizon radius a black hole grows to, in blocks. Larger in space.")
        @Config.RangeDouble(min = 4, max = 64)
        public double singularityRadius = 40.0;

        @Config.Comment("Blocks a black hole may devour per tick. Lower this if it lags your server.")
        @Config.RangeInt(min = 0, max = 20000)
        public int singularityBlocksPerTick = 700;

        @Config.Comment("Health share Eternity regains per second when nobody worthy is fighting him.")
        @Config.RangeDouble(min = 0, max = 1)
        public double regenPerSecond = 0.01;
    }

    public static class Client {
        @Config.Comment("Strength of the darkening, fog and dread while Eternity shares your dimension.")
        @Config.RangeDouble(min = 0, max = 2)
        public double presenceStrength = 1.0;

        @Config.Comment("Camera shake during encounters and the finale.")
        public boolean screenShake = true;

        @Config.Comment("Softens full-screen flashes. Turn on if you are sensitive to flashing light.")
        public boolean reduceFlashing = false;

        @Config.Comment("Lets Eternity drag your gaze toward him when he stares at you.")
        public boolean forcedGaze = true;

        @Config.Comment("Applies post-processing distortion (chromatic split, desaturation) at key moments.")
        public boolean distortionShaders = true;
    }

    @Mod.EventBusSubscriber(modid = Eternity.MODID)
    private static class Sync {
        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (event.getModID().equals(Eternity.MODID)) {
                ConfigManager.sync(Eternity.MODID, Config.Type.INSTANCE);
            }
        }
    }
}
