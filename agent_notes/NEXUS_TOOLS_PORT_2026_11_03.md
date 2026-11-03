# Nexus Compass, Roadgun and Corrupter — 3 November 2026

## Author decisions and source priority

The Compass is suppressed by MAG and the enhanced antimagical artifact, not TECH.
The active, registered Paper `NexusCompass` handler is authoritative: mainhand
right-click, 30-second cooldown, usable outside and inside Nexus, never Imprinted.
Its 58 saved destinations are bundled byte-for-byte from the actual Paper JSON.
The commented datapack implementation is not evidence that the item was disabled.
This corrects the earlier four-flag Nexus input audit without opening ordinary
wand input in either excluded dimension.

Corrupter retains the source global cancellation: each active `sim_cube` step has
a 0.25% chance to remove every such field in the current Nexus, including foreign
owners and newly created children. The author's analogous 0.5% global cancellation
answer for wire-center explosions is recorded for that future port; wire-center
explosions are not implemented by this batch.

## Native behavior

- `LbcNexusToolItems` registers all three as Polymer items with the existing models,
  X/S/A tooltip frames, source names and nonitalic lore. Canonical loot keeps every
  source property except its native item ID. Three scoped `ported/` entries expose
  exactly these completed tools; no broad Nexus reward graph is filtered or opened.
- The Compass reads its bundled table and optional
  `config/luckywars/nexus_coords.json` at startup, successful datapack reload or
  `/skywars reloadconfig`. Malformed live overrides retain the last valid table.
  Use/tick never reads files. Teleport preserves exact XYZ, camera and momentum.
  Canonical scoreboard cooldown and active-tick remainder survive player/world
  restoration; personal/global TimeStop pauses the clock.
- Roadgun is Nexus-only. Its native BlockDisplay carries the player along the
  initial direction, independently of subsequent camera movement. The 35 moving
  steps cover 525 blocks. Ages 1–36 paint the source thirteen 7×7×7 white-concrete
  samples with independent integrity 0.002. Geometric skipping preserves that
  distribution without thousands of empty trials per frame. At age 39 it dismounts,
  creates the source 5×1×5 finish pad below the actual player, and plays the source
  trident sound. Slow Falling lasts 300 ticks; cooldown is 15 seconds. Early exit
  abandons the ride without a finish pad. One inert AIR-display tick is omitted.
- Corrupter is Nexus-only and shares the existing fair, budgeted ray queue with
  exact entity hitboxes and non-air block interception. Its narrowly scoped Nexus
  admission does not widen ordinary-wand admission or collision. A hit creates one
  aligned native field and starts the 20-second cooldown; a miss costs nothing.
  Each field writes black concrete, independently branches with source 50% chances
  to six nonblack neighbors, removes source-eligible nonmob entities and deals
  10 outside-border damage to one live source-eligible mob. WILD damage retains
  captured ownership. Children advance on later ticks; work has a shared CPU budget.

Right-click air and block dispatch share the same handler and common action locks.
Only owned actors are indexed; no global entity scan, tick NBT read or per-use file
IO is added. Temporary async chunk tickets are released after the last dependent
actor and on cleanup. Typed resident state rebinds actual native bodies, owners,
directions and ages during full-world rollback. Canonical native snapshots own
poses and passengers; no parallel entity/NBT ledger is introduced.

## Remaining boundaries

The green wire is unjammable by author decision, but its complete `random_glack`
chain still reaches TMT and other unfinished payloads. TMT remains last. Nexus
trees, converters, center cubes, recursive wire explosions and the other dedicated
tools remain separate work. The full Nexus, skill2 and broad full-rewind release
gates are unchanged. No resource-pack change is necessary for these three models.

## Verification

Evidence is recorded under `build/verification/nexus-tools-2026-11-03/`.
Final test `7d241372` / production `6eda26e2`: successful Gradle `compileJava`,
six groups in six actual mode1 matches on the exact final test artifact:
`nexus-tools`, `vanity-rollback`, `dimension-items`, `ray-queue-fairness`,
`raw-raycast-spells`, `nexus-red-wire`. The first covers real air/block input,
mainhand restriction, cache/reload, source MAG rejection, momentum/look, active
cooldown/TimeStop, actual same-team WILD death and native credit, cross-owner
cancellation, typed paused actors and the full 525-block ride/finish pad. The full
coordinator repeatedly restores a nonempty paused Nexus field and its owner.

The source proof checks byte-identical 58-point Paper coordinates and all source
loot parameters. The package proof guards 9867 other inputs and 272 existing author
paths, replaces fourteen freshly compiled class families and eight JSON resources
in the previously qualified Loom archives, verifies every archive entry, and keeps
map authoring/deletion classes absent from production. Both final server JARs are
published to `build/libs` after all native checks and frozen-input verification.

Early fixture failures are retained as evidence: an existing 32-second test-player
cooldown was not cleared, and a test assumed a vanilla cow had 20 rather than 10 HP.
The fixture now owns its initial cooldowns and waits for an actual native death;
production match cleanup also clears the tools' owned cooldowns. A failed run's
normal shutdown exceeded the harness's 180-second wait and then completed naturally.
Its durable core/auxiliary snapshots were restored with no forced termination.
Only the isolated wrapper's normal-stop allowance was extended to 600 seconds.
The final run exited normally, restored controlled bytes exactly and retained no locks.

No main-server stop/start, original resource-pack edits, physical-client rendering
or peak-TPS qualification is claimed. Installation is tracked by `main-install.json`.

Installed to the main test-server mods directory after verified normal shutdown,
with rollback `602fd22c` and `restarted:false`. Both build/libs server JARs match
the qualified artifacts. The main server was not stopped or started by this work.
