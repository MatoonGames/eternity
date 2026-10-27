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
- **You will know him when you see him.** A gaunt giant whose body is a window onto deep
  space, stars moving inside it and twinkling on it. A blue mask of a face looks out of a
  tall squared hood, two great jointed crests curl out of it into hooks, his fingers are
  long splinters, and a pale tattered cloak hangs down his back with panels at each shoulder
  that lift with his arms like wings. Cloak and crests swing on springs, trailing after
  everything he does. Splinters of broken reality turn in the air around
  him and stardust trails from his fingers. Every attack shows in his hands first: a hand
  drawn back burning, a black star cupped between his palms, dark lances growing round a
  raised hand, columns of light climbing from both palms, fists brought down like a forge.
- **The world's music makes way.** When a fight begins or he comes close, whatever
  Minecraft was playing fades out instead of playing on underneath, and nothing new starts
  until a while after the fight is over.
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

## Adam Warlock

Grown in a cocoon by the Sovereign to be perfect, and sure that he is. He wants the stones for
himself, and whoever has been collecting them is in his way. For now he only comes when called
(`/eternity warlock summon`); one day he will come on his own.

- **How he looks.** A man a little taller than you in crimson armour: plates over a dark mesh
  suit, a chevron of gold converging down his chest, a high collar, layered pauldrons,
  vambraces, knee guards and boots, a gold belt with a jewelled buckle. Slicked-back golden hair
  with a quiff, blue eyes, an amber gem on his brow, and a crimson cape lined with gold that
  streams and ripples with every move he makes.
- **How he burns.** As his power rises his skin turns to burnished gold, his eyes, the gem and
  the seams of his armour light from within, and a corona of golden fire stands out behind him.
  Embers stream off him. Flying, he leans head first into it, banks through his turns, and
  leaves a trail of fire and golden afterimages of himself.
- **How he fights.** Every blow takes a share of your maximum health, through armour, but he
  cannot touch creative mode.

  | Move | What happens |
  | --- | --- |
  | **Fly-by** | He coils in the air, fist drawn back, then breaks the sound barrier and comes through you fist first, banks round and comes again: twice, three times evolved. A hit (30%) stops the world for an instant and sends you flying |
  | **Combo** | In close: a jab, a cross, an uppercut that launches you; if it lands he is above you before you stop rising and hammers you back down |
  | **Quantum volley** | Bolts of quantum energy from one palm, then the other: five, seven evolved |
  | **Quantum beam** | Palms together around a growing sun, then a beam that follows you |
  | **Meteor dive** | He climbs out of sight, a gold ring marks the ground, and he comes down fists first and lands like a hero. For a moment after he lands he is wide open |
  | **Cocoon** | Badly hurt, he wraps himself in gold. Break it before it hatches (it takes blows for him) and he reels, open; fail, and he comes out healed and *evolved*: brighter, faster and a quarter harder |

- **He cannot be killed.** No blow takes more than 4% of him (half again as much while he lands
  or reels). Beaten, he presses a hand to his side, swears it is not over, and leaves.

`/eternity warlock showcase` brings him to show you everything he can do, one move after another,
without hurting anyone.

## Tamarata

A desert planet out past Neptune, added to Galacticraft's solar system. It needs a tier-3
rocket (configurable). It is hot and windy, but the air is breathable and the gravity is like
home. That lasts only as long as the planet does: once it is broken, the air is gone and nothing
falls, so bring oxygen gear, and bring whatever you need to fuel a rocket home.

- **The land.** Red dunes over banded terracotta, old craters, and plains of sand fused to glass.
  Everywhere are the remains of the people who lived here and of the armies that came to fight
  the Champion: broken towers of quartz and prismarine, toppled colossi half sunk in the sand,
  the ribcages of things too big to have been animals, skull piles, trench lines, and fields of
  armour stands in dented armour beside rusted blades, broken anvils and torn banners.
- **The landing site.** Arrivals come down in landing balloons onto a paved plateau ringed with
  broken obelisks. In the middle is an intact launch pad.
- **The Colosseum.** 150 blocks east of the landing site: a ruined arena, its floor drifted with
  sand, tiers of seats around it with whole sections fallen in, an outer wall of arches broken
  down to its footings in places, and four gates. The Champion lives here.
- **The sky.** Rust overhead, burnt orange at the horizon, a small white sun a long way off, and
  low in the east a ringed giant. Like the vanilla sun, both are square: the sun a hard white tile
  with a square glow, the giant a face of shaded cells banded with cloud, its flat ring passing
  behind and in front of it. Broken, the planet has no sky: only space, stars and a belt of
  rubble. The sky itself never cracks; that is for the end of the universe alone.

