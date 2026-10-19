# Creative players and developer lobby bypass

`GameController` now owns the shared policy helpers:

- `isDeveloper(player)` checks the entity tag `developer`.
- `canUseGameplayItems(player)` permits an active participant during a match regardless of game mode, or a tagged developer during lobby/loading. It always rejects observers, including tagged developers.
- `inventoryLocked(player)` keeps observer inventories locked and skips the ordinary lobby/loading inventory lock for developers.

`LbcItems.epoch()` and `currentEpoch(String)` expose the existing item entity epoch for native block-entity reuse.

## Implemented scope

- `GameController.java`: central predicates; developer bypass for lobby/loading inventory locking and routine lobby relocation/reset. `alivePlayers()` and authoritative role/scoring state are untouched.
- `LbcItems.java`, `LbcClouds.java`, `LbcTrinkets.java`, `LbcFrost.java`: owner/ability eligibility uses `canUseGameplayItems` where it guards a gameplay ability. Current-epoch tags keep accepted lobby/loading projectiles, clouds, TNT, and nanobot markers alive through the lobby entity audit; dynamite and effect ticking runs during the debug window. Acid projectiles inherit the cloud owner and current epoch. Player target eligibility continues to use `allowDamage`, so a developer tag does not make observers or nonparticipants valid targets.
- `MapManager.java`: lobby entity cleanup retains entities bearing the current LBC item epoch, allowing accepted debug effects to survive its periodic sweep. `LbcItems.currentEpoch(Entity)` is the shared predicate.
- `CreativeBlockDropsMixin.java`: modifies the `ServerPlayerGameMode.destroyBlock` calls to `ServerPlayer.preventsBlockDrops()` and `ServerPlayer.hasCorrectToolForDrops(BlockState)`. For ordinary Creative breaks it lets the existing native break flow continue to `Block.playerDestroy` and native loot exactly once, including bare-hand breaks. A developer tag preserves vanilla's no-drop branch. If the game controller identifies the player as an observer, the mixin preserves the no-drop branch.
- `LobbyInventoryPacketMixin` and `LobbyItemPickupMixin` already use `inventoryLocked(player)`, so they inherit the developer bypass while preserving observer locking.

## Hook assumptions and root wiring

The mapped 26.3 source for `ServerPlayerGameMode.destroyBlock(BlockPos)` removes the block, then executes `ServerPlayer.preventsBlockDrops()` before constructing the native destroyed-with stack and checking `hasCorrectToolForDrops`. The mixin bypasses both Creative gates for ordinary non-observer players without the `developer` tag. Native `Block.playerDestroy`, the actual held tool passed to its loot context, and the block loot table remain authoritative.

Уточнение пользователя:

> Дроп даже пустой рукой

Fabric item and block interaction callbacks are in `SkyWars2.java`. Root wiring uses `canUseGameplayItems(player)` for relevant item-use restrictions and retains explicit observer checks. `LbcMagic.java` also uses the predicate for ability permission without a creative-mode exclusion.

## GameChecks test plan for root integration

1. In a game controller fixture, break a stone block bare-handed as an ordinary non-observer Creative player through `ServerPlayerGameMode.destroyBlock`; confirm native loot appears once.
2. Repeat with the `developer` tag and confirm no block loot appears.
3. Repeat with a live participant in Survival and confirm native drops are unchanged.
4. Confirm `canUseGameplayItems` accepts an active Creative participant and a lobby developer, and rejects an observer carrying the `developer` tag.
5. Confirm an observer remains inventory-locked even when tagged `developer`.
6. In lobby, throw custom dynamite and a chemical lingering potion as a tagged developer; confirm their accepted entities survive `MapManager.cleanupLobbyEntities`, can affect mobs, and cannot damage players. Confirm the same uses are rejected for observers.

The root's final build passed all 30 server runner groups in 11 matches
(212.50 s), including native bare-hand Creative drops, developer exclusion,
lobby debug items and observer guards. The exact fixtures and limits are in
[SERVER_TESTS.md](../SERVER_TESTS.md). The tested JAR is installed, its
predecessor is backed up, and both servers are stopped.
