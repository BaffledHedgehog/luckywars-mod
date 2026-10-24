# Void Arcanum native contract (24 October 2026)

Implementation: [LbcVoidArcanum.java](../src/main/java/me/skywars2/game/LbcVoidArcanum.java).
Presentation/source audit: [NEUTRAL_CONTINUATIONS_RESOURCES.md](NEUTRAL_CONTINUATIONS_RESOURCES.md).
Author resource pack remains read-only; no new presentation assets are required.

## Source

Read-only original LBC functions under
`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/`:
`items/arcanums/void_curse`, `void_curse2`, `void_curse_spawn`,
`void_curse_spawn2`, `void_curse_tick`, `raycast/voidcurse`,
and `raycast/voidcursed1` through `voidcursed5` (all `.mcfunction`).

Each source hand activates for4000 and creates a marker four local blocks
ahead of the feet-based origin. A hand's upkeep loops over every owned hole,
which duplicates charges when both hands are held. The source crowds markers
within10 together and chooses tiers1–5 by their count. Direct generic damage
amounts are .2/.25/.3/.35/.4 each active frame, with hurt radii1.5/2/4/8/24.
Native HurtTime is retained. Nonplayer teleport radii are3/4/8/12/30, homing
speed is .13 except .5 at tier5. Source terrain is cubic, with drops in tiers1/2,
strict deletion in tiers3/4, and a2% air template plus obsidian cube at tier5.
Old code contains no usable indirect kill attribution; the port supplies it.

## Author decisions and current implementation

- Nearby holes within10 merge across any owners into one native body (user).
  All original contributions remain, including contributions beyond the level5
  cap. Removing one owner removes only that owner's contributions.
- Any combination involving different player teams becomes permanently WILD
  (user), including an extra hostile contribution after reaching tier5.
  WILD homing/direct damage can target owners and allies. Technical roles remain
  protected by the shared native target guard.
- Small shader models are used for tiers1–4, the large disc at tier5 (user).
  The lens boundary equals the terrain destruction radius, using radial
  radii1/2/4/8/24. Shared shader scale is radius/14, matching author RP
  `black_hole_trace.glsl:29` and `black_hole_image.glsl:21` (`BH_REACH=14.0`). Obsidian placement and
  the probabilistic template are removed (user).
- Native velocity replaces source teleport for ordinary nonplayer entities,
  including friendly mobs, dropped items and projectiles (user). Acceleration
  scales by tier from the shared Duplicating .18 inner/.012 outer reference.
  Players retain the source absence of physical pull, including WILD players.
  Ordinary direct physical damage can hurt friendly mobs; ordinary homing seeks
  enemies. Ordinary allied players and the current author are protected.
- Each actual hand transition to holding the item can activate once for4000.
  Native successful F swap resets the hand latch, allowing another activation
  even when an earlier hole remains. All owned contributions survive while
  either hand retains an arcanum. Unequip both hands, death, logout, insufficient
  upkeep, magic cancellation and match stop release their owned contributions.

The static item is `skywars2:void_arcanum`, Polymer repeating_command_block,
model `lbc:arcanumblackhole`, tierA, with the original craftusable/transformable/
arcanum/arcanumt/void_curse flags. LbcItems construction initializes dynamic source score
holders4000/11/220. The scoped loot table retains the exact source name/lore;
the Java default item does not duplicate those dynamic lore components.

Follow-up author ruling (user): a merged body's total upkeep doubles by level:
11/22/44/88/176 at levels1/2/3/4/5. Extra contributions beyond5 retain their
ownership and WILD significance, while the body's total cost remains176 (explicit final author answer, user).
That total is split proportionally by each owner's original contribution count.
Integer shares round down, then indivisible remainder points rotate between
owners each active tick, preserving the exact total. Merging precedes the
frame's charge, and the shares are captured before a failed payer can change
the body's membership. Each failed payer loses only their contributions.

Follow-up author ruling (user): credit belongs to the owner with the most
contributions. Equal majority owners are sampled uniformly by native world
randomness, giving50/50 for two tied owners. The chosen owner's original
contribution cause/order is retained. These rulings replace the earlier
provisional per-contribution price and round-robin source selection.

## Native lifecycle, attribution and terrain

One native Marker subclass owns each body. `shouldBeSaved()` is false. Its
tags include `skywars2.void_arcanum`, `void_curse` and `magic`, plus current item
epoch. The internal contribution record exists before native publication;
synchronous loaded callbacks therefore see ownership. Loaded stale markers
with no current internal body are discarded. Native unload releases the body,
visual and terrain work. No marker NBT tick loop or persistent source crowd.

Contributions retain UUID and the original indirect-cause order. Real direct
harm uses native GENERIC with the chosen player source; WILD uses the narrow
`WildDamage` subclass for the shared friendly-fire exception. Terrain support
loss, native block side effects and displacement reuse the selected contribution's exact cause,
so continuing motion does not mint a new influence order and overwrite a later
real player hit. WILD causes retain that policy for later terrain/fall deaths.

Terrain uses the existing `BlockAreaWork` with early AIR/empty-section filtering,
resident chunks only, one moving work set per body, and no old-region backlog.
Successive radial fronts grow from the center after inner work drains. Work
clips to the body's latest bounds and checks its live spherical radius and
antimagic before each write. Tiers1/2 use native block destruction with drops;
higher tiers erase containers without their inventories dropping. Global
terrain allowance is8192 scanned cells and approximately4ms per active frame;
the first serviced hole rotates so large holes do not starve later bodies.
This is a code budget, not a measured performance guarantee.

Homing preserves the source's unlimited nearest target in the loaded dimension,
iterating loaded native entities with resident-chunk/role/team checks and no
forced chunk loading. Physical damage/pull use one source-tier local query.
Each terrain batch shares one support-candidate query across its block writes.
Global Time Stop
pauses physical simulation/upkeep, while owner and body stop tags pause their
active frame. Frost pauses that owner's active work; magic cancellation removes
its contributions and antimagic at the body cancels that body.

Integration API: `register`, constructor, `tick`, `forget`, `stop`, `handSwapped`,
`loaded(Entity)`, `unloaded(Entity)` and static `isArcanum(ItemStack)`.
Package fixtures: `spawn(owner,Vec3)`, `snapshot()`, `upkeep(UUID)`, `source(Hole)`,
`physics`, `terrain`, `updateVisual` and Hole fields `level`, `point`, `marker`,
`members`, `wild`. Renderer integration uses
shared create/update with disc flag and rebind; indirect integration requires
the original-cause support-removal and displacement overloads.

## Verification scope

The source subtask performed no runtime qualification. Parent integration wires
registration, lifecycle, successful native F, WILD damage, original terrain
causes and mutable shared visuals. Qualification below records the final artifact;
its native fixtures include explicit boundary calls and bounded terrain probes.

## Qualification

Qualified build `e9548260` passed 50 recorded suite results in two actual matches (162.45s). Exact 299 sources / 815 classes / 2790 resources match the tested artifact. Installed stopped main; rollback `04c4694d`, main not started. Includes manual boundary ticks and small terrain probes; no client pixels, full radius24 dense-load performance or actual disk restart/unload/disconnect claim. Evidence: `build/verification/void-arcanum-final/`.
