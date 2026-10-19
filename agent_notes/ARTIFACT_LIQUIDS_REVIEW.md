# Type 1:3 native API and historical cube review

The geometric findings below describe the earlier cube implementation.
The user subsequently requested spherical liquid hardening. The current
row-interval sphere implementation and its verification are documented in
[ARTIFACT_LIQUIDS_PORT.md](ARTIFACT_LIQUIDS_PORT.md) and
[SERVER_TESTS.md](../SERVER_TESTS.md). Native API and frosted-ice parity
findings remain applicable.

Read-only review of `src/main/java/me/skywars2/game/LbcMagic.java` and native
Minecraft 26.3 sources. No build or live-server checks were run.

## Findings

- `findNewLiquids` uses the exact inclusive cube `±12`. On activation it scans
  the new cube; after movement it subtracts the old cube using six disjoint
  slabs. The `abs(delta) > 24` fallback handles non-overlap; at exactly 24 the
  cubes still share a one-block plane and the slab arithmetic preserves that
  intersection. (Current `LbcMagic.java`, `findNewLiquids`.)
- `findLiquids` clamps to inclusive `LevelHeightAccessor.getMinY()/getMaxY()`;
  26.3 defines max Y as `minY + height - 1`, so section iteration through
  `y1 >> 4` is in range. It skips unavailable chunks with
  `ServerChunkCache.getChunkNow`, then uses `LevelChunkSection.hasOnlyAir()` and
  `maybeHas(IS_LIQUID)` before visiting only the intersecting cube cells.
  `ChunkPos.getX(long)` / `getZ(long)`, `getSectionIndexFromSectionY`, and
  `hasOnlyAir` are present in 26.3 sources. `IS_LIQUID` is derived from the
  three exact legacy replacements (lava, water, powder snow); it does not widen
  to the `minecraft:fluids` tag.
- Chunk-load work is queued by chunk key only for chunks currently intersecting
  an active liquid aura. `tickLiquids` retains keys until `getChunkNow` sees a
  FULL chunk; unload removes the key, and a later load can enqueue it again.
  The queue is bounded by indexed aura chunks, though one pulse scans every
  ready queued key and every overlapping aura for that key without a per-tick
  budget. This is a possible burst cost if many covered chunks become visible
  together.
- `clear` removes a holder's aura and reindexes its chunks; when the last aura
  is removed it also clears queued positions/chunks. `stop` clears all liquid
  indexes and pending updates. Unload calls `clear` for players. Stale changed
  positions left while another aura is active are rechecked against
  `liquidCovered` before a write, so removed aura coverage alone cannot convert
  them.
- The current conversion flags
  `Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS` equal 258
  in 26.3 (`2 | 256`), which matches non-strict `/setblock` and `/fill`; it is
  not the `UPDATE_SKIP_ON_PLACE` 512 flag. The code stores old states and batches
  `updateNeighboursOnBlockSet` after each source-state pass, matching the three
  source fills' ordered passes. The replacement states here are default solid
  states, so the direct `Level.setBlock` call does not need `BlockInput`'s
  neighbor-shape preparation for these particular outputs.
- The shared `intercept` path still distinguishes the arrow and unstable
  predicates; `IS_FALLING` is a separate `FallingBlockEntity` predicate. Holder
  selection applies the special `time_stopped` veto only to arrows. Explosion
  handling remains selected only by `NO_EXPLOSIONS`; liquid handling only by
  `NO_LIQUIDS`. Liquid auras are refreshed and beacon-suppressed before falling
  interception, so that interception uses only currently enabled aura holders.
  No arrow/explosion regression was found by source review, but this was not
  runtime-tested.

## Reproducible native behavior checks

For natural water spread, place a `Blocks.WATER.defaultBlockState()` source on
a solid floor in an active aura, leave a known adjacent target cell open, and
invoke the real fluid tick on the server thread:

```java
BlockState source = level.getBlockState(sourcePos);
level.getFluidState(sourcePos).tick(serverLevel, sourcePos, source);
```

