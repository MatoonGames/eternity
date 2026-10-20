# Eternity

A Minecraft 1.12.2 Forge mod. Something vast moves between the worlds.

Requires Lucraft: Core, InfinityCraft, HeroesExpansion, Galacticraft Legacy and Tickrate Changer.

## How it plays

- **He is always somewhere.** Eternity is persistent world state, not a mob. He drifts
  through a dimension and every few minutes tears a rift and steps into another one,
  or somewhere else in the same one. In a new world he starts anywhere, in any
  dimension he may enter: the overworld, the Nether, the End, or space. His body only
  exists when someone is close.
- **You can feel him.** While he shares your dimension the world dims, fog closes in,
  the music stops, and things are carried to you on the wind: distant thunder, groans,
  chains, horns. They come from his direction. Closer means more often and louder.
- **You can see him coming.** His rifts are cracks across the whole sky, hundreds of
  blocks tall, visible to the entire dimension at any distance and through terrain.
- **You can follow him.** A rift stays open for a while after he passes through it
  (`riftLingerSeconds`). Step into its heart and you come out where he went.
- **The stones draw him.** Each new stone type you take changes the world around you
  without a word: a silence, a shimmer behind you, a rift on the horizon only you saw.
  The more you hold, the more restless he becomes and the closer his rifts land.
  Collect all six and he **will** come. Nothing is ever taken from you.
- **Without the stones you are nothing to him.** Blows pass through him. Only someone
  holding stones can hurt him, and only all six really matter. Creative mode does not
  protect you.
- **His attacks are cataclysms.** A touch that unmakes and hurls everyone nearby; black
  holes that swell to 40 blocks, drag in everything and devour the terrain before
  detonating; starfall raining craters; the ground shattering in rings of eruptions;
  lances of void that carve trenches. Terrain damage can be turned off with
  `destroyTerrain`.
- **In space he is a colossus.** In Galacticraft dimensions he is many times larger
  and can be seen drifting through the void from thousands of blocks away.
- **If he dies, everyone knows.** The finale plays in every dimension at once: reality
  fractures, rifts tear open across every sky, and time itself slows toward a stop
  (through Tickrate Changer) as everything collapses toward a supernova.
- **Only the Astral Regulator can save the universe.** Use it on him while he dies,
  before the supernova, and it floats into his heart and drinks back everything he is
  pouring out. Time recovers, the rifts seal, and the charged Regulator falls to the
  ground as an indestructible Lucraft object. Things go back to normal.
- **If nobody does, the world ends.** For `unmakingMinutes` (30 by default) reality
  comes apart: time lurches, gravity fails, land turns into things it should not be,
  pieces of the world drift into the sky, holes open onto nothing, and the sky itself
  shatters like glass, the void showing through. Then the final collapse erases the land around every player, the
  light goes out, and only the void remains. Turn this off with `worldCanEnd`.

## The Stone Finder

