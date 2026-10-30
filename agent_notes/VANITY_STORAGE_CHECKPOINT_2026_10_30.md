# Processing/storage menus and native block events for Vanity

## Contract and release boundary

The [full author rewind contract](VANITY_ACTOR_CHECKPOINT_2026_10_30.md) remains
mandatory. This extends the [native views component](VANITY_VIEWS_CHECKPOINT_2026_10_30.md)
and its existing terrain/body owners. It is NOT the complete rewind transaction.
Vanity acquisition remains disabled and the full original25 Chaos gate stays CLOSED.
No main installation, restart, push, pool reduction, RP/template or pending-question
changes. Questions belong in CHAT; preserve the author's answers and live overrides.

## Seven additional storage/processing forms

MatchPlayerViews now supports native shulker boxes, physical ender chests, furnaces,
blast furnaces, smokers, brewing stands and crafters. It still captures exact native
menu classes, cursor/components/title/drag, current block bindings and current player
inventory slots. Unsupported workstation/plugin/entity-backed shapes still require
dedicated owners and fail explicitly. The earlier camera completion phase is retained.

An ender session records the player's UUID and active chest level/position. At replay,
the binding resolves the CURRENT player's own PlayerEnderChestContainer, with its
contents restored exclusively by MatchPlayerCheckpoint. It validates the restored
physical ender BE first, closes future sessions, and then sets the native activeChest
pointer before native menu construction. It does not retain the old player's container,
copy ender contents into a world chest, or attach two owners to one shared inventory.
Two players may use one restored ender block while retaining independent contents.

Processing menus use the restored BE's OWN ContainerData object. Capture rejects
furnace variants with the wrong block class or detached data, and likewise rejects
detached brewing/crafter data. Replay reconstructs the appropriate native constructor
with current inventory, current BE and its native data access. Fuel/cooking/brewing
values, crafter disabled/triggered values and inventory contents remain owned by the
terrain owner's native BE NBT image. No copied SimpleContainerData array replaces
live progress. Native recipe lookup, result-slot restrictions and crafter preview
reconstruction are reused; opening a restored window does not consume crafting inputs
or invoke another loot-table unpack/claim.

## Idempotent native sessions

A saved session whose exact native kind/type/rows, binding and title already match,
with all 36 inventory slots bound to the current player, is retained. Its cursor/drag
values and content are refreshed without another native close/open. Removed backing
objects, changed titles, retired player inventories or detached data fail this reuse
check and reconstruct a fresh native session. This matters for shulkers: closing and
reopening an already-correct window can queue an older close event after an existing
deduplicated open event. Replay now leaves the window ID/object and viewer count stable
and emits no extra block events when only reconciling the same correct session.

## Native queued/deferred block events

MatchBlockEvents composes with MatchTerrainCheckpoint. The first included chunk of a
level captures independent copies of its ordered blockEvents and blockEventsToReschedule
values, including immutable positions, native block identities and both integer
parameters. The image tracks the terrain owner's included chunk set. After ALL block
and BE writes, alongside scheduled block/fluid queue restoration, it removes future
events for included chunks and republishes their captured events in captured order.
The actual native set/list instances remain in place; replay calls no block handler.

Events outside the owned chunks keep their current relative order and values. Included
events retain their saved relative order across included chunks. A partial restore
places those saved entries after retained unowned entries; it cannot promise the
original interleaving with an independently changed outside timeline. If the owner
covers every queued event, the whole native order is exact. Future full world/chunk-load
coordination must supply that coverage. Native runBlockEvents resumes normally later.
This removes stale close/count events from the future before reopening restored menus.

Native viewer reconstruction does not capture every private BE animation/cache field.
Shulker/ender lid progress, arbitrary BE runtime and world game-event/listener/private
service stores still require their full owners before release. The block-event queue
is one explicit native timeline component, not all world metadata/events.

## Exact native proof

Immutable artifact: `build/verification/skywars2-vanity-storage-8ac3de54.jar`.
SHA256: `8ac3de54f6b7f2efac588b1fd0c63d030580269a997510649de9016f703d2b91`.
11,172 frozen inputs, including both runner scripts; all 5,332 processed class/resource
entries matched the exact JAR. Receipts cover 84 native entries, 12 legacy Vanity
functions and 14 existing application damage/view owner files. Offline build passed.

The new storage scenario and existing views/terrain scenarios passed together first.
The SAME final artifact then passed SEVENTEEN suites in ONE actual mode1 match:
`vanity-storage`, `vanity-views`, `vanity-actors`, `vanity-player`, `vanity-core`,
`vanity-terrain`, `map-lighting`, `time-stop`, `witch-acquisition`, `witch-lifecycle`,
`polymorph`, `effect-layers`, `projectile-body-collision`, `entity-merging`,
`momentum-teleports`, `glass-armor`, `indirect-kills`. Runner elapsed 67.94 seconds.
Normal child exit, exact tracked snapshots restored, no suite/cleanup failures or
retained locks. Focus mode intentionally leaves qualified:false; the explicit completed
suite list defines this proof, not the runner's generic default measurementLimit.

VanityStorageChecks opens real native menus across three rounds, destroys all seven
backing BEs, applies the existing terrain/body/actor phases, and requires native player
respawn and CURRENT inventory/BE/data bindings. It checks distinct personal/storage
cursors and two owners' private ender contents; two viewers at one ender block; actual
offline pending/reconnect ender replay; exact fuel/cooking/brewing fields; restored
native shulker slot restrictions; native crafter disabled/powered data, its published
data packet and four-plank recipe preview without consuming logs. Repeated link replay
must retain the same correct menu and send no extra screen-open or block event.

At least four real match/world ticks verify live furnace data continues updating and
the shulker's native openCount remains correct after queued events execute. Another
restore rewinds the progressed state. Assertions compare ordered owned queued/deferred
events, reject the future count37 event, and retain an explicitly unowned future event.
The previous real dimension/respawn/camera/hidden-view/drag tests also pass unchanged.
No physical-client rendering, arbitrary-map, every native BE animation phase or peak-TPS
claim is made. Existing startup warnings remain in the logs.

All 199 author dirty paths and all 31 live override files remain byte-identical.
Main JAR remains `d9087f0a07a321a5a9e57cac7f6f58a2d4ca6a86da9829a52623799f0342e087`.
Evidence: `build/verification/vanity-storage/{baseline,candidate,sources,final-audit}.json`,
`inputs-8ac3de54.json`, focused/final SHA-named logs/results, numbered build logs and
bounded local commit receipt. Prior root documentation remains intact.

## Required follow-up

Complete input/writer/lifecycle coordination and automatic offline replay remain
unfinished. Remaining workstation/entity containers, private BE/multipart state,
AI/navigation/sensors/RNG, ability/attribution/FPV controllers, TimeStop internals,
world clocks/game events/listeners/metadata, entity-region/pending IO and map jobs/
baselines still need coherent owners and final persistence. Retain the existing
terrain/core/body/actor/combat/view owners. These additions do not enable full Vanity.
