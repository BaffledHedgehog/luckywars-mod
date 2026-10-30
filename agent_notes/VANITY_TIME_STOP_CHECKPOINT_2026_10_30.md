# Vanity Time Stop checkpoint — 30 October 2026

Status: NATIVE COMPONENT, NOT RELEASED. Full original25 Chaos gate remains CLOSED;
Vanity acquisition is still gated. This is a further owner in the full requested
rewind, not the complete all-player/world/IO/lifecycle transaction.

## Canonical ownership

LbcTimeStop.Checkpoint independently saves stopped-dimension durations, per-player
cooldowns, frozen bodies' ORIGINAL pre-freeze position/motion/rotation/gravity,
correction/presentation stamps, immunity caches, accepted ordered raw hit queues,
trail points, and contact leases/cell indexes/shared holder counts. Mutable service
objects are not reused as image values. Frozen body metadata/attributes/NBT stay in
MatchEntityState/MatchPlayerCheckpoint/MatchActorCheckpoint; native scheduled block
and fluid PAYLOADS stay in MatchTerrainCheckpoint. No second terrain/tick/body model
or new function/scheduled-function feature is introduced.

Native combat and Time Stop deferred hits now use ONE MatchDamageSources graph.
MatchActorCheckpoint creates that owner and exposes its current-body UUID lookup.
The extracted helper retains original DamageSource objects/classes, immutable
accepted causes, positions/policy and nested CapturedDamage parents. It independently
records direct/causing references and rebinds registered actors/players through the
canonical body owner. Detached historical anchors/players absent from the captured online roster stay
historical instead of demanding a nonexistent future login; private mutable
anchor runtime still needs its own owner. No plain-source substitution, fresh causal
order, new hit admission or damage replay occurs while restoring the graph. Existing
native combat uses the same helper instead of a separate source-restoration algorithm.

## Restore phases

1. Freeze simulation and quarantine writers in the future full coordinator. Restore
   world metadata first. Capture/replay requires the same stable match/epoch/server
   thread, outside a damage replay, native queue drain/copy/rebase or another rewind.
2. Time Stop prepareFrozen validates contact ownership and loaded chunks, then
   releases future contact geometry and detaches future private maps WITHOUT end(),
   thaw impulses, trail particles/sounds or replaying the future hit queue. Frozen
   body admission and service ticking stay suspended while this transaction is open.
3. LbcTimeStopSchedules.Checkpoint restores the paused clock ANCHORS and derives
   LevelTicks/current LevelChunkTicks links. It does not shift or copy their actual
   scheduled entries. Anchors preserve elapsed pause time: exact saved values after
   world-time rewind, otherwise rebased by currentTime minus capturedTime. Native
   registration/queue-head indexes rebuild against current containers without calling
   added()/end() and converting the same queue twice.
4. Canonical terrain restores native block/fluid payloads, block events and geometry;
   canonical actors/players restore current bodies, physical state and relationships.
5. finishFrozen resolves ALL frozen targets, cached players, hit targets and source
   references before installing anything. Missing UUIDs return an explicit pending
   set; the independent image retains the WHOLE queue and leases for retry. Native
   body type/dimension and contact terrain must match the saved shape. Current loaded
   chunks receive new correctly shared lease objects; all holder/body/cell indexes
   become coherent before fallible native block writes. An install error stays
   quarantined and can release those coherent leases during a trusted-baseline restore.
6. Saved durations/cooldowns, immunity entries, trails and raw ordered hits install
   against current bodies. Trail spatial cells rebuild from the saved points. Server
   correction/presentation stamps rebase to the current monotonic server tick, keeping
   their ages/sentinels. Fresh native motion/TNT presentation packets publish; true
   fuses remain unchanged. Final world RNG replay belongs after all restoration work.

Cancellation discards the active-transaction token without applying an old image;
it requires the frozen native thread but can clean up after epoch/lifecycle changes.
A partial transaction still needs the caller's trusted baseline or normal match
teardown before resuming. Normal Time Stop stop() also invalidates that token, so
match/service cancellation cannot leave the next use of clocks permanently blocked
or allow an abandoned finishFrozen to resurrect the cancelled image.

## Native evidence

FINAL immutable artifact: `build\verification\skywars2-vanity-time-stop-69660e96.jar`.
SHA256: `69660e96ded1fe1bd8edfafc9e216ea040dd17982de30addef0bd2b568a7ba0e`.
11,283 frozen source/build/runner inputs; all 5,424
processed class/resource entries match that exact JAR. Source receipts include
176 native archive entries plus reviewed legacy/canonical owners,
217 total. Native archive SHA remains
`1ad6e7adb36253216767c99b8fccc48da3a1b1eb53aef0bddc51936d5e873a6a`.