## The Champion of the Universe

Tryco Slatterus: an Elder of the Universe who has spent billions of years doing the one thing he
cares about, which is fighting. He is three blocks tall and built like a mountain, with pale
blue-grey skin, a great swept-back red mane, a high gold collar, a navy sleeveless top cut in a
deep V, a gold championship belt with a black-stone plate, gold cuffs, red tights and red boots.
He wears the Power Stone in his brow without understanding it, and the stone feeds on his temper.
There is one of him per world, and his story happens once.

- **His arena.** Come near the Colosseum and he is there, fighting: the last warriors of Tamarata
  still pour through his gates in waves, and he demolishes them. Come close and he finishes them
  with a roar and calls you out.
- **You cannot beat him head-on.** From the front he blocks most blows and slips most of the
  rest; what gets through takes a sliver of him at most (1.5%, double that from behind or while
  he recovers from a big swing). Knocked down, he lies there laughing, then gets up healed by the
  stone, and angrier. Every blow of his takes a share of your maximum health, through armour,
  but he cannot touch creative mode.
- **His rage is everything.** It shows on his boss bar, and in him: the stone burns brighter, his
  eyes light, the veins stand out purple, and at the top of it a crackling aura of the stone's
  light stands out around him. He gets faster and hits harder with every stage: *Composed*,
  *Heated*, *Furious*, *Berserk* (half again as hard).

  | Makes him angrier | Calms him |
  | --- | --- |
  | Slipping his blows while you are close enough to be hit | Standing in front of him and trading blows, which he respects |
  | Staying out of his reach | |
  | Hitting him from range (arrows, or swings from too far away) | |
  | Refusing his counter-stance dare | |
  | Taunting him: sneaking up and down in front of him, or slapping him with an empty hand | |

- **How he fights.** Boxing first, and raw strength; never a beam.

  | Move | What happens |
  | --- | --- |
  | **Jab-jab-cross** | The lead hand twice, then the right with all his weight turned behind it |
  | **Liver hook** | A step to the side and in, and the left hook whips round, low |
  | **Uppercut** | Down and coiled, then up through you: it launches you |
  | **Superman punch** | A crouch, a leap across the arena flat out with his fist ahead of him, and down into a lunge (30%) |
  | **Clinch and throw** | He reaches for you; if he gets hold, up over his head you go, and then you are thrown |
  | **Shoulder charge** | Shoulder down, and he comes straight, through whatever is in the way |
  | **Ground pound** | Both fists high, a hop, and down: rings of force race out across the ground and the floor cracks |
  | **Counter stance** | Guard dropped, chin out, beckoning. Hit him and the right comes over the top of your swing |
  | **Primordial roar** | The air itself is thrown back from him, and so are you. The stone drinks it in |
  | **Flex** | Between bouts, because he can |
  | **Hammer quake** | Berserk only: a leap with both fists locked overhead, and down like a hammer (up to 40%) |

- **Planet Breaker.** At the top of his rage he loses himself entirely: he gathers himself, the
  stone blazing, leaps out of sight, hangs there a moment like a second sun, and drives both fists
  into Tamarata. A wave runs out from the Colosseum for about sixteen seconds, tearing the planet
  apart behind it a chunk at a time into a field of drifting fragments, and then the whole rest of
  the planet anyone has loaded turns to rubble at once. Anywhere loaded later breaks as it loads,
  and anywhere never visited is rubble from the start. Only the island your launch pad stands on
  holds. From then on there
  is no up and no down there: you drift, pushing along where you look (jump to rise, sneak to
  sink), and nobody falls out of the wreck. Planet Breaker does not hurt you; it ends the world
  under your feet instead. It never touches any other dimension, and a Champion summoned anywhere
  else by command only goes through the motions.
- **The trade.** Stranded, unable to fly or even swim through the wreck, he bargains: the stone
  for a rocket. Right-click him holding a Galacticraft rocket (tier one to three) and it is
  his. He tears the stone from his brow and throws it to you (it is the one true Power Stone,
  claimed by you, and the Relic Tracker follows it), hauls himself across to the rocket, climbs
  in, and is gone for good. Your own rocket is still where you left it, on the island.
- **One Power Stone.** If a Power Stone already exists anywhere else, his brow is empty, and he
  only owes you a debt.

`/eternity champion showcase` brings him to wherever you are to show you every move, without
hurting anyone or breaking anything. His music borrows a fight track until he has one of his own.

## Odin's Vault

