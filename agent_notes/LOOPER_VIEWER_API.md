# Looper artifact viewer projection APIs

This note records the Polymer 0.18.2+26.3 API and repository refresh hooks for
the Looper artifact-description requirement. The authoritative requirement is
that only a viewer/player with Looper perk 3 sees an artifact description;
other viewers see the ordinary unknown-artifact presentation. The server keeps
one real stack, including for an item entity on the ground. The requirement is
per-recipient projection and forbids mutating the real stack or deleting an
unresolved artifact.

## Polymer conversion

`PolymerItem` provides this exact overridable method:

```java
default ItemStack getPolymerItemStack(
        ItemStack itemStack,
        TooltipFlag tooltipType,
        PacketContext context,
        HolderLookup.Provider lookup)
```

The default delegates to `PolymerItemUtils.createItemStack(itemStack,
tooltipType, context, lookup)`. It is a supported per-recipient conversion
hook, including when Polymer converts item stacks in inventories or item
entities. `PacketContext` identifies the recipient through
`PolymerCommonUtils.getPlayer(context)`; the method returns `null` when there
is no server player. The repository already uses this accessor in
`GameChecks.java` around line 1602, and gets contexts from
`PacketContextProvider.getPacketContext()`.

Default conversion order in Polymer's `PolymerItemUtils.createItemStack` is:

1. Select the client item/model and construct a proxy stack.
2. Copy configured components, then call `modifyBasePolymerItemStack`.
3. Set Polymer restoration data in the proxy's `CUSTOM_DATA`.
4. Build the vanilla tooltip from the authoritative stack with
   `itemStack.getTooltipLines(...)`; remove its first line (the item name).
5. Call `PolymerItem.modifyClientTooltip(tooltip, itemStack, context)`.
6. Convert each remaining tooltip line to a clean-style component and put it
   in proxy `DataComponents.LORE` as `ItemLore`.
7. Run Polymer's item modification event and return the client projection.

For the artifact description, the most direct hook is
`modifyClientTooltip(List<Component>, ItemStack, PacketContext)`: inspect the
recipient through `PolymerCommonUtils.getPlayer(context)` and append the
description only when that player's current loadout has perk 3. This keeps
Polymer's standard native item name and lore formatting, executes before
Polymer applies `CLEAN_STYLE`, and leaves the stored stack untouched. Do not
gate this visual projection with `canUseGameplayItems`; observers can still
be viewers and may have the selected perk. Gameplay eligibility remains a
separate rule.

Alternatively, the full `getPolymerItemStack` override runs after base
conversion, but must preserve the generated name/lore and Polymer metadata
while editing only the client stack. `getPolymerItemStack` is also the useful
extension point if a complete client-side stack projection is needed. Its
output carries Polymer's restoration blob; the real stack is recovered by
`PolymerItemUtils.getRealItemStack` (existing `GameChecks` roundtrip probes).

## Refreshing after perk changes

The repository's existing native inventory refresh pattern is
`menu.sendAllDataToRemote()`; see `GameMenus.java` and
`LobbyInventoryPacketMixin.java`. `AbstractContainerMenu.broadcastChanges()`
is used for routine inventory changes (`GameController.java`), while
`sendAllDataToRemote()` is the stronger refresh used by current menus and
includes the active menu state. On a perk toggle, refresh the player's
`inventoryMenu` and, if distinct, `containerMenu`; this covers inventory
slots, cursor/carried stack, and currently open container slots. No real stack
mutation is needed to make Polymer recompute a projection when these menu
packets are sent.

Fabric's cached Networking API 5.0.13 provides
`PlayerLookup.tracking(Entity)`, an immutable collection of current viewers;
call it on the server thread. For a known entity, filter this collection to the
viewer whose perk changed. `PlayerLookup.tracking(player)` means players
watching that player; it does not enumerate entities watched by the player,
and Fabric documents that a player entity is not guaranteed to appear in its
own tracking collection.

