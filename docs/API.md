# Eternity addon API

Everything an addon needs lives in `com.matoon.eternity.api`. What is there is kept working from one Eternity release to
the next (`EternityAPI.API_VERSION` says which); the rest of the mod is public Java too and an addon may reach into it,
but those classes can change without notice.

## Setting up

`./gradlew build` makes two extra jars in `build/libs`:

- `Eternity-1.12.2-<version>-dev.jar`: the whole mod, unobfuscated. Put it on your compile classpath and in your dev
  client's `run/mods` (with Eternity's own dependencies: LucraftCore, HeroesExpansion, InfinityCraft, Galacticraft and
  TickrateChanger).
- `Eternity-1.12.2-<version>-api.jar`: the API package and its sources only. Enough to compile against unless you extend
  `CosmicAbility`, whose superclass is in the full jar.

Depend on Eternity in your `@Mod`: `dependencies = "required-after:eternity"`.

## The ability bar: forces and colours

Every Lucraft ability Eternity adds is framed on the ability bar in the colour of whoever or whatever it belongs to:

| Force | Frame |
| --- | --- |
| Eternity | magenta |
| The Living Tribunal | brown |
| Master Order | white |
| Lord Chaos | cyan |
| The In-Betweener | gray |
| Death | black |
| Space / Mind / Reality / Power / Time / Soul Stone | light blue / yellow / red / purple / green / orange (InfinityCraft's own) |

That covers the Regulator's powers held in a hand or set in the Destroyer Armor's chest, the stones' powers Eternity adds
to the gauntlet (time, mind control, the snap and its modes), the Reality and Soul Stones' powers at the Gardener's
hedge, and the minds the Mind Stone holds.

Tint your own abilities with `Force.X.tint(ability)` or `EternityAPI.tint(ability, EnumAbilityBarColor.X)`. Abilities a
`RegulatorBeing` grants are tinted with its `bar(...)` colour for you.

## New beings for the Astral Regulator

Register in pre-init. The being's powers then come onto the ability bar whenever a Regulator holding it is in a hand or
in the Destroyer Armor's chest, its energy is added to what the Regulator gives off, and the Regulator's tooltip lists it
with its powers.

```java
@Mod.EventHandler
public void preInit(FMLPreInitializationEvent event) {
    RegulatorBeing being = EternityAPI.registerBeing(RegulatorBeing.builder("myaddon:galactus")
            .name("entity.myaddon.galactus.name", 0x8FD0FF)              // tooltip name and colour
            .header("tooltip.myaddon.hungers", 0x4FA0E0, 0xD8F0FF)       // heading over its powers
            .bar(EnumAbilityBarColor.BLUE)                               // ability bar frame
            .energy(750_000_000L)                                        // FE/t it adds
            .powers("devour", "power_cosmic")                            // listed: myaddon.abilities.<key>.name,
                                                                         // icon myaddon:textures/abilities/<key>.png
            .grants((entity, map) -> {
                map.put("devour", new AbilityDevour(entity).setMaxCooldown(200));
                map.put("power_cosmic", new AbilityPowerCosmic(entity).setMaxCooldown(600));
            })
            .entity(EntityGalactus.class)                                // the Regulator can be used on him
            .sensedBy((player, held) -> !held.contains("myaddon:galactus") && EntityGalactus.beatenNear(player) != null)
            .absorbedBy((player, regulator, hand, held) -> {
                EntityGalactus g = EntityGalactus.beatenNear(player);
                if (held.contains("myaddon:galactus") || g == null) {
                    return false;
                }
                EternityAPI.absorb(regulator, "myaddon:galactus");
                g.setDead();
                return true;
            })
            .build());
    if (event.getSide().isClient()) {
        being.setFigure((shrink, time) -> MyRenders.drawGalactusSmall(shrink, time));   // optional, in the lens
    }
}
```

- `absorbedBy` runs on the server when a Regulator is raised; beings are tried in registration order (Eternity's own
  first) and the first that takes returns true. `sensedBy` is its client-side guess, only used to swing the hand.
  You can skip both and call `EternityAPI.absorb(stack, id)` from your own fight instead.
- `EternityAPI.newRegulator(ids...)` makes a Regulator already holding beings (for loot or rewards), without the event.

## Abilities

Extend `CosmicAbility` for a power that acts once on its key: implement `use()`, which runs on the server and returns
whether the power went off (no cooldown starts if not). As with every Lucraft ability, register an `AbilityEntry` for
each class and give it a constructor taking only the entity:

```java
@SubscribeEvent
public static void onRegisterAbilities(RegistryEvent.Register<AbilityEntry> event) {
    event.getRegistry().register(new AbilityEntry(AbilityDevour.class, new ResourceLocation("myaddon", "devour")));
}

public static class AbilityDevour extends CosmicAbility {
    public AbilityDevour(EntityLivingBase entity) {
        super(entity, new ResourceLocation("myaddon", "textures/abilities/devour.png"));
    }

    @Override
    protected boolean use() {
        EntityLivingBase target = EternityAPI.sighted(entity, 40D);
        return target != null && EternityAPI.strike(target, EternityAPI.cosmicDamage("devour", entity), 0.25F);
    }
}
```

## Events

Posted on `MinecraftForge.EVENT_BUS`, server side:

| Event | When |
| --- | --- |
| `RegulatorAbsorbEvent` | A Regulator has taken a being in (Eternity's or an addon's) |
| `SnapEvent.Pre` | Someone with all six stones snaps; cancel it and the snap fails, with no wear or burn |
| `SnapEvent.Post` | A snap worked: the gauntlet has worn, the arm burned |
| `StoryBeatEvent` | A beat of the story is recorded for the first time (a player's, or the world's when the player is null) |

## Everything else in `EternityAPI`

| Area | Methods |
| --- | --- |
| Regulator | `beings`, `being`, `regulator`, `isRegulator`, `absorbed`, `absorb`, `newRegulator`, `holds`, `holdsCharged`, `energyPerTick` |
| Bar colours | `tint`, `color(EnumInfinityStone)` |
| Stones | `wields`, `snap`, `snapMode`, `dust`, `timeRate`, `setTimeRate`, `resetTimeRate` |
| World | `mayDestroyBlocks`, `mayGrief`, `erase`, `carve`, `teleport`, `nexusDimension`, `vaultDimension`, `starChamberDimension`, `sovereignDimension`, `tamarataDimension`, `aim`, `sighted` |
| Harm | `cosmicDamage`, `strike`, `shove`, `place`, `pilotedArmor`, `sheltered` |
| Story | `storyEnabled`, `hasSeen`, `see`, `worldHasSeen`, `worldSee`, `fellows`, `fellowship`, `devotion`, `shout` |

Rules worth keeping to, as Eternity itself does:

- Anything that breaks or rewrites blocks checks `mayDestroyBlocks()` (the server's no-grief switch).
- A boss attack hits through `strike`/`shove`/`place`, so a player inside the Destroyer Armor is fought the same way, and
  an area attack skips anyone `sheltered` (the armour around them is hit instead).
- Name your story beats `<modid>:<name>` so they never collide with Eternity's.