VanityTimeStopChecks activates actual clocks and cooldowns in a real mode1 match.
Two exempt players leave three real stopped-time trail points over four native
world ticks. A normal Cow control takes the same two wrapped WILD electric hits
outside the stop; the frozen Cow and a real player queue three interleaved hits.
Two overlapping TNTs share one native collision lease and a falling block has another.
The image captures a genuinely elapsed paused block/fluid clock and original launch
physics. A further source service frame appends a FOURTH trail point after capture:
the forward baseline emits four, while both restores must emit the original three.

Each rewind discards a different future stop, a populated future trail, a queued
99-damage hit and a future chest inventory. It restores real terrain/lighting,
registered actors, native player/core state and source links, then thaws and lets
native world ticks replay the accepted damage. Resulting HP must equal the actual
unpaused control; player damage and current native killer/attacker references match. A separate native player-shaped
source anchor, never registered as a live player, must remain historical and absent
from PlayerList throughout; unlike the captured online owner, it must never become
a pending-login requirement.
No hit applies during restore, no accepted saved hit disappears, no future hit fires,
and native cooldown/scheduler delays/contact collision/pre-freeze versus live frozen
physics/true TNT fuses resume correctly. Shared leases release exactly once back to
CAVE_AIR/AIR. The same immutable image is reused twice.

The first cycle uses genuine one-life death/native replacement respawn. The second
also disconnects the dead owner and exercises explicit pending-UUID deferral, real
native login, current body restoration and retry. Native emitted teleport/load IDs
are acknowledged by the synthetic client. A final real service cancellation during
prepare invalidates the abandoned finish and scheduler gate, and another clocks cast
succeeds. This is explicit native retry proof, NOT automatic durable offline replay.

Initial build errors were fixture-only Level/ServerLevel argument types. Initial
native attempts correctly accepted all three hits but exposed the fixture's wrong
20-HP assumption for a vanilla Cow (actually 10); checks now compare captured native
starting HP and the real control. Rejected logs/results are retained. Predecessor
b93ad03d passed the focused original-resource fixture (28.95s) and the full27 run.
Final review made fallible lease publication coherent, retained already-historical
player sources, and added the cancellation invariant/real lifecycle test; the FINAL artifact below was rebuilt and rerun.

FINAL artifact passed TWENTY-SEVEN suites in ONE actual mode1 match:
`vanity-time-stop`, `vanity-world`, `vanity-random`, `vanity-bound-views`, `vanity-workstations`, `vanity-storage`, `vanity-views`, `vanity-actors`, `vanity-player`, `vanity-core`, `vanity-terrain`, `map-lighting`, `time-stop`, `time-stop-contacts`, `witch-acquisition`, `witch-lifecycle`, `polymorph`, `effect-layers`, `projectile-body-collision`, `entity-merging`, `momentum-teleports`, `glass-armor`, `indirect-kills`, `enchantment-rules`, `advanced-anvil`, `tiny-reforge`, `reroll-machine`.
Runner elapsed 91.13 seconds. Normal owned-child exit, exact tracked
snapshots restored, no suite/cleanup errors or retained locks. Focus mode deliberately
keeps qualified:false; the explicit suite list defines the evidence. Existing startup
warnings remain. No physical-client/GPU, real network latency, arbitrary-map or
peak-TPS claim is made. Tiny/Reroll use their existing explicit temporary loot probe
and native forced crafting-chunk preparation; those pack/level/ticket bytes restore
after normal shutdown. No new gameplay function support is shipped.

All 199 author dirty paths and all 31 live grimoire overrides remain byte-identical.
Main JAR remains
`d9087f0a07a321a5a9e57cac7f6f58a2d4ca6a86da9829a52623799f0342e087`.
No author RP/template/question-file edits, main installation/restart, push or partial
release. Evidence lives under `build/verification/vanity-time-stop/`: baseline,
candidate/frozen inputs, source receipts, numbered builds/runs, SHA-named historical
logs/results, prepared-fixture, final-audit and bounded commit receipt. Prior root
documentation tails remain byte-for-byte intact.

## Remaining full-contract work

All canonical world/core/terrain/body/actor/combat/menu/RNG owners remain required.
The full owner must capture a quiescent boundary, quarantine ALL writers/input/IO,
reconcile all loaded/unloaded map/chunk storage and private ability/AI/block-entity/
attribution/multipart state, and own failure/lifecycle transitions. This component
requires loaded contact chunks and canonical bodies; it does not reconstruct an
unloaded lease's storage or replay an in-progress native callback. It does not promise
full-world restoration when only a bounded terrain scope is restored. Pending offline
images still need automatic durable login/lifecycle ownership without pausing the
whole match indefinitely. None of those requirements may be replaced by the focused
component proof; full Vanity and the original release chain remain gated.