In 26.3 the exact signature is
`FluidState.tick(ServerLevel, BlockPos, BlockState)`; it delegates to
`FlowingFluid.tick`, whose `spread` path performs normal `Level` writes. Keep
the target within the aura for the conversion assertion. For the boundary case,
arrange the only open route across the cube edge and assert the outside water
remains. A direct fluid tick is deterministic enough to trigger the native
spread code without waiting for a random world tick; layout still needs a
solid floor and open route so the intended neighbor is reached.

The current two-cell fixture in `GameChecks.checkArtifactLiquids` can fail for
the right vanilla reason: it puts both the flow source and target inside the
aura, so the next pulse converts both adjacent water blocks to frosted ice.
Those two ice blocks have only one frosted-ice neighbor each. Neighbor updates
from the conversion call `FrostedIceBlock.neighborChanged`, which melts an ice
block when its changed neighbor is frosted ice and it has fewer than two
frosted-ice neighbors. This can melt the pair during the same pulse. That is
native behavior, not a difference from the source command.

I compared the local old Paper 1.21.11 jar's `FillCommand`, `BlockInput`,
`ServerLevel`, and `FrostedIceBlock` bytecode with the 26.3 source. Old
`/fill` also places with `2 | 256`, uses `BlockInput.place`'s neighbor-shape
update before `setBlock`, and batches `updateNeighboursOnBlockSet` afterward;
old frosted ice also melts on this `<2`-frosted-neighbors callback. The source
three-fill sequence therefore has the same two-cell instability.

A more focused test keeps only the flowed cell inside the aura: put the water
source at `(13, 200, 0)` (outside the inclusive radius-12 cube centered at
`(0, 200, 0)`), leave `(12, 200, 0)` as the adjacent flow target, and make the
other target neighbors solid with a floor below. Calling the source fluid tick
flows water into the in-range target; the next pulse converts just that target
to frosted ice, whose outside neighbor is water rather than frosted ice. Assert
the target becomes frosted ice and the source at X=13 remains water. A 2×2
in-range patch of water is another stable fixture because every resulting ice
block has two ice neighbors.

For reproducible frosted-ice melting, use an isolated age-3 frosted-ice state at
a position with local raw brightness greater than 8 (for example, exposed to
daylight). With zero frosted-ice neighbors, `fewerNeigboursThan(..., 4)` is
true, so the scheduled block tick does not depend on its random one-in-three
branch. `BlockState.tick(ServerLevel, BlockPos, RandomSource)` is public in
26.3 and invokes the native scheduled-tick handler; `randomTick` is the wrong
entry point here because `FrostedIceBlock` melts in `tick`, not `randomTick`.

```java
BlockState frosted = Blocks.FROSTED_ICE.defaultBlockState()
        .setValue(FrostedIceBlock.AGE, FrostedIceBlock.MAX_AGE);
level.setBlock(pos, frosted, Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS);
frosted.tick(serverLevel, pos, RandomSource.create(0L));
```

Assert the native tick changed the block to water, then flush the effect's
queued liquid work (directly through package-visible `tickLiquids()` in
`GameChecks`, or on its normal 5-tick cadence) and assert it is converted back
to frosted ice. Check `level.getMaxLocalRawBrightness(pos) > 8` before the call;
otherwise the native scheduled tick reschedules instead of melting. The melt
path calls `Level.setBlock`, which the current arena `ArenaBlockMixin` observes.

## Native source references

All references below are in the local Loom-cache
`minecraft-merged-d6f9bb6878-26.3-sources.jar`:

- `world/level/LevelHeightAccessor.java`: inclusive max Y, min/max section Y,
  and section-index conversion.
- `world/level/chunk/LevelChunkSection.java`: `hasOnlyAir` and `maybeHas`.
- `world/level/ChunkPos.java`: packed chunk-key `getX` and `getZ`.
- `world/level/material/FluidState.java`: public `tick(ServerLevel, pos, state)`.
- `world/level/material/FlowingFluid.java`: native `tick` delegates into spread.
- `world/level/block/FrostedIceBlock.java`: scheduled `tick`, age-3 melt, and
  `Level.setBlock` transitions.
