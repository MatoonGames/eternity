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

## The Relic Tracker

A golden compass whose needle points at the nearest relic in your dimension: an Infinity
Stone (the Tesseract among them) or an Astral Regulator. It finds them in a chest or any
other container, in another player's inventory or ender chest, dropped on the ground
(including Lucraft's indestructible item entities), in an item frame, held by a mob, in
flight into Eternity's heart, or socketed in a gauntlet or packed in a shulker box anywhere
in that list. Relics you carry yourself are ignored. A relic in an unloaded area is shown
at its last known location; in older worlds, an InfinityCraft stone nobody has found yet
points to its temple. In the Nexus, the Soul Gem on the In-Betweener's brow counts too.
Its tooltip names what the needle is drawn to. With nothing to find, the needle spins.

InfinityCraft's stone temples are turned off: new chunks never generate them. Temples
already generated in existing worlds are left alone.

Craft it from a compass in the middle, gold ingots around it and an eye of ender below.
Stone Finders from older worlds become Relic Trackers.

## One true relic

There is only one of each relic, and the mod remembers who has it.

- **The first one taken is the true one.** The first player to take a Tesseract claims it,
  and the same goes for the Soul Stone (the gem on the In-Betweener's brow, or any found
  before it). The claimed stone is marked; the Relic Tracker, Eternity and everything else
  that counts stones only recognise that copy. If Order and Chaos send the gem back to the
  In-Betweener's brow it is nobody's again, until someone takes it.
- **The Tesseract.** HeroesExpansion sets one out on a pedestal in a Norse village. Once one
  has been claimed, no village sets out another, and any unclaimed one set out earlier is
  gone the next time its village loads. Punch the true Tesseract and it breaks open: the
  Tesseract is gone, and the Space Stone that falls out is the true relic from then on (the
  Relic Tracker forgets the empty Tesseract and follows the stone).
- **It is redrawn.** The Tesseract is a cube of clear glass with the Space Stone in its
  heart, a white star. Its light fills the cube in deep blue and turquoise clouds, and flakes
  of light come loose from it and drift slowly outward, fading before the glass. The glass
  shows only as glass does: brighter toward its edges, with a soft sheen. In the world and in
  hand its light flares slowly in the eye: a long blue streak, a turning starburst and a
  faint violet ring, swelling and fading every six seconds or so. Hold it in either hand and it hums, deep and throbbing, for you and for
  anyone near you; on the ground it hums more quietly.
- **Relics cannot be destroyed.** Not by fire, lava or explosions (Lucraft already sees to
  that), not by Eternity's black holes, which swallow everything else that is not alive,
  and not by the void: a stone or Regulator that falls out of the world is held at its edge.

`/eternity relics` shows who claimed each relic and when, and where every relic was last
seen and who was carrying it.

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
- **The trial.** He asks six questions, and the chamber shakes as he speaks. While he speaks
  your view turns up to his face on the throne. Each question has four answers. The best
  answers are humble but unbowed. Grovelling earns nothing, and defiance is remembered.
  Acquittal needs 11 of 12 points: one imperfect answer at most.
- **The verdict.** Acquitted, you are sent home through the wormhole to where you were
  taken. Convicted, you lose everything: your inventory, armour, ender chest, Lucraft's
  extra slots, your superpower and all its abilities, every level, every potion effect,
  and Eternity's memory of every stone you ever held. Convicted after defying him twice
  or more, you are cast out: disconnected and
  banned from that world for good. In singleplayer that means your own world. Only an
  operator running `/eternity tribunal pardon <player>` from the console can let you back.
- **Or fight him.** Every question has a fifth answer, marked in red: refuse to be judged.
  He rises from his throne, nearly sixty blocks tall, and fights. Every time he gets back
  up he *ascends*, wielding more of his power, a quarter harder each time and faster.
  Creative mode does not protect you from him:

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
  big pool of absorption keeps you alive long enough to break it. At its surface it
  burns `heatDamage` (15) plus `heatMaxHealthShare` (half your max health) every second.
  Creative and spectator players are not burned.
- **Only close blows count.** You must be within `strikeReach` (8 blocks) of its hitbox,
  which reaches well past the rock. Anything thrown or fired from farther away burns up before it lands.
  Every tenth of its health it loses, it flares and hurls everyone back.
- **Breaking it open.** When it runs out of health it breaks open over five seconds,
  then explodes, flinging shards and leaving a glowing cloud. The Astral Regulator hangs
  in the air where its heart was, glowing, with a pillar of light that every player in
  that dimension can see from any distance until someone takes it. Whoever broke it is
  announced to the server, and the
  comet never appears in that world again.

## The Nexus of All Realities

The place where every universe touches every other (dimension -1932, `nexus.dimension`).
Reach it through a Rift Gate, by following Eternity through one of his rifts when he wanders
there, or with `/eternity nexus`.

- **What drifted in.** An endless neon void strewn with pieces of other realities, stacked
  five layers deep above the abyss to near the top of the world, so there is always
  something above and below you, and the same every visit. Islands floating on nothing, no
  two alike: some a single round mass, some sprawling over several lobes with ragged edges,
  flat or rolling in hills, hanging beneath them in a shallow bowl or a long jagged point;
  grassed in swamp green, pink bloom or dark mire, banded underneath in violet, dusk blue or
  ember orange, with bouncy slime pools in their hollows, roots hanging from them and little
  rocks drifting beside them. Some carry groves of magenta mushrooms taller than towers on
  wavering striped stalks, domed, flat or bell-shaped, their gills faintly glowing. Eyeballs
  the size of houses; magenta orbs with holes bored through them; geodesic spheres; great
  orange tubes bent into loops, mostly standing upright; clusters of pink crystals; little
  banded planets; and twisting spires climbing a hundred blocks or more out of the abyss,
  threaded with glowing rings.
- **What moves.** Clusters of floating eyes whose pupils turn to follow you. Gigantic
  green clawed tentacles rising out of the abyss, swaying as if underwater. Comic-book
  panels hanging in the air, each a window onto somewhere else: a drowned swamp with
  something tall and red-eyed watching from it, a world on fire, a lattice of molecules,
  open space, neon waves, a single blinking eye.
- **The sky.** A neon dome whose colours never stop drifting, nebula arms wheeling at the
  zenith, a vast geodesic moon, banded planets (one ringed) and orange tubes looping
  through the heavens.
- **The abyss.** The whole floor of the Nexus is a dark that runs to the horizon, a crimson
  haze drifting over it and red eyes opening in it, and out of it reach hands beyond
  counting, gaunt and long-fingered, clutching slowly at nothing. Come down toward them and
  you hear it screaming, louder the closer you get, and the nearest hands strain up at you,
  stretching and grasping faster. In the last few blocks they have hold of you and pull.
  Touch the surface and it is quicksand: you are caught, and you cannot walk, jump or fly.
  Five of the hands rise around you, arch over and close on you, and drag you under, slowly,
  over about seven seconds. The view shakes in their grip and narrows; it splits, tears into
  bands of colour and static, drains of colour, and goes out a square at a time from the
  edges in until there is nothing left. Then you are gone. No armour, totem, creative mode
  or Regulator saves you, and whatever you carried is lost with you (only the stones and the
  Regulator are given back, at the hub). Mobs sink the same way. Spectators are left alone,
  and with `reduceFlashing` the shaking stops and the corruption changes slowly instead of
  seething.
- **Being there.** Gravity only half believes in you: you drift down rather than fall, and
  falls do a third of the damage. Soft colour hangs in the air and voices from other
  realities drift past, over music of its own. Everything moves slowly and nothing flashes:
  the horizon leans gently and the view breathes over the better part of a minute, and even
  that stops with the client option `nexusWarp` off or `reduceFlashing` on.
- **Leaving.** A vortex of every colour stands upright on the hub island where you arrive.
  Walk into it and it returns you to wherever you were before you entered the Nexus.

## The In-Betweener

Made by Master Order and Lord Chaos to be the bridge between them, he grew greater than
either, so they set him at the exact junction of their powers: the one point in the Nexus
where they meet and cancel out. No opposites, no transitions, no dualities. To mock him
they left him a bubble of glass made from the spot where their powers meet: a smooth,
nearly clear lens with a wandering seam of colourless light round it, Order's gold lattice
on one side and Chaos's churning blue on the other. Inside it he is omnipotent. Outside it the neutrality
would strip him of everything he is. He has never been outside it, and he does not know.

- **Finding him.** He is at 512, 120, −512 in the Nexus, about 720 blocks from the hub. A
  pale grey seam stands in the sky over the spot, the one colourless thing in the Nexus, and
  above it two great bands of light stream in from opposite horizons: Order's gold and
  Chaos's pale blue, greying as they come, cancelling to nothing above his glass. A Stone
  Finder points at the Soul Gem on his brow. The closer you get, the more the Nexus gives up:
  colour flakes off the islands block by block until everything is one flat grey, fewer
  things have drifted in and nothing lives there, the motes and voices stop, the hum fades to
  silence, the music stops, the sky and fog go grey and the horizon stops leaning. Around the
  middle, identical grey cubes hang motionless in a perfect lattice, and a perfect grey disc
  hangs in the air, reached by four lines of grey stepping stones (one from the hub's side).
  Above it, the bubble.
- **The epicentre.** Within 48 blocks of the junction there is no up or down and no before
  or after. Nothing falls: off the ground you hang where you are, rising if you jump and
  sinking if you sneak. Nothing changes: hunger does not grow, no effect wears off, nothing
  burns, no breath runs short, and nobody heals (harm still comes; it is done to you, it is
  not a change in you). Almost nothing is heard: no footsteps, no blocks, no wind, only him,
  his masters and what they do. Grey dust hangs in the air and never drifts, the fog closes
  to one flat even grey, and the edges of sight have already lost their colour.
- **He is not a man, and will not be spoken to as one.** Player-sized, bald, split straight
  down the middle: his right side white, his left side black, a quartered tunic under a black
  belt, each boot the opposite colour of its leg, and a small orange gem burning on his brow.
  He stands on nothing inside his glass and regards you the way a mountain regards weather.
  He mentions, once and in passing, that he is bigger than Eternity; after that he just acts
  like it. He talks like a smug, bored prisoner who thinks he is a god: short, dry, dismissive,
  and entirely sure of himself, glass or no glass. He never tells you that you are doing well;
  you see it when he gets defensive, goes quiet, or starts arguing with himself.
- **You look at whoever is talking.** While he speaks to you your view turns to face him;
  when Order and Chaos talk, it turns from one to the other as they take turns.
- **Nothing is described.** Whatever he does as he speaks, you see him do: frost of two
  colours creeping over the inside of his glass, the glass going dark but for his two eyes,
  his glass filling with stars or crowded with copies of him all looking at you, the gem on
  his brow flaring, a knuckle rapping the glass and a ring running round it, the glass
  rippling as he laughs, thinning and bulging toward you as he beckons, a crack appearing in
  front of you that was not there a moment ago.
- **It is a game.** Outside his glass he cannot touch you, read you or harm you; inside it,
  he can do anything. So every move he makes is meant to bring you in. He offers to show you
  your whole life if you lay your hand on the glass; holds the Soul Gem out against glass gone
  thin and invites you to reach through; points out a crack and asks you to look closely;
  admits, very quietly, that he is lonely; warns you that his makers are coming and that the
  only safe place is in there with him. Each lure he chooses suits what he has seen of you.
  Step in, even halfway twice over, and he has won. Your move, all along, is the opposite one:
  to make him want the outside badly enough to break his glass with you, without ever letting
  him see that this is what you want.
- **Out here, he is bluffing.** He has no power outside his glass at all, and he will never
  admit it. He threatens constantly: he could unmake you with a thought,
  fold you into a point, turn your blood to light. Nothing ever happens. The worst he can
  actually do is turn his back and refuse to speak to you for `castOutMinutes` (5), and he
  cannot make you leave. So you can annoy him: rap on the glass, keep talking while he sulks,
  sit down and stare at the back of his head. He blusters, then rants, and still nothing
  happens.
- **Taking his argument apart.** Call the bluff and keep asking simple questions. If he can
  reach you, why hasn't he? If he sees everything, why does he need you closer? Could he stop
  a stone thrown at his glass? Has anything he has ever willed passed through it? Eliminate
  every way out of the answer and he finally says it aloud: nothing passes the glass; out here
  he is a voice in a jar. But he is sure it is the glass that stops him, not his nature, and
  that it will all be his again once the glass is gone. Exposing him is no victory on its own.
  It is the opening that makes him want out.
- **What he keeps count of.** Nothing on the screen marks an answer as wise. Behind it he
  keeps his **ego**, his **hunger** for the outside, his **suspicion** of you, how near he has
  **drawn** you, and how far you have got on his **nerves**, and he remembers what you have
  shown him. Flattery feeds his ego and starves his hunger; insults starve his ego until he
  turns his back on you; eagerness feeds his suspicion until he sees your hand and stops
  playing; annoyance makes him bluster. Say you came for the gem and later claim you did not,
  and he notices. Play too long without going anywhere and he calls it a draw.
- **The turn.** Only when he wants the outside enough, and suspects you little enough, does he
  raise leaving himself, as if idly. Then he names his condition: step through the outer
  glass and push with him from the centre. It is his last lure, and the right answer is the
  one Thanos knew: he pushes from inside, you hit it from outside, and you never set foot in
  the glass. There is a second way to his turn: tell him the truth about the outside, then dare
  him to prove you wrong. It works only if his ego is high and his suspicion low; otherwise he
  has watched that gambit for ten thousand years.
- **Breaking the glass.** When he presses, face the glass and swing, hard. A blow is measured
  as it would land: the weapon in hand, its enchantments, how fully the swing was drawn, a
  critical if you come down on it, doubled by a charged Astral Regulator or the Power Stone.
  Anything weaker than `shatterBlow` (12) glances off; the glass takes `shatterStrength`
  (300) in all, within forty seconds, and closes up if you stop. Fail and he is unimpressed.
- **The step.** The glass bursts into shards. He hangs on nothing with his arms flung wide,
  laughing, then walks down out of the air, crosses where the glass was, and falls. The black
  and the white run out of him into the grey. Even then he calls it a temporary asymmetry and
  orders you to lift him. Pluck the Soul Gem from his brow; or raise a charged Astral
  Regulator over him and draw him in whole (the gem falls free into your hand).
- **There is only one of him.** He never respawns, and the glass is never remade. When Order
  and Chaos arrive they find nothing left in him worth putting back, and let him go: his
  empty grey body drifts away from the junction, out over the islands, for about a minute
  and a half, and comes to rest somewhere in the Nexus, 320 to 720 blocks out, turning
  slowly over and over in the void. A faint grey glimmer hangs about it, the only way to
  spot it from far off. Find it and you can still take the gem from his brow, if nobody has,
  or raise a charged Astral Regulator and draw him in. One day there may be a way to bring
  him back.
- **How it can end.**

  | Outcome | How |
  | --- | --- |
  | **The Theft** | Win the game, break the glass with him, and take the gem while he lies helpless. |
  | **The Pride Trap** | Tell him what waits outside, then dare him to step out and prove you wrong, while his ego is swollen and he does not suspect you. |
  | **The Wager** | Ask for the gem and he refuses, but offers three paradoxes instead. Answer all three as he would and he pours a boon out of his lens into you (thirty levels, and for twenty minutes the strength and endurance of something not quite mortal). Lose one and the stake is that you walk into his glass of your own free will: he cannot make you, so you choose whether to honour it. Refuse and he can only rage at you, and turn his back. |
  | **Doubt** | Tell him what waits outside, then point out that if he were truly everything he would not need to leave. |
  | **Seen through** | Let your suspicion show. He laughs, declares the game over and turns his back on you. You can keep pestering him until he talks again, but he will trust you a little less. |
  | **Drawn in** | Take one lure too many. Inside, he reads what you are. Only curious, and you are sent away weaker, slower and duller for twenty minutes, ten levels lighter. A thief, or one too eager to help, and he shows you what everything is: life and death trading places in your body, up and down trading places in the world, copies of him everywhere, time stopping for you and not for him, and finally a touch on the brow as the world splits into white and black. **You die. Every time.** |
  | **The Regulator** | Fallen outside his glass, or drifting in the Nexus afterwards, he can be drawn into a charged Astral Regulator. He never comes back, and his makers are not pleased. |

  The gem is the Soul Stone, and there is only one. If a Soul Stone already exists anywhere
  else in the world, his brow is empty, and he has a different story.
- **He remembers.** Come back and he greets you by how you left him: the thief with the eye
  he has left fixed on you (and the bait of stepping closer), the one he killed as tediously
  circular, the one who made him doubt with the news that he has concluded, many times, that
  you were wrong, the one who walked into his glass and out again with an invitation to do it
  again, and the one who answered his paradoxes with something close to attention.

## Master Order and Lord Chaos

The moment the glass breaks, the In-Betweener's makers feel it. Two vast heads rise at
opposite edges of the sky and draw nearer as the clock runs down: Master Order, a bald,
brown, furious man's face in a halo of golden fire, and Lord Chaos, a lumpen violet mass in
pale blue whose eyes and mouth open onto deep space. They are the heart of Eternity: less
than he is, and not so absolute. They do not simply unmake whoever is there.

- **Escaping them.** You have `wrathSeconds` (60). Be more than `wrathRadius` (64) blocks from
  the junction when they arrive and you have escaped: they let his empty body drift off
  and promise everyone in the Nexus that they will see you again.
- **Answering to them.** Anyone still near must answer for it. They speak to the thief
  first if the thief stayed, otherwise to whoever is nearest. Order speaks in gold, rigid and
  absolute, in capitals when angered; Chaos in pale blue, gleeful and capricious, finishing
  his brother's sentences. What you say changes the fight that follows: anger makes them hit
  a quarter harder, delighting Chaos makes him play with you between blows, handing back the
  gem costs each of them a quarter of their strength, and showing them Eternity caged in a
  charged Regulator (*we are the heart of Eternity, and you have put him in a jar*) makes
  every blow you strike with it land twice over. If you drew the In-Betweener himself into
  the Regulator, they know. Walking away does not end the conversation. It ends the talking.
- **Fighting them.** Then they fight everyone near the junction, each with a boss bar and
  `mastersHealth` (1,500); no blow takes more than `mastersMaxHitFraction` (2%) of it. Every
  attack takes a share of your maximum health through any armour, but each can be seen coming.
  Remember that nobody heals at the epicentre: step out of it to recover.
  Creative mode does not protect you from them.

  | Master Order | Lord Chaos |
  | --- | --- |
  | **Decree**: a burning line from his eyes, its end crawling across the floor after you | **The Dice**: two vast dice of deep space tumble down, and whatever they show happens: a shower of stars, everyone flung skyward, everyone shuffled across the disc with their hotbars scrambled, a burst under everyone's feet; or he heals you by mistake, hurts himself laughing, or rolls double sixes and attacks twice more |
  | **Edict**: a law for everyone at the junction, written across the sky in gold: STAND STILL, DO NOT STRIKE, or LOOK UPON ME. Break it and a bolt of punishment falls on you | **The Maw**: he inhales, dragging everything toward his mouth, and bites what reaches it |
  | **Tessellation**: the floor divides into a grid of golden tiles in a pattern symmetrical both ways; the lit ones erupt, then every other one | **Wanderers**: little rifts of deep space set loose on the floor, wandering at random and leaping without warning |
  | **Alignment**: a golden line across the floor through you; everything is drawn to it, then judgement runs its length | **Masks**: he hides among copies of himself with no boss bar to tell them apart. Strike a mask and it bursts in your face; strike him and they fall away |

  When either is badly hurt, they work together: Order's gold and Chaos's deep space cross
  on you, and the crossing hunts you across the floor.
- **The Regulator.** They know exactly what an Astral Regulator is. The first time each of
  them sees one in your hand they say so, and what they say depends on what is already in it:
  Eternity, the Living Tribunal, the In-Betweener, or one of them. While you carry one they
  circle wider, keeping more room between it and themselves. Raise it at one still standing
  and it takes nothing, but the head flinches back and tells you to put it down. Lying beaten,
  they see it coming and plead, or threaten.
- **Beaten.** Each lies dazed for twelve seconds when beaten. Raise a charged Astral Regulator
  at it then and it is drawn in, and never comes again; otherwise it withdraws. Either way the
  other fights on enraged. Drive both off, or draw both in, and everyone is told. With both
  inside Regulators nobody is left to come for the In-Betweener at all: break his glass and his
  body drifts off on its own when the time runs out. Flee more than 120 blocks from the junction, or die, and they let the
  rest go.

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

Master Order's laws join them once he is inside the Regulator:

| Ability | Cooldown | Effect |
| --- | --- | --- |
| Decree | 6s | A single line of judgement from your eyes for two seconds, following your look |
| Edict | 20s | Everything within 14 blocks of where you look is bound by law for six seconds: it can barely move, strike or choose |
| Tessellation | 15s | A grid of golden tiles where you look; the lit ones erupt, then all the others (22 damage each) |
| Alignment | 18s | A golden line along your gaze; everything near is drawn into it, then judgement runs its length (30 damage) |

Lord Chaos's whims:

| Ability | Cooldown | Effect |
| --- | --- | --- |
| The Dice | 10s | Roll two dice where you look and live with what they show: a great burst, a shower of stars, everything shuffled, everything flung skyward, a heal for you, or a little harm to you |
| The Maw | 15s | Inhale the world for three seconds; whatever reaches you is bitten (16 damage) |
| Wanderers | 18s | Five little rifts set loose where you look, wandering at random for seven seconds |
| Disarray | 30s | Everything near where you look turns on its neighbours |

And, if you ever draw the In-Betweener in, three paradoxes nobody is told about:

| Ability | Cooldown | Effect |
| --- | --- | --- |
| Inversion | 5s | You and whatever you look at trade places |
| Equilibrium | 45s | The health of whatever you look at becomes the health it lacks (beings like Eternity are beyond it) |
| Paradox | 30s | For everything around you, up and down trade places: it falls into the sky, hangs, and comes back down hard |

Wielded power ignores armour but, unlike his own, does not reach into creative mode.
Terrain damage follows `destroyTerrain`.

Holding a charged Regulator in either hand also gives you Eternity's endurance: his 1,024
health is added to your maximum and to your current health at once, and no single blow
can take more than `maxHitFraction` (3.5%) of your maximum, just as with him. Vanilla
caps maximum health at 1,024, so that is as high as it goes. Put it away and the extra
health goes with it.

It is also a source of Forge Energy that never runs dry: anything can draw from it (it
cannot be filled), and carried anywhere in your inventory it keeps everything else there
that runs on FE charged. It gives 10k FE/t empty, 1M FE/t more for Eternity, 100M more for
the Living Tribunal, 250M each for Master Order and Lord Chaos and 500M for the
In-Betweener (Java's integers stop it at a little over 2.1B FE/t).

What it holds shows. With Eternity inside, its heart is a universe of violet stars; once
it has absorbed the Living Tribunal it evolves: the crystal warms to gold, a golden sun
burns at its heart, and his power circles it in three turning rings of gold, rays of light
and drifting motes. Its tooltip shows every being inside it, side by side in the crystal,
in a frame with lights racing around it; with more than one inside, they wheel slowly round
one another, and past two their powers run on into a second column.

## Commands (op level 2)

| Command | Effect |
| --- | --- |
| `/eternity status` | Where he is, what he is doing, and each player's stone count |
| `/eternity summon` | He tears open a rift in front of you and hunts you |
| `/eternity travel <dim>` | Sends him to another dimension |
| `/eternity hunt [player]` / `release` | Starts or ends a hunt |
| `/eternity omen <1-6> [player]` | Plays a stone omen (6 includes the arrival heralds) |
| `/eternity stones [player]` | Lists the stones a player is carrying |
| `/eternity relics` | Who claimed the Tesseract and the Soul Stone and when, and where each relic was last seen |
| `/eternity relics release <tesseract\|soul_stone>` | Forgets a claim, so the next copy someone takes becomes the true one |
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
| `/eternity nexus [player]` | Sends them into the Nexus of All Realities, at its heart |
| `/eternity inbetweener [status]` | Whether his glass is whole, where his body is drifting, where the Soul Gem is, whom he is talking to, and how long until Order and Chaos arrive |
| `/eternity inbetweener visit [player]` | Sends them to the edge of his grey disc, facing the glass |
| `/eternity inbetweener wrath` | Order and Chaos arrive now, if his glass is broken |
| `/eternity inbetweener masters [player]` | Master Order and Lord Chaos come and turn to them now, for testing |
| `/eternity inbetweener revive` | Puts him back in whole glass, colour and all, keeping what he remembers of everyone |
| `/eternity inbetweener reset` | Mends his glass, puts the gem back on his brow (unless a Soul Stone exists elsewhere), brings back him and his masters if they were absorbed, and makes him forget everyone |
| `/eternity comet restore` | Makes it whole and lets it return, even after it was broken open |
| `/tpdimension <player> <dim> [x y z]` | Sends a player to another dimension: to its spawn on safe ground, or to exact coordinates (`~` is relative to the player) |

## Configuration

`config/eternity.cfg` covers rift timing, encounter delays, rebirth after death,
blacklisted dimensions, space scale, the damage table by stone count, the Cosmic Comet
(how rare it is, its space dimension, size, speed, heat, health and escape time), the
In-Betweener (whether he wears the Soul Stone, how hard his glass is to break, how long he
keeps his back turned when offended) and his masters (how long they take to come, how near you must
be to face them, how much they can take), and client options: presence strength, screen
shake, reduced flashing, forced gaze and distortion shaders.

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
layout must match `client/render/ModelTribunal.java`. The In-Betweener's skin and its
drained grey twin are painted the same way by `tools/InBetweenerSkin.java`, in the vanilla
player layout, matching `client/render/ModelInBetweener.java`; Master Order's and Lord
Chaos's heads by `tools/OrderChaosSkin.java`, matching `client/render/ModelOrderChaos.java`.
