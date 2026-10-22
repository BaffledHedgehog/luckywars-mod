# Next special-drop source batch — 22 October 2026

Research only. This is a recommendation, not an implemented/qualified target. No code, builds, server or RP changes were made. Crocodildo and Muravei are outside this audit.

Legacy root below: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`.

Recommend exactly five native loot-chest branches. They share one token presentation and one native block/loot implementation, require no encounter lifecycle and no new RP assets.

| Flag | Source dispatch | Native loot table | Source token payload |
| --- | --- | --- | --- |
| bastion | function/drops/specialdrops.mcfunction:3 | minecraft:chests/bastion_treasure | loot_table/barriersnottraps.json:310 |
| end_city | function/drops/specialdrops.mcfunction:4 | minecraft:chests/end_city_treasure | loot_table/barriersnottraps.json:178 |
| treasure | function/drops/specialdrops.mcfunction:5 | minecraft:chests/buried_treasure | loot_table/barriersnottraps.json:354 |
| village | function/drops/specialdrops.mcfunction:6 | minecraft:chests/village/village_weaponsmith | loot_table/barriersnottraps.json:222 |
| bonus_chest | function/drops/specialdrops.mcfunction:7 | minecraft:chests/spawn_bonus_chest | loot_table/barriersnottraps.json:266 |

Each command is unconditional `setblock ~ ~ ~ minecraft:chest{LootTable:...}` at the dropped item's position, with command-default replacement semantics, no nearest-player targeting and no source loot seed. Final two lines of specialdrops kill count=1 or decrement count by one. Source dispatch is gated by item drop/working tags (`function/items_tick_tag.mcfunction:19`, `items_tick.mcfunction:2`, `items_tick_act.mcfunction:1`). Preserve one activation/debit per eligible tick for stacks; no whole-stack consumption or inventory scan.

All five ordinary token definitions: repeating_command_block, count1, tooltip_style `lbc:tier_d`, item_model `lbc:drop_not_trap`; yellow nonitalic translated names `drop_bastion`, `drop_end_city`, `drop_treasure`, `drop_village`, `drop_bonus`; one white nonitalic translated lore row `trap_desc`; custom_data `{FLAG:1,drop:1}`. All weight10; qualities respectively5/5/5/0/-5. See ordinary entry ranges 138-357. Legacy luckyblock also supplies same flags at loot_table/legacy_luckyblock.json:1404/1422/1440/1458/1476. Imba entries at loot_table/imba/barriersnottraps.json:82/102/122/142/162 add imba:1, weight0 and have empty components/no names or lore: do not silently classify their undecorated source payload as an already approved new presentation.

Read-only RP checks: initial audit read the stale documented `instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8/assets/minecraft/lang/ru_ru.json`. Root then identified the active RP as `C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3/minecraft/resourcepacks/all_in_one_1.21.8`; this actual active path was separately checked. Active `assets/minecraft/lang/ru_ru.json:2609,2613-2617` contains all five exact names and `trap_desc`, identical to the values first read (including source spelling). Active `assets/lbc/items/drop_not_trap.json:1` points to `lbc:item/drop_not_trap`; active `assets/lbc/models/item/drop_not_trap.json:1` uses generated-item parent and `minecraft:item/drop_not_trap` texture. Active tier_d frame/background PNG and mcmeta files exist under `assets/lbc/textures/gui/sprites/tooltip/`. Reuse these existing keys/model/style unchanged; no asset additions are indicated. The 26.3(1) path is not asserted to be current.
Existing target building blocks, not proof of these branches being ready:

- `src/main/java/me/skywars2/game/LbcHelicopterTriggerItem.java:31-38`: native Polymer token registration and component presentation pattern.
- `LbcHelicopterEncounter.java:80-87,101,133-138,265-277`: custom_data subset match, event-driven pending dropped-item set, arena/game/time-stop guards, unit debit, unload/stop cleanup. Generalize pattern, not the helicopter encounter state.
- `LbcItems.java:734,812,1189,1317`: load/unload/tick/stop integration points.
- `MapManager.java:514,520-534`: native chest placement then RandomizableContainerBlockEntity loot-table/seed assignment pattern. For source fidelity do not eagerly roll into player-context rewards; vanilla opening should own loot context.
- `MapManager.java:669`: recordBlockChange for eventual map cleanup; normal native world mutation hooks may already cover placement, verify before duplicating journal calls.
- Copied `src/main/resources/data/lbc/loot_table/barriersnottraps.json:178,222,266,310,354` still contains source flags. Presence in copied data does not qualify runtime dispatch or make those command-block tokens usable.

Boundaries to settle explicitly in implementation/verification: source replacement of existing blocks and containers (including their native drop behavior); failed placement must have a deliberate debit policy (source still debits regardless); avoid adjacent-chest accidental merging if native setblock differs from command placement; preserve exact location flooring, default chest state, unopened deferred loot and replacement cleanup. Current authorization can preserve source behavior without extra author questions; ask only if changing it. Keep ordinary gameplay Lucky Blocks disabled as already required; direct fixtures are enough to exercise these isolated branches.

Defer TNT as separate follow-up: `specialdrops.mcfunction:8` and `function/drops/tnts.mcfunction:1-13` spawn13 primed TNT at Y+.1, fuse70, vertical velocity1 and specified X/Z offsets (center, eight ±.1 combinations, four axial ±.2). Adds attribution/explosion/team/service/cleanup concerns absent in the chest batch. User requires preserved causal owner even where source omits attribution.

Defer creepers/cages: `function/drops/creepers.mcfunction:1-10` already mixes radius1/2 creepers with lbc:creeper_wand death loot; `anvilcage.mcfunction:1-8` edits a cage plus a shaft to absolute Y319, spawns damaging falling anvils at +40/+42 and targets nearest nonspectator via specialdrops:20. These are separate harmful-target and terrain-edit contracts, unsuitable padding for a small chest batch.