- `world/level/block/state/BlockBehaviour.java`: public `BlockState.tick` and
  separate `randomTick` dispatch.
- `world/level/block/Block.java`: update constants (`CLIENTS=2`,
  `SKIP_BLOCK_ENTITY_SIDEEFFECTS=256`, `SKIP_ON_PLACE=512`).
- `server/commands/SetBlockCommand.java` and `FillCommand.java`: native 258
  placement and batched neighbor updates.

## User-confirmed shared sphere and row-difference review

The user confirmed that liquids and falling blocks share radius 16. This is a
target behavior update; the older source cube in `ARTIFACT_LIQUIDS_NEXT.md`
remains the historical datapack behavior, while the port contract is the
user-confirmed sphere recorded in `ARTIFACT_LIQUIDS_PORT.md`.

The current `liquidWidths()` table stores `floor(sqrt(16²-dy²-dz²))` for each
integer Y/Z offset. Thus a row includes exactly integer X offsets satisfying
`dx²+dy²+dz² <= 16²`; it includes the six axial endpoints and excludes the
corners of the 33³ bounding cube. `findLiquids` clamps the bounding cube to
world build-height before looking up sections, and each row clamps its X
interval to the current chunk. Negative chunk coordinates remain correctly
partitioned because arithmetic `>> 4` and `cx << 4` use 16-block floor-aligned
chunk boundaries.

For movement, the method computes the current sphere interval and the old
sphere interval independently for the same absolute `(y,z)` row. If the old
row is absent, it collects the full current interval. Otherwise it collects
only `[currentLeft, oldLeft-1]` and `[oldRight+1, currentRight]`, each clamped to
the current interval. Since both row cross-sections are contiguous X intervals,
this is exactly `currentSphere \ oldSphere`, including the cases where the old
interval contains the entire new interval, the intervals are disjoint, or the
centers differ along X, Y, or Z. Invalid/empty ranges are no-ops in
`collectLiquids`' loop. New-row and chunk-load scans pass `old=null`, so they
scan the complete current sphere portion in that chunk.

`liquidCovered` applies the same spherical rule with long-valued coordinate
deltas and squared distance `<=16²`. It is used both when `Level#setBlock`
queues newly created liquid states and immediately before queued writes. The
falling-entity interceptor receives the same `LIQUID_RADIUS`, so its squared
distance test shares radius 16. This review found the scan geometry and coverage
predicate aligned; it does not change or claim equivalence with the legacy
cube.

## Beacon-resume fixture note

The current recovery probe `(0,199,1)` is adjacent to the original activation
water at `(0,199,0)`, which is already frosted ice. The probe is first set to
water while the beacon suppresses and removes the aura; after beacon removal,
the fresh sphere scan correctly selects it. When the probe becomes frosted ice,
batched `updateNeighboursOnBlockSet` notifies the existing frosted neighbor.
Vanilla `FrostedIceBlock.neighborChanged` melts that neighbor when it has fewer
than two frosted-ice neighbors, and the melt's native neighbor updates can melt
the probe as well. This can make the post-resume state assertion read water
while the suppression/reactivation path is correct. Place the recovery probe
away from existing frosted ice (for example `(0,199,5)`) to isolate the aura
resume assertion; keep the adjacent-pair melt behavior as its own native test.

## Falling-block test-state codec finding

The focused gravel conversion failure came from the synthetic entity fixture,
not the production state mapping. Minecraft 26.3 `FallingBlockEntity` reads its
`BlockState` using `BlockState.CODEC` and defaults to sand when decoding fails.
That codec accepts a registry-key string for a default state or its full
`{id, properties}` representation for a non-default state; the test's legacy
`{Name: "minecraft:gravel"}` compound is invalid for this field. Consequently,
all probes loaded as sand. Generate the test tag with
`TagValueOutput.store("BlockState", BlockState.CODEC, expectedState)` and assert
`entity.getBlockState().equals(expectedState)` immediately after `load` for
every probe and stop case. The native production `getBlockState()` read and
conversion map need no change.
