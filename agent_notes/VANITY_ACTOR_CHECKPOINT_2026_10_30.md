# Native actor bodies and relationships for the Vanity checkpoint

## Author contract and release boundary

(user) Vanity rewinds the FULL match: world/map, ALL players including killed,
offline and newly joined players, items, effects, statuses, attributes, native and
private counters, tags/NBT, positions, dimensions and motion. Captured offline
players retain their restored state until login; later newcomers become observers.
Authority control follows its current actual owner.

This stage adds registered native actor bodies and explicit relationships to the
existing terrain/core/player components. It is NOT the full Vanity implementation.
Acquisition remains disabled and the full original25 Chaos release gate stays CLOSED.
No main installation/restart/push, reduced pool, RP/template or question-file edits.
Questions belong in CHAT. Full private AI/service/world coordination remains required.

## Shared native value image

`MatchEntityState` now owns the common NBT, attributes, metadata and runtime image
for BOTH player bodies and other entities. `MatchPlayerCheckpoint` delegates that
phase and retains its native respawn/progress/menu/cooldown/network responsibilities.
The previous `MatchPlayerRuntime` became `MatchEntityRuntime`, conditional on native
Entity/LivingEntity/Player/ServerPlayer/Projectile type. There is no second effect,
hidden-health, attribute or movement implementation for mobs.

Native NBT, serializer-copied metadata (including defaults), complete native attribute
values including transient modifiers, signed hidden effects, independent owned leases,
ShadowAbsorption and the reviewed native runtime allowlist are copied independently.
Raw motion survives the native NBT limit of 10. Full-width timers survive NBT shorts.
Final position is set after pose/scale metadata callbacks. Passenger NBT is removed
from each independent body image; native load must not manufacture duplicate children.
Every replay copies the saved image again, so later mutation/lease withdrawal cannot
consume the checkpoint. This is still a reviewed subset of native transient state,
not recursive cloning of arbitrary Java objects.

## Native actor registration and identity

`MatchActorCheckpoint.captureRegistered` enumerates the native
PersistentEntitySectionManager's section storage across all server levels. It includes
HIDDEN registered sections; ServerLevel.getAllEntities() only exposes visible entities
and missed a native dimension replacement on a repeated restore. Capture does not load
chunks, drain asynchronous queues, generate terrain or consult a synthetic actor list.
Players have their separate body/lifecycle phase and are excluded from this actor set.

The snapshot retains original Java objects, native IDs, UUIDs, classes and body images.
Native cross-dimension replacement may create a different object sharing a saved UUID;
that later alias is retired and the ORIGINAL instance is revived. This keeps existing
private service references usable; it does not restore the values in those services.
Unsaved custom subclasses are supported without replacing them with vanilla factories.

Before any mutation the phase checks the server thread, native freeze, game/epoch,
stable map, fixed identity and resident destinations. An original actor already
UNLOADED_TO_CHUNK is rejected until the full owner reconciles entity region storage.
Missing chunks are never synchronously generated from a half-restored checkpoint.

The body phase cuts native vehicle/passenger references without dismount game events,
then invokes native FINAL setRemoved, unsetRemoved, level assignment and addFreshEntity.
It uses the existing native manager callbacks for sections, UUID/index lookup, tick
lists, trackers, waypoints and dynamic listeners. It does not patch those maps by hand.
Final setRemoved avoids LivingEntity.remove's death-effect spawning and brain clearing.
Neither lethal damage nor death loot is replayed to erase future entities.

Retirement uses CHANGED_DIMENSION for every saved UUID, including a later dimension
alias. DISCARD on that alias would erase restored native scoreboard entries belonging
to the same UUID. Truly future identities use DISCARDED. The original native team,
score and cached ScoreAccess are checked after composition with the core phase.

A narrow server/thread context suppresses only this mod's ENTITY_LOAD/ENTITY_UNLOAD
factory/cleanup listeners during actor rewind. Re-registering an accepted spell must
not recast it, inherit another current source, or re-run drop/encounter admission.
Native manager callbacks and other native tracking behavior still execute. Queued
unload notices for restored identities are explicitly cancelled. Outside that context
the existing gameplay wiring is unchanged.

## Relationship phase and player lifecycle

`restoreBodiesFrozen` and `restoreLinksFrozen` are deliberately separate. Native
player bodies can respawn/change dimension between them. The link phase resolves
UUIDs to actual current player sessions and original restored actor instances:

- Raw native Mob targets, including clearing future targets.
- Projectile owner UUIDs, including saved owners that were unresolved at capture.
- Deep native leash data and angular momentum, plus the actual holder UUID. Fence
  knots normally serialize as a position; the explicit UUID immediately reconnects
  the original knot without manufacturing another one on the next native tick.
- Ordered native passenger graphs and saved passenger poses, using the real
  startRiding(force, sendEventAndTriggers=false) path and native link/passenger packets.
  Vanilla itself places the first player ahead of a previously mounted mob; tests
  compare the recorded snapshot order rather than an invented expected order.

