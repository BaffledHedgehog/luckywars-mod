# Cursor and crafting inventory refresh hooks

Read-only source confirmation plus the narrow mixin `LbcCursorCraftingMixin` (no menu polling). The existing `LbcInventoryTriggerMixin` continues to cover native inventory slot changes; this mixin adds carried cursor and crafting-input changes that do not alter main inventory.

## Native API/source facts

Minecraft source is local at `.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-d6f9bb6878/26.3/minecraft-merged-d6f9bb6878-26.3-sources.jar`.

- `ServerGamePacketListenerImpl.handleContainerClick(ServerboundContainerClickPacket)` calls `PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level())` first, then `player.resetLastActionTime()`, then `containerMenu.clicked(...)`, then `containerMenu.broadcastChanges()`. Injecting snapshot after the `resetLastActionTime()` invocation runs after the thread handoff and before native mutations. TAIL compares the actual cursor and craft-input contents after click acceptance/rejection.
- `handlePlaceRecipe(ServerboundPlaceRecipePacket)` has the same thread-check then reset ordering; recipe book placement can mutate crafting inputs without an ordinary grid-slot click, so it is covered separately.
- `AbstractContainerMenu` exposes `public final NonNullList<Slot> slots`, `getCarried()`, and `setCarried(ItemStack)`. `Slot.container` is public final.
- Craft grids implement `CraftingContainer extends Container`, including `TransientCraftingContainer` for `AbstractCraftingMenu` and inventory 2x2. The input container's `getContainerSize()` / `getItem(int)` enumerate only its 4 or 9 slots. `ResultSlot` points at `ResultContainer`, not `CraftingContainer`, so result-only changes are excluded unless they also change grid/cursor.
- `ServerPlayer.containerListener` reports only `slotChanged` and forwards to `INVENTORY_CHANGED` when the slot container is the player's main `Inventory`; `ContainerListener` has no carried-stack callback. So attaching an extra ContainerListener alone would miss cursor updates.

## Implementation behavior

`LbcCursorCraftingMixin` snapshots only the current menu's carried stack and unique `CraftingContainer` contents at native packet-handler time, compares with `ItemStack.matches` at TAIL, and calls `game.items().magic().inventoryChanged(player)` only if they differ. Snapshotting is bounded to current menu crafting slots (maximum 9 per grid) and happens only for native click / recipe placement packets. The existing refresh scheduler coalesces changes until tick processing.

Register `LbcCursorCraftingMixin` in `skywars2.mixins.json`'s `mixins` list. Per instruction, I did not edit the mixin JSON or `LbcMagic`, `GameChecks`, or `SkyWars2`.
