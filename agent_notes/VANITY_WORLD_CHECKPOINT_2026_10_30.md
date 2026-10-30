# Vanity native world metadata checkpoint — 30 October 2026

Status: NATIVE COMPONENT, NOT RELEASED. Full original25 Chaos gate remains CLOSED.
Vanity acquisition remains gated. This extends the partial rewind components; it
is not the full author-requested world/all-player transaction.

## Native ownership and image

MatchWorldCheckpoint is an explicit GLOBAL owner, separate from partial-chunk
terrain restoration. It composes before terrain/body/view restoration and the final
world-RNG replay. It retains existing level/saved-data/service/container identities.
Restoration requires the same stable match/epoch, native main thread and frozen
simulation. It rejects replacement dimensions, primary data, services, resource
manager/reloadable registries or changed data configuration before writing saved
fields. Resource reloads and dimension replacement need a separate coordinator.

Native 26.3 clocks and saved weather are server-wide; WorldBorder is per dimension.
The image records all current clock definitions and original ServerClockInstance
objects, raw totalTicks/partialTick/rate/paused values, and independent marker-map
contents. Reinstating the original map entries preserves existing borrowed clock
references after future removal/replacement. Native loadFrom restores the fractional
state, unlike setTotalTicks (which would clear partialTick). No clock runs during
capture/replay. Native dirty state and environment cache invalidation follow replay.

Weather captures independent clear/rain/thunder timers and both boolean flags.
Every dimension has its own previous/current raw rain and thunder amplitudes and
skyDarken image. Native setRainLevel/setThunderLevel collapse interpolation history;
typed fields preserve it instead. Raw thunder is not confused with the rain-scaled
getThunderLevel result. Normal native weather ticking resumes afterwards.

Every border retains its own center, absolute limit, damage/safe distances, warning
parameters and initialized flag. Static native geometry rebuilds after field replay.
Moving borders preserve the ORIGINAL extent object and its immutable from/to and
begin/end/duration anchors, plus saved mutable progress/current/previous size.
Recreating a lerp from its current size would lose the original speed/history.
The private native BorderExtent interface prevents a normal typed pointer accessor;
one reviewed reflective extent field retains that object. All mutable primitives
use typed mixins. Current native listeners/transports stay live; checkpoint replay
does not replay historical border-listener actions. Final full border packets use
each CURRENT player's own dimension, not a single shared overworld border.

PrimaryLevelData is the canonical owner of shared gameTime, initialized state,
immutable LevelSettings and raw world spawn. DerivedLevelData delegates and has
no-op setters for several of these fields; writing every dimension separately would
be incorrect. LevelSettings retains default mode, difficulty/hardcore/lock,
allowCommands, name and unchanged data configuration. It does not change current
players' individual modes. Raw and effective border-adjusted spawn are independent
images; respawn positions are immutable copies and angles are not recanonicalized.

The original GameRules/GameRuleMap and native reference-map objects remain live.
All saved Boolean/Integer rule values and key membership restore without codec
clamping, followed by native derived-rule notifications and spawn-policy refresh.
Unknown mutable rule value types are rejected. Full clock, weather, spawn,
difficulty and dimension-specific border packets publish through CURRENT player
connections. This is native packet proof, not a physical-client/GPU result.

Unused function/scheduled-function support is deliberately outside this component.
No TimerQueue adapter, scheduler accessor or extra function resources are shipped.
Ordinary mod queues still need their own full-rewind ownership; this omission is
not permission to skip existing gameplay queues or their clocks.

## Exact native evidence

Final immutable artifact: `build\verification\skywars2-vanity-world-fde7cd7d.jar`.
SHA256: `fde7cd7d850c36a8a4a46e2da79a852652a9507cb1e0de9a536b005b746b8125`.
11,267 frozen source/build/runner inputs; every one of the
5,411 processed class/resource entries matches the exact JAR.
Source receipts cover 171 native archive entries and existing
legacy/canonical owners, 208 total. Native source archive remains
`1ad6e7adb36253216767c99b8fccc48da3a1b1eb53aef0bddc51936d5e873a6a`.

VanityWorldChecks runs in an actual mode1 match with two native players observing
different dimensions. It mutates/replaces clock entries and marker sets, removes a
rule, changes settings/spawns/weather and replaces border extents. Repeated restore
must reproduce every captured metadata field, clock/container identity, raw weather
interpolation and native border samples/lerp speed. Native clocks and borders run
for four real world ticks TWICE; continuation must equal the saved native trajectory.
An explicit paused-clock image retains its rate and fractional phase through actual
manager ticking. A live (unfrozen) restore rejects without changing saved fields.
Captured outgoing packets prove per-dimension border selection and full clock sync.
Actual emitted teleport IDs receive native synthetic-client acknowledgements;
cleanup verifies original dimensions and absence of leftover transfer immunity.

The first expanded predecessor passed the new component but exposed a TEST cleanup
error: the borrowed second player stayed in Nether, causing the subsequent lighting
and Time Stop preconditions to fail. The fixture now explicitly returns that native
player to the original dimension before its existing body/terrain cleanup and checks
both players' worlds. A second predecessor exposed native dimension-transfer
immunity because the synthetic client had not acknowledged its actual teleport ID.
The final fixture now processes real emitted teleport/load acknowledgements in both
directions and checks that transfer immunity is gone; existing damage tests stay
unchanged. Rejected runs are retained under 6790085b and fd5a6dd9 logs/results.
The final build also excludes the unneeded function implementation and test resources.

FINAL artifact passed TWENTY-FIVE suites in ONE actual mode1 match:
`vanity-world`, `vanity-random`, `vanity-bound-views`, `vanity-workstations`, `vanity-storage`, `vanity-views`, `vanity-actors`, `vanity-player`, `vanity-core`, `vanity-terrain`, `map-lighting`, `time-stop`, `witch-acquisition`, `witch-lifecycle`, `polymorph`, `effect-layers`, `projectile-body-collision`, `entity-merging`, `momentum-teleports`, `glass-armor`, `indirect-kills`, `enchantment-rules`, `advanced-anvil`, `tiny-reforge`, `reroll-machine`.
Runner elapsed 67.25 seconds. No suite/cleanup errors; normal owned
child shutdown, exact tracked snapshots restored and no retained locks/live child.
Focus mode leaves qualified:false; the completed suite list defines this evidence.
Existing bootstrap warnings remain. No arbitrary-map, physical-client or peak-TPS
claim is made. Tiny/Reroll use the existing explicit temporary loot probe and native
forced crafting-chunk preparation. Its pack, level metadata and ticket files restore
only after confirmed normal shutdown; no function probe is included in the final pack.

All 199 pre-existing author dirty paths and all 31 live grimoire override files remain
byte-identical. Main JAR remains
`d9087f0a07a321a5a9e57cac7f6f58a2d4ca6a86da9829a52623799f0342e087`.
No author RP/template/question-file edits, main install/restart, push or partial
release. Exact evidence is under `build/verification/vanity-world/`: baseline,
candidate/frozen inputs, source receipts, numbered build/run logs, final-audit,
prepared-fixture and bounded local commit receipt. Earlier root-documentation tails
are preserved byte-for-byte.

## Remaining work

Retain all canonical core/terrain/body/actor/combat/menu/RNG owners. Still needed:
private/custom GUI and block-entity/AI state, ability/attribution/multipart/Time Stop
owners, writer/input/lifecycle quarantine, automatic offline replay, full map/chunk/
storage reconciliation and the final all-player transaction. Other world/private
saved-data systems are not covered by this metadata component. Full Vanity stays
gated until the complete author contract and release chain are verified.
