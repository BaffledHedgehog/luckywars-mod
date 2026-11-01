# Native content registry delta, 1 November 2026

Read-only audit of vanilla 26.3 against 1.21.11. No server startup, build, Java/resource/RP edit or installation was performed by this audit. Data generation invoked the existing vanilla `net.minecraft.data.Main --reports` only. This report records the initial infinite-shulker graph, before the parent implements its refresh; the Mobgun catalog was concurrently updated as noted below.

## Primary sources and reproduction

Current vanilla server: `E:/servers_mew/skywars2_last/versions/26.3/server-26.3.jar`.
Baseline bundled vanilla server: `E:/servers_mew/cringe1_21_4_skywars/cache/mojang_1.21.11.jar`, nested `META-INF/versions/1.21.11/server-1.21.11.jar`; exact dependency JARs extracted from that same bundle. Baseline JAR version.json identifies 1.21.11/world version 4671.

Execution used existing `E:/servers_mew/skywars2_last/runtime/jdk-25/bin/java.exe`, classpath of current vanilla server plus current server libraries; baseline used only its own bundled libraries. Both report processes exited 0. An initial attempt with Loom-patched merged jars failed for missing FabricRegistry; an initial baseline attempt with mixed historical server libraries failed with an incompatible Codec. Those attempts produced no valid report and are not evidence. Existing merged jars are patched and must not be substituted for the vanilla server JAR in report generation.

Evidence root: `build/verification/content-refresh-2026-11-01/`:

- `native-registry/reports/registries.json`: exact current native built-in registries.
- `native-registry-1.21.11/reports/registries.json`: exact baseline built-in registries.
- `registry-delta.json`: complete sorted IDs, counts, graph omissions, technical classification and source SHA-256.
- `old-report.args`, `old-native-report.log`: baseline invocation and successful output.

Current source corroboration: `.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-d6f9bb6878/26.3/minecraft-merged-d6f9bb6878-26.3-sources.jar`, entries `net/minecraft/world/item/Items.java`, `net/minecraft/references/ItemIds.java`, `net/minecraft/references/BlockItemIds.java`, `net/minecraft/world/entity/EntityTypeIds.java`, `net/minecraft/world/entity/EntityTypes.java`. Source declares colored/copper families programmatically; matching identifiers with regex alone is insufficient. The exact native dump avoids those omissions.

## Infinite shulker

Native ITEM count rises from **1505 to 1658**: **153 additions, no removals**. All 153 are absent from the initial infinite loot graph. They include poplar wood/leaves/boats, cinnabar/sulfur blocks, colored wool/concrete stairs and slabs, 16 cushions, straw bed, new maps, golden dandelion, sulfur-cube egg/bucket, red shrub, shelf mushroom and music disc bounce. Full IDs are in `registries.minecraft:item.added_since_1_21_11` in the JSON.

Starting `src/main/resources/data/lbc/loot_table/infinity_no_shulker.json` and recursively following actual loot-table value/name references reaches **1816 tables and 1503 distinct base item IDs**. All references resolve, and all reachable minecraft item IDs exist in current registry. There are **155 missing current IDs**: 153 new IDs plus the preexisting omission **`minecraft:stripped_bamboo_block`**, and technical **`minecraft:air`**. This yields **154 usable additions** when air is kept separate.

The existing table is already broad: command blocks, operator/test blocks and all 17 shulker-box bases are reachable despite the root filename. Do not infer an exclusion policy from that filename or remove existing creative-only items. Technical/special bases already reachable include: `barrier`, `structure_void`, `light`, `structure_block`, `jigsaw`, `command_block`, `chain_command_block`, `repeating_command_block`, `command_block_minecart`, `debug_stick`, `knowledge_book`, `spawner`, `trial_spawner`, `vault`, `bedrock`, `end_portal_frame`, `reinforced_deepslate`, `test_block`, `test_instance_block`, `player_head`, `dragon_head`. `air` represents an empty stack rather than a usable reward; classify it explicitly instead of silently treating it as an ordinary gameplay item.

## Mobgun

Native ENTITY_TYPE count rises from **157 to 161**, adding only `minecraft:cushion`, `minecraft:poplar_boat`, `minecraft:poplar_chest_boat`, **`minecraft:sulfur_cube`**. No removals. Initial `src/main/resources/skywars2/mobgun_presets.json` lacked sulfur_cube; the Mobgun agent added it concurrently, so the machine JSON's final all-entity catalog diff already reflects that addition. Other missing registry entities are boats, displays, decorations, projectiles, player/technical entities rather than missing Mob subclasses.

