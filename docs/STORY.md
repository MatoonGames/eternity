# Eternity: Story Bible

This covers the story, the opposition that drives it, what every side stands to lose, the
reasoning behind each character's choices, and the systems we need to build to carry it.
Nothing here is implemented yet, except Chapter 6 (the Grandmaster), which is built.

---

## 0. Ground rules

Use these to check every scene, line and quest step, including new material.

1. **Every action has a reason the character would give.** A character does something because
   it serves what they want, in a way that fits what they know. If we can't explain why, the beat
   goes.
2. **Every side can lose something it can't get back.** Every chapter's climax costs somebody
   something permanent. Nobody's stakes are abstract.
3. **Nothing pulls the player out of the world.** No death-screen visions, no dream dimensions.
   Scenes happen where the player already is, with the people who are actually there.
4. **We build nothing new for Asgard.** Odin's Treasure Room is all of Asgard the player sees.
5. **No clichés, no quips.** See §9.
6. **Players work it out.** We hand them the pieces, not the answer.

---

## 1. The premise and its logic

### What Death wants

The living have come to outnumber the dead, and Death wants the balance put back. The only way
to do that at the scale she needs is a full set of stones, closed in one hand: half of all life,
gone at once. She doesn't think of it as cruelty. She thinks of it as a correction, overdue.

