# Magic suppression: event and placement API audit

Read-only source review for the Mystic Artifact `custom_data {mystical:1,type:1}` and Anti-Magic Beacon. Minecraft API is the local `minecraft-merged-d6f9bb6878-26.3-sources.jar`; Fabric API is `0.161.0+26.3`.

## Inventory event recommendation

Use a small mixin on native `InventoryChangeTrigger.trigger`:

```java
public void trigger(ServerPlayer player, Inventory inventory, ItemStack changedItem)
```

This is called for native inventory changes and gives the affected player; it avoids polling every player. Recompute `nomagic_active` from `player.getInventory()` by checking indices `0..35` and `Inventory.SLOT_OFFHAND` (`40`) only. In this version `Inventory.getContainerSize()` is 42: slots 36–39 are armor and 41 is body armor, so do not iterate the entire container. This covers main inventory/hotbar/offhand and excludes armor, cursor, and crafting. The trigger itself also runs for equipment edits; the recomputation remains restricted to the requested slots.

Keep `ObserverAdvancementMixin`'s existing `PlayerAdvancements.award` HEAD cancellation for observers. That mixin does not itself refresh inventory-derived state; `InventoryChangeTrigger` is a separate target and needs its own injection (or a dedicated mixin). Also perform one recomputation at player join/respawn because an event hook only covers subsequent changes.

Old source confirms the predicate and search scopes: `items/changed_inventory/sel.mcfunction` lines 60–62 checks `container.*` and `weapon.*` for exactly `{mystical:1,type:1}`, adding/removing `nomagic_active`; `artifact.json` line 187 creates that custom data. No armor check participates.

## Beacon placement and marker lifecycle

Native item placement entrypoint: `Item.useOn(UseOnContext)` (overridden by `BlockItem.useOn`). The context exposes `getClickedPos()`, `getClickedFace()`, `getClickLocation()`, `getItemInHand()`, and `getPlayer()`. If wrapping the native Panda Spawn Egg behavior, delegate to `Items.PANDA_SPAWN_EGG.useOn(context)` so vanilla spawn-egg entity data and placement semantics remain in force; handle the resulting marker/rod location only after confirming the result consumes the action. For manual block placement, vanilla `BlockItem.placeBlock` uses `level.setBlock(pos, placementState, 11)`; old beacon activation uses `setblock` and does not specify a flag. Existing arena tracking injects `Level.setBlock(BlockPos, BlockState, int flags, int recursion): boolean` at RETURN and only records successful writes (`ArenaBlockMixin` lines 16–22).

Native marker creation API is `EntityType.MARKER.create(level, EntitySpawnReason.EVENT)`, then `marker.setPos(...)`, `marker.addTag("stopper_magic")`, and `ServerLevel.addFreshEntity(marker)`. `Marker` saves through the normal entity serialization path: base `Entity` persists UUID, position, and entity tags; `Marker`'s additional save/load payload is empty. Its `getAddEntityPacket` throws, so it is server-side and must not be treated as a client-visible entity. Existing Fabric lifecycle hooks in `SkyWars2` establish exact callback shapes:

```java
ServerEntityEvents.ENTITY_LOAD.register((Entity entity, ServerLevel level) -> ...);
ServerEntityEvents.ENTITY_UNLOAD.register((Entity entity, ServerLevel level) -> ...);
```

These callbacks are already used for item and lobby entity bookkeeping. They can attach/rebuild runtime beacon state for a persisted marker on load and discard transient indexes on unload; rely on the native entity's saved `stopper_magic` tag/position as durable identity. The spawn egg's `entity_data`/tags are applied as part of native spawn before the load notification, so load handling can filter the marker tag.

## Detecting rod removal without 31³ scans

`ArenaBlockMixin` already observes every successful native setBlock write in the active arena and calls `game.maps().recordBlockChange(pos)`. Add a narrow beacon notification at that same point (or through `MapManager.recordBlockChange`): compare only the changed `BlockPos` against indexed active beacon positions; if the resulting state is no longer `Blocks.LIGHTNING_ROD`, complete that beacon once. This avoids re-running the old `fill ~15 ... ~-15` (31×31×31) each 2 ticks. Since mixin sees new state only after success, replacement and removal both resolve from the current block state.

Old lifecycle facts, from read-only datapack functions:

- `stopperactivate_magic.mcfunction`: if marker position is air, put a rod there; if occupied (and not already rod), put one above and teleport marker to that position; then tag `activated`. The marker's current position is therefore the rod position after activation.
- `stopper_magic.mcfunction`: on each 2-tick pass clears `#minecraft:magic` in a 31³ cube, removes magic entities in radius 20, and calls `stopperbreaked_magic` if an activated marker no longer sits on a rod.
- `stopperbreaked_magic.mcfunction`: execute as nearest dropped lightning rod and return the spawn egg in `stopperbreaked_magicasitem`; then `kill @s` removes the original marker. This corrects the stale claim in `MAGIC_SUPPRESSION_NEXT.md` that the marker was left behind. `stopper_magic_destroy.mcfunction` is for each affected target entity (particles + `kill @s`); it does not own marker cleanup.

Thus native removal should mirror this as an idempotent one-shot at the exact rod coordinate: return one beacon item and discard its marker/index once. Marker removal should also remove any transient tracking entry. On shutdown, clear transient indexes; on entity unload, remove the in-memory entry and let native save/load restore the tagged marker.

## Source links

- [ObserverAdvancementMixin](../src/main/java/me/skywars2/mixin/ObserverAdvancementMixin.java)
- [ArenaBlockMixin](../src/main/java/me/skywars2/mixin/ArenaBlockMixin.java)
- [MapManager.recordBlockChange](../src/main/java/me/skywars2/game/MapManager.java#L593)
- [Fabric entity lifecycle registration](../src/main/java/me/skywars2/SkyWars2.java#L59)
- Old datapack: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/{items/changed_inventory/sel,other/stopperactivate_magic,other/stopper_magic,other/stopperbreaked_magic,other/stopperbreaked_magicasitem,other/stopper_magic_destroy}.mcfunction`.
