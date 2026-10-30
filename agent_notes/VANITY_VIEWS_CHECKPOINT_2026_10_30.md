# Native cameras and block-storage sessions for Vanity

## Contract and release boundary

The [full author rewind contract](VANITY_ACTOR_CHECKPOINT_2026_10_30.md) remains
mandatory. This extends the existing terrain/core/body/actor/combat owners with
native player views. It is NOT the complete rollback transaction. Vanity acquisition
remains disabled; the full original25 Chaos release gate stays CLOSED. No main
installation, restart, push, pool reduction, RP/template or pending-question edits.
Questions belong in CHAT. Preserve the author's answers and live loot overrides.

## Supported logical sessions

`MatchPlayerViews` records each connected player's native camera UUID, supported
storage binding, independent cursor stack/components, screen title, and quick-craft
type/status/selected slot indices. Native title capture calls the provider once.
Titles round-trip through the registry-aware Component codec: `Component.copy()`
alone shares mutable sibling nodes. Every restore builds another independent title.

Supported exact menu classes are ChestMenu, HopperMenu and DispenserMenu, backed
by native chest/trapped-chest/barrel/hopper/dispenser/dropper block entities. Native
two-block CompoundContainer bindings preserve their ordering. Bindings retain the
level, immutable position, native BE type/class and size, not a removed BE instance.
Nested compounds, SimpleContainer/plugin backings, workstations, ender/shulker and
entity-backed storage require dedicated adapters and fail explicitly before replay.

Storage contents remain owned by MatchTerrainCheckpoint's native BE image. Personal
inventory, crafting and the PERSONAL menu's cursor remain owned by
MatchPlayerCheckpoint. The active storage cursor belongs only to the view image.
The body component now permits the explicitly supported storage shapes at capture.
It never treats the storage cursor as a second personal cursor.

`MatchActorCheckpoint.prepareViewsFrozen()` must precede terrain replay. It validates
ALL current future menu shapes first, discards future storage cursors and closes the
native sessions, balancing native viewer counters. It also detaches future native
cameras, including newcomers. Closing a menu normally returns/drops its cursor;
clearing it first prevents future items being retained. Native storage close/open
can emit sounds, block events and scheduled rechecks; their full timeline still
belongs to the future complete coordinator. Actor-body replay repeats this safe
preparation as a fallback; callers must still use the earlier pre-terrain phase.

After terrain, core/player bodies and actor/combat relationships are restored, the
view phase resolves all current backing BEs before opening any saved window. Native
openMenu builds a fresh session with the CURRENT player's Inventory and native
synchronizer. Cursor and drag state then bind new slot objects, with full native
content publication. Replay does not reuse old menu/player/BE objects or duplicate
the terrain inventory owner. Technical menu IDs/state counters are fresh native
transport sessions; queued client input still requires the full transaction barrier.

## Camera publication and pending identities

Native ServerPlayer.setCamera also teleports the BODY to its camera. The view owner
instead publishes only the native camera link, chunk-source move, camera packet and
connection position baseline. It reuses FullInvisibility.cameraChanging BEFORE the
link/packet, retaining the existing viewer exception for fully invisible players.
Current position, rotation, raw velocity and known client movement remain intact.

UUID resolution uses actual current PlayerList bodies and the actor owner's original
registered instances. A missing/offline/wrong-level camera falls back to self and
reports its UUID as pending. A live camera also waits for native isChunkTracked AND
actual entity tracking before attachment. `finishViewsFrozen()` retries ONLY cameras
after native chunk/entity publication, without reopening menus or restoring bodies.
Same-dimension client respawn keeps world entities; a dimension change creates a new
client world. The final proof exercises the latter and requires a target AddEntity
AFTER that respawn and BEFORE the camera attachment. No duplicate manual spawn
pipeline, native seenBy mutation, extra entity registration or artificial world tick
is introduced. The full owner must distinguish publication waits from offline replay.

This captures native getCamera state. FPV drones send camera packets through their
own private controller/session owner; their native field can remain self. Those
private controls, tickets and future queued packets are NOT restored by this component.

## Exact native proof

Immutable artifact: `build/verification/skywars2-vanity-views-cd03192e.jar`.
SHA256: `cd03192e5d38aa0e9e7492ee33ed70cac0fb87e6aa4520b361000e370502f3f5`.
11,155 frozen inputs, including both runner scripts; all 5,318 processed class/resource
entries matched the exact JAR. Receipts cover 62 native entries, 12 legacy Vanity
functions, 11 existing damage-source owners and 3 existing camera/view owners.
The offline build and isolated native view scenario passed.

The SAME final artifact then passed SIXTEEN suites in ONE actual mode1 match:
`vanity-views`, `vanity-actors`, `vanity-player`, `vanity-core`, `vanity-terrain`,
`map-lighting`, `time-stop`, `witch-acquisition`, `witch-lifecycle`, `polymorph`,
`effect-layers`, `projectile-body-collision`, `entity-merging`, `momentum-teleports`,
`glass-armor`, `indirect-kills`. Runner elapsed 67.64 seconds. Normal child exit,
exact tracked file snapshots restored, no suite/cleanup failures or retained locks.
Focus mode intentionally leaves `qualified:false`; its explicit completed suite list
defines the proof, not the generic runner measurementLimit for its default suite set.

The new fixture uses native double-chest/hopper/dropper menus and real quick-craft
clicks, independent personal/storage cursors, a mutable nested source title, genuinely
destroyed/recreated BEs through the existing terrain owner, actual Nether travel,
death/respawn twice, at least four real match ticks, logout/login, and explicit pending
camera replay. It checks current inventory/BE bindings, exact cursor components,
balanced viewer counts and repeated replay, full-invisibility camera exceptions,
initial and deferred publication preserving movement, packet ordering, and rejected
unsupported menus before mutation. Restored drag performs real native distribution.

Earlier attempts exposed fixture assumptions: teleport(resetCamera=true) erased the
prepared camera; same-dimension respawn did not require another target spawn; native
dimension immunity and memory-bot chunk flow required real protocol acknowledgements.
The final harness reads only the count of genuinely emitted outstanding chunk batches
and acknowledges each through the native packet handler, including borrowed bots.
It does not rewrite counters or bypass server tracking. Extreme body motion is checked
before new fixture input; the idle fixture then stays nearby during publication waits.
Numbered build logs and SHA-named failed/final/isolated logs retain these findings.

All 199 author dirty paths and all 31 live override files remain byte-identical.
Main JAR remains `d9087f0a07a321a5a9e57cac7f6f58a2d4ca6a86da9829a52623799f0342e087`.
Evidence: `build/verification/vanity-views/{baseline,candidate,sources,final-audit}.json`,
`inputs-cd03192e.json`, SHA-named logs/results and bounded local commit receipt.
Existing startup warnings remain recorded; no physical-client rendering, arbitrary-map
or peak-TPS claim is made.

## Required follow-up

The complete input/writer/lifecycle transaction and automatic offline replay remain
unfinished. Native freeze still permits player input/ticks. Private ability/controller/
attribution stores, AI/navigation/sensors/RNG, multipart actors, remaining containers,
TimeStop's underlying state, world clocks/events/metadata, pending/entity-region IO,
map jobs/baselines and final persistence still need their own coherent checkpoint
owners. Native cameras/storage sessions are one component of that work, not full Vanity.
