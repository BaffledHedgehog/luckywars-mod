# Native player actor ledgers for the Vanity checkpoint

## Contract and release boundary

The [full author contract](VANITY_ACTOR_CHECKPOINT_2026_10_30.md) still applies:
rewind the entire match, all players, map and native/private state, including motion,
offline players and newcomers. This component does NOT complete that transaction.
Vanity acquisition remains disabled; the full original25 Chaos gate stays CLOSED.
No main installation/restart/push, pool reduction, RP/template or question-file edits.
Questions belong in CHAT; preserve the author's pending answers and live loot overrides.

## Player ledgers and original native identities

`MatchPlayerLinks` composes with the existing two-phase `MatchActorCheckpoint`.
It records each connected player's pearl UUID set and fishing-hook UUID independently
of the mutable native collections. Capture rejects inconsistent/unregistered ledger
entries instead of silently losing them. Ordinary actor images still own each actual
pearl, hook and vehicle; no parallel entity factory or serialization system was added.

Native pearl owner assignment automatically registers a pearl on its current player.
During body replay that player may still be the dead body about to be replaced by a
genuine respawn. Resolving an EntityReference after removal can find the new body,
so subsequent deregistration alone does not clear the old body's set. The ledger owner
therefore tracks every actual player body observed across the phases and clears old,
intermediate, current and newcomer references before rebinding. The final phase rebuilds
the captured sets and fishing pointer on the actual current UUID session. Missing
players remain explicit pending UUIDs; removed player bodies retain no stale ledger.

Projectile replay also clears a later owner when the captured projectile was ownerless.
Hooked entities are recorded as UUIDs and rebound through native `setHookedEntity`,
which updates both the reference and the metadata's native entity ID after respawn.
Ordered vehicle/passenger graphs retain their existing actor owner.

## Native persistence and client pairing

The exact 26.3 source distinguishes three operations. `ServerPlayer.load` reads the
player body; it does NOT itself spawn the embedded vehicle or pearls. Native
`PrepareSpawnTask` separately calls `loadAndSpawnEnderPearls` and
`loadAndSpawnParentVehicle`. They may create new objects with the saved UUIDs from
the logout file. The existing registered-body replay retires those aliases, including
hidden sections and pearls in another dimension, and restores the original instances.
The final player ledger and passenger graph refer to those original actors.

FishingHook overrides both additional NBT methods with empty implementations. Its
biting/out-of-water/life/nibble/lure/hook countdowns, angle, open-water flag and state
enum therefore join the reviewed `MatchEntityRuntime` allowlist. Pearl ticketTimer
is also restored as its native remaining countdown. RNG state and the global native
chunk-ticket store are NOT claimed restored by these fields.

A fishing-hook spawn packet embeds its owner's native integer ID, which changes
on player respawn. After link resolution the hook is re-paired to existing native
tracking viewers using remove/add and full current metadata, position and raw motion.
The add packet contains the actual restored player's ID. Future viewers continue to
use the ordinary native tracker. No client-rendering or network-latency proof is claimed.

## Exact native proof

Immutable artifact: `build/verification/skywars2-vanity-player-links-6dde778e.jar`.
SHA256: `6dde778ea75988828c3435c40be5ef282b19b849b83703c2e259e1c225674e56`.
11,123 frozen inputs, including both runner scripts; all 5,291 processed class/resource
entries matched the tested JAR. Source receipts cover 31 native Java entries and
12 legacy Vanity functions. The final build succeeded offline.

The exact artifact passed THIRTEEN gameplay suites in ONE actual mode1 match:
`vanity-actors`, `vanity-player`, `vanity-core`, `vanity-terrain`, `map-lighting`,
`time-stop`, `witch-acquisition`, `witch-lifecycle`, `polymorph`, `effect-layers`,
`projectile-body-collision`, `entity-merging`, `momentum-teleports`.
Runner elapsed 53.11 seconds; normal child exit, exact tracked file snapshots restored,
no suite/cleanup failures or retained locks. Focus mode intentionally has
`qualified:false`; this is component proof, not a full release qualification.

The existing `VanityActorChecks` now additionally exercises two original native pearls
in Overworld/Nether, an ownerless snowball and a fishing hook. Real lethal player damage,
native respawn twice, at least four actual running match ticks, actual disconnect/login,
old-body ledger cleanup, saved unsynchronized fishing counters, current hooked-player
IDs, current-owner hook packets and repeated link replay are asserted. After native
logout, both actual native configuration loader methods deserialize the genuine saved
player file and create vehicle/pearl aliases before replay. Assertions require those
aliases to be replaced by the original objects without extra actors or stale ledgers.
This calls the real loader methods around native login; it does not simulate an entire
asynchronous configuration/network handshake or install an automatic pending-login hook.

The Nether fixture owns a distinct nonpersistent, nonsimulating native loading ticket,
removed in finally. A warm getChunk alone allowed the chunk to unload between restores;
the production resident-destination guard correctly rejected that attempt. It was not
weakened, and restore still does not generate/load missing destinations.

Failed receipts remain archived: `cb10a033` exposed the intermediate respawn body's
stale ledger; `02fc6501` exposed the fixture's missing residency ticket. Intermediate
`654af299` passed the focused native suite. The final `6dde778e` passed all 13 above.
A Polymer missing chunk-packet-context diagnostic occurs before the gameplay fixtures;
the same diagnostic is present in the parent's `ebed6639` native log. This stage does
not claim a completely warning-free server startup or fix that unrelated diagnostic.

All 199 author-owned dirty paths and 31 live loot override files remain byte-identical.
Main JAR stays `d9087f0a07a321a5a9e57cac7f6f58a2d4ca6a86da9829a52623799f0342e087`.
Evidence: `build/verification/vanity-player-links/{baseline,candidate,sources,final-audit}.json`,
`inputs-6dde778e.json`, SHA-named logs/results, numbered builds and the local commit receipt.

## Required follow-up

The full input/writer/lifecycle coordinator, automatic pending offline-body replay,
later-loaded entity regions and persistent storage reconciliation remain required.
Native freeze still permits player input/ticks. Private abilities, encounters,
attribution, AI goals/navigation/sensors/RNG, combat/multipart/camera/container references,
TimeStop's underlying identity/motion/clock state, world clocks/events/metadata and map
jobs/baselines are not completed by this component. Preserve and compose the existing
terrain/core/player/actor owners; do not enable acquisition before the complete rewind.
