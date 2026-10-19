# Mystical artifact type 1:3: fluid and falling block hardening

Historical source research. The bounded implementation is described in
[ARTIFACT_LIQUIDS_PORT.md](ARTIFACT_LIQUIDS_PORT.md); current verification and
deployment evidence is maintained in [SERVER_TESTS.md](../SERVER_TESTS.md).
The cube described below is legacy behavior. The user requested a sphere for
liquids; the current port uses radius 16, matching the falling-block radius.

This is read-only source research for the next artifact effect after the type
1:2 explosion-suppression batch. It records the legacy datapack behavior and a
candidate implementation shape; the recommendations below are not behavior
already implemented or verified in the new mod.

## Legacy source behavior

The type 1:3 artifact is selected in
`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/artifact.json`
by custom data `{mystical:1,type:3}` (the entry also has
`transformable:1`, `accessory:1`, and `unresolved_artifact:1`). The old English
and Russian `artifact_3` values describe “Hardening (fluids and falling
blocks)” and “Отвердевание (жидкостей и падающих блоков)” respectively in
`plugins/luckywars/resourcepack_lang/{en_us,ru_ru}.json:3426`.

The scheduled path is `function/load.mcfunction` → `tick5t` (reschedules itself
every 5 ticks) → `mobs_5t` → `pre_player_5t` → `players_5t`. The scheduled
`tick5t` iterator excludes dimensions `minecraft:nexus` and
`minecraft:imprinted`; `pre_player_5t` dispatches player work only outside
spectator mode. `players_5t:1` calls `other/artifact/afkartifact5t` when the
player has `afkartifact5t_active` and there is no `stopper_magic` marker within
20 blocks. Inventory refresh code in `items/changed_inventory/sel:87-88,101`
sets that broad active tag for a `mystical:1` item in `container.*` or
`weapon.*`, and clears it when neither search finds one.

Inside `other/artifact/afkartifact5t`, the type-specific dispatch checks
`tag=!spectator` and an inventory item with both `mystical:1` and `type:3`, then
calls `other/artifact/nofluid`. That function does not require the item to be
selected, does not test `unresolved_artifact`, and does not test
`nomagic_active`. The only explicit magic suppression on this route is the
20-block `stopper_magic` check in `players_5t`.

`other/artifact/nofluid.mcfunction:1-4` performs these operations at the
holder's position:

- In the inclusive relative cube from `~12 ~12 ~12` through `~-12 ~-12 ~-12`,
  replace `minecraft:lava` with basalt, `minecraft:water` with frosted ice, and
  `minecraft:powder_snow` with snow blocks. This is a 25×25×25 block volume
  (15,625 positions), not a sphere.
- Select every `minecraft:falling_block` entity at `distance=..16` and run
  `other/artifact/block_otverditel` at each entity. This entity selector uses a
  radius 16 sphere.

`block_otverditel.mcfunction:1-24` changes sand to sandstone, gravel to
cobblestone, red sand to red sandstone, the three anvil states to iron blocks,
and these concrete powders to their matching concrete: white, blue, orange,
brown, light blue, green, black, gray, light gray, lime, magenta, purple, pink,
red, yellow, and cyan. The stone-bricks and netherite-block branches place
that same material in the world at the falling entity's position. Each branch
reads the entity's `BlockState.Name`, not the existing world block there.
After those conditional conversions,
the function always emits smoke, plays `block.fire.extinguish` at the entity's
position to `@a`, then kills the falling-block entity. Thus every falling block
inside the sphere is removed, including states without a listed conversion.

The source entity and block operations are centered differently: the block
cube is anchored to the holder's execution position, while the falling block
radius is measured from that same position in Euclidean distance. The caller
has no explicit chunk-loaded check or chunk-load request. Static source does
not establish how a fill behaves at every unloaded-chunk edge; a port should
avoid loading chunks while inspecting this region.

## Related block tags and dependencies

`data/minecraft/tags/block/liquid.json` lists water, lava, and powder snow.
`data/minecraft/tags/block/fluids.json` has `replace:false` and lists lava,
water, powder snow, `#minecraft:cauldrons`, snow, snow block, `#minecraft:ice`,
and clay. The `nofluid` function uses none of these tags: it names its three
replaced block states directly. In particular, it does not convert cauldrons,
ice, clay, snow, or snow blocks. Do not use the broader `fluids` tag as a
substitute without making that behavior expansion an explicit product choice.

The falling-entity path depends on direct access to `FallingBlock` state and
the existing block conversions listed above. The aura's external suppression
dependency is the `stopper_magic` marker within 20 blocks. `nomagic_active` is
not a source veto here. The liquid behavior does not depend on a block tag.

## Recommended next-port boundary

Port the native type 1:3 artifact loot entry and this effect as one batch:
the imported table can be named
`data/lbc/loot_table/ported/mystical_artifact_liquids.json`;
preserve the source name, lore, formatting, model, tooltip style, and custom
data; replace only the server item id with `skywars2:mystical_artifact`.
Implement the active holder gate, the 20-block `stopper_magic` veto, the
three named fluid-state conversions in the inclusive ±12 cube, and the
falling-block conversions plus smoke, sound, and entity removal inside the
radius 16 sphere. Keep the source's non-spectator restriction. The broad active
item search should not accidentally enable effects from observer copies or
other artifact types. The old path gates on non-spectator state; the new mod
should use the project's `game.isObserver(player)` check so Adventure
observers are excluded as well.

For CPU cost, register active type 1:3 holders from inventory changes and
remove them on item loss, player removal, death, or world change. Keep an
index of active auras by world/chunk so a block or entity update can find only
nearby eligible holders. On activation, scan the currently loaded part of the
source cube once; after movement, inspect only newly covered block slices.
Before any block read, confirm its chunk is already loaded and skip unloaded
chunks without requesting them. Apply the source block conversion to fluid
spread and other relevant server-side block-state updates inside an active
cube. For falling blocks, inspect entities once when an aura activates and
again when a falling entity moves into an active radius; keep the source's
squared-distance threshold of 256 for the radius 16 sphere.

Event hooks must cover natural fluid flow and frosted-ice melt/replacement as
well as player placement. If public Fabric events do not report every relevant
fluid state change, use one narrow server-side hook around the native state
mutation and filter it through the active-aura index. A small bounded
reconciliation pass can cover mutation paths that the hook misses, but should
not rescan the full cube for every holder every five ticks. Test fluid spread,
source updates, frosted-ice melting, all three direct source states, falling
blocks entering and leaving the radius, overlapping holders, the marker veto,
observer exclusion, and unloaded chunk boundaries before treating behavior as
equivalent.

Keep this batch limited to type 1:3. The wider `minecraft:fluids` tag,
anti-magic `nomagic_active` behavior, mana systems, other artifact types, and
unrelated falling-block behavior are outside the legacy effect path described
here.
