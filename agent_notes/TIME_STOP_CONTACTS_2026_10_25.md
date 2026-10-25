# Frozen TNT presentation and contact solids ? 25 October 2026

User point8: frozen primed TNT remains visible and keeps flashing; frozen TNT and
falling blocks retain the legacy physical contact cell. Prepared source only;
build, native execution and READY qualification are recorded by the integrating agent.

Exact legacy inputs (read only):
`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/`:
- `minecraft/tags/entity_type/spawn_barrier_on_time_stop.json`: only TNT and falling blocks.
- `lbc/function/time_stopped_tickentity_data_save.mcfunction`: feet cell aligned xyz,
  barrier replaces `#minecraft:airs` only; both old/new fuse fields become32000,
  zero motion and NoGravity1; other lifetime/AI fields are overwritten because
  legacy entities continue ticking.
- `lbc/function/time_stopped_tickentity_data_save2.mcfunction`,
  `time_stopped_tickentity_data_restore2.mcfunction`: capture and restore native state.
- `lbc/function/timestop_return_motion.mcfunction`: delete barrier on restoration.

Exact26.3 source jar:
`.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-d6f9bb6878/26.3/minecraft-merged-d6f9bb6878-26.3-sources.jar`:
- `net/minecraft/world/entity/item/PrimedTnt.java`: client also decrements the
  synchronized fuse and discards TNT at0. Server-only tick cancellation cannot
  keep the vanilla client TNT alive.
- `net/minecraft/client/renderer/entity/TntRenderer.java`: render fuse is
  `getFuse()-partialTicks+1`; white flashing alternates every5 fuse ticks.
- `net/minecraft/server/level/ServerEntity.java`: initial pairing and dirty
  metadata both construct `ClientboundSetEntityDataPacket`; constructor snapshots
  default/nondefault metadata before subsequent tracking.

Implementation: server gameplay fuse is untouched. A presentation-only long fuse
(32000 minus server tick modulo1000) is substituted into the actual native TNT
pairing/dirty metadata. Refresh every40ticks preserves continuous five-tick
flashing and client survival even over extended stops. Thaw explicitly sends the
real fuse, including default80, without consuming a native fuse tick. This needs
`TimeStopNativeMixin$TntData` and `$TntTracking` registration. Entity texture,
block state, owner and native explosion remain unchanged.

Contact leases belong to LbcTimeStop. Only prior air variants are replaced,
matching the exact legacy mask. All preexisting blocks and block entities remain
untouched, including inventories. Each feet cell has one prior-state snapshot and
a holder count for overlaps. Quiet side-effect-suppressed writes avoid drops,
neighbor and on-place mutation. Final release restores the exact air variant only
while the owned barrier remains; a newer gameplay block/BE wins. Release also
occurs for removed, dead, newly exempt and dimension-transferred entities and
actual service stop. Same-instance dimension travel restores temporary gravity
and velocity; another stopped dimension takes a fresh snapshot at its new position.

Existing authoritative gates retain original velocity, gameplay fuse/lifetimes,
AI state, scheduled delay, same-dimension damage replay, per-dimension scope,
ORAORA-only controlled display exemption, native projectile pause, spell pause,
Greed/technical guards and actual match lifecycle. No legacy lifetime/NoAI rewrites
are needed while native simulation itself is gated. The legacy barrier's blind
cleanup is deliberately tightened to owned, overlapping restoration.

Native fixture integration: `TimeStopContactChecks.run(game)` starts the45tick
suite; call `tick(game)` from GameChecks tick. Register `time-stop-contacts`,
`time-stop-contacts-stop-probe` and `time-stop-contacts-cleanup`. Stop probe is
followed by actual match stop and cleanup in the next stable lobby boundary.
The suite uses actual Cow body/support movement and Arrow block impact against
both payload contact cells, fuse-one TNT across real server ticks, native
ServerEntity pairing and dirty metadata plus stream-codec round trip, exact air
restoration, overlap removal, unchanged existing container/BE, newer container
replacement, original velocity/gravity and actual stop. Native packets and
source-defined flash arithmetic do not qualify real client pixels, real disk
restart/disconnect or peak-capacity behavior.


## Unload and persistence follow-up

Exact Level.getBlockState/setBlock call the loading getChunk path. A removed
holder can outlive its loaded chunk, so release now retains the original
LevelChunk reference and restores directly on that reference when not resident.
The level path is used only when getChunkNow returns that identical chunk.
Lease objects also carry chunk identity so an old holder cannot release a new
instance at identical coordinates. No chunk ticket or forced reload is added.

Entity.saveWithoutId expression hooks serialize the original velocity and
NoGravity flag while leaving the live frozen entity untouched. Native fuse,
owner and other persistent fields retain their native values. This also avoids
freezing temporary flags permanently when normal entity storage saves before
UNLOADED_TO_CHUNK removal; queued end-tick ENTITY_UNLOAD cleanup is too late.

SerializableChunkData.copyOf RETURN operates on its detached section copies,
only in chunks with owned contact leases and only for identical chunk references.
It replaces owned barriers in those copies with the exact original airs.
Snapshots with changes omit heightmaps and invalidate lighting, so native load
rebuilds both from persistent blocks. Live sections/heightmaps/collision never
change during save. A newer gameplay block is retained in both live and saved
state. New mixins: TimeStopNativeMixin$Persistence and $ChunkPersistence.

The native fixture now invokes real Entity.saveWithoutId/load and
SerializableChunkData.copyOf, checks unchanged live fuse/velocity/gravity and
contact block, exact saved air, omitted stale heightmaps/lighting and wrong
chunk-instance guard. Metadata codecs execute with the actual viewer packet
context required by Polymer/Fabric. These bounded fixtures do not simulate actual
disk save/restart or physical chunk unloading; those limits remain explicit.


The exact-air native fixtures retain a temporary solid anchor in the same section
before installing cave/void air: LevelChunk.setBlockState skips air writes when
its section is empty. This fixture condition is asserted before acquiring leases;
the anchor is covered by the original world snapshots and restored after checks.
