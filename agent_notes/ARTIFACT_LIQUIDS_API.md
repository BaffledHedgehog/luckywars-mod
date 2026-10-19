# Type 1:3 liquid and falling-block native API findings

Read-only API research for the type 1:3 port. Target sources are from the local
Loom-cache `minecraft-merged-d6f9bb6878-26.3-sources.jar`. This note describes
available APIs and their limits; it does not claim a port or runtime validation.
Legacy behavior and the user contract remain in [ARTIFACT_LIQUIDS_NEXT.md](ARTIFACT_LIQUIDS_NEXT.md).

## Loaded-chunk palette scan

For a loaded-only scan, get a `LevelChunk` with
`((ServerLevel) level).getChunkSource().getChunkNow(chunkX, chunkZ)`. In
`ServerChunkCache.java`, `getChunkNow` returns `null` off the server main thread,
when no visible holder exists, or when `ChunkStatus.FULL` is unavailable. It
does not request generation/loading. `getChunk(x, z, ChunkStatus.FULL, false)`
also has `loadOrGenerate=false`, but it can wait through the main-thread
processor; `getChunkNow` is the better fit for an effect that should simply skip
unloaded chunks.

`ChunkAccess.getSections()` returns `LevelChunkSection[]`. Each section's
`maybeHas(Predicate<BlockState>)` delegates to the section block-state palette's
`maybeHas`; it tests palette entries rather than all 4096 coordinates. This is
the useful fast reject for empty/nonmatching sections. For a matching section,
`LevelChunkSection.getBlockState(localX, localY, localZ)` reads palette-backed
states at local coordinates 0..15. `ChunkAccess` also exposes
`getMinSectionY()` / `getMaxSectionY()` through `LevelHeightAccessor`, and
`getSectionIndexFromSectionY(sectionY)` for array lookup.

Suggested bounded scan for the exact inclusive holder-centered cube: enumerate
only chunk X/Z coordinates intersecting `[cx-12,cx+12] × [cz-12,cz+12]`, call
`getChunkNow`, then clamp the cube Y interval to each chunk's min/max block Y
and iterate the intersecting section Ys. Use `section.maybeHas(s ->
s.is(Blocks.LAVA) || s.is(Blocks.WATER) || s.is(Blocks.POWDER_SNOW))` as the
palette guard. In matching sections, visit only the cube/section intersection
(X/Z clamped to that chunk) and compare exact states before converting. That
preserves the source cube and skips absent chunks without turning the 5-tick
effect into unconditional calls to `Level.getBlockState` across unloaded
coordinates. Palette guard is per section, so it can still scan coordinate
intersections in sections that contain one target palette entry elsewhere.

For command-equivalent placement, 26.3 `BlockInput.place(level, pos, 258)` is a
useful reference: it first runs `Block.updateFromNeighbourShapes`, then calls
`level.setBlock` (and processes block entity data only when a tag is supplied).
The normal `/setblock` path
uses flags `2 | 256` (258), then calls
`level.updateNeighboursOnBlockSet(pos, oldState)`. `/fill` uses the same 258
placement per changed position, but batches those neighbor updates until after
the block loop. For a palette scan, collect successful replacements and each
position's old state, place with equivalent neighbor-shape preparation and
flags 258, then call `updateNeighboursOnBlockSet` for the collected positions.
This matches the non-strict native fill update pattern and avoids doing the
neighbor work inline for each converted cell. Keep the scan and update phase on
the server thread, as required by `getChunkNow`.

The core native calls can be shaped as `new BlockInput(replacement, Set.of(),
null).place(level, pos, 258)` after saving the old state; retain `(pos, oldState)`
for each successful placement, then loop that list through
`level.updateNeighboursOnBlockSet(pos, oldState)`. This can be reused for the
matching falling-block conversion at `entity.blockPosition()` before
`entity.kill(serverLevel)`.

## Native mutation hook coverage

The current [ArenaBlockMixin](../src/main/java/me/skywars2/mixin/ArenaBlockMixin.java)
injects at RETURN of the `Level.setBlock(BlockPos, BlockState, int, int)` overload
and only processes a successful return when `this` is the active arena level.
Vanilla `Level.setBlock` calls `LevelChunk.setBlockState`, then performs normal
block update work. Frosted-ice age increments use `level.setBlock(pos, ..., 2)`;
its melt path also goes through `level.setBlock` (see `FrostedIceBlock`). So
those vanilla changes pass through this hook when they occur in the arena.