| Question | Answer | Why it holds |
| --- | --- | --- |
| Why doesn't she do it herself? | The abstract entities (Death, Eternity, Order, Chaos) answer to the Living Tribunal, and the law forbids them acting on creation directly. Her power is only over what has *already* died: she can take a soul, refuse one, or release one. She can't end a life that hasn't ended | The Tribunal already exists in the mod and judges cosmic transgressions. A mortal holding the stones is outside that law, so an abstract that wants something done needs a mortal |
| Why the stones? | Only a full set gives a mortal the power to do it | The stones are the one thing in the mod that outranks the abstracts |
| Why a mortal who can't die? | The stones are kept by things that kill: oath-bound dead, the Destroyer, the Champion, the Gardener. An agent who can die won't reach six | A practical requirement, not a romantic one |
| Why the player? | Everyone born in this universe is spoken for. Their souls are owed to a god, a hall, a realm (Valhalla, Hel, the Sovereign's birthing pods). Refusing one of them would be noticed and contested. **The player arrived in this world from nowhere.** No family, no people, no god has a claim on them. She can refuse them and nobody will ever know | It's the cheapest possible agent for her: nobody to answer to, nobody who'll come looking |
| Why can't the player die? | She won't let them in. Every respawn is her turning them away | It protects her investment, and builds a dependence she can lean on |
| What does refusing them cost her? | **Every respawn is a death that doesn't happen.** Each time she sends the player back, she makes the imbalance she wants fixed a little worse | She's paying for her agent with the thing she wants most. It's why she's impatient, and why she can't afford a refusal at the end |
| Why seduce them? | Anyone holding all six stones stands above every abstract, her included. At the end she won't be able to command the player, only ask. So from the first meeting she builds the one thing that might still work then: they have to *want* to do it for her | The seduction is planning for the moment she loses power over them |
| Why disguise herself? | Mortals flinch from her real face. The human face lets her be listened to | It's a tool, so she drops it as soon as it stops working (Ch.2) |
| Does she want Eternity dead? | **No.** Eternity is all of existence. If he dies, the universe unmakes itself; life ends and there's nothing left to balance. Death is the end of life, and with no life she has nothing to be | So she eventually sends the player to the Regulator. She needs Eternity out of the way, not dead (Ch.4) |

### What everyone else wants

- **Eternity** wants to keep existing. A being holding all six stones can rewrite him. He can't
  speak to a mortal, because the universe doesn't hold conversations. He *reacts*, harder with
  each stone taken: omens, rifts landing closer, the hunt at six. All of that is already in the
  code (`world/Director.java`).
- **Adam Warlock** was grown by the Sovereign to be perfect and sent to bring them the stones.
  Once he learns who the player works for, his goal changes from *getting* the stones to *keeping
  them from her*. A cull would take half of everything, including the people who made him.

### Why the story starts now

**Odin is in the Odinsleep**, the long sleep in which he renews his strength. While he sleeps,
his enchantments weaken.

- It's why the ward on the Tesseract in the Norse village is failing.
- It's why his vault is guarded only by the Einherjar and the Destroyer.
- It's why Death moves now: when Odin wakes, the window closes.

### Why the stones are where they are

| Stone | Keeper | Why it's there |
| --- | --- | --- |
| Space | A Norse village, under oath | Odin took the Tesseract out of his vault and hid it in Midgard with a family sworn to him. He wouldn't keep two stones in one room |
| Reality | Odin's Treasure Room, as the Aether | The Aether can't be destroyed, so Asgard locked it away |
| Power | The Champion | An Elder of the Universe. Wears it in his brow without knowing what it is |
| Soul | The In-Betweener | Order and Chaos imprisoned him at the Nexus; the gem was on him when they did |
| Time | The Gardener | An Elder. Uses it to grow life on dead worlds |
| Mind | The Grandmaster | An Elder. Keeps it in play in his games |

The Elders of the Universe are the oldest mortal beings in the cosmos, and each has spent
billions of years on one obsession. They're natural keepers. They also know each other, so once
one falls, the others hear (Ch.5, Ch.6).

---

## 2. Four-corner opposition

From John Truby's *The Anatomy of Story*: the hero and three opponents all want the same thing,
each for different values and by a different method. Each opponent attacks the hero's weakness
in a different way, and **the opponents fight each other**, not just the hero.

**What they all want: the six stones.** Underneath that, each wants to decide what happens to
life in the universe.

```
     PLAYER ─────────────────── ADAM WARLOCK
   (self-determination)      (purpose / protection)
     │      ╲                ╱      │
     │        ╲            ╱        │
     │          ╲        ╱          │
     │            ╲    ╱            │
     │              ╳               │
     │            ╱    ╲            │
     │          ╱        ╲          │
     │        ╱            ╲        │
   ETERNITY ─────────────────── DEATH
   (existence)                 (balance)

  Diagonals are the deepest oppositions:
  Player ↔ Death (the agent against the one using them)
  Warlock ↔ Eternity (each would make the other's purpose impossible)
```

| | **Player** | **Death** | **Adam Warlock** | **Eternity** |
| --- | --- | --- | --- | --- |
| **Wants the stones…** | For a purpose they don't have yet | Assembled and closed: the cull | For the Sovereign at first; later, kept from Death and scattered | Never assembled by anyone |
| **Values** | Freedom: choosing their own purpose | Balance: everything ends in proportion | Purpose: being what he was made for, then choosing what that is | Existence: everything continues |
| **Method** | Whatever works | Proxy, selective truth, attachment | Persuasion, then force | Escalating pressure, then annihilation |
| **Weakness** | No purpose of their own, so takes whoever's offered | Can't act directly; every refusal of the player costs her | Built to obey; doesn't know who he is without Mother's mission | Can't negotiate; can't tell a hero from a threat |
| **Stands to lose** | Their world, their deathlessness, everyone they meet | Her existence, her freedom (the Tribunal), the balance itself | His place with the Sovereign, his life | His existence, or his freedom (the Regulator) |

### The player's weakness, and how each opponent attacks it

The player's weakness is that **they're an instrument.** They act on direction, here a voice
that says "go north". In a sandbox game that's literally true, and the story makes it the
problem.

- **Death exploits it.** She hands the player a purpose that's small and reasonable at first
  ("take the cube out of that village, it's killing them") and lets them believe it's theirs.
- **Warlock names it.** His existing line, "I was made to be perfect. What were you made for?",
  becomes the centre of their rivalry. He's projecting: he's an instrument too, and he knows it.
- **Eternity punishes it.** Each stone brings him closer, which makes the player rely more on the
  one being who keeps sending them back.

### How the opponents fight each other

| Pair | The conflict | Where it shows |
| --- | --- | --- |
| **Death vs. Eternity** | Counterparts. She needs him out of the way; he resists anything that would remake him | She never names him. She arms the player against him (Ch.4). Order and Chaos, who are part of him, speak against her |
| **Death vs. Warlock** | He's the one being who sees what she is and says it out loud. She can't touch him: abstracts can't act directly, and his cocoon keeps bringing him back. **Both he and the player are deathless, he by his own nature and the player by her refusal.** That's the mirror | Ch.2, he exposes her. She never argues with him; she answers the player. In the finale, if he chooses to die, he becomes the one soul she could never have taken, given to her freely |
| **Warlock vs. Eternity** | Each stone Warlock takes draws Eternity exactly as the player's do. Eternity doesn't care about motives | If Warlock takes a stone from the player (§7.6), the omens shift to him. In the finale he may stand between Eternity's death and the end of the universe |
| **Player vs. Warlock** | Rivals for the same objects, each someone else's tool | Ch.2 to Ch.4 always. After that, every chapter, unless the player earns the alliance (§6), and then the two corners merge |
| **Player vs. Eternity** | The existing hunt | The finale |
| **Player vs. Death** | Invisible until Ch.2, then the real story | Every scene with her |

Every chapter also sets up its own local four-corner around one stone, so the structure repeats
at both levels.

---

## 3. Stakes and sacrifice

### What each corner risks, chapter by chapter

**The player**
- **Their world.** The Tesseract's leak starts taking pieces of the world near them. The Aether
  rewrites their surroundings. Eternity's rifts land closer with every stone. If Eternity dies
  uncontained, the save itself ends (`world/Unmaking.java`). Few games put the actual world on
  the table; this one already does.
- **Their deathlessness.** It exists only as long as Death wants them. If they refuse her at the
  end, she lets go, and their next death is permanent (§6, Finale).
- **Everyone they meet.** Hrafn, the Einherjar, Tamarata's last warriors, the Gardener and the
  Grandmaster all die on this road. The player causes or allows each death.
- **Their stones.** Warlock can take them (§7.6).

**Death**
- **Her existence.** If Eternity dies uncontained, she ends with everything else.
- **Her freedom.** Using a mortal proxy is exactly what the Tribunal exists to judge. If the
  player refuses her, she has broken the law with nothing to show for it, and the Tribunal is
  coming either way.
- **The balance.** Every respawn she grants makes it worse. A refusal at the end leaves the
  universe more lopsided than when she started.

**Adam Warlock**
- **His place.** Every time he comes home empty-handed, Mother is less patient. In Ch.3 he tells
  the player she has already started growing another one. He's replaceable, and he knows it.
- **His life.** In the finale, he can give it.
- **On the alliance path:** his innocence (the Gardener), Mother's offer, refused to Death's face,
  and the purpose he was made for, when the stones are broken.

**Eternity**
- **His existence**, if he's killed and nobody holds him together.
- **His freedom**, if he's contained: the universe goes on, but he's held in a crystal in a
  mortal's hand.

### The sacrifice ledger

Who gives up what, whether they chose it, and why.

| Ch. | Who | What they lose | Chosen? | Why they do it |
| --- | --- | --- | --- | --- |
| 1 | Ragna | Her life | No | Caught in the leak. The inciting incident |
| 1 | **Hrafn** | His life, his family's oath | **Yes** | The barrow opens only for his blood, and the dead will hunt whoever breaks the oath. He stays below so they don't follow him up into his village |
| 2 | The Einherjar | Their lives | Yes (duty) | They're the last guard while Odin sleeps |
| 2 | The player | Their innocence; the vault's trust | Yes | They take the Aether knowing it's guarded |
| 3 | **The Champion** | His world, his people, and the thing he lived for: he's never lost | Partly | His temper breaks his own planet. The loss is the first of his life, and Death has waited billions of years to see it |
| 4 | The In-Betweener | The gem, his freedom, or himself (by ending) | Varies | Existing endings |
| 4 | Order and Chaos | Their creation, and possibly themselves (absorbed) | Yes | They come to punish |
| 5 | **The Gardener** | His life | **Yes** | He binds the Time Stone into his own body, so the only way to take it is to kill him. He wants the player to know what the stones cost |
| 6 | **The Grandmaster** | His life | Yes (a wager) | His device stops only when he dies. He bet his life on a game he couldn't lose, and lost |
| 6 | **Warlock** | Mother's favour, his reason for existing | **Yes** | He walks off a rigged board, which means walking away from the Sovereign |
| F | **Warlock** *(conditional)* | His life | **Yes** | The only other vessel that can hold Eternity's death |
| F | **The player** *(conditional)* | Their deathlessness | **Yes** | Refusing Death |
| F | **Death** | The balance; her standing before the Tribunal | No | The player decides it for her |
| 4 | **The player** *(alliance)* | The Soul Stone, given to Warlock | **Yes** | So the six are never in one hand until the end |
| 5 | **Warlock** *(alliance)* | His innocence: the Gardener's life is on his hands | **Yes** | To spare the player, and keep the stone from the hand Death watches |
| F | **The player and Warlock** *(alliance)* | Every stone, forever; the player's deathlessness; Warlock's purpose | **Yes** | Breaking them is the only way nobody can ever be sent after them again |

---

## 4. The cast

Comic characters behave the way they do in the comics, adjusted only where the mod has already
decided (Warlock is the Sovereign's son). Original characters exist only where the world needs a
human face.

### Death

- **True form.** Robed and hooded, the face a skull, beautiful in proportion. **She doesn't
  speak.** In the comics, Death is famously silent. In her true form everything is gesture and
  stillness, written in the dialogue box as action, never as speech.
- **The disguise.** She dresses for wherever death happens. In the Norse village she's a Norse
  woman in mourning clothes: dark wool, silver brooches, hair bound up. Around thirty to look at,
  pale, composed, striking without trying. Nobody looks twice at a mourner at a funeral.
- **How she's attractive.** Attention, not flirtation. She remembers everything you say. She
  stands closer than a stranger would. She looks at you a beat longer than she needs to. She
  never compliments you; she notices you, which is worse.
- **How she talks.** Plain, quiet, exact. Short sentences. Asked about herself, she asks about
  you. **She doesn't lie.** A lie can be caught, and a truth told selectively can't, so the truth
  is the more reliable tool. When she has to tell you something unpleasant, she says it
  accurately and doesn't apologise.
- **The rule for her appearances: she's there when something dies.** It's her nature, so it
  needs no setup. It means she can reach the player whenever the story needs, since players kill
  constantly. And it means the people who notice her are the ones who've been at a lot of deaths
  (Hrafn, the Champion, the Gardener).
- **Arc.** She doesn't change. She wants the same thing in the last scene as the first. What
  changes is how much of herself she has to show to get it: disguised and kind (Ch.1); exposed
  and honest (Ch.2); silent and directing (Ch.3–6); and in the finale, asking, in the face she
  started with, because the player outranks her now.

### Adam Warlock

- **Basis.** The Sovereign grew him in a cocoon to be perfect (*Guardians of the Galaxy Vol. 3*):
  arrogant, sheltered, eager to please Mother. In the comics he grows into a brooding,
  self-sacrificing figure bound up with the Soul Gem and afraid of what he could become. The mod's
  Warlock is the young one; the story is how he becomes the other.
- **Why he's after the stones.** The Sovereign sent him. They believe the stones would let them
  perfect their people. He's never questioned an order.
- **Why he works out what Death is.** He was built to wield the stones, the Soul Stone above all,
  so he can see souls. In the vault (Ch.2) he watches the Einherjar's souls leave their bodies and
  go into the woman standing next to the player. The player can't see it. Nobody needs to tell him.
- **Why that changes his goal.** Death wanting the stones means a cull, and a cull takes half of
  everything, Mother's people included. He can't fight Death. He can fight her agent.
- **Why he talks first.** He's not a killer by temperament. He's a child following instructions,
  suddenly out of his depth, and he believes the player is being used, which is true.
- **Why he can't just kill the player.** She won't let them die. So he fights to take stones, not
  to kill (§7.6).
- **Arc: instrument to agent.**
  - Ch.2: "Mother sent me."
  - Ch.3: Mother has started growing a replacement; he's fighting to stay wanted.
  - Ch.4: the In-Betweener mocks him for being made, and it lands.
  - Ch.5: he watches an Elder choose to die for what he believes.
  - Ch.6: the Grandmaster offers to let him win the last stone for Mother, and he walks away.
  - Finale: he asks the player for the stones, to scatter them, not deliver them. Or, if Eternity
    dies with no Regulator, he spends his own life holding the universe together.
- **The alliance branch** (§6, "The alliance path"). If the player earned his trust by Ch.4, he
  proposes a pact and carries the Soul Stone. On Mars he may take his first life to spare the
  player. On the Grandmaster's board he refuses Mother's offer without a pause. In the finale he
  fights Eternity beside the player, refuses Death's offer to spare his people, and breaks the
  stones with them. The arc is the same; on this path he reaches the end of it a chapter early,
  and he reaches it next to the player.
- **Existing lines to rewrite.** "Those stones are mine. I was made to carry them." stays; it's
  where he starts. "This isn't over. The stones are MINE." goes: it's a stock villain exit, and he
  isn't a villain.

### Eternity

He stays wordless; the universe doesn't explain itself. His corner shows in what he does (the
omens, the rifts, the hunt) and is voiced by **Order and Chaos**, who are part of him ("We are
the heart of Eternity", already in the code). They're how the player learns he'll come at six
(Ch.4).

### Hrafn (original)

- **Who.** Ragna's brother, and keeper of the Tesseract's oath in the Norse village. Not a seer:
  a farmer whose family inherited a duty. In his forties, practical, tired, stubborn.
- **Why he exists.** The village needs someone with a stake. The oath says keep the cube, and the
  cube is killing his people. He's the only one willing to break it.
- **Why he matters.** He's the first person to suspect the stranger, and he does it without any
  magic, just by paying attention: she has been at every death in the village this month, and at
  one before it happened.
- **Arc.** Duty to choice. He breaks a six-generation oath to save the people it was meant to
  protect, and he pays for it with his life. It's the same move the player makes at the end, if
  they make it.

### The Champion (exists)

All he cares about is fighting, and he's honourable inside that. **New:** he's known Death a long
time. She has sat in the stands at every fight he's had for billions of years, waiting for him to
lose. He isn't afraid of her; he finds her tedious. He pays his debts: after the existing rocket
trade, he tells the player where the next stone is (Ch.3). **What he loses:** his world, and the
record that was his whole identity.

### The In-Betweener (exists)

Vain, lonely, a gossip. **New:** he lives between life and death, so he can *feel* the imbalance:
the living side pressing harder against him than it ever has. He tells the player what "balance"
means. It isn't conscience; it's that knowing something the player doesn't is the most fun he's
had in centuries (Ch.4).

### The Gardener (new)

- **Basis.** An Elder of the Universe who has devoted billions of years to cultivating life.
  Gentle, patient, contemplative. Terrible when he decides to act.
- **Where.** Galacticraft's **Mars**, an existing dimension. He plants on dead worlds because
  that's where life is needed. From orbit, or on landing, the player sees impossible green on a
  red planet.
- **What he does, and why.** Life is everything he values. He knows who sent the player and why.
  He talks. Then he tries to kill them, and it doesn't work. So he does the last thing he can: he
  binds the Time Stone into his own body. If they want it, they'll have to kill him for it. He
  wants the stones to cost the player something they'll remember.

### The Grandmaster (new)

- **Basis.** An Elder obsessed with games. He plays everything, cheats when it suits him, and is
  rarely bored for long.
- **His device.** The Mind Stone is kept in play: it teleports at random around his board and only
  stops if he dies. He built it that way because he believes he can't lose.
- **Why he comes to the player.** He warned the other Elders after the Champion fell. The Gardener
  wouldn't listen. Now he's the last, and he'd rather pick the board than wait to be found.
- **Look.** Blue skin, white eyes, silver hair swept back off a high forehead with a lock down each
  side of his face. A high flared gold collar and a gold breastplate over a dark suit, an engraved
  pauldron on his left shoulder, coils of gold up his right forearm, keys at his belt, long blue
  coat tails. Never still: he strolls, shuffles cards from hand to hand without looking, conducts
  pieces with a finger, and laughs with his whole body.
- **How he talks.** Delighted, quick, sure of himself. He talks about games because everything is
  one to him, including his own death. He never threatens; he states the rules.

### Supporting cast who don't speak

The Einherjar and the Destroyer (both exist).

---

## 5. Structure

### 5.1 The master Hero's Journey

Our one inversion: **the Mentor is the main opponent.** Everything else runs straight, so the
player feels the structure working on them.

| Stage | In Eternity | Why it goes here |
| --- | --- | --- |
| Ordinary World | Survival. Odin sleeps; nobody knows | The game's normal state |
| Call to Adventure | A Norse woman thrown across the world by the Tesseract dies in front of the player. A stranger is with her | An inciting incident in the world, caused by the failing ward |
| Refusal | The player can ignore it. The leaks keep coming closer, and they start taking pieces of the player's world | The call comes back as consequences, not nagging |
| Meeting the Mentor | The stranger, who is Death | The false mentor |
| Crossing the Threshold | Hrafn dies opening the barrow. The Tesseract takes the player into Odin's vault | The first place the player couldn't reach alone, paid for |
| Tests, Allies, Enemies | Warlock's offer; the Champion | The opposition takes shape |
| Approach to the Inmost Cave | The Nexus | Where the truth is kept |
| Ordeal | The In-Betweener explains "balance". Order and Chaos explain what's coming | **The master climax** (below) |
| Reward | Time, from the Gardener, at the price of his life | The first stone taken knowing everything |
| The Road Back | The Grandmaster's game; six stones; Eternity | The world reacting |
| Resurrection | The fight, the Regulator (or Warlock), and Death asking | The final choice |
| Return with the Elixir | The Tribunal's verdict, and what the player chose | Existing system, new conditions |

### 5.2 The master Freytag pyramid

```
                         CLIMAX
              Ch.4: "balance" means half of everything.
                     Eternity will come at six.
                          /\
         RISING ACTION   /  \   FALLING ACTION
   Ch.2 She's Death     /    \  Ch.5 The Gardener (taken knowingly)
   Ch.3 The Champion   /      \ Ch.6 The Grandmaster (refusing the frame)
                      /        \
   EXPOSITION        /          \   CATASTROPHE / RESOLUTION
   Ch.1 The cube, the stranger,      Eternity, the choice, the Tribunal
        Hrafn
```

Freytag puts the climax in the middle, at the point of no return. Ours is the moment the player
learns the full truth. From then on every stone is taken knowingly, and every death after it is
on them.

### 5.3 Sub-pyramids

Every chapter is a full five-beat pyramid with its own cost.

| Ch. | Exposition | Rising | Climax | Falling | Resolution | Who pays |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | Ragna dies; the stranger | Tracing the leaks; the village; Hrafn | The barrow: Hrafn stays below | The stranger at his body | The cube shows the vault | Hrafn |
| 2 | Odin sleeps | The vault; the Einherjar | The Aether; Warlock names her | She confirms; Warlock's offer | The Eternal Flame sets the stone | The Einherjar; the player's innocence |
| 3 | "Past the last blue world" | Rocket, Tamarata, the arena | Planet Breaker; the Champion loses | Stranded | The trade; the debt paid | The Champion |
| 4 | "Where all the worlds touch" | The Nexus; Warlock there first | The trick | Order and Chaos | The truth, then the Regulator | The In-Betweener; the player's last excuse |
| 5 | Green on Mars | The garden; the conversation | The stone bound into him | Kill him or leave | The stone, taken knowingly | The Gardener |
| 6 | He comes to the player | The game; Warlock across the board | Warlock walks; the board breaks | The Grandmaster dies | Six stones | The Grandmaster; Warlock's place |

---

## 6. The chapters

### The three-clue rule

Every objective has three clues. None of them is a waypoint, and any two are enough.

1. **Told.** A line from a character: accurate, but it only makes sense alongside something
   else. In Death's case that's in character. People are more committed to conclusions they
   think they reached themselves.
2. **Found.** Something in the world that confirms or completes it.
3. **Felt.** A mechanic: the Relic Tracker swinging, a stone humming, the omens.

---

### Chapter 1: The Oath (Space)

**Local four-corner.**
- The player.
- **Hrafn**, who wants the cube gone.
- **The oath**, voiced by the elders and enforced by the dead, which says keep it.
- **The stranger**, who wants it in the player's hands.

#### Stakes

The Tesseract's ward is failing, so the cube is throwing pieces of the north across the world,
and pieces of the world back. If the player ignores it, the leaks escalate, and they reach the
player:
- blocks of their base swapped for snow and turf;
- vanilla villagers vanishing;
- animals they've penned gone.

None of it is reversible. The longer the player waits, the more they lose.

#### The inciting incident: Ragna

**What happens.** Fragments of the north begin turning up across the world: a patch of Norse turf
in a desert, a fishing boat in a forest, a door with no house. They get more frequent with time,
and denser closer to the village. Eventually a person is caught in one.

**When.** Leak events start after the first few in-game days (configurable). The Ragna event fires
once the player has made some progress, such as an iron kit or a first visit to the Nether, so it
doesn't land on someone still punching trees. In the story, the timing is the Odinsleep deepening.

**The scene.** It plays in the world: the camera eases in, there's no teleport. The player comes
over a rise and finds a disc of northern ground, snow on it, set into whatever biome they're in.
A woman lies half inside it, her legs fused into the stone where she arrived. She's alive. A
second woman kneels with her, holding her hand.

> **RAGNA:** Where is this?
>
> **STRANGER:** A long way south. You're not in pain.
>
> **RAGNA:** I was at the line. Hanging the washing. *(she sees the player)* Tell Hrafn. Tell
> him it's worse.
>
> *Her hand goes slack. The stranger stays a moment, closes the woman's eyes, then looks up at
> the player.*
>
> **STRANGER:** Her name was Ragna.
>
> *Player:* `What happened to her?` / `Who's Hrafn?` / `Who are you?`
>
> **STRANGER** *(What happened)***:** There's a village on the northern coast. They keep
> something for Odin. It's started throwing people across the world. She's the ninth.
>
> **STRANGER** *(Who are you)***:** Someone who keeps arriving too late. *(she stands)* You saw
> her come through?
>
> **STRANGER:** Her brother is called Hrafn. The thing they keep is under their burial mound.
> Their oath won't let them take it out, and it won't stop until somebody does.
>
> *Player:* `Why are you telling me?` / `Where is it?` / *(say nothing)*
>
> **STRANGER** *(Why me)***:** You were here.
>
> **STRANGER** *(Where)***:** North. Follow what doesn't belong.
>
> *She walks away north. If the player follows, she's an ordinary woman walking. Out of sight,
> she's gone.*

**Why it works.**
- She's there because someone was dying. That's her nature, and the scene needs no other reason.
- She knew when and where Ragna would die, and placed herself where the player would come. She
  didn't cause it; she can't.
- "You were here" is true and tells you nothing. It's her first selective truth.
- "Follow what doesn't belong" is the told clue. The leak scars get denser toward the village, and
  the player triangulates the source themselves.

#### Finding the village

- **Told:** "Northern coast." "Follow what doesn't belong."
- **Found:** The scars cluster toward the village. The displaced objects are Norse: a carved
  bowl, a shield with the village's mark, a grave-marker carved with Ragna's family name.
- **Felt:** The Relic Tracker swings toward the strongest relic it can sense and reads
  *"something hums"*, never *Tesseract*.

#### The village and Hrafn

The village (HeroesExpansion worldgen, tagged by `world/RelicTracker.java`) is sullen and
frightened. Two more people have vanished, and there's a fresh grave. **The stranger is at it,**
standing among the mourners. She doesn't acknowledge the player in front of the others.

Hrafn is at the stone they set up for Ragna. If the player tells him how she died, he trusts
them. If not, he works it out from the marker they're carrying.

> **HRAFN:** My grandfather's grandfather swore it. Keep it under the mound until Odin asks for
> it back. *(he looks at the mound)* Odin isn't asking. It's taking us instead.
>
> **HRAFN:** The doors only open for our blood. And the ones who guard it are ours. They know
> the oath. Whoever breaks it, they'll come for.
>
> *Player:* `Then I'll carry it out.` / `What happens to you?` / `Why not leave it?`
>
> **HRAFN** *(What happens to you)***:** *(he doesn't answer that)* You'll carry it. It can't be
> me. If it's in my hands when the oath breaks, they'll tear it out of them.

On the way to the mound he stops the player.

> **HRAFN:** The woman at the graves. Dark hair. Brooches.
>
> **HRAFN:** She was at Ragna's. She was at Leif's the week before. *(he stops)* She was at
> Gunnhild's house the day before Gunnhild went. Gunnhild wasn't sick yet.
>
> **HRAFN:** Who is she?
>
> *Player:* `I don't know.` / `She sent me here.` / `She helped me.`
>
> **HRAFN** *(She sent me)***:** Then find out what she wants for it.

#### The barrow

A new structure under the mound:
- **Draugr**, Norse undead: Hrafn's ancestors, bound by the oath.
- **Doors sealed with the oath's words.** They open only to Hrafn's touch and only in the order
  the lines are spoken. The lines are scattered across grave-markers in the village and the
  barrow, so the player and Hrafn piece them together.
- **The Tesseract** at the bottom, in a ring of failing ward-light.

**Why the player has to be the one to carry it:** the oath binds Hrafn's line. If one of them
takes the cube from the ward, the dead tear it from their hands. The player is outside the oath.
And while the oath holds, the dead aren't Death's to take, which is one more thing she can't do
herself.

#### The climax: Hrafn stays

The player lifts the cube. The ward goes out. The oath breaks, and every draugr in the barrow
turns toward Hrafn.

> **HRAFN:** Go. Up, and don't stop.
>
> *Player:* `Come with me.` / `I'll hold them.` / *(go)*
>
> **HRAFN:** If I go up, they follow me up. Into the village. *(he draws)* They're mine. I'll
> keep them down here.
>
> **HRAFN:** Tell them I broke it. Not you. Me.

The barrow seals behind the player. The fight goes on below them, then stops.

**Why he does it:** the dead hunt the oath-breaker. If he runs, he leads them into the village he
broke the oath to save. Dying below is the only way the oath-breaking saves anyone. He
understood the cost before he opened the first door. That's why he didn't answer the player.

#### Falling action: the stranger at the mound

The player comes up. The leaks have stopped. Hours later, or at dawn, they can go back down,
because the barrow's doors are open now. Hrafn lies among his ancestors, and the stranger is
kneeling by him.

> **STRANGER:** He knew what it would cost before he opened the first door. He came anyway.
>
> *Player:* `You knew.` / `Did you know this would happen?` / *(say nothing)*
>
> **STRANGER** *(to either question)***:** Yes.
>
> **STRANGER:** You'd have gone down there whether I told you or not. So would he.

She doesn't lie, and she doesn't apologise. She uses his sacrifice to make the player's choice
seem inevitable. The player has also now heard her admit she knows who'll die. They'll remember
that in Ch.2.

#### Resolution: the way on

Held, the cube flickers with images of where it was kept longest: a tall hall, treasures in bays,
a wall of light at the far end (the existing vault). Using it while one of those images is
showing takes the player there. This is a figure-it-out step: the player notices the visions and
tries it.

**Why the vault is next.** The stranger tells them, at the mound:

> **STRANGER:** The cube was kept in Odin's treasure room a long time before he sent it here.
> There's another one in there. Odin is asleep. He won't sleep for ever.

She gives the reason (he's asleep) and the deadline (he'll wake). She doesn't say what's in the
room.

---

### Chapter 2: The Vault (Reality)

**Local four-corner.**
- The player.
- **Death.**
- **Warlock**, sent for the same stone in the same window.
- **Odin's defences** (the Einherjar, the Destroyer), which want everything to stay where it is.

**Code change.** The vault's Tesseract stand becomes the **Aether**: a sealed column of dark glass
with a slow red fluid inside, bound in iron (`world/VaultDirector.java`'s TESSERACT stand,
`world/OdinsVault.java`). The vault Tesseract is removed; the village is the only Space Stone.

#### Stakes

- **For the player:** the Aether gets into them and starts rewriting the world around them,
  starting with wherever they live.
- **For Warlock:** coming home without the Aether.
- **For the Einherjar:** their lives, against an intruder Odin isn't awake to stop.

#### Beats

1. **Arrival and the Einherjar.** The player arrives by Tesseract, and the existing Einherjar
   fight plays out. **When they die, she's there** in the hall behind the player, in mourning
   clothes, kneeling by each one in turn. The player has now seen her at Ragna's death, Hrafn's
   death and these. That's enough to make them uneasy.
2. **The vault.** The existing treasures, the Casket and the Destroyer.
3. **The Aether.** Touching the column shatters it. The Aether doesn't go into the inventory: it
   *seeks a host*, as in the MCU, and goes into the player. This is a status effect, not a
   cutscene.
   - The edges of the screen vein red.
   - Nearby blocks flicker into other blocks for an instant.
   - Mobs sometimes ignore you, as if you were one of them.
   - Changes near the player's spawn point become **permanent** while it's in them.

   The Destroyer wakes: the existing trigger extends from "Casket lifted" to "Casket lifted *or*
   Aether taken".
4. **Warlock** comes through the vault doors after the Destroyer falls, or during the fight if it
   runs long. **Scene 2A.**
5. **The Eternal Flame.** The clue: near the Chalice of Muspelheim, the red veins *pull toward
   it*. Use the flame while carrying the Aether, and it draws the Aether out and sets it into the
   **Reality Stone**. The flame goes out for good: the Eternal Flame is spent.
   - **Why the Flame:** the Aether is the Reality Stone in liquid form, and setting it takes heat
     no mortal fire reaches. The Eternal Flame of Muspelheim is the only such fire the player will
     ever stand next to.
   - **Why it costs the Flame:** setting a stone takes everything the fire has. It's a relic of
     Asgard, spent, and Odin will wake to find it dark. One more thing that can't be undone.

#### Scene 2A: "You're Death"

*Warlock lands in the hall: two dead Einherjar, the stranger standing over the second one, the
player with the Aether in them.*

> **WARLOCK:** Mother sent me for the Aether. *(he sees the red at the edges of the player)*
> You've already...
>
> *He stops. He's looking past the player at the stranger, at the guard's body, at the air
> between them.*
>
> **WARLOCK:** Their souls went into her. Both of them.
>
> **WARLOCK** *(to the player)***:** You didn't see it.
>
> **WARLOCK** *(to the stranger)***:** You're Death.
>
> **STRANGER:** Yes.
>
> *(The camera holds on the player for a long beat.)*
>
> **STRANGER** *(to the player, not to him)***:** You've died, since you came into this world.
> A fall. Something in the dark. Each time you came back. You never asked why.
>
> **STRANGER:** I sent you back.
>
> *Player:* `Why?` / `What do you want?` / `Get away from me.`
>
> **STRANGER** *(Why)***:** Because I need something done that I'm not allowed to do.
>
> **STRANGER** *(What do you want)***:** The stones. All of them, in one hand.
>
> **WARLOCK:** Ask her what for.
>
> **STRANGER:** Balance.
>
> **WARLOCK:** That isn't an answer.
>
> **STRANGER:** It's the only one there is.
>
> *(She doesn't look at him once. Everything she says, she says to the player.)*
>
> **STRANGER:** I haven't lied to you. The cube was killing that village. You stopped it.

**Why she tells the truth here:** with Warlock standing there, a denial would be caught on the
spot and would ruin her credibility with the player for good. The truth, told calmly, makes her
the one person in the room who has never lied to them. It costs her nothing yet, because she
hasn't said what "balance" means.

#### Scene 2B: The offer

> **WARLOCK** *(to the player)***:** She'll use you until there's nothing left of you, and then
> she'll have you anyway.
>
> **WARLOCK:** Give me the stones. I'll take them where she can't reach them.
>
> *Player:* `Where?` / `To your mother.` / `Why should I trust you?`
>
> **WARLOCK** *(To your mother)***:** *(a pause, because it's true)* ...Yes.
>
> **WARLOCK** *(Why trust you)***:** I'm not asking you to trust me. I'm asking you not to trust
> her.

**The choice:**
- `All right.` → stance **TRUCE**. He isn't sure he believes it.
  > **WARLOCK:** Then stop. Don't take another one. If you do, I'll know.
- `No.` → stance **RIVAL**.
  > **WARLOCK:** Then I'll take them from you. I can't kill you; she's made sure of that. But I
  > can take them.
- Ten seconds of silence counts as no. He reads it in their face.

Through all of it the stranger says nothing. She lets him make his case.

**Why she doesn't threaten the player:** a threat made early wears out and breeds resentment, and
right now the player wants the stones anyway. She's saving her leverage for when it matters.

**After the offer.** Warlock leaves, whatever the player chose. The stranger says one more thing:

> **STRANGER:** He was made to obey someone. He thinks you were too. *(she looks at the
> player)* Were you?

Then she's gone. From here the dialogue labels her **Death**, and she never wears the mourning
clothes in front of the player again until the finale. She spoke when she needed to be believed.
Now she doesn't need to.

> **On her silence.** From Ch.3 to the finale Death doesn't speak. She gestures, points, and
> stands where the player will see her. Direction comes as action in the dialogue box: *"She
> turns her face up toward the sky, past the last blue world, and holds it there."* The player's
> dialogue options still work, and she answers with what she does.

#### TRUCE, then a stone

If the player agrees and then takes another stone, Warlock arrives within minutes, and his stance
becomes **BETRAYED**.

> **WARLOCK:** You said you'd stop.
>
> **WARLOCK:** I believed you. *(quieter)* I didn't know anyone could just... say it, and not
> mean it.

His fights from then on start one evolution higher (the existing cocoon mechanic).

---

### Chapter 3: The Champion (Power)

**Local four-corner.**
- The player.
- **The Champion**, who wants a fight.
- **Warlock**, who wants the Champion to keep the stone.
- **Death**, who has wanted the Champion to lose for a billion years.

#### Stakes

- **For the Champion:** his world, his last people, and the unbroken record that is his whole
  identity.
- **For Warlock:** his place. **Mother has started growing another one.**
- **For the player:** a broken planet with no air and no way home unless they brought fuel and
  oxygen (existing Tamarata rules).

#### Direction

After the vault, the next time the player kills something, Death is standing over it. *She turns
her face up toward the sky, past the moon, past the red one, to the last blue world, and keeps
going. Then she raises a fist slowly, as if holding up a belt.*

- **Told:** past the last blue world (Neptune); a fighter; a championship belt.
- **Found:** Galacticraft's celestial map shows Tamarata just past Neptune (`world/Tamarata.java`).
  From space, the Colosseum's fires are visible on its face.
- **Felt:** Galacticraft's tier-3 rocket is the journey. In space, the Relic Tracker swings toward
  Tamarata.

#### Beats

1. Rocket, travel, landing site, the Colosseum (existing).
2. **Death in the stands.** The Champion's army waves (existing) kill Tamarata's last warriors.
   Death walks among them in the seats.
3. The Champion calls the player out (existing). If Warlock is a **RIVAL** or **BETRAYED**, he
   arrives: **Scene 3A**.
4. **Planet Breaker.** It kills everything left alive on Tamarata. The Champion loses, the first
   loss of his life. **Scene 3B.**
5. Stranded; the rocket trade (existing).
6. **The debt. Scene 3C.**

#### Scene 3A: Three in the ring

> **CHAMPION:** *(to the stands, where Death sits)* You again. *(to the player)* She's been at
> every fight I've ever had. Sits there. Waits for me to lose. *(he cracks his neck)* Billions of
> years. Hasn't happened yet.
>
> *Warlock comes down onto the sand between them.*
>
> **WARLOCK:** Elder. Whatever they offer you, don't give them that stone. They're carrying it
> to *her*.
>
> **CHAMPION:** I don't care who it goes to. I care who earns it.
>
> **WARLOCK:** *(to the player, low)* Mother's growing another one. Did you know that? Another
> me. If I go back without anything again, there won't be a reason to keep this one.
>
> **CHAMPION:** Two challengers. *(he settles into a stance)* Both of you, then. Whoever's still
> standing, we'll talk.

**Why the Champion makes it a three-way:** fighting is all he values, and two opponents make a
better fight than one. The fight is a free-for-all: the Champion targets whoever hit him last, and
Warlock targets the player. When Warlock is beaten he cocoons, hatches, and leaves.

> **WARLOCK** *(leaving)***:** Ask her what balance means.

*Alliance path: when Warlock cocoons, the Champion goes to smash it, and the player can stand in
his way (§6, "The alliance path").*

#### Scene 3B: The first loss

*After Planet Breaker. The Colosseum is gone. The Champion is on his knees in the rubble,
surrounded by his own dead. Death is standing in what's left of the stands.*

> **CHAMPION:** *(looking at her, a long time)* Well.
>
> **CHAMPION:** You waited long enough.
>
> *Death inclines her head. It's the closest thing to respect she shows anyone in the story.*

#### Scene 3C: The debt

After the trade (existing):

> **CHAMPION:** I owe you. I pay what I owe. There's a thing in a glass jar, where all the
> worlds touch. It's had a stone on its head longer than I've had mine. Order and Chaos put it
> there.
>
> **CHAMPION:** And her. *(he nods at where Death stood)* She doesn't sit in the stands for people
> she likes. She sits there for people who are going to lose.

**Why he tells the player:** he honours debts, and the location is the most useful thing he knows.
The warning isn't care; it's what an old fighter has noticed.

---

### Chapter 4: The Nexus (Soul): the master climax

**Local four-corner.**
- The player.
- **The In-Betweener**, who wants out and wants to be admired.
- **Warlock**, who wants the Soul Stone kept from Death. He was made to hold it.
- **Order and Chaos**, who want the In-Betweener kept in his glass, and who speak for Eternity.

#### Stakes

- **For the player:** the truth. After this chapter they can't claim they didn't know.
- **For the In-Betweener:** the existing endings, each of which costs him something.
- **For Order and Chaos:** their creation, and possibly themselves.

#### Direction

- **Told:** the Champion's "where all the worlds touch", "a glass jar", "Order and Chaos".
- **Found:** with three stones, Eternity's rifts land closer (existing), and sometimes open into
  the Nexus. The Rift Gate ability (existing) can open there if the player knows where.
- **Felt:** the Soul Stone's hum once they're inside.

#### Beats

1. **The Nexus** (existing). If Warlock is a rival, he's at the glass already, trying to persuade
   the In-Betweener not to deal with the player: **Scene 4A**.
2. The existing In-Betweener conversation, with one new node, **`IMBALANCE`**: **Scene 4B**.
3. The trick (existing endings).
4. Order and Chaos (existing), with new lines: **Scene 4C**.
5. **Death names the Regulator: Scene 4D.**

#### Scene 4A: Two made things

> **IN-BETWEENER:** *(to the player)* Another one. He's been shouting at my glass about *you*
> for an hour. *(of Warlock)* He's made, isn't he? You can always tell. They stand like they're
> waiting to be told what to do.
>
> **WARLOCK:** I am not...
>
> **IN-BETWEENER:** Order and Chaos made me. Somebody made you. And you're *still* doing as
> you're told. At least I'm in a jar. What's your excuse?
>
> *Warlock has no answer. He fights, or he leaves.*

**Why it matters for Warlock:** it's the first time anyone names what he is, and it comes from a
being he holds in contempt. He's angry because it's true.

#### Scene 4B: `IMBALANCE` (the climax)

*Inserted into the existing conversation once the player carries Death's mark, whichever branch
they're on. The In-Betweener can see what stands behind the player.*

> **IN-BETWEENER:** *(stopping)* Oh, she's right behind you. *(beat)* Has she told you what
> you're doing?
>
> *Player:* `Balance.` / `She said she needs something done.` / `I know who she is.`
>
> **IN-BETWEENER:** Balance. *(he laughs)* I'm the gap between living and dead. I *feel* it.
> One side of me has never been this heavy. Everything that breathes, pushing.
>
> **IN-BETWEENER:** You want to know what even looks like? *(he holds up his two hands, one
> black, one white, and lowers one)* About half. Half of every living thing, on every world, all
> at once. Your hand. Her idea.
>
> **IN-BETWEENER:** And she can't do it herself, can she? Not allowed. So she found someone
> who'll keep getting up.

**Why the In-Betweener tells them:** he's vain, and knowing something the player doesn't is a
pleasure he's been starved of for centuries. He isn't warning them. He wants to watch their face.

**Why it's the master climax:** from here the player knows everything. They're carrying the stones
toward the deaths of half of all life. Every stone after this is taken knowingly.

#### Scene 4C: Order and Chaos

New line pairs in the existing WRATH/W_ nodes (`world/InBetweenerDialogue.java`,
`world/OrderAndChaos.java`):

> **ORDER:** You carry four. At six, he comes for you. Eternity. All that is.
>
> **CHAOS:** He won't talk. He can't. You'll just see the sky go.
>
> **ORDER:** And kill him, and everything goes with him. Your world. Hers. *(beat)* Ask yourself
> why the end of all things will want you to keep the universe alive.

**Why they say it:** they're part of Eternity. This is the only time his corner gets a voice, and
it's a warning and a threat in one breath, which is exactly who Order and Chaos are.

#### Scene 4D: Death names the Regulator

*The next time the player kills something, Death is there, silent. She looks at the sky, then
points straight up. Then she cups her hands together around something small, and closes them.*

**What she's showing them:** the Comet (in the sky) and the Regulator (what you hold to contain
something). The Comet has crossed the sky all game (`world/CometDirector.java`), so most players
have seen it.

**Why now:**
- Order and Chaos have told the player Eternity is coming, so keeping it secret is pointless.
- She needs the player to survive Eternity *without killing him*. If he dies, everything ends, her
  included.
- **What will save the universe is what Death wants the player to have.** That's the turn Order's
  line set up.

*Alliance path: after the Soul Stone, Warlock comes down to the hub island with a new deal
(Scene AL-1, §6, "The alliance path").*

---

### Chapter 5: The Garden (Time)

**Local four-corner.**
- The player.
- **The Gardener**, who wants every living thing to live.
- **Death**, who has stood at the edge of his garden before.
- **Warlock**, who for the first time stands *with* someone rather than *for* someone.

#### Stakes

- **For the Gardener:** his life, chosen.
- **For the player:** an Elder's blood on their hands, or no stone.
- **For Warlock:** watching someone do what he hasn't been able to: choose.

#### Direction

The Grandmaster warned the Elders after the Champion fell. The Gardener's answer wasn't to hide.
It was to grow.

- **Told:** Death, at a kill: *she crouches and lays her palm flat on the ground. Where she
  touches it, nothing grows. Then she points at the red planet.*
- **Found:** strange seeds start turning up in the overworld: plants that don't exist, growing
  where nothing should. The Gardener is seeding the inner worlds. Their tooltips read *"Not from
  here."* On Galacticraft's Mars the player sees green from orbit.
- **Felt:** on Mars the stones tick softly, like a clock.

#### Beats

1. **Mars.** The garden: an oasis of impossible growth on the red plain.
2. **The Gardener talks: Scene 5A.** He's kind. If Warlock is a rival, he's there, on the
   Gardener's side.
3. **He tries to kill the player.** His plants strangle them. They die and wake at their spawn,
   on Earth. **Death won't let them in.** Now the Gardener knows exactly what he's dealing with.
4. **The return.** The player comes back to Mars. The Power Stone (existing ability) breaks his
   vines.
5. **He binds the stone: Scene 5B.**
6. **The choice:** kill him for it, or leave without it.

#### Scene 5A: The Gardener

> **GARDENER:** You came a long way. Sit, if you like. *(he goes on working)* Nothing grew here
> for four billion years. Now look.
>
> **GARDENER:** I know who you carry for. She stood at the edge of this garden once, the way she
> does. Waiting for something of mine to die.
>
> **GARDENER:** I won't give you the stone.
>
> *Player:* `I can make you.` / `She'll find another way.` / `What would you do?`
>
> **GARDENER** *(What would you do)***:** I'd put them down. Every one. I'd go home and grow
> something.
>
> **WARLOCK** *(if present, to the Gardener, quietly)***:** They won't.
>
> **GARDENER:** No. *(he stands, still holding his trowel)* I didn't think so.
>
> *The ground opens under the player's feet.*

**Why he attacks:** life is everything he values, and he knows what the stones are for. He's
patient until patience fails.

**Why it fails:** Death won't let the player die. For the first time, the player feels her gift
used as a weapon against someone trying to save half the universe.

#### Scene 5B: The price

*The player is back, and the vines are broken. The Gardener is on his knees among his ruined
plants. He presses the Time Stone into his own chest, and it sinks in. Roots grow out from it
under his skin.*

> **GARDENER:** There. Now it's part of something living.
>
> **GARDENER:** You can have it. *(he looks up at the player)* You'll have to take it out of me.
>
> **WARLOCK** *(if present)***:** Don't. *(to the Gardener)* Don't make them...
>
> **GARDENER:** Someone should know what these cost. *(to Warlock, gently)* Stand aside, child.
> It has to be their hands.
>
> *Player:* `(Take it.)` / `(Leave.)`

**Why he does it:** he can't stop the player and he can't destroy the stone. What he can do is
make sure the stone isn't taken from a pedestal or a pocket. It's taken out of a living thing that
didn't want to die. If the player goes on, they go on knowing that. It's the only weapon he has
left, and it's aimed at their conscience.

**Take it.** Death is kneeling by him the moment he falls, and for the first and only time she
touches someone gently while the player watches: an Elder who opposed her all his life, hers now.
If Warlock is present, he says nothing at all, and leaves without fighting.

**Leave.** The story waits. The Gardener lets them go and goes back to his garden. Death appears
at the player's next kill, silent, and waits. The player can come back. Nothing else moves until
they do.

*Alliance path: the Gardener offers the stone to Warlock's hands instead (§6, "The alliance
path").*

---

### Chapter 6: The Game (Mind)

*Built: `world/GrandmasterDirector.java`, `world/Kingstable.java`, `entity/EntityGrandmaster.java`.
Try it with `/eternity grandmaster begin`.*

**Local four-corner.**
- The player.
- **The Grandmaster**, who wants the best game in a million years.
- **Warlock**, offered victory if he plays for Mother.
- **The rules**: a board that can't be won fairly.

#### Stakes

- **For the Grandmaster:** his life. The device stops only when he dies, and he's sure that will
  never happen. He puts it on the table himself.
- **For the player:** what they lay on the table against it: every stone they carry, or the
  Astral Regulator. If he wins, he keeps it, and nobody gets anything back from him while he's
  alive.
- **For Warlock:** his last chance to bring Mother a stone, and with it, a reason to be kept.

**Why the Grandmaster comes to the player:** he's the only Elder who saw them coming. The
Champion laughed at his warning and the Gardener ignored it. Now he's the last one left, and he'd
rather choose the board than wait to be found.

**Why he wants a stake:** a game with nothing on it isn't a game to him. His life is worth
something, so the other side of the table has to be worth something too. He names the two
things the player has that are: their stones, or the one thing that can save the universe at the
end. Choosing which to risk is the player's first move.

**Direction.** None is needed: he arrives. When a player holding five stones wakes (or at the
next dawn), the board rises around them in rings, gold and blue, three blocks to a square, out of
whatever was there. Everything it covers is kept and put back afterwards.

**The Mind Stone** is in his device, and the device is the board. It leaps from square to square
after every move and only stops if he dies. The player can see where it will land next, and it
can't be caught.

#### The game: the king's table

He calls it hnefatafl, and says the Norse learned it from him and got the rules wrong. It's
played with the pieces themselves, on the real board: the player walks among them, lays a hand
on one of their own, and then on the square it should go to.

- Seven squares by seven. The **throne** is the middle; the four **refuges** are the corners.
- The player holds the **king** and his four **guards**, round the throne. The opponent holds
  eight **raiders**, two on each edge. Raiders move first.
- Every piece moves like a rook. Only the king may stop on the throne or a refuge; raiders may
  cross the empty throne but never stop on it.
- A piece is taken when a move closes it between two enemies. Moving between two enemies is safe.
  The refuges, the empty throne and **the Mind Stone's square** count as an enemy of everyone, and
  nothing may enter or cross the stone's square. The king is armed.
- The king falls like anyone else, except on or beside the throne, where it takes four sides.
- The king wins by reaching a refuge. The raiders win by taking him, by leaving the guards no
  move, or if the game runs too long. The house doesn't play for draws.

**Why this game:** it's asymmetric, and so is the story. The player is the king: hunted,
outnumbered, trying to get off the board, which is the whole chapter in one rule. It's a Norse
game, and Chapter 1 happened in a Norse village. The stone as a moving square that counts against
everyone is his addition: it's how he "keeps it in play", and it means no two games are the same.
Played straight, the king's side is the stronger, so a player who thinks will find a way out;
that's exactly when he cheats.

#### Beats

1. **The board rises: Scene 6A.** He names their stones, and his brothers.
2. **Warlock arrives** at the far side, promised the stone for Mother if he wins.
3. **The stakes.** "My life's on my side of the table. Put yours on the other." The player lays
   their stones, or the Regulator, on the pedestal at the board's edge, where they turn in the air
   for the rest of the game.
4. **The rules**, as a cutscene: the camera leaves the player and the board demonstrates each rule as he
   says it (`client/RulesCutscene.java`, timed by `world/GrandmasterScenes.java`). Skippable. Then a
   written copy (*The King's Table*, by En Dwi Gast).
5. **The game.** Warlock flies to his raiders and carries them. The Grandmaster strolls the edge,
   shuffles cards, watches.
6. **The Grandmaster cheats.** He wants a long game, and he decides how it ends:
   - whenever the king has an open road to a refuge, the board shifts: a raider slides into the
     road under his hand, or rises out of the square, or the king is back on the throne;
   - while Warlock plays, whenever Warlock is about to take the king, the king steps aside, and
     nobody touched him. ("That was my move." "And this is my board.")
7. **Warlock walks: Scene 6B**, after the second cheat, whichever way it went. The Grandmaster
   then plays the raiders himself, harder, and keeps cheating. If he takes the king, the pot is
   his, and he sets the pieces up again.
8. **The way out.** Break the board, not the opponent. The device is the board.
   - **Told:** "It stays in play until the game ends. My rules."
   - **Found:** the stone only ever appears on the board's squares.
   - **Felt:** striking a square cracks it and makes the stone stutter, and he flinches.
   - **Seen:** every time the board shifts, light cracks across the squares under the move. And once,
     after he has walked, Warlock says from the edge: "You're still playing on his board."
   Every blow counts; a blow on the stone's own square counts four times. A square struck three
   times falls in, with whatever stood on it. Enough blows and the board goes, all of it.
9. **The board breaks, and the Grandmaster with it: Scene 6C. Six stones.**

The player can break the board at any point, even before they've played a move. If they do it
before Warlock walks, Warlock never walks: he leaves without a word, and stays a rival into the
finale.

*Alliance path: Warlock refuses the game outright and breaks squares beside the player (§6, "The
alliance path").*

#### Scene 6A: The board

> **GRANDMASTER:** Five! One of them off my brother. I told them. *(he walks between the
> pieces, delighted)* Tryco said, "Let them come." Ord said, "Peace." I said, "Game."
>
> **GRANDMASTER:** You want the last one. *(the stone flickering)* It stays in play until the
> game ends. My rules.
>
> **GRANDMASTER:** And my life's on the table, so you know I'm serious.
>
> *Warlock lands at the far side of the board.*
>
> **GRANDMASTER:** And your opponent! I asked who wanted it more than you. He said yes before I'd
> finished asking. *(to Warlock)* Win, and it's yours. For Mother.
>
> **WARLOCK:** *(to the player)* This is the last chance I'll get.
>
> **GRANDMASTER:** My life's on my side of the table. Put yours on the other.
>
> **GRANDMASTER:** Every stone you carry. Or the Regulator. Nothing less is worth my life.

On the Regulator: "The Regulator. You'd bet the only way out there is on a board game. Good." On
every stone: "All of them. Now it's a game." On anything less: "And the rest."

#### Scene 6B: Warlock walks

*After the Grandmaster's second cheat. Warlock stops.*

> **WARLOCK:** He's moving the board.
>
> **GRANDMASTER:** It's my board.
>
> **WARLOCK:** *(to the player)* It doesn't matter who wins. He decides who wins. *(beat)* That's
> what Mother does.
>
> **GRANDMASTER:** Walk off and you forfeit. No stone. Nothing to take home.
>
> **WARLOCK:** I know.
>
> *He steps off the board.*
>
> **WARLOCK:** She'll grow another one. Let her.
>
> **GRANDMASTER:** Then you're playing me.

**Why he walks:** the rigged game is what Mother's mission has always been, a frame someone else
set with a prize someone else chose. He's been building to this since the In-Betweener's line.
He doesn't help the player; he stops serving the Grandmaster. And he gives up the only thing
keeping him wanted at home. **His turning point is the player's cue:** they've just watched
someone step off the board, and the answer is in front of them.

Warlock's stance becomes **REFUSED**, whatever it was before (`GrandmasterData.warlockRefused`).

#### Scene 6C: The last Elder

*The board breaks outward from the stone, square by square, and every piece goes down with its
square. The Grandmaster's knees go, and light comes through him along cracks. The stone stops.*

> **GRANDMASTER:** *(lying among the broken pieces, oddly pleased)* Not in the rules.
>
> **GRANDMASTER:** Best game in a million years.
>
> *He comes apart into cards, and they blow away.*
>
> *Death is there. She doesn't look at the Grandmaster. She looks at you, holding six.*

The pot comes back to the player. The board sinks, the ground it covered returns, and the Mind
Stone lies on it, still.

**After.** Warlock, if he walked, comes down to the player and says one thing before he goes:

> **WARLOCK:** She'll ask you, at the end. She won't be able to make you. Answer her as
> yourself.

---

### The Finale

The existing systems play out, with new scenes, a new sacrifice and one new decision.

#### The hunt (existing)

Eternity comes. If Warlock is still a **RIVAL** or **BETRAYED** (he never walked off the board,
because the player broke it before he did), he makes one last attempt to take stones before
Eternity arrives.

#### When Eternity falls

**1. The player has the Regulator: containment (existing).** Time recovers, the rifts seal, the
universe holds, and Eternity is held in a crystal in the player's hand. Go to Scene F1.

**2. No Regulator, and Warlock is REFUSED: Warlock's sacrifice.**

*The sky is splitting. Time is slowing to a stop. Warlock comes down out of the fracture to where
Eternity is dying.*

> **WARLOCK:** I was made to hold the stones. *(he looks at the light pouring out of Eternity)*
> I think I was made to hold this.
>
> *Player:* `It'll kill you.` / `Don't.` / *(say nothing)*
>
> **WARLOCK:** Yes. *(beat)* It's mine to choose. That's the point.
>
> *He wraps himself and Eternity's death in the cocoon. Gold, then white. The supernova draws back
> into it, and the cocoon closes, and doesn't hatch. When it cracks open there's only a crystal
> inside, with Eternity held in it, and nothing else.*
>
> *Death is standing beside the cocoon. She kneels and takes something the player can't see into
> her hands: the one soul she could never have taken, given freely.*

**Why it works:** the Sovereign built his cocoon to hold power and remake him; it's the only other
vessel in the cosmos that can take what Eternity pours out. It needs a life inside it to close,
and that life doesn't come back out. He gives it because, for the first time, the choice is his.
The universe is saved by the one character who stopped being an instrument, and the cost is
everything he is.

**3. No Regulator and no Warlock: Unmaking (existing).** Nobody wins, Death included. As the world
comes apart (`world/Unmaking.java`), she appears in her true form, and for the only time in the
story **she's afraid**. She looks at the player, and then she comes apart with everything else.
**Why:** no life, no death. The player has given her everything and nothing.

**4. The player survives without killing him (existing loop).** He returns every 30 minutes and the
story waits. After each encounter Death appears, her cupped hands closing again: *contain him.*

#### Scene F1: She asks

*This plays after containment, by either the Regulator or Warlock, inside the existing 8–15 minute
gap before the Tribunal. Death stands before the player in her human face for the first time since
the vault, in her mourning clothes, composed, standing closer than she should.*

> **DEATH:** You did it.
>
> **DEATH:** No one left to stop you now. Close your hand.
>
> *Player:* `And kill half of everything.` / `Why should I?` / `What happens if I don't?`
>
> **DEATH** *(And kill half)***:** And put it right. More things alive than this world can carry.
> It's never been like this. It won't hold.
>
> **DEATH** *(Why should I)***:** *(she glances aside, at the places those who died on this road
> fell)* Because I'll give them back.
>
> **DEATH:** Hrafn. The Gardener. The old player-of-games. *(if Warlock died)* Him. *(beat)* Their
> souls are mine. I can let them go, the way I let you go. Close your hand, and they walk out of
> me.
>
> **DEATH** *(What happens if I don't)***:** *(she doesn't answer for a long time)* I can't make
> you. You're holding all of it.
>
> **DEATH:** But I'll stop sending you back.
>
> **DEATH:** I'm asking.

**Why she offers the dead back:** her power is over what has already died. She can take, refuse or
release a soul; it's the same power that has kept the player alive. These are the people the
player lost or killed, so it's the most personal thing she can offer. It's also accurate, so it
isn't a lie.

**Why she asks rather than commands:** a player holding six stones outranks her. She can't command
them, and a threat to someone above her is only a statement of what she'll do afterwards. So she
does the only things left: the face that worked at the start, the closeness, the truth, and the
dead.

**Why the Tribunal is her deadline:** it's coming anyway. Using a proxy is exactly what it judges.
If the cull is done when it arrives, she has broken the law and won. If not, she has broken it for
nothing.

#### The choice

| Choice | What happens | Who sacrifices |
| --- | --- | --- |
| **Close your hand** (the cull) | Half of every living thing in every loaded world dies at once. **Hrafn, the Gardener, the Grandmaster, and Warlock (if he died) are standing in the world again, alive, where they fell.** Death inclines her head to the player and is gone. The player keeps their deathlessness | Half of everything |
| **Refuse, and keep the stones** | Death looks at the player for a long time, then she's gone. The dead stay dead. **The player's next death after the Tribunal's verdict is permanent** | The player: their deathlessness, and their dead |
| **Give them to Warlock** (only if he's alive and REFUSED) | He says he'll scatter them across the cosmos, and not take them to Mother. "She'll come after me. Let her." **The player's next death is permanent** | The player, their deathlessness; Warlock, his home, for good |

**Why refusing costs the player their deathlessness:** she kept them alive because they were
useful. An agent who refuses is no longer an investment. Sending them back makes her imbalance
worse with every respawn, so she stops. It's not revenge, just accounting.

#### The Tribunal (existing)

New conditioned lines in `world/TribunalDialogue.java`:
- **If the player culled**, an extra question: "HALF OF ALL THAT LIVES. AT HER WORD. FOR FOUR
  SOULS. WHY?"
- **If the player refused**, a Q5 variant: "YOU WERE HER INSTRUMENT AND YOU REFUSED HER. WHY SHOULD
  I BELIEVE YOU WILL REFUSE THE NEXT?"
- **Earlier choices** (the Gardener taken or left, Warlock's offer kept or broken, Warlock's
  sacrifice) move the score.
- **The Tribunal also rules on Death, offscreen**, for acting through a proxy. A line in every
  verdict: "AND DEATH WILL ANSWER TO ME FOR HER PART IN THIS."

---

### The alliance path

A route where the player and Warlock end the story as allies, fighting Eternity side by side and
standing together when Death asks.

#### It doesn't soften the rivalry

The rule holds. Say no to Warlock once, or keep collecting after saying yes, and he's the
player's rival from then on. He fights them in Ch.2, Ch.3 and Ch.4 whatever path they're on.

The alliance can't happen until **after the midpoint**, and it isn't Warlock being talked out of
it. It happens because by then both of them have changed:

- **He's changed tactics.** He has tried to stop the player three or four times and lost every
  time. He can't stop them, and he knows it.
- **The player knows the truth.** Since `IMBALANCE` they know what "balance" means. Warlock was
  trying to stop someone who didn't know. Now he's facing someone who does, and he wants to know
  what they'll do with it.

He doesn't offer to stop the player. He offers a different deal.

#### Why Warlock would ally with Death's agent

- **Stopping isn't enough.** If the player stops collecting, Death finds someone else. She found
  them, and she'll find the next person nobody has a claim on, who may never stop to ask what
  balance means. The stones stay where they are, and keepers die: the Champion is stranded, the
  In-Betweener can be robbed. Leaving them scattered just postpones the cull.
- **She needs one hand.** If the six are never all in one hand until the moment they're
  destroyed, she never gets her cull. Two hands, one of them a hand she can't influence, beats one.
- **Only all six can break one.** Nothing in creation can destroy a single stone; the Gardener
  can't. But the six together can do anything, including unmaking themselves. The Sovereign built
  Warlock to wield the stones, so he knows what they can do. That's the only place Death can't
  send someone after them.

#### Why the player would ally with him

- He's the one being who has never lied to them, Death included. She tells selective truths; he
  says the whole thing.
- They're facing Eternity at six whether they're allied or not, and he's the only fighter who can
  stand next to them in that fight.
- It's their own purpose. For the first time, the plan isn't something somebody handed them.

#### Why Death doesn't cut the player off when she sees it

The alliance is still gathering the stones, which is all she needs. At the end the six will be
together, even if only for the moment before they're broken, and in that moment she'll ask. She's
patient, and she believes she can still turn the player then. Cutting them off now would only stop
the gathering.

**She does try to split them.** At the next kill after the pact, she appears, silent, looks toward
wherever Warlock is, and then *cups her hands as if around a cocoon, and opens them on a second
one.* Mother is growing another of him. He was made to answer when Mother calls. It's a selective
truth: everything in it is accurate.

---

#### Trust: what earns the pact

Warlock tracks **trust** (`StoryData.warlockTrust`) from what he has *seen* the player do. Acts
count; words only count where his whole grievance is about words.

| When | What the player does | Trust | Why it moves him |
| --- | --- | --- | --- |
| Ch.2 | Tells him no honestly (RIVAL) | **+1** | He hates being lied to. A straight no is respect |
| Ch.2 | Says yes, then takes another stone (BETRAYED) | **−1** | The first lie anyone told him (Scene "TRUCE, then a stone") |
| Ch.3 | **Shields his cocoon from the Champion** | **+1** | See below. Mercy when there was nothing to gain from it, and at a cost |
| Ch.4 | Gives him the Soul Stone | Pact | See Scene AL-1 |

**The Ch.3 cocoon.** In the three-way fight, when Warlock is beaten he cocoons in the middle of
the arena (existing mechanic). The Champion doesn't share a ring: a cocoon in his arena is an
interruption, so he walks over to smash it open while it's helpless.
- The player can stand between them. The Champion turns on them instead, taking the fight to the
  player while it's still two against one in all but name.
- Or the player can let him smash it. Warlock reels out, open and humiliated (existing break
  mechanic), and leaves.

Inside the cocoon, Warlock is aware. He knows who stood between him and the Champion.

> **CHAMPION:** *(to the player, as they block him)* He's not yours to look after.
>
> **CHAMPION:** *(a grin)* All right. You first.

If the player shielded him, Warlock says, as he hatches and leaves:

> **WARLOCK:** *(not looking at them)* Why?
>
> *(He goes before they can answer.)*

#### Scene AL-1: The pact (Ch.4)

*After the Soul Stone is taken and Order and Chaos are dealt with. Warlock, if he's a rival, has
been at the Nexus all chapter. He comes down to the player on the hub island. He doesn't attack.*

**If trust is 1 or more,** he proposes it:

> **WARLOCK:** Now you know.
>
> **WARLOCK:** I can't stop you. I've tried. *(he counts it in his head)* Four times.
>
> **WARLOCK:** If you stop now, she finds someone else. Someone who never asks.
>
> *Player:* `Then what?` / `Why are you telling me?` / `Get out of my way.`
>
> **WARLOCK** *(Then what)***:** She needs them in one hand. So they're never in one hand. *(he holds
> out his)* Give me that one. The one I was made for.
>
> **WARLOCK:** We take the rest between us. And at the end we bring them together once, and break
> them. Nothing breaks a stone but all six. Then there's nothing for her to send anyone after.
>
> *Player:* `(Give him the Soul Stone.)` / `(Keep it.)`

**If trust is 0 or less,** he won't propose anything. He's there to fight. Before or after the
fight, the player can **offer the Soul Stone unasked**: an act, not a word, which is the only kind
of promise a betrayer has left.

> **WARLOCK:** *(not taking it yet)* Why?
>
> *Player:* `Because you were right.` / `Because I need you.` / *(say nothing)*
>
> **WARLOCK:** *(he takes it)* If this is a trick, it's a good one.
>
> *(And then the plan, as above.)*

**Given:**

> *He holds the stone. A long beat. His eyes go gold to the edges.*
>
> **WARLOCK:** ...It knows me.

Stance becomes **ALLY**. Warlock carries the Soul Stone from now on. He stops ambushing the player
and turns up at the beats below instead.

**Kept:** nothing changes. He stays a rival, and the offer never comes again. He knows what it means
when someone keeps the one stone that was made for him.

**What the pact costs.**
- **The player** loses the Soul Stone's powers, maybe for good, and trusts the being who's fought
  them all game with the thing he wants most.
- **Warlock** takes on Eternity's attention, which follows any stone.
- **Both** gamble the cosmos on the other.

---

#### Ch.5 on the alliance path: whose hands

Warlock comes to Mars with the player.

> **GARDENER:** I know who you carry for.
>
> **WARLOCK:** Not any more.
>
> **GARDENER:** *(looking past the player, at what stands behind them)* She's still standing there.

The rest of Scene 5A plays as written: the conversation, then the strangling, because the
Gardener still sees Death's mark on the player. Warlock doesn't fight him; he argues, and the
vines take the player anyway. When the player returns, Scene 5B plays to the binding, then
changes:

> **GARDENER:** *(to Warlock)* You don't carry her mark.
>
> **GARDENER:** Then let it be you. Whoever ends up holding it should be the one who paid for it.
>
> *Player:* `(Take it yourself.)` / `(Let Warlock do it.)` / `(Leave.)`

**Why the Gardener offers it:** he can't stop them, but he can choose whose hand his stone ends up
in. He'd rather it was a hand Death doesn't own. And his whole point is that whoever holds it pays
for it.

**Let Warlock do it.** It's the first life Warlock has ever taken.

> *He kneels. It takes a long time.*
>
> *Death is there when it's done, kneeling across from him over the Gardener's body. For a moment
> they're face to face. He doesn't look away.*
>
> **WARLOCK:** *(after she's gone, quietly)* I didn't think it would be so quiet.

Warlock carries **Time and Soul**. The Gardener's death is on his hands, not the player's, and the
Tribunal knows the difference.

**Why Warlock does it:** it spares the player a second Elder on their conscience, and it keeps
another stone out of the hand Death is watching. He doesn't do it because someone told him to.
That's the first adult thing he does, and it costs him his innocence.

**Take it yourself.** As written. Warlock stays with the player afterwards instead of leaving.

---

#### Ch.6 on the alliance path: no opponent

The Grandmaster has watched everything. He knows they're allied. He offers Warlock the game
anyway, because the bet he's really interested in is *the alliance*.

> **GRANDMASTER:** And your opponent! *(to Warlock)* Win, and it's yours. For Mother. I spoke to
> her. She'd take you back, with this.
>
> **WARLOCK:** No.
>
> **GRANDMASTER:** *(delighted)* You didn't even think about it.
>
> **WARLOCK:** I did. A long time ago. *(he walks to the player's side of the board)*
>
> **GRANDMASTER:** Then it's me. *(rolling up his sleeves)* Better.

The Grandmaster plays the raiders himself from the first move, and cheats from the first move.
Warlock stands at the edge of the board. **When the player starts breaking squares, he breaks them
too.** That's the first time they fight on the same side, and it's against the board, not each
other.

**Why it works here:** Warlock already walked off Mother's board when he made the pact. The game
isn't a temptation any more; it's a confirmation. The Grandmaster loses the only bet he cared
about, and the board.

`GrandmasterData.Warlock` gets a new state, **ALLIED**: Warlock spawns aside (`warlockAside()`)
instead of at his post, there's no PLAYING phase, and his blows on squares count with the
player's.

---

#### The finale on the alliance path

**The hunt.** The alliance holds six between them: four or five in the player's hand, one or two in
Warlock's. Eternity reacts to the stones being gathered, not to one hand, so **he comes for both.**
Warlock fights alongside the player for the whole encounter as a companion: same moves, turned on
Eternity, and cocooning when he's beaten down instead of leaving.

**When Eternity falls.**
- **The player has the Regulator:** containment, as normal.
- **No Regulator:** Warlock's sacrifice plays as written. He dies holding Eternity's death, and the
  stones he carried fall from the cocoon. The player gathers them alone.

**Scene AL-2: Together, once.**

*To break the stones they have to be together, so Warlock hands his over, and for one moment all
six are in the player's hand. Death is there the instant they are, in her human face, as in F1.*

**She speaks to Warlock first.** It's the first time she has ever addressed him.

> **DEATH:** *(to Warlock)* You're right. I don't love anything.
>
> **DEATH:** But I keep count. Half is half; it doesn't have to be any particular half. Your
> mother's people needn't be in it. *(beat)* Tell them to close their hand.
>
> **WARLOCK:** *(a long time)* No.
>
> **WARLOCK:** You'd have let me say yes. That's all I needed to know.

**Why she speaks to him now:** he has influence over the hand that matters, and for the first time
he has something she can offer. A cull chooses nobody in particular, so sparing the Sovereign costs
her nothing. She never addressed him before because he was never worth addressing.

**Then she asks the player.** F1 plays as written: the dead given back, the promise to stop sending
them back. If Warlock died, the offer includes him, and he's the hardest one to refuse.

#### The choice, on the alliance path

| Choice | What happens | Who sacrifices |
| --- | --- | --- |
| **Break them** | Warlock lays his hand over the player's, and they close it together. The six stones crack, and light goes out of them, and they're dust. **They never exist again**: the relic ledger records them as destroyed, and nothing in the world ever spawns another. Death looks at Warlock, for the first time as if he were there, then at the player, and then she's gone. **The player's next death is permanent.** The dead stay dead | **The player:** every stone power they fought for, their deathlessness, their dead. **Warlock:** the purpose he was made for, and Mother, for good. **Death:** everything, permanently. No future agent can ever gather what no longer exists |
| **Break them alone** (Warlock died holding Eternity) | As above, one hand. The cull would have brought him back, and the player didn't take it | The player, and Warlock, whose death stands |
| **The cull** | Warlock won't hand over his stones, or he snatches them back. **The alliance breaks, and the last fight of the game is the player against Warlock** with five stones against his one or two, and Death watching. Beaten, he cocoons; the player takes his stones and closes their hand. Half of everything dies, the dead walk back, and Warlock hatches into a universe with half of it gone, beside the person who did it | Half of everything; the alliance; Warlock |
| **He scatters them** | As in the main finale: he takes all six and goes. "She'll come after me. Let her." Chosen by someone who trusts him with all of it | The player's deathlessness; Warlock's home |

**Why breaking them costs the player their deathlessness:** it's the same reason as refusing.
They're no longer an investment, and every respawn would make Death's imbalance worse for nothing.
This time there's also nothing left that she could ever use them for.

**After the break.** Before the Tribunal arrives:

> **WARLOCK:** Mother made me for those. *(he looks at his empty hand)* I'll have to find out what
> else I'm for.
>
> *Player:* `So will I.` / `Stay.` / *(say nothing)*
>
> **WARLOCK** *(Stay)***:** *(a beat, then he does)*

He stands with the player when the Tribunal comes.

**The Tribunal, on the alliance path.** Additional conditioned lines in
`world/TribunalDialogue.java`:
- "SIX FRAGMENTS OLDER THAN THIS UNIVERSE. BROKEN, BY TWO CREATURES WHO WERE MADE TO SERVE. ON WHOSE
  AUTHORITY?"
- If Warlock is present: "AND YOU, CHILD OF THE SOVEREIGN. YOU STAND WITH THE ONE YOU WERE SENT TO
  STOP. WHY?" (He answers. It counts toward the player's score.)
- If the Gardener died at Warlock's hand, the Tribunal puts that question to him, not the player.

#### How the four corners change

On this path two corners merge in the last act, which is legitimate: Truby's opposition is meant to
shift as the story turns. The tension doesn't drop, because the remaining opponents both press
harder on the alliance's weak point:
- **Death** works on it at once, in her one scene with Warlock and her gestures to the player.
- **Eternity** now hunts two holders, not one.
- **Mother** pulls at Warlock offstage.

The player's weakness, being an instrument, gets its answer in a person rather than a speech: the
alliance is the first purpose in the story that the player chose with someone, instead of taking it
from someone.

---

## 7. Groundwork

The systems every chapter depends on. Build and prove them with **Chapter 1** as a vertical
slice.

### 7.1 `StoryData`: per-player story state

A WorldSavedData, following the existing pattern (`EternityData`, `InBetweenerData`):

```
chapter          CH1 … CH6, FINALE, EPILOGUE
beats            Set<String>   one-shot beats seen
clues            Set<String>   clues found
warlock          UNMET, TRUCE, RIVAL, BETRAYED, REFUSED, ALLY, DEAD
warlockTrust     int           +1 honest no, −1 betrayal, +1 shielding his cocoon (Ch.3)
custody          Set<Stone>    stones Warlock carries on the alliance path
gardener         ALIVE, KILLED, KILLED_BY_WARLOCK
deathRevealed    boolean       after Scene 2A
mortal           boolean       Death has let go; next death is permanent
ending           NONE, CULL, REFUSED, WARLOCK, BROKEN, UNMADE
```

- `RelicLedger` claims advance the chapter.
- `Director`'s level-6 omen starts the Finale.
- Finale and Tribunal outcomes record the ending and set `mortal`.

### 7.2 The Scene engine (in-world cutscenes)

Today, `client/DialogueCamera.java` only turns the player's head toward a speaker while a
dialogue GUI is open. We need:

- **Server:** a `SceneDirector`, tick-driven like the other directors. It freezes the player where
  they stand, positions the actors, steps the timeline, waits for answers, writes `StoryData`, then
  releases the player. **Scenes play where they happen; no teleporting to special dimensions.**
- **Client:** a camera rig (a client-only camera entity set as the render-view entity) moving on
  eased keyframes for position, look-at and FOV. Letterbox, subtitles and speaker plates
  generalised from `client/GuiTribunal.java`. Italic stage-direction lines for Death's silence.
- **Conversations:** one shared engine generalised from `world/InBetweenerDialogue.java`'s
  `Node`/`Answer` model (speakers, flags, effects).
- **Data:** scenes as JSON in `assets/eternity/scenes/`, lines as lang keys in `en_us.lang`.
  Existing dialogue migrates last.
- **Skippable,** and respects `reduceFlashing`.

### 7.3 `StoryNpc` and Death's presence

- **A `StoryNpc` base class** for Ragna, Hrafn, the Gardener and the Grandmaster: look-at,
  gestures, conversation on right-click.
- **Death's presence system:** when the story needs her, she spawns at the next significant death
  near the player (a named NPC, a boss, or any kill while a story beat is pending). Disguised
  before Scene 2A, true form after, human face again in F1. She never appears from nothing.

### 7.4 Chapter 1 content

- Leak events: scattered northern fragments, increasing in density toward the tagged village, and
  later reaching the player's base.
- The Ragna event, and Hrafn.
- The barrow under the mound, the draugr, and the oath-door puzzle.
- The Tesseract moved from loose in the village to the barrow altar.
- Tesseract visions and travel to the vault.

### 7.5 Relic system

- Add `AETHER`, `REALITY_STONE`, `MIND_STONE` and `TIME_STONE` kinds to `compat/Relics.java` and
  `world/RelicLedger.java`.
- The Aether opens into Reality the way the Tesseract opens into Space.
- Remove the vault's Tesseract. Add a spent state for the Eternal Flame.

### 7.6 Warlock

- Natural appearances at the scripted beats, plus a cooldown ambush near objectives while he's
  RIVAL or BETRAYED.
- Beating the player takes one stone: he can't kill them, so he takes what they carry. Eternity's
  attention follows the stone.
- Each defeat raises his starting evolution tier.
- The cocoon sacrifice in the finale.
- **Alliance path:**
  - The Champion goes for Warlock's cocoon in the Ch.3 three-way, and the player can block him.
  - The pact at the Nexus.
  - Warlock carries stones. `Director`'s level-6 trigger counts the alliance's combined set, and
    Eternity hunts both holders.
  - Companion AI for the finale: he follows the player, targets Eternity, and cocoons instead of
    leaving.
  - A final duel if the player chooses the cull.
- **Grandmaster:** an `ALLIED` state in `GrandmasterData.Warlock`. He stands aside from the
  start, the Grandmaster plays the raiders himself, and Warlock's blows on squares count.
- **Relics:** a destroyed state in `RelicLedger` for the Break ending, so no stone ever spawns
  again.

### 7.7 Permanent death

When `mortal` is set and the player dies, they don't respawn: spectator mode, as in hardcore. A
config option can soften this (normal respawns, Death gone from the world) for people who don't
want it, but the default should be the real stake.

### 7.8 New places

Only two: the **Gardener's garden** on Galacticraft's Mars (a structure, not a dimension), and the
**Grandmaster's board**, built around the player wherever they are and removed afterwards.

---

## 8. Decisions for you

1. **The cull.** Half of all loaded living things is mechanically simple. Do other players count?
   And what lasts after it: empty villages, a world statistic, the sky?
2. **Multiplayer.** One true relic means one player per world holds each stone. Does the story
   follow whoever claims the Tesseract, with others as witnesses and helpers? Or is it a
   single-player story that multiplayer can watch?
3. **Warlock taking stones.** It sharpens the rivalry, but losing a stone you fought for hurts.
   Should he take one, or none?
4. **Leak timing.** How early do displacement events start, how often do they come, and how soon
   do they reach the player's base?
5. **The Eternal Flame.** Is spending it permanently too much for a vault you've built as a
   showpiece? It could relight when Odin wakes, which would be a long-term world event.
6. **Warlock between beats on the alliance path.** Does he follow the player around in Ch.5 and
   Ch.6 as a companion, or only turn up at story beats until the finale? Turning up at beats is
   cheaper and keeps his scenes rarer, which gives them more weight.
7. **Breaking the stones.** It's permanent for the world: stone powers, Destroyer Armor socket
   gifts and anything else built on the stones stop working for good. Is that the right price, or
   should a broken-stones world keep something (inert "spent" stones, for display)?

---

## 9. Writing rules

Use these when writing or reviewing any line.

**Don't:**
- Use pet names or flirt in words. Death is attractive through attention, never through lines.
- Use jokes to release tension. If a scene is funny, it's because a character is being
  themselves (the In-Betweener's vanity, the Champion's single-mindedness, the Grandmaster's
  delight), not because they're quipping.
- Write lines that sound like a trailer: "This isn't over." "I've been expecting you." "You're not
  like the others." "You have no idea what you've done." "We're not so different, you and I."
  "Not on my list."
- Explain what the player can see.
- Let characters announce their feelings ("I'm angry", "I'm sorry for your loss"). Show them in
  what they do, or don't say.
- Have anyone tell the player what to feel about Death.
- Kill a character only for shock. Every death in §3 has a reason the character would give.

**Do:**
- Write from want. Every line is the character trying to get something.
- Keep it short. Most lines under fifteen words. Silences are lines.
- Let characters talk past each other. Death never answers Warlock; she answers the player.
- Leave things unsaid that the player will understand later: "You were here." "She was at
  Gunnhild's before Gunnhild was sick." Hrafn not answering "What happens to you?"
- Make answers differ in *intent*, not tone. Never nice / neutral / rude.

---

## Sources

- [Elders of the Universe (Marvel Fandom)](https://marvel.fandom.com/wiki/Elders_of_the_Universe_(Earth-616)):
  the Gardener, the Grandmaster and the Champion as Elders.
- [Death (Marvel Comics) (Wikipedia)](https://en.wikipedia.org/wiki/Death_(Marvel_Comics)): her
  silence, and the balance between living and dead.
