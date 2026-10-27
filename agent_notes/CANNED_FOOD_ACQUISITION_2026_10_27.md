# Canned Food: native acquisition gap - 2026-10-27

Bounded read-only audit at the parent-reported HEAD f6285db. Only this new note is written; no source/RP, production, fixture, runner, build, server or Git mutations. This note corrects the acquisition assumption in [CANNED_FOOD_SOURCE_2026_10_27.md](CANNED_FOOD_SOURCE_2026_10_27.md); that source note remains unchanged.

## Finding

The raw Canned Food leaf exists in the source tree, but it is **not packaged or reachable through an established native normal-play producer**. The complete11-outcome consumer can be ported independently as code, but operator spawning or a newly extracted only-access leaf would not close normal-play acquisition. No minimal already-live producer was established; the broader LuckyWars source producer remains a genuine port gap.

The inspected actual artifact is `build/libs/skywars2-0.1.0.jar`, SHA256 **b8cb264789913000ca310664cfb07af03cf4b6b797e8be90dca9b9d18a084f04**. This is filesystem/JAR inspection, not a new build or runtime registry observation.

| Resource | Raw src/main/resources | build/resources/main | Inspected actual JAR |
|---|---|---|---|
| data/lbc/loot_table/lucky_items.json | Present | Absent | Absent |
| data/lbc/loot_table/lucky_items_1.json | Present | Absent | Absent |
| data/lbc/loot_table/luckyblock.json | Present | Absent | Absent |
| data/lbc/loot_table/luckyblock_without_rec.json | Present | Absent | Absent |
| data/lbc/loot_table/luckyblock_64.json | Present | Absent | Absent |
| data/minecraft/loot_table/blocks/red_mushroom_block.json | Source legacy block table inspected | Absent | Absent |

`build.gradle processResources` uses an explicit include whitelist. No lucky_items/luckyblock inclusion was found. Existing voidman and transformable-pool inclusion does not imply inclusion of these unrelated tables. The raw lucky_items file still uses legacy functions/function fields (63 literal function keys), rather than the modern modifier/type schema used by already migrated native leaves.

## Exact source route and leaf

Legacy `data/minecraft/loot_table/blocks/red_mushroom_block.json` maps the special all-six-faces-false block state to source LuckyBlock loot. Current pools check global `legacy lbc.math ==0`, distinguish Silk Touch and use actual source `luck_amount`, Fortune and count modifiers. The first separate legacy1 pool points to legacy_luckyblock. This is a block-loot producer, not an item-use callback.

The current chain is special red_mushroom_block break -> lbc:luckyblock -> lbc:lucky_items -> pool0 entry74. Source luckyblock also has independent high-quality/imba/barrier/other branches; preserving source probabilities requires their actual conditions and complete dependencies, not replacing the table with the canned leaf. luckyblock_without_rec and luckyblock_64 are additional source references. lucky_items_1 is also used by source `other/everythingend/trueknowledge`, which creates a loot chest; no corresponding native trueknowledge producer was found. Source evil-LuckyBlock mob DeathLootTable references likewise point to the same unshipped LuckyBlock tables.

Entry74: repeating_command_block count16, weight10/quality7, model lbc:konserva, tier_d, light_purple nonitalic translated konserva, two same-style konserva.lore.1/.2 rows, custom_data `{konserva:1}` ONLY. There is no transformable/accessory/category/instantitem/edible flag. No old variant, recipe or canned-food advancement route was established by the source audit. The existing RP name/model/style are usable without RP edits; that does not itself deliver an item.

## Native route checks

- `GameController.startFailure:227` explicitly rejects **config.gameMode !=1**. LuckyWars mode2 is not a supported match start. `GameLuckyModifiers` storing/resolving a mode2 category does not implement LuckyBlock breaking or delivery.
- Native `ArenaBlockMixin` records arena changes and shared block ownership. Fabric AttackBlock/UseBlock callbacks in SkyWars2 handle action/observer restrictions and menu interactions; none supplies the missing source LuckyBlock block-loot table. No production Java reference to lucky_items/luckyblock table IDs or konserva was found.
- MapManager ordinary chest assignment uses `swrg:chests/<category>_0`; no konserva payload or lucky_items reference was found in the native source resources/configuration searched. Source canonical Canned Food occurrence is the single raw lucky_items entry, not an existing SWRG chest result.
- Normal/Ultra/Lightning reforge uses LbcBolts' explicit category-to-transformables tables. None targets lucky_items or contains Canned Food. `LbcRerollCrafting.tables` additionally requires transformable1, which the source can lacks. Adding that flag/category or injecting the can into accessory rolls would invent acquisition and alter source probabilities.
- LbcLoadouts grants only actual configured kit/skill/trinket tables through its generic rollTable helper. No canned/lucky_items loadout reference was found. A generic method accepting arbitrary IDs is not a caller that awards the can.
- LootTestCatalog is an operator suggestion filter, distinct from the resource registry and normal acquisition. It currently lists qualified tables and the lbc:ported/ prefix; it does not list lucky_items. Adding a suggestion or operator-only narrow leaf cannot qualify normal play. Even a direct operator roll of the missing root table would not provide the unimplemented block-break route.
- No separate item alias, replacement native item registration, group reroll result, crafting recipe or source-equivalent existing native producer was established for konserva. TMT metadata-name indexes are descriptive registries, not a native Canned Food delivery path.