For a known `ItemEntity`, a one-recipient refresh can resend the native metadata
without changing entity data:

```java
viewer.connection.send(new ClientboundSetEntityDataPacket(
        itemEntity.getId(), itemEntity.getEntityData().getNonDefaultValues()));
```

The 26.3 packet is a record with fields `(int id,
List<SynchedEntityData.DataValue<?>> packedItems)`. `ItemEntity` stores its
stack in synced `DATA_ITEM`; `getNonDefaultValues()` returns a full current
snapshot and does not mark values dirty. The packet's ordinary item-stack
encoding runs Polymer's `ItemStackPacketCodecMixin`, which calls
`PolymerItemUtils.getPolymerItemStack(itemStack, context, registryAccess)`
under the connection's packet context. Thus sending the native packet to only
the changed viewer re-runs the normal Polymer projection without touching the
real stack. Use it only if the viewer is in `PlayerLookup.tracking(itemEntity)`.

For an already tracked living entity wearing/holding an artifact, the native
refresh packet is `ClientboundSetEquipmentPacket(int entity,
List<Pair<EquipmentSlot, ItemStack>> slots)`. Vanilla `ServerEntity.sendPairingData`
uses this packet for initial equipment sync. Build the list for the entity's
current equipment slots and send it to the toggled viewer only when
`PlayerLookup.tracking(entity).contains(viewer)`. The packet's ItemStack
encoding likewise passes through Polymer's stack codec. This is a native
packet refresh, not a custom packet engine, and it leaves authoritative
equipment unchanged.

The lookup is entity-to-viewer; there is no native helper for enumerating all
entities a particular player tracks. Avoid a world-wide scan. On a perk change,
use a one-time spatial query around that viewer over the relevant vanilla
entity tracking radius (entity types expose `clientTrackingRange()` in chunks),
then filter each candidate with `PlayerLookup.tracking(candidate)` before
sending. This is enough to catch relevant visible dropped-item and living
entity stacks while keeping refresh work local to the changed viewer. Candidate
query bounds should account for the largest relevant entity type's
`clientTrackingRange() * 16` blocks and the level's view-distance limit.

## Sources checked

- Cached source jar:
  `C:/Users/yarik_temp/.gradle/caches/modules-2/files-2.1/eu.pb4/polymer-core/0.18.2+26.3/b6b76cb8184ceec3de099762a03da7cc3cd0339b/polymer-core-0.18.2+26.3-sources.jar`
  (`PolymerItem.java`, `PolymerItemUtils.java`).
- Cached source jar:
  `C:/Users/yarik_temp/.gradle/caches/modules-2/files-2.1/eu.pb4/polymer-common/0.18.2+26.3/98775ab324bbdac63e25ecdcee017de9bf0d7397/polymer-common-0.18.2+26.3-sources.jar`
  (`PolymerCommonUtils.java`).
- Cached Fabric Networking API source:
  `C:/Users/yarik_temp/.gradle/caches/modules-2/files-2.1/net.fabricmc.fabric-api/fabric-networking-api-v1/5.0.13+d269470c7d/91f5f3aa61699450156a32f75b9e947e7a809c7d/fabric-networking-api-v1-5.0.13+d269470c7d-sources.jar`
  (`PlayerLookup.java`).
- Cached Minecraft 26.3 named source:
  `.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-d6f9bb6878/26.3/minecraft-merged-d6f9bb6878-26.3-sources.jar`
  (`ItemEntity.java`, `SynchedEntityData.java`, `ClientboundSetEntityDataPacket.java`,
  `ClientboundSetEquipmentPacket.java`, `ServerEntity.java`, `EntityType.java`).
- Repository hooks: `src/main/java/me/skywars2/game/GameChecks.java`,
  `GameMenus.java`, `GameController.java`, and
  `src/main/java/me/skywars2/mixin/LobbyInventoryPacketMixin.java`.