`EntityTypes.java` registers sulfur_cube with `MobCategory.MONSTER`, size 0.49, eye height 0.175, spawn scale 2 and tracking range 10. Source `monster/cubemob/SulfurCube.java` extends `AbstractCubeMob`; `Cushion` extends `BlockAttachedEntity`; `Mannequin` extends `Avatar`. Mannequin is already a baseline entity and must not be misreported as newly added mob. The existing catalog already includes copper_golem, nautilus, zombie_nautilus, camel_husk and parched; its originalDataVersion does not establish its current content date.

## Other exhaustive lists and limits

Native POTION remains **46**, MOB_EFFECT remains **40**; neither registry adds/removes an ID after 1.21.11. Registry membership alone does not establish that every potion component/effect variant is present in each consumer's curated reward list. Dynamic/data-driven registries such as enchantments are absent from this built-in registry dump and require JAR data/source comparison separately. New map items are valid native base IDs; whether generated destination data is desired is a behavior choice beyond base-item completeness.

This audit proves base-ID coverage and exact vanilla version delta, not successful gameplay opening/shooting or release qualification. No internet source was needed.

## Extended complete-registry audit

The JSON now includes **all_builtin_registry_deltas** (every registry appearing in either native report, with counts, exact added/removed keys and registry-presence flags) and **jar_data_registry_deltas** (top-level vanilla data registries plus each worldgen subregistry). JAR comparison excludes recipes, loot tables, tags, advancements, predicates, embedded feature packs and test fixtures: those are not interpreted as canonical registry membership. Shared-key payloads are compared as parsed JSON, so formatting changes alone do not count. Data-file key presence is primary-source evidence; it is not an independently loaded dynamic-registry dump.

| Gameplay registry/list | 1.21.11 | 26.3 | Key changes |
|---|---:|---:|---|
| Blocks | 1166 | 1286 | +120; exact IDs in JSON |
| Attributes | 35 | 40 | +air_drag_modifier, below_name_distance, bounciness, friction_modifier, name_tag_distance |
| Particle types | 115 | 128 | +13, listed below |
| Sound events | 1838 | 1991 | +153; exact IDs in JSON |
| Game events | 60 | 61 | +bounce |
| Custom stats | 77 | 78 | +sleep_in_straw_bed |
| Map decoration types | 35 | 40 | +abandoned_camp, ancient_city, desert_pyramid, mineshaft, ocean_ruin_warm |
| Fluids | 5 | 5 | None |
| Villager types/professions | 7/15 | 7/15 | None |
| Enchantments, vanilla data keys | 43 | 43 | None; 20 shared payloads differ |
| Trim materials/patterns | 11/18 | 11/18 | None; all 11 material payloads differ |
| Biomes, vanilla data keys | 65 | 67 | +dappled_forest, sulfur_caves; all 65 shared payloads differ |
| Jukebox songs | 21 | 22 | +bounce |
| Banner/painting variants | 43/51 | 43/51 | None |
| Goat-horn instruments | 8 | 8 | None |
| Trial-spawner configurations | 28 | 28 | None |
| Damage types | 50 | 51 | +sulfur_cube_hot |

New particle keys: `geyser`, `geyser_base`, `geyser_plume`, `geyser_poof`, `noxious_gas`, `noxious_gas_cloud`, `orange_poplar_leaves`, `pause_mob_growth`, `red_poplar_leaves`, `reset_mob_growth`, `sulfur_bubbles`, `sulfur_cube_goo`, `yellow_poplar_leaves` (all minecraft namespace).

Animal visual-variant keys are unchanged: cat 11, chicken 3, cow 3, pig 3, frog 3, wolf 9, zombie nautilus 2. Their existing data payloads mostly change; this does not create new variant IDs. New separate sound-variant data registries contain cat `classic`/`royal`, chicken `classic`/`picky`, cow `classic`/`moody`, pig `big`/`classic`/`mini`. Wolf sound variants retain the same 7 keys. Sulfur-cube archetypes are a new 12-key data registry: `bouncy`, `explosive`, `fast_flat`, `fast_sliding`, `high_resistance`, `hot`, `light`, `regular`, `slow_bouncy`, `slow_flat`, `slow_sliding`, `sticky`.

Internal engine and schema registries also change. Do not translate an absent registry into missing gameplay entries: for example old built-in `decorated_pot_pattern` (24 including blank) is no longer in the current built-in report, while 23 nonblank patterns now appear as data files. Worldgen `feature`/`carver`/material registries likewise move or rename. Data-component registry changes from 104 to 122 (20 additions, 2 removals), including `swing_animation`?`attack_animation`, animal sound-variant components and sulfur-cube content; this is a serialization review cue, not an instruction to add engine-internal identifiers to reward lists.

Existing exhaustive consumers should be checked against actual relevant keys, not all registry families indiscriminately. The complete-list audit agent received the new attribute/particle/biome/sound-variant identifiers. Base-item, mob and gameplay consumer implementation/qualification remains with the parent and those component agents.