The phase validates native membership even for hidden registered bodies. It returns
an explicit set of unresolved/offline UUIDs instead of silently reporting a complete
restore. The full coordinator must retain these pending relationships. There is no
automatic pending-login hook in this stage.

Native PlayerList.remove can unload a departing sole rider's ENTIRE vehicle tree.
The test therefore repeats the body phase after genuine disconnect, verifies the
pending UUID, performs genuine native login, then explicitly replays core, actor
bodies, player body and relationships. This demonstrates correct phase composition;
it does not claim that arbitrary native login NBT/RootVehicle/ender-pearl ledgers
already have a complete transactional coordinator.

## Exact native proof

Final immutable artifact: `build/verification/skywars2-vanity-actors-ebed6639.jar`.
SHA256: `ebed6639ebe5b535e3ff713b490817f8082729af5d9a2d72730d775806ab71b6`.
11,119 frozen inputs (including both runner scripts), 5,288 processed class/resource
entries matched the exact JAR. Source receipts contain 25 native Java entries and
12 legacy Vanity functions. Build succeeded offline.

The FINAL exact artifact passed THIRTEEN gameplay suites in ONE actual mode1 match:
`vanity-actors`, `vanity-player`, `vanity-core`, `vanity-terrain`, `map-lighting`,
`time-stop`, `witch-acquisition`, `witch-lifecycle`, `polymorph`, `effect-layers`,
`projectile-body-collision`, `entity-merging`, `momentum-teleports`. Runner elapsed
61.18 seconds. The child exited normally; exact tracked fixture-file snapshots were
restored, with no cleanup errors or retained locks. Focus mode intentionally leaves
`qualified:false`; passing a component does not qualify the full release.

All 199 author-owned dirty paths and 31 live loot overrides remain byte-identical.
Main JAR remains `d9087f0a07a321a5a9e57cac7f6f58a2d4ca6a86da9829a52623799f0342e087`.
No main installation/restart/push, question-file or RP/template change occurred.
The earlier `4c1b0aa2` passed the same 13 suites before unchanged source/config line
endings were preserved to minimize the patch. A formatting receipt verifies all
1,874 class files are identical; only the mixin JSON's byte representation changed,
with identical decoded configuration. The final exact JAR was tested again.

Failed native artifacts/logs remain archived: `2b273708` exposed the test's wrong
assumption about passenger order; `f7b5a82d` failed the repeated dimension-alias check.
The actor traversal now includes native hidden sections, rather than visible-only
ServerLevel.getAllEntities. Earlier passing focused/combined receipts are also retained.

Evidence: `build/verification/vanity-actors/{baseline,candidate,sources,final-audit}.json`,
`inputs-ebed6639.json`, SHA-named gameplay log/results, numbered build logs, formatting
receipt and the bounded local commit receipt. The runner's generic measurementLimit
text describes its default suite set; the explicit focus/completed list above defines
THIS stage's native proof.

`VanityActorChecks` creates exclusively owned Gamma and native cow, skeleton, pig,
unsaved custom cow, snowball, primed TNT, item and fence-knot actors. It exercises
real lethal damage/removal, a genuine native cross-dimension clone, genuine player
death/respawn and disconnect/login, and at least four REAL running match ticks between
replays. Original object/UUID/ID lookup, transient attributes, hidden effects and lease
withdrawal, full fire timer, raw motion, native item components/TNT fuse, actual current
owner/target/leash/rider references and native tracker spawn/passenger packets are
asserted. Future items/death drops and an Oozing-bearing mob disappear without child
death-effect spawns. Repeated link replay must be idempotent. No manual world tick,
fake player replacement or deserialized lookalike substitutes for these native paths.

## Required follow-up before acquisition/release

The full input/writer quarantine and multi-component transaction are still missing.
Native freeze permits player ticks. Pending loads/entity region files, later-loaded
chunks and other persistent world IO are not rewound by this registered-body set.
The coordinator must pin/resident-check destinations and reconcile pending/storage
state before these phases. Native simulation clocks, world/map metadata/jobs/baselines,
scheduled world events, final persistence and automatic offline/newcomer handling
remain required alongside the existing terrain/core/player components.

Private ability/encounter queues, attribution ledgers, AI goals/sensors/navigation/
controls, RNG, native combat/reference state outside the reviewed allowlist, player
RootVehicle/ender-pearl/fishing ledgers, multipart poses, non-personal containers,
camera/proxy ownership and packet-only visuals require their owners' snapshot phases.
Native Brain NBT covers serialized memories, not every private runtime AI state.
TimeStop's own underlying-motion/clock/identity ledger also needs full coordination.
Preserving the same object does NOT rewind its arbitrary mutable subclass fields.
These omissions must not be hidden by opening Vanity or the original25 release gate.
No full-world, arbitrary NPC/private-AI replay, physical-client, network-race, peak-TPS
or final-release qualification is claimed by the focused native checks.
