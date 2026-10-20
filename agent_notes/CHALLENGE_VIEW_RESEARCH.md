# Challenge item view research (20 October 2026)

Research only; no behavior, resource pack, or template changes. Implementation depends on the pending user choice between per-viewer hiding and legacy destructive component mutation/chat spam.

## Installed source evidence

`gradle.properties`: Minecraft 26.3, Polymer 0.18.2+26.3.

Polymer source archive:
`C:/Users/yarik_temp/.gradle/caches/modules-2/files-2.1/eu.pb4/polymer-core/0.18.2+26.3/b6b76cb8184ceec3de099762a03da7cc3cd0339b/polymer-core-0.18.2+26.3-sources.jar`.
Minecraft source archive:
`.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-d6f9bb6878/26.3/minecraft-merged-d6f9bb6878-26.3-sources.jar`.

## Global projection API

In Polymer archive `eu/pb4/polymer/core/api/item/PolymerItemUtils.java`:

```java
public static final Event<BiPredicate<ItemInstance, PacketContext>> CONTEXT_ITEM_CHECK;
public static final Event<ItemModificationEventHandler> ITEM_MODIFICATION_EVENT;
public interface ItemModificationEventHandler {
    ItemStack modifyItem(ItemStack original, ItemStack client, PacketContext context);
}
public static ItemStack getPolymerItemStack(ItemStack itemStack, PacketContext context, HolderLookup.Provider lookup);
```

`CONTEXT_ITEM_CHECK` explicitly supports forcing vanilla items into Polymer conversion. `getPolymerItemStack` checks it for ordinary items and calls `createItemStack`; registered Polymer items already use their implementation. `createItemStack` constructs a distinct output, stores the authoritative original under `$polymer:stack`, builds its tooltip/lore, then invokes `ITEM_MODIFICATION_EVENT` as its final step. The modification event alone does not cover unchanged vanilla stacks: use both hooks for the affected viewer. Existing built-in SimplePolymerItem paths use this builder; a custom override that bypasses the builder would need individual review.

The final handler can set only client components: empty `ITEM_NAME` and `CUSTOM_NAME`, remove/empty `LORE`, and set `TOOLTIP_DISPLAY` to `new TooltipDisplay(true, ReferenceSortedSets.emptySet())`. Hiding tooltip alone does not blank the selected-hotbar name; blanking names closes that visible route. Keep custom data and all gameplay/model components intact, especially Polymer restoration data. Never mutate `original`. This is visual concealment, not removal of information from the network payload: Polymer restoration NBT retains the original data.

Global packet coverage is confirmed by `eu/pb4/polymer/core/mixin/item/packet/ItemStackPacketCodecMixin.java`, whose encode HEAD calls `getPolymerItemStack` whenever `PacketContext` exists. `mixin/item/ItemStackMixin.java` also projects codec encoding on the server networking thread. These cover normal item-stack serialization in inventory, cursor, metadata and equipment packets. Receive decoding calls `getRealItemStack`. Nested item components follow normal stack serialization; test bundle/container previews if this scope is selected.

## Existing owner and refresh

Keep cosmetic projection and resend responsibility in `src/main/java/me/skywars2/game/LbcSkills.java`, beside Looper's `artifactTooltip` and `refreshViewer`. Do not add another inventory scanner. Keep the challenge selection/state in its existing challenge/loadout owner and expose a concurrent immutable/volatile cosmetic snapshot, following `LbcLoadouts` artifact access: packet encoding can run off the server thread. Do not read mutable scoreboard/world state in callbacks.

`refreshViewer(ServerPlayer)` already calls `inventoryMenu.sendAllDataToRemote()` and the distinct open `containerMenu.sendAllDataToRemote()`. Minecraft `AbstractContainerMenu.sendAllDataToRemote()` copies all menu slots AND carried stack and calls synchronizer `sendInitialData`; `ServerPlayer` sends `ClientboundContainerSetContentPacket(..., slotItems, carriedItem)`. Thus cursor is included without manually changing it or forcing an extra cursor packet.

For all-items hiding, generalize existing artifact-only resend filters on rare state transitions:

- Every tracked `ItemEntity`: resend `ClientboundSetEntityDataPacket(entity.getId(), entity.getEntityData().getNonDefaultValues())` when non-null, as existing helper does. Its item metadata serializes through Polymer again.
- Every tracked `LivingEntity`: send current equipment via `ClientboundSetEquipmentPacket(int entityId, List<Pair<EquipmentSlot, ItemStack>> slots)` for all relevant nonempty slots, using the existing `EQUIPMENT_SLOTS`. Check `PlayerLookup.tracking(entity).contains(viewer)` before sending. Menu refresh already covers the viewer's own equipment inventory.
- Continue using actual authoritative stacks in packets; conversion occurs at encoding. Do not alter inventories/equipment, dirty metadata, respawn entities, or broadcast the cosmetic update to other viewers.

Publish the new cosmetic snapshot BEFORE resend. Trigger once on entering/leaving the challenge, relevant loadout/challenge configuration changes and existing reset/respawn lifecycle; ordinary later item packets project automatically. Existing Looper resend can be generalized/reused so combined changes do not create duplicate scans. Restoration on leaving uses the same resend path and original server stacks.

Validation if implemented: two viewers with opposite flags; vanilla + native Polymer items; hotbar name, tooltip/lore, cursor, open container, dropped item and tracked mob/player equipment; toggle on/off restores presentation and byte-equivalent server components; creative roundtrip preserves originals. No runtime claim has been tested in this research.