A golden compass whose needle points at the nearest Infinity Stone in your dimension:
in a chest or any other container, in another player's inventory or ender chest,
dropped on the ground (including Lucraft's indestructible item entities), in an item
frame, held by a mob, or socketed in a gauntlet or packed in a shulker box anywhere in
that list. Stones you carry yourself are ignored. A stone in an unloaded area is shown
at its last known location; in older worlds, an InfinityCraft stone nobody has found yet
points to its temple. With nothing to find, the needle spins.

InfinityCraft's stone temples are turned off: new chunks never generate them. Temples
already generated in existing worlds are left alone.

Craft it from a compass in the middle, gold ingots around it and an eye of ender below.

## The Living Tribunal

Cage Eternity in the Astral Regulator and you will have drawn the attention of the one
being above him. Some minutes later (8 to 15 by default) the Living Tribunal comes for
whoever released the Regulator.

- **He takes you.** Without warning he is there in front of you, vast, filling the sky:
  a golden giant with a burning white void in his chest, his head floating above his
  shoulders under a purple hood, with three faces. He reaches out and his hand closes
  over you. You are dragged through a wormhole between worlds.
- **The Star Chamber.** A courtroom outside every universe: a hall of obsidian veined
  with light, pillars of white stone, a golden dais, and at the far end the Tribunal,
  sixty blocks tall on his throne. There is no leaving: no portal, command, death or
  disconnect gets you out. Log out and the trial starts again when you return. Nothing
  can hurt you there and nothing can be built or broken.
- **The trial.** He asks six questions, and the chamber shakes as he speaks. Each has
  four answers. The best answers are humble but unbowed. Grovelling earns nothing, and
  defiance is remembered. Acquittal needs 11 of 12 points: one imperfect answer at most.
- **The verdict.** Acquitted, you are sent home through the wormhole to where you were
  taken. Convicted, you lose everything: your inventory, armour, ender chest, Lucraft's
  extra slots, your superpower and all its abilities, every level, every potion effect,
  and Eternity's memory of every stone you ever held. Convicted after defying him twice
  or more, you are cast out: disconnected and
  banned from that world for good. In singleplayer that means your own world. Only an
  operator running `/eternity tribunal pardon <player>` from the console can let you back.
- **Or fight him.** Every question has a fifth answer, marked in red: refuse to be judged.
  He rises from his throne, nearly sixty blocks tall, and fights. Every time he gets back
  up he *ascends*, wielding more of his power, a quarter harder each time and faster:

  | Ascension | New judgements |
  | --- | --- |
  | Start | **The Gavel**: a golden sigil where you stand, then his fist. **Equity's Gaze**: beams of white fire sweep the floor after you. **Necessity's Storm**: lightning falls on every spot a sigil marks. **The Scales of Balance**: two great pans of light crash down either side of you, one after the other |
  | I | **Sentence**: a cage of light holds you and bursts. **Edict of Exile**: the floor opens onto the void and drags you in. **Convergence**: golden blades circle you and close |
  | II | **Pillars of Creation**: columns of light march across the floor at you. **Cosmic Verdict**: a ring of light races across the whole hall; jump it or be struck. **Weight of Judgement**: crushing gravity pins you to the floor |
  | III | **Erasure**: a wall of light sweeps the length of the hall with a single gap to stand in. **Multiversal Echoes**: ghosts of him from other universes bring their fists down beside his |

  The newest judgements come up most often. Blows only count from close to his body
  (melee and short reach), and without a charged Astral Regulator in hand they do a tenth
  as much. No blow takes more than 2.5% of him. He cannot die: beaten, he falls to his
  knees for 12 seconds (`downSeconds`). Raise a charged Regulator at him in that moment
  and he is drawn into it, streaming into your hand, and you are sent home. There is only
  one Living Tribunal in each world (`tribunal.perWorld`, -1 for no limit): once he has been
  absorbed, he never comes for anyone again. Miss the moment and he rises, fully healed and
  one ascension higher.
  If he beats you, you do not die; he strips you of everything and sends you home.


There is exactly one Cosmic Comet in each world, and it holds the only Astral Regulator.

- **Over the overworld, very rarely.** A glowing blue rock crosses the sky about 600
  blocks up, trailing a long blue ion tail and a broad, pale dust tail. Chase it. If it
  stays beyond every player's render distance for more than `escapeSeconds` (3 minutes),
  it is gone. It may come back one day, with the wounds you gave it.
- **In space (dimension -30), still rare but more often.** The same comet, the same size,
  but there it drifts slowly, visible from 2,500 blocks away, for up to 40 minutes.
- **It burns you.** Inside its heat everything takes damage twice a second, rising
  sharply the closer you get. Armour, resistance, enchantments and fire resistance do
  nothing. The damage scales with max health, so extra hearts don't help either. Only a
  huge pool of absorption keeps you alive long enough to break it. Creative and
  spectator players are not burned.
- **Only close blows count.** You must be within `strikeReach` (8 blocks) of its hitbox,
  which reaches well past the rock. Anything thrown or fired from farther away burns up before it lands.
  Every tenth of its health it loses, it flares and hurls everyone back.
- **Breaking it open.** When it runs out of health it breaks open over five seconds,
  then explodes, flinging shards and leaving a glowing cloud. The Astral Regulator hangs
  in the air where its heart was, glowing, with a pillar of light that every player in
  that dimension can see from any distance until someone takes it. Whoever broke it is
  announced to the server, and the
  comet never appears in that world again.

## The Astral Regulator

A faceted crystal of pale light with a gold ring set inside it. It cannot be crafted: the
only one is inside the Cosmic Comet. Right-click while looking at Eternity as he dies.
Like the stones, it cannot be destroyed.

Once charged its name burns purple, and its tooltip becomes a window into the crystal:
the absorbed being turning slowly inside the orb, with the powers it grants listed
beside it. Hold it in either hand and those powers are yours, as Lucraft abilities on
the ability bar:

| Ability | Cooldown | Effect |
| --- | --- | --- |
| Unmake | 2s | Tears apart whatever you reach for (40 damage) and hurls everything near it |
| Void Lances | 5s | Six rifts open around your target and fire lances that carve the ground |
| Starfall | 15s | Fourteen stars rain down where you look |
| Shatter | 12s | The ground erupts in rings racing outward from where you look |
| Singularity | 60s | A black hole opens where you look (at least 12 blocks away) |
| Rift Step | 3s | Step through a rift to wherever you are looking, up to 80 blocks |
| Rift Gate | 60s | Opens a screen for a dimension and coordinates, then tears one of his great rifts open in front of you leading there. Anyone can walk through until both ends seal (`riftLingerSeconds`, at least 10s) |

Once the Regulator has absorbed the Living Tribunal too, his judgements join them:

| Ability | Cooldown | Effect |
| --- | --- | --- |
| The Gavel | 4s | A golden sigil where you look, then a fist out of nowhere (40 damage, hurls) |
| Equity's Gaze | 8s | Twin beams of white fire from your eyes for two seconds, following your look |
| Necessity's Storm | 10s | Eight sigils around where you look, each struck by lightning |
| The Scales of Balance | 15s | Two pans of light crash down either side of where you look, one after the other |
| Sentence | 20s | A cage of light holds whatever you look at for two and a half seconds, then bursts (50 damage) |
| Convergence | 15s | Golden blades circle where you look and meet (45 damage) |
| Pillars of Creation | 12s | Twelve columns of light erupt in a line marching away from you |
| Cosmic Verdict | 60s | A ring of light bursts out 30 blocks from you, striking everything it crosses |

Wielded power ignores armour but, unlike his own, does not reach into creative mode.
Terrain damage follows `destroyTerrain`.

Holding a charged Regulator in either hand also gives you Eternity's endurance: his 1,024
health is added to your maximum and to your current health at once, and no single blow
can take more than `maxHitFraction` (3.5%) of your maximum, just as with him. Vanilla
caps maximum health at 1,024, so that is as high as it goes. Put it away and the extra
health goes with it.

It is also a source of Forge Energy that never runs dry: anything can draw from it (it
cannot be filled), and carried anywhere in your inventory it keeps everything else there
that runs on FE charged. It gives 10k FE/t empty, 1.01M FE/t holding Eternity, and 101M
FE/t once it holds the Living Tribunal too.

What it holds shows. With Eternity inside, its heart is a universe of violet stars; once
it has absorbed the Living Tribunal it evolves: the crystal warms to gold, a golden sun
burns at its heart, and his power circles it in three turning rings of gold, rays of light
and drifting motes. Its tooltip shows every being inside it, side by side in the crystal,
in a frame with lights racing around it.

## Commands (op level 2)

| Command | Effect |
| --- | --- |
| `/eternity status` | Where he is, what he is doing, and each player's stone count |
| `/eternity summon` | He tears open a rift in front of you and hunts you |
| `/eternity travel <dim>` | Sends him to another dimension |
| `/eternity hunt [player]` / `release` | Starts or ends a hunt |
| `/eternity omen <1-6> [player]` | Plays a stone omen (6 includes the arrival heralds) |
| `/eternity stones [player]` | Lists the stones a player is carrying |
| `/eternity wound <percent>` | Damages him, for testing the fight |
| `/eternity slay` | Starts the finale immediately |
| `/eternity doom [0-1]` | Skips straight into the end of the world, at the given progress |
| `/eternity restore` | Heals reality from the unmaking or the void, and gives players their game mode back |
| `/eternity reset` | Brings him back and forgets every player's stones |
| `/eternity comet [status]` | Where the comet is, its integrity, and how long it has been out of sight |
| `/eternity comet summon` | Brings it into your sky (full size if you are in space) |
| `/eternity comet wound <percent>` | Damages it, for testing |
| `/eternity comet depart` | Lets it slip away |
| `/eternity tribunal [status]` | Whom he is coming for or judging, and who has been cast out |
| `/eternity tribunal summon [player]` | He takes them now |
| `/eternity tribunal pardon <player>` | Lets someone he cast out return |
| `/eternity comet restore` | Makes it whole and lets it return, even after it was broken open |
| `/tpdimension <player> <dim> [x y z]` | Sends a player to another dimension: to its spawn on safe ground, or to exact coordinates (`~` is relative to the player) |

## Configuration

`config/eternity.cfg` covers rift timing, encounter delays, rebirth after death,
blacklisted dimensions, space scale, the damage table by stone count, the Cosmic Comet
(how rare it is, its space dimension, size, speed, heat, health and escape time), and client
options: presence strength, screen shake, reduced flashing, forced gaze and
distortion shaders.

## Building

```
./gradlew build
```

Lucraft, HeroesExpansion and InfinityCraft deobfuscated jars live in `libs/`.
Galacticraft is reached by reflection, so it is not needed to compile. To run the
dev client or server, put the Galacticraft Legacy release jar in `run/mods/`.

Sounds are converted from the original WAV pack with
`tools/convert_sounds.sh <wav dir> [ffmpeg]`. They come from Freesound; see the
credits in `mcmod.info`.

The Living Tribunal's skin and glow layer are painted by `tools/TribunalSkin.java`:
`javac -d out tools/TribunalSkin.java` then
`java -cp out TribunalSkin src/main/resources/assets/eternity/textures/entity`. Its box
layout must match `client/render/ModelTribunal.java`.
