# Terrain component for the full Vanity checkpoint

## Author contract and release boundary

(user) Vanity rewinds the WHOLE match to its acquisition checkpoint: map, all
players including dead/offline/newcomer cases, native/private state, positions,
dimensions, motion, effects/layers, attributes, counters and core/map parameters.
Player NBT alone is not an acceptable substitute. Offline captured players keep
their restored state until login; newcomers become spectators.

This stage implements and verifies the reusable **terrain component**, not that
complete ability. `Mode.VANITY.implemented()` remains false. No Vanity acquisition,
death trigger, incomplete rewind, reduced Chaos pool, main installation, restart
or push is enabled. Full original25 Chaos release gate remains CLOSED.
Questions belong in CHAT; the author-owned pending-question file is untouched.

## Implementation

`MatchTerrainCheckpoint` copies native section palettes, including biomes, rather
than allocating a map entry for every air block. Each admitted chunk also owns
independent full block-entity NBT and native block/fluid scheduler images. Duplicate
admission keeps the first image. NBT is copied again on load, so later edits and
repeated restores cannot mutate the saved payload.

The resident-chunk discovery helper uses existing `ArenaChunkAccess`; it never
requests chunk generation. Explicit `include` is the extension point for the
future owner's chunk-load boundary. That boundary is NOT wired by this stage.
Restore validates the admitted resident set before writing; missing chunks fail
instead of synchronously generating terrain from a partially restored world.

Restoration is resumable. Its explicit cell budget counts unchanged examined
cells as well as writes; regular-AIR-only sections skip cheaply. Exact air variants
remain distinct. Only changed states use the existing `MapManager.restoreSnapshot`
owner, retaining native heightmap/lighting/client handling and suppressing drops,
shape and placement side effects. Unsupported fire/lava therefore remain in place.

All captured block entities load even when the block state never changed: inventory
edits do not necessarily produce a block mutation. Destroyed containers are recreated
and future containers disappear without item drops. Same-state NBT changes explicitly
enter the native block-update queue, so sign text reaches existing viewers through
`ChunkHolder`'s normal publisher. There is no second block-entity packet engine.
Biome changes use the native palette fill and existing ChunkMap biome publication.

`TerrainCheckpointTicksAccess` reads native pending SavedTicks without unpacking or
mutating the source queue. Live ScheduledTicks retain long timestamps, priority and
exact sub-tick order instead of passing through int-delay serialization. Pending
entries retain their native negative ordering. Capture/restore use the existing
`LbcTimeStopSchedules` clock conversion. After ALL block writes finish, old/future
queues are cleared and captured delays are rebased to the current level timeline;
the full owner can instead rewind level time before this operation.

`MapLighting.Update` remains the lighting/send-dependency owner. Restore waits for
its asynchronous propagation and final light packets without joining unfinished
futures. Cancellation releases send gates, without declaring partial work complete.

The caller must freeze simulation, quarantine player/other writers, retain chunks
and close cancelled operations. Native tick freeze alone still permits player ticks;
this component does not claim to provide the full match input barrier. Server-thread,
frozen-restore and overlapping-restore checks enforce its local preconditions.

## Native verification

Final immutable artifact:
`build/verification/skywars2-vanity-terrain-3571b419.jar`.
SHA256: `3571b419895ea88e4834c1a27bd143c24ff420a72320f907da0ee1ab15cb37a1`.
11,060 frozen inputs and 5,246 processed class/resource entries matched the exact JAR.
`build/verification/vanity-terrain/sources.json` records 12 native source entries and
12 legacy Vanity files, with archive/path SHA receipts. Legacy player-only loops
are historical evidence, not the new full-rewind specification.

`vanity-terrain` runs on two actual resident chunks in a real playing mode1 match:

1. Copies palettes and queues, then changes chest contents/components without
   changing its state, destroys another chest, and adds a future container.
2. Changes unsupported lava/fire, cave air, an emissive block, biome and sign text.
3. Replaces scheduled work, including a delay above int range and negative sub-order.
4. Checks pending native capture leaves its source unmodified, live restoration is
   rejected before writes, overlapping restores are rejected, and a one-cell scan
   really examines at most one cell.
5. Restores the same immutable image three times, with actual native world ticks
   between the second and third. Checks delays, priority/order and future-work removal.
6. Verifies native light propagation and actual selected viewer light packets, and
   the restored sign payload in native block-entity packets after the native broadcast.

The final exact artifact passed **six gameplay suites in one actual mode1 match**:
`vanity-terrain`, `map-lighting`, `time-stop`, `witch-acquisition`, `witch-lifecycle`,
`polymorph`. Owned runner elapsed 49.93 seconds; normal child shutdown, exact tracked
fixture-file restoration, no cleanup errors or retained locks. The fixture restores
its edited cells, original queues/biomes/provenance, viewer connection/tracking,
force-load ownership and native freeze state. Earlier artifact `9bc05b01` also passed
the initial narrower terrain test; its evidence remains archived separately.

The final audit rechecked all 199 author-owned dirty paths and 31 live loot overrides
byte-for-byte. Main JAR remains `d9087f0a07a321a5a9e57cac7f6f58a2d4ca6a86da9829a52623799f0342e087`.
No resource-pack/template files changed.

Evidence under `build/verification/vanity-terrain`: `baseline.json`, `candidate.json`,
`inputs-3571b419.json`, `sources.json`, `3571b419-armor-sets-gameplay.log`, matching
results JSON, build logs, `final-audit.json` and local commit receipt.

## Remaining work and proof limits

Full Vanity still needs actor/player lifecycle restoration (including replaced,
offline and newly joined players), native/private ability queues and identities,
scoreboard/profiles/core/map parameters, world metadata/events, shared clocks,
checkpoint ownership and actual acquisition/death integration. Later-loaded chunks
need the full owner's admission hook and residency policy. This stage alone does
not snapshot entities, block-entity transient runtime state, native block events,
all persistent world data, settings or private gameplay state.

The native test proves the listed two-chunk component paths and regressions, not
whole-map performance, physical client rendering, input isolation, unloaded-chunk
roundtrips, multi-dimension rewind or full simultaneous multiplayer restoration.
The acquisition gate must stay closed until that complete dependency is implemented
and proved. Next work is the full core/player/private-state checkpoint plus its
native lifecycle/input/timeline coordinator, reusing this terrain component.