Odin's Treasure Room, deep under the palace of Valaskjalf: a dimension of its own (-1934,
`vault.dimension`) with nothing in it but the inside of the vault. There is no sky and no outside to
see. For now the only way in is `/eternity vault`; the doors behind you lead back to wherever you
came from.

- **The hall.** You arrive at the foot of a flight of steps, lamps set low in its walls. At the top
  is the hall: tall and narrow, its walls stepping in as they rise toward a lattice of light running
  the length of the roof, scored with diagonal grooves, with deep bays down either side where the
  treasures stand in pools of light. A cold haze swallows the far end, where a wall of light glows
  through the gloom; pale shafts fall from the roof, dust turns slowly in them, and away from the
  lamps it is dark. Nothing in the vault can be built or broken.
- **The treasures.** Down the right side: the Warlock's Eye, a milky orb in three gold claws that
  turns to watch you; HeroesExpansion's Tesseract on a gold cradle; the Crown of Black Fire, Surtur's
  black iron split with burning cracks, dark fire licking off it; and the Eternal Flame, burning in
  the bronze Chalice of Muspelheim on the fluted Pillar of Privaldi. Down the left: the fake Infinity
  Gauntlet with all six stones; the Tuning Fork, its tines shivering as it rings; the Tablet of Life
  and Time, carved with a ring of runes that glows; and HeroesExpansion's Mjolnir, which only the
  worthy can lift. In the middle of the floor, on the frosted Pedestal of Fimbulvetr, the Casket of
  Ancient Winters. Mjolnir and the Tesseract are set out the first time anyone comes (the Tesseract
  only while nobody has claimed one, and it follows the one-true-relic rules like a village's).
- **The Einherjar.** Two of Asgard's palace guard stand at the head of the steps: blackened steel and
  gold, mustard cloaks, helms crowned with a ring of horns, round bronze shields and spears taller
  than they are. They challenge you and come at you from either side behind their shields: a thrust
  with their whole weight behind it, quick jabs, the shaft swept flat across, a shield bash, or a
  crouch behind the shield with the spear over it, answering any blow on the shield at once. Blows
  from in front land on their shields unless an axe knocks them aside. They are only mortal.
- **The Destroyer.** About twenty seconds after you come in (`destroyerDelay`), or the moment anyone
  lifts the Casket, the Destroyer wakes in its niche at the far end: a shudder through every plate,
  fire kindling behind its face, its head coming round to you, and a first step. It punches and
  backhands you across the hall, brings both fists down so rings of force race over the floor, and
  stamps at anything near its feet. Then its face plates swing open, the furnace behind them gathers,
  and it pours out a ray that follows you, or sweeps it across the hall at chest height. Almost
  nothing hurts it: only someone wielding something worthy of it (the Power Stone, held or in a
  gauntlet in hand; Mjolnir or Stormbreaker, held or thrown; a charged Astral Regulator) gets through
  its armour; anything else lands a tenth as hard at most. Beaten, it falls to its knees and then on
  its face, and the fire in it goes out; Odin's smiths have it standing in its niche again after
  `destroyerRepairMinutes` (20). Every blow of its takes a share of your maximum health, through
  armour, but it cannot touch creative mode. Its music borrows a fight track for now.
- **The Casket of Ancient Winters** works. Right-click its pedestal to take it (and wake the
  Destroyer), or to put it back. Use it and a gale of ancient winter pours out ahead of you: whatever
  it catches is frozen in ice where it stands and can barely move for five seconds; water ices over,
  lava crusts to stone, fires go out, snow settles and spikes of ice erupt from the ground. Sneak and
  use it and winter bursts out all round you instead. Held, it freezes the water under your feet. It
  never changes the world inside the vault, the Star Chamber or the Nexus, nor anywhere with
  `casketChangesTerrain` off, and like the stones it cannot be destroyed.

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
| `/eternity relics` | Who claimed the Tesseract, the Soul Stone and the Power Stone and when, and where each relic was last seen |
| `/eternity relics release <tesseract|soul_stone|power_stone>` ||soul_stone>` | Forgets a claim, so the next copy someone takes becomes the true one |
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
| `/eternity warlock [status]` | Where Adam Warlock is, what he is doing, his health, and whether he has evolved |
| `/eternity warlock summon [player]` | He streaks down out of the sky at them and fights |
| `/eternity warlock showcase [player]` | He comes to show off every move in turn, harmlessly |
| `/eternity warlock fight` / `calm` | He stops holding back, or pulls his punches |
| `/eternity warlock move <taunt\|flyby\|combo\|volley\|beam\|dive\|cocoon>` | He does that next |
| `/eternity warlock wound <percent>` | Damages him, for testing his cocoon and his retreat |
| `/eternity warlock dismiss` | He leaves |
| `/eternity champion [status]` | Where the Champion is in his story, what he is doing, his rage, and where the Power Stone is |
| `/eternity champion visit [player]` | Sends them to Tamarata, outside the Colosseum (or to the island, once the planet is broken) |
| `/eternity champion summon [player]` | A Champion of their own, wherever they are, who fights them; he never breaks anything |
| `/eternity champion showcase [player]` | The same, showing off every move in turn, harmlessly |
| `/eternity champion move <flex|challenge|jab_cross|hook|uppercut|haymaker|grab|rush|pound|counter|roar|hammer|breaker>` | He does that next |
| `/eternity champion rage <0-100>` | Sets his rage |
| `/eternity champion break` | He loses his temper completely and breaks Tamarata |
| `/eternity champion strand` | Skips to him adrift in the wreck, the planet breaking around him |
| `/eternity champion restore` | Brings him back to his Colosseum with the stone in his brow; Tamarata is whole wherever it was not yet broken, and chunks generated from then on are whole again |
| `/eternity champion dismiss` | Sends a summoned Champion away |
| `/eternity vault [visit] [player]` | Sends them into Odin's Vault, at the foot of the steps |
| `/eternity vault leave [player]` | Sends them back out to wherever they came in from |
| `/eternity vault status` | Who is inside, how long until the Destroyer wakes, its state, the guards, the Casket and the relics |
| `/eternity vault wake` | The Destroyer wakes now |
| `/eternity vault guards` | Two fresh Einherjar take their posts |
| `/eternity vault calm` / `fight` | Everything in the vault pulls its blows, or stops pulling them |
| `/eternity vault move <punch\|backhand\|hammer\|stomp\|ray\|sweep>` | The Destroyer does that next |
| `/eternity vault guard <thrust\|jab\|sweep\|bash\|guard>` | The Einherjar do that next |
| `/eternity vault wound <percent>` | Damages the Destroyer, for testing its fall |
| `/eternity vault restore` | The Casket back on its pedestal, Mjolnir and the Tesseract back on their stands if they are gone, the Destroyer mended |
| `/tpdimension <player> <dim> [x y z]` | Sends a player to another dimension: to its spawn on safe ground, or to exact coordinates (`~` is relative to the player) |

## Configuration

`config/eternity.cfg` covers rift timing, encounter delays, rebirth after death,
blacklisted dimensions, space scale, the damage table by stone count, the Cosmic Comet
(how rare it is, its space dimension, size, speed, heat, health and escape time), the
In-Betweener (whether he wears the Soul Stone, how hard his glass is to break, how long he
keeps his back turned when offended) and his masters (how long they take to come, how near you must
be to face them, how much they can take), the Champion (whether he is there and wears the Power
Stone, Tamarata's dimension id and rocket tier, his health, damage and how much a blow can take of him,
how fast his rage builds and cools, how far Planet Breaker reaches and how fast it erases), Odin's Vault (its
dimension id, how soon the Destroyer wakes, its health and armour, how hard it hits, how long it takes to mend, the
Einherjar's health and spears, and whether the Casket changes the world), and client options: presence strength, screen
shake, reduced flashing, forced gaze and distortion shaders.

## Building

```
./gradlew build
```

Lucraft, HeroesExpansion and InfinityCraft deobfuscated jars live in `libs/`.
The Galacticraft Legacy release jar goes in `run/mods/`: the build deobfuscates it to
compile Tamarata against (only `compat/galacticraft` uses its classes, and none of it is
shipped), and the dev client and server load it from there.

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
Adam Warlock's skin, his golden form and his glow layer are painted by `tools/WarlockSkin.java`
in a 128x128 layout of their own, matching `client/render/ModelAdamWarlock.java`.
The Champion's skin, his rage and stone glow layers, and Tamarata's icon for the celestial map are
painted by `tools/ChampionSkin.java` (`java -cp out ChampionSkin <entity texture dir> <celestialbodies
texture dir>`), in a 128x128 layout matching `client/render/ModelChampion.java`.
Everything in Odin's Vault that is not a block (the Einherjar, the Destroyer and its glow, the treasures and
their glow) is painted by `tools/VaultSkin.java` onto the layouts `client/render/VaultBoxes.java` packs:
`javac -d out tools/VaultSkin.java src/main/java/com/matoon/eternity/client/render/VaultBoxes.java` then
`java -cp out VaultSkin src/main/resources/assets/eternity/textures/entity`.
