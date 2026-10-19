# Polymer beacon block API audit

Confirmed against the local Minecraft 26.3 sources and Polymer 0.18.2+26.3 sources. The implementation now uses a real server block with vanilla lightning-rod client rendering.

## Polymer and native block API

- `BeaconBlock extends LightningRodBlock implements PolymerBlock, EntityBlock` in `LbcMagic` uses the inherited `FACING`, `POWERED`, and `WATERLOGGED` properties. `getPolymerBlockState(BlockState, PacketContext)` maps all three values to the unaffected vanilla `Blocks.LIGHTNING_ROD` state, preserving the placement-facing and powered visual. Polymer's exact interface signature is `BlockState getPolymerBlockState(BlockState state, @Nullable PacketContext context)`.
- The custom block is registered in `BuiltInRegistries.BLOCK`; its `PolymerBlockItem(Block, Item.Properties, Item, boolean)` uses the existing Panda Spawn Egg appearance/model. `PolymerBlockUtils.registerBlockEntity(BEACON_DATA)` marks the server-only BE type for Polymer, so clients do not receive a custom BE payload. No custom packet or resource-pack assets were added.
- Native 26.3 `BlockBehaviour` has `onPlace(BlockState, Level, BlockPos, BlockState, boolean)` and `affectNeighborsAfterRemoval(BlockState, ServerLevel, BlockPos, boolean)`. There is no `onRemove` method in this version. `LevelChunk` calls the old state's `affectNeighborsAfterRemoval(...)` during a state replacement, then calls the new state's `onPlace(...)`. The project instead centralizes lifecycle and arena journal updates in the successful `Level.setBlock(...)` hook in `ArenaBlockMixin`.

## Non-ticking block entity and chunk index

- `BeaconData` is a native `BlockEntity` with only a saved `epoch` string, no ticker, and no custom client update packet. The epoch uses the existing `LbcItems` epoch API; it identifies stale blocks after the active match changes.
- On chunk load, `LbcMagic.chunkLoaded(LevelChunk)` iterates `chunk.getBlockEntitiesPos()`, which includes native stored block-entity positions, checks the block state, and restores each valid beacon into the runtime chunk-region index and scan queue. It does not scan every block or force-load neighboring chunks. On chunk unload, centers in that chunk are removed from runtime indexes; pending chunk NBT retains their position and epoch for the next load.
- A beacon with an obsolete epoch is journaled through `MapManager.recordBlockChange(pos)` and removed. For stop cleanup, loaded beacon blocks are removed directly and unloaded positions are left to the existing MapManager recovery journal; the path does not synchronously load chunks.
- `BeaconBlock.getDrops(...)` obtains the existing `lbc:ported/magic_beacon` loot through `LbcLoadouts`/`game.lbc().rollTable`. `getCloneItemStack(...)` returns the same custom item for middle-click pick. The arena block-change hook keeps `MapManager`'s mutation bounds journal current.

## Creative destruction call chain

The local Minecraft source JAR is `.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-d6f9bb6878/26.3/minecraft-merged-d6f9bb6878-26.3-sources.jar`. In `ServerPlayerGameMode.destroyBlock(BlockPos)`, native order is: `Block.playerWillDestroy(...)`; `Level.removeBlock(pos, false)`; `Block.destroy(...)` if changed; then `ServerPlayer.preventsBlockDrops()` early return; then `hasCorrectToolForDrops(...)`; finally `Block.playerDestroy(...)` when changed and tool-valid. The registered block's native loot path is therefore reached through `playerDestroy` after the game-mode checks.

`CreativeBlockDropsMixin` modifies the results of both `preventsBlockDrops()` and `hasCorrectToolForDrops(...)` within `destroyBlock`, preserving the native block-break/loot flow for eligible Creative players. It leaves developer-tagged players and observers under their existing restrictions. The beacon's overridden native loot function returns the ported beacon item through the prior table.

## Source references

- [LbcMagic](../src/main/java/me/skywars2/game/LbcMagic.java): block/item registration, Polymer mapping, `BeaconData`, chunk load/unload indexing, and loot.
- [ArenaBlockMixin](../src/main/java/me/skywars2/mixin/ArenaBlockMixin.java): successful native block writes and arena cleanup journal.
- [CreativeBlockDropsMixin](../src/main/java/me/skywars2/mixin/CreativeBlockDropsMixin.java): creative drop gates in the native game-mode break path.
- [MapManager.recordBlockChange](../src/main/java/me/skywars2/game/MapManager.java#L593): persistent arena mutation journal.
