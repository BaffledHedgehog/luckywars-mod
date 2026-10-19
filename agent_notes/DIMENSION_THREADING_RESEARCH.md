# Dimension-level parallel ticking research (Minecraft 26.3)

## Conclusion

Per-dimension parallel ticking is a real design used by DimThread, and it can run several `ServerLevel` ticks at once with a barrier so the overall step waits for the slowest dimension. But none of the examined projects offers a current 26.3 build suitable for this server: DimensionalThreading/DimThread's latest GitHub release is for Minecraft 1.19, and MCMTFabric's latest release is explicitly experimental for 1.21.1. Porting/adapting dimension tick threading here is a major engine-level thread-safety project, not a small executor change.

For this plugin, an arena level is only one dimension in the same process, and the match controller is a singleton. Its `GameController.tick()` mutates shared match state, player state, map state, items, and game stage in one sequence. `SkyWars2` invokes it from `END_SERVER_TICK`, while entity load/unload, chunk lifecycle, damage/death, and player lifecycle callbacks directly call the same controller/services. If vanilla level ticks are moved to worker threads, those callbacks and player teleports can touch the singleton and shared player/global state concurrently with other dimension work. A single arena also means the arena's own ticking remains the dominant serial unit; empty or cheap Overworld/Nether/End do not make its CPU work parallel.

## Projects and what they change

### DimensionalThreading / DimThread

The project's README describes one independent thread per dimension, configurable through `dimthread_active` and `dimthread_thread_count`, and says all dimensions synchronize so overall MSPT follows the slowest dimension. That means it parallelizes the per-dimension world-tick work, then joins at a tick barrier. It needs more available threads than ticking dimensions to avoid making things slower. [Project README](https://github.com/WearBlackAllDay/DimensionalThreading) and [release history](https://github.com/WearBlackAllDay/DimensionalThreading/releases) show the latest release is 1.2.7 for Minecraft 1.19; no 26.3 release/build is published.

This approach changes a vanilla invariant: work inside `ServerLevel`/`ServerChunkCache` which ordinarily runs on the one server thread may execute on per-dimension workers. It relies on dimensions being sufficiently independent, and it must preserve barrier ordering where worlds share tick-global state. Existing project issues document the exact hazards: one crash is caused by shared `LegacyRandomSource` access from multiple dimension workers ([issue #74](https://github.com/WearBlackAllDay/DimensionalThreading/issues/74)); another mod's chunk event explicitly rejects callbacks off the server thread ([issue #40](https://github.com/WearBlackAllDay/DimensionalThreading/issues/40)); AE2 reports server-thread-only state accessed from its end-world tick while DimThread runs world ticks on workers ([issue #87](https://github.com/WearBlackAllDay/DimensionalThreading/issues/87)). The project's vanilla-parity goal is useful intent, not a guarantee that arbitrary mods are thread-safe.

### MCMTFabric

The Fabric port is based on JMT-MCMT. Its current GitHub release list marks its 1.21 release experimental and tags it for 1.21.1, not 26.3 ([releases](https://github.com/himekifee/MCMTFabric/releases), [repo README](https://github.com/himekifee/MCMTFabric)). Earlier MCMT releases explicitly dispatched world, entity, tile-entity and environment ticks to a thread pool. The 1.19.1 release notes say it separated environment, entity, and block-entity work into phases after interactions such as piston-head/block-entity updates and falling sand caused ordering bugs; those notes also call block-entity multithreading unreliable ([release page](https://github.com/himekifee/MCMTFabric/releases)). This is broader and more invasive than merely assigning each dimension a worker. No 26.3 branch/release was found in the official project repository.

### Vanilla 26.3 context

This workspace targets Minecraft 26.3 (`gradle.properties`). The observed server code keeps the game loop and end-server callbacks on the server thread today. A threading mod would have to change that execution contract and preserve ordering with async chunk IO/generation, global tasks, network/player lifecycle, and cross-dimension entity transfers. A project released for 1.21.1 or 1.19 cannot be assumed to port across the 26.3 world/chunk refactors just because its concept is clear.

## Fit to this project

- `GameController.tick()` (`GameController.java:426`) reads and writes shared stage, participants, counters, config, maps, items and players. It also executes real damage calls and can teleport participants.
- `SkyWars2.game` is a process-wide singleton; `END_SERVER_TICK` calls `game.tick()` and `GameChecks.tick(game)`. Entity load/unload, chunk load/unload, player join/leave/respawn and damage/death callbacks all access the same singleton and its item services (`SkyWars2.java:37-38, 57-120`).
- Thus ticking the arena on a worker while Overworld/Nether/End or the server callback path is active creates shared mutable-state races. Synchronized waits around `tick()` alone do not make callbacks, shared RNG, player teleport, or plugin event listeners thread-safe.
- Cross-dimension teleports are especially important: a player cannot safely be owned by two concurrent world ticks during transfer, and an entity lifecycle callback in one dimension can mutate match state while the destination world is being ticked.

## Narrow path and effort estimate

Do not wrap `MinecraftServer.tickChildren`, `ServerLevel.tick`, `GameController.tick`, damage listeners, or Fabric lifecycle callbacks in a generic `Executor`. Do not treat synchronized/volatile fields as sufficient world isolation.

The narrow scope worth pursuing is pure computation whose inputs can be captured as immutable data on the server thread and whose outputs can be applied in a deterministic, ordered server-thread phase. For the current explosion hotspot, first keep optimizing native synchronous work (for example, block-access caches, precomputed immutable offsets/rays where semantics allow, and fewer repeated queries). If later profiling justifies worker use, a worker may compute only over a detached immutable snapshot; all world reads, RNG decisions that affect gameplay, damage, block mutations, item drops, entity lifecycle and callbacks remain on the server thread. That is a bespoke algorithm split, not full dimension tick parallelism.

Effort rating: high/very high for dimension-level threading in this mod stack: port a threading architecture to 26.3, audit all reachable vanilla and mod callbacks for thread affinity, establish ownership/transfer rules for entities and globals, preserve deterministic phase order/barriers, and run gameplay regression/performance tests across both ordinary and cross-dimension cases. For one arena and a singleton match, likely benefit is low unless Overworld/Nether/End also carry substantial independent tick load; it cannot make the one hot arena dimension's serial tick itself multi-core.

## Follow-up: official DimThread Reforked listing

The author-published platform pages identify **Dimensional Threading Reforked** (`dimthreads`) as an unofficial continuation of DimensionalThreading-Reforged / WearBlackAllDay DimThread. Its stated behavior is still per-dimension independent worker threads with a synchronization barrier; CurseForge notes it also contains WorldThreaded patches and advertises maintained Fabric/Forge/NeoForge builds through 1.21.1, with Fabric 1.20.1 also listed. Modrinth compatibility currently lists 1.21–1.21.1, 1.20.1, 1.19.2, 1.18.2 and 1.16.5. The current listing/release metadata shows no Minecraft 26.3 artifact.

Primary listings: [author's Modrinth page](https://modrinth.com/mod/dimthreads) and [author's CurseForge page](https://www.curseforge.com/minecraft/mc-mods/dimthreads). This refork is more current than the original upstream 1.19 release but still does not remove the 26.3 compatibility gap or the thread-safety concerns above.
