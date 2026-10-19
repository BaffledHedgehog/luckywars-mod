# Projectile lifetime: source and map-bound audit

Read-only audit for the future ordinary `stterra` port. No code, build, or server changes. Old datapack path: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc`; Minecraft API checked in local 26.3 merged jar.

## Legacy lifetimes and ranges

- Ordinary staff Terra (`workingitems/sauvojen_staffs/terra_preloop.mcfunction`) summons a marker with `data.lifetime:200`, tags `raycastterra/player/slowraycast`, then copies caster rotation/ID/team and activates it. The live dispatcher is `raycastselector_player.mcfunction` → `lbcr:raycast_terra`. That function increments `Distance`, tests mobs at each sample, and advances by `^ ^ ^0.3` while Distance is 1..6. At Distance 7, `raycast_end_with_tp` resets Distance to 0 and teleports to the same position; the next selector tick begins another batch. Thus 1..6 gives about 1.8 blocks of progress per tick, not a total range cap. `raycastselector` increments marker Lifetime once per tick and kills at the `data.lifetime` value, so 200 is a 200-tick TTL. The requested ordinary Terra behavior explicitly has no fixed TTL or total distance cap.
- Upgraded quick Terra (`terra_preloop_upgraded.mcfunction`) uses `lifetime:20`, `fast_terra/rare`; `raycastselector_rare.mcfunction` dispatches to `raycast_terra_fast`, which reaches `Distance=26` after 25 increments of 0.3, or about 7.5 blocks per tick before its end/reset routine. Its `lifetime:20` is a 20-tick TTL, not a 26-step total travel limit. This is a separate upgraded staff behavior, not ordinary Terra.
- Terra Sword charge (`true_terra_sword_hit.mcfunction`) creates a `terra_ch` marker with `lifetime:50`, `slowraycast/rare`. It has a finite source marker lifetime; do not generalize ordinary Terra's no-fixed-TTL requirement to these charges.
- Razor Pine arrows (`workingitems/razorpine.mcfunction` and `razorpine2.mcfunction`) are actual native arrows. Source initializes Owner, rotation, and motion (the latter from a positioned marker plus random spread); these functions set no custom `Lifetime` or `Distance` cap. Native arrow collision/in-ground persistence owns their lifetime. The upgraded wand path `items/wands/sauvojen_wand/flying_wand_tick_terra_upgraded.mcfunction` periodically invokes several Terra-related marker abilities, including ordinary/upgraded Terra and `true_terra_sword_hit`; its own `cold` cadence is not a projectile lifetime.
- Existing acid remains intentionally finite: current `LbcClouds` tracks `ACID_AGE` to 300, preserving that separate contract. This audit does not recommend changing acid or Terra Sword charge lifetimes.

## Map bounds and chunk readiness (context, not projectile culling)

`MapManager.boundsFor()` derives cuboids from each selected placement origin plus the loaded structure-template size, then compacts touching/overlapping boxes (`MapManager.java:762-805`). On successful placement, those boxes become the private `mapBounds`; lobby bounds are stored separately (`950-956`). Thus a conservative selected-map placement envelope already exists. It is not exposed as a public containment helper. `activeBoxes` is a persistent recovery journal combining lobby/map and recorded edits; it can include old/out-of-map changes and should not be treated as the current play boundary.

`chunksReady()` adds load tickets and waits for each placement slice plus one neighboring block's chunks (`232-247`). `finishPlacement()` completes map readiness, and `complete()` releases those temporary chunk tickets (`MapManager.java:1104-1117`). Map-ready therefore means placement finished, not that every chunk in `mapBounds` stays loaded or entity-ticking. `forceMarkerChunks()` force-loads only tagged `SWRG` map-marker chunks (`968-974`), not projectile routes.

Native 26.3 distinction:

- `LevelReader.hasChunkAt(BlockPos)` delegates to `hasChunk(sectionX, sectionZ)`; on `ServerChunkCache`, `hasChunk` tests a visible chunk at FULL status. It is a non-loading loaded-chunk guard. Avoid replacing it with `getChunkAt` in projectile cleanup, which can request/load a chunk.
- `ServerLevel.isPositionEntityTicking(BlockPos)` checks both `PersistentEntitySectionManager.canPositionTick` and `DistanceManager.inEntityTickingRange`. This is stricter than “chunk is loaded”: loaded chunks outside entity-ticking range do not run ordinary entity ticks. Native `ServerLevel.isLoaded(BlockPos)` is another existing guard, distinct from entity-ticking readiness.

## Existing cleanup and culling policy

`LbcItems` stamps managed entities with a fresh `skywars2.items.<uuid>` epoch and discards stale-epoch entities on load (`LbcItems.java:432-434, 465-472, 568-577`); `stop()` stops projectile managers and rotates that epoch (`771-794`). `MapManager.clearTransientEntities()` removes arena entities before fresh map/lobby placement (`525-527`). These are match/reset cleanup paths, not a per-tick outside-map test.

Although current selected-map placement cuboids are available internally as private `mapBounds`, they must not be used to cull Terra projectiles: supported play includes the Nether/“закартье” and very large coordinates. The user clarified the intended cull rule:

> (user) «при факте удаления от игроков и мобов на достаточную дистанцию разумно удалять снаряд, никто не будет пытаться его догнать, он довольно быстрый»

> (user) «закартье - тоже поддерживаемая игрой зона если что, в ней все тоже должно работать, если игрок решат там подраться (даже на огромных координатах)».

So ordinary Terra should have no fixed lifetime or total route distance; culling is based on sufficient separation from players/mobs per the accepted policy, not arena bounds or a fixed coordinate/range. Culling must ignore already-passed targets behind the projectile after it has passed their possible hit area, but retain it for possible targets still ahead; even a far-ahead target beyond 300 blocks keeps the projectile alive until it is passed. An empty target query at the current point alone does not prove there can be no later hit. The old ±500 value remains available through optional `noBorders`; that is a separate hit/damage-range setting, not the projectile lifetime or route-distance limit. For chunks, keep non-loading loaded/ticking guards and epoch/unload cleanup; do not assume a loaded chunk is entity-ticking.

## Source paths

- Legacy: `workingitems/sauvojen_staffs/{terra_preloop,terra_preloop_upgraded,true_terra_sword_hit}.mcfunction`; `raycastselector.mcfunction`, `raycastselector_player.mcfunction`, `raycastselector_rare.mcfunction`; `raycast_end_with_tp.mcfunction`; `lbcr/{raycast_terra,raycast_terra_fast}.mcfunction`; `workingitems/{razorpine,razorpine2}.mcfunction`; `items/wands/sauvojen_wand/flying_wand_tick_terra_upgraded.mcfunction`.
- Current project: `src/main/java/me/skywars2/game/MapManager.java` (`chunksReady`, `boundsFor`, `mapBounds`, `finishPlacement`, `releaseChunkTickets`); `LbcItems.java` (epoch/entity load cleanup); `LbcClouds.java` (finite acid attachment age).
- Native 26.3 local merged jar: `LevelReader.hasChunkAt`, `ServerChunkCache.hasChunk`, `ServerLevel.isLoaded`, `ServerLevel.isPositionEntityTicking`.