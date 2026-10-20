# Electro shield client rendering review

Date: 2026-10-20. Read-only source inspection; no build or server run.

Parent integration completed: `effectiveModel()` now returns `minecraft:shield`.
Build and four isolated-server suites passed on SHA `6fc4df11…`; source,
Polymer projection and actual container-slot packet preserve the exact base
and six layers for loot and 2x2/3x3 crafted previews/taken output. Bare default
model and existing blocking/reflection are covered. No real-client manual
render test was performed. See SERVER_TESTS and
`build/verification/electro-shield-render-verification.json`.

## Finding

`LbcElectro.registerShield()` sets `ITEM_MODEL=minecraft:shield` with `.component(...)`, but Minecraft 26.3 overwrites this registration-time value. Native `Item.Properties` uses `model = ResourceKey::identifier` (Item.java:398), and `finalizeInitializer` appends an initializer which sets `ITEM_NAME` and `ITEM_MODEL` to the effective defaults **after** the explicitly configured component initializer (Item.java:732-734). Thus the registered default model becomes `skywars2:electro_shield`.

`new SimplePolymerItem(props, Items.SHIELD, true)` preserves that server model: `SimplePolymerItem.getPolymerItemModel` delegates to `PolymerItem.super`, which returns `stack.get(ITEM_MODEL)`. `PolymerItemUtils.createItemStack` then assigns that model to the outgoing vanilla shield. A vanilla client without a resource pack has no `skywars2:electro_shield` item-model definition, so it cannot reach the native shield special renderer through this model.

The narrow registration fix is an `Item.Properties` override of `effectiveModel()` returning `minecraft:shield`, matching the established adjacent item registrations. Alternatively `useModel=false` makes Polymer choose the replacement vanilla shield model; this also discards any deliberate per-stack model overrides, so the effectiveModel override preserves more of the existing contract. No Java changes made in this review.

## Pattern transport and native renderer

Polymer 0.18.2+26.3 `PolymerItemUtils.COMPONENTS_TO_COPY` explicitly contains `BANNER_PATTERNS` and `BASE_COLOR` (lines 143-144). `createItemStack` copies their values from the source stack (lines 437-445). No additional Polymer copied-component registration is required.

Minecraft 26.3 `ShieldSpecialRenderer.extractArgument` reads immutable stack components. `submit` reads `BANNER_PATTERNS` with EMPTY fallback and nullable `BASE_COLOR`; either a nonempty pattern list or a base color selects patterned rendering and calls `BannerRenderer.submitPatterns`. No legacy BlockEntityTag, custom model data, or resource pack is needed for ordinary vanilla patterns.

The bundled `assets/minecraft/items/shield.json` uses a `minecraft:using_item` condition whose two branches are `minecraft:special` models with renderer type `minecraft:shield` and base models `minecraft:item/shield` / `minecraft:item/shield_blocking`. Thus the correct item-model identifier is `minecraft:shield`.

## Exact inspected sources

- Project: `src/main/java/me/skywars2/game/LbcElectro.java:90-107`.
- Native source jar: `.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-d6f9bb6878/26.3/minecraft-merged-d6f9bb6878-26.3-sources.jar` entries `net/minecraft/world/item/Item.java`, `net/minecraft/client/renderer/special/ShieldSpecialRenderer.java`.
- Native jar in the same directory: `minecraft-merged-d6f9bb6878-26.3.jar`, entry `assets/minecraft/items/shield.json`.
- Polymer source jar: `C:/Users/yarik_temp/.gradle/caches/modules-2/files-2.1/eu.pb4/polymer-core/0.18.2+26.3/b6b76cb8184ceec3de099762a03da7cc3cd0339b/polymer-core-0.18.2+26.3-sources.jar`, entries `eu/pb4/polymer/core/api/item/{SimplePolymerItem,PolymerItem,PolymerItemUtils}.java`.

This establishes a concrete registration defect from source. It does not replace client verification after repair, and does not independently prove every live acquisition path retains loot-table pattern components.
