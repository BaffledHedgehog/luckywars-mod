# Map lighting source audit ? 26 October 2026

Read-only production audit. No build, server, install, resource-pack or live override changes. Exact official source: `.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-d6f9bb6878/26.3/minecraft-merged-d6f9bb6878-26.3-sources.jar`. Source references below are archive entries with 1-based lines. Read current root checkpoint and agent_notes/AGENTS before writing this new note.

## Concrete missing synchronization

- `MapManager.java:231-245` finishes immediately when the work queue empties. Clear uses native `world.setBlock` at911; structure paste uses `placeInWorld(..., MAP_WRITE_FLAGS)` at1108. Flags at97 preserve native light updates.
- `MapManager.java:1124-1189` discovers markers/spawns and completes successfully without waiting on lighting. `complete:1422-1437` clears busy, releases loading tickets and calls the consumer.
- `GameController.java:265-283` immediately marks the selection ready and may start a queued game; start teleports players at435, using native teleportTo at868. No custom full chunk/light packet in this path.
- Thus the success callback/ticket release and player arrival can precede queued light propagation. This is a proven missing barrier, not proof that every observed dark chunk is caused by this race.

## Native write and correct barrier

- `net/minecraft/world/level/chunk/LevelChunk.java:288-300`: section emptiness queues updateSectionStatus; different light properties synchronously update skyLightSources then queue checkBlock. No flag-based light suppression here.
- `net/minecraft/server/level/ThreadedLevelLightEngine.java:59-66,118-128`: checkBlock is a PRE_UPDATE submitted to a chunk-prioritized dispatcher; ordinary writes do not await its execution.
- Same class185-207: runUpdate processes up to1000 tasks, runs PRE_UPDATE, invokes `super.runLightUpdates()`, then runs POST_UPDATE.
- `net/minecraft/world/level/lighting/LevelLightEngine.java:48-58` runs both block and sky engines. `LightEngine.java:137-152` checks nodes, drains decrease/increase propagation, marks inconsistencies and swaps the published section map. Consequently POST_UPDATE is after actual propagation/publication, not merely task submission.
- Public native API `ThreadedLevelLightEngine.waitForPendingTasks(int chunkX,int chunkZ):210-212` enqueues an empty POST_UPDATE through that same dispatcher. Request it after final writes for EACH touched/loaded chunk, await all asynchronously/poll isDone from server tick, retain tickets and busy state until completion, surface exceptional futures. Never block the server tick on an unfinished join. A single arbitrary chunk barrier is insufficient because priority ordering spans chunks.
- Public `ChunkMap.waitForLightBeforeSending(centerChunk,chunkRadius):1268-1275` adds the same barriers as send dependencies over radius+1. `ChunkHolder.java:90-103` gates getChunkToSend on sendSync; PlayerChunkSender.java86-104 uses this getter. This is the native send-gating model; a post-placement completion barrier alone does not prevent earlier in-progress full sends.

## Correct client resynchronization

- `PlayerChunkSender.java:76-77` sends `ClientboundLevelChunkWithLightPacket(chunk,level.getLightEngine(),null,null)`; the packet snapshots current published light, so prematurely sampled light remains possible without send gating.
- `ChunkHolder.java:142-167` ignores light broadcast marking when no ticking chunk. More importantly174-187 sends native light delta packets only to `playerProvider.getPlayers(pos,true)` (tracked BORDER viewers), then clears filters.
- `ChunkMap.java:226-243,1113-1123` proves true means tracked-border only. Normal interior viewers rely on client block-change lighting and do not receive those authoritative light deltas. Explicit bulk-write final light refresh is therefore meaningful even though native block flags retain UPDATE_CLIENTS.
- After all barriers complete, construct `new ClientboundLightUpdatePacket(pos, lightEngine, null, null)` and send to `chunkMap.getPlayers(pos,false)` for affected chunks (include one-chunk propagation margin). `ClientboundLightUpdatePacketData.java:36-72` treats null filters as all sky/block sections, copies published arrays and carries empty-section masks. `getPlayers(...,false)` returns all actually tracked viewers, excluding pending chunks; pending viewers receive fresh light via their later initial packet. Avoid sending light for chunks the client does not have.
- A full chunk resend is unnecessary for a light-only refresh; the native light packet preserves block entities and chunk lifecycle. Do not run `ThreadedLevelLightEngine.runLightUpdates()` on the server thread: its override54-55 deliberately throws.

## Implementation qualification boundary

This note identifies exact APIs and the missing ordering/resync path. It does not claim live reproduction, physical client pixel validation, or that a code fix has passed gameplay checks. Existing protected author files, deletions and dirty production changes were not touched.

## Read-only implementation review (same day)

Reviewed new MapLighting.Update, MapManager integration and ArenaBlockMixin without edits to production/tests or invoking build/server. No blocking synchronization defect found in the reviewed map-operation path.

- Global completion send gate is installed before Level writes, and resident-margin native POST barriers do not depend on that gate. Exact source ServerChunkCache.java462-468 queues section-light notifications to the main thread; ChunkHolder.sectionLightChanged142-167 uses getTickingChunk, not getChunkToSend. ChunkMap.prepareTickingChunk668-682 defers onChunkReadyToSend on sendSync but still returns the LevelChunk immediately, so the load/ticking future and light engine can progress while initial sends are gated. No dependency cycle found.
- Late writes invalidate propagation and clear resend queue before the per-chunk dedupe return, including repeated writes to an already known chunk. A new set of barriers therefore covers them. Existing published packets may be temporarily stale, but the rebuilt queue sends a later authoritative snapshot before completion.
- Successful map operation retains busy and loading tickets through all propagation and bounded final resends. Completion/cancellation releases the send gate; MapManager then releases loading tickets. No join on incomplete futures found.
- Resend queue snapshots only already tracked chunks; tracking remains gated for initial/pending sends. Viewer membership is refreshed when sending; viewers who leave do not receive unloaded-chunk light packets. Full packet construction occurs after the barrier, on the server thread.
- removeLobbyOverlay and out-of-operation maintainLobbyOverlay create their own updates and enqueue eventual full refresh. Overlapping overlay jobs add completion dependencies without waiting on each other. They process sequentially; later updates supply the final correction. Their ordinary terrain writes are deliberately not delayed until the lighting refresh.
- Limited edge caveat: gate() tracks holder identity only when an affected chunk is first added or when barriers are rebuilt. If an affected margin holder unloads and is replaced during the same operation, its replacement is not gated until finishWrites. The main map loader retains tickets for its prepared neighboring chunks, so this does not establish a live map-path failure; standalone overlay jobs rely on normal lobby residency. No new chunk generation is introduced by the repair.

This review is source reasoning, not client-pixel or runtime test qualification. Parent runs the native focused verification separately.
