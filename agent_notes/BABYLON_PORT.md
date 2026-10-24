# Gate of Babylon native port (24 October 2026)

This contract concerns `LbcBabylon.java` and the narrowly scoped
`lbc:ported/babylon_gate` acquisition leaf. Runtime qualification is recorded
separately after the shared integration and actual server checks.

## Author rulings

Each held hand independently pays300 every two ticks and emits exactly one
randomly selected projectile at exactly one randomly selected source origin
(user). The source's mutable shared `#random5` cascade is repaired. Native
projectiles receive the actual caster directly; arbitrary nearby tagged selectors
and the global `reikast` marker deletion are removed. Main hand is processed
before offhand, so remaining mana determines whether the second hand can pay.

Do not add immunity to external projectile impulses without asking the user.
Standard native arrow and trident physics remain intact. All direct and delayed
effects must inherit the actual current owner after reflection/Curse (user).
Ordinary allied players and technical roles retain shared native protection;
friendly mobs remain eligible for physical hits.

## Exact source

Read-only source root:
`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/`.

- `players_2t.mcfunction:10` dispatches `items/arcanums/5t/select.mcfunction`
  every two ticks with the radius20 stopper-magic gate.
- `items/arcanums/5t/babylon_gate.mcfunction:1-8` selects the origin and debits
  `babylon_gate_mana_tick`; `config.mcfunction:9,72-73` sets300 and3000/sec.
- `items/arcanums/5t/babylon_gate/sel1.mcfunction` through `sel5.mcfunction`
  supply the exact local motion, ownership and forced dust transition.
- `items/arcanums/5t/babylon_gate/1.mcfunction` through `5.mcfunction` define
  the exact native projectile payloads.

The eyes-relative origins are `(2.5,2,0)`, `(-2.5,2,0)`, `(0,3,0)`,
`(4,0,0)`, `(-4,0,0)`. Their matching local velocity vectors are
`(-.2,.2,3)`, `(.2,.2,3)`, `(0,0,3)`, `(-.5,.3,3)`, `(.5,.3,3)`.
World velocity components retain source milliblock flooring. Native current and
previous rotation are initialized from that velocity before the first tick.

Kinds1-3 are native tipped arrows with Raid Omen amplifiers101/109/110,
source duration20 and colors16746496/16777215/16770560. The native tipped-arrow
item's default potion duration scale `.125` yields two ticks on impact.
The source transport normalizes exactly two remaining ticks to one counter
charge. Its three live payloads are a radius1/Fuse0 creeper, vanilla lightning,
and outer fire fill followed by fractional-coordinate central air destroy.
See [the effect audit](BABYLON_EFFECTS_SOURCE.md) for exact suppression and
counter behavior, including players and mobs, skill19, greed dormancy and
mandatory owner inheritance.
Kind4 is an ordinary native trident. Kind5 is a native firework with
`LifeTime:100`, `ShotAtAngle:1b`, and these four explosions, all trail/twinkle:

| Shape | Color | Fade |
| --- | ---: | ---: |
| small_ball |16754176 |7031040 |
| large_ball |16743168 |0 |
| star |16775936 |7955200 |
| burst |16748800 |8932608 |

## Runtime and lifecycle

Native Polymer ID `skywars2:babylon_gate` uses repeating-command-block proxy,
`lbc:babylongate`, `lbc:tier_a` and `gate_of_babylon`. Flags are
`transformable`, `arcanum`, `arcanum2t`, `gate_of_babylon`. Full original loot
name/lore belongs to the scoped acquisition leaf; author resource pack is
read-only and no duplicated translations are needed.

The constructor initializes config through its supplied `LbcMana`; it does not
access `game.items()` before the containing service is assigned. Holding uses
shared active role, antimagic/noMagic, frost, actual Time Stop and server-freeze
guards. Released native projectiles retain their ordinary physics and lifecycle.
All three native subclasses are unsaved, epoch-marked, retired on unload,
removed on owner forget and match stop, and cannot revive from a plain disk
entity. Target filtering resolves the current native projectile owner.

Shared integration must route native projectile target checks through
`canAffect`, including firework splash, and connect register/tick/load/unload/
forget/stop. `BabylonArrow.doPostHurtEffects` scopes `effectArrow()` only around
the native superclass call, restoring/removing the thread-local reference in
`finally`. Shared living-effect ingress uses that arrow and the three recognized
markers to call `LbcBabylonEffects.accept`; native potion duration scaling and
post-hit hooks are preserved, and a rejected marker never falls through to
vanilla Raid Omen. Current owner is resolved at this real native effect call.
Greed pauses and resumes the native counters (user), superseding the source's
tag-dormancy quirk.

Deterministic package fixture `emit(owner,originIndex,kind)` uses
one-based indices1..5 and spends no mana; `snapshot()` and `count()` inspect
the actual native actors. Audited special-arrow behavior must be reconciled
into shared effect ingress before qualification.

No source server, author pack, existing acquisition pools, user RerollCrafting,
AcidAnt or Helicopter checks are changed by the core implementation.

## Qualified batch evidence

Build `0fb48f1a` passed62 recorded suite results across two actual mode1 matches (144.06s). Exact 306 sources / 837 classes / 2793 resources match the tested artifact. Installed stopped main with rollback `daa2f866`; main was not started. Shared lifecycle, source transport and active-Greed guards are integrated. Three protected user sources are unchanged/excluded. Evidence: `build/verification/babylon-final/`. Boundary fixtures include manual ticks; no real client pixels, actual disk restart/disconnect or peak-capacity performance claim.
