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

    @Config.Comment("The Nexus of All Realities.")
    public static NexusSettings nexus = new NexusSettings();

    @Config.Comment("The In-Betweener, imprisoned at the point of absolute neutrality in the Nexus.")
    public static InBetweenerSettings inbetweener = new InBetweenerSettings();

    @Config.Comment("Adam Warlock, who wants the stones for himself.")
    public static Warlock warlock = new Warlock();

    @Config.Comment("The Champion of the Universe, and Tamarata, the war-ruined planet he lives on.")
    public static Champion champion = new Champion();

    @Config.Comment("Odin's Treasure Room, the vault under Asgard's palace: its guards, the Destroyer, and the Casket of Ancient Winters.")
    public static Vault vault = new Vault();

    @Config.Comment("Client-side presentation. Each player can change these.")
    public static Client client = new Client();

    public static class Vault {
        @Config.Comment("Dimension id of Odin's Vault. Change it if another mod uses it.")
        public int dimension = -1934;

        @Config.Comment("Seconds after someone enters the vault before the Destroyer wakes.")
        @Config.RangeInt(min = 0, max = 3600)
        public int destroyerDelay = 20;

        @Config.Comment("The Destroyer's health.")
        @Config.RangeDouble(min = 1, max = 100000)
        public double destroyerHealth = 800.0;

        @Config.Comment({"Share of a blow that gets through the Destroyer's armour from anyone not wielding something worthy",
                "of it: the Power Stone (held, or in a gauntlet in hand), Mjolnir, or a charged Astral Regulator."})
        @Config.RangeDouble(min = 0, max = 1)
        public double destroyerArmour = 0.12;

        @Config.Comment("Most of the Destroyer's health a single blow can take; a tenth of that without something worthy of it.")
        @Config.RangeDouble(min = 0.0001, max = 1)
        public double destroyerMaxHitFraction = 0.06;

        @Config.Comment({"Scales everything the Destroyer does to you. Its blows take shares of your maximum health, through",
                "armour: a punch 18%, its fists brought down together 26%, and its ray as long as you stand in it."})
        @Config.RangeDouble(min = 0, max = 10)
        public double destroyerDamageScale = 1.0;

        @Config.Comment("Minutes after it falls before Odin's smiths have the Destroyer standing in its niche again.")
        @Config.RangeInt(min = 0, max = 100000)
        public int destroyerRepairMinutes = 20;

        @Config.Comment("Each Einherjar guard's health.")
        @Config.RangeDouble(min = 1, max = 10000)
        public double einherjarHealth = 60.0;

        @Config.Comment("Damage of a spear thrust from an Einherjar. Armour turns it like any blow.")
        @Config.RangeDouble(min = 0, max = 1000)
        public double einherjarDamage = 8.0;

        @Config.Comment({"Whether the Casket of Ancient Winters changes the world it is opened on: water freezing, snow on",
                "the ground, ice erupting from it. Never inside the vault itself."})
        public boolean casketChangesTerrain = true;

        @Config.Comment("Chance that a fallen Einherjar leaves each piece of his armour behind (helm, cuirass, greaves, boots).")
        @Config.RangeDouble(min = 0, max = 1)
        public double einherjarArmourDrop = 0.5;

        @Config.Comment("Chance that a fallen Einherjar leaves his spear, and separately his shield, behind.")
        @Config.RangeDouble(min = 0, max = 1)
        public double einherjarWeaponDrop = 1.0;

        @Config.Comment("How much the Destroyer Armor can take before it goes down on its knees and needs mending with Uru (its base integrity, before upgrades).")
        @Config.RangeDouble(min = 1, max = 100000)
        public double armorHealth = 400.0;

        @Config.Comment("Damage of a punch from the Destroyer Armor.")
        @Config.RangeDouble(min = 0, max = 10000)
        public double armorPunchDamage = 24.0;

        @Config.Comment("Damage at the heart of the rings when the Destroyer Armor brings its fists down; less out toward their edge.")
        @Config.RangeDouble(min = 0, max = 10000)
        public double armorSlamDamage = 30.0;

        @Config.Comment("Damage the Destroyer Armor's ray does, five times a second, to whatever stands in it.")
        @Config.RangeDouble(min = 0, max = 10000)
        public double armorRayDamage = 5.0;

        @Config.Comment({"Whether the Destroyer Armor changes the world: its fists break soft blocks they land on, and its ray",
                "sets fires where it strikes. Never inside the vault, and only where mobGriefing allows it."})
        public boolean armorChangesTerrain = true;

        @Config.Comment({"How hungry the Destroyer Armor's furnace is: 1 burns lava at the normal rate, 0.5 half as fast, 0 never.",
                "A full furnace (8 buckets) lasts about ten minutes of walking."})
        @Config.RangeDouble(min = 0, max = 100)
        public double armorLavaBurn = 1.0;

        @Config.Comment("How much integrity the Destroyer Armor has left when claimed from the fallen Destroyer, from 0 to 1.")
        @Config.RangeDouble(min = 0.01, max = 1)
        public double armorClaimedIntegrity = 0.1;

        @Config.Comment({"How hard the mod's bosses hit the Destroyer Armor. Their blows take a share of a player's health; on someone",
                "inside the armour they take the same share of this much of its integrity (its base integrity times this). At 1 the",
                "armour is worth no more against them than its pilot; at 0.6 it lasts about two thirds longer."})
        @Config.RangeDouble(min = 0, max = 10)
        public double armorBossShare = 0.6;
    }

    public static class Champion {
        @Config.Comment("Master switch. When false he is not on Tamarata, though the planet is still there to visit.")
        public boolean enabled = true;

        @Config.Comment("Dimension id of Tamarata. Change it if another mod uses it. Galacticraft registers it.")
        public int dimension = -1933;

        @Config.Comment("Rocket tier needed to reach Tamarata.")
        @Config.RangeInt(min = 1, max = 10)
        public int tierRequired = 3;

        @Config.Comment({"Whether the Power Stone is set in his brow, the price of the rocket that gets him off his broken",
                "planet. There is only ever one: if a Power Stone already exists anywhere else, his brow is empty."})
        public boolean holdsPowerStone = true;

        @Config.Comment("His health. Knocked down, the stone heals him back up, so this only decides how often that happens.")
        @Config.RangeDouble(min = 1, max = 100000)
        public double health = 1500.0;

        @Config.Comment({"Most of his health any single blow can take. He blocks or slips most blows from the front anyway;",
                "beating him head-on is meant to be hopeless."})
        @Config.RangeDouble(min = 0.0001, max = 1)
        public double maxHitFraction = 0.015;

        @Config.Comment({"Scales everything he does to you. His blows take shares of your maximum health, through armour:",
                "a jab 6%, a haymaker 30%, a hammer quake up to 40%. Berserk, half again as much."})
        @Config.RangeDouble(min = 0, max = 10)
        public double damageScale = 1.0;

        @Config.Comment({"How fast his rage builds when you make a fool of him: every blow you slip, every second you stay",
                "out of his reach, every cheap shot from range, every taunt. 1 is normal."})
        @Config.RangeDouble(min = 0, max = 10)
        public double rageGain = 1.0;

        @Config.Comment("How fast his rage cools while you stand and trade blows with him honourably. 1 is normal.")
        @Config.RangeDouble(min = 0, max = 10)
        public double rageDecay = 1.0;

        @Config.Comment({"How far out from his arena his planet-breaking blow erases Tamarata. The rest of the planet breaks",
                "apart as it is seen. It never touches any other dimension."})
        @Config.RangeInt(min = 32, max = 400)
        public int destructionRadius = 160;

        @Config.Comment({"Chunks torn into rubble each tick while the planet breaks: first the wave out from his fists, then,",
                "once it has run its course, the whole rest of the planet anyone has loaded, all at once. Each chunk is",
                "cheap. Only a quarter of them may be chunks a player can see, since each of those is rebuilt on their screen.",
                "Lower it if the server struggles; the breaking only takes longer."})
        @Config.RangeInt(min = 1, max = 256)
        public int chunksPerTick = 24;
    }

    public static class Warlock {
        @Config.Comment("Damage he takes before he is driven off.")
        @Config.RangeDouble(min = 1, max = 1024)
        public double health = 600.0;

        @Config.Comment({"Most of his health any single blow can take. Landing, and reeling from a broken cocoon, he is open",
                "and takes half again as much."})
        @Config.RangeDouble(min = 0.001, max = 1)
        public double maxHitFraction = 0.04;

        @Config.Comment({"Scales everything he does to you. His blows take shares of your maximum health, through armour:",
                "a fly-by punch 30%, the combo up to half, the meteor dive up to a third. Evolved, a quarter more."})
        @Config.RangeDouble(min = 0, max = 10)
        public double damageScale = 1.0;

        @Config.Comment("Damage his cocoon takes before it breaks, as a share of his maximum health.")
        @Config.RangeDouble(min = 0.01, max = 10)
        public double cocoonStrength = 0.2;

        @Config.Comment("Share of his maximum health he comes out of an unbroken cocoon with.")
        @Config.RangeDouble(min = 0.01, max = 1)
        public double evolvedHealth = 0.65;
    }

    public static class InBetweenerSettings {
        @Config.Comment("Master switch. When false he is not there, and nobody can speak to him.")
        public boolean enabled = true;

        @Config.Comment({"Whether the Soul Stone is set in his brow, for anyone clever or cruel enough to take it. There is",
                "only ever one: if a Soul Stone already exists anywhere else in the world, his brow is empty."})
        public boolean holdsSoulStone = true;

        @Config.Comment("Seconds after his glass breaks before Master Order and Lord Chaos arrive, and his empty body is cast off into the Nexus.")
        @Config.RangeInt(min = 15, max = 3600)
        public int wrathSeconds = 60;

        @Config.Comment({"Anyone this close to the junction when they arrive must face them: first in words, then in battle.",
                "Farther away, you have escaped."})
        @Config.RangeInt(min = 8, max = 1024)
        public int wrathRadius = 64;

        @Config.Comment({"Damage it takes, in total, to break his glass while he presses from within.",
                "Only blows at least as strong as shatterBlow count at all."})
        @Config.RangeDouble(min = 1, max = 100000)
        public double shatterStrength = 300.0;

        @Config.Comment({"The weakest blow his glass even notices: your attack damage with the weapon in hand, its enchantments,",
                "how fully the swing was charged, and a critical. A charged Astral Regulator or the Power Stone in hand doubles it."})
        @Config.RangeDouble(min = 0, max = 10000)
        public double shatterBlow = 12.0;

        @Config.Comment("Damage Master Order and Lord Chaos each take before they withdraw.")
        @Config.RangeDouble(min = 1, max = 10000000)
        public double mastersHealth = 1500.0;

        @Config.Comment("Largest share of either one's health a single blow can remove.")
        @Config.RangeDouble(min = 0.0001, max = 1)
        public double mastersMaxHitFraction = 0.02;

        @Config.Comment({"Minutes he keeps his back turned, refusing to speak, after you offend him or he sees through you.",
                "Outside his glass that is all he can do; he cannot make you leave, and you can keep pestering him."})
        @Config.RangeInt(min = 0, max = 100000)
        public int castOutMinutes = 5;
    }

    public static class NexusSettings {
        @Config.Comment("Dimension id of the Nexus of All Realities. Change it if another mod uses it.")
        public int dimension = -1932;
    }

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
        public double heatDamage = 15.0;

        @Config.Comment({"Share of the victim's max health its heat burns away per second at its surface.",
                "Because it scales with max health, extra hearts do not help. Absorption does."})
        @Config.RangeDouble(min = 0, max = 100)
        public double heatMaxHealthShare = 0.5;

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

        @Config.Comment({"In the Nexus of All Realities the horizon leans gently and the view slowly breathes.",
                "Off, or with reduceFlashing on, the view holds still."})
        public boolean nexusWarp = true;
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