This is not evidence that every state mutation is intercepted: direct
`LevelChunk.setBlockState` calls bypass `Level.setBlock`, and the mixin excludes
all non-arena worlds. The existing hook is an arena-change observer and current
magic notification point; it is not itself a universal fluid-flow veto or
replacement hook. A new effect that must convert incoming flow should inject at
the mutation boundary before commit (or otherwise synchronously replace the
state), and must account for non-arena use only if that is in scope. Do not
infer full random-tick coverage solely from the broad method target.

## FallingBlockEntity API

For an existing `FallingBlockEntity`, `getBlockState()` publicly exposes the
represented state; its coordinates come from the entity's normal position API.
`Entity.kill(ServerLevel)` removes it with reason `KILLED` and emits the entity
death game event, but does not run block item drops. `discard()` also removes
without a kill event. The legacy function always kills every falling-block
entity in the 16-block sphere after its conversion checks, so `kill(serverLevel)`
is the closest native removal call when retaining that event behavior matters.

Correction from the actual read-only legacy function
`world/datapacksdisabled/LBC/data/lbc/function/other/artifact/block_otverditel.mcfunction`:
every listed branch executes `setblock ~ ~ ~ <converted block>` at the falling
entity's position. Sand, gravel, red sand, anvils and concrete powders place
their mapped solid; stone bricks and netherite blocks place the same block they
represent. No branch rewrites the entity's private `blockState`. The entity's
`BlockState.Name` is only the condition used to choose what world block to
place. Therefore the port only needs to read `getBlockState()`, place the
corresponding world state for matching branches, and remove the entity. It does
not need an accessor or an entity-state mutation path.

`FallingBlockEntity.fall(level,pos,state)` is not suitable here: it replaces the
source block (with its fluid legacy block) and adds a new entity. For tests that
need synthetic `FallingBlockEntity` state, the public `(EntityType, Level)`
constructor plus public `Entity.load(ValueInput)` works with
`TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag)`;
the native falling-block load hook reads `BlockState` through
`BlockState.CODEC`. Build that tag with
`TagValueOutput.store("BlockState", BlockState.CODEC, state)`. In 26.3 the
codec represents a default block state as its registry key and a non-default
state as the full codec object (`id` plus `properties`). The old
`{Name: "minecraft:gravel"}` compound is not this codec format. A decode failure
is silently replaced by `FallingBlockEntity.DEFAULT_BLOCK_STATE` (sand), so
synthetic entity tests can accidentally test sand for every intended state.
After `Entity.load`, assert `entity.getBlockState().equals(state)` before testing
conversion. Existing test helpers in `GameChecks.java` already exercise
`saveWithoutId` and `Entity.load(TagValueInput...)` patterns at lines 716-718,
883-886, and 931-935, so this path does not require an accessor.
The native fall factory also removes waterlogging from its spawned entity state.

## Source references

- Loom source jar: `.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-d6f9bb6878/26.3/minecraft-merged-d6f9bb6878-26.3-sources.jar`
- `net/minecraft/server/level/ServerChunkCache.java`: `getChunk(..., loadOrGenerate)` and `getChunkNow`.
- `net/minecraft/world/level/chunk/ChunkAccess.java`: `getSections`, section lookup, bounds, and `findBlocks` palette-guard pattern.
- `net/minecraft/world/level/chunk/LevelChunkSection.java`: palette-backed `getBlockState` and `maybeHas`.
- `net/minecraft/world/level/chunk/PalettedContainer.java`: `maybeHas` delegates to palette entries.
- `net/minecraft/world/level/Level.java`: `setBlock` mutation route and `getBlockState` chunk lookup.
- `net/minecraft/server/commands/SetBlockCommand.java`: native setblock placement flags and neighbor update.
- `net/minecraft/server/commands/FillCommand.java`: 258 placement loop and batched neighbor updates.
- `net/minecraft/commands/arguments/blocks/BlockInput.java`: neighbor-shape update before `Level.setBlock`.
- `net/minecraft/world/level/block/FrostedIceBlock.java`: age/melt transitions call `Level.setBlock`.
- `net/minecraft/world/entity/item/FallingBlockEntity.java`: constructor, `fall`, `getBlockState`, and state persistence.
- `net/minecraft/world/entity/Entity.java`: `kill(ServerLevel)`, public `load(ValueInput)`, and `saveWithoutId` behavior.
- `net/minecraft/world/level/block/state/BlockState.java`: 26.3 `CODEC` and `FULL_CODEC` representation.
- Legacy read-only `world/datapacksdisabled/LBC/data/lbc/function/other/artifact/block_otverditel.mcfunction`: every branch places a world block before killing the falling entity.