## Whole-pool boundary

lucky_items has one pool and102 source entries. A recursive traversal of **nested minecraft:loot_table references only** reaches119 source tables (all raw files exist). Of those only47 are packaged in the inspected JAR, leaving72 absent. This is a lower-bound dependency traversal: container_loot references such as infinite shulker -> lbc:infinity introduce additional obligations not counted by that traversal.

Unpackaged examples include lucky_items itself, luckyblock_imba, inf_shulker, inf_steak, god_dice, shadowglove, attributefood, multiple projectile/food helper tables, Halloween lucky_items and the imba subtree. Raw infinite_shulker has an infinity container-loot dependency; infinite_steak has its own inf1 behavior; god_dice has a separate super-dice gameplay payload. A generic vanilla carrier is not proof those source behaviors are complete. Existing source audits also retain unfinished broader accessory/Infinity/seasonal families. Thus whole-pool handler/resource closure is not established and is materially false as a packaging claim.

The root additionally contains six conditionally selected luckyblock_imba entries with huge literal weights and a conditional Halloween table. These require actual modern codec/condition/effective-weight review when their family is ported. Do not sum raw weights, truncate unresolved entries, renormalize the can against a reduced subset, or force a condition in order to describe the result as original normal-play probability.

## Smallest honest next scope

No safe pre-existing normal acquisition seam was found to attach this one consumer to. A faithful complete acquisition patch would need a separately authorized source producer restoration (for example the real special block-loot/LuckyWars route) and its full reachable table/gameplay closure, or another independently established complete source producer. That is broader than adding the Canned Food consumer and is not authorized by the fact that a raw leaf exists.

A narrow migrated leaf may support debugging/presentation verification if explicitly approved, but it must be labeled operator-only and leaves the item incomplete for normal play. Do not claim a complete family from that leaf, manual `/loot`, a fixture-built stack or an invented kit/reforge injection. Parent can choose the next complete patch scope using this concrete gap; this audit does not enable unresolved pools or request chat clarification.

## Evidence SHA256

```text
build.gradle 85edaad19ea2bb255254d4927a62115a48c9f0c78f51a20d912d55fae4aa06af
src/main/java/me/skywars2/game/GameController.java 3539528cc0aeda12172b307504c4e3f7f8fe530c6ba64a04c1299769599de98b
src/main/java/me/skywars2/game/GameLuckyModifiers.java 36e3b5aa64c3141ab60b3f9948584fdefafd4f0ae225500867453709afbd65d2
src/main/java/me/skywars2/game/LbcBolts.java 186a92f64a7daafe6eb58c39be9f23f1137613468c1a8d5dc399d14fe32cf5e2
src/main/java/me/skywars2/game/LbcRerollCrafting.java 6ef809478e69d576a50224bd9435e3b310134c94845b32391a265c52f4252dd4
src/main/java/me/skywars2/game/LbcLoadouts.java aca0e966dfb0168b4849e665c5f3456eb9524e547f517c7f47320f47ce03c2db
src/main/java/me/skywars2/game/LootTestCatalog.java 671b775eed95d86555d5d2515619d9d27368c88515cbda7829126bd0d62765b7
src/main/java/me/skywars2/mixin/ArenaBlockMixin.java 72a9796326bc7df1a5074b2c1bd35ef36d3c560c8da585804569f37ffd2840d4
src/main/resources/skywars2/loot_test_catalog.json bfe4c23bb2ffc19a1ab8f2cbbe79240045452d89fe6802b2ea415b5b1b6e8a27
src/main/resources/skywars2/lbc_loadouts.json 99aad58d57f2215005b5aaad9a3b6dcdbdf21d80ecac06020443b1f96ba7555f
src/main/resources/data/lbc/loot_table/lucky_items.json a9b73cc64f482231beb27f0ae07328180bdcb358fa6cb9cb0867369d792bbcb2
legacy lbc/loot_table/lucky_items.json a9b73cc64f482231beb27f0ae07328180bdcb358fa6cb9cb0867369d792bbcb2
legacy lbc/loot_table/luckyblock.json cf05aa79ddf00e034588ec942243e84ae804168e5f58039cde3f0f73fb07dc6a
legacy lbc/loot_table/luckyblock_without_rec.json 30abce67d5c6c08decea78f392ad3d7efaa6721db72a0bff13b8a402f783007c
legacy lbc/loot_table/luckyblock_64.json ab473d451962ad95732b3aa0e6c4545ab18ecbec3a01cf4e0a3520cc141d5e8c
legacy minecraft/loot_table/blocks/red_mushroom_block.json 5fed79c495014a7ebac99a9d14b359b515e21e7358ca9cdbc4af55513fab1c30
legacy lbc/function/other/everythingend/trueknowledge.mcfunction 80ce2bddf3ac903a729101f3f528dd3fcd134e54ffe523257705a4c17b2c963e
```

Frozen static audit. No new native acquisition observation, implementation or qualification is claimed.
